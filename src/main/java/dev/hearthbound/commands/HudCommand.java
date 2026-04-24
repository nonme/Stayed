package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.ui.TestHud;

public class HudCommand extends AbstractPlayerCommand {

    public HudCommand() {
        super("hud", "Toggle the test HUD overlay");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
        Player playerEntity = store.getComponent(playerRef, Player.getComponentType());
        if (playerEntity == null) {
            ctx.sendMessage(Message.raw("Could not get player entity."));
            return;
        }

        if (playerEntity.getHudManager().getCustomHud() != null) {
            // Hide by sending empty update (setCustomHud(null) crashes the client)
            UICommandBuilder builder = new UICommandBuilder();
            builder.set("#HudRoot.Visible", false);
            playerEntity.getHudManager().getCustomHud().update(false, builder);
            ctx.sendMessage(Message.raw("HUD hidden."));
        } else {
            TestHud hud = new TestHud(player);
            playerEntity.getHudManager().setCustomHud(player, hud);
            ctx.sendMessage(Message.raw("HUD shown."));
        }
    }
}
