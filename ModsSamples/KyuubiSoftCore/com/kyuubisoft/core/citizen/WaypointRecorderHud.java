/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud
 *  com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  javax.annotation.Nonnull
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.kyuubisoft.core.i18n.CoreI18n;
import javax.annotation.Nonnull;

public class WaypointRecorderHud
extends CustomUIHud {
    private static final long BUILD_GRACE_MS = 1000L;
    private final String citizenId;
    private int waypointCount;
    private volatile long builtAtMs = 0L;

    public WaypointRecorderHud(PlayerRef playerRef, String citizenId, int initialCount) {
        super(playerRef);
        this.citizenId = citizenId;
        this.waypointCount = initialCount;
    }

    protected void build(@Nonnull UICommandBuilder ui) {
        ui.append("Hud/Citizen/WaypointRecorder.ui");
        ui.set("#WpRecTitle.Text", CoreI18n.getInstance().get("citizen.wprec.recording", this.citizenId));
        ui.set("#WpRecCount.Text", CoreI18n.getInstance().get("citizen.wprec.count", this.waypointCount));
        this.builtAtMs = System.currentTimeMillis();
    }

    public void updateCount(int count) {
        this.waypointCount = count;
        if (this.builtAtMs == 0L || System.currentTimeMillis() - this.builtAtMs < 1000L) {
            return;
        }
        UICommandBuilder ui = new UICommandBuilder();
        ui.set("#WpRecCount.Text", CoreI18n.getInstance().get("citizen.wprec.count", count));
        this.update(false, ui);
    }
}

