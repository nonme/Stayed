package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
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
        addSubCommand(new ExtraCommand());
        addSubCommand(new AllCommand());
        addSubCommand(new ClearCommand());
        addSubCommand(new CountCommand());
    }

    /** /hb pathways generate <strategy> — base strategy only (no commute, no warehouse hub). */
    private static class GenerateCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> strategyArg =
                withRequiredArg("strategy", "mst | knn | hub | shortcuts", ArgTypes.STRING);
        private final OptionalArg<Double> ratioArg =
                withOptionalArg("ratio", "Detour-ratio threshold for SHORTCUTS (default 2)", ArgTypes.DOUBLE);

        GenerateCommand() {
            super("generate", "Build base-strategy pathways only (mst/knn/hub/shortcuts)");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store,
                               Ref<EntityStore> playerRef, PlayerRef player, World world) {
            VillageData village = VillageManager.get().getVillageData(store, playerRef);
            if (village == null || !village.isFounded()) {
                ctx.sendMessage(Message.raw("No village found."));
                return;
            }
            String raw = ctx.get(strategyArg);
            PathwayBuilder.Strategy strategy = PathwayBuilder.Strategy.parse(raw);
            if (strategy == null) {
                ctx.sendMessage(Message.raw("Unknown strategy '" + raw
                        + "'. Use mst | knn | hub | shortcuts. (Did you mean /hb pathways extra?)"));
                return;
            }
            Double ratio = ctx.provided(ratioArg) ? ctx.get(ratioArg) : null;
            int placed = PathwayBuilder.connectBase(world, village, strategy, ratio);
            VillageManager.get().save(store, playerRef, village);
            String ratioInfo = (strategy == PathwayBuilder.Strategy.MST_PLUS_SHORTCUTS && ratio != null)
                    ? " ratio=" + ratio : "";
            ctx.sendMessage(Message.raw("Base pathways [" + strategy + "]" + ratioInfo
                    + ": " + placed + " blocks placed, "
                    + village.getPathwayBlocks().size() + " total in registry."));
        }
    }

    /** /hb pathways extra — only commute (home↔workplace) and warehouse-hub edges. */
    private static class ExtraCommand extends AbstractPlayerCommand {
        ExtraCommand() {
            super("extra", "Add only commute + warehouse-hub pathways on top of existing");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store,
                               Ref<EntityStore> playerRef, PlayerRef player, World world) {
            VillageData village = VillageManager.get().getVillageData(store, playerRef);
            if (village == null || !village.isFounded()) {
                ctx.sendMessage(Message.raw("No village found."));
                return;
            }
            int placed = PathwayBuilder.connectExtra(world, village);
            VillageManager.get().save(store, playerRef, village);
            ctx.sendMessage(Message.raw("Extra pathways: " + placed + " blocks placed, "
                    + village.getPathwayBlocks().size() + " total in registry."));
        }
    }

    /** /hb pathways all <strategy> — base strategy followed by extra layer in one call. */
    private static class AllCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> strategyArg =
                withRequiredArg("strategy", "mst | knn | hub | shortcuts", ArgTypes.STRING);
        private final OptionalArg<Double> ratioArg =
                withOptionalArg("ratio", "Detour-ratio threshold for SHORTCUTS (default 2)", ArgTypes.DOUBLE);

        AllCommand() {
            super("all", "Build base + commute + warehouse-hub pathways in one go");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store,
                               Ref<EntityStore> playerRef, PlayerRef player, World world) {
            VillageData village = VillageManager.get().getVillageData(store, playerRef);
            if (village == null || !village.isFounded()) {
                ctx.sendMessage(Message.raw("No village found."));
                return;
            }
            String raw = ctx.get(strategyArg);
            PathwayBuilder.Strategy strategy = PathwayBuilder.Strategy.parse(raw);
            if (strategy == null) {
                ctx.sendMessage(Message.raw("Unknown strategy '" + raw
                        + "'. Use mst | knn | hub | shortcuts."));
                return;
            }
            Double ratio = ctx.provided(ratioArg) ? ctx.get(ratioArg) : null;
            int placed = PathwayBuilder.connectAll(world, village, strategy, ratio);
            VillageManager.get().save(store, playerRef, village);
            String ratioInfo = (strategy == PathwayBuilder.Strategy.MST_PLUS_SHORTCUTS && ratio != null)
                    ? " ratio=" + ratio : "";
            ctx.sendMessage(Message.raw("All pathways [" + strategy + "]" + ratioInfo
                    + ": " + placed + " blocks placed, "
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
