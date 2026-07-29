/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Map;
import java.util.logging.Logger;

public class SmartConfigManager {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft SmartConfig");

    public static <T> T loadAndMerge(Path file, Class<T> type, T defaults, Gson gson) {
        if (gson == null) {
            gson = new GsonBuilder().setPrettyPrinting().create();
        }
        if (!Files.exists(file, new LinkOption[0])) {
            SmartConfigManager.save(file, defaults, gson);
            return defaults;
        }
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            JsonObject existingJson = JsonParser.parseString(content).getAsJsonObject();
            JsonObject defaultsJson = gson.toJsonTree(defaults).getAsJsonObject();
            boolean changed = SmartConfigManager.mergeDefaults(existingJson, defaultsJson);
            T result = gson.fromJson((JsonElement)existingJson, type);
            if (changed) {
                String merged = gson.toJson(existingJson);
                Files.writeString(file, (CharSequence)merged, StandardCharsets.UTF_8, new OpenOption[0]);
                LOGGER.info("Config updated with new default fields: " + String.valueOf(file.getFileName()));
            }
            return result;
        }
        catch (Exception e) {
            LOGGER.warning("Failed to load config " + String.valueOf(file) + ", using defaults: " + e.getMessage());
            SmartConfigManager.save(file, defaults, gson);
            return defaults;
        }
    }

    private static boolean mergeDefaults(JsonObject existing, JsonObject defaults) {
        boolean changed = false;
        for (Map.Entry<String, JsonElement> entry : defaults.entrySet()) {
            String key = entry.getKey();
            JsonElement defaultValue = entry.getValue();
            if (!existing.has(key)) {
                existing.add(key, defaultValue.deepCopy());
                changed = true;
                continue;
            }
            if (!defaultValue.isJsonObject() || !existing.get(key).isJsonObject() || !SmartConfigManager.mergeDefaults(existing.getAsJsonObject(key), defaultValue.getAsJsonObject())) continue;
            changed = true;
        }
        return changed;
    }

    public static <T> void save(Path file, T config, Gson gson) {
        try {
            Files.createDirectories(file.getParent(), new FileAttribute[0]);
            String json = gson.toJson(config);
            Files.writeString(file, (CharSequence)json, StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (IOException e) {
            LOGGER.warning("Failed to save config " + String.valueOf(file) + ": " + e.getMessage());
        }
    }
}

