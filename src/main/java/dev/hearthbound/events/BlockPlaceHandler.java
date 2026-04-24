package dev.hearthbound.events;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.building.BuildingSystem;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

import java.util.logging.Logger;

/**
 * Handles PlaceBlockEvent to detect Founding Stone placement.
 * Shows ghost preview of Town Hall with rotation matching block placement direction.
 */
public class BlockPlaceHandler extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    private static final Logger LOGGER = Logger.getLogger(BlockPlaceHandler.class.getName());

    public BlockPlaceHandler() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                       PlaceBlockEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || item.isEmpty()) return;

        String itemId = item.getItemId();
        if (!BuildingType.FOUNDING_STONE_BLOCK.equals(itemId)) return;

        Ref<EntityStore> playerRef = chunk.getReferenceTo(entityIndex);
        if (playerRef == null) return;

        Player player = chunk.getComponent(entityIndex, Player.getComponentType());
        if (player == null) return;

        // Prefab authoring mode: let the player drop the Founding Stone inside a prefab
        // selection without triggering ghost preview / founding flow.
        if (BuildingSystem.get().isPrefabAuthoring(player.getUuid())) {
            return;
        }

        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village != null && village.isFounded()) {
            LOGGER.info("Village already founded, ignoring Founding Stone placement");
            return;
        }

        if (BuildingSystem.get().hasActivePreview()) {
            LOGGER.info("Ghost preview already active, ignoring");
            return;
        }

        Vector3i pos = event.getTargetBlock();
        LOGGER.info("Founding Stone placed at " + pos.x + ", " + pos.y + ", " + pos.z);

        EntityStore entityStore = (EntityStore) commandBuffer.getExternalData();
        World world = entityStore.getWorld();

        // Defer to next tick (store is locked during EntityEventSystem)
        Ref<EntityStore> refForLater = playerRef;
        world.execute(() -> {
            // getRotationIndex returns the anchor's yaw in the same encoding the prefab editor
            // uses when saving the anchor's rotation. Pass it through unchanged — the rotation
            // math in PrefabLoader will take the difference with the prefab's anchor rotation.
            int rotation = 0;
            try {
                BlockAccessor accessor = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
                if (accessor != null) {
                    rotation = accessor.getRotationIndex(pos.x, pos.y, pos.z) & 0x3;
                    LOGGER.info("Founding Stone placed at " + pos + " rotation=" + rotation);
                }
            } catch (Exception e) {
                LOGGER.warning("Could not read block rotation: " + e.getMessage());
            }

            Store<EntityStore> liveStore = world.getEntityStore().getStore();
            BuildingSystem.get().showGhostPreview(liveStore, refForLater, world,
                    BuildingType.TOWN_HALL, pos.x, pos.y, pos.z, rotation);
            LOGGER.info("Ghost preview shown with rotation " + rotation);
        });
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
