package dev.hearthbound.test.packages;

import dev.hearthbound.test.engine.TestCase;
import dev.hearthbound.test.engine.TestPackage;
import dev.hearthbound.test.steps.AssertHousingStep;
import dev.hearthbound.test.steps.AuditStep;
import dev.hearthbound.test.steps.CleanupTestNpcsStep;
import dev.hearthbound.test.steps.FakeBuildingStep;
import dev.hearthbound.test.steps.ForceVillageTickStep;
import dev.hearthbound.test.steps.LogStep;
import dev.hearthbound.test.steps.SetupVillageStep;
import dev.hearthbound.test.steps.SnapshotStep;
import dev.hearthbound.test.steps.SpawnVillagersStep;
import dev.hearthbound.test.steps.TeleportPlayerFarStep;
import dev.hearthbound.test.steps.TeleportPlayerHomeStep;
import dev.hearthbound.test.steps.WaitStep;
import dev.hearthbound.test.engine.TestStep;
import dev.hearthbound.village.BuildingType;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage 2 — housing. Validates that VillageTickHandler's housing/work assignment
 * loop correctly pairs villagers with completed residential / work buildings.
 *
 * Each case attaches a teardown that runs unconditionally — pass, fail, or
 * abort — so the next case always starts with no test villagers and no test
 * buildings in the village. Buildings persist on the player's village normally,
 * but FakeBuildingStep tags them with a testMarker so cleanup can find them.
 */
public final class HousingPackage {

    private HousingPackage() {}

    /** All fake-building offsets are applied relative to the founding stone position. */
    private static final int FB_X = 100;
    private static final int FB_Y = 64;
    private static final int FB_Z = 100;

    public static TestPackage build() {
        return new TestPackage("housing", List.of(
                buildHousingBasic(),
                buildHousingMany(),
                buildHousingOverflow(),
                buildHousingWork(),
                buildHousingAfterChunkReload()));
    }

    private static TestCase buildHousingBasic() {
        return TestCase.of("housing_basic",
                new LogStep("housing_basic — 1 house, 1 villager, expect 1 housed"),
                new SetupVillageStep(),
                new FakeBuildingStep(BuildingType.HOUSE_HUMAN, FB_X, FB_Y, FB_Z),
                new SpawnVillagersStep(1),
                new ForceVillageTickStep(3),
                new AssertHousingStep(1, 0),
                new AuditStep(true, "post-assign"))
                .withTeardown(
                        new CleanupTestNpcsStep(),
                        new AuditStep(true, "post-cleanup"));
    }

    private static TestCase buildHousingMany() {
        List<TestStep> steps = new ArrayList<>();
        steps.add(new LogStep("housing_many — 30 houses, 30 villagers, expect 30 housed"));
        steps.add(new SetupVillageStep());
        for (int i = 0; i < 30; i++) {
            steps.add(new FakeBuildingStep(
                    BuildingType.HOUSE_HUMAN, FB_X + (i % 10) * 8, FB_Y, FB_Z + (i / 10) * 8));
        }
        steps.add(new SpawnVillagersStep(30));
        steps.add(new ForceVillageTickStep(10));
        steps.add(new AssertHousingStep(30, 0));
        steps.add(new AuditStep(true, "post-assign"));
        return new TestCase("housing_many", steps).withTeardown(
                new CleanupTestNpcsStep(),
                new AuditStep(true, "post-cleanup"));
    }

    private static TestCase buildHousingOverflow() {
        List<TestStep> steps = new ArrayList<>();
        steps.add(new LogStep("housing_overflow — 5 houses, 20 villagers, expect 5 housed + 15 homeless"));
        steps.add(new SetupVillageStep());
        for (int i = 0; i < 5; i++) {
            steps.add(new FakeBuildingStep(
                    BuildingType.HOUSE_HUMAN, FB_X + i * 8, FB_Y, FB_Z));
        }
        steps.add(new SpawnVillagersStep(20));
        steps.add(new ForceVillageTickStep(10));
        steps.add(new AssertHousingStep(5, 15));
        steps.add(new AuditStep(true, "post-assign"));
        return new TestCase("housing_overflow", steps).withTeardown(
                new CleanupTestNpcsStep(),
                new AuditStep(true, "post-cleanup"));
    }

    private static TestCase buildHousingWork() {
        // 10 houses + 10 villagers + 1 farm + 1 sawmill + 1 mine.
        // We don't assert work assignment counts here (the role-assignment logic
        // is tested separately in roles_*); this case validates the housing
        // pass tolerates a populated village with mixed building types.
        List<TestStep> steps = new ArrayList<>();
        steps.add(new LogStep("housing_work — 10 houses + workplaces, expect 10 housed"));
        steps.add(new SetupVillageStep());
        for (int i = 0; i < 10; i++) {
            steps.add(new FakeBuildingStep(
                    BuildingType.HOUSE_HUMAN, FB_X + i * 8, FB_Y, FB_Z));
        }
        steps.add(new FakeBuildingStep(BuildingType.FARM,    FB_X,      FB_Y, FB_Z + 16));
        steps.add(new FakeBuildingStep(BuildingType.SAWMILL, FB_X + 16, FB_Y, FB_Z + 16));
        steps.add(new FakeBuildingStep(BuildingType.MINE,    FB_X + 32, FB_Y, FB_Z + 16));
        steps.add(new SpawnVillagersStep(10));
        steps.add(new ForceVillageTickStep(10));
        steps.add(new AssertHousingStep(10, 0));
        steps.add(new AuditStep(true, "post-assign"));
        return new TestCase("housing_work", steps).withTeardown(
                new CleanupTestNpcsStep(),
                new AuditStep(true, "post-cleanup"));
    }

    private static TestCase buildHousingAfterChunkReload() {
        return TestCase.of("housing_after_chunk_reload",
                new LogStep("housing_after_chunk_reload — assign, far-tp, return, assignments preserved"),
                new SetupVillageStep(),
                new FakeBuildingStep(BuildingType.HOUSE_HUMAN, FB_X,     FB_Y, FB_Z),
                new FakeBuildingStep(BuildingType.HOUSE_HUMAN, FB_X + 8, FB_Y, FB_Z),
                new FakeBuildingStep(BuildingType.HOUSE_HUMAN, FB_X + 16, FB_Y, FB_Z),
                new SpawnVillagersStep(3),
                new ForceVillageTickStep(5),
                new AssertHousingStep(3, 0),
                new SnapshotStep("before-tp"),
                new TeleportPlayerFarStep(),
                new WaitStep(60_000),
                new TeleportPlayerHomeStep(),
                new WaitStep(10_000),
                new SnapshotStep("after-tp"),
                new AssertHousingStep(3, 0),
                new AuditStep(true, "post-reload"))
                .withTeardown(
                        new CleanupTestNpcsStep(),
                        new AuditStep(true, "post-cleanup"));
    }
}
