package dev.hearthbound.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import it.unimi.dsi.fastutil.Pair;

import java.util.logging.Logger;

public class NpcManager {

    private static final Logger LOGGER = Logger.getLogger(NpcManager.class.getName());
    private static final String ROOT_INTERACTION = "Hearthbound";

    public static Pair<Ref<EntityStore>, INonPlayerCharacter> spawnNpc(
            Store<EntityStore> store, Vector3d position, Vector3f rotation, String roleName) {
        Pair<Ref<EntityStore>, INonPlayerCharacter> result =
                NPCPlugin.get().spawnNPC(store, roleName, null, position, rotation);

        if (result != null) {
            assignInteraction(store, result.first());
            fixPersistentModelScale(store, result.first());
        }

        return result;
    }

    /** Same as {@link #spawnNpc} but does not attach the Hearthbound RootInteraction.
     *  Use for NPCs whose interaction is handled entirely by their role's JSON
     *  InteractionInstruction — e.g. rescue victims that complete an objective task
     *  on F, or hostile mobs that shouldn't be talked to at all. */
    public static Pair<Ref<EntityStore>, INonPlayerCharacter> spawnNpcNoInteraction(
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
    private static void fixPersistentModelScale(Store<EntityStore> store, Ref<EntityStore> npcRef) {
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

    /** Spawns a rescue victim NPC: no pre-attached interaction, the caller is
     *  expected to call {@link #assignRescueInteraction} after placing VillagerData
     *  and applying the appearance. Keeping the two steps separate avoids the F-key
     *  hint appearing before the victim looks like a villager. */
    public static Pair<Ref<EntityStore>, INonPlayerCharacter> spawnRescueVictim(
            Store<EntityStore> store, Vector3d position, Vector3f rotation, String roleName) {
        Pair<Ref<EntityStore>, INonPlayerCharacter> result =
                NPCPlugin.get().spawnNPC(store, roleName, null, position, rotation);
        if (result != null) {
            fixPersistentModelScale(store, result.first());
        }
        return result;
    }

    /**
     * Assigns the HearthboundRescue RootInteraction to a rescue victim NPC so that
     * pressing F opens the rescue dialog UI (not the elf dialog).
     */
    public static void assignRescueInteraction(Store<EntityStore> store, Ref<EntityStore> npcRef) {
        Interactable interactable = store.getComponent(npcRef, Interactable.getComponentType());
        if (interactable == null) {
            store.putComponent(npcRef, Interactable.getComponentType(), Interactable.INSTANCE);
        }
        Interactions interactions = store.getComponent(npcRef, Interactions.getComponentType());
        if (interactions == null) {
            interactions = new Interactions();
        }
        interactions.setInteractionId(InteractionType.Use, "HearthboundRescue");
        interactions.setInteractionHint("server.interactionHints.talk");
        store.putComponent(npcRef, Interactions.getComponentType(), interactions);
        LOGGER.fine("Assigned HearthboundRescue interaction to entity " + npcRef);
    }

    /**
     * Assigns the Hearthbound RootInteraction to an NPC entity so that
     * pressing F triggers the OpenCustomUI interaction.
     */
    public static void assignInteraction(Store<EntityStore> store, Ref<EntityStore> npcRef) {
        // Ensure Interactable marker component exists (required for F-key hint to show)
        Interactable interactable = store.getComponent(npcRef, Interactable.getComponentType());
        if (interactable == null) {
            store.putComponent(npcRef, Interactable.getComponentType(), Interactable.INSTANCE);
        }

        // Set the RootInteraction and hint
        Interactions interactions = store.getComponent(npcRef, Interactions.getComponentType());
        if (interactions == null) {
            interactions = new Interactions();
        }
        interactions.setInteractionId(InteractionType.Use, ROOT_INTERACTION);
        interactions.setInteractionHint("server.interactionHints.talk");
        store.putComponent(npcRef, Interactions.getComponentType(), interactions);
        LOGGER.fine("Assigned RootInteraction '" + ROOT_INTERACTION + "' to entity " + npcRef);
    }
}
