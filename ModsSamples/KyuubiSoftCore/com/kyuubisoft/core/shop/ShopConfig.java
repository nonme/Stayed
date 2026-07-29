/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.shop;

import java.util.ArrayList;
import java.util.List;

public class ShopConfig {
    public String id;
    public String title;
    public String permission;
    public String currencyId;
    public ShopSettings settings = new ShopSettings();
    public List<ShopItemConfig> items = new ArrayList<ShopItemConfig>();
    public boolean buyEnabled = true;
    public boolean sellEnabled = false;
    public String sellCurrencyId;
    public int sellPricePercent = 50;
    public List<String> sellWhitelist;
    public List<String> sellBlacklist;
    public List<SellItemConfig> sellItems;

    public void mergeItems(ShopConfig other) {
        if (other == null || other.items == null) {
            return;
        }
        int replaced = 0;
        int added = 0;
        for (ShopItemConfig customItem : other.items) {
            if (customItem.itemId == null) continue;
            boolean found = false;
            for (int i = 0; i < this.items.size(); ++i) {
                if (!customItem.itemId.equals(this.items.get((int)i).itemId)) continue;
                this.items.set(i, customItem);
                found = true;
                ++replaced;
                break;
            }
            if (found) continue;
            this.items.add(customItem);
            ++added;
        }
    }

    public static class ShopSettings {
        public int itemsPerPage = 9;
        public int dailyShopSize = 0;
        public boolean showSoldOut = true;
        public boolean allowBuyMultiple = false;
    }

    public static class ShopItemConfig {
        public String itemId;
        public String name;
        public int quantity = 1;
        public int cost;
        public int sellPrice = 0;
        public String description;
        public int weight = 100;
        public String category;
        public int maxPurchases = 0;
        public int maxSells = 0;
        public String permission;
        public String command;
        public boolean commandRunAsServer = true;

        public boolean isCommand() {
            return this.command != null && !this.command.trim().isEmpty();
        }
    }

    public static class SellItemConfig {
        public String itemId;
        public int sellPrice;
        public int maxSellsPerDay = 0;
    }
}

