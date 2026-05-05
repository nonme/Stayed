package dev.hearthbound.test.audit;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.math.util.ChunkUtil;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.npc.StayedNpcIdentityComponent;
import dev.hearthbound.quest.RescueQuestManager;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Read-only scan of every NPC-lifecycle invariant we care about. See TESTING.md §4.
 *
 * Never mutates state — that would make the audit its own bug source. Returns
 * an {@link AuditResult} that is either clean or carries a list of violations
 * with enough detail to identify the offending entity / record.
 *
 * Safe to call from any thread; iterates the entity store via {@code forEachChunk}
 * which handles its own locking.
 */
public final class NpcRegistryInvariantAudit {

    private NpcRegistryInvariantAudit() {}

    public static AuditResult run(World world, Store<EntityStore> store) {
        long startNanos = System.nanoTime();
        List<Violation> violations = new ArrayList<>();

        // First pass: walk every loaded NPC entity and collect its identity component.
        // This gives us the live set of (npcId → entityUuids) and (entityUuid → npcId).
        Map<String, List<UUID>> npcIdToEntityUuids = new HashMap<>();
        Map<UUID, String> entityUuidToNpcId = new HashMap<>();
        Map<UUID, Long> entityUuidToObservedChunk = new HashMap<>();
        Map<UUID, String> entityUuidToDetails = new HashMap<>();

        Archetype<EntityStore> npcQuery = Archetype.of(NPCEntity.getComponentType());
        store.forEachChunk(npcQuery, (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(i);
                StayedNpcIdentityComponent identity = store.getComponent(
                        ref, StayedNpcIdentityComponent.getComponentType());
                UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
                if (uc == null) continue;
                UUID entityUuid = uc.getUuid();
                if (entityUuid == null) continue;

                TransformComponent transform = store.getComponent(
                        ref, TransformComponent.getComponentType());
                if (transform != null && transform.getPosition() != null) {
                    var pos = transform.getPosition();
                    long observedChunk = ChunkUtil.indexChunkFromBlock(
                            pos.getX(), pos.getZ());
                    entityUuidToObservedChunk.put(entityUuid, observedChunk);
                }
                NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
                entityUuidToDetails.put(entityUuid, describeEntity(npc, transform));

                if (identity == null) continue;
                String npcId = identity.getNpcId();
                if (npcId == null || npcId.isBlank()) continue;

                npcIdToEntityUuids.computeIfAbsent(npcId, k -> new ArrayList<>()).add(entityUuid);
                entityUuidToNpcId.put(entityUuid, npcId);
            }
        });

        // Invariant 1: DUPLICATE_ID — two or more loaded entities with the same npcId.
        for (Map.Entry<String, List<UUID>> e : npcIdToEntityUuids.entrySet()) {
            if (e.getValue().size() > 1) {
                violations.add(new Violation(
                        ViolationType.DUPLICATE_ID,
                        e.getKey(),
                        null,
                        "loaded entities sharing npcId: " + e.getValue()));
            }
        }

        NpcRegistry registry = NpcRegistry.get();

        // Invariant 2: ORPHAN_ENTITY — entity claims an npcId not in the registry.
        for (Map.Entry<UUID, String> e : entityUuidToNpcId.entrySet()) {
            String npcId = e.getValue();
            if (registry.getRecordByNpcId(npcId) == null) {
                violations.add(new Violation(
                        ViolationType.ORPHAN_ENTITY,
                        npcId,
                        e.getKey(),
                        "entity carries HB_NPCID but registry has no matching record; "
                                + entityUuidToDetails.getOrDefault(e.getKey(), "no live details")));
            }
        }

        // Snapshot of all records up front — we walk them several times below.
        List<NpcRegistry.NpcRecord> allRecords = new ArrayList<>(registry.allRecords());

