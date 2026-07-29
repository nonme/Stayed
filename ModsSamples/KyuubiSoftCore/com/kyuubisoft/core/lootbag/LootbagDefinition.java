/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.lootbag;

import java.util.List;

public class LootbagDefinition {
    private String name;
    private String description;
    private String icon;
    private String rarity;
    private List<LootItem> items;
    private List<LootItem> guaranteedItems;
    private Integer pickCount;
    private Boolean allowDuplicates;
    private List<PoolItem> pool;
    private transient String id;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name != null ? this.name : "Lootbag";
    }

    public String getDescription() {
        return this.description;
    }

    public String getIcon() {
        return this.icon != null ? this.icon : "Chest_Wood";
    }

    public String getRarity() {
        return this.rarity != null ? this.rarity : "common";
    }

    public List<LootItem> getItems() {
        return this.items;
    }

    public List<LootItem> getGuaranteedItems() {
        return this.guaranteedItems;
    }

    public int getPickCount() {
        return this.pickCount != null ? this.pickCount : 0;
    }

    public boolean isAllowDuplicates() {
        return this.allowDuplicates != null ? this.allowDuplicates : false;
    }

    public List<PoolItem> getPool() {
        return this.pool;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public void setItems(List<LootItem> items) {
        this.items = items;
    }

    public void setGuaranteedItems(List<LootItem> guaranteedItems) {
        this.guaranteedItems = guaranteedItems;
    }

    public void setPool(List<PoolItem> pool) {
        this.pool = pool;
    }

    public void setPickCount(Integer pickCount) {
        this.pickCount = pickCount;
    }

    public void setAllowDuplicates(Boolean allowDuplicates) {
        this.allowDuplicates = allowDuplicates;
    }

    public boolean isFixedItems() {
        return this.items != null && !this.items.isEmpty() && (this.pool == null || this.pool.isEmpty());
    }

    public boolean isRandomPool() {
        return !(this.items != null && !this.items.isEmpty() || this.guaranteedItems != null && !this.guaranteedItems.isEmpty() || this.pool == null || this.pool.isEmpty() || this.getPickCount() <= 0);
    }

    public boolean isHybrid() {
        return this.guaranteedItems != null && !this.guaranteedItems.isEmpty() && this.pool != null && !this.pool.isEmpty() && this.getPickCount() > 0;
    }

    public String getRarityColor() {
        return switch (this.getRarity().toLowerCase()) {
            case "uncommon" -> "#55FF55";
            case "rare" -> "#5555FF";
            case "epic" -> "#AA00AA";
            case "legendary" -> "#FFAA00";
            default -> "#AAAAAA";
        };
    }

    public static class PoolItem
    extends LootItem {
        private int weight;

        public PoolItem() {
        }

        public PoolItem(String itemId, int amount, int weight) {
            super(itemId, amount);
            this.weight = weight;
        }

        public int getWeight() {
            return this.weight > 0 ? this.weight : 1;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }
    }

    public static class LootItem {
        private String itemId;
        private int amount;

        public LootItem() {
        }

        public LootItem(String itemId, int amount) {
            this.itemId = itemId;
            this.amount = amount;
        }

        public String getItemId() {
            return this.itemId;
        }

        public int getAmount() {
            return this.amount > 0 ? this.amount : 1;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }
    }
}

