/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.google.gson.JsonSyntaxException
 */
package com.yourname.companion.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.yourname.companion.data.PlayerCompanionData;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CompanionDataStore {
    private final Path filePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public CompanionDataStore(Path filePath) {
        this.filePath = filePath;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public Map<UUID, PlayerCompanionData> loadAll() {
        if (!Files.exists(this.filePath, new LinkOption[0])) {
            return new HashMap<UUID, PlayerCompanionData>();
        }
        try (BufferedReader reader = Files.newBufferedReader(this.filePath, StandardCharsets.UTF_8);){
            JsonElement root = JsonParser.parseReader((Reader)reader);
            if (root == null || !root.isJsonObject()) {
                HashMap<UUID, PlayerCompanionData> hashMap2 = new HashMap<UUID, PlayerCompanionData>();
                return hashMap2;
            }
            JsonObject rootObj = root.getAsJsonObject();
            JsonObject players = rootObj.has("players") ? rootObj.getAsJsonObject("players") : rootObj;
            HashMap<UUID, PlayerCompanionData> result = new HashMap<UUID, PlayerCompanionData>();
            for (Map.Entry entry : players.entrySet()) {
                try {
                    PlayerCompanionData data;
                    UUID ownerId = UUID.fromString((String)entry.getKey());
                    JsonObject playerJson = ((JsonElement)entry.getValue()).getAsJsonObject();
                    if (!playerJson.has("companions")) {
                        this.migratePlayerData(playerJson);
                    }
                    if ((data = (PlayerCompanionData)this.gson.fromJson((JsonElement)playerJson, PlayerCompanionData.class)) == null) continue;
                    data.ownerId = ownerId;
                    result.put(ownerId, data);
                }
                catch (IllegalArgumentException illegalArgumentException) {}
            }
            HashMap<UUID, PlayerCompanionData> hashMap = result;
            return hashMap;
        }
        catch (JsonSyntaxException | IOException ex) {
            return new HashMap<UUID, PlayerCompanionData>();
        }
    }

    public void saveAll(Map<UUID, PlayerCompanionData> data) throws IOException {
        CompanionDataFile dataFile = CompanionDataFile.fromUuidMap(data);
        Path parent = this.filePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent, new FileAttribute[0]);
        }
        Path tempFile = this.filePath.resolveSibling(this.filePath.getFileName().toString() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8, new OpenOption[0]);){
            this.gson.toJson((Object)dataFile, (Appendable)writer);
        }
        Files.move(tempFile, this.filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private void migratePlayerData(JsonObject json) {
        boolean hasLegacyCompanion;
        JsonArray companions = new JsonArray();
        String entityId = this.getStringOrNull(json, "companionEntityId");
        String uniqueId = this.getStringOrNull(json, "companionUniqueId");
        String name = this.getStringOrNull(json, "companionName");
        String modelId = this.getStringOrNull(json, "appearanceModelId");
        boolean bl = hasLegacyCompanion = uniqueId != null || name != null || entityId != null || this.getIntOrDefault(json, "combatKills", 0) > 0 || this.getIntOrDefault(json, "farmHarvests", 0) > 0;
        if (hasLegacyCompanion) {
            JsonObject record = new JsonObject();
            record.addProperty("uniqueId", uniqueId != null ? uniqueId : UUID.randomUUID().toString());
            if (name != null) {
                record.addProperty("name", name);
            }
            if (modelId != null) {
                record.addProperty("appearanceModelId", modelId);
            }
            record.addProperty("mode", this.getStringOrDefault(json, "mode", "FIGHTER"));
            record.addProperty("followMode", this.getStringOrDefault(json, "followMode", "FOLLOW"));
            record.addProperty("combatLevel", (Number)this.getIntOrDefault(json, "combatLevel", 1));
            record.addProperty("combatKills", (Number)this.getIntOrDefault(json, "combatKills", 0));
            record.addProperty("farmLevel", (Number)this.getIntOrDefault(json, "farmLevel", 1));
            record.addProperty("farmHarvests", (Number)this.getIntOrDefault(json, "farmHarvests", 0));
            record.addProperty("farmAutoResume", Boolean.valueOf(this.getBoolOrDefault(json, "farmAutoResume", false)));
            if (json.has("farmAreaTopLeft")) {
                record.add("farmAreaTopLeft", json.get("farmAreaTopLeft"));
            }
            if (json.has("farmAreaBottomRight")) {
                record.add("farmAreaBottomRight", json.get("farmAreaBottomRight"));
            }
            if (json.has("linkedChests")) {
                record.add("linkedChests", json.get("linkedChests"));
            }
            record.addProperty("mineLevel", (Number)this.getIntOrDefault(json, "mineLevel", 1));
            record.addProperty("mineBlocks", (Number)this.getIntOrDefault(json, "mineBlocks", 0));
            record.addProperty("deathCount", (Number)this.getIntOrDefault(json, "deathCount", 0));
            record.addProperty("startingGearGiven", Boolean.valueOf(this.getBoolOrDefault(json, "startingGearGiven", false)));
            record.addProperty("fallen", Boolean.valueOf(false));
            if (json.has("savedInventory")) {
                record.add("savedInventory", json.get("savedInventory"));
            }
            companions.add((JsonElement)record);
        }
        if (json.has("fallenCompanions") && json.get("fallenCompanions").isJsonArray()) {
            JsonArray fallenArray = json.getAsJsonArray("fallenCompanions");
            for (JsonElement fe : fallenArray) {
                String fcModel;
                if (!fe.isJsonObject()) continue;
                JsonObject fc = fe.getAsJsonObject();
                JsonObject record = new JsonObject();
                String fcId = this.getStringOrNull(fc, "companionUniqueId");
                record.addProperty("uniqueId", fcId != null ? fcId : UUID.randomUUID().toString());
                String fcName = this.getStringOrNull(fc, "name");
                if (fcName != null) {
                    record.addProperty("name", fcName);
                }
                if ((fcModel = this.getStringOrNull(fc, "appearanceModelId")) != null) {
                    record.addProperty("appearanceModelId", fcModel);
                }
                record.addProperty("combatLevel", (Number)this.getIntOrDefault(fc, "combatLevel", 1));
                record.addProperty("combatKills", (Number)this.getIntOrDefault(fc, "combatKills", 0));
                record.addProperty("farmLevel", (Number)this.getIntOrDefault(fc, "farmLevel", 1));
                record.addProperty("farmHarvests", (Number)this.getIntOrDefault(fc, "farmHarvests", 0));
                record.addProperty("mode", this.getStringOrDefault(fc, "mode", "FIGHTER"));
                record.addProperty("followMode", this.getStringOrDefault(fc, "followMode", "FOLLOW"));
                record.addProperty("farmAutoResume", Boolean.valueOf(this.getBoolOrDefault(fc, "farmAutoResume", false)));
                if (fc.has("farmAreaTopLeft")) {
                    record.add("farmAreaTopLeft", fc.get("farmAreaTopLeft"));
                }
                if (fc.has("farmAreaBottomRight")) {
                    record.add("farmAreaBottomRight", fc.get("farmAreaBottomRight"));
                }
                if (fc.has("linkedChests")) {
                    record.add("linkedChests", fc.get("linkedChests"));
                }
                record.addProperty("deathCount", (Number)this.getIntOrDefault(fc, "deathCount", 1));
                String cause = this.getStringOrNull(fc, "deathCause");
                if (cause != null) {
                    record.addProperty("deathCause", cause);
                }
                if (fc.has("deathTime")) {
                    record.addProperty("deathTime", (Number)fc.get("deathTime").getAsLong());
                }
                record.addProperty("fallen", Boolean.valueOf(true));
                if (fc.has("savedInventory")) {
                    record.add("savedInventory", fc.get("savedInventory"));
                }
                companions.add((JsonElement)record);
            }
        }
        json.add("companions", (JsonElement)companions);
        json.remove("companionEntityId");
        json.remove("companionUniqueId");
        json.remove("companionName");
        json.remove("appearanceModelId");
        json.remove("mode");
        json.remove("followMode");
        json.remove("combatLevel");
        json.remove("combatKills");
        json.remove("farmLevel");
        json.remove("farmHarvests");
        json.remove("farmAutoResume");
        json.remove("farmAreaTopLeft");
        json.remove("farmAreaBottomRight");
        json.remove("deathCount");
        json.remove("linkedChests");
        json.remove("savedInventory");
        json.remove("startingGearGiven");
        json.remove("fallenCompanions");
    }

    private String getStringOrNull(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            return null;
        }
        try {
            return json.get(key).getAsString();
        }
        catch (Exception e) {
            return null;
        }
    }

    private String getStringOrDefault(JsonObject json, String key, String def) {
        String v = this.getStringOrNull(json, key);
        return v != null ? v : def;
    }

    private int getIntOrDefault(JsonObject json, String key, int def) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            return def;
        }
        try {
            return json.get(key).getAsInt();
        }
        catch (Exception e) {
            return def;
        }
    }

    private boolean getBoolOrDefault(JsonObject json, String key, boolean def) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            return def;
        }
        try {
            return json.get(key).getAsBoolean();
        }
        catch (Exception e) {
            return def;
        }
    }

    private static final class CompanionDataFile {
        private Map<String, PlayerCompanionData> players = new HashMap<String, PlayerCompanionData>();

        private CompanionDataFile() {
        }

        private static CompanionDataFile fromUuidMap(Map<UUID, PlayerCompanionData> data) {
            CompanionDataFile file = new CompanionDataFile();
            for (Map.Entry<UUID, PlayerCompanionData> entry : data.entrySet()) {
                PlayerCompanionData playerData = entry.getValue();
                if (playerData == null) continue;
                file.players.put(entry.getKey().toString(), playerData);
            }
            return file;
        }
    }
}

