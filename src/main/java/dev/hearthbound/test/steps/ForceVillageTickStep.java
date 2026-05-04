package dev.hearthbound.test.steps;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.HearthboundPlugin;
import dev.hearthbound.events.VillageTickHandler;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;

/**
 * Synchronously runs the VillageTickHandler tick {@code count} times in a row,
 * fast-forwarding the housing / role assignment / production pipeline that
 * normally fires every 5 seconds. Each call is a full tick — the same one the
 * scheduler invokes — so behavior is identical, just compressed in time.
 */
public final class ForceVillageTickStep implements TestStep {

    private final int count;

    public ForceVillageTickStep(int count) {
        this.count = count;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        VillageTickHandler handler = HearthboundPlugin.get().getVillageTickHandler();
        if (handler == null) {
            return StepResult.fail("VillageTickHandler not initialised");
        }
        for (int i = 0; i < count; i++) {
            handler.runOneTickNow();
        }

        // The tick may teleport villagers to assigned houses via entity.moveTo,
        // which leaves NpcPositionTracker (5s scheduler) lagging. Sync the
        // registry positions now so a follow-up audit sees fresh data instead
        // of stale spawn coordinates.
        int synced = 0;
        for (NpcRegistry.NpcRecord record : NpcRegistry.get().allRecords()) {
            if (record.entityUuid == null) continue;
            Ref<EntityStore> ref = ctx.getWorld().getEntityRef(record.entityUuid);
            if (ref == null || !ref.isValid()) continue;
            TransformComponent tc = ctx.getStore().getComponent(
                    ref, TransformComponent.getComponentType());
            if (tc == null || tc.getPosition() == null) continue;
            var pos = tc.getPosition();
            NpcRegistry.get().updatePosition(record.entityUuid, pos.x, pos.y, pos.z);
            synced++;
        }

        ctx.getLogger().info("ForceVillageTick: ran " + count + " tick(s), synced "
                + synced + " positions");
        return StepResult.pass(count + " ticks");
    }

    @Override
    public String getName() { return "ForceVillageTick(" + count + ")"; }
}
