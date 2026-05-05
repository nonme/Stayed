package dev.hearthbound.npc;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.UUID;

/**
 * HyCitizens-style live NPC lookup. UUID refs can be stale or temporarily
 * unavailable around chunk load/restart boundaries, so restoration code must
 * also scan loaded NPC entities by the durable HB_NPCID backlink.
 */
public final class NpcLiveEntityResolver {

    private NpcLiveEntityResolver() {}

    @SuppressWarnings("unchecked")
    public static Ref<EntityStore> findLiveNpcByRecord(Store<EntityStore> store,
                                                       NpcRegistry.NpcRecord record) {
        if (store == null || record == null) return null;
        String npcId = record.npcId;
        UUID entityUuid = record.entityUuid;
        Ref<EntityStore>[] found = new Ref[]{null};
        Archetype<EntityStore> query = Archetype.of(NPCEntity.getComponentType());
        store.forEachChunk(query, (chunk, buf) -> {
            if (found[0] != null) return;
            for (int i = 0; i < chunk.size(); i++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(i);
                StayedNpcIdentityComponent id = store.getComponent(
                        ref, StayedNpcIdentityComponent.getComponentType());
                if (id != null && npcId != null && npcId.equals(id.getNpcId())) {
                    found[0] = ref;
                    return;
                }
                if (entityUuid != null) {
                    UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
                    if (uuidComponent != null && entityUuid.equals(uuidComponent.getUuid())) {
                        found[0] = ref;
                        return;
                    }
                }
            }
        });
        return found[0];
    }
}
