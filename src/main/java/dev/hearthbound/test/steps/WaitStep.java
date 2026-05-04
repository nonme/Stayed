package dev.hearthbound.test.steps;

import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;

/**
 * Asks the runner to wait {@code ms} milliseconds before the next step. Used
 * after far-teleport to give the engine time to unload chunks.
 */
public final class WaitStep implements TestStep {

    private final long ms;

    public WaitStep(long ms) {
        this.ms = ms;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        ctx.getLogger().info("Wait " + ms + "ms");
        return StepResult.delay(ms);
    }

    @Override
    public String getName() { return "Wait(" + ms + "ms)"; }
}
