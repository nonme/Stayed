/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.protocol.AnimationSlot
 *  com.hypixel.hytale.protocol.Position
 *  com.hypixel.hytale.protocol.SoundCategory
 *  com.hypixel.hytale.protocol.ToClientPacket
 *  com.hypixel.hytale.protocol.packets.entities.PlayAnimation
 *  com.hypixel.hytale.protocol.packets.world.PlaySoundEvent3D
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.entities.PlayAnimation;
import com.hypixel.hytale.protocol.packets.world.PlaySoundEvent3D;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.kyuubisoft.core.citizen.CitizenData;
import com.kyuubisoft.core.citizen.CitizenService;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class CitizenBeaconManager {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft CitizenBeacon");
    private static final double MESSAGE_RANGE = 32.0;
    private static final double MESSAGE_RANGE_SQ = 1024.0;
    private final Map<String, Set<String>> activeBeacons = new ConcurrentHashMap<String, Set<String>>();

    public void sendBeacon(CitizenData citizen, String message, CitizenService service, Collection<Player> players) {
        if (citizen == null) {
            return;
        }
        CitizenData.BeaconReaction reaction = null;
        if (citizen.beaconReactions != null) {
            reaction = citizen.beaconReactions.get(message);
        }
        if (reaction != null && reaction.type != null) {
            this.dispatchReaction(citizen, reaction, players);
        }
        this.activeBeacons.computeIfAbsent(citizen.id, k -> ConcurrentHashMap.newKeySet()).add(message);
        service.dispatchBeaconReceived(citizen.id, message);
    }

    private void dispatchReaction(CitizenData citizen, CitizenData.BeaconReaction reaction, Collection<Player> players) {
        switch (reaction.type.toLowerCase()) {
            case "animation": {
                this.playAnimation(citizen, reaction.value, players);
                break;
            }
            case "sound": {
                this.playSound(citizen, reaction.value, players);
                break;
            }
            case "message": {
                this.sendNearbyMessage(citizen, reaction.value, players);
                break;
            }
            case "appearance": {
                this.storeAppearanceOverride(citizen, reaction.value);
                break;
            }
            default: {
                LOGGER.fine("Unknown beacon reaction type: " + reaction.type);
            }
        }
    }

    private void playAnimation(CitizenData citizen, String animationName, Collection<Player> players) {
        if (citizen.cachedNetworkId == 0 || animationName == null) {
            return;
        }
        try {
            PlayAnimation packet = new PlayAnimation(citizen.cachedNetworkId, null, animationName, AnimationSlot.Emote);
            for (Player p : players) {
                try {
                    PlayerRef pRef = p.getPlayerRef();
                    if (pRef == null) continue;
                    pRef.getPacketHandler().writeNoCache((ToClientPacket)packet);
                }
                catch (Exception exception) {}
            }
        }
        catch (Exception e) {
            LOGGER.fine("Failed to play beacon animation: " + e.getMessage());
        }
    }

    private void playSound(CitizenData citizen, String soundEventName, Collection<Player> players) {
        if (soundEventName == null || soundEventName.isEmpty()) {
            return;
        }
        try {
            int soundIndex = SoundEvent.getAssetMap().getIndexOrDefault((Object)soundEventName, -1);
            if (soundIndex < 0) {
                LOGGER.fine("[BEACON] Unknown sound event: " + soundEventName);
                return;
            }
            double px = citizen.resolvedPosX != 0.0 ? citizen.resolvedPosX : citizen.posX;
            double py = citizen.resolvedPosY != 0.0 ? citizen.resolvedPosY : citizen.posY;
            double pz = citizen.resolvedPosZ != 0.0 ? citizen.resolvedPosZ : citizen.posZ;
            PlaySoundEvent3D packet = new PlaySoundEvent3D(soundIndex, SoundCategory.SFX, new Position(px, py, pz), 1.0f, 1.0f);
            for (Player p : players) {
                try {
                    PlayerRef pRef = p.getPlayerRef();
                    if (pRef == null) continue;
                    pRef.getPacketHandler().writeNoCache((ToClientPacket)packet);
                }
                catch (Exception exception) {}
            }
        }
        catch (Exception e) {
            LOGGER.fine("[BEACON] Sound error for " + citizen.id + ": " + e.getMessage());
        }
    }

    private void sendNearbyMessage(CitizenData citizen, String text, Collection<Player> players) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String formatted = "[" + citizen.getDisplayName() + "] " + text;
        Message msg = Message.raw((String)formatted);
        double cx = citizen.resolvedPosX;
        double cy = citizen.resolvedPosY;
        double cz = citizen.resolvedPosZ;
        for (Player player : players) {
            try {
                TransformComponent transform;
                PlayerRef pRef = player.getPlayerRef();
                if (pRef == null || (transform = (TransformComponent)pRef.getComponent(TransformComponent.getComponentType())) == null) continue;
                Vector3d pos = transform.getPosition();
                double dx = pos.x - cx;
                double dy = pos.y - cy;
                double dz = pos.z - cz;
                if (!(dx * dx + dy * dy + dz * dz <= 1024.0)) continue;
                player.sendMessage(msg);
            }
            catch (Exception exception) {}
        }
    }

    private void storeAppearanceOverride(CitizenData citizen, String value) {
        this.activeBeacons.computeIfAbsent(citizen.id, k -> ConcurrentHashMap.newKeySet()).add("appearance:" + value);
        LOGGER.fine("Appearance override stored for " + citizen.id + ": " + value);
    }

    public void clearBeacon(String citizenId, String message) {
        Set<String> beacons = this.activeBeacons.get(citizenId);
        if (beacons != null) {
            beacons.remove(message);
            beacons.remove("appearance:" + message);
            if (beacons.isEmpty()) {
                this.activeBeacons.remove(citizenId);
            }
        }
    }

    public Set<String> getActiveBeacons(String citizenId) {
        Set<String> beacons = this.activeBeacons.get(citizenId);
        return beacons != null ? Collections.unmodifiableSet(beacons) : Collections.emptySet();
    }

    public void clearAll() {
        this.activeBeacons.clear();
    }
}

