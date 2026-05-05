package dev.hearthbound.test.steps;

import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;

public final class SaveRestartSnapshotStep implements TestStep {

    @Override
    public StepResult execute(TestContext ctx) {
        return RestartSnapshotStore.save(ctx);
    }

    @Override
    public String getName() { return "SaveRestartSnapshot"; }
}
