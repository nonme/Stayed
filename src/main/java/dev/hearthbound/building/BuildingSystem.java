package dev.hearthbound.building;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.npc.ElfSage;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the building process: ghost preview, founding, resource-based construction.
 *
 * New flow (Stage 2):
 * 1. Player places Founding Stone → showGhostPreview (with rotation)
 * 2. F on stone → TownHallPage (pre-founding: name + Confirm)
 * 3. Confirm → clearGhostPreview, found village, spawn elf near door
 * 4. F on stone → TownHallPage (post-founding: village info + construction tab)
 * 5. "Start Construction" → startResourceBuilding (block-by-block consuming from container)
 * 6. Building complete → onBuildingComplete
 */
public class BuildingSystem {

    private static final Logger LOGGER = Logger.getLogger(BuildingSystem.class.getName());

    private static BuildingSystem instance;

    public static BuildingSystem get() { return instance; }
    public static void init() { instance = new BuildingSystem(); }

    private ResourceBlockPlacer activeBuilder;
    private BuildingRecord activeRecord;
    private List<BlockPlacer.BlockEntry> activeBuildPlan;
    private int activeRotation = 0;
    // Snapshot of blocks that existed before ghost preview was placed, keyed by "x,y,z"
    private Map<String, String> ghostSnapshot = new HashMap<>();
    // Players who are currently authoring a prefab — Founding Stone place/break events
    // skip village flow for them so they can drop anchor blocks inside prefab selections
    // without triggering ghost preview or village reset.
    private final java.util.Set<UUID> prefabAuthors = java.util.concurrent.ConcurrentHashMap.newKeySet();

    // Global "turbo" flag for debugging — makes ResourceBlockPlacer tick almost instantly
    // so we don't sit through the full animation on every rebuild/test cycle.
    private volatile boolean fastBuild = false;

    public boolean isFastBuild() { return fastBuild; }
    public void setFastBuild(boolean enabled) { this.fastBuild = enabled; }

    private BuildingSystem() {}

    public boolean isPrefabAuthoring(UUID playerUuid) {
        return playerUuid != null && prefabAuthors.contains(playerUuid);
    }

    /** Returns the new state after toggle. */
    public boolean togglePrefabAuthoring(UUID playerUuid) {
        if (playerUuid == null) return false;
        if (prefabAuthors.remove(playerUuid)) return false;
        prefabAuthors.add(playerUuid);
        return true;
    }

    // ========== Ghost Preview ==========

