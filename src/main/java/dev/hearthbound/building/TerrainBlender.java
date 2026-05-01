package dev.hearthbound.building;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

import java.util.logging.Logger;

/**
 * Shared terrain-editing utilities used by both RescueQuestManager (quest prefab sites)
 * and ElfSage (wanderer tent site).
 *
 * <p>All public methods take a World, a center (centerX, groundY, centerZ), and an
 * {@code ext} footprint array of the form {@code [minX, maxX, minZ, maxZ, maxPrefabY]}
 * where offsets are relative to the anchor/center block.</p>
 */
public final class TerrainBlender {

    private static final Logger LOGGER = Logger.getLogger(TerrainBlender.class.getName());

    private TerrainBlender() {}

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /** Total blend radius in blocks outside the footprint. */
    public static final int BLEND_RADIUS = 8;

    /** Maximum Branch/Leaves hops from a grounded trunk before a block is considered floating. */
    private static final int MAX_LEAF_HANG = 10;

    /** Cell size for terrain blend noise (bilinear interpolation grid). */
    private static final int NOISE_CELL = 6;

    private static final String[] GRASS_DECOR = {
        "Plant_Grass_Sharp", "Plant_Grass_Sharp_Short", "Plant_Grass_Sharp_Tall"
    };

    // -------------------------------------------------------------------------
    // Main entry points
    // -------------------------------------------------------------------------

    /**
     * Flood-fill removes all vegetation connected to the footprint interior,
     * including tree crowns floating above the prefab and partial trunks
     * outside its edges.
     *
     * <p>Seed: every vegetation block at groundY+1 inside the footprint + BLEND_RADIUS.
     * Spread: 6-connected neighbours within [centerX±(halfX+60), groundY..groundY+60,
     * centerZ±(halfZ+60)]. This removes entire trees whose trunks overlap the footprint,
     * including their overhanging crowns.
     *
     * @param ext [minX, maxX, minZ, maxZ, maxPrefabY] — offsets from anchor
     */
    public static void clearVegetation(World world, int centerX, int groundY, int centerZ,
            int[] ext) {
        int minX = centerX + ext[0] - 60;
        int maxX = centerX + ext[1] + 60;
        int minY = groundY;
        int maxY = groundY + 60;
        int minZ = centerZ + ext[2] - 60;
        int maxZ = centerZ + ext[3] + 60;

        java.util.Set<Long> visited = new java.util.HashSet<>();
        java.util.ArrayDeque<long[]> queue = new java.util.ArrayDeque<>();

        int seedMargin = BLEND_RADIUS;
        for (int dx = ext[0] - seedMargin; dx <= ext[1] + seedMargin; dx++) {
            for (int dz = ext[2] - seedMargin; dz <= ext[3] + seedMargin; dz++) {
                int wx = centerX + dx;
                int wz = centerZ + dz;
                for (int wy = groundY + 1; wy <= groundY + 3; wy++) {
                    var bt = world.getBlockType(wx, wy, wz);
                    if (bt == null) continue;
                    String id = bt.getId();
                    if (id != null && isVegetation(id)) {
                        long key = packXYZ(wx, wy, wz);
                        if (visited.add(key)) queue.add(new long[]{wx, wy, wz});
                    }
                }
            }
        }

        int[] dx6 = {1, -1, 0, 0, 0, 0};
        int[] dy6 = {0, 0, 1, -1, 0, 0};
        int[] dz6 = {0, 0, 0, 0, 1, -1};

        while (!queue.isEmpty()) {
            long[] pos = queue.poll();
            int x = (int) pos[0], y = (int) pos[1], z = (int) pos[2];
            world.breakBlock(x, y, z, 0);
            for (int d = 0; d < 6; d++) {
                int nx = x + dx6[d], ny = y + dy6[d], nz = z + dz6[d];
                if (nx < minX || nx > maxX || ny < minY || ny > maxY || nz < minZ || nz > maxZ) continue;
                long key = packXYZ(nx, ny, nz);
                if (!visited.add(key)) continue;
                var bt = world.getBlockType(nx, ny, nz);
                if (bt == null) continue;
                String id = bt.getId();
                if (id != null && isVegetation(id)) queue.add(new long[]{nx, ny, nz});
            }
        }
    }

