/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.logger.HytaleLogger
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 *  com.hypixel.hytale.math.vector.Vector3d
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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.movement.controllers.MotionController;
import com.yourname.companion.data.CompanionMode;
import com.yourname.companion.data.CompanionRecord;
import com.yourname.companion.data.ProgressionConfig;
import com.yourname.companion.runtime.CompanionRuntimeState;
import com.yourname.companion.runtime.Vec3;
import com.yourname.companion.systems.WorkPositioning;
import com.yourname.companion.util.InvUtil;
import com.yourname.companion.util.WorldQueries;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class MinerSystem {
    private static final boolean DEBUG_MINING_TRACE = false;
    private static final boolean DEBUG_MINING_VISUAL = false;
    private static final int MINE_SCAN_INTERVAL = 40;
    private static final int MINE_RADIUS = 500;
    private static final int MINE_SCAN_MAX_BLOCK_CHECKS = 12000;
    private static final int MINE_WIDE_SCAN_STRIDE = 6;
    private static final int MINE_FOR_START_RADIUS = 24;
    private static final int MINE_FOR_MAX_RADIUS = 300;
    private static final int MINE_FOR_SCAN_MAX_BLOCK_CHECKS = 24000;
    private static final int MINE_FOR_LOCAL_PRIORITY_RADIUS = 6;
    private static final double MINE_RANGE = 0.85;
    private static final double MINE_VERTICAL_TOLERANCE = 2.5;
    private static final double MOVE_SPEED = 5.0;
    private static final int UNREACHABLE_TIMEOUT = 80;
    private static final long MINE_FOR_NO_PROGRESS_TIMEOUT_TICKS = 100L;
    private static final long MINE_FOR_BLOCKED_TARGET_COOLDOWN_TICKS = 240L;
    private static final int MINE_FOR_STUCK_DIG_THRESHOLD = 20;
    private static final double MINE_FOR_PROGRESS_EPSILON = 0.35;
    private static final long MINE_MOVE_COMMAND_INTERVAL_TICKS = 5L;
    private static final long MINE_START_MOVE_COMMAND_INTERVAL_TICKS = 10L;
    private static final long MINE_ATTEMPT_COOLDOWN_TICKS = 12L;
    private static final long MINE_VISUAL_LEAD_TICKS = 5L;
    private static final int MINE_EMPTY_STOP_CONSECUTIVE = 15;
    private static final double MINE_LANE_TURN_CENTER_REACH = 0.3;
    private final HytaleLogger logger;
    private boolean miningVisualMethodSnapshotLogged;
    private boolean miningVisualSuccessLogged;

    public MinerSystem(HytaleLogger logger) {
        this.logger = logger;
    }

    public boolean evaluateMine(World world, CompanionRecord companion, CompanionRuntimeState runtime, Vector3d companionPos, long currentTick) {
        if (companion.mode != CompanionMode.MINER) {
            return false;
        }
        if (!runtime.miningActive) {
            return false;
        }
        if (runtime.mineMoveToAnchorPending) {
            return true;
        }
        if (!runtime.mineForSpecific) {
            return true;
        }
        if (runtime.mineTargetPos != null) {
            return true;
        }
        if (currentTick - runtime.lastMineScanTick < 40L) {
            return false;
        }
        runtime.lastMineScanTick = currentTick;
        if (runtime.mineTargetBlockId != null) {
            int radius = runtime.mineSearchRadius > 0 ? runtime.mineSearchRadius : 24;
            Vec3 found = this.scanForBlockType(world, companionPos, runtime.mineTargetBlockId, Math.min(radius, 6), runtime);
            if (found == null) {
                Vector3d scanOrigin = companionPos;
                if (runtime.mineLastMinedPos != null) {
                    scanOrigin = new Vector3d(runtime.mineLastMinedPos.x, runtime.mineLastMinedPos.y, runtime.mineLastMinedPos.z);
                }
                found = this.scanForBlockType(world, scanOrigin, runtime.mineTargetBlockId, radius, runtime);
            }
            if (found != null) {
                runtime.mineTargetPos = found;
                runtime.mineUnreachableTicks = 0;
                runtime.mineLastTargetDistance = -1.0;
                runtime.mineLastForwardProgressTick = runtime.currentTick;
                runtime.mineSearchRadius = 24;
                runtime.mineStatusText = "Moving to matching block" + (String)(runtime.mineTargetPriority != null ? " (" + runtime.mineTargetPriority + ")" : "");
                return true;
            }
            if (radius >= 300) {
                this.stopMineForNoMatchingBlocks(runtime);
                return false;
            }
            runtime.mineSearchRadius = this.nextMineForSearchRadius(radius);
            runtime.mineTargetPos = null;
            runtime.mineTargetPriority = null;
            runtime.mineStatusText = "Searching wider area";
        }
        return false;
    }

    public void executeMineSeek(World world, Store<EntityStore> store, NPCEntity npcEntity, Ref<EntityStore> companionRef, CompanionRecord companion, CompanionRuntimeState runtime, Vector3d companionPos) {
        boolean noForwardProgress;
        if (runtime.mineBoundaryTurnPhase == 2 && runtime.mineAnchorPos != null) {
            int anchorZ;
            Vector3d anchor = new Vector3d(runtime.mineAnchorPos.x, runtime.mineAnchorPos.y, runtime.mineAnchorPos.z);
            double dx = anchor.x - companionPos.x;
            double dz = anchor.z - companionPos.z;
            double h = Math.sqrt(dx * dx + dz * dz);
            double v = Math.abs(anchor.y - companionPos.y);
            int anchorX = (int)Math.floor(anchor.x);
            if ((this.isInHorizontalBlock(companionPos, anchorX, anchorZ = (int)Math.floor(anchor.z)) || h <= 0.3) && v <= 2.5) {
                runtime.mineAnchorPos = null;
                runtime.mineMoveToAnchorPending = false;
                this.finishBoundaryTurn(runtime);
                return;
            }
            this.moveToward(companion, runtime, npcEntity, companionPos, anchor);
            runtime.mineStatusText = "Moving into next tunnel lane";
            return;
        }
        if (runtime.mineMoveToAnchorPending && runtime.mineAnchorPos != null) {
            Vector3d anchor = new Vector3d(runtime.mineAnchorPos.x, runtime.mineAnchorPos.y, runtime.mineAnchorPos.z);
            double dx = anchor.x - companionPos.x;
            double dz = anchor.z - companionPos.z;
            double h = Math.sqrt(dx * dx + dz * dz);
            double v = Math.abs(anchor.y - companionPos.y);
            if (h <= 0.25 && v <= 2.5) {
                this.snapToMineAnchor(store, companionRef, npcEntity, runtime, anchor);
                runtime.mineMoveToAnchorPending = false;
                runtime.mineTargetPos = null;
                runtime.mineStatusText = "Aligned to mine start";
            } else {
                if (runtime.currentTick - runtime.lastMineMoveTick >= 10L) {
                    this.moveToward(companion, runtime, npcEntity, companionPos, anchor);
                }
                runtime.mineStatusText = "Moving to mine start position";
                return;
            }
        }
        if (!runtime.mineForSpecific) {
            this.executeDirectionalMine(world, store, companionRef, npcEntity, companion, runtime, companionPos);
            return;
        }
        if (runtime.mineTargetPos == null) {
            return;
        }
        Vector3d targetPos = new Vector3d(runtime.mineTargetPos.x, runtime.mineTargetPos.y, runtime.mineTargetPos.z);
        int bx = (int)Math.floor(runtime.mineTargetPos.x);
        int by = (int)Math.floor(runtime.mineTargetPos.y);
        int bz = (int)Math.floor(runtime.mineTargetPos.z);
        double horizontal = this.horizontalDistanceToBlockXZ(companionPos, bx, bz);
        double vertical = Math.abs(targetPos.y - companionPos.y);
        if (horizontal <= 0.85 && vertical <= 2.5) {
            String minedBlockRaw;
            try {
                MotionController mc = npcEntity.getRole().getActiveMotionController();
                if (mc != null) {
                    mc.forceVelocity(new Vector3d(0.0, 0.0, 0.0), null, false);
                }
            }
            catch (Throwable mc) {
                // empty catch block
            }
            if (runtime.minePendingBreakPos != null) {
                int pbx = (int)Math.floor(runtime.minePendingBreakPos.x);
                int pby = (int)Math.floor(runtime.minePendingBreakPos.y);
                int pbz = (int)Math.floor(runtime.minePendingBreakPos.z);
                if (pbx != bx || pby != by || pbz != bz) {
                    runtime.minePendingBreakPos = null;
                    runtime.minePendingBreakBlockId = null;
                }
            }
            if (runtime.currentTick < runtime.nextMineAttemptTick) {
                return;
            }
            String string = minedBlockRaw = runtime.minePendingBreakBlockId != null ? runtime.minePendingBreakBlockId : WorldQueries.getBlockTypeAt(world, bx, by, bz);
            if (runtime.minePendingBreakPos == null) {
                this.tryTriggerMiningVisual(npcEntity, companionRef, store);
                runtime.minePendingBreakPos = new Vec3((double)bx + 0.5, by, (double)bz + 0.5);
                runtime.minePendingBreakBlockId = minedBlockRaw;
                runtime.mineStatusText = "Swinging pickaxe...";
                runtime.nextMineAttemptTick = runtime.currentTick + 5L;
                return;
            }
            boolean mined = WorldQueries.breakBlock(world, bx, by, bz);
            runtime.nextMineAttemptTick = runtime.currentTick + this.mineAttemptCooldownTicksForLevel(companion.mineLevel);
            runtime.minePendingBreakPos = null;
            runtime.minePendingBreakBlockId = null;
            if (mined) {
                ++companion.mineBlocks;
                this.checkMineLevelUp(companion);
                boolean rewarded = this.grantMiningYieldToCompanion(npcEntity, companionRef, store, runtime, minedBlockRaw);
                runtime.mineLastMinedPos = new Vec3((double)bx + 0.5, by, (double)bz + 0.5);
                if (!rewarded) {
                    this.handleInventoryFullWhileMining(companion, runtime);
                    return;
                }
                runtime.mineStatusText = "Mining block...";
            } else {
                runtime.mineStatusText = "Mine attempt failed; retrying";
            }
            runtime.mineTargetPos = null;
            runtime.mineTargetPriority = null;
            this.markBlockedMineForTarget(runtime, targetPos);
            runtime.mineLastTargetDistance = -1.0;
            runtime.mineLastForwardProgressTick = 0L;
            runtime.mineUnreachableTicks = 0;
            runtime.lastMineScanTick = runtime.currentTick - 40L;
            return;
        }
        if (runtime.mineForSpecific) {
            boolean dugOut;
            if (runtime.stuckTicks >= 20 && (dugOut = this.tryDigOutTowardMineTarget(world, store, companionRef, npcEntity, companion, runtime, companionPos, targetPos))) {
                return;
            }
            boolean cleared = this.clearPathTowardTarget(world, store, companionRef, npcEntity, companion, runtime, companionPos, targetPos);
            if (cleared) {
                return;
            }
        }
        this.moveToward(companion, runtime, npcEntity, companionPos, targetPos);
        runtime.mineStatusText = "Moving to target";
        runtime.mineUnreachableTicks += 5;
        double distToTarget = companionPos.distanceTo(targetPos);
        if (runtime.mineLastTargetDistance < 0.0 || distToTarget <= runtime.mineLastTargetDistance - 0.35) {
            runtime.mineLastTargetDistance = distToTarget;
            runtime.mineLastForwardProgressTick = runtime.currentTick;
        }
        if (runtime.mineLastForwardProgressTick <= 0L) {
            runtime.mineLastForwardProgressTick = runtime.currentTick;
        }
        boolean bl = noForwardProgress = runtime.currentTick - runtime.mineLastForwardProgressTick >= 100L;
        if (runtime.mineUnreachableTicks >= 80 || noForwardProgress) {
            runtime.mineTargetPos = null;
            runtime.mineTargetPriority = null;
            runtime.mineLastTargetDistance = -1.0;
            runtime.mineLastForwardProgressTick = 0L;
            runtime.mineUnreachableTicks = 0;
            runtime.mineStatusText = noForwardProgress ? "No forward progress; switching target" : "Target unreachable; retrying";
            runtime.lastMineScanTick = runtime.currentTick - 40L;
        }
    }

    private double horizontalDistanceToBlockXZ(Vector3d pos, int blockX, int blockZ) {
        double nearestX = Math.max((double)blockX, Math.min(pos.x, (double)blockX + 1.0));
        double nearestZ = Math.max((double)blockZ, Math.min(pos.z, (double)blockZ + 1.0));
        double dx = pos.x - nearestX;
        double dz = pos.z - nearestZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void executeDirectionalMine(World world, Store<EntityStore> store, Ref<EntityStore> companionRef, NPCEntity npcEntity, CompanionRecord companion, CompanionRuntimeState runtime, Vector3d companionPos) {
        int forwardBaseY;
        int baseY;
        int sx = runtime.mineStepX;
        int sz = runtime.mineStepZ;
        if (sx == 0 && sz == 0) {
            runtime.mineStatusText = "No mining direction";
            return;
        }
        if (runtime.mineBoundaryTurnPhase == 1 && (sx != runtime.mineStepX || sz != runtime.mineStepZ)) {
            runtime.mineBoundaryTurnPhase = 0;
        }
        int standX = (int)Math.floor(companionPos.x);
        int standZ = (int)Math.floor(companionPos.z);
        runtime.mineClearanceY = baseY = this.resolveLegYFromSupport(world, standX, standZ, companionPos);
        int legY = baseY;
        int frontX = standX + sx;
        int frontZ = standZ + sz;
        int headY = legY + 1;
        boolean legMineable = this.isMineableBlock(world, frontX, legY, frontZ);
        boolean headMineable = this.isMineableBlock(world, frontX, headY, frontZ);
        if (!legMineable && !headMineable && Math.abs((forwardBaseY = this.resolveLegYFromSupport(world, frontX, frontZ, new Vector3d((double)frontX + 0.5, companionPos.y - 1.0, (double)frontZ + 0.5))) - baseY) <= 1) {
            legY = forwardBaseY;
            headY = legY + 1;
            legMineable = this.isMineableBlock(world, frontX, legY, frontZ);
            headMineable = this.isMineableBlock(world, frontX, headY, frontZ);
        }
        if (legMineable || headMineable) {
            String minedBlockRaw;
            int by;
            boolean preferHead;
            boolean bl = preferHead = runtime.mineStepY > 0;
            if (legMineable && headMineable) {
                by = preferHead ? headY : legY;
                minedBlockRaw = preferHead ? WorldQueries.getBlockTypeAt(world, frontX, headY, frontZ) : WorldQueries.getBlockTypeAt(world, frontX, legY, frontZ);
            } else if (headMineable) {
                by = headY;
                minedBlockRaw = WorldQueries.getBlockTypeAt(world, frontX, headY, frontZ);
            } else {
                by = legY;
                minedBlockRaw = WorldQueries.getBlockTypeAt(world, frontX, legY, frontZ);
            }
            if (runtime.currentTick < runtime.nextMineAttemptTick) {
                runtime.mineStatusText = "Adjusting position for mine";
                return;
            }
            boolean mined = WorldQueries.breakBlock(world, frontX, by, frontZ);
            runtime.nextMineAttemptTick = runtime.currentTick + this.mineAttemptCooldownTicksForLevel(companion.mineLevel);
            if (mined) {
                ++companion.mineBlocks;
                this.checkMineLevelUp(companion);
                boolean rewarded = this.grantMiningYieldToCompanion(npcEntity, companionRef, store, runtime, minedBlockRaw);
                runtime.mineLastMinedPos = new Vec3((double)frontX + 0.5, by, (double)frontZ + 0.5);
                if (runtime.mineBoundaryTurnPhase != 1) {
                    this.clearReturnLaneShiftState(runtime);
                } else if (!this.hasRemainingTurnColumnBlock(by, legY, headY, legMineable, headMineable)) {
                    runtime.mineBoundaryTurnPhase = 2;
                    runtime.mineAnchorPos = new Vec3((double)frontX + 0.5, legY, (double)frontZ + 0.5);
                    runtime.mineMoveToAnchorPending = false;
                    runtime.mineTargetPos = null;
                    runtime.mineTargetPriority = null;
                    runtime.minePendingBreakPos = null;
                    runtime.minePendingBreakBlockId = null;
                    runtime.mineUnreachableTicks = 0;
                    runtime.mineStatusText = "Moving into next tunnel lane";
                }
                if (!rewarded) {
                    this.handleInventoryFullWhileMining(companion, runtime);
                    return;
                }
                runtime.mineStatusText = "Mining block...";
            } else {
                runtime.mineStatusText = "Mine attempt failed; retrying";
            }
            return;
        }
        if (runtime.mineBoundaryTurnPhase == 1) {
            Vector3d turnTarget = new Vector3d((double)frontX + 0.5, (double)legY, (double)frontZ + 0.5);
            double turnDx = turnTarget.x - companionPos.x;
            double turnDz = turnTarget.z - companionPos.z;
            double turnH = Math.sqrt(turnDx * turnDx + turnDz * turnDz);
            if (this.isInHorizontalBlock(companionPos, frontX, frontZ) || turnH <= 0.3) {
                this.finishBoundaryTurn(runtime);
                return;
            }
            this.moveToward(companion, runtime, npcEntity, companionPos, turnTarget);
            runtime.mineStatusText = "Moving into next tunnel lane";
            return;
        }
        boolean emptyAhead = this.hasConsecutiveAirTunnelAhead(world, standX, standZ, legY, headY, sx, sz, 15);
        if (emptyAhead && runtime.mineStepY > 0) {
            emptyAhead = this.hasConsecutiveAirTunnelAhead(world, standX, standZ, legY - 1, legY, sx, sz, 15);
        }
        if (emptyAhead) {
            if (!runtime.mineForSpecific && runtime.miningActive && runtime.mineBoundaryTurnPhase == 0 && this.beginBoundaryTurn(runtime)) {
                return;
            }
            if (this.tryOppositeReturnLaneAfterEmptyTunnel(runtime)) {
                return;
            }
            runtime.miningActive = false;
            runtime.mineForSpecific = false;
            runtime.mineTargetPos = null;
            runtime.mineTargetBlockId = null;
            runtime.mineMoveToAnchorPending = false;
            runtime.commandActive = false;
            runtime.mineReturnToLoadedArea = true;
            this.clearReturnLaneShiftState(runtime);
            runtime.mineStatusText = "Stopped: empty tunnel ahead";
            return;
        }
        Vector3d moveTarget = new Vector3d((double)frontX + 0.5, (double)legY, (double)frontZ + 0.5);
        double dx = moveTarget.x - companionPos.x;
        double dz = moveTarget.z - companionPos.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        if (h <= 0.15) {
            runtime.mineStatusText = "Advancing tunnel";
            return;
        }
        this.moveToward(companion, runtime, npcEntity, companionPos, moveTarget);
        runtime.mineStatusText = "Moving to target";
    }

    private boolean hasRemainingTurnColumnBlock(int minedY, int legY, int headY, boolean legMineable, boolean headMineable) {
        return minedY == legY && headMineable || minedY == headY && legMineable;
    }

    private boolean beginBoundaryTurn(CompanionRuntimeState runtime) {
        if (runtime == null) {
            return false;
        }
        int sx = runtime.mineStepX;
        int sz = runtime.mineStepZ;
        if (sx == 0 && sz == 0) {
            return false;
        }
        int sideSign = runtime.mineBoundaryTurnNextSide == 0 ? 1 : runtime.mineBoundaryTurnNextSide;
        int sideX = sideSign * -sz;
        int sideZ = sideSign * sx;
        if (sideX == 0 && sideZ == 0) {
            return false;
        }
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
        return true;
    }

    private boolean isInHorizontalBlock(Vector3d pos, int blockX, int blockZ) {
        if (pos == null) {
            return false;
        }
        return (int)Math.floor(pos.x) == blockX && (int)Math.floor(pos.z) == blockZ;
    }

    private boolean tryOppositeReturnLaneAfterEmptyTunnel(CompanionRuntimeState runtime) {
        if (runtime == null) {
            return false;
        }
        if (runtime.mineShiftLaneAfterReturnAttempt != 1 || runtime.mineShiftLaneAfterReturnOrigin == null) {
            return false;
        }
        if (!runtime.miningActive || runtime.mineForSpecific) {
            this.clearReturnLaneShiftState(runtime);
            return false;
        }
        int sx = runtime.mineStepX;
        int sz = runtime.mineStepZ;
        int sideX = -sz;
        int sideZ = sx;
        if (sideX == 0 && sideZ == 0) {
            this.clearReturnLaneShiftState(runtime);
            return false;
        }
        runtime.mineShiftLaneAfterReturnAttempt = 2;
        runtime.mineTargetPos = null;
        runtime.mineTargetPriority = null;
        runtime.minePendingBreakPos = null;
        runtime.minePendingBreakBlockId = null;
        runtime.mineLastTargetDistance = -1.0;
        runtime.mineLastForwardProgressTick = runtime.currentTick;
        runtime.mineUnreachableTicks = 0;
        runtime.mineEmptyForwardConsecutive = 0;
        runtime.mineMoveToAnchorPending = true;
        runtime.mineAnchorPos = new Vec3(runtime.mineShiftLaneAfterReturnOrigin.x - (double)sideX, runtime.mineShiftLaneAfterReturnOrigin.y, runtime.mineShiftLaneAfterReturnOrigin.z - (double)sideZ);
        runtime.mineStatusText = "Trying opposite tunnel lane";
        return true;
    }

    private void finishBoundaryTurn(CompanionRuntimeState runtime) {
        if (runtime == null) {
            return;
        }
        int originalX = runtime.mineBoundaryTurnOriginalStepX;
        int originalZ = runtime.mineBoundaryTurnOriginalStepZ;
        if (originalX != 0 || originalZ != 0) {
            runtime.mineStepX = -originalX;
            runtime.mineStepZ = -originalZ;
        }
        runtime.mineBoundaryTurnPhase = 0;
        runtime.mineBoundaryTurnOriginalStepX = 0;
        runtime.mineBoundaryTurnOriginalStepZ = 0;
        runtime.mineBoundaryTurnNextSide = runtime.mineBoundaryTurnNextSide == 0 ? -1 : -runtime.mineBoundaryTurnNextSide;
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
        runtime.mineStatusText = "Mining back on next lane";
    }

    private void clearReturnLaneShiftState(CompanionRuntimeState runtime) {
        if (runtime == null) {
            return;
        }
        runtime.mineShiftLaneAfterReturn = false;
        runtime.mineShiftLaneAfterReturnAttempt = 0;
        runtime.mineShiftLaneAfterReturnOrigin = null;
        runtime.mineBoundaryTurnPhase = 0;
        runtime.mineBoundaryTurnOriginalStepX = 0;
        runtime.mineBoundaryTurnOriginalStepZ = 0;
    }

    private void moveToward(CompanionRecord companion, CompanionRuntimeState runtime, NPCEntity npcEntity, Vector3d from, Vector3d to) {
        if (runtime.currentTick - runtime.lastMineMoveTick < 5L) {
            return;
        }
        runtime.lastMineMoveTick = runtime.currentTick;
        if (WorkPositioning.setManagedWorkPosition(npcEntity, to, this.logger)) {
            return;
        }
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len <= 0.01) {
            return;
        }
        try {
            double moveMult = 1.0;
            if (companion != null) {
                int idx = Math.max(0, Math.min(companion.mineLevel - 1, ProgressionConfig.MINE_MOVE_SPEED_MULT.length - 1));
                moveMult = ProgressionConfig.MINE_MOVE_SPEED_MULT[idx];
            }
            double speed = 5.0 * moveMult;
            Vector3d velocity = new Vector3d(dx / len * speed, 0.0, dz / len * speed);
            MotionController mc = npcEntity.getRole().getActiveMotionController();
            if (mc != null) {
                mc.forceVelocity(velocity, null, false);
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to move companion to mine target.");
        }
    }

    private void snapToMineAnchor(Store<EntityStore> store, Ref<EntityStore> companionRef, NPCEntity npcEntity, CompanionRuntimeState runtime, Vector3d anchor) {
        if (anchor == null) {
            return;
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
        if (runtime != null) {
            runtime.lastPosition = new Vec3(anchor.x, anchor.y, anchor.z);
            runtime.stuckTicks = 0;
        }
    }

    private int resolveLegYFromSupport(World world, int standX, int standZ, Vector3d companionPos) {
        int floorY = (int)Math.floor(companionPos.y);
        int minY = floorY - 4;
        int maxY = floorY + 1;
        int supportY = Integer.MIN_VALUE;
        for (int y = maxY; y >= minY; --y) {
            String id = WorldQueries.getBlockTypeAt(world, standX, y, standZ);
            if (!this.isMineableId(id)) continue;
            supportY = y;
            break;
        }
        if (supportY != Integer.MIN_VALUE) {
            return supportY + 1;
        }
        return floorY - 1;
    }

    private boolean isMineableBlock(World world, int x, int y, int z) {
        return this.isMineableId(WorldQueries.getBlockTypeAt(world, x, y, z));
    }

    private boolean isMineableId(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        String v = id.toLowerCase(Locale.ROOT).trim();
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

    private boolean isAirLikeId(String id) {
        if (id == null || id.isBlank()) {
            return true;
        }
        String v = id.toLowerCase(Locale.ROOT).trim();
        if (v.startsWith("*")) {
            v = v.substring(1);
        }
        if (v.equals("air") || v.equals("empty")) {
            return true;
        }
        if (v.endsWith(":air") || v.endsWith(":empty")) {
            return true;
        }
        if (v.contains("id=empty")) {
            return true;
        }
        if (v.contains("group='air'")) {
            return true;
        }
        if (v.contains("drawtype=empty")) {
            return true;
        }
        if (v.contains("material=empty")) {
            return true;
        }
        return v.contains(" blockid:0") || v.startsWith("blockid:0");
    }

    private boolean hasConsecutiveAirTunnelAhead(World world, int fromX, int fromZ, int legY, int headY, int stepX, int stepZ, int requiredConsecutive) {
        if (world == null || requiredConsecutive <= 0) {
            return false;
        }
        for (int step = 1; step <= requiredConsecutive; ++step) {
            int tx = fromX + stepX * step;
            int tz = fromZ + stepZ * step;
            String legId = WorldQueries.getBlockTypeAt(world, tx, legY, tz);
            String headId = WorldQueries.getBlockTypeAt(world, tx, headY, tz);
            if (this.isAirLikeId(legId) && this.isAirLikeId(headId)) continue;
            return false;
        }
        return true;
    }

    private Vec3 scanForBlockType(World world, Vector3d center, String targetBlockType, int radius, CompanionRuntimeState runtime) {
        String bestPriority;
        String canonicalTarget = this.canonicalBlockId(targetBlockType);
        if (canonicalTarget == null || canonicalTarget.isBlank()) {
            return null;
        }
        this.pruneExpiredBlockedMineForTargets(runtime);
        int cx = (int)Math.floor(center.x);
        int cy = (int)Math.floor(center.y);
        int cz = (int)Math.floor(center.z);
        int standY = cy;
        double bestPreferredScore = Double.MAX_VALUE;
        Vec3 bestPreferredPos = null;
        String bestPreferredPriority = null;
        double bestFallbackScore = Double.MAX_VALUE;
        Vec3 bestFallbackPos = null;
        String bestFallbackPriority = null;
        int checks = 0;
        int stride = 1;
        for (int dx = -radius; dx <= radius; dx += stride) {
            for (int dz = -radius; dz <= radius; dz += stride) {
                for (int dy : new int[]{0, 1, -1, 2, -2, 3, -3}) {
                    if (++checks > 24000) {
                        String partialBestPriority;
                        Vec3 partialBestPos = bestPreferredPos != null ? bestPreferredPos : bestFallbackPos;
                        String string = partialBestPriority = bestPreferredPos != null ? bestPreferredPriority : bestFallbackPriority;
                        if (runtime != null) {
                            runtime.mineTargetPriority = partialBestPriority;
                        }
                        return partialBestPos;
                    }
                    int bx = cx + dx;
                    int by = cy + dy;
                    int bz = cz + dz;
                    String blockType = this.canonicalBlockId(WorldQueries.getBlockTypeAt(world, bx, by, bz));
                    if (blockType == null || !blockType.equalsIgnoreCase(canonicalTarget) || !this.isReachableMineForCandidate(standY, by) || this.isBlockedMineForCandidate(runtime, bx, by, bz)) continue;
                    double score = this.scoreMineForCandidate(world, center, bx, by, bz, standY);
                    String priority = this.classifyMineForCandidatePriority(standY, by);
                    if (this.isPreferredMineForCandidate(standY, by)) {
                        if (!(score < bestPreferredScore)) continue;
                        bestPreferredScore = score;
                        bestPreferredPos = new Vec3((double)bx + 0.5, (double)by + 0.5, (double)bz + 0.5);
                        bestPreferredPriority = priority;
                        continue;
                    }
                    if (!(score < bestFallbackScore)) continue;
                    bestFallbackScore = score;
                    bestFallbackPos = new Vec3((double)bx + 0.5, (double)by + 0.5, (double)bz + 0.5);
                    bestFallbackPriority = priority;
                }
            }
        }
        Vec3 bestPos = bestPreferredPos != null ? bestPreferredPos : bestFallbackPos;
        String string = bestPriority = bestPreferredPos != null ? bestPreferredPriority : bestFallbackPriority;
        if (runtime != null) {
            runtime.mineTargetPriority = bestPriority;
        }
        return bestPos;
    }

    private int nextMineForSearchRadius(int currentRadius) {
        if (currentRadius < 48) {
            return 48;
        }
        if (currentRadius < 96) {
            return 96;
        }
        if (currentRadius < 160) {
            return 160;
        }
        if (currentRadius < 220) {
            return 220;
        }
        return 300;
    }

    private boolean isReachableMineForCandidate(int standY, int blockY) {
        int dy = blockY - standY;
        return dy <= 1;
    }

    private boolean isPreferredMineForCandidate(int standY, int blockY) {
        int dy = blockY - standY;
        return dy >= 0 && dy <= 1;
    }

    private boolean isBlockedMineForCandidate(CompanionRuntimeState runtime, int bx, int by, int bz) {
        if (runtime == null || runtime.mineBlockedTargetUntilByKey.isEmpty()) {
            return false;
        }
        Long untilTick = runtime.mineBlockedTargetUntilByKey.get(this.mineForTargetKey(bx, by, bz));
        return untilTick != null && runtime.currentTick < untilTick;
    }

    private void markBlockedMineForTarget(CompanionRuntimeState runtime, Vector3d targetPos) {
        if (runtime == null || targetPos == null) {
            return;
        }
        int bx = (int)Math.floor(targetPos.x);
        int by = (int)Math.floor(targetPos.y);
        int bz = (int)Math.floor(targetPos.z);
        runtime.mineBlockedTargetUntilByKey.put(this.mineForTargetKey(bx, by, bz), runtime.currentTick + 240L);
    }

    private void pruneExpiredBlockedMineForTargets(CompanionRuntimeState runtime) {
        if (runtime == null || runtime.mineBlockedTargetUntilByKey.isEmpty()) {
            return;
        }
        runtime.mineBlockedTargetUntilByKey.entrySet().removeIf(entry -> entry.getValue() == null || runtime.currentTick >= (Long)entry.getValue());
    }

    private String mineForTargetKey(int bx, int by, int bz) {
        return bx + ":" + by + ":" + bz;
    }

    private double scoreMineForCandidate(World world, Vector3d center, int bx, int by, int bz, int standY) {
        Vector3d blockCenter = new Vector3d((double)bx + 0.5, (double)by + 0.5, (double)bz + 0.5);
        double horizontal = Math.sqrt((blockCenter.x - center.x) * (blockCenter.x - center.x) + (blockCenter.z - center.z) * (blockCenter.z - center.z));
        int dy = by - standY;
        double verticalPenalty = dy == 0 ? 0.0 : (dy == 1 ? 4.0 : (dy > 1 ? 12.0 + (double)dy * 4.0 : 30.0 + (double)Math.abs(dy) * 8.0));
        double occupancyPenalty = this.hasAirAccessForMineFor(world, bx, by, bz) ? 0.0 : 10.0;
        double immediateBias = horizontal <= 2.25 ? -2.0 : 0.0;
        return horizontal + verticalPenalty + occupancyPenalty + immediateBias;
    }

    private String classifyMineForCandidatePriority(int standY, int blockY) {
        int dy = blockY - standY;
        if (dy == 0) {
            return "same-level";
        }
        if (dy == 1) {
            return "plus-one";
        }
        if (dy > 1) {
            return "above-fallback";
        }
        return "below-fallback";
    }

    private boolean hasAirAccessForMineFor(World world, int x, int y, int z) {
        return this.isAirLikeId(WorldQueries.getBlockTypeAt(world, x, y + 1, z)) || this.isAirLikeId(WorldQueries.getBlockTypeAt(world, x, y + 2, z)) || this.isAirLikeId(WorldQueries.getBlockTypeAt(world, x, y, z + 1)) || this.isAirLikeId(WorldQueries.getBlockTypeAt(world, x, y, z - 1)) || this.isAirLikeId(WorldQueries.getBlockTypeAt(world, x + 1, y, z)) || this.isAirLikeId(WorldQueries.getBlockTypeAt(world, x - 1, y, z));
    }

    private boolean clearPathTowardTarget(World world, Store<EntityStore> store, Ref<EntityStore> companionRef, NPCEntity npcEntity, CompanionRecord companion, CompanionRuntimeState runtime, Vector3d companionPos, Vector3d targetPos) {
        String minedBlockRaw;
        if (world == null || runtime == null || companionPos == null || targetPos == null) {
            return false;
        }
        if (runtime.currentTick < runtime.nextMineAttemptTick) {
            return false;
        }
        double dx = targetPos.x - companionPos.x;
        double dz = targetPos.z - companionPos.z;
        if (Math.abs(dx) < 0.01 && Math.abs(dz) < 0.01) {
            return false;
        }
        int stepX = 0;
        int stepZ = 0;
        if (Math.abs(dx) >= Math.abs(dz)) {
            stepX = dx >= 0.0 ? 1 : -1;
        } else {
            stepZ = dz >= 0.0 ? 1 : -1;
        }
        int standX = (int)Math.floor(companionPos.x);
        int standZ = (int)Math.floor(companionPos.z);
        int frontX = standX + stepX;
        int frontZ = standZ + stepZ;
        int legY = this.resolveLegYFromSupport(world, standX, standZ, companionPos);
        int headY = legY + 1;
        int mineY = this.choosePathClearY(world, targetPos, frontX, frontZ, legY, headY);
        if (mineY == Integer.MIN_VALUE) {
            return false;
        }
        if (runtime.minePendingBreakPos != null) {
            int pbx = (int)Math.floor(runtime.minePendingBreakPos.x);
            int pby = (int)Math.floor(runtime.minePendingBreakPos.y);
            int pbz = (int)Math.floor(runtime.minePendingBreakPos.z);
            if (pbx != frontX || pby != mineY || pbz != frontZ) {
                runtime.minePendingBreakPos = null;
                runtime.minePendingBreakBlockId = null;
            }
        }
        String string = minedBlockRaw = runtime.minePendingBreakBlockId != null ? runtime.minePendingBreakBlockId : WorldQueries.getBlockTypeAt(world, frontX, mineY, frontZ);
        if (runtime.minePendingBreakPos == null) {
            this.tryTriggerMiningVisual(npcEntity, companionRef, store);
            runtime.minePendingBreakPos = new Vec3((double)frontX + 0.5, mineY, (double)frontZ + 0.5);
            runtime.minePendingBreakBlockId = minedBlockRaw;
            runtime.mineStatusText = "Swinging pickaxe...";
            runtime.nextMineAttemptTick = runtime.currentTick + 5L;
            return true;
        }
        boolean mined = WorldQueries.breakBlock(world, frontX, mineY, frontZ);
        runtime.nextMineAttemptTick = runtime.currentTick + this.mineAttemptCooldownTicksForLevel(companion.mineLevel);
        runtime.minePendingBreakPos = null;
        runtime.minePendingBreakBlockId = null;
        if (!mined) {
            runtime.mineStatusText = "Clearing path failed; retrying";
            return true;
        }
        ++companion.mineBlocks;
        this.checkMineLevelUp(companion);
        boolean rewarded = this.grantMiningYieldToCompanion(npcEntity, companionRef, store, runtime, minedBlockRaw);
        runtime.mineLastMinedPos = new Vec3((double)frontX + 0.5, mineY, (double)frontZ + 0.5);
        if (!rewarded) {
            this.handleInventoryFullWhileMining(companion, runtime);
            return true;
        }
        runtime.mineStatusText = "Clearing path to target";
        return true;
    }

    private boolean tryDigOutTowardMineTarget(World world, Store<EntityStore> store, Ref<EntityStore> companionRef, NPCEntity npcEntity, CompanionRecord companion, CompanionRuntimeState runtime, Vector3d companionPos, Vector3d targetPos) {
        if (world == null || store == null || companionRef == null || npcEntity == null || runtime == null || companionPos == null || targetPos == null) {
            return false;
        }
        if (runtime.currentTick < runtime.nextMineAttemptTick) {
            return false;
        }
        double dx = targetPos.x - companionPos.x;
        double dz = targetPos.z - companionPos.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len <= 0.05) {
            return false;
        }
        int stepX = 0;
        int stepZ = 0;
        if (Math.abs(dx) >= Math.abs(dz)) {
            stepX = dx >= 0.0 ? 1 : -1;
        } else {
            int n = stepZ = dz >= 0.0 ? 1 : -1;
        }
        if (stepX == 0 && stepZ == 0) {
            return false;
        }
        int standX = (int)Math.floor(companionPos.x);
        int standZ = (int)Math.floor(companionPos.z);
        int legY = this.resolveLegYFromSupport(world, standX, standZ, companionPos);
        int headY = legY + 1;
        int frontX = standX + stepX;
        int frontZ = standZ + stepZ;
        return this.breakMineForStuckObstacle(world, store, companionRef, npcEntity, companion, runtime, standX, headY, standZ) || this.breakMineForStuckObstacle(world, store, companionRef, npcEntity, companion, runtime, standX, headY + 1, standZ) || this.breakMineForStuckObstacle(world, store, companionRef, npcEntity, companion, runtime, frontX, headY, frontZ) || this.breakMineForStuckObstacle(world, store, companionRef, npcEntity, companion, runtime, frontX, legY, frontZ);
    }

    private boolean breakMineForStuckObstacle(World world, Store<EntityStore> store, Ref<EntityStore> companionRef, NPCEntity npcEntity, CompanionRecord companion, CompanionRuntimeState runtime, int x, int y, int z) {
        String minedBlockRaw = WorldQueries.getBlockTypeAt(world, x, y, z);
        if (!this.isMineableId(minedBlockRaw)) {
            return false;
        }
        boolean mined = WorldQueries.breakBlock(world, x, y, z);
        runtime.nextMineAttemptTick = runtime.currentTick + this.mineAttemptCooldownTicksForLevel(companion.mineLevel);
        if (!mined) {
            return false;
        }
        ++companion.mineBlocks;
        this.checkMineLevelUp(companion);
        boolean rewarded = this.grantMiningYieldToCompanion(npcEntity, companionRef, store, runtime, minedBlockRaw);
        runtime.mineLastMinedPos = new Vec3((double)x + 0.5, y, (double)z + 0.5);
        runtime.mineStatusText = "Digging out";
        runtime.mineLastForwardProgressTick = runtime.currentTick;
        runtime.mineUnreachableTicks = 0;
        runtime.stuckTicks = 0;
        if (!rewarded) {
            this.handleInventoryFullWhileMining(companion, runtime);
        }
        return true;
    }

    private void handleInventoryFullWhileMining(CompanionRecord companion, CompanionRuntimeState runtime) {
        boolean canDeposit;
        if (runtime == null) {
            return;
        }
        runtime.mineTargetPos = null;
        runtime.mineTargetPriority = null;
        runtime.mineBlockedTargetUntilByKey.clear();
        runtime.mineLastTargetDistance = -1.0;
        runtime.mineLastForwardProgressTick = 0L;
        runtime.minePendingBreakPos = null;
        runtime.minePendingBreakBlockId = null;
        runtime.nextMineAttemptTick = 0L;
        boolean bl = canDeposit = companion != null && companion.linkedChests != null && !companion.linkedChests.isEmpty();
        if (canDeposit) {
            runtime.depositRequested = true;
            runtime.mineDepositOnlyMined = true;
            runtime.commandActive = true;
            runtime.mineReturnToLoadedArea = true;
            this.clearReturnLaneShiftState(runtime);
            runtime.mineStatusText = "Inventory full; depositing";
            return;
        }
        runtime.miningActive = false;
        runtime.mineForSpecific = false;
        runtime.commandActive = false;
        runtime.mineReturnToLoadedArea = true;
        this.clearReturnLaneShiftState(runtime);
        runtime.mineStatusText = "Inventory full; stopped";
    }

    private void stopMineForNoMatchingBlocks(CompanionRuntimeState runtime) {
        if (runtime == null) {
            return;
        }
        runtime.mineTargetPos = null;
        runtime.mineTargetPriority = null;
        runtime.mineTargetBlockId = null;
        runtime.mineBlockedTargetUntilByKey.clear();
        runtime.mineLastTargetDistance = -1.0;
        runtime.mineLastForwardProgressTick = 0L;
        runtime.minePendingBreakPos = null;
        runtime.minePendingBreakBlockId = null;
        runtime.nextMineAttemptTick = 0L;
        runtime.mineSearchRadius = 0;
        runtime.mineUnreachableTicks = 0;
        runtime.miningActive = false;
        runtime.mineForSpecific = false;
        runtime.commandActive = false;
        runtime.mineStatusText = "Stopped: no matching blocks nearby";
    }

    private int choosePathClearY(World world, Vector3d targetPos, int frontX, int frontZ, int legY, int headY) {
        if (world == null) {
            return Integer.MIN_VALUE;
        }
        int[] candidateYs = targetPos != null && targetPos.y > (double)headY + 0.25 ? new int[]{headY, headY + 1, legY, legY - 1} : (targetPos != null && targetPos.y < (double)legY - 0.25 ? new int[]{legY, headY, legY - 1, headY + 1} : new int[]{legY, headY, legY - 1, headY + 1});
        for (int y : candidateYs) {
            if (!this.isMineableBlock(world, frontX, y, frontZ)) continue;
            return y;
        }
        return Integer.MIN_VALUE;
    }

    private String normalizeBlockToken(String raw) {
        if (raw == null) {
            return "";
        }
        String lower = raw.toLowerCase(Locale.ROOT).trim();
        int idIdx = lower.indexOf("id=");
        if (idIdx >= 0) {
            int start = idIdx + 3;
            int end = lower.indexOf(44, start);
            if (end < 0) {
                end = lower.indexOf(125, start);
            }
            if (end > start) {
                lower = lower.substring(start, end).trim();
            }
        }
        if (lower.startsWith("*")) {
            lower = lower.substring(1);
        }
        return lower;
    }

    private boolean matchesBlockType(String actualRaw, String normalizedNeedle) {
        if (actualRaw == null || normalizedNeedle == null || normalizedNeedle.isBlank()) {
            return false;
        }
        String actual = this.normalizeBlockToken(actualRaw);
        if (actual.equals(normalizedNeedle)) {
            return true;
        }
        if (actual.endsWith("_" + normalizedNeedle)) {
            return true;
        }
        return actual.contains(normalizedNeedle);
    }

    private boolean grantMiningYieldToCompanion(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, CompanionRuntimeState runtime, String minedBlockRaw) {
        String canonical = this.canonicalBlockId(minedBlockRaw);
        if (canonical == null || canonical.isBlank()) {
            return true;
        }
        List<String> candidates = this.candidateMiningRewardItemIds(canonical);
        if (candidates.isEmpty()) {
            return true;
        }
        for (String itemId : candidates) {
            try {
                if (!InvUtil.giveItemToCompanion(npcEntity, companionRef, store, itemId, 1, this.logger)) continue;
                runtime.mineCollectedItemIds.add(itemId);
                return true;
            }
            catch (Throwable throwable) {
            }
        }
        return false;
    }

    private List<String> candidateMiningRewardItemIds(String canonicalBlockId) {
        ArrayList<String> ids = new ArrayList<String>();
        if (canonicalBlockId == null || canonicalBlockId.isBlank()) {
            return ids;
        }
        String blockId = canonicalBlockId.trim();
        if (this.isOreLikeBlockId(blockId)) {
            String oreCore = this.detectKnownOreCore(blockId);
            if (oreCore != null && !oreCore.isBlank()) {
                this.addRewardCandidate(ids, "Ore_" + oreCore);
            }
            return ids;
        }
        this.addRewardCandidate(ids, blockId);
        this.addRewardCandidate(ids, blockId.replace("_Block", "_Item"));
        this.addRewardCandidate(ids, blockId + "_Item");
        this.addRewardCandidate(ids, blockId.replace("_Block", ""));
        return ids;
    }

    private void addRewardCandidate(List<String> out, String id) {
        if (out == null || id == null) {
            return;
        }
        String v = id.trim();
        if (v.isBlank() || out.contains(v)) {
            return;
        }
        out.add(v);
    }

    private boolean tryTriggerMiningVisual(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store) {
        boolean interaction = this.tryTriggerMiningInteraction(npcEntity);
        boolean animation = interaction || this.tryTriggerMiningAnimation(npcEntity, companionRef, store);
        return animation;
    }

    private boolean tryTriggerMiningInteraction(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return false;
        }
        String[] methodNames = new String[]{"executeInteraction", "triggerInteraction", "useInteraction", "performInteraction", "startInteraction", "queueInteraction"};
        LinkedHashSet<String> interactionIds = new LinkedHashSet<String>();
        interactionIds.add("Pickaxe_Mine");
        interactionIds.add("Outlander_Peon_Pickaxe_Swing_Down");
        interactionIds.add("Outlander_Peon_Pickaxe_Swing_Left");
        interactionIds.add("NPC_Attack_Melee_Simple");
        interactionIds.add("NPC_Attack_Melee_Simple1");
        interactionIds.add("NPC_Attack_Melee_Simple2");
        interactionIds.add("NPC_Attack_Melee_Simple3");
        try {
            this.logMiningVisualRuntimeMethodSnapshotOnce(npcEntity);
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

    private boolean tryTriggerMiningAnimation(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store) {
        if (npcEntity == null) {
            return false;
        }
        String[] methodNames = new String[]{"playAnimation", "setAnimation", "playEmote", "setEmote", "performEmote"};
        String[] itemKeys = new String[]{"Mine", "SwingDown", "SwingLeft"};
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
                if ("playAnimation".equalsIgnoreCase(name) && companionRef != null && store != null && m.getParameterCount() == 4 && Ref.class.isAssignableFrom(m.getParameterTypes()[0]) && m.getParameterTypes()[2] == String.class && (slot = this.resolveItemAnimationSlot(m.getParameterTypes()[1])) != null) {
                    String[] stringArray = itemKeys;
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
                for (String key : itemKeys) {
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

    private void logMiningVisualRuntimeMethodSnapshotOnce(NPCEntity npcEntity) {
        if (this.miningVisualMethodSnapshotLogged || npcEntity == null) {
            return;
        }
        this.miningVisualMethodSnapshotLogged = true;
        try {
            ArrayList<String> candidates = new ArrayList<String>();
            for (Method m : npcEntity.getClass().getMethods()) {
                String n = m.getName().toLowerCase(Locale.ROOT);
                if (!n.contains("interaction") && !n.contains("animation") && !n.contains("emote")) continue;
                Class<?>[] p = m.getParameterTypes();
                StringBuilder sb = new StringBuilder(m.getName()).append("(");
                for (int i = 0; i < p.length; ++i) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append(p[i].getSimpleName());
                }
                sb.append(")");
                candidates.add(sb.toString());
            }
            if (!candidates.isEmpty()) {
                this.logger.at(Level.INFO).log("[MineVisual] runtime methods: " + String.join((CharSequence)" | ", candidates));
            } else {
                this.logger.at(Level.INFO).log("[MineVisual] runtime methods: none matched interaction/animation/emote");
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Failed runtime method snapshot for mining visuals.");
        }
    }

    private Object resolveItemAnimationSlot(Class<?> slotType) {
        if (slotType == null || !slotType.isEnum()) {
            return null;
        }
        try {
            String[] preferred;
            ?[] constants = slotType.getEnumConstants();
            if (constants == null || constants.length == 0) {
                return null;
            }
            for (String want : preferred = new String[]{"Item", "Main", "MainHand", "UpperBody", "Body", "Status"}) {
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

    private boolean isOreLikeBlockId(String canonicalBlockId) {
        if (canonicalBlockId == null || canonicalBlockId.isBlank()) {
            return false;
        }
        String v = canonicalBlockId.toLowerCase(Locale.ROOT);
        return v.contains("_ore") || v.startsWith("ore_") || v.contains("ore_");
    }

    private String detectKnownOreCore(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return null;
        }
        String v = blockId.toLowerCase(Locale.ROOT);
        if (v.contains("thorium")) {
            return "Thorium";
        }
        if (v.contains("cobalt")) {
            return "Cobalt";
        }
        if (v.contains("mithril")) {
            return "Mithril";
        }
        if (v.contains("onyxium")) {
            return "Onyxium";
        }
        if (v.contains("prisma")) {
            return "Prisma";
        }
        if (v.contains("adamantite")) {
            return "Adamantite";
        }
        if (v.contains("adamentite")) {
            return "Adamantite";
        }
        if (v.contains("adamanitite")) {
            return "Adamantite";
        }
        if (v.contains("gold")) {
            return "Gold";
        }
        if (v.contains("silver")) {
            return "Silver";
        }
        if (v.contains("copper")) {
            return "Copper";
        }
        if (v.contains("iron")) {
            return "Iron";
        }
        return null;
    }

    private String canonicalBlockId(String raw) {
        String lv;
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
        if ((lv = v.toLowerCase(Locale.ROOT)).isBlank() || lv.equals("air") || lv.equals("empty")) {
            return null;
        }
        return v;
    }

    private boolean checkMineLevelUp(CompanionRecord companion) {
        int newLevel = ProgressionConfig.mineLevelForBlocks(companion.mineBlocks);
        if (newLevel > companion.mineLevel) {
            companion.mineLevel = newLevel;
            return true;
        }
        return false;
    }

    private long mineAttemptCooldownTicksForLevel(int mineLevel) {
        int idx = Math.max(0, Math.min(mineLevel - 1, ProgressionConfig.MINE_SPEED_MULT.length - 1));
        double speedMult = ProgressionConfig.MINE_SPEED_MULT[idx];
        if (speedMult <= 1.0E-4) {
            speedMult = 1.0;
        }
        long ticks = Math.round(12.0 / speedMult);
        return Math.max(1L, ticks);
    }
}

