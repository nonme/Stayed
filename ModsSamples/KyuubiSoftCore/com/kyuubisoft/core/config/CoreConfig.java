/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kyuubisoft.core.webeditor.WebEditorConfig;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CoreConfig {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path dataFolder;
    private ConfigData data;

    public CoreConfig(Path dataFolder) {
        this.dataFolder = dataFolder;
        this.data = new ConfigData();
    }

    public void load() {
        Path configFile;
        block31: {
            configFile = this.dataFolder.resolve("config.json");
            if (Files.exists(configFile, new LinkOption[0])) {
                try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8);){
                    ConfigData loaded = GSON.fromJson((Reader)reader, ConfigData.class);
                    if (loaded != null) {
                        boolean migrated = false;
                        if (loaded.webEditor == null) {
                            loaded.webEditor = new WebEditorConfig();
                            migrated = true;
                        }
                        if (loaded.storage == null) {
                            loaded.storage = new StorageConfig();
                            migrated = true;
                        }
                        if (loaded.modules == null) {
                            loaded.modules = new ModulesConfig();
                            migrated = true;
                        }
                        if (loaded.webEditor.isEnabled() && !loaded.modules.webEditor) {
                            loaded.modules.webEditor = true;
                            migrated = true;
                        }
                        this.data = loaded;
                        if (migrated) {
                            this.save();
                            LOGGER.info("Core config migrated (added missing sections)");
                        }
                        LOGGER.info("Core config loaded (logLevel: " + this.getLogLevel() + ")");
                        return;
                    }
                }
                catch (Exception e) {
                    LOGGER.warning("Failed to load config from file: " + e.getMessage());
                }
            }
            try (InputStream is = this.getClass().getResourceAsStream("/config.json");){
                if (is == null) break block31;
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);){
                    ConfigData loaded = GSON.fromJson((Reader)reader, ConfigData.class);
                    if (loaded != null) {
                        this.data = loaded;
                        LOGGER.info("Core config loaded from JAR defaults (logLevel: " + this.getLogLevel() + ")");
                    }
                }
            }
            catch (Exception e) {
                LOGGER.warning("Failed to load config from JAR: " + e.getMessage());
            }
        }
        if (!Files.exists(configFile, new LinkOption[0])) {
            this.save();
        }
    }

    public void save() {
        try {
            Path configFile = this.dataFolder.resolve("config.json");
            Files.createDirectories(configFile.getParent(), new FileAttribute[0]);
            Files.writeString(configFile, (CharSequence)GSON.toJson(this.data), StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (Exception e) {
            LOGGER.warning("Failed to save config: " + e.getMessage());
        }
    }

    private ConfigData ensureData() {
        if (this.data == null) {
            this.data = new ConfigData();
        }
        return this.data;
    }

    public String getLanguage() {
        return this.ensureData().language;
    }

    public void setLanguage(String language) {
        this.ensureData().language = language;
    }

    public String getEconomyProvider() {
        return this.ensureData().economyProvider;
    }

    public String getLogLevel() {
        String level = this.ensureData().logLevel;
        return level != null ? level.toLowerCase() : "info";
    }

    public int getLogLevelNum() {
        return switch (this.getLogLevel()) {
            case "error" -> 0;
            case "warning" -> 1;
            case "all", "debug" -> 3;
            default -> 2;
        };
    }

    public String getTitle() {
        return this.ensureData().title;
    }

    public String getDescription() {
        return this.ensureData().description;
    }

    public List<LinkEntry> getLinks() {
        ArrayList links = this.ensureData().links;
        return links != null ? links : new ArrayList();
    }

    public ModulesConfig getModules() {
        ConfigData d = this.ensureData();
        if (d.modules == null) {
            d.modules = new ModulesConfig();
        }
        return d.modules;
    }

    public WebEditorConfig getWebEditor() {
        ConfigData d = this.ensureData();
        if (d.webEditor == null) {
            d.webEditor = new WebEditorConfig();
        }
        return d.webEditor;
    }

    public String getStorageType() {
        return this.ensureData().storage != null ? this.ensureData().storage.type : "file";
    }

    public StorageConfig getStorageConfig() {
        ConfigData d = this.ensureData();
        if (d.storage == null) {
            d.storage = new StorageConfig();
        }
        return d.storage;
    }

    public static class ConfigData {
        public String language = "en-US";
        public String economyProvider = "auto";
        public String logLevel = "info";
        public String title = "KyuubiSoft Server";
        public String description = "Willkommen im Admin-Bereich. Waehle ein Tool aus der Liste.";
        public List<LinkEntry> links = new ArrayList<LinkEntry>();
        public StorageConfig storage = new StorageConfig();
        public WebEditorConfig webEditor = new WebEditorConfig();
        public ModulesConfig modules = new ModulesConfig();
    }

    public static class StorageConfig {
        public String type = "file";
        public MySQLConfig mysql = new MySQLConfig();
    }

    public static class ModulesConfig {
        public boolean citizens = true;
        public boolean dialogs = true;
        public boolean shops = true;
        public boolean lootbags = true;
        public boolean tracking = true;
        public boolean webEditor = false;
        public boolean dynamicImages = true;
        public boolean playerPreferences = true;
    }

    public static class LinkEntry {
        public String name = "";
        public String url = "";
        public String description = "";
        public String color;
    }

    public static class MySQLConfig {
        public String host = "localhost";
        public int port = 3306;
        public String database = "kyuubisoft";
        public String username = "root";
        public String password = "";
        public String tablePrefix = "ks_";
        public int maxPoolSize = 10;
    }
}

