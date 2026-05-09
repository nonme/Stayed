package dev.hearthbound.npc;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure route planner for guard patrols over the village road graph.
 */
public final class GuardPatrolRoute {
    private static final int MAX_STRAIGHT_SEGMENT = 8;
    private static final int MAX_BRIDGE_GAP = 3;
    private static final int PERIMETER_SECTORS = 16;
    private static final int MIN_PERIMETER_WAYPOINTS = 6;

    private GuardPatrolRoute() {}

    public enum LoopMode {
        LOOP,
        PING_PONG
    }

    public record RoadPoint(int x, int y, int z) {}

    public record BuildingFootprint(int minX, int minZ, int maxX, int maxZ) {
        boolean contains(RoadPoint point) {
            return point.x >= minX && point.x <= maxX && point.z >= minZ && point.z <= maxZ;
        }
    }

    public record Route(List<RoadPoint> waypoints, LoopMode loopMode) {
        public boolean isUsable() {
            return waypoints.size() >= 2;
        }
    }

    public static Route build(List<RoadPoint> roadPoints, List<BuildingFootprint> buildingFootprints) {
        if (roadPoints == null || roadPoints.isEmpty()) {
            return new Route(List.of(), LoopMode.PING_PONG);
        }

        Map<Key, RoadPoint> safePoints = collectSafePoints(roadPoints, buildingFootprints);
        if (safePoints.size() < 2) {
            return new Route(List.copyOf(safePoints.values()), LoopMode.PING_PONG);
        }

        Map<Key, List<Key>> graph = buildGraph(safePoints);
        bridgeSmallGaps(graph, buildingFootprints == null ? List.of() : buildingFootprints);
        Route perimeterRoute = buildPerimeterRoute(safePoints, graph, buildingFootprints);
        if (perimeterRoute.isUsable()) {
            return perimeterRoute;
        }

        Set<Key> component = largestComponent(graph);
        if (component.size() < 2) {
            return new Route(component.stream().map(safePoints::get).toList(), LoopMode.PING_PONG);
        }

        boolean loop = component.stream().allMatch(k -> graph.getOrDefault(k, List.of()).size() == 2);
        List<Key> rawRoute = loop ? orderedCycle(component, graph) : edgeCoverWalk(component, graph);
        List<RoadPoint> waypoints = compress(rawRoute.stream().map(safePoints::get).toList());

        return new Route(waypoints, loop ? LoopMode.LOOP : LoopMode.PING_PONG);
    }

    private static Route buildPerimeterRoute(Map<Key, RoadPoint> roadPoints,
                                             Map<Key, List<Key>> graph,
                                             List<BuildingFootprint> buildingFootprints) {
        List<BuildingFootprint> footprints = buildingFootprints == null ? List.of() : buildingFootprints;
        if (footprints.isEmpty()) {
            return new Route(List.of(), LoopMode.PING_PONG);
        }

        Bounds bounds = buildingBounds(footprints);
        double centerX = (bounds.minX + bounds.maxX) / 2.0;
        double centerZ = (bounds.minZ + bounds.maxZ) / 2.0;
        double minRadius = Math.max(5.0, Math.max(bounds.maxX - bounds.minX, bounds.maxZ - bounds.minZ) * 0.45);

        Map<Integer, RoadPoint> sectorPoints = new HashMap<>();
        Map<Integer, Key> sectorKeys = new HashMap<>();
        Map<Integer, Double> sectorScores = new HashMap<>();
        for (Map.Entry<Key, RoadPoint> entry : roadPoints.entrySet()) {
            RoadPoint point = entry.getValue();
            double dx = point.x - centerX;
            double dz = point.z - centerZ;
            double distance = Math.hypot(dx, dz);
            if (distance < minRadius) {
                continue;
            }

            int sector = sector(dx, dz);
            Double previousScore = sectorScores.get(sector);
            if (previousScore == null || distance > previousScore) {
                sectorPoints.put(sector, point);
                sectorKeys.put(sector, entry.getKey());
                sectorScores.put(sector, distance);
            }
        }

        if (sectorPoints.size() < MIN_PERIMETER_WAYPOINTS) {
            return new Route(List.of(), LoopMode.PING_PONG);
        }

        List<Key> anchors = new ArrayList<>(sectorKeys.values());
        anchors.sort(Comparator
                .comparingDouble((Key k) -> Math.atan2(k.z - centerZ, k.x - centerX))
                .thenComparingInt(Key::x)
                .thenComparingInt(Key::z));

        List<Key> rawRoute = new ArrayList<>();
        for (int i = 0; i < anchors.size(); i++) {
            Key from = anchors.get(i);
            Key to = anchors.get((i + 1) % anchors.size());
            List<Key> segment = shortestPath(from, to, graph);
            if (segment.isEmpty()) {
                segment = List.of(from, to);
            }
            appendSegment(rawRoute, segment);
        }
        if (rawRoute.size() > 1 && rawRoute.get(0).equals(rawRoute.get(rawRoute.size() - 1))) {
            rawRoute.remove(rawRoute.size() - 1);
        }

        List<RoadPoint> waypoints = compress(rawRoute.stream()
                .map(roadPoints::get)
                .filter(p -> p != null)
                .toList());
        return new Route(waypoints, LoopMode.LOOP);
    }

