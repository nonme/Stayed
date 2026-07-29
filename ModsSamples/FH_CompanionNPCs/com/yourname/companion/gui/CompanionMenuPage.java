/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
 *  com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
 *  com.hypixel.hytale.server.core.ui.builder.EventData
 *  com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
 *  com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.Universe
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.yourname.companion.gui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
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
import com.yourname.companion.gui.CompanionMenuEventData;
import com.yourname.companion.runtime.CompanionRuntimeState;
import com.yourname.companion.storage.CompanionManager;
import com.yourname.companion.systems.CompanionSystem;
import com.yourname.companion.util.WorldQueries;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class CompanionMenuPage
extends InteractiveCustomUIPage<CompanionMenuEventData> {
    private static final long UI_REFRESH_MS = 400L;
    private static final long FEEDBACK_HOLD_MS = 3500L;
    private final CompanionSystem companionSystem;
    private final CompanionManager companionManager;
    private ScheduledExecutorService refreshExecutor;
    private volatile boolean refreshClosed;
    private volatile String lastRefreshSnapshot = "";
    private volatile String feedbackText = "";
    private volatile long feedbackExpiresAtMs;

    public CompanionMenuPage(PlayerRef playerRef, CompanionSystem companionSystem, CompanionManager companionManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, CompanionMenuEventData.CODEC);
        this.companionSystem = companionSystem;
        this.companionManager = companionManager;
    }

    public void build(Ref<EntityStore> ref, UICommandBuilder commands, UIEventBuilder events, Store<EntityStore> store) {
        commands.append("Pages/Companion_Main.ui");
        this.writeStateLabels(commands, this.currentFeedback());
        this.startAutoRefresh();
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CompanionFollowBtn", EventData.of((String)"Action", (String)"follow"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CompanionStayBtn", EventData.of((String)"Action", (String)"stay"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CompanionPatrolBtn", EventData.of((String)"Action", (String)"patrol"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CompanionFreeBtn", EventData.of((String)"Action", (String)"free"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CompanionFightBtn", EventData.of((String)"Action", (String)"fight"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CompanionFarmBtn", EventData.of((String)"Action", (String)"farm"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CompanionMinerBtn", EventData.of((String)"Action", (String)"miner"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CompanionGiveBtn", EventData.of((String)"Action", (String)"give"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CompanionTakeBtn", EventData.of((String)"Action", (String)"take"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CompanionEquipBtn", EventData.of((String)"Action", (String)"equip"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CompanionGiveAllBtn", EventData.of((String)"Action", (String)"giveall"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CompanionSummonBtn", EventData.of((String)"Action", (String)"summon"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CompanionDismissBtn", EventData.of((String)"Action", (String)"dismiss"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CompanionReviveBtn", EventData.of((String)"Action", (String)"revive"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CompanionRenameBtn", EventData.of((String)"Action", (String)"rename"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#Cmd1Btn", EventData.of((String)"Action", (String)"cmd_1"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#Cmd2Btn", EventData.of((String)"Action", (String)"cmd_2"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#Cmd3Btn", EventData.of((String)"Action", (String)"cmd_3"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CompanionPrevBtn", EventData.of((String)"Action", (String)"prev"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CompanionNextBtn", EventData.of((String)"Action", (String)"next"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CompanionCloseBtn", EventData.of((String)"Action", (String)"close"), false);
    }

    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, CompanionMenuEventData eventData) {
        String action;
        super.handleDataEvent(ref, store, (Object)eventData);
        if (eventData == null || eventData.action == null || eventData.action.isBlank()) {
            return;
        }
        PlayerCompanionData pData = this.companionManager.getOrCreate(this.playerRef.getUuid());
        CompanionRecord companion = this.resolveCompanion(pData);
        Object feedback = null;
        switch (action = eventData.action.toLowerCase()) {
            case "prev": 
            case "next": {
                if (pData.companions.isEmpty()) {
                    feedback = "No companions.";
                    break;
                }
                if (pData.companions.size() == 1) {
                    feedback = "Only one companion.";
                    break;
                }
                int currentIdx = 0;
                for (int i = 0; i < pData.companions.size(); ++i) {
                    if (companion == null || !pData.companions.get((int)i).uniqueId.equals(companion.uniqueId)) continue;
                    currentIdx = i;
                    break;
                }
                int newIdx = action.equals("prev") ? (currentIdx - 1 + pData.companions.size()) % pData.companions.size() : (currentIdx + 1) % pData.companions.size();
                CompanionRecord selected = pData.companions.get(newIdx);
                pData.selectedCompanionId = selected.uniqueId;
                companion = selected;
                feedback = "Selected: " + selected.getDisplayName() + " (" + (newIdx + 1) + "/" + pData.companions.size() + ")";
                break;
            }
            case "follow": {
                if (companion == null) {
                    feedback = "No companion selected.";
                    break;
                }
                this.companionSystem.applyStance(this.playerRef, companion, FollowMode.FOLLOW);
                this.saveSilently();
                feedback = companion.getDisplayName() + " set to Follow.";
                break;
            }
            case "stay": {
                if (companion == null) {
                    feedback = "No companion selected.";
                    break;
                }
                this.companionSystem.applyStance(this.playerRef, companion, FollowMode.STAY);
                this.saveSilently();
                feedback = companion.getDisplayName() + " set to Stay.";
                break;
            }
            case "patrol": {
                if (companion == null) {
                    feedback = "No companion selected.";
                    break;
                }
                this.companionSystem.applyStance(this.playerRef, companion, FollowMode.PATROL);
                this.saveSilently();
                feedback = companion.getDisplayName() + " set to Patrol.";
                break;
            }
            case "free": {
                if (companion == null) {
                    feedback = "No companion selected.";
                    break;
                }
                this.companionSystem.applyStance(this.playerRef, companion, FollowMode.FREE);
                this.saveSilently();
                feedback = companion.getDisplayName() + " set to Free.";
                break;
            }
            case "fight": {
                if (companion == null) {
                    feedback = "No companion selected.";
                    break;
                }
                companion.mode = CompanionMode.FIGHTER;
                companion.farmAutoResume = false;
                this.companionManager.getRuntime((String)companion.uniqueId).commandActive = false;
                if (companion.active) {
                    String eq = this.companionSystem.switchRoleEquipment(this.playerRef, companion);
                    feedback = companion.getDisplayName() + " set to Fighter." + (String)(eq != null ? " " + eq : "");
                } else {
                    feedback = companion.getDisplayName() + " set to Fighter.";
                }
                this.saveSilently();
                break;
            }
            case "farm": {
                if (companion == null) {
                    feedback = "No companion selected.";
                    break;
                }
                companion.mode = CompanionMode.FARMER;
                if (companion.active) {
                    String eq = this.companionSystem.switchRoleEquipment(this.playerRef, companion);
                    feedback = companion.getDisplayName() + " set to Farmer." + (String)(eq != null ? " " + eq : "");
                } else {
                    feedback = companion.getDisplayName() + " set to Farmer.";
                }
                this.saveSilently();
                break;
            }
            case "miner": {
                if (companion == null) {
                    feedback = "No companion selected.";
                    break;
                }
                companion.mode = CompanionMode.MINER;
                companion.farmAutoResume = false;
                this.companionManager.getRuntime((String)companion.uniqueId).commandActive = false;
                if (companion.active) {
                    String eq = this.companionSystem.switchRoleEquipment(this.playerRef, companion);
                    feedback = companion.getDisplayName() + " set to Miner." + (String)(eq != null ? " " + eq : "");
                } else {
                    feedback = companion.getDisplayName() + " set to Miner.";
                }
                this.saveSilently();
                break;
            }
            case "give": {
                if (companion == null) {
                    feedback = "No companion selected.";
                    break;
                }
                if (!companion.active) {
                    feedback = "Companion not active.";
                    break;
                }
                boolean gaveOk = this.companionSystem.giveHandItem(this.playerRef, companion);
                feedback = gaveOk ? "Item given to " + companion.getDisplayName() + "." : "Could not give item. Hand empty or inventory full.";
                break;
            }
            case "take": {
                if (companion == null) {
                    feedback = "No companion selected.";
                    break;
                }
                if (!companion.active) {
                    feedback = "Companion not active.";
                    break;
                }
                boolean tookOk = this.companionSystem.takeHandItem(this.playerRef, companion);
                feedback = tookOk ? "Item taken from " + companion.getDisplayName() + "." : "Could not take item.";
                break;
            }
            case "equip": {
                if (companion == null) {
                    feedback = "No companion selected.";
                    break;
                }
                if (!companion.active) {
                    feedback = "Companion not active.";
                    break;
                }
                String equipType = this.companionSystem.equipHeldItemOnCompanion(this.playerRef, companion);
                if (equipType == null) {
                    feedback = "Hold an armor or weapon item first.";
                    break;
                }
                feedback = "Equipped " + equipType + " on " + companion.getDisplayName() + ".";
                break;
            }
            case "giveall": {
                if (companion == null) {
                    feedback = "No companion selected.";
                    break;
                }
                if (!companion.active) {
                    feedback = "Companion not active.";
                    break;
                }
                int giveCount = this.companionSystem.giveAllItems(this.playerRef, companion);
                feedback = giveCount > 0 ? "Gave " + giveCount + " item stack(s) to " + companion.getDisplayName() + "." : "No items to give or inventory full.";
                break;
            }
            case "summon": {
                if (companion == null) {
                    feedback = "No companion selected.";
                    break;
                }
                if (companion.active) {
                    feedback = companion.getDisplayName() + " is already active.";
                    break;
                }
                if (companion.fallen) {
                    feedback = companion.getDisplayName() + " is fallen. Use Revive.";
                    break;
                }
                boolean summonOk = this.companionSystem.summonCompanion(this.playerRef, companion);
                this.saveSilently();
                feedback = summonOk ? companion.getDisplayName() + " summoned." : "Failed to summon.";
                break;
            }
            case "dismiss": {
                if (companion == null) {
                    feedback = "No companion selected.";
                    break;
                }
                if (!companion.active) {
                    feedback = "Companion not active.";
                    break;
                }
                this.companionSystem.dismissCompanion(this.playerRef, companion);
                this.saveSilently();
                feedback = companion.getDisplayName() + " dismissed.";
                break;
            }
            case "revive": {
                List<CompanionRecord> fallen = pData.getFallenCompanions();
                if (fallen.isEmpty()) {
                    feedback = "No fallen companions to revive.";
                    break;
                }
                CompanionRecord toRevive = fallen.get(0);
                String reviveError = this.companionSystem.tryReviveCompanion(this.playerRef, toRevive, true);
                if (reviveError != null) {
                    feedback = reviveError;
                    break;
                }
                this.saveSilently();
                feedback = toRevive.getDisplayName() + " revived!";
                break;
            }
            case "reroll": {
                if (companion == null) {
                    feedback = "No companion selected.";
                    break;
                }
                if (!companion.active) {
                    feedback = "Companion not active.";
                    break;
                }
                String modelId = this.companionSystem.rerollAppearance(this.playerRef, companion);
                feedback = modelId != null ? "Appearance rerolled." : "Failed to reroll appearance.";
                break;
            }
            case "rename": {
                if (companion == null) {
                    feedback = "No companion selected.";
                    break;
                }
                this.playerRef.sendMessage(Message.raw((String)"Type: /companion rename <new name>"));
                this.playerRef.sendMessage(Message.raw((String)("Current name: " + companion.getDisplayName())));
                this.close();
                return;
            }
            case "cmd_1": 
            case "cmd_2": 
            case "cmd_3": {
                if (companion == null) {
                    feedback = "No companion selected.";
                    break;
                }
                if (!companion.active) {
                    feedback = "Companion not active.";
                    break;
                }
                feedback = this.handleRoleCommand(action, companion, pData);
                break;
            }
            case "close": {
                this.close();
                return;
            }
            default: {
                feedback = "Unknown action: " + action;
            }
        }
        this.updateFeedback((String)feedback);
        UICommandBuilder update = new UICommandBuilder();
        this.writeStateLabels(update, this.currentFeedback());
        this.sendUpdate(update);
    }

    public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) {
        this.stopAutoRefresh();
        super.onDismiss(ref, store);
    }

    private CompanionRecord resolveCompanion(PlayerCompanionData pData) {
        CompanionRecord c = pData.getSelectedCompanion();
        if (c != null) {
            return c;
        }
        if (pData.companions.size() == 1) {
            return pData.companions.get(0);
        }
        List<CompanionRecord> actives = pData.getActiveCompanions();
        if (!actives.isEmpty()) {
            return actives.get(0);
        }
        if (!pData.companions.isEmpty()) {
            return pData.companions.get(0);
        }
        return null;
    }

    private void writeStateLabels(UICommandBuilder commands, String feedback) {
        PlayerCompanionData pData = this.companionManager.getOrCreate(this.playerRef.getUuid());
        CompanionRecord companion = this.resolveCompanion(pData);
        if (companion != null) {
            CompanionRuntimeState runtime = this.companionManager.getRuntime(companion.uniqueId);
            commands.set("#CompanionNameValue.Text", companion.getDisplayName());
            String status = companion.fallen ? "FALLEN" : (companion.active ? "ACTIVE" : "DORMANT");
            commands.set("#CompanionEntityValue.Text", status);
            String roleName = switch (companion.mode) {
                default -> throw new MatchException(null, null);
                case CompanionMode.FIGHTER -> "Fighter";
                case CompanionMode.FARMER -> "Farmer";
                case CompanionMode.MINER -> "Miner";
            };
            commands.set("#CompanionModeValue.Text", roleName);
            String stanceName = switch (companion.followMode) {
                default -> throw new MatchException(null, null);
                case FollowMode.FOLLOW -> "Follow";
                case FollowMode.STAY -> "Stay";
                case FollowMode.PATROL -> "Patrol";
                case FollowMode.FREE -> "Free";
            };
            commands.set("#CompanionStanceValue.Text", stanceName);
            int combatIdx = Math.max(0, Math.min(companion.combatLevel - 1, ProgressionConfig.COMBAT_LEVEL_NAMES.length - 1));
            commands.set("#CompanionCombatValue.Text", "Lv" + companion.combatLevel + " (" + ProgressionConfig.COMBAT_LEVEL_NAMES[combatIdx] + ") - " + companion.combatKills + " kills");
            int farmIdx = Math.max(0, Math.min(companion.farmLevel - 1, ProgressionConfig.FARM_LEVEL_NAMES.length - 1));
            commands.set("#CompanionFarmValue.Text", "Lv" + companion.farmLevel + " (" + ProgressionConfig.FARM_LEVEL_NAMES[farmIdx] + ") - " + companion.farmHarvests + " harvests");
            int mineIdx = Math.max(0, Math.min(companion.mineLevel - 1, ProgressionConfig.MINE_LEVEL_NAMES.length - 1));
            commands.set("#CompanionMineValue.Text", "Lv" + companion.mineLevel + " (" + ProgressionConfig.MINE_LEVEL_NAMES[mineIdx] + ") - " + companion.mineBlocks + " blocks");
            if (companion.active) {
                String[] eqStats = this.companionSystem.getEquipmentStats(this.playerRef, companion);
                commands.set("#CompanionWeaponValue.Text", eqStats[0] + " (Atk: " + eqStats[2] + ")");
                commands.set("#CompanionArmorValue.Text", eqStats[1] + " (Def: " + eqStats[3] + ")");
            } else {
                commands.set("#CompanionWeaponValue.Text", "--");
                commands.set("#CompanionArmorValue.Text", "--");
            }
            switch (companion.mode) {
                case FIGHTER: {
                    commands.set("#Cmd1Btn.Text", "Fight");
                    commands.set("#Cmd2Btn.Text", "Max Defend");
                    commands.set("#Cmd3Btn.Text", companion.lootModeEnabled ? "Loot: ON" : "Loot: OFF");
                    commands.set("#CommandStatusValue.Text", "");
                    break;
                }
                case FARMER: {
                    commands.set("#Cmd1Btn.Text", "Farm");
                    commands.set("#Cmd2Btn.Text", "Set TL");
                    commands.set("#Cmd3Btn.Text", "Set BR");
                    commands.set("#CommandStatusValue.Text", this.formatFarmCommandStatus(runtime));
                    break;
                }
                case MINER: {
                    commands.set("#Cmd1Btn.Text", "Mine");
                    commands.set("#Cmd2Btn.Text", "Stop Mining");
                    commands.set("#Cmd3Btn.Text", "Mine For This");
                    commands.set("#CommandStatusValue.Text", this.formatMineCommandStatus(companion, runtime));
                }
            }
            this.populateInventorySlots(commands, companion);
        } else {
            commands.set("#CompanionNameValue.Text", "None selected");
            commands.set("#CompanionEntityValue.Text", "--");
            commands.set("#CompanionModeValue.Text", "--");
            commands.set("#CompanionStanceValue.Text", "--");
            commands.set("#CompanionWeaponValue.Text", "--");
            commands.set("#CompanionArmorValue.Text", "--");
            commands.set("#CompanionCombatValue.Text", "--");
            commands.set("#CompanionFarmValue.Text", "--");
            commands.set("#CompanionMineValue.Text", "--");
            commands.set("#Cmd1Btn.Text", "Fight");
            commands.set("#Cmd2Btn.Text", "Max Defend");
            commands.set("#Cmd3Btn.Text", "Loot: OFF");
            commands.set("#CommandStatusValue.Text", "");
            for (int i = 0; i < 8; ++i) {
                commands.set("#InvSlot" + i + ".Text", "--");
            }
        }
        commands.set("#CompanionFeedback.Text", feedback != null ? feedback : "");
    }

    private void startAutoRefresh() {
        if (this.refreshExecutor != null) {
            return;
        }
        this.refreshClosed = false;
        this.refreshExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "companion-menu-refresh-" + String.valueOf(this.playerRef.getUuid()));
            thread.setDaemon(true);
            return thread;
        });
        this.refreshExecutor.scheduleAtFixedRate(this::pushAutoRefresh, 400L, 400L, TimeUnit.MILLISECONDS);
    }

    private void stopAutoRefresh() {
        this.refreshClosed = true;
        ScheduledExecutorService executor = this.refreshExecutor;
        this.refreshExecutor = null;
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void pushAutoRefresh() {
        if (this.refreshClosed) {
            return;
        }
        String snapshot = this.buildRefreshSnapshot();
        if (Objects.equals(snapshot, this.lastRefreshSnapshot)) {
            return;
        }
        this.lastRefreshSnapshot = snapshot;
        UICommandBuilder update = new UICommandBuilder();
        this.writeStateLabels(update, this.currentFeedback());
        try {
            this.sendUpdate(update);
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    private String buildRefreshSnapshot() {
        PlayerCompanionData pData = this.companionManager.getOrCreate(this.playerRef.getUuid());
        CompanionRecord companion = this.resolveCompanion(pData);
        if (companion == null) {
            return "none|" + this.currentFeedback();
        }
        CompanionRuntimeState runtime = this.companionManager.getRuntime(companion.uniqueId);
        return String.join((CharSequence)"|", companion.uniqueId, companion.getDisplayName(), String.valueOf(companion.active), String.valueOf(companion.fallen), String.valueOf((Object)companion.mode), String.valueOf((Object)companion.followMode), String.valueOf(companion.combatLevel), String.valueOf(companion.combatKills), String.valueOf(companion.farmLevel), String.valueOf(companion.farmHarvests), String.valueOf(companion.mineLevel), String.valueOf(companion.mineBlocks), runtime != null ? String.valueOf(runtime.farmStatusText) : "", runtime != null ? String.valueOf(runtime.mineStatusText) : "", runtime != null ? String.valueOf(runtime.mineTargetBlockId) : "", this.currentFeedback());
    }

    private void updateFeedback(String feedback) {
        if (feedback == null || feedback.isBlank()) {
            return;
        }
        this.feedbackText = feedback;
        this.feedbackExpiresAtMs = System.currentTimeMillis() + 3500L;
        this.lastRefreshSnapshot = "";
    }

    private String currentFeedback() {
        String current = this.feedbackText;
        if (current == null || current.isBlank()) {
            return "";
        }
        if (System.currentTimeMillis() > this.feedbackExpiresAtMs) {
            this.feedbackText = "";
            return "";
        }
        return current;
    }

    private String formatFarmCommandStatus(CompanionRuntimeState runtime) {
        if (runtime == null) {
            return "Farm Status: Inactive";
        }
        String status = runtime.farmStatusText;
        if (status == null || status.isBlank()) {
            status = runtime.commandActive ? "Waiting" : "Inactive";
        }
        return "Farm Status: " + status;
    }

    private String formatMineCommandStatus(CompanionRecord companion, CompanionRuntimeState runtime) {
        boolean hasStatus;
        if (companion == null && runtime == null) {
            return "Mining Status: Inactive";
        }
        String target = runtime != null ? runtime.mineTargetBlockId : null;
        String status = runtime != null ? runtime.mineStatusText : null;
        boolean hasTarget = target != null && !target.isBlank();
        boolean bl = hasStatus = status != null && !status.isBlank();
        if (!hasTarget && !hasStatus) {
            String fallback = runtime != null && runtime.miningActive ? "Waiting" : "Inactive";
            return "Mining Status: " + fallback;
        }
        if (hasTarget && hasStatus) {
            return "Mining For: " + target + " | Status: " + status;
        }
        if (hasTarget) {
            return "Mining For: " + target;
        }
        return "Mining Status: " + status;
    }

    private String handleRoleCommand(String action, CompanionRecord companion, PlayerCompanionData pData) {
        return switch (companion.mode) {
            default -> throw new MatchException(null, null);
            case CompanionMode.FIGHTER -> this.handleFighterCmd(action, companion);
            case CompanionMode.FARMER -> this.handleFarmerCmd(action, companion, pData);
            case CompanionMode.MINER -> this.handleMinerCmd(action, companion);
        };
    }

    private String handleFighterCmd(String action, CompanionRecord companion) {
        CompanionRuntimeState rt = this.companionManager.getRuntime(companion.uniqueId);
        PlayerCompanionData pData = this.companionManager.getOrCreate(this.playerRef.getUuid());
        return switch (action) {
            case "cmd_1" -> {
                this.companionSystem.prepareIssuedCommand(this.playerRef, companion);
                rt.maxDefendMode = false;
                rt.commandActive = true;
                yield companion.getDisplayName() + " set to Attack mode.";
            }
            case "cmd_2" -> {
                this.companionSystem.prepareIssuedCommand(this.playerRef, companion);
                rt.maxDefendMode = true;
                rt.commandActive = true;
                yield companion.getDisplayName() + " set to Max Defend mode.";
            }
            case "cmd_3" -> {
                boolean v1 = companion.lootModeEnabled = !companion.lootModeEnabled;
                if (!companion.lootModeEnabled) {
                    this.companionSystem.clearLootState(this.playerRef, companion);
                }
                this.saveSilently();
                yield companion.getDisplayName() + " loot pickup " + (companion.lootModeEnabled ? "enabled." : "disabled.");
            }
            default -> "Unknown command.";
        };
    }

    private String handleFarmerCmd(String action, CompanionRecord companion, PlayerCompanionData pData) {
        CompanionRuntimeState rt = this.companionManager.getRuntime(companion.uniqueId);
        return switch (action) {
            case "cmd_1" -> {
                this.companionSystem.prepareIssuedCommand(this.playerRef, companion);
                rt.commandActive = true;
                companion.farmAutoResume = true;
                this.companionSystem.suspendOwnerFollowTargetingForActiveWork(this.playerRef, companion);
                this.saveSilently();
                yield companion.getDisplayName() + " farming mode active. (Crop detection API pending)";
            }
            case "cmd_2" -> {
                BlockPos look = WorldQueries.getLookBlockPos(this.playerRef, 6.0);
                if (look == null) {
                    yield "Look at a block for farm TL.";
                }
                companion.farmAreaTopLeft = look;
                this.saveSilently();
                yield "Farm TL set to (" + look.x + ", " + look.y + ", " + look.z + ").";
            }
            case "cmd_3" -> {
                BlockPos look = WorldQueries.getLookBlockPos(this.playerRef, 6.0);
                if (look == null) {
                    yield "Look at a block for farm BR.";
                }
                companion.farmAreaBottomRight = look;
                this.saveSilently();
                yield "Farm BR set to (" + look.x + ", " + look.y + ", " + look.z + ").";
            }
            default -> "Unknown command.";
        };
    }

    private String handleMinerCmd(String action, CompanionRecord companion) {
        CompanionRuntimeState rt = this.companionManager.getRuntime(companion.uniqueId);
        return switch (action) {
            case "cmd_1" -> {
                BlockPos lookBlock;
                this.companionSystem.prepareIssuedCommand(this.playerRef, companion);
                World mineWorld = Universe.get().getWorld(this.playerRef.getWorldUuid());
                BlockPos v0 = lookBlock = mineWorld != null ? WorldQueries.getRobustLookBlockPos(mineWorld, this.playerRef, 20.0) : null;
                if (lookBlock == null) {
                    yield "Look at a block to mine.";
                }
                yield this.companionSystem.startDirectionalMine(this.playerRef, companion, lookBlock.x, lookBlock.y, lookBlock.z);
            }
            case "cmd_2" -> {
                this.companionSystem.prepareIssuedCommand(this.playerRef, companion);
                String msg = this.companionSystem.stopMining(this.playerRef, companion);
                this.companionSystem.stopCompanionMotionNow(this.playerRef, companion);
                yield msg;
            }
            case "cmd_3" -> {
                String blockType;
                BlockPos lookBlock;
                this.companionSystem.prepareIssuedCommand(this.playerRef, companion);
                World mineWorld = Universe.get().getWorld(this.playerRef.getWorldUuid());
                BlockPos v2 = lookBlock = mineWorld != null ? WorldQueries.getRobustLookBlockPos(mineWorld, this.playerRef, 20.0) : null;
                if (lookBlock == null) {
                    yield "Look directly at the block to mine for.";
                }
                String v3 = blockType = mineWorld != null ? WorldQueries.getBlockTypeAt(mineWorld, lookBlock.x, lookBlock.y, lookBlock.z) : null;
                if (blockType == null) {
                    yield "Could not identify block type.";
                }
                String canonicalMineFor = this.canonicalBlockIdForMine(blockType);
                if (canonicalMineFor == null || canonicalMineFor.isBlank() || this.isAirLikeMineTarget(canonicalMineFor)) {
                    yield "That block is not mineable (air/empty).";
                }
                yield this.companionSystem.startMineForBlock(this.playerRef, companion, canonicalMineFor, lookBlock.x, lookBlock.y, lookBlock.z);
            }
            default -> "Unknown command.";
        };
    }

    private String canonicalBlockIdForMine(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.trim();
        String lower = v.toLowerCase(Locale.ROOT);
        int idIdx = lower.indexOf("id=");
        if (idIdx >= 0) {
            int start = idIdx + 3;
            int end = v.indexOf(44, start);
            if (end < 0) {
                end = v.indexOf(125, start);
            }
            if (end > start) {
                v = v.substring(start, end).trim();
            }
        } else if (lower.startsWith("blockid:")) {
            v = v.substring("blockid:".length()).trim();
        }
        if (v.startsWith("*")) {
            v = v.substring(1).trim();
        }
        return v.isBlank() ? null : v;
    }

    private boolean isAirLikeMineTarget(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return true;
        }
        String v = blockId.toLowerCase(Locale.ROOT);
        return v.equals("air") || v.equals("empty") || v.endsWith(":air") || v.endsWith(":empty") || v.contains("id=empty") || v.contains("group='air'") || v.contains("drawtype=empty") || v.contains("material=empty") || v.contains(" blockid:0") || v.startsWith("blockid:0");
    }

    private void populateInventorySlots(UICommandBuilder commands, CompanionRecord companion) {
        Map<String, Integer> inv = null;
        if (companion.active) {
            inv = this.companionSystem.getLiveInventory(this.playerRef, companion);
        }
        if (inv == null || inv.isEmpty()) {
            inv = companion.savedInventory;
        }
        if (inv == null || inv.isEmpty()) {
            for (int i = 0; i < 8; ++i) {
                commands.set("#InvSlot" + i + ".Text", "--");
            }
            return;
        }
        int slot = 0;
        for (Map.Entry<String, Integer> entry : inv.entrySet()) {
            if (slot >= 8) break;
            String itemName = this.formatItemName(entry.getKey());
            commands.set("#InvSlot" + slot + ".Text", itemName + " x" + String.valueOf(entry.getValue()));
            ++slot;
        }
        while (slot < 8) {
            commands.set("#InvSlot" + slot + ".Text", "--");
            ++slot;
        }
    }

    private String formatItemName(String itemId) {
        if (itemId == null) {
            return "Unknown";
        }
        String cleaned = itemId.replace("Weapon_", "").replace("Tool_", "").replace("Armor_", "").replace("Seed_", "Seed: ");
        return cleaned.replace('_', ' ');
    }

    private void saveSilently() {
        try {
            this.companionManager.save();
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }
}

