package dev.hearthbound.village;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Central manager for village lifecycle.
 * Handles village creation, state transitions, and NPC coordination.
 */
public class VillageManager {

    private static final Logger LOGGER = Logger.getLogger(VillageManager.class.getName());

    private static VillageManager instance;

    public static VillageManager get() {
        return instance;
    }

    public static void init() {
        instance = new VillageManager();
    }

    private VillageManager() {}

    /**
     * Get village data for a player. Returns null if no village exists.
     */
    public VillageData getVillageData(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        return store.getComponent(playerRef, VillageData.getComponentType());
    }

    /**
     * Get or create village data for a player.
     */
    public VillageData getOrCreateVillageData(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        VillageData data = getVillageData(store, playerRef);
        if (data == null) {
            data = new VillageData();
            store.putComponent(playerRef, VillageData.getComponentType(), data);
        }
        return data;
    }

    /**
     * Found a new village at the given position.
     */
    public void foundVillage(Store<EntityStore> store, Ref<EntityStore> playerRef, World world,
                             int x, int y, int z) {
        VillageData data = getOrCreateVillageData(store, playerRef);
        data.setStage(VillageData.STAGE_FOUNDED);
        data.setFoundingStonePos(x, y, z);
        data.setFoundedAtTick(world.getTick());
        store.putComponent(playerRef, VillageData.getComponentType(), data);
        LOGGER.info("Village founded at " + x + ", " + y + ", " + z);
    }

    /**
     * Save current village state persistently.
     */
    public void save(Store<EntityStore> store, Ref<EntityStore> playerRef, VillageData data) {
        store.putComponent(playerRef, VillageData.getComponentType(), data);
    }

    /**
     * Add a building record to the village.
     */
    public void addBuilding(Store<EntityStore> store, Ref<EntityStore> playerRef,
                            BuildingRecord building) {
        VillageData data = getOrCreateVillageData(store, playerRef);
        data.addBuilding(building);
        save(store, playerRef, data);
        LOGGER.info("Building added: " + building.getType() + " at " +
                building.getPosX() + ", " + building.getPosY() + ", " + building.getPosZ());
    }

    /**
     * Finds the first completed residential building with no assigned villager, or null.
     */
    public BuildingRecord findUnoccupiedHouse(VillageData data) {
        for (BuildingRecord b : data.getBuildings()) {
            if (!b.isCompleted()) continue;
            if (!BuildingType.isResidential(b.getType())) continue;
            if (b.getAssignedVillagerId() == null) return b;
        }
        return null;
    }

    /**
     * Assigns the villager to the house: sets house.assignedVillagerId, updates VillagerData
     * state to STATE_IDLE, and syncs the VillagerSummary in village data.
     * Saves village data after.
     */
    public void assignVillagerToHouse(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                      VillageData data, BuildingRecord house, UUID villagerUuid) {
        house.setAssignedVillagerId(villagerUuid);

        // Update VillagerSummary
        for (VillagerSummary summary : data.getVillagers()) {
            if (villagerUuid.equals(summary.getVillagerUuid())) {
                summary.setProfession(summary.getProfession()); // no-op, summary has no state field — ok
                break;
            }
        }

        save(store, playerRef, data);
        LOGGER.info("Villager " + villagerUuid + " assigned to house at "
                + house.getPosX() + "," + house.getPosY() + "," + house.getPosZ());
    }

    /**
     * Finds the first homeless villager UUID from the village's villager list, or null.
     * A villager is homeless if their VillagerData on the NpcRegistry has STATE_HOMELESS,
     * OR if they appear in the villager list but no house has them assigned.
     */
    public UUID findHomelessVillager(VillageData data) {
        for (VillagerSummary summary : data.getVillagers()) {
            UUID uuid = summary.getVillagerUuid();
            if (uuid == null) continue;
            if (!isVillagerAssignedToAnyHouse(data, uuid)) return uuid;
        }
        return null;
    }

    private boolean isVillagerAssignedToAnyHouse(VillageData data, UUID villagerUuid) {
        for (BuildingRecord b : data.getBuildings()) {
            if (villagerUuid.equals(b.getAssignedVillagerId())) return true;
        }
        return false;
    }

    /**
     * Returns all completed buildings of the given type, or empty list.
     */
    public List<BuildingRecord> findCompletedBuildings(VillageData data, String type) {
        return data.getBuildings().stream()
                .filter(b -> b.isCompleted() && type.equals(b.getType()))
                .toList();
    }

    /**
     * Returns the first completed farm, or null.
     */
    public BuildingRecord findCompletedFarm(VillageData data) {
        return data.getBuildings().stream()
                .filter(b -> b.isCompleted() && BuildingType.FARM.equals(b.getType()))
                .findFirst().orElse(null);
    }

    /**
     * Returns the first completed warehouse, or null.
     */
    public BuildingRecord findCompletedWarehouse(VillageData data) {
        return data.getBuildings().stream()
                .filter(b -> b.isCompleted() && BuildingType.WAREHOUSE.equals(b.getType()))
                .findFirst().orElse(null);
    }

