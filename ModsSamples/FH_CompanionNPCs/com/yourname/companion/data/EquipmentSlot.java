/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.asset.type.item.config.ItemArmor
 *  com.hypixel.hytale.server.core.inventory.ItemStack
 */
package com.yourname.companion.data;

import com.hypixel.hytale.server.core.asset.type.item.config.ItemArmor;
import com.hypixel.hytale.server.core.inventory.ItemStack;

public enum EquipmentSlot {
    WEAPON("Weapon"),
    OFFHAND("Offhand"),
    HELMET("Helmet"),
    CHESTPLATE("Chestplate"),
    LEGGINGS("Leggings"),
    BOOTS("Boots");

    private final String displayName;

    private EquipmentSlot(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public static EquipmentSlot forItem(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return null;
        }
        String lower = assetId.toLowerCase();
        if (EquipmentSlot.isAmmo(assetId)) {
            return null;
        }
        try {
            ItemStack stack = new ItemStack(assetId, 1);
            if (stack != null && !stack.isEmpty() && stack.getItem() != null) {
                ItemArmor armor = stack.getItem().getArmor();
                if (armor != null && armor.getArmorSlot() != null) {
                    String slotName = armor.getArmorSlot().name().toLowerCase();
                    if (slotName.contains("head")) {
                        return HELMET;
                    }
                    if (slotName.contains("chest")) {
                        return CHESTPLATE;
                    }
                    if (slotName.contains("leg")) {
                        return LEGGINGS;
                    }
                    if (slotName.contains("hand") || slotName.contains("foot")) {
                        return BOOTS;
                    }
                }
                if (stack.getItem().getWeapon() != null) {
                    if (lower.contains("shield")) {
                        return OFFHAND;
                    }
                    return WEAPON;
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        if (lower.startsWith("weapon_") || lower.contains("_sword") || lower.contains("_axe") || lower.contains("_spear") || lower.contains("_staff") || lower.contains("_bow") || lower.contains("_crossbow") || lower.contains("_club") || lower.contains("_wand") || lower.contains("_mace") || lower.contains("_daggers") || lower.contains("_battleaxe")) {
            if (lower.contains("shield")) {
                return OFFHAND;
            }
            return WEAPON;
        }
        if (lower.contains("shield") || lower.contains("_torch") || lower.equals("crude_torch")) {
            return OFFHAND;
        }
        if (lower.startsWith("armor_")) {
            if (lower.contains("_head") || lower.contains("_helm") || lower.contains("_hood") || lower.contains("_hat")) {
                return HELMET;
            }
            if (lower.contains("_chest") || lower.contains("_cuirass") || lower.contains("_tunic") || lower.contains("_vest")) {
                return CHESTPLATE;
            }
            if (lower.contains("_legs") || lower.contains("_greaves") || lower.contains("_pants") || lower.contains("_leggings")) {
                return LEGGINGS;
            }
            if (lower.contains("_hands") || lower.contains("_gauntlets") || lower.contains("_gloves") || lower.contains("_boots") || lower.contains("_feet")) {
                return BOOTS;
            }
        }
        return null;
    }

    public static boolean isAmmo(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return false;
        }
        String lower = assetId.toLowerCase();
        return lower.contains("arrow") || lower.contains("bolt") || lower.contains("ammo") || lower.contains("quiver");
    }

    public static boolean isTwoHanded(String assetId) {
        if (assetId == null) {
            return false;
        }
        String lower = assetId.toLowerCase();
        return lower.contains("battleaxe") || lower.contains("shortbow") || lower.contains("longbow") || lower.contains("crossbow") || lower.contains("longsword") || lower.contains("bow_") || lower.contains("gun") || lower.contains("rifle") || lower.contains("pistol") || lower.contains("handgun") || lower.contains("blunderbuss");
    }

    public static String getItemDisplayName(String assetId) {
        if (assetId == null) {
            return "Unknown";
        }
        return assetId.replace("Weapon_", "").replace("Tool_", "").replace("Armor_", "").replace("Seed_", "Seed: ").replace('_', ' ');
    }
}

