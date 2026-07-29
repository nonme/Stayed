package dev.hearthbound.building;

/**
 * Resolves the right state-variant prefab name for a given door/gate state-block ID.
 *
 * <p>Each (block family, open/close, in/out) combination has a one-block prefab under
 * {@code Server/Prefabs/}. Placing the state-variant directly via {@code world.setBlock}
 * or {@code chunk.setBlock} loses rotation, so we always go through {@code BlockSelection.placeNoReturn}.
 * That requires a prefab per variant — one fixed prefab per state would replace fence-gates
 * with the village door (the warehouse/tavern bug that motivated this resolver).
 */
public final class DoorPrefabResolver {

    private DoorPrefabResolver() {}

    /**
     * Picks the matching prefab name (without {@code .prefab.json}) for the given state-block ID.
     * Returns {@code null} for unrecognised blocks — callers must skip placement in that case
     * rather than fall back to a default door prefab, otherwise the bug returns.
     */
    public static String resolve(String stateBlockId) {
        if (stateBlockId == null || stateBlockId.isBlank()) return null;
        String id = stateBlockId.startsWith("*") ? stateBlockId.substring(1) : stateBlockId;

        boolean open = id.contains("OpenDoor");
        boolean closed = id.contains("CloseDoor");
        if (!open && !closed) return null;
        boolean out = id.contains("DoorOut");

        if (id.contains("Furniture_Village_Door")) {
            return doorVariant("door", open, out);
        }
        if (id.contains("Wood_Softwood_Fence_Gate")) {
            return doorVariant("gate_softwood", open, out);
        }
        if (id.contains("Wood_Hardwood_Fence_Gate")) {
            return doorVariant("gate_hardwood", open, out);
        }
        return null;
    }

    private static String doorVariant(String base, boolean open, boolean out) {
        return base + (open ? "_open" : "_closed") + (out ? "_out" : "_in");
    }
}