        // Invariant 3: MISSING_ENTITY — record's chunk is loaded but no entity matches.
        // We approximate "chunk loaded" as: at least one entity in that chunk index
        // surfaced in our forEachChunk scan. If the registry's record points to a
        // chunk we never observed, the chunk is unloaded and the NPC is legitimately
        // asleep — skip.
        Set<Long> observedChunks = new HashSet<>(entityUuidToObservedChunk.values());
        for (NpcRegistry.NpcRecord record : allRecords) {
            if (record.entityUuid == null) continue;
            if (RescueQuestManager.isQuestEnemyRecord(record)) continue;
            if (!observedChunks.contains(record.chunkIndex)) continue;

            Ref<EntityStore> ref = world.getEntityRef(record.entityUuid);
            boolean entityLive = ref != null && ref.isValid();
            boolean entityFoundInScan = entityUuidToNpcId.containsKey(record.entityUuid);
            if (!entityLive && !entityFoundInScan) {
                violations.add(new Violation(
                        ViolationType.MISSING_ENTITY,
                        record.npcId,
                        record.entityUuid,
                        "registry chunk " + record.chunkIndex
                                + " has loaded entities but none match this record; "
                                + describeRecord(record)));
            }
        }

        // Invariant 4: POSITION_DRIFT — live entity is far from the chunk the
        // registry record claims. NpcPositionTracker syncs aggressively, so a 1–2
        // chunk drift between syncs is normal (Wander behavior moves NPCs across
        // boundaries between updates). We only flag a drift if the entity is
        // more than DRIFT_TOLERANCE_CHUNKS away — that's the size of "tracker
        // is broken" or "NPC reloaded to the wrong place".
        final int DRIFT_TOLERANCE_CHUNKS = 3;
        for (NpcRegistry.NpcRecord record : allRecords) {
            if (record.entityUuid == null) continue;
            Long observedChunk = entityUuidToObservedChunk.get(record.entityUuid);
            if (observedChunk == null) continue; // entity not loaded — nothing to compare
            if (observedChunk.longValue() == record.chunkIndex) continue;
            int dx = ChunkUtil.xOfChunkIndex(observedChunk)
                    - ChunkUtil.xOfChunkIndex(record.chunkIndex);
            int dz = ChunkUtil.zOfChunkIndex(observedChunk)
                    - ChunkUtil.zOfChunkIndex(record.chunkIndex);
            int chebyshev = Math.max(Math.abs(dx), Math.abs(dz));
            if (chebyshev <= DRIFT_TOLERANCE_CHUNKS) continue;
            violations.add(new Violation(
                    ViolationType.POSITION_DRIFT,
                    record.npcId,
                    record.entityUuid,
                    "entity at chunk " + observedChunk
                            + ", registry says " + record.chunkIndex
                            + " (drift=" + chebyshev + " chunks)"));
        }

        // Invariant 5: STALE_VILLAGE_REF — a building.assignedVillagerId points to no
        // record in either index. Walk every player's village data.
        try {
            Universe universe = Universe.get();
            for (var playerRef : universe.getPlayers()) {
                if (playerRef == null) continue;
                UUID playerUuid = playerRef.getUuid();
                if (playerUuid == null) continue;
                Ref<EntityStore> entityPlayerRef = world.getEntityRef(playerUuid);
                if (entityPlayerRef == null || !entityPlayerRef.isValid()) continue;

                VillageData village = store.getComponent(
                        entityPlayerRef, VillageData.getComponentType());
                if (village == null) continue;

                for (BuildingRecord b : village.getBuildings()) {
                    UUID assigned = b.getAssignedVillagerId();
                    if (assigned == null) continue;
                    if (registry.getRecord(assigned) != null) continue;
                    // Maybe the entity got a new UUID after respawn — search by villager
                    // summary instead. If summary's UUID resolves, the building reference
                    // itself is the stale one (registry already updated).
                    boolean resolvedViaSummary = false;
                    if (VillageManager.get().findVillagerSummary(village, assigned) != null) {
                        // Summary still keys on the same UUID — registry has nothing.
                        resolvedViaSummary = false;
                    }
                    if (!resolvedViaSummary) {
                        violations.add(new Violation(
                                ViolationType.STALE_VILLAGE_REF,
                                null,
                                assigned,
                                "building " + b.getType() + " at "
                                        + b.getPosX() + "," + b.getPosY() + "," + b.getPosZ()
                                        + " assigned to a villager with no registry record"));
                    }
                }
            }
        } catch (Exception ex) {
            // Universe access can throw before world is fully booted — record as a
            // soft note rather than crashing the audit.
            violations.add(new Violation(
                    ViolationType.STALE_VILLAGE_REF,
                    null, null,
                    "STALE_VILLAGE_REF check threw: " + ex.getMessage()));
        }

