/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
 *  com.hypixel.hytale.server.core.asset.type.item.config.Item
 *  com.hypixel.hytale.server.core.modules.entity.EntityModule
 */
package com.kyuubisoft.core.webeditor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.kyuubisoft.core.webeditor.ModConfigProvider;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebEditorRequest {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft WebEditor");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final JsonObject payload;

    private WebEditorRequest(JsonObject payload) {
        this.payload = payload;
    }

    public JsonObject getPayload() {
        return this.payload;
    }

    public String encode() {
        return GSON.toJson(this.payload);
    }

    public static WebEditorRequest generate(List<ModConfigProvider> providers, String serverName, String uploaderName, String uploaderUuid) {
        JsonObject root = new JsonObject();
        JsonObject metadata = new JsonObject();
        metadata.addProperty("serverName", serverName);
        JsonObject uploader = new JsonObject();
        uploader.addProperty("name", uploaderName);
        uploader.addProperty("uuid", uploaderUuid);
        metadata.add("uploader", uploader);
        metadata.addProperty("time", System.currentTimeMillis());
        metadata.addProperty("protocolVersion", 1);
        JsonObject versions = new JsonObject();
        for (ModConfigProvider modConfigProvider : providers) {
            versions.addProperty(modConfigProvider.getModId(), "1.0");
        }
        metadata.add("pluginVersions", versions);
        root.add("metadata", metadata);
        JsonObject configs = new JsonObject();
        for (ModConfigProvider provider : providers) {
            JsonObject modConfigs = new JsonObject();
            Map<String, JsonElement> exported = provider.exportConfigs();
            if (exported != null) {
                for (Map.Entry<String, JsonElement> entry : exported.entrySet()) {
                    modConfigs.add(entry.getKey(), entry.getValue());
                }
            }
            configs.add(provider.getModId(), modConfigs);
        }
        root.add("configs", configs);
        JsonObject jsonObject = new JsonObject();
        for (ModConfigProvider provider : providers) {
            Map<String, Map<String, String>> l10n = provider.exportLocalization();
            if (l10n == null || l10n.isEmpty()) continue;
            JsonObject modL10n = new JsonObject();
            for (Map.Entry<String, Map<String, String>> langEntry : l10n.entrySet()) {
                JsonObject translations = new JsonObject();
                for (Map.Entry<String, String> kv : langEntry.getValue().entrySet()) {
                    translations.addProperty(kv.getKey(), kv.getValue());
                }
                modL10n.add(langEntry.getKey(), translations);
            }
            jsonObject.add(provider.getModId(), modL10n);
        }
        root.add("localization", jsonObject);
        root.add("customReferences", WebEditorRequest.buildCustomReferences());
        return new WebEditorRequest(root);
    }

    private static JsonObject buildCustomReferences() {
        JsonObject refs = new JsonObject();
        try {
            TreeSet itemKeys = new TreeSet(Item.getAssetMap().getAssetMap().keySet());
            JsonArray items = new JsonArray();
            for (String id : itemKeys) {
                items.add(id);
            }
            refs.add("items", items);
        }
        catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to collect item IDs for web editor", e);
            refs.add("items", new JsonArray());
        }
        try {
            TreeSet blockKeys = new TreeSet(BlockType.getAssetMap().getAssetMap().keySet());
            JsonArray blocks = new JsonArray();
            for (String id : blockKeys) {
                blocks.add(id);
            }
            refs.add("blocks", blocks);
        }
        catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to collect block IDs for web editor", e);
            refs.add("blocks", new JsonArray());
        }
        try {
            EntityModule entityModule = EntityModule.get();
            Field idMapField = EntityModule.class.getDeclaredField("idMap");
            idMapField.setAccessible(true);
            Map idMap = (Map)idMapField.get(entityModule);
            TreeSet entityKeys = new TreeSet(idMap.keySet());
            JsonArray entities = new JsonArray();
            for (String id : entityKeys) {
                entities.add(id);
            }
            refs.add("entities", entities);
        }
        catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to collect entity IDs for web editor", e);
            refs.add("entities", new JsonArray());
        }
        return refs;
    }

    public void appendSocketInfo(String channelId, int protocolVersion) {
        JsonObject socket = new JsonObject();
        socket.addProperty("protocolVersion", protocolVersion);
        socket.addProperty("channelId", channelId);
        this.payload.add("socket", socket);
    }
}

