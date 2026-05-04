package dev.hearthbound.test.engine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Per-test file logger. Writes to {@code mods/HearthboundData/test_logs/<timestamp>_<test>.log}
 * and mirrors WARN/ERROR lines to the server logger so they show up in the
 * console too.
 *
 * Uses {@code synchronized} writes — many test steps write from the same
 * world thread but TickScheduler can also fire from its own pool, so be safe.
 */
public final class TestLogger implements AutoCloseable {

    private static final Logger SERVER_LOGGER = Logger.getLogger(TestLogger.class.getName());
    private static final Path LOG_DIR = Paths.get("mods", "HearthboundData", "test_logs");
    private static final DateTimeFormatter TS_FILE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter TS_LINE = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final Path logFile;
    private final BufferedWriter writer;
    private boolean closed = false;

    public static TestLogger open(String testName) {
        try {
            Files.createDirectories(LOG_DIR);
        } catch (IOException e) {
            SERVER_LOGGER.warning("Failed to create test log dir: " + e.getMessage());
        }
        String stamp = LocalDateTime.now().format(TS_FILE);
        Path file = LOG_DIR.resolve(stamp + "_" + sanitize(testName) + ".log");
        try {
            BufferedWriter w = Files.newBufferedWriter(file);
            return new TestLogger(file, w);
        } catch (IOException e) {
            SERVER_LOGGER.warning("Failed to open test log " + file + ": " + e.getMessage());
            return new TestLogger(file, null);
        }
    }

    private TestLogger(Path logFile, BufferedWriter writer) {
        this.logFile = logFile;
        this.writer = writer;
    }

    public Path getLogFile() { return logFile; }

    public synchronized void info(String line) {
        write("INFO", line);
    }

    public synchronized void warn(String line) {
        write("WARN", line);
        SERVER_LOGGER.warning("[test] " + line);
    }

    public synchronized void error(String line) {
        write("ERROR", line);
        SERVER_LOGGER.warning("[test] " + line);
    }

    private void write(String level, String line) {
        if (closed || writer == null) return;
        try {
            writer.write(LocalDateTime.now().format(TS_LINE));
            writer.write(' ');
            writer.write(level);
            writer.write(' ');
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            SERVER_LOGGER.warning("TestLogger write failed: " + e.getMessage());
        }
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        if (writer == null) return;
        try {
            writer.close();
        } catch (IOException e) {
            SERVER_LOGGER.warning("TestLogger close failed: " + e.getMessage());
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
