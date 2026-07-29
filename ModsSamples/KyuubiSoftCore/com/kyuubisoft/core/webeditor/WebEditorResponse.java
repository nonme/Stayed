/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.webeditor;

import com.google.gson.JsonObject;
import com.kyuubisoft.core.webeditor.ModConfigProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebEditorResponse {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft WebEditor");
    private final JsonObject payload;

    public WebEditorResponse(JsonObject payload) {
        this.payload = payload;
    }

    public List<String> apply(List<ModConfigProvider> providers) {
        ModConfigProvider provider;
        ArrayList<String> results = new ArrayList<String>();
        LinkedHashSet<String> affectedModIds = new LinkedHashSet<String>();
        HashMap<String, Integer> configCounts = new HashMap<String, Integer>();
        if (this.payload.has("changes") && this.payload.get("changes").isJsonObject()) {
            JsonObject changes = this.payload.getAsJsonObject("changes");
            for (String modId : changes.keySet()) {
                provider = this.findProvider(providers, modId);
                if (provider == null) {
                    LOGGER.warning("No ConfigProvider for mod: " + modId);
                    results.add(modId + ": SKIPPED (unknown mod)");
                    continue;
                }
                JsonObject modChanges = changes.getAsJsonObject(modId);
                int count = 0;
                for (String fileName : modChanges.keySet()) {
                    try {
                        provider.importConfig(fileName, modChanges.get(fileName));
                        ++count;
                    }
                    catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to import " + modId + "/" + fileName, e);
                        results.add(modId + "/" + fileName + ": ERROR - " + e.getMessage());
                    }
                }
                affectedModIds.add(modId);
                configCounts.put(modId, count);
            }
        }
        if (this.payload.has("localizationChanges") && this.payload.get("localizationChanges").isJsonObject()) {
            JsonObject l10nChanges = this.payload.getAsJsonObject("localizationChanges");
            for (String modId : l10nChanges.keySet()) {
                provider = this.findProvider(providers, modId);
                if (provider == null) continue;
                JsonObject modL10n = l10nChanges.getAsJsonObject(modId);
                for (String language : modL10n.keySet()) {
                    JsonObject translations = modL10n.getAsJsonObject(language);
                    HashMap<String, String> translationMap = new HashMap<String, String>();
                    for (String key : translations.keySet()) {
                        translationMap.put(key, translations.get(key).getAsString());
                    }
                    provider.importLocalization(language, translationMap);
                }
                affectedModIds.add(modId);
            }
        }
        for (String modId : affectedModIds) {
            ModConfigProvider provider2 = this.findProvider(providers, modId);
            if (provider2 == null) continue;
            try {
                String reloadResult = provider2.reload();
                int configs = configCounts.getOrDefault(modId, 0);
                results.add(modId + ": " + configs + " configs updated, " + reloadResult);
            }
            catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to reload " + modId, e);
                results.add(modId + ": RELOAD ERROR - " + e.getMessage());
            }
        }
        return results;
    }

    public String getSessionId() {
        return this.payload.has("sessionId") ? this.payload.get("sessionId").getAsString() : null;
    }

    private ModConfigProvider findProvider(List<ModConfigProvider> providers, String modId) {
        for (ModConfigProvider p : providers) {
            if (!p.getModId().equals(modId)) continue;
            return p;
        }
        return null;
    }
}

