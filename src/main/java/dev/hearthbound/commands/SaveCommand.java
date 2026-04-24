package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.village.VillageData;

public class SaveCommand extends AbstractPlayerCommand {

    private final RequiredArg<Integer> valueArg;

    public SaveCommand() {
        super("save", "Save a test value persistently");
        valueArg = withRequiredArg("value", "Value to save", ArgTypes.INTEGER);
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
        int value = ctx.get(valueArg);

        VillageData data = store.getComponent(playerRef, VillageData.getComponentType());
        if (data == null) {
            data = new VillageData();
        }
        data.setStage(value);

        store.putComponent(playerRef, VillageData.getComponentType(), data);
        ctx.sendMessage(Message.raw("Saved stage value: " + value));
    }
}
