/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.logger.HytaleLogger
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.server.core.inventory.Inventory
 *  com.hypixel.hytale.server.core.inventory.ItemStack
 *  com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer
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
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.movement.controllers.MotionController;
import com.yourname.companion.data.BlockPos;
import com.yourname.companion.data.CompanionMode;
import com.yourname.companion.data.CompanionRecord;
import com.yourname.companion.data.ProgressionConfig;
import com.yourname.companion.runtime.CompanionRuntimeState;
import com.yourname.companion.runtime.MovementOwner;
import com.yourname.companion.runtime.Vec3;
import com.yourname.companion.systems.WorkPositioning;
import com.yourname.companion.util.InvUtil;
import com.yourname.companion.util.WorldQueries;
import java.lang.invoke.CallSite;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.regex.Pattern;

public final class FarmSystem {
    private static final boolean DEBUG_FARM_TRACE = false;
    private static final int FARM_SCAN_INTERVAL = 80;
    private static final int FARM_IDLE_SCAN_INTERVAL = 240;
    private static final int FARM_IDLE_SCAN_JITTER = 40;
    private static final double FARM_ACTION_RANGE = 1.75;
    private static final double WATER_RANGE = 2.75;
    private static final double HARVEST_VERTICAL_TOLERANCE = 2.5;
    private static final double MOVE_SPEED = 4.0;
    private static final long FARM_MOVE_COMMAND_INTERVAL_TICKS = 5L;
    private static final double MOVE_DEADZONE = 0.15;
    private static final int UNREACHABLE_TIMEOUT = 80;
    private static final int STUCK_HARVEST_REPEAT_THRESHOLD = 3;
    private static final long STUCK_HARVEST_COOLDOWN_TICKS = 300L;
    private static final int FARM_SCAN_Y_BELOW = 3;
    private static final int FARM_SCAN_Y_ABOVE = 3;
    private static final int MAX_FARM_AREA_BLOCKS = 1600;
    private static final long NO_HARVEST_AFTER_PLANT_TICKS = 600L;
    private static final long FARM_IDLE_LOG_INTERVAL_TICKS = 200L;
    private static final Set<String> TILLABLE_SOIL_IDS = Set.of("soil_dirt", "soil_dirt_burnt", "soil_dirt_cold", "soil_dirt_dry", "soil_grass", "soil_grass_burnt", "soil_grass_cold", "soil_grass_deep", "soil_grass_dry", "soil_grass_full", "soil_grass_sunny", "soil_leaves", "soil_mud", "soil_mud_dry", "soil_needles", "soil_pathway");
    private static final String TILLED_SOIL_ID = "soil_dirt_tilled";
    private static final String SEED_PREFIX = "plant_seeds_";
    private static final String CROP_PREFIX = "plant_crop_";
    private static final String LIFE_ESSENCE_ITEM_ID = "Ingredient_Life_Essence";
    private final HytaleLogger logger;

    public FarmSystem(HytaleLogger logger) {
        this.logger = logger;
    }

