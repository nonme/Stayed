/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.ArchetypeChunk
 *  com.hypixel.hytale.component.CommandBuffer
 *  com.hypixel.hytale.component.ComponentType
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.component.system.EntityEventSystem
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.event.events.ecs.DiscoverZoneEvent
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.WorldMapTracker$ZoneDiscoveryInfo
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.kyuubisoft.core.tracking.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DiscoverZoneEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.tracking.TrackingService;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class ZoneDiscoveryTrackerSystem
extends EntityEventSystem<EntityStore, DiscoverZoneEvent> {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core Tracking");
    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> playerRefComponentType = PlayerRef.getComponentType();

    public ZoneDiscoveryTrackerSystem() {
        super(DiscoverZoneEvent.class);
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return this.playerRefComponentType;
    }

    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull DiscoverZoneEvent event) {
        try {
            PlayerRef playerRef = (PlayerRef)chunk.getComponent(index, this.playerRefComponentType);
            if (playerRef == null) {
                return;
            }
            Player player = (Player)playerRef.getComponent(Player.getComponentType());
            if (player == null) {
                return;
            }
            WorldMapTracker.ZoneDiscoveryInfo discoveryInfo = event.getDiscoveryInfo();
            if (discoveryInfo == null) {
                return;
            }
            String zoneName = discoveryInfo.zoneName();
            if (zoneName == null || zoneName.isEmpty()) {
                return;
            }
            LOGGER.fine("Zone discovered: " + zoneName + " by " + player.getDisplayName());
            TrackingService service = TrackingService.getInstance();
            if (service != null) {
                service.dispatchZoneDiscovered(player, zoneName);
            }
        }
        catch (Exception e) {
            LOGGER.fine("ZoneDiscoveryTrackerSystem error: " + e.getMessage());
        }
    }
}

