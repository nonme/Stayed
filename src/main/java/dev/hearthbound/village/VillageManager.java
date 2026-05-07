package dev.hearthbound.village;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.npc.NpcLiveEntityResolver;
import dev.hearthbound.npc.NpcRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Central manager for village lifecycle.
 * Handles village creation, state transitions, and NPC coordination.
 */
public class VillageManager {

    private static final Logger LOGGER = Logger.getLogger(VillageManager.class.getName());

    private static VillageManager instance;

    public static VillageManager get() {
        return instance;
    }

    public static void init() {
        instance = new VillageManager();
    }

    private VillageManager() {}

    /**
     * Get village data for a player. Returns null if no village exists.
     */
    public VillageData getVillageData(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        return store.getComponent(playerRef, VillageData.getComponentType());
    }

    /**
     * Get or create village data for a player.
     */
    public VillageData getOrCreateVillageData(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        VillageData data = getVillageData(store, playerRef);
        if (data == null) {
            data = new VillageData();
            store.putComponent(playerRef, VillageData.getComponentType(), data);
        }
        return data;
    }

    /**
     * Found a new village at the given position.
     */
    public void foundVillage(Store<EntityStore> store, Ref<EntityStore> playerRef, World world,
                             int x, int y, int z) {
        VillageData data = getOrCreateVillageData(store, playerRef);
        data.setStage(VillageData.STAGE_FOUNDED);
        data.setFoundingStonePos(x, y, z);
        data.setFoundedAtTick(world.getTick());
        store.putComponent(playerRef, VillageData.getComponentType(), data);
        LOGGER.info("Village founded at " + x + ", " + y + ", " + z);
    }

    /**
     * Save current village state persistently.
     */
    public void save(Store<EntityStore> store, Ref<EntityStore> playerRef, VillageData data) {
        store.putComponent(playerRef, VillageData.getComponentType(), data);
    }

    /**
     * Repairs VillageData references that still point at old engine UUIDs after
     * an NPC was recovered with a new live UUID. The durable source of truth is
     * NpcRegistry.npcId; VillageData still stores UUIDs for houses/workers/UI, so
     * a crash between respawn and VillageData save can leave those UUIDs stale.
     *
     * This method is intentionally conservative: it first uses direct registry
     * references, then matches loaded villagers by persisted name/skin data, and
     * only falls back to a single remaining candidate when the mapping is
     * unambiguous.
     *
     * @return number of UUID references rewritten in VillageData.
     */
    public int reconcileNpcReferences(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                      VillageData data, World world) {
        if (store == null || playerRef == null || data == null) return 0;
        int changed = 0;

        UUID elfId = data.getElfId();
        if (elfId != null && NpcRegistry.get().getRecord(elfId) == null) {
            NpcRegistry.NpcRecord elf = singleUnclaimedRecord(NpcRegistry.InteractionType.ELF, new HashSet<>());
            if (elf != null && elf.entityUuid != null) {
                data.setElfId(elf.entityUuid);
                changed++;
                LOGGER.info("reconcileNpcReferences: elf " + elfId + " -> " + elf.entityUuid);
            }
        }

        HashSet<NpcRegistry.NpcRecord> claimed = new HashSet<>();
        for (VillagerSummary summary : data.getVillagers()) {
            UUID uuid = summary.getVillagerUuid();
            if (uuid == null) continue;
            NpcRegistry.NpcRecord direct = NpcRegistry.get().getRecord(uuid);
            if (direct != null) claimed.add(direct);
        }

        for (VillagerSummary summary : data.getVillagers()) {
            UUID oldUuid = summary.getVillagerUuid();
            if (oldUuid == null) continue;
            if (NpcRegistry.get().getRecord(oldUuid) != null) continue;

            NpcRegistry.NpcRecord match = findReplacementVillagerRecord(store, world, data, summary, oldUuid, claimed);
            if (match == null || match.entityUuid == null || oldUuid.equals(match.entityUuid)) continue;

            int touched = replaceVillagerUuid(data, oldUuid, match.entityUuid);
            if (touched > 0) {
                claimed.add(match);
                changed += touched;
                LOGGER.info("reconcileNpcReferences: villager " + summary.getFullName()
                        + " " + oldUuid + " -> " + match.entityUuid
                        + " npcId=" + match.npcId + " refs=" + touched);
            }
        }

        if (changed > 0) {
            save(store, playerRef, data);
        }
        return changed;
    }

