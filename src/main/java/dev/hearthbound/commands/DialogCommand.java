package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.ui.TestDialogPage;

public class DialogCommand extends AbstractPlayerCommand {

    public DialogCommand() {
        super("dialog", "Open a test dialog page");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
        Player playerEntity = store.getComponent(playerRef, Player.getComponentType());
        if (playerEntity == null) {
            ctx.sendMessage(Message.raw("Could not get player entity."));
            return;
        }

        TestDialogPage page = new TestDialogPage(player,
                "Welcome to Hearthbound",
                "This is a test dialog. Press ESC to close.");

        playerEntity.getPageManager().openCustomPage(playerRef, store, page);
        ctx.sendMessage(Message.raw("Dialog opened."));
    }
}
