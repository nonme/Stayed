package dev.hearthbound.events;

import com.hypixel.hytale.builtin.adventure.objectives.Objective;
import com.hypixel.hytale.builtin.adventure.objectives.ObjectivePlugin;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.npc.HearthboundDataStore;
import dev.hearthbound.npc.NpcManager;
import dev.hearthbound.npc.VillagerScheduler;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.npc.NpcRestorer;
import dev.hearthbound.npc.StayedRoleChangeApplier;
import dev.hearthbound.npc.StayedRoleNames;
import dev.hearthbound.quest.RescueQuestManager;
import dev.hearthbound.ui.RescueDialogPage;
import dev.hearthbound.ui.VillageHud;
import dev.hearthbound.util.TickScheduler;
import dev.hearthbound.building.ResourceProducer;
import dev.hearthbound.building.WarehouseDepositor;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;
import dev.hearthbound.village.VillagerData;
import dev.hearthbound.village.VillagerSummary;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

/**
 * Periodic handler for village lifecycle events:
 * - Converting any live rescue followers into villagers (no proximity check —
 *   a follower has already agreed to join, position doesn't matter)
 * - Updating the village HUD
 *
 * NPC skin restoration on chunk load is handled by NpcChunkLoadHandler.
 */
public class VillageTickHandler {

    private static final Logger LOGGER = Logger.getLogger(VillageTickHandler.class.getName());
    private static final long TICK_INTERVAL_MS = 5000;
    private static final double WORK_START = 6.0;
    private static final double WORK_END   = 20.0;

    private final Random rng = new Random();
    private static final String VILLAGER_ROLE = "Villager_Human";
    private static final String FOLLOWER_ROLE = "Villager_Rescue_Follower";
    private static final String VICTIM_ROLE   = "Villager_Rescue_Trapped";

    private ScheduledFuture<?> tickTask;
    private VillageHud hud;
    private final VillagerScheduler villagerScheduler = new VillagerScheduler();

    private Ref<EntityStore> lastPlayerRef;
    private World lastWorld;

    public void start(Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
        stop();
        this.lastPlayerRef = playerRef;
        this.lastWorld = world;

        tickTask = TickScheduler.runRepeating(world, TICK_INTERVAL_MS, TICK_INTERVAL_MS, () -> {
            Store<EntityStore> liveStore = world.getEntityStore().getStore();
            tick(liveStore, playerRef, world);
        });

        LOGGER.info("VillageTickHandler started");
    }

    /**
     * Synchronously runs one full village tick using the most recent player/world
     * pair captured by {@link #start}. Intended for the integration test framework
     * (ForceVillageTickStep) where we need to fast-forward the housing/role
     * assignment loop instead of waiting out the 5 s scheduler interval.
     *
     * Must be called on the world thread (the test runner already schedules its
     * steps via {@code world.execute(...)} so this is satisfied).
     */
    public void runOneTickNow() {
        if (lastPlayerRef == null || lastWorld == null) return;
        if (!lastPlayerRef.isValid()) return;
        Store<EntityStore> liveStore = lastWorld.getEntityStore().getStore();
        tick(liveStore, lastPlayerRef, lastWorld);
    }

    public void stop() {
        if (tickTask != null) {
            tickTask.cancel(false);
            tickTask = null;
        }
    }

    public VillageHud getHud() {
        return hud;
    }

    public VillagerScheduler getVillagerScheduler() {
        return villagerScheduler;
    }

    private void tick(Store<EntityStore> store, Ref<EntityStore> playerRef, World world) {
        if (!playerRef.isValid()) {
            stop();
            return;
        }

        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village == null || !village.isFounded()) return;

