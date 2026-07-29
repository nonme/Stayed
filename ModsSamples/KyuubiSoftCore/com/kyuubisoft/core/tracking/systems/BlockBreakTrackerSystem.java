/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.ArchetypeChunk
 *  com.hypixel.hytale.component.CommandBuffer
 *  com.hypixel.hytale.component.ComponentType
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.component.system.EntityEventSystem
 *  com.hypixel.hytale.math.vector.Vector3i
 *  com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.kyuubisoft.core.tracking.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.tracking.BlockCategories;
import com.kyuubisoft.core.tracking.PlacedBlockTracker;
import com.kyuubisoft.core.tracking.TrackingService;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class BlockBreakTrackerSystem
extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core Tracking");
    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> playerRefComponentType = PlayerRef.getComponentType();

    public BlockBreakTrackerSystem() {
        super(BreakBlockEvent.class);
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return this.playerRefComponentType;
    }

    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull BreakBlockEvent event) {
        try {
            Vector3i targetBlock;
            if (event.isCancelled()) {
                return;
            }
            PlayerRef playerRef = (PlayerRef)chunk.getComponent(index, this.playerRefComponentType);
            if (playerRef == null) {
                return;
            }
            Player player = (Player)playerRef.getComponent(Player.getComponentType());
            if (player == null) {
                return;
            }
            BlockType blockType = event.getBlockType();
            if (blockType == null) {
                return;
            }
            String blockId = blockType.getId();
            if (blockId == null || blockId.isEmpty()) {
                return;
            }
            TrackingService service = TrackingService.getInstance();
            if (service == null) {
                return;
            }
            PlacedBlockTracker placedTracker = service.getPlacedBlockTracker();
            if (placedTracker != null && (targetBlock = event.getTargetBlock()) != null && placedTracker.wasPlacedByPlayer(playerRef.getUuid(), targetBlock.getX(), targetBlock.getY(), targetBlock.getZ())) {
                LOGGER.fine("[CoreTracker] Skipping self-placed block: " + blockId);
                return;
            }
            String blockGroup = blockType.getGroup();
            String category = BlockCategories.categorize(blockGroup, blockId);
            if (category != null) {
                service.dispatchBlockBroken(player, blockId, blockGroup, category);
            } else if (BlockCategories.isHarvestable(blockGroup, blockId)) {
                service.dispatchBlockHarvested(player, blockId, blockGroup);
                if (BlockCategories.isFlower(blockGroup, blockId)) {
                    service.dispatchFlowerPicked(player, blockId, blockGroup);
                }
            }
        }
        catch (Exception e) {
            LOGGER.fine("BlockBreakTrackerSystem error: " + e.getMessage());
        }
    }
}

