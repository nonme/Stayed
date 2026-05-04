package dev.hearthbound.test.steps;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Moves every registered villager to a random offset in [-maxOffset, +maxOffset]
 * on each axis. Used by stress packages to thrash positions and exercise the
 * NpcPositionTracker / chunk-boundary code paths.
 */
public final class NudgeAllNpcsRandomStep implements TestStep {

    private final double maxOffset;

    public NudgeAllNpcsRandomStep(double maxOffset) {
        this.maxOffset = maxOffset;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int moved = 0;
        for (NpcRegistry.NpcRecord r : NpcRegistry.get().allRecords()) {
            if (r.entityUuid == null) continue;
            Entity entity = ctx.getWorld().getEntity(r.entityUuid);
            if (entity == null) continue;
            Ref<EntityStore> ref = ctx.getWorld().getEntityRef(r.entityUuid);
            if (ref == null || !ref.isValid()) continue;
            TransformComponent tc = ctx.getStore().getComponent(
                    ref, TransformComponent.getComponentType());
            if (tc == null || tc.getPosition() == null) continue;
            var pos = tc.getPosition();

            double dx = (rng.nextDouble() - 0.5) * 2 * maxOffset;
            double dz = (rng.nextDouble() - 0.5) * 2 * maxOffset;
            entity.moveTo(ref, pos.x + dx, pos.y, pos.z + dz, ctx.getStore());
            moved++;
        }
        ctx.getLogger().info("NudgeAllRandom: moved " + moved + " (max=" + maxOffset + ")");
        return StepResult.pass(moved + " moved");
    }

    @Override
    public String getName() { return "NudgeAllRandom(" + maxOffset + ")"; }
}
