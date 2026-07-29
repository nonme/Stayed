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
 *  com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent$Post
 *  com.hypixel.hytale.server.core.inventory.MaterialQuantity
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
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.tracking.TrackingService;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class CraftRecipeTrackerSystem
extends EntityEventSystem<EntityStore, CraftRecipeEvent.Post> {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core Tracking");
    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> playerRefComponentType = PlayerRef.getComponentType();

    public CraftRecipeTrackerSystem() {
        super(CraftRecipeEvent.Post.class);
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return this.playerRefComponentType;
    }

    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull CraftRecipeEvent.Post event) {
        try {
            PlayerRef playerRef = (PlayerRef)chunk.getComponent(index, this.playerRefComponentType);
            if (playerRef == null) {
                return;
            }
            Player player = (Player)playerRef.getComponent(Player.getComponentType());
            if (player == null) {
                return;
            }
            CraftingRecipe recipe = event.getCraftedRecipe();
            if (recipe == null) {
                return;
            }
            MaterialQuantity primaryOutput = recipe.getPrimaryOutput();
            String recipeId = recipe.getId();
            String outputItemId = primaryOutput != null ? primaryOutput.getItemId() : null;
            String itemId = outputItemId != null ? outputItemId : recipeId;
            int quantity = event.getQuantity();
            if (itemId == null || itemId.isEmpty()) {
                return;
            }
            TrackingService service = TrackingService.getInstance();
            if (service == null) {
                return;
            }
            service.dispatchItemCrafted(player, itemId, quantity);
        }
        catch (Exception e) {
            LOGGER.fine("CraftRecipeTrackerSystem error: " + e.getMessage());
        }
    }
}