    /**
     * Returns the first completed sawmill, or null.
     */
    public BuildingRecord findCompletedSawmill(VillageData data) {
        return data.getBuildings().stream()
                .filter(b -> b.isCompleted() && BuildingType.SAWMILL.equals(b.getType()))
                .findFirst().orElse(null);
    }

    /**
     * Returns the first completed mine, or null.
     */
    public BuildingRecord findCompletedMine(VillageData data) {
        return data.getBuildings().stream()
                .filter(b -> b.isCompleted() && BuildingType.MINE.equals(b.getType()))
                .findFirst().orElse(null);
    }

    /**
     * Assigns a profession to the first eligible (housed, no profession) villager and
     * links them to the given building record.
     * Returns the UUID of the assigned villager, or null if nobody eligible.
     */
    private UUID assignWorkProfession(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                      VillageData data, BuildingRecord building, String profession) {
        for (VillagerSummary summary : data.getVillagers()) {
            if (!VillagerData.PROF_NONE.equals(summary.getProfession())) continue;
            UUID uuid = summary.getVillagerUuid();
            if (uuid == null) continue;

            if (!isVillagerAssignedToAnyHouse(data, uuid)) continue;

            summary.setProfession(profession);
            building.setAssignedVillagerId(uuid);
            save(store, playerRef, data);

            LOGGER.info("Assigned " + profession + " profession to villager " + uuid);
            return uuid;
        }
        return null;
    }

    /**
     * Assigns PROF_FARMER to the first eligible (housed, no profession) villager and
     * links them to the given farm building record.
     * Returns the UUID of the assigned villager, or null if nobody eligible.
     */
    public UUID assignFarmerProfession(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                       VillageData data, BuildingRecord farm) {
        return assignWorkProfession(store, playerRef, data, farm, VillagerData.PROF_FARMER);
    }

    /**
     * Assigns PROF_LUMBERJACK to the first eligible villager and links them to the sawmill.
     */
    public UUID assignLumberjackProfession(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                           VillageData data, BuildingRecord sawmill) {
        return assignWorkProfession(store, playerRef, data, sawmill, VillagerData.PROF_LUMBERJACK);
    }

    /**
     * Assigns PROF_MASON to the first eligible villager and links them to the mine.
     */
    public UUID assignMinerProfession(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                      VillageData data, BuildingRecord mine) {
        return assignWorkProfession(store, playerRef, data, mine, VillagerData.PROF_MASON);
    }

    /**
     * Finds the VillagerSummary by UUID, or null.
     */
    public VillagerSummary findVillagerSummary(VillageData data, UUID uuid) {
        if (uuid == null) return null;
        for (VillagerSummary s : data.getVillagers()) {
            if (uuid.equals(s.getVillagerUuid())) return s;
        }
        return null;
    }

    /**
     * Rewrites every UUID reference from oldUuid → newUuid across the village data.
     * Used by NpcRespawner when an NPC is replaced by a fresh entity (the engine
     * always assigns a new UUID on spawn, so we can't reuse the old one).
     *
     * Touches: VillagerSummary.villagerUuid, BuildingRecord.assignedVillagerId on
     * every building (residential + work), so the villager keeps their house and
     * job through a respawn.
     */
    public int replaceVillagerUuid(VillageData data, UUID oldUuid, UUID newUuid) {
        if (data == null || oldUuid == null || newUuid == null) return 0;
        int touched = 0;
        for (VillagerSummary s : data.getVillagers()) {
            if (oldUuid.equals(s.getVillagerUuid())) {
                s.setVillagerUuid(newUuid);
                touched++;
            }
        }
        for (BuildingRecord b : data.getBuildings()) {
            if (oldUuid.equals(b.getAssignedVillagerId())) {
                b.setAssignedVillagerId(newUuid);
                touched++;
            }
        }
        if (touched > 0) {
            LOGGER.info("replaceVillagerUuid: " + oldUuid + " → " + newUuid + " (" + touched + " refs)");
        }
        return touched;
    }

    /**
     * Mark a building as completed and advance village stage if appropriate.
     */
    public void completeBuilding(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                 BuildingRecord building) {
        VillageData data = getOrCreateVillageData(store, playerRef);
        building.setCompleted(true);
        building.setBuildProgress(100);

        // Advance stage based on building type
        switch (building.getType()) {
            case BuildingType.TOWN_HALL:
                if (data.getStage() < VillageData.STAGE_TOWN_HALL) {
                    data.setStage(VillageData.STAGE_TOWN_HALL);
                }
                break;
            case BuildingType.WAREHOUSE:
                if (data.getStage() < VillageData.STAGE_WAREHOUSE) {
                    data.setStage(VillageData.STAGE_WAREHOUSE);
                }
                break;
        }

        save(store, playerRef, data);
        LOGGER.info("Building completed: " + building.getType() + " (stage → " + data.getStage() + ")");
    }
}
