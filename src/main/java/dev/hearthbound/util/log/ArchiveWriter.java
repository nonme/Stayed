package dev.hearthbound.util.log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

/**
 * Cold log channel: NDJSON file + in-memory ring buffer.
 *
 * Every LogEvent that passes the level gate ends up here, regardless of
 * console suppression. This is the source of truth when investigating bug
 * reports — the user runs /hb log dump and we package the buffer + last
 * rotated files.
 *
 * Threading: writes are submitted to a single background thread to keep
 * disk I/O off whatever thread is logging (game/world threads, scheduler,
 * etc.).
 *
 * Rotation: 5 files × 5 MB. When the active file hits the limit, it is
 * renamed with a numeric suffix and a fresh file opened. Files older than
 * the oldest of the kept generation are deleted.
 */
final class ArchiveWriter {

    private static final Path DIR = Paths.get("mods", "HearthboundData", "logs");
    private static final long MAX_BYTES_PER_FILE = 5L * 1024L * 1024L;
    private static final int MAX_FILES = 5;
    private static final String ACTIVE_FILE = "session.ndjson";
    private static final DateTimeFormatter ROT_TS =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final RingBuffer ring;
    private final LinkedBlockingQueue<LogEvent> queue = new LinkedBlockingQueue<>(10_000);
    private final Thread worker;
    private volatile boolean running = true;

    private BufferedWriter writer;
    private long bytesWritten = 0L;

    ArchiveWriter(int ringCapacity) {
        this.ring = new RingBuffer(ringCapacity);
        try {
            Files.createDirectories(DIR);
            openActive();
        } catch (IOException e) {
            System.err.println("[Hearthbound] ArchiveWriter init failed: " + e.getMessage());
        }
        this.worker = new Thread(this::drain, "hearthbound-log-archive");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    void enqueue(LogEvent ev) {
        ring.add(ev);
        // Best-effort: if queue is full, drop. Console aggregation still has the line.
        queue.offer(ev);
    }

    /** Snapshot of in-memory ring buffer (most recent ~N entries). */
    List<LogEvent> ringSnapshot() {
        return ring.snapshot();
    }

    /** Path of the currently active session file. */
    Path activeFile() {
        return DIR.resolve(ACTIVE_FILE);
    }

    Path directory() {
        return DIR;
    }

    void shutdown() {
        running = false;
        worker.interrupt();
        try { worker.join(2_000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        flush();
        closeQuiet();
    }

    private void drain() {
        while (running || !queue.isEmpty()) {
            try {
                LogEvent ev = queue.poll(500, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (ev != null) writeOne(ev);
            } catch (InterruptedException e) {
                if (!running) break;
                Thread.currentThread().interrupt();
            }
        }
    }

    private void writeOne(LogEvent ev) {
        if (writer == null) return;
        try {
            String line = gson.toJson(ev.toMap());
            writer.write(line);
            writer.newLine();
            bytesWritten += line.length() + 1;
            if (bytesWritten >= MAX_BYTES_PER_FILE) {
                rotate();
            }
        } catch (IOException e) {
            System.err.println("[Hearthbound] ArchiveWriter write failed: " + e.getMessage());
            closeQuiet();
        }
    }

    private void flush() {
        if (writer != null) {
            try { writer.flush(); } catch (IOException ignored) {}
        }
    }

    private void openActive() throws IOException {
        Path p = DIR.resolve(ACTIVE_FILE);
        writer = Files.newBufferedWriter(
                p, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        bytesWritten = Files.exists(p) ? Files.size(p) : 0L;
    }

    private void rotate() throws IOException {
        closeQuiet();
        Path active = DIR.resolve(ACTIVE_FILE);
        String stamp = ROT_TS.format(Instant.now());
        Path rotated = DIR.resolve("session-" + stamp + ".ndjson");
        try {
            Files.move(active, rotated);
        } catch (IOException ignored) {
            // If move fails, keep appending — rotation is best-effort.
        }
        pruneOldFiles();
        openActive();
    }

    private void pruneOldFiles() {
        try (Stream<Path> s = Files.list(DIR)) {
            List<Path> rotated = new ArrayList<>();
            s.filter(Files::isRegularFile)
              .filter(p -> p.getFileName().toString().startsWith("session-")
                        && p.getFileName().toString().endsWith(".ndjson"))
              .forEach(rotated::add);
            rotated.sort(Comparator.comparingLong(p -> {
                try { return Files.getLastModifiedTime(p).toMillis(); }
                catch (IOException e) { return 0L; }
            }));
            // Keep at most (MAX_FILES - 1) rotated; active file is separate.
            int toDelete = rotated.size() - (MAX_FILES - 1);
            for (int i = 0; i < toDelete; i++) {
                try { Files.deleteIfExists(rotated.get(i)); } catch (IOException ignored) {}
            }
        } catch (IOException ignored) {}
    }

    private void closeQuiet() {
        if (writer != null) {
            try { writer.close(); } catch (IOException ignored) {}
            writer = null;
        }
    }
}
