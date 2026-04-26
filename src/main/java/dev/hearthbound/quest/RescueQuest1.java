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
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HiddenFromAdventurePlayers;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.logging.Logger;

/** Spawns the "trap with cheese" rescue encounter: prefab pit + trapped victim + a goblin guard.
 *
 *  <p>The prefab's anchor sits at (0, 0, 0) inside the file. Grass/surface is at prefab Y=8,
 *  spikes at Y=1-2, cheese lure at Y=11. We place the prefab so its surface row lines up
 *  with the world's ground level at the target block column. */
public final class RescueQuest1 {

    private static final Logger LOGGER = Logger.getLogger(RescueQuest1.class.getName());

    public static final String PREFAB_NAME = "Villager_ResqueQuest_trap";
    public static final String VICTIM_ROLE = "Villager_Rescue_Trapped";
    public static final String GUARD_ROLE = "Goblin_Scavenger";
    private static final String OBJECTIVE_LINE_ID = "ObjectiveLine_RescueQuest_1";

    /** Prefab Y coordinate of the grass/surface row. */
    private static final int PREFAB_SURFACE_Y = 8;
    /** Victim spawn cell in prefab-local coords. (1, 2, 0) stands on the solid Rock_Stone
     *  floor at (1, 1, 0) with Empty above and no spike in that column — hand-picked from
     *  the prefab's block map. Victim role is also marked Invulnerable as a safety net. */
    private static final int VICTIM_DX = 1;
    private static final int VICTIM_PREFAB_Y = 2;
    private static final int VICTIM_DZ = 0;
    /** Guard offset from the pit center on the surface. Kept close enough that the player
     *  encounters the goblin before reaching the pit, but far enough from the 7x7 camouflage
     *  footprint (x,z in -3..3) that the goblin's wander is unlikely to drop it in. */
    private static final int GUARD_DX = 8;
    private static final int GUARD_DZ = 0;

    private static final double MIN_DISTANCE = 300.0;
    private static final double MAX_DISTANCE = 500.0;
    private static final int MAX_SPAWN_ATTEMPTS = 5;

    /** Block ID prefixes that mark a bad spawn location (water, void air, etc.). */
    private static final List<String> BAD_BLOCK_PREFIXES = List.of("Fluid", "Empty");

    /** All quest NPC refs (victim, guard, follower). Cleared on cleanup so /hb quest start
     *  can remove leftovers from the previous run before spawning a new encounter. */
    private static final List<Ref<EntityStore>> activeNpcRefs = new ArrayList<>();

    /** Registers a follower ref spawned by RescueDialogPage so cleanup can remove it. */
    public static void registerFollower(Ref<EntityStore> ref) {
        activeNpcRefs.add(ref);
    }

    /** Unregisters a follower ref (e.g. after converting to villager). */
    public static void unregisterNpc(Ref<EntityStore> ref) {
        activeNpcRefs.remove(ref);
    }

