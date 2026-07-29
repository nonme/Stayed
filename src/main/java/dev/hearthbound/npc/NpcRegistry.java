package dev.hearthbound.npc;

import com.hypixel.hytale.math.util.ChunkUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.hypixel.hytale.server.core.universe.world.World;
/**
 * In-memory registry of all Stayed-managed NPCs.
 *
 * Indexed by two keys:
 *   - npcId         — stable plugin-level identity (UUID string), the primary key.
 *                     Survives respawns; written to BSON on the entity via
 *                     {@link StayedNpcIdentityComponent}.
 *   - entityUuid    — the engine UUIDComponent value of the currently spawned entity.
 *                     Changes whenever an NPC is replaced (e.g. lost-and-respawned).
 *
 * Restoration flow lives in {@link dev.hearthbound.events.NpcChunkLoadHandler}:
 * on chunk load, the handler scans entities in the chunk, matches them to records
 * by npcId (read from {@link StayedNpcIdentityComponent}), spawns fresh entities
 * for records whose entity is missing, and re-applies skin and interaction. The
 * registry itself runs no schedulers — it is a passive store.
 */
public final class NpcRegistry {

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("npc.registry");
    private static final NpcRegistry INSTANCE = new NpcRegistry();

    public static NpcRegistry get() {
        return INSTANCE;
    }

    public enum InteractionType { ELF, RESCUE, FOLLOWER, VILLAGER, NONE }

    /**
     * Persistent identity + restoration data for one NPC.
     *
     * Position fields (lastX/Y/Z) are kept up to date by {@link NpcPositionTracker}
     * and serve as the spawn-point fallback when the original entity is missing
     * after chunk load.
     *
     * Backwards-compatibility note: this class keeps every public field that
     * existed in the previous registry (entityUuid, roleName, interaction,
     * skinSeed, chunkIndex, hasPosition, lastX/Y/Z, broken, restorePending) so
     * callsites elsewhere in the mod compile unchanged. New code should prefer
     * {@link #npcId}, which is the durable identity.
     */
    public static final class NpcRecord {
        /** Stable plugin-level identity. Mirrors {@link StayedNpcIdentityComponent#getNpcId()}. */
        public volatile String npcId;
        /** Engine UUID of the currently spawned entity. Mutates when the entity is respawned. */
        public volatile UUID entityUuid;
        public volatile String roleName;
        /** Shared template role name, e.g. Villager_Human. Old saves derive this from roleName. */
        public volatile String baseRoleName;
        public final InteractionType interaction;
        /** Non-zero for villagers/rescue victims that need a skin applied. */
        public final long skinSeed;
        /** Chunk index where this NPC was last seen. Updated on position sync. */
        public volatile long chunkIndex;
        /** World that owns this NPC. Null only for legacy records before first migration. */
        public volatile UUID worldUuid;
        /** Human-readable world name for logs/save inspection. */
        public volatile String worldName;
        /** Last known world position. Valid only when hasPosition=true. */
        public volatile double lastX, lastY, lastZ;
        /** False until the first position sync. */
        public volatile boolean hasPosition;
        /** Stable original/home position used as a second chunk anchor. */
        public volatile double baseX, baseY, baseZ;
        public volatile long baseChunkIndex;
        public volatile boolean hasBasePosition;
        /** Reserved — no longer set by registry code; kept so older save files load. */
        public volatile boolean restorePending = false;
        /** Reserved — no longer set by registry code; kept so older save files load. */
        public volatile boolean broken = false;
        /** Reserved — no longer set by registry code; kept so older save files load. */
        public volatile boolean brokenNotified = false;
        /**
         * Non-null only on records created by the integration test framework.
         * Stored as the test name that spawned this NPC; lets {@code /hb test
         * cleanup} find and remove leftover test NPCs even after a server
         * restart that interrupted teardown. Persisted to data.json.
         */
        public volatile String testMarker = null;

        public NpcRecord(UUID entityUuid, String roleName, InteractionType interaction,
                         long skinSeed, long chunkIndex) {
            this(UUID.randomUUID().toString(), entityUuid, roleName, interaction, skinSeed, chunkIndex);
        }

