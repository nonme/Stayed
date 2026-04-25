package dev.hearthbound.commands;

import com.hypixel.hytale.builtin.adventure.objectives.ObjectivePlugin;
import com.hypixel.hytale.builtin.adventure.objectives.markers.reachlocation.ReachLocationMarker;
import com.hypixel.hytale.builtin.adventure.objectives.markers.reachlocation.ReachLocationMarkerAsset;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
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
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

import java.util.UUID;

public class QuestCommand extends AbstractCommandCollection {

    public QuestCommand() {
        super("quest", "Quest testing commands");
        addSubCommand(new StartQuest1Command());
        addSubCommand(new ResetQuest1Command());
        addSubCommand(new AddMarkerCommand());
        addSubCommand(new StartLineCommand());
        addSubCommand(new EnableMarkersCommand());
    }

    /** Clears rescueQuestStarted so the elf dialog shows the "We need settlers" button again. */
    private static class ResetQuest1Command extends AbstractPlayerCommand {
        ResetQuest1Command() {
            super("reset", "Reset rescue quest flag so elf offers it again");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef playerRef, World world) {
            Player player = store.getComponent(ref, Player.getComponentType());
            RescueQuest1.cancelAllObjectives(store, ref, player);
            RescueQuest1.removeAllMarkers(store);
            RescueQuest1.cleanup(store);

            VillageData village = VillageManager.get().getOrCreateVillageData(store, ref);
            village.setRescueQuestStarted(false);
            VillageManager.get().save(store, ref, village);

            ctx.sendMessage(Message.raw("Rescue quest reset — talk to the elf to start again"));
        }
    }

    private static class StartQuest1Command extends AbstractPlayerCommand {
        StartQuest1Command() {
            super("start", "Reset and start rescue quest 1 at a random point 300-500 blocks away");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef playerRef, World world) {
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) {
                ctx.sendMessage(Message.raw("Could not read player transform"));
                return;
            }
            UUIDComponent uuid = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuid == null) {
                ctx.sendMessage(Message.raw("Player UUID not found"));
                return;
            }
            Player player = store.getComponent(ref, Player.getComponentType());

            Vector3d fromPos = transform.getPosition();
            UUID playerUuid = uuid.getUuid();
            ctx.sendMessage(Message.raw("Searching for quest spawn location (up to 5 attempts)..."));

            RescueQuest1.startForPlayer(world, store, ref, player, playerUuid, fromPos, spawned -> {
                if (spawned == null) {
                    playerRef.sendMessage(Message.raw("Failed to start quest (prefab error or no valid location)"));
                } else {
                    playerRef.sendMessage(Message.raw(String.format(
                            "Quest started. Trap at (%.0f, %.0f, %.0f)",
                            spawned.victimPos().getX(),
                            spawned.victimPos().getY(),
                            spawned.victimPos().getZ())));
                }
            });
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
            RescueQuest1.applyMarkerIconOnce();
            String lineId = ctx.get(lineIdArg);
            UUIDComponent uuid = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuid == null) {
                ctx.sendMessage(Message.raw("Player UUID not found"));
                return;
            }
            var objective = ObjectivePlugin.get().startObjectiveLine(
                    store, lineId, java.util.Set.of(uuid.getUuid()),
                    world.getWorldConfig().getUuid(), null);
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
