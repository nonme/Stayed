/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.server.core.entity.UUIDComponent
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  com.hypixel.hytale.server.npc.corecomponents.ActionBase
 *  com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase
 *  com.hypixel.hytale.server.npc.role.Role
 *  com.hypixel.hytale.server.npc.sensorinfo.InfoProvider
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.kyuubisoft.core.api.CoreAPI;
import com.kyuubisoft.core.citizen.CitizenData;
import com.kyuubisoft.core.citizen.CitizenService;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActionCitizenInteract
extends ActionBase {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Citizen Action");

    public ActionCitizenInteract(BuilderActionBase builderActionBase) {
        super(builderActionBase);
        if (CoreAPI.isDebug()) {
            LOGGER.info("[CitizenAction] ActionCitizenInteract instance created");
        }
    }

    public boolean canExecute(Ref<EntityStore> ref, Role role, InfoProvider sensorInfo, double dt, Store<EntityStore> store) {
        boolean hasTarget;
        boolean baseResult = super.canExecute(ref, role, sensorInfo, dt, store);
        Ref target = role.getStateSupport().getInteractionIterationTarget();
        boolean bl = hasTarget = target != null;
        if (CoreAPI.isDebug()) {
            LOGGER.info("[CitizenAction] canExecute: base=" + baseResult + " hasTarget=" + hasTarget + " target=" + String.valueOf(target));
        }
        return baseResult && hasTarget;
    }

    public boolean execute(Ref<EntityStore> ref, Role role, InfoProvider sensorInfo, double dt, Store<EntityStore> store) {
        super.execute(ref, role, sensorInfo, dt, store);
        if (CoreAPI.isDebug()) {
            LOGGER.info("[CitizenAction] execute() called!");
        }
        try {
            CitizenService citizenService;
            Ref playerReference = role.getStateSupport().getInteractionIterationTarget();
            if (playerReference == null) {
                LOGGER.warning("[CitizenAction] playerReference is null");
                return false;
            }
            PlayerRef playerRef = (PlayerRef)store.getComponent(playerReference, PlayerRef.getComponentType());
            if (playerRef == null) {
                LOGGER.warning("[CitizenAction] PlayerRef component is null");
                return false;
            }
            Player player = (Player)store.getComponent(playerReference, Player.getComponentType());
            if (player == null) {
                LOGGER.warning("[CitizenAction] Player component is null");
                return false;
            }
            UUIDComponent uuidComponent = (UUIDComponent)store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComponent == null) {
                LOGGER.warning("[CitizenAction] UUIDComponent is null");
                return false;
            }
            UUID npcUuid = uuidComponent.getUuid();
            if (CoreAPI.isDebug()) {
                LOGGER.info("[CitizenAction] NPC UUID: " + String.valueOf(npcUuid));
            }
            if ((citizenService = CitizenService.getInstance()) == null) {
                LOGGER.warning("[CitizenAction] CitizenService is null");
                return false;
            }
            CitizenData citizen = citizenService.getCitizenByUUID(npcUuid);
            if (citizen == null) {
                LOGGER.warning("[CitizenAction] No citizen found for UUID: " + String.valueOf(npcUuid));
                return false;
            }
            if (CoreAPI.isDebug()) {
                LOGGER.info("[CitizenAction] F-Key interaction: " + citizen.id + " by " + playerRef.getUsername());
            }
            citizenService.dispatchInteract(player, citizen.id);
            return true;
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[CitizenAction] Error during citizen interaction", e);
            return false;
        }
    }
}

