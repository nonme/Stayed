/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.citizen;

import com.kyuubisoft.core.citizen.CitizenData;
import com.kyuubisoft.core.citizen.CitizenService;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class CitizenScheduleManager {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft CitizenSchedule");
    private final Map<String, String> currentPeriods = new ConcurrentHashMap<String, String>();
    private final Map<String, OriginalState> originalStates = new ConcurrentHashMap<String, OriginalState>();

    public void tick(Collection<CitizenData> citizens, CitizenService service, float worldTime) {
        String period = CitizenScheduleManager.determinePeriod(worldTime);
        for (CitizenData citizen : citizens) {
            String oldPeriod;
            if (citizen.schedule == null || citizen.schedule.isEmpty() || period.equals(oldPeriod = this.currentPeriods.get(citizen.id))) continue;
            CitizenData.ScheduleEntry entry = this.findEntry(citizen, period);
            String previousPeriod = oldPeriod;
            this.currentPeriods.put(citizen.id, period);
            if (entry != null) {
                this.applySchedule(citizen, entry, previousPeriod, period, service);
                continue;
            }
            this.restoreOriginal(citizen, previousPeriod, period, service);
        }
    }

    static String determinePeriod(float worldTime) {
        int hour = (int)(worldTime * 24.0f) % 24;
        if (hour >= 5 && hour <= 7) {
            return "dawn";
        }
        if (hour >= 8 && hour <= 17) {
            return "day";
        }
        if (hour >= 18 && hour <= 19) {
            return "dusk";
        }
        return "night";
    }

    private CitizenData.ScheduleEntry findEntry(CitizenData citizen, String period) {
        for (CitizenData.ScheduleEntry e : citizen.schedule) {
            if (!period.equals(e.period)) continue;
            return e;
        }
        return null;
    }

    private void applySchedule(CitizenData citizen, CitizenData.ScheduleEntry entry, String oldPeriod, String newPeriod, CitizenService service) {
        if (!this.originalStates.containsKey(citizen.id)) {
            OriginalState orig = new OriginalState();
            orig.movementType = citizen.movementType;
            orig.movementRadius = citizen.movementRadius;
            orig.hideNpc = citizen.hideNpc;
            orig.attitude = citizen.attitude;
            this.originalStates.put(citizen.id, orig);
        }
        boolean wasHidden = citizen.hideNpc;
        if (entry.movementType != null) {
            citizen.movementType = entry.movementType;
            citizen.movementRadius = entry.movementRadius;
        }
        if (entry.attitude != null) {
            citizen.attitude = entry.attitude;
        }
        if (entry.hidden && !wasHidden) {
            citizen.hideNpc = true;
            service.despawnCitizen(citizen);
            LOGGER.fine("[SCHED] " + citizen.id + " hidden for period " + newPeriod);
        } else if (!entry.hidden && wasHidden) {
            citizen.hideNpc = false;
            LOGGER.fine("[SCHED] " + citizen.id + " shown for period " + newPeriod);
        }
        service.dispatchScheduleChange(citizen.id, oldPeriod, newPeriod);
        LOGGER.fine("[SCHED] " + citizen.id + ": " + oldPeriod + " -> " + newPeriod);
    }

    private void restoreOriginal(CitizenData citizen, String oldPeriod, String newPeriod, CitizenService service) {
        OriginalState orig = this.originalStates.remove(citizen.id);
        if (orig == null) {
            service.dispatchScheduleChange(citizen.id, oldPeriod, newPeriod);
            return;
        }
        boolean wasHidden = citizen.hideNpc;
        citizen.movementType = orig.movementType;
        citizen.movementRadius = orig.movementRadius;
        citizen.attitude = orig.attitude;
        if (orig.hideNpc != wasHidden) {
            citizen.hideNpc = orig.hideNpc;
            if (!orig.hideNpc && wasHidden) {
                LOGGER.fine("[SCHED] " + citizen.id + " restored visible for period " + newPeriod);
            }
        }
        service.dispatchScheduleChange(citizen.id, oldPeriod, newPeriod);
        LOGGER.fine("[SCHED] " + citizen.id + " restored to original for period " + newPeriod);
    }

    public String getCurrentPeriod(String citizenId) {
        return this.currentPeriods.get(citizenId);
    }

    public void unregister(String citizenId) {
        this.currentPeriods.remove(citizenId);
        this.originalStates.remove(citizenId);
    }

    public void clearAll() {
        this.currentPeriods.clear();
        this.originalStates.clear();
    }

    private static class OriginalState {
        String movementType;
        float movementRadius;
        boolean hideNpc;
        String attitude;

        private OriginalState() {
        }
    }
}

