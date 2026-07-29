/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.entity.entities.Player
 */
package com.kyuubisoft.core.tracking;

import com.hypixel.hytale.server.core.entity.entities.Player;

public interface TrackingListener {
    default public void onBlockBroken(Player player, String blockId, String blockGroup, String category) {
    }

    default public void onBlockPlaced(Player player, String blockId, String blockGroup) {
    }

    default public void onKill(Player player, String victimType, boolean isPlayerKill) {
    }

    default public void onDamageDealt(Player player, int damage, String victimType) {
    }

    default public void onDamageTaken(Player player, int damage, String causeType, String sourceType) {
    }

    default public void onDistanceTraveled(Player player, int blocks) {
    }

    default public void onPlaytimeMinute(Player player) {
    }

    default public void onBlockHarvested(Player player, String blockId, String blockGroup) {
    }

    default public void onFlowerPicked(Player player, String blockId, String blockGroup) {
    }

    default public void onZoneDiscovered(Player player, String zoneName) {
    }

    default public void onItemCrafted(Player player, String itemId, int amount) {
    }
}

