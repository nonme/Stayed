/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.ComponentAccessor
 *  com.hypixel.hytale.component.Holder
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.system.ISystem
 *  com.hypixel.hytale.event.EventPriority
 *  com.hypixel.hytale.protocol.InteractionType
 *  com.hypixel.hytale.protocol.ToClientPacket
 *  com.hypixel.hytale.protocol.packets.player.UpdateMemoriesFeatureStatus
 *  com.hypixel.hytale.server.core.HytaleServer
 *  com.hypixel.hytale.server.core.command.system.AbstractCommand
 *  com.hypixel.hytale.server.core.entity.Entity
 *  com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage
 *  com.hypixel.hytale.server.core.entity.nameplate.Nameplate
 *  com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent
 *  com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent
 *  com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent
 *  com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
 *  com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent
 *  com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap
 *  com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue
 *  com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes
 *  com.hypixel.hytale.server.core.modules.time.WorldTimeResource
 *  com.hypixel.hytale.server.core.plugin.JavaPlugin
 *  com.hypixel.hytale.server.core.plugin.JavaPluginInit
 *  com.hypixel.hytale.server.core.universe.Universe
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk
 *  com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent
 *  com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager$MarkerProvider
 *  com.hypixel.hytale.server.npc.NPCPlugin
 *  com.hypixel.hytale.server.npc.asset.builder.BuilderManager
 */
