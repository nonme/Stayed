package dev.hearthbound.building;

import com.hypixel.hytale.server.core.universe.world.World;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.village.VillageData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Builds pathway networks between buildings: replaces Soil_Grass* with Soil_Pathway
 * along an A*-routed line per pair, and persists every converted cell so we can
 * undo or regenerate later.
 *
 * Pure utility — no state, no scheduling. The caller is responsible for saving
 * the VillageData component after we mutate its pathway list.
 */
public final class PathwayBuilder {

    private static final Logger LOGGER = Logger.getLogger(PathwayBuilder.class.getName());

    private static final String PATHWAY_BLOCK = "Soil_Pathway";
    /** When clearing, we restore to this — single canonical grass id, see chat thread. */
    private static final String GRASS_RESTORE_BLOCK = "Soil_Grass";

    /** Y range scanned downward from doorY+lookup to find the surface block under each cell. */
    private static final int SURFACE_SCAN_UP = 4;
    private static final int SURFACE_SCAN_DOWN = 8;

    /**
     * Wider scan range used only when resolving a building's door endpoint. The mine's
     * anchor sits 10 blocks above the actual entrance, so we need to be able to fall
     * far enough to land on the real ground next to the building.
     */
    private static final int DOOR_SCAN_UP = 5;
    private static final int DOOR_SCAN_DOWN = 16;

    /**
     * Manhattan-distance radius used by the door BFS when searching for the closest
     * grass-or-pathway tile near a building's entrance. Big enough to clear interior
     * thresholds, the mine's stone slab, and the farm's internal walkway, while staying
     * close enough to the building that the endpoint visually reads as "the door".
     */
    private static final int DOOR_BFS_RADIUS = 12;

    /** A* hard cap so a hopelessly blocked route can't run forever. */
    private static final int MAX_ASTAR_ITERATIONS = 20000;
    /** Manhattan distance ceiling — paths longer than this are skipped. */
    private static final int MAX_PATH_LENGTH = 120;

    private PathwayBuilder() {}

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Connects every completed building in the village via a Minimum Spanning Tree.
     * Each edge is routed with A* and converted cell-by-cell. Returns total blocks placed.
     */
    public static int connectAll(World world, VillageData village) {
        List<int[]> nodes = collectDoorNodes(world, village);
        if (nodes.size() < 2) return 0;

        for (int i = 0; i < nodes.size(); i++) {
            int[] n = nodes.get(i);
            String surfaceId = readBlockId(world, n[0], n[1], n[2]);
            LOGGER.info("PathwayBuilder node[" + i + "] = (" + n[0] + "," + n[1] + "," + n[2]
                    + ") surface=" + surfaceId);
        }

        List<int[]> edges = minimumSpanningTree(nodes);
        int placed = 0;
        for (int[] e : edges) {
            int[] a = nodes.get(e[0]);
            int[] b = nodes.get(e[1]);
            placed += routeBetween(world, village, a[0], a[1], a[2], b[0], b[1], b[2]);
        }
        LOGGER.info("PathwayBuilder.connectAll: " + nodes.size() + " buildings, "
                + edges.size() + " edges, " + placed + " blocks placed");
        return placed;
    }

    private static String readBlockId(World world, int x, int y, int z) {
        try {
            var bt = world.getBlockType(x, y, z);
            return bt != null ? bt.getId() : "Empty";
        } catch (Exception e) {
            return "?";
        }
    }

    /**
     * Connects a single new building to its nearest already-connected neighbor.
     * Used at building completion so we don't recompute the whole network.
     */
    public static int connectNewBuilding(World world, VillageData village, BuildingRecord newBuilding) {
        int[] target = doorNode(world, newBuilding);
        if (target == null) return 0;

        int[] nearest = null;
        long bestDistSq = Long.MAX_VALUE;
        for (BuildingRecord other : village.getBuildings()) {
            if (other == newBuilding) continue;
            if (!other.isCompleted()) continue;
            int[] node = doorNode(world, other);
            if (node == null) continue;
            long dx = node[0] - target[0];
            long dz = node[2] - target[2];
            long d = dx * dx + dz * dz;
            if (d < bestDistSq) {
                bestDistSq = d;
                nearest = node;
            }
        }
        if (nearest == null) return 0;
        return routeBetween(world, village, nearest[0], nearest[1], nearest[2],
                target[0], target[1], target[2]);
    }

