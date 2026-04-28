package dev.hearthbound.building;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.connectedblocks.ConnectedBlocksUtil;

public class BlockPlacer {

    private static final Logger LOGGER = Logger.getLogger(BlockPlacer.class.getName());

    public record BlockEntry(int x, int y, int z, String blockType, int rotation) {

        public BlockEntry(int x, int y, int z, String blockType) {
            this(x, y, z, blockType, 0);
        }
    }

    private final World world;
    private final List<BlockEntry> blocks;
    private final long delayMs;
    private final Runnable onComplete;
    private int currentIndex;
    private ScheduledFuture<?> task;

    public BlockPlacer(World world, List<BlockEntry> blocks, long delayMs, Runnable onComplete) {
        this.world = world;
        this.blocks = List.copyOf(blocks);
        this.delayMs = delayMs;
        this.onComplete = onComplete;
        this.currentIndex = 0;
    }

    public BlockPlacer(World world, List<BlockEntry> blocks, long delayMs) {
        this(world, blocks, delayMs, null);
    }

    public void start(ScheduledExecutorService scheduler) {
        task = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (currentIndex >= blocks.size()) {
                    if (task != null) {
                        task.cancel(false);
                    }
                    if (onComplete != null) {
                        world.execute(onComplete);
                    }
                    return;
                }
                BlockEntry entry = blocks.get(currentIndex);
                world.execute(() -> placeBlock(world, entry));
                currentIndex++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "BlockPlacer tick failed at index " + currentIndex, e);
                if (task != null) {
                    task.cancel(false);
                }
            }
        }, 0, delayMs, TimeUnit.MILLISECONDS);
    }

    public static void placeBlock(World world, BlockEntry entry) {
        if ("Empty".equals(entry.blockType())) {
            silentRemoveBlock(world, entry.x(), entry.y(), entry.z());
            return;
        }
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(entry.x(), entry.z()));
        if (chunk == null) {
            world.setBlock(entry.x(), entry.y(), entry.z(), entry.blockType());
            return;
        }
        var assetMap = BlockType.getAssetMap();
        // Strip '*' prefix for assetMap lookup — state-variant IDs use '*' as a marker.
        // If not found in assetMap, pass the original blockType (with '*') to world.setBlock —
        // the engine accepts '*Foo_State_Definitions_Bar' directly via setBlock.
        String originalId = entry.blockType();
        String lookupId = originalId.startsWith("*") ? originalId.substring(1) : originalId;
        int blockId = assetMap.getIndex(lookupId);
        if (blockId == Integer.MIN_VALUE) {
            world.setBlock(entry.x(), entry.y(), entry.z(), originalId);
            return;
        }
        BlockType bt = assetMap.getAsset(blockId);
        int rotation = entry.rotation();
        chunk.setBlock(entry.x(), entry.y(), entry.z(), blockId, bt, rotation, 0, 4);
        ConnectedBlocksUtil.setConnectedBlockAndNotifyNeighbors(
                blockId,
                RotationTuple.get(rotation),
                Vector3i.ZERO,
                new Vector3i(entry.x(), entry.y(), entry.z()),
                chunk,
                chunk.getBlockChunk());
    }

    /**
     * Removes a block silently — no break sound, no particles, no item drops.
     * Uses setBlock(Empty) with suppression flags instead of breakBlock.
     */
    public static void silentRemoveBlock(World world, int x, int y, int z) {
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) {
            world.breakBlock(x, y, z, 0);
            return;
        }
        var assetMap = BlockType.getAssetMap();
        int emptyId = assetMap.getIndex("Empty");
        if (emptyId == Integer.MIN_VALUE) {
            world.breakBlock(x, y, z, 0);
            return;
        }
        // NO_SEND_PARTICLES(4) | NO_SEND_AUDIO(1024) | NO_DROP_ITEMS(2048)
        chunk.setBlock(x, y, z, emptyId, assetMap.getAsset(emptyId), 0, 0, 4 | 1024 | 2048);
    }

    public static void updateConnectedBlock(World world, int x, int y, int z, String blockTypeKey, int rotation) {
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) {
            return;
        }
        var assetMap = BlockType.getAssetMap();
        int blockId = assetMap.getIndex(blockTypeKey);
        if (blockId == Integer.MIN_VALUE) {
            return;
        }
        ConnectedBlocksUtil.setConnectedBlockAndNotifyNeighbors(
                blockId, RotationTuple.get(rotation), Vector3i.ZERO,
                new Vector3i(x, y, z), chunk, chunk.getBlockChunk());
    }

    public boolean isFinished() {
        return currentIndex >= blocks.size();
    }

    public int getProgress() {
        return blocks.isEmpty() ? 100 : (currentIndex * 100 / blocks.size());
    }

    public int getBlocksPlaced() {
        return currentIndex;
    }

    public int getTotal() {
        return blocks.size();
    }

    public void cancel() {
        if (task != null) {
            task.cancel(false);
        }
    }
}
