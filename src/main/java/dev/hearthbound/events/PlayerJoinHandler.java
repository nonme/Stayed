package dev.hearthbound.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.HearthboundPlugin;
import dev.hearthbound.building.BuildingSystem;
import dev.hearthbound.npc.ElfSage;

import java.util.logging.Logger;

public class PlayerJoinHandler {

    private static final Logger LOGGER = Logger.getLogger(PlayerJoinHandler.class.getName());

    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        try {
            World world = player.getWorld();
            Store<EntityStore> store = world.getEntityStore().getStore();
            Ref<EntityStore> playerRef = event.getPlayerRef();

            ElfSage.spawnIfNeeded(store, playerRef, world);

            // Resume construction that was interrupted by a server restart.
            Player playerObj = event.getPlayer();
            BuildingSystem.get().resumeConstructionIfNeeded(
                    store, playerRef, world, playerObj.getUuid());

            // Start village tick handler (HUD, settler spawning)
            @SuppressWarnings("removal")
            com.hypixel.hytale.server.core.universe.PlayerRef pRef = player.getPlayerRef();

            VillageTickHandler tickHandler = HearthboundPlugin.get().getVillageTickHandler();
            tickHandler.start(store, playerRef, pRef, world);

            // Sync live NPC positions every few seconds — keeps registry chunkIndex
            // and lastX/Y/Z accurate so we can recover NPCs that wander out of
            // their original chunk before being saved.
            dev.hearthbound.npc.NpcPositionTracker.start(world);

            // First player ready means we have a world handle — start the
            // periodic NpcRegistry self-check (idempotent, safe to call again).
            HearthboundPlugin.startSelfCheckIfNeeded(world);

        } catch (Exception e) {
            LOGGER.warning("Failed to initialize village for player: " + e.getMessage());
        }
    }
}
