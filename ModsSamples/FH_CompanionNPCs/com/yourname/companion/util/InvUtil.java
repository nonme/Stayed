/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.logger.HytaleLogger
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 *  com.hypixel.hytale.protocol.ItemArmorSlot
 *  com.hypixel.hytale.server.core.asset.type.item.config.ItemArmor
 *  com.hypixel.hytale.server.core.asset.type.item.config.ItemWeapon
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
package com.yourname.companion.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.ItemArmorSlot;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemArmor;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemWeapon;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.logging.Level;

public final class InvUtil {
    private static final HytaleLogger DEBUG_LOGGER = HytaleLogger.get((String)"CompanionNPC");
    private static final boolean DEBUG_INVENTORY_LOG = false;
    private static final String[] WEAPON_TIER_ORDER = new String[]{"Weapon_Sword_Crude", "Weapon_Sword_Wood", "Weapon_Mace_Crude", "Weapon_Sword_Iron", "Weapon_Mace_Iron", "Weapon_Sword_Steel", "Weapon_Mace_Steel", "Weapon_Sword_Diamond", "Weapon_Mace_Diamond", "Weapon_Crossbow", "Weapon_Sword_Cobalt", "Weapon_Mace_Cobalt", "Weapon_Sword_Thorium", "Weapon_Sword_Adamantite", "Weapon_Sword_Mithril", "Weapon_Longsword_Scarab", "Weapon_Crossbow_Ancient_Steel", "Weapon_Gun", "Weapon_Assault_Rifle"};
    private static final String[] PICKAXE_TIER_ORDER = new String[]{"Tool_Pickaxe_Crude", "Tool_Pickaxe_Wood", "Tool_Pickaxe_Iron", "Tool_Pickaxe_Steel", "Tool_Pickaxe_Diamond"};
    private static final String[] HOE_TIER_ORDER = new String[]{"Tool_Hoe_Crude", "Tool_Hoe_Wood", "Tool_Hoe_Iron", "Tool_Hoe_Steel", "Tool_Hoe_Diamond"};

    private InvUtil() {
    }

