package dev.hearthbound.npc;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StayedRoleNames {
    private static final Pattern GENERATED_ROLE_PATTERN = Pattern.compile(
            "^Stayed_([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})_(.+)_Role$");

    private StayedRoleNames() {}

    public static String generatedRoleName(String npcId, String baseRoleName) {
        String normalizedNpcId = normalizeNpcId(npcId);
        String normalizedBaseRole = normalizeBaseRoleName(baseRoleName);
        return "Stayed_" + normalizedNpcId + "_" + normalizedBaseRole + "_Role";
    }

    public static String extractNpcId(String roleName) {
        if (roleName == null || roleName.isBlank()) return null;
        Matcher matcher = GENERATED_ROLE_PATTERN.matcher(roleName);
        return matcher.matches() ? matcher.group(1).toLowerCase() : null;
    }

    public static String extractBaseRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) return null;
        Matcher matcher = GENERATED_ROLE_PATTERN.matcher(roleName);
        return matcher.matches() ? matcher.group(2) : roleName;
    }

    public static boolean isGeneratedRoleName(String roleName) {
        return extractNpcId(roleName) != null;
    }

    private static String normalizeNpcId(String npcId) {
        if (npcId == null || npcId.isBlank()) {
            throw new IllegalArgumentException("npcId is required for a persistent Stayed NPC role");
        }
        return UUID.fromString(npcId).toString();
    }

    private static String normalizeBaseRoleName(String baseRoleName) {
        if (baseRoleName == null || baseRoleName.isBlank()) {
            throw new IllegalArgumentException("baseRoleName is required for a persistent Stayed NPC role");
        }
        if (baseRoleName.startsWith("Stayed_")) {
            String extracted = extractBaseRoleName(baseRoleName);
            if (extracted != null && !extracted.equals(baseRoleName)) return extracted;
        }
        if (!baseRoleName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("baseRoleName contains unsupported characters: " + baseRoleName);
        }
        return baseRoleName;
    }
}
