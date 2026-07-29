/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.common.util.ArrayUtil
 *  com.hypixel.hytale.protocol.ToClientPacket
 *  com.hypixel.hytale.protocol.packets.setup.AssetFinalize
 *  com.hypixel.hytale.protocol.packets.setup.AssetInitialize
 *  com.hypixel.hytale.protocol.packets.setup.AssetPart
 *  com.hypixel.hytale.protocol.packets.setup.RequestCommonAssetsRebuild
 *  com.hypixel.hytale.server.core.asset.common.CommonAsset
 *  com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry
 *  com.hypixel.hytale.server.core.io.PacketHandler
 */
package com.kyuubisoft.core.image;

import com.hypixel.hytale.common.util.ArrayUtil;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.setup.AssetFinalize;
import com.hypixel.hytale.protocol.packets.setup.AssetInitialize;
import com.hypixel.hytale.protocol.packets.setup.AssetPart;
import com.hypixel.hytale.protocol.packets.setup.RequestCommonAssetsRebuild;
import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import com.hypixel.hytale.server.core.io.PacketHandler;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class DynamicImageAsset
extends CommonAsset {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core");
    private static final String[] HASHES = new String[]{"00496e666f487562010000000000000000000000000000000000000000000000", "00496e666f487562020000000000000000000000000000000000000000000000", "00496e666f487562030000000000000000000000000000000000000000000000", "00496e666f487562040000000000000000000000000000000000000000000000", "00496e666f487562050000000000000000000000000000000000000000000000", "00496e666f487562060000000000000000000000000000000000000000000000", "00496e666f487562070000000000000000000000000000000000000000000000", "00496e666f487562080000000000000000000000000000000000000000000000", "00496e666f487562090000000000000000000000000000000000000000000000", "00496e666f4875620a0000000000000000000000000000000000000000000000", "00496e666f4875620b0000000000000000000000000000000000000000000000", "00496e666f4875620c0000000000000000000000000000000000000000000000", "00496e666f4875620d0000000000000000000000000000000000000000000000", "00496e666f4875620e0000000000000000000000000000000000000000000000"};
    private static final String[] PATHS = new String[]{"UI/Custom/Pages/InfoHub/DynamicImage1.png", "UI/Custom/Pages/InfoHub/DynamicImage2.png", "UI/Custom/Pages/InfoHub/DynamicImage3.png", "UI/Custom/Pages/InfoHub/DynamicImage4.png", "UI/Custom/Pages/InfoHub/DynamicImage5.png", "UI/Custom/Pages/InfoHub/DynamicImage6.png", "UI/Custom/Pages/InfoHub/DynamicImage7.png", "UI/Custom/Pages/InfoHub/DynamicImage8.png", "UI/Custom/Pages/InfoHub/DynamicImage9.png", "UI/Custom/Pages/InfoHub/DynamicImage10.png", "UI/Custom/Pages/InfoHub/DynamicImage11.png", "UI/Custom/Pages/InfoHub/DynamicImage12.png", "UI/Custom/Pages/InfoHub/DynamicImage13.png", "UI/Custom/Pages/InfoHub/DynamicImage14.png"};
    private static final Map<UUID, boolean[]> USED_SLOTS = new HashMap<UUID, boolean[]>();
    private static final UUID DEFAULT_PLAYER_UUID = new UUID(0L, 0L);
    private static final String NPC_AVATAR_PATH_PREFIX = "UI/Custom/Avatars/Npc/";
    private final byte[] data;
    private final int slotIndex;
    private final String customPath;
    private final UUID playerUuid;

    public DynamicImageAsset(byte[] pngData, UUID playerUuid) {
        this(pngData, DynamicImageAsset.claimSlot(playerUuid), playerUuid);
    }

    private DynamicImageAsset(byte[] pngData, int slotIndex, UUID playerUuid) {
        super(PATHS[slotIndex], HASHES[slotIndex], pngData);
        this.data = pngData;
        this.slotIndex = slotIndex;
        this.customPath = null;
        this.playerUuid = DynamicImageAsset.normalizePlayerUuid(playerUuid);
        LOGGER.fine("Dynamic image slot allocated: " + slotIndex + " path=" + PATHS[slotIndex]);
    }

    private DynamicImageAsset(byte[] pngData, String path, String hash, UUID playerUuid) {
        super(path, hash, pngData);
        this.data = pngData;
        this.slotIndex = -1;
        this.customPath = path;
        this.playerUuid = DynamicImageAsset.normalizePlayerUuid(playerUuid);
    }

    protected CompletableFuture<byte[]> getBlob0() {
        return CompletableFuture.completedFuture(this.data);
    }

    public String getPath() {
        return this.customPath != null ? this.customPath : PATHS[this.slotIndex];
    }

    public int getSlotIndex() {
        return this.slotIndex;
    }

    public static DynamicImageAsset createForNpc(byte[] pngData, UUID playerUuid, String username) {
        String safeName = username.trim().toLowerCase().replaceAll("[^a-z0-9_]", "");
        String path = NPC_AVATAR_PATH_PREFIX + safeName + ".png";
        String hash = DynamicImageAsset.generateNpcHash(safeName);
        return new DynamicImageAsset(pngData, path, hash, playerUuid);
    }

    public static String getNpcAvatarPath(String username) {
        String safeName = username.trim().toLowerCase().replaceAll("[^a-z0-9_]", "");
        return NPC_AVATAR_PATH_PREFIX + safeName + ".png";
    }

    private static String generateNpcHash(String safeName) {
        byte[] nameBytes;
        StringBuilder sb = new StringBuilder("004e504341");
        for (byte b : nameBytes = safeName.getBytes(StandardCharsets.UTF_8)) {
            sb.append(String.format("%02x", b));
        }
        while (sb.length() < 64) {
            sb.append("0");
        }
        return sb.substring(0, 64);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static DynamicImageAsset createForSlot(byte[] pngData, UUID playerUuid, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= PATHS.length) {
            throw new IllegalArgumentException("Invalid slot index: " + slotIndex);
        }
        Map<UUID, boolean[]> map = USED_SLOTS;
        synchronized (map) {
            boolean[] slots = DynamicImageAsset.getSlots(playerUuid);
            slots[slotIndex] = true;
        }
        return new DynamicImageAsset(pngData, slotIndex, playerUuid);
    }

    public static CommonAsset empty(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= PATHS.length) {
            throw new IllegalArgumentException("Invalid slot index: " + slotIndex);
        }
        return CommonAssetRegistry.getByName((String)PATHS[slotIndex]);
    }

    public static void sendToPlayer(PacketHandler handler, CommonAsset asset) {
        DynamicImageAsset.sendToPlayer(handler, asset, true);
    }

    public static void sendToPlayer(PacketHandler handler, CommonAsset asset, boolean requestRebuild) {
        if (handler == null || asset == null) {
            LOGGER.warning("Cannot send asset: handler or asset is null");
            return;
        }
        try {
            byte[] allBytes = (byte[])asset.getBlob().join();
            byte[][] parts = ArrayUtil.split((byte[])allBytes, (int)0x280000);
            ToClientPacket[] packets = new ToClientPacket[2 + parts.length];
            packets[0] = new AssetInitialize(asset.toPacket(), allBytes.length);
            for (int i = 0; i < parts.length; ++i) {
                packets[1 + i] = new AssetPart(parts[i]);
            }
            packets[packets.length - 1] = new AssetFinalize();
            handler.write(packets);
            if (requestRebuild) {
                handler.writeNoCache((ToClientPacket)new RequestCommonAssetsRebuild());
            }
            LOGGER.fine("Sent dynamic image asset: " + asset.getName() + " (" + allBytes.length + " bytes)");
        }
        catch (Exception e) {
            LOGGER.warning("Failed to send asset: " + e.getMessage());
        }
    }

    public static void requestRebuild(PacketHandler handler) {
        if (handler == null) {
            return;
        }
        handler.writeNoCache((ToClientPacket)new RequestCommonAssetsRebuild());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static int claimSlot(UUID playerUuid) {
        Map<UUID, boolean[]> map = USED_SLOTS;
        synchronized (map) {
            boolean[] slots = DynamicImageAsset.getSlots(playerUuid);
            for (int i = slots.length - 1; i >= 0; --i) {
                if (slots[i]) continue;
                slots[i] = true;
                LOGGER.fine("Claimed dynamic image slot: " + i + " for player " + String.valueOf(playerUuid));
                return i;
            }
        }
        throw new IllegalStateException("No dynamic image slots available (max " + PATHS.length + ")");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void releaseSlot(UUID playerUuid, int slotIndex) {
        Map<UUID, boolean[]> map = USED_SLOTS;
        synchronized (map) {
            boolean[] slots = DynamicImageAsset.getSlots(playerUuid);
            if (slotIndex >= 0 && slotIndex < slots.length) {
                slots[slotIndex] = false;
                LOGGER.fine("Released dynamic image slot: " + slotIndex + " for player " + String.valueOf(playerUuid));
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void releaseAllSlots(UUID playerUuid) {
        Map<UUID, boolean[]> map = USED_SLOTS;
        synchronized (map) {
            USED_SLOTS.remove(DynamicImageAsset.normalizePlayerUuid(playerUuid));
            LOGGER.fine("Released all dynamic image slots for player " + String.valueOf(playerUuid));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static int getAvailableSlotCount(UUID playerUuid) {
        Map<UUID, boolean[]> map = USED_SLOTS;
        synchronized (map) {
            boolean[] slots = DynamicImageAsset.getSlots(playerUuid);
            int count = 0;
            for (boolean used : slots) {
                if (used) continue;
                ++count;
            }
            return count;
        }
    }

    private static boolean[] getSlots(UUID playerUuid) {
        UUID normalized = DynamicImageAsset.normalizePlayerUuid(playerUuid);
        return USED_SLOTS.computeIfAbsent(normalized, k -> new boolean[PATHS.length]);
    }

    private static UUID normalizePlayerUuid(UUID playerUuid) {
        return playerUuid != null ? playerUuid : DEFAULT_PLAYER_UUID;
    }

    public static int getTotalSlots() {
        return PATHS.length;
    }
}

