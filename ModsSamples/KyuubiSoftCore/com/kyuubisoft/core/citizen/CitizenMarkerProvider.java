/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.protocol.Direction
 *  com.hypixel.hytale.protocol.FormattedMessage
 *  com.hypixel.hytale.protocol.Position
 *  com.hypixel.hytale.protocol.Transform
 *  com.hypixel.hytale.protocol.packets.worldmap.ContextMenuItem
 *  com.hypixel.hytale.protocol.packets.worldmap.MapMarker
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager$MarkerProvider
 *  com.hypixel.hytale.server.core.universe.world.worldmap.markers.MarkersCollector
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.Transform;
import com.hypixel.hytale.protocol.packets.worldmap.ContextMenuItem;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MarkersCollector;
import com.kyuubisoft.core.citizen.CitizenData;
import com.kyuubisoft.core.citizen.CitizenService;
import com.kyuubisoft.core.i18n.CoreI18n;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class CitizenMarkerProvider
implements WorldMapManager.MarkerProvider {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Markers");
    private final CitizenService citizenService;

    public CitizenMarkerProvider(CitizenService citizenService) {
        this.citizenService = citizenService;
    }

    public void update(World world, Player player, MarkersCollector collector) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getPlayerRef().getUuid();
        Map<String, CitizenData> markedCitizens = this.citizenService.getCitizensWithMarkerForPlayer(playerId);
        for (Map.Entry<String, CitizenData> entry : markedCitizens.entrySet()) {
            Vector3d pos;
            CitizenData citizen = entry.getValue();
            String markerType = this.citizenService.getMarker(citizen.id, playerId);
            if (markerType == null || citizen.worldName != null && !CitizenService.matchesWorld(citizen.worldName, world.getName()) || (pos = citizen.spawnRelative ? this.citizenService.resolvePosition(citizen, world) : new Vector3d(citizen.posX, citizen.posY, citizen.posZ)) == null || !collector.isInViewDistance(pos)) continue;
            String markerImage = this.getMarkerImage(markerType);
            String markerName = this.getMarkerName(citizen, markerType);
            String markerId = "citizen_" + citizen.id;
            FormattedMessage nameMsg = new FormattedMessage();
            nameMsg.rawText = markerName;
            Transform transform = new Transform(new Position(pos.x, pos.y, pos.z), new Direction(citizen.rotY, 0.0f, 0.0f));
            ContextMenuItem[] contextItems = this.buildContextMenuItems(citizen);
            collector.add(new MapMarker(markerId, nameMsg, markerImage, transform, contextItems, null));
        }
    }

    private ContextMenuItem[] buildContextMenuItems(CitizenData citizen) {
        if (citizen.mapContextActions == null || citizen.mapContextActions.isEmpty()) {
            return null;
        }
        ContextMenuItem[] items = new ContextMenuItem[citizen.mapContextActions.size()];
        for (int i = 0; i < citizen.mapContextActions.size(); ++i) {
            CitizenData.ContextMenuAction action = citizen.mapContextActions.get(i);
            items[i] = new ContextMenuItem(action.name, action.command);
        }
        return items;
    }

    private String getMarkerImage(String markerType) {
        return switch (markerType) {
            case "quest_available" -> "Quest_Available.png";
            case "quest_turn_in" -> "Quest_TurnIn.png";
            case "quest_progress" -> "Quest_Progress.png";
            default -> "Player.png";
        };
    }

    private String getMarkerName(CitizenData citizen, String markerType) {
        CoreI18n i18n = CoreI18n.getInstance();
        String citizenName = citizen.name != null ? i18n.get(citizen.name) : citizen.id;
        String suffix = switch (markerType) {
            case "quest_available" -> " (!)";
            case "quest_turn_in" -> " (?)";
            case "quest_progress" -> " (...)";
            default -> "";
        };
        return citizenName + suffix;
    }
}

