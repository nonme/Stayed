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
 * - NPC state stored here, not in BSON
 * - ChunkPreLoadProcessEvent triggers restore polling via scheduleRestoreOne()
 * - reconcileTask() runs every 1s and re-restores any NPC whose ref became invalid —
 *   this is the fix for getEntityRef() timeout when the player is far from the NPC chunk
 * - pendingRemovals: deferred deletion for NPCs in unloaded chunks
 */
public final class NpcRegistry {

    private static final Logger LOGGER = Logger.getLogger(NpcRegistry.class.getName());

    private static final NpcRegistry INSTANCE = new NpcRegistry();

    public static NpcRegistry get() {
        return INSTANCE;
    }

    private static final long POLL_INTERVAL_MS      = 200;
    private static final long POLL_TIMEOUT_MS       = 12_000;
    private static final long RECONCILE_INTERVAL_MS = 1_000;

    public enum InteractionType { ELF, RESCUE, FOLLOWER, VILLAGER, NONE }

    /**
     * Everything needed to restore — or fully respawn — an NPC.
     * Stored outside BSON — survives chunk unload without any BSON dependency.
     *
     * Position fields (lastX/Y/Z) are updated continuously by NpcPositionTracker and
     * give us a respawn point if the original entity is lost (entity removed, BSON
     * corruption, world wipe). chunkIndex is derived from the position.
     */
    public static final class NpcRecord {
        public final UUID entityUuid;
        public final String roleName;
        public final InteractionType interaction;
        /** Non-zero for villagers/rescue victims that need a skin applied. */
        public final long skinSeed;
        /** Chunk index where this NPC was last seen. Updated on successful restore and on position sync. */
        public volatile long chunkIndex;
        /** Last known world position. Valid only when hasPosition=true. */
        public volatile double lastX, lastY, lastZ;
        /** False until the first position sync — old records loaded from disk start out without coords. */
        public volatile boolean hasPosition;
        /**
         * True while scheduleRestoreOne() is actively polling for this entity.
         * Prevents reconcileTask from launching a duplicate polling loop.
         */
        public volatile boolean restorePending = false;
        /**
         * Circuit breaker state for the respawn fallback. After too many failed
         * respawns, we mark the NPC broken and stop trying so a single bad role
         * config or corrupt data can't burn CPU forever.
         */
        public volatile int respawnFailCount = 0;
        public volatile long firstFailTimeMs = 0L;
        public volatile boolean broken = false;
        /** Set once we've notified the player about a broken NPC, to avoid chat-spamming. */
        public volatile boolean brokenNotified = false;

        public NpcRecord(UUID entityUuid, String roleName, InteractionType interaction,
                         long skinSeed, long chunkIndex) {
            this.entityUuid = entityUuid;
            this.roleName   = roleName;
            this.interaction = interaction;
            this.skinSeed   = skinSeed;
            this.chunkIndex = chunkIndex;
        }

        public void setPosition(double x, double y, double z) {
            this.lastX = x;
            this.lastY = y;
            this.lastZ = z;
            this.hasPosition = true;
        }
    }

    // uuid → record
    private final ConcurrentHashMap<UUID, NpcRecord> records = new ConcurrentHashMap<>();

