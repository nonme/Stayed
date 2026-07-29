/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.kyuubisoft.core.registry;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.webeditor.ModConfigProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModMenuRegistry {
    private static final List<ModMenuEntry> entries = new ArrayList<ModMenuEntry>();
    private static ModPageOpener coreAdminOpener;
    private static String currentLanguage;
    private static NpcProfileEditorCallback npcProfileEditorCallback;
    private static final List<ReloadEntry> reloadHandlers;
    private static final List<ModConfigProvider> configProviders;
    private static final List<LanguageChangeListener> languageListeners;

    public static void register(ModMenuEntry entry) {
        entries.add(entry);
    }

    public static List<ModMenuEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public static void clear() {
        entries.clear();
        languageListeners.clear();
        configProviders.clear();
        currentLanguage = null;
    }

    public static void setCoreAdminOpener(ModPageOpener opener) {
        coreAdminOpener = opener;
    }

    public static void openCoreAdmin(Player player, PlayerRef playerRef, Ref<EntityStore> ref, Store<EntityStore> store) {
        if (coreAdminOpener != null) {
            coreAdminOpener.open(player, playerRef, ref, store);
        }
    }

    public static void setCurrentLanguage(String language) {
        currentLanguage = language;
    }

    public static String getCurrentLanguage() {
        return currentLanguage != null ? currentLanguage : "en-US";
    }

    public static void setNpcProfileEditorCallback(NpcProfileEditorCallback cb) {
        npcProfileEditorCallback = cb;
    }

    public static boolean hasNpcProfileEditor() {
        return npcProfileEditorCallback != null;
    }

    public static void openNpcProfileEditor(Player player, PlayerRef playerRef, Ref<EntityStore> ref, Store<EntityStore> store, String citizenId) {
        if (npcProfileEditorCallback != null) {
            npcProfileEditorCallback.open(player, playerRef, ref, store, citizenId);
        }
    }

    public static void addReloadHandler(String modId, String modName, ReloadHandler handler) {
        reloadHandlers.add(new ReloadEntry(modId, modName, handler));
    }

    public static List<String> reloadAll() {
        ArrayList<String> results = new ArrayList<String>();
        for (ReloadEntry entry : reloadHandlers) {
            try {
                String result = entry.handler.reload();
                results.add(entry.modName + ": " + result);
            }
            catch (Exception e) {
                results.add(entry.modName + ": ERROR - " + e.getMessage());
            }
        }
        return results;
    }

    public static List<ReloadEntry> getReloadHandlers() {
        return Collections.unmodifiableList(reloadHandlers);
    }

    public static void addConfigProvider(ModConfigProvider provider) {
        configProviders.add(provider);
    }

    public static List<ModConfigProvider> getConfigProviders() {
        return Collections.unmodifiableList(configProviders);
    }

    public static ModConfigProvider getConfigProvider(String modId) {
        for (ModConfigProvider p : configProviders) {
            if (!p.getModId().equals(modId)) continue;
            return p;
        }
        return null;
    }

    public static void addLanguageChangeListener(LanguageChangeListener listener) {
        languageListeners.add(listener);
        if (currentLanguage != null) {
            try {
                listener.onLanguageChange(currentLanguage);
            }
            catch (Exception e) {
                System.err.println("Error auto-syncing language for new listener: " + e.getMessage());
            }
        }
    }

    public static void notifyLanguageChange(String newLanguage) {
        currentLanguage = newLanguage;
        for (LanguageChangeListener listener : languageListeners) {
            try {
                listener.onLanguageChange(newLanguage);
            }
            catch (Exception e) {
                System.err.println("Error notifying language change listener: " + e.getMessage());
            }
        }
    }

    static {
        currentLanguage = null;
        reloadHandlers = new ArrayList<ReloadEntry>();
        configProviders = new ArrayList<ModConfigProvider>();
        languageListeners = new ArrayList<LanguageChangeListener>();
    }

    @FunctionalInterface
    public static interface ModPageOpener {
        public void open(Player var1, PlayerRef var2, Ref<EntityStore> var3, Store<EntityStore> var4);
    }

    @FunctionalInterface
    public static interface NpcProfileEditorCallback {
        public void open(Player var1, PlayerRef var2, Ref<EntityStore> var3, Store<EntityStore> var4, String var5);
    }

    public record ReloadEntry(String modId, String modName, ReloadHandler handler) {
    }

    @FunctionalInterface
    public static interface ReloadHandler {
        public String reload();
    }

    @FunctionalInterface
    public static interface LanguageChangeListener {
        public void onLanguageChange(String var1);
    }

    public record ModMenuEntry(String id, String name, String description, String version, String icon, ModPageOpener opener) {
    }
}

