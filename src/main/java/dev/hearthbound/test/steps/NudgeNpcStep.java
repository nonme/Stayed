package dev.hearthbound.test.steps;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Moves every live NPC matching {@code filter} by ({@code dx}, {@code dy},
 * {@code dz}) blocks via {@link Entity#moveTo}.
 *
 * Used by chunk_border etc. to deterministically push villagers across a
 * chunk boundary before forcing a chunk unload — exercises exactly the path
 * that produced the original 760-respawn cascade.
 */
public final class NudgeNpcStep implements TestStep {

    private final Predicate<NpcRegistry.NpcRecord> filter;
    private final double dx;
    private final double dy;
    private final double dz;

    public NudgeNpcStep(Predicate<NpcRegistry.NpcRecord> filter,
                        double dx, double dy, double dz) {
        this.filter = filter;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        List<NpcRegistry.NpcRecord> targets = new ArrayList<>();
        for (NpcRegistry.NpcRecord r : NpcRegistry.get().allRecords()) {
            if (r.entityUuid != null && filter.test(r)) targets.add(r);
        }

        int moved = 0;
        for (NpcRegistry.NpcRecord r : targets) {
            Entity entity = ctx.getWorld().getEntity(r.entityUuid);
            if (entity == null) continue;
            Ref<EntityStore> ref = ctx.getWorld().getEntityRef(r.entityUuid);
            if (ref == null || !ref.isValid()) continue;

            TransformComponent tc = ctx.getStore().getComponent(
                    ref, TransformComponent.getComponentType());
            if (tc == null || tc.getPosition() == null) continue;
            var pos = tc.getPosition();

            entity.moveTo(ref, pos.x + dx, pos.y + dy, pos.z + dz, ctx.getStore());
            moved++;
        }

        ctx.getLogger().info("Nudge: moved " + moved + "/" + targets.size()
                + " by (" + dx + "," + dy + "," + dz + ")");
        return StepResult.pass(moved + " moved");
    }

    @Override
    public String getName() {
        return "Nudge(" + dx + "," + dy + "," + dz + ")";
    }
}
