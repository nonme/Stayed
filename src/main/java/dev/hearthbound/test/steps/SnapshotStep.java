package dev.hearthbound.test.steps;

import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

/**
 * Captures a {@link RegistrySnapshot} of NpcRegistry + VillageData and stores
 * it in the test context under {@code "snapshot:" + name}. Pair with
 * {@link DiffStep} to verify that nothing changed (or only expected things
 * changed) across an interval.
 */
public final class SnapshotStep implements TestStep {

    public static final String KEY_PREFIX = "snapshot:";

    private final String name;

    public SnapshotStep(String name) {
        this.name = name;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        VillageData village = VillageManager.get().getVillageData(
                ctx.getStore(), ctx.getPlayerRef());
        RegistrySnapshot snap = RegistrySnapshot.capture(village);
        ctx.put(KEY_PREFIX + name, snap);
        ctx.getLogger().info("Snapshot[" + name + "]: " + snap.npcsByNpcId.size()
                + " npcs, " + snap.buildingsByKey.size() + " buildings");
        return StepResult.pass();
    }

    @Override
    public String getName() { return "Snapshot(" + name + ")"; }
}
