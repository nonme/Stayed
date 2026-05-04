package dev.hearthbound.test.audit;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.util.TickScheduler;

import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

/**
 * Periodic background runner for {@link NpcRegistryInvariantAudit}. Logs every
 * violation as WARN so regressions surface in the server log even when no test
 * is active.
 *
 * Lifecycle: started from {@code HearthboundPlugin.setup()} once the world is
 * available; stopped on shutdown alongside the rest of the scheduler.
 *
 * The integration-test runner pauses self-check while a test is in flight —
 * the test runs explicit {@link AuditStep} calls and we don't want noise from
 * the periodic check racing with deliberate teardown.
 */
public final class NpcRegistrySelfCheck {

    private static final Logger LOGGER = Logger.getLogger(NpcRegistrySelfCheck.class.getName());
    private static final long INITIAL_DELAY_MS = 30_000L;
    private static final long PERIOD_MS = 30_000L;

    private static ScheduledFuture<?> task;
    private static volatile boolean paused = false;

    private NpcRegistrySelfCheck() {}

    public static synchronized void start(World world) {
        if (task != null) return;
        task = TickScheduler.runRepeating(world, INITIAL_DELAY_MS, PERIOD_MS, () -> {
            if (paused) return;
            try {
                Store<EntityStore> store = world.getEntityStore().getStore();
                AuditResult result = NpcRegistryInvariantAudit.run(world, store);
                if (!result.isClean()) {
                    LOGGER.warning("NpcRegistrySelfCheck: " + result.summary());
                    for (Violation v : result.getViolations()) {
                        LOGGER.warning("  " + v);
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("NpcRegistrySelfCheck failed: " + e.getMessage());
            }
        });
        LOGGER.info("NpcRegistrySelfCheck started (period " + PERIOD_MS + "ms)");
    }

    public static synchronized void stop() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
    }

    /** Test runner uses this to silence WARN noise during deliberate test setup. */
    public static void setPaused(boolean value) {
        paused = value;
    }
}
