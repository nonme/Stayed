/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.entity.entities.Player
 */
package com.kyuubisoft.core.shop;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.kyuubisoft.core.shop.CurrencyProvider;
import com.kyuubisoft.core.util.ItemUtils;

public class ItemCurrencyProvider
implements CurrencyProvider {
    private final String itemId;
    private final String displayName;

    public ItemCurrencyProvider(String itemId, String displayName) {
        this.itemId = itemId;
        this.displayName = displayName;
    }

    public static ItemCurrencyProvider fromCurrencyId(String currencyId) {
        String itemId = currencyId.substring("item:".length());
        String name = ItemUtils.formatItemName(itemId);
        return new ItemCurrencyProvider(itemId, name);
    }

    @Override
    public String getCurrencyId() {
        return "item:" + this.itemId;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public String getIconItemId() {
        return this.itemId;
    }

    @Override
    public int getBalance(Player player) {
        return ItemUtils.countItem(player, this.itemId);
    }

    @Override
    public boolean spend(Player player, int amount) {
        return ItemUtils.removeItem(player, this.itemId, amount);
    }

    @Override
    public void refund(Player player, int amount) {
        ItemUtils.grantItem(player, this.itemId, amount);
    }
}

