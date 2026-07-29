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
 *  com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent
 *  com.hypixel.hytale.server.core.inventory.ItemStack
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
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.tracking.BlockCategories;
import com.kyuubisoft.core.tracking.TrackingService;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class InteractivePickupTrackerSystem
extends EntityEventSystem<EntityStore, InteractivelyPickupItemEvent> {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core Tracking");
    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> playerRefComponentType = PlayerRef.getComponentType();

    public InteractivePickupTrackerSystem() {
        super(InteractivelyPickupItemEvent.class);
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return this.playerRefComponentType;
    }

    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractivelyPickupItemEvent event) {
        try {
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
            ItemStack itemStack = event.getItemStack();
            if (itemStack == null || itemStack.isEmpty()) {
                return;
            }
            String blockKey = itemStack.getBlockKey();
            if (blockKey == null || blockKey.isEmpty()) {
                return;
            }
            BlockType blockType = (BlockType)BlockType.getAssetMap().getAsset((Object)blockKey);
            if (blockType == null) {
                return;
            }
            String blockId = blockType.getId();
            String blockGroup = blockType.getGroup();
            if (!BlockCategories.isHarvestable(blockGroup, blockId)) {
                return;
            }
            TrackingService service = TrackingService.getInstance();
            if (service == null) {
                return;
            }
            LOGGER.fine("Block harvested via InteractivePickup: " + blockId + " (group=" + blockGroup + ") by " + player.getDisplayName());
            service.dispatchBlockHarvested(player, blockId, blockGroup);
            if (BlockCategories.isFlower(blockGroup, blockId)) {
                service.dispatchFlowerPicked(player, blockId, blockGroup);
            }
        }
        catch (Exception e) {
            LOGGER.fine("InteractivePickupTracker error: " + e.getMessage());
        }
    }
}

