/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Archetype
 *  com.hypixel.hytale.component.ArchetypeChunk
 *  com.hypixel.hytale.component.CommandBuffer
 *  com.hypixel.hytale.component.ComponentType
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.server.core.asset.type.model.config.Model
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.modules.entity.component.ModelComponent
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage$EntitySource
 *  com.hypixel.hytale.server.core.modules.entity.damage.Damage$Source
 *  com.hypixel.hytale.server.core.modules.entity.damage.DamageCause
 *  com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  com.hypixel.hytale.server.npc.entities.NPCEntity
 *  javax.annotation.Nonnull
 */
package com.kyuubisoft.core.tracking.systems;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.kyuubisoft.core.tracking.TrackingService;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class DamageTrackerSystem
extends DamageEventSystem {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core Tracking");
    @Nonnull
    private final ComponentType<EntityStore, Player> playerComponentType = Player.getComponentType();

    @Nonnull
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage event) {
        try {
            Player attackerPlayer;
            Damage.EntitySource entitySource;
            Ref attackerRef;
            Damage.Source source;
            float damageAmount = event.getAmount();
            if (damageAmount <= 0.0f) {
                return;
            }
            int damage = (int)Math.ceil(damageAmount);
            Ref victimRef = chunk.getReferenceTo(index);
            if (victimRef == null || !victimRef.isValid()) {
                return;
            }
            TrackingService service = TrackingService.getInstance();
            if (service == null) {
                return;
            }
            Player victimPlayer = (Player)store.getComponent(victimRef, this.playerComponentType);
            if (victimPlayer != null) {
                String causeType = this.getCauseType(event);
                String sourceType = event.getSource() instanceof Damage.EntitySource ? "entity" : "environment";
                service.dispatchDamageTaken(victimPlayer, damage, causeType, sourceType);
            }
            if ((source = event.getSource()) instanceof Damage.EntitySource && (attackerRef = (entitySource = (Damage.EntitySource)source).getRef()) != null && attackerRef.isValid() && (attackerPlayer = (Player)store.getComponent(attackerRef, this.playerComponentType)) != null) {
                String victimType = this.getEntityType((Ref<EntityStore>)victimRef, store, victimPlayer != null);
                service.dispatchDamageDealt(attackerPlayer, damage, victimType);
            }
        }
        catch (Exception e) {
            LOGGER.fine("DamageTrackerSystem error: " + e.getMessage());
        }
    }

    private String getCauseType(Damage event) {
        try {
            String causeId;
            DamageCause cause = event.getCause();
            if (cause != null && (causeId = cause.getId()) != null && !causeId.isEmpty()) {
                return causeId;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return "unknown";
    }

    private String getEntityType(Ref<EntityStore> ref, Store<EntityStore> store, boolean isPlayer) {
        if (isPlayer) {
            return "player";
        }
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

