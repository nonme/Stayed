package dev.hearthbound.village;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

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
