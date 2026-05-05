package dev.hearthbound.building;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import dev.hearthbound.village.BuildingRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Reads the world inside a farm's bbox and classifies every cell into actionable
 * categories for the farmer NPC: harvest, weed, replant, water.
 *
 * Block-id rules (from cropinfo PoC dump):
 *   - Stage1 crop:   "Plant_Crop_X_Block"                                   (no leading "*")
 *   - Mature crop:   "*Plant_Crop_X_Block_State_Definitions_StageFinal"     (ready to harvest)
 *   - Mid stages:    "*Plant_Crop_X_Block_State_Definitions_Stage{2,3}"     (still growing)
 *   - Tilled empty:  "Soil_Dirt_Tilled"                                     (ready to plant)
 *   - Tilled state:  "*Soil_Dirt_Tilled_State_Definitions_{Watered,Fertilized,Fertilized_Watered}"
 *   - Weeds on plot: Plant_Sapling_*, Plant_Leaves_*, Plant_Crop_Wild_Grass_*, Plant_Bush*
 *
 * Anything else (stone, wood, decoration) is ignored — the player owns it.
 */
public final class FarmScanner {

    private static final Logger LOGGER = Logger.getLogger(FarmScanner.class.getName());

    public enum Action { HARVEST, WEED, TILL, REPLANT, WATER }

    public record Target(int x, int y, int z, Action action, String cropType) {
        /** Convenience: target without a crop type (weed/till/water). */
        public static Target of(int x, int y, int z, Action action) {
            return new Target(x, y, z, action, null);
        }
    }

    public record Scan(List<Target> harvest, List<Target> weed, List<Target> till,
                       List<Target> replant, List<Target> water) {
        public boolean isEmpty() {
            return harvest.isEmpty() && weed.isEmpty() && till.isEmpty()
                && replant.isEmpty() && water.isEmpty();
        }
        public int size() {
            return harvest.size() + weed.size() + till.size() + replant.size() + water.size();
        }
    }

    private FarmScanner() {}

    /** Returns an empty Scan if the farm has no bounds. */
    public static Scan scan(World world, BuildingRecord farm) {
        FarmBounds.Bounds b = FarmBounds.compute(farm);
        if (b == null) {
            return new Scan(List.of(), List.of(), List.of(), List.of(), List.of());
        }

        List<Target> harvest = new ArrayList<>();
        List<Target> weed    = new ArrayList<>();
        List<Target> till    = new ArrayList<>();
        List<Target> replant = new ArrayList<>();
        List<Target> water   = new ArrayList<>();

        for (int y = b.minY(); y <= b.maxY(); y++) {
            for (int x = b.minX(); x <= b.maxX(); x++) {
                for (int z = b.minZ(); z <= b.maxZ(); z++) {
                    classifyCell(world, x, y, z, harvest, weed, till, replant, water);
                }
            }
        }

        return new Scan(harvest, weed, till, replant, water);
    }

    private static void classifyCell(World world, int x, int y, int z,
                                     List<Target> harvest, List<Target> weed, List<Target> till,
                                     List<Target> replant, List<Target> water) {
        String id = readId(world, x, y, z);
        if (id == null) return;
        String norm = id.startsWith("*") ? id.substring(1) : id;

        // Plot-soil candidates that should become tilled. Soil_Pathway is a paved walkway
        // the player explicitly placed — never till that. Soil_Mud_Dry is what a tilled cell
        // decays back into when nothing is planted (engine farming lifecycle), so it's our
        // primary "needs re-tilling" signal.
        if (isTillable(norm)) {
            // Only till cells that are actually inside a planting row — the prefab puts a
            // Soil_Pathway boundary around each crop column, so a tillable block neighboring
            // pathway is fine, but we still want to skip cells where the soil above-or-below
            // is structurally part of the building (covered by a roof, fence, etc.). For now
            // accept everything tillable; the bbox is already constrained to the plot.
            till.add(Target.of(x, y, z, Action.TILL));
            return;
        }

        // Weeds: anything *non-crop* that's organically grown on the plot. We require the
        // cell directly below to be tilled soil — otherwise a tree growing next to the
        // plot edge (in the bbox y-range but not over a tilled tile) would be flagged.
        if (isWeed(norm) && hasTilledBelow(world, x, y, z)) {
            weed.add(Target.of(x, y, z, Action.WEED));
            return;
        }

        // Harvest-ready: any crop block whose id ends with _StageFinal.
        if (norm.startsWith("Plant_Crop_") && norm.endsWith("_StageFinal")) {
            // Filler (top half of a tall crop) shares the same id as the root block; harvest only
            // the root — the engine drops the upper part automatically when the bottom breaks.
            if (isCropFillerAbove(world, x, y, z)) return;
            String crop = extractCropType(norm);
            if (crop == null) return;
            harvest.add(new Target(x, y, z, Action.HARVEST, crop));
            return;
        }

        // Tilled soil — either empty (replant candidate) or planted-but-dry (water candidate).
        // Note: we never water an *empty* tilled cell. The next farmer cycle will replant it,
        // and watering an empty cell has no useful effect (no crop to feed).
        if (norm.equals("Soil_Dirt_Tilled") || norm.startsWith("Soil_Dirt_Tilled_State_Definitions_")) {
            String above = readId(world, x, y + 1, z);
            boolean cellAboveIsCrop = above != null
                    && stripStar(above).startsWith("Plant_Crop_");

            if (cellAboveIsCrop) {
                if (!norm.contains("Watered")) {
                    water.add(Target.of(x, y, z, Action.WATER));
                }
            } else if (isCellAbsent(world, x, y + 1, z)) {
                replant.add(Target.of(x, y, z, Action.REPLANT));
            }
        }
    }

