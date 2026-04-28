package dev.hearthbound.events;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.ui.MinePage;
import dev.hearthbound.village.BuildingType;

/**
 * Intercepts F-key interaction on the Mine Torch anchor and opens the mine management UI.
 */
public class MineTorchHandler extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

    public MineTorchHandler() {
        super(UseBlockEvent.Pre.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                       UseBlockEvent.Pre event) {
        BlockType blockType = event.getBlockType();
        if (blockType == null) return;
        if (!BuildingType.MINE_SIGN_BLOCK.equals(blockType.getId())) return;

        Vector3i pos = event.getTargetBlock();
        if (pos == null) return;

        Ref<EntityStore> ref = null;
        if (event.getContext() != null) {
            ref = event.getContext().getOwningEntity();
        }
        if (ref == null) return;

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        event.setCancelled(true);

        World world = player.getWorld();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        MinePage page = new MinePage(playerRef, ref, world, pos.x, pos.y, pos.z);
        player.getPageManager().openCustomPage(ref, store, page);
    }
}
