/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.lootbag;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kyuubisoft.core.lootbag.LootbagDefinition;
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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class LootbagConfig {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private Map<String, LootbagDefinition> lootbags = new HashMap<String, LootbagDefinition>();
    private int configVersion = 0;

    public static LootbagConfig loadFromFile(Path configFile) {
        LootbagConfig config = new LootbagConfig();
        if (configFile == null || !Files.exists(configFile, new LinkOption[0])) {
            return config;
        }
        try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8);){
            LootbagConfig loaded = GSON.fromJson((Reader)reader, LootbagConfig.class);
            if (loaded != null && loaded.lootbags != null) {
                for (Map.Entry<String, LootbagDefinition> entry : loaded.lootbags.entrySet()) {
                    entry.getValue().setId(entry.getKey());
                }
                config.lootbags.putAll(loaded.lootbags);
                config.configVersion = loaded.configVersion;
            }
        }
        catch (Exception e) {
            LOGGER.warning("Failed to load lootbag config from " + String.valueOf(configFile) + ": " + e.getMessage());
        }
        return config;
    }

    public void merge(LootbagConfig other) {
        if (other == null || other.lootbags == null) {
            return;
        }
        for (Map.Entry<String, LootbagDefinition> entry : other.lootbags.entrySet()) {
            entry.getValue().setId(entry.getKey());
            this.lootbags.put(entry.getKey(), entry.getValue());
        }
    }

    public void save(Path configFile) {
        try {
            Files.createDirectories(configFile.getParent(), new FileAttribute[0]);
            try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8, new OpenOption[0]);){
                GSON.toJson((Object)this, (Appendable)writer);
            }
        }
        catch (IOException e) {
            LOGGER.warning("Failed to save lootbag config: " + e.getMessage());
        }
    }

    public LootbagDefinition getLootbag(String lootbagId) {
        return this.lootbags.get(lootbagId);
    }

    public boolean hasLootbag(String lootbagId) {
        return this.lootbags.containsKey(lootbagId);
    }

    public Map<String, LootbagDefinition> getAllLootbags() {
        return this.lootbags;
    }

    public void addLootbag(String id, LootbagDefinition definition) {
        definition.setId(id);
        this.lootbags.put(id, definition);
    }

    public void removeLootbag(String id) {
        this.lootbags.remove(id);
    }

    public int getConfigVersion() {
        return this.configVersion;
    }

    public void setConfigVersion(int configVersion) {
        this.configVersion = configVersion;
    }
}

