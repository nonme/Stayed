/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class DevExportService {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Path exportFolder;

    public DevExportService(Path dataFolder) {
        this.exportFolder = dataFolder.resolve("dev-export");
    }

    public Path export(String type, int count, Object data) {
        try {
            Files.createDirectories(this.exportFolder, new FileAttribute[0]);
            LinkedHashMap<String, Object> envelope = new LinkedHashMap<String, Object>();
            envelope.put("_type", type);
            envelope.put("_count", count);
            envelope.put("_exportedAt", LocalDateTime.now().format(TIMESTAMP));
            envelope.put("data", data);
            Path file = this.exportFolder.resolve(type + ".json");
            Files.writeString(file, (CharSequence)GSON.toJson(envelope), StandardCharsets.UTF_8, new OpenOption[0]);
            return file;
        }
        catch (IOException e) {
            LOGGER.warning("Failed to export " + type + ": " + e.getMessage());
            return null;
        }
    }

    public Path exportRaw(String type, Map<String, Object> envelope) {
        try {
            Files.createDirectories(this.exportFolder, new FileAttribute[0]);
            Path file = this.exportFolder.resolve(type + ".json");
            Files.writeString(file, (CharSequence)GSON.toJson(envelope), StandardCharsets.UTF_8, new OpenOption[0]);
            return file;
        }
        catch (IOException e) {
            LOGGER.warning("Failed to export " + type + ": " + e.getMessage());
            return null;
        }
    }
}