    /**
     * Removes floating vegetation fragments left after blendTerrain lowered terrain.
     * Inside the footprint + blend zone, removes stray terrain blocks above the prefab roof.
     * Outside the footprint, uses Dijkstra-BFS to find disconnected tree crowns and removes them.
     *
     * @param ext [minX, maxX, minZ, maxZ, maxPrefabY]
     */
    public static void clearFloating(World world, int centerX, int groundY, int centerZ,
            int[] ext) {
        int r = BLEND_RADIUS;
        int roofWorldY = groundY + ext[4];

        // Inside footprint + blend zone: bottom-up stray-block removal above roof
        for (int ox = ext[0] - r; ox <= ext[1] + r; ox++) {
            for (int oz = ext[2] - r; oz <= ext[3] + r; oz++) {
                int wx = centerX + ox, wz = centerZ + oz;
                for (int wy = roofWorldY; wy <= roofWorldY + 20; wy++) {
                    var bt = world.getBlockType(wx, wy, wz);
                    if (bt == null) continue;
                    String id = bt.getId();
                    if (id == null || id.startsWith("Empty") || id.startsWith("Air") || id.startsWith("Filter_")) continue;
                    var below = world.getBlockType(wx, wy - 1, wz);
                    String belowId = below != null ? below.getId() : null;
                    boolean belowAir = belowId == null
                            || belowId.startsWith("Empty") || belowId.startsWith("Air") || belowId.startsWith("Filter_");
                    if (belowAir) world.breakBlock(wx, wy, wz, 0);
                }
            }
        }

        // Outside footprint: Dijkstra-BFS grounded-component check
        int delMinX = centerX + ext[0] - r, delMaxX = centerX + ext[1] + r;
        int delMinZ = centerZ + ext[2] - r, delMaxZ = centerZ + ext[3] + r;
        int extra = 12;
        int colMinX = delMinX - extra, colMaxX = delMaxX + extra;
        int colMinZ = delMinZ - extra, colMaxZ = delMaxZ + extra;
        int scanMinY = groundY - 8;
        int scanMaxY = groundY + 40;

        java.util.HashMap<Long, String> candidates = new java.util.HashMap<>();
        for (int wx = colMinX; wx <= colMaxX; wx++) {
            for (int wz = colMinZ; wz <= colMaxZ; wz++) {
                int ox = wx - centerX, oz = wz - centerZ;
                if (ox >= ext[0] && ox <= ext[1] && oz >= ext[2] && oz <= ext[3]) continue;
                for (int wy = scanMinY; wy <= scanMaxY; wy++) {
                    var bt = world.getBlockType(wx, wy, wz);
                    if (bt == null) continue;
                    String id = bt.getId();
                    if (id != null && isVegetation(id)) candidates.put(packXYZ(wx, wy, wz), id);
                }
            }
        }

        java.util.HashMap<Long, Integer> bestDist = new java.util.HashMap<>();
        java.util.PriorityQueue<int[]> pq = new java.util.PriorityQueue<>(
                java.util.Comparator.comparingInt(a -> a[0]));

        for (long key : candidates.keySet()) {
            int[] xyz = unpackXYZ(key);
            var below = world.getBlockType(xyz[0], xyz[1] - 1, xyz[2]);
            String belowId = below != null ? below.getId() : null;
            if (belowId != null && (belowId.startsWith("Soil_") || belowId.startsWith("Rock_"))) {
                bestDist.put(key, 0);
                pq.offer(new int[]{0, xyz[0], xyz[1], xyz[2]});
            }
        }

        int[] dx6 = {1, -1, 0, 0, 0, 0};
        int[] dy6 = {0, 0, 1, -1, 0, 0};
        int[] dz6 = {0, 0, 0, 0, 1, -1};

        while (!pq.isEmpty()) {
            int[] cur = pq.poll();
            int dist = cur[0], wx = cur[1], wy = cur[2], wz = cur[3];
            long key = packXYZ(wx, wy, wz);
            Integer recorded = bestDist.get(key);
            if (recorded != null && dist > recorded) continue;
            for (int d = 0; d < 6; d++) {
                int nx = wx + dx6[d], ny = wy + dy6[d], nz = wz + dz6[d];
                long nkey = packXYZ(nx, ny, nz);
                String nid = candidates.get(nkey);
                if (nid == null) continue;
                int step = isTrunkOrLog(nid) ? 0 : 1;
                int newDist = dist + step;
                if (newDist > MAX_LEAF_HANG) continue;
                Integer prev = bestDist.get(nkey);
                if (prev == null || newDist < prev) {
                    bestDist.put(nkey, newDist);
                    pq.offer(new int[]{newDist, nx, ny, nz});
                }
            }
        }

        for (long key : candidates.keySet()) {
            if (bestDist.containsKey(key)) continue;
            int[] xyz = unpackXYZ(key);
            int wx = xyz[0], wy = xyz[1], wz = xyz[2];
            if (wx < delMinX || wx > delMaxX || wz < delMinZ || wz > delMaxZ) continue;
            world.breakBlock(wx, wy, wz, 0);
        }

        // Second pass: orphan crowns beyond the collectZone
        int ext2 = 32;
        int ext2MinX = delMinX - ext2, ext2MaxX = delMaxX + ext2;
        int ext2MinZ = delMinZ - ext2, ext2MaxZ = delMaxZ + ext2;

        java.util.HashMap<Long, String> ext2Candidates = new java.util.HashMap<>();
        for (int wx = ext2MinX; wx <= ext2MaxX; wx++) {
            for (int wz = ext2MinZ; wz <= ext2MaxZ; wz++) {
                if (wx >= delMinX && wx <= delMaxX && wz >= delMinZ && wz <= delMaxZ) continue;
                for (int wy = scanMinY; wy <= scanMaxY; wy++) {
                    var bt = world.getBlockType(wx, wy, wz);
                    if (bt == null) continue;
                    String id = bt.getId();
                    if (id != null && isVegetation(id)) ext2Candidates.put(packXYZ(wx, wy, wz), id);
                }
            }
        }

        java.util.HashMap<Long, Integer> ext2Dist = new java.util.HashMap<>();
        java.util.PriorityQueue<int[]> ext2Pq = new java.util.PriorityQueue<>(
                java.util.Comparator.comparingInt(a -> a[0]));
        for (long key : ext2Candidates.keySet()) {
            int[] xyz = unpackXYZ(key);
            var below = world.getBlockType(xyz[0], xyz[1] - 1, xyz[2]);
            String belowId = below != null ? below.getId() : null;
            if (belowId != null && (belowId.startsWith("Soil_") || belowId.startsWith("Rock_"))) {
                ext2Dist.put(key, 0);
                ext2Pq.offer(new int[]{0, xyz[0], xyz[1], xyz[2]});
            }
        }

        while (!ext2Pq.isEmpty()) {
            int[] cur = ext2Pq.poll();
            int dist = cur[0], wx = cur[1], wy = cur[2], wz = cur[3];
            long key = packXYZ(wx, wy, wz);
            Integer recorded = ext2Dist.get(key);
            if (recorded != null && dist > recorded) continue;
            for (int d = 0; d < 6; d++) {
                int nx = wx + dx6[d], ny = wy + dy6[d], nz = wz + dz6[d];
                if (nx >= delMinX && nx <= delMaxX && nz >= delMinZ && nz <= delMaxZ) continue;
                long nkey = packXYZ(nx, ny, nz);
                String nid = ext2Candidates.get(nkey);
                if (nid == null) continue;
                int step = isTrunkOrLog(nid) ? 0 : 1;
                int newDist = dist + step;
                if (newDist > MAX_LEAF_HANG) continue;
                Integer prev = ext2Dist.get(nkey);
                if (prev == null || newDist < prev) {
                    ext2Dist.put(nkey, newDist);
                    ext2Pq.offer(new int[]{newDist, nx, ny, nz});
                }
            }
        }

        for (long key : ext2Candidates.keySet()) {
            if (ext2Dist.containsKey(key)) continue;
            int[] xyz = unpackXYZ(key);
            world.breakBlock(xyz[0], xyz[1], xyz[2], 0);
        }
    }