        convertAllFollowers(store, playerRef, village, world);
        int reconciled = VillageManager.get().reconcileNpcReferences(store, playerRef, village, world);
        if (reconciled > 0) {
            village = VillageManager.get().getVillageData(store, playerRef);
            if (village == null) return;
        }
        // Free buildings whose assigned villager is permanently lost. Runs
        // before the assign* passes so a freed slot can immediately be picked
        // up by another villager on the same tick — no empty house or
        // workplace lingers when there's a candidate available.
        cleanupOrphanedAssignments(store, playerRef, village, world);
        // Re-read in case cleanup mutated village state via save().
        village = VillageManager.get().getVillageData(store, playerRef);
        if (village == null) return;
        assignHomelessVillagers(store, playerRef, village, world);
        assignUnstaffedFarms(store, playerRef, village);
        assignUnstaffedSawmills(store, playerRef, village);
        assignUnstaffedMines(store, playerRef, village);
        assignUnstaffedGuardHouses(store, playerRef, village);
        assignUnstaffedForges(store, playerRef, village);
        tickHunger(store, village);
        tickProduction(store, playerRef, village, world);
        villagerScheduler.tick(store, playerRef, village, world);
    }

    private void convertAllFollowers(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                     VillageData village, World world) {
        // Don't convert while the player is still walking back — wait for Return objective to finish
        if (isReturnObjectiveActive(store, playerRef)) return;

        // Walk RESCUE/FOLLOWER records. We only act on entities whose chunk is
        // currently loaded — a follower in an unloaded chunk will be picked up
        // by NpcChunkLoadHandler the moment its chunk reloads, no need for an
        // active force-load here.
        for (NpcRegistry.NpcRecord record : NpcRegistry.get().allRecords()) {
            if (record.interaction != NpcRegistry.InteractionType.RESCUE
                    && record.interaction != NpcRegistry.InteractionType.FOLLOWER) continue;
            if (record.entityUuid == null) continue;

            Ref<EntityStore> ref = world.getEntityRef(record.entityUuid);
            if (ref == null || !ref.isValid()) continue;

            NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
            if (npcEntity == null) continue;

            String currentRole = StayedRoleNames.extractBaseRoleName(npcEntity.getRoleName());

            if (FOLLOWER_ROLE.equals(currentRole)) {
                VillageData liveVillage = VillageManager.get().getVillageData(store, playerRef);
                if (liveVillage == null) continue;
                convertFollowerToVillager(store, playerRef, ref, liveVillage, world);

            } else if (VICTIM_ROLE.equals(currentRole) && FOLLOWER_ROLE.equals(record.baseRoleName())) {
                // RoleChangeSystem hasn't applied yet (player flew away right after dialog).
                // Registry already says Follower → re-issue the role change.
                StayedRoleChangeApplier.applyOrSchedule(ref, store, world, record,
                        false, "convert-followers-reapply");
                LOGGER.info("convertAllFollowers: ensured Trapped->Follower role change for uuid="
                        + record.entityUuid);
            }
        }
    }

    /** Returns true while Objective_RescueTrap_Return is still in the player's active set. */
    private boolean isReturnObjectiveActive(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) return false;
        var activeUuids = player.getPlayerConfigData().getActiveObjectiveUUIDs();
        if (activeUuids == null || activeUuids.isEmpty()) return false;
        for (UUID uuid : activeUuids) {
            Objective obj = ObjectivePlugin.get().getObjectiveDataStore().getObjective(uuid);
            if (obj != null && RescueDialogPage.OBJECTIVE_RETURN_ID.equals(obj.getObjectiveId())) {
                return true;
            }
        }
        return false;
    }

    private void convertFollowerToVillager(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                           Ref<EntityStore> followerRef, VillageData village, World world) {
        try {
            NPCEntity npcEntity = store.getComponent(followerRef, NPCEntity.getComponentType());
            if (npcEntity == null) return;

            RescueQuestManager.unregisterNpc(followerRef);

            // Save village data immediately — before the deferred moveTo
            VillagerData villagerData = store.getComponent(followerRef, VillagerData.getComponentType());
            UUID followerUuid = NpcManager.extractUuid(store, followerRef);
            VillagerSummary summary = villagerData != null
                    ? new VillagerSummary(villagerData)
                    : new VillagerSummary();
            summary.setVillagerUuid(followerUuid);
            village.addVillager(summary);
            village.setRescueQuestStarted(false);
            VillageManager.get().save(store, playerRef, village);

            // Update NpcRegistry to reflect the new role and no interaction, then persist.
            // Without this, after a server restart NpcRestorer would re-apply the RESCUE
            // interaction to a villager, and the saved role name would still say Follower.
            if (followerUuid != null) {
                NpcRegistry.NpcRecord oldRecord = NpcRegistry.get().getRecord(followerUuid);
                String npcId = oldRecord != null ? oldRecord.npcId : UUID.randomUUID().toString();
                long skinSeed = oldRecord != null ? oldRecord.skinSeed : 0L;
                long chunkIndex = oldRecord != null ? oldRecord.chunkIndex : 0L;
                NpcRegistry.NpcRecord newRecord = new NpcRegistry.NpcRecord(
                        npcId, followerUuid, VILLAGER_ROLE, NpcRegistry.InteractionType.VILLAGER, skinSeed, chunkIndex);
                if (oldRecord != null && oldRecord.hasPosition) {
                    newRecord.setPosition(oldRecord.lastX, oldRecord.lastY, oldRecord.lastZ);
                }
                // changeAppearance=false — keep the PlayerSkin assigned at rescue time.
                StayedRoleChangeApplier.persistAndApply(followerRef, store, world, newRecord,
                        false, "follower-to-villager");
            }

            LOGGER.info(() -> "Rescue follower converted to villager (total: " + village.getVillagerCount() + ")");

            // On the next tick (after RoleChangeSystem applies the new role):
            // - if the villager is far from the village, teleport them next to the player
            //   (player just returned, so they're standing in open space — safe to land near)
            // - always update leashPoint to the founding stone so WanderInCircle stays in village
            double leashX = village.getFoundingStoneX() + 0.5;
            double leashY = village.getFoundingStoneY() + 0.1;
            double leashZ = village.getFoundingStoneZ() + 0.5;

            world.execute(() -> {
                if (followerUuid == null) return;
                Store<EntityStore> liveStore = world.getEntityStore().getStore();
                Entity entity = world.getEntity(followerUuid);
                if (entity == null || !followerRef.isValid()) return;

                var tcType = com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType();
                var followerTc = liveStore.getComponent(followerRef, tcType);
                boolean farFromVillage = true;
                if (followerTc != null) {
                    var pos = followerTc.getPosition();
                    double dx = pos.getX() - leashX;
                    double dz = pos.getZ() - leashZ;
                    farFromVillage = dx * dx + dz * dz > 40.0 * 40.0;
                }

                if (farFromVillage) {
                    // Land next to the player — they're standing in open space near the village
                    var playerTc = liveStore.getComponent(playerRef, tcType);
                    if (playerTc != null) {
                        var playerPos = playerTc.getPosition();
                        entity.moveTo(followerRef,
                                playerPos.getX() + 2.0, playerPos.getY(), playerPos.getZ() + 2.0,
                                liveStore);
                        LOGGER.fine("Teleported new villager next to player (was far from village)");
                    }
                }

                // Always anchor wander around the founding stone
                NPCEntity liveNpcEntity = liveStore.getComponent(followerRef, NPCEntity.getComponentType());
                if (liveNpcEntity != null) {
                    liveNpcEntity.setLeashPoint(new Vector3d(leashX, leashY, leashZ));
                }

                // Re-apply interaction and skin after role change — RoleChangeSystem applies
                // the new role asynchronously and resets the Interactions component.
                // restoreAfterRoleChange() defers applyInteraction() so it runs after the
                // role is fully applied, ensuring our HearthboundVillager handler wins.
                NpcRegistry.NpcRecord liveRecord = NpcRegistry.get().getRecord(followerUuid);
                if (liveRecord != null) {
                    NpcRestorer.restoreAfterRoleChange(followerRef, world, liveRecord);
                }
            });

        } catch (Exception e) {
            LOGGER.warning("convertFollowerToVillager failed: " + e.getMessage());
        }
    }

    // Hunger increase per tick (every 5 seconds)
    private static final int HUNGER_PER_TICK = 1;

    /**
     * Increases hunger for every registered villager and saves updated VillagerData.
     * Iterates over NPCEntity chunks to find live villagers by UUID.
     * Profession is read from VillagerSummary (VillageData BSON), not VillagerData component
     * — VillagerData.getProfession() always returns PROF_NONE on live entities.
     */
    private void tickHunger(Store<EntityStore> store, VillageData village) {
        Archetype<EntityStore> query = Archetype.of(NPCEntity.getComponentType());
        store.forEachChunk(query, (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                try {
                    Ref<EntityStore> ref = chunk.getReferenceTo(i);
                    VillagerData data = store.getComponent(ref, VillagerData.getComponentType());
                    if (data == null) continue;

                    UUID uuid = NpcManager.extractUuid(store, ref);
                    if (uuid == null) continue;
                    VillagerSummary summary = VillageManager.get().findVillagerSummary(village, uuid);
                    if (summary == null) continue;

                    // Farmers eat what they grow — keep them at 0 hunger always
                    if (VillagerData.PROF_FARMER.equals(summary.getProfession())) {
                        if (data.getHunger() > 0) {
                            data.setHunger(0);
                            store.putComponent(ref, VillagerData.getComponentType(), data);
                        }
                        continue;
                    }
                    data.setHunger(data.getHunger() + HUNGER_PER_TICK);
                    store.putComponent(ref, VillagerData.getComponentType(), data);
                } catch (Exception ignored) {}
            }
        });
    }

    /**
     * For each completed production building with an assigned worker, during work hours,
     * rolls a chance to produce one item and deposits it into the warehouse.
     */
    private void tickProduction(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                VillageData village, World world) {
        double time24 = getTime24(store);
        if (time24 < 0) return;
        if (time24 < WORK_START || time24 >= WORK_END) return;

        BuildingRecord warehouse = VillageManager.get().findCompletedWarehouse(village);
        if (warehouse == null) {
            LOGGER.info("tickProduction: no completed warehouse, skipping");
            return;
        }

        for (BuildingRecord building : village.getBuildings()) {
            if (!building.isCompleted()) continue;
            if (building.getAssignedVillagerId() == null) continue;

            String type = building.getType();
            String itemId = ResourceProducer.roll(type, rng);
            LOGGER.info("tickProduction: " + type + " assigned=" + building.getAssignedVillagerId()
                    + " rolled=" + (itemId != null ? itemId : "nothing"));
            if (itemId == null) continue;

            boolean deposited = WarehouseDepositor.deposit(world, warehouse, itemId);
            LOGGER.info("tickProduction: deposit " + itemId + " → " + (deposited ? "OK" : "FAILED/FULL"));
        }
    }

    private double getTime24(Store<EntityStore> store) {
        try {
            com.hypixel.hytale.server.core.modules.time.WorldTimeResource resource =
                    (com.hypixel.hytale.server.core.modules.time.WorldTimeResource)
                    store.getResource(com.hypixel.hytale.server.core.modules.time.WorldTimeResource.getResourceType());
            if (resource == null) return -1;
            java.time.LocalDateTime dt = resource.getGameDateTime();
            return dt.getHour() + dt.getMinute() / 60.0;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Frees any building whose assignedVillagerId points to a villager that is
     * permanently lost — either no NpcRegistry record at all (the registry
     * entry was wiped or never created) or a record whose circuit breaker has
     * tripped (broken=true). The reconcile path keeps trying to recover live
     * NPCs, so we only free assignments we know won't come back.
     *
     * Also detects a separate kind of damage: the entity is alive but missing
     * its VillagerData component (which can happen if a respawn ran on an
     * older code path that didn't restore the component). The scheduler
     * silently skips villagers without VillagerData, so they stand idle
     * forever. We re-attach the component from VillagerSummary so they wake
     * up on the next scheduler tick.
     *
     * Both work buildings and houses are touched: a freed work slot is filled
     * by assignUnstaffed* on the same tick, a freed house by
     * assignHomelessVillagers on the next iteration of the same tick. The
     * VillagerSummary stays in place so the player's resident count doesn't
     * shrink — we only mark the post empty.
     */
    private void cleanupOrphanedAssignments(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                            VillageData village, World world) {
        boolean anyFreed = false;
        java.util.HashSet<UUID> seen = new java.util.HashSet<>();
        for (BuildingRecord building : village.getBuildings()) {
            UUID assigned = building.getAssignedVillagerId();
            if (assigned == null) continue;
            NpcRegistry.NpcRecord record = VillageManager.get().findVillagerRecord(village, assigned);
            if (record != null && record.entityUuid != null && !assigned.equals(record.entityUuid)) {
                int rewritten = VillageManager.get().replaceVillagerUuid(village, assigned, record.entityUuid);
                if (rewritten > 0) {
                    assigned = record.entityUuid;
                    anyFreed = true;
                    LOGGER.info("cleanupOrphanedAssignments: rewrote stale assignment refs to "
                            + assigned + " (" + rewritten + " refs)");
                }
            }
            boolean orphaned = record == null || record.broken;
            if (orphaned) {
                String reason = record == null ? "no registry entry" : "broken=true";
                LOGGER.info("cleanupOrphanedAssignments: freeing " + building.getType() + " at "
                        + building.getPosX() + "," + building.getPosY() + "," + building.getPosZ()
                        + " (assigned=" + assigned + ", " + reason + ")");
                building.setAssignedVillagerId(null);
                if (!BuildingType.isResidential(building.getType()) && record == null) {
                    VillagerSummary summary = VillageManager.get().findVillagerSummary(village, assigned);
                    if (summary != null) summary.setProfession(VillagerData.PROF_NONE);
                }
                anyFreed = true;
                continue;
            }
            // Self-heal: a villager whose entity is alive but is missing the
            // VillagerData component sits idle forever (the scheduler skips it).
            // Re-attach the component from VillagerSummary so they wake up on
            // the next scheduler tick. dedupe by UUID — a villager assigned to
            // both a house and a workplace would otherwise be checked twice.
            if (seen.add(assigned)) {
                healVillagerDataIfMissing(store, world, assigned, village);
            }
        }
        if (anyFreed) {
            VillageManager.get().save(store, playerRef, village);
        }
    }

    /**
     * Re-attaches VillagerData to a live villager that lost the component.
     * Uses VillagerSummary as the source of truth for race/profession/name and
     * NpcRegistry for the skin seed. No-op if the entity isn't loaded right
     * now, if VillagerData is already present, or if the summary is missing.
     */
    private void healVillagerDataIfMissing(Store<EntityStore> store, World world, UUID villagerUuid,
                                            VillageData village) {
        Ref<EntityStore> ref = world.getEntityRef(villagerUuid);
        if (ref == null || !ref.isValid()) return;
        VillagerData existing = store.getComponent(ref, VillagerData.getComponentType());
        if (existing != null) return;

        VillagerSummary summary = VillageManager.get().findVillagerSummary(village, villagerUuid);
        if (summary == null) return;
        NpcRegistry.NpcRecord record = NpcRegistry.get().getRecord(villagerUuid);
        long skinSeed = record != null ? record.skinSeed : 0L;

        VillagerData data = new VillagerData();
        data.setSkinSeed(skinSeed);
        data.setRace(summary.getRace());
        data.setProfession(summary.getProfession());
        data.setFirstName(summary.getFirstName());
        data.setLastName(summary.getLastName());
        store.putComponent(ref, VillagerData.getComponentType(), data);
        LOGGER.info("healVillagerDataIfMissing: re-attached VillagerData to " + villagerUuid
                + " (" + summary.getFullName() + ")");
    }

    /**
     * Matches every unhoused villager to a completed, unoccupied residential building.
     * Runs every tick so it handles both cases:
     * - house built before any villager exists
     * - villager arrives after houses already built
     */
    private void assignHomelessVillagers(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                         VillageData village, World world) {
        VillageManager mgr = VillageManager.get();

        UUID homelessUuid;
        while ((homelessUuid = mgr.findHomelessVillager(village)) != null) {
            BuildingRecord house = mgr.findUnoccupiedHouse(village);
            if (house == null) break; // no free houses right now

            mgr.assignVillagerToHouse(store, playerRef, village, house, homelessUuid);

            // Reload village after save so the next loop iteration sees updated state
            village = mgr.getVillageData(store, playerRef);
            if (village == null) break;

            final UUID assignedUuid = homelessUuid;
            final int houseX = house.getPosX();
            final int houseY = house.getPosY();
            final int houseZ = house.getPosZ();
            final int houseRotation = house.getRotation();

            // Move the villager to a tile inside their new house on the next world tick.
            // Stand-point is one block in front of the brazier (which faces the room interior).
            world.execute(() -> {
                Store<EntityStore> liveStore = world.getEntityStore().getStore();
                Entity entity = world.getEntity(assignedUuid);
                if (entity == null) return;

                Ref<EntityStore> villagerRef = world.getEntityRef(assignedUuid);
                if (villagerRef == null || !villagerRef.isValid()) return;

                double[] stand = BuildingType.getInteriorStandPoint(houseX, houseY, houseZ, houseRotation);
                double doorX = stand[0], doorY = stand[1], doorZ = stand[2];

                entity.moveTo(villagerRef, doorX, doorY, doorZ, liveStore);
                NpcRegistry.get().updatePosition(assignedUuid, doorX, doorY, doorZ);
                dev.hearthbound.npc.HearthboundDataStore.get().markDirty();

                // Mark villager as housed so hasHome() returns true and happiness shows correctly.
                VillagerData vd = liveStore.getComponent(villagerRef, VillagerData.getComponentType());
                if (vd != null && !vd.isHasHouse()) {
                    vd.setHasHouse(true);
                    liveStore.putComponent(villagerRef, VillagerData.getComponentType(), vd);
                }

                // Re-anchor wander to the house door so the villager stays near their home
                NPCEntity npcEntity = liveStore.getComponent(villagerRef, NPCEntity.getComponentType());
                if (npcEntity != null) {
                    npcEntity.setLeashPoint(new Vector3d(doorX, doorY, doorZ));
                }

                LOGGER.info("Villager " + assignedUuid + " moved to house door at "
                        + doorX + "," + doorY + "," + doorZ);
            });
        }
    }

    /**
     * For every completed farm with no assigned worker, tries to assign the first
     * eligible villager (has a house, has no profession).
     * Mirrors assignHomelessVillagers — runs every tick so order of building/arrival
     * doesn't matter.
     */
    private void assignUnstaffedFarms(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                      VillageData village) {
        VillageManager mgr = VillageManager.get();
        for (BuildingRecord building : village.getBuildings()) {
            if (!building.isCompleted()) continue;
            if (!BuildingType.FARM.equals(building.getType())) continue;
            if (building.getAssignedVillagerId() != null) continue;

            UUID uuid = mgr.assignFarmerProfession(store, playerRef, village, building);
            if (uuid != null) {
                LOGGER.info("assignUnstaffedFarms: assigned " + uuid + " to farm at "
                        + building.getPosX() + "," + building.getPosY() + "," + building.getPosZ());
                village = mgr.getVillageData(store, playerRef);
                if (village == null) return;
            }
        }
    }

    private void assignUnstaffedSawmills(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                         VillageData village) {
        VillageManager mgr = VillageManager.get();
        for (BuildingRecord building : village.getBuildings()) {
            if (!building.isCompleted()) continue;
            if (!BuildingType.SAWMILL.equals(building.getType())) continue;
            if (building.getAssignedVillagerId() != null) continue;

            UUID uuid = mgr.assignLumberjackProfession(store, playerRef, village, building);
            if (uuid != null) {
                LOGGER.info("assignUnstaffedSawmills: assigned " + uuid + " to sawmill at "
                        + building.getPosX() + "," + building.getPosY() + "," + building.getPosZ());
                village = mgr.getVillageData(store, playerRef);
                if (village == null) return;
            }
        }
    }

    private void assignUnstaffedMines(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                      VillageData village) {
        VillageManager mgr = VillageManager.get();
        for (BuildingRecord building : village.getBuildings()) {
            if (!building.isCompleted()) continue;
            if (!BuildingType.MINE.equals(building.getType())) continue;
            if (building.getAssignedVillagerId() != null) continue;

            UUID uuid = mgr.assignMinerProfession(store, playerRef, village, building);
            if (uuid != null) {
                LOGGER.info("assignUnstaffedMines: assigned " + uuid + " to mine at "
                        + building.getPosX() + "," + building.getPosY() + "," + building.getPosZ());
                village = mgr.getVillageData(store, playerRef);
                if (village == null) return;
            }
        }
    }

    private void assignUnstaffedGuardHouses(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                            VillageData village) {
        VillageManager mgr = VillageManager.get();
        for (BuildingRecord building : village.getBuildings()) {
            if (!building.isCompleted()) continue;
            if (!BuildingType.GUARD_HOUSE.equals(building.getType())) continue;
            if (building.getAssignedVillagerId() != null) continue;

            UUID uuid = mgr.assignGuardProfession(store, playerRef, village, building);
            if (uuid != null) {
                LOGGER.info("assignUnstaffedGuardHouses: assigned " + uuid + " to guard house at "
                        + building.getPosX() + "," + building.getPosY() + "," + building.getPosZ());
                village = mgr.getVillageData(store, playerRef);
                if (village == null) return;
            }
        }
    }

    private void assignUnstaffedForges(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                       VillageData village) {
        VillageManager mgr = VillageManager.get();
        for (BuildingRecord building : village.getBuildings()) {
            if (!building.isCompleted()) continue;
            if (!BuildingType.FORGE.equals(building.getType())) continue;
            if (building.getAssignedVillagerId() != null) continue;

            UUID uuid = mgr.assignBlacksmithProfession(store, playerRef, village, building);
            if (uuid != null) {
                LOGGER.info("assignUnstaffedForges: assigned " + uuid + " to forge at "
                        + building.getPosX() + "," + building.getPosY() + "," + building.getPosZ());
                village = mgr.getVillageData(store, playerRef);
                if (village == null) return;
            }
        }
    }
}
