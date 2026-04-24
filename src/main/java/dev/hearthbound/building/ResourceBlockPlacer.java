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
    private ScheduledFuture<?> task;
    private String lastHeldBlock = null;
    private BuilderBehavior builderBehavior;
    // Last recorded elf position, used to detect when he's walking (behavior tree Wander).
    // While he's walking we skip the placement tick — the animation reads as "contractor
    // inspecting the site" rather than placing a block mid-stride.
    private com.hypixel.hytale.math.vector.Vector3d lastKnownPos;
    private static final double MOVEMENT_EPSILON_SQ = 0.01; // ~0.1 block squared

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
        LOGGER.info("ResourceBlockPlacer started (" + blocks.size() + " blocks)");
        scheduleNextTick(randomInRange(BASE_DELAY_MIN_MS, BASE_DELAY_MAX_MS));
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

            // Skip this tick if the elf is walking (Wander motion from the builder role
            // pathing him across the construction site). Placing a block mid-stride both
            // looks wrong visually and means lookAtBlock can't aim at the right spot.
            if (builderBehavior != null && isElfMoving()) {
                scheduleNextTick(PAUSED_DELAY_MS);
                return;
            }

            BlockPlacer.BlockEntry entry = blocks.get(currentIndex);

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
            boolean fast = dev.hearthbound.building.BuildingSystem.get() != null
                    && dev.hearthbound.building.BuildingSystem.get().isFastBuild();
            if (consumeResource(normalizedType)) {
                BlockPlacer.placeBlock(world, entry);
                currentIndex++;
                if (paused) {
                    paused = false;
                    LOGGER.info("ResourceBlockPlacer resumed at block " + currentIndex);
                }
                // Longer pause right after switching to a new material; it reads as the elf
                // "picking up the next tool" rather than a metronomic placement tempo.
                nextDelay = fast ? FAST_DELAY_MS : (switchingMaterial
                        ? randomInRange(SWITCH_DELAY_MIN_MS, SWITCH_DELAY_MAX_MS)
                        : randomInRange(BASE_DELAY_MIN_MS, BASE_DELAY_MAX_MS));
            } else {
                if (!paused) {
                    paused = true;
                    LOGGER.info("ResourceBlockPlacer paused — need " + normalizedType +
                            " (block " + currentIndex + "/" + blocks.size() + ")");
                }
                nextDelay = fast ? FAST_DELAY_MS : PAUSED_DELAY_MS;
            }
            scheduleNextTick(nextDelay);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "ResourceBlockPlacer tick failed at index " + currentIndex, e);
            scheduleNextTick(PAUSED_DELAY_MS);
        }
    }

    /** Returns true when 1 unit of the resource was removed from the building's storage. */
    private boolean consumeResource(String normalizedItemId) {
        return buildingRecord.removeResource(normalizedItemId, 1) == 1;
    }

    /**
     * Detects motion by sampling the elf's position across ticks — if it shifted by more
     * than {@link #MOVEMENT_EPSILON_SQ}, he's mid-stride. Falls back to "not moving" when
     * we can't read a position (chunk unloaded, entity gone).
     */
    private boolean isElfMoving() {
        com.hypixel.hytale.math.vector.Vector3d now = builderBehavior.getPosition();
        if (now == null) {
            lastKnownPos = null;
            return false;
        }
        boolean moving = false;
        if (lastKnownPos != null) {
            double dx = now.x - lastKnownPos.x;
            double dy = now.y - lastKnownPos.y;
            double dz = now.z - lastKnownPos.z;
            moving = dx * dx + dy * dy + dz * dz > MOVEMENT_EPSILON_SQ;
        }
        lastKnownPos = new com.hypixel.hytale.math.vector.Vector3d(now.x, now.y, now.z);
        return moving;
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
            remaining.merge(normalizeBlockId(blocks.get(i).blockType()), 1, Integer::sum);
        }
        return remaining;
    }

    static String normalizeBlockId(String blockType) {
        String b = blockType.startsWith("*") ? blockType.substring(1) : blockType;
        int idx = b.indexOf("_State_Definitions_");
        return idx != -1 ? b.substring(0, idx) : b;
    }
}
