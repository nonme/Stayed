package dev.hearthbound.util.log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Console output with deduplication and "immediate-first" semantics.
 *
 * For every {@link LogEvent} we look at its coalesce key (callsite by default,
 * but suppression strategies can override). The first event for a key inside
 * the current {@link #WINDOW_MS} window is printed immediately. Subsequent
 * events with the same key inside the window are not printed but counted.
 * When the window expires, if N>0 dups were suppressed we print one summary
 * line.
 *
 * This guarantees every distinct callsite is visible at least once per window
 * (so signal isn't lost), while preventing log storms.
 *
 * Important: this only governs the CONSOLE channel. The archive channel
 * stores every event with full context regardless of aggregation.
 */
final class ConsoleAggregator {

    private static final long WINDOW_MS = 5_000L;

    /** Per-key state in the current window. */
    private static final class WindowState {
        long firstTsMs;        // window start
        int suppressed;        // count of suppressed events after the first one
        String lastMessage;    // last suppressed message — printed in summary
        String category;
        LogLevel maxLevel;     // escalate summary level if any suppressed event was higher
    }

    private final ConcurrentHashMap<String, WindowState> windows = new ConcurrentHashMap<>();
    private final Logger jul = Logger.getLogger("dev.hearthbound");

    /**
     * Decide whether to print the event now. May also flush an expired
     * window summary as a side-effect.
     */
    void emit(LogEvent ev) {
        // Always-print levels: errors and warnings bypass aggregation entirely.
        // Spammy locations should opt-in to coalescing via their callsite or
        // suppression strategy — we never silently swallow a WARN/ERROR.
        if (ev.level.atLeast(LogLevel.WARN)) {
            print(ev, 0);
            return;
        }

        String key = ev.coalesceKey();
        WindowState[] toFlush = new WindowState[1];

        windows.compute(key, (k, state) -> {
            long now = ev.tsMs;
            if (state == null || now - state.firstTsMs >= WINDOW_MS) {
                // Window expired — flush previous (if any) and open fresh.
                if (state != null && state.suppressed > 0) {
                    toFlush[0] = state;
                }
                WindowState fresh = new WindowState();
                fresh.firstTsMs = now;
                fresh.suppressed = 0;
                fresh.maxLevel = ev.level;
                fresh.category = ev.category;
                fresh.lastMessage = ev.message;
                // Print this event as the "first in window".
                print(ev, 0);
                return fresh;
            }
            // Inside window — suppress, count.
            state.suppressed++;
            state.lastMessage = ev.message;
            state.category = ev.category;
            if (ev.level.rank() > state.maxLevel.rank()) state.maxLevel = ev.level;
            return state;
        });

        if (toFlush[0] != null) {
            flushSummary(toFlush[0]);
        }
    }

    /** Periodic sweep — flush windows that expired without a follow-up event. */
    void tick(long nowMs) {
        windows.entrySet().removeIf(e -> {
            WindowState s = e.getValue();
            if (nowMs - s.firstTsMs >= WINDOW_MS) {
                if (s.suppressed > 0) flushSummary(s);
                return true;
            }
            return false;
        });
    }

    private void flushSummary(WindowState s) {
        // Reuse jul handler so format/colour matches normal lines.
        String line = "[" + s.category + "] " + s.lastMessage
                + " (×" + (s.suppressed + 1) + " in 5s)";
        jul.log(s.maxLevel.julLevel(), line);
    }

    private void print(LogEvent ev, int extra) {
        StringBuilder sb = new StringBuilder(ev.message.length() + 32);
        sb.append('[').append(ev.category).append("] ").append(ev.message);
        if (extra > 0) sb.append(" (×").append(extra + 1).append(" in 5s)");
        if (ev.throwable != null) {
            jul.log(ev.level.julLevel(), sb.toString(), ev.throwable);
        } else {
            jul.log(ev.level.julLevel(), sb.toString());
        }
    }
}