        public NpcRecord(String npcId, UUID entityUuid, String roleName, InteractionType interaction,
                         long skinSeed, long chunkIndex) {
            this.npcId = npcId != null ? npcId : UUID.randomUUID().toString();
            this.entityUuid = entityUuid;
            this.baseRoleName = StayedRoleNames.extractBaseRoleName(roleName);
            refreshGeneratedRoleName();
            this.interaction = interaction;
            this.skinSeed = skinSeed;
            this.chunkIndex = chunkIndex;
        }

        public String baseRoleName() {
            return baseRoleName;
        }

        public void refreshGeneratedRoleName() {
            this.roleName = StayedRoleNames.generatedRoleName(this.npcId, this.baseRoleName);
        }

        public void setPosition(double x, double y, double z) {
            if (!hasBasePosition) {
                setBasePosition(x, y, z);
            }
            this.lastX = x;
            this.lastY = y;
            this.lastZ = z;
            this.hasPosition = true;
        }

        public void setBasePosition(double x, double y, double z) {
            this.baseX = x;
            this.baseY = y;
            this.baseZ = z;
            this.baseChunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            this.hasBasePosition = true;
        }

        public void copyBasePositionFrom(NpcRecord other) {
            if (other == null || !other.hasBasePosition) return;
            this.baseX = other.baseX;
            this.baseY = other.baseY;
            this.baseZ = other.baseZ;
            this.baseChunkIndex = other.baseChunkIndex;
            this.hasBasePosition = true;
        }

        public void setWorld(UUID worldUuid, String worldName) {
            this.worldUuid = worldUuid;
            this.worldName = worldName;
        }

        public boolean hasWorld() {
            return worldUuid != null;
        }

        public boolean belongsToWorld(UUID currentWorldUuid) {
            return worldUuid == null || currentWorldUuid == null || worldUuid.equals(currentWorldUuid);
        }
    }

    // npcId → record (primary index)
    private final ConcurrentHashMap<String, NpcRecord> byNpcId = new ConcurrentHashMap<>();
    // entityUuid → record (secondary lookup; mutates with entity respawns)
    private final ConcurrentHashMap<UUID, NpcRecord> byEntityUuid = new ConcurrentHashMap<>();

    private record PendingRemovalKey(UUID worldUuid, UUID entityUuid) {}

    public static final class PendingRemoval {
        public final UUID worldUuid;
        public final String worldName;
        public final UUID entityUuid;
        public final long chunkIndex;

        private PendingRemoval(UUID worldUuid, String worldName, UUID entityUuid, long chunkIndex) {
            this.worldUuid = worldUuid;
            this.worldName = worldName;
            this.entityUuid = entityUuid;
            this.chunkIndex = chunkIndex;
        }
    }

    // (worldUuid, entityUuid) → tombstone. worldUuid=null is legacy/global.
    private final ConcurrentHashMap<PendingRemovalKey, PendingRemoval> pendingRemovals = new ConcurrentHashMap<>();

    private NpcRegistry() {}

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    public void register(NpcRecord record) {
        if (record == null) return;
        byNpcId.put(record.npcId, record);
        if (record.entityUuid != null) byEntityUuid.put(record.entityUuid, record);
        LOG.debug("NpcRegistry registered " + record.roleName + " npcId=" + record.npcId
                + " entityUuid=" + record.entityUuid + " interaction=" + record.interaction);
    }

    /**
     * Convenience for spawn pathways: writes {@link StayedNpcIdentityComponent}
     * onto the freshly spawned entity and inserts the record into the registry
     * in one step. This is the entry point every spawn site should use so that
     * {@link DuplicateNpcPrevention} can see the new entity by its npcId.
     */
    public void registerWithIdentity(
            com.hypixel.hytale.component.Store<
                    com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store,
            com.hypixel.hytale.component.Ref<
                    com.hypixel.hytale.server.core.universe.world.storage.EntityStore> ref,
            NpcRecord record) {
        if (record == null) return;
        if (store != null && ref != null) {
            if (!record.hasWorld()) {
                World world = store.getExternalData() != null ? store.getExternalData().getWorld() : null;
                record.setWorld(worldUuidOf(world), worldNameOf(world));
            }
            store.putComponent(ref, StayedNpcIdentityComponent.getComponentType(),
                    new StayedNpcIdentityComponent(record.npcId));
        }
        register(record);
    }

