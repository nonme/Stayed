package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
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
import dev.hearthbound.npc.ElfSage;
import dev.hearthbound.npc.HearthboundDataStore;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.quest.RescueQuestManager;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillagerSummary;

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
        if (oldData != null && oldData.getElfId() != null) {
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
                    removeNpc(store, world, uuid);
                }
            }
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