    private static ItemContainer invokeContainerGetter(Object inventory, String methodName) {
        if (inventory == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        try {
            Method method = inventory.getClass().getMethod(methodName, new Class[0]);
            Object value = method.invoke(inventory, new Object[0]);
            if (value instanceof ItemContainer) {
                ItemContainer container = (ItemContainer)value;
                return container;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    public static List<ItemContainer> getCompanionContainersForRead(NPCEntity npcEntity) {
        return InvUtil.getCompanionContainers(npcEntity);
    }

    public static List<ItemContainer> getCompanionContainersForWrite(NPCEntity npcEntity) {
        return InvUtil.getCompanionContainers(npcEntity);
    }

    private static List<ItemContainer> getCompanionContainers(NPCEntity npcEntity) {
        ArrayList<ItemContainer> containers = new ArrayList<ItemContainer>();
        if (npcEntity == null) {
            return containers;
        }
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return containers;
            }
            ItemContainer hotbar = InvUtil.invokeContainerGetter(inventory, "getHotbar");
            ItemContainer storage = InvUtil.invokeContainerGetter(inventory, "getStorage");
            ItemContainer backpack = InvUtil.invokeContainerGetter(inventory, "getBackpack");
            CombinedItemContainer combined = inventory.getCombinedHotbarFirst();
            List<ItemContainer> separate = new ArrayList<ItemContainer>();
            if (hotbar != null) {
                separate.add(hotbar);
            }
            if (storage != null) {
                separate.add(storage);
            }
            if (backpack != null) {
                separate.add(backpack);
            }
            if (!(separate = InvUtil.uniqueByIdentity(separate)).isEmpty()) {
                return separate;
            }
            if (combined != null) {
                containers.add((ItemContainer)combined);
                return containers;
            }
            containers.addAll(separate);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return InvUtil.uniqueByIdentity(containers);
    }

    private static List<ItemContainer> uniqueByIdentity(List<ItemContainer> containers) {
        ArrayList<ItemContainer> unique = new ArrayList<ItemContainer>();
        IdentityHashMap<ItemContainer, Boolean> seen = new IdentityHashMap<ItemContainer, Boolean>();
        for (ItemContainer container : containers) {
            if (container == null || seen.containsKey(container)) continue;
            seen.put(container, Boolean.TRUE);
            unique.add(container);
        }
        return unique;
    }

    private static int getContainerSlotCount(ItemContainer container) {
        if (container == null) {
            return 0;
        }
        try {
            for (String methodName : new String[]{"getSlotCount", "getSize", "size", "getCapacity"}) {
                try {
                    Number n;
                    int v;
                    Method m = container.getClass().getMethod(methodName, new Class[0]);
                    Object value = m.invoke((Object)container, new Object[0]);
                    if (!(value instanceof Number) || (v = (n = (Number)value).intValue()) <= 0) continue;
                    return v;
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return 0;
    }

    private static int getOccupiedSlotCount(ItemContainer container) {
        if (container == null) {
            return 0;
        }
        int[] occupied = new int[]{0};
        try {
            container.forEach((slot, itemStack) -> {
                if (itemStack != null && !itemStack.isEmpty()) {
                    occupied[0] = occupied[0] + 1;
                }
            });
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return occupied[0];
    }

    public static boolean giveItemToCompanion(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, String itemId, int count, HytaleLogger logger) {
        if (npcEntity == null || itemId == null || itemId.isBlank() || count <= 0) {
            return false;
        }
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                logger.at(Level.WARNING).log("Companion has no inventory \u2014 cannot give " + itemId);
                return false;
            }
            ItemStack itemStack = new ItemStack(itemId, count);
            ItemStack remainder = InvUtil.addItemStackToCompanionInventory(npcEntity, itemStack);
            int remaining = remainder == null || remainder.isEmpty() ? 0 : remainder.getQuantity();
            return remaining < count;
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)logger.at(Level.WARNING).withCause(t)).log("Failed to give item " + itemId + " to companion.");
            return false;
        }
    }

    public static ItemStack addItemStackToCompanionInventory(NPCEntity npcEntity, ItemStack itemStack) {
        if (npcEntity == null || itemStack == null || itemStack.isEmpty()) {
            return itemStack;
        }
        try {
            ItemStack remaining = itemStack;
            List<ItemContainer> containers = InvUtil.getCompanionContainersForWrite(npcEntity);
            InvUtil.logContainerSnapshot(npcEntity, "write-start item=" + itemStack.getItemId() + " qty=" + itemStack.getQuantity(), containers);
            for (ItemContainer container : containers) {
                if (remaining == null || remaining.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                remaining = InvUtil.insertIntoContainer(container, remaining);
            }
            int left = remaining == null || remaining.isEmpty() ? 0 : remaining.getQuantity();
            return remaining == null ? ItemStack.EMPTY : remaining;
        }
        catch (Throwable ignored) {
            return itemStack;
        }
    }

    public static boolean transferHandToCompanion(PlayerRef playerRef, NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, HytaleLogger logger) {
        try {
            ItemStack remainder;
            int remaining;
            Player playerEntity = (Player)store.getComponent(playerRef.getReference(), Player.getComponentType());
            if (playerEntity == null) {
                return false;
            }
            Inventory playerInv = playerEntity.getInventory();
            if (playerInv == null) {
                return false;
            }
            ItemStack handItem = playerInv.getItemInHand();
            if (handItem == null || handItem.isEmpty()) {
                return false;
            }
            Inventory companionInv = npcEntity.getInventory();
            if (companionInv == null) {
                return false;
            }
            String itemId = handItem.getItemId();
            int qty = handItem.getQuantity();
            int added = qty - (remaining = (remainder = InvUtil.addItemStackToCompanionInventory(npcEntity, handItem)) == null || remainder.isEmpty() ? 0 : remainder.getQuantity());
            if (added > 0) {
                short activeSlot = playerInv.getActiveHotbarSlot();
                if (added >= qty) {
                    playerInv.getHotbar().setItemStackForSlot(activeSlot, ItemStack.EMPTY);
                } else {
                    playerInv.getHotbar().setItemStackForSlot(activeSlot, handItem.withQuantity(qty - added));
                }
                return true;
            }
            InvUtil.logContainerSnapshot(npcEntity, "hand-transfer-failed item=" + handItem.getItemId() + " qty=" + qty, InvUtil.getCompanionContainersForWrite(npcEntity));
            return false;
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)logger.at(Level.WARNING).withCause(t)).log("Failed to transfer hand item to companion.");
            return false;
        }
    }

    public static boolean transferFromCompanionToPlayer(PlayerRef playerRef, NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, HytaleLogger logger) {
        try {
            Player playerEntity = (Player)store.getComponent(playerRef.getReference(), Player.getComponentType());
            if (playerEntity == null) {
                return false;
            }
            Inventory playerInv = playerEntity.getInventory();
            if (playerInv == null) {
                return false;
            }
            Inventory companionInv = npcEntity.getInventory();
            if (companionInv == null) {
                return false;
            }
            ContainerSlotRef found = InvUtil.findFirstOccupiedCompanionSlot(npcEntity);
            if (found == null || found.stack == null || found.slot < 0) {
                return false;
            }
            ItemStackTransaction tx = playerInv.getCombinedHotbarFirst().addItemStack(found.stack);
            if (tx == null || !tx.succeeded()) {
                return false;
            }
            ItemStack remainder = tx.getRemainder();
            int remaining = remainder == null || remainder.isEmpty() ? 0 : remainder.getQuantity();
            int taken = found.stack.getQuantity() - remaining;
            if (taken > 0) {
                if (taken >= found.stack.getQuantity()) {
                    found.container.setItemStackForSlot(found.slot, ItemStack.EMPTY);
                } else {
                    found.container.setItemStackForSlot(found.slot, found.stack.withQuantity(found.stack.getQuantity() - taken));
                }
                return true;
            }
            InvUtil.logContainerSnapshot(npcEntity, "take-transfer-failed item=" + found.stack.getItemId() + " qty=" + found.stack.getQuantity(), InvUtil.getCompanionContainersForRead(npcEntity));
            return false;
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)logger.at(Level.WARNING).withCause(t)).log("Failed to transfer item from companion to player.");
            return false;
        }
    }

