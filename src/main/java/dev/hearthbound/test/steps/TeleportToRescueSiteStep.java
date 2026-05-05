package dev.hearthbound.test.steps;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;

/**
 * Teleports the player to the latest rescue site saved by StartRescueQuestStep.
 */
public final class TeleportToRescueSiteStep implements TestStep {

    @Override
    public StepResult execute(TestContext ctx) {
        Vector3d pos = ctx.get("lastRescueEnemyPos");
        if (pos == null) pos = ctx.get("lastRescueVictimPos");
        if (pos == null) {
            return StepResult.fail("no rescue site position in context");
        }
        Teleport teleport = Teleport.createForPlayer(
                ctx.getWorld(),
                new Vector3d(pos.getX(), pos.getY() + 2.0, pos.getZ()),
                new Vector3f(0, 0, 0));
        ctx.getStore().addComponent(ctx.getPlayerRef(), Teleport.getComponentType(), teleport);
        ctx.getLogger().info("TeleportToRescueSite: " + (int) pos.getX()
                + "," + (int) pos.getY() + "," + (int) pos.getZ());
        return StepResult.pass();
    }

    @Override
    public String getName() { return "TeleportToRescueSite"; }
}