    /**
     * Blends terrain around the prefab footprint using a signed-distance-field approach.
     * Smoothly transitions from groundY (at the footprint edge) to the natural terrain height
     * (at BLEND_RADIUS blocks out). Corner concavities get a quarter-circle carve to avoid
     * sharp rectangular steps.
     *
     * @param ext [minX, maxX, minZ, maxZ, maxPrefabY]
     */
    public static void blendTerrain(World world, int centerX, int groundY, int centerZ,
            int[] ext) {
        int r = BLEND_RADIUS;
        int snapMinX = ext[0] - r;
        int snapMaxX = ext[1] + r;
        int snapMinZ = ext[2] - r;
        int snapMaxZ = ext[3] + r;
        int snapshotW = snapMaxX - snapMinX + 1;
        int snapshotH = snapMaxZ - snapMinZ + 1;
        int[] snapshot = new int[snapshotW * snapshotH];

        for (int ox = snapMinX; ox <= snapMaxX; ox++) {
            for (int oz = snapMinZ; oz <= snapMaxZ; oz++) {
                int wx = centerX + ox;
                int wz = centerZ + oz;
                var chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(wx, wz));
                int topY = chunk != null ? chunk.getHeight(wx & 31, wz & 31) : groundY;
                int naturalY = solidGroundAt(world, wx, topY, wz);
                snapshot[(oz - snapMinZ) * snapshotW + (ox - snapMinX)] = (naturalY == -1) ? groundY : naturalY;
            }
        }

