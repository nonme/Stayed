/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.ComponentType
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.inventory.InventoryComponent
 *  com.hypixel.hytale.server.core.inventory.InventoryComponent$Armor
 *  com.hypixel.hytale.server.core.inventory.InventoryComponent$Backpack
 *  com.hypixel.hytale.server.core.inventory.InventoryComponent$Hotbar
 *  com.hypixel.hytale.server.core.inventory.InventoryComponent$Storage
 *  com.hypixel.hytale.server.core.inventory.InventoryComponent$Tool
 *  com.hypixel.hytale.server.core.inventory.InventoryComponent$Utility
 *  com.hypixel.hytale.server.core.inventory.ItemStack
 *  com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer
 *  com.hypixel.hytale.server.core.inventory.container.ItemContainer
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.kyuubisoft.core.util;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Field;

public class PlayerInventoryAccess {
    private final Ref<EntityStore> ref;
    private final Store<EntityStore> store;

    private PlayerInventoryAccess(Ref<EntityStore> ref, Store<EntityStore> store) {
        this.ref = ref;
        this.store = store;
    }

    public static PlayerInventoryAccess of(Player player) {
        Ref ref = player.getReference();
        return new PlayerInventoryAccess((Ref<EntityStore>)ref, (Store<EntityStore>)ref.getStore());
    }

    public static PlayerInventoryAccess of(Ref<EntityStore> ref, Store<EntityStore> store) {
        return new PlayerInventoryAccess(ref, store);
    }

    public ItemContainer getHotbar() {
        InventoryComponent.Hotbar comp = (InventoryComponent.Hotbar)this.store.getComponent(this.ref, InventoryComponent.Hotbar.getComponentType());
        return comp != null ? comp.getInventory() : null;
    }

    public ItemContainer getStorage() {
        InventoryComponent.Storage comp = (InventoryComponent.Storage)this.store.getComponent(this.ref, InventoryComponent.Storage.getComponentType());
        return comp != null ? comp.getInventory() : null;
    }

    public ItemContainer getArmor() {
        InventoryComponent.Armor comp = (InventoryComponent.Armor)this.store.getComponent(this.ref, InventoryComponent.Armor.getComponentType());
        return comp != null ? comp.getInventory() : null;
    }

    public ItemContainer getTools() {
        InventoryComponent.Tool comp = (InventoryComponent.Tool)this.store.getComponent(this.ref, InventoryComponent.Tool.getComponentType());
        return comp != null ? comp.getInventory() : null;
    }

    public ItemContainer getUtility() {
        InventoryComponent.Utility comp = (InventoryComponent.Utility)this.store.getComponent(this.ref, InventoryComponent.Utility.getComponentType());
        return comp != null ? comp.getInventory() : null;
    }

    public ItemContainer getBackpack() {
        InventoryComponent.Backpack comp = (InventoryComponent.Backpack)this.store.getComponent(this.ref, InventoryComponent.Backpack.getComponentType());
        return comp != null ? comp.getInventory() : null;
    }

    public ItemStack getItemInHand() {
        ItemContainer hotbar = this.getHotbar();
        if (hotbar == null) {
            return null;
        }
        byte activeSlot = this.getActiveHotbarSlot();
        return hotbar.getItemStack((short)activeSlot);
    }

    public CombinedItemContainer getCombinedHotbarFirst() {
        return InventoryComponent.getCombined(this.store, this.ref, (ComponentType[])InventoryComponent.HOTBAR_FIRST);
    }

    public CombinedItemContainer getCombinedStorageFirst() {
        return InventoryComponent.getCombined(this.store, this.ref, (ComponentType[])InventoryComponent.STORAGE_FIRST);
    }

    public CombinedItemContainer getCombinedEverything() {
        return InventoryComponent.getCombined(this.store, this.ref, (ComponentType[])InventoryComponent.EVERYTHING);
    }

    public ItemContainer getSectionById(int sectionId) {
        ComponentType compType = InventoryComponent.getComponentTypeById((int)sectionId);
        if (compType == null) {
            return null;
        }
        InventoryComponent comp = (InventoryComponent)this.store.getComponent(this.ref, compType);
        return comp != null ? comp.getInventory() : null;
    }

    public byte getActiveHotbarSlot() {
        try {
            InventoryComponent.Hotbar comp = (InventoryComponent.Hotbar)this.store.getComponent(this.ref, InventoryComponent.Hotbar.getComponentType());
            if (comp == null) {
                return 0;
            }
            Field field = InventoryComponent.Hotbar.class.getDeclaredField("activeSlot");
            field.setAccessible(true);
            return field.getByte(comp);
        }
        catch (Exception e) {
            return 0;
        }
    }

    public void markChanged() {
        try {
            InventoryComponent.Hotbar hotbar = (InventoryComponent.Hotbar)this.store.getComponent(this.ref, InventoryComponent.Hotbar.getComponentType());
            if (hotbar != null) {
                hotbar.markDirty();
            }
        }
        catch (Exception hotbar) {
            // empty catch block
        }
        try {
            InventoryComponent.Storage storage = (InventoryComponent.Storage)this.store.getComponent(this.ref, InventoryComponent.Storage.getComponentType());
            if (storage != null) {
                storage.markDirty();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}

