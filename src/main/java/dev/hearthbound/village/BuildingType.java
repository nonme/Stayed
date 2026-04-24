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

    // Anchor block ID (custom Hearthbound block)
    public static final String FOUNDING_STONE_BLOCK = "Hearthbound_Founding_Stone";

    // Ghost preview block (transparent, no collision)
    public static final String GHOST_BLOCK = "Hearthbound_Ghost";

    private BuildingType() {}

    /** Prefab filename without extension, or null if using programmatic generation. */
    public static String getPrefabName(String type) {
        return switch (type) {
            case TOWN_HALL -> "Townhall_lvl1_v1";
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
            default -> FOUNDING_STONE_BLOCK;
        };
    }

    /**
     * Y offset of the anchor placeholder block inside the prefab coordinate space.
     * Used to align prefab Y coords to the world Y of the founding stone.
     * Town Hall: statue starts at prefabY=2 → anchorPrefabY=2.
     */
    public static int getAnchorPrefabY(String type) {
        return switch (type) {
            case TOWN_HALL -> 2;
            default -> 0;
        };
    }

    /** Door offset (dx, dz) relative to anchor, door faces -Z at rotation=0. */
    public static int[] getDoorOffset(String type, int rotation) {
        int dx = 0;
        int dz = switch (type) {
            case TOWN_HALL -> -4;
            case WAREHOUSE -> -3;
            default -> -2;
        };
        return switch (rotation) {
            case 1 -> new int[]{-dz, dx};
            case 2 -> new int[]{-dx, -dz};
            case 3 -> new int[]{dz, -dx};
            default -> new int[]{dx, dz};
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
