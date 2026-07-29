/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopPlayerData {
    private static final transient Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, ShopState> shopStates = new HashMap<String, ShopState>();

    public ShopState getShopState(String shopId) {
        return this.shopStates.computeIfAbsent(shopId, k -> new ShopState());
    }

    public boolean isEmpty() {
        return this.shopStates.isEmpty();
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static ShopPlayerData fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return new ShopPlayerData();
        }
        try {
            ShopPlayerData data = GSON.fromJson(json, ShopPlayerData.class);
            return data != null ? data : new ShopPlayerData();
        }
        catch (Exception e) {
            return new ShopPlayerData();
        }
    }

    public static class ShopState {
        private final Map<String, Integer> purchaseCounts = new HashMap<String, Integer>();
        private Map<String, Integer> sellCounts = new HashMap<String, Integer>();
        private List<String> dailyItemIds;
        private String dailyDateStr;
        private String purchaseDateStr;

        public int getPurchaseCount(String itemId) {
            return this.purchaseCounts.getOrDefault(itemId, 0);
        }

        public void recordPurchase(String itemId) {
            this.purchaseCounts.merge(itemId, 1, Integer::sum);
        }

        public int getSellCount(String itemId) {
            if (this.sellCounts == null) {
                return 0;
            }
            return this.sellCounts.getOrDefault(itemId, 0);
        }

        public void recordSell(String itemId) {
            if (this.sellCounts == null) {
                this.sellCounts = new HashMap<String, Integer>();
            }
            this.sellCounts.merge(itemId, 1, Integer::sum);
        }

        public void resetPurchases() {
            this.purchaseCounts.clear();
            if (this.sellCounts != null) {
                this.sellCounts.clear();
            }
        }

        public boolean checkDailyPurchaseReset() {
            String today = LocalDate.now().toString();
            if (today.equals(this.purchaseDateStr)) {
                return false;
            }
            this.purchaseDateStr = today;
            this.resetPurchases();
            return true;
        }

        public boolean isDailyShopValid() {
            if (this.dailyDateStr == null) {
                return false;
            }
            if (this.dailyItemIds == null || this.dailyItemIds.isEmpty()) {
                return false;
            }
            try {
                return LocalDate.parse(this.dailyDateStr).equals(LocalDate.now());
            }
            catch (Exception e) {
                return false;
            }
        }

        public List<String> getDailyItemIds() {
            return this.dailyItemIds;
        }

        public void setDailyItems(List<String> itemIds) {
            this.dailyItemIds = itemIds;
            this.purchaseDateStr = this.dailyDateStr = LocalDate.now().toString();
            this.resetPurchases();
        }
    }
}

