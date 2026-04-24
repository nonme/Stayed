package dev.hearthbound.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ComponentUpdate;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.EntityUpdate;
import com.hypixel.hytale.protocol.ModelTransform;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.TransformUpdate;
import com.hypixel.hytale.protocol.packets.entities.EntityUpdates;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controls NPC builder behavior during construction:
 * - Freeze in place (Frozen component)
 * - Rotate head/body toward current block being placed
 * - Unfreeze when construction ends
 *
 * Rotation is sent as a client packet (TransformUpdate) to the village owner.

 */
public class BuilderBehavior {

    private static final Logger LOGGER = Logger.getLogger(BuilderBehavior.class.getName());

    private final World world;
    private final UUID npcUuid;
    private final UUID ownerUuid;
    private Ref<EntityStore> npcRef;
    private boolean frozen = false;

    /**
     * @param world      the world
     * @param npcUuid    UUID of the builder NPC
     * @param ownerUuid  UUID of the player who owns the village (receives rotation packets)
     */
    public BuilderBehavior(World world, UUID npcUuid, UUID ownerUuid) {
        this.world = world;
        this.npcUuid = npcUuid;
        this.ownerUuid = ownerUuid;
    }

    /**
     * Freeze the NPC in place — stops all JSON behavior tree movement.
     */
    public void freeze() {
        if (frozen) return;
        try {
            npcRef = world.getEntityRef(npcUuid);
            if (npcRef == null || !npcRef.isValid()) return;

            Store<EntityStore> store = npcRef.getStore();
            store.addComponent(npcRef, Frozen.getComponentType(), Frozen.get());

            // Stop movement animation to prevent "running in place"
            // Frozen alone doesn't stop the walk animation — must be stopped explicitly.
            try {
                AnimationUtils.stopAnimation(npcRef, AnimationSlot.Movement, store);
            } catch (Exception animEx) {
                LOGGER.fine("Failed to stop walk animation on freeze: " + animEx.getMessage());
            }

            frozen = true;
            LOGGER.info("Builder NPC frozen for construction");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to freeze builder NPC", e);
        }
    }

    /**
     * Unfreeze the NPC — resumes JSON behavior tree.
     */
    public void unfreeze() {
        if (!frozen) return;
        try {
            if (npcRef != null && npcRef.isValid()) {
                Store<EntityStore> store = npcRef.getStore();
                store.tryRemoveComponent(npcRef, Frozen.getComponentType());
            }
            frozen = false;
            npcRef = null;
            LOGGER.info("Builder NPC unfrozen after construction");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to unfreeze builder NPC", e);
        }
    }

    /**
     * Returns the NPC's current world position, or {@code null} if the entity isn't loaded.
     */
    public Vector3d getPosition() {
        try {
            if (npcRef == null || !npcRef.isValid()) {
                npcRef = world.getEntityRef(npcUuid);
            }
            if (npcRef == null || !npcRef.isValid()) return null;
            TransformComponent transform = npcRef.getStore()
                    .getComponent(npcRef, TransformComponent.getComponentType());
            return transform != null ? transform.getPosition() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Rotate the NPC's head and body to look at the given block position.
     * Sends a TransformUpdate packet to the village owner.
     */
    public void lookAtBlock(int blockX, int blockY, int blockZ) {
        try {
            if (npcRef == null || !npcRef.isValid()) {
                npcRef = world.getEntityRef(npcUuid);
            }
            if (npcRef == null || !npcRef.isValid()) return;

            Store<EntityStore> store = npcRef.getStore();

            TransformComponent transform = store.getComponent(npcRef, TransformComponent.getComponentType());
            if (transform == null) return;
            Vector3d npcPos = transform.getPosition();

            NetworkId networkId = store.getComponent(npcRef, NetworkId.getComponentType());
            if (networkId == null) return;

            // Direction from NPC to block center
            double dx = (blockX + 0.5) - npcPos.x;
            double dy = (blockY + 0.5) - npcPos.y;
            double dz = (blockZ + 0.5) - npcPos.z;

            float yaw = (float)(Math.atan2(dx, dz) + Math.PI);
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            float pitch = (float) Math.atan2(dy, horizontalDist);

            Direction lookDirection = new Direction(yaw, pitch, 0.0f);
            Direction bodyDirection = new Direction(yaw, 0.0f, 0.0f);

            ModelTransform modelTransform = new ModelTransform();
            modelTransform.lookOrientation = lookDirection;
            modelTransform.bodyOrientation = bodyDirection;

            TransformUpdate update = new TransformUpdate(modelTransform);
            EntityUpdate entityUpdate = new EntityUpdate(
                    networkId.getId(), null, new ComponentUpdate[]{update});
            EntityUpdates packet = new EntityUpdates(
                    null, new EntityUpdate[]{entityUpdate});

            // Send to village owner
            PlayerRef playerRef = Universe.get().getPlayer(ownerUuid);
            if (playerRef != null) {
                playerRef.getPacketHandler().write((ToClientPacket) packet);
            }

        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to rotate builder NPC toward block", e);
        }
    }

    /**
     * Move the NPC to the given position (e.g., in front of the building door).
     */
    public void moveTo(double x, double y, double z) {
        try {
            if (npcRef == null || !npcRef.isValid()) {
                npcRef = world.getEntityRef(npcUuid);
            }
            if (npcRef == null || !npcRef.isValid()) return;

            Entity elf = world.getEntity(npcUuid);
            if (elf != null) {
                Store<EntityStore> store = npcRef.getStore();
                elf.moveTo(npcRef, x, y, z, store);
                LOGGER.info("Builder NPC moved to " + x + "," + y + "," + z);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Failed to move builder NPC", e);
        }
    }

    public boolean isFrozen() {
        return frozen;
    }
}
