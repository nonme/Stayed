/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Component
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.RemoveReason
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.math.vector.Vector3f
 *  com.hypixel.hytale.server.core.entity.Entity
 *  com.hypixel.hytale.server.core.entity.nameplate.Nameplate
 *  com.hypixel.hytale.server.core.universe.world.World
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.universe.world.World;
import com.kyuubisoft.core.citizen.CitizenData;
import com.kyuubisoft.core.citizen.CitizenService;
import com.kyuubisoft.core.citizen.QuestMarkerEntity;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QuestMarkerManager {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft QuestMarkers");
    private static final double MARKER_Y_OFFSET = 2.2;
    private static final double EXTRA_PER_SCALE = 0.4;
    private final Map<String, MarkerData> markers = new ConcurrentHashMap<String, MarkerData>();

    public void setMarker(String citizenId, UUID playerId, String markerText, World world) {
        MarkerData existing = this.markers.get(citizenId);
        if (existing != null && existing.entity != null) {
            existing.entity.addVisiblePlayer(playerId);
            if (!markerText.equals(existing.entity.getMarkerText())) {
                existing.entity.setMarkerText(markerText);
                this.updateNameplate(existing, markerText);
            }
            return;
        }
        CitizenService citizenService = CitizenService.getInstance();
        if (citizenService == null) {
            return;
        }
        CitizenData citizen = citizenService.getCitizen(citizenId);
        if (citizen == null) {
            return;
        }
        try {
            Vector3d citizenPos = citizenService.resolvePosition(citizen, world);
            double scale = Math.max(0.01, (double)citizen.scale);
            double yOffset = 2.2 * scale + (scale - 1.0) * 0.4 + (double)citizen.nametagOffset;
            Vector3d markerPos = new Vector3d(citizenPos.x, citizenPos.y + yOffset, citizenPos.z);
            QuestMarkerEntity entity = new QuestMarkerEntity(world);
            entity.setCitizenId(citizenId);
            entity.setMarkerText(markerText);
            entity.addVisiblePlayer(playerId);
            world.spawnEntity((Entity)entity, markerPos, new Vector3f(0.0f, 0.0f, 0.0f));
            Ref ref = entity.getReference();
            if (ref != null) {
                Store store = ref.getStore();
                store.addComponent(ref, Nameplate.getComponentType(), (Component)new Nameplate(markerText));
            }
            MarkerData data = new MarkerData();
            data.entity = entity;
            data.citizenId = citizenId;
            this.markers.put(citizenId, data);
            LOGGER.info("Quest marker '" + markerText + "' created above " + citizenId + " for player " + String.valueOf(playerId));
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create quest marker for " + citizenId, e);
        }
    }

    public void removeMarker(String citizenId, UUID playerId, World world) {
        MarkerData data = this.markers.get(citizenId);
        if (data == null || data.entity == null) {
            return;
        }
        data.entity.removeVisiblePlayer(playerId);
        if (data.entity.getVisiblePlayers().isEmpty()) {
            this.despawnMarker(citizenId);
        }
    }

    public void removeAllForPlayer(UUID playerId, World world) {
        for (Map.Entry<String, MarkerData> entry : new ArrayList<Map.Entry<String, MarkerData>>(this.markers.entrySet())) {
            QuestMarkerEntity entity = entry.getValue().entity;
            if (entity == null || !entity.isVisibleTo(playerId)) continue;
            entity.removeVisiblePlayer(playerId);
            if (!entity.getVisiblePlayers().isEmpty()) continue;
            this.despawnMarker(entry.getKey());
        }
    }

    public void despawnMarker(String citizenId) {
        MarkerData data = this.markers.remove(citizenId);
        if (data == null || data.entity == null) {
            return;
        }
        try {
            Ref ref = data.entity.getReference();
            if (ref != null) {
                Store store = ref.getStore();
                store.removeEntity(ref, RemoveReason.REMOVE);
            }
            LOGGER.fine("Despawned quest marker for " + citizenId);
        }
        catch (Exception e) {
            LOGGER.fine("Failed to despawn quest marker: " + e.getMessage());
        }
    }

    public void removeAll() {
        for (String citizenId : new ArrayList<String>(this.markers.keySet())) {
            this.despawnMarker(citizenId);
        }
    }

    public boolean hasMarker(String citizenId) {
        return this.markers.containsKey(citizenId);
    }

    public QuestMarkerEntity getMarker(String citizenId) {
        MarkerData data = this.markers.get(citizenId);
        return data != null ? data.entity : null;
    }

    private void updateNameplate(MarkerData data, String text) {
        try {
            Store store;
            Nameplate np;
            Ref ref = data.entity.getReference();
            if (ref != null && (np = (Nameplate)(store = ref.getStore()).getComponent(ref, Nameplate.getComponentType())) != null) {
                np.setText(text);
            }
        }
        catch (Exception e) {
            LOGGER.fine("Failed to update marker nameplate: " + e.getMessage());
        }
    }

    private static class MarkerData {
        QuestMarkerEntity entity;
        String citizenId;

        private MarkerData() {
        }
    }
}

