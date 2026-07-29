/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.universe.world.WorldMapTracker
 */
package com.kyuubisoft.core.registry;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class HudVisibilityService {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft HudVisibility");
    private static final List<HudVisibilityListener> listeners = new CopyOnWriteArrayList<HudVisibilityListener>();
    private static final Map<UUID, Boolean> playerMapState = new ConcurrentHashMap<UUID, Boolean>();
    private static Field mapVisibleField = null;
    private static boolean reflectionFailed = false;

    public static void addListener(HudVisibilityListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(HudVisibilityListener listener) {
        listeners.remove(listener);
    }

    public static boolean isMapOpen(UUID playerId) {
        Boolean state = playerMapState.get(playerId);
        return state != null && state != false;
    }

    public static void tick(Collection<Player> onlinePlayers) {
        if (reflectionFailed || listeners.isEmpty()) {
            return;
        }
        for (Player player : onlinePlayers) {
            try {
                boolean mapOpen;
                UUID playerId;
                Boolean previousState;
                if (player == null || player.getPlayerRef() == null || (previousState = playerMapState.put(playerId = player.getPlayerRef().getUuid(), mapOpen = HudVisibilityService.readMapVisible(player))) != null && previousState == mapOpen) continue;
                for (HudVisibilityListener listener : listeners) {
                    try {
                        listener.onMapVisibilityChanged(player, playerId, mapOpen);
                    }
                    catch (Exception e) {
                        LOGGER.fine("HudVisibility listener error: " + e.getMessage());
                    }
                }
            }
            catch (Exception e) {
                LOGGER.fine("HudVisibility tick error: " + e.getMessage());
            }
        }
    }

    public static void clearPlayer(UUID playerId) {
        playerMapState.remove(playerId);
    }

    private static boolean readMapVisible(Player player) {
        try {
            WorldMapTracker tracker = player.getWorldMapTracker();
            if (tracker != null && mapVisibleField != null) {
                return mapVisibleField.getBoolean(tracker);
            }
        }
        catch (Exception e) {
            LOGGER.fine("Could not read map visibility: " + e.getMessage());
        }
        return false;
    }

    static {
        try {
            mapVisibleField = WorldMapTracker.class.getDeclaredField("clientHasWorldMapVisible");
            mapVisibleField.setAccessible(true);
            LOGGER.info("HudVisibilityService: Reflection on WorldMapTracker.clientHasWorldMapVisible successful");
        }
        catch (Exception e) {
            reflectionFailed = true;
            LOGGER.warning("HudVisibilityService: Could not access WorldMapTracker.clientHasWorldMapVisible - " + e.getMessage());
        }
    }

    @FunctionalInterface
    public static interface HudVisibilityListener {
        public void onMapVisibilityChanged(Player var1, UUID var2, boolean var3);
    }
}

