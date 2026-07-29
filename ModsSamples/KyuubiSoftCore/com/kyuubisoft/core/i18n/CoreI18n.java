/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.i18n;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.kyuubisoft.core.i18n.I18nContext;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class CoreI18n {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core");
    private static final Gson GSON = new Gson();
    private static final int CURRENT_LOCALIZATION_VERSION = 30;
    private static final String VERSION_KEY = "_localization_version";
    private static final String[] BUNDLED_LANGUAGES = new String[]{"en-US", "de-DE", "fr-FR", "es-ES", "pt-BR", "ru-RU", "pl-PL", "tr-TR", "it-IT"};
    private static CoreI18n instance;
    private final Map<String, Map<String, String>> translations = new HashMap<String, Map<String, String>>();
    private String currentLanguage = "en-US";
    private Path localizationFolder;

    private CoreI18n() {
    }

    public static CoreI18n getInstance() {
        if (instance == null) {
            instance = new CoreI18n();
        }
        return instance;
    }

    public void load(String language, Path dataFolder) {
        Path localizationFolder;
        this.currentLanguage = language;
        this.localizationFolder = localizationFolder = dataFolder.resolve("localization");
        this.extractDefaultLanguages(localizationFolder);
        List<String> discoveredLanguages = this.discoverLanguages(localizationFolder);
        this.loadLanguageFile(localizationFolder.resolve("en-US.json"), "en-US");
        this.loadAllCustomFiles(localizationFolder, "en-US");
        for (String lang : discoveredLanguages) {
            if ("en-US".equals(lang)) continue;
            Path langFile = localizationFolder.resolve(lang + ".json");
            if (Files.exists(langFile, new LinkOption[0])) {
                this.loadLanguageFile(langFile, lang);
            }
            this.loadAllCustomFiles(localizationFolder, lang);
        }
        if (!discoveredLanguages.contains(language) && !"en-US".equals(language)) {
            LOGGER.warning("[Core] Configured language '" + language + "' not found in localization/ folder");
        }
        StringBuilder sb = new StringBuilder("[Core] Loaded " + discoveredLanguages.size() + " language(s): ");
        for (String lang : discoveredLanguages) {
            sb.append(lang).append("(").append(this.getTranslationCount(lang)).append(") ");
        }
        LOGGER.info(sb.toString().trim());
    }

    public List<String> discoverLanguages(Path folder) {
        ArrayList<String> languages = new ArrayList<String>();
        if (!Files.exists(folder, new LinkOption[0]) || !Files.isDirectory(folder, new LinkOption[0])) {
            languages.add("en-US");
            return languages;
        }
        try (Stream<Path> stream = Files.list(folder);){
            stream.filter(p -> {
                String name = p.getFileName().toString();
                return name.endsWith(".json") && !name.startsWith("custom_") && !name.contains(".backup") && !name.contains(".example");
            }).forEach(p -> {
                String name = p.getFileName().toString();
                String langCode = name.substring(0, name.length() - 5);
                languages.add(langCode);
            });
        }
        catch (IOException e) {
            LOGGER.warning("[Core] Failed to scan localization folder: " + e.getMessage());
        }
        languages.remove("en-US");
        languages.sort(String::compareTo);
        languages.addFirst("en-US");
        return languages;
    }

    private void extractDefaultLanguages(Path localizationFolder) {
        try {
            if (!Files.exists(localizationFolder, new LinkOption[0])) {
                Files.createDirectories(localizationFolder, new FileAttribute[0]);
                LOGGER.info("[Core] Created localization folder: " + String.valueOf(localizationFolder));
            }
            for (String lang : BUNDLED_LANGUAGES) {
                this.extractOrUpdateLanguage("defaults/localization/" + lang + ".json", localizationFolder.resolve(lang + ".json"), lang);
            }
        }
        catch (IOException e) {
            LOGGER.warning("[Core] Failed to extract default languages: " + e.getMessage());
        }
    }

    private void extractOrUpdateLanguage(String resourcePath, Path targetPath, String langName) throws IOException {
        if (!Files.exists(targetPath, new LinkOption[0])) {
            this.extractResource(resourcePath, targetPath);
            LOGGER.info("[Core] Extracted: " + String.valueOf(targetPath.getFileName()));
            return;
        }
        int existingVersion = this.getFileVersion(targetPath);
        if (existingVersion < 30) {
            Path backupPath = targetPath.getParent().resolve(langName + ".json.backup-v" + existingVersion);
            try {
                Files.copy(targetPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("[Core] Created backup: " + String.valueOf(backupPath.getFileName()));
                Files.delete(targetPath);
                this.extractResource(resourcePath, targetPath);
                LOGGER.info("[Core] Updated " + langName + ".json from v" + existingVersion + " to v30");
            }
            catch (Exception e) {
                LOGGER.warning("[Core] Failed to update " + langName + ".json: " + e.getMessage());
            }
        }
    }

    private int getFileVersion(Path filePath) {
        try {
            String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            Type mapType = new TypeToken<Map<String, String>>(this){
                {
                    Objects.requireNonNull(this$0);
                }
            }.getType();
            Map langMap = (Map)GSON.fromJson(content, mapType);
            if (langMap != null && langMap.containsKey(VERSION_KEY)) {
                try {
                    return Integer.parseInt((String)langMap.get(VERSION_KEY));
                }
                catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return 0;
    }

    private void extractResource(String resourcePath, Path targetPath) throws IOException {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(resourcePath);){
            if (is == null) {
                LOGGER.warning("[Core] Resource not found in JAR: " + resourcePath);
                return;
            }
            Files.copy(is, targetPath, new CopyOption[0]);
        }
    }

    private void loadAllCustomFiles(Path folder, String language) {
        String prefix = "custom_" + language;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, prefix + "*.json");){
            ArrayList<Path> files = new ArrayList<Path>();
            for (Path file : stream) {
                files.add(file);
            }
            files.sort(Comparator.comparing(p -> p.getFileName().toString()));
            for (Path file : files) {
                this.loadCustomFile(file, language);
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private void loadCustomFile(Path filePath, String language) {
        if (!Files.exists(filePath, new LinkOption[0])) {
            return;
        }
        try {
            String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            Type mapType = new TypeToken<Map<String, String>>(this){
                {
                    Objects.requireNonNull(this$0);
                }
            }.getType();
            Map customMap = (Map)GSON.fromJson(content, mapType);
            if (customMap != null && !customMap.isEmpty()) {
                this.translations.computeIfAbsent(language, k -> new HashMap()).putAll(customMap);
                LOGGER.info("[Core] Loaded " + customMap.size() + " custom translations from " + String.valueOf(filePath.getFileName()));
            }
        }
        catch (Exception e) {
            LOGGER.warning("[Core] Failed to load custom file " + String.valueOf(filePath) + ": " + e.getMessage());
        }
    }

    private void loadLanguageFile(Path filePath, String language) {
        try {
            Type mapType;
            if (!Files.exists(filePath, new LinkOption[0])) {
                LOGGER.warning("[Core] Language file not found: " + String.valueOf(filePath));
                return;
            }
            String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            Map langMap = (Map)GSON.fromJson(content, mapType = new TypeToken<Map<String, String>>(this){
                {
                    Objects.requireNonNull(this$0);
                }
            }.getType());
            if (langMap == null) {
                LOGGER.warning("[Core] Failed to parse language file (null result): " + String.valueOf(filePath));
                return;
            }
            this.translations.put(language, langMap);
        }
        catch (Exception e) {
            LOGGER.warning("[Core] Failed to load language file " + String.valueOf(filePath) + ": " + e.getMessage());
        }
    }

    public String get(String key) {
        String value;
        Map<String, String> fallbackLang;
        String value2;
        Map<String, String> langMap;
        String lang = I18nContext.getForMod("core");
        if (lang == null) {
            lang = this.currentLanguage;
        }
        if ((langMap = this.translations.get(lang)) != null && (value2 = langMap.get(key)) != null) {
            return value2;
        }
        if (!"en-US".equals(lang) && (fallbackLang = this.translations.get("en-US")) != null && (value = fallbackLang.get(key)) != null) {
            return value;
        }
        return key;
    }

    public String get(String key, Object ... args) {
        String template = this.get(key);
        for (int i = 0; i < args.length; ++i) {
            template = template.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return template;
    }

    public String get(String key, Map<String, Object> placeholders) {
        String template = this.get(key);
        for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
            template = template.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return template;
    }

    public boolean has(String key) {
        Map<String, String> langMap;
        String lang = I18nContext.getForMod("core");
        if (lang == null) {
            lang = this.currentLanguage;
        }
        if ((langMap = this.translations.get(lang)) != null && langMap.containsKey(key)) {
            return true;
        }
        Map<String, String> fallbackLang = this.translations.get("en-US");
        return fallbackLang != null && fallbackLang.containsKey(key);
    }

    public String getCurrentLanguage() {
        return this.currentLanguage;
    }

    public int getTranslationCount(String language) {
        Map<String, String> langMap = this.translations.get(language);
        return langMap != null ? langMap.size() : 0;
    }

    public void reload(Path dataFolder) {
        this.translations.clear();
        this.load(this.currentLanguage, dataFolder);
    }

    public void setCustomTranslation(String language, String key, String value) {
        this.translations.computeIfAbsent(language, k -> new HashMap()).put(key, value);
        if (this.localizationFolder == null) {
            LOGGER.warning("[Core] Cannot save custom translation: localizationFolder not set");
            return;
        }
        Path customFile = this.localizationFolder.resolve("custom_" + language + ".json");
        Map<String, String> customMap = this.loadOrCreateCustomMap(customFile);
        if (value == null || value.isEmpty()) {
            customMap.remove(key);
        } else {
            customMap.put(key, value);
        }
        this.saveCustomFile(customFile, customMap);
    }

    public String[] getAvailableLanguages() {
        return BUNDLED_LANGUAGES;
    }

    private Map<String, String> loadOrCreateCustomMap(Path customFile) {
        if (Files.exists(customFile, new LinkOption[0])) {
            try {
                String content = new String(Files.readAllBytes(customFile), StandardCharsets.UTF_8);
                Type mapType = new TypeToken<Map<String, String>>(this){
                    {
                        Objects.requireNonNull(this$0);
                    }
                }.getType();
                Map map = (Map)GSON.fromJson(content, mapType);
                if (map != null) {
                    return new LinkedHashMap<String, String>(map);
                }
            }
            catch (Exception e) {
                LOGGER.warning("[Core] Failed to read custom file: " + e.getMessage());
            }
        }
        return new LinkedHashMap<String, String>();
    }

    private void saveCustomFile(Path customFile, Map<String, String> map) {
        try {
            Files.createDirectories(customFile.getParent(), new FileAttribute[0]);
            Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
            String json = prettyGson.toJson(map);
            Files.writeString(customFile, (CharSequence)json, StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (IOException e) {
            LOGGER.warning("[Core] Failed to save custom file " + String.valueOf(customFile) + ": " + e.getMessage());
        }
    }
}

