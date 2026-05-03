package dev.hearthbound.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.util.TickScheduler;

import java.awt.Color;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Continuously syncs the world position of every live registered NPC into its
 * NpcRegistry record. Runs every {@link #SYNC_INTERVAL_MS} on the world thread.
 *
 * Why we need this: chunkIndex used to be updated only on restore, so an NPC
 * that wandered into a different chunk and got saved with that chunk would
 * leave a stale chunkIndex behind. After a server restart we'd force-load the
 * stale chunk, find no entity, and the NPC would appear lost. With continuous
 * position tracking, the registry always points at where the NPC actually is,
 * so chunk force-loads target the right chunk and Pass 2 of NpcChunkLoadHandler
 * picks the entity up.
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
        // All entity-store reads must happen on the world thread.
        world.execute(() -> {
            var store = world.getEntityStore().getStore();
            boolean anyChanged = false;
            for (NpcRegistry.NpcRecord record : NpcRegistry.get().allRecords()) {
                // Notify the player about NPCs the circuit breaker has marked
                // unrecoverable. Done here (not at break time) so we don't fire
                // the notification before the player is in the world.
                if (record.broken && !record.brokenNotified) {
                    notifyPlayersAboutBrokenNpc(record);
                    record.brokenNotified = true;
                    HearthboundDataStore.get().markDirty();
                }

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

    private static void notifyPlayersAboutBrokenNpc(NpcRegistry.NpcRecord record) {
        try {
            String text = "[Stayed] A villager (" + record.roleName + ") could not be restored after multiple attempts. "
                    + "Please send this as a bug report to the developer: uuid=" + record.entityUuid
                    + (record.hasPosition
                            ? (" lastPos=(" + (int) record.lastX + "," + (int) record.lastY + "," + (int) record.lastZ + ")")
                            : " lastPos=unknown");
            Message msg = Message.raw(text).color(Color.RED);
            for (PlayerRef pr : Universe.get().getPlayers()) {
                if (pr == null) continue;
                pr.sendMessage(msg);
            }
            LOGGER.warning("notifyPlayersAboutBrokenNpc: " + text);
        } catch (Exception e) {
            LOGGER.warning("notifyPlayersAboutBrokenNpc failed: " + e.getMessage());
        }
    }
}
