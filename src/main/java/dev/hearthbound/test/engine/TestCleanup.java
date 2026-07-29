package dev.hearthbound.test.engine;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hearthbound.npc.HearthboundDataStore;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.npc.StayedIntegrationTestNpcMarkerComponent;
import dev.hearthbound.npc.StayedNpcIdentityComponent;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;
import dev.hearthbound.village.VillagerSummary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
/**
 * Centralised cleanup logic for the integration test framework.
 *
 * Two layers, each safe to call on its own:
 * <ol>
 *   <li>{@link #removeTestNpcs} — removes every entity carrying
 *       {@link StayedIntegrationTestNpcMarkerComponent} in loaded chunks AND
 *       every {@link NpcRegistry.NpcRecord} whose {@code testMarker} is set.
 *       Records whose chunk is unloaded are queued for deferred removal.</li>
 *   <li>{@link #removeTestBuildingsAndOrphans} — drops every {@link BuildingRecord}
 *       with a non-null testMarker from the player's village, and prunes any
 *       orphaned {@link VillagerSummary} (UUID no longer in registry).</li>
 * </ol>
 *
 * Both functions are best-effort; they never throw and return a {@link Result}
 * with counters for diagnostics. The framework calls them from teardown; the
 * {@code /hb test cleanup} command calls them from the player command thread
 * for cross-session recovery.
 */
public final class TestCleanup {

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("test");
    private TestCleanup() {}

    public static final class Result {
        public int entitiesRemoved;
        public int registryUnregistered;
        public int markedForDeferredRemoval;
        public int villageSummariesRemoved;
        public int villageBuildingsRemoved;

        @Override
        public String toString() {
            return "entities=" + entitiesRemoved
                    + " registry=" + registryUnregistered
                    + " deferred=" + markedForDeferredRemoval
                    + " summaries=" + villageSummariesRemoved
                    + " buildings=" + villageBuildingsRemoved;
        }
    }

    /**
     * Walks loaded chunks and removes every test-marker NPC entity, then
     * unregisters every registry record with a non-null testMarker (from any
     * test, whether the entity is loaded or not). Returns a populated Result.
     *
     * Callers are responsible for invoking {@link HearthboundDataStore#save()}
     * afterwards if they want the changes to persist immediately — this method
     * does not save by itself, so the building-cleanup pass can run first and
     * we save once at the end.
     */
    public static Result removeTestNpcs(World world, Store<EntityStore> store) {
        Result result = new Result();
        Set<UUID> entityUuidsRemoved = new HashSet<>();
        Set<String> testNpcIds = new HashSet<>();
        List<Ref<EntityStore>> refsToRemove = new ArrayList<>();

        for (NpcRegistry.NpcRecord rec : NpcRegistry.get().allRecords()) {
            boolean hasTestMarker = rec.testMarker != null && !rec.testMarker.isBlank();
            if (hasTestMarker && rec.npcId != null && !rec.npcId.isBlank()) {
                testNpcIds.add(rec.npcId);
            }
        }

        // Layer 1: walk loaded chunks for test-marker entities and stale
        // Stayed-managed orphan entities. The latter are already broken: they
        // carry HB_NPCID but no registry record can restore skin/interactions.
        // Test entities can lose the entity-side marker after a chunk unload,
        // so registry-side test markers are also matched through HB_NPCID.
        Archetype<EntityStore> npcQuery = Archetype.of(NPCEntity.getComponentType());
        store.forEachChunk(npcQuery, (chunk, buf) -> {
            for (int i = 0; i < chunk.size(); i++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(i);
                StayedIntegrationTestNpcMarkerComponent marker = store.getComponent(
                        ref, StayedIntegrationTestNpcMarkerComponent.getComponentType());
                StayedNpcIdentityComponent identity = store.getComponent(
                        ref, StayedNpcIdentityComponent.getComponentType());
                boolean orphanManagedNpc = false;
                boolean registryMarkedTestNpc = false;
                if (identity != null && identity.getNpcId() != null && !identity.getNpcId().isBlank()) {
                    orphanManagedNpc = NpcRegistry.get().getRecordByNpcId(identity.getNpcId()) == null;
                    registryMarkedTestNpc = testNpcIds.contains(identity.getNpcId());
                }
                if (marker == null && !orphanManagedNpc && !registryMarkedTestNpc) continue;
                UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
                if (uc != null && uc.getUuid() != null) entityUuidsRemoved.add(uc.getUuid());
                refsToRemove.add(ref);
            }
        });

        for (Ref<EntityStore> ref : refsToRemove) {
            try {
                store.removeEntity(ref, RemoveReason.REMOVE);
                result.entitiesRemoved++;
            } catch (Exception e) {
                LOG.warn("TestCleanup: removeEntity failed: " + e.getMessage());
            }
        }

        // Layer 2: every registry record that either (a) was just physically
        // removed in Layer 1 or (b) has a testMarker (covers unloaded chunks
        // and records from old sessions that predated testMarker persistence).
        // Also unregisters records whose entity was removed in Layer 1 but
        // whose registry entry survived — prevents MISSING_ENTITY audit noise.
        List<NpcRegistry.NpcRecord> toUnregister = new ArrayList<>();
        for (NpcRegistry.NpcRecord rec : NpcRegistry.get().allRecords()) {
            boolean hasTestMarker = rec.testMarker != null && !rec.testMarker.isBlank();
            boolean entityJustRemoved = rec.entityUuid != null
                    && entityUuidsRemoved.contains(rec.entityUuid);
            if (hasTestMarker || entityJustRemoved) {
                toUnregister.add(rec);
            }
        }

        for (NpcRegistry.NpcRecord rec : toUnregister) {
            UUID entityUuid = rec.entityUuid;
            if (entityUuid != null) {
                if (!entityUuidsRemoved.contains(entityUuid)
                        && world.getEntity(entityUuid) == null) {
                    NpcRegistry.get().markForRemoval(rec.worldUuid, rec.worldName, entityUuid, rec.chunkIndex);
                    result.markedForDeferredRemoval++;
                }
                NpcRegistry.get().unregister(entityUuid);
                entityUuidsRemoved.add(entityUuid);
            } else {
                NpcRegistry.get().unregisterByNpcId(rec.npcId);
            }
            result.registryUnregistered++;
        }

        // Stash for the caller so it can prune village summaries by UUID.
        // We attach it to the result via a side channel — keep it simple by
        // returning the set explicitly through a helper.
        cachedRemovedUuids.set(entityUuidsRemoved);
        return result;
    }

