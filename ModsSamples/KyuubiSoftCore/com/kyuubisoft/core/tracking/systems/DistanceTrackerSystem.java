/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Archetype
 *  com.hypixel.hytale.component.ArchetypeChunk
 *  com.hypixel.hytale.component.CommandBuffer
 *  com.hypixel.hytale.component.ComponentType
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.component.system.tick.EntityTickingSystem
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.kyuubisoft.core.tracking.systems;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.tracking.TrackingService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class DistanceTrackerSystem
extends EntityTickingSystem<EntityStore> {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core Tracking");
    private static final ComponentType<EntityStore, Player> PLAYER_TYPE = Player.getComponentType();
    private static final ComponentType<EntityStore, PlayerRef> PLAYER_REF_TYPE = PlayerRef.getComponentType();
    private static final ComponentType<EntityStore, TransformComponent> TRANSFORM_TYPE = TransformComponent.getComponentType();
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final double MIN_DISTANCE = 0.15;
    private static final double MAX_DISTANCE_PER_CHECK = 100.0;
    private final Map<UUID, Vector3d> lastPositions = new ConcurrentHashMap<UUID, Vector3d>();
    private final Map<UUID, Double> accumulatedDistance = new ConcurrentHashMap<UUID, Double>();
    private int tickCounter = 0;

    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return false;
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return Archetype.of((ComponentType[])new ComponentType[]{PLAYER_TYPE, PLAYER_REF_TYPE, TRANSFORM_TYPE});
    }

    public void tick(float deltaTime, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (index == 0) {
            ++this.tickCounter;
        }
        if (this.tickCounter < 20) {
            return;
        }
        if (index == 0) {
            this.tickCounter = 0;
        }
        try {
            Player player = (Player)chunk.getComponent(index, PLAYER_TYPE);
            if (player == null) {
                return;
            }
            PlayerRef playerRef = (PlayerRef)chunk.getComponent(index, PLAYER_REF_TYPE);
            if (playerRef == null) {
                return;
            }
            TransformComponent transform = (TransformComponent)chunk.getComponent(index, TRANSFORM_TYPE);
            if (transform == null) {
                return;
            }
            UUID playerId = playerRef.getUuid();
            Vector3d currentPos = transform.getPosition();
            if (currentPos == null) {
                return;
            }
            double cx = currentPos.x;
            double cy = currentPos.y;
            double cz = currentPos.z;
            Vector3d lastPos = this.lastPositions.get(playerId);
            if (lastPos == null) {
                this.lastPositions.put(playerId, new Vector3d(cx, cy, cz));
                this.accumulatedDistance.putIfAbsent(playerId, 0.0);
                return;
            }
            double dx = cx - lastPos.x;
            double dz = cz - lastPos.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            lastPos.assign(cx, cy, cz);
            if (distance < 0.15) {
                return;
            }
            if (distance > 100.0) {
                LOGGER.fine("[DistanceTracker] Filtered large distance: " + String.format("%.1f", distance));
                return;
            }
            double total = this.accumulatedDistance.getOrDefault(playerId, 0.0) + distance;
            int wholeBlocks = (int)total;
            if (wholeBlocks > 0) {
                this.accumulatedDistance.put(playerId, total - (double)wholeBlocks);
                TrackingService service = TrackingService.getInstance();
                if (service != null) {
                    service.dispatchDistanceTraveled(player, wholeBlocks);
                }
            } else {
                this.accumulatedDistance.put(playerId, total);
            }
        }
        catch (Exception e) {
            LOGGER.fine("DistanceTrackerSystem error: " + e.getMessage());
        }
    }

    public void clearPlayer(UUID playerId) {
        this.lastPositions.remove(playerId);
        this.accumulatedDistance.remove(playerId);
    }
}

