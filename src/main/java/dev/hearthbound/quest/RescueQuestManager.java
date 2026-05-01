package dev.hearthbound.quest;

import com.hypixel.hytale.builtin.adventure.objectives.Objective;
import com.hypixel.hytale.builtin.adventure.objectives.ObjectivePlugin;
import com.hypixel.hytale.builtin.adventure.objectives.markers.reachlocation.ReachLocationMarker;
import com.hypixel.hytale.builtin.adventure.objectives.task.ReachLocationTask;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.environment.config.Environment;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HiddenFromAdventurePlayers;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.npc.HearthboundDataStore;
import dev.hearthbound.npc.NpcManager;
import dev.hearthbound.npc.NpcRegistry;
import dev.hearthbound.npc.NpcRestorer;
import dev.hearthbound.npc.VillagerAppearance;
import dev.hearthbound.npc.VillagerNames;
import dev.hearthbound.npc.appearance.BodyArchetype;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;
import dev.hearthbound.village.VillagerData;
import dev.hearthbound.village.VillagerSummary;
import it.unimi.dsi.fastutil.Pair;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.math.util.ChunkUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Manages all rescue quest variants: selects the next variant using rotation
 * logic, places the prefab, spawns NPCs, and handles cleanup.
 *
 * <p>
 * Rotation rules:
 * <ol>
 * <li>TRAP is always first (tutorial). After it fires once, it leaves the pool
 * forever.</li>
 * <li>From the remaining 3 (CABIN, RUINS, CAMP), exclude the 2 most recently
 * played.</li>
 * <li>Result: next quest is always one the player hasn't seen in the last 2
 * rounds.</li>
 * </ol>
 */
public final class RescueQuestManager {

    private static final Logger LOGGER = Logger.getLogger(RescueQuestManager.class.getName());

    // -------------------------------------------------------------------------
    // Quest variants
    // -------------------------------------------------------------------------
    public enum QuestVariant {
        TRAP("Villager_ResqueQuest_trap"),
        CABIN("Villager_ResqueQuest_cabin"),
        RUINS("Villager_ResqueQuest_ruins"),
        CAMP("Villager_ResqueQuest_camp");

        public final String prefabName;

        QuestVariant(String prefabName) {
            this.prefabName = prefabName;
        }
    }

    /**
     * Marker block name in cabin/ruins/camp prefabs indicating where enemies
     * should spawn. The block is removed after prefab placement; an NPC is
     * spawned at its position.
     */
    private static final String ENEMY_MARKER = "Deco_Bone_Skulls";

    /**
     * Marker block name in cabin/ruins/camp prefabs indicating where the victim
     * should spawn. The block is removed after prefab placement; an NPC is
     * spawned at its position.
     */
    private static final String VICTIM_MARKER = "Deco_Bone_Skulls_Feran";

    // Enemy roles per variant — cycled round-robin if there are more spawn points than roles.
    private static final String[] CABIN_ENEMIES = {"Wolf_Black"};
    private static final String[] RUINS_ENEMIES = {"Skeleton_Archer"};
    private static final String[] CAMP_ENEMIES = {"Goblin_Scrapper", "Goblin_Scavenger", "Goblin_Miner"};

    // TRAP variant uses hardcoded offsets (no skull markers — old prefab kept as-is)
    private static final String TRAP_GUARD_ROLE = "Goblin_Scavenger";
    private static final int TRAP_VICTIM_DX = 1;
    private static final int TRAP_VICTIM_PREFAB_Y = 2;
    private static final int TRAP_VICTIM_DZ = 0;
    private static final int TRAP_GUARD_DX = 8;
    private static final int TRAP_GUARD_DZ = 0;
    private static final int TRAP_PREFAB_SURFACE_Y = 8;

    private static final String VARIANT_VICTIM_ROLE = "Villager_Rescue_Trapped";

    // Prefab surface Y: the prefab-local Y layer that should align with groundY.
    // CAMP: subsoil fill at Y=0..1, main pathway at Y=2.
    // CABIN: subsoil fill at Y=0, ground surface at Y=1.
    // RUINS: Soil_Grass already at Y=0, no fill layer.
    private static int prefabSurfaceY(QuestVariant variant) {
        return switch (variant) {
            case CAMP ->
                2;
            case CABIN ->
                1;
            default ->
                0;
        };
    }

    // Real footprint extents relative to anchor (0,0,0), derived from prefab block ranges.
    // Format: [minX, maxX, minZ, maxZ, maxPrefabY] — maxPrefabY is the highest non-empty block Y in the prefab.
    // World top of prefab = groundY + (maxPrefabY - prefabSurfaceY).
    private static final int[] CABIN_EXTENT = {-9, 10, -11, 12, 22};
    private static final int[] RUINS_EXTENT = {-11, 11, -7, 7, 11};
    private static final int[] CAMP_EXTENT = {-12, 12, -11, 11, 18};

    // Maximum allowed height difference (blocks) across the prefab footprint
    // Max allowed height difference between any two corners of the prefab footprint
    private static final int MAX_CORNER_VARIANCE = 3;
    // If getHeight() is more than this above solid ground, the column is on a tree — reject
    private static final int MAX_TREE_HEIGHT = 5;

    // Objective line IDs per variant. TRAP keeps the original ID for backward compat.
    private static final String OBJECTIVE_LINE_TRAP = "ObjectiveLine_RescueQuest_1";
    private static final String OBJECTIVE_LINE_CABIN = "ObjectiveLine_RescueQuest_Cabin";
    private static final String OBJECTIVE_LINE_RUINS = "ObjectiveLine_RescueQuest_Ruins";
    private static final String OBJECTIVE_LINE_CAMP = "ObjectiveLine_RescueQuest_Camp";

    private static String objectiveLineFor(QuestVariant variant) {
        return switch (variant) {
            case CABIN ->
                OBJECTIVE_LINE_CABIN;
            case RUINS ->
                OBJECTIVE_LINE_RUINS;
            case CAMP ->
                OBJECTIVE_LINE_CAMP;
            default ->
                OBJECTIVE_LINE_TRAP;
        };
    }

    private static final double MIN_DISTANCE = 250.0;
    private static final double MAX_DISTANCE = 450.0;
    private static final int MAX_SPAWN_ATTEMPTS = 20;

    // -------------------------------------------------------------------------
    // Active NPC tracking
    // -------------------------------------------------------------------------
    private static final List<Ref<EntityStore>> activeNpcRefs = new ArrayList<>();

    public static void registerFollower(Ref<EntityStore> ref) {
        activeNpcRefs.add(ref);
    }

    public static void unregisterNpc(Ref<EntityStore> ref) {
        activeNpcRefs.remove(ref);
    }

    // -------------------------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------------------------
    public static void cleanup(Store<EntityStore> store) {
        for (Ref<EntityStore> ref : new ArrayList<>(activeNpcRefs)) {
            try {
                UUID uuid = NpcManager.extractUuid(store, ref);
                if (uuid != null) {
                    removeQuestNpc(store, uuid, ref);
                }
            } catch (Exception e) {
                LOGGER.warning("cleanup: failed to remove NPC ref: " + e.getMessage());
            }
        }
        activeNpcRefs.clear();

        for (NpcRegistry.NpcRecord record : NpcRegistry.get().allRecords()) {
            if (record.interaction != NpcRegistry.InteractionType.RESCUE) {
                continue;
            }
            LOGGER.info("cleanup: removing stale RESCUE record uuid=" + record.entityUuid);
            removeQuestNpc(store, record.entityUuid, null);
        }

        HearthboundDataStore.get().save();
    }

    private static void removeQuestNpc(Store<EntityStore> store, UUID uuid, Ref<EntityStore> ref) {
        NpcRegistry.NpcRecord record = NpcRegistry.get().getRecord(uuid);
        long chunkIndex = record != null ? record.chunkIndex : 0L;
        NpcRegistry.get().unregister(uuid);

        if (ref != null && ref.isValid()) {
            store.removeEntity(ref, RemoveReason.REMOVE);
        } else {
            NpcRegistry.get().markForRemoval(uuid, chunkIndex);
        }
    }

