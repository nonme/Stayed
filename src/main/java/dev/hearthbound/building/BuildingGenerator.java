package dev.hearthbound.building;

import dev.hearthbound.village.BuildingType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates building block layouts programmatically.
 * These are placeholder designs — will be replaced with Asset Editor prefabs later.
 * Blocks are returned sorted bottom-to-top (Y ascending) for natural build order.
 *
 * Rotation: 0=N (door -Z), 1=E (door +X), 2=S (door +Z), 3=W (door -X)
 * Matches NESW VariantRotation index from block placement.
 */
public class BuildingGenerator {

    /**
     * Generate block list for a building type at the given anchor position with rotation.
     * The anchor block is NOT included — it's already placed by the player.
     *
     * @param type     building type constant
     * @param anchorX  anchor block X
     * @param anchorY  anchor block Y (ground level)
     * @param anchorZ  anchor block Z
     * @param rotation 0=N, 1=E, 2=S, 3=W
     * @return list of blocks to place, sorted by Y then X then Z
     */
    public static List<BlockPlacer.BlockEntry> generate(String type, int anchorX, int anchorY, int anchorZ, int rotation) {
        // Generate with default rotation (door on -Z = North)
        List<BlockPlacer.BlockEntry> base = generateBase(type, anchorX, anchorY, anchorZ);
        if (rotation == 0 || base.isEmpty()) return base;
        return applyRotation(base, anchorX, anchorZ, rotation);
    }

    /** Overload without rotation for backwards compatibility. */
    public static List<BlockPlacer.BlockEntry> generate(String type, int anchorX, int anchorY, int anchorZ) {
        return generate(type, anchorX, anchorY, anchorZ, 0);
    }

    private static List<BlockPlacer.BlockEntry> generateBase(String type, int anchorX, int anchorY, int anchorZ) {
        return switch (type) {
            case BuildingType.TOWN_HALL -> generateTownHall(anchorX, anchorY, anchorZ);
            case BuildingType.WAREHOUSE -> generateWarehouse(anchorX, anchorY, anchorZ);
            case BuildingType.HOUSE_HUMAN -> generateHumanHouse(anchorX, anchorY, anchorZ);
            case BuildingType.HOUSE_KWEEBEC -> generateKweebecHouse(anchorX, anchorY, anchorZ);
            case BuildingType.HOUSE_TRORK -> generateTrorkHouse(anchorX, anchorY, anchorZ);
            default -> List.of();
        };
    }

    /**
     * Rotate block positions around the anchor point (cx, cz).
     * rotation: 1=90° CW (E), 2=180° (S), 3=270° CW (W)
     */
    private static List<BlockPlacer.BlockEntry> applyRotation(
            List<BlockPlacer.BlockEntry> blocks, int cx, int cz, int rotation) {
        List<BlockPlacer.BlockEntry> rotated = new ArrayList<>(blocks.size());
        for (BlockPlacer.BlockEntry b : blocks) {
            int dx = b.x() - cx;
            int dz = b.z() - cz;
            int rx, rz;
            switch (rotation) {
                case 1 -> { rx = -dz; rz = dx; }   // 90° CW: (dx,dz) → (-dz, dx)
                case 2 -> { rx = -dx; rz = -dz; }   // 180°
                case 3 -> { rx = dz; rz = -dx; }     // 270° CW
                default -> { rx = dx; rz = dz; }
            }
            rotated.add(new BlockPlacer.BlockEntry(cx + rx, b.y(), cz + rz, b.blockType()));
        }
        return rotated;
    }

    /**
     * Get the door position offset (relative to anchor) for a building, after rotation.
     * Used to place elf near the door after founding.
     */
    public static int[] getDoorOffset(String type, int rotation) {
        // Default door offset (door on -Z side = North)
        int dx = 0, dz;
        switch (type) {
            case BuildingType.TOWN_HALL -> dz = -4; // one block in front of door
            case BuildingType.WAREHOUSE -> dz = -3;
            default -> dz = -2;
        }
        // Rotate the offset
        int rx, rz;
        switch (rotation) {
            case 1 -> { rx = -dz; rz = dx; }
            case 2 -> { rx = -dx; rz = -dz; }
            case 3 -> { rx = dz; rz = -dx; }
            default -> { rx = dx; rz = dz; }
        }
        return new int[]{rx, rz};
    }

    /**
     * Returns the required resources (item ID → count) for a building type.
     * Calculated from the actual build plan to ensure accuracy.
     */
    public static Map<String, Integer> getRequiredResources(String type) {
        // Generate at origin with no rotation — we just need block counts
        List<BlockPlacer.BlockEntry> plan = generateBase(type, 0, 0, 0);
        Map<String, Integer> resources = new LinkedHashMap<>();
        for (BlockPlacer.BlockEntry entry : plan) {
            resources.merge(entry.blockType(), 1, Integer::sum);
        }
        return resources;
    }

    // ========== Building generators (door always on -Z side, rotation applied later) ==========

