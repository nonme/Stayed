package dev.hearthbound.village;

import dev.hearthbound.building.WarehouseDepositor;

import java.util.Random;

public final class VillageProductionTest {
    public static void main(String[] args) {
        abstractDepositAddsToWarehouseStorage();
        abstractFoodWithdrawConsumesOneStoredFood();
        abstractFoodWithdrawReturnsNullWhenNoFoodExists();
    }

    private static void abstractDepositAddsToWarehouseStorage() {
        BuildingRecord warehouse = new BuildingRecord(BuildingType.WAREHOUSE, 1, 2, 3);

        boolean deposited = WarehouseDepositor.depositAbstract(warehouse, "Wood_Oak_Trunk");

        assertTrue(deposited, "valid abstract deposit should succeed");
        assertEquals(1, warehouse.getResourceCount("Wood_Oak_Trunk"), "warehouse storage should receive produced item");
    }

    private static void abstractFoodWithdrawConsumesOneStoredFood() {
        BuildingRecord warehouse = new BuildingRecord(BuildingType.WAREHOUSE, 1, 2, 3);
        warehouse.addResource("Plant_Crop_Carrot_Item", 2);

        String food = WarehouseDepositor.withdrawRandomFoodAbstract(warehouse, new Random(0));

        assertEquals("Plant_Crop_Carrot_Item", food, "stored food item should be withdrawn");
        assertEquals(1, warehouse.getResourceCount("Plant_Crop_Carrot_Item"), "withdraw should consume exactly one item");
    }

    private static void abstractFoodWithdrawReturnsNullWhenNoFoodExists() {
        BuildingRecord warehouse = new BuildingRecord(BuildingType.WAREHOUSE, 1, 2, 3);
        warehouse.addResource("Rock_Stone_Cobble", 5);

        String food = WarehouseDepositor.withdrawRandomFoodAbstract(warehouse, new Random(0));

        assertEquals(null, food, "non-food storage should not satisfy food withdrawal");
        assertEquals(5, warehouse.getResourceCount("Rock_Stone_Cobble"), "non-food resources should be untouched");
    }

    private static void assertTrue(boolean actual, String message) {
        if (!actual) throw new AssertionError(message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + "\nexpected: " + expected + "\nactual:   " + actual);
        }
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + "\nexpected: " + expected + "\nactual:   " + actual);
        }
    }
}
