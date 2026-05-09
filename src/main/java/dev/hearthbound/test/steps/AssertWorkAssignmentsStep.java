package dev.hearthbound.test.steps;

import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Asserts that completed work buildings of the given types have assigned workers.
 */
public final class AssertWorkAssignmentsStep implements TestStep {

    private final Map<String, Integer> expectedAssignedByType;

    public AssertWorkAssignmentsStep(Map<String, Integer> expectedAssignedByType) {
        this.expectedAssignedByType = new LinkedHashMap<>(expectedAssignedByType);
    }

    @Override
    public StepResult execute(TestContext ctx) {
        VillageData village = VillageManager.get().getVillageData(
                ctx.getStore(), ctx.getPlayerRef());
        if (village == null) return StepResult.fail("no village");

        Map<String, Integer> actual = new LinkedHashMap<>();
        for (String type : expectedAssignedByType.keySet()) {
            actual.put(type, 0);
        }
        for (BuildingRecord building : village.getBuildings()) {
            if (!building.isCompleted()) continue;
            if (!actual.containsKey(building.getType())) continue;
            if (building.getAssignedVillagerId() == null) continue;
            actual.put(building.getType(), actual.get(building.getType()) + 1);
        }

        ctx.getLogger().info("WorkAssignments: actual=" + actual + " expected=" + expectedAssignedByType);
        for (Map.Entry<String, Integer> entry : expectedAssignedByType.entrySet()) {
            int actualCount = actual.getOrDefault(entry.getKey(), 0);
            if (actualCount != entry.getValue()) {
                return StepResult.fail(entry.getKey() + " assigned=" + actualCount
                        + " expected=" + entry.getValue());
            }
        }
        return StepResult.pass("work assigned=" + actual);
    }

    @Override
    public String getName() {
        return "AssertWorkAssignments";
    }
}
