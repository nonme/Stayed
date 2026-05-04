package dev.hearthbound.test.steps;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

/**
 * Teleports the player back to the position remembered by
 * {@link TeleportPlayerFarStep}, falling back to the founding stone of the
 * current village when no remembered position exists.
 */
public final class TeleportPlayerHomeStep implements TestStep {

    @Override
    public StepResult execute(TestContext ctx) {
        double[] home = ctx.get(TeleportPlayerFarStep.HOME_POS_KEY);
        if (home == null) {
            VillageData village = VillageManager.get().getVillageData(
                    ctx.getStore(), ctx.getPlayerRef());
            if (village == null || !village.isFounded()) {
                return StepResult.fail("no remembered home and no founded village");
            }
            home = new double[]{
                    village.getFoundingStoneX() + 0.5,
                    village.getFoundingStoneY() + 1.0,
                    village.getFoundingStoneZ() + 0.5,
            };
        }

        Teleport teleport = Teleport.createForPlayer(
                ctx.getWorld(),
                new Vector3d(home[0], home[1], home[2]),
                new Vector3f(0, 0, 0));
        ctx.getStore().addComponent(
                ctx.getPlayerRef(), Teleport.getComponentType(), teleport);
        ctx.getLogger().info("TeleportPlayerHome: " + (int) home[0] + ","
                + (int) home[1] + "," + (int) home[2]);
        return StepResult.pass();
    }

    @Override
    public String getName() { return "TeleportHome"; }
}
