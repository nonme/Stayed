/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 */
package com.kyuubisoft.core.i18n;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.kyuubisoft.core.api.CoreAPI;
import com.kyuubisoft.core.i18n.PlayerPreferences;
import com.kyuubisoft.core.kslang.KsLang;

public class I18nContext {
    private static final ThreadLocal<String> PLAYER_LANG = new ThreadLocal();
    private static final ThreadLocal<PlayerRef> PLAYER_REF = new ThreadLocal();

    public static String get() {
        return PLAYER_LANG.get();
    }

    public static String getForMod(String modId) {
        PlayerRef ref = PLAYER_REF.get();
        if (ref != null) {
            String ksLangOverride = KsLang.getPlayerOverride(ref.getUuid());
            if (ksLangOverride != null && !ksLangOverride.isEmpty()) {
                return ksLangOverride;
            }
            try {
                String modOverride;
                PlayerPreferences prefs = CoreAPI.getPlayerPreferences(ref.getUuid());
                if (prefs != null && (modOverride = prefs.getModLanguageOverride(modId)) != null && !modOverride.isEmpty()) {
                    return modOverride;
                }
            }
            catch (Exception prefs) {
                // empty catch block
            }
            String contextLang = PLAYER_LANG.get();
            if (contextLang != null && !contextLang.isEmpty()) {
                return contextLang;
            }
            return CoreAPI.getPlayerLanguage(ref);
        }
        return PLAYER_LANG.get();
    }

    public static PlayerRef getPlayerRef() {
        return PLAYER_REF.get();
    }

    public static void clear() {
        PLAYER_LANG.remove();
        PLAYER_REF.remove();
    }

    public static void run(PlayerRef ref, Runnable action) {
        String lang = KsLang.getPlayerLanguage(ref);
        PLAYER_LANG.set(lang);
        PLAYER_REF.set(ref);
        try {
            action.run();
        }
        finally {
            PLAYER_LANG.remove();
            PLAYER_REF.remove();
        }
    }

    public static void runWithLanguage(String language, Runnable action) {
        PLAYER_LANG.set(language);
        try {
            action.run();
        }
        finally {
            PLAYER_LANG.remove();
        }
    }
}

