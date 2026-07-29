/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.protocol.ComponentUpdate
 *  com.hypixel.hytale.protocol.Direction
 *  com.hypixel.hytale.protocol.EntityUpdate
 *  com.hypixel.hytale.protocol.ModelTransform
 *  com.hypixel.hytale.protocol.ToClientPacket
 *  com.hypixel.hytale.protocol.TransformUpdate
 *  com.hypixel.hytale.protocol.packets.entities.EntityUpdates
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
 *  com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ComponentUpdate;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.EntityUpdate;
import com.hypixel.hytale.protocol.ModelTransform;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.TransformUpdate;
import com.hypixel.hytale.protocol.packets.entities.EntityUpdates;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.citizen.CitizenData;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class CitizenRotationManager {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft CitizenRotation");
    private static final double MAX_ROTATION_DISTANCE = 25.0;
    private static final double MAX_ROTATION_DISTANCE_SQ = 625.0;
    private static final float YAW_THRESHOLD = 0.015f;
    private static final float PITCH_THRESHOLD = 0.015f;
    private final Map<String, Map<UUID, Direction>> lastDirections = new ConcurrentHashMap<String, Map<UUID, Direction>>();
    private final AtomicBoolean firstPacketLogged = new AtomicBoolean(false);

    public void tick(Collection<CitizenData> citizens, Collection<Player> onlinePlayers) {
        if (onlinePlayers.isEmpty()) {
            return;
        }
        for (CitizenData citizen : citizens) {
            if (!citizen.rotateTowardsPlayer || citizen.entityRef == null || citizen.getMovementType() != CitizenData.MovementType.IDLE) continue;
            try {
                this.rotateCitizenToPlayers(citizen, onlinePlayers);
            }
            catch (Exception exception) {}
        }
    }

    private void rotateCitizenToPlayers(CitizenData citizen, Collection<Player> players) {
        Ref<EntityStore> entityRef = citizen.entityRef;
        if (!entityRef.isValid()) {
            return;
        }
        if (citizen.cachedNetworkId <= 0) {
            return;
        }
        int netId = citizen.cachedNetworkId;
        Vector3d npcPos = new Vector3d(citizen.resolvedPosX, citizen.resolvedPosY, citizen.resolvedPosZ);
        Map citizenDirs = this.lastDirections.computeIfAbsent(citizen.id, k -> new ConcurrentHashMap());
        for (Player player : players) {
            try {
                PlayerRef playerRef = player.getPlayerRef();
                if (playerRef == null) continue;
                UUID playerId = playerRef.getUuid();
                Vector3d playerPos = new Vector3d(playerRef.getTransform().getPosition());
                double dx = playerPos.x - npcPos.x;
                double dz = playerPos.z - npcPos.z;
                double distSq = dx * dx + dz * dz;
                if (distSq > 625.0) continue;
                float yaw = (float)(Math.atan2(dx, dz) + Math.PI);
                double dy = playerPos.y - npcPos.y;
                double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
                float pitch = (float)Math.atan2(dy, horizontalDistance);
                Direction lastDir = (Direction)citizenDirs.get(playerId);
                if (lastDir != null) {
                    float yawDiff = Math.abs(yaw - lastDir.yaw);
                    float pitchDiff = Math.abs(pitch - lastDir.pitch);
                    if (yawDiff < 0.015f && pitchDiff < 0.015f) continue;
                }
                Direction lookDirection = new Direction(yaw, pitch, 0.0f);
                Direction bodyDirection = new Direction(yaw, 0.0f, 0.0f);
                ModelTransform transform = new ModelTransform();
                transform.lookOrientation = lookDirection;
                transform.bodyOrientation = bodyDirection;
                TransformUpdate update = new TransformUpdate(transform);
                EntityUpdate entityUpdate = new EntityUpdate(netId, null, new ComponentUpdate[]{update});
                EntityUpdates packet = new EntityUpdates(null, new EntityUpdate[]{entityUpdate});
                playerRef.getPacketHandler().write((ToClientPacket)packet);
                citizenDirs.put(playerId, lookDirection);
                if (!this.firstPacketLogged.compareAndSet(false, true)) continue;
                LOGGER.fine("[ROT] First rotation packet sent! citizen=" + citizen.id + " netId=" + netId + " yaw=" + String.format("%.3f", Float.valueOf(yaw)) + " pitch=" + String.format("%.3f", Float.valueOf(pitch)) + " to player " + playerRef.getUsername());
            }
            catch (Exception exception) {}
        }
    }

    public void rotateToPlayer(CitizenData citizen, Player player) {
        if (citizen.entityRef == null || !citizen.entityRef.isValid()) {
            return;
        }
        try {
            Store store = citizen.entityRef.getStore();
            TransformComponent npcTransform = (TransformComponent)store.getComponent(citizen.entityRef, TransformComponent.getComponentType());
            if (npcTransform == null) {
                return;
            }
            NetworkId networkId = (NetworkId)store.getComponent(citizen.entityRef, NetworkId.getComponentType());
            if (networkId == null) {
                return;
            }
            Vector3d npcPos = npcTransform.getPosition();
            PlayerRef playerRef = player.getPlayerRef();
            Vector3d playerPos = new Vector3d(playerRef.getTransform().getPosition());
            double dx = playerPos.x - npcPos.x;
            double dz = playerPos.z - npcPos.z;
            double dy = playerPos.y - npcPos.y;
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float)(Math.atan2(dx, dz) + Math.PI);
            float pitch = (float)Math.atan2(dy, horizontalDistance);
            Direction lookDirection = new Direction(yaw, pitch, 0.0f);
            Direction bodyDirection = new Direction(yaw, 0.0f, 0.0f);
            ModelTransform transform = new ModelTransform();
            transform.lookOrientation = lookDirection;
            transform.bodyOrientation = bodyDirection;
            TransformUpdate update = new TransformUpdate(transform);
            EntityUpdate entityUpdate = new EntityUpdate(networkId.getId(), null, new ComponentUpdate[]{update});
            EntityUpdates packet = new EntityUpdates(null, new EntityUpdate[]{entityUpdate});
            playerRef.getPacketHandler().write((ToClientPacket)packet);
            LOGGER.fine("[ROT] One-time rotation for " + citizen.id + " \u2192 " + playerRef.getUsername());
        }
        catch (Exception e) {
            LOGGER.fine("[ROT] One-time rotation error: " + e.getMessage());
        }
    }

    public void clearCitizen(String citizenId) {
        this.lastDirections.remove(citizenId);
    }

    public void clearAll() {
        this.lastDirections.clear();
    }
}

