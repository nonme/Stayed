package dev.hearthbound.building;

import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import dev.hearthbound.npc.BuilderBehavior;
import dev.hearthbound.util.TickScheduler;
import dev.hearthbound.village.BuildingRecord;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Places building blocks one by one, consuming matching resources from the building's
 * local storage map. Pauses when resources run out, resumes automatically once refilled.
 */
public class ResourceBlockPlacer {

    private static final Logger LOGGER = Logger.getLogger(ResourceBlockPlacer.class.getName());
    // Delay envelope between placements — jittered so the build doesn't look metronomic.
    private static final long BASE_DELAY_MIN_MS = 200;
    private static final long BASE_DELAY_MAX_MS = 400;
    // Longer pause whenever the elf switches to a new material, like "grabbing a new tool".
    private static final long SWITCH_DELAY_MIN_MS = 500;
    private static final long SWITCH_DELAY_MAX_MS = 1000;
    // Retry cadence while waiting for resources to be deposited.
    private static final long PAUSED_DELAY_MS = 300;
    // Collapsed delay for BuildingSystem.fastBuild — just enough that we still yield between
    // placements so we don't starve the world thread.
    private static final long FAST_DELAY_MS = 10;
    // Break timing for rogue blocks placed after site-clearing (e.g. a griefer).
    private static final long BREAK_IMPACT_MS = 100;
    private static final long GAP_MIN_MS = 200;
    private static final long GAP_MAX_MS = 350;

    private final World world;
    private final List<BlockPlacer.BlockEntry> blocks;
    private final BuildingRecord buildingRecord;
    private final double safeX, safeY, safeZ;
    private final Runnable onComplete;
    private final UUID elfUuid;
    private final UUID ownerUuid;
    private final java.util.Random rng = new java.util.Random();

    private int currentIndex = 0;
    private boolean paused = false;
    private boolean cancelled = false;
    // True when the elf has started the break animation for currentIndex and we're waiting
    // for BREAK_DELAY before actually removing the old block and placing the new one.
    private boolean inBreakPhase = false;
    // True after the old block was removed — waiting GAP_DELAY before placing the new one.
    private boolean inGapPhase = false;
    private ScheduledFuture<?> task;
    private String lastHeldBlock = null;
    private BuilderBehavior builderBehavior;

    public ResourceBlockPlacer(World world, List<BlockPlacer.BlockEntry> blocks,
                                BuildingRecord buildingRecord,
                                double safeX, double safeY, double safeZ,
                                UUID elfUuid, UUID ownerUuid, Runnable onComplete) {
        this.world = world;
        this.blocks = List.copyOf(blocks);
        this.buildingRecord = buildingRecord;
        this.safeX = safeX;
        this.safeY = safeY;
        this.safeZ = safeZ;
        this.elfUuid = elfUuid;
        this.ownerUuid = ownerUuid;
        this.onComplete = onComplete;
    }

    public void start() {
        if (elfUuid != null) {
            // Builder role already keeps the NPC stationary and free of head-rotation
            // from the behavior tree, so no freeze/moveTo needed — we only need
            // BuilderBehavior for the lookAtBlock packet path.
            builderBehavior = new BuilderBehavior(world, elfUuid, ownerUuid);
        }
        currentIndex = computeResumeIndex();
        if (currentIndex > 0) {
            LOGGER.info("ResourceBlockPlacer resuming from block " + currentIndex + "/" + blocks.size());
        } else {
            LOGGER.info("ResourceBlockPlacer started (" + blocks.size() + " blocks)");
        }
        scheduleNextTick(randomInRange(BASE_DELAY_MIN_MS, BASE_DELAY_MAX_MS));
    }

    /**
     * Scans the plan from the beginning and returns the index of the first block
     * that doesn't match what's already in the world — i.e. where construction left off.
     * Free blocks (plants, decorations) are skipped because they may not survive a restart.
     */
    private int computeResumeIndex() {
        for (int i = 0; i < blocks.size(); i++) {
            BlockPlacer.BlockEntry entry = blocks.get(i);
            String expected = normalizeBlockId(entry.blockType());
            // Free/decoration blocks are unreliable markers — skip them when scanning.
            if (isFreeBlock(expected)) continue;
            try {
                var bt = world.getBlockType(entry.x(), entry.y(), entry.z());
                String actual = bt != null ? normalizeBlockId(bt.getId()) : "Empty";
                if (!actual.equals(expected)) {
                    return i;
                }
            } catch (Exception e) {
                return i;
            }
        }
        return blocks.size();
    }

    private void scheduleNextTick(long delayMs) {
        if (cancelled) return;
        task = TickScheduler.runLater(world, delayMs, this::tick);
    }

