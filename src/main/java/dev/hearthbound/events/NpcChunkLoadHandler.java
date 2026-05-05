package dev.hearthbound.events;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.EntityChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hearthbound.npc.ElfNpcComponent;
import dev.hearthbound.npc.HearthboundDataStore;
import dev.hearthbound.npc.NpcLiveEntityResolver;
import dev.hearthbound.npc.NpcManager;
import dev.hearthbound.npc.NpcPositionTracker;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.npc.NpcRestorer;
import dev.hearthbound.npc.StayedNpcIdentityComponent;
import dev.hearthbound.util.TickScheduler;
import it.unimi.dsi.fastutil.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Primary NPC restoration path. Reacts to {@link ChunkPreLoadProcessEvent} and:
 *
 * 1. Sanitises any {@link PersistentModel} with broken scale (engine bug guard).
 * 2. Processes deferred removals queued for this chunk.
 * 3. Walks the chunk's entity holders and matches each NPC to a registry record
 *    via its {@link StayedNpcIdentityComponent} (the durable backlink).
 *    - Match found            → schedule restoration (skin + interaction).
 *    - Identity but no record → orphan, queued for engine-level removal.
 *    - Legacy elf entity      → migrate {@code HB_ELF} into {@code HB_NPCID}
 *                                so future scans find it through the new index.
 * 4. Collects restore candidates the same way HyCitizens does: current saved
 *    chunk, base/home chunk, and entities already present in the chunk holders.
 *    Missing candidates are resolved by npcId/UUID before any fresh spawn is
 *    allowed.
 */
@SuppressWarnings("rawtypes")
public class NpcChunkLoadHandler {

    private static final Logger LOGGER = Logger.getLogger(NpcChunkLoadHandler.class.getName());
    private static final int MAX_FRESH_SPAWN_ATTEMPTS = 40;
    private static final int MAX_PENDING_REMOVAL_ATTEMPTS = 240;
    private static final long FRESH_SPAWN_RESOLVE_TIMEOUT_MS = 15_000L;
    private static final long FRESH_SPAWN_RETRY_MS = 500L;
    private static final Set<String> freshSpawnsInFlight = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> pendingRemovalTasks = ConcurrentHashMap.newKeySet();

