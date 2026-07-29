package dev.hearthbound.test.engine;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.test.audit.NpcRegistrySelfCheck;
import dev.hearthbound.util.TickScheduler;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Singleton runner for integration test packages and individual cases.
 *
 * Threading: every step is dispatched via {@link TickScheduler#runLater}, which
 * executes the runnable inside {@code world.execute(...)} — i.e. on the world
 * thread. Between steps the runner inserts a small delay so the engine has a
 * tick or two to apply mutations before the next step runs. Steps that need a
 * longer pause return {@link StepResult#wait(long)}.
 *
 * Single-instance: only one package or case can run at a time. Subsequent
 * starts are rejected with a clear message.
 */
public final class StayedIntegrationTestRunner {

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("test");
    private static final long INTER_STEP_DELAY_MS = 100L;

    private static final StayedIntegrationTestRunner INSTANCE = new StayedIntegrationTestRunner();

    public static StayedIntegrationTestRunner get() { return INSTANCE; }

    /** Snapshot of the in-flight test for {@code /hb test status}. */
    public static final class Status {
        public final String packageName;
        public final String caseName;
        public final String stepName;
        public final int caseIndex;
        public final int caseCount;
        public final int stepIndex;
        public final int stepCount;

        public Status(String packageName, String caseName, String stepName,
                      int caseIndex, int caseCount, int stepIndex, int stepCount) {
            this.packageName = packageName;
            this.caseName = caseName;
            this.stepName = stepName;
            this.caseIndex = caseIndex;
            this.caseCount = caseCount;
            this.stepIndex = stepIndex;
            this.stepCount = stepCount;
        }

        @Override
        public String toString() {
            return packageName + "/" + caseName
                    + " step " + (stepIndex + 1) + "/" + stepCount
                    + " (" + stepName + ")"
                    + ", case " + (caseIndex + 1) + "/" + caseCount;
        }
    }

    private final AtomicReference<Status> currentStatus = new AtomicReference<>(null);
    private volatile boolean abortRequested = false;
    private volatile Path lastLogFile = null;
    private volatile TestReport lastReport = null;

    private StayedIntegrationTestRunner() {}

    public boolean isRunning() { return currentStatus.get() != null; }
    public Status getStatus() { return currentStatus.get(); }
    public Path getLastLogFile() { return lastLogFile; }
    public TestReport getLastReport() { return lastReport; }

    public void requestAbort() {
        if (currentStatus.get() != null) {
            abortRequested = true;
            LOG.info("StayedIntegrationTestRunner: abort requested");
        }
    }

    /**
     * Runs a whole package. Reports start/end via {@code chatHandler} (one line
     * each, suitable for {@link com.hypixel.hytale.server.core.Message}.raw).
     */
    public boolean runPackage(TestPackage pkg, World world, Store<EntityStore> store,
                              Ref<EntityStore> playerRef, PlayerRef player,
                              Consumer<String> chatHandler) {
        if (!startInternal(pkg.getName())) {
            chatHandler.accept("Test runner busy: " + currentStatus.get());
            return false;
        }
        chatHandler.accept("▶ Running package " + pkg.getName()
                + " (" + pkg.getCases().size() + " case(s))");
        TestReport report = new TestReport(pkg.getName());
        lastReport = report;

        NpcRegistrySelfCheck.setPaused(true);
        runCasesAsync(pkg, 0, world, store, playerRef, player, chatHandler, report);
        return true;
    }

    /**
     * Runs a single case as a one-case package — same machinery, just one item.
     */
    public boolean runCase(String packageLabel, TestCase testCase, World world,
                           Store<EntityStore> store, Ref<EntityStore> playerRef,
                           PlayerRef player, Consumer<String> chatHandler) {
        TestPackage pkg = new TestPackage(packageLabel, List.of(testCase));
        return runPackage(pkg, world, store, playerRef, player, chatHandler);
    }

    private boolean startInternal(String packageName) {
        return currentStatus.compareAndSet(null,
                new Status(packageName, "(starting)", "(starting)", 0, 0, 0, 0));
    }

    private void runCasesAsync(TestPackage pkg, int caseIndex, World world,
                               Store<EntityStore> store, Ref<EntityStore> playerRef,
                               PlayerRef player, Consumer<String> chatHandler,
                               TestReport report) {
        if (caseIndex >= pkg.getCases().size() || abortRequested) {
            finish(pkg, report, chatHandler);
            return;
        }

        // Before the very first case run a best-effort cleanup of any leftovers
        // from a previous session that ended with a server crash before teardown.
        TestCase testCase = pkg.getCases().get(caseIndex);
        if (caseIndex == 0 && !testCase.isSkipPreRunCleanup()) {
            TestCleanup.Result pre = TestCleanup.fullCleanup(world, store, playerRef);
            if (pre.entitiesRemoved > 0 || pre.registryUnregistered > 0
                    || pre.villageBuildingsRemoved > 0 || pre.villageSummariesRemoved > 0) {
                LOG.info("StayedIntegrationTestRunner: pre-run cleanup removed stale test state: " + pre);
            }
        }

        chatHandler.accept("▶ " + testCase.getName());

        TestLogger logger = TestLogger.open(pkg.getName() + "_" + testCase.getName());
        lastLogFile = logger.getLogFile();
        logger.info("=== " + pkg.getName() + " / " + testCase.getName() + " ===");
        long caseStartMs = System.currentTimeMillis();

        TestContext ctx = new TestContext(world, store, playerRef, player, logger,
                testCase.getName());
        runStepsAsync(pkg, caseIndex, testCase, 0, ctx, chatHandler, report,
                caseStartMs, logger);
    }

    private void runStepsAsync(TestPackage pkg, int caseIndex, TestCase testCase,
                               int stepIndex, TestContext ctx,
                               Consumer<String> chatHandler, TestReport report,
                               long caseStartMs, TestLogger logger) {
        if (abortRequested) {
            startTeardown(pkg, caseIndex, testCase, ctx, chatHandler, report,
                    caseStartMs, logger, false, "aborted");
            return;
        }
        if (stepIndex >= testCase.getSteps().size()) {
            startTeardown(pkg, caseIndex, testCase, ctx, chatHandler, report,
                    caseStartMs, logger, true, "all steps passed");
            return;
        }

        TestStep step = testCase.getSteps().get(stepIndex);
        currentStatus.set(new Status(
                pkg.getName(), testCase.getName(), step.getName(),
                caseIndex, pkg.getCases().size(),
                stepIndex, testCase.getSteps().size()));

        TickScheduler.runLater(ctx.getWorld(), 0L, () -> {
            StepResult result;
            long stepStart = System.currentTimeMillis();
            logger.info("step " + (stepIndex + 1) + "/" + testCase.getSteps().size()
                    + " " + step.getName() + " — start");
            try {
                result = step.execute(ctx);
            } catch (Exception e) {
                logger.error(step.getName() + " threw: " + e.getMessage());
                result = StepResult.fail("step threw " + e.getClass().getSimpleName()
                        + ": " + e.getMessage());
            }
            long stepMs = System.currentTimeMillis() - stepStart;
            String resultLabel = result.isPassed() ? "PASS" : "FAIL";
            String resultDetails = result.getMessage().isEmpty() ? "" : " — " + result.getMessage();
            logger.info("step " + (stepIndex + 1) + " " + step.getName()
                    + " " + resultLabel + " (" + stepMs + "ms)" + resultDetails);

            if (!result.isPassed()) {
                startTeardown(pkg, caseIndex, testCase, ctx, chatHandler, report,
                        caseStartMs, logger, false,
                        step.getName() + ": " + result.getMessage());
                return;
            }

            long delay = result.getWaitMs() > 0 ? result.getWaitMs() : INTER_STEP_DELAY_MS;
            TickScheduler.runLater(ctx.getWorld(), delay, () ->
                    runStepsAsync(pkg, caseIndex, testCase, stepIndex + 1, ctx,
                            chatHandler, report, caseStartMs, logger));
        });
    }

    /**
     * Entry point into the teardown phase. Always called exactly once per case,
     * regardless of whether {@link #runStepsAsync} ended in pass, fail, or abort.
     * The {@code passed}/{@code message} args carry the verdict from the steps
     * phase. Teardown always continues after failures, but any teardown failure
     * makes the case fail at the end so cleanup regressions cannot be reported
     * as false-positive passes.
     */
    private void startTeardown(TestPackage pkg, int caseIndex, TestCase testCase,
                               TestContext ctx, Consumer<String> chatHandler,
                               TestReport report, long caseStartMs, TestLogger logger,
                               boolean passed, String message) {
        if (testCase.getTeardown().isEmpty()) {
            finishCase(pkg, caseIndex, testCase, ctx, chatHandler, report,
                    caseStartMs, logger, passed, message);
            return;
        }
        logger.info("--- teardown (" + testCase.getTeardown().size() + " step(s)) ---");
        runTeardownAsync(pkg, caseIndex, testCase, 0, ctx, chatHandler, report,
                caseStartMs, logger, passed, message);
    }

    private void runTeardownAsync(TestPackage pkg, int caseIndex, TestCase testCase,
                                  int stepIndex, TestContext ctx,
                                  Consumer<String> chatHandler, TestReport report,
                                  long caseStartMs, TestLogger logger,
                                  boolean passed, String message) {
        if (stepIndex >= testCase.getTeardown().size()) {
            finishCase(pkg, caseIndex, testCase, ctx, chatHandler, report,
                    caseStartMs, logger, passed, message);
            return;
        }

        TestStep step = testCase.getTeardown().get(stepIndex);
        // Status reflects teardown progress so /hb test status stays meaningful
        // while cleanup runs (which can take several seconds for big batches).
        currentStatus.set(new Status(
                pkg.getName(), testCase.getName(), "teardown:" + step.getName(),
                caseIndex, pkg.getCases().size(),
                stepIndex, testCase.getTeardown().size()));

        TickScheduler.runLater(ctx.getWorld(), 0L, () -> {
            StepResult result;
            long stepStart = System.currentTimeMillis();
            logger.info("teardown " + (stepIndex + 1) + "/" + testCase.getTeardown().size()
                    + " " + step.getName() + " — start");
            try {
                result = step.execute(ctx);
            } catch (Exception e) {
                // Teardown exceptions are isolated — log and move on so a broken
                // cleanup step doesn't take down the rest of the cleanup chain.
                logger.error("teardown " + step.getName() + " threw: " + e.getMessage());
                result = StepResult.fail("teardown threw " + e.getClass().getSimpleName()
                        + ": " + e.getMessage());
            }
            long stepMs = System.currentTimeMillis() - stepStart;
            String resultLabel = result.isPassed() ? "PASS" : "FAIL";
            String resultDetails = result.getMessage().isEmpty() ? "" : " — " + result.getMessage();
            logger.info("teardown " + (stepIndex + 1) + " " + step.getName()
                    + " " + resultLabel + " (" + stepMs + "ms)" + resultDetails);

            boolean computedPassed = passed;
            String computedMessage = message;
            if (!result.isPassed()) {
                computedPassed = false;
                String teardownMessage = "teardown " + step.getName() + ": " + result.getMessage();
                computedMessage = message == null || message.isEmpty()
                        ? teardownMessage
                        : message + "; " + teardownMessage;
            }
            final boolean nextPassed = computedPassed;
            final String nextMessage = computedMessage;

            // Teardown always continues so cleanup gets every chance to run.
            long delay = result.getWaitMs() > 0 ? result.getWaitMs() : INTER_STEP_DELAY_MS;
            TickScheduler.runLater(ctx.getWorld(), delay, () ->
                    runTeardownAsync(pkg, caseIndex, testCase, stepIndex + 1, ctx,
                            chatHandler, report, caseStartMs, logger,
                            nextPassed, nextMessage));
        });
    }

    private void finishCase(TestPackage pkg, int caseIndex, TestCase testCase,
                            TestContext ctx, Consumer<String> chatHandler,
                            TestReport report, long caseStartMs, TestLogger logger,
                            boolean passed, String message) {
        long durationMs = System.currentTimeMillis() - caseStartMs;
        logger.info("=== " + (passed ? "PASS" : "FAIL") + " (" + durationMs + "ms) " + message);
        logger.close();

        TestReport.CaseOutcome outcome = new TestReport.CaseOutcome(
                testCase.getName(), passed, message, durationMs, logger.getLogFile());
        report.add(outcome);

        chatHandler.accept((passed ? "✔ " : "✘ ") + testCase.getName()
                + " " + (passed ? "passed" : "failed: " + message)
                + " (" + (durationMs / 1000) + "s)");

        TickScheduler.runLater(ctx.getWorld(), INTER_STEP_DELAY_MS, () ->
                runCasesAsync(pkg, caseIndex + 1, ctx.getWorld(), ctx.getStore(),
                        ctx.getPlayerRef(), ctx.getPlayer(), chatHandler, report));
    }

    private void finish(TestPackage pkg, TestReport report, Consumer<String> chatHandler) {
        currentStatus.set(null);
        abortRequested = false;
        NpcRegistrySelfCheck.setPaused(false);
        chatHandler.accept(report.allPassed()
                ? "✔ " + report.summaryLine()
                : "✘ " + report.summaryLine());
        chatHandler.accept(report.summaryTable());
        if (lastLogFile != null) {
            chatHandler.accept("Logs: " + lastLogFile);
        }
    }

    /** Convenience for {@link TestStep}s that need to record a fresh test NPC UUID. */
    public static void rememberSpawnedNpc(TestContext ctx, UUID uuid) {
        @SuppressWarnings("unchecked")
        java.util.List<UUID> list = (java.util.List<UUID>) ctx.get("spawnedNpcs");
        if (list == null) {
            list = new java.util.ArrayList<>();
            ctx.put("spawnedNpcs", list);
        }
        list.add(uuid);
    }
}
