package dev.hearthbound.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;
import dev.hearthbound.village.VillagerData;
import dev.hearthbound.village.VillagerSummary;
import it.unimi.dsi.fastutil.Pair;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Re-creates a lost NPC entity from its persisted registry record.
 *
 * Used as the last-resort fallback when the original entity can't be found
 * in any chunk (entity removed, BSON corruption, world wipe). The new entity
 * gets a new UUID — VillageManager.replaceVillagerUuid rewrites every saved
 * reference, so the villager keeps their house, job, name, profession and
 * skin (regenerated deterministically from the seed).
 *
 * Caller owns picking the spawn position. Heuristics live in
 * {@link #pickRespawnPosition(Ref, Store, NpcRegistry.NpcRecord)}: prefer
 * lastKnownPosition, fall back to the assigned house brazier, then to the
 * founding stone.
 */
public final class NpcRespawner {

    private static final Logger LOGGER = Logger.getLogger(NpcRespawner.class.getName());

    /** Base role used when the recorded role can't be loaded — VillagerScheduler will re-promote later. */
    private static final String FALLBACK_ROLE = "Villager_Human";

    private NpcRespawner() {}

    /**
     * Result of a respawn attempt. ok=true means the new entity is alive in the
     * world and the village data has been rewritten to reference it.
     */
    public static final class Result {
        public final boolean ok;
        public final UUID newUuid;
        public final String reason;

        private Result(boolean ok, UUID newUuid, String reason) {
            this.ok = ok;
            this.newUuid = newUuid;
            this.reason = reason;
        }

        static Result success(UUID newUuid) { return new Result(true, newUuid, null); }
        static Result failure(String reason) { return new Result(false, null, reason); }
    }

    /**
     * Async respawn entry point. Loads the spawn chunk off the world thread,
     * then runs the actual spawn back on the world thread. Caller gets a
     * CompletableFuture<Result> instead of a sync return — blocking on the
     * chunk future from inside world.execute() deadlocks (chunk completion
     * is itself driven by the world thread).
     *
     * Safe to call from anywhere; everything that touches the entity store
     * is dispatched onto the world thread internally.
     */
    public static java.util.concurrent.CompletableFuture<Result> respawn(
            World world, Store<EntityStore> storeAtCallTime,
            Ref<EntityStore> playerRefAtCallTime,
            NpcRegistry.NpcRecord oldRecord,
            Vector3d spawnPos) {
        var future = new java.util.concurrent.CompletableFuture<Result>();
        if (oldRecord == null) { future.complete(Result.failure("null record")); return future; }
        if (spawnPos == null) { future.complete(Result.failure("null position")); return future; }
        if (oldRecord.broken) { future.complete(Result.failure("circuit breaker tripped")); return future; }

        long chunkIndex = NpcManager.chunkIndexFor(spawnPos);
        world.getChunkAsync(chunkIndex).whenComplete((chunk, error) -> {
            if (error != null) {
                future.complete(Result.failure("chunk load failed: " + error.getMessage()));
                return;
            }
            // Re-enter the world thread for the actual spawn — chunk async callback
            // can fire on the scheduler pool, and entity-store mutations must happen
            // on the world thread.
            world.execute(() -> {
                try {
                    Result r = doSpawn(world, oldRecord, spawnPos);
                    future.complete(r);
                } catch (Exception e) {
                    future.complete(Result.failure("spawn threw: " + e.getMessage()));
                }
            });
        });
        return future;
    }

    /** World-thread half of the respawn. Caller holds the latest store/playerRef. */
    private static Result doSpawn(World world, NpcRegistry.NpcRecord oldRecord, Vector3d spawnPos) {
        Store<EntityStore> store = world.getEntityStore().getStore();

        String roleToUse = resolveRole(oldRecord.roleName);
        if (roleToUse == null) return Result.failure("role not registered: " + oldRecord.roleName);

        Vector3f rotation = new Vector3f(0f, 0f, 0f);
        Pair<Ref<EntityStore>, INonPlayerCharacter> spawn;
        try {
            spawn = NpcManager.spawnNpc(store, spawnPos, rotation, roleToUse);
        } catch (Exception e) {
            return Result.failure("spawnNPC threw: " + e.getMessage());
        }
        if (spawn == null || spawn.first() == null) return Result.failure("spawnNPC returned null");

        Ref<EntityStore> newRef = spawn.first();
        UUID newUuid = NpcManager.extractUuid(store, newRef);
        if (newUuid == null) return Result.failure("new entity has no UUID");

        long newChunkIndex = NpcManager.chunkIndexFor(spawnPos);
        NpcRegistry.NpcRecord newRecord = new NpcRegistry.NpcRecord(
                newUuid, oldRecord.roleName, oldRecord.interaction,
                oldRecord.skinSeed, newChunkIndex);
        newRecord.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
        NpcRegistry.get().unregister(oldRecord.entityUuid);
        NpcRegistry.get().register(newRecord);

        // Re-fetch the player ref each call: between scheduling the respawn and
        // actually running it the player may have logged in (or out) and the
        // ref captured at call time may now be stale.
        Ref<EntityStore> playerRef = findPlayerOwningOldUuid(world, store, oldRecord.entityUuid);
        VillageData village = playerRef != null
                ? VillageManager.get().getVillageData(store, playerRef)
                : null;

        // Restore the entity-side data the rest of the mod expects to find on
        // a villager. Without VillagerData the scheduler skips this entity, so
        // it never gets routed to its sawmill / farm / mine and never has a
        // role-change applied — it just stands as a plain Villager_Human.
        // For elves, write ElfNpcComponent and update VillageData.elfId so the
        // dialog and quest systems see the new entity as the player's elf.
        if (oldRecord.interaction == NpcRegistry.InteractionType.VILLAGER) {
            attachVillagerData(store, newRef, oldRecord, village);
        } else if (oldRecord.interaction == NpcRegistry.InteractionType.ELF) {
            attachElfData(store, newRef, oldRecord, playerRef, village);
        }

        // Rewrite UUID references so the resident keeps their house and job.
        if (village != null) {
            VillageManager.get().replaceVillagerUuid(village, oldRecord.entityUuid, newUuid);
            // Elf is referenced via VillageData.elfId, not the villager-summary
            // path, so handle it explicitly here.
            if (oldRecord.entityUuid.equals(village.getElfId())) {
                village.setElfId(newUuid);
            }
            VillageManager.get().save(store, playerRef, village);
        }

        // Apply skin + interaction. NpcRestorer schedules these with a delay so
        // the client receives the spawn packet first.
        NpcRestorer.restore(newRef, store, world, newRecord);

        HearthboundDataStore.get().markDirty();
        LOGGER.info("NpcRespawner: respawned " + oldRecord.roleName + " "
                + oldRecord.entityUuid + " → " + newUuid + " at "
                + (int) spawnPos.x + "," + (int) spawnPos.y + "," + (int) spawnPos.z);
        return Result.success(newUuid);
    }

    /**
     * Mirror of NpcRegistry.findPlayerOwningNpc but kept here so the respawner
     * can re-resolve ownership on the world thread without poking back into
     * the registry. Looks up the player whose VillageData still references
     * the old UUID — that's the village we need to rewrite.
     */
    private static Ref<EntityStore> findPlayerOwningOldUuid(World world, Store<EntityStore> store, UUID oldUuid) {
        try {
            var universe = com.hypixel.hytale.server.core.universe.Universe.get();
            for (var pr : universe.getPlayers()) {
                if (pr == null) continue;
                UUID playerUuid = pr.getUuid();
                if (playerUuid == null) continue;
                Ref<EntityStore> entityRef = world.getEntityRef(playerUuid);
                if (entityRef == null || !entityRef.isValid()) continue;
                VillageData v = store.getComponent(entityRef, VillageData.getComponentType());
                if (v == null) continue;
                if (oldUuid.equals(v.getElfId())) return entityRef;
                for (var s : v.getVillagers()) {
                    if (oldUuid.equals(s.getVillagerUuid())) return entityRef;
                }
                for (var b : v.getBuildings()) {
                    if (oldUuid.equals(b.getAssignedVillagerId())) return entityRef;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Attach a fresh VillagerData component to the respawned villager, mirroring
     * the fields kept in VillagerSummary. The scheduler queries this component
     * to pick targets and apply role changes — without it the villager just
     * stands around in the base Villager_Human role and never goes to work.
     */
    private static void attachVillagerData(Store<EntityStore> store, Ref<EntityStore> newRef,
                                           NpcRegistry.NpcRecord oldRecord, VillageData village) {
        VillagerSummary summary = village != null
                ? VillageManager.get().findVillagerSummary(village, oldRecord.entityUuid)
                : null;

        VillagerData data = new VillagerData();
        data.setSkinSeed(oldRecord.skinSeed);
        if (summary != null) {
            data.setRace(summary.getRace());
            data.setProfession(summary.getProfession());
            data.setFirstName(summary.getFirstName());
            data.setLastName(summary.getLastName());
            // The scheduler reconciles hasHouse on its own next tick by walking
            // buildings, so we leave it false here and let it self-correct.
        }
        store.putComponent(newRef, VillagerData.getComponentType(), data);
    }

    /**
     * Attach the ElfNpcComponent so handlers that resolve the elf via
     * entity scan (rather than VillageData.elfId) still see this entity as
     * an elf. Owner UUID is taken from the player who owns the village.
     */
    private static void attachElfData(Store<EntityStore> store, Ref<EntityStore> newRef,
                                      NpcRegistry.NpcRecord oldRecord,
                                      Ref<EntityStore> playerRef, VillageData village) {
        String ownerUuid = "";
        if (playerRef != null) {
            try {
                var uc = store.getComponent(playerRef,
                        com.hypixel.hytale.server.core.entity.UUIDComponent.getComponentType());
                if (uc != null && uc.getUuid() != null) ownerUuid = uc.getUuid().toString();
            } catch (Exception ignored) {}
        }
        store.putComponent(newRef, ElfNpcComponent.getComponentType(), new ElfNpcComponent(ownerUuid));
    }

    /**
     * Picks a sensible spawn point for a lost NPC.
     *
     * Order:
     *   1. Last-known position (set by NpcPositionTracker while NPC was alive).
     *   2. Interior stand point of the house this villager is assigned to.
     *   3. Founding stone of the village.
     *   4. null if none of the above are available — caller must abort.
     */
    public static Vector3d pickRespawnPosition(Ref<EntityStore> playerRef, Store<EntityStore> store,
                                                NpcRegistry.NpcRecord record) {
        if (record.hasPosition) {
            return new Vector3d(record.lastX, record.lastY, record.lastZ);
        }
        VillageData village = playerRef != null
                ? VillageManager.get().getVillageData(store, playerRef)
                : null;
        if (village == null) return null;

        // Villager fallback: assigned house → founding stone.
        for (BuildingRecord b : village.getBuildings()) {
            if (!record.entityUuid.equals(b.getAssignedVillagerId())) continue;
            if (!BuildingType.isResidential(b.getType())) continue;
            double[] stand = BuildingType.getInteriorStandPoint(
                    b.getPosX(), b.getPosY(), b.getPosZ(), b.getRotation());
            return new Vector3d(stand[0], stand[1], stand[2]);
        }
        // Elf fallback: founding stone (Aelin lives "outdoors" near it).
        if (village.getFoundingStoneX() != 0 || village.getFoundingStoneY() != 0
                || village.getFoundingStoneZ() != 0) {
            return new Vector3d(
                    village.getFoundingStoneX() + 0.5,
                    village.getFoundingStoneY() + 1.0,
                    village.getFoundingStoneZ() + 0.5);
        }
        return null;
    }

    /**
     * Picks a role name that NPCPlugin can actually instantiate. If the recorded
     * role is missing (renamed JSON, asset reload, etc.), we fall back to the
     * base Villager_Human role so the NPC still appears — VillagerScheduler will
     * pick a profession again next tick if the village has work for them.
     */
    private static String resolveRole(String recordedRole) {
        if (recordedRole != null) {
            int idx = NPCPlugin.get().getIndex(recordedRole);
            if (idx != Integer.MIN_VALUE) return recordedRole;
            LOGGER.warning("NpcRespawner: recorded role '" + recordedRole
                    + "' not registered — falling back to " + FALLBACK_ROLE);
        }
        int fallback = NPCPlugin.get().getIndex(FALLBACK_ROLE);
        if (fallback == Integer.MIN_VALUE) return null;
        return FALLBACK_ROLE;
    }
}
