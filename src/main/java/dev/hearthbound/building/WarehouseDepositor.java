package dev.hearthbound.building;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.hearthbound.village.BuildingRecord;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Deposits and withdraws items from Warehouse Storage Chests.
 * Chest offsets mirror StorageChestReader — same four chests, same rotation math.
 */
public final class WarehouseDepositor {

    private static final Logger LOGGER = Logger.getLogger(WarehouseDepositor.class.getName());

    // Food items villagers can plausibly eat raw. Wheat/rice and bulky cooking crops are
    // intentionally excluded until we add prepared food recipes.
    public static final String[] FOOD_ITEMS = {
        "Plant_Crop_Carrot_Item",
        "Plant_Crop_Potato_Item",
        "Plant_Crop_Lettuce_Item",
        "Plant_Crop_Corn_Item",
        "Plant_Crop_Turnip_Item",
        "Plant_Crop_Cauliflower_Item",
        "Plant_Crop_Tomato_Item",
        "Plant_Crop_Onion_Item",
        "Plant_Crop_Chilli_Item",
    };

    // Same offsets as StorageChestReader (relative to Counter anchor, prefab-local before rotation)
    private static final int[][] CHEST_OFFSETS = {
        { -1,  0, -6 },
        {  1,  0, -7 },
        {  4,  0, -7 },
        {  5,  0, -5 },
    };

    private WarehouseDepositor() {}

    /**
     * Tries to add one unit of {@code itemId} to any warehouse chest that has capacity.
     * @return true if deposited successfully
     */
    public static boolean deposit(World world, BuildingRecord warehouse, String itemId) {
        int anchorX = warehouse.getPosX();
        int anchorY = warehouse.getPosY();
        int anchorZ = warehouse.getPosZ();
        int rotationSteps = warehouse.getRotation() % 4;

        for (int[] offset : CHEST_OFFSETS) {
            int dx = offset[0];
            int dy = offset[1];
            int dz = offset[2];

            for (int i = 0; i < rotationSteps; i++) {
                int tmp = dx;
                dx = dz;
                dz = -tmp;
            }

            int wx = anchorX + dx;
            int wy = anchorY + dy;
            int wz = anchorZ + dz;

            if (tryDepositAt(world, wx, wy, wz, itemId)) return true;
        }

        LOGGER.fine("WarehouseDepositor: no room for " + itemId + " in warehouse at "
                + anchorX + "," + anchorY + "," + anchorZ);
        return false;
    }

    private static boolean tryDepositAt(World world, int x, int y, int z, String itemId) {
        try {
            WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(x, z));
            if (chunk == null) return false;

            ItemContainerBlock containerBlock = getContainerBlock(world, chunk, x, y, z);
            if (containerBlock == null) return false;

            ItemContainer inv = containerBlock.getItemContainer();
            if (inv == null) return false;

            // succeeded() can be true even when nothing was actually inserted (stack returned as remainder).
            // Pattern from FH_CompanionNPCs: only treat as deposited if remainder is empty.
            var result = inv.addItemStack(new ItemStack(itemId, 1));
            if (!result.succeeded()) return false;
            ItemStack remainder = result.getRemainder();
            return remainder == null || remainder.isEmpty();
        } catch (Exception e) {
            LOGGER.warning("WarehouseDepositor: error at " + x + "," + y + "," + z + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Tries to withdraw one unit of any food item from warehouse chests.
     * Checks FOOD_ITEMS in priority order, takes from first chest that has any.
     * @return the item ID that was taken, or null if no food available
     */
    public static String withdrawFood(World world, BuildingRecord warehouse) {
        int[][] rotatedOffsets = buildRotatedOffsets(warehouse.getRotation() % 4);
        int anchorX = warehouse.getPosX();
        int anchorY = warehouse.getPosY();
        int anchorZ = warehouse.getPosZ();

        for (String foodId : FOOD_ITEMS) {
            for (int[] offset : rotatedOffsets) {
                int wx = anchorX + offset[0];
                int wy = anchorY + offset[1];
                int wz = anchorZ + offset[2];
                if (tryWithdrawAt(world, wx, wy, wz, foodId)) return foodId;
            }
        }
        return null;
    }

    public static String withdrawRandomFood(World world, BuildingRecord warehouse, Random random) {
        List<String> foods = new ArrayList<>(List.of(FOOD_ITEMS));
        Collections.shuffle(foods, random != null ? random : new Random());
        int[][] rotatedOffsets = buildRotatedOffsets(warehouse.getRotation() % 4);
        int anchorX = warehouse.getPosX();
        int anchorY = warehouse.getPosY();
        int anchorZ = warehouse.getPosZ();

        for (String foodId : foods) {
            for (int[] offset : rotatedOffsets) {
                int wx = anchorX + offset[0];
                int wy = anchorY + offset[1];
                int wz = anchorZ + offset[2];
                if (tryWithdrawAt(world, wx, wy, wz, foodId)) return foodId;
            }
        }
        return null;
    }

    private static boolean tryWithdrawAt(World world, int x, int y, int z, String itemId) {
        try {
            WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(x, z));
            if (chunk == null) return false;
            ItemContainerBlock containerBlock = getContainerBlock(world, chunk, x, y, z);
            if (containerBlock == null) return false;
            ItemContainer inv = containerBlock.getItemContainer();
            if (inv == null) return false;
            var result = inv.removeItemStack(new ItemStack(itemId, 1), true, true);
            return result.succeeded();
        } catch (Exception e) {
            LOGGER.warning("WarehouseDepositor.withdraw: error at " + x + "," + y + "," + z + ": " + e.getMessage());
            return false;
        }
    }

    private static int[][] buildRotatedOffsets(int rotationSteps) {
        int[][] result = new int[CHEST_OFFSETS.length][3];
        for (int i = 0; i < CHEST_OFFSETS.length; i++) {
            int dx = CHEST_OFFSETS[i][0];
            int dy = CHEST_OFFSETS[i][1];
            int dz = CHEST_OFFSETS[i][2];
            for (int r = 0; r < rotationSteps; r++) {
                int tmp = dx; dx = dz; dz = -tmp;
            }
            result[i] = new int[]{dx, dy, dz};
        }
        return result;
    }

    private static ItemContainerBlock getContainerBlock(World world, WorldChunk chunk, int x, int y, int z) {
        Ref<ChunkStore> blockRef = chunk.getBlockComponentEntity(x, y, z);
        if (blockRef == null) return null;
        return world.getChunkStore().getStore().getComponent(
                blockRef, BlockModule.get().getItemContainerBlockComponentType());
    }
}
