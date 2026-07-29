/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.protocol.AnimationSlot
 *  com.hypixel.hytale.protocol.ToClientPacket
 *  com.hypixel.hytale.protocol.packets.entities.PlayAnimation
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.command.system.CommandManager
 *  com.hypixel.hytale.server.core.command.system.CommandSender
 *  com.hypixel.hytale.server.core.console.ConsoleSender
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
 *  com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.entities.PlayAnimation;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.kyuubisoft.core.citizen.CitizenData;
import com.kyuubisoft.core.citizen.CitizenService;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class CitizenProximityManager {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft CitizenProximity");
    private final Map<String, Set<UUID>> nearbyPlayers = new ConcurrentHashMap<String, Set<UUID>>();
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<String, Long>();

    public void tick(Collection<CitizenData> citizens, Collection<Player> players, CitizenService service) {
        long now = System.currentTimeMillis();
        for (CitizenData citizen : citizens) {
            if (citizen.proximityReactions == null || citizen.proximityReactions.isEmpty() || citizen.spawnedEntityUUID == null) continue;
            Set current = this.nearbyPlayers.computeIfAbsent(citizen.id, k -> ConcurrentHashMap.newKeySet());
            HashSet<UUID> stillNear = new HashSet<UUID>();
            for (Player player : players) {
                try {
                    boolean inMaxRange;
                    Vector3d pos;
                    PlayerRef ref = player.getPlayerRef();
                    if (ref == null) continue;
                    UUID playerId = ref.getUuid();
                    TransformComponent transform = (TransformComponent)ref.getComponent(TransformComponent.getComponentType());
                    if (transform == null || (pos = transform.getPosition()) == null) continue;
                    double dx = pos.x - citizen.resolvedPosX;
                    double dz = pos.z - citizen.resolvedPosZ;
                    double distSq = dx * dx + dz * dz;
                    for (int i = 0; i < citizen.proximityReactions.size(); ++i) {
                        CitizenData.ProximityReaction reaction = citizen.proximityReactions.get(i);
                        float rangeSq = reaction.range * reaction.range;
                        boolean inRange = distSq <= (double)rangeSq;
                        boolean wasNear = current.contains(playerId);
                        if (inRange && !wasNear && reaction.onEnter) {
                            if (!this.checkCooldown(citizen.id, playerId, i, reaction.cooldownSeconds, now)) continue;
                            this.dispatchReaction(player, citizen, reaction, (float)Math.sqrt(distSq));
                            continue;
                        }
                        if (inRange || !wasNear || !reaction.onExit || !this.checkCooldown(citizen.id, playerId, i, reaction.cooldownSeconds, now)) continue;
                        this.dispatchReaction(player, citizen, reaction, -1.0f);
                    }
                    float maxRange = 0.0f;
                    for (CitizenData.ProximityReaction r : citizen.proximityReactions) {
                        if (!(r.range > maxRange)) continue;
                        maxRange = r.range;
                    }
                    boolean bl = inMaxRange = distSq <= (double)(maxRange * maxRange);
                    if (inMaxRange) {
                        stillNear.add(playerId);
                        if (current.contains(playerId)) continue;
                        service.dispatchProximityEnter(player, citizen.id, (float)Math.sqrt(distSq));
                        continue;
                    }
                    if (!current.contains(playerId)) continue;
                    service.dispatchProximityExit(player, citizen.id);
                }
                catch (Exception e) {
                    LOGGER.fine("Proximity tick error for citizen " + citizen.id + ": " + e.getMessage());
                }
            }
            current.clear();
            current.addAll(stillNear);
        }
    }

    private boolean checkCooldown(String citizenId, UUID playerId, int reactionIdx, int cooldownSec, long now) {
        String key = citizenId + ":" + String.valueOf(playerId) + ":" + reactionIdx;
        Long last = this.cooldowns.get(key);
        if (last != null && now - last < (long)cooldownSec * 1000L) {
            return false;
        }
        this.cooldowns.put(key, now);
        return true;
    }

    private void dispatchReaction(Player player, CitizenData citizen, CitizenData.ProximityReaction reaction, float distance) {
        if (reaction.type == null || reaction.value == null) {
            return;
        }
        try {
            switch (reaction.type.toLowerCase()) {
                case "message": {
                    player.sendMessage(Message.raw((String)reaction.value));
                    break;
                }
                case "command": {
                    CommandManager cmdManager = CommandManager.get();
                    if (cmdManager != null) {
                        String resolved = reaction.value.replace("{player}", player.getPlayerRef().getUsername()).replace("{citizen}", citizen.id);
                        cmdManager.handleCommand((CommandSender)ConsoleSender.INSTANCE, resolved);
                    }
                    break;
                }
                case "emote": {
                    NetworkId nid;
                    if (citizen.entityRef == null || (nid = (NetworkId)citizen.entityRef.getStore().getComponent(citizen.entityRef, NetworkId.getComponentType())) == null) break;
                    PlayAnimation packet = new PlayAnimation(nid.getId(), null, reaction.value, AnimationSlot.Emote);
                    PlayerRef pRef = player.getPlayerRef();
                    if (pRef != null) {
                        pRef.getPacketHandler().writeNoCache((ToClientPacket)packet);
                    }
                    break;
                }
                case "sound": {
                    LOGGER.fine("Sound reaction not yet implemented: " + reaction.value + " for citizen " + citizen.id);
                }
            }
        }
        catch (Exception e) {
            LOGGER.warning("Failed to dispatch proximity reaction '" + reaction.type + "' for citizen " + citizen.id + ": " + e.getMessage());
        }
    }

    public void unregister(String citizenId) {
        this.nearbyPlayers.remove(citizenId);
        this.cooldowns.keySet().removeIf(k -> k.startsWith(citizenId + ":"));
    }

    public void clearAll() {
        this.nearbyPlayers.clear();
        this.cooldowns.clear();
    }
}

