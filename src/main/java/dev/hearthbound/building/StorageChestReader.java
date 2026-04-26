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
import java.util.logging.Logger;

/**
 * Reads ItemContainerBlock contents from all Hearthbound_Storage_Chest blocks
 * that belong to a completed Warehouse.
 *
 * Chest positions are derived from known prefab-relative offsets (anchor = Counter),
 * so we always hit the exact right blocks instead of scanning a radius.
 */
public class StorageChestReader {

    private static final Logger LOGGER = Logger.getLogger(StorageChestReader.class.getName());

    /**
     * Prefab-local offsets of each Storage Chest master block relative to the Counter anchor.
     * Anchor is at prefab (-2, 1, 3) (no-filler Counter). Chests are at prefabY=1.
     * Offsets = chest_prefab_pos - anchor_prefab_pos, but Y is same level (dy=0).
     *
     * Chest prefab positions (filler=0): (-3,1,-3), (-1,1,-4), (2,1,-4), (3,1,-2)
     * Anchor prefab position:            (-2,1,3)
     * dx = chest.x - anchor.x,  dz = chest.z - anchor.z
     */
    private static final int[][] CHEST_OFFSETS = {
        { -3 - (-2),  1 - 1,  -3 - 3 },   // (-1, 0, -6)
        { -1 - (-2),  1 - 1,  -4 - 3 },   //  (1, 0, -7)
        {  2 - (-2),  1 - 1,  -4 - 3 },   //  (4, 0, -7)
        {  3 - (-2),  1 - 1,  -2 - 3 },   //  (5, 0, -5)
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

        LOGGER.info("StorageChestReader: read " + totals.size() + " unique items from warehouse at "
                + anchorX + "," + anchorY + "," + anchorZ + " rotation=" + record.getRotation());
        return totals;
    }

    private static void readChestAt(World world, int x, int y, int z, Map<String, Integer> totals) {
        try {
            WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(x, z));
            if (chunk == null) {
                LOGGER.warning("StorageChestReader: chunk not loaded at " + x + "," + y + "," + z);
                return;
            }

            // getBlockComponentHolder covers both live Ref (ticking) and unspawned Holder cases
            Holder<ChunkStore> holder = chunk.getBlockComponentHolder(x, y, z);
            if (holder == null) {
                // Fallback to live Ref
                Ref<ChunkStore> blockRef = chunk.getBlockComponentEntity(x, y, z);
                if (blockRef == null) {
                    LOGGER.warning("StorageChestReader: no block entity at " + x + "," + y + "," + z);
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
                LOGGER.warning("StorageChestReader: no ItemContainerBlock at " + x + "," + y + "," + z);
                return;
            }

            readContainer(container, totals);
        } catch (Exception e) {
            LOGGER.warning("StorageChestReader: error reading chest at " + x + "," + y + "," + z + ": " + e);
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