    private static List<Key> shortestPath(Key from, Key to, Map<Key, List<Key>> graph) {
        ArrayDeque<Key> queue = new ArrayDeque<>();
        Map<Key, Key> previous = new HashMap<>();
        queue.add(from);
        previous.put(from, null);
        while (!queue.isEmpty()) {
            Key current = queue.removeFirst();
            if (current.equals(to)) {
                return reconstructPath(to, previous);
            }
            for (Key next : graph.getOrDefault(current, List.of())) {
                if (!previous.containsKey(next)) {
                    previous.put(next, current);
                    queue.addLast(next);
                }
            }
        }
        return List.of();
    }

    private static List<Key> reconstructPath(Key end, Map<Key, Key> previous) {
        List<Key> path = new ArrayList<>();
        Key current = end;
        while (current != null) {
            path.add(current);
            current = previous.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    private static void appendSegment(List<Key> route, List<Key> segment) {
        for (Key key : segment) {
            if (route.isEmpty() || !route.get(route.size() - 1).equals(key)) {
                route.add(key);
            }
        }
    }

    private static Bounds buildingBounds(List<BuildingFootprint> footprints) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BuildingFootprint footprint : footprints) {
            if (footprint == null) {
                continue;
            }
            minX = Math.min(minX, footprint.minX);
            minZ = Math.min(minZ, footprint.minZ);
            maxX = Math.max(maxX, footprint.maxX);
            maxZ = Math.max(maxZ, footprint.maxZ);
        }
        return new Bounds(minX, minZ, maxX, maxZ);
    }

    private static int sector(double dx, double dz) {
        double angle = Math.atan2(dz, dx);
        double normalized = (angle + Math.PI) / (Math.PI * 2.0);
        return Math.min(PERIMETER_SECTORS - 1, (int) Math.floor(normalized * PERIMETER_SECTORS));
    }

    private static Map<Key, RoadPoint> collectSafePoints(List<RoadPoint> roadPoints,
                                                         List<BuildingFootprint> buildingFootprints) {
        Map<Key, RoadPoint> points = new HashMap<>();
        List<BuildingFootprint> footprints = buildingFootprints == null ? List.of() : buildingFootprints;
        for (RoadPoint point : roadPoints) {
            if (point == null || insideAnyFootprint(point, footprints)) {
                continue;
            }
            points.putIfAbsent(new Key(point.x, point.z), point);
        }
        return points;
    }

    private static boolean insideAnyFootprint(RoadPoint point, List<BuildingFootprint> footprints) {
        for (BuildingFootprint footprint : footprints) {
            if (footprint != null && footprint.contains(point)) {
                return true;
            }
        }
        return false;
    }

    private static Map<Key, List<Key>> buildGraph(Map<Key, RoadPoint> points) {
        Map<Key, List<Key>> graph = new HashMap<>();
        for (Key key : points.keySet()) {
            List<Key> neighbors = new ArrayList<>(4);
            addNeighbor(points, neighbors, key.x + 1, key.z);
            addNeighbor(points, neighbors, key.x - 1, key.z);
            addNeighbor(points, neighbors, key.x, key.z + 1);
            addNeighbor(points, neighbors, key.x, key.z - 1);
            neighbors.sort(Key.ORDER);
            graph.put(key, neighbors);
        }
        return graph;
    }

