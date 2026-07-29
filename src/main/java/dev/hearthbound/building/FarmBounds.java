package dev.hearthbound.building;

import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.BuildingType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Computes the world-space bounding box that covers the planting area of a farm building.
 *
 * The bbox is derived from the prefab geometry:
 *   - find native-local (lx, ly, lz) of every Soil_Dirt_Tilled and Plant_Crop_* block,
 *   - take min/max in each axis,
 *   - rotate the corners by the farm's world rotation,
 *   - translate to the anchor's world position.
 *
 * Min/max layout per type is cached — same prefab → same bounds, only the world translation
 * changes per BuildingRecord.
 */
public final class FarmBounds {

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("build.layout");
    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
        }
        public int width()  { return maxX - minX + 1; }
        public int height() { return maxY - minY + 1; }
        public int depth()  { return maxZ - minZ + 1; }
    }

    /** Cached native-local bounds per building type+variant key. */
    private record LocalBounds(int minLX, int minLY, int minLZ, int maxLX, int maxLY, int maxLZ) {}
    private static final Map<String, LocalBounds> LOCAL_CACHE = new HashMap<>();

    private FarmBounds() {}

    /**
     * Returns the world-space bbox of the farm's planting area. Returns null if the
     * building isn't a farm or the prefab has no recognizable plot blocks.
     *
     * The Y range covers crop blocks (Plant_Crop_*) plus filler cells above them — high
     * crops like corn occupy two stacked cells.
     */
    public static Bounds compute(BuildingRecord farm) {
        if (farm == null) return null;
        if (!BuildingType.FARM.equals(farm.getType())) return null;

        LocalBounds local = getLocalBounds(farm.getType(), farm.getVariant());
        if (local == null) return null;

        BuildingLayout.Layout layout = BuildingLayout.get(farm.getType(), farm.getVariant());
        int steps = layout.rotationSteps(farm.getRotation());

        // Rotate all 4 horizontal corners of the local bbox; vertical extent doesn't rotate.
        int[][] corners = {
            BuildingLayout.rotateLocalOffset(local.minLX, 0, local.minLZ, steps),
            BuildingLayout.rotateLocalOffset(local.minLX, 0, local.maxLZ, steps),
            BuildingLayout.rotateLocalOffset(local.maxLX, 0, local.minLZ, steps),
            BuildingLayout.rotateLocalOffset(local.maxLX, 0, local.maxLZ, steps),
        };

        int rMinX = corners[0][0], rMaxX = corners[0][0];
        int rMinZ = corners[0][2], rMaxZ = corners[0][2];
        for (int[] c : corners) {
            if (c[0] < rMinX) rMinX = c[0];
            if (c[0] > rMaxX) rMaxX = c[0];
            if (c[2] < rMinZ) rMinZ = c[2];
            if (c[2] > rMaxZ) rMaxZ = c[2];
        }

        int worldX = farm.getPosX();
        int worldY = farm.getPosY();
        int worldZ = farm.getPosZ();

        return new Bounds(
            worldX + rMinX, worldY + local.minLY, worldZ + rMinZ,
            worldX + rMaxX, worldY + local.maxLY, worldZ + rMaxZ
        );
    }

    private static LocalBounds getLocalBounds(String type, int variant) {
        String key = type + ":" + variant;
        return LOCAL_CACHE.computeIfAbsent(key, k -> computeLocal(type, variant));
    }

    private static LocalBounds computeLocal(String type, int variant) {
        String prefabName = BuildingType.getPrefabName(type, variant);
        String anchorId = BuildingType.getAnchorBlockId(type);
        int anchorPrefabY = BuildingType.getAnchorPrefabY(type, variant);

        if (prefabName == null) return null;

        List<BlockPlacer.BlockEntry> blocks =
                PrefabLoader.loadNativeLocal(prefabName, anchorId, anchorPrefabY);
        if (blocks.isEmpty()) {
            LOG.warn("FarmBounds: no prefab blocks for type=" + type + " variant=" + variant);
            return null;
        }

        boolean any = false;
        int minLX = Integer.MAX_VALUE, maxLX = Integer.MIN_VALUE;
        int minLY = Integer.MAX_VALUE, maxLY = Integer.MIN_VALUE;
        int minLZ = Integer.MAX_VALUE, maxLZ = Integer.MIN_VALUE;

        for (BlockPlacer.BlockEntry b : blocks) {
            String id = stripStar(b.blockType());
            boolean isPlot = id.startsWith("Soil_Dirt_Tilled")
                          || id.startsWith("Plant_Crop_");
            if (!isPlot) continue;

            // loadNativeLocal returns y = anchorPrefabY + ly, convert to true ly.
            int lx = b.x();
            int ly = b.y() - anchorPrefabY;
            int lz = b.z();

            if (lx < minLX) minLX = lx;
            if (lx > maxLX) maxLX = lx;
            if (ly < minLY) minLY = ly;
            if (ly > maxLY) maxLY = ly;
            if (lz < minLZ) minLZ = lz;
            if (lz > maxLZ) maxLZ = lz;
            any = true;
        }

        if (!any) {
            LOG.warn("FarmBounds: no Soil_Dirt_Tilled / Plant_Crop_* blocks in prefab " + prefabName);
            return null;
        }

        // Extend Y up by 2 to cover tall crops (corn, wheat StageFinal — root + filler stack).
        maxLY += 2;

        LOG.info("FarmBounds local for " + type + ": x[" + minLX + ".." + maxLX + "]"
                + " y[" + minLY + ".." + maxLY + "] z[" + minLZ + ".." + maxLZ + "]");
        return new LocalBounds(minLX, minLY, minLZ, maxLX, maxLY, maxLZ);
    }

    private static String stripStar(String id) {
        if (id == null) return "";
        return id.startsWith("*") ? id.substring(1) : id;
    }
}
