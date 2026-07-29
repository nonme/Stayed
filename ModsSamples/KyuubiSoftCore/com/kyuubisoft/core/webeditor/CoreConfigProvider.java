/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.webeditor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.kyuubisoft.core.CorePlugin;
import com.kyuubisoft.core.i18n.CoreI18n;
import com.kyuubisoft.core.webeditor.ModConfigProvider;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class CoreConfigProvider
implements ModConfigProvider {
    private static final Logger LOGGER = Logger.getLogger("Core WebEditor");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type STRING_MAP_TYPE = new TypeToken<Map<String, String>>(){}.getType();
    private final CorePlugin plugin;

    public CoreConfigProvider(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getModId() {
        return "core";
    }

    @Override
    public Map<String, JsonElement> exportConfigs() {
        LinkedHashMap<String, JsonElement> result = new LinkedHashMap<String, JsonElement>();
        Path dataFolder = this.plugin.getDataDirectory();
        this.readJsonFile(dataFolder.resolve("citizens.json"), "citizens.json", result);
        this.readJsonFile(dataFolder.resolve("custom").resolve("custom_citizens.json"), "custom/custom_citizens.json", result);
        this.readJsonFile(dataFolder.resolve("configs").resolve("lootbags.json"), "configs/lootbags.json", result);
        this.readJsonFile(dataFolder.resolve("custom").resolve("custom_lootbags.json"), "custom/custom_lootbags.json", result);
        Path shopsDir = dataFolder.resolve("shops");
        if (Files.isDirectory(shopsDir, new LinkOption[0])) {
            this.listJsonFiles(shopsDir).forEach(file -> {
                String relativeName = "shops/" + file.getFileName().toString();
                this.readJsonFile((Path)file, relativeName, (Map<String, JsonElement>)result);
            });
        }
        return result;
    }

    @Override
    public Map<String, Map<String, String>> exportLocalization() {
        LinkedHashMap<String, Map<String, String>> result = new LinkedHashMap<String, Map<String, String>>();
        Path localizationFolder = this.plugin.getDataDirectory().resolve("localization");
        if (!Files.isDirectory(localizationFolder, new LinkOption[0])) {
            return result;
        }
        try (Stream<Path> files = Files.list(localizationFolder);){
            files.filter(p -> {
                String name = p.getFileName().toString();
                return name.endsWith(".json") && !name.contains(".backup") && !name.contains(".example");
            }).sorted().forEach(file -> {
                try {
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    Map translations = (Map)GSON.fromJson(content, STRING_MAP_TYPE);
                    if (translations != null) {
                        String fileName = file.getFileName().toString();
                        String langCode = fileName.startsWith("custom_") ? fileName.substring(7, fileName.length() - 5) : fileName.substring(0, fileName.length() - 5);
                        result.computeIfAbsent(langCode, k -> new LinkedHashMap()).putAll(translations);
                    }
                }
                catch (Exception e) {
                    LOGGER.warning("Failed to read localization file " + String.valueOf(file) + ": " + e.getMessage());
                }
            });
        }
        catch (IOException e) {
            LOGGER.warning("Failed to list localization folder: " + e.getMessage());
        }
        return result;
    }

    @Override
    public void importConfig(String fileName, JsonElement data) {
        Path dataFolder = this.plugin.getDataDirectory();
        Path targetFile = dataFolder.resolve(fileName);
        try {
            Files.createDirectories(targetFile.getParent(), new FileAttribute[0]);
            if (fileName.contains("custom_citizens") && data.isJsonObject()) {
                this.mergeCitizensFile(targetFile, data.getAsJsonObject());
            } else if (fileName.contains("custom_lootbags") && data.isJsonObject()) {
                this.mergeLootbagsFile(targetFile, data.getAsJsonObject());
            } else if ("config.json".equals(fileName) && data.isJsonObject()) {
                this.mergeConfigJson(targetFile, data.getAsJsonObject());
            } else {
                String json = GSON.toJson(data);
                Files.writeString(targetFile, (CharSequence)json, StandardCharsets.UTF_8, new OpenOption[0]);
            }
            LOGGER.info("Imported config: " + fileName);
        }
        catch (IOException e) {
            LOGGER.warning("Failed to import config " + fileName + ": " + e.getMessage());
        }
    }

    private void mergeConfigJson(Path targetFile, JsonObject incoming) throws IOException {
        JsonObject existing = new JsonObject();
        if (Files.exists(targetFile, new LinkOption[0])) {
            try {
                String content = Files.readString(targetFile, StandardCharsets.UTF_8);
                existing = JsonParser.parseString(content).getAsJsonObject();
            }
            catch (Exception e) {
                LOGGER.fine("Could not parse existing config.json for merge: " + e.getMessage());
            }
        }
        JsonObject merged = new JsonObject();
        for (String key : existing.keySet()) {
            merged.add(key, existing.get(key));
        }
        for (String key : incoming.keySet()) {
            merged.add(key, incoming.get(key));
        }
        Files.writeString(targetFile, (CharSequence)GSON.toJson(merged), StandardCharsets.UTF_8, new OpenOption[0]);
        LOGGER.info("Merged config.json: " + incoming.keySet().size() + " keys from editor, " + existing.keySet().size() + " existing keys");
    }

    private void mergeCitizensFile(Path targetFile, JsonObject incoming) throws IOException {
        LinkedHashMap<String, JsonElement> incomingById = new LinkedHashMap<String, JsonElement>();
        if (incoming.has("citizens") && incoming.get("citizens").isJsonArray()) {
            for (JsonElement el : incoming.getAsJsonArray("citizens")) {
                if (!el.isJsonObject() || !el.getAsJsonObject().has("id")) continue;
                incomingById.put(el.getAsJsonObject().get("id").getAsString(), el);
            }
        }
        LinkedHashMap<String, JsonElement> existingById = new LinkedHashMap<String, JsonElement>();
        ArrayList<String> existingDisabledIds = new ArrayList<String>();
        int existingVersion = 1;
        if (Files.exists(targetFile, new LinkOption[0])) {
            try {
                String content = Files.readString(targetFile, StandardCharsets.UTF_8);
                JsonObject existing = JsonParser.parseString(content).getAsJsonObject();
                if (existing.has("citizens") && existing.get("citizens").isJsonArray()) {
                    for (JsonElement el : existing.getAsJsonArray("citizens")) {
                        if (!el.isJsonObject() || !el.getAsJsonObject().has("id")) continue;
                        existingById.put(el.getAsJsonObject().get("id").getAsString(), el);
                    }
                }
                if (existing.has("disabled_base_ids") && existing.get("disabled_base_ids").isJsonArray()) {
                    for (JsonElement el : existing.getAsJsonArray("disabled_base_ids")) {
                        existingDisabledIds.add(el.getAsString());
                    }
                }
                if (existing.has("configVersion")) {
                    existingVersion = existing.get("configVersion").getAsInt();
                }
            }
            catch (Exception e) {
                LOGGER.fine("Could not parse existing citizens file for merge: " + e.getMessage());
            }
        }
        LinkedHashMap<String, JsonElement> merged = new LinkedHashMap<String, JsonElement>(existingById);
        merged.putAll(incomingById);
        LinkedHashSet<String> disabledSet = new LinkedHashSet<String>(existingDisabledIds);
        if (incoming.has("disabled_base_ids") && incoming.get("disabled_base_ids").isJsonArray()) {
            for (JsonElement el : incoming.getAsJsonArray("disabled_base_ids")) {
                disabledSet.add(el.getAsString());
            }
        }
        JsonObject result = new JsonObject();
        result.addProperty("configVersion", existingVersion);
        JsonArray disabledArr = new JsonArray();
        for (String id : disabledSet) {
            disabledArr.add(id);
        }
        result.add("disabled_base_ids", disabledArr);
        JsonArray citizensArr = new JsonArray();
        for (JsonElement el : merged.values()) {
            citizensArr.add(el);
        }
        result.add("citizens", citizensArr);
        Files.writeString(targetFile, (CharSequence)GSON.toJson(result), StandardCharsets.UTF_8, new OpenOption[0]);
        LOGGER.info("Merged citizens file: " + merged.size() + " citizens (" + incomingById.size() + " from editor, " + (merged.size() - incomingById.size()) + " preserved)");
    }

    private void mergeLootbagsFile(Path targetFile, JsonObject incoming) throws IOException {
        JsonObject incomingLootbags = incoming.has("lootbags") && incoming.get("lootbags").isJsonObject() ? incoming.getAsJsonObject("lootbags") : new JsonObject();
        JsonObject existingLootbags = new JsonObject();
        int existingVersion = 1;
        if (Files.exists(targetFile, new LinkOption[0])) {
            try {
                String content = Files.readString(targetFile, StandardCharsets.UTF_8);
                Iterator<String> existing = JsonParser.parseString(content).getAsJsonObject();
                if (((JsonObject)((Object)existing)).has("lootbags") && ((JsonObject)((Object)existing)).get("lootbags").isJsonObject()) {
                    existingLootbags = ((JsonObject)((Object)existing)).getAsJsonObject("lootbags");
                }
                if (((JsonObject)((Object)existing)).has("configVersion")) {
                    existingVersion = ((JsonObject)((Object)existing)).get("configVersion").getAsInt();
                }
            }
            catch (Exception e) {
                LOGGER.fine("Could not parse existing lootbags file for merge: " + e.getMessage());
            }
        }
        JsonObject merged = new JsonObject();
        for (String key : existingLootbags.keySet()) {
            merged.add(key, existingLootbags.get(key));
        }
        for (String key : incomingLootbags.keySet()) {
            merged.add(key, incomingLootbags.get(key));
        }
        JsonObject result = new JsonObject();
        result.addProperty("configVersion", existingVersion);
        result.add("lootbags", merged);
        Files.writeString(targetFile, (CharSequence)GSON.toJson(result), StandardCharsets.UTF_8, new OpenOption[0]);
        LOGGER.info("Merged lootbags file: " + merged.size() + " lootbags (" + incomingLootbags.size() + " from editor, " + (merged.size() - incomingLootbags.size()) + " preserved)");
    }

    @Override
    public void importLocalization(String language, Map<String, String> translations) {
        Path localizationFolder = this.plugin.getDataDirectory().resolve("localization");
        Path langFile = localizationFolder.resolve(language + ".json");
        try {
            String existing;
            Map existingMap;
            Files.createDirectories(localizationFolder, new FileAttribute[0]);
            LinkedHashMap<String, String> merged = new LinkedHashMap<String, String>();
            if (Files.exists(langFile, new LinkOption[0]) && (existingMap = (Map)GSON.fromJson(existing = Files.readString(langFile, StandardCharsets.UTF_8), STRING_MAP_TYPE)) != null) {
                merged.putAll(existingMap);
            }
            merged.putAll(translations);
            String json = GSON.toJson(merged);
            Files.writeString(langFile, (CharSequence)json, StandardCharsets.UTF_8, new OpenOption[0]);
            LOGGER.info("Imported localization for " + language + " (" + translations.size() + " entries)");
        }
        catch (IOException e) {
            LOGGER.warning("Failed to import localization for " + language + ": " + e.getMessage());
        }
    }

    @Override
    public String reload() {
        Path dataFolder = this.plugin.getDataDirectory();
        this.plugin.getCitizenService().reloadAllWorlds();
        this.plugin.getDialogService().load(dataFolder);
        this.plugin.getLootbagAdminService().load(dataFolder);
        this.plugin.getShopService().loadShopsFromDirectory(dataFolder.resolve("shops"));
        CoreI18n.getInstance().reload(dataFolder);
        int citizenCount = this.plugin.getCitizenService().getCitizenCount();
        int lootbagCount = this.plugin.getLootbagAdminService().getAll().size();
        int shopCount = this.plugin.getShopService().getAllShops().size();
        return citizenCount + " Citizens, " + lootbagCount + " Lootbags, " + shopCount + " Shops loaded";
    }

    private void readJsonFile(Path file, String key, Map<String, JsonElement> target) {
        if (!Files.exists(file, new LinkOption[0])) {
            return;
        }
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            JsonElement element = JsonParser.parseString(content);
            target.put(key, element);
        }
        catch (Exception e) {
            LOGGER.warning("Failed to read " + String.valueOf(file) + ": " + e.getMessage());
        }
    }

    private Stream<Path> listJsonFiles(Path folder) {
        try {
            return Files.list(folder).filter(p -> p.getFileName().toString().endsWith(".json")).sorted();
        }
        catch (IOException e) {
            LOGGER.warning("Failed to list folder " + String.valueOf(folder) + ": " + e.getMessage());
            return Stream.empty();
        }
    }
}

