package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.building.CraftabilityIndex;

public class CraftabilityCommand extends AbstractCommandCollection {

    public CraftabilityCommand() {
        super("craftability", "Query item obtain sources (craftable/gatherable/none)");
        addSubCommand(new StatsCommand());
        addSubCommand(new CheckCommand());
        addSubCommand(new DumpCommand());
    }

    /** Prints index summary counts. */
    private static class StatsCommand extends AbstractPlayerCommand {
        StatsCommand() { super("stats", "Print CraftabilityIndex summary"); }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            if (CraftabilityIndex.isEmpty()) {
                ctx.sendMessage(Message.raw("[craftability] Index not built yet — server may still be loading."));
                return;
            }
            ctx.sendMessage(Message.raw("[craftability] Index size: " + CraftabilityIndex.size() + " items"));
        }
    }

    /** Saves the current index to disk immediately. */
    private static class DumpCommand extends AbstractPlayerCommand {
        DumpCommand() { super("dump", "Save craftability index to mods/HearthboundData/craftability.json"); }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            if (CraftabilityIndex.isEmpty()) {
                ctx.sendMessage(Message.raw("[craftability] Index not built yet."));
                return;
            }
            CraftabilityIndex.saveToDisk();
            ctx.sendMessage(Message.raw("[craftability] Saved " + CraftabilityIndex.size() + " items to mods/HearthboundData/craftability.json"));
        }
    }

    /** Checks a single item ID. Usage: /hb craftability check <itemId> */
    private static class CheckCommand extends AbstractPlayerCommand {
        private final DefaultArg<String> itemIdArg;

        CheckCommand() {
            super("check", "Check obtain source for an item: /hb craftability check <itemId>");
            itemIdArg = withDefaultArg("itemId", "Item ID to check", ArgTypes.STRING, "Wood_Softwood_Planks", "Wood_Softwood_Planks");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            String itemId = ctx.get(itemIdArg);
            CraftabilityIndex.ObtainSource source = CraftabilityIndex.getSource(itemId);
            ctx.sendMessage(Message.raw("[craftability] " + itemId + " -> " + source
                    + (CraftabilityIndex.isFree(itemId) ? " (FREE in construction)" : " (costs resources)")));
        }
    }
}
