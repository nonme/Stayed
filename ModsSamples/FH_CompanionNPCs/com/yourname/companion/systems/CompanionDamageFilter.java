/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.ArchetypeChunk
 *  com.hypixel.hytale.component.CommandBuffer
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.SystemGroup
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.logger.HytaleLogger
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.server.core.entity.UUIDComponent
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.ProjectileComponent
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage$EntitySource
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage$Source
 *  com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem
 *  com.hypixel.hytale.server.core.universe.Universe
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  com.hypixel.hytale.server.npc.entities.NPCEntity
 *  com.hypixel.hytale.server.npc.movement.controllers.MotionController
 */
package com.yourname.companion.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.movement.controllers.MotionController;
import com.yourname.companion.data.PlayerCompanionData;
import com.yourname.companion.storage.CompanionManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class CompanionDamageFilter
extends DamageEventSystem {
    private static final long LAUNCH_SUPPRESS_DURATION_MS = 1200L;
    private static final long REACTIVE_ATTACKER_DURATION_MS = 15000L;
    private final SystemGroup<EntityStore> group;
    private final CompanionManager companionManager;
    private final HytaleLogger logger;
    private volatile boolean loggedOnce = false;

    public CompanionDamageFilter(SystemGroup<EntityStore> group, CompanionManager mgr, HytaleLogger log) {
        this.group = group;
        this.companionManager = mgr;
        this.logger = log;
    }

    public SystemGroup<EntityStore> getGroup() {
        return this.group;
    }

    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> cmd, Damage damage) {
        block17: {
            if (damage.isCancelled()) {
                return;
            }
            try {
                UUID sourceUuid;
                Ref<EntityStore> targetRef = this.resolveTargetRef(damage, chunk, index);
                if (targetRef == null || !targetRef.isValid()) {
                    return;
                }
                Ref<EntityStore> sourceRef = this.resolveSourceRef(damage);
                UUID targetUuid = this.getEntityUuid(store, targetRef);
                UUID reactiveAttackerUuid = sourceUuid = sourceRef != null && sourceRef.isValid() ? this.getEntityUuid(store, sourceRef) : null;
                UUID projectileCreatorUuid = this.resolveProjectileCreatorUuid(store, sourceRef);
                if (projectileCreatorUuid != null) {
                    reactiveAttackerUuid = projectileCreatorUuid;
                }
                Set<UUID> activeCompanionEntityIds = this.companionManager.getAllActiveCompanionEntityIds();
                boolean sourceIsCompanion = sourceUuid != null && activeCompanionEntityIds.contains(sourceUuid) || reactiveAttackerUuid != null && activeCompanionEntityIds.contains(reactiveAttackerUuid);
                boolean targetIsCompanion = targetUuid != null && activeCompanionEntityIds.contains(targetUuid);
                boolean sourceIsPlayer = sourceRef != null && sourceRef.isValid() && this.isPlayer(store, sourceRef, sourceUuid);
                boolean targetIsPlayer = this.isPlayer(store, targetRef, targetUuid);
                if (targetIsCompanion && this.isCompanionImmuneEnvironmentDamage(damage)) {
                    damage.setCancelled(true);
                    this.dampenCompanionLaunch(store, targetRef);
                    return;
                }
                if (targetIsCompanion) {
                    this.dampenCompanionLaunch(store, targetRef);
                    if (reactiveAttackerUuid != null && !sourceIsCompanion && !sourceIsPlayer) {
                        this.companionManager.markReactiveAttackerByEntityId(targetUuid, reactiveAttackerUuid, 15000L);
                    }
                    if (sourceIsPlayer || sourceRef == null || !sourceRef.isValid()) {
                        this.companionManager.markLaunchSuppressionByEntityId(targetUuid, 1200L);
                    }
                }
                if (targetIsPlayer && reactiveAttackerUuid != null && !sourceIsCompanion && !sourceIsPlayer) {
                    this.companionManager.markOwnerCompanionsReactiveAttacker(targetUuid, reactiveAttackerUuid, 15000L);
                }
                if (!sourceIsCompanion && !sourceIsPlayer) {
                    return;
                }
                if (sourceIsCompanion && targetIsPlayer) {
                    boolean allow;
                    UUID sourceOwnerId = this.companionManager.findOwnerByCompanionEntityId(sourceUuid);
                    PlayerCompanionData sourceOwner = sourceOwnerId != null ? this.companionManager.get(sourceOwnerId) : null;
                    boolean bl = allow = sourceOwner != null && sourceOwner.allowCompanionVsPlayerDamage;
                    if (!allow) {
                        damage.setCancelled(true);
                    }
                    return;
                }
                if (sourceIsPlayer && targetIsCompanion) {
                    boolean allow;
                    UUID targetOwnerId = this.companionManager.findOwnerByCompanionEntityId(targetUuid);
                    PlayerCompanionData targetOwner = targetOwnerId != null ? this.companionManager.get(targetOwnerId) : null;
                    boolean bl = allow = targetOwner != null && targetOwner.allowCompanionVsPlayerDamage;
                    if (!allow) {
                        damage.setCancelled(true);
                    }
                    this.dampenCompanionLaunch(store, targetRef);
                    return;
                }
                if (sourceIsCompanion && targetIsCompanion) {
                    boolean allow;
                    UUID sourceOwnerId = this.companionManager.findOwnerByCompanionEntityId(sourceUuid);
                    PlayerCompanionData sourceOwner = sourceOwnerId != null ? this.companionManager.get(sourceOwnerId) : null;
                    boolean bl = allow = sourceOwner != null && sourceOwner.allowCompanionVsCompanionDamage;
                    if (!allow) {
                        damage.setCancelled(true);
                    }
                    return;
                }
            }
            catch (Throwable t) {
                if (this.loggedOnce) break block17;
                this.loggedOnce = true;
                ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("[DamageFilter] Error in companion damage filter.");
            }
        }
    }

    private UUID getEntityUuid(Store<EntityStore> store, Ref<EntityStore> ref) {
        try {
            UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ref, UUIDComponent.getComponentType());
            return uuidComp != null ? uuidComp.getUuid() : null;
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private boolean isPlayer(Store<EntityStore> store, Ref<EntityStore> ref, UUID uuid) {
        try {
            if (uuid != null && Universe.get().getPlayer(uuid) != null) {
                return true;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            return store.getComponent(ref, Player.getComponentType()) != null;
        }
        catch (Throwable throwable) {
            try {
                ProjectileComponent projectile = (ProjectileComponent)store.getComponent(ref, ProjectileComponent.getComponentType());
                if (projectile != null) {
                    Field creatorField = projectile.getClass().getDeclaredField("creatorUuid");
                    creatorField.setAccessible(true);
                    Object creator = creatorField.get(projectile);
                    if (creator instanceof UUID) {
                        UUID creatorUuid = (UUID)creator;
                        return Universe.get().getPlayer(creatorUuid) != null;
                    }
                }
            }
            catch (Throwable throwable2) {
                // empty catch block
            }
            return false;
        }
    }

    private UUID resolveProjectileCreatorUuid(Store<EntityStore> store, Ref<EntityStore> ref) {
        try {
            UUID u;
            ProjectileComponent projectile = (ProjectileComponent)store.getComponent(ref, ProjectileComponent.getComponentType());
            if (projectile == null) {
                return null;
            }
            Field creatorField = projectile.getClass().getDeclaredField("creatorUuid");
            creatorField.setAccessible(true);
            Object creator = creatorField.get(projectile);
            return creator instanceof UUID ? (u = (UUID)creator) : null;
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private Ref<EntityStore> resolveSourceRef(Damage damage) {
        String[] fieldNames;
        String[] methodNames;
        try {
            Damage.Source source = damage.getSource();
            if (source instanceof Damage.EntitySource) {
                Damage.EntitySource entitySource = (Damage.EntitySource)source;
                return entitySource.getRef();
            }
        }
        catch (Throwable source) {
            // empty catch block
        }
        for (String methodName : methodNames = new String[]{"getSourceRef", "getAttackerRef", "getInstigatorRef", "getDamagerRef", "getEntityRef"}) {
            try {
                Ref ref;
                Method method = damage.getClass().getMethod(methodName, new Class[0]);
                Object value = method.invoke((Object)damage, new Object[0]);
                if (!(value instanceof Ref)) continue;
                Ref cast = ref = (Ref)value;
                return cast;
            }
            catch (Throwable method) {
                // empty catch block
            }
        }
        for (String fieldName : fieldNames = new String[]{"sourceRef", "attackerRef", "instigatorRef", "damagerRef", "entityRef"}) {
            try {
                Ref ref;
                Field f = damage.getClass().getDeclaredField(fieldName);
                f.setAccessible(true);
                Object value = f.get(damage);
                if (!(value instanceof Ref)) continue;
                Ref cast = ref = (Ref)value;
                return cast;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return null;
    }

    private void dampenCompanionLaunch(Store<EntityStore> store, Ref<EntityStore> targetRef) {
        if (store == null || targetRef == null || !targetRef.isValid()) {
            return;
        }
        try {
            NPCEntity npc = (NPCEntity)store.getComponent(targetRef, NPCEntity.getComponentType());
            if (npc == null) {
                return;
            }
            MotionController mc = npc.getRole().getActiveMotionController();
            if (mc != null) {
                mc.forceVelocity(new Vector3d(0.0, 0.0, 0.0), null, false);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private boolean isCompanionImmuneEnvironmentDamage(Damage damage) {
        String[] fieldNames;
        String[] methodNames;
        if (damage == null) {
            return false;
        }
        for (String methodName : methodNames = new String[]{"getCause", "getDamageCause", "getType"}) {
            try {
                Method method = damage.getClass().getMethod(methodName, new Class[0]);
                Object value = method.invoke((Object)damage, new Object[0]);
                if (!this.isCompanionImmuneCause(value)) continue;
                return true;
            }
            catch (Throwable method) {
                // empty catch block
            }
        }
        for (String fieldName : fieldNames = new String[]{"damageCause", "cause", "damageCauseIndex"}) {
            try {
                Field f = damage.getClass().getDeclaredField(fieldName);
                f.setAccessible(true);
                Object value = f.get(damage);
                if (!this.isCompanionImmuneCause(value)) continue;
                return true;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return false;
    }

    private boolean isCompanionImmuneCause(Object value) {
        if (value == null) {
            return false;
        }
        String cause = String.valueOf(value);
        return "SUFFOCATION".equalsIgnoreCase(cause) || "DROWNING".equalsIgnoreCase(cause);
    }

    private Ref<EntityStore> resolveTargetRef(Damage damage, ArchetypeChunk<EntityStore> chunk, int index) {
        String[] methodNames;
        for (String methodName : methodNames = new String[]{"getTargetRef", "getTargetEntityRef", "getTarget", "getVictimRef", "getVictim", "getEntityRef"}) {
            try {
                Ref ref;
                Method method = damage.getClass().getMethod(methodName, new Class[0]);
                Object value = method.invoke((Object)damage, new Object[0]);
                if (!(value instanceof Ref)) continue;
                Ref cast = ref = (Ref)value;
                return cast;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        try {
            return chunk.getReferenceTo(index);
        }
        catch (Throwable throwable) {
            return null;
        }
    }
}

