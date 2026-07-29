package dev.hearthbound.village;

import java.util.UUID;

public final class VillageOfflineStateTest {
    public static void main(String[] args) {
        houseLookupUsesCompletedResidentialAssignments();
        abstractHungerMutatesSummariesWithoutEntities();
        farmersDoNotAccumulateAbstractHunger();
        abstractHungerRefreshesLegacyHouseMirror();
    }

    private static void houseLookupUsesCompletedResidentialAssignments() {
        VillageData village = new VillageData();
        UUID housed = UUID.randomUUID();
        UUID homeless = UUID.randomUUID();

        BuildingRecord incomplete = new BuildingRecord(BuildingType.HOUSE_HUMAN, 1, 2, 3);
        incomplete.setAssignedVillagerId(homeless);
        village.addBuilding(incomplete);

        BuildingRecord house = new BuildingRecord(BuildingType.HOUSE_HUMAN, 4, 5, 6);
        house.setCompleted(true);
        house.setAssignedVillagerId(housed);
        village.addBuilding(house);

        assertSame(house, VillageManager.findHouseOf(village, housed), "completed assigned house should be found");
        assertNull(VillageManager.findHouseOf(village, homeless), "incomplete assigned house should not count");
        assertTrue(VillageManager.hasHouse(village, housed), "housed villager should have house");
        assertFalse(VillageManager.hasHouse(village, homeless), "homeless villager should not have house");
    }

    private static void abstractHungerMutatesSummariesWithoutEntities() {
        VillageData village = new VillageData();
        VillagerSummary settler = new VillagerSummary();
        settler.setVillagerUuid(UUID.randomUUID());
        settler.setProfession(VillagerData.PROF_NONE);
        settler.setHunger(41);
        village.addVillager(settler);

        VillageManager.tickHungerAbstract(village);

        assertEquals(42, settler.getHunger(), "abstract hunger should increase summary hunger");
    }

    private static void farmersDoNotAccumulateAbstractHunger() {
        VillageData village = new VillageData();
        VillagerSummary farmer = new VillagerSummary();
        farmer.setVillagerUuid(UUID.randomUUID());
        farmer.setProfession(VillagerData.PROF_FARMER);
        farmer.setHunger(35);
        village.addVillager(farmer);

        VillageManager.tickHungerAbstract(village);

        assertEquals(0, farmer.getHunger(), "farmer hunger should reset in current gameplay model");
    }

    private static void abstractHungerRefreshesLegacyHouseMirror() {
        VillageData village = new VillageData();
        UUID uuid = UUID.randomUUID();
        VillagerSummary settler = new VillagerSummary();
        settler.setVillagerUuid(uuid);
        settler.setHasHouse(false);
        village.addVillager(settler);

        BuildingRecord house = new BuildingRecord(BuildingType.HOUSE_HUMAN, 1, 2, 3);
        house.setCompleted(true);
        house.setAssignedVillagerId(uuid);
        village.addBuilding(house);

        VillageManager.tickHungerAbstract(village);

        assertTrue(settler.isHasHouse(), "legacy hasHouse mirror should refresh from building assignments");
    }

    private static void assertSame(Object expected, Object actual, String message) {
        if (expected != actual) throw new AssertionError(message);
    }

    private static void assertNull(Object actual, String message) {
        if (actual != null) throw new AssertionError(message + "\nactual: " + actual);
    }

    private static void assertTrue(boolean actual, String message) {
        if (!actual) throw new AssertionError(message);
    }

    private static void assertFalse(boolean actual, String message) {
        if (actual) throw new AssertionError(message);
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + "\nexpected: " + expected + "\nactual:   " + actual);
        }
    }
}
