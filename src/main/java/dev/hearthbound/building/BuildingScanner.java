package dev.hearthbound.building;

import com.hypixel.hytale.server.core.universe.world.World;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * Scans the world against a building plan to detect when a player has already built
 * (or pasted) the structure manually. Used by {@code BuildingSystem.startResourceBuilding}
 * to short-circuit construction when the area already matches the prefab closely enough —
 * the prefab is then re-placed atomically and the building is marked complete instead of
 * the elf rebuilding it block-by-block.
 *
 * <p>Only non-free blocks count toward the match — free blocks (plants, decoration,
 * Stayed_*) are unreliable markers because the player wouldn't typically place them.
 */
public final class BuildingScanner {

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("build.scan");
    private BuildingScanner() {}

    /**
     * Returns the fraction of plan blocks (0.0–1.0) that match what currently exists in
     * the world at the same coordinates. Free blocks are excluded from both numerator
     * and denominator. Returns 0.0 when there are no countable blocks (defensive).
     */
    public static double scanMatchPercent(World world, List<BlockPlacer.BlockEntry> plan) {
        int total = 0;
        int matched = 0;
        int mismatchSamplesLogged = 0;
        final int MAX_MISMATCH_SAMPLES = 10;

        for (BlockPlacer.BlockEntry entry : plan) {
            String expected = ResourceBlockPlacer.normalizeBlockId(entry.blockType());
            if (ResourceBlockPlacer.isFreeBlock(expected)) continue;

            total++;
            String actual;
            try {
                var bt = world.getBlockType(entry.x(), entry.y(), entry.z());
                actual = bt != null ? ResourceBlockPlacer.normalizeBlockId(bt.getId()) : "Empty";
            } catch (Exception e) {
                actual = "Empty";
            }
            if (actual.equals(expected)) {
                matched++;
            } else if (mismatchSamplesLogged < MAX_MISMATCH_SAMPLES) {
                LOG.info("[Scanner] mismatch at (" + entry.x() + "," + entry.y() + "," + entry.z()
                        + "): expected=" + expected + " actual=" + actual);
                mismatchSamplesLogged++;
            }
        }

        if (total == 0) {
            LOG.info("[Scanner] no countable blocks in plan (size=" + plan.size() + ")");
            return 0.0;
        }
        double pct = (double) matched / total;
        LOG.info("[Scanner] match=" + matched + "/" + total + " (" + (int)(pct * 100) + "%)");
        return pct;
    }

    /**
     * Returns the resources needed to repair a completed building — only blocks that
     * currently differ from the prefab plan are counted. Free blocks are excluded.
     * Returns an empty map when the building is fully intact.
     */
    public static Map<String, Integer> getRepairCost(World world, List<BlockPlacer.BlockEntry> plan) {
        Map<String, Integer> cost = new LinkedHashMap<>();
        for (BlockPlacer.BlockEntry entry : plan) {
            String expected = ResourceBlockPlacer.normalizeBlockId(entry.blockType());
            if (ResourceBlockPlacer.isFreeBlock(expected)) continue;

            String actual;
            try {
                var bt = world.getBlockType(entry.x(), entry.y(), entry.z());
                actual = bt != null ? ResourceBlockPlacer.normalizeBlockId(bt.getId()) : "Empty";
            } catch (Exception e) {
                actual = "Empty";
            }
            if (!actual.equals(expected)) {
                cost.merge(expected, 1, Integer::sum);
            }
        }
        return cost;
    }
}
