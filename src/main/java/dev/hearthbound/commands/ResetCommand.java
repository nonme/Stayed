package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.village.ResetVillageService;

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
        ResetVillageService.reset(store, playerRef, world);
        ctx.sendMessage(Message.raw(ResetVillageService.DEFAULT_MESSAGE));
    }
}