    public static void onChunkLoad(ChunkPreLoadProcessEvent event) {
        WorldChunk chunk = event.getChunk();
        if (chunk == null) return;
        World world = chunk.getWorld();
        if (world == null) return;

        sanitizeChunkPersistentModels(event);

        long chunkIndex = chunk.getIndex();
        NpcRegistry registry = NpcRegistry.get();
        NpcPositionTracker.requestSync(world);

        Set<String> seenNpcIds = new HashSet<>();
        List<UUID> orphansToRemove = new ArrayList<>();
        boolean dirty = false;

        Holder chunkHolder = event.getHolder();
        EntityChunk entityChunk = chunkHolder != null
                ? (EntityChunk) chunkHolder.getComponent(EntityChunk.getComponentType())
                : null;

        Set<NpcRegistry.NpcRecord> chunkCandidates = collectChunkRecords(registry, chunkIndex, entityChunk);

        if (entityChunk != null) {
            int scanned = 0;
            for (Holder entityHolder : entityChunk.getEntityHolders()) {
                if (entityHolder == null) continue;
                NPCEntity npc = (NPCEntity) entityHolder.getComponent(NPCEntity.getComponentType());
                if (npc == null) continue;
                UUIDComponent uc = (UUIDComponent) entityHolder.getComponent(UUIDComponent.getComponentType());
                if (uc == null) continue;
                UUID entityUuid = uc.getUuid();
                scanned++;

                // Deferred removal takes precedence — we don't want to revive a
                // marked-for-deletion entity by re-applying skin/interactions.
                if (registry.isPendingRemoval(entityUuid)) {
                    LOGGER.info("[CHUNKLOAD] chunk=" + chunkIndex + " entity=" + entityUuid
                            + " action=PENDING_REMOVAL");
                    schedulePendingRemoval(entityUuid, world);
                    dirty = true;
                    continue;
                }

                String npcId = readNpcIdFromHolder(entityHolder);

                if (npcId == null) {
                    // Migration path: an entity from before the identity component
                    // existed, recognised by the legacy ElfNpcComponent or by a
                    // direct entityUuid match in the registry.
                    npcId = migrateLegacyEntity(entityHolder, entityUuid, registry);
                    if (npcId != null) {
                        entityChunk.markNeedsSaving();
                        dirty = true;
                    }
                }

                if (npcId == null) {
                    String roleName = npc.getRole() != null ? npc.getRole().getRoleName() : "?";
                    LOGGER.info("[CHUNKLOAD] chunk=" + chunkIndex + " entity=" + entityUuid
                            + " role=" + roleName + " action=NO_NPCID");
                    continue;
                }

                NpcRegistry.NpcRecord record = registry.getRecordByNpcId(npcId);
                if (record == null) {
                    // The entity insists it belongs to us but the registry has
                    // forgotten it. Treat as an orphan and remove on the world
                    // thread once the entity ref becomes valid.
                    LOGGER.warning("[CHUNKLOAD] chunk=" + chunkIndex + " entity=" + entityUuid
                            + " npcId=" + npcId + " action=ORPHAN_REMOVE");
                    orphansToRemove.add(entityUuid);
                    continue;
                }
                chunkCandidates.add(record);

                boolean alreadySeen = seenNpcIds.contains(npcId);
                seenNpcIds.add(npcId);

                // Re-bind the record to the engine UUID present in this chunk.
                boolean rebind = !entityUuid.equals(record.entityUuid);
                if (rebind) {
                    registry.bindEntityUuid(npcId, entityUuid);
                    dirty = true;
                }

                LOGGER.info("[CHUNKLOAD] chunk=" + chunkIndex + " entity=" + entityUuid
                        + " npcId=" + npcId
                        + " action=" + (alreadySeen ? "DUPLICATE_IN_CHUNK" : "RESTORE")
                        + " rebind=" + rebind
                        + " recordEntityUuid=" + record.entityUuid);

                scheduleRestore(world, npcId, entityUuid, record);
            }
            if (scanned > 0) {
                LOGGER.info("[CHUNKLOAD] chunk=" + chunkIndex + " scannedNpcs=" + scanned
                        + " uniqueNpcIds=" + seenNpcIds.size());
            }
        }

        // HyCitizens-style candidate pass: do not trust only one saved chunk.
        // Process records whose current position, base/home position, or chunk
        // holder identity links them to this loading chunk.
        for (NpcRegistry.NpcRecord record : chunkCandidates) {
            if (seenNpcIds.contains(record.npcId)) continue;
            // Goblins and other NONE-interaction extras are disposable — we
            // record them for housekeeping but never resurrect them.
            if (record.interaction == NpcRegistry.InteractionType.NONE) continue;
            if (!record.hasPosition) continue;
            scheduleFreshSpawn(world, record, chunkIndex);
        }

        for (UUID orphanUuid : orphansToRemove) {
            schedulePendingRemoval(orphanUuid, world);
        }

        if (dirty) {
            HearthboundDataStore.get().markDirty();
        }
    }

    // -------------------------------------------------------------------------
    // Holder helpers
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static String readNpcIdFromHolder(Holder entityHolder) {
        StayedNpcIdentityComponent identity = (StayedNpcIdentityComponent)
                entityHolder.getComponent(StayedNpcIdentityComponent.getComponentType());
        if (identity == null) return null;
        String npcId = identity.getNpcId();
        return (npcId == null || npcId.isBlank()) ? null : npcId;
    }