        // Outside SDF blend
        for (int ox = snapMinX; ox <= snapMaxX; ox++) {
            for (int oz = snapMinZ; oz <= snapMaxZ; oz++) {
                double ddx = ox < ext[0] ? ext[0] - ox : (ox > ext[1] ? ox - ext[1] : 0);
                double ddz = oz < ext[2] ? ext[2] - oz : (oz > ext[3] ? oz - ext[3] : 0);
                double dist = Math.sqrt(ddx * ddx + ddz * ddz);
                if (dist <= 0 || dist > r) continue;

                int wx = centerX + ox;
                int wz = centerZ + oz;
                int naturalY = snapshot[(oz - snapMinZ) * snapshotW + (ox - snapMinX)];
                if (naturalY == groundY) continue;
                if (groundY - naturalY > 5) continue;

                double noise = blendNoise(wx, wz) * (dist / r);
                int targetY = (int) Math.round(groundY + (naturalY - groundY) * dist / r + noise);
                if (targetY == naturalY) continue;

                if (targetY > naturalY) {
                    for (int fy = naturalY + 1; fy < targetY; fy++) {
                        world.setBlock(wx, fy, wz, "Soil_Dirt");
                    }
                    world.setBlock(wx, targetY, wz, "Soil_Grass");
                } else {
                    for (int wy = naturalY + 3; wy > targetY; wy--) {
                        world.breakBlock(wx, wy, wz, 0);
                    }
                    var top = world.getBlockType(wx, targetY, wz);
                    if (top != null) {
                        String topId = top.getId();
                        if (topId != null && shouldReplaceWithGrass(topId)) {
                            world.setBlock(wx, targetY, wz, "Soil_Grass");
                        }
                    }
                }
            }
        }

