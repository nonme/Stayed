package dev.hearthbound.test.steps;

import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Asserts that the village has exactly {@code expectedHoused} houses with an
 * {@code assignedVillagerId} and {@code expectedHomelessVillagers} villager
 * summaries that no house is pointing at.
 *
 * Each villager UUID must appear in at most one house — duplicate assignment
 * is reported as a separate failure (split assignment is the worst housing
 * bug, so we surface it loudly rather than letting the count check hide it).
 */
public final class AssertHousingStep implements TestStep {

    private final int expectedHoused;
    private final int expectedHomelessVillagers;

    public AssertHousingStep(int expectedHoused, int expectedHomelessVillagers) {
        this.expectedHoused = expectedHoused;
        this.expectedHomelessVillagers = expectedHomelessVillagers;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        VillageData village = VillageManager.get().getVillageData(
                ctx.getStore(), ctx.getPlayerRef());
        if (village == null) return StepResult.fail("no village");

        int housed = 0;
        Set<UUID> assignedSeen = new HashSet<>();
        Set<UUID> duplicates = new HashSet<>();
        @SuppressWarnings("unchecked")
        List<UUID> spawned = (List<UUID>) ctx.get("spawnedNpcs");
        Set<UUID> scopedVillagers = spawned != null
                ? new HashSet<>(spawned)
                : Set.of();
        boolean scopedToCurrentTest = !scopedVillagers.isEmpty();

        for (BuildingRecord b : village.getBuildings()) {
            if (!isResidential(b.getType())) continue;
            if (!b.isCompleted()) continue;
            if (scopedToCurrentTest && !ctx.getTestName().equals(b.getTestMarker())) continue;
            UUID id = b.getAssignedVillagerId();
            if (id == null) continue;
            if (scopedToCurrentTest && !scopedVillagers.contains(id)) continue;
            housed++;
            if (!assignedSeen.add(id)) duplicates.add(id);
        }

        int homeless = 0;
        if (scopedToCurrentTest) {
            for (UUID id : scopedVillagers) {
                if (id != null && !assignedSeen.contains(id)) homeless++;
            }
        } else {
            for (var summary : village.getVillagers()) {
                UUID id = summary.getVillagerUuid();
                if (id == null) continue;
                if (!assignedSeen.contains(id)) homeless++;
            }
        }

        ctx.getLogger().info("Housing: housed=" + housed
                + " homeless=" + homeless
                + " duplicates=" + duplicates.size()
                + (scopedToCurrentTest ? " scope=current-test" : " scope=village"));

        if (!duplicates.isEmpty()) {
            return StepResult.fail("split assignment: " + duplicates.size()
                    + " villagers assigned to multiple houses");
        }
        if (housed != expectedHoused) {
            return StepResult.fail("housed=" + housed
                    + " expected=" + expectedHoused);
        }
        if (homeless != expectedHomelessVillagers) {
            return StepResult.fail("homeless=" + homeless
                    + " expected=" + expectedHomelessVillagers);
        }
        return StepResult.pass("housed=" + housed + " homeless=" + homeless);
    }

    private static boolean isResidential(String type) {
        return BuildingType.HOUSE_HUMAN.equals(type)
                || BuildingType.HOUSE_KWEEBEC.equals(type)
                || BuildingType.HOUSE_TRORK.equals(type);
    }

    @Override
    public String getName() {
        return "AssertHousing(housed=" + expectedHoused
                + ", homeless=" + expectedHomelessVillagers + ")";
    }
}
