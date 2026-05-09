package dev.hearthbound.test.steps;

import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.VillageData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable snapshot of NpcRegistry + VillageData state at one point in time.
 * Stored in TestContext under a name and compared later via DiffStep.
 *
 * Only the load-bearing fields are captured — anything that legitimately
 * mutates between scenarios (e.g. live entity refs) is omitted to keep diffs
 * meaningful. If a future test needs a field that's missing, add it here and
 * to {@link #describeChanges} together.
 */
public final class RegistrySnapshot {

    public static final class NpcEntry {
        public final String npcId;
        public final UUID entityUuid;
        public final String roleName;
        public final long chunkIndex;

        NpcEntry(String npcId, UUID entityUuid, String roleName, long chunkIndex) {
            this.npcId = npcId;
            this.entityUuid = entityUuid;
            this.roleName = roleName;
            this.chunkIndex = chunkIndex;
        }
    }

    public static final class BuildingEntry {
        public final String type;
        public final int x, y, z;
        public final UUID assignedVillager;
        public final boolean completed;

        BuildingEntry(String type, int x, int y, int z, UUID assignedVillager, boolean completed) {
            this.type = type;
            this.x = x; this.y = y; this.z = z;
            this.assignedVillager = assignedVillager;
            this.completed = completed;
        }

        String key() { return type + "@" + x + "," + y + "," + z; }
    }

    public final Map<String, NpcEntry> npcsByNpcId;
    public final Map<String, BuildingEntry> buildingsByKey;

    private RegistrySnapshot(Map<String, NpcEntry> npcs, Map<String, BuildingEntry> buildings) {
        this.npcsByNpcId = npcs;
        this.buildingsByKey = buildings;
    }

    public static RegistrySnapshot capture(VillageData village) {
        Map<String, NpcEntry> npcs = new HashMap<>();
        for (NpcRegistry.NpcRecord r : NpcRegistry.get().allRecords()) {
            npcs.put(r.npcId, new NpcEntry(r.npcId, r.entityUuid, r.baseRoleName(), r.chunkIndex));
        }
        Map<String, BuildingEntry> buildings = new HashMap<>();
        if (village != null) {
            for (BuildingRecord b : village.getBuildings()) {
                BuildingEntry e = new BuildingEntry(
                        b.getType(), b.getPosX(), b.getPosY(), b.getPosZ(),
                        b.getAssignedVillagerId(), b.isCompleted());
                buildings.put(e.key(), e);
            }
        }
        return new RegistrySnapshot(npcs, buildings);
    }
}
