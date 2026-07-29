/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.logger.HytaleLogger
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe
 *  com.hypixel.hytale.server.core.event.events.player.PlayerCraftEvent
 *  com.hypixel.hytale.server.core.inventory.MaterialQuantity
 *  com.hypixel.hytale.server.core.plugin.JavaPlugin
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.World
 */
package com.yourname.companion.systems;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.event.events.player.PlayerCraftEvent;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.yourname.companion.data.CompanionRecord;
import com.yourname.companion.data.PlayerCompanionData;
import com.yourname.companion.data.ReviveConfig;
import com.yourname.companion.storage.CompanionManager;
import com.yourname.companion.systems.CompanionSystem;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;

public final class ReviveBenchIntegration {
    private static final boolean DEBUG_REVIVE_BENCH_DISCOVERY = false;
    private static final String[] BENCH_CANDIDATES = new String[]{"Arcanebench", "Alchemybench", "Workbench"};
    private static final String[] CATEGORY_CANDIDATES = new String[]{"Arcane_Misc", "Arcane_Portals"};
    private final CompanionManager companionManager;
    private final CompanionSystem companionSystem;
    private final HytaleLogger logger;
    private volatile boolean recipesRegistered = false;
    private volatile boolean registrationAttempted = false;

    private static int costReviveLast() {
        return ReviveConfig.REVIVE_COST_AMOUNT;
    }

    private static int costReviveBest() {
        return Math.max(0, ReviveConfig.REVIVE_COST_AMOUNT - 1);
    }

    public ReviveBenchIntegration(CompanionManager mgr, CompanionSystem sys, HytaleLogger log) {
        this.companionManager = mgr;
        this.companionSystem = sys;
        this.logger = log;
    }

    public void registerEvents(JavaPlugin plugin) {
        plugin.getEventRegistry().register(PlayerCraftEvent.class, (Object)null, this::onPlayerCraft);
        this.logger.at(Level.INFO).log("[ReviveBench] Event listener registered.");
    }

    public void tryRegisterRecipe(World world) {
        if (this.registrationAttempted) {
            return;
        }
        this.registrationAttempted = true;
        world.execute(() -> {
            try {
                this.doRegisterRecipes();
            }
            catch (Throwable t) {
                ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("[ReviveBench] Recipe registration failed. /companion revive still works.");
            }
        });
    }

    private void doRegisterRecipes() {
        this.logger.at(Level.INFO).log("[ReviveBench] Recipe registration disabled; use /companion revive.");
    }

    private void logAllBenches() {
    }

    private void onPlayerCraft(PlayerCraftEvent event) {
        if (!this.recipesRegistered) {
            return;
        }
        CraftingRecipe recipe = event.getCraftedRecipe();
        ReviveType type = this.identifyReviveRecipe(recipe);
        if (type == null) {
            return;
        }
        PlayerRef playerRef = this.resolvePlayerRef(event);
        if (playerRef == null) {
            this.logger.at(Level.WARNING).log("[ReviveBench] Crafted revive recipe but could not identify player.");
            return;
        }
        switch (type.ordinal()) {
            case 0: {
                this.performReviveLast(playerRef);
                break;
            }
            case 1: {
                this.performReviveBest(playerRef);
            }
        }
    }

    private ReviveType identifyReviveRecipe(CraftingRecipe recipe) {
        if (recipe == null) {
            return null;
        }
        try {
            MaterialQuantity[] inputs = recipe.getInput();
            if (inputs == null || inputs.length != 1) {
                return null;
            }
            if (!ReviveConfig.REVIVE_COST_ITEM.equals(inputs[0].getItemId())) {
                return null;
            }
            int qty = inputs[0].getQuantity();
            if (qty == ReviveBenchIntegration.costReviveLast()) {
                return ReviveType.LAST_KILLED;
            }
            if (qty == ReviveBenchIntegration.costReviveBest()) {
                return ReviveType.BEST_COMPANION;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    private PlayerRef resolvePlayerRef(PlayerCraftEvent event) {
        try {
            Method[] m = event.getClass().getMethod("getPlayerRef", new Class[0]);
            Object result = m.invoke((Object)event, new Object[0]);
            if (result instanceof PlayerRef) {
                PlayerRef pr = (PlayerRef)result;
                return pr;
            }
        }
        catch (Throwable m) {
            // empty catch block
        }
        try {
            for (Method m : event.getClass().getMethods()) {
                Object result;
                if (!PlayerRef.class.isAssignableFrom(m.getReturnType()) || m.getParameterCount() != 0 || !((result = m.invoke((Object)event, new Object[0])) instanceof PlayerRef)) continue;
                PlayerRef pr = (PlayerRef)result;
                return pr;
            }
        }
        catch (Throwable m) {
            // empty catch block
        }
        try {
            for (Class<?> clazz = event.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
                for (Field f : clazz.getDeclaredFields()) {
                    f.setAccessible(true);
                    Object v = f.get(event);
                    if (!(v instanceof PlayerRef)) continue;
                    PlayerRef pr = (PlayerRef)v;
                    return pr;
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    private void performReviveLast(PlayerRef playerRef) {
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        List<CompanionRecord> fallen = data.getFallenCompanions();
        if (fallen.isEmpty()) {
            playerRef.sendMessage(Message.raw((String)"No fallen companions to revive."));
            return;
        }
        this.doRevive(playerRef, fallen.get(fallen.size() - 1));
    }

    private void performReviveBest(PlayerRef playerRef) {
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        List<CompanionRecord> fallen = data.getFallenCompanions();
        if (fallen.isEmpty()) {
            playerRef.sendMessage(Message.raw((String)"No fallen companions to revive."));
            return;
        }
        CompanionRecord best = null;
        int bestValue = -1;
        for (CompanionRecord fc : fallen) {
            int value = fc.combatLevel + fc.farmLevel + fc.combatKills + fc.farmHarvests + (fc.savedInventory != null ? fc.savedInventory.size() * 10 : 0);
            if (value <= bestValue) continue;
            bestValue = value;
            best = fc;
        }
        if (best != null) {
            this.doRevive(playerRef, best);
        }
    }

    private void doRevive(PlayerRef playerRef, CompanionRecord companion) {
        String reviveError = this.companionSystem.tryReviveCompanion(playerRef, companion, false);
        if (reviveError == null) {
            playerRef.sendMessage(Message.raw((String)(companion.getDisplayName() + " has been revived! (Combat Lv" + companion.combatLevel + " | Farm Lv" + companion.farmLevel + ")")));
        } else {
            playerRef.sendMessage(Message.raw((String)reviveError));
        }
        try {
            this.companionManager.save();
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("[ReviveBench] Failed to save after revive.");
        }
    }

    private static enum ReviveType {
        LAST_KILLED,
        BEST_COMPANION;

    }
}

