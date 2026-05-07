package dev.hearthbound.npc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.itemanimation.config.ItemPlayerAnimations;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.systems.RoleChangeSystem;

import dev.hearthbound.building.BuildingLayout;
import dev.hearthbound.building.WarehouseDepositor;
import dev.hearthbound.util.TickScheduler;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;
import dev.hearthbound.village.VillagerData;
import dev.hearthbound.village.VillagerSummary;

/**
 * Drives villager daily schedule:
 *
 *   NIGHT  (20:00–06:00) → home
 *   WORK   (06:00–20:00) → profession-specific location (farmers → farm)
 *
 * Movement pattern:
 *   - Traveling: switch to ROLE_TRAVELING + spawn invisible marker entity at destination
 *     + setMarkedEntity("LockedTarget", markerRef) so Seek walks NPC there via pathfinder
 *   - Arrived: switch to destination role + setLeashPoint for WanderInCircle
 */
public class VillagerScheduler {

    private static final Logger LOGGER = Logger.getLogger(VillagerScheduler.class.getName());

    // Role names — must match JSON files in Server/NPC/Roles/
    private static final String ROLE_VILLAGER    = "Villager_Human";
    private static final String ROLE_FARMER      = "Villager_Human_Farmer";
    private static final String ROLE_LUMBERJACK  = "Villager_Human_Lumberjack";
    private static final String ROLE_MINER       = "Villager_Human_Miner";
    private static final String ROLE_GUARD       = "Villager_Human_Guard";
    private static final String ROLE_BLACKSMITH  = "Villager_Human_Blacksmith";
    private static final String ROLE_TRAVELING   = "Villager_Human_Traveling";
    private static final String ROLE_EATING      = "Villager_Human_Eating";

    // Day phases (24h clock)
    private static final double WORK_START   = 6.0;
    private static final double WORK_END     = 18.0;
    private static final double LUNCH_START  = 12.0;
    private static final double LUNCH_END    = 13.5;
    private static final double DINNER_START = 18.0;
    private static final double DINNER_END   = 19.5;

    // Switch to "arrived" once the NPC is within this distance of the target
    private static final double ARRIVAL_RADIUS_SQ = 3.0 * 3.0;
    private static final long TRAVEL_STUCK_MS = 15_000L;
    private static final double TRAVEL_PROGRESS_EPS_SQ = 1.0;
    private static final double TRAVEL_MOVE_EPS_SQ = 0.25;

    // Per-villager last scheduled target (to detect changes)
    private final Map<UUID, ScheduleTarget> lastTarget = new HashMap<>();
    // Per-villager route origin. Needed when leaving work/eating buildings whose doors
    // may block pathfinding before the destination route has a chance to open.
    private final Map<UUID, ScheduleTarget> routeOrigin = new HashMap<>();
    private final Map<UUID, Set<GateKey>> activeRouteGates = new HashMap<>();
    private final Map<GateKey, Gate> knownGates = new HashMap<>();
    private final Map<UUID, TravelProgress> travelProgress = new HashMap<>();
    // Per-villager marker entity ref (invisible Seek target)
    private final Map<UUID, Ref<EntityStore>> markerRefs = new HashMap<>();
    // Per-villager current activity label for UI
    private final Map<UUID, String> activityLabel = new HashMap<>();
    // Villagers already fed during the current meal window (lunch or dinner)
    // Cleared when the meal window ends, preventing repeated feedVillager() calls.
    private final Set<UUID> fedThisMeal = new HashSet<>();
    private final Set<UUID> eatingVillagers = new HashSet<>();
    private final Random foodRandom = new Random();

    // Farming state machine — drives the in-place harvest/replant/water/weed loop while a
    // farmer is at their farm and in WORKING activity. Stateless across restarts.
    private final FarmerWorkBehavior farmerWork = new FarmerWorkBehavior();

    // Cached during the current tick(): village owner UUID, used to route lookAtBlock packets
    // for the farmer (mirrors how BuilderBehavior addresses the elf).
    private UUID currentOwnerUuid;

    public static final String ACTIVITY_GOING_TO_WORK = "Going to work";
    public static final String ACTIVITY_WORKING        = "Working";
    public static final String ACTIVITY_GOING_TO_EAT  = "Going to eat";
    public static final String ACTIVITY_EATING         = "Eating";
    public static final String ACTIVITY_GOING_HOME     = "Going home";
    public static final String ACTIVITY_RESTING        = "Resting";

    /**
     * Clears all per-villager schedule state. Called from /hb reset so the scheduler
     * does not leak references to villagers that were just removed from the village.
     * Best-effort despawns invisible Seek-target marker entities — if a marker is in an
     * unloaded chunk the ref is silently dropped (the entity gets garbage-collected by
     * the engine on next chunk load with no live reference holding it).
     */
    public void clear(Store<EntityStore> store) {
        for (Ref<EntityStore> markerRef : markerRefs.values()) {
            if (markerRef != null && markerRef.isValid()) {
                try {
                    store.removeEntity(markerRef, RemoveReason.REMOVE);
                } catch (RuntimeException e) {
                    LOGGER.warning("VillagerScheduler.clear: failed to remove marker: " + e.getMessage());
                }
            }
        }
        lastTarget.clear();
        routeOrigin.clear();
        activeRouteGates.clear();
        knownGates.clear();
        travelProgress.clear();
        markerRefs.clear();
        activityLabel.clear();
        fedThisMeal.clear();
        eatingVillagers.clear();
        LOGGER.info("VillagerScheduler cleared");
    }

    // null gateX means "no gate/door to manage"
    private record Gate(int x, int y, int z, int rotation, String openBlock, String closeBlock) {}
    private record GateKey(int x, int y, int z) {}
    private record TravelProgress(double x, double y, double z, double distanceSq, long lastProgressAt) {}

