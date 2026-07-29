/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.logger.HytaleLogger
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.server.npc.entities.NPCEntity
 *  com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport
 */
package com.yourname.companion.systems;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;

final class WorkPositioning {
    private static final String MANAGED_ROLE = "FH_Companion_Managed";
    private static final int WORK_TARGET_SLOT = 0;
    private static final int AUTONOMOUS_TARGET_SLOT = 1;
    private static Field storedPositionsField;
    private static boolean storedPositionsLookupFailed;

    private WorkPositioning() {
    }

    static boolean isManagedCompanion(NPCEntity npcEntity) {
        return npcEntity != null && MANAGED_ROLE.equalsIgnoreCase(npcEntity.getRoleName());
    }

    static boolean setManagedWorkPosition(NPCEntity npcEntity, Vector3d targetPos, HytaleLogger logger) {
        return WorkPositioning.setStoredPosition(npcEntity, 0, targetPos, true, logger);
    }

    static boolean setManagedAutonomousPosition(NPCEntity npcEntity, Vector3d targetPos, HytaleLogger logger) {
        return WorkPositioning.setStoredPosition(npcEntity, 1, targetPos, true, logger);
    }

    static void clearManagedAutonomousPosition(NPCEntity npcEntity, HytaleLogger logger) {
        WorkPositioning.clearStoredPosition(npcEntity, 1, logger);
    }

    static void clearManagedWorkPosition(NPCEntity npcEntity, HytaleLogger logger) {
        WorkPositioning.clearStoredPosition(npcEntity, 0, logger);
    }

    private static boolean setStoredPosition(NPCEntity npcEntity, int slot, Vector3d targetPos, boolean clearTargets, HytaleLogger logger) {
        if (!WorkPositioning.isManagedCompanion(npcEntity) || targetPos == null) {
            return false;
        }
        try {
            Vector3d[] slots = WorkPositioning.resolveStoredPositions(npcEntity);
            if (slots == null || slots.length <= slot) {
                return false;
            }
            slots[slot] = new Vector3d(targetPos.x, targetPos.y, targetPos.z);
            if (clearTargets) {
                WorkPositioning.clearFollowTargets(npcEntity);
            }
            try {
                npcEntity.getRole().getWorldSupport().requestNewPath();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            return true;
        }
        catch (Throwable t) {
            if (logger != null) {
                ((HytaleLogger.Api)logger.at(Level.WARNING).withCause(t)).log("Failed to set managed work position.");
            }
            return false;
        }
    }

    private static void clearStoredPosition(NPCEntity npcEntity, int slot, HytaleLogger logger) {
        block4: {
            if (!WorkPositioning.isManagedCompanion(npcEntity)) {
                return;
            }
            try {
                Vector3d[] slots = WorkPositioning.resolveStoredPositions(npcEntity);
                if (slots == null || slots.length <= slot) {
                    return;
                }
                slots[slot] = Vector3d.MIN;
            }
            catch (Throwable t) {
                if (logger == null) break block4;
                ((HytaleLogger.Api)logger.at(Level.WARNING).withCause(t)).log("Failed to clear managed work position.");
            }
        }
    }

    private static Vector3d[] resolveStoredPositions(NPCEntity npcEntity) throws ReflectiveOperationException {
        if (storedPositionsLookupFailed || npcEntity == null || npcEntity.getRole() == null) {
            return null;
        }
        MarkedEntitySupport marked = npcEntity.getRole().getMarkedEntitySupport();
        if (marked == null) {
            return null;
        }
        if (storedPositionsField == null) {
            try {
                storedPositionsField = marked.getClass().getDeclaredField("storedPositions");
                storedPositionsField.setAccessible(true);
            }
            catch (ReflectiveOperationException e) {
                storedPositionsLookupFailed = true;
                throw e;
            }
        }
        return (Vector3d[])storedPositionsField.get(marked);
    }

    private static void clearFollowTargets(NPCEntity npcEntity) {
        try {
            npcEntity.onFlockSetTarget("LockedTarget", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            npcEntity.onFlockSetTarget("Target", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            npcEntity.onFlockSetTarget("CombatTarget", null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        WorkPositioning.clearDirectFlockTarget(npcEntity);
    }

    private static void clearDirectFlockTarget(NPCEntity npcEntity) {
        String[] methodNames;
        if (npcEntity == null) {
            return;
        }
        for (String methodName : methodNames = new String[]{"onFlockSetTarget", "onFlockSetMarkedTarget", "onFlockSetMarkedEntity"}) {
            try {
                for (Method method : npcEntity.getClass().getMethods()) {
                    if (!method.getName().equals(methodName) || method.getParameterCount() != 1) continue;
                    method.invoke((Object)npcEntity, new Object[]{null});
                    return;
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
    }
}