    /**
     * Restores grass for every registered pathway block. Only touches cells that
     * currently hold a Soil_Pathway* block — anything the player changed manually
     * is left alone (and dropped from the registry).
     */
    public static int clearAll(World world, VillageData village) {
        int restored = 0;
        for (int[] p : village.getPathwayBlocks()) {
            try {
                var bt = world.getBlockType(p[0], p[1], p[2]);
                String id = bt != null ? bt.getId() : "Empty";
                if (id.startsWith(PATHWAY_BLOCK)) {
                    world.setBlock(p[0], p[1], p[2], GRASS_RESTORE_BLOCK);
                    restored++;
                }
            } catch (Exception e) {
                // Chunk unloaded — skip; the block stays as pathway in-world but
                // the registry will be wiped below, so it'll be orphaned. Acceptable
                // for a debug command.
            }
        }
        village.clearPathwayBlocks();
        LOGGER.info("PathwayBuilder.clearAll: restored " + restored + " of "
                + village.getPathwayBlocks().size() + " registered cells");
        return restored;
    }

    // ── Door nodes ─────────────────────────────────────────────────────────────

    private static List<int[]> collectDoorNodes(World world, VillageData village) {
        List<int[]> out = new ArrayList<>();
        for (BuildingRecord b : village.getBuildings()) {
            if (!b.isCompleted()) continue;
            int[] node = doorNode(world, b);
            if (node != null) out.add(node);
        }
        return out;
    }

    /**
     * Returns the world (x, y, z) of an open-ground tile near this building's entrance —
     * the place where a road should physically end.
     *
     * <p>Strategy: pick a base point near the door/entrance, then breadth-first search
     * outward looking for the first grass-or-pathway tile. We do BFS rather than a
     * straight line because some buildings ({@code anchorPrefabRotation != 0}, like the
     * farm) have door offsets whose sign doesn't trivially encode "outward direction"
     * in world space. BFS just looks in every direction and picks the closest valid
     * tile, which works regardless of how the prefab is oriented internally.
     *
     * <p>Existing {@code Soil_Pathway} is a valid endpoint, not just grass — that lets a
     * new external road fork off pathway tiles already baked into a prefab (the farm has
     * an internal Soil_Pathway corridor between its plots).
     */
    private static int[] doorNode(World world, BuildingRecord b) {
        // Endpoint resolution:
        //   - Buildings with a prefab door (house, town hall, warehouse): seed at the
        //     door tile, then probe each of the 4 cardinal directions for the nearest
        //     grass-or-pathway. The closest hit wins. Cardinal-only avoids the diagonal
        //     "drift sideways" that BFS produced.
        //   - Farm: hard-coded seed at the fence opening (native (+1, -4) from scarecrow).
        //     Same 4-direction probe outward.
        //   - Mine, sawmill: open structures with no fixed egress. Use BFS from anchor.
        if (BuildingType.TOWN_HALL.equals(b.getType())) {
            // Town hall has two doors in the prefab; BuildingLayout.findDoor non-deterministically
            // picks one (often the side door). We want the front entrance, which is at native
            // (0, 1, -2) relative to founding-stone anchor (-5, 1, 1) → relative offset (5, -3).
            int steps = BuildingLayout.get(b.getType(), b.getVariant()).rotationSteps(b.getRotation());
            int[] doorLocal = BuildingLayout.rotateLocalOffset(5, 0, -3, steps);
            int seedX = b.getPosX() + doorLocal[0];
            int seedZ = b.getPosZ() + doorLocal[2];
            return cardinalProbeEndpoint(world, b, seedX, seedZ, "townhall-front");
        }
        BuildingLayout.Layout layout = BuildingLayout.get(b.getType(), b.getVariant());
        if (layout != null && layout.hasDoor()) {
            int steps = layout.rotationSteps(b.getRotation());
            int[] doorLocal = BuildingLayout.rotateLocalOffset(
                    layout.doorLX(), layout.doorLY(), layout.doorLZ(), steps);
            int seedX = b.getPosX() + doorLocal[0];
            int seedZ = b.getPosZ() + doorLocal[2];
            return cardinalProbeEndpoint(world, b, seedX, seedZ, "layout-door");
        }
        if (BuildingType.FARM.equals(b.getType())) {
            // The scarecrow's anchor block is the centered (filler=0) tile at native
            // (0, 2, +4), not the leftmost column. The fence opening is at (0, *, -4),
            // so gate-from-anchor in native = (0, -8). Verified empirically: at world
            // anchor (-18,-181) rot=3 the gate lands at (-26,-181), which matches.
            int steps = BuildingLayout.get(b.getType(), b.getVariant()).rotationSteps(b.getRotation());
            int[] gateLocal = BuildingLayout.rotateLocalOffset(0, 0, -8, steps);
            int seedX = b.getPosX() + gateLocal[0];
            int seedZ = b.getPosZ() + gateLocal[2];
            LOGGER.info("doorNode farm steps=" + steps + " gateLocal=("
                    + gateLocal[0] + "," + gateLocal[2] + ") seed=(" + seedX + "," + seedZ + ")");
            return cardinalProbeEndpoint(world, b, seedX, seedZ, "farm-gate");
        }

        // Open structures (mine, sawmill).
        int[] found = bfsForGrassOrPathway(world, b.getPosX(), b.getPosZ(), b.getPosY());
        if (found != null) {
            LOGGER.info("doorNode " + b.getType() + " anchor=(" + b.getPosX() + ","
                    + b.getPosY() + "," + b.getPosZ() + ") rot=" + b.getRotation()
                    + " bfs-from-anchor → endpoint=(" + found[0] + "," + found[1]
                    + "," + found[2] + ")");
            return found;
        }
        int y = surfaceYWide(world, b.getPosX(), b.getPosZ(), b.getPosY());
        if (y == Integer.MIN_VALUE) y = b.getPosY();
        return new int[]{ b.getPosX(), y, b.getPosZ() };
    }

