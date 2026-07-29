/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.logger.HytaleLogger
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 *  com.hypixel.hytale.math.util.ChunkUtil
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.math.vector.Vector3i
 *  com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerBlockWindow
 *  com.hypixel.hytale.server.core.entity.entities.player.windows.ItemContainerWindow
 *  com.hypixel.hytale.server.core.entity.entities.player.windows.Window
 *  com.hypixel.hytale.server.core.inventory.ItemStack
 *  com.hypixel.hytale.server.core.inventory.container.ItemContainer
 *  com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  com.hypixel.hytale.server.npc.entities.NPCEntity
 *  com.hypixel.hytale.server.npc.movement.controllers.MotionController
 */
package com.yourname.companion.systems;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerBlockWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ItemContainerWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.movement.controllers.MotionController;
import com.yourname.companion.data.BlockPos;
import com.yourname.companion.data.CompanionRecord;
import com.yourname.companion.data.EquipmentSlot;
import com.yourname.companion.data.PlayerCompanionData;
import com.yourname.companion.runtime.CompanionRuntimeState;
import com.yourname.companion.systems.WorkPositioning;
import com.yourname.companion.util.WorldQueries;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

public final class DepositSystem {
    private static final int DEPOSIT_COOLDOWN = 200;
    private static final int DEPOSIT_BLOCKED_CHEST_TICKS = 100;
    private static final double DEPOSIT_SEEK_RADIUS = 10.0;
    private static final double INTERACT_RANGE = 3.0;
    private static final double MOVE_SPEED = 4.0;
    private static final boolean DEBUG_DEPOSIT_DIAG = false;
    private static final String[] CONTAINER_STATE_CLASS_NAMES = new String[]{"com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock", "com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerBlock", "com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState", "com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerBlockState"};
    private static final String[] KNOWN_CONTAINER_BLOCK_IDS = new String[]{"bench_alchemy", "bench_arcane", "bench_armory", "bench_armour", "bench_builders", "bench_campfire", "bench_cooking", "bench_farming", "bench_furnace", "bench_furniture", "bench_loom", "bench_lumbermill", "bench_memories", "bench_salvage", "bench_tannery", "bench_trough", "bench_weapon", "bench_workbench", "furniture_ancient_chest_large", "furniture_ancient_chest_small", "furniture_christmas_chest_small", "furniture_christmas_chest_small_green", "furniture_christmas_chest_small_red", "furniture_christmas_chest_small_reddotted", "furniture_christmas_chest_small_white", "furniture_crude_chest_large", "furniture_crude_chest_small", "furniture_desert_chest_large", "furniture_desert_chest_small", "furniture_dungeon_chest_epic", "furniture_dungeon_chest_epic_large", "furniture_dungeon_chest_legendary_large", "furniture_feran_chest_large", "furniture_feran_chest_small", "furniture_frozen_castle_chest_large", "furniture_frozen_castle_chest_small", "furniture_goblin_chest_small", "furniture_human_ruins_chest_large", "furniture_human_ruins_chest_small", "furniture_jungle_chest_large", "furniture_jungle_chest_small", "furniture_kweebec_chest_large", "furniture_kweebec_chest_small", "furniture_lumberjack_chest_large", "furniture_lumberjack_chest_small", "furniture_royal_magic_chest_large", "furniture_royal_magic_chest_small", "furniture_scarak_hive_chest_large", "furniture_scarak_hive_chest_small", "furniture_tavern_chest_large", "furniture_tavern_chest_small", "furniture_temple_dark_chest_large", "furniture_temple_dark_chest_small", "furniture_temple_emerald_chest_large", "furniture_temple_emerald_chest_small", "furniture_temple_light_chest_large", "furniture_temple_light_chest_small", "furniture_temple_scarak_chest_large", "furniture_temple_scarak_chest_small", "furniture_temple_wind_chest_large", "furniture_temple_wind_chest_small", "furniture_village_chest_large", "furniture_village_chest_small"};
    private final HytaleLogger logger;

    public DepositSystem(HytaleLogger logger) {
        this.logger = logger;
    }

    public boolean evaluateDeposit(World world, Store<EntityStore> store, Ref<EntityStore> companionRef, NPCEntity npcEntity, PlayerCompanionData data, CompanionRecord companion, CompanionRuntimeState runtime, Vector3d companionPos, long currentTick) {
        List<BlockPos> linkedChests;
        if (runtime.depositBlockedChestKey != null && currentTick >= runtime.depositBlockedUntilTick) {
            runtime.depositBlockedChestKey = null;
            runtime.depositBlockedUntilTick = 0L;
        }
        if ((linkedChests = this.getEffectiveLinkedChests(companion, data)).isEmpty()) {
            return false;
        }
        if (runtime.depositRequested) {
            return true;
        }
        if (!data.depositEnabled) {
            return false;
        }
        if (currentTick - runtime.lastDepositTick < 200L) {
            return false;
        }
        if (!this.hasDepositableItems(npcEntity, runtime, companion)) {
            return false;
        }
        for (BlockPos chestPos : linkedChests) {
            Vector3d chestVec = new Vector3d((double)chestPos.x + 0.5, (double)chestPos.y + 0.5, (double)chestPos.z + 0.5);
            if (!(companionPos.distanceTo(chestVec) <= 10.0)) continue;
            return true;
        }
        return false;
    }

