package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.building.PathwayBuilder;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

/**
 * /hb pathways generate|clear|count — debug commands for the pathway network.
 * Useful while iterating: regenerate after tuning A* weights, or wipe before
 * trying a new layout.
 */
public class PathwaysCommand extends AbstractCommandCollection {

    public PathwaysCommand() {
        super("pathways", "Generate, clear, or inspect village pathways");
        addSubCommand(new GenerateCommand());
        addSubCommand(new ClearCommand());
        addSubCommand(new CountCommand());
    }

    private static class GenerateCommand extends AbstractPlayerCommand {
        GenerateCommand() {
            super("generate", "Build pathways between all completed buildings (MST)");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store,
                               Ref<EntityStore> playerRef, PlayerRef player, World world) {
            VillageData village = VillageManager.get().getVillageData(store, playerRef);
            if (village == null || !village.isFounded()) {
                ctx.sendMessage(Message.raw("No village found."));
                return;
            }
            int placed = PathwayBuilder.connectAll(world, village);
            VillageManager.get().save(store, playerRef, village);
            ctx.sendMessage(Message.raw("Pathways generated: " + placed + " blocks placed, "
                    + village.getPathwayBlocks().size() + " total in registry."));
        }
    }

    private static class ClearCommand extends AbstractPlayerCommand {
        ClearCommand() {
            super("clear", "Remove all registered pathway blocks (restore grass)");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store,
                               Ref<EntityStore> playerRef, PlayerRef player, World world) {
            VillageData village = VillageManager.get().getVillageData(store, playerRef);
            if (village == null || !village.isFounded()) {
                ctx.sendMessage(Message.raw("No village found."));
                return;
            }
            int registered = village.getPathwayBlocks().size();
            int restored = PathwayBuilder.clearAll(world, village);
            VillageManager.get().save(store, playerRef, village);
            ctx.sendMessage(Message.raw("Pathways cleared: " + restored + " blocks restored to grass ("
                    + registered + " were registered)."));
        }
    }

    private static class CountCommand extends AbstractPlayerCommand {
        CountCommand() {
            super("count", "Show how many pathway blocks are registered");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store,
                               Ref<EntityStore> playerRef, PlayerRef player, World world) {
            VillageData village = VillageManager.get().getVillageData(store, playerRef);
            if (village == null || !village.isFounded()) {
                ctx.sendMessage(Message.raw("No village found."));
                return;
            }
            ctx.sendMessage(Message.raw("Registered pathway blocks: "
                    + village.getPathwayBlocks().size()));
        }
    }
}
