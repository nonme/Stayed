/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.entity.entities.Player
 */
package com.kyuubisoft.core.reward;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.kyuubisoft.core.api.CoreAPI;
import com.kyuubisoft.core.economy.ExternalEconomyBridge;
import com.kyuubisoft.core.lootbag.LootbagService;
import com.kyuubisoft.core.util.CommandUtils;
import com.kyuubisoft.core.util.ItemUtils;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

public class RewardGrantHelper {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core");
    private static boolean rpgApiChecked = false;
    private static boolean rpgApiAvailable = false;
    private static Object rpgApiInstance;
    private static Method rpgAddXpMethod;
    private static Method rpgAddXpSimpleMethod;
    private static Object rpgQuestXpSource;
    private static boolean endlessApiChecked;
    private static boolean endlessApiAvailable;
    private static Object endlessApiInstance;
    private static Method endlessGrantXpMethod;
    private static boolean lcApiChecked;
    private static boolean lcApiAvailable;
    private static Object lcServiceInstance;
    private static Method lcAddXpMethod;
    private static Method lcRemoveXpMethod;

    private RewardGrantHelper() {
    }

    public static boolean grantItem(Player player, String itemId, int amount) {
        if (itemId == null || itemId.isBlank() || amount <= 0) {
            LOGGER.warning("Invalid item reward: itemId=" + itemId + " amount=" + amount);
            return false;
        }
        boolean success = ItemUtils.grantItem(player, itemId, amount);
        if (success && CoreAPI.isDebug()) {
            LOGGER.info("Granted " + amount + "x " + itemId + " to " + player.getDisplayName());
        }
        return success;
    }

    public static boolean grantCommand(Player player, String command, String executor) {
        if (command == null || command.isBlank()) {
            LOGGER.warning("Empty command reward");
            return false;
        }
        return CommandUtils.executeCommand(player, command, executor);
    }

    public static boolean grantLootbag(Player player, String lootbagId) {
        if (lootbagId == null || lootbagId.isBlank()) {
            LOGGER.warning("Empty lootbag ID");
            return false;
        }
        return LootbagService.grant(player, lootbagId);
    }

    public static boolean grantMmoXp(Player player, String skill, int amount) {
        if (skill == null || skill.isBlank() || amount <= 0) {
            LOGGER.warning("Invalid MMO XP reward: skill=" + skill + " amount=" + amount);
            return false;
        }
        String command = "mmoskill addxp " + player.getDisplayName() + " " + skill + " " + amount;
        boolean success = CommandUtils.executeAsConsole(player, command);
        if (success && CoreAPI.isDebug()) {
            LOGGER.info("Granted " + amount + " " + skill + " MMO XP to " + player.getDisplayName());
        }
        return success;
    }

    public static boolean grantRpgXp(Player player, int amount) {
        if (amount <= 0) {
            return false;
        }
        if (!RewardGrantHelper.initRpgApi()) {
            LOGGER.fine("RPGLevelingAPI not available for RPG XP reward");
            return false;
        }
        try {
            UUID uuid = player.getPlayerRef().getUuid();
            if (rpgAddXpMethod != null && rpgQuestXpSource != null) {
                rpgAddXpMethod.invoke(rpgApiInstance, uuid, amount, rpgQuestXpSource);
            } else if (rpgAddXpSimpleMethod != null) {
                rpgAddXpSimpleMethod.invoke(rpgApiInstance, uuid, amount);
            } else {
                LOGGER.warning("No addXP method found on RPGLevelingAPI");
                return false;
            }
            if (CoreAPI.isDebug()) {
                LOGGER.info("Granted " + amount + " RPG XP to " + player.getDisplayName() + " via RPGLevelingAPI");
            }
            return true;
        }
        catch (Exception e) {
            LOGGER.warning("Failed to grant RPG XP via API: " + e.getMessage());
            return false;
        }
    }