    /** Removes all tracked quest NPCs. Call before spawning a new encounter or on hardreset.
     *  NPCs in unloaded chunks are deferred via NpcRegistry.markForRemoval(). */
    public static void cleanup(Store<EntityStore> store) {
        for (Ref<EntityStore> ref : activeNpcRefs) {
            try {
                UUID uuid = NpcManager.extractUuid(store, ref);
                if (uuid != null) {
                    NpcRegistry.NpcRecord record = NpcRegistry.get().getRecord(uuid);
                    long chunkIndex = record != null ? record.chunkIndex : 0L;
                    NpcRegistry.get().unregister(uuid);
                    if (ref.isValid()) {
                        store.removeEntity(ref, RemoveReason.REMOVE);
                    } else {
                        // NPC is in an unloaded chunk — defer deletion to NpcChunkLoadHandler.
                        NpcRegistry.get().markForRemoval(uuid, chunkIndex);
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("cleanup: failed to remove NPC ref: " + e.getMessage());
            }
        }
        activeNpcRefs.clear();
        HearthboundDataStore.get().save();
    }

    /** Removes all ReachLocationMarker entities from the world. Stale markers confuse
     *  ReachLocationTask.setup0 which always picks the closest one to the player. */
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

    /** Cancels all active objectives for the player. */
    public static void cancelAllObjectives(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                           com.hypixel.hytale.server.core.entity.entities.Player player) {
        if (player == null) return;
        Set<UUID> active = player.getPlayerConfigData().getActiveObjectiveUUIDs();
        if (active == null || active.isEmpty()) return;
        for (UUID objectiveUuid : new java.util.HashSet<>(active)) {
            ObjectivePlugin.get().cancelObjective(objectiveUuid, store);
        }
    }

    private RescueQuest1() {}

    /**
     * Full quest start sequence: cancel active objectives, cleanup old NPCs, find a good
     * spawn location 300-500 blocks from {@code fromPos}, spawn the trap + NPCs, place the
     * objective marker, and start the ObjectiveLine.
     *
     * <p>All async work runs on the world thread. {@code onDone} is called with the spawned
     * positions on success, or with {@code null} on failure. May be called from any thread.
     *
     * @param playerUuid  UUID of the player (for objective registration)
     * @param fromPos     reference position — quest spawns 300-500 blocks from here
     * @param playerRef   player entity ref (for objective cancel + objective binding)
     * @param player      Player component (for objective cancel)
     * @param onDone      called on the world thread with Spawned result (null on failure)
     */
    public static void startForPlayer(World world, Store<EntityStore> store,
                                      Ref<EntityStore> playerRef,
                                      com.hypixel.hytale.server.core.entity.entities.Player player,
                                      UUID playerUuid,
                                      Vector3d fromPos,
                                      Consumer<Spawned> onDone) {
        applyMarkerIconOnce();

        // Kick off chunk search immediately (pure async, no store writes).
        // All store mutations (cancel objectives, cleanup, spawn) happen on the world thread
        // inside world.execute() to avoid touching the store from the event/UI thread.
        findSpawnLocation(world, fromPos, ThreadLocalRandom.current(), 0)
                .thenAccept(candidate -> world.execute(() -> {
                    Store<EntityStore> liveStore = world.getEntityStore().getStore();

                    // Read a fresh Player component for objective cancellation
                    com.hypixel.hytale.server.core.entity.entities.Player livePlayer =
                            liveStore.getComponent(playerRef,
                                    com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
                    cancelAllObjectives(liveStore, playerRef, livePlayer);
                    removeAllMarkers(liveStore);
                    cleanup(liveStore);

                    if (candidate == null) {
                        LOGGER.warning("startForPlayer: could not find valid spawn location after " + MAX_SPAWN_ATTEMPTS + " attempts");
                        onDone.accept(null);
                        return;
                    }

                    int blockX = candidate[0];
                    int groundY = candidate[1];
                    int blockZ = candidate[2];

                    Set<String> takenFirstNames = collectTakenFullNames(liveStore, playerRef);
                    Spawned spawned = RescueQuest1.spawn(world, liveStore, blockX, groundY, blockZ, takenFirstNames);
                    if (spawned == null) {
                        LOGGER.warning("startForPlayer: RescueQuest1.spawn() failed at (" + blockX + "," + groundY + "," + blockZ + ")");
                        onDone.accept(null);
                        return;
                    }

                    // Place the objective marker exactly on the victim
                    spawnMarker(liveStore, spawned.victimPos(), playerRef);

                    Objective objective = ObjectivePlugin.get().startObjectiveLine(
                            liveStore, OBJECTIVE_LINE_ID, Set.of(playerUuid),
                            world.getWorldConfig().getUuid(), null);
                    if (objective == null) {
                        LOGGER.warning("startForPlayer: failed to start ObjectiveLine " + OBJECTIVE_LINE_ID);
                    }

                    LOGGER.info("Quest started. Trap at (" + blockX + "," + groundY + "," + blockZ + ")");
                    onDone.accept(spawned);
                }));
    }

    /**
     * Recursively tries up to {@link #MAX_SPAWN_ATTEMPTS} times to find a spawn position
     * 300-500 blocks from {@code fromPos} that is not on water or void.
     *
     * <p>Returns a future that resolves to {@code int[]{blockX, groundY, blockZ}} on success,
     * or {@code null} if all attempts fail (caller should fall back to the last candidate).
     */
    private static CompletableFuture<int[]> findSpawnLocation(World world, Vector3d fromPos,
                                                               ThreadLocalRandom rng, int attempt) {
        double angle = rng.nextDouble(Math.PI * 2);
        double distance = rng.nextDouble(MIN_DISTANCE, MAX_DISTANCE);
        int blockX = (int) Math.floor(fromPos.getX() + Math.cos(angle) * distance);
        int blockZ = (int) Math.floor(fromPos.getZ() + Math.sin(angle) * distance);

        int centerChunkX = ChunkUtil.chunkCoordinate(blockX);
        int centerChunkZ = ChunkUtil.chunkCoordinate(blockZ);

        // Load the 3×3 chunk square around the center so the full trap footprint is available.
        List<CompletableFuture<?>> loads = new ArrayList<>();
        for (int cx = centerChunkX - 1; cx <= centerChunkX + 1; cx++) {
            for (int cz = centerChunkZ - 1; cz <= centerChunkZ + 1; cz++) {
                loads.add(world.getChunkAsync(ChunkUtil.indexChunk(cx, cz)));
            }
        }

        long centerChunkIndex = ChunkUtil.indexChunk(centerChunkX, centerChunkZ);
        int finalBlockX = blockX;
        int finalBlockZ = blockZ;

        return CompletableFuture.allOf(loads.toArray(new CompletableFuture[0]))
                .thenCompose(v -> {
                    var centerChunk = world.getChunk(centerChunkIndex);
                    int chunkLocalX = finalBlockX & 15;
                    int chunkLocalZ = finalBlockZ & 15;
                    int groundY = (centerChunk != null)
                            ? centerChunk.getHeight(chunkLocalX, chunkLocalZ)
                            : (int) Math.floor(fromPos.getY());

                    // Check the surface block for water / void
                    var surfaceBlock = world.getBlockType(finalBlockX, groundY, finalBlockZ);
                    boolean isBad = false;
                    if (surfaceBlock != null) {
                        String id = surfaceBlock.getId();
                        for (String prefix : BAD_BLOCK_PREFIXES) {
                            if (id != null && id.startsWith(prefix)) {
                                isBad = true;
                                break;
                            }
                        }
                    }

                    if (!isBad) {
                        return CompletableFuture.completedFuture(new int[]{finalBlockX, groundY, finalBlockZ});
                    }

                    if (attempt + 1 >= MAX_SPAWN_ATTEMPTS) {
                        // All attempts exhausted — use this position anyway
                        LOGGER.warning("findSpawnLocation: all " + MAX_SPAWN_ATTEMPTS
                                + " attempts bad terrain, using last candidate at ("
                                + finalBlockX + "," + groundY + "," + finalBlockZ + ")");
                        return CompletableFuture.completedFuture(new int[]{finalBlockX, groundY, finalBlockZ});
                    }

                    LOGGER.fine("findSpawnLocation: attempt " + attempt + " bad terrain at ("
                            + finalBlockX + "," + groundY + "," + finalBlockZ + "), retrying");
                    return findSpawnLocation(world, fromPos, rng, attempt + 1);
                });
    }

    private static void spawnMarker(Store<EntityStore> store, Vector3d pos,
                                    Ref<EntityStore> playerRef) {
        spawnNamedMarker(store, pos, playerRef, "RescueTrap_Marker");
    }

    /** Spawns the return-to-village marker at {@code pos} so ReachLocation task can complete. */
    public static void spawnReturnMarker(Store<EntityStore> store, Vector3d pos,
                                         Ref<EntityStore> playerRef) {
        spawnNamedMarker(store, pos, playerRef, "Village_Return_Marker");
    }

    private static void spawnNamedMarker(Store<EntityStore> store, Vector3d pos,
                                         Ref<EntityStore> playerRef, String markerName) {
        TransformComponent playerTransform = store.getComponent(playerRef, TransformComponent.getComponentType());
        var rotation = playerTransform != null ? playerTransform.getRotation() : new Vector3f(0, 0, 0);

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(ReachLocationMarker.getComponentType(),
                new ReachLocationMarker(markerName));
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
        if (markerIconApplied) return;
        ReachLocationTask.MARKER_ICON = "UserF.png";
        markerIconApplied = true;
    }

    /** Places the trap at {@code (centerX, groundY, centerZ)} and spawns NPCs.
     *  Must be called on the world thread with the target chunk already loaded. */
    public static Spawned spawn(World world, Store<EntityStore> store,
                                int centerX, int groundY, int centerZ,
                                Set<String> takenFirstNames) {
        int prefabOriginWorldY = groundY - PREFAB_SURFACE_Y;

        try {
            BlockSelection selection = PrefabStore.get()
                    .getAssetPrefabFromAnyPack(PREFAB_NAME + ".prefab.json");
            if (selection == null) {
                LOGGER.warning("Prefab not found: " + PREFAB_NAME);
                return null;
            }
            selection.setAnchor(0, 0, 0);
            selection.placeNoReturn(world,
                    new Vector3i(centerX, prefabOriginWorldY, centerZ), store);
            LOGGER.info("Rescue trap prefab placed at center=(" + centerX + "," + groundY + "," + centerZ + ")");
        } catch (Exception e) {
            LOGGER.warning("Failed to place rescue trap prefab: " + e.getMessage());
            return null;
        }

        Vector3d victimPos = new Vector3d(
                centerX + VICTIM_DX + 0.5,
                prefabOriginWorldY + VICTIM_PREFAB_Y,
                centerZ + VICTIM_DZ + 0.5);
        Pair<Ref<EntityStore>, INonPlayerCharacter> victim =
                NpcManager.spawnNpc(store, victimPos, new Vector3f(0, 0, 0), VICTIM_ROLE);
        if (victim == null) {
            LOGGER.warning("Failed to spawn rescue victim");
        } else {
            final Ref<EntityStore> victimRef = victim.first();
            activeNpcRefs.add(victimRef);

            long skinSeed = ThreadLocalRandom.current().nextLong();
            BodyArchetype body = VillagerAppearance.predictBody(skinSeed);
            String[] name = VillagerNames.rollHumanName(skinSeed, body, takenFirstNames);
            VillagerData victimData = new VillagerData(VillagerData.RACE_HUMAN, name[0], name[1], skinSeed);
            store.putComponent(victimRef, VillagerData.getComponentType(), victimData);

            UUID victimUuid = NpcManager.extractUuid(store, victimRef);
            if (victimUuid != null) {
                long chunkIndex = NpcManager.chunkIndexFor(victimPos);
                NpcRegistry.NpcRecord record = new NpcRegistry.NpcRecord(
                        victimUuid, VICTIM_ROLE, NpcRegistry.InteractionType.RESCUE, skinSeed, chunkIndex);
                NpcRegistry.get().register(record);
                HearthboundDataStore.get().save();
                // Entity is live — restore immediately (interaction now, skin with delay).
                NpcRestorer.restore(victimRef, store, world, record);
            }
        }

        Vector3d guardPos = new Vector3d(
                centerX + GUARD_DX + 0.5,
                groundY + 1,
                centerZ + GUARD_DZ + 0.5);
        var guard = NpcManager.spawnNpc(store, guardPos, new Vector3f(0, 0, 0), GUARD_ROLE);
        if (guard == null) {
            LOGGER.warning("Failed to spawn rescue guard");
        } else {
            activeNpcRefs.add(guard.first());
            UUID guardUuid = NpcManager.extractUuid(store, guard.first());
            if (guardUuid != null) {
                long chunkIndex = NpcManager.chunkIndexFor(guardPos);
                NpcRegistry.get().register(new NpcRegistry.NpcRecord(
                        guardUuid, GUARD_ROLE, NpcRegistry.InteractionType.NONE, 0L, chunkIndex));
                HearthboundDataStore.get().save();
            }
        }

        return new Spawned(victimPos, guardPos);
    }

    private static Set<String> collectTakenFullNames(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        Set<String> taken = new HashSet<>();
        VillageData village = VillageManager.get().getVillageData(store, playerRef);
        if (village == null) return taken;
        for (VillagerSummary v : village.getVillagers()) {
            String full = v.getFullName();
            if (!full.isEmpty()) taken.add(full);
        }
        return taken;
    }

    public record Spawned(Vector3d victimPos, Vector3d guardPos) {}
}
