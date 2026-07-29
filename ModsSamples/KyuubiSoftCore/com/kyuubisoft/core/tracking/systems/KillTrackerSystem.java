/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Archetype
 *  com.hypixel.hytale.component.CommandBuffer
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.server.core.asset.type.model.config.Model
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.modules.entity.component.ModelComponent
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage$EntitySource
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage$Source
 *  com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent
 *  com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems$OnDeathSystem
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  com.hypixel.hytale.server.npc.entities.NPCEntity
 *  javax.annotation.Nonnull
 */
package com.kyuubisoft.core.tracking.systems;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.kyuubisoft.core.tracking.TrackingService;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class KillTrackerSystem
extends DeathSystems.OnDeathSystem {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core Tracking");

    @Nonnull
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    public void onComponentAdded(@Nonnull Ref<EntityStore> victimRef, @Nonnull DeathComponent component, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        try {
            Damage deathInfo = component.getDeathInfo();
            if (deathInfo == null) {
                return;
            }
            Damage.Source source = deathInfo.getSource();
            if (!(source instanceof Damage.EntitySource)) {
                return;
            }
            Damage.EntitySource entitySource = (Damage.EntitySource)source;
            Ref killerRef = entitySource.getRef();
            if (!killerRef.isValid()) {
                return;
            }
            Player killerPlayer = (Player)store.getComponent(killerRef, Player.getComponentType());
            if (killerPlayer == null) {
                return;
            }
            PlayerRef playerRef = (PlayerRef)store.getComponent(killerRef, PlayerRef.getComponentType());
            if (playerRef == null) {
                return;
            }
            Player victimPlayer = (Player)store.getComponent(victimRef, Player.getComponentType());
            boolean isPlayerKill = victimPlayer != null;
            String victimType = isPlayerKill ? "player" : this.getEntityType(victimRef, store);
            TrackingService service = TrackingService.getInstance();
            if (service != null) {
                service.dispatchKill(killerPlayer, victimType, isPlayerKill);
            }
        }
        catch (Exception e) {
            LOGGER.fine("KillTrackerSystem error: " + e.getMessage());
        }
    }

    private String getEntityType(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            String modelId;
            Model model;
            String npcTypeId;
            NPCEntity npcEntity = (NPCEntity)store.getComponent(ref, NPCEntity.getComponentType());
            if (npcEntity != null && (npcTypeId = npcEntity.getNPCTypeId()) != null && !npcTypeId.isEmpty()) {
                return npcTypeId;
            }
            ModelComponent modelComponent = (ModelComponent)store.getComponent(ref, ModelComponent.getComponentType());
            if (modelComponent != null && (model = modelComponent.getModel()) != null && (modelId = model.getModelAssetId()) != null && !modelId.isEmpty()) {
                return modelId;
            }
        }
        catch (Exception e) {
            LOGGER.fine("getEntityType error: " + e.getMessage());
        }
        return "unknown";
    }
}

