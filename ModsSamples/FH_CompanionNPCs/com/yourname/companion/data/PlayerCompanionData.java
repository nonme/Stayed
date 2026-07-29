/*
 * Decompiled with CFR 0.152.
 */
package com.yourname.companion.data;

import com.yourname.companion.data.BlockPos;
import com.yourname.companion.data.CompanionRecord;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class PlayerCompanionData {
    public UUID ownerId;
    public List<CompanionRecord> companions = new ArrayList<CompanionRecord>();
    public String selectedCompanionId;
    public double followDistance = 3.0;
    public double teleportDistance = 25.0;
    public int lootRadius = 8;
    public int chestScanLimit = 24;
    public List<BlockPos> linkedChests = new ArrayList<BlockPos>();
    public Set<String> lootWhitelist = new HashSet<String>();
    public Set<String> lootBlacklist = new HashSet<String>();
    public boolean lootEnabled = true;
    public boolean depositEnabled = true;
    public boolean allowDepositWhileStay = true;
    public boolean allowCompanionVsCompanionDamage = false;
    public boolean allowCompanionVsPlayerDamage = false;
    public boolean consumeCompanionAmmo = false;
    public boolean uiTooltipsEnabled = false;
    public int moveItemsCap = 640;
    public Set<String> protectedItemIds = new HashSet<String>();

    public PlayerCompanionData() {
    }

    public PlayerCompanionData(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public List<CompanionRecord> getActiveCompanions() {
        ArrayList<CompanionRecord> result = new ArrayList<CompanionRecord>();
        for (CompanionRecord c : this.companions) {
            if (!c.active || c.fallen) continue;
            result.add(c);
        }
        return result;
    }

    public List<CompanionRecord> getDormantCompanions() {
        ArrayList<CompanionRecord> result = new ArrayList<CompanionRecord>();
        for (CompanionRecord c : this.companions) {
            if (c.active || c.fallen) continue;
            result.add(c);
        }
        return result;
    }

    public List<CompanionRecord> getFallenCompanions() {
        ArrayList<CompanionRecord> result = new ArrayList<CompanionRecord>();
        for (CompanionRecord c : this.companions) {
            if (!c.fallen) continue;
            result.add(c);
        }
        return result;
    }

    public CompanionRecord findCompanion(String uniqueId) {
        if (uniqueId == null) {
            return null;
        }
        for (CompanionRecord c : this.companions) {
            if (!uniqueId.equals(c.uniqueId)) continue;
            return c;
        }
        return null;
    }

    public CompanionRecord findCompanionByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String lower = name.toLowerCase(Locale.ROOT).trim();
        for (CompanionRecord c : this.companions) {
            if (c.name == null || !c.name.toLowerCase(Locale.ROOT).trim().equals(lower)) continue;
            return c;
        }
        return null;
    }

    public CompanionRecord findCompanionByEntityId(String entityId) {
        if (entityId == null) {
            return null;
        }
        for (CompanionRecord c : this.companions) {
            if (!entityId.equals(c.entityId)) continue;
            return c;
        }
        return null;
    }

    public CompanionRecord getSelectedCompanion() {
        return this.findCompanion(this.selectedCompanionId);
    }

    public CompanionRecord resolveSelectedOrOnly() {
        CompanionRecord selected = this.getSelectedCompanion();
        if (selected != null && selected.active && !selected.fallen) {
            return selected;
        }
        List<CompanionRecord> actives = this.getActiveCompanions();
        if (actives.size() == 1) {
            return actives.get(0);
        }
        return null;
    }

    public boolean hasAnyActiveCompanion() {
        for (CompanionRecord c : this.companions) {
            if (!c.active || c.fallen) continue;
            return true;
        }
        return false;
    }
}

