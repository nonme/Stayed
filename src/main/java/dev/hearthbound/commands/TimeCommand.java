package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class TimeCommand extends AbstractPlayerCommand {

    private final RequiredArg<Integer> hourArg;

    public TimeCommand() {
        super("time", "Set game time (0-23)");
        hourArg = withRequiredArg("hour", "Hour of day (0-23)", ArgTypes.INTEGER);
    }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef, PlayerRef player, World world) {
        int hour = ctx.get(hourArg);
        if (hour < 0 || hour > 23) {
            ctx.sendMessage(Message.raw("Hour must be 0-23."));
            return;
        }

        // dayTime 0.0 = midnight, 0.5 = noon (hour/24)
        double dayTime = hour / 24.0;

        WorldTimeResource timeResource = (WorldTimeResource) store.getResource(WorldTimeResource.getResourceType());
        if (timeResource == null) {
            ctx.sendMessage(Message.raw("Time resource not available."));
            return;
        }

        timeResource.setDayTime(dayTime, world, store);
        ctx.sendMessage(Message.raw("Time set to " + hour + ":00"));
    }
}