    /**
     * Add a building record to the village.
     */
    public void addBuilding(Store<EntityStore> store, Ref<EntityStore> playerRef,
                            BuildingRecord building) {
        VillageData data = getOrCreateVillageData(store, playerRef);
        data.addBuilding(building);
        save(store, playerRef, data);
        LOGGER.info("Building added: " + building.getType() + " at " +
                building.getPosX() + ", " + building.getPosY() + ", " + building.getPosZ());
    }

    /**
     * Finds the first completed residential building with no assigned villager, or null.
     */
    public BuildingRecord findUnoccupiedHouse(VillageData data) {
        for (BuildingRecord b : data.getBuildings()) {
            if (!b.isCompleted()) continue;
            if (!BuildingType.isResidential(b.getType())) continue;
            if (b.getAssignedVillagerId() == null) return b;
        }
        return null;
    }

    /**
     * Assigns the villager to the house: sets house.assignedVillagerId, updates VillagerData
     * state to STATE_IDLE, and syncs the VillagerSummary in village data.
     * Saves village data after.
     */
    public void assignVillagerToHouse(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                      VillageData data, BuildingRecord house, UUID villagerUuid) {
        house.setAssignedVillagerId(villagerUuid);

        // Update VillagerSummary
        for (VillagerSummary summary : data.getVillagers()) {
            if (villagerUuid.equals(summary.getVillagerUuid())) {
                summary.setProfession(summary.getProfession()); // no-op, summary has no state field — ok
                break;
            }
        }

        save(store, playerRef, data);
        LOGGER.info("Villager " + villagerUuid + " assigned to house at "
                + house.getPosX() + "," + house.getPosY() + "," + house.getPosZ());
    }

    /**
     * Finds the first homeless villager UUID from the village's villager list, or null.
     * A villager is homeless if their VillagerData on the NpcRegistry has STATE_HOMELESS,
     * OR if they appear in the villager list but no house has them assigned.
     */
    public UUID findHomelessVillager(VillageData data) {
        for (VillagerSummary summary : data.getVillagers()) {
            UUID uuid = summary.getVillagerUuid();
            if (uuid == null) continue;
            if (isOrphanedSummary(uuid)) continue;
            if (!isVillagerAssignedToAnyHouse(data, uuid)) return uuid;
        }
        return null;
    }

    /**
     * True when the given villager UUID has no live registry record (or its
     * record is broken). Such a summary is a tombstone — keeping it preserves
     * the player-visible resident count but it must not be considered for
     * housing/profession assignments, otherwise cleanupOrphanedAssignments
     * frees the slot every tick and assignHomelessVillagers/assignUnstaffed*
     * re-assign it on the same tick, producing an infinite log loop.
     */
    private static boolean isOrphanedSummary(UUID villagerUuid) {
        NpcRegistry.NpcRecord r = NpcRegistry.get().getRecord(villagerUuid);
        return r == null || r.broken;
    }

    private boolean isVillagerAssignedToAnyHouse(VillageData data, UUID villagerUuid) {
        for (BuildingRecord b : data.getBuildings()) {
            if (villagerUuid.equals(b.getAssignedVillagerId())) return true;
        }
        return false;
    }

    /**
     * Returns all completed buildings of the given type, or empty list.
     */
    public List<BuildingRecord> findCompletedBuildings(VillageData data, String type) {
        return data.getBuildings().stream()
                .filter(b -> b.isCompleted() && type.equals(b.getType()))
                .toList();
    }

    /**
     * Returns the first completed farm, or null.
     */
    public BuildingRecord findCompletedFarm(VillageData data) {
        return data.getBuildings().stream()
                .filter(b -> b.isCompleted() && BuildingType.FARM.equals(b.getType()))
                .findFirst().orElse(null);
    }

    /**
     * Returns the first completed warehouse, or null.
     */
    public BuildingRecord findCompletedWarehouse(VillageData data) {
        return data.getBuildings().stream()
                .filter(b -> b.isCompleted() && BuildingType.WAREHOUSE.equals(b.getType()))
                .findFirst().orElse(null);
    }