    public void executeDepositSeek(World world, Store<EntityStore> store, NPCEntity npcEntity, Ref<EntityStore> companionRef, PlayerCompanionData data, CompanionRecord companion, CompanionRuntimeState runtime, Vector3d companionPos, long currentTick) {
        BlockPos bestChest;
        List<BlockPos> linkedChests = this.getEffectiveLinkedChests(companion, data);
        if (runtime.depositBlockedChestKey != null && currentTick >= runtime.depositBlockedUntilTick) {
            runtime.depositBlockedChestKey = null;
            runtime.depositBlockedUntilTick = 0L;
        }
        if ((bestChest = this.findBestChest(linkedChests, companionPos, runtime, currentTick)) == null) {
            runtime.depositRequested = false;
            return;
        }
        Vector3d chestVec = new Vector3d((double)bestChest.x + 0.5, (double)bestChest.y + 0.5, (double)bestChest.z + 0.5);
        Vector3d workPos = this.resolveDepositWorkPosition(world, companionPos, bestChest);
        double distance = companionPos.distanceTo(chestVec);
        if (distance <= 3.0) {
            this.executeDepositCycle(world, store, npcEntity, data, companion, runtime, linkedChests, bestChest, companionPos, currentTick);
            return;
        }
        try {
            if (!WorkPositioning.setManagedWorkPosition(npcEntity, workPos, this.logger)) {
                double dx = workPos.x - companionPos.x;
                double dz = workPos.z - companionPos.z;
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.01) {
                    Vector3d velocity = new Vector3d(dx / len * 4.0, 0.0, dz / len * 4.0);
                    MotionController mc = npcEntity.getRole().getActiveMotionController();
                    if (mc != null) {
                        mc.forceVelocity(velocity, null, false);
                    }
                }
                npcEntity.getRole().getWorldSupport().requestNewPath();
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to path companion to chest.");
        }
    }

    private Vector3d resolveDepositWorkPosition(World world, Vector3d companionPos, BlockPos chestPos) {
        if (chestPos == null) {
            return companionPos != null ? companionPos : Vector3d.ZERO;
        }
        int[][] offsets = new int[][]{{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        Vector3d best = null;
        double bestDist = Double.MAX_VALUE;
        for (int[] offset : offsets) {
            double dist;
            int standX = chestPos.x + offset[0];
            int standZ = chestPos.z + offset[1];
            if (!this.isDepositStandPosition(world, standX, chestPos.y, standZ)) continue;
            Vector3d candidate = new Vector3d((double)standX + 0.5, (double)chestPos.y, (double)standZ + 0.5);
            double d = dist = companionPos != null ? companionPos.distanceTo(candidate) : 0.0;
            if (!(dist < bestDist)) continue;
            best = candidate;
            bestDist = dist;
        }
        if (best != null) {
            return best;
        }
        return new Vector3d((double)chestPos.x + 0.5, (double)chestPos.y, (double)chestPos.z + 0.5);
    }

    private boolean isDepositStandPosition(World world, int standX, int feetY, int standZ) {
        if (world == null) {
            return false;
        }
        String feetBlock = this.normalizeBlockId(WorldQueries.getBlockTypeAt(world, standX, feetY, standZ));
        String headBlock = this.normalizeBlockId(WorldQueries.getBlockTypeAt(world, standX, feetY + 1, standZ));
        String floorBlock = this.normalizeBlockId(WorldQueries.getBlockTypeAt(world, standX, feetY - 1, standZ));
        return this.isDepositAirLike(feetBlock) && this.isDepositAirLike(headBlock) && !this.isDepositAirLike(floorBlock);
    }

    private boolean isDepositAirLike(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return true;
        }
        String low = blockId.toLowerCase();
        return low.equals("air") || low.equals("empty") || low.contains("id=empty") || low.contains("group='air'") || low.contains("drawtype=empty") || low.contains("material=empty");
    }

    private String normalizeBlockId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String lower = raw.toLowerCase().trim();
        int idIdx = lower.indexOf("id=");
        if (idIdx >= 0) {
            int start = idIdx + 3;
            int endComma = lower.indexOf(44, start);
            int endBrace = lower.indexOf(125, start);
            int end = -1;
            if (endComma >= 0 && endBrace >= 0) {
                end = Math.min(endComma, endBrace);
            } else if (endComma >= 0) {
                end = endComma;
            } else if (endBrace >= 0) {
                end = endBrace;
            }
            if (end > start) {
                return lower.substring(start, end).trim();
            }
        }
        return lower;
    }

