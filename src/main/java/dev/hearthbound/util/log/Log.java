package dev.hearthbound.util.log;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Main entry point for logging.
 *
 * Two channels:
 *  - Console (java.util.logging) — gated by category level + console suppression.
 *  - Archive (NDJSON file + in-memory ring buffer) — receives every event past
 *    the level gate, regardless of console suppression.
 *
 * Usage:
 * <pre>
 *   private static final Log LOG = Log.get("npc.dup");
 *
 *   LOG.info("Spawned villager")                         // simple line
 *   LOG.with("uuid", uuid).with("role", role).info(msg)  // structured
 *   LOG.atMostEvery("setGate:" + pos, 5_000)
 *      .debug("door state changed")                      // throttled
 * </pre>
 *
 * Suppression strategies (firstN/atMostEvery/sample) only affect console;
 * archive always sees the full event.
 */
public final class Log {

    // ---- Singleton infrastructure ----

    private static final ConcurrentHashMap<String, Log> LOGGERS = new ConcurrentHashMap<>();
    private static final ConsoleAggregator CONSOLE = new ConsoleAggregator();
    private static volatile ArchiveWriter ARCHIVE;
    private static final ConcurrentHashMap<String, Long> TAIL_UNTIL = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> MUTE_UNTIL = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService SWEEPER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "hearthbound-log-sweeper");
                t.setDaemon(true);
                return t;
            });

    static {
        // Periodic flush of expired aggregation windows.
        SWEEPER.scheduleAtFixedRate(
                () -> CONSOLE.tick(System.currentTimeMillis()),
                1, 1, TimeUnit.SECONDS);
    }

    /**
     * Initialise the archive on plugin startup.
     * Safe to call multiple times — only first call creates the writer.
     */
    public static synchronized void install() {
        if (ARCHIVE == null) {
            ARCHIVE = new ArchiveWriter(5_000);
            LogConfig.get(); // force load
        }
    }

    /** Shutdown hook — flush archive cleanly. */
    public static synchronized void shutdown() {
        if (ARCHIVE != null) {
            ARCHIVE.shutdown();
            ARCHIVE = null;
        }
    }

    public static Log get(String category) {
        return LOGGERS.computeIfAbsent(category, Log::new);
    }

    /** Force-emit category to console for {@code durationMs} regardless of level config. */
    public static void tail(String category, long durationMs) {
        TAIL_UNTIL.put(category, System.currentTimeMillis() + durationMs);
    }

    /** Suppress category from console for {@code durationMs}. Archive still receives it. */
    public static void mute(String category, long durationMs) {
        MUTE_UNTIL.put(category, System.currentTimeMillis() + durationMs);
    }

    static ArchiveWriter archive() { return ARCHIVE; }

    // ---- Per-logger state ----

    private final String category;

    private Log(String category) {
        this.category = category;
    }

    public String category() { return category; }

    // ---- Direct logging (no context) ----

    public void trace(String msg) { dispatch(LogLevel.TRACE, msg, null, null, Suppression.NONE); }
    public void debug(String msg) { dispatch(LogLevel.DEBUG, msg, null, null, Suppression.NONE); }
    public void info(String msg)  { dispatch(LogLevel.INFO,  msg, null, null, Suppression.NONE); }
    public void warn(String msg)  { dispatch(LogLevel.WARN,  msg, null, null, Suppression.NONE); }
    public void error(String msg) { dispatch(LogLevel.ERROR, msg, null, null, Suppression.NONE); }
    public void error(String msg, Throwable t) { dispatch(LogLevel.ERROR, msg, t, null, Suppression.NONE); }
    public void warn(String msg,  Throwable t) { dispatch(LogLevel.WARN,  msg, t, null, Suppression.NONE); }
    public void info(String msg,  Throwable t) { dispatch(LogLevel.INFO,  msg, t, null, Suppression.NONE); }
    public void debug(String msg, Throwable t) { dispatch(LogLevel.DEBUG, msg, t, null, Suppression.NONE); }
    public void trace(String msg, Throwable t) { dispatch(LogLevel.TRACE, msg, t, null, Suppression.NONE); }

    /** Lazy variants — message supplier only invoked if level passes. */
    public void trace(Supplier<String> msg) { lazyDispatch(LogLevel.TRACE, msg, null, null, Suppression.NONE); }
    public void debug(Supplier<String> msg) { lazyDispatch(LogLevel.DEBUG, msg, null, null, Suppression.NONE); }
    public void info(Supplier<String> msg)  { lazyDispatch(LogLevel.INFO,  msg, null, null, Suppression.NONE); }

    // ---- Builder paths ----

    /** Start a builder with a structured context field. */
    public LogBuilder with(String key, Object value) {
        return new LogBuilder(this).with(key, value);
    }

    /** Console rate limiting: at most one console line per {@code intervalMs} per key. */
    public LogBuilder atMostEvery(String key, long intervalMs) {
        return new LogBuilder(this).atMostEvery(key, intervalMs);
    }

    /** Console: print first {@code n} occurrences of {@code key}, then silent until restart. */
    public LogBuilder firstN(String key, int n) {
        return new LogBuilder(this).firstN(key, n);
    }

    /** Console: random sampling at {@code rate} (0..1). */
    public LogBuilder sample(double rate) {
        return new LogBuilder(this).sample(rate);
    }

    // ---- Internal dispatch ----

    boolean isConsoleEnabled(LogLevel level) {
        // Tail override forces console regardless of level.
        Long tailUntil = TAIL_UNTIL.get(category);
        if (tailUntil != null && System.currentTimeMillis() < tailUntil) return true;
        return level.atLeast(LogConfig.get().effectiveLevel(category));
    }

    boolean isMuted() {
        Long muteUntil = MUTE_UNTIL.get(category);
        return muteUntil != null && System.currentTimeMillis() < muteUntil;
    }

    void dispatch(LogLevel level,
                  String msg,
                  Throwable t,
                  Map<String, Object> ctx,
                  Suppression suppression) {
        // Archive receives EVERYTHING (TRACE and up) regardless of category
        // level — bug reports need the full picture. Disk growth is bounded
        // by the 5x5MB rotation in ArchiveWriter, so this is safe.
        // Console honours per-category level + mute + suppression.
        boolean consoleEligible = isConsoleEnabled(level) && !isMuted();

        // Resolve callsite for both archive (so jq can filter by file:line)
        // and console (so the aggregator can coalesce).
        String callsite = CallsiteResolver.resolve();

        LogEvent ev = new LogEvent(
                System.currentTimeMillis(),
                level,
                category,
                msg,
                Thread.currentThread().getName(),
                callsite,
                ctx,
                t);

        ArchiveWriter aw = ARCHIVE;
        if (aw != null) aw.enqueue(ev);

        if (consoleEligible && suppression.passes(suppressionKey(callsite, ev))) {
            CONSOLE.emit(ev);
        }
    }

    void lazyDispatch(LogLevel level,
                      Supplier<String> msg,
                      Throwable t,
                      Map<String, Object> ctx,
                      Suppression suppression) {
        // Archive receives everything, so we must materialise the message
        // even if the console level gate would reject it. The lazy form
        // is still useful when callers want to skip building the string
        // for TRACE-level events that nobody is listening to — handled
        // by the explicit TRACE archive opt-out below.
        if (level == LogLevel.TRACE
                && !level.atLeast(LogConfig.get().effectiveLevel(category))) {
            return;
        }
        dispatch(level, msg.get(), t, ctx, suppression);
    }

    /**
     * Suppression key. If a builder set an explicit "_supKey" we use it,
     * otherwise the resolved callsite. Falls back to category|message
     * (worst case: no callsite + no explicit key).
     */
    private String suppressionKey(String callsite, LogEvent ev) {
        Object explicit = ev.ctx.get("_supKey");
        if (explicit instanceof String s) return s;
        if (callsite != null) return callsite;
        return ev.category + "|" + ev.message;
    }

    // ---- Builder class ----

    public static final class LogBuilder {
        private final Log owner;
        private Map<String, Object> ctx;
        private Suppression suppression = Suppression.NONE;

        LogBuilder(Log owner) { this.owner = owner; }

        public LogBuilder with(String key, Object value) {
            if (ctx == null) ctx = new LinkedHashMap<>();
            ctx.put(key, value);
            return this;
        }

        public LogBuilder atMostEvery(String key, long intervalMs) {
            this.suppression = Suppression.atMostEvery(intervalMs);
            return withSuppressionKey(key);
        }

        public LogBuilder firstN(String key, int n) {
            this.suppression = Suppression.firstN(n);
            return withSuppressionKey(key);
        }

        public LogBuilder sample(double rate) {
            this.suppression = Suppression.sample(rate);
            return this;
        }

        private LogBuilder withSuppressionKey(String key) {
            if (ctx == null) ctx = new LinkedHashMap<>();
            ctx.put("_supKey", key);
            return this;
        }

        public void trace(String msg) { owner.dispatch(LogLevel.TRACE, msg, null, ctx, suppression); }
        public void debug(String msg) { owner.dispatch(LogLevel.DEBUG, msg, null, ctx, suppression); }
        public void info(String msg)  { owner.dispatch(LogLevel.INFO,  msg, null, ctx, suppression); }
        public void warn(String msg)  { owner.dispatch(LogLevel.WARN,  msg, null, ctx, suppression); }
        public void error(String msg) { owner.dispatch(LogLevel.ERROR, msg, null, ctx, suppression); }
        public void warn(String msg, Throwable t)  { owner.dispatch(LogLevel.WARN,  msg, t, ctx, suppression); }
        public void error(String msg, Throwable t) { owner.dispatch(LogLevel.ERROR, msg, t, ctx, suppression); }
        public void info(String msg, Throwable t)  { owner.dispatch(LogLevel.INFO,  msg, t, ctx, suppression); }
        public void debug(String msg, Throwable t) { owner.dispatch(LogLevel.DEBUG, msg, t, ctx, suppression); }
        public void trace(String msg, Throwable t) { owner.dispatch(LogLevel.TRACE, msg, t, ctx, suppression); }

        public void debug(Supplier<String> msg) { owner.lazyDispatch(LogLevel.DEBUG, msg, null, ctx, suppression); }
        public void info(Supplier<String> msg)  { owner.lazyDispatch(LogLevel.INFO,  msg, null, ctx, suppression); }
    }
}
