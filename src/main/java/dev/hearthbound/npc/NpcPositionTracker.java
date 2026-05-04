package dev.hearthbound.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.util.TickScheduler;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Continuously syncs the world position of every live registered NPC into its
 * {@link NpcRegistry.NpcRecord}. Runs every {@link #SYNC_INTERVAL_MS} on the
 * world thread.
 *
 * The recorded position is the spawn-point fallback used by
 * {@link dev.hearthbound.events.NpcChunkLoadHandler} when an NPC's entity is
 * gone but the registry record still claims the chunk. Without this loop a
 * villager who walks across a chunk boundary right before unload would have a
 * stale position pinned to where they originally spawned.
 */
public final class NpcPositionTracker {

    private static final Logger LOGGER = Logger.getLogger(NpcPositionTracker.class.getName());
    private static final long SYNC_INTERVAL_MS = 5_000L;

    private static volatile World trackedWorld = null;
    private static ScheduledFuture<?> task = null;

    private NpcPositionTracker() {}

    public static synchronized void start(World world) {
        if (task != null) return;
        trackedWorld = world;
        task = TickScheduler.getExecutor().scheduleAtFixedRate(NpcPositionTracker::tick,
                SYNC_INTERVAL_MS, SYNC_INTERVAL_MS, TimeUnit.MILLISECONDS);
        LOGGER.info("NpcPositionTracker started (interval=" + SYNC_INTERVAL_MS + "ms)");
    }

    public static synchronized void stop() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
        trackedWorld = null;
    }

    private static void tick() {
        World world = trackedWorld;
        if (world == null) return;
        world.execute(() -> {
            var store = world.getEntityStore().getStore();
            boolean anyChanged = false;
            for (NpcRegistry.NpcRecord record : NpcRegistry.get().allRecords()) {
                if (record.entityUuid == null) continue;
                Ref<EntityStore> ref = world.getEntityRef(record.entityUuid);
                if (ref == null || !ref.isValid()) continue;
                TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
                if (tc == null || tc.getPosition() == null) continue;
                var pos = tc.getPosition();
                if (NpcRegistry.get().updatePosition(record.entityUuid, pos.x, pos.y, pos.z)) {
                    anyChanged = true;
                }
            }
            if (anyChanged) HearthboundDataStore.get().markDirty();
        });
    }
}