    private record ScheduleTarget(double x, double y, double z, String arrivedRole, String activity,
                                   Integer gateX, Integer gateY, Integer gateZ,
                                   int gateRotation,
                                   String openBlock, String closeBlock,
                                   List<Gate> gates) {
        ScheduleTarget(double x, double y, double z, String arrivedRole, String activity,
                       Integer gateX, Integer gateY, Integer gateZ,
                       int gateRotation, String openBlock, String closeBlock) {
            this(x, y, z, arrivedRole, activity,
                    gateX, gateY, gateZ, gateRotation, openBlock, closeBlock,
                    gateX != null
                            ? List.of(new Gate(gateX, gateY, gateZ, gateRotation, openBlock, closeBlock))
                            : List.of());
        }
        ScheduleTarget(double x, double y, double z, String arrivedRole, String activity) {
            this(x, y, z, arrivedRole, activity, null, null, null, 0, null, null, List.of());
        }
        boolean hasGate() { return !gates.isEmpty(); }
    }

    public void tick(Store<EntityStore> store, Ref<EntityStore> playerRef, VillageData village, World world) {
        double time24 = getTime24(store);
        if (time24 < 0) return;

        // Cache the village owner UUID for this tick so per-villager hooks can route
        // animation/rotation packets to the right player.
        try {
            var uuidComp = store.getComponent(playerRef,
                    com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType());
            currentOwnerUuid = uuidComp != null ? uuidComp.getUuid() : null;
        } catch (Exception e) {
            currentOwnerUuid = null;
        }

        // Clear fed-tracking between meal windows so villagers can eat again next meal.
        // Do not clear eatingVillagers here: a meal that already started should finish,
        // otherwise late arrivals can consume one item and leave still hungry.
        if (!inWindow(time24, LUNCH_START, LUNCH_END) && !inWindow(time24, DINNER_START, DINNER_END)) {
            fedThisMeal.clear();
        }

        BuildingRecord farm      = VillageManager.get().findCompletedFarm(village);
        BuildingRecord warehouse = VillageManager.get().findCompletedWarehouse(village);
        BuildingRecord sawmill   = VillageManager.get().findCompletedSawmill(village);
        BuildingRecord mine      = VillageManager.get().findCompletedMine(village);
        BuildingRecord guardHouse = VillageManager.get().findCompletedGuardHouse(village);
        BuildingRecord forge     = VillageManager.get().findCompletedForge(village);

        Archetype<EntityStore> query = Archetype.of(NPCEntity.getComponentType());
        store.forEachChunk(query, (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                try {
                    Ref<EntityStore> ref = chunk.getReferenceTo(i);
                    VillagerData data = store.getComponent(ref, VillagerData.getComponentType());
                    if (data == null) continue;
                    if (VillagerData.RACE_ELF.equals(data.getRace())) continue;

                    UUID uuid = NpcManager.extractUuid(store, ref);
                    if (uuid == null) continue;
                    if (VillageManager.get().findVillagerSummary(village, uuid) == null) continue;

                    boolean hasHouse = findVillagerHouse(village, uuid) != null;
                    if (hasHouse) {
                        if (!data.isHasHouse()) {
                            data.setHasHouse(true);
                            store.putComponent(ref, VillagerData.getComponentType(), data);
                        }
                        tickVillager(ref, store, data, village, farm, warehouse, sawmill,
                                mine, guardHouse, forge, time24, world);
                    }
                } catch (Exception e) {
                    LOGGER.warning("VillagerScheduler tick error: " + e.getMessage());
                }
            }
        });
    }

    private void tickVillager(Ref<EntityStore> ref, Store<EntityStore> store, VillagerData data,
                              VillageData village, BuildingRecord farm, BuildingRecord warehouse,
                              BuildingRecord sawmill, BuildingRecord mine,
                              BuildingRecord guardHouse, BuildingRecord forge,
                              double time24, World world) {
        UUID uuid = NpcManager.extractUuid(store, ref);
        if (uuid == null) return;

        BuildingRecord house = findVillagerHouse(village, uuid);
        VillagerSummary summary = VillageManager.get().findVillagerSummary(village, uuid);
        String profession = summary != null ? summary.getProfession() : VillagerData.PROF_NONE;

        // Personal workplace lookup beats the village-wide "first of type" fallback so each
        // farmer/lumberjack/miner reports to the building that actually assigned them.
        BuildingRecord ownFarm    = findVillagerWorkplace(village, uuid, BuildingType.FARM);
        BuildingRecord ownSawmill = findVillagerWorkplace(village, uuid, BuildingType.SAWMILL);
        BuildingRecord ownMine    = findVillagerWorkplace(village, uuid, BuildingType.MINE);
        BuildingRecord ownGuardHouse = findVillagerWorkplace(village, uuid, BuildingType.GUARD_HOUSE);
        BuildingRecord ownForge   = findVillagerWorkplace(village, uuid, BuildingType.FORGE);

        BuildingRecord effectiveFarm    = ownFarm    != null ? ownFarm    : farm;
        BuildingRecord effectiveSawmill = ownSawmill != null ? ownSawmill : sawmill;
        BuildingRecord effectiveMine    = ownMine    != null ? ownMine    : mine;
        BuildingRecord effectiveGuardHouse = ownGuardHouse != null ? ownGuardHouse : guardHouse;
        BuildingRecord effectiveForge   = ownForge   != null ? ownForge   : forge;

        ScheduleTarget target = resolveTarget(uuid, data, profession, village, house,
                effectiveFarm, warehouse, effectiveSawmill, effectiveMine,
                effectiveGuardHouse, effectiveForge, time24);
        if (target == null) return;

        ScheduleTarget prev = lastTarget.get(uuid);
        boolean targetChanged = prev == null
                || distanceSq(prev.x(), prev.y(), prev.z(), target.x(), target.y(), target.z()) > ARRIVAL_RADIUS_SQ;

        // Check if villager has arrived at the current target
        boolean arrived = !targetChanged && isNearTarget(store, ref, target);

        ScheduleTarget houseTarget = house != null ? homeTarget(house, village) : null;
        boolean isFarmerWorkingTarget = ROLE_FARMER.equals(target.arrivedRole())
                && ACTIVITY_WORKING.equals(target.activity());

        if (targetChanged) {
            ScheduleTarget originTarget = prev != null ? prev : inferCurrentOrigin(store, ref, village,
                    houseTarget, effectiveFarm, warehouse, effectiveSawmill, effectiveMine,
                    effectiveGuardHouse, effectiveForge);
            // Open both ends of the route. Some work buildings have their own doors.
            openRoute(uuid, originTarget, houseTarget, target, world);
            lastTarget.put(uuid, target);
            routeOrigin.put(uuid, originTarget);
            resetTravelProgress(uuid, store, ref, target);
            activityLabel.put(uuid, travelActivity(target.activity()));
            // Farming state is invalidated whenever the farmer leaves the farm — going home,
            // going to eat, or being reassigned. Drop the runtime state so the next arrival
            // starts a fresh pick.
            if (!isFarmerWorkingTarget) {
                farmerWork.clear(uuid, world);
            }
            LOGGER.info("scheduleTargetChanged uuid=" + uuid
                    + " time24=" + String.format("%.2f", time24)
                    + " profession=" + profession
                    + " activity=" + travelActivity(target.activity())
                    + " origin=" + describeTarget(originTarget)
                    + " target=(" + String.format("%.2f,%.2f,%.2f", target.x(), target.y(), target.z()) + ")");
            world.execute(() -> {
                Store<EntityStore> liveStore = world.getEntityStore().getStore();
                startTraveling(ref, liveStore, target, world, uuid);
            });
        } else if (arrived) {
            switchRole(ref, store, target.arrivedRole(), world);
            // Don't clobber the farmer's per-cell leashPoint with the farm-center one — the
            // FarmerWorkBehavior tick below moves the leash to whichever crop/weed/tile it's
            // currently working on. Other arrivedRoles get the standard center-pin.
            if (!isFarmerWorkingTarget) {
                setLeashPoint(ref, store, new Vector3d(target.x(), target.y(), target.z()));
            }
            removeMarker(uuid, world);
            activityLabel.put(uuid, target.activity());
            closeRouteIfUnused(uuid, world);
            routeOrigin.remove(uuid);
            travelProgress.remove(uuid);
            LOGGER.info("scheduleArrived uuid=" + uuid
                    + " time24=" + String.format("%.2f", time24)
                    + " activity=" + target.activity()
                    + " role=" + target.arrivedRole());

            if (ACTIVITY_EATING.equals(target.activity()) && warehouse != null
                    && !fedThisMeal.contains(uuid)) {
                tickMeal(ref, warehouse, world, uuid);
            }

            if (isFarmerWorkingTarget && effectiveFarm != null) {
                // Hand the farmer off to its own fast (~0.3s) tick loop. start() is idempotent —
                // calling it every village tick just refreshes the warehouse/owner context.
                farmerWork.start(uuid, world, effectiveFarm, warehouse, currentOwnerUuid);
            } else {
                farmerWork.clear(uuid, world);
            }
        } else {
            // Still traveling — rebind marker every tick so Seek stays active
            ScheduleTarget originTarget = routeOrigin.get(uuid);
            if (originTarget == null) {
                originTarget = inferCurrentOrigin(store, ref, village,
                        houseTarget, effectiveFarm, warehouse, effectiveSawmill, effectiveMine,
                        effectiveGuardHouse, effectiveForge);
                routeOrigin.put(uuid, originTarget);
            }
            openRoute(uuid, originTarget, houseTarget, target, world);
            if (!isFarmerWorkingTarget) {
                farmerWork.clear(uuid, world);
            }
            boolean stuck = updateTravelProgress(uuid, store, ref, target);
            world.execute(() -> {
                Store<EntityStore> liveStore = world.getEntityStore().getStore();
                if (stuck) {
                    removeMarker(uuid, world);
                    LOGGER.info("scheduleTravelStuck uuid=" + uuid
                            + " activity=" + travelActivity(target.activity())
                            + " target=(" + String.format("%.2f,%.2f,%.2f", target.x(), target.y(), target.z()) + ")"
                            + " action=reissueRoute");
                    startTraveling(ref, liveStore, target, world, uuid);
                } else {
                    maintainTraveling(ref, liveStore, target, world, uuid);
                }
            });
        }
    }

    private void startTraveling(Ref<EntityStore> ref, Store<EntityStore> store,
                                ScheduleTarget target, World world, UUID uuid) {
        Vector3d dest = new Vector3d(target.x(), target.y(), target.z());

        // 1. Spawn/reposition marker before role switch so Seek target is ready immediately
        Ref<EntityStore> marker = ensureMarker(uuid, ref, store, world, dest);

        // 2. Set leash to destination so WanderInCircle pulls NPC there immediately,
        //    even if Seek isn't available yet.
        NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
        if (npcEntity != null) {
            npcEntity.setLeashPoint(dest);
        }

        // 3. Switch to traveling role (Seek → LockedTarget)
        switchRole(ref, store, ROLE_TRAVELING, world);

        // 4. After role switch is applied, bind marker as LockedTarget.
        //    RoleChangeSystem defers the role swap, so we schedule binding after it.
        if (marker != null) {
            final Ref<EntityStore> markerRef = marker;
            world.execute(() -> {
                NPCEntity liveNpc = store.getComponent(ref, NPCEntity.getComponentType());
                if (liveNpc != null && liveNpc.getRole() != null
                        && liveNpc.getRole().getMarkedEntitySupport() != null) {
                    liveNpc.getRole().getMarkedEntitySupport().setMarkedEntity("LockedTarget", markerRef);
                    liveNpc.setLeashPoint(dest);
                }
            });
        }
    }

    /**
     * Called every tick while NPC is still traveling — rebinds marker so Seek stays active.
     */
    private void maintainTraveling(Ref<EntityStore> ref, Store<EntityStore> store,
                                   ScheduleTarget target, World world, UUID uuid) {
        Vector3d dest = new Vector3d(target.x(), target.y(), target.z());
        Ref<EntityStore> marker = ensureMarker(uuid, ref, store, world, dest);
        if (marker != null) {
            NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
            if (npc != null && npc.getRole() != null && npc.getRole().getMarkedEntitySupport() != null) {
                npc.getRole().getMarkedEntitySupport().setMarkedEntity("LockedTarget", marker);
                npc.setLeashPoint(dest);
            }
        }
    }

    /**
     * Spawns or repositions the invisible marker entity that the Seek motion tracks.
     */
    private Ref<EntityStore> ensureMarker(UUID uuid, Ref<EntityStore> npcRef,
                                           Store<EntityStore> store, World world, Vector3d position) {
        Ref<EntityStore> existing = markerRefs.get(uuid);
        if (existing != null && existing.isValid()) {
            // Reuse — just move it
            TransformComponent tc = store.getComponent(existing, TransformComponent.getComponentType());
            if (tc != null) {
                tc.setPosition(position);
                return existing;
            }
        }

        // Spawn new invisible marker (Projectile + Intangible, no visuals)
        try {
            Holder holder = EntityStore.REGISTRY.newHolder();
            ProjectileComponent projectile = new ProjectileComponent("Projectile");
            holder.putComponent(ProjectileComponent.getComponentType(), projectile);
            holder.putComponent(TransformComponent.getComponentType(),
                    new TransformComponent(position, new Vector3f(0, 0, 0)));
            holder.ensureComponent(UUIDComponent.getComponentType());
            holder.ensureComponent(Intangible.getComponentType());
            holder.addComponent(NetworkId.getComponentType(),
                    new NetworkId(((EntityStore) world.getEntityStore().getStore().getExternalData())
                            .takeNextNetworkId()));
            projectile.initialize();

            Ref<EntityStore> markerRef = world.getEntityStore().getStore()
                    .addEntity(holder, AddReason.SPAWN);
            if (markerRef == null || !markerRef.isValid()) {
                LOGGER.warning("VillagerScheduler: failed to spawn marker for " + uuid);
                return null;
            }
            markerRefs.put(uuid, markerRef);
            return markerRef;
        } catch (Exception e) {
            LOGGER.warning("VillagerScheduler: marker spawn error: " + e.getMessage());
            return null;
        }
    }

    private void removeMarker(UUID uuid, World world) {
        Ref<EntityStore> marker = markerRefs.remove(uuid);
        if (marker != null && marker.isValid()) {
            try {
                world.getEntityStore().getStore().removeEntity(marker, RemoveReason.REMOVE);
            } catch (Exception ignored) {}
        }
    }

    private boolean isNearTarget(Store<EntityStore> store, Ref<EntityStore> ref, ScheduleTarget target) {
        TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
        if (tc == null) return false;
        Vector3d pos = tc.getPosition();
        return distanceSq(pos.getX(), pos.getY(), pos.getZ(), target.x(), target.y(), target.z()) <= ARRIVAL_RADIUS_SQ;
    }

    private void openRoute(UUID uuid, ScheduleTarget origin, ScheduleTarget house,
                           ScheduleTarget target, World world) {
        Set<GateKey> gates = new HashSet<>();
        collectGateKeys(gates, origin);
        collectGateKeys(gates, house);
        collectGateKeys(gates, target);
        activeRouteGates.put(uuid, gates);
        setGateState(origin, world, true);
        setGateState(house, world, true);
        setGateState(target, world, true);
    }

    private void closeRouteIfUnused(UUID uuid, World world) {
        Set<GateKey> gates = activeRouteGates.remove(uuid);
        if (gates == null || gates.isEmpty()) return;
        for (GateKey key : gates) {
            if (isGateInUse(key)) continue;
            Gate gate = knownGates.get(key);
            if (gate != null) setSingleGateState(gate, world, false);
        }
    }

    private boolean isGateInUse(GateKey key) {
        for (Set<GateKey> route : activeRouteGates.values()) {
            if (route.contains(key)) return true;
        }
        return false;
    }

    private void collectGateKeys(Set<GateKey> out, ScheduleTarget target) {
        if (target == null || !target.hasGate()) return;
        for (Gate gate : target.gates()) {
            GateKey key = gateKey(gate);
            out.add(key);
            knownGates.put(key, gate);
        }
    }

    private GateKey gateKey(Gate gate) {
        return new GateKey(gate.x(), gate.y(), gate.z());
    }

    private ScheduleTarget inferCurrentOrigin(Store<EntityStore> store, Ref<EntityStore> ref,
                                              VillageData village, ScheduleTarget houseTarget,
                                              BuildingRecord farm, BuildingRecord warehouse,
                                              BuildingRecord sawmill, BuildingRecord mine,
                                              BuildingRecord guardHouse, BuildingRecord forge) {
        TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
        if (tc == null) return null;
        Vector3d pos = tc.getPosition();
        ScheduleTarget best = null;
        double bestDist = Double.MAX_VALUE;

        bestDist = considerOrigin(pos, houseTarget, bestDist);
        if (bestDist != Double.MAX_VALUE) best = houseTarget;

        ScheduleTarget warehouseOrigin = warehouse != null ? warehouseTarget(warehouse) : null;
        double dist = considerOrigin(pos, warehouseOrigin, bestDist);
        if (dist < bestDist) { bestDist = dist; best = warehouseOrigin; }

        ScheduleTarget farmOrigin = farm != null ? farmTarget(farm) : null;
        dist = considerOrigin(pos, farmOrigin, bestDist);
        if (dist < bestDist) { bestDist = dist; best = farmOrigin; }

        ScheduleTarget sawmillOrigin = sawmill != null ? workTarget(sawmill, ROLE_LUMBERJACK) : null;
        dist = considerOrigin(pos, sawmillOrigin, bestDist);
        if (dist < bestDist) { bestDist = dist; best = sawmillOrigin; }

        ScheduleTarget mineOrigin = mine != null ? workTarget(mine, ROLE_MINER) : null;
        dist = considerOrigin(pos, mineOrigin, bestDist);
        if (dist < bestDist) { bestDist = dist; best = mineOrigin; }

        ScheduleTarget guardOrigin = guardHouse != null ? workTarget(guardHouse, ROLE_GUARD) : null;
        dist = considerOrigin(pos, guardOrigin, bestDist);
        if (dist < bestDist) { bestDist = dist; best = guardOrigin; }

        ScheduleTarget forgeOrigin = forge != null ? workTarget(forge, ROLE_BLACKSMITH) : null;
        dist = considerOrigin(pos, forgeOrigin, bestDist);
        if (dist < bestDist) { best = forgeOrigin; }

        return best;
    }

    private double considerOrigin(Vector3d pos, ScheduleTarget target, double bestDist) {
        if (pos == null || target == null) return bestDist;
        double dist = distanceSq(pos.getX(), pos.getY(), pos.getZ(), target.x(), target.y(), target.z());
        return dist < bestDist && dist <= 20.0 * 20.0 ? dist : bestDist;
    }

    private void resetTravelProgress(UUID uuid, Store<EntityStore> store, Ref<EntityStore> ref, ScheduleTarget target) {
        TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
        if (tc == null) {
            travelProgress.remove(uuid);
            return;
        }
        Vector3d pos = tc.getPosition();
        travelProgress.put(uuid, new TravelProgress(
                pos.getX(), pos.getY(), pos.getZ(),
                distanceSq(pos.getX(), pos.getY(), pos.getZ(), target.x(), target.y(), target.z()),
                System.currentTimeMillis()));
    }

    private boolean updateTravelProgress(UUID uuid, Store<EntityStore> store, Ref<EntityStore> ref, ScheduleTarget target) {
        TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
        if (tc == null) return false;
        Vector3d pos = tc.getPosition();
        double distSqToTarget = distanceSq(pos.getX(), pos.getY(), pos.getZ(), target.x(), target.y(), target.z());
        long now = System.currentTimeMillis();
        TravelProgress prev = travelProgress.get(uuid);
        if (prev == null) {
            travelProgress.put(uuid, new TravelProgress(pos.getX(), pos.getY(), pos.getZ(), distSqToTarget, now));
            return false;
        }

        double movedSq = distanceSq(prev.x(), prev.y(), prev.z(), pos.getX(), pos.getY(), pos.getZ());
        boolean madeProgress = distSqToTarget < prev.distanceSq() - TRAVEL_PROGRESS_EPS_SQ
                || movedSq > TRAVEL_MOVE_EPS_SQ;
        if (madeProgress) {
            travelProgress.put(uuid, new TravelProgress(pos.getX(), pos.getY(), pos.getZ(), distSqToTarget, now));
            return false;
        }
        if (now - prev.lastProgressAt() < TRAVEL_STUCK_MS) return false;
        travelProgress.put(uuid, new TravelProgress(pos.getX(), pos.getY(), pos.getZ(), distSqToTarget, now));
        return true;
    }

    private String describeTarget(ScheduleTarget target) {
        if (target == null) return "none";
        return target.activity() + "@("
                + String.format("%.2f,%.2f,%.2f", target.x(), target.y(), target.z()) + ")";
    }

    private String getCurrentRole(Store<EntityStore> store, Ref<EntityStore> ref) {
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc == null || npc.getRole() == null) return "";
        return npc.getRole().getRoleName();
    }

    private BuildingRecord findVillagerHouse(VillageData village, UUID villagerUuid) {
        for (BuildingRecord b : village.getBuildings()) {
            if (!b.isCompleted()) continue;
            if (!BuildingType.isResidential(b.getType())) continue;
            if (villagerUuid.equals(b.getAssignedVillagerId())) return b;
        }
        return null;
    }

    /**
     * Returns the completed work building of {@code type} this villager is assigned to.
     * Without this lookup, multiple farms (or sawmills/mines) of the same type all share
     * the village-wide "first completed of type" target — every farmer ends up at the same
     * farm regardless of which one assigned them.
     */
    private BuildingRecord findVillagerWorkplace(VillageData village, UUID villagerUuid, String type) {
        for (BuildingRecord b : village.getBuildings()) {
            if (!b.isCompleted()) continue;
            if (!type.equals(b.getType())) continue;
            if (villagerUuid.equals(b.getAssignedVillagerId())) return b;
        }
        return null;
    }

    private ScheduleTarget resolveTarget(UUID uuid, VillagerData data, String profession, VillageData village,
                                         BuildingRecord house, BuildingRecord farm,
                                         BuildingRecord warehouse, BuildingRecord sawmill,
                                         BuildingRecord mine, BuildingRecord guardHouse,
                                         BuildingRecord forge, double time24) {
        if (data.isStarving() && warehouse != null) return warehouseTarget(warehouse);

        boolean inMealWindow = inWindow(time24, LUNCH_START, LUNCH_END) || inWindow(time24, DINNER_START, DINNER_END);

        if (warehouse != null && eatingVillagers.contains(uuid)) {
            if (data.getHunger() > 0) {
                return warehouseTarget(warehouse);
            }
            eatingVillagers.remove(uuid);
            fedThisMeal.add(uuid);
        }

        if (data.isHungry() && warehouse != null && inMealWindow && !fedThisMeal.contains(uuid)) {
            return warehouseTarget(warehouse);
        }

        if (!inWindow(time24, WORK_START, WORK_END)) return homeTarget(house, village);

        if (VillagerData.PROF_FARMER.equals(profession) && farm != null) return farmTarget(farm);
        if (VillagerData.PROF_LUMBERJACK.equals(profession) && sawmill != null) return workTarget(sawmill, ROLE_LUMBERJACK);
        if (VillagerData.PROF_MASON.equals(profession) && mine != null) return workTarget(mine, ROLE_MINER);
        if (VillagerData.PROF_GUARD.equals(profession) && guardHouse != null) return workTarget(guardHouse, ROLE_GUARD);
        if (VillagerData.PROF_BLACKSMITH.equals(profession) && forge != null) return workTarget(forge, ROLE_BLACKSMITH);

        return homeTarget(house, village);
    }

    private ScheduleTarget homeTarget(BuildingRecord house, VillageData village) {
        if (house != null) {
            BuildingLayout.Layout layout = BuildingLayout.get(house.getType(), house.getVariant());
            int steps = layout.rotationSteps(house.getRotation());
            int[] center = rotateLocalOffset(layout.centerLX(), layout.floorLY(), layout.centerLZ(), steps);
            double targetY = house.getPosY() + layout.floorLY() + 1.0;
            if (layout.hasDoor()) {
                int[] door = rotateLocalOffset(layout.doorLX(), layout.doorLY(), layout.doorLZ(), steps);
                int doorRot = layout.doorWorldRotation(house.getRotation());
                return new ScheduleTarget(
                        house.getPosX() + center[0] + 0.5,
                        targetY,
                        house.getPosZ() + center[2] + 0.5,
                        ROLE_VILLAGER, ACTIVITY_RESTING,
                        house.getPosX() + door[0], house.getPosY() + door[1], house.getPosZ() + door[2],
                        doorRot,
                        layout.openBlock(), layout.closeBlock(),
                        gatesForLayout(layout, house, steps));
            }
            return new ScheduleTarget(
                    house.getPosX() + center[0] + 0.5,
                    targetY,
                    house.getPosZ() + center[2] + 0.5,
                    ROLE_VILLAGER, ACTIVITY_RESTING);
        }
        return new ScheduleTarget(
                village.getFoundingStoneX() + 0.5,
                village.getFoundingStoneY() + 1.0,
                village.getFoundingStoneZ() + 0.5,
                ROLE_VILLAGER, ACTIVITY_RESTING);
    }

    private ScheduleTarget workTarget(BuildingRecord building, String arrivedRole) {
        BuildingLayout.Layout layout = BuildingLayout.get(building.getType(), building.getVariant());
        int steps = layout.rotationSteps(building.getRotation());
        int[] center = rotateLocalOffset(layout.centerLX(), layout.floorLY(), layout.centerLZ(), steps);
        double targetY = building.getPosY() + layout.floorLY() + 1.0;
        LOGGER.info("workTarget [" + building.getType() + "] anchor=(" + building.getPosX() + "," + building.getPosY() + "," + building.getPosZ() + ")"
                + " rot=" + building.getRotation() + " steps=" + steps
                + " localCenter=(" + layout.centerLX() + "," + layout.centerLZ() + ")"
                + " worldCenter=(" + (building.getPosX() + center[0] + 0.5) + "," + targetY + "," + (building.getPosZ() + center[2] + 0.5) + ")"
                + " hasDoor=" + layout.hasDoor());
        if (layout.hasDoor()) {
            int[] door = rotateLocalOffset(layout.doorLX(), layout.doorLY(), layout.doorLZ(), steps);
            int doorRot = layout.doorWorldRotation(building.getRotation());
            LOGGER.info("workTarget [" + building.getType() + "] door local=("
                    + layout.doorLX() + "," + layout.doorLY() + "," + layout.doorLZ()
                    + ") world=(" + (building.getPosX() + door[0]) + ","
                    + (building.getPosY() + door[1]) + ","
                    + (building.getPosZ() + door[2]) + ") rot=" + doorRot
                    + " openBlock=" + layout.openBlock()
                    + " closeBlock=" + layout.closeBlock());
            return new ScheduleTarget(
                    building.getPosX() + center[0] + 0.5,
                    targetY,
                    building.getPosZ() + center[2] + 0.5,
                    arrivedRole, ACTIVITY_WORKING,
                    building.getPosX() + door[0], building.getPosY() + door[1], building.getPosZ() + door[2],
                    doorRot,
                    layout.openBlock(), layout.closeBlock(),
                    gatesForLayout(layout, building, steps));
        }
        return new ScheduleTarget(
                building.getPosX() + center[0] + 0.5,
                targetY,
                building.getPosZ() + center[2] + 0.5,
                arrivedRole, ACTIVITY_WORKING);
    }

    private ScheduleTarget farmTarget(BuildingRecord farm) {
        BuildingLayout.Layout layout = BuildingLayout.get(farm.getType(), farm.getVariant());
        int steps = layout.rotationSteps(farm.getRotation());
        int[] center = rotateLocalOffset(layout.centerLX(), layout.floorLY(), layout.centerLZ(), steps);
        double targetY = farm.getPosY() + layout.floorLY() + 1.0;
        if (layout.hasDoor()) {
            int[] door = rotateLocalOffset(layout.doorLX(), layout.doorLY(), layout.doorLZ(), steps);
            int doorRot = layout.doorWorldRotation(farm.getRotation());
            return new ScheduleTarget(
                    farm.getPosX() + center[0] + 0.5,
                    targetY,
                    farm.getPosZ() + center[2] + 0.5,
                    ROLE_FARMER, ACTIVITY_WORKING,
                    farm.getPosX() + door[0], farm.getPosY() + door[1], farm.getPosZ() + door[2],
                    doorRot,
                    layout.openBlock(), layout.closeBlock(),
                    gatesForLayout(layout, farm, steps));
        }
        return new ScheduleTarget(
                farm.getPosX() + center[0] + 0.5,
                targetY,
                farm.getPosZ() + center[2] + 0.5,
                ROLE_FARMER, ACTIVITY_WORKING);
    }

    /**
     * Rotates a prefab-local offset by the building's rotation (same CCW formula as PrefabLoader).
     * Returns {wx_offset, wy_offset, wz_offset} to add to the anchor world position.
     */
    private static int[] rotateLocalOffset(int lx, int ly, int lz, int rotation) {
        return dev.hearthbound.building.BuildingLayout.rotateLocalOffset(lx, ly, lz, rotation);
    }

    private static List<Gate> gatesForLayout(BuildingLayout.Layout layout,
                                             BuildingRecord building,
                                             int steps) {
        if (layout == null || !layout.hasDoor()) return List.of();
        return layout.doors().stream()
                .map(door -> {
                    int[] rotated = rotateLocalOffset(door.lx(), door.ly(), door.lz(), steps);
                    int rot = door.worldRotation(building.getRotation(), layout.anchorPrefabRotation());
                    return new Gate(
                            building.getPosX() + rotated[0],
                            building.getPosY() + rotated[1],
                            building.getPosZ() + rotated[2],
                            rot,
                            door.openBlock(),
                            door.closeBlock());
                })
                .toList();
    }

    private ScheduleTarget warehouseTarget(BuildingRecord warehouse) {
        BuildingLayout.Layout layout = BuildingLayout.get(warehouse.getType(), warehouse.getVariant());
        int steps = layout.rotationSteps(warehouse.getRotation());
        int[] center = rotateLocalOffset(layout.centerLX(), layout.floorLY(), layout.centerLZ(), steps);
        double targetY = warehouse.getPosY() + layout.floorLY() + 1.0;
        if (layout.hasDoor()) {
            int[] door = rotateLocalOffset(layout.doorLX(), layout.doorLY(), layout.doorLZ(), steps);
            int doorRot = layout.doorWorldRotation(warehouse.getRotation());
            return new ScheduleTarget(
                    warehouse.getPosX() + center[0] + 0.5,
                    targetY,
                    warehouse.getPosZ() + center[2] + 0.5,
                    ROLE_EATING, ACTIVITY_EATING,
                    warehouse.getPosX() + door[0], warehouse.getPosY() + door[1], warehouse.getPosZ() + door[2],
                    doorRot,
                    layout.openBlock(), layout.closeBlock(),
                    gatesForLayout(layout, warehouse, steps));
        }
        return new ScheduleTarget(
                warehouse.getPosX() + center[0] + 0.5,
                targetY,
                warehouse.getPosZ() + center[2] + 0.5,
                ROLE_EATING, ACTIVITY_EATING);
    }

    /**
     * Takes one random edible food item from the warehouse and restores part of hunger.
     * Called once per village tick while the villager is at the warehouse eating.
     * Deferred via world.execute() to avoid "Store is currently processing" — called from forEachChunk.
     */
    private void tickMeal(Ref<EntityStore> ref, BuildingRecord warehouse, World world, UUID uuid) {
        String foodId = WarehouseDepositor.withdrawRandomFood(world, warehouse, foodRandom);
        if (foodId == null) {
            eatingVillagers.remove(uuid);
            fedThisMeal.add(uuid);
            LOGGER.fine("tickMeal: no food in warehouse for " + uuid);
            return;
        }

        world.execute(() -> {
            if (!ref.isValid()) return;
            Store<EntityStore> liveStore = world.getEntityStore().getStore();
            VillagerData liveData = liveStore.getComponent(ref, VillagerData.getComponentType());
            if (liveData == null) return;
            liveData.setHunger(liveData.getHunger() - 20);
            liveStore.putComponent(ref, VillagerData.getComponentType(), liveData);
            HotbarUtil.setSlot0(world, uuid, foodId);
            playEatingAnimation(ref, liveStore, foodId);
            if (liveData.getHunger() <= 0) {
                eatingVillagers.remove(uuid);
                fedThisMeal.add(uuid);
            } else {
                eatingVillagers.add(uuid);
            }
            LOGGER.info("tickMeal: " + uuid + " ate " + foodId + " hunger=" + liveData.getHunger());
        });
    }

    private void playEatingAnimation(Ref<EntityStore> ref, Store<EntityStore> store, String foodId) {
        try {
            Item item = Item.getAssetMap().getAsset(foodId);
            if (item == null) return;
            String animationsId = item.getPlayerAnimationsId();
            if (animationsId == null || animationsId.isBlank()) return;
            ItemPlayerAnimations animations = ItemPlayerAnimations.getAssetMap().getAsset(animationsId);
            if (animations == null) return;
            AnimationUtils.playAnimation(ref, AnimationSlot.Action, animations, "Consume", store);
        } catch (RuntimeException e) {
            LOGGER.fine("tickMeal: failed to play consume animation for " + foodId + ": " + e.getMessage());
        }
    }

    private void switchRole(Ref<EntityStore> ref, Store<EntityStore> store, String roleName, World world) {
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc == null || npc.getRole() == null) return;
        if (roleName.equals(npc.getRole().getRoleName())) return;
        int idx = NPCPlugin.get().getIndex(roleName);
        if (idx == Integer.MIN_VALUE) {
            LOGGER.warning("VillagerScheduler: role not registered: " + roleName);
            return;
        }
        String oldRoleName = npc.getRole().getRoleName();
        RoleChangeSystem.requestRoleChange(ref, npc.getRole(), idx, false, store);
        // RoleChangeSystem clears the Interactions component asynchronously, so the
        // F-key UI binding (InteractionId="StayedVillager") is lost on every role
        // swap unless we re-apply it. Skin/profession item are re-applied on the
        // same retry schedule.
        UUID uuid = NpcManager.extractUuid(store, ref);
        if (uuid != null) {
            NpcRegistry.NpcRecord record = NpcRegistry.get().getRecord(uuid);
            if (record != null) {
                LOGGER.info("[ROLEBIND] uuid=" + uuid + " " + oldRoleName + "→" + roleName
                        + " scheduling restoreAfterRoleChange interaction=" + record.interaction);
                NpcRestorer.restoreAfterRoleChange(ref, world, record);
            } else {
                LOGGER.warning("[ROLEBIND] uuid=" + uuid + " " + oldRoleName + "→" + roleName
                        + " NO REGISTRY RECORD — interactions will not be restored");
            }
        } else {
            LOGGER.warning("[ROLEBIND] " + oldRoleName + "→" + roleName
                    + " NO UUID — interactions will not be restored");
        }
        // After role change, re-equip profession item — RoleChangeSystem is async,
        // so delay to let the new role settle before touching inventory.
        TickScheduler.getExecutor().schedule(() ->
            world.execute(() -> {
                if (!ref.isValid()) return;
                NpcRestorer.equipProfessionItem(ref, world.getEntityStore().getStore(), roleName);
            }),
            500L, TimeUnit.MILLISECONDS
        );
    }

    private void setLeashPoint(Ref<EntityStore> ref, Store<EntityStore> store, Vector3d pos) {
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc != null) npc.setLeashPoint(pos);
    }

    private void setGateState(ScheduleTarget target, World world, boolean open) {
        if (target == null || !target.hasGate()) return;
        for (Gate gate : target.gates()) {
            setSingleGateState(gate, world, open);
        }
    }

    private void setSingleGateState(Gate gate, World world, boolean open) {
        int gx = gate.x(), gy = gate.y(), gz = gate.z();
        int gateRotation = gate.rotation();
        // Use a single-door prefab + rotate() so the engine places state-variant with correct rotation.
        // world.setBlock("*...CloseDoorIn") resets rotation to 0; chunk.setBlock + setBlock combo
        // also loses rotation. Only BlockSelection.placeNoReturn preserves both state and rotation.
        String stateBlock = open ? gate.openBlock() : gate.closeBlock();
        boolean doorOut = stateBlock != null && stateBlock.contains("DoorOut");
        String prefabName = open
                ? (doorOut ? "door_open_out" : "door_open_in")
                : (doorOut ? "door_closed_out" : "door_closed_in");
        world.execute(() -> {
            try {
                com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection selection =
                        com.hypixel.hytale.server.core.prefab.PrefabStore.get()
                                .getAssetPrefabFromAnyPack(prefabName + ".prefab.json");
                // door_*_in.prefab.json has door at rotation=0; rotate gateRotation steps.
                int angleDeg = (gateRotation * 90) % 360;
                com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection rotated =
                        angleDeg == 0 ? selection : selection.rotate(com.hypixel.hytale.math.Axis.Y, angleDeg);
                // Door base block is at (0,0,0) in the prefab; Y-rotation keeps it there.
                rotated.setAnchorAtWorldPos(0, 0, 0);
                com.hypixel.hytale.math.vector.Vector3i gatePos =
                        new com.hypixel.hytale.math.vector.Vector3i(gx, gy, gz);
                Store<EntityStore> liveStore = world.getEntityStore().getStore();
                rotated.placeNoReturn(world, gatePos, liveStore);
                LOGGER.info("setGateState: prefab=" + prefabName
                        + " stateBlock=" + stateBlock
                        + " rot=" + gateRotation
                        + " at (" + gx + "," + gy + "," + gz + ")");
            } catch (Exception e) {
                LOGGER.warning("setGateState failed: " + e.getMessage());
            }
        });
    }

    private double getTime24(Store<EntityStore> store) {
        try {
            WorldTimeResource resource = (WorldTimeResource)
                    store.getResource(WorldTimeResource.getResourceType());
            if (resource == null) return -1;
            LocalDateTime dt = resource.getGameDateTime();
            return dt.getHour() + dt.getMinute() / 60.0;
        } catch (Exception e) {
            return -1;
        }
    }

    private boolean inWindow(double time24, double start, double end) {
        if (start < end) return time24 >= start && time24 < end;
        return time24 >= start || time24 < end;
    }

    private double distanceSq(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }

    private String travelActivity(String arrived) {
        return switch (arrived) {
            case ACTIVITY_WORKING -> ACTIVITY_GOING_TO_WORK;
            case ACTIVITY_EATING  -> ACTIVITY_GOING_TO_EAT;
            case ACTIVITY_RESTING -> ACTIVITY_GOING_HOME;
            default               -> arrived;
        };
    }

    public String getActivityLabel(UUID uuid) {
        return activityLabel.get(uuid);
    }

    public void clearVillager(UUID uuid) {
        lastTarget.remove(uuid);
        activityLabel.remove(uuid);
        markerRefs.remove(uuid);
    }
}
