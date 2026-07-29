/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.lootbag;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kyuubisoft.core.lootbag.LootbagConfig;
import com.kyuubisoft.core.lootbag.LootbagDefinition;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class LootbagAdminService {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CURRENT_VERSION = 3;
    private static LootbagAdminService instance;
    private Path dataDirectory;
    private LootbagConfig standardConfig;
    private Map<String, LootbagDefinition> customLootbags = new LinkedHashMap<String, LootbagDefinition>();
    private LootbagConfig mergedConfig;

    public LootbagAdminService() {
        instance = this;
    }

    public static LootbagAdminService getInstance() {
        return instance;
    }

    public void load(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        Path configsDir = dataDirectory.resolve("configs");
        Path configFile = configsDir.resolve("lootbags.json");
        this.extractDefaultsIfNeeded(configsDir, configFile);
        this.standardConfig = LootbagConfig.loadFromFile(configFile);
        LOGGER.info("Loaded " + this.standardConfig.getAllLootbags().size() + " standard lootbags");
        this.loadCustomLootbags();
        this.rebuildMerged();
    }

    private void extractDefaultsIfNeeded(Path configsDir, Path configFile) {
        block18: {
            try {
                LootbagConfig existing;
                boolean needsExtract;
                Files.createDirectories(configsDir, new FileAttribute[0]);
                boolean bl = needsExtract = !Files.exists(configFile, new LinkOption[0]);
                if (!needsExtract && (existing = LootbagConfig.loadFromFile(configFile)).getConfigVersion() < 3) {
                    Path backup = configsDir.resolve("lootbags.json.backup-v" + existing.getConfigVersion());
                    Files.copy(configFile, backup, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("Backed up lootbags.json (v" + existing.getConfigVersion() + ")");
                    needsExtract = true;
                }
                if (needsExtract) {
                    try (InputStream is = this.getClass().getResourceAsStream("/defaults/lootbags.json");){
                        if (is != null) {
                            Files.copy(is, configFile, StandardCopyOption.REPLACE_EXISTING);
                            LOGGER.info("Extracted default lootbags.json (v3)");
                        }
                    }
                }
                Path customDir = this.dataDirectory.resolve("custom");
                Files.createDirectories(customDir, new FileAttribute[0]);
                Path exampleFile = customDir.resolve("custom_lootbags.json.example");
                if (Files.exists(exampleFile, new LinkOption[0])) break block18;
                LinkedHashMap<String, Object> example = new LinkedHashMap<String, Object>();
                example.put("configVersion", 3);
                example.put("_comment", "Custom lootbags override standard ones by ID. This file is NEVER overwritten on updates.");
                example.put("lootbags", new LinkedHashMap());
                try (BufferedWriter w = Files.newBufferedWriter(exampleFile, StandardCharsets.UTF_8, new OpenOption[0]);){
                    GSON.toJson(example, (Appendable)w);
                }
            }
            catch (IOException e) {
                LOGGER.warning("Failed to extract lootbag defaults: " + e.getMessage());
            }
        }
    }

    private void loadCustomLootbags() {
        this.customLootbags = new LinkedHashMap<String, LootbagDefinition>();
        Path customFile = this.dataDirectory.resolve("custom").resolve("custom_lootbags.json");
        if (!Files.exists(customFile, new LinkOption[0])) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(customFile, StandardCharsets.UTF_8);){
            LootbagConfig loaded = GSON.fromJson((Reader)reader, LootbagConfig.class);
            if (loaded != null && loaded.getAllLootbags() != null) {
                for (Map.Entry<String, LootbagDefinition> entry : loaded.getAllLootbags().entrySet()) {
                    entry.getValue().setId(entry.getKey());
                    this.customLootbags.put(entry.getKey(), entry.getValue());
                }
            }
            LOGGER.info("Loaded " + this.customLootbags.size() + " custom lootbags");
        }
        catch (Exception e) {
            LOGGER.severe("Failed to load custom lootbags: " + e.getMessage());
        }
    }

    private void rebuildMerged() {
        this.mergedConfig = new LootbagConfig();
        if (this.standardConfig != null) {
            this.mergedConfig.merge(this.standardConfig);
        }
        for (Map.Entry<String, LootbagDefinition> entry : this.customLootbags.entrySet()) {
            this.mergedConfig.addLootbag(entry.getKey(), entry.getValue());
        }
        this.mergedConfig.setConfigVersion(3);
    }

    public void saveCustomLootbags() {
        Path customFile = this.dataDirectory.resolve("custom").resolve("custom_lootbags.json");
        try {
            Files.createDirectories(customFile.getParent(), new FileAttribute[0]);
            LinkedHashMap<String, Object> wrapper = new LinkedHashMap<String, Object>();
            wrapper.put("configVersion", 3);
            wrapper.put("lootbags", this.customLootbags);
            try (BufferedWriter writer = Files.newBufferedWriter(customFile, StandardCharsets.UTF_8, new OpenOption[0]);){
                GSON.toJson(wrapper, (Appendable)writer);
            }
            LOGGER.info("Saved " + this.customLootbags.size() + " custom lootbags");
        }
        catch (Exception e) {
            LOGGER.severe("Failed to save custom lootbags: " + e.getMessage());
        }
        this.rebuildMerged();
    }

    public boolean isCustom(String id) {
        return this.customLootbags.containsKey(id);
    }

    public LootbagDefinition get(String id) {
        return this.mergedConfig != null ? this.mergedConfig.getLootbag(id) : null;
    }

    public Map<String, LootbagDefinition> getAll() {
        return this.mergedConfig != null ? this.mergedConfig.getAllLootbags() : new LinkedHashMap();
    }

    public Map<String, LootbagDefinition> getCustomLootbags() {
        return this.customLootbags;
    }

    public void addCustom(String id, LootbagDefinition def) {
        def.setId(id);
        this.customLootbags.put(id, def);
        this.rebuildMerged();
    }

    public void updateCustom(String oldId, String newId, LootbagDefinition def) {
        if (!oldId.equals(newId)) {
            this.customLootbags.remove(oldId);
        }
        def.setId(newId);
        this.customLootbags.put(newId, def);
        this.rebuildMerged();
    }

    public void removeCustom(String id) {
        this.customLootbags.remove(id);
        this.rebuildMerged();
    }

    public LootbagConfig getMergedConfig() {
        return this.mergedConfig;
    }

    public Path getDataDirectory() {
        return this.dataDirectory;
    }
}

