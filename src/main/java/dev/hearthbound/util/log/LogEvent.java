package dev.hearthbound.util.log;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single log event. Carries enough metadata for both console output
 * and structured archive (NDJSON).
 *
 * Created cheaply on every log call (after level gating) — no allocations
 * in the suppression-strategy path.
 */
public final class LogEvent {

    public final long tsMs;
    public final LogLevel level;
    public final String category;
    public final String message;
    public final String thread;
    public final String callsite;        // ClassName:lineNumber, lazily computed
    public final Map<String, Object> ctx;  // structured fields (uuid, coords, role…)
    public final Throwable throwable;

    public LogEvent(long tsMs,
                    LogLevel level,
                    String category,
                    String message,
                    String thread,
                    String callsite,
                    Map<String, Object> ctx,
                    Throwable throwable) {
        this.tsMs = tsMs;
        this.level = level;
        this.category = category;
        this.message = message;
        this.thread = thread;
        this.callsite = callsite;
        this.ctx = ctx == null ? Map.of() : ctx;
        this.throwable = throwable;
    }

    /** Compact key used to coalesce in console channel: callsite identifies the source. */
    public String coalesceKey() {
        return callsite != null ? callsite : (category + "|" + message);
    }

    /** Build a flat copy for serialization (caller can mutate freely). */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ts", tsMs);
        m.put("lvl", level.name());
        m.put("cat", category);
        m.put("msg", message);
        if (thread != null) m.put("thread", thread);
        if (callsite != null) m.put("at", callsite);
        if (!ctx.isEmpty()) m.put("ctx", ctx);
        if (throwable != null) {
            m.put("err", throwable.getClass().getName() + ": " + throwable.getMessage());
        }
        return m;
    }
}
