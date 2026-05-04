package dev.hearthbound.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.test.engine.StayedIntegrationTestRunner;
import dev.hearthbound.test.engine.TestCase;
import dev.hearthbound.test.engine.TestCleanup;
import dev.hearthbound.test.engine.TestPackage;
import dev.hearthbound.test.packages.ChunksPackage;
import dev.hearthbound.test.packages.HousingPackage;
import dev.hearthbound.test.packages.RolesPackage;
import dev.hearthbound.test.packages.SmokePackage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * {@code /hb test ...} — entry point for the integration test framework.
 *
 * Subcommands:
 * <ul>
 *   <li>{@code /hb test list}   — show all packages</li>
 *   <li>{@code /hb test pkg <name>} — run a whole package</li>
 *   <li>{@code /hb test run <case>} — run a single case from any package</li>
 *   <li>{@code /hb test status} — current step (if running)</li>
 *   <li>{@code /hb test abort}  — request graceful abort</li>
 *   <li>{@code /hb test logs}   — print path to most recent log file</li>
 * </ul>
 */
public class TestCommand extends AbstractCommandCollection {

    /**
     * Static registry of packages available for {@code /hb test pkg}. Each
     * entry is built lazily so package construction can read live state at
     * the moment the test is invoked.
     */
    static final Map<String, Supplier<TestPackage>> PACKAGES = new LinkedHashMap<>();
    static {
        PACKAGES.put("smoke",   SmokePackage::build);
        PACKAGES.put("chunks",  ChunksPackage::build);
        PACKAGES.put("housing", HousingPackage::build);
        PACKAGES.put("roles",   RolesPackage::build);
    }

    public TestCommand() {
        super("test", "Integration test framework");
        addSubCommand(new ListCommand());
        addSubCommand(new PkgCommand());
        addSubCommand(new RunCommand());
        addSubCommand(new StatusCommand());
        addSubCommand(new AbortCommand());
        addSubCommand(new LogsCommand());
        addSubCommand(new CleanupCommand());
    }

    private static Consumer<String> chatHandler(CommandContext ctx) {
        return msg -> ctx.sendMessage(Message.raw(msg));
    }

    // -------------------------------------------------------------------------

    private static class ListCommand extends AbstractPlayerCommand {
        ListCommand() {
            super("list", "List all available test packages and cases");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef,
                               PlayerRef player, World world) {
            StringBuilder sb = new StringBuilder("Test packages:\n");
            for (Map.Entry<String, Supplier<TestPackage>> entry : PACKAGES.entrySet()) {
                TestPackage pkg = entry.getValue().get();
                sb.append("  ").append(entry.getKey())
                        .append(" (").append(pkg.getCases().size()).append(" case(s)):\n");
                for (TestCase c : pkg.getCases()) {
                    sb.append("    - ").append(c.getName())
                            .append(" (").append(c.getSteps().size()).append(" step(s))\n");
                }
            }
            ctx.sendMessage(Message.raw(sb.toString()));
        }
    }

    // -------------------------------------------------------------------------

    private static class PkgCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> nameArg;

        PkgCommand() {
            super("pkg", "Run a test package by name");
            this.nameArg = withRequiredArg("name", "Package name (see /hb test list)", ArgTypes.STRING);
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef,
                               PlayerRef player, World world) {
            String name = ctx.get(nameArg);
            Supplier<TestPackage> supplier = PACKAGES.get(name);
            if (supplier == null) {
                ctx.sendMessage(Message.raw("Unknown package: " + name + ". Try /hb test list."));
                return;
            }
            TestPackage pkg = supplier.get();
            StayedIntegrationTestRunner.get().runPackage(
                    pkg, world, store, playerRef, player, chatHandler(ctx));
        }
    }

    // -------------------------------------------------------------------------

    private static class RunCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> caseArg;

        RunCommand() {
            super("run", "Run a single test case by name (searches every package)");
            this.caseArg = withRequiredArg("case", "Case name (see /hb test list)", ArgTypes.STRING);
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef,
                               PlayerRef player, World world) {
            String wantedCase = ctx.get(caseArg);
            for (Map.Entry<String, Supplier<TestPackage>> entry : PACKAGES.entrySet()) {
                TestPackage pkg = entry.getValue().get();
                for (TestCase c : pkg.getCases()) {
                    if (c.getName().equals(wantedCase)) {
                        StayedIntegrationTestRunner.get().runCase(
                                entry.getKey(), c, world, store, playerRef, player, chatHandler(ctx));
                        return;
                    }
                }
            }
            ctx.sendMessage(Message.raw("Unknown case: " + wantedCase + ". Try /hb test list."));
        }
    }

    // -------------------------------------------------------------------------

    private static class StatusCommand extends AbstractPlayerCommand {
        StatusCommand() {
            super("status", "Show what the test runner is currently doing");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef,
                               PlayerRef player, World world) {
            StayedIntegrationTestRunner.Status status =
                    StayedIntegrationTestRunner.get().getStatus();
            if (status == null) {
                ctx.sendMessage(Message.raw("Test runner idle"));
            } else {
                ctx.sendMessage(Message.raw("Test runner: " + status));
            }
        }
    }

    // -------------------------------------------------------------------------

    private static class AbortCommand extends AbstractPlayerCommand {
        AbortCommand() {
            super("abort", "Request graceful abort of the running test");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef,
                               PlayerRef player, World world) {
            if (!StayedIntegrationTestRunner.get().isRunning()) {
                ctx.sendMessage(Message.raw("Nothing running"));
                return;
            }
            StayedIntegrationTestRunner.get().requestAbort();
            ctx.sendMessage(Message.raw("Abort requested — runner will stop after the current step"));
        }
    }

    // -------------------------------------------------------------------------

    private static class LogsCommand extends AbstractPlayerCommand {
        LogsCommand() {
            super("logs", "Print the path of the most recent test log file");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef,
                               PlayerRef player, World world) {
            var path = StayedIntegrationTestRunner.get().getLastLogFile();
            if (path == null) {
                ctx.sendMessage(Message.raw("No tests have been run yet"));
            } else {
                ctx.sendMessage(Message.raw("Last log: " + path));
            }
        }
    }

    // -------------------------------------------------------------------------

    private static class CleanupCommand extends AbstractPlayerCommand {
        CleanupCommand() {
            super("cleanup", "Remove all leftover test NPCs and buildings (cross-session recovery)");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> playerRef,
                               PlayerRef player, World world) {
            if (StayedIntegrationTestRunner.get().isRunning()) {
                ctx.sendMessage(Message.raw("Test runner is active — abort first with /hb test abort"));
                return;
            }
            TestCleanup.Result result = TestCleanup.fullCleanup(world, store, playerRef);
            ctx.sendMessage(Message.raw("Test cleanup done: " + result));
        }
    }
}
