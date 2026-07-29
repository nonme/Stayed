/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.command.system.CommandContext
 *  com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.Universe
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.yourname.companion.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.yourname.companion.data.BlockPos;
import com.yourname.companion.data.CompanionMode;
import com.yourname.companion.data.CompanionRecord;
import com.yourname.companion.data.FollowMode;
import com.yourname.companion.data.PlayerCompanionData;
import com.yourname.companion.data.ProgressionConfig;
import com.yourname.companion.gui.CompanionPanel;
import com.yourname.companion.runtime.CompanionRuntimeState;
import com.yourname.companion.runtime.PlayerRuntimeState;
import com.yourname.companion.storage.CompanionManager;
import com.yourname.companion.systems.CompanionSystem;
import com.yourname.companion.util.WorldQueries;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class CompanionCommand
extends AbstractPlayerCommand {
    private static final int MAX_LINKED_CHESTS = 10;
    private static final long UI_OPEN_DEBOUNCE_MS = 1200L;
    private static final Logger LOGGER = Logger.getLogger("CompanionNPC");
    private final CompanionManager companionManager;
    private final CompanionSystem companionSystem;
    private final CompanionPanel companionPanel;
    private final Map<UUID, Long> lastUiOpenAttemptMs = new ConcurrentHashMap<UUID, Long>();

    public CompanionCommand(CompanionManager companionManager, CompanionSystem companionSystem, CompanionPanel companionPanel) {
        super("companion", "Companion plugin command");
        this.companionManager = companionManager;
        this.companionSystem = companionSystem;
        this.companionPanel = companionPanel;
        this.setAllowsExtraArguments(true);
    }

    protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> playerRefEntity, PlayerRef playerRef, World world) {
        String subcommand;
        String[] parts = context.getInputString().trim().split("\\s+");
        switch (subcommand = parts.length > 1 ? parts[1].toLowerCase() : "help") {
            case "create": {
                this.createCompanion(context, playerRef, parts);
                break;
            }
            case "list": {
                this.showList(context, playerRef);
                break;
            }
            case "select": {
                this.selectCompanion(context, playerRef, parts);
                break;
            }
            case "status": {
                this.showStatus(context, playerRef, parts);
                break;
            }
            case "debug": {
                this.showDebug(context, playerRef, parts);
                break;
            }
            case "progression": 
            case "levels": {
                this.showProgression(context, playerRef, parts);
                break;
            }
            case "roles": {
                this.showRoles(context, false);
                break;
            }
            case "rolesall": {
                this.showRoles(context, true);
                break;
            }
            case "models": {
                this.showModels(context, parts);
                break;
            }
            case "appearance": {
                this.setAppearance(context, playerRef, parts);
                break;
            }
            case "appearancerandom": {
                this.clearAppearance(context, playerRef, parts);
                break;
            }
            case "rerollappearance": {
                this.rerollAppearance(context, playerRef, parts);
                break;
            }
            case "summon": {
                this.summon(context, playerRef, parts);
                break;
            }
            case "dismiss": {
                this.dismiss(context, playerRef, parts);
                break;
            }
            case "follow": {
                this.setStance(context, playerRef, FollowMode.FOLLOW, parts);
                break;
            }
            case "stay": {
                this.setStance(context, playerRef, FollowMode.STAY, parts);
                break;
            }
            case "fighter": 
            case "fight": 
            case "defend": {
                this.setRole(context, playerRef, CompanionMode.FIGHTER, parts);
                break;
            }
            case "farmer": 
            case "farm": {
                this.setRole(context, playerRef, CompanionMode.FARMER, parts);
                break;
            }
            case "miner": 
            case "mine": {
                this.setRole(context, playerRef, CompanionMode.MINER, parts);
                break;
            }
            case "patrol": {
                this.setStance(context, playerRef, FollowMode.PATROL, parts);
                break;
            }
            case "free": {
                this.setStance(context, playerRef, FollowMode.FREE, parts);
                break;
            }
            case "giveall": {
                this.giveAll(context, playerRef, parts);
                break;
            }
            case "takeall": {
                this.takeAll(context, playerRef, parts);
                break;
            }
            case "deposit": {
                this.requestDeposit(context, playerRef);
                break;
            }
            case "chestlink": {
                this.linkChest(context, playerRef);
                break;
            }
            case "chestunlink": {
                this.unlinkChest(context, playerRef);
                break;
            }
            case "givehand": {
                this.giveHand(context, playerRef, parts);
                break;
            }
            case "takehand": {
                this.takeHand(context, playerRef, parts);
                break;
            }
            case "equip": {
                this.equipHeldItem(context, playerRef, parts);
                break;
            }
            case "rename": {
                this.renameCompanion(context, playerRef, parts);
                break;
            }
            case "menu": {
                this.handleMenu(context, playerRef, parts);
                break;
            }
            case "uimenu": 
            case "menuui": {
                this.openUIMenu(context, store, playerRefEntity, playerRef);
                break;
            }
            case "revive": {
                this.handleRevive(context, playerRef, parts, world);
                break;
            }
            case "fallen": {
                this.showFallen(context, playerRef);
                break;
            }
            case "farmtest": {
                this.farmTest(context, playerRef, world);
                break;
            }
            case "help": {
                this.sendHelp(context);
                break;
            }
            default: {
                this.sendHelp(context);
            }
        }
    }

    private CompanionRecord resolveTarget(CommandContext context, PlayerRef playerRef, String[] parts, int nameArgIndex) {
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        if (nameArgIndex >= 0 && parts.length > nameArgIndex) {
            String nameArg = this.joinArgs(parts, nameArgIndex);
            CompanionRecord byName = data.findCompanionByName(nameArg);
            if (byName != null) {
                return byName;
            }
            context.sendMessage(Message.raw((String)("No companion named '" + nameArg + "'. Use /companion list.")));
            return null;
        }
        CompanionRecord selected = data.getSelectedCompanion();
        if (selected != null && selected.active && !selected.fallen) {
            return selected;
        }
        List<CompanionRecord> actives = data.getActiveCompanions();
        if (actives.size() == 1) {
            return actives.get(0);
        }
        if (actives.isEmpty()) {
            context.sendMessage(Message.raw((String)"No active companion. Use /companion summon."));
        } else {
            context.sendMessage(Message.raw((String)"Multiple companions active \u2014 specify a name or use /companion select <name>."));
        }
        return null;
    }

    private String joinArgs(String[] parts, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < parts.length; ++i) {
            if (i > from) {
                sb.append(" ");
            }
            sb.append(parts[i]);
        }
        return sb.toString().trim();
    }

    private void createCompanion(CommandContext context, PlayerRef playerRef, String[] parts) {
        String name;
        String string = name = parts.length > 2 ? this.joinArgs(parts, 2) : null;
        if (name != null && name.length() > 32) {
            context.sendMessage(Message.raw((String)"Name too long (max 32 characters)."));
            return;
        }
        if (name != null && this.companionManager.isNameTaken(name, null)) {
            context.sendMessage(Message.raw((String)"That name is already taken."));
            return;
        }
        CompanionRecord companion = this.companionSystem.createCompanion(playerRef, name);
        this.save(context);
        context.sendMessage(Message.raw((String)("Created companion: " + companion.getDisplayName())));
        context.sendMessage(Message.raw((String)"Use /companion list to see your roster."));
    }

    private void showList(CommandContext context, PlayerRef playerRef) {
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        if (data.companions.isEmpty()) {
            context.sendMessage(Message.raw((String)"No companions yet. Use /companion create [name] to get started."));
            return;
        }
        context.sendMessage(Message.raw((String)("=== Your Companions (" + data.companions.size() + " total) ===")));
        for (int i = 0; i < data.companions.size(); ++i) {
            CompanionRecord c = data.companions.get(i);
            String marker = c.uniqueId != null && c.uniqueId.equals(data.selectedCompanionId) ? " <--" : "";
            context.sendMessage(Message.raw((String)("  " + (i + 1) + ". " + c.getStatusSummary() + marker)));
        }
    }

    private void selectCompanion(CommandContext context, PlayerRef playerRef, String[] parts) {
        if (parts.length < 3) {
            context.sendMessage(Message.raw((String)"Usage: /companion select <name or number>"));
            return;
        }
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        String arg = this.joinArgs(parts, 2);
        try {
            int idx = Integer.parseInt(arg) - 1;
            if (idx >= 0 && idx < data.companions.size()) {
                CompanionRecord c = data.companions.get(idx);
                data.selectedCompanionId = c.uniqueId;
                this.save(context);
                context.sendMessage(Message.raw((String)("Selected: " + c.getDisplayName())));
                return;
            }
        }
        catch (NumberFormatException idx) {
            // empty catch block
        }
        CompanionRecord c = data.findCompanionByName(arg);
        if (c != null) {
            data.selectedCompanionId = c.uniqueId;
            this.save(context);
            context.sendMessage(Message.raw((String)("Selected: " + c.getDisplayName())));
        } else {
            context.sendMessage(Message.raw((String)("No companion found: " + arg)));
        }
    }

    private void showStatus(CommandContext context, PlayerRef playerRef, String[] parts) {
        CompanionRecord companion = this.resolveTarget(context, playerRef, parts, 2);
        if (companion == null) {
            return;
        }
        CompanionRuntimeState runtime = this.companionManager.getRuntime(companion.uniqueId);
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        context.sendMessage(Message.raw((String)("=== " + companion.getDisplayName() + " ===")));
        context.sendMessage(Message.raw((String)("Role: " + String.valueOf((Object)companion.mode) + " | Stance: " + String.valueOf((Object)companion.followMode) + " | State: " + String.valueOf((Object)runtime.state))));
        context.sendMessage(Message.raw((String)("Combat: Lv" + companion.combatLevel + " (" + ProgressionConfig.COMBAT_LEVEL_NAMES[Math.min(companion.combatLevel - 1, ProgressionConfig.COMBAT_LEVEL_NAMES.length - 1)] + ") \u2014 " + companion.combatKills + " kills")));
        context.sendMessage(Message.raw((String)("Farming: Lv" + companion.farmLevel + " (" + ProgressionConfig.FARM_LEVEL_NAMES[Math.min(companion.farmLevel - 1, ProgressionConfig.FARM_LEVEL_NAMES.length - 1)] + ") \u2014 " + companion.farmHarvests + " harvests")));
        context.sendMessage(Message.raw((String)("Mining: Lv" + companion.mineLevel + " (" + ProgressionConfig.MINE_LEVEL_NAMES[Math.min(companion.mineLevel - 1, ProgressionConfig.MINE_LEVEL_NAMES.length - 1)] + ") \u2014 " + companion.mineBlocks + " blocks")));
        context.sendMessage(Message.raw((String)("Linked chests: " + data.linkedChests.size())));
        context.sendMessage(Message.raw((String)("Loot: " + (data.lootEnabled ? "ON" : "OFF") + " | Deposit: " + (data.depositEnabled ? "ON" : "OFF"))));
        context.sendMessage(Message.raw((String)("Fallen companions: " + data.getFallenCompanions().size())));
        context.sendMessage(Message.raw((String)("Active: " + (companion.active ? "YES" : "NO"))));
    }

    private void showDebug(CommandContext context, PlayerRef playerRef, String[] parts) {
        CompanionRecord companion = this.resolveTarget(context, playerRef, parts, 2);
        if (companion == null) {
            return;
        }
        String debugInfo = this.companionSystem.getDebugInfo(playerRef, companion);
        context.sendMessage(Message.raw((String)("[DEBUG] " + debugInfo)));
    }

    private void showProgression(CommandContext context, PlayerRef playerRef, String[] parts) {
        CompanionRecord companion = this.resolveTarget(context, playerRef, parts, 2);
        if (companion == null) {
            return;
        }
        context.sendMessage(Message.raw((String)("=== " + companion.getDisplayName() + " Progression ===")));
        int combatIdx = Math.min(companion.combatLevel - 1, ProgressionConfig.COMBAT_LEVEL_NAMES.length - 1);
        context.sendMessage(Message.raw((String)("Combat: Lv" + companion.combatLevel + " \u2014 " + ProgressionConfig.COMBAT_LEVEL_NAMES[combatIdx])));
        context.sendMessage(Message.raw((String)("  Kills: " + companion.combatKills)));
        int killsNeeded = ProgressionConfig.killsToNextCombatLevel(companion.combatLevel, companion.combatKills);
        if (killsNeeded > 0) {
            context.sendMessage(Message.raw((String)("  Next level in " + killsNeeded + " kills")));
        } else if (killsNeeded == -1) {
            context.sendMessage(Message.raw((String)"  MAX LEVEL reached!"));
        }
        int dmgIdx = Math.min(companion.combatLevel - 1, ProgressionConfig.COMBAT_DAMAGE_MULT.length - 1);
        double dmgMult = ProgressionConfig.COMBAT_DAMAGE_MULT[dmgIdx];
        if (dmgMult > 1.0) {
            context.sendMessage(Message.raw((String)("  Damage bonus: +" + (int)((dmgMult - 1.0) * 100.0) + "%")));
        }
        int farmIdx = Math.min(companion.farmLevel - 1, ProgressionConfig.FARM_LEVEL_NAMES.length - 1);
        context.sendMessage(Message.raw((String)("Farming: Lv" + companion.farmLevel + " \u2014 " + ProgressionConfig.FARM_LEVEL_NAMES[farmIdx])));
        context.sendMessage(Message.raw((String)("  Harvests: " + companion.farmHarvests)));
        int harvestsNeeded = ProgressionConfig.harvestsToNextFarmLevel(companion.farmLevel, companion.farmHarvests);
        if (harvestsNeeded > 0) {
            context.sendMessage(Message.raw((String)("  Next level in " + harvestsNeeded + " harvests")));
        } else if (harvestsNeeded == -1) {
            context.sendMessage(Message.raw((String)"  MAX LEVEL reached!"));
        }
        double speedMult = ProgressionConfig.FARM_SPEED_MULT[farmIdx];
        if (speedMult > 1.0) {
            context.sendMessage(Message.raw((String)("  Harvest speed: +" + (int)((speedMult - 1.0) * 100.0) + "%")));
        }
        if (ProgressionConfig.FARM_AUTO_REPLANT[farmIdx]) {
            context.sendMessage(Message.raw((String)"  Auto-replant: ACTIVE"));
        }
        if (ProgressionConfig.FARM_DOUBLE_YIELD[farmIdx]) {
            context.sendMessage(Message.raw((String)"  Double yield: ACTIVE"));
        }
        int mineIdx = Math.min(companion.mineLevel - 1, ProgressionConfig.MINE_LEVEL_NAMES.length - 1);
        context.sendMessage(Message.raw((String)("Mining: Lv" + companion.mineLevel + " \u2014 " + ProgressionConfig.MINE_LEVEL_NAMES[mineIdx])));
        context.sendMessage(Message.raw((String)("  Blocks mined: " + companion.mineBlocks)));
        int blocksNeeded = ProgressionConfig.blocksToNextMineLevel(companion.mineLevel, companion.mineBlocks);
        if (blocksNeeded > 0) {
            context.sendMessage(Message.raw((String)("  Next level in " + blocksNeeded + " blocks")));
        } else if (blocksNeeded == -1) {
            context.sendMessage(Message.raw((String)"  MAX LEVEL reached!"));
        }
        double mineMult = ProgressionConfig.MINE_SPEED_MULT[mineIdx];
        if (mineMult > 1.0) {
            context.sendMessage(Message.raw((String)("  Mining speed: +" + (int)((mineMult - 1.0) * 100.0) + "%")));
        }
    }

    public void showInteractMenu(PlayerRef playerRef) {
        String string;
        String currentRole;
        PlayerCompanionData data;
        CompanionRecord companion;
        PlayerRuntimeState playerRuntime = this.companionManager.getPlayerRuntime(playerRef.getUuid());
        if (playerRuntime.awaitingChestLink) {
            playerRuntime.awaitingChestLink = false;
        }
        if ((companion = (data = this.companionManager.getOrCreate(playerRef.getUuid())).resolveSelectedOrOnly()) != null) {
            switch (companion.mode) {
                default: {
                    throw new MatchException(null, null);
                }
                case FIGHTER: {
                    v0 = "Fighter";
                    break;
                }
                case FARMER: {
                    v0 = "Farmer";
                    break;
                }
                case MINER: {
                    v0 = "Miner";
                    break;
                }
            }
        } else {
            v0 = currentRole = "?";
        }
        if (companion != null) {
            switch (companion.followMode) {
                default: {
                    throw new MatchException(null, null);
                }
                case FOLLOW: {
                    string = "Follow";
                    break;
                }
                case STAY: {
                    string = "Stay";
                    break;
                }
                case PATROL: {
                    string = "Patrol";
                    break;
                }
                case FREE: {
                    string = "Free";
                    break;
                }
            }
        } else {
            string = "?";
        }
        String currentStance = string;
        String name = companion != null ? companion.getDisplayName() : "No companion selected";
        playerRef.sendMessage(Message.raw((String)""));
        playerRef.sendMessage(Message.raw((String)("=== Companion Menu (" + name + ") ===")));
        playerRef.sendMessage(Message.raw((String)("Current: " + currentRole + " + " + currentStance)));
        playerRef.sendMessage(Message.raw((String)"Type /companion menu <number> to select:"));
        playerRef.sendMessage(Message.raw((String)""));
        playerRef.sendMessage(Message.raw((String)"-- Role (what they do) --"));
        playerRef.sendMessage(Message.raw((String)"  1. Fighter (proactive combat, guards you)"));
        playerRef.sendMessage(Message.raw((String)"  2. Farmer (harvest crops, fights if attacked)"));
        playerRef.sendMessage(Message.raw((String)"  3. Miner (mine blocks, collect ores)"));
        playerRef.sendMessage(Message.raw((String)"-- Stance (where they are) --"));
        playerRef.sendMessage(Message.raw((String)"  4. Follow (companion follows you)"));
        playerRef.sendMessage(Message.raw((String)"  5. Stay (companion holds position)"));
        playerRef.sendMessage(Message.raw((String)"  6. Patrol (wanders within 20 blocks of anchor)"));
        playerRef.sendMessage(Message.raw((String)"  7. Free (no movement restrictions)"));
        playerRef.sendMessage(Message.raw((String)"-- Items --"));
        playerRef.sendMessage(Message.raw((String)"  8. Link a chest"));
        playerRef.sendMessage(Message.raw((String)"  9. Unlink a chest"));
        playerRef.sendMessage(Message.raw((String)"  10. Force deposit now"));
        playerRef.sendMessage(Message.raw((String)"  11. Give held item to companion"));
        playerRef.sendMessage(Message.raw((String)"  12. Take item from companion"));
        playerRef.sendMessage(Message.raw((String)"  13. Give All items to companion"));
        playerRef.sendMessage(Message.raw((String)"  14. Take All items from companion"));
        playerRef.sendMessage(Message.raw((String)"-- Other --"));
        playerRef.sendMessage(Message.raw((String)"  15. Rename (/companion rename <name>)"));
        playerRef.sendMessage(Message.raw((String)"  16. View status & progression"));
        playerRef.sendMessage(Message.raw((String)"  17. Reroll appearance"));
        playerRef.sendMessage(Message.raw((String)"  18. Dismiss companion"));
        playerRef.sendMessage(Message.raw((String)"  19. View fallen companions"));
        playerRef.sendMessage(Message.raw((String)"  20. List all companions"));
        playerRef.sendMessage(Message.raw((String)"  21. Equip held item on companion"));
        playerRef.sendMessage(Message.raw((String)""));
        playerRef.sendMessage(Message.raw((String)"Combos: Fighter+Follow=Companion, Fighter+Stay=Guard"));
        playerRef.sendMessage(Message.raw((String)"        Farmer+Follow=Harvester, Farmer+Stay=Farmhand"));
        playerRef.sendMessage(Message.raw((String)"        Miner+Follow=Digger, Miner+Stay=Excavator"));
    }

    private void handleMenu(CommandContext context, PlayerRef playerRef, String[] parts) {
        int choice;
        if (parts.length < 3) {
            this.showInteractMenu(playerRef);
            return;
        }
        try {
            choice = Integer.parseInt(parts[2]);
        }
        catch (NumberFormatException e) {
            context.sendMessage(Message.raw((String)"Invalid menu option. Use a number 1-21."));
            return;
        }
        switch (choice) {
            case 1: {
                this.setRole(context, playerRef, CompanionMode.FIGHTER, parts);
                break;
            }
            case 2: {
                this.setRole(context, playerRef, CompanionMode.FARMER, parts);
                break;
            }
            case 3: {
                this.setRole(context, playerRef, CompanionMode.MINER, parts);
                break;
            }
            case 4: {
                this.setStance(context, playerRef, FollowMode.FOLLOW, parts);
                break;
            }
            case 5: {
                this.setStance(context, playerRef, FollowMode.STAY, parts);
                break;
            }
            case 6: {
                this.setStance(context, playerRef, FollowMode.PATROL, parts);
                break;
            }
            case 7: {
                this.setStance(context, playerRef, FollowMode.FREE, parts);
                break;
            }
            case 8: {
                this.startChestLink(context, playerRef);
                break;
            }
            case 9: {
                this.unlinkChest(context, playerRef);
                break;
            }
            case 10: {
                this.requestDeposit(context, playerRef);
                break;
            }
            case 11: {
                this.giveHand(context, playerRef, parts);
                break;
            }
            case 12: {
                this.takeHand(context, playerRef, parts);
                break;
            }
            case 13: {
                this.giveAll(context, playerRef, parts);
                break;
            }
            case 14: {
                this.takeAll(context, playerRef, parts);
                break;
            }
            case 15: {
                context.sendMessage(Message.raw((String)"Use: /companion rename <new name>"));
                break;
            }
            case 16: {
                this.showStatus(context, playerRef, parts);
                this.showProgression(context, playerRef, parts);
                break;
            }
            case 17: {
                this.rerollAppearance(context, playerRef, parts);
                break;
            }
            case 18: {
                this.dismiss(context, playerRef, parts);
                break;
            }
            case 19: {
                this.showFallen(context, playerRef);
                break;
            }
            case 20: {
                this.showList(context, playerRef);
                break;
            }
            case 21: {
                this.equipHeldItem(context, playerRef, parts);
                break;
            }
            default: {
                context.sendMessage(Message.raw((String)"Invalid menu option. Use a number 1-21."));
            }
        }
    }

    private void openUIMenu(CommandContext context, Store<EntityStore> store, Ref<EntityStore> playerRefEntity, PlayerRef playerRef) {
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        if (data.companions == null || data.companions.isEmpty()) {
            context.sendMessage(Message.raw((String)"No companions added"));
            return;
        }
        if (this.companionPanel != null) {
            try {
                if (this.tryOpenVisualPanel(playerRef, store)) {
                    return;
                }
            }
            catch (Throwable t) {
                context.sendMessage(Message.raw((String)"Visual companion UI failed to open. Check server log."));
                return;
            }
        }
        context.sendMessage(Message.raw((String)"Visual companion UI is not available."));
    }

    public void openUIMenuForPlayer(PlayerRef playerRef) {
        try {
            PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
            if (data.companions == null || data.companions.isEmpty()) {
                playerRef.sendMessage(Message.raw((String)"No companions added"));
                return;
            }
            UUID worldId = playerRef.getWorldUuid();
            if (worldId == null) {
                return;
            }
            World world = Universe.get().getWorld(worldId);
            if (world == null) {
                return;
            }
            Store store = world.getEntityStore().getStore();
            if (this.companionPanel != null && this.tryOpenVisualPanel(playerRef, (Store<EntityStore>)store)) {
                return;
            }
            playerRef.sendMessage(Message.raw((String)"Visual companion UI is not available."));
        }
        catch (Throwable t) {
            playerRef.sendMessage(Message.raw((String)"Visual companion UI failed to open. Check server log."));
        }
    }

    public boolean isVisualUiOpen(UUID playerId) {
        return this.companionPanel != null && playerId != null && this.companionPanel.isOpen(playerId);
    }

    private boolean tryOpenVisualPanel(PlayerRef playerRef, Store<EntityStore> store) {
        long last;
        if (this.companionPanel == null || playerRef == null || store == null) {
            return false;
        }
        UUID playerId = playerRef.getUuid();
        if (playerId == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - (last = this.lastUiOpenAttemptMs.getOrDefault(playerId, 0L).longValue()) < 1200L) {
            return true;
        }
        if (this.companionPanel.isOpen(playerId)) {
            this.companionPanel.closePanel(playerId);
        }
        this.companionPanel.openPanel(playerRef, store);
        this.lastUiOpenAttemptMs.put(playerId, now);
        return true;
    }

    private void startChestLink(CommandContext context, PlayerRef playerRef) {
        PlayerRuntimeState playerRuntime = this.companionManager.getPlayerRuntime(playerRef.getUuid());
        playerRuntime.awaitingChestLink = true;
        context.sendMessage(Message.raw((String)"Chest link mode activated!"));
        context.sendMessage(Message.raw((String)"Look at the chest you want to link and press F (activate)."));
        context.sendMessage(Message.raw((String)"Or use /companion chestlink while looking at the chest."));
        context.sendMessage(Message.raw((String)"Type /companion menu to cancel."));
    }

    public void completeChestLink(PlayerRef playerRef) {
        PlayerRuntimeState playerRuntime = this.companionManager.getPlayerRuntime(playerRef.getUuid());
        if (!playerRuntime.awaitingChestLink) {
            return;
        }
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        CompanionRecord selected = data.getSelectedCompanion();
        if (selected == null) {
            playerRef.sendMessage(Message.raw((String)"No selected companion."));
            playerRuntime.awaitingChestLink = false;
            return;
        }
        if (selected.linkedChests.size() >= 10) {
            playerRef.sendMessage(Message.raw((String)"Linked chest limit reached (10)."));
            playerRuntime.awaitingChestLink = false;
            return;
        }
        World world = playerRef.getWorldUuid() != null ? Universe.get().getWorld(playerRef.getWorldUuid()) : null;
        try {
            LOGGER.info("[ChestLinkDiag] complete " + WorldQueries.describeStorageLookResolution(world, playerRef, 5.0));
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        BlockPos lookedAt = WorldQueries.getLookedAtStorageBlockPos(world, playerRef, 5.0);
        if (lookedAt == null) {
            playerRef.sendMessage(Message.raw((String)"Could not find a storage block. Look at a chest and try again."));
            return;
        }
        if (selected.linkedChests.contains(lookedAt)) {
            playerRef.sendMessage(Message.raw((String)"That chest is already linked."));
            playerRuntime.awaitingChestLink = false;
            return;
        }
        selected.linkedChests.add(lookedAt);
        playerRuntime.awaitingChestLink = false;
        this.save(playerRef);
        playerRef.sendMessage(Message.raw((String)(selected.getDisplayName() + " linked chest at (" + lookedAt.x + ", " + lookedAt.y + ", " + lookedAt.z + ")! Total: " + selected.linkedChests.size())));
    }

    private void renameCompanion(CommandContext context, PlayerRef playerRef, String[] parts) {
        if (parts.length < 3) {
            context.sendMessage(Message.raw((String)"Usage: /companion rename <new name>"));
            return;
        }
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        CompanionRecord companion = data.resolveSelectedOrOnly();
        if (companion == null) {
            context.sendMessage(Message.raw((String)"Select a companion first with /companion select."));
            return;
        }
        String newName = this.joinArgs(parts, 2);
        if (newName.length() > 32) {
            context.sendMessage(Message.raw((String)"Name too long (max 32 characters)."));
            return;
        }
        if (newName.isBlank()) {
            context.sendMessage(Message.raw((String)"Name cannot be empty."));
            return;
        }
        if (this.companionManager.isNameTaken(newName, companion.uniqueId)) {
            context.sendMessage(Message.raw((String)"That name is already taken by another companion."));
            return;
        }
        this.companionSystem.renameCompanion(playerRef, companion, newName);
        this.save(context);
        context.sendMessage(Message.raw((String)("Companion renamed to: " + newName)));
    }

    private void showFallen(CommandContext context, PlayerRef playerRef) {
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        List<CompanionRecord> fallen = data.getFallenCompanions();
        if (fallen.isEmpty()) {
            context.sendMessage(Message.raw((String)"No fallen companions."));
            return;
        }
        context.sendMessage(Message.raw((String)"=== Fallen Companions ==="));
        context.sendMessage(Message.raw((String)"Use /companion revive <number or name>"));
        context.sendMessage(Message.raw((String)""));
        for (int i = 0; i < fallen.size(); ++i) {
            context.sendMessage(Message.raw((String)("  " + (i + 1) + ". " + fallen.get(i).getStatusSummary())));
        }
    }

    private void handleRevive(CommandContext context, PlayerRef playerRef, String[] parts, World world) {
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        List<CompanionRecord> fallen = data.getFallenCompanions();
        if (fallen.isEmpty()) {
            context.sendMessage(Message.raw((String)"No fallen companions to revive."));
            return;
        }
        if (parts.length < 3) {
            this.showFallen(context, playerRef);
            context.sendMessage(Message.raw((String)""));
            context.sendMessage(Message.raw((String)"Select one: /companion revive <number or name>"));
            return;
        }
        CompanionRecord target = null;
        String arg = this.joinArgs(parts, 2);
        try {
            int idx = Integer.parseInt(arg) - 1;
            if (idx >= 0 && idx < fallen.size()) {
                target = fallen.get(idx);
            }
        }
        catch (NumberFormatException idx) {
            // empty catch block
        }
        if (target == null) {
            for (CompanionRecord fc : fallen) {
                if (fc.name == null || !fc.name.equalsIgnoreCase(arg)) continue;
                target = fc;
                break;
            }
        }
        if (target == null) {
            context.sendMessage(Message.raw((String)"Invalid choice. Use /companion fallen to see the list."));
            return;
        }
        String reviveError = this.companionSystem.tryReviveCompanion(playerRef, target, true);
        if (reviveError == null) {
            this.save(context);
            context.sendMessage(Message.raw((String)(target.getDisplayName() + " has been revived!")));
            context.sendMessage(Message.raw((String)("Combat Lv" + target.combatLevel + " | Farm Lv" + target.farmLevel + " \u00e2\u20ac\u201d all progress restored.")));
        } else {
            context.sendMessage(Message.raw((String)reviveError));
        }
    }

    private void summon(CommandContext context, PlayerRef playerRef, String[] parts) {
        List<CompanionRecord> dormant;
        if (parts.length > 2 && parts[2].equalsIgnoreCase("all")) {
            int count = this.companionSystem.summonAll(playerRef);
            this.save(context);
            context.sendMessage(Message.raw((String)("Summoned " + count + " companion(s).")));
            return;
        }
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        if (parts.length > 2) {
            String nameArg = this.joinArgs(parts, 2);
            CompanionRecord c = data.findCompanionByName(nameArg);
            if (c == null) {
                context.sendMessage(Message.raw((String)("No companion named '" + nameArg + "'.")));
                return;
            }
            if (c.fallen) {
                context.sendMessage(Message.raw((String)(c.getDisplayName() + " is fallen. Use /companion revive.")));
                return;
            }
            boolean ok = this.companionSystem.summonCompanion(playerRef, c);
            this.save(context);
            context.sendMessage(Message.raw((String)(ok ? c.getDisplayName() + " summoned." : "Failed to summon.")));
            return;
        }
        if (data.companions.isEmpty()) {
            CompanionRecord companion = this.companionSystem.createCompanion(playerRef, null);
            this.save(context);
            context.sendMessage(Message.raw((String)"Companion summoned. Press F on your companion to open the menu."));
            return;
        }
        CompanionRecord target = data.getSelectedCompanion();
        if (target != null && target.fallen) {
            target = null;
        }
        if (target == null) {
            target = data.resolveSelectedOrOnly();
        }
        if (target == null && !(dormant = data.getDormantCompanions()).isEmpty()) {
            target = dormant.get(0);
        }
        if (target == null) {
            context.sendMessage(Message.raw((String)"All companions are active or fallen."));
            return;
        }
        boolean ok = this.companionSystem.summonCompanion(playerRef, target);
        this.save(context);
        context.sendMessage(Message.raw((String)(ok ? target.getDisplayName() + " summoned." : "Failed to summon.")));
    }

    private void dismiss(CommandContext context, PlayerRef playerRef, String[] parts) {
        if (parts.length > 2 && parts[2].equalsIgnoreCase("all")) {
            int count = this.companionSystem.dismissAll(playerRef);
            this.save(context);
            context.sendMessage(Message.raw((String)("Dismissed " + count + " companion(s).")));
            return;
        }
        CompanionRecord companion = this.resolveTarget(context, playerRef, parts, 2);
        if (companion == null) {
            return;
        }
        this.companionSystem.dismissCompanion(playerRef, companion);
        this.save(context);
        context.sendMessage(Message.raw((String)(companion.getDisplayName() + " dismissed.")));
    }

    private void setRole(CommandContext context, PlayerRef playerRef, CompanionMode role, String[] parts) {
        CompanionRecord companion = this.resolveTarget(context, playerRef, parts, 2);
        if (companion == null) {
            return;
        }
        companion.mode = role;
        if (role != CompanionMode.FARMER) {
            companion.farmAutoResume = false;
        }
        this.save(context);
        String roleName = switch (role) {
            default -> throw new MatchException(null, null);
            case CompanionMode.FIGHTER -> "Fighter";
            case CompanionMode.FARMER -> "Farmer";
            case CompanionMode.MINER -> "Miner";
        };
        String stanceName = switch (companion.followMode) {
            default -> throw new MatchException(null, null);
            case FollowMode.FOLLOW -> "Follow";
            case FollowMode.STAY -> "Stay";
            case FollowMode.PATROL -> "Patrol";
            case FollowMode.FREE -> "Free";
        };
        context.sendMessage(Message.raw((String)(companion.getDisplayName() + ": " + roleName + " + " + stanceName)));
    }

    private void setStance(CommandContext context, PlayerRef playerRef, FollowMode stance, String[] parts) {
        CompanionRecord companion = this.resolveTarget(context, playerRef, parts, 2);
        if (companion == null) {
            return;
        }
        this.companionSystem.applyStance(playerRef, companion, stance);
        this.save(context);
        String roleName = switch (companion.mode) {
            default -> throw new MatchException(null, null);
            case CompanionMode.FIGHTER -> "Fighter";
            case CompanionMode.FARMER -> "Farmer";
            case CompanionMode.MINER -> "Miner";
        };
        String stanceName = switch (stance) {
            default -> throw new MatchException(null, null);
            case FollowMode.FOLLOW -> "Follow";
            case FollowMode.STAY -> "Stay";
            case FollowMode.PATROL -> "Patrol";
            case FollowMode.FREE -> "Free";
        };
        context.sendMessage(Message.raw((String)(companion.getDisplayName() + ": " + roleName + " + " + stanceName)));
    }

    private void requestDeposit(CommandContext context, PlayerRef playerRef) {
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        CompanionRecord selected = data.resolveSelectedOrOnly();
        if (selected == null || !selected.active || selected.fallen) {
            context.sendMessage(Message.raw((String)"No active selected companion for deposit."));
            return;
        }
        CompanionRuntimeState runtime = this.companionManager.getRuntime(selected.uniqueId);
        runtime.depositRequested = true;
        context.sendMessage(Message.raw((String)("Deposit requested for " + selected.getDisplayName() + ".")));
    }

    private void showRoles(CommandContext context, boolean includeHidden) {
        List<String> roles = this.companionSystem.getAvailableRoleTemplates(includeHidden);
        if (roles.isEmpty()) {
            context.sendMessage(Message.raw((String)"No role templates exposed."));
            return;
        }
        String label = includeHidden ? "all role templates" : "public role templates";
        context.sendMessage(Message.raw((String)("Exposed " + label + " (" + roles.size() + "):")));
        for (String role : roles) {
            context.sendMessage(Message.raw((String)(" - " + role)));
        }
    }

    private void showModels(CommandContext context, String[] parts) {
        String filter = parts.length > 2 ? parts[2] : "";
        List<String> models = this.companionSystem.getAvailableModelIds(filter, true, this.companionSystem.getDefaultModelListLimit());
        if (models.isEmpty()) {
            context.sendMessage(Message.raw((String)("No human-like model assets found for filter: " + (filter.isBlank() ? "<none>" : filter))));
            return;
        }
        context.sendMessage(Message.raw((String)("Human-like model assets (" + models.size() + " shown):")));
        for (String modelId : models) {
            context.sendMessage(Message.raw((String)(" - " + modelId)));
        }
    }

    private void setAppearance(CommandContext context, PlayerRef playerRef, String[] parts) {
        if (parts.length < 3) {
            context.sendMessage(Message.raw((String)"Usage: /companion appearance <modelId>"));
            return;
        }
        CompanionRecord companion = this.resolveTarget(context, playerRef, new String[0], -1);
        if (companion == null) {
            return;
        }
        String modelId = parts[2];
        boolean ok = this.companionSystem.setPreferredAppearanceModel(playerRef, companion, modelId);
        if (!ok) {
            context.sendMessage(Message.raw((String)("Unknown model asset id: " + modelId)));
            return;
        }
        this.save(context);
        context.sendMessage(Message.raw((String)(companion.getDisplayName() + " appearance set to " + modelId)));
    }

    private void clearAppearance(CommandContext context, PlayerRef playerRef, String[] parts) {
        CompanionRecord companion = this.resolveTarget(context, playerRef, parts, 2);
        if (companion == null) {
            return;
        }
        this.companionSystem.clearPreferredAppearanceModel(playerRef, companion);
        this.save(context);
        context.sendMessage(Message.raw((String)(companion.getDisplayName() + " appearance override cleared.")));
    }

    private void rerollAppearance(CommandContext context, PlayerRef playerRef, String[] parts) {
        CompanionRecord companion = this.resolveTarget(context, playerRef, parts, 2);
        if (companion == null) {
            return;
        }
        if (!companion.active) {
            context.sendMessage(Message.raw((String)"Companion not active. Use /companion summon."));
            return;
        }
        String modelId = this.companionSystem.rerollAppearance(playerRef, companion);
        if (modelId == null) {
            context.sendMessage(Message.raw((String)"Failed to reroll appearance."));
            return;
        }
        context.sendMessage(Message.raw((String)(companion.getDisplayName() + " appearance reapplied: " + modelId)));
    }

    private void linkChest(CommandContext context, PlayerRef playerRef) {
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        CompanionRecord selected = data.resolveSelectedOrOnly();
        if (selected == null) {
            context.sendMessage(Message.raw((String)"No selected companion."));
            return;
        }
        if (selected.linkedChests.size() >= 10) {
            context.sendMessage(Message.raw((String)"Linked chest limit reached (10)."));
            return;
        }
        World world = playerRef.getWorldUuid() != null ? Universe.get().getWorld(playerRef.getWorldUuid()) : null;
        try {
            LOGGER.info("[ChestLinkDiag] command-link " + WorldQueries.describeStorageLookResolution(world, playerRef, 5.0));
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        BlockPos lookedAt = WorldQueries.getLookedAtStorageBlockPos(world, playerRef, 5.0);
        if (lookedAt == null) {
            context.sendMessage(Message.raw((String)"Could not find a storage block to link."));
            return;
        }
        if (selected.linkedChests.contains(lookedAt)) {
            context.sendMessage(Message.raw((String)"That chest is already linked."));
            return;
        }
        selected.linkedChests.add(lookedAt);
        this.save(context);
        context.sendMessage(Message.raw((String)(selected.getDisplayName() + " linked chest at " + this.formatBlockPos(lookedAt))));
    }

    private void unlinkChest(CommandContext context, PlayerRef playerRef) {
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        CompanionRecord selected = data.resolveSelectedOrOnly();
        if (selected == null) {
            context.sendMessage(Message.raw((String)"No selected companion."));
            return;
        }
        World world = playerRef.getWorldUuid() != null ? Universe.get().getWorld(playerRef.getWorldUuid()) : null;
        try {
            LOGGER.info("[ChestLinkDiag] command-unlink " + WorldQueries.describeStorageLookResolution(world, playerRef, 5.0));
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        BlockPos lookedAt = WorldQueries.getLookedAtStorageBlockPos(world, playerRef, 5.0);
        if (lookedAt == null) {
            context.sendMessage(Message.raw((String)"Could not find a storage block to unlink."));
            return;
        }
        boolean removed = selected.linkedChests.remove(lookedAt);
        if (!removed) {
            context.sendMessage(Message.raw((String)"That chest is not linked."));
            return;
        }
        this.save(context);
        context.sendMessage(Message.raw((String)(selected.getDisplayName() + " unlinked chest at " + this.formatBlockPos(lookedAt))));
    }

    private void giveAll(CommandContext context, PlayerRef playerRef, String[] parts) {
        CompanionRecord companion = this.resolveTarget(context, playerRef, parts, 2);
        if (companion == null) {
            return;
        }
        if (!companion.active) {
            context.sendMessage(Message.raw((String)"Companion not active."));
            return;
        }
        int count = this.companionSystem.giveAllItems(playerRef, companion);
        context.sendMessage(Message.raw((String)(count > 0 ? "Gave " + count + " item stack(s) to " + companion.getDisplayName() + "." : "No items to give or companion inventory full.")));
    }

    private void takeAll(CommandContext context, PlayerRef playerRef, String[] parts) {
        CompanionRecord companion = this.resolveTarget(context, playerRef, parts, 2);
        if (companion == null) {
            return;
        }
        if (!companion.active) {
            context.sendMessage(Message.raw((String)"Companion not active."));
            return;
        }
        int count = this.companionSystem.takeAllItems(playerRef, companion);
        context.sendMessage(Message.raw((String)(count > 0 ? "Took " + count + " item stack(s) from " + companion.getDisplayName() + "." : "Companion has no items to take.")));
    }

    private void giveHand(CommandContext context, PlayerRef playerRef, String[] parts) {
        CompanionRecord companion = this.resolveTarget(context, playerRef, parts, 2);
        if (companion == null) {
            return;
        }
        boolean ok = this.companionSystem.giveHandItem(playerRef, companion);
        if (ok) {
            context.sendMessage(Message.raw((String)("Item transferred to " + companion.getDisplayName() + ".")));
        } else {
            context.sendMessage(Message.raw((String)"Could not transfer item. Inventory may be full."));
        }
    }

    private void takeHand(CommandContext context, PlayerRef playerRef, String[] parts) {
        CompanionRecord companion = this.resolveTarget(context, playerRef, parts, 2);
        if (companion == null) {
            return;
        }
        boolean ok = this.companionSystem.takeHandItem(playerRef, companion);
        if (ok) {
            context.sendMessage(Message.raw((String)("Item taken from " + companion.getDisplayName() + ".")));
        } else {
            context.sendMessage(Message.raw((String)"Could not take item."));
        }
    }

    private void equipHeldItem(CommandContext context, PlayerRef playerRef, String[] parts) {
        CompanionRecord companion = this.resolveTarget(context, playerRef, parts, 2);
        if (companion == null) {
            return;
        }
        if (!companion.active) {
            context.sendMessage(Message.raw((String)"Companion not active. Use /companion summon."));
            return;
        }
        String type = this.companionSystem.equipHeldItemOnCompanion(playerRef, companion);
        if (type == null) {
            context.sendMessage(Message.raw((String)"That item cannot be equipped (must be armor or weapon)."));
            return;
        }
        String slotDesc = type.equals("armor") ? "armor slot" : "weapon slot";
        context.sendMessage(Message.raw((String)("Equipped item on " + companion.getDisplayName() + " (" + slotDesc + ").")));
    }

    private void farmTest(CommandContext context, PlayerRef playerRef, World world) {
        context.sendMessage(Message.raw((String)"=== Farm Test: Scanning blocks nearby ==="));
        Vector3d pos = playerRef.getTransform().getPosition();
        if (pos == null) {
            context.sendMessage(Message.raw((String)"Could not get player position."));
            return;
        }
        int cx = (int)Math.floor(pos.x);
        int cy = (int)Math.floor(pos.y);
        int cz = (int)Math.floor(pos.z);
        context.sendMessage(Message.raw((String)("Player at (" + cx + ", " + cy + ", " + cz + ")")));
        List<String> blocks = WorldQueries.debugScanBlocks(world, cx, cy, cz, 5);
        if (blocks.isEmpty()) {
            context.sendMessage(Message.raw((String)"No non-air blocks found within 5 blocks."));
            return;
        }
        context.sendMessage(Message.raw((String)("Found " + blocks.size() + " blocks:")));
        for (String info : blocks) {
            context.sendMessage(Message.raw((String)("  " + info)));
        }
        int cropCount = 0;
        for (int dx = -5; dx <= 5; ++dx) {
            for (int dz = -5; dz <= 5; ++dz) {
                for (int dy = -2; dy <= 2; ++dy) {
                    if (!WorldQueries.isHarvestableCrop(world, cx + dx, cy + dy, cz + dz) || ++cropCount > 5) continue;
                    context.sendMessage(Message.raw((String)("  CROP at (" + (cx + dx) + "," + (cy + dy) + "," + (cz + dz) + ")")));
                }
            }
        }
        context.sendMessage(Message.raw((String)("Total harvestable crops detected: " + cropCount)));
    }

    private void sendHelp(CommandContext context) {
        context.sendMessage(Message.raw((String)"=== Companion Commands ==="));
        context.sendMessage(Message.raw((String)"-- Roster --"));
        context.sendMessage(Message.raw((String)"/companion create [name] \u2014 create a new companion"));
        context.sendMessage(Message.raw((String)"/companion list \u2014 show all your companions"));
        context.sendMessage(Message.raw((String)"/companion select <name|#> \u2014 select companion for commands"));
        context.sendMessage(Message.raw((String)"/companion summon [name|all] \u2014 spawn companion(s)"));
        context.sendMessage(Message.raw((String)"/companion dismiss [name|all] \u2014 despawn companion(s)"));
        context.sendMessage(Message.raw((String)"-- Role (what they do) --"));
        context.sendMessage(Message.raw((String)"/companion fighter [name] \u2014 set role to Fighter"));
        context.sendMessage(Message.raw((String)"/companion farmer [name] \u2014 set role to Farmer"));
        context.sendMessage(Message.raw((String)"/companion miner [name] \u2014 set role to Miner"));
        context.sendMessage(Message.raw((String)"-- Stance (where they are) --"));
        context.sendMessage(Message.raw((String)"/companion follow [name] \u2014 companion follows you"));
        context.sendMessage(Message.raw((String)"/companion stay [name] \u2014 companion holds position"));
        context.sendMessage(Message.raw((String)"/companion patrol [name] \u2014 wander near anchor (20 blocks)"));
        context.sendMessage(Message.raw((String)"/companion free [name] \u2014 no movement restrictions"));
        context.sendMessage(Message.raw((String)"-- Info --"));
        context.sendMessage(Message.raw((String)"/companion status [name] \u2014 show companion status"));
        context.sendMessage(Message.raw((String)"/companion debug [name] \u2014 show debug info"));
        context.sendMessage(Message.raw((String)"/companion progression [name] \u2014 show level details"));
        context.sendMessage(Message.raw((String)"/companion menu \u2014 open interactive text menu"));
        context.sendMessage(Message.raw((String)"/companion uimenu \u2014 open graphical UI menu"));
        context.sendMessage(Message.raw((String)"-- Items --"));
        context.sendMessage(Message.raw((String)"/companion rename <name> \u2014 rename selected companion"));
        context.sendMessage(Message.raw((String)"/companion chestlink \u2014 link looked-at chest"));
        context.sendMessage(Message.raw((String)"/companion chestunlink \u2014 unlink looked-at chest"));
        context.sendMessage(Message.raw((String)"/companion deposit \u2014 force deposit for all active"));
        context.sendMessage(Message.raw((String)"/companion givehand [name] \u2014 give held item"));
        context.sendMessage(Message.raw((String)"/companion takehand [name] \u2014 take item from companion"));
        context.sendMessage(Message.raw((String)"/companion giveall [name] \u2014 give all items to companion"));
        context.sendMessage(Message.raw((String)"/companion takeall [name] \u2014 take all items from companion"));
        context.sendMessage(Message.raw((String)"/companion equip [name] \u2014 equip held armor/weapon on companion"));
        context.sendMessage(Message.raw((String)"-- Revival --"));
        context.sendMessage(Message.raw((String)"/companion fallen \u2014 list fallen companions"));
        context.sendMessage(Message.raw((String)"/companion revive <#|name> \u2014 revive a fallen companion"));
        context.sendMessage(Message.raw((String)"-- Appearance --"));
        context.sendMessage(Message.raw((String)"/companion appearance <modelId> \u2014 set model"));
        context.sendMessage(Message.raw((String)"/companion rerollappearance [name] \u2014 randomize model"));
        context.sendMessage(Message.raw((String)"/companion roles|models \u2014 list available assets"));
    }

    private void save(CommandContext context) {
        try {
            this.companionManager.save();
        }
        catch (IOException ex) {
            context.sendMessage(Message.raw((String)"Failed to save companion data."));
        }
    }

    private void save(PlayerRef playerRef) {
        try {
            this.companionManager.save();
        }
        catch (IOException ex) {
            playerRef.sendMessage(Message.raw((String)"Failed to save companion data."));
        }
    }

    private String formatBlockPos(BlockPos pos) {
        return pos.worldId + " (" + pos.x + ", " + pos.y + ", " + pos.z + ")";
    }
}

