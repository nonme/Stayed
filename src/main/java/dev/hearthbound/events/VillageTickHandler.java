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
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.systems.RoleChangeSystem;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.npc.HearthboundDataStore;
import dev.hearthbound.npc.NpcManager;
import dev.hearthbound.npc.VillagerScheduler;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.npc.NpcRestorer;
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

    public void start(Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
        stop();

        tickTask = TickScheduler.runRepeating(world, TICK_INTERVAL_MS, TICK_INTERVAL_MS, () -> {
            Store<EntityStore> liveStore = world.getEntityStore().getStore();
            tick(liveStore, playerRef, world);
        });

        LOGGER.info("VillageTickHandler started");
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
        assignHomelessVillagers(store, playerRef, village, world);
        assignUnstaffedFarms(store, playerRef, village);
        assignUnstaffedSawmills(store, playerRef, village);
        assignUnstaffedMines(store, playerRef, village);
        tickHunger(store, village);
        tickProduction(store, playerRef, village, world);
        villagerScheduler.tick(store, playerRef, village, world);
    }

    private void convertAllFollowers(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                     VillageData village, World world) {
        // Don't convert while the player is still walking back — wait for Return objective to finish
        if (isReturnObjectiveActive(store, playerRef)) return;

        // Walk every RESCUE-tagged registry record. This covers both loaded and unloaded NPCs,
        // and handles the race where the player flies away before RoleChangeSystem runs:
        //   - NPC loaded as Trapped  → re-apply Follower role change here, convert next tick
        //   - NPC loaded as Follower → convert immediately
        //   - NPC in unloaded chunk  → force-load so it appears on the next tick
        for (NpcRegistry.NpcRecord record : NpcRegistry.get().allRecords()) {
            if (record.interaction != NpcRegistry.InteractionType.RESCUE
                    && record.interaction != NpcRegistry.InteractionType.FOLLOWER) continue;

            Ref<EntityStore> ref = world.getEntityRef(record.entityUuid);
            if (ref == null || !ref.isValid()) {
                // Chunk not loaded — load it, then convert immediately in the callback
                // instead of waiting for the next tick (chunk may unload again by then).
                if (!record.restorePending) {
                    LOGGER.info("convertAllFollowers: loading chunk for follower uuid="
                            + record.entityUuid + " chunkIndex=" + record.chunkIndex);
                    final NpcRegistry.NpcRecord rec = record;
                    world.getChunkAsync(record.chunkIndex).thenRun(() -> world.execute(() -> {
                        Store<EntityStore> liveStore = world.getEntityStore().getStore();
                        Ref<EntityStore> liveRef = world.getEntityRef(rec.entityUuid);
                        if (liveRef == null || !liveRef.isValid()) return;
                        NPCEntity liveNpc = liveStore.getComponent(liveRef, NPCEntity.getComponentType());
                        if (liveNpc == null) return;
                        String role = liveNpc.getRoleName();
                        VillageData liveVillage = VillageManager.get().getVillageData(liveStore, playerRef);
                        if (liveVillage == null) return;
                        if (FOLLOWER_ROLE.equals(role)) {
                            convertFollowerToVillager(liveStore, playerRef, liveRef, liveVillage, world);
                        } else if (VICTIM_ROLE.equals(role) && FOLLOWER_ROLE.equals(rec.roleName)) {
                            int idx = NPCPlugin.get().getIndex(FOLLOWER_ROLE);
                            if (idx >= 0) RoleChangeSystem.requestRoleChange(liveRef, liveNpc.getRole(), idx, false, liveStore);
                        }
                    }));
                }
                continue;
            }

            NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
            if (npcEntity == null) continue;

            String currentRole = npcEntity.getRoleName();

            if (FOLLOWER_ROLE.equals(currentRole)) {
                // Ready to convert — reload village in case a previous iteration saved it
                VillageData liveVillage = VillageManager.get().getVillageData(store, playerRef);
                if (liveVillage == null) continue;
                convertFollowerToVillager(store, playerRef, ref, liveVillage, world);

            } else if (VICTIM_ROLE.equals(currentRole) && FOLLOWER_ROLE.equals(record.roleName)) {
                // RoleChangeSystem hasn't applied the role change yet (player flew away immediately
                // after dialog). The registry was already updated to FOLLOWER_ROLE by RescueDialogPage,
                // so we know the player agreed to join — re-apply the role change here.
                int followerRoleIndex = NPCPlugin.get().getIndex(FOLLOWER_ROLE);
                if (followerRoleIndex >= 0) {
                    RoleChangeSystem.requestRoleChange(ref, npcEntity.getRole(), followerRoleIndex, false, store);
                    LOGGER.info("convertAllFollowers: re-applied Trapped→Follower role change for uuid="
                            + record.entityUuid);
                }
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

            int villagerRoleIndex = NPCPlugin.get().getIndex(VILLAGER_ROLE);
            if (villagerRoleIndex < 0) {
                LOGGER.warning("convertFollowerToVillager: role '" + VILLAGER_ROLE + "' not found");
                return;
            }

            // changeAppearance=false — keep the PlayerSkin assigned at rescue time
            RoleChangeSystem.requestRoleChange(followerRef, npcEntity.getRole(), villagerRoleIndex, false, store);
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
                long skinSeed = oldRecord != null ? oldRecord.skinSeed : 0L;
                long chunkIndex = oldRecord != null ? oldRecord.chunkIndex : 0L;
                NpcRegistry.get().updateRecord(new NpcRegistry.NpcRecord(
                        followerUuid, VILLAGER_ROLE, NpcRegistry.InteractionType.VILLAGER, skinSeed, chunkIndex));
                HearthboundDataStore.get().save();
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
    private static final int HUNGER_PER_TICK = 2;

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
}
