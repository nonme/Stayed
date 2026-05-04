package dev.hearthbound.test.steps;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

/**
 * Ensures a founded village exists. If one already exists — no-op. Otherwise
 * founds a village at the player's current position with a placeholder name
 * so subsequent test steps can rely on {@link VillageData#isFounded()}.
 *
 * Cleanup intentionally does NOT remove this village — the player keeps it
 * after the test finishes (per TESTING.md §2).
 */
public final class SetupVillageStep implements TestStep {

    @Override
    public StepResult execute(TestContext ctx) {
        VillageData village = VillageManager.get().getVillageData(
                ctx.getStore(), ctx.getPlayerRef());
        if (village != null && village.isFounded()) {
            ctx.getLogger().info("SetupVillage: village already founded at "
                    + village.getFoundingStoneX() + ","
                    + village.getFoundingStoneY() + ","
                    + village.getFoundingStoneZ());
            return StepResult.pass("already founded");
        }

        TransformComponent transform = ctx.getStore().getComponent(
                ctx.getPlayerRef(), TransformComponent.getComponentType());
        if (transform == null || transform.getPosition() == null) {
            return StepResult.fail("no player transform");
        }
        Vector3d pos = transform.getPosition();
        int x = (int) pos.getX();
        int y = (int) pos.getY();
        int z = (int) pos.getZ();

        VillageManager.get().foundVillage(
                ctx.getStore(), ctx.getPlayerRef(), ctx.getWorld(), x, y, z);

        VillageData fresh = VillageManager.get().getVillageData(
                ctx.getStore(), ctx.getPlayerRef());
        if (fresh != null && fresh.getVillageName() != null && fresh.getVillageName().isBlank()) {
            fresh.setVillageName("TestVillage");
            VillageManager.get().save(ctx.getStore(), ctx.getPlayerRef(), fresh);
        }

        ctx.getLogger().info("SetupVillage: founded at " + x + "," + y + "," + z);
        return StepResult.pass("founded at " + x + "," + y + "," + z);
    }
}
