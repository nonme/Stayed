package dev.hearthbound.commands;

import com.hypixel.hytale.builtin.adventure.objectives.Objective;
import com.hypixel.hytale.builtin.adventure.objectives.ObjectivePlugin;
import com.hypixel.hytale.builtin.adventure.objectives.markers.reachlocation.ReachLocationMarker;
import com.hypixel.hytale.builtin.adventure.objectives.markers.reachlocation.ReachLocationMarkerAsset;
import com.hypixel.hytale.builtin.adventure.objectives.task.ReachLocationTask;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HiddenFromAdventurePlayers;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.quest.RescueQuest1;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class QuestCommand extends AbstractCommandCollection {

    public QuestCommand() {
        super("quest", "Quest testing commands");
        addSubCommand(new StartQuest1Command());
        addSubCommand(new AddMarkerCommand());
        addSubCommand(new StartLineCommand());
        addSubCommand(new EnableMarkersCommand());
    }

    /** Cancels all active objectives for the player. Used before re-starting a quest
     *  so we don't pile up duplicates on each test run. */
    private static void cancelAllObjectives(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        Player playerComponent = store.getComponent(playerRef, Player.getComponentType());
        if (playerComponent == null) return;
        Set<UUID> active = playerComponent.getPlayerConfigData().getActiveObjectiveUUIDs();
        if (active == null || active.isEmpty()) return;
        for (UUID objectiveUuid : new HashSet<>(active)) {
            ObjectivePlugin.get().cancelObjective(objectiveUuid, store);
        }
    }

    /** Removes every ReachLocationMarker entity in the world. Without this, repeated
     *  /hb quest start calls leave stale marker entities around; ReachLocationTask.setup0
     *  picks the closest one to the player's position, which in practice is always an
     *  old marker next to the player rather than the freshly spawned one far away. */
    private static void removeAllReachLocationMarkers(Store<EntityStore> store) {
        var markerType = ReachLocationMarker.getComponentType();
        java.util.List<Ref<EntityStore>> toRemove = new java.util.ArrayList<>();
        store.forEachChunk(markerType, (chunk, cb) -> {
            for (int i = 0; i < chunk.size(); i++) {
                toRemove.add(chunk.getReferenceTo(i));
            }
        });
        for (Ref<EntityStore> ref : toRemove) {
            store.removeEntity(ref, RemoveReason.REMOVE);
        }
    }

    /** Swap the default Home icon to a danger-themed crossed-weapons icon.
     *  Deferred until the Objectives plugin is initialized — touching the class
     *  during HearthboundPlugin.setup() runs its static initializer too early
     *  and NPEs on ObjectivePlugin.get(). */
    private static boolean markerIconApplied = false;
    private static void applyMarkerIconOnce() {
        if (markerIconApplied) return;
        ReachLocationTask.MARKER_ICON = "UserF.png";
        markerIconApplied = true;
    }

    /** All-in-one: cancels any active objectives, picks a random point 100-200 blocks
     *  from the player at a random compass angle, spawns a ReachLocationMarker there,
     *  and starts the rescue quest objective line. */
    private static class StartQuest1Command extends AbstractPlayerCommand {
        private static final double MIN_DISTANCE = 300.0;
        private static final double MAX_DISTANCE = 500.0;

        StartQuest1Command() {
            super("start", "Reset and start rescue quest 1 at a random point 300-500 blocks away");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef playerRef, World world) {
            applyMarkerIconOnce();

            TransformComponent playerTransform = store.getComponent(ref, TransformComponent.getComponentType());
            if (playerTransform == null) {
                ctx.sendMessage(Message.raw("Could not read player transform"));
                return;
            }
            UUIDComponent uuid = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuid == null) {
                ctx.sendMessage(Message.raw("Player UUID not found"));
                return;
            }

            cancelAllObjectives(store, ref);
            removeAllReachLocationMarkers(store);
            RescueQuest1.cleanup(store);

            Vector3d playerPos = playerTransform.getPosition();
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            double angle = rng.nextDouble(0, Math.PI * 2);
            double distance = rng.nextDouble(MIN_DISTANCE, MAX_DISTANCE);
            double markerX = playerPos.getX() + Math.cos(angle) * distance;
            double markerZ = playerPos.getZ() + Math.sin(angle) * distance;
            double markerY = playerPos.getY();
            var playerRotation = playerTransform.getRotation();
            UUID playerUuid = uuid.getUuid();

            ctx.sendMessage(Message.raw(String.format(
                    "Loading chunk for marker at (%.0f, %.0f, %.0f), distance %.0f blocks...",
                    markerX, markerY, markerZ, distance)));

            // Force-load all chunks the trap footprint may touch.
            // Prefab is 7×7 centered, guard is +8 X from center — the whole footprint
            // fits within a 3×3 chunk square around center. We iterate chunk coords
            // (not block offsets) so the set is exact regardless of center alignment.
            int blockX = (int) Math.floor(markerX);
            int blockZ = (int) Math.floor(markerZ);
            int centerChunkX = ChunkUtil.chunkCoordinate(blockX);
            int centerChunkZ = ChunkUtil.chunkCoordinate(blockZ);
            List<CompletableFuture<?>> loads = new ArrayList<>();
            for (int cx = centerChunkX - 1; cx <= centerChunkX + 1; cx++) {
                for (int cz = centerChunkZ - 1; cz <= centerChunkZ + 1; cz++) {
                    loads.add(world.getChunkAsync(ChunkUtil.indexChunk(cx, cz)));
                }
            }
            long centerChunkIndex = ChunkUtil.indexChunk(centerChunkX, centerChunkZ);
            CompletableFuture.allOf(loads.toArray(new CompletableFuture[0])).thenAccept(v -> world.execute(() -> {
                Store<EntityStore> liveStore = world.getEntityStore().getStore();

                // Use the center chunk's heightmap for the trap anchor Y.
                var centerChunk = world.getChunk(centerChunkIndex);
                int chunkLocalX = blockX & 15;
                int chunkLocalZ = blockZ & 15;
                int groundY = (centerChunk != null)
                        ? centerChunk.getHeight(chunkLocalX, chunkLocalZ)
                        : (int) Math.floor(markerY);

                RescueQuest1.Spawned spawned = RescueQuest1.spawn(world, liveStore, blockX, groundY, blockZ);
                if (spawned == null) {
                    playerRef.sendMessage(Message.raw("Failed to spawn rescue trap (prefab error)"));
                    return;
                }

                // Put the marker on the victim so players see exactly where to aim for.
                Vector3d markerPos = spawned.victimPos();

                Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
                holder.addComponent(ReachLocationMarker.getComponentType(),
                        new ReachLocationMarker("RescueTrap_Marker"));
                var model = ObjectivePlugin.get().getObjectiveLocationMarkerModel();
                holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
                holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
                holder.addComponent(TransformComponent.getComponentType(),
                        new TransformComponent(markerPos, playerRotation));
                holder.ensureComponent(UUIDComponent.getComponentType());
                holder.ensureComponent(Intangible.getComponentType());
                holder.ensureComponent(HiddenFromAdventurePlayers.getComponentType());
                liveStore.addEntity(holder, AddReason.SPAWN);

                String lineId = "ObjectiveLine_RescueQuest_1";
                Objective objective = ObjectivePlugin.get().startObjectiveLine(
                        liveStore, lineId, Set.of(playerUuid), world.getWorldConfig().getUuid(), null);
                if (objective == null) {
                    playerRef.sendMessage(Message.raw("Failed to start ObjectiveLine: " + lineId));
                    return;
                }
                playerRef.sendMessage(Message.raw(String.format(
                        "Quest started. Trap at (%d, %d, %d)", blockX, groundY, blockZ)));
            }));
        }
    }

    private static class AddMarkerCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> markerIdArg =
                withRequiredArg("markerId", "ReachLocationMarker asset id", ArgTypes.STRING);

        AddMarkerCommand() {
            super("marker", "Spawn a ReachLocationMarker entity at your position");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef playerRef, World world) {
            String markerId = ctx.get(markerIdArg);
            if (ReachLocationMarkerAsset.getAssetMap().getAsset(markerId) == null) {
                ctx.sendMessage(Message.raw("ReachLocationMarkerAsset not found: " + markerId));
                return;
            }
            TransformComponent playerTransform = store.getComponent(ref, TransformComponent.getComponentType());
            if (playerTransform == null) {
                ctx.sendMessage(Message.raw("Could not read player transform"));
                return;
            }
            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
            holder.addComponent(ReachLocationMarker.getComponentType(), new ReachLocationMarker(markerId));
            var model = ObjectivePlugin.get().getObjectiveLocationMarkerModel();
            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
            holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
            holder.addComponent(TransformComponent.getComponentType(),
                    new TransformComponent(playerTransform.getPosition(), playerTransform.getRotation()));
            holder.ensureComponent(UUIDComponent.getComponentType());
            holder.ensureComponent(Intangible.getComponentType());
            holder.ensureComponent(HiddenFromAdventurePlayers.getComponentType());
            store.addEntity(holder, AddReason.SPAWN);
            ctx.sendMessage(Message.raw("Spawned marker " + markerId + " at your position"));
        }
    }

    private static class StartLineCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> lineIdArg =
                withRequiredArg("lineId", "ObjectiveLine asset id", ArgTypes.STRING);

        StartLineCommand() {
            super("startline", "Start an ObjectiveLine for yourself");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef playerRef, World world) {
            applyMarkerIconOnce();
            String lineId = ctx.get(lineIdArg);
            UUIDComponent uuid = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuid == null) {
                ctx.sendMessage(Message.raw("Player UUID not found"));
                return;
            }
            Objective objective = ObjectivePlugin.get().startObjectiveLine(
                    store, lineId, Set.of(uuid.getUuid()), world.getWorldConfig().getUuid(), null);
            if (objective == null) {
                ctx.sendMessage(Message.raw("Failed to start ObjectiveLine: " + lineId));
                return;
            }
            ctx.sendMessage(Message.raw("Started ObjectiveLine " + lineId));
        }
    }

    private static class EnableMarkersCommand extends AbstractWorldCommand {
        EnableMarkersCommand() {
            super("enablemarkers", "Enable objective markers for this world");
        }

        @Override
        protected void execute(CommandContext ctx, World world, Store<EntityStore> store) {
            WorldConfig worldConfig = world.getWorldConfig();
            worldConfig.setObjectiveMarkersEnabled(true);
            worldConfig.markChanged();
            ctx.sendMessage(Message.raw("Objective markers enabled in " + world.getName()));
        }
    }
}
