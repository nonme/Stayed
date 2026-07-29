/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.ComponentAccessor
 *  com.hypixel.hytale.component.ComponentType
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.inventory.InventoryComponent
 *  com.hypixel.hytale.server.core.inventory.ItemStack
 *  com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer
 *  com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction
 */
package com.kyuubisoft.core.util;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import java.util.logging.Logger;

public class ItemUtils {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core");

    public static boolean grantItem(Player player, String itemId, int amount) {
        if (itemId == null || itemId.isBlank() || amount <= 0) {
            return false;
        }
        try {
            ItemStack itemStack = new ItemStack(itemId, amount);
            if (!itemStack.isValid()) {
                LOGGER.warning("Invalid item ID: " + itemId);
                return false;
            }
            Ref ref = player.getReference();
            Store store = ref.getStore();
            CombinedItemContainer container = InventoryComponent.getCombined((ComponentAccessor)store, (Ref)ref, (ComponentType[])InventoryComponent.HOTBAR_FIRST);
            if (container == null) {
                return false;
            }
            ItemStackTransaction transaction = container.addItemStack(itemStack);
            return transaction != null && (transaction.getRemainder() == null || transaction.getRemainder().isEmpty());
        }
        catch (Exception e) {
            LOGGER.warning("Failed to grant item " + itemId + ": " + e.getMessage());
            return false;
        }
    }

    public static int countItem(Player player, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return 0;
        }
        try {
            Ref ref = player.getReference();
            Store store = ref.getStore();
            CombinedItemContainer combined = InventoryComponent.getCombined((ComponentAccessor)store, (Ref)ref, (ComponentType[])InventoryComponent.HOTBAR_FIRST);
            if (combined == null) {
                return 0;
            }
            return combined.countItemStacks(stack -> itemId.equals(stack.getItemId()));
        }
        catch (Exception e) {
            LOGGER.fine("Failed to count item " + itemId + ": " + e.getMessage());
            return 0;
        }
    }

    public static boolean removeItem(Player player, String itemId, int amount) {
        if (itemId == null || itemId.isBlank() || amount <= 0) {
            return false;
        }
        try {
            Ref ref = player.getReference();
            Store store = ref.getStore();
            CombinedItemContainer combined = InventoryComponent.getCombined((ComponentAccessor)store, (Ref)ref, (ComponentType[])InventoryComponent.HOTBAR_FIRST);
            if (combined == null) {
                return false;
            }
            ItemStack toRemove = new ItemStack(itemId, amount);
            if (!combined.canRemoveItemStack(toRemove)) {
                return false;
            }
            ItemStackTransaction transaction = combined.removeItemStack(toRemove);
            return transaction != null && transaction.succeeded();
        }
        catch (Exception e) {
            LOGGER.warning("Failed to remove item " + itemId + ": " + e.getMessage());
            return false;
        }
    }

    public static String formatItemName(String itemId) {
        String[] prefixes;
        if (itemId == null || itemId.isBlank()) {
            return "Unknown";
        }
        String name = itemId.contains(":") ? itemId.substring(itemId.indexOf(58) + 1) : itemId;
        for (String prefix : prefixes = new String[]{"Weapon_", "Tool_", "Armor_", "Food_", "Potion_", "Block_", "Ingredient_", "Resource_", "Consumable_", "Material_", "Item_", "Ore_"}) {
            if (!name.startsWith(prefix)) continue;
            name = name.substring(prefix.length());
            break;
        }
        name = name.replace('_', ' ');
        return name.trim();
    }
}

