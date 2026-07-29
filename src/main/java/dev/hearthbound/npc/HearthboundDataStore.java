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
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import dev.hearthbound.util.TickScheduler;

/**
 * Persists NPC registry data to disk so it survives server restarts.
 *
 * - File: mods/HearthboundData/data.json
 * - Atomic write via temp file + ATOMIC_MOVE (so a crash mid-write can't tear).
 * - Loaded once at startup before any chunk loads.
 *
 * Migration: records written by older versions did not contain the `npcId`
 * field. On load we synthesise one from the engine UUID as a string, so the
 * stable identity is identical to what the entity already carried.
 *
 * Deferred removals are persisted because reset/test cleanup can delete the
 * registry record while the physical entity is asleep in an unloaded chunk.
 * Without a durable tombstone, a server restart turns that entity into an
 * orphan carrying HB_NPCID with no matching record.
 */
public final class HearthboundDataStore {

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("data");
    private static final Path DATA_FILE = Paths.get("mods", "HearthboundData", "data.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static HearthboundDataStore instance;

    public static HearthboundDataStore get() {
        if (instance == null) instance = new HearthboundDataStore();
        return instance;
    }

    private HearthboundDataStore() {}

    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private ScheduledFuture<?> flushTask = null;
    private static final long PERIODIC_SAVE_INTERVAL_MS = 5_000L;

    public void markDirty() {
        dirty.set(true);
    }

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
        if (dirty.getAndSet(false)) save();
    }

    public void loadAndPopulateRegistry() {
        PersistedData data = loadFromDisk();
        NpcRegistry registry = NpcRegistry.get();
        registry.clear();

        int migrated = 0;
        for (PersistedRecord r : data.npcs) {
            try {
                UUID entityUuid = UUID.fromString(r.uuid);
                NpcRegistry.InteractionType interaction =
                        NpcRegistry.InteractionType.valueOf(r.interaction);

                String npcId = (r.npcId != null && !r.npcId.isBlank()) ? r.npcId : r.uuid;
                if (r.npcId == null || r.npcId.isBlank()) migrated++;

                String baseRoleName = (r.baseRoleName != null && !r.baseRoleName.isBlank())
                        ? r.baseRoleName
                        : StayedRoleNames.extractBaseRoleName(r.role);
                NpcRegistry.NpcRecord record = new NpcRegistry.NpcRecord(
                        npcId, entityUuid, baseRoleName, interaction, r.skinSeed, r.chunkIndex);
                if (r.worldUuid != null && !r.worldUuid.isBlank()) {
                    try {
                        record.setWorld(UUID.fromString(r.worldUuid), r.worldName);
                    } catch (Exception e) {
                        LOG.warn("Invalid world UUID on NPC record " + npcId + ": " + r.worldUuid);
                    }
                }

                if (r.hasPosition != null && r.hasPosition
                        && r.lastX != null && r.lastY != null && r.lastZ != null) {
                    record.setPosition(r.lastX, r.lastY, r.lastZ);
                }
                if (r.hasBasePosition != null && r.hasBasePosition
                        && r.baseX != null && r.baseY != null && r.baseZ != null) {
                    record.setBasePosition(r.baseX, r.baseY, r.baseZ);
                } else if (record.hasPosition) {
                    record.setBasePosition(record.lastX, record.lastY, record.lastZ);
                }
                if (r.testMarker != null && !r.testMarker.isBlank()) {
                    record.testMarker = r.testMarker;
                }

                registry.register(record);
            } catch (Exception e) {
                LOG.warn("Skipping invalid persisted NPC record: " + r.uuid + " — " + e.getMessage());
            }
        }

        int pendingLoaded = 0;
        if (data.pendingRemovals != null) {
            for (PersistedPendingRemoval r : data.pendingRemovals) {
                if (r == null || r.uuid == null || r.uuid.isBlank()) continue;
                try {
                    UUID worldUuid = null;
                    if (r.worldUuid != null && !r.worldUuid.isBlank()) {
                        worldUuid = UUID.fromString(r.worldUuid);
                    }
                    registry.markForRemoval(worldUuid, r.worldName, UUID.fromString(r.uuid), r.chunkIndex);
                    pendingLoaded++;
                } catch (Exception e) {
                    LOG.warn("Skipping invalid pending NPC removal: " + r.uuid + " — " + e.getMessage());
                }
            }
        }

        LOG.info("HearthboundDataStore: loaded " + data.npcs.size() + " NPC record(s) from disk"
                + (migrated > 0 ? " (" + migrated + " migrated to npcId)" : "")
                + (pendingLoaded > 0 ? ", pending removals=" + pendingLoaded : ""));
        if (migrated > 0) markDirty();
    }

