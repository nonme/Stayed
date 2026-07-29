/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.entity.entities.Player
 */
package com.kyuubisoft.core.economy;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.kyuubisoft.core.economy.ExternalEconomyBridge;
import com.kyuubisoft.core.shop.CurrencyProvider;

public class ExternalEconomyCurrencyProvider
implements CurrencyProvider {
    @Override
    public String getCurrencyId() {
        return "economy";
    }

    @Override
    public String getDisplayName() {
        String name = ExternalEconomyBridge.getInstance().getCurrencyName();
        return name != null ? name : "Money";
    }

    @Override
    public String getIconItemId() {
        return "Ingredient_Bar_Gold";
    }

    @Override
    public int getBalance(Player player) {
        return (int)ExternalEconomyBridge.getInstance().getBalance(player.getPlayerRef().getUuid());
    }

    @Override
    public boolean spend(Player player, int amount) {
        return ExternalEconomyBridge.getInstance().withdraw(player.getPlayerRef().getUuid(), amount);
    }

    @Override
    public void refund(Player player, int amount) {
        ExternalEconomyBridge.getInstance().deposit(player.getPlayerRef().getUuid(), amount);
    }
}

