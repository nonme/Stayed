package dev.hearthbound.village;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hearthbound.HearthboundPlugin;
import dev.hearthbound.building.BuildingSystem;
import dev.hearthbound.building.PathwayBuilder;
import dev.hearthbound.npc.ElfSage;
import dev.hearthbound.npc.HearthboundDataStore;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.npc.StayedNpcIdentityComponent;
import dev.hearthbound.quest.RescueQuestManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Shared implementation behind /hb reset and integration tests that need a
 * fresh-player-like state. Keep reset semantics here so tests do not copy a
 * subtly different cleanup path.
 */
public final class ResetVillageService {

    public static final String DEFAULT_MESSAGE =
            "Village reset. Anchor blocks have been removed; existing building structures "
                    + "remain in the world — break them manually if you want a clean look. "
                    + "A new wanderer elf is waiting at world spawn.";

    private ResetVillageService() {}

    public static void reset(Store<EntityStore> store, Ref<EntityStore> playerRef, World world) {
        VillageData oldData = store.getComponent(playerRef, VillageData.getComponentType());

        BuildingSystem.get().reset(store);

        Player playerEntity = store.getComponent(playerRef, Player.getComponentType());
        RescueQuestManager.cancelAllObjectives(store, playerRef, playerEntity);
        RescueQuestManager.removeAllMarkers(store);
        RescueQuestManager.cleanup(store);

        Set<UUID> doomedNpcUuids = new HashSet<>();
        if (oldData != null && oldData.getElfId() != null) {
            doomedNpcUuids.add(oldData.getElfId());
            removeNpc(store, world, oldData.getElfId());
        }

        Vector3d spawn = ElfSage.getWanderSpawnPosition(world);
        if (spawn != null) {
            ElfSage.purgeOrphanedElfSages(store, world, spawn, 8.0, null);
        }

        if (oldData != null) {
            for (VillagerSummary v : oldData.getVillagers()) {
                UUID uuid = v.getVillagerUuid();
                if (uuid != null) {
                    doomedNpcUuids.add(uuid);
                    removeNpc(store, world, uuid);
                }
            }
        }

        for (NpcRegistry.NpcRecord record : NpcRegistry.get().allRecords()) {
            if (record.entityUuid != null) {
                doomedNpcUuids.add(record.entityUuid);
                removeNpc(store, world, record.entityUuid);
            }
        }
        removeLoadedManagedNpcs(store, doomedNpcUuids);

        if (oldData != null && !oldData.getPathwayBlocks().isEmpty()) {
            PathwayBuilder.clearAll(world, oldData);
        }

        if (oldData != null) {
            for (BuildingRecord b : oldData.getBuildings()) {
                String anchorBlockId = BuildingType.getAnchorBlockId(b.getType());
                String currentBlock = readBlockId(world, b.getPosX(), b.getPosY(), b.getPosZ());
                if (anchorBlockId.equals(currentBlock)) {
                    world.breakBlock(b.getPosX(), b.getPosY(), b.getPosZ(), 0);
                }
            }
        }

        HearthboundPlugin.get().getVillageTickHandler().getVillagerScheduler().clear(store);

        NpcRegistry.get().clearRecords();
        HearthboundDataStore.get().save();

        store.putComponent(playerRef, VillageData.getComponentType(), new VillageData());
        ElfSage.spawnIfNeeded(store, playerRef, world);
    }

    private static void removeNpc(Store<EntityStore> store, World world, UUID uuid) {
        Entity entity = world.getEntity(uuid);
        if (entity != null) {
            entity.remove();
            return;
        }
        NpcRegistry.NpcRecord record = NpcRegistry.get().getRecord(uuid);
        long chunkIndex = record != null ? record.chunkIndex : 0L;
        NpcRegistry.get().markForRemoval(uuid, chunkIndex);
    }

    private static void removeLoadedManagedNpcs(Store<EntityStore> store, Set<UUID> doomedNpcUuids) {
        java.util.List<Ref<EntityStore>> refsToRemove = new java.util.ArrayList<>();
        Archetype<EntityStore> npcQuery = Archetype.of(NPCEntity.getComponentType());
        store.forEachChunk(npcQuery, (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(i);
                StayedNpcIdentityComponent identity = store.getComponent(
                        ref, StayedNpcIdentityComponent.getComponentType());
                if (identity == null || identity.getNpcId() == null || identity.getNpcId().isBlank()) {
                    continue;
                }
                UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
                UUID entityUuid = uc != null ? uc.getUuid() : null;
                boolean explicitlyDoomed = entityUuid != null && doomedNpcUuids.contains(entityUuid);
                boolean orphan = NpcRegistry.get().getRecordByNpcId(identity.getNpcId()) == null;
                boolean managedByRegistry = NpcRegistry.get().getRecordByNpcId(identity.getNpcId()) != null;
                if (explicitlyDoomed || orphan || managedByRegistry) {
                    refsToRemove.add(ref);
                }
            }
        });
        for (Ref<EntityStore> ref : refsToRemove) {
            try {
                store.removeEntity(ref, RemoveReason.REMOVE);
            } catch (Exception ignored) {
                // Reset is best-effort; deferred removals handle unloaded entities.
            }
        }
    }

    private static String readBlockId(World world, int x, int y, int z) {
        try {
            var bt = world.getBlockType(x, y, z);
            return bt != null ? bt.getId() : "Empty";
        } catch (Exception e) {
            return "Empty";
        }
    }
}
