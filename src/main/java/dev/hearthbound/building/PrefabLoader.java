package dev.hearthbound.building;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Loads a .prefab.json and converts it to a BlockEntry list for ResourceBlockPlacer.
 *
 * Coordinate mapping: the founding stone in the world sits at the same position as
 * the anchor block placeholder in the prefab. The prefab anchor (anchorX/Y/Z from the
 * file) is at (0,0,0); the founding stone placeholder (e.g. Furniture_Village_Statue)
 * sits at a known offset inside the prefab. Pass that offset as anchorPrefabY so the
 * loader can align prefab coords to world coords correctly.
 *
 * Example for Town Hall: statue at prefabY=2, so anchorPrefabY=2.
 * worldY = prefabY + (foundingStoneWorldY - anchorPrefabY)
 */
public class PrefabLoader {

    private static final Logger LOGGER = Logger.getLogger(PrefabLoader.class.getName());

    private static final Set<String> SKIP_BLOCKS = Set.of(
            "Empty",
            "Editor_Empty",
            "Editor_Anchor",
            "Filter_Air_Block",
            "Hearthbound_Ghost_Cube",
            "Hearthbound_Ghost_Stairs",
            "Hearthbound_Ghost_Roof",
            "Hearthbound_Ghost_Roof_Flat",
            "Hearthbound_Ghost_Roof_Shallow",
            "Hearthbound_Ghost_Roof_Steep",
            "Hearthbound_Ghost_Roof_Hollow",
            "Hearthbound_Ghost_Fence",
            "Hearthbound_Ghost_Half",
            // The player places the Founding Stone themselves; it must never end up in a
            // build plan (otherwise ghost preview would erase it) even if a prefab author
            // left one inside the selection.
            "Hearthbound_Founding_Stone",
            "Soil_Grass",
            "Soil_Dirt",
            "Soil_Ash"
    );

    /**
     * Load a prefab and return blocks in world coordinates, ready for block-by-block placement.
     * Rotation is computed automatically by comparing the anchor block's rotation in the prefab
     * to the worldRotation (rotation of the anchor block as placed in the world).
     *
     * @param prefabName      filename without extension, e.g. "Townhall_lvl1_v1"
     * @param anchorBlockId   block ID used as anchor placeholder in the prefab (will be skipped)
     * @param anchorPrefabY   Y coordinate of the anchor placeholder block inside the prefab
     * @param worldX          world X of the founding stone / anchor
     * @param worldY          world Y of the founding stone / anchor
     * @param worldZ          world Z of the founding stone / anchor
     * @param worldRotation   NESW rotation of the anchor block as placed in the world (0–3)
     * @return list sorted bottom-to-top, empty on failure
     */
    public static List<BlockPlacer.BlockEntry> load(
            String prefabName, String anchorBlockId, int anchorPrefabY,
            int worldX, int worldY, int worldZ, int worldRotation) {
        return load(prefabName, anchorBlockId, anchorPrefabY, worldX, worldY, worldZ, worldRotation, false);
    }

    public static List<BlockPlacer.BlockEntry> load(
            String prefabName, String anchorBlockId, int anchorPrefabY,
            int worldX, int worldY, int worldZ, int worldRotation, boolean mineOrder) {
        try {
            BlockSelection selection = PrefabStore.get().getAssetPrefabFromAnyPack(prefabName + ".prefab.json");
            int prefabRotation = readAnchorRotation(selection, anchorBlockId, anchorPrefabY);
            int rotationSteps = (worldRotation - prefabRotation + 4) % 4;
            // Never include Empty entries in the build plan — terrain clearing is handled
            // separately by startResourceBuilding before the elf begins construction.
            List<BlockPlacer.BlockEntry> blocks = extractBlocks(
                    selection, anchorBlockId, anchorPrefabY, worldX, worldY, worldZ, rotationSteps,
                    false);
            return mineOrder ? sortForMineOrder(blocks, worldY) : blocks;
        } catch (Exception e) {
            LOGGER.warning("Failed to load prefab '" + prefabName + "': " + e.getMessage());
            return List.of();
        }
    }

    /** Overload with no rotation. */
    public static List<BlockPlacer.BlockEntry> load(
            String prefabName, String anchorBlockId, int anchorPrefabY,
            int worldX, int worldY, int worldZ) {
        return load(prefabName, anchorBlockId, anchorPrefabY, worldX, worldY, worldZ, 0, false);
    }

