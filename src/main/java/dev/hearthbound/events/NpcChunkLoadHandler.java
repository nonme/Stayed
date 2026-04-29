package dev.hearthbound.events;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.EntityChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hearthbound.npc.HearthboundDataStore;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.util.TickScheduler;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


/**
 * On every chunk load, collects the NPC records that belong to this chunk and
 * schedules restoration via NpcRegistry.
 *
 * Two-pass strategy:
 *
 * Pass 1 — registry chunk index: records whose stored chunkIndex matches the
 *           loading chunk.
 *
 * Pass 2 — physical entity scan: reads EntityChunk from event.getHolder() and
 *           matches entity UUIDs against the registry. Catches NPCs that have
 *           walked into a different chunk since chunkIndex was last recorded,
 *           so a stale chunkIndex never causes a missed restoration.
 */
@SuppressWarnings("rawtypes")
public class NpcChunkLoadHandler {

    private static final Logger LOGGER = Logger.getLogger(NpcChunkLoadHandler.class.getName());

    public static void onChunkLoad(ChunkPreLoadProcessEvent event) {
        WorldChunk chunk = event.getChunk();
        if (chunk == null) return;
        World world = chunk.getWorld();
        if (world == null) return;

        // Sanitize before any NPC processing — prevents "Scale must be > 0" world crash
        // when the engine wrote a bad PersistentModel to BSON.
        sanitizeChunkPersistentModels(event);

        long chunkIndex = chunk.getIndex();
        NpcRegistry registry = NpcRegistry.get();

        // Pass 1: records whose stored chunkIndex matches this chunk.
        Set<UUID> toRestore = new LinkedHashSet<>();
        for (NpcRegistry.NpcRecord r : registry.getForChunk(chunkIndex)) {
            toRestore.add(r.entityUuid);
        }

        // Pass 2: physical entity scan — scans every NPC entity in this chunk's EntityChunk:
        // - if UUID is in pendingRemovals: delete it (deferred removal from when chunk was unloaded)
        // - if UUID is in registry but NOT in toRestore yet: add to restore set
        // NOTE: do NOT update record.chunkIndex here. Physical EntityChunk scan reflects
        // which chunk holds the entity in BSON, which can differ from where getEntityRef()
        // will find it (entity may straddle chunk boundaries). chunkIndex is updated only
        // when an entity is explicitly spawned or teleported to a known position.
        boolean removedAny = false;
        Holder chunkHolder = event.getHolder();
        if (chunkHolder != null) {
            EntityChunk entityChunk = (EntityChunk) chunkHolder.getComponent(EntityChunk.getComponentType());
            if (entityChunk != null) {
                for (Holder entityHolder : entityChunk.getEntityHolders()) {
                    if (entityHolder == null) continue;
                    NPCEntity npc = (NPCEntity) entityHolder.getComponent(NPCEntity.getComponentType());
                    if (npc == null) continue;
                    UUIDComponent uc = (UUIDComponent) entityHolder.getComponent(UUIDComponent.getComponentType());
                    if (uc == null) continue;
                    UUID uuid = uc.getUuid();

                    if (registry.isPendingRemoval(uuid)) {
                        registry.clearPendingRemoval(uuid);
                        removedAny = true;
                        schedulePendingRemoval(uuid, world);
                        continue;
                    }

                    NpcRegistry.NpcRecord record = registry.getRecord(uuid);
                    if (record != null) {
                        toRestore.add(uuid);
                    }
                }
            }
        }
        if (removedAny) {
            HearthboundDataStore.get().save();
        }

        if (toRestore.isEmpty()) return;

        LOGGER.info("NpcChunkLoadHandler: chunk " + chunkIndex
                + " loaded, scheduling restore for " + toRestore.size() + " NPC(s)");

        for (UUID uuid : toRestore) {
            NpcRegistry.NpcRecord record = registry.getRecord(uuid);
            if (record == null) continue;
            registry.scheduleRestoreOne(record, world);
        }
    }

    /**
     * Scans all entities in the loading chunk and repairs invalid PersistentModel scale values.
     * Prevents the "Scale must be > 0" world crash caused by the engine writing scale=0 to BSON after spawn.
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
     * Polls world.getEntityRef(uuid) every 100ms until found, then removes via
     * store.removeEntity(ref, RemoveReason.REMOVE). Polling is needed because
     * ChunkPreLoadProcessEvent fires before BSON entities are deserialized,
     * so the entity ref isn't valid yet at the time of the event.
     */
    private static void schedulePendingRemoval(UUID uuid, World world) {
        int[] attempts = {0};
        ScheduledFuture<?>[] taskRef = {null};

        taskRef[0] = TickScheduler.getExecutor().scheduleAtFixedRate(() ->
            world.execute(() -> {
                Ref<EntityStore> ref = world.getEntityRef(uuid);
                if (ref != null && ref.isValid()) {
                    world.getEntityStore().getStore().removeEntity(ref, RemoveReason.REMOVE);
                    LOGGER.info("NpcChunkLoadHandler: removed deferred NPC uuid=" + uuid
                            + " after " + attempts[0] + " attempt(s)");
                    if (taskRef[0] != null) taskRef[0].cancel(false);
                    return;
                }
                if (++attempts[0] >= 24) {
                    LOGGER.warning("NpcChunkLoadHandler: gave up removing uuid=" + uuid);
                    if (taskRef[0] != null) taskRef[0].cancel(false);
                }
            }),
        100L, 250L, TimeUnit.MILLISECONDS);
    }
}
