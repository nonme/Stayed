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
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
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
import dev.hearthbound.quest.RescueQuestManager;
import dev.hearthbound.quest.RescueQuestManager.QuestVariant;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public class QuestCommand extends AbstractCommandCollection {

    public QuestCommand() {
        super("quest", "Quest testing commands");
        addSubCommand(new StartQuestCommand());
        addSubCommand(new StartVariantCommand());
        addSubCommand(new ResetQuestCommand());
        addSubCommand(new AddMarkerCommand());
        addSubCommand(new StartLineCommand());
        addSubCommand(new EnableMarkersCommand());
    }

    /** Start next quest in rotation (same as clicking "We need settlers" in dialog). */
    private static class StartQuestCommand extends AbstractPlayerCommand {
        StartQuestCommand() {
            super("start", "Start next rescue quest variant (follows rotation)");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef playerRef, World world) {
            launchVariant(ctx, store, ref, playerRef, world, null);
        }
    }

    /**
     * /hb quest variant <trap|cabin|ruins|camp> — force a specific variant for testing.
     * Bypasses rotation but still records the play in history.
     */
    private static class StartVariantCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> variantArg =
                withRequiredArg("variant", "trap | cabin | ruins | camp", ArgTypes.STRING);

        StartVariantCommand() {
            super("variant", "Start a specific rescue quest variant (trap/cabin/ruins/camp)");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef playerRef, World world) {
            String raw = ctx.get(variantArg).toUpperCase();
            QuestVariant variant;
            try {
                variant = QuestVariant.valueOf(raw);
            } catch (IllegalArgumentException e) {
                String valid = Arrays.stream(QuestVariant.values())
                        .map(v -> v.name().toLowerCase())
                        .collect(Collectors.joining(", "));
                ctx.sendMessage(Message.raw("Unknown variant '" + raw + "'. Valid: " + valid));
                return;
            }
            launchVariant(ctx, store, ref, playerRef, world, variant);
        }
    }

    private static void launchVariant(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                                      PlayerRef playerRef, World world, QuestVariant forced) {
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) { ctx.sendMessage(Message.raw("Could not read player transform")); return; }
        UUIDComponent uuidComp = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComp == null) { ctx.sendMessage(Message.raw("Player UUID not found")); return; }
        Player player = store.getComponent(ref, Player.getComponentType());

        VillageData village = VillageManager.get().getOrCreateVillageData(store, ref);
        QuestVariant variant = forced != null ? forced : RescueQuestManager.pickNextVariant(village);
        RescueQuestManager.recordVariantPlayed(village, variant);
        village.setRescueQuestStarted(true);
        VillageManager.get().save(store, ref, village);

        Vector3d fromPos = transform.getPosition();
        UUID playerUuid = uuidComp.getUuid();
        ctx.sendMessage(Message.raw("Starting variant " + variant.name() + "..."));

        RescueQuestManager.startForPlayer(world, store, ref, player, playerUuid, fromPos, variant, spawned -> {
            if (spawned == null) {
                playerRef.sendMessage(Message.raw("Failed to start quest (prefab error or no valid location)"));
            } else {
                playerRef.sendMessage(Message.raw(String.format(
                        "Quest started [%s]. Victim at (%.0f, %.0f, %.0f)",
                        variant.name(),
                        spawned.victimPos().getX(),
                        spawned.victimPos().getY(),
                        spawned.victimPos().getZ())));
            }
        });
    }

    /** Reset quest state fully — clears started flag, trap-done flag, and history. */
    private static class ResetQuestCommand extends AbstractPlayerCommand {
        ResetQuestCommand() {
            super("reset", "Reset all rescue quest state (started, trap-done, history)");
        }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                               PlayerRef playerRef, World world) {
            Player player = store.getComponent(ref, Player.getComponentType());
            RescueQuestManager.cancelAllObjectives(store, ref, player);
            RescueQuestManager.removeAllMarkers(store);
            RescueQuestManager.cleanup(store);

            VillageData village = VillageManager.get().getOrCreateVillageData(store, ref);
            village.setRescueQuestStarted(false);
            village.setRescueQuestTrapDone(false);
            village.getRescueQuestHistory().clear();
            VillageManager.get().save(store, ref, village);

            ctx.sendMessage(Message.raw("Rescue quest fully reset. Talk to the elf to start again."));
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
            RescueQuestManager.applyMarkerIconOnce();
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
