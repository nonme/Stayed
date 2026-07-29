package dev.hearthbound.npc;

import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.UUID;
/**
 * Small helpers for swapping items in an NPC's hotbar slot 0 (the visible held item).
 * Pulled out of BuilderBehavior so the same logic can drive the farmer.
 */
public final class HotbarUtil {

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("npc.hotbar");
    private HotbarUtil() {}

    /**
     * Replaces whatever sits in hotbar slot 0 with a single unit of {@code itemId}.
     * No-op (silent) if the entity isn't a LivingEntity, has no inventory, or the swap fails.
     */
    public static void setSlot0(World world, UUID npcUuid, String itemId) {
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
            LOG.debug("HotbarUtil.setSlot0 failed for " + itemId, e);
        }
    }

    public static void clearSlot0(World world, UUID npcUuid) {
        try {
            Entity entity = world.getEntity(npcUuid);
            if (!(entity instanceof LivingEntity living)) return;
            var inv = living.getInventory();
            if (inv == null) return;
            inv.getHotbar().setItemStackForSlot((short) 0, null);
        } catch (Exception e) {
            LOG.debug("HotbarUtil.clearSlot0 failed", e);
        }
    }

    /**
     * Returns the item id currently in slot 0 of the NPC's hotbar, or {@code null} if the
     * entity isn't a LivingEntity, has no inventory, or the slot is empty.
     */
    public static String readSlot0(World world, UUID npcUuid) {
        try {
            Entity entity = world.getEntity(npcUuid);
            if (!(entity instanceof LivingEntity living)) return null;
            var inv = living.getInventory();
            if (inv == null) return null;
            ItemStack stack = inv.getHotbar().getItemStack((short) 0);
            if (stack == null || stack.isEmpty()) return null;
            return stack.getItemId();
        } catch (Exception e) {
            LOG.debug("HotbarUtil.readSlot0 failed", e);
            return null;
        }
    }
}
