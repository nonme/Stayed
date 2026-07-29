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
 *  com.hypixel.hytale.protocol.packets.window.WindowType
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.windows.Window
 *  com.hypixel.hytale.server.core.inventory.Inventory
 *  com.hypixel.hytale.server.core.inventory.InventoryChangeEvent
 *  com.hypixel.hytale.server.core.inventory.ItemStack
 *  com.hypixel.hytale.server.core.inventory.container.ItemContainer
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
import com.hypixel.hytale.protocol.packets.window.WindowType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.InventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.tracking.TrackingService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class ProcessingBenchOutputTracker
extends EntityEventSystem<EntityStore, InventoryChangeEvent> {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core Tracking");
    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();
    private final Map<UUID, Map<String, Integer>> snapshots = new ConcurrentHashMap<UUID, Map<String, Integer>>();

    public ProcessingBenchOutputTracker() {
        super(InventoryChangeEvent.class);
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return this.playerRefType;
    }

    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InventoryChangeEvent event) {
        try {
            Map<String, Integer> current;
            PlayerRef playerRef = (PlayerRef)chunk.getComponent(index, this.playerRefType);
            if (playerRef == null) {
                return;
            }
            Player player = (Player)playerRef.getComponent(Player.getComponentType());
            if (player == null) {
                return;
            }
            UUID playerId = player.getUuid();
            Map<String, Integer> previous = this.snapshots.put(playerId, current = this.buildInventorySnapshot(player));
            if (previous == null) {
                return;
            }
            boolean hasBenchWindow = false;
            for (Window w : player.getWindowManager().getWindows()) {
                if (w.getType() == WindowType.Processing) {
                    hasBenchWindow = true;
                    break;
                }
                String className = w.getClass().getSimpleName();
                if (!"SimpleCraftingWindow".equals(className)) continue;
                hasBenchWindow = true;
                break;
            }
            if (!hasBenchWindow) {
                return;
            }
            TrackingService service = TrackingService.getInstance();
            if (service != null) {
                for (Map.Entry<String, Integer> entry : current.entrySet()) {
                    int prev = previous.getOrDefault(entry.getKey(), 0);
                    int delta = entry.getValue() - prev;
                    if (delta <= 0) continue;
                    service.dispatchItemCrafted(player, entry.getKey(), delta);
                }
            }
        }
        catch (Exception e) {
            LOGGER.fine("ProcessingBenchOutputTracker error: " + e.getMessage());
        }
    }

    private Map<String, Integer> buildInventorySnapshot(Player player) {
        HashMap<String, Integer> items = new HashMap<String, Integer>();
        Inventory inventory = player.getInventory();
        this.countContainer(items, inventory.getStorage());
        this.countContainer(items, inventory.getHotbar());
        this.countContainer(items, inventory.getBackpack());
        return items;
    }

    private void countContainer(Map<String, Integer> items, ItemContainer container) {
        if (container == null) {
            return;
        }
        short capacity = container.getCapacity();
        for (short i = 0; i < capacity; i = (short)(i + 1)) {
            String itemId;
            ItemStack stack = container.getItemStack(i);
            if (stack == null || stack.isEmpty() || (itemId = stack.getItemId()) == null) continue;
            items.merge(itemId, stack.getQuantity(), Integer::sum);
        }
    }

    public void clearPlayer(UUID playerId) {
        this.snapshots.remove(playerId);
    }
}