    private static void bridgeSmallGaps(Map<Key, List<Key>> graph, List<BuildingFootprint> footprints) {
        boolean changed;
        do {
            changed = false;
            List<Set<Key>> components = components(graph);
            Bridge best = null;
            for (int i = 0; i < components.size(); i++) {
                for (int j = i + 1; j < components.size(); j++) {
                    Bridge candidate = nearestBridge(components.get(i), components.get(j));
                    if (candidate == null || candidate.distance > MAX_BRIDGE_GAP) {
                        continue;
                    }
                    if (bridgeCrossesFootprint(candidate.a, candidate.b, footprints)) {
                        continue;
                    }
                    if (best == null || candidate.distance < best.distance) {
                        best = candidate;
                    }
                }
            }
            if (best != null) {
                addUndirectedEdge(graph, best.a, best.b);
                changed = true;
            }
        } while (changed);
    }

    private static boolean bridgeCrossesFootprint(Key a, Key b, List<BuildingFootprint> footprints) {
        int minX = Math.min(a.x, b.x);
        int maxX = Math.max(a.x, b.x);
        int minZ = Math.min(a.z, b.z);
        int maxZ = Math.max(a.z, b.z);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                RoadPoint point = new RoadPoint(x, 0, z);
                for (BuildingFootprint footprint : footprints) {
                    if (footprint != null && footprint.contains(point)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static List<Set<Key>> components(Map<Key, List<Key>> graph) {
        Set<Key> seen = new HashSet<>();
        List<Set<Key>> components = new ArrayList<>();
        for (Key start : sortedKeys(graph.keySet())) {
            if (seen.contains(start)) {
                continue;
            }
            Set<Key> component = new LinkedHashSet<>();
            ArrayDeque<Key> queue = new ArrayDeque<>();
            queue.add(start);
            seen.add(start);
            while (!queue.isEmpty()) {
                Key key = queue.removeFirst();
                component.add(key);
                for (Key next : graph.getOrDefault(key, List.of())) {
                    if (seen.add(next)) {
                        queue.addLast(next);
                    }
                }
            }
            components.add(component);
        }
        return components;
    }

    private static Bridge nearestBridge(Set<Key> first, Set<Key> second) {
        Bridge best = null;
        for (Key a : first) {
            for (Key b : second) {
                int distance = chebyshev(a, b);
                if (best == null || distance < best.distance) {
                    best = new Bridge(a, b, distance);
                }
            }
        }
        return best;
    }

    private static void addUndirectedEdge(Map<Key, List<Key>> graph, Key a, Key b) {
        addEdge(graph, a, b);
        addEdge(graph, b, a);
    }

    private static void addEdge(Map<Key, List<Key>> graph, Key from, Key to) {
        List<Key> neighbors = graph.computeIfAbsent(from, k -> new ArrayList<>());
        if (!neighbors.contains(to)) {
            neighbors.add(to);
            neighbors.sort(Key.ORDER);
        }
    }

    private static int chebyshev(Key a, Key b) {
        return Math.max(Math.abs(a.x - b.x), Math.abs(a.z - b.z));
    }

    private static void addNeighbor(Map<Key, RoadPoint> points, List<Key> neighbors, int x, int z) {
        Key candidate = new Key(x, z);
        if (points.containsKey(candidate)) {
            neighbors.add(candidate);
        }
    }

    private static Set<Key> largestComponent(Map<Key, List<Key>> graph) {
        Set<Key> seen = new HashSet<>();
        Set<Key> best = Set.of();
        for (Key start : sortedKeys(graph.keySet())) {
            if (seen.contains(start)) {
                continue;
            }
            Set<Key> component = new LinkedHashSet<>();
            ArrayDeque<Key> queue = new ArrayDeque<>();
            queue.add(start);
            seen.add(start);
            while (!queue.isEmpty()) {
                Key key = queue.removeFirst();
                component.add(key);
                for (Key next : graph.getOrDefault(key, List.of())) {
                    if (seen.add(next)) {
                        queue.addLast(next);
                    }
                }
            }
            if (component.size() > best.size()) {
                best = component;
            }
        }
        return best;
    }

    private static List<Key> edgeCoverWalk(Set<Key> component, Map<Key, List<Key>> graph) {
        Key start = chooseStart(component, graph);
        List<Key> out = new ArrayList<>();
        Set<Edge> visitedEdges = new HashSet<>();
        walk(start, null, component, graph, visitedEdges, out);
        return out;
    }

    private static void walk(Key current, Key parent, Set<Key> component, Map<Key, List<Key>> graph,
                             Set<Edge> visitedEdges, List<Key> out) {
        out.add(current);
        for (Key next : graph.getOrDefault(current, List.of())) {
            if (!component.contains(next)) {
                continue;
            }
            Edge edge = new Edge(current, next);
            if (!visitedEdges.add(edge)) {
                continue;
            }
            walk(next, current, component, graph, visitedEdges, out);
            if (hasUnvisitedEdge(current, component, graph, visitedEdges)) {
                out.add(current);
            }
        }
    }

    private static boolean hasUnvisitedEdge(Key current, Set<Key> component, Map<Key, List<Key>> graph,
                                            Set<Edge> visitedEdges) {
        for (Key next : graph.getOrDefault(current, List.of())) {
            if (component.contains(next) && !visitedEdges.contains(new Edge(current, next))) {
                return true;
            }
        }
        return false;
    }

    private static Key chooseStart(Set<Key> component, Map<Key, List<Key>> graph) {
        return sortedKeys(component).stream()
                .filter(k -> graph.getOrDefault(k, List.of()).size() <= 1)
                .findFirst()
                .orElseGet(() -> sortedKeys(component).get(0));
    }

    private static List<Key> orderedCycle(Set<Key> component, Map<Key, List<Key>> graph) {
        Key start = sortedKeys(component).get(0);
        List<Key> out = new ArrayList<>();
        Set<Key> seen = new HashSet<>();
        Key previous = null;
        Key current = start;
        while (current != null && seen.add(current)) {
            out.add(current);
            Key prior = previous;
            previous = current;
            current = graph.getOrDefault(current, List.of()).stream()
                    .filter(component::contains)
                    .filter(k -> !k.equals(prior))
                    .findFirst()
                    .orElse(null);
        }
        return out;
    }

    private static List<RoadPoint> compress(List<RoadPoint> raw) {
        if (raw.size() <= 2) {
            return List.copyOf(raw);
        }

        List<RoadPoint> out = new ArrayList<>();
        out.add(raw.get(0));
        RoadPoint lastKept = raw.get(0);
        for (int i = 1; i < raw.size() - 1; i++) {
            RoadPoint prev = raw.get(i - 1);
            RoadPoint current = raw.get(i);
            RoadPoint next = raw.get(i + 1);
            if (isTurn(prev, current, next) || manhattan(lastKept, current) >= MAX_STRAIGHT_SEGMENT) {
                out.add(current);
                lastKept = current;
            }
        }
        RoadPoint last = raw.get(raw.size() - 1);
        if (!out.get(out.size() - 1).equals(last)) {
            out.add(last);
        }
        return List.copyOf(out);
    }

    private static boolean isTurn(RoadPoint previous, RoadPoint current, RoadPoint next) {
        int dx1 = Integer.compare(current.x - previous.x, 0);
        int dz1 = Integer.compare(current.z - previous.z, 0);
        int dx2 = Integer.compare(next.x - current.x, 0);
        int dz2 = Integer.compare(next.z - current.z, 0);
        return dx1 != dx2 || dz1 != dz2;
    }

    private static int manhattan(RoadPoint a, RoadPoint b) {
        return Math.abs(a.x - b.x) + Math.abs(a.z - b.z);
    }

    private static List<Key> sortedKeys(Set<Key> keys) {
        List<Key> sorted = new ArrayList<>(keys);
        sorted.sort(Key.ORDER);
        return sorted;
    }

    private record Key(int x, int z) {
        static final Comparator<Key> ORDER = Comparator.comparingInt(Key::x).thenComparingInt(Key::z);
    }

    private record Edge(Key a, Key b) {
        Edge {
            if (Key.ORDER.compare(a, b) > 0) {
                Key tmp = a;
                a = b;
                b = tmp;
            }
        }
    }

    private record Bridge(Key a, Key b, int distance) {}

    private record Bounds(int minX, int minZ, int maxX, int maxZ) {}
}
