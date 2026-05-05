package dev.hearthbound.building;

import dev.hearthbound.village.BuildingType;

import java.util.random.RandomGenerator;

/**
 * Drop tables for resource-producing buildings.
 * Each building has a base spawn chance and a weighted item table.
 */
public final class ResourceProducer {

    // Chance per village tick (every 5s) that a building produces anything.
    // FARM is not on the table any more — it's driven by FarmerWorkBehavior reading the
    // actual planted crops on the plot, so the village only gets what the farmer harvests.
    public static final double SAWMILL_PRODUCE_CHANCE  = 0.25;
    public static final double MINE_PRODUCE_CHANCE     = 0.25;

    private record Drop(String itemId, double weight) {}

    private static final Drop[] SAWMILL_TABLE = {
        new Drop("Wood_Beech_Trunk", 0.50),
        new Drop("Wood_Oak_Trunk",   0.50),
    };

    private static final Drop[] MINE_TABLE = {
        new Drop("Rock_Stone_Cobble",  0.90),
        new Drop("Rock_Basalt_Cobble", 0.05),
        new Drop("Ore_Copper",         0.03),
        new Drop("Ore_Iron",           0.02),
    };

    private ResourceProducer() {}

    /**
     * Returns the item ID to produce this tick for the given building type,
     * or null if the roll fails or the building type doesn't produce anything.
     */
    public static String roll(String buildingType, RandomGenerator rng) {
        return switch (buildingType) {
            case BuildingType.SAWMILL  -> rollTable(SAWMILL_TABLE, SAWMILL_PRODUCE_CHANCE, rng);
            case BuildingType.MINE     -> rollTable(MINE_TABLE,    MINE_PRODUCE_CHANCE,    rng);
            // FARM intentionally absent — see FarmerWorkBehavior.
            default -> null;
        };
    }

    private static String rollTable(Drop[] table, double produceChance, RandomGenerator rng) {
        if (rng.nextDouble() >= produceChance) return null;

        double roll = rng.nextDouble();
        double cumulative = 0.0;
        for (Drop drop : table) {
            cumulative += drop.weight();
            if (roll < cumulative) return drop.itemId();
        }
        // Fallback to last entry (handles floating-point edge cases)
        return table[table.length - 1].itemId();
    }
}
