package dev.hearthbound.test.engine;

/**
 * One step in a test scenario. Executes synchronously on the world thread.
 *
 * Implementations must be pure-ish: they may mutate the world via the context,
 * but they must not block the world thread for more than a frame. Use
 * {@link StepResult#delay(long)} to ask the runner for a longer delay before
 * the next step (e.g. wait for chunk unloads).
 */
@FunctionalInterface
public interface TestStep {

    StepResult execute(TestContext context);

    /** Used in the test report to identify the step. Default is the class simple name. */
    default String getName() {
        return getClass().getSimpleName();
    }
}
