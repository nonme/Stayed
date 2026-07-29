/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.protocol.PlayerSkin
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CitizenData {
    public String id;
    public String name;
    public String group;
    public String worldName;
    public double posX;
    public double posY;
    public double posZ;
    public float rotX;
    public float rotY;
    public float rotZ;
    public boolean spawnRelative;
    public String modelId;
    public boolean isPlayerModel;
    public String skinUsername;
    public boolean useLiveSkin;
    public float scale = 1.0f;
    public String npcRoleId;
    public String entityTypeId;
    public String helmet;
    public String chest;
    public String leggings;
    public String gloves;
    public String mainHand;
    public String offHand;
    public String attitude = "PASSIVE";
    public boolean rotateTowardsPlayer;
    public String movementType = "IDLE";
    public float movementRadius;
    public float walkSpeed = 1.0f;
    public List<WaypointData> waypoints;
    public String pathShape = "LOOP";
    public float waypointPause = 0.0f;
    public List<CommandAction> deathCommands;
    public float messageDelay = 0.0f;
    public List<AnimationConfig> animations;
    public boolean hideNametag;
    public float nametagOffset;
    public boolean fKeyInteractionEnabled;
    public List<ConditionalDialog> dialogs;
    public String permission;
    public String noPermissionMessage;
    public List<String> interactCommands;
    public List<CommandAction> commandActions;
    public List<String> messages;
    public String messageSelectionMode = "RANDOM";
    public boolean hideNpc;
    public String shopId;
    public String sellShopId;
    public boolean bankingEnabled;
    public String nametagColor;
    public String nametagFormat;
    public String spawnParticles;
    public String despawnParticles;
    public float spawnFxDuration;
    public float despawnFxDuration;
    public Map<String, String> attachmentIds;
    public String gradientSet;
    public String gradientId;
    public PlayerSkin customSkin;
    public List<ContextMenuAction> mapContextActions;
    public List<ProximityReaction> proximityReactions;
    public List<ScheduleEntry> schedule;
    public Map<String, BeaconReaction> beaconReactions;
    public List<EmoteReaction> emoteReactions;
    public List<AppearanceOverride> appearanceOverrides;
    public boolean takesDamage;
    public boolean invulnerable;
    public boolean respawnOnDeath = true;
    public float respawnDelay = 5.0f;
    public transient Map<UUID, String> playerMarkers = new ConcurrentHashMap<UUID, String>();
    public transient UUID spawnedEntityUUID;
    public transient Ref<EntityStore> entityRef;
    public transient int cachedNetworkId;
    public transient long createdAt;
    public transient String customSourceFile;
    public transient double resolvedPosX;
    public transient double resolvedPosY;
    public transient double resolvedPosZ;

    public void migrateCommands() {
        if (this.commandActions == null && this.interactCommands != null && !this.interactCommands.isEmpty()) {
            this.commandActions = new ArrayList<CommandAction>();
            for (String cmd : this.interactCommands) {
                if (cmd == null || cmd.trim().isEmpty()) continue;
                this.commandActions.add(new CommandAction(cmd.trim(), true));
            }
            this.interactCommands = null;
        }
    }

    public String getDisplayName() {
        return this.name != null ? this.name : this.id;
    }

    public boolean hasValidPosition() {
        return this.worldName != null && !this.worldName.isEmpty();
    }

    public MovementType getMovementType() {
        if (this.movementType == null) {
            return MovementType.IDLE;
        }
        try {
            return MovementType.valueOf(this.movementType.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            return MovementType.IDLE;
        }
    }

    public AttitudeType getAttitudeType() {
        if (this.attitude == null) {
            return AttitudeType.PASSIVE;
        }
        try {
            return AttitudeType.valueOf(this.attitude.toUpperCase());
        }
        catch (IllegalArgumentException e) {
            return AttitudeType.PASSIVE;
        }
    }

    public String resolveRoleName() {
        if (this.npcRoleId != null && !this.npcRoleId.isEmpty() && !"Empty_Role".equals(this.npcRoleId)) {
            String resolved = this.npcRoleId;
            if (resolved.startsWith("Citizen_")) {
                resolved = resolved.replace("Citizen_Wander_Passive_", "KS_NPC_Wander_").replace("Citizen_Wander_Generic_", "KS_NPC_WanderI_").replace("Citizen_Path_", "KS_Path_").replace("Citizen_Interactable_Role", "KS_NPC_Interactable_Role").replace("Citizen_Idle_Role", "KS_NPC_Idle_Role");
            }
            return resolved;
        }
        MovementType movement = this.getMovementType();
        if (movement == MovementType.WANDER || movement == MovementType.WANDER_CIRCLE) {
            int snappedRadius = CitizenData.snapRadius(this.movementRadius);
            if (this.fKeyInteractionEnabled) {
                return "KS_NPC_WanderI_R" + snappedRadius + "_Interactable_Role";
            }
            return "KS_NPC_Wander_R" + snappedRadius + "_Role";
        }
        if (movement == MovementType.PATH) {
            String safeId = this.id.replaceAll("[^a-zA-Z0-9_]", "_");
            int delay = Math.round(this.waypointPause);
            int speedKey = Math.round(this.walkSpeed * 10.0f);
            String base = "KS_Path_" + safeId;
            if (delay > 0) {
                base = base + "_D" + delay;
            }
            if (speedKey != 10) {
                base = base + "_S" + speedKey;
            }
            if (this.fKeyInteractionEnabled) {
                return base + "_Interactable_Role";
            }
            return base + "_Role";
        }
        if (movement == MovementType.WANDER_RECT) {
            int snappedRadius = CitizenData.snapRadius(this.movementRadius);
            if (this.fKeyInteractionEnabled) {
                return "KS_NPC_WanderI_R" + snappedRadius + "_Interactable_Role";
            }
            return "KS_NPC_Wander_R" + snappedRadius + "_Role";
        }
        if (this.fKeyInteractionEnabled) {
            return "KS_NPC_Interactable_Role";
        }
        return "KS_NPC_Idle_Role";
    }

    private static int snapRadius(float radius) {
        if (radius < 3.0f) {
            return 2;
        }
        if (radius < 7.0f) {
            return 5;
        }
        if (radius < 12.0f) {
            return 10;
        }
        return 15;
    }

    public static class CommandAction {
        public String command;
        public boolean runAsServer = true;

        public CommandAction() {
        }

        public CommandAction(String command, boolean runAsServer) {
            this.command = command;
            this.runAsServer = runAsServer;
        }
    }

    public static enum MovementType {
        IDLE,
        WANDER,
        WANDER_CIRCLE,
        WANDER_RECT,
        PATH;

    }

    public static enum AttitudeType {
        PASSIVE,
        NEUTRAL,
        AGGRESSIVE;

    }

    public static class AppearanceOverride {
        public String condition;
        public String entityTypeId;
        public String skinUsername;
        public String helmet;
        public String chest;
        public String leggings;
        public String gloves;
        public String mainHand;
        public String offHand;
    }

    public static class EmoteReaction {
        public String trigger;
        public String animationName;
        public int animationSlot = 4;
        public float cooldown = 10.0f;
    }

    public static class BeaconReaction {
        public String type;
        public String value;
    }

    public static class ScheduleEntry {
        public String period;
        public String movementType;
        public float movementRadius;
        public boolean hidden;
        public String attitude;
    }

    public static class ProximityReaction {
        public float range = 8.0f;
        public String type;
        public String value;
        public int cooldownSeconds = 30;
        public boolean onEnter = true;
        public boolean onExit;
    }

    public static class ContextMenuAction {
        public String name;
        public String command;

        public ContextMenuAction() {
        }

        public ContextMenuAction(String name, String command) {
            this.name = name;
            this.command = command;
        }
    }

    public static class AnimationConfig {
        public String type;
        public String animationName;
        public int animationSlot;
        public float interval;
        public float proximityRange;
        public boolean stopAfterTime;
        public String stopAnimation;
        public float stopTime;
    }

    public static class WaypointData {
        public double x;
        public double y;
        public double z;
        public float rotY;
        public float pauseTime;

        public WaypointData() {
        }

        public WaypointData(double x, double y, double z, float rotY, float pauseTime) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.rotY = rotY;
            this.pauseTime = pauseTime;
        }
    }

    public static class DialogCondition {
        public String type;
        public String value;
        public boolean negate;
    }

    public static class ConditionalDialog {
        public String dialogId;
        public DialogCondition condition;
    }
}

