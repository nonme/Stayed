package dev.hearthbound.test.steps;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;

import java.util.function.Predicate;

/**
 * Deterministically moves matching live NPCs to a local position inside another
 * chunk. Unlike NudgeNpcStep, this guarantees the target chunk/local coordinate.
 */
public final class MoveNpcsToChunkStep implements TestStep {

    private final Predicate<NpcRegistry.NpcRecord> filter;
    private final int chunkDx;
    private final int chunkDz;
    private final double localX;
    private final double localZ;

    public MoveNpcsToChunkStep(Predicate<NpcRegistry.NpcRecord> filter,
                               int chunkDx, int chunkDz, double localX, double localZ) {
        this.filter = filter;
        this.chunkDx = chunkDx;
        this.chunkDz = chunkDz;
        this.localX = localX;
        this.localZ = localZ;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        int moved = 0;
        for (NpcRegistry.NpcRecord record : NpcRegistry.get().allRecords()) {
            if (!filter.test(record) || record.entityUuid == null) continue;
            Entity entity = ctx.getWorld().getEntity(record.entityUuid);
            if (entity == null) continue;
            Ref<EntityStore> ref = ctx.getWorld().getEntityRef(record.entityUuid);
            if (ref == null || !ref.isValid()) continue;
            TransformComponent tc = ctx.getStore().getComponent(ref, TransformComponent.getComponentType());
            if (tc == null || tc.getPosition() == null) continue;

            var pos = tc.getPosition();
            long chunk = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
            int targetChunkX = ChunkUtil.xOfChunkIndex(chunk) + chunkDx;
            int targetChunkZ = ChunkUtil.zOfChunkIndex(chunk) + chunkDz;
            double x = ChunkUtil.worldCoordFromLocalCoord(targetChunkX, (int) localX) + (localX % 1.0);
            double z = ChunkUtil.worldCoordFromLocalCoord(targetChunkZ, (int) localZ) + (localZ % 1.0);
            entity.moveTo(ref, x, pos.y, z, ctx.getStore());
            NPCEntity npcEntity = ctx.getStore().getComponent(ref, NPCEntity.getComponentType());
            if (npcEntity != null) {
                npcEntity.setLeashPoint(new com.hypixel.hytale.math.vector.Vector3d(x, pos.y, z));
            }
            moved++;
        }
        ctx.getLogger().info("MoveNpcsToChunk: moved " + moved
                + " to chunk offset " + chunkDx + "," + chunkDz
                + " local=" + localX + "," + localZ);
        return moved > 0 ? StepResult.pass(moved + " moved") : StepResult.fail("no live NPCs moved");
    }

    @Override
    public String getName() {
        return "MoveNpcsToChunk(" + chunkDx + "," + chunkDz + ")";
    }
}
