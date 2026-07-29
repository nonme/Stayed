package dev.hearthbound.util.log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-call suppression strategies that govern whether the CONSOLE channel
 * sees an event. They never affect the archive — full data still lands
 * in NDJSON regardless.
 *
 * Each strategy returns {@code true} from {@link #passes(String)} when the
 * event should reach the console.
 */
public abstract class Suppression {

    /** No suppression — always passes. */
    public static final Suppression NONE = new Suppression() {
        @Override public boolean passes(String key) { return true; }
    };

    public abstract boolean passes(String key);

    /**
     * Print the first {@code n} occurrences for each distinct key, then
     * stay silent until the process restarts. Good for one-shot startup
     * notices.
     */
    public static Suppression firstN(int n) {
        return new FirstN(n);
    }

    /**
     * Print at most once per {@code intervalMs} for each distinct key.
     * Rejected events are not counted in console — but archive still keeps
     * them.
     */
    public static Suppression atMostEvery(long intervalMs) {
        return new AtMostEvery(intervalMs);
    }

    /** Random sampling: each event passes with probability {@code rate}. */
    public static Suppression sample(double rate) {
        return new Sample(rate);
    }

    // -- Implementations --------------------------------------------------

    private static final class FirstN extends Suppression {
        private final int limit;
        private final ConcurrentHashMap<String, AtomicInteger> counts = new ConcurrentHashMap<>();
        FirstN(int limit) { this.limit = limit; }
        @Override public boolean passes(String key) {
            AtomicInteger c = counts.computeIfAbsent(key, k -> new AtomicInteger());
            return c.incrementAndGet() <= limit;
        }
    }

    private static final class AtMostEvery extends Suppression {
        private final long intervalMs;
        private final ConcurrentHashMap<String, AtomicLong> last = new ConcurrentHashMap<>();
        AtMostEvery(long intervalMs) { this.intervalMs = intervalMs; }
        @Override public boolean passes(String key) {
            long now = System.currentTimeMillis();
            AtomicLong slot = last.computeIfAbsent(key, k -> new AtomicLong(0L));
            long prev = slot.get();
            if (now - prev < intervalMs) return false;
            // CAS so concurrent callers don't both pass.
            return slot.compareAndSet(prev, now);
        }
    }

    private static final class Sample extends Suppression {
        private final double rate;
        Sample(double rate) {
            if (rate < 0.0 || rate > 1.0) throw new IllegalArgumentException("rate 0..1");
            this.rate = rate;
        }
        @Override public boolean passes(String key) {
            return ThreadLocalRandom.current().nextDouble() < rate;
        }
    }
}