    public static void removeAllMarkers(Store<EntityStore> store) {
        var markerType = ReachLocationMarker.getComponentType();
        List<Ref<EntityStore>> toRemove = new ArrayList<>();
        store.forEachChunk(markerType, (chunk, cb) -> {
            for (int i = 0; i < chunk.size(); i++) {
                toRemove.add(chunk.getReferenceTo(i));
            }
        });
        for (Ref<EntityStore> ref : toRemove) {
            store.removeEntity(ref, RemoveReason.REMOVE);
        }
    }

    public static void cancelAllObjectives(Store<EntityStore> store, Ref<EntityStore> playerRef,
            com.hypixel.hytale.server.core.entity.entities.Player player) {
        if (player == null) {
            return;
        }
        Set<UUID> active = player.getPlayerConfigData().getActiveObjectiveUUIDs();
        if (active == null || active.isEmpty()) {
            return;
        }
        for (UUID objectiveUuid : new java.util.HashSet<>(active)) {
            ObjectivePlugin.get().cancelObjective(objectiveUuid, store);
        }
    }

    // -------------------------------------------------------------------------
    // Rotation logic
    // -------------------------------------------------------------------------
    /**
     * Picks the next quest variant for this player.
     *
     * <p>
     * Migration note: existing saves where {@code rescueQuestStarted=true} but
     * {@code rescueQuestHistory} is empty had only the TRAP variant available,
     * so we infer that TRAP was already played and set
     * {@code rescueQuestTrapDone=true}.
     */
    public static QuestVariant pickNextVariant(VillageData village) {
        // Backward-compat migration for saves predating the rotation system
        if (village.isRescueQuestStarted() && !village.isRescueQuestTrapDone()
                && village.getRescueQuestHistory().isEmpty()) {
            village.setRescueQuestTrapDone(true);
        }

        if (!village.isRescueQuestTrapDone()) {
            return QuestVariant.TRAP;
        }

        List<QuestVariant> pool = new ArrayList<>(Arrays.asList(
                QuestVariant.CABIN, QuestVariant.RUINS, QuestVariant.CAMP));

        // Exclude the 2 most recently played
        List<String> history = village.getRescueQuestHistory();
        int excludeCount = Math.min(2, history.size());
        for (int i = history.size() - 1; i >= history.size() - excludeCount; i--) {
            String recent = history.get(i);
            pool.removeIf(v -> v.name().equals(recent));
        }

        if (pool.isEmpty()) {
            // Safety fallback — shouldn't happen with 3 variants and 2 excluded
            pool = new ArrayList<>(Arrays.asList(QuestVariant.CABIN, QuestVariant.RUINS, QuestVariant.CAMP));
        }

        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    /**
     * Records a completed quest variant into the village's history and marks
     * TRAP as done if it was played. Caller must persist VillageData after this
     * call.
     */
    public static void recordVariantPlayed(VillageData village, QuestVariant variant) {
        if (variant == QuestVariant.TRAP) {
            village.setRescueQuestTrapDone(true);
        } else {
            village.getRescueQuestHistory().add(variant.name());
        }
    }

    // -------------------------------------------------------------------------
    // Quest start entry point
    // -------------------------------------------------------------------------
    public static void startForPlayer(World world, Store<EntityStore> store,
            Ref<EntityStore> playerRef,
            com.hypixel.hytale.server.core.entity.entities.Player player,
            UUID playerUuid,
            Vector3d fromPos,
            QuestVariant variant,
            Consumer<Spawned> onDone) {
        applyMarkerIconOnce();

        findSpawnLocation(world, fromPos, ThreadLocalRandom.current(), variant)
                .thenAccept(candidate -> world.execute(() -> {
            Store<EntityStore> liveStore = world.getEntityStore().getStore();

            com.hypixel.hytale.server.core.entity.entities.Player livePlayer
                    = liveStore.getComponent(playerRef,
                            com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
            cancelAllObjectives(liveStore, playerRef, livePlayer);
            removeAllMarkers(liveStore);
            cleanup(liveStore);

            if (candidate == null) {
                LOGGER.warning("startForPlayer: no valid spawn after " + MAX_SPAWN_ATTEMPTS + " attempts");
                onDone.accept(null);
                return;
            }

            int blockX = candidate[0];
            int groundY = candidate[1];
            int blockZ = candidate[2];
            QuestVariant chosenVariant = QuestVariant.values()[candidate[4]];

            Set<String> takenNames = collectTakenFullNames(liveStore, playerRef);
            Spawned spawned = spawn(world, liveStore, blockX, groundY, blockZ, takenNames, chosenVariant);
            if (spawned == null) {
                LOGGER.warning("startForPlayer: spawn() failed at ("
                        + blockX + "," + groundY + "," + blockZ + ") variant=" + variant);
                onDone.accept(null);
                return;
            }

            spawnQuestMarker(liveStore, spawned.victimPos(), playerRef, chosenVariant);

            String objectiveLineId = objectiveLineFor(chosenVariant);
            Objective objective = ObjectivePlugin.get().startObjectiveLine(
                    liveStore, objectiveLineId, Set.of(playerUuid),
                    world.getWorldConfig().getUuid(), null);
            if (objective == null) {
                LOGGER.warning("startForPlayer: failed to start ObjectiveLine " + objectiveLineId);
            }

            LOGGER.info("Quest started. Variant=" + chosenVariant
                    + " at (" + blockX + "," + groundY + "," + blockZ + ")");
            onDone.accept(spawned);
        }));
    }

    // -------------------------------------------------------------------------
    // Spawn dispatch
    // -------------------------------------------------------------------------
    public static Spawned spawn(World world, Store<EntityStore> store,
            int centerX, int groundY, int centerZ,
            Set<String> takenFirstNames, QuestVariant variant) {
        if (variant == QuestVariant.TRAP) {
            return spawnTrap(world, store, centerX, groundY, centerZ, takenFirstNames);
        }
        return spawnVariant(world, store, centerX, groundY, centerZ, takenFirstNames, variant);
    }

    // -------------------------------------------------------------------------
    // TRAP spawn — hardcoded offsets, no skull markers
    // -------------------------------------------------------------------------
    private static Spawned spawnTrap(World world, Store<EntityStore> store,
            int centerX, int groundY, int centerZ,
            Set<String> takenFirstNames) {
        int prefabOriginY = groundY - TRAP_PREFAB_SURFACE_Y;

        try {
            BlockSelection selection = PrefabStore.get()
                    .getAssetPrefabFromAnyPack(QuestVariant.TRAP.prefabName + ".prefab.json");
            if (selection == null) {
                LOGGER.warning("Prefab not found: " + QuestVariant.TRAP.prefabName);
                return null;
            }
            selection.setAnchor(0, 0, 0);
            selection.placeNoReturn(world, new Vector3i(centerX, prefabOriginY, centerZ), store);
        } catch (Exception e) {
            LOGGER.warning("Failed to place trap prefab: " + e.getMessage());
            return null;
        }

        Vector3d victimPos = new Vector3d(
                centerX + TRAP_VICTIM_DX + 0.5,
                prefabOriginY + TRAP_VICTIM_PREFAB_Y,
                centerZ + TRAP_VICTIM_DZ + 0.5);
        spawnVictim(store, world, victimPos, takenFirstNames, QuestVariant.TRAP);

        Vector3d guardPos = new Vector3d(
                centerX + TRAP_GUARD_DX + 0.5,
                groundY + 1,
                centerZ + TRAP_GUARD_DZ + 0.5);
        spawnEnemy(store, guardPos, TRAP_GUARD_ROLE);

        return new Spawned(victimPos, guardPos);
    }

    // -------------------------------------------------------------------------
    // Marker-based spawn (CABIN, RUINS, CAMP)
    // -------------------------------------------------------------------------
    /**
     * Places the prefab, reads skull-marker positions from the raw block data
     * before placement so they can be cleared and replaced by NPC spawns.
     */
    private static Spawned spawnVariant(World world, Store<EntityStore> store,
            int centerX, int groundY, int centerZ,
            Set<String> takenFirstNames, QuestVariant variant) {
        String prefabFile = variant.prefabName + ".prefab.json";
        int prefabOriginY = groundY - prefabSurfaceY(variant);

        BlockSelection selection;
        try {
            selection = PrefabStore.get().getAssetPrefabFromAnyPack(prefabFile);
            if (selection == null) {
                LOGGER.warning("Prefab not found: " + prefabFile);
                return null;
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to load prefab " + prefabFile + ": " + e.getMessage());
            return null;
        }

        // Collect marker positions before placing (we need local coords from the selection).
        var blockTypeMap = BlockType.getAssetMap();
        int selOriginX = selection.getX();
        int selOriginY = selection.getY();
        int selOriginZ = selection.getZ();

        List<Vector3d> enemyPositions = new ArrayList<>();
        // Marker block world positions to clear after placement
        List<int[]> markerWorldPositions = new ArrayList<>();

        // ForEachBlock captures local-to-selection coordinates; we convert to world coords.
        final Vector3d[] victimPosHolder = {null};
        selection.forEachBlock((bx, by, bz, holder) -> {
            if (holder.filler() != 0) {
                return;
            }
            BlockType bt = blockTypeMap.getAsset(holder.blockId());
            if (bt == null) {
                return;
            }
            String id = bt.getId();

            // Local position relative to selection origin
            int lx = bx - selOriginX;
            int ly = by - selOriginY;
            int lz = bz - selOriginZ;

            // World position
            int wx = centerX + lx;
            int wy = prefabOriginY + ly;
            int wz = centerZ + lz;

            if (VICTIM_MARKER.equals(id)) {
                victimPosHolder[0] = new Vector3d(wx + 0.5, wy, wz + 0.5);
                markerWorldPositions.add(new int[]{wx, wy, wz});
            } else if (ENEMY_MARKER.equals(id)) {
                enemyPositions.add(new Vector3d(wx + 0.5, wy + 1.0, wz + 0.5));
                markerWorldPositions.add(new int[]{wx, wy, wz});
            }
        });

        if (victimPosHolder[0] == null) {
            LOGGER.warning("No victim marker (" + VICTIM_MARKER + ") in prefab: " + prefabFile);
            return null;
        }
        Vector3d victimPos = victimPosHolder[0];

        // Clear vegetation inside footprint before placing so nothing pokes through the prefab
        int[] ext = footprintExtent(variant);
        clearVegetation(world, centerX, groundY, centerZ, ext);

        // Place the prefab (marker blocks included — we remove them right after)
        try {
            selection.setAnchor(0, 0, 0);
            selection.placeNoReturn(world, new Vector3i(centerX, prefabOriginY, centerZ), store);
        } catch (Exception e) {
            LOGGER.warning("Failed to place prefab " + prefabFile + ": " + e.getMessage());
            return null;
        }

        // Remove marker blocks placed by the prefab
        for (int[] pos : markerWorldPositions) {
            world.breakBlock(pos[0], pos[1], pos[2], 0);
        }

        // Fill loot chests for variants that have them
        if (variant == QuestVariant.CAMP) {
            fillCampChest(world, centerX, groundY, centerZ, ThreadLocalRandom.current());
        }

        // Smooth terrain transition around the footprint using signed distance to rectangle
        blendTerrain(world, centerX, groundY, centerZ, ext);
        // Remove decor/plants left floating after blendTerrain lowered ground outside the footprint.
        clearFloating(world, centerX, groundY, centerZ, ext);
        // Restore grass plants on the blend zone — clearVegetation wiped them, blendTerrain left bare soil.
        restoreGrassDecor(world, centerX, groundY, centerZ, ext);

        spawnVictim(store, world, victimPos, takenFirstNames, variant);

        String[] enemyRoles = enemyRolesFor(variant);
        for (int i = 0; i < enemyPositions.size(); i++) {
            String role = enemyRoles[i % enemyRoles.length];
            spawnEnemy(store, enemyPositions.get(i), role);
        }

        Vector3d firstEnemyPos = enemyPositions.isEmpty() ? victimPos : enemyPositions.get(0);
        return new Spawned(victimPos, firstEnemyPos);
    }

    // -------------------------------------------------------------------------
    // NPC spawn helpers
    // -------------------------------------------------------------------------
    private static void spawnVictim(Store<EntityStore> store, World world,
            Vector3d pos, Set<String> takenFirstNames, QuestVariant variant) {
        Pair<Ref<EntityStore>, INonPlayerCharacter> result
                = NpcManager.spawnNpc(store, pos, new Vector3f(0, 0, 0), VARIANT_VICTIM_ROLE);
        if (result == null) {
            LOGGER.warning("Failed to spawn rescue victim at " + pos);
            return;
        }

        Ref<EntityStore> victimRef = result.first();
        activeNpcRefs.add(victimRef);

        long skinSeed = ThreadLocalRandom.current().nextLong();
        BodyArchetype body = VillagerAppearance.predictBody(skinSeed);
        String[] name = VillagerNames.rollHumanName(skinSeed, body, takenFirstNames);
        VillagerData victimData = new VillagerData(VillagerData.RACE_HUMAN, name[0], name[1], skinSeed);
        victimData.setQuestVariant(variant.name());
        store.putComponent(victimRef, VillagerData.getComponentType(), victimData);

        UUID victimUuid = NpcManager.extractUuid(store, victimRef);
        if (victimUuid != null) {
            long chunkIndex = NpcManager.chunkIndexFor(pos);
            NpcRegistry.NpcRecord record = new NpcRegistry.NpcRecord(
                    victimUuid, VARIANT_VICTIM_ROLE, NpcRegistry.InteractionType.RESCUE, skinSeed, chunkIndex);
            NpcRegistry.get().register(record);
            HearthboundDataStore.get().save();
            NpcRestorer.restore(victimRef, store, world, record);
        }
    }

    private static void spawnEnemy(Store<EntityStore> store, Vector3d pos, String role) {
        Pair<Ref<EntityStore>, INonPlayerCharacter> result
                = NpcManager.spawnNpc(store, pos, new Vector3f(0, 0, 0), role);
        if (result == null) {
            LOGGER.warning("Failed to spawn enemy (" + role + ") at " + pos);
            return;
        }
        Ref<EntityStore> enemyRef = result.first();
        activeNpcRefs.add(enemyRef);
        UUID enemyUuid = NpcManager.extractUuid(store, enemyRef);
        if (enemyUuid != null) {
            long chunkIndex = NpcManager.chunkIndexFor(pos);
            NpcRegistry.get().register(new NpcRegistry.NpcRecord(
                    enemyUuid, role, NpcRegistry.InteractionType.NONE, 0L, chunkIndex));
            HearthboundDataStore.get().save();
        }
    }

    private static String[] enemyRolesFor(QuestVariant variant) {
        return switch (variant) {
            case CABIN ->
                CABIN_ENEMIES;
            case RUINS ->
                RUINS_ENEMIES;
            case CAMP ->
                CAMP_ENEMIES;
            default ->
                new String[]{};
        };
    }

    // -------------------------------------------------------------------------
    // Objective markers
    // -------------------------------------------------------------------------
    private static void spawnQuestMarker(Store<EntityStore> store, Vector3d pos,
            Ref<EntityStore> playerRef, QuestVariant variant) {
        String markerName = switch (variant) {
            case CABIN ->
                "RescueTrap_Marker_Cabin";
            case RUINS ->
                "RescueTrap_Marker_Ruins";
            case CAMP ->
                "RescueTrap_Marker_Camp";
            default ->
                "RescueTrap_Marker";
        };
        spawnNamedMarker(store, pos, playerRef, markerName);
    }

    public static void spawnReturnMarker(Store<EntityStore> store, Vector3d pos,
            Ref<EntityStore> playerRef) {
        spawnNamedMarker(store, pos, playerRef, "Village_Return_Marker");
    }

    private static void spawnNamedMarker(Store<EntityStore> store, Vector3d pos,
            Ref<EntityStore> playerRef, String markerName) {
        TransformComponent playerTransform = store.getComponent(playerRef, TransformComponent.getComponentType());
        var rotation = playerTransform != null ? playerTransform.getRotation() : new Vector3f(0, 0, 0);

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(ReachLocationMarker.getComponentType(), new ReachLocationMarker(markerName));
        var model = ObjectivePlugin.get().getObjectiveLocationMarkerModel();
        holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
        holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(pos, rotation));
        holder.ensureComponent(UUIDComponent.getComponentType());
        holder.ensureComponent(Intangible.getComponentType());
        holder.ensureComponent(HiddenFromAdventurePlayers.getComponentType());
        store.addEntity(holder, AddReason.SPAWN);
    }

    private static boolean markerIconApplied = false;

    public static void applyMarkerIconOnce() {
        if (markerIconApplied) {
            return;
        }
        ReachLocationTask.MARKER_ICON = "UserF.png";
        markerIconApplied = true;
    }

    // -------------------------------------------------------------------------
    // Spawn location search
    // -------------------------------------------------------------------------
    // Score category bases — gaps are large enough that no intra-category penalty
    // can cross a category boundary (max intra-category spread ≈ 250).
    private static final int SCORE_BASE_NORMAL = 10_000; // normal biome, no water
    private static final int SCORE_BASE_RESTRICTED = -10_000; // Zone2/Zone4, no water → force RUINS
    private static final int SCORE_BASE_WATER = -1_000_000; // any water in footprint

    // Restricted biome environment ID prefixes
    private static final String[] RESTRICTED_ENV_PREFIXES = {
        "Env_Zone2_", "Env_Zone4_", "Env_Zone1_Shores"
    };

    /**
     * Returns a 4-element array: [blockX, groundY, blockZ, variantOrdinal].
     * variantOrdinal may differ from the requested variant when the best
     * candidate is in a restricted biome and RUINS is forced instead.
     */
    private static CompletableFuture<int[]> findSpawnLocation(World world, Vector3d fromPos,
            ThreadLocalRandom rng,
            QuestVariant variant) {
        // Phase 1: generate all candidate positions up-front.
        int[][] positions = new int[MAX_SPAWN_ATTEMPTS][2];
        for (int i = 0; i < MAX_SPAWN_ATTEMPTS; i++) {
            double angle = rng.nextDouble(Math.PI * 2);
            double distance = rng.nextDouble(MIN_DISTANCE, MAX_DISTANCE);
            positions[i][0] = (int) Math.floor(fromPos.getX() + Math.cos(angle) * distance);
            positions[i][1] = (int) Math.floor(fromPos.getZ() + Math.sin(angle) * distance);
        }

        // Phase 2: collect all unique chunk indices that need loading.
        Set<Long> chunkIndices = new HashSet<>();
        for (int[] pos : positions) {
            int cx = ChunkUtil.chunkCoordinate(pos[0]);
            int cz = ChunkUtil.chunkCoordinate(pos[1]);
            for (int dcx = cx - 1; dcx <= cx + 1; dcx++) {
                for (int dcz = cz - 1; dcz <= cz + 1; dcz++) {
                    chunkIndices.add(ChunkUtil.indexChunk(dcx, dcz));
                }
            }
        }

        List<CompletableFuture<?>> loads = new ArrayList<>();
        for (long idx : chunkIndices) {
            loads.add(world.getChunkAsync(idx));
        }

        return CompletableFuture.allOf(loads.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    int[] bestCandidate = null;
                    int bestScore = Integer.MIN_VALUE;

                    for (int[] pos : positions) {
                        int blockX = pos[0], blockZ = pos[1];
                        int[] result = scoreCandidate(world, fromPos, blockX, blockZ, variant);
                        if (result == null) {
                            continue;
                        }
                        int score = result[3];
                        if (score > bestScore) {
                            bestScore = score;
                            bestCandidate = result;
                        }
                    }

                    if (bestCandidate != null) {
                        QuestVariant chosen = QuestVariant.values()[bestCandidate[4]];
                        LOGGER.info("findSpawnLocation: best score=" + bestScore
                                + " variant=" + chosen
                                + " at (" + bestCandidate[0] + "," + bestCandidate[1] + "," + bestCandidate[2] + ")");
                    } else {
                        LOGGER.warning("findSpawnLocation: no candidate found in " + MAX_SPAWN_ATTEMPTS + " attempts");
                    }
                    return bestCandidate;
                });
    }

    /**
     * Evaluates a single candidate position and returns a 5-element array:
     * [blockX, groundY, blockZ, score, variantOrdinal], or null if the chunks
     * needed for evaluation are not loaded.
     *
     * Score categories (non-overlapping by design): SCORE_BASE_NORMAL (+10000):
     * normal biome, no water SCORE_BASE_RESTRICTED (-10000): restricted biome
     * (Zone2/Zone4), no water — forces RUINS SCORE_BASE_WATER (-1000000): any
     * water in footprint perimeter
     *
     * Within each category: base - maxPairDiff*10 - (hasTree ? 50 : 0)
     * Guarantees: the worst normal score (≈9750) > best restricted score
     * (≈-10000) the worst restricted score (≈-10250) > best water score
     * (≈-1000000)
     */
    private static int[] scoreCandidate(World world, Vector3d fromPos,
            int blockX, int blockZ, QuestVariant requestedVariant) {
        // TRAP: single center point, no footprint/biome scoring
        if (requestedVariant == QuestVariant.TRAP) {
            WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(blockX, blockZ));
            if (chunk == null) {
                return null;
            }
            int topY = chunk.getHeight(blockX & 31, blockZ & 31);
            int groundY = solidGroundAt(world, blockX, topY, blockZ);
            if (groundY == -1) {
                return null;
            }
            int score = SCORE_BASE_NORMAL - (topY - groundY > MAX_TREE_HEIGHT ? 50 : 0);
            return new int[]{blockX, groundY, blockZ, score, requestedVariant.ordinal()};
        }

        int[] fe = footprintExtent(requestedVariant);
        int feMinX = fe[0], feMaxX = fe[1], feMinZ = fe[2], feMaxZ = fe[3];

        // Full perimeter edges + center cross (same as before)
        List<int[]> scanPoints = new ArrayList<>();
        for (int dx = feMinX; dx <= feMaxX; dx++) {
            scanPoints.add(new int[]{dx, feMinZ});
            scanPoints.add(new int[]{dx, feMaxZ});
        }
        for (int dz = feMinZ + 1; dz <= feMaxZ - 1; dz++) {
            scanPoints.add(new int[]{feMinX, dz});
            scanPoints.add(new int[]{feMaxX, dz});
        }
        for (int dx = feMinX; dx <= feMaxX; dx++) {
            scanPoints.add(new int[]{dx, 0});
        }
        for (int dz = feMinZ; dz <= feMaxZ; dz++) {
            scanPoints.add(new int[]{0, dz});
        }

        int[][] cornerOffsets = {
            {feMinX, feMinZ}, {feMinX, feMaxZ}, {feMaxX, feMinZ}, {feMaxX, feMaxZ}
        };
        int[] cornerGroundY = new int[4];
        boolean hasWater = false;
        boolean hasTree = false;

        for (int[] pt : scanPoints) {
            int sx = blockX + pt[0], sz = blockZ + pt[1];
            WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(sx, sz));
            if (chunk == null) {
                return null; // chunk not loaded — skip whole candidate

                        }int topY = chunk.getHeight(sx & 31, sz & 31);
            int groundY = solidGroundAt(world, sx, topY, sz);
            if (groundY == -1) {
                hasWater = true;
                continue;
            }
            if (topY - groundY > MAX_TREE_HEIGHT) {
                hasTree = true;
            }
            for (int ci = 0; ci < 4; ci++) {
                if (pt[0] == cornerOffsets[ci][0] && pt[1] == cornerOffsets[ci][1]) {
                    cornerGroundY[ci] = groundY;
                    break;
                }
            }
        }