    /** Replaces the record for an existing entityUuid. */
    public void updateRecord(NpcRecord newRecord) {
        if (newRecord == null) return;
        NpcRecord old = byEntityUuid.get(newRecord.entityUuid);
        if (old != null) {
            // Inherit the stable npcId from the prior record so persistent identity
            // is preserved even when callers construct a fresh NpcRecord with the
            // legacy 5-arg constructor (which generates a new npcId).
            newRecord.npcId = old.npcId;
            newRecord.refreshGeneratedRoleName();
            newRecord.copyBasePositionFrom(old);
            if (!newRecord.hasWorld()) {
                newRecord.setWorld(old.worldUuid, old.worldName);
            }
            byNpcId.put(old.npcId, newRecord);
        } else {
            byNpcId.put(newRecord.npcId, newRecord);
        }
        if (newRecord.entityUuid != null) byEntityUuid.put(newRecord.entityUuid, newRecord);
        LOG.debug("NpcRegistry updated " + newRecord.roleName + " npcId=" + newRecord.npcId
                + " entityUuid=" + newRecord.entityUuid);
    }

    public void unregister(UUID entityUuid) {
        if (entityUuid == null) return;
        NpcRecord removed = byEntityUuid.remove(entityUuid);
        if (removed != null) {
            byNpcId.remove(removed.npcId);
            LOG.debug("NpcRegistry unregistered " + removed.roleName + " npcId=" + removed.npcId
                    + " entityUuid=" + entityUuid);
        }
    }

    public void unregisterByNpcId(String npcId) {
        if (npcId == null || npcId.isBlank()) return;
        NpcRecord removed = byNpcId.remove(npcId);
        if (removed != null && removed.entityUuid != null) {
            byEntityUuid.remove(removed.entityUuid);
        }
    }

    public void clear() {
        byNpcId.clear();
        byEntityUuid.clear();
        pendingRemovals.clear();
        LOG.info("NpcRegistry cleared");
    }

    /** Clears NPC records only — pending removals survive to delete unloaded entities. */
    public void clearRecords() {
        byNpcId.clear();
        byEntityUuid.clear();
        LOG.info("NpcRegistry records cleared");
    }

    // -------------------------------------------------------------------------
    // Lookup
    // -------------------------------------------------------------------------

    public NpcRecord getRecord(UUID entityUuid) {
        return entityUuid != null ? byEntityUuid.get(entityUuid) : null;
    }

    public NpcRecord getRecordByNpcId(String npcId) {
        return (npcId == null || npcId.isBlank()) ? null : byNpcId.get(npcId);
    }

    public java.util.Collection<NpcRecord> allRecords() {
        return new ArrayList<>(byNpcId.values());
    }

    public List<NpcRecord> getForChunk(long chunkIndex) {
        return getForChunk(null, chunkIndex);
    }