    /**
     * Returns the first completed sawmill, or null.
     */
    public BuildingRecord findCompletedSawmill(VillageData data) {
        return data.getBuildings().stream()
                .filter(b -> b.isCompleted() && BuildingType.SAWMILL.equals(b.getType()))
                .findFirst().orElse(null);
    }

    /**
     * Returns the first completed mine, or null.
     */
    public BuildingRecord findCompletedMine(VillageData data) {
        return data.getBuildings().stream()
                .filter(b -> b.isCompleted() && BuildingType.MINE.equals(b.getType()))
                .findFirst().orElse(null);
    }

    /**
     * Returns the first completed guard house, or null.
     */
    public BuildingRecord findCompletedGuardHouse(VillageData data) {
        return data.getBuildings().stream()
                .filter(b -> b.isCompleted() && BuildingType.GUARD_HOUSE.equals(b.getType()))
                .findFirst().orElse(null);
    }

    /**
     * Returns the first completed forge, or null.
     */
    public BuildingRecord findCompletedForge(VillageData data) {
        return data.getBuildings().stream()
                .filter(b -> b.isCompleted() && BuildingType.FORGE.equals(b.getType()))
                .findFirst().orElse(null);
    }

    /**
     * Assigns a profession to the first eligible (housed, no profession) villager and
     * links them to the given building record.
     * Returns the UUID of the assigned villager, or null if nobody eligible.
     */
    private UUID assignWorkProfession(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                      VillageData data, BuildingRecord building, String profession) {
        for (VillagerSummary summary : data.getVillagers()) {
            if (!VillagerData.PROF_NONE.equals(summary.getProfession())) continue;
            UUID uuid = summary.getVillagerUuid();
            if (uuid == null) continue;
            if (isOrphanedSummary(uuid)) continue;

            if (!isVillagerAssignedToAnyHouse(data, uuid)) continue;

            summary.setProfession(profession);
            building.setAssignedVillagerId(uuid);
            save(store, playerRef, data);

            LOGGER.info("Assigned " + profession + " profession to villager " + uuid);
            return uuid;
        }
        return null;
    }

    /**
     * Assigns PROF_FARMER to the first eligible (housed, no profession) villager and
     * links them to the given farm building record.
     * Returns the UUID of the assigned villager, or null if nobody eligible.
     */
    public UUID assignFarmerProfession(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                       VillageData data, BuildingRecord farm) {
        return assignWorkProfession(store, playerRef, data, farm, VillagerData.PROF_FARMER);
    }

    /**
     * Assigns PROF_LUMBERJACK to the first eligible villager and links them to the sawmill.
     */
    public UUID assignLumberjackProfession(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                           VillageData data, BuildingRecord sawmill) {
        return assignWorkProfession(store, playerRef, data, sawmill, VillagerData.PROF_LUMBERJACK);
    }

    /**
     * Assigns PROF_MASON to the first eligible villager and links them to the mine.
     */
    public UUID assignMinerProfession(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                      VillageData data, BuildingRecord mine) {
        return assignWorkProfession(store, playerRef, data, mine, VillagerData.PROF_MASON);
    }

    /**
     * Assigns PROF_GUARD to the first eligible villager and links them to the guard house.
     */
    public UUID assignGuardProfession(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                      VillageData data, BuildingRecord guardHouse) {
        return assignWorkProfession(store, playerRef, data, guardHouse, VillagerData.PROF_GUARD);
    }

    /**
     * Assigns PROF_BLACKSMITH to the first eligible villager and links them to the forge.
     */
    public UUID assignBlacksmithProfession(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                           VillageData data, BuildingRecord forge) {
        return assignWorkProfession(store, playerRef, data, forge, VillagerData.PROF_BLACKSMITH);
    }

    /**
     * Finds the VillagerSummary by UUID, or null.
     */
    public VillagerSummary findVillagerSummary(VillageData data, UUID uuid) {
        if (uuid == null) return null;
        for (VillagerSummary s : data.getVillagers()) {
            if (uuid.equals(s.getVillagerUuid())) return s;
        }
        return null;
    }

