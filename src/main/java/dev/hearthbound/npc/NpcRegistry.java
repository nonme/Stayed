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
 * In-memory registry of all Hearthbound NPCs.
 * NPC state needed for restoration is stored HERE, not inside the NPC entity's BSON.
 * On ChunkPreLoadProcessEvent we know which NPCs belong to which chunk from this registry,
 * so we can start polling world.getEntityRef(uuid) without waiting for BSON deserialization.
 *
 * Every NPC spawn must call register(). Every NPC despawn must call unregister().
 */
public final class NpcRegistry {

    private static final Logger LOGGER = Logger.getLogger(NpcRegistry.class.getName());

    private static final NpcRegistry INSTANCE = new NpcRegistry();

    public static NpcRegistry get() {
        return INSTANCE;
    }

    private static final long POLL_INTERVAL_MS = 200;
    private static final long POLL_INTERVAL_MS = 200;
    private static final long POLL_TIMEOUT_MS = 12_000;

    public enum InteractionType { ELF, RESCUE, NONE }

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
        /** Chunk index where this NPC was spawned (used for ChunkPreLoad matching). */
        public volatile long chunkIndex;

        public NpcRecord(UUID entityUuid, String roleName, InteractionType interaction,
                         long skinSeed, long chunkIndex) {
            this.entityUuid    = entityUuid;
            this.roleName      = roleName;
            this.interaction   = interaction;
            this.skinSeed      = skinSeed;
            this.chunkIndex    = chunkIndex;
        }
    }

    // uuid → record
    private final ConcurrentHashMap<UUID, NpcRecord> records = new ConcurrentHashMap<>();

    // uuid → chunkIndex: NPCs that must be deleted when their chunk next loads.
    // Storing chunkIndex allows matching on ChunkPreLoadProcessEvent
    // by chunk without needing a physical entity scan.
    private final ConcurrentHashMap<UUID, Long> pendingRemovals = new ConcurrentHashMap<>();

    private NpcRegistry() {}

    public void register(NpcRecord record) {
        records.put(record.entityUuid, record);
        LOGGER.fine("NpcRegistry registered " + record.roleName + " uuid=" + record.entityUuid
                + " chunk=" + record.chunkIndex + " interaction=" + record.interaction);
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

    /**
     * Marks a UUID for deletion when its chunk next loads.
     * Use when the entity's chunk is not currently loaded so world.getEntity() returns null.
     * uuid → chunkIndex: stores chunkIndex for O(1) chunk matching on ChunkPreLoadProcessEvent.
     */
    public void markForRemoval(UUID uuid, long chunkIndex) {
        pendingRemovals.put(uuid, chunkIndex);
        LOGGER.fine("NpcRegistry: marked for deferred removal uuid=" + uuid);
    }

    public boolean isPendingRemoval(UUID uuid) {
        return pendingRemovals.containsKey(uuid);
    }

    /** Returns uuid → chunkIndex snapshot of all pending removals. */
    public Map<UUID, Long> getPendingRemovals() {
        return Collections.unmodifiableMap(pendingRemovals);
    }

    public void clearPendingRemoval(UUID uuid) {
        pendingRemovals.remove(uuid);
    }

    /** Returns the record for the given UUID, or null if not registered. */
    public NpcRecord getRecord(UUID uuid) {
        return records.get(uuid);
    }

    /** Returns a snapshot of all registered records. */
    public java.util.Collection<NpcRecord> allRecords() {
        return new ArrayList<>(records.values());
    }

    /** Returns all records whose last-known chunk matches the given index. */
    public List<NpcRecord> getForChunk(long chunkIndex) {
        List<NpcRecord> result = new ArrayList<>();
        for (NpcRecord r : records.values()) {
            if (r.chunkIndex == chunkIndex) result.add(r);
        }
        return result;
    }

    /**
     * For a freshly loaded chunk: for each registered NPC that belongs to this chunk,
     * poll world.getEntityRef(uuid) every 200ms until the entity appears (or 12s timeout),
     * then restore skin + interaction on the world thread.
     *
     * Polls world.getEntityRef(uuid) every 200ms until entity appears,
     * then restores skin and interaction on the world thread.
     */
    public void scheduleRestoreForChunk(long chunkIndex, World world) {
        List<NpcRecord> forChunk = getForChunk(chunkIndex);
        if (forChunk.isEmpty()) return;

        LOGGER.info("NpcRegistry: chunk " + chunkIndex + " loaded, scheduling restore for "
                + forChunk.size() + " NPC(s)");

        for (NpcRecord record : forChunk) {
            scheduleRestoreOne(record, world);
        }
    }

    public void scheduleRestoreOne(NpcRecord record, World world) {
        long startTime = System.currentTimeMillis();
        boolean[] done = {false};
        ScheduledFuture<?>[] taskRef = {null};

        taskRef[0] = TickScheduler.getExecutor().scheduleAtFixedRate(() -> {
            if (done[0]) return;

            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= POLL_TIMEOUT_MS) {
                done[0] = true;
                if (taskRef[0] != null) taskRef[0].cancel(false);
                LOGGER.warning("NpcRegistry: timeout restoring " + record.roleName
                        + " uuid=" + record.entityUuid + " after " + elapsed + "ms");
                return;
            }

            world.execute(() -> {
                if (done[0]) return;

                var ref = world.getEntityRef(record.entityUuid);
                if (ref == null || !ref.isValid()) return; // not in registry yet — keep polling

                done[0] = true;
                if (taskRef[0] != null) taskRef[0].cancel(false);

                LOGGER.info("NpcRegistry: restoring " + record.roleName
                        + " uuid=" + record.entityUuid
                        + " after " + (System.currentTimeMillis() - startTime) + "ms");

                var store = world.getEntityStore().getStore();
                NpcRestorer.restore(ref, store, world, record);
            });

        }, 0L, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Updates the stored chunk index for an NPC that has moved to a different chunk.
     * Call this whenever an NPC teleports or is respawned at a new position.
     */
    public void updateChunk(UUID uuid, long newChunkIndex) {
        NpcRecord r = records.get(uuid);
        if (r != null) r.chunkIndex = newChunkIndex;
    }
}