        int maxPairDiff = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = i + 1; j < 4; j++) {
                maxPairDiff = Math.max(maxPairDiff, Math.abs(cornerGroundY[i] - cornerGroundY[j]));
            }
        }
        int minCornerY = Math.min(Math.min(cornerGroundY[0], cornerGroundY[1]),
                Math.min(cornerGroundY[2], cornerGroundY[3]));

        // Biome check at center block, at ground level
        boolean restricted = isRestrictedBiome(world, blockX, minCornerY, blockZ);
        QuestVariant chosenVariant = (restricted && requestedVariant != QuestVariant.RUINS)
                ? QuestVariant.RUINS : requestedVariant;

        int scoreBase = hasWater ? SCORE_BASE_WATER
                : restricted ? SCORE_BASE_RESTRICTED
                        : SCORE_BASE_NORMAL;
        int score = scoreBase - maxPairDiff * 10 - (hasTree ? 50 : 0);

        return new int[]{blockX, minCornerY, blockZ, score, chosenVariant.ordinal()};
    }

    private static boolean isRestrictedBiome(World world, int blockX, int blockY, int blockZ) {
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(blockX, blockZ));
        if (chunk == null) {
            return false;
        }
        try {
            int envId = chunk.getBlockChunk().getEnvironment(blockX & 31, blockY, blockZ & 31);
            Environment env = (Environment) Environment.getAssetMap().getAsset(envId);
            if (env == null) {
                return false;
            }
            String envId2 = env.getId();
            if (envId2 == null) {
                return false;
            }
            for (String prefix : RESTRICTED_ENV_PREFIXES) {
                if (envId2.startsWith(prefix)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    // Returns [minX, maxX, minZ, maxZ] offsets from anchor for the given variant.
    private static int[] footprintExtent(QuestVariant variant) {
        return switch (variant) {
            case CABIN ->
                CABIN_EXTENT;
            case RUINS ->
                RUINS_EXTENT;
            case CAMP ->
                CAMP_EXTENT;
            default ->
                new int[]{0, 0, 0, 0};
        };
    }

    // -------------------------------------------------------------------------
    // Terrain editing helpers
    // -------------------------------------------------------------------------
    /**
     * Flood-fill removes all vegetation connected to the footprint interior,
     * including tree crowns floating above the prefab and partial trunks
     * outside its edges.
     *
     * Seed: every vegetation block at groundY+1 inside the footprint. Spread:
     * 6-connected neighbours within [centerX±(halfX+15), groundY..groundY+40,
     * centerZ±(halfZ+15)]. This removes entire trees whose trunks overlap the
     * footprint, including their overhanging crowns, without touching terrain
     * or buildings outside that band.
     */
    // ext = [minX, maxX, minZ, maxZ] offsets from anchor
    private static void clearVegetation(World world, int centerX, int groundY, int centerZ,
            int[] ext) {
        // BFS bbox: large enough to contain crowns of any tree whose trunk was seeded.
        int minX = centerX + ext[0] - 60;
        int maxX = centerX + ext[1] + 60;
        int minY = groundY;
        int maxY = groundY + 60;
        int minZ = centerZ + ext[2] - 60;
        int maxZ = centerZ + ext[3] + 60;

        Set<Long> visited = new HashSet<>();
        java.util.ArrayDeque<long[]> queue = new java.util.ArrayDeque<>();

        // Seed zone: footprint + BLEND_RADIUS so trunks in the blend zone (where terrain is
        // also levelled) are caught and their entire crown removed, even if it extends far outside.
        int seedMargin = BLEND_RADIUS;
        for (int dx = ext[0] - seedMargin; dx <= ext[1] + seedMargin; dx++) {
            for (int dz = ext[2] - seedMargin; dz <= ext[3] + seedMargin; dz++) {
                int wx = centerX + dx;
                int wz = centerZ + dz;
                for (int wy = groundY + 1; wy <= groundY + 3; wy++) {
                    var bt = world.getBlockType(wx, wy, wz);
                    if (bt == null) {
                        continue;
                    }
                    String id = bt.getId();
                    if (id != null && isVegetation(id)) {
                        long key = packXYZ(wx, wy, wz);
                        if (visited.add(key)) {
                            queue.add(new long[]{wx, wy, wz});
                        }
                    }
                }
            }
        }

        int[] dx6 = {1, -1, 0, 0, 0, 0};
        int[] dy6 = {0, 0, 1, -1, 0, 0};
        int[] dz6 = {0, 0, 0, 0, 1, -1};

        while (!queue.isEmpty()) {
            long[] pos = queue.poll();
            int x = (int) pos[0], y = (int) pos[1], z = (int) pos[2];
            world.breakBlock(x, y, z, 0);

            for (int d = 0; d < 6; d++) {
                int nx = x + dx6[d], ny = y + dy6[d], nz = z + dz6[d];
                if (nx < minX || nx > maxX || ny < minY || ny > maxY || nz < minZ || nz > maxZ) {
                    continue;
                }
                long key = packXYZ(nx, ny, nz);
                if (!visited.add(key)) {
                    continue;
                }
                var bt = world.getBlockType(nx, ny, nz);
                if (bt == null) {
                    continue;
                }
                String id = bt.getId();
                if (id != null && isVegetation(id)) {
                    queue.add(new long[]{nx, ny, nz});
                }
            }
        }
    }

    private static long packXYZ(int x, int y, int z) {
        // Pack into a single long: x and z fit in 26 bits each (±33M blocks), y in 12 bits (4096)
        return ((long) (x + 33_554_432) << 38) | ((long) (z + 33_554_432) << 12) | (y & 0xFFF);
    }

    private static boolean isVegetation(String id) {
        return id.startsWith("Plant_")
                || id.startsWith("Deco_Nest")
                || (id.startsWith("Wood_") && (id.contains("_Trunk") || id.contains("_Leaves")
                || id.contains("_Branch") || id.contains("_Log") || id.contains("_Beam")
                || id.contains("_Roots")))
                || id.startsWith("Filter_");
    }

    // Maximum Branch/Leaves hops from a grounded trunk before a block is considered floating.
    private static final int MAX_LEAF_HANG = 10;

    // Outside footprint: Dijkstra-BFS from grounded vegetation (sitting on Soil/Rock).
    // Trunk/Log = 0 cost, Branch/Leaves = 1 cost per hop. Unreachable within MAX_LEAF_HANG removed.
    // Inside footprint (above roof): simple top-down air-below check for stray terrain blocks.
    private static void clearFloating(World world, int centerX, int groundY, int centerZ,
            int[] ext) {
        int r = BLEND_RADIUS;
        int roofWorldY = groundY + ext[4];

        // --- Inside footprint + blend zone: bottom-up pass for stray terrain above the roof ---
        // Extended to footprint±BLEND_RADIUS and +20 blocks above roof.
        // Bottom-up order: once the lowest floating block is removed, the block above it
        // also has air below on the next iteration — entire stacks collapse in one pass.
        for (int ox = ext[0] - r; ox <= ext[1] + r; ox++) {
            for (int oz = ext[2] - r; oz <= ext[3] + r; oz++) {
                int wx = centerX + ox, wz = centerZ + oz;
                for (int wy = roofWorldY; wy <= roofWorldY + 20; wy++) {
                    var bt = world.getBlockType(wx, wy, wz);
                    if (bt == null) {
                        continue;
                    }
                    String id = bt.getId();
                    if (id == null || id.startsWith("Empty") || id.startsWith("Air") || id.startsWith("Filter_")) {
                        continue;
                    }
                    var below = world.getBlockType(wx, wy - 1, wz);
                    String belowId = below != null ? below.getId() : null;
                    boolean belowAir = belowId == null
                            || belowId.startsWith("Empty") || belowId.startsWith("Air") || belowId.startsWith("Filter_");
                    if (belowAir) {
                        world.breakBlock(wx, wy, wz, 0);
                    }
                }
            }
        }

        // --- Outside footprint: Dijkstra-BFS grounded-component check ---
        // deleteZone: where we actually remove floating blocks (footprint perimeter + blend radius).
        // collectZone: wider area used to build the BFS graph, so trees whose trunks sit just
        //   outside deleteZone are still reachable seeds and don't get their crowns falsely cut.
        int delMinX = centerX + ext[0] - r, delMaxX = centerX + ext[1] + r;
        int delMinZ = centerZ + ext[2] - r, delMaxZ = centerZ + ext[3] + r;
        int extra = 12; // extra BFS context radius beyond deleteZone
        int colMinX = delMinX - extra, colMaxX = delMaxX + extra;
        int colMinZ = delMinZ - extra, colMaxZ = delMaxZ + extra;
        int scanMinY = groundY - 8;
        int scanMaxY = groundY + 40;

        // Collect all vegetation blocks in the wider collectZone
        java.util.HashMap<Long, String> candidates = new java.util.HashMap<>();
        for (int wx = colMinX; wx <= colMaxX; wx++) {
            for (int wz = colMinZ; wz <= colMaxZ; wz++) {
                int ox = wx - centerX, oz = wz - centerZ;
                if (ox >= ext[0] && ox <= ext[1] && oz >= ext[2] && oz <= ext[3]) {
                    continue;
                }
                for (int wy = scanMinY; wy <= scanMaxY; wy++) {
                    var bt = world.getBlockType(wx, wy, wz);
                    if (bt == null) {
                        continue;
                    }
                    String id = bt.getId();
                    if (id != null && isVegetation(id)) {
                        candidates.put(packXYZ(wx, wy, wz), id);
                    }
                }
            }
        }

        // Dijkstra-BFS: best distance found per block
        java.util.HashMap<Long, Integer> bestDist = new java.util.HashMap<>();
        java.util.PriorityQueue<int[]> pq = new java.util.PriorityQueue<>(
                java.util.Comparator.comparingInt(a -> a[0]));

        // Seed: vegetation whose block directly below is Soil_*/Rock_* → distance 0
        for (long key : candidates.keySet()) {
            int[] xyz = unpackXYZ(key);
            var below = world.getBlockType(xyz[0], xyz[1] - 1, xyz[2]);
            String belowId = below != null ? below.getId() : null;
            if (belowId != null && (belowId.startsWith("Soil_") || belowId.startsWith("Rock_"))) {
                bestDist.put(key, 0);
                pq.offer(new int[]{0, xyz[0], xyz[1], xyz[2]});
            }
        }

        int[] dx6 = {1, -1, 0, 0, 0, 0};
        int[] dy6 = {0, 0, 1, -1, 0, 0};
        int[] dz6 = {0, 0, 0, 0, 1, -1};

        while (!pq.isEmpty()) {
            int[] cur = pq.poll();
            int dist = cur[0], wx = cur[1], wy = cur[2], wz = cur[3];
            long key = packXYZ(wx, wy, wz);

            Integer recorded = bestDist.get(key);
            if (recorded != null && dist > recorded) {
                continue; // stale entry
            }
            for (int d = 0; d < 6; d++) {
                int nx = wx + dx6[d], ny = wy + dy6[d], nz = wz + dz6[d];
                long nkey = packXYZ(nx, ny, nz);
                String nid = candidates.get(nkey);
                if (nid == null) {
                    continue;
                }

                int step = isTrunkOrLog(nid) ? 0 : 1;
                int newDist = dist + step;
                if (newDist > MAX_LEAF_HANG) {
                    continue;
                }

                Integer prev = bestDist.get(nkey);
                if (prev == null || newDist < prev) {
                    bestDist.put(nkey, newDist);
                    pq.offer(new int[]{newDist, nx, ny, nz});
                }
            }
        }

        // Remove floating vegetation — only within deleteZone, not the wider collectZone
        for (long key : candidates.keySet()) {
            if (bestDist.containsKey(key)) {
                continue;
            }
            int[] xyz = unpackXYZ(key);
            int wx = xyz[0], wy = xyz[1], wz = xyz[2];
            if (wx < delMinX || wx > delMaxX || wz < delMinZ || wz > delMaxZ) {
                continue;
            }
            world.breakBlock(wx, wy, wz, 0);
        }

        // Second pass: catch crowns of trees whose trunks were inside deleteZone (and thus
        // removed), but whose canopy extends beyond collectZone and was never scanned.
        // Strategy: flood-fill from grounded vegetation OUTSIDE deleteZone. Any vegetation
        // block in the extended zone that is not reachable through blocks outside deleteZone
        // is a detached fragment — remove it.
        int ext2 = 32; // extra reach beyond deleteZone for orphan-crown detection
        int ext2MinX = delMinX - ext2, ext2MaxX = delMaxX + ext2;
        int ext2MinZ = delMinZ - ext2, ext2MaxZ = delMaxZ + ext2;

        java.util.HashMap<Long, String> ext2Candidates = new java.util.HashMap<>();
        for (int wx = ext2MinX; wx <= ext2MaxX; wx++) {
            for (int wz = ext2MinZ; wz <= ext2MaxZ; wz++) {
                // Skip interior of deleteZone — those blocks were already handled
                if (wx >= delMinX && wx <= delMaxX && wz >= delMinZ && wz <= delMaxZ) {
                    continue;
                }
                for (int wy = scanMinY; wy <= scanMaxY; wy++) {
                    var bt = world.getBlockType(wx, wy, wz);
                    if (bt == null) {
                        continue;
                    }
                    String id = bt.getId();
                    if (id != null && isVegetation(id)) {
                        ext2Candidates.put(packXYZ(wx, wy, wz), id);
                    }
                }
            }
        }

        // Seed: grounded vegetation outside deleteZone
        java.util.HashMap<Long, Integer> ext2Dist = new java.util.HashMap<>();
        java.util.PriorityQueue<int[]> ext2Pq = new java.util.PriorityQueue<>(
                java.util.Comparator.comparingInt(a -> a[0]));
        for (long key : ext2Candidates.keySet()) {
            int[] xyz = unpackXYZ(key);
            var below = world.getBlockType(xyz[0], xyz[1] - 1, xyz[2]);
            String belowId = below != null ? below.getId() : null;
            if (belowId != null && (belowId.startsWith("Soil_") || belowId.startsWith("Rock_"))) {
                ext2Dist.put(key, 0);
                ext2Pq.offer(new int[]{0, xyz[0], xyz[1], xyz[2]});
            }
        }

        // BFS only through blocks outside deleteZone
        while (!ext2Pq.isEmpty()) {
            int[] cur = ext2Pq.poll();
            int dist = cur[0], wx = cur[1], wy = cur[2], wz = cur[3];
            long key = packXYZ(wx, wy, wz);
            Integer recorded = ext2Dist.get(key);
            if (recorded != null && dist > recorded) {
                continue;
            }

            for (int d = 0; d < 6; d++) {
                int nx = wx + dx6[d], ny = wy + dy6[d], nz = wz + dz6[d];
                // Do not traverse through deleteZone — it's a severed region
                if (nx >= delMinX && nx <= delMaxX && nz >= delMinZ && nz <= delMaxZ) {
                    continue;
                }
                long nkey = packXYZ(nx, ny, nz);
                String nid = ext2Candidates.get(nkey);
                if (nid == null) {
                    continue;
                }
                int step = isTrunkOrLog(nid) ? 0 : 1;
                int newDist = dist + step;
                if (newDist > MAX_LEAF_HANG) {
                    continue;
                }
                Integer prev = ext2Dist.get(nkey);
                if (prev == null || newDist < prev) {
                    ext2Dist.put(nkey, newDist);
                    ext2Pq.offer(new int[]{newDist, nx, ny, nz});
                }
            }
        }

        // Remove unreachable fragments in the extended zone (excluding deleteZone interior)
        for (long key : ext2Candidates.keySet()) {
            if (ext2Dist.containsKey(key)) {
                continue;
            }
            int[] xyz = unpackXYZ(key);
            world.breakBlock(xyz[0], xyz[1], xyz[2], 0);
        }
    }

    private static boolean isTrunkOrLog(String id) {
        return id.contains("_Trunk") || id.contains("_Log") || id.contains("_Beam");
    }

    private static int[] unpackXYZ(long key) {
        int x = (int) ((key >> 38) - 33_554_432);
        int z = (int) (((key >> 12) & 0x3FFFFFFL) - 33_554_432);
        int y = (int) (key & 0xFFF);
        return new int[]{x, y, z};
    }

    // Low-frequency smooth noise in [-1.5, +1.5] blocks for terrain blend irregularity.
    // Bilinear interpolation between a coarse grid of hash values (cell size = NOISE_CELL).
    // Neighbouring columns get similar values → smooth waves, not per-block randomness.
    private static final int NOISE_CELL = 6;

    private static double blendNoise(int wx, int wz) {
        int gx = Math.floorDiv(wx, NOISE_CELL);
        int gz = Math.floorDiv(wz, NOISE_CELL);
        double fx = (wx - gx * NOISE_CELL) / (double) NOISE_CELL; // 0..1 within cell
        double fz = (wz - gz * NOISE_CELL) / (double) NOISE_CELL;

        double v00 = hashToFloat(gx, gz);
        double v10 = hashToFloat(gx + 1, gz);
        double v01 = hashToFloat(gx, gz + 1);
        double v11 = hashToFloat(gx + 1, gz + 1);

        // Smoothstep on both axes for even softer transitions
        double sx = fx * fx * (3 - 2 * fx);
        double sz = fz * fz * (3 - 2 * fz);

        double v = v00 * (1 - sx) * (1 - sz) + v10 * sx * (1 - sz) + v01 * (1 - sx) * sz + v11 * sx * sz;
        return (v - 0.5) * 3.0; // map [0,1] → [-1.5, +1.5]
    }

    private static final String[] GRASS_DECOR = {
        "Plant_Grass_Sharp", "Plant_Grass_Sharp_Short", "Plant_Grass_Sharp_Tall"
    };

    private static void maybeSpawnGrassDecor(World world, int wx, int wy, int wz) {
        int h = wx * 1000003 + wz * 999983;
        h = (h ^ (h >> 13)) * 1274126177;
        h = h ^ (h >> 16);
        int roll = h & 0xFF; // 0..255
        if (roll >= 60) {
            return; // ~23% chance

                }String plant = GRASS_DECOR[((h >> 8) & 0xFF) % GRASS_DECOR.length];
        // Only place if the target cell is air
        var existing = world.getBlockType(wx, wy, wz);
        String existId = existing != null ? existing.getId() : null;
        if (existId != null && !existId.startsWith("Empty") && !existId.startsWith("Air") && !existId.startsWith("Filter_")) {
            return;
        }
        world.setBlock(wx, wy, wz, plant);
    }

    // After blendTerrain+clearFloating, scans the blend zone outside the footprint and places
    // grass plants on bare Soil_Grass/Soil_Grass_Full columns where the cell above is empty.
    private static void restoreGrassDecor(World world, int centerX, int groundY, int centerZ,
            int[] ext) {
        int r = BLEND_RADIUS;
        for (int ox = ext[0] - r; ox <= ext[1] + r; ox++) {
            for (int oz = ext[2] - r; oz <= ext[3] + r; oz++) {
                if (ox >= ext[0] && ox <= ext[1] && oz >= ext[2] && oz <= ext[3]) {
                    continue;
                }
                int wx = centerX + ox;
                int wz = centerZ + oz;
                // Find topmost solid block via block scan (getHeight may be stale)
                int surfY = solidGroundByBlockScan(world, wx, groundY, wz);
                if (surfY == -1) {
                    continue;
                }
                var surf = world.getBlockType(wx, surfY, wz);
                if (surf == null) {
                    continue;
                }
                String surfId = surf.getId();
                if (surfId == null || !surfId.startsWith("Soil_Grass")) {
                    continue;
                }
                maybeSpawnGrassDecor(world, wx, surfY + 1, wz);
            }
        }
    }

    private static double hashToFloat(int gx, int gz) {
        int h = gx * 374761393 + gz * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        h = h ^ (h >> 16);
        return (h & 0xFFFF) / 65535.0; // [0, 1]
    }

    // Scans blocks via getBlockType (bypasses stale getHeight cache).
    // Returns the topmost Soil/Rock/Gravel/Sand block near groundY, or -1 if not found.
    private static int solidGroundByBlockScan(World world, int x, int groundY, int z) {
        for (int y = groundY + 4; y >= groundY - 4; y--) {
            var bt = world.getBlockType(x, y, z);
            if (bt == null) {
                continue;
            }
            String id = bt.getId();
            if (id == null) {
                continue;
            }
            if (id.startsWith("Soil_") || id.startsWith("Rock_")
                    || id.startsWith("Gravel") || id.startsWith("Sand")) {
                return y;
            }
        }
        return -1;
    }

    /**
     * Walks down from topY+3 to find solid ground (Soil_*, Rock_*, Gravel*,
     * Sand*), skipping vegetation and tree roots.
     *
     * Uses {@code chunk.getFluidId()} to check fluid cells directly — more
     * reliable than getBlockType() which misses ocean floor where the height
     * map points to solid ground below the water column. Returns -1 if any
     * fluid is present (underwater column). Returns topY if no ground found
     * within 30 blocks.
     */
    private static int solidGroundAt(World world, int x, int topY, int z) {
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));

        for (int y = topY + 3; y >= topY - 30; y--) {
            // Check fluid layer first — this is what SharedStructures uses for water detection
            if (chunk != null) {
                try {
                    int fluidId = chunk.getFluidId(x, y, z);
                    if (fluidId != Integer.MIN_VALUE && fluidId != 0) {
                        return -1;
                    }
                } catch (Throwable ignored) {
                }
            }

            var bt = world.getBlockType(x, y, z);
            if (bt == null) {
                continue;
            }
            String id = bt.getId();
            if (id == null) {
                continue;
            }
            if (id.startsWith("Soil_") || id.startsWith("Rock_")
                    || id.startsWith("Gravel") || id.startsWith("Sand")) {
                return y;
            }
        }
        return topY;
    }

    // Total blend radius in blocks outside the footprint
    private static final int BLEND_RADIUS = 8;

    /**
     * Blends terrain around the prefab footprint.
     *
     * Algorithm: 1. Snapshot all natural heights in the blend zone BEFORE any
     * edits. This avoids the stale-getHeight bug and lets inside/outside use
     * the same ground truth. 2. Outside pass (SDF): for each column outside the
     * footprint within BLEND_RADIUS, interpolate height from groundY (at
     * dist=0) to naturalY (at dist=BLEND_RADIUS). SDF of rectangle gives
     * elliptic isolines at corners — no sharp rectangular edges. 3. Inside
     * corners pass: for each of the 4 corners, a quarter-circle zone of radius
     * BLEND_RADIUS carved from groundY toward the snapshotted outside height.
     * Only columns where the prefab placed Soil_* are touched (walls are
     * skipped).
     */
    // ext = [minX, maxX, minZ, maxZ] offsets from anchor
    private static void blendTerrain(World world, int centerX, int groundY, int centerZ,
            int[] ext) {
        int r = BLEND_RADIUS;
        // Snapshot spans the full asymmetric extent + blend radius on each side
        int snapMinX = ext[0] - r;
        int snapMaxX = ext[1] + r;
        int snapMinZ = ext[2] - r;
        int snapMaxZ = ext[3] + r;
        int snapshotW = snapMaxX - snapMinX + 1;
        int snapshotH = snapMaxZ - snapMinZ + 1;
        int[] snapshot = new int[snapshotW * snapshotH];

        for (int ox = snapMinX; ox <= snapMaxX; ox++) {
            for (int oz = snapMinZ; oz <= snapMaxZ; oz++) {
                int wx = centerX + ox;
                int wz = centerZ + oz;
                var chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(wx, wz));
                int topY = chunk != null ? chunk.getHeight(wx & 31, wz & 31) : groundY;
                int naturalY = solidGroundAt(world, wx, topY, wz);
                snapshot[(oz - snapMinZ) * snapshotW + (ox - snapMinX)] = (naturalY == -1) ? groundY : naturalY;
            }
        }

        // Step 2: outside SDF blend — SDF of asymmetric rectangle
        for (int ox = snapMinX; ox <= snapMaxX; ox++) {
            for (int oz = snapMinZ; oz <= snapMaxZ; oz++) {
                // SDF of the prefab rectangle: positive outside, 0 on boundary, negative inside
                double ddx = ox < ext[0] ? ext[0] - ox : (ox > ext[1] ? ox - ext[1] : 0);
                double ddz = oz < ext[2] ? ext[2] - oz : (oz > ext[3] ? oz - ext[3] : 0);
                double dist = Math.sqrt(ddx * ddx + ddz * ddz);
                if (dist <= 0 || dist > r) {
                    continue;
                }

                int wx = centerX + ox;
                int wz = centerZ + oz;
                int naturalY = snapshot[(oz - snapMinZ) * snapshotW + (ox - snapMinX)];
                if (naturalY == groundY) {
                    continue;
                }

                // Skip columns that drop far below groundY — caves, cliffs, overhangs.
                // Filling them would build a solid wall from cave floor to surface.
                if (groundY - naturalY > 5) {
                    continue;
                }

                double noise = blendNoise(wx, wz) * (dist / r);
                int targetY = (int) Math.round(groundY + (naturalY - groundY) * dist / r + noise);
                if (targetY == naturalY) {
                    continue;
                }

                if (targetY > naturalY) {
                    for (int fy = naturalY + 1; fy < targetY; fy++) {
                        world.setBlock(wx, fy, wz, "Soil_Dirt");
                    }
                    world.setBlock(wx, targetY, wz, "Soil_Grass");
                } else {
                    for (int wy = naturalY + 3; wy > targetY; wy--) {
                        world.breakBlock(wx, wy, wz, 0);
                    }
                    var top = world.getBlockType(wx, targetY, wz);
                    if (top != null) {
                        String topId = top.getId();
                        if (topId != null && shouldReplaceWithGrass(topId)) {
                            world.setBlock(wx, targetY, wz, "Soil_Grass");
                        }
                    }
                }
            }
        }

        // Step 3: inside corners — quarter-circle carve at each actual corner of the prefab rectangle
        int[][] corners = {{ext[0], ext[2]}, {ext[0], ext[3]}, {ext[1], ext[2]}, {ext[1], ext[3]}};
        int[][] cornerDirs = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}}; // outward direction per corner
        for (int ci = 0; ci < 4; ci++) {
            int cornerX = centerX + corners[ci][0];
            int cornerZ = centerZ + corners[ci][1];
            int[] cd = cornerDirs[ci];

            // Read current (post-SDF) heights for 3 diagonal points just outside the corner
            int outsideTotal = 0, outsideCount = 0;
            for (int d = 1; d <= 3; d++) {
                int sx = cornerX + cd[0] * d;
                int sz = cornerZ + cd[1] * d;
                int oy = solidGroundByBlockScan(world, sx, groundY, sz);
                if (oy != -1) {
                    outsideTotal += oy;
                    outsideCount++;
                }
            }
            if (outsideCount == 0) {
                continue;
            }
            int outsideY = (int) Math.round((double) outsideTotal / outsideCount);
            // Only carve if outside is lower than prefab floor — that's where the sharp step is.
            // If outside is higher or equal, outside SDF pass already handles the transition.
            if (outsideY >= groundY) {
                continue;
            }

            for (int i = 0; i <= r; i++) {
                for (int j = 0; j <= r; j++) {
                    double dist = Math.sqrt((double) i * i + j * j);
                    if (dist > r) {
                        continue;
                    }

                    int wx = cornerX - cd[0] * i;
                    int wz = cornerZ - cd[1] * j;

                    // Only touch columns where the prefab placed Soil_* (skip walls / structure)
                    var surfBlock = world.getBlockType(wx, groundY, wz);
                    if (surfBlock == null) {
                        continue;
                    }
                    String surfId = surfBlock.getId();
                    if (surfId == null || !surfId.startsWith("Soil_")) {
                        continue;
                    }

                    // dist=0 (corner vertex) → outsideY, dist=r (deep inside) → groundY
                    int targetY = (int) Math.round(outsideY + (groundY - outsideY) * dist / r);
                    if (targetY >= groundY) {
                        continue; // never add blocks above prefab floor
                    }
                    // Carve only — lower the corner toward outsideY
                    for (int wy = groundY; wy > targetY; wy--) {
                        world.breakBlock(wx, wy, wz, 0);
                    }
                    var top = world.getBlockType(wx, targetY, wz);
                    if (top != null) {
                        String topId = top.getId();
                        if (topId != null && shouldReplaceWithGrass(topId)) {
                            world.setBlock(wx, targetY, wz, "Soil_Grass");
                        }
                    }
                }
            }
        }
    }

    // Returns true for bare-dirt / generic rock surfaces that look wrong exposed — replace with grass.
    // Excludes Rock_Chalk and Rock_Marble (naturally white, fine on surface) and Soil_Clay_White.
    private static boolean shouldReplaceWithGrass(String blockId) {
        if (blockId.equals("Soil_Dirt")) {
            return true;
        }
        if (blockId.equals("Rock_Chalk") || blockId.equals("Rock_Marble")) {
            return false;
        }
        if (blockId.equals("Soil_Clay_White")) {
            return false;
        }
        if (blockId.startsWith("Rock_")) {
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Misc helpers
    // -------------------------------------------------------------------------
    private static Set<String> collectTakenFullNames(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        Set<String> taken = new HashSet<>();
        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village == null) {
            return taken;
        }
        for (VillagerSummary v : village.getVillagers()) {
            String full = v.getFullName();
            if (!full.isEmpty()) {
                taken.add(full);
            }
        }
        return taken;
    }

    // -------------------------------------------------------------------------
    // Chest loot
    // -------------------------------------------------------------------------
    // CAMP chest: local coords (x=-1, y=1, z=10), prefabSurfaceY=2
    // World coords: centerX-1, groundY-1, centerZ+10
    private static void fillCampChest(World world, int centerX, int groundY, int centerZ,
            ThreadLocalRandom rng) {
        int cx = centerX - 1;
        int cy = groundY - 1;
        int cz = centerZ + 10;
        try {
            WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(cx, cz));
            if (chunk == null) {
                return;
            }
            Ref<ChunkStore> blockRef = chunk.getBlockComponentEntity(cx, cy, cz);
            if (blockRef == null) {
                return;
            }
            ItemContainerBlock containerBlock = world.getChunkStore().getStore()
                    .getComponent(blockRef, BlockModule.get().getItemContainerBlockComponentType());
            if (containerBlock == null) {
                return;
            }
            ItemContainer inv = containerBlock.getItemContainer();
            if (inv == null) {
                return;
            }

            // Guaranteed loot
            addToChest(inv, "Food_Wildmeat_Raw", 1 + rng.nextInt(2));  // 1-2
            addToChest(inv, "Ingredient_Bone_Fragment", 2 + rng.nextInt(3));  // 2-4
            addToChest(inv, "Weapon_Arrow_Crude", 8 + rng.nextInt(9));  // 8-16

            // ~50% chance: copper bars (2-4)
            if (rng.nextBoolean()) {
                addToChest(inv, "Ingredient_Bar_Copper", 2 + rng.nextInt(3)); // 2-4
            }

            // ~50% chance: iron bars (1-2)
            if (rng.nextBoolean()) {
                addToChest(inv, "Ingredient_Bar_Iron", 1 + rng.nextInt(2)); // 1-2
            }

            // ~50% chance: gold ore (1-2)
            if (rng.nextBoolean()) {
                addToChest(inv, "Ore_Gold", 1 + rng.nextInt(2)); // 1-2
            }

            // ~25% chance: rusty weapon (axe or club)
            if (rng.nextInt(4) == 0) {
                String weapon = rng.nextBoolean() ? "Weapon_Axe_Iron_Rusty" : "Weapon_Club_Iron_Rusty";
                addToChest(inv, weapon, 1);
            }

        } catch (Exception e) {
            LOGGER.warning("fillCampChest: failed at " + cx + "," + cy + "," + cz + ": " + e.getMessage());
        }
    }

    private static void addToChest(ItemContainer inv, String itemId, int qty) {
        try {
            inv.addItemStack(new ItemStack(itemId, qty));
        } catch (Exception e) {
            LOGGER.fine("addToChest: failed for " + itemId + ": " + e.getMessage());
        }
    }

    private RescueQuestManager() {
    }

    public record Spawned(Vector3d victimPos, Vector3d guardPos) {

    }
}
