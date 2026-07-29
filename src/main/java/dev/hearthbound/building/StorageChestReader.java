package dev.hearthbound.building;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.hearthbound.village.BuildingRecord;
import dev.hearthbound.village.BuildingType;

import java.util.LinkedHashMap;
import java.util.Map;
/**
 * Reads ItemContainerBlock contents from all Stayed_Storage_Chest blocks
 * that belong to a completed Warehouse.
 *
 * Chest positions are derived from known prefab-relative offsets (anchor = Counter),
 * so we always hit the exact right blocks instead of scanning a radius.
 */
public class StorageChestReader {

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("build.resource");
    /**
     * Prefab-local offsets of each Storage Chest master block relative to the Warehouse anchor.
     * Derived from Warehouse_lvl1_v2.prefab.json: anchor at (1,2,2).
     * Master chests (container=true, no filler): (-6,2,0), (-5,5,3), (4,5,3), (6,2,1)
     * dx = chest.x - anchor.x, dy = chest.y - anchor.y, dz = chest.z - anchor.z
     *
     * WarehouseDepositor references this array directly — update both together or only here.
     */
    static final int[][] CHEST_OFFSETS = {
        { -7,  0, -2 },
        { -6,  3,  1 },
        {  3,  3,  1 },
        {  5,  0, -1 },
    };

    /**
     * Returns aggregated contents of all warehouse storage chests.
     * record.rotation is the NESW rotation of the anchor block as placed in the world.
     * anchorPrefabRotation for Warehouse Counter is 0 (no rotation in prefab).
     */
    public static Map<String, Integer> readAll(World world, BuildingRecord record) {
        Map<String, Integer> totals = new LinkedHashMap<>();

        int anchorX = record.getPosX();
        int anchorY = record.getPosY();
        int anchorZ = record.getPosZ();
        // prefab anchor rotation = 0 (Counter has no rotation baked in)
        int rotationSteps = (record.getRotation() - 0 + 4) % 4;

        for (int[] offset : CHEST_OFFSETS) {
            int dx = offset[0];
            int dy = offset[1];
            int dz = offset[2];

            // Apply same rotation transform as PrefabLoader.extractBlocks
            for (int i = 0; i < rotationSteps; i++) {
                int tmp = dx;
                dx = dz;
                dz = -tmp;
            }

            int wx = anchorX + dx;
            int wy = anchorY + dy;
            int wz = anchorZ + dz;

            readChestAt(world, wx, wy, wz, totals);
        }

        LOG.info("StorageChestReader: read " + totals.size() + " unique items from warehouse at "
                + anchorX + "," + anchorY + "," + anchorZ + " rotation=" + record.getRotation());
        return totals;
    }

    private static void readChestAt(World world, int x, int y, int z, Map<String, Integer> totals) {
        try {
            WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(x, z));
            if (chunk == null) {
                LOG.warn("StorageChestReader: chunk not loaded at " + x + "," + y + "," + z);
                return;
            }

            // getBlockComponentHolder covers both live Ref (ticking) and unspawned Holder cases
            Holder<ChunkStore> holder = chunk.getBlockComponentHolder(x, y, z);
            if (holder == null) {
                // Fallback to live Ref
                Ref<ChunkStore> blockRef = chunk.getBlockComponentEntity(x, y, z);
                if (blockRef == null) {
                    LOG.warn("StorageChestReader: no block entity at " + x + "," + y + "," + z);
                    return;
                }
                ItemContainerBlock container = world.getChunkStore().getStore().getComponent(
                        blockRef, BlockModule.get().getItemContainerBlockComponentType());
                if (container != null) readContainer(container, totals);
                return;
            }

            ItemContainerBlock container = holder.getComponent(
                    BlockModule.get().getItemContainerBlockComponentType());
            if (container == null) {
                LOG.warn("StorageChestReader: no ItemContainerBlock at " + x + "," + y + "," + z);
                return;
            }

            readContainer(container, totals);
        } catch (Exception e) {
            LOG.warn("StorageChestReader: error reading chest at " + x + "," + y + "," + z + ": " + e);
        }
    }

    private static void readContainer(ItemContainerBlock container, Map<String, Integer> totals) {
        var inv = container.getItemContainer();
        for (short slot = 0; slot < inv.getCapacity(); slot++) {
            ItemStack stack = inv.getItemStack(slot);
            if (stack == null || stack.isEmpty()) continue;
            totals.merge(stack.getItemId(), stack.getQuantity(), Integer::sum);
        }
    }
}
