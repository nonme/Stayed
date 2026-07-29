/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.math.util.ChunkUtil
 *  com.hypixel.hytale.math.vector.Transform
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.server.core.entity.UUIDComponent
 *  com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
 *  com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent
 *  com.hypixel.hytale.server.core.modules.entity.item.ItemComponent
 *  com.hypixel.hytale.server.core.modules.entity.item.PickupItemComponent
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.Universe
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  com.hypixel.hytale.server.npc.entities.NPCEntity
 */
package com.yourname.companion.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PickupItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.yourname.companion.data.BlockPos;
import java.lang.invoke.CallSite;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class WorldQueries {
    private WorldQueries() {
    }

    public static BlockPos getLookBlockPos(PlayerRef playerRef, double distance) {
        if (playerRef == null) {
            return null;
        }
        try {
            if (playerRef.getWorldUuid() != null) {
                World world = Universe.get().getWorld(playerRef.getWorldUuid());
                BlockPos exact = WorldQueries.tryGetTargetBlockViaTargetUtil(world, playerRef, distance);
                if (exact != null) {
                    return exact;
                }
                BlockPos los = WorldQueries.getLookedAtSolidBlockPos(world, playerRef, distance);
                if (los != null) {
                    return los;
                }
            }
        }
        catch (Throwable world) {
            // empty catch block
        }
        Transform transform = playerRef.getTransform();
        if (transform == null) {
            return null;
        }
        Vector3d start = transform.getPosition();
        Vector3d direction = transform.getDirection();
        if (start == null || direction == null) {
            return null;
        }
        Vector3d normalized = direction.clone();
        if (normalized.length() <= 1.0E-4) {
            return null;
        }
        normalized.normalize().scale(distance);
        Vector3d target = new Vector3d(start).add(normalized);
        int x = (int)Math.floor(target.x);
        int y = (int)Math.floor(target.y);
        int z = (int)Math.floor(target.z);
        return new BlockPos(playerRef.getWorldUuid().toString(), x, y, z);
    }

    public static BlockPos getLookedAtSolidBlockPos(World world, PlayerRef playerRef, double maxDistance) {
        if (world == null || playerRef == null) {
            return null;
        }
        Transform transform = playerRef.getTransform();
        if (transform == null) {
            return null;
        }
        Vector3d start = transform.getPosition();
        Vector3d direction = transform.getDirection();
        if (start == null || direction == null) {
            return null;
        }
        Vector3d normalized = direction.clone();
        if (normalized.length() <= 1.0E-4) {
            return null;
        }
        normalized.normalize();
        HashSet<CallSite> visited = new HashSet<CallSite>();
        double step = 0.2;
        for (double d = 0.35; d <= maxDistance; d += step) {
            String blockType;
            int z;
            int y;
            int x = (int)Math.floor(start.x + normalized.x * d);
            String key = x + ":" + (y = (int)Math.floor(start.y + normalized.y * d)) + ":" + (z = (int)Math.floor(start.z + normalized.z * d));
            if (!visited.add((CallSite)((Object)key)) || WorldQueries.isAirLikeBlockType(blockType = WorldQueries.getBlockTypeAt(world, x, y, z))) continue;
            return new BlockPos(playerRef.getWorldUuid().toString(), x, y, z);
        }
        return null;
    }

    public static BlockPos getRobustLookBlockPos(World world, PlayerRef playerRef, double maxDistance) {
        Transform transform;
        if (world == null || playerRef == null) {
            return null;
        }
        BlockPos exact = WorldQueries.tryGetTargetBlockViaTargetUtil(world, playerRef, maxDistance);
        if (exact != null) {
            return exact;
        }
        BlockPos direct = WorldQueries.getLookedAtSolidBlockPos(world, playerRef, maxDistance);
        if (direct != null) {
            return direct;
        }
        BlockPos approx = WorldQueries.getLookBlockPos(playerRef, maxDistance);
        if (approx != null) {
            for (int dy = 0; dy <= 4; ++dy) {
                int y = approx.y - dy;
                String type = WorldQueries.getBlockTypeAt(world, approx.x, y, approx.z);
                if (WorldQueries.isAirLikeBlockType(type)) continue;
                return new BlockPos(approx.worldId, approx.x, y, approx.z);
            }
        }
        if ((transform = playerRef.getTransform()) != null && transform.getPosition() != null) {
            int px = (int)Math.floor(transform.getPosition().x);
            int py = (int)Math.floor(transform.getPosition().y);
            int pz = (int)Math.floor(transform.getPosition().z);
            for (int dy = 1; dy <= 4; ++dy) {
                int y = py - dy;
                String type = WorldQueries.getBlockTypeAt(world, px, y, pz);
                if (WorldQueries.isAirLikeBlockType(type)) continue;
                return new BlockPos(playerRef.getWorldUuid().toString(), px, y, pz);
            }
        }
        return null;
    }

    public static BlockPos getLookedAtStorageBlockPos(World world, PlayerRef playerRef, double maxDistance) {
        BlockPos approx;
        if (world == null || playerRef == null || playerRef.getWorldUuid() == null) {
            return null;
        }
        BlockPos direct = WorldQueries.getLookedAtSolidBlockPos(world, playerRef, maxDistance);
        BlockPos exact = WorldQueries.tryGetTargetBlockViaTargetUtil(world, playerRef, maxDistance);
        BlockPos[] candidates = WorldQueries.sameBlockPos(exact, approx = WorldQueries.getLookBlockPos(playerRef, maxDistance)) && exact != null ? new BlockPos[]{exact, direct, approx} : new BlockPos[]{exact, approx, direct};
        for (BlockPos candidate : candidates) {
            BlockPos resolved = WorldQueries.resolveNearbyStorageBlock(world, candidate);
            if (resolved == null) continue;
            return resolved;
        }
        for (BlockPos candidate : candidates) {
            if (candidate == null) continue;
            return candidate;
        }
        return null;
    }

    public static String describeStorageLookResolution(World world, PlayerRef playerRef, double maxDistance) {
        if (world == null || playerRef == null || playerRef.getWorldUuid() == null) {
            return "world/player unavailable";
        }
        BlockPos direct = WorldQueries.getLookedAtSolidBlockPos(world, playerRef, maxDistance);
        BlockPos exact = WorldQueries.tryGetTargetBlockViaTargetUtil(world, playerRef, maxDistance);
        BlockPos approx = WorldQueries.getLookBlockPos(playerRef, maxDistance);
        BlockPos resolved = WorldQueries.getLookedAtStorageBlockPos(world, playerRef, maxDistance);
        return "direct=" + WorldQueries.describeStorageCandidate(world, direct) + " exact=" + WorldQueries.describeStorageCandidate(world, exact) + " approx=" + WorldQueries.describeStorageCandidate(world, approx) + " resolved=" + WorldQueries.describeStorageCandidate(world, resolved);
    }

    private static String describeStorageCandidate(World world, BlockPos pos) {
        if (pos == null) {
            return "null";
        }
        String blockId = WorldQueries.getBlockTypeAt(world, pos.x, pos.y, pos.z);
        String stateName = "null";
        try {
            Object state = WorldQueries.invokeMethod(world, "getState", pos.x, pos.y, pos.z, true);
            if (state != null) {
                stateName = state.getClass().getSimpleName();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return WorldQueries.formatBlockPos(pos) + "[" + String.valueOf(blockId) + "|" + stateName + "]";
    }

    private static String formatBlockPos(BlockPos pos) {
        return "(" + pos.x + "," + pos.y + "," + pos.z + ")";
    }

    private static boolean sameBlockPos(BlockPos a, BlockPos b) {
        if (a == null || b == null) {
            return false;
        }
        return a.x == b.x && a.y == b.y && a.z == b.z && Objects.equals(a.worldId, b.worldId);
    }

    private static BlockPos resolveNearbyStorageBlock(World world, BlockPos origin) {
        int[][] offsets;
        if (world == null || origin == null) {
            return null;
        }
        for (int[] offset : offsets = new int[][]{{0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}}) {
            int x = origin.x + offset[0];
            int y = origin.y + offset[1];
            int z = origin.z + offset[2];
            if (!WorldQueries.hasStorageContainer(world, x, y, z)) continue;
            return new BlockPos(origin.worldId, x, y, z);
        }
        return null;
    }

    private static boolean hasStorageContainer(World world, int x, int y, int z) {
        try {
            String[] methods;
            long chunkIndex = ChunkUtil.indexChunkFromBlock((int)x, (int)z);
            WorldChunk chunk = world.getChunk(chunkIndex);
            if (chunk == null) {
                return false;
            }
            Object state = WorldQueries.getChunkStateReflectively(chunk, x, y, z);
            if (state == null) {
                int localX = x & 0x1F;
                int localZ = z & 0x1F;
                state = WorldQueries.getChunkStateReflectively(chunk, localX, y, localZ);
            }
            if (state == null) {
                return false;
            }
            try {
                Method m = state.getClass().getMethod("getItemContainer", new Class[0]);
                if (m.invoke(state, new Object[0]) != null) {
                    return true;
                }
            }
            catch (Throwable m) {
                // empty catch block
            }
            for (String methodName : methods = new String[]{"getItemContainer", "getContainer", "getStorage", "getInventory", "getPrimaryContainer", "getInputContainer", "getOutputContainer"}) {
                try {
                    Method m = state.getClass().getMethod(methodName, new Class[0]);
                    Object result = m.invoke(state, new Object[0]);
                    if (result == null) continue;
                    return true;
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
            String blockId = WorldQueries.getBlockTypeAt(world, x, y, z);
            if (blockId != null) {
                String low = blockId.toLowerCase();
                return low.contains("quickstacker") || low.contains("chest") || low.contains("container") || low.contains("box");
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return false;
    }

    private static BlockPos tryGetTargetBlockViaTargetUtil(World world, PlayerRef playerRef, double maxDistance) {
        if (world == null || playerRef == null || playerRef.getWorldUuid() == null) {
            return null;
        }
        try {
            Class<?> targetUtil = Class.forName("com.hypixel.hytale.server.core.util.TargetUtil");
            Method m = null;
            for (Method cand : targetUtil.getMethods()) {
                Class<?>[] p;
                if (!"getTargetBlock".equals(cand.getName()) || (p = cand.getParameterTypes()).length != 3 || !p[0].isAssignableFrom(playerRef.getReference().getClass())) continue;
                m = cand;
                break;
            }
            if (m == null) {
                return null;
            }
            Store accessor = world.getEntityStore().getStore();
            Object vec = m.invoke(null, playerRef.getReference(), maxDistance, accessor);
            if (vec == null) {
                return null;
            }
            Integer x = WorldQueries.invokeIntGetter(vec, "getX");
            Integer y = WorldQueries.invokeIntGetter(vec, "getY");
            Integer z = WorldQueries.invokeIntGetter(vec, "getZ");
            if (x == null || y == null || z == null) {
                return null;
            }
            try {
                Transform tf = playerRef.getTransform();
                if (tf != null && tf.getPosition() != null) {
                    double dz;
                    double dy;
                    Vector3d p = tf.getPosition();
                    double dx = (double)x.intValue() + 0.5 - p.x;
                    double dist = Math.sqrt(dx * dx + (dy = (double)y.intValue() + 0.5 - p.y) * dy + (dz = (double)z.intValue() + 0.5 - p.z) * dz);
                    if (dist > maxDistance + 4.0) {
                        return null;
                    }
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            return new BlockPos(playerRef.getWorldUuid().toString(), x, y, z);
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private static Integer invokeIntGetter(Object obj, String name) {
        try {
            Object v = obj.getClass().getMethod(name, new Class[0]).invoke(obj, new Object[0]);
            if (v instanceof Number) {
                Number n = (Number)v;
                return n.intValue();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    private static boolean isAirLikeBlockType(String blockType) {
        if (blockType == null || blockType.isBlank()) {
            return true;
        }
        String v = blockType.trim().toLowerCase();
        if (v.startsWith("*")) {
            v = v.substring(1).trim();
        }
        return v.equals("air") || v.equals("empty") || v.endsWith(":air") || v.endsWith(":empty") || v.contains("id=empty") || v.contains("group='air'") || v.contains("drawtype=empty") || v.contains("material=empty") || v.contains(" blockid:0") || v.startsWith("blockid:0");
    }

    public static List<NearbyEntity> getNearbyLivingEntities(World world, Vector3d center, double radius) {
        ArrayList<NearbyEntity> result = new ArrayList<NearbyEntity>();
        if (world == null || center == null) {
            return result;
        }
        try {
            Store store = world.getEntityStore().getStore();
            double radiusSq = radius * radius;
            store.forEachChunk((Query)TransformComponent.getComponentType(), (chunk, cmd) -> {
                for (int i = 0; i < chunk.size(); ++i) {
                    double distSq;
                    Vector3d pos;
                    DeathComponent death;
                    TransformComponent transform;
                    Ref ref = chunk.getReferenceTo(i);
                    if (ref == null || !ref.isValid() || (transform = (TransformComponent)chunk.getComponent(i, TransformComponent.getComponentType())) == null || (death = (DeathComponent)chunk.getComponent(i, DeathComponent.getComponentType())) != null || (pos = transform.getPosition()) == null || !((distSq = WorldQueries.distanceSquared(center, pos)) <= radiusSq)) continue;
                    result.add(new NearbyEntity((Ref<EntityStore>)ref, pos, Math.sqrt(distSq)));
                }
            });
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return result;
    }

    public static List<NearbyEntity> getNearbyHostiles(World world, Vector3d center, double radius, UUID companionId, UUID ownerId) {
        List<NearbyEntity> all = WorldQueries.getNearbyLivingEntities(world, center, radius);
        ArrayList<NearbyEntity> hostiles = new ArrayList<NearbyEntity>();
        if (all.isEmpty()) {
            return hostiles;
        }
        Store store = world.getEntityStore().getStore();
        for (NearbyEntity ne : all) {
            try {
                NPCEntity npcEntity;
                UUID entityId;
                UUIDComponent uuidComp = (UUIDComponent)store.getComponent(ne.ref, UUIDComponent.getComponentType());
                if (uuidComp != null && (entityId = uuidComp.getUuid()) != null && (entityId.equals(companionId) || entityId.equals(ownerId)) || (npcEntity = (NPCEntity)store.getComponent(ne.ref, NPCEntity.getComponentType())) == null) continue;
                hostiles.add(ne);
            }
            catch (Throwable throwable) {}
        }
        return hostiles;
    }

    public static List<NearbyEntity> getNearbyDroppedItems(World world, Vector3d center, double radius) {
        ArrayList<NearbyEntity> result = new ArrayList<NearbyEntity>();
        if (world == null || center == null) {
            return result;
        }
        try {
            Store store = world.getEntityStore().getStore();
            double radiusSq = radius * radius;
            store.forEachChunk((Query)TransformComponent.getComponentType(), (chunk, cmd) -> {
                for (int i = 0; i < chunk.size(); ++i) {
                    double distSq;
                    Vector3d pos;
                    TransformComponent transform;
                    NPCEntity npc;
                    Ref ref = chunk.getReferenceTo(i);
                    if (ref == null || !ref.isValid() || (npc = (NPCEntity)chunk.getComponent(i, NPCEntity.getComponentType())) != null) continue;
                    ItemComponent itemComp = (ItemComponent)store.getComponent(ref, ItemComponent.getComponentType());
                    PickupItemComponent pickupComp = (PickupItemComponent)store.getComponent(ref, PickupItemComponent.getComponentType());
                    if (itemComp == null && pickupComp == null || (transform = (TransformComponent)chunk.getComponent(i, TransformComponent.getComponentType())) == null || (pos = transform.getPosition()) == null || !((distSq = WorldQueries.distanceSquared(center, pos)) <= radiusSq)) continue;
                    result.add(new NearbyEntity((Ref<EntityStore>)ref, pos, Math.sqrt(distSq)));
                }
            });
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return result;
    }

    public static boolean isHarvestableCrop(World world, int x, int y, int z) {
        try {
            String blockName = WorldQueries.getBlockTypeAt(world, x, y, z);
            if (blockName == null) {
                return false;
            }
            String lower = blockName.toLowerCase();
            return lower.contains("crop") || lower.contains("wheat") || lower.contains("corn") || lower.contains("carrot") || lower.contains("potato") || lower.contains("beet") || lower.contains("tomato") || lower.contains("lettuce") || lower.contains("onion") || lower.contains("pumpkin") || lower.contains("melon") || lower.contains("berry") || lower.contains("cotton");
        }
        catch (Throwable t) {
            return false;
        }
    }

    public static List<BlockPos> getNearbyHarvestableCrops(World world, String worldId, Vector3d center, int radius) {
        ArrayList<BlockPos> result = new ArrayList<BlockPos>();
        if (world == null || center == null) {
            return result;
        }
        try {
            int cx = (int)Math.floor(center.x);
            int cy = (int)Math.floor(center.y);
            int cz = (int)Math.floor(center.z);
            int scanLimit = Math.min(radius, 8);
            for (int dx = -scanLimit; dx <= scanLimit; ++dx) {
                for (int dz = -scanLimit; dz <= scanLimit; ++dz) {
                    for (int dy = -2; dy <= 2; ++dy) {
                        if (!WorldQueries.isHarvestableCrop(world, cx + dx, cy + dy, cz + dz)) continue;
                        result.add(new BlockPos(worldId, cx + dx, cy + dy, cz + dz));
                    }
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return result;
    }

    public static boolean harvestCrop(World world, int x, int y, int z) {
        if (world == null) {
            return false;
        }
        try {
            String before = WorldQueries.getBlockTypeAt(world, x, y, z);
            try {
                Class<?> harvestUtils = Class.forName("com.hypixel.hytale.server.core.modules.block.BlockHarvestUtils");
                Method[] methodArray = harvestUtils.getMethods();
                int n = methodArray.length;
                for (int i = 0; i < n; ++i) {
                    Method method = methodArray[i];
                    if (!"performBlockBreak".equals(method.getName()) || !WorldQueries.invokeBreakLike(method, null, world, x, y, z) || !WorldQueries.didBlockChange(world, x, y, z, before)) continue;
                    return true;
                }
            }
            catch (ClassNotFoundException classNotFoundException) {
                // empty catch block
            }
            for (Method method : world.getClass().getMethods()) {
                String name = method.getName();
                if (!"breakBlock".equals(name) && !"destroyBlock".equals(name) && !"removeBlock".equals(name) || !WorldQueries.invokeBreakLike(method, world, world, x, y, z) || !WorldQueries.didBlockChange(world, x, y, z, before)) continue;
                return true;
            }
            return WorldQueries.trySetBlockToEmpty(world, x, y, z) && WorldQueries.didBlockChange(world, x, y, z, before);
        }
        catch (Throwable t) {
            return false;
        }
    }

    public static boolean breakBlock(World world, int x, int y, int z) {
        return WorldQueries.harvestCrop(world, x, y, z);
    }

    private static boolean invokeBreakLike(Method method, Object target, World world, int x, int y, int z) {
        return WorldQueries.invokeBreakLike(method, target, world, x, y, z, true) || WorldQueries.invokeBreakLike(method, target, world, x, y, z, false);
    }

    private static boolean invokeBreakLike(Method method, Object target, World world, int x, int y, int z, boolean boolValue) {
        try {
            Class<?>[] params = method.getParameterTypes();
            Object[] args = new Object[params.length];
            int intIndex = 0;
            for (int i = 0; i < params.length; ++i) {
                Class<?> p = params[i];
                if (World.class.isAssignableFrom(p)) {
                    args[i] = world;
                    continue;
                }
                if (p == Integer.TYPE || p == Integer.class) {
                    args[i] = intIndex == 0 ? x : (intIndex == 1 ? y : z);
                    ++intIndex;
                    continue;
                }
                if (p == Boolean.TYPE || p == Boolean.class) {
                    args[i] = boolValue;
                    continue;
                }
                return false;
            }
            if (intIndex < 3) {
                return false;
            }
            method.invoke(target, args);
            return true;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean trySetBlockToEmpty(World world, int x, int y, int z) {
        String[] emptyIds = new String[]{"empty", "air", "Air"};
        for (Method method : world.getClass().getMethods()) {
            Class<?>[] params;
            String name = method.getName();
            if (!"setBlock".equals(name) && !"setBlockType".equals(name) || (params = method.getParameterTypes()).length < 4 || params[0] != Integer.TYPE && params[0] != Integer.class || params[1] != Integer.TYPE && params[1] != Integer.class || params[2] != Integer.TYPE && params[2] != Integer.class || params[3] != String.class) continue;
            for (String emptyId : emptyIds) {
                try {
                    method.invoke((Object)world, x, y, z, emptyId);
                    return true;
                }
                catch (Throwable throwable) {
                }
            }
        }
        return false;
    }

    private static boolean didBlockChange(World world, int x, int y, int z, String beforeRaw) {
        String after;
        String before = beforeRaw == null ? "" : beforeRaw.trim().toLowerCase();
        String afterRaw = WorldQueries.getBlockTypeAt(world, x, y, z);
        String string = after = afterRaw == null ? "" : afterRaw.trim().toLowerCase();
        if (before.isBlank() && after.isBlank()) {
            return false;
        }
        return !before.equals(after);
    }

    public static String getBlockTypeAt(World world, int x, int y, int z) {
        try {
            String worldType = WorldQueries.reflectWorldBlockType(world, x, y, z);
            if (worldType != null && !worldType.isBlank() && !"null".equalsIgnoreCase(worldType)) {
                return worldType;
            }
            long chunkIndex = ChunkUtil.indexChunkFromBlock((int)x, (int)z);
            WorldChunk chunk = world.getChunk(chunkIndex);
            if (chunk == null) {
                return null;
            }
            Object state = null;
            state = WorldQueries.getChunkStateReflectively(chunk, x, y, z);
            if (state == null) {
                int localX = x & 0x1F;
                int localZ = z & 0x1F;
                state = WorldQueries.getChunkStateReflectively(chunk, localX, y, localZ);
            }
            if (state == null) {
                return null;
            }
            String reflected = WorldQueries.reflectBlockIdentity(state);
            if (reflected != null && !reflected.isBlank() && !"null".equalsIgnoreCase(reflected)) {
                return reflected;
            }
            String str = state.toString();
            if (str != null && !str.isBlank() && !"null".equalsIgnoreCase(str)) {
                return str;
            }
            return null;
        }
        catch (Throwable t) {
            return null;
        }
    }

    private static String reflectWorldBlockType(World world, int x, int y, int z) {
        if (world == null) {
            return null;
        }
        try {
            String s;
            Method m2;
            try {
                m2 = world.getClass().getMethod("getBlockType", Integer.TYPE, Integer.TYPE, Integer.TYPE);
                Object blockType = m2.invoke((Object)world, x, y, z);
                if (blockType != null && (s = blockType.toString()) != null && !s.isBlank()) {
                    return s;
                }
            }
            catch (NoSuchMethodException m2) {
                // empty catch block
            }
            try {
                m2 = world.getClass().getMethod("getBlock", Integer.TYPE, Integer.TYPE, Integer.TYPE);
                Object blockId = m2.invoke((Object)world, x, y, z);
                if (blockId != null && (s = blockId.toString()) != null && !s.isBlank()) {
                    return "BlockId:" + s;
                }
            }
            catch (NoSuchMethodException noSuchMethodException) {}
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    private static String reflectBlockIdentity(Object state) {
        if (state == null) {
            return null;
        }
        try {
            try {
                String s;
                Method method = state.getClass().getMethod("getBlockType", new Class[0]);
                Object blockType = method.invoke(state, new Object[0]);
                if (blockType != null && (s = blockType.toString()) != null && !s.isBlank()) {
                    return s;
                }
            }
            catch (NoSuchMethodException noSuchMethodException) {
                // empty catch block
            }
            for (String methodName : new String[]{"getType", "getBlockId", "getId", "getName"}) {
                try {
                    String s;
                    Method m = state.getClass().getMethod(methodName, new Class[0]);
                    Object v = m.invoke(state, new Object[0]);
                    if (v == null || (s = v.toString()) == null || s.isBlank()) continue;
                    return s;
                }
                catch (NoSuchMethodException noSuchMethodException) {
                    // empty catch block
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    public static List<String> debugScanBlocks(World world, int cx, int cy, int cz, int radius) {
        ArrayList<String> results = new ArrayList<String>();
        try {
            int scanLimit = Math.min(radius, 5);
            for (int dx = -scanLimit; dx <= scanLimit; ++dx) {
                for (int dz = -scanLimit; dz <= scanLimit; ++dz) {
                    for (int dy = -2; dy <= 2; ++dy) {
                        String str;
                        Object state;
                        int bx = cx + dx;
                        int by = cy + dy;
                        int bz = cz + dz;
                        long chunkIndex = ChunkUtil.indexChunkFromBlock((int)bx, (int)bz);
                        WorldChunk chunk = world.getChunk(chunkIndex);
                        if (chunk == null || (state = WorldQueries.getChunkStateReflectively(chunk, bx, by, bz)) == null || (str = state.toString()) == null || str.isBlank() || str.toLowerCase().contains("air")) continue;
                        StringBuilder info = new StringBuilder();
                        info.append("(").append(bx).append(",").append(by).append(",").append(bz).append(") ");
                        info.append("class=").append(state.getClass().getName());
                        info.append(" toString=").append(str);
                        try {
                            Method[] methods = state.getClass().getMethods();
                            StringBuilder methodNames = new StringBuilder();
                            for (Method m : methods) {
                                if (m.getDeclaringClass() == Object.class) continue;
                                if (methodNames.length() > 0) {
                                    methodNames.append(",");
                                }
                                methodNames.append(m.getName());
                            }
                            info.append(" methods=[").append((CharSequence)methodNames).append("]");
                        }
                        catch (Throwable throwable) {
                            // empty catch block
                        }
                        results.add(info.toString());
                        if (results.size() < 20) continue;
                        return results;
                    }
                }
            }
        }
        catch (Throwable t) {
            results.add("Error: " + t.getMessage());
        }
        return results;
    }

    private static Object getChunkStateReflectively(WorldChunk chunk, int x, int y, int z) {
        if (chunk == null) {
            return null;
        }
        try {
            return WorldQueries.invokeMethod(chunk, "getState", x, y, z);
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeMethod(Object target, String methodName, Object ... args) throws Exception {
        if (target == null || methodName == null) {
            return null;
        }
        for (Method method : target.getClass().getMethods()) {
            Class<?>[] parameterTypes;
            if (!methodName.equals(method.getName()) || (parameterTypes = method.getParameterTypes()).length != args.length || !WorldQueries.parametersAccept(parameterTypes, args)) continue;
            return method.invoke(target, args);
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + methodName);
    }

    private static boolean parametersAccept(Class<?>[] parameterTypes, Object[] args) {
        for (int i = 0; i < parameterTypes.length; ++i) {
            Class<?> parameterType;
            Object arg = args[i];
            Class<?> rawType = parameterTypes[i];
            if (!(arg == null ? rawType.isPrimitive() : !(parameterType = WorldQueries.wrapPrimitive(rawType)).isAssignableFrom(arg.getClass()))) continue;
            return false;
        }
        return true;
    }

    private static Class<?> wrapPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == Integer.TYPE) {
            return Integer.class;
        }
        if (type == Long.TYPE) {
            return Long.class;
        }
        if (type == Boolean.TYPE) {
            return Boolean.class;
        }
        if (type == Double.TYPE) {
            return Double.class;
        }
        if (type == Float.TYPE) {
            return Float.class;
        }
        if (type == Short.TYPE) {
            return Short.class;
        }
        if (type == Byte.TYPE) {
            return Byte.class;
        }
        if (type == Character.TYPE) {
            return Character.class;
        }
        return type;
    }

    private static double distanceSquared(Vector3d a, Vector3d b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public static final class NearbyEntity {
        public final Ref<EntityStore> ref;
        public final Vector3d position;
        public final double distance;

        public NearbyEntity(Ref<EntityStore> ref, Vector3d position, double distance) {
            this.ref = ref;
            this.position = position;
            this.distance = distance;
        }
    }
}