    public List<NpcRecord> getForChunk(UUID worldUuid, long chunkIndex) {
        List<NpcRecord> result = new ArrayList<>();
        for (NpcRecord r : byNpcId.values()) {
            if (r.chunkIndex == chunkIndex && r.belongsToWorld(worldUuid)) result.add(r);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Live-entity binding
    // -------------------------------------------------------------------------

    /**
     * Re-points a record at a freshly spawned engine entity. Used after a
     * lost-NPC respawn or after migrating an old entity that did not yet carry
     * a {@link StayedNpcIdentityComponent}.
     */
    public void bindEntityUuid(String npcId, UUID newEntityUuid) {
        if (npcId == null || newEntityUuid == null) return;
        NpcRecord record = byNpcId.get(npcId);
        if (record == null) return;
        UUID oldUuid = record.entityUuid;
        if (oldUuid != null && !oldUuid.equals(newEntityUuid)) {
            byEntityUuid.remove(oldUuid);
        }
        record.entityUuid = newEntityUuid;
        byEntityUuid.put(newEntityUuid, record);
    }

    public boolean bindEntityUuid(String npcId, UUID newEntityUuid, UUID worldUuid) {
        if (npcId == null || newEntityUuid == null) return false;
        NpcRecord record = byNpcId.get(npcId);
        if (record == null || !record.belongsToWorld(worldUuid)) return false;
        bindEntityUuid(npcId, newEntityUuid);
        return true;
    }

    /**
     * Position update from {@link NpcPositionTracker}. Returns true when the
     * delta is large enough to be worth persisting.
     */
    public boolean updatePosition(UUID entityUuid, double x, double y, double z) {
        NpcRecord r = byEntityUuid.get(entityUuid);
        if (r == null) return false;
        boolean significant = !r.hasPosition
                || Math.abs(r.lastX - x) >= 1.0
                || Math.abs(r.lastZ - z) >= 1.0
                || Math.abs(r.lastY - y) >= 2.0;
        r.setPosition(x, y, z);
        r.chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        return significant;
    }

    public void updateChunk(UUID entityUuid, long newChunkIndex) {
        NpcRecord r = byEntityUuid.get(entityUuid);
        if (r != null) r.chunkIndex = newChunkIndex;
    }

    // -------------------------------------------------------------------------
    // Pending removals (deferred deletion of NPCs in unloaded chunks)
    // -------------------------------------------------------------------------

    public void markForRemoval(UUID entityUuid, long chunkIndex) {
        markForRemoval(null, null, entityUuid, chunkIndex);
    }

    public void markForRemoval(UUID worldUuid, UUID entityUuid, long chunkIndex) {
        markForRemoval(worldUuid, null, entityUuid, chunkIndex);
    }

    public void markForRemoval(UUID worldUuid, String worldName, UUID entityUuid, long chunkIndex) {
        if (entityUuid == null) return;
        pendingRemovals.put(new PendingRemovalKey(worldUuid, entityUuid),
                new PendingRemoval(worldUuid, worldName, entityUuid, chunkIndex));
        LOG.debug("NpcRegistry: marked for deferred removal entityUuid=" + entityUuid
                + " worldUuid=" + worldUuid);
    }

    public boolean isPendingRemoval(UUID entityUuid) {
        if (entityUuid == null) return false;
        for (PendingRemovalKey key : pendingRemovals.keySet()) {
            if (entityUuid.equals(key.entityUuid())) return true;
        }
        return false;
    }

    public boolean isPendingRemoval(UUID worldUuid, UUID entityUuid) {
        if (entityUuid == null) return false;
        return pendingRemovals.containsKey(new PendingRemovalKey(worldUuid, entityUuid))
                || pendingRemovals.containsKey(new PendingRemovalKey(null, entityUuid));
    }

    public Map<UUID, Long> getPendingRemovals() {
        Map<UUID, Long> flattened = new java.util.HashMap<>();
        for (PendingRemoval removal : pendingRemovals.values()) {
            flattened.put(removal.entityUuid, removal.chunkIndex);
        }
        return Collections.unmodifiableMap(flattened);
    }

    public List<PendingRemoval> getPendingRemovalEntries() {
        return new ArrayList<>(pendingRemovals.values());
    }

    public void clearPendingRemoval(UUID entityUuid) {
        if (entityUuid == null) return;
        pendingRemovals.keySet().removeIf(key -> entityUuid.equals(key.entityUuid()));
    }

    public void clearPendingRemoval(UUID worldUuid, UUID entityUuid) {
        if (entityUuid == null) return;
        pendingRemovals.remove(new PendingRemovalKey(worldUuid, entityUuid));
        pendingRemovals.remove(new PendingRemovalKey(null, entityUuid));
    }

    public static UUID worldUuidOf(World world) {
        try {
            return world != null && world.getWorldConfig() != null
                    ? world.getWorldConfig().getUuid()
                    : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String worldNameOf(World world) {
        try {
            return world != null ? world.getName() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Legacy API kept for callsite compatibility (no-ops in the new design)
    // -------------------------------------------------------------------------

    /** No-op. Legacy hook from the old reconcile loop; restoration now lives in the chunk handler. */
    public void stopReconcileTask() {}
}
