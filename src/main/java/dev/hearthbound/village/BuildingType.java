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
    public static final String FARM = "farm";
    public static final String SAWMILL = "sawmill";
    public static final String MINE = "mine";

    // Anchor block IDs (custom Hearthbound blocks)
    public static final String FOUNDING_STONE_BLOCK = "Stayed_Founding_Stone";
    public static final String BRAZIER_BLOCK = "Stayed_Brazier";
    public static final String SCARECROW_BLOCK = "Stayed_Scarecrow";
    public static final String COUNTER_BLOCK = "Stayed_Counter";
    public static final String LUMBERMILL_BLOCK = "Stayed_Lumbermill";
    public static final String MINE_SIGN_BLOCK = "Stayed_Mine_Sign";


    private BuildingType() {}

    /** Returns the building type for the given anchor block ID, or null if not an anchor. */
    public static String getBuildingTypeForAnchor(String blockId) {
        return switch (blockId) {
            case FOUNDING_STONE_BLOCK -> TOWN_HALL;
            case BRAZIER_BLOCK -> HOUSE_HUMAN;
            case SCARECROW_BLOCK -> FARM;
            case COUNTER_BLOCK -> WAREHOUSE;
            case LUMBERMILL_BLOCK -> SAWMILL;
            case MINE_SIGN_BLOCK -> MINE;
            default -> null;
        };
    }

    public static boolean isAnchorBlock(String blockId) {
        return getBuildingTypeForAnchor(blockId) != null;
    }

    /** Prefab filename without extension, or null if using programmatic generation. */
    public static String getPrefabName(String type) {
        return switch (type) {
            case TOWN_HALL -> "Townhall_lvl1_v3";
            case HOUSE_HUMAN -> "VillagerHouse_lvl1_v1";
            case FARM -> "Farm_lvl1_v1";
            case WAREHOUSE -> "Warehouse_lvl1_v1";
            case SAWMILL -> "Sawmill_lvl1_v1";
            case MINE -> "Mine_lvl1_v1";
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
            case FARM -> SCARECROW_BLOCK;
            case WAREHOUSE -> COUNTER_BLOCK;
            case SAWMILL -> LUMBERMILL_BLOCK;
            case MINE -> MINE_SIGN_BLOCK;
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
            case TOWN_HALL -> 1;
            case HOUSE_HUMAN -> 1;
            case FARM -> 2;
            case WAREHOUSE -> 1;
            case SAWMILL -> 2;
            case MINE -> 10;
            default -> 0;
        };
    }

    /**
     * Explicit NPC stand-point for buildings where auto-detection fails (e.g. mine — pit filled
     * with stone makes center-of-mass useless). Returns {centerLX, floorLY, centerLZ} or null
     * to fall back to auto-detection.
     * Measured in-game: Mine_Sign anchor → miner stood at world (-129,71,229), anchor (-118,80,227),
     * rot=1 steps=0 → local offset lx=-11.5 ly=-10 lz=+1.5 → rounded lx=-12 lz=2 floorLY=-10.
     */
    public static int[] getWorkPointOverride(String type) {
        return switch (type) {
            case MINE -> new int[]{-12, -10, 2};
            default -> null;
        };
    }

    /**
     * Door position offset (dx, dz) relative to the anchor block, accounting for building rotation.
     * Only used for NPC positioning (elf after build, villager recall/assign) — not for block placement.
     *
     * Base offsets are in prefab-native coords (before any rotation):
     *   HOUSE_HUMAN: Brazier at prefab (x=2,z=3), door center at (x=1,z=-4) → dx=-1, dz=-7
     *   TOWN_HALL:   anchor at (-5,1,1), door at (0,-2) facing -Z; stand at (0,-3) → dx=5, dz=-4
     *
     * The prefab anchor block has its own rotation baked in (Brazier rotation=2 in prefab).
     * Actual rotation steps = (record.rotation - anchorPrefabRotation + 4) % 4.
     */
    public static int[] getDoorOffset(String type, int recordRotation) {
        int dx, dz, anchorPrefabRotation;
        switch (type) {
            // Brazier anchor at prefab (2,1,3); stand two blocks in front at (2,1,1) → offset (0,-2).
            case HOUSE_HUMAN -> { dx =  0; dz = -2; anchorPrefabRotation = 0; }
            // Anchor at prefab (-5,1,1); main door at (0,1,-2) facing -Z; stand at (0,-3) → dx=5, dz=-4.
            case TOWN_HALL   -> { dx =  5; dz = -4; anchorPrefabRotation = 0; }
            case WAREHOUSE   -> { dx =  0; dz = -3; anchorPrefabRotation = 0; }
            // Farm: scarecrow anchor at prefab (0,2,4), gate at (0,1,-4) → dz = -4-4 = -8.
            // Stand just outside the gate (one block in front) → dz = -9.
            case FARM        -> { dx =  0; dz = -9; anchorPrefabRotation = 2; }
            // Sawmill: open yard, no door — stand in front of the building.
            case SAWMILL     -> { dx =  0; dz = -3; anchorPrefabRotation = 2; }
            // Mine: door at prefab (7,10,-5), anchor at (4,10,1) → dx=3, dz=-6. anchorPrefabRotation=1.
            case MINE        -> { dx =  3; dz = -6; anchorPrefabRotation = 1; }
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
            case FARM -> "Farm";
            case SAWMILL -> "Sawmill";
            case MINE -> "Mine";
            default -> type;
        };
    }
}
