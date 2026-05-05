package dev.hearthbound.test.steps;

import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import dev.hearthbound.building.BuildingSystem;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

/**
 * Calls the same backend path as TownHallPage's Confirm button.
 */
public final class ConfirmFoundingStep implements TestStep {

    private final int offsetX;
    private final int offsetZ;

    public ConfirmFoundingStep(int offsetX, int offsetZ) {
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        TransformComponent transform = ctx.getStore().getComponent(
                ctx.getPlayerRef(), TransformComponent.getComponentType());
        if (transform == null || transform.getPosition() == null) {
            return StepResult.fail("no player transform");
        }
        var pos = transform.getPosition();
        int x = (int) Math.floor(pos.x) + offsetX;
        int y = (int) Math.floor(pos.y) - 1;
        int z = (int) Math.floor(pos.z) + offsetZ;

        BuildingSystem.get().confirmFounding(ctx.getStore(), ctx.getPlayerRef(), ctx.getWorld(),
                "Aelin Test", x, y, z, 0);
        ctx.put(TeleportPlayerFarStep.HOME_POS_KEY, new double[]{ x + 0.5, y + 1.0, z + 0.5 });

        VillageData village = VillageManager.get().getVillageData(ctx.getStore(), ctx.getPlayerRef());
        if (village == null || !village.isFounded()) return StepResult.fail("village not founded");
        if (village.findBuilding(BuildingType.TOWN_HALL) == null) {
            return StepResult.fail("town hall record missing after founding");
        }
        ctx.getLogger().info("ConfirmFounding: " + x + "," + y + "," + z);
        return StepResult.pass();
    }

    @Override
    public String getName() { return "ConfirmFounding"; }
}
