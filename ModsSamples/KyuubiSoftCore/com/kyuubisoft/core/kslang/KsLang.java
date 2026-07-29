/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.command.system.AbstractCommand
 *  com.hypixel.hytale.server.core.plugin.JavaPlugin
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 */
package com.kyuubisoft.core.kslang;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.kyuubisoft.core.kslang.KsLangCommand;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class KsLang {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft KsLang");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String OWNER_PROPERTY = "kyuubisoft.kslang.owner";
    private static Path sharedDir;
    private static final Map<String, String> prefsCache;
    private static final List<BiConsumer<UUID, String>> changeListeners;
    private static boolean initialized;
    private static JavaPlugin owningPlugin;
    private static final Map<String, String> KNOWN_LANGUAGE_NAMES;
    private static final Map<String, String> discoveredLanguages;
    private static volatile long lastPrefsLoad;
    private static final long PREFS_RELOAD_INTERVAL_MS = 5000L;

    private KsLang() {
    }

    public static void init(JavaPlugin plugin, String modId, String modDisplayName) {
        if (initialized) {
            KsLang.discoverLanguages(plugin.getDataDirectory().resolve("localization"));
            return;
        }
        initialized = true;
        owningPlugin = plugin;
        sharedDir = plugin.getDataDirectory().getParent().resolve("kslang");
        try {
            Files.createDirectories(sharedDir, new FileAttribute[0]);
        }
        catch (IOException e) {
            LOGGER.warning("[KsLang] Failed to create shared directory: " + e.getMessage());
        }
        KsLang.registerMod(modId, modDisplayName);
        KsLang.loadPrefs();
        KsLang.discoverLanguages(plugin.getDataDirectory().resolve("localization"));
        String currentOwner = System.getProperty(OWNER_PROPERTY);
        if (currentOwner == null || currentOwner.isEmpty()) {
            System.setProperty(OWNER_PROPERTY, modId);
            try {
                plugin.getCommandRegistry().registerCommand((AbstractCommand)new KsLangCommand());
                LOGGER.info("[KsLang] Registered /kslang command (owner: " + modId + ")");
            }
            catch (Exception e) {
                LOGGER.warning("[KsLang] Failed to register /kslang command: " + e.getMessage());
            }
        } else {
            LOGGER.info("[KsLang] /kslang command already owned by " + currentOwner + ", skipping registration");
        }
        LOGGER.info("[KsLang] Initialized for mod: " + modDisplayName + " (" + modId + "), " + discoveredLanguages.size() + " languages discovered");
    }

    public static String getPlayerLanguage(PlayerRef ref) {
        if (ref != null) {
            String uuid;
            String override;
            long now = System.currentTimeMillis();
            if (now - lastPrefsLoad > 5000L) {
                KsLang.loadPrefs();
                lastPrefsLoad = now;
            }
            if ((override = prefsCache.get(uuid = ref.getUuid().toString())) != null && !override.isEmpty()) {
                return override;
            }
            String clientLang = ref.getLanguage();
            if (clientLang != null && !clientLang.isEmpty()) {
                return clientLang;
            }
        }
        return "en-US";
    }

    public static void setPlayerLanguage(UUID playerId, String langCode) {
        prefsCache.put(playerId.toString(), langCode);
        KsLang.savePrefs();
        lastPrefsLoad = System.currentTimeMillis();
        KsLang.notifyListeners(playerId, langCode);
    }

    public static void clearPlayerLanguage(UUID playerId) {
        prefsCache.remove(playerId.toString());
        KsLang.savePrefs();
        lastPrefsLoad = System.currentTimeMillis();
        KsLang.notifyListeners(playerId, null);
    }

    public static void addChangeListener(BiConsumer<UUID, String> listener) {
        changeListeners.add(listener);
    }

    private static void notifyListeners(UUID playerId, String langCode) {
        for (BiConsumer<UUID, String> listener : changeListeners) {
            try {
                listener.accept(playerId, langCode);
            }
            catch (Exception e) {
                LOGGER.fine("[KsLang] Listener error: " + e.getMessage());
            }
        }
    }

    public static String getPlayerOverride(UUID playerId) {
        return prefsCache.get(playerId.toString());
    }

    public static Map<String, String> getSupportedLanguages() {
        if (discoveredLanguages.isEmpty()) {
            return Collections.unmodifiableMap(KNOWN_LANGUAGE_NAMES);
        }
        return Collections.unmodifiableMap(discoveredLanguages);
    }

    public static List<Map<String, String>> getRegisteredMods() {
        Path modsFile = KsLang.getModsFile();
        if (!Files.exists(modsFile, new LinkOption[0])) {
            return List.of();
        }
        try {
            String content = Files.readString(modsFile, StandardCharsets.UTF_8);
            Type listType = new TypeToken<List<Map<String, String>>>(){}.getType();
            List<Map<String, String>> mods = (List<Map<String, String>>)GSON.fromJson(content, listType);
            return mods != null ? mods : List.of();
        }
        catch (Exception e) {
            LOGGER.warning("[KsLang] Failed to read mods.json: " + e.getMessage());
            return List.of();
        }
    }

    public static void discoverLanguages(Path localizationFolder) {
        if (localizationFolder == null || !Files.exists(localizationFolder, new LinkOption[0]) || !Files.isDirectory(localizationFolder, new LinkOption[0])) {
            return;
        }
        try (Stream<Path> stream = Files.list(localizationFolder);){
            stream.filter(p -> {
                String name = p.getFileName().toString();
                return name.endsWith(".json") && !name.startsWith("custom_") && !name.contains(".backup") && !name.contains(".example");
            }).forEach(p -> {
                String name = p.getFileName().toString();
                String code = name.substring(0, name.length() - 5);
                if (!discoveredLanguages.containsKey(code)) {
                    String displayName = KNOWN_LANGUAGE_NAMES.getOrDefault(code, code);
                    discoveredLanguages.put(code, displayName);
                }
            });
        }
        catch (IOException e) {
            LOGGER.fine("[KsLang] Could not scan localization folder: " + e.getMessage());
        }
    }

    private static void registerMod(String modId, String modDisplayName) {
        Path modsFile = KsLang.getModsFile();
        ArrayList mods = new ArrayList();
        if (Files.exists(modsFile, new LinkOption[0])) {
            try {
                String content = Files.readString(modsFile, StandardCharsets.UTF_8);
                Type listType = new TypeToken<List<Map<String, String>>>(){}.getType();
                List list = (List)GSON.fromJson(content, listType);
                if (list != null) {
                    mods.addAll(list);
                }
            }
            catch (Exception e) {
                LOGGER.warning("[KsLang] Failed to read existing mods.json: " + e.getMessage());
            }
        }
        boolean found = false;
        for (Map map : mods) {
            if (!modId.equals(map.get("id"))) continue;
            map.put("name", modDisplayName);
            found = true;
            break;
        }
        if (!found) {
            LinkedHashMap<String, String> entry = new LinkedHashMap<String, String>();
            entry.put("id", modId);
            entry.put("name", modDisplayName);
            mods.add(entry);
        }
        try {
            Files.writeString(modsFile, (CharSequence)GSON.toJson(mods), StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (IOException e) {
            LOGGER.warning("[KsLang] Failed to write mods.json: " + e.getMessage());
        }
    }

    private static void loadPrefs() {
        Path prefsFile = KsLang.getPrefsFile();
        if (!Files.exists(prefsFile, new LinkOption[0])) {
            prefsCache.clear();
            return;
        }
        try {
            String content = Files.readString(prefsFile, StandardCharsets.UTF_8);
            Type mapType = new TypeToken<Map<String, String>>(){}.getType();
            Map loaded = (Map)GSON.fromJson(content, mapType);
            if (loaded != null) {
                prefsCache.putAll(loaded);
                prefsCache.keySet().retainAll(loaded.keySet());
            }
        }
        catch (Exception e) {
            LOGGER.warning("[KsLang] Failed to load prefs.json: " + e.getMessage());
        }
    }

    private static void savePrefs() {
        Path prefsFile = KsLang.getPrefsFile();
        try {
            Files.writeString(prefsFile, (CharSequence)GSON.toJson(prefsCache), StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (IOException e) {
            LOGGER.warning("[KsLang] Failed to save prefs.json: " + e.getMessage());
        }
    }

    private static Path getPrefsFile() {
        return sharedDir.resolve("prefs.json");
    }

    private static Path getModsFile() {
        return sharedDir.resolve("mods.json");
    }

    static {
        prefsCache = new ConcurrentHashMap<String, String>();
        changeListeners = Collections.synchronizedList(new ArrayList());
        initialized = false;
        KNOWN_LANGUAGE_NAMES = new LinkedHashMap<String, String>();
        KNOWN_LANGUAGE_NAMES.put("en-US", "English");
        KNOWN_LANGUAGE_NAMES.put("de-DE", "Deutsch");
        KNOWN_LANGUAGE_NAMES.put("fr-FR", "Francais");
        KNOWN_LANGUAGE_NAMES.put("es-ES", "Espanol");
        KNOWN_LANGUAGE_NAMES.put("pt-BR", "Portugues (BR)");
        KNOWN_LANGUAGE_NAMES.put("ru-RU", "Russian");
        KNOWN_LANGUAGE_NAMES.put("pl-PL", "Polski");
        KNOWN_LANGUAGE_NAMES.put("tr-TR", "Turkish");
        KNOWN_LANGUAGE_NAMES.put("it-IT", "Italiano");
        discoveredLanguages = new LinkedHashMap<String, String>();
        lastPrefsLoad = 0L;
    }
}

