package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.village.VillageData;

public class LoadCommand extends AbstractPlayerCommand {

    public LoadCommand() {
        super("load", "Load the saved test value");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
        VillageData data = store.getComponent(playerRef, VillageData.getComponentType());
        if (data == null) {
            ctx.sendMessage(Message.raw("No saved data found."));
            return;
        }

        ctx.sendMessage(Message.raw("Stage: " + data.getStage() + ", Village: " + data.getVillageName()));
    }
}
