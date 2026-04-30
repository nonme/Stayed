package dev.hearthbound.npc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

    // Per-villager last scheduled target (to detect changes)
    private final Map<UUID, ScheduleTarget> lastTarget = new HashMap<>();
    // Per-villager marker entity ref (invisible Seek target)
    private final Map<UUID, Ref<EntityStore>> markerRefs = new HashMap<>();
    // Per-villager current activity label for UI
    private final Map<UUID, String> activityLabel = new HashMap<>();
    // Villagers already fed during the current meal window (lunch or dinner)
    // Cleared when the meal window ends, preventing repeated feedVillager() calls.
    private final Set<UUID> fedThisMeal = new HashSet<>();

    public static final String ACTIVITY_GOING_TO_WORK = "Going to work";
    public static final String ACTIVITY_WORKING        = "Working";
    public static final String ACTIVITY_GOING_TO_EAT  = "Going to eat";
    public static final String ACTIVITY_EATING         = "Eating";
    public static final String ACTIVITY_GOING_HOME     = "Going home";
    public static final String ACTIVITY_RESTING        = "Resting";


    // null gateX means "no gate/door to manage"
    private record ScheduleTarget(double x, double y, double z, String arrivedRole, String activity,
                                   Integer gateX, Integer gateY, Integer gateZ,
                                   int gateRotation,
                                   String openBlock, String closeBlock) {
        ScheduleTarget(double x, double y, double z, String arrivedRole, String activity) {
            this(x, y, z, arrivedRole, activity, null, null, null, 0, null, null);
        }
        boolean hasGate() { return gateX != null; }
    }

    public void tick(Store<EntityStore> store, Ref<EntityStore> playerRef, VillageData village, World world) {
        double time24 = getTime24(store);
        if (time24 < 0) return;

        // Clear fed-tracking between meal windows so villagers can eat again next meal.
        if (!inWindow(time24, LUNCH_START, LUNCH_END) && !inWindow(time24, DINNER_START, DINNER_END)) {
            fedThisMeal.clear();
        }

        BuildingRecord farm      = VillageManager.get().findCompletedFarm(village);
        BuildingRecord warehouse = VillageManager.get().findCompletedWarehouse(village);
        BuildingRecord sawmill   = VillageManager.get().findCompletedSawmill(village);
        BuildingRecord mine      = VillageManager.get().findCompletedMine(village);

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
                        tickVillager(ref, store, data, village, farm, warehouse, sawmill, mine, time24, world);
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
                              double time24, World world) {
        UUID uuid = NpcManager.extractUuid(store, ref);
        if (uuid == null) return;

        BuildingRecord house = findVillagerHouse(village, uuid);
        VillagerSummary summary = VillageManager.get().findVillagerSummary(village, uuid);
        String profession = summary != null ? summary.getProfession() : VillagerData.PROF_NONE;

        ScheduleTarget target = resolveTarget(uuid, data, profession, village, house, farm, warehouse, sawmill, mine, time24);
        if (target == null) return;

        ScheduleTarget prev = lastTarget.get(uuid);
        boolean targetChanged = prev == null
                || distanceSq(prev.x(), prev.y(), prev.z(), target.x(), target.y(), target.z()) > ARRIVAL_RADIUS_SQ;

        // Check if villager has arrived at the current target
        boolean arrived = !targetChanged && isNearTarget(store, ref, target);

        ScheduleTarget houseTarget = house != null ? homeTarget(house, village) : null;

        if (targetChanged) {
            // Always open house door when NPC starts traveling (leaving or arriving)
            setGateState(houseTarget, world, true);
            lastTarget.put(uuid, target);
            activityLabel.put(uuid, travelActivity(target.activity()));
            world.execute(() -> {
                Store<EntityStore> liveStore = world.getEntityStore().getStore();
                startTraveling(ref, liveStore, target, world, uuid);
            });
        } else if (arrived) {
            switchRole(ref, store, target.arrivedRole(), world);
            setLeashPoint(ref, store, new Vector3d(target.x(), target.y(), target.z()));
            removeMarker(uuid, world);
            activityLabel.put(uuid, target.activity());
            setGateState(houseTarget, world, false);

            if (ACTIVITY_EATING.equals(target.activity()) && warehouse != null
                    && !fedThisMeal.contains(uuid)) {
                fedThisMeal.add(uuid);
                feedVillager(ref, warehouse, world, uuid);
            }
        } else {
            // Still traveling — rebind marker every tick so Seek stays active
            world.execute(() -> {
                Store<EntityStore> liveStore = world.getEntityStore().getStore();
                maintainTraveling(ref, liveStore, target, world, uuid);
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

    private ScheduleTarget resolveTarget(UUID uuid, VillagerData data, String profession, VillageData village,
                                         BuildingRecord house, BuildingRecord farm,
                                         BuildingRecord warehouse, BuildingRecord sawmill,
                                         BuildingRecord mine, double time24) {
        if (data.isStarving() && warehouse != null) return warehouseTarget(warehouse);

        boolean inMealWindow = inWindow(time24, LUNCH_START, LUNCH_END) || inWindow(time24, DINNER_START, DINNER_END);

        // Keep villager at warehouse for the full meal window, even after hunger reset.
        // Without this, hunger=0 after feedVillager → next tick resolves a different target → NPC leaves immediately.
        if (inMealWindow && warehouse != null && fedThisMeal.contains(uuid)) {
            return warehouseTarget(warehouse);
        }

        if (data.isHungry() && warehouse != null && inMealWindow) {
            return warehouseTarget(warehouse);
        }

        if (!inWindow(time24, WORK_START, WORK_END)) return homeTarget(house, village);

        if (VillagerData.PROF_FARMER.equals(profession) && farm != null) return farmTarget(farm);
        if (VillagerData.PROF_LUMBERJACK.equals(profession) && sawmill != null) return workTarget(sawmill, ROLE_LUMBERJACK);
        if (VillagerData.PROF_MASON.equals(profession) && mine != null) return workTarget(mine, ROLE_MINER);

        return homeTarget(house, village);
    }

    private ScheduleTarget homeTarget(BuildingRecord house, VillageData village) {
        if (house != null) {
            BuildingLayout.Layout layout = BuildingLayout.get(house.getType());
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
                        layout.openBlock(), layout.closeBlock());
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
        BuildingLayout.Layout layout = BuildingLayout.get(building.getType());
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
            return new ScheduleTarget(
                    building.getPosX() + center[0] + 0.5,
                    targetY,
                    building.getPosZ() + center[2] + 0.5,
                    arrivedRole, ACTIVITY_WORKING,
                    building.getPosX() + door[0], building.getPosY() + door[1], building.getPosZ() + door[2],
                    doorRot,
                    layout.openBlock(), layout.closeBlock());
        }
        return new ScheduleTarget(
                building.getPosX() + center[0] + 0.5,
                targetY,
                building.getPosZ() + center[2] + 0.5,
                arrivedRole, ACTIVITY_WORKING);
    }

    private ScheduleTarget farmTarget(BuildingRecord farm) {
        BuildingLayout.Layout layout = BuildingLayout.get(farm.getType());
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
                    layout.openBlock(), layout.closeBlock());
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

    private ScheduleTarget warehouseTarget(BuildingRecord warehouse) {
        BuildingLayout.Layout layout = BuildingLayout.get(warehouse.getType());
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
                    layout.openBlock(), layout.closeBlock());
        }
        return new ScheduleTarget(
                warehouse.getPosX() + center[0] + 0.5,
                targetY,
                warehouse.getPosZ() + center[2] + 0.5,
                ROLE_EATING, ACTIVITY_EATING);
    }

    /**
     * Takes one food item from the warehouse, equips it in the NPC's hand, and resets hunger to 0.
     * Called once when the villager arrives at the warehouse to eat.
     * Deferred via world.execute() to avoid "Store is currently processing" — called from forEachChunk.
     */
    private void feedVillager(Ref<EntityStore> ref, BuildingRecord warehouse, World world, UUID uuid) {
        String foodId = WarehouseDepositor.withdrawFood(world, warehouse);
        if (foodId == null) {
            LOGGER.fine("feedVillager: no food in warehouse for " + uuid);
            return;
        }

        // Defer store write — we are inside forEachChunk, can't call putComponent directly.
        // Reset hunger — food display is handled by ROLE_EATING JSON StateTransitions (SetHotbar + EquipHotbar)
        world.execute(() -> {
            if (!ref.isValid()) return;
            Store<EntityStore> liveStore = world.getEntityStore().getStore();
            VillagerData liveData = liveStore.getComponent(ref, VillagerData.getComponentType());
            if (liveData == null) return;
            liveData.setHunger(0);
            liveStore.putComponent(ref, VillagerData.getComponentType(), liveData);
            LOGGER.fine("feedVillager: " + uuid + " ate " + foodId);
        });
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
        RoleChangeSystem.requestRoleChange(ref, npc.getRole(), idx, false, store);
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
        int gx = target.gateX(), gy = target.gateY(), gz = target.gateZ();
        int gateRotation = target.gateRotation();
        // Use a single-door prefab + rotate() so the engine places state-variant with correct rotation.
        // world.setBlock("*...CloseDoorIn") resets rotation to 0; chunk.setBlock + setBlock combo
        // also loses rotation. Only BlockSelection.placeNoReturn preserves both state and rotation.
        String prefabName = open ? "door_open_in" : "door_closed_in";
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
                LOGGER.fine("setGateState: prefab=" + prefabName + " rot=" + gateRotation + " at (" + gx + "," + gy + "," + gz + ")");
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
