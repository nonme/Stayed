package dev.hearthbound.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.events.NpcChunkLoadHandler;
import dev.hearthbound.util.TickScheduler;
import it.unimi.dsi.fastutil.Pair;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Last-resort recovery for registry records whose managed entity is missing
 * while its chunk is loaded. This is deliberately stricter than ordinary
 * chunk-load restoration: it has one in-flight task per npcId, always tries to
 * resolve a live entity before spawning, and rate-limits spawn attempts.
 */
public final class NpcMissingEntityRecovery {

    private static final Logger LOGGER = Logger.getLogger(NpcMissingEntityRecovery.class.getName());
    private static final long RESOLVE_DELAY_MS = 1_500L;
    private static final long SPAWN_COOLDOWN_MS = 30_000L;

    private static final java.util.Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private static final Map<String, Long> lastSpawnAttemptMs = new ConcurrentHashMap<>();

    private NpcMissingEntityRecovery() {}

    public record Target(double x, double y, double z) {}

    public static boolean request(World world, NpcRegistry.NpcRecord record,
                                  Target target, String reason) {
        if (world == null || record == null || record.npcId == null || record.npcId.isBlank()) {
            return false;
        }
        if (record.broken || record.interaction == NpcRegistry.InteractionType.NONE) {
            return false;
        }
        if (!inFlight.add(record.npcId)) {
            LOGGER.info("[NPC-RECOVERY] skip npcId=" + record.npcId
                    + " reason=" + reason + " alreadyInFlight=true");
            return false;
        }

        world.execute(() -> resolveBeforeLoading(world, record.npcId, target, reason));
        return true;
    }

    private static void resolveBeforeLoading(World world, String npcId,
                                             Target target, String reason) {
        NpcRegistry.NpcRecord record = NpcRegistry.get().getRecordByNpcId(npcId);
        if (record == null) {
            finish(npcId);
            return;
        }

        Store<EntityStore> store = world.getEntityStore().getStore();
        Ref<EntityStore> live = NpcLiveEntityResolver.findLiveNpcByRecord(store, record);
        if (live != null && live.isValid()) {
            restoreOrMove(world, store, live, record, target, reason, "preload-scan");
            finish(npcId);
            return;
        }

        world.getChunkAsync(record.chunkIndex);
        if (record.hasBasePosition && record.baseChunkIndex != record.chunkIndex) {
            world.getChunkAsync(record.baseChunkIndex);
        }
        scheduleFinalResolve(world, npcId, target, reason);
    }

    private static void scheduleFinalResolve(World world, String npcId,
                                             Target target, String reason) {
        world.execute(() -> TickScheduler.runLater(world, RESOLVE_DELAY_MS, () ->
                resolveOrSpawn(world, npcId, target, reason)));
    }

    private static void resolveOrSpawn(World world, String npcId,
                                       Target target, String reason) {
        try {
            NpcRegistry.NpcRecord record = NpcRegistry.get().getRecordByNpcId(npcId);
            if (record == null) return;

            Store<EntityStore> store = world.getEntityStore().getStore();
            Ref<EntityStore> live = NpcLiveEntityResolver.findLiveNpcByRecord(store, record);
            if (live != null && live.isValid()) {
                restoreOrMove(world, store, live, record, target, reason, "post-load-scan");
                return;
            }

            long now = System.currentTimeMillis();
            long last = lastSpawnAttemptMs.getOrDefault(npcId, 0L);
            if (now - last < SPAWN_COOLDOWN_MS) {
                LOGGER.warning("[NPC-RECOVERY] cooldown npcId=" + npcId
                        + " reason=" + reason
                        + " remainingMs=" + (SPAWN_COOLDOWN_MS - (now - last)));
                return;
            }
            lastSpawnAttemptMs.put(npcId, now);

            Target spawnTarget = target != null ? target : fallbackTarget(record);
            if (spawnTarget == null) {
                LOGGER.warning("[NPC-RECOVERY] no spawn target npcId=" + npcId
                        + " reason=" + reason);
                return;
            }

            spawnReplacement(world, store, record, spawnTarget, reason);
        } finally {
            finish(npcId);
        }
    }

    private static Target fallbackTarget(NpcRegistry.NpcRecord record) {
        if (record.hasPosition) return new Target(record.lastX, record.lastY, record.lastZ);
        if (record.hasBasePosition) return new Target(record.baseX, record.baseY, record.baseZ);
        return null;
    }

    private static void restoreOrMove(World world, Store<EntityStore> store,
                                      Ref<EntityStore> ref, NpcRegistry.NpcRecord record,
                                      Target target, String reason, String phase) {
        UUID liveUuid = NpcManager.extractUuid(store, ref);
        if (liveUuid != null && !liveUuid.equals(record.entityUuid)) {
            UUID oldUuid = record.entityUuid;
            NpcRegistry.get().bindEntityUuid(record.npcId, liveUuid);
            if (oldUuid != null) {
                NpcChunkLoadHandler.rewriteVillageReferences(world, store, oldUuid, liveUuid);
            }
            HearthboundDataStore.get().save();
            record = NpcRegistry.get().getRecordByNpcId(record.npcId);
            if (record == null) return;
        }

        NpcRestorer.restore(ref, store, world, record);
        if (target != null) {
            NpcTeleporter.doMove(world, store, ref, record, target.x(), target.y(), target.z());
        }
        LOGGER.info("[NPC-RECOVERY] resolved npcId=" + record.npcId
                + " liveUuid=" + liveUuid
                + " reason=" + reason
                + " phase=" + phase
                + " spawned=false");
    }

    private static void spawnReplacement(World world, Store<EntityStore> store,
                                         NpcRegistry.NpcRecord record,
                                         Target target, String reason) {
        UUID oldUuid = record.entityUuid;
        Pair<Ref<EntityStore>, INonPlayerCharacter> spawn = StayedNpcSpawner.spawnPersistent(
                store,
                new Vector3d(target.x(), target.y(), target.z()),
                new Vector3f(0, 0, 0),
                record);
        if (spawn == null || spawn.first() == null) {
            LOGGER.warning("[NPC-RECOVERY] spawn returned null npcId=" + record.npcId
                    + " reason=" + reason);
            return;
        }

        Ref<EntityStore> ref = spawn.first();
        attachInteractionData(store, ref, record);

        UUID newUuid = NpcManager.extractUuid(store, ref);
        if (newUuid == null) {
            LOGGER.warning("[NPC-RECOVERY] spawned entity has no UUID npcId=" + record.npcId
                    + " reason=" + reason);
            return;
        }

        NpcRegistry.get().updatePosition(newUuid, target.x(), target.y(), target.z());
        if (oldUuid != null && !oldUuid.equals(newUuid)) {
            NpcChunkLoadHandler.rewriteVillageReferences(world, store, oldUuid, newUuid);
        }

        HearthboundDataStore.get().save();
        NpcRestorer.restore(ref, store, world, NpcRegistry.get().getRecordByNpcId(record.npcId));
        LOGGER.warning("[NPC-RECOVERY] spawned replacement npcId=" + record.npcId
                + " oldUuid=" + oldUuid
                + " newUuid=" + newUuid
                + " reason=" + reason
                + " at=(" + target.x() + "," + target.y() + "," + target.z() + ")");
    }

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

    private static void finish(String npcId) {
        if (npcId != null) inFlight.remove(npcId);
    }
}
