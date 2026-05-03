package dev.hearthbound.building;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.universe.world.World;

import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillagerSummary;

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

    /**
     * Edge-generation algorithm. Each strategy connects the same set of door nodes a
     * different way, so visually the network looks distinct while the per-edge routing
     * stays identical. Pick via {@code /hb pathways generate <strategy>}.
     */
    public enum Strategy {
        /** Minimum Spanning Tree: N-1 edges, shortest total length, every building reachable. */
        MST,
        /** k-Nearest Neighbors with k=2: each building wired to its 2 closest peers. */
        KNN,
        /** Town hall is a hub: every other building gets a direct edge to it. */
        HUB,
        /** MST plus extra edges where the in-tree detour is much longer than straight-line. */
        MST_PLUS_SHORTCUTS;

        /** Returns null on an unknown name so the caller can show a clear error. */
        public static Strategy parse(String s) {
            if (s == null) return null;
            return switch (s.toLowerCase()) {
                case "mst" -> MST;
                case "knn" -> KNN;
                case "hub" -> HUB;
                case "shortcuts", "mst-plus", "mst+", "shortcut" -> MST_PLUS_SHORTCUTS;
                default -> null;
            };
        }
    }

    /** A door endpoint plus a back-pointer to the building so HUB can find the town hall. */
    private record Node(int x, int y, int z, BuildingRecord building) {}

    /** When a tree-distance vs straight-line ratio exceeds this, a shortcut edge is added. */
    private static final double DETOUR_RATIO_THRESHOLD = 2;

    // ── Public API ─────────────────────────────────────────────────────────────

    /** Default for callers that don't pick a strategy: KNN base + commute + warehouse. */
    public static int connectAll(World world, VillageData village) {
        return connectAll(world, village, Strategy.KNN, null);
    }

    /**
     * Lays both layers in one call: the base strategy followed by commute and
     * warehouse-hub edges. Used by building-completion callsites where the player
     * doesn't pick layers manually. {@code shortcutsRatio} is only consulted by
     * {@link Strategy#MST_PLUS_SHORTCUTS}; pass null for the default.
     */
    public static int connectAll(World world, VillageData village, Strategy strategy,
                                 Double shortcutsRatio) {
        int placedBase = connectBase(world, village, strategy, shortcutsRatio);
        int placedExtra = connectExtra(world, village);
        return placedBase + placedExtra;
    }

    /**
     * Lays only the base strategy edges (no commute, no warehouse-hub). Use this
     * when iterating on the visual look of a strategy without the extra layer
     * muddling the picture.
     */
    public static int connectBase(World world, VillageData village, Strategy strategy,
                                   Double shortcutsRatio) {
        List<Node> nodes = collectNodes(world, village);
        if (nodes.size() < 2) return 0;
        logNodes(world, nodes);

        double ratio = shortcutsRatio != null ? shortcutsRatio : DETOUR_RATIO_THRESHOLD;
        List<int[]> edges = buildEdges(nodes, strategy, ratio);
        // Base strategies are responsible for connectivity, so we accept an
        // imperfect path through small obstructions rather than leaving a
        // building stranded.
        int placed = routeEdges(world, village, nodes, edges, true);
        LOGGER.info("PathwayBuilder.connectBase[" + strategy + "]: "
                + nodes.size() + " buildings, " + edges.size() + " edges, "
                + placed + " blocks placed");
        return placed;
    }

    /**
     * Lays only the extra layer on top of whatever already exists: commute edges
     * (home↔workplace per villager) and warehouse-hub edges (warehouse↔work
     * buildings). Useful for iterating on the extra layer alone after a base run.
     *
     * <p>Does not deduplicate against existing pathways in the world — if a
     * commute pair was also drawn by a base strategy earlier, A* will simply
     * retrace the existing tiles (the path-place step is idempotent).
     */
    public static int connectExtra(World world, VillageData village) {
        List<Node> nodes = collectNodes(world, village);
        if (nodes.size() < 2) return 0;

        List<int[]> edges = new ArrayList<>();
        Set<Long> seenEdges = new HashSet<>();
        addCommuteEdges(village, nodes, edges, seenEdges);
        int afterCommute = edges.size();
        addWarehouseHubEdges(nodes, edges, seenEdges);
        int afterWarehouse = edges.size();

        // Drop any edge whose endpoints are already linked by an existing Soil_Pathway
        // network in the world — otherwise we'd lay a parallel road 1-2 blocks beside
        // it. The base strategy and earlier extra runs both contribute to this network.
        int dropped = 0;
        List<int[]> filteredEdges = new ArrayList<>();
        for (int[] e : edges) {
            Node a = nodes.get(e[0]);
            Node b = nodes.get(e[1]);
            if (isReachableViaPathway(world, a.x, a.y, a.z, b.x, b.z)) {
                dropped++;
                LOGGER.info("connectExtra: skip edge " + a.building.getType()
                        + " → " + b.building.getType() + " (already reachable via pathway)");
                continue;
            }
            filteredEdges.add(e);
        }

        // Extra edges are by definition redundant — if A* can't find a clean
        // path the same pair is already reachable via the base network, so we
        // refuse to fall back through obstructions. Better no parallel road
        // than one that tunnels under the player's fences and trees.
        int placed = routeEdges(world, village, nodes, filteredEdges, false);
        LOGGER.info("PathwayBuilder.connectExtra: "
                + afterCommute + " commute edges, "
                + (afterWarehouse - afterCommute) + " warehouse-hub edges, "
                + dropped + " skipped (already connected), "
                + placed + " blocks placed");
        return placed;
    }

    private static void logNodes(World world, List<Node> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            String surfaceId = readBlockId(world, n.x, n.y, n.z);
            LOGGER.info("PathwayBuilder node[" + i + "] = " + n.building.getType()
                    + " (" + n.x + "," + n.y + "," + n.z + ") surface=" + surfaceId);
        }
    }

    private static int routeEdges(World world, VillageData village, List<Node> nodes,
                                   List<int[]> edges, boolean allowFallback) {
        int placed = 0;
        for (int[] e : edges) {
            Node a = nodes.get(e[0]);
            Node b = nodes.get(e[1]);
            placed += routeBetween(world, village, a.x, a.y, a.z, b.x, b.y, b.z, allowFallback);
        }
        return placed;
    }

    /**
     * How close the existing pathway network must come to a door endpoint to count
     * as "already reachable" — door endpoints sit on grass, not on a pathway tile,
     * so we need a small slack distance to call the road "right next to the door".
     */
    private static final int PATHWAY_PROXIMITY_RADIUS = 3;
    /** Safety cap on BFS exploration so big networks don't stall the command. */
    private static final int MAX_REACHABILITY_BFS_NODES = 5000;
    /**
     * Max gap (in blocks, Chebyshev) the reachability BFS will jump across when the
     * next pathway tile is separated by non-pathway terrain — boulders, dirt patches,
     * cobble outcrops. A real road built across rocky terrain leaves the rock untouched
     * (we only convert grass), so the Soil_Pathway tiles end up forming islands. Without
     * a gap-jump the BFS treats those islands as disconnected and we end up laying a
     * parallel road right next to the existing one. Three blocks is enough to bridge
     * typical decor (1-3 cobble stones, narrow gravel strip) without letting the BFS
     * teleport across genuine empty stretches of the map.
     */
    private static final int PATHWAY_GAP_JUMP = 3;

    /**
     * True if the existing Soil_Pathway* network in the world already connects
     * {@code from} to within {@link #PATHWAY_PROXIMITY_RADIUS} of {@code to}.
     *
     * <p>Strategy: start a BFS from a small box around {@code from}, but only
     * traverse cells that are {@code Soil_Pathway*}. On each visited cell we check
     * whether we're close enough to {@code to}. If so — connected. Otherwise we
     * exhaust the network or the node cap and report not-connected.
     *
     * <p>Why not start strictly on {@code from}: door endpoints live on grass,
     * never on a pathway tile, so a strict start would terminate immediately. The
     * small box around {@code from} lets us discover the pathway tile that the
     * existing road last touched near this door.
     */
    private static boolean isReachableViaPathway(World world,
                                                  int fromX, int fromY, int fromZ,
                                                  int toX, int toZ) {
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        Set<Long> visited = new HashSet<>();

        // Seed the BFS with every pathway tile in a small box around `from`. If the
        // base strategy laid a road that ends near this door, one of these tiles
        // will pick it up. If nothing nearby is pathway, the queue stays empty and
        // we report not-connected (correctly — there's no road to walk along).
        for (int dx = -PATHWAY_PROXIMITY_RADIUS; dx <= PATHWAY_PROXIMITY_RADIUS; dx++) {
            for (int dz = -PATHWAY_PROXIMITY_RADIUS; dz <= PATHWAY_PROXIMITY_RADIUS; dz++) {
                int x = fromX + dx;
                int z = fromZ + dz;
                int y = surfaceY(world, x, z, fromY);
                if (y == Integer.MIN_VALUE) continue;
                String id = readBlockId(world, x, y, z);
                if (id == null || !id.startsWith("Soil_Pathway")) continue;
                long key = packXZ(x, z);
                if (visited.add(key)) queue.add(new int[]{ x, y, z });
            }
        }
        LOGGER.info("isReachableViaPathway: from=(" + fromX + "," + fromY + "," + fromZ
                + ") to=(" + toX + "," + toZ + ")  seedTiles=" + queue.size());
        if (queue.isEmpty()) return false;

        int explored = 0;
        while (!queue.isEmpty() && explored++ < MAX_REACHABILITY_BFS_NODES) {
            int[] cur = queue.poll();
            int cx = cur[0], cy = cur[1], cz = cur[2];

            // Reached the target's neighborhood — same proximity rule as the seed box.
            if (Math.abs(cx - toX) <= PATHWAY_PROXIMITY_RADIUS
                    && Math.abs(cz - toZ) <= PATHWAY_PROXIMITY_RADIUS) {
                LOGGER.info("isReachableViaPathway: HIT after " + explored
                        + " nodes, at (" + cx + "," + cy + "," + cz + ")");
                return true;
            }

            // 8-connected expansion plus a small "gap jump": when a 1-step neighbor is
            // not a pathway tile, we still scan a Chebyshev box of size PATHWAY_GAP_JUMP
            // around it and take the first pathway tile we find. This bridges short non-
            // pathway interruptions (cobble/dirt/gravel decor, narrow rocky strips) that
            // routeBetween left untouched because Soil_Pathway is only laid over grass.
            // The road is physically continuous in those spots — only the registry tiles
            // are interrupted — so jumping the gap recovers connectivity without letting
            // the BFS teleport across genuinely empty terrain.
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    int nx = cx + dx;
                    int nz = cz + dz;
                    long key = packXZ(nx, nz);
                    if (!visited.add(key)) continue;
                    int ny = surfaceY(world, nx, nz, cy);
                    if (ny == Integer.MIN_VALUE) continue;
                    String id = readBlockId(world, nx, ny, nz);
                    if (id != null && id.startsWith("Soil_Pathway")) {
                        queue.add(new int[]{ nx, ny, nz });
                        continue;
                    }
                    // Direct neighbor isn't a pathway. Probe a small Chebyshev box for
                    // the closest pathway tile and enqueue that, marking the intermediate
                    // cells visited so we don't re-probe them from another direction.
                    int[] bridged = findPathwayWithinGap(world, nx, nz, cy, visited);
                    if (bridged != null) {
                        queue.add(bridged);
                    }
                }
            }
        }
        LOGGER.info("isReachableViaPathway: MISS after exhausting " + explored
                + " nodes (visited=" + visited.size() + ")");
        return false;
    }

    /**
     * Looks within a Chebyshev box of size {@link #PATHWAY_GAP_JUMP} around (sx, sz)
     * for the closest Soil_Pathway tile. Cells inside the box are added to
     * {@code visited} regardless of outcome so the outer BFS won't re-probe them.
     * Returns {@code {x, y, z}} of the discovered pathway tile or null if none found.
     *
     * <p>The "closest" check is done by ascending Chebyshev radius — we find the tile
     * minimum number of jumps away, breaking ties by iteration order (good enough for
     * short bridges; we're not running Dijkstra here).
     */
    private static int[] findPathwayWithinGap(World world, int sx, int sz, int hintY,
                                                Set<Long> visited) {
        for (int r = 1; r <= PATHWAY_GAP_JUMP; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    // Only the ring at radius r — interior cells were already scanned
                    // at smaller r (or are the source itself when r >= 1).
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int x = sx + dx;
                    int z = sz + dz;
                    long key = packXZ(x, z);
                    if (!visited.add(key)) continue;
                    int y = surfaceY(world, x, z, hintY);
                    if (y == Integer.MIN_VALUE) continue;
                    String id = readBlockId(world, x, y, z);
                    if (id != null && id.startsWith("Soil_Pathway")) {
                        return new int[]{ x, y, z };
                    }
                }
            }
        }
        return null;
    }

    /**
     * Adds an edge between each villager's home and their workplace. Both buildings
     * carry the villager's UUID in {@code BuildingRecord.assignedVillagerId}; we look
     * the villager up in the node set and add the edge if both endpoints are present
     * and not already connected by the base strategy.
     */
    private static void addCommuteEdges(VillageData village, List<Node> nodes,
                                         List<int[]> edges, Set<Long> seenEdges) {
        for (VillagerSummary v : village.getVillagers()) {
            UUID uuid = v.getVillagerUuid();
            String prof = v.getProfession();
            if (uuid == null || prof == null || prof.isEmpty()) continue;

            int homeIdx = -1, workIdx = -1;
            for (int i = 0; i < nodes.size(); i++) {
                BuildingRecord b = nodes.get(i).building;
                if (!uuid.equals(b.getAssignedVillagerId())) continue;
                if (BuildingType.isResidential(b.getType())) homeIdx = i;
                else if (isWorkBuilding(b.getType())) workIdx = i;
            }
            if (homeIdx < 0 || workIdx < 0) continue;
            long key = edgeKey(homeIdx, workIdx);
            if (seenEdges.add(key)) edges.add(new int[]{ homeIdx, workIdx });
        }
    }

    /**
     * Adds an edge from the warehouse (if any) to every work building (farm/sawmill/mine).
     * The KNN strategy already wires the warehouse into the residential network, so
     * this hub-around-warehouse layer is purely about logistics — workers don't need
     * to walk through houses to drop produce off.
     */
    private static void addWarehouseHubEdges(List<Node> nodes, List<int[]> edges,
                                              Set<Long> seenEdges) {
        int warehouseIdx = -1;
        for (int i = 0; i < nodes.size(); i++) {
            if (BuildingType.WAREHOUSE.equals(nodes.get(i).building.getType())) {
                warehouseIdx = i;
                break;
            }
        }
        if (warehouseIdx < 0) return;
        for (int i = 0; i < nodes.size(); i++) {
            if (i == warehouseIdx) continue;
            if (!isWorkBuilding(nodes.get(i).building.getType())) continue;
            long key = edgeKey(warehouseIdx, i);
            if (seenEdges.add(key)) edges.add(new int[]{ warehouseIdx, i });
        }
    }

    private static boolean isWorkBuilding(String type) {
        return BuildingType.FARM.equals(type)
                || BuildingType.SAWMILL.equals(type)
                || BuildingType.MINE.equals(type);
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

    private static List<Node> collectNodes(World world, VillageData village) {
        List<Node> out = new ArrayList<>();
        for (BuildingRecord b : village.getBuildings()) {
            if (!b.isCompleted()) continue;
            int[] node = doorNode(world, b);
            if (node != null) out.add(new Node(node[0], node[1], node[2], b));
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
     * True if a 2-block-tall person can stand on the surface tile at (x, surfaceY, z).
     * Checks {@code surfaceY+1} and {@code surfaceY+2} — both must be air-like
     * ({@code BlockMaterial.Empty}). This catches fences, tree trunks, walls, custom
     * anchor blocks, and any other solid block above the surface in one universal
     * check, without needing a name-based whitelist.
     *
     * <p>Pathways are meant to be walked on by NPCs of standard height, so the
     * 2-block clearance rule mirrors actual NPC navigation requirements.
     */
    private static boolean hasHeadroom(World world, int x, int surfaceY, int z) {
        try {
            for (int dy = 1; dy <= 2; dy++) {
                var bt = world.getBlockType(x, surfaceY + dy, z);
                if (bt == null) continue; // null treated as empty (chunk edges, etc.)
                if (bt.getMaterial() != BlockMaterial.Empty) return false;
            }
            return true;
        } catch (Exception e) {
            // On a read error, be permissive — better an imperfect path than refusing
            // to route at all. The fallback A* pass exists for exactly this case.
            return true;
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

    // ── Edge generation ────────────────────────────────────────────────────────

    /** Squared XZ distance between two nodes — units are blocks². */
    private static long distSq(Node a, Node b) {
        long dx = a.x - b.x;
        long dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    /**
     * Dispatches to the chosen strategy. Returns edges as {@code int[]{i, j}} indices
     * into {@code nodes}. All strategies guarantee that every node is reachable.
     * {@code shortcutsRatio} is only consulted by {@link Strategy#MST_PLUS_SHORTCUTS}.
     */
    private static List<int[]> buildEdges(List<Node> nodes, Strategy strategy, double shortcutsRatio) {
        return switch (strategy) {
            case MST -> minimumSpanningTree(nodes);
            case KNN -> kNearestNeighbors(nodes, 2);
            case HUB -> hubAndSpoke(nodes);
            case MST_PLUS_SHORTCUTS -> mstPlusShortcuts(nodes, shortcutsRatio);
        };
    }

    /** Prim's MST: N-1 edges, every node reachable, minimal total length. */
    private static List<int[]> minimumSpanningTree(List<Node> nodes) {
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
            Node pu = nodes.get(u);
            for (int v = 0; v < n; v++) {
                if (inTree[v]) continue;
                long d = distSq(pu, nodes.get(v));
                if (d < minDist[v]) { minDist[v] = d; parent[v] = u; }
            }
        }

        List<int[]> edges = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            if (parent[i] >= 0) edges.add(new int[]{ parent[i], i });
        }
        return edges;
    }

    /**
     * Each node gets edges to its {@code k} nearest peers. Edges are deduplicated so
     * an A↔B and B↔A pair only routes once. Connectivity isn't strictly guaranteed for
     * pathological layouts, but with k=2 on a typical village footprint it always is.
     */
    private static List<int[]> kNearestNeighbors(List<Node> nodes, int k) {
        int n = nodes.size();
        Set<Long> seen = new HashSet<>();
        List<int[]> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Node a = nodes.get(i);
            // Pick the k smallest by squared distance, ignoring self.
            List<int[]> candidates = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                candidates.add(new int[]{ j, (int) Math.min(Integer.MAX_VALUE, distSq(a, nodes.get(j))) });
            }
            candidates.sort(Comparator.comparingInt(c -> c[1]));
            int take = Math.min(k, candidates.size());
            for (int t = 0; t < take; t++) {
                int j = candidates.get(t)[0];
                long key = edgeKey(i, j);
                if (seen.add(key)) edges.add(new int[]{ i, j });
            }
        }
        return edges;
    }

    /**
     * Town hall is the network hub: every other building gets a direct edge to it.
     * Falls back to MST if (somehow) no town hall is present — defensive only, the
     * village always has one in practice.
     */
    private static List<int[]> hubAndSpoke(List<Node> nodes) {
        int hubIndex = -1;
        for (int i = 0; i < nodes.size(); i++) {
            if (BuildingType.TOWN_HALL.equals(nodes.get(i).building.getType())) {
                hubIndex = i;
                break;
            }
        }
        if (hubIndex < 0) {
            LOGGER.warning("PathwayBuilder.hubAndSpoke: no town hall in node set; falling back to MST");
            return minimumSpanningTree(nodes);
        }
        List<int[]> edges = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            if (i == hubIndex) continue;
            edges.add(new int[]{ hubIndex, i });
        }
        return edges;
    }

    /**
     * Start with MST, then add a shortcut edge for every pair whose in-tree path is
     * more than {@code ratio}× the straight-line distance. Walks the tree once per
     * pair to compute the in-tree distance (O(N³), fine for village-scale N≤~20).
     */
    private static List<int[]> mstPlusShortcuts(List<Node> nodes, double ratio) {
        List<int[]> edges = new ArrayList<>(minimumSpanningTree(nodes));
        int n = nodes.size();

        // Adjacency from the MST so we can run a BFS for tree distance.
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) {
            adj.get(e[0]).add(e[1]);
            adj.get(e[1]).add(e[0]);
        }

        Set<Long> seen = new HashSet<>();
        for (int[] e : edges) seen.add(edgeKey(e[0], e[1]));

        for (int i = 0; i < n; i++) {
            double[] treeDist = bfsTreeDistance(nodes, adj, i);
            for (int j = i + 1; j < n; j++) {
                if (seen.contains(edgeKey(i, j))) continue;
                double straight = Math.sqrt(distSq(nodes.get(i), nodes.get(j)));
                if (straight < 1.0) continue;
                if (treeDist[j] / straight > ratio) {
                    edges.add(new int[]{ i, j });
                    seen.add(edgeKey(i, j));
                }
            }
        }
        return edges;
    }

    /** Sum-of-edge-weights distance from {@code source} to every other node along the MST. */
    private static double[] bfsTreeDistance(List<Node> nodes, List<List<Integer>> adj, int source) {
        int n = nodes.size();
        double[] dist = new double[n];
        java.util.Arrays.fill(dist, Double.POSITIVE_INFINITY);
        dist[source] = 0;
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
        queue.add(source);
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v : adj.get(u)) {
                if (Double.isFinite(dist[v])) continue;
                dist[v] = dist[u] + Math.sqrt(distSq(nodes.get(u), nodes.get(v)));
                queue.add(v);
            }
        }
        return dist;
    }

    /** Symmetric key for an unordered pair of node indices. */
    private static long edgeKey(int i, int j) {
        int lo = Math.min(i, j);
        int hi = Math.max(i, j);
        return ((long) lo << 32) | (hi & 0xFFFFFFFFL);
    }

    // ── Per-edge routing ───────────────────────────────────────────────────────

    /**
     * Routes one edge using A* in the XZ plane and converts grass cells along the
     * way. Y is resolved per-cell by a downward surface scan from the source door's Y.
     * Returns the number of blocks converted on this edge.
     */
    private static int routeBetween(World world, VillageData village,
                                     int x1, int y1, int z1, int x2, int y2, int z2,
                                     boolean allowFallback) {
        int manhattan = Math.abs(x1 - x2) + Math.abs(z1 - z2);
        if (manhattan == 0) return 0;
        if (manhattan > MAX_PATH_LENGTH) {
            LOGGER.info("routeBetween: SKIP — distance " + manhattan + " > " + MAX_PATH_LENGTH
                    + "  (" + x1 + "," + z1 + ") → (" + x2 + "," + z2 + ")");
            return 0;
        }

        List<long[]> path = aStar(world, x1, z1, y1, x2, z2, true);
        boolean respectedObstructions = true;
        if (path == null) {
            if (!allowFallback) {
                // Caller (typically connectExtra) prefers no road to a road that
                // tunnels under fences/trees. The same pair is reachable via the
                // base network anyway, so dropping this edge is fine.
                LOGGER.info("routeBetween: SKIP — A* couldn't avoid obstructions and fallback disabled  ("
                        + x1 + "," + z1 + ") → (" + x2 + "," + z2 + ")  manhattan=" + manhattan);
                return 0;
            }
            // Retry without obstruction filtering — the road will go through small
            // obstacles (a stray fence post, a tree) but at least we'll get connectivity.
            // Better an imperfect path than none.
            path = aStar(world, x1, z1, y1, x2, z2, false);
            respectedObstructions = false;
            if (path == null) {
                LOGGER.info("routeBetween: A* FAILED  (" + x1 + "," + y1 + "," + z1
                        + ") → (" + x2 + "," + y2 + "," + z2 + ")  manhattan=" + manhattan);
                return 0;
            }
            LOGGER.info("routeBetween: A* fell back to obstruction-free pass  ("
                    + x1 + "," + z1 + ") → (" + x2 + "," + z2 + ")  pathLen=" + path.size());
        }

        // A* now allows diagonal moves: a step (+1,+1) jumps past two grid-adjacent
        // cells, so two diagonal Soil_Pathway tiles only touch at a corner. Insert a
        // filler tile on each diagonal hop so the resulting road is edge-connected.
        path = densifyDiagonals(world, path, respectedObstructions);

        int placed = 0;
        int grassSeen = 0, nonGrassSeen = 0;
        Map<String, Integer> nonGrassBreakdown = new HashMap<>();
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
                String id = readBlockId(world, x, y, z);
                nonGrassBreakdown.merge(id, 1, Integer::sum);
            }
        }
        LOGGER.info("routeBetween OK: (" + x1 + "," + z1 + ") → (" + x2 + "," + z2
                + ") pathLen=" + path.size() + " placed=" + placed
                + " grass=" + grassSeen + " nonGrass=" + nonGrassSeen
                + (nonGrassSeen > 0 ? "  nonGrassTypes=" + nonGrassBreakdown : ""));
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
     * 2D A* on the XZ grid with 8-connected moves. Diagonal steps cost ~1.41× a
     * cardinal step (encoded as 14 vs 10 in integer cost) so the heuristic stays
     * admissible. For each visited cell we resolve the surface Y by scanning downward
     * from a hint Y. Cells whose surface jumps by more than 2 from the previous cell
     * are treated as unscalable.
     *
     * <p>To stop a diagonal from sneaking between two grid-adjacent obstacles
     * ("corner cutting" at fence corners), a diagonal move is only allowed if at
     * least one of the two cardinal neighbors that share a corner with the
     * destination is itself walkable.
     *
     * <p>Returns a list of {x, z, surfaceY} or null if no path was found. The
     * returned path may contain diagonal hops; callers that need a visually
     * 4-connected road must densify it (see {@code densifyDiagonals}).
     */
    private static List<long[]> aStar(World world, int sx, int sz, int hintY, int gx, int gz,
                                       boolean respectObstructions) {
        record Node(int x, int z, int y, double f) {}

        // Cardinal cost = 10, diagonal = 14 (≈ 10·√2). Keeps everything in ints.
        final int COST_CARDINAL = 10;
        final int COST_DIAGONAL = 14;
        final int[][] dirs = {
                {1,0},{-1,0},{0,1},{0,-1},
                {1,1},{1,-1},{-1,1},{-1,-1},
        };

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

            for (int[] d : dirs) {
                int nx = cur.x + d[0];
                int nz = cur.z + d[1];
                long nKey = packXZ(nx, nz);
                if (closed.contains(nKey)) continue;

                int neighborSurface = surfaceY(world, nx, nz, cur.y);
                if (neighborSurface == Integer.MIN_VALUE) continue;
                int slope = Math.abs(neighborSurface - cur.y);
                if (slope > 2) continue; // unscalable cliff

                // Reject the cell if a 2-block-tall NPC can't stand on it — fence, tree
                // trunk, wall, sign, etc. all leave a solid block above the surface. The
                // caller disables this check on the fallback retry to guarantee connectivity.
                if (respectObstructions && !hasHeadroom(world, nx, neighborSurface, nz)) continue;

                boolean diagonal = (d[0] != 0 && d[1] != 0);
                if (diagonal && respectObstructions) {
                    // No corner-cutting: the diagonal step is only legal if at least one
                    // of the two cardinal neighbors sharing the corner is itself walkable.
                    // Otherwise the road would slip diagonally between two fences/trees.
                    if (!cardinalSideOpen(world, cur.x + d[0], cur.z, cur.y)
                            && !cardinalSideOpen(world, cur.x, cur.z + d[1], cur.y)) {
                        continue;
                    }
                }

                int stepCost = (diagonal ? COST_DIAGONAL : COST_CARDINAL) + slope * 30;
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

    /**
     * True if the cardinal-neighbor cell at {@code (x, z)} has a walkable surface and
     * no obstruction on top — used by the diagonal corner-cutting guard. Slope is
     * checked against the diagonal source's surface so we don't sidestep a cliff.
     */
    private static boolean cardinalSideOpen(World world, int x, int z, int sourceY) {
        int y = surfaceY(world, x, z, sourceY);
        if (y == Integer.MIN_VALUE) return false;
        if (Math.abs(y - sourceY) > 2) return false;
        return hasHeadroom(world, x, y, z);
    }

    /**
     * Walks the path and, for every diagonal hop {@code (prev → curr)} where prev and
     * curr differ on both axes, inserts one cardinal filler tile between them so the
     * resulting Soil_Pathway tiles share an edge instead of just touching at a corner.
     *
     * <p>Filler placement: prefer the side that's already walkable (matches A*'s
     * corner-cutting guard); if neither is walkable but we already fell back to the
     * obstruction-free pass, just pick {@code (curr.x, prev.z)} — better an imperfect
     * filler than a visually broken road.
     */
    private static List<long[]> densifyDiagonals(World world, List<long[]> path,
                                                  boolean respectObstructions) {
        if (path.size() < 2) return path;
        List<long[]> out = new ArrayList<>();
        out.add(path.get(0));
        for (int i = 1; i < path.size(); i++) {
            long[] prev = path.get(i - 1);
            long[] curr = path.get(i);
            int px = (int) prev[0], pz = (int) prev[1], py = (int) prev[2];
            int cx = (int) curr[0], cz = (int) curr[1];
            boolean diagonal = (px != cx) && (pz != cz);
            if (diagonal) {
                // Two candidates for the filler tile — pick the one with a valid surface
                // and (if obstructions matter) no obstruction. Same rules A* used.
                int[] filler = pickDiagonalFiller(world, px, pz, cx, cz, py, respectObstructions);
                if (filler != null) {
                    out.add(new long[]{ filler[0], filler[1], filler[2] });
                }
            }
            out.add(curr);
        }
        return out;
    }

    /**
     * Returns {x, z, y} of a walkable cardinal neighbor that bridges the diagonal
     * step from (px, pz) to (cx, cz), or null if neither side qualifies. The two
     * candidates are (cx, pz) and (px, cz) — the corners of the implicit 2×2 box.
     */
    private static int[] pickDiagonalFiller(World world, int px, int pz, int cx, int cz,
                                             int hintY, boolean respectObstructions) {
        int[] best = null;
        for (int[] cand : new int[][]{{cx, pz}, {px, cz}}) {
            int y = surfaceY(world, cand[0], cand[1], hintY);
            if (y == Integer.MIN_VALUE) continue;
            if (Math.abs(y - hintY) > 2) continue;
            if (respectObstructions && !hasHeadroom(world, cand[0], y, cand[1])) continue;
            return new int[]{ cand[0], cand[1], y };
        }
        // On the obstruction-free fallback, accept any side with a valid surface even
        // if obstructed — visual continuity beats the missing tile.
        if (!respectObstructions) {
            for (int[] cand : new int[][]{{cx, pz}, {px, cz}}) {
                int y = surfaceY(world, cand[0], cand[1], hintY);
                if (y == Integer.MIN_VALUE) continue;
                if (Math.abs(y - hintY) > 2) continue;
                return new int[]{ cand[0], cand[1], y };
            }
        }
        return best;
    }

    /**
     * Octile distance heuristic in the same integer scale as A*'s step costs
     * (cardinal=10, diagonal=14). Admissible for 8-connected grids.
     */
    private static double heuristic(int x, int z, int gx, int gz) {
        int dx = Math.abs(x - gx);
        int dz = Math.abs(z - gz);
        int min = Math.min(dx, dz);
        int max = Math.max(dx, dz);
        return 14 * min + 10 * (max - min);
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
