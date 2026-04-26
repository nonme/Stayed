package dev.hearthbound.npc;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import dev.hearthbound.util.TickScheduler;

/**
 * In-memory registry of all Hearthbound NPCs, independent of BSON.
 *
 * In-memory registry of all Hearthbound NPCs, independent of BSON.
 * - NPC state stored here, not in BSON
 * - ChunkPreLoadProcessEvent triggers restore polling via scheduleRestoreOne()
 * - reconcileTask() runs every 1s and re-restores any NPC whose ref became invalid
 *   reconcileTask() re-restores any NPC whose ref became invalid — fix for
 *   getEntityRef() timeout when player is far from the NPC chunk at chunk load time
 * - pendingRemovals: deferred deletion for NPCs in unloaded chunks
 */
public final class NpcRegistry {

    private static final Logger LOGGER = Logger.getLogger(NpcRegistry.class.getName());

    private static final NpcRegistry INSTANCE = new NpcRegistry();

    public static NpcRegistry get() {
        return INSTANCE;
    }

    private static final long POLL_INTERVAL_MS = 200;
    private static final long POLL_INTERVAL_MS  = 200;
    private static final long POLL_TIMEOUT_MS   = 12_000;

    private static final long RECONCILE_INTERVAL_MS = 1_000;
    private static final long RECONCILE_INTERVAL_MS = 1_000;

    public enum InteractionType { ELF, RESCUE, VILLAGER, NONE }

    /**
     * Everything needed to restore an NPC after chunk reload.
     * Stored outside BSON — survives chunk unload without any BSON dependency.
     */
    public static final class NpcRecord {
        public final UUID entityUuid;
        public final String roleName;
        public final InteractionType interaction;
        /** Non-zero for villagers/rescue victims that need a skin applied. */
        public final long skinSeed;
        /** Chunk index where this NPC was last seen. Updated on successful restore. */
        public volatile long chunkIndex;
        /**
         * True while scheduleRestoreOne() is actively polling for this entity.
         * Prevents reconcileTask from launching a duplicate polling loop.
         */
        public volatile boolean restorePending = false;

        public NpcRecord(UUID entityUuid, String roleName, InteractionType interaction,
                         long skinSeed, long chunkIndex) {
            this.entityUuid = entityUuid;
            this.roleName   = roleName;
            this.interaction = interaction;
            this.skinSeed   = skinSeed;
            this.chunkIndex = chunkIndex;
        }
    }

    // uuid → record
    private final ConcurrentHashMap<UUID, NpcRecord> records = new ConcurrentHashMap<>();

    // uuid → chunkIndex: NPCs that must be deleted when their chunk next loads.
    // uuid → chunkIndex: stores chunkIndex for O(1) chunk matching on ChunkPreLoadProcessEvent.
    private final ConcurrentHashMap<UUID, Long> pendingRemovals = new ConcurrentHashMap<>();

    // World reference for reconcile task — set when first scheduleRestoreOne is called.
    // All our NPCs are in the same world so one reference is sufficient.
    private volatile World reconcileWorld = null;
    private ScheduledFuture<?> reconcileTask = null;

    private NpcRegistry() {}

    public void register(NpcRecord record) {
        records.put(record.entityUuid, record);
        LOGGER.fine("NpcRegistry registered " + record.roleName + " uuid=" + record.entityUuid
                + " chunk=" + record.chunkIndex + " interaction=" + record.interaction);
    }

    /**
     * Replaces the record for an existing NPC (e.g. after role change).
     * Preserves restorePending state from the old record so active polling isn't disrupted.
     */
    public void updateRecord(NpcRecord newRecord) {
        NpcRecord old = records.get(newRecord.entityUuid);
        if (old != null) newRecord.restorePending = old.restorePending;
        records.put(newRecord.entityUuid, newRecord);
        LOGGER.fine("NpcRegistry updated " + newRecord.roleName + " uuid=" + newRecord.entityUuid
                + " interaction=" + newRecord.interaction);
    }

    public void unregister(UUID uuid) {
        NpcRecord removed = records.remove(uuid);
        if (removed != null) {
            LOGGER.fine("NpcRegistry unregistered " + removed.roleName + " uuid=" + uuid);
        }
    }

    public void clear() {
        records.clear();
        pendingRemovals.clear();
        LOGGER.info("NpcRegistry cleared");
    }

    /** Clears NPC records only — pending removals survive to delete unloaded entities. */
    public void clearRecords() {
        records.clear();
        LOGGER.info("NpcRegistry records cleared");
    }

    public void markForRemoval(UUID uuid, long chunkIndex) {
        pendingRemovals.put(uuid, chunkIndex);
        LOGGER.fine("NpcRegistry: marked for deferred removal uuid=" + uuid);
    }

    public boolean isPendingRemoval(UUID uuid) {
        return pendingRemovals.containsKey(uuid);
    }

    public Map<UUID, Long> getPendingRemovals() {
        return Collections.unmodifiableMap(pendingRemovals);
    }

    public void clearPendingRemoval(UUID uuid) {
        pendingRemovals.remove(uuid);
    }

