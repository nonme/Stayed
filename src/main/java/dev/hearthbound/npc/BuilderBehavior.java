package dev.hearthbound.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.ComponentUpdate;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.EntityUpdate;
import com.hypixel.hytale.protocol.ModelTransform;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.TransformUpdate;
import com.hypixel.hytale.protocol.packets.entities.EntityUpdates;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.util.ChunkUtil;

import java.util.ArrayList;
import java.util.List;
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

            // Stop movement animation to prevent "running in place" — Frozen alone
            // doesn't stop the walk animation, must be stopped explicitly.
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

    // Item animation IDs — match the JSON file names in Server/Item/Animations/NPC/Intelligent/Outlander/
    private static final String ANIM_ITEM_PICKAXE = "Outlander_Pickaxe";
    private static final String ANIM_ITEM_BLOCK   = "Item";

    /** Plays the pickaxe Mine swing on the Action slot. Call before breaking a block. */
    public void playBreakAnimation() {
        withRef(ref -> {
            Store<EntityStore> store = ref.getStore();
            AnimationUtils.playAnimation(ref, AnimationSlot.Action, ANIM_ITEM_PICKAXE, "Mine", false, store);
        });
    }

    /** Plays the generic block-place animation on the Action slot. Call before placing a block. */
    public void playBuildAnimation() {
        withRef(ref -> {
            Store<EntityStore> store = ref.getStore();
            AnimationUtils.playAnimation(ref, AnimationSlot.Action, ANIM_ITEM_BLOCK, "Build", false, store);
        });
    }

    /** Stops the Action animation slot (called after block is placed). */
    public void stopActionAnimation() {
        withRef(ref -> AnimationUtils.stopAnimation(ref, AnimationSlot.Action, ref.getStore()));
    }

    /**
     * Swaps slot 0 to a pickaxe so the Mine animation looks correct.
     * The previous item ID is returned so the caller can restore it.
     */
    public void equipPickaxe() {
        withRef(ref -> setHotbarSlot0(ref, "Tool_Pickaxe_Iron"));
    }

    /** Restores slot 0 to the given block item after the break phase. */
    public void equipBlock(String blockItemId) {
        withRef(ref -> setHotbarSlot0(ref, blockItemId));
    }

    private void setHotbarSlot0(Ref<EntityStore> ref, String itemId) {
        try {
            Entity entity = world.getEntity(npcUuid);
            if (!(entity instanceof LivingEntity living)) return;
            var inv = living.getInventory();
            if (inv == null) return;
            var hotbar = inv.getHotbar();
            ItemStack current = hotbar.getItemStack((short) 0);
            if (current != null && !current.isEmpty()) {
                hotbar.removeItemStackFromSlot((short) 0, current.getQuantity());
            }
            hotbar.addItemStackToSlot((short) 0, new ItemStack(itemId, 1));
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "setHotbarSlot0 failed for " + itemId, e);
        }
    }

    /**
     * Removes Plant_Grass entity-blocks stacked at y+1 and y+2 above the target block.
     * Plant_Grass is a block (not an entity), so we clear it via chunk.setBlock(Empty).
     * settings=10: skip setState + skip setFillerBlocksAt (correct for clearing vegetation).
     */
    public void clearVegetationAbove(int bx, int by, int bz) {
        var assetMap = BlockType.getAssetMap();
        int emptyId = assetMap.getIndex("Empty");
        BlockType emptyType = emptyId != Integer.MIN_VALUE ? assetMap.getAsset(emptyId) : null;
        if (emptyType == null) return;

        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(bx, bz));
        if (chunk == null) return;
        for (int dy = 1; dy <= 3; dy++) {
            int py = by + dy;
            int idx = chunk.getBlock(bx, py, bz);
            BlockType bt = assetMap.getAsset(idx);
            if (bt == null || bt == emptyType) continue;
            String id = bt.getId();
            if (id != null && (id.startsWith("Plant_") || id.contains("_Roots"))) {
                chunk.setBlock(bx, py, bz, emptyId, emptyType, 0, 0, 10);
            }
        }
    }

    /**
     * Removes non-player entities whose block-floor position is (bx, by+1, bz) —
     * i.e. decorative entities (pebbles, props) standing on top of the target block.
     * Uses forEachChunk to iterate all entities with a TransformComponent.
     */
    public void clearEntitiesOnBlock(int bx, int by, int bz) {
        withRef(npcRef -> {
            Store<EntityStore> store = npcRef.getStore();
            List<Ref<EntityStore>> toRemove = new ArrayList<>();

            store.forEachChunk(
                Query.and(TransformComponent.getComponentType()),
                (chunk, commandBuffer) -> {
                    for (int i = 0; i < chunk.size(); i++) {
                        Ref<EntityStore> r = chunk.getReferenceTo(i);
                        if (r == null || !r.isValid()) continue;
                        // Never remove players or the builder itself
                        if (store.getComponent(r, Player.getComponentType()) != null) continue;
                        if (r.equals(npcRef)) continue;

                        TransformComponent tc = chunk.getComponent(i, TransformComponent.getComponentType());
                        if (tc == null) continue;
                        Vector3d p = tc.getPosition();
                        // Entity is "on" the block if its floor coords match (bx, by+1, bz)
                        if ((int) Math.floor(p.x) == bx
                                && (int) Math.floor(p.y) == by + 1
                                && (int) Math.floor(p.z) == bz) {
                            toRemove.add(r);
                        }
                    }
                }
            );

            for (Ref<EntityStore> r : toRemove) {
                if (r.isValid()) {
                    try {
                        store.removeEntity(r, RemoveReason.REMOVE);
                    } catch (Exception e) {
                        LOGGER.log(Level.FINE, "clearEntitiesOnBlock: removeEntity failed", e);
                    }
                }
            }
        });
    }

    /** Returns true if the block at (x,y,z) is non-air and should be broken first. */
    public boolean isBlockPresent(int x, int y, int z) {
        var assetMap = BlockType.getAssetMap();
        try {
            BlockType bt = world.getBlockType(x, y, z);
            if (bt == null) return false;
            String id = bt.getId();
            return id != null && !id.equals("Empty") && !id.equals("Editor_Empty") && !id.equals("Filter_Air_Block");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true when the engine's walk motion controller considers the NPC to be
     * walking. More reliable than position-delta sampling — updated every engine tick,
     * never lags by one tick, and works even if the NPC is sliding on terrain.
     */
    public boolean isWalking() {
        try {
            if (npcRef == null || !npcRef.isValid()) {
                npcRef = world.getEntityRef(npcUuid);
            }
            if (npcRef == null || !npcRef.isValid()) return false;
            MovementStatesComponent mc = npcRef.getStore()
                    .getComponent(npcRef, MovementStatesComponent.getComponentType());
            return mc != null && mc.getMovementStates() != null && mc.getMovementStates().walking;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Updates the NPC's leash point so the Leash sensor in the Builder role keeps him
     * near the current build zone center rather than his spawn position.
     */
    public void setLeashPoint(double x, double y, double z) {
        withRef(ref -> {
            NPCEntity npcEntity = ref.getStore().getComponent(ref, NPCEntity.getComponentType());
            if (npcEntity != null) {
                npcEntity.setLeashPoint(new Vector3d(x, y, z));
            }
        });
    }

    private void withRef(java.util.function.Consumer<Ref<EntityStore>> action) {
        try {
            if (npcRef == null || !npcRef.isValid()) {
                npcRef = world.getEntityRef(npcUuid);
            }
            if (npcRef == null || !npcRef.isValid()) return;
            action.accept(npcRef);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "BuilderBehavior action failed", e);
        }
    }
}
