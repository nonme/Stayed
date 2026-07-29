/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.ComponentAccessor
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.protocol.AnimationSlot
 *  com.hypixel.hytale.protocol.ToClientPacket
 *  com.hypixel.hytale.protocol.packets.entities.PlayAnimation
 *  com.hypixel.hytale.server.core.entity.AnimationUtils
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
 *  com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.entities.PlayAnimation;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.CorePlugin;
import com.kyuubisoft.core.citizen.CitizenData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class CitizenAnimationManager {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft CitizenAnims");
    private final Map<String, List<AnimTimerState>> timerStates = new ConcurrentHashMap<String, List<AnimTimerState>>();
    private final Map<String, Set<Integer>> activeProximity = new ConcurrentHashMap<String, Set<Integer>>();

    public void register(CitizenData citizen) {
        if (citizen.animations == null || citizen.animations.isEmpty()) {
            return;
        }
        ArrayList<AnimTimerState> timers = new ArrayList<AnimTimerState>();
        for (CitizenData.AnimationConfig anim : citizen.animations) {
            if (!"TIMED".equalsIgnoreCase(anim.type) && !"TIMED_RANDOM".equalsIgnoreCase(anim.type)) continue;
            AnimTimerState state = new AnimTimerState();
            state.config = anim;
            state.nextTriggerMs = System.currentTimeMillis() + (long)(anim.interval * 1000.0f);
            state.isRandom = "TIMED_RANDOM".equalsIgnoreCase(anim.type);
            timers.add(state);
        }
        if (!timers.isEmpty()) {
            this.timerStates.put(citizen.id, timers);
        }
    }

    public void unregister(String citizenId) {
        this.timerStates.remove(citizenId);
        this.activeProximity.remove(citizenId);
    }

    public void tick(Collection<CitizenData> citizens, Collection<Player> onlinePlayers) {
        long now = System.currentTimeMillis();
        for (CitizenData citizen : citizens) {
            if (citizen.entityRef == null || citizen.animations == null) continue;
            this.tickTimedAnimations(citizen, now, onlinePlayers);
            this.tickProximityAnimations(citizen, onlinePlayers);
        }
    }

    private void tickTimedAnimations(CitizenData citizen, long now, Collection<Player> players) {
        List<AnimTimerState> timers = this.timerStates.get(citizen.id);
        if (timers == null) {
            return;
        }
        for (AnimTimerState state : timers) {
            if (now < state.nextTriggerMs) continue;
            this.playAnimation(citizen.entityRef, state.config, players);
            float interval = state.config.interval;
            if (state.isRandom) {
                interval *= 0.5f + ThreadLocalRandom.current().nextFloat();
            }
            state.nextTriggerMs = now + (long)(interval * 1000.0f);
            if (!state.config.stopAfterTime || !(state.config.stopTime > 0.0f)) continue;
            state.stopAtMs = now + (long)(state.config.stopTime * 1000.0f);
            state.needsStop = true;
        }
        for (AnimTimerState state : timers) {
            if (!state.needsStop || now < state.stopAtMs) continue;
            this.stopAnimation(citizen.entityRef, state.config, players);
            state.needsStop = false;
        }
    }

    private void tickProximityAnimations(CitizenData citizen, Collection<Player> onlinePlayers) {
        if (citizen.entityRef == null) {
            return;
        }
        for (CitizenData.AnimationConfig anim : citizen.animations) {
            if (!"PROXIMITY".equalsIgnoreCase(anim.type) || anim.proximityRange <= 0.0f) continue;
            boolean playerNearby = this.isPlayerNearby(citizen, onlinePlayers, anim.proximityRange);
            Set active = this.activeProximity.computeIfAbsent(citizen.id, k -> ConcurrentHashMap.newKeySet());
            if (playerNearby && !active.contains(anim.animationSlot)) {
                this.playAnimation(citizen.entityRef, anim, onlinePlayers);
                active.add(anim.animationSlot);
                continue;
            }
            if (playerNearby || !active.contains(anim.animationSlot)) continue;
            this.stopAnimation(citizen.entityRef, anim, onlinePlayers);
            active.remove(anim.animationSlot);
        }
    }

    public void onInteract(CitizenData citizen) {
        if (citizen.entityRef == null || citizen.animations == null) {
            return;
        }
        Collection<Player> players = CorePlugin.getInstance().getOnlinePlayers();
        for (CitizenData.AnimationConfig anim : citizen.animations) {
            if (!"ON_INTERACT".equalsIgnoreCase(anim.type)) continue;
            this.playAnimation(citizen.entityRef, anim, players);
            float stopDelay = anim.stopTime > 0.0f ? anim.stopTime : 3.0f;
            List timers = this.timerStates.computeIfAbsent(citizen.id, k -> new ArrayList());
            AnimTimerState stopState = new AnimTimerState();
            stopState.config = anim;
            stopState.needsStop = true;
            stopState.stopAtMs = System.currentTimeMillis() + (long)(stopDelay * 1000.0f);
            timers.add(stopState);
        }
    }

    private void playAnimation(Ref<EntityStore> entityRef, CitizenData.AnimationConfig anim, Collection<Player> players) {
        try {
            AnimationSlot slot = this.resolveSlot(anim.animationSlot);
            NetworkId nid = (NetworkId)entityRef.getStore().getComponent(entityRef, NetworkId.getComponentType());
            if (nid == null) {
                return;
            }
            PlayAnimation packet = new PlayAnimation(nid.getId(), null, anim.animationName, slot);
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
            LOGGER.fine("Failed to play animation: " + e.getMessage());
        }
    }

    private void stopAnimation(Ref<EntityStore> entityRef, CitizenData.AnimationConfig anim, Collection<Player> players) {
        try {
            AnimationSlot slot = this.resolveSlot(anim.animationSlot);
            if (slot == AnimationSlot.Movement) {
                AnimationUtils.stopAnimation(entityRef, (AnimationSlot)slot, (ComponentAccessor)entityRef.getStore());
                return;
            }
            if (anim.stopAnimation != null && !anim.stopAnimation.isEmpty()) {
                NetworkId nid = (NetworkId)entityRef.getStore().getComponent(entityRef, NetworkId.getComponentType());
                if (nid != null) {
                    PlayAnimation packet = new PlayAnimation(nid.getId(), null, anim.stopAnimation, slot);
                    for (Player p : players) {
                        try {
                            PlayerRef pRef = p.getPlayerRef();
                            if (pRef == null) continue;
                            pRef.getPacketHandler().writeNoCache((ToClientPacket)packet);
                        }
                        catch (Exception exception) {}
                    }
                }
            } else {
                AnimationUtils.stopAnimation(entityRef, (AnimationSlot)slot, (ComponentAccessor)entityRef.getStore());
            }
        }
        catch (Exception e) {
            LOGGER.fine("Failed to stop animation: " + e.getMessage());
        }
    }

    private AnimationSlot resolveSlot(int slot) {
        AnimationSlot[] values = AnimationSlot.VALUES;
        if (slot >= 0 && slot < values.length) {
            return values[slot];
        }
        return AnimationSlot.Action;
    }

    private boolean isPlayerNearby(CitizenData citizen, Collection<Player> players, float range) {
        double rangeSq = range * range;
        double cx = citizen.resolvedPosX;
        double cy = citizen.resolvedPosY;
        double cz = citizen.resolvedPosZ;
        for (Player player : players) {
            try {
                TransformComponent transform = (TransformComponent)player.getReference().getStore().getComponent(player.getReference(), TransformComponent.getComponentType());
                if (transform == null) continue;
                Vector3d pos = transform.getPosition();
                double dx = pos.x - cx;
                double dy = pos.y - cy;
                double dz = pos.z - cz;
                if (!(dx * dx + dy * dy + dz * dz <= rangeSq)) continue;
                return true;
            }
            catch (Exception exception) {
            }
        }
        return false;
    }

    public void clear() {
        this.timerStates.clear();
        this.activeProximity.clear();
    }

    private static class AnimTimerState {
        CitizenData.AnimationConfig config;
        long nextTriggerMs;
        boolean isRandom;
        boolean needsStop;
        long stopAtMs;

        private AnimTimerState() {
        }
    }
}