    public NpcRecord getRecord(UUID uuid) {
        return records.get(uuid);
    }

    public java.util.Collection<NpcRecord> allRecords() {
        return new ArrayList<>(records.values());
    }

    public List<NpcRecord> getForChunk(long chunkIndex) {
        List<NpcRecord> result = new ArrayList<>();
        for (NpcRecord r : records.values()) {
            if (r.chunkIndex == chunkIndex) result.add(r);
        }
        return result;
    }

    public void scheduleRestoreForChunk(long chunkIndex, World world) {
        List<NpcRecord> forChunk = getForChunk(chunkIndex);
        if (forChunk.isEmpty()) return;
        for (NpcRecord record : forChunk) {
            scheduleRestoreOne(record, world);
        }
    }

    /**
     * Polls world.getEntityRef(uuid) every 200ms until entity appears (or 12s timeout),
     * then restores skin + interaction on the world thread.
     *
     * On timeout: does NOT give up — reconcileTask() will retry every 1s until entity
     * is found. This handles the case where the player is far from the NPC chunk when
     * the chunk loads, so getEntityRef() returns null during the initial polling window.
     * Polls world.getEntityRef(uuid) every 200ms until entity appears (or 12s timeout),
     */
    public void scheduleRestoreOne(NpcRecord record, World world) {
        if (record.restorePending) return; // already polling
        record.restorePending = true;
        ensureReconcileTask(world);

        long startTime = System.currentTimeMillis();
        boolean[] done = {false};
        ScheduledFuture<?>[] taskRef = {null};

        taskRef[0] = TickScheduler.getExecutor().scheduleAtFixedRate(() -> {
            if (done[0]) return;

            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= POLL_TIMEOUT_MS) {
                done[0] = true;
                if (taskRef[0] != null) taskRef[0].cancel(false);
                // Mark as not pending so reconcileTask can retry when player approaches.
                record.restorePending = false;
                LOGGER.fine("NpcRegistry: initial poll timed out for " + record.roleName
                        + " uuid=" + record.entityUuid + " — reconcileTask will retry");
                return;
            }

            world.execute(() -> {
                if (done[0]) return;

                var ref = world.getEntityRef(record.entityUuid);
                if (ref == null || !ref.isValid()) return;

                done[0] = true;
                if (taskRef[0] != null) taskRef[0].cancel(false);

                LOGGER.info("NpcRegistry: restoring " + record.roleName
                        + " uuid=" + record.entityUuid
                        + " after " + (System.currentTimeMillis() - startTime) + "ms");

                applyRestoreAndUpdateChunk(ref, world, record);
            });

        }, 0L, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Background task that runs every 1s and re-restores any registered NPC whose
     * ref is not currently valid and is not already being polled.
     *
     * Background task that runs every 1s and re-restores any registered NPC —
     * makes NPCs recover even when their chunk was loaded while the player was far away
     * (causing the initial 12s poll to time out without finding the entity).
     */
    private void ensureReconcileTask(World world) {
        reconcileWorld = world;
        if (reconcileTask != null) return;
        reconcileTask = TickScheduler.getExecutor().scheduleAtFixedRate(() -> {
            World w = reconcileWorld;
            if (w == null) return;

            List<NpcRecord> unresolved = new ArrayList<>();
            for (NpcRecord r : records.values()) {
                if (!r.restorePending) unresolved.add(r);
            }
            if (unresolved.isEmpty()) return;

            w.execute(() -> {
                for (NpcRecord record : unresolved) {
                    if (records.get(record.entityUuid) == null) continue; // unregistered
                    if (record.restorePending) continue; // race: just started polling

                    var ref = w.getEntityRef(record.entityUuid);
                    if (ref == null || !ref.isValid()) continue; // chunk not loaded yet

                    // Entity is in an active chunk — restore it.
                    LOGGER.fine("NpcRegistry: reconcile restoring " + record.roleName
                            + " uuid=" + record.entityUuid);
                    applyRestoreAndUpdateChunk(ref, w, record);
                }
            });
        }, RECONCILE_INTERVAL_MS, RECONCILE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void applyRestoreAndUpdateChunk(
            com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> ref,
            World world, NpcRecord record) {
        record.restorePending = false;
        var store = world.getEntityStore().getStore();

        // Update chunkIndex to entity's actual current position.
        try {
            var tc = store.getComponent(ref,
                    com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            if (tc != null && tc.getPosition() != null) {
                var pos = tc.getPosition();
                record.chunkIndex = ChunkUtil.indexChunkFromBlock((int) pos.x, (int) pos.z);
            }
        } catch (Exception ignored) {}

        NpcRestorer.restore(ref, store, world, record);
    }

    public void stopReconcileTask() {
        if (reconcileTask != null) {
            reconcileTask.cancel(false);
            reconcileTask = null;
        }
        reconcileWorld = null;
    }

    public void updateChunk(UUID uuid, long newChunkIndex) {
        NpcRecord r = records.get(uuid);
        if (r != null) r.chunkIndex = newChunkIndex;
    }
}
