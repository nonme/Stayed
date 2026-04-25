package dev.hearthbound.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import it.unimi.dsi.fastutil.Pair;

import java.util.UUID;
import java.util.logging.Logger;

public class NpcManager {

    private static final Logger LOGGER = Logger.getLogger(NpcManager.class.getName());

    /**
     * Spawns an NPC and fixes the PersistentModel scale=0 engine bug.
     * Does NOT register in NpcRegistry — caller is responsible because only the
     * caller knows the skin seed, interaction type, and other restore data.
     */
    public static Pair<Ref<EntityStore>, INonPlayerCharacter> spawnNpc(
            Store<EntityStore> store, Vector3d position, Vector3f rotation, String roleName) {
        Pair<Ref<EntityStore>, INonPlayerCharacter> result =
                NPCPlugin.get().spawnNPC(store, roleName, null, position, rotation);

        if (result != null) {
            fixPersistentModelScale(store, result.first());
        }

        return result;
    }

    /**
     * Fixes PersistentModel scale=0 bug in the engine: after spawnNPC() the engine
     * sometimes writes scale=0 into PersistentModel, which causes "Scale must be > 0"
     * crash on next chunk load, making the NPC non-persistent.
     */
    public static void fixPersistentModelScale(Store<EntityStore> store, Ref<EntityStore> npcRef) {
        try {
            PersistentModel pm = store.getComponent(npcRef, PersistentModel.getComponentType());
            if (pm == null) return;
            Model.ModelReference ref = pm.getModelReference();
            if (ref == null) return;
            if (ref.getScale() <= 0f) {
                pm.setModelReference(new Model.ModelReference(
                        ref.getModelAssetId(), 1.0f,
                        ref.getRandomAttachmentIds(), ref.isStaticModel()));
                LOGGER.fine("Fixed PersistentModel scale=0 for NPC " + npcRef);
            }
        } catch (Exception e) {
            LOGGER.warning("fixPersistentModelScale failed: " + e.getMessage());
        }
    }

    /**
     * Extracts the UUID from a freshly spawned NPC entity.
     */
    public static UUID extractUuid(Store<EntityStore> store, Ref<EntityStore> npcRef) {
        UUIDComponent uc = store.getComponent(npcRef, UUIDComponent.getComponentType());
        return uc != null ? uc.getUuid() : null;
    }

    /**
     * Computes the chunk index for a world position.
     */
    public static long chunkIndexFor(Vector3d position) {
        return ChunkUtil.indexChunkFromBlock((int) position.getX(), (int) position.getZ());
    }
}
