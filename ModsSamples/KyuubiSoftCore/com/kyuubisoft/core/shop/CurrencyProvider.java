/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.entity.entities.Player
 */
package com.kyuubisoft.core.shop;

import com.hypixel.hytale.server.core.entity.entities.Player;

public interface CurrencyProvider {
    public String getCurrencyId();

    public String getDisplayName();

    public String getIconItemId();

    public int getBalance(Player var1);

    public boolean spend(Player var1, int var2);

    public void refund(Player var1, int var2);
}

