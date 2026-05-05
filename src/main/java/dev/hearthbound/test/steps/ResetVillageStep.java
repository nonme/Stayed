package dev.hearthbound.test.steps;

import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;
import dev.hearthbound.village.ResetVillageService;

/**
 * Resets the player's village through the same implementation used by /hb reset.
 */
public final class ResetVillageStep implements TestStep {

    @Override
    public StepResult execute(TestContext ctx) {
        ResetVillageService.reset(ctx.getStore(), ctx.getPlayerRef(), ctx.getWorld());
        ctx.getLogger().info("ResetVillage: fresh village state requested");
        return StepResult.pass();
    }

    @Override
    public String getName() { return "ResetVillage"; }
}