    private static boolean initRpgApi() {
        if (rpgApiChecked) {
            return rpgApiAvailable;
        }
        rpgApiChecked = true;
        try {
            block10: {
                Class<?> apiClass = Class.forName("org.zuxaw.plugin.api.RPGLevelingAPI");
                Method isAvailable = apiClass.getMethod("isAvailable", new Class[0]);
                if (!((Boolean)isAvailable.invoke(null, new Object[0])).booleanValue()) {
                    LOGGER.info("RPGLevelingAPI.isAvailable() = false");
                    return false;
                }
                Method getApi = apiClass.getMethod("get", new Class[0]);
                rpgApiInstance = getApi.invoke(null, new Object[0]);
                if (rpgApiInstance == null) {
                    LOGGER.info("RPGLevelingAPI.get() returned null");
                    return false;
                }
                Class<?> xpSourceClass = Class.forName("org.zuxaw.plugin.api.XPSource");
                try {
                    rpgAddXpMethod = apiClass.getMethod("addXP", UUID.class, Double.TYPE, xpSourceClass);
                    Method createSource = xpSourceClass.getMethod("create", String.class);
                    rpgQuestXpSource = createSource.invoke(null, "QUEST");
                    LOGGER.info("RPGLevelingAPI: Using addXP(UUID, double, XPSource) with QUEST source");
                }
                catch (NoSuchMethodException e) {
                    LOGGER.info("RPGLevelingAPI: 3-arg addXP not found, trying 2-arg");
                }
                try {
                    rpgAddXpSimpleMethod = apiClass.getMethod("addXP", UUID.class, Double.TYPE);
                }
                catch (NoSuchMethodException e) {
                    if (rpgAddXpMethod != null) break block10;
                    LOGGER.warning("RPGLevelingAPI: No addXP method found at all");
                    return false;
                }
            }
            rpgApiAvailable = true;
            LOGGER.info("RPGLevelingAPI integration initialized for rewards");
            return true;
        }
        catch (ClassNotFoundException e) {
            LOGGER.info("RPGLeveling not installed (class not found)");
            return false;
        }
        catch (Exception e) {
            LOGGER.warning("Failed to init RPGLevelingAPI: " + e.getMessage());
            return false;
        }
    }

    public static boolean grantEndlessXp(Player player, int amount) {
        if (amount <= 0) {
            return false;
        }
        if (!RewardGrantHelper.initEndlessApi()) {
            LOGGER.fine("EndlessLevelingAPI not available for Endless XP reward");
            return false;
        }
        try {
            UUID uuid = player.getPlayerRef().getUuid();
            endlessGrantXpMethod.invoke(endlessApiInstance, uuid, amount);
            if (CoreAPI.isDebug()) {
                LOGGER.info("Granted " + amount + " Endless XP to " + player.getDisplayName() + " via EndlessLevelingAPI");
            }
            return true;
        }
        catch (Exception e) {
            LOGGER.warning("Failed to grant Endless XP via API: " + e.getMessage());
            return false;
        }
    }

    private static boolean initEndlessApi() {
        if (endlessApiChecked) {
            return endlessApiAvailable;
        }
        endlessApiChecked = true;
        try {
            Class<?> apiClass = Class.forName("com.airijko.endlessleveling.api.EndlessLevelingAPI");
            Method getApi = apiClass.getMethod("get", new Class[0]);
            endlessApiInstance = getApi.invoke(null, new Object[0]);
            if (endlessApiInstance == null) {
                LOGGER.info("EndlessLevelingAPI.get() returned null");
                return false;
            }
            endlessGrantXpMethod = apiClass.getMethod("grantXp", UUID.class, Double.TYPE);
            endlessApiAvailable = true;
            LOGGER.info("EndlessLevelingAPI integration initialized for rewards");
            return true;
        }
        catch (ClassNotFoundException e) {
            LOGGER.info("EndlessLeveling not installed (class not found)");
            return false;
        }
        catch (Exception e) {
            LOGGER.warning("Failed to init EndlessLevelingAPI: " + e.getMessage());
            return false;
        }
    }

    public static boolean grantLevelingXp(Player player, long amount) {
        if (amount <= 0L) {
            return false;
        }
        if (!RewardGrantHelper.initLevelingCoreApi()) {
            LOGGER.fine("LevelingCore not available for XP reward");
            return false;
        }
        try {
            UUID uuid = player.getPlayerRef().getUuid();
            lcAddXpMethod.invoke(lcServiceInstance, uuid, amount);
            if (CoreAPI.isDebug()) {
                LOGGER.info("Granted " + amount + " LevelingCore XP to " + player.getDisplayName());
            }
            return true;
        }
        catch (Exception e) {
            LOGGER.warning("Failed to grant LevelingCore XP: " + e.getMessage());
            return false;
        }
    }

