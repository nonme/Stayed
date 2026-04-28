package dev.hearthbound.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hearthbound.HearthboundPlugin;
import dev.hearthbound.npc.ElfSage;

import java.util.logging.Logger;

public class PlayerJoinHandler {

    private static final Logger LOGGER = Logger.getLogger(PlayerJoinHandler.class.getName());

    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        player.sendMessage(Message.raw("Welcome to Hearthbound, " + player.getDisplayName() + "!"));

        try {
            World world = player.getWorld();
            Store<EntityStore> store = world.getEntityStore().getStore();
            Ref<EntityStore> playerRef = event.getPlayerRef();

            ElfSage.spawnIfNeeded(store, playerRef, world);

            // Start village tick handler (HUD, settler spawning)
            @SuppressWarnings("removal")
            com.hypixel.hytale.server.core.universe.PlayerRef pRef = player.getPlayerRef();

            VillageTickHandler tickHandler = HearthboundPlugin.get().getVillageTickHandler();
            tickHandler.start(store, playerRef, pRef, world);

        } catch (Exception e) {
            LOGGER.warning("Failed to initialize village for player: " + e.getMessage());
        }
    }
}