    // uuid → chunkIndex: NPCs that must be deleted when their chunk next loads.
    // Stores chunkIndex for O(1) chunk matching on ChunkPreLoadProcessEvent.
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
     * Handles NPCs whose chunk was loaded while the player was far away, causing
     * the initial 12s poll to time out before the entity became accessible.
     */
    private void ensureReconcileTask(World world) {
        reconcileWorld = world;
        if (reconcileTask != null) return;
        reconcileTask = TickScheduler.getExecutor().scheduleAtFixedRate(() -> {
            World w = reconcileWorld;
            if (w == null) return;

            List<NpcRecord> unresolved = new ArrayList<>();
            for (NpcRecord r : records.values()) {
                if (!r.restorePending && !r.broken) unresolved.add(r);
            }
            if (unresolved.isEmpty()) return;

            w.execute(() -> {
                for (NpcRecord record : unresolved) {
                    if (records.get(record.entityUuid) == null) continue; // unregistered
                    if (record.restorePending) continue; // race: just started polling

                    var ref = w.getEntityRef(record.entityUuid);
                    if (ref != null && ref.isValid()) {
                        // Entity is in an active chunk — restore it.
                        LOGGER.fine("NpcRegistry: reconcile restoring " + record.roleName
                                + " uuid=" + record.entityUuid);
                        applyRestoreAndUpdateChunk(ref, w, record);
                        continue;
                    }

                    // Entity ref invalid — chunk where the NPC was last seen is
                    // not currently in memory. Kick off a force-load + retry on
                    // a separate task so the reconcile tick stays cheap.
                    //
                    // We don't gate on hasPosition: chunkIndex is always present
                    // (set on spawn / load from disk), and old records loaded
                    // before the position-tracking change have valid chunkIndex
                    // but no lastX/Y/Z. Such records still need to be recovered
                    // — the respawn fallback will use VillageData heuristics
                    // (assigned house → founding stone) to pick a position.
                    scheduleForceLoadAndRetry(record, w);
                }
            });
        }, RECONCILE_INTERVAL_MS, RECONCILE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Force-loads the chunk containing the NPC's last known position, then
     * retries getEntityRef. If that still fails after the chunk has had time
     * to deserialise, escalates to a respawn through NpcRespawner.
     *
     * Uses restorePending as a mutex so we don't fire multiple parallel loads
     * for the same NPC. The flag is cleared in either branch (restore or
     * respawn) so the next reconcile tick can act on a fresh state.
     */
    private void scheduleForceLoadAndRetry(NpcRecord record, World world) {
        if (!record.restorePending) {
            record.restorePending = true;
        } else {
            return;
        }
        long targetChunk = record.chunkIndex;
        world.getChunkAsync(targetChunk).whenComplete((chunk, error) -> {
            if (error != null) {
                LOGGER.warning("NpcRegistry: force-load failed for " + record.entityUuid
                        + " chunk=" + targetChunk + ": " + error.getMessage());
                record.restorePending = false;
                return;
            }
            // Give the chunk a moment to finish deserialising entities, then check.
            TickScheduler.getExecutor().schedule(() -> world.execute(() -> {
                record.restorePending = false;
                var ref = world.getEntityRef(record.entityUuid);
                if (ref != null && ref.isValid()) {
                    LOGGER.info("NpcRegistry: force-load recovered " + record.roleName
                            + " uuid=" + record.entityUuid + " in chunk " + targetChunk);
                    applyRestoreAndUpdateChunk(ref, world, record);
                    return;
                }
                // Entity is gone — respawn from the saved position. We pick the
                // position here on the world thread because pickRespawnPosition
                // touches village data which lives on a player ref.
                LOGGER.warning("NpcRegistry: entity not found in loaded chunk "
                        + targetChunk + " for " + record.entityUuid + " — respawning");
                attemptRespawn(record, world);
            }), 1500L, TimeUnit.MILLISECONDS);
        });
    }

    /**
     * Find the village owner whose data references this NPC, then have
     * NpcRespawner replace the entity. We need the player ref because
     * VillageData is a component on the player entity.
     */
    private void attemptRespawn(NpcRecord record, World world) {
        var store = world.getEntityStore().getStore();
        com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> playerRef =
                findPlayerOwningNpc(world, store, record.entityUuid);
        com.hypixel.hytale.math.vector.Vector3d pos = NpcRespawner.pickRespawnPosition(playerRef, store, record);
        if (pos == null) {
            LOGGER.warning("NpcRegistry: no respawn position for " + record.entityUuid + " — skipping");
            recordRespawnFailure(record.entityUuid);
            return;
        }
        // Fire-and-forget: respawn() returns a future; we log the outcome but
        // don't block. The world thread is freed immediately so chunk loads
        // can complete (blocking on getChunkAsync().join() from inside a
        // world.execute() deadlocks the server).
        UUID uuid = record.entityUuid;
        NpcRespawner.respawn(world, store, playerRef, record, pos)
                .thenAccept(result -> {
                    if (!result.ok) {
                        LOGGER.warning("NpcRegistry: respawn failed for " + uuid + ": " + result.reason);
                        recordRespawnFailure(uuid);
                    }
                });
    }

    /**
     * Walks player entities to find the one whose VillageData references this NPC.
     * Single-player worlds have one player; this is O(villagers) per call but only
     * runs on respawn, which is rare. Returns null if no owner found — respawner
     * will then fall back to the founding-stone heuristic.
     */
    private com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore>
            findPlayerOwningNpc(World world,
                    com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store,
                    UUID npcUuid) {
        try {
            var universe = com.hypixel.hytale.server.core.universe.Universe.get();
            for (var playerRef : universe.getPlayers()) {
                if (playerRef == null) continue;
                UUID playerUuid = playerRef.getUuid();
                if (playerUuid == null) continue;
                var entityRef = world.getEntityRef(playerUuid);
                if (entityRef == null || !entityRef.isValid()) continue;
                var village = store.getComponent(entityRef, dev.hearthbound.village.VillageData.getComponentType());
                if (village == null) continue;
                if (npcUuid.equals(village.getElfId())) return entityRef;
                for (var summary : village.getVillagers()) {
                    if (npcUuid.equals(summary.getVillagerUuid())) return entityRef;
                }
                for (var building : village.getBuildings()) {
                    if (npcUuid.equals(building.getAssignedVillagerId())) return entityRef;
                }
            }
        } catch (Exception e) {
            LOGGER.fine("findPlayerOwningNpc failed: " + e.getMessage());
        }
        return null;
    }

    private void applyRestoreAndUpdateChunk(
            com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> ref,
            World world, NpcRecord record) {
        record.restorePending = false;
        var store = world.getEntityStore().getStore();

        // Sync chunkIndex + lastX/Y/Z to entity's actual current position.
        // hasPosition=true after this means future restores can be skipped if the
        // chunk loads through Pass 2 of NpcChunkLoadHandler — the position is
        // authoritative regardless of where chunkIndex points.
        try {
            var tc = store.getComponent(ref,
                    com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            if (tc != null && tc.getPosition() != null) {
                var pos = tc.getPosition();
                record.setPosition(pos.x, pos.y, pos.z);
                record.chunkIndex = ChunkUtil.indexChunkFromBlock((int) pos.x, (int) pos.z);
            }
        } catch (Exception ignored) {}

        // Successful restore clears any prior failure history.
        record.respawnFailCount = 0;
        record.firstFailTimeMs = 0L;
        record.broken = false;
        record.brokenNotified = false;

        NpcRestorer.restore(ref, store, world, record);
    }

    /**
     * Updates the cached world position for an NPC. Called by NpcPositionTracker
     * every few seconds for live entities. The chunkIndex is recomputed from x/z
     * so it tracks where the NPC actually is, not where it was when last restored.
     *
     * @return true if the position changed enough to warrant persisting (>= 1 block
     *         of horizontal movement), so the caller can mark data dirty.
     */
    public boolean updatePosition(UUID uuid, double x, double y, double z) {
        NpcRecord r = records.get(uuid);
        if (r == null) return false;
        boolean significant = !r.hasPosition
                || Math.abs(r.lastX - x) >= 1.0
                || Math.abs(r.lastZ - z) >= 1.0
                || Math.abs(r.lastY - y) >= 2.0;
        r.setPosition(x, y, z);
        r.chunkIndex = ChunkUtil.indexChunkFromBlock((int) x, (int) z);
        return significant;
    }

    /**
     * Records a failed respawn attempt and trips the circuit breaker after
     * MAX_RESPAWN_FAILS within RESPAWN_FAIL_WINDOW_MS. Once broken, no further
     * respawns are attempted for this NPC.
     */
    public void recordRespawnFailure(UUID uuid) {
        NpcRecord r = records.get(uuid);
        if (r == null) return;
        long now = System.currentTimeMillis();
        if (r.firstFailTimeMs == 0L || now - r.firstFailTimeMs > RESPAWN_FAIL_WINDOW_MS) {
            r.firstFailTimeMs = now;
            r.respawnFailCount = 1;
        } else {
            r.respawnFailCount++;
        }
        if (r.respawnFailCount >= MAX_RESPAWN_FAILS) {
            r.broken = true;
            LOGGER.warning("NpcRegistry: NPC " + uuid + " (" + r.roleName + ") tripped circuit breaker after "
                    + r.respawnFailCount + " failed respawns — marking broken");
        }
    }

    private static final int MAX_RESPAWN_FAILS = 3;
    private static final long RESPAWN_FAIL_WINDOW_MS = 5 * 60 * 1000L;

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
