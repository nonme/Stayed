/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.entity.entities.Player
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.server.core.entity.entities.Player;

public interface CitizenListener {
    default public void onCitizenInteract(Player player, String citizenId) {
    }

    default public void onDialogChoice(Player player, String dialogId, int choiceIndex, String choiceText) {
    }

    default public void onDialogComplete(Player player, String dialogId) {
    }

    default public void onDialogInput(Player player, String dialogId, String input) {
    }

    default public void onCitizenProximityEnter(Player player, String citizenId, float distance) {
    }

    default public void onCitizenProximityExit(Player player, String citizenId) {
    }

    default public void onCitizenScheduleChange(String citizenId, String fromPeriod, String toPeriod) {
    }

    default public void onCitizenBeaconReceived(String citizenId, String message) {
    }

    default public void onCitizenDeath(String citizenId) {
    }

    default public void onCitizenSpawn(String citizenId) {
    }
}

