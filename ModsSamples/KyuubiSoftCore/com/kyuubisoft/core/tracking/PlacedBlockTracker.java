/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.tracking;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class PlacedBlockTracker {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core Tracking");
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<BlockPos, Long>> placedBlocksWithTime = new ConcurrentHashMap();
    private boolean enabled = true;
    private long expiryMinutes = 30L;

    public void configure(boolean enabled, long expiryMinutes) {
        this.enabled = enabled;
        this.expiryMinutes = expiryMinutes;
        LOGGER.info("PlacedBlockTracker configured: enabled=" + enabled + ", expiryMinutes=" + expiryMinutes);
    }

    public void recordPlacement(UUID playerId, int x, int y, int z) {
        if (!this.enabled) {
            return;
        }
        this.placedBlocksWithTime.computeIfAbsent(playerId, k -> new ConcurrentHashMap()).put(new BlockPos(x, y, z), System.currentTimeMillis());
    }

    public boolean wasPlacedByPlayer(UUID playerId, int x, int y, int z) {
        long elapsedMs;
        if (!this.enabled) {
            return false;
        }
        ConcurrentHashMap<BlockPos, Long> playerBlocks = this.placedBlocksWithTime.get(playerId);
        if (playerBlocks == null || playerBlocks.isEmpty()) {
            return false;
        }
        BlockPos pos = new BlockPos(x, y, z);
        Long placedTime = playerBlocks.get(pos);
        if (placedTime == null) {
            return false;
        }
        if (this.expiryMinutes > 0L && (elapsedMs = System.currentTimeMillis() - placedTime) > this.expiryMinutes * 60000L) {
            playerBlocks.remove(pos);
            return false;
        }
        playerBlocks.remove(pos);
        return true;
    }

    public void clearPlayer(UUID playerId) {
        this.placedBlocksWithTime.remove(playerId);
    }

    public void cleanupExpired() {
        if (this.expiryMinutes <= 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        long expiryMs = this.expiryMinutes * 60000L;
        int removed = 0;
        Iterator<Map.Entry<UUID, ConcurrentHashMap<BlockPos, Long>>> iterator = this.placedBlocksWithTime.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ConcurrentHashMap<BlockPos, Long>> entry = iterator.next();
            ConcurrentHashMap<BlockPos, Long> blocks = entry.getValue();
            int before = blocks.size();
            blocks.entrySet().removeIf(e -> now - (Long)e.getValue() > expiryMs);
            removed += before - blocks.size();
            if (!blocks.isEmpty()) continue;
            iterator.remove();
        }
        if (removed > 0) {
            LOGGER.fine("PlacedBlockTracker cleanup: removed " + removed + " expired entries, " + this.placedBlocksWithTime.size() + " players tracked");
        }
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    private record BlockPos(int x, int y, int z) {
    }
}

