/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.SystemGroup
 *  com.hypixel.hytale.component.system.ISystem
 *  com.hypixel.hytale.logger.HytaleLogger
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.command.system.AbstractCommand
 *  com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent
 *  com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction
 *  com.hypixel.hytale.server.core.plugin.JavaPlugin
 *  com.hypixel.hytale.server.core.plugin.JavaPluginInit
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.Universe
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.yourname.companion;

import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.yourname.companion.commands.CompanionCommand;
import com.yourname.companion.commands.FhcCommand;
import com.yourname.companion.data.CompanionRecord;
import com.yourname.companion.data.PlayerCompanionData;
import com.yourname.companion.data.ProgressionConfig;
import com.yourname.companion.data.ReviveConfig;
import com.yourname.companion.gui.CompanionPanel;
import com.yourname.companion.storage.CompanionDataStore;
import com.yourname.companion.storage.CompanionManager;
import com.yourname.companion.systems.CompanionDamageFilter;
import com.yourname.companion.systems.CompanionEquipmentManager;
import com.yourname.companion.systems.CompanionItemUseHandler;
import com.yourname.companion.systems.CompanionOpenUiInteraction;
import com.yourname.companion.systems.CompanionSystem;
import com.yourname.companion.systems.InteractionHandler;
import com.yourname.companion.systems.ReviveBenchIntegration;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CompanionPlugin
extends JavaPlugin {
    private static final int CONNECT_RESTORE_MAX_ATTEMPTS = 20;
    private static final long CONNECT_RESTORE_DELAY_MS = 500L;
    private CompanionManager companionManager;
    private CompanionSystem companionSystem;
    private CompanionCommand companionCommand;
    private FhcCommand fhcCommand;
    private InteractionHandler interactionHandler;
    private CompanionItemUseHandler companionItemUseHandler;
    private ReviveBenchIntegration reviveBench;
    private boolean hyuiAvailable;
    private CompanionPanel companionPanel;

    public CompanionPlugin(JavaPluginInit init) {
        super(init);
    }

    protected void setup() {
        this.silencePluginLogging();
        this.getCodecRegistry(Interaction.CODEC).register("CompanionOpenUi", CompanionOpenUiInteraction.class, CompanionOpenUiInteraction.CODEC);
        this.getLogger().at(Level.INFO).log("Registered interaction codec in setup: CompanionOpenUi");
    }

    public void start() {
        this.silencePluginLogging();
        this.getLogger().at(Level.INFO).log("CompanionPlugin started");
        Path dataDirectory = this.getDataDirectory();
        Path dataPath = dataDirectory.resolve("companion-plugin-data.json");
        ProgressionConfig.loadOrCreate(dataDirectory.resolve("progression-config.json"), this.getLogger());
        ReviveConfig.loadOrCreate(dataDirectory.resolve("revive-config.json"), this.getLogger());
        this.companionManager = new CompanionManager(new CompanionDataStore(dataPath));
        this.companionManager.load();
        this.companionSystem = new CompanionSystem(this.companionManager, this.getLogger());
        this.companionSystem.start();
        this.hyuiAvailable = this.isHyUIAvailable();
        if (this.hyuiAvailable) {
            try {
                CompanionEquipmentManager equipmentManager = new CompanionEquipmentManager(this.getLogger());
                this.companionPanel = new CompanionPanel(this.companionManager, this.companionSystem, equipmentManager, this.getLogger());
                this.getLogger().at(Level.INFO).log("HyUI detected - visual companion panel enabled.");
            }
            catch (Throwable t) {
                this.hyuiAvailable = false;
                this.companionPanel = null;
                ((HytaleLogger.Api)this.getLogger().at(Level.WARNING).withCause(t)).log("HyUI detected but visual panel init failed.");
            }
        } else {
            this.companionPanel = null;
            this.getLogger().at(Level.WARNING).log("HyUI not available - visual companion panel disabled.");
        }
        this.companionCommand = new CompanionCommand(this.companionManager, this.companionSystem, this.companionPanel);
        this.getCommandRegistry().registerCommand((AbstractCommand)this.companionCommand);
        this.fhcCommand = new FhcCommand(this.companionCommand);
        this.getCommandRegistry().registerCommand((AbstractCommand)this.fhcCommand);
        CompanionOpenUiInteraction.setOpenUiCallback(playerRef -> this.companionCommand.openUIMenuForPlayer((PlayerRef)playerRef));
        this.companionSystem.setInteractionMenuCallback(playerRef -> {
            try {
                this.companionCommand.openUIMenuForPlayer((PlayerRef)playerRef);
            }
            catch (Throwable t) {
                ((HytaleLogger.Api)this.getLogger().at(Level.WARNING).withCause(t)).log("Failed to open companion UI menu.");
            }
        });
        this.interactionHandler = new InteractionHandler(this.companionManager, this.companionSystem, this.companionCommand, this.getLogger());
        this.interactionHandler.tryRegisterEvents(this);
        this.companionItemUseHandler = new CompanionItemUseHandler(this.companionCommand, this.getLogger());
        this.companionItemUseHandler.registerEvents(this);
        this.reviveBench = new ReviveBenchIntegration(this.companionManager, this.companionSystem, this.getLogger());
        this.reviveBench.registerEvents(this);
        this.companionSystem.setReviveBenchIntegration(this.reviveBench);
        try {
            SystemGroup dmgFilterGroup = this.getEntityStoreRegistry().registerSystemGroup();
            CompanionDamageFilter damageFilter = new CompanionDamageFilter((SystemGroup<EntityStore>)dmgFilterGroup, this.companionManager, this.getLogger());
            this.getEntityStoreRegistry().registerSystem((ISystem)damageFilter);
            this.getLogger().at(Level.INFO).log("Companion damage filter registered.");
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.getLogger().at(Level.WARNING).withCause(t)).log("Failed to register damage filter \u2014 companion friendly fire may occur.");
        }
        this.getEventRegistry().register(PlayerConnectEvent.class, this::onPlayerConnect);
        this.getLogger().at(Level.INFO).log("CompanionPlugin setup complete \u2014 all systems loaded.");
    }

    public void shutdown() {
        this.silencePluginLogging();
        this.getLogger().at(Level.INFO).log("CompanionPlugin shutdown");
        CompanionOpenUiInteraction.setOpenUiCallback(null);
        if (this.companionPanel != null) {
            try {
                this.companionPanel.shutdown();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        if (this.companionSystem != null) {
            this.companionSystem.shutdown();
        }
        if (this.companionManager == null) {
            return;
        }
        try {
            this.companionManager.save();
        }
        catch (IOException ex) {
            ((HytaleLogger.Api)this.getLogger().at(Level.SEVERE).withCause((Throwable)ex)).log("Failed to save companion data");
        }
    }

    private void onPlayerConnect(PlayerConnectEvent event) {
        PlayerRef player = event.getPlayerRef();
        if (this.companionManager == null) {
            return;
        }
        this.companionManager.getOrCreate(player.getUuid());
        this.companionManager.resetPlayerRuntime(player.getUuid());
        this.companionManager.clearAllRuntimes(player.getUuid());
        this.runPostConnectRestore(player, 0);
        player.sendMessage(Message.raw((String)"CompanionPlugin loaded. Use /fhc or /companion uimenu."));
    }

    private void runPostConnectRestore(PlayerRef player, int attempt) {
        if (player == null || this.companionManager == null || this.companionSystem == null) {
            return;
        }
        UUID worldId = player.getWorldUuid();
        if (worldId == null) {
            this.schedulePostConnectRetry(player, attempt);
            return;
        }
        World world = Universe.get().getWorld(worldId);
        if (world == null) {
            this.schedulePostConnectRetry(player, attempt);
            return;
        }
        world.execute(() -> this.performPostConnectRestore(player));
    }

    private void schedulePostConnectRetry(PlayerRef player, int attempt) {
        if (attempt >= 20) {
            this.getLogger().at(Level.WARNING).log("Post-connect companion restore timed out for " + player.getUsername());
            return;
        }
        CompletableFuture.delayedExecutor(500L, TimeUnit.MILLISECONDS).execute(() -> this.runPostConnectRestore(player, attempt + 1));
    }

    private void performPostConnectRestore(PlayerRef player) {
        Set<String> toResummon;
        int cleaned;
        int claimed = this.companionSystem.reconcileExistingManagedCompanions(player, 96.0);
        if (claimed > 0) {
            this.getLogger().at(Level.INFO).log("Reconciled " + claimed + " existing companion NPC(s) for " + player.getUsername());
        }
        if ((cleaned = this.companionSystem.cleanupOrphanManagedCompanions(player, 96.0)) > 0) {
            this.getLogger().at(Level.INFO).log("Cleaned " + cleaned + " orphan companion NPC(s) for " + player.getUsername());
        }
        if (!(toResummon = this.companionManager.consumePendingResummons(player.getUuid())).isEmpty()) {
            PlayerCompanionData data = this.companionManager.getOrCreate(player.getUuid());
            int count = 0;
            int alreadyRecovered = 0;
            int deferred = 0;
            for (CompanionRecord companion : data.companions) {
                if (!toResummon.contains(companion.uniqueId) || companion.fallen) continue;
                if (companion.active && this.companionSystem.hasLiveEntity(player, companion)) {
                    ++alreadyRecovered;
                    continue;
                }
                if (this.companionSystem.summonCompanion(player, companion, true)) {
                    ++count;
                    continue;
                }
                this.companionManager.markPendingResummon(player.getUuid(), companion.uniqueId);
                ++deferred;
            }
            if (count > 0) {
                player.sendMessage(Message.raw((String)("Auto-summoned " + count + " companion(s) from last session.")));
                this.getLogger().at(Level.INFO).log("Auto-summoned " + count + " companions for " + player.getUsername());
            }
            if (deferred > 0) {
                this.getLogger().at(Level.INFO).log("Deferred " + deferred + " companion auto-summon(s) for " + player.getUsername() + ".");
            }
            if (alreadyRecovered > 0) {
                this.getLogger().at(Level.INFO).log("Kept " + alreadyRecovered + " already-loaded companion(s) for " + player.getUsername() + ".");
            }
        }
        try {
            this.companionManager.save();
        }
        catch (IOException ex) {
            ((HytaleLogger.Api)this.getLogger().at(Level.SEVERE).withCause((Throwable)ex)).log("Failed to save companion data");
        }
    }

    private boolean isHyUIAvailable() {
        try {
            Class.forName("au.ellie.hyui.builders.PageBuilder");
            return true;
        }
        catch (Throwable t) {
            return false;
        }
    }

    private void silencePluginLogging() {
        if (CompanionSystem.isAppearancePersistDebugEnabled()) {
            try {
                this.getLogger().setLevel(Level.INFO);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                HytaleLogger.get((String)"CompanionNPC").setLevel(Level.INFO);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                Logger.getLogger("CompanionNPC").setLevel(Level.INFO);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            return;
        }
        try {
            this.getLogger().setLevel(Level.OFF);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            HytaleLogger.get((String)"CompanionNPC").setLevel(Level.OFF);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            Logger.getLogger("CompanionNPC").setLevel(Level.OFF);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }
}

