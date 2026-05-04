package dev.hearthbound.test.steps;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Teleports the player a long distance in a random XZ direction so the chunk
 * around the previous area is forced to unload.
 *
 * Default distance is 5000 blocks — comfortably beyond any reasonable view
 * distance. Always remembers the original position under the scratch key
 * {@code "homePos"} so {@link TeleportPlayerHomeStep} can return there.
 */
public final class TeleportPlayerFarStep implements TestStep {

    public static final String HOME_POS_KEY = "homePos";

    private final double distance;

    public TeleportPlayerFarStep() { this(5000.0); }

    public TeleportPlayerFarStep(double distance) {
        this.distance = distance;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        TransformComponent transform = ctx.getStore().getComponent(
                ctx.getPlayerRef(), TransformComponent.getComponentType());
        if (transform == null || transform.getPosition() == null) {
            return StepResult.fail("no player transform");
        }
        Vector3d here = transform.getPosition();
        if (!ctx.has(HOME_POS_KEY)) {
            ctx.put(HOME_POS_KEY, new double[]{ here.getX(), here.getY(), here.getZ() });
        }

        double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
        double dx = Math.cos(angle) * distance;
        double dz = Math.sin(angle) * distance;
        double tx = here.getX() + dx;
        double ty = here.getY();
        double tz = here.getZ() + dz;

        Teleport teleport = Teleport.createForPlayer(
                ctx.getWorld(),
                new Vector3d(tx, ty, tz),
                new Vector3f(0, 0, 0));
        ctx.getStore().addComponent(
                ctx.getPlayerRef(), Teleport.getComponentType(), teleport);
        ctx.getLogger().info("TeleportPlayerFar: " + (int) tx + "," + (int) ty + "," + (int) tz
                + " (dist=" + (int) distance + ")");
        return StepResult.pass();
    }

    @Override
    public String getName() { return "TeleportFar(" + (int) distance + ")"; }
}
