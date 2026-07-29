/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.lang.reflect.Method;
import java.util.logging.Logger;

class CitizenMHUDIntegration {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core MHUD");
    private static final String HUD_IDENTIFIER = "KyuubiSoft_WaypointRecorder";
    private static boolean available = false;
    private static boolean checked = false;
    private static Object mhudInstance = null;
    private static Method setCustomHudMethod = null;
    private static Method hideCustomHudMethod = null;

    CitizenMHUDIntegration() {
    }

    static boolean isAvailable() {
        if (!checked) {
            CitizenMHUDIntegration.checkAvailability();
        }
        return available;
    }

    private static void checkAvailability() {
        checked = true;
        try {
            Class<?> cls = Class.forName("com.buuz135.mhud.MultipleHUD");
            mhudInstance = cls.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
            if (mhudInstance == null) {
                checked = false;
                return;
            }
            setCustomHudMethod = cls.getMethod("setCustomHud", Player.class, PlayerRef.class, String.class, CustomUIHud.class);
            try {
                hideCustomHudMethod = cls.getMethod("hideCustomHud", Player.class, String.class);
            }
            catch (NoSuchMethodException e) {
                hideCustomHudMethod = cls.getMethod("hideCustomHud", Player.class, PlayerRef.class, String.class);
            }
            available = true;
            LOGGER.info("MHUD integration enabled for WaypointRecorder");
        }
        catch (ClassNotFoundException e) {
            available = false;
        }
        catch (Exception e) {
            LOGGER.warning("MHUD integration failed: " + e.getMessage());
            available = false;
        }
    }

    static boolean setCustomHud(Player player, PlayerRef playerRef, CustomUIHud hud) {
        if (!CitizenMHUDIntegration.isAvailable()) {
            return false;
        }
        try {
            setCustomHudMethod.invoke(mhudInstance, player, playerRef, HUD_IDENTIFIER, hud);
            return true;
        }
        catch (Exception e) {
            LOGGER.warning("Failed to set HUD via MHUD: " + e.getMessage());
            return false;
        }
    }

    static boolean hideCustomHud(Player player, PlayerRef playerRef) {
        if (!CitizenMHUDIntegration.isAvailable()) {
            return false;
        }
        try {
            if (hideCustomHudMethod.getParameterCount() == 2) {
                hideCustomHudMethod.invoke(mhudInstance, player, HUD_IDENTIFIER);
            } else {
                hideCustomHudMethod.invoke(mhudInstance, player, playerRef, HUD_IDENTIFIER);
            }
            return true;
        }
        catch (Exception e) {
            LOGGER.warning("Failed to hide HUD via MHUD: " + e.getMessage());
            return false;
        }
    }
}