    private long randomInRange(long minInclusive, long maxInclusive) {
        if (maxInclusive <= minInclusive) return minInclusive;
        return minInclusive + rng.nextLong(maxInclusive - minInclusive + 1);
    }

    private void tick() {
        try {
            if (currentIndex >= blocks.size()) {
                cancel();
                clearElfHeldItem();
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }

            // Skip this tick if the elf is walking — placing mid-stride looks wrong and
            // lookAtBlock can't aim correctly. isWalking() reads the engine's movement
            // state flag directly, which is more reliable than position-delta sampling.
            if (builderBehavior != null && !inBreakPhase && !inGapPhase
                    && builderBehavior.isWalking()) {
                scheduleNextTick(PAUSED_DELAY_MS);
                return;
            }

            BlockPlacer.BlockEntry entry = blocks.get(currentIndex);
            boolean fast = dev.hearthbound.building.BuildingSystem.get() != null
                    && dev.hearthbound.building.BuildingSystem.get().isFastBuild();

            if (builderBehavior != null) {
                builderBehavior.lookAtBlock(entry.x(), entry.y(), entry.z());
            }

            boolean switchingMaterial = !entry.blockType().equals(lastHeldBlock);
            if (switchingMaterial) {
                lastHeldBlock = entry.blockType();
                updateElfHeldItem(entry.blockType());
            }

            long nextDelay;
            String normalizedType = normalizeBlockId(entry.blockType());

            // ── Gap phase completion ────────────────────────────────────────────────
            // Old block is already gone — now equip block item, play Build anim, place.
            if (inGapPhase) {
                inGapPhase = false;
                if (builderBehavior != null) {
                    builderBehavior.equipBlock(normalizedType);
                    builderBehavior.playBuildAnimation();
                }
                BlockPlacer.placeBlock(world, entry);
                currentIndex++;
                nextDelay = fast ? FAST_DELAY_MS : randomInRange(BASE_DELAY_MIN_MS, BASE_DELAY_MAX_MS);
                scheduleNextTick(nextDelay);
                return;
            }

            // ── Break phase completion ──────────────────────────────────────────────
            // Mine animation finished — remove the old block, enter gap phase so the
            // player briefly sees the empty cell before the new block appears.
            if (inBreakPhase) {
                inBreakPhase = false;
                if (builderBehavior != null) {
                    builderBehavior.clearVegetationAbove(entry.x(), entry.y(), entry.z());
                    builderBehavior.clearEntitiesOnBlock(entry.x(), entry.y(), entry.z());
                }
                BlockPlacer.silentRemoveBlock(world, entry.x(), entry.y(), entry.z());
                inGapPhase = true;
                scheduleNextTick(fast ? FAST_DELAY_MS : randomInRange(GAP_MIN_MS, GAP_MAX_MS));
                return;
            }

            // All resources were deposited before construction started — no per-block consume needed.
            // ── Break phase start ─────────────────────────────────────────────────────────────────
            // Only break terrain blocks (stone, soil, etc.) — never break blocks that are already
            // part of the building (e.g. on resume after server restart).
            if (!fast && builderBehavior != null
                    && isTerrainAtPos(entry.x(), entry.y(), entry.z())) {
                inBreakPhase = true;
                builderBehavior.equipPickaxe();
                builderBehavior.playBreakAnimation();
                scheduleNextTick(fast ? FAST_DELAY_MS : BREAK_IMPACT_MS);
                return;
            }

            // No existing block — or fast mode — go straight to place.
            if (builderBehavior != null) {
                builderBehavior.clearVegetationAbove(entry.x(), entry.y(), entry.z());
                builderBehavior.clearEntitiesOnBlock(entry.x(), entry.y(), entry.z());
                builderBehavior.playBuildAnimation();
            }
            BlockPlacer.placeBlock(world, entry);
            currentIndex++;
            nextDelay = fast ? FAST_DELAY_MS : (switchingMaterial
                    ? randomInRange(SWITCH_DELAY_MIN_MS, SWITCH_DELAY_MAX_MS)
                    : randomInRange(BASE_DELAY_MIN_MS, BASE_DELAY_MAX_MS));
            scheduleNextTick(nextDelay);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "ResourceBlockPlacer tick failed at index " + currentIndex, e);
            scheduleNextTick(PAUSED_DELAY_MS);
        }
    }

    private boolean isTerrainAtPos(int x, int y, int z) {
        try {
            var bt = world.getBlockType(x, y, z);
            return SiteClearer.isTerrainBlock(bt != null ? bt.getId() : null);
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns true when 1 unit of the resource was removed from the building's storage. */
    private boolean consumeResource(String normalizedItemId) {
        return buildingRecord.removeResource(normalizedItemId, 1) == 1;
    }

    public double getSafeX() { return safeX; }
    public double getSafeY() { return safeY; }
    public double getSafeZ() { return safeZ; }

    /**
     * Debug: immediately place every remaining block, bypass resource checks, and fire
     * {@link #onComplete}. Used by {@code /hb instabuild} to skip the animation when
     * iterating on a prefab. Safe to call from the world thread.
     */
    public void finishNow() {
        if (cancelled || currentIndex >= blocks.size()) return;
        for (int i = currentIndex; i < blocks.size(); i++) {
            try {
                BlockPlacer.placeBlock(world, blocks.get(i));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "finishNow: placeBlock failed at index " + i, e);
            }
        }
        currentIndex = blocks.size();
        cancel();
        clearElfHeldItem();
        if (onComplete != null) {
            onComplete.run();
        }
        LOGGER.info("ResourceBlockPlacer: finishNow completed " + blocks.size() + " blocks");
    }

    public void cancel() {
        cancelled = true;
        inBreakPhase = false;
        inGapPhase = false;
        if (task != null) {
            task.cancel(false);
            task = null;
        }
    }

    public boolean isFinished() {
        return currentIndex >= blocks.size();
    }

    public boolean isPaused() {
        return paused;
    }

    public int getProgress() {
        return blocks.isEmpty() ? 100 : (currentIndex * 100 / blocks.size());
    }

    public int getBlocksPlaced() {
        return currentIndex;
    }

    public int getTotal() {
        return blocks.size();
    }

    /** Returns the block type currently needed (or null if finished). */
    public String getCurrentNeededBlock() {
        if (currentIndex >= blocks.size()) return null;
        return blocks.get(currentIndex).blockType();
    }

    /**
     * Give the elf the current building block to hold in its hand.
     */
    private void updateElfHeldItem(String blockType) {
        try {
            if (elfUuid == null) return;
            Entity elf = world.getEntity(elfUuid);
            if (elf instanceof LivingEntity living) {
                var inv = living.getInventory();
                if (inv != null) {
                    var hotbar = inv.getHotbar();
                    ItemStack current = hotbar.getItemStack((short) 0);
                    if (current != null && !current.isEmpty()) {
                        hotbar.removeItemStackFromSlot((short) 0, current.getQuantity());
                    }
                    hotbar.addItemStackToSlot((short) 0, new ItemStack(blockType, 1));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not update elf held item", e);
        }
    }

    private void clearElfHeldItem() {
        try {
            if (elfUuid == null) return;
            Entity elf = world.getEntity(elfUuid);
            if (elf instanceof LivingEntity living) {
                var inv = living.getInventory();
                if (inv != null) {
                    inv.getHotbar().removeItemStackFromSlot((short) 0, 1);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not clear elf held item", e);
        }
    }

    /**
     * Returns remaining resources needed (from currentIndex onward).
     */
    public java.util.Map<String, Integer> getRemainingResources() {
        java.util.Map<String, Integer> remaining = new java.util.LinkedHashMap<>();
        for (int i = currentIndex; i < blocks.size(); i++) {
            String id = normalizeBlockId(blocks.get(i).blockType());
            if (!isFreeBlock(id)) {
                remaining.merge(id, 1, Integer::sum);
            }
        }
        return remaining;
    }

    static String normalizeBlockId(String blockType) {
        String b = blockType.startsWith("*") ? blockType.substring(1) : blockType;
        int idx = b.indexOf("_State_Definitions_");
        return idx != -1 ? b.substring(0, idx) : b;
    }

    /** Blocks that are placed for free — no player deposit needed. */
    static boolean isFreeBlock(String normalizedId) {
        return normalizedId.startsWith("Plant_")
                || normalizedId.startsWith("Ingredient_")
                || normalizedId.startsWith("Stayed_")
                || "Empty".equals(normalizedId)
                || "Deco_Mug".equals(normalizedId)
                || "Deco_Inkwell".equals(normalizedId)
                || "Deco_Scroll".equals(normalizedId)
                || "Soil_Dirt_Tilled".equals(normalizedId);
    }

    /** Sawmill-specific free blocks: decorative sticks/branches placed by the elf for free. */
    static boolean isSawmillFreeBlock(String normalizedId) {
        return "Wood_Stick".equals(normalizedId);
    }

    /** Mine-specific free blocks: the stone and ore being excavated cost nothing. */
    static boolean isMineExcavationBlock(String normalizedId) {
        return "Rock_Stone".equals(normalizedId)
                || normalizedId.startsWith("Ore_")
                || normalizedId.startsWith("Rubble_");
    }
}
