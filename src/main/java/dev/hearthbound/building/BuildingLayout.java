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

    /** Returns the cached layout for the given building type (variant 0), computing it if needed. */
    public static Layout get(String buildingType) {
        return get(buildingType, 0);
    }

    /**
     * Returns the cached layout for the given building type and prefab variant.
     * Different house variants have different interior centers and door positions, so each
     * variant gets its own cached Layout.
     */
    public static Layout get(String buildingType, int variant) {
        String key = buildingType + ":" + variant;
        return CACHE.computeIfAbsent(key, k -> compute(buildingType, variant));
    }

    private static Layout compute(String type, int variant) {
        String prefabName    = BuildingType.getPrefabName(type, variant);
        String anchorId      = BuildingType.getAnchorBlockId(type);
        int    anchorPrefabY = BuildingType.getAnchorPrefabY(type, variant);

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

        // Find door/gate block — skip for open-air buildings (mine, sawmill) that have
        // decorative doors the NPC should not try to open/enter.
        // Read directly from the prefab via findDoor: PrefabLoader.extractBlocks drops doors
        // (they're placed atomically by the final replaceWithPrefab swap, not block-by-block),
        // so scanning the build plan would always return null.
        boolean hasManagedDoor = !BuildingType.MINE.equals(type) && !BuildingType.SAWMILL.equals(type);
        PrefabLoader.DoorInfo doorInfo = hasManagedDoor
                ? PrefabLoader.findDoor(prefabName, anchorId, anchorPrefabY)
                : null;

        String openBlock  = doorInfo != null ? openVariant(doorInfo.blockId())  : null;
        String closeBlock = doorInfo != null ? closeVariant(doorInfo.blockId()) : null;

        // Interior center: use hardcoded override if available, otherwise auto-detect.
        int centerLX = 0, centerLZ = 0, floorLY = 0;
        int[] workOverride = dev.hearthbound.village.BuildingType.getWorkPointOverride(type);
        if (workOverride != null) {
            centerLX = workOverride[0];
            floorLY  = workOverride[1];
            centerLZ = workOverride[2];
        } else {
            // Iterate over true ly values: 0 (anchor level), -1, -2 (farm: scarecrow 2 above ground).
            for (int tryLy : new int[]{0, -1, -2}) {
                int targetY = tryLy + anchorPrefabY;
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
        }

        Layout layout = new Layout(
                centerLX, centerLZ, floorLY, anchorPrefabRotation,
                doorInfo != null ? doorInfo.lx() : null,
                doorInfo != null ? doorInfo.ly() : null,
                doorInfo != null ? doorInfo.lz() : null,
                doorInfo != null ? doorInfo.nativeRotation() : null,
                openBlock, closeBlock);

        LOGGER.info(String.format("BuildingLayout computed for %s: center=(%d,%d)%s",
                type, centerLX, centerLZ,
                layout.hasDoor()
                        ? " door=(" + layout.doorLX() + "," + layout.doorLY() + "," + layout.doorLZ() + ")"
                        : " no-door"));
        return layout;
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
        // Skip anchor/custom blocks
        if (id.startsWith("Stayed_")) return false;
        String lower = id.toLowerCase();
        return !lower.contains("fence") && !lower.contains("wall")
                && !lower.contains("door") && !lower.contains("gate")
                && !lower.contains("roof") && !lower.contains("beam")
                && !lower.contains("trunk") && !lower.contains("leaves")
                && !lower.contains("scarecrow") && !lower.contains("brazier")
                && !lower.contains("counter") && !lower.contains("chest")
                && !lower.contains("crop") && !lower.contains("plant")
                && !lower.contains("furniture") && !lower.contains("rubble")
                && !lower.contains("branch") && !lower.contains("deco")
                && !lower.contains("lantern") && !lower.contains("rope")
                && !lower.contains("sign") && !lower.contains("ore_");
    }

    /**
     * World position of the Town Hall interior stand-point for {@code village}.
     * This is where the elf ends up after finishing any build (see
     * {@code BuildingSystem.onBuildingComplete}) and the canonical recall target
     * for Aelin and any villager pulled back from the Town Hall UI.
     *
     * <p>Returns {@code null} if the village has no founding stone yet — callers
     * must fall back to a safe spot of their own choosing in that case.
     */
    public static double[] townHallStandPoint(dev.hearthbound.village.VillageData village) {
        if (village == null || !village.isFounded()) return null;
        Layout layout = get(dev.hearthbound.village.BuildingType.TOWN_HALL);
        int steps = layout.rotationSteps(village.getRotation());
        int[] center = rotateLocalOffset(layout.centerLX(), layout.floorLY(), layout.centerLZ(), steps);
        return new double[]{
                village.getFoundingStoneX() + center[0] + 0.5,
                village.getFoundingStoneY() + layout.floorLY() + 1.0,
                village.getFoundingStoneZ() + center[2] + 0.5
        };
    }

    /** Rotates native-local coords (lx, ly, lz) by the given number of 90° CCW steps. */
    public static int[] rotateLocalOffset(int lx, int ly, int lz, int steps) {
        steps = ((steps % 4) + 4) % 4;
        for (int i = 0; i < steps; i++) {
            int tmp = lx;
            lx = lz;
            lz = -tmp;
        }
        return new int[]{lx, ly, lz};
    }

    private static Layout fallback() {
        return new Layout(0, 0, 0, 0, null, null, null, null, null, null);
    }
}
