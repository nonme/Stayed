/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.hypixel.hytale.component.Component
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.RemoveReason
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.logger.HytaleLogger
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 *  com.hypixel.hytale.math.vector.Transform
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.math.vector.Vector3f
 *  com.hypixel.hytale.protocol.ItemArmorSlot
 *  com.hypixel.hytale.protocol.PlayerSkin
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.asset.type.attitude.Attitude
 *  com.hypixel.hytale.server.core.asset.type.item.config.ItemArmor
 *  com.hypixel.hytale.server.core.asset.type.model.config.Model
 *  com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset
 *  com.hypixel.hytale.server.core.cosmetics.CosmeticsModule
 *  com.hypixel.hytale.server.core.entity.UUIDComponent
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.nameplate.Nameplate
 *  com.hypixel.hytale.server.core.inventory.Inventory
 *  com.hypixel.hytale.server.core.inventory.ItemStack
 *  com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer
 *  com.hypixel.hytale.server.core.inventory.container.ItemContainer
 *  com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction
 *  com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent
 *  com.hypixel.hytale.server.core.modules.entity.component.ModelComponent
 *  com.hypixel.hytale.server.core.modules.entity.component.PersistentModel
 *  com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
 *  com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent
 *  com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent
 *  com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap
 *  com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue
 *  com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.Universe
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  com.hypixel.hytale.server.npc.NPCPlugin
 *  com.hypixel.hytale.server.npc.entities.NPCEntity
 *  com.hypixel.hytale.server.npc.movement.controllers.MotionController
 *  com.hypixel.hytale.server.npc.role.Role
 *  com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport
 *  com.hypixel.hytale.server.npc.role.support.StateSupport
 *  com.hypixel.hytale.server.npc.role.support.WorldSupport
 *  it.unimi.dsi.fastutil.Pair
 */
package com.yourname.companion.systems;

import com.google.gson.Gson;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.ItemArmorSlot;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemArmor;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.movement.controllers.MotionController;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import com.hypixel.hytale.server.npc.role.support.StateSupport;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;
import com.yourname.companion.data.BlockPos;
import com.yourname.companion.data.CompanionMode;
import com.yourname.companion.data.CompanionRecord;
import com.yourname.companion.data.CompanionState;
import com.yourname.companion.data.EquipmentSlot;
import com.yourname.companion.data.FollowMode;
import com.yourname.companion.data.PlayerCompanionData;
import com.yourname.companion.data.ProgressionConfig;
import com.yourname.companion.data.ReviveConfig;
import com.yourname.companion.runtime.CompanionRuntimeState;
import com.yourname.companion.runtime.MovementOwner;
import com.yourname.companion.runtime.PlayerRuntimeState;
import com.yourname.companion.runtime.Vec3;
import com.yourname.companion.storage.CompanionManager;
import com.yourname.companion.systems.CombatAssist;
import com.yourname.companion.systems.DepositSystem;
import com.yourname.companion.systems.FarmSystem;
import com.yourname.companion.systems.LootSystem;
import com.yourname.companion.systems.MinerSystem;
import com.yourname.companion.systems.ReviveBenchIntegration;
import com.yourname.companion.systems.WorkPositioning;
import com.yourname.companion.util.InvUtil;
import com.yourname.companion.util.WorldQueries;
import it.unimi.dsi.fastutil.Pair;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class CompanionSystem {
    private static final String MINING_VISUAL_PICKAXE_ID = "Tool_Pickaxe_Crude";
    private static final String FARM_VISUAL_HOE_ID = "Tool_Hoe_Crude";
    private static final String FARM_VISUAL_SICKLE_ID = "Tool_Sickle_Crude";
    private static final String FARM_VISUAL_WATERING_CAN_ID = "Tool_Watering_Can";
    private static final Set<String> FARM_VISUAL_TOOL_IDS = Set.of("Tool_Hoe_Crude".toLowerCase(Locale.ROOT), "Tool_Sickle_Crude".toLowerCase(Locale.ROOT), "Tool_Watering_Can".toLowerCase(Locale.ROOT));
    private static final boolean DEBUG_RECRUIT_FOLLOW = false;
    private static final boolean DEBUG_APPEARANCE_PERSIST = false;
    private static final boolean DEBUG_APPEARANCE_CAPTURE_DIAG = false;
    private static final boolean DEBUG_MINER_TELEPORT_TRACE = false;
    private static final boolean DEBUG_COMPANION_AI_STATE = false;
    private static final boolean DEBUG_FARM_DIAG = false;
    private static final boolean ENABLE_RECRUIT_SCAN_FALLBACK = true;
    private static final int COMPANION_HOTBAR_SLOTS = 9;
    private static final int COMPANION_STORAGE_SLOTS = 18;
    private static final int COMPANION_OFFHAND_SLOTS = 1;
    private static final long AI_TICK_MS = 250L;
    private static final long TICKS_PER_SECOND = 20L;
    private static final long AUTO_REVIVE_CHECK_INTERVAL_TICKS = 20L;
    private static final List<String> HUMAN_SAFE_BODY_CHARACTERISTICS = List.of("Default", "Muscular");
    private static final String HUMAN_SAFE_EARS = "Default";
    private static final Set<String> DISALLOWED_RECRUIT_MOUTHS = Set.of("Mouth_Vampire", "Mouth_Orc");
    private static final String DEFAULT_RECRUIT_FACE = "Face_Neutral";
    private static final String DEFAULT_RECRUIT_MOUTH = "Mouth_Default";
    private static final int STUCK_MAX_TICKS = 60;
    private static final double STUCK_MIN_MOVEMENT = 0.2;
    private static final long REGEN_DELAY_TICKS = 140L;
    private static final long REGEN_INTERVAL_TICKS = 20L;
    private static final float REGEN_PCT_PER_TICK = 0.015f;
    private static final long INVENTORY_SNAPSHOT_INTERVAL_TICKS = 40L;
    private static final long INVENTORY_LAYOUT_REPAIR_INTERVAL_TICKS = 100L;
    private static final long APPEARANCE_SNAPSHOT_INTERVAL_TICKS = 40L;
    private static final long RECRUIT_APPEARANCE_FINALIZE_TICKS = 20L;
    private static final long WORK_ACTIVE_INVALID_REF_GRACE_TICKS = 2400L;
    private static final long AUTONOMOUS_INVALID_REF_GRACE_TICKS = 40L;
    private static final double FOLLOW_START_BUFFER = 1.5;
    private static final double FOLLOW_STOP_BUFFER = 0.5;
    private static final double TELEPORT_FOLLOW_DISTANCE = 25.0;
    private static final double COMBAT_LEASH_DISTANCE = 20.0;
    private static final double STAY_LEASH_DISTANCE = 3.0;
    private static final String COMPANION_ROLE = "FH_Companion_Managed";
    private static final String RANDOM_COSMETIC_TOKEN = "__RANDOM_COSMETIC__";
    private static final String PLAYER_SKIN_TOKEN_PREFIX = "PSK|";
    private static final String APPEARANCE_MODEL_TOKEN_PREFIX = "PMOD|";
    private static final int APPEARANCE_CAPTURE_MAX_ATTEMPTS = 8;
    private static final long APPEARANCE_CAPTURE_RETRY_TICKS = 20L;
    private static final int FOLLOW_BOOTSTRAP_TICKS = 4;
    private static final long FOLLOW_STALL_REPATH_TICKS = 40L;
    private static final int FOLLOW_MOVE_REPATH_STUCK_TICKS = 40;
    private static final double FOLLOW_BOOTSTRAP_SPEED = 0.28;
    private static final long FOLLOW_TARGET_REFRESH_TICKS = 40L;
    private static final long FOLLOW_PERIODIC_REPATH_TICKS = 80L;
    private static final double FOLLOW_FAR_REPATH_EXTRA_DISTANCE = 6.0;
    private static final double ANTI_LAUNCH_MAX_VERTICAL_DELTA = 2.25;
    private static final long START_STATE_NUDGE_COOLDOWN_TICKS = 20L;
    private static final boolean ENABLE_SUFFOCATION_BLOCK_PROBE = true;
    private static final long SUFFOCATION_RECOVER_COOLDOWN_TICKS = 20L;
    private static final String RANDOM_HUMAN_MODEL = "PlayerTestModel_V";
    private static final String RANDOM_HUMAN_MODEL_MERCHANT = "PlayerTestModel_G";
    private static final String RANDOM_HUMAN_MODEL_GUILD = "PlayerTestModel_V";
    private static final int DEFAULT_MODEL_LIST_LIMIT = 80;
    private static final List<String> HUMAN_ROLE_PRIORITY = Arrays.asList("human", "commoner", "villager", "citizen", "civilian", "merchant", "guard", "farmer", "npc", "player");
    private static final List<String> HUMAN_MODEL_INCLUDE = Arrays.asList("human", "commoner", "villager", "citizen", "civilian", "merchant", "guard", "farmer", "person", "humanoid", "player");
    private static final List<String> ANIMAL_MODEL_EXCLUDE = Arrays.asList("fox", "wolf", "bear", "boar", "deer", "bat", "salmon", "fish", "bird", "spider", "skeleton", "zombie", "golem", "kweebec", "orc", "goblin", "dragon", "slime");
    private static final Gson APPEARANCE_GSON = new Gson();
    private static final Map<String, Double> WEAPON_ATK_OVERRIDES = Map.ofEntries(Map.entry("weapon_sword_crude", 2.0), Map.entry("weapon_sword_wood", 2.0), Map.entry("weapon_sword_iron", 5.0), Map.entry("weapon_sword_steel", 8.0), Map.entry("weapon_sword_diamond", 12.0), Map.entry("weapon_sword_cobalt", 12.0), Map.entry("weapon_mace_crude", 6.0), Map.entry("weapon_mace_iron", 6.0), Map.entry("weapon_mace_steel", 8.0), Map.entry("weapon_mace_diamond", 10.0), Map.entry("weapon_mace_cobalt", 10.0), Map.entry("weapon_shortbow_crude", 4.0), Map.entry("weapon_shortbow", 4.0), Map.entry("weapon_crossbow", 7.0), Map.entry("weapon_battleaxe", 10.0), Map.entry("weapon_battleaxe_iron", 10.0), Map.entry("weapon_battleaxe_thorium", 13.0), Map.entry("weapon_battleaxe_cobalt", 15.0), Map.entry("weapon_daggers", 4.0), Map.entry("weapon_daggers_iron", 4.0), Map.entry("weapon_daggers_thorium", 6.0), Map.entry("weapon_daggers_cobalt", 7.0), Map.entry("weapon_spear", 6.0), Map.entry("weapon_shield", 0.0), Map.entry("shield_diamond", 0.0));
    private static final Map<String, Double> ARMOR_DEF_OVERRIDES = Map.ofEntries(Map.entry("armor_copper_head", 1.0), Map.entry("armor_copper_chest", 1.5), Map.entry("armor_copper_legs", 1.25), Map.entry("armor_copper_hands", 0.75), Map.entry("armor_iron_head", 1.5), Map.entry("armor_iron_chest", 2.25), Map.entry("armor_iron_legs", 1.875), Map.entry("armor_iron_hands", 1.125), Map.entry("armor_steel_head", 2.0), Map.entry("armor_steel_chest", 3.0), Map.entry("armor_steel_legs", 2.5), Map.entry("armor_steel_hands", 1.5), Map.entry("armor_diamond_head", 2.5), Map.entry("armor_diamond_chest", 3.75), Map.entry("armor_diamond_legs", 3.125), Map.entry("armor_diamond_hands", 1.875), Map.entry("shield_diamond", 5.0), Map.entry("weapon_shield", 5.0));
    private final CompanionManager companionManager;
    private final HytaleLogger logger;
    private final Map<String, AppearanceSnapshot> cachedAppearanceByCompanionId = new ConcurrentHashMap<String, AppearanceSnapshot>();
    private ScheduledExecutorService executor;
    private final CombatAssist combatAssist;
    private final LootSystem lootSystem;
    private final DepositSystem depositSystem;
    private final FarmSystem farmSystem;
    private final MinerSystem minerSystem;
    private final Map<UUID, Long> lastUiOpenMsByPlayer = new ConcurrentHashMap<UUID, Long>();
    private volatile boolean attitudeOverridesAvailable = true;
    private long globalTick = 0L;
    private Consumer<PlayerRef> interactionMenuCallback;
    private ReviveBenchIntegration reviveBenchIntegration;
    private volatile boolean tickHeartbeatLogged = false;
    private static final double PATROL_LEASH_DISTANCE = 20.0;
    private static final double PATROL_SEGMENT_DISTANCE = 15.0;
    private static final double PATROL_REACH_DISTANCE = 1.15;
    private static final double PATROL_WAYPOINT_STEP = 2.5;
    private static final double FREE_ROAM_RADIUS = 8.0;
    private static final double FREE_ROAM_MIN_DISTANCE = 3.0;
    private static final double FREE_ROAM_STEP_DISTANCE = 7.0;
    private static final double FREE_ROAM_REACH_DISTANCE = 1.15;
    private static final double AUTONOMOUS_OWNER_REDIRECT_DISTANCE = 100.0;
    private static final double AUTONOMOUS_OWNER_REDIRECT_OFFSET = 10.0;
    private static final long FREE_ROAM_RETARGET_TICKS = 100L;
    private static final long FREE_ROAM_BREAK_COOLDOWN_TICKS = 12L;
    private static final Set<String> RECRUITABLE_ROLES = Set.of("FH_Companion_Recruitable", "FH_Companion_Recruitable_Merchant", "FH_Companion_Recruitable_Guild", "Companion_Recruitable", "Companion_Recruitable_Merchant", "Companion_Recruitable_Guild", "Kweebec_Companion_Recruitable");
    private static final double RECRUIT_SCAN_RADIUS = 8.0;
    private static final long RECRUIT_THREAT_REFRESH_INTERACT_GRACE_MS = 2000L;
    private static final double RECRUITABLE_THREAT_SCAN_RADIUS = 12.0;
    private static final String[] RECRUITABLE_THREAT_ROLE_KEYWORDS = new String[]{"spider", "scorpion", "zombie", "skeleton", "wraith", "bandit", "outlaw", "raider", "goblin", "orc", "scarak", "slug", "snake", "wolf", "bear", "hyena", "raptor", "basilisk", "magma", "mole", "rat", "undead", "crawler", "creeper", "void"};

    public CompanionSystem(CompanionManager companionManager, HytaleLogger logger) {
        this.companionManager = companionManager;
        this.logger = logger;
        this.combatAssist = new CombatAssist(logger);
        this.lootSystem = new LootSystem(logger);
        this.depositSystem = new DepositSystem(logger);
        this.farmSystem = new FarmSystem(logger);
        this.minerSystem = new MinerSystem(logger);
    }

    public void setInteractionMenuCallback(Consumer<PlayerRef> callback) {
        this.interactionMenuCallback = callback;
    }

    public void setReviveBenchIntegration(ReviveBenchIntegration integration) {
        this.reviveBenchIntegration = integration;
    }

    public void start() {
        if (this.executor != null) {
            return;
        }
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "companion-ai");
            thread.setDaemon(true);
            return thread;
        });
        this.executor.scheduleAtFixedRate(this::safeTickAll, 250L, 250L, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        if (this.executor != null) {
            this.executor.shutdownNow();
            this.executor = null;
        }
        this.snapshotAllActiveCompanionAppearanceOnly();
        this.snapshotAllActiveCompanionInventoryOnly();
    }

    public boolean summonCompanion(PlayerRef playerRef, CompanionRecord companion) {
        return this.summonCompanion(playerRef, companion, false);
    }

    public boolean summonCompanion(PlayerRef playerRef, CompanionRecord companion, boolean resumeWorkOnSummon) {
        UUID eid;
        if (playerRef == null || companion == null || companion.fallen) {
            return false;
        }
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            return false;
        }
        Vector3d persistedRestorePos = this.preferredRestorePosition(playerRef, companion);
        if (companion.entityId != null && (eid = this.parseUuid(companion.entityId)) != null) {
            Ref ref2;
            block29: {
                Ref ref = world.getEntityRef(eid);
                if (ref != null && ref.isValid()) {
                    try {
                        String roleName;
                        Store store = world.getEntityStore().getStore();
                        NPCEntity npcEntity = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
                        if (npcEntity == null || (roleName = npcEntity.getRoleName()) == null || roleName.equalsIgnoreCase(COMPANION_ROLE)) break block29;
                        this.logger.at(Level.INFO).log("[summonCompanion] entity has wrong role \"" + roleName + "\", removing and respawning as FH_Companion_Managed");
                        try {
                            store.removeEntity(ref, RemoveReason.REMOVE);
                        }
                        catch (Throwable throwable) {
                            // empty catch block
                        }
                        companion.entityId = null;
                    }
                    catch (Throwable store) {
                        // empty catch block
                    }
                }
            }
            if (companion.entityId != null && (ref2 = world.getEntityRef(eid)) != null && ref2.isValid()) {
                CompanionRuntimeState runtime = this.companionManager.getRuntime(companion.uniqueId);
                Store store = world.getEntityStore().getStore();
                TransformComponent transform = (TransformComponent)store.getComponent(ref2, TransformComponent.getComponentType());
                if (persistedRestorePos != null && transform != null && this.shouldUsePersistedRestorePosition(companion)) {
                    transform.teleportPosition(new Vector3d(persistedRestorePos));
                } else if (companion.followMode == FollowMode.FOLLOW) {
                    this.teleportNearOwner(world, (Ref<EntityStore>)ref2, playerRef);
                }
                runtime.state = switch (companion.followMode) {
                    case FollowMode.STAY -> CompanionState.IDLE_STAY;
                    case FollowMode.PATROL -> CompanionState.IDLE_PATROL;
                    case FollowMode.FREE -> CompanionState.IDLE_FREE;
                    default -> CompanionState.IDLE_FOLLOW;
                };
                this.restorePersistentWorkState(companion, runtime, resumeWorkOnSummon);
                runtime.followBootstrapTicks = companion.followMode == FollowMode.FOLLOW ? 4 : 0;
                runtime.stuckTicks = 0;
                runtime.stayAnchor = null;
                this.resetPatrolAndFreeRuntime(runtime);
                try {
                    NPCEntity npcEntity = (NPCEntity)store.getComponent(ref2, NPCEntity.getComponentType());
                    if (npcEntity != null) {
                        if (companion.followMode == FollowMode.STAY) {
                            if (transform != null && transform.getPosition() != null) {
                                runtime.stayAnchor = this.toVec3(transform.getPosition());
                            }
                            this.setHoldTarget(npcEntity, (Ref<EntityStore>)ref2);
                        } else if (companion.followMode == FollowMode.PATROL) {
                            Vector3d currentPos = transform != null ? transform.getPosition() : persistedRestorePos;
                            this.initializePatrolRoute(runtime, transform, currentPos);
                            this.driveAutonomousMovement(npcEntity, runtime, runtime.patrolForwardTarget);
                        } else if (companion.followMode == FollowMode.FREE) {
                            Vector3d currentPos = transform != null ? transform.getPosition() : persistedRestorePos;
                            this.seedFreeRoamDirection(runtime, transform);
                            this.initializeFreeRoam(runtime, currentPos);
                            this.pickNextFreeRoamTarget(runtime, currentPos);
                            this.driveAutonomousMovement(npcEntity, runtime, runtime.freeRoamTarget);
                        } else {
                            this.ensureNonIdleState(npcEntity, (Ref<EntityStore>)ref2, (Store<EntityStore>)store, runtime);
                            this.setFollowTarget(npcEntity, playerRef, (Ref<EntityStore>)ref2, (Store<EntityStore>)store);
                        }
                        try {
                            npcEntity.getRole().getWorldSupport().requestNewPath();
                        }
                        catch (Throwable throwable) {}
                    }
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                return true;
            }
        }
        return this.spawnCompanion(world, playerRef, companion, persistedRestorePos, true, resumeWorkOnSummon);
    }

    public void dismissCompanion(PlayerRef playerRef, CompanionRecord companion) {
        Ref ref;
        if (playerRef == null || companion == null) {
            return;
        }
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            companion.entityId = null;
            companion.active = false;
            return;
        }
        UUID eid = this.parseUuid(companion.entityId);
        if (eid != null && (ref = world.getEntityRef(eid)) != null && ref.isValid()) {
            Store store = world.getEntityStore().getStore();
            try {
                TransformComponent transform = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
                if (transform != null && transform.getPosition() != null) {
                    this.updateCompanionLastKnownLocation(companion, playerRef, transform.getPosition());
                }
            }
            catch (Throwable transform) {
                // empty catch block
            }
            NPCEntity npcEntity = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
            this.cacheAppearanceSnapshot(companion, npcEntity, (Ref<EntityStore>)ref, (Store<EntityStore>)store, "dismiss");
            this.saveCompanionInventory((Store<EntityStore>)store, (Ref<EntityStore>)ref, companion);
            store.removeEntity(ref, RemoveReason.REMOVE);
        }
        companion.entityId = null;
        companion.active = false;
        this.companionManager.resetRuntime(companion.uniqueId);
        this.saveCompanionData("dismissCompanion", companion);
    }

    public int summonAll(PlayerRef playerRef) {
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        int count = 0;
        for (CompanionRecord c : data.companions) {
            if (c.fallen || c.active && c.entityId != null && !c.entityId.isBlank() || !this.summonCompanion(playerRef, c)) continue;
            ++count;
        }
        return count;
    }

    public int dismissAll(PlayerRef playerRef) {
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        int count = 0;
        for (CompanionRecord c : new ArrayList<CompanionRecord>(data.getActiveCompanions())) {
            this.dismissCompanion(playerRef, c);
            ++count;
        }
        return count;
    }

    public CompanionRecord createCompanion(PlayerRef playerRef, String name) {
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        CompanionRecord companion = new CompanionRecord(UUID.randomUUID().toString());
        companion.name = name;
        data.companions.add(companion);
        data.selectedCompanionId = companion.uniqueId;
        this.summonCompanion(playerRef, companion);
        return companion;
    }

    public int cleanupOrphanManagedCompanions(PlayerRef playerRef, double radius) {
        if (playerRef == null) {
            return 0;
        }
        UUID worldId = playerRef.getWorldUuid();
        if (worldId == null) {
            return 0;
        }
        World world = Universe.get().getWorld(worldId);
        if (world == null) {
            return 0;
        }
        Vector3d playerPos = playerRef.getTransform().getPosition();
        if (playerPos == null) {
            return 0;
        }
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        HashSet<String> ownedNames = new HashSet<String>();
        HashSet<String> knownEntityIds = new HashSet<String>();
        for (CompanionRecord c : data.companions) {
            if (c == null) continue;
            ownedNames.add(c.getDisplayName(playerRef.getUsername()));
            if (c.entityId == null || c.entityId.isBlank()) continue;
            knownEntityIds.add(c.entityId);
        }
        Store store = world.getEntityStore().getStore();
        int removed = 0;
        for (WorldQueries.NearbyEntity ne : WorldQueries.getNearbyLivingEntities(world, playerPos, radius)) {
            try {
                boolean removeAsGeneric;
                String eid;
                NPCEntity npc = (NPCEntity)store.getComponent(ne.ref, NPCEntity.getComponentType());
                if (npc == null) continue;
                boolean managedRole = COMPANION_ROLE.equals(npc.getRoleName());
                boolean playerLikeRole = this.isPlayerLikeRole(npc.getRoleName());
                UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ne.ref, UUIDComponent.getComponentType());
                String string = eid = uuidComp != null && uuidComp.getUuid() != null ? uuidComp.getUuid().toString() : null;
                if (eid != null && knownEntityIds.contains(eid)) continue;
                Nameplate np = (Nameplate)store.getComponent(ne.ref, Nameplate.getComponentType());
                String text = np != null ? np.getText() : null;
                boolean ownerNamed = text != null && ownedNames.contains(text);
                String lowerText = text != null ? text.toLowerCase(Locale.ROOT) : "";
                boolean unnamedDefault = text == null || text.isBlank() || lowerText.contains("idle.default");
                boolean genericCompanionName = lowerText.contains("companion");
                boolean likelyOrphanCompanion = (managedRole || playerLikeRole) && unnamedDefault;
                boolean bl = removeAsGeneric = (managedRole || playerLikeRole) && genericCompanionName && !ownerNamed;
                if (!ownerNamed && !likelyOrphanCompanion && !removeAsGeneric) continue;
                store.removeEntity(ne.ref, RemoveReason.REMOVE);
                ++removed;
            }
            catch (Throwable throwable) {}
        }
        return removed;
    }

    public int reconcileExistingManagedCompanions(PlayerRef playerRef, double radius) {
        if (playerRef == null) {
            return 0;
        }
        UUID worldId = playerRef.getWorldUuid();
        if (worldId == null) {
            return 0;
        }
        World world = Universe.get().getWorld(worldId);
        if (world == null) {
            return 0;
        }
        Vector3d playerPos = playerRef.getTransform().getPosition();
        if (playerPos == null) {
            return 0;
        }
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        HashMap<String, CompanionRecord> byEntityId = new HashMap<String, CompanionRecord>();
        HashMap<String, List> byDisplayName = new HashMap<String, List>();
        for (CompanionRecord c : data.companions) {
            String expected;
            if (c == null || c.fallen) continue;
            if (c.entityId != null && !c.entityId.isBlank()) {
                byEntityId.put(c.entityId, c);
            }
            if ((expected = c.getDisplayName(playerRef.getUsername())) == null || expected.isBlank()) continue;
            byDisplayName.computeIfAbsent(expected, k -> new ArrayList()).add(c);
        }
        Store store = world.getEntityStore().getStore();
        int claimed = 0;
        ArrayList<Ref<EntityStore>> orphanCandidates = new ArrayList<Ref<EntityStore>>();
        for (WorldQueries.NearbyEntity ne : WorldQueries.getNearbyLivingEntities(world, playerPos, radius)) {
            try {
                String text;
                UUIDComponent uuidComp;
                NPCEntity nPCEntity = (NPCEntity)store.getComponent(ne.ref, NPCEntity.getComponentType());
                if (nPCEntity == null || (uuidComp = (UUIDComponent)store.getComponent(ne.ref, UUIDComponent.getComponentType())) == null || uuidComp.getUuid() == null) continue;
                String eid = uuidComp.getUuid().toString();
                CompanionRecord byId = (CompanionRecord)byEntityId.get(eid);
                if (byId != null) {
                    byId.entityId = eid;
                    byId.active = true;
                    this.restoreSavedInventoryToNpc(nPCEntity, ne.ref, (Store<EntityStore>)store, byId, "reconcile-entity-id");
                    this.applyRecordedEquipmentToNpc(nPCEntity, byId);
                    this.primeRuntimeAfterReconnectClaim(byId);
                    this.persistCompanionAppearanceToken(byId, nPCEntity, ne.ref, (Store<EntityStore>)store, "reconcile-entity-id");
                    this.ensureCompanionName((Store<EntityStore>)store, ne.ref, playerRef, byId);
                    ++claimed;
                    continue;
                }
                Nameplate np = (Nameplate)store.getComponent(ne.ref, Nameplate.getComponentType());
                String string = text = np != null ? np.getText() : null;
                if (text == null || text.isBlank() || text.toLowerCase(Locale.ROOT).contains("idle.default")) {
                    if (!COMPANION_ROLE.equalsIgnoreCase(nPCEntity.getRoleName()) || !this.isPlayerLikeRole(nPCEntity.getRoleName())) continue;
                    orphanCandidates.add(ne.ref);
                    continue;
                }
                List candidates = (List)byDisplayName.get(text);
                if (candidates == null || candidates.size() != 1 || !COMPANION_ROLE.equalsIgnoreCase(nPCEntity.getRoleName())) continue;
                CompanionRecord record = (CompanionRecord)candidates.get(0);
                if (record.entityId != null && !record.entityId.isBlank() && !record.entityId.equals(eid)) {
                    store.removeEntity(ne.ref, RemoveReason.REMOVE);
                    continue;
                }
                record.entityId = eid;
                record.active = true;
                this.restoreSavedInventoryToNpc(nPCEntity, ne.ref, (Store<EntityStore>)store, record, "reconcile-name");
                this.applyRecordedEquipmentToNpc(nPCEntity, record);
                this.primeRuntimeAfterReconnectClaim(record);
                this.persistCompanionAppearanceToken(record, nPCEntity, ne.ref, (Store<EntityStore>)store, "reconcile-name");
                this.ensureCompanionName((Store<EntityStore>)store, ne.ref, playerRef, record);
                ++claimed;
            }
            catch (Throwable throwable) {}
        }
        if (!orphanCandidates.isEmpty()) {
            ArrayList<CompanionRecord> unbound = new ArrayList<CompanionRecord>();
            for (CompanionRecord companionRecord : data.companions) {
                if (companionRecord == null || companionRecord.fallen || companionRecord.active && companionRecord.entityId != null && !companionRecord.entityId.isBlank()) continue;
                unbound.add(companionRecord);
            }
            for (Ref ref : orphanCandidates) {
                UUIDComponent uuidComp;
                NPCEntity npc;
                if (unbound.isEmpty()) break;
                if (ref == null || !ref.isValid() || (npc = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType())) == null || !COMPANION_ROLE.equalsIgnoreCase(npc.getRoleName()) || (uuidComp = (UUIDComponent)store.getComponent(ref, UUIDComponent.getComponentType())) == null || uuidComp.getUuid() == null) continue;
                String eid = uuidComp.getUuid().toString();
                String token = this.captureBestAppearanceToken(npc, (Ref<EntityStore>)ref, (Store<EntityStore>)store);
                if (token == null || token.isBlank()) continue;
                CompanionRecord matched = null;
                for (CompanionRecord c : unbound) {
                    if (c.appearanceModelId == null || !c.appearanceModelId.equals(token)) continue;
                    matched = c;
                    break;
                }
                if (matched == null && unbound.size() == 1 && orphanCandidates.size() == 1) {
                    matched = (CompanionRecord)unbound.get(0);
                }
                if (matched == null) continue;
                matched.entityId = eid;
                matched.active = true;
                this.restoreSavedInventoryToNpc(npc, (Ref<EntityStore>)ref, (Store<EntityStore>)store, matched, "reconcile-orphan-appearance");
                this.applyRecordedEquipmentToNpc(npc, matched);
                this.primeRuntimeAfterReconnectClaim(matched);
                this.persistCompanionAppearanceToken(matched, npc, (Ref<EntityStore>)ref, (Store<EntityStore>)store, "reconcile-orphan-appearance");
                this.ensureCompanionName((Store<EntityStore>)store, (Ref<EntityStore>)ref, playerRef, matched);
                unbound.remove(matched);
                ++claimed;
            }
        }
        return claimed;
    }

    private void primeRuntimeAfterReconnectClaim(CompanionRecord companion) {
        if (companion == null) {
            return;
        }
        CompanionRuntimeState runtime = this.companionManager.getRuntime(companion.uniqueId);
        if (runtime == null) {
            return;
        }
        runtime.state = switch (companion.followMode) {
            case FollowMode.STAY -> CompanionState.IDLE_STAY;
            case FollowMode.PATROL -> CompanionState.IDLE_PATROL;
            case FollowMode.FREE -> CompanionState.IDLE_FREE;
            default -> CompanionState.IDLE_FOLLOW;
        };
        runtime.followBootstrapTicks = companion.followMode == FollowMode.FOLLOW ? 4 : 0;
        runtime.stuckTicks = 0;
        runtime.stayAnchor = null;
        this.resetPatrolAndFreeRuntime(runtime);
        this.restorePersistentWorkState(companion, runtime, true);
        if (runtime.commandActive && companion.mode == CompanionMode.FARMER) {
            runtime.lastFarmScanTick = Math.max(0L, runtime.currentTick - 200L);
            runtime.farmStatusText = "Resuming farm";
        }
    }

    private void restorePersistentWorkState(CompanionRecord companion, CompanionRuntimeState runtime, boolean resumeWorkOnSummon) {
        boolean resumeFarm;
        if (companion == null || runtime == null) {
            return;
        }
        runtime.commandActive = resumeFarm = resumeWorkOnSummon && companion.mode == CompanionMode.FARMER && companion.farmAutoResume;
        boolean bl = runtime.farmDepositPending = resumeFarm && companion.farmDepositPending;
        if (resumeFarm) {
            runtime.lastFarmScanTick = Math.max(0L, runtime.currentTick - 200L);
            runtime.farmStatusText = companion.farmStatusText != null && !companion.farmStatusText.isBlank() ? companion.farmStatusText : "Resuming farm";
        } else {
            runtime.farmTargetPos = null;
            runtime.farmTaskType = null;
            runtime.farmPlantSeedId = null;
        }
        runtime.miningActive = false;
        runtime.mineForSpecific = false;
    }

    public void applyStance(PlayerRef playerRef, CompanionRecord companion, FollowMode stance) {
        if (companion == null || stance == null) {
            return;
        }
        companion.followMode = stance;
        CompanionRuntimeState runtime = this.companionManager.getRuntime(companion.uniqueId);
        runtime.commandActive = false;
        runtime.miningActive = false;
        runtime.mineForSpecific = false;
        runtime.farmTargetPos = null;
        runtime.farmTaskType = null;
        runtime.farmPlantSeedId = null;
        runtime.farmStatusText = null;
        runtime.mineTargetPos = null;
        runtime.mineTargetBlockId = null;
        runtime.mineStatusText = null;
        runtime.mineMoveToAnchorPending = false;
        runtime.mineAnchorPos = null;
        runtime.state = switch (stance) {
            case FollowMode.STAY -> CompanionState.IDLE_STAY;
            case FollowMode.PATROL -> CompanionState.IDLE_PATROL;
            case FollowMode.FREE -> CompanionState.IDLE_FREE;
            default -> CompanionState.IDLE_FOLLOW;
        };
        runtime.followBootstrapTicks = stance == FollowMode.FOLLOW ? 4 : 0;
        runtime.stuckTicks = 0;
        this.resetPatrolAndFreeRuntime(runtime);
        if (stance != FollowMode.STAY) {
            runtime.stayAnchor = null;
        }
        if (playerRef == null || !companion.active || companion.entityId == null || companion.entityId.isBlank()) {
            return;
        }
        UUID worldId = playerRef.getWorldUuid();
        if (worldId == null) {
            return;
        }
        World world = Universe.get().getWorld(worldId);
        if (world == null) {
            return;
        }
        UUID eid = this.parseUuid(companion.entityId);
        if (eid == null) {
            return;
        }
        Ref ref = world.getEntityRef(eid);
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store store = world.getEntityStore().getStore();
        NPCEntity npcEntity = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
        if (npcEntity == null) {
            return;
        }
        try {
            this.ensureManagedRoleForCompanion(npcEntity, companion, (Ref<EntityStore>)ref, (Store<EntityStore>)store);
            switch (stance) {
                case STAY: {
                    this.setHoldTarget(npcEntity, (Ref<EntityStore>)ref);
                    break;
                }
                case FOLLOW: {
                    this.ensureNonIdleState(npcEntity, (Ref<EntityStore>)ref, (Store<EntityStore>)store, runtime);
                    this.setFollowTarget(npcEntity, playerRef, (Ref<EntityStore>)ref, (Store<EntityStore>)store);
                    break;
                }
                case PATROL: {
                    this.clearManualMovementTargets(npcEntity);
                    TransformComponent transform = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
                    Vector3d currentPos = transform != null ? transform.getPosition() : (runtime.lastPosition != null ? this.toVector3d(runtime.lastPosition) : null);
                    this.initializePatrolRoute(runtime, transform, currentPos);
                    this.driveAutonomousMovement(npcEntity, runtime, runtime.patrolForwardTarget);
                    break;
                }
                case FREE: {
                    this.clearManualMovementTargets(npcEntity);
                    TransformComponent transform = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
                    Vector3d currentPos = transform != null ? transform.getPosition() : (runtime.lastPosition != null ? this.toVector3d(runtime.lastPosition) : null);
                    this.seedFreeRoamDirection(runtime, transform);
                    this.initializeFreeRoam(runtime, currentPos);
                    this.pickNextFreeRoamTarget(runtime, currentPos);
                    this.driveAutonomousMovement(npcEntity, runtime, runtime.freeRoamTarget);
                    break;
                }
            }
            try {
                npcEntity.getRole().getWorldSupport().requestNewPath();
            }
            catch (Throwable throwable) {}
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public boolean hasLiveEntity(PlayerRef playerRef, CompanionRecord companion) {
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        return active != null && active.ref != null && active.ref.isValid();
    }

    public CompanionRecord findCompanionByEntity(PlayerRef playerRef, Ref<EntityStore> entityRef, World world) {
        if (playerRef == null || entityRef == null || world == null) {
            return null;
        }
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        Store store = world.getEntityStore().getStore();
        try {
            UUIDComponent uuidComp = (UUIDComponent)store.getComponent(entityRef, UUIDComponent.getComponentType());
            if (uuidComp != null) {
                String entityIdStr = uuidComp.getUuid().toString();
                return data.findCompanionByEntityId(entityIdStr);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    public boolean summon(PlayerRef playerRef) {
        List<CompanionRecord> dormant;
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        CompanionRecord companion = data.resolveSelectedOrOnly();
        if (companion == null && !(dormant = data.getDormantCompanions()).isEmpty()) {
            companion = dormant.get(0);
        }
        if (companion == null) {
            return false;
        }
        return this.summonCompanion(playerRef, companion);
    }

    public void dismiss(PlayerRef playerRef) {
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        CompanionRecord companion = data.resolveSelectedOrOnly();
        if (companion != null) {
            this.dismissCompanion(playerRef, companion);
        }
    }

    public boolean hasActiveCompanion(PlayerRef playerRef) {
        if (playerRef == null) {
            return false;
        }
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        return data.hasAnyActiveCompanion();
    }

    public boolean isCompanionOf(PlayerRef playerRef, Ref<EntityStore> entityRef, World world) {
        return this.findCompanionByEntity(playerRef, entityRef, world) != null;
    }

    public void restoreInventory(PlayerRef playerRef, CompanionRecord companion) {
        if (playerRef == null || companion == null) {
            return;
        }
        if (companion.savedInventory == null || companion.savedInventory.isEmpty()) {
            return;
        }
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        if (active == null) {
            return;
        }
        try {
            NPCEntity npcEntity = (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType());
            if (npcEntity == null) {
                return;
            }
            this.restoreSavedInventoryToNpc(npcEntity, active.ref, active.store, companion, "manual-restore");
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to restore companion inventory.");
        }
    }

    public String getDebugInfo(PlayerRef playerRef, CompanionRecord companion) {
        CompanionRuntimeState runtime = this.companionManager.getRuntime(companion.uniqueId);
        StringBuilder sb = new StringBuilder();
        sb.append("Role: ").append((Object)companion.mode).append(" | Stance: ").append((Object)companion.followMode);
        sb.append(" | State: ").append((Object)runtime.state);
        sb.append(" | StuckTicks: ").append(runtime.stuckTicks);
        sb.append(" | CombatTarget: ").append(runtime.combatTargetId != null ? "YES" : "none");
        sb.append(" | LootTarget: ").append(runtime.lootTargetId != null ? "YES" : "none");
        sb.append(" | FarmTarget: ").append(runtime.farmTargetPos != null ? "YES" : "none");
        sb.append(" | DepositReq: ").append(runtime.depositRequested);
        if (companion.entityId != null) {
            UUID cid;
            World world = Universe.get().getWorld(playerRef.getWorldUuid());
            if (world != null && (cid = this.parseUuid(companion.entityId)) != null) {
                Ref ref = world.getEntityRef(cid);
                if (ref != null && ref.isValid()) {
                    Store store = world.getEntityStore().getStore();
                    TransformComponent transform = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
                    if (transform != null) {
                        Vector3d cPos = transform.getPosition();
                        Vector3d oPos = playerRef.getTransform().getPosition();
                        if (cPos != null && oPos != null) {
                            sb.append(" | Dist: ").append(String.format("%.1f", cPos.distanceTo(oPos)));
                        }
                    }
                    try {
                        NPCEntity npc = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
                        if (npc != null) {
                            String aiState = npc.getRole().getStateSupport().getStateName();
                            sb.append(" | NpcAI: ").append(aiState != null ? aiState : "null");
                        }
                    }
                    catch (Throwable t) {
                        sb.append(" | NpcAI: ERROR");
                    }
                } else {
                    sb.append(" | Entity: INVALID");
                }
            }
        } else {
            sb.append(" | Entity: NONE");
        }
        sb.append(" | CombatLv: ").append(companion.combatLevel).append(" (").append(companion.combatKills).append(" kills)");
        sb.append(" | FarmLv: ").append(companion.farmLevel).append(" (").append(companion.farmHarvests).append(" harvests)");
        sb.append(" | MineLv: ").append(companion.mineLevel).append(" (").append(companion.mineBlocks).append(" blocks)");
        sb.append(" | Mining: ").append(runtime.miningActive ? "YES" : "no");
        sb.append(" | MaxDefend: ").append(runtime.maxDefendMode ? "YES" : "no");
        return sb.toString();
    }

    public List<String> getAvailableRoleTemplates(boolean includeHidden) {
        NPCPlugin npcPlugin = NPCPlugin.get();
        if (npcPlugin == null) {
            return List.of();
        }
        List roles = npcPlugin.getRoleTemplateNames(includeHidden);
        return roles == null ? List.of() : new ArrayList(roles);
    }

    public List<String> getAvailableModelIds(String filter, boolean humanOnly, int limit) {
        ArrayList<String> ids = new ArrayList<String>();
        Map models = ModelAsset.getAssetMap().getAssetMap();
        if (models == null || models.isEmpty()) {
            return ids;
        }
        String normalizedFilter = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        for (String modelId : models.keySet()) {
            if (modelId == null || modelId.isEmpty()) continue;
            String lowered = modelId.toLowerCase(Locale.ROOT);
            if (!normalizedFilter.isEmpty() && !lowered.contains(normalizedFilter) || humanOnly && !this.isHumanModelId(lowered)) continue;
            ids.add(modelId);
        }
        ids.sort(Comparator.naturalOrder());
        if (ids.size() > limit) {
            return new ArrayList<String>(ids.subList(0, limit));
        }
        return ids;
    }

    public int getDefaultModelListLimit() {
        return 80;
    }

    public boolean setPreferredAppearanceModel(PlayerRef playerRef, CompanionRecord companion, String modelId) {
        if (playerRef == null || companion == null || modelId == null || modelId.isBlank()) {
            return false;
        }
        ModelAsset modelAsset = (ModelAsset)ModelAsset.getAssetMap().getAsset((Object)modelId);
        if (modelAsset == null) {
            return false;
        }
        this.setAppearanceModelId(companion, modelId, "setPreferredAppearanceModel");
        this.applyAppearanceToActiveCompanion(playerRef, companion, modelId);
        return true;
    }

    public void clearPreferredAppearanceModel(PlayerRef playerRef, CompanionRecord companion) {
        if (playerRef == null || companion == null) {
            return;
        }
        this.setAppearanceModelId(companion, null, "clearPreferredAppearanceModel");
    }

    public boolean applyAppearanceToActiveCompanion(PlayerRef playerRef, CompanionRecord companion, String modelId) {
        if (playerRef == null || companion == null || modelId == null || modelId.isBlank()) {
            return false;
        }
        ModelAsset modelAsset = (ModelAsset)ModelAsset.getAssetMap().getAsset((Object)modelId);
        if (modelAsset == null) {
            return false;
        }
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        if (active == null) {
            return false;
        }
        NPCEntity npcEntity = (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType());
        if (npcEntity == null) {
            return false;
        }
        npcEntity.setAppearance(active.ref, modelAsset, active.store);
        this.applyPersistentSkinOverlay(companion, active.ref, active.store, "applyAppearanceToActiveCompanion");
        return true;
    }

    public static boolean isAppearancePersistDebugEnabled() {
        return false;
    }

    public String rerollAppearance(PlayerRef playerRef, CompanionRecord companion) {
        NPCEntity npcEntity;
        String randomizedAppearance;
        if (playerRef == null || companion == null) {
            return null;
        }
        String preferred = companion.appearanceModelId;
        if (preferred != null && !preferred.isBlank() && this.applyAppearanceToActiveCompanion(playerRef, companion, preferred)) {
            return preferred;
        }
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        if (active != null && this.isPersistableAppearanceId(randomizedAppearance = this.applyRandomCosmeticSkin(active.ref, active.store, npcEntity = (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType())))) {
            this.setAppearanceModelId(companion, randomizedAppearance, "appearance-randomized");
            return randomizedAppearance;
        }
        String randomId = this.chooseRandomHumanModelId();
        if (randomId != null && this.applyAppearanceToActiveCompanion(playerRef, companion, randomId)) {
            this.setAppearanceModelId(companion, randomId, "rerollAppearance-randomModel");
            return randomId;
        }
        return null;
    }

    public void renameCompanion(PlayerRef playerRef, CompanionRecord companion, String newName) {
        if (playerRef == null || companion == null || newName == null || newName.isBlank()) {
            return;
        }
        companion.name = newName.trim();
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        if (active != null) {
            this.ensureCompanionName(active.store, active.ref, playerRef, companion);
        }
    }

    public LinkedHashMap<String, Integer> getLiveInventory(PlayerRef playerRef, CompanionRecord companion) {
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        if (active == null) {
            return null;
        }
        NPCEntity npcEntity = (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType());
        if (npcEntity == null) {
            return null;
        }
        this.sanitizeSavedInventoryData(companion);
        this.sanitizeLiveInventory(npcEntity);
        LinkedHashMap<String, Integer> result = new LinkedHashMap<String, Integer>();
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return result;
            }
            ItemContainer armorContainer = inventory.getArmor();
            if (armorContainer != null) {
                ArrayList invalidArmorSlots = new ArrayList();
                armorContainer.forEach((slot, itemStack) -> {
                    if (itemStack != null && !itemStack.isEmpty()) {
                        String id = itemStack.getItemId();
                        if (!this.isSafeInventoryItemId(id)) {
                            invalidArmorSlots.add(slot);
                            return;
                        }
                        result.merge(id, itemStack.getQuantity(), Integer::sum);
                    }
                });
                Iterator iterator = invalidArmorSlots.iterator();
                while (iterator.hasNext()) {
                    short slot2 = (Short)iterator.next();
                    try {
                        armorContainer.setItemStackForSlot(slot2, ItemStack.EMPTY);
                    }
                    catch (Throwable throwable) {}
                }
            }
            for (ItemContainer container : InvUtil.getCompanionContainersForRead(npcEntity)) {
                this.readInventoryContainer(result, container);
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to read live companion inventory.");
        }
        return result;
    }

    public List<String[]> getLiveInventoryStacks(PlayerRef playerRef, CompanionRecord companion) {
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        if (active == null) {
            return null;
        }
        NPCEntity npcEntity = (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType());
        if (npcEntity == null) {
            return null;
        }
        this.sanitizeSavedInventoryData(companion);
        this.sanitizeLiveInventory(npcEntity);
        ArrayList<String[]> result = new ArrayList<String[]>();
        try {
            for (ItemContainer container : InvUtil.getCompanionContainersForRead(npcEntity)) {
                ArrayList invalidSlots = new ArrayList();
                container.forEach((slot, itemStack) -> {
                    if (itemStack == null || itemStack.isEmpty()) {
                        return;
                    }
                    String id = itemStack.getItemId();
                    if (!this.isSafeInventoryItemId(id)) {
                        invalidSlots.add(slot);
                        return;
                    }
                    int qty = Math.max(1, itemStack.getQuantity());
                    result.add(new String[]{id, EquipmentSlot.getItemDisplayName(id), Integer.toString(qty)});
                });
                Iterator iterator = invalidSlots.iterator();
                while (iterator.hasNext()) {
                    short slot2 = (Short)iterator.next();
                    try {
                        container.setItemStackForSlot(slot2, ItemStack.EMPTY);
                    }
                    catch (Throwable throwable) {}
                }
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to read live companion inventory stacks.");
        }
        return result;
    }

    private void readInventoryContainer(LinkedHashMap<String, Integer> result, ItemContainer container) {
        if (container == null) {
            return;
        }
        ArrayList invalidSlots = new ArrayList();
        container.forEach((slot, itemStack) -> {
            if (itemStack != null && !itemStack.isEmpty()) {
                String id = itemStack.getItemId();
                if (!this.isSafeInventoryItemId(id)) {
                    invalidSlots.add(slot);
                    return;
                }
                result.merge(id, itemStack.getQuantity(), Integer::sum);
            }
        });
        Iterator iterator = invalidSlots.iterator();
        while (iterator.hasNext()) {
            short slot2 = (Short)iterator.next();
            try {
                container.setItemStackForSlot(slot2, ItemStack.EMPTY);
            }
            catch (Throwable throwable) {}
        }
    }

    private boolean isSafeInventoryItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        String value = itemId.trim();
        if (value.isBlank() || value.equalsIgnoreCase("null") || value.equalsIgnoreCase("none") || value.contains("{") || value.contains("}") || value.contains("=") || value.contains(":")) {
            return false;
        }
        try {
            ItemStack stack = new ItemStack(value, 1);
            return stack != null && !stack.isEmpty() && stack.getItem() != null;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private void sanitizeSavedInventoryData(CompanionRecord companion) {
        if (companion == null) {
            return;
        }
        try {
            if (companion.savedInventory != null && !companion.savedInventory.isEmpty()) {
                companion.savedInventory.entrySet().removeIf(entry -> entry == null || !this.isSafeInventoryItemId((String)entry.getKey()) || entry.getValue() == null || (Integer)entry.getValue() <= 0);
            }
            if (companion.savedInventoryStacks != null && !companion.savedInventoryStacks.isEmpty()) {
                companion.savedInventoryStacks.removeIf(stack -> stack == null || ((String[])stack).length < 1 || !this.isSafeInventoryItemId(stack[0]));
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void sanitizeLiveInventory(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return;
        }
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return;
            }
            try {
                ItemContainer armor = inventory.getArmor();
                this.sanitizeItemContainer(armor);
            }
            catch (Throwable armor) {
                // empty catch block
            }
            try {
                ItemContainer utility = inventory.getUtility();
                this.sanitizeItemContainer(utility);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            for (ItemContainer container : InvUtil.getCompanionContainersForRead(npcEntity)) {
                this.sanitizeItemContainer(container);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void sanitizeItemContainer(ItemContainer container) {
        if (container == null) {
            return;
        }
        ArrayList invalidSlots = new ArrayList();
        try {
            container.forEach((slot, itemStack) -> {
                if (itemStack == null || itemStack.isEmpty()) {
                    return;
                }
                String id = itemStack.getItemId();
                if (!this.isSafeInventoryItemId(id)) {
                    invalidSlots.add(slot);
                }
            });
            Iterator iterator = invalidSlots.iterator();
            while (iterator.hasNext()) {
                short slot2 = (Short)iterator.next();
                try {
                    container.setItemStackForSlot(slot2, ItemStack.EMPTY);
                }
                catch (Throwable throwable) {}
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public boolean giveHandItem(PlayerRef playerRef, CompanionRecord companion) {
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        if (active == null) {
            return false;
        }
        NPCEntity npcEntity = (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType());
        if (npcEntity == null) {
            return false;
        }
        return InvUtil.transferHandToCompanion(playerRef, npcEntity, active.ref, active.store, this.logger);
    }

    public int giveSpecificItem(PlayerRef playerRef, CompanionRecord companion, String itemId) {
        if (playerRef == null || companion == null || itemId == null || itemId.isBlank()) {
            return 0;
        }
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            return 0;
        }
        Store store = world.getEntityStore().getStore();
        if (store == null) {
            return 0;
        }
        Player playerEntity = (Player)store.getComponent(playerRef.getReference(), Player.getComponentType());
        if (playerEntity == null) {
            return 0;
        }
        Inventory playerInv = playerEntity.getInventory();
        if (playerInv == null) {
            return 0;
        }
        CombinedItemContainer playerContainer = playerInv.getCombinedHotbarFirst();
        if (playerContainer == null) {
            return 0;
        }
        short[] fromSlot = new short[]{-1};
        ItemStack[] fromStack = new ItemStack[]{null};
        playerContainer.forEach((slot, itemStack) -> {
            if (fromStack[0] != null) {
                return;
            }
            if (itemStack != null && !itemStack.isEmpty() && itemId.equalsIgnoreCase(itemStack.getItemId())) {
                fromSlot[0] = slot;
                fromStack[0] = itemStack;
            }
        });
        if (fromSlot[0] < 0 || fromStack[0] == null) {
            return 0;
        }
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        if (active != null) {
            NPCEntity npcEntity = (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType());
            if (npcEntity == null) {
                return 0;
            }
            ItemStack rem = InvUtil.addItemStackToCompanionInventory(npcEntity, fromStack[0]);
            int remaining = rem == null || rem.isEmpty() ? 0 : rem.getQuantity();
            int moved = fromStack[0].getQuantity() - remaining;
            if (moved <= 0) {
                return 0;
            }
            int playerRemain = fromStack[0].getQuantity() - moved;
            if (playerRemain <= 0) {
                playerContainer.setItemStackForSlot(fromSlot[0], ItemStack.EMPTY);
            } else {
                playerContainer.setItemStackForSlot(fromSlot[0], fromStack[0].withQuantity(playerRemain));
            }
            return moved;
        }
        int qty = Math.max(1, fromStack[0].getQuantity());
        companion.savedInventory.merge(fromStack[0].getItemId(), qty, Integer::sum);
        playerContainer.setItemStackForSlot(fromSlot[0], ItemStack.EMPTY);
        return qty;
    }

    public boolean takeHandItem(PlayerRef playerRef, CompanionRecord companion) {
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        if (active == null) {
            return false;
        }
        NPCEntity npcEntity = (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType());
        if (npcEntity == null) {
            return false;
        }
        return InvUtil.transferFromCompanionToPlayer(playerRef, npcEntity, active.ref, active.store, this.logger);
    }

    public String tryConsumeReviveCost(PlayerRef playerRef) {
        if (playerRef == null) {
            return "Player not found.";
        }
        if (!ReviveConfig.hasReviveCost()) {
            return null;
        }
        Inventory playerInv = this.getPlayerInventory(playerRef);
        if (playerInv == null) {
            return "Player inventory not found.";
        }
        int available = this.countPlayerItem(playerInv, ReviveConfig.REVIVE_COST_ITEM);
        if (available < ReviveConfig.REVIVE_COST_AMOUNT) {
            return "Need " + ReviveConfig.getReviveCostText() + " to revive.";
        }
        int remainingToRemove = ReviveConfig.REVIVE_COST_AMOUNT;
        remainingToRemove = this.removePlayerItems(playerInv.getHotbar(), ReviveConfig.REVIVE_COST_ITEM, remainingToRemove);
        try {
            remainingToRemove = this.removePlayerItems(playerInv.getStorage(), ReviveConfig.REVIVE_COST_ITEM, remainingToRemove);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            remainingToRemove = this.removePlayerItems(playerInv.getBackpack(), ReviveConfig.REVIVE_COST_ITEM, remainingToRemove);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return remainingToRemove <= 0 ? null : "Failed to consume revive cost.";
    }

    public String tryReviveCompanion(PlayerRef playerRef, CompanionRecord companion, boolean consumeReviveCost) {
        String reviveCostError;
        if (playerRef == null) {
            return "Player not found.";
        }
        if (companion == null) {
            return "Companion not found.";
        }
        if (!companion.fallen) {
            return companion.getDisplayName() + " is not fallen.";
        }
        if (consumeReviveCost) {
            int available;
            Inventory playerInv = this.getPlayerInventory(playerRef);
            if (playerInv == null) {
                return "Player inventory not found.";
            }
            int n = available = ReviveConfig.hasReviveCost() ? this.countPlayerItem(playerInv, ReviveConfig.REVIVE_COST_ITEM) : Integer.MAX_VALUE;
            if (available < ReviveConfig.REVIVE_COST_AMOUNT) {
                return "Need " + ReviveConfig.getReviveCostText() + " to revive.";
            }
        }
        boolean previousFallen = companion.fallen;
        String previousDeathCause = companion.deathCause;
        long previousDeathTime = companion.deathTime;
        companion.fallen = false;
        companion.deathCause = null;
        companion.deathTime = 0L;
        boolean spawned = this.summonCompanion(playerRef, companion);
        if (!spawned) {
            companion.fallen = previousFallen;
            companion.deathCause = previousDeathCause;
            companion.deathTime = previousDeathTime;
            return "Failed to revive. Try again.";
        }
        if (consumeReviveCost && (reviveCostError = this.tryConsumeReviveCost(playerRef)) != null) {
            this.dismissCompanion(playerRef, companion);
            companion.fallen = previousFallen;
            companion.deathCause = previousDeathCause;
            companion.deathTime = previousDeathTime;
            companion.active = false;
            companion.entityId = null;
            return reviveCostError;
        }
        this.restoreInventory(playerRef, companion);
        this.saveCompanionData("tryReviveCompanion", companion);
        return null;
    }

    public int takeSpecificItem(PlayerRef playerRef, CompanionRecord companion, String itemId) {
        if (playerRef == null || companion == null || itemId == null || itemId.isBlank()) {
            return 0;
        }
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            return 0;
        }
        Store store = world.getEntityStore().getStore();
        if (store == null) {
            return 0;
        }
        Player playerEntity = (Player)store.getComponent(playerRef.getReference(), Player.getComponentType());
        if (playerEntity == null) {
            return 0;
        }
        Inventory playerInv = playerEntity.getInventory();
        if (playerInv == null || playerInv.getCombinedHotbarFirst() == null) {
            return 0;
        }
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        if (active != null) {
            NPCEntity npcEntity = (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType());
            if (npcEntity == null) {
                return 0;
            }
            InvUtil.ContainerSlotRef found = InvUtil.findMatchingCompanionSlot(npcEntity, itemId);
            if (found == null || found.stack == null) {
                return 0;
            }
            ItemStackTransaction tx = playerInv.getCombinedHotbarFirst().addItemStack(found.stack);
            if (tx == null || !tx.succeeded()) {
                return 0;
            }
            ItemStack remainder = tx.getRemainder();
            int remaining = remainder == null || remainder.isEmpty() ? 0 : remainder.getQuantity();
            int moved = found.stack.getQuantity() - remaining;
            if (moved <= 0) {
                return 0;
            }
            if (remaining <= 0) {
                found.container.setItemStackForSlot(found.slot, ItemStack.EMPTY);
            } else {
                found.container.setItemStackForSlot(found.slot, found.stack.withQuantity(remaining));
            }
            return moved;
        }
        int available = companion.savedInventory.getOrDefault(itemId, 0);
        if (available <= 0) {
            return 0;
        }
        ItemStack stack = new ItemStack(itemId, available);
        ItemStackTransaction tx = playerInv.getCombinedHotbarFirst().addItemStack(stack);
        if (tx == null || !tx.succeeded()) {
            return 0;
        }
        ItemStack remainder = tx.getRemainder();
        int remaining = remainder == null || remainder.isEmpty() ? 0 : remainder.getQuantity();
        int moved = available - remaining;
        if (moved <= 0) {
            return 0;
        }
        int newQty = available - moved;
        if (newQty <= 0) {
            companion.savedInventory.remove(itemId);
        } else {
            companion.savedInventory.put(itemId, newQty);
        }
        return moved;
    }

    private int countPlayerItem(Inventory playerInv, String itemId) {
        if (playerInv == null || itemId == null || itemId.isBlank()) {
            return 0;
        }
        int total = this.countItemInContainer(playerInv.getHotbar(), itemId);
        try {
            total += this.countItemInContainer(playerInv.getStorage(), itemId);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            total += this.countItemInContainer(playerInv.getBackpack(), itemId);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return total;
    }

    private int countItemInContainer(ItemContainer container, String itemId) {
        if (container == null || itemId == null || itemId.isBlank()) {
            return 0;
        }
        int[] total = new int[]{0};
        container.forEach((slot, itemStack) -> {
            if (itemStack == null || itemStack.isEmpty()) {
                return;
            }
            if (itemId.equalsIgnoreCase(itemStack.getItemId())) {
                total[0] = total[0] + Math.max(1, itemStack.getQuantity());
            }
        });
        return total[0];
    }

    private int removePlayerItems(ItemContainer container, String itemId, int remainingToRemove) {
        if (container == null || remainingToRemove <= 0 || itemId == null || itemId.isBlank()) {
            return remainingToRemove;
        }
        ArrayList changes = new ArrayList();
        int[] remaining = new int[]{remainingToRemove};
        container.forEach((slot, itemStack) -> {
            if (remaining[0] <= 0 || itemStack == null || itemStack.isEmpty()) {
                return;
            }
            if (!itemId.equalsIgnoreCase(itemStack.getItemId())) {
                return;
            }
            int qty = Math.max(1, itemStack.getQuantity());
            int remove = Math.min(qty, remaining[0]);
            int newQty = qty - remove;
            changes.add(new short[]{slot, (short)newQty});
            remaining[0] = remaining[0] - remove;
        });
        for (short[] change : changes) {
            short slot2 = change[0];
            short newQty = change[1];
            if (newQty <= 0) {
                container.setItemStackForSlot(slot2, ItemStack.EMPTY);
                continue;
            }
            ItemStack current = container.getItemStack(slot2);
            if (current == null || current.isEmpty()) {
                container.setItemStackForSlot(slot2, ItemStack.EMPTY);
                continue;
            }
            container.setItemStackForSlot(slot2, current.withQuantity((int)newQty));
        }
        return remaining[0];
    }

    public int giveAllItems(PlayerRef playerRef, CompanionRecord companion) {
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        if (active == null) {
            return 0;
        }
        NPCEntity npcEntity = (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType());
        if (npcEntity == null) {
            return 0;
        }
        try {
            Player playerEntity = (Player)active.store.getComponent(playerRef.getReference(), Player.getComponentType());
            if (playerEntity == null) {
                return 0;
            }
            Inventory playerInv = playerEntity.getInventory();
            if (playerInv == null) {
                return 0;
            }
            Inventory companionInv = npcEntity.getInventory();
            if (companionInv == null) {
                return 0;
            }
            int transferred = 0;
            CombinedItemContainer playerContainer = playerInv.getCombinedHotbarFirst();
            if (playerContainer == null) {
                return 0;
            }
            ArrayList items = new ArrayList();
            playerContainer.forEach((slot, itemStack) -> {
                if (itemStack != null && !itemStack.isEmpty()) {
                    items.add(Pair.of((Object)slot, (Object)itemStack));
                }
            });
            for (Pair pair : items) {
                short slot2 = (Short)pair.first();
                ItemStack item = (ItemStack)pair.second();
                ItemStack remainder = InvUtil.addItemStackToCompanionInventory(npcEntity, item);
                int remaining = remainder == null || remainder.isEmpty() ? 0 : remainder.getQuantity();
                if (remaining >= item.getQuantity()) continue;
                if (remaining <= 0) {
                    playerContainer.setItemStackForSlot(slot2, ItemStack.EMPTY);
                } else {
                    playerContainer.setItemStackForSlot(slot2, remainder);
                }
                ++transferred;
            }
            return transferred;
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed in giveAllItems.");
            return 0;
        }
    }

    public int takeAllItems(PlayerRef playerRef, CompanionRecord companion) {
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        if (active == null) {
            return 0;
        }
        NPCEntity npcEntity = (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType());
        if (npcEntity == null) {
            return 0;
        }
        try {
            Player playerEntity = (Player)active.store.getComponent(playerRef.getReference(), Player.getComponentType());
            if (playerEntity == null) {
                return 0;
            }
            Inventory playerInv = playerEntity.getInventory();
            if (playerInv == null) {
                return 0;
            }
            Inventory companionInv = npcEntity.getInventory();
            if (companionInv == null) {
                return 0;
            }
            int transferred = 0;
            List<InvUtil.ContainerSlotRef> items = this.collectCompanionInventorySlots(npcEntity, companion);
            for (InvUtil.ContainerSlotRef pair : items) {
                short slot = pair.slot;
                ItemStack item = pair.stack;
                ItemStackTransaction tx = playerInv.getCombinedHotbarFirst().addItemStack(item);
                if (tx == null || !tx.succeeded()) continue;
                ItemStack remainder = tx.getRemainder();
                if (remainder == null || remainder.isEmpty()) {
                    pair.container.setItemStackForSlot(slot, ItemStack.EMPTY);
                } else {
                    pair.container.setItemStackForSlot(slot, remainder);
                }
                ++transferred;
            }
            return transferred;
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed in takeAllItems.");
            return 0;
        }
    }

    private List<InvUtil.ContainerSlotRef> collectCompanionInventorySlots(NPCEntity npcEntity, CompanionRecord companion) {
        ArrayList<InvUtil.ContainerSlotRef> items = new ArrayList<InvUtil.ContainerSlotRef>();
        if (npcEntity == null) {
            return items;
        }
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return items;
            }
            Map<String, Integer> skipEquipped = this.buildBulkTransferSkipMap(companion);
            for (ItemContainer container : InvUtil.getCompanionContainersForRead(npcEntity)) {
                this.collectContainerSlots(items, container, skipEquipped);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return items;
    }

    private void collectContainerSlots(List<InvUtil.ContainerSlotRef> items, ItemContainer container, Map<String, Integer> skipEquipped) {
        if (container == null) {
            return;
        }
        container.forEach((slot, itemStack) -> {
            if (itemStack != null && !itemStack.isEmpty()) {
                if (this.shouldSkipBulkTransferItem((ItemStack)itemStack, skipEquipped)) {
                    return;
                }
                items.add(new InvUtil.ContainerSlotRef(container, slot, (ItemStack)itemStack));
            }
        });
    }

    private Map<String, Integer> buildBulkTransferSkipMap(CompanionRecord companion) {
        HashMap<String, Integer> skip = new HashMap<String, Integer>();
        if (companion == null) {
            return skip;
        }
        this.addSkipEquip(skip, companion.getEquipped(EquipmentSlot.HELMET));
        this.addSkipEquip(skip, companion.getEquipped(EquipmentSlot.CHESTPLATE));
        this.addSkipEquip(skip, companion.getEquipped(EquipmentSlot.LEGGINGS));
        this.addSkipEquip(skip, companion.getEquipped(EquipmentSlot.BOOTS));
        this.addSkipEquip(skip, companion.getEquipped(EquipmentSlot.WEAPON));
        this.addSkipEquip(skip, companion.getEquipped(EquipmentSlot.OFFHAND));
        return skip;
    }

    private void addSkipEquip(Map<String, Integer> skip, String itemId) {
        if (skip == null || itemId == null || itemId.isBlank()) {
            return;
        }
        skip.merge(itemId, 1, Integer::sum);
    }

    private boolean shouldSkipBulkTransferItem(ItemStack itemStack, Map<String, Integer> skipEquipped) {
        if (itemStack == null || itemStack.isEmpty() || skipEquipped == null || skipEquipped.isEmpty()) {
            return false;
        }
        String itemId = itemStack.getItemId();
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        Integer remaining = skipEquipped.get(itemId);
        if (remaining == null || remaining <= 0) {
            return false;
        }
        if (remaining == 1) {
            skipEquipped.remove(itemId);
        } else {
            skipEquipped.put(itemId, remaining - 1);
        }
        return true;
    }

    public String startDirectionalMine(PlayerRef playerRef, CompanionRecord companion, int targetX, int targetY, int targetZ) {
        if (playerRef == null || companion == null) {
            return "Unable to start mining.";
        }
        CompanionRuntimeState rt = this.companionManager.getRuntime(companion.uniqueId);
        this.suspendOwnerFollowTargetingForActiveWork(playerRef, companion);
        Vector3d anchor = this.getPlayerPosition(playerRef);
        if (anchor == null) {
            return "Unable to resolve player position.";
        }
        int startLegY = this.preferredDirectionalMineLegY(playerRef, targetX, targetY, targetZ);
        int stepX = Integer.compare(targetX, (int)Math.floor(anchor.x));
        int stepZ = Integer.compare(targetZ, (int)Math.floor(anchor.z));
        if (stepX != 0 && stepZ != 0) {
            if (Math.abs((double)targetX + 0.5 - anchor.x) >= Math.abs((double)targetZ + 0.5 - anchor.z)) {
                stepZ = 0;
            } else {
                stepX = 0;
            }
        }
        if (stepX == 0 && stepZ == 0) {
            int[] facing = this.getFacingStepFromPlayerRotation(playerRef);
            stepX = facing[0];
            stepZ = facing[1];
            if (stepX == 0 && stepZ == 0) {
                stepZ = 1;
            }
        }
        rt.miningActive = true;
        rt.mineForSpecific = false;
        rt.mineTargetBlockId = null;
        rt.mineTargetPos = null;
        rt.mineBlockedTargetUntilByKey.clear();
        int anchorX = targetX - stepX;
        int anchorZ = targetZ - stepZ;
        rt.mineAnchorPos = new Vec3((double)anchorX + 0.5, startLegY, (double)anchorZ + 0.5);
        rt.mineMoveToAnchorPending = true;
        rt.mineStepX = stepX;
        rt.mineStepY = 0;
        rt.mineStepZ = stepZ;
        rt.mineClearanceY = startLegY;
        rt.mineUnreachableTicks = 0;
        rt.mineEmptyForwardConsecutive = 0;
        rt.mineFallDetectedTick = 0L;
        rt.mineLastMinedPos = null;
        rt.mineDepositOnlyMined = false;
        rt.mineReturnToLoadedArea = false;
        rt.mineShiftLaneAfterReturn = false;
        rt.mineShiftLaneAfterReturnAttempt = 0;
        rt.mineShiftLaneAfterReturnOrigin = null;
        rt.mineBoundaryTurnPhase = 0;
        rt.mineBoundaryTurnNextSide = 1;
        rt.mineBoundaryTurnOriginalStepX = 0;
        rt.mineBoundaryTurnOriginalStepZ = 0;
        rt.mineCollectedItemIds.clear();
        rt.nextMineAttemptTick = 0L;
        rt.commandActive = true;
        rt.mineStatusText = "Moving to mine start position";
        return companion.getDisplayName() + " moving to mine start position.";
    }

    private Inventory getPlayerInventory(PlayerRef playerRef) {
        if (playerRef == null) {
            return null;
        }
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            return null;
        }
        Store store = world.getEntityStore().getStore();
        if (store == null) {
            return null;
        }
        Player playerEntity = (Player)store.getComponent(playerRef.getReference(), Player.getComponentType());
        return playerEntity != null ? playerEntity.getInventory() : null;
    }

    private int preferredDirectionalMineLegY(PlayerRef playerRef, int targetX, int targetY, int targetZ) {
        if (playerRef == null || playerRef.getWorldUuid() == null) {
            return targetY;
        }
        try {
            World world = Universe.get().getWorld(playerRef.getWorldUuid());
            if (world == null) {
                return targetY;
            }
            String targetId = WorldQueries.getBlockTypeAt(world, targetX, targetY, targetZ);
            String belowId = WorldQueries.getBlockTypeAt(world, targetX, targetY - 1, targetZ);
            if (this.isMineableDirectionalStartBlock(targetId) && this.isMineableDirectionalStartBlock(belowId)) {
                return targetY - 1;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return targetY;
    }

    private boolean isMineableDirectionalStartBlock(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return false;
        }
        String v = blockId.toLowerCase(Locale.ROOT).trim();
        if (v.startsWith("*")) {
            v = v.substring(1);
        }
        if (v.equals("air") || v.equals("empty")) {
            return false;
        }
        if (v.endsWith(":air") || v.endsWith(":empty")) {
            return false;
        }
        if (v.contains("id=empty")) {
            return false;
        }
        if (v.contains("group='air'")) {
            return false;
        }
        if (v.contains("drawtype=empty")) {
            return false;
        }
        if (v.contains("material=empty")) {
            return false;
        }
        return !v.contains(" blockid:0") && !v.startsWith("blockid:0");
    }

    public String startMineForBlock(PlayerRef playerRef, CompanionRecord companion, String blockType, int targetX, int targetY, int targetZ) {
        if (playerRef == null || companion == null) {
            return "Unable to start Mine For.";
        }
        if (blockType == null || blockType.isBlank()) {
            return "Could not identify block type.";
        }
        String canonicalTarget = this.canonicalMineTargetId(blockType);
        if (canonicalTarget == null || canonicalTarget.isBlank() || this.isAirLikeMineTarget(canonicalTarget)) {
            return "That block is not mineable (air/empty).";
        }
        CompanionRuntimeState rt = this.companionManager.getRuntime(companion.uniqueId);
        this.suspendOwnerFollowTargetingForActiveWork(playerRef, companion);
        rt.miningActive = true;
        rt.mineForSpecific = true;
        rt.mineSearchRadius = 24;
        rt.mineTargetBlockId = canonicalTarget;
        rt.mineTargetPos = new Vec3((double)targetX + 0.5, (double)targetY + 0.5, (double)targetZ + 0.5);
        rt.mineBlockedTargetUntilByKey.clear();
        rt.mineUnreachableTicks = 0;
        rt.mineEmptyForwardConsecutive = 0;
        rt.mineFallDetectedTick = 0L;
        rt.mineLastMinedPos = null;
        rt.mineDepositOnlyMined = false;
        rt.mineReturnToLoadedArea = false;
        rt.mineShiftLaneAfterReturn = false;
        rt.mineShiftLaneAfterReturnAttempt = 0;
        rt.mineShiftLaneAfterReturnOrigin = null;
        rt.mineBoundaryTurnPhase = 0;
        rt.mineBoundaryTurnNextSide = 1;
        rt.mineBoundaryTurnOriginalStepX = 0;
        rt.mineBoundaryTurnOriginalStepZ = 0;
        rt.mineCollectedItemIds.clear();
        rt.commandActive = true;
        rt.mineStatusText = "Searching for " + canonicalTarget;
        return companion.getDisplayName() + " searching for " + canonicalTarget + ".";
    }

    public String stopMining(PlayerRef playerRef, CompanionRecord companion) {
        if (playerRef == null || companion == null) {
            return "Unable to stop mining.";
        }
        CompanionRuntimeState rt = this.companionManager.getRuntime(companion.uniqueId);
        rt.miningActive = false;
        rt.mineForSpecific = false;
        rt.mineSearchRadius = 0;
        rt.mineTargetBlockId = null;
        rt.mineTargetPos = null;
        rt.mineBlockedTargetUntilByKey.clear();
        rt.mineAnchorPos = null;
        rt.mineMoveToAnchorPending = false;
        rt.mineStepX = 0;
        rt.mineStepY = 0;
        rt.mineStepZ = 0;
        rt.mineClearanceY = 0;
        rt.mineUnreachableTicks = 0;
        rt.mineEmptyForwardConsecutive = 0;
        rt.mineFallDetectedTick = 0L;
        rt.mineLastMinedPos = null;
        rt.mineDepositOnlyMined = false;
        rt.mineReturnToLoadedArea = false;
        rt.mineShiftLaneAfterReturn = false;
        rt.mineShiftLaneAfterReturnAttempt = 0;
        rt.mineShiftLaneAfterReturnOrigin = null;
        rt.mineBoundaryTurnPhase = 0;
        rt.mineBoundaryTurnNextSide = 1;
        rt.mineBoundaryTurnOriginalStepX = 0;
        rt.mineBoundaryTurnOriginalStepZ = 0;
        rt.mineCollectedItemIds.clear();
        rt.commandActive = false;
        rt.mineStatusText = "Stopped";
        this.restoreFollowTargetsIfNeeded(playerRef, companion);
        return companion.getDisplayName() + " stopped mining.";
    }

    public void suspendOwnerFollowTargetingForActiveWork(PlayerRef playerRef, CompanionRecord companion) {
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        if (active == null) {
            return;
        }
        NPCEntity npcEntity = (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType());
        if (npcEntity == null) {
            return;
        }
        this.clearFollowTargetChannels(npcEntity);
        try {
            npcEntity.getRole().getWorldSupport().requestNewPath();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public void prepareIssuedCommand(PlayerRef playerRef, CompanionRecord companion) {
        if (companion == null) {
            return;
        }
        CompanionRuntimeState runtime = this.companionManager.getRuntime(companion.uniqueId);
        if (runtime == null) {
            return;
        }
        runtime.lastCombatScanTick = 0L;
        if (companion.followMode != FollowMode.FREE) {
            return;
        }
        this.resetPatrolAndFreeRuntime(runtime);
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        if (active == null) {
            return;
        }
        NPCEntity npcEntity = (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType());
        if (npcEntity == null) {
            return;
        }
        this.clearManualMovementTargets(npcEntity);
    }

    private void restoreFollowTargetsIfNeeded(PlayerRef playerRef, CompanionRecord companion) {
        if (playerRef == null || companion == null || companion.followMode != FollowMode.FOLLOW) {
            return;
        }
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        if (active == null) {
            return;
        }
        NPCEntity npcEntity = (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType());
        if (npcEntity == null) {
            return;
        }
        CompanionRuntimeState runtime = this.companionManager.getRuntime(companion.uniqueId);
        if (runtime != null) {
            runtime.state = CompanionState.IDLE_FOLLOW;
            runtime.followBootstrapTicks = 4;
            runtime.wasHolding = false;
        }
        this.ensureNonIdleState(npcEntity, active.ref, active.store, runtime);
        this.setFollowTarget(npcEntity, playerRef, active.ref, active.store);
        try {
            npcEntity.getRole().getWorldSupport().requestNewPath();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public void stopCompanionMotionNow(PlayerRef playerRef, CompanionRecord companion) {
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        if (active == null) {
            return;
        }
        try {
            NPCEntity npcEntity = (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType());
            if (npcEntity == null) {
                return;
            }
            try {
                MotionController mc = npcEntity.getRole().getActiveMotionController();
                if (mc != null) {
                    mc.forceVelocity(new Vector3d(0.0, 0.0, 0.0), null, false);
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                npcEntity.onFlockSetTarget("LockedTarget", active.ref);
                npcEntity.onFlockSetTarget("Target", active.ref);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                npcEntity.getRole().getWorldSupport().requestNewPath();
            }
            catch (Throwable throwable) {}
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private Vector3d getPlayerPosition(PlayerRef playerRef) {
        if (playerRef == null || playerRef.getWorldUuid() == null) {
            return null;
        }
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            return null;
        }
        Store store = world.getEntityStore().getStore();
        if (store == null) {
            return null;
        }
        Ref ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return null;
        }
        TransformComponent tf = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
        return tf != null ? tf.getPosition() : null;
    }

    private int[] getFacingStepFromPlayerRotation(PlayerRef playerRef) {
        if (playerRef == null || playerRef.getWorldUuid() == null) {
            return new int[]{0, 0};
        }
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            return new int[]{0, 0};
        }
        Store store = world.getEntityStore().getStore();
        if (store == null) {
            return new int[]{0, 0};
        }
        Ref ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return new int[]{0, 0};
        }
        TransformComponent tf = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
        if (tf == null || tf.getRotation() == null) {
            return new int[]{0, 0};
        }
        float yaw = tf.getRotation().y;
        double rad = Math.toRadians(yaw);
        int sx = (int)Math.round(-Math.sin(rad));
        int sz = (int)Math.round(Math.cos(rad));
        if (Math.abs(sx) >= Math.abs(sz)) {
            return new int[]{Integer.compare(sx, 0), 0};
        }
        return new int[]{0, Integer.compare(sz, 0)};
    }

    private int[] getFacingStepFromRotation(TransformComponent transform) {
        if (transform == null || transform.getRotation() == null) {
            return new int[]{0, 1};
        }
        float yaw = transform.getRotation().y;
        double rad = Math.toRadians(yaw);
        int sx = (int)Math.round(-Math.sin(rad));
        int sz = (int)Math.round(Math.cos(rad));
        if (Math.abs(sx) >= Math.abs(sz)) {
            return new int[]{Integer.compare(sx, 0), 0};
        }
        return new int[]{0, Integer.compare(sz, 0)};
    }

    private double distance2D(Vector3d a, Vector3d b) {
        if (a == null || b == null) {
            return Double.MAX_VALUE;
        }
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private String canonicalMineTargetId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.trim();
        String lower = v.toLowerCase(Locale.ROOT);
        int idIdx = lower.indexOf("id=");
        if (idIdx >= 0) {
            int start = idIdx + 3;
            int end = v.indexOf(44, start);
            if (end < 0) {
                end = v.indexOf(125, start);
            }
            if (end > start) {
                v = v.substring(start, end).trim();
            }
        } else if (lower.startsWith("blockid:")) {
            v = v.substring("blockid:".length()).trim();
        }
        if (v.startsWith("*")) {
            v = v.substring(1).trim();
        }
        return v.isBlank() ? null : v;
    }

    private boolean isAirLikeMineTarget(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return true;
        }
        String v = blockId.toLowerCase(Locale.ROOT);
        return v.equals("air") || v.equals("empty") || v.endsWith(":air") || v.endsWith(":empty") || v.contains("id=empty") || v.contains("group='air'") || v.contains("drawtype=empty") || v.contains("material=empty") || v.contains(" blockid:0") || v.startsWith("blockid:0");
    }

    public String switchRoleEquipment(PlayerRef playerRef, CompanionRecord companion) {
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        if (active == null) {
            return null;
        }
        NPCEntity npcEntity = (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType());
        if (npcEntity == null) {
            return null;
        }
        return switch (companion.mode) {
            default -> throw new MatchException(null, null);
            case CompanionMode.FIGHTER -> {
                String w = InvUtil.equipBestWeapon(npcEntity, active.ref, active.store, this.logger);
                if (w != null) {
                    yield "Equipped weapon: " + this.formatItemId(w);
                }
                yield null;
            }
            case CompanionMode.MINER -> {
                String p = InvUtil.equipBestPickaxe(npcEntity, active.ref, active.store, this.logger);
                if (p != null) {
                    yield "Equipped pickaxe: " + this.formatItemId(p);
                }
                yield null;
            }
            case CompanionMode.FARMER -> {
                String h = InvUtil.equipBestHoe(npcEntity, active.ref, active.store, this.logger);
                if (h != null) {
                    yield "Equipped hoe: " + this.formatItemId(h);
                }
                yield null;
            }
        };
    }

    public String[] getEquipmentStats(PlayerRef playerRef, CompanionRecord companion) {
        NPCEntity npcEntity;
        if (companion == null) {
            return new String[]{"--", "--", "--", "--"};
        }
        String weaponId = companion.getEquipped(EquipmentSlot.WEAPON);
        ArrayList<String> armorItemIds = new ArrayList<String>();
        for (EquipmentSlot slot2 : new EquipmentSlot[]{EquipmentSlot.HELMET, EquipmentSlot.CHESTPLATE, EquipmentSlot.LEGGINGS, EquipmentSlot.BOOTS, EquipmentSlot.OFFHAND}) {
            String equipped = companion.getEquipped(slot2);
            if (equipped == null || equipped.isBlank()) continue;
            armorItemIds.add(equipped);
        }
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        NPCEntity nPCEntity = npcEntity = active != null ? (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType()) : null;
        if (npcEntity != null) {
            try {
                Inventory inventory = npcEntity.getInventory();
                if (inventory != null) {
                    CombinedItemContainer container;
                    if ((weaponId == null || weaponId.isBlank()) && (container = inventory.getCombinedHotbarFirst()) != null) {
                        ItemStack[] slot0 = new ItemStack[]{null};
                        container.forEach((slot, is) -> {
                            if (slot == 0) {
                                slot0[0] = is;
                            }
                        });
                        if (slot0[0] != null && !slot0[0].isEmpty()) {
                            weaponId = slot0[0].getItemId();
                        }
                    }
                    if (armorItemIds.isEmpty()) {
                        ArrayList<String> liveArmor = new ArrayList<String>();
                        for (ItemArmorSlot armorSlot : ItemArmorSlot.values()) {
                            ItemStack armorItem = InvUtil.getEquippedArmor(npcEntity, armorSlot);
                            if (armorItem == null || armorItem.isEmpty() || armorItem.getItemId() == null) continue;
                            liveArmor.add(armorItem.getItemId());
                        }
                        if (!liveArmor.isEmpty()) {
                            armorItemIds = liveArmor;
                        }
                    }
                }
            }
            catch (Throwable t) {
                ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Failed reading live equipment stats.");
            }
        }
        String weaponName = weaponId == null || weaponId.isBlank() ? "None" : this.formatItemId(weaponId);
        double totalAtk = this.estimateWeaponDamageByItemId(weaponId);
        int armorCount = 0;
        double totalDef = 0.0;
        for (String armorId : armorItemIds) {
            if (armorId == null || armorId.isBlank()) continue;
            ++armorCount;
            totalDef += this.estimateArmorDefenseByItemId(armorId);
        }
        Object armorSummary = armorCount > 0 ? armorCount + " piece(s)" : "None";
        int combatIdx = Math.max(0, Math.min(companion.combatLevel - 1, ProgressionConfig.COMBAT_DAMAGE_MULT.length - 1));
        double effectiveAtk = totalAtk * ProgressionConfig.COMBAT_DAMAGE_MULT[combatIdx];
        return new String[]{weaponName, armorSummary, String.format("%.1f", effectiveAtk), String.format("%.1f", totalDef)};
    }

    private double estimateWeaponDamageByItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return 0.0;
        }
        String id = itemId.toLowerCase(Locale.ROOT);
        Double exact = WEAPON_ATK_OVERRIDES.get(id);
        if (exact != null) {
            return exact;
        }
        if (id.contains("crossbow")) {
            return 7.0;
        }
        if (id.contains("shortbow") || id.contains("bow")) {
            return 4.0;
        }
        if (id.contains("battleaxe")) {
            if (id.contains("cobalt")) {
                return 15.0;
            }
            if (id.contains("thorium")) {
                return 13.0;
            }
            return 10.0;
        }
        if (id.contains("dagger")) {
            if (id.contains("cobalt")) {
                return 7.0;
            }
            if (id.contains("thorium")) {
                return 6.0;
            }
            return 4.0;
        }
        if (id.contains("mace")) {
            if (id.contains("cobalt")) {
                return 10.0;
            }
            return 6.0;
        }
        if (id.contains("spear")) {
            return 6.0;
        }
        if (id.contains("sword")) {
            if (id.contains("diamond") || id.contains("cobalt")) {
                return 12.0;
            }
            if (id.contains("steel")) {
                return 8.0;
            }
            if (id.contains("iron")) {
                return 5.0;
            }
            if (id.contains("wood") || id.contains("crude")) {
                return 2.0;
            }
            return 5.0;
        }
        if (id.startsWith("weapon_")) {
            return 3.0;
        }
        return 0.0;
    }

    private double estimateArmorDefenseByItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return 0.0;
        }
        String id = itemId.toLowerCase(Locale.ROOT);
        Double exact = ARMOR_DEF_OVERRIDES.get(id);
        if (exact != null) {
            return exact;
        }
        if (id.contains("shield")) {
            return 5.0;
        }
        double materialBase = 1.0;
        if (id.contains("diamond")) {
            materialBase = 2.5;
        } else if (id.contains("steel")) {
            materialBase = 2.0;
        } else if (id.contains("iron")) {
            materialBase = 1.5;
        } else if (id.contains("copper")) {
            materialBase = 1.0;
        } else if (id.contains("wood")) {
            materialBase = 0.7;
        } else if (id.contains("crude")) {
            materialBase = 0.5;
        }
        double pieceMult = 1.0;
        if (id.contains("chest")) {
            pieceMult = 1.5;
        } else if (id.contains("legs")) {
            pieceMult = 1.25;
        } else if (id.contains("head") || id.contains("helmet")) {
            pieceMult = 1.0;
        } else if (id.contains("hands") || id.contains("boots") || id.contains("gloves")) {
            pieceMult = 0.75;
        }
        return materialBase * pieceMult;
    }

    private String formatItemId(String itemId) {
        if (itemId == null) {
            return "Unknown";
        }
        return itemId.replace("Weapon_", "").replace("Tool_", "").replace("Armor_", "").replace('_', ' ');
    }

    public String equipHeldItemOnCompanion(PlayerRef playerRef, CompanionRecord companion) {
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        if (active == null) {
            return null;
        }
        NPCEntity npcEntity = (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType());
        if (npcEntity == null) {
            return null;
        }
        try {
            Player playerEntity = (Player)active.store.getComponent(playerRef.getReference(), Player.getComponentType());
            if (playerEntity == null) {
                return null;
            }
            Inventory playerInv = playerEntity.getInventory();
            if (playerInv == null) {
                return null;
            }
            ItemStack handItem = playerInv.getItemInHand();
            if (handItem == null || handItem.isEmpty()) {
                return null;
            }
            String itemId = handItem.getItemId();
            String type = null;
            Object displaced = null;
            boolean displacedFallbackInventoryItem = false;
            if (handItem.getItem().getArmor() != null) {
                displaced = InvUtil.equipArmorOnCompanion(npcEntity, active.ref, active.store, handItem, this.logger);
                type = "armor";
            } else if (handItem.getItem().getWeapon() != null) {
                displaced = InvUtil.equipWeaponOnCompanion(npcEntity, active.ref, active.store, handItem, this.logger);
                type = "weapon";
                if (displaced != null && !displaced.isEmpty()) {
                    try {
                        displacedFallbackInventoryItem = displaced.getItem().getWeapon() == null && companion.getEquipped(EquipmentSlot.WEAPON) == null;
                    }
                    catch (Throwable ignored) {
                        displacedFallbackInventoryItem = false;
                    }
                }
            }
            if (type == null) {
                return null;
            }
            short activeSlot = playerInv.getActiveHotbarSlot();
            playerInv.getHotbar().setItemStackForSlot(activeSlot, ItemStack.EMPTY);
            if (displaced != null) {
                if (displacedFallbackInventoryItem) {
                    try {
                        CombinedItemContainer combined;
                        Inventory companionInv = npcEntity.getInventory();
                        CombinedItemContainer combinedItemContainer = combined = companionInv != null ? companionInv.getCombinedHotbarFirst() : null;
                        if (combined != null) {
                            ItemStack remainder;
                            ItemStackTransaction tx = combined.addItemStack(displaced);
                            ItemStack itemStack = remainder = tx != null ? tx.getRemainder() : displaced;
                            displaced = remainder == null || remainder.isEmpty() ? null : remainder;
                        }
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                }
                if (displaced != null && !displaced.isEmpty()) {
                    playerInv.getCombinedHotbarFirst().addItemStack(displaced);
                }
            }
            return type;
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to equip held item on companion.");
            return null;
        }
    }

    private void awardCombatLevelRewards(PlayerRef playerRef, CompanionRecord companion, int newLevel) {
        NPCEntity npcEntity;
        if (newLevel < 1 || newLevel > 10) {
            return;
        }
        int idx = newLevel - 1;
        if (idx >= ProgressionConfig.COMBAT_REWARDS.length) {
            return;
        }
        String[] rewards = ProgressionConfig.COMBAT_REWARDS[idx];
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        NPCEntity nPCEntity = npcEntity = active != null ? (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType()) : null;
        if (npcEntity != null) {
            this.syncRecordedEquipmentFromNpc(npcEntity, companion);
        }
        StringBuilder rewardMsg = new StringBuilder();
        rewardMsg.append(companion.getDisplayName()).append(": Combat Level ").append(newLevel).append(" (").append(ProgressionConfig.COMBAT_LEVEL_NAMES[idx]).append(")! Rewards: ");
        boolean equipmentChanged = false;
        boolean requiresRecordedEquipmentApply = false;
        boolean rewardItemsGranted = false;
        boolean rewardWeaponEquipped = false;
        for (String reward : rewards) {
            int count;
            String[] parts = reward.split(":");
            if (parts.length != 2) continue;
            String itemId = parts[0];
            try {
                count = Integer.parseInt(parts[1]);
            }
            catch (NumberFormatException e) {
                continue;
            }
            if (npcEntity != null && active != null) {
                boolean handledAsDirectEquip = false;
                EquipmentSlot rewardSlot = EquipmentSlot.forItem(itemId);
                boolean weaponReward = rewardSlot == EquipmentSlot.WEAPON;
                boolean offhandReward = rewardSlot == EquipmentSlot.OFFHAND;
                boolean armorReward = rewardSlot == EquipmentSlot.HELMET || rewardSlot == EquipmentSlot.CHESTPLATE || rewardSlot == EquipmentSlot.LEGGINGS || rewardSlot == EquipmentSlot.BOOTS;
                boolean twoHandedReward = weaponReward && EquipmentSlot.isTwoHanded(itemId);
                boolean eligibleDirectWeaponReward = weaponReward && !twoHandedReward && !rewardWeaponEquipped;
                boolean eligibleDirectArmorReward = armorReward;
                boolean eligibleDirectOffhandReward = offhandReward;
                if (count == 1 && (eligibleDirectWeaponReward || eligibleDirectArmorReward || eligibleDirectOffhandReward)) {
                    handledAsDirectEquip = this.recordRewardEquipmentIfBetter(companion, npcEntity, active.ref, active.store, itemId);
                    equipmentChanged |= handledAsDirectEquip;
                    if (handledAsDirectEquip && weaponReward) {
                        rewardWeaponEquipped = true;
                    }
                    if (handledAsDirectEquip && (weaponReward || offhandReward)) {
                        requiresRecordedEquipmentApply = true;
                    }
                }
                if (!handledAsDirectEquip) {
                    InvUtil.giveItemToCompanion(npcEntity, active.ref, active.store, itemId, count, this.logger);
                    rewardItemsGranted = true;
                }
            }
            rewardMsg.append(itemId).append(" x").append(count).append(", ");
        }
        if (npcEntity != null && active != null && (equipmentChanged || rewardItemsGranted)) {
            if (requiresRecordedEquipmentApply) {
                this.applyRecordedEquipmentToNpc(npcEntity, companion);
            }
            InvUtil.equipBestArmor(npcEntity, active.ref, active.store, this.logger);
            this.syncRecordedEquipmentFromNpc(npcEntity, companion);
            this.saveCompanionData("awardCombatLevelRewards", companion);
        }
        if (playerRef != null) {
            playerRef.sendMessage(Message.raw((String)rewardMsg.toString()));
        }
    }

    private boolean recordRewardEquipmentIfBetter(CompanionRecord companion, NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, String itemId) {
        if (companion == null || npcEntity == null || itemId == null || itemId.isBlank()) {
            return false;
        }
        try {
            boolean armorReward;
            EquipmentSlot slot = EquipmentSlot.forItem(itemId);
            if (slot == null || EquipmentSlot.isAmmo(itemId)) {
                return false;
            }
            if (slot == EquipmentSlot.OFFHAND && companion.getEquipped(EquipmentSlot.WEAPON) != null && EquipmentSlot.isTwoHanded(companion.getEquipped(EquipmentSlot.WEAPON))) {
                return false;
            }
            ItemStack rewardItem = new ItemStack(itemId, 1);
            boolean bl = armorReward = slot == EquipmentSlot.HELMET || slot == EquipmentSlot.CHESTPLATE || slot == EquipmentSlot.LEGGINGS || slot == EquipmentSlot.BOOTS;
            if (armorReward) {
                InvUtil.equipArmorOnCompanion(npcEntity, companionRef, store, rewardItem, this.logger);
                companion.setEquipped(slot, itemId);
                return true;
            }
            String previous = companion.getEquipped(slot);
            if (this.shouldPreserveLevelRewardReplacement(previous)) {
                InvUtil.giveItemToCompanion(npcEntity, companionRef, store, previous, 1, this.logger);
            }
            companion.setEquipped(slot, itemId);
            if (slot == EquipmentSlot.WEAPON && EquipmentSlot.isTwoHanded(itemId) && companion.getEquipped(EquipmentSlot.OFFHAND) != null) {
                InvUtil.giveItemToCompanion(npcEntity, companionRef, store, companion.getEquipped(EquipmentSlot.OFFHAND), 1, this.logger);
                companion.setEquipped(EquipmentSlot.OFFHAND, null);
            }
            return true;
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.INFO).withCause(t)).log("Could not direct-equip reward: " + itemId);
            return false;
        }
    }

    private boolean shouldPreserveLevelRewardReplacement(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return false;
        }
        EquipmentSlot slot = EquipmentSlot.forItem(assetId);
        return slot == EquipmentSlot.OFFHAND;
    }

    private boolean isRewardArmorBetter(NPCEntity npcEntity, CompanionRecord companion, EquipmentSlot slot, ItemStack rewardItem) {
        try {
            ItemArmor armor;
            ItemStack current = null;
            ItemArmor itemArmor = armor = rewardItem.getItem() != null ? rewardItem.getItem().getArmor() : null;
            if (armor != null && armor.getArmorSlot() != null) {
                current = InvUtil.getEquippedArmor(npcEntity, armor.getArmorSlot());
            }
            if ((current == null || current.isEmpty()) && companion != null) {
                String recorded = companion.getEquipped(slot);
                current = recorded != null && !recorded.isBlank() ? new ItemStack(recorded, 1) : null;
            }
            return InvUtil.isArmorBetter(rewardItem, current);
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isRewardWeaponBetter(CompanionRecord companion, ItemStack rewardItem) {
        try {
            String recorded = companion != null ? companion.getEquipped(EquipmentSlot.WEAPON) : null;
            ItemStack current = recorded != null && !recorded.isBlank() ? new ItemStack(recorded, 1) : null;
            return InvUtil.isWeaponBetter(rewardItem, current);
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private void awardFarmLevelRewards(PlayerRef playerRef, CompanionRecord companion, int newLevel) {
        double eternalSeedChance;
        if (newLevel < 1 || newLevel > 10) {
            return;
        }
        int idx = newLevel - 1;
        if (idx >= ProgressionConfig.FARM_REWARDS.length) {
            return;
        }
        String[] rewards = ProgressionConfig.FARM_REWARDS[idx];
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        NPCEntity npcEntity = active != null ? (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType()) : null;
        StringBuilder rewardMsg = new StringBuilder();
        rewardMsg.append(companion.getDisplayName()).append(": Farming Level ").append(newLevel).append(" (").append(ProgressionConfig.FARM_LEVEL_NAMES[idx]).append(")! ");
        double speedMult = ProgressionConfig.FARM_SPEED_MULT[idx];
        if (speedMult > 1.0) {
            rewardMsg.append("Harvest speed +").append((int)((speedMult - 1.0) * 100.0)).append("%. ");
        }
        if ((eternalSeedChance = ProgressionConfig.FARM_ETERNAL_SEED_CHANCE[Math.min(idx, ProgressionConfig.FARM_ETERNAL_SEED_CHANCE.length - 1)]) > 0.0) {
            rewardMsg.append("Eternal seed chance +").append((int)Math.round(eternalSeedChance * 100.0)).append("%. ");
        }
        if (ProgressionConfig.FARM_AUTO_REPLANT[idx]) {
            rewardMsg.append("Auto-replant enabled! ");
        }
        if (ProgressionConfig.FARM_DOUBLE_YIELD[idx]) {
            rewardMsg.append("Double yield chance! ");
        }
        rewardMsg.append("Rewards: ");
        boolean rewardItemsGranted = false;
        for (String reward : rewards) {
            int count;
            String[] parts = reward.split(":");
            if (parts.length != 2) continue;
            String itemId = parts[0];
            try {
                count = Integer.parseInt(parts[1]);
            }
            catch (NumberFormatException e) {
                continue;
            }
            if (npcEntity != null && active != null) {
                InvUtil.giveItemToCompanion(npcEntity, active.ref, active.store, itemId, count, this.logger);
                rewardItemsGranted = true;
            }
            rewardMsg.append(itemId).append(" x").append(count).append(", ");
        }
        if (npcEntity != null && active != null && rewardItemsGranted) {
            InvUtil.equipBestArmor(npcEntity, active.ref, active.store, this.logger);
            this.syncRecordedEquipmentFromNpc(npcEntity, companion);
            this.saveCompanionData("awardFarmLevelRewards", companion);
        }
        if (playerRef != null) {
            playerRef.sendMessage(Message.raw((String)rewardMsg.toString()));
        }
    }

    private void tickAll() {
        UUID worldId;
        this.globalTick += 5L;
        if (!this.tickHeartbeatLogged && this.globalTick >= 100L) {
            this.tickHeartbeatLogged = true;
            this.logger.at(Level.INFO).log("[TickHeartbeat] tick loop running, globalTick=" + this.globalTick);
        }
        Universe universe = Universe.get();
        Set<UUID> friendlyIdsSnapshot = this.companionManager.getAllActiveCompanionEntityIds();
        if (this.reviveBenchIntegration != null) {
            for (PlayerRef pr : universe.getPlayers()) {
                World w;
                worldId = pr.getWorldUuid();
                if (worldId == null || (w = universe.getWorld(worldId)) == null) continue;
                this.reviveBenchIntegration.tryRegisterRecipe(w);
                break;
            }
        }
        for (PlayerRef playerRef : universe.getPlayers()) {
            PlayerRuntimeState playerRuntime;
            World world;
            PlayerCompanionData data;
            block30: {
                worldId = playerRef.getWorldUuid();
                if (worldId == null) continue;
                data = this.companionManager.getOrCreate(playerRef.getUuid());
                world = universe.getWorld(worldId);
                if (world == null) continue;
                playerRuntime = this.companionManager.getPlayerRuntime(playerRef.getUuid());
                try {
                    DeathComponent ownerDeath;
                    Ref playerEntityRef = playerRef.getReference();
                    Store store = world.getEntityStore().getStore();
                    DeathComponent deathComponent = ownerDeath = playerEntityRef != null && playerEntityRef.isValid() ? (DeathComponent)store.getComponent(playerEntityRef, DeathComponent.getComponentType()) : null;
                    if (ownerDeath != null) {
                        playerRuntime.ownerWasDead = true;
                        continue;
                    }
                    if (!playerRuntime.ownerWasDead) break block30;
                    playerRuntime.ownerWasDead = false;
                    for (CompanionRecord c : data.getActiveCompanions()) {
                        CompanionRuntimeState rt = this.companionManager.getRuntime(c.uniqueId);
                        rt.stayAnchor = null;
                        rt.stuckTicks = 0;
                        if (c.followMode == FollowMode.FOLLOW) {
                            boolean recovered;
                            block31: {
                                Ref cRef;
                                rt.state = CompanionState.IDLE_FOLLOW;
                                rt.followBootstrapTicks = 4;
                                UUID cid = this.parseUuid(c.entityId);
                                recovered = false;
                                if (cid != null && (cRef = world.getEntityRef(cid)) != null && cRef.isValid()) {
                                    TransformComponent cTransform = (TransformComponent)world.getEntityStore().getStore().getComponent(cRef, TransformComponent.getComponentType());
                                    Vector3d beforeTp = cTransform != null ? cTransform.getPosition() : null;
                                    this.teleportNearOwner(world, (Ref<EntityStore>)cRef, playerRef);
                                    this.logMinerTeleportTrace("ownerRespawn", c, rt, beforeTp, playerRef.getTransform().getPosition(), playerRef.getTransform().getPosition());
                                    try {
                                        NPCEntity npcEntity = (NPCEntity)world.getEntityStore().getStore().getComponent(cRef, NPCEntity.getComponentType());
                                        if (npcEntity == null) break block31;
                                        this.ensureNonIdleState(npcEntity, (Ref<EntityStore>)cRef, (Store<EntityStore>)world.getEntityStore().getStore(), rt);
                                        this.setFollowTarget(npcEntity, playerRef, (Ref<EntityStore>)cRef, (Store<EntityStore>)world.getEntityStore().getStore());
                                        try {
                                            npcEntity.getRole().getWorldSupport().requestNewPath();
                                        }
                                        catch (Throwable throwable) {
                                            // empty catch block
                                        }
                                        recovered = true;
                                    }
                                    catch (Throwable npcEntity) {
                                        // empty catch block
                                    }
                                }
                            }
                            if (recovered) continue;
                            c.entityId = null;
                            this.summonCompanion(playerRef, c, false);
                            continue;
                        }
                        if (c.followMode == FollowMode.STAY) {
                            rt.state = CompanionState.IDLE_STAY;
                            continue;
                        }
                        if (c.followMode == FollowMode.PATROL) {
                            rt.state = CompanionState.IDLE_PATROL;
                            continue;
                        }
                        rt.state = CompanionState.IDLE_FREE;
                    }
                    this.logger.at(Level.INFO).log("Owner respawned \u00c3\u00a2\u00e2\u201a\u00ac\u00e2\u20ac\u009d FOLLOW companions teleported.");
                }
                catch (Throwable playerEntityRef) {
                    // empty catch block
                }
            }
            try {
                Vector3d lastOwnerPos;
                Vector3d ownerPos = playerRef.getTransform().getPosition();
                UUID currentWorldId = playerRef.getWorldUuid();
                boolean worldChanged = playerRuntime.lastOwnerWorldId != null && currentWorldId != null && !playerRuntime.lastOwnerWorldId.equals(currentWorldId);
                boolean jumpedFar = false;
                if (ownerPos != null && playerRuntime.lastOwnerPos != null && (lastOwnerPos = this.toVector3d(playerRuntime.lastOwnerPos)) != null) {
                    boolean bl = jumpedFar = ownerPos.distanceTo(lastOwnerPos) > data.teleportDistance;
                }
                if (worldChanged || jumpedFar) {
                    for (CompanionRecord c : data.getActiveCompanions()) {
                        boolean recovered;
                        block32: {
                            Ref cRef;
                            boolean activeTask;
                            if (c.followMode != FollowMode.FOLLOW) continue;
                            CompanionRuntimeState rt = this.companionManager.getRuntime(c.uniqueId);
                            boolean bl = activeTask = c.mode == CompanionMode.FARMER && rt.commandActive || c.mode == CompanionMode.MINER && rt.miningActive;
                            if (activeTask) continue;
                            UUID cid = this.parseUuid(c.entityId);
                            recovered = false;
                            if (cid != null && (cRef = world.getEntityRef(cid)) != null && cRef.isValid()) {
                                TransformComponent cTransform = (TransformComponent)world.getEntityStore().getStore().getComponent(cRef, TransformComponent.getComponentType());
                                Vector3d beforeTp = cTransform != null ? cTransform.getPosition() : null;
                                this.teleportNearOwner(world, (Ref<EntityStore>)cRef, playerRef);
                                this.logMinerTeleportTrace(worldChanged ? "ownerWorldChanged" : "ownerJumpedFar", c, rt, beforeTp, playerRef.getTransform().getPosition(), playerRef.getTransform().getPosition());
                                rt.state = CompanionState.IDLE_FOLLOW;
                                rt.followBootstrapTicks = 4;
                                try {
                                    NPCEntity npcEntity = (NPCEntity)world.getEntityStore().getStore().getComponent(cRef, NPCEntity.getComponentType());
                                    if (npcEntity == null) break block32;
                                    this.ensureNonIdleState(npcEntity, (Ref<EntityStore>)cRef, (Store<EntityStore>)world.getEntityStore().getStore(), rt);
                                    this.setFollowTarget(npcEntity, playerRef, (Ref<EntityStore>)cRef, (Store<EntityStore>)world.getEntityStore().getStore());
                                    try {
                                        npcEntity.getRole().getWorldSupport().requestNewPath();
                                    }
                                    catch (Throwable throwable) {
                                        // empty catch block
                                    }
                                    recovered = true;
                                }
                                catch (Throwable throwable) {
                                    // empty catch block
                                }
                            }
                        }
                        if (recovered) continue;
                        c.entityId = null;
                        this.summonCompanion(playerRef, c, false);
                    }
                }
                playerRuntime.lastOwnerPos = ownerPos != null ? this.toVec3(ownerPos) : null;
                playerRuntime.lastOwnerWorldId = currentWorldId;
            }
            catch (Throwable ownerPos) {
                // empty catch block
            }
            HashSet<String> seenEntityIds = new HashSet<String>();
            for (CompanionRecord companion : data.companions) {
                if (companion == null || companion.entityId == null || companion.entityId.isBlank() || seenEntityIds.add(companion.entityId)) continue;
                companion.active = false;
                companion.entityId = null;
                this.logger.at(Level.WARNING).log("Cleared duplicate companion entity binding for " + companion.getDisplayName());
            }
            if (ReviveConfig.hasAutoReviveCooldown() && this.globalTick % 20L == 0L) {
                world.execute(() -> this.maybeAutoReviveFallenCompanions(playerRef, data));
            }
            for (CompanionRecord companion : data.companions) {
                if (!companion.active || companion.fallen || companion.entityId == null) continue;
                CompanionRuntimeState runtime = this.companionManager.getRuntime(companion.uniqueId);
                world.execute(() -> this.safeUpdateCompanion(world, playerRef, data, companion, runtime, friendlyIdsSnapshot));
            }
            if (this.isRecruitFallbackScanDue()) {
                world.execute(() -> this.safeScanForRecruitableNPCs(world, playerRef));
            }
            if (this.globalTick % 40L != 0L) continue;
            world.execute(() -> this.safeRefreshRecruitableThreatResponses(world, playerRef));
        }
    }

    private boolean isRecruitFallbackScanDue() {
        return this.globalTick % 10L == 0L;
    }

    private void safeTickAll() {
        try {
            this.tickAll();
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.SEVERE).withCause(t)).log("Companion tick loop crashed.");
        }
    }

    private void safeUpdateCompanion(World world, PlayerRef playerRef, PlayerCompanionData data, CompanionRecord companion, CompanionRuntimeState runtime, Set<UUID> friendlyEntityIds) {
        try {
            this.updateCompanion(world, playerRef, data, companion, runtime, friendlyEntityIds);
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.SEVERE).withCause(t)).log("Companion update failed for " + companion.getDisplayName());
        }
    }

    private void updateCompanion(World world, PlayerRef playerRef, PlayerCompanionData data, CompanionRecord companion, CompanionRuntimeState runtime, Set<UUID> friendlyEntityIds) {
        boolean combatRecoveryBlocked;
        UUID companionId = this.parseUuid(companion.entityId);
        if (companionId == null) {
            companion.entityId = null;
            companion.active = false;
            return;
        }
        Ref ref = world.getEntityRef(companionId);
        if (ref == null || !ref.isValid()) {
            boolean resumed;
            boolean autonomousActive;
            boolean workActive = companion.mode == CompanionMode.MINER && runtime.miningActive || companion.mode == CompanionMode.FARMER && runtime.commandActive;
            boolean bl = autonomousActive = companion.followMode == FollowMode.STAY || companion.followMode == FollowMode.PATROL || companion.followMode == FollowMode.FREE;
            if (workActive || autonomousActive) {
                long graceTicks;
                long l = graceTicks = workActive ? 2400L : 40L;
                if (runtime.invalidEntityRefSinceTick <= 0L) {
                    runtime.invalidEntityRefSinceTick = this.globalTick;
                    this.logger.at(Level.INFO).log("[CompanionRef] invalid ref grace started companion=" + companion.getDisplayName() + " mode=" + String.valueOf((Object)companion.mode) + " stance=" + String.valueOf((Object)companion.followMode) + " entityId=" + companion.entityId);
                }
                if (this.globalTick - runtime.invalidEntityRefSinceTick < graceTicks) {
                    return;
                }
            }
            String staleEntityId = companion.entityId;
            companion.entityId = null;
            this.companionManager.resetRuntime(companion.uniqueId);
            boolean shouldRecoverToOwner = companion.followMode == FollowMode.FOLLOW && (companion.mode != CompanionMode.FARMER || !companion.farmAutoResume) && companion.mode != CompanionMode.MINER;
            boolean shouldRecoverAutonomous = companion.followMode == FollowMode.STAY || companion.followMode == FollowMode.PATROL || companion.followMode == FollowMode.FREE;
            boolean bl2 = resumed = (shouldRecoverToOwner || shouldRecoverAutonomous) && this.summonCompanion(playerRef, companion, false);
            if (!resumed) {
                companion.active = true;
                this.saveCompanionData("detachInvalidEntityRef", companion);
            } else {
                this.logger.at(Level.INFO).log("Re-summoned companion after invalid live entity reference. oldEntityId=" + staleEntityId + " companion=" + companion.getDisplayName());
            }
            return;
        }
        Store store = world.getEntityStore().getStore();
        runtime.invalidEntityRefSinceTick = 0L;
        this.sanitizeSavedInventoryData(companion);
        DeathComponent deathComponent = (DeathComponent)store.getComponent(ref, DeathComponent.getComponentType());
        if (deathComponent != null) {
            this.onCompanionDeath(world, (Store<EntityStore>)store, (Ref<EntityStore>)ref, playerRef, companion, deathComponent);
            this.companionManager.resetRuntime(companion.uniqueId);
            return;
        }
        TransformComponent transform = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        runtime.currentTick = this.globalTick;
        Vector3d npcPos = transform.getPosition();
        Vector3d ownerPos = playerRef.getTransform().getPosition();
        this.updateCompanionLastKnownLocation(companion, playerRef, npcPos);
        if (this.recoverCompanionFromSuffocatingBlock(world, (Ref<EntityStore>)ref, transform, playerRef, companion, runtime)) {
            npcPos = transform.getPosition();
            ownerPos = playerRef.getTransform().getPosition();
            this.updateCompanionLastKnownLocation(companion, playerRef, npcPos);
        }
        double distance = npcPos.distanceTo(ownerPos);
        NPCEntity npcEntity = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
        this.sanitizeLiveInventory(npcEntity);
        this.ensureManagedRoleForCompanion(npcEntity, companion, (Ref<EntityStore>)ref, (Store<EntityStore>)store);
        this.syncFarmHeldItemVisual(companion, runtime, npcEntity);
        this.syncMiningHeldItemVisual(companion, runtime, npcEntity, (Ref<EntityStore>)ref, (Store<EntityStore>)store);
        this.enforceMiningFollowIsolation(npcEntity, companion, runtime);
        this.maybeCaptureMissingAppearance(companion, runtime, npcEntity, (Ref<EntityStore>)ref, (Store<EntityStore>)store);
        this.checkAndConsumeInteraction(npcEntity, (Ref<EntityStore>)ref, playerRef, data, companion, (Store<EntityStore>)store);
        this.maybeRepairActiveInventoryLayout(runtime, npcEntity);
        this.maybeSnapshotActiveInventory(runtime, companion, (Store<EntityStore>)store, (Ref<EntityStore>)ref);
        this.maybeSnapshotActiveAppearance(runtime, companion, (Store<EntityStore>)store, (Ref<EntityStore>)ref);
        boolean activeTaskCommand = companion.mode == CompanionMode.FARMER && runtime.commandActive || companion.mode == CompanionMode.MINER && runtime.miningActive;
        boolean farmAreaDefined = companion.farmAreaTopLeft != null && companion.farmAreaBottomRight != null;
        long nowMs = System.currentTimeMillis();
        if (runtime.launchSuppressUntilMs > nowMs && npcPos != null && ownerPos != null) {
            boolean aboveClamp;
            double maxY = ownerPos.y + 1.0;
            boolean roamingAutonomous = companion.followMode == FollowMode.FREE || companion.followMode == FollowMode.PATROL;
            boolean bl = aboveClamp = npcPos.y > maxY;
            if (aboveClamp) {
                Vector3d beforeClamp = npcPos;
                transform.teleportPosition(new Vector3d(npcPos.x, maxY, npcPos.z));
                npcPos = transform.getPosition();
                this.logMinerTeleportTrace("launchSuppressClamp", companion, runtime, beforeClamp, npcPos, ownerPos);
            }
            if (roamingAutonomous && !aboveClamp) {
                runtime.launchSuppressUntilMs = 0L;
            } else {
                runtime.combatTargetId = null;
                if (!activeTaskCommand && companion.followMode == FollowMode.FOLLOW) {
                    runtime.state = CompanionState.IDLE_FOLLOW;
                    this.setFollowTarget(npcEntity, playerRef, (Ref<EntityStore>)ref, (Store<EntityStore>)store);
                }
                try {
                    MotionController mc;
                    MotionController motionController = mc = npcEntity != null ? npcEntity.getRole().getActiveMotionController() : null;
                    if (mc != null) {
                        mc.forceVelocity(new Vector3d(0.0, 0.0, 0.0), null, false);
                    }
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
        }
        if (npcPos != null && ownerPos != null && npcPos.y - ownerPos.y > 2.25) {
            Vector3d beforeClamp = npcPos;
            transform.teleportPosition(new Vector3d(npcPos.x, ownerPos.y + 0.6, npcPos.z));
            this.logMinerTeleportTrace("antiLaunchClamp", companion, runtime, beforeClamp, transform.getPosition(), ownerPos);
            runtime.stuckTicks = 0;
            runtime.lastPosition = this.toVec3(ownerPos);
            runtime.combatTargetId = null;
            if (!activeTaskCommand && companion.followMode == FollowMode.FOLLOW) {
                runtime.state = CompanionState.IDLE_FOLLOW;
            }
            try {
                MotionController mc;
                MotionController motionController = mc = npcEntity != null ? npcEntity.getRole().getActiveMotionController() : null;
                if (mc != null) {
                    mc.forceVelocity(new Vector3d(0.0, 0.0, 0.0), null, false);
                }
            }
            catch (Throwable mc) {
                // empty catch block
            }
            return;
        }
        this.keepOwnerFriendly(npcEntity, playerRef);
        if (this.globalTick % 30L == 0L) {
            this.keepAllPlayersFriendly(npcEntity, playerRef);
        }
        this.ensureCompanionName((Store<EntityStore>)store, (Ref<EntityStore>)ref, playerRef, companion);
        boolean miningActive = companion.mode == CompanionMode.MINER && runtime.miningActive;
        boolean bl = combatRecoveryBlocked = runtime.state == CompanionState.COMBAT_ENGAGE || runtime.combatTargetId != null;
        if (!miningActive && !combatRecoveryBlocked && companion.followMode == FollowMode.FOLLOW && this.isNpcInStartState(npcEntity)) {
            this.ensureNonIdleState(npcEntity, (Ref<EntityStore>)ref, (Store<EntityStore>)store, runtime);
            this.setFollowTarget(npcEntity, playerRef, (Ref<EntityStore>)ref, (Store<EntityStore>)store);
            try {
                npcEntity.getRole().getWorldSupport().requestNewPath();
            }
            catch (Throwable roamingAutonomous) {
                // empty catch block
            }
        }
        CompanionState nextState = this.evaluatePriorityLadder(world, (Store<EntityStore>)store, playerRef, (Ref<EntityStore>)ref, npcEntity, data, companion, runtime, npcPos, ownerPos, distance, friendlyEntityIds);
        CompanionState previousState = runtime.state;
        if (nextState != runtime.state) {
            runtime.state = nextState;
            runtime.stateEnteredTick = this.globalTick;
            if (previousState == CompanionState.COMBAT_ENGAGE && nextState == CompanionState.IDLE_FOLLOW && companion.followMode == FollowMode.FOLLOW && !activeTaskCommand) {
                this.recoverFollowAfterCombatClear(npcEntity, companion, playerRef, (Ref<EntityStore>)ref, (Store<EntityStore>)store, runtime);
            }
        }
        this.updateMovementOwner(runtime, this.ownerForState(runtime.state));
        this.syncManagedWorkPosition(npcEntity, runtime);
        this.executeState(world, (Store<EntityStore>)store, playerRef, (Ref<EntityStore>)ref, npcEntity, data, companion, runtime, npcPos, ownerPos, distance);
        this.checkProgression(playerRef, companion, runtime);
        this.tickOutOfCombatRegen((Store<EntityStore>)store, (Ref<EntityStore>)ref, companion, runtime);
    }

    private void tickOutOfCombatRegen(Store<EntityStore> store, Ref<EntityStore> companionRef, CompanionRecord companion, CompanionRuntimeState runtime) {
        boolean inCombatNow;
        if (store == null || companionRef == null || companion == null || runtime == null) {
            return;
        }
        if (companion.fallen) {
            return;
        }
        boolean bl = inCombatNow = runtime.state == CompanionState.COMBAT_ENGAGE || runtime.combatTargetId != null;
        if (inCombatNow) {
            runtime.regenWasInCombat = true;
            runtime.regenStartTick = 0L;
            runtime.regenLastApplyTick = 0L;
            return;
        }
        if (runtime.regenWasInCombat) {
            runtime.regenWasInCombat = false;
            runtime.regenStartTick = runtime.currentTick + 140L;
            runtime.regenLastApplyTick = 0L;
            return;
        }
        if (runtime.regenStartTick <= 0L || runtime.currentTick < runtime.regenStartTick) {
            return;
        }
        if (runtime.regenLastApplyTick > 0L && runtime.currentTick - runtime.regenLastApplyTick < 20L) {
            return;
        }
        try {
            EntityStatMap stats = (EntityStatMap)store.getComponent(companionRef, EntityStatMap.getComponentType());
            if (stats == null) {
                return;
            }
            EntityStatValue health = stats.get(DefaultEntityStatTypes.getHealth());
            if (health == null) {
                return;
            }
            float current = health.get();
            float max = health.getMax();
            if (max <= 0.0f || current >= max) {
                return;
            }
            float heal = Math.max(1.0f, max * 0.015f);
            float target = Math.min(max, current + heal);
            stats.setStatValue(DefaultEntityStatTypes.getHealth(), target);
            runtime.regenLastApplyTick = runtime.currentTick;
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Companion regen tick failed.");
        }
    }

    private CompanionState evaluatePriorityLadder(World world, Store<EntityStore> store, PlayerRef ownerRef, Ref<EntityStore> companionRef, NPCEntity npcEntity, PlayerCompanionData data, CompanionRecord companion, CompanionRuntimeState runtime, Vector3d npcPos, Vector3d ownerPos, double distance, Set<UUID> friendlyEntityIds) {
        boolean lootToggleActive;
        boolean hasCombat;
        boolean shouldBeMoving;
        Vector3d anchor;
        boolean strictRoleOverride;
        boolean inCombat = runtime.state == CompanionState.COMBAT_ENGAGE;
        boolean strictFollowOrStay = companion.followMode == FollowMode.FOLLOW || companion.followMode == FollowMode.STAY;
        String roleName = npcEntity != null ? npcEntity.getRoleName() : null;
        boolean nonManagedRole = roleName != null && !COMPANION_ROLE.equalsIgnoreCase(roleName);
        boolean activeTaskCommand = companion.mode == CompanionMode.FARMER && runtime.commandActive || companion.mode == CompanionMode.MINER && runtime.miningActive;
        boolean farmAreaDefined = companion.farmAreaTopLeft != null && companion.farmAreaBottomRight != null;
        boolean bl = strictRoleOverride = nonManagedRole && companion.followMode != FollowMode.PATROL && companion.followMode != FollowMode.FREE && !activeTaskCommand;
        if (!(companion.followMode != FollowMode.FOLLOW || !(distance > 25.0) || activeTaskCommand || companion.mode == CompanionMode.FARMER && farmAreaDefined)) {
            if (runtime.currentTick - runtime.lastFollowTeleportLogTick >= 200L) {
                runtime.lastFollowTeleportLogTick = runtime.currentTick;
                this.logger.at(Level.FINE).log("[FollowTeleportCatchup] companion=" + companion.uniqueId + " dist=" + String.format("%.2f", distance) + " state=" + String.valueOf((Object)runtime.state) + " tick=" + runtime.currentTick);
            }
            return CompanionState.TELEPORT_CATCHUP;
        }
        if (companion.followMode == FollowMode.PATROL && runtime.patrolAnchor != null && npcPos.distanceTo(anchor = this.toVector3d(runtime.patrolAnchor)) > 25.0) {
            return CompanionState.TELEPORT_CATCHUP;
        }
        boolean bl2 = shouldBeMoving = runtime.state == CompanionState.IDLE_FOLLOW || runtime.state == CompanionState.LOOT_SEEK || runtime.state == CompanionState.DEPOSIT_SEEK || runtime.state == CompanionState.FARM_SEEK || runtime.state == CompanionState.MINE_SEEK;
        if (shouldBeMoving && runtime.stuckTicks >= 60) {
            if (companion.mode == CompanionMode.MINER && runtime.miningActive && runtime.mineForSpecific) {
                if (runtime.mineTargetPos != null) {
                    runtime.mineStatusText = "Digging out";
                    return CompanionState.MINE_SEEK;
                }
                runtime.mineTargetPos = null;
                runtime.mineUnreachableTicks = 0;
                runtime.mineStatusText = "Retargeting";
                return CompanionState.IDLE_STAY;
            }
            return CompanionState.RECOVER_STUCK;
        }
        if (strictRoleOverride) {
            return switch (companion.followMode) {
                case FollowMode.STAY -> CompanionState.IDLE_STAY;
                default -> CompanionState.IDLE_FOLLOW;
            };
        }
        if (companion.followMode == FollowMode.FOLLOW && distance > 20.0 && runtime.combatTargetId != null) {
            this.logger.at(Level.INFO).log("[CombatLeash] abandon combat \u2014 player too far. companion=" + companion.uniqueId + " dist=" + String.format("%.1f", distance) + " target=" + runtime.combatTargetId);
            this.combatAssist.abandonCombat(runtime, npcEntity);
        } else if (this.combatAssist.isCombatEnabled(companion.mode) && (companion.followMode != FollowMode.FOLLOW || distance <= 20.0) && (hasCombat = this.combatAssist.evaluateCombat(world, store, ownerRef, companionRef, npcEntity, companion, runtime, ownerPos, npcPos, this.globalTick, friendlyEntityIds != null ? friendlyEntityIds : Set.of()))) {
            return CompanionState.COMBAT_ENGAGE;
        }
        if (runtime.depositRequested && this.depositSystem.evaluateDeposit(world, store, companionRef, npcEntity, data, companion, runtime, npcPos, this.globalTick)) {
            return CompanionState.DEPOSIT_SEEK;
        }
        boolean bl3 = lootToggleActive = companion.mode == CompanionMode.FIGHTER && companion.lootModeEnabled;
        if (lootToggleActive && data.lootEnabled) {
            if (this.lootSystem.evaluateLoot(world, store, companionRef, npcEntity, data, runtime, npcPos, this.globalTick)) {
                return CompanionState.LOOT_SEEK;
            }
        } else if (!(runtime.lootTargetId == null || lootToggleActive && data.lootEnabled)) {
            this.clearLootState(npcEntity, companion, runtime);
        }
        if (companion.mode == CompanionMode.FARMER && runtime.commandActive) {
            boolean farmActive = this.farmSystem.evaluateFarm(world, npcEntity, companion, runtime, npcPos, this.globalTick);
            if (farmActive) {
                return CompanionState.FARM_SEEK;
            }
            if (farmAreaDefined) {
                return CompanionState.IDLE_STAY;
            }
        }
        if (companion.mode == CompanionMode.MINER && runtime.mineReturnToLoadedArea) {
            return CompanionState.MINE_SEEK;
        }
        if (companion.mode == CompanionMode.MINER && runtime.miningActive) {
            if (this.minerSystem.evaluateMine(world, companion, runtime, npcPos, this.globalTick)) {
                return CompanionState.MINE_SEEK;
            }
            return CompanionState.IDLE_STAY;
        }
        if (activeTaskCommand) {
            return CompanionState.IDLE_STAY;
        }
        return switch (companion.followMode) {
            case FollowMode.STAY -> CompanionState.IDLE_STAY;
            case FollowMode.PATROL -> CompanionState.IDLE_PATROL;
            case FollowMode.FREE -> CompanionState.IDLE_FREE;
            default -> CompanionState.IDLE_FOLLOW;
        };
    }

    private void executeState(World world, Store<EntityStore> store, PlayerRef ownerRef, Ref<EntityStore> companionRef, NPCEntity npcEntity, PlayerCompanionData data, CompanionRecord companion, CompanionRuntimeState runtime, Vector3d npcPos, Vector3d ownerPos, double distance) {
        boolean needsMovement = switch (runtime.state) {
            case CompanionState.COMBAT_ENGAGE, CompanionState.LOOT_SEEK, CompanionState.DEPOSIT_SEEK, CompanionState.FARM_SEEK, CompanionState.MINE_SEEK, CompanionState.MINE_ACTIVE, CompanionState.TELEPORT_CATCHUP, CompanionState.RECOVER_STUCK -> true;
            case CompanionState.IDLE_FOLLOW -> {
                if (distance > data.followDistance + 1.5) {
                    yield true;
                }
                yield false;
            }
            case CompanionState.IDLE_PATROL -> true;
            default -> false;
        };
        this.syncRoleStateForRuntime(npcEntity, companionRef, store, runtime);
        if (needsMovement) {
            runtime.stayAnchor = null;
            if (runtime.state != CompanionState.IDLE_FOLLOW) {
                try {
                    MotionController mc = npcEntity.getRole().getActiveMotionController();
                    if (mc != null) {
                        mc.forceVelocity(new Vector3d(0.0, 0.0, 0.0), null, false);
                    }
                }
                catch (Throwable mc) {
                    // empty catch block
                }
            }
        }
        switch (runtime.state) {
            case TELEPORT_CATCHUP: {
                Vector3d beforeTp;
                if (runtime.combatTargetId != null) {
                    this.combatAssist.abandonCombat(runtime, npcEntity);
                }
                if (companion.followMode == FollowMode.PATROL && runtime.patrolAnchor != null) {
                    Store s = world.getEntityStore().getStore();
                    TransformComponent tc = (TransformComponent)s.getComponent(companionRef, TransformComponent.getComponentType());
                    if (tc != null) {
                        Vector3d beforeTp2 = tc.getPosition();
                        tc.teleportPosition(this.toVector3d(runtime.patrolAnchor));
                        this.logMinerTeleportTrace("teleportCatchupPatrol", companion, runtime, beforeTp2, tc.getPosition(), ownerPos);
                    }
                } else {
                    beforeTp = npcPos;
                    this.teleportNearOwner(world, companionRef, ownerRef);
                    this.logMinerTeleportTrace("teleportCatchupOwner", companion, runtime, beforeTp, ownerPos, ownerPos);
                }
                runtime.stuckTicks = 0;
                runtime.lastPosition = this.toVec3(npcPos);
                switch (companion.followMode) {
                    case STAY: {
                        CompanionState companionState = CompanionState.IDLE_STAY;
                        break;
                    }
                    case PATROL: {
                        CompanionState companionState = CompanionState.IDLE_PATROL;
                        break;
                    }
                    case FREE: {
                        CompanionState companionState = CompanionState.IDLE_FREE;
                        break;
                    }
                    default: {
                        CompanionState companionState = runtime.state = CompanionState.IDLE_FOLLOW;
                    }
                }
                if (runtime.state != CompanionState.IDLE_FOLLOW) break;
                runtime.followBootstrapTicks = 4;
                try {
                    this.setFollowTarget(npcEntity, ownerRef, companionRef, store);
                }
                catch (Throwable beforeTp3) {
                    // empty catch block
                }
                try {
                    npcEntity.getRole().getWorldSupport().requestNewPath();
                }
                catch (Throwable beforeTp3) {}
                break;
            }
            case RECOVER_STUCK: {
                if (companion.mode == CompanionMode.MINER && runtime.miningActive) {
                    Vector3d afterTp;
                    Vector3d anchor;
                    TransformComponent recoverTransform = (TransformComponent)store.getComponent(companionRef, TransformComponent.getComponentType());
                    Vector3d beforeTp = recoverTransform != null ? recoverTransform.getPosition() : npcPos;
                    Vector3d recoverPos = null;
                    boolean usedTurnAnchorRecovery = false;
                    if (runtime.mineBoundaryTurnPhase == 2 && runtime.mineAnchorPos != null && (anchor = this.toVector3d(runtime.mineAnchorPos)) != null) {
                        int ax = (int)Math.floor(anchor.x);
                        int ay = (int)Math.floor(anchor.y);
                        int az = (int)Math.floor(anchor.z);
                        double dx = anchor.x - beforeTp.x;
                        double dz = anchor.z - beforeTp.z;
                        double h = Math.sqrt(dx * dx + dz * dz);
                        if (h <= 2.0 && this.canOccupyRecoveryColumn(world, ax, ay, az)) {
                            recoverPos = new Vector3d(anchor.x, (double)ay + 0.05, anchor.z);
                            usedTurnAnchorRecovery = true;
                        }
                    }
                    if (recoverPos == null) {
                        recoverPos = this.findMiningRecoverStuckPos(world, runtime, beforeTp);
                    }
                    if (recoverPos != null && recoverTransform != null) {
                        recoverTransform.teleportPosition(recoverPos);
                    }
                    Vector3d vector3d = afterTp = recoverTransform != null ? recoverTransform.getPosition() : recoverPos;
                    this.logMinerTeleportTrace(recoverPos != null ? (usedTurnAnchorRecovery ? "recoverStuckMiningTurnAnchor" : "recoverStuckMiningLocal") : "recoverStuckMiningNoLocal", companion, runtime, beforeTp, afterTp, ownerPos);
                    runtime.stuckTicks = 0;
                    runtime.lastPosition = this.toVec3(afterTp != null ? afterTp : beforeTp);
                    runtime.state = CompanionState.MINE_SEEK;
                    break;
                }
                Vector3d beforeTp = npcPos;
                this.teleportNearOwner(world, companionRef, ownerRef);
                this.logMinerTeleportTrace("recoverStuck", companion, runtime, beforeTp, ownerPos, ownerPos);
                runtime.stuckTicks = 0;
                runtime.lastPosition = this.toVec3(npcPos);
                runtime.state = switch (companion.followMode) {
                    case FollowMode.STAY -> CompanionState.IDLE_STAY;
                    case FollowMode.PATROL -> CompanionState.IDLE_PATROL;
                    case FollowMode.FREE -> CompanionState.IDLE_FREE;
                    default -> CompanionState.IDLE_FOLLOW;
                };
                break;
            }
            case COMBAT_ENGAGE: {
                this.combatAssist.executeCombat(world, store, npcEntity, companionRef, ownerRef, data, companion, runtime, ownerPos, npcPos);
                runtime.stuckTicks = 0;
                break;
            }
            case LOOT_SEEK: {
                this.lootSystem.executeLootSeek(world, store, npcEntity, companionRef, runtime, npcPos);
                this.updateStuckState(runtime, npcPos, true);
                break;
            }
            case DEPOSIT_SEEK: {
                if (runtime.mineDepositOnlyMined && this.redirectMiningTowardOwnerIfNeeded(npcEntity, runtime, npcPos, ownerPos)) {
                    this.updateStuckState(runtime, npcPos, true);
                    break;
                }
                this.depositSystem.executeDepositSeek(world, store, npcEntity, companionRef, data, companion, runtime, npcPos, this.globalTick);
                this.updateStuckState(runtime, npcPos, true);
                break;
            }
            case FARM_SEEK: {
                this.farmSystem.executeFarmSeek(world, store, npcEntity, companionRef, companion, runtime, npcPos);
                this.updateStuckState(runtime, npcPos, true);
                break;
            }
            case MINE_SEEK: 
            case MINE_ACTIVE: {
                if (this.redirectMiningTowardOwnerIfNeeded(npcEntity, runtime, npcPos, ownerPos)) {
                    this.updateStuckState(runtime, npcPos, true);
                    break;
                }
                if (runtime.mineReturnToLoadedArea && !runtime.miningActive) {
                    runtime.mineReturnToLoadedArea = false;
                    runtime.mineStatusText = "Returned to loaded area";
                    this.updateStuckState(runtime, npcPos, false);
                    break;
                }
                this.minerSystem.executeMineSeek(world, store, npcEntity, companionRef, companion, runtime, npcPos);
                this.updateStuckState(runtime, npcPos, true);
                break;
            }
            case IDLE_STAY: {
                this.executeIdleStay(npcEntity, companionRef, runtime, (TransformComponent)store.getComponent(companionRef, TransformComponent.getComponentType()), npcPos, world);
                break;
            }
            case IDLE_PATROL: {
                this.executeIdlePatrol(npcEntity, companionRef, runtime, npcPos, ownerPos, world);
                break;
            }
            case IDLE_FREE: {
                this.executeIdleFree(world, store, companionRef, npcEntity, runtime, npcPos, ownerPos);
                break;
            }
            default: {
                if (companion.mode == CompanionMode.FARMER && runtime.commandActive || companion.mode == CompanionMode.MINER && runtime.miningActive) {
                    this.executeIdleStay(npcEntity, companionRef, runtime, (TransformComponent)store.getComponent(companionRef, TransformComponent.getComponentType()), npcPos, world);
                    break;
                }
                this.executeIdleFollow(npcEntity, companionRef, ownerRef, data, runtime, npcPos, ownerPos, distance, world);
            }
        }
    }

    private void executeIdleFollow(NPCEntity npcEntity, Ref<EntityStore> companionRef, PlayerRef ownerRef, PlayerCompanionData data, CompanionRuntimeState runtime, Vector3d npcPos, Vector3d ownerPos, double distance, World world) {
        boolean shouldHold;
        double followDistance = data.followDistance;
        double followStartDistance = followDistance + 1.5;
        double followStopDistance = followDistance + 0.5;
        boolean shouldMove = distance > followStartDistance;
        boolean bl = shouldHold = distance <= followStopDistance;
        if (shouldMove) {
            boolean periodicRefresh;
            runtime.stayAnchor = null;
            Store store = world.getEntityStore().getStore();
            if (runtime.wasHolding) {
                try {
                    MotionController mc = npcEntity.getRole().getActiveMotionController();
                    if (mc != null) {
                        mc.forceVelocity(new Vector3d(0.0, 0.0, 0.0), null, false);
                    }
                }
                catch (Throwable mc) {
                    // empty catch block
                }
                runtime.followBootstrapTicks = 4;
                runtime.wasHolding = false;
                this.setFollowTarget(npcEntity, ownerRef, companionRef, (Store<EntityStore>)store);
                try {
                    npcEntity.getRole().resetAllInstructions();
                }
                catch (Throwable mc) {
                    // empty catch block
                }
                try {
                    npcEntity.getRole().getWorldSupport().requestNewPath();
                }
                catch (Throwable mc) {
                    // empty catch block
                }
            }
            boolean bl2 = periodicRefresh = runtime.currentTick - runtime.lastFollowTargetRefreshTick >= 40L;
            if (periodicRefresh) {
                boolean farBehind;
                this.setFollowTarget(npcEntity, ownerRef, companionRef, (Store<EntityStore>)store);
                runtime.lastFollowTargetRefreshTick = runtime.currentTick;
                boolean bl3 = farBehind = distance > followDistance + 6.0;
                if (farBehind && runtime.currentTick - runtime.lastRepathTick >= 80L) {
                    runtime.lastRepathTick = runtime.currentTick;
                    try {
                        npcEntity.getRole().getWorldSupport().requestNewPath();
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                }
            }
            if (runtime.followBootstrapTicks > 0) {
                if (ownerPos != null && npcPos != null) {
                    try {
                        Vector3d delta = new Vector3d(ownerPos.x - npcPos.x, 0.0, ownerPos.z - npcPos.z);
                        double len = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
                        if (len > 0.001) {
                            Vector3d v = new Vector3d(delta.x / len * 0.28, 0.0, delta.z / len * 0.28);
                            MotionController mc = npcEntity.getRole().getActiveMotionController();
                            if (mc != null) {
                                mc.forceVelocity(v, null, false);
                            }
                        }
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                }
                runtime.followBootstrapTicks = Math.max(0, runtime.followBootstrapTicks - 1);
            }
            if (runtime.stuckTicks >= 40 && runtime.currentTick - runtime.lastStartStateNudgeTick >= 40L) {
                runtime.lastStartStateNudgeTick = runtime.currentTick;
                runtime.followBootstrapTicks = 4;
                this.setFollowTarget(npcEntity, ownerRef, companionRef, (Store<EntityStore>)store);
                try {
                    npcEntity.getRole().resetAllInstructions();
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    npcEntity.getRole().getWorldSupport().requestNewPath();
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                this.logger.at(Level.INFO).log("[FollowMoveRepath] companion=" + companionRef.getIndex() + " dist=" + String.format("%.2f", distance) + " stuckTicks=" + runtime.stuckTicks + " tick=" + runtime.currentTick);
            }
        } else if (shouldHold) {
            runtime.followBootstrapTicks = 0;
            this.holdFollowCompanion(npcEntity, companionRef, ownerRef, runtime, world);
            runtime.stuckTicks = 0;
        }
        boolean trackStuck = shouldMove && distance > followDistance + 2.0;
        this.updateStuckState(runtime, npcPos, trackStuck);
    }

    private void maybeCaptureMissingAppearance(CompanionRecord companion, CompanionRuntimeState runtime, NPCEntity npcEntity, Ref<EntityStore> ref, Store<EntityStore> store) {
        if (companion == null || runtime == null || npcEntity == null || ref == null || store == null) {
            return;
        }
        if (companion.appearanceModelId != null && !companion.appearanceModelId.isBlank() && !this.isPlayerTestFallbackAppearance(companion.appearanceModelId)) {
            return;
        }
        if (runtime.appearanceCaptureAttempts >= 8) {
            return;
        }
        if (runtime.nextAppearanceCaptureTick > 0L && runtime.currentTick < runtime.nextAppearanceCaptureTick) {
            return;
        }
        String captured = this.captureBestAppearanceToken(npcEntity, ref, store);
        if (this.isPersistableAppearanceId(captured)) {
            boolean existingKnownCustom;
            boolean capturedFallback = this.isPlayerTestFallbackAppearance(captured);
            boolean recruitOrigin = this.isRecruitableRole(companion.sourceRoleName);
            boolean bl = existingKnownCustom = companion.appearanceModelId != null && !companion.appearanceModelId.isBlank() && !this.isPlayerTestFallbackAppearance(companion.appearanceModelId);
            if (capturedFallback && (recruitOrigin || existingKnownCustom)) {
                ++runtime.appearanceCaptureAttempts;
                runtime.nextAppearanceCaptureTick = runtime.currentTick + 20L;
                return;
            }
            this.setAppearanceModelId(companion, captured, "appearance-captured");
            runtime.appearanceCaptureAttempts = 8;
            runtime.nextAppearanceCaptureTick = 0L;
            this.saveCompanionData("maybeCaptureMissingAppearance", companion);
            return;
        }
        ++runtime.appearanceCaptureAttempts;
        runtime.nextAppearanceCaptureTick = runtime.currentTick + 20L;
    }

    private void executeIdleStay(NPCEntity npcEntity, Ref<EntityStore> companionRef, CompanionRuntimeState runtime, TransformComponent transform, Vector3d npcPos, World world) {
        this.freezeCompanion(npcEntity, companionRef, runtime, npcPos, world);
        runtime.lastPosition = this.toVec3(npcPos);
        runtime.stuckTicks = 0;
    }

    private void executeIdlePatrol(NPCEntity npcEntity, Ref<EntityStore> companionRef, CompanionRuntimeState runtime, Vector3d npcPos, Vector3d ownerPos, World world) {
        Vector3d desiredPos;
        if (this.redirectAutonomousTowardOwnerIfNeeded(npcEntity, runtime, npcPos, ownerPos)) {
            return;
        }
        Store store = world.getEntityStore().getStore();
        TransformComponent transform = (TransformComponent)store.getComponent(companionRef, TransformComponent.getComponentType());
        this.initializePatrolRoute(runtime, transform, npcPos);
        if (runtime.patrolAnchor == null) {
            runtime.stuckTicks = 0;
            return;
        }
        Vector3d anchor = this.toVector3d(runtime.patrolAnchor);
        if (anchor != null && npcPos.distanceTo(anchor) > 20.0) {
            if (transform != null) {
                transform.teleportPosition(anchor);
            }
            this.initializePatrolRoute(runtime, transform, anchor);
            this.driveAutonomousMovement(npcEntity, runtime, runtime.patrolForwardTarget);
            runtime.lastPosition = this.toVec3(anchor);
            runtime.stuckTicks = 0;
            return;
        }
        Vec3 desired = runtime.patrolReturning || runtime.patrolForwardTarget == null ? runtime.patrolAnchor : runtime.patrolForwardTarget;
        Vector3d vector3d = desiredPos = desired != null ? this.toVector3d(desired) : null;
        if (desiredPos == null) {
            runtime.stuckTicks = 0;
            return;
        }
        if (npcPos.distanceTo(desiredPos) <= 1.15) {
            if (runtime.patrolReturning) {
                runtime.patrolReturning = false;
                runtime.patrolForwardTarget = this.computePatrolForwardTarget(runtime);
            } else {
                runtime.patrolReturning = true;
            }
            desired = runtime.patrolReturning ? runtime.patrolAnchor : runtime.patrolForwardTarget;
            desiredPos = desired != null ? this.toVector3d(desired) : null;
        }
        this.driveAutonomousMovement(npcEntity, runtime, this.patrolWaypointTarget(npcPos, desiredPos));
        runtime.lastPosition = this.toVec3(npcPos);
        runtime.stuckTicks = 0;
    }

    private void executeIdleFree(World world, Store<EntityStore> store, Ref<EntityStore> companionRef, NPCEntity npcEntity, CompanionRuntimeState runtime, Vector3d npcPos, Vector3d ownerPos) {
        Vector3d targetPos;
        boolean needsTarget;
        if (this.redirectAutonomousTowardOwnerIfNeeded(npcEntity, runtime, npcPos, ownerPos)) {
            return;
        }
        this.initializeFreeRoam(runtime, npcPos);
        boolean bl = needsTarget = runtime.freeRoamTarget == null || runtime.currentTick >= runtime.nextFreeRoamRetargetTick;
        if (!needsTarget && runtime.freeRoamTarget != null && ((targetPos = this.toVector3d(runtime.freeRoamTarget)) == null || npcPos.distanceTo(targetPos) <= 1.15)) {
            needsTarget = true;
        }
        if (needsTarget) {
            this.pickNextFreeRoamTarget(runtime, npcPos);
        }
        this.tryClearFreeObstacle(world, store, companionRef, npcEntity, runtime, npcPos, runtime.freeRoamTarget);
        this.driveAutonomousMovement(npcEntity, runtime, runtime.freeRoamTarget);
        runtime.lastPosition = this.toVec3(npcPos);
        runtime.stuckTicks = 0;
    }

    private boolean redirectAutonomousTowardOwnerIfNeeded(NPCEntity npcEntity, CompanionRuntimeState runtime, Vector3d npcPos, Vector3d ownerPos) {
        Vec3 redirectTarget;
        if (npcEntity == null || runtime == null || npcPos == null || ownerPos == null) {
            return false;
        }
        double distance = npcPos.distanceTo(ownerPos);
        if (distance <= 100.0) {
            return false;
        }
        double dx = npcPos.x - ownerPos.x;
        double dz = npcPos.z - ownerPos.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        double dirX = len > 0.01 ? dx / len : 1.0;
        double dirZ = len > 0.01 ? dz / len : 0.0;
        runtime.freeRoamTarget = redirectTarget = new Vec3(ownerPos.x + dirX * 10.0, ownerPos.y, ownerPos.z + dirZ * 10.0);
        runtime.nextFreeRoamRetargetTick = runtime.currentTick + 100L;
        this.driveAutonomousMovement(npcEntity, runtime, redirectTarget);
        runtime.lastPosition = this.toVec3(npcPos);
        runtime.stuckTicks = 0;
        return true;
    }

    private boolean redirectMiningTowardOwnerIfNeeded(NPCEntity npcEntity, CompanionRuntimeState runtime, Vector3d npcPos, Vector3d ownerPos) {
        if (npcEntity == null || runtime == null || npcPos == null || ownerPos == null) {
            return false;
        }
        if (!(runtime.miningActive || runtime.mineReturnToLoadedArea || runtime.mineDepositOnlyMined)) {
            return false;
        }
        if (runtime.miningActive && !runtime.mineReturnToLoadedArea && !runtime.mineDepositOnlyMined) {
            return false;
        }
        double distance = npcPos.distanceTo(ownerPos);
        if (distance <= 100.0) {
            if (runtime.mineReturnToLoadedArea && !runtime.miningActive && !runtime.mineDepositOnlyMined) {
                runtime.mineReturnToLoadedArea = false;
                runtime.mineStatusText = "Returned to loaded area";
            }
            return false;
        }
        if (runtime.miningActive && !runtime.mineForSpecific) {
            double towardOwner = (double)runtime.mineStepX * (ownerPos.x - npcPos.x) + (double)runtime.mineStepZ * (ownerPos.z - npcPos.z);
            if (towardOwner > 0.1) {
                return false;
            }
            this.beginDirectionalMiningBoundaryTurn(runtime);
            return false;
        }
        double dx = npcPos.x - ownerPos.x;
        double dz = npcPos.z - ownerPos.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        double dirX = len > 0.01 ? dx / len : 1.0;
        double dirZ = len > 0.01 ? dz / len : 0.0;
        Vec3 redirectTarget = new Vec3(ownerPos.x + dirX * 10.0, ownerPos.y, ownerPos.z + dirZ * 10.0);
        runtime.mineTargetPos = null;
        runtime.mineTargetPriority = null;
        runtime.mineLastTargetDistance = -1.0;
        runtime.mineLastForwardProgressTick = runtime.currentTick;
        runtime.mineUnreachableTicks = 0;
        runtime.mineMoveToAnchorPending = false;
        runtime.mineSearchRadius = 0;
        runtime.mineStatusText = "Returning to loaded area";
        this.driveAutonomousMovement(npcEntity, runtime, redirectTarget);
        runtime.lastPosition = this.toVec3(npcPos);
        runtime.stuckTicks = 0;
        return true;
    }

    private void maybeAutoReviveFallenCompanions(PlayerRef playerRef, PlayerCompanionData data) {
        if (playerRef == null || data == null || !ReviveConfig.hasAutoReviveCooldown()) {
            return;
        }
        long cooldownTicks = ReviveConfig.getAutoReviveCooldownTicks();
        if (cooldownTicks <= 0L) {
            return;
        }
        for (CompanionRecord companion : data.companions) {
            String reviveError;
            if (companion == null || !companion.fallen || companion.deathTime <= 0L || this.globalTick - companion.deathTime < cooldownTicks || (reviveError = this.tryReviveCompanion(playerRef, companion, true)) != null) continue;
            try {
                playerRef.sendMessage(Message.raw((String)(companion.getDisplayName() + " auto-revived after cooldown.")));
            }
            catch (Throwable throwable) {}
        }
    }

    private void beginDirectionalMiningBoundaryTurn(CompanionRuntimeState runtime) {
        if (runtime == null) {
            return;
        }
        if (!runtime.miningActive || runtime.mineForSpecific || runtime.mineBoundaryTurnPhase != 0) {
            return;
        }
        int sx = runtime.mineStepX;
        int sz = runtime.mineStepZ;
        if (sx == 0 && sz == 0) {
            return;
        }
        int sideSign = runtime.mineBoundaryTurnNextSide == 0 ? 1 : runtime.mineBoundaryTurnNextSide;
        int sideX = sideSign * -sz;
        int sideZ = sideSign * sx;
        if (sideX == 0 && sideZ == 0) {
            return;
        }
        runtime.mineShiftLaneAfterReturn = false;
        runtime.mineShiftLaneAfterReturnAttempt = 1;
        runtime.mineShiftLaneAfterReturnOrigin = null;
        runtime.mineBoundaryTurnPhase = 1;
        runtime.mineBoundaryTurnOriginalStepX = sx;
        runtime.mineBoundaryTurnOriginalStepZ = sz;
        runtime.mineStepX = sideX;
        runtime.mineStepZ = sideZ;
        runtime.mineTargetPos = null;
        runtime.mineTargetPriority = null;
        runtime.minePendingBreakPos = null;
        runtime.minePendingBreakBlockId = null;
        runtime.mineLastTargetDistance = -1.0;
        runtime.mineLastForwardProgressTick = runtime.currentTick;
        runtime.mineUnreachableTicks = 0;
        runtime.mineEmptyForwardConsecutive = 0;
        runtime.mineMoveToAnchorPending = false;
        runtime.mineAnchorPos = null;
        runtime.mineStatusText = "Turning into next tunnel lane";
    }

    private void resetPatrolAndFreeRuntime(CompanionRuntimeState runtime) {
        if (runtime == null) {
            return;
        }
        runtime.patrolAnchor = null;
        runtime.patrolForwardTarget = null;
        runtime.patrolReturning = false;
        runtime.patrolStepX = 0;
        runtime.patrolStepZ = 0;
        runtime.freeRoamAnchor = null;
        runtime.freeRoamTarget = null;
        runtime.nextFreeRoamRetargetTick = 0L;
        runtime.freeDirX = 0.0;
        runtime.freeDirZ = 0.0;
        runtime.lastFreeBreakTick = 0L;
    }

    private void initializePatrolRoute(CompanionRuntimeState runtime, TransformComponent transform, Vector3d npcPos) {
        if (runtime == null || npcPos == null) {
            return;
        }
        if (runtime.patrolAnchor == null) {
            runtime.patrolAnchor = this.toVec3(npcPos);
        }
        if (runtime.patrolStepX == 0 && runtime.patrolStepZ == 0 && transform != null) {
            int[] facing = this.getFacingStepFromRotation(transform);
            runtime.patrolStepX = facing[0];
            runtime.patrolStepZ = facing[1];
        }
        if (runtime.patrolStepX == 0 && runtime.patrolStepZ == 0) {
            runtime.patrolStepZ = 1;
        }
        if (runtime.patrolForwardTarget == null) {
            runtime.patrolForwardTarget = this.computePatrolForwardTarget(runtime);
        }
    }

    private Vec3 computePatrolForwardTarget(CompanionRuntimeState runtime) {
        if (runtime == null || runtime.patrolAnchor == null) {
            return null;
        }
        return new Vec3(runtime.patrolAnchor.x + (double)runtime.patrolStepX * 15.0, runtime.patrolAnchor.y, runtime.patrolAnchor.z + (double)runtime.patrolStepZ * 15.0);
    }

    private void initializeFreeRoam(CompanionRuntimeState runtime, Vector3d npcPos) {
        if (runtime == null || npcPos == null) {
            return;
        }
        if (runtime.freeRoamAnchor == null) {
            runtime.freeRoamAnchor = this.toVec3(npcPos);
        }
        if (Math.abs(runtime.freeDirX) < 0.001 && Math.abs(runtime.freeDirZ) < 0.001) {
            double angle = ThreadLocalRandom.current().nextDouble(0.0, Math.PI * 2);
            runtime.freeDirX = Math.cos(angle);
            runtime.freeDirZ = Math.sin(angle);
        }
    }

    private void seedFreeRoamDirection(CompanionRuntimeState runtime, TransformComponent transform) {
        double dirZ;
        if (runtime == null || transform == null || transform.getRotation() == null) {
            return;
        }
        if (Math.abs(runtime.freeDirX) >= 0.001 || Math.abs(runtime.freeDirZ) >= 0.001) {
            return;
        }
        float yaw = transform.getRotation().y;
        double rad = Math.toRadians(yaw);
        double dirX = -Math.sin(rad);
        double len = Math.sqrt(dirX * dirX + (dirZ = Math.cos(rad)) * dirZ);
        if (len <= 0.01) {
            return;
        }
        runtime.freeDirX = dirX / len;
        runtime.freeDirZ = dirZ / len;
    }

    private void pickNextFreeRoamTarget(CompanionRuntimeState runtime, Vector3d npcPos) {
        if (runtime == null || npcPos == null) {
            return;
        }
        runtime.freeRoamAnchor = this.toVec3(npcPos);
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        Vec3 target = null;
        double baseDirX = runtime.freeDirX;
        double baseDirZ = runtime.freeDirZ;
        if (Math.abs(baseDirX) < 0.001 && Math.abs(baseDirZ) < 0.001) {
            double angle = rng.nextDouble(0.0, Math.PI * 2);
            baseDirX = Math.cos(angle);
            baseDirZ = Math.sin(angle);
        }
        for (int i = 0; i < 10; ++i) {
            double lateral;
            double angleOffset = rng.nextDouble(-0.45, 0.45);
            double cos = Math.cos(angleOffset);
            double sin = Math.sin(angleOffset);
            double dirX = baseDirX * cos - baseDirZ * sin;
            double dirZ = baseDirX * sin + baseDirZ * cos;
            double radius = rng.nextDouble(3.0, 7.0);
            Vec3 candidate = new Vec3(npcPos.x + dirX * radius + -dirZ * (lateral = rng.nextDouble(-1.25, 1.25)), npcPos.y, npcPos.z + dirZ * radius + dirX * lateral);
            if (!(this.distance2D(npcPos, this.toVector3d(candidate)) >= 2.75)) continue;
            target = candidate;
            runtime.freeDirX = dirX;
            runtime.freeDirZ = dirZ;
            break;
        }
        if (target == null) {
            target = new Vec3(npcPos.x + baseDirX * 3.0, npcPos.y, npcPos.z + baseDirZ * 3.0);
            runtime.freeDirX = baseDirX;
            runtime.freeDirZ = baseDirZ;
        }
        runtime.freeRoamTarget = target;
        runtime.nextFreeRoamRetargetTick = runtime.currentTick + 100L;
    }

    private void tryClearFreeObstacle(World world, Store<EntityStore> store, Ref<EntityStore> companionRef, NPCEntity npcEntity, CompanionRuntimeState runtime, Vector3d npcPos, Vec3 freeTarget) {
        if (world == null || store == null || companionRef == null || npcEntity == null || runtime == null || npcPos == null || freeTarget == null) {
            return;
        }
        if (runtime.currentTick - runtime.lastFreeBreakTick < 12L) {
            return;
        }
        Vector3d targetPos = this.toVector3d(freeTarget);
        if (targetPos == null) {
            return;
        }
        double dx = targetPos.x - npcPos.x;
        double dz = targetPos.z - npcPos.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len <= 0.05) {
            return;
        }
        int stepX = (int)Math.round(dx / len);
        int stepZ = (int)Math.round(dz / len);
        if (Math.abs(stepX) >= Math.abs(stepZ)) {
            stepX = Integer.compare(stepX, 0);
            stepZ = 0;
        } else {
            stepX = 0;
            stepZ = Integer.compare(stepZ, 0);
        }
        if (stepX == 0 && stepZ == 0) {
            return;
        }
        int standX = (int)Math.floor(npcPos.x);
        int standY = (int)Math.floor(npcPos.y);
        int standZ = (int)Math.floor(npcPos.z);
        int frontX = standX + stepX;
        int frontZ = standZ + stepZ;
        int legY = standY;
        int headY = standY + 1;
        if (this.breakFreeObstacleBlock(world, store, companionRef, npcEntity, runtime, frontX, headY, frontZ) || this.breakFreeObstacleBlock(world, store, companionRef, npcEntity, runtime, frontX, legY, frontZ)) {
            runtime.lastFreeBreakTick = runtime.currentTick;
        }
    }

    private boolean breakFreeObstacleBlock(World world, Store<EntityStore> store, Ref<EntityStore> companionRef, NPCEntity npcEntity, CompanionRuntimeState runtime, int x, int y, int z) {
        String blockId = WorldQueries.getBlockTypeAt(world, x, y, z);
        if (!this.isFreeObstacleMineable(blockId)) {
            return false;
        }
        boolean broke = WorldQueries.breakBlock(world, x, y, z);
        if (broke) {
            this.grantFreeObstacleYield(npcEntity, companionRef, store, runtime, blockId);
        }
        return broke;
    }

    private boolean isFreeObstacleMineable(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return false;
        }
        String v = blockId.trim().toLowerCase(Locale.ROOT);
        if (v.startsWith("*")) {
            v = v.substring(1).trim();
        }
        return !v.equals("air") && !v.equals("empty") && !v.endsWith(":air") && !v.endsWith(":empty") && !v.contains("id=empty") && !v.contains("group='air'") && !v.contains("drawtype=empty") && !v.contains("material=empty") && !v.contains(" blockid:0") && !v.startsWith("blockid:0");
    }

    private void grantFreeObstacleYield(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, CompanionRuntimeState runtime, String minedBlockRaw) {
        if (npcEntity == null || companionRef == null || store == null || minedBlockRaw == null || minedBlockRaw.isBlank()) {
            return;
        }
        for (String itemId : this.candidateFreeObstacleRewardItemIds(minedBlockRaw)) {
            try {
                if (!InvUtil.giveItemToCompanion(npcEntity, companionRef, store, itemId, 1, this.logger)) continue;
                runtime.mineCollectedItemIds.add(itemId);
                return;
            }
            catch (Throwable throwable) {
            }
        }
    }

    private List<String> candidateFreeObstacleRewardItemIds(String minedBlockRaw) {
        ArrayList<String> ids = new ArrayList<String>();
        if (minedBlockRaw == null || minedBlockRaw.isBlank()) {
            return ids;
        }
        String explicitDrop = this.extractNamedField(minedBlockRaw, "itemId");
        if (explicitDrop != null && this.isSafeInventoryItemId(explicitDrop)) {
            this.addFreeObstacleRewardCandidate(ids, explicitDrop);
        }
        return ids;
    }

    private String canonicalFreeObstacleBlockId(String minedBlockRaw) {
        int colon;
        if (minedBlockRaw == null || minedBlockRaw.isBlank()) {
            return "";
        }
        String explicitId = this.extractNamedField(minedBlockRaw, "id");
        if (explicitId != null && !explicitId.isBlank()) {
            return explicitId;
        }
        String token = minedBlockRaw.trim();
        int hash = token.indexOf(35);
        if (hash >= 0) {
            token = token.substring(0, hash);
        }
        if ((colon = token.indexOf(58)) >= 0) {
            token = token.substring(colon + 1);
        }
        if (token.startsWith("*")) {
            token = token.substring(1);
        }
        return token.trim();
    }

    private String extractNamedField(String raw, String fieldName) {
        char c;
        int end;
        if (raw == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        String needle = fieldName + "=";
        int start = raw.indexOf(needle);
        if (start < 0) {
            return null;
        }
        for (end = start += needle.length(); end < raw.length() && (c = raw.charAt(end)) != ',' && c != '}' && !Character.isWhitespace(c); ++end) {
        }
        if (end <= start) {
            return null;
        }
        String value = raw.substring(start, end).trim();
        return value.isBlank() ? null : value;
    }

    private void addFreeObstacleRewardCandidate(List<String> out, String id) {
        if (out == null || id == null) {
            return;
        }
        String value = id.trim();
        if (value.isBlank() || out.contains(value)) {
            return;
        }
        out.add(value);
    }

    private Vec3 patrolWaypointTarget(Vector3d npcPos, Vector3d desiredPos) {
        if (npcPos == null || desiredPos == null) {
            return null;
        }
        double dx = desiredPos.x - npcPos.x;
        double dz = desiredPos.z - npcPos.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len <= 2.5 || len <= 0.01) {
            return new Vec3(desiredPos.x, desiredPos.y, desiredPos.z);
        }
        double scale = 2.5 / len;
        return new Vec3(npcPos.x + dx * scale, npcPos.y, npcPos.z + dz * scale);
    }

    private void driveAutonomousMovement(NPCEntity npcEntity, CompanionRuntimeState runtime, Vec3 target) {
        if (npcEntity == null || runtime == null) {
            return;
        }
        if (target == null) {
            WorkPositioning.clearManagedAutonomousPosition(npcEntity, this.logger);
            return;
        }
        if (runtime.currentTick - runtime.lastRepathTick < 15L) {
            return;
        }
        runtime.lastRepathTick = runtime.currentTick;
        WorkPositioning.setManagedAutonomousPosition(npcEntity, this.toVector3d(target), this.logger);
    }

    private void clearManualMovementTargets(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return;
        }
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
        this.clearAllCombatTargetChannels(npcEntity);
        WorkPositioning.clearManagedWorkPosition(npcEntity, this.logger);
        WorkPositioning.clearManagedAutonomousPosition(npcEntity, this.logger);
        try {
            npcEntity.getRole().getWorldSupport().requestNewPath();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void checkProgression(PlayerRef playerRef, CompanionRecord companion, CompanionRuntimeState runtime) {
        int expectedMineLevel;
        int expectedFarmLevel;
        if (this.combatAssist.consumeKillSignal(runtime)) {
            ++companion.combatKills;
            int newLevel = ProgressionConfig.combatLevelForKills(companion.combatKills);
            if (newLevel > companion.combatLevel) {
                companion.combatLevel = newLevel;
                this.awardCombatLevelRewards(playerRef, companion, newLevel);
                playerRef.sendMessage(Message.raw((String)(companion.getDisplayName() + " reached Combat Level " + newLevel + "! (" + ProgressionConfig.COMBAT_LEVEL_NAMES[newLevel - 1] + ")")));
            }
            this.saveCompanionData("combatKillCredit", companion);
        }
        if ((expectedFarmLevel = ProgressionConfig.farmLevelForHarvests(companion.farmHarvests)) > companion.farmLevel) {
            companion.farmLevel = expectedFarmLevel;
            this.awardFarmLevelRewards(playerRef, companion, expectedFarmLevel);
            playerRef.sendMessage(Message.raw((String)(companion.getDisplayName() + " reached Farming Level " + expectedFarmLevel + "! (" + ProgressionConfig.FARM_LEVEL_NAMES[expectedFarmLevel - 1] + ")")));
        }
        if ((expectedMineLevel = ProgressionConfig.mineLevelForBlocks(companion.mineBlocks)) > companion.mineLevel) {
            companion.mineLevel = expectedMineLevel;
            this.awardMineLevelRewards(playerRef, companion, expectedMineLevel);
            playerRef.sendMessage(Message.raw((String)(companion.getDisplayName() + " reached Mining Level " + expectedMineLevel + "! (" + ProgressionConfig.MINE_LEVEL_NAMES[expectedMineLevel - 1] + ")")));
        }
    }

    private void awardMineLevelRewards(PlayerRef playerRef, CompanionRecord companion, int newLevel) {
        if (playerRef == null || companion == null || newLevel <= 0) {
            return;
        }
        int moveIdx = Math.max(0, Math.min(newLevel - 1, ProgressionConfig.MINE_MOVE_SPEED_MULT.length - 1));
        int speedIdx = Math.max(0, Math.min(newLevel - 1, ProgressionConfig.MINE_SPEED_MULT.length - 1));
        double moveMult = ProgressionConfig.MINE_MOVE_SPEED_MULT[moveIdx];
        double mineMult = ProgressionConfig.MINE_SPEED_MULT[speedIdx];
        StringBuilder rewardMsg = new StringBuilder();
        rewardMsg.append(companion.getDisplayName()).append(": Mining Level ").append(newLevel).append(" rewards - ");
        if (moveMult > 1.0) {
            rewardMsg.append("Move speed +").append((int)Math.round((moveMult - 1.0) * 100.0)).append("%. ");
        }
        if (mineMult > 1.0) {
            rewardMsg.append("Mining speed +").append((int)Math.round((mineMult - 1.0) * 100.0)).append("%. ");
        }
        playerRef.sendMessage(Message.raw((String)rewardMsg.toString().trim()));
    }

    private void ensureNonIdleState(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, CompanionRuntimeState runtime) {
        if (npcEntity == null) {
            return;
        }
        try {
            long now;
            long l = now = runtime != null ? runtime.currentTick : this.globalTick;
            if (runtime == null || now - runtime.lastStartStateNudgeTick >= 20L) {
                npcEntity.getRole().getWorldSupport().requestNewPath();
                if (runtime != null) {
                    runtime.lastStartStateNudgeTick = now;
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void syncManagedWorkPosition(NPCEntity npcEntity, CompanionRuntimeState runtime) {
        boolean freeActive;
        if (npcEntity == null || runtime == null) {
            return;
        }
        boolean workMining = runtime.miningActive && runtime.mineForSpecific && (runtime.mineTargetPos != null || runtime.state == CompanionState.MINE_SEEK || runtime.state == CompanionState.MINE_ACTIVE || runtime.state == CompanionState.IDLE_STAY);
        boolean patrolActive = runtime.state == CompanionState.IDLE_PATROL && runtime.patrolAnchor != null;
        boolean bl = freeActive = runtime.state == CompanionState.IDLE_FREE && runtime.freeRoamTarget != null;
        if (workMining || patrolActive || freeActive) {
            return;
        }
        WorkPositioning.clearManagedWorkPosition(npcEntity, this.logger);
        WorkPositioning.clearManagedAutonomousPosition(npcEntity, this.logger);
    }

    private void suppressWandering(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return;
        }
        try {
            npcEntity.getRole().resetAllInstructions();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            MotionController mc = npcEntity.getRole().getActiveMotionController();
            if (mc != null) {
                mc.forceVelocity(new Vector3d(0.0, 0.0, 0.0), null, false);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void freezeCompanion(NPCEntity npcEntity, Ref<EntityStore> companionRef, CompanionRuntimeState runtime, Vector3d currentPos, World world) {
        Store store;
        TransformComponent transform;
        if (runtime.stayAnchor == null) {
            runtime.stayAnchor = this.toVec3(currentPos);
        }
        Store freezeStore = world.getEntityStore().getStore();
        this.ensureStayState(npcEntity, companionRef, (Store<EntityStore>)freezeStore);
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
        try {
            this.setDirectFlockTargetOnNpcEntity(npcEntity, companionRef);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            this.clearAllCombatTargetChannels(npcEntity);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            npcEntity.getRole().resetAllInstructions();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            MotionController mc = npcEntity.getRole().getActiveMotionController();
            if (mc != null) {
                mc.forceVelocity(new Vector3d(0.0, 0.0, 0.0), null, true);
            }
        }
        catch (Throwable mc) {
            // empty catch block
        }
        Vector3d anchor = this.toVector3d(runtime.stayAnchor);
        if (anchor.distanceTo(currentPos) > 3.0 && (transform = (TransformComponent)(store = world.getEntityStore().getStore()).getComponent(companionRef, TransformComponent.getComponentType())) != null) {
            transform.teleportPosition(anchor);
        }
    }

    private void holdFollowCompanion(NPCEntity npcEntity, Ref<EntityStore> companionRef, PlayerRef ownerRef, CompanionRuntimeState runtime, World world) {
        boolean firstHold;
        if (npcEntity == null || companionRef == null || ownerRef == null || world == null) {
            return;
        }
        Store store = world.getEntityStore().getStore();
        this.ensureNonIdleState(npcEntity, companionRef, (Store<EntityStore>)store, runtime);
        boolean bl = firstHold = !runtime.wasHolding;
        if (firstHold) {
            this.setFollowTarget(npcEntity, ownerRef, companionRef, (Store<EntityStore>)store);
            try {
                npcEntity.getRole().getWorldSupport().requestNewPath();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            runtime.lastFollowTargetRefreshTick = runtime.currentTick;
        }
        runtime.wasHolding = true;
    }

    private void updateStuckState(CompanionRuntimeState runtime, Vector3d currentPos, boolean shouldMove) {
        Vec3 last = runtime.lastPosition;
        if (last == null || last.x == 0.0 && last.y == 0.0 && last.z == 0.0 && runtime.stuckTicks == 0) {
            runtime.lastPosition = this.toVec3(currentPos);
            return;
        }
        if (!shouldMove) {
            runtime.stuckTicks = 0;
            runtime.lastPosition = this.toVec3(currentPos);
            return;
        }
        double dx = currentPos.x - last.x;
        double dy = currentPos.y - last.y;
        double dz = currentPos.z - last.z;
        double moved = Math.sqrt(dx * dx + dy * dy + dz * dz);
        runtime.stuckTicks = moved < 0.2 ? (runtime.stuckTicks += 5) : 0;
        runtime.lastPosition = this.toVec3(currentPos);
    }

    private boolean spawnCompanion(World world, PlayerRef playerRef, CompanionRecord companion) {
        return this.spawnCompanion(world, playerRef, companion, null, false, false);
    }

    private boolean spawnCompanion(World world, PlayerRef playerRef, CompanionRecord companion, Vector3d preferredPosition) {
        return this.spawnCompanion(world, playerRef, companion, preferredPosition, false, false);
    }

    private boolean spawnCompanion(World world, PlayerRef playerRef, CompanionRecord companion, Vector3d preferredPosition, boolean forceManagedRole, boolean resumeWorkOnSummon) {
        Pair spawned;
        PlayerSkin spawnPlayerSkin;
        String seededToken;
        Vector3d position;
        String roleName;
        NPCPlugin npcPlugin = NPCPlugin.get();
        if (npcPlugin == null) {
            this.logger.at(Level.WARNING).log("NPCPlugin not available; cannot spawn companion.");
            return false;
        }
        String string = roleName = forceManagedRole ? this.pickRoleName(npcPlugin) : this.resolveSpawnRoleName(npcPlugin, companion);
        if (roleName == null) {
            this.logger.at(Level.WARNING).log("No NPC role templates available.");
            return false;
        }
        Transform transform = playerRef.getTransform();
        Vector3d vector3d = position = preferredPosition != null ? new Vector3d(preferredPosition) : this.spawnPosition(transform);
        if (preferredPosition != null) {
            position.y += 0.25;
        }
        Vector3f rotation = transform.getRotation();
        String preferredAppearance = this.resolveStructuredAppearanceToken(companion);
        if (preferredAppearance == null || preferredAppearance.isBlank()) {
            preferredAppearance = companion.appearanceModelId;
        }
        boolean plainPlayerSkinPrototype = this.usePlainPlayerSkinRecruitPrototype(companion);
        if (this.shouldIgnoreRecruitPlayerSkinToken(companion, preferredAppearance)) {
            if (this.shouldIgnoreRecruitPlayerSkinToken(companion, companion.appearanceModelId)) {
                this.setAppearanceModelId(companion, null, "spawnCompanion-ignoreRecruitPlayerSkin");
            }
            if (this.shouldIgnoreRecruitPlayerSkinToken(companion, companion.savedPlayerSkinToken)) {
                companion.savedPlayerSkinToken = null;
            }
            preferredAppearance = null;
        }
        if (this.shouldUseSeededRecruitPlayerSkin(companion, preferredAppearance) && this.isPlayerSkinToken(seededToken = this.generatePlayerSkinTokenFromSeed(companion, "spawnCompanion-seededRecruit"))) {
            preferredAppearance = seededToken;
            this.setAppearanceModelId(companion, seededToken, "spawnCompanion-seededRecruit");
        }
        if ((spawnPlayerSkin = this.decodePlayerSkinToken(preferredAppearance)) != null) {
            int roleIndex = this.resolveRoleIndexByName(npcPlugin, roleName);
            Model spawnModel = this.createPlayerSkinModel(spawnPlayerSkin);
            spawned = roleIndex >= 0 && spawnModel != null ? npcPlugin.spawnEntity(world.getEntityStore().getStore(), roleIndex, position, rotation, spawnModel, null) : npcPlugin.spawnNPC(world.getEntityStore().getStore(), roleName, null, position, rotation);
        } else {
            spawned = npcPlugin.spawnNPC(world.getEntityStore().getStore(), roleName, null, position, rotation);
        }
        if (spawned == null) {
            return false;
        }
        Ref ref = (Ref)spawned.first();
        Store store = world.getEntityStore().getStore();
        UUIDComponent uuidComponent = (UUIDComponent)store.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return false;
        }
        companion.entityId = uuidComponent.getUuid().toString();
        companion.active = true;
        this.ensureCompanionName((Store<EntityStore>)store, (Ref<EntityStore>)ref, playerRef, companion);
        Object object = spawned.second();
        if (object instanceof NPCEntity) {
            NPCEntity npcEntity = (NPCEntity)object;
            if (spawnPlayerSkin != null) {
                try {
                    PlayerSkinComponent component = new PlayerSkinComponent(this.clonePlayerSkin(spawnPlayerSkin));
                    this.markSkinComponentDirty(component);
                    store.putComponent(ref, PlayerSkinComponent.getComponentType(), (Component)component);
                }
                catch (Throwable component) {
                    // empty catch block
                }
            }
            this.ensureCompanionInventorySize(npcEntity);
            if (!plainPlayerSkinPrototype) {
                this.applySpawnAppearance(playerRef, companion, (Ref<EntityStore>)ref, (Store<EntityStore>)store, npcEntity);
                this.applyCachedAppearanceSnapshot(companion, npcEntity, (Ref<EntityStore>)ref, (Store<EntityStore>)store, "spawn");
                this.persistCompanionAppearanceToken(companion, npcEntity, (Ref<EntityStore>)ref, (Store<EntityStore>)store, "spawn");
                this.applyPersistentSkinOverlay(companion, (Ref<EntityStore>)ref, (Store<EntityStore>)store, "spawn");
            }
            this.ensureNonIdleState(npcEntity, (Ref<EntityStore>)ref, (Store<EntityStore>)store, null);
            CompanionRuntimeState spawnRuntime = this.companionManager.getRuntime(companion.uniqueId);
            try {
                if (companion.followMode == FollowMode.STAY) {
                    spawnRuntime.stayAnchor = this.toVec3(position);
                    this.setHoldTarget(npcEntity, (Ref<EntityStore>)ref);
                } else if (companion.followMode == FollowMode.PATROL) {
                    this.initializePatrolRoute(spawnRuntime, (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType()), position);
                    this.driveAutonomousMovement(npcEntity, spawnRuntime, spawnRuntime.patrolForwardTarget);
                } else if (companion.followMode == FollowMode.FREE) {
                    this.seedFreeRoamDirection(spawnRuntime, (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType()));
                    this.initializeFreeRoam(spawnRuntime, position);
                    this.pickNextFreeRoamTarget(spawnRuntime, position);
                    this.driveAutonomousMovement(npcEntity, spawnRuntime, spawnRuntime.freeRoamTarget);
                } else {
                    this.setFollowTarget(npcEntity, playerRef, (Ref<EntityStore>)ref, (Store<EntityStore>)store);
                }
                npcEntity.getRole().getWorldSupport().requestNewPath();
            }
            catch (Throwable t) {
                this.logger.at(Level.INFO).log("Could not set initial companion target.");
            }
            try {
                npcEntity.getRole().getStateSupport().setInteractable(ref, true);
            }
            catch (Throwable t) {
                this.logger.at(Level.INFO).log("Could not set companion interactable.");
            }
            this.sanitizeSavedInventoryData(companion);
            if (!companion.savedInventory.isEmpty()) {
                for (Map.Entry entry : companion.savedInventory.entrySet()) {
                    InvUtil.giveItemToCompanion(npcEntity, (Ref<EntityStore>)ref, (Store<EntityStore>)store, (String)entry.getKey(), (Integer)entry.getValue(), this.logger);
                }
                this.logger.at(Level.INFO).log("Restored " + companion.savedInventory.size() + " item types to " + companion.getDisplayName());
                companion.savedInventory.clear();
            }
            if (!companion.startingGearGiven) {
                for (String gearEntry : ProgressionConfig.STARTING_GEAR) {
                    int count;
                    String[] parts = gearEntry.split(":");
                    if (parts.length != 2) continue;
                    String itemId = parts[0];
                    try {
                        count = Integer.parseInt(parts[1]);
                    }
                    catch (NumberFormatException e) {
                        continue;
                    }
                    InvUtil.giveItemToCompanion(npcEntity, (Ref<EntityStore>)ref, (Store<EntityStore>)store, itemId, count, this.logger);
                }
                companion.startingGearGiven = true;
                this.logger.at(Level.INFO).log("Gave starting gear to " + companion.getDisplayName());
            }
            if (!this.applyRecordedEquipmentToNpc(npcEntity, companion)) {
                InvUtil.equipBestWeapon(npcEntity, (Ref<EntityStore>)ref, (Store<EntityStore>)store, this.logger);
                InvUtil.equipBestArmor(npcEntity, (Ref<EntityStore>)ref, (Store<EntityStore>)store, this.logger);
            }
            if (!plainPlayerSkinPrototype) {
                this.ensureManagedRoleForCompanion(npcEntity, companion, (Ref<EntityStore>)ref, (Store<EntityStore>)store);
                this.applyPersistentSkinOverlay(companion, (Ref<EntityStore>)ref, (Store<EntityStore>)store, "spawn-post-equip");
            }
        }
        CompanionRuntimeState runtime = this.companionManager.getRuntime(companion.uniqueId);
        runtime.state = switch (companion.followMode) {
            case FollowMode.STAY -> CompanionState.IDLE_STAY;
            case FollowMode.PATROL -> CompanionState.IDLE_PATROL;
            case FollowMode.FREE -> CompanionState.IDLE_FREE;
            default -> CompanionState.IDLE_FOLLOW;
        };
        this.restorePersistentWorkState(companion, runtime, resumeWorkOnSummon);
        if (runtime.commandActive) {
            runtime.lastFarmScanTick = Math.max(0L, runtime.currentTick - 200L);
            if (companion.mode == CompanionMode.FARMER) {
                runtime.farmStatusText = "Resuming farm";
            }
        }
        runtime.followBootstrapTicks = companion.followMode == FollowMode.FOLLOW ? 4 : 0;
        this.resetPatrolAndFreeRuntime(runtime);
        if (companion.followMode == FollowMode.STAY) {
            runtime.stayAnchor = this.toVec3(position);
        } else if (companion.followMode == FollowMode.PATROL) {
            runtime.patrolAnchor = this.toVec3(position);
        } else if (companion.followMode == FollowMode.FREE) {
            runtime.freeRoamAnchor = this.toVec3(position);
        }
        return true;
    }

    private void ensureCombatState(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store) {
    }

    private void ensureStayState(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store) {
    }

    private void syncRoleStateForRuntime(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, CompanionRuntimeState runtime) {
        if (npcEntity == null || companionRef == null || store == null || runtime == null) {
            return;
        }
        if (runtime.state != CompanionState.IDLE_FOLLOW && runtime.state != CompanionState.IDLE_STAY && runtime.state != CompanionState.COMBAT_ENGAGE) {
            return;
        }
        this.ensureNonIdleState(npcEntity, companionRef, store, runtime);
    }

    private void ensureRoleState(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, String state, String subState) {
    }

    private void ensureCompanionInventorySize(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return;
        }
        try {
            Method method = npcEntity.getClass().getMethod("setInventorySize", Integer.TYPE, Integer.TYPE, Integer.TYPE);
            method.invoke((Object)npcEntity, 9, 18, 1);
            return;
        }
        catch (NoSuchMethodException method) {
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Failed to apply companion inventory size via NPCEntity.");
        }
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory != null) {
                short backpackSlots;
                this.tryResizeInventoryContainer(inventory, "resizeHotbar", 9);
                this.tryResizeInventoryContainer(inventory, "resizeStorage", 18);
                this.tryResizeInventoryContainer(inventory, "resizeOffHand", 1);
                this.tryResizeInventoryContainer(inventory, "resizeOffhand", 1);
                this.tryResizeInventoryContainer(inventory, "resizeUtility", 1);
                short s = backpackSlots = inventory.getBackpack() != null ? inventory.getBackpack().getCapacity() : (short)0;
                if (backpackSlots > 0) {
                    inventory.resizeBackpack(backpackSlots, List.of());
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private boolean tryResizeInventoryContainer(Object inventory, String methodName, int size) {
        if (inventory == null || methodName == null || methodName.isBlank() || size < 0) {
            return false;
        }
        for (Class paramType : new Class[]{Short.TYPE, Integer.TYPE}) {
            try {
                Method method = inventory.getClass().getMethod(methodName, paramType, List.class);
                Integer arg = paramType == Short.TYPE ? (int)size : size;
                method.invoke(inventory, arg, List.of());
                return true;
            }
            catch (NoSuchMethodException method) {
            }
            catch (Throwable t) {
                ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Failed inventory resize call " + methodName + "(size,list).");
                return false;
            }
            try {
                Method method = inventory.getClass().getMethod(methodName, paramType);
                Integer arg = paramType == Short.TYPE ? (int)size : size;
                method.invoke(inventory, arg);
                return true;
            }
            catch (NoSuchMethodException method) {
            }
            catch (Throwable t) {
                ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Failed inventory resize call " + methodName + "(size).");
                return false;
            }
        }
        return false;
    }

    private void maybeRepairActiveInventoryLayout(CompanionRuntimeState runtime, NPCEntity npcEntity) {
        if (runtime == null || npcEntity == null) {
            return;
        }
        if (runtime.currentTick - runtime.lastInventoryLayoutRepairTick < 100L) {
            return;
        }
        runtime.lastInventoryLayoutRepairTick = runtime.currentTick;
        if (!this.isCompanionInventoryLayoutUndersized(npcEntity)) {
            return;
        }
        this.ensureCompanionInventorySize(npcEntity);
    }

    private boolean isCompanionInventoryLayoutUndersized(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return false;
        }
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return false;
            }
            int hotbarSlots = this.getContainerCapacity(inventory.getHotbar());
            int storageSlots = this.getContainerCapacity(inventory.getStorage());
            return hotbarSlots < 9 || storageSlots < 18;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private int getContainerCapacity(Object container) {
        if (container == null) {
            return 0;
        }
        for (String methodName : new String[]{"getCapacity", "getSlotCount", "getSize", "size"}) {
            try {
                Object value = container.getClass().getMethod(methodName, new Class[0]).invoke(container, new Object[0]);
                if (!(value instanceof Number)) continue;
                Number n = (Number)value;
                return n.intValue();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return 0;
    }

    private String pickRoleName(NPCPlugin npcPlugin) {
        List roles = npcPlugin.getRoleTemplateNames(true);
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        if (npcPlugin.hasRoleName(COMPANION_ROLE)) {
            return COMPANION_ROLE;
        }
        if (npcPlugin.hasRoleName("player")) {
            return "player";
        }
        for (String preferred : HUMAN_ROLE_PRIORITY) {
            for (String role : roles) {
                if (!role.equalsIgnoreCase(preferred)) continue;
                return role;
            }
        }
        for (String preferred : HUMAN_ROLE_PRIORITY) {
            for (String role : roles) {
                if (!role.toLowerCase().contains(preferred)) continue;
                return role;
            }
        }
        return (String)roles.get(0);
    }

    private String resolveSpawnRoleName(NPCPlugin npcPlugin, CompanionRecord companion) {
        if (npcPlugin == null) {
            return null;
        }
        return this.pickRoleName(npcPlugin);
    }

    private boolean isPlayerLikeRole(String roleName) {
        if (roleName == null) {
            return false;
        }
        String lower = roleName.toLowerCase(Locale.ROOT);
        return lower.contains("player") || lower.contains("companion") || lower.contains("human") || lower.contains("commoner") || lower.contains("villager");
    }

    private Vector3d spawnPosition(Transform ownerTransform) {
        Vector3d ownerPos = ownerTransform.getPosition();
        Vector3d position = new Vector3d(ownerPos);
        Vector3d direction = ownerTransform.getDirection();
        double behindDist = 3.5;
        double lateralSpread = (ThreadLocalRandom.current().nextDouble() - 0.5) * 3.0;
        if (direction != null) {
            double dx = -direction.x;
            double dz = -direction.z;
            double horizontalLength = Math.sqrt(dx * dx + dz * dz);
            if (horizontalLength > 0.01) {
                double nx = dx / horizontalLength;
                double nz = dz / horizontalLength;
                position.x += nx * behindDist + -nz * lateralSpread;
                position.z += nz * behindDist + nx * lateralSpread;
            }
        } else {
            double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0;
            position.x += Math.cos(angle) * behindDist;
            position.z += Math.sin(angle) * behindDist;
        }
        position.y += 0.5;
        return position;
    }

    private Vector3d preferredRestorePosition(PlayerRef playerRef, CompanionRecord companion) {
        if (playerRef == null || companion == null) {
            return null;
        }
        Vector3d farmRestore = this.preferredFarmRestorePosition(playerRef, companion);
        if (farmRestore != null) {
            return farmRestore;
        }
        if (companion.followMode != FollowMode.STAY && companion.followMode != FollowMode.PATROL && companion.followMode != FollowMode.FREE) {
            return null;
        }
        BlockPos lastPos = companion.lastKnownLocation;
        UUID worldId = playerRef.getWorldUuid();
        if (lastPos == null || lastPos.worldId == null || worldId == null) {
            return null;
        }
        if (!worldId.toString().equalsIgnoreCase(lastPos.worldId)) {
            return null;
        }
        return new Vector3d((double)lastPos.x + 0.5, (double)lastPos.y, (double)lastPos.z + 0.5);
    }

    private boolean shouldUsePersistedRestorePosition(CompanionRecord companion) {
        if (companion == null) {
            return false;
        }
        if (this.hasSavedFarmArea(companion) && companion.mode == CompanionMode.FARMER) {
            return true;
        }
        return companion.followMode == FollowMode.STAY || companion.followMode == FollowMode.PATROL || companion.followMode == FollowMode.FREE;
    }

    private Vector3d preferredFarmRestorePosition(PlayerRef playerRef, CompanionRecord companion) {
        String farmWorldId;
        if (playerRef == null || companion == null) {
            return null;
        }
        if (companion.mode != CompanionMode.FARMER || !this.hasSavedFarmArea(companion)) {
            return null;
        }
        UUID worldId = playerRef.getWorldUuid();
        String string = farmWorldId = companion.farmAreaTopLeft.worldId != null && !companion.farmAreaTopLeft.worldId.isBlank() ? companion.farmAreaTopLeft.worldId : companion.farmAreaBottomRight.worldId;
        if (worldId == null || farmWorldId == null || !worldId.toString().equalsIgnoreCase(farmWorldId)) {
            return null;
        }
        int minX = Math.min(companion.farmAreaTopLeft.x, companion.farmAreaBottomRight.x);
        int maxX = Math.max(companion.farmAreaTopLeft.x, companion.farmAreaBottomRight.x);
        int minY = Math.min(companion.farmAreaTopLeft.y, companion.farmAreaBottomRight.y);
        int maxY = Math.max(companion.farmAreaTopLeft.y, companion.farmAreaBottomRight.y);
        int minZ = Math.min(companion.farmAreaTopLeft.z, companion.farmAreaBottomRight.z);
        int maxZ = Math.max(companion.farmAreaTopLeft.z, companion.farmAreaBottomRight.z);
        double x = ((double)minX + (double)maxX + 1.0) * 0.5;
        double z = ((double)minZ + (double)maxZ + 1.0) * 0.5;
        return new Vector3d(x, (double)maxY + 1.0, z);
    }

    private boolean hasSavedFarmArea(CompanionRecord companion) {
        return companion != null && companion.farmAreaTopLeft != null && companion.farmAreaBottomRight != null;
    }

    private boolean recoverCompanionFromSuffocatingBlock(World world, Ref<EntityStore> companionRef, TransformComponent transform, PlayerRef ownerRef, CompanionRecord companion, CompanionRuntimeState runtime) {
        if (world == null || companionRef == null || transform == null || ownerRef == null || runtime == null) {
            return false;
        }
        if (runtime.currentTick - runtime.lastSuffocationRecoverTick < 20L) {
            return false;
        }
        Vector3d currentPos = transform.getPosition();
        if (currentPos == null) {
            return false;
        }
        if (!this.isCompanionInsideSolidBlock(world, currentPos)) {
            return false;
        }
        boolean miningActive = companion != null && companion.mode == CompanionMode.MINER && runtime.miningActive;
        Vector3d localSafePos = this.findLocalSuffocationRecoveryPos(world, currentPos);
        if (localSafePos == null && miningActive) {
            localSafePos = this.findMiningSuffocationRecoveryPos(world, runtime, currentPos);
        }
        if (localSafePos != null) {
            transform.teleportPosition(localSafePos);
            this.logMinerTeleportTrace("suffocationRecoverLocal", companion, runtime, currentPos, transform.getPosition(), ownerRef != null ? ownerRef.getTransform().getPosition() : null);
        } else if (!miningActive) {
            this.teleportNearOwner(world, companionRef, ownerRef);
            this.logMinerTeleportTrace("suffocationRecoverOwner", companion, runtime, currentPos, ownerRef != null ? ownerRef.getTransform().getPosition() : null, ownerRef != null ? ownerRef.getTransform().getPosition() : null);
        } else {
            return false;
        }
        runtime.lastSuffocationRecoverTick = runtime.currentTick;
        runtime.stuckTicks = 0;
        runtime.lastPosition = this.toVec3(transform.getPosition());
        this.logger.at(Level.INFO).log("[CompanionSuffocationRecover] companion=" + companion.getDisplayName() + " mode=" + String.valueOf((Object)companion.mode) + " local=" + (localSafePos != null));
        return true;
    }

    private boolean isCompanionInsideSolidBlock(World world, Vector3d pos) {
        if (world == null || pos == null) {
            return false;
        }
        int x = (int)Math.floor(pos.x);
        int z = (int)Math.floor(pos.z);
        int feetY = (int)Math.floor(pos.y + 0.05);
        int chestY = (int)Math.floor(pos.y + 1.0);
        int headY = (int)Math.floor(pos.y + 1.6);
        return this.isSolidBlockAt(world, x, chestY, z) || this.isSolidBlockAt(world, x, headY, z) || this.isSolidBlockAt(world, x, feetY, z) && this.isSolidBlockAt(world, x, chestY, z);
    }

    private Vector3d findLocalSuffocationRecoveryPos(World world, Vector3d origin) {
        if (world == null || origin == null) {
            return null;
        }
        int baseX = (int)Math.floor(origin.x);
        int baseY = (int)Math.floor(origin.y);
        int baseZ = (int)Math.floor(origin.z);
        int[] offsets = new int[]{0, 1, -1, 2, -2};
        for (int dy = 0; dy <= 3; ++dy) {
            for (int dx : offsets) {
                int[] nArray = offsets;
                int n = nArray.length;
                for (int i = 0; i < n; ++i) {
                    int x = baseX + dx;
                    int y = baseY + dy;
                    int dz = nArray[i];
                    int z = baseZ + dz;
                    if (!this.canOccupyRecoveryColumn(world, x, y, z)) continue;
                    return new Vector3d((double)x + 0.5, (double)y + 0.05, (double)z + 0.5);
                }
            }
        }
        return null;
    }

    private Vector3d findMiningSuffocationRecoveryPos(World world, CompanionRuntimeState runtime, Vector3d origin) {
        Vector3d[] anchors;
        if (world == null || runtime == null || origin == null) {
            return null;
        }
        Vector3d pos = this.findRecoveryNear(world, origin, 4, 4);
        if (pos != null) {
            return pos;
        }
        for (Vector3d anchor : anchors = new Vector3d[]{this.toVector3d(runtime.lastPosition), this.toVector3d(runtime.mineAnchorPos), this.toVector3d(runtime.mineLastMinedPos)}) {
            if (anchor == null || (pos = this.findRecoveryNear(world, anchor, 3, 3)) == null) continue;
            return pos;
        }
        return null;
    }

    private Vector3d findMiningRecoverStuckPos(World world, CompanionRuntimeState runtime, Vector3d origin) {
        Vector3d[] anchors;
        if (world == null || runtime == null || origin == null) {
            return null;
        }
        Vector3d pos = this.findRecoveryNear(world, origin, 6, 4);
        if (pos != null) {
            return pos;
        }
        for (Vector3d anchor : anchors = new Vector3d[]{this.toVector3d(runtime.lastPosition), this.toVector3d(runtime.mineAnchorPos), this.toVector3d(runtime.mineLastMinedPos)}) {
            if (anchor == null || (pos = this.findRecoveryNear(world, anchor, 5, 4)) == null) continue;
            return pos;
        }
        return null;
    }

    private Vector3d findRecoveryNear(World world, Vector3d origin, int horizontalRadius, int verticalRadiusUp) {
        if (world == null || origin == null) {
            return null;
        }
        int baseX = (int)Math.floor(origin.x);
        int baseY = (int)Math.floor(origin.y);
        int baseZ = (int)Math.floor(origin.z);
        for (int dy = -1; dy <= verticalRadiusUp; ++dy) {
            for (int radius = 0; radius <= horizontalRadius; ++radius) {
                for (int dx = -radius; dx <= radius; ++dx) {
                    for (int dz = -radius; dz <= radius; ++dz) {
                        int x = baseX + dx;
                        int y = baseY + dy;
                        int z = baseZ + dz;
                        if (!this.canOccupyRecoveryColumn(world, x, y, z)) continue;
                        return new Vector3d((double)x + 0.5, (double)y + 0.05, (double)z + 0.5);
                    }
                }
            }
        }
        return null;
    }

    private boolean canOccupyRecoveryColumn(World world, int x, int y, int z) {
        return this.isAirLikeBlockAt(world, x, y, z) && this.isAirLikeBlockAt(world, x, y + 1, z) && this.isAirLikeBlockAt(world, x, y + 2, z);
    }

    private boolean isSolidBlockAt(World world, int x, int y, int z) {
        return !this.isAirLikeBlockAt(world, x, y, z);
    }

    private boolean isAirLikeBlockAt(World world, int x, int y, int z) {
        String blockType = WorldQueries.getBlockTypeAt(world, x, y, z);
        if (blockType == null || blockType.isBlank()) {
            return true;
        }
        String v = blockType.trim().toLowerCase(Locale.ROOT);
        if (v.startsWith("*")) {
            v = v.substring(1).trim();
        }
        return v.equals("air") || v.equals("empty") || v.endsWith(":air") || v.endsWith(":empty") || v.contains("id=empty") || v.contains("group='air'") || v.contains("drawtype=empty") || v.contains("material=empty") || v.contains(" blockid:0") || v.startsWith("blockid:0");
    }

    private void teleportNearOwner(World world, Ref<EntityStore> ref, PlayerRef playerRef) {
        Store store;
        block6: {
            store = world.getEntityStore().getStore();
            TransformComponent transform = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) {
                return;
            }
            Vector3d beforeTp = transform.getPosition();
            Vector3d target = this.spawnPosition(playerRef.getTransform());
            transform.teleportPosition(target);
            try {
                PlayerCompanionData data;
                UUID ownerId;
                UUIDComponent uuidComponent = (UUIDComponent)store.getComponent(ref, UUIDComponent.getComponentType());
                if (uuidComponent == null || uuidComponent.getUuid() == null || (ownerId = this.companionManager.findOwnerByCompanionEntityId(uuidComponent.getUuid())) == null || (data = this.companionManager.get(ownerId)) == null || data.companions == null) break block6;
                String entityId = uuidComponent.getUuid().toString();
                for (CompanionRecord companion : data.companions) {
                    if (companion == null || companion.entityId == null || !entityId.equalsIgnoreCase(companion.entityId)) continue;
                    CompanionRuntimeState runtime = this.companionManager.getRuntime(companion.uniqueId);
                    this.logMinerTeleportTrace("teleportNearOwner", companion, runtime, beforeTp, transform.getPosition(), playerRef != null ? playerRef.getTransform().getPosition() : null);
                    break;
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        try {
            world.execute(() -> {
                try {
                    TransformComponent postTransform = (TransformComponent)store.getComponent(ref, TransformComponent.getComponentType());
                    if (postTransform == null) {
                        return;
                    }
                    Vector3d postPos = postTransform.getPosition();
                    Vector3d ownerPos = playerRef.getTransform().getPosition();
                    if (postPos == null || ownerPos == null) {
                        return;
                    }
                    if (postPos.distanceTo(ownerPos) > 8.0) {
                        double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0;
                        Vector3d safePos = new Vector3d(ownerPos);
                        safePos.x += Math.cos(angle) * 3.5;
                        safePos.z += Math.sin(angle) * 3.5;
                        safePos.y += 0.5;
                        postTransform.teleportPosition(safePos);
                    }
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            });
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void applySpawnAppearance(PlayerRef playerRef, CompanionRecord companion, Ref<EntityStore> ref, Store<EntityStore> store, NPCEntity npcEntity) {
        String resolved;
        String randomizedAppearance;
        boolean shouldRandomizeCosmetics;
        String seededToken;
        String recruitSeededAppearance = this.resolveRecruitSkinToneAppearance(companion, companion.appearanceModelId);
        if (this.shouldIgnoreRecruitPlayerSkinToken(companion, companion.appearanceModelId)) {
            this.setAppearanceModelId(companion, null, "applySpawnAppearance-ignoreRecruitPlayerSkin");
        }
        if (this.shouldIgnoreRecruitPlayerSkinToken(companion, companion.savedPlayerSkinToken)) {
            companion.savedPlayerSkinToken = null;
        }
        if (this.hasExplicitPlayerSkinAppearance(companion)) {
            String preferredSkin = this.resolveStructuredAppearanceToken(companion);
            if (preferredSkin == null || preferredSkin.isBlank()) {
                preferredSkin = companion.appearanceModelId;
            }
            if (this.isPlayerSkinToken(preferredSkin) && this.applyPlayerSkinToken(ref, store, preferredSkin)) {
                return;
            }
        }
        if (this.applyStructuredAppearanceSnapshot(companion, ref, store, "spawn")) {
            return;
        }
        String preferred = this.resolveStructuredAppearanceToken(companion);
        if (preferred == null || preferred.isBlank()) {
            preferred = companion.appearanceModelId;
        }
        if (this.isRandomAppearanceToken(preferred)) {
            preferred = null;
            this.setAppearanceModelId(companion, null, "applySpawnAppearance-randomTokenReset");
        }
        boolean isRecruitOrigin = this.isRecruitableRole(companion.sourceRoleName);
        String recruitSkinToneAppearance = this.resolveRecruitSkinToneAppearance(companion, preferred);
        if (recruitSkinToneAppearance != null) {
            preferred = recruitSkinToneAppearance;
            this.setAppearanceModelId(companion, APPEARANCE_MODEL_TOKEN_PREFIX + recruitSkinToneAppearance, "applySpawnAppearance-recruitSkinTone");
        } else if (this.shouldUseSeededRecruitPlayerSkin(companion, preferred) && this.isPlayerSkinToken(seededToken = this.generatePlayerSkinTokenFromSeed(companion, "applySpawnAppearance-seededRecruit"))) {
            preferred = seededToken;
            this.setAppearanceModelId(companion, seededToken, "applySpawnAppearance-seededRecruit");
        }
        boolean bl = shouldRandomizeCosmetics = (preferred == null || preferred.isBlank()) && !isRecruitOrigin;
        if (shouldRandomizeCosmetics && this.isPersistableAppearanceId(randomizedAppearance = this.applyRandomCosmeticSkin(ref, store, npcEntity))) {
            this.setAppearanceModelId(companion, randomizedAppearance, "appearance-randomized");
            this.logger.at(Level.INFO).log("Applied randomized cosmetic skin to " + companion.getDisplayName() + " appearanceId=" + randomizedAppearance);
            return;
        }
        if (this.isPlayerSkinToken(preferred)) {
            if (this.applyPlayerSkinToken(ref, store, preferred)) {
                this.logger.at(Level.INFO).log("Applied companion player-skin token to " + companion.getDisplayName());
                return;
            }
            this.logger.at(Level.WARNING).log("Failed to apply companion player-skin token for " + companion.getDisplayName() + "; keeping token for retry.");
        }
        if (this.isAppearanceModelToken(preferred)) {
            if (this.applyAppearanceModelToken(ref, store, preferred)) {
                this.logger.at(Level.INFO).log("Applied companion model token to " + companion.getDisplayName());
                return;
            }
            this.logger.at(Level.WARNING).log("Failed to apply companion model token for " + companion.getDisplayName() + "; keeping token for retry.");
        }
        if ((resolved = this.resolveModelId(preferred)) == null && preferred != null && !preferred.isBlank()) {
            try {
                if (this.isSafeAppearanceString(preferred)) {
                    NPCEntity.setAppearance(ref, (String)preferred, store);
                    String captured = this.captureRecruitAppearanceId(npcEntity, ref, store);
                    if (this.isPersistableAppearanceId(captured)) {
                        this.setAppearanceModelId(companion, captured, "appearance-captured");
                    }
                    this.logger.at(Level.INFO).log("Applied companion appearance string " + preferred + " to " + companion.getDisplayName());
                    return;
                }
            }
            catch (Throwable captured) {
                // empty catch block
            }
            this.setAppearanceModelId(companion, null, "applySpawnAppearance-resolvedMissing");
        }
        if (resolved == null || resolved.isBlank()) {
            if (isRecruitOrigin) {
                return;
            }
            resolved = this.chooseDeterministicHumanModelId(companion.uniqueId);
        }
        if (resolved == null || resolved.isBlank()) {
            this.logger.at(Level.WARNING).log("No human-like model assets found for companion.");
            return;
        }
        ModelAsset asset = (ModelAsset)ModelAsset.getAssetMap().getAsset((Object)resolved);
        if (asset == null) {
            return;
        }
        npcEntity.setAppearance(ref, asset, store);
        this.setAppearanceModelId(companion, resolved, "applyAppearanceToActiveCompanion");
        this.logger.at(Level.INFO).log("Applied companion model " + resolved + " to " + companion.getDisplayName());
    }

    private String applyRandomCosmeticSkin(Ref<EntityStore> ref, Store<EntityStore> store, NPCEntity npcEntity) {
        try {
            CosmeticsModule cosmeticsModule = CosmeticsModule.get();
            if (cosmeticsModule == null) {
                return null;
            }
            PlayerSkin randomSkin = cosmeticsModule.generateRandomSkin((Random)ThreadLocalRandom.current());
            if (randomSkin == null) {
                return null;
            }
            String skinText = randomSkin.toString();
            NPCEntity.setAppearance(ref, (String)skinText, store);
            String captured = this.captureRecruitAppearanceId(npcEntity, ref, store);
            if (this.isPersistableAppearanceId(captured)) {
                return captured;
            }
            return null;
        }
        catch (Throwable t) {
            this.logger.at(Level.INFO).log("CosmeticsModule not available for companion appearance: " + t.getMessage());
            return null;
        }
    }

    private String chooseRandomHumanModelId() {
        String preferredRandom = this.resolveModelId("PlayerTestModel_V");
        if (preferredRandom != null) {
            return preferredRandom;
        }
        ArrayList<Object> preferred = new ArrayList<Object>();
        for (Object id : ModelAsset.getAssetMap().getAssetMap().keySet()) {
            String lower;
            if (id == null || !(lower = ((String)id).toLowerCase(Locale.ROOT)).equals("playertestmodel_v") && !lower.equals("playertestmodel_g")) continue;
            preferred.add(id);
        }
        if (!preferred.isEmpty()) {
            Collections.shuffle(preferred, ThreadLocalRandom.current());
            return (String)preferred.get(0);
        }
        ArrayList<String> playerTestModels = new ArrayList<String>();
        for (String id : ModelAsset.getAssetMap().getAssetMap().keySet()) {
            if (id == null || !id.toLowerCase(Locale.ROOT).contains("playertestmodel")) continue;
            playerTestModels.add(id);
        }
        if (!playerTestModels.isEmpty()) {
            Collections.shuffle(playerTestModels, ThreadLocalRandom.current());
            return (String)playerTestModels.get(0);
        }
        List<String> candidates = this.getAvailableModelIds("", true, Integer.MAX_VALUE);
        if (candidates.isEmpty()) {
            return null;
        }
        Collections.shuffle(candidates, ThreadLocalRandom.current());
        for (String id : candidates) {
            if (id.toLowerCase(Locale.ROOT).contains("player")) continue;
            return id;
        }
        return candidates.get(0);
    }

    private String chooseDeterministicHumanModelId(String seedKey) {
        String preferredRandom = this.resolveModelId("PlayerTestModel_V");
        if (preferredRandom != null) {
            return preferredRandom;
        }
        List<Object> candidates = new ArrayList();
        for (String id : ModelAsset.getAssetMap().getAssetMap().keySet()) {
            if (id == null || !id.equalsIgnoreCase("PlayerTestModel_V") && !id.equalsIgnoreCase(RANDOM_HUMAN_MODEL_MERCHANT)) continue;
            candidates.add(id);
        }
        if (candidates.isEmpty()) {
            candidates = this.getAvailableModelIds("", true, Integer.MAX_VALUE);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(String::compareToIgnoreCase);
        int idx = Math.floorMod(seedKey != null ? seedKey.hashCode() : 0, candidates.size());
        return (String)candidates.get(idx);
    }

    private String resolveModelId(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return null;
        }
        ModelAsset exact = (ModelAsset)ModelAsset.getAssetMap().getAsset((Object)modelId);
        if (exact != null) {
            return modelId;
        }
        for (String id : ModelAsset.getAssetMap().getAssetMap().keySet()) {
            if (id == null || !id.equalsIgnoreCase(modelId)) continue;
            return id;
        }
        return null;
    }

    private boolean isHumanModelId(String loweredModelId) {
        boolean includeMatch = false;
        for (String token : HUMAN_MODEL_INCLUDE) {
            if (!loweredModelId.contains(token)) continue;
            includeMatch = true;
            break;
        }
        if (!includeMatch) {
            return false;
        }
        for (String token : ANIMAL_MODEL_EXCLUDE) {
            if (!loweredModelId.contains(token)) continue;
            return false;
        }
        return true;
    }

    private boolean isRecruitableRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return false;
        }
        if (roleName.regionMatches(true, 0, "FH_Companion_Recruitable", 0, "FH_Companion_Recruitable".length())) {
            return true;
        }
        if (roleName.regionMatches(true, 0, "Companion_Recruitable", 0, "Companion_Recruitable".length())) {
            return true;
        }
        if (roleName.regionMatches(true, 0, "Kweebec_Companion_Recruitable", 0, "Kweebec_Companion_Recruitable".length())) {
            return true;
        }
        for (String id : RECRUITABLE_ROLES) {
            if (!id.equalsIgnoreCase(roleName)) continue;
            return true;
        }
        return false;
    }

    private void checkAndConsumeInteraction(NPCEntity npcEntity, Ref<EntityStore> companionRef, PlayerRef ownerRef, PlayerCompanionData ownerData, CompanionRecord companion, Store<EntityStore> store) {
        if (npcEntity == null || this.interactionMenuCallback == null) {
            return;
        }
        try {
            StateSupport stateSupport = npcEntity.getRole().getStateSupport();
            if (this.consumeInteractionTriggered(stateSupport, ownerRef)) {
                UUID ownerId = ownerRef.getUuid();
                if (ownerId != null) {
                    long lastOpen;
                    long now = System.currentTimeMillis();
                    if (now - (lastOpen = this.lastUiOpenMsByPlayer.getOrDefault(ownerId, 0L).longValue()) < 5000L) {
                        return;
                    }
                    this.lastUiOpenMsByPlayer.put(ownerId, now);
                }
                if (ownerData != null && companion != null && companion.uniqueId != null) {
                    ownerData.selectedCompanionId = companion.uniqueId;
                    this.saveCompanionData("selectInteractedCompanion", companion);
                }
                this.interactionMenuCallback.accept(ownerRef);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public NPCEntity getNpcEntity(PlayerRef playerRef, CompanionRecord companion) {
        ActiveCompanionContext active = this.getActiveCompanionContext(playerRef, companion);
        if (active == null) {
            return null;
        }
        try {
            return (NPCEntity)active.store.getComponent(active.ref, NPCEntity.getComponentType());
        }
        catch (Throwable t) {
            return null;
        }
    }

    private ActiveCompanionContext getActiveCompanionContext(PlayerRef playerRef, CompanionRecord companion) {
        if (playerRef == null || companion == null || companion.entityId == null) {
            return null;
        }
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            return null;
        }
        UUID eid = this.parseUuid(companion.entityId);
        if (eid == null) {
            return null;
        }
        Ref ref = world.getEntityRef(eid);
        if (ref == null || !ref.isValid()) {
            return null;
        }
        return new ActiveCompanionContext((Ref<EntityStore>)ref, (Store<EntityStore>)world.getEntityStore().getStore());
    }

    private void ensureCompanionName(Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, CompanionRecord companion) {
        try {
            String expected = companion.getDisplayName(playerRef.getUsername());
            store.putComponent(ref, DisplayNameComponent.getComponentType(), (Component)new DisplayNameComponent(Message.raw((String)expected)));
            Nameplate nameplate = (Nameplate)store.ensureAndGetComponent(ref, Nameplate.getComponentType());
            if (nameplate != null && !expected.equals(nameplate.getText())) {
                nameplate.setText(expected);
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to set companion nameplate.");
        }
    }

    private void setFollowTarget(NPCEntity npcEntity, PlayerRef ownerRef, Ref<EntityStore> companionRef, Store<EntityStore> store) {
        if (npcEntity == null || ownerRef == null) {
            return;
        }
        try {
            Ref ownerEntityRef = ownerRef.getReference();
            boolean locked = this.setTargetChannelOnNpcEntity(npcEntity, "LockedTarget", ownerEntityRef);
            boolean targetSet = this.setTargetChannelOnNpcEntity(npcEntity, "Target", ownerEntityRef);
            boolean direct = this.setDirectFlockTargetOnNpcEntity(npcEntity, ownerEntityRef);
            boolean bl = this.clearAllCombatTargetChannels(npcEntity);
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to set follow target.");
        }
    }

    private void setHoldTarget(NPCEntity npcEntity, Ref<EntityStore> selfRef) {
        if (npcEntity == null || selfRef == null) {
            return;
        }
        try {
            this.setTargetChannelOnNpcEntity(npcEntity, "LockedTarget", selfRef);
            this.setTargetChannelOnNpcEntity(npcEntity, "Target", selfRef);
            this.setDirectFlockTargetOnNpcEntity(npcEntity, selfRef);
            this.clearAllCombatTargetChannels(npcEntity);
            try {
                npcEntity.getRole().getWorldSupport().requestNewPath();
            }
            catch (Throwable throwable) {}
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to set hold target.");
        }
    }

    private long ensureCompanionSkinSeed(CompanionRecord companion, String reason) {
        if (companion == null) {
            return 0L;
        }
        if (companion.skinSeed != 0L) {
            return companion.skinSeed;
        }
        long seed = ThreadLocalRandom.current().nextLong();
        if (seed == 0L) {
            seed = 1L;
        }
        companion.skinSeed = seed;
        this.saveCompanionData(reason, companion);
        return seed;
    }

    private boolean hasExplicitPlayerSkinAppearance(CompanionRecord companion) {
        if (companion == null) {
            return false;
        }
        if (companion.savedPlayerSkinToken != null && this.isPlayerSkinToken(companion.savedPlayerSkinToken)) {
            return true;
        }
        return companion.appearanceModelId != null && this.isPlayerSkinToken(companion.appearanceModelId);
    }

    private boolean shouldTreatPlayerSkinAsSeedOverlay(CompanionRecord companion) {
        if (companion == null || companion.skinSeed == 0L) {
            return false;
        }
        return !this.hasExplicitPlayerSkinAppearance(companion);
    }

    private boolean shouldRejectRecruitPlayerSkinCapture(CompanionRecord companion, String token) {
        if (companion == null || !this.isRecruitableRole(companion.sourceRoleName)) {
            return false;
        }
        if (!this.isPlayerSkinToken(token)) {
            return false;
        }
        if (this.usePlainPlayerSkinRecruitPrototype(companion)) {
            return false;
        }
        if (this.isExplicitRecruitSkinVariant(companion.appearanceModelId) || this.isExplicitRecruitSkinVariant(companion.savedModelAssetId)) {
            return true;
        }
        if (this.resolveRecruitSkinToneAppearance(companion, companion.appearanceModelId) != null) {
            return true;
        }
        return this.shouldTreatPlayerSkinAsSeedOverlay(companion);
    }

    private PlayerSkin generateSkinFromSeed(long seed) {
        if (seed == 0L) {
            return null;
        }
        try {
            CosmeticsModule cosmeticsModule = CosmeticsModule.get();
            if (cosmeticsModule == null) {
                return null;
            }
            PlayerSkin generated = cosmeticsModule.generateRandomSkin(new Random(seed));
            return this.clonePlayerSkin(generated);
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private PlayerSkin clampToHumanSafeRecruitSkin(PlayerSkin skin, long seed) {
        PlayerSkin clamped;
        PlayerSkin playerSkin = clamped = skin != null ? this.clonePlayerSkin(skin) : new PlayerSkin();
        if (clamped.bodyCharacteristic == null || clamped.bodyCharacteristic.isBlank() || !HUMAN_SAFE_BODY_CHARACTERISTICS.contains(clamped.bodyCharacteristic)) {
            int idx = Math.floorMod(seed, HUMAN_SAFE_BODY_CHARACTERISTICS.size());
            clamped.bodyCharacteristic = HUMAN_SAFE_BODY_CHARACTERISTICS.get(idx);
        }
        clamped.ears = HUMAN_SAFE_EARS;
        if (clamped.face == null || clamped.face.isBlank()) {
            clamped.face = DEFAULT_RECRUIT_FACE;
        }
        if (clamped.mouth == null || clamped.mouth.isBlank() || DISALLOWED_RECRUIT_MOUTHS.contains(clamped.mouth)) {
            clamped.mouth = DEFAULT_RECRUIT_MOUTH;
        }
        return clamped;
    }

    private boolean isHumanSafeRecruitPlayerSkin(PlayerSkin skin) {
        if (skin == null) {
            return false;
        }
        if (skin.bodyCharacteristic == null || !HUMAN_SAFE_BODY_CHARACTERISTICS.contains(skin.bodyCharacteristic)) {
            return false;
        }
        if (skin.ears == null || !HUMAN_SAFE_EARS.equals(skin.ears)) {
            return false;
        }
        return skin.mouth == null || !DISALLOWED_RECRUIT_MOUTHS.contains(skin.mouth);
    }

    private boolean isHumanSafeRecruitPlayerSkinToken(String token) {
        return this.isHumanSafeRecruitPlayerSkin(this.decodePlayerSkinToken(token));
    }

    private boolean isBaseWandererRecruitRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return false;
        }
        String lower = roleName.toLowerCase(Locale.ROOT);
        if (lower.contains("merchant") || lower.contains("guild")) {
            return false;
        }
        return lower.startsWith("fh_companion_recruitable") || lower.startsWith("companion_recruitable");
    }

    private String generateConstrainedRecruitPlayerSkinToken(UUID entityUuid) {
        long seed;
        long l = seed = entityUuid != null ? entityUuid.getMostSignificantBits() ^ entityUuid.getLeastSignificantBits() : 0L;
        if (seed == 0L) {
            seed = 1L;
        }
        PlayerSkin generated = this.generateSkinFromSeed(seed);
        PlayerSkin clamped = this.clampToHumanSafeRecruitSkin(generated, seed);
        return this.encodePlayerSkinToken(clamped);
    }

    private String generatePlayerSkinTokenFromSeed(CompanionRecord companion, String reason) {
        if (companion == null) {
            return null;
        }
        long seed = this.ensureCompanionSkinSeed(companion, "ensureCompanionSkinSeed:" + reason);
        if (seed == 0L) {
            return null;
        }
        PlayerSkin generated = this.generateSkinFromSeed(seed);
        if (generated == null) {
            return null;
        }
        return this.encodePlayerSkinToken(generated);
    }

    private String resolveRecruitSkinToneAppearance(CompanionRecord companion, String preferredAppearance) {
        String lower;
        if (companion == null) {
            return null;
        }
        if (!this.isRecruitableRole(companion.sourceRoleName)) {
            return null;
        }
        String normalized = preferredAppearance;
        if (this.isAppearanceModelToken(normalized)) {
            normalized = this.decodeAppearanceModelToken(normalized);
        }
        if (normalized != null && !normalized.isBlank() && (lower = normalized.toLowerCase(Locale.ROOT)).matches("forestcompanion_random(_merchant|_guild)?_skin[1-4]")) {
            return normalized;
        }
        String family = this.resolveRecruitSkinToneFamily(companion.sourceRoleName, preferredAppearance);
        if (family == null || family.isBlank()) {
            return null;
        }
        long seed = this.ensureCompanionSkinSeed(companion, "resolveRecruitSkinToneAppearance");
        if (seed == 0L) {
            return null;
        }
        return this.buildRecruitSkinToneAppearance(family, seed);
    }

    private String resolveRecruitSkinToneFamily(String roleName, String preferredAppearance) {
        String normalized = preferredAppearance;
        if (this.isAppearanceModelToken(normalized)) {
            normalized = this.decodeAppearanceModelToken(normalized);
        }
        if (normalized != null && !normalized.isBlank()) {
            String lower = normalized.toLowerCase(Locale.ROOT);
            if (lower.startsWith("forestcompanion_random_merchant_skin") || lower.equals("forestcompanion_random_merchant")) {
                return "ForestCompanion_Random_Merchant";
            }
            if (lower.startsWith("forestcompanion_random_guild_skin") || lower.equals("forestcompanion_random_guild")) {
                return "ForestCompanion_Random_Guild";
            }
            if (lower.startsWith("forestcompanion_random_skin") || lower.equals("forestcompanion_random")) {
                return "ForestCompanion_Random";
            }
        }
        if (roleName == null || roleName.isBlank()) {
            return null;
        }
        String lowerRole = roleName.toLowerCase(Locale.ROOT);
        if (lowerRole.contains("merchant")) {
            return "ForestCompanion_Random_Merchant";
        }
        if (lowerRole.contains("guild")) {
            return "ForestCompanion_Random_Guild";
        }
        if (this.isBaseWandererRecruitRole(roleName)) {
            return "ForestCompanion_Random";
        }
        return null;
    }

    private String buildRecruitSkinToneAppearance(String family, long seed) {
        if (family == null || family.isBlank()) {
            return null;
        }
        int skinIndex = (int)Math.floorMod(seed, 4L) + 1;
        return family + "_Skin" + skinIndex;
    }

    private long deriveEntitySkinSeed(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        if (entityRef == null || store == null) {
            return 0L;
        }
        try {
            UUIDComponent uuidComponent = (UUIDComponent)store.getComponent(entityRef, UUIDComponent.getComponentType());
            if (uuidComponent != null && uuidComponent.getUuid() != null) {
                long seed = uuidComponent.getUuid().getMostSignificantBits() ^ uuidComponent.getUuid().getLeastSignificantBits();
                return seed != 0L ? seed : 1L;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return 0L;
    }

    private PlayerSkin decodePlayerSkinToken(String token) {
        if (!this.isPlayerSkinToken(token)) {
            return null;
        }
        try {
            String body = token.substring(PLAYER_SKIN_TOKEN_PREFIX.length());
            String[] p = this.splitTokenParts(body, 20);
            PlayerSkin skin = new PlayerSkin();
            skin.bodyCharacteristic = p[0];
            skin.underwear = p[1];
            skin.face = p[2];
            skin.eyes = p[3];
            skin.ears = p[4];
            skin.mouth = p[5];
            skin.facialHair = p[6];
            skin.haircut = p[7];
            skin.eyebrows = p[8];
            skin.pants = p[9];
            skin.overpants = p[10];
            skin.undertop = p[11];
            skin.overtop = p[12];
            skin.shoes = p[13];
            skin.headAccessory = p[14];
            skin.faceAccessory = p[15];
            skin.earAccessory = p[16];
            skin.skinFeature = p[17];
            skin.gloves = p[18];
            skin.cape = p[19];
            return skin;
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private Model createPlayerSkinModel(PlayerSkin skin) {
        if (skin == null) {
            return null;
        }
        try {
            CosmeticsModule cosmeticsModule = CosmeticsModule.get();
            if (cosmeticsModule == null) {
                return null;
            }
            return cosmeticsModule.createModel(skin);
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private boolean shouldUseSeededRecruitPlayerSkin(CompanionRecord companion, String preferredAppearance) {
        return false;
    }

    private boolean shouldIgnoreRecruitPlayerSkinToken(CompanionRecord companion, String appearanceId) {
        if (companion == null) {
            return false;
        }
        if (!this.isRecruitableRole(companion.sourceRoleName)) {
            return false;
        }
        if (!this.isPlayerSkinToken(appearanceId)) {
            return false;
        }
        return !this.isHumanSafeRecruitPlayerSkinToken(appearanceId);
    }

    private PlayerSkin buildSkinOverlay(PlayerSkin baseSkin, PlayerSkin generatedSkin) {
        if (generatedSkin == null || generatedSkin.bodyCharacteristic == null || generatedSkin.bodyCharacteristic.isBlank()) {
            return baseSkin != null ? this.clonePlayerSkin(baseSkin) : null;
        }
        PlayerSkin merged = baseSkin != null ? this.clonePlayerSkin(baseSkin) : new PlayerSkin();
        merged.bodyCharacteristic = generatedSkin.bodyCharacteristic;
        if (generatedSkin.face != null && !generatedSkin.face.isBlank()) {
            merged.face = generatedSkin.face;
        }
        if (generatedSkin.ears != null && !generatedSkin.ears.isBlank()) {
            merged.ears = generatedSkin.ears;
        }
        if (generatedSkin.mouth != null && !generatedSkin.mouth.isBlank()) {
            merged.mouth = generatedSkin.mouth;
        }
        if (generatedSkin.skinFeature != null && !generatedSkin.skinFeature.isBlank()) {
            merged.skinFeature = generatedSkin.skinFeature;
        }
        return merged;
    }

    private void markSkinComponentDirty(PlayerSkinComponent component) {
        if (component == null) {
            return;
        }
        try {
            Method method = component.getClass().getMethod("setNetworkOutdated", new Class[0]);
            method.invoke((Object)component, new Object[0]);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void applyPersistentSkinOverlay(CompanionRecord companion, Ref<EntityStore> ref, Store<EntityStore> store, String reason) {
        if (companion == null || ref == null || store == null) {
            return;
        }
        if (this.hasExplicitPlayerSkinAppearance(companion)) {
            return;
        }
        if (this.resolveRecruitSkinToneAppearance(companion, companion.appearanceModelId) != null && !this.isExplicitRecruitSkinVariant(companion.appearanceModelId)) {
            return;
        }
        long seed = this.ensureCompanionSkinSeed(companion, "ensureCompanionSkinSeed:" + reason);
        if (seed == 0L) {
            return;
        }
        PlayerSkin generated = this.generateSkinFromSeed(seed);
        if (generated == null) {
            return;
        }
        PlayerSkin baseSkin = null;
        try {
            PlayerSkinComponent existing = (PlayerSkinComponent)store.getComponent(ref, PlayerSkinComponent.getComponentType());
            if (existing != null && existing.getPlayerSkin() != null) {
                baseSkin = existing.getPlayerSkin();
            }
        }
        catch (Throwable existing) {
            // empty catch block
        }
        PlayerSkin merged = this.buildSkinOverlay(baseSkin, generated);
        if (merged == null) {
            return;
        }
        try {
            PlayerSkinComponent component = new PlayerSkinComponent(merged);
            this.markSkinComponentDirty(component);
            store.putComponent(ref, PlayerSkinComponent.getComponentType(), (Component)component);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private boolean clearFollowTargetChannels(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return false;
        }
        boolean clearedAny = false;
        clearedAny |= this.clearTargetChannelOnNpcEntity(npcEntity, "LockedTarget");
        clearedAny |= this.clearTargetChannelOnNpcEntity(npcEntity, "Target");
        return clearedAny |= this.clearDirectFlockTargetOnNpcEntity(npcEntity);
    }

    private void enforceMiningFollowIsolation(NPCEntity npcEntity, CompanionRecord companion, CompanionRuntimeState runtime) {
        if (npcEntity == null || companion == null || runtime == null) {
            return;
        }
        if (companion.mode != CompanionMode.MINER || !runtime.miningActive) {
            return;
        }
        this.clearFollowTargetChannels(npcEntity);
        runtime.followBootstrapTicks = 0;
        runtime.wasHolding = false;
    }

    private void recoverFollowAfterCombatClear(NPCEntity npcEntity, CompanionRecord companion, PlayerRef playerRef, Ref<EntityStore> companionRef, Store<EntityStore> store, CompanionRuntimeState runtime) {
        if (npcEntity == null || companion == null || playerRef == null || companionRef == null || store == null || runtime == null) {
            return;
        }
        try {
            this.ensureManagedRoleForCompanion(npcEntity, companion, companionRef, store);
            runtime.combatTargetId = null;
            runtime.stuckTicks = 0;
            runtime.followBootstrapTicks = 4;
            this.clearAllCombatTargetChannels(npcEntity);
            this.ensureNonIdleState(npcEntity, companionRef, store, runtime);
            this.setFollowTarget(npcEntity, playerRef, companionRef, store);
            try {
                npcEntity.getRole().getWorldSupport().requestNewPath();
            }
            catch (Throwable throwable) {}
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private boolean clearAllCombatTargetChannels(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return false;
        }
        boolean clearedAny = false;
        clearedAny |= this.clearTargetChannelOnNpcEntity(npcEntity, "CombatTarget");
        clearedAny |= this.clearTargetChannelOnNpcEntity(npcEntity, "CombatTargetRanged");
        clearedAny |= this.clearTargetChannelOnNpcEntity(npcEntity, "CombatTargetAxe");
        clearedAny |= this.clearTargetChannelOnNpcEntity(npcEntity, "CombatTargetSpear");
        clearedAny |= this.clearTargetChannelOnNpcEntity(npcEntity, "CombatTargetBow");
        clearedAny |= this.clearTargetChannelOnNpcEntity(npcEntity, "CombatTargetCrossbow");
        clearedAny |= this.clearTargetChannelOnNpcEntity(npcEntity, "CombatTargetGun");
        return clearedAny |= this.clearTargetChannelOnNpcEntity(npcEntity, "CombatTargetShieldBlock");
    }

    private boolean setLockedTargetOnNpcEntity(NPCEntity npcEntity, Object targetRefOrEntity) {
        return this.setTargetChannelOnNpcEntity(npcEntity, "LockedTarget", targetRefOrEntity);
    }

    private boolean setTargetChannelOnNpcEntity(NPCEntity npcEntity, String channel, Object targetRefOrEntity) {
        String[] methodNames;
        block17: {
            block16: {
                if (npcEntity == null || channel == null || targetRefOrEntity == null) {
                    return false;
                }
                try {
                    Ref anyRef;
                    if (!(targetRefOrEntity instanceof Ref)) break block16;
                    Ref entityRef = anyRef = (Ref)targetRefOrEntity;
                    try {
                        WorldSupport worldSupport = npcEntity.getRole().getWorldSupport();
                        if (worldSupport != null) {
                            for (Method method : worldSupport.getClass().getMethods()) {
                                int params;
                                if (!"setMarkedEntity".equals(method.getName()) || (params = method.getParameterCount()) != 2) continue;
                                method.invoke((Object)worldSupport, channel, entityRef);
                                return true;
                            }
                        }
                    }
                    catch (Throwable worldSupport) {
                    }
                }
                catch (Throwable anyRef) {
                    // empty catch block
                }
            }
            try {
                MarkedEntitySupport marked = npcEntity.getRole().getMarkedEntitySupport();
                if (marked == null) break block17;
                try {
                    if (targetRefOrEntity instanceof Ref) {
                        Ref anyRef;
                        Ref entityRef = anyRef = (Ref)targetRefOrEntity;
                        marked.setMarkedEntity(channel, entityRef);
                        return true;
                    }
                }
                catch (Throwable throwable) {}
            }
            catch (Throwable marked) {
                // empty catch block
            }
        }
        for (String methodName : methodNames = new String[]{"onFlockSetTarget", "onFlockSetMarkedTarget", "onFlockSetMarkedEntity", "setMarkedEntity"}) {
            try {
                for (Method method : npcEntity.getClass().getMethods()) {
                    int params;
                    if (!method.getName().equals(methodName) || (params = method.getParameterCount()) != 2) continue;
                    method.invoke((Object)npcEntity, channel, targetRefOrEntity);
                    return true;
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return false;
    }

    private boolean clearTargetChannelOnNpcEntity(NPCEntity npcEntity, String channel) {
        if (npcEntity == null || channel == null) {
            return false;
        }
        boolean cleared = false;
        try {
            npcEntity.onFlockSetTarget(channel, null);
            cleared = true;
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            MarkedEntitySupport marked = npcEntity.getRole().getMarkedEntitySupport();
            if (marked != null) {
                marked.setMarkedEntity(channel, null);
                cleared = true;
            }
        }
        catch (Throwable marked) {
            // empty catch block
        }
        try {
            WorldSupport worldSupport = npcEntity.getRole().getWorldSupport();
            if (worldSupport != null) {
                for (Method method : worldSupport.getClass().getMethods()) {
                    if (!"setMarkedEntity".equals(method.getName()) || method.getParameterCount() != 2) continue;
                    method.invoke((Object)worldSupport, channel, null);
                    cleared = true;
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return cleared;
    }

    private boolean clearDirectFlockTargetOnNpcEntity(NPCEntity npcEntity) {
        String[] methodNames;
        if (npcEntity == null) {
            return false;
        }
        for (String methodName : methodNames = new String[]{"onFlockSetTarget", "onFlockSetMarkedTarget", "onFlockSetMarkedEntity", "setMarkedEntity"}) {
            try {
                for (Method method : npcEntity.getClass().getMethods()) {
                    int params;
                    if (!method.getName().equals(methodName) || (params = method.getParameterCount()) != 1) continue;
                    method.invoke((Object)npcEntity, new Object[]{null});
                    return true;
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return false;
    }

    private boolean setDirectFlockTargetOnNpcEntity(NPCEntity npcEntity, Object targetRefOrEntity) {
        String[] methodNames;
        if (npcEntity == null || targetRefOrEntity == null) {
            return false;
        }
        for (String methodName : methodNames = new String[]{"onFlockSetTarget", "onFlockSetMarkedTarget", "onFlockSetMarkedEntity"}) {
            try {
                for (Method method : npcEntity.getClass().getMethods()) {
                    if (!method.getName().equals(methodName) || method.getParameterCount() != 1) continue;
                    method.invoke((Object)npcEntity, targetRefOrEntity);
                    return true;
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return false;
    }

    private void keepOwnerFriendly(NPCEntity npcEntity, PlayerRef ownerRef) {
        if (npcEntity == null || ownerRef == null) {
            return;
        }
        this.safeOverrideAttitude(npcEntity, (Ref<EntityStore>)ownerRef.getReference(), Attitude.REVERED, 12.0);
    }

    private void keepAllPlayersFriendly(NPCEntity npcEntity, PlayerRef ownerRef) {
        if (npcEntity == null || !this.attitudeOverridesAvailable) {
            return;
        }
        try {
            for (PlayerRef player : Universe.get().getPlayers()) {
                if (player.equals(ownerRef)) continue;
                this.safeOverrideAttitude(npcEntity, (Ref<EntityStore>)player.getReference(), Attitude.FRIENDLY, 12.0);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
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

    private boolean setRoleStateSafe(NPCEntity npcEntity, Ref<EntityStore> selfRef, Store<EntityStore> store, String state, String subState) {
        if (npcEntity == null || selfRef == null || store == null || state == null || subState == null) {
            return false;
        }
        try {
            StateSupport directStateSupport = npcEntity.getRole().getStateSupport();
            if (this.invokeSetStateOnSupport(directStateSupport, selfRef, store, state, subState)) {
                return true;
            }
        }
        catch (Throwable directStateSupport) {
            // empty catch block
        }
        try {
            Object roleIndex = npcEntity.getClass().getMethod("getRoleIndex", new Class[0]).invoke((Object)npcEntity, new Object[0]);
            if (roleIndex == null) {
                return false;
            }
            Object stateSupport = roleIndex.getClass().getMethod("getStateSupport", new Class[0]).invoke(roleIndex, new Object[0]);
            if (stateSupport == null) {
                return false;
            }
            return this.invokeSetStateOnSupport(stateSupport, selfRef, store, state, subState);
        }
        catch (Throwable throwable) {
            return false;
        }
    }

    private boolean invokeSetStateOnSupport(Object stateSupport, Ref<EntityStore> selfRef, Store<EntityStore> store, String state, String subState) {
        if (stateSupport == null) {
            return false;
        }
        for (Method method : stateSupport.getClass().getMethods()) {
            if (!"setState".equals(method.getName())) continue;
            int pc = method.getParameterCount();
            try {
                if (pc == 4) {
                    method.invoke(stateSupport, selfRef, state, subState, store);
                    return true;
                }
                if (pc == 3) {
                    method.invoke(stateSupport, selfRef, state, subState);
                    return true;
                }
                if (pc != 2) continue;
                method.invoke(stateSupport, state, subState);
                return true;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return false;
    }

    private boolean applyRecordedEquipmentToNpc(NPCEntity npcEntity, CompanionRecord companion) {
        if (npcEntity == null || companion == null) {
            return false;
        }
        try {
            CombinedItemContainer hotbar;
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return false;
            }
            boolean applied = false;
            String weapon = companion.getEquipped(EquipmentSlot.WEAPON);
            if (weapon != null && EquipmentSlot.isTwoHanded(weapon) && companion.getEquipped(EquipmentSlot.OFFHAND) != null) {
                companion.setEquipped(EquipmentSlot.OFFHAND, null);
            }
            if ((hotbar = inventory.getCombinedHotbarFirst()) != null) {
                if (weapon != null && !weapon.isBlank()) {
                    hotbar.setItemStackForSlot((short)0, new ItemStack(weapon, 1));
                    applied = true;
                } else {
                    hotbar.setItemStackForSlot((short)0, ItemStack.EMPTY);
                }
            }
            String offhand = companion.getEquipped(EquipmentSlot.OFFHAND);
            ItemContainer utility = inventory.getUtility();
            if (utility != null) {
                if (offhand != null && !offhand.isBlank()) {
                    utility.setItemStackForSlot((short)0, new ItemStack(offhand, 1));
                    try {
                        inventory.setActiveUtilitySlot((byte)0);
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                    applied = true;
                } else {
                    utility.setItemStackForSlot((short)0, ItemStack.EMPTY);
                    try {
                        inventory.setActiveUtilitySlot((byte)0);
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                }
            }
            this.clearAllArmorSlots(inventory);
            applied |= this.setArmorFromRecord(inventory, companion, EquipmentSlot.HELMET, this.armorIndexFor(companion, EquipmentSlot.HELMET));
            applied |= this.setArmorFromRecord(inventory, companion, EquipmentSlot.CHESTPLATE, this.armorIndexFor(companion, EquipmentSlot.CHESTPLATE));
            applied |= this.setArmorFromRecord(inventory, companion, EquipmentSlot.LEGGINGS, this.armorIndexFor(companion, EquipmentSlot.LEGGINGS));
            return applied |= this.setArmorFromRecord(inventory, companion, EquipmentSlot.BOOTS, this.armorIndexFor(companion, EquipmentSlot.BOOTS));
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to apply recorded equipment.");
            return false;
        }
    }

    private boolean setArmorFromRecord(Inventory inventory, CompanionRecord companion, EquipmentSlot slot, short armorIndex) {
        String assetId = companion.getEquipped(slot);
        ItemContainer armor = inventory.getArmor();
        if (armor == null) {
            return false;
        }
        if (assetId == null || assetId.isBlank()) {
            armor.setItemStackForSlot(armorIndex, ItemStack.EMPTY);
            return false;
        }
        armor.setItemStackForSlot(armorIndex, new ItemStack(assetId, 1));
        return true;
    }

    private short armorIndexFor(CompanionRecord companion, EquipmentSlot slot) {
        Short runtime;
        if (companion != null && (runtime = this.resolveArmorIndexFromItem(companion.getEquipped(slot))) != null) {
            return runtime;
        }
        return this.fallbackArmorIndex(slot);
    }

    private Short resolveArmorIndexFromItem(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return null;
        }
        try {
            ItemStack probe = new ItemStack(assetId, 1);
            if (probe.getItem() == null || probe.getItem().getArmor() == null || probe.getItem().getArmor().getArmorSlot() == null) {
                return null;
            }
            return (short)probe.getItem().getArmor().getArmorSlot().getValue();
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private void syncFarmHeldItemVisual(CompanionRecord companion, CompanionRuntimeState runtime, NPCEntity npcEntity) {
        if (runtime == null || npcEntity == null || companion == null) {
            return;
        }
        String requiredToolId = this.resolveFarmVisualToolId(companion, runtime);
        if (requiredToolId != null) {
            if (runtime.farmHeldVisualApplied && requiredToolId.equals(runtime.farmHeldVisualToolId)) {
                this.ensureActiveHotbarSlotZeroForVisual(npcEntity);
                return;
            }
            if (runtime.farmHeldVisualApplied) {
                this.restoreHeldItemAfterFarm(runtime, npcEntity, companion);
            }
            this.captureCurrentHeldItemForFarm(runtime, npcEntity);
            try {
                runtime.farmHeldVisualApplied = this.equipFarmToolVisual(npcEntity, runtime, requiredToolId);
                runtime.farmHeldVisualToolId = runtime.farmHeldVisualApplied ? requiredToolId : null;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            return;
        }
        if (runtime.farmHeldVisualApplied) {
            this.restoreHeldItemAfterFarm(runtime, npcEntity, companion);
        }
    }

    private String resolveFarmVisualToolId(CompanionRecord companion, CompanionRuntimeState runtime) {
        if (companion == null || runtime == null) {
            return null;
        }
        if (companion.mode != CompanionMode.FARMER || !runtime.commandActive) {
            return null;
        }
        String taskType = runtime.farmTaskType;
        if ("TILL".equals(taskType)) {
            return FARM_VISUAL_HOE_ID;
        }
        if ("HARVEST".equals(taskType)) {
            return FARM_VISUAL_SICKLE_ID;
        }
        if ("WATER".equals(taskType)) {
            return FARM_VISUAL_WATERING_CAN_ID;
        }
        return null;
    }

    private void captureCurrentHeldItemForFarm(CompanionRuntimeState runtime, NPCEntity npcEntity) {
        if (runtime == null || npcEntity == null) {
            return;
        }
        runtime.farmHeldPreviousItemId = null;
        runtime.farmHeldPreviousQuantity = 0;
        runtime.farmHeldVisualSynthetic = false;
        runtime.farmHeldVisualSlotZeroWasTool = false;
        try {
            CombinedItemContainer hotbar;
            Inventory inventory = npcEntity.getInventory();
            CombinedItemContainer combinedItemContainer = hotbar = inventory != null ? inventory.getCombinedHotbarFirst() : null;
            if (hotbar == null) {
                return;
            }
            ItemStack current = hotbar.getItemStack((short)0);
            if (current == null || current.isEmpty()) {
                return;
            }
            if (this.isFarmVisualTool(current)) {
                this.clearLeakedFarmVisualTools(npcEntity);
                return;
            }
            runtime.farmHeldPreviousItemId = current.getItemId();
            runtime.farmHeldPreviousQuantity = Math.max(1, current.getQuantity());
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void restoreHeldItemAfterFarm(CompanionRuntimeState runtime, NPCEntity npcEntity, CompanionRecord companion) {
        if (runtime == null || npcEntity == null) {
            return;
        }
        try {
            CombinedItemContainer hotbar;
            Inventory inventory = npcEntity.getInventory();
            CombinedItemContainer combinedItemContainer = hotbar = inventory != null ? inventory.getCombinedHotbarFirst() : null;
            if (hotbar != null) {
                if (!runtime.farmHeldVisualSlotZeroWasTool) {
                    if (runtime.farmHeldVisualSynthetic) {
                        this.clearSyntheticFarmToolFromInventory(npcEntity, runtime.farmHeldVisualToolId);
                    } else {
                        this.moveSlotZeroItemToOtherHotbarSlot((ItemContainer)hotbar);
                    }
                }
                if (runtime.farmHeldPreviousItemId != null && !runtime.farmHeldPreviousItemId.isBlank()) {
                    hotbar.setItemStackForSlot((short)0, new ItemStack(runtime.farmHeldPreviousItemId, Math.max(1, runtime.farmHeldPreviousQuantity)));
                } else if (!runtime.farmHeldVisualSlotZeroWasTool) {
                    this.applyRecordedEquipmentToNpc(npcEntity, companion);
                }
            } else {
                this.applyRecordedEquipmentToNpc(npcEntity, companion);
            }
        }
        catch (Throwable ignored) {
            try {
                this.applyRecordedEquipmentToNpc(npcEntity, companion);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        finally {
            runtime.farmHeldVisualApplied = false;
            runtime.farmHeldVisualToolId = null;
            runtime.farmHeldPreviousItemId = null;
            runtime.farmHeldPreviousQuantity = 0;
            runtime.farmHeldVisualSynthetic = false;
            runtime.farmHeldVisualSlotZeroWasTool = false;
        }
    }

    private boolean equipFarmToolVisual(NPCEntity npcEntity, CompanionRuntimeState runtime, String toolItemId) {
        if (npcEntity == null || runtime == null || toolItemId == null || toolItemId.isBlank()) {
            return false;
        }
        try {
            CombinedItemContainer hotbar;
            Inventory inventory = npcEntity.getInventory();
            CombinedItemContainer combinedItemContainer = hotbar = inventory != null ? inventory.getCombinedHotbarFirst() : null;
            if (hotbar == null) {
                return false;
            }
            this.clearLeakedFarmVisualTools(npcEntity);
            runtime.farmHeldVisualSynthetic = true;
            hotbar.setItemStackForSlot((short)0, new ItemStack(toolItemId, 1));
            this.ensureActiveHotbarSlotZeroForVisual(npcEntity);
            return true;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private void clearSyntheticFarmToolFromInventory(NPCEntity npcEntity, String toolItemId) {
        if (npcEntity == null || toolItemId == null || toolItemId.isBlank()) {
            return;
        }
        try {
            for (ItemContainer container : InvUtil.getCompanionContainersForWrite(npcEntity)) {
                if (container == null) continue;
                container.forEach((slot, itemStack) -> {
                    if (this.isItemId((ItemStack)itemStack, toolItemId)) {
                        container.setItemStackForSlot(slot, ItemStack.EMPTY);
                    }
                });
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void clearLeakedFarmVisualTools(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return;
        }
        try {
            for (ItemContainer container : InvUtil.getCompanionContainersForWrite(npcEntity)) {
                if (container == null) continue;
                container.forEach((slot, itemStack) -> {
                    if (!this.isFarmVisualTool((ItemStack)itemStack)) {
                        return;
                    }
                    container.setItemStackForSlot(slot, ItemStack.EMPTY);
                });
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private boolean isFarmVisualTool(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItemId() == null) {
            return false;
        }
        return FARM_VISUAL_TOOL_IDS.contains(stack.getItemId().toLowerCase(Locale.ROOT));
    }

    private boolean isItemId(ItemStack stack, String itemId) {
        return stack != null && !stack.isEmpty() && stack.getItemId() != null && stack.getItemId().equalsIgnoreCase(itemId);
    }

    private void syncMiningHeldItemVisual(CompanionRecord companion, CompanionRuntimeState runtime, NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store) {
        boolean shouldShowMiningTool;
        if (runtime == null || npcEntity == null || companion == null) {
            return;
        }
        boolean bl = shouldShowMiningTool = companion.mode == CompanionMode.MINER && runtime.miningActive;
        if (shouldShowMiningTool) {
            if (!runtime.miningHeldVisualApplied) {
                this.captureCurrentHeldItemForMining(runtime, npcEntity);
                try {
                    runtime.miningHeldVisualApplied = this.equipMiningPickaxeVisual(npcEntity);
                }
                catch (Throwable throwable) {}
            } else {
                this.ensureActiveHotbarSlotZeroForVisual(npcEntity);
            }
            return;
        }
        if (runtime.miningHeldVisualApplied) {
            this.restoreHeldItemAfterMining(runtime, npcEntity, companion);
        }
    }

    private void captureCurrentHeldItemForMining(CompanionRuntimeState runtime, NPCEntity npcEntity) {
        if (runtime == null || npcEntity == null) {
            return;
        }
        runtime.miningHeldPreviousItemId = null;
        runtime.miningHeldPreviousQuantity = 0;
        try {
            CombinedItemContainer hotbar;
            Inventory inventory = npcEntity.getInventory();
            CombinedItemContainer combinedItemContainer = hotbar = inventory != null ? inventory.getCombinedHotbarFirst() : null;
            if (hotbar == null) {
                return;
            }
            ItemStack current = hotbar.getItemStack((short)0);
            if (current == null || current.isEmpty()) {
                return;
            }
            runtime.miningHeldPreviousItemId = current.getItemId();
            runtime.miningHeldPreviousQuantity = Math.max(1, current.getQuantity());
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void restoreHeldItemAfterMining(CompanionRuntimeState runtime, NPCEntity npcEntity, CompanionRecord companion) {
        if (runtime == null || npcEntity == null) {
            return;
        }
        try {
            CombinedItemContainer hotbar;
            Inventory inventory = npcEntity.getInventory();
            CombinedItemContainer combinedItemContainer = hotbar = inventory != null ? inventory.getCombinedHotbarFirst() : null;
            if (hotbar != null) {
                this.moveSlotZeroItemToOtherHotbarSlot((ItemContainer)hotbar);
                if (runtime.miningHeldPreviousItemId != null && !runtime.miningHeldPreviousItemId.isBlank()) {
                    hotbar.setItemStackForSlot((short)0, new ItemStack(runtime.miningHeldPreviousItemId, Math.max(1, runtime.miningHeldPreviousQuantity)));
                } else {
                    this.applyRecordedEquipmentToNpc(npcEntity, companion);
                }
            } else {
                this.applyRecordedEquipmentToNpc(npcEntity, companion);
            }
        }
        catch (Throwable ignored) {
            try {
                this.applyRecordedEquipmentToNpc(npcEntity, companion);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        finally {
            runtime.miningHeldVisualApplied = false;
            runtime.miningHeldPreviousItemId = null;
            runtime.miningHeldPreviousQuantity = 0;
        }
    }

    private boolean equipMiningPickaxeVisual(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return false;
        }
        try {
            CombinedItemContainer hotbar;
            Inventory inventory = npcEntity.getInventory();
            CombinedItemContainer combinedItemContainer = hotbar = inventory != null ? inventory.getCombinedHotbarFirst() : null;
            if (hotbar == null) {
                return false;
            }
            ItemStack slotZero = hotbar.getItemStack((short)0);
            if (slotZero != null && !slotZero.isEmpty() && InvUtil.getPickaxeTier(slotZero.getItemId()) >= 0) {
                this.ensureActiveHotbarSlotZeroForVisual(npcEntity);
                return true;
            }
            ItemContainer[] bestContainer = new ItemContainer[]{null};
            short[] bestSlot = new short[]{-1};
            ItemStack[] bestStack = new ItemStack[]{null};
            int[] bestTier = new int[]{-1};
            for (ItemContainer container : InvUtil.getCompanionContainersForWrite(npcEntity)) {
                if (container == null) continue;
                container.forEach((slot, itemStack) -> {
                    if (itemStack == null || itemStack.isEmpty()) {
                        return;
                    }
                    String itemId = itemStack.getItemId();
                    int tier = InvUtil.getPickaxeTier(itemId);
                    if (tier < 0) {
                        return;
                    }
                    if (container == hotbar && slot == 0) {
                        return;
                    }
                    if (tier > bestTier[0]) {
                        bestTier[0] = tier;
                        bestContainer[0] = container;
                        bestSlot[0] = slot;
                        bestStack[0] = itemStack;
                    }
                });
            }
            if (bestContainer[0] == null || bestSlot[0] < 0 || bestStack[0] == null || bestStack[0].isEmpty()) {
                hotbar.setItemStackForSlot((short)0, new ItemStack(MINING_VISUAL_PICKAXE_ID, 1));
                this.ensureActiveHotbarSlotZeroForVisual(npcEntity);
                return true;
            }
            if (bestContainer[0] == hotbar) {
                hotbar.setItemStackForSlot((short)0, bestStack[0]);
                hotbar.setItemStackForSlot(bestSlot[0], slotZero != null ? slotZero : ItemStack.EMPTY);
            } else {
                bestContainer[0].setItemStackForSlot(bestSlot[0], slotZero != null ? slotZero : ItemStack.EMPTY);
                hotbar.setItemStackForSlot((short)0, bestStack[0]);
            }
            this.ensureActiveHotbarSlotZeroForVisual(npcEntity);
            return true;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private void ensureActiveHotbarSlotZeroForVisual(NPCEntity npcEntity) {
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

    private void moveSlotZeroItemToOtherHotbarSlot(ItemContainer hotbar) {
        if (hotbar == null) {
            return;
        }
        try {
            ItemStack current = hotbar.getItemStack((short)0);
            if (current == null || current.isEmpty()) {
                return;
            }
            short[] emptySlot = new short[]{-1};
            short[] matchingSlot = new short[]{-1};
            hotbar.forEach((slot, itemStack) -> {
                if (slot == 0) {
                    return;
                }
                if (matchingSlot[0] < 0 && itemStack != null && !itemStack.isEmpty() && itemStack.getItemId() != null && itemStack.getItemId().equalsIgnoreCase(current.getItemId())) {
                    matchingSlot[0] = slot;
                    return;
                }
                if (emptySlot[0] < 0 && (itemStack == null || itemStack.isEmpty())) {
                    emptySlot[0] = slot;
                }
            });
            if (matchingSlot[0] >= 0) {
                ItemStack target = hotbar.getItemStack(matchingSlot[0]);
                if (target != null && !target.isEmpty()) {
                    hotbar.setItemStackForSlot(matchingSlot[0], target.withQuantity(target.getQuantity() + Math.max(1, current.getQuantity())));
                    hotbar.setItemStackForSlot((short)0, ItemStack.EMPTY);
                }
                return;
            }
            if (emptySlot[0] >= 0) {
                hotbar.setItemStackForSlot(emptySlot[0], current);
                hotbar.setItemStackForSlot((short)0, ItemStack.EMPTY);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void preserveFallbackHotbarSlotZeroItem(ItemContainer hotbar) {
        if (hotbar == null) {
            return;
        }
        try {
            ItemStack current = hotbar.getItemStack((short)0);
            if (!this.isFallbackNonWeapon(current)) {
                return;
            }
            short[] emptySlot = new short[]{-1};
            short[] matchingSlot = new short[]{-1};
            hotbar.forEach((slot, itemStack) -> {
                if (slot == 0) {
                    return;
                }
                if (emptySlot[0] >= 0 && matchingSlot[0] >= 0) {
                    return;
                }
                if (itemStack == null || itemStack.isEmpty()) {
                    if (emptySlot[0] < 0) {
                        emptySlot[0] = slot;
                    }
                    return;
                }
                if (matchingSlot[0] < 0 && current.getItemId() != null && current.getItemId().equals(itemStack.getItemId())) {
                    matchingSlot[0] = slot;
                }
            });
            if (matchingSlot[0] >= 0) {
                ItemStack target = hotbar.getItemStack(matchingSlot[0]);
                if (target != null && !target.isEmpty()) {
                    hotbar.setItemStackForSlot(matchingSlot[0], target.withQuantity(target.getQuantity() + Math.max(1, current.getQuantity())));
                    hotbar.setItemStackForSlot((short)0, ItemStack.EMPTY);
                }
                return;
            }
            if (emptySlot[0] >= 0) {
                hotbar.setItemStackForSlot(emptySlot[0], current);
                hotbar.setItemStackForSlot((short)0, ItemStack.EMPTY);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private boolean isFallbackNonWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            return stack.getItem() == null || stack.getItem().getWeapon() == null;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private short fallbackArmorIndex(EquipmentSlot slot) {
        return switch (slot) {
            case EquipmentSlot.HELMET -> 0;
            case EquipmentSlot.CHESTPLATE -> 1;
            case EquipmentSlot.BOOTS -> 2;
            case EquipmentSlot.LEGGINGS -> 3;
            default -> 0;
        };
    }

    private void clearAllArmorSlots(Inventory inventory) {
        try {
            ItemContainer armor = inventory.getArmor();
            if (armor == null) {
                return;
            }
            for (short i = 0; i < 4; i = (short)(i + 1)) {
                this.clearArmorSlotWithFallbacks(armor, i);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void clearArmorSlotWithFallbacks(ItemContainer armor, short slotIndex) {
        ItemStack after2;
        try {
            armor.setItemStackForSlot(slotIndex, ItemStack.EMPTY);
            after2 = armor.getItemStack(slotIndex);
            if (after2 == null || after2.isEmpty() || after2.getQuantity() <= 0) {
                return;
            }
        }
        catch (Throwable after2) {
            // empty catch block
        }
        try {
            armor.setItemStackForSlot(slotIndex, null);
            after2 = armor.getItemStack(slotIndex);
            if (after2 == null || after2.isEmpty() || after2.getQuantity() <= 0) {
                return;
            }
        }
        catch (Throwable after3) {
            // empty catch block
        }
        try {
            ItemStack current = armor.getItemStack(slotIndex);
            if (current != null && !current.isEmpty()) {
                armor.setItemStackForSlot(slotIndex, current.withQuantity(0));
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void saveCompanionInventory(Store<EntityStore> store, Ref<EntityStore> ref, CompanionRecord companion) {
        try {
            NPCEntity npcEntity = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
            if (npcEntity != null && npcEntity.getInventory() != null) {
                String currentAppearance = this.captureBestAppearanceToken(npcEntity, ref, store);
                if (this.isPersistableAppearanceId(currentAppearance)) {
                    boolean knownCustomBefore;
                    boolean recruitOrigin = this.isRecruitableRole(companion.sourceRoleName);
                    boolean unknownBefore = companion.appearanceModelId == null || companion.appearanceModelId.isBlank();
                    boolean fallbackNow = this.isPlayerTestFallbackAppearance(currentAppearance);
                    boolean genericRecruitRandomNow = this.isGenericRecruitRandomAppearance(currentAppearance);
                    boolean seededOverlayNow = this.shouldRejectRecruitPlayerSkinCapture(companion, currentAppearance);
                    boolean bl = knownCustomBefore = !unknownBefore && !this.isPlayerTestFallbackAppearance(companion.appearanceModelId);
                    if (!(recruitOrigin && (fallbackNow && (unknownBefore || knownCustomBefore) || genericRecruitRandomNow && knownCustomBefore || seededOverlayNow && knownCustomBefore))) {
                        this.setAppearanceModelId(companion, currentAppearance, "fallbackAppearanceCapture");
                    }
                }
                this.syncRecordedEquipmentFromNpc(npcEntity, companion);
                this.snapshotSavedInventoryFromNpc(npcEntity, companion);
                if (!companion.savedInventory.isEmpty()) {
                    this.logger.at(Level.INFO).log("Saved " + companion.savedInventory.size() + " item types from " + companion.getDisplayName() + " on dismiss/death.");
                }
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Could not save companion inventory.");
        }
    }

    private void restoreSavedInventoryToNpc(NPCEntity npcEntity, Ref<EntityStore> ref, Store<EntityStore> store, CompanionRecord companion, String source) {
        if (npcEntity == null || companion == null) {
            return;
        }
        if (companion.savedInventory == null || companion.savedInventory.isEmpty()) {
            return;
        }
        try {
            HashMap<String, Integer> liveInventory = new HashMap<String, Integer>();
            for (ItemContainer container : InvUtil.getCompanionContainersForRead(npcEntity)) {
                this.collectSavedInventory(liveInventory, container);
            }
            int restoredTypes = 0;
            for (Map.Entry<String, Integer> entry : companion.savedInventory.entrySet()) {
                int liveQty;
                String itemId = entry.getKey();
                int savedQty = entry.getValue() != null ? entry.getValue() : 0;
                int missingQty = savedQty - (liveQty = liveInventory.getOrDefault(itemId, 0).intValue());
                if (missingQty <= 0 || !InvUtil.giveItemToCompanion(npcEntity, ref, store, itemId, missingQty, this.logger)) continue;
                ++restoredTypes;
            }
            this.logger.at(Level.INFO).log("Restored " + restoredTypes + " missing item types to " + companion.getDisplayName() + " during " + source + ".");
            companion.savedInventory.clear();
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to restore companion inventory during " + source + ".");
        }
    }

    private void syncRecordedEquipmentFromNpc(NPCEntity npcEntity, CompanionRecord companion) {
        if (npcEntity == null || companion == null) {
            return;
        }
        try {
            ItemStack slot1;
            ItemStack slot0;
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return;
            }
            String weaponId = null;
            CombinedItemContainer hotbar = inventory.getCombinedHotbarFirst();
            if (hotbar != null && (slot0 = hotbar.getItemStack((short)0)) != null && !slot0.isEmpty()) {
                weaponId = slot0.getItemId();
            }
            companion.setEquipped(EquipmentSlot.WEAPON, weaponId);
            String offhandId = null;
            ItemContainer utility = inventory.getUtility();
            if (utility != null && (slot1 = utility.getItemStack((short)0)) != null && !slot1.isEmpty()) {
                offhandId = slot1.getItemId();
            }
            companion.setEquipped(EquipmentSlot.OFFHAND, offhandId);
            ItemContainer armor = inventory.getArmor();
            if (armor != null) {
                companion.setEquipped(EquipmentSlot.HELMET, null);
                companion.setEquipped(EquipmentSlot.CHESTPLATE, null);
                companion.setEquipped(EquipmentSlot.LEGGINGS, null);
                companion.setEquipped(EquipmentSlot.BOOTS, null);
                for (short i = 0; i < 4; i = (short)(i + 1)) {
                    String itemId;
                    EquipmentSlot slot;
                    ItemStack stack = armor.getItemStack(i);
                    if (stack == null || stack.isEmpty() || (slot = EquipmentSlot.forItem(itemId = stack.getItemId())) == null) continue;
                    companion.setEquipped(slot, itemId);
                }
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Failed to sync equipped gear from live NPC.");
        }
    }

    private void collectSavedInventory(Map<String, Integer> out, ItemContainer container) {
        if (out == null || container == null) {
            return;
        }
        container.forEach((slot, itemStack) -> {
            if (itemStack != null && !itemStack.isEmpty()) {
                String itemId = itemStack.getItemId();
                if (!this.isSafeInventoryItemId(itemId)) {
                    return;
                }
                out.merge(itemId, itemStack.getQuantity(), Integer::sum);
            }
        });
    }

    private void collectSavedInventoryStacks(List<String[]> out, ItemContainer container) {
        if (out == null || container == null) {
            return;
        }
        container.forEach((slot, itemStack) -> {
            if (itemStack == null || itemStack.isEmpty()) {
                return;
            }
            String itemId = itemStack.getItemId();
            if (!this.isSafeInventoryItemId(itemId)) {
                return;
            }
            out.add(new String[]{itemId, EquipmentSlot.getItemDisplayName(itemId), Integer.toString(Math.max(1, itemStack.getQuantity()))});
        });
    }

    private void snapshotSavedInventoryFromNpc(NPCEntity npcEntity, CompanionRecord companion) {
        if (npcEntity == null || companion == null) {
            return;
        }
        companion.savedInventory.clear();
        companion.savedInventoryStacks.clear();
        for (ItemContainer container : InvUtil.getCompanionContainersForRead(npcEntity)) {
            this.collectSavedInventory(companion.savedInventory, container);
            this.collectSavedInventoryStacks(companion.savedInventoryStacks, container);
        }
        this.sanitizeSavedInventoryData(companion);
    }

    private void onCompanionDeath(World world, Store<EntityStore> store, Ref<EntityStore> companionRef, PlayerRef ownerRef, CompanionRecord companion, DeathComponent deathComponent) {
        NPCEntity npcEntity = (NPCEntity)store.getComponent(companionRef, NPCEntity.getComponentType());
        try {
            TransformComponent transform = (TransformComponent)store.getComponent(companionRef, TransformComponent.getComponentType());
            if (transform != null) {
                this.updateCompanionLastKnownLocation(companion, ownerRef, transform.getPosition());
            }
        }
        catch (Throwable transform) {
            // empty catch block
        }
        this.cacheAppearanceSnapshot(companion, npcEntity, companionRef, store, "death");
        this.saveCompanionInventory(store, companionRef, companion);
        String deathCause = this.resolveCompanionDeathCause(world, store, companion, deathComponent);
        companion.fallen = true;
        companion.active = false;
        companion.deathCount = Math.max(0, companion.deathCount) + 1;
        companion.deathCause = deathCause;
        companion.deathTime = this.globalTick;
        companion.entityId = null;
        try {
            if (companionRef != null && companionRef.isValid()) {
                store.removeEntity(companionRef, RemoveReason.REMOVE);
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to remove dead companion entity.");
        }
        try {
            if (ownerRef != null) {
                ownerRef.sendMessage(Message.raw((String)("Your companion " + companion.getDisplayName() + " has fallen!")));
                ownerRef.sendMessage(Message.raw((String)"Their progress has been preserved."));
                ownerRef.sendMessage(Message.raw((String)"Use /companion revive to bring them back."));
                ownerRef.sendMessage(Message.raw((String)("Cost: " + ReviveConfig.getReviveCostText())));
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to notify about companion death.");
        }
        this.saveCompanionData("onCompanionDeath", companion);
    }

    private String resolveCompanionDeathCause(World world, Store<EntityStore> store, CompanionRecord companion, DeathComponent deathComponent) {
        String cause = this.extractDeathMessageText(deathComponent);
        if (cause != null && !cause.isBlank() && !"Unknown".equalsIgnoreCase(cause)) {
            return cause;
        }
        Ref<EntityStore> sourceRef = this.resolveDeathSourceRef(deathComponent);
        String sourceName = this.resolveDeathSourceName(store, sourceRef);
        if (sourceName != null && !sourceName.isBlank()) {
            return sourceName;
        }
        String runtimeName = this.resolveDeathSourceNameFromRuntime(world, store, companion);
        if (runtimeName != null && !runtimeName.isBlank()) {
            return runtimeName;
        }
        return "Unknown";
    }

    private String extractDeathMessageText(DeathComponent deathComponent) {
        try {
            String raw;
            Message deathMessage = deathComponent != null ? deathComponent.getDeathMessage() : null;
            String string = raw = deathMessage != null ? deathMessage.getRawText() : null;
            if (raw == null) {
                return null;
            }
            return (raw = raw.trim()).isBlank() ? null : raw;
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private Ref<EntityStore> resolveDeathSourceRef(DeathComponent deathComponent) {
        String[] fieldNames;
        String[] methodNames;
        if (deathComponent == null) {
            return null;
        }
        for (String methodName : methodNames = new String[]{"getSourceRef", "getAttackerRef", "getInstigatorRef", "getDamagerRef", "getEntityRef"}) {
            try {
                Ref ref;
                Method method = deathComponent.getClass().getMethod(methodName, new Class[0]);
                Object value = method.invoke((Object)deathComponent, new Object[0]);
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
                Field field = deathComponent.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(deathComponent);
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

    private String resolveDeathSourceName(Store<EntityStore> store, Ref<EntityStore> sourceRef) {
        if (store == null || sourceRef == null || !sourceRef.isValid()) {
            return null;
        }
        try {
            DisplayNameComponent displayName = (DisplayNameComponent)store.getComponent(sourceRef, DisplayNameComponent.getComponentType());
            if (displayName != null) {
                String raw;
                Message message = displayName.getDisplayName();
                String string = raw = message != null ? message.getRawText() : null;
                if (raw != null && !raw.isBlank()) {
                    return raw.trim();
                }
            }
        }
        catch (Throwable displayName) {
            // empty catch block
        }
        try {
            String roleName;
            NPCEntity npc = (NPCEntity)store.getComponent(sourceRef, NPCEntity.getComponentType());
            if (npc != null && (roleName = npc.getRoleName()) != null && !roleName.isBlank()) {
                return roleName;
            }
        }
        catch (Throwable npc) {
            // empty catch block
        }
        try {
            UUIDComponent uuidComponent = (UUIDComponent)store.getComponent(sourceRef, UUIDComponent.getComponentType());
            if (uuidComponent != null && uuidComponent.getUuid() != null) {
                return uuidComponent.getUuid().toString();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    private String resolveDeathSourceNameFromRuntime(World world, Store<EntityStore> store, CompanionRecord companion) {
        Vector3d center;
        if (world == null || store == null || companion == null || companion.uniqueId == null) {
            return null;
        }
        CompanionRuntimeState runtime = this.companionManager.getRuntime(companion.uniqueId);
        if (runtime == null) {
            return null;
        }
        String[] candidateIds = new String[]{runtime.reactiveAttackerId, runtime.combatTargetId};
        BlockPos lastPos = companion.lastKnownLocation;
        Vector3d vector3d = center = lastPos != null ? new Vector3d((double)lastPos.x + 0.5, (double)lastPos.y + 0.5, (double)lastPos.z + 0.5) : null;
        if (center == null) {
            return null;
        }
        List<WorldQueries.NearbyEntity> nearby = WorldQueries.getNearbyLivingEntities(world, center, 32.0);
        for (String candidateId : candidateIds) {
            UUID uuid = this.parseUuid(candidateId);
            if (uuid == null) continue;
            for (WorldQueries.NearbyEntity ne : nearby) {
                try {
                    String name;
                    UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ne.ref, UUIDComponent.getComponentType());
                    if (uuidComp == null || uuidComp.getUuid() == null || !uuid.equals(uuidComp.getUuid()) || (name = this.resolveDeathSourceName(store, ne.ref)) == null || name.isBlank()) continue;
                    return name;
                }
                catch (Throwable throwable) {
                }
            }
        }
        return null;
    }

    public boolean tryRecruitNearbyFromInteract(PlayerRef playerRef) {
        if (playerRef == null) {
            return false;
        }
        UUID worldId = playerRef.getWorldUuid();
        if (worldId == null) {
            return false;
        }
        World world = Universe.get().getWorld(worldId);
        if (world == null) {
            return false;
        }
        Store store = world.getEntityStore().getStore();
        Transform playerTf = playerRef.getTransform();
        Vector3d playerPos = playerTf.getPosition();
        if (playerPos == null) {
            return false;
        }
        Vector3d playerDir = playerTf.getDirection();
        if (playerDir == null) {
            playerDir = new Vector3d(0.0, 0.0, 1.0);
        }
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        HashSet<String> managedEntityIds = new HashSet<String>();
        for (CompanionRecord c : data.companions) {
            if (c.entityId == null) continue;
            managedEntityIds.add(c.entityId);
        }
        List<WorldQueries.NearbyEntity> nearby = WorldQueries.getNearbyLivingEntities(world, playerPos, 8.0);
        WorldQueries.NearbyEntity best = null;
        double bestScore = Double.MAX_VALUE;
        NPCEntity bestNpc = null;
        for (WorldQueries.NearbyEntity ne : nearby) {
            try {
                double score;
                NPCEntity npcEntity;
                UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ne.ref, UUIDComponent.getComponentType());
                if (uuidComp != null && managedEntityIds.contains(uuidComp.getUuid().toString()) || (npcEntity = (NPCEntity)store.getComponent(ne.ref, NPCEntity.getComponentType())) == null || !this.isRecruitableRole(npcEntity.getRoleName()) || ne.distance > 5.0 || Double.isInfinite(score = this.facingRecruitScore(playerPos, playerDir, ne.position, ne.distance)) || !(score < bestScore)) continue;
                best = ne;
                bestScore = score;
                bestNpc = npcEntity;
            }
            catch (Throwable throwable) {}
        }
        if (best == null || bestNpc == null) {
            return false;
        }
        Ref<EntityStore> recruitRef = best.ref;
        world.execute(() -> {
            try {
                Store s = world.getEntityStore().getStore();
                NPCEntity npc = (NPCEntity)s.getComponent(recruitRef, NPCEntity.getComponentType());
                if (npc == null) {
                    return;
                }
                PlayerCompanionData d = this.companionManager.getOrCreate(playerRef.getUuid());
                this.recruitWorldNPC(world, (Store<EntityStore>)s, recruitRef, npc, playerRef, d);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        });
        return true;
    }

    private double facingRecruitScore(Vector3d playerPos, Vector3d playerDir, Vector3d targetPos, double distance) {
        if (playerPos == null || playerDir == null || targetPos == null) {
            return Double.POSITIVE_INFINITY;
        }
        double vx = targetPos.x - playerPos.x;
        double vy = targetPos.y - playerPos.y;
        double vz = targetPos.z - playerPos.z;
        double vLen = Math.sqrt(vx * vx + vy * vy + vz * vz);
        double dLen = Math.sqrt(playerDir.x * playerDir.x + playerDir.y * playerDir.y + playerDir.z * playerDir.z);
        if (vLen < 1.0E-4 || dLen < 1.0E-4) {
            return Double.POSITIVE_INFINITY;
        }
        double dot = (vx * playerDir.x + vy * playerDir.y + vz * playerDir.z) / (vLen * dLen);
        if (dot < 0.55) {
            return Double.POSITIVE_INFINITY;
        }
        double centerPenalty = 1.0 - dot;
        return centerPenalty * 8.0 + distance;
    }

    public boolean tryRecruitTargetFromInteract(PlayerRef playerRef, Ref<EntityStore> targetRef, World world) {
        if (playerRef == null || targetRef == null || world == null) {
            return false;
        }
        Store store = world.getEntityStore().getStore();
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        try {
            NPCEntity npcEntity;
            UUIDComponent uuidComp = (UUIDComponent)store.getComponent(targetRef, UUIDComponent.getComponentType());
            if (uuidComp != null) {
                for (CompanionRecord c : data.companions) {
                    if (c.entityId == null || !c.entityId.equals(uuidComp.getUuid().toString())) continue;
                    return false;
                }
            }
            if ((npcEntity = (NPCEntity)store.getComponent(targetRef, NPCEntity.getComponentType())) == null) {
                return false;
            }
            if (!this.isRecruitableRole(npcEntity.getRoleName())) {
                return false;
            }
            try {
                boolean pressedF;
                StateSupport stateSupport = npcEntity.getRole().getStateSupport();
                boolean bl = pressedF = stateSupport != null && this.consumeInteractionTriggered(stateSupport, playerRef);
                if (!pressedF) {
                    return false;
                }
            }
            catch (Throwable t) {
                return false;
            }
            world.execute(() -> {
                try {
                    Store s = world.getEntityStore().getStore();
                    NPCEntity npc = (NPCEntity)s.getComponent(targetRef, NPCEntity.getComponentType());
                    if (npc == null) {
                        return;
                    }
                    PlayerCompanionData d = this.companionManager.getOrCreate(playerRef.getUuid());
                    this.recruitWorldNPC(world, (Store<EntityStore>)s, targetRef, npc, playerRef, d);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            });
            return true;
        }
        catch (Throwable t) {
            return false;
        }
    }

    public boolean tryRecruitNearestByProximity(PlayerRef playerRef, World world) {
        if (playerRef == null || world == null) {
            return false;
        }
        try {
            Transform playerTf = playerRef.getTransform();
            Vector3d playerPos = playerTf.getPosition();
            if (playerPos == null) {
                return false;
            }
            Vector3d playerDir = playerTf.getDirection();
            if (playerDir == null) {
                playerDir = new Vector3d(0.0, 0.0, 1.0);
            }
            Store store = world.getEntityStore().getStore();
            PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
            HashSet<String> managedEntityIds = new HashSet<String>();
            for (CompanionRecord c : data.companions) {
                if (c.entityId == null) continue;
                managedEntityIds.add(c.entityId);
            }
            List<WorldQueries.NearbyEntity> nearby = WorldQueries.getNearbyLivingEntities(world, playerPos, 8.0);
            WorldQueries.NearbyEntity bestCandidate = null;
            double bestScore = Double.MAX_VALUE;
            NPCEntity bestNpc = null;
            for (WorldQueries.NearbyEntity ne : nearby) {
                try {
                    double score;
                    NPCEntity npcEntity;
                    UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ne.ref, UUIDComponent.getComponentType());
                    if (uuidComp != null && managedEntityIds.contains(uuidComp.getUuid().toString()) || (npcEntity = (NPCEntity)store.getComponent(ne.ref, NPCEntity.getComponentType())) == null || !this.isRecruitableRole(npcEntity.getRoleName()) || ne.distance > 5.0 || Double.isInfinite(score = this.facingRecruitScore(playerPos, playerDir, ne.position, ne.distance)) || !(score < bestScore)) continue;
                    bestScore = score;
                    bestCandidate = ne;
                    bestNpc = npcEntity;
                }
                catch (Throwable throwable) {}
            }
            if (bestCandidate == null) {
                return false;
            }
            WorldQueries.NearbyEntity candidate = bestCandidate;
            world.execute(() -> {
                try {
                    Store s = world.getEntityStore().getStore();
                    NPCEntity npc = (NPCEntity)s.getComponent(candidate.ref, NPCEntity.getComponentType());
                    if (npc == null) {
                        return;
                    }
                    PlayerCompanionData d = this.companionManager.getOrCreate(playerRef.getUuid());
                    this.recruitWorldNPC(world, (Store<EntityStore>)s, candidate.ref, npc, playerRef, d);
                }
                catch (Throwable t) {
                    ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("[CompanionDebug] Proximity recruit failed.");
                }
            });
            return true;
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("[CompanionDebug] tryRecruitNearestByProximity crashed.");
            return false;
        }
    }

    private void safeScanForRecruitableNPCs(World world, PlayerRef playerRef) {
        try {
            this.scanForRecruitableNPCs(world, playerRef);
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("[CompanionDebug] scanForRecruitableNPCs crashed.");
        }
    }

    private void safeRefreshRecruitableThreatResponses(World world, PlayerRef playerRef) {
        try {
            this.refreshRecruitableThreatResponses(world, playerRef);
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Recruitable threat refresh failed.");
        }
    }

    private void scanForRecruitableNPCs(World world, PlayerRef playerRef) {
        if (world == null || playerRef == null) {
            return;
        }
        Transform playerTf = playerRef.getTransform();
        Vector3d playerPos = playerTf.getPosition();
        if (playerPos == null) {
            return;
        }
        Vector3d playerDir = playerTf.getDirection();
        if (playerDir == null) {
            playerDir = new Vector3d(0.0, 0.0, 1.0);
        }
        Store store = world.getEntityStore().getStore();
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        HashSet<String> managedEntityIds = new HashSet<String>();
        for (CompanionRecord c : data.companions) {
            if (c.entityId == null) continue;
            managedEntityIds.add(c.entityId);
        }
        List<WorldQueries.NearbyEntity> nearby = WorldQueries.getNearbyLivingEntities(world, playerPos, 8.0);
        for (WorldQueries.NearbyEntity ne : nearby) {
            try {
                StateSupport stateSupport;
                boolean pressedF;
                double score;
                String roleName;
                NPCEntity npcEntity;
                UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ne.ref, UUIDComponent.getComponentType());
                if (uuidComp != null && managedEntityIds.contains(uuidComp.getUuid().toString()) || (npcEntity = (NPCEntity)store.getComponent(ne.ref, NPCEntity.getComponentType())) == null || !this.isRecruitableRole(roleName = npcEntity.getRoleName()) || ne.distance > 5.0 || Double.isInfinite(score = this.facingRecruitScore(playerPos, playerDir, ne.position, ne.distance)) || !(pressedF = this.consumeInteractionTriggered(stateSupport = npcEntity.getRole().getStateSupport(), playerRef))) continue;
                this.recruitWorldNPC(world, (Store<EntityStore>)store, ne.ref, npcEntity, playerRef, data);
                return;
            }
            catch (Throwable throwable) {
            }
        }
    }

    private void refreshRecruitableThreatResponses(World world, PlayerRef playerRef) {
        Vector3d playerPos;
        long lastInteract;
        if (world == null || playerRef == null) {
            return;
        }
        PlayerRuntimeState playerRuntime = this.companionManager.getPlayerRuntime(playerRef.getUuid());
        if (playerRuntime != null && (lastInteract = playerRuntime.lastRecruitInteractMs) > 0L && System.currentTimeMillis() - lastInteract < 2000L) {
            return;
        }
        Transform playerTf = playerRef.getTransform();
        Vector3d vector3d = playerPos = playerTf != null ? playerTf.getPosition() : null;
        if (playerPos == null) {
            return;
        }
        Store store = world.getEntityStore().getStore();
        PlayerCompanionData data = this.companionManager.get(playerRef.getUuid());
        HashSet<String> managedEntityIds = new HashSet<String>();
        if (data != null && data.companions != null) {
            for (CompanionRecord companion : data.companions) {
                if (companion == null || companion.entityId == null || companion.entityId.isBlank()) continue;
                managedEntityIds.add(companion.entityId);
            }
        }
        List<WorldQueries.NearbyEntity> nearby = WorldQueries.getNearbyLivingEntities(world, playerPos, 8.0);
        for (WorldQueries.NearbyEntity ne : nearby) {
            try {
                NPCEntity hostileNpc;
                Ref<EntityStore> hostileRef;
                NPCEntity recruitableNpc;
                UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ne.ref, UUIDComponent.getComponentType());
                if (uuidComp != null && managedEntityIds.contains(uuidComp.getUuid().toString()) || (recruitableNpc = (NPCEntity)store.getComponent(ne.ref, NPCEntity.getComponentType())) == null || !this.isRecruitableRole(recruitableNpc.getRoleName()) || (hostileRef = this.findNearestRecruitableThreat(world, (Store<EntityStore>)store, ne.position, ne.ref, playerRef)) == null) continue;
                this.safeOverrideAttitude(recruitableNpc, hostileRef, Attitude.HOSTILE, 8.0);
                try {
                    recruitableNpc.onFlockSetTarget("LockedTarget", hostileRef);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    recruitableNpc.onFlockSetTarget("CombatTarget", hostileRef);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    recruitableNpc.getRole().getWorldSupport().requestNewPath();
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                if ((hostileNpc = (NPCEntity)store.getComponent(hostileRef, NPCEntity.getComponentType())) == null) continue;
                this.safeOverrideAttitude(hostileNpc, ne.ref, Attitude.HOSTILE, 8.0);
                try {
                    hostileNpc.onFlockSetTarget("LockedTarget", ne.ref);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    hostileNpc.onFlockSetTarget("Target", ne.ref);
                }
                catch (Throwable throwable) {
                }
            }
            catch (Throwable throwable) {}
        }
    }

    private Ref<EntityStore> findNearestRecruitableThreat(World world, Store<EntityStore> store, Vector3d recruitablePos, Ref<EntityStore> recruitableRef, PlayerRef playerRef) {
        if (world == null || store == null || recruitablePos == null || recruitableRef == null || playerRef == null) {
            return null;
        }
        UUID recruitableId = null;
        try {
            UUIDComponent uuidComp = (UUIDComponent)store.getComponent(recruitableRef, UUIDComponent.getComponentType());
            if (uuidComp != null) {
                recruitableId = uuidComp.getUuid();
            }
        }
        catch (Throwable uuidComp) {
            // empty catch block
        }
        List<WorldQueries.NearbyEntity> nearby = WorldQueries.getNearbyHostiles(world, recruitablePos, 12.0, recruitableId, playerRef.getUuid());
        Ref<EntityStore> bestRef = null;
        double bestDist = Double.MAX_VALUE;
        for (WorldQueries.NearbyEntity ne : nearby) {
            String roleName = this.getRoleName(store, ne.ref);
            if (this.isRecruitableRole(roleName) || this.isCompanionRole(roleName) || !this.isRecruitableThreatRole(roleName) || !(ne.distance < bestDist)) continue;
            bestDist = ne.distance;
            bestRef = ne.ref;
        }
        return bestRef;
    }

    private String getRoleName(Store<EntityStore> store, Ref<EntityStore> ref) {
        try {
            NPCEntity npcEntity = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
            return npcEntity != null ? npcEntity.getRoleName() : null;
        }
        catch (Throwable t) {
            return null;
        }
    }

    private void ensureRecruitableSkinToneAppearance(Ref<EntityStore> recruitableRef, Store<EntityStore> store, NPCEntity npcEntity) {
        if (recruitableRef == null || store == null || npcEntity == null) {
            return;
        }
        if (!this.isRecruitableRole(npcEntity.getRoleName())) {
            return;
        }
        long seed = this.deriveEntitySkinSeed(recruitableRef, store);
        if (seed == 0L) {
            return;
        }
        String family = this.resolveRecruitSkinToneFamily(npcEntity.getRoleName(), this.captureRecruitAppearanceId(npcEntity, recruitableRef, store));
        if (family == null || family.isBlank()) {
            return;
        }
        if (this.isAlreadySafeRecruitAppearanceFamily(recruitableRef, store, family)) {
            return;
        }
        String desiredAppearance = this.buildRecruitSkinToneAppearance(family, seed);
        if (desiredAppearance == null || desiredAppearance.isBlank()) {
            return;
        }
        try {
            String currentModel;
            ModelComponent modelComponent = (ModelComponent)store.getComponent(recruitableRef, ModelComponent.getComponentType());
            if (modelComponent != null && modelComponent.getModel() != null && desiredAppearance.equalsIgnoreCase(currentModel = modelComponent.getModel().getModelAssetId())) {
                return;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            NPCEntity.setAppearance(recruitableRef, (String)desiredAppearance, store);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private boolean isAlreadySafeRecruitAppearanceFamily(Ref<EntityStore> recruitableRef, Store<EntityStore> store, String family) {
        if (recruitableRef == null || store == null || family == null || family.isBlank()) {
            return false;
        }
        try {
            ModelComponent modelComponent = (ModelComponent)store.getComponent(recruitableRef, ModelComponent.getComponentType());
            if (modelComponent == null || modelComponent.getModel() == null) {
                return false;
            }
            String currentModel = modelComponent.getModel().getModelAssetId();
            if (currentModel == null || currentModel.isBlank()) {
                return false;
            }
            if (family.equalsIgnoreCase(currentModel)) {
                return true;
            }
            for (int i = 1; i <= 4; ++i) {
                if (!(family + "_Skin" + i).equalsIgnoreCase(currentModel)) continue;
                return true;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return false;
    }

    private boolean isCompanionRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return false;
        }
        String lower = roleName.toLowerCase(Locale.ROOT);
        return lower.contains("companion_managed") || lower.contains("companion_recruitable") || lower.contains("kweebec_companion_recruitable");
    }

    private boolean isRecruitableThreatRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return false;
        }
        String lower = roleName.toLowerCase(Locale.ROOT);
        for (String token : RECRUITABLE_THREAT_ROLE_KEYWORDS) {
            if (!lower.contains(token)) continue;
            return true;
        }
        return false;
    }

    private boolean usePlainPlayerSkinRecruitPrototype(CompanionRecord companion) {
        return false;
    }

    private void recruitWorldNPC(World world, Store<EntityStore> store, Ref<EntityStore> npcRef, NPCEntity npcEntity, PlayerRef playerRef, PlayerCompanionData data) {
        TransformComponent npcTransform = (TransformComponent)store.getComponent(npcRef, TransformComponent.getComponentType());
        Vector3d npcPos = npcTransform != null ? npcTransform.getPosition() : playerRef.getTransform().getPosition();
        AppearanceSnapshot appearanceSnapshot = this.captureAppearanceSnapshot(npcEntity, npcRef, store);
        String bestToken = this.captureBestAppearanceToken(npcEntity, npcRef, store);
        boolean recruitInPlaceForAppearance = false;
        CompanionRecord companion = new CompanionRecord(UUID.randomUUID().toString());
        companion.mode = CompanionMode.FIGHTER;
        companion.followMode = FollowMode.FOLLOW;
        companion.sourceRoleName = npcEntity.getRoleName();
        long recruitSkinSeed = this.deriveEntitySkinSeed(npcRef, store);
        if (recruitSkinSeed == 0L) {
            recruitSkinSeed = ThreadLocalRandom.current().nextLong();
        }
        if (recruitSkinSeed == 0L) {
            recruitSkinSeed = 1L;
        }
        companion.skinSeed = recruitSkinSeed;
        if (this.usePlainPlayerSkinRecruitPrototype(companion)) {
            this.cachedAppearanceByCompanionId.remove(companion.uniqueId);
            companion.savedPlayerSkinToken = null;
            companion.savedModelAssetId = null;
            companion.savedModelComponentJson = null;
            companion.savedPersistentModelJson = null;
            companion.appearanceSource = null;
            companion.appearanceLastUpdatedUtc = 0L;
            String seededToken = this.generatePlayerSkinTokenFromSeed(companion, "recruitWorldNPC-plainPlayerSkin");
            if (this.isPlayerSkinToken(seededToken)) {
                this.setAppearanceModelId(companion, seededToken, "recruitWorldNPC-plainPlayerSkin");
            } else {
                this.setAppearanceModelId(companion, null, "recruitWorldNPC-plainPlayerSkin-none");
            }
        } else {
            String seededToken;
            this.cachedAppearanceByCompanionId.remove(companion.uniqueId);
            if (appearanceSnapshot.playerSkin != null) {
                this.setAppearanceModelId(companion, this.encodePlayerSkinToken(appearanceSnapshot.playerSkin), "recruitWorldNPC-playerSkin");
            } else if (appearanceSnapshot.capturedToken != null && !this.isRandomAppearanceToken(appearanceSnapshot.capturedToken) && !this.isGenericRecruitRandomAppearance(appearanceSnapshot.capturedToken)) {
                this.setAppearanceModelId(companion, appearanceSnapshot.capturedToken, "recruitWorldNPC-capturedToken");
            } else if (appearanceSnapshot.modelAssetId != null && !this.isGenericRecruitRandomAppearance(appearanceSnapshot.modelAssetId)) {
                this.setAppearanceModelId(companion, APPEARANCE_MODEL_TOKEN_PREFIX + appearanceSnapshot.modelAssetId, "recruitWorldNPC-modelAsset");
            } else if (this.isPersistableAppearanceId(bestToken) && !this.isRandomAppearanceToken(bestToken) && !this.isGenericRecruitRandomAppearance(bestToken) && !this.isPlayerTestFallbackAppearance(bestToken)) {
                this.setAppearanceModelId(companion, bestToken, "recruitWorldNPC-bestToken");
            } else {
                this.setAppearanceModelId(companion, null, "recruitWorldNPC-none");
                this.logAppearanceCaptureDiagnostics("recruit-none", companion, npcEntity, npcRef, store);
            }
            String recruitSkinToneAppearance = this.resolveRecruitSkinToneAppearance(companion, companion.appearanceModelId);
            if (recruitSkinToneAppearance != null) {
                this.setAppearanceModelId(companion, APPEARANCE_MODEL_TOKEN_PREFIX + recruitSkinToneAppearance, "recruitWorldNPC-recruitSkinTone");
            } else if (this.shouldUseSeededRecruitPlayerSkin(companion, companion.appearanceModelId) && this.isPlayerSkinToken(seededToken = this.generatePlayerSkinTokenFromSeed(companion, "recruitWorldNPC-seededSkin"))) {
                this.setAppearanceModelId(companion, seededToken, "recruitWorldNPC-seededSkin");
            }
        }
        String recruitedWeapon = this.captureRecruitWeaponId(npcEntity);
        if (recruitedWeapon != null && !recruitedWeapon.isBlank()) {
            companion.setEquipped(EquipmentSlot.WEAPON, recruitedWeapon);
        }
        data.companions.add(companion);
        data.selectedCompanionId = companion.uniqueId;
        if (recruitInPlaceForAppearance) {
            try {
                UUIDComponent uuidComp = (UUIDComponent)store.getComponent(npcRef, UUIDComponent.getComponentType());
                if (uuidComp != null) {
                    companion.entityId = uuidComp.getUuid().toString();
                }
                companion.active = true;
                this.ensureManagedRoleForCompanion(npcEntity, companion, npcRef, store);
                this.reapplyAppearanceSnapshotToNpc(appearanceSnapshot, companion, npcRef, store, "recruit-in-place");
                this.keepOwnerFriendly(npcEntity, playerRef);
                this.ensureCompanionName(store, npcRef, playerRef, companion);
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
                this.clearAllCombatTargetChannels(npcEntity);
                try {
                    npcEntity.getRole().resetAllInstructions();
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    npcEntity.getRole().getStateSupport().setInteractable(npcRef, true);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                CompanionRuntimeState rt = this.companionManager.getRuntime(companion.uniqueId);
                rt.state = CompanionState.IDLE_FOLLOW;
                rt.stayAnchor = null;
                rt.stuckTicks = 0;
                rt.combatTargetId = null;
                rt.followBootstrapTicks = 4;
                try {
                    this.ensureNonIdleState(npcEntity, npcRef, store, rt);
                    this.setFollowTarget(npcEntity, playerRef, npcRef, store);
                    npcEntity.getRole().getWorldSupport().requestNewPath();
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                this.scheduleRecruitInPlaceFollowPrime(world, npcRef, playerRef, companion, appearanceSnapshot, 8);
                playerRef.sendMessage(Message.raw((String)"A wandering companion has joined you!"));
                playerRef.sendMessage(Message.raw((String)"Use /companion menu or press F to manage them."));
                this.logger.at(Level.INFO).log("Player " + playerRef.getUsername() + " recruited world NPC in-place as companion " + companion.uniqueId);
                this.companionManager.getPlayerRuntime((UUID)playerRef.getUuid()).lastRecruitInteractMs = 0L;
                this.saveCompanionData("recruitWorldNPC-in-place", companion);
                return;
            }
            catch (Throwable t) {
                ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Recruit in-place appearance preservation failed; falling back to respawn.");
            }
        }
        try {
            store.removeEntity(npcRef, RemoveReason.REMOVE);
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to remove recruited world NPC.");
        }
        boolean spawned = this.spawnCompanion(world, playerRef, companion, npcPos, true, false);
        if (spawned) {
            try {
                NPCEntity spawnedNpc;
                Ref spawnedRef;
                UUID eid = this.parseUuid(companion.entityId);
                if (eid != null && (spawnedRef = world.getEntityRef(eid)) != null && spawnedRef.isValid() && (spawnedNpc = (NPCEntity)store.getComponent(spawnedRef, NPCEntity.getComponentType())) != null) {
                    if (appearanceSnapshot.playerSkin != null) {
                        try {
                            store.putComponent(spawnedRef, PlayerSkinComponent.getComponentType(), (Component)new PlayerSkinComponent(this.clonePlayerSkin(appearanceSnapshot.playerSkin)));
                            this.setAppearanceModelId(companion, this.encodePlayerSkinToken(appearanceSnapshot.playerSkin), "recruitWorldNPC-playerSkin");
                        }
                        catch (Throwable throwable) {}
                    } else {
                        boolean appliedSnapshotComponents = false;
                        try {
                            if (appearanceSnapshot.modelComponent != null) {
                                store.putComponent(spawnedRef, ModelComponent.getComponentType(), (Component)appearanceSnapshot.modelComponent);
                                appliedSnapshotComponents = true;
                            }
                        }
                        catch (Throwable throwable) {
                            // empty catch block
                        }
                        try {
                            if (appearanceSnapshot.persistentModel != null) {
                                store.putComponent(spawnedRef, PersistentModel.getComponentType(), (Component)appearanceSnapshot.persistentModel);
                                appliedSnapshotComponents = true;
                            }
                        }
                        catch (Throwable throwable) {
                            // empty catch block
                        }
                        if (!appliedSnapshotComponents && appearanceSnapshot.modelAssetId != null && !appearanceSnapshot.modelAssetId.isBlank()) {
                            try {
                                NPCEntity.setAppearance((Ref)spawnedRef, (String)appearanceSnapshot.modelAssetId, store);
                                if (!this.isGenericRecruitRandomAppearance(appearanceSnapshot.modelAssetId)) {
                                    this.setAppearanceModelId(companion, APPEARANCE_MODEL_TOKEN_PREFIX + appearanceSnapshot.modelAssetId, "recruitWorldNPC-modelAsset");
                                }
                            }
                            catch (Throwable throwable) {}
                        } else {
                            String spawnedToken = this.captureBestAppearanceToken(spawnedNpc, (Ref<EntityStore>)spawnedRef, store);
                            if (!(!this.isPersistableAppearanceId(spawnedToken) || this.isRandomAppearanceToken(spawnedToken) || this.isGenericRecruitRandomAppearance(spawnedToken) || this.isPlayerTestFallbackAppearance(spawnedToken) || this.shouldRejectRecruitPlayerSkinCapture(companion, spawnedToken))) {
                                this.setAppearanceModelId(companion, spawnedToken, "recruitWorldNPC-spawnedBestToken");
                            }
                        }
                    }
                    String beforeRecruitPersist = companion.appearanceModelId;
                    this.persistCompanionAppearanceToken(companion, spawnedNpc, (Ref<EntityStore>)spawnedRef, store, "recruit-spawn");
                    if (this.isPlayerSkinToken(companion.appearanceModelId) && this.shouldRejectRecruitPlayerSkinCapture(companion, companion.appearanceModelId) && beforeRecruitPersist != null && !beforeRecruitPersist.isBlank()) {
                        this.setAppearanceModelId(companion, beforeRecruitPersist, "recruitWorldNPC-restorePrePersistAppearance");
                    }
                    this.ensureManagedRoleForCompanion(spawnedNpc, companion, (Ref<EntityStore>)spawnedRef, store);
                    this.ensureNonIdleState(spawnedNpc, (Ref<EntityStore>)spawnedRef, store, null);
                    this.setFollowTarget(spawnedNpc, playerRef, (Ref<EntityStore>)spawnedRef, store);
                    this.keepOwnerFriendly(spawnedNpc, playerRef);
                    this.applyRecordedEquipmentToNpc(spawnedNpc, companion);
                    this.syncRecordedEquipmentFromNpc(spawnedNpc, companion);
                    try {
                        spawnedNpc.getRole().getWorldSupport().requestNewPath();
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                }
                CompanionRuntimeState rt = this.companionManager.getRuntime(companion.uniqueId);
                rt.state = CompanionState.IDLE_FOLLOW;
                rt.stayAnchor = null;
                rt.stuckTicks = 0;
                rt.combatTargetId = null;
                rt.recruitAppearanceFinalizeTick = this.globalTick + 20L;
                if (companion.appearanceModelId == null || companion.appearanceModelId.isBlank()) {
                    rt.appearanceCaptureAttempts = 0;
                    rt.nextAppearanceCaptureTick = this.globalTick + 20L;
                } else {
                    rt.appearanceCaptureAttempts = 8;
                    rt.nextAppearanceCaptureTick = 0L;
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            playerRef.sendMessage(Message.raw((String)"A wandering companion has joined you!"));
            playerRef.sendMessage(Message.raw((String)"Use /companion menu or press F to manage them."));
            this.logger.at(Level.INFO).log("Player " + playerRef.getUsername() + " recruited world NPC as companion " + companion.uniqueId);
            this.companionManager.getPlayerRuntime((UUID)playerRef.getUuid()).lastRecruitInteractMs = 0L;
            this.saveCompanionData("recruitWorldNPC", companion);
        } else {
            data.companions.remove(companion);
            playerRef.sendMessage(Message.raw((String)"Failed to recruit companion. Try again."));
        }
    }

    private void scheduleRecruitInPlaceFollowPrime(World world, Ref<EntityStore> npcRef, PlayerRef playerRef, CompanionRecord companion, AppearanceSnapshot appearanceSnapshot, int attemptsRemaining) {
        if (world == null || npcRef == null || playerRef == null || companion == null || attemptsRemaining <= 0) {
            return;
        }
        world.execute(() -> {
            try {
                Store liveStore = world.getEntityStore().getStore();
                NPCEntity liveNpc = (NPCEntity)liveStore.getComponent(npcRef, NPCEntity.getComponentType());
                if (liveNpc == null) {
                    return;
                }
                this.ensureManagedRoleForCompanion(liveNpc, companion, npcRef, (Store<EntityStore>)liveStore);
                this.reapplyAppearanceSnapshotToNpc(appearanceSnapshot, companion, npcRef, (Store<EntityStore>)liveStore, "recruit-in-place-delayed");
                this.keepOwnerFriendly(liveNpc, playerRef);
                try {
                    liveNpc.onFlockSetTarget("LockedTarget", null);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    liveNpc.onFlockSetTarget("Target", null);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                this.clearAllCombatTargetChannels(liveNpc);
                String roleName = null;
                try {
                    roleName = liveNpc.getRoleName();
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                if (!COMPANION_ROLE.equalsIgnoreCase(roleName)) {
                    this.scheduleRecruitInPlaceFollowPrime(world, npcRef, playerRef, companion, appearanceSnapshot, attemptsRemaining - 1);
                    return;
                }
                this.ensureNonIdleState(liveNpc, npcRef, (Store<EntityStore>)liveStore, null);
                this.setFollowTarget(liveNpc, playerRef, npcRef, (Store<EntityStore>)liveStore);
                try {
                    liveNpc.getRole().getWorldSupport().requestNewPath();
                }
                catch (Throwable throwable) {}
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        });
    }

    private void reapplyAppearanceSnapshotToNpc(AppearanceSnapshot snapshot, CompanionRecord companion, Ref<EntityStore> npcRef, Store<EntityStore> store, String reason) {
        if (snapshot == null || companion == null || npcRef == null || store == null) {
            return;
        }
        try {
            if (snapshot.playerSkin != null) {
                store.putComponent(npcRef, PlayerSkinComponent.getComponentType(), (Component)new PlayerSkinComponent(this.clonePlayerSkin(snapshot.playerSkin)));
                this.setAppearanceModelId(companion, this.encodePlayerSkinToken(snapshot.playerSkin), "reapplyAppearanceSnapshotToNpc-skin:" + reason);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            if (snapshot.modelComponent != null) {
                store.putComponent(npcRef, ModelComponent.getComponentType(), (Component)snapshot.modelComponent);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            if (snapshot.persistentModel != null) {
                store.putComponent(npcRef, PersistentModel.getComponentType(), (Component)snapshot.persistentModel);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private boolean shouldRecruitInPlaceForAppearance(AppearanceSnapshot snapshot, String bestToken) {
        if (snapshot == null) {
            return false;
        }
        if (snapshot.playerSkin != null) {
            return false;
        }
        if (snapshot.modelComponent == null && snapshot.persistentModel == null) {
            return false;
        }
        boolean capturedIsGenericRandom = this.isGenericRecruitRandomAppearance(snapshot.capturedToken);
        boolean modelIsGenericRandom = this.isGenericRecruitRandomAppearance(snapshot.modelAssetId);
        boolean bestIsGenericRandom = this.isGenericRecruitRandomAppearance(bestToken);
        return capturedIsGenericRandom || modelIsGenericRandom || bestIsGenericRandom;
    }

    private String captureRecruitAppearanceId(NPCEntity npcEntity) {
        return this.captureRecruitAppearanceId(npcEntity, null, null);
    }

    private AppearanceSnapshot captureAppearanceSnapshot(NPCEntity npcEntity, Ref<EntityStore> npcRef, Store<EntityStore> store) {
        AppearanceSnapshot snapshot = new AppearanceSnapshot();
        snapshot.capturedToken = this.captureRecruitAppearanceId(npcEntity, npcRef, store);
        if (npcRef != null && store != null) {
            try {
                PlayerSkinComponent skinComponent = (PlayerSkinComponent)store.getComponent(npcRef, PlayerSkinComponent.getComponentType());
                if (skinComponent != null && skinComponent.getPlayerSkin() != null) {
                    snapshot.playerSkin = this.clonePlayerSkin(skinComponent.getPlayerSkin());
                    if (snapshot.capturedToken == null || snapshot.capturedToken.isBlank()) {
                        snapshot.capturedToken = this.encodePlayerSkinToken(snapshot.playerSkin);
                    }
                }
            }
            catch (Throwable skinComponent) {
                // empty catch block
            }
            try {
                ModelComponent modelComponent = (ModelComponent)store.getComponent(npcRef, ModelComponent.getComponentType());
                if (modelComponent != null && modelComponent.getModel() != null) {
                    String modelId;
                    snapshot.modelComponent = modelComponent;
                    String modelToken = this.normalizeAppearanceCandidate(modelComponent.getModel());
                    if (this.isPersistableAppearanceId(modelToken) && !this.isGenericRecruitRandomAppearance(modelToken) && !this.isPlayerTestFallbackAppearance(modelToken)) {
                        snapshot.capturedToken = modelToken;
                    }
                    if (this.isSafeAppearanceString(modelId = modelComponent.getModel().getModelAssetId())) {
                        snapshot.modelAssetId = modelId;
                    }
                }
            }
            catch (Throwable modelComponent) {
                // empty catch block
            }
            if (snapshot.modelAssetId == null || snapshot.modelAssetId.isBlank()) {
                try {
                    PersistentModel persistent = (PersistentModel)store.getComponent(npcRef, PersistentModel.getComponentType());
                    if (persistent != null && persistent.getModelReference() != null) {
                        snapshot.persistentModel = persistent;
                        String modelId = persistent.getModelReference().getModelAssetId();
                        if (this.isSafeAppearanceString(modelId)) {
                            snapshot.modelAssetId = modelId;
                        }
                    }
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
        }
        return snapshot;
    }

    private void cacheAppearanceSnapshot(CompanionRecord companion, NPCEntity npcEntity, Ref<EntityStore> npcRef, Store<EntityStore> store, String reason) {
        boolean useful;
        if (companion == null || companion.uniqueId == null || companion.uniqueId.isBlank()) {
            return;
        }
        if (this.resolveRecruitSkinToneAppearance(companion, companion.appearanceModelId) != null && !this.isExplicitRecruitSkinVariant(companion.appearanceModelId)) {
            return;
        }
        AppearanceSnapshot snapshot = this.captureAppearanceSnapshot(npcEntity, npcRef, store);
        if (snapshot != null && snapshot.playerSkin != null && this.shouldTreatPlayerSkinAsSeedOverlay(companion)) {
            snapshot.playerSkin = null;
            if (snapshot.capturedToken != null && this.isPlayerSkinToken(snapshot.capturedToken)) {
                snapshot.capturedToken = null;
            }
        }
        boolean bl = useful = snapshot != null && (snapshot.playerSkin != null || snapshot.modelAssetId != null && !snapshot.modelAssetId.isBlank() || snapshot.modelComponent != null || snapshot.persistentModel != null || snapshot.capturedToken != null && !snapshot.capturedToken.isBlank());
        if (!useful) {
            return;
        }
        this.cachedAppearanceByCompanionId.put(companion.uniqueId, snapshot);
        this.persistStructuredAppearanceSnapshot(companion, snapshot);
        this.persistCompanionAppearanceToken(companion, npcEntity, npcRef, store, "cache-" + reason);
        this.saveCompanionData("cacheAppearanceSnapshot:" + reason, companion);
    }

    private void applyCachedAppearanceSnapshot(CompanionRecord companion, NPCEntity npcEntity, Ref<EntityStore> npcRef, Store<EntityStore> store, String reason) {
        if (companion == null || npcEntity == null || npcRef == null || store == null) {
            return;
        }
        if (this.isExplicitRecruitSkinVariant(companion.appearanceModelId) && companion.savedModelComponentJson != null && !companion.savedModelComponentJson.isBlank()) {
            return;
        }
        if (this.hasExplicitPlayerSkinAppearance(companion)) {
            return;
        }
        if (this.resolveRecruitSkinToneAppearance(companion, companion.appearanceModelId) != null) {
            return;
        }
        AppearanceSnapshot snapshot = this.cachedAppearanceByCompanionId.get(companion.uniqueId);
        if (snapshot == null) {
            return;
        }
        try {
            if (snapshot.playerSkin != null) {
                store.putComponent(npcRef, PlayerSkinComponent.getComponentType(), (Component)new PlayerSkinComponent(this.clonePlayerSkin(snapshot.playerSkin)));
                this.setAppearanceModelId(companion, this.encodePlayerSkinToken(snapshot.playerSkin), "applyCachedAppearanceSnapshot-skin:" + reason);
            } else if (snapshot.modelComponent != null || snapshot.persistentModel != null) {
                if (snapshot.modelComponent != null) {
                    store.putComponent(npcRef, ModelComponent.getComponentType(), (Component)snapshot.modelComponent);
                }
                if (snapshot.persistentModel != null) {
                    store.putComponent(npcRef, PersistentModel.getComponentType(), (Component)snapshot.persistentModel);
                }
            } else if (snapshot.modelAssetId != null && !snapshot.modelAssetId.isBlank()) {
                try {
                    NPCEntity.setAppearance(npcRef, (String)snapshot.modelAssetId, store);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                this.setAppearanceModelId(companion, APPEARANCE_MODEL_TOKEN_PREFIX + snapshot.modelAssetId, "applyCachedAppearanceSnapshot-model:" + reason);
            }
            try {
                if (snapshot.modelComponent != null) {
                    store.putComponent(npcRef, ModelComponent.getComponentType(), (Component)snapshot.modelComponent);
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                if (snapshot.persistentModel != null) {
                    store.putComponent(npcRef, PersistentModel.getComponentType(), (Component)snapshot.persistentModel);
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            this.saveCompanionData("applyCachedAppearanceSnapshot:" + reason, companion);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void persistStructuredAppearanceSnapshot(CompanionRecord companion, AppearanceSnapshot snapshot) {
        if (companion == null || snapshot == null) {
            return;
        }
        try {
            if (snapshot.modelComponent != null) {
                companion.savedModelComponentJson = APPEARANCE_GSON.toJson((Object)snapshot.modelComponent, ModelComponent.class);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            if (snapshot.persistentModel != null) {
                companion.savedPersistentModelJson = APPEARANCE_GSON.toJson((Object)snapshot.persistentModel, PersistentModel.class);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private boolean applyStructuredAppearanceSnapshot(CompanionRecord companion, Ref<EntityStore> npcRef, Store<EntityStore> store, String reason) {
        if (companion == null || npcRef == null || store == null) {
            return false;
        }
        if (this.hasExplicitPlayerSkinAppearance(companion)) {
            return false;
        }
        boolean applied = false;
        try {
            ModelComponent modelComponent;
            if (companion.savedModelComponentJson != null && !companion.savedModelComponentJson.isBlank() && (modelComponent = (ModelComponent)APPEARANCE_GSON.fromJson(companion.savedModelComponentJson, ModelComponent.class)) != null) {
                store.putComponent(npcRef, ModelComponent.getComponentType(), (Component)modelComponent);
                applied = true;
            }
        }
        catch (Throwable modelComponent) {
            // empty catch block
        }
        try {
            PersistentModel persistentModel;
            if (companion.savedPersistentModelJson != null && !companion.savedPersistentModelJson.isBlank() && (persistentModel = (PersistentModel)APPEARANCE_GSON.fromJson(companion.savedPersistentModelJson, PersistentModel.class)) != null) {
                store.putComponent(npcRef, PersistentModel.getComponentType(), (Component)persistentModel);
                applied = true;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return applied;
    }

    private PlayerSkin clonePlayerSkin(PlayerSkin src) {
        if (src == null) {
            return null;
        }
        PlayerSkin copy = new PlayerSkin();
        copy.bodyCharacteristic = src.bodyCharacteristic;
        copy.underwear = src.underwear;
        copy.face = src.face;
        copy.eyes = src.eyes;
        copy.ears = src.ears;
        copy.mouth = src.mouth;
        copy.facialHair = src.facialHair;
        copy.haircut = src.haircut;
        copy.eyebrows = src.eyebrows;
        copy.pants = src.pants;
        copy.overpants = src.overpants;
        copy.undertop = src.undertop;
        copy.overtop = src.overtop;
        copy.shoes = src.shoes;
        copy.headAccessory = src.headAccessory;
        copy.faceAccessory = src.faceAccessory;
        copy.earAccessory = src.earAccessory;
        copy.skinFeature = src.skinFeature;
        copy.gloves = src.gloves;
        copy.cape = src.cape;
        return copy;
    }

    private String captureRecruitAppearanceId(NPCEntity npcEntity, Ref<EntityStore> npcRef, Store<EntityStore> store) {
        String candidate;
        Object value;
        if (npcEntity == null) {
            return null;
        }
        String[] appearanceMethods = new String[]{"getAppearanceId", "getAppearance", "getSkinId", "getSkin", "getCurrentAppearance", "getModelId", "getModel", "getModelAssetId"};
        for (String methodName : appearanceMethods) {
            try {
                Method method = npcEntity.getClass().getMethod(methodName, new Class[0]);
                value = method.invoke((Object)npcEntity, new Object[0]);
                candidate = this.normalizeAppearanceCandidate(value);
                if (!this.isPersistableAppearanceId(candidate)) continue;
                return candidate;
            }
            catch (Throwable method) {
                // empty catch block
            }
        }
        try {
            for (Method method : npcEntity.getClass().getMethods()) {
                String name;
                if (method.getParameterCount() != 0 || !(name = method.getName().toLowerCase(Locale.ROOT)).contains("appearance") && !name.contains("skin") && !name.contains("model") || !this.isPersistableAppearanceId(candidate = this.normalizeAppearanceCandidate(value = method.invoke((Object)npcEntity, new Object[0])))) continue;
                return candidate;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        if (npcRef != null && store != null) {
            try {
                String token;
                PlayerSkinComponent skinComponent = (PlayerSkinComponent)store.getComponent(npcRef, PlayerSkinComponent.getComponentType());
                if (skinComponent != null && skinComponent.getPlayerSkin() != null && this.isPersistableAppearanceId(token = this.encodePlayerSkinToken(skinComponent.getPlayerSkin()))) {
                    return token;
                }
            }
            catch (Throwable skinComponent) {
                // empty catch block
            }
            try {
                String fromPersistent;
                PersistentModel persistent = (PersistentModel)store.getComponent(npcRef, PersistentModel.getComponentType());
                if (persistent != null && persistent.getModelReference() != null && this.isPersistableModelAssetId(fromPersistent = persistent.getModelReference().getModelAssetId())) {
                    return APPEARANCE_MODEL_TOKEN_PREFIX + fromPersistent;
                }
            }
            catch (Throwable persistent) {
                // empty catch block
            }
            try {
                ModelComponent modelComponent = (ModelComponent)store.getComponent(npcRef, ModelComponent.getComponentType());
                if (modelComponent != null && modelComponent.getModel() != null) {
                    String modelToken = this.normalizeAppearanceCandidate(modelComponent.getModel());
                    if (this.isPersistableAppearanceId(modelToken) && !this.isGenericRecruitRandomAppearance(modelToken) && !this.isPlayerTestFallbackAppearance(modelToken)) {
                        return modelToken;
                    }
                    String fromModel = modelComponent.getModel().getModelAssetId();
                    if (this.isPersistableModelAssetId(fromModel)) {
                        return APPEARANCE_MODEL_TOKEN_PREFIX + fromModel;
                    }
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return null;
    }

    private String captureBestAppearanceToken(NPCEntity npcEntity, Ref<EntityStore> npcRef, Store<EntityStore> store) {
        String captured;
        if (npcRef != null && store != null) {
            try {
                String token;
                PlayerSkinComponent skinComponent = (PlayerSkinComponent)store.getComponent(npcRef, PlayerSkinComponent.getComponentType());
                if (skinComponent != null && skinComponent.getPlayerSkin() != null && this.isPersistableAppearanceId(token = this.encodePlayerSkinToken(skinComponent.getPlayerSkin()))) {
                    return token;
                }
            }
            catch (Throwable skinComponent) {
                // empty catch block
            }
            try {
                String modelId;
                PersistentModel persistent = (PersistentModel)store.getComponent(npcRef, PersistentModel.getComponentType());
                if (persistent != null && persistent.getModelReference() != null && this.isPersistableModelAssetId(modelId = persistent.getModelReference().getModelAssetId()) && !this.isPlayerTestFallbackAppearance(modelId)) {
                    return APPEARANCE_MODEL_TOKEN_PREFIX + modelId;
                }
            }
            catch (Throwable persistent) {
                // empty catch block
            }
            try {
                ModelComponent modelComponent = (ModelComponent)store.getComponent(npcRef, ModelComponent.getComponentType());
                if (modelComponent != null && modelComponent.getModel() != null) {
                    String modelToken = this.normalizeAppearanceCandidate(modelComponent.getModel());
                    if (this.isPersistableAppearanceId(modelToken) && !this.isGenericRecruitRandomAppearance(modelToken) && !this.isPlayerTestFallbackAppearance(modelToken)) {
                        return modelToken;
                    }
                    String modelId = modelComponent.getModel().getModelAssetId();
                    if (this.isPersistableModelAssetId(modelId) && !this.isPlayerTestFallbackAppearance(modelId)) {
                        return APPEARANCE_MODEL_TOKEN_PREFIX + modelId;
                    }
                }
            }
            catch (Throwable modelComponent) {
                // empty catch block
            }
        }
        if (this.isPersistableAppearanceId(captured = this.captureRecruitAppearanceId(npcEntity, npcRef, store)) && !this.isPlayerTestFallbackAppearance(captured)) {
            return captured;
        }
        return null;
    }

    private void logAppearanceCaptureDiagnostics(String reason, CompanionRecord companion, NPCEntity npcEntity, Ref<EntityStore> npcRef, Store<EntityStore> store) {
    }

    private void logMinerTeleportTrace(String reason, CompanionRecord companion, CompanionRuntimeState runtime, Vector3d from, Vector3d to, Vector3d ownerPos) {
    }

    private void persistCompanionAppearanceToken(CompanionRecord companion, NPCEntity npcEntity, Ref<EntityStore> npcRef, Store<EntityStore> store, String reason) {
        boolean existingKnownCustom;
        if (companion == null || npcEntity == null) {
            return;
        }
        String token = this.captureBestAppearanceToken(npcEntity, npcRef, store);
        if (!this.isPersistableAppearanceId(token)) {
            this.logAppearanceCaptureDiagnostics("persist-token-missing:" + reason, companion, npcEntity, npcRef, store);
            return;
        }
        boolean tokenIsFallback = this.isPlayerTestFallbackAppearance(token);
        boolean recruitOrigin = this.isRecruitableRole(companion.sourceRoleName);
        boolean tokenIsGenericRecruitRandom = this.isGenericRecruitRandomAppearance(token);
        if (this.shouldRejectRecruitPlayerSkinCapture(companion, token)) {
            return;
        }
        if (tokenIsFallback && recruitOrigin) {
            return;
        }
        if (tokenIsGenericRecruitRandom && recruitOrigin) {
            return;
        }
        boolean bl = existingKnownCustom = companion.appearanceModelId != null && !companion.appearanceModelId.isBlank() && !this.isGenericRecruitRandomAppearance(companion.appearanceModelId) && !this.isPlayerTestFallbackAppearance(companion.appearanceModelId);
        if (tokenIsFallback && existingKnownCustom) {
            return;
        }
        this.setAppearanceModelId(companion, token, "persistCompanionAppearanceToken:" + reason);
    }

    private boolean isPersistableAppearanceId(String appearanceId) {
        return this.isSafeAppearanceString(appearanceId);
    }

    private boolean isPersistableModelAssetId(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return false;
        }
        String lower = modelId.toLowerCase(Locale.ROOT);
        if (this.isRandomAppearanceToken(lower)) {
            return false;
        }
        return !lower.startsWith("com.hypixel.");
    }

    private boolean isRandomAppearanceToken(String appearanceId) {
        if (appearanceId == null) {
            return false;
        }
        String lower = appearanceId.toLowerCase(Locale.ROOT);
        return lower.equals(RANDOM_COSMETIC_TOKEN.toLowerCase(Locale.ROOT));
    }

    private boolean isPlayerTestFallbackAppearance(String appearanceId) {
        String lower;
        if (appearanceId == null) {
            return false;
        }
        String normalized = appearanceId;
        if (this.isAppearanceModelToken(normalized)) {
            try {
                String decoded = this.decodeAppearanceModelToken(normalized);
                if (decoded != null && !decoded.isBlank()) {
                    normalized = decoded;
                }
            }
            catch (Throwable decoded) {
                // empty catch block
            }
        }
        return (lower = normalized.toLowerCase(Locale.ROOT)).equals("playertestmodel_v") || lower.equals("playertestmodel_g");
    }

    private boolean isGenericRecruitRandomAppearance(String appearanceId) {
        String lower;
        if (appearanceId == null || appearanceId.isBlank()) {
            return false;
        }
        String normalized = appearanceId;
        if (this.isAppearanceModelToken(normalized)) {
            try {
                String decoded = this.decodeAppearanceModelToken(normalized);
                if (decoded != null && !decoded.isBlank()) {
                    normalized = decoded;
                }
            }
            catch (Throwable decoded) {
                // empty catch block
            }
        }
        if ((lower = normalized.toLowerCase(Locale.ROOT)).matches("forestcompanion_random(_merchant|_guild)?_skin[1-4]")) {
            return false;
        }
        return lower.startsWith("forestcompanion_random_") || lower.startsWith("companion_random_") || lower.contains("_random_merchant") || lower.contains("_random_guild") || lower.contains("_random_companion");
    }

    private boolean isSafeAppearanceString(String appearanceId) {
        if (appearanceId == null || appearanceId.isBlank()) {
            return false;
        }
        if (this.isPlayerSkinToken(appearanceId)) {
            return true;
        }
        if (this.isAppearanceModelToken(appearanceId)) {
            return true;
        }
        if (this.isRandomAppearanceToken(appearanceId)) {
            return false;
        }
        if (appearanceId.contains("@")) {
            return false;
        }
        return !appearanceId.startsWith("com.hypixel.");
    }

    private String normalizeAppearanceCandidate(Object value) {
        String[] idMethods;
        if (value == null) {
            return null;
        }
        if (value instanceof PlayerSkin) {
            PlayerSkin skin = (PlayerSkin)value;
            return this.encodePlayerSkinToken(skin);
        }
        if (value instanceof String) {
            String s = (String)value;
            String trimmed = s.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
        try {
            String[] nestedSkin;
            Method getPlayerSkin = value.getClass().getMethod("getPlayerSkin", new Class[0]);
            if (getPlayerSkin.getParameterCount() == 0 && (nestedSkin = getPlayerSkin.invoke(value, new Object[0])) instanceof PlayerSkin) {
                PlayerSkin skin = (PlayerSkin)nestedSkin;
                return this.encodePlayerSkinToken(skin);
            }
        }
        catch (Throwable getPlayerSkin) {
            // empty catch block
        }
        for (String methodName : idMethods = new String[]{"getId", "getAssetId", "getModelId", "getAppearanceId", "name"}) {
            try {
                String s;
                String trimmed;
                Object nested;
                Method method = value.getClass().getMethod(methodName, new Class[0]);
                if (method.getParameterCount() != 0 || !((nested = method.invoke(value, new Object[0])) instanceof String) || (trimmed = (s = (String)nested).trim()).isEmpty()) continue;
                return trimmed;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        String text = value.toString();
        if (text == null) {
            return null;
        }
        if ((text = text.trim()).isEmpty()) {
            return null;
        }
        if (text.contains("@")) {
            return null;
        }
        return text;
    }

    private String encodePlayerSkinToken(PlayerSkin skin) {
        if (skin == null) {
            return null;
        }
        CharSequence[] fields = new String[]{this.safeTokenPart(skin.bodyCharacteristic), this.safeTokenPart(skin.underwear), this.safeTokenPart(skin.face), this.safeTokenPart(skin.eyes), this.safeTokenPart(skin.ears), this.safeTokenPart(skin.mouth), this.safeTokenPart(skin.facialHair), this.safeTokenPart(skin.haircut), this.safeTokenPart(skin.eyebrows), this.safeTokenPart(skin.pants), this.safeTokenPart(skin.overpants), this.safeTokenPart(skin.undertop), this.safeTokenPart(skin.overtop), this.safeTokenPart(skin.shoes), this.safeTokenPart(skin.headAccessory), this.safeTokenPart(skin.faceAccessory), this.safeTokenPart(skin.earAccessory), this.safeTokenPart(skin.skinFeature), this.safeTokenPart(skin.gloves), this.safeTokenPart(skin.cape)};
        return PLAYER_SKIN_TOKEN_PREFIX + String.join((CharSequence)"|", fields);
    }

    private String safeTokenPart(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("|", "\\p");
    }

    private String[] splitTokenParts(String tokenBody, int expectedParts) {
        List<String> parts = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < tokenBody.length(); ++i) {
            char ch = tokenBody.charAt(i);
            if (escaped) {
                if (ch == '\\') {
                    current.append('\\');
                } else if (ch == 'p') {
                    current.append('|');
                } else {
                    current.append(ch);
                }
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '|') {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        parts.add(current.toString());
        while (parts.size() < expectedParts) {
            parts.add("");
        }
        if (parts.size() > expectedParts) {
            parts = parts.subList(0, expectedParts);
        }
        return parts.toArray(new String[0]);
    }

    private boolean isPlayerSkinToken(String appearanceId) {
        return appearanceId != null && appearanceId.startsWith(PLAYER_SKIN_TOKEN_PREFIX);
    }

    private boolean isAppearanceModelToken(String appearanceId) {
        return appearanceId != null && appearanceId.startsWith(APPEARANCE_MODEL_TOKEN_PREFIX);
    }

    private String decodeAppearanceModelToken(String token) {
        if (!this.isAppearanceModelToken(token)) {
            return null;
        }
        String modelId = token.substring(APPEARANCE_MODEL_TOKEN_PREFIX.length());
        return this.isPersistableModelAssetId(modelId) ? modelId : null;
    }

    private boolean applyPlayerSkinToken(Ref<EntityStore> ref, Store<EntityStore> store, String token) {
        if (ref == null || store == null || !this.isPlayerSkinToken(token)) {
            return false;
        }
        try {
            PlayerSkin skin = this.decodePlayerSkinToken(token);
            if (skin == null) {
                return false;
            }
            PlayerSkinComponent component = new PlayerSkinComponent(skin);
            this.markSkinComponentDirty(component);
            store.putComponent(ref, PlayerSkinComponent.getComponentType(), (Component)component);
            return true;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private boolean applyAppearanceModelToken(Ref<EntityStore> ref, Store<EntityStore> store, String token) {
        if (ref == null || store == null || !this.isAppearanceModelToken(token)) {
            return false;
        }
        try {
            String modelId = this.decodeAppearanceModelToken(token);
            if (modelId == null) {
                return false;
            }
            NPCEntity.setAppearance(ref, (String)modelId, store);
            return true;
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
            String lower = state.toLowerCase(Locale.ROOT);
            return lower.startsWith("start.");
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private String captureRecruitWeaponId(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return null;
        }
        try {
            CombinedItemContainer combined;
            Inventory inventory = npcEntity.getInventory();
            CombinedItemContainer combinedItemContainer = combined = inventory != null ? inventory.getCombinedHotbarFirst() : null;
            if (combined == null) {
                return null;
            }
            ItemStack slot0 = combined.getItemStack((short)0);
            if (slot0 != null && !slot0.isEmpty()) {
                return slot0.getItemId();
            }
            String[] found = new String[]{null};
            combined.forEach((slot, itemStack) -> {
                if (found[0] != null) {
                    return;
                }
                if (itemStack != null && !itemStack.isEmpty()) {
                    found[0] = itemStack.getItemId();
                }
            });
            return found[0];
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private void trySwitchRecruitedNpcToManagedRole(NPCEntity npcEntity, CompanionRecord companion, Ref<EntityStore> npcRef, Store<EntityStore> store) {
        this.ensureManagedRoleForCompanion(npcEntity, companion, npcRef, store);
    }

    private int resolveRoleIndexByName(NPCPlugin npcPlugin, String roleName) {
        if (npcPlugin == null || roleName == null || roleName.isBlank()) {
            return -1;
        }
        try {
            List roles = npcPlugin.getRoleTemplateNames(true);
            if (roles == null) {
                return -1;
            }
            for (int i = 0; i < roles.size(); ++i) {
                String name = (String)roles.get(i);
                if (name == null || !name.equalsIgnoreCase(roleName)) continue;
                return i;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return -1;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private void ensureManagedRoleForCompanion(NPCEntity npcEntity, CompanionRecord companion, Ref<EntityStore> npcRef, Store<EntityStore> store) {
        if (npcEntity == null) return;
        if (companion == null) return;
        if (npcRef == null) return;
        if (store == null) {
            return;
        }
        try {
            String validAppearance;
            String preferredAppearance;
            String beforeRole = npcEntity.getRoleName();
            if (beforeRole != null && beforeRole.equalsIgnoreCase(COMPANION_ROLE)) {
                return;
            }
            NPCPlugin npcPlugin = NPCPlugin.get();
            if (npcPlugin == null) return;
            if (!npcPlugin.hasRoleName(COMPANION_ROLE)) {
                return;
            }
            int managedRoleIndex = this.resolveRoleIndexByName(npcPlugin, COMPANION_ROLE);
            try {
                npcEntity.setRoleName(COMPANION_ROLE);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            if (managedRoleIndex >= 0) {
                try {
                    npcEntity.setRoleIndex(managedRoleIndex);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    npcEntity.setSpawnRoleIndex(managedRoleIndex);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                this.requestRoleChangeOnRoleObject(npcEntity, managedRoleIndex);
            }
            if ((preferredAppearance = this.resolveStructuredAppearanceToken(companion)) == null || preferredAppearance.isBlank()) {
                preferredAppearance = companion.appearanceModelId;
            }
            if ((validAppearance = this.resolveModelId(preferredAppearance)) != null) {
                try {
                    NPCEntity.setAppearance(npcRef, (String)validAppearance, store);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                this.setAppearanceModelId(companion, validAppearance, "ensureManagedRoleForCompanion-validAppearance");
            } else if (this.isPlayerSkinToken(preferredAppearance)) {
                try {
                    NPCEntity.setAppearance(npcRef, (String)"Player", store);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                boolean applied = this.applyPlayerSkinToken(npcRef, store, preferredAppearance);
                if (applied) {
                    // empty if block
                }
            } else if (this.isAppearanceModelToken(preferredAppearance)) {
                boolean applied = this.applyAppearanceModelToken(npcRef, store, preferredAppearance);
                if (applied) {
                    // empty if block
                }
            } else if (this.isSafeAppearanceString(preferredAppearance)) {
                try {
                    NPCEntity.setAppearance(npcRef, (String)preferredAppearance, store);
                }
                catch (Throwable applied) {}
            } else if (preferredAppearance == null || !preferredAppearance.isBlank()) {
                // empty if block
            }
            this.applyPersistentSkinOverlay(companion, npcRef, store, "ensureManagedRoleForCompanion");
            String afterRole = null;
            try {
                afterRole = npcEntity.getRoleName();
                return;
            }
            catch (Throwable throwable) {
                return;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void requestRoleChangeOnRoleObject(NPCEntity npcEntity, int roleIndex) {
        if (npcEntity == null || roleIndex < 0) {
            return;
        }
        try {
            Role role = npcEntity.getRole();
            if (role == null) {
                return;
            }
            try {
                Method setRoleIndexMethod = role.getClass().getMethod("setRoleIndex", Integer.TYPE, String.class);
                setRoleIndexMethod.invoke((Object)role, roleIndex, "FH_Companion_Managed_Runtime");
            }
            catch (Throwable setRoleIndexMethod) {
                // empty catch block
            }
            try {
                Method setRoleChangeRequestedMethod = role.getClass().getMethod("setRoleChangeRequested", new Class[0]);
                setRoleChangeRequestedMethod.invoke((Object)role, new Object[0]);
            }
            catch (Throwable throwable) {}
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private boolean consumeInteractionTriggered(Object stateSupport, PlayerRef playerRef) {
        if (stateSupport == null || playerRef == null) {
            return false;
        }
        try {
            Ref ownerRef = playerRef.getReference();
            Boolean consumed = this.invokeConsumeInteractionBool(stateSupport, (Ref<EntityStore>)ownerRef);
            if (consumed != null) {
                return consumed;
            }
            boolean before = this.invokeWillInteractWith(stateSupport, (Ref<EntityStore>)ownerRef);
            if (!before) {
                return false;
            }
            this.invokeConsumeInteractionVoid(stateSupport, (Ref<EntityStore>)ownerRef);
            boolean after = this.invokeWillInteractWith(stateSupport, (Ref<EntityStore>)ownerRef);
            return !after;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private boolean invokeWillInteractWith(Object stateSupport, Ref<EntityStore> ownerRef) throws Exception {
        Method method = stateSupport.getClass().getMethod("willInteractWith", Ref.class);
        Object result = method.invoke(stateSupport, ownerRef);
        if (result instanceof Boolean) {
            Boolean b = (Boolean)result;
            return b;
        }
        return false;
    }

    private Boolean invokeConsumeInteractionBool(Object stateSupport, Ref<EntityStore> ownerRef) throws Exception {
        Method method = stateSupport.getClass().getMethod("consumeInteraction", Ref.class);
        Object result = method.invoke(stateSupport, ownerRef);
        if (result instanceof Boolean) {
            Boolean b = (Boolean)result;
            return b;
        }
        if (result != null) {
            String text = result.toString();
            if ("true".equalsIgnoreCase(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text)) {
                return false;
            }
        }
        return null;
    }

    private void invokeConsumeInteractionVoid(Object stateSupport, Ref<EntityStore> ownerRef) throws Exception {
        Method method = stateSupport.getClass().getMethod("consumeInteraction", Ref.class);
        method.invoke(stateSupport, ownerRef);
    }

    private MovementOwner ownerForState(CompanionState state) {
        if (state == null) {
            return MovementOwner.NONE;
        }
        return switch (state) {
            case CompanionState.COMBAT_ENGAGE -> MovementOwner.COMBAT;
            case CompanionState.LOOT_SEEK -> MovementOwner.LOOT;
            case CompanionState.DEPOSIT_SEEK -> MovementOwner.DEPOSIT;
            case CompanionState.FARM_SEEK -> MovementOwner.FARM;
            case CompanionState.MINE_SEEK, CompanionState.MINE_ACTIVE -> MovementOwner.MINE;
            case CompanionState.TELEPORT_CATCHUP -> MovementOwner.TELEPORT;
            case CompanionState.RECOVER_STUCK -> MovementOwner.RECOVER;
            case CompanionState.IDLE_STAY -> MovementOwner.STAY;
            case CompanionState.IDLE_PATROL -> MovementOwner.PATROL;
            case CompanionState.IDLE_FREE -> MovementOwner.FREE;
            case CompanionState.IDLE_FOLLOW -> MovementOwner.FOLLOW;
            default -> MovementOwner.NONE;
        };
    }

    private void updateMovementOwner(CompanionRuntimeState runtime, MovementOwner nextOwner) {
        MovementOwner owner;
        if (runtime == null) {
            return;
        }
        MovementOwner movementOwner = owner = nextOwner != null ? nextOwner : MovementOwner.NONE;
        if (runtime.movementOwner == owner) {
            return;
        }
        runtime.movementOwner = owner;
        runtime.movementOwnerChangedTick = runtime.currentTick;
    }

    private void setAppearanceModelId(CompanionRecord companion, String newValue, String reason) {
        if (companion == null) {
            return;
        }
        String oldValue = companion.appearanceModelId;
        if (this.shouldPreserveExplicitRecruitSkinVariant(companion, oldValue, newValue, reason)) {
            return;
        }
        companion.appearanceModelId = newValue;
        this.syncStructuredAppearancePersistence(companion, newValue, reason);
    }

    private boolean shouldPreserveExplicitRecruitSkinVariant(CompanionRecord companion, String oldValue, String newValue, String reason) {
        String why;
        if (companion == null || !this.isRecruitableRole(companion.sourceRoleName)) {
            return false;
        }
        if (!this.isExplicitRecruitSkinVariant(oldValue)) {
            return false;
        }
        if (this.isExplicitRecruitSkinVariant(newValue)) {
            return false;
        }
        String string = why = reason != null ? reason : "";
        if ("setPreferredAppearanceModel".equals(why) || "clearPreferredAppearanceModel".equals(why) || "rerollAppearance-randomModel".equals(why) || "appearance-randomized".equals(why)) {
            return false;
        }
        return this.isPlayerTestFallbackAppearance(newValue) || this.isGenericRecruitRandomAppearance(newValue);
    }

    private boolean isExplicitRecruitSkinVariant(String appearanceId) {
        if (appearanceId == null || appearanceId.isBlank()) {
            return false;
        }
        String normalized = appearanceId;
        if (this.isAppearanceModelToken(normalized)) {
            try {
                String decoded = this.decodeAppearanceModelToken(normalized);
                if (decoded != null && !decoded.isBlank()) {
                    normalized = decoded;
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return normalized.toLowerCase(Locale.ROOT).matches("forestcompanion_random(_merchant|_guild)?_skin[1-4]");
    }

    private void syncStructuredAppearancePersistence(CompanionRecord companion, String value, String reason) {
        String why;
        if (companion == null) {
            return;
        }
        String string = why = reason != null ? reason : "";
        if ("clearPreferredAppearanceModel".equals(why)) {
            companion.savedPlayerSkinToken = null;
            companion.savedModelAssetId = null;
            companion.savedModelComponentJson = null;
            companion.savedPersistentModelJson = null;
            companion.appearanceSource = "UNKNOWN";
            companion.appearanceLastUpdatedUtc = System.currentTimeMillis();
            return;
        }
        if (value == null || value.isBlank()) {
            return;
        }
        if (this.isPlayerTestFallbackAppearance(value) || this.isRandomAppearanceToken(value)) {
            return;
        }
        if (this.isPlayerSkinToken(value)) {
            companion.savedPlayerSkinToken = value;
            companion.savedModelAssetId = null;
            companion.appearanceSource = "PLAYER_SKIN";
            companion.appearanceLastUpdatedUtc = System.currentTimeMillis();
            return;
        }
        if (this.isAppearanceModelToken(value)) {
            String decoded = this.decodeAppearanceModelToken(value);
            if (decoded != null && !decoded.isBlank() && !this.isPlayerTestFallbackAppearance(decoded)) {
                companion.savedModelAssetId = decoded;
                companion.appearanceSource = "MODEL_ASSET";
                companion.appearanceLastUpdatedUtc = System.currentTimeMillis();
            }
            return;
        }
        if (this.isSafeAppearanceString(value) && !this.isPlayerTestFallbackAppearance(value)) {
            companion.savedModelAssetId = value;
            companion.appearanceSource = "MODEL_ASSET";
            companion.appearanceLastUpdatedUtc = System.currentTimeMillis();
        }
    }

    private String resolveStructuredAppearanceToken(CompanionRecord companion) {
        if (companion == null) {
            return null;
        }
        if (this.shouldIgnoreRecruitPlayerSkinToken(companion, companion.appearanceModelId)) {
            return null;
        }
        if (this.usePlainPlayerSkinRecruitPrototype(companion)) {
            return this.isPlayerSkinToken(companion.appearanceModelId) ? companion.appearanceModelId : null;
        }
        if (this.resolveRecruitSkinToneAppearance(companion, companion.appearanceModelId) != null) {
            return null;
        }
        if (companion.savedPlayerSkinToken != null && !this.shouldIgnoreRecruitPlayerSkinToken(companion, companion.savedPlayerSkinToken) && this.isPlayerSkinToken(companion.savedPlayerSkinToken)) {
            return companion.savedPlayerSkinToken;
        }
        if (companion.savedModelAssetId != null && !companion.savedModelAssetId.isBlank() && !this.isPlayerTestFallbackAppearance(companion.savedModelAssetId)) {
            return APPEARANCE_MODEL_TOKEN_PREFIX + companion.savedModelAssetId;
        }
        return null;
    }

    private void saveCompanionData(String reason, CompanionRecord companion) {
        try {
            this.companionManager.save();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void snapshotAllActiveCompanionInventoryOnly() {
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                return;
            }
            for (PlayerRef playerRef : universe.getPlayers()) {
                Store store;
                PlayerCompanionData data;
                World world;
                if (playerRef == null || (world = universe.getWorld(playerRef.getWorldUuid())) == null || (data = this.companionManager.get(playerRef.getUuid())) == null || data.companions == null || (store = world.getEntityStore().getStore()) == null) continue;
                for (CompanionRecord companion : data.companions) {
                    Ref ref;
                    UUID eid;
                    if (companion == null || !companion.active || companion.entityId == null || (eid = this.parseUuid(companion.entityId)) == null || (ref = world.getEntityRef(eid)) == null || !ref.isValid()) continue;
                    this.snapshotCompanionInventoryOnly((Store<EntityStore>)store, (Ref<EntityStore>)ref, companion);
                }
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to snapshot active companion inventory during shutdown.");
        }
    }

    private void updateCompanionLastKnownLocation(CompanionRecord companion, PlayerRef ownerRef, Vector3d pos) {
        if (companion == null || ownerRef == null || ownerRef.getWorldUuid() == null || pos == null) {
            return;
        }
        companion.lastKnownLocation = new BlockPos(ownerRef.getWorldUuid().toString(), (int)Math.floor(pos.x), (int)Math.floor(pos.y), (int)Math.floor(pos.z));
    }

    public BlockPos getCompanionCurrentOrLastKnownLocation(PlayerRef ownerRef, CompanionRecord companion) {
        if (companion == null || ownerRef == null) {
            return companion != null ? companion.lastKnownLocation : null;
        }
        if (companion.active && companion.entityId != null) {
            try {
                Store store;
                TransformComponent transform;
                Ref ref;
                World world = Universe.get().getWorld(ownerRef.getWorldUuid());
                if (world != null && (ref = world.getEntityRef(UUID.fromString(companion.entityId))) != null && ref.isValid() && (transform = (TransformComponent)(store = world.getEntityStore().getStore()).getComponent(ref, TransformComponent.getComponentType())) != null && transform.getPosition() != null) {
                    BlockPos live;
                    Vector3d pos = transform.getPosition();
                    companion.lastKnownLocation = live = new BlockPos(ownerRef.getWorldUuid().toString(), (int)Math.floor(pos.x), (int)Math.floor(pos.y), (int)Math.floor(pos.z));
                    return live;
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return companion.lastKnownLocation;
    }

    private void snapshotAllActiveCompanionAppearanceOnly() {
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                return;
            }
            for (PlayerRef playerRef : universe.getPlayers()) {
                Store store;
                PlayerCompanionData data;
                World world;
                if (playerRef == null || (world = universe.getWorld(playerRef.getWorldUuid())) == null || (data = this.companionManager.get(playerRef.getUuid())) == null || data.companions == null || (store = world.getEntityStore().getStore()) == null) continue;
                for (CompanionRecord companion : data.companions) {
                    NPCEntity npcEntity;
                    Ref ref;
                    UUID eid;
                    if (companion == null || !companion.active || companion.entityId == null || (eid = this.parseUuid(companion.entityId)) == null || (ref = world.getEntityRef(eid)) == null || !ref.isValid() || (npcEntity = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType())) == null) continue;
                    this.cacheAppearanceSnapshot(companion, npcEntity, (Ref<EntityStore>)ref, (Store<EntityStore>)store, "shutdown");
                }
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to snapshot active companion appearance during shutdown.");
        }
    }

    private void snapshotCompanionInventoryOnly(Store<EntityStore> store, Ref<EntityStore> ref, CompanionRecord companion) {
        if (store == null || ref == null || companion == null) {
            return;
        }
        try {
            NPCEntity npcEntity = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
            if (npcEntity == null || npcEntity.getInventory() == null) {
                return;
            }
            this.snapshotSavedInventoryFromNpc(npcEntity, companion);
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Could not snapshot active companion inventory.");
        }
    }

    private void maybeSnapshotActiveInventory(CompanionRuntimeState runtime, CompanionRecord companion, Store<EntityStore> store, Ref<EntityStore> ref) {
        if (runtime == null || companion == null || !companion.active) {
            return;
        }
        if (runtime.currentTick - runtime.lastInventorySnapshotTick < 40L) {
            return;
        }
        this.snapshotCompanionInventoryOnly(store, ref, companion);
        runtime.lastInventorySnapshotTick = runtime.currentTick;
    }

    private void maybeSnapshotActiveAppearance(CompanionRuntimeState runtime, CompanionRecord companion, Store<EntityStore> store, Ref<EntityStore> ref) {
        if (runtime == null || companion == null || !companion.active) {
            return;
        }
        try {
            NPCEntity npcEntity = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
            if (npcEntity == null) {
                return;
            }
            if (runtime.recruitAppearanceFinalizeTick > 0L && runtime.currentTick >= runtime.recruitAppearanceFinalizeTick) {
                this.cacheAppearanceSnapshot(companion, npcEntity, ref, store, "recruit-finalized");
                runtime.lastAppearanceSnapshotTick = runtime.currentTick;
                runtime.recruitAppearanceFinalizeTick = 0L;
                return;
            }
            if (runtime.currentTick - runtime.lastAppearanceSnapshotTick < 40L) {
                return;
            }
            this.cacheAppearanceSnapshot(companion, npcEntity, ref, store, "periodic");
            runtime.lastAppearanceSnapshotTick = runtime.currentTick;
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public void clearLootState(PlayerRef playerRef, CompanionRecord companion) {
        if (companion == null) {
            return;
        }
        CompanionRuntimeState runtime = this.companionManager.getRuntime(companion.uniqueId);
        NPCEntity npcEntity = null;
        try {
            if (playerRef != null && playerRef.getWorldUuid() != null && companion.entityId != null) {
                Ref ref;
                World world = Universe.get().getWorld(playerRef.getWorldUuid());
                UUID entityId = this.parseUuid(companion.entityId);
                if (world != null && entityId != null && (ref = world.getEntityRef(entityId)) != null && ref.isValid()) {
                    Store store = world.getEntityStore().getStore();
                    npcEntity = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        this.clearLootState(npcEntity, companion, runtime);
    }

    private void clearLootState(NPCEntity npcEntity, CompanionRecord companion, CompanionRuntimeState runtime) {
        if (runtime == null) {
            return;
        }
        runtime.lootTargetId = null;
        runtime.lootUnreachableTicks = 0;
        runtime.lastLootScanTick = 0L;
        runtime.lastLootDebugTick = 0L;
        if (runtime.state == CompanionState.LOOT_SEEK) {
            runtime.state = switch (companion != null ? companion.followMode : FollowMode.FOLLOW) {
                case FollowMode.STAY -> CompanionState.IDLE_STAY;
                case FollowMode.PATROL -> CompanionState.IDLE_PATROL;
                case FollowMode.FREE -> CompanionState.IDLE_FREE;
                default -> CompanionState.IDLE_FOLLOW;
            };
        }
        try {
            if (npcEntity != null) {
                npcEntity.onFlockSetTarget("LootTarget", null);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            if (npcEntity != null) {
                npcEntity.onFlockSetTarget("Target", null);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            if (npcEntity != null) {
                npcEntity.onFlockSetTarget("LockedTarget", null);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            if (npcEntity != null) {
                npcEntity.getRole().getWorldSupport().requestNewPath();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private Vec3 toVec3(Vector3d v) {
        return new Vec3(v.x, v.y, v.z);
    }

    private Vector3d toVector3d(Vec3 v) {
        return new Vector3d(v.x, v.y, v.z);
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

    private static final class ActiveCompanionContext {
        final Ref<EntityStore> ref;
        final Store<EntityStore> store;

        ActiveCompanionContext(Ref<EntityStore> ref, Store<EntityStore> store) {
            this.ref = ref;
            this.store = store;
        }
    }

    private static final class AppearanceSnapshot {
        private PlayerSkin playerSkin;
        private String modelAssetId;
        private String capturedToken;
        private ModelComponent modelComponent;
        private PersistentModel persistentModel;

        private AppearanceSnapshot() {
        }
    }
}

