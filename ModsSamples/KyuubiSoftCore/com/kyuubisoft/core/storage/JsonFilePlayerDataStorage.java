/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.storage;

import com.kyuubisoft.core.storage.PlayerDataStorage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.UUID;
import java.util.logging.Logger;

public class JsonFilePlayerDataStorage
implements PlayerDataStorage {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Storage");
    private final Path baseDir;

    public JsonFilePlayerDataStorage(Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public String loadJson(String tableName, UUID playerId) {
        Path file = this.resolveFile(tableName, playerId);
        Path backup = this.resolveBackup(tableName, playerId);
        String json = this.tryReadFile(file);
        if (json != null) {
            return json;
        }
        json = this.tryReadFile(backup);
        if (json != null) {
            LOGGER.info("Recovered data from backup: " + String.valueOf(backup.getFileName()));
        }
        return json;
    }

    @Override
    public void saveJson(String tableName, UUID playerId, String username, String json) {
        Path dir = this.baseDir.resolve(tableName);
        Path file = dir.resolve(String.valueOf(playerId) + ".json");
        try {
            Files.createDirectories(dir, new FileAttribute[0]);
            if (Files.exists(file, new LinkOption[0])) {
                Path backup = dir.resolve(String.valueOf(playerId) + ".json.bak");
                Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(file, (CharSequence)json, StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (IOException e) {
            LOGGER.warning("Failed to save data for " + String.valueOf(playerId) + " in " + tableName + ": " + e.getMessage());
        }
    }

    @Override
    public void delete(String tableName, UUID playerId) {
        try {
            Path file = this.resolveFile(tableName, playerId);
            Path backup = this.resolveBackup(tableName, playerId);
            Files.deleteIfExists(file);
            Files.deleteIfExists(backup);
        }
        catch (IOException e) {
            LOGGER.warning("Failed to delete data for " + String.valueOf(playerId) + " in " + tableName + ": " + e.getMessage());
        }
    }

    @Override
    public boolean exists(String tableName, UUID playerId) {
        return Files.exists(this.resolveFile(tableName, playerId), new LinkOption[0]);
    }

    @Override
    public void ensureTableExists(String tableName) {
        try {
            Files.createDirectories(this.baseDir.resolve(tableName), new FileAttribute[0]);
        }
        catch (IOException e) {
            LOGGER.warning("Failed to create directory for " + tableName + ": " + e.getMessage());
        }
    }

    @Override
    public void shutdown() {
    }

    @Override
    public String getTypeName() {
        return "file";
    }

    public Path getBaseDir() {
        return this.baseDir;
    }

    private Path resolveFile(String tableName, UUID playerId) {
        return this.baseDir.resolve(tableName).resolve(String.valueOf(playerId) + ".json");
    }

    private Path resolveBackup(String tableName, UUID playerId) {
        return this.baseDir.resolve(tableName).resolve(String.valueOf(playerId) + ".json.bak");
    }

    private String tryReadFile(Path path) {
        if (!Files.exists(path, new LinkOption[0])) {
            return null;
        }
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            if (content != null && !content.isBlank()) {
                return content;
            }
        }
        catch (IOException e) {
            LOGGER.info("Could not read file " + String.valueOf(path) + ": " + e.getMessage());
        }
        return null;
    }
}