    /**
     * Tries to attach a {@link StayedNpcIdentityComponent} to an old entity that
     * predates the new identity scheme.
     *
     * Strategy:
     *   - If the entity already lives in the registry under its engine UUID, reuse
     *     the stable npcId from that record (covers villagers, rescue NPCs).
     *   - Otherwise fall back to the legacy {@link ElfNpcComponent} marker —
     *     the elf is the only NPC type that previously had a persistent BSON
     *     marker. Reuse its engine UUID as the npcId so old data.json records
     *     match up.
     *
     * Returns the assigned npcId, or null if migration was not possible.
     */
    @SuppressWarnings("unchecked")
    private static String migrateLegacyEntity(Holder entityHolder, UUID entityUuid, NpcRegistry registry) {
        NpcRegistry.NpcRecord existing = registry.getRecord(entityUuid);
        String npcId;
        if (existing != null) {
            npcId = existing.npcId;
        } else {
            ElfNpcComponent legacyElf = (ElfNpcComponent)
                    entityHolder.getComponent(ElfNpcComponent.getComponentType());
            if (legacyElf == null) return null;
            npcId = entityUuid.toString();
        }
        entityHolder.putComponent(StayedNpcIdentityComponent.getComponentType(),
                new StayedNpcIdentityComponent(npcId));
        // Drop the legacy marker if present — its sole purpose was to identify
        // the elf entity, which the new component now does for every NPC.
        entityHolder.tryRemoveComponent(ElfNpcComponent.getComponentType());
        LOGGER.info("NpcChunkLoadHandler: migrated legacy NPC entityUuid=" + entityUuid
                + " → npcId=" + npcId);
        return npcId;
    }

    private static Set<NpcRegistry.NpcRecord> collectChunkRecords(NpcRegistry registry,
                                                                  long chunkIndex,
                                                                  EntityChunk entityChunk) {
        Set<NpcRegistry.NpcRecord> candidates = new LinkedHashSet<>();
        for (NpcRegistry.NpcRecord record : registry.allRecords()) {
            if (isRecordTrackedInChunk(record, chunkIndex)) {
                candidates.add(record);
            }
        }
        if (entityChunk == null) return candidates;

        for (Holder entityHolder : entityChunk.getEntityHolders()) {
            NpcRegistry.NpcRecord matched = resolveChunkEntityRecord(entityHolder, registry);
            if (matched != null) candidates.add(matched);
        }
        return candidates;
    }

    private static boolean isRecordTrackedInChunk(NpcRegistry.NpcRecord record, long chunkIndex) {
        if (record == null) return false;
        if (record.hasPosition && record.chunkIndex == chunkIndex) return true;
        return record.hasBasePosition && record.baseChunkIndex == chunkIndex;
    }

