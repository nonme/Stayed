package dev.hearthbound.test.steps;

import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestCleanup;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Removes every loaded test NPC entity, every registry record with a
 * non-null testMarker, and any orphaned villager summary in the player's
 * village. See {@link TestCleanup#fullCleanup} for full semantics.
 *
 * Always passes — cleanup is best-effort and is meant to run from a
 * {@link dev.hearthbound.test.engine.TestCase#getTeardown teardown} block,
 * which always executes regardless of the test verdict.
 */
public final class CleanupTestNpcsStep implements TestStep {

    @Override
    public StepResult execute(TestContext ctx) {
        TestCleanup.Result result = TestCleanup.fullCleanup(
                ctx.getWorld(), ctx.getStore(), ctx.getPlayerRef());

        // Forget the per-case scratch list now that the registry-side cleanup
        // has handled everything anchored to a registry record.
        ctx.put("spawnedNpcs", new ArrayList<UUID>());

        ctx.getLogger().info("CleanupTestNpcs: " + result);
        return StepResult.pass(result.toString());
    }

    @Override
    public String getName() { return "CleanupTestNpcs"; }
}
