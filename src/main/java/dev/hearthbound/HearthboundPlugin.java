package dev.hearthbound;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemDropList;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;
import dev.hearthbound.building.BuildingSystem;
import dev.hearthbound.building.CraftabilityIndex;
import dev.hearthbound.commands.HearthboundCommand;
import dev.hearthbound.events.BlockBreakHandler;
import dev.hearthbound.events.BlockPlaceHandler;
import dev.hearthbound.events.BrazierHandler;
import dev.hearthbound.events.FoundingStoneHandler;
import dev.hearthbound.events.CounterHandler;
import dev.hearthbound.events.ForgeHandler;
import dev.hearthbound.events.TavernHandler;
import dev.hearthbound.events.InteractionDebugHandler;
import dev.hearthbound.events.SawmillHandler;
import dev.hearthbound.events.WoodcuttersHandler;

import dev.hearthbound.events.MineTorchHandler;
import dev.hearthbound.events.ScarecrowHandler;
import dev.hearthbound.events.TargetDummyHandler;
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

public class HearthboundPlugin extends JavaPlugin {

    private static final dev.hearthbound.util.log.Log LOG =
            dev.hearthbound.util.log.Log.get("plugin");
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
        // Logger first — everything else logs through it.
        dev.hearthbound.util.log.Log.install();
        villageTickHandler = new VillageTickHandler();

        // Initialize managers
        VillageManager.init();
        BuildingSystem.init();

        dev.hearthbound.npc.StayedRoleAssetPackManager.setup();

        // Load persisted NPC records from disk and populate NpcRegistry.
        // Must happen before any player joins so ChunkPreLoadProcessEvent
        // can find records for NPC chunks that load near the spawn.
        dev.hearthbound.npc.HearthboundDataStore.get().loadAndPopulateRegistry();
        dev.hearthbound.npc.StayedRoleGenerator.get().regenerateAllRoles(
                dev.hearthbound.npc.NpcRegistry.get().allRecords());
        // Periodic flush: NpcPositionTracker marks the store dirty as villagers
        // walk; this drains the flag to disk every 5s so a crash loses at most
        // a few seconds of position drift instead of every move since boot.
        dev.hearthbound.npc.HearthboundDataStore.get().startPeriodicFlush();

        // Register ECS components
        VillageData.register(this.getEntityStoreRegistry());
        VillagerData.register(this.getEntityStoreRegistry());
        // Legacy elf marker — kept registered so existing chunk saves can still
        // be deserialised. New code uses StayedNpcIdentityComponent instead;
        // NpcChunkLoadHandler migrates legacy entities on first load.
        dev.hearthbound.npc.ElfNpcComponent.register(this.getEntityStoreRegistry());
        dev.hearthbound.npc.StayedNpcIdentityComponent.register(this.getEntityStoreRegistry());
        dev.hearthbound.npc.StayedIntegrationTestNpcMarkerComponent.register(
                this.getEntityStoreRegistry());

        // Register ECS event systems
        this.getEntityStoreRegistry().registerSystem(new BlockPlaceHandler());
        this.getEntityStoreRegistry().registerSystem(new BlockBreakHandler());
        this.getEntityStoreRegistry().registerSystem(new FoundingStoneHandler());
        this.getEntityStoreRegistry().registerSystem(new BrazierHandler());
        this.getEntityStoreRegistry().registerSystem(new ScarecrowHandler());
        this.getEntityStoreRegistry().registerSystem(new CounterHandler());
        this.getEntityStoreRegistry().registerSystem(new WoodcuttersHandler());
        this.getEntityStoreRegistry().registerSystem(new SawmillHandler());
        this.getEntityStoreRegistry().registerSystem(new MineTorchHandler());
        this.getEntityStoreRegistry().registerSystem(new ForgeHandler());
        this.getEntityStoreRegistry().registerSystem(new TavernHandler());
        this.getEntityStoreRegistry().registerSystem(new TargetDummyHandler());
        // Engine-level guard against duplicate NPC entities sharing one npcId.
        this.getEntityStoreRegistry().registerSystem(new dev.hearthbound.npc.DuplicateNpcPrevention());

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

        // Founder's Almanac — opened via the Stayed_Founders_Almanac inventory item
        // (right-click → Stayed_OpenAlmanac interaction → this page id).
        OpenCustomUIInteraction.registerCustomPageSupplier(
                this, HearthboundPlugin.class, "hearthbound_almanac",
                (ref, accessor, playerRef, context) ->
                        new dev.hearthbound.ui.AlmanacPage(playerRef, ref)
        );

        // Register events
        this.getEventRegistry().register(LoadedAssetsEvent.class, Item.class, CraftabilityIndex::onItemsLoaded);
        this.getEventRegistry().register(LoadedAssetsEvent.class, ItemDropList.class, CraftabilityIndex::onDropListsLoaded);
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, PlayerJoinHandler::onPlayerReady);
        this.getEventRegistry().registerGlobal(PlayerInteractEvent.class, InteractionDebugHandler::onPlayerInteract);
        this.getEventRegistry().registerGlobal(PlayerMouseButtonEvent.class, InteractionDebugHandler::onPlayerMouseButton);
        InteractionDebugHandler.registerPacketWatcher();
        // EventPriority.LAST: NPC plugin runs first and finishes deserialising
        // identity components (e.g. StayedNpcIdentityComponent) before our
        // handler walks the holders. Without this, Holder.getComponent returned
        // null for every NPC at chunk-load time and we couldn't match entities
        // to registry records.
        this.getEventRegistry().registerGlobal(EventPriority.LAST,
                ChunkPreLoadProcessEvent.class, NpcChunkLoadHandler::onChunkLoad);

        // Register commands
        this.getCommandRegistry().registerCommand(new HearthboundCommand());
    }

    /**
     * Starts the periodic NpcRegistry self-check once we have a world handle.
     * Called from PlayerJoinHandler on the first PlayerReady event (the plugin
     * does not get a world until at least one player joins).
     */
    public static void startSelfCheckIfNeeded(
            com.hypixel.hytale.server.core.universe.world.World world) {
        if (world == null) return;
        dev.hearthbound.test.audit.NpcRegistrySelfCheck.start(world);
    }

    @Override
    protected void shutdown() {
        // Save any in-flight NPC state and cancel all scheduled tasks.
        InteractionDebugHandler.unregisterPacketWatcher();
        dev.hearthbound.test.audit.NpcRegistrySelfCheck.stop();
        dev.hearthbound.npc.NpcPositionTracker.stop();
        dev.hearthbound.npc.HearthboundDataStore.get().stopPeriodicFlush();
        dev.hearthbound.npc.HearthboundDataStore.get().save();
        dev.hearthbound.npc.NpcRegistry.get().stopReconcileTask();
        dev.hearthbound.util.TickScheduler.shutdown();
        LOG.info("HearthboundPlugin shutdown complete");
        dev.hearthbound.util.log.Log.shutdown();
    }
}
