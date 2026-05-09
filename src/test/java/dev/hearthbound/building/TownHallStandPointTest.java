package dev.hearthbound.building;

import dev.hearthbound.village.VillageData;

public final class TownHallStandPointTest {
    public static void main(String[] args) {
        standPointUsesTownHallLayoutCenter();
    }

    private static void standPointUsesTownHallLayoutCenter() {
        VillageData village = new VillageData();
        village.setStage(VillageData.STAGE_FOUNDED);
        village.setFoundingStonePos(100, 64, -40);
        village.setRotation(0);

        double[] stand = BuildingLayout.townHallStandPoint(village);
        BuildingLayout.Layout layout = BuildingLayout.get(dev.hearthbound.village.BuildingType.TOWN_HALL);

        assertEquals(100 + layout.centerLX() + 0.5, stand[0], "x should use Town Hall layout center");
        assertEquals(64 + layout.floorLY() + 1.0, stand[1], "y should use Town Hall floor");
        assertEquals(-40 + layout.centerLZ() + 0.5, stand[2], "z should use Town Hall layout center");
    }

    private static void assertEquals(double expected, double actual, String message) {
        if (Math.abs(expected - actual) > 0.0001) {
            throw new AssertionError(message + "\nexpected: " + expected + "\nactual:   " + actual);
        }
    }
}
