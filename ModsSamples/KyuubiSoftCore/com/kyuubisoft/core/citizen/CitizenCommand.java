/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.math.vector.Vector3f
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.command.system.AbstractCommand
 *  com.hypixel.hytale.server.core.command.system.CommandContext
 *  com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
 *  com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
 *  com.hypixel.hytale.server.core.command.system.basecommands.CommandBase
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage
 *  com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.Universe
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.CorePlugin;
import com.kyuubisoft.core.api.CoreAPI;
import com.kyuubisoft.core.citizen.CitizenData;
import com.kyuubisoft.core.citizen.CitizenService;
import com.kyuubisoft.core.citizen.NpcViewerPage;
import com.kyuubisoft.core.citizen.QuestMarkerManager;
import com.kyuubisoft.core.util.CommandUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

public class CitizenCommand
extends AbstractCommandCollection {
    private final CorePlugin plugin;

    protected boolean canGeneratePermission() {
        return false;
    }

    public CitizenCommand(CorePlugin plugin) {
        super("kscitizen", "NPC citizen management");
        this.requirePermission("ks.citizen");
        this.plugin = plugin;
        this.addAliases(new String[]{"npc"});
        this.addSubCommand((AbstractCommand)new ListCommand(this));
        this.addSubCommand((AbstractCommand)new CreateCommand(this));
        this.addSubCommand((AbstractCommand)new RemoveCommand(this));
        this.addSubCommand((AbstractCommand)new TeleportCommand(this));
        this.addSubCommand((AbstractCommand)new RespawnCommand(this));
        this.addSubCommand((AbstractCommand)new RespawnAllCommand(this));
        this.addSubCommand((AbstractCommand)new ReloadCommand(this));
        this.addSubCommand((AbstractCommand)new InfoCommand(this));
        this.addSubCommand((AbstractCommand)new MarkerCommand(this));
        this.addSubCommand((AbstractCommand)new ViewerCommand(this));
        this.addSubCommand((AbstractCommand)new WpAddCommand(this));
        this.addSubCommand((AbstractCommand)new WpUndoCommand(this));
        this.addSubCommand((AbstractCommand)new WpDoneCommand(this));
        this.addSubCommand((AbstractCommand)new WpRecordCommand(this));
    }

    private CitizenService getService() {
        return this.plugin.getCitizenService();
    }

    private boolean checkPermission(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            return true;
        }
        Player player = (Player)ctx.senderAs(Player.class);
        if (!player.hasPermission("citizen.admin")) {
            ctx.sendMessage(Message.raw((String)"\u00a7cNo permission. Required: citizen.admin"));
            return false;
        }
        return true;
    }

    private boolean checkShowcaseWrite(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            return true;
        }
        Player player = (Player)ctx.senderAs(Player.class);
        if (CoreAPI.isShowcaseBlocked(player.getPlayerRef())) {
            ctx.sendMessage(Message.raw((String)"\u00a7cNo permission. Server is in read-only showcase mode."));
            return false;
        }
        return true;
    }

    private String[] parseArgs(CommandContext ctx) {
        String input = ctx.getInputString().trim();
        return input.split("\\s+");
    }

    private class ListCommand
    extends CommandBase {
        final /* synthetic */ CitizenCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        ListCommand(CitizenCommand citizenCommand) {
            CitizenCommand citizenCommand2 = citizenCommand;
            Objects.requireNonNull(citizenCommand2);
            this.this$0 = citizenCommand2;
            super("list", "List all citizens");
            this.requirePermission("ks.citizen.admin");
        }

        protected void executeSync(CommandContext ctx) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            String[] parts = this.this$0.parseArgs(ctx);
            String filter = parts.length > 1 ? parts[1] : null;
            Collection<CitizenData> citizens = this.this$0.getService().getAllCitizens();
            if (citizens.isEmpty()) {
                ctx.sendMessage(Message.raw((String)"\u00a77No citizens defined."));
                return;
            }
            ctx.sendMessage(Message.raw((String)("\u00a76=== Citizens (" + citizens.size() + ") ===")));
            for (CitizenData c : citizens) {
                if (filter != null && !filter.equals(c.group)) continue;
                String status = c.spawnedEntityUUID != null ? "\u00a7a\u25cf" : "\u00a7c\u25cb";
                String group = c.group != null ? " \u00a78[" + c.group + "]" : "";
                ctx.sendMessage(Message.raw((String)(status + " \u00a7f" + c.id + group + " \u00a77(" + Math.round(c.posX) + ", " + Math.round(c.posY) + ", " + Math.round(c.posZ) + ")")));
            }
        }
    }

    private class CreateCommand
    extends AbstractPlayerCommand {
        final /* synthetic */ CitizenCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        CreateCommand(CitizenCommand citizenCommand) {
            CitizenCommand citizenCommand2 = citizenCommand;
            Objects.requireNonNull(citizenCommand2);
            this.this$0 = citizenCommand2;
            super("create", "Create a citizen at your position");
            this.requirePermission("ks.citizen.admin");
        }

        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            if (!this.this$0.checkShowcaseWrite(ctx)) {
                return;
            }
            String[] parts = this.this$0.parseArgs(ctx);
            if (parts.length < 2) {
                ctx.sendMessage(Message.raw((String)"\u00a7eUsage: /kscitizen create <id>"));
                return;
            }
            String id = parts[1];
            Player player = (Player)ctx.senderAs(Player.class);
            if (this.this$0.getService().getCitizen(id) != null) {
                ctx.sendMessage(Message.raw((String)("\u00a7cCitizen '" + id + "' already exists!")));
                return;
            }
            CitizenData citizen = new CitizenData();
            citizen.id = id;
            citizen.name = id;
            citizen.worldName = world.getName();
            TransformComponent transform = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
            if (transform != null) {
                Vector3f rot;
                Vector3d pos = transform.getPosition();
                if (pos != null) {
                    citizen.posX = pos.x;
                    citizen.posY = pos.y;
                    citizen.posZ = pos.z;
                }
                if ((rot = transform.getRotation()) != null) {
                    citizen.rotY = rot.y;
                }
            }
            citizen.attitude = "PASSIVE";
            citizen.movementType = "IDLE";
            citizen.isPlayerModel = true;
            citizen.fKeyInteractionEnabled = true;
            citizen.scale = 1.0f;
            this.this$0.getService().addCitizen(citizen);
            world.execute(() -> this.this$0.getService().spawnCitizen(citizen, world));
            ctx.sendMessage(Message.raw((String)("\u00a7aCitizen '" + id + "' created at your position!")));
        }
    }

    private class RemoveCommand
    extends CommandBase {
        final /* synthetic */ CitizenCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        RemoveCommand(CitizenCommand citizenCommand) {
            CitizenCommand citizenCommand2 = citizenCommand;
            Objects.requireNonNull(citizenCommand2);
            this.this$0 = citizenCommand2;
            super("remove", "Remove a citizen");
            this.requirePermission("ks.citizen.admin");
        }

        protected void executeSync(CommandContext ctx) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            if (!this.this$0.checkShowcaseWrite(ctx)) {
                return;
            }
            String[] parts = this.this$0.parseArgs(ctx);
            if (parts.length < 2) {
                ctx.sendMessage(Message.raw((String)"\u00a7eUsage: /kscitizen remove <id>"));
                return;
            }
            String id = parts[1];
            CitizenData citizen = this.this$0.getService().getCitizen(id);
            if (citizen == null) {
                ctx.sendMessage(Message.raw((String)("\u00a7cCitizen '" + id + "' not found!")));
                return;
            }
            this.this$0.getService().removeCitizen(id);
            ctx.sendMessage(Message.raw((String)("\u00a7aCitizen '" + id + "' removed!")));
        }
    }

    private class TeleportCommand
    extends AbstractPlayerCommand {
        final /* synthetic */ CitizenCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        TeleportCommand(CitizenCommand citizenCommand) {
            CitizenCommand citizenCommand2 = citizenCommand;
            Objects.requireNonNull(citizenCommand2);
            this.this$0 = citizenCommand2;
            super("tp", "Teleport to a citizen");
            this.requirePermission("ks.citizen.admin");
        }

        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            String[] parts = this.this$0.parseArgs(ctx);
            if (parts.length < 2) {
                ctx.sendMessage(Message.raw((String)"\u00a7eUsage: /kscitizen tp <id>"));
                return;
            }
            String id = parts[1];
            CitizenData citizen = this.this$0.getService().getCitizen(id);
            if (citizen == null) {
                ctx.sendMessage(Message.raw((String)("\u00a7cCitizen '" + id + "' not found!")));
                return;
            }
            Player player = (Player)ctx.senderAs(Player.class);
            String cmd = "tp " + playerRef.getUsername() + " " + citizen.posX + " " + citizen.posY + " " + citizen.posZ;
            CommandUtils.executeAsConsole(player, cmd);
            ctx.sendMessage(Message.raw((String)("\u00a7aTeleported to citizen '" + id + "'!")));
        }
    }

    private class RespawnCommand
    extends AbstractPlayerCommand {
        final /* synthetic */ CitizenCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        RespawnCommand(CitizenCommand citizenCommand) {
            CitizenCommand citizenCommand2 = citizenCommand;
            Objects.requireNonNull(citizenCommand2);
            this.this$0 = citizenCommand2;
            super("respawn", "Respawn a citizen");
            this.requirePermission("ks.citizen.admin");
        }

        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            String[] parts = this.this$0.parseArgs(ctx);
            if (parts.length < 2) {
                ctx.sendMessage(Message.raw((String)"\u00a7eUsage: /kscitizen respawn <id>"));
                return;
            }
            String id = parts[1];
            CitizenData citizen = this.this$0.getService().getCitizen(id);
            if (citizen == null) {
                ctx.sendMessage(Message.raw((String)("\u00a7cCitizen '" + id + "' not found!")));
                return;
            }
            this.this$0.getService().despawnCitizen(citizen);
            world.execute(() -> this.this$0.getService().spawnCitizen(citizen, world));
            ctx.sendMessage(Message.raw((String)("\u00a7aCitizen '" + id + "' respawned!")));
        }
    }

    private class RespawnAllCommand
    extends CommandBase {
        final /* synthetic */ CitizenCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        RespawnAllCommand(CitizenCommand citizenCommand) {
            CitizenCommand citizenCommand2 = citizenCommand;
            Objects.requireNonNull(citizenCommand2);
            this.this$0 = citizenCommand2;
            super("respawnall", "Despawn & respawn all citizens");
            this.requirePermission("ks.citizen.admin");
        }

        protected void executeSync(CommandContext ctx) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            if (!this.this$0.checkShowcaseWrite(ctx)) {
                return;
            }
            CitizenService service = this.this$0.getService();
            Collection<CitizenData> citizens = service.getAllCitizens();
            if (citizens == null || citizens.isEmpty()) {
                ctx.sendMessage(Message.raw((String)"\u00a7eNo citizens registered."));
                return;
            }
            Universe universe = Universe.get();
            if (universe == null) {
                ctx.sendMessage(Message.raw((String)"\u00a7cUniverse not available."));
                return;
            }
            World world = universe.getDefaultWorld();
            if (world == null) {
                ctx.sendMessage(Message.raw((String)"\u00a7cDefault world not available."));
                return;
            }
            int count = citizens.size();
            ctx.sendMessage(Message.raw((String)("\u00a7eDespawning " + count + " citizens...")));
            world.execute(() -> {
                for (CitizenData citizen : new ArrayList(citizens)) {
                    try {
                        service.despawnCitizen(citizen, world);
                    }
                    catch (Exception exception) {}
                }
                ctx.sendMessage(Message.raw((String)("\u00a7eRespawning " + count + " citizens...")));
                int spawned = 0;
                for (CitizenData citizen : new ArrayList(citizens)) {
                    try {
                        service.spawnCitizen(citizen, world);
                        ++spawned;
                    }
                    catch (Exception e) {
                        ctx.sendMessage(Message.raw((String)("\u00a7cFailed to respawn '" + citizen.id + "': " + e.getMessage())));
                    }
                }
                ctx.sendMessage(Message.raw((String)("\u00a7aRespawned " + spawned + "/" + count + " citizens.")));
            });
        }
    }

    private class ReloadCommand
    extends CommandBase {
        final /* synthetic */ CitizenCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        ReloadCommand(CitizenCommand citizenCommand) {
            CitizenCommand citizenCommand2 = citizenCommand;
            Objects.requireNonNull(citizenCommand2);
            this.this$0 = citizenCommand2;
            super("reload", "Reload citizens config");
            this.requirePermission("ks.citizen.admin");
        }

        protected void executeSync(CommandContext ctx) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            if (!this.this$0.checkShowcaseWrite(ctx)) {
                return;
            }
            this.this$0.getService().reloadAllWorlds();
            ctx.sendMessage(Message.raw((String)("\u00a7aCitizens reloading across all worlds... (" + this.this$0.getService().getCitizenCount() + " citizens)")));
        }
    }

    private class InfoCommand
    extends CommandBase {
        final /* synthetic */ CitizenCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        InfoCommand(CitizenCommand citizenCommand) {
            CitizenCommand citizenCommand2 = citizenCommand;
            Objects.requireNonNull(citizenCommand2);
            this.this$0 = citizenCommand2;
            super("info", "Show citizen details");
            this.requirePermission("ks.citizen.admin");
        }

        protected void executeSync(CommandContext ctx) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            String[] parts = this.this$0.parseArgs(ctx);
            if (parts.length < 2) {
                ctx.sendMessage(Message.raw((String)"\u00a7eUsage: /kscitizen info <id>"));
                return;
            }
            String id = parts[1];
            CitizenData citizen = this.this$0.getService().getCitizen(id);
            if (citizen == null) {
                ctx.sendMessage(Message.raw((String)("\u00a7cCitizen '" + id + "' not found!")));
                return;
            }
            String status = citizen.spawnedEntityUUID != null ? "\u00a7aSpawned" : "\u00a7cNot spawned";
            ctx.sendMessage(Message.raw((String)("\u00a76=== Citizen: " + citizen.id + " ===")));
            ctx.sendMessage(Message.raw((String)("\u00a77Name: \u00a7f" + citizen.getDisplayName())));
            ctx.sendMessage(Message.raw((String)("\u00a77Group: \u00a7f" + (citizen.group != null ? citizen.group : "none"))));
            ctx.sendMessage(Message.raw((String)("\u00a77World: \u00a7f" + citizen.worldName)));
            ctx.sendMessage(Message.raw((String)("\u00a77Position: \u00a7f" + String.format("%.1f, %.1f, %.1f", citizen.posX, citizen.posY, citizen.posZ))));
            ctx.sendMessage(Message.raw((String)("\u00a77Model: \u00a7f" + (String)(citizen.isPlayerModel ? "Player (" + citizen.skinUsername + ")" : citizen.modelId))));
            ctx.sendMessage(Message.raw((String)("\u00a77Role: \u00a7f" + citizen.resolveRoleName())));
            ctx.sendMessage(Message.raw((String)("\u00a77Movement: \u00a7f" + citizen.movementType)));
            ctx.sendMessage(Message.raw((String)("\u00a77Status: " + status)));
            if (citizen.dialogs != null && !citizen.dialogs.isEmpty()) {
                ctx.sendMessage(Message.raw((String)("\u00a77Dialogs: \u00a7f" + citizen.dialogs.size() + " entries")));
            }
            if (citizen.shopId != null) {
                ctx.sendMessage(Message.raw((String)("\u00a77Shop: \u00a7f" + citizen.shopId)));
            }
        }
    }

    private class MarkerCommand
    extends CommandBase {
        final /* synthetic */ CitizenCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        MarkerCommand(CitizenCommand citizenCommand) {
            CitizenCommand citizenCommand2 = citizenCommand;
            Objects.requireNonNull(citizenCommand2);
            this.this$0 = citizenCommand2;
            super("marker", "Set/remove quest marker above citizen");
            this.requirePermission("ks.citizen.admin");
        }

        protected void executeSync(CommandContext ctx) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            if (!ctx.isPlayer()) {
                ctx.sendMessage(Message.raw((String)"\u00a7cThis command can only be used by players!"));
                return;
            }
            String[] parts = this.this$0.parseArgs(ctx);
            if (parts.length < 2) {
                ctx.sendMessage(Message.raw((String)"\u00a7eUsage: /kscitizen marker <citizenId> [! | ? | off]"));
                return;
            }
            String citizenId = parts[1];
            String markerText = parts.length >= 3 ? parts[2] : "!";
            CitizenData citizen = this.this$0.getService().getCitizen(citizenId);
            if (citizen == null) {
                ctx.sendMessage(Message.raw((String)("\u00a7cCitizen '" + citizenId + "' not found!")));
                return;
            }
            QuestMarkerManager markerManager = this.this$0.getService().getQuestMarkerManager();
            if (markerManager == null) {
                ctx.sendMessage(Message.raw((String)"\u00a7cQuestMarkerManager not available!"));
                return;
            }
            Player player = (Player)ctx.senderAs(Player.class);
            UUID playerId = player.getPlayerRef().getUuid();
            World world = player.getWorld();
            if (world == null) {
                ctx.sendMessage(Message.raw((String)"\u00a7cCould not determine your world!"));
                return;
            }
            if ("off".equalsIgnoreCase(markerText)) {
                world.execute(() -> markerManager.removeMarker(citizenId, playerId, world));
                ctx.sendMessage(Message.raw((String)("\u00a7aMarker removed from " + citizenId)));
            } else {
                world.execute(() -> markerManager.setMarker(citizenId, playerId, markerText, world));
                ctx.sendMessage(Message.raw((String)("\u00a7aMarker '" + markerText + "' set on " + citizenId + " (only visible to you)")));
            }
        }
    }

    private class ViewerCommand
    extends AbstractPlayerCommand {
        final /* synthetic */ CitizenCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        ViewerCommand(CitizenCommand citizenCommand) {
            CitizenCommand citizenCommand2 = citizenCommand;
            Objects.requireNonNull(citizenCommand2);
            this.this$0 = citizenCommand2;
            super("viewer", "Open NPC viewer page");
            this.requirePermission("ks.citizen.admin");
        }

        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            Player player = (Player)ctx.senderAs(Player.class);
            NpcViewerPage page = new NpcViewerPage(this.this$0.plugin, player, playerRef);
            player.getPageManager().openCustomPage(ref, store, (CustomUIPage)page);
        }
    }

    private class WpAddCommand
    extends AbstractPlayerCommand {
        final /* synthetic */ CitizenCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        WpAddCommand(CitizenCommand citizenCommand) {
            CitizenCommand citizenCommand2 = citizenCommand;
            Objects.requireNonNull(citizenCommand2);
            this.this$0 = citizenCommand2;
            super("wpadd", "Add waypoint at current position");
            this.requirePermission("ks.citizen.admin");
            this.setAllowsExtraArguments(true);
        }

        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            if (!this.this$0.checkShowcaseWrite(ctx)) {
                return;
            }
            Player player = (Player)ctx.senderAs(Player.class);
            String citizenId = this.this$0.getService().getRecordingCitizenId(playerRef.getUuid());
            if (citizenId == null) {
                ctx.sendMessage(Message.raw((String)"No active waypoint recording. Use /kscitizen wprecord <citizenId> first.").color("#FF5555"));
                return;
            }
            CitizenData citizen = this.this$0.getService().getCitizen(citizenId);
            if (citizen == null) {
                ctx.sendMessage(Message.raw((String)("Citizen '" + citizenId + "' no longer exists!")).color("#FF5555"));
                return;
            }
            TransformComponent transform = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) {
                ctx.sendMessage(Message.raw((String)"Could not get your position!").color("#FF5555"));
                return;
            }
            Vector3d pos = transform.getPosition();
            Vector3f rot = transform.getRotation();
            if (pos == null) {
                return;
            }
            String[] parts = this.this$0.parseArgs(ctx);
            float pause = 0.0f;
            if (parts.length >= 2) {
                try {
                    pause = Float.parseFloat(parts[1]);
                    if (pause < 0.0f) {
                        pause = 0.0f;
                    }
                    if (pause > 30.0f) {
                        pause = 30.0f;
                    }
                }
                catch (NumberFormatException numberFormatException) {
                    // empty catch block
                }
            }
            if (citizen.waypoints == null) {
                citizen.waypoints = new ArrayList<CitizenData.WaypointData>();
            }
            float rotY = rot != null ? rot.y : 0.0f;
            citizen.waypoints.add(new CitizenData.WaypointData(pos.x, pos.y, pos.z, rotY, pause));
            float maxPause = 0.0f;
            for (CitizenData.WaypointData wp : citizen.waypoints) {
                if (!(wp.pauseTime > maxPause)) continue;
                maxPause = wp.pauseTime;
            }
            citizen.waypointPause = maxPause;
            int count = citizen.waypoints.size();
            this.this$0.getService().updateRecorderHud(playerRef.getUuid(), count);
            String pauseInfo = pause > 0.0f ? " (pause: " + String.format("%.0f", Float.valueOf(pause)) + "s)" : "";
            ctx.sendMessage(Message.raw((String)("WP #" + count + " at " + String.format("%.1f, %.1f, %.1f", pos.x, pos.y, pos.z) + pauseInfo)).color("#55FF55"));
        }
    }

    private class WpUndoCommand
    extends AbstractPlayerCommand {
        final /* synthetic */ CitizenCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        WpUndoCommand(CitizenCommand citizenCommand) {
            CitizenCommand citizenCommand2 = citizenCommand;
            Objects.requireNonNull(citizenCommand2);
            this.this$0 = citizenCommand2;
            super("wpundo", "Remove last waypoint");
            this.requirePermission("ks.citizen.admin");
        }

        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            if (!this.this$0.checkShowcaseWrite(ctx)) {
                return;
            }
            String citizenId = this.this$0.getService().getRecordingCitizenId(playerRef.getUuid());
            if (citizenId == null) {
                ctx.sendMessage(Message.raw((String)"No active waypoint recording.").color("#FF5555"));
                return;
            }
            CitizenData citizen = this.this$0.getService().getCitizen(citizenId);
            if (citizen == null || citizen.waypoints == null || citizen.waypoints.isEmpty()) {
                ctx.sendMessage(Message.raw((String)"No waypoints to remove.").color("#FF5555"));
                return;
            }
            citizen.waypoints.removeLast();
            int count = citizen.waypoints.size();
            this.this$0.getService().updateRecorderHud(playerRef.getUuid(), count);
            ctx.sendMessage(Message.raw((String)("Last waypoint removed. " + count + " remaining.")).color("#FFAA00"));
        }
    }

    private class WpDoneCommand
    extends AbstractPlayerCommand {
        final /* synthetic */ CitizenCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        WpDoneCommand(CitizenCommand citizenCommand) {
            CitizenCommand citizenCommand2 = citizenCommand;
            Objects.requireNonNull(citizenCommand2);
            this.this$0 = citizenCommand2;
            super("wpdone", "Stop waypoint recording");
            this.requirePermission("ks.citizen.admin");
        }

        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            if (!this.this$0.checkShowcaseWrite(ctx)) {
                return;
            }
            Player player = (Player)ctx.senderAs(Player.class);
            String citizenId = this.this$0.getService().getRecordingCitizenId(playerRef.getUuid());
            if (citizenId == null) {
                ctx.sendMessage(Message.raw((String)"No active waypoint recording.").color("#FF5555"));
                return;
            }
            CitizenData citizen = this.this$0.getService().getCitizen(citizenId);
            int count = citizen != null && citizen.waypoints != null ? citizen.waypoints.size() : 0;
            this.this$0.getService().stopWaypointRecording(playerRef.getUuid(), player, playerRef);
            this.this$0.getService().updateCitizen(citizen);
            ctx.sendMessage(Message.raw((String)("Recording stopped. " + count + " waypoints saved for " + citizenId + ".")).color("#55FF55"));
            if (count < 2) {
                ctx.sendMessage(Message.raw((String)"Need at least 2 waypoints for PATH!").color("#FF5555"));
            }
        }
    }

    private class WpRecordCommand
    extends AbstractPlayerCommand {
        final /* synthetic */ CitizenCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        WpRecordCommand(CitizenCommand citizenCommand) {
            CitizenCommand citizenCommand2 = citizenCommand;
            Objects.requireNonNull(citizenCommand2);
            this.this$0 = citizenCommand2;
            super("wprecord", "Start waypoint recording for a citizen");
            this.requirePermission("ks.citizen.admin");
        }

        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
            if (!this.this$0.checkPermission(ctx)) {
                return;
            }
            if (!this.this$0.checkShowcaseWrite(ctx)) {
                return;
            }
            Player player = (Player)ctx.senderAs(Player.class);
            String[] parts = this.this$0.parseArgs(ctx);
            if (parts.length < 2) {
                ctx.sendMessage(Message.raw((String)"\u00a7eUsage: /kscitizen wprecord <citizenId>"));
                return;
            }
            String citizenId = parts[1];
            CitizenData citizen = this.this$0.getService().getCitizen(citizenId);
            if (citizen == null) {
                ctx.sendMessage(Message.raw((String)("\u00a7cCitizen '" + citizenId + "' not found!")));
                return;
            }
            this.this$0.getService().startWaypointRecording(playerRef.getUuid(), citizenId, player, playerRef);
            int wpCount = citizen.waypoints != null ? citizen.waypoints.size() : 0;
            ctx.sendMessage(Message.raw((String)("Recording waypoints for " + citizenId + " (" + wpCount + " existing). Walk around and use:")).color("#44cc88"));
            ctx.sendMessage(Message.raw((String)"  /kscitizen wpadd \u2014 add waypoint at your position").color("#88aacc"));
            ctx.sendMessage(Message.raw((String)"  /kscitizen wpundo \u2014 remove last waypoint").color("#88aacc"));
            ctx.sendMessage(Message.raw((String)"  /kscitizen wpdone \u2014 stop recording").color("#88aacc"));
        }
    }
}

