/*
 * Decompiled with CFR 0.152.
 */
package com.yourname.companion.data;

import com.yourname.companion.data.BlockPos;
import com.yourname.companion.data.CompanionMode;
import com.yourname.companion.data.FollowMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FallenCompanion {
    public String companionUniqueId;
    public String name;
    public String appearanceModelId;
    public int combatLevel;
    public int combatKills;
    public int farmLevel;
    public int farmHarvests;
    public CompanionMode mode = CompanionMode.FIGHTER;
    public FollowMode followMode = FollowMode.FOLLOW;
    public boolean farmAutoResume = false;
    public BlockPos farmAreaTopLeft;
    public BlockPos farmAreaBottomRight;
    public List<BlockPos> linkedChests = new ArrayList<BlockPos>();
    public String deathCause;
    public long deathTime;
    public Map<String, Integer> savedInventory = new LinkedHashMap<String, Integer>();

    public FallenCompanion() {
    }

    public FallenCompanion(String companionUniqueId, String name, String appearanceModelId, int combatLevel, int combatKills, int farmLevel, int farmHarvests, String deathCause, long deathTime, Map<String, Integer> savedInventory) {
        this.companionUniqueId = companionUniqueId;
        this.name = name;
        this.appearanceModelId = appearanceModelId;
        this.combatLevel = combatLevel;
        this.combatKills = combatKills;
        this.farmLevel = farmLevel;
        this.farmHarvests = farmHarvests;
        this.deathCause = deathCause;
        this.deathTime = deathTime;
        if (savedInventory != null) {
            this.savedInventory.putAll(savedInventory);
        }
    }

    public String getDisplayName() {
        return this.name != null && !this.name.isBlank() ? this.name : "Unnamed Companion";
    }

    public String getSummary() {
        String inv = this.savedInventory != null && !this.savedInventory.isEmpty() ? " (" + this.savedInventory.size() + " item type(s))" : "";
        return this.getDisplayName() + " [Combat Lv" + this.combatLevel + " / Farm Lv" + this.farmLevel + "]" + inv + (String)(this.deathCause != null ? " \u2014 Died: " + this.deathCause : "");
    }
}

