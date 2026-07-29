package dev.hearthbound.util.log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Per-category log levels. Categories are dot-separated; effective level
 * inherits from the longest registered prefix.
 *
 *   root  = INFO
 *   "npc.dup" = WARN     ← npc.dup.foo also WARN
 *   "npc"     = INFO     ← npc.schedule = INFO
 *
 * Persisted to mods/HearthboundData/logging.json. Reloadable at runtime
 * (via /hb log reload). Missing file = built from DEFAULTS.
 */
public final class LogConfig {

    private static final Path FILE = Paths.get("mods", "HearthboundData", "logging.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Built-in category quietening — applied when no config file exists.
     *
     * Strategy: WARN for spammers (per-tick or per-chunk-load); DEBUG for
     * verbose dev-only flows (gate state, ghost preview, scanning). Errors
     * still always reach console — Log.dispatch bypasses suppression for
     * WARN+ levels.
     */
    private static final Map<String, LogLevel> DEFAULTS = new LinkedHashMap<>();
    static {
        // Root catches everything not explicitly listed.
        DEFAULTS.put("root", LogLevel.INFO);

        // --- Top spammers (every tick / every NPC / every chunk load) ---
        DEFAULTS.put("npc.dup",            LogLevel.WARN);
        DEFAULTS.put("npc.role",           LogLevel.WARN);
        DEFAULTS.put("npc.role.apply",     LogLevel.WARN);
        DEFAULTS.put("npc.role.assetpack", LogLevel.WARN);
        DEFAULTS.put("npc.role.gen",       LogLevel.WARN);
        DEFAULTS.put("npc.schedule",       LogLevel.WARN);
        DEFAULTS.put("npc.schedule.gate",  LogLevel.WARN);
        DEFAULTS.put("npc.chunkload",      LogLevel.WARN);
        DEFAULTS.put("npc.recovery",       LogLevel.WARN);
        DEFAULTS.put("npc.tracker",        LogLevel.WARN);
        DEFAULTS.put("npc.teleport",       LogLevel.WARN);
        DEFAULTS.put("npc.registry",       LogLevel.WARN);
        DEFAULTS.put("npc.meal",           LogLevel.WARN);
        DEFAULTS.put("npc.farmer",         LogLevel.WARN);
        DEFAULTS.put("npc.builder",        LogLevel.INFO);
        DEFAULTS.put("npc.skin",           LogLevel.WARN);
        DEFAULTS.put("npc.elf",            LogLevel.INFO);
        DEFAULTS.put("npc.manager",        LogLevel.INFO);
        DEFAULTS.put("npc.hotbar",         LogLevel.WARN);
        DEFAULTS.put("npc.spawner",        LogLevel.INFO);

        DEFAULTS.put("interact",           LogLevel.WARN);

        // --- Build pipeline ---
        DEFAULTS.put("build",              LogLevel.INFO);
        DEFAULTS.put("build.ghost",        LogLevel.DEBUG);
        DEFAULTS.put("build.placer",       LogLevel.DEBUG);
        DEFAULTS.put("build.scan",         LogLevel.DEBUG);
        DEFAULTS.put("build.layout",       LogLevel.DEBUG);
        DEFAULTS.put("build.path",         LogLevel.DEBUG);
        DEFAULTS.put("build.terrain",      LogLevel.DEBUG);
        DEFAULTS.put("build.resource",     LogLevel.DEBUG);
        DEFAULTS.put("build.prefab",       LogLevel.DEBUG);
        DEFAULTS.put("build.craftability", LogLevel.INFO);

        // --- Events ---
        DEFAULTS.put("event.block",        LogLevel.INFO);
        DEFAULTS.put("event.player",       LogLevel.INFO);
        DEFAULTS.put("event.tick",         LogLevel.WARN);

        // --- UI pages: only show errors. ---
        DEFAULTS.put("ui",                 LogLevel.WARN);

        // --- Commands: dev-only, INFO is fine since they fire on demand. ---
        DEFAULTS.put("cmd",                LogLevel.INFO);

        // --- Misc ---
        DEFAULTS.put("plugin",             LogLevel.INFO);
        DEFAULTS.put("village",            LogLevel.INFO);
        DEFAULTS.put("quest",              LogLevel.INFO);
        DEFAULTS.put("data",               LogLevel.INFO);
        DEFAULTS.put("util.tick",          LogLevel.WARN);
        DEFAULTS.put("test",               LogLevel.INFO);
    }

    private final AtomicReference<ConcurrentHashMap<String, LogLevel>> levels =
            new AtomicReference<>(new ConcurrentHashMap<>());

    private static LogConfig instance;
    public static LogConfig get() {
        if (instance == null) instance = new LogConfig();
        return instance;
    }

    private LogConfig() {
        load();
    }

    /**
     * Resolve effective level for a category by walking up the dot-hierarchy.
     * "npc.dup.retry" → tries "npc.dup.retry", "npc.dup", "npc", then "root".
     */
    public LogLevel effectiveLevel(String category) {
        Map<String, LogLevel> map = levels.get();
        String key = category;
        while (true) {
            LogLevel lvl = map.get(key);
            if (lvl != null) return lvl;
            int dot = key.lastIndexOf('.');
            if (dot < 0) break;
            key = key.substring(0, dot);
        }
        LogLevel root = map.get("root");
        return root != null ? root : LogLevel.INFO;
    }

    /** Set a category override at runtime. Persists to disk. */
    public void set(String category, LogLevel level) {
        levels.get().put(category, level);
        save();
    }

    /** Read-only snapshot for /hb log listing. */
    public Map<String, LogLevel> snapshot() {
        return new LinkedHashMap<>(levels.get());
    }

    public void reload() {
        load();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        ConcurrentHashMap<String, LogLevel> next = new ConcurrentHashMap<>();
        DEFAULTS.forEach(next::put);
        if (Files.exists(FILE)) {
            try {
                String json = new String(Files.readAllBytes(FILE), StandardCharsets.UTF_8);
                Map<String, Object> raw = GSON.fromJson(json, Map.class);
                if (raw != null) {
                    raw.forEach((k, v) -> {
                        if (v instanceof String s) {
                            LogLevel lvl = LogLevel.parseOrDefault(s, null);
                            if (lvl != null) next.put(k, lvl);
                        }
                    });
                }
            } catch (IOException | RuntimeException e) {
                // Fall back to defaults; can't use Log here without recursion risk.
                System.err.println("[Hearthbound] LogConfig load failed: " + e.getMessage());
            }
        } else {
            // Write defaults so user can edit.
            saveSilent(next);
        }
        levels.set(next);
    }

    private void save() {
        saveSilent(levels.get());
    }

    private void saveSilent(Map<String, LogLevel> map) {
        try {
            Files.createDirectories(FILE.getParent());
            Map<String, String> serial = new LinkedHashMap<>();
            map.forEach((k, v) -> serial.put(k, v.name()));
            Files.write(FILE, GSON.toJson(serial).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("[Hearthbound] LogConfig save failed: " + e.getMessage());
        }
    }
}
