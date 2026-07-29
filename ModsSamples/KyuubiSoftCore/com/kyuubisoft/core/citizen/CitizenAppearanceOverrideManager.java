/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.citizen;

import com.kyuubisoft.core.citizen.CitizenBeaconManager;
import com.kyuubisoft.core.citizen.CitizenData;
import com.kyuubisoft.core.citizen.CitizenScheduleManager;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class CitizenAppearanceOverrideManager {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft CitizenAppearance");
    private final Map<String, CitizenData.AppearanceOverride> activeOverrides = new ConcurrentHashMap<String, CitizenData.AppearanceOverride>();
    private final Map<String, OriginalAppearance> originals = new ConcurrentHashMap<String, OriginalAppearance>();
    private final Set<String> pendingRespawns = ConcurrentHashMap.newKeySet();

    public void tick(Collection<CitizenData> citizens, CitizenScheduleManager scheduleManager, CitizenBeaconManager beaconManager) {
        for (CitizenData citizen : citizens) {
            CitizenData.AppearanceOverride current;
            CitizenData.AppearanceOverride matched;
            boolean same;
            if (citizen.appearanceOverrides == null || citizen.appearanceOverrides.isEmpty() || (same = (matched = this.evaluate(citizen, scheduleManager, beaconManager)) == (current = this.activeOverrides.get(citizen.id)) || matched != null && current != null && Objects.equals(matched.condition, current.condition))) continue;
            if (matched != null) {
                this.applyOverride(citizen, matched);
                continue;
            }
            this.restoreOriginal(citizen);
        }
    }

    public CitizenData.AppearanceOverride evaluate(CitizenData citizen, CitizenScheduleManager scheduleManager, CitizenBeaconManager beaconManager) {
        if (citizen.appearanceOverrides == null) {
            return null;
        }
        for (CitizenData.AppearanceOverride override : citizen.appearanceOverrides) {
            if (!this.evaluateCondition(override.condition, citizen.id, scheduleManager, beaconManager)) continue;
            return override;
        }
        return null;
    }

    private boolean evaluateCondition(String condition, String citizenId, CitizenScheduleManager scheduleManager, CitizenBeaconManager beaconManager) {
        if (condition == null || condition.isEmpty()) {
            return false;
        }
        String[] parts = condition.split(":", 2);
        if (parts.length != 2) {
            return false;
        }
        return switch (parts[0]) {
            case "schedule" -> {
                if (scheduleManager != null && parts[1].equals(scheduleManager.getCurrentPeriod(citizenId))) {
                    yield true;
                }
                yield false;
            }
            case "beacon" -> {
                if (beaconManager != null && beaconManager.getActiveBeacons(citizenId).contains(parts[1])) {
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    private void applyOverride(CitizenData citizen, CitizenData.AppearanceOverride override) {
        if (!this.originals.containsKey(citizen.id)) {
            OriginalAppearance orig = new OriginalAppearance();
            orig.entityTypeId = citizen.entityTypeId;
            orig.skinUsername = citizen.skinUsername;
            orig.helmet = citizen.helmet;
            orig.chest = citizen.chest;
            orig.leggings = citizen.leggings;
            orig.gloves = citizen.gloves;
            orig.mainHand = citizen.mainHand;
            orig.offHand = citizen.offHand;
            this.originals.put(citizen.id, orig);
        }
        if (override.entityTypeId != null) {
            citizen.entityTypeId = override.entityTypeId;
        }
        if (override.skinUsername != null) {
            citizen.skinUsername = override.skinUsername;
        }
        if (override.helmet != null) {
            citizen.helmet = override.helmet;
        }
        if (override.chest != null) {
            citizen.chest = override.chest;
        }
        if (override.leggings != null) {
            citizen.leggings = override.leggings;
        }
        if (override.gloves != null) {
            citizen.gloves = override.gloves;
        }
        if (override.mainHand != null) {
            citizen.mainHand = override.mainHand;
        }
        if (override.offHand != null) {
            citizen.offHand = override.offHand;
        }
        this.activeOverrides.put(citizen.id, override);
        this.pendingRespawns.add(citizen.id);
        LOGGER.fine("[APPEARANCE] " + citizen.id + " override applied: " + override.condition);
    }

    private void restoreOriginal(CitizenData citizen) {
        OriginalAppearance orig = this.originals.remove(citizen.id);
        if (orig == null) {
            return;
        }
        citizen.entityTypeId = orig.entityTypeId;
        citizen.skinUsername = orig.skinUsername;
        citizen.helmet = orig.helmet;
        citizen.chest = orig.chest;
        citizen.leggings = orig.leggings;
        citizen.gloves = orig.gloves;
        citizen.mainHand = orig.mainHand;
        citizen.offHand = orig.offHand;
        this.activeOverrides.remove(citizen.id);
        this.pendingRespawns.add(citizen.id);
        LOGGER.fine("[APPEARANCE] " + citizen.id + " restored to original");
    }

    public Set<String> drainPendingRespawns() {
        HashSet<String> result = new HashSet<String>(this.pendingRespawns);
        this.pendingRespawns.clear();
        return result;
    }

    public CitizenData.AppearanceOverride getActiveOverride(String citizenId) {
        return this.activeOverrides.get(citizenId);
    }

    public void clearAll() {
        this.activeOverrides.clear();
        this.originals.clear();
        this.pendingRespawns.clear();
    }

    private static class OriginalAppearance {
        String entityTypeId;
        String skinUsername;
        String helmet;
        String chest;
        String leggings;
        String gloves;
        String mainHand;
        String offHand;

        private OriginalAppearance() {
        }
    }
}

