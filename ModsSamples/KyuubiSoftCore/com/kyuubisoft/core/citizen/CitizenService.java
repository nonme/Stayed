/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.assetstore.map.DefaultAssetMap
 *  com.hypixel.hytale.component.Archetype
 *  com.hypixel.hytale.component.Component
 *  com.hypixel.hytale.component.ComponentAccessor
 *  com.hypixel.hytale.component.ComponentType
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.RemoveReason
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.function.consumer.TriConsumer
 *  com.hypixel.hytale.math.util.ChunkUtil
 *  com.hypixel.hytale.math.vector.Transform
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.math.vector.Vector3f
 *  com.hypixel.hytale.protocol.AnimationSlot
 *  com.hypixel.hytale.protocol.Direction
 *  com.hypixel.hytale.protocol.ParticleSystem
 *  com.hypixel.hytale.protocol.PlayerSkin
 *  com.hypixel.hytale.protocol.Position
 *  com.hypixel.hytale.protocol.ToClientPacket
 *  com.hypixel.hytale.protocol.UpdateType
 *  com.hypixel.hytale.protocol.packets.assets.UpdateParticleSystems
 *  com.hypixel.hytale.protocol.packets.world.SpawnParticleSystem
 *  com.hypixel.hytale.server.core.HytaleServer
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.asset.type.model.config.Model
 *  com.hypixel.hytale.server.core.asset.type.model.config.Model$ModelReference
 *  com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem
 *  com.hypixel.hytale.server.core.cosmetics.CosmeticsModule
 *  com.hypixel.hytale.server.core.entity.AnimationUtils
 *  com.hypixel.hytale.server.core.entity.Entity
 *  com.hypixel.hytale.server.core.entity.Frozen
 *  com.hypixel.hytale.server.core.entity.UUIDComponent
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage
 *  com.hypixel.hytale.server.core.inventory.Inventory
 *  com.hypixel.hytale.server.core.inventory.ItemStack
 *  com.hypixel.hytale.server.core.modules.entity.component.Interactable
 *  com.hypixel.hytale.server.core.modules.entity.component.PersistentModel
 *  com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
 *  com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId
 *  com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap
 *  com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes
 *  com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.Universe
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.path.WorldPath
 *  com.hypixel.hytale.server.core.universe.world.path.WorldPathConfig
 *  com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  com.hypixel.hytale.server.npc.NPCPlugin
 *  com.hypixel.hytale.server.npc.corecomponents.world.BodyMotionPath
 *  com.hypixel.hytale.server.npc.entities.NPCEntity
 *  com.hypixel.hytale.server.npc.movement.MotionKind
 *  com.hypixel.hytale.server.npc.movement.controllers.MotionController
 *  com.hypixel.hytale.server.npc.movement.controllers.MotionControllerBase
 *  com.hypixel.hytale.server.npc.role.Role
 *  it.unimi.dsi.fastutil.Pair
 */