package com.kyuubisoft.core;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.player.UpdateMemoriesFeatureStatus;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.BuilderManager;
import com.kyuubisoft.core.api.CoreAPI;
import com.kyuubisoft.core.citizen.BuilderActionCitizenInteract;
import com.kyuubisoft.core.citizen.CitizenAdminPage;
import com.kyuubisoft.core.citizen.CitizenAnimationManager;
import com.kyuubisoft.core.citizen.CitizenAppearanceOverrideManager;
import com.kyuubisoft.core.citizen.CitizenChunkListener;
import com.kyuubisoft.core.citizen.CitizenCommand;
import com.kyuubisoft.core.citizen.CitizenDamageListener;
import com.kyuubisoft.core.citizen.CitizenData;
import com.kyuubisoft.core.citizen.CitizenHologramManager;
import com.kyuubisoft.core.citizen.CitizenMarkerProvider;
import com.kyuubisoft.core.citizen.CitizenProximityManager;
import com.kyuubisoft.core.citizen.CitizenRotationManager;
import com.kyuubisoft.core.citizen.CitizenScheduleManager;
import com.kyuubisoft.core.citizen.CitizenService;
import com.kyuubisoft.core.commands.ApplyEditsCommand;
import com.kyuubisoft.core.commands.CoreAdminCommand;
import com.kyuubisoft.core.commands.DevExportCommand;
import com.kyuubisoft.core.commands.EditorCommand;
import com.kyuubisoft.core.commands.TrustEditorCommand;
import com.kyuubisoft.core.config.CoreConfig;
import com.kyuubisoft.core.dialog.DialogService;
import com.kyuubisoft.core.economy.ExternalEconomyBridge;
import com.kyuubisoft.core.economy.ExternalEconomyCurrencyProvider;
import com.kyuubisoft.core.i18n.CoreI18n;
import com.kyuubisoft.core.i18n.PlayerPreferencesService;
import com.kyuubisoft.core.image.DynamicImageService;
import com.kyuubisoft.core.kslang.KsLang;
import com.kyuubisoft.core.lootbag.LootbagAdminService;
import com.kyuubisoft.core.registry.HudVisibilityService;
import com.kyuubisoft.core.registry.ModMenuRegistry;
import com.kyuubisoft.core.shop.ShopCommand;
import com.kyuubisoft.core.shop.ShopService;
import com.kyuubisoft.core.storage.DatabaseManager;
import com.kyuubisoft.core.storage.JsonFilePlayerDataStorage;
import com.kyuubisoft.core.storage.MySQLPlayerDataStorage;
import com.kyuubisoft.core.storage.PlayerDataStorage;
import com.kyuubisoft.core.tracking.TrackingService;
import com.kyuubisoft.core.tracking.systems.BlockBreakTrackerSystem;
import com.kyuubisoft.core.tracking.systems.BlockPlaceTrackerSystem;
import com.kyuubisoft.core.tracking.systems.CraftRecipeTrackerSystem;
import com.kyuubisoft.core.tracking.systems.DamageTrackerSystem;
import com.kyuubisoft.core.tracking.systems.DistanceTrackerSystem;
import com.kyuubisoft.core.tracking.systems.InteractivePickupTrackerSystem;
import com.kyuubisoft.core.tracking.systems.KillTrackerSystem;
import com.kyuubisoft.core.tracking.systems.ProcessingBenchOutputTracker;
import com.kyuubisoft.core.tracking.systems.ZoneDiscoveryTrackerSystem;
import com.kyuubisoft.core.ui.CoreAdminPage;
import com.kyuubisoft.core.ui.ShopAdminPage;
import com.kyuubisoft.core.webeditor.CoreConfigProvider;
import com.kyuubisoft.core.webeditor.WebEditorService;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class CorePlugin
extends JavaPlugin {
    public static final String VERSION = "2.2.3";
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core");
    private static CorePlugin instance;
    private static volatile boolean permissionsInitialized;
    private CoreConfig coreConfig;
    private LootbagAdminService lootbagAdminService;
    private TrackingService trackingService;
    private CitizenService citizenService;
    private DialogService dialogService;
    private CitizenChunkListener citizenChunkListener;
    private CitizenMarkerProvider citizenMarkerProvider;
    private final Set<String> markerProviderWorlds = ConcurrentHashMap.newKeySet();
    private DistanceTrackerSystem distanceTracker;
    private ProcessingBenchOutputTracker processingBenchTracker;
    private ShopService shopService;
    private DynamicImageService dynamicImageService;
    private DatabaseManager databaseManager;
    private PlayerDataStorage mysqlStorage;
    private PlayerPreferencesService playerPreferencesService;
    private ScheduledExecutorService scheduler;
    private WebEditorService webEditorService;
    private final Map<UUID, Player> onlinePlayers = new ConcurrentHashMap<UUID, Player>();
    private static final String[] CITIZEN_ROLE_FILES;

    public CorePlugin(JavaPluginInit init) {
        super(init);
        instance = this;
        LOGGER.info("KyuubiSoft Core loaded");
    }

    public static CorePlugin getInstance() {
        return instance;
    }

    protected void setup() {
        this.coreConfig = new CoreConfig(this.getDataDirectory());
        this.coreConfig.load();
        LOGGER.info("Core config loaded");
        this.initializeStorage();
        KsLang.init(this, "core", "Core");
        CoreI18n.getInstance().load(this.coreConfig.getLanguage(), this.getDataDirectory());
        CoreConfig.ModulesConfig modules = this.coreConfig.getModules();
        this.logModuleStatus(modules);
        if (modules.lootbags) {
            this.lootbagAdminService = new LootbagAdminService();
            this.lootbagAdminService.load(this.getDataDirectory());
            LOGGER.info("Lootbag config loaded: " + this.lootbagAdminService.getAll().size() + " definitions");
        }
        if (modules.citizens) {
            this.citizenService = new CitizenService();
            this.citizenService.load(this.getDataDirectory());
            this.citizenChunkListener = new CitizenChunkListener(this.citizenService);
            this.citizenService.setChunkListener(this.citizenChunkListener);
            LOGGER.info("CitizenService loaded: " + this.citizenService.getCitizenCount() + " citizens");
            try {
                NPCPlugin.get().registerCoreComponentType("KSCitizenInteract", BuilderActionCitizenInteract::new);
                if (CoreAPI.isDebug()) {
                    LOGGER.info("Registered KSCitizenInteract NPC action type");
                }
            }
            catch (Exception e) {
                LOGGER.warning("Failed to register KSCitizenInteract action: " + e.getMessage());
            }
            this.extractCitizenRoleFiles();
            this.citizenMarkerProvider = new CitizenMarkerProvider(this.citizenService);
        }
        if (modules.dialogs) {
            if (!modules.citizens) {
                LOGGER.warning("Module 'dialogs' enabled but 'citizens' disabled \u2014 NPC dialog features will be limited");
            }
            this.dialogService = new DialogService();
            this.dialogService.load(this.getDataDirectory());
            LOGGER.info("DialogService loaded: " + this.dialogService.getAllDialogs().size() + " dialog trees");
        }
        if (modules.shops) {
            this.shopService = new ShopService();
            this.shopService.setDataFolder(this.getDataDirectory());
            this.shopService.extractDefaults(this.getDataDirectory().resolve("shops"));
            this.shopService.loadShopsFromDirectory(this.getDataDirectory().resolve("shops"));
            LOGGER.info("ShopService initialized: " + this.shopService.getAllShops().size() + " shops loaded");
        }
        if (modules.dynamicImages) {
            this.dynamicImageService = new DynamicImageService();
            this.dynamicImageService.setAvatarCacheDir(this.getDataDirectory().resolve("avatar-cache"));
            LOGGER.info("DynamicImageService initialized");
        }
        this.getCommandRegistry().registerCommand((AbstractCommand)new CoreAdminCommand(this));
        this.getCommandRegistry().registerCommand((AbstractCommand)new DevExportCommand(this));
        if (modules.citizens) {
            this.getCommandRegistry().registerCommand((AbstractCommand)new CitizenCommand(this));
        }
        if (modules.shops) {
            this.getCommandRegistry().registerCommand((AbstractCommand)new ShopCommand());
        }
        if (modules.webEditor) {
            this.webEditorService = new WebEditorService(this.coreConfig.getWebEditor());
            this.getCommandRegistry().registerCommand((AbstractCommand)new EditorCommand(this));
            this.getCommandRegistry().registerCommand((AbstractCommand)new ApplyEditsCommand(this));
            this.getCommandRegistry().registerCommand((AbstractCommand)new TrustEditorCommand(this));
            LOGGER.info("Web Editor enabled: " + this.coreConfig.getWebEditor().getEditorUrl());
        }
        ModMenuRegistry.addConfigProvider(new CoreConfigProvider(this));
        ArrayList<String> cmds = new ArrayList<String>(List.of("/ksadmin", "/ksdev"));
        if (modules.citizens) {
            cmds.add("/kscitizen");
        }
        if (modules.shops) {
            cmds.add("/ksshop");
        }
        cmds.add("/kslang");
        if (modules.webEditor) {
            cmds.add("/kseditor");
            cmds.add("/ksapplyconfig");
            cmds.add("/kstrusteditor");
        }
        LOGGER.info("Commands registered: " + String.join((CharSequence)", ", cmds));
        ModMenuRegistry.setCurrentLanguage(this.coreConfig.getLanguage());
        ModMenuRegistry.addLanguageChangeListener(newLang -> {
            if (!newLang.equals(this.coreConfig.getLanguage())) {
                this.coreConfig.setLanguage(newLang);
                this.coreConfig.save();
                CoreI18n.getInstance().load(newLang, this.getDataDirectory());
                if (CoreAPI.isDebug()) {
                    LOGGER.info("Core language updated to: " + newLang);
                }
            }
        });
        ModMenuRegistry.addReloadHandler("core", "Core", () -> {
            StringBuilder result = new StringBuilder();
            if (this.citizenService != null) {
                this.citizenService.reloadAllWorlds();
                result.append(this.citizenService.getCitizenCount()).append(" Citizens, ");
            }
            if (this.dialogService != null) {
                this.dialogService.load(this.getDataDirectory());
                result.append(this.dialogService.getAllDialogs().size()).append(" Dialogs, ");
            }
            if (this.shopService != null) {
                this.shopService.loadShopsFromDirectory(this.getDataDirectory().resolve("shops"));
                result.append(this.shopService.getAllShops().size()).append(" Shops, ");
            }
            if (this.lootbagAdminService != null) {
                this.lootbagAdminService.load(this.getDataDirectory());
            }
            CoreI18n.getInstance().load(this.coreConfig.getLanguage(), this.getDataDirectory());
            String r = result.toString();
            return r.isEmpty() ? "Config reloaded" : r.substring(0, r.length() - 2) + " loaded";
        });
        ModMenuRegistry.setCoreAdminOpener((player, playerRef, ref, store) -> {
            CoreAdminPage page = new CoreAdminPage(this, player, playerRef);
            player.getPageManager().openCustomPage(ref, store, (CustomUIPage)page);
        });
        if (modules.tracking) {
            this.trackingService = new TrackingService();
            this.trackingService.getPlacedBlockTracker().configure(true, 30L);
            LOGGER.info("TrackingService initialized");
        }
        if (this.shopService != null) {
            this.shopService.initStorage(this.mysqlStorage);
        }
        if (modules.playerPreferences) {
            PlayerDataStorage prefsStorage = this.mysqlStorage;
            if (prefsStorage == null) {
                prefsStorage = new JsonFilePlayerDataStorage(this.getDataDirectory());
            }
            this.playerPreferencesService = new PlayerPreferencesService(prefsStorage);
            LOGGER.info("PlayerPreferencesService initialized (" + prefsStorage.getTypeName() + ")");
        }
        CoreAPI.init(this);
    }

    protected void start() {
        LOGGER.info("Starting KyuubiSoft Core...");
        this.loadCitizenRoles();
        try {
            this.getEventRegistry().register(PlayerConnectEvent.class, event -> {
                World world;
                UUID playerId = event.getPlayerRef().getUuid();
                this.onlinePlayers.put(playerId, event.getPlayer());
                if (this.playerPreferencesService != null) {
                    this.playerPreferencesService.loadPlayer(playerId, event.getPlayerRef().getUsername());
                }
                try {
                    event.getPlayerRef().getPacketHandler().write((ToClientPacket)new UpdateMemoriesFeatureStatus(false));
                }
                catch (Exception e) {
                    LOGGER.fine("Failed to send UpdateMemoriesFeatureStatus: " + e.getMessage());
                }
                if (this.citizenMarkerProvider != null) {
                    try {
                        World defaultWorld;
                        Universe universe = Universe.get();
                        if (universe != null && (defaultWorld = universe.getDefaultWorld()) != null) {
                            this.registerMarkerProviderOnWorld(defaultWorld);
                        }
                    }
                    catch (Exception e) {
                        LOGGER.warning("Could not register marker provider: " + e.getMessage());
                    }
                }
                if (this.citizenService != null && (world = event.getPlayer().getWorld()) != null) {
                    HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> world.execute(() -> {
                        this.citizenService.spawnAllInWorld(world);
                        this.runLegacyHologramCleanup(world);
                    }), 2L, TimeUnit.SECONDS);
                    HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> world.execute(() -> this.citizenService.cleanupStaleEntities(world)), 7L, TimeUnit.SECONDS);
                    HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> world.execute(() -> this.citizenService.cleanupOrphanEntities(world)), 12L, TimeUnit.SECONDS);
                }
                this.resetPlayerNameplate(event.getPlayer());
            });
            this.getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
                UUID playerId = event.getPlayerRef().getUuid();
                this.onlinePlayers.remove(playerId);
                if (this.playerPreferencesService != null) {
                    this.playerPreferencesService.savePlayer(playerId, event.getPlayerRef().getUsername());
                    this.playerPreferencesService.clearPlayer(playerId);
                }
                if (this.trackingService != null) {
                    this.trackingService.clearPlayer(playerId);
                }
                if (this.distanceTracker != null) {
                    this.distanceTracker.clearPlayer(playerId);
                }
                if (this.processingBenchTracker != null) {
                    this.processingBenchTracker.clearPlayer(playerId);
                }
                if (this.shopService != null) {
                    this.shopService.clearPlayer(playerId);
                }
                if (this.dynamicImageService != null) {
                    this.dynamicImageService.releasePlayerImages(playerId);
                }
                HudVisibilityService.clearPlayer(playerId);
                if (this.onlinePlayers.isEmpty() && this.citizenService != null) {
                    try {
                        Universe universe = Universe.get();
                        if (universe != null) {
                            for (World w : universe.getWorlds().values()) {
                                w.execute(() -> {
                                    if (this.onlinePlayers.isEmpty()) {
                                        this.citizenService.resetAllSpawnStates(w);
                                    }
                                });
                            }
                        } else {
                            this.citizenService.resetAllSpawnStates();
                        }
                    }
                    catch (Exception e) {
                        this.citizenService.resetAllSpawnStates();
                    }
                    this.citizenService.saveState();
                }
            });
            this.getEventRegistry().registerGlobal(DrainPlayerFromWorldEvent.class, event -> {
                if (this.citizenService == null) {
                    return;
                }
                World drainedWorld = event.getWorld();
                if (drainedWorld == null) {
                    return;
                }
                try {
                    if (drainedWorld.getPlayerCount() <= 1) {
                        String worldName = drainedWorld.getName();
                        int cleared = 0;
                        for (CitizenData citizen : this.citizenService.getAllCitizens()) {
                            if (!CitizenService.matchesWorld(citizen.worldName, worldName) || citizen.entityRef == null && citizen.cachedNetworkId == 0) continue;
                            citizen.entityRef = null;
                            citizen.cachedNetworkId = 0;
                            if (this.citizenService.getAnimationManager() != null) {
                                this.citizenService.getAnimationManager().unregister(citizen.id);
                            }
                            ++cleared;
                        }
                        if (cleared > 0 && CoreAPI.isDebug()) {
                            LOGGER.info("Last player leaving " + worldName + " \u2014 cleared " + cleared + " citizen refs (UUIDs preserved for reattach)");
                        }
                    }
                }
                catch (Exception e) {
                    LOGGER.fine("DrainPlayerFromWorld citizen cleanup error: " + e.getMessage());
                }
            });
            this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, event -> {
                World w = event.getWorld();
                if (w == null) {
                    return;
                }
                if (!permissionsInitialized) {
                    try {
                        Player p;
                        Holder holder = event.getHolder();
                        if (holder != null && (p = (Player)holder.getComponent(Player.getComponentType())) != null) {
                            permissionsInitialized = true;
                            for (String node : new String[]{"ks.shop.user.use"}) {
                                try {
                                    p.hasPermission(node, true);
                                }
                                catch (Exception exception) {
                                    // empty catch block
                                }
                            }
                        }
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                if (this.citizenMarkerProvider != null) {
                    this.registerMarkerProviderOnWorld(w);
                }
                if (this.citizenService != null) {
                    HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> w.execute(() -> this.citizenService.spawnAllInWorld(w)), 2L, TimeUnit.SECONDS);
                    HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> w.execute(() -> {
                        this.citizenService.cleanupStaleEntities(w);
                        this.citizenService.cleanupOrphanEntities(w);
                    }), 10L, TimeUnit.SECONDS);
                }
            });
            LOGGER.info("Player events registered (tracking cache)");
            this.getEventRegistry().registerGlobal(PlayerInteractEvent.class, event -> {
                if (event.getActionType() == InteractionType.Use) {
                    LOGGER.fine("[F-Key] PlayerInteractEvent: cancelled=" + event.isCancelled() + " targetEntity=" + (event.getTargetEntity() != null ? event.getTargetEntity().getClass().getSimpleName() : "null") + " targetRef=" + (event.getTargetRef() != null ? "valid" : "null") + " targetBlock=" + String.valueOf(event.getTargetBlock()));
                }
                if (event.isCancelled()) {
                    return;
                }
                if (event.getActionType() != InteractionType.Use) {
                    return;
                }
                if (this.citizenService == null) {
                    return;
                }
                Ref targetRef = event.getTargetRef();
                Entity targetEntity = event.getTargetEntity();
                if (targetRef == null && targetEntity == null) {
                    return;
                }
                CitizenData citizen = this.citizenService.getCitizenByEntityRef(targetRef, targetEntity);
                if (citizen == null) {
                    LOGGER.fine("[F-Key] Entity found but not a citizen (targetEntity=" + (String)(targetEntity != null ? targetEntity.getClass().getSimpleName() + " uuid=" + String.valueOf(targetEntity.getUuid()) : "null") + ")");
                    return;
                }
                if (!citizen.fKeyInteractionEnabled) {
                    return;
                }
                if (citizen.getMovementType() == CitizenData.MovementType.IDLE) {
                    return;
                }
                Player player = event.getPlayer();
                if (player == null) {
                    return;
                }
                if (CoreAPI.isDebug()) {
                    LOGGER.info("[F-Key] PlayerInteractEvent fallback: " + citizen.id + " by " + player.getPlayerRef().getUsername());
                }
                this.citizenService.dispatchInteract(player, citizen.id);
            });
            this.getEventRegistry().registerGlobal(EventPriority.LAST, ChunkPreLoadProcessEvent.class, event -> {
                WorldChunk chunk;
                if (this.citizenChunkListener != null && (chunk = event.getChunk()) != null && chunk.getWorld() != null) {
                    this.citizenChunkListener.onChunkPreLoad(chunk.getWorld(), chunk.getIndex());
                }
            });
            LOGGER.info("Citizen chunk listener registered");
            if (this.shopService != null) {
                ExternalEconomyBridge economyBridge = ExternalEconomyBridge.getInstance();
                economyBridge.initialize(this.coreConfig.getEconomyProvider());
                if (economyBridge.isAvailable()) {
                    this.shopService.registerCurrency(new ExternalEconomyCurrencyProvider());
                    LOGGER.info("External economy registered as shop currency: " + String.valueOf((Object)economyBridge.getActiveBackend()) + " (" + economyBridge.getProviderName() + ")");
                }
            }
            if (this.trackingService != null) {
                this.getEntityStoreRegistry().registerSystem((ISystem)new BlockBreakTrackerSystem());
                this.getEntityStoreRegistry().registerSystem((ISystem)new BlockPlaceTrackerSystem());
                this.getEntityStoreRegistry().registerSystem((ISystem)new KillTrackerSystem());
                this.getEntityStoreRegistry().registerSystem((ISystem)new DamageTrackerSystem());
                this.distanceTracker = new DistanceTrackerSystem();
                this.getEntityStoreRegistry().registerSystem((ISystem)this.distanceTracker);
                this.getEntityStoreRegistry().registerSystem((ISystem)new InteractivePickupTrackerSystem());
                this.getEntityStoreRegistry().registerSystem((ISystem)new ZoneDiscoveryTrackerSystem());
                this.getEntityStoreRegistry().registerSystem((ISystem)new CraftRecipeTrackerSystem());
                LOGGER.info("ECS tracking systems registered (block, kill, damage, distance, harvest, zone, craft)");
                this.processingBenchTracker = new ProcessingBenchOutputTracker();
                this.getEntityStoreRegistry().registerSystem((ISystem)this.processingBenchTracker);
                LOGGER.info("ProcessingBench output tracker registered (ECS)");
            }
            if (this.citizenService != null) {
                this.getEntityStoreRegistry().registerSystem((ISystem)new CitizenDamageListener());
            }
            this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Core-Scheduler");
                t.setDaemon(true);
                return t;
            });
            if (this.trackingService != null) {
                this.scheduler.scheduleAtFixedRate(this::tickPlaytime, 60L, 60L, TimeUnit.SECONDS);
                this.scheduler.scheduleAtFixedRate(this.trackingService.getPlacedBlockTracker()::cleanupExpired, 5L, 5L, TimeUnit.MINUTES);
                if (CoreAPI.isDebug()) {
                    LOGGER.info("Tracking schedulers started (playtime, block cleanup)");
                }
            }
            if (this.citizenService != null) {
                this.scheduler.scheduleAtFixedRate(this::tickCitizenAnimations, 1L, 500L, TimeUnit.MILLISECONDS);
                this.scheduler.scheduleAtFixedRate(this::tickCitizenRotation, 1L, 100L, TimeUnit.MILLISECONDS);
                this.scheduler.scheduleAtFixedRate(this::tickCitizenPausedState, 1L, 50L, TimeUnit.MILLISECONDS);
                this.scheduler.scheduleAtFixedRate(this::tickCitizenHealth, 5L, 5L, TimeUnit.SECONDS);
                this.scheduler.scheduleAtFixedRate(this::tickCitizenEffects, 1L, 1L, TimeUnit.SECONDS);
                this.scheduler.scheduleAtFixedRate(this::tickCitizenProximity, 2L, 500L, TimeUnit.MILLISECONDS);
                this.scheduler.scheduleAtFixedRate(this::tickCitizenSchedule, 5L, 10L, TimeUnit.SECONDS);
                this.scheduler.scheduleAtFixedRate(() -> {
                    if (!this.onlinePlayers.isEmpty()) {
                        try {
                            this.citizenService.save();
                        }
                        catch (Exception e) {
                            LOGGER.fine("Periodic citizen save failed: " + e.getMessage());
                        }
                    }
                }, 60L, 60L, TimeUnit.SECONDS);
                if (this.citizenService.getSkinManager() != null) {
                    this.scheduler.scheduleAtFixedRate(this.citizenService.getSkinManager()::cleanupCache, 10L, 10L, TimeUnit.MINUTES);
                }
                if (CoreAPI.isDebug()) {
                    LOGGER.info("Citizen schedulers started (animation, rotation, health, effects, proximity, schedule, save, skin)");
                }
            }
            this.scheduler.scheduleAtFixedRate(() -> HudVisibilityService.tick(this.onlinePlayers.values()), 2L, 500L, TimeUnit.MILLISECONDS);
            if (this.webEditorService != null) {
                this.webEditorService.setConfigProviders(ModMenuRegistry.getConfigProviders());
                LOGGER.info("Web Editor: " + ModMenuRegistry.getConfigProviders().size() + " config providers registered");
                this.scheduler.scheduleAtFixedRate(() -> {
                    try {
                        this.webEditorService.cleanupExpiredSessions();
                    }
                    catch (Exception e) {
                        LOGGER.fine("Editor session cleanup failed: " + e.getMessage());
                    }
                }, 5L, 5L, TimeUnit.MINUTES);
            }
            CoreAPI.registerNpcPageOpener("showcase_admin", (player, playerRef, ref, store) -> {
                CoreAdminPage page = new CoreAdminPage(this, player, playerRef);
                player.getPageManager().openCustomPage(ref, store, (CustomUIPage)page);
            });
            if (this.citizenService != null) {
                CoreAPI.registerNpcPageOpener("showcase_citizens", (player, playerRef, ref, store) -> {
                    CitizenAdminPage page = new CitizenAdminPage(this, player, playerRef);
                    player.getPageManager().openCustomPage(ref, store, (CustomUIPage)page);
                });
            }
            if (this.shopService != null) {
                CoreAPI.registerNpcPageOpener("showcase_shops", (player, playerRef, ref, store) -> {
                    ShopAdminPage page = new ShopAdminPage(this, player, playerRef);
                    player.getPageManager().openCustomPage(ref, store, (CustomUIPage)page);
                });
            }
            LOGGER.info("KyuubiSoft Core started!");
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start Core", e);
        }
    }

    protected void shutdown() {
        LOGGER.info("Shutting down KyuubiSoft Core...");
        if (this.webEditorService != null) {
            this.webEditorService.shutdown();
            LOGGER.info("Web Editor sessions closed");
        }
        if (this.scheduler != null) {
            this.scheduler.shutdown();
            try {
                if (!this.scheduler.awaitTermination(5L, TimeUnit.SECONDS)) {
                    this.scheduler.shutdownNow();
                }
            }
            catch (InterruptedException e) {
                this.scheduler.shutdownNow();
            }
        }
        if (this.citizenService != null) {
            block13: {
                this.citizenService.saveState();
                LOGGER.info("Citizen state saved on shutdown");
                try {
                    Map worlds;
                    Universe universe = Universe.get();
                    if (universe == null || (worlds = universe.getWorlds()) == null || worlds.isEmpty()) break block13;
                    CountDownLatch latch = new CountDownLatch(worlds.size());
                    for (World w : worlds.values()) {
                        w.execute(() -> {
                            try {
                                this.citizenService.despawnAllInWorld(w);
                            }
                            finally {
                                latch.countDown();
                            }
                        });
                    }
                    try {
                        latch.await(5L, TimeUnit.SECONDS);
                    }
                    catch (InterruptedException interruptedException) {
                        // empty catch block
                    }
                    LOGGER.info("Citizen entities removed from world store");
                }
                catch (Exception e) {
                    LOGGER.fine("Could not despawn citizens with world reference: " + e.getMessage());
                }
            }
            this.citizenService.despawnAll();
        }
        if (this.dynamicImageService != null) {
            this.dynamicImageService.shutdown();
        }
        if (this.databaseManager != null) {
            this.databaseManager.shutdown();
            LOGGER.info("Database connection pool closed");
        }
        this.onlinePlayers.clear();
        LOGGER.info("KyuubiSoft Core shutdown complete!");
    }

    private void logModuleStatus(CoreConfig.ModulesConfig m) {
        LOGGER.info("Modules: citizens=" + (m.citizens ? "ON" : "OFF") + ", dialogs=" + (m.dialogs ? "ON" : "OFF") + ", shops=" + (m.shops ? "ON" : "OFF") + ", lootbags=" + (m.lootbags ? "ON" : "OFF") + ", tracking=" + (m.tracking ? "ON" : "OFF") + ", webEditor=" + (m.webEditor ? "ON" : "OFF") + ", dynamicImages=" + (m.dynamicImages ? "ON" : "OFF") + ", playerPreferences=" + (m.playerPreferences ? "ON" : "OFF"));
    }

    private void tickPlaytime() {
        if (this.trackingService == null) {
            return;
        }
        for (Player player : this.onlinePlayers.values()) {
            try {
                this.trackingService.dispatchPlaytimeMinute(player);
            }
            catch (Exception e) {
                LOGGER.fine("Playtime tick error: " + e.getMessage());
            }
        }
    }

    private void tickCitizenAnimations() {
        if (this.citizenService == null || this.onlinePlayers.isEmpty()) {
            return;
        }
        CitizenAnimationManager animManager = this.citizenService.getAnimationManager();
        if (animManager == null) {
            return;
        }
        try {
            Player firstPlayer = this.onlinePlayers.values().iterator().next();
            World world = firstPlayer.getWorld();
            if (world == null) {
                return;
            }
            world.execute(() -> {
                try {
                    animManager.tick(this.citizenService.getAllCitizens(), this.onlinePlayers.values());
                }
                catch (Exception e) {
                    LOGGER.fine("Citizen animation tick error: " + e.getMessage());
                }
            });
        }
        catch (Exception e) {
            LOGGER.fine("Citizen animation tick scheduling error: " + e.getMessage());
        }
    }

    private void tickCitizenRotation() {
        if (this.citizenService == null || this.onlinePlayers.isEmpty()) {
            return;
        }
        CitizenRotationManager rotManager = this.citizenService.getRotationManager();
        if (rotManager == null) {
            return;
        }
        try {
            rotManager.tick(this.citizenService.getAllCitizens(), this.onlinePlayers.values());
        }
        catch (Exception e) {
            LOGGER.fine("Citizen rotation tick error: " + e.getMessage());
        }
    }

    private void tickCitizenPausedState() {
        if (this.citizenService == null || this.onlinePlayers.isEmpty()) {
            return;
        }
        try {
            Player firstPlayer = this.onlinePlayers.values().iterator().next();
            World world = firstPlayer.getWorld();
            if (world == null) {
                return;
            }
            world.execute(() -> {
                try {
                    this.citizenService.tickPausedCitizens(world, this.onlinePlayers.values());
                }
                catch (Exception e) {
                    LOGGER.fine("Citizen paused tick error: " + e.getMessage());
                }
            });
        }
        catch (Exception e) {
            LOGGER.fine("Citizen paused tick scheduling error: " + e.getMessage());
        }
    }

    private void tickCitizenHealth() {
        if (this.citizenService == null || this.onlinePlayers.isEmpty()) {
            return;
        }
        try {
            Player firstPlayer = this.onlinePlayers.values().iterator().next();
            World world = firstPlayer.getWorld();
            if (world == null) {
                return;
            }
            world.execute(() -> {
                try {
                    Store store = world.getEntityStore().getStore();
                    for (CitizenData citizen : this.citizenService.getAllCitizens()) {
                        EntityStatValue health;
                        EntityStatMap statMap;
                        if (citizen.takesDamage || citizen.entityRef == null || !citizen.entityRef.isValid() || (statMap = (EntityStatMap)store.getComponent(citizen.entityRef, EntityStatMap.getComponentType())) == null || (health = statMap.get(DefaultEntityStatTypes.getHealth())) == null || !(health.get() < health.getMax())) continue;
                        statMap.maximizeStatValue(DefaultEntityStatTypes.getHealth());
                    }
                }
                catch (Exception e) {
                    LOGGER.fine("Citizen health tick error: " + e.getMessage());
                }
            });
        }
        catch (Exception e) {
            LOGGER.fine("Citizen health tick scheduling error: " + e.getMessage());
        }
    }

    private void tickCitizenEffects() {
        if (this.citizenService == null || this.onlinePlayers.isEmpty()) {
            return;
        }
        try {
            Player firstPlayer = this.onlinePlayers.values().iterator().next();
            World world = firstPlayer.getWorld();
            if (world == null) {
                return;
            }
            world.execute(() -> {
                try {
                    Store store = world.getEntityStore().getStore();
                    for (CitizenData citizen : this.citizenService.getAllCitizens()) {
                        if (citizen.takesDamage || citizen.entityRef == null || !citizen.entityRef.isValid()) continue;
                        try {
                            EffectControllerComponent effectCtrl = (EffectControllerComponent)store.getComponent(citizen.entityRef, EffectControllerComponent.getComponentType());
                            if (effectCtrl == null || effectCtrl.getActiveEffects() == null || effectCtrl.getActiveEffects().isEmpty()) continue;
                            effectCtrl.clearEffects(citizen.entityRef, (ComponentAccessor)store);
                        }
                        catch (Exception exception) {}
                    }
                }
                catch (Exception e) {
                    LOGGER.fine("Citizen effect tick error: " + e.getMessage());
                }
            });
        }
        catch (Exception e) {
            LOGGER.fine("Citizen effect tick scheduling error: " + e.getMessage());
        }
    }

    private void tickCitizenProximity() {
        if (this.citizenService == null || this.onlinePlayers.isEmpty()) {
            return;
        }
        CitizenProximityManager proxManager = this.citizenService.getProximityManager();
        if (proxManager == null) {
            return;
        }
        try {
            Player firstPlayer = this.onlinePlayers.values().iterator().next();
            World world = firstPlayer.getWorld();
            if (world == null) {
                return;
            }
            world.execute(() -> {
                try {
                    proxManager.tick(this.citizenService.getAllCitizens(), this.onlinePlayers.values(), this.citizenService);
                }
                catch (Exception e) {
                    LOGGER.fine("Citizen proximity tick error: " + e.getMessage());
                }
            });
        }
        catch (Exception e) {
            LOGGER.fine("Citizen proximity tick scheduling error: " + e.getMessage());
        }
    }

    private void tickCitizenSchedule() {
        if (this.citizenService == null || this.onlinePlayers.isEmpty()) {
            return;
        }
        CitizenScheduleManager schedManager = this.citizenService.getScheduleManager();
        if (schedManager == null) {
            return;
        }
        try {
            Player firstPlayer = this.onlinePlayers.values().iterator().next();
            World world = firstPlayer.getWorld();
            if (world == null) {
                return;
            }
            world.execute(() -> {
                try {
                    Store store = world.getEntityStore().getStore();
                    WorldTimeResource timeResource = (WorldTimeResource)store.getResource(WorldTimeResource.getResourceType());
                    if (timeResource == null) {
                        return;
                    }
                    float dayProgress = timeResource.getDayProgress();
                    schedManager.tick(this.citizenService.getAllCitizens(), this.citizenService, dayProgress);
                    CitizenAppearanceOverrideManager appearanceManager = this.citizenService.getAppearanceOverrideManager();
                    if (appearanceManager != null) {
                        appearanceManager.tick(this.citizenService.getAllCitizens(), schedManager, this.citizenService.getBeaconManager());
                        Set<String> pending = appearanceManager.drainPendingRespawns();
                        for (String citizenId : pending) {
                            CitizenData cd = this.citizenService.getCitizen(citizenId);
                            if (cd == null || cd.spawnedEntityUUID == null) continue;
                            this.citizenService.despawnCitizen(cd, world);
                            this.citizenService.spawnCitizen(cd, world);
                        }
                    }
                }
                catch (Exception e) {
                    LOGGER.fine("Citizen schedule tick error: " + e.getMessage());
                }
            });
        }
        catch (Exception e) {
            LOGGER.fine("Citizen schedule tick scheduling error: " + e.getMessage());
        }
    }

    private void extractCitizenRoleFiles() {
        try {
            Path rolesDir = this.getDataDirectory().resolve("npc-roles");
            if (Files.isDirectory(rolesDir, new LinkOption[0])) {
                try (Stream<Path> files = Files.list(rolesDir);){
                    files.filter(p -> p.getFileName().toString().startsWith("KS_NPC_") && p.getFileName().toString().endsWith(".json")).forEach(p -> {
                        try {
                            Files.delete(p);
                        }
                        catch (Exception exception) {
                            // empty catch block
                        }
                    });
                }
            }
            Files.createDirectories(rolesDir, new FileAttribute[0]);
            for (String fileName : CITIZEN_ROLE_FILES) {
                Path targetFile = rolesDir.resolve(fileName);
                try (InputStream is = ((Object)((Object)this)).getClass().getResourceAsStream("/citizen-roles/" + fileName);){
                    if (is == null) continue;
                    byte[] content = is.readAllBytes();
                    if (content.length >= 3 && (content[0] & 0xFF) == 239 && (content[1] & 0xFF) == 187 && (content[2] & 0xFF) == 191) {
                        content = Arrays.copyOfRange(content, 3, content.length);
                    }
                    Files.write(targetFile, content, new OpenOption[0]);
                }
            }
        }
        catch (Exception e) {
            LOGGER.warning("Failed to extract citizen role files: " + e.getMessage());
        }
    }

    private void loadCitizenRoles() {
        try {
            NPCPlugin npcPlugin = NPCPlugin.get();
            if (npcPlugin == null) {
                LOGGER.warning("NPCPlugin not available \u2014 cannot load citizen roles");
                return;
            }
            BuilderManager builderManager = npcPlugin.getBuilderManager();
            Path rolesDir = this.getDataDirectory().resolve("npc-roles");
            int loaded = 0;
            int skipped = 0;
            for (String fileName : CITIZEN_ROLE_FILES) {
                ArrayList errors;
                String roleName = fileName.replace(".json", "");
                Path targetFile = rolesDir.resolve(fileName);
                if (!Files.exists(targetFile, new LinkOption[0])) {
                    LOGGER.warning("Role file not found on disk: " + String.valueOf(targetFile));
                    ++skipped;
                    continue;
                }
                boolean forceReload = npcPlugin.getIndex(roleName) >= 0;
                int roleIndex = builderManager.loadFile(targetFile, forceReload, errors = new ArrayList());
                if (roleIndex >= 0 && errors.isEmpty()) {
                    ++loaded;
                    LOGGER.info("Loaded citizen role: " + roleName + " (index=" + roleIndex + ", forceReload=" + forceReload + ")");
                    continue;
                }
                ++skipped;
                LOGGER.warning("FAILED to load role " + roleName + " (index=" + roleIndex + ", forceReload=" + forceReload + "): " + String.valueOf(errors));
            }
            LOGGER.info("Citizen roles: " + loaded + " loaded" + (String)(skipped > 0 ? ", " + skipped + " failed" : ""));
            ArrayList<String> missing = new ArrayList<String>();
            for (String fileName : CITIZEN_ROLE_FILES) {
                String roleName = fileName.replace(".json", "");
                if (npcPlugin.getIndex(roleName) >= 0) continue;
                missing.add(roleName);
            }
            if (!missing.isEmpty()) {
                LOGGER.warning("MISSING citizen roles after load: " + String.valueOf(missing) + " \u2014 Try deleting the npc-roles/ directory and restarting");
            }
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load citizen roles", e);
        }
    }

    private void initializeStorage() {
        String storageType = this.coreConfig.getStorageType();
        if ("mysql".equalsIgnoreCase(storageType)) {
            try {
                CoreConfig.MySQLConfig mysqlConfig = this.coreConfig.getStorageConfig().mysql;
                this.databaseManager = new DatabaseManager();
                this.databaseManager.initialize(mysqlConfig.host, mysqlConfig.port, mysqlConfig.database, mysqlConfig.username, mysqlConfig.password, mysqlConfig.maxPoolSize);
                this.mysqlStorage = new MySQLPlayerDataStorage(this.databaseManager, mysqlConfig.tablePrefix);
                LOGGER.info("Storage backend: MySQL (" + mysqlConfig.host + ":" + mysqlConfig.port + "/" + mysqlConfig.database + ")");
            }
            catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to initialize MySQL storage \u2014 falling back to file-based storage", e);
                this.databaseManager = null;
                this.mysqlStorage = null;
            }
        } else {
            LOGGER.info("Storage backend: File (local JSON)");
        }
    }

    public PlayerDataStorage getMySQLStorage() {
        return this.mysqlStorage;
    }

    public boolean isMySQLStorageActive() {
        return this.mysqlStorage != null;
    }

    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public CoreConfig getCoreConfig() {
        return this.coreConfig;
    }

    public WebEditorService getWebEditorService() {
        return this.webEditorService;
    }

    public LootbagAdminService getLootbagAdminService() {
        return this.lootbagAdminService;
    }

    public CoreI18n getI18n() {
        return CoreI18n.getInstance();
    }

    public PlayerPreferencesService getPlayerPreferencesService() {
        return this.playerPreferencesService;
    }

    public TrackingService getTrackingService() {
        return this.trackingService;
    }

    public CitizenService getCitizenService() {
        return this.citizenService;
    }

    public DialogService getDialogService() {
        return this.dialogService;
    }

    public ShopService getShopService() {
        return this.shopService;
    }

    public Collection<Player> getOnlinePlayers() {
        return this.onlinePlayers.values();
    }

    public Logger getPluginLogger() {
        return LOGGER;
    }

    public void resetPlayerNameplate(Player player) {
        World world = player.getWorld();
        if (world == null) {
            return;
        }
        world.execute(() -> {
            try {
                Store store = world.getEntityStore().getStore();
                Ref ref = player.getReference();
                Nameplate nameplate = (Nameplate)store.getComponent(ref, Nameplate.getComponentType());
                if (nameplate != null) {
                    String currentText = nameplate.getText();
                    String username = player.getDisplayName();
                    if (currentText != null && !currentText.equals(username)) {
                        nameplate.setText(username);
                        if (CoreAPI.isDebug()) {
                            LOGGER.info("Reset nameplate for " + username + " (was: '" + currentText + "')");
                        }
                    }
                }
            }
            catch (Exception e) {
                LOGGER.fine("Nameplate reset failed: " + e.getMessage());
            }
        });
    }

    private void runLegacyHologramCleanup(World world) {
        Path marker = this.getDataDirectory().resolve(".legacy_holograms_cleaned");
        if (Files.exists(marker, new LinkOption[0])) {
            return;
        }
        CitizenHologramManager holoManager = this.citizenService.getHologramManager();
        if (holoManager == null) {
            return;
        }
        int removed = holoManager.cleanupLegacyHolograms(world);
        if (removed > 0 && CoreAPI.isDebug()) {
            LOGGER.info("Cleaned up " + removed + " legacy hologram entities in " + world.getName());
        }
        try {
            Files.createFile(marker, new FileAttribute[0]);
            if (CoreAPI.isDebug()) {
                LOGGER.info("Legacy hologram cleanup complete \u2014 marker set");
            }
        }
        catch (Exception e) {
            LOGGER.fine("Could not create cleanup marker: " + e.getMessage());
        }
    }

    private void registerMarkerProviderOnWorld(World world) {
        if (this.citizenMarkerProvider == null || world == null) {
            return;
        }
        String worldName = world.getName();
        if (worldName == null || this.markerProviderWorlds.contains(worldName)) {
            return;
        }
        try {
            world.getWorldMapManager().addMarkerProvider("citizen_markers", (WorldMapManager.MarkerProvider)this.citizenMarkerProvider);
            this.markerProviderWorlds.add(worldName);
            if (CoreAPI.isDebug()) {
                LOGGER.info("CitizenMarkerProvider registered on world: " + worldName);
            }
        }
        catch (Exception e) {
            LOGGER.warning("Could not register marker provider on " + worldName + ": " + e.getMessage());
        }
    }

    static {
        permissionsInitialized = false;
        CITIZEN_ROLE_FILES = new String[]{"KS_NPC_Idle_Role.json", "KS_NPC_Interactable_Role.json", "KS_NPC_Wander_R2_Role.json", "KS_NPC_Wander_R5_Role.json", "KS_NPC_Wander_R10_Role.json", "KS_NPC_Wander_R15_Role.json", "KS_NPC_WanderI_R2_Interactable_Role.json", "KS_NPC_WanderI_R5_Interactable_Role.json", "KS_NPC_WanderI_R10_Interactable_Role.json", "KS_NPC_WanderI_R15_Interactable_Role.json"};
    }
}

