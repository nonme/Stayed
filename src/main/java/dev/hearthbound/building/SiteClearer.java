package dev.hearthbound.building;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import dev.hearthbound.npc.BuilderBehavior;
import dev.hearthbound.util.TickScheduler;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;

/**
 * Pre-construction site clearing pass: elf walks the build plan and breaks every cell
 * that contains a terrain block (stone, soil, ore, vegetation, tree trunks etc.).
 * Empty cells and player-placed non-terrain blocks are skipped.
 * If the player already broke a cell the elf would have cleared, it is skipped instantly.
 *
 * When all cells are processed, fires onComplete to hand off to ResourceBlockPlacer.
 */
public class SiteClearer {

    private static final Logger LOGGER = Logger.getLogger(SiteClearer.class.getName());

    // Time from Mine animation start to block removal — timed to the swing impact.
    private static final long BREAK_IMPACT_MS = 100;
    // Pause after removing a block so the player sees the empty cell.
    private static final long GAP_MIN_MS = 200;
    private static final long GAP_MAX_MS = 350;
    // Fast-mode collapsed delay.
    private static final long FAST_DELAY_MS = 10;

    private final World world;
    private final List<BlockPlacer.BlockEntry> plan;
    private final BuilderBehavior builderBehavior;
    private final Runnable onComplete;
    private final java.util.Random rng = new java.util.Random();

    private int currentIndex = 0;
    private boolean inBreakPhase = false;
    private boolean cancelled = false;
    private ScheduledFuture<?> task;
    // Packed-coordinate → normalized block type for every block in the original plan.
    // Used to skip cells where we already placed a building block on a prior run.
    private final java.util.Map<Long, String> plannedBlocks;

    public SiteClearer(World world, List<BlockPlacer.BlockEntry> plan,
                       BuilderBehavior builderBehavior, Runnable onComplete) {
        this.world = world;
        this.builderBehavior = builderBehavior;
        this.onComplete = onComplete;
        this.plannedBlocks = buildPlannedMap(plan);
        // Expand the plan to the full bounding box so cells that were saved as implicit
        // air in the prefab (no Empty entry) are also scanned for terrain blocks.
        this.plan = expandToBbox(plan);
    }

    private static java.util.Map<Long, String> buildPlannedMap(List<BlockPlacer.BlockEntry> plan) {
        java.util.Map<Long, String> map = new java.util.HashMap<>(plan.size() * 2);
        for (BlockPlacer.BlockEntry e : plan) {
            String normalized = ResourceBlockPlacer.normalizeBlockId(e.blockType());
            if (!"Empty".equals(normalized)) {
                map.put(packCoord(e.x(), e.y(), e.z()), normalized);
            }
        }
        return map;
    }

    private static long packCoord(int x, int y, int z) {
        return ((long)(x & 0xFFFFF) << 40) | ((long)(y & 0xFFFFF) << 20) | (z & 0xFFFFF);
    }