    /**
     * Show ghost preview using Filter_Air_Block (passthrough, visible as ghost).
     */
    public boolean showGhostPreview(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                     World world, String buildingType,
                                     int anchorX, int anchorY, int anchorZ, int rotation) {
        List<BlockPlacer.BlockEntry> plan = loadBuildPlan(buildingType, anchorX, anchorY, anchorZ, rotation);
        if (plan.isEmpty()) {
            LOGGER.warning("No building plan for type: " + buildingType);
            return false;
        }

        // Set active plan early so hasActivePreview() returns true during block placement,
        // preventing PlaceBlockEvent re-entrancy from triggering a second ghost preview
        // while we are still placing the first one.
        activeBuildPlan = plan;
        activeRotation = rotation;

        // Collect Empty cells that sit below the anchor level and need terrain clearing.
        List<BlockPlacer.BlockEntry> belowEmpty = loadBelowAnchorEmptyCells(buildingType,
                anchorX, anchorY, anchorZ, rotation);

        // Snapshot every cell we're about to overwrite so we can restore terrain when the ghost
        // is cleared (including after a server restart — the snapshot is persisted on VillageData).
        ghostSnapshot = new HashMap<>();
        int placed = 0;
        int yMin = Integer.MAX_VALUE, yMax = Integer.MIN_VALUE;

        // Snapshot + clear below-anchor Empty cells first.
        for (BlockPlacer.BlockEntry entry : belowEmpty) {
            var existing = world.getBlockType(entry.x(), entry.y(), entry.z());
            String existingId = (existing != null) ? existing.getId() : "Empty";
            // Only clear if something is actually there — no point touching truly empty air.
            if (existingId.equals("Empty") || existingId.equals("Editor_Empty")) continue;
            ghostSnapshot.put(entry.x() + "," + entry.y() + "," + entry.z(), existingId);
            BlockPlacer.silentRemoveBlock(world, entry.x(), entry.y(), entry.z());
            placed++;
            if (entry.y() < yMin) yMin = entry.y();
            if (entry.y() > yMax) yMax = entry.y();
        }

        for (BlockPlacer.BlockEntry entry : plan) {
            if (isDoorBlock(entry.blockType()) || isDecorBlock(entry.blockType())) continue;
            var existing = world.getBlockType(entry.x(), entry.y(), entry.z());
            String existingId = (existing != null) ? existing.getId() : "Empty";
            ghostSnapshot.put(entry.x() + "," + entry.y() + "," + entry.z(), existingId);
            if (isPlantBlock(entry.blockType())) {
                BlockPlacer.silentRemoveBlock(world, entry.x(), entry.y(), entry.z());
            } else {
                String ghostId = toGhostBlock(entry.blockType());
                BlockPlacer.placeBlock(world, new BlockPlacer.BlockEntry(
                        entry.x(), entry.y(), entry.z(), ghostId, entry.rotation()));
            }
            placed++;
            if (entry.y() < yMin) yMin = entry.y();
            if (entry.y() > yMax) yMax = entry.y();
        }
        // Second pass: connected-block update so fences/walls/roofs orient against neighbors.
        // Plant cells were cleared to Empty — skip them here.
        for (BlockPlacer.BlockEntry entry : plan) {
            if (isDoorBlock(entry.blockType()) || isDecorBlock(entry.blockType())) continue;
            if (isPlantBlock(entry.blockType())) continue;
            String ghostId = toGhostBlock(entry.blockType());
            BlockPlacer.updateConnectedBlock(world, entry.x(), entry.y(), entry.z(), ghostId, entry.rotation());
        }

        // Persist the snapshot so BlockBreakHandler can restore terrain after a restart.
        if (store != null && playerRef != null) {
            VillageData village = VillageManager.get().getOrCreateVillageData(store, playerRef);
            village.setPendingGhostSnapshot(ghostSnapshot);
            VillageManager.get().save(store, playerRef, village);
        }

        LOGGER.info("[Ghost] showGhostPreview: type=" + buildingType + " rotation=" + rotation
                + " plan=" + plan.size() + " placed=" + placed
                + " Y=[" + yMin + ".." + yMax + "]"
                + " anchor=(" + anchorX + "," + anchorY + "," + anchorZ + ")");
        return true;
    }

    /** Overload without rotation for backwards compatibility. */
    public boolean showGhostPreview(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                     World world, String buildingType,
                                     int anchorX, int anchorY, int anchorZ) {
        return showGhostPreview(store, playerRef, world, buildingType, anchorX, anchorY, anchorZ, 0);
    }

