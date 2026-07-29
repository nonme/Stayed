/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.protocol.AnimationSlot
 *  com.hypixel.hytale.protocol.ToClientPacket
 *  com.hypixel.hytale.protocol.packets.entities.PlayAnimation
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.entities.PlayAnimation;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.kyuubisoft.core.citizen.CitizenData;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class CitizenEmoteManager {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft CitizenEmote");
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<String, Long>();

    public void triggerEvent(String eventName, CitizenData citizen, Collection<Player> players) {
        if (citizen.emoteReactions == null || citizen.entityRef == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (CitizenData.EmoteReaction reaction : citizen.emoteReactions) {
            String cooldownKey;
            Long lastTrigger;
            if (!eventName.equals(reaction.trigger) || (lastTrigger = this.cooldowns.get(cooldownKey = citizen.id + ":" + reaction.trigger)) != null && now - lastTrigger < (long)(reaction.cooldown * 1000.0f)) continue;
            this.cooldowns.put(cooldownKey, now);
            this.sendAnimation(citizen, reaction, players);
        }
    }

    public void triggerEventForAll(String eventName, Collection<CitizenData> citizens, Collection<Player> players) {
        for (CitizenData citizen : citizens) {
            this.triggerEvent(eventName, citizen, players);
        }
    }

    private void sendAnimation(CitizenData citizen, CitizenData.EmoteReaction reaction, Collection<Player> players) {
        try {
            if (citizen.cachedNetworkId == 0) {
                return;
            }
            AnimationSlot slot = switch (reaction.animationSlot) {
                case 0 -> AnimationSlot.Movement;
                case 1 -> AnimationSlot.Status;
                case 2 -> AnimationSlot.Action;
                case 3 -> AnimationSlot.Face;
                default -> AnimationSlot.Emote;
            };
            PlayAnimation packet = new PlayAnimation(citizen.cachedNetworkId, null, reaction.animationName, slot);
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
            LOGGER.fine("Failed to play emote for " + citizen.id + ": " + e.getMessage());
        }
    }

    public void clearAll() {
        this.cooldowns.clear();
    }
}

