package dev.hearthbound.test.steps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import dev.hearthbound.npc.HearthboundDataStore;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.test.engine.StepResult;
import dev.hearthbound.test.engine.TestContext;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

final class RestartSnapshotStore {

    static final Path SNAPSHOT_FILE = Paths.get("mods", "HearthboundData", "restart_test_snapshot.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private RestartSnapshotStore() {}

    static StepResult save(TestContext ctx) {
        try {
            Snapshot snapshot = new Snapshot();
            snapshot.createdAtMs = System.currentTimeMillis();
            TransformComponent transform = ctx.getStore().getComponent(
                    ctx.getPlayerRef(), TransformComponent.getComponentType());
            if (transform != null && transform.getPosition() != null) {
                snapshot.homeX = transform.getPosition().x;
                snapshot.homeY = transform.getPosition().y;
                snapshot.homeZ = transform.getPosition().z;
            }
            for (NpcRegistry.NpcRecord record : NpcRegistry.get().allRecords()) {
                Record out = new Record();
                out.npcId = record.npcId;
                out.entityUuid = record.entityUuid != null ? record.entityUuid.toString() : "";
                out.role = record.baseRoleName();
                out.interaction = record.interaction.name();
                out.chunkIndex = record.chunkIndex;
                out.hasPosition = record.hasPosition;
                out.lastX = record.lastX;
                out.lastY = record.lastY;
                out.lastZ = record.lastZ;
                out.testMarker = record.testMarker;
                snapshot.records.add(out);
            }
            HearthboundDataStore.get().save();
            Files.createDirectories(SNAPSHOT_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(SNAPSHOT_FILE)) {
                GSON.toJson(snapshot, writer);
            }
            ctx.getLogger().info("RestartSnapshot: saved " + snapshot.records.size()
                    + " record(s) to " + SNAPSHOT_FILE.toAbsolutePath());
            return StepResult.pass(snapshot.records.size() + " saved");
        } catch (Exception e) {
            return StepResult.fail("save restart snapshot failed: " + e.getMessage());
        }
    }

    static Snapshot load() throws Exception {
        try (Reader reader = Files.newBufferedReader(SNAPSHOT_FILE)) {
            return GSON.fromJson(reader, Snapshot.class);
        }
    }

    static void delete() throws Exception {
        Files.deleteIfExists(SNAPSHOT_FILE);
    }

    static final class Snapshot {
        long createdAtMs;
        double homeX, homeY, homeZ;
        List<Record> records = new ArrayList<>();
    }

    static final class Record {
        String npcId;
        String entityUuid;
        String role;
        String interaction;
        long chunkIndex;
        boolean hasPosition;
        double lastX, lastY, lastZ;
        String testMarker;
    }
}