        // Invariant 6: DUPLICATE_NPCID_IN_REGISTRY — two records by entityUuid that
        // resolve to the same npcId. The registry is supposed to keep these 1:1.
        Map<String, List<UUID>> npcIdInRegistry = new HashMap<>();
        for (NpcRegistry.NpcRecord record : allRecords) {
            if (record.entityUuid == null) continue;
            npcIdInRegistry.computeIfAbsent(record.npcId, k -> new ArrayList<>())
                    .add(record.entityUuid);
        }
        for (Map.Entry<String, List<UUID>> e : npcIdInRegistry.entrySet()) {
            if (e.getValue().size() > 1) {
                violations.add(new Violation(
                        ViolationType.DUPLICATE_NPCID_IN_REGISTRY,
                        e.getKey(),
                        null,
                        "registry holds " + e.getValue().size()
                                + " records with same npcId, entityUuids=" + e.getValue()));
            }
        }

        // Invariant 7: INDEX_DESYNC — for every record, the byEntityUuid lookup
        // must return the same record reference.
        for (NpcRegistry.NpcRecord record : allRecords) {
            if (record.entityUuid == null) continue;
            NpcRegistry.NpcRecord byEntity = registry.getRecord(record.entityUuid);
            if (byEntity != record) {
                violations.add(new Violation(
                        ViolationType.INDEX_DESYNC,
                        record.npcId,
                        record.entityUuid,
                        "byNpcId record != byEntityUuid record"));
            }
        }

        long durationNanos = System.nanoTime() - startNanos;
        return new AuditResult(violations, durationNanos);
    }

    private static String describeRecord(NpcRegistry.NpcRecord record) {
        if (record == null) return "record=null";
        StringBuilder sb = new StringBuilder();
        sb.append("role=").append(record.roleName)
                .append(" interaction=").append(record.interaction)
                .append(" lastChunk=").append(record.chunkIndex);
        if (record.hasPosition) {
            sb.append(" lastPos=")
                    .append(format(record.lastX)).append(",")
                    .append(format(record.lastY)).append(",")
                    .append(format(record.lastZ));
        } else {
            sb.append(" lastPos=<none>");
        }
        if (record.testMarker != null && !record.testMarker.isBlank()) {
            sb.append(" testMarker=").append(record.testMarker);
        }
        return sb.toString();
    }

    private static String describeEntity(NPCEntity npc, TransformComponent transform) {
        String role = "?";
        try {
            if (npc != null && npc.getRole() != null) {
                role = npc.getRole().getRoleName();
            }
        } catch (Exception ignored) {
            role = "?";
        }
        String pos = "<none>";
        long chunk = Long.MIN_VALUE;
        if (transform != null && transform.getPosition() != null) {
            var p = transform.getPosition();
            pos = format(p.getX()) + "," + format(p.getY()) + "," + format(p.getZ());
            chunk = ChunkUtil.indexChunkFromBlock(p.getX(), p.getZ());
        }
        return "role=" + role + " pos=" + pos
                + (chunk != Long.MIN_VALUE ? " chunk=" + chunk : "");
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