    /**
     * Walks from {@code (seedX, seedZ)} along each of the 4 cardinal directions one
     * block at a time, looking for grass or existing pathway. Returns the first hit
     * across all four rays — i.e. the closest grass tile reachable straight from the
     * seed, with ties broken by the iteration order (+X, -X, +Z, -Z).
     *
     * <p>If no direction finds a hit within {@link #DOOR_BFS_RADIUS} the seed itself
     * is returned so callers always get a usable endpoint.
     */
    private static int[] cardinalProbeEndpoint(World world, BuildingRecord b,
                                                int seedX, int seedZ, String src) {
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        int bestX = seedX, bestZ = seedZ, bestY = Integer.MIN_VALUE;
        int bestDist = Integer.MAX_VALUE;

        for (int[] d : dirs) {
            for (int step = 1; step <= DOOR_BFS_RADIUS; step++) {
                int x = seedX + d[0] * step;
                int z = seedZ + d[1] * step;
                int y = surfaceYWide(world, x, z, b.getPosY());
                if (y == Integer.MIN_VALUE) continue;
                String id = readBlockId(world, x, y, z);
                if (isGrassOrPathway(id) && step < bestDist) {
                    bestX = x; bestZ = z; bestY = y; bestDist = step;
                    break; // shorter steps in other directions can still beat this
                }
            }
        }

        if (bestY == Integer.MIN_VALUE) {
            // No grass on any cardinal ray — fall back to the seed itself.
            int seedY = surfaceYWide(world, seedX, seedZ, b.getPosY());
            bestY = (seedY == Integer.MIN_VALUE) ? b.getPosY() : seedY;
        }

        LOGGER.info("doorNode " + b.getType() + " anchor=(" + b.getPosX() + ","
                + b.getPosY() + "," + b.getPosZ() + ") rot=" + b.getRotation() + " "
                + src + " seed=(" + seedX + "," + seedZ + ")"
                + " → endpoint=(" + bestX + "," + bestY + "," + bestZ
                + ") dist=" + (bestDist == Integer.MAX_VALUE ? "fallback" : bestDist));
        return new int[]{ bestX, bestY, bestZ };
    }

    /** World rotation index 0..3 → unit XZ vector pointing "outward" from a door. */
    private static int[] directionFromRotation(int rot) {
        return switch (rot & 0x3) {
            case 1  -> new int[]{ 1,  0};
            case 2  -> new int[]{ 0, -1};
            case 3  -> new int[]{-1,  0};
            default -> new int[]{ 0,  1};
        };
    }

