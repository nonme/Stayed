/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.tracking;

import java.util.Set;

public final class BlockCategories {
    public static final Set<String> WOOD_GROUPS = Set.of("Wood");
    public static final Set<String> DIG_GROUPS = Set.of("Dirt", "Sand", "Gravel", "Mud", "Soil", "Snow", "Grass", "Moss", "Cover");
    public static final Set<String> MINE_GROUPS = Set.of("Stone");
    public static final Set<String> EXCLUDED_GROUPS = Set.of("Plant", "Leaves", "Fluid_Water", "Lava", "Props", "Air");
    public static final Set<String> HARVEST_GROUPS = Set.of("Plant", "Leaves", "Grass", "Moss", "Cover");
    public static final Set<String> HARVEST_EXCLUDED_GROUPS = Set.of("Wood", "Stone", "Props");

    public static String categorize(String blockGroup, String blockId) {
        if (blockGroup != null && !blockGroup.isEmpty()) {
            if (EXCLUDED_GROUPS.contains(blockGroup)) {
                return null;
            }
            if (WOOD_GROUPS.contains(blockGroup)) {
                return "chopped";
            }
            if (DIG_GROUPS.contains(blockGroup)) {
                return "dug";
            }
            if (MINE_GROUPS.contains(blockGroup)) {
                return "mined";
            }
            if (blockGroup.startsWith("Ore")) {
                return "mined";
            }
        }
        return BlockCategories.categorizeFallback(blockId);
    }

    private static String categorizeFallback(String blockId) {
        if (blockId == null || blockId.isEmpty()) {
            return null;
        }
        String lower = blockId.toLowerCase();
        if (lower.contains("air") || lower.contains("empty")) {
            return null;
        }
        if (lower.contains("wood") || lower.contains("log") || lower.contains("plank")) {
            return "chopped";
        }
        if (lower.contains("dirt") || lower.contains("sand") || lower.contains("gravel")) {
            return "dug";
        }
        return "mined";
    }

    public static boolean isHarvestable(String blockGroup, String blockId) {
        if (blockGroup != null && !blockGroup.isBlank()) {
            if (HARVEST_EXCLUDED_GROUPS.contains(blockGroup)) {
                return false;
            }
            return HARVEST_GROUPS.contains(blockGroup);
        }
        return BlockCategories.isHarvestableFallback(blockId);
    }

    private static boolean isHarvestableFallback(String blockId) {
        if (blockId == null) {
            return false;
        }
        String lower = blockId.toLowerCase();
        return lower.contains("plant") || lower.contains("flower") || lower.contains("crop") || lower.contains("grass") || lower.contains("leaves") || lower.contains("mushroom") || lower.contains("berry") || lower.contains("herb");
    }

    public static boolean isFlower(String blockGroup, String blockId) {
        if (blockId == null) {
            return false;
        }
        return blockId.toLowerCase().contains("flower");
    }

    private BlockCategories() {
    }
}

