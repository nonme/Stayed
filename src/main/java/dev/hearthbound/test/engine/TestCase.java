package dev.hearthbound.test.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * One named test case: a sequence of {@link TestStep}s plus an optional
 * teardown sequence that runs unconditionally afterwards.
 *
 * <ul>
 *   <li>{@link #getSteps()} — the test logic. The case fails as soon as any
 *       step returns {@link StepResult#fail(String)}.</li>
 *   <li>{@link #getTeardown()} — cleanup steps. The runner executes these
 *       <em>always</em> — after a passing run, after a failed step, and after
 *       an abort. A teardown step's failure is logged, teardown continues, and
 *       the final case verdict is failed.</li>
 * </ul>
 *
 * Use {@link #withTeardown(TestStep...)} on the builder side to attach
 * cleanup. Tests that don't need teardown can use {@link #of(String, TestStep...)}.
 */
public final class TestCase {

    private final String name;
    private final List<TestStep> steps;
    private final List<TestStep> teardown;

    public TestCase(String name, List<TestStep> steps) {
        this(name, steps, List.of());
    }

    public TestCase(String name, List<TestStep> steps, List<TestStep> teardown) {
        this.name = name;
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
        this.teardown = Collections.unmodifiableList(new ArrayList<>(teardown));
    }

    public static TestCase of(String name, TestStep... steps) {
        return new TestCase(name, new ArrayList<>(Arrays.asList(steps)));
    }

    /**
     * Returns a new TestCase identical to this one but with the given teardown
     * steps appended. Existing steps are preserved.
     */
    public TestCase withTeardown(TestStep... teardownSteps) {
        return new TestCase(this.name, this.steps,
                new ArrayList<>(Arrays.asList(teardownSteps)));
    }

    public String getName() { return name; }
    public List<TestStep> getSteps() { return steps; }
    public List<TestStep> getTeardown() { return teardown; }
}
