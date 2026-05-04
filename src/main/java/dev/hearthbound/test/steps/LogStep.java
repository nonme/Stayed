package dev.hearthbound.test.steps;

import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;

/** Writes a line to the per-test log. Useful between phases. */
public final class LogStep implements TestStep {

    private final String message;

    public LogStep(String message) {
        this.message = message;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        ctx.getLogger().info(message);
        return StepResult.pass();
    }

    @Override
    public String getName() { return "Log"; }
}