    /**
     * Soil types we re-till to recover plot tiles after the engine's farming lifecycle
     * decays them, or to reclaim grass that grew over an unmaintained plot. Soil_Pathway
     * is excluded — it's a deliberate paved walkway, not abandoned soil.
     */
    private static boolean isTillable(String norm) {
        if (norm.startsWith("Soil_Pathway")) return false;
        if (norm.startsWith("Soil_Dirt_Tilled")) return false;
        return norm.startsWith("Soil_Grass")    // Soil_Grass, Soil_Grass_Full
            || norm.equals("Soil_Dirt")
            || norm.startsWith("Soil_Mud_Dry")
            || norm.startsWith("Soil_Mud");
    }

    private static boolean hasTilledBelow(World world, int x, int y, int z) {
        // Walk down a few cells — tall weeds (saplings that turned into trees) sit several
        // blocks above the soil, so a strict y-1 check would miss them.
        for (int dy = 1; dy <= 4; dy++) {
            String id = readId(world, x, y - dy, z);
            if (id == null) continue;
            String norm = id.startsWith("*") ? id.substring(1) : id;
            if (norm.startsWith("Soil_Dirt_Tilled")) return true;
            // If we hit non-soil ground first (stone, pathway, etc.) without finding tilled,
            // this cell isn't over our plot — don't pretend it's a weed.
            if (norm.startsWith("Soil_") || norm.startsWith("Rock_")) return false;
        }
        return false;
    }

    /** True if the block at (x,y+1,z) is the same crop id — i.e. this cell is the top of a tall crop. */
    private static boolean isCropFillerAbove(World world, int x, int y, int z) {
        String above = readId(world, x, y + 1, z);
        if (above == null) return false;
        // If the block above is *also* Plant_Crop_*, then this position is the top half of a stack.
        // We harvest the root only — top is at y+1, so harvest at y is correct; the filler above
        // disappears automatically. So the *current* cell is filler iff y-1 also has the same crop.
        String below = readId(world, x, y - 1, z);
        if (below == null) return false;
        String belowNorm = below.startsWith("*") ? below.substring(1) : below;
        return belowNorm.startsWith("Plant_Crop_");
    }

    private static String stripStar(String id) {
        if (id == null) return "";
        return id.startsWith("*") ? id.substring(1) : id;
    }

    private static boolean isCellAbsent(World world, int x, int y, int z) {
        String id = readId(world, x, y, z);
        if (id == null) return true;
        return id.equals("Empty") || id.equals("Editor_Empty") || id.equals("Filter_Air_Block");
    }

    private static boolean isWeed(String norm) {
        if (norm.startsWith("Plant_Sapling_")) return true;
        if (norm.startsWith("Plant_Leaves_")) return true;
        if (norm.startsWith("Plant_Bush")) return true;
        if (norm.startsWith("Plant_Crop_Wild_")) return true;
        if (norm.startsWith("Plant_Sunflower")) return true;
        // Trees that grew from saplings: trunk + branches end up where saplings used to stand.
        if (norm.startsWith("Wood_") && (norm.contains("_Trunk") || norm.contains("_Branch"))) return true;
        return false;
    }

    /** Extracts "X" from "Plant_Crop_X_Block_State_Definitions_StageFinal" or "Plant_Crop_X_Block". */
    static String extractCropType(String norm) {
        if (!norm.startsWith("Plant_Crop_")) return null;
        int prefix = "Plant_Crop_".length();
        int blockIdx = norm.indexOf("_Block", prefix);
        if (blockIdx < 0) return null;
        return norm.substring(prefix, blockIdx);
    }

    private static String readId(World world, int x, int y, int z) {
        try {
            BlockType bt = world.getBlockType(x, y, z);
            return bt != null ? bt.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
