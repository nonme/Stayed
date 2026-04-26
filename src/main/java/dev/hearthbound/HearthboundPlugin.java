package dev.hearthbound;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;
import dev.hearthbound.building.BuildingSystem;
import dev.hearthbound.commands.HearthboundCommand;
import dev.hearthbound.events.BlockBreakHandler;
import dev.hearthbound.events.BlockPlaceHandler;
import dev.hearthbound.events.BrazierHandler;
import dev.hearthbound.events.FoundingStoneHandler;
import dev.hearthbound.events.NpcChunkLoadHandler;
import dev.hearthbound.events.PlayerJoinHandler;
import dev.hearthbound.events.VillageTickHandler;
import dev.hearthbound.ui.ElfDialogPage;
import dev.hearthbound.ui.RescueDialogPage;
import dev.hearthbound.ui.VillagerDialogPage;
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

        // Load persisted NPC records from disk and populate NpcRegistry.
        // Must happen before any player joins so ChunkPreLoadProcessEvent
        // can find records for NPC chunks that load near the spawn.
        dev.hearthbound.npc.HearthboundDataStore.get().loadAndPopulateRegistry();

        // Register ECS components
        VillageData.register(this.getEntityStoreRegistry());
        VillagerData.register(this.getEntityStoreRegistry());
        dev.hearthbound.npc.ElfNpcComponent.register(this.getEntityStoreRegistry());

        // Register ECS event systems
        this.getEntityStoreRegistry().registerSystem(new BlockPlaceHandler());
        this.getEntityStoreRegistry().registerSystem(new BlockBreakHandler());
        this.getEntityStoreRegistry().registerSystem(new FoundingStoneHandler());
        this.getEntityStoreRegistry().registerSystem(new BrazierHandler());

        // Register NPC interaction → Elf dialog page
        OpenCustomUIInteraction.registerCustomPageSupplier(
                this, HearthboundPlugin.class, "hearthbound_dialog",
                (ref, accessor, playerRef, context) ->
                        new ElfDialogPage(playerRef)
        );

        // Register rescue victim interaction → Rescue dialog page
        // context.getTargetEntity() is the NPC the player pressed F on; ref is the player entity.
        OpenCustomUIInteraction.registerCustomPageSupplier(
                this, HearthboundPlugin.class, "hearthbound_rescue_dialog",
                (ref, accessor, playerRef, context) -> {
                    var npcRef = context.getTargetEntity();
                    if (npcRef == null) return null;
                    return new RescueDialogPage(playerRef, npcRef);
                }
        );

        // Register villager info dialog
        OpenCustomUIInteraction.registerCustomPageSupplier(
                this, HearthboundPlugin.class, "hearthbound_villager_dialog",
                (ref, accessor, playerRef, context) -> {
                    var npcRef = context.getTargetEntity();
                    if (npcRef == null) return null;
                    return new VillagerDialogPage(playerRef, npcRef);
                }
        );

        // Register events
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, PlayerJoinHandler::onPlayerReady);
        this.getEventRegistry().registerGlobal(ChunkPreLoadProcessEvent.class, NpcChunkLoadHandler::onChunkLoad);

        // Register commands
        this.getCommandRegistry().registerCommand(new HearthboundCommand());
    }

    @Override
    protected void shutdown() {
        // Save any in-flight NPC state and cancel all scheduled tasks.
        dev.hearthbound.npc.HearthboundDataStore.get().save();
        dev.hearthbound.npc.NpcRegistry.get().stopReconcileTask();
        dev.hearthbound.util.TickScheduler.shutdown();
        LOGGER.info("HearthboundPlugin shutdown complete");
    }
}
