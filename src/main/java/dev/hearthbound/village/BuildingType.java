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
    public static final String WOODCUTTERS_HUT = "woodcutters_hut";
    /** Legacy type string stored in BSON by v0.8.x saves. Kept as a read-only alias — always maps to WOODCUTTERS_HUT display/prefab logic. */
    public static final String SAWMILL_LEGACY = "sawmill";
    public static final String SAWMILL = "sawmill_v2";
    public static final String MINE = "mine";
    public static final String GUARD_HOUSE = "guard_house";
    public static final String FORGE = "forge";
    public static final String TAVERN = "tavern";

    // Functional categories — used by the Founder's Almanac to group buildings in the catalog.
    // These are display-only strings and are NOT persisted to BSON, so they can be renamed freely.
    public static final String CATEGORY_CIVIC      = "civic";
    public static final String CATEGORY_PRODUCTION = "production";
    public static final String CATEGORY_CRAFTING   = "crafting";
    public static final String CATEGORY_SERVICES   = "services";
    public static final String CATEGORY_HOUSING    = "housing";
    public static final String CATEGORY_DEFENSE    = "defense";
    public static final String CATEGORY_STORAGE    = "storage";

    // Anchor block IDs (custom Hearthbound blocks)
    public static final String FOUNDING_STONE_BLOCK = "Stayed_Founding_Stone";
    public static final String HOUSE_BLOCK = "Stayed_House";
    public static final String FARM_BLOCK = "Stayed_Farm";
    public static final String WAREHOUSE_BLOCK = "Stayed_Warehouse";
    public static final String WOODCUTTERS_HUT_BLOCK = "Stayed_Woodcutters_Hut";
    public static final String SAWMILL_BLOCK = "Stayed_Sawmill";
    public static final String MINE_BLOCK = "Stayed_Mine";
    public static final String GUARD_HOUSE_BLOCK = "Stayed_Guard_House";
    public static final String FORGE_BLOCK = "Stayed_Forge";
    public static final String TAVERN_BLOCK = "Stayed_Tavern";

    // Per-variant tables for HOUSE_HUMAN. variant=0..3 = v2 variants.
    private static final String[] HOUSE_HUMAN_PREFABS = {
            "VillagerHouse_lvl1_v2",
            "VillagerHouse_lvl1_v2_alt1",
            "VillagerHouse_lvl1_v2_alt2",
            "VillagerHouse_lvl1_v2_alt3",
    };
    // Brazier Y inside each prefab (verified by grepping the prefab files for Stayed_House).
    private static final int[] HOUSE_HUMAN_ANCHOR_Y = {1, 1, 1, 1};
    private static final String[] HOUSE_HUMAN_VARIANT_NAMES = {
            "Variant 1", "Variant 2", "Variant 3", "Variant 4"
    };

    private BuildingType() {}

    /** Returns the building type for the given anchor block ID, or null if not an anchor. */
    public static String getBuildingTypeForAnchor(String blockId) {
        return switch (blockId) {
            case FOUNDING_STONE_BLOCK -> TOWN_HALL;
            case HOUSE_BLOCK -> HOUSE_HUMAN;
            case FARM_BLOCK -> FARM;
            case WAREHOUSE_BLOCK -> WAREHOUSE;
            case WOODCUTTERS_HUT_BLOCK -> WOODCUTTERS_HUT;
            case SAWMILL_BLOCK -> SAWMILL;
            case MINE_BLOCK -> MINE;
            case GUARD_HOUSE_BLOCK -> GUARD_HOUSE;
            case FORGE_BLOCK -> FORGE;
            case TAVERN_BLOCK -> TAVERN;
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
            case FARM -> "Farm_lvl1_v2";
            case WAREHOUSE -> "Warehouse_lvl1_v2";
            case WOODCUTTERS_HUT, SAWMILL_LEGACY -> "WoodcuttersHut_lvl1_v1";
            case SAWMILL -> "Sawmill_lvl1_v2";
            case MINE -> "Mine_lvl1_v2";
            case GUARD_HOUSE -> "GuardHouse_lvl1_v1";
            case FORGE -> "Forge_lvl1_v1";
            case TAVERN -> "Tavern_lvl1_v1";
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
            case HOUSE_HUMAN -> HOUSE_BLOCK;
            case FARM -> FARM_BLOCK;
            case WAREHOUSE -> WAREHOUSE_BLOCK;
            case WOODCUTTERS_HUT, SAWMILL_LEGACY -> WOODCUTTERS_HUT_BLOCK;
            case SAWMILL -> SAWMILL_BLOCK;
            case MINE -> MINE_BLOCK;
            case GUARD_HOUSE -> GUARD_HOUSE_BLOCK;
            case FORGE -> FORGE_BLOCK;
            case TAVERN -> TAVERN_BLOCK;
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
            case TOWN_HALL -> 2;
            case FARM -> 1;
            case WAREHOUSE -> 2;
            case WOODCUTTERS_HUT, SAWMILL_LEGACY -> 1;
            case SAWMILL -> 4;
            case MINE -> 8;
            case GUARD_HOUSE -> 1;
            case FORGE -> 2;
            case TAVERN -> 4;
            default -> 0;
        };
    }

    /**
     * Explicit NPC stand-point for buildings where auto-detection fails (e.g. mine — pit filled
     * with stone makes center-of-mass useless). Returns {centerLX, floorLY, centerLZ} or null
     * to fall back to auto-detection.
     */
    public static int[] getWorkPointOverride(String type) {
        // Farm v2: anchor (Stayed_Farm) at (-3,1,0). Centre of field at prefab (0,0,0) → lx=3, ly=0, lz=0.
        if (FARM.equals(type)) return new int[]{3, 0, 0};
        // Mine v2: anchor (Stayed_Mine) at prefab (9,8,4). Cave floor Empty blocks centred at (0,1,-4)
        // in prefab space → local lx=-9, lz=-4. floorLY=-7 (Empty air at cave bottom; solid floor at -8).
        if (MINE.equals(type)) return new int[]{-9, -7, -4};
        return null;
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
            // v3: anchor at (0,2,2) rot=2, door at (0,2,-2) facing -Z, stand at z=-3 → dx=0, dz=-5.
            case TOWN_HALL   -> { dx =  0; dz = -5; anchorPrefabRotation = 2; }
            // v2: anchor (1,2,2) rot=0, entrance at prefab (0,*,-2), exterior at (0,*,-3) → dx=-1, dz=-5.
            case WAREHOUSE   -> { dx = -1; dz = -5; anchorPrefabRotation = 0; }
            // Farm v2: scarecrow anchor at (-3,1,0) rot=1, no gate — stand beside anchor.
            case FARM        -> { dx =  1; dz =  0; anchorPrefabRotation = 1; }
            // WoodcuttersHut: open yard, no door — stand in front of the building.
            case WOODCUTTERS_HUT, SAWMILL_LEGACY -> { dx =  0; dz = -3; anchorPrefabRotation = 2; }
            // Sawmill (v2): open yard — anchor at prefab (2,4,5) rot=2, stand in front.
            case SAWMILL     -> { dx =  0; dz = -3; anchorPrefabRotation = 2; }
            // v2: anchor (9,8,5) rot=3, fence gate (4,8,-9) rot=1 facing +X, stand outside at (3,8,-9) → dx=-6, dz=-14.
            case MINE        -> { dx = -6; dz =-14; anchorPrefabRotation = 3; }
            // GuardHouse: anchor (Target Dummy) sits OUTSIDE the building on the training yard
            // at prefab (5,1,-1). External door is at (2,1,3) facing +X — so dx=-3, dz=4 from
            // anchor to door. anchorPrefabRotation=1 because the dummy is stored with rot=1.
            // Note: anchor outside the building means the safeBuildPos/dir extension trick used
            // for other types lands inside walls. The placement code special-cases this type.
            case GUARD_HOUSE -> { dx = -3; dz =  4; anchorPrefabRotation = 1; }
            // Forge: anvil anchor at prefab (2,2,3) rot=3; door at (3,2,-10) rot=1.
            // Stand one tile past the door (z=-11) → from anchor: dx=1, dz=-14.
            case FORGE       -> { dx =  1; dz =-14; anchorPrefabRotation = 3; }
            // Tavern: anchor (-4,4,4) rot=2, main door (-1,1,0) rot=2 facing -Z, stand at (-1,1,-1) → dx=3, dz=-5.
            case TAVERN      -> { dx =  3; dz = -5; anchorPrefabRotation = 2; }
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
            case WOODCUTTERS_HUT, SAWMILL_LEGACY -> "Woodcutter's Hut";
            case SAWMILL -> "Sawmill";
            case MINE -> "Mine";
            case GUARD_HOUSE -> "Guard House";
            case FORGE -> "Forge";
            case TAVERN -> "Tavern";
            default -> type;
        };
    }

    /**
     * Functional category for the Almanac catalog. Returns CATEGORY_* constants;
     * unknown types default to CATEGORY_CIVIC (so the Almanac never drops a row silently).
     */
    public static String getCategory(String type) {
        return switch (type) {
            case TOWN_HALL -> CATEGORY_CIVIC;
            case FARM, WOODCUTTERS_HUT, SAWMILL_LEGACY, MINE -> CATEGORY_PRODUCTION;
            case SAWMILL, FORGE -> CATEGORY_CRAFTING;
            case TAVERN -> CATEGORY_SERVICES;
            case HOUSE_HUMAN, HOUSE_KWEEBEC, HOUSE_TRORK -> CATEGORY_HOUSING;
            case GUARD_HOUSE -> CATEGORY_DEFENSE;
            case WAREHOUSE -> CATEGORY_STORAGE;
            default -> CATEGORY_CIVIC;
        };
    }

    public static String getCategoryDisplayName(String category) {
        return switch (category) {
            case CATEGORY_CIVIC      -> "Civic";
            case CATEGORY_PRODUCTION -> "Production";
            case CATEGORY_CRAFTING   -> "Crafting";
            case CATEGORY_SERVICES   -> "Services";
            case CATEGORY_HOUSING    -> "Housing";
            case CATEGORY_DEFENSE    -> "Defense";
            case CATEGORY_STORAGE    -> "Storage";
            default                  -> category;
        };
    }

    /** Short description shown in the Almanac next to the building name. */
    public static String getShortDescription(String type) {
        return switch (type) {
            case TOWN_HALL       -> "Heart of the village. Aelin lives here.";
            case WAREHOUSE       -> "Central storage for the settlement.";
            case HOUSE_HUMAN     -> "Home for two settlers — a pair can raise children here.";
            case FARM            -> "Grows vegetables and grain.";
            case WOODCUTTERS_HUT,
                 SAWMILL_LEGACY  -> "A lumberjack fells nearby trees.";
            case SAWMILL         -> "Carpenter turns logs into planks more efficiently.";
            case MINE            -> "Stone, coal and ore from deep underground.";
            case GUARD_HOUSE     -> "A guard patrols the village perimeter.";
            case FORGE           -> "Smith crafts weapons, tools and armour from ore.";
            case TAVERN          -> "Cooked meals raise everyone's spirits.";
            default              -> "";
        };
    }
}
