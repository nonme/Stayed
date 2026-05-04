package dev.hearthbound.test.steps;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;

/**
 * Teleports the player to a fixed world position. Uses the engine's Teleport
 * component, the same path {@code /hb warp} uses — this triggers chunk
 * load/unload around the new position.
 */
public final class TeleportPlayerStep implements TestStep {

    private final double x, y, z;

    public TeleportPlayerStep(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        Teleport teleport = Teleport.createForPlayer(
                ctx.getWorld(),
                new Vector3d(x, y, z),
                new Vector3f(0, 0, 0));
        ctx.getStore().addComponent(
                ctx.getPlayerRef(), Teleport.getComponentType(), teleport);
        ctx.getLogger().info("TeleportPlayer: " + x + "," + y + "," + z);
        return StepResult.pass();
    }

    @Override
    public String getName() {
        return "Teleport(" + (int) x + "," + (int) y + "," + (int) z + ")";
    }
}
