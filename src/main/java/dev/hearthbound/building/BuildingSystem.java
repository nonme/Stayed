package dev.hearthbound.building;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.npc.BuilderBehavior;
import dev.hearthbound.npc.ElfSage;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.BuildingType;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

import java.util.ArrayList;
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

    private SiteClearer activeClearer;
    private ResourceBlockPlacer activeBuilder;
    private BuildingRecord activeRecord;

    /**
     * One ghost preview session per player. Cycling variants only affects the calling player,
     * and one player breaking their brazier doesn't wipe another player's preview.
     */
    private static final class PreviewSession {
        List<Ref<EntityStore>> refs;
        int rotation;
        int variant;
        String buildingType;
        int anchorX, anchorY, anchorZ;
    }

    private final java.util.Map<UUID, PreviewSession> previews = new java.util.concurrent.ConcurrentHashMap<>();

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
     * Spawns BlockEntity markers for each structural block in the plan.
     * Never writes to the chunk — no snapshot, no restore, no cascade, no collision holes.
     * Stores the session per-player so cycling variants and clearing only affect this player.
     */
    public boolean showGhostPreview(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                     World world, String buildingType,
                                     int anchorX, int anchorY, int anchorZ, int rotation, int variant) {
        List<BlockPlacer.BlockEntry> plan = loadBuildPlan(buildingType, anchorX, anchorY, anchorZ, rotation, variant);
        if (plan.isEmpty()) {
            LOGGER.warning("No building plan for type: " + buildingType + " variant=" + variant);
            return false;
        }

        UUID playerUuid = resolvePlayerUuid(store, playerRef);
        if (playerUuid == null) {
            LOGGER.warning("[Ghost] showGhostPreview: player UUID is null, refusing to spawn preview");
            return false;
        }

        PreviewSession session = new PreviewSession();
        session.refs = GhostPreview.show(store, plan);
        session.rotation = rotation;
        session.variant = variant;
        session.buildingType = buildingType;
        session.anchorX = anchorX;
        session.anchorY = anchorY;
        session.anchorZ = anchorZ;
        previews.put(playerUuid, session);

        GhostPreview.sendBoundingBox(playerUuid, world, plan);

        LOGGER.info("[Ghost] showGhostPreview: type=" + buildingType + " rotation=" + rotation
                + " variant=" + variant + " plan=" + plan.size()
                + " entities=" + session.refs.size()
                + " anchor=(" + anchorX + "," + anchorY + "," + anchorZ + ")");
        return true;
    }

    /** Overload without explicit variant — uses 0 (legacy default). */
    public boolean showGhostPreview(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                     World world, String buildingType,
                                     int anchorX, int anchorY, int anchorZ, int rotation) {
        return showGhostPreview(store, playerRef, world, buildingType, anchorX, anchorY, anchorZ, rotation, 0);
    }

    /** Overload without rotation or variant — uses 0 for both (legacy). */
    public boolean showGhostPreview(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                     World world, String buildingType,
                                     int anchorX, int anchorY, int anchorZ) {
        return showGhostPreview(store, playerRef, world, buildingType, anchorX, anchorY, anchorZ, 0, 0);
    }

    /** Removes the calling player's preview entities. Must be called on the world thread. */
    public void clearGhostPreview(Store<EntityStore> store, Ref<EntityStore> playerRef, World world) {
        UUID playerUuid = resolvePlayerUuid(store, playerRef);
        clearGhostPreviewFor(store, playerUuid, world);
    }

    /**
     * Removes a specific player's preview by UUID. Used by callers that have the UUID directly
     * (e.g. block break events where the entity ref is the breaker).
     */
    public void clearGhostPreviewFor(Store<EntityStore> store, UUID playerUuid, World world) {
        if (playerUuid == null) return;
        PreviewSession session = previews.remove(playerUuid);
        if (session == null) return;
        GhostPreview.clearBoundingBox(playerUuid, world);
        GhostPreview.clear(store, session.refs);
    }

    public boolean hasActivePreview(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        UUID uuid = resolvePlayerUuid(store, playerRef);
        if (uuid == null) return false;
        PreviewSession s = previews.get(uuid);
        return s != null && s.refs != null && !s.refs.isEmpty();
    }

    /** @deprecated kept so legacy callsites compile; checks across all players and is racy. */
    @Deprecated
    public boolean hasActivePreview() {
        for (PreviewSession s : previews.values()) {
            if (s.refs != null && !s.refs.isEmpty()) return true;
        }
        return false;
    }

    public int getActiveRotation(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        UUID uuid = resolvePlayerUuid(store, playerRef);
        if (uuid == null) return 0;
        PreviewSession s = previews.get(uuid);
        return s != null ? s.rotation : 0;
    }

    /** @deprecated kept so legacy callsites compile; returns 0 if multiple players are active. */
    @Deprecated
    public int getActiveRotation() {
        if (previews.size() == 1) {
            return previews.values().iterator().next().rotation;
        }
        return 0;
    }

    public int getActiveVariant(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        UUID uuid = resolvePlayerUuid(store, playerRef);
        if (uuid == null) return 0;
        PreviewSession s = previews.get(uuid);
        return s != null ? s.variant : 0;
    }

    /**
     * Re-renders the active preview for this player using the new variant. Used by the
     * VillagerHousePage variant switcher. Returns the new variant index or -1 if no preview
     * is active.
     */
    public int cycleHouseVariant(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                  World world, int delta) {
        UUID uuid = resolvePlayerUuid(store, playerRef);
        if (uuid == null) return -1;
        PreviewSession s = previews.get(uuid);
        if (s == null) return -1;
        if (!BuildingType.HOUSE_HUMAN.equals(s.buildingType)) return s.variant;

        int next = BuildingType.wrapHouseVariant(s.variant + delta);
        // Snapshot anchor + rotation before clearing wipes the session.
        String type = s.buildingType;
        int rot = s.rotation;
        int ax = s.anchorX, ay = s.anchorY, az = s.anchorZ;

        clearGhostPreviewFor(store, uuid, world);
        showGhostPreview(store, playerRef, world, type, ax, ay, az, rot, next);
        return next;
    }

    private static UUID resolvePlayerUuid(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        if (playerRef == null) return null;
        com.hypixel.hytale.server.core.entity.entities.Player player =
                store.getComponent(playerRef, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
        return player != null ? player.getUuid() : null;
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

        if (activeClearer != null) {
            activeClearer.cancel();
            activeClearer = null;
        }
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

        int variant = record.getVariant();
        List<BlockPlacer.BlockEntry> plan = loadBuildPlan(record.getType(),
                record.getPosX(), record.getPosY(), record.getPosZ(), rotation, variant);

        if (plan.isEmpty()) {
            LOGGER.warning("No building plan for type: " + record.getType() + " variant=" + variant);
            return;
        }

        activeRecord = record;
        record.setRotation(rotation);
        village.setConstructionStarted(true);
        VillageManager.get().save(store, playerRef, village);

        // For mine: clear all terrain below the anchor level before construction begins.
        // This is done once here so the elf never has to "place Empty" during building.
        if (BuildingType.MINE.equals(record.getType())) {
            List<BlockPlacer.BlockEntry> belowEmpty = loadBelowAnchorEmptyCells(
                    record.getType(), record.getPosX(), record.getPosY(), record.getPosZ(), rotation, variant);
            for (BlockPlacer.BlockEntry entry : belowEmpty) {
                var existing = world.getBlockType(entry.x(), entry.y(), entry.z());
                String existingId = (existing != null) ? existing.getId() : "Empty";
                if (!existingId.equals("Empty") && !existingId.equals("Editor_Empty")) {
                    BlockPlacer.silentRemoveBlock(world, entry.x(), entry.y(), entry.z());
                }
            }
            LOGGER.info("Mine terrain cleared: " + belowEmpty.size() + " cells below anchor");
        }

        // Safe position: a couple blocks in front of the building door.
        int[] doorOffset = BuildingType.getDoorOffset(record.getType(), rotation, record.getVariant());
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

        final double fSafeX = safeX, fSafeY = safeY, fSafeZ = safeZ;
        final UUID fElfUuid = elfUuid;

        dev.hearthbound.npc.BuilderBehavior builderBehavior = fElfUuid != null
                ? new dev.hearthbound.npc.BuilderBehavior(world, fElfUuid, ownerUuid)
                : null;

        // Pin the leash point to the horizontal center of the build plan so the elf
        // stays near the construction site rather than drifting to a random corner.
        if (builderBehavior != null && !plan.isEmpty()) {
            double cx = plan.stream().mapToInt(BlockPlacer.BlockEntry::x).average().orElse(fSafeX);
            double cz = plan.stream().mapToInt(BlockPlacer.BlockEntry::z).average().orElse(fSafeZ);
            double cy = plan.stream().mapToInt(BlockPlacer.BlockEntry::y).min().orElse((int) fSafeY) + 1.0;
            builderBehavior.setLeashPoint(cx, cy, cz);
        }

        // Phase 1: clear all occupied cells with pickaxe animation, skipping cells the
        // player already broke. Phase 2: block-by-block construction starts on completion.
        java.util.Set<Long> occupiedCells = loadOccupiedCells(record.getType(),
                record.getPosX(), record.getPosY(), record.getPosZ(), rotation, variant);
        activeClearer = new SiteClearer(world, plan, occupiedCells, builderBehavior, () -> {
            activeClearer = null;
            activeBuilder = new ResourceBlockPlacer(world, plan, record,
                    fSafeX, fSafeY, fSafeZ, fElfUuid, ownerUuid, () -> {
                world.execute(() -> {
                    Store<EntityStore> worldStore = world.getEntityStore().getStore();
                    onBuildingComplete(worldStore, playerRef, world, record);
                });
            });
            activeBuilder.start();
            LOGGER.info("Site cleared — construction started: " + record.getType()
                    + " (" + plan.size() + " blocks)");
        });
        activeClearer.start();

        LOGGER.info("Site clearing started: " + record.getType() + " (" + plan.size() + " blocks to scan)");
    }

    private void onBuildingComplete(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                     World world, BuildingRecord record) {
        // Swap in the real prefab now that the shell is done. The block-by-block build
        // pass can't fix up connected-blocks (fences, stairs, wall corners) correctly
        // because each block looks at neighbors that don't exist yet at placement time.
        // PrefabStore.placeNoReturn places the whole selection atomically, so the
        // engine's connected-block resolver has the full neighborhood available.
        replaceWithPrefab(world, store, record, record.getRotation());

        // After any build the elf moves inside the Town Hall and wanders there.
        if (activeBuilder != null) {
            Vector3d returnPos = elfReturnPos(store, playerRef, record,
                    activeBuilder.getSafeX(), activeBuilder.getSafeY(), activeBuilder.getSafeZ());
            ElfSage.respawnAs(store, playerRef, world, ElfSage.ROLE_VILLAGER,
                    returnPos, new Vector3f(0, 0, 0));

        }

        VillageManager mgr = VillageManager.get();
        mgr.completeBuilding(store, playerRef, record);

        // After completion, draw a pathway from this building's door to the nearest existing
        // completed building's door — gives the village an organic road network as it grows.
        VillageData villageForPath = mgr.getVillageData(store, playerRef);
        if (villageForPath != null) {
            int placed = PathwayBuilder.connectNewBuilding(world, villageForPath, record);
            if (placed > 0) {
                mgr.save(store, playerRef, villageForPath);
                LOGGER.info("Pathway placed: " + placed + " blocks from " + record.getType());
            }
        }

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
     * Where the elf should stand after finishing a build — inside the Town Hall.
     * Uses BuildingLayout interior center so the elf ends up inside, not at the door.
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

        BuildingLayout.Layout layout = BuildingLayout.get(BuildingType.TOWN_HALL);
        int steps = layout.rotationSteps(rot);
        int[] center = BuildingLayout.rotateLocalOffset(
                layout.centerLX(), layout.floorLY(), layout.centerLZ(), steps);
        return new Vector3d(sx + center[0] + 0.5, sy + layout.floorLY() + 1.0, sz + center[2] + 0.5);
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
        int variant = record.getVariant();
        String prefabName = BuildingType.getPrefabName(record.getType(), variant);
        if (prefabName == null) return;

        try {
            com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection selection =
                    com.hypixel.hytale.server.core.prefab.PrefabStore.get()
                            .getAssetPrefabFromAnyPack(prefabName + ".prefab.json");

            // Our PrefabLoader and the engine's BlockSelection.rotate(Axis.Y) now use the
            // same handedness (CCW), so we rotate by exactly steps * 90°.
            String anchorBlockId = BuildingType.getAnchorBlockId(record.getType());
            int anchorPrefabY = BuildingType.getAnchorPrefabY(record.getType(), variant);
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

    // ========== Build Plan ==========

    private List<BlockPlacer.BlockEntry> loadBelowAnchorEmptyCells(
            String type, int anchorX, int anchorY, int anchorZ, int rotation, int variant) {
        String prefabName = BuildingType.getPrefabName(type, variant);
        if (prefabName == null) return List.of();
        String anchorBlockId = BuildingType.getAnchorBlockId(type);
        int anchorPrefabY = BuildingType.getAnchorPrefabY(type, variant);
        return PrefabLoader.loadBelowAnchorEmpty(prefabName, anchorBlockId, anchorPrefabY,
                anchorX, anchorY, anchorZ, rotation);
    }

    private java.util.Set<Long> loadOccupiedCells(
            String type, int anchorX, int anchorY, int anchorZ, int rotation, int variant) {
        String prefabName = BuildingType.getPrefabName(type, variant);
        if (prefabName == null) return java.util.Set.of();
        String anchorBlockId = BuildingType.getAnchorBlockId(type);
        int anchorPrefabY = BuildingType.getAnchorPrefabY(type, variant);
        return PrefabLoader.loadOccupiedCells(prefabName, anchorBlockId, anchorPrefabY,
                anchorX, anchorY, anchorZ, rotation);
    }

    /**
     * Loads the build plan for a building type and variant. Uses prefab if one is defined,
     * falls back to programmatic generation otherwise. variant=0 always maps to the original
     * prefab (back-compat with old saves that have no variant field).
     */
    private List<BlockPlacer.BlockEntry> loadBuildPlan(
            String type, int anchorX, int anchorY, int anchorZ, int rotation, int variant) {
        String prefabName = BuildingType.getPrefabName(type, variant);
        if (prefabName != null) {
            String anchorBlockId = BuildingType.getAnchorBlockId(type);
            int anchorPrefabY = BuildingType.getAnchorPrefabY(type, variant);
            boolean mineOrder = dev.hearthbound.village.BuildingType.MINE.equals(type);
            List<BlockPlacer.BlockEntry> plan = PrefabLoader.load(
                    prefabName, anchorBlockId, anchorPrefabY, anchorX, anchorY, anchorZ, rotation, mineOrder);
            if (!plan.isEmpty()) return plan;
            LOGGER.warning("Prefab '" + prefabName + "' empty, falling back to generator");
        }
        return BuildingGenerator.generate(type, anchorX, anchorY, anchorZ, rotation);
    }

    // ========== Status Queries ==========

    public boolean isBuilding() {
        return activeClearer != null || (activeBuilder != null && !activeBuilder.isFinished());
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
     * Returns the total resource requirements for a building type (variant 0).
     * Uses prefab if available, falls back to BuildingGenerator.
     */
    public static java.util.Map<String, Integer> getRequiredResources(String type) {
        return getRequiredResources(type, 0);
    }

    /**
     * Variant-aware resource requirements. Different house variants have different sizes
     * and therefore different resource costs.
     */
    public static java.util.Map<String, Integer> getRequiredResources(String type, int variant) {
        String prefabName = BuildingType.getPrefabName(type, variant);
        if (prefabName != null) {
            String anchorBlockId = BuildingType.getAnchorBlockId(type);
            int anchorPrefabY = BuildingType.getAnchorPrefabY(type, variant);
            List<BlockPlacer.BlockEntry> plan = PrefabLoader.load(prefabName, anchorBlockId, anchorPrefabY, 0, anchorPrefabY, 0);
            if (!plan.isEmpty()) {
                java.util.Map<String, Integer> resources = new java.util.LinkedHashMap<>();
                boolean isMine = dev.hearthbound.village.BuildingType.MINE.equals(type);
                for (BlockPlacer.BlockEntry e : plan) {
                    String id = normalizeBlockId(e.blockType());
                    if (ResourceBlockPlacer.isFreeBlock(id)) continue;
                    if (isMine && ResourceBlockPlacer.isMineExcavationBlock(id)) continue;
                    resources.merge(id, 1, Integer::sum);
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

    /**
     * Called on player join. If VillageData shows construction was in progress when the
     * server stopped, resumes it from scratch — SiteClearer will skip already-cleared
     * terrain instantly, and ResourceBlockPlacer will overwrite already-placed blocks
     * (idempotent). The elf is respawned in builder role first.
     */
    public void resumeConstructionIfNeeded(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                           World world, UUID ownerUuid) {
        if (isBuilding()) return;

        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village == null || !village.isConstructionStarted()) return;

        BuildingRecord inProgress = village.getBuildings().stream()
                .filter(b -> !b.isCompleted())
                .findFirst()
                .orElse(null);
        if (inProgress == null) {
            // All buildings completed but flag wasn't cleared — fix it.
            village.setConstructionStarted(false);
            VillageManager.get().save(store, playerRef, village);
            return;
        }

        LOGGER.info("[Resume] Resuming interrupted construction: " + inProgress.getType()
                + " rotation=" + inProgress.getRotation()
                + " pos=(" + inProgress.getPosX() + "," + inProgress.getPosY() + "," + inProgress.getPosZ() + ")"
                + " for player " + ownerUuid);
        startResourceBuilding(store, playerRef, world, inProgress, inProgress.getRotation(), ownerUuid);
    }

    /** Reset all state (for /hb reset command). */
    public void reset(Store<EntityStore> store) {
        if (activeClearer != null) {
            activeClearer.cancel();
            activeClearer = null;
        }
        if (activeBuilder != null) {
            activeBuilder.cancel();
            activeBuilder = null;
        }
        activeRecord = null;
        for (PreviewSession s : previews.values()) {
            GhostPreview.clear(store, s.refs);
        }
        previews.clear();
    }
}
