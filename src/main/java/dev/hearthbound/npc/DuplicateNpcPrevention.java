package dev.hearthbound.npc;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hearthbound.util.TickScheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

/**
 * Engine-level guard that ensures at most one entity is alive per Stayed npcId.
 *
 * Whenever the engine adds an NPC entity to a chunk (spawn or chunk-load), this
 * system reads its {@link StayedNpcIdentityComponent}. If a different valid entity
 * is already known under that npcId, the new entity is removed via the command
 * buffer. This is the safety net that catches every duplication path — buggy
 * lifecycle code, double chunk-load on the same NPC, etc. — and erases the
 * duplicate the moment it appears.
 */
public final class DuplicateNpcPrevention extends RefSystem<EntityStore> {

    private static final Logger LOGGER = Logger.getLogger(DuplicateNpcPrevention.class.getName());

    private final Query<EntityStore> query = NPCEntity.getComponentType();
    private final Map<String, Ref<EntityStore>> activeNpcs = new ConcurrentHashMap<>();

    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        String npcId = readNpcId(ref, store);
        java.util.UUID entityUuid = readEntityUuid(ref, store);
        NpcRegistry registry = NpcRegistry.get();
        LOGGER.info("[DUPGUARD] onEntityAdded reason=" + reason
                + " ref=" + ref + " entityUuid=" + entityUuid
                + " npcId=" + npcId);
        if (registry.isPendingRemoval(entityUuid)) {
            LOGGER.info("[DUPGUARD] removing pending-removal NPC entityUuid=" + entityUuid
                    + " npcId=" + npcId + " reason=" + reason);
            commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
            registry.clearPendingRemoval(entityUuid);
            HearthboundDataStore.get().markDirty();
            return;
        }
        if (npcId == null) return;

        Ref<EntityStore> existing = activeNpcs.get(npcId);
        if (existing != null && existing.isValid() && !existing.equals(ref)) {
            LOGGER.warning("[DUPGUARD] removing duplicate entity for npcId="
                    + npcId + " (existing ref still valid: " + existing + ", new=" + ref + ")");
            commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
            return;
        }
        activeNpcs.put(npcId, ref);

        NpcRegistry.NpcRecord record = registry.getRecordByNpcId(npcId);
        if (record == null) return;
        if (entityUuid != null && !entityUuid.equals(record.entityUuid)) {
            registry.bindEntityUuid(npcId, entityUuid);
            HearthboundDataStore.get().markDirty();
        }

        World world = resolveWorld(store);
        if (world == null) {
            LOGGER.warning("[DUPGUARD-RESTORE] no world for npcId=" + npcId
                    + " entityUuid=" + entityUuid + " reason=" + reason);
            return;
        }

        schedulePostAddRestore(world, ref, npcId, entityUuid, reason);
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason,
                               @Nonnull Store<EntityStore> store,
                               @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        String npcId = readNpcId(ref, store);
        if (npcId == null) return;

        Ref<EntityStore> existing = activeNpcs.get(npcId);
        if (existing != null && existing.equals(ref)) {
            activeNpcs.remove(npcId);
        }
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return query;
    }

    private static String readNpcId(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || store == null) return null;
        StayedNpcIdentityComponent identity = store.getComponent(ref,
                StayedNpcIdentityComponent.getComponentType());
        if (identity == null) return null;
        String npcId = identity.getNpcId();
        return (npcId == null || npcId.isBlank()) ? null : npcId;
    }

    private static java.util.UUID readEntityUuid(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || store == null) return null;
        com.hypixel.hytale.server.core.entity.UUIDComponent uc = store.getComponent(ref,
                com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType());
        return uc != null ? uc.getUuid() : null;
    }

    private static World resolveWorld(Store<EntityStore> store) {
        if (store == null || store.getExternalData() == null) return null;
        return store.getExternalData().getWorld();
    }

    private static void schedulePostAddRestore(World world, Ref<EntityStore> initialRef,
                                               String npcId, java.util.UUID entityUuid,
                                               AddReason reason) {
        TickScheduler.getExecutor().schedule(() -> world.execute(() -> {
            NpcRegistry registry = NpcRegistry.get();
            NpcRegistry.NpcRecord record = registry.getRecordByNpcId(npcId);
            if (record == null) return;

            Store<EntityStore> liveStore = world.getEntityStore().getStore();
            Ref<EntityStore> liveRef = initialRef != null && initialRef.isValid()
                    ? initialRef : NpcLiveEntityResolver.findLiveNpcByRecord(liveStore, record);
            if (liveRef == null || !liveRef.isValid()) {
                LOGGER.info("[DUPGUARD-RESTORE] npcId=" + npcId
                        + " entityUuid=" + entityUuid
                        + " reason=" + reason
                        + " skipped=no live ref");
                return;
            }

            java.util.UUID liveUuid = NpcManager.extractUuid(liveStore, liveRef);
            if (liveUuid != null && !liveUuid.equals(record.entityUuid)) {
                registry.bindEntityUuid(npcId, liveUuid);
                HearthboundDataStore.get().markDirty();
                record = registry.getRecordByNpcId(npcId);
            }

            NpcManager.fixPersistentModelScale(liveStore, liveRef);
            NpcRestorer.restore(liveRef, liveStore, world, record);
            LOGGER.info("[DUPGUARD-RESTORE] npcId=" + npcId
                    + " entityUuid=" + liveUuid
                    + " reason=" + reason);
        }), 50L, TimeUnit.MILLISECONDS);
    }
}