    /**
     * Manhattan-radius BFS from (cx, cz) looking for the closest tile whose surface is
     * grass or existing pathway. Returns {x, y, z} or null if nothing found within
     * {@link #DOOR_BFS_RADIUS}.
     */
    private static int[] bfsForGrassOrPathway(World world, int cx, int cz, int hintY) {
        Set<Long> visited = new HashSet<>();
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        queue.add(new int[]{ cx, cz, 0 });
        visited.add(packXZ(cx, cz));

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int x = cur[0], z = cur[1], dist = cur[2];

            int y = surfaceYWide(world, x, z, hintY);
            if (y != Integer.MIN_VALUE) {
                String id = readBlockId(world, x, y, z);
                if (isGrassOrPathway(id)) {
                    return new int[]{ x, y, z };
                }
            }

            if (dist >= DOOR_BFS_RADIUS) continue;
            for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int nx = x + d[0];
                int nz = z + d[1];
                long key = packXZ(nx, nz);
                if (visited.add(key)) {
                    queue.add(new int[]{ nx, nz, dist + 1 });
                }
            }
        }
        return null;
    }

    /**
     * Wider-range surface scan used for the door endpoint only. Skips not just air and
     * vegetation but every non-natural surface (wood, stone bricks, furniture, custom
     * Stayed_ blocks, …) — otherwise a house roof or interior floor pinned above the
     * actual ground would fool the scan into anchoring the pathway in mid-air.
     */
    private static int surfaceYWide(World world, int x, int z, int hintY) {
        int top = hintY + DOOR_SCAN_UP;
        int bottom = hintY - DOOR_SCAN_DOWN;
        for (int y = top; y >= bottom; y--) {
            try {
                var bt = world.getBlockType(x, y, z);
                if (bt == null) continue;
                String id = bt.getId();
                if (!isNaturalGround(id)) continue;
                return y;
            } catch (Exception e) {
                return Integer.MIN_VALUE;
            }
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Returns true if the block directly above the surface tile is a *small* obstacle
     * a road shouldn't cross — fences, tree trunks, leaves, signs, brazier, scarecrow.
     *
     * <p>We deliberately do NOT block on broad/tall structures (walls, roofs, beams,
     * stone) because those usually appear far above the ground tile we're walking on
     * (e.g. a building's eaves over the dirt path beneath). Blocking on them would
     * make A* refuse any path that grazes a structure's footprint, even when the
     * ground under it is clear.
     */
    private static boolean isObstructed(World world, int x, int surfaceY, int z) {
        try {
            var bt = world.getBlockType(x, surfaceY + 1, z);
            if (bt == null) return false;
            String id = bt.getId();
            if (id == null || id.isEmpty()) return false;
            // Tree parts.
            if (id.contains("Trunk") || id.contains("Leaves")) return true;
            // Fences and gates.
            if (id.contains("Fence") || id.contains("Gate")) return true;
            // Custom anchor blocks — scarecrow, brazier, mine sign, lumbermill, etc.
            if (id.startsWith("Stayed_")) return true;
            // Furniture left in the open (lamps, signs, posts).
            if (id.startsWith("Furniture_")) return true;
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Soil, rock, sand, gravel — anything you'd expect to find as natural ground.
     * Explicitly excludes everything player-built (Wood_*, Stayed_*, Furniture_*, Cloth_*,
     * Deco_*, …) so the scan walks past house roofs and floors down to actual terrain.
     */
    private static boolean isNaturalGround(String id) {
        if (id == null || id.isEmpty()) return false;
        return id.startsWith("Soil_")
                || id.startsWith("Rock_")
                || id.startsWith("Sand_")
                || id.startsWith("Snow_")
                || id.startsWith("Ice_")
                || id.startsWith("Ore_");
    }

    // ── MST ────────────────────────────────────────────────────────────────────

    /** Returns edges as int[]{i, j} indices into nodes. Prim's algorithm. */
    private static List<int[]> minimumSpanningTree(List<int[]> nodes) {
        int n = nodes.size();
        boolean[] inTree = new boolean[n];
        int[] parent = new int[n];
        long[] minDist = new long[n];
        for (int i = 0; i < n; i++) { minDist[i] = Long.MAX_VALUE; parent[i] = -1; }
        minDist[0] = 0;

        for (int i = 0; i < n; i++) {
            int u = -1;
            long best = Long.MAX_VALUE;
            for (int v = 0; v < n; v++) {
                if (!inTree[v] && minDist[v] < best) { best = minDist[v]; u = v; }
            }
            if (u == -1) break;
            inTree[u] = true;
            int[] pu = nodes.get(u);
            for (int v = 0; v < n; v++) {
                if (inTree[v]) continue;
                int[] pv = nodes.get(v);
                long dx = pu[0] - pv[0];
                long dz = pu[2] - pv[2];
                long d = dx * dx + dz * dz;
                if (d < minDist[v]) { minDist[v] = d; parent[v] = u; }
            }
        }

        List<int[]> edges = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            if (parent[i] >= 0) edges.add(new int[]{ parent[i], i });
        }
        return edges;
    }

    // ── Per-edge routing ───────────────────────────────────────────────────────

    /**
     * Routes one edge using A* in the XZ plane and converts grass cells along the
     * way. Y is resolved per-cell by a downward surface scan from the source door's Y.
     * Returns the number of blocks converted on this edge.
     */
    private static int routeBetween(World world, VillageData village,
                                     int x1, int y1, int z1, int x2, int y2, int z2) {
        int manhattan = Math.abs(x1 - x2) + Math.abs(z1 - z2);
        if (manhattan == 0) return 0;
        if (manhattan > MAX_PATH_LENGTH) {
            LOGGER.info("routeBetween: SKIP — distance " + manhattan + " > " + MAX_PATH_LENGTH
                    + "  (" + x1 + "," + z1 + ") → (" + x2 + "," + z2 + ")");
            return 0;
        }

        List<long[]> path = aStar(world, x1, z1, y1, x2, z2, true);
        if (path == null) {
            // Retry without obstruction filtering — the road will go through small
            // obstacles (a stray fence post, a tree) but at least we'll get connectivity.
            // Better an imperfect path than none.
            path = aStar(world, x1, z1, y1, x2, z2, false);
            if (path == null) {
                LOGGER.info("routeBetween: A* FAILED  (" + x1 + "," + y1 + "," + z1
                        + ") → (" + x2 + "," + y2 + "," + z2 + ")  manhattan=" + manhattan);
                return 0;
            }
            LOGGER.info("routeBetween: A* fell back to obstruction-free pass  ("
                    + x1 + "," + z1 + ") → (" + x2 + "," + z2 + ")  pathLen=" + path.size());
        }

        int placed = 0;
        int grassSeen = 0, nonGrassSeen = 0;
        for (long[] cell : path) {
            int x = (int) cell[0];
            int z = (int) cell[1];
            int y = (int) cell[2];
            if (placeIfGrass(world, x, y, z)) {
                village.addPathwayBlock(x, y, z);
                placed++;
                grassSeen++;
            } else {
                nonGrassSeen++;
            }
        }
        LOGGER.info("routeBetween OK: (" + x1 + "," + z1 + ") → (" + x2 + "," + z2
                + ") pathLen=" + path.size() + " placed=" + placed
                + " grass=" + grassSeen + " nonGrass=" + nonGrassSeen);
        return placed;
    }

    /** Replaces a grass-family block with Soil_Pathway. Returns true if a change was made. */
    private static boolean placeIfGrass(World world, int x, int y, int z) {
        try {
            var bt = world.getBlockType(x, y, z);
            if (bt == null) return false;
            String id = bt.getId();
            if (!isGrassBlock(id)) return false;
            world.setBlock(x, y, z, PATHWAY_BLOCK);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Matches any Soil_Grass* variant (Dry, Wet, Cold, Sunny, Deep, Burnt, Full, …). */
    private static boolean isGrassBlock(String id) {
        return id != null && id.startsWith("Soil_Grass");
    }

    /** Either grass (we can convert) or existing pathway (we can join onto). */
    private static boolean isGrassOrPathway(String id) {
        return id != null && (id.startsWith("Soil_Grass") || id.startsWith("Soil_Pathway"));
    }

    // ── A* in XZ plane with per-cell Y resolution ──────────────────────────────

    /**
     * 2D A* on the XZ grid. For each visited cell we resolve the surface Y by
     * scanning downward from a hint Y. Cells whose surface drops/raises by more
     * than 1 from the previous cell get a stiff penalty (avoids climbing cliffs).
     *
     * Returns a list of {x, z, surfaceY} or null if no path was found.
     */
    private static List<long[]> aStar(World world, int sx, int sz, int hintY, int gx, int gz,
                                       boolean respectObstructions) {
        record Node(int x, int z, int y, double f) {}

        Map<Long, Integer> bestG = new HashMap<>();
        Map<Long, long[]> cameFrom = new HashMap<>();
        Map<Long, Integer> cellY = new HashMap<>();
        Set<Long> closed = new HashSet<>();

        int startSurface = surfaceY(world, sx, sz, hintY);
        if (startSurface == Integer.MIN_VALUE) startSurface = hintY;

        long startKey = packXZ(sx, sz);
        cellY.put(startKey, startSurface);
        bestG.put(startKey, 0);

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        open.add(new Node(sx, sz, startSurface, heuristic(sx, sz, gx, gz)));

        int iterations = 0;
        while (!open.isEmpty() && iterations++ < MAX_ASTAR_ITERATIONS) {
            Node cur = open.poll();
            long curKey = packXZ(cur.x, cur.z);
            if (closed.contains(curKey)) continue;
            closed.add(curKey);

            if (cur.x == gx && cur.z == gz) {
                return reconstruct(cameFrom, cellY, curKey);
            }

            int curG = bestG.getOrDefault(curKey, Integer.MAX_VALUE);

            for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                int nx = cur.x + d[0];
                int nz = cur.z + d[1];
                long nKey = packXZ(nx, nz);
                if (closed.contains(nKey)) continue;

                int neighborSurface = surfaceY(world, nx, nz, cur.y);
                if (neighborSurface == Integer.MIN_VALUE) continue;
                int slope = Math.abs(neighborSurface - cur.y);
                if (slope > 2) continue; // unscalable cliff

                // Reject the cell if something solid is sitting on top of the surface —
                // fence, tree trunk, wall, sign, etc. The pathway must not run through
                // obstacles even when the ground tile underneath is grass. The caller
                // disables this check on the fallback retry to guarantee connectivity.
                if (respectObstructions && isObstructed(world, nx, neighborSurface, nz)) continue;

                int stepCost = 1 + slope * 3;
                int tentativeG = curG + stepCost;

                if (tentativeG < bestG.getOrDefault(nKey, Integer.MAX_VALUE)) {
                    bestG.put(nKey, tentativeG);
                    cellY.put(nKey, neighborSurface);
                    cameFrom.put(nKey, new long[]{ cur.x, cur.z });
                    double f = tentativeG + heuristic(nx, nz, gx, gz);
                    open.add(new Node(nx, nz, neighborSurface, f));
                }
            }
        }
        return null;
    }

    private static double heuristic(int x, int z, int gx, int gz) {
        int dx = Math.abs(x - gx);
        int dz = Math.abs(z - gz);
        return dx + dz;
    }

    private static long packXZ(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static List<long[]> reconstruct(Map<Long, long[]> cameFrom, Map<Long, Integer> cellY, long endKey) {
        List<long[]> out = new ArrayList<>();
        long cur = endKey;
        while (true) {
            int x = (int) (cur >> 32);
            int z = (int) (cur & 0xFFFFFFFFL);
            int y = cellY.getOrDefault(cur, 0);
            out.add(new long[]{ x, z, y });
            long[] prev = cameFrom.get(cur);
            if (prev == null) break;
            cur = packXZ((int) prev[0], (int) prev[1]);
        }
        java.util.Collections.reverse(out);
        return out;
    }

    /**
     * Scans downward from {@code hintY + SURFACE_SCAN_UP} looking for the topmost
     * natural-ground block at (x, z). Same filtering rules as {@link #surfaceYWide}:
     * walks past anything built by the player so A* doesn't try to route over rooftops.
     */
    private static int surfaceY(World world, int x, int z, int hintY) {
        int top = hintY + SURFACE_SCAN_UP;
        int bottom = hintY - SURFACE_SCAN_DOWN;
        for (int y = top; y >= bottom; y--) {
            try {
                var bt = world.getBlockType(x, y, z);
                if (bt == null) continue;
                if (!isNaturalGround(bt.getId())) continue;
                return y;
            } catch (Exception e) {
                return Integer.MIN_VALUE;
            }
        }
        return Integer.MIN_VALUE;
    }
}