    public synchronized void save() {
        NpcRegistry registry = NpcRegistry.get();

        PersistedData data = new PersistedData();
        for (NpcRegistry.NpcRecord r : registry.allRecords()) {
            PersistedRecord pr = new PersistedRecord();
            pr.npcId       = r.npcId;
            pr.uuid        = r.entityUuid != null ? r.entityUuid.toString() : "";
            pr.role        = r.roleName;
            pr.baseRoleName = r.baseRoleName();
            pr.worldUuid   = r.worldUuid != null ? r.worldUuid.toString() : null;
            pr.worldName   = r.worldName;
            pr.interaction = r.interaction.name();
            pr.skinSeed    = r.skinSeed;
            pr.chunkIndex  = r.chunkIndex;
            if (r.hasPosition) {
                pr.hasPosition = true;
                pr.lastX = r.lastX;
                pr.lastY = r.lastY;
                pr.lastZ = r.lastZ;
            }
            if (r.hasBasePosition) {
                pr.hasBasePosition = true;
                pr.baseX = r.baseX;
                pr.baseY = r.baseY;
                pr.baseZ = r.baseZ;
            }
            if (r.testMarker != null && !r.testMarker.isBlank()) {
                pr.testMarker = r.testMarker;
            }
            data.npcs.add(pr);
        }

        for (NpcRegistry.PendingRemoval e : registry.getPendingRemovalEntries()) {
            PersistedPendingRemoval pending = new PersistedPendingRemoval();
            pending.uuid = e.entityUuid.toString();
            pending.worldUuid = e.worldUuid != null ? e.worldUuid.toString() : null;
            pending.worldName = e.worldName;
            pending.chunkIndex = e.chunkIndex;
            data.pendingRemovals.add(pending);
        }

        try {
            Files.createDirectories(DATA_FILE.getParent());
            Path tmp = tempFileForSave(DATA_FILE);
            try (BufferedWriter writer = Files.newBufferedWriter(tmp)) {
                GSON.toJson(data, writer);
            }
            Files.move(tmp, DATA_FILE,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            LOG.debug("HearthboundDataStore: saved " + data.npcs.size() + " NPC(s)");
        } catch (IOException e) {
            LOG.warn("HearthboundDataStore: failed to save data", e);
        }
    }

    static Path tempFileForSave(Path dataFile) {
        Path parent = dataFile.getParent();
        String fileName = dataFile.getFileName().toString();
        return parent.resolve(fileName + "." + UUID.randomUUID() + ".tmp");
    }

    private PersistedData loadFromDisk() {
        if (!Files.exists(DATA_FILE)) {
            LOG.info("HearthboundDataStore: no data file found, starting fresh");
            return new PersistedData();
        }
        try (FileReader reader = new FileReader(DATA_FILE.toFile())) {
            PersistedData data = GSON.fromJson(reader, PersistedData.class);
            if (data == null) return new PersistedData();
            if (data.npcs == null) data.npcs = new ArrayList<>();
            if (data.pendingRemovals == null) data.pendingRemovals = new ArrayList<>();
            return data;
        } catch (Exception e) {
            LOG.warn("HearthboundDataStore: failed to load data: " + e.getMessage());
            return new PersistedData();
        }
    }

    /** Top-level persisted structure. */
    private static final class PersistedData {
        List<PersistedRecord> npcs = new ArrayList<>();
        List<PersistedPendingRemoval> pendingRemovals = new ArrayList<>();
    }

    /**
     * Plain data object — Gson serialises/deserialises this directly.
     *
     * `npcId` was added in the post-rewrite format. Old files have it as null;
     * the load path synthesises a value from `uuid` so the stable identity is
     * identical to the entity's engine UUID at the time of migration.
     *
     * `broken` (legacy circuit breaker) and similar fields are no longer kept.
     * Gson silently ignores any extra keys present in old files.
     */
    private static final class PersistedRecord {
        String npcId;
        String uuid;
        String role;
        String baseRoleName;
        String worldUuid;
        String worldName;
        String interaction;
        long   skinSeed;
        long   chunkIndex;
        Double lastX, lastY, lastZ;
        Boolean hasPosition;
        Double baseX, baseY, baseZ;
        Boolean hasBasePosition;
        /**
         * Set on records spawned by the integration test framework. Lets
         * {@code /hb test cleanup} find and delete leftover test NPCs after
         * a server restart that interrupted teardown.
         */
        String testMarker;
    }

    private static final class PersistedPendingRemoval {
        String uuid;
        String worldUuid;
        String worldName;
        long chunkIndex;
    }
}
