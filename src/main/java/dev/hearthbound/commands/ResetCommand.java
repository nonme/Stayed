package dev.hearthbound.commands;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.HearthboundPlugin;
import dev.hearthbound.building.BuildingSystem;
import dev.hearthbound.building.PathwayBuilder;
import dev.hearthbound.npc.ElfSage;
import dev.hearthbound.npc.HearthboundDataStore;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.npc.StayedNpcIdentityComponent;
import dev.hearthbound.quest.RescueQuestManager;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillagerSummary;

import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Wipes the player's village state and respawns a fresh wanderer elf at world spawn —
 * same state as a brand-new player. Existing built structures (walls, roofs, etc.) stay
 * in the world; only anchor blocks are removed so the player can place new ones.
 */
public class ResetCommand extends AbstractPlayerCommand {

    public ResetCommand() {
        super("reset", "Wipe village data and start fresh (existing buildings remain in the world)");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef,
                           PlayerRef player, World world) {
        VillageData oldData = store.getComponent(playerRef, VillageData.getComponentType());

        // Cancel active construction and clear ghost previews for all players.
        BuildingSystem.get().reset(store);

        // Cancel objectives + remove their visual markers before deleting RESCUE NPCs,
        // so dangling objective UUIDs don't keep pointing at NPCs we're about to remove.
        Player playerEntity = store.getComponent(playerRef, Player.getComponentType());
        RescueQuestManager.cancelAllObjectives(store, playerRef, playerEntity);
        RescueQuestManager.removeAllMarkers(store);
        RescueQuestManager.cleanup(store);

        // Despawn the elf — defers to chunk load if it's in an unloaded chunk.
        Set<UUID> doomedNpcUuids = new HashSet<>();
        if (oldData != null && oldData.getElfId() != null) {
            doomedNpcUuids.add(oldData.getElfId());
            removeNpc(store, world, oldData.getElfId());
        }

        // Wipe orphaned Elf_Sage NPCs near the spawn point — catches sages that lost
        // their UUID association in past sessions and would otherwise duplicate after spawn.
        Vector3d spawn = ElfSage.getWanderSpawnPosition(world);
        if (spawn != null) {
            ElfSage.purgeOrphanedElfSages(store, world, spawn, 8.0, null);
        }

        // Despawn every villager registered in the old village.
        if (oldData != null) {
            for (VillagerSummary v : oldData.getVillagers()) {
                UUID uuid = v.getVillagerUuid();
                if (uuid != null) {
                    doomedNpcUuids.add(uuid);
                    removeNpc(store, world, uuid);
                }
            }
        }

        // Reset means "wipe Stayed NPC state", not only "wipe NPCs still
        // referenced by VillageData". Old broken test/rescue paths can leave
        // registry records or loaded entities outside the current village;
        // remove them too before clearRecords() would make them orphans.
        for (NpcRegistry.NpcRecord record : NpcRegistry.get().allRecords()) {
            if (record.entityUuid != null) {
                doomedNpcUuids.add(record.entityUuid);
                removeNpc(store, world, record.entityUuid);
            }
        }
        removeLoadedManagedNpcs(store, doomedNpcUuids);

        // Restore grass over registered pathway blocks before we wipe VillageData. Otherwise
        // the registry vanishes and the pathway tiles linger in the world without an undo handle.
        if (oldData != null && !oldData.getPathwayBlocks().isEmpty()) {
            PathwayBuilder.clearAll(world, oldData);
        }

        // Break anchor blocks of every building. Without this F-key on a leftover anchor
        // would either open a UI for a building that no longer exists in VillageData
        // (after the wipe below) or, if the player re-places the same anchor type on top
        // of the broken anchor's coords, the new BuildingRecord would clash with the old
        // one we just dropped.
        if (oldData != null) {
            for (BuildingRecord b : oldData.getBuildings()) {
                String anchorBlockId = BuildingType.getAnchorBlockId(b.getType());
                String currentBlock = readBlockId(world, b.getPosX(), b.getPosY(), b.getPosZ());
                if (anchorBlockId.equals(currentBlock)) {
                    world.breakBlock(b.getPosX(), b.getPosY(), b.getPosZ(), 0);
                }
            }
        }

        // Clear per-villager schedule cache so freed UUIDs don't linger in the scheduler.
        HearthboundPlugin.get().getVillageTickHandler().getVillagerScheduler().clear(store);

        // Wipe the in-memory NPC registry. pendingRemovals is preserved so NPCs queued
        // above for deferred removal still get deleted when their chunks load.
        NpcRegistry.get().clearRecords();
        HearthboundDataStore.get().save();

        // Fresh village data — every quest flag and counter reset to defaults.
        store.putComponent(playerRef, VillageData.getComponentType(), new VillageData());

        // Spawn a new wanderer elf at world spawn, same as first player join.
        ElfSage.spawnIfNeeded(store, playerRef, world);

        ctx.sendMessage(Message.raw(
                "Village reset. Anchor blocks have been removed; existing building structures " +
                "remain in the world — break them manually if you want a clean look. " +
                "A new wanderer elf is waiting at world spawn."));
    }

    /**
     * Removes an NPC by UUID. If its chunk is loaded the entity is deleted directly;
     * otherwise it is queued for removal on next chunk load via NpcRegistry.markForRemoval.
     */
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

    /**
     * Catches every loaded Stayed-managed NPC before reset clears the registry.
     * If it carries HB_NPCID, it belongs to this mod and must not survive a
     * full reset as a future orphan.
     */
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
                // reset is best-effort; deferred removals handle unloaded entities
            }
        }
    }

    /** Returns the block id at the given world coords, or "Empty" if the chunk is unloaded. */
    private static String readBlockId(World world, int x, int y, int z) {
        try {
            var bt = world.getBlockType(x, y, z);
            return bt != null ? bt.getId() : "Empty";
        } catch (Exception e) {
            return "Empty";
        }
    }
}
