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
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.building.BuildingSystem;
import dev.hearthbound.village.BuildingType;

import java.util.logging.Logger;

/**
 * Handles BreakBlockEvent on the Founding Stone.
 *
 * <ul>
 *   <li>Pre-founding: cleans up the ghost preview.</li>
 *   <li>Post-founding but before the Town Hall is completed: reverts the village to the
 *       pre-founded state so the player can re-place the stone elsewhere.</li>
 *   <li>Always: force-clears any orphaned Filter_Air ghost blocks in the area.</li>
 * </ul>
 */
public class BlockBreakHandler extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private static final Logger LOGGER = Logger.getLogger(BlockBreakHandler.class.getName());

    public BlockBreakHandler() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                       BreakBlockEvent event) {
        String blockId = event.getBlockType().getId();
        if (!BuildingType.FOUNDING_STONE_BLOCK.equals(blockId)) return;

        Vector3i pos = event.getTargetBlock();
        Ref<EntityStore> playerRef = chunk.getReferenceTo(entityIndex);

        // Skip the village reset flow for prefab authors — they can freely break
        // the anchor block inside their prefab selection without wiping state.
        Player player = chunk.getComponent(entityIndex, Player.getComponentType());
        if (player != null && BuildingSystem.get().isPrefabAuthoring(player.getUuid())) {
            return;
        }

        EntityStore entityStore = (EntityStore) commandBuffer.getExternalData();
        World world = entityStore.getWorld();

        world.execute(() -> {
            Store<EntityStore> worldStore = world.getEntityStore().getStore();
            // Always attempt a proper snapshot-based restore — this handles both the in-memory
            // case (ghost still active this session) and the post-restart case (snapshot loaded
            // from VillageData).
            BuildingSystem.get().clearGhostPreview(worldStore, playerRef, world);
            if (playerRef != null) {
                BuildingSystem.get().resetFoundingIfPreTownHall(worldStore, playerRef, world);
            }
            // Belt-and-braces: if somehow ghost blocks exist outside the snapshot (legacy state),
            // brute-force remove them so the area is clean.
            BuildingSystem.get().clearOrphanedGhost(world, pos.x, pos.y, pos.z, 20);
        });
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
