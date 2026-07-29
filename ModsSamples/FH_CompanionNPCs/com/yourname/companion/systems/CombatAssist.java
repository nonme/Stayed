/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.logger.HytaleLogger
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.server.core.asset.type.attitude.Attitude
 *  com.hypixel.hytale.server.core.entity.UUIDComponent
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.inventory.Inventory
 *  com.hypixel.hytale.server.core.inventory.ItemStack
 *  com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer
 *  com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage$EntitySource
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage$Source
 *  com.hypixel.hytale.server.core.modules.entity.damage.DamageCause
 *  com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems
 *  com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  com.hypixel.hytale.server.npc.entities.NPCEntity
 */
package com.yourname.companion.systems;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.yourname.companion.data.CompanionMode;
import com.yourname.companion.data.CompanionRecord;
import com.yourname.companion.data.EquipmentSlot;
import com.yourname.companion.data.PlayerCompanionData;
import com.yourname.companion.runtime.CompanionRuntimeState;
import com.yourname.companion.util.WorldQueries;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class CombatAssist {
    private static final boolean DEBUG_COMBAT_TRACE = false;
    private static final boolean ENABLE_PASSIVE_NONHOSTILE_FILTER = true;
    private static final String[] HOSTILE_ROLE_KEYWORDS = new String[]{"skeleton", "zombie", "wraith", "ghoul", "undead", "bandit", "outlaw", "outlander", "raider", "goblin", "trork", "shadow_knight", "spider", "scorpion", "scarak", "crawler", "void", "wolf", "bear", "hyena", "raptor", "rex", "tiger", "sabertooth", "leopard", "crocodile", "cobra", "snake", "hound", "emberwulf", "yeti", "stalker", "golem", "spirit", "dragon", "spark", "hedera", "slug", "magma", "mole", "rat", "rhino", "toad", "snapdragon", "trillodon", "basilisk", "creeper", "orc"};
    private static final String[] INVALID_COMBAT_ROLE_KEYWORDS = new String[]{"spawner_", "_spawner", "debug_", "test_"};
    private static final String[] PROTECTED_PASSIVE_ROLE_KEYWORDS = new String[]{"chicken", "chicken_desert", "chicken_chick", "cow", "calf", "pig", "pig_wild", "piglet", "sheep", "lamb", "goat", "kid", "turkey", "turkey_chick", "duck", "horse", "foal", "camel", "camel_calf", "donkey", "alpaca", "llama", "rabbit", "bunny", "hare", "deer", "doe", "stag", "fox", "cat", "mouse", "antelope", "bison", "bison_calf", "moose", "moose_bull", "moose_cow", "ram", "boar", "boar_piglet", "warthog", "warthog_piglet", "meerkat", "armadillo", "mouflon", "mouflon_lamb", "mosshorn", "skrill", "skrill_chick", "bird", "owl", "snowy_owl", "sparrow", "pigeon", "dove", "crow", "raven", "parrot", "finch", "greenfinch", "flamingo", "vulture", "hawk", "woodpecker", "robin", "jay", "bluejay", "cardinal", "pelican", "seagull", "heron", "penguin", "archaeopteryx", "tetrabird", "frog", "gecko", "sand_lizard", "tortoise", "turtle", "fish", "trout", "rainbow_trout", "catfish", "salmon", "bass", "carp", "minnow", "bluegill", "pike", "pufferfish", "clownfish", "chevron_tang", "lemon_peel", "piranha", "jellyfish", "lobster", "crab", "trilobite", "moray", "whale", "shark", "frostgill", "critter", "flying_critter", "squirrel", "temple_squirrel", "bat", "ice_bat", "snail", "frost_snail", "butterfly", "silk_larva", "lava_shellfish", "root_spirit", "kweebec", "feran", "cactee", "merchant", "civilian", "villager", "fisherman"};
    private static final double DEFEND_RADIUS = 20.0;
    private static final double FARMER_DEFEND_RADIUS = 10.0;
    private static final double COMBAT_LEASH_RADIUS = 20.0;
    private static final int COMBAT_SCAN_INTERVAL = 10;
    private static final int COMBAT_IDLE_SCAN_INTERVAL = 40;
    private static final int PASSIVE_ONLY_SCAN_BACKOFF_TICKS = 120;
    private static final int COMBAT_STICKINESS = 200;
    private static final int TARGET_SWITCH_THRESHOLD = 500;
    private static final int ATTITUDE_REFRESH_INTERVAL = 40;
    private static final long COMBAT_STALL_TIMEOUT_TICKS = 240L;
    private static final int FOLLOW_RECOVERY_BOOTSTRAP_TICKS = 40;
    private static final long COMBAT_REACQUIRE_BLOCK_TICKS = 120L;
    private static final double RANGED_MAX_DISTANCE = 20.0;
    private static final double RANGED_HOLD_DISTANCE = 8.0;
    private static final int RANGED_ATTACK_COOLDOWN_TICKS = 40;
    private static final double MELEE_ATTACK_DISTANCE = 2.2;
    private static final int MELEE_ATTACK_COOLDOWN_TICKS = 30;
    private static final double MAX_DEFEND_HOLD_RADIUS = 5.0;
    private static final int MAX_DEFEND_ATTACK_COOLDOWN_TICKS = 200;
    private static final long KILL_CREDIT_CLAIM_TTL_TICKS = 200L;
    private static final long RECENT_DAMAGE_KILL_CREDIT_TICKS = 160L;
    private final HytaleLogger logger;
    private final Map<String, Long> claimedKillTargetTicks = new ConcurrentHashMap<String, Long>();
    private volatile boolean attitudeOverridesAvailable = true;
    private static volatile boolean shieldRuntimeMethodSnapshotLogged = false;
    private static final String WEAPON_BOW = "BOW";
    private static final String WEAPON_CROSSBOW = "CROSSBOW";
    private static final String WEAPON_GUN = "GUN";
    private static final String WEAPON_DAGGER = "DAGGER";
    private static final String WEAPON_SWORD = "SWORD";
    private static final String WEAPON_MACE = "MACE";
    private static final String WEAPON_AXE = "AXE";
    private static final String WEAPON_SPEAR = "SPEAR";
    private static final String WEAPON_SHIELD = "SHIELD";
    private static final String WEAPON_MELEE_GENERIC = "MELEE_GENERIC";

    public CombatAssist(HytaleLogger logger) {
        this.logger = logger;
    }

    public double getScanRadius(CompanionMode mode) {
        return mode == CompanionMode.FIGHTER ? 20.0 : 10.0;
    }

    public boolean isCombatEnabled(CompanionMode mode) {
        return true;
    }

    public boolean evaluateCombat(World world, Store<EntityStore> store, PlayerRef ownerRef, Ref<EntityStore> companionRef, NPCEntity npcEntity, CompanionRecord companion, CompanionRuntimeState runtime, Vector3d ownerPos, Vector3d companionPos, long currentTick, Set<UUID> friendlyEntityIds) {
        int scanInterval;
        this.pollRecentDamageKillCredit(world, store, runtime, currentTick);
        int n = scanInterval = runtime.combatTargetId == null ? 40 : 10;
        if (runtime.lastCombatScanTick == 0L && companion != null && companion.uniqueId != null) {
            int phase = Math.floorMod(companion.uniqueId.hashCode(), Math.max(1, scanInterval));
            runtime.lastCombatScanTick = Math.max(0L, currentTick - (long)phase);
        }
        if (currentTick - runtime.lastCombatScanTick < (long)scanInterval) {
            if (runtime.combatTargetId == null) {
                return false;
            }
            boolean valid = this.isTargetValid(world, store, runtime.combatTargetId, ownerPos, runtime);
            if (!valid) {
                if (this.isDeadOrMissingTarget(world, store, runtime.combatTargetId)) {
                    this.onTargetKilled(runtime, runtime.combatTargetId, currentTick);
                }
                this.clearCombatTarget(runtime, npcEntity);
            }
            return valid;
        }
        runtime.lastCombatScanTick = currentTick;
        UUID ownerId = ownerRef.getUuid();
        UUID compId = null;
        try {
            UUIDComponent uuidComp = (UUIDComponent)store.getComponent(companionRef, UUIDComponent.getComponentType());
            if (uuidComp != null) {
                compId = uuidComp.getUuid();
            }
        }
        catch (Throwable uuidComp) {
            // empty catch block
        }
        double scanRadius = this.getScanRadius(companion.mode);
        List<WorldQueries.NearbyEntity> candidates = WorldQueries.getNearbyHostiles(world, companionPos, scanRadius, compId, ownerId);
        int preFilterCount = candidates.size();
        ArrayList filteredReasons = new ArrayList();
        candidates.removeIf(ne -> {
            boolean filtered;
            boolean player = this.isPlayerEntity(store, ne.ref);
            boolean invalidCombatNpc = this.isInvalidCombatNpc(store, ne.ref);
            boolean companionNpc = this.isCompanionNpc(store, ne.ref);
            boolean saneCombatNpc = this.hasSaneCombatPosition(store, ne.ref, companionPos, scanRadius);
            boolean friendly = this.isFriendlyEntity(store, ne.ref, friendlyEntityIds);
            boolean reactive = this.isReactiveAttacker(store, ne.ref, runtime);
            boolean passive = this.isProtectedPassiveNpc(store, ne.ref);
            boolean hostile = this.isLikelyHostileNpc(store, ne.ref);
            boolean bl = filtered = player || invalidCombatNpc || companionNpc || !saneCombatNpc || friendly || passive && !reactive || !hostile && !reactive;
            if (filtered && filteredReasons.size() < 8) {
                String reason = player ? "player" : (invalidCombatNpc ? "invalid" : (companionNpc ? "companion" : (!saneCombatNpc ? "distance" : (friendly ? "friendly" : (passive && !reactive ? "passive" : "nonhostile")))));
                filteredReasons.add(this.describeTargetRole(ne.ref, store) + ":" + reason);
            }
            return filtered;
        });
        int postFilterCount = candidates.size();
        if (candidates.isEmpty()) {
            if (runtime.combatTargetId != null && this.isDeadOrMissingTarget(world, store, runtime.combatTargetId)) {
                this.onTargetKilled(runtime, runtime.combatTargetId, currentTick);
            }
            if (runtime.combatTargetId != null) {
                this.clearCombatTarget(runtime, npcEntity);
            }
            if (runtime.combatTargetId == null && preFilterCount > 0 && postFilterCount == 0) {
                runtime.lastCombatScanTick = currentTick + 120L;
            }
            return false;
        }
        Ref<EntityStore> bestTarget = null;
        double bestScore = -1.0;
        UUID currentTargetId = this.parseUuid(runtime.combatTargetId);
        for (WorldQueries.NearbyEntity hostile : candidates) {
            double score;
            if (this.isBlockedCombatTarget(store, hostile.ref, runtime) || !((score = this.scoreCombatTarget(ownerPos, hostile, currentTargetId, store)) > bestScore)) continue;
            bestScore = score;
            bestTarget = hostile.ref;
        }
        if (bestTarget == null) {
            if (runtime.combatTargetId != null) {
                this.clearCombatTarget(runtime, npcEntity);
            } else {
                runtime.lastCombatScanTick = currentTick + 120L;
            }
            return false;
        }
        if (currentTargetId != null && runtime.combatStickyTicks < 200) {
            double currentScore = 0.0;
            for (WorldQueries.NearbyEntity hostile : candidates) {
                try {
                    UUIDComponent uuidComp = (UUIDComponent)store.getComponent(hostile.ref, UUIDComponent.getComponentType());
                    if (uuidComp == null || !currentTargetId.equals(uuidComp.getUuid())) continue;
                    currentScore = this.scoreCombatTarget(ownerPos, hostile, currentTargetId, store);
                    break;
                }
                catch (Throwable throwable) {
                }
            }
            if (bestScore - currentScore < 500.0) {
                runtime.combatStickyTicks += 5;
                boolean valid = this.isTargetValid(world, store, runtime.combatTargetId, ownerPos, runtime);
                if (!valid) {
                    if (this.isDeadOrMissingTarget(world, store, runtime.combatTargetId)) {
                        this.onTargetKilled(runtime, runtime.combatTargetId, currentTick);
                    }
                    this.clearCombatTarget(runtime, npcEntity);
                }
                return valid;
            }
        }
        this.setNewCombatTarget(world, store, npcEntity, ownerRef, companionRef, bestTarget, runtime);
        return true;
    }

    public void executeCombat(World world, Store<EntityStore> store, NPCEntity npcEntity, Ref<EntityStore> companionRef, PlayerRef ownerRef, PlayerCompanionData ownerData, CompanionRecord companion, CompanionRuntimeState runtime, Vector3d ownerPos, Vector3d companionPos) {
        UUID targetId;
        if (runtime.combatTargetId == null) {
            return;
        }
        if (runtime.lastCombatPathTick <= 0L) {
            runtime.lastCombatPathTick = runtime.currentTick;
        }
        if ((targetId = this.parseUuid(runtime.combatTargetId)) == null) {
            this.clearCombatTarget(runtime, npcEntity);
            return;
        }
        Ref targetRef = world.getEntityRef(targetId);
        if (targetRef == null || !targetRef.isValid()) {
            this.onTargetKilled(runtime, runtime.combatTargetId, runtime.currentTick);
            this.clearCombatTarget(runtime, npcEntity);
            return;
        }
        if (this.isPlayerEntity(store, (Ref<EntityStore>)targetRef) || this.isCompanionNpc(store, (Ref<EntityStore>)targetRef) || this.isProtectedPassiveNpc(store, (Ref<EntityStore>)targetRef) && !this.isReactiveAttacker(store, (Ref<EntityStore>)targetRef, runtime)) {
            this.clearCombatTarget(runtime, npcEntity);
            return;
        }
        DeathComponent death = (DeathComponent)store.getComponent(targetRef, DeathComponent.getComponentType());
        if (death != null) {
            this.onTargetKilled(runtime, runtime.combatTargetId, runtime.currentTick);
            this.clearCombatTarget(runtime, npcEntity);
            return;
        }
        if (ownerPos.distanceTo(companionPos) > 20.0) {
            this.clearCombatTarget(runtime, npcEntity);
            return;
        }
        boolean statelessRole = true;
        boolean inStartState = this.isNpcInStartState(npcEntity);
        if (!statelessRole) {
            if (!inStartState) {
                runtime.lastCombatPathTick = runtime.currentTick;
            } else if (runtime.currentTick - runtime.lastCombatPathTick >= 240L) {
                runtime.combatBlockedTargetId = runtime.combatTargetId;
                runtime.combatBlockedUntilTick = runtime.currentTick + 120L;
                this.clearCombatTarget(runtime, npcEntity);
                return;
            }
        }
        String profile = this.getWeaponProfile(npcEntity, companion);
        if (runtime.maxDefendMode) {
            this.executeMaxDefendCombat(world, store, npcEntity, companionRef, ownerRef, (Ref<EntityStore>)targetRef, companionPos, ownerPos, runtime, companion, profile, !inStartState);
            runtime.combatStickyTicks += 5;
            return;
        }
        if (WEAPON_BOW.equals(profile) || WEAPON_CROSSBOW.equals(profile) || WEAPON_GUN.equals(profile)) {
            this.executeRangedCombat(store, npcEntity, companionRef, ownerRef, ownerData, (Ref<EntityStore>)targetRef, companionPos, runtime, companion, profile, !inStartState);
            runtime.combatStickyTicks += 5;
            return;
        }
        try {
            TransformComponent targetTf = (TransformComponent)store.getComponent(targetRef, TransformComponent.getComponentType());
            Vector3d targetPos = targetTf != null ? targetTf.getPosition() : null;
            double targetDist = targetPos != null ? companionPos.distanceTo(targetPos) : Double.MAX_VALUE;
            npcEntity.onFlockSetTarget("LockedTarget", targetRef);
            try {
                npcEntity.onFlockSetTarget("Target", targetRef);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                npcEntity.onFlockSetTarget("CombatTarget", targetRef);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                npcEntity.onFlockSetTarget("CombatTargetRanged", null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            this.applyMeleeTargetChannels(npcEntity, (Ref<EntityStore>)targetRef, profile, this.hasShieldEquipped(npcEntity, companion), targetDist, runtime.currentTick);
            if (runtime.combatStickyTicks % 10 == 0) {
                npcEntity.getRole().getWorldSupport().requestNewPath();
            }
            if (runtime.combatStickyTicks % 40 == 0) {
                this.safeOverrideAttitude(npcEntity, (Ref<EntityStore>)targetRef, Attitude.HOSTILE, 12.0);
                this.safeOverrideAttitude(npcEntity, (Ref<EntityStore>)ownerRef.getReference(), Attitude.REVERED, 12.0);
            }
            this.forceEnemyFocusOnCompanion(store, (Ref<EntityStore>)targetRef, companionRef, ownerRef, runtime.currentTick);
            if (targetDist <= 2.2 && this.isNpcInAttackState(npcEntity)) {
                this.markRecentCombatDamage(runtime, (Ref<EntityStore>)targetRef, store);
                runtime.lastMeleeAttackTick = runtime.currentTick;
                if (!inStartState) {
                    runtime.lastCombatPathTick = runtime.currentTick;
                }
            } else if (targetDist <= 2.2 && runtime.currentTick - runtime.lastMeleeAttackTick >= 30L) {
                float damageAmount = this.estimateMeleeDamage(companion);
                Damage dmg = new Damage((Damage.Source)new Damage.EntitySource(companionRef), DamageCause.PHYSICAL, damageAmount);
                DamageSystems.executeDamage((Ref)targetRef, store, (Damage)dmg);
                this.markRecentCombatDamage(runtime, (Ref<EntityStore>)targetRef, store);
                this.pollRecentDamageKillCredit(world, store, runtime, runtime.currentTick);
                runtime.lastMeleeAttackTick = runtime.currentTick;
                if (!inStartState) {
                    runtime.lastCombatPathTick = runtime.currentTick;
                }
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to direct companion combat.");
        }
        runtime.combatStickyTicks += 5;
    }

    private void executeMaxDefendCombat(World world, Store<EntityStore> store, NPCEntity npcEntity, Ref<EntityStore> companionRef, PlayerRef ownerRef, Ref<EntityStore> targetRef, Vector3d companionPos, Vector3d ownerPos, CompanionRuntimeState runtime, CompanionRecord companion, String profile, boolean allowPathProgressRefresh) {
        try {
            boolean interactionGuard;
            TransformComponent targetTf = (TransformComponent)store.getComponent(targetRef, TransformComponent.getComponentType());
            Vector3d targetPos = targetTf != null ? targetTf.getPosition() : null;
            double targetDist = targetPos != null ? companionPos.distanceTo(targetPos) : Double.MAX_VALUE;
            boolean shieldEquipped = this.hasShieldEquipped(npcEntity, companion);
            double ownerDist = ownerPos.distanceTo(companionPos);
            if (ownerDist > 5.35) {
                npcEntity.onFlockSetTarget("LockedTarget", ownerRef.getReference());
                try {
                    npcEntity.onFlockSetTarget("Target", ownerRef.getReference());
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    npcEntity.onFlockSetTarget("CombatTarget", null);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    npcEntity.onFlockSetTarget("CombatTargetRanged", null);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                this.clearMeleeTargetChannels(npcEntity);
                if (runtime.combatStickyTicks % 10 == 0) {
                    npcEntity.getRole().getWorldSupport().requestNewPath();
                }
                return;
            }
            npcEntity.onFlockSetTarget("LockedTarget", companionRef);
            try {
                npcEntity.onFlockSetTarget("Target", targetRef);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                npcEntity.onFlockSetTarget("CombatTarget", targetRef);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                npcEntity.onFlockSetTarget("CombatTargetRanged", null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            this.applyMeleeTargetChannels(npcEntity, targetRef, profile, shieldEquipped, targetDist, runtime.currentTick);
            boolean inBlockRange = targetDist <= 3.2;
            try {
                npcEntity.onFlockSetTarget("CombatTargetShield", shieldEquipped && inBlockRange ? targetRef : null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                npcEntity.onFlockSetTarget("CombatTargetShieldBlock", shieldEquipped && inBlockRange ? targetRef : null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            String blockSlot = this.applyWeaponBlockChannels(npcEntity, targetRef, profile, shieldEquipped, inBlockRange);
            if (inBlockRange && !(interactionGuard = this.tryTriggerWeaponBlockInteraction(npcEntity, profile, shieldEquipped))) {
                this.tryTriggerBlockAnimation(npcEntity, companionRef, store, profile, shieldEquipped);
            }
            this.forceEnemyFocusOnCompanion(store, targetRef, companionRef, ownerRef, runtime.currentTick);
            if (runtime.combatStickyTicks % 80 == 0) {
                interactionGuard = this.tryTriggerTauntEmote(npcEntity);
            }
            if (targetDist > 2.2) {
                return;
            }
            if (runtime.currentTick - runtime.lastMeleeAttackTick < 200L) {
                return;
            }
            float damageAmount = this.estimateMeleeDamage(companion);
            Damage dmg = new Damage((Damage.Source)new Damage.EntitySource(companionRef), DamageCause.PHYSICAL, damageAmount);
            DamageSystems.executeDamage(targetRef, store, (Damage)dmg);
            this.markRecentCombatDamage(runtime, targetRef, store);
            this.pollRecentDamageKillCredit(world, store, runtime, runtime.currentTick);
            runtime.lastMeleeAttackTick = runtime.currentTick;
            if (allowPathProgressRefresh) {
                runtime.lastCombatPathTick = runtime.currentTick;
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Max Defend combat tick failed.");
        }
    }

    private void executeRangedCombat(Store<EntityStore> store, NPCEntity npcEntity, Ref<EntityStore> companionRef, PlayerRef ownerRef, PlayerCompanionData ownerData, Ref<EntityStore> targetRef, Vector3d companionPos, CompanionRuntimeState runtime, CompanionRecord companion, String profile, boolean allowPathProgressRefresh) {
        try {
            TransformComponent targetTf = (TransformComponent)store.getComponent(targetRef, TransformComponent.getComponentType());
            if (targetTf == null || targetTf.getPosition() == null) {
                return;
            }
            double targetDist = companionPos.distanceTo(targetTf.getPosition());
            boolean consumeAmmo = ownerData != null && ownerData.consumeCompanionAmmo;
            this.ensureActiveHotbarSlotZero(npcEntity);
            if (consumeAmmo) {
                if (!this.hasRangedAmmo(npcEntity)) {
                    return;
                }
            } else {
                this.ensureVirtualAmmo(npcEntity);
            }
            boolean crossbow = WEAPON_CROSSBOW.equals(profile);
            boolean gun = WEAPON_GUN.equals(profile);
            if (targetDist > 20.0) {
                try {
                    npcEntity.onFlockSetTarget("LockedTarget", targetRef);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    npcEntity.onFlockSetTarget("Target", targetRef);
                }
                catch (Throwable throwable) {}
            } else {
                try {
                    npcEntity.onFlockSetTarget("LockedTarget", companionRef);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    npcEntity.onFlockSetTarget("Target", companionRef);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
            try {
                npcEntity.onFlockSetTarget("CombatTargetRanged", targetRef);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            if (crossbow) {
                try {
                    npcEntity.onFlockSetTarget("CombatTargetCrossbow", targetRef);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    npcEntity.onFlockSetTarget("CombatTargetBow", null);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    npcEntity.onFlockSetTarget("CombatTargetGun", null);
                }
                catch (Throwable throwable) {}
            } else if (gun) {
                try {
                    npcEntity.onFlockSetTarget("CombatTargetGun", targetRef);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    npcEntity.onFlockSetTarget("CombatTargetBow", null);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    npcEntity.onFlockSetTarget("CombatTargetCrossbow", null);
                }
                catch (Throwable throwable) {}
            } else {
                try {
                    npcEntity.onFlockSetTarget("CombatTargetBow", targetRef);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    npcEntity.onFlockSetTarget("CombatTargetCrossbow", null);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    npcEntity.onFlockSetTarget("CombatTargetGun", null);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
            try {
                npcEntity.onFlockSetTarget("CombatTarget", null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            this.clearMeleeTargetChannels(npcEntity);
            if (targetDist > 20.0 && runtime.combatStickyTicks % 8 == 0) {
                npcEntity.getRole().getWorldSupport().requestNewPath();
            }
            if (runtime.combatStickyTicks % 40 == 0) {
                this.safeOverrideAttitude(npcEntity, targetRef, Attitude.HOSTILE, 12.0);
                this.safeOverrideAttitude(npcEntity, (Ref<EntityStore>)ownerRef.getReference(), Attitude.REVERED, 12.0);
            }
            this.forceEnemyFocusOnCompanion(store, targetRef, companionRef, ownerRef, runtime.currentTick);
            if (targetDist > 20.0) {
                return;
            }
            if (allowPathProgressRefresh) {
                runtime.lastCombatPathTick = runtime.currentTick;
            }
            if (this.isNpcInAttackState(npcEntity)) {
                this.markRecentCombatDamage(runtime, targetRef, store);
                runtime.lastRangedAttackTick = runtime.currentTick;
                if (allowPathProgressRefresh) {
                    runtime.lastCombatPathTick = runtime.currentTick;
                }
                return;
            }
            if (runtime.currentTick - runtime.lastRangedAttackTick < 40L) {
                return;
            }
            if (consumeAmmo && !this.consumeOneRangedAmmo(npcEntity)) {
                return;
            }
            float damageAmount = this.estimateRangedDamage(companion);
            Damage dmg = new Damage((Damage.Source)new Damage.EntitySource(companionRef), DamageCause.PHYSICAL, damageAmount);
            DamageSystems.executeDamage(targetRef, store, (Damage)dmg);
            this.markRecentCombatDamage(runtime, targetRef, store);
            this.pollRecentDamageKillCredit(null, store, runtime, runtime.currentTick);
            runtime.lastRangedAttackTick = runtime.currentTick;
            if (allowPathProgressRefresh) {
                runtime.lastCombatPathTick = runtime.currentTick;
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Ranged combat tick failed.");
        }
    }

    private String getWeaponProfile(NPCEntity npcEntity, CompanionRecord companion) {
        String offLower;
        String weaponId = this.getEquippedWeaponId(npcEntity, companion);
        String offhandId = this.getEquippedOffhandId(companion);
        String lower = weaponId != null ? weaponId.toLowerCase(Locale.ROOT) : "";
        String string = offLower = offhandId != null ? offhandId.toLowerCase(Locale.ROOT) : "";
        if (lower.contains("crossbow")) {
            return WEAPON_CROSSBOW;
        }
        if (lower.contains("bow")) {
            return WEAPON_BOW;
        }
        if (lower.contains("gun") || lower.contains("rifle") || lower.contains("pistol") || lower.contains("handgun") || lower.contains("blunderbuss")) {
            return WEAPON_GUN;
        }
        if (lower.contains("dagger") || lower.contains("daggers") || lower.contains("knife")) {
            return WEAPON_DAGGER;
        }
        if (lower.contains("spear") || lower.contains("polearm")) {
            return WEAPON_SPEAR;
        }
        if (lower.contains("mace") || lower.contains("club") || lower.contains("flail")) {
            return WEAPON_MACE;
        }
        if (lower.contains("longsword") || lower.contains("greatsword") || lower.contains("great_sword")) {
            return WEAPON_AXE;
        }
        if (lower.contains("axe") || lower.contains("battleaxe")) {
            return WEAPON_AXE;
        }
        if (lower.contains("sword")) {
            return WEAPON_SWORD;
        }
        if (lower.contains("shield") || offLower.contains("shield")) {
            return WEAPON_SHIELD;
        }
        return WEAPON_MELEE_GENERIC;
    }

    private String getEquippedWeaponId(NPCEntity npcEntity, CompanionRecord companion) {
        String weaponId;
        String string = weaponId = companion != null ? companion.getEquipped(EquipmentSlot.WEAPON) : null;
        if (weaponId == null || weaponId.isBlank()) {
            try {
                ItemStack item;
                CombinedItemContainer hotbar;
                Inventory inv = npcEntity != null ? npcEntity.getInventory() : null;
                CombinedItemContainer combinedItemContainer = hotbar = inv != null ? inv.getCombinedHotbarFirst() : null;
                if (hotbar != null && (item = hotbar.getItemStack((short)0)) != null && !item.isEmpty()) {
                    weaponId = item.getItemId();
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return weaponId;
    }

    private String getEquippedOffhandId(CompanionRecord companion) {
        return companion != null ? companion.getEquipped(EquipmentSlot.OFFHAND) : null;
    }

    private boolean hasShieldEquipped(NPCEntity npcEntity, CompanionRecord companion) {
        String weapon = this.getEquippedWeaponId(npcEntity, companion);
        String offhand = this.getEquippedOffhandId(companion);
        String w = weapon != null ? weapon.toLowerCase(Locale.ROOT) : "";
        String o = offhand != null ? offhand.toLowerCase(Locale.ROOT) : "";
        return w.contains("shield") || o.contains("shield");
    }

    private void applyMeleeTargetChannels(NPCEntity npcEntity, Ref<EntityStore> targetRef, String profile, boolean shieldEquipped, double targetDist, long currentTick) {
        String slot;
        this.clearMeleeTargetChannels(npcEntity);
        switch (profile) {
            case "SWORD": {
                String string = "CombatTargetSword";
                break;
            }
            case "MACE": {
                String string = "CombatTargetMace";
                break;
            }
            case "AXE": {
                String string = "CombatTargetAxe";
                break;
            }
            case "SPEAR": {
                String string = "CombatTargetSpear";
                break;
            }
            case "DAGGER": {
                String string = "CombatTargetDaggers";
                break;
            }
            case "SHIELD": {
                String string = "CombatTargetShield";
                break;
            }
            default: {
                String string = slot = null;
            }
        }
        if (slot != null) {
            try {
                npcEntity.onFlockSetTarget(slot, targetRef);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                npcEntity.onFlockSetTarget("CombatTarget", targetRef);
            }
            catch (Throwable throwable) {}
        } else {
            try {
                npcEntity.onFlockSetTarget("CombatTarget", targetRef);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        boolean shouldBlock = shieldEquipped && targetDist <= 2.8 && currentTick % 20L < 5L;
        try {
            npcEntity.onFlockSetTarget("CombatTargetShieldBlock", shouldBlock ? targetRef : null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void clearMeleeTargetChannels(NPCEntity npcEntity) {
        try {
            npcEntity.onFlockSetTarget("CombatTargetSword", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            npcEntity.onFlockSetTarget("CombatTargetMace", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            npcEntity.onFlockSetTarget("CombatTargetAxe", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            npcEntity.onFlockSetTarget("CombatTargetSpear", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            npcEntity.onFlockSetTarget("CombatTargetDaggers", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            npcEntity.onFlockSetTarget("CombatTargetShield", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            npcEntity.onFlockSetTarget("CombatTargetShieldBlock", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            npcEntity.onFlockSetTarget("CombatTargetBlock", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            npcEntity.onFlockSetTarget("CombatTargetBlockSword", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            npcEntity.onFlockSetTarget("CombatTargetBlockDaggers", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            npcEntity.onFlockSetTarget("CombatTargetBlockAxe", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            npcEntity.onFlockSetTarget("CombatTargetBlockMace", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            npcEntity.onFlockSetTarget("CombatTargetBlockSpear", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private String applyWeaponBlockChannels(NPCEntity npcEntity, Ref<EntityStore> targetRef, String profile, boolean shieldEquipped, boolean inBlockRange) {
        try {
            npcEntity.onFlockSetTarget("CombatTargetBlockSword", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            npcEntity.onFlockSetTarget("CombatTargetBlockDaggers", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            npcEntity.onFlockSetTarget("CombatTargetBlockAxe", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            npcEntity.onFlockSetTarget("CombatTargetBlockMace", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            npcEntity.onFlockSetTarget("CombatTargetBlockSpear", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        if (!inBlockRange) {
            try {
                npcEntity.onFlockSetTarget("CombatTargetBlock", null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            return "none_out_of_range";
        }
        if (shieldEquipped) {
            try {
                npcEntity.onFlockSetTarget("CombatTargetBlock", null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            return "shield";
        }
        String slot = switch (profile) {
            case WEAPON_SWORD -> "CombatTargetBlockSword";
            case WEAPON_DAGGER -> "CombatTargetBlockDaggers";
            case WEAPON_AXE -> "CombatTargetBlockAxe";
            case WEAPON_MACE -> "CombatTargetBlockMace";
            case WEAPON_SPEAR -> "CombatTargetBlockSpear";
            default -> "CombatTargetBlock";
        };
        try {
            npcEntity.onFlockSetTarget(slot, targetRef);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        if (!"CombatTargetBlock".equals(slot)) {
            try {
                npcEntity.onFlockSetTarget("CombatTargetBlock", null);
            }
            catch (Throwable throwable) {}
        } else {
            try {
                npcEntity.onFlockSetTarget("CombatTargetBlock", targetRef);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return slot;
    }

    private boolean isNpcInAttackState(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return false;
        }
        try {
            String stateName = npcEntity.getRole().getStateSupport().getStateName();
            if (stateName == null) {
                return false;
            }
            String lower = stateName.toLowerCase(Locale.ROOT);
            return lower.startsWith("attack") || lower.startsWith("combat") || lower.contains(".attack") || lower.contains(".combat");
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private boolean hasRangedAmmo(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return false;
        }
        try {
            CombinedItemContainer container;
            Inventory inventory = npcEntity.getInventory();
            CombinedItemContainer combinedItemContainer = container = inventory != null ? inventory.getCombinedHotbarFirst() : null;
            if (container == null) {
                return false;
            }
            boolean[] found = new boolean[]{false};
            container.forEach((slot, itemStack) -> {
                if (found[0] || itemStack == null || itemStack.isEmpty()) {
                    return;
                }
                if (this.isAmmoItemId(itemStack.getItemId())) {
                    found[0] = true;
                }
            });
            return found[0];
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private boolean consumeOneRangedAmmo(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return false;
        }
        try {
            CombinedItemContainer container;
            Inventory inventory = npcEntity.getInventory();
            CombinedItemContainer combinedItemContainer = container = inventory != null ? inventory.getCombinedHotbarFirst() : null;
            if (container == null) {
                return false;
            }
            short[] targetSlot = new short[]{-1};
            ItemStack[] target = new ItemStack[]{null};
            container.forEach((slot, itemStack) -> {
                if (targetSlot[0] >= 0 || itemStack == null || itemStack.isEmpty()) {
                    return;
                }
                if (this.isAmmoItemId(itemStack.getItemId())) {
                    targetSlot[0] = slot;
                    target[0] = itemStack;
                }
            });
            if (targetSlot[0] < 0 || target[0] == null) {
                return false;
            }
            int qty = Math.max(0, target[0].getQuantity());
            if (qty <= 1) {
                container.setItemStackForSlot(targetSlot[0], ItemStack.EMPTY);
            } else {
                container.setItemStackForSlot(targetSlot[0], new ItemStack(target[0].getItemId(), qty - 1));
            }
            return true;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private void ensureVirtualAmmo(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return;
        }
        try {
            CombinedItemContainer container;
            if (this.hasRangedAmmo(npcEntity)) {
                return;
            }
            Inventory inventory = npcEntity.getInventory();
            CombinedItemContainer combinedItemContainer = container = inventory != null ? inventory.getCombinedHotbarFirst() : null;
            if (container == null) {
                return;
            }
            container.setItemStackForSlot((short)1, new ItemStack("Weapon_Arrow_Crude", 1));
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void ensureActiveHotbarSlotZero(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return;
        }
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return;
            }
            for (Method m : inventory.getClass().getMethods()) {
                if (!m.getName().equals("setActiveHotbarSlot") || m.getParameterCount() != 1) continue;
                Class<?> type = m.getParameterTypes()[0];
                try {
                    if (type == Byte.TYPE || type == Byte.class) {
                        m.invoke((Object)inventory, (byte)0);
                    } else if (type == Short.TYPE || type == Short.class) {
                        m.invoke((Object)inventory, (short)0);
                    } else {
                        if (type != Integer.TYPE && type != Integer.class) continue;
                        m.invoke((Object)inventory, 0);
                    }
                    return;
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private boolean isAmmoItemId(String itemId) {
        if (itemId == null) {
            return false;
        }
        String lower = itemId.toLowerCase(Locale.ROOT);
        return lower.contains("arrow") || lower.contains("ammo") || lower.contains("bolt") || lower.contains("quiver") || lower.contains("bullet") || lower.contains("cartridge");
    }

    private float estimateRangedDamage(CompanionRecord companion) {
        String lower;
        int combatLevel = companion != null ? Math.max(1, companion.combatLevel) : 1;
        String weaponId = companion != null ? companion.getEquipped(EquipmentSlot.WEAPON) : null;
        String string = lower = weaponId != null ? weaponId.toLowerCase(Locale.ROOT) : "";
        float base = lower.contains("crossbow") ? 8.0f : (lower.contains("gun") || lower.contains("rifle") || lower.contains("pistol") || lower.contains("handgun") || lower.contains("blunderbuss") ? 9.0f : 6.0f);
        return Math.min(24.0f, base + (float)combatLevel * 0.2f);
    }

    private float estimateMeleeDamage(CompanionRecord companion) {
        int combatLevel = companion != null ? Math.max(1, companion.combatLevel) : 1;
        String weaponId = companion != null ? companion.getEquipped(EquipmentSlot.WEAPON) : null;
        String lower = weaponId != null ? weaponId.toLowerCase(Locale.ROOT) : "";
        float base = 4.0f;
        if (lower.contains("dagger")) {
            base = 4.5f;
        } else if (lower.contains("sword")) {
            base = 6.0f;
        } else if (lower.contains("mace")) {
            base = 7.0f;
        } else if (lower.contains("axe")) {
            base = 7.5f;
        } else if (lower.contains("spear")) {
            base = 6.5f;
        }
        return Math.min(24.0f, base + (float)combatLevel * 0.3f);
    }

    public boolean consumeKillSignal(CompanionRuntimeState runtime) {
        if (runtime.pendingKillCredit) {
            runtime.pendingKillCredit = false;
            return true;
        }
        return false;
    }

    private boolean isFriendlyEntity(Store<EntityStore> store, Ref<EntityStore> ref, Set<UUID> friendlyEntityIds) {
        if (friendlyEntityIds == null || friendlyEntityIds.isEmpty()) {
            return false;
        }
        try {
            UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComp != null) {
                return friendlyEntityIds.contains(uuidComp.getUuid());
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return false;
    }

    private boolean isPlayerEntity(Store<EntityStore> store, Ref<EntityStore> ref) {
        try {
            Player player = (Player)store.getComponent(ref, Player.getComponentType());
            return player != null;
        }
        catch (Throwable t) {
            return false;
        }
    }

    private boolean isProtectedPassiveNpc(Store<EntityStore> store, Ref<EntityStore> ref) {
        try {
            NPCEntity npc = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
            if (npc == null) {
                return false;
            }
            String roleName = npc.getRoleName();
            if (roleName == null || roleName.isBlank()) {
                return false;
            }
            String roleLower = roleName.toLowerCase(Locale.ROOT);
            for (String token : HOSTILE_ROLE_KEYWORDS) {
                if (!roleLower.contains(token)) continue;
                return false;
            }
            for (String token : PROTECTED_PASSIVE_ROLE_KEYWORDS) {
                if (!roleLower.contains(token)) continue;
                return true;
            }
            return false;
        }
        catch (Throwable t) {
            return false;
        }
    }

    private boolean isLikelyHostileNpc(Store<EntityStore> store, Ref<EntityStore> ref) {
        try {
            NPCEntity npc = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
            if (npc == null) {
                return false;
            }
            String roleLower = "";
            try {
                String roleName = npc.getRoleName();
                roleLower = roleName == null ? "" : roleName.toLowerCase(Locale.ROOT);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            for (String token : HOSTILE_ROLE_KEYWORDS) {
                if (roleLower.isEmpty() || !roleLower.contains(token)) continue;
                return true;
            }
            return false;
        }
        catch (Throwable t) {
            return false;
        }
    }

    private boolean isInvalidCombatNpc(Store<EntityStore> store, Ref<EntityStore> ref) {
        try {
            NPCEntity npc = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
            if (npc == null) {
                return false;
            }
            String roleName = npc.getRoleName();
            if (roleName == null || roleName.isBlank()) {
                return false;
            }
            String roleLower = roleName.toLowerCase(Locale.ROOT);
            for (String token : INVALID_COMBAT_ROLE_KEYWORDS) {
                if (!roleLower.contains(token)) continue;
                return true;
            }
            return false;
        }
        catch (Throwable t) {
            return false;
        }
    }

    private boolean isCompanionNpc(Store<EntityStore> store, Ref<EntityStore> ref) {
        try {
            NPCEntity npc = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
            if (npc == null) {
                return false;
            }
            String roleName = npc.getRoleName();
            if (roleName == null || roleName.isBlank()) {
                return false;
            }
            String roleLower = roleName.toLowerCase(Locale.ROOT);
            return roleLower.contains("companion_recruitable") || roleLower.contains("companion_managed") || roleLower.contains("kweebec_companion_recruitable");
        }
        catch (Throwable t) {
            return false;
        }
    }

    private boolean hasSaneCombatPosition(Store<EntityStore> store, Ref<EntityStore> ref, Vector3d companionPos, double scanRadius) {
        if (store == null || ref == null || companionPos == null) {
            return false;
        }
        try {
            TransformComponent tf = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
            if (tf == null || tf.getPosition() == null) {
                return false;
            }
            return companionPos.distanceTo(tf.getPosition()) <= scanRadius + 2.0;
        }
        catch (Throwable t) {
            return false;
        }
    }

    private boolean isReactiveAttacker(Store<EntityStore> store, Ref<EntityStore> ref, CompanionRuntimeState runtime) {
        if (store == null || ref == null || runtime == null) {
            return false;
        }
        if (runtime.reactiveAttackerId == null || runtime.reactiveAttackerId.isBlank()) {
            return false;
        }
        if (runtime.reactiveAttackerUntilMs <= System.currentTimeMillis()) {
            return false;
        }
        try {
            UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComp == null || uuidComp.getUuid() == null) {
                return false;
            }
            return runtime.reactiveAttackerId.equalsIgnoreCase(uuidComp.getUuid().toString());
        }
        catch (Throwable t) {
            return false;
        }
    }

    private double scoreCombatTarget(Vector3d ownerPos, WorldQueries.NearbyEntity hostile, UUID currentTargetId, Store<EntityStore> store) {
        double score = 1000.0 - hostile.distance * 50.0;
        if (currentTargetId != null) {
            try {
                UUIDComponent uuidComp = (UUIDComponent)store.getComponent(hostile.ref, UUIDComponent.getComponentType());
                if (uuidComp != null && currentTargetId.equals(uuidComp.getUuid())) {
                    score += 200.0;
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        if (hostile.distance < 4.0) {
            score += 150.0;
        }
        return score;
    }

    private void onTargetKilled(CompanionRuntimeState runtime, String targetId, long currentTick) {
        if (runtime == null || targetId == null || targetId.isBlank()) {
            return;
        }
        if (!this.isRecentCombatDamageTarget(runtime, targetId, currentTick)) {
            return;
        }
        this.cleanupClaimedKills(currentTick);
        Long claimedAt = this.claimedKillTargetTicks.putIfAbsent(targetId, currentTick);
        if (claimedAt == null) {
            runtime.pendingKillCredit = true;
            runtime.recentCombatDamageTargetId = null;
            runtime.recentCombatDamageTick = 0L;
        }
    }

    private void markRecentCombatDamage(CompanionRuntimeState runtime, Ref<EntityStore> targetRef, Store<EntityStore> store) {
        if (runtime == null || targetRef == null || store == null) {
            return;
        }
        try {
            UUIDComponent uuidComp = (UUIDComponent)store.getComponent(targetRef, UUIDComponent.getComponentType());
            if (uuidComp == null || uuidComp.getUuid() == null) {
                return;
            }
            runtime.recentCombatDamageTargetId = uuidComp.getUuid().toString();
            runtime.recentCombatDamageTick = runtime.currentTick;
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private boolean isRecentCombatDamageTarget(CompanionRuntimeState runtime, String targetId, long currentTick) {
        if (runtime == null || targetId == null || targetId.isBlank()) {
            return false;
        }
        if (runtime.recentCombatDamageTargetId == null || runtime.recentCombatDamageTargetId.isBlank()) {
            return false;
        }
        if (!targetId.equals(runtime.recentCombatDamageTargetId)) {
            return false;
        }
        return currentTick - runtime.recentCombatDamageTick <= 160L;
    }

    private void pollRecentDamageKillCredit(World world, Store<EntityStore> store, CompanionRuntimeState runtime, long currentTick) {
        if (runtime == null || store == null) {
            return;
        }
        String targetId = runtime.recentCombatDamageTargetId;
        if (targetId == null || targetId.isBlank()) {
            return;
        }
        if (currentTick - runtime.recentCombatDamageTick > 160L) {
            runtime.recentCombatDamageTargetId = null;
            runtime.recentCombatDamageTick = 0L;
            return;
        }
        if (world != null && this.isDeadOrMissingTarget(world, store, targetId)) {
            this.onTargetKilled(runtime, targetId, currentTick);
        }
    }

    private void cleanupClaimedKills(long currentTick) {
        if (this.claimedKillTargetTicks.isEmpty()) {
            return;
        }
        this.claimedKillTargetTicks.entrySet().removeIf(e -> currentTick - (Long)e.getValue() > 200L);
    }

    private void setNewCombatTarget(World world, Store<EntityStore> store, NPCEntity npcEntity, PlayerRef ownerRef, Ref<EntityStore> companionRef, Ref<EntityStore> targetRef, CompanionRuntimeState runtime) {
        try {
            String nextTargetId;
            UUIDComponent uuidComp = (UUIDComponent)store.getComponent(targetRef, UUIDComponent.getComponentType());
            String string = nextTargetId = uuidComp != null ? uuidComp.getUuid().toString() : null;
            if (nextTargetId != null && nextTargetId.equals(runtime.combatTargetId)) {
                return;
            }
            if (uuidComp != null) {
                runtime.combatTargetId = nextTargetId;
            }
            runtime.combatStickyTicks = 0;
            npcEntity.onFlockSetTarget("LockedTarget", targetRef);
            try {
                npcEntity.onFlockSetTarget("Target", targetRef);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                npcEntity.onFlockSetTarget("CombatTarget", targetRef);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                npcEntity.onFlockSetTarget("CombatTargetRanged", null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                npcEntity.onFlockSetTarget("CombatTargetBow", null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                npcEntity.onFlockSetTarget("CombatTargetCrossbow", null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                npcEntity.onFlockSetTarget("CombatTargetGun", null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            this.clearMeleeTargetChannels(npcEntity);
            try {
                npcEntity.onFlockSetTarget("CombatTargetShieldBlock", null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            npcEntity.getRole().getWorldSupport().requestNewPath();
            this.safeOverrideAttitude(npcEntity, targetRef, Attitude.HOSTILE, 12.0);
            String roleName = "unknown";
            try {
                NPCEntity targetNpc = (NPCEntity)store.getComponent(targetRef, NPCEntity.getComponentType());
                if (targetNpc != null && targetNpc.getRoleName() != null) {
                    roleName = targetNpc.getRoleName();
                }
            }
            catch (Throwable throwable) {}
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to set combat target.");
        }
    }

    private String describeTargetRole(World world, Store<EntityStore> store, String targetIdStr) {
        UUID targetId = this.parseUuid(targetIdStr);
        if (targetId == null) {
            return "invalid-uuid";
        }
        Ref ref = world.getEntityRef(targetId);
        if (ref == null || !ref.isValid()) {
            return "missing";
        }
        return this.describeTargetRole((Ref<EntityStore>)ref, store);
    }

    private String describeTargetRole(Ref<EntityStore> targetRef, Store<EntityStore> store) {
        if (targetRef == null || store == null) {
            return "missing";
        }
        try {
            NPCEntity npc = (NPCEntity)store.getComponent(targetRef, NPCEntity.getComponentType());
            if (npc != null && npc.getRoleName() != null && !npc.getRoleName().isBlank()) {
                return npc.getRoleName();
            }
            Player player = (Player)store.getComponent(targetRef, Player.getComponentType());
            if (player != null) {
                return "player";
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return "unknown";
    }

    private void safeOverrideAttitude(NPCEntity npcEntity, Ref<EntityStore> targetRef, Attitude attitude, double seconds) {
        if (!this.attitudeOverridesAvailable || npcEntity == null || targetRef == null || attitude == null) {
            return;
        }
        try {
            npcEntity.getRole().getWorldSupport().overrideAttitude(targetRef, attitude, seconds);
        }
        catch (Throwable t) {
            this.attitudeOverridesAvailable = false;
            this.logger.at(Level.INFO).log("Companion attitude override unavailable; continuing without overrides.");
        }
    }

    private void forceEnemyFocusOnCompanion(Store<EntityStore> store, Ref<EntityStore> hostileRef, Ref<EntityStore> companionRef, PlayerRef ownerRef, long currentTick) {
        if (store == null || hostileRef == null || companionRef == null || ownerRef == null) {
            return;
        }
        try {
            NPCEntity hostileNpc = (NPCEntity)store.getComponent(hostileRef, NPCEntity.getComponentType());
            if (hostileNpc == null) {
                return;
            }
            try {
                hostileNpc.onFlockSetTarget("LockedTarget", companionRef);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                hostileNpc.onFlockSetTarget("Target", companionRef);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                hostileNpc.getRole().getWorldSupport().overrideAttitude(companionRef, Attitude.HOSTILE, 10.0);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                hostileNpc.getRole().getWorldSupport().overrideAttitude(ownerRef.getReference(), Attitude.NEUTRAL, 6.0);
            }
            catch (Throwable throwable) {}
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private boolean tryTriggerTauntEmote(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return false;
        }
        String[] methodNames = new String[]{"playEmote", "setEmote", "performEmote", "playAnimation", "setAnimation"};
        String[] taunts = new String[]{"chicken", "Chicken", "ChickenDance", "Emote_ChickenDance", "Taunt", "Wave"};
        try {
            for (Method m : npcEntity.getClass().getMethods()) {
                String name = m.getName();
                boolean matches = false;
                for (String candidate : methodNames) {
                    if (!candidate.equalsIgnoreCase(name)) continue;
                    matches = true;
                    break;
                }
                if (!matches || m.getParameterCount() != 1 || m.getParameterTypes()[0] != String.class) continue;
                for (String taunt : taunts) {
                    try {
                        m.invoke((Object)npcEntity, taunt);
                        return true;
                    }
                    catch (Throwable throwable) {
                    }
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return false;
    }

    private boolean tryTriggerBlockAnimation(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, String profile, boolean shieldEquipped) {
        if (npcEntity == null || companionRef == null || store == null) {
            return false;
        }
        String[] methodNames = new String[]{"playAnimation", "setAnimation", "playEmote", "setEmote", "performEmote"};
        String[] blockKeys = this.getBlockAnimationKeys(profile, shieldEquipped);
        try {
            for (Method m : npcEntity.getClass().getMethods()) {
                Object slot;
                String name = m.getName();
                boolean matches = false;
                for (String candidate : methodNames) {
                    if (!candidate.equalsIgnoreCase(name)) continue;
                    matches = true;
                    break;
                }
                if (!matches) continue;
                if ("playAnimation".equalsIgnoreCase(name) && m.getParameterCount() == 4 && Ref.class.isAssignableFrom(m.getParameterTypes()[0]) && m.getParameterTypes()[2] == String.class && (slot = this.resolveAnimationSlot(m.getParameterTypes()[1])) != null) {
                    String[] stringArray = blockKeys;
                    int n = stringArray.length;
                    for (int candidate = 0; candidate < n; ++candidate) {
                        String key = stringArray[candidate];
                        try {
                            m.invoke((Object)npcEntity, companionRef, slot, key, store);
                            return true;
                        }
                        catch (Throwable throwable) {
                            continue;
                        }
                    }
                }
                if (m.getParameterCount() != 1 || m.getParameterTypes()[0] != String.class) continue;
                for (String key : blockKeys) {
                    try {
                        m.invoke((Object)npcEntity, key);
                        return true;
                    }
                    catch (Throwable throwable) {
                    }
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return false;
    }

    private String[] getBlockAnimationKeys(String profile, boolean shieldEquipped) {
        LinkedHashSet<String> keys = new LinkedHashSet<String>();
        if (shieldEquipped || WEAPON_SHIELD.equals(profile)) {
            keys.add("Shield_Guard");
            keys.add("shield_guard");
            keys.add("GuardBash");
            keys.add("GuardHurt");
            keys.add("Guard");
            keys.add("guard");
            keys.add("Guard_Bash");
            keys.add("guardbash");
            keys.add("block");
            keys.add("Block");
            return keys.toArray(new String[0]);
        }
        switch (profile) {
            case "DAGGER": {
                keys.add("Dagger_Guard");
                keys.add("Daggers_Guard");
                break;
            }
            case "SWORD": {
                keys.add("Sword_Guard");
                break;
            }
            case "AXE": {
                keys.add("Axe_Guard");
                break;
            }
            case "MACE": {
                keys.add("Mace_Guard");
                break;
            }
            case "SPEAR": {
                keys.add("Spear_Guard");
                break;
            }
        }
        keys.add("Guard");
        keys.add("guard");
        keys.add("Guard_Bash");
        keys.add("guardbash");
        keys.add("block");
        keys.add("Block");
        return keys.toArray(new String[0]);
    }

    private Object resolveAnimationSlot(Class<?> slotType) {
        if (slotType == null || !slotType.isEnum()) {
            return null;
        }
        try {
            String[] preferred;
            ?[] constants = slotType.getEnumConstants();
            if (constants == null || constants.length == 0) {
                return null;
            }
            for (String want : preferred = new String[]{"Status", "Item", "Main", "MainHand", "OffHand", "UpperBody", "Body"}) {
                for (Object c : constants) {
                    if (!want.equalsIgnoreCase(String.valueOf(c))) continue;
                    return c;
                }
            }
            return constants[0];
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private boolean tryTriggerWeaponBlockInteraction(NPCEntity npcEntity, String profile, boolean shieldEquipped) {
        if (npcEntity == null) {
            return false;
        }
        String[] methodNames = new String[]{"executeInteraction", "triggerInteraction", "useInteraction", "performInteraction", "startInteraction", "queueInteraction"};
        String[] interactionIds = this.getBlockInteractionIds(profile, shieldEquipped);
        try {
            for (Method m : npcEntity.getClass().getMethods()) {
                String name = m.getName();
                boolean matches = false;
                for (String candidate : methodNames) {
                    if (!candidate.equalsIgnoreCase(name)) continue;
                    matches = true;
                    break;
                }
                if (!matches) continue;
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class) {
                    for (String interactionId : interactionIds) {
                        try {
                            m.invoke((Object)npcEntity, interactionId);
                            return true;
                        }
                        catch (Throwable throwable) {
                        }
                    }
                }
                if (m.getParameterCount() != 2 || m.getParameterTypes()[0] != String.class || m.getParameterTypes()[1] != Boolean.TYPE && m.getParameterTypes()[1] != Boolean.class) continue;
                for (String interactionId : interactionIds) {
                    try {
                        m.invoke((Object)npcEntity, interactionId, Boolean.TRUE);
                        return true;
                    }
                    catch (Throwable throwable) {
                    }
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return false;
    }

    private String[] getBlockInteractionIds(String profile, boolean shieldEquipped) {
        LinkedHashSet<String> ids = new LinkedHashSet<String>();
        if (shieldEquipped || WEAPON_SHIELD.equals(profile)) {
            ids.add("Skeleton_Knight_Shield_Block");
            ids.add("Skeleton_Burnt_Praetorian_Shield_Block");
            ids.add("Skeleton_Burnt_Soldier_Shield_Block");
            ids.add("Weapon_Shield_Secondary_Guard");
            ids.add("Weapon_Shield_Secondary_Guard_Wield");
            ids.add("Shield_Block");
        }
        switch (profile) {
            case "SWORD": {
                ids.add("Outlander_Marauder_Sword_Block");
                break;
            }
            case "AXE": 
            case "MACE": 
            case "DAGGER": 
            case "SPEAR": 
            case "MELEE_GENERIC": {
                ids.add("Outlander_Brute_Block");
                break;
            }
        }
        ids.add("Outlander_Marauder_Sword_Block");
        ids.add("Outlander_Brute_Block");
        return ids.toArray(new String[0]);
    }

    private void logShieldRuntimeMethodSnapshotOnce(NPCEntity npcEntity) {
    }

    private void clearCombatTarget(CompanionRuntimeState runtime, NPCEntity npcEntity) {
        runtime.combatTargetId = null;
        runtime.combatStickyTicks = 0;
        runtime.lastCombatPathTick = 0L;
        runtime.stuckTicks = 0;
        runtime.followBootstrapTicks = 40;
        runtime.lastFollowTargetRefreshTick = 0L;
        runtime.lastCombatScanTick = 0L;
        if (npcEntity != null) {
            try {
                npcEntity.onFlockSetTarget("CombatTarget", null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                npcEntity.onFlockSetTarget("CombatTargetRanged", null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                npcEntity.onFlockSetTarget("CombatTargetBow", null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                npcEntity.onFlockSetTarget("CombatTargetCrossbow", null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                npcEntity.onFlockSetTarget("CombatTargetGun", null);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            this.clearMeleeTargetChannels(npcEntity);
        }
    }

    public void abandonCombat(CompanionRuntimeState runtime, NPCEntity npcEntity) {
        if (runtime == null) {
            return;
        }
        this.clearCombatTarget(runtime, npcEntity);
    }

    private boolean isTargetValid(World world, Store<EntityStore> store, String targetIdStr, Vector3d ownerPos, CompanionRuntimeState runtime) {
        UUID targetId = this.parseUuid(targetIdStr);
        if (targetId == null) {
            return false;
        }
        Ref ref = world.getEntityRef(targetId);
        if (ref == null || !ref.isValid()) {
            return false;
        }
        if (this.isPlayerEntity(store, (Ref<EntityStore>)ref)) {
            return false;
        }
        if (this.isInvalidCombatNpc(store, (Ref<EntityStore>)ref)) {
            return false;
        }
        if (this.isProtectedPassiveNpc(store, (Ref<EntityStore>)ref) && !this.isReactiveAttacker(store, (Ref<EntityStore>)ref, runtime)) {
            return false;
        }
        DeathComponent death = (DeathComponent)store.getComponent(ref, DeathComponent.getComponentType());
        return death == null;
    }

    private boolean isDeadOrMissingTarget(World world, Store<EntityStore> store, String targetIdStr) {
        UUID targetId = this.parseUuid(targetIdStr);
        if (targetId == null) {
            return false;
        }
        Ref ref = world.getEntityRef(targetId);
        if (ref == null || !ref.isValid()) {
            return true;
        }
        DeathComponent death = (DeathComponent)store.getComponent(ref, DeathComponent.getComponentType());
        return death != null;
    }

    private boolean isBlockedCombatTarget(Store<EntityStore> store, Ref<EntityStore> ref, CompanionRuntimeState runtime) {
        if (runtime == null || runtime.combatBlockedTargetId == null || runtime.currentTick > runtime.combatBlockedUntilTick) {
            return false;
        }
        try {
            UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ref, UUIDComponent.getComponentType());
            return uuidComp != null && runtime.combatBlockedTargetId.equals(uuidComp.getUuid().toString());
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isNpcInStartState(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return false;
        }
        try {
            String state = npcEntity.getRole().getStateSupport().getStateName();
            if (state == null) {
                return false;
            }
            return state.toLowerCase(Locale.ROOT).startsWith("start.");
        }
        catch (Throwable ignored) {
            return false;
        }
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

