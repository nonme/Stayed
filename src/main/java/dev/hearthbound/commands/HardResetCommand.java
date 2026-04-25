package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.building.BuildingSystem;
import dev.hearthbound.npc.ElfSage;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.village.VillageData;

import java.util.UUID;

/**
 * Full mod reset: wipes village progress, despawns the player's elf, and respawns
 * a fresh wanderer elf at the world spawn — same state as a brand-new player.
 * Existing structures (built town hall, warehouses) are NOT torn down; the player
 * can remove them manually if they want a clean slate.
 */
public class HardResetCommand extends AbstractPlayerCommand {

    public HardResetCommand() {
        super("hardreset", "Fully reset mod progress and respawn wanderer elf at world spawn");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef,
                           PlayerRef player, World world) {
        BuildingSystem.get().reset();
        BuildingSystem.get().clearGhostPreview(store, playerRef, world);

        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform != null) {
            Vector3d pos = transform.getPosition();
            BuildingSystem.get().clearOrphanedGhost(world, (int) pos.x, (int) pos.y, (int) pos.z, 20);
        }

        VillageData oldData = store.getComponent(playerRef, VillageData.getComponentType());
        if (oldData != null && oldData.getElfId() != null) {
            UUID elfUuid = oldData.getElfId();
            NpcRegistry.NpcRecord oldRecord = NpcRegistry.get().getRecord(elfUuid);
            long oldChunkIndex = oldRecord != null ? oldRecord.chunkIndex : 0L;
            Entity elf = world.getEntity(elfUuid);
            if (elf != null) {
                elf.remove();
            } else {
                // Elf is in an unloaded chunk — defer removal to NpcChunkLoadHandler.
                NpcRegistry.get().markForRemoval(elfUuid, oldChunkIndex);
            }
        }

        // Wipe every Elf_Sage NPC near the spawn point — catches orphans from prior
        // hardresets/sessions where an elf lost its UUID association and kept existing.
        com.hypixel.hytale.math.vector.Vector3d spawn = ElfSage.getWanderSpawnPosition(world);
        if (spawn != null) {
            ElfSage.purgeOrphanedElfSages(store, world, spawn, 8.0, null);
        }

        // Clear only NPC records — pending removals must survive to delete unloaded entities.
        NpcRegistry.get().clearRecords();
        dev.hearthbound.npc.HearthboundDataStore.get().save();

        // Fresh village data — all flags (metElf, foundingStoneGiven, etc.) default back to
        // their initial state so the opening dialogue plays again.
        store.putComponent(playerRef, VillageData.getComponentType(), new VillageData());

        // Spawn a fresh wanderer elf at world spawn, same as first player join.
        ElfSage.spawnIfNeeded(store, playerRef, world);

        ctx.sendMessage(Message.raw("Hard reset complete — a new wanderer elf is waiting at spawn."));
    }
}
