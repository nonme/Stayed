package dev.hearthbound.util.log;

import java.util.logging.Level;

/**
 * Log severity levels. Ordered: TRACE < DEBUG < INFO < WARN < ERROR.
 * Mapped to java.util.logging Level for the underlying console handler.
 */
public enum LogLevel {
    TRACE(10, Level.FINER),
    DEBUG(20, Level.FINE),
    INFO(30, Level.INFO),
    WARN(40, Level.WARNING),
    ERROR(50, Level.SEVERE);

    private final int rank;
    private final Level julLevel;

    LogLevel(int rank, Level julLevel) {
        this.rank = rank;
        this.julLevel = julLevel;
    }

    public int rank() { return rank; }
    public Level julLevel() { return julLevel; }

    public boolean atLeast(LogLevel other) {
        return this.rank >= other.rank;
    }

    public static LogLevel parseOrDefault(String s, LogLevel fallback) {
        if (s == null) return fallback;
        try {
            return valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