    public static boolean removeLevelingXp(Player player, long amount) {
        if (amount <= 0L) {
            return false;
        }
        if (!RewardGrantHelper.initLevelingCoreApi()) {
            LOGGER.fine("LevelingCore not available for XP removal");
            return false;
        }
        try {
            UUID uuid = player.getPlayerRef().getUuid();
            lcRemoveXpMethod.invoke(lcServiceInstance, uuid, amount);
            if (CoreAPI.isDebug()) {
                LOGGER.info("Removed " + amount + " LevelingCore XP from " + player.getDisplayName());
            }
            return true;
        }
        catch (Exception e) {
            LOGGER.warning("Failed to remove LevelingCore XP: " + e.getMessage());
            return false;
        }
    }

    private static boolean initLevelingCoreApi() {
        if (lcApiChecked) {
            return lcApiAvailable;
        }
        try {
            Class<?> apiClass = Class.forName("com.azuredoom.levelingcore.LevelingCoreApi");
            Method getService = apiClass.getMethod("getLevelServiceIfPresent", new Class[0]);
            Object optional = getService.invoke(null, new Object[0]);
            Method isPresent = optional.getClass().getMethod("isPresent", new Class[0]);
            if (!((Boolean)isPresent.invoke(optional, new Object[0])).booleanValue()) {
                LOGGER.fine("LevelingCore: LevelService not present yet, will retry");
                return false;
            }
            Method getMethod = optional.getClass().getMethod("get", new Class[0]);
            lcServiceInstance = getMethod.invoke(optional, new Object[0]);
            Class<?> serviceClass = lcServiceInstance.getClass();
            try {
                lcAddXpMethod = serviceClass.getMethod("addXp", UUID.class, Long.TYPE);
            }
            catch (NoSuchMethodException e) {
                for (Class<?> iface : serviceClass.getInterfaces()) {
                    try {
                        lcAddXpMethod = iface.getMethod("addXp", UUID.class, Long.TYPE);
                        break;
                    }
                    catch (NoSuchMethodException noSuchMethodException) {
                    }
                }
            }
            try {
                lcRemoveXpMethod = serviceClass.getMethod("removeXp", UUID.class, Long.TYPE);
            }
            catch (NoSuchMethodException e) {
                for (Class<?> iface : serviceClass.getInterfaces()) {
                    try {
                        lcRemoveXpMethod = iface.getMethod("removeXp", UUID.class, Long.TYPE);
                        break;
                    }
                    catch (NoSuchMethodException noSuchMethodException) {
                    }
                }
            }
            if (lcAddXpMethod == null) {
                LOGGER.warning("LevelingCore: addXp method not found on LevelService");
                lcApiChecked = true;
                return false;
            }
            lcApiAvailable = true;
            lcApiChecked = true;
            LOGGER.info("LevelingCore integration initialized for rewards");
            return true;
        }
        catch (ClassNotFoundException e) {
            lcApiChecked = true;
            LOGGER.info("LevelingCore not installed (class not found)");
            return false;
        }
        catch (Exception e) {
            LOGGER.warning("Failed to init LevelingCore API: " + e.getMessage());
            return false;
        }
    }

    public static boolean grantCurrency(Player player, String currency, int amount) {
        if (amount <= 0) {
            return false;
        }
        ExternalEconomyBridge bridge = ExternalEconomyBridge.getInstance();
        if (bridge.isAvailable()) {
            UUID uuid = player.getPlayerRef().getUuid();
            boolean success = bridge.deposit(uuid, amount);
            if (success) {
                LOGGER.fine("Granted " + amount + " " + bridge.getCurrencyName() + " to " + player.getDisplayName() + " via " + String.valueOf((Object)bridge.getActiveBackend()));
            } else {
                LOGGER.warning("Failed to grant " + amount + " currency to " + player.getDisplayName());
            }
            return success;
        }
        String command = "eco give " + player.getDisplayName() + " " + amount;
        boolean success = CommandUtils.executeAsConsole(player, command);
        if (success) {
            LOGGER.fine("Granted " + amount + " " + (currency != null ? currency : "gold") + " to " + player.getDisplayName() + " via command");
        }
        return success;
    }

    static {
        endlessApiChecked = false;
        endlessApiAvailable = false;
        lcApiChecked = false;
        lcApiAvailable = false;
    }
}