    /**
     * Returns blocks in native local coordinates — relative to the anchor block, no rotation.
     * (lx, ly, lz) = (0, 0, 0) is the anchor position.
     * Used by BuildingLayout to inspect prefab geometry before any world rotation is applied.
     */
    public static List<BlockPlacer.BlockEntry> loadNativeLocal(
            String prefabName, String anchorBlockId, int anchorPrefabY) {
        try {
            BlockSelection selection = PrefabStore.get().getAssetPrefabFromAnyPack(prefabName + ".prefab.json");
            return extractBlocks(selection, anchorBlockId, anchorPrefabY,
                    0, anchorPrefabY, 0, 0);
        } catch (Exception e) {
            LOGGER.warning("Failed to load prefab '" + prefabName + "' (native local): " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Returns world-coordinate positions of Empty cells that are strictly below the anchor
     * Y level (ly < 0). Used by the ghost preview to clear terrain under the building footprint.
     */
    public static List<BlockPlacer.BlockEntry> loadBelowAnchorEmpty(
            String prefabName, String anchorBlockId, int anchorPrefabY,
            int worldX, int worldY, int worldZ, int worldRotation) {
        try {
            BlockSelection selection = PrefabStore.get().getAssetPrefabFromAnyPack(prefabName + ".prefab.json");
            int prefabRotation = readAnchorRotation(selection, anchorBlockId, anchorPrefabY);
            int rotationSteps = (worldRotation - prefabRotation + 4) % 4;
            return extractBelowAnchorEmpty(selection, anchorBlockId, anchorPrefabY,
                    worldX, worldY, worldZ, rotationSteps);
        } catch (Exception e) {
            LOGGER.warning("Failed to load prefab '" + prefabName + "' for below-anchor empty: " + e.getMessage());
            return List.of();
        }
    }

    private static List<BlockPlacer.BlockEntry> extractBelowAnchorEmpty(
            BlockSelection selection, String anchorBlockId, int anchorPrefabY,
            int worldX, int worldY, int worldZ, int rotationSteps) {

        var assetMap = BlockType.getAssetMap();

        int prefabOriginX = selection.getX();
        int prefabOriginY = selection.getY();
        int prefabOriginZ = selection.getZ();

        int[] anchorLocal = findAnchorLocal(selection, anchorBlockId, anchorPrefabY,
                prefabOriginX, prefabOriginY, prefabOriginZ);
        int anchorLX = anchorLocal[0];
        int anchorLZ = anchorLocal[1];

        List<BlockPlacer.BlockEntry> result = new ArrayList<>();

        selection.forEachBlock((bx, by, bz, holder) -> {
            if (holder.filler() != 0) return;

            int ly = (by - prefabOriginY) - anchorPrefabY;
            if (ly >= 0) return;

            BlockType blockType = assetMap.getAsset(holder.blockId());
            String id = blockType != null ? blockType.getId() : "Empty";
            if (!id.equals("Empty") && !id.equals("Editor_Empty")) return;

            int lx = (bx - prefabOriginX) - anchorLX;
            int lz = (bz - prefabOriginZ) - anchorLZ;

            for (int i = 0; i < rotationSteps; i++) {
                int tmp = lx;
                lx = lz;
                lz = -tmp;
            }

            result.add(new BlockPlacer.BlockEntry(worldX + lx, worldY + ly, worldZ + lz, "Empty", 0));
        });

        return result;
    }

    /** Returns the anchor block's local [lx, lz] relative to the selection corner. */
    private static int[] findAnchorLocal(BlockSelection selection, String anchorBlockId, int anchorPrefabY,
                                          int prefabOriginX, int prefabOriginY, int prefabOriginZ) {
        var assetMap = BlockType.getAssetMap();
        int[] found = {0, 0};
        selection.forEachBlock((bx, by, bz, holder) -> {
            if (holder.filler() != 0) return;
            BlockType bt = assetMap.getAsset(holder.blockId());
            if (bt == null) return;
            if (bt.getId().equals(anchorBlockId) && by - prefabOriginY == anchorPrefabY) {
                found[0] = bx - prefabOriginX;
                found[1] = bz - prefabOriginZ;
            }
        });
        return found;
    }

    /**
     * Returns the yaw rotation (0–3) of the anchor block as stored in the prefab.
     * This is the "default orientation" of the prefab. Use it to compute rotation steps:
     *   rotationSteps = (worldRotation - readAnchorRotation(prefabName, anchorId, anchorPrefabY) + 4) % 4
     */
    public static int readAnchorRotation(String prefabName, String anchorBlockId, int anchorPrefabY) {
        try {
            BlockSelection selection = PrefabStore.get().getAssetPrefabFromAnyPack(prefabName + ".prefab.json");
            return readAnchorRotation(selection, anchorBlockId, anchorPrefabY);
        } catch (Exception e) {
            LOGGER.warning("PrefabLoader.readAnchorRotation failed for '" + prefabName + "': " + e.getMessage());
            return 0;
        }
    }

    /** Reads the yaw rotation of the anchor block from the prefab (lowest 2 bits of RotationTuple index). */
    private static int readAnchorRotation(BlockSelection selection, String anchorBlockId, int anchorPrefabY) {
        var assetMap = BlockType.getAssetMap();
        int[] found = {0};
        selection.forEachBlock((bx, by, bz, holder) -> {
            if (found[0] != 0) return;
            if (holder.filler() != 0) return;
            BlockType bt = assetMap.getAsset(holder.blockId());
            if (bt == null) return;
            if (bt.getId().equals(anchorBlockId) && by - selection.getY() == anchorPrefabY) {
                found[0] = holder.rotation() % 4; // yaw = low 2 bits
            }
        });
        return found[0];
    }

    private static List<BlockPlacer.BlockEntry> extractBlocks(
            BlockSelection selection, String anchorBlockId, int anchorPrefabY,
            int worldX, int worldY, int worldZ, int rotationSteps) {
        return extractBlocks(selection, anchorBlockId, anchorPrefabY,
                worldX, worldY, worldZ, rotationSteps, false);
    }

    private static List<BlockPlacer.BlockEntry> extractBlocks(
            BlockSelection selection, String anchorBlockId, int anchorPrefabY,
            int worldX, int worldY, int worldZ, int rotationSteps, boolean includeBelowEmpty) {

        var assetMap = BlockType.getAssetMap();

        int prefabOriginX = selection.getX();
        int prefabOriginY = selection.getY();
        int prefabOriginZ = selection.getZ();

        int[] anchorLocal = findAnchorLocal(selection, anchorBlockId, anchorPrefabY,
                prefabOriginX, prefabOriginY, prefabOriginZ);
        int anchorLX = anchorLocal[0];
        int anchorLZ = anchorLocal[1];

        List<BlockPlacer.BlockEntry> result = new ArrayList<>();

        selection.forEachBlock((bx, by, bz, holder) -> {
            BlockType blockType = assetMap.getAsset(holder.blockId());
            if (blockType == null) return;

            if (holder.filler() != 0) return;

            String id = blockType.getId();

            // Coords relative to the anchor block (not the selection corner).
            int lx = (bx - prefabOriginX) - anchorLX;
            int ly = (by - prefabOriginY) - anchorPrefabY;
            int lz = (bz - prefabOriginZ) - anchorLZ;

            // For mine mode: include Empty cells below the anchor as explicit clear operations.
            if ((id.equals("Empty") || id.equals("Editor_Empty")) && includeBelowEmpty && ly < 0) {
                for (int i = 0; i < rotationSteps; i++) { int t = lx; lx = lz; lz = -t; }
                result.add(new BlockPlacer.BlockEntry(worldX + lx, worldY + ly, worldZ + lz, "Empty", 0));
                return;
            }

            if (SKIP_BLOCKS.contains(id)) return;
            if (id.equals(anchorBlockId)) return;

            // Below the floor (ly < -1): skip background fill blocks that already exist in
            // the terrain and would never be visible — placing them is pure waste.
            if (ly < -1 && isMineBackfill(id)) return;

            // Rotate local XZ around anchor (0,0) by rotationSteps * 90° CCW: (x,z) → (z, -x).
            // Direction paired with the CCW yaw rotation in rotateBlockRotation — keeping the
            // two handedness-consistent preserves relative geometry across all 4 placements.
            for (int i = 0; i < rotationSteps; i++) {
                int tmp = lx;
                lx = lz;
                lz = -tmp;
            }

            int wx = worldX + lx;
            int wy = worldY + ly;
            int wz = worldZ + lz;

            // Block rotation: each step adds 1 to the yaw (NESW index wraps at 4)
            // Only rotate the yaw component (low 2 bits in most RotationTuple layouts)
            int blockRotation = rotateBlockRotation(holder.rotation(), rotationSteps);

            result.add(new BlockPlacer.BlockEntry(wx, wy, wz, id, blockRotation));
        });

        return sortForBuildOrder(result);
    }

    /**
     * Mine-specific build order:
     *   1. Everything at or above the anchor Y (ly >= 0) — bottom-to-top (normal build order).
     *   2. Everything below the anchor Y (ly < 0) — top-to-bottom (digging downward).
     *
     * Within each group the standard shell/furniture and radial sort still apply.
     */
    private static List<BlockPlacer.BlockEntry> sortForMineOrder(
            List<BlockPlacer.BlockEntry> blocks, int anchorWorldY) {
        List<BlockPlacer.BlockEntry> above = new java.util.ArrayList<>();
        List<BlockPlacer.BlockEntry> below = new java.util.ArrayList<>();
        for (BlockPlacer.BlockEntry e : blocks) {
            if (e.y() >= anchorWorldY - 1) above.add(e);
            else below.add(e);
        }
        // Above: normal bottom-to-top (sortForBuildOrder already does this)
        List<BlockPlacer.BlockEntry> sortedAbove = sortForBuildOrder(above);
        // Below: top-to-bottom — negate y for the sort key, then restore
        below.sort((a, b) -> {
            int c = Integer.compare(b.y(), a.y()); // descending y = top-to-bottom
            if (c != 0) return c;
            return Integer.compare(buildPass(a.blockType()), buildPass(b.blockType()));
        });

        List<BlockPlacer.BlockEntry> result = new java.util.ArrayList<>(blocks.size());
        result.addAll(sortedAbove);
        result.addAll(below);
        return result;
    }

    /**
     * Orders blocks so the NPC builds the shell first, then returns for furniture.
     *
     * <p>Two passes bottom-to-top:
     * <ol>
     *   <li>Structural shell: rocks → wood/planks → other structure. Within a Y layer,
     *       blocks are grouped by material category and placed together, which reads as
     *       "mason lays the stone course, then the carpenter lays the wooden course".</li>
     *   <li>Furniture and decor.</li>
     * </ol>
     *
     * <p>Inside each (pass, y, category, blockType) bucket, plain blocks come before
     * corner variants (so the adjacent walls exist when the corner tries to orient itself),
     * and positions walk around the group's centroid in a clockwise sweep instead of
     * scanning rows by X — it reads as the builder pacing around the room.
     */
    private static List<BlockPlacer.BlockEntry> sortForBuildOrder(List<BlockPlacer.BlockEntry> blocks) {
        // Primary grouping: (pass, y, category, cornerFlag, blockType). These buckets must
        // stay in this order — radial sort only rearranges entries WITHIN a bucket.
        java.util.Map<BucketKey, List<BlockPlacer.BlockEntry>> buckets = new java.util.LinkedHashMap<>();
        for (BlockPlacer.BlockEntry e : blocks) {
            BucketKey key = new BucketKey(
                    buildPass(e.blockType()),
                    e.y(),
                    shellCategory(e.blockType()),
                    isCornerVariant(e.blockType()) ? 1 : 0,
                    e.blockType());
            buckets.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(e);
        }

        // Deterministic bucket order: pass → y → category → corner-flag → blockType.
        List<BucketKey> keys = new java.util.ArrayList<>(buckets.keySet());
        keys.sort(BucketKey::compareTo);

        List<BlockPlacer.BlockEntry> out = new java.util.ArrayList<>(blocks.size());
        for (BucketKey key : keys) {
            List<BlockPlacer.BlockEntry> bucket = buckets.get(key);
            radialSort(bucket);
            out.addAll(bucket);
        }
        return out;
    }

    /**
     * Sorts blocks in-place by angle around their centroid, then by radius.
     * The result is a CW sweep that reads as "builder walks around the perimeter" rather
     * than the default row-by-row scan that snaps side to side across the anchor.
     */
    private static void radialSort(List<BlockPlacer.BlockEntry> bucket) {
        if (bucket.size() < 2) return;
        double sumX = 0, sumZ = 0;
        for (BlockPlacer.BlockEntry e : bucket) {
            sumX += e.x();
            sumZ += e.z();
        }
        final double cx = sumX / bucket.size();
        final double cz = sumZ / bucket.size();
        bucket.sort((a, b) -> {
            // atan2 with (dx, dz) argument order gives a CCW sweep from +X. Negate so that
            // earlier entries are on the +Z (south) side and the sweep goes CW when viewed
            // from above — matches our CW XZ rotation convention.
            double angleA = Math.atan2(a.x() - cx, a.z() - cz);
            double angleB = Math.atan2(b.x() - cx, b.z() - cz);
            int cmp = Double.compare(angleA, angleB);
            if (cmp != 0) return cmp;
            // Tie-break: closer to centroid first, then deterministic x/z.
            double rA = Math.hypot(a.x() - cx, a.z() - cz);
            double rB = Math.hypot(b.x() - cx, b.z() - cz);
            cmp = Double.compare(rA, rB);
            if (cmp != 0) return cmp;
            if (a.x() != b.x()) return Integer.compare(a.x(), b.x());
            return Integer.compare(a.z(), b.z());
        });
    }

    private record BucketKey(int pass, int y, int category, int cornerFlag, String blockType)
            implements Comparable<BucketKey> {
        @Override
        public int compareTo(BucketKey o) {
            int c = Integer.compare(pass, o.pass); if (c != 0) return c;
            c = Integer.compare(y, o.y); if (c != 0) return c;
            c = Integer.compare(category, o.category); if (c != 0) return c;
            c = Integer.compare(cornerFlag, o.cornerFlag); if (c != 0) return c;
            return blockType.compareTo(o.blockType);
        }
    }

    /** Background fill blocks that exist in natural terrain and need not be placed by the elf. */
    private static boolean isMineBackfill(String id) {
        return "Rock_Stone".equals(id)
                || id.startsWith("Ore_")
                || id.startsWith("Rubble_");
    }

    /** True for connected-block state variants named *..._State_Definitions_Corner, etc. */
    private static boolean isCornerVariant(String blockId) {
        return blockId.contains("_State_Definitions_Corner");
    }

    /** 0 = structural shell pass, 1 = furniture/decor pass. */
    private static int buildPass(String blockId) {
        String stripped = blockId.startsWith("*") ? blockId.substring(1) : blockId;
        if (stripped.contains("Door")) return 0; // doors are part of the shell
        if (stripped.equals("Furniture_Village_Planter")) return 0; // exception: structural planter
        if (stripped.startsWith("Furniture_")) return 1;
        if (stripped.startsWith("Deco_")) return 1;
        if (stripped.startsWith("Bench_")) return 1;
        if (stripped.startsWith("Container_")) return 1;
        if (stripped.startsWith("Plant_")) return 1;
        if (stripped.startsWith("Ingredient_")) return 1;
        return 0;
    }

    /** Within the shell pass, lower category = placed earlier within the same Y layer. */
    private static int shellCategory(String blockId) {
        String stripped = blockId.startsWith("*") ? blockId.substring(1) : blockId;
        // "Dependent" shell blocks lean on or connect to their neighbors (stairs, slabs,
        // beams, fences, and Rock_*_Wall which Hytale models as a low fence-like piece).
        // Placing them last in the Y-layer ensures their supports exist first, so the
        // connected-blocks pass can orient them correctly instead of leaving them floating.
        // Note: Wood_*_Wall is a full block, not a fence — so it stays in the Wood category.
        boolean isDependent = stripped.contains("Stairs")
                || stripped.contains("Slab")
                || stripped.contains("Beam")
                || stripped.startsWith("Wood_") && stripped.contains("Fence")
                || stripped.startsWith("Rock_") && stripped.contains("Wall")
                || stripped.contains("_Branch")
                || stripped.contains("_Corner");
        if (isDependent) return 3;
        if (stripped.startsWith("Rock_")) return 0;
        if (stripped.startsWith("Wood_")) return 1;
        if (stripped.startsWith("Cloth_")) return 2;
        if (stripped.startsWith("Plant_")) return 4;
        if (stripped.startsWith("Soil_")) return 4;
        // Doors, windows, fence gates and anything else we consider structural
        return 5;
    }

    /**
     * Rotates a RotationTuple index around the vertical axis by {@code steps} quarter-turns.
     *
     * <p>RotationTuple index = {@code roll*16 + pitch*4 + yaw} (each 0–3). We only touch yaw,
     * so tilted blocks (inverted stairs, upside-down slabs) keep their tilt.
     *
     * <p>Empirically the game's yaw encoding runs opposite to its XZ rotation direction: door
     * positions come out right with {@code (x,z)→(z,-x)} CCW on XZ, but block facings come
     * out right with {@code yaw + steps} (CW). Even-step placements (0°, 180°) don't care
     * about the sign; the mismatch is only visible on odd steps (90°, 270°) where one pair
     * of directions mirrors the other. Keep this formula unless both position and yaw tests
     * agree on a new handedness.
     */
    static int rotateBlockRotation(int rotationIndex, int steps) {
        if (steps == 0) return rotationIndex;
        int N = 4;
        int roll  = rotationIndex / (N * N);
        int pitch = (rotationIndex % (N * N)) / N;
        int yaw   = rotationIndex % N;
        yaw = (yaw + steps) % N;
        return roll * N * N + pitch * N + yaw;
    }
}
