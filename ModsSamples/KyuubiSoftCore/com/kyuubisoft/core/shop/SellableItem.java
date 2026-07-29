/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.shop;

public class SellableItem {
    public final String itemId;
    public final int quantity;
    public final int sellPrice;

    public SellableItem(String itemId, int quantity, int sellPrice) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.sellPrice = sellPrice;
    }
}

