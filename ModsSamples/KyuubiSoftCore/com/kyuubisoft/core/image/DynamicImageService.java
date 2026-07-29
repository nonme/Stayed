/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 */
package com.kyuubisoft.core.image;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.kyuubisoft.core.image.DynamicImageAsset;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class DynamicImageService {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core");
    private static DynamicImageService instance;
    public static final String HYVATAR_BASE_URL = "https://hyvatar.io";
    private static final Map<String, CacheEntry> IMAGE_CACHE;
    private static final long CACHE_TTL_MS = 300000L;
    private final Map<UUID, Map<String, Integer>> playerImages = new ConcurrentHashMap<UUID, Map<String, Integer>>();
    private Path avatarCacheDir;
    private final HttpClient httpClient;
    private final ExecutorService downloadExecutor;
    private final Map<UUID, ScheduledFuture<?>> pendingRebuilds = new ConcurrentHashMap();
    private final ScheduledExecutorService rebuildScheduler;
    private final Map<UUID, Long> lastRebuildTime = new ConcurrentHashMap<UUID, Long>();
    private static final long MIN_REBUILD_INTERVAL_MS = 1000L;
    private final Map<UUID, Set<String>> preloadedNpcAvatars = new ConcurrentHashMap<UUID, Set<String>>();

    public DynamicImageService() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();
        this.downloadExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "Core-ImageDownloader");
            t.setDaemon(true);
            return t;
        });
        this.rebuildScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Core-AssetRebuildDebounce");
            t.setDaemon(true);
            return t;
        });
        instance = this;
    }

    private void scheduleRebuild(PlayerRef playerRef) {
        UUID playerId = playerRef.getUuid();
        ScheduledFuture<?> existing = this.pendingRebuilds.remove(playerId);
        if (existing != null) {
            existing.cancel(false);
        }
        long now = System.currentTimeMillis();
        long lastRebuild = this.lastRebuildTime.getOrDefault(playerId, 0L);
        long sinceLastRebuild = now - lastRebuild;
        long delay = Math.max(1000L, 1000L - sinceLastRebuild);
        this.pendingRebuilds.put(playerId, this.rebuildScheduler.schedule(() -> {
            this.pendingRebuilds.remove(playerId);
            try {
                this.lastRebuildTime.put(playerId, System.currentTimeMillis());
                DynamicImageAsset.requestRebuild(playerRef.getPacketHandler());
                LOGGER.fine("Debounced asset rebuild sent for " + String.valueOf(playerId));
            }
            catch (Exception e) {
                LOGGER.fine("Failed to send debounced rebuild: " + e.getMessage());
            }
        }, delay, TimeUnit.MILLISECONDS));
    }

    public void cleanupPlayer(UUID playerId) {
        ScheduledFuture<?> pending = this.pendingRebuilds.remove(playerId);
        if (pending != null) {
            pending.cancel(false);
        }
        this.lastRebuildTime.remove(playerId);
        this.playerImages.remove(playerId);
        this.preloadedNpcAvatars.remove(playerId);
    }

    public static DynamicImageService getInstance() {
        return instance;
    }

    public String loadPlayerAvatar(PlayerRef playerRef, String username, RenderType type, int size) {
        if (playerRef == null || username == null || username.isEmpty()) {
            return null;
        }
        String url = DynamicImageService.buildHyvatarUrl(username, type, size);
        return this.loadImageFromUrl(playerRef, url, "avatar_" + username + "_" + type.name());
    }

    public String loadPlayerHead(PlayerRef playerRef, String username) {
        return this.loadPlayerAvatar(playerRef, username, RenderType.HEAD, 64);
    }

    public String loadPlayerAvatarToSlot(PlayerRef playerRef, String username, int slotIndex) {
        if (playerRef == null || username == null || username.isEmpty()) {
            return null;
        }
        if (slotIndex < 0 || slotIndex >= DynamicImageAsset.getTotalSlots()) {
            LOGGER.warning("Invalid slot index: " + slotIndex);
            return null;
        }
        String url = DynamicImageService.buildHyvatarUrl(username, RenderType.HEAD, 128);
        return this.loadImageToSlot(playerRef, url, slotIndex, "avatar_slot" + slotIndex + "_" + username);
    }

    public void loadPlayerAvatarToSlotAsync(PlayerRef playerRef, String username, int slotIndex, Consumer<String> onComplete) {
        if (playerRef == null || username == null || username.isEmpty()) {
            if (onComplete != null) {
                onComplete.accept(null);
            }
            return;
        }
        if (slotIndex < 0 || slotIndex >= DynamicImageAsset.getTotalSlots()) {
            LOGGER.warning("Invalid slot index: " + slotIndex);
            if (onComplete != null) {
                onComplete.accept(null);
            }
            return;
        }
        String url = DynamicImageService.buildHyvatarUrl(username, RenderType.HEAD, 128);
        String imageKey = "avatar_slot" + slotIndex + "_" + username;
        this.downloadExecutor.submit(() -> {
            String result = this.loadImageToSlot(playerRef, url, slotIndex, imageKey);
            if (onComplete != null) {
                onComplete.accept(result);
            }
        });
    }

    public void setAvatarCacheDir(Path dir) {
        this.avatarCacheDir = dir;
        try {
            Files.createDirectories(dir, new FileAttribute[0]);
        }
        catch (IOException e) {
            LOGGER.warning("Failed to create avatar cache dir: " + e.getMessage());
        }
    }

    public void loadPlayerAvatarToSlotPersistentAsync(PlayerRef playerRef, String username, int slotIndex, Consumer<String> onComplete) {
        if (playerRef == null || username == null || username.isEmpty()) {
            if (onComplete != null) {
                onComplete.accept(null);
            }
            return;
        }
        if (slotIndex < 0 || slotIndex >= DynamicImageAsset.getTotalSlots()) {
            LOGGER.warning("Invalid slot index: " + slotIndex);
            if (onComplete != null) {
                onComplete.accept(null);
            }
            return;
        }
        UUID playerId = playerRef.getUuid();
        String imageKey = "avatar_slot" + slotIndex + "_" + username;
        Map<String, Integer> existingSlots = this.playerImages.get(playerId);
        if (existingSlots != null && existingSlots.containsKey(imageKey)) {
            String cachedPath = this.getPathForSlot(slotIndex);
            LOGGER.fine("Avatar already loaded for player, skipping re-send: " + imageKey);
            if (onComplete != null) {
                onComplete.accept(cachedPath);
            }
            return;
        }
        this.downloadExecutor.submit(() -> {
            block7: {
                try {
                    String url;
                    Path cacheFile;
                    byte[] imageBytes = null;
                    String safeUsername = username.trim().toLowerCase().replaceAll("[^a-z0-9_]", "");
                    Path path = cacheFile = this.avatarCacheDir != null ? this.avatarCacheDir.resolve(safeUsername + ".png") : null;
                    if (cacheFile != null && Files.exists(cacheFile, new LinkOption[0])) {
                        imageBytes = Files.readAllBytes(cacheFile);
                        LOGGER.fine("Avatar loaded from disk cache: " + safeUsername);
                    }
                    if ((imageBytes == null || imageBytes.length == 0) && (imageBytes = this.downloadImage(url = DynamicImageService.buildHyvatarUrl(username, RenderType.HEAD, 128))) != null && imageBytes.length > 0 && cacheFile != null) {
                        Files.write(cacheFile, imageBytes, new OpenOption[0]);
                        LOGGER.fine("Avatar saved to disk cache: " + safeUsername);
                    }
                    if (imageBytes == null || imageBytes.length == 0) {
                        if (onComplete != null) {
                            onComplete.accept(null);
                        }
                        return;
                    }
                    DynamicImageAsset asset = DynamicImageAsset.createForSlot(imageBytes, playerId, slotIndex);
                    DynamicImageAsset.sendToPlayer(playerRef.getPacketHandler(), asset, false);
                    this.scheduleRebuild(playerRef);
                    this.playerImages.computeIfAbsent(playerId, k -> new ConcurrentHashMap()).put(imageKey, slotIndex);
                    if (onComplete != null) {
                        onComplete.accept(asset.getPath());
                    }
                }
                catch (Exception e) {
                    LOGGER.warning("Failed to load persistent avatar for " + username + ": " + e.getMessage());
                    if (onComplete == null) break block7;
                    onComplete.accept(null);
                }
            }
        });
    }

    public void preloadNpcAvatarAsync(PlayerRef playerRef, String username, Consumer<String> onComplete) {
        if (playerRef == null || username == null || username.isEmpty()) {
            if (onComplete != null) {
                onComplete.accept(null);
            }
            return;
        }
        UUID playerId = playerRef.getUuid();
        String safeName = username.trim().toLowerCase().replaceAll("[^a-z0-9_]", "");
        Set<String> preloaded = this.preloadedNpcAvatars.get(playerId);
        if (preloaded != null && preloaded.contains(safeName)) {
            String path = DynamicImageAsset.getNpcAvatarPath(username);
            if (onComplete != null) {
                onComplete.accept(path);
            }
            return;
        }
        this.downloadExecutor.submit(() -> {
            block7: {
                try {
                    String url;
                    Path cacheFile;
                    byte[] imageBytes = null;
                    Path path = cacheFile = this.avatarCacheDir != null ? this.avatarCacheDir.resolve(safeName + ".png") : null;
                    if (cacheFile != null && Files.exists(cacheFile, new LinkOption[0])) {
                        imageBytes = Files.readAllBytes(cacheFile);
                    }
                    if ((imageBytes == null || imageBytes.length == 0) && (imageBytes = this.downloadImage(url = DynamicImageService.buildHyvatarUrl(username, RenderType.HEAD, 128))) != null && imageBytes.length > 0 && cacheFile != null) {
                        Files.write(cacheFile, imageBytes, new OpenOption[0]);
                    }
                    if (imageBytes == null || imageBytes.length == 0) {
                        if (onComplete != null) {
                            onComplete.accept(null);
                        }
                        return;
                    }
                    DynamicImageAsset asset = DynamicImageAsset.createForNpc(imageBytes, playerId, username);
                    DynamicImageAsset.sendToPlayer(playerRef.getPacketHandler(), asset, false);
                    this.preloadedNpcAvatars.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(safeName);
                    if (onComplete != null) {
                        onComplete.accept(asset.getPath());
                    }
                }
                catch (Exception e) {
                    LOGGER.warning("Failed to preload NPC avatar for " + username + ": " + e.getMessage());
                    if (onComplete == null) break block7;
                    onComplete.accept(null);
                }
            }
        });
    }

    public void loadNpcAvatarAsync(PlayerRef playerRef, String username, Consumer<String> onComplete) {
        if (playerRef == null || username == null || username.isEmpty()) {
            if (onComplete != null) {
                onComplete.accept(null);
            }
            return;
        }
        UUID playerId = playerRef.getUuid();
        String safeName = username.trim().toLowerCase().replaceAll("[^a-z0-9_]", "");
        Set<String> preloaded = this.preloadedNpcAvatars.get(playerId);
        if (preloaded != null && preloaded.contains(safeName)) {
            String path = DynamicImageAsset.getNpcAvatarPath(username);
            if (onComplete != null) {
                onComplete.accept(path);
            }
            return;
        }
        this.downloadExecutor.submit(() -> {
            block7: {
                try {
                    String url;
                    Path cacheFile;
                    byte[] imageBytes = null;
                    Path path = cacheFile = this.avatarCacheDir != null ? this.avatarCacheDir.resolve(safeName + ".png") : null;
                    if (cacheFile != null && Files.exists(cacheFile, new LinkOption[0])) {
                        imageBytes = Files.readAllBytes(cacheFile);
                    }
                    if ((imageBytes == null || imageBytes.length == 0) && (imageBytes = this.downloadImage(url = DynamicImageService.buildHyvatarUrl(username, RenderType.HEAD, 128))) != null && imageBytes.length > 0 && cacheFile != null) {
                        Files.write(cacheFile, imageBytes, new OpenOption[0]);
                    }
                    if (imageBytes == null || imageBytes.length == 0) {
                        if (onComplete != null) {
                            onComplete.accept(null);
                        }
                        return;
                    }
                    DynamicImageAsset asset = DynamicImageAsset.createForNpc(imageBytes, playerId, username);
                    DynamicImageAsset.sendToPlayer(playerRef.getPacketHandler(), asset, false);
                    this.scheduleRebuild(playerRef);
                    this.preloadedNpcAvatars.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(safeName);
                    if (onComplete != null) {
                        onComplete.accept(asset.getPath());
                    }
                }
                catch (Exception e) {
                    LOGGER.warning("Failed to load NPC avatar for " + username + ": " + e.getMessage());
                    if (onComplete == null) break block7;
                    onComplete.accept(null);
                }
            }
        });
    }

    public void triggerRebuild(PlayerRef playerRef) {
        this.scheduleRebuild(playerRef);
    }

    public String loadImageToSlot(PlayerRef playerRef, String url, int slotIndex, String imageKey) {
        if (playerRef == null || url == null || url.isEmpty()) {
            return null;
        }
        UUID playerId = playerRef.getUuid();
        try {
            byte[] imageBytes = this.downloadImage(url);
            if (imageBytes == null || imageBytes.length == 0) {
                LOGGER.warning("Failed to download image: " + url);
                return null;
            }
            DynamicImageAsset asset = DynamicImageAsset.createForSlot(imageBytes, playerId, slotIndex);
            DynamicImageAsset.sendToPlayer(playerRef.getPacketHandler(), asset, false);
            this.scheduleRebuild(playerRef);
            this.playerImages.computeIfAbsent(playerId, k -> new ConcurrentHashMap()).put(imageKey, slotIndex);
            LOGGER.info("Loaded dynamic image for player " + String.valueOf(playerId) + " in slot " + slotIndex + ": " + imageKey);
            return asset.getPath();
        }
        catch (Exception e) {
            LOGGER.warning("Failed to load image to slot " + slotIndex + ": " + e.getMessage());
            return null;
        }
    }

    public String loadImageFromUrl(PlayerRef playerRef, String url, String imageKey) {
        if (playerRef == null || url == null || url.isEmpty()) {
            return null;
        }
        UUID playerId = playerRef.getUuid();
        try {
            Map<String, Integer> playerSlots = this.playerImages.get(playerId);
            if (playerSlots != null && playerSlots.containsKey(imageKey)) {
                int existingSlot = playerSlots.get(imageKey);
                LOGGER.fine("Image already loaded for player: " + imageKey + " in slot " + existingSlot);
                return this.getPathForSlot(existingSlot);
            }
            byte[] imageBytes = this.downloadImage(url);
            if (imageBytes == null || imageBytes.length == 0) {
                LOGGER.warning("Failed to download image: " + url);
                return null;
            }
            if (DynamicImageAsset.getAvailableSlotCount(playerId) == 0) {
                LOGGER.warning("No available image slots for player " + String.valueOf(playerId));
                return null;
            }
            DynamicImageAsset asset = new DynamicImageAsset(imageBytes, playerId);
            DynamicImageAsset.sendToPlayer(playerRef.getPacketHandler(), DynamicImageAsset.empty(asset.getSlotIndex()), false);
            DynamicImageAsset.sendToPlayer(playerRef.getPacketHandler(), asset, false);
            this.scheduleRebuild(playerRef);
            this.playerImages.computeIfAbsent(playerId, k -> new ConcurrentHashMap()).put(imageKey, asset.getSlotIndex());
            LOGGER.info("Loaded dynamic image for player " + String.valueOf(playerId) + ": " + imageKey);
            return asset.getPath();
        }
        catch (IllegalStateException e) {
            LOGGER.warning("No slots available: " + e.getMessage());
            return null;
        }
        catch (Exception e) {
            LOGGER.warning("Failed to load image: " + e.getMessage());
            return null;
        }
    }

    public void releasePlayerImages(UUID playerId) {
        Map<String, Integer> slots = this.playerImages.remove(playerId);
        if (slots != null) {
            for (Integer slotIndex : slots.values()) {
                DynamicImageAsset.releaseSlot(playerId, slotIndex);
            }
            LOGGER.fine("Released " + slots.size() + " images for player " + String.valueOf(playerId));
        }
        DynamicImageAsset.releaseAllSlots(playerId);
    }

    public void releaseImage(UUID playerId, String imageKey) {
        Integer slotIndex;
        Map<String, Integer> slots = this.playerImages.get(playerId);
        if (slots != null && (slotIndex = slots.remove(imageKey)) != null) {
            DynamicImageAsset.releaseSlot(playerId, slotIndex);
            LOGGER.fine("Released image " + imageKey + " for player " + String.valueOf(playerId));
        }
    }

    public static String buildHyvatarUrl(String username, RenderType type, int size) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        RenderType resolvedType = type != null ? type : RenderType.HEAD;
        String encodedUsername = URLEncoder.encode(username.trim(), StandardCharsets.UTF_8).replace("+", "%20");
        StringBuilder url = new StringBuilder(HYVATAR_BASE_URL).append("/").append(resolvedType.getPath()).append("/").append(encodedUsername);
        int normalizedSize = Math.max(64, Math.min(2048, size));
        url.append("?size=").append(normalizedSize);
        return url.toString();
    }

    private byte[] downloadImage(String url) {
        CacheEntry cached = IMAGE_CACHE.get(url);
        if (cached != null && !cached.isExpired()) {
            LOGGER.fine("Image cache hit: " + url);
            return cached.data;
        }
        try {
            LOGGER.fine("Downloading image: " + url);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().header("Accept", "image/png,image/*").timeout(Duration.ofSeconds(15L)).build();
            HttpResponse<byte[]> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                LOGGER.warning("Image download failed with HTTP " + response.statusCode() + ": " + url);
                return null;
            }
            byte[] data = response.body();
            LOGGER.fine("Downloaded image: " + url + " (" + data.length + " bytes)");
            IMAGE_CACHE.put(url, new CacheEntry(data));
            return data;
        }
        catch (IOException | InterruptedException e) {
            LOGGER.warning("Failed to download image from " + url + ": " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    private String getPathForSlot(int slotIndex) {
        return "UI/Custom/Pages/InfoHub/DynamicImage" + (slotIndex + 1) + ".png";
    }

    public void clearCache() {
        IMAGE_CACHE.clear();
        LOGGER.info("Image cache cleared");
    }

    public void shutdown() {
        this.playerImages.clear();
        IMAGE_CACHE.clear();
        this.downloadExecutor.shutdownNow();
        instance = null;
    }

    static {
        IMAGE_CACHE = new ConcurrentHashMap<String, CacheEntry>();
    }

    public static enum RenderType {
        HEAD("render"),
        FULL("render/full"),
        CAPE("render/cape");

        private final String path;

        private RenderType(String path) {
            this.path = path;
        }

        public String getPath() {
            return this.path;
        }
    }

    private static class CacheEntry {
        final byte[] data;
        final long createdAt;

        CacheEntry(byte[] data) {
            this.data = data;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - this.createdAt > 300000L;
        }
    }
}

