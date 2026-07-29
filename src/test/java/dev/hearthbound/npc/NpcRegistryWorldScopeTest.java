package dev.hearthbound.npc;

import com.hypixel.hytale.math.util.ChunkUtil;

import java.util.List;
import java.util.UUID;

public final class NpcRegistryWorldScopeTest {

    // npcId must be a UUID string — NpcRecord builds its generated role name from it
    // via StayedRoleNames, which rejects anything UUID.fromString can't parse.
    private static final String REAL_NPC_ID   = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String FLAT_NPC_ID   = "aaaaaaaa-0000-0000-0000-000000000002";
    private static final String LEGACY_NPC_ID = "aaaaaaaa-0000-0000-0000-000000000003";

    public static void main(String[] args) {
        chunkCandidatesAreScopedToWorld();
        pendingRemovalsAreScopedToWorld();
        unscopedLegacyRecordsRemainVisibleForMigration();
    }

    private static void chunkCandidatesAreScopedToWorld() {
        UUID realWorld = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID flatWorld = UUID.fromString("22222222-2222-2222-2222-222222222222");
        long chunk = ChunkUtil.indexChunk(3, 4);

        NpcRegistry registry = NpcRegistry.get();
        registry.clear();
        try {
            NpcRegistry.NpcRecord realRecord = new NpcRegistry.NpcRecord(
                    REAL_NPC_ID, UUID.randomUUID(), "Elf_Sage_Wanderer",
                    NpcRegistry.InteractionType.ELF, 0L, chunk);
            realRecord.setWorld(realWorld, "real_world");
            registry.register(realRecord);

            NpcRegistry.NpcRecord flatRecord = new NpcRegistry.NpcRecord(
                    FLAT_NPC_ID, UUID.randomUUID(), "Elf_Sage_Wanderer",
                    NpcRegistry.InteractionType.ELF, 0L, chunk);
            flatRecord.setWorld(flatWorld, "flat_world");
            registry.register(flatRecord);

            List<NpcRegistry.NpcRecord> realCandidates = registry.getForChunk(realWorld, chunk);

            assertEquals(1, realCandidates.size(), "real world chunk candidate count");
            assertEquals(REAL_NPC_ID, realCandidates.get(0).npcId, "real world candidate npcId");
        } finally {
            registry.clear();
        }
    }

    private static void pendingRemovalsAreScopedToWorld() {
        UUID realWorld = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID flatWorld = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID entityUuid = UUID.randomUUID();

        NpcRegistry registry = NpcRegistry.get();
        registry.clear();
        try {
            registry.markForRemoval(realWorld, entityUuid, 123L);

            assertTrue(registry.isPendingRemoval(realWorld, entityUuid),
                    "entity should be pending in owner world");
            assertFalse(registry.isPendingRemoval(flatWorld, entityUuid),
                    "same uuid must not be pending in another world");
        } finally {
            registry.clear();
        }
    }

    private static void unscopedLegacyRecordsRemainVisibleForMigration() {
        UUID realWorld = UUID.fromString("11111111-1111-1111-1111-111111111111");
        long chunk = ChunkUtil.indexChunk(5, 6);

        NpcRegistry registry = NpcRegistry.get();
        registry.clear();
        try {
            NpcRegistry.NpcRecord legacy = new NpcRegistry.NpcRecord(
                    LEGACY_NPC_ID, UUID.randomUUID(), "Elf_Sage_Wanderer",
                    NpcRegistry.InteractionType.ELF, 0L, chunk);
            registry.register(legacy);

            List<NpcRegistry.NpcRecord> candidates = registry.getForChunk(realWorld, chunk);

            assertEquals(1, candidates.size(), "legacy record should be visible before migration");
            assertEquals(LEGACY_NPC_ID, candidates.get(0).npcId, "legacy candidate npcId");
        } finally {
            registry.clear();
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertTrue(boolean actual, String message) {
        if (!actual) throw new AssertionError(message);
    }

    private static void assertFalse(boolean actual, String message) {
        if (actual) throw new AssertionError(message);
    }
}
