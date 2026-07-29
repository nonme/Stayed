/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.logger.HytaleLogger
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 *  com.hypixel.hytale.protocol.InteractionType
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent
 *  com.hypixel.hytale.server.core.inventory.ItemStack
 *  com.hypixel.hytale.server.core.plugin.JavaPlugin
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.Universe
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.yourname.companion.systems;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.yourname.companion.commands.CompanionCommand;
import com.yourname.companion.data.CompanionRecord;
import com.yourname.companion.data.PlayerCompanionData;
import com.yourname.companion.runtime.PlayerRuntimeState;
import com.yourname.companion.storage.CompanionManager;
import com.yourname.companion.systems.CompanionSystem;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class InteractionHandler {
    private static final long INTERACT_DEBOUNCE_MS = 450L;
    private static final boolean DEBUG_INTERACTION = false;
    private final CompanionManager companionManager;
    private final CompanionSystem companionSystem;
    private final CompanionCommand companionCommand;
    private final HytaleLogger logger;
    private final Map<UUID, Long> lastInteractAtByPlayer = new ConcurrentHashMap<UUID, Long>();

    public InteractionHandler(CompanionManager companionManager, CompanionSystem companionSystem, CompanionCommand companionCommand, HytaleLogger logger) {
        this.companionManager = companionManager;
        this.companionSystem = companionSystem;
        this.companionCommand = companionCommand;
        this.logger = logger;
    }

    public boolean onEntityInteract(PlayerRef playerRef, Ref<EntityStore> targetEntityRef, World world) {
        if (playerRef == null || world == null) {
            return false;
        }
        PlayerRuntimeState playerRuntime = this.companionManager.getPlayerRuntime(playerRef.getUuid());
        if (playerRuntime.awaitingChestLink) {
            this.companionCommand.completeChestLink(playerRef);
            return true;
        }
        if (targetEntityRef != null) {
            CompanionRecord companion = this.companionSystem.findCompanionByEntity(playerRef, targetEntityRef, world);
            if (companion != null) {
                PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
                data.selectedCompanionId = companion.uniqueId;
                this.companionCommand.openUIMenuForPlayer(playerRef);
                return true;
            }
            if (this.companionSystem.tryRecruitTargetFromInteract(playerRef, targetEntityRef, world)) {
                playerRef.sendMessage(Message.raw((String)"Recruit complete."));
                return true;
            }
        }
        return false;
    }

    public void onBlockInteract(PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }
        PlayerRuntimeState playerRuntime = this.companionManager.getPlayerRuntime(playerRef.getUuid());
        if (playerRuntime.awaitingChestLink) {
            this.companionCommand.completeChestLink(playerRef);
        }
    }

    public void tryRegisterEvents(JavaPlugin plugin) {
        this.tryRegisterEntityInteract(plugin);
    }

    private void tryRegisterEntityInteract(JavaPlugin plugin) {
        try {
            plugin.getEventRegistry().register(PlayerInteractEvent.class, (Object)null, this::onPlayerInteract);
            this.logger.at(Level.INFO).log("InteractionHandler: registered PlayerInteractEvent for recruit/chest-link.");
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to register interaction event.");
        }
    }

    private void onPlayerInteract(PlayerInteractEvent event) {
        try {
            String itemId;
            long last;
            if (event == null || event.getPlayer() == null) {
                return;
            }
            InteractionType action = event.getActionType();
            if (action != InteractionType.Use && action != InteractionType.Secondary && action != InteractionType.Primary) {
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
            long now = System.currentTimeMillis();
            if (now - (last = this.lastInteractAtByPlayer.getOrDefault(playerId, 0L).longValue()) < 450L) {
                return;
            }
            this.lastInteractAtByPlayer.put(playerId, now);
            PlayerRuntimeState playerRuntime = this.companionManager.getPlayerRuntime(playerId);
            playerRuntime.lastRecruitInteractMs = now;
            if (playerRuntime.awaitingChestLink) {
                this.companionCommand.completeChestLink(playerRef);
                return;
            }
            UUID worldId = playerRef.getWorldUuid();
            World world = worldId != null ? Universe.get().getWorld(worldId) : null;
            Ref targetRef = event.getTargetRef();
            if (targetRef == null && event.getTargetEntity() != null) {
                targetRef = event.getTargetEntity().getReference();
            }
            if (world != null && targetRef != null && this.onEntityInteract(playerRef, (Ref<EntityStore>)targetRef, world)) {
                return;
            }
            if (world != null && this.companionSystem.tryRecruitNearestByProximity(playerRef, world)) {
                playerRef.sendMessage(Message.raw((String)"Recruited companion!"));
                return;
            }
            ItemStack held = event.getItemInHand();
            if (held != null && !held.isEmpty() && (itemId = held.getItemId()) != null && (itemId.startsWith("Companion_") || itemId.startsWith("Portal_") || itemId.startsWith("PortalKey_"))) {
                return;
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Interaction handler input processing failed.");
        }
    }
}

