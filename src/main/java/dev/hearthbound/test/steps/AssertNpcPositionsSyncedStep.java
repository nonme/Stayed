package dev.hearthbound.test.steps;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
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
 * Verifies that live NPC transform data has been synced back into NpcRegistry.
 * This catches the stale chunkIndex bug that can appear after an NPC walks or
 * is moved to a different chunk before unload.
 */
public final class AssertNpcPositionsSyncedStep implements TestStep {

    private final Predicate<NpcRegistry.NpcRecord> filter;
    private final double tolerance;
    private final boolean checkExactPosition;

    public AssertNpcPositionsSyncedStep(Predicate<NpcRegistry.NpcRecord> filter) {
        this(filter, 8.0, false);
    }

    public AssertNpcPositionsSyncedStep(Predicate<NpcRegistry.NpcRecord> filter, double tolerance) {
        this(filter, tolerance, true);
    }

    public AssertNpcPositionsSyncedStep(Predicate<NpcRegistry.NpcRecord> filter,
                                        double tolerance, boolean checkExactPosition) {
        this.filter = filter;
        this.tolerance = tolerance;
        this.checkExactPosition = checkExactPosition;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        List<String> errors = new ArrayList<>();
        int checked = 0;
        for (NpcRegistry.NpcRecord record : NpcRegistry.get().allRecords()) {
            if (!filter.test(record) || record.entityUuid == null) continue;
            Ref<EntityStore> ref = ctx.getWorld().getEntityRef(record.entityUuid);
            if (ref == null || !ref.isValid()) continue;
            TransformComponent tc = ctx.getStore().getComponent(ref, TransformComponent.getComponentType());
            if (tc == null || tc.getPosition() == null) continue;
            checked++;
            var pos = tc.getPosition();
            long liveChunk = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
            if (record.chunkIndex != liveChunk) {
                errors.add(record.npcId + " chunk " + record.chunkIndex + " != live " + liveChunk);
            }
            if (!record.hasPosition) {
                errors.add(record.npcId + " has no recorded position");
                continue;
            }
            if (!checkExactPosition) continue;
            double dx = pos.x - record.lastX;
            double dy = pos.y - record.lastY;
            double dz = pos.z - record.lastZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > tolerance) {
                errors.add(record.npcId + " position drift " + String.format("%.2f", dist));
            }
        }
        if (!errors.isEmpty()) {
            for (String error : errors) ctx.getLogger().warn("PositionSync: " + error);
            return StepResult.fail(errors.size() + " stale position(s)");
        }
        ctx.getLogger().info("PositionSync: checked " + checked + " live NPC(s)");
        return StepResult.pass(checked + " checked");
    }

    @Override
    public String getName() { return "AssertNpcPositionsSynced"; }
}
