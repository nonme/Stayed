package dev.hearthbound.util;

import com.hypixel.hytale.server.core.universe.world.World;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for scheduling periodic tasks that execute on the world thread.
 * Wraps ScheduledExecutorService + world.execute() for thread safety.
 */
public class TickScheduler {

    private static final Logger LOGGER = Logger.getLogger(TickScheduler.class.getName());
    private static final ScheduledExecutorService EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "hearthbound-scheduler");
                t.setDaemon(true);
                return t;
            });

    /**
     * Schedule a task to run once after a delay, on the world thread.
     */
    public static ScheduledFuture<?> runLater(World world, long delayMs, Runnable task) {
        return EXECUTOR.schedule(() -> {
            try {
                world.execute(task);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Scheduled task failed", e);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Schedule a repeating task on the world thread.
     * Returns a ScheduledFuture that can be cancelled.
     */
    public static ScheduledFuture<?> runRepeating(World world, long initialDelayMs, long periodMs, Runnable task) {
        ScheduledFuture<?>[] holder = new ScheduledFuture<?>[1];
        holder[0] = EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                world.execute(task);
            } catch (Exception e) {
                if (e instanceof IllegalThreadStateException
                        || (e.getCause() instanceof IllegalThreadStateException)) {
                    LOGGER.fine("World thread stopped, cancelling repeating task");
                    if (holder[0] != null) holder[0].cancel(false);
                    return;
                }
                LOGGER.log(Level.WARNING, "Repeating task failed", e);
            }
        }, initialDelayMs, periodMs, TimeUnit.MILLISECONDS);
        return holder[0];
    }

    /**
     * Get the underlying executor for BlockPlacer or other direct usage.
     */
    public static ScheduledExecutorService getExecutor() {
        return EXECUTOR;
    }

    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }
}
