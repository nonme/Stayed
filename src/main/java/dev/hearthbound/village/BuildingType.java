package dev.hearthbound.village;

/**
 * Constants for building types.
 * Using string constants instead of enum for easier BSON serialization.
 */
public final class BuildingType {

    public static final String TOWN_HALL = "town_hall";
    public static final String WAREHOUSE = "warehouse";
    public static final String HOUSE_HUMAN = "house_human";
    public static final String HOUSE_KWEEBEC = "house_kweebec";
    public static final String HOUSE_TRORK = "house_trork";

    // Anchor block IDs (custom Hearthbound blocks)
    public static final String FOUNDING_STONE_BLOCK = "Hearthbound_Founding_Stone";
    public static final String BRAZIER_BLOCK = "Hearthbound_Brazier";

    // Ghost preview block (transparent, no collision)
    public static final String GHOST_BLOCK = "Hearthbound_Ghost";

    private BuildingType() {}

    /** Prefab filename without extension, or null if using programmatic generation. */
    public static String getPrefabName(String type) {
        return switch (type) {
            case TOWN_HALL -> "Townhall_lvl1_v1";
            case HOUSE_HUMAN -> "VillagerHouse_lvl1_v1";
            default -> null;
        };
    }

    /**
     * Block ID used as the anchor placeholder in the prefab.
     * This block is already placed by the player in the world — skip it during placement.
     */
    public static String getAnchorBlockId(String type) {
        return switch (type) {
            case TOWN_HALL -> FOUNDING_STONE_BLOCK;
            case HOUSE_HUMAN -> BRAZIER_BLOCK;
            default -> FOUNDING_STONE_BLOCK;
        };
    }

    /**
     * Y offset of the anchor placeholder block inside the prefab coordinate space.
     * Used to align prefab Y coords to the world Y of the anchor block.
     * Town Hall: statue at prefabY=2 → anchorPrefabY=2.
     * House: brazier Y will be set once prefab is finalized (default 0 until then).
     */
    public static int getAnchorPrefabY(String type) {
        return switch (type) {
            case TOWN_HALL -> 2;
            case HOUSE_HUMAN -> 1;
            default -> 0;
        };
    }

    /**
     * Door position offset (dx, dz) relative to the anchor block, accounting for building rotation.
     * Only used for NPC positioning (elf after build, villager recall/assign) — not for block placement.
     *
     * Base offsets are in prefab-native coords (before any rotation):
     *   HOUSE_HUMAN: Brazier at prefab (x=2,z=3), door center at (x=1,z=-4) → dx=-1, dz=-7
     *   TOWN_HALL:   anchor at (0,0), door at z=-4 → dx=0, dz=-4
     *
     * The prefab anchor block has its own rotation baked in (Brazier rotation=2 in prefab).
     * Actual rotation steps = (record.rotation - anchorPrefabRotation + 4) % 4.
     */
    public static int[] getDoorOffset(String type, int recordRotation) {
        int dx, dz, anchorPrefabRotation;
        switch (type) {
            case HOUSE_HUMAN -> { dx = -1; dz = -3; anchorPrefabRotation = 2; }
            case TOWN_HALL   -> { dx =  0; dz = -4; anchorPrefabRotation = 0; }
            case WAREHOUSE   -> { dx =  0; dz = -3; anchorPrefabRotation = 0; }
            default          -> { dx =  0; dz = -2; anchorPrefabRotation = 0; }
        }
        int steps = (recordRotation - anchorPrefabRotation + 4) % 4;
        return switch (steps) {
            case 1 -> new int[]{-dz,  dx};
            case 2 -> new int[]{-dx, -dz};
            case 3 -> new int[]{ dz, -dx};
            default -> new int[]{dx,   dz};
        };
    }

    public static boolean isResidential(String type) {
        return type != null && type.startsWith("house_");
    }

    public static String getDisplayName(String type) {
        return switch (type) {
            case TOWN_HALL -> "Town Hall";
            case WAREHOUSE -> "Warehouse";
            case HOUSE_HUMAN -> "Human House";
            case HOUSE_KWEEBEC -> "Kweebec House";
            case HOUSE_TRORK -> "Trork House";
            default -> type;
        };
    }
}
