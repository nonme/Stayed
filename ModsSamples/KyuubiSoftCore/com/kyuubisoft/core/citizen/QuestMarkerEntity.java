/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.ComponentAccessor
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.server.core.entity.Entity
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QuestMarkerEntity
extends Entity {
    private final Set<UUID> visibleToPlayers = ConcurrentHashMap.newKeySet();
    private String citizenId;
    private String markerText = "!";

    public QuestMarkerEntity() {
    }

    public QuestMarkerEntity(World world) {
        super(world);
    }

    public boolean isHiddenFromLivingEntity(Ref<EntityStore> selfRef, Ref<EntityStore> viewerRef, ComponentAccessor<EntityStore> accessor) {
        try {
            Player player = (Player)accessor.getComponent(viewerRef, Player.getComponentType());
            if (player != null) {
                UUID playerId = player.getPlayerRef().getUuid();
                return !this.visibleToPlayers.contains(playerId);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return true;
    }

    public boolean isCollidable() {
        return false;
    }

    public void addVisiblePlayer(UUID playerId) {
        this.visibleToPlayers.add(playerId);
    }

    public void removeVisiblePlayer(UUID playerId) {
        this.visibleToPlayers.remove(playerId);
    }

    public boolean isVisibleTo(UUID playerId) {
        return this.visibleToPlayers.contains(playerId);
    }

    public Set<UUID> getVisiblePlayers() {
        return this.visibleToPlayers;
    }

    public String getCitizenId() {
        return this.citizenId;
    }

    public void setCitizenId(String citizenId) {
        this.citizenId = citizenId;
    }

    public String getMarkerText() {
        return this.markerText;
    }

    public void setMarkerText(String text) {
        this.markerText = text;
    }
}

