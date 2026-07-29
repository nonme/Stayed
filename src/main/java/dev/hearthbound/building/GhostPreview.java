package dev.hearthbound.building;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.NonSerialized;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.player.ClearDebugShapes;
import com.hypixel.hytale.protocol.packets.player.DisplayDebug;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.BlockEntity;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.UUID;
import java.util.List;
/**
 * Spawns transient BlockEntity markers for building ghost previews.
 * Never writes to the chunk — no snapshot, no restore, no cascade, no collision holes.
 * NonSerialized ensures entities vanish on server restart without any cleanup.
 */
public final class GhostPreview {

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("build.ghost");
    private static final int MAX_ENTITIES = 4096;

    private GhostPreview() {}

    /**
     * Spawns one BlockEntity per structural block in the plan.
     * Skips doors, trapdoors, decorative blocks (Deco_/Furniture_), and plants (Plant_).
     * Must be called on the world thread.
     */
    public static List<Ref<EntityStore>> show(Store<EntityStore> store,
                                               List<BlockPlacer.BlockEntry> plan) {
        var blockTypeMap = BlockType.getAssetMap();
        EntityStore entityStore = store.getExternalData();
        List<Ref<EntityStore>> refs = new ArrayList<>();
        int skipped = 0;

        for (BlockPlacer.BlockEntry entry : plan) {
            if (refs.size() >= MAX_ENTITIES) break;

            String blockType = entry.blockType();
            if (isDoorBlock(blockType) || isDecorBlock(blockType) || isPlantBlock(blockType)) {
                skipped++;
                continue;
            }

            // Strip state variant suffix and '*' prefix to get the placeable block ID.
            String blockId = normalizeBlockId(blockType);

            // Skip filler blocks (empty/air markers used internally by PrefabLoader).
            if (blockId.isEmpty() || blockId.equals("Empty") || blockId.equals("Editor_Empty")) {
                skipped++;
                continue;
            }

            // Verify block exists in the asset registry — unknown IDs would cause a client crash.
            if (blockTypeMap.getAsset(blockId) == null) {
                skipped++;
                continue;
            }

            RotationTuple rt = RotationTuple.get(entry.rotation() & 0xFF);
            // BlockEntity meshes use -Z forward; block RotationTuple yaw is chunk-space aligned.
            // Adding π aligns the preview mesh to match the block's actual world facing.
            float yawRad = (float) (rt.yaw().getRadians() + Math.PI);
            Vector3f euler = new Vector3f(
                    (float) rt.pitch().getRadians(),
                    yawRad,
                    (float) rt.roll().getRadians());
            // BlockEntity canonical offset: +0.5 on X and Z from block min corner, Y unchanged.
            Vector3d pos = new Vector3d(entry.x() + 0.5, entry.y(), entry.z() + 0.5);

            try {
                Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
                holder.addComponent(NetworkId.getComponentType(),
                        new NetworkId(entityStore.takeNextNetworkId()));
                holder.addComponent(EntityStore.REGISTRY.getNonSerializedComponentType(),
                        NonSerialized.get());
                holder.addComponent(BlockEntity.getComponentType(), new BlockEntity(blockId));
                holder.addComponent(TransformComponent.getComponentType(),
                        new TransformComponent(pos, euler));
                holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(euler));
                holder.ensureComponent(UUIDComponent.getComponentType());
                Ref<EntityStore> ref = store.addEntity(holder, AddReason.SPAWN);
                if (ref != null) refs.add(ref);
            } catch (RuntimeException e) {
                LOG.warn("GhostPreview: failed to spawn entity for " + blockId + ": " + e.getMessage());
            }
        }

        LOG.info("[Ghost] show: spawned=" + refs.size() + " skipped=" + skipped
                + " planSize=" + plan.size());
        return refs;
    }

    /**
     * Removes all preview entities. Safe to call with null or empty list.
     * Must be called on the world thread.
     */
    public static void clear(Store<EntityStore> store, List<Ref<EntityStore>> refs) {
        if (refs == null || refs.isEmpty()) return;
        int removed = 0;
        for (Ref<EntityStore> ref : refs) {
            if (ref != null && ref.isValid()) {
                try {
                    store.removeEntity(ref, RemoveReason.REMOVE);
                    removed++;
                } catch (RuntimeException e) {
                    LOG.warn("GhostPreview: failed to remove entity: " + e.getMessage());
                }
            }
        }
        refs.clear();
        LOG.info("[Ghost] clear: removed=" + removed);
    }

    private static boolean isDoorBlock(String blockType) {
        return blockType.contains("Door") || blockType.contains("Trapdoor");
    }

    private static boolean isDecorBlock(String blockType) {
        String base = blockType.startsWith("*") ? blockType.substring(1) : blockType;
        return base.startsWith("Deco_");
    }

    private static boolean isPlantBlock(String blockType) {
        String base = blockType.startsWith("*") ? blockType.substring(1) : blockType;
        return base.startsWith("Plant_");
    }

    private static String normalizeBlockId(String blockType) {
        String base = blockType.startsWith("*") ? blockType.substring(1) : blockType;
        int idx = base.indexOf("_State_Definitions_");
        return idx != -1 ? base.substring(0, idx) : base;
    }

    /**
     * Sends a wireframe bounding box covering the full plan footprint to the player.
     * Duration is 10 seconds — call repeatedly via TickScheduler to keep it alive.
     * Must be called on the world thread.
     */
    public static void sendBoundingBox(UUID playerUuid, World world, List<BlockPlacer.BlockEntry> plan) {
        if (plan == null || plan.isEmpty()) return;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPlacer.BlockEntry e : plan) {
            if (e.x() < minX) minX = e.x();
            if (e.y() < minY) minY = e.y();
            if (e.z() < minZ) minZ = e.z();
            if (e.x() > maxX) maxX = e.x();
            if (e.y() > maxY) maxY = e.y();
            if (e.z() > maxZ) maxZ = e.z();
        }
        float sizeX = maxX - minX + 1;
        float sizeY = maxY - minY + 1;
        float sizeZ = maxZ - minZ + 1;
        float cx = minX + sizeX / 2.0f;
        float cy = minY + sizeY / 2.0f;
        float cz = minZ + sizeZ / 2.0f;
        // Column-major 4x4 matrix: diagonal = scale, last column = translation
        float[] transform = {sizeX, 0, 0, 0,  0, sizeY, 0, 0,  0, 0, sizeZ, 0,  cx, cy, cz, 1};
        com.hypixel.hytale.protocol.Vector3f color = new com.hypixel.hytale.protocol.Vector3f(1.0f, 0.75f, 0.2f);
        DisplayDebug packet = new DisplayDebug(DebugShape.Cube, transform, color, 99999.0f, (byte) 1, new float[16], 0.1f);
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef != null) {
            playerRef.getPacketHandler().write((ToClientPacket) packet);
        }
    }

    /** Clears all debug shapes for the player. Must be called on the world thread. */
    public static void clearBoundingBox(UUID playerUuid, World world) {
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef != null) {
            playerRef.getPacketHandler().write((ToClientPacket) new ClearDebugShapes());
        }
    }
}
