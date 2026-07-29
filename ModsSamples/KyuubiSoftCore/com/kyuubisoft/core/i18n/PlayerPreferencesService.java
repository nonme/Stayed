/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.i18n;

import com.google.gson.Gson;
import com.kyuubisoft.core.i18n.LanguageSettingsHelper;
import com.kyuubisoft.core.i18n.PlayerPreferences;
import com.kyuubisoft.core.storage.PlayerDataStorage;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class PlayerPreferencesService {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core");
    private static final Gson GSON = new Gson();
    private static final String TABLE_NAME = "player_preferences";
    private final ConcurrentHashMap<UUID, PlayerPreferences> cache = new ConcurrentHashMap();
    private final PlayerDataStorage storage;

    public PlayerPreferencesService(PlayerDataStorage storage) {
        this.storage = storage;
        if (storage != null) {
            storage.ensureTableExists(TABLE_NAME);
        }
    }

    public void loadPlayer(UUID playerId, String username) {
        if (this.storage == null) {
            this.cache.put(playerId, new PlayerPreferences());
            return;
        }
        try {
            PlayerPreferences prefs;
            String json = this.storage.loadJson(TABLE_NAME, playerId);
            if (json != null && (prefs = GSON.fromJson(json, PlayerPreferences.class)) != null) {
                this.cache.put(playerId, prefs);
                LOGGER.info("[Core] Loaded preferences for " + username + (String)(prefs.hasLanguageOverride() ? " (lang=" + prefs.getLanguageOverride() + ")" : " (auto)") + (String)(prefs.getModLanguageOverrides().isEmpty() ? "" : " mods=" + String.valueOf(prefs.getModLanguageOverrides())));
                return;
            }
        }
        catch (Exception e) {
            LOGGER.warning("[Core] Failed to load preferences for " + username + ": " + e.getMessage());
        }
        this.cache.put(playerId, new PlayerPreferences());
    }

    public void savePlayer(UUID playerId, String username) {
        PlayerPreferences prefs = this.cache.get(playerId);
        if (prefs == null || this.storage == null) {
            return;
        }
        try {
            String json = GSON.toJson(prefs);
            this.storage.saveJson(TABLE_NAME, playerId, username, json);
        }
        catch (Exception e) {
            LOGGER.warning("[Core] Failed to save preferences for " + username + ": " + e.getMessage());
        }
    }

    public void clearPlayer(UUID playerId) {
        this.cache.remove(playerId);
    }

    public PlayerPreferences getPreferences(UUID playerId) {
        return this.cache.getOrDefault(playerId, new PlayerPreferences());
    }

    public void setLanguageOverride(UUID playerId, String username, String language) {
        if (language == null || language.isEmpty() || "auto".equals(language)) {
            this.clearLanguageOverride(playerId, username);
            return;
        }
        PlayerPreferences prefs = this.cache.computeIfAbsent(playerId, k -> new PlayerPreferences());
        if (language.equals(prefs.getLanguageOverride())) {
            return;
        }
        prefs.setLanguageOverride(language);
        this.savePlayer(playerId, username);
        if (LanguageSettingsHelper.isKnownLanguageCode(language)) {
            LOGGER.info("[Core] Set language override for " + username + ": " + language);
        } else {
            LOGGER.warning("[Core] Set language override for " + username + ": " + language + " (unknown language code \u2014 caller may have parameter order wrong)");
        }
    }

    public void clearLanguageOverride(UUID playerId, String username) {
        PlayerPreferences prefs = this.cache.get(playerId);
        if (prefs == null || prefs.getLanguageOverride() == null) {
            return;
        }
        prefs.setLanguageOverride(null);
        this.savePlayer(playerId, username);
        LOGGER.info("[Core] Cleared language override for " + username + " (auto-detect)");
    }

    public void setModLanguageOverride(UUID playerId, String username, String modId, String language) {
        if (language == null || language.isEmpty() || "auto".equals(language) || "global".equals(language)) {
            this.clearModLanguageOverride(playerId, username, modId);
            return;
        }
        PlayerPreferences prefs = this.cache.computeIfAbsent(playerId, k -> new PlayerPreferences());
        String previous = prefs.getModLanguageOverride(modId);
        if (language.equals(previous)) {
            return;
        }
        prefs.setModLanguageOverride(modId, language);
        this.savePlayer(playerId, username);
        if (LanguageSettingsHelper.isKnownLanguageCode(language)) {
            LOGGER.info("[Core] Set mod language override for " + username + ": " + modId + "=" + language);
        } else {
            LOGGER.warning("[Core] Set mod language override for " + username + ": " + modId + "=" + language + " (unknown language code \u2014 caller may have parameter order wrong)");
        }
    }

    public void clearModLanguageOverride(UUID playerId, String username, String modId) {
        PlayerPreferences prefs = this.cache.get(playerId);
        if (prefs == null || prefs.getModLanguageOverride(modId) == null) {
            return;
        }
        prefs.clearModLanguageOverride(modId);
        this.savePlayer(playerId, username);
        LOGGER.info("[Core] Cleared mod language override for " + username + ": " + modId + " (global)");
    }
}

