package dev.hearthbound.util.log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * On-demand dump of the cold log channel.
 *
 * Bundles two things into a single zip:
 *  1. memory.ndjson — current in-memory ring buffer (most recent ~5000 events).
 *  2. session-*.ndjson — all rotated NDJSON files plus the active one.
 *
 * Output: mods/HearthboundData/logs/dump-<timestamp>.zip
 *
 * This is what the user attaches to a bug report.
 */
public final class LogDump {

    private static final Path OUT_DIR = Paths.get("mods", "HearthboundData", "logs");
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

    private LogDump() {}

    public static Path create() throws IOException {
        Files.createDirectories(OUT_DIR);
        Path zipPath = OUT_DIR.resolve("dump-" + STAMP.format(Instant.now()) + ".zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            writeRingBuffer(zip);
            writeSessionFiles(zip);
        }
        return zipPath;
    }

    private static void writeRingBuffer(ZipOutputStream zip) throws IOException {
        ArchiveWriter aw = Log.archive();
        if (aw == null) return;
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        List<LogEvent> events = aw.ringSnapshot();
        ZipEntry entry = new ZipEntry("memory.ndjson");
        zip.putNextEntry(entry);
        for (LogEvent ev : events) {
            byte[] line = (gson.toJson(ev.toMap()) + "\n").getBytes(StandardCharsets.UTF_8);
            zip.write(line);
        }
        zip.closeEntry();
    }

    private static void writeSessionFiles(ZipOutputStream zip) throws IOException {
        if (!Files.isDirectory(OUT_DIR)) return;
        List<Path> files = new ArrayList<>();
        try (Stream<Path> s = Files.list(OUT_DIR)) {
            s.filter(Files::isRegularFile)
              .filter(p -> {
                  String n = p.getFileName().toString();
                  return n.endsWith(".ndjson") && (n.startsWith("session"));
              })
              .forEach(files::add);
        }
        files.sort(Comparator.comparing(p -> p.getFileName().toString()));
        for (Path p : files) {
            ZipEntry entry = new ZipEntry(p.getFileName().toString());
            zip.putNextEntry(entry);
            try (InputStream in = new BufferedInputStream(Files.newInputStream(p))) {
                copy(in, zip);
            }
            zip.closeEntry();
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }
}
