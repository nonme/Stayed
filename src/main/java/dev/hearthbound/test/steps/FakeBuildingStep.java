package dev.hearthbound.test.steps;

import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

/**
 * Adds a fully-completed BuildingRecord directly to the player's VillageData
 * — no construction pipeline, no resources consumed, no actual blocks placed.
 *
 * Used by housing/roles tests that need a populated village without paying
 * the real construction time. Each created BuildingRecord is tagged with the
 * current test name via {@link BuildingRecord#setTestMarker} so
 * {@link CleanupTestNpcsStep} removes test buildings via {@link dev.hearthbound.test.engine.TestCleanup}.
 */
public final class FakeBuildingStep implements TestStep {

    private final String type;
    private final int x;
    private final int y;
    private final int z;

    public FakeBuildingStep(String type, int x, int y, int z) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        VillageData village = VillageManager.get().getVillageData(
                ctx.getStore(), ctx.getPlayerRef());
        if (village == null || !village.isFounded()) {
            return StepResult.fail("village not founded");
        }

        BuildingRecord record = new BuildingRecord(type, x, y, z);
        record.setBuildProgress(100);
        record.setCompleted(true);
        record.setTestMarker(ctx.getTestName());
        VillageManager.get().addBuilding(ctx.getStore(), ctx.getPlayerRef(), record);

        ctx.getLogger().info("FakeBuilding: " + type + " at " + x + "," + y + "," + z);
        return StepResult.pass(type);
    }

    @Override
    public String getName() { return "FakeBuilding(" + type + ")"; }
}