    /**
     * Removes the ghost preview and restores any terrain we temporarily overwrote.
     *
     * <p>Uses the in-memory snapshot first (fast path when nothing restarted). If that's empty
     * (e.g. singleton state was lost across a restart), falls back to {@link VillageData}'s
     * persisted snapshot.
     */
    public void clearGhostPreview(Store<EntityStore> store, Ref<EntityStore> playerRef, World world) {
        String caller = new Throwable().getStackTrace().length > 1
                ? new Throwable().getStackTrace()[1].toString() : "unknown";
        LOGGER.info("[Ghost] clearGhostPreview called from: " + caller
                + " | inMemorySnapshot=" + ghostSnapshot.size());

        Map<String, String> snapshot = ghostSnapshot;
        if (snapshot.isEmpty() && store != null && playerRef != null) {
            VillageData village = VillageManager.get().getVillageData(store, playerRef);
            if (village != null) {
                snapshot = village.getPendingGhostSnapshot();
                LOGGER.info("[Ghost] using persisted snapshot, size=" + snapshot.size());
            }
        }

        int restored = 0;
        int skipped = 0;
        // Count distinct block IDs found at skipped positions, and Y range of each
        java.util.Map<String, Integer> skippedByBlock = new java.util.TreeMap<>();
        java.util.Map<String, Integer> skippedByBlockMinY = new java.util.TreeMap<>();
        java.util.Map<String, Integer> skippedByBlockMaxY = new java.util.TreeMap<>();
        for (Map.Entry<String, String> e : snapshot.entrySet()) {
            int[] xyz = parseCoordKey(e.getKey());
            if (xyz == null) continue;
            int x = xyz[0], y = xyz[1], z = xyz[2];
            String original = e.getValue();
            var bt = world.getBlockType(x, y, z);
            String currentId = (bt != null) ? bt.getId() : "Empty";
            boolean isEmptyNow = currentId.equals("Empty") || currentId.equals("Editor_Empty");
            boolean hadRealBlock = original != null && !original.equals("Empty") && !original.equals("Editor_Empty");
            if (isGhostBlockId(currentId) || (isEmptyNow && hadRealBlock)) {
                if (original == null || original.equals("Empty") || original.equals("Editor_Empty")) {
                    BlockPlacer.silentRemoveBlock(world, x, y, z);
                } else {
                    world.setBlock(x, y, z, original);
                }
                restored++;
            } else {
                String foundId = (bt != null) ? bt.getId() : "null";
                skippedByBlock.merge(foundId, 1, Integer::sum);
                skippedByBlockMinY.merge(foundId, y, Math::min);
                skippedByBlockMaxY.merge(foundId, y, Math::max);
                skipped++;
            }
        }
        activeBuildPlan = null;
        ghostSnapshot.clear();
        if (store != null && playerRef != null) {
            VillageData village = VillageManager.get().getVillageData(store, playerRef);
            if (village != null) {
                village.setPendingGhostSnapshot(new java.util.HashMap<>());
                VillageManager.get().save(store, playerRef, village);
            }
        }
        LOGGER.info("[Ghost] clearGhostPreview done: restored=" + restored + " skipped=" + skipped);
        if (!skippedByBlock.isEmpty()) {
            StringBuilder sb = new StringBuilder("[Ghost] skipped blocks (found at those coords): ");
            for (Map.Entry<String, Integer> entry : skippedByBlock.entrySet()) {
                sb.append(entry.getKey()).append("×").append(entry.getValue())
                  .append(" Y=[").append(skippedByBlockMinY.get(entry.getKey()))
                  .append("..").append(skippedByBlockMaxY.get(entry.getKey())).append("] ");
            }
            LOGGER.info(sb.toString());
        }
    }

    private static int[] parseCoordKey(String key) {
        String[] parts = key.split(",");
        if (parts.length != 3) return null;
        try {
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
        } catch (NumberFormatException e) {
            return null;
        }
    }


