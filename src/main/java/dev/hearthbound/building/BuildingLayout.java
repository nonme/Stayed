package dev.hearthbound.building;

import dev.hearthbound.village.BuildingType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Computes interior target and door/gate positions for each building type
 * by analysing the prefab at startup. Results are cached — computed once per type.
 *
 * All coordinates are local to the anchor block (anchor = origin 0,0,0).
 * Apply BuildingRecord rotation via VillagerScheduler.rotateLocalOffset() before use.
 */
public final class BuildingLayout {

    private static final Logger LOGGER = Logger.getLogger(BuildingLayout.class.getName());

    /**
     * Immutable layout descriptor for one building type.
     *
     * @param centerLX           local X of the interior stand-point (native prefab space)
     * @param centerLZ           local Z of the interior stand-point (native prefab space)
     * @param floorLY            local Y of the walkable floor relative to anchor.
     *                           0 for most buildings; -2 for farm (Scarecrow is 2 blocks above ground).
     * @param anchorPrefabRotation yaw rotation (0–3) of the anchor block inside the prefab.
     *                             Apply rotationSteps = (worldRot - anchorPrefabRotation + 4) % 4
     *                             when rotating native-local coords to world space.
     * @param doorLX    local X of the door/gate block (native prefab space), or null
     * @param doorLY    local Y of the door/gate block
     * @param doorLZ    local Z of the door/gate block
     * @param openBlock  block ID for the open state, or null
     * @param closeBlock block ID for the closed state, or null
     */
    public record Layout(int centerLX, int centerLZ, int floorLY, int anchorPrefabRotation,
                         Integer doorLX, Integer doorLY, Integer doorLZ,
                         Integer doorNativeRotation,
                         String openBlock, String closeBlock) {
        public boolean hasDoor() { return doorLX != null; }

        /**
         * Rotation to use when placing the door/gate block for a given world rotation.
         * rotateBlockRotation applies the same yaw transform as PrefabLoader.
         */
        public int doorWorldRotation(int worldRotation) {
            if (doorNativeRotation == null) return 0;
            int steps = rotationSteps(worldRotation);
            return PrefabLoader.rotateBlockRotation(doorNativeRotation, steps);
        }

        /** Steps to rotate native-local coords to match this building's world orientation. */
        public int rotationSteps(int worldRotation) {
            return (worldRotation - anchorPrefabRotation + 4) % 4;
        }
    }

    private static final Map<String, Layout> CACHE = new HashMap<>();

    /** Returns the cached layout for the given building type, computing it if needed. */
    public static Layout get(String buildingType) {
        return CACHE.computeIfAbsent(buildingType, BuildingLayout::compute);
    }

    private static Layout compute(String type) {
        String prefabName    = BuildingType.getPrefabName(type);
        String anchorId      = BuildingType.getAnchorBlockId(type);
        int    anchorPrefabY = BuildingType.getAnchorPrefabY(type);

        if (prefabName == null) return fallback();

        // Read the anchor's own rotation from the prefab — needed to compute rotationSteps later.
        int anchorPrefabRotation = PrefabLoader.readAnchorRotation(prefabName, anchorId, anchorPrefabY);

        // Native local coords: (0,0,0) = anchor position, no world rotation applied.
        // Using loadNativeLocal so the anchor's own prefab rotation doesn't flip the coords.
        List<BlockPlacer.BlockEntry> blocks =
                PrefabLoader.loadNativeLocal(prefabName, anchorId, anchorPrefabY);

        if (blocks.isEmpty()) {
            LOGGER.warning("BuildingLayout: no blocks loaded for type=" + type);
            return fallback();
        }

        // loadNativeLocal returns b.y() = anchorPrefabY + ly (not ly itself).
        // Convert to true local Y: ly = b.y() - anchorPrefabY.

        // Find door/gate block (filler=0 only, lower block of a 2-tall door).
        BlockPlacer.BlockEntry doorEntry = blocks.stream()
                .filter(b -> isClosedDoor(b.blockType()))
                .findFirst()
                .orElse(null);

        String openBlock  = doorEntry != null ? openVariant(doorEntry.blockType())  : null;
        String closeBlock = doorEntry != null ? closeVariant(doorEntry.blockType()) : null;
        // True local Y of the door block relative to anchor.
        int doorTrueLY = doorEntry != null ? (doorEntry.y() - anchorPrefabY) : 0;

        // Interior center: average X/Z of walkable floor blocks.
        // Iterate over true ly values: 0 (same height as anchor), -1, -2 (farm: Scarecrow is 2 above ground).
        int centerLX = 0, centerLZ = 0, floorLY = 0;
        for (int tryLy : new int[]{0, -1, -2}) {
            int targetY = tryLy + anchorPrefabY; // b.y() value that corresponds to this ly
            double sumX = 0, sumZ = 0;
            int count = 0;
            for (BlockPlacer.BlockEntry b : blocks) {
                if (b.y() != targetY) continue;
                if (!isWalkable(b.blockType())) continue;
                sumX += b.x();
                sumZ += b.z();
                count++;
            }
            if (count > 0) {
                centerLX = (int) Math.round(sumX / count);
                centerLZ = (int) Math.round(sumZ / count);
                floorLY  = tryLy;
                break;
            }
        }

        Layout layout = new Layout(
                centerLX, centerLZ, floorLY, anchorPrefabRotation,
                doorEntry != null ? doorEntry.x() : null,
                doorEntry != null ? doorTrueLY : null,
                doorEntry != null ? doorEntry.z() : null,
                doorEntry != null ? doorEntry.rotation() : null,
                openBlock, closeBlock);

        LOGGER.info(String.format("BuildingLayout computed for %s: center=(%d,%d)%s",
                type, centerLX, centerLZ,
                layout.hasDoor()
                        ? " door=(" + layout.doorLX() + "," + layout.doorLY() + "," + layout.doorLZ() + ")"
                        : " no-door"));
        return layout;
    }

    private static boolean isClosedDoor(String id) {
        // PrefabLoader returns base block ID (e.g. "Furniture_Village_Door"), not state-variant.
        return id != null && id.contains("Door") && !id.contains("Trapdoor");
    }

    private static String openVariant(String id) {
        String base = stripStateVariant(id);
        return "*" + base + "_State_Definitions_OpenDoorIn";
    }

    private static String closeVariant(String id) {
        String base = stripStateVariant(id);
        return "*" + base + "_State_Definitions_CloseDoorIn";
    }

    private static String stripStateVariant(String id) {
        String s = id.startsWith("*") ? id.substring(1) : id;
        int idx = s.indexOf("_State_Definitions_");
        return idx != -1 ? s.substring(0, idx) : s;
    }

    private static boolean isWalkable(String id) {
        if (id == null || id.isBlank()) return false;
        if (id.equals("Empty") || id.equals("Filter_Air_Block")) return false;
        // Structural/decoration blocks that are not floor
        String lower = id.toLowerCase();
        return !lower.contains("fence") && !lower.contains("wall")
                && !lower.contains("door") && !lower.contains("gate")
                && !lower.contains("roof") && !lower.contains("beam")
                && !lower.contains("trunk") && !lower.contains("leaves")
                && !lower.contains("scarecrow") && !lower.contains("brazier")
                && !lower.contains("counter") && !lower.contains("chest")
                && !lower.contains("crop") && !lower.contains("plant");
    }

    private static Layout fallback() {
        return new Layout(0, 0, 0, 0, null, null, null, null, null, null);
    }
}
