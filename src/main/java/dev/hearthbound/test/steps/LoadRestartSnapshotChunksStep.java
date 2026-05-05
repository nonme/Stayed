package dev.hearthbound.test.steps;

import com.hypixel.hytale.math.util.ChunkUtil;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;
import dev.hearthbound.test.engine.TestStep;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Force-loads every chunk that can contain a restart snapshot NPC. The one
 * chunk ring catches NPCs saved close to a border or walking during shutdown.
 */
public final class LoadRestartSnapshotChunksStep implements TestStep {

    @Override
    public StepResult execute(TestContext ctx) {
        try {
            RestartSnapshotStore.Snapshot snapshot = RestartSnapshotStore.load();
            Set<Long> chunks = new LinkedHashSet<>();
            for (RestartSnapshotStore.Record record : snapshot.records) {
                if (!record.hasPosition) continue;
                long center = record.chunkIndex;
                int cx = ChunkUtil.xOfChunkIndex(center);
                int cz = ChunkUtil.zOfChunkIndex(center);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        long idx = ChunkUtil.indexChunk(cx + dx, cz + dz);
                        chunks.add(idx);
                        ctx.getWorld().getChunkAsync(idx);
                    }
                }
            }
            ctx.getLogger().info("LoadRestartSnapshotChunks: requested " + chunks.size()
                    + " chunk(s) around snapshot records");
            return StepResult.delay(5_000L);
        } catch (Exception e) {
            return StepResult.fail("load restart snapshot chunks failed: " + e.getMessage());
        }
    }

    @Override
    public String getName() { return "LoadRestartSnapshotChunks"; }
}