    public boolean evaluateFarm(World world, NPCEntity npcEntity, CompanionRecord companion, CompanionRuntimeState runtime, Vector3d companionPos, long currentTick) {
        if (companion.mode != CompanionMode.FARMER) {
            runtime.farmStatusText = "Inactive";
            return false;
        }
        this.runPendingHarvestVerification(world, runtime, currentTick);
        if (runtime.farmBlockedTargetKey != null && currentTick >= runtime.farmBlockedUntilTick) {
            runtime.farmBlockedTargetKey = null;
            runtime.farmBlockedUntilTick = 0L;
        }
        if (companion.farmAreaTopLeft == null || companion.farmAreaBottomRight == null) {
            runtime.farmStatusText = "Farm area not set";
            runtime.farmTargetPos = null;
            runtime.farmTaskType = null;
            runtime.farmPlantSeedId = null;
            runtime.nextFarmScanTick = 0L;
            return false;
        }
        if (runtime.farmTargetPos == null && runtime.nextFarmScanTick > 0L && currentTick < runtime.nextFarmScanTick) {
            return false;
        }
        if (runtime.farmTargetPos != null && currentTick - runtime.lastFarmScanTick < 80L) {
            if (!this.isCurrentFarmTargetActionable(world, npcEntity, runtime)) {
                runtime.farmStatusText = "Retargeting";
                runtime.farmTargetPos = null;
                runtime.farmTaskType = null;
                runtime.farmPlantSeedId = null;
                runtime.nextFarmScanTick = 0L;
                return false;
            }
            runtime.farmStatusText = "Acting: " + runtime.farmTaskType;
            return true;
        }
        runtime.lastFarmScanTick = currentTick;
        FarmBounds bounds = FarmBounds.from(companion.farmAreaTopLeft, companion.farmAreaBottomRight);
        if (bounds == null || bounds.area() <= 0) {
            runtime.farmStatusText = "Invalid farm area";
            runtime.farmTargetPos = null;
            runtime.farmTaskType = null;
            runtime.farmPlantSeedId = null;
            runtime.nextFarmScanTick = 0L;
            return false;
        }
        if (bounds.area() > 1600) {
            runtime.farmStatusText = "Farm area too large";
            runtime.farmTargetPos = null;
            runtime.farmTaskType = null;
            runtime.farmPlantSeedId = null;
            runtime.nextFarmScanTick = currentTick + 240L + (long)this.farmScanJitter(companion);
            return false;
        }
        int minY = Math.min(companion.farmAreaTopLeft.y, companion.farmAreaBottomRight.y) - 3;
        int maxY = Math.max(companion.farmAreaTopLeft.y, companion.farmAreaBottomRight.y) + 3;
        ArrayList<BlockPos> harvestReady = new ArrayList<BlockPos>();
        ArrayList<BlockPos> plantable = new ArrayList<BlockPos>();
        ArrayList<BlockPos> tillable = new ArrayList<BlockPos>();
        ArrayList<BlockPos> waterable = new ArrayList<BlockPos>();
        Map<String, Integer> seedCounts = this.getAvailableSeeds(npcEntity);
        HashMap<String, Integer> sampledIds = new HashMap<String, Integer>();
        this.scanFarmArea(world, bounds, minY, maxY, harvestReady, plantable, tillable, waterable, sampledIds, runtime);
        if (sampledIds.isEmpty() && companionPos != null) {
            int cy = (int)Math.floor(companionPos.y);
            int broadMinY = Math.max(0, cy - 48);
            int broadMaxY = cy + 16;
            if (broadMinY < minY || broadMaxY > maxY) {
                this.scanFarmArea(world, bounds, broadMinY, broadMaxY, harvestReady, plantable, tillable, waterable, sampledIds, runtime);
                minY = Math.min(minY, broadMinY);
                maxY = Math.max(maxY, broadMaxY);
            }
        }
        int sampledCropBlocks = this.countSampledCropBlocks(sampledIds);
        this.pruneExpiredNoHarvestTargets(runtime, currentTick);
        harvestReady.removeIf(pos -> this.isNoHarvestTarget(runtime, currentTick, pos.x, pos.y, pos.z));
        if (runtime.farmDepositPending && harvestReady.isEmpty() && companion.linkedChests != null && !companion.linkedChests.isEmpty()) {
            runtime.farmStatusText = "Depositing harvest";
            runtime.depositRequested = true;
            runtime.farmDepositPending = false;
            runtime.farmTargetPos = null;
            runtime.farmTaskType = null;
            runtime.farmPlantSeedId = null;
            runtime.nextFarmScanTick = 0L;
            return false;
        }
        if (harvestReady.isEmpty() && plantable.isEmpty() && tillable.isEmpty() && waterable.isEmpty()) {
            runtime.farmStatusText = sampledIds.isEmpty() ? "No loaded blocks in farm area" : (sampledCropBlocks > 0 ? "Waiting for mature crops" : "No farm tasks in area");
            runtime.farmTargetPos = null;
            runtime.farmTaskType = null;
            runtime.farmPlantSeedId = null;
            runtime.nextFarmScanTick = currentTick + 240L + (long)this.farmScanJitter(companion);
            return false;
        }
        BlockPos target = this.rowOrderedTarget(tillable, bounds, companion.farmAreaTopLeft, companion.farmAreaBottomRight, runtime, currentTick);
        String task = "TILL";
        if (target == null && !seedCounts.isEmpty()) {
            target = this.rowOrderedTarget(plantable, bounds, companion.farmAreaTopLeft, companion.farmAreaBottomRight, runtime, currentTick);
            task = "PLANT";
        }
        if (target == null) {
            target = this.rowOrderedTarget(waterable, bounds, companion.farmAreaTopLeft, companion.farmAreaBottomRight, runtime, currentTick);
            task = "WATER";
        }
        if (target == null) {
            target = this.rowOrderedTarget(harvestReady, bounds, companion.farmAreaTopLeft, companion.farmAreaBottomRight, runtime, currentTick);
            task = "HARVEST";
        }
        if (target == null) {
            runtime.farmStatusText = !plantable.isEmpty() && seedCounts.isEmpty() ? "No seeds available" : "No actionable farm target";
            runtime.farmTargetPos = null;
            runtime.farmTaskType = null;
            runtime.farmPlantSeedId = null;
            runtime.nextFarmScanTick = currentTick + 240L + (long)this.farmScanJitter(companion);
            return false;
        }
        runtime.farmTargetPos = new Vec3((double)target.x + 0.5, (double)target.y + 0.5, (double)target.z + 0.5);
        runtime.farmTaskType = task;
        runtime.farmPlantSeedId = "PLANT".equals(task) ? this.chooseSeedId(seedCounts) : null;
        runtime.farmUnreachableTicks = 0;
        runtime.farmStatusText = "Acting: " + task;
        runtime.nextFarmScanTick = 0L;
        return true;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public void executeFarmSeek(World world, Store<EntityStore> store, NPCEntity npcEntity, Ref<EntityStore> companionRef, CompanionRecord companion, CompanionRuntimeState runtime, Vector3d companionPos) {
        double actionRange;
        if (runtime.farmTargetPos == null) {
            runtime.farmStatusText = "Waiting for farm target";
            return;
        }
        String taskType = runtime.farmTaskType != null ? runtime.farmTaskType : "HARVEST";
        runtime.farmStatusText = "Acting: " + taskType;
        int cropX = (int)Math.floor(runtime.farmTargetPos.x);
        int cropY = (int)Math.floor(runtime.farmTargetPos.y);
        int cropZ = (int)Math.floor(runtime.farmTargetPos.z);
        Vector3d targetPos = new Vector3d(runtime.farmTargetPos.x, runtime.farmTargetPos.y, runtime.farmTargetPos.z);
        Vector3d workPos = this.resolveFarmWorkPosition(world, companionPos, cropX, cropY, cropZ, taskType);
        double dxToWork = workPos.x - companionPos.x;
        double dzToWork = workPos.z - companionPos.z;
        double horizontalDistanceToWork = Math.sqrt(dxToWork * dxToWork + dzToWork * dzToWork);
        double dxToTarget = targetPos.x - companionPos.x;
        double dzToTarget = targetPos.z - companionPos.z;
        double horizontalDistanceToTarget = Math.sqrt(dxToTarget * dxToTarget + dzToTarget * dzToTarget);
        double horizontalDistance = Math.min(horizontalDistanceToWork, horizontalDistanceToTarget);
        double verticalDistance = Math.abs(workPos.y - companionPos.y);
        String targetKey = this.farmTargetKey(cropX, cropY, cropZ);
        double d = actionRange = "WATER".equals(taskType) ? 2.75 : 1.75;
        if (horizontalDistance <= actionRange && verticalDistance <= 2.5) {
            try {
                MotionController mc = npcEntity.getRole().getActiveMotionController();
                if (mc != null) {
                    mc.forceVelocity(new Vector3d(0.0, 0.0, 0.0), null, false);
                }
            }
            catch (Throwable mc) {
                // empty catch block
            }
            boolean harvested = false;
            if ("HARVEST".equals(taskType)) {
                String beforeRaw = WorldQueries.getBlockTypeAt(world, cropX, cropY, cropZ);
                String beforeLower = beforeRaw != null ? beforeRaw.toLowerCase() : "";
                String beforeNorm = this.normalizeBlockId(beforeLower);
                boolean beforeIsMature = this.isHarvestReadyCrop(beforeNorm, beforeLower);
                if (this.isNoHarvestTarget(runtime, runtime.currentTick, cropX, cropY, cropZ)) {
                    beforeIsMature = false;
                }
                boolean attempted = beforeIsMature && this.harvestCropForFarm(world, cropX, cropY, cropZ, beforeNorm);
                String immediateRaw = WorldQueries.getBlockTypeAt(world, cropX, cropY, cropZ);
                String immediateLower = immediateRaw != null ? immediateRaw.toLowerCase() : "";
                String immediateNorm = this.normalizeBlockId(immediateLower);
                boolean immediateIsMature = this.isHarvestReadyCrop(immediateNorm, immediateLower);
                boolean bl = harvested = attempted && !immediateIsMature;
                if (harvested) {
                    this.grantHarvestYieldToCompanion(npcEntity, companionRef, store, companion, beforeNorm);
                    runtime.farmDepositPending = true;
                    runtime.farmWateredTargetKeys.remove(targetKey);
                }
                runtime.farmHarvestVerifyTargetKey = targetKey;
                runtime.farmHarvestVerifyX = cropX;
                runtime.farmHarvestVerifyY = cropY;
                runtime.farmHarvestVerifyZ = cropZ;
                runtime.farmHarvestVerifyTick1 = runtime.currentTick + 1L;
                runtime.farmHarvestVerifyTick2 = runtime.currentTick + 10L;
                runtime.farmHarvestBeforeId = beforeNorm;
                runtime.farmHarvestAttempted = attempted;
            } else if ("TILL".equals(taskType)) {
                harvested = this.tillSoil(world, cropX, cropY, cropZ);
                if (!harvested) {
                    harvested = this.tillSoil(world, cropX, cropY - 1, cropZ);
                }
            } else if ("PLANT".equals(taskType)) {
                String seedId = runtime.farmPlantSeedId;
                harvested = this.plantSeedAt(world, npcEntity, cropX, cropY, cropZ, seedId);
                if (harvested) {
                    runtime.farmWateredTargetKeys.remove(this.farmTargetKey(cropX, cropY, cropZ));
                    runtime.farmNoHarvestUntilByTargetKey.put(this.farmTargetKey(cropX, cropY, cropZ), runtime.currentTick + 600L);
                }
            } else if ("WATER".equals(taskType)) {
                harvested = this.waterCropAt(world, runtime, cropX, cropY, cropZ);
            }
            if (harvested) {
                if ("HARVEST".equals(taskType)) {
                    ++companion.farmHarvests;
                    this.checkFarmLevelUp(companion);
                }
                runtime.farmLastTargetKey = null;
                runtime.farmStuckRepeatCount = 0;
                int levelIdx = Math.min(companion.farmLevel - 1, ProgressionConfig.FARM_SPEED_MULT.length - 1);
                if (levelIdx < ProgressionConfig.FARM_AUTO_REPLANT.length && !ProgressionConfig.FARM_AUTO_REPLANT[levelIdx]) {
                    // empty if block
                }
            } else if ("HARVEST".equals(taskType)) {
                if (targetKey.equals(runtime.farmLastTargetKey)) {
                    ++runtime.farmStuckRepeatCount;
                } else {
                    runtime.farmLastTargetKey = targetKey;
                    runtime.farmStuckRepeatCount = 1;
                }
                if (runtime.farmStuckRepeatCount >= 3) {
                    runtime.farmBlockedTargetKey = targetKey;
                    runtime.farmBlockedUntilTick = runtime.currentTick + 300L;
                    runtime.farmStuckRepeatCount = 0;
                    runtime.farmLastTargetKey = null;
                }
            } else {
                runtime.farmBlockedTargetKey = targetKey;
                runtime.farmBlockedUntilTick = runtime.currentTick + 300L;
            }
            runtime.farmTargetPos = null;
            runtime.farmTaskType = null;
            runtime.farmPlantSeedId = null;
            runtime.farmUnreachableTicks = 0;
            runtime.farmTurnTargetKey = null;
            runtime.farmTurnUntilTick = 0L;
            runtime.lastFarmScanTick = Math.max(0L, runtime.currentTick - 80L);
            runtime.nextFarmScanTick = 0L;
            return;
        }
        try {
            if (runtime.movementOwner != MovementOwner.FARM) {
                return;
            }
            if (runtime.currentTick - runtime.lastFarmMoveTick >= 5L) {
                if (WorkPositioning.setManagedWorkPosition(npcEntity, workPos, this.logger)) {
                    runtime.lastFarmMoveTick = runtime.currentTick;
                    return;
                }
                double dx = workPos.x - companionPos.x;
                double dz = workPos.z - companionPos.z;
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.15) {
                    Vector3d velocity = new Vector3d(dx / len * 4.0, 0.0, dz / len * 4.0);
                    MotionController mc = npcEntity.getRole().getActiveMotionController();
                    if (mc != null) {
                        mc.forceVelocity(velocity, null, false);
                    }
                }
                runtime.lastFarmMoveTick = runtime.currentTick;
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to move companion to crop.");
        }
        ++runtime.farmUnreachableTicks;
        if (runtime.farmUnreachableTicks >= 80) {
            String blockedKey;
            int tx = (int)Math.floor(runtime.farmTargetPos.x);
            int ty = (int)Math.floor(runtime.farmTargetPos.y);
            int tz = (int)Math.floor(runtime.farmTargetPos.z);
            runtime.farmBlockedTargetKey = blockedKey = this.farmTargetKey(tx, ty, tz);
            runtime.farmBlockedUntilTick = runtime.currentTick + 300L;
            runtime.farmTargetPos = null;
            runtime.farmTaskType = null;
            runtime.farmPlantSeedId = null;
            runtime.farmUnreachableTicks = 0;
            runtime.farmTurnTargetKey = null;
            runtime.farmTurnUntilTick = 0L;
            runtime.lastFarmScanTick = Math.max(0L, runtime.currentTick - 80L);
            runtime.farmStatusText = "Retargeting (unreachable)";
            runtime.nextFarmScanTick = 0L;
        }
    }

    private Vector3d resolveFarmWorkPosition(World world, Vector3d companionPos, int cropX, int cropY, int cropZ, String taskType) {
        double feetY = "TILL".equals(taskType) ? (double)cropY + 1.0 : (double)cropY;
        int[][] offsets = new int[][]{{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        Vector3d best = null;
        double bestDist = Double.MAX_VALUE;
        for (int[] offset : offsets) {
            double dist;
            int standX = cropX + offset[0];
            int standZ = cropZ + offset[1];
            if (!this.isFarmStandPosition(world, standX, (int)Math.floor(feetY), standZ)) continue;
            Vector3d candidate = new Vector3d((double)standX + 0.5, feetY, (double)standZ + 0.5);
            double d = dist = companionPos != null ? companionPos.distanceTo(candidate) : 0.0;
            if (!(dist < bestDist)) continue;
            best = candidate;
            bestDist = dist;
        }
        if (best != null) {
            return best;
        }
        return new Vector3d((double)cropX + 0.5, feetY, (double)cropZ + 0.5);
    }

    private boolean isFarmStandPosition(World world, int standX, int feetY, int standZ) {
        if (world == null) {
            return false;
        }
        String feetBlock = this.normalizeBlockId(this.safeLower(WorldQueries.getBlockTypeAt(world, standX, feetY, standZ)));
        String headBlock = this.normalizeBlockId(this.safeLower(WorldQueries.getBlockTypeAt(world, standX, feetY + 1, standZ)));
        String floorBlock = this.normalizeBlockId(this.safeLower(WorldQueries.getBlockTypeAt(world, standX, feetY - 1, standZ)));
        return this.isFarmAirLike(feetBlock) && this.isFarmAirLike(headBlock) && !this.isFarmAirLike(floorBlock);
    }

    private boolean isFarmAirLike(String normId) {
        if (normId == null || normId.isBlank()) {
            return true;
        }
        return "air".equals(normId) || "empty".equals(normId) || normId.contains("id=empty") || normId.contains("group='air'") || normId.contains("drawtype=empty") || normId.contains("material=empty");
    }

    private void scanFarmArea(World world, FarmBounds bounds, int minY, int maxY, List<BlockPos> harvestReady, List<BlockPos> plantable, List<BlockPos> tillable, List<BlockPos> waterable, Map<String, Integer> sampledIds, CompanionRuntimeState runtime) {
        for (int x = bounds.minX; x <= bounds.maxX; ++x) {
            for (int z = bounds.minZ; z <= bounds.maxZ; ++z) {
                for (int y = minY; y <= maxY; ++y) {
                    String id = WorldQueries.getBlockTypeAt(world, x, y, z);
                    if (id == null || id.isBlank()) continue;
                    String lower = id.toLowerCase();
                    String normId = this.normalizeBlockId(lower);
                    if (sampledIds != null) {
                        sampledIds.merge(normId, 1, Integer::sum);
                    }
                    if (this.isWaterableCrop(world, x, y, z, normId, lower, runtime)) {
                        waterable.add(new BlockPos("", x, y, z));
                        continue;
                    }
                    if (this.isHarvestReadyCrop(normId, lower)) {
                        if (!this.isRootCropBlock(world, x, y, z)) continue;
                        harvestReady.add(new BlockPos("", x, y, z));
                        continue;
                    }
                    if (this.isPlantableTilledSoil(world, x, y, z, normId)) {
                        plantable.add(new BlockPos("", x, y + 1, z));
                        continue;
                    }
                    if (!this.isTillableSoil(normId) || !this.isOpenForTilling(world, x, y, z)) continue;
                    tillable.add(new BlockPos("", x, y, z));
                }
            }
        }
    }

    private boolean isHarvestReadyCrop(String normId, String rawLower) {
        if (normId == null) {
            return false;
        }
        if (!normId.contains(CROP_PREFIX)) {
            return false;
        }
        if (normId.contains("stagefinal") || normId.contains("stage_final")) {
            return true;
        }
        if (this.hasExplicitFalseStateMarker(rawLower, "mature") || this.hasExplicitFalseStateMarker(normId, "mature") || this.hasExplicitFalseStateMarker(rawLower, "is_mature") || this.hasExplicitFalseStateMarker(normId, "is_mature")) {
            return false;
        }
        if (this.hasTruthyStateMarker(rawLower, "mature", false) || this.hasTruthyStateMarker(normId, "mature", false) || this.hasTruthyStateMarker(rawLower, "is_mature", false) || this.hasTruthyStateMarker(normId, "is_mature", false)) {
            return true;
        }
        String cropCore = this.extractCropCore(normId);
        int normStage = this.extractStageIndex(normId);
        int rawStage = this.extractStageIndex(rawLower);
        int stage = Math.max(normStage, rawStage);
        int knownMaxStage = this.expectedMaxStageIndexForCrop(cropCore);
        if (stage >= 0 && knownMaxStage >= 0) {
            return stage >= knownMaxStage;
        }
        int maxStage = Math.max(this.extractMaxStageIndex(rawLower), this.extractMaxStageIndex(normId));
        if (stage >= 0 && maxStage >= 0) {
            return stage >= maxStage;
        }
        if (stage >= 0) {
            return false;
        }
        return false;
    }

    private boolean hasExplicitFalseStateMarker(String value, String marker) {
        if (value == null || value.isBlank() || marker == null || marker.isBlank()) {
            return false;
        }
        String v = value.toLowerCase(Locale.ROOT);
        String m = Pattern.quote(marker.toLowerCase(Locale.ROOT));
        Pattern p = Pattern.compile("(^|[^a-z0-9_])" + m + "\\s*[:=]\\s*(false|0)($|[^a-z0-9_])");
        return p.matcher(v).find();
    }

    private boolean hasTruthyStateMarker(String value, String marker, boolean allowBareToken) {
        if (value == null || value.isBlank() || marker == null || marker.isBlank()) {
            return false;
        }
        String v = value.toLowerCase(Locale.ROOT);
        String m = Pattern.quote(marker.toLowerCase(Locale.ROOT));
        Pattern truePat = Pattern.compile("(^|[^a-z0-9_])" + m + "\\s*[:=]\\s*(true|1)($|[^a-z0-9_])");
        if (truePat.matcher(v).find()) {
            return true;
        }
        if (allowBareToken) {
            return this.containsWord(v, marker.toLowerCase(Locale.ROOT));
        }
        return false;
    }

    private boolean containsWord(String value, String word) {
        if (value == null || value.isBlank() || word == null || word.isBlank()) {
            return false;
        }
        int from = 0;
        int idx;
        while ((idx = value.indexOf(word, from)) >= 0) {
            boolean rightOk;
            int end = idx + word.length();
            boolean leftOk = idx == 0 || !this.isWordChar(value.charAt(idx - 1));
            boolean bl = rightOk = end >= value.length() || !this.isWordChar(value.charAt(end));
            if (leftOk && rightOk) {
                return true;
            }
            from = idx + 1;
        }
        return false;
    }

    private boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private int extractMaxStageIndex(String value) {
        String[] maxTokens;
        int idx;
        if (value == null || value.isBlank()) {
            return -1;
        }
        int max = -1;
        int scan = 0;
        while (scan < value.length() && (idx = value.indexOf("stage", scan)) >= 0) {
            int slash = value.indexOf(47, idx);
            if (slash > idx) {
                int p;
                for (p = slash + 1; p < value.length() && !Character.isDigit(value.charAt(p)); ++p) {
                }
                int start = p;
                while (p < value.length() && Character.isDigit(value.charAt(p))) {
                    ++p;
                }
                if (p > start) {
                    try {
                        int rhs = Integer.parseInt(value.substring(start, p));
                        if (rhs > max) {
                            max = rhs;
                        }
                    }
                    catch (NumberFormatException rhs) {
                        // empty catch block
                    }
                }
            }
            scan = idx + 5;
        }
        for (String token : maxTokens = new String[]{"maxstage", "max_stage", "stage_max", "stagemax", "maxage", "max-age"}) {
            int p;
            int idx2 = value.indexOf(token);
            if (idx2 < 0) continue;
            for (p = idx2 + token.length(); p < value.length() && !Character.isDigit(value.charAt(p)); ++p) {
            }
            int start = p;
            while (p < value.length() && Character.isDigit(value.charAt(p))) {
                ++p;
            }
            if (p <= start) continue;
            try {
                int v = Integer.parseInt(value.substring(start, p));
                if (v <= max) continue;
                max = v;
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        return max;
    }

    private int expectedMaxStageIndexForCrop(String cropCore) {
        if (cropCore == null || cropCore.isBlank()) {
            return -1;
        }
        return switch (cropCore) {
            case "carrot", "lettuce", "cauliflower", "aubergine", "onion" -> 2;
            case "corn", "wheat", "rice", "turnip", "cotton", "pumpkin", "potato", "tomato" -> 3;
            case "chilli" -> 4;
            default -> -1;
        };
    }

    private int extractStageIndex(String value) {
        int p;
        if (value == null || value.isBlank()) {
            return -1;
        }
        int idx = value.indexOf("stage");
        if (idx < 0) {
            return -1;
        }
        for (p = idx + "stage".length(); p < value.length() && (value.charAt(p) == '_' || value.charAt(p) == '-' || value.charAt(p) == '=' || value.charAt(p) == ':'); ++p) {
        }
        int start = p;
        while (p < value.length() && Character.isDigit(value.charAt(p))) {
            ++p;
        }
        if (p <= start) {
            return -1;
        }
        try {
            return Integer.parseInt(value.substring(start, p));
        }
        catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private String summarizeTopIds(Map<String, Integer> sampledIds, int maxEntries) {
        if (sampledIds == null || sampledIds.isEmpty()) {
            return "none";
        }
        ArrayList<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(sampledIds.entrySet());
        entries.sort((a, b) -> Integer.compare((Integer)b.getValue(), (Integer)a.getValue()));
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(maxEntries, entries.size());
        for (int i = 0; i < limit; ++i) {
            Map.Entry e = (Map.Entry)entries.get(i);
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append((String)e.getKey()).append(" x").append(e.getValue());
        }
        return sb.toString();
    }

    private int countSampledCropBlocks(Map<String, Integer> sampledIds) {
        if (sampledIds == null || sampledIds.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (Map.Entry<String, Integer> e : sampledIds.entrySet()) {
            if (e == null || e.getKey() == null || e.getValue() == null || !e.getKey().contains(CROP_PREFIX)) continue;
            total += Math.max(0, e.getValue());
        }
        return total;
    }

    private boolean isPlantableTilledSoil(World world, int x, int y, int z, String normId) {
        if (!this.isTilledSoil(normId)) {
            return false;
        }
        String above = this.normalizeBlockId(this.safeLower(WorldQueries.getBlockTypeAt(world, x, y + 1, z)));
        return above.isBlank() || "empty".equals(above) || "air".equals(above);
    }

    private boolean isWaterableCrop(World world, int x, int y, int z, String normId, String rawLower, CompanionRuntimeState runtime) {
        if (world == null || normId == null || !normId.contains(CROP_PREFIX)) {
            return false;
        }
        if (!this.isRootCropBlock(world, x, y, z)) {
            return false;
        }
        String soilBelow = this.normalizeBlockId(this.safeLower(WorldQueries.getBlockTypeAt(world, x, y - 1, z)));
        return this.isTilledSoil(soilBelow) && !this.isWateredSoil(soilBelow);
    }

    private boolean isWateredSoil(String normId) {
        if (normId == null || normId.isBlank()) {
            return false;
        }
        return normId.contains("watered") || normId.contains("wet") || normId.contains("moist") || normId.contains("irrigated");
    }

    private boolean isOpenForTilling(World world, int x, int y, int z) {
        String above = this.normalizeBlockId(this.safeLower(WorldQueries.getBlockTypeAt(world, x, y + 1, z)));
        return above.isBlank() || "empty".equals(above) || "air".equals(above);
    }

    private boolean isCurrentFarmTargetActionable(World world, NPCEntity npcEntity, CompanionRuntimeState runtime) {
        if (world == null || runtime == null || runtime.farmTargetPos == null) {
            return false;
        }
        String task = runtime.farmTaskType != null ? runtime.farmTaskType : "HARVEST";
        int x = (int)Math.floor(runtime.farmTargetPos.x);
        int y = (int)Math.floor(runtime.farmTargetPos.y);
        int z = (int)Math.floor(runtime.farmTargetPos.z);
        if ("HARVEST".equals(task)) {
            String raw = this.safeLower(WorldQueries.getBlockTypeAt(world, x, y, z));
            String norm = this.normalizeBlockId(raw);
            if (!this.isRootCropBlock(world, x, y, z)) {
                return false;
            }
            if (this.isNoHarvestTarget(runtime, runtime.currentTick, x, y, z)) {
                return false;
            }
            return this.isHarvestReadyCrop(norm, raw);
        }
        if ("PLANT".equals(task)) {
            String seedId = runtime.farmPlantSeedId;
            Map<String, Integer> seeds = this.getAvailableSeeds(npcEntity);
            if (seedId == null || !seeds.containsKey(seedId)) {
                return false;
            }
            String above = this.normalizeBlockId(this.safeLower(WorldQueries.getBlockTypeAt(world, x, y, z)));
            String below = this.normalizeBlockId(this.safeLower(WorldQueries.getBlockTypeAt(world, x, y - 1, z)));
            boolean open = above.isBlank() || "empty".equals(above) || "air".equals(above);
            return open && this.isTilledSoil(below);
        }
        if ("TILL".equals(task)) {
            String here = this.normalizeBlockId(this.safeLower(WorldQueries.getBlockTypeAt(world, x, y, z)));
            return this.isTillableSoil(here) && this.isOpenForTilling(world, x, y, z);
        }
        if ("WATER".equals(task)) {
            String raw = this.safeLower(WorldQueries.getBlockTypeAt(world, x, y, z));
            String norm = this.normalizeBlockId(raw);
            return this.isWaterableCrop(world, x, y, z, norm, raw, runtime);
        }
        return false;
    }

    private boolean isTillableSoil(String normId) {
        if (normId == null) {
            return false;
        }
        if (TILLABLE_SOIL_IDS.contains(normId)) {
            return true;
        }
        if (normId.contains("soil_mud") && normId.contains("dry")) {
            return true;
        }
        if (normId.contains("mud_dry")) {
            return true;
        }
        return normId.contains("dry_mud");
    }

    private void grantHarvestYieldToCompanion(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, CompanionRecord companion, String beforeNorm) {
        String[] candidates;
        String core = this.extractCropCore(beforeNorm);
        if (core == null || core.isBlank()) {
            return;
        }
        String harvestItemId = this.canonicalHarvestItemIdForCrop(core);
        if (harvestItemId == null) {
            this.grantLifeEssenceByCrop(npcEntity, companionRef, store, core);
            return;
        }
        for (String itemId : candidates = new String[]{harvestItemId}) {
            try {
                if (!InvUtil.giveItemToCompanion(npcEntity, companionRef, store, itemId, 1, this.logger)) continue;
                this.grantEternalSeedByCrop(npcEntity, companionRef, store, companion, core);
                this.grantLifeEssenceByCrop(npcEntity, companionRef, store, core);
                return;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        this.grantLifeEssenceByCrop(npcEntity, companionRef, store, core);
    }

    private void grantEternalSeedByCrop(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, CompanionRecord companion, String cropCore) {
        if (companion == null || cropCore == null || cropCore.isBlank()) {
            return;
        }
        int levelIdx = Math.max(0, Math.min(companion.farmLevel - 1, ProgressionConfig.FARM_ETERNAL_SEED_CHANCE.length - 1));
        double chance = ProgressionConfig.FARM_ETERNAL_SEED_CHANCE[levelIdx];
        if (chance <= 0.0 || ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }
        String eternalSeedId = this.canonicalEternalSeedIdForCrop(cropCore);
        if (eternalSeedId == null) {
            return;
        }
        try {
            if (InvUtil.giveItemToCompanion(npcEntity, companionRef, store, eternalSeedId, 1, this.logger)) {
                companion.farmEternalSeedsGrantedByCrop.merge(cropCore.toLowerCase(Locale.ROOT), 1, Integer::sum);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private String canonicalHarvestItemIdForCrop(String cropCore) {
        if (cropCore == null || cropCore.isBlank()) {
            return null;
        }
        String core = cropCore.toLowerCase(Locale.ROOT);
        if (core.endsWith("_eternal")) {
            core = core.substring(0, core.length() - "_eternal".length());
        }
        return switch (core) {
            case "wheat" -> "Plant_Crop_Wheat_Item";
            case "lettuce" -> "Plant_Crop_Lettuce_Item";
            case "corn" -> "Plant_Crop_Corn_Item";
            case "carrot" -> "Plant_Crop_Carrot_Item";
            case "turnip" -> "Plant_Crop_Turnip_Item";
            case "cauliflower" -> "Plant_Crop_Cauliflower_Item";
            case "aubergine" -> "Plant_Crop_Aubergine_Item";
            case "pumpkin" -> "Plant_Crop_Pumpkin_Item";
            case "tomato" -> "Plant_Crop_Tomato_Item";
            case "cotton" -> "Plant_Crop_Cotton_Item";
            case "chilli" -> "Plant_Crop_Chilli_Item";
            case "rice" -> "Plant_Crop_Rice_Item";
            case "onion" -> "Plant_Crop_Onion_Item";
            case "potato" -> "Plant_Crop_Potato_Item";
            default -> null;
        };
    }

    private String canonicalEternalSeedIdForCrop(String cropCore) {
        if (cropCore == null || cropCore.isBlank()) {
            return null;
        }
        String core = cropCore.toLowerCase(Locale.ROOT);
        if (core.endsWith("_eternal")) {
            core = core.substring(0, core.length() - "_eternal".length());
        }
        return switch (core) {
            case "wheat" -> "Plant_Seeds_Wheat_Eternal";
            case "lettuce" -> "Plant_Seeds_Lettuce_Eternal";
            case "corn" -> "Plant_Seeds_Corn_Eternal";
            case "carrot" -> "Plant_Seeds_Carrot_Eternal";
            case "turnip" -> "Plant_Seeds_Turnip_Eternal";
            case "cauliflower" -> "Plant_Seeds_Cauliflower_Eternal";
            case "aubergine" -> "Plant_Seeds_Aubergine_Eternal";
            case "pumpkin" -> "Plant_Seeds_Pumpkin_Eternal";
            case "tomato" -> "Plant_Seeds_Tomato_Eternal";
            case "cotton" -> "Plant_Seeds_Cotton_Eternal";
            case "chilli" -> "Plant_Seeds_Chilli_Eternal";
            case "rice" -> "Plant_Seeds_Rice_Eternal";
            case "onion" -> "Plant_Seeds_Onion_Eternal";
            case "potato" -> "Plant_Seeds_Potato_Eternal";
            default -> null;
        };
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private void grantLifeEssenceByCrop(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, String cropCore) {
        int[] range = this.lifeEssenceRangeForCrop(cropCore);
        if (range == null) {
            return;
        }
        int min = Math.max(0, range[0]);
        int max = Math.max(min, range[1]);
        if (max <= 0) {
            return;
        }
        int qty = ThreadLocalRandom.current().nextInt(min, max + 1);
        if (qty <= 0) {
            return;
        }
        try {
            if (!InvUtil.giveItemToCompanion(npcEntity, companionRef, store, LIFE_ESSENCE_ITEM_ID, qty, this.logger)) return;
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private int[] lifeEssenceRangeForCrop(String cropCore) {
        if (cropCore == null || cropCore.isBlank()) {
            return null;
        }
        return new int[]{1, 2};
    }

    private String extractCropCore(String normId) {
        if (normId == null || normId.isBlank()) {
            return null;
        }
        String n = normId.startsWith("*") ? normId.substring(1) : normId;
        int start = n.indexOf(CROP_PREFIX);
        if (start < 0) {
            return null;
        }
        int end = n.indexOf("_block", start += CROP_PREFIX.length());
        if (end <= start) {
            return null;
        }
        return n.substring(start, end);
    }

    private boolean isRootCropBlock(World world, int x, int y, int z) {
        String here = this.normalizeBlockId(this.safeLower(WorldQueries.getBlockTypeAt(world, x, y, z)));
        if (!here.contains(CROP_PREFIX)) {
            return false;
        }
        String below = this.normalizeBlockId(this.safeLower(WorldQueries.getBlockTypeAt(world, x, y - 1, z)));
        return this.isTilledSoil(below);
    }

    private boolean isTilledSoil(String normId) {
        if (normId == null || normId.isBlank()) {
            return false;
        }
        if (TILLED_SOIL_ID.equals(normId)) {
            return true;
        }
        return normId.contains("tilled");
    }

    private void pruneExpiredNoHarvestTargets(CompanionRuntimeState runtime, long currentTick) {
        if (runtime == null || runtime.farmNoHarvestUntilByTargetKey.isEmpty()) {
            return;
        }
        runtime.farmNoHarvestUntilByTargetKey.entrySet().removeIf(e -> e.getValue() == null || (Long)e.getValue() <= currentTick);
    }

    private boolean isNoHarvestTarget(CompanionRuntimeState runtime, long currentTick, int x, int y, int z) {
        if (runtime == null || runtime.farmNoHarvestUntilByTargetKey.isEmpty()) {
            return false;
        }
        Long until = runtime.farmNoHarvestUntilByTargetKey.get(this.farmTargetKey(x, y, z));
        return until != null && until > currentTick;
    }

    private boolean tillSoil(World world, int x, int y, int z) {
        try {
            String current = WorldQueries.getBlockTypeAt(world, x, y, z);
            if (current == null) {
                return false;
            }
            String normId = this.normalizeBlockId(current.toLowerCase());
            if (TILLED_SOIL_ID.equals(normId)) {
                return true;
            }
            if (!this.isTillableSoil(normId)) {
                return false;
            }
            try {
                Method setMethod = world.getClass().getMethod("setBlock", Integer.TYPE, Integer.TYPE, Integer.TYPE, String.class);
                setMethod.invoke((Object)world, x, y, z, TILLED_SOIL_ID);
                return true;
            }
            catch (NoSuchMethodException e) {
                return false;
            }
        }
        catch (Throwable t) {
            return false;
        }
    }

    private boolean harvestCropForFarm(World world, int x, int y, int z, String beforeNorm) {
        if (beforeNorm != null && beforeNorm.contains("eternal")) {
            return this.harvestEternalCropWithoutBreaking(world, x, y, z, beforeNorm);
        }
        return WorldQueries.harvestCrop(world, x, y, z);
    }

    private boolean harvestEternalCropWithoutBreaking(World world, int x, int y, int z, String beforeNorm) {
        String[] resetCandidates;
        if (world == null || beforeNorm == null || beforeNorm.isBlank()) {
            return false;
        }
        String cropCore = this.extractCropCore(beforeNorm);
        if (cropCore == null || cropCore.isBlank()) {
            return false;
        }
        if (cropCore.endsWith("_eternal")) {
            cropCore = cropCore.substring(0, cropCore.length() - "_eternal".length());
        }
        String baseCropId = CROP_PREFIX + cropCore;
        for (String candidate : resetCandidates = this.eternalRegrowBlockCandidates(baseCropId)) {
            String after;
            if (candidate == null || candidate.isBlank() || !this.trySetBlockType(world, x, y, z, new String[]{candidate}) || !(after = this.normalizeBlockId(this.safeLower(WorldQueries.getBlockTypeAt(world, x, y, z)))).contains(baseCropId) || !after.contains("eternal") || this.isHarvestReadyCrop(after, after)) continue;
            return true;
        }
        return false;
    }

    private String[] eternalRegrowBlockCandidates(String baseCropId) {
        if (baseCropId == null || baseCropId.isBlank()) {
            return new String[0];
        }
        return new String[]{baseCropId + "_eternal_block_state_definitions_stage0", "*" + baseCropId + "_eternal_block_state_definitions_stage0", baseCropId + "_block_eternal_state_definitions_stage0", "*" + baseCropId + "_block_eternal_state_definitions_stage0", baseCropId + "_block_state_definitions_eternal_stage0", "*" + baseCropId + "_block_state_definitions_eternal_stage0", baseCropId + "_eternal_block_state_definitions_stage1", "*" + baseCropId + "_eternal_block_state_definitions_stage1", baseCropId + "_block_eternal_state_definitions_stage1", "*" + baseCropId + "_block_eternal_state_definitions_stage1", baseCropId + "_block_state_definitions_eternal_stage1", "*" + baseCropId + "_block_state_definitions_eternal_stage1", baseCropId + "_eternal", "*" + baseCropId + "_eternal", baseCropId + "_block_eternal", "*" + baseCropId + "_block_eternal", baseCropId + "_eternal_block", "*" + baseCropId + "_eternal_block"};
    }

    private Map<String, Integer> getAvailableSeeds(NPCEntity npcEntity) {
        HashMap<String, Integer> counts = new HashMap<String, Integer>();
        if (npcEntity == null) {
            return counts;
        }
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return counts;
            }
            CombinedItemContainer container = inventory.getCombinedHotbarFirst();
            if (container == null) {
                return counts;
            }
            container.forEach((slot, stack) -> {
                if (stack == null || stack.isEmpty()) {
                    return;
                }
                String itemId = stack.getItemId();
                if (itemId == null || itemId.isBlank()) {
                    return;
                }
                String low = itemId.toLowerCase(Locale.ROOT);
                if (!this.isPlantingSeedId(low)) {
                    return;
                }
                counts.merge(low, Math.max(1, stack.getQuantity()), Integer::sum);
            });
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return counts;
    }

    private boolean isPlantingSeedId(String itemIdLower) {
        if (itemIdLower == null || itemIdLower.isBlank()) {
            return false;
        }
        if (!itemIdLower.startsWith(SEED_PREFIX)) {
            return false;
        }
        String cropCore = this.cropCoreFromSeedId(itemIdLower);
        return cropCore != null && this.canonicalHarvestItemIdForCrop(cropCore) != null;
    }

    private String chooseSeedId(Map<String, Integer> seedCounts) {
        if (seedCounts == null || seedCounts.isEmpty()) {
            return null;
        }
        String best = null;
        for (String id : seedCounts.keySet()) {
            if (best != null && id.compareTo(best) >= 0) continue;
            best = id;
        }
        return best;
    }

    private boolean plantSeedAt(World world, NPCEntity npcEntity, int x, int y, int z, String preferredSeedId) {
        if (world == null || npcEntity == null) {
            return false;
        }
        String airAtPlant = this.normalizeBlockId(this.safeLower(WorldQueries.getBlockTypeAt(world, x, y, z)));
        if (!(airAtPlant.isBlank() || "empty".equals(airAtPlant) || "air".equals(airAtPlant))) {
            return false;
        }
        String soilBelow = this.normalizeBlockId(this.safeLower(WorldQueries.getBlockTypeAt(world, x, y - 1, z)));
        if (!this.isTilledSoil(soilBelow)) {
            return false;
        }
        String seedId = preferredSeedId;
        Map<String, Integer> seeds = this.getAvailableSeeds(npcEntity);
        if (seedId == null || !seeds.containsKey(seedId)) {
            seedId = this.chooseSeedId(seeds);
        }
        if (seedId == null) {
            return false;
        }
        String cropCore = this.cropCoreFromSeedId(seedId);
        if (cropCore.isBlank()) {
            return false;
        }
        String baseCropId = CROP_PREFIX + cropCore;
        boolean eternalSeed = this.isEternalSeedId(seedId);
        String[] candidates = this.plantingBlockCandidates(baseCropId, eternalSeed);
        boolean placed = false;
        for (String candidate : candidates) {
            boolean expectedEternal;
            if (candidate == null || candidate.isBlank() || !this.trySetBlockType(world, x, y, z, new String[]{candidate})) continue;
            String after = this.normalizeBlockId(this.safeLower(WorldQueries.getBlockTypeAt(world, x, y, z)));
            boolean expectedCrop = after.contains(baseCropId) || after.contains(CROP_PREFIX);
            boolean bl = expectedEternal = !eternalSeed || after.contains("eternal");
            if (!expectedCrop || !expectedEternal) continue;
            placed = true;
            break;
        }
        if (!placed) {
            return false;
        }
        if (!this.consumeOneSeed(npcEntity, seedId)) {
            // empty if block
        }
        return true;
    }

    private String[] plantingBlockCandidates(String baseCropId, boolean eternalSeed) {
        if (baseCropId == null || baseCropId.isBlank()) {
            return new String[0];
        }
        if (!eternalSeed) {
            return new String[]{baseCropId, "*" + baseCropId, baseCropId + "_block_state_definitions_stage0", "*" + baseCropId + "_block_state_definitions_stage0", baseCropId + "_block_state_definitions_stage1", "*" + baseCropId + "_block_state_definitions_stage1"};
        }
        return new String[]{baseCropId + "_eternal", "*" + baseCropId + "_eternal", baseCropId + "_block_eternal", "*" + baseCropId + "_block_eternal", baseCropId + "_eternal_block", "*" + baseCropId + "_eternal_block", baseCropId + "_eternal_block_state_definitions_stage0", "*" + baseCropId + "_eternal_block_state_definitions_stage0", baseCropId + "_eternal_block_state_definitions_stage1", "*" + baseCropId + "_eternal_block_state_definitions_stage1", baseCropId + "_block_eternal_state_definitions_stage0", "*" + baseCropId + "_block_eternal_state_definitions_stage0", baseCropId + "_block_eternal_state_definitions_stage1", "*" + baseCropId + "_block_eternal_state_definitions_stage1", baseCropId + "_block_state_definitions_eternal_stage0", "*" + baseCropId + "_block_state_definitions_eternal_stage0", baseCropId + "_block_state_definitions_eternal_stage1", "*" + baseCropId + "_block_state_definitions_eternal_stage1"};
    }

    private String cropCoreFromSeedId(String seedId) {
        if (seedId == null || seedId.isBlank()) {
            return "";
        }
        String low = seedId.toLowerCase(Locale.ROOT);
        if (!low.startsWith(SEED_PREFIX)) {
            return "";
        }
        String cropCore = low.substring(SEED_PREFIX.length());
        if (cropCore.endsWith("_eternal")) {
            cropCore = cropCore.substring(0, cropCore.length() - "_eternal".length());
        }
        return cropCore;
    }

    private boolean isEternalSeedId(String seedId) {
        return seedId != null && seedId.toLowerCase(Locale.ROOT).endsWith("_eternal");
    }

    private boolean waterCropAt(World world, CompanionRuntimeState runtime, int x, int y, int z) {
        if (world == null) {
            return false;
        }
        String cropRaw = this.safeLower(WorldQueries.getBlockTypeAt(world, x, y, z));
        String cropNorm = this.normalizeBlockId(cropRaw);
        if (!this.isWaterableCrop(world, x, y, z, cropNorm, cropRaw, runtime)) {
            return false;
        }
        int soilY = y - 1;
        String before = this.normalizeBlockId(this.safeLower(WorldQueries.getBlockTypeAt(world, x, soilY, z)));
        if (this.isWateredSoil(before)) {
            if (runtime != null) {
                runtime.farmWateredTargetKeys.add(this.farmTargetKey(x, y, z));
            }
            return true;
        }
        String[] candidates = new String[]{"Soil_Dirt_Tilled_State_Definitions_Watered", "*Soil_Dirt_Tilled_State_Definitions_Watered", "soil_dirt_tilled_state_definitions_watered", "*soil_dirt_tilled_state_definitions_watered"};
        boolean placed = this.trySetBlockType(world, x, soilY, z, candidates);
        if (!placed) {
            return false;
        }
        String after = this.normalizeBlockId(this.safeLower(WorldQueries.getBlockTypeAt(world, x, soilY, z)));
        boolean changed = this.isWateredSoil(after);
        if (changed && runtime != null) {
            runtime.farmWateredTargetKeys.add(this.farmTargetKey(x, y, z));
        }
        return changed;
    }

    private boolean trySetBlockType(World world, int x, int y, int z, String[] blockIds) {
        Method m2;
        if (world == null || blockIds == null || blockIds.length == 0) {
            return false;
        }
        try {
            m2 = world.getClass().getMethod("setBlock", Integer.TYPE, Integer.TYPE, Integer.TYPE, String.class);
            for (String id : blockIds) {
                if (id == null || id.isBlank()) continue;
                try {
                    m2.invoke((Object)world, x, y, z, id);
                    return true;
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
        }
        catch (NoSuchMethodException m2) {
            // empty catch block
        }
        try {
            m2 = world.getClass().getMethod("setBlockType", Integer.TYPE, Integer.TYPE, Integer.TYPE, String.class);
            for (String id : blockIds) {
                if (id == null || id.isBlank()) continue;
                try {
                    m2.invoke((Object)world, x, y, z, id);
                    return true;
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
        }
        catch (NoSuchMethodException noSuchMethodException) {
            // empty catch block
        }
        return false;
    }

    private boolean consumeOneSeed(NPCEntity npcEntity, String seedIdLower) {
        if (npcEntity == null || seedIdLower == null || seedIdLower.isBlank()) {
            return false;
        }
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return false;
            }
            CombinedItemContainer container = inventory.getCombinedHotbarFirst();
            if (container == null) {
                return false;
            }
            boolean[] removed = new boolean[]{false};
            short[] slotToWrite = new short[]{-1};
            ItemStack[] replacement = new ItemStack[]{null};
            container.forEach((slot, stack) -> {
                if (removed[0]) {
                    return;
                }
                if (stack == null || stack.isEmpty()) {
                    return;
                }
                String id = stack.getItemId();
                if (id == null) {
                    return;
                }
                if (!id.toLowerCase(Locale.ROOT).equals(seedIdLower)) {
                    return;
                }
                int qty = stack.getQuantity();
                slotToWrite[0] = slot;
                replacement[0] = qty <= 1 ? ItemStack.EMPTY : stack.withQuantity(qty - 1);
                removed[0] = true;
            });
            if (!removed[0] || slotToWrite[0] < 0 || replacement[0] == null) {
                return false;
            }
            container.setItemStackForSlot(slotToWrite[0], replacement[0]);
            return true;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private String normalizeBlockId(String rawLower) {
        if (rawLower == null || rawLower.isBlank()) {
            return "";
        }
        int idIdx = rawLower.indexOf("id=");
        if (idIdx >= 0) {
            String token;
            int start = idIdx + 3;
            int endComma = rawLower.indexOf(44, start);
            int endBrace = rawLower.indexOf(125, start);
            int end = -1;
            if (endComma >= 0 && endBrace >= 0) {
                end = Math.min(endComma, endBrace);
            } else if (endComma >= 0) {
                end = endComma;
            } else if (endBrace >= 0) {
                end = endBrace;
            }
            if (end > start && !(token = rawLower.substring(start, end).trim()).isBlank()) {
                return token;
            }
        }
        return rawLower.trim();
    }

    private Double getFacingDot2D(NPCEntity npcEntity, Vector3d targetPos, Vector3d companionPos) {
        try {
            if (npcEntity == null || targetPos == null || companionPos == null) {
                return null;
            }
            Object tf = npcEntity.getClass().getMethod("getTransform", new Class[0]).invoke((Object)npcEntity, new Object[0]);
            if (tf == null) {
                return null;
            }
            Object dirObj = tf.getClass().getMethod("getDirection", new Class[0]).invoke(tf, new Object[0]);
            if (!(dirObj instanceof Vector3d)) {
                return null;
            }
            Vector3d dir = (Vector3d)dirObj;
            double fwdX = dir.x;
            double fwdZ = dir.z;
            double fwdLen = Math.sqrt(fwdX * fwdX + fwdZ * fwdZ);
            if (fwdLen < 1.0E-4) {
                return null;
            }
            fwdX /= fwdLen;
            fwdZ /= fwdLen;
            double toX = targetPos.x - companionPos.x;
            double toZ = targetPos.z - companionPos.z;
            double toLen = Math.sqrt(toX * toX + toZ * toZ);
            if (toLen < 0.15) {
                return 1.0;
            }
            return fwdX * (toX /= toLen) + fwdZ * (toZ /= toLen);
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private BlockPos nearest(Vector3d from, List<BlockPos> positions) {
        if (from == null || positions == null || positions.isEmpty()) {
            return null;
        }
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : positions) {
            double d = from.distanceTo(new Vector3d((double)p.x + 0.5, (double)p.y + 0.5, (double)p.z + 0.5));
            if (!(d < bestDist)) continue;
            bestDist = d;
            best = p;
        }
        return best;
    }

    private BlockPos nearestUnblocked(Vector3d from, List<BlockPos> positions, CompanionRuntimeState runtime, long currentTick) {
        if (from == null || positions == null || positions.isEmpty()) {
            return null;
        }
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : positions) {
            double d;
            String key = this.farmTargetKey(p.x, p.y, p.z);
            if (runtime != null && runtime.farmBlockedTargetKey != null && key.equals(runtime.farmBlockedTargetKey) && currentTick < runtime.farmBlockedUntilTick || !((d = from.distanceTo(new Vector3d((double)p.x + 0.5, (double)p.y + 0.5, (double)p.z + 0.5))) < bestDist)) continue;
            bestDist = d;
            best = p;
        }
        return best;
    }

    private BlockPos rowOrderedTarget(List<BlockPos> positions, FarmBounds bounds, BlockPos topLeft, BlockPos bottomRight, CompanionRuntimeState runtime, long currentTick) {
        if (positions == null || positions.isEmpty() || bounds == null || topLeft == null || bottomRight == null) {
            return null;
        }
        HashMap<CallSite, BlockPos> byXZ = new HashMap<CallSite, BlockPos>();
        for (BlockPos p : positions) {
            if (p == null) continue;
            String key = p.x + "," + p.z;
            byXZ.putIfAbsent((CallSite)((Object)key), p);
        }
        int zStart = topLeft.z;
        int zEnd = bottomRight.z;
        int zStep = zStart <= zEnd ? 1 : -1;
        int xStart = topLeft.x;
        int xEnd = bottomRight.x;
        int xStep = xStart <= xEnd ? 1 : -1;
        int z = zStart;
        while (true) {
            int x = xStart;
            while (true) {
                if (x < bounds.minX || x > bounds.maxX || z < bounds.minZ || z > bounds.maxZ) {
                    if (x == xEnd) {
                        break;
                    }
                } else {
                    BlockPos p = (BlockPos)byXZ.get(x + "," + z);
                    if (p != null && !this.isBlocked(runtime, currentTick, p.x, p.y, p.z)) {
                        return p;
                    }
                    if (x == xEnd) break;
                }
                x += xStep;
            }
            if (z == zEnd) break;
            z += zStep;
        }
        return this.nearestUnblocked(new Vector3d((double)bounds.minX + 0.5, 0.0, (double)bounds.minZ + 0.5), positions, runtime, currentTick);
    }

    private boolean isBlocked(CompanionRuntimeState runtime, long currentTick, int x, int y, int z) {
        if (runtime == null || runtime.farmBlockedTargetKey == null) {
            return false;
        }
        return this.farmTargetKey(x, y, z).equals(runtime.farmBlockedTargetKey) && currentTick < runtime.farmBlockedUntilTick;
    }

    private String farmTargetKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    private void runPendingHarvestVerification(World world, CompanionRuntimeState runtime, long currentTick) {
        String id;
        if (runtime == null || runtime.farmHarvestVerifyTargetKey == null) {
            return;
        }
        if (runtime.farmHarvestVerifyTick1 > 0L && currentTick >= runtime.farmHarvestVerifyTick1) {
            id = this.normalizeBlockId(this.safeLower(WorldQueries.getBlockTypeAt(world, runtime.farmHarvestVerifyX, runtime.farmHarvestVerifyY, runtime.farmHarvestVerifyZ)));
            runtime.farmHarvestVerifyTick1 = 0L;
        }
        if (runtime.farmHarvestVerifyTick2 > 0L && currentTick >= runtime.farmHarvestVerifyTick2) {
            boolean unchanged;
            id = this.normalizeBlockId(this.safeLower(WorldQueries.getBlockTypeAt(world, runtime.farmHarvestVerifyX, runtime.farmHarvestVerifyY, runtime.farmHarvestVerifyZ)));
            String before = runtime.farmHarvestBeforeId != null ? runtime.farmHarvestBeforeId : "";
            boolean bl = unchanged = !before.isBlank() && before.equals(id);
            if (runtime.farmHarvestAttempted && unchanged) {
                runtime.farmNoHarvestUntilByTargetKey.put(runtime.farmHarvestVerifyTargetKey, currentTick + 300L);
            }
            runtime.farmHarvestVerifyTick2 = 0L;
        }
        if (runtime.farmHarvestVerifyTick1 == 0L && runtime.farmHarvestVerifyTick2 == 0L) {
            runtime.farmHarvestVerifyTargetKey = null;
            runtime.farmHarvestBeforeId = null;
            runtime.farmHarvestAttempted = false;
        }
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private int farmScanJitter(CompanionRecord companion) {
        if (companion == null || companion.uniqueId == null || companion.uniqueId.isBlank()) {
            return 0;
        }
        int h = Math.abs(companion.uniqueId.hashCode());
        return h % 41;
    }

    public int getAdjustedScanInterval(int farmLevel) {
        int levelIdx = Math.min(farmLevel - 1, ProgressionConfig.FARM_SPEED_MULT.length - 1);
        double speedMult = ProgressionConfig.FARM_SPEED_MULT[levelIdx];
        return (int)(80.0 / speedMult);
    }

    private boolean checkFarmLevelUp(CompanionRecord companion) {
        int newLevel = ProgressionConfig.farmLevelForHarvests(companion.farmHarvests);
        if (newLevel > companion.farmLevel) {
            companion.farmLevel = newLevel;
            return true;
        }
        return false;
    }

    public boolean didFarmLevelUp(CompanionRecord companion) {
        int expected = ProgressionConfig.farmLevelForHarvests(companion.farmHarvests);
        return expected > companion.farmLevel;
    }

    private static final class FarmBounds {
        final int minX;
        final int maxX;
        final int minZ;
        final int maxZ;

        private FarmBounds(int minX, int maxX, int minZ, int maxZ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        static FarmBounds from(BlockPos a, BlockPos b) {
            if (a == null || b == null) {
                return null;
            }
            return new FarmBounds(Math.min(a.x, b.x), Math.max(a.x, b.x), Math.min(a.z, b.z), Math.max(a.z, b.z));
        }

        int area() {
            return (this.maxX - this.minX + 1) * (this.maxZ - this.minZ + 1);
        }
    }
}

