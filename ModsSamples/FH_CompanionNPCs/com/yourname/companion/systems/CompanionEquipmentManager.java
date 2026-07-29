/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.logger.HytaleLogger
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.inventory.Inventory
 *  com.hypixel.hytale.server.core.inventory.ItemStack
 *  com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer
 *  com.hypixel.hytale.server.core.inventory.container.ItemContainer
 *  com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  com.hypixel.hytale.server.npc.entities.NPCEntity
 */
package com.yourname.companion.systems;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.yourname.companion.data.CompanionRecord;
import com.yourname.companion.data.EquipmentSlot;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class CompanionEquipmentManager {
    private final HytaleLogger logger;

    public CompanionEquipmentManager(HytaleLogger logger) {
        this.logger = logger;
    }

    public String equipItem(CompanionRecord companion, EquipmentSlot slot, String assetId, PlayerRef playerRef, NPCEntity npcEntity, Store<EntityStore> store) {
        if (companion == null || slot == null || assetId == null) {
            return "Invalid parameters.";
        }
        EquipmentSlot expectedSlot = EquipmentSlot.forItem(assetId);
        if (expectedSlot == null) {
            return "Item is not equippable.";
        }
        if (expectedSlot != slot) {
            return assetId + " doesn't go in " + slot.getDisplayName() + " slot.";
        }
        if (slot == EquipmentSlot.OFFHAND && companion.equippedWeapon != null && EquipmentSlot.isTwoHanded(companion.equippedWeapon)) {
            return "Cannot use offhand with two-handed weapon.";
        }
        try {
            String oldItem;
            Player player = (Player)store.getComponent(playerRef.getReference(), Player.getComponentType());
            if (player == null) {
                return "Player not found.";
            }
            Inventory playerInv = player.getInventory();
            if (playerInv == null) {
                return "Player inventory not found.";
            }
            if (!this.removeItemFromPlayer(playerInv, assetId)) {
                return "Item not found in your inventory.";
            }
            if (slot == EquipmentSlot.WEAPON && EquipmentSlot.isTwoHanded(assetId) && companion.equippedOffhand != null) {
                this.returnItemToPlayer(playerInv, companion.equippedOffhand);
                companion.setEquipped(EquipmentSlot.OFFHAND, null);
                if (npcEntity != null) {
                    this.clearNpcSlot(npcEntity, EquipmentSlot.OFFHAND);
                }
            }
            if ((oldItem = companion.getEquipped(slot)) != null) {
                this.returnItemToPlayer(playerInv, oldItem);
            }
            companion.setEquipped(slot, assetId);
            if (npcEntity != null) {
                this.applyEquipmentToNpc(companion, npcEntity);
            }
            return null;
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to equip " + assetId);
            return "Equip failed: " + t.getMessage();
        }
    }

    public String unequipItem(CompanionRecord companion, EquipmentSlot slot, PlayerRef playerRef, NPCEntity npcEntity, Store<EntityStore> store) {
        if (companion == null || slot == null) {
            return "Invalid parameters.";
        }
        String equipped = companion.getEquipped(slot);
        if (equipped == null) {
            return slot.getDisplayName() + " slot is empty.";
        }
        try {
            Player player = (Player)store.getComponent(playerRef.getReference(), Player.getComponentType());
            if (player == null) {
                return "Player not found.";
            }
            Inventory playerInv = player.getInventory();
            if (playerInv == null) {
                return "Player inventory not found.";
            }
            ItemStack unequippedStack = null;
            if (npcEntity != null) {
                unequippedStack = this.extractNpcEquippedStack(npcEntity, slot);
            }
            if (unequippedStack == null || unequippedStack.isEmpty()) {
                unequippedStack = new ItemStack(equipped, 1);
            }
            if (!this.returnItemStackToPlayer(playerInv, unequippedStack)) {
                return "Could not move item to your inventory.";
            }
            companion.setEquipped(slot, null);
            if (npcEntity != null) {
                this.applyEquipmentToNpc(companion, npcEntity);
            }
            return null;
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to unequip from " + String.valueOf((Object)slot));
            return "Unequip failed: " + t.getMessage();
        }
    }

    public List<String[]> collectEquippableItems(PlayerRef playerRef, Store<EntityStore> store) {
        ArrayList<String[]> result = new ArrayList<String[]>();
        try {
            Player player = (Player)store.getComponent(playerRef.getReference(), Player.getComponentType());
            if (player == null) {
                return result;
            }
            Inventory playerInv = player.getInventory();
            if (playerInv == null) {
                return result;
            }
            this.scanPlayerContainers(playerInv, result, true);
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to scan player inventory.");
        }
        return result;
    }

    public List<String[]> collectInventoryItems(PlayerRef playerRef, Store<EntityStore> store) {
        ArrayList<String[]> result = new ArrayList<String[]>();
        try {
            Player player = (Player)store.getComponent(playerRef.getReference(), Player.getComponentType());
            if (player == null) {
                return result;
            }
            Inventory playerInv = player.getInventory();
            if (playerInv == null) {
                return result;
            }
            this.scanPlayerContainers(playerInv, result, false);
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to scan full player inventory.");
        }
        return result;
    }

    private void scanContainer(ItemContainer container, List<String[]> result) {
        if (container == null) {
            return;
        }
        container.forEach((slot, itemStack) -> {
            if (itemStack == null || itemStack.isEmpty()) {
                return;
            }
            String itemId = itemStack.getItemId();
            if (itemId == null) {
                return;
            }
            int qty = Math.max(1, itemStack.getQuantity());
            EquipmentSlot eqSlot = EquipmentSlot.forItem(itemId);
            if (eqSlot != null || EquipmentSlot.isAmmo(itemId)) {
                result.add(new String[]{itemId, EquipmentSlot.getItemDisplayName(itemId), Integer.toString(qty)});
            }
        });
    }

    private void scanContainerAll(ItemContainer container, List<String[]> result) {
        if (container == null || result == null) {
            return;
        }
        container.forEach((slot, itemStack) -> {
            if (itemStack == null || itemStack.isEmpty()) {
                return;
            }
            String itemId = itemStack.getItemId();
            if (itemId == null || itemId.isBlank()) {
                return;
            }
            int qty = Math.max(1, itemStack.getQuantity());
            result.add(new String[]{itemId, EquipmentSlot.getItemDisplayName(itemId), Integer.toString(qty)});
        });
    }

    private void scanPlayerContainers(Inventory playerInv, List<String[]> result, boolean equippableOnly) {
        if (playerInv == null || result == null) {
            return;
        }
        if (equippableOnly) {
            this.scanContainer(playerInv.getHotbar(), result);
        } else {
            this.scanContainerAll(playerInv.getHotbar(), result);
        }
        try {
            if (equippableOnly) {
                this.scanContainer(playerInv.getStorage(), result);
            } else {
                this.scanContainerAll(playerInv.getStorage(), result);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            ItemContainer backpack = playerInv.getBackpack();
            if (equippableOnly) {
                this.scanContainer(backpack, result);
            } else {
                this.scanContainerAll(backpack, result);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public void applyEquipmentToNpc(CompanionRecord companion, NPCEntity npcEntity) {
        if (companion == null || npcEntity == null) {
            return;
        }
        String equippedWeapon = companion.getEquipped(EquipmentSlot.WEAPON);
        if (equippedWeapon != null && EquipmentSlot.isTwoHanded(equippedWeapon) && companion.getEquipped(EquipmentSlot.OFFHAND) != null) {
            companion.setEquipped(EquipmentSlot.OFFHAND, null);
        }
        this.clearAllArmorSlots(npcEntity);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            String equipped = companion.getEquipped(slot);
            if (equipped == null) continue;
            this.updateNpcVisual(npcEntity, slot, equipped);
        }
    }

    private boolean removeItemFromPlayer(Inventory playerInv, String assetId) {
        if (this.removeFromContainer(playerInv.getHotbar(), assetId)) {
            return true;
        }
        try {
            return this.removeFromContainer(playerInv.getStorage(), assetId);
        }
        catch (Throwable t) {
            return false;
        }
    }

    private boolean removeFromContainer(ItemContainer container, String assetId) {
        if (container == null) {
            return false;
        }
        boolean[] removed = new boolean[]{false};
        short[] targetSlot = new short[]{-1};
        container.forEach((slot, itemStack) -> {
            if (removed[0]) {
                return;
            }
            if (itemStack != null && !itemStack.isEmpty() && assetId.equals(itemStack.getItemId())) {
                targetSlot[0] = slot;
                removed[0] = true;
            }
        });
        if (targetSlot[0] >= 0) {
            ItemStack current = container.getItemStack(targetSlot[0]);
            if (current != null && current.getQuantity() > 1) {
                container.setItemStackForSlot(targetSlot[0], current.withQuantity(current.getQuantity() - 1));
            } else {
                container.setItemStackForSlot(targetSlot[0], ItemStack.EMPTY);
            }
            return true;
        }
        return false;
    }

    private void returnItemToPlayer(Inventory playerInv, String assetId) {
        try {
            ItemStack itemStack = new ItemStack(assetId, 1);
            this.returnItemStackToPlayer(playerInv, itemStack);
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to return " + assetId + " to player.");
        }
    }

    private boolean returnItemStackToPlayer(Inventory playerInv, ItemStack itemStack) {
        if (playerInv == null || itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        try {
            ItemStackTransaction tx = playerInv.getCombinedHotbarFirst().addItemStack(itemStack);
            if (tx == null || !tx.succeeded()) {
                return false;
            }
            ItemStack remainder = tx.getRemainder();
            return remainder == null || remainder.isEmpty();
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to return item stack to player.");
            return false;
        }
    }

    private ItemStack extractNpcEquippedStack(NPCEntity npcEntity, EquipmentSlot slot) {
        if (npcEntity == null || slot == null) {
            return ItemStack.EMPTY;
        }
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return ItemStack.EMPTY;
            }
            return switch (slot) {
                default -> throw new MatchException(null, null);
                case EquipmentSlot.WEAPON -> this.extractFromContainer((ItemContainer)inventory.getCombinedHotbarFirst(), (short)0);
                case EquipmentSlot.OFFHAND -> this.extractFromContainer(inventory.getUtility(), (short)0);
                case EquipmentSlot.HELMET -> this.extractFromContainer(inventory.getArmor(), (short)0);
                case EquipmentSlot.CHESTPLATE -> this.extractFromContainer(inventory.getArmor(), (short)1);
                case EquipmentSlot.LEGGINGS -> this.extractFromContainer(inventory.getArmor(), (short)2);
                case EquipmentSlot.BOOTS -> this.extractFromContainer(inventory.getArmor(), (short)3);
            };
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to extract NPC equipped stack from " + String.valueOf((Object)slot));
            return ItemStack.EMPTY;
        }
    }

    private ItemStack extractFromContainer(ItemContainer container, short slotIndex) {
        if (container == null) {
            return ItemStack.EMPTY;
        }
        ItemStack current = container.getItemStack(slotIndex);
        if (current == null || current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        container.setItemStackForSlot(slotIndex, ItemStack.EMPTY);
        return current;
    }

    private void updateNpcVisual(NPCEntity npcEntity, EquipmentSlot slot, String assetId) {
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return;
            }
            switch (slot) {
                case WEAPON: {
                    CombinedItemContainer container = inventory.getCombinedHotbarFirst();
                    if (container != null) {
                        container.setItemStackForSlot((short)0, new ItemStack(assetId, 1));
                    }
                    break;
                }
                case OFFHAND: {
                    ItemContainer container = inventory.getUtility();
                    if (container != null) {
                        container.setItemStackForSlot((short)0, new ItemStack(assetId, 1));
                        try {
                            inventory.setActiveUtilitySlot((byte)0);
                        }
                        catch (Throwable throwable) {}
                    }
                    break;
                }
                case HELMET: 
                case CHESTPLATE: 
                case LEGGINGS: 
                case BOOTS: {
                    Short runtimeIndex = this.resolveArmorIndexFromItem(assetId);
                    short slotIndex = runtimeIndex != null ? runtimeIndex.shortValue() : this.fallbackArmorIndex(slot);
                    this.setArmorSlot(inventory, slotIndex, assetId);
                }
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to update NPC visual for " + String.valueOf((Object)slot));
        }
    }

    private void clearNpcSlot(NPCEntity npcEntity, EquipmentSlot slot) {
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return;
            }
            switch (slot) {
                case WEAPON: {
                    CombinedItemContainer container = inventory.getCombinedHotbarFirst();
                    if (container != null) {
                        container.setItemStackForSlot((short)0, ItemStack.EMPTY);
                    }
                    break;
                }
                case OFFHAND: {
                    ItemContainer container = inventory.getUtility();
                    if (container != null) {
                        container.setItemStackForSlot((short)0, ItemStack.EMPTY);
                        try {
                            inventory.setActiveUtilitySlot((byte)0);
                        }
                        catch (Throwable throwable) {}
                    }
                    break;
                }
                case HELMET: 
                case CHESTPLATE: 
                case LEGGINGS: 
                case BOOTS: {
                    Short runtimeIndex = this.resolveArmorIndexFromItem(this.companionSlotDefaultItem(slot));
                    short slotIndex = runtimeIndex != null ? runtimeIndex.shortValue() : this.fallbackArmorIndex(slot);
                    this.setArmorSlot(inventory, slotIndex, null);
                }
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to clear NPC slot " + String.valueOf((Object)slot));
        }
    }

    private void clearAllArmorSlots(NPCEntity npcEntity) {
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return;
            }
            ItemContainer armor = inventory.getArmor();
            if (armor == null) {
                return;
            }
            for (short i = 0; i < 4; i = (short)(i + 1)) {
                this.clearArmorSlotWithFallbacks(armor, i);
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to clear all NPC armor slots.");
        }
    }

    private void clearArmorSlotWithFallbacks(ItemContainer armorContainer, short slotIndex) {
        ItemStack after2;
        try {
            armorContainer.setItemStackForSlot(slotIndex, ItemStack.EMPTY);
            after2 = armorContainer.getItemStack(slotIndex);
            if (after2 == null || after2.isEmpty() || after2.getQuantity() <= 0) {
                return;
            }
        }
        catch (Throwable after2) {
            // empty catch block
        }
        try {
            armorContainer.setItemStackForSlot(slotIndex, null);
            after2 = armorContainer.getItemStack(slotIndex);
            if (after2 == null || after2.isEmpty() || after2.getQuantity() <= 0) {
                return;
            }
        }
        catch (Throwable after3) {
            // empty catch block
        }
        try {
            ItemStack current = armorContainer.getItemStack(slotIndex);
            if (current != null && !current.isEmpty()) {
                armorContainer.setItemStackForSlot(slotIndex, current.withQuantity(0));
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void preserveFallbackSlotZeroItem(ItemContainer container) {
        if (container == null) {
            return;
        }
        try {
            ItemStack current = container.getItemStack((short)0);
            if (!this.isFallbackNonWeapon(current)) {
                return;
            }
            short[] emptySlot = new short[]{-1};
            short[] matchingSlot = new short[]{-1};
            container.forEach((slot, itemStack) -> {
                if (slot == 0) {
                    return;
                }
                if (emptySlot[0] >= 0 && matchingSlot[0] >= 0) {
                    return;
                }
                if (itemStack == null || itemStack.isEmpty()) {
                    if (emptySlot[0] < 0) {
                        emptySlot[0] = slot;
                    }
                    return;
                }
                if (matchingSlot[0] < 0 && current.getItemId() != null && current.getItemId().equals(itemStack.getItemId())) {
                    matchingSlot[0] = slot;
                }
            });
            if (matchingSlot[0] >= 0) {
                ItemStack target = container.getItemStack(matchingSlot[0]);
                if (target != null && !target.isEmpty()) {
                    container.setItemStackForSlot(matchingSlot[0], target.withQuantity(target.getQuantity() + Math.max(1, current.getQuantity())));
                    container.setItemStackForSlot((short)0, ItemStack.EMPTY);
                }
                return;
            }
            if (emptySlot[0] >= 0) {
                container.setItemStackForSlot(emptySlot[0], current);
                container.setItemStackForSlot((short)0, ItemStack.EMPTY);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private boolean isFallbackNonWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            return stack.getItem() == null || stack.getItem().getWeapon() == null;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private Short resolveArmorIndexFromItem(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return null;
        }
        try {
            ItemStack probe = new ItemStack(assetId, 1);
            if (probe.getItem() == null || probe.getItem().getArmor() == null || probe.getItem().getArmor().getArmorSlot() == null) {
                return null;
            }
            return (short)probe.getItem().getArmor().getArmorSlot().getValue();
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private short fallbackArmorIndex(EquipmentSlot slot) {
        return switch (slot) {
            case EquipmentSlot.HELMET -> 0;
            case EquipmentSlot.CHESTPLATE -> 1;
            case EquipmentSlot.BOOTS -> 2;
            case EquipmentSlot.LEGGINGS -> 3;
            default -> 0;
        };
    }

    private String companionSlotDefaultItem(EquipmentSlot slot) {
        return switch (slot) {
            case EquipmentSlot.HELMET -> "Armor_Copper_Head";
            case EquipmentSlot.CHESTPLATE -> "Armor_Copper_Chest";
            case EquipmentSlot.LEGGINGS -> "Armor_Copper_Legs";
            case EquipmentSlot.BOOTS -> "Armor_Copper_Hands";
            default -> null;
        };
    }

    private void setArmorSlot(Inventory inventory, short slotIndex, String assetId) {
        ItemContainer armorContainer = inventory.getArmor();
        if (armorContainer == null) {
            return;
        }
        if (assetId != null) {
            armorContainer.setItemStackForSlot(slotIndex, new ItemStack(assetId, 1));
        } else {
            this.clearArmorSlotWithFallbacks(armorContainer, slotIndex);
        }
    }
}

