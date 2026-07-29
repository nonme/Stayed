/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.entity.entities.Player
 */
package com.kyuubisoft.core.lootbag;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.kyuubisoft.core.lootbag.LootbagAdminService;
import com.kyuubisoft.core.lootbag.LootbagDefinition;
import com.kyuubisoft.core.util.ItemUtils;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class LootbagService {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core");
    private static final SecureRandom RANDOM = new SecureRandom();

    public static List<ResolvedItem> grantByName(Player player, String lootbagId) {
        LootbagDefinition def = LootbagService.getDefinition(lootbagId);
        if (def == null) {
            LOGGER.warning("Lootbag not found: " + lootbagId);
            return List.of();
        }
        return LootbagService.resolveAndGrant(player, def);
    }

    public static boolean grant(Player player, String lootbagId) {
        List<ResolvedItem> items = LootbagService.grantByName(player, lootbagId);
        return !items.isEmpty() && items.stream().anyMatch(ResolvedItem::isCollected);
    }

    public static LootbagDefinition getDefinition(String lootbagId) {
        LootbagAdminService admin = LootbagAdminService.getInstance();
        if (admin == null) {
            return null;
        }
        return admin.get(lootbagId);
    }

    public static Map<String, LootbagDefinition> getAllDefinitions() {
        LootbagAdminService admin = LootbagAdminService.getInstance();
        if (admin == null) {
            return Collections.emptyMap();
        }
        return admin.getAll();
    }

    public static boolean exists(String lootbagId) {
        return LootbagService.getDefinition(lootbagId) != null;
    }

    public static List<ResolvedItem> resolve(LootbagDefinition definition) {
        ArrayList<ResolvedItem> result = new ArrayList<ResolvedItem>();
        if (definition.getItems() != null) {
            for (LootbagDefinition.LootItem item : definition.getItems()) {
                result.add(new ResolvedItem(item.getItemId(), item.getAmount()));
            }
        }
        if (definition.getGuaranteedItems() != null) {
            for (LootbagDefinition.LootItem item : definition.getGuaranteedItems()) {
                result.add(new ResolvedItem(item.getItemId(), item.getAmount()));
            }
        }
        if (definition.getPool() != null && !definition.getPool().isEmpty() && definition.getPickCount() > 0) {
            result.addAll(LootbagService.pickRandom(definition.getPool(), definition.getPickCount(), definition.isAllowDuplicates()));
        }
        return result;
    }

    public static boolean grantResolved(Player player, List<ResolvedItem> items) {
        boolean allSuccess = true;
        for (ResolvedItem item : items) {
            if (item.isCollected()) continue;
            boolean success = ItemUtils.grantItem(player, item.getItemId(), item.getAmount());
            if (success) {
                item.setCollected(true);
                continue;
            }
            allSuccess = false;
        }
        return allSuccess;
    }

    public static List<ResolvedItem> resolveAndGrant(Player player, LootbagDefinition definition) {
        List<ResolvedItem> items = LootbagService.resolve(definition);
        LootbagService.grantResolved(player, items);
        LOGGER.info("Granted lootbag '" + definition.getId() + "' with " + items.size() + " items to " + player.getDisplayName());
        return items;
    }

    private static List<ResolvedItem> pickRandom(List<LootbagDefinition.PoolItem> pool, int count, boolean allowDuplicates) {
        int totalWeight;
        ArrayList<ResolvedItem> result = new ArrayList<ResolvedItem>();
        ArrayList<LootbagDefinition.PoolItem> available = new ArrayList<LootbagDefinition.PoolItem>(pool);
        block0: for (int i = 0; i < count && !available.isEmpty() && (totalWeight = available.stream().mapToInt(LootbagDefinition.PoolItem::getWeight).sum()) > 0; ++i) {
            int roll = RANDOM.nextInt(totalWeight);
            int cumulative = 0;
            for (int j = 0; j < available.size(); ++j) {
                if (roll >= (cumulative += ((LootbagDefinition.PoolItem)available.get(j)).getWeight())) continue;
                LootbagDefinition.PoolItem picked = (LootbagDefinition.PoolItem)available.get(j);
                result.add(new ResolvedItem(picked.getItemId(), picked.getAmount()));
                if (allowDuplicates) continue block0;
                available.remove(j);
                continue block0;
            }
        }
        return result;
    }

    public static class ResolvedItem {
        private final String itemId;
        private final int amount;
        private boolean collected;

        public ResolvedItem(String itemId, int amount) {
            this.itemId = itemId;
            this.amount = amount;
            this.collected = false;
        }

        public String getItemId() {
            return this.itemId;
        }

        public int getAmount() {
            return this.amount;
        }

        public boolean isCollected() {
            return this.collected;
        }

        public void setCollected(boolean collected) {
            this.collected = collected;
        }

        public String getDisplayName() {
            return this.amount + "x " + ItemUtils.formatItemName(this.itemId);
        }
    }
}