    /**
     * Brute-force clear every ghost block ({@code Hearthbound_Ghost_*}) and passthrough
     * {@code Filter_Air_Block} in a cube around the given center. Use as a fallback when
     * the in-memory build plan is gone (server restart, singleton state lost) so the player
     * can re-place the Founding Stone without leftover phantom blocks.
     */
    public void clearOrphanedGhost(World world, int cx, int cy, int cz, int radius) {
        String caller = new Throwable().getStackTrace().length > 1
                ? new Throwable().getStackTrace()[1].toString() : "unknown";
        LOGGER.info("[Ghost] clearOrphanedGhost called from: " + caller
                + " | center=(" + cx + "," + cy + "," + cz + ") radius=" + radius);
        int cleared = 0;
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = cy - 2; y <= cy + radius + 2; y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    var bt = world.getBlockType(x, y, z);
                    if (bt == null) continue;
                    if (isGhostBlockId(bt.getId())) {
                        BlockPlacer.silentRemoveBlock(world, x, y, z);
                        cleared++;
                    }
                }
            }
        }
        LOGGER.info("[Ghost] clearOrphanedGhost done: cleared=" + cleared);
    }

    /**
     * Matches every id we use for ghost preview:
     * {@code Hearthbound_Ghost_*} direct ids and their state variants
     * (e.g. {@code *Hearthbound_Ghost_Fence_State_Definitions_Corner}), plus
     * {@code Filter_Air_Block} used as a passthrough placeholder for doors/decor.
     */
    private static boolean isGhostBlockId(String id) {
        if (id == null) return false;
        return id.contains("Hearthbound_Ghost_") || id.contains("Filter_Air");
    }

    /** True when the cell is truly empty — we should not overwrite it with a ghost otherwise. */
    private static boolean isEmptyCell(World world, int x, int y, int z) {
        var bt = world.getBlockType(x, y, z);
        if (bt == null) return true;
        String id = bt.getId();
        return id.equals("Empty") || id.equals("Editor_Empty") || id.equals("Filter_Air_Block")
                || isGhostBlockId(id);
    }

    public boolean hasActivePreview() {
        return activeBuildPlan != null;
    }

    public int getActiveRotation() {
        return activeRotation;
    }

    // ========== Founding ==========

    /**
     * Called when player clicks "Confirm" in TownHallPage pre-founding phase.
     * Clears ghost, founds village, spawns elf near door.
     */
    public void confirmFounding(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                 World world, String villageName,
                                 int anchorX, int anchorY, int anchorZ, int rotation) {
        // Restore terrain behind the ghost — construction will overwrite it again, but this
        // keeps the flow symmetric with "break stone during preview": the world is always
        // returned to its pre-ghost state before anything else happens.
        clearGhostPreview(store, playerRef, world);

        // Found village
        VillageManager.get().foundVillage(store, playerRef, world, anchorX, anchorY, anchorZ);
        VillageData village = VillageManager.get().getOrCreateVillageData(store, playerRef);
        village.setVillageName(villageName);
        village.setRotation(rotation);

        // Add Town Hall building record (not yet built)
        BuildingRecord townHall = new BuildingRecord(BuildingType.TOWN_HALL, anchorX, anchorY, anchorZ);
        townHall.setRotation(rotation);
        village.addBuilding(townHall);
        VillageManager.get().save(store, playerRef, village);

        // Spawn elf near where the door will be
        spawnVillageElf(store, playerRef, world, anchorX, anchorY, anchorZ, rotation);

        LOGGER.info("Village \"" + villageName + "\" founded at " +
                anchorX + "," + anchorY + "," + anchorZ + " rotation=" + rotation);
    }

    /**
     * Reverts a village back to the pre-founded state when the player breaks the Founding Stone
     * before the Town Hall is completed. Cancels in-flight construction, despawns the village elf,
     * and drops the Town Hall record along with any deposited resources.
     *
     * <p>Returns {@code true} if a reset actually happened (i.e. the village was founded and the
     * Town Hall was not yet built), so callers can decide whether to re-trigger the ghost preview.
     */
    public boolean resetFoundingIfPreTownHall(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                              World world) {
        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village == null || !village.isFounded()) return false;

        BuildingRecord townHall = village.findBuilding(BuildingType.TOWN_HALL);
        if (townHall != null && townHall.isCompleted()) return false;

        if (activeBuilder != null && !activeBuilder.isFinished()) {
            activeBuilder.cancel();
            activeBuilder = null;
            activeRecord = null;
        }

        UUID elfId = village.getElfId();
        if (elfId != null) {
            Entity elf = world.getEntity(elfId);
            if (elf != null) elf.remove();
        }

        village.getBuildings().removeIf(b -> BuildingType.TOWN_HALL.equals(b.getType()));
        village.setStage(VillageData.STAGE_NONE);
        village.setConstructionStarted(false);
        village.setElfId(null);
        VillageManager.get().save(store, playerRef, village);

        LOGGER.info("Village reset to pre-founded state");
        return true;
    }

    /**
     * Despawn wanderer elf, spawn village elf near the building door.
     */
    private void spawnVillageElf(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                  World world, int anchorX, int anchorY, int anchorZ, int rotation) {
        world.execute(() -> {
            Store<EntityStore> worldStore = world.getEntityStore().getStore();
            VillageData village = VillageManager.get().getVillageData(worldStore, playerRef);
            if (village == null) {
                LOGGER.warning("spawnVillageElf: VillageData is null");
                return;
            }

            // Spawn elf near door via respawnAs so NpcRegistry is updated correctly.
            int[] doorOffset = BuildingType.getDoorOffset(BuildingType.TOWN_HALL, rotation);
            Vector3d elfPos = new Vector3d(
                    anchorX + doorOffset[0],
                    anchorY + 1,
                    anchorZ + doorOffset[1]
            );
            float elfYaw = switch (rotation) {
                case 0 -> 180f;
                case 1 -> 270f;
                case 2 -> 0f;
                case 3 -> 90f;
                default -> 0f;
            };
            ElfSage.respawnAs(worldStore, playerRef, world,
                    ElfSage.ROLE_WANDERER, elfPos, new Vector3f(0, elfYaw, 0));
            LOGGER.info("Village elf respawned near door: " + elfPos);
        });
    }

    // ========== Resource-based Construction ==========

    /**
     * Start building block-by-block, consuming resources from the Founding Stone container.
     * Called from TownHallPage when player clicks "Start Construction".
     */
    public void startResourceBuilding(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                       World world, BuildingRecord record, int rotation,
                                       UUID ownerUuid) {
        if (activeBuilder != null && !activeBuilder.isFinished()) {
            LOGGER.warning("Another building is already in progress");
            return;
        }

        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village == null) return;

        List<BlockPlacer.BlockEntry> plan = loadBuildPlan(record.getType(),
                record.getPosX(), record.getPosY(), record.getPosZ(), rotation);

        if (plan.isEmpty()) {
            LOGGER.warning("No building plan for type: " + record.getType());
            return;
        }

        activeRecord = record;
        record.setRotation(rotation);
        village.setConstructionStarted(true);
        VillageManager.get().save(store, playerRef, village);

        // Safe position: a couple blocks in front of the building door.
        int[] doorOffset = BuildingType.getDoorOffset(record.getType(), rotation);
        double dirX = doorOffset[0] == 0 ? 0 : (doorOffset[0] > 0 ? 2 : -2);
        double dirZ = doorOffset[1] == 0 ? 0 : (doorOffset[1] > 0 ? 2 : -2);
        double safeX = record.getPosX() + doorOffset[0] + dirX + 0.5;
        double safeY = record.getPosY() + 1;
        double safeZ = record.getPosZ() + doorOffset[1] + dirZ + 0.5;

        // Swap wanderer-role elf to builder-role elf at the safe position. The builder role
        // has BodyMotion: Nothing and no HeadMotion so our lookAtBlock packets steer him.
        float elfYaw = switch (rotation) {
            case 0 -> 180f;
            case 1 -> 270f;
            case 2 -> 0f;
            case 3 -> 90f;
            default -> 0f;
        };
        ElfSage.respawnAs(store, playerRef, world, ElfSage.ROLE_BUILDER,
                new Vector3d(safeX, safeY, safeZ), new Vector3f(0, elfYaw, 0));

        // Reload the village data so we pick up the new elf UUID that respawnAs just wrote.
        village = VillageManager.get().getVillageData(store, playerRef);
        UUID elfUuid = village != null ? village.getElfId() : null;

        activeBuilder = new ResourceBlockPlacer(world, plan, record,
                safeX, safeY, safeZ, elfUuid, ownerUuid, () -> {
            world.execute(() -> {
                Store<EntityStore> worldStore = world.getEntityStore().getStore();
                onBuildingComplete(worldStore, playerRef, world, record);
            });
        });
        activeBuilder.start();

        LOGGER.info("Resource building started: " + record.getType() + " (" + plan.size() + " blocks)");
    }

    private void onBuildingComplete(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                     World world, BuildingRecord record) {
        // Swap in the real prefab now that the shell is done. The block-by-block build
        // pass can't fix up connected-blocks (fences, stairs, wall corners) correctly
        // because each block looks at neighbors that don't exist yet at placement time.
        // PrefabStore.placeNoReturn places the whole selection atomically, so the
        // engine's connected-block resolver has the full neighborhood available.
        replaceWithPrefab(world, store, record, record.getRotation());

        // After building a residential house the elf returns to the Town Hall (wanderer role).
        // After building the Town Hall itself he also uses wanderer so he stands near the entrance.
        // ROLE_VILLAGER was used while wander-radius pointed at the new house — no longer needed.
        if (activeBuilder != null) {
            Vector3d returnPos = elfReturnPos(store, playerRef, record,
                    activeBuilder.getSafeX(), activeBuilder.getSafeY(), activeBuilder.getSafeZ());
            ElfSage.respawnAs(store, playerRef, world, ElfSage.ROLE_WANDERER,
                    returnPos, new Vector3f(0, 0, 0));
        }

        VillageManager mgr = VillageManager.get();
        mgr.completeBuilding(store, playerRef, record);

        // When a farm is built, assign farmer profession to the first eligible villager
        if (BuildingType.FARM.equals(record.getType())) {
            dev.hearthbound.village.VillageData village = mgr.getVillageData(store, playerRef);
            if (village != null) {
                java.util.UUID farmerUuid = mgr.assignFarmerProfession(store, playerRef, village, record);
                if (farmerUuid != null) {
                    LOGGER.info("Farm built — assigned farmer: " + farmerUuid);
                } else {
                    LOGGER.info("Farm built — no eligible villager for farmer yet");
                }
            }
        }

        activeBuilder = null;
        activeRecord = null;
        LOGGER.info("Building complete: " + record.getType());
    }

    /**
     * Where the elf should stand after finishing a build.
     * For any building type: the Town Hall door (founding stone + door offset).
     * Falls back to the builder's safe spot if village data is missing.
     */
    private Vector3d elfReturnPos(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                   BuildingRecord record, double fallbackX, double fallbackY, double fallbackZ) {
        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village == null) return new Vector3d(fallbackX, fallbackY, fallbackZ);

        int sx = village.getFoundingStoneX();
        int sy = village.getFoundingStoneY();
        int sz = village.getFoundingStoneZ();
        int rot = village.getRotation();
        int[] doorOffset = BuildingType.getDoorOffset(BuildingType.TOWN_HALL, rot);
        return new Vector3d(sx + doorOffset[0] + 0.5, sy + 1, sz + doorOffset[1] + 0.5);
    }

    /**
     * Finalizes a freshly-built shell by dropping a real prefab instance on top of it.
     *
     * <p>The shell builder places blocks one by one, which means every stairs/fence/wall
     * computes its connected-block shape against whatever neighbors exist at that instant,
     * often producing wrong corners. Re-placing the prefab at the end lets the engine
     * resolve every connection with the full neighborhood visible.
     */
    private void replaceWithPrefab(World world, Store<EntityStore> store,
                                   BuildingRecord record, int rotation) {
        String prefabName = BuildingType.getPrefabName(record.getType());
        if (prefabName == null) return;

        try {
            com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection selection =
                    com.hypixel.hytale.server.core.prefab.PrefabStore.get()
                            .getAssetPrefabFromAnyPack(prefabName + ".prefab.json");

            // Our PrefabLoader and the engine's BlockSelection.rotate(Axis.Y) now use the
            // same handedness (CCW), so we rotate by exactly steps * 90°.
            String anchorBlockId = BuildingType.getAnchorBlockId(record.getType());
            int anchorPrefabY = BuildingType.getAnchorPrefabY(record.getType());
            int prefabRotation = readPrefabAnchorYaw(selection, anchorBlockId, anchorPrefabY);
            int steps = (rotation - prefabRotation + 4) % 4;
            int angleDeg = (steps * 90) % 360;

            com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection rotated =
                    angleDeg == 0 ? selection : selection.rotate(com.hypixel.hytale.math.Axis.Y, angleDeg);

            // Pin the selection anchor to the anchor block inside the prefab so placeNoReturn
            // puts that block exactly at the world anchor position (the founding stone).
            int[] anchorLocal = findPrefabAnchor(rotated, anchorBlockId, anchorPrefabY);
            if (anchorLocal == null) {
                LOGGER.warning("replaceWithPrefab: anchor block not found in rotated prefab, skipping final swap");
                return;
            }
            rotated.setAnchorAtWorldPos(anchorLocal[0], anchorLocal[1], anchorLocal[2]);

            com.hypixel.hytale.math.vector.Vector3i stonePos =
                    new com.hypixel.hytale.math.vector.Vector3i(
                            record.getPosX(), record.getPosY(), record.getPosZ());
            rotated.placeNoReturn(world, stonePos, store);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "replaceWithPrefab failed", e);
        }
    }

    private static int readPrefabAnchorYaw(
            com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection selection,
            String anchorBlockId, int anchorPrefabY) {
        var assetMap = com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType.getAssetMap();
        int[] found = {0};
        selection.forEachBlock((bx, by, bz, holder) -> {
            if (found[0] != 0) return;
            if (holder.filler() != 0) return;
            var bt = assetMap.getAsset(holder.blockId());
            if (bt == null) return;
            if (bt.getId().equals(anchorBlockId) && by - selection.getY() == anchorPrefabY) {
                found[0] = holder.rotation() % 4;
            }
        });
        return found[0];
    }

    /** Finds the anchor block's prefab-local coords so we can pin placeNoReturn on it. */
    private static int[] findPrefabAnchor(
            com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection selection,
            String anchorBlockId, int anchorPrefabY) {
        var assetMap = com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType.getAssetMap();
        int[] found = {Integer.MIN_VALUE, 0, 0};
        selection.forEachBlock((bx, by, bz, holder) -> {
            if (found[0] != Integer.MIN_VALUE) return;
            if (holder.filler() != 0) return;
            var bt = assetMap.getAsset(holder.blockId());
            if (bt == null) return;
            // Match by id plus Y offset — multiple statues at different heights would otherwise
            // clash (e.g. statue base vs upper filler).
            if (bt.getId().equals(anchorBlockId) && by - selection.getY() == anchorPrefabY) {
                found[0] = bx;
                found[1] = by;
                found[2] = bz;
            }
        });
        return found[0] == Integer.MIN_VALUE ? null : found;
    }

    private static boolean isDoorBlock(String blockType) {
        return blockType.contains("Door") || blockType.contains("Trapdoor");
    }

    /** Returns true for decorative/furniture blocks that should be skipped in ghost preview. */
    private static boolean isDecorBlock(String blockType) {
        String base = blockType.startsWith("*") ? blockType.substring(1) : blockType;
        if (base.equals("Furniture_Village_Planter")) return false;
        return base.startsWith("Deco_") || base.startsWith("Furniture_");
    }

    private static boolean isPlantBlock(String blockType) {
        String base = blockType.startsWith("*") ? blockType.substring(1) : blockType;
        return base.startsWith("Plant_");
    }

    /** Maps a real block ID to its no-collision ghost equivalent based on shape. */
    private static String toGhostBlock(String blockType) {
        String base = blockType.startsWith("*") ? blockType.substring(1) : blockType;

        int stateIdx = base.indexOf("_State_Definitions_");
        String shape = stateIdx != -1 ? base.substring(0, stateIdx) : base;
        String stateSuffix = stateIdx != -1 ? base.substring(stateIdx) : "";

        String lower = shape.toLowerCase();

        // For wall/fence blocks, directly map state variants to their ghost counterparts
        // instead of letting the connected-block system auto-detect corners — the prefab's
        // diagonal wall layouts don't always satisfy the Corner adjacency pattern.
        if ((lower.contains("_wall") && !lower.contains("wood_village_wall"))
                || lower.contains("_fence")) {
            if (!stateSuffix.isEmpty()) {
                // Preserve the state variant: Corner → *Hearthbound_Ghost_Fence_State_Definitions_Corner
                return "*Hearthbound_Ghost_Fence" + stateSuffix;
            }
            return "Hearthbound_Ghost_Fence";
        }

        if (lower.contains("_stairs"))             return "Hearthbound_Ghost_Stairs";
        if (lower.contains("_corner"))             return "Hearthbound_Ghost_Corner";
        if (lower.contains("wood_village_wall"))   return "Hearthbound_Ghost_Cube";
        if (lower.contains("_roof_flat"))    return "Hearthbound_Ghost_Roof_Flat";
        if (lower.contains("_roof_shallow")) return "Hearthbound_Ghost_Roof_Shallow";
        if (lower.contains("_roof_steep"))   return "Hearthbound_Ghost_Roof_Steep";
        if (lower.contains("_roof_hollow"))  return "Hearthbound_Ghost_Roof_Hollow";
        if (lower.contains("_roof"))         return "Hearthbound_Ghost_Roof";
        if (lower.contains("_half"))         return "Hearthbound_Ghost_Half";
        return "Hearthbound_Ghost_Cube";
    }

    // ========== Build Plan ==========

    private List<BlockPlacer.BlockEntry> loadBelowAnchorEmptyCells(
            String type, int anchorX, int anchorY, int anchorZ, int rotation) {
        String prefabName = BuildingType.getPrefabName(type);
        if (prefabName == null) return List.of();
        String anchorBlockId = BuildingType.getAnchorBlockId(type);
        int anchorPrefabY = BuildingType.getAnchorPrefabY(type);
        return PrefabLoader.loadBelowAnchorEmpty(prefabName, anchorBlockId, anchorPrefabY,
                anchorX, anchorY, anchorZ, rotation);
    }

    /**
     * Loads the build plan for a building type. Uses prefab if one is defined,
     * falls back to programmatic generation otherwise.
     */
    private List<BlockPlacer.BlockEntry> loadBuildPlan(
            String type, int anchorX, int anchorY, int anchorZ, int rotation) {
        String prefabName = BuildingType.getPrefabName(type);
        if (prefabName != null) {
            String anchorBlockId = BuildingType.getAnchorBlockId(type);
            int anchorPrefabY = BuildingType.getAnchorPrefabY(type);
            List<BlockPlacer.BlockEntry> plan = PrefabLoader.load(
                    prefabName, anchorBlockId, anchorPrefabY, anchorX, anchorY, anchorZ, rotation);
            if (!plan.isEmpty()) return plan;
            LOGGER.warning("Prefab '" + prefabName + "' empty, falling back to generator");
        }
        return BuildingGenerator.generate(type, anchorX, anchorY, anchorZ, rotation);
    }

    // ========== Status Queries ==========

    public boolean isBuilding() {
        return activeBuilder != null && !activeBuilder.isFinished();
    }

    /** Debug: short-circuit the active build to completion. Returns false if nothing is building. */
    public boolean finishActiveBuildNow() {
        if (activeBuilder == null || activeBuilder.isFinished()) return false;
        activeBuilder.finishNow();
        return true;
    }

    public boolean isPaused() {
        return activeBuilder != null && activeBuilder.isPaused();
    }

    public int getBuildProgress() {
        return activeBuilder != null ? activeBuilder.getProgress() : 0;
    }

    public BuildingRecord getActiveRecord() {
        return activeRecord;
    }

    /** Returns remaining resources needed by the active builder, or null if not building. */
    public java.util.Map<String, Integer> getRemainingResources() {
        if (activeBuilder == null || activeBuilder.isFinished()) return null;
        return activeBuilder.getRemainingResources();
    }

    /**
     * Returns the total resource requirements for a building type.
     * Uses prefab if available, falls back to BuildingGenerator.
     */
    public static java.util.Map<String, Integer> getRequiredResources(String type) {
        String prefabName = BuildingType.getPrefabName(type);
        if (prefabName != null) {
            String anchorBlockId = BuildingType.getAnchorBlockId(type);
            int anchorPrefabY = BuildingType.getAnchorPrefabY(type);
            List<BlockPlacer.BlockEntry> plan = PrefabLoader.load(prefabName, anchorBlockId, anchorPrefabY, 0, anchorPrefabY, 0);
            if (!plan.isEmpty()) {
                java.util.Map<String, Integer> resources = new java.util.LinkedHashMap<>();
                for (BlockPlacer.BlockEntry e : plan) {
                    String id = normalizeBlockId(e.blockType());
                    if (!ResourceBlockPlacer.isFreeBlock(id)) {
                        resources.merge(id, 1, Integer::sum);
                    }
                }
                return resources;
            }
        }
        return BuildingGenerator.getRequiredResources(type);
    }

    /** Strips '*' prefix and '_State_Definitions_...' suffix to get the placeable item ID. */
    private static String normalizeBlockId(String blockType) {
        String b = blockType.startsWith("*") ? blockType.substring(1) : blockType;
        int idx = b.indexOf("_State_Definitions_");
        return idx != -1 ? b.substring(0, idx) : b;
    }

    /** Reset all state (for /hb reset command). */
    public void reset() {
        if (activeBuilder != null) {
            activeBuilder.cancel();
            activeBuilder = null;
        }
        activeRecord = null;
        activeBuildPlan = null;
        activeRotation = 0;
        ghostSnapshot.clear();
    }
}
