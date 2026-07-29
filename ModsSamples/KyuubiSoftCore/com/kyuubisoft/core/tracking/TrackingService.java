/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.entity.entities.Player
 */
package com.kyuubisoft.core.tracking;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.kyuubisoft.core.api.CoreAPI;
import com.kyuubisoft.core.tracking.PlacedBlockTracker;
import com.kyuubisoft.core.tracking.TrackingListener;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class TrackingService {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core Tracking");
    private static TrackingService instance;
    private final List<TrackingListener> listeners = new CopyOnWriteArrayList<TrackingListener>();
    private final PlacedBlockTracker placedBlockTracker;
    private final Set<UUID> debugPlayers = ConcurrentHashMap.newKeySet();

    public TrackingService() {
        instance = this;
        this.placedBlockTracker = new PlacedBlockTracker();
    }

    public static TrackingService getInstance() {
        return instance;
    }

    public void addListener(TrackingListener listener) {
        this.listeners.add(listener);
        if (CoreAPI.isDebug()) {
            LOGGER.info("Tracking listener registered: " + listener.getClass().getSimpleName());
        }
    }

    public void removeListener(TrackingListener listener) {
        this.listeners.remove(listener);
    }

    public int getListenerCount() {
        return this.listeners.size();
    }

    public boolean toggleDebug(UUID playerId) {
        if (this.debugPlayers.contains(playerId)) {
            this.debugPlayers.remove(playerId);
            return false;
        }
        this.debugPlayers.add(playerId);
        return true;
    }

    public boolean isDebugEnabled(UUID playerId) {
        return this.debugPlayers.contains(playerId);
    }

    public void clearDebugPlayer(UUID playerId) {
        this.debugPlayers.remove(playerId);
    }

    private void debugMsg(Player player, String msg) {
        UUID pid;
        if (player == null) {
            return;
        }
        UUID uUID = pid = player.getPlayerRef() != null ? player.getPlayerRef().getUuid() : null;
        if (pid != null && this.debugPlayers.contains(pid) && CoreAPI.isDebug()) {
            try {
                LOGGER.info("[DEBUG " + player.getDisplayName() + "] " + msg);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public void dispatchBlockBroken(Player player, String blockId, String blockGroup, String category) {
        this.debugMsg(player, "blocks_" + category + " " + blockId + " (group: " + blockGroup + ")");
        for (TrackingListener l : this.listeners) {
            try {
                l.onBlockBroken(player, blockId, blockGroup, category);
            }
            catch (Exception e) {
                LOGGER.fine("TrackingListener error in onBlockBroken: " + e.getMessage());
            }
        }
    }

    public void dispatchBlockPlaced(Player player, String blockId, String blockGroup) {
        this.debugMsg(player, "blocks_placed " + blockId + " (group: " + blockGroup + ")");
        for (TrackingListener l : this.listeners) {
            try {
                l.onBlockPlaced(player, blockId, blockGroup);
            }
            catch (Exception e) {
                LOGGER.fine("TrackingListener error in onBlockPlaced: " + e.getMessage());
            }
        }
    }

    public void dispatchKill(Player player, String victimType, boolean isPlayerKill) {
        this.debugMsg(player, (isPlayerKill ? "player_kills" : "kills") + " " + victimType);
        for (TrackingListener l : this.listeners) {
            try {
                l.onKill(player, victimType, isPlayerKill);
            }
            catch (Exception e) {
                LOGGER.fine("TrackingListener error in onKill: " + e.getMessage());
            }
        }
    }

    public void dispatchDamageDealt(Player player, int damage, String victimType) {
        this.debugMsg(player, "damage_dealt " + damage + " -> " + victimType);
        for (TrackingListener l : this.listeners) {
            try {
                l.onDamageDealt(player, damage, victimType);
            }
            catch (Exception e) {
                LOGGER.fine("TrackingListener error in onDamageDealt: " + e.getMessage());
            }
        }
    }

    public void dispatchDamageTaken(Player player, int damage, String causeType, String sourceType) {
        this.debugMsg(player, "damage_taken " + damage + " (cause: " + causeType + ", source: " + sourceType + ")");
        for (TrackingListener l : this.listeners) {
            try {
                l.onDamageTaken(player, damage, causeType, sourceType);
            }
            catch (Exception e) {
                LOGGER.fine("TrackingListener error in onDamageTaken: " + e.getMessage());
            }
        }
    }

    public void dispatchDistanceTraveled(Player player, int blocks) {
        for (TrackingListener l : this.listeners) {
            try {
                l.onDistanceTraveled(player, blocks);
            }
            catch (Exception e) {
                LOGGER.fine("TrackingListener error in onDistanceTraveled: " + e.getMessage());
            }
        }
    }

    public void dispatchPlaytimeMinute(Player player) {
        for (TrackingListener l : this.listeners) {
            try {
                l.onPlaytimeMinute(player);
            }
            catch (Exception e) {
                LOGGER.fine("TrackingListener error in onPlaytimeMinute: " + e.getMessage());
            }
        }
    }

    public void dispatchBlockHarvested(Player player, String blockId, String blockGroup) {
        this.debugMsg(player, "blocks_harvested " + blockId + " (group: " + blockGroup + ")");
        for (TrackingListener l : this.listeners) {
            try {
                l.onBlockHarvested(player, blockId, blockGroup);
            }
            catch (Exception e) {
                LOGGER.fine("TrackingListener error in onBlockHarvested: " + e.getMessage());
            }
        }
    }

    public void dispatchFlowerPicked(Player player, String blockId, String blockGroup) {
        this.debugMsg(player, "flowers_picked " + blockId + " (group: " + blockGroup + ")");
        for (TrackingListener l : this.listeners) {
            try {
                l.onFlowerPicked(player, blockId, blockGroup);
            }
            catch (Exception e) {
                LOGGER.fine("TrackingListener error in onFlowerPicked: " + e.getMessage());
            }
        }
    }

    public void dispatchZoneDiscovered(Player player, String zoneName) {
        this.debugMsg(player, "zone_discovered " + zoneName);
        for (TrackingListener l : this.listeners) {
            try {
                l.onZoneDiscovered(player, zoneName);
            }
            catch (Exception e) {
                LOGGER.fine("TrackingListener error in onZoneDiscovered: " + e.getMessage());
            }
        }
    }

    public void dispatchItemCrafted(Player player, String itemId, int amount) {
        this.debugMsg(player, "items_crafted " + itemId + " x" + amount);
        for (TrackingListener l : this.listeners) {
            try {
                l.onItemCrafted(player, itemId, amount);
            }
            catch (Exception e) {
                LOGGER.fine("TrackingListener error in onItemCrafted: " + e.getMessage());
            }
        }
    }

    public PlacedBlockTracker getPlacedBlockTracker() {
        return this.placedBlockTracker;
    }

    public void clearPlayer(UUID playerId) {
        this.placedBlockTracker.clearPlayer(playerId);
    }
}

