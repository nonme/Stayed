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
    public static final String GUARD_HOUSE = "guard_house";
    public static final String FORGE = "forge";

    // Anchor block IDs (custom Hearthbound blocks)
    public static final String FOUNDING_STONE_BLOCK = "Stayed_Founding_Stone";
    public static final String BRAZIER_BLOCK = "Stayed_Brazier";
    public static final String SCARECROW_BLOCK = "Stayed_Scarecrow";
    public static final String COUNTER_BLOCK = "Stayed_Counter";
    public static final String LUMBERMILL_BLOCK = "Stayed_Lumbermill";
    public static final String MINE_SIGN_BLOCK = "Stayed_Mine_Sign";
    public static final String TARGET_DUMMY_BLOCK = "Stayed_Target_Dummy";
    public static final String FORGE_BLOCK = "Stayed_Forge";

    // Per-variant tables for HOUSE_HUMAN. variant=0 maps to the original v1 prefab so
    // existing village data (which has no variant field, defaults to 0) keeps rendering
    // the same building.
    private static final String[] HOUSE_HUMAN_PREFABS = {
            "VillagerHouse_lvl1_v1",
            "VillagerHouse_lvl1_v1_alt1",
            "VillagerHouse_lvl1_v1_alt2",
            "VillagerHouse_lvl1_v1_alt3",
    };
    // Brazier Y inside each prefab (verified by grepping the prefab files for Stayed_Brazier).
    private static final int[] HOUSE_HUMAN_ANCHOR_Y = {1, 1, 1, 1};
    private static final String[] HOUSE_HUMAN_VARIANT_NAMES = {
            "Variant 1", "Variant 2", "Variant 3", "Variant 4"
    };

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
            case TARGET_DUMMY_BLOCK -> GUARD_HOUSE;
            case FORGE_BLOCK -> FORGE;
            default -> null;
        };
    }

    public static boolean isAnchorBlock(String blockId) {
        return getBuildingTypeForAnchor(blockId) != null;
    }

    /** Prefab filename without extension, or null if using programmatic generation. */
    public static String getPrefabName(String type) {
        return getPrefabName(type, 0);
    }

    /**
     * Prefab filename without extension for the given variant, or null if using programmatic
     * generation. Only HOUSE_HUMAN currently has multiple variants; for other types the variant
     * argument is ignored. variant=0 always maps to the original prefab so old saves keep working.
     */
    public static String getPrefabName(String type, int variant) {
        if (HOUSE_HUMAN.equals(type)) {
            int v = clampHouseVariant(variant);
            return HOUSE_HUMAN_PREFABS[v];
        }
        return switch (type) {
            case TOWN_HALL -> "Townhall_lvl1_v3";
            case FARM -> "Farm_lvl1_v1";
            case WAREHOUSE -> "Warehouse_lvl1_v1";
            case SAWMILL -> "Sawmill_lvl1_v1";
            case MINE -> "Mine_lvl1_v1";
            case GUARD_HOUSE -> "GuardHouse_lvl1_v1";
            case FORGE -> "Forge_lvl1_v1";
            default -> null;
        };
    }

    public static int getHouseVariantCount() {
        return HOUSE_HUMAN_PREFABS.length;
    }

    public static String getHouseVariantName(int variant) {
        return HOUSE_HUMAN_VARIANT_NAMES[clampHouseVariant(variant)];
    }

    /** Wraps any int (positive or negative) into the valid variant range with cyclic semantics. */
    public static int wrapHouseVariant(int variant) {
        int n = HOUSE_HUMAN_PREFABS.length;
        return ((variant % n) + n) % n;
    }

    private static int clampHouseVariant(int variant) {
        if (variant < 0 || variant >= HOUSE_HUMAN_PREFABS.length) return 0;
        return variant;
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
            case GUARD_HOUSE -> TARGET_DUMMY_BLOCK;
            case FORGE -> FORGE_BLOCK;
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
        return getAnchorPrefabY(type, 0);
    }

    /**
     * Per-variant anchor Y for buildings with multiple prefab variants.
     * For HOUSE_HUMAN the brazier sits at different heights across variants
     * (e.g. alt3 has it on the second floor at Y=4). Other types ignore the variant.
     */
    public static int getAnchorPrefabY(String type, int variant) {
        if (HOUSE_HUMAN.equals(type)) {
            return HOUSE_HUMAN_ANCHOR_Y[clampHouseVariant(variant)];
        }
        return switch (type) {
            case TOWN_HALL -> 1;
            case FARM -> 2;
            case WAREHOUSE -> 1;
            case SAWMILL -> 2;
            case MINE -> 10;
            case GUARD_HOUSE -> 1;
            case FORGE -> 2;
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
     * Override the elf's "safe spawn" offset (relative to the anchor block, before world rotation
     * is applied) for buildings where the standard {@code doorOffset + doorOffset.sign() * 2}
     * trick lands inside a wall. Returns null for buildings that should use the default logic.
     *
     * <p>GuardHouse: anchor (Target Dummy) is OUTSIDE the building on the training yard, so
     * "continue past the door" pushes the elf into the building. Pin him to the dummy's own
     * tile (offset 0,0) — the yard is open ground, no collision.
     *
     * @param type             building type constant
     * @param recordRotation   yaw rotation of the placed anchor block (0..3)
     * @return {dx, dz} world-rotated offset from anchor to elf spawn, or null for default
     */
    public static int[] getSafeBuildOffset(String type, int recordRotation) {
        if (!GUARD_HOUSE.equals(type)) return null;
        // Native offset: one tile to the -X side of the dummy (open yard, no wall there).
        // Apply the same rotation transform getDoorOffset uses (anchorPrefabRotation = 1).
        int dx = -1, dz = 0, anchorPrefabRotation = 1;
        int steps = (recordRotation - anchorPrefabRotation + 4) % 4;
        return switch (steps) {
            case 1 -> new int[]{-dz,  dx};
            case 2 -> new int[]{-dx, -dz};
            case 3 -> new int[]{ dz, -dx};
            default -> new int[]{dx,   dz};
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
        return getDoorOffset(type, recordRotation, 0);
    }

    /**
     * Variant-aware overload. Different house variants have the brazier rotated differently
     * inside their prefabs, so the steps from native to world differ.
     */
    public static int[] getDoorOffset(String type, int recordRotation, int variant) {
        int dx, dz, anchorPrefabRotation;
        switch (type) {
            // Brazier anchor at prefab (2,1,3); stand two blocks in front at (2,1,1) → offset (0,-2).
            // NOTE: villager recall/assignment uses getInteriorStandPoint instead — this branch is
            // only here for legacy callers (e.g. building construction) that need a "near house" point.
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
            // GuardHouse: anchor (Target Dummy) sits OUTSIDE the building on the training yard
            // at prefab (5,1,-1). External door is at (2,1,3) facing +X — so dx=-3, dz=4 from
            // anchor to door. anchorPrefabRotation=1 because the dummy is stored with rot=1.
            // Note: anchor outside the building means the safeBuildPos/dir extension trick used
            // for other types lands inside walls. The placement code special-cases this type.
            case GUARD_HOUSE -> { dx = -3; dz =  4; anchorPrefabRotation = 1; }
            // Forge: anvil anchor at prefab (2,2,3) rot=3; door at (3,2,-10) rot=1.
            // Stand one tile past the door (z=-11) → from anchor: dx=1, dz=-14.
            case FORGE       -> { dx =  1; dz =-14; anchorPrefabRotation = 3; }
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

    /**
     * Stand-point right in front of the brazier, inside the house, used for villager
     * recall and home assignment. Independent of prefab variant — relies only on the
     * brazier's world rotation, which always points into the room (the brazier is placed
     * against a wall with its open face toward the interior in every prefab variant).
     *
     * @param brazierX brazier world X
     * @param brazierY brazier world Y (block where the brazier sits)
     * @param brazierZ brazier world Z
     * @param brazierWorldRotation rotation index 0..3 of the brazier as placed (record.getRotation())
     * @return double[]{x, y, z} center-of-tile world coordinates one block in front of the brazier
     */
    public static double[] getInteriorStandPoint(int brazierX, int brazierY, int brazierZ,
                                                  int brazierWorldRotation) {
        // rot 0 → +Z, 1 → +X, 2 → -Z, 3 → -X
        int dx, dz;
        switch (brazierWorldRotation & 0x3) {
            case 1  -> { dx =  1; dz =  0; }
            case 2  -> { dx =  0; dz = -1; }
            case 3  -> { dx = -1; dz =  0; }
            default -> { dx =  0; dz =  1; }
        }
        return new double[]{
                brazierX + dx + 0.5,
                brazierY,
                brazierZ + dz + 0.5
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
            case GUARD_HOUSE -> "Guard House";
            case FORGE -> "Forge";
            default -> type;
        };
    }
}