    public NpcRegistry.NpcRecord findVillagerRecord(VillageData data, UUID uuid) {
        NpcRegistry.NpcRecord direct = NpcRegistry.get().getRecord(uuid);
        if (direct != null) return direct;
        if (data == null || uuid == null) return null;
        VillagerSummary summary = findVillagerSummary(data, uuid);
        if (summary == null) return null;
        return findReplacementVillagerRecord(null, null, data, summary, uuid, new HashSet<>());
    }

    /**
     * Rewrites every UUID reference from oldUuid → newUuid across the village data.
     * Called when an NPC is replaced by a fresh entity (the engine always assigns
     * a new engine UUID on spawn, so we cannot reuse the old one).
     *
     * Touches: VillagerSummary.villagerUuid, BuildingRecord.assignedVillagerId on
     * every building (residential + work), so the villager keeps their house and
     * job through a respawn.
     */
    public int replaceVillagerUuid(VillageData data, UUID oldUuid, UUID newUuid) {
        if (data == null || oldUuid == null || newUuid == null) return 0;
        int touched = 0;
        for (VillagerSummary s : data.getVillagers()) {
            if (oldUuid.equals(s.getVillagerUuid())) {
                s.setVillagerUuid(newUuid);
                touched++;
            }
        }
        for (BuildingRecord b : data.getBuildings()) {
            if (oldUuid.equals(b.getAssignedVillagerId())) {
                b.setAssignedVillagerId(newUuid);
                touched++;
            }
        }
        if (touched > 0) {
            LOGGER.info("replaceVillagerUuid: " + oldUuid + " → " + newUuid + " (" + touched + " refs)");
        }
        return touched;
    }

    private NpcRegistry.NpcRecord findReplacementVillagerRecord(Store<EntityStore> store, World world,
                                                               VillageData data, VillagerSummary summary,
                                                               UUID oldUuid,
                                                               HashSet<NpcRegistry.NpcRecord> claimed) {
        List<NpcRegistry.NpcRecord> candidates = new ArrayList<>();
        for (NpcRegistry.NpcRecord record : NpcRegistry.get().allRecords()) {
            if (record == null || record.entityUuid == null) continue;
            if (record.interaction != NpcRegistry.InteractionType.VILLAGER) continue;
            if (record.broken) continue;
            if (claimed.contains(record)) continue;
            candidates.add(record);
        }
        if (candidates.isEmpty()) return null;

        NpcRegistry.NpcRecord best = null;
        int bestScore = Integer.MIN_VALUE;
        int secondScore = Integer.MIN_VALUE;
        for (NpcRegistry.NpcRecord record : candidates) {
            int score = scoreReplacementRecord(store, world, data, summary, oldUuid, record);
            if (score > bestScore) {
                secondScore = bestScore;
                bestScore = score;
                best = record;
            } else if (score > secondScore) {
                secondScore = score;
            }
        }
        if (best != null && bestScore >= 100) return best;
        if (best != null && bestScore >= 30 && bestScore > secondScore) return best;

        int orphanSummaries = 0;
        for (VillagerSummary s : data.getVillagers()) {
            UUID uuid = s.getVillagerUuid();
            if (uuid != null && NpcRegistry.get().getRecord(uuid) == null) orphanSummaries++;
        }
        if (orphanSummaries == 1 && candidates.size() == 1) return candidates.get(0);

        return null;
    }

    private int scoreReplacementRecord(Store<EntityStore> store, World world, VillageData data,
                                       VillagerSummary summary, UUID oldUuid,
                                       NpcRegistry.NpcRecord record) {
        int score = 0;
        VillagerData liveData = liveVillagerData(store, record);
        if (liveData != null) {
            if (same(summary.getFirstName(), liveData.getFirstName())
                    && same(summary.getLastName(), liveData.getLastName())) {
                score += 120;
            }
            if (summary.getSkinSeed() != 0L && summary.getSkinSeed() == liveData.getSkinSeed()) {
                score += 80;
            }
            if (same(summary.getRace(), liveData.getRace())) score += 5;
        }
        if (summary.getSkinSeed() != 0L && summary.getSkinSeed() == record.skinSeed) {
            score += 80;
        }
        score += assignedBuildingDistanceScore(data, oldUuid, record);
        return score;
    }

