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
import dev.hearthbound.util.log.Log;
import dev.hearthbound.util.log.LogConfig;
import dev.hearthbound.util.log.LogDump;
import dev.hearthbound.util.log.LogLevel;

import java.nio.file.Path;
import java.util.Map;

/**
 * /hb log — runtime control over the structured logger.
 *
 *   /hb log list                  — show category → level overrides
 *   /hb log set <cat> <level>     — set per-category level (TRACE..ERROR)
 *   /hb log dump                  — package the in-memory ring + rotated NDJSON files into a zip
 *   /hb log tail <cat> [seconds]  — temporarily force category to console (default 60s)
 *   /hb log mute <cat> [seconds]  — temporarily silence category in console (default 300s)
 *   /hb log reload                — re-read mods/HearthboundData/logging.json
 */
public class LogCommand extends AbstractCommandCollection {

    public LogCommand() {
        super("log", "Structured logger control");
        addSubCommand(new ListCommand());
        addSubCommand(new SetCommand());
        addSubCommand(new DumpCommand());
        addSubCommand(new TailCommand());
        addSubCommand(new MuteCommand());
        addSubCommand(new ReloadCommand());
    }

    private static class ListCommand extends AbstractPlayerCommand {
        ListCommand() { super("list", "Show per-category log levels"); }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            Map<String, LogLevel> snap = LogConfig.get().snapshot();
            ctx.sendMessage(Message.raw("[log] " + snap.size() + " categor(ies):"));
            snap.forEach((cat, lvl) ->
                    ctx.sendMessage(Message.raw("  " + cat + " = " + lvl.name())));
        }
    }

    private static class SetCommand extends AbstractPlayerCommand {
        private final DefaultArg<String> catArg;
        private final DefaultArg<String> lvlArg;

        SetCommand() {
            super("set", "Set log level: /hb log set <category> <TRACE|DEBUG|INFO|WARN|ERROR>");
            catArg = withDefaultArg("category", "Category name", ArgTypes.STRING, "root", "root");
            lvlArg = withDefaultArg("level", "Log level", ArgTypes.STRING, "INFO", "INFO");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            String cat = ctx.get(catArg);
            String lvlStr = ctx.get(lvlArg);
            LogLevel lvl = LogLevel.parseOrDefault(lvlStr, null);
            if (lvl == null) {
                ctx.sendMessage(Message.raw("[log] invalid level: " + lvlStr + " (use TRACE|DEBUG|INFO|WARN|ERROR)"));
                return;
            }
            LogConfig.get().set(cat, lvl);
            ctx.sendMessage(Message.raw("[log] " + cat + " = " + lvl.name() + " (saved to logging.json)"));
        }
    }

    private static class DumpCommand extends AbstractPlayerCommand {
        DumpCommand() { super("dump", "Package ring buffer + rotated logs into a zip for bug reports"); }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            try {
                Path zip = LogDump.create();
                ctx.sendMessage(Message.raw("[log] dump written to " + zip.toAbsolutePath()));
            } catch (Exception e) {
                ctx.sendMessage(Message.raw("[log] dump failed: " + e.getMessage()));
            }
        }
    }

    private static class TailCommand extends AbstractPlayerCommand {
        private final DefaultArg<String> catArg;
        private final DefaultArg<Integer> secsArg;

        TailCommand() {
            super("tail", "Force category to console for N seconds: /hb log tail <category> [60]");
            catArg = withDefaultArg("category", "Category name", ArgTypes.STRING, "root", "root");
            secsArg = withDefaultArg("seconds", "Duration in seconds", ArgTypes.INTEGER, 60, "60");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            String cat = ctx.get(catArg);
            int secs = ctx.get(secsArg);
            Log.tail(cat, secs * 1_000L);
            ctx.sendMessage(Message.raw("[log] tailing " + cat + " for " + secs + "s"));
        }
    }

    private static class MuteCommand extends AbstractPlayerCommand {
        private final DefaultArg<String> catArg;
        private final DefaultArg<Integer> secsArg;

        MuteCommand() {
            super("mute", "Silence category in console for N seconds (archive still gets it)");
            catArg = withDefaultArg("category", "Category name", ArgTypes.STRING, "root", "root");
            secsArg = withDefaultArg("seconds", "Duration in seconds", ArgTypes.INTEGER, 300, "300");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            String cat = ctx.get(catArg);
            int secs = ctx.get(secsArg);
            Log.mute(cat, secs * 1_000L);
            ctx.sendMessage(Message.raw("[log] muted " + cat + " for " + secs + "s"));
        }
    }

    private static class ReloadCommand extends AbstractPlayerCommand {
        ReloadCommand() { super("reload", "Re-read mods/HearthboundData/logging.json"); }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            LogConfig.get().reload();
            ctx.sendMessage(Message.raw("[log] config reloaded"));
        }
    }
}