    /**
     * Cleans up village-side leftovers from removed test NPCs and buildings.
     * Must be called <em>after</em> {@link #removeTestNpcs} in the same
     * cleanup pass — it consults the UUIDs the previous call removed.
     */
    public static void removeTestBuildingsAndOrphans(
            World world, Store<EntityStore> store, Ref<EntityStore> playerRef,
            Result result) {
        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village == null) return;

        boolean changed = false;

        // Drop test-tagged buildings.
        int beforeBuildings = village.getBuildings().size();
        village.getBuildings().removeIf(b -> b.getTestMarker() != null && !b.getTestMarker().isEmpty());
        result.villageBuildingsRemoved = beforeBuildings - village.getBuildings().size();
        if (result.villageBuildingsRemoved > 0) changed = true;

        // Remove every villager summary whose UUID is no longer in the registry,
        // using VillageManager.removeVillager so building slots are also cleared.
        List<UUID> toEvict = new ArrayList<>();
        for (VillagerSummary s : village.getVillagers()) {
            UUID id = s.getVillagerUuid();
            if (id != null && NpcRegistry.get().getRecord(id) == null) {
                toEvict.add(id);
            }
        }
        for (UUID id : toEvict) {
            VillageManager.get().removeVillager(village, id);
            result.villageSummariesRemoved++;
            changed = true;
        }

        // Sweep any remaining stale building slots (e.g. legacy buildings whose
        // assignedVillagerId was never in the registry to begin with).
        Set<UUID> liveUuids = new HashSet<>();
        for (VillagerSummary s : village.getVillagers()) {
            if (s.getVillagerUuid() != null) liveUuids.add(s.getVillagerUuid());
        }
        int staleCleared = VillageManager.get().removeStaleAssignments(village, liveUuids);
        if (staleCleared > 0) changed = true;

        if (changed) {
            VillageManager.get().save(store, playerRef, village);
        }

        cachedRemovedUuids.remove();
    }

    /** Convenience: full pass + persist registry. */
    public static Result fullCleanup(World world, Store<EntityStore> store,
                                     Ref<EntityStore> playerRef) {
        Result r = removeTestNpcs(world, store);
        removeTestBuildingsAndOrphans(world, store, playerRef, r);
        if (r.registryUnregistered > 0 || r.markedForDeferredRemoval > 0) {
            HearthboundDataStore.get().save();
        }
        return r;
    }

    /**
     * Thread-local because cleanup runs on the world thread, but we want to
     * keep removeTestNpcs and removeTestBuildingsAndOrphans as separate calls
     * (so a caller can interleave logging or persist between them) without
     * inventing a public mutable state container.
     */
    private static final ThreadLocal<Set<UUID>> cachedRemovedUuids = new ThreadLocal<>();
}
