package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.building.BuildingSystem;

/**
 * Debug command that immediately finishes the currently-running construction.
 * Bypasses resource checks — intended for iterating on a prefab or mechanic
 * without waiting for the full animation.
 */
public class InstaBuildCommand extends AbstractPlayerCommand {

    public InstaBuildCommand() {
        super("instabuild", "Finish the current construction instantly");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef,
                           PlayerRef player, World world) {
        boolean finished = BuildingSystem.get().finishActiveBuildNow();
        ctx.sendMessage(Message.raw(finished
                ? "Construction finished instantly."
                : "Nothing is being built right now."));
    }
}
