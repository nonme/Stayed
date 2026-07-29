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
import java.util.logging.Level;

public final class ReviveConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int DEFAULT_REVIVE_COST_AMOUNT = 0;
    private static final String DEFAULT_REVIVE_COST_ITEM = "";
    private static final int DEFAULT_AUTO_REVIVE_COOLDOWN_SECONDS = 0;
    public static int REVIVE_COST_AMOUNT = 0;
    public static String REVIVE_COST_ITEM = "";
    public static int AUTO_REVIVE_COOLDOWN_SECONDS = 0;

    private ReviveConfig() {
    }

    public static void loadOrCreate(Path filePath, HytaleLogger logger) {
        block14: {
            REVIVE_COST_AMOUNT = 0;
            REVIVE_COST_ITEM = DEFAULT_REVIVE_COST_ITEM;
            AUTO_REVIVE_COOLDOWN_SECONDS = 0;
            if (filePath == null) {
                return;
            }
            try {
                Path parent = filePath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent, new FileAttribute[0]);
                }
                if (!Files.exists(filePath, new LinkOption[0])) {
                    ReviveConfig.writeDefaultConfig(filePath);
                    if (logger != null) {
                        logger.at(Level.INFO).log("Created default revive config at " + String.valueOf(filePath));
                    }
                    return;
                }
                try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);){
                    JsonElement root = JsonParser.parseReader((Reader)reader);
                    if (root == null || !root.isJsonObject()) {
                        throw new IOException("Revive config root must be a JSON object.");
                    }
                    JsonObject rootObject = root.getAsJsonObject();
                    ReviveConfig.apply(rootObject);
                    if (ReviveConfig.needsRewrite(rootObject)) {
                        ReviveConfig.writeConfig(filePath);
                    }
                }
            }
            catch (Throwable t) {
                if (logger == null) break block14;
                ((HytaleLogger.Api)logger.at(Level.WARNING).withCause(t)).log("Failed to load revive config. Using defaults.");
            }
        }
    }

    private static void writeDefaultConfig(Path filePath) throws IOException {
        JsonObject root = ReviveConfig.buildJson(0, DEFAULT_REVIVE_COST_ITEM, 0);
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8, new OpenOption[0]);){
            GSON.toJson((JsonElement)root, (Appendable)writer);
        }
    }

    private static void writeConfig(Path filePath) throws IOException {
        JsonObject root = ReviveConfig.buildJson(REVIVE_COST_AMOUNT, REVIVE_COST_ITEM, AUTO_REVIVE_COOLDOWN_SECONDS);
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8, new OpenOption[0]);){
            GSON.toJson((JsonElement)root, (Appendable)writer);
        }
    }

    private static void apply(JsonObject root) {
        REVIVE_COST_AMOUNT = Math.max(0, ReviveConfig.getInt(root, "reviveCostAmount", 0));
        REVIVE_COST_ITEM = ReviveConfig.getString(root, "reviveCostItem", DEFAULT_REVIVE_COST_ITEM).trim();
        AUTO_REVIVE_COOLDOWN_SECONDS = Math.max(0, ReviveConfig.getInt(root, "autoReviveCooldownSeconds", 0));
        if (REVIVE_COST_AMOUNT <= 0 || REVIVE_COST_ITEM.isBlank()) {
            REVIVE_COST_AMOUNT = 0;
            REVIVE_COST_ITEM = DEFAULT_REVIVE_COST_ITEM;
        }
    }

    private static JsonObject buildJson(int reviveCostAmount, String reviveCostItem, int autoReviveCooldownSeconds) {
        JsonObject root = new JsonObject();
        root.addProperty("reviveCostAmount", (Number)Math.max(0, reviveCostAmount));
        root.addProperty("reviveCostItem", reviveCostItem == null ? DEFAULT_REVIVE_COST_ITEM : reviveCostItem);
        root.addProperty("autoReviveCooldownSeconds", (Number)Math.max(0, autoReviveCooldownSeconds));
        return root;
    }

    private static boolean needsRewrite(JsonObject root) {
        if (!root.has("reviveCostAmount")) {
            return true;
        }
        if (!root.has("reviveCostItem")) {
            return true;
        }
        if (!root.has("autoReviveCooldownSeconds")) {
            return true;
        }
        int currentAmount = ReviveConfig.getInt(root, "reviveCostAmount", 0);
        String currentItem = ReviveConfig.getString(root, "reviveCostItem", DEFAULT_REVIVE_COST_ITEM).trim();
        int currentCooldown = ReviveConfig.getInt(root, "autoReviveCooldownSeconds", 0);
        return currentAmount != REVIVE_COST_AMOUNT || !currentItem.equals(REVIVE_COST_ITEM) || currentCooldown != AUTO_REVIVE_COOLDOWN_SECONDS;
    }

    public static boolean hasReviveCost() {
        return REVIVE_COST_AMOUNT > 0 && REVIVE_COST_ITEM != null && !REVIVE_COST_ITEM.isBlank();
    }

    public static String getReviveCostText() {
        if (!ReviveConfig.hasReviveCost()) {
            return "none";
        }
        return REVIVE_COST_AMOUNT + " " + REVIVE_COST_ITEM;
    }

    public static boolean hasAutoReviveCooldown() {
        return AUTO_REVIVE_COOLDOWN_SECONDS > 0;
    }

    public static long getAutoReviveCooldownTicks() {
        return Math.max(0L, (long)AUTO_REVIVE_COOLDOWN_SECONDS) * 20L;
    }

    private static int getInt(JsonObject root, String key, int fallback) {
        try {
            if (root.has(key) && root.get(key).isJsonPrimitive()) {
                return root.get(key).getAsInt();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return fallback;
    }

    private static String getString(JsonObject root, String key, String fallback) {
        try {
            if (root.has(key) && root.get(key).isJsonPrimitive()) {
                return root.get(key).getAsString();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return fallback;
    }
}

