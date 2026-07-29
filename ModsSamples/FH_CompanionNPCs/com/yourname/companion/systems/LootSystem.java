/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.logger.HytaleLogger
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.server.core.entity.UUIDComponent
 *  com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  com.hypixel.hytale.server.npc.entities.NPCEntity
 *  com.hypixel.hytale.server.npc.movement.controllers.MotionController
 */
package com.yourname.companion.systems;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.movement.controllers.MotionController;
import com.yourname.companion.data.PlayerCompanionData;
import com.yourname.companion.runtime.CompanionRuntimeState;
import com.yourname.companion.util.InvUtil;
import com.yourname.companion.util.WorldQueries;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public final class LootSystem {
    private static final int LOOT_SCAN_INTERVAL = 20;
    private static final double LOOT_RADIUS = 12.0;
    private static final double PICKUP_RANGE = 3.5;
    private static final int LOOT_SWITCH_THRESHOLD = 250;
    private static final int UNREACHABLE_TIMEOUT = 60;
    private static final int MAX_ENTITIES_PER_SCAN = 30;
    private static final long NO_SPACE_BACKOFF_TICKS = 100L;
    private static final double PICKUP_HORIZONTAL_RANGE = 2.25;
    private static final double PICKUP_VERTICAL_TOLERANCE = 2.5;
    private final HytaleLogger logger;

    public LootSystem(HytaleLogger logger) {
        this.logger = logger;
    }

    public boolean evaluateLoot(World world, Store<EntityStore> store, Ref<EntityStore> companionRef, NPCEntity npcEntity, PlayerCompanionData data, CompanionRuntimeState runtime, Vector3d companionPos, long currentTick) {
        if (runtime.lootNoSpaceUntilTick > currentTick) {
            return false;
        }
        boolean hasSpace = InvUtil.hasInventorySpace(npcEntity);
        if (!hasSpace) {
            this.debugLoot(runtime, currentTick, "no inventory space; scanning for stackable pickup");
        }
        if (currentTick - runtime.lastLootScanTick < 20L) {
            boolean keep;
            boolean bl = keep = runtime.lootTargetId != null && this.isLootTargetValid(world, store, runtime.lootTargetId, companionPos);
            if (!keep) {
                this.clearLootTargets(npcEntity, runtime, false);
            }
            this.debugLoot(runtime, currentTick, "throttle: keepTarget=" + keep + " target=" + (runtime.lootTargetId == null ? "none" : runtime.lootTargetId));
            return keep;
        }
        runtime.lastLootScanTick = currentTick;
        List<WorldQueries.NearbyEntity> items = WorldQueries.getNearbyDroppedItems(world, companionPos, 12.0);
        this.debugLoot(runtime, currentTick, "scan: found=" + items.size());
        if (items.isEmpty()) {
            if (!hasSpace) {
                runtime.lootNoSpaceUntilTick = currentTick + 100L;
            }
            this.clearLootTargets(npcEntity, runtime, false);
            runtime.lootTargetId = null;
            this.debugLoot(runtime, currentTick, "scan: no loot candidates");
            return false;
        }
        int limit = Math.min(items.size(), 30);
        Ref<EntityStore> bestTarget = null;
        double bestScore = -1.0;
        UUID currentLootId = this.parseUuid(runtime.lootTargetId);
        for (int i = 0; i < limit; ++i) {
            WorldQueries.NearbyEntity item = items.get(i);
            double score = this.scoreLootTarget(companionPos, item, currentLootId, store);
            if (!(score > bestScore)) continue;
            bestScore = score;
            bestTarget = item.ref;
        }
        if (bestTarget == null) {
            if (!hasSpace) {
                runtime.lootNoSpaceUntilTick = currentTick + 100L;
            }
            this.clearLootTargets(npcEntity, runtime, false);
            runtime.lootTargetId = null;
            this.debugLoot(runtime, currentTick, "scan: no best target");
            return false;
        }
        if (currentLootId != null && this.isLootTargetValid(world, store, runtime.lootTargetId, companionPos)) {
            this.debugLoot(runtime, currentTick, "stick: keeping existing target=" + runtime.lootTargetId);
            return true;
        }
        try {
            UUIDComponent uuidComp = (UUIDComponent)store.getComponent(bestTarget, UUIDComponent.getComponentType());
            if (uuidComp != null) {
                runtime.lootTargetId = uuidComp.getUuid().toString();
                runtime.lootUnreachableTicks = 0;
                this.debugLoot(runtime, currentTick, "select: target=" + runtime.lootTargetId);
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Loot target UUID read failed.");
        }
        return runtime.lootTargetId != null;
    }

    public void executeLootSeek(World world, Store<EntityStore> store, NPCEntity npcEntity, Ref<EntityStore> companionRef, CompanionRuntimeState runtime, Vector3d companionPos) {
        if (runtime.lootTargetId == null) {
            return;
        }
        UUID targetId = this.parseUuid(runtime.lootTargetId);
        if (targetId == null) {
            this.clearLootTargets(npcEntity, runtime, true);
            runtime.lootTargetId = null;
            return;
        }
        Ref targetRef = world.getEntityRef(targetId);
        if (targetRef == null || !targetRef.isValid()) {
            this.clearLootTargets(npcEntity, runtime, true);
            runtime.lootTargetId = null;
            return;
        }
        TransformComponent targetTransform = (TransformComponent)store.getComponent(targetRef, TransformComponent.getComponentType());
        if (targetTransform == null) {
            this.clearLootTargets(npcEntity, runtime, true);
            runtime.lootTargetId = null;
            return;
        }
        Vector3d targetPos = targetTransform.getPosition();
        if (targetPos == null) {
            this.clearLootTargets(npcEntity, runtime, true);
            runtime.lootTargetId = null;
            return;
        }
        double dx = targetPos.x - companionPos.x;
        double dz = targetPos.z - companionPos.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double verticalDistance = Math.abs(targetPos.y - companionPos.y);
        double distance = companionPos.distanceTo(targetPos);
        this.debugLoot(runtime, runtime.currentTick, "execute: dist=" + String.format("%.2f", distance) + " target=" + runtime.lootTargetId);
        if (distance <= 3.5 || horizontalDistance <= 2.25 && verticalDistance <= 2.5) {
            try {
                try {
                    npcEntity.onFlockSetTarget("LockedTarget", null);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    npcEntity.onFlockSetTarget("Target", null);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                npcEntity.onFlockSetTarget("LootTarget", targetRef);
                try {
                    MotionController mc = npcEntity.getRole().getActiveMotionController();
                    if (mc != null) {
                        mc.forceVelocity(new Vector3d(0.0, 0.0, 0.0), null, false);
                    }
                }
                catch (Throwable mc) {
                    // empty catch block
                }
                try {
                    npcEntity.getRole().getWorldSupport().requestNewPath();
                }
                catch (Throwable mc) {}
            }
            catch (Throwable mc) {
                // empty catch block
            }
            runtime.lootUnreachableTicks = 0;
            this.debugLoot(runtime, runtime.currentTick, "execute: in pickup range");
            return;
        }
        try {
            npcEntity.onFlockSetTarget("LockedTarget", targetRef);
            try {
                npcEntity.onFlockSetTarget("Target", targetRef);
            }
            catch (Throwable mc) {
                // empty catch block
            }
            try {
                npcEntity.onFlockSetTarget("LootTarget", targetRef);
            }
            catch (Throwable mc) {
                // empty catch block
            }
            npcEntity.getRole().getWorldSupport().requestNewPath();
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to path companion to loot.");
        }
        runtime.lootUnreachableTicks += 5;
        if (runtime.lootUnreachableTicks >= 60) {
            this.clearLootTargets(npcEntity, runtime, true);
            runtime.lootTargetId = null;
            runtime.lootUnreachableTicks = 0;
            this.debugLoot(runtime, runtime.currentTick, "execute: target timeout cleared");
        }
    }

    private void clearLootTargets(NPCEntity npcEntity, CompanionRuntimeState runtime, boolean clearMovementTargets) {
        try {
            npcEntity.onFlockSetTarget("LootTarget", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        if (clearMovementTargets) {
            try {
                npcEntity.onFlockSetTarget("Target", null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                npcEntity.onFlockSetTarget("LockedTarget", null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
    }

    private void debugLoot(CompanionRuntimeState runtime, long currentTick, String msg) {
        if (runtime == null) {
            return;
        }
        if (runtime.lastLootDebugTick != 0L && currentTick - runtime.lastLootDebugTick < 20L) {
            return;
        }
        runtime.lastLootDebugTick = currentTick;
        this.logger.at(Level.FINE).log("[LootDebug] " + msg);
    }

    private double scoreLootTarget(Vector3d companionPos, WorldQueries.NearbyEntity item, UUID currentLootId, Store<EntityStore> store) {
        double score = 500.0 - item.distance * 30.0;
        if (item.distance < 2.0) {
            score += 200.0;
        }
        if (currentLootId != null) {
            try {
                UUIDComponent uuidComp = (UUIDComponent)store.getComponent(item.ref, UUIDComponent.getComponentType());
                if (uuidComp != null && currentLootId.equals(uuidComp.getUuid())) {
                    score += 120.0;
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return score;
    }

    private boolean isLootTargetValid(World world, Store<EntityStore> store, String targetIdStr, Vector3d companionPos) {
        UUID targetId = this.parseUuid(targetIdStr);
        if (targetId == null) {
            return false;
        }
        Ref ref = world.getEntityRef(targetId);
        if (ref == null || !ref.isValid()) {
            return false;
        }
        TransformComponent transform = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getPosition() == null) {
            return false;
        }
        return companionPos.distanceTo(transform.getPosition()) <= 18.0;
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }
}

