package dev.hearthbound.npc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
public final class StayedRoleGenerator {
    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("npc.role.gen");
    private static final StayedRoleGenerator INSTANCE =
            new StayedRoleGenerator(StayedRoleAssetPackManager.rolesPath());

    private final Path generatedRolesPath;
    private final ConcurrentHashMap<String, String> lastGeneratedContent = new ConcurrentHashMap<>();

    public static StayedRoleGenerator get() {
        return INSTANCE;
    }

    public StayedRoleGenerator(Path generatedRolesPath) {
        this.generatedRolesPath = generatedRolesPath;
    }

    public String generateRoleIfChanged(NpcRegistry.NpcRecord record) {
        if (record == null) throw new IllegalArgumentException("record is required");
        if (record.interaction == NpcRegistry.InteractionType.NONE) {
            return record.baseRoleName();
        }
        return generateRoleIfChanged(record.npcId, record.baseRoleName());
    }

    public String generateRoleIfChanged(String npcId, String baseRoleName) {
        String generatedRoleName = StayedRoleNames.generatedRoleName(npcId, baseRoleName);
        String content = loadBaseRoleJson(baseRoleName);
        writeIfChanged(generatedRoleName, content);
        return generatedRoleName;
    }

    public void regenerateAllRoles(Collection<NpcRegistry.NpcRecord> records) {
        if (records == null) return;
        for (NpcRegistry.NpcRecord record : records) {
            if (record == null || record.npcId == null || record.npcId.isBlank()) continue;
            if (record.interaction == NpcRegistry.InteractionType.NONE) continue;
            generateRoleIfChanged(record.npcId, record.baseRoleName());
        }
    }

    private String loadBaseRoleJson(String baseRoleName) {
        String normalized = StayedRoleNames.extractBaseRoleName(baseRoleName);
        String resourcePath = "/Server/NPC/Roles/" + normalized + ".json";
        try (InputStream input = StayedRoleGenerator.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing bundled base role JSON: " + resourcePath);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed reading base role JSON " + resourcePath, e);
        }
    }

    private void writeIfChanged(String generatedRoleName, String content) {
        try {
            Files.createDirectories(generatedRolesPath);
            String previous = lastGeneratedContent.get(generatedRoleName);
            Path file = generatedRolesPath.resolve(generatedRoleName + ".json");
            if (previous == null && Files.exists(file)) {
                previous = Files.readString(file, StandardCharsets.UTF_8);
            }
            if (content.equals(previous)) return;
            Files.writeString(file, content, StandardCharsets.UTF_8);
            lastGeneratedContent.put(generatedRoleName, content);
            LOG.info("[STAYED-ROLEGEN] wrote " + generatedRoleName);
        } catch (IOException e) {
            throw new IllegalStateException("Failed writing generated role " + generatedRoleName, e);
        }
    }

    @SuppressWarnings("unused")
    private void cleanupStaleGeneratedRoles(String npcId, Set<String> activeRoleNames) {
        String prefix = "Stayed_" + npcId + "_";
        try {
            Files.createDirectories(generatedRolesPath);
            try (var stream = Files.list(generatedRolesPath)) {
                stream.filter(path -> {
                    String name = path.getFileName().toString();
                    return name.startsWith(prefix) && name.endsWith(".json");
                }).forEach(path -> {
                    String filename = path.getFileName().toString();
                    String roleName = filename.substring(0, filename.length() - ".json".length());
                    if (activeRoleNames.contains(roleName)) return;
                    try {
                        Files.deleteIfExists(path);
                        lastGeneratedContent.remove(roleName);
                        LOG.info("[STAYED-ROLEGEN] deleted stale " + roleName);
                    } catch (IOException e) {
                        LOG.warn("Failed deleting stale generated role " + roleName + ": " + e.getMessage());
                    }
                });
            }
        } catch (IOException e) {
            LOG.warn("Failed scanning generated role directory: " + e.getMessage());
        }
    }
}
