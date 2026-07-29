/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.hypixel.hytale.logger.HytaleLogger
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 */
package com.yourname.companion.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.logging.Level;

public final class ProgressionConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String[] DEFAULT_STARTING_GEAR = new String[]{"Tool_Pickaxe_Crude:1"};
    public static String[] STARTING_GEAR = ProgressionConfig.clone1d(DEFAULT_STARTING_GEAR);
    public static final int MAX_COMBAT_LEVEL = 10;
    public static final int[] COMBAT_LEVEL_THRESHOLDS = new int[]{0, 3, 8, 20, 40, 60, 70, 80, 90, 100};
    public static final String[] COMBAT_LEVEL_NAMES = new String[]{"Recruit", "Apprentice", "Warrior", "Veteran", "Champion", "Legend", "Elite", "Master", "Mythic", "Ascendant"};
    public static final double[] COMBAT_DAMAGE_MULT = new double[]{1.0, 1.0, 1.1, 1.2, 1.3, 1.5, 1.5, 1.5, 1.65, 1.815};
    private static final String[][] DEFAULT_COMBAT_REWARDS = new String[][]{new String[0], {"Furniture_Crude_Torch:1"}, {"Armor_Copper_Chest:1", "Armor_Copper_Legs:1", "Armor_Copper_Head:1", "Armor_Copper_Hands:1"}, {"Armor_Iron_Chest:1", "Armor_Iron_Legs:1", "Armor_Iron_Head:1", "Armor_Iron_Hands:1"}, {"Armor_Cobalt_Chest:1", "Armor_Cobalt_Legs:1", "Armor_Cobalt_Head:1", "Armor_Cobalt_Hands:1", "Weapon_Crossbow:1", "Weapon_Arrow_Crude:128", "Weapon_Sword_Cobalt:1"}, {"Armor_Thorium_Chest:1", "Armor_Thorium_Legs:1", "Armor_Thorium_Head:1", "Armor_Thorium_Hands:1", "Weapon_Sword_Thorium:1"}, {"Weapon_Sword_Adamantite:1", "Weapon_Crossbow_Ancient_Steel:1", "Armor_Adamantite_Chest:1", "Armor_Adamantite_Legs:1", "Armor_Adamantite_Head:1", "Armor_Adamantite_Hands:1", "Weapon_Shield_Adamantite:1", "Weapon_Gun:1", "Weapon_Arrow_Crude:100", "Weapon_Arrow_Crude:100"}, {"Armor_Mithril_Chest:1", "Armor_Mithril_Legs:1", "Armor_Mithril_Head:1", "Armor_Mithril_Hands:1", "Weapon_Sword_Mithril:1", "Weapon_Shield_Mithril:1"}, {"Weapon_Longsword_Scarab:1"}, {"Weapon_Assault_Rifle:1", "Weapon_Arrow_Crude:100", "Weapon_Arrow_Crude:100"}};
    public static String[][] COMBAT_REWARDS = ProgressionConfig.clone2d(DEFAULT_COMBAT_REWARDS);
    public static final int MAX_FARM_LEVEL = 10;
    public static final int[] FARM_LEVEL_THRESHOLDS = new int[]{0, 50, 125, 240, 410, 665, 1050, 1625, 2490, 3790};
    public static final String[] FARM_LEVEL_NAMES = new String[]{"Novice", "Apprentice", "Green Thumb", "Master Farmer", "Harvest Lord", "Field Marshal", "Crop Savant", "Agri Virtuoso", "Land Titan", "Eternal Harvester"};
    private static final double[] DEFAULT_FARM_SPEED_MULT = new double[]{1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9};
    public static double[] FARM_SPEED_MULT = ProgressionConfig.clone1d(DEFAULT_FARM_SPEED_MULT);
    private static final double[] DEFAULT_FARM_ETERNAL_SEED_CHANCE = new double[]{0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9};
    public static double[] FARM_ETERNAL_SEED_CHANCE = ProgressionConfig.clone1d(DEFAULT_FARM_ETERNAL_SEED_CHANCE);
    public static final boolean[] FARM_AUTO_REPLANT = new boolean[]{false, false, false, true, true, true, true, true, true, true};
    public static final boolean[] FARM_DOUBLE_YIELD = new boolean[]{false, false, false, false, true, true, true, true, true, true};
    private static final String[][] DEFAULT_FARM_REWARDS = new String[][]{new String[0], {"Seed_Wheat:10", "Seed_Carrot:10"}, {"Seed_Wheat:20", "Seed_Carrot:20", "Seed_Potato:10"}, {"Seed_Wheat:40", "Seed_Carrot:40", "Seed_Potato:20"}, {"Seed_Wheat:60", "Seed_Carrot:60", "Seed_Potato:40"}, new String[0], new String[0], new String[0], new String[0], new String[0]};
    public static String[][] FARM_REWARDS = ProgressionConfig.clone2d(DEFAULT_FARM_REWARDS);
    public static final int MAX_MINE_LEVEL = 10;
    public static final int[] MINE_LEVEL_THRESHOLDS = new int[]{0, 50, 125, 240, 410, 665, 1050, 1625, 2490, 3790};
    public static final String[] MINE_LEVEL_NAMES = new String[]{"Novice", "Prospector", "Excavator", "Master Miner", "Earth Shaper", "Tunnel Smith", "Bedrock Seeker", "Vein Hunter", "Deep Delver", "Mountainbreaker"};
    private static final double[] DEFAULT_MINE_SPEED_MULT = new double[]{0.85, 0.9, 0.95, 1.0, 1.05, 1.1, 1.15, 1.2, 1.25, 1.3};
    public static double[] MINE_SPEED_MULT = ProgressionConfig.clone1d(DEFAULT_MINE_SPEED_MULT);
    public static final double[] MINE_MOVE_SPEED_MULT = new double[]{1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9};

    private ProgressionConfig() {
    }

    public static int combatLevelForKills(int kills) {
        for (int i = COMBAT_LEVEL_THRESHOLDS.length - 1; i >= 0; --i) {
            if (kills < COMBAT_LEVEL_THRESHOLDS[i]) continue;
            return i + 1;
        }
        return 1;
    }

    public static int farmLevelForHarvests(int harvests) {
        for (int i = FARM_LEVEL_THRESHOLDS.length - 1; i >= 0; --i) {
            if (harvests < FARM_LEVEL_THRESHOLDS[i]) continue;
            return i + 1;
        }
        return 1;
    }

    public static int killsToNextCombatLevel(int currentLevel, int currentKills) {
        if (currentLevel >= 10) {
            return -1;
        }
        return COMBAT_LEVEL_THRESHOLDS[currentLevel] - currentKills;
    }

    public static int harvestsToNextFarmLevel(int currentLevel, int currentHarvests) {
        if (currentLevel >= 10) {
            return -1;
        }
        return FARM_LEVEL_THRESHOLDS[currentLevel] - currentHarvests;
    }

    public static int mineLevelForBlocks(int blocks) {
        for (int i = MINE_LEVEL_THRESHOLDS.length - 1; i >= 0; --i) {
            if (blocks < MINE_LEVEL_THRESHOLDS[i]) continue;
            return i + 1;
        }
        return 1;
    }

    public static int blocksToNextMineLevel(int currentLevel, int currentBlocks) {
        if (currentLevel >= 10) {
            return -1;
        }
        return MINE_LEVEL_THRESHOLDS[currentLevel] - currentBlocks;
    }

    public static void loadOrCreate(Path filePath, HytaleLogger logger) {
        block13: {
            ProgressionConfig.resetRewardDefaults();
            if (filePath == null) {
                return;
            }
            try {
                Path parent = filePath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent, new FileAttribute[0]);
                }
                if (!Files.exists(filePath, new LinkOption[0])) {
                    ProgressionConfig.writeDefaultConfig(filePath);
                    if (logger != null) {
                        logger.at(Level.INFO).log("Created default progression config at " + String.valueOf(filePath));
                    }
                    return;
                }
                try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);){
                    JsonElement root = JsonParser.parseReader((Reader)reader);
                    if (root == null || !root.isJsonObject()) {
                        throw new IOException("Progression config root must be a JSON object.");
                    }
                    ProgressionConfig.applyConfig(root.getAsJsonObject(), logger);
                }
            }
            catch (Throwable t) {
                ProgressionConfig.resetRewardDefaults();
                if (logger == null) break block13;
                ((HytaleLogger.Api)logger.at(Level.WARNING).withCause(t)).log("Failed to load progression config; using defaults.");
            }
        }
    }

    private static void applyConfig(JsonObject root, HytaleLogger logger) {
        STARTING_GEAR = ProgressionConfig.readRewardList(root, "startingGear", DEFAULT_STARTING_GEAR);
        COMBAT_REWARDS = ProgressionConfig.readRewardTable(root, "combatRewards", DEFAULT_COMBAT_REWARDS, 10);
        FARM_REWARDS = ProgressionConfig.readRewardTable(root, "farmRewards", DEFAULT_FARM_REWARDS, 10);
        FARM_SPEED_MULT = ProgressionConfig.readDoubleArray(root, "farmSpeedMult", DEFAULT_FARM_SPEED_MULT, 10);
        FARM_ETERNAL_SEED_CHANCE = ProgressionConfig.readDoubleArray(root, "farmEternalSeedChance", DEFAULT_FARM_ETERNAL_SEED_CHANCE, 10);
        MINE_SPEED_MULT = ProgressionConfig.readDoubleArray(root, "mineSpeedMult", DEFAULT_MINE_SPEED_MULT, 10);
        if (logger != null) {
            logger.at(Level.INFO).log("Loaded progression config overrides.");
        }
    }

    private static void writeDefaultConfig(Path filePath) throws IOException {
        JsonObject root = new JsonObject();
        root.add("startingGear", GSON.toJsonTree((Object)DEFAULT_STARTING_GEAR));
        root.add("combatRewards", GSON.toJsonTree((Object)DEFAULT_COMBAT_REWARDS));
        root.add("farmRewards", GSON.toJsonTree((Object)DEFAULT_FARM_REWARDS));
        root.add("farmSpeedMult", GSON.toJsonTree((Object)DEFAULT_FARM_SPEED_MULT));
        root.add("farmEternalSeedChance", GSON.toJsonTree((Object)DEFAULT_FARM_ETERNAL_SEED_CHANCE));
        root.add("mineSpeedMult", GSON.toJsonTree((Object)DEFAULT_MINE_SPEED_MULT));
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8, new OpenOption[0]);){
            GSON.toJson((JsonElement)root, (Appendable)writer);
        }
    }

    private static void resetRewardDefaults() {
        STARTING_GEAR = ProgressionConfig.clone1d(DEFAULT_STARTING_GEAR);
        COMBAT_REWARDS = ProgressionConfig.clone2d(DEFAULT_COMBAT_REWARDS);
        FARM_REWARDS = ProgressionConfig.clone2d(DEFAULT_FARM_REWARDS);
        FARM_SPEED_MULT = ProgressionConfig.clone1d(DEFAULT_FARM_SPEED_MULT);
        FARM_ETERNAL_SEED_CHANCE = ProgressionConfig.clone1d(DEFAULT_FARM_ETERNAL_SEED_CHANCE);
        MINE_SPEED_MULT = ProgressionConfig.clone1d(DEFAULT_MINE_SPEED_MULT);
    }

    private static String[] readRewardList(JsonObject root, String key, String[] defaults) {
        if (root == null || !root.has(key)) {
            return ProgressionConfig.clone1d(defaults);
        }
        try {
            String[] parsed = (String[])GSON.fromJson(root.get(key), String[].class);
            return parsed != null ? parsed : ProgressionConfig.clone1d(defaults);
        }
        catch (Throwable ignored) {
            return ProgressionConfig.clone1d(defaults);
        }
    }

    private static String[][] readRewardTable(JsonObject root, String key, String[][] defaults, int expectedLevels) {
        if (root == null || !root.has(key)) {
            return ProgressionConfig.clone2d(defaults);
        }
        try {
            JsonElement element = root.get(key);
            if (element == null || !element.isJsonArray()) {
                return ProgressionConfig.clone2d(defaults);
            }
            ArrayList rows = new ArrayList();
            element.getAsJsonArray().forEach(entry -> {
                try {
                    String[] parsed = (String[])GSON.fromJson(entry, String[].class);
                    rows.add(parsed != null ? parsed : new String[]{});
                }
                catch (Throwable ignored) {
                    rows.add(new String[0]);
                }
            });
            if (rows.size() != expectedLevels) {
                return ProgressionConfig.clone2d(defaults);
            }
            return (String[][])rows.toArray((T[])new String[0][]);
        }
        catch (Throwable ignored) {
            return ProgressionConfig.clone2d(defaults);
        }
    }

    private static double[] readDoubleArray(JsonObject root, String key, double[] defaults, int expectedLevels) {
        if (root == null || !root.has(key)) {
            return ProgressionConfig.clone1d(defaults);
        }
        try {
            double[] parsed = (double[])GSON.fromJson(root.get(key), double[].class);
            if (parsed == null || parsed.length != expectedLevels) {
                return ProgressionConfig.clone1d(defaults);
            }
            return parsed;
        }
        catch (Throwable ignored) {
            return ProgressionConfig.clone1d(defaults);
        }
    }

    private static double[] clone1d(double[] source) {
        return source != null ? (double[])source.clone() : new double[]{};
    }

    private static String[] clone1d(String[] source) {
        return source != null ? (String[])source.clone() : new String[]{};
    }

    private static String[][] clone2d(String[][] source) {
        if (source == null) {
            return new String[0][];
        }
        String[][] out = new String[source.length][];
        for (int i = 0; i < source.length; ++i) {
            out[i] = source[i] != null ? (String[])source[i].clone() : new String[]{};
        }
        return out;
    }
}