    private static List<BlockPlacer.BlockEntry> generateTownHall(int cx, int cy, int cz) {
        List<BlockPlacer.BlockEntry> blocks = new ArrayList<>();
        String wall = "Rock_Chalk_Brick";
        String floor = "Wood_Softwood_Planks";
        String roof = "Wood_Softwood_Roof_Flat";
        String pillar = "Wood_Birch_Trunk";

        int x1 = cx - 3, x2 = cx + 3;
        int z1 = cz - 3, z2 = cz + 3;

        // Floor (y = cy)
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                if (x == cx && z == cz) continue; // anchor block already placed
                blocks.add(new BlockPlacer.BlockEntry(x, cy, z, floor));
            }
        }

        // Walls (y = cy+1 to cy+3, height 3)
        for (int y = cy + 1; y <= cy + 3; y++) {
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    boolean isEdgeX = (x == x1 || x == x2);
                    boolean isEdgeZ = (z == z1 || z == z2);
                    if (!isEdgeX && !isEdgeZ) continue;

                    // Door opening: front center (-Z side), 2 blocks high
                    if (z == z1 && x == cx && y <= cy + 2) continue;

                    // Corner pillars
                    if (isEdgeX && isEdgeZ) {
                        blocks.add(new BlockPlacer.BlockEntry(x, y, z, pillar));
                    } else {
                        blocks.add(new BlockPlacer.BlockEntry(x, y, z, wall));
                    }
                }
            }
        }

        // Roof (y = cy+4)
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                blocks.add(new BlockPlacer.BlockEntry(x, cy + 4, z, roof));
            }
        }

        return blocks;
    }

    private static List<BlockPlacer.BlockEntry> generateWarehouse(int cx, int cy, int cz) {
        List<BlockPlacer.BlockEntry> blocks = new ArrayList<>();
        String wall = "Rock_Stone_Cobble";
        String floor = "Rock_Stone_Cobble";
        String roof = "Wood_Softwood_Roof_Flat";

        int x1 = cx - 2, x2 = cx + 2;
        int z1 = cz - 2, z2 = cz + 2;

        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                if (x == cx && z == cz) continue;
                blocks.add(new BlockPlacer.BlockEntry(x, cy, z, floor));
            }
        }

        for (int y = cy + 1; y <= cy + 3; y++) {
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    boolean isEdge = (x == x1 || x == x2 || z == z1 || z == z2);
                    if (!isEdge) continue;
                    if (z == z1 && x == cx && y <= cy + 2) continue;
                    blocks.add(new BlockPlacer.BlockEntry(x, y, z, wall));
                }
            }
        }

        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                blocks.add(new BlockPlacer.BlockEntry(x, cy + 4, z, roof));
            }
        }

        return blocks;
    }

    private static List<BlockPlacer.BlockEntry> generateHumanHouse(int cx, int cy, int cz) {
        List<BlockPlacer.BlockEntry> blocks = new ArrayList<>();
        String wall = "Wood_Softwood_Planks";
        String floor = "Rock_Stone_Cobble";
        String roof = "Wood_Softwood_Roof_Flat";

        int x1 = cx - 2, x2 = cx + 2;
        int z1 = cz - 2, z2 = cz + 2;

        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                if (x == cx && z == cz) continue;
                blocks.add(new BlockPlacer.BlockEntry(x, cy, z, floor));
            }
        }

        for (int y = cy + 1; y <= cy + 3; y++) {
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    boolean isEdge = (x == x1 || x == x2 || z == z1 || z == z2);
                    if (!isEdge) continue;
                    if (z == z1 && x == cx && y <= cy + 2) continue;
                    blocks.add(new BlockPlacer.BlockEntry(x, y, z, wall));
                }
            }
        }

        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                blocks.add(new BlockPlacer.BlockEntry(x, cy + 4, z, roof));
            }
        }

        return blocks;
    }

    private static List<BlockPlacer.BlockEntry> generateKweebecHouse(int cx, int cy, int cz) {
        List<BlockPlacer.BlockEntry> blocks = new ArrayList<>();
        String trunk = "Wood_Birch_Trunk";
        String leaves = "Plant_Leaves_Birch";

        for (int y = cy + 1; y <= cy + 4; y++) {
            blocks.add(new BlockPlacer.BlockEntry(cx, y, cz, trunk));
        }

        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = cz - 1; z <= cz + 1; z++) {
                if (x == cx && z == cz) continue;
                blocks.add(new BlockPlacer.BlockEntry(x, cy + 3, z, leaves));
            }
        }
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                if (x == cx && z == cz) continue;
                blocks.add(new BlockPlacer.BlockEntry(x, cy + 4, z, leaves));
            }
        }

        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = cz - 1; z <= cz + 1; z++) {
                if (x == cx && z == cz) continue;
                boolean isEdge = (x == cx - 1 || x == cx + 1 || z == cz - 1 || z == cz + 1);
                if (isEdge) {
                    if (z == cz - 1 && x == cx) continue;
                    blocks.add(new BlockPlacer.BlockEntry(x, cy + 1, z, trunk));
                }
            }
        }

        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = cz - 1; z <= cz + 1; z++) {
                if (x == cx && z == cz) continue;
                blocks.add(new BlockPlacer.BlockEntry(x, cy + 2, z, trunk));
            }
        }

        return blocks;
    }

    private static List<BlockPlacer.BlockEntry> generateTrorkHouse(int cx, int cy, int cz) {
        List<BlockPlacer.BlockEntry> blocks = new ArrayList<>();
        String wall = "Rock_Stone_Cobble";
        String floor = "Rock_Stone_Cobble";
        String roof = "Wood_Darkwood_Roof_Flat";

        int x1 = cx - 2, x2 = cx + 2;
        int z1 = cz - 2, z2 = cz + 2;

        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                if (x == cx && z == cz) continue;
                blocks.add(new BlockPlacer.BlockEntry(x, cy, z, floor));
            }
        }

        for (int y = cy + 1; y <= cy + 3; y++) {
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    boolean isEdge = (x == x1 || x == x2 || z == z1 || z == z2);
                    if (!isEdge) continue;
                    if (z == z1 && (x == cx || x == cx - 1) && y <= cy + 2) continue;
                    blocks.add(new BlockPlacer.BlockEntry(x, y, z, wall));
                }
            }
        }

        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                blocks.add(new BlockPlacer.BlockEntry(x, cy + 4, z, roof));
            }
        }

        return blocks;
    }
}
