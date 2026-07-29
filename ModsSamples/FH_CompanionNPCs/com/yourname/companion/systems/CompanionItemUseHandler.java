/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.logger.HytaleLogger
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 *  com.hypixel.hytale.protocol.InteractionType
 *  com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent
 *  com.hypixel.hytale.server.core.inventory.ItemStack
 *  com.hypixel.hytale.server.core.plugin.JavaPlugin
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 */
package com.yourname.companion.systems;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.yourname.companion.commands.CompanionCommand;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class CompanionItemUseHandler {
    private static final long UI_OPEN_COOLDOWN_MS = 750L;
    private static final Set<String> CONTROL_ITEM_IDS = Set.of("Companion_Control_Totem", "Portal_Device", "Companion_Radio");
    private final CompanionCommand companionCommand;
    private final HytaleLogger logger;
    private final Map<UUID, Long> lastOpenAtByPlayer = new ConcurrentHashMap<UUID, Long>();

    public CompanionItemUseHandler(CompanionCommand companionCommand, HytaleLogger logger) {
        this.companionCommand = companionCommand;
        this.logger = logger;
    }

    public void registerEvents(JavaPlugin plugin) {
        plugin.getEventRegistry().register(PlayerInteractEvent.class, (Object)null, this::onPlayerInteract);
        this.logger.at(Level.INFO).log("Companion control-item listener registered.");
    }

    private void onPlayerInteract(PlayerInteractEvent event) {
        try {
            long last;
            if (event == null || event.getPlayer() == null) {
                return;
            }
            InteractionType action = event.getActionType();
            if (action != InteractionType.Use && action != InteractionType.Secondary) {
                return;
            }
            ItemStack held = event.getItemInHand();
            if (held == null || held.isEmpty()) {
                return;
            }
            String itemId = held.getItemId();
            if (itemId == null || !this.isControlItem(itemId)) {
                return;
            }
            PlayerRef playerRef = event.getPlayer().getPlayerRef();
            if (playerRef == null) {
                return;
            }
            UUID playerId = playerRef.getUuid();
            if (playerId == null) {
                return;
            }
            if (this.companionCommand.isVisualUiOpen(playerId)) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now - (last = this.lastOpenAtByPlayer.getOrDefault(playerId, 0L).longValue()) < 750L) {
                return;
            }
            this.lastOpenAtByPlayer.put(playerId, now);
            this.logger.at(Level.INFO).log("Companion control item used: " + itemId + " action=" + String.valueOf(action));
            this.companionCommand.openUIMenuForPlayer(playerRef);
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed handling companion control-item use.");
        }
    }

    private boolean isControlItem(String itemId) {
        if (CONTROL_ITEM_IDS.contains(itemId)) {
            return true;
        }
        return itemId.startsWith("Portal_") || itemId.startsWith("PortalKey_") || itemId.startsWith("Companion_");
    }
}