    private void executeDepositCycle(World world, Store<EntityStore> store, NPCEntity npcEntity, PlayerCompanionData data, CompanionRecord companion, CompanionRuntimeState runtime, List<BlockPos> linkedChests, BlockPos activeChest, Vector3d companionPos, long currentTick) {
        boolean manualDeposit = runtime != null && runtime.depositRequested;
        int maxItems = manualDeposit ? Integer.MAX_VALUE : Math.max(1, data.moveItemsCap);
        int maxChests = Math.max(1, data.chestScanLimit);
        int movedItems = 0;
        int scanned = 0;
        int writable = 0;
        try {
            if (!this.hasDepositableItems(npcEntity, runtime, companion)) {
                runtime.depositRequested = false;
                runtime.lastDepositTick = currentTick;
                return;
            }
            List<BlockPos> chests = this.orderedLinkedChests(linkedChests, activeChest, companionPos, runtime, currentTick);
            for (BlockPos chestPos : chests) {
                if (scanned < maxChests) {
                    int movedHere;
                    ++scanned;
                    ItemContainer chestContainer = this.getChestContainer(world, store, chestPos, data);
                    if (chestContainer == null) {
                        this.logDepositContainerMiss(world, chestPos);
                        continue;
                    }
                    ++writable;
                    if ((movedItems += (movedHere = this.moveDepositableItemsIntoChest(npcEntity, chestContainer, maxItems - movedItems, runtime, companion))) < maxItems && this.hasDepositableItems(npcEntity, runtime, companion)) continue;
                }
                break;
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Deposit cycle failed.");
        }
        boolean stillHasDepositable = this.hasDepositableItems(npcEntity, runtime, companion);
        if (runtime.mineDepositOnlyMined && !this.hasDepositableItems(npcEntity, runtime, companion)) {
            runtime.mineDepositOnlyMined = false;
            runtime.mineReturnToLoadedArea = false;
            runtime.mineCollectedItemIds.clear();
        }
        if (movedItems == 0 && stillHasDepositable && activeChest != null) {
            runtime.depositBlockedChestKey = this.blockPosKey(activeChest);
            runtime.depositBlockedUntilTick = currentTick + 100L;
            runtime.depositRequested = true;
            runtime.lastDepositTick = 0L;
            return;
        }
        runtime.depositRequested = false;
        runtime.lastDepositTick = currentTick;
    }

    private BlockPos findBestChest(List<BlockPos> linkedChests, Vector3d companionPos, CompanionRuntimeState runtime, long currentTick) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : linkedChests) {
            Vector3d chestVec;
            double dist;
            if (this.isBlockedChest(runtime, currentTick, pos) || !((dist = companionPos.distanceTo(chestVec = new Vector3d((double)pos.x + 0.5, (double)pos.y + 0.5, (double)pos.z + 0.5))) < bestDist)) continue;
            bestDist = dist;
            best = pos;
        }
        return best;
    }

    private List<BlockPos> orderedLinkedChests(List<BlockPos> linkedChests, BlockPos activeChest, Vector3d companionPos, CompanionRuntimeState runtime, long currentTick) {
        ArrayList<BlockPos> ordered = new ArrayList<BlockPos>();
        if (linkedChests == null) {
            return ordered;
        }
        if (activeChest != null && !this.isBlockedChest(runtime, currentTick, activeChest)) {
            ordered.add(activeChest);
        }
        for (BlockPos pos2 : linkedChests) {
            if (pos2 == null || activeChest != null && this.sameBlockPos(activeChest, pos2) || this.isBlockedChest(runtime, currentTick, pos2)) continue;
            ordered.add(pos2);
        }
        ordered.sort(Comparator.comparingDouble(pos -> companionPos.distanceTo(new Vector3d((double)pos.x + 0.5, (double)pos.y + 0.5, (double)pos.z + 0.5))));
        return ordered;
    }

    private ItemContainer getChestContainer(World world, Store<EntityStore> store, BlockPos chestPos, PlayerCompanionData data) {
        if (world == null || chestPos == null) {
            return null;
        }
        ItemContainer direct = this.getChestContainerDirect(world, store, chestPos, data);
        if (direct != null) {
            return direct;
        }
        List<BlockPos> nearbyCandidates = this.getNearbyChestCandidates(world, chestPos);
        for (BlockPos nearby : nearbyCandidates) {
            ItemContainer remapped;
            if (this.sameBlockPos(chestPos, nearby) || (remapped = this.getChestContainerDirect(world, store, nearby, data)) == null) continue;
            return remapped;
        }
        return null;
    }

    private ItemContainer getChestContainerDirect(World world, Store<EntityStore> store, BlockPos chestPos, PlayerCompanionData data) {
        if (world == null || chestPos == null) {
            return null;
        }
        try {
            try {
                Object resolvedState = this.invokeMethod(world, "getState", chestPos.x, chestPos.y, chestPos.z, true);
                ItemContainer resolvedContainer = this.getContainerFromResolvedState(chestPos, resolvedState, "world.getState(...,true)");
                if (resolvedContainer != null) {
                    return resolvedContainer;
                }
            }
            catch (Throwable resolvedState) {
                // empty catch block
            }
            long chunkIndex = ChunkUtil.indexChunkFromBlock((int)chestPos.x, (int)chestPos.z);
            WorldChunk chunk = world.getChunk(chunkIndex);
            if (chunk == null) {
                return null;
            }
            ItemContainer repairedStateContainer = this.getOrCreateContainerStateContainer(world, chunk, chestPos);
            if (repairedStateContainer != null) {
                return repairedStateContainer;
            }
            ItemContainer openWindowContainer = this.findOpenWindowContainer(world, chestPos);
            if (openWindowContainer != null) {
                return openWindowContainer;
            }
            ItemContainer spatialContainer = this.getSpatialContainer(world, chestPos);
            if (spatialContainer != null) {
                return spatialContainer;
            }
            ItemContainer blockComponentContainer = this.getBlockComponentContainer(world, chunk, chestPos);
            if (blockComponentContainer != null) {
                return blockComponentContainer;
            }
            Object state = this.getChunkStateReflectively(chunk, chestPos.x, chestPos.y, chestPos.z);
            if (state == null) {
                int localX = chestPos.x & 0x1F;
                int localZ = chestPos.z & 0x1F;
                state = this.getChunkStateReflectively(chunk, localX, chestPos.y, localZ);
            }
            if (state == null) {
                return null;
            }
            ItemContainer directStateContainer = this.getContainerFromResolvedState(chestPos, state, "chunk.getState");
            if (directStateContainer != null) {
                return directStateContainer;
            }
            ItemContainer reflected = this.tryResolveContainerReflectively(state);
            if (reflected != null) {
                return reflected;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    private ItemContainer getOrCreateContainerStateContainer(World world, WorldChunk chunk, BlockPos chestPos) {
        if (world == null || chunk == null || chestPos == null) {
            return null;
        }
        try {
            Class<?> blockStateClass = Class.forName("com.hypixel.hytale.server.core.universe.world.meta.BlockState");
            Object ensuredState = this.invokeStaticMethod(blockStateClass, "ensureState", chunk, chestPos.x, chestPos.y, chestPos.z);
            ItemContainer ensuredContainer = this.getContainerFromResolvedState(chestPos, ensuredState, "BlockState.ensureState");
            if (ensuredContainer != null) {
                return ensuredContainer;
            }
        }
        catch (Throwable blockStateClass) {
            // empty catch block
        }
        try {
            BlockType blockType = world.getBlockType(chestPos.x, chestPos.y, chestPos.z);
            if (!this.shouldRepairMissingContainerState(blockType)) {
                return null;
            }
            Object repairedState = this.createContainerStateReflectively(chunk, chestPos, blockType);
            if (repairedState == null) {
                return null;
            }
            ItemContainer repairedContainer = this.tryResolveContainerReflectively(repairedState);
            if (repairedContainer == null) {
                return null;
            }
            if (!this.setChunkStateReflectively(chunk, chestPos, repairedState)) {
                return null;
            }
            return repairedContainer;
        }
        catch (Throwable t) {
            return null;
        }
    }

    private Object createContainerStateReflectively(WorldChunk chunk, BlockPos chestPos, BlockType blockType) {
        for (String className : CONTAINER_STATE_CLASS_NAMES) {
            try {
                Class<?> stateClass = Class.forName(className);
                Object state = stateClass.getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
                try {
                    this.invokeMethod(state, "setPosition", chunk, new Vector3i(chestPos.x, chestPos.y, chestPos.z));
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                Object initialized = null;
                try {
                    initialized = this.invokeMethod(state, "initialize", blockType);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                if (initialized instanceof Boolean && !((Boolean)initialized).booleanValue() || this.tryResolveContainerReflectively(state) == null) continue;
                return state;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return null;
    }

    private boolean shouldRepairMissingContainerState(BlockType blockType) {
        if (blockType == null || blockType.isUnknown()) {
            return false;
        }
        try {
            if (blockType.getInteractions() != null) {
                for (String interaction : blockType.getInteractions().values()) {
                    if (!"Open_Container".equalsIgnoreCase(String.valueOf(interaction))) continue;
                    return true;
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return this.looksLikeContainerBlock(String.valueOf(blockType));
    }

    private ItemContainer getContainerFromResolvedState(BlockPos chestPos, Object resolvedState, String source) {
        if (resolvedState == null) {
            return null;
        }
        ItemContainer reflected = this.tryResolveContainerReflectively(resolvedState);
        if (reflected != null) {
            return reflected;
        }
        return null;
    }

    private ItemContainer getSpatialContainer(World world, BlockPos chestPos) {
        return null;
    }

    private List<BlockPos> getNearbyChestCandidates(World world, BlockPos origin) {
        int[][] offsets;
        ArrayList<BlockPos> out = new ArrayList<BlockPos>();
        if (world == null || origin == null) {
            return out;
        }
        String originBlockId = WorldQueries.getBlockTypeAt(world, origin.x, origin.y, origin.z);
        String originCanonicalId = this.canonicalContainerId(originBlockId);
        for (int[] offset : offsets = new int[][]{{0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {2, 0, 0}, {-2, 0, 0}, {0, 0, 2}, {0, 0, -2}, {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1}, {2, 0, 1}, {2, 0, -1}, {-2, 0, 1}, {-2, 0, -1}, {1, 0, 2}, {-1, 0, 2}, {1, 0, -2}, {-1, 0, -2}, {0, 1, 0}, {0, -1, 0}, {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1}, {1, -1, 0}, {-1, -1, 0}, {0, -1, 1}, {0, -1, -1}}) {
            int x = origin.x + offset[0];
            int y = origin.y + offset[1];
            int z = origin.z + offset[2];
            String blockId = WorldQueries.getBlockTypeAt(world, x, y, z);
            String candidateCanonicalId = this.canonicalContainerId(blockId);
            if (!this.looksLikeContainerBlock(blockId) && !this.matchesCanonicalContainer(originCanonicalId, candidateCanonicalId) && !this.isLikelyLargeChestPartner(originBlockId, blockId, offset)) continue;
            this.addBlockPos(out, new BlockPos(origin.worldId, x, y, z));
        }
        return out;
    }

    private boolean looksLikeContainerBlock(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return false;
        }
        String low = blockId.toLowerCase();
        String canonical = this.canonicalContainerId(low);
        for (String id : KNOWN_CONTAINER_BLOCK_IDS) {
            if (!canonical.contains(id)) continue;
            return true;
        }
        return low.contains("chest") || low.contains("container") || low.contains("box") || low.contains("quickstacker");
    }

    private String canonicalContainerId(String raw) {
        String normalized = this.normalizeBlockId(raw);
        if (normalized.isBlank()) {
            return "";
        }
        String out = normalized;
        if (out.startsWith("*")) {
            out = out.substring(1);
        }
        out = out.replace("_state_definitions_openwindow", "");
        out = out.replace("_state_definitions_closewindow", "");
        out = out.replace("_definitions_openwindow", "");
        out = out.replace("_definitions_closewindow", "");
        return out;
    }

    private String toOpenStateId(String raw) {
        String normalized = this.normalizeBlockId(raw);
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.replace("_state_definitions_closewindow", "_state_definitions_openwindow").replace("_definitions_closewindow", "_definitions_openwindow");
    }

    private boolean matchesCanonicalContainer(String left, String right) {
        return !left.isBlank() && left.equals(right);
    }

    private boolean isLikelyLargeChestPartner(String originBlockId, String candidateBlockId, int[] offset) {
        String candidateLow;
        String originLow = originBlockId == null ? "" : originBlockId.toLowerCase();
        String string = candidateLow = candidateBlockId == null ? "" : candidateBlockId.toLowerCase();
        if (!originLow.contains("chest_large") && !originLow.contains("hitboxtype='chest_large'")) {
            return false;
        }
        if (!(candidateLow.contains("chest") || candidateLow.contains("container") || candidateLow.contains("box"))) {
            return false;
        }
        return offset[1] == 0 && Math.abs(offset[0]) + Math.abs(offset[2]) <= 2;
    }

    private void addBlockPos(List<BlockPos> out, BlockPos candidate) {
        if (out == null || candidate == null) {
            return;
        }
        for (BlockPos existing : out) {
            if (!this.sameBlockPos(existing, candidate)) continue;
            return;
        }
        out.add(candidate);
    }

    private ItemContainer findOpenWindowContainer(World world, BlockPos chestPos) {
        if (world == null || chestPos == null) {
            return null;
        }
        try {
            for (Player player : world.getPlayers()) {
                if (player == null || player.getWindowManager() == null) continue;
                for (Window window : player.getWindowManager().getWindows()) {
                    ItemContainerWindow itemWindow;
                    ContainerBlockWindow blockWindow;
                    if (!(window instanceof ContainerBlockWindow) || (blockWindow = (ContainerBlockWindow)window).getX() != chestPos.x || blockWindow.getY() != chestPos.y || blockWindow.getZ() != chestPos.z || !(window instanceof ItemContainerWindow) || (itemWindow = (ItemContainerWindow)window).getItemContainer() == null) continue;
                    return itemWindow.getItemContainer();
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    private ItemContainer getBlockComponentContainer(World world, WorldChunk chunk, BlockPos chestPos) {
        if (world == null || chestPos == null) {
            return null;
        }
        ItemContainer viaBlockModule = this.getBlockModuleContainerComponent(world, chestPos);
        if (viaBlockModule != null) {
            return viaBlockModule;
        }
        if (chunk == null) {
            return null;
        }
        try {
            ItemContainer refContainer;
            ItemContainer holderContainer;
            Class<?> containerBlockClass = Class.forName("com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock");
            Object componentType = this.invokeStaticMethod(containerBlockClass, "getComponentType", new Object[0]);
            if (componentType == null) {
                return null;
            }
            int localX = chestPos.x & 0x1F;
            int localZ = chestPos.z & 0x1F;
            Object holder = null;
            try {
                holder = this.invokeMethod(chunk, "getBlockComponentHolder", chestPos.x, chestPos.y, chestPos.z);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            if (holder == null) {
                try {
                    holder = this.invokeMethod(chunk, "getBlockComponentHolder", localX, chestPos.y, localZ);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
            if ((holderContainer = this.getComponentContainerFromHolder(holder, componentType, chestPos, "holder")) != null) {
                return holderContainer;
            }
            Object ref = null;
            try {
                ref = this.invokeMethod(chunk, "getBlockComponentEntity", chestPos.x, chestPos.y, chestPos.z);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            if (!this.isValidRef(ref)) {
                try {
                    ref = this.invokeMethod(chunk, "getBlockComponentEntity", localX, chestPos.y, localZ);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
            if ((refContainer = this.getComponentContainerFromRef(ref, componentType, chestPos, "ref")) != null) {
                return refContainer;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    private ItemContainer getBlockModuleContainerComponent(World world, BlockPos chestPos) {
        try {
            Class<?> containerBlockClass = Class.forName("com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock");
            Object componentType = this.invokeStaticMethod(containerBlockClass, "getComponentType", new Object[0]);
            if (componentType == null) {
                return null;
            }
            Class<?> blockModuleClass = Class.forName("com.hypixel.hytale.server.core.modules.block.BlockModule");
            Object component = this.invokeStaticMethod(blockModuleClass, "getComponent", componentType, world, chestPos.x, chestPos.y, chestPos.z);
            return this.tryResolveContainerReflectively(component);
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private ItemContainer getComponentContainerFromHolder(Object holder, Object componentType, BlockPos chestPos, String source) {
        if (holder == null || componentType == null) {
            return null;
        }
        try {
            Object component = this.invokeMethod(holder, "getComponent", componentType);
            ItemContainer container = this.tryResolveContainerReflectively(component);
            if (container != null) {
                return container;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    private ItemContainer getComponentContainerFromRef(Object ref, Object componentType, BlockPos chestPos, String source) {
        if (!this.isValidRef(ref) || componentType == null) {
            return null;
        }
        try {
            Object store = this.invokeMethod(ref, "getStore", new Object[0]);
            if (store == null) {
                return null;
            }
            Object component = this.invokeMethod(store, "getComponent", ref, componentType);
            ItemContainer container = this.tryResolveContainerReflectively(component);
            if (container != null) {
                return container;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    private ItemContainer tryResolveContainerReflectively(Object state) {
        String[] directMethods;
        if (state == null) {
            return null;
        }
        for (String methodName : directMethods = new String[]{"getItemContainer", "getContainer", "getStorage", "getInventory", "getPrimaryContainer", "getInputContainer", "getOutputContainer"}) {
            try {
                Method m = state.getClass().getMethod(methodName, new Class[0]);
                Object c = m.invoke(state, new Object[0]);
                ItemContainer resolved = this.unwrapItemContainerCandidate(c);
                if (resolved == null) continue;
                return resolved;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return null;
    }

    private Object getChunkStateReflectively(WorldChunk chunk, int x, int y, int z) {
        if (chunk == null) {
            return null;
        }
        try {
            return this.invokeMethod(chunk, "getState", x, y, z);
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private boolean setChunkStateReflectively(WorldChunk chunk, BlockPos chestPos, Object state) {
        if (chunk == null || chestPos == null || state == null) {
            return false;
        }
        try {
            Object result = this.invokeMethod(chunk, "setState", chestPos.x, chestPos.y, chestPos.z, state, true);
            return !(result instanceof Boolean) || (Boolean)result != false;
        }
        catch (Throwable ignored) {
            try {
                Object result = this.invokeMethod(chunk, "setState", chestPos.x, chestPos.y, chestPos.z, state);
                return !(result instanceof Boolean) || (Boolean)result != false;
            }
            catch (Throwable ignoredAgain) {
                return false;
            }
        }
    }

    private boolean isValidRef(Object ref) {
        if (ref == null) {
            return false;
        }
        try {
            Object valid = this.invokeMethod(ref, "isValid", new Object[0]);
            return valid instanceof Boolean && (Boolean)valid != false;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private Object invokeMethod(Object target, String methodName, Object ... args) throws Exception {
        if (target == null || methodName == null) {
            return null;
        }
        return this.invokeMethod(target.getClass(), target, methodName, args);
    }

    private Object invokeStaticMethod(Class<?> targetClass, String methodName, Object ... args) throws Exception {
        if (targetClass == null || methodName == null) {
            return null;
        }
        return this.invokeMethod(targetClass, (Object)null, methodName, args);
    }

    private Object invokeMethod(Class<?> targetClass, Object target, String methodName, Object ... args) throws Exception {
        for (Method method : targetClass.getMethods()) {
            Class<?>[] parameterTypes;
            if (!methodName.equals(method.getName()) || (parameterTypes = method.getParameterTypes()).length != args.length || !this.parametersAccept(parameterTypes, args)) continue;
            return method.invoke(target, args);
        }
        throw new NoSuchMethodException(targetClass.getName() + "." + methodName);
    }

    private boolean parametersAccept(Class<?>[] parameterTypes, Object[] args) {
        for (int i = 0; i < parameterTypes.length; ++i) {
            Class<?> parameterType;
            Object arg = args[i];
            Class<?> rawType = parameterTypes[i];
            if (!(arg == null ? rawType.isPrimitive() : !(parameterType = this.wrapPrimitive(rawType)).isAssignableFrom(arg.getClass()))) continue;
            return false;
        }
        return true;
    }

    private Class<?> wrapPrimitive(Class<?> type) {
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

    private ItemContainer unwrapItemContainerCandidate(Object candidate) {
        String[] nestedMethods;
        if (candidate == null) {
            return null;
        }
        if (candidate instanceof ItemContainer) {
            ItemContainer container = (ItemContainer)candidate;
            return container;
        }
        for (String methodName : nestedMethods = new String[]{"getItemContainer", "getContainer", "getStorage", "getCombinedHotbarFirst"}) {
            try {
                Method m = candidate.getClass().getMethod(methodName, new Class[0]);
                Object nested = m.invoke(candidate, new Object[0]);
                if (!(nested instanceof ItemContainer)) continue;
                ItemContainer container = (ItemContainer)nested;
                return container;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return null;
    }

    private int moveDepositableItemsIntoChest(NPCEntity npcEntity, ItemContainer chestContainer, int cap, CompanionRuntimeState runtime, CompanionRecord companion) {
        if (npcEntity == null || chestContainer == null || cap <= 0) {
            return 0;
        }
        int moved = 0;
        for (ItemContainer companionContainer : this.getCompanionContainersForRead(npcEntity)) {
            if (companionContainer == null) continue;
            ArrayList slots = new ArrayList();
            companionContainer.forEach((slot, stack) -> {
                if (stack == null || stack.isEmpty()) {
                    return;
                }
                String id = stack.getItemId();
                if (!this.isDepositableItem(id, runtime, companion)) {
                    return;
                }
                slots.add(new ContainerSlotRef(companionContainer, slot, (ItemStack)stack));
            });
            for (ContainerSlotRef ref : slots) {
                int afterQty;
                int movedNow;
                int beforeQty;
                if (moved >= cap) {
                    return moved;
                }
                ItemStack stack2 = ref.stack;
                if (stack2 == null || stack2.isEmpty() || (beforeQty = stack2.getQuantity()) <= 0) continue;
                ItemStack remaining = stack2;
                boolean inserted = false;
                try {
                    ItemStackTransaction tx = chestContainer.addItemStack(stack2);
                    if (tx != null && tx.succeeded()) {
                        ItemStack rem = tx.getRemainder();
                        remaining = rem == null ? ItemStack.EMPTY : rem;
                        inserted = true;
                    } else {
                        remaining = stack2;
                    }
                }
                catch (Throwable ignored) {
                    remaining = stack2;
                }
                if (!inserted || this.sameQuantity(remaining, stack2)) {
                    ItemStack rem = this.insertIntoMatchingStacks(chestContainer, stack2);
                    remaining = (rem = this.insertIntoEmptySlots(chestContainer, rem)) == null ? ItemStack.EMPTY : rem;
                }
                if ((movedNow = Math.max(0, beforeQty - (afterQty = remaining == null || remaining.isEmpty() ? 0 : remaining.getQuantity()))) <= 0) continue;
                moved += movedNow;
                ItemStack replacement = remaining == null || remaining.isEmpty() ? ItemStack.EMPTY : remaining;
                try {
                    ref.container.setItemStackForSlot(ref.slot, replacement);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                if (moved < cap) continue;
                return moved;
            }
        }
        return moved;
    }

    private boolean sameQuantity(ItemStack a, ItemStack b) {
        if (a == null || a.isEmpty()) {
            return b == null || b.isEmpty();
        }
        if (b == null || b.isEmpty()) {
            return false;
        }
        return a.getQuantity() == b.getQuantity();
    }

    private ItemStack insertIntoMatchingStacks(ItemContainer chest, ItemStack incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack[] remain = new ItemStack[]{incoming};
        chest.forEach((slot, existing) -> {
            if (remain[0] == null || remain[0].isEmpty()) {
                return;
            }
            if (existing == null || existing.isEmpty()) {
                return;
            }
            if (!this.sameItem((ItemStack)existing, remain[0])) {
                return;
            }
            int max = this.safeMaxStack((ItemStack)existing);
            int qty = existing.getQuantity();
            if (qty >= max) {
                return;
            }
            int canAdd = Math.min(max - qty, remain[0].getQuantity());
            if (canAdd <= 0) {
                return;
            }
            ItemStack newExisting = existing.withQuantity(qty + canAdd);
            ItemStack newRemain = remain[0].withQuantity(remain[0].getQuantity() - canAdd);
            try {
                chest.setItemStackForSlot(slot, newExisting);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            remain[0] = newRemain.getQuantity() <= 0 ? ItemStack.EMPTY : newRemain;
        });
        return remain[0];
    }

    private ItemStack insertIntoEmptySlots(ItemContainer chest, ItemStack incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack[] remain = new ItemStack[]{incoming};
        ArrayList emptySlots = new ArrayList();
        chest.forEach((slot, existing) -> {
            if (existing == null || existing.isEmpty()) {
                emptySlots.add(slot);
            }
        });
        Iterator iterator = emptySlots.iterator();
        while (iterator.hasNext()) {
            int move;
            short slot2 = (Short)iterator.next();
            if (remain[0] == null || remain[0].isEmpty() || (move = Math.min(this.safeMaxStack(remain[0]), remain[0].getQuantity())) <= 0) break;
            ItemStack toInsert = remain[0].withQuantity(move);
            try {
                chest.setItemStackForSlot(slot2, toInsert);
            }
            catch (Throwable ignored) {
                continue;
            }
            int left = remain[0].getQuantity() - move;
            remain[0] = left <= 0 ? ItemStack.EMPTY : remain[0].withQuantity(left);
        }
        return remain[0];
    }

    private int safeMaxStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 1;
        }
        return 64;
    }

    private boolean sameItem(ItemStack a, ItemStack b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return false;
        }
        String ai = a.getItemId();
        String bi = b.getItemId();
        return ai != null && ai.equals(bi);
    }

    private boolean hasDepositableItems(NPCEntity npcEntity, CompanionRuntimeState runtime, CompanionRecord companion) {
        if (npcEntity == null) {
            return false;
        }
        try {
            for (ItemContainer container : this.getCompanionContainersForRead(npcEntity)) {
                if (container == null) continue;
                boolean[] found = new boolean[]{false};
                container.forEach((slot, stack) -> {
                    if (found[0]) {
                        return;
                    }
                    if (stack == null || stack.isEmpty()) {
                        return;
                    }
                    String id = stack.getItemId();
                    if (this.isDepositableItem(id, runtime, companion)) {
                        found[0] = true;
                    }
                });
                if (!found[0]) continue;
                return true;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return false;
    }

    private boolean isDepositableItem(String itemId, CompanionRuntimeState runtime, CompanionRecord companion) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        if (this.isGearItem(itemId)) {
            return false;
        }
        if (this.isAmmoItem(itemId)) {
            return false;
        }
        if (this.isSeedItem(itemId)) {
            return false;
        }
        if (runtime != null && runtime.mineDepositOnlyMined) {
            return runtime.mineCollectedItemIds.contains(itemId);
        }
        return true;
    }

    private boolean isSeedItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        String low = itemId.toLowerCase();
        return low.startsWith("plant_seeds_") || low.startsWith("plant_seed_") || low.startsWith("seeds_") || low.startsWith("seed_") || low.endsWith("_seeds") || low.endsWith("_seed") || low.contains("_seeds_") || low.contains("_seed_") || low.contains("seedbag") || low.contains("seed_bag");
    }

    private boolean isAmmoItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        String low = itemId.toLowerCase();
        return low.equals("arrow") || low.equals("arrows") || low.equals("ammo") || low.startsWith("arrow_") || low.startsWith("arrows_") || low.startsWith("ammo_") || low.startsWith("bolt_") || low.startsWith("bolts_") || low.endsWith("_arrow") || low.endsWith("_arrows") || low.endsWith("_ammo") || low.endsWith("_bolt") || low.endsWith("_bolts") || low.contains("_arrow_") || low.contains("_arrows_") || low.contains("_ammo_") || low.contains("_bolt_") || low.contains("_bolts_") || low.contains("quiver");
    }

    private List<ItemContainer> getCompanionContainersForRead(NPCEntity npcEntity) {
        ArrayList<ItemContainer> out = new ArrayList<ItemContainer>();
        if (npcEntity == null || npcEntity.getInventory() == null) {
            return out;
        }
        IdentityHashMap<ItemContainer, Boolean> seen = new IdentityHashMap<ItemContainer, Boolean>();
        this.addContainer(out, seen, (ItemContainer)npcEntity.getInventory().getCombinedHotbarFirst());
        return out;
    }

    private void addContainer(List<ItemContainer> out, IdentityHashMap<ItemContainer, Boolean> seen, ItemContainer container) {
        if (container == null || seen.containsKey(container)) {
            return;
        }
        seen.put(container, Boolean.TRUE);
        out.add(container);
    }

    private boolean isGearItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        String low = itemId.toLowerCase();
        if (low.contains("weapon_") || low.contains("tool_") || low.startsWith("weapon_") || low.startsWith("tool_") || low.startsWith("armor_") || low.startsWith("armour_") || low.startsWith("shield_") || low.startsWith("bow_") || low.startsWith("crossbow_") || low.startsWith("sword_") || low.startsWith("axe_") || low.startsWith("pickaxe_") || low.startsWith("spear_") || low.startsWith("staff_") || low.startsWith("wand_") || low.startsWith("helmet_") || low.startsWith("chestplate_") || low.startsWith("leggings_") || low.startsWith("boots_") || low.endsWith("_helmet") || low.endsWith("_chestplate") || low.endsWith("_leggings") || low.endsWith("_boots") || low.endsWith("_shield") || low.endsWith("_bow") || low.endsWith("_crossbow") || low.endsWith("_sword") || low.endsWith("_axe") || low.endsWith("_pickaxe") || low.endsWith("_spear") || low.endsWith("_staff") || low.endsWith("_wand")) {
            return true;
        }
        return EquipmentSlot.forItem(itemId) != null;
    }

    private boolean isBlockedChest(CompanionRuntimeState runtime, long currentTick, BlockPos pos) {
        if (runtime == null || pos == null || runtime.depositBlockedChestKey == null) {
            return false;
        }
        return currentTick < runtime.depositBlockedUntilTick && runtime.depositBlockedChestKey.equals(this.blockPosKey(pos));
    }

    private boolean sameBlockPos(BlockPos a, BlockPos b) {
        return a != null && b != null && a.x == b.x && a.y == b.y && a.z == b.z;
    }

    private String blockPosKey(BlockPos pos) {
        return pos.x + "," + pos.y + "," + pos.z;
    }

    private void logDepositContainerMiss(World world, BlockPos chestPos) {
    }

    private String formatBlockPos(BlockPos pos) {
        if (pos == null) {
            return "(null)";
        }
        return "(" + pos.x + "," + pos.y + "," + pos.z + ")";
    }

    private String formatBlockPosList(List<BlockPos> positions) {
        if (positions == null || positions.isEmpty()) {
            return "[]";
        }
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < positions.size(); ++i) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(this.formatBlockPos(positions.get(i)));
        }
        out.append(']');
        return out.toString();
    }

    private List<BlockPos> getEffectiveLinkedChests(CompanionRecord companion, PlayerCompanionData data) {
        if (companion != null && companion.linkedChests != null && !companion.linkedChests.isEmpty()) {
            return companion.linkedChests;
        }
        return List.of();
    }

    private static final class ContainerSlotRef {
        final ItemContainer container;
        final short slot;
        final ItemStack stack;

        ContainerSlotRef(ItemContainer container, short slot, ItemStack stack) {
            this.container = container;
            this.slot = slot;
            this.stack = stack;
        }
    }
}