    /**
     * Fills in every coordinate inside the axis-aligned bounding box of the plan,
     * then sorts top-to-bottom so upper blocks are removed before lower ones —
     * avoids mid-air floating debris and looks like a natural demolition sweep.
     */
    private static List<BlockPlacer.BlockEntry> expandToBbox(List<BlockPlacer.BlockEntry> plan) {
        if (plan.isEmpty()) return List.of();
        int minX = plan.stream().mapToInt(BlockPlacer.BlockEntry::x).min().getAsInt();
        int maxX = plan.stream().mapToInt(BlockPlacer.BlockEntry::x).max().getAsInt();
        int minY = plan.stream().mapToInt(BlockPlacer.BlockEntry::y).min().getAsInt();
        int maxY = plan.stream().mapToInt(BlockPlacer.BlockEntry::y).max().getAsInt();
        int minZ = plan.stream().mapToInt(BlockPlacer.BlockEntry::z).min().getAsInt();
        int maxZ = plan.stream().mapToInt(BlockPlacer.BlockEntry::z).max().getAsInt();

        List<BlockPlacer.BlockEntry> bbox = new java.util.ArrayList<>(
                (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1));
        for (int y = maxY; y >= minY; y--) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    bbox.add(new BlockPlacer.BlockEntry(x, y, z, "Empty"));
                }
            }
        }
        return bbox;
    }

    public void start() {
        LOGGER.info("SiteClearer started (" + plan.size() + " blocks to scan)");
        scheduleNextTick(0);
    }

    public void cancel() {
        cancelled = true;
        inBreakPhase = false;
        if (task != null) {
            task.cancel(false);
            task = null;
        }
    }

    private void scheduleNextTick(long delayMs) {
        if (cancelled) return;
        task = TickScheduler.runLater(world, delayMs, this::tick);
    }

    private long randomInRange(long min, long max) {
        if (max <= min) return min;
        return min + rng.nextLong(max - min + 1);
    }

    private void tick() {
        try {
            // Skip cells until we find one that needs clearing.
            while (currentIndex < plan.size()) {
                BlockPlacer.BlockEntry entry = plan.get(currentIndex);

                if (inBreakPhase) {
                    // Impact moment: remove the block, clear vegetation/entities, enter gap.
                    inBreakPhase = false;
                    builderBehavior.clearVegetationAbove(entry.x(), entry.y(), entry.z());
                    builderBehavior.clearEntitiesOnBlock(entry.x(), entry.y(), entry.z());
                    BlockPlacer.silentRemoveBlock(world, entry.x(), entry.y(), entry.z());
                    currentIndex++;

                    boolean fast = isFast();
                    scheduleNextTick(fast ? FAST_DELAY_MS : randomInRange(GAP_MIN_MS, GAP_MAX_MS));
                    return;
                }

                if (builderBehavior != null && builderBehavior.isWalking()) {
                    scheduleNextTick(isFast() ? FAST_DELAY_MS : 100);
                    return;
                }

                String blockId = getBlockId(entry.x(), entry.y(), entry.z());

                // If the world already has exactly what the plan expects here, skip —
                // this block was placed on a previous run. Takes priority over terrain check
                // so we never break our own blocks even if they happen to be "terrain" IDs.
                String planned = plannedBlocks.get(packCoord(entry.x(), entry.y(), entry.z()));
                if (planned != null) {
                    String normalizedActual = ResourceBlockPlacer.normalizeBlockId(blockId);
                    if (planned.equals(normalizedActual)) {
                        currentIndex++;
                        continue;
                    }
                }

                if (!isTerrainBlock(blockId)) {
                    // Air, player-placed block, or already cleared by the player — skip instantly.
                    currentIndex++;
                    continue;
                }

                // Terrain block present — start break animation.
                builderBehavior.lookAtBlock(entry.x(), entry.y(), entry.z());
                builderBehavior.equipPickaxe();
                builderBehavior.playBreakAnimation();
                inBreakPhase = true;
                scheduleNextTick(isFast() ? FAST_DELAY_MS : BREAK_IMPACT_MS);
                return;
            }

            // All cells processed.
            LOGGER.info("SiteClearer finished");
            if (onComplete != null) onComplete.run();

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "SiteClearer tick failed at index " + currentIndex, e);
            scheduleNextTick(100);
        }
    }

    private boolean isFast() {
        BuildingSystem bs = BuildingSystem.get();
        return bs != null && bs.isFastBuild();
    }

    private String getBlockId(int x, int y, int z) {
        try {
            BlockType bt = world.getBlockType(x, y, z);
            return bt != null ? bt.getId() : "Empty";
        } catch (Exception e) {
            return "Empty";
        }
    }

    /**
     * Returns true for blocks that appear in natural world generation and should be
     * cleared by the elf before construction. Player-placed blocks are NOT terrain.
     */
    static boolean isTerrainBlock(String id) {
        if (id == null || id.equals("Empty") || id.equals("Editor_Empty")
                || id.equals("Filter_Air_Block")) return false;
        // Raw stone variants (not bricks/stairs/walls — those are player-crafted)
        if (id.startsWith("Rock_Stone") || id.startsWith("Rock_Chalk")
                || id.startsWith("Rock_Basalt") || id.startsWith("Rock_Aqua")
                || id.startsWith("Rock_Igneous") || id.startsWith("Rock_Limestone")
                || id.startsWith("Rock_Sandstone") || id.startsWith("Rock_Marble")
                || id.startsWith("Rock_Quartzite") || id.startsWith("Rock_Shale")
                || id.startsWith("Rock_Slate") || id.startsWith("Rock_Volcanic")) return true;
        if (id.startsWith("Ore_") || id.startsWith("Rubble_")) return true;
        // Soil — raw terrain only, not crafted clay bricks/tiles
        if (id.startsWith("Soil_Grass") || id.startsWith("Soil_Dirt")
                || id.startsWith("Soil_Sand") || id.startsWith("Soil_Gravel")
                || id.startsWith("Soil_Mud") || id.startsWith("Soil_Snow")
                || id.startsWith("Soil_Pebbles") || id.startsWith("Soil_Needles")
                || id.startsWith("Soil_Roots") || id.startsWith("Soil_Leaves")
                || id.startsWith("Soil_Hive") || id.startsWith("Soil_Seaweed")
                || id.equals("Soil_Ash") || id.equals("Soil_Volcanic_Gravel")
                || id.equals("Soil_Magma_Cooled_Gravel")) return true;
        // Bush, fern, flower, mushroom, vine — solid vegetation blocks that occupy a cell.
        // Plant_Grass* is intentionally excluded: it's a thin decoration that sits on top
        // of a Soil block. It gets removed automatically via clearVegetationAbove() when
        // the soil block beneath it is broken — no need to spend a separate break tick on it.
        if (id.startsWith("Plant_Bush") || id.startsWith("Plant_Fern")
                || id.startsWith("Plant_Flower") || id.startsWith("Plant_Mushroom")
                || id.startsWith("Plant_Vine") || id.startsWith("Plant_Crop_Mushroom")
                || id.startsWith("Plant_Leaves")) return true;
        // Tree trunks that generate naturally
        if (id.endsWith("_Trunk")) return true;
        // Water/liquid terrain
        if (id.equals("Water") || id.equals("Lava")) return true;
        return false;
    }

}
