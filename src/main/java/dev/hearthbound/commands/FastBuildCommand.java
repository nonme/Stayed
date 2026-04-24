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
 * Debug toggle that collapses the block-by-block build delay down to ~10ms so test
 * iterations don't have to sit through the full animation.
 */
public class FastBuildCommand extends AbstractPlayerCommand {

    public FastBuildCommand() {
        super("fastbuild", "Toggle accelerated construction animation");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef,
                           PlayerRef player, World world) {
        boolean enabled = !BuildingSystem.get().isFastBuild();
        BuildingSystem.get().setFastBuild(enabled);
        ctx.sendMessage(Message.raw(enabled
                ? "Fast build ON — construction runs at full speed."
                : "Fast build OFF — construction back to normal pacing."));
    }
}
