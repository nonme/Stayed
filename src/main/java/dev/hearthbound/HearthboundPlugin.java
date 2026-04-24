package dev.hearthbound;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.hearthbound.building.BuildingSystem;
import dev.hearthbound.commands.HearthboundCommand;
import dev.hearthbound.events.BlockBreakHandler;
import dev.hearthbound.events.BlockPlaceHandler;
import dev.hearthbound.events.FoundingStoneHandler;
import dev.hearthbound.events.PlayerJoinHandler;
import dev.hearthbound.events.VillageTickHandler;
import dev.hearthbound.ui.ElfDialogPage;
import dev.hearthbound.ui.RescueDialogPage;
import dev.hearthbound.village.VillageData;
import dev.hearthbound.village.VillageManager;
import dev.hearthbound.village.VillagerData;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

public class HearthboundPlugin extends JavaPlugin {

    static final Logger LOGGER = Logger.getLogger(HearthboundPlugin.class.getName());
    private static HearthboundPlugin instance;
    private VillageTickHandler villageTickHandler;

    public static HearthboundPlugin get() {
        return instance;
    }

    public VillageTickHandler getVillageTickHandler() {
        return villageTickHandler;
    }

    public HearthboundPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        instance = this;
        villageTickHandler = new VillageTickHandler();

        // Initialize managers
        VillageManager.init();
        BuildingSystem.init();

        // Register ECS components
        VillageData.register(this.getEntityStoreRegistry());
        VillagerData.register(this.getEntityStoreRegistry());

        // Register ECS event systems
        this.getEntityStoreRegistry().registerSystem(new BlockPlaceHandler());
        this.getEntityStoreRegistry().registerSystem(new BlockBreakHandler());
        this.getEntityStoreRegistry().registerSystem(new FoundingStoneHandler());

        // Register NPC interaction → Elf dialog page
        OpenCustomUIInteraction.registerCustomPageSupplier(
                this, HearthboundPlugin.class, "hearthbound_dialog",
                (ref, accessor, playerRef, context) ->
                        new ElfDialogPage(playerRef)
        );

        // Register rescue victim interaction → Rescue dialog page
        OpenCustomUIInteraction.registerCustomPageSupplier(
                this, HearthboundPlugin.class, "hearthbound_rescue_dialog",
                (ref, accessor, playerRef, context) ->
                        new RescueDialogPage(playerRef)
        );

        // Register events
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, PlayerJoinHandler::onPlayerReady);

        // Register commands
        this.getCommandRegistry().registerCommand(new HearthboundCommand());
    }
}
