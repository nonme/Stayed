package dev.hearthbound.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.HearthboundPlugin;
import dev.hearthbound.building.BuildingSystem;
import dev.hearthbound.npc.ElfSage;

public class PlayerJoinHandler {

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("event.player");
    private static final String LEGACY_LUMBERMILL = "Stayed_Lumbermill";
    private static final String WOODCUTTERS_HUT_BLOCK  = "Stayed_Woodcutters_Hut";

    /**
     * Replaces any Stayed_Lumbermill stacks in the player's inventory with
     * Stayed_Woodcutters_Hut (one-time item rename that shipped with the building rework).
     * Safe to call on every join — exits immediately when nothing to replace.
     */
    private static void migrateLegacyItems(Player player) {
        try {
            var inv = player.getInventory().getCombinedHotbarFirst();
            int count = inv.countItemStacks(s -> LEGACY_LUMBERMILL.equals(s.getItemId()));
            if (count == 0) return;
            var removeTx = inv.removeItemStack(new ItemStack(LEGACY_LUMBERMILL, count), true, true);
            if (!removeTx.succeeded()) return;
            inv.addItemStack(new ItemStack(WOODCUTTERS_HUT_BLOCK, count));
            LOG.info("Migrated " + count + " Stayed_Lumbermill → Stayed_Woodcutters_Hut for player "
                    + player.getUuid());
        } catch (Exception e) {
            LOG.warn("migrateLegacyItems failed", e);
        }
    }

    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        try {
            World world = player.getWorld();
            Store<EntityStore> store = world.getEntityStore().getStore();
            Ref<EntityStore> playerRef = event.getPlayerRef();

            LOG.info("PlayerReady: player=" + player.getUuid()
                    + " refValid=" + (playerRef != null && playerRef.isValid())
                    + " world=" + (world != null ? world.getName() : "null"));

            migrateLegacyItems(player);

            ElfSage.spawnIfNeeded(store, playerRef, world);

            // Resume construction that was interrupted by a server restart.
            Player playerObj = event.getPlayer();
            BuildingSystem.get().resumeConstructionIfNeeded(
                    store, playerRef, world, playerObj.getUuid());

            // Start village tick handler (HUD, settler spawning)
            @SuppressWarnings("removal")
            com.hypixel.hytale.server.core.universe.PlayerRef pRef = player.getPlayerRef();

            VillageTickHandler tickHandler = HearthboundPlugin.get().getVillageTickHandler();
            tickHandler.runCatchUp(store, playerRef, world);
            tickHandler.start(store, playerRef, pRef, world);

            // Sync live NPC positions aggressively — keeps registry chunkIndex
            // and lastX/Y/Z accurate so we can recover NPCs that wander out of
            // their original chunk before being saved.
            dev.hearthbound.npc.NpcPositionTracker.start(world);

            // First player ready means we have a world handle — start the
            // periodic NpcRegistry self-check (idempotent, safe to call again).
            HearthboundPlugin.startSelfCheckIfNeeded(world);
            LOG.info("PlayerReady: Hearthbound initialization complete for " + player.getUuid());

        } catch (Exception e) {
            LOG.warn("Failed to initialize village for player", e);
        }
    }
}
