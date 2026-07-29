/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Archetype
 *  com.hypixel.hytale.component.ComponentType
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.RemoveReason
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.server.core.entity.UUIDComponent
 *  com.hypixel.hytale.server.core.entity.entities.ProjectileComponent
 *  com.hypixel.hytale.server.core.entity.nameplate.Nameplate
 *  com.hypixel.hytale.server.core.universe.world.World
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.universe.world.World;
import com.kyuubisoft.core.citizen.CitizenData;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Logger;

public class CitizenHologramManager {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft CitizenHolograms");

    public void setNametag(CitizenData citizen, World world) {
        if (citizen.entityRef == null || !citizen.entityRef.isValid()) {
            return;
        }
        if (citizen.hideNametag) {
            this.clearNametag(citizen, world);
            return;
        }
        String displayName = citizen.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            return;
        }
        if (citizen.nametagFormat != null && !citizen.nametagFormat.isEmpty()) {
            displayName = citizen.nametagFormat.replace("{name}", displayName);
        }
        try {
            Store store = world.getEntityStore().getStore();
            Nameplate nameplate = (Nameplate)store.ensureAndGetComponent(citizen.entityRef, Nameplate.getComponentType());
            nameplate.setText(displayName);
        }
        catch (Exception e) {
            LOGGER.warning("Failed to set nametag for " + citizen.id + ": " + e.getMessage());
        }
    }

    public void clearNametag(CitizenData citizen, World world) {
        if (citizen.entityRef == null || !citizen.entityRef.isValid()) {
            return;
        }
        try {
            Store store = world.getEntityStore().getStore();
            Nameplate nameplate = (Nameplate)store.getComponent(citizen.entityRef, Nameplate.getComponentType());
            if (nameplate != null) {
                nameplate.setText("");
            }
        }
        catch (Exception e) {
            LOGGER.fine("Failed to clear nametag for " + citizen.id + ": " + e.getMessage());
        }
    }

    public int cleanupLegacyHolograms(World world) {
        Store store = world.getEntityStore().getStore();
        Archetype query = Archetype.of((ComponentType[])new ComponentType[]{ProjectileComponent.getComponentType(), Nameplate.getComponentType()});
        ArrayList legacyUUIDs = new ArrayList();
        try {
            store.forEachChunk((Query)query, (chunk, commandBuffer) -> {
                for (int i = 0; i < chunk.size(); ++i) {
                    try {
                        UUIDComponent uuidComp;
                        String assetName;
                        ProjectileComponent proj = (ProjectileComponent)chunk.getComponent(i, ProjectileComponent.getComponentType());
                        if (proj == null || !"KS_Hologram".equals(assetName = proj.getProjectileAssetName()) && !"Projectile".equals(assetName) || (uuidComp = (UUIDComponent)chunk.getComponent(i, UUIDComponent.getComponentType())) == null || uuidComp.getUuid() == null) continue;
                        legacyUUIDs.add(uuidComp.getUuid());
                        continue;
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
            });
        }
        catch (Exception e) {
            LOGGER.warning("Failed to scan for legacy holograms: " + e.getMessage());
            return 0;
        }
        int removed = 0;
        for (UUID uuid : legacyUUIDs) {
            try {
                Ref ref = world.getEntityRef(uuid);
                if (ref == null || !ref.isValid()) continue;
                store.removeEntity(ref, RemoveReason.REMOVE);
                ++removed;
            }
            catch (Exception e) {
                LOGGER.fine("Failed to remove legacy hologram: " + e.getMessage());
            }
        }
        return removed;
    }
}

