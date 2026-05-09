package dev.hearthbound.npc;

import java.util.List;

public final class GuardPatrolRouteTest {
    public static void main(String[] args) {
        lShapedRoadKeepsTheCorner();
        buildingFootprintsAreExcluded();
        smallRoadGapsAreBridgedIntoOnePatrolRoute();
        perimeterRoadsBeatDenseLocalCycles();
    }

    private static void lShapedRoadKeepsTheCorner() {
        GuardPatrolRoute.Route route = GuardPatrolRoute.build(
                List.of(
                        new GuardPatrolRoute.RoadPoint(0, 64, 0),
                        new GuardPatrolRoute.RoadPoint(1, 64, 0),
                        new GuardPatrolRoute.RoadPoint(2, 64, 0),
                        new GuardPatrolRoute.RoadPoint(2, 64, 1),
                        new GuardPatrolRoute.RoadPoint(2, 64, 2)
                ),
                List.of());

        assertEquals(GuardPatrolRoute.LoopMode.PING_PONG, route.loopMode(), "L-shaped open road should ping-pong");
        assertEquals(List.of(
                new GuardPatrolRoute.RoadPoint(0, 64, 0),
                new GuardPatrolRoute.RoadPoint(2, 64, 0),
                new GuardPatrolRoute.RoadPoint(2, 64, 2)
        ), route.waypoints(), "L-shaped road should compress to endpoint, corner, endpoint");
    }

    private static void buildingFootprintsAreExcluded() {
        GuardPatrolRoute.Route route = GuardPatrolRoute.build(
                List.of(
                        new GuardPatrolRoute.RoadPoint(0, 64, 0),
                        new GuardPatrolRoute.RoadPoint(1, 64, 0),
                        new GuardPatrolRoute.RoadPoint(2, 64, 0),
                        new GuardPatrolRoute.RoadPoint(3, 64, 0),
                        new GuardPatrolRoute.RoadPoint(4, 64, 0)
                ),
                List.of(new GuardPatrolRoute.BuildingFootprint(2, 0, 3, 0)));

        assertEquals(List.of(
                new GuardPatrolRoute.RoadPoint(0, 64, 0),
                new GuardPatrolRoute.RoadPoint(1, 64, 0)
        ), route.waypoints(), "route should use the largest safe component outside buildings");
    }

    private static void smallRoadGapsAreBridgedIntoOnePatrolRoute() {
        GuardPatrolRoute.Route route = GuardPatrolRoute.build(
                List.of(
                        new GuardPatrolRoute.RoadPoint(0, 64, 0),
                        new GuardPatrolRoute.RoadPoint(1, 64, 0),
                        new GuardPatrolRoute.RoadPoint(4, 64, 0),
                        new GuardPatrolRoute.RoadPoint(5, 64, 0),
                        new GuardPatrolRoute.RoadPoint(5, 64, 1),
                        new GuardPatrolRoute.RoadPoint(5, 64, 2)
                ),
                List.of());

        assertEquals(List.of(
                new GuardPatrolRoute.RoadPoint(0, 64, 0),
                new GuardPatrolRoute.RoadPoint(5, 64, 0),
                new GuardPatrolRoute.RoadPoint(5, 64, 2)
        ), route.waypoints(), "nearby road islands should be treated as one continuous patrol route");
    }

    private static void perimeterRoadsBeatDenseLocalCycles() {
        GuardPatrolRoute.Route route = GuardPatrolRoute.build(
                List.of(
                        new GuardPatrolRoute.RoadPoint(-12, 64, -12),
                        new GuardPatrolRoute.RoadPoint(0, 64, -14),
                        new GuardPatrolRoute.RoadPoint(12, 64, -12),
                        new GuardPatrolRoute.RoadPoint(14, 64, 0),
                        new GuardPatrolRoute.RoadPoint(12, 64, 12),
                        new GuardPatrolRoute.RoadPoint(0, 64, 14),
                        new GuardPatrolRoute.RoadPoint(-12, 64, 12),
                        new GuardPatrolRoute.RoadPoint(-14, 64, 0),
                        new GuardPatrolRoute.RoadPoint(-3, 64, -3),
                        new GuardPatrolRoute.RoadPoint(-2, 64, -3),
                        new GuardPatrolRoute.RoadPoint(-1, 64, -3),
                        new GuardPatrolRoute.RoadPoint(0, 64, -3),
                        new GuardPatrolRoute.RoadPoint(1, 64, -3),
                        new GuardPatrolRoute.RoadPoint(2, 64, -3),
                        new GuardPatrolRoute.RoadPoint(3, 64, -3),
                        new GuardPatrolRoute.RoadPoint(3, 64, -2),
                        new GuardPatrolRoute.RoadPoint(3, 64, -1),
                        new GuardPatrolRoute.RoadPoint(3, 64, 0),
                        new GuardPatrolRoute.RoadPoint(3, 64, 1),
                        new GuardPatrolRoute.RoadPoint(3, 64, 2),
                        new GuardPatrolRoute.RoadPoint(3, 64, 3),
                        new GuardPatrolRoute.RoadPoint(2, 64, 3),
                        new GuardPatrolRoute.RoadPoint(1, 64, 3),
                        new GuardPatrolRoute.RoadPoint(0, 64, 3),
                        new GuardPatrolRoute.RoadPoint(-1, 64, 3),
                        new GuardPatrolRoute.RoadPoint(-2, 64, 3),
                        new GuardPatrolRoute.RoadPoint(-3, 64, 3),
                        new GuardPatrolRoute.RoadPoint(-3, 64, 2),
                        new GuardPatrolRoute.RoadPoint(-3, 64, 1),
                        new GuardPatrolRoute.RoadPoint(-3, 64, 0),
                        new GuardPatrolRoute.RoadPoint(-3, 64, -1),
                        new GuardPatrolRoute.RoadPoint(-3, 64, -2)
                ),
                List.of(
                        new GuardPatrolRoute.BuildingFootprint(-9, -9, -5, -5),
                        new GuardPatrolRoute.BuildingFootprint(5, -9, 9, -5),
                        new GuardPatrolRoute.BuildingFootprint(-9, 5, -5, 9),
                        new GuardPatrolRoute.BuildingFootprint(5, 5, 9, 9)
                ));

        assertEquals(GuardPatrolRoute.LoopMode.LOOP, route.loopMode(), "perimeter roads should form a loop");
        assertEquals(List.of(
                new GuardPatrolRoute.RoadPoint(-12, 64, -12),
                new GuardPatrolRoute.RoadPoint(0, 64, -14),
                new GuardPatrolRoute.RoadPoint(12, 64, -12),
                new GuardPatrolRoute.RoadPoint(14, 64, 0),
                new GuardPatrolRoute.RoadPoint(12, 64, 12),
                new GuardPatrolRoute.RoadPoint(0, 64, 14),
                new GuardPatrolRoute.RoadPoint(-12, 64, 12),
                new GuardPatrolRoute.RoadPoint(-14, 64, 0)
        ), route.waypoints(), "guard patrol should prefer village perimeter anchors over a dense local cycle");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + "\nexpected: " + expected + "\nactual:   " + actual);
        }
    }
}
