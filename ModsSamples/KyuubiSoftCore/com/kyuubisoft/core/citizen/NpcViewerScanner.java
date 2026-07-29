/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Archetype
 *  com.hypixel.hytale.component.ComponentType
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.component.query.Query
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.server.core.entity.UUIDComponent
 *  com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.npc.entities.NPCEntity
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.kyuubisoft.core.citizen.CitizenData;
import com.kyuubisoft.core.citizen.CitizenService;
import com.kyuubisoft.core.citizen.NpcViewerEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class NpcViewerScanner {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft NPC Viewer");
    private static final String[] CITIZEN_ROLE_PREFIXES = new String[]{"KS_NPC_", "KS_Path_", "Citizen_", "Empty_Role"};

    public static List<NpcViewerEntry> scan(World world, CitizenService citizenService) {
        ArrayList<NpcViewerEntry> results = new ArrayList<NpcViewerEntry>();
        try {
            Store store = world.getEntityStore().getStore();
            Archetype query = Archetype.of((ComponentType)NPCEntity.getComponentType());
            store.forEachChunk((Query)query, (chunk, commandBuffer) -> {
                for (int i = 0; i < chunk.size(); ++i) {
                    try {
                        NpcViewerEntry.NpcCategory category;
                        UUID uuid;
                        NPCEntity npc = (NPCEntity)chunk.getComponent(i, NPCEntity.getComponentType());
                        if (npc == null) continue;
                        UUIDComponent uuidComp = (UUIDComponent)chunk.getComponent(i, UUIDComponent.getComponentType());
                        UUID uUID = uuid = uuidComp != null ? uuidComp.getUuid() : null;
                        if (uuid == null) continue;
                        TransformComponent transform = (TransformComponent)chunk.getComponent(i, TransformComponent.getComponentType());
                        double px = 0.0;
                        double py = 0.0;
                        double pz = 0.0;
                        if (transform != null && transform.getPosition() != null) {
                            Vector3d pos = transform.getPosition();
                            px = pos.x;
                            py = pos.y;
                            pz = pos.z;
                        }
                        String npcTypeId = npc.getNPCTypeId();
                        String roleName = npc.getRoleName();
                        CitizenData citizen = citizenService.getCitizenByUUID(uuid);
                        String citizenId = null;
                        String citizenName = null;
                        if (citizen != null) {
                            category = NpcViewerEntry.NpcCategory.REGISTERED;
                            citizenId = citizen.id;
                            citizenName = citizen.name;
                        } else {
                            category = NpcViewerScanner.isCitizenRole(roleName) ? NpcViewerEntry.NpcCategory.ORPHANED : NpcViewerEntry.NpcCategory.WORLD;
                        }
                        results.add(new NpcViewerEntry(uuid, npcTypeId, roleName, px, py, pz, citizenId, citizenName, category));
                        continue;
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
            });
        }
        catch (Exception e2) {
            LOGGER.warning("NPC scan failed: " + e2.getMessage());
        }
        results.sort(Comparator.comparingInt(e -> e.category().ordinal()).thenComparing(NpcViewerEntry::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return results;
    }

    public static boolean isCitizenRole(String roleName) {
        if (roleName == null) {
            return false;
        }
        for (String prefix : CITIZEN_ROLE_PREFIXES) {
            if (!roleName.startsWith(prefix) && !roleName.equals(prefix)) continue;
            return true;
        }
        return false;
    }
}

