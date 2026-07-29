/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.ui.DropdownEntryInfo
 *  com.hypixel.hytale.server.core.ui.LocalizableString
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 */
package com.kyuubisoft.core.i18n;

import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.kyuubisoft.core.api.CoreAPI;
import com.kyuubisoft.core.i18n.PlayerPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LanguageSettingsHelper {
    private static final Map<String, String> LANGUAGE_NAMES = new LinkedHashMap<String, String>();
    private static final List<ModLanguageEntry> modEntries;

    public static List<DropdownEntryInfo> createLanguageDropdownEntries(String autoDisplayName) {
        ArrayList<DropdownEntryInfo> entries = new ArrayList<DropdownEntryInfo>();
        entries.add(new DropdownEntryInfo(LocalizableString.fromString((String)("Auto (" + autoDisplayName + ")")), "auto"));
        for (Map.Entry<String, String> entry : LANGUAGE_NAMES.entrySet()) {
            entries.add(new DropdownEntryInfo(LocalizableString.fromString((String)entry.getValue()), entry.getKey()));
        }
        return entries;
    }

    public static String getLanguageDisplayName(String langCode) {
        return LANGUAGE_NAMES.getOrDefault(langCode, langCode);
    }

    public static boolean isKnownLanguageCode(String langCode) {
        return langCode != null && LANGUAGE_NAMES.containsKey(langCode);
    }

    public static String getCurrentDropdownValue(UUID playerId) {
        PlayerPreferences prefs = CoreAPI.getPlayerPreferences(playerId);
        if (prefs != null && prefs.hasLanguageOverride()) {
            return prefs.getLanguageOverride();
        }
        return "auto";
    }

    public static void applyLanguageSelection(UUID playerId, String username, String value) {
        if ("auto".equals(value) || value == null || value.isEmpty()) {
            CoreAPI.clearPlayerLanguageOverride(playerId, username);
        } else {
            CoreAPI.setPlayerLanguageOverride(playerId, username, value);
        }
    }

    public static String getAutoLanguageName(PlayerRef ref) {
        String clientLang;
        if (ref != null && (clientLang = ref.getLanguage()) != null && LANGUAGE_NAMES.containsKey(clientLang)) {
            return LANGUAGE_NAMES.get(clientLang);
        }
        return LanguageSettingsHelper.getLanguageDisplayName(CoreAPI.getServerLanguage());
    }

    public static void registerMod(String modId, String displayName) {
        modEntries.add(new ModLanguageEntry(modId, displayName));
    }

    public static List<ModLanguageEntry> getRegisteredMods() {
        return Collections.unmodifiableList(modEntries);
    }

    public static void clearModRegistry() {
        modEntries.clear();
    }

    public static List<DropdownEntryInfo> createModLanguageDropdownEntries(String globalDisplayName, String autoDisplayName) {
        ArrayList<DropdownEntryInfo> entries = new ArrayList<DropdownEntryInfo>();
        entries.add(new DropdownEntryInfo(LocalizableString.fromString((String)("Global (" + globalDisplayName + ")")), "global"));
        entries.add(new DropdownEntryInfo(LocalizableString.fromString((String)("Auto (" + autoDisplayName + ")")), "auto"));
        for (Map.Entry<String, String> entry : LANGUAGE_NAMES.entrySet()) {
            entries.add(new DropdownEntryInfo(LocalizableString.fromString((String)entry.getValue()), entry.getKey()));
        }
        return entries;
    }

    public static String getCurrentModDropdownValue(UUID playerId, String modId) {
        String modOverride;
        PlayerPreferences prefs = CoreAPI.getPlayerPreferences(playerId);
        if (prefs != null && (modOverride = prefs.getModLanguageOverride(modId)) != null && !modOverride.isEmpty()) {
            return modOverride;
        }
        return "global";
    }

    public static void applyModLanguageSelection(UUID playerId, String username, String modId, String value) {
        if ("global".equals(value) || value == null || value.isEmpty()) {
            CoreAPI.clearPlayerModLanguageOverride(playerId, username, modId);
        } else if ("auto".equals(value)) {
            CoreAPI.clearPlayerModLanguageOverride(playerId, username, modId);
        } else {
            CoreAPI.setPlayerModLanguageOverride(playerId, username, modId, value);
        }
    }

    static {
        LANGUAGE_NAMES.put("en-US", "English");
        LANGUAGE_NAMES.put("de-DE", "Deutsch");
        LANGUAGE_NAMES.put("fr-FR", "Francais");
        LANGUAGE_NAMES.put("es-ES", "Espanol");
        LANGUAGE_NAMES.put("pt-BR", "Portugues (BR)");
        LANGUAGE_NAMES.put("ru-RU", "Russian");
        LANGUAGE_NAMES.put("pl-PL", "Polski");
        LANGUAGE_NAMES.put("tr-TR", "Turkish");
        LANGUAGE_NAMES.put("it-IT", "Italiano");
        modEntries = new ArrayList<ModLanguageEntry>();
    }

    public record ModLanguageEntry(String modId, String displayName) {
    }
}

