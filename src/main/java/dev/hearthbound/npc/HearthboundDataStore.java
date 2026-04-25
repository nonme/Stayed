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
import java.util.logging.Logger;

/**
 * Persists NPC registry data to disk so it survives server restarts.
 *
 * Persists NPC registry data across server restarts:
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
                registry.register(new NpcRegistry.NpcRecord(
                        uuid, r.role, interaction, r.skinSeed, r.chunkIndex));
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

    /** Plain data object — Gson serialises/deserialises this directly. */
    private static final class PersistedRecord {
        String uuid;
        String role;
        String interaction;
        long   skinSeed;
        long   chunkIndex;
    }

    private static final class PersistedPendingRemoval {
        String uuid;
        long   chunkIndex;
    }
}