    public static String equipBestWeapon(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, HytaleLogger logger) {
        if (npcEntity == null) {
            return null;
        }
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return null;
            }
            CombinedItemContainer container = inventory.getCombinedHotbarFirst();
            if (container == null) {
                return null;
            }
            String[] bestItemId = new String[]{null};
            int[] bestTier = new int[]{-1};
            short[] bestSlot = new short[]{-1};
            container.forEach((slot, itemStack) -> {
                if (itemStack == null || itemStack.isEmpty()) {
                    return;
                }
                String id = itemStack.getItemId();
                if (id == null) {
                    return;
                }
                int tier = InvUtil.getWeaponTier(id);
                if (tier > bestTier[0]) {
                    bestTier[0] = tier;
                    bestItemId[0] = id;
                    bestSlot[0] = slot;
                }
            });
            if (bestItemId[0] == null || bestSlot[0] < 0) {
                return null;
            }
            if (bestSlot[0] == 0) {
                return bestItemId[0];
            }
            ItemContainer hotbar = inventory.getHotbar();
            if (hotbar == null) {
                return bestItemId[0];
            }
            Object slot0Item = null;
            Object bestSlotItem = null;
            short targetSlot = bestSlot[0];
            ItemStack[] items = new ItemStack[2];
            container.forEach((slot, itemStack) -> {
                if (slot == 0) {
                    items[0] = itemStack;
                }
                if (slot == targetSlot) {
                    items[1] = itemStack;
                }
            });
            container.setItemStackForSlot((short)0, items[1] != null ? items[1] : ItemStack.EMPTY);
            container.setItemStackForSlot(targetSlot, items[0] != null ? items[0] : ItemStack.EMPTY);
            logger.at(Level.INFO).log("Equipped best weapon: " + bestItemId[0] + " (tier " + bestTier[0] + ") to hotbar slot 0");
            return bestItemId[0];
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)logger.at(Level.WARNING).withCause(t)).log("Failed to equip best weapon.");
            return null;
        }
    }

    public static int getWeaponTier(String itemId) {
        if (itemId == null) {
            return -1;
        }
        for (int i = 0; i < WEAPON_TIER_ORDER.length; ++i) {
            if (!itemId.equals(WEAPON_TIER_ORDER[i])) continue;
            return i;
        }
        if (itemId.startsWith("Weapon_Sword") || itemId.startsWith("Weapon_Mace")) {
            return 0;
        }
        return -1;
    }

    public static int getPickaxeTier(String itemId) {
        if (itemId == null) {
            return -1;
        }
        for (int i = 0; i < PICKAXE_TIER_ORDER.length; ++i) {
            if (!itemId.equals(PICKAXE_TIER_ORDER[i])) continue;
            return i;
        }
        if (itemId.startsWith("Tool_Pickaxe")) {
            return 0;
        }
        return -1;
    }

    public static int getHoeTier(String itemId) {
        if (itemId == null) {
            return -1;
        }
        for (int i = 0; i < HOE_TIER_ORDER.length; ++i) {
            if (!itemId.equals(HOE_TIER_ORDER[i])) continue;
            return i;
        }
        if (itemId.startsWith("Tool_Hoe")) {
            return 0;
        }
        return -1;
    }

    public static String equipBestPickaxe(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, HytaleLogger logger) {
        return InvUtil.equipBestToolByTier(npcEntity, companionRef, store, logger, "pickaxe", InvUtil::getPickaxeTier);
    }

    public static String equipBestHoe(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, HytaleLogger logger) {
        return InvUtil.equipBestToolByTier(npcEntity, companionRef, store, logger, "hoe", InvUtil::getHoeTier);
    }

    private static String equipBestToolByTier(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, HytaleLogger logger, String toolName, ToIntFunction<String> tierFn) {
        if (npcEntity == null) {
            return null;
        }
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return null;
            }
            CombinedItemContainer container = inventory.getCombinedHotbarFirst();
            if (container == null) {
                return null;
            }
            String[] bestItemId = new String[]{null};
            int[] bestTier = new int[]{-1};
            short[] bestSlot = new short[]{-1};
            container.forEach((slot, itemStack) -> {
                if (itemStack == null || itemStack.isEmpty()) {
                    return;
                }
                String id = itemStack.getItemId();
                if (id == null) {
                    return;
                }
                int tier = tierFn.applyAsInt(id);
                if (tier > bestTier[0]) {
                    bestTier[0] = tier;
                    bestItemId[0] = id;
                    bestSlot[0] = slot;
                }
            });
            if (bestItemId[0] == null || bestSlot[0] < 0) {
                return null;
            }
            if (bestSlot[0] == 0) {
                return bestItemId[0];
            }
            short targetSlot = bestSlot[0];
            ItemStack[] items = new ItemStack[2];
            container.forEach((slot, itemStack) -> {
                if (slot == 0) {
                    items[0] = itemStack;
                }
                if (slot == targetSlot) {
                    items[1] = itemStack;
                }
            });
            container.setItemStackForSlot((short)0, items[1] != null ? items[1] : ItemStack.EMPTY);
            container.setItemStackForSlot(targetSlot, items[0] != null ? items[0] : ItemStack.EMPTY);
            logger.at(Level.INFO).log("Equipped best " + toolName + ": " + bestItemId[0] + " (tier " + bestTier[0] + ") to hotbar slot 0");
            return bestItemId[0];
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)logger.at(Level.WARNING).withCause(t)).log("Failed to equip best " + toolName + ".");
            return null;
        }
    }

    public static ItemStack equipArmorOnCompanion(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, ItemStack itemStack, HytaleLogger logger) {
        if (npcEntity == null || itemStack == null || itemStack.isEmpty()) {
            return null;
        }
        try {
            ItemArmor armorInfo = itemStack.getItem().getArmor();
            if (armorInfo == null) {
                return null;
            }
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return null;
            }
            ItemContainer armorContainer = inventory.getArmor();
            if (armorContainer == null) {
                return null;
            }
            short slotIndex = (short)armorInfo.getArmorSlot().getValue();
            ItemStack current = armorContainer.getItemStack(slotIndex);
            armorContainer.setItemStackForSlot(slotIndex, itemStack);
            logger.at(Level.INFO).log("Equipped armor " + itemStack.getItemId() + " in slot " + armorInfo.getArmorSlot().name());
            return current != null && !current.isEmpty() ? current : null;
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)logger.at(Level.WARNING).withCause(t)).log("Failed to equip armor on companion.");
            return null;
        }
    }

    public static ItemStack equipWeaponOnCompanion(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, ItemStack itemStack, HytaleLogger logger) {
        if (npcEntity == null || itemStack == null || itemStack.isEmpty()) {
            return null;
        }
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return null;
            }
            CombinedItemContainer container = inventory.getCombinedHotbarFirst();
            if (container == null) {
                return null;
            }
            ItemStack[] slot0 = new ItemStack[]{null};
            container.forEach((slot, is) -> {
                if (slot == 0) {
                    slot0[0] = is;
                }
            });
            container.setItemStackForSlot((short)0, itemStack);
            logger.at(Level.INFO).log("Equipped weapon " + itemStack.getItemId() + " in hotbar slot 0");
            return slot0[0] != null && !slot0[0].isEmpty() ? slot0[0] : null;
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)logger.at(Level.WARNING).withCause(t)).log("Failed to equip weapon on companion.");
            return null;
        }
    }

    public static ItemStack getEquippedArmor(NPCEntity npcEntity, ItemArmorSlot slot) {
        if (npcEntity == null || slot == null) {
            return null;
        }
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return null;
            }
            ItemContainer armorContainer = inventory.getArmor();
            if (armorContainer == null) {
                return null;
            }
            ItemStack item = armorContainer.getItemStack((short)slot.getValue());
            return item != null && !item.isEmpty() ? item : null;
        }
        catch (Throwable t) {
            return null;
        }
    }

    public static boolean isArmorBetter(ItemStack newArmor, ItemStack currentArmor) {
        if (newArmor == null || newArmor.isEmpty()) {
            return false;
        }
        if (currentArmor == null || currentArmor.isEmpty()) {
            return true;
        }
        try {
            ItemArmor newInfo = newArmor.getItem().getArmor();
            ItemArmor curInfo = currentArmor.getItem().getArmor();
            if (newInfo == null) {
                return false;
            }
            if (curInfo == null) {
                return true;
            }
            return newInfo.getBaseDamageResistance() > curInfo.getBaseDamageResistance();
        }
        catch (Throwable t) {
            return false;
        }
    }

    public static boolean isWeaponBetter(ItemStack newWeapon, ItemStack currentWeapon) {
        if (newWeapon == null || newWeapon.isEmpty()) {
            return false;
        }
        if (currentWeapon == null || currentWeapon.isEmpty()) {
            return true;
        }
        try {
            int newTier = InvUtil.getWeaponTier(newWeapon.getItemId());
            int curTier = InvUtil.getWeaponTier(currentWeapon.getItemId());
            if (newTier < 0) {
                return false;
            }
            if (curTier < 0) {
                return true;
            }
            return newTier > curTier;
        }
        catch (Throwable t) {
            return false;
        }
    }

    public static boolean equipIfBetter(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, ItemStack itemStack, HytaleLogger logger) {
        if (npcEntity == null || itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        try {
            ItemArmor armorInfo = itemStack.getItem().getArmor();
            if (armorInfo != null) {
                ItemStack current = InvUtil.getEquippedArmor(npcEntity, armorInfo.getArmorSlot());
                if (InvUtil.isArmorBetter(itemStack, current)) {
                    Inventory inventory;
                    ItemStack displaced = InvUtil.equipArmorOnCompanion(npcEntity, companionRef, store, itemStack, logger);
                    if (displaced != null && (inventory = npcEntity.getInventory()) != null) {
                        inventory.getCombinedHotbarFirst().addItemStack(displaced);
                    }
                    return true;
                }
                return false;
            }
            ItemWeapon weaponInfo = itemStack.getItem().getWeapon();
            if (weaponInfo != null) {
                Inventory inventory = npcEntity.getInventory();
                if (inventory == null) {
                    return false;
                }
                CombinedItemContainer container = inventory.getCombinedHotbarFirst();
                if (container == null) {
                    return false;
                }
                ItemStack[] slot0 = new ItemStack[]{null};
                container.forEach((slot, is) -> {
                    if (slot == 0) {
                        slot0[0] = is;
                    }
                });
                if (InvUtil.isWeaponBetter(itemStack, slot0[0])) {
                    ItemStack displaced = InvUtil.equipWeaponOnCompanion(npcEntity, companionRef, store, itemStack, logger);
                    if (displaced != null) {
                        container.addItemStack(displaced);
                    }
                    return true;
                }
                return false;
            }
            return false;
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)logger.at(Level.WARNING).withCause(t)).log("Failed in equipIfBetter.");
            return false;
        }
    }

    public static void equipBestArmor(NPCEntity npcEntity, Ref<EntityStore> companionRef, Store<EntityStore> store, HytaleLogger logger) {
        if (npcEntity == null) {
            return;
        }
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return;
            }
            for (ItemArmorSlot slot : ItemArmorSlot.values()) {
                ItemStack currentEquipped = InvUtil.getEquippedArmor(npcEntity, slot);
                ItemStack[] bestFound = new ItemStack[]{null};
                short[] bestSlot = new short[]{-1};
                ItemContainer[] bestContainer = new ItemContainer[]{null};
                double[] bestResist = new double[]{-1.0};
                if (currentEquipped != null) {
                    try {
                        ItemArmor curInfo = currentEquipped.getItem().getArmor();
                        if (curInfo != null) {
                            bestResist[0] = curInfo.getBaseDamageResistance();
                        }
                    }
                    catch (Throwable curInfo) {
                        // empty catch block
                    }
                }
                List<ItemContainer> containers = InvUtil.getCompanionContainersForRead(npcEntity);
                for (ItemContainer container : containers) {
                    if (container == null) continue;
                    container.forEach((invSlot, itemStack) -> {
                        if (itemStack == null || itemStack.isEmpty()) {
                            return;
                        }
                        try {
                            ItemArmor armorInfo = itemStack.getItem().getArmor();
                            if (armorInfo == null) {
                                return;
                            }
                            if (armorInfo.getArmorSlot() != slot) {
                                return;
                            }
                            double resist = armorInfo.getBaseDamageResistance();
                            if (resist > bestResist[0]) {
                                bestResist[0] = resist;
                                bestFound[0] = itemStack;
                                bestSlot[0] = invSlot;
                                bestContainer[0] = container;
                            }
                        }
                        catch (Throwable throwable) {
                            // empty catch block
                        }
                    });
                }
                if (bestFound[0] == null || bestSlot[0] < 0 || bestContainer[0] == null) continue;
                bestContainer[0].setItemStackForSlot(bestSlot[0], ItemStack.EMPTY);
                ItemStack displaced = InvUtil.equipArmorOnCompanion(npcEntity, companionRef, store, bestFound[0], logger);
                if (displaced == null) continue;
                bestContainer[0].addItemStack(displaced);
            }
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)logger.at(Level.WARNING).withCause(t)).log("Failed to equip best armor.");
        }
    }

    public static boolean hasInventorySpace(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return false;
        }
        try {
            List<ItemContainer> containers = InvUtil.getCompanionContainersForRead(npcEntity);
            for (ItemContainer container : containers) {
                if (!InvUtil.hasContainerSpace(container)) continue;
                return true;
            }
            InvUtil.logContainerSnapshot(npcEntity, "space-check-full", containers);
            return false;
        }
        catch (Throwable t) {
            return true;
        }
    }

    public static ContainerSlotRef findMatchingCompanionSlot(NPCEntity npcEntity, String itemId) {
        if (npcEntity == null || itemId == null || itemId.isBlank()) {
            return null;
        }
        try {
            for (ItemContainer container : InvUtil.getCompanionContainersForRead(npcEntity)) {
                ContainerSlotRef found = InvUtil.findMatchingSlot(container, itemId);
                if (found == null) continue;
                return found;
            }
            return null;
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    public static ContainerSlotRef findFirstOccupiedCompanionSlot(NPCEntity npcEntity) {
        if (npcEntity == null) {
            return null;
        }
        try {
            for (ItemContainer container : InvUtil.getCompanionContainersForRead(npcEntity)) {
                ContainerSlotRef found = InvUtil.findFirstOccupiedSlot(container);
                if (found == null) continue;
                return found;
            }
            return null;
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private static ItemStack insertIntoContainer(ItemContainer container, ItemStack incoming) {
        if (container == null || incoming == null || incoming.isEmpty()) {
            return incoming;
        }
        try {
            ItemStackTransaction tx = container.addItemStack(incoming);
            if (tx == null || !tx.succeeded()) {
                return incoming;
            }
            ItemStack remainder = tx.getRemainder();
            return remainder == null ? ItemStack.EMPTY : remainder;
        }
        catch (Throwable ignored) {
            return incoming;
        }
    }

    private static boolean hasContainerSpace(ItemContainer container) {
        if (container == null) {
            return false;
        }
        boolean[] hasSpace = new boolean[]{false};
        container.forEach((slot, itemStack) -> {
            if (itemStack == null || itemStack.isEmpty()) {
                hasSpace[0] = true;
            }
        });
        return hasSpace[0];
    }

    private static ContainerSlotRef findMatchingSlot(ItemContainer container, String itemId) {
        if (container == null) {
            return null;
        }
        ContainerSlotRef[] found = new ContainerSlotRef[]{null};
        container.forEach((slot, itemStack) -> {
            if (found[0] != null) {
                return;
            }
            if (itemStack != null && !itemStack.isEmpty() && itemId.equalsIgnoreCase(itemStack.getItemId())) {
                found[0] = new ContainerSlotRef(container, slot, (ItemStack)itemStack);
            }
        });
        return found[0];
    }

    private static ContainerSlotRef findFirstOccupiedSlot(ItemContainer container) {
        if (container == null) {
            return null;
        }
        ContainerSlotRef[] found = new ContainerSlotRef[]{null};
        container.forEach((slot, itemStack) -> {
            if (found[0] != null) {
                return;
            }
            if (itemStack != null && !itemStack.isEmpty()) {
                found[0] = new ContainerSlotRef(container, slot, (ItemStack)itemStack);
            }
        });
        return found[0];
    }

    public static boolean hasAnyItemId(NPCEntity npcEntity, Set<String> itemIds) {
        if (npcEntity == null || itemIds == null || itemIds.isEmpty()) {
            return false;
        }
        try {
            Inventory inventory = npcEntity.getInventory();
            if (inventory == null) {
                return false;
            }
            CombinedItemContainer container = inventory.getCombinedHotbarFirst();
            if (container == null) {
                return false;
            }
            boolean[] found = new boolean[]{false};
            container.forEach((slot, stack) -> {
                if (found[0]) {
                    return;
                }
                if (stack == null || stack.isEmpty()) {
                    return;
                }
                String id = stack.getItemId();
                if (id == null || id.isBlank()) {
                    return;
                }
                if (itemIds.contains(id)) {
                    found[0] = true;
                }
            });
            return found[0];
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private static void logContainerSnapshot(NPCEntity npcEntity, String context, List<ItemContainer> chosenContainers) {
    }

    private static String describeContainers(List<ItemContainer> containers) {
        if (containers == null || containers.isEmpty()) {
            return "[]";
        }
        ArrayList<String> parts = new ArrayList<String>();
        for (ItemContainer container : containers) {
            parts.add(InvUtil.describeContainer(container == null ? "null" : container.getClass().getSimpleName(), container));
        }
        return ((Object)parts).toString();
    }

    private static String describeContainer(String label, ItemContainer container) {
        if (container == null) {
            return label + "(null)";
        }
        return label + "(slots=" + InvUtil.getContainerSlotCount(container) + ",occupied=" + InvUtil.getOccupiedSlotCount(container) + ",type=" + container.getClass().getSimpleName() + ")";
    }

    public static final class ContainerSlotRef {
        public final ItemContainer container;
        public final short slot;
        public final ItemStack stack;

        public ContainerSlotRef(ItemContainer container, short slot, ItemStack stack) {
            this.container = container;
            this.slot = slot;
            this.stack = stack;
        }
    }
}