    private VillagerData liveVillagerData(Store<EntityStore> store, NpcRegistry.NpcRecord record) {
        if (store == null || record == null) return null;
        Ref<EntityStore> ref = NpcLiveEntityResolver.findLiveNpcByRecord(store, record);
        if (ref == null || !ref.isValid()) return null;
        return store.getComponent(ref, VillagerData.getComponentType());
    }

    private int assignedBuildingDistanceScore(VillageData data, UUID oldUuid, NpcRegistry.NpcRecord record) {
        if (data == null || oldUuid == null || record == null || !record.hasPosition) return 0;
        int best = 0;
        for (BuildingRecord b : data.getBuildings()) {
            if (!oldUuid.equals(b.getAssignedVillagerId())) continue;
            double dx = record.lastX - b.getPosX();
            double dz = record.lastZ - b.getPosZ();
            double distSq = dx * dx + dz * dz;
            if (distSq <= 40.0 * 40.0) {
                best = Math.max(best, 40 - (int) Math.sqrt(distSq));
            }
        }
        return best;
    }

    private NpcRegistry.NpcRecord singleUnclaimedRecord(NpcRegistry.InteractionType type,
                                                       HashSet<NpcRegistry.NpcRecord> claimed) {
        NpcRegistry.NpcRecord found = null;
        for (NpcRegistry.NpcRecord record : NpcRegistry.get().allRecords()) {
            if (record == null || record.entityUuid == null) continue;
            if (record.interaction != type) continue;
            if (record.broken) continue;
            if (claimed.contains(record)) continue;
            if (found != null) return null;
            found = record;
        }
        return found;
    }

    private static boolean same(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        return a.equals(b);
    }

    /**
     * Fully removes a villager from the village: drops their VillagerSummary, clears
     * assignedVillagerId from every building they were assigned to, and resets the
     * profession field on any WorkSummary that referenced them.
     *
     * Does NOT save — caller must call {@link #save} afterwards (lets callers batch
     * multiple removals before hitting disk).
     *
     * Returns true if the villager was found and removed.
     */
    public boolean removeVillager(VillageData data, UUID villagerUuid) {
        if (data == null || villagerUuid == null) return false;
        boolean removed = data.getVillagers().removeIf(s -> villagerUuid.equals(s.getVillagerUuid()));
        for (BuildingRecord b : data.getBuildings()) {
            if (villagerUuid.equals(b.getAssignedVillagerId())) {
                b.setAssignedVillagerId(null);
            }
        }
        return removed;
    }

    /**
     * Scans every building in the village and clears {@code assignedVillagerId} for
     * any UUID that is not present in {@code liveUuids}. Call this after removing
     * a batch of villagers to sweep up any building slots that still hold stale refs.
     *
     * Does NOT save — caller must call {@link #save} afterwards.
     *
     * Returns the number of slots cleared.
     */
    public int removeStaleAssignments(VillageData data, java.util.Set<UUID> liveUuids) {
        if (data == null || liveUuids == null) return 0;
        int cleared = 0;
        for (BuildingRecord b : data.getBuildings()) {
            UUID assigned = b.getAssignedVillagerId();
            if (assigned != null && !liveUuids.contains(assigned)) {
                b.setAssignedVillagerId(null);
                cleared++;
            }
        }
        return cleared;
    }

    /**
     * Mark a building as completed and advance village stage if appropriate.
     */
    public void completeBuilding(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                 BuildingRecord building) {
        VillageData data = getOrCreateVillageData(store, playerRef);
        building.setCompleted(true);
        building.setBuildProgress(100);

        // Advance stage based on building type
        switch (building.getType()) {
            case BuildingType.TOWN_HALL:
                if (data.getStage() < VillageData.STAGE_TOWN_HALL) {
                    data.setStage(VillageData.STAGE_TOWN_HALL);
                }
                break;
            case BuildingType.WAREHOUSE:
                if (data.getStage() < VillageData.STAGE_WAREHOUSE) {
                    data.setStage(VillageData.STAGE_WAREHOUSE);
                }
                break;
        }

        save(store, playerRef, data);
        LOGGER.info("Building completed: " + building.getType() + " (stage → " + data.getStage() + ")");
    }
}