package com.kyuubisoft.core.citizen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.function.consumer.TriConsumer;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.ParticleSystem;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.UpdateType;
import com.hypixel.hytale.protocol.packets.assets.UpdateParticleSystems;
import com.hypixel.hytale.protocol.packets.world.SpawnParticleSystem;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.path.WorldPath;
import com.hypixel.hytale.server.core.universe.world.path.WorldPathConfig;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.corecomponents.world.BodyMotionPath;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.movement.MotionKind;
import com.hypixel.hytale.server.npc.movement.controllers.MotionController;
import com.hypixel.hytale.server.npc.movement.controllers.MotionControllerBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.kyuubisoft.core.api.CoreAPI;
import com.kyuubisoft.core.citizen.CitizenAnimationManager;
import com.kyuubisoft.core.citizen.CitizenAppearanceOverrideManager;
import com.kyuubisoft.core.citizen.CitizenBankHandler;
import com.kyuubisoft.core.citizen.CitizenBeaconManager;
import com.kyuubisoft.core.citizen.CitizenChunkListener;
import com.kyuubisoft.core.citizen.CitizenData;
import com.kyuubisoft.core.citizen.CitizenDialogInterceptor;
import com.kyuubisoft.core.citizen.CitizenEmoteManager;
import com.kyuubisoft.core.citizen.CitizenHologramManager;
import com.kyuubisoft.core.citizen.CitizenListener;
import com.kyuubisoft.core.citizen.CitizenMHUDIntegration;
import com.kyuubisoft.core.citizen.CitizenProximityManager;
import com.kyuubisoft.core.citizen.CitizenRotationManager;
import com.kyuubisoft.core.citizen.CitizenScheduleManager;
import com.kyuubisoft.core.citizen.CitizenSkinManager;
import com.kyuubisoft.core.citizen.NpcViewerScanner;
import com.kyuubisoft.core.citizen.QuestMarkerManager;
import com.kyuubisoft.core.citizen.WaypointRecorderHud;
import com.kyuubisoft.core.dialog.DialogCondition;
import com.kyuubisoft.core.dialog.DialogService;
import com.kyuubisoft.core.shop.ShopService;
import com.kyuubisoft.core.util.CommandUtils;
import it.unimi.dsi.fastutil.Pair;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CitizenService {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Citizens");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static CitizenService instance;
    private final Map<String, CitizenData> citizens = new ConcurrentHashMap<String, CitizenData>();
    private final List<CitizenListener> listeners = new CopyOnWriteArrayList<CitizenListener>();
    private final List<CitizenDialogInterceptor> dialogInterceptors = new CopyOnWriteArrayList<CitizenDialogInterceptor>();
    private volatile CitizenBankHandler bankHandler;
    private Path dataFolder;
    private CitizenSkinManager skinManager;
    private CitizenAnimationManager animationManager;
    private CitizenRotationManager rotationManager;
    private CitizenHologramManager hologramManager;
    private QuestMarkerManager questMarkerManager;
    private CitizenProximityManager proximityManager;
    private CitizenScheduleManager scheduleManager;
    private CitizenBeaconManager beaconManager;
    private CitizenEmoteManager emoteManager;
    private CitizenAppearanceOverrideManager appearanceOverrideManager;
    private final Map<String, Integer> messageCounters = new ConcurrentHashMap<String, Integer>();
    private final Map<String, NPCEntity> npcEntities = new ConcurrentHashMap<String, NPCEntity>();
    private final Set<String> citizensCurrentlySpawning = ConcurrentHashMap.newKeySet();
    private final Map<String, PausedInfo> pausedCitizens = new ConcurrentHashMap<String, PausedInfo>();
    private final Map<UUID, String> staleUUIDs = new ConcurrentHashMap<UUID, String>();
    private final Set<UUID> protectedNpcUuids = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> waypointRecordings = new ConcurrentHashMap<UUID, String>();
    private final Map<UUID, WaypointRecorderHud> activeRecorderHuds = new ConcurrentHashMap<UUID, WaypointRecorderHud>();
    private final AtomicBoolean stateDirty = new AtomicBoolean(false);
    private final AtomicBoolean saveScheduled = new AtomicBoolean(false);
    private CitizenChunkListener chunkListener;
    private volatile boolean reloading = false;
    private volatile long citizenGeneration = 0L;
    private static final String DEFAULT_CUSTOM_FILE = "custom_citizens.json";
    private static final int BATCH_SPAWN_SIZE = 5;
    private static final Pattern COLOR_PATTERN;
    private static final Map<String, String> NAMED_COLORS;

    public NPCEntity getNpcEntity(String citizenId) {
        return this.npcEntities.get(citizenId);
    }

    public CitizenService() {
        instance = this;
        this.skinManager = new CitizenSkinManager();
        this.animationManager = new CitizenAnimationManager();
        this.rotationManager = new CitizenRotationManager();
        this.hologramManager = new CitizenHologramManager();
        this.questMarkerManager = new QuestMarkerManager();
        this.proximityManager = new CitizenProximityManager();
        this.scheduleManager = new CitizenScheduleManager();
        this.beaconManager = new CitizenBeaconManager();
        this.emoteManager = new CitizenEmoteManager();
        this.appearanceOverrideManager = new CitizenAppearanceOverrideManager();
    }

    public static CitizenService getInstance() {
        return instance;
    }

    public void registerProtectedNpcUuid(UUID uuid) {
        if (uuid != null) {
            this.protectedNpcUuids.add(uuid);
        }
    }

    public void unregisterProtectedNpcUuid(UUID uuid) {
        if (uuid != null) {
            this.protectedNpcUuids.remove(uuid);
        }
    }

    public Set<UUID> getProtectedNpcUuids() {
        return Set.copyOf(this.protectedNpcUuids);
    }

    public void load(Path dataFolder) {
        this.load(dataFolder, false);
    }

    public void load(Path dataFolder, boolean skipSkinPrefetch) {
        this.dataFolder = dataFolder;
        Path citizensFile = dataFolder.resolve("citizens.json");
        if (!Files.exists(citizensFile, new LinkOption[0])) {
            this.extractDefault(citizensFile);
        }
        try {
            String content = new String(Files.readAllBytes(citizensFile), StandardCharsets.UTF_8);
            CitizensConfig config = GSON.fromJson(content, CitizensConfig.class);
            if (config != null && config.citizens != null) {
                this.citizens.clear();
                for (CitizenData citizen : config.citizens) {
                    if (citizen.id == null || citizen.id.isEmpty()) {
                        LOGGER.warning("Citizen ohne ID uebersprungen");
                        continue;
                    }
                    if (citizen.scale <= 0.0f) {
                        citizen.scale = 1.0f;
                    }
                    if (citizen.playerMarkers == null) {
                        citizen.playerMarkers = new ConcurrentHashMap<UUID, String>();
                    }
                    citizen.migrateCommands();
                    this.citizens.put(citizen.id, citizen);
                }
                LOGGER.info("Loaded " + this.citizens.size() + " citizens from citizens.json");
            } else {
                LOGGER.warning("citizens.json is empty or invalid");
            }
        }
        catch (Exception e) {
            LOGGER.warning("Failed to load citizens.json: " + e.getMessage());
        }
        HashSet<String> citizensJsonIds = new HashSet<String>(this.citizens.keySet());
        this.createCustomDirectory();
        this.loadCustomCitizens();
        this.loadState(dataFolder);
        this.cleanupCitizensJson(citizensJsonIds);
        ++this.citizenGeneration;
        if (this.skinManager != null) {
            this.skinManager.loadDiskCache(dataFolder);
            if (!skipSkinPrefetch) {
                this.prefetchMissingSkins();
            }
        }
    }

    private void prefetchMissingSkins() {
        if (this.skinManager == null) {
            return;
        }
        ArrayList<CompletableFuture<PlayerSkin>> futures = new ArrayList<CompletableFuture<PlayerSkin>>();
        for (CitizenData citizen : this.citizens.values()) {
            if (!citizen.isPlayerModel || citizen.skinUsername == null || citizen.skinUsername.isEmpty() || this.skinManager.getCachedSkin(citizen.skinUsername) != null) continue;
            if (CoreAPI.isDebug()) {
                LOGGER.info("Prefetching skin for " + citizen.id + " (" + citizen.skinUsername + ")...");
            }
            futures.add(this.skinManager.fetchSkin(citizen.skinUsername));
        }
        if (!futures.isEmpty()) {
            LOGGER.info("Prefetching " + futures.size() + " missing NPC skins...");
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30L, TimeUnit.SECONDS);
                LOGGER.info("Skin prefetch complete \u2014 all skins cached");
            }
            catch (TimeoutException e) {
                LOGGER.warning("Skin prefetch timed out after 30s \u2014 some NPCs may appear without skin initially");
            }
            catch (Exception e) {
                LOGGER.warning("Skin prefetch error: " + e.getMessage());
            }
        }
    }

    public void save() {
        if (this.dataFolder == null) {
            return;
        }
        Path citizensFile = this.dataFolder.resolve("citizens.json");
        try {
            if (Files.exists(citizensFile, new LinkOption[0])) {
                Path backupFile = this.dataFolder.resolve("citizens.json.backup");
                Files.copy(citizensFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
            }
            ArrayList<CitizenData> baseCitizens = new ArrayList<CitizenData>();
            for (CitizenData c : this.citizens.values()) {
                if (c.customSourceFile != null) continue;
                baseCitizens.add(c);
            }
            CitizensConfig config = new CitizensConfig();
            config.citizens = baseCitizens;
            String json = GSON.toJson(config);
            Files.write(citizensFile, json.getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
            int customCount = this.citizens.size() - baseCitizens.size();
            LOGGER.fine("Saved " + baseCitizens.size() + " base citizens to citizens.json" + (String)(customCount > 0 ? " (skipped " + customCount + " custom)" : ""));
        }
        catch (Exception e) {
            LOGGER.warning("Failed to save citizens.json: " + e.getMessage());
        }
        this.saveState();
    }

    public void loadState(Path dataFolder) {
        Path stateFile = dataFolder.resolve("citizen_state.json");
        if (!Files.exists(stateFile, new LinkOption[0])) {
            int migrated = 0;
            for (CitizenData citizen : this.citizens.values()) {
                if (citizen.spawnedEntityUUID == null) continue;
                ++migrated;
            }
            if (migrated > 0) {
                LOGGER.info("No citizen_state.json found \u2014 migrated " + migrated + " UUIDs from citizens.json");
                this.saveState();
            }
            return;
        }
        try {
            UUID uuid;
            Map<Object, Object> staleMap;
            Map citizenMap;
            String content = new String(Files.readAllBytes(stateFile), StandardCharsets.UTF_8);
            CitizenStateFile stateObj = GSON.fromJson(content, CitizenStateFile.class);
            if (stateObj != null && stateObj.citizens != null) {
                citizenMap = stateObj.citizens;
                staleMap = stateObj.stale != null ? stateObj.stale : Map.of();
            } else {
                Type mapType = new TypeToken<Map<String, String>>(this){
                    {
                        Objects.requireNonNull(this$0);
                    }
                }.getType();
                citizenMap = (Map)GSON.fromJson(content, mapType);
                staleMap = Map.of();
                if (citizenMap != null) {
                    LOGGER.info("Migrating citizen_state.json from flat format to structured format");
                }
            }
            if (citizenMap == null) {
                return;
            }
            int restored = 0;
            int stale = 0;
            for (Map.Entry entry : citizenMap.entrySet()) {
                String citizenId = (String)entry.getKey();
                try {
                    uuid = UUID.fromString((String)entry.getValue());
                }
                catch (IllegalArgumentException e) {
                    continue;
                }
                CitizenData citizen = this.citizens.get(citizenId);
                if (citizen != null) {
                    citizen.spawnedEntityUUID = uuid;
                    ++restored;
                    continue;
                }
                this.staleUUIDs.put(uuid, citizenId);
                ++stale;
            }
            int restoredStale = 0;
            for (Map.Entry<Object, Object> entry : staleMap.entrySet()) {
                try {
                    uuid = UUID.fromString((String)entry.getKey());
                    this.staleUUIDs.put(uuid, (String)entry.getValue());
                    ++restoredStale;
                }
                catch (IllegalArgumentException illegalArgumentException) {}
            }
            LOGGER.info("Loaded citizen_state.json: " + restored + " UUIDs restored" + (String)(stale > 0 ? ", " + stale + " stale (deleted citizens)" : "") + (String)(restoredStale > 0 ? ", " + restoredStale + " stale UUIDs restored from disk" : ""));
        }
        catch (Exception e) {
            LOGGER.warning("Failed to load citizen_state.json: " + e.getMessage());
        }
    }

    public void saveState() {
        if (this.dataFolder == null) {
            return;
        }
        Path stateFile = this.dataFolder.resolve("citizen_state.json");
        try {
            CitizenStateFile stateObj = new CitizenStateFile();
            for (CitizenData citizenData : this.citizens.values()) {
                if (citizenData.spawnedEntityUUID == null) continue;
                stateObj.citizens.put(citizenData.id, citizenData.spawnedEntityUUID.toString());
            }
            for (Map.Entry entry : this.staleUUIDs.entrySet()) {
                stateObj.stale.put(((UUID)entry.getKey()).toString(), (String)entry.getValue());
            }
            String json = GSON.toJson(stateObj);
            Files.write(stateFile, json.getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
            LOGGER.fine("Saved citizen_state.json: " + stateObj.citizens.size() + " UUIDs" + (String)(stateObj.stale.isEmpty() ? "" : ", " + stateObj.stale.size() + " stale"));
        }
        catch (Exception e) {
            LOGGER.warning("Failed to save citizen_state.json: " + e.getMessage());
        }
    }

    public void markStateDirty() {
        this.stateDirty.set(true);
        if (this.saveScheduled.compareAndSet(false, true)) {
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                this.saveScheduled.set(false);
                if (this.stateDirty.compareAndSet(true, false)) {
                    this.saveState();
                }
            }, 500L, TimeUnit.MILLISECONDS);
        }
    }

    private void saveCustomFile(String fileName) {
        if (this.dataFolder == null) {
            return;
        }
        Path customDir = this.dataFolder.resolve("custom");
        Path file = customDir.resolve(fileName);
        try {
            ArrayList<CitizenData> fileCitizens = new ArrayList<CitizenData>();
            for (CitizenData c : this.citizens.values()) {
                if (!fileName.equals(c.customSourceFile)) continue;
                fileCitizens.add(c);
            }
            List<Object> disabledIds = List.of();
            int configVersion = 1;
            if (Files.exists(file, new LinkOption[0])) {
                try {
                    String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                    CustomCitizensConfig existing = GSON.fromJson(content, CustomCitizensConfig.class);
                    if (existing != null) {
                        if (existing.disabled_base_ids != null) {
                            disabledIds = existing.disabled_base_ids;
                        }
                        configVersion = existing.configVersion;
                    }
                }
                catch (Exception e) {
                    LOGGER.fine("Could not parse existing custom file for metadata: " + fileName);
                }
            } else if (fileCitizens.isEmpty()) {
                return;
            }
            CustomCitizensConfig config = new CustomCitizensConfig();
            config.configVersion = configVersion;
            config.disabled_base_ids = disabledIds;
            config.citizens = fileCitizens;
            String json = GSON.toJson(config);
            Files.write(file, json.getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
            if (CoreAPI.isDebug()) {
                LOGGER.info("Updated custom file " + fileName + " with " + fileCitizens.size() + " citizens");
            }
        }
        catch (Exception e) {
            LOGGER.warning("Failed to save custom file " + fileName + ": " + e.getMessage());
        }
    }

    private void saveAllCustomFiles() {
        HashSet<String> customFiles = new HashSet<String>();
        for (CitizenData c : this.citizens.values()) {
            if (c.customSourceFile == null) continue;
            customFiles.add(c.customSourceFile);
        }
        for (String fileName : customFiles) {
            this.saveCustomFile(fileName);
        }
    }

    public void reload() {
        this.despawnAll();
        this.load(this.dataFolder);
    }

    public void reload(World world) {
        this.despawnAllInWorld(world);
        this.load(this.dataFolder, true);
        for (CitizenData citizen : this.citizens.values()) {
            citizen.spawnedEntityUUID = null;
            citizen.entityRef = null;
            citizen.cachedNetworkId = 0;
        }
        this.npcEntities.clear();
        this.spawnAllInWorld(world);
        this.save();
        this.saveAllCustomFiles();
    }

    public void reloadAllWorlds() {
        Universe universe = Universe.get();
        if (universe == null) {
            this.reload();
            return;
        }
        this.reloading = true;
        ArrayList allWorlds = new ArrayList(universe.getWorlds().values());
        HashMap<String, UUID> uuidBackup = new HashMap<String, UUID>();
        for (CitizenData citizenData : this.citizens.values()) {
            if (citizenData.spawnedEntityUUID == null) continue;
            uuidBackup.put(citizenData.id, citizenData.spawnedEntityUUID);
        }
        this.load(this.dataFolder, true);
        for (CitizenData citizenData : this.citizens.values()) {
            citizenData.entityRef = null;
            citizenData.cachedNetworkId = 0;
            citizenData.spawnedEntityUUID = null;
        }
        this.npcEntities.clear();
        for (Map.Entry entry : uuidBackup.entrySet()) {
            if (this.citizens.containsKey(entry.getKey())) continue;
            this.staleUUIDs.put((UUID)entry.getValue(), (String)entry.getKey());
        }
        for (World world : allWorlds) {
            boolean hasPlayers = world.getPlayerCount() > 0;
            world.execute(() -> {
                String worldName = world.getName();
                Store store = world.getEntityStore().getStore();
                for (Map.Entry entry : uuidBackup.entrySet()) {
                    CitizenData citizen = this.citizens.get(entry.getKey());
                    if (citizen != null && !CitizenService.matchesWorld(citizen.worldName, worldName)) continue;
                    try {
                        Ref ref = world.getEntityRef((UUID)entry.getValue());
                        if (ref != null && ref.isValid()) {
                            store.removeEntity(ref, RemoveReason.REMOVE);
                            LOGGER.fine("Removed old entity for " + (String)entry.getKey() + " in " + worldName);
                            continue;
                        }
                        if (citizen == null || !CitizenService.matchesWorld(citizen.worldName, worldName)) continue;
                        citizen.spawnedEntityUUID = (UUID)entry.getValue();
                    }
                    catch (Exception e) {
                        if (citizen == null || !CitizenService.matchesWorld(citizen.worldName, worldName)) continue;
                        citizen.spawnedEntityUUID = (UUID)entry.getValue();
                    }
                }
                if (hasPlayers) {
                    this.spawnAllInWorld(world);
                    if (CoreAPI.isDebug()) {
                        LOGGER.info("Reloaded citizens in " + worldName + " (active world)");
                    }
                } else if (CoreAPI.isDebug()) {
                    LOGGER.info("Skipped spawn in " + worldName + " (no players \u2014 ChunkListener handles on entry)");
                }
            });
        }
        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            this.reloading = false;
            ++this.citizenGeneration;
            this.save();
            this.saveAllCustomFiles();
            LOGGER.info("Citizens reloaded across " + allWorlds.size() + " worlds (" + this.citizens.size() + " total)");
        }, 1000L, TimeUnit.MILLISECONDS);
    }

    private void cleanupCitizensJson(Set<String> citizensJsonIds) {
        HashSet<String> baseIds = new HashSet<String>();
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("defaults/citizens.json");){
            if (is == null) {
                return;
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            CitizensConfig defaultConfig = GSON.fromJson(content, CitizensConfig.class);
            if (defaultConfig != null && defaultConfig.citizens != null) {
                for (CitizenData c : defaultConfig.citizens) {
                    if (c.id == null) continue;
                    baseIds.add(c.id);
                }
            }
        }
        catch (Exception e) {
            LOGGER.fine("Could not load JAR defaults for cleanup check: " + e.getMessage());
            return;
        }
        int migrated = 0;
        for (CitizenData citizen : this.citizens.values()) {
            if (citizen.customSourceFile != null || baseIds.contains(citizen.id)) continue;
            citizen.customSourceFile = DEFAULT_CUSTOM_FILE;
            ++migrated;
        }
        int dualFile = 0;
        for (CitizenData citizen : this.citizens.values()) {
            if (citizen.customSourceFile == null || !citizensJsonIds.contains(citizen.id) || baseIds.contains(citizen.id)) continue;
            ++dualFile;
        }
        if (migrated > 0 || dualFile > 0) {
            if (migrated > 0) {
                LOGGER.info("Citizens cleanup: moved " + migrated + " leaked custom citizen(s) to custom_citizens.json");
            }
            if (dualFile > 0) {
                LOGGER.info("Citizens cleanup: removing " + dualFile + " custom citizen(s) from citizens.json (already in custom file)");
            }
            this.save();
            this.saveAllCustomFiles();
        }
    }

    private void extractDefault(Path targetPath) {
        try {
            if (!Files.exists(targetPath.getParent(), new LinkOption[0])) {
                Files.createDirectories(targetPath.getParent(), new FileAttribute[0]);
            }
            try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("defaults/citizens.json");){
                if (is != null) {
                    Files.copy(is, targetPath, new CopyOption[0]);
                    if (CoreAPI.isDebug()) {
                        LOGGER.info("Extracted default citizens.json");
                    }
                }
            }
        }
        catch (IOException e) {
            LOGGER.warning("Failed to extract default citizens.json: " + e.getMessage());
        }
    }

    private void createCustomDirectory() {
        block8: {
            try {
                Path customDir = this.dataFolder.resolve("custom");
                Files.createDirectories(customDir, new FileAttribute[0]);
                Path exampleFile = customDir.resolve("custom_citizens.json.example");
                if (Files.exists(exampleFile, new LinkOption[0])) break block8;
                LinkedHashMap<String, Object> example = new LinkedHashMap<String, Object>();
                example.put("configVersion", 1);
                example.put("_comment", "Custom citizens override base ones by ID. Add disabled_base_ids to remove base citizens. This file is NEVER overwritten on updates.");
                example.put("disabled_base_ids", List.of());
                example.put("citizens", List.of());
                try (BufferedWriter w = Files.newBufferedWriter(exampleFile, StandardCharsets.UTF_8, new OpenOption[0]);){
                    GSON.toJson(example, (Appendable)w);
                }
            }
            catch (IOException e) {
                LOGGER.warning("Failed to create custom citizens directory: " + e.getMessage());
            }
        }
    }

    private void loadCustomCitizens() {
        Path customDir = this.dataFolder.resolve("custom");
        if (!Files.isDirectory(customDir, new LinkOption[0])) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(customDir, "custom_citizens*.json");){
            ArrayList<Path> files = new ArrayList<Path>();
            for (Path file : stream) {
                files.add(file);
            }
            files.sort(Comparator.comparing(p -> p.getFileName().toString()));
            for (Path file : files) {
                this.loadSingleCustomCitizensFile(file);
            }
        }
        catch (IOException e) {
            LOGGER.warning("Failed to scan custom citizens directory: " + e.getMessage());
        }
    }

    private void loadSingleCustomCitizensFile(Path file) {
        try {
            String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            CustomCitizensConfig customConfig = GSON.fromJson(content, CustomCitizensConfig.class);
            if (customConfig == null) {
                return;
            }
            String fileName = file.getFileName().toString();
            int disabled = 0;
            if (customConfig.disabled_base_ids != null) {
                for (String disabledId : customConfig.disabled_base_ids) {
                    if (this.citizens.remove(disabledId) == null) continue;
                    ++disabled;
                    if (!CoreAPI.isDebug()) continue;
                    LOGGER.info("Disabled base citizen: " + disabledId);
                }
            }
            int added = 0;
            int overridden = 0;
            if (customConfig.citizens != null) {
                for (CitizenData citizen : customConfig.citizens) {
                    if (citizen.id == null || citizen.id.isEmpty()) {
                        LOGGER.warning("Custom citizen ohne ID uebersprungen in " + fileName);
                        continue;
                    }
                    if (citizen.scale <= 0.0f) {
                        citizen.scale = 1.0f;
                    }
                    if (citizen.playerMarkers == null) {
                        citizen.playerMarkers = new ConcurrentHashMap<UUID, String>();
                    }
                    citizen.migrateCommands();
                    citizen.customSourceFile = fileName;
                    if (this.citizens.containsKey(citizen.id)) {
                        ++overridden;
                    } else {
                        ++added;
                    }
                    this.citizens.put(citizen.id, citizen);
                }
            }
            LOGGER.info("Loaded " + fileName + ": " + added + " added, " + overridden + " overridden, " + disabled + " disabled");
        }
        catch (Exception e) {
            LOGGER.warning("Failed to load " + String.valueOf(file.getFileName()) + ": " + e.getMessage());
        }
    }

    public CitizenData getCitizen(String id) {
        return this.citizens.get(id);
    }

    public Collection<CitizenData> getAllCitizens() {
        return Collections.unmodifiableCollection(this.citizens.values());
    }

    public List<CitizenData> getCitizensByGroup(String group) {
        ArrayList<CitizenData> result = new ArrayList<CitizenData>();
        for (CitizenData c : this.citizens.values()) {
            if (!group.equals(c.group)) continue;
            result.add(c);
        }
        return result;
    }

    public void addCitizen(CitizenData citizen) {
        if (citizen.id == null || citizen.id.isEmpty()) {
            LOGGER.warning("Cannot add citizen without ID");
            return;
        }
        if (citizen.playerMarkers == null) {
            citizen.playerMarkers = new ConcurrentHashMap<UUID, String>();
        }
        if (citizen.customSourceFile == null) {
            citizen.customSourceFile = DEFAULT_CUSTOM_FILE;
        }
        this.citizens.put(citizen.id, citizen);
        ++this.citizenGeneration;
        this.save();
        this.saveCustomFile(citizen.customSourceFile);
    }

    public boolean renameCitizen(String oldId, String newId) {
        if (oldId == null || newId == null || newId.isEmpty()) {
            return false;
        }
        if (oldId.equals(newId)) {
            return true;
        }
        if (this.citizens.containsKey(newId)) {
            return false;
        }
        CitizenData citizen = this.citizens.remove(oldId);
        if (citizen == null) {
            return false;
        }
        citizen.id = newId;
        this.citizens.put(newId, citizen);
        ++this.citizenGeneration;
        this.save();
        if (citizen.customSourceFile != null) {
            this.saveCustomFile(citizen.customSourceFile);
        }
        return true;
    }

    public void updateCitizen(CitizenData citizen) {
        if (citizen.id == null || !this.citizens.containsKey(citizen.id)) {
            return;
        }
        this.citizens.put(citizen.id, citizen);
        ++this.citizenGeneration;
        this.save();
        if (citizen.customSourceFile != null) {
            this.saveCustomFile(citizen.customSourceFile);
        }
    }

    public void removeCitizen(String id) {
        this.removeCitizen(id, null);
    }

    public void removeCitizen(String id, World world) {
        CitizenData citizen = this.citizens.remove(id);
        if (citizen == null) {
            return;
        }
        ++this.citizenGeneration;
        World resolvedWorld = world;
        if (resolvedWorld == null) {
            resolvedWorld = this.resolveWorldForCitizen(citizen);
        }
        if (resolvedWorld != null) {
            World w = resolvedWorld;
            w.execute(() -> this.despawnCitizen(citizen, w));
        } else {
            this.despawnCitizen(citizen);
        }
        this.save();
        if (citizen.customSourceFile != null) {
            this.saveCustomFile(citizen.customSourceFile);
        }
    }

    private World resolveWorldForCitizen(CitizenData citizen) {
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                return null;
            }
            World defaultWorld = universe.getDefaultWorld();
            if (defaultWorld != null && CitizenService.matchesWorld(citizen.worldName, defaultWorld.getName())) {
                return defaultWorld;
            }
            return defaultWorld;
        }
        catch (Exception e) {
            LOGGER.fine("resolveWorldForCitizen error: " + e.getMessage());
            return null;
        }
    }

    public void setChunkListener(CitizenChunkListener listener) {
        this.chunkListener = listener;
    }

    public int getCitizenCount() {
        return this.citizens.size();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void spawnCitizen(CitizenData citizen, World world) {
        block56: {
            Vector3d position;
            Store store;
            int roleIndex;
            String roleName;
            NPCPlugin npcPlugin;
            block55: {
                block54: {
                    block53: {
                        block52: {
                            if (!this.citizensCurrentlySpawning.add(citizen.id)) {
                                LOGGER.fine("Spawn skipped (already spawning): " + citizen.id);
                                return;
                            }
                            if (citizen.spawnedEntityUUID != null || citizen.entityRef != null) {
                                UUID oldUUID = citizen.spawnedEntityUUID;
                                this.despawnCitizen(citizen, world);
                                if (oldUUID != null) {
                                    this.staleUUIDs.put(oldUUID, citizen.id);
                                }
                            }
                            if (!citizen.hideNpc) break block52;
                            LOGGER.fine("Citizen " + citizen.id + " is hidden (hideNpc=true), skipping spawn");
                            this.citizensCurrentlySpawning.remove(citizen.id);
                            return;
                        }
                        npcPlugin = NPCPlugin.get();
                        if (npcPlugin != null) break block53;
                        LOGGER.warning("NPCPlugin not available \u2014 cannot spawn citizens");
                        this.citizensCurrentlySpawning.remove(citizen.id);
                        return;
                    }
                    roleName = citizen.resolveRoleName();
                    LOGGER.fine("[Citizen-Spawn] " + citizen.id + " \u2192 role: " + roleName + " (fKey=" + citizen.fKeyInteractionEnabled + ", npcRoleId=" + citizen.npcRoleId + ")");
                    if (citizen.getMovementType() == CitizenData.MovementType.PATH) {
                        this.registerCitizenPathSync(citizen, world);
                    }
                    if (citizen.getMovementType() == CitizenData.MovementType.PATH && (citizen.waypoints == null || citizen.waypoints.size() < 2)) {
                        LOGGER.warning("[PATH] Citizen " + citizen.id + " has < 2 waypoints, falling back to IDLE role");
                        String string = roleName = citizen.fKeyInteractionEnabled ? "KS_NPC_Interactable_Role" : "KS_NPC_Idle_Role";
                    }
                    if (citizen.getMovementType() == CitizenData.MovementType.PATH && npcPlugin.getIndex(roleName) < 0) {
                        this.ensurePathRole(roleName, citizen, npcPlugin);
                    }
                    if ((roleIndex = npcPlugin.getIndex(roleName)) >= 0) break block54;
                    LOGGER.warning("NPC Role not found: " + roleName + " for citizen " + citizen.id + " \u2014 available roles: " + npcPlugin.getRoleTemplateNames(false).size());
                    this.citizensCurrentlySpawning.remove(citizen.id);
                    return;
                }
                LOGGER.fine("[Citizen-Spawn] " + citizen.id + " \u2192 roleIndex: " + roleIndex);
                store = world.getEntityStore().getStore();
                position = this.resolvePosition(citizen, world);
                citizen.resolvedPosX = position.x;
                citizen.resolvedPosY = position.y;
                citizen.resolvedPosZ = position.z;
                long spawnChunkIdx = ChunkUtil.indexChunkFromBlock((int)((int)position.x), (int)((int)position.z));
                if (world.getChunkIfLoaded(spawnChunkIdx) != null) break block55;
                LOGGER.fine("Cannot spawn citizen " + citizen.id + ": chunk not loaded at " + (int)position.x + "/" + (int)position.z + ", deferring to ChunkListener");
                this.citizensCurrentlySpawning.remove(citizen.id);
                return;
            }
            try {
                try {
                    PlayerSkin cachedSkin;
                    Vector3f rotation = new Vector3f(citizen.rotX, citizen.rotY, citizen.rotZ);
                    Model spawnModel = null;
                    if (!citizen.isPlayerModel && citizen.entityTypeId != null && !citizen.entityTypeId.isEmpty()) {
                        HashMap attachIds;
                        float modelScale = Math.max(0.01f, citizen.scale);
                        spawnModel = new Model.ModelReference(citizen.entityTypeId, modelScale, attachIds = citizen.attachmentIds != null ? citizen.attachmentIds : new HashMap()).toModel();
                        if (spawnModel == null) {
                            LOGGER.warning("Invalid entityTypeId for citizen " + citizen.id + ": " + citizen.entityTypeId);
                        } else {
                            LOGGER.fine("[Citizen-Spawn] " + citizen.id + " model created: type=" + citizen.entityTypeId + " modelScale=" + modelScale);
                        }
                    }
                    if (citizen.isPlayerModel && citizen.customSkin != null) {
                        if (this.skinManager != null) {
                            citizen.customSkin = this.skinManager.ensureNoNullFields(citizen.customSkin);
                        }
                        try {
                            spawnModel = CosmeticsModule.get().createModel(citizen.customSkin, Math.max(0.01f, citizen.scale));
                            LOGGER.fine("[Citizen-Spawn] " + citizen.id + " using customSkin model");
                        }
                        catch (Exception e) {
                            LOGGER.warning("[Citizen-Spawn] " + citizen.id + " Failed to create customSkin model: " + e.getMessage() + " \u2014 resetting customSkin");
                            citizen.customSkin = null;
                        }
                    } else if (citizen.isPlayerModel && citizen.skinUsername != null && !citizen.skinUsername.isEmpty() && this.skinManager != null && (cachedSkin = this.skinManager.getCachedSkin(citizen.skinUsername)) != null && (spawnModel = this.skinManager.createModelFromSkin(cachedSkin, citizen.scale)) != null) {
                        LOGGER.fine("[Citizen-Spawn] " + citizen.id + " using cached skin model for " + citizen.skinUsername);
                    }
                    LOGGER.fine("[Citizen-Spawn] " + citizen.id + " spawning: isPlayerModel=" + citizen.isPlayerModel + " configScale=" + citizen.scale + " hasCustomModel=" + (spawnModel != null));
                    Pair result = npcPlugin.spawnEntity(store, roleIndex, position, rotation, spawnModel, (TriConsumer)(citizen.fKeyInteractionEnabled ? (npcEntity2, holder, st) -> holder.addComponent(Interactable.getComponentType(), (Component)Interactable.INSTANCE) : null), null);
                    if (result != null && result.first() != null) {
                        citizen.entityRef = (Ref)result.first();
                        citizen.createdAt = System.currentTimeMillis();
                        try {
                            NetworkId nid = (NetworkId)store.getComponent(citizen.entityRef, NetworkId.getComponentType());
                            citizen.cachedNetworkId = nid != null ? nid.getId() : 0;
                        }
                        catch (Exception e) {
                            citizen.cachedNetworkId = 0;
                        }
                        NPCEntity npcEntity = (NPCEntity)result.second();
                        if (npcEntity != null) {
                            this.npcEntities.put(citizen.id, npcEntity);
                        }
                        if (npcEntity != null) {
                            citizen.spawnedEntityUUID = npcEntity.getUuid();
                        } else {
                            citizen.spawnedEntityUUID = null;
                            LOGGER.warning("[Citizen-Spawn] " + citizen.id + " NPCEntity is null after spawn \u2014 UUID not set, will respawn on next chunk load");
                        }
                        this.markStateDirty();
                        float safeScale = Math.max(0.01f, citizen.scale);
                        try {
                            PersistentModel persistentModel = (PersistentModel)store.getComponent(citizen.entityRef, PersistentModel.getComponentType());
                            if (persistentModel != null) {
                                Model.ModelReference currentRef = persistentModel.getModelReference();
                                if (currentRef != null) {
                                    persistentModel.setModelReference(new Model.ModelReference(currentRef.getModelAssetId(), safeScale, currentRef.getRandomAttachmentIds(), currentRef.isStaticModel()));
                                    LOGGER.fine("[Citizen-Spawn] " + citizen.id + " PersistentModel scale corrected to " + safeScale + " (modelAssetId=" + currentRef.getModelAssetId() + ")");
                                } else if (spawnModel != null) {
                                    persistentModel.setModelReference(new Model.ModelReference(spawnModel.getModelAssetId(), safeScale, spawnModel.getRandomAttachmentIds(), spawnModel.getAnimationSetMap() == null));
                                    LOGGER.fine("[Citizen-Spawn] " + citizen.id + " PersistentModel set from spawnModel, scale=" + safeScale);
                                }
                            }
                        }
                        catch (Exception e) {
                            LOGGER.warning("[Citizen-Spawn] " + citizen.id + " Failed to set PersistentModel: " + e.getMessage());
                        }
                        if (npcEntity != null) {
                            npcEntity.setInitialModelScale(safeScale);
                        }
                        if (npcEntity != null) {
                            npcEntity.setLeashPoint(position);
                            npcEntity.setLeashHeading(citizen.rotY);
                        }
                        this.applyEquipment(npcEntity, citizen);
                        try {
                            EntityStatMap statMap = (EntityStatMap)store.getComponent(citizen.entityRef, EntityStatMap.getComponentType());
                            if (statMap != null) {
                                statMap.maximizeStatValue(DefaultEntityStatTypes.getHealth());
                            }
                        }
                        catch (Exception e) {
                            LOGGER.fine("Failed to maximize health: " + e.getMessage());
                        }
                        if (citizen.isPlayerModel && citizen.customSkin != null) {
                            if (this.skinManager != null) {
                                this.skinManager.applySkin(citizen.entityRef, citizen.customSkin, citizen.scale);
                                HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                                    if (citizen.entityRef == null || !citizen.entityRef.isValid()) {
                                        return;
                                    }
                                    world.execute(() -> this.skinManager.applySkin(citizen.entityRef, citizen.customSkin, citizen.scale));
                                }, 2L, TimeUnit.SECONDS);
                            }
                        } else if (citizen.isPlayerModel && citizen.skinUsername != null && !citizen.skinUsername.isEmpty() && this.skinManager != null) {
                            PlayerSkin cachedSkin2 = this.skinManager.getCachedSkin(citizen.skinUsername);
                            if (cachedSkin2 != null) {
                                this.skinManager.applySkin(citizen.entityRef, cachedSkin2, citizen.scale);
                            }
                            this.skinManager.fetchSkin(citizen.skinUsername).thenAccept(skin -> {
                                if (skin != null && citizen.entityRef != null) {
                                    try {
                                        world.execute(() -> this.skinManager.applySkin(citizen.entityRef, (PlayerSkin)skin, citizen.scale));
                                    }
                                    catch (Exception e) {
                                        LOGGER.fine("Failed to apply skin async: " + e.getMessage());
                                    }
                                }
                            });
                            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                                if (citizen.entityRef == null || !citizen.entityRef.isValid()) {
                                    return;
                                }
                                PlayerSkin retrySkin = this.skinManager.getCachedSkin(citizen.skinUsername);
                                if (retrySkin != null) {
                                    world.execute(() -> this.skinManager.applySkin(citizen.entityRef, retrySkin, citizen.scale));
                                }
                            }, 2L, TimeUnit.SECONDS);
                        }
                        if (this.hologramManager != null) {
                            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> world.execute(() -> this.hologramManager.setNametag(citizen, world)), 50L, TimeUnit.MILLISECONDS);
                        }
                        if (this.animationManager != null) {
                            this.animationManager.register(citizen);
                        }
                        if (citizen.spawnParticles != null && !citizen.spawnParticles.isEmpty()) {
                            this.sendParticleEffect(citizen.spawnParticles, citizen.spawnFxDuration, citizen.resolvedPosX, citizen.resolvedPosY, citizen.resolvedPosZ, world);
                        }
                        for (CitizenListener l : this.listeners) {
                            try {
                                l.onCitizenSpawn(citizen.id);
                            }
                            catch (Exception e) {
                                LOGGER.fine("CitizenListener error in onCitizenSpawn: " + e.getMessage());
                            }
                        }
                        LOGGER.fine("Spawned citizen: " + citizen.id + " at " + citizen.posX + ", " + citizen.posY + ", " + citizen.posZ + " (role: " + roleName + ")");
                        break block56;
                    }
                    LOGGER.warning("Failed to spawn citizen: " + citizen.id + " (NPCPlugin returned null)");
                }
                catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error spawning citizen " + citizen.id, e);
                }
            }
            catch (Throwable throwable) {
                throw throwable;
            }
            finally {
                this.citizensCurrentlySpawning.remove(citizen.id);
            }
        }
    }

    public boolean isCitizenSpawning(String citizenId) {
        return this.citizensCurrentlySpawning.contains(citizenId);
    }

    public void reattachCitizen(CitizenData citizen, World world, Ref<EntityStore> existingRef) {
        try {
            Store store;
            citizen.entityRef = existingRef;
            try {
                store = existingRef.getStore();
                if (store != null) {
                    NetworkId nid = (NetworkId)store.getComponent(existingRef, NetworkId.getComponentType());
                    citizen.cachedNetworkId = nid != null ? nid.getId() : 0;
                }
            }
            catch (Exception e) {
                citizen.cachedNetworkId = 0;
            }
            try {
                NPCEntity npcEntity;
                store = existingRef.getStore();
                if (store != null && (npcEntity = (NPCEntity)store.getComponent(existingRef, NPCEntity.getComponentType())) != null) {
                    this.npcEntities.put(citizen.id, npcEntity);
                }
            }
            catch (Exception e) {
                LOGGER.fine("Could not get NPCEntity for reattach: " + e.getMessage());
            }
            if (this.animationManager != null) {
                this.animationManager.register(citizen);
            }
            LOGGER.info("Reattached citizen: " + citizen.id + " (UUID=" + String.valueOf(citizen.spawnedEntityUUID) + ", npcEntity=" + (this.npcEntities.containsKey(citizen.id) ? "cached" : "null") + ", skin=" + (citizen.customSkin != null ? "customSkin" : (citizen.skinUsername != null ? citizen.skinUsername : "none")) + ")");
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                if (citizen.entityRef == null || !citizen.entityRef.isValid()) {
                    return;
                }
                world.execute(() -> this.reattachApplyComponents(citizen, world));
            }, 500L, TimeUnit.MILLISECONDS);
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reattaching citizen " + citizen.id, e);
        }
    }

    private void reattachApplyComponents(CitizenData citizen, World world) {
        try {
            if (citizen.entityRef == null || !citizen.entityRef.isValid()) {
                LOGGER.fine("Reattach phase 2 skipped: entityRef invalid for " + citizen.id);
                return;
            }
            Store store = citizen.entityRef.getStore();
            NPCEntity npcEntity = this.npcEntities.get(citizen.id);
            try {
                Model.ModelReference currentRef;
                PersistentModel persistentModel = (PersistentModel)store.getComponent(citizen.entityRef, PersistentModel.getComponentType());
                if (persistentModel != null && (currentRef = persistentModel.getModelReference()) != null && currentRef.getScale() < 0.01f) {
                    float safeScale = Math.max(0.01f, citizen.scale);
                    persistentModel.setModelReference(new Model.ModelReference(currentRef.getModelAssetId(), safeScale, currentRef.getRandomAttachmentIds(), currentRef.isStaticModel()));
                    LOGGER.info("Reattach: Fixed PersistentModel scale for " + citizen.id + " (was " + currentRef.getScale() + ", now " + safeScale + ")");
                }
            }
            catch (Exception e) {
                LOGGER.fine("Reattach: Could not check/fix PersistentModel scale for " + citizen.id);
            }
            if (npcEntity != null) {
                this.applyEquipment(npcEntity, citizen);
            }
            if (citizen.isPlayerModel && this.skinManager != null) {
                if (citizen.customSkin != null) {
                    this.skinManager.applySkin(citizen.entityRef, citizen.customSkin, citizen.scale);
                    HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                        if (citizen.entityRef == null || !citizen.entityRef.isValid()) {
                            return;
                        }
                        world.execute(() -> this.skinManager.applySkin(citizen.entityRef, citizen.customSkin, citizen.scale));
                    }, 2L, TimeUnit.SECONDS);
                } else if (citizen.skinUsername != null && !citizen.skinUsername.isEmpty()) {
                    PlayerSkin cachedSkin = this.skinManager.getCachedSkin(citizen.skinUsername);
                    if (cachedSkin != null) {
                        this.skinManager.applySkin(citizen.entityRef, cachedSkin, citizen.scale);
                    }
                    HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                        if (citizen.entityRef == null || !citizen.entityRef.isValid()) {
                            return;
                        }
                        this.skinManager.fetchSkin(citizen.skinUsername).thenAccept(retrySkin -> {
                            if (retrySkin != null && citizen.entityRef != null && citizen.entityRef.isValid()) {
                                try {
                                    world.execute(() -> this.skinManager.applySkin(citizen.entityRef, (PlayerSkin)retrySkin, citizen.scale));
                                }
                                catch (Exception ex) {
                                    LOGGER.fine("Reattach delayed skin retry failed: " + ex.getMessage());
                                }
                            }
                        });
                    }, 2L, TimeUnit.SECONDS);
                    this.skinManager.fetchSkin(citizen.skinUsername).thenAccept(skin -> {
                        if (skin != null && citizen.entityRef != null && citizen.entityRef.isValid()) {
                            try {
                                world.execute(() -> this.skinManager.applySkin(citizen.entityRef, (PlayerSkin)skin, citizen.scale));
                                this.save();
                            }
                            catch (Exception e) {
                                LOGGER.fine("Failed to re-apply skin async: " + e.getMessage());
                            }
                        }
                    });
                }
            }
            if (this.hologramManager != null) {
                this.hologramManager.setNametag(citizen, world);
            }
            LOGGER.fine("Reattach phase 2 complete: " + citizen.id);
        }
        catch (IndexOutOfBoundsException archEx) {
            LOGGER.warning("Corrupted archetype for " + citizen.id + " \u2014 respawning fresh");
            citizen.entityRef = null;
            citizen.cachedNetworkId = 0;
            citizen.spawnedEntityUUID = null;
            this.spawnCitizen(citizen, world);
        }
        catch (Exception e) {
            LOGGER.warning("Error in reattach phase 2 for " + citizen.id + ": " + e.getMessage());
        }
    }

    private void applyEquipment(NPCEntity npcEntity, CitizenData citizen) {
        boolean hasEquipment;
        if (npcEntity == null) {
            return;
        }
        boolean bl = hasEquipment = citizen.helmet != null || citizen.chest != null || citizen.leggings != null || citizen.gloves != null || citizen.mainHand != null || citizen.offHand != null;
        if (!citizen.isPlayerModel) {
            try {
                Inventory inventory = npcEntity.getInventory();
                if (inventory != null) {
                    if (citizen.mainHand == null) {
                        inventory.getHotbar().setItemStackForSlot((short)0, ItemStack.EMPTY);
                    }
                    if (hasEquipment) {
                        this.applyConfiguredEquipment(inventory, citizen);
                    }
                }
            }
            catch (Exception e) {
                LOGGER.warning("Failed to clear default equipment for " + citizen.id + ": " + e.getMessage());
            }
            return;
        }
        if (!hasEquipment) {
            if (citizen.getMovementType() == CitizenData.MovementType.WANDER) {
                try {
                    Inventory inventory = npcEntity.getInventory();
                    if (inventory != null) {
                        inventory.getHotbar().setItemStackForSlot((short)0, ItemStack.EMPTY);
                    }
                }
                catch (Exception e) {
                    LOGGER.fine("Failed to clear sword for WANDER NPC " + citizen.id + ": " + e.getMessage());
                }
            }
            return;
        }
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return;
            }
            if (citizen.mainHand == null) {
                inventory.getHotbar().setItemStackForSlot((short)0, ItemStack.EMPTY);
            }
            this.applyConfiguredEquipment(inventory, citizen);
            LOGGER.fine("Applied equipment to citizen: " + citizen.id);
        }
        catch (Exception e) {
            LOGGER.warning("Failed to apply equipment to " + citizen.id + ": " + e.getMessage());
        }
    }

    private void applyConfiguredEquipment(Inventory inventory, CitizenData citizen) {
        if (citizen.helmet != null) {
            inventory.getArmor().setItemStackForSlot((short)0, new ItemStack(citizen.helmet));
        }
        if (citizen.chest != null) {
            inventory.getArmor().setItemStackForSlot((short)1, new ItemStack(citizen.chest));
        }
        if (citizen.gloves != null) {
            inventory.getArmor().setItemStackForSlot((short)2, new ItemStack(citizen.gloves));
        }
        if (citizen.leggings != null) {
            inventory.getArmor().setItemStackForSlot((short)3, new ItemStack(citizen.leggings));
        }
        if (citizen.mainHand != null) {
            inventory.getHotbar().setItemStackForSlot((short)0, new ItemStack(citizen.mainHand));
        }
        if (citizen.offHand != null) {
            inventory.getUtility().setItemStackForSlot((short)0, new ItemStack(citizen.offHand));
        }
    }

    private void registerCitizenPathSync(CitizenData citizen, World world) {
        if (citizen.waypoints == null || citizen.waypoints.size() < 2) {
            LOGGER.fine("Citizen " + citizen.id + " has < 2 waypoints, skipping path registration");
            return;
        }
        try {
            ArrayList<Transform> transforms = new ArrayList<Transform>();
            for (CitizenData.WaypointData wp : citizen.waypoints) {
                transforms.add(new Transform(wp.x, wp.y, wp.z, 0.0f, wp.rotY, 0.0f));
            }
            String pathName = "citizen_" + citizen.id;
            WorldPath path = new WorldPath(pathName, transforms);
            WorldPathConfig config = world.getWorldPathConfig();
            config.putPath(path);
            config.save(world);
            LOGGER.info("Registered WorldPath '" + pathName + "' with " + transforms.size() + " waypoints (live config, before spawn)");
        }
        catch (Exception e) {
            LOGGER.warning("Failed to register WorldPath sync for citizen " + citizen.id + ": " + e.getMessage());
        }
    }

    private void unregisterCitizenPath(CitizenData citizen, World world) {
        String pathName = "citizen_" + citizen.id;
        try {
            WorldPathConfig config = world.getWorldPathConfig();
            config.removePath(pathName);
            config.save(world);
            LOGGER.fine("Unregistered WorldPath '" + pathName + "' (live config)");
        }
        catch (Exception e) {
            LOGGER.fine("Error unregistering WorldPath for citizen " + citizen.id + ": " + e.getMessage());
        }
    }

    private void ensurePathRole(String roleName, CitizenData citizen, NPCPlugin npcPlugin) {
        try {
            boolean interactable;
            String pathName = "citizen_" + citizen.id;
            String shape = "LOOP".equalsIgnoreCase(citizen.pathShape) ? "Loop" : "Line";
            String nodeDelayBlock = "";
            if (citizen.waypointPause > 0.0f) {
                nodeDelayBlock = "\"MinNodeDelay\": %s,\n\"MaxNodeDelay\": %s,\n\"UseNodeViewDirection\": true,".formatted(String.valueOf((double)citizen.waypointPause), String.valueOf((double)citizen.waypointPause));
            }
            String interactionBlock = (interactable = roleName.contains("_Interactable_")) ? ",\n\"InteractionInstruction\": {\n  \"Instructions\": [\n    { \"Continue\": true, \"Sensor\": { \"Type\": \"Any\" }, \"Actions\": [{ \"Type\": \"SetInteractable\", \"Interactable\": true }] },\n    { \"Sensor\": { \"Type\": \"HasInteracted\" }, \"Actions\": [{ \"Type\": \"KSCitizenInteract\" }] }\n  ]\n}" : "";
            String json = "{\n  \"Type\": \"Generic\",\n  \"DefaultPlayerAttitude\": \"Ignore\",\n  \"DefaultNPCAttitude\": \"Ignore\",\n  \"ApplySeparation\": false,\n  \"KnockbackScale\": 0,\n  \"Appearance\": \"Player\",\n  \"MaxHealth\": 100,\n  \"MotionControllerList\": [\n    { \"Type\": \"Walk\", \"MaxWalkSpeed\": %.1f, \"Gravity\": 10, \"RunThreshold\": 0.3,\n      \"MaxFallSpeed\": 15, \"MaxRotationSpeed\": 360, \"Acceleration\": 10 }\n  ],\n  \"Instructions\": [\n    { \"Instructions\": [\n      {\n        \"Sensor\": {\n          \"Type\": \"Path\",\n          \"PathType\": \"WorldPath\",\n          \"Path\": \"%s\",\n          \"Range\": 100\n        },\n        \"BodyMotion\": {\n          \"Type\": \"Path\",\n          \"Shape\": \"%s\",\n          \"Direction\": \"Forward\",\n          %s\n          \"StartAtNearestNode\": true,\n          \"MinRelSpeed\": 0.18,\n          \"MaxRelSpeed\": 0.25\n        }\n      }\n    ] }\n  ]%s,\n  \"NameTranslationKey\": \"Citizen\"\n}".formatted((double)citizen.walkSpeed * 3.0, pathName, shape, nodeDelayBlock, interactionBlock);
            Path rolesDir = this.dataFolder.resolve("npc-roles");
            Files.createDirectories(rolesDir, new FileAttribute[0]);
            Path targetFile = rolesDir.resolve(roleName + ".json");
            Files.writeString(targetFile, (CharSequence)json, new OpenOption[0]);
            ArrayList errors = new ArrayList();
            int roleIndex = npcPlugin.getBuilderManager().loadFile(targetFile, false, errors);
            if (roleIndex >= 0 && errors.isEmpty()) {
                LOGGER.info("Generated Generic path role: " + roleName + " (path=" + pathName + ", shape=" + shape + ", pause=" + citizen.waypointPause + "s, speed=" + citizen.walkSpeed + ", index=" + roleIndex + ")");
            } else {
                LOGGER.warning("Failed to generate path role " + roleName + ": " + String.valueOf(errors));
            }
        }
        catch (Exception e) {
            LOGGER.warning("Error generating path role " + roleName + ": " + e.getMessage());
        }
    }

    public void despawnCitizen(CitizenData citizen, World world) {
        if (citizen.despawnParticles != null && !citizen.despawnParticles.isEmpty() && world != null) {
            double px = citizen.resolvedPosX != 0.0 ? citizen.resolvedPosX : citizen.posX;
            double py = citizen.resolvedPosY != 0.0 ? citizen.resolvedPosY : citizen.posY;
            double pz = citizen.resolvedPosZ != 0.0 ? citizen.resolvedPosZ : citizen.posZ;
            this.sendParticleEffect(citizen.despawnParticles, citizen.despawnFxDuration, px, py, pz, world);
        }
        boolean entityRemoved = false;
        if (citizen.entityRef != null && world != null) {
            try {
                if (citizen.entityRef.isValid()) {
                    Store store = world.getEntityStore().getStore();
                    store.removeEntity(citizen.entityRef, RemoveReason.REMOVE);
                    entityRemoved = true;
                    if (CoreAPI.isDebug()) {
                        LOGGER.info("Removed entity for citizen: " + citizen.id + " (via Ref)");
                    }
                }
            }
            catch (Exception e) {
                LOGGER.fine("Failed to remove entity via Ref for " + citizen.id + ": " + e.getMessage());
            }
        }
        if (!entityRemoved && citizen.spawnedEntityUUID != null && world != null) {
            try {
                Ref ref = world.getEntityRef(citizen.spawnedEntityUUID);
                if (ref != null && ref.isValid()) {
                    Store store = world.getEntityStore().getStore();
                    store.removeEntity(ref, RemoveReason.REMOVE);
                    entityRemoved = true;
                    if (CoreAPI.isDebug()) {
                        LOGGER.info("Removed entity for citizen: " + citizen.id + " (via UUID fallback)");
                    }
                }
            }
            catch (Exception e) {
                LOGGER.fine("Failed to remove entity via UUID for " + citizen.id + ": " + e.getMessage());
            }
        }
        if (this.animationManager != null) {
            this.animationManager.unregister(citizen.id);
        }
        if (citizen.getMovementType() == CitizenData.MovementType.PATH && world != null) {
            this.unregisterCitizenPath(citizen, world);
        }
        citizen.spawnedEntityUUID = null;
        citizen.entityRef = null;
        citizen.cachedNetworkId = 0;
        this.npcEntities.remove(citizen.id);
        this.pausedCitizens.remove(citizen.id);
    }

    public void despawnCitizen(CitizenData citizen) {
        this.despawnCitizen(citizen, null);
    }

    public void resetAllSpawnStates(World world) {
        String worldName = world != null ? world.getName() : null;
        int removed = 0;
        int reset = 0;
        for (CitizenData citizen : this.citizens.values()) {
            if (citizen.spawnedEntityUUID == null || worldName != null && !CitizenService.matchesWorld(citizen.worldName, worldName)) continue;
            boolean entityRemoved = false;
            if (citizen.entityRef != null && world != null) {
                try {
                    if (citizen.entityRef.isValid()) {
                        Store store = world.getEntityStore().getStore();
                        store.removeEntity(citizen.entityRef, RemoveReason.REMOVE);
                        entityRemoved = true;
                    }
                }
                catch (Exception e) {
                    LOGGER.fine("Failed to remove entity via Ref for " + citizen.id + ": " + e.getMessage());
                }
            }
            if (!entityRemoved && world != null) {
                try {
                    Ref ref = world.getEntityRef(citizen.spawnedEntityUUID);
                    if (ref != null && ref.isValid()) {
                        Store store = world.getEntityStore().getStore();
                        store.removeEntity(ref, RemoveReason.REMOVE);
                        entityRemoved = true;
                    }
                }
                catch (Exception e) {
                    LOGGER.fine("Failed to remove entity via UUID for " + citizen.id + ": " + e.getMessage());
                }
            }
            if (entityRemoved) {
                citizen.spawnedEntityUUID = null;
                ++removed;
            } else {
                LOGGER.fine("Entity not removed for " + citizen.id + " \u2014 UUID preserved for chunk cleanup");
            }
            citizen.entityRef = null;
            citizen.cachedNetworkId = 0;
            if (this.animationManager != null) {
                this.animationManager.unregister(citizen.id);
            }
            ++reset;
        }
        if (reset > 0) {
            LOGGER.info("Reset spawn state for " + reset + " citizens in " + (worldName != null ? worldName : "all worlds") + " (" + removed + " entities removed)");
        }
    }

    public void resetAllSpawnStates() {
        int count = 0;
        for (CitizenData citizen : this.citizens.values()) {
            if (citizen.spawnedEntityUUID == null) continue;
            citizen.entityRef = null;
            citizen.cachedNetworkId = 0;
            if (this.animationManager != null) {
                this.animationManager.unregister(citizen.id);
            }
            ++count;
        }
        if (count > 0) {
            LOGGER.info("Reset spawn state for " + count + " citizens (tracking only, no world)");
        }
    }

    public static boolean matchesWorld(String citizenWorldName, String actualWorldName) {
        if (citizenWorldName == null || actualWorldName == null) {
            return false;
        }
        if (citizenWorldName.equals(actualWorldName)) {
            return true;
        }
        return actualWorldName.toLowerCase().contains(citizenWorldName.toLowerCase());
    }

    public Vector3d resolvePosition(CitizenData citizen, World world) {
        if (citizen.spawnRelative) {
            try {
                Transform spawn;
                ISpawnProvider spawnProvider = world.getWorldConfig().getSpawnProvider();
                if (spawnProvider != null && (spawn = spawnProvider.getSpawnPoint(world, new UUID(0L, 0L))) != null) {
                    Vector3d spawnPos = spawn.getPosition();
                    return new Vector3d(spawnPos.x + citizen.posX, spawnPos.y + citizen.posY, spawnPos.z + citizen.posZ);
                }
            }
            catch (Exception e) {
                LOGGER.warning("Failed to resolve spawn-relative position for " + citizen.id + ", falling back to absolute: " + e.getMessage());
            }
        }
        return new Vector3d(citizen.posX, citizen.posY, citizen.posZ);
    }

    public void spawnAllInWorld(World world) {
        int n;
        String worldName = world.getName();
        ArrayList<CitizenData> toSpawn = new ArrayList<CitizenData>();
        ArrayList<CitizenData> toReattach = new ArrayList<CitizenData>();
        int skippedActive = 0;
        int skippedPending = 0;
        int skippedUnloaded = 0;
        for (CitizenData citizenData : this.citizens.values()) {
            if (!CitizenService.matchesWorld(citizenData.worldName, worldName) || citizenData.hideNpc) continue;
            if (citizenData.entityRef != null && citizenData.entityRef.isValid()) {
                ++skippedActive;
                continue;
            }
            if (this.chunkListener != null && this.chunkListener.isPendingSpawn(citizenData.id)) {
                ++skippedPending;
                continue;
            }
            if (this.citizensCurrentlySpawning.contains(citizenData.id)) {
                ++skippedPending;
                continue;
            }
            Vector3d vector3d = this.resolvePosition(citizenData, world);
            long chunkIdx = ChunkUtil.indexChunkFromBlock((int)((int)vector3d.x), (int)((int)vector3d.z));
            if (world.getChunkIfLoaded(chunkIdx) == null) {
                if (this.chunkListener != null && !this.chunkListener.isPendingSpawn(citizenData.id)) {
                    this.chunkListener.triggerSpawnForChunk(citizenData, world, chunkIdx);
                }
                ++skippedUnloaded;
                continue;
            }
            if (citizenData.spawnedEntityUUID != null) {
                Ref existingRef = world.getEntityRef(citizenData.spawnedEntityUUID);
                if (existingRef != null && existingRef.isValid()) {
                    boolean refUsable = false;
                    try {
                        Store testStore = existingRef.getStore();
                        if (testStore != null) {
                            testStore.getComponent(existingRef, NetworkId.getComponentType());
                            refUsable = true;
                        }
                    }
                    catch (Exception e) {
                        LOGGER.warning("Entity ref for " + citizenData.id + " is stale (IndexOutOfBounds), will respawn fresh");
                    }
                    if (refUsable) {
                        toReattach.add(citizenData);
                        continue;
                    }
                }
                this.staleUUIDs.put(citizenData.spawnedEntityUUID, citizenData.id);
            }
            toSpawn.add(citizenData);
        }
        int reattached = 0;
        for (CitizenData citizenData : toReattach) {
            Ref existingRef = world.getEntityRef(citizenData.spawnedEntityUUID);
            if (existingRef == null || !existingRef.isValid()) continue;
            this.reattachCitizen(citizenData, world, (Ref<EntityStore>)existingRef);
            ++reattached;
        }
        if (!toSpawn.isEmpty()) {
            this.spawnBatch(world, toSpawn, 0, worldName);
        }
        if ((n = toSpawn.size()) > 0 || reattached > 0 || skippedPending > 0 || skippedUnloaded > 0) {
            LOGGER.info("spawnAllInWorld " + worldName + ": " + n + " queued for batch spawn, " + reattached + " reattached" + (String)(skippedActive > 0 ? ", " + skippedActive + " already active" : "") + (String)(skippedPending > 0 ? ", " + skippedPending + " pending in chunk listener" : "") + (String)(skippedUnloaded > 0 ? ", " + skippedUnloaded + " chunk not loaded (deferred to ChunkListener)" : ""));
        }
    }

    private void spawnBatch(World world, List<CitizenData> toSpawn, int startIndex, String worldName) {
        int end = Math.min(startIndex + 5, toSpawn.size());
        int spawned = 0;
        for (int i = startIndex; i < end; ++i) {
            CitizenData citizen = toSpawn.get(i);
            if (citizen.entityRef != null && citizen.entityRef.isValid() || this.citizensCurrentlySpawning.contains(citizen.id)) continue;
            this.spawnCitizen(citizen, world);
            ++spawned;
        }
        if (spawned > 0) {
            LOGGER.fine("Batch spawn " + worldName + ": " + spawned + " spawned (batch " + (startIndex / 5 + 1) + ", index " + startIndex + "-" + (end - 1) + ")");
        }
        if (end < toSpawn.size()) {
            world.execute(() -> this.spawnBatch(world, toSpawn, end, worldName));
        }
    }

    public void despawnAll() {
        if (this.questMarkerManager != null) {
            this.questMarkerManager.removeAll();
        }
        for (CitizenData citizen : this.citizens.values()) {
            this.despawnCitizen(citizen);
        }
        this.npcEntities.clear();
        LOGGER.info("Despawned all citizens");
    }

    public void despawnAllInWorld(World world) {
        String worldName = world.getName();
        int count = 0;
        for (CitizenData citizen : this.citizens.values()) {
            if (!CitizenService.matchesWorld(citizen.worldName, worldName) || citizen.entityRef == null && citizen.spawnedEntityUUID == null) continue;
            this.despawnCitizen(citizen, world);
            ++count;
        }
        if (count > 0) {
            LOGGER.info("Despawned " + count + " citizens from world " + worldName + " (entities removed from store)");
        }
    }

    public void cleanupStaleEntities(World world) {
        if (this.staleUUIDs.isEmpty()) {
            return;
        }
        Store store = world.getEntityStore().getStore();
        int removed = 0;
        ArrayList<UUID> cleaned = new ArrayList<UUID>();
        for (Map.Entry<UUID, String> entry : this.staleUUIDs.entrySet()) {
            UUID uuid = entry.getKey();
            try {
                Ref ref = world.getEntityRef(uuid);
                if (ref != null && ref.isValid()) {
                    store.removeEntity(ref, RemoveReason.REMOVE);
                    ++removed;
                    cleaned.add(uuid);
                    if (!CoreAPI.isDebug()) continue;
                    LOGGER.info("Cleaned up orphaned entity: UUID=" + String.valueOf(uuid) + " (was citizen: " + entry.getValue() + ")");
                    continue;
                }
                if (ref == null) continue;
                cleaned.add(uuid);
            }
            catch (Exception e) {
                LOGGER.fine("Stale entity cleanup error for UUID " + String.valueOf(uuid) + ": " + e.getMessage());
            }
        }
        for (UUID uuid : cleaned) {
            this.staleUUIDs.remove(uuid);
        }
        if (removed > 0) {
            LOGGER.info("Orphan cleanup: removed " + removed + " stale entities in " + world.getName() + " (" + this.staleUUIDs.size() + " remaining)");
        }
    }

    public int getStaleUUIDCount() {
        return this.staleUUIDs.size();
    }

    public void cleanupOrphanEntities(World world) {
        try {
            Store store = world.getEntityStore().getStore();
            Archetype query = Archetype.of((ComponentType)NPCEntity.getComponentType());
            ArrayList orphanUUIDs = new ArrayList();
            store.forEachChunk((Query)query, (chunk, commandBuffer) -> {
                for (int i = 0; i < chunk.size(); ++i) {
                    try {
                        CitizenData owner;
                        UUID uuid;
                        UUIDComponent uuidComp;
                        String roleName;
                        NPCEntity npc = (NPCEntity)chunk.getComponent(i, NPCEntity.getComponentType());
                        if (npc == null || !NpcViewerScanner.isCitizenRole(roleName = npc.getRoleName()) || (uuidComp = (UUIDComponent)chunk.getComponent(i, UUIDComponent.getComponentType())) == null || uuidComp.getUuid() == null || this.protectedNpcUuids.contains(uuid = uuidComp.getUuid()) || (owner = this.getCitizenByUUID(uuid)) != null) continue;
                        orphanUUIDs.add(uuid);
                        continue;
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
            });
            int removed = 0;
            for (UUID uuid : orphanUUIDs) {
                try {
                    Ref ref = world.getEntityRef(uuid);
                    if (ref != null && ref.isValid()) {
                        store.removeEntity(ref, RemoveReason.REMOVE);
                        ++removed;
                    }
                    this.staleUUIDs.remove(uuid);
                }
                catch (Exception e) {
                    LOGGER.fine("Orphan entity removal error for UUID " + String.valueOf(uuid) + ": " + e.getMessage());
                }
            }
            if (removed > 0) {
                LOGGER.info("Orphan scan: removed " + removed + " orphan citizen entities in " + world.getName());
                this.markStateDirty();
            }
        }
        catch (Exception e) {
            LOGGER.warning("Orphan entity scan failed in " + world.getName() + ": " + e.getMessage());
        }
    }

    public void setMarker(String citizenId, UUID playerId, String markerType) {
        CitizenData citizen = this.citizens.get(citizenId);
        if (citizen != null) {
            citizen.playerMarkers.put(playerId, markerType);
        }
    }

    public void clearMarker(String citizenId, UUID playerId) {
        CitizenData citizen = this.citizens.get(citizenId);
        if (citizen != null) {
            citizen.playerMarkers.remove(playerId);
        }
    }

    public String getMarker(String citizenId, UUID playerId) {
        CitizenData citizen = this.citizens.get(citizenId);
        if (citizen != null) {
            return citizen.playerMarkers.get(playerId);
        }
        return null;
    }

    public Map<String, CitizenData> getCitizensWithMarkerForPlayer(UUID playerId) {
        HashMap<String, CitizenData> result = new HashMap<String, CitizenData>();
        for (CitizenData citizen : this.citizens.values()) {
            String marker = citizen.playerMarkers.get(playerId);
            if (marker == null) continue;
            result.put(citizen.id, citizen);
        }
        return result;
    }

    public void addListener(CitizenListener listener) {
        this.listeners.add(listener);
        if (CoreAPI.isDebug()) {
            LOGGER.info("Citizen listener registered: " + listener.getClass().getSimpleName());
        }
    }

    public void removeListener(CitizenListener listener) {
        this.listeners.remove(listener);
    }

    public void addDialogInterceptor(CitizenDialogInterceptor interceptor) {
        this.dialogInterceptors.add(interceptor);
        if (CoreAPI.isDebug()) {
            LOGGER.info("Dialog interceptor registered: " + interceptor.getClass().getSimpleName());
        }
    }

    public void removeDialogInterceptor(CitizenDialogInterceptor interceptor) {
        this.dialogInterceptors.remove(interceptor);
    }

    public void setBankHandler(CitizenBankHandler handler) {
        this.bankHandler = handler;
        if (CoreAPI.isDebug()) {
            LOGGER.info("Bank handler registered: " + (handler != null ? handler.getClass().getSimpleName() : "null"));
        }
    }

    public CitizenBankHandler getBankHandler() {
        return this.bankHandler;
    }

    public void dispatchCitizenDeath(String citizenId) {
        for (CitizenListener l : this.listeners) {
            try {
                l.onCitizenDeath(citizenId);
            }
            catch (Exception e) {
                LOGGER.fine("CitizenListener error in onCitizenDeath: " + e.getMessage());
            }
        }
    }

    public void dispatchProximityEnter(Player player, String citizenId, float distance) {
        for (CitizenListener l : this.listeners) {
            try {
                l.onCitizenProximityEnter(player, citizenId, distance);
            }
            catch (Exception e) {
                LOGGER.fine("CitizenListener error in onCitizenProximityEnter: " + e.getMessage());
            }
        }
    }

    public void dispatchProximityExit(Player player, String citizenId) {
        for (CitizenListener l : this.listeners) {
            try {
                l.onCitizenProximityExit(player, citizenId);
            }
            catch (Exception e) {
                LOGGER.fine("CitizenListener error in onCitizenProximityExit: " + e.getMessage());
            }
        }
    }

    public void dispatchScheduleChange(String citizenId, String fromPeriod, String toPeriod) {
        for (CitizenListener l : this.listeners) {
            try {
                l.onCitizenScheduleChange(citizenId, fromPeriod, toPeriod);
            }
            catch (Exception e) {
                LOGGER.fine("CitizenListener error in onCitizenScheduleChange: " + e.getMessage());
            }
        }
    }

    public void dispatchBeaconReceived(String citizenId, String message) {
        for (CitizenListener l : this.listeners) {
            try {
                l.onCitizenBeaconReceived(citizenId, message);
            }
            catch (Exception e) {
                LOGGER.fine("CitizenListener error in onCitizenBeaconReceived: " + e.getMessage());
            }
        }
    }

    public void pauseMovement(CitizenData citizen, Player player) {
        if (citizen == null || player == null) {
            return;
        }
        if (citizen.getMovementType() == CitizenData.MovementType.IDLE) {
            return;
        }
        if (citizen.entityRef == null || !citizen.entityRef.isValid()) {
            return;
        }
        if (this.pausedCitizens.containsKey(citizen.id)) {
            return;
        }
        try {
            Store store = citizen.entityRef.getStore();
            TransformComponent transform = (TransformComponent)store.getComponent(citizen.entityRef, TransformComponent.getComponentType());
            if (transform == null) {
                return;
            }
            Vector3d pos = transform.getPosition();
            UUID playerUuid = player.getPlayerRef().getUuid();
            store.addComponent(citizen.entityRef, Frozen.getComponentType(), (Component)Frozen.get());
            try {
                AnimationUtils.stopAnimation(citizen.entityRef, (AnimationSlot)AnimationSlot.Movement, (ComponentAccessor)store);
            }
            catch (Exception animEx) {
                LOGGER.fine("Failed to stop walk animation on pause: " + animEx.getMessage());
            }
            this.pausedCitizens.put(citizen.id, new PausedInfo(pos.x, pos.y, pos.z, playerUuid));
            LOGGER.info("[Pause] Paused citizen: " + citizen.id + " at (" + String.format("%.1f, %.1f, %.1f", pos.x, pos.y, pos.z) + ") interacting with " + player.getPlayerRef().getUsername() + " movement=" + String.valueOf((Object)citizen.getMovementType()));
        }
        catch (Exception e) {
            LOGGER.warning("Failed to pause movement for " + citizen.id + ": " + e.getMessage());
        }
    }

    public void resumeMovement(String citizenId) {
        if (citizenId == null) {
            LOGGER.info("[Resume] citizenId is null, skipping");
            return;
        }
        PausedInfo info = this.pausedCitizens.get(citizenId);
        if (info != null) {
            info.resumeRequested = true;
            LOGGER.info("[Resume] resumeRequested=true for citizen: " + citizenId);
        } else {
            LOGGER.info("[Resume] citizen " + citizenId + " NOT in pausedCitizens (size=" + this.pausedCitizens.size() + ", keys=" + String.valueOf(this.pausedCitizens.keySet()) + ")");
        }
    }

    public boolean isPaused(String citizenId) {
        return this.pausedCitizens.containsKey(citizenId);
    }

    private void resetMotionControllerState(String citizenId) {
        try {
            NPCEntity npcEntity = this.getNpcEntity(citizenId);
            if (npcEntity == null) {
                return;
            }
            Role role = npcEntity.getRole();
            if (role == null) {
                return;
            }
            MotionController mc = role.getActiveMotionController();
            if (mc == null) {
                return;
            }
            if (mc instanceof MotionControllerBase) {
                MotionControllerBase mcBase = (MotionControllerBase)mc;
                mcBase.setMotionKind(MotionKind.MOVING);
                try {
                    Field prevSpeedField = MotionControllerBase.class.getDeclaredField("previousSpeed");
                    prevSpeedField.setAccessible(true);
                    Field maxSpeedField = MotionControllerBase.class.getDeclaredField("maxHorizontalSpeed");
                    maxSpeedField.setAccessible(true);
                    double maxSpeed = maxSpeedField.getDouble(mcBase);
                    prevSpeedField.setDouble(mcBase, maxSpeed > 0.0 ? maxSpeed : 0.5);
                    Field lastMKField = MotionControllerBase.class.getDeclaredField("lastMovementStateUpdatedMotionKind");
                    lastMKField.setAccessible(true);
                    lastMKField.set(mcBase, null);
                    Field idleField = MotionControllerBase.class.getDeclaredField("idleMotionKind");
                    idleField.setAccessible(true);
                    idleField.setBoolean(mcBase, false);
                    LOGGER.fine("[Resume] MotionController reset: previousSpeed=" + (maxSpeed > 0.0 ? maxSpeed : 0.5) + ", motionKind=MOVING, cache cleared");
                }
                catch (IllegalAccessException | NoSuchFieldException e) {
                    LOGGER.fine("[Resume] Reflection fallback failed: " + e.getMessage());
                }
            }
        }
        catch (Exception e) {
            LOGGER.fine("[Resume] resetMotionControllerState error: " + e.getMessage());
        }
    }

    private void resetBodyMotionPath(String citizenId) {
        try {
            Method getBodyMotion;
            Object bodyMotion;
            NPCEntity npcEntity = this.getNpcEntity(citizenId);
            if (npcEntity == null) {
                return;
            }
            Role role = npcEntity.getRole();
            if (role == null) {
                return;
            }
            Field lastStepField = Role.class.getDeclaredField("lastBodyMotionStep");
            lastStepField.setAccessible(true);
            Object lastStep = lastStepField.get(role);
            if (lastStep != null && (bodyMotion = (getBodyMotion = lastStep.getClass().getMethod("getBodyMotion", new Class[0])).invoke(lastStep, new Object[0])) instanceof BodyMotionPath) {
                BodyMotionPath pathMotion = (BodyMotionPath)bodyMotion;
                Method invalidate = BodyMotionPath.class.getDeclaredMethod("invalidateWaypoint", new Class[0]);
                invalidate.setAccessible(true);
                invalidate.invoke((Object)pathMotion, new Object[0]);
                Method reset = BodyMotionPath.class.getDeclaredMethod("reset", new Class[0]);
                reset.setAccessible(true);
                reset.invoke((Object)pathMotion, new Object[0]);
                LOGGER.fine("[Resume] BodyMotionPath reset: invalidateWaypoint + reset (will re-acquire nearest node)");
            }
            lastStepField.set(role, null);
        }
        catch (NoSuchFieldException | NoSuchMethodException e) {
            LOGGER.fine("[Resume] BodyMotionPath reset not applicable: " + e.getMessage());
        }
        catch (Exception e) {
            LOGGER.fine("[Resume] resetBodyMotionPath error: " + e.getMessage());
        }
    }

    private void sendParticleEffect(String particleId, float duration, double x, double y, double z, World world) {
        try {
            Object effectId = particleId;
            if (duration > 0.0f) {
                try {
                    DefaultAssetMap assetMap = com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem.getAssetMap();
                    com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem originalAsset = (com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem)assetMap.getAsset((Object)particleId);
                    if (originalAsset != null) {
                        ParticleSystem packetClone = originalAsset.toPacket().clone();
                        String virtualId = particleId + "__cz_" + Math.round(duration * 10.0f);
                        packetClone.id = virtualId;
                        packetClone.lifeSpan = duration;
                        effectId = virtualId;
                        UpdateParticleSystems updatePacket = new UpdateParticleSystems(UpdateType.AddOrUpdate, Map.of(virtualId, packetClone), null);
                        for (PlayerRef pRef : world.getPlayerRefs()) {
                            try {
                                pRef.getPacketHandler().writeNoCache((ToClientPacket)updatePacket);
                            }
                            catch (Exception exception) {}
                        }
                        String cleanupId = virtualId;
                        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                            try {
                                UpdateParticleSystems removePacket = new UpdateParticleSystems(UpdateType.Remove, null, new String[]{cleanupId});
                                for (PlayerRef pRef : world.getPlayerRefs()) {
                                    try {
                                        pRef.getPacketHandler().writeNoCache((ToClientPacket)removePacket);
                                    }
                                    catch (Exception exception) {}
                                }
                            }
                            catch (Exception exception) {
                                // empty catch block
                            }
                        }, (long)(duration * 1000.0f) + 2000L, TimeUnit.MILLISECONDS);
                    }
                }
                catch (Exception e) {
                    LOGGER.fine("Failed to create virtual particle system for " + particleId + ": " + e.getMessage());
                    effectId = particleId;
                }
            }
            SpawnParticleSystem particlePacket = new SpawnParticleSystem((String)effectId, new Position(x, y, z), new Direction(0.0f, 0.0f, 0.0f), 1.0f, null);
            for (PlayerRef pRef : world.getPlayerRefs()) {
                try {
                    pRef.getPacketHandler().writeNoCache((ToClientPacket)particlePacket);
                }
                catch (Exception exception) {}
            }
        }
        catch (Exception e) {
            LOGGER.fine("Failed to spawn particle effect " + particleId + ": " + e.getMessage());
        }
    }

    public void tickPausedCitizens(World world, Collection<Player> onlinePlayers) {
        if (this.pausedCitizens.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, PausedInfo>> iterator = this.pausedCitizens.entrySet().iterator();
        while (iterator.hasNext()) {
            Store store;
            Map.Entry<String, PausedInfo> entry = iterator.next();
            String citizenId = entry.getKey();
            PausedInfo info = entry.getValue();
            CitizenData citizen = this.citizens.get(citizenId);
            if (citizen == null || citizen.entityRef == null || !citizen.entityRef.isValid()) {
                iterator.remove();
                continue;
            }
            if (!info.resumeRequested && info.playerUuid != null && System.currentTimeMillis() - info.pausedAt > 500L) {
                for (Player player : onlinePlayers) {
                    if (!player.getPlayerRef().getUuid().equals(info.playerUuid)) continue;
                    try {
                        CustomUIPage currentPage = player.getPageManager().getCustomPage();
                        if (currentPage != null) break;
                        LOGGER.info("[Tick] Player page closed for citizen " + citizenId + " \u2014 auto-resuming (page-check)");
                        info.resumeRequested = true;
                    }
                    catch (Exception e) {
                        LOGGER.fine("[Tick] Page-check error: " + e.getMessage());
                    }
                    break;
                }
            }
            if (!info.resumeRequested && System.currentTimeMillis() - info.pausedAt > 30000L) {
                LOGGER.warning("[Tick] Citizen " + citizenId + " paused for >30s \u2014 auto-resuming! (resumeMovement was probably never called)");
                info.resumeRequested = true;
            }
            if (info.resumeRequested) {
                iterator.remove();
                try {
                    store = citizen.entityRef.getStore();
                    store.tryRemoveComponent(citizen.entityRef, Frozen.getComponentType());
                    this.resetMotionControllerState(citizen.id);
                    if (citizen.getMovementType() == CitizenData.MovementType.PATH) {
                        this.resetBodyMotionPath(citizen.id);
                    }
                    LOGGER.info("[Tick] Resumed citizen: " + citizenId + " (Frozen removed, motion reset, movement=" + String.valueOf((Object)citizen.getMovementType()) + ")");
                }
                catch (Exception e) {
                    LOGGER.warning("[Tick] Resume error for " + citizenId + ": " + e.getMessage());
                }
                continue;
            }
            try {
                store = citizen.entityRef.getStore();
                TransformComponent transform = (TransformComponent)store.getComponent(citizen.entityRef, TransformComponent.getComponentType());
                if (transform == null) continue;
                Vector3d currentPos = transform.getPosition();
                double dx = currentPos.x - info.x;
                double dz = currentPos.z - info.z;
                if (dx * dx + dz * dz > 0.01) {
                    transform.setPosition(new Vector3d(info.x, info.y, info.z));
                }
                if (info.playerUuid == null || this.rotationManager == null) continue;
                for (Player player : onlinePlayers) {
                    if (!player.getPlayerRef().getUuid().equals(info.playerUuid)) continue;
                    this.rotationManager.rotateToPlayer(citizen, player);
                }
            }
            catch (Exception e) {
                LOGGER.fine("Paused tick error for " + citizenId + ": " + e.getMessage());
            }
        }
    }

    public void dispatchInteract(Player player, String citizenId) {
        boolean hasCommands;
        Store store;
        Ref ref;
        PlayerRef playerRef;
        CoreAPI.NpcPageOpener pageOpener;
        CitizenData citizen = this.citizens.get(citizenId);
        if (citizen == null) {
            return;
        }
        if (citizen.permission != null && !citizen.permission.isEmpty() && !player.hasPermission(citizen.permission)) {
            if (citizen.noPermissionMessage != null && !citizen.noPermissionMessage.isEmpty()) {
                player.sendMessage(Message.raw((String)this.replacePlaceholders(citizen.noPermissionMessage, player, citizen)).color("#ff6666"));
            }
            return;
        }
        this.pauseMovement(citizen, player);
        if (this.rotationManager != null && citizen.getMovementType() != CitizenData.MovementType.IDLE) {
            this.rotationManager.rotateToPlayer(citizen, player);
        }
        if (this.animationManager != null) {
            this.animationManager.onInteract(citizen);
        }
        for (CitizenListener l : this.listeners) {
            try {
                l.onCitizenInteract(player, citizenId);
            }
            catch (Exception e) {
                LOGGER.fine("CitizenListener error in onCitizenInteract: " + e.getMessage());
            }
        }
        LOGGER.info("[Citizens] dispatchInteract for '" + citizenId + "', interceptors: " + this.dialogInterceptors.size());
        if (!this.dialogInterceptors.isEmpty()) {
            PlayerRef playerRef2 = player.getPlayerRef();
            Ref ref2 = player.getReference();
            Store store2 = ref2.getStore();
            for (CitizenDialogInterceptor interceptor : this.dialogInterceptors) {
                try {
                    if (!interceptor.interceptDialog(player, playerRef2, (Ref<EntityStore>)ref2, (Store<EntityStore>)store2, citizenId)) continue;
                    return;
                }
                catch (Exception e) {
                    LOGGER.fine("Dialog interceptor error: " + e.getMessage());
                }
            }
        }
        if ((pageOpener = CoreAPI.getNpcPageOpener(citizenId)) != null) {
            playerRef = player.getPlayerRef();
            ref = player.getReference();
            store = ref.getStore();
            try {
                pageOpener.open(player, playerRef, (Ref<EntityStore>)ref, (Store<EntityStore>)store);
                return;
            }
            catch (Exception e) {
                LOGGER.fine("NpcPageOpener error for '" + citizenId + "': " + e.getMessage());
            }
        }
        if (citizen.bankingEnabled && this.bankHandler != null) {
            playerRef = player.getPlayerRef();
            ref = player.getReference();
            store = ref.getStore();
            try {
                if (this.bankHandler.openBank(player, playerRef, (Ref<EntityStore>)ref, (Store<EntityStore>)store, citizenId)) {
                    return;
                }
            }
            catch (Exception e) {
                LOGGER.fine("Bank handler error: " + e.getMessage());
            }
        }
        if (citizen.dialogs != null && !citizen.dialogs.isEmpty()) {
            this.openCitizenDialog(player, citizen);
            return;
        }
        if (citizen.messages != null && !citizen.messages.isEmpty()) {
            this.sendCitizenMessages(player, citizen);
        }
        if (citizen.shopId != null && !citizen.shopId.isEmpty()) {
            this.openCitizenShop(player, citizen);
            return;
        }
        boolean bl = hasCommands = citizen.commandActions != null && !citizen.commandActions.isEmpty() || citizen.interactCommands != null && !citizen.interactCommands.isEmpty();
        if (hasCommands) {
            this.executeCitizenCommands(player, citizen);
        }
        if (this.pausedCitizens.containsKey(citizenId)) {
            String cid = citizenId;
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> this.resumeMovement(cid), 5L, TimeUnit.SECONDS);
        }
    }

    private void openCitizenDialog(Player player, CitizenData citizen) {
        try {
            DialogService dialogService = DialogService.getInstance();
            if (dialogService == null) {
                LOGGER.warning("DialogService not available");
                return;
            }
            String dialogId = null;
            for (CitizenData.ConditionalDialog cd : citizen.dialogs) {
                if (cd.dialogId == null) continue;
                if (cd.condition == null) {
                    if (CoreAPI.isDebug()) {
                        LOGGER.info("[Dialog] " + cd.dialogId + " has no condition \u2192 selected (fallback)");
                    }
                    dialogId = cd.dialogId;
                    break;
                }
                DialogCondition condition = new DialogCondition();
                condition.type = cd.condition.type;
                condition.value = cd.condition.value;
                condition.negate = cd.condition.negate;
                boolean matched = dialogService.checkCondition(player, condition);
                if (CoreAPI.isDebug()) {
                    LOGGER.info("[Dialog] " + cd.dialogId + " condition " + cd.condition.type + "(" + cd.condition.value + ") = " + matched);
                }
                if (!matched) continue;
                dialogId = cd.dialogId;
                break;
            }
            if (dialogId == null) {
                LOGGER.info("[Dialog] No matching dialog for citizen " + citizen.id + " \u2014 resuming movement (was paused before openCitizenDialog)");
                this.resumeMovement(citizen.id);
                if (citizen.messages != null && !citizen.messages.isEmpty()) {
                    this.sendCitizenMessages(player, citizen);
                }
                return;
            }
            PlayerRef playerRef = player.getPlayerRef();
            Ref ref = player.getReference();
            Store store = ref.getStore();
            dialogService.openDialog(player, playerRef, (Ref<EntityStore>)ref, (Store<EntityStore>)store, dialogId, citizen.id);
        }
        catch (Exception e) {
            LOGGER.warning("Failed to open dialog for citizen " + citizen.id + ": " + e.getMessage());
        }
    }

    private void openCitizenShop(Player player, CitizenData citizen) {
        try {
            ShopService shopService = ShopService.getInstance();
            if (shopService == null) {
                LOGGER.warning("ShopService not available");
                return;
            }
            PlayerRef playerRef = player.getPlayerRef();
            Ref ref = player.getReference();
            Store store = ref.getStore();
            shopService.openShop(player, playerRef, (Ref<EntityStore>)ref, (Store<EntityStore>)store, citizen.shopId, citizen.id);
        }
        catch (Exception e) {
            LOGGER.warning("Failed to open shop for citizen " + citizen.id + ": " + e.getMessage());
        }
    }

    public void startWaypointRecording(UUID playerUuid, String citizenId, Player player, PlayerRef playerRef) {
        this.waypointRecordings.put(playerUuid, citizenId);
        CitizenData citizen = this.getCitizen(citizenId);
        int count = citizen != null && citizen.waypoints != null ? citizen.waypoints.size() : 0;
        WaypointRecorderHud hud = new WaypointRecorderHud(playerRef, citizenId, count);
        this.activeRecorderHuds.put(playerUuid, hud);
        if (CitizenMHUDIntegration.isAvailable()) {
            if (!CitizenMHUDIntegration.setCustomHud(player, playerRef, hud)) {
                player.getHudManager().setCustomHud(playerRef, (CustomUIHud)hud);
                hud.show();
            }
        } else {
            player.getHudManager().setCustomHud(playerRef, (CustomUIHud)hud);
            hud.show();
        }
    }

    public void stopWaypointRecording(UUID playerUuid, Player player, PlayerRef playerRef) {
        this.waypointRecordings.remove(playerUuid);
        WaypointRecorderHud hud = this.activeRecorderHuds.remove(playerUuid);
        if (hud != null) {
            if (CitizenMHUDIntegration.isAvailable()) {
                CitizenMHUDIntegration.hideCustomHud(player, playerRef);
            } else {
                UICommandBuilder ui = new UICommandBuilder();
                ui.set("#WpRecRoot.Visible", false);
                hud.update(false, ui);
            }
        }
    }

    public String getRecordingCitizenId(UUID playerUuid) {
        return this.waypointRecordings.get(playerUuid);
    }

    public boolean isRecordingWaypoints(UUID playerUuid) {
        return this.waypointRecordings.containsKey(playerUuid);
    }

    public void updateRecorderHud(UUID playerUuid, int count) {
        WaypointRecorderHud hud = this.activeRecorderHuds.get(playerUuid);
        if (hud != null) {
            hud.updateCount(count);
        }
    }

    private void sendCitizenMessages(Player player, CitizenData citizen) {
        List<String> msgs = citizen.messages;
        if (msgs == null || msgs.isEmpty()) {
            return;
        }
        String mode = citizen.messageSelectionMode != null ? citizen.messageSelectionMode.toUpperCase() : "RANDOM";
        String citizenName = citizen.getDisplayName();
        switch (mode) {
            case "SEQUENTIAL": {
                String counterKey = String.valueOf(player.getPlayerRef().getUuid()) + ":" + citizen.id;
                int idx = this.messageCounters.getOrDefault(counterKey, 0);
                if (idx >= msgs.size()) {
                    idx = 0;
                }
                String msg = this.replacePlaceholders(msgs.get(idx), player, citizen);
                player.sendMessage(CitizenService.parseColoredMessage("[" + citizenName + "] " + msg, "#aaddff"));
                this.messageCounters.put(counterKey, idx + 1);
                break;
            }
            case "ALL": {
                long delayMs = (long)(citizen.messageDelay * 1000.0f);
                if (delayMs <= 0L) {
                    for (String msg : msgs) {
                        String resolved = this.replacePlaceholders(msg, player, citizen);
                        player.sendMessage(CitizenService.parseColoredMessage("[" + citizenName + "] " + resolved, "#aaddff"));
                    }
                } else {
                    for (int i = 0; i < msgs.size(); ++i) {
                        String resolved = this.replacePlaceholders(msgs.get(i), player, citizen);
                        long totalDelay = delayMs * (long)i;
                        if (totalDelay == 0L) {
                            player.sendMessage(CitizenService.parseColoredMessage("[" + citizenName + "] " + resolved, "#aaddff"));
                            continue;
                        }
                        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                            try {
                                player.sendMessage(CitizenService.parseColoredMessage("[" + citizenName + "] " + resolved, "#aaddff"));
                            }
                            catch (Exception exception) {
                                // empty catch block
                            }
                        }, totalDelay, TimeUnit.MILLISECONDS);
                    }
                }
                break;
            }
            default: {
                int idx = ThreadLocalRandom.current().nextInt(msgs.size());
                String msg = this.replacePlaceholders(msgs.get(idx), player, citizen);
                player.sendMessage(CitizenService.parseColoredMessage("[" + citizenName + "] " + msg, "#aaddff"));
            }
        }
    }

    private void executeCitizenCommands(Player player, CitizenData citizen) {
        if (citizen.commandActions != null && !citizen.commandActions.isEmpty()) {
            for (CitizenData.CommandAction cmdAction : citizen.commandActions) {
                if (cmdAction.command == null || cmdAction.command.trim().isEmpty()) continue;
                try {
                    String resolved = this.replacePlaceholders(cmdAction.command.trim(), player, citizen);
                    if (resolved.startsWith("{ChatMessage}")) {
                        String msgText = resolved.substring("{ChatMessage}".length()).trim();
                        player.sendMessage(CitizenService.parseColoredMessage(msgText, "#aaddff"));
                        continue;
                    }
                    if (resolved.startsWith("/")) {
                        resolved = resolved.substring(1);
                    }
                    if (cmdAction.runAsServer) {
                        CommandUtils.executeAsConsole(player, resolved);
                        continue;
                    }
                    CommandUtils.executeCommand(player, resolved, "player");
                }
                catch (Exception e) {
                    LOGGER.fine("Failed to execute citizen command: " + cmdAction.command + " - " + e.getMessage());
                }
            }
            return;
        }
        if (citizen.interactCommands != null && !citizen.interactCommands.isEmpty()) {
            for (String cmd : citizen.interactCommands) {
                if (cmd == null || cmd.trim().isEmpty()) continue;
                try {
                    String resolved = this.replacePlaceholders(cmd.trim(), player, citizen);
                    if (resolved.startsWith("/")) {
                        resolved = resolved.substring(1);
                    }
                    CommandUtils.executeAsConsole(player, resolved);
                }
                catch (Exception e) {
                    LOGGER.fine("Failed to execute citizen command: " + cmd + " - " + e.getMessage());
                }
            }
        }
    }

    private String replacePlaceholders(String text, Player player, CitizenData citizen) {
        if (text == null) {
            return "";
        }
        return text.replace("{PlayerName}", player.getPlayerRef().getUsername()).replace("{CitizenName}", citizen.getDisplayName()).replace("{CitizenId}", citizen.id != null ? citizen.id : "");
    }

    public CitizenData getCitizenByEntityRef(Ref<?> targetRef) {
        return this.getCitizenByEntityRef(targetRef, null);
    }

    public CitizenData getCitizenByEntityRef(Ref<?> targetRef, Entity targetEntity) {
        UUID targetUUID;
        if (targetRef == null && targetEntity == null) {
            return null;
        }
        if (targetEntity != null && (targetUUID = targetEntity.getUuid()) != null) {
            for (CitizenData citizen : this.citizens.values()) {
                if (citizen.spawnedEntityUUID == null || !citizen.spawnedEntityUUID.equals(targetUUID)) continue;
                return citizen;
            }
        }
        if (targetRef != null) {
            for (CitizenData citizen : this.citizens.values()) {
                if (citizen.entityRef == null || !citizen.entityRef.equals(targetRef)) continue;
                return citizen;
            }
        }
        return null;
    }

    public void dispatchDialogChoice(Player player, String dialogId, int choiceIndex, String choiceText) {
        for (CitizenListener l : this.listeners) {
            try {
                l.onDialogChoice(player, dialogId, choiceIndex, choiceText);
            }
            catch (Exception e) {
                LOGGER.fine("CitizenListener error in onDialogChoice: " + e.getMessage());
            }
        }
    }

    public void dispatchDialogComplete(Player player, String dialogId) {
        for (CitizenListener l : this.listeners) {
            try {
                l.onDialogComplete(player, dialogId);
            }
            catch (Exception e) {
                LOGGER.fine("CitizenListener error in onDialogComplete: " + e.getMessage());
            }
        }
    }

    public void dispatchDialogInput(Player player, String dialogId, String input) {
        for (CitizenListener l : this.listeners) {
            try {
                l.onDialogInput(player, dialogId, input);
            }
            catch (Exception e) {
                LOGGER.fine("CitizenListener error in onDialogInput: " + e.getMessage());
            }
        }
    }

    public CitizenSkinManager getSkinManager() {
        return this.skinManager;
    }

    public CitizenAnimationManager getAnimationManager() {
        return this.animationManager;
    }

    public CitizenRotationManager getRotationManager() {
        return this.rotationManager;
    }

    public CitizenHologramManager getHologramManager() {
        return this.hologramManager;
    }

    public QuestMarkerManager getQuestMarkerManager() {
        return this.questMarkerManager;
    }

    public CitizenProximityManager getProximityManager() {
        return this.proximityManager;
    }

    public CitizenScheduleManager getScheduleManager() {
        return this.scheduleManager;
    }

    public CitizenBeaconManager getBeaconManager() {
        return this.beaconManager;
    }

    public CitizenEmoteManager getEmoteManager() {
        return this.emoteManager;
    }

    public CitizenAppearanceOverrideManager getAppearanceOverrideManager() {
        return this.appearanceOverrideManager;
    }

    public void sendBeacon(String citizenId, String message, Collection<Player> players) {
        CitizenData citizen;
        if (this.beaconManager != null && (citizen = this.citizens.get(citizenId)) != null) {
            this.beaconManager.sendBeacon(citizen, message, this, players);
        }
    }

    public void triggerEmote(String citizenId, String eventName, Collection<Player> players) {
        CitizenData citizen;
        if (this.emoteManager != null && (citizen = this.citizens.get(citizenId)) != null) {
            this.emoteManager.triggerEvent(eventName, citizen, players);
        }
    }

    public boolean isReloading() {
        return this.reloading;
    }

    public long getCitizenGeneration() {
        return this.citizenGeneration;
    }

    public void notifyCitizensChanged() {
        ++this.citizenGeneration;
    }

    public static Message parseColoredMessage(String text, String defaultColor) {
        Message part;
        String segment;
        if (text == null || text.isEmpty()) {
            return Message.raw((String)"");
        }
        Matcher matcher = COLOR_PATTERN.matcher(text);
        if (!matcher.find()) {
            return Message.raw((String)text).color(defaultColor);
        }
        Message result = null;
        int lastEnd = 0;
        String currentColor = defaultColor;
        matcher.reset();
        while (matcher.find()) {
            String colorKey;
            if (matcher.start() > lastEnd) {
                segment = text.substring(lastEnd, matcher.start());
                part = Message.raw((String)segment).color(currentColor);
                Message message = result = result == null ? part : result.insert(part);
            }
            if ((colorKey = matcher.group(1)).startsWith("#")) {
                currentColor = colorKey;
            } else {
                String hex = NAMED_COLORS.get(colorKey.toUpperCase());
                if (hex != null) {
                    currentColor = hex;
                }
            }
            lastEnd = matcher.end();
        }
        if (lastEnd < text.length()) {
            segment = text.substring(lastEnd);
            part = Message.raw((String)segment).color(currentColor);
            result = result == null ? part : result.insert(part);
        }
        return result != null ? result : Message.raw((String)"");
    }

    public CitizenData cloneCitizen(String sourceId) {
        CitizenData source = this.getCitizen(sourceId);
        if (source == null) {
            return null;
        }
        String json = GSON.toJson(source);
        CitizenData clone = GSON.fromJson(json, CitizenData.class);
        if (clone == null) {
            return null;
        }
        if (clone.scale <= 0.0f) {
            clone.scale = 1.0f;
        }
        if (clone.getMovementType() == CitizenData.MovementType.PATH) {
            clone.npcRoleId = null;
        }
        clone.id = sourceId + "_copy";
        while (this.getCitizen(clone.id) != null) {
            clone.id = sourceId + "_" + System.currentTimeMillis() % 10000L;
        }
        clone.spawnedEntityUUID = null;
        clone.entityRef = null;
        clone.playerMarkers = new ConcurrentHashMap<UUID, String>();
        clone.createdAt = 0L;
        this.addCitizen(clone);
        return clone;
    }

    public CitizenData getCitizenByUUID(UUID entityUUID) {
        if (entityUUID == null) {
            return null;
        }
        for (CitizenData citizen : this.citizens.values()) {
            if (!entityUUID.equals(citizen.spawnedEntityUUID)) continue;
            return citizen;
        }
        return null;
    }

    public List<String> getAvailableRoleNames() {
        try {
            NPCPlugin npcPlugin = NPCPlugin.get();
            if (npcPlugin != null) {
                return npcPlugin.getRoleTemplateNames(false);
            }
        }
        catch (Exception e) {
            LOGGER.fine("Failed to get role template names: " + e.getMessage());
        }
        return List.of("Empty_Role");
    }

    static {
        COLOR_PATTERN = Pattern.compile("\\{([A-Za-z]+|#[0-9A-Fa-f]{6})\\}");
        NAMED_COLORS = Map.ofEntries(Map.entry("RED", "#FF0000"), Map.entry("GREEN", "#00FF00"), Map.entry("BLUE", "#0000FF"), Map.entry("YELLOW", "#FFFF00"), Map.entry("ORANGE", "#FFA500"), Map.entry("PINK", "#FFC0CB"), Map.entry("PURPLE", "#800080"), Map.entry("CYAN", "#00FFFF"), Map.entry("WHITE", "#FFFFFF"), Map.entry("BLACK", "#000000"), Map.entry("GOLD", "#FFD700"), Map.entry("SILVER", "#C0C0C0"), Map.entry("GRAY", "#808080"), Map.entry("BRONZE", "#CD7F32"), Map.entry("DARKRED", "#8B0000"), Map.entry("DARKGREEN", "#006400"), Map.entry("DARKBLUE", "#00008B"), Map.entry("MAROON", "#800000"), Map.entry("NAVY", "#000080"), Map.entry("OLIVE", "#808000"), Map.entry("LIGHTBLUE", "#ADD8E6"), Map.entry("LIGHTGREEN", "#90EE90"), Map.entry("LIME", "#32CD32"), Map.entry("AQUA", "#00CED1"), Map.entry("CORAL", "#FF7F50"), Map.entry("CRIMSON", "#DC143C"), Map.entry("SALMON", "#FA8072"), Map.entry("TOMATO", "#FF6347"), Map.entry("MAGENTA", "#FF00FF"), Map.entry("INDIGO", "#4B0082"), Map.entry("TEAL", "#008080"), Map.entry("TURQUOISE", "#40E0D0"), Map.entry("VIOLET", "#EE82EE"), Map.entry("LAVENDER", "#E6E6FA"));
    }

    private static class CitizensConfig {
        public List<CitizenData> citizens;

        private CitizensConfig() {
        }
    }

    static class CitizenStateFile {
        Map<String, String> citizens = new LinkedHashMap<String, String>();
        Map<String, String> stale = new LinkedHashMap<String, String>();

        CitizenStateFile() {
        }
    }

    private static class CustomCitizensConfig {
        public int configVersion;
        public List<String> disabled_base_ids;
        public List<CitizenData> citizens;

        private CustomCitizensConfig() {
        }
    }

    static class PausedInfo {
        final double x;
        final double y;
        final double z;
        final UUID playerUuid;
        final long pausedAt;
        volatile boolean resumeRequested;

        PausedInfo(double x, double y, double z, UUID playerUuid) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.playerUuid = playerUuid;
            this.pausedAt = System.currentTimeMillis();
        }
    }
}

