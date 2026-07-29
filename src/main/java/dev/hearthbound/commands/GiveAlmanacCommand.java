package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Dev command: places the Founder's Almanac into the player's inventory.
 * Production flow will hand it out via the elf's intro dialog instead.
 */
public class GiveAlmanacCommand extends AbstractPlayerCommand {

    private static final String ITEM_ID = "Stayed_Founders_Almanac";

    public GiveAlmanacCommand() {
        super("almanac", "Give the player a Founder's Almanac (dev)");
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef,
                           PlayerRef player, World world) {
        Player playerEntity = store.getComponent(playerRef, Player.getComponentType());
        if (playerEntity == null) {
            ctx.sendMessage(Message.raw("Could not get player entity."));
            return;
        }
        var tx = playerEntity.getInventory().getCombinedHotbarFirst()
                .addItemStack(new ItemStack(ITEM_ID, 1));
        ctx.sendMessage(Message.raw(tx.succeeded()
                ? "Founder's Almanac added to your inventory."
                : "Inventory full — could not add Founder's Almanac."));
    }
}
