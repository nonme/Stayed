package dev.hearthbound.npc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import dev.hearthbound.util.TickScheduler;

/**
 * Persists NPC registry data to disk so it survives server restarts.
 *
 * - File: mods/HearthboundData/data.json
 * - Written atomically via temp file to avoid partial writes on crash
 * - Loaded at plugin startup to populate NpcRegistry before any chunks load
 *
 * Format:
 * {
 *   "npcs": [
 *     { "uuid": "...", "role": "...", "interaction": "ELF|RESCUE|NONE",
 *       "skinSeed": 0, "chunkIndex": 12345 },
 *     ...
 *   ],
 *   "pendingRemovals": [ "uuid1", "uuid2", ... ]
 * }
 */
public final class HearthboundDataStore {

    private static final Logger LOGGER = Logger.getLogger(HearthboundDataStore.class.getName());
    private static final Path DATA_FILE = Paths.get("mods", "HearthboundData", "data.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static HearthboundDataStore instance;

    public static HearthboundDataStore get() {
        if (instance == null) instance = new HearthboundDataStore();
        return instance;
    }

    private HearthboundDataStore() {}

    // Dirty flag: set by callers that mutate state we want persisted (notably
    // NpcPositionTracker on significant moves). The flush task drains it every
    // PERIODIC_SAVE_INTERVAL_MS, so we don't write the file every tick.
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private ScheduledFuture<?> flushTask = null;
    private static final long PERIODIC_SAVE_INTERVAL_MS = 30_000L;

    /** Marks the in-memory registry as having unsaved changes. */
    public void markDirty() {
        dirty.set(true);
    }

    /**
     * Starts the periodic flush task. Idempotent — safe to call multiple times.
     * The task runs on the shared scheduler and only writes when dirty=true,
     * so it's effectively free when nothing has changed.
     */
    public void startPeriodicFlush() {
        if (flushTask != null) return;
        flushTask = TickScheduler.getExecutor().scheduleAtFixedRate(() -> {
            if (dirty.compareAndSet(true, false)) {
                save();
            }
        }, PERIODIC_SAVE_INTERVAL_MS, PERIODIC_SAVE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public void stopPeriodicFlush() {
        if (flushTask != null) {
            flushTask.cancel(false);
            flushTask = null;
        }
        // Final flush so we don't lose pending changes on shutdown.
        if (dirty.getAndSet(false)) save();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Loads all NPC records from disk and registers them in NpcRegistry.
     * Call once at plugin startup before any player joins.
     */
    public void loadAndPopulateRegistry() {
        PersistedData data = loadFromDisk();
        NpcRegistry registry = NpcRegistry.get();
        registry.clear();
        for (PersistedRecord r : data.npcs) {
            try {
                UUID uuid = UUID.fromString(r.uuid);
                NpcRegistry.InteractionType interaction =
                        NpcRegistry.InteractionType.valueOf(r.interaction);
                NpcRegistry.NpcRecord record = new NpcRegistry.NpcRecord(
                        uuid, r.role, interaction, r.skinSeed, r.chunkIndex);
                // Restore last-known position if present in the saved file. Boxed
                // Doubles are null when the field didn't exist in older saves —
                // hasPosition stays false in that case (backwards compatible).
                if (r.hasPosition != null && r.hasPosition
                        && r.lastX != null && r.lastY != null && r.lastZ != null) {
                    record.setPosition(r.lastX, r.lastY, r.lastZ);
                }
                if (r.broken != null && r.broken) record.broken = true;
                registry.register(record);
            } catch (Exception e) {
                LOGGER.warning("Skipping invalid persisted NPC record: " + r.uuid + " — " + e.getMessage());
            }
        }
        for (PersistedPendingRemoval pr : data.pendingRemovals) {
            try {
                registry.markForRemoval(UUID.fromString(pr.uuid), pr.chunkIndex);
            } catch (Exception e) {
                LOGGER.warning("Skipping invalid pending removal: " + pr.uuid);
            }
        }
        LOGGER.info("HearthboundDataStore: loaded " + data.npcs.size() + " NPC record(s), "
                + data.pendingRemovals.size() + " pending removal(s) from disk");
    }

    /**
     * Persists the current NpcRegistry state (records + pending removals) to disk.
     * Call after any spawn, respawn, or despawn that changes the registry.
     */
    public void save() {
        NpcRegistry registry = NpcRegistry.get();

        PersistedData data = new PersistedData();
        for (NpcRegistry.NpcRecord r : registry.allRecords()) {
            PersistedRecord pr = new PersistedRecord();
            pr.uuid        = r.entityUuid.toString();
            pr.role        = r.roleName;
            pr.interaction = r.interaction.name();
            pr.skinSeed    = r.skinSeed;
            pr.chunkIndex  = r.chunkIndex;
            if (r.hasPosition) {
                pr.hasPosition = true;
                pr.lastX = r.lastX;
                pr.lastY = r.lastY;
                pr.lastZ = r.lastZ;
            }
            if (r.broken) pr.broken = true;
            data.npcs.add(pr);
        }
        for (Map.Entry<UUID, Long> entry : registry.getPendingRemovals().entrySet()) {
            PersistedPendingRemoval ppr = new PersistedPendingRemoval();
            ppr.uuid       = entry.getKey().toString();
            ppr.chunkIndex = entry.getValue();
            data.pendingRemovals.add(ppr);
        }

        try {
            Files.createDirectories(DATA_FILE.getParent());
            Path tmp = DATA_FILE.getParent().resolve("data.json.tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(tmp)) {
                GSON.toJson(data, writer);
            }
            Files.move(tmp, DATA_FILE,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            LOGGER.fine("HearthboundDataStore: saved " + data.npcs.size() + " NPC(s), "
                    + data.pendingRemovals.size() + " pending removal(s)");
        } catch (IOException e) {
            LOGGER.warning("HearthboundDataStore: failed to save data: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private PersistedData loadFromDisk() {
        if (!Files.exists(DATA_FILE)) {
            LOGGER.info("HearthboundDataStore: no data file found, starting fresh");
            return new PersistedData();
        }
        try (FileReader reader = new FileReader(DATA_FILE.toFile())) {
            PersistedData data = GSON.fromJson(reader, PersistedData.class);
            if (data == null) return new PersistedData();
            if (data.npcs == null) data.npcs = new ArrayList<>();
            if (data.pendingRemovals == null) data.pendingRemovals = new ArrayList<>();
            return data;
        } catch (Exception e) {
            LOGGER.warning("HearthboundDataStore: failed to load data: " + e.getMessage());
            return new PersistedData();
        }
    }

    /** Top-level persisted structure. */
    private static final class PersistedData {
        List<PersistedRecord>         npcs            = new ArrayList<>();
        List<PersistedPendingRemoval> pendingRemovals = new ArrayList<>();
    }

    /**
     * Plain data object — Gson serialises/deserialises this directly.
     *
     * Boxed types (Double, Boolean) are used for the fields added after v1 of
     * this format so they default to null when reading old saves. The load
     * code treats null as "not present" rather than zero, so an old record
     * doesn't accidentally pin an NPC to (0, 0, 0).
     */
    private static final class PersistedRecord {
        String uuid;
        String role;
        String interaction;
        long   skinSeed;
        long   chunkIndex;
        Double lastX, lastY, lastZ;
        Boolean hasPosition;
        Boolean broken;
    }

    private static final class PersistedPendingRemoval {
        String uuid;
        long   chunkIndex;
    }
}