    private static NpcRegistry.NpcRecord resolveChunkEntityRecord(Holder entityHolder,
                                                                  NpcRegistry registry) {
        if (entityHolder == null) return null;
        NPCEntity npc = (NPCEntity) entityHolder.getComponent(NPCEntity.getComponentType());
        if (npc == null) return null;

        String npcId = readNpcIdFromHolder(entityHolder);
        if (npcId != null) {
            NpcRegistry.NpcRecord byIdentity = registry.getRecordByNpcId(npcId);
            if (byIdentity != null) return byIdentity;
        }

        UUIDComponent uc = (UUIDComponent) entityHolder.getComponent(UUIDComponent.getComponentType());
        if (uc != null) {
            NpcRegistry.NpcRecord byUuid = registry.getRecord(uc.getUuid());
            if (byUuid != null) return byUuid;
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // Restore / spawn
    // -------------------------------------------------------------------------

    /**
     * Resolves the entity ref for a known NPC and runs {@link NpcRestorer#restore}.
     * Polls briefly because the entity is not yet "live" at the time of the
     * pre-load event — its ref becomes valid a tick or two later.
     */
    private static void scheduleRestore(World world, String npcId, UUID entityUuid,
                                        NpcRegistry.NpcRecord record) {
        int[] attempts = {0};
        ScheduledFuture<?>[] taskRef = {null};
        taskRef[0] = TickScheduler.getExecutor().scheduleAtFixedRate(() ->
            world.execute(() -> {
                NpcRegistry.NpcRecord liveRecord = NpcRegistry.get().getRecordByNpcId(npcId);
                if (liveRecord == null) {
                    if (taskRef[0] != null) taskRef[0].cancel(false);
                    return;
                }
                Store<EntityStore> store = world.getEntityStore().getStore();
                Ref<EntityStore> ref = world.getEntityRef(liveRecord.entityUuid);
                if (ref == null || !ref.isValid()) {
                    ref = NpcLiveEntityResolver.findLiveNpcByRecord(store, liveRecord);
                }
                if (ref != null && ref.isValid()) {
                    UUID liveUuid = NpcManager.extractUuid(store, ref);
                    if (liveUuid != null && !liveUuid.equals(liveRecord.entityUuid)) {
                        NpcRegistry.get().bindEntityUuid(npcId, liveUuid);
                        HearthboundDataStore.get().markDirty();
                    }
                    NpcRestorer.restore(ref, store, world, liveRecord);
                    if (taskRef[0] != null) taskRef[0].cancel(false);
                    return;
                }
                if (++attempts[0] >= 120) {
                    LOGGER.warning("NpcChunkLoadHandler: entity ref never became valid for npcId="
                            + npcId + " entityUuid=" + entityUuid);
                    if (taskRef[0] != null) taskRef[0].cancel(false);
                }
            }),
        100L, 250L, TimeUnit.MILLISECONDS);
    }

    /**
     * Spawns a fresh entity for a record whose chunk loaded but whose old entity
     * is gone (BSON removed it, world wipe, etc.). The new entity inherits the
     * stable npcId so duplicates that surface later are erased by
     * {@link dev.hearthbound.npc.DuplicateNpcPrevention}.
     */
    private static void scheduleFreshSpawn(World world, NpcRegistry.NpcRecord record, long triggerChunkIndex) {
        final String npcId = record.npcId;
        final String role = record.roleName;
        final Vector3d pos = new Vector3d(record.lastX, record.lastY, record.lastZ);
        final long resolveStartMs = System.currentTimeMillis();
        if (!freshSpawnsInFlight.add(npcId)) {
            LOGGER.info("[FRESHSPAWN-DEFER] npcId=" + npcId + " already has a retry task");
            return;
        }

        int[] resolveAttempts = {0};
        int[] spawnAttempts = {0};
        int[] edgeDefers = {0};
        ScheduledFuture<?>[] taskRef = {null};
        taskRef[0] = TickScheduler.getExecutor().scheduleAtFixedRate(() ->
            world.execute(() -> {
                NpcRegistry.NpcRecord live = NpcRegistry.get().getRecordByNpcId(npcId);
                if (live == null) {
                    freshSpawnsInFlight.remove(npcId);
                    if (taskRef[0] != null) taskRef[0].cancel(false);
                    return;
                }
                // Scan all loaded NPC entities for one that already carries our npcId.
                // We cannot rely on world.getEntityRef(live.entityUuid) here because the
                // engine may assign a new UUID to the entity when it deserialises from
                // BSON — the old UUID in the registry is then stale and the lookup returns
                // null even though the entity loaded correctly from disk. We also cannot
                // rely on StayedNpcIdentityComponent being visible in ChunkPreLoadProcessEvent
                // holders (the pre-load event fires before BSON components are materialised
                // into the live ECS store). Scanning the live store avoids both issues.
                Store<EntityStore> store = world.getEntityStore().getStore();
                Ref<EntityStore> alreadyLoaded = NpcLiveEntityResolver.findLiveNpcByRecord(store, live);
                if (alreadyLoaded != null) {
                    // Entity loaded from BSON — bind the (possibly new) UUID and restore.
                    UUID loadedUuid = NpcManager.extractUuid(store, alreadyLoaded);
                    if (loadedUuid != null && !loadedUuid.equals(live.entityUuid)) {
                        UUID oldUuid = live.entityUuid;
                        NpcRegistry.get().bindEntityUuid(npcId, loadedUuid);
                        LOGGER.info("[FRESHSPAWN-SKIP] npcId=" + npcId
                                + " found via scan, rebound uuid " + oldUuid + " → " + loadedUuid);
                        if (oldUuid != null) rewriteVillageReferences(world, store, oldUuid, loadedUuid);
                        HearthboundDataStore.get().markDirty();
                    }
                    NpcRestorer.restore(alreadyLoaded, store, world, live);
                    freshSpawnsInFlight.remove(npcId);
                    if (taskRef[0] != null) taskRef[0].cancel(false);
                    return;
                }

                long elapsedMs = System.currentTimeMillis() - resolveStartMs;
                if (elapsedMs < FRESH_SPAWN_RESOLVE_TIMEOUT_MS) {
                    int attempt = ++resolveAttempts[0];
                    if (attempt == 1 || attempt % 10 == 0) {
                        LOGGER.info("[FRESHSPAWN-RESOLVE] npcId=" + npcId
                                + " role=" + role
                                + " waitingForEntity elapsedMs=" + elapsedMs
                                + " timeoutMs=" + FRESH_SPAWN_RESOLVE_TIMEOUT_MS);
                    }
                    return;
                }

                if (shouldDeferFreshSpawnForEdgeCandidate(world, live)) {
                    LOGGER.info("[FRESHSPAWN-DEFER] npcId=" + npcId
                            + " role=" + role
                            + " triggerChunk=" + triggerChunkIndex
                            + " reason=EDGE_CANDIDATE_CHUNKS_NOT_READY");
                    if (++edgeDefers[0] >= 120) {
                        freshSpawnsInFlight.remove(npcId);
                        if (taskRef[0] != null) taskRef[0].cancel(false);
                    }
                    return;
                }

                Pair<Ref<EntityStore>, INonPlayerCharacter> spawn;
                try {
                    spawn = NpcManager.spawnNpc(store, pos, new Vector3f(0, 0, 0), role);
                } catch (Exception e) {
                    LOGGER.warning("NpcChunkLoadHandler: respawn threw for npcId=" + npcId + ": " + e.getMessage());
                    if (++spawnAttempts[0] >= MAX_FRESH_SPAWN_ATTEMPTS) {
                        freshSpawnsInFlight.remove(npcId);
                        if (taskRef[0] != null) taskRef[0].cancel(false);
                    }
                    return;
                }
                if (spawn == null || spawn.first() == null) {
                    int attempt = ++spawnAttempts[0];
                    if (attempt >= MAX_FRESH_SPAWN_ATTEMPTS) {
                        LOGGER.warning("NpcChunkLoadHandler: respawn returned null for npcId=" + npcId
                                + " role=" + role + " after " + attempt + " attempt(s)");
                        freshSpawnsInFlight.remove(npcId);
                        if (taskRef[0] != null) taskRef[0].cancel(false);
                    } else {
                        LOGGER.info("[FRESHSPAWN-RETRY] npcId=" + npcId
                                + " role=" + role + " attempt=" + attempt);
                    }
                    return;
                }

                Ref<EntityStore> newRef = spawn.first();
                store.putComponent(newRef, StayedNpcIdentityComponent.getComponentType(),
                        new StayedNpcIdentityComponent(npcId));

                UUID oldUuid = live.entityUuid;
                UUID newUuid = NpcManager.extractUuid(store, newRef);
                LOGGER.info("[FRESHSPAWN] npcId=" + npcId
                        + " oldEntityUuid=" + oldUuid
                        + " newEntityUuid=" + newUuid
                        + " role=" + role);
                if (newUuid != null) {
                    NpcRegistry.get().bindEntityUuid(npcId, newUuid);
                    if (oldUuid != null && !oldUuid.equals(newUuid)) {
                        rewriteVillageReferences(world, store, oldUuid, newUuid);
                    }
                }
                attachInteractionData(store, newRef, live);
                HearthboundDataStore.get().markDirty();
                LOGGER.info("NpcChunkLoadHandler: respawned " + role + " npcId=" + npcId
                        + " at " + (int) pos.x + "," + (int) pos.y + "," + (int) pos.z);

                NpcRestorer.restore(newRef, store, world, live);
                freshSpawnsInFlight.remove(npcId);
                if (taskRef[0] != null) taskRef[0].cancel(false);
            }),
        FRESH_SPAWN_RETRY_MS, FRESH_SPAWN_RETRY_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * After a fresh spawn replaces a lost villager, the engine assigns a new
     * UUID. Walk every player's village and rewrite any reference to the old
     * UUID — without this, the resident keeps their record but the building
     * assignment / house slot can no longer find them on the next tick.
     *
     * For elf records we update {@link dev.hearthbound.village.VillageData#getElfId()}
     * directly. For villagers we delegate to
     * {@link dev.hearthbound.village.VillageManager#replaceVillagerUuid}.
     */
    private static void rewriteVillageReferences(World world, Store<EntityStore> store,
                                                 UUID oldUuid, UUID newUuid) {
        try {
            var universe = com.hypixel.hytale.server.core.universe.Universe.get();
            for (var pr : universe.getPlayers()) {
                if (pr == null) continue;
                UUID playerUuid = pr.getUuid();
                if (playerUuid == null) continue;
                Ref<EntityStore> playerRef = world.getEntityRef(playerUuid);
                if (playerRef == null || !playerRef.isValid()) continue;
                var village = store.getComponent(playerRef,
                        dev.hearthbound.village.VillageData.getComponentType());
                if (village == null) continue;

                boolean changed = false;
                if (oldUuid.equals(village.getElfId())) {
                    village.setElfId(newUuid);
                    changed = true;
                }
                int rewritten = dev.hearthbound.village.VillageManager.get()
                        .replaceVillagerUuid(village, oldUuid, newUuid);
                if (rewritten > 0) changed = true;
                if (changed) {
                    dev.hearthbound.village.VillageManager.get().save(store, playerRef, village);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("rewriteVillageReferences failed: " + e.getMessage());
        }
    }

    /**
     * Re-attach the per-interaction data component a respawned NPC needs so
     * downstream systems (scheduler, dialogs) can see it again.
     */
    private static void attachInteractionData(Store<EntityStore> store, Ref<EntityStore> ref,
                                              NpcRegistry.NpcRecord record) {
        if (record.interaction == NpcRegistry.InteractionType.VILLAGER) {
            dev.hearthbound.village.VillagerData data = store.getComponent(ref,
                    dev.hearthbound.village.VillagerData.getComponentType());
            if (data == null) {
                data = new dev.hearthbound.village.VillagerData();
                data.setSkinSeed(record.skinSeed);
                store.putComponent(ref, dev.hearthbound.village.VillagerData.getComponentType(), data);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Misc
    // -------------------------------------------------------------------------

    /**
     * Scans entities in the loading chunk and repairs invalid PersistentModel
     * scales. Defends against the engine writing scale=0 to BSON, which would
     * crash the world on next chunk load with "Scale must be > 0".
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void sanitizeChunkPersistentModels(ChunkPreLoadProcessEvent event) {
        Holder chunkHolder = event.getHolder();
        if (chunkHolder == null) return;
        EntityChunk entityChunk = (EntityChunk) chunkHolder.getComponent(EntityChunk.getComponentType());
        if (entityChunk == null) return;

        int repaired = 0;
        for (Holder entityHolder : entityChunk.getEntityHolders()) {
            if (entityHolder == null) continue;
            PersistentModel pm = (PersistentModel) entityHolder.getComponent(PersistentModel.getComponentType());
            if (pm == null) continue;
            Model.ModelReference ref = pm.getModelReference();
            if (ref == null) {
                entityHolder.tryRemoveComponent(PersistentModel.getComponentType());
                repaired++;
                continue;
            }
            float scale = ref.getScale();
            if (Float.isFinite(scale) && scale > 0.0f && scale <= 100.0f) continue;
            String assetId = ref.getModelAssetId();
            if (assetId == null || assetId.isEmpty()) {
                entityHolder.tryRemoveComponent(PersistentModel.getComponentType());
            } else {
                float fixedScale = Float.isFinite(scale) && scale > 0.0f ? 100.0f : 0.01f;
                entityHolder.putComponent(PersistentModel.getComponentType(),
                        new PersistentModel(new Model.ModelReference(
                                assetId, fixedScale, ref.getRandomAttachmentIds(), ref.isStaticModel())));
            }
            repaired++;
        }
        if (repaired > 0) {
            entityChunk.markNeedsSaving();
            LOGGER.warning("Repaired " + repaired + " invalid PersistentModel scale(s) in chunk "
                    + event.getChunk().getX() + "," + event.getChunk().getZ());
        }
    }

    /**
     * Polls for the entity to materialise, then removes it. Needed because
     * ChunkPreLoadProcessEvent fires before the engine deserialises BSON
     * entities into refs.
     */
    private static void schedulePendingRemoval(UUID entityUuid, World world) {
        if (!pendingRemovalTasks.add(entityUuid)) return;
        int[] attempts = {0};
        ScheduledFuture<?>[] taskRef = {null};

        taskRef[0] = TickScheduler.getExecutor().scheduleAtFixedRate(() ->
            world.execute(() -> {
                Ref<EntityStore> ref = world.getEntityRef(entityUuid);
                if (ref != null && ref.isValid()) {
                    world.getEntityStore().getStore().removeEntity(ref, RemoveReason.REMOVE);
                    NpcRegistry.get().clearPendingRemoval(entityUuid);
                    HearthboundDataStore.get().markDirty();
                    pendingRemovalTasks.remove(entityUuid);
                    LOGGER.info("NpcChunkLoadHandler: removed deferred NPC entityUuid=" + entityUuid
                            + " after " + attempts[0] + " attempt(s)");
                    if (taskRef[0] != null) taskRef[0].cancel(false);
                    return;
                }
                if (++attempts[0] >= MAX_PENDING_REMOVAL_ATTEMPTS) {
                    pendingRemovalTasks.remove(entityUuid);
                    LOGGER.warning("NpcChunkLoadHandler: gave up removing entityUuid=" + entityUuid
                            + " but left pending-removal tombstone intact");
                    if (taskRef[0] != null) taskRef[0].cancel(false);
                }
            }),
        100L, 250L, TimeUnit.MILLISECONDS);
    }

    /**
     * Extra safety beyond HyCitizens: if the saved position is close enough to
     * a chunk border that a crash could have left data.json one chunk behind,
     * do not respawn until the adjacent candidate chunk is loaded or at least
     * loadable from memory. This avoids creating a replacement in the stale
     * chunk while the real entity is asleep just across the border.
     */
    private static boolean shouldDeferFreshSpawnForEdgeCandidate(World world,
                                                                 NpcRegistry.NpcRecord record) {
        if (world == null || record == null || !record.hasPosition) return false;
        return shouldDeferEdgePosition(world, record.lastX, record.lastZ)
                || (record.hasBasePosition && shouldDeferEdgePosition(world, record.baseX, record.baseZ));
    }

    private static boolean shouldDeferEdgePosition(World world, double x, double z) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int localX = Math.floorMod(blockX, com.hypixel.hytale.math.util.ChunkUtil.SIZE);
        int localZ = Math.floorMod(blockZ, com.hypixel.hytale.math.util.ChunkUtil.SIZE);
        if (localX > 1
                && localX < com.hypixel.hytale.math.util.ChunkUtil.SIZE - 2
                && localZ > 1
                && localZ < com.hypixel.hytale.math.util.ChunkUtil.SIZE - 2) {
            return false;
        }

        long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(x, z);
        int chunkX = com.hypixel.hytale.math.util.ChunkUtil.xOfChunkIndex(chunkIndex);
        int chunkZ = com.hypixel.hytale.math.util.ChunkUtil.zOfChunkIndex(chunkIndex);

        boolean defer = false;
        if (localX <= 1) defer |= ensureCandidateChunkLoaded(world, chunkX - 1, chunkZ);
        if (localX >= com.hypixel.hytale.math.util.ChunkUtil.SIZE - 2) {
            defer |= ensureCandidateChunkLoaded(world, chunkX + 1, chunkZ);
        }
        if (localZ <= 1) defer |= ensureCandidateChunkLoaded(world, chunkX, chunkZ - 1);
        if (localZ >= com.hypixel.hytale.math.util.ChunkUtil.SIZE - 2) {
            defer |= ensureCandidateChunkLoaded(world, chunkX, chunkZ + 1);
        }
        return defer;
    }

    private static boolean ensureCandidateChunkLoaded(World world, int chunkX, int chunkZ) {
        long candidate = com.hypixel.hytale.math.util.ChunkUtil.indexChunk(chunkX, chunkZ);
        if (world.getChunkIfLoaded(candidate) != null) return false;
        if (world.getChunkIfInMemory(candidate) != null) {
            world.loadChunkIfInMemory(candidate);
        }
        return true;
    }
}
