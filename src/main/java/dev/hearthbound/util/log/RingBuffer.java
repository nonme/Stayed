package dev.hearthbound.util.log;

import java.util.ArrayList;
import java.util.List;

/**
 * Lock-protected fixed-size circular buffer of LogEvents.
 * Older entries get overwritten by newer ones once capacity is hit.
 *
 * Optimised for: many writes (every log call), rare reads (only on /hb log dump).
 */
final class RingBuffer {

    private final LogEvent[] entries;
    private int head = 0;        // index of next slot to write
    private boolean wrapped = false;
    private final Object lock = new Object();

    RingBuffer(int capacity) {
        if (capacity < 16) capacity = 16;
        this.entries = new LogEvent[capacity];
    }

    void add(LogEvent e) {
        synchronized (lock) {
            entries[head] = e;
            head++;
            if (head >= entries.length) {
                head = 0;
                wrapped = true;
            }
        }
    }

    /** Snapshot in chronological order (oldest first). */
    List<LogEvent> snapshot() {
        synchronized (lock) {
            int size = wrapped ? entries.length : head;
            List<LogEvent> out = new ArrayList<>(size);
            if (!wrapped) {
                for (int i = 0; i < head; i++) out.add(entries[i]);
            } else {
                for (int i = 0; i < entries.length; i++) {
                    int idx = (head + i) % entries.length;
                    out.add(entries[idx]);
                }
            }
            return out;
        }
    }

    int capacity() {
        return entries.length;
    }
}
