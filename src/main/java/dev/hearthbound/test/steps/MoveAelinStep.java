package dev.hearthbound.test.steps;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.npc.ElfSage;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

/**
 * Moves Aelin through the real ElfSage role-change/move path. Used to test
 * cross-chunk and chunk-border persistence without bypassing elf-specific code.
 */
public final class MoveAelinStep implements TestStep {

    public enum Target { CHUNK_CENTER, CHUNK_BORDER }

    private final Target target;
    private final int chunkDx;
    private final int chunkDz;
    private final String role;

    public MoveAelinStep(Target target, int chunkDx, int chunkDz, String role) {
        this.target = target;
        this.chunkDx = chunkDx;
        this.chunkDz = chunkDz;
        this.role = role;
    }

    @Override
    public StepResult execute(TestContext ctx) {
        VillageData village = VillageManager.get().getVillageData(ctx.getStore(), ctx.getPlayerRef());
        if (village == null || village.getElfId() == null) return StepResult.fail("Aelin missing");
        NpcRegistry.NpcRecord record = NpcRegistry.get().getRecord(village.getElfId());
        if (record == null) return StepResult.fail("Aelin registry record missing");

        double baseX = record.hasPosition ? record.lastX : 0.0;
        double baseY = record.hasPosition ? record.lastY : 80.0;
        double baseZ = record.hasPosition ? record.lastZ : 0.0;
        var ref = ctx.getWorld().getEntityRef(village.getElfId());
        if (ref != null && ref.isValid()) {
            TransformComponent tc = ctx.getStore().getComponent(ref, TransformComponent.getComponentType());
            if (tc != null && tc.getPosition() != null) {
                baseX = tc.getPosition().x;
                baseY = tc.getPosition().y;
                baseZ = tc.getPosition().z;
            }
        }

        long currentChunk = ChunkUtil.indexChunkFromBlock(baseX, baseZ);
        int targetChunkX = ChunkUtil.xOfChunkIndex(currentChunk) + chunkDx;
        int targetChunkZ = ChunkUtil.zOfChunkIndex(currentChunk) + chunkDz;
        double local = target == Target.CHUNK_BORDER ? 31.25 : 16.0;
        double x = ChunkUtil.worldCoordFromLocalCoord(targetChunkX, (int) local) + (local % 1.0);
        double z = ChunkUtil.worldCoordFromLocalCoord(targetChunkZ, (int) local) + (local % 1.0);
        Vector3d pos = new Vector3d(x, baseY, z);

        ElfSage.respawnAs(ctx.getStore(), ctx.getPlayerRef(), ctx.getWorld(),
                role, pos, new Vector3f(0, 0, 0));
        ctx.put("aelin:lastTargetChunk", ChunkUtil.indexChunkFromBlock(x, z));
        ctx.getLogger().info("MoveAelin: " + target + " role=" + role
                + " pos=" + (int) x + "," + (int) baseY + "," + (int) z
                + " chunk=" + ctx.get("aelin:lastTargetChunk"));
        return StepResult.pass();
    }

    @Override
    public String getName() {
        return "MoveAelin(" + target + "," + role + ")";
    }
}