        // Inside corners: quarter-circle carve
        int[][] corners = {{ext[0], ext[2]}, {ext[0], ext[3]}, {ext[1], ext[2]}, {ext[1], ext[3]}};
        int[][] cornerDirs = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        for (int ci = 0; ci < 4; ci++) {
            int cornerX = centerX + corners[ci][0];
            int cornerZ = centerZ + corners[ci][1];
            int[] cd = cornerDirs[ci];

            int outsideTotal = 0, outsideCount = 0;
            for (int d = 1; d <= 3; d++) {
                int sx = cornerX + cd[0] * d;
                int sz = cornerZ + cd[1] * d;
                int oy = solidGroundByBlockScan(world, sx, groundY, sz);
                if (oy != -1) { outsideTotal += oy; outsideCount++; }
            }
            if (outsideCount == 0) continue;
            int outsideY = (int) Math.round((double) outsideTotal / outsideCount);
            if (outsideY >= groundY) continue;

            for (int i = 0; i <= r; i++) {
                for (int j = 0; j <= r; j++) {
                    double dist = Math.sqrt((double) i * i + j * j);
                    if (dist > r) continue;

                    int wx = cornerX - cd[0] * i;
                    int wz = cornerZ - cd[1] * j;

                    var surfBlock = world.getBlockType(wx, groundY, wz);
                    if (surfBlock == null) continue;
                    String surfId = surfBlock.getId();
                    if (surfId == null || !surfId.startsWith("Soil_")) continue;

                    int targetY = (int) Math.round(outsideY + (groundY - outsideY) * dist / r);
                    if (targetY >= groundY) continue;
                    for (int wy = groundY; wy > targetY; wy--) {
                        world.breakBlock(wx, wy, wz, 0);
                    }
                    var top = world.getBlockType(wx, targetY, wz);
                    if (top != null) {
                        String topId = top.getId();
                        if (topId != null && shouldReplaceWithGrass(topId)) {
                            world.setBlock(wx, targetY, wz, "Soil_Grass");
                        }
                    }
                }
            }
        }
    }

    /**
     * After blendTerrain+clearFloating, scans the blend zone outside the footprint and places
     * grass plants on bare Soil_Grass columns where the cell above is empty.
     *
     * @param ext [minX, maxX, minZ, maxZ, maxPrefabY]
     */
    public static void restoreGrassDecor(World world, int centerX, int groundY, int centerZ,
            int[] ext) {
        int r = BLEND_RADIUS;
        for (int ox = ext[0] - r; ox <= ext[1] + r; ox++) {
            for (int oz = ext[2] - r; oz <= ext[3] + r; oz++) {
                if (ox >= ext[0] && ox <= ext[1] && oz >= ext[2] && oz <= ext[3]) continue;
                int wx = centerX + ox;
                int wz = centerZ + oz;
                int surfY = solidGroundByBlockScan(world, wx, groundY, wz);
                if (surfY == -1) continue;
                var surf = world.getBlockType(wx, surfY, wz);
                if (surf == null) continue;
                String surfId = surf.getId();
                if (surfId == null || !surfId.startsWith("Soil_Grass")) continue;
                maybeSpawnGrassDecor(world, wx, surfY + 1, wz);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Solid ground helpers (public — used by ElfSage, RescueQuestManager)
    // -------------------------------------------------------------------------

    /**
     * Walks down from topY+3 to find solid ground (Soil_*, Rock_*, Gravel*, Sand*),
     * skipping vegetation and tree roots.
     *
     * Uses chunk.getFluidId() to detect fluid cells — more reliable than getBlockType()
     * for underwater columns where the height map points below the water surface.
     * Returns -1 if any fluid is present. Returns topY if no ground found within 30 blocks.
     */
    public static int solidGroundAt(World world, int x, int topY, int z) {
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
        for (int y = topY + 3; y >= topY - 30; y--) {
            if (chunk != null) {
                try {
                    int fluidId = chunk.getFluidId(x, y, z);
                    if (fluidId != Integer.MIN_VALUE && fluidId != 0) return -1;
                } catch (Throwable ignored) {}
            }
            var bt = world.getBlockType(x, y, z);
            if (bt == null) continue;
            String id = bt.getId();
            if (id == null) continue;
            if (id.startsWith("Soil_") || id.startsWith("Rock_")
                    || id.startsWith("Gravel") || id.startsWith("Sand")) return y;
        }
        return topY;
    }

    /**
     * Scans via getBlockType (bypasses stale getHeight cache).
     * Returns the topmost Soil/Rock/Gravel/Sand block near groundY, or -1 if not found.
     */
    public static int solidGroundByBlockScan(World world, int x, int groundY, int z) {
        for (int y = groundY + 4; y >= groundY - 4; y--) {
            var bt = world.getBlockType(x, y, z);
            if (bt == null) continue;
            String id = bt.getId();
            if (id == null) continue;
            if (id.startsWith("Soil_") || id.startsWith("Rock_")
                    || id.startsWith("Gravel") || id.startsWith("Sand")) return y;
        }
        return -1;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static long packXYZ(int x, int y, int z) {
        return ((long) (x + 33_554_432) << 38) | ((long) (z + 33_554_432) << 12) | (y & 0xFFF);
    }

    private static int[] unpackXYZ(long key) {
        int x = (int) ((key >> 38) - 33_554_432);
        int z = (int) (((key >> 12) & 0x3FFFFFFL) - 33_554_432);
        int y = (int) (key & 0xFFF);
        return new int[]{x, y, z};
    }

    private static boolean isVegetation(String id) {
        return id.startsWith("Plant_")
                || id.startsWith("Deco_Nest")
                || (id.startsWith("Wood_") && (id.contains("_Trunk") || id.contains("_Leaves")
                || id.contains("_Branch") || id.contains("_Log") || id.contains("_Beam")
                || id.contains("_Roots")))
                || id.startsWith("Filter_");
    }

    private static boolean isTrunkOrLog(String id) {
        return id.contains("_Trunk") || id.contains("_Log") || id.contains("_Beam");
    }

    private static boolean shouldReplaceWithGrass(String blockId) {
        if (blockId.equals("Soil_Dirt")) return true;
        if (blockId.equals("Rock_Chalk") || blockId.equals("Rock_Marble")) return false;
        if (blockId.equals("Soil_Clay_White")) return false;
        if (blockId.startsWith("Rock_")) return true;
        return false;
    }

    private static double blendNoise(int wx, int wz) {
        int gx = Math.floorDiv(wx, NOISE_CELL);
        int gz = Math.floorDiv(wz, NOISE_CELL);
        double fx = (wx - gx * NOISE_CELL) / (double) NOISE_CELL;
        double fz = (wz - gz * NOISE_CELL) / (double) NOISE_CELL;
        double v00 = hashToFloat(gx, gz);
        double v10 = hashToFloat(gx + 1, gz);
        double v01 = hashToFloat(gx, gz + 1);
        double v11 = hashToFloat(gx + 1, gz + 1);
        double sx = fx * fx * (3 - 2 * fx);
        double sz = fz * fz * (3 - 2 * fz);
        double v = v00 * (1 - sx) * (1 - sz) + v10 * sx * (1 - sz) + v01 * (1 - sx) * sz + v11 * sx * sz;
        return (v - 0.5) * 3.0;
    }

    private static double hashToFloat(int gx, int gz) {
        int h = gx * 374761393 + gz * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        h = h ^ (h >> 16);
        return (h & 0xFFFF) / 65535.0;
    }

    private static void maybeSpawnGrassDecor(World world, int wx, int wy, int wz) {
        int h = wx * 1000003 + wz * 999983;
        h = (h ^ (h >> 13)) * 1274126177;
        h = h ^ (h >> 16);
        int roll = h & 0xFF;
        if (roll >= 60) return; // ~23% chance
        String plant = GRASS_DECOR[((h >> 8) & 0xFF) % GRASS_DECOR.length];
        var existing = world.getBlockType(wx, wy, wz);
        String existId = existing != null ? existing.getId() : null;
        if (existId != null && !existId.startsWith("Empty") && !existId.startsWith("Air")
                && !existId.startsWith("Filter_")) return;
        world.setBlock(wx, wy, wz, plant);
    }
}
