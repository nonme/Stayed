/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Component
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.protocol.PlayerSkin
 *  com.hypixel.hytale.server.core.asset.type.model.config.Model
 *  com.hypixel.hytale.server.core.asset.type.model.config.Model$ModelReference
 *  com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry
 *  com.hypixel.hytale.server.core.cosmetics.CosmeticsModule
 *  com.hypixel.hytale.server.core.modules.entity.component.ModelComponent
 *  com.hypixel.hytale.server.core.modules.entity.component.PersistentModel
 *  com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.kyuubisoft.core.citizen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.citizen.CitizenData;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

public class CitizenSkinManager {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft CitizenSkins");
    private static final String PLAYERDB_URL = "https://playerdb.co/api/player/hytale/";
    private static final long CACHE_TTL_MS = 1800000L;
    private static final Gson DISK_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type SKIN_MAP_TYPE = new TypeToken<Map<String, PlayerSkin>>(){}.getType();
    private final Map<String, CachedSkin> skinCache = new ConcurrentHashMap<String, CachedSkin>();
    private Path diskCachePath;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();

    public CompletableFuture<PlayerSkin> fetchSkin(String username) {
        if (username == null || username.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        CachedSkin cached = this.skinCache.get(username.toLowerCase());
        if (cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached.skin);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return this.fetchFromPlayerDB(username);
            }
            catch (Exception e) {
                LOGGER.warning("Failed to fetch skin for " + username + ": " + e.getMessage());
                if (cached != null) {
                    return cached.skin;
                }
                return null;
            }
        });
    }

    private PlayerSkin fetchFromPlayerDB(String username) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(PLAYERDB_URL + username)).header("User-Agent", "KyuubiSoft-Core/1.1").timeout(Duration.ofSeconds(10L)).GET().build();
        HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            LOGGER.warning("PlayerDB returned " + response.statusCode() + " for " + username);
            return null;
        }
        PlayerSkin skin = this.parseSkinResponse(response.body());
        if (skin != null) {
            this.skinCache.put(username.toLowerCase(), new CachedSkin(skin));
            LOGGER.info("Cached skin for " + username);
            this.saveDiskCache();
        }
        return skin;
    }

    private PlayerSkin parseSkinResponse(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("data")) {
                return null;
            }
            JsonObject data = root.getAsJsonObject("data");
            if (!data.has("player")) {
                return null;
            }
            JsonObject player = data.getAsJsonObject("player");
            if (!player.has("skin")) {
                return null;
            }
            JsonObject skin = player.getAsJsonObject("skin");
            return new PlayerSkin(this.getStr(skin, "bodyCharacteristic"), this.getStr(skin, "underwear"), this.getStr(skin, "face"), this.getStr(skin, "eyes"), this.getStr(skin, "ears"), this.getStr(skin, "mouth"), this.getStr(skin, "facialHair"), this.getStr(skin, "haircut"), this.getStr(skin, "eyebrows"), this.getStr(skin, "pants"), this.getStr(skin, "overpants"), this.getStr(skin, "undertop"), this.getStr(skin, "overtop"), this.getStr(skin, "shoes"), this.getStr(skin, "headAccessory"), this.getStr(skin, "faceAccessory"), this.getStr(skin, "earAccessory"), this.getStr(skin, "skinFeature"), this.getStr(skin, "gloves"), this.getStr(skin, "cape"));
        }
        catch (Exception e) {
            LOGGER.warning("Failed to parse skin JSON: " + e.getMessage());
            return null;
        }
    }

    private String getStr(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            String val = obj.get(key).getAsString();
            return val.isEmpty() ? null : val;
        }
        return null;
    }

    public void loadDiskCache(Path dataFolder) {
        this.diskCachePath = dataFolder.resolve("skin-cache.json");
        if (!Files.exists(this.diskCachePath, new LinkOption[0])) {
            LOGGER.info("No skin disk cache found, skins will be fetched from API on first spawn");
            return;
        }
        try {
            String content = new String(Files.readAllBytes(this.diskCachePath), StandardCharsets.UTF_8);
            Map diskSkins = (Map)DISK_GSON.fromJson(content, SKIN_MAP_TYPE);
            if (diskSkins != null) {
                for (Map.Entry entry : diskSkins.entrySet()) {
                    if (entry.getValue() == null) continue;
                    this.skinCache.put(((String)entry.getKey()).toLowerCase(), new CachedSkin((PlayerSkin)entry.getValue()));
                }
                LOGGER.info("Loaded " + diskSkins.size() + " skins from disk cache");
            }
        }
        catch (Exception e) {
            LOGGER.warning("Failed to load skin disk cache: " + e.getMessage());
        }
    }

    private synchronized void saveDiskCache() {
        if (this.diskCachePath == null) {
            return;
        }
        try {
            HashMap<String, PlayerSkin> diskSkins = new HashMap<String, PlayerSkin>();
            for (Map.Entry<String, CachedSkin> entry : this.skinCache.entrySet()) {
                diskSkins.put(entry.getKey(), entry.getValue().skin);
            }
            String json = DISK_GSON.toJson(diskSkins);
            Files.write(this.diskCachePath, json.getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
            LOGGER.fine("Saved " + diskSkins.size() + " skins to disk cache");
        }
        catch (Exception e) {
            LOGGER.warning("Failed to save skin disk cache: " + e.getMessage());
        }
    }

    public PlayerSkin getCachedSkin(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        CachedSkin cached = this.skinCache.get(username.toLowerCase());
        return cached != null ? cached.skin : null;
    }

    public Model createModelFromSkin(PlayerSkin skin, float scale) {
        if (skin == null) {
            return null;
        }
        try {
            CosmeticsModule cosmetics = CosmeticsModule.get();
            if (cosmetics != null) {
                float safeScale = Math.max(0.01f, scale);
                return cosmetics.createModel(skin, safeScale);
            }
        }
        catch (Exception e) {
            LOGGER.warning("Failed to create model from skin: " + e.getMessage());
        }
        return null;
    }

    public void applySkin(Ref<EntityStore> entityRef, PlayerSkin skin, float scale) {
        block7: {
            if (entityRef == null || skin == null) {
                return;
            }
            PlayerSkin safeSkin = this.ensureNoNullFields(skin);
            try {
                Store store = entityRef.getStore();
                float safeScale = Math.max(0.01f, scale);
                CosmeticsModule cosmetics = CosmeticsModule.get();
                if (cosmetics == null) break block7;
                Model playerModel = cosmetics.createModel(safeSkin, safeScale);
                if (playerModel != null) {
                    PlayerSkinComponent skinComponent = new PlayerSkinComponent(safeSkin);
                    store.putComponent(entityRef, PlayerSkinComponent.getComponentType(), (Component)skinComponent);
                    ModelComponent modelComponent = new ModelComponent(playerModel);
                    store.putComponent(entityRef, ModelComponent.getComponentType(), (Component)modelComponent);
                    try {
                        PersistentModel pm = (PersistentModel)store.getComponent(entityRef, PersistentModel.getComponentType());
                        if (pm != null) {
                            pm.setModelReference(new Model.ModelReference(playerModel.getModelAssetId(), safeScale, playerModel.getRandomAttachmentIds(), playerModel.getAnimationSetMap() == null));
                        }
                    }
                    catch (Exception e) {
                        LOGGER.fine("Could not update PersistentModel after skin: " + e.getMessage());
                    }
                    LOGGER.fine("Applied skin to NPC entity (scale=" + safeScale + ")");
                    break block7;
                }
                LOGGER.warning("createModel returned null \u2014 skin NOT applied");
            }
            catch (Exception e) {
                LOGGER.warning("Failed to apply skin: " + e.getMessage());
            }
        }
    }

    PlayerSkin ensureNoNullFields(PlayerSkin skin) {
        CosmeticsModule cosmetics = CosmeticsModule.get();
        CosmeticRegistry reg = cosmetics.getRegistry();
        try {
            cosmetics.validateSkin(skin);
            return skin;
        }
        catch (Exception exception) {
            PlayerSkin base = cosmetics.generateRandomSkin(new Random());
            PlayerSkin result = new PlayerSkin(base);
            if (skin.bodyCharacteristic != null && CitizenSkinManager.partIdValid(reg.getBodyCharacteristics(), skin.bodyCharacteristic)) {
                result.bodyCharacteristic = skin.bodyCharacteristic;
            }
            if (skin.underwear != null && CitizenSkinManager.partIdValid(reg.getUnderwear(), skin.underwear)) {
                result.underwear = skin.underwear;
            }
            if (skin.face != null && reg.getFaces().containsKey(skin.face)) {
                result.face = skin.face;
            }
            if (skin.eyes != null && CitizenSkinManager.partIdValid(reg.getEyes(), skin.eyes)) {
                result.eyes = skin.eyes;
            }
            if (skin.ears != null && reg.getEars().containsKey(skin.ears)) {
                result.ears = skin.ears;
            }
            if (skin.mouth != null && reg.getMouths().containsKey(skin.mouth)) {
                result.mouth = skin.mouth;
            }
            if (skin.facialHair != null && CitizenSkinManager.partIdValid(reg.getFacialHairs(), skin.facialHair)) {
                result.facialHair = skin.facialHair;
            }
            if (skin.haircut != null && CitizenSkinManager.partIdValid(reg.getHaircuts(), skin.haircut)) {
                result.haircut = skin.haircut;
            }
            if (skin.eyebrows != null && CitizenSkinManager.partIdValid(reg.getEyebrows(), skin.eyebrows)) {
                result.eyebrows = skin.eyebrows;
            }
            if (skin.pants != null && CitizenSkinManager.partIdValid(reg.getPants(), skin.pants)) {
                result.pants = skin.pants;
            }
            if (skin.overpants != null && CitizenSkinManager.partIdValid(reg.getOverpants(), skin.overpants)) {
                result.overpants = skin.overpants;
            }
            if (skin.undertop != null && CitizenSkinManager.partIdValid(reg.getUndertops(), skin.undertop)) {
                result.undertop = skin.undertop;
            }
            if (skin.overtop != null && CitizenSkinManager.partIdValid(reg.getOvertops(), skin.overtop)) {
                result.overtop = skin.overtop;
            }
            if (skin.shoes != null && CitizenSkinManager.partIdValid(reg.getShoes(), skin.shoes)) {
                result.shoes = skin.shoes;
            }
            if (skin.headAccessory != null && CitizenSkinManager.partIdValid(reg.getHeadAccessories(), skin.headAccessory)) {
                result.headAccessory = skin.headAccessory;
            }
            if (skin.faceAccessory != null && CitizenSkinManager.partIdValid(reg.getFaceAccessories(), skin.faceAccessory)) {
                result.faceAccessory = skin.faceAccessory;
            }
            if (skin.earAccessory != null && CitizenSkinManager.partIdValid(reg.getEarAccessories(), skin.earAccessory)) {
                result.earAccessory = skin.earAccessory;
            }
            if (skin.skinFeature != null && CitizenSkinManager.partIdValid(reg.getSkinFeatures(), skin.skinFeature)) {
                result.skinFeature = skin.skinFeature;
            }
            if (skin.gloves != null && CitizenSkinManager.partIdValid(reg.getGloves(), skin.gloves)) {
                result.gloves = skin.gloves;
            }
            if (skin.cape != null && CitizenSkinManager.partIdValid(reg.getCapes(), skin.cape)) {
                result.cape = skin.cape;
            }
            LOGGER.info("Sanitized skin: replaced null/invalid fields with random defaults");
            return result;
        }
    }

    private static boolean partIdValid(Map<String, ?> registryMap, String compoundValue) {
        if (compoundValue == null || !compoundValue.contains(".")) {
            return false;
        }
        String baseId = compoundValue.substring(0, compoundValue.indexOf(46));
        return registryMap.containsKey(baseId);
    }

    public CompletableFuture<PlayerSkin> fetchAndApplySkin(CitizenData citizen, Ref<EntityStore> entityRef, Executor worldExecutor) {
        if (citizen.skinUsername == null || citizen.skinUsername.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return ((CompletableFuture)this.fetchSkin(citizen.skinUsername).thenAcceptAsync(skin -> {
            if (skin != null && entityRef != null) {
                this.applySkin(entityRef, (PlayerSkin)skin, citizen.scale);
            }
        }, worldExecutor)).thenApply(v -> this.skinCache.containsKey(citizen.skinUsername.toLowerCase()) ? this.skinCache.get((Object)citizen.skinUsername.toLowerCase()).skin : null);
    }

    public void cleanupCache() {
        for (Map.Entry<String, CachedSkin> entry : this.skinCache.entrySet()) {
            if (!entry.getValue().isExpired()) continue;
            this.fetchSkin(entry.getKey());
        }
    }

    public void clearCache() {
        this.skinCache.clear();
    }

    public int getCacheSize() {
        return this.skinCache.size();
    }

    private static class CachedSkin {
        final PlayerSkin skin;
        final long fetchedAt;

        CachedSkin(PlayerSkin skin) {
            this.skin = skin;
            this.fetchedAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - this.fetchedAt > 1800000L;
        }
    }
}

