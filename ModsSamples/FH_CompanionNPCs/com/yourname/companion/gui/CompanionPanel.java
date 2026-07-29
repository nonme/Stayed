/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.logger.HytaleLogger
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 *  com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.inventory.ItemStack
 *  com.hypixel.hytale.server.core.ui.ItemGridSlot
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.Universe
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  com.hypixel.hytale.server.npc.entities.NPCEntity
 */
package com.yourname.companion.gui;

import au.ellie.hyui.builders.ButtonBuilder;
import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.ItemGridBuilder;
import au.ellie.hyui.builders.LabelBuilder;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.builders.TextFieldBuilder;
import au.ellie.hyui.events.SlotClickingEventData;
import au.ellie.hyui.events.UIContext;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.yourname.companion.data.BlockPos;
import com.yourname.companion.data.CompanionMode;
import com.yourname.companion.data.CompanionRecord;
import com.yourname.companion.data.EquipmentSlot;
import com.yourname.companion.data.FollowMode;
import com.yourname.companion.data.PlayerCompanionData;
import com.yourname.companion.data.ProgressionConfig;
import com.yourname.companion.runtime.CompanionRuntimeState;
import com.yourname.companion.storage.CompanionManager;
import com.yourname.companion.systems.CompanionEquipmentManager;
import com.yourname.companion.systems.CompanionSystem;
import com.yourname.companion.util.WorldQueries;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class CompanionPanel {
    private static final String TAB_EQUIPMENT = "equipment";
    private static final String TAB_CONTROLS = "controls";
    private static final String TAB_STATS = "stats";
    private static final int MAX_LINKED_CHESTS = 10;
    private final CompanionManager companionManager;
    private final CompanionSystem companionSystem;
    private final CompanionEquipmentManager equipmentManager;
    private final HytaleLogger logger;
    private final Map<UUID, HyUIPage> openPages = new ConcurrentHashMap<UUID, HyUIPage>();
    private final Map<UUID, String> currentTab = new ConcurrentHashMap<UUID, String>();
    private final Map<UUID, List<String[]>> inventoryCache = new ConcurrentHashMap<UUID, List<String[]>>();
    private final Map<UUID, List<String[]>> companionInventoryCache = new ConcurrentHashMap<UUID, List<String[]>>();
    private final Map<UUID, Long> liveRefreshTokens = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, String> liveRefreshSnapshots = new ConcurrentHashMap<UUID, String>();
    private final Map<UUID, Long> interactionPauseUntilMs = new ConcurrentHashMap<UUID, Long>();
    private final Set<UUID> textEntryFocused = ConcurrentHashMap.newKeySet();

    public CompanionPanel(CompanionManager companionManager, CompanionSystem companionSystem, CompanionEquipmentManager equipmentManager, HytaleLogger logger) {
        this.companionManager = companionManager;
        this.companionSystem = companionSystem;
        this.equipmentManager = equipmentManager;
        this.logger = logger;
    }

    public boolean isOpen(UUID playerId) {
        return this.openPages.containsKey(playerId);
    }

    private void clearPanelState(UUID playerId, HyUIPage expectedPage) {
        if (playerId == null) {
            return;
        }
        if (expectedPage != null) {
            this.openPages.compute(playerId, (id, current) -> current == expectedPage ? null : current);
        } else {
            this.openPages.remove(playerId);
        }
        this.liveRefreshTokens.remove(playerId);
        this.liveRefreshSnapshots.remove(playerId);
        this.interactionPauseUntilMs.remove(playerId);
        this.textEntryFocused.remove(playerId);
        this.inventoryCache.remove(playerId);
        this.companionInventoryCache.remove(playerId);
    }

    public void closePanel(UUID playerId) {
        HyUIPage page = this.openPages.remove(playerId);
        this.clearPanelState(playerId, page);
        if (page != null) {
            this.closePageWithHoverClear(page, true);
        }
    }

    private void closePageWithHoverClear(HyUIPage page, boolean delayedClose) {
        if (page == null) {
            return;
        }
        try {
            this.clearHoverText(page);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            page.close();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private void clearHoverText(HyUIPage page) {
        this.clearGridHover(page, "companion-inv-grid");
        this.clearGridHover(page, "player-inventory-grid");
        this.clearGridHover(page, "equip-slot-head");
        this.clearGridHover(page, "equip-slot-chest");
        this.clearGridHover(page, "equip-slot-legs");
        this.clearGridHover(page, "equip-slot-gloves");
        this.clearGridHover(page, "equip-slot-weapon");
        this.clearGridHover(page, "equip-slot-offhand");
    }

    private void clearGridHover(HyUIPage page, String id) {
        try {
            page.getById(id, ItemGridBuilder.class).ifPresent(grid -> {
                for (ItemGridSlot slot : grid.getSlots()) {
                    if (slot == null) continue;
                    try {
                        slot.setName("");
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                    try {
                        slot.setDescription("");
                    }
                    catch (Throwable throwable) {}
                }
            });
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public void openPanel(PlayerRef playerRef, Store<EntityStore> store) {
        UUID playerId = playerRef.getUuid();
        HyUIPage oldPage = this.openPages.remove(playerId);
        if (oldPage != null) {
            this.closePageWithHoverClear(oldPage, false);
        }
        String tab = this.currentTab.getOrDefault(playerId, TAB_EQUIPMENT);
        this.currentTab.put(playerId, tab);
        PlayerCompanionData pData = this.companionManager.getOrCreate(playerId);
        CompanionRecord companion = this.resolveCompanion(pData);
        List<String[]> inventoryItems = this.equipmentManager.collectInventoryItems(playerRef, store);
        this.inventoryCache.put(playerId, inventoryItems);
        String html = this.buildHtml(companion, tab, inventoryItems, pData, playerRef);
        try {
            PageBuilder pb = (PageBuilder)PageBuilder.pageForPlayer(playerRef).fromHtml(html);
            pb.onDismiss((dismissedPage, wasOpenedElsewhere) -> this.clearPanelState(playerId, (HyUIPage)dismissedPage));
            this.registerEvents(pb, playerRef, store, tab);
            HyUIPage page = pb.open(store);
            this.openPages.put(playerId, page);
            this.startLiveRefresh(playerRef);
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to open CompanionPanel");
            playerRef.sendMessage(Message.raw((String)"Failed to open visual panel. Try /companion uimenu instead."));
        }
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

    private String buildHtml(CompanionRecord companion, String tab, List<String[]> inventoryItems, PlayerCompanionData pData, PlayerRef playerRef) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("<div class='page-overlay'>");
        sb.append("<div class='container' data-hyui-title='Companion' style='anchor-width: 1490; anchor-height: 1004;'>");
        sb.append("<div class='container-contents' style='layout-mode: top;'>");
        this.buildHeader(sb, companion, pData);
        this.buildThreeColumnLayout(sb, companion, inventoryItems, pData, playerRef);
        this.buildBottomBar(sb, pData);
        sb.append("</div></div></div>");
        return sb.toString();
    }

    private void buildThreeColumnLayout(StringBuilder sb, CompanionRecord companion, List<String[]> inventoryItems, PlayerCompanionData pData, PlayerRef playerRef) {
        sb.append("<div style='layout-mode: left; anchor-height: 840; padding: 6;'>");
        sb.append("<div style='anchor-width: 595; anchor-height: 828; layout-mode: top; padding: 8;'>");
        sb.append("<p style='font-size: 20; padding: 4;'>Equipment</p>");
        sb.append("<div style='anchor-height: 2; background-color: rgba(220,220,220,0.25); margin-bottom: 6;'></div>");
        this.buildEquipmentColumn(sb, companion, inventoryItems, pData, playerRef);
        sb.append("</div>");
        sb.append("<div style='anchor-width: 2; anchor-height: 828; background-color: rgba(220,220,220,0.30);'></div>");
        sb.append("<div style='anchor-width: 484; anchor-height: 828; layout-mode: top; padding: 8;'>");
        sb.append("<p style='font-size: 20; padding: 4;'>Controls</p>");
        sb.append("<div style='anchor-height: 2; background-color: rgba(220,220,220,0.25); margin-bottom: 6;'></div>");
        this.buildControlsColumn(sb, companion, pData);
        sb.append("</div>");
        sb.append("<div style='anchor-width: 2; anchor-height: 828; background-color: rgba(220,220,220,0.30);'></div>");
        sb.append("<div style='anchor-width: 340; anchor-height: 828; layout-mode: top; padding: 16 8 8 16;'>");
        sb.append("<p style='font-size: 20; padding: 4;'>Stats</p>");
        sb.append("<div style='anchor-height: 2; background-color: rgba(220,220,220,0.25); margin-bottom: 6;'></div>");
        this.buildStatsColumn(sb, companion, playerRef);
        sb.append("</div>");
        sb.append("</div>");
    }

    private void buildEquipmentColumn(StringBuilder sb, CompanionRecord companion, List<String[]> inventoryItems, PlayerCompanionData pData, PlayerRef playerRef) {
        if (companion == null) {
            sb.append("<p style='font-size: 14; padding: 10;'>No companion selected.</p>");
            return;
        }
        sb.append("<p style='font-size: 14; padding: 2;'>Companion Gear</p>");
        sb.append("<div style='layout-mode: left; anchor-height: 78;'>");
        sb.append("<div style='anchor-width: 16;'></div>");
        this.buildEquipSlotGrid(sb, companion, EquipmentSlot.HELMET, "equip-slot-head", "Head", pData);
        sb.append("<div style='anchor-width: 16;'></div>");
        this.buildEquipSlotGrid(sb, companion, EquipmentSlot.CHESTPLATE, "equip-slot-chest", "Chest", pData);
        sb.append("<div style='anchor-width: 16;'></div>");
        this.buildEquipSlotGrid(sb, companion, EquipmentSlot.LEGGINGS, "equip-slot-legs", "Legs", pData);
        sb.append("</div>");
        sb.append("<div style='layout-mode: left; anchor-height: 78;'>");
        sb.append("<div style='anchor-width: 16;'></div>");
        this.buildEquipSlotGrid(sb, companion, EquipmentSlot.BOOTS, "equip-slot-gloves", "Gloves", pData);
        sb.append("<div style='anchor-width: 16;'></div>");
        this.buildEquipSlotGrid(sb, companion, EquipmentSlot.WEAPON, "equip-slot-weapon", "Weapon", pData);
        sb.append("<div style='anchor-width: 16;'></div>");
        this.buildEquipSlotGrid(sb, companion, EquipmentSlot.OFFHAND, "equip-slot-offhand", "Offhand", pData);
        sb.append("</div>");
        sb.append("<p style='font-size: 11; padding: 2;'>(Click slot item to unequip)</p>");
        sb.append("<div style='anchor-height: 10;'></div>");
        sb.append("<p style='font-size: 14; padding: 2;'>Companion Inventory</p>");
        sb.append("<div style='layout-mode: left; anchor-height: 178;'>");
        sb.append("<div style='anchor-width: 8;'></div>");
        this.buildCompanionInventoryGrid(sb, companion, pData, playerRef);
        sb.append("</div>");
        sb.append("<div style='anchor-height: 36;'></div>");
        sb.append("<p style='font-size: 14; padding: 2;'>Your Inventory</p>");
        sb.append("<div style='anchor-height: 0;'></div>");
        sb.append("<div style='layout-mode: left; anchor-height: 430;'>");
        sb.append("<div style='anchor-width: 8;'></div>");
        this.buildPlayerInventoryGrid(sb, inventoryItems, pData);
        sb.append("</div>");
        sb.append("<p style='font-size: 11; padding: 2;'>(Click equippable/ammo items to send)</p>");
    }

    private void buildControlsColumn(StringBuilder sb, CompanionRecord companion, PlayerCompanionData pData) {
        String cmd2;
        String cmd1;
        if (companion == null) {
            sb.append("<p style='font-size: 14; padding: 10;'>No companion selected. Use /companion create.</p>");
            return;
        }
        CompanionRuntimeState rt = this.companionManager.getRuntime(companion.uniqueId);
        sb.append("<p style='font-size: 14; padding: 2;'>Stance</p>");
        sb.append("<div style='layout-mode: left; anchor-height: 34;'>");
        sb.append("<div style='anchor-width: 6;'></div>");
        this.buildBtn(sb, "btn-follow", "Follow", 112);
        this.buildBtn(sb, "btn-stay", "Stay", 112);
        this.buildBtn(sb, "btn-patrol", "Patrol", 112);
        this.buildBtn(sb, "btn-free", "Free", 112);
        sb.append("</div>");
        sb.append("<p style='font-size: 14; padding: 2;'>Role</p>");
        sb.append("<div style='layout-mode: left; anchor-height: 34;'>");
        sb.append("<div style='anchor-width: 42;'></div>");
        this.buildBtn(sb, "btn-fighter", "Fighter", 128);
        this.buildBtn(sb, "btn-farmer", "Farmer", 128);
        this.buildBtn(sb, "btn-miner", "Miner", 128);
        sb.append("</div>");
        sb.append("<p style='font-size: 14; padding: 2;'>Items</p>");
        sb.append("<div style='layout-mode: left; anchor-height: 34;'>");
        sb.append("<div style='anchor-width: 42;'></div>");
        this.buildBtn(sb, "btn-give", "Give", 128);
        this.buildBtn(sb, "btn-take", "Take", 128);
        this.buildBtn(sb, "btn-giveall", "Give All", 128);
        sb.append("</div>");
        sb.append("<div style='layout-mode: left; anchor-height: 34;'>");
        sb.append("<div style='anchor-width: 178;'></div>");
        this.buildBtn(sb, "btn-takeall", "Take All", 128);
        sb.append("</div>");
        sb.append("<p style='font-size: 14; padding: 2;'>Management</p>");
        sb.append("<div style='layout-mode: left; anchor-height: 34;'>");
        sb.append("<div style='anchor-width: 42;'></div>");
        this.buildBtn(sb, "btn-summon", "Summon", 128);
        this.buildBtn(sb, "btn-dismiss", "Dismiss", 128);
        this.buildBtn(sb, "btn-revive", "Revive", 128);
        sb.append("</div>");
        sb.append("<p style='font-size: 14; padding: 2;'>Deposit</p>");
        sb.append("<div style='layout-mode: left; anchor-height: 34;'>");
        sb.append("<div style='anchor-width: 42;'></div>");
        this.buildBtn(sb, "btn-setbox", "Link Chest", 128);
        this.buildBtn(sb, "btn-unsetbox", "Unlink Chest", 140);
        this.buildBtn(sb, "btn-deposit", "Deposit", 116);
        sb.append("</div>");
        int chestCount = companion.linkedChests != null ? companion.linkedChests.size() : 0;
        sb.append("<p id='ctrl-linked-chests' style='font-size: 12; padding: 2;'>Linked chests: ").append(chestCount).append("</p>");
        sb.append("<p style='font-size: 14; padding: 2;'>Commands</p>");
        sb.append("<div style='layout-mode: left; anchor-height: 34;'>");
        sb.append("<div style='anchor-width: 42;'></div>");
        String cmd3 = switch (companion.mode) {
            case CompanionMode.FIGHTER -> {
                cmd1 = "Fight";
                cmd2 = "Max Defend";
                yield companion.lootModeEnabled ? "Loot: ON" : "Loot: OFF";
            }
            case CompanionMode.FARMER -> {
                cmd1 = "Farm";
                cmd2 = "Set TL";
                yield "Set BR";
            }
            case CompanionMode.MINER -> {
                cmd1 = "Mine";
                cmd2 = "Stop";
                yield "Mine For";
            }
            default -> {
                cmd1 = "Cmd 1";
                cmd2 = "Cmd 2";
                yield "Cmd 3";
            }
        };
        this.buildBtn(sb, "btn-cmd1", cmd1, 128);
        this.buildBtn(sb, "btn-cmd2", cmd2, 128);
        this.buildBtn(sb, "btn-cmd3", cmd3, 128);
        sb.append("</div>");
        sb.append("<div style='anchor-height: 4;'></div>");
        sb.append("<div style='layout-mode: left; anchor-height: 22;'>");
        sb.append("<p id='ctrl-farm-area' style='font-size: 12; anchor-width: 440; padding: 1;'>");
        sb.append((String)(companion.mode == CompanionMode.FARMER ? "Linked Farm Area: " + CompanionPanel.esc(this.formatFarmArea(companion)) : ""));
        sb.append("</p>");
        sb.append("</div>");
        sb.append("<div style='layout-mode: left; anchor-height: 22;'>");
        sb.append("<p id='ctrl-farm-coords' style='font-size: 12; anchor-width: 440; padding: 1;'>");
        sb.append((String)(companion.mode == CompanionMode.FARMER ? "Farm Coordinates: " + CompanionPanel.esc(this.formatFarmCoordinates(companion)) : ""));
        sb.append("</p>");
        sb.append("</div>");
        sb.append("<div style='layout-mode: left; anchor-height: 22;'>");
        sb.append("<p id='ctrl-farm-status' style='font-size: 12; anchor-width: 440; padding: 1;'>");
        sb.append((String)(companion.mode == CompanionMode.FARMER ? "Farm Status: " + CompanionPanel.esc(this.formatFarmStatus(rt)) : ""));
        sb.append("</p>");
        sb.append("</div>");
        sb.append("<div style='anchor-height: 4;'></div>");
        sb.append("<div style='layout-mode: left; anchor-height: 22;'>");
        sb.append("<p id='ctrl-command-status' style='font-size: 12; anchor-width: 440; padding: 1;'>");
        sb.append(CompanionPanel.esc(this.formatCommandStatus(companion, rt)));
        sb.append("</p>");
        sb.append("</div>");
        sb.append("<div style='layout-mode: left; anchor-height: 22;'>");
        sb.append("<p id='ctrl-command-detail' style='font-size: 12; anchor-width: 440; padding: 1;'>");
        sb.append(CompanionPanel.esc(this.formatCommandDetail(companion, rt)));
        sb.append("</p>");
        sb.append("</div>");
        sb.append("<div style='anchor-height: 10;'></div>");
        sb.append("<p style='font-size: 14; padding: 2;'>Change Name</p>");
        sb.append("<div style='layout-mode: left; anchor-height: 34;'>");
        sb.append("<div style='anchor-width: 7;'></div>");
        String currentName = companion.name != null ? companion.name : "";
        sb.append("<input id='renameInput' type='text' ");
        sb.append("value='").append(CompanionPanel.esc(currentName)).append("' ");
        sb.append("style='anchor-width: 260; anchor-height: 28; margin-right: 8;'>");
        sb.append("</input>");
        this.buildBtn(sb, "btn-rename-ok", "OK", 100);
        sb.append("</div>");
    }

    private void buildStatsColumn(StringBuilder sb, CompanionRecord companion, PlayerRef playerRef) {
        if (companion == null) {
            sb.append("<p style='font-size: 14; padding: 10;'>No companion selected.</p>");
            return;
        }
        this.buildStatsTab(sb, companion, playerRef);
    }

    private void buildHeader(StringBuilder sb, CompanionRecord companion, PlayerCompanionData pData) {
        sb.append("<div style='layout-mode: top; anchor-height: 64; padding: 8;'>");
        sb.append("<div style='layout-mode: left; anchor-height: 28;'>");
        if (companion != null) {
            sb.append("<p id='hdr-name' style='font-size: 22; anchor-width: 350;'>");
            sb.append(CompanionPanel.esc(companion.getDisplayName()));
            sb.append("</p>");
            sb.append("<p id='hdr-status' style='font-size: 16; anchor-width: 120;'>[");
            sb.append(companion.getStatusTag());
            sb.append("]</p>");
            String role = switch (companion.mode) {
                default -> throw new MatchException(null, null);
                case CompanionMode.FIGHTER -> "Fighter";
                case CompanionMode.FARMER -> "Farmer";
                case CompanionMode.MINER -> "Miner";
            };
            String stance = switch (companion.followMode) {
                default -> throw new MatchException(null, null);
                case FollowMode.FOLLOW -> "Follow";
                case FollowMode.STAY -> "Stay";
                case FollowMode.PATROL -> "Patrol";
                case FollowMode.FREE -> "Free";
            };
            sb.append("<p id='hdr-role-stance' style='font-size: 16; anchor-width: 220;'>");
            sb.append(role).append(" + ").append(stance);
            sb.append("</p>");
            int idx = 0;
            for (int i = 0; i < pData.companions.size(); ++i) {
                if (companion.uniqueId == null || !companion.uniqueId.equals(pData.companions.get((int)i).uniqueId)) continue;
                idx = i;
                break;
            }
            sb.append("<p id='hdr-index' style='font-size: 14;'>(");
            sb.append(idx + 1).append("/").append(pData.companions.size());
            sb.append(")</p>");
        } else {
            sb.append("<p id='hdr-name' style='font-size: 22;'>No companion selected</p>");
        }
        sb.append("</div>");
        sb.append("<div style='layout-mode: left; anchor-height: 18;'>");
        sb.append("<div style='anchor-width: 350;'></div>");
        sb.append("<p id='hdr-death-cause' style='font-size: 12; padding: 2 2 0 0; anchor-width: 370;'>");
        sb.append(CompanionPanel.esc(this.formatHeaderDeathCause(companion)));
        sb.append("</p>");
        sb.append("</div>");
        sb.append("</div>");
    }

    private void buildTabButtons(StringBuilder sb, String activeTab) {
        sb.append("<div style='layout-mode: left; anchor-height: 35; padding: 3;'>");
        this.buildTabBtn(sb, "tab-equipment", activeTab.equals(TAB_EQUIPMENT) ? ">> Equipment <<" : "Equipment");
        this.buildTabBtn(sb, "tab-controls", activeTab.equals(TAB_CONTROLS) ? ">> Controls <<" : "Controls");
        this.buildTabBtn(sb, "tab-stats", activeTab.equals(TAB_STATS) ? ">> Stats <<" : "Stats");
        sb.append("</div>");
    }

    private void buildTabBtn(StringBuilder sb, String id, String label) {
        sb.append("<button id='").append(id).append("' ");
        sb.append("style='anchor-width: 160; anchor-height: 30; margin-right: 8;'>");
        sb.append(CompanionPanel.esc(label));
        sb.append("</button>");
    }

    private void buildEquipmentTab(StringBuilder sb, CompanionRecord companion, List<String[]> inventoryItems, PlayerCompanionData pData, PlayerRef playerRef) {
        sb.append("<div style='layout-mode: left; anchor-height: 560;'>");
        sb.append("<div style='anchor-width: 480; anchor-height: 560; layout-mode: top; padding: 10;'>");
        sb.append("<p style='font-size: 20; padding: 5;'>Companion Equipment</p>");
        if (companion != null) {
            sb.append("<div style='layout-mode: left; anchor-height: 85;'>");
            sb.append("<div style='anchor-width: 140;'></div>");
            this.buildEquipSlotGrid(sb, companion, EquipmentSlot.HELMET, "equip-slot-head", "Head", pData);
            sb.append("<div style='anchor-width: 140;'></div>");
            sb.append("</div>");
            sb.append("<div style='layout-mode: left; anchor-height: 85;'>");
            this.buildEquipSlotGrid(sb, companion, EquipmentSlot.BOOTS, "equip-slot-gloves", "Gloves", pData);
            sb.append("<div style='anchor-width: 20;'></div>");
            this.buildEquipSlotGrid(sb, companion, EquipmentSlot.CHESTPLATE, "equip-slot-chest", "Chest", pData);
            sb.append("<div style='anchor-width: 20;'></div>");
            this.buildEquipSlotGrid(sb, companion, EquipmentSlot.WEAPON, "equip-slot-weapon", "Weapon", pData);
            sb.append("</div>");
            sb.append("<div style='layout-mode: left; anchor-height: 85;'>");
            this.buildEquipSlotGrid(sb, companion, EquipmentSlot.OFFHAND, "equip-slot-offhand", "Offhand", pData);
            sb.append("<div style='anchor-width: 20;'></div>");
            this.buildEquipSlotGrid(sb, companion, EquipmentSlot.LEGGINGS, "equip-slot-legs", "Legs", pData);
            sb.append("<div style='anchor-width: 140;'></div>");
            sb.append("</div>");
            sb.append("<p style='font-size: 11; padding: 3;'>(Click equipped item to unequip)</p>");
            sb.append("<p style='font-size: 16; padding: 5;'>Companion Inventory</p>");
            this.buildCompanionInventoryGrid(sb, companion, pData, playerRef);
        } else {
            sb.append("<p style='font-size: 14; padding: 20;'>No companion selected.</p>");
        }
        sb.append("</div>");
        sb.append("<div style='anchor-width: 520; anchor-height: 560; layout-mode: top; padding: 10;'>");
        sb.append("<p style='font-size: 20; padding: 5;'>Your Inventory</p>");
        this.buildPlayerInventoryGrid(sb, inventoryItems, pData);
        sb.append("<p style='font-size: 11; padding: 3;'>(Click equippable/ammo items to send)</p>");
        sb.append("</div>");
        sb.append("</div>");
    }

    private void buildEquipSlotGrid(StringBuilder sb, CompanionRecord companion, EquipmentSlot slot, String id, String label, PlayerCompanionData pData) {
        String equipped = companion.getEquipped(slot);
        boolean tipsOn = pData != null && pData.uiTooltipsEnabled;
        sb.append("<div style='layout-mode: top; anchor-width: 100; padding: 3;'>");
        sb.append("<p style='font-size: 12;'>").append(label).append("</p>");
        sb.append("<div id='").append(id).append("' class='item-grid' ");
        sb.append("data-hyui-slots-per-row='1' ");
        sb.append("data-hyui-info-display='").append(this.gridInfoDisplayMode(pData)).append("' ");
        sb.append("data-hyui-are-items-draggable='false' ");
        sb.append("style='anchor-width: 55; anchor-height: 55;'>");
        sb.append("<div class='item-grid-slot' ");
        sb.append("data-hyui-activatable='true' ");
        if (equipped != null) {
            sb.append("data-hyui-item-id='").append(CompanionPanel.esc(equipped)).append("' ");
            sb.append("data-hyui-quantity='1' ");
            if (tipsOn) {
                sb.append("data-hyui-name='").append(CompanionPanel.esc(EquipmentSlot.getItemDisplayName(equipped))).append("' ");
                sb.append("data-hyui-description='Click to unequip' ");
            }
            sb.append(">");
        } else {
            if (tipsOn) {
                sb.append("data-hyui-name='").append(label).append(" (empty)' ");
                sb.append("data-hyui-description='").append(label).append(" slot' ");
            }
            sb.append(">");
        }
        sb.append("</div>");
        sb.append("</div>");
        sb.append("</div>");
    }

    private void buildCompanionInventoryGrid(StringBuilder sb, CompanionRecord companion, PlayerCompanionData pData, PlayerRef playerRef) {
        List<String[]> companionItems = this.collectCompanionInventoryItems(companion, playerRef);
        this.companionInventoryCache.put(playerRef.getUuid(), companionItems);
        boolean tipsOn = pData != null && pData.uiTooltipsEnabled;
        sb.append("<div id='companion-inv-grid' class='item-grid' ");
        sb.append("data-hyui-slots-per-row='9' ");
        sb.append("data-hyui-info-display='").append(this.gridInfoDisplayMode(pData)).append("' ");
        sb.append("data-hyui-are-items-draggable='false' ");
        sb.append("style='anchor-width: 560; anchor-height: 180;'>");
        int count = 0;
        for (String[] entry : companionItems) {
            if (count >= 27) break;
            sb.append("<div class='item-grid-slot' ");
            sb.append("data-hyui-activatable='true' ");
            sb.append("data-hyui-item-id='").append(CompanionPanel.esc(entry[0])).append("' ");
            sb.append("data-hyui-quantity='").append(entry[2]).append("' ");
            if (tipsOn) {
                sb.append("data-hyui-name='").append(CompanionPanel.esc(entry[1])).append(" x").append(entry[2]).append("' ");
            }
            sb.append(">");
            sb.append("</div>");
            ++count;
        }
        for (int i = count; i < 27; ++i) {
            sb.append("<div class='item-grid-slot'></div>");
        }
        sb.append("</div>");
    }

    private void buildPlayerInventoryGrid(StringBuilder sb, List<String[]> inventoryItems, PlayerCompanionData pData) {
        boolean tipsOn = pData != null && pData.uiTooltipsEnabled;
        sb.append("<div id='player-inventory-grid' class='item-grid' ");
        sb.append("data-hyui-slots-per-row='9' ");
        sb.append("data-hyui-info-display='").append(this.gridInfoDisplayMode(pData)).append("' ");
        sb.append("data-hyui-are-items-draggable='false' ");
        sb.append("style='anchor-width: 560; anchor-height: 320; margin-top: 0;'>");
        int maxSlots = 45;
        for (int i = 0; i < maxSlots; ++i) {
            if (i < inventoryItems.size()) {
                String[] item = inventoryItems.get(i);
                sb.append("<div class='item-grid-slot' ");
                sb.append("data-hyui-activatable='true' ");
                sb.append("data-hyui-item-id='").append(CompanionPanel.esc(item[0])).append("' ");
                String qty = item.length >= 3 ? item[2] : "1";
                sb.append("data-hyui-quantity='").append(CompanionPanel.esc(qty)).append("' ");
                if (tipsOn) {
                    sb.append("data-hyui-name='").append(CompanionPanel.esc(item[1])).append(" x").append(CompanionPanel.esc(qty)).append("' ");
                }
                sb.append(">");
                sb.append("</div>");
                continue;
            }
            sb.append("<div class='item-grid-slot'></div>");
        }
        sb.append("</div>");
    }

    private void buildControlsTab(StringBuilder sb, CompanionRecord companion, PlayerCompanionData pData) {
        String cmd2;
        String cmd1;
        sb.append("<div style='layout-mode: top; anchor-height: 560; padding: 10;'>");
        if (companion == null) {
            sb.append("<p style='font-size: 16; padding: 20;'>No companion selected. Use /companion create.</p>");
            sb.append("</div>");
            return;
        }
        sb.append("<p style='font-size: 16; padding: 3;'>Stance:</p>");
        sb.append("<div style='layout-mode: left; anchor-height: 35; padding: 2;'>");
        this.buildBtn(sb, "btn-follow", "Follow", 105);
        this.buildBtn(sb, "btn-stay", "Stay", 105);
        this.buildBtn(sb, "btn-patrol", "Patrol", 105);
        this.buildBtn(sb, "btn-free", "Free", 105);
        sb.append("</div>");
        sb.append("<p style='font-size: 16; padding: 3;'>Role:</p>");
        sb.append("<div style='layout-mode: left; anchor-height: 35; padding: 2;'>");
        this.buildBtn(sb, "btn-fighter", "Fighter", 105);
        this.buildBtn(sb, "btn-farmer", "Farmer", 105);
        this.buildBtn(sb, "btn-miner", "Miner", 105);
        sb.append("</div>");
        sb.append("<p style='font-size: 16; padding: 3;'>Items:</p>");
        sb.append("<div style='layout-mode: left; anchor-height: 35; padding: 2;'>");
        this.buildBtn(sb, "btn-give", "Give Item", 110);
        this.buildBtn(sb, "btn-take", "Take Item", 110);
        this.buildBtn(sb, "btn-giveall", "Give All", 110);
        this.buildBtn(sb, "btn-takeall", "Take All", 110);
        sb.append("</div>");
        sb.append("<p style='font-size: 16; padding: 3;'>Management:</p>");
        sb.append("<div style='layout-mode: left; anchor-height: 35; padding: 2;'>");
        this.buildBtn(sb, "btn-summon", "Summon", 130);
        this.buildBtn(sb, "btn-dismiss", "Dismiss", 130);
        this.buildBtn(sb, "btn-revive", "Revive", 130);
        sb.append("</div>");
        sb.append("<p style='font-size: 16; padding: 3;'>Deposit:</p>");
        sb.append("<div style='layout-mode: left; anchor-height: 35; padding: 2;'>");
        this.buildBtn(sb, "btn-setbox", "Link Chest", 110);
        this.buildBtn(sb, "btn-unsetbox", "Unlink Chest", 122);
        this.buildBtn(sb, "btn-deposit", "Deposit Now", 108);
        int chestCount = companion.linkedChests != null ? companion.linkedChests.size() : 0;
        sb.append("<p style='font-size: 13; padding: 5;'>Linked: ").append(chestCount).append("</p>");
        sb.append("</div>");
        sb.append("<p style='font-size: 16; padding: 3;'>Commands:</p>");
        sb.append("<div style='layout-mode: left; anchor-height: 35; padding: 2;'>");
        String cmd3 = switch (companion.mode) {
            case CompanionMode.FIGHTER -> {
                cmd1 = "Fight";
                cmd2 = "Max Defend";
                yield companion.lootModeEnabled ? "Loot: ON" : "Loot: OFF";
            }
            case CompanionMode.FARMER -> {
                cmd1 = "Farm";
                cmd2 = "Set TL";
                yield "Set BR";
            }
            case CompanionMode.MINER -> {
                cmd1 = "Mine";
                cmd2 = "Stop Mining";
                yield "Mine For This";
            }
            default -> {
                cmd1 = "Cmd 1";
                cmd2 = "Cmd 2";
                yield "Cmd 3";
            }
        };
        this.buildBtn(sb, "btn-cmd1", cmd1, 130);
        this.buildBtn(sb, "btn-cmd2", cmd2, 130);
        this.buildBtn(sb, "btn-cmd3", cmd3, 130);
        sb.append("</div>");
        sb.append("<div style='anchor-height: 2;'></div>");
        sb.append("<p style='font-size: 14; padding: 2;'>Change Name</p>");
        sb.append("<div style='layout-mode: left; anchor-height: 32;'>");
        sb.append("<div style='anchor-width: 7;'></div>");
        sb.append("<input id='renameInput' type='text' ");
        sb.append("value='").append(CompanionPanel.esc(companion.name != null ? companion.name : "")).append("' ");
        sb.append("style='anchor-width: 394; anchor-height: 26;'></input>");
        sb.append("</div>");
        sb.append("<div style='layout-mode: left; anchor-height: 30;'>");
        sb.append("<div style='anchor-width: 154;'></div>");
        this.buildBtn(sb, "btn-rename-ok", "OK", 100);
        sb.append("</div>");
        sb.append("</div>");
    }

    private void buildBtn(StringBuilder sb, String id, String label, int width) {
        this.buildBtnStyled(sb, id, label, width, null);
    }

    private void buildBtnStyled(StringBuilder sb, String id, String label, int width, String extraStyle) {
        sb.append("<button id='").append(id).append("' ");
        sb.append("style='anchor-width: ").append(width).append("; anchor-height: 32; margin-right: 6;");
        if (extraStyle != null && !extraStyle.isBlank()) {
            sb.append(" ").append(extraStyle);
        }
        sb.append("'>");
        sb.append(CompanionPanel.esc(label));
        sb.append("</button>");
    }

    private void buildBtnNoRightMargin(StringBuilder sb, String id, String label, int width) {
        sb.append("<button id='").append(id).append("' ");
        sb.append("style='anchor-width: ").append(width).append("; anchor-height: 32; margin-right: 0;'>");
        sb.append(CompanionPanel.esc(label));
        sb.append("</button>");
    }

    private String formatFarmArea(CompanionRecord companion) {
        if (companion == null || companion.farmAreaTopLeft == null || companion.farmAreaBottomRight == null) {
            return "Not set";
        }
        int width = Math.abs(companion.farmAreaBottomRight.x - companion.farmAreaTopLeft.x) + 1;
        int depth = Math.abs(companion.farmAreaBottomRight.z - companion.farmAreaTopLeft.z) + 1;
        int total = width * depth;
        return width + "x" + depth + " (" + total + " blocks)";
    }

    private String formatFarmCoordinates(CompanionRecord companion) {
        if (companion == null || companion.farmAreaTopLeft == null || companion.farmAreaBottomRight == null) {
            return "TL -, BR -";
        }
        return "TL (" + companion.farmAreaTopLeft.x + "," + companion.farmAreaTopLeft.y + "," + companion.farmAreaTopLeft.z + ") BR (" + companion.farmAreaBottomRight.x + "," + companion.farmAreaBottomRight.y + "," + companion.farmAreaBottomRight.z + ")";
    }

    private String formatFarmStatus(CompanionRuntimeState runtime) {
        if (runtime == null || runtime.farmStatusText == null || runtime.farmStatusText.isBlank()) {
            return "Unknown";
        }
        return runtime.farmStatusText;
    }

    private String formatCommandStatus(CompanionRecord companion, CompanionRuntimeState runtime) {
        if (companion == null) {
            return "";
        }
        return switch (companion.mode) {
            default -> throw new MatchException(null, null);
            case CompanionMode.FARMER -> "";
            case CompanionMode.MINER -> "Mining Status: " + this.formatMineStatus(runtime);
            case CompanionMode.FIGHTER -> "";
        };
    }

    private String formatCommandDetail(CompanionRecord companion, CompanionRuntimeState runtime) {
        if (companion == null) {
            return "";
        }
        return switch (companion.mode) {
            default -> throw new MatchException(null, null);
            case CompanionMode.FARMER -> "";
            case CompanionMode.MINER -> this.formatMineTarget(runtime);
            case CompanionMode.FIGHTER -> "";
        };
    }

    private String formatMineStatus(CompanionRuntimeState runtime) {
        if (runtime == null) {
            return "Inactive";
        }
        if (runtime.mineStatusText != null && !runtime.mineStatusText.isBlank()) {
            return runtime.mineStatusText;
        }
        return runtime.miningActive ? "Waiting" : "Inactive";
    }

    private String formatMineTarget(CompanionRuntimeState runtime) {
        if (runtime == null || runtime.mineTargetBlockId == null || runtime.mineTargetBlockId.isBlank()) {
            return "";
        }
        return "Mining For: " + runtime.mineTargetBlockId;
    }

    private void buildStatsTab(StringBuilder sb, CompanionRecord companion, PlayerRef playerRef) {
        sb.append("<div style='layout-mode: top; anchor-height: 700; padding: 15;'>");
        if (companion == null) {
            sb.append("<p style='font-size: 16; padding: 20;'>No companion selected.</p>");
            sb.append("</div>");
            return;
        }
        sb.append("<p style='font-size: 20; padding: 5;'>Combat</p>");
        int combatIdx = Math.max(0, Math.min(companion.combatLevel - 1, ProgressionConfig.COMBAT_LEVEL_NAMES.length - 1));
        sb.append("<p id='stat-combat-level' style='font-size: 15; padding: 3;'>Level ").append(companion.combatLevel);
        sb.append(" - ").append(ProgressionConfig.COMBAT_LEVEL_NAMES[combatIdx]).append("</p>");
        sb.append("<p id='stat-combat-kills' style='font-size: 13; padding: 2;'>Kills: ").append(companion.combatKills).append("</p>");
        int killsNeeded = ProgressionConfig.killsToNextCombatLevel(companion.combatLevel, companion.combatKills);
        if (killsNeeded > 0) {
            sb.append("<p style='font-size: 13; padding: 2;'>Next level in ").append(killsNeeded).append(" kills</p>");
        } else if (killsNeeded == -1) {
            sb.append("<p style='font-size: 13; padding: 2;'>MAX LEVEL</p>");
        }
        double dmgMult = ProgressionConfig.COMBAT_DAMAGE_MULT[Math.min(combatIdx, ProgressionConfig.COMBAT_DAMAGE_MULT.length - 1)];
        if (dmgMult > 1.0) {
            sb.append("<p style='font-size: 13; padding: 2;'>Damage bonus: +").append((int)((dmgMult - 1.0) * 100.0)).append("%</p>");
        }
        sb.append("<div style='anchor-height: 15;'></div>");
        sb.append("<p style='font-size: 20; padding: 5;'>Farming</p>");
        int farmIdx = Math.max(0, Math.min(companion.farmLevel - 1, ProgressionConfig.FARM_LEVEL_NAMES.length - 1));
        sb.append("<p id='stat-farm-level' style='font-size: 15; padding: 3;'>Level ").append(companion.farmLevel);
        sb.append(" - ").append(ProgressionConfig.FARM_LEVEL_NAMES[farmIdx]).append("</p>");
        sb.append("<p id='stat-farm-harvests' style='font-size: 13; padding: 2;'>Harvests: ").append(companion.farmHarvests).append("</p>");
        int harvestsNeeded = ProgressionConfig.harvestsToNextFarmLevel(companion.farmLevel, companion.farmHarvests);
        if (harvestsNeeded > 0) {
            sb.append("<p style='font-size: 13; padding: 2;'>Next level in ").append(harvestsNeeded).append(" harvests</p>");
        } else if (harvestsNeeded == -1) {
            sb.append("<p style='font-size: 13; padding: 2;'>MAX LEVEL</p>");
        }
        if (ProgressionConfig.FARM_AUTO_REPLANT[farmIdx]) {
            sb.append("<p style='font-size: 13; padding: 2;'>Auto-replant: ACTIVE</p>");
        }
        if (ProgressionConfig.FARM_DOUBLE_YIELD[farmIdx]) {
            sb.append("<p style='font-size: 13; padding: 2;'>Double yield: ACTIVE</p>");
        }
        sb.append("<div style='anchor-height: 15;'></div>");
        sb.append("<p style='font-size: 20; padding: 5;'>Mining</p>");
        int mineIdx = Math.max(0, Math.min(companion.mineLevel - 1, ProgressionConfig.MINE_LEVEL_NAMES.length - 1));
        sb.append("<p id='stat-mine-level' style='font-size: 15; padding: 3;'>Level ").append(companion.mineLevel);
        sb.append(" - ").append(ProgressionConfig.MINE_LEVEL_NAMES[mineIdx]).append("</p>");
        sb.append("<p id='stat-mine-blocks' style='font-size: 13; padding: 2;'>Blocks mined: ").append(companion.mineBlocks).append("</p>");
        int blocksNeeded = ProgressionConfig.blocksToNextMineLevel(companion.mineLevel, companion.mineBlocks);
        sb.append("<p id='stat-mine-next' style='font-size: 13; padding: 2;'>").append(this.formatMineNextLevelText(blocksNeeded)).append("</p>");
        double mineMult = ProgressionConfig.MINE_SPEED_MULT[Math.min(mineIdx, ProgressionConfig.MINE_SPEED_MULT.length - 1)];
        sb.append("<p id='stat-mine-speed' style='font-size: 13; padding: 2;'>").append(this.formatPercentBonusText("Mining speed", mineMult)).append("</p>");
        double mineMoveMult = ProgressionConfig.MINE_MOVE_SPEED_MULT[Math.min(mineIdx, ProgressionConfig.MINE_MOVE_SPEED_MULT.length - 1)];
        sb.append("<p id='stat-mine-move-speed' style='font-size: 13; padding: 2;'>").append(this.formatPercentBonusText("Move speed", mineMoveMult)).append("</p>");
        sb.append("<div style='anchor-height: 15;'></div>");
        if (playerRef != null) {
            sb.append("<p style='font-size: 20; padding: 5;'>Equipment</p>");
            try {
                String[] eqStats = this.companionSystem.getEquipmentStats(playerRef, companion);
                if (eqStats != null && eqStats.length >= 4) {
                    sb.append("<p id='stat-weapon' style='font-size: 14; padding: 2;'>Weapon: ").append(CompanionPanel.esc(eqStats[0])).append(" (Atk: ").append(eqStats[2]).append(")</p>");
                    sb.append("<p id='stat-armor' style='font-size: 14; padding: 2;'>Armor: ").append(CompanionPanel.esc(eqStats[1])).append(" (Def: ").append(eqStats[3]).append(")</p>");
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        sb.append("<div style='anchor-height: 15;'></div>");
        sb.append("<p id='stat-deaths' style='font-size: 14; padding: 2;'>Deaths: ").append(companion.deathCount).append("</p>");
        sb.append("<div style='anchor-height: 8;'></div>");
        sb.append("<div style='layout-mode: left; anchor-height: 34;'>");
        sb.append("<div style='anchor-width: 25;'></div>");
        this.buildBtn(sb, "btn-remove", "Remove Companion", 320);
        sb.append("</div>");
        sb.append("<div style='anchor-height: 6;'></div>");
        sb.append("<div style='layout-mode: left; anchor-height: 34;'>");
        sb.append("<div style='anchor-width: 25;'></div>");
        this.buildBtn(sb, "btn-clear-set-boxes", "Clear Set Boxes", 320);
        sb.append("</div>");
        sb.append("<div style='anchor-height: 6;'></div>");
        sb.append("<div style='layout-mode: left; anchor-height: 34;'>");
        sb.append("<div style='anchor-width: 25;'></div>");
        this.buildBtn(sb, "btn-clear-farm-area", "Clear Farm Area", 320);
        sb.append("</div>");
        sb.append("<div style='anchor-height: 8;'></div>");
        sb.append("<div style='layout-mode: left; anchor-height: 24;'>");
        sb.append("<div style='anchor-width: 25;'></div>");
        sb.append("<p id='stat-location' style='font-size: 13; padding: 2; anchor-width: 335;'>").append(CompanionPanel.esc(this.formatCompanionLocation(playerRef, companion))).append("</p>");
        sb.append("</div>");
        sb.append("</div>");
    }

    private void buildBottomBar(StringBuilder sb, PlayerCompanionData pData) {
        sb.append("<div style='layout-mode: top; anchor-height: 40; padding: 2;'>");
        sb.append("<div style='layout-mode: left; anchor-height: 0;'>");
        sb.append("</div>");
        sb.append("<div style='layout-mode: left; anchor-height: 36; padding: 0;'>");
        this.buildBtn(sb, "btn-prev", "< Prev", 130);
        this.buildBtn(sb, "btn-next", "Next >", 130);
        String ammo = pData != null && pData.consumeCompanionAmmo ? "Ammo: Consume" : "Ammo: Infinite";
        String tips = pData != null && pData.uiTooltipsEnabled ? "Tooltips: ON" : "Tooltips: OFF";
        sb.append("<div style='anchor-width: 676;'></div>");
        this.buildBtn(sb, "btn-ammo", ammo, 170);
        this.buildBtn(sb, "btn-tooltips", tips, 190);
        this.buildBtnNoRightMargin(sb, "btn-close", "CLOSE", 130);
        sb.append("</div>");
        sb.append("</div>");
    }

    private void registerEvents(PageBuilder pb, PlayerRef playerRef, Store<EntityStore> store, String tab) {
        UUID playerId = playerRef.getUuid();
        pb.addEventListener("btn-prev", CustomUIEventBindingType.Activating, (obj, ctx) -> {
            this.navigateCompanion(playerRef, -1);
            this.refreshDynamicSections((UIContext)ctx, playerRef, store);
        });
        pb.addEventListener("btn-next", CustomUIEventBindingType.Activating, (obj, ctx) -> {
            this.navigateCompanion(playerRef, 1);
            this.refreshDynamicSections((UIContext)ctx, playerRef, store);
        });
        pb.addEventListener("btn-ammo", CustomUIEventBindingType.Activating, (obj, ctx) -> {
            PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
            data.consumeCompanionAmmo = !data.consumeCompanionAmmo;
            this.saveSilently();
            playerRef.sendMessage(Message.raw((String)("Companion ranged ammo consumption: " + (data.consumeCompanionAmmo ? "ON" : "OFF"))));
            this.refreshDynamicSections((UIContext)ctx, playerRef, store);
        });
        pb.addEventListener("btn-tooltips", CustomUIEventBindingType.Activating, (obj, ctx) -> this.toggleTooltips(playerRef, store));
        pb.addEventListener("btn-close", CustomUIEventBindingType.Activating, (obj, ctx) -> {
            try {
                HyUIPage currentPage;
                if (ctx != null && (currentPage = (HyUIPage)ctx.getPage().orElse(null)) != null) {
                    this.clearPanelState(playerId, currentPage);
                    this.closePageWithHoverClear(currentPage, false);
                    return;
                }
                this.closePanel(playerId);
            }
            catch (Throwable t) {
                ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Close button failed.");
            }
        });
        this.registerEquipmentEvents(pb, playerRef, store);
        this.registerControlEvents(pb, playerRef, store);
        this.registerRenameEvents(pb, playerRef, store);
    }

    private void registerEquipmentEvents(PageBuilder pb, PlayerRef playerRef, Store<EntityStore> store) {
        UUID playerId = playerRef.getUuid();
        this.registerEquipSlotClick(pb, "equip-slot-head", EquipmentSlot.HELMET, playerRef, store);
        this.registerEquipSlotClick(pb, "equip-slot-chest", EquipmentSlot.CHESTPLATE, playerRef, store);
        this.registerEquipSlotClick(pb, "equip-slot-legs", EquipmentSlot.LEGGINGS, playerRef, store);
        this.registerEquipSlotClick(pb, "equip-slot-gloves", EquipmentSlot.BOOTS, playerRef, store);
        this.registerEquipSlotClick(pb, "equip-slot-weapon", EquipmentSlot.WEAPON, playerRef, store);
        this.registerEquipSlotClick(pb, "equip-slot-offhand", EquipmentSlot.OFFHAND, playerRef, store);
        pb.addEventListener("player-inventory-grid", CustomUIEventBindingType.SlotClicking, SlotClickingEventData.class, (data, ctx) -> {
            try {
                int idx = data.getSlotIndex();
                List<String[]> cache = this.inventoryCache.get(playerId);
                if (cache == null || idx < 0 || idx >= cache.size()) {
                    playerRef.sendMessage(Message.raw((String)"Empty slot."));
                    return;
                }
                String[] item = cache.get(idx);
                String assetId = item[0];
                PlayerCompanionData pData = this.companionManager.getOrCreate(playerId);
                CompanionRecord companion = this.resolveCompanion(pData);
                if (companion == null) {
                    playerRef.sendMessage(Message.raw((String)"No companion selected."));
                    return;
                }
                if (EquipmentSlot.isAmmo(assetId)) {
                    int movedAmmo = this.companionSystem.giveSpecificItem(playerRef, companion, assetId);
                    if (movedAmmo <= 0) {
                        playerRef.sendMessage(Message.raw((String)"Could not move ammo item."));
                        return;
                    }
                    playerRef.sendMessage(Message.raw((String)("Sent " + item[1] + " x" + movedAmmo + " to companion inventory.")));
                    this.saveSilently();
                    this.rebuildGrids((UIContext)ctx, playerRef, store);
                    return;
                }
                EquipmentSlot slot = EquipmentSlot.forItem(assetId);
                if (slot == null) {
                    int moved = this.companionSystem.giveSpecificItem(playerRef, companion, assetId);
                    if (moved <= 0) {
                        playerRef.sendMessage(Message.raw((String)"Could not move item."));
                        return;
                    }
                    playerRef.sendMessage(Message.raw((String)("Sent " + item[1] + " x" + moved + " to companion inventory.")));
                    this.saveSilently();
                    this.rebuildGrids((UIContext)ctx, playerRef, store);
                    return;
                }
                NPCEntity npcEntity = companion.active ? this.companionSystem.getNpcEntity(playerRef, companion) : null;
                String err = this.equipmentManager.equipItem(companion, slot, assetId, playerRef, npcEntity, store);
                if (err != null) {
                    playerRef.sendMessage(Message.raw((String)err));
                    return;
                }
                playerRef.sendMessage(Message.raw((String)("Equipped " + item[1] + " on " + companion.getDisplayName() + "!")));
                this.saveSilently();
                this.rebuildGrids((UIContext)ctx, playerRef, store);
            }
            catch (Throwable t) {
                ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Error handling equip click");
            }
        });
        pb.addEventListener("companion-inv-grid", CustomUIEventBindingType.SlotClicking, SlotClickingEventData.class, (data, ctx) -> {
            try {
                int idx = data.getSlotIndex();
                List<String[]> cache = this.companionInventoryCache.get(playerId);
                if (cache == null || idx < 0 || idx >= cache.size()) {
                    playerRef.sendMessage(Message.raw((String)"Empty slot."));
                    return;
                }
                String[] item = cache.get(idx);
                String assetId = item[0];
                PlayerCompanionData pData = this.companionManager.getOrCreate(playerId);
                CompanionRecord companion = this.resolveCompanion(pData);
                if (companion == null) {
                    playerRef.sendMessage(Message.raw((String)"No companion selected."));
                    return;
                }
                int moved = this.companionSystem.takeSpecificItem(playerRef, companion, assetId);
                if (moved <= 0) {
                    playerRef.sendMessage(Message.raw((String)"Could not take item."));
                    return;
                }
                playerRef.sendMessage(Message.raw((String)("Took " + EquipmentSlot.getItemDisplayName(assetId) + " x" + moved + ".")));
                this.saveSilently();
                this.rebuildGrids((UIContext)ctx, playerRef, store);
            }
            catch (Throwable t) {
                ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Error handling companion item click");
            }
        });
    }

    private void registerEquipSlotClick(PageBuilder pb, String id, EquipmentSlot slot, PlayerRef playerRef, Store<EntityStore> store) {
        UUID playerId = playerRef.getUuid();
        pb.addEventListener(id, CustomUIEventBindingType.SlotClicking, SlotClickingEventData.class, (data, ctx) -> {
            try {
                PlayerCompanionData pData = this.companionManager.getOrCreate(playerId);
                CompanionRecord companion = this.resolveCompanion(pData);
                if (companion == null) {
                    return;
                }
                String equipped = companion.getEquipped(slot);
                if (equipped == null) {
                    playerRef.sendMessage(Message.raw((String)(slot.getDisplayName() + " slot is empty.")));
                    return;
                }
                NPCEntity npcEntity = companion.active ? this.companionSystem.getNpcEntity(playerRef, companion) : null;
                String err = this.equipmentManager.unequipItem(companion, slot, playerRef, npcEntity, store);
                if (err != null) {
                    playerRef.sendMessage(Message.raw((String)err));
                    return;
                }
                playerRef.sendMessage(Message.raw((String)("Unequipped from " + slot.getDisplayName() + ".")));
                this.saveSilently();
                this.rebuildGrids((UIContext)ctx, playerRef, store);
            }
            catch (Throwable t) {
                ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Error handling unequip click");
            }
        });
    }

    private void registerControlEvents(PageBuilder pb, PlayerRef playerRef, Store<EntityStore> store) {
        UUID playerId = playerRef.getUuid();
        this.bindControlButton(pb, "btn-follow", "follow", playerRef, store);
        this.bindControlButton(pb, "btn-stay", "stay", playerRef, store);
        this.bindControlButton(pb, "btn-patrol", "patrol", playerRef, store);
        this.bindControlButton(pb, "btn-free", "free", playerRef, store);
        this.bindControlButton(pb, "btn-fighter", "fighter", playerRef, store);
        this.bindControlButton(pb, "btn-farmer", "farmer", playerRef, store);
        this.bindControlButton(pb, "btn-miner", "miner", playerRef, store);
        this.bindControlButton(pb, "btn-give", "give", playerRef, store);
        this.bindControlButton(pb, "btn-take", "take", playerRef, store);
        this.bindControlButton(pb, "btn-giveall", "giveall", playerRef, store);
        this.bindControlButton(pb, "btn-takeall", "takeall", playerRef, store);
        this.bindControlButton(pb, "btn-summon", "summon", playerRef, store);
        this.bindControlButton(pb, "btn-dismiss", "dismiss", playerRef, store);
        this.bindControlButton(pb, "btn-revive", "revive", playerRef, store);
        this.bindControlButton(pb, "btn-remove", "remove", playerRef, store);
        this.bindControlButton(pb, "btn-clear-set-boxes", "clear_set_boxes", playerRef, store);
        this.bindControlButton(pb, "btn-clear-farm-area", "clear_farm_area", playerRef, store);
        pb.addEventListener("btn-setbox", CustomUIEventBindingType.Activating, (obj, ctx) -> {
            this.setLookedAtChest(playerRef);
            this.refreshDynamicSections((UIContext)ctx, playerRef, store);
        });
        pb.addEventListener("btn-unsetbox", CustomUIEventBindingType.Activating, (obj, ctx) -> {
            this.unsetLookedAtChest(playerRef);
            this.refreshDynamicSections((UIContext)ctx, playerRef, store);
        });
        this.bindControlButton(pb, "btn-deposit", "deposit", playerRef, store);
        this.bindControlButton(pb, "btn-cmd1", "cmd_1", playerRef, store);
        this.bindControlButton(pb, "btn-cmd2", "cmd_2", playerRef, store);
        this.bindControlButton(pb, "btn-cmd3", "cmd_3", playerRef, store);
    }

    private void registerRenameEvents(PageBuilder pb, PlayerRef playerRef, Store<EntityStore> store) {
        UUID playerId = playerRef.getUuid();
        try {
            pb.addEventListener("renameInput", CustomUIEventBindingType.FocusGained, (obj, ctx) -> {
                this.textEntryFocused.add(playerId);
                this.pauseLiveUpdates(playerId, 15000L);
            });
            pb.addEventListener("renameInput", CustomUIEventBindingType.FocusLost, (obj, ctx) -> {
                this.textEntryFocused.remove(playerId);
                this.pauseLiveUpdates(playerId, 1500L);
            });
            pb.addEventListener("renameInput", CustomUIEventBindingType.ValueChanged, (obj, ctx) -> {
                this.textEntryFocused.add(playerId);
                this.pauseLiveUpdates(playerId, 15000L);
            });
            pb.addEventListener("btn-rename-ok", CustomUIEventBindingType.Activating, (obj, ctx) -> {
                try {
                    String newName;
                    this.pauseLiveUpdates(playerId, 2000L);
                    PlayerCompanionData pData = this.companionManager.getOrCreate(playerRef.getUuid());
                    CompanionRecord companion = this.resolveCompanion(pData);
                    if (companion == null) {
                        playerRef.sendMessage(Message.raw((String)"No companion selected."));
                        return;
                    }
                    String raw = ctx.getValue("renameInput", String.class).or(() -> ctx.getValue("renameInput").map(String::valueOf)).orElse("");
                    String string = newName = raw != null ? raw.trim() : "";
                    if (newName.isBlank()) {
                        playerRef.sendMessage(Message.raw((String)"Name cannot be empty."));
                        return;
                    }
                    if (newName.length() > 32) {
                        playerRef.sendMessage(Message.raw((String)"Name too long (max 32 characters)."));
                        return;
                    }
                    if (this.companionManager.isNameTaken(newName, companion.uniqueId)) {
                        playerRef.sendMessage(Message.raw((String)"That name is already taken."));
                        return;
                    }
                    this.companionSystem.renameCompanion(playerRef, companion, newName);
                    this.saveSilently();
                    playerRef.sendMessage(Message.raw((String)("Companion renamed to: " + newName)));
                    this.refreshDynamicSections((UIContext)ctx, playerRef, store);
                }
                catch (Throwable t) {
                    ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Rename via panel failed.");
                    playerRef.sendMessage(Message.raw((String)"Failed to rename companion."));
                }
            });
        }
        catch (IllegalArgumentException missingElement) {
            ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause((Throwable)missingElement)).log("Rename controls not present in panel markup; skipping rename button binding.");
        }
    }

    private void setLookedAtChest(PlayerRef playerRef) {
        World world = playerRef.getWorldUuid() != null ? Universe.get().getWorld(playerRef.getWorldUuid()) : null;
        try {
            this.logger.at(Level.INFO).log("[ChestLinkDiag] ui-link " + WorldQueries.describeStorageLookResolution(world, playerRef, 5.0));
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        BlockPos lookedAt = WorldQueries.getLookedAtStorageBlockPos(world, playerRef, 5.0);
        if (lookedAt == null) {
            playerRef.sendMessage(Message.raw((String)"Look at a storage block and try again."));
            return;
        }
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        CompanionRecord selected = data.getSelectedCompanion();
        if (selected == null) {
            playerRef.sendMessage(Message.raw((String)"No selected companion."));
            return;
        }
        if (selected.linkedChests.size() >= 10) {
            playerRef.sendMessage(Message.raw((String)"Linked chest limit reached (10)."));
            return;
        }
        if (selected.linkedChests.contains(lookedAt)) {
            playerRef.sendMessage(Message.raw((String)"That chest is already linked."));
            return;
        }
        selected.linkedChests.add(lookedAt);
        this.saveSilently();
        playerRef.sendMessage(Message.raw((String)(selected.getDisplayName() + " linked chest at (" + lookedAt.x + ", " + lookedAt.y + ", " + lookedAt.z + ").")));
    }

    private void unsetLookedAtChest(PlayerRef playerRef) {
        World world = playerRef.getWorldUuid() != null ? Universe.get().getWorld(playerRef.getWorldUuid()) : null;
        try {
            this.logger.at(Level.INFO).log("[ChestLinkDiag] ui-unlink " + WorldQueries.describeStorageLookResolution(world, playerRef, 5.0));
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        BlockPos lookedAt = WorldQueries.getLookedAtStorageBlockPos(world, playerRef, 5.0);
        if (lookedAt == null) {
            playerRef.sendMessage(Message.raw((String)"Look at a linked storage block and try again."));
            return;
        }
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        CompanionRecord selected = data.getSelectedCompanion();
        if (selected == null) {
            playerRef.sendMessage(Message.raw((String)"No selected companion."));
            return;
        }
        boolean removed = selected.linkedChests.remove(lookedAt);
        if (removed) {
            this.saveSilently();
            playerRef.sendMessage(Message.raw((String)(selected.getDisplayName() + " unlinked chest at (" + lookedAt.x + ", " + lookedAt.y + ", " + lookedAt.z + ").")));
        } else {
            playerRef.sendMessage(Message.raw((String)"That block is not currently linked."));
        }
    }

    private void bindControlButton(PageBuilder pb, String buttonId, String action, PlayerRef playerRef, Store<EntityStore> store) {
        pb.addEventListener(buttonId, CustomUIEventBindingType.Activating, (obj, ctx) -> {
            this.pauseLiveUpdates(playerRef.getUuid(), 1500L);
            this.logger.at(Level.INFO).log("[CompanionPanel] UI action " + action + " from " + buttonId);
            try {
                this.handleControlAction(action, playerRef);
            }
            catch (Throwable t) {
                ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("UI action failed: " + action);
                playerRef.sendMessage(Message.raw((String)("Action failed: " + action + ". Check server log.")));
                return;
            }
            if (!this.openPages.containsKey(playerRef.getUuid())) {
                return;
            }
            this.refreshDynamicSections((UIContext)ctx, playerRef, store);
        });
    }

    private void bindGenericButton(PageBuilder pb, String buttonId, PlayerRef playerRef, Runnable action) {
        pb.addEventListener(buttonId, CustomUIEventBindingType.Activating, (obj, ctx) -> {
            this.pauseLiveUpdates(playerRef.getUuid(), 1500L);
            action.run();
        });
    }

    private void refreshPanel(PlayerRef playerRef, Store<EntityStore> store, long delayMs) {
        UUID playerId = playerRef.getUuid();
        this.closePanel(playerId);
        CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS).execute(() -> this.runOnWorldThread(playerRef, () -> {
            try {
                this.openPanel(playerRef, store);
            }
            catch (Throwable t) {
                ((HytaleLogger.Api)this.logger.at(Level.WARNING).withCause(t)).log("Failed to refresh CompanionPanel");
            }
        }));
    }

    private void refreshDynamicSections(UIContext ctx, PlayerRef playerRef, Store<EntityStore> store) {
        UUID playerId = playerRef.getUuid();
        PlayerCompanionData pData = this.companionManager.getOrCreate(playerId);
        CompanionRecord companion = this.resolveCompanion(pData);
        this.rebuildHeaderAndControls(ctx, playerRef, pData, companion);
        this.rebuildGrids(ctx, playerRef, store);
    }

    private void toggleTooltips(PlayerRef playerRef, Store<EntityStore> store) {
        PlayerCompanionData data = this.companionManager.getOrCreate(playerRef.getUuid());
        data.uiTooltipsEnabled = !data.uiTooltipsEnabled;
        this.saveSilently();
        playerRef.sendMessage(Message.raw((String)("Companion UI tooltips: " + (data.uiTooltipsEnabled ? "ON" : "OFF"))));
        this.refreshPanel(playerRef, store, 60L);
    }

    private void rebuildHeaderAndControls(UIContext ctx, PlayerRef playerRef, PlayerCompanionData pData, CompanionRecord companion) {
        try {
            String cmd2;
            String cmd1;
            String ammo = pData != null && pData.consumeCompanionAmmo ? "Ammo: Consume" : "Ammo: Infinite";
            String tips = pData != null && pData.uiTooltipsEnabled ? "Tooltips: ON" : "Tooltips: OFF";
            ctx.getById("btn-ammo", ButtonBuilder.class).ifPresent(btn -> btn.withText(ammo));
            ctx.getById("btn-tooltips", ButtonBuilder.class).ifPresent(btn -> btn.withText(tips));
            if (companion == null) {
                ctx.getById("hdr-name", LabelBuilder.class).ifPresent(label -> label.withText("No companion selected"));
                ctx.getById("hdr-status", LabelBuilder.class).ifPresent(label -> label.withText("[-]"));
                ctx.getById("hdr-role-stance", LabelBuilder.class).ifPresent(label -> label.withText("-"));
                ctx.getById("hdr-index", LabelBuilder.class).ifPresent(label -> label.withText("(0/0)"));
                ctx.getById("hdr-death-cause", LabelBuilder.class).ifPresent(label -> label.withText(""));
                return;
            }
            String role = switch (companion.mode) {
                default -> throw new MatchException(null, null);
                case CompanionMode.FIGHTER -> "Fighter";
                case CompanionMode.FARMER -> "Farmer";
                case CompanionMode.MINER -> "Miner";
            };
            String stance = switch (companion.followMode) {
                default -> throw new MatchException(null, null);
                case FollowMode.FOLLOW -> "Follow";
                case FollowMode.STAY -> "Stay";
                case FollowMode.PATROL -> "Patrol";
                case FollowMode.FREE -> "Free";
            };
            int idx = 0;
            for (int i = 0; i < pData.companions.size(); ++i) {
                if (companion.uniqueId == null || !companion.uniqueId.equals(pData.companions.get((int)i).uniqueId)) continue;
                idx = i;
                break;
            }
            int displayIndex = idx + 1;
            ctx.getById("hdr-name", LabelBuilder.class).ifPresent(label -> label.withText(companion.getDisplayName()));
            ctx.getById("hdr-status", LabelBuilder.class).ifPresent(label -> label.withText("[" + companion.getStatusTag() + "]"));
            ctx.getById("hdr-role-stance", LabelBuilder.class).ifPresent(label -> label.withText(role + " + " + stance));
            ctx.getById("hdr-index", LabelBuilder.class).ifPresent(label -> label.withText("(" + displayIndex + "/" + pData.companions.size() + ")"));
            ctx.getById("hdr-death-cause", LabelBuilder.class).ifPresent(label -> label.withText(this.formatHeaderDeathCause(companion)));
            int chestCount = companion.linkedChests != null ? companion.linkedChests.size() : 0;
            ctx.getById("ctrl-linked-chests", LabelBuilder.class).ifPresent(label -> label.withText("Linked chests: " + chestCount));
            CompanionRuntimeState rt = this.companionManager.getRuntime(companion.uniqueId);
            ctx.getById("ctrl-farm-area", LabelBuilder.class).ifPresent(label -> label.withText((String)(companion.mode == CompanionMode.FARMER ? "Linked Farm Area: " + this.formatFarmArea(companion) : "")));
            ctx.getById("ctrl-farm-coords", LabelBuilder.class).ifPresent(label -> label.withText((String)(companion.mode == CompanionMode.FARMER ? "Farm Coordinates: " + this.formatFarmCoordinates(companion) : "")));
            ctx.getById("ctrl-farm-status", LabelBuilder.class).ifPresent(label -> label.withText((String)(companion.mode == CompanionMode.FARMER ? "Farm Status: " + this.formatFarmStatus(rt) : "")));
            ctx.getById("ctrl-command-status", LabelBuilder.class).ifPresent(label -> label.withText(this.formatCommandStatus(companion, rt)));
            ctx.getById("ctrl-command-detail", LabelBuilder.class).ifPresent(label -> label.withText(this.formatCommandDetail(companion, rt)));
            ctx.getById("stat-location", LabelBuilder.class).ifPresent(label -> label.withText(this.formatCompanionLocation(playerRef, companion)));
            String cmd3 = switch (companion.mode) {
                case CompanionMode.FIGHTER -> {
                    cmd1 = "Fight";
                    cmd2 = "Max Defend";
                    yield companion.lootModeEnabled ? "Loot: ON" : "Loot: OFF";
                }
                case CompanionMode.FARMER -> {
                    cmd1 = "Farm";
                    cmd2 = "Set TL";
                    yield "Set BR";
                }
                case CompanionMode.MINER -> {
                    cmd1 = "Mine";
                    cmd2 = "Stop";
                    yield "Mine For";
                }
                default -> {
                    cmd1 = "Cmd 1";
                    cmd2 = "Cmd 2";
                    yield "Cmd 3";
                }
            };
            ctx.getById("btn-cmd1", ButtonBuilder.class).ifPresent(btn -> btn.withText(cmd1));
            ctx.getById("btn-cmd2", ButtonBuilder.class).ifPresent(btn -> btn.withText(cmd2));
            ctx.getById("btn-cmd3", ButtonBuilder.class).ifPresent(btn -> btn.withText(cmd3));
            ctx.getById("renameInput", TextFieldBuilder.class).ifPresent(tf -> tf.withValue(companion.name != null ? companion.name : ""));
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Failed to rebuild header/controls.");
        }
    }

    private void startLiveRefresh(PlayerRef playerRef) {
        UUID playerId = playerRef.getUuid();
        long token = System.nanoTime();
        this.liveRefreshTokens.put(playerId, token);
        this.liveRefreshSnapshots.put(playerId, "");
        this.scheduleLiveRefresh(playerRef, token);
    }

    private void scheduleLiveRefresh(PlayerRef playerRef, long token) {
        CompletableFuture.delayedExecutor(400L, TimeUnit.MILLISECONDS).execute(() -> this.runOnWorldThread(playerRef, () -> {
            UUID playerId = playerRef.getUuid();
            if (!Objects.equals(this.liveRefreshTokens.get(playerId), token)) {
                return;
            }
            HyUIPage page = this.openPages.get(playerId);
            if (page == null || page.getPage().isEmpty()) {
                this.clearPanelState(playerId, page);
                return;
            }
            try {
                CompanionRecord companion;
                if (this.isLiveRefreshPaused(playerId)) {
                    if (Objects.equals(this.liveRefreshTokens.get(playerId), token) && this.openPages.containsKey(playerId)) {
                        this.scheduleLiveRefresh(playerRef, token);
                    }
                    return;
                }
                PlayerCompanionData pData = this.companionManager.getOrCreate(playerId);
                String snapshot = this.buildLiveRefreshSnapshot(playerRef, pData, companion = this.resolveCompanion(pData));
                if (!Objects.equals(snapshot, this.liveRefreshSnapshots.get(playerId))) {
                    if (page != this.openPages.get(playerId) || page.getPage().isEmpty()) {
                        this.clearPanelState(playerId, page);
                        return;
                    }
                    this.liveRefreshSnapshots.put(playerId, snapshot);
                    this.rebuildLiveStatus(page, playerRef, pData, companion);
                    this.rebuildLiveInventory(page, playerRef);
                    if (page == this.openPages.get(playerId) && page.getPage().isPresent()) {
                        page.updatePage(false);
                    }
                }
            }
            catch (Throwable t) {
                this.clearPanelState(playerId, page);
                ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Live CompanionPanel refresh failed.");
                return;
            }
            if (Objects.equals(this.liveRefreshTokens.get(playerId), token) && this.openPages.containsKey(playerId)) {
                this.scheduleLiveRefresh(playerRef, token);
            }
        }));
    }

    private void pauseLiveUpdates(UUID playerId, long durationMs) {
        long until = System.currentTimeMillis() + Math.max(0L, durationMs);
        this.interactionPauseUntilMs.merge(playerId, until, Math::max);
    }

    private boolean isLiveRefreshPaused(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        if (this.textEntryFocused.contains(playerId)) {
            return true;
        }
        long pauseUntil = this.interactionPauseUntilMs.getOrDefault(playerId, 0L);
        return System.currentTimeMillis() < pauseUntil;
    }

    private String buildLiveRefreshSnapshot(PlayerRef playerRef, PlayerCompanionData pData, CompanionRecord companion) {
        if (companion == null) {
            return "none";
        }
        CompanionRuntimeState rt = this.companionManager.getRuntime(companion.uniqueId);
        int idx = 0;
        if (pData != null) {
            for (int i = 0; i < pData.companions.size(); ++i) {
                if (companion.uniqueId == null || !companion.uniqueId.equals(pData.companions.get((int)i).uniqueId)) continue;
                idx = i + 1;
                break;
            }
        }
        int chestCount = companion.linkedChests != null ? companion.linkedChests.size() : 0;
        return String.join((CharSequence)"|", companion.getDisplayName(), companion.getStatusTag(), String.valueOf((Object)companion.mode), String.valueOf((Object)companion.followMode), String.valueOf(companion.combatLevel), String.valueOf(companion.combatKills), String.valueOf(companion.farmLevel), String.valueOf(companion.farmHarvests), String.valueOf(companion.mineLevel), String.valueOf(companion.mineBlocks), String.valueOf(idx), pData != null ? String.valueOf(pData.companions.size()) : "0", String.valueOf(chestCount), this.formatFarmArea(companion), this.formatFarmCoordinates(companion), this.formatFarmStatus(rt), this.formatCommandStatus(companion, rt), this.formatCommandDetail(companion, rt), this.formatCompanionLocation(playerRef, companion), this.buildInventorySnapshot(companion, playerRef));
    }

    private void rebuildLiveStatus(UIContext ctx, PlayerRef playerRef, PlayerCompanionData pData, CompanionRecord companion) {
        try {
            if (companion == null) {
                ctx.getById("hdr-name", LabelBuilder.class).ifPresent(label -> label.withText("No companion selected"));
                ctx.getById("hdr-status", LabelBuilder.class).ifPresent(label -> label.withText("[-]"));
                ctx.getById("hdr-role-stance", LabelBuilder.class).ifPresent(label -> label.withText("-"));
                ctx.getById("hdr-index", LabelBuilder.class).ifPresent(label -> label.withText("(0/0)"));
                ctx.getById("hdr-death-cause", LabelBuilder.class).ifPresent(label -> label.withText(""));
                ctx.getById("ctrl-linked-chests", LabelBuilder.class).ifPresent(label -> label.withText("Linked chests: 0"));
                ctx.getById("ctrl-farm-area", LabelBuilder.class).ifPresent(label -> label.withText(""));
                ctx.getById("ctrl-farm-coords", LabelBuilder.class).ifPresent(label -> label.withText(""));
                ctx.getById("ctrl-farm-status", LabelBuilder.class).ifPresent(label -> label.withText(""));
                ctx.getById("ctrl-command-status", LabelBuilder.class).ifPresent(label -> label.withText(""));
                ctx.getById("ctrl-command-detail", LabelBuilder.class).ifPresent(label -> label.withText(""));
                ctx.getById("stat-location", LabelBuilder.class).ifPresent(label -> label.withText("Location: --"));
                return;
            }
            String role = switch (companion.mode) {
                default -> throw new MatchException(null, null);
                case CompanionMode.FIGHTER -> "Fighter";
                case CompanionMode.FARMER -> "Farmer";
                case CompanionMode.MINER -> "Miner";
            };
            String stance = switch (companion.followMode) {
                default -> throw new MatchException(null, null);
                case FollowMode.FOLLOW -> "Follow";
                case FollowMode.STAY -> "Stay";
                case FollowMode.PATROL -> "Patrol";
                case FollowMode.FREE -> "Free";
            };
            int idx = 0;
            if (pData != null) {
                for (int i = 0; i < pData.companions.size(); ++i) {
                    if (companion.uniqueId == null || !companion.uniqueId.equals(pData.companions.get((int)i).uniqueId)) continue;
                    idx = i + 1;
                    break;
                }
            }
            int total = pData != null ? pData.companions.size() : 0;
            int displayIndex = idx;
            int chestCount = companion.linkedChests != null ? companion.linkedChests.size() : 0;
            CompanionRuntimeState rt = this.companionManager.getRuntime(companion.uniqueId);
            ctx.getById("hdr-name", LabelBuilder.class).ifPresent(label -> label.withText(companion.getDisplayName()));
            ctx.getById("hdr-status", LabelBuilder.class).ifPresent(label -> label.withText("[" + companion.getStatusTag() + "]"));
            ctx.getById("hdr-role-stance", LabelBuilder.class).ifPresent(label -> label.withText(role + " + " + stance));
            ctx.getById("hdr-index", LabelBuilder.class).ifPresent(label -> label.withText("(" + displayIndex + "/" + total + ")"));
            ctx.getById("hdr-death-cause", LabelBuilder.class).ifPresent(label -> label.withText(this.formatHeaderDeathCause(companion)));
            ctx.getById("ctrl-linked-chests", LabelBuilder.class).ifPresent(label -> label.withText("Linked chests: " + chestCount));
            ctx.getById("ctrl-farm-area", LabelBuilder.class).ifPresent(label -> label.withText((String)(companion.mode == CompanionMode.FARMER ? "Linked Farm Area: " + this.formatFarmArea(companion) : "")));
            ctx.getById("ctrl-farm-coords", LabelBuilder.class).ifPresent(label -> label.withText((String)(companion.mode == CompanionMode.FARMER ? "Farm Coordinates: " + this.formatFarmCoordinates(companion) : "")));
            ctx.getById("ctrl-farm-status", LabelBuilder.class).ifPresent(label -> label.withText((String)(companion.mode == CompanionMode.FARMER ? "Farm Status: " + this.formatFarmStatus(rt) : "")));
            ctx.getById("ctrl-command-status", LabelBuilder.class).ifPresent(label -> label.withText(this.formatCommandStatus(companion, rt)));
            ctx.getById("ctrl-command-detail", LabelBuilder.class).ifPresent(label -> label.withText(this.formatCommandDetail(companion, rt)));
            ctx.getById("stat-location", LabelBuilder.class).ifPresent(label -> label.withText(this.formatCompanionLocation(playerRef, companion)));
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Failed to rebuild live panel status.");
        }
    }

    private void rebuildLiveInventory(UIContext ctx, PlayerRef playerRef) {
        try {
            UUID playerId = playerRef.getUuid();
            PlayerCompanionData pData = this.companionManager.getOrCreate(playerId);
            CompanionRecord companion = this.resolveCompanion(pData);
            this.rebuildCompanionInventoryGrid(ctx, companion, playerRef, pData);
            this.rebuildStatsLabels(ctx, playerRef, companion);
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Failed to rebuild live panel inventory.");
        }
    }

    private String buildInventorySnapshot(CompanionRecord companion, PlayerRef playerRef) {
        List<String[]> items = this.collectCompanionInventoryItems(companion, playerRef);
        if (items == null || items.isEmpty()) {
            return "inv:none";
        }
        StringBuilder sb = new StringBuilder(items.size() * 24);
        for (String[] entry : items) {
            if (entry == null) continue;
            String itemId = entry.length > 0 && entry[0] != null ? entry[0] : "";
            String qty = entry.length > 2 && entry[2] != null ? entry[2] : "1";
            sb.append(itemId).append('=').append(qty).append(';');
        }
        return sb.toString();
    }

    private void runOnWorldThread(PlayerRef playerRef, Runnable task) {
        try {
            World world = Universe.get().getWorld(playerRef.getWorldUuid());
            if (world != null) {
                world.execute(task);
                return;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        task.run();
    }

    private void handleControlAction(String action, PlayerRef playerRef) {
        String feedback;
        UUID playerId = playerRef.getUuid();
        PlayerCompanionData pData = this.companionManager.getOrCreate(playerId);
        CompanionRecord companion = this.resolveCompanion(pData);
        if (companion == null) {
            playerRef.sendMessage(Message.raw((String)"No companion selected."));
            return;
        }
        switch (action) {
            case "follow": {
                this.companionSystem.applyStance(playerRef, companion, FollowMode.FOLLOW);
                this.saveSilently();
                Object object = companion.getDisplayName() + " set to Follow.";
                break;
            }
            case "stay": {
                this.companionSystem.applyStance(playerRef, companion, FollowMode.STAY);
                this.saveSilently();
                Object object = companion.getDisplayName() + " set to Stay.";
                break;
            }
            case "patrol": {
                this.companionSystem.applyStance(playerRef, companion, FollowMode.PATROL);
                this.saveSilently();
                Object object = companion.getDisplayName() + " set to Patrol.";
                break;
            }
            case "free": {
                this.companionSystem.applyStance(playerRef, companion, FollowMode.FREE);
                this.saveSilently();
                Object object = companion.getDisplayName() + " set to Free.";
                break;
            }
            case "fighter": {
                companion.mode = CompanionMode.FIGHTER;
                companion.farmAutoResume = false;
                CompanionRuntimeState rt = this.companionManager.getRuntime(companion.uniqueId);
                rt.commandActive = false;
                String eq = companion.active ? this.companionSystem.switchRoleEquipment(playerRef, companion) : null;
                this.saveSilently();
                Object object = companion.getDisplayName() + " set to Fighter." + (String)(eq != null ? " " + eq : "");
                break;
            }
            case "farmer": {
                companion.mode = CompanionMode.FARMER;
                String eq = companion.active ? this.companionSystem.switchRoleEquipment(playerRef, companion) : null;
                this.saveSilently();
                Object object = companion.getDisplayName() + " set to Farmer." + (String)(eq != null ? " " + eq : "");
                break;
            }
            case "miner": {
                companion.mode = CompanionMode.MINER;
                companion.farmAutoResume = false;
                CompanionRuntimeState rt = this.companionManager.getRuntime(companion.uniqueId);
                rt.commandActive = false;
                String eq = companion.active ? this.companionSystem.switchRoleEquipment(playerRef, companion) : null;
                this.saveSilently();
                Object object = companion.getDisplayName() + " set to Miner." + (String)(eq != null ? " " + eq : "");
                break;
            }
            case "give": {
                Object object;
                if (!companion.active) {
                    object = "Companion not active.";
                    break;
                }
                if (this.companionSystem.giveHandItem(playerRef, companion)) {
                    object = "Item given.";
                    break;
                }
                object = "Could not give item.";
                break;
            }
            case "take": {
                Object object;
                if (!companion.active) {
                    object = "Companion not active.";
                    break;
                }
                if (this.companionSystem.takeHandItem(playerRef, companion)) {
                    object = "Item taken.";
                    break;
                }
                object = "Could not take item.";
                break;
            }
            case "giveall": {
                Object object;
                if (!companion.active) {
                    object = "Companion not active.";
                    break;
                }
                int c = this.companionSystem.giveAllItems(playerRef, companion);
                if (c > 0) {
                    object = "Gave " + c + " stack(s).";
                    break;
                }
                object = "No items to give.";
                break;
            }
            case "takeall": {
                Object object;
                if (!companion.active) {
                    object = "Companion not active.";
                    break;
                }
                int c = this.companionSystem.takeAllItems(playerRef, companion);
                if (c > 0) {
                    object = "Took " + c + " stack(s).";
                    break;
                }
                object = "No items to take.";
                break;
            }
            case "summon": {
                Object object;
                if (companion.fallen) {
                    object = companion.getDisplayName() + " is fallen. Use Revive.";
                    break;
                }
                if (companion.active) {
                    object = companion.getDisplayName() + " is already active.";
                    break;
                }
                boolean ok = this.companionSystem.summonCompanion(playerRef, companion);
                this.saveSilently();
                if (ok) {
                    object = companion.getDisplayName() + " summoned.";
                    break;
                }
                object = "Failed to summon.";
                break;
            }
            case "dismiss": {
                Object object;
                if (!companion.active) {
                    object = "Companion not active.";
                    break;
                }
                this.companionSystem.dismissCompanion(playerRef, companion);
                this.saveSilently();
                object = companion.getDisplayName() + " dismissed.";
                break;
            }
            case "revive": {
                Object object;
                if (!companion.fallen) {
                    object = companion.getDisplayName() + " is not fallen.";
                    break;
                }
                String reviveError = this.companionSystem.tryReviveCompanion(playerRef, companion, true);
                if (reviveError != null) {
                    object = reviveError;
                    break;
                }
                this.saveSilently();
                object = companion.getDisplayName() + " revived!";
                break;
            }
            case "reroll": {
                Object object;
                if (!companion.active) {
                    object = "Companion not active.";
                    break;
                }
                String modelId = this.companionSystem.rerollAppearance(playerRef, companion);
                if (modelId != null) {
                    object = "Appearance rerolled.";
                    break;
                }
                object = "Failed to reroll.";
                break;
            }
            case "remove": {
                Object object;
                int idx = -1;
                for (int i = 0; i < pData.companions.size(); ++i) {
                    CompanionRecord r = pData.companions.get(i);
                    if (r == null || companion.uniqueId == null || !companion.uniqueId.equals(r.uniqueId)) continue;
                    idx = i;
                    break;
                }
                if (idx < 0) {
                    object = "Companion not found.";
                    break;
                }
                if (companion.active) {
                    this.companionSystem.dismissCompanion(playerRef, companion);
                }
                this.companionManager.clearRuntime(companion.uniqueId);
                String removedName = companion.getDisplayName();
                pData.companions.remove(idx);
                if (pData.companions.isEmpty()) {
                    pData.selectedCompanionId = null;
                    this.saveSilently();
                    this.closePanel(playerId);
                    object = removedName + " removed.";
                    break;
                }
                int newIdx = Math.max(0, idx - 1);
                CompanionRecord selected = pData.companions.get(newIdx);
                pData.selectedCompanionId = selected.uniqueId;
                this.saveSilently();
                object = removedName + " removed. Selected: " + selected.getDisplayName();
                break;
            }
            case "deposit": {
                CompanionRuntimeState rt = this.companionManager.getRuntime(companion.uniqueId);
                rt.depositRequested = true;
                Object object = "Deposit requested for " + companion.getDisplayName() + ".";
                break;
            }
            case "clear_set_boxes": {
                companion.linkedChests.clear();
                CompanionRuntimeState rt = this.companionManager.getRuntime(companion.uniqueId);
                rt.depositRequested = false;
                this.saveSilently();
                Object object = "Cleared linked chests for " + companion.getDisplayName() + ".";
                break;
            }
            case "clear_farm_area": {
                companion.farmAreaTopLeft = null;
                companion.farmAreaBottomRight = null;
                CompanionRuntimeState rt = this.companionManager.getRuntime(companion.uniqueId);
                rt.farmTargetPos = null;
                rt.farmTaskType = null;
                rt.farmPlantSeedId = null;
                rt.farmStatusText = "Farm area cleared";
                this.saveSilently();
                Object object = "Cleared farm area for " + companion.getDisplayName() + ".";
                break;
            }
            case "cmd_1": {
                Object object = this.handleRoleCmd("cmd_1", companion, pData, playerRef);
                break;
            }
            case "cmd_2": {
                Object object = this.handleRoleCmd("cmd_2", companion, pData, playerRef);
                break;
            }
            case "cmd_3": {
                Object object = this.handleRoleCmd("cmd_3", companion, pData, playerRef);
                break;
            }
            default: {
                Object object = feedback = "Unknown action.";
            }
        }
        if (feedback != null) {
            playerRef.sendMessage(Message.raw((String)feedback));
        }
    }

    private String handleRoleCmd(String cmd, CompanionRecord companion, PlayerCompanionData pData, PlayerRef playerRef) {
        if (!companion.active) {
            return "Companion not active.";
        }
        CompanionRuntimeState rt = this.companionManager.getRuntime(companion.uniqueId);
        return switch (companion.mode) {
            default -> throw new MatchException(null, null);
            case CompanionMode.FIGHTER -> {
                switch (cmd) {
                    case "cmd_1": {
                        this.companionSystem.prepareIssuedCommand(playerRef, companion);
                        rt.maxDefendMode = false;
                        rt.commandActive = true;
                        yield companion.getDisplayName() + " set to Attack.";
                    }
                    case "cmd_2": {
                        this.companionSystem.prepareIssuedCommand(playerRef, companion);
                        rt.maxDefendMode = true;
                        rt.commandActive = true;
                        yield companion.getDisplayName() + " set to Max Defend.";
                    }
                    case "cmd_3": {
                        boolean v1 = companion.lootModeEnabled = !companion.lootModeEnabled;
                        if (!companion.lootModeEnabled) {
                            this.companionSystem.clearLootState(playerRef, companion);
                        }
                        this.saveSilently();
                        yield companion.getDisplayName() + " loot pickup " + (companion.lootModeEnabled ? "enabled." : "disabled.");
                    }
                }
                yield "Unknown.";
            }
            case CompanionMode.FARMER -> {
                switch (cmd) {
                    case "cmd_1": {
                        this.companionSystem.prepareIssuedCommand(playerRef, companion);
                        rt.commandActive = true;
                        companion.farmAutoResume = true;
                        this.companionSystem.suspendOwnerFollowTargetingForActiveWork(playerRef, companion);
                        rt.lastFarmScanTick = -1000000L;
                        rt.farmTargetPos = null;
                        rt.farmTaskType = null;
                        rt.farmPlantSeedId = null;
                        rt.farmStatusText = "Starting";
                        this.saveSilently();
                        this.logger.at(Level.INFO).log("[FarmDiag] cmd_1 start | companion=" + companion.uniqueId + " mode=" + String.valueOf((Object)companion.mode) + " follow=" + String.valueOf((Object)companion.followMode) + " commandActive=" + rt.commandActive + " areaSet=" + (companion.farmAreaTopLeft != null && companion.farmAreaBottomRight != null));
                        yield companion.getDisplayName() + " farming active.";
                    }
                    case "cmd_2": {
                        BlockPos look = this.resolveFarmCornerTarget(playerRef);
                        if (look == null) {
                            yield "Look at a block to set Farm TL.";
                        }
                        companion.farmAreaTopLeft = look;
                        this.normalizeFarmAreaCorners(companion);
                        rt.lastFarmScanTick = -1000000L;
                        rt.farmTargetPos = null;
                        rt.farmTaskType = null;
                        rt.farmPlantSeedId = null;
                        rt.farmStatusText = "Farm TL set";
                        this.saveSilently();
                        if (companion.farmAreaBottomRight != null) {
                            int spanX = Math.abs(companion.farmAreaBottomRight.x - companion.farmAreaTopLeft.x) + 1;
                            int spanZ = Math.abs(companion.farmAreaBottomRight.z - companion.farmAreaTopLeft.z) + 1;
                            int total = spanX * spanZ;
                            this.logger.at(Level.INFO).log("[FarmDiag] corner TL set | companion=" + companion.uniqueId + " TL=(" + companion.farmAreaTopLeft.x + "," + companion.farmAreaTopLeft.y + "," + companion.farmAreaTopLeft.z + ") BR=(" + companion.farmAreaBottomRight.x + "," + companion.farmAreaBottomRight.y + "," + companion.farmAreaBottomRight.z + ") area=" + spanX + "x" + spanZ + " (" + total + ")");
                            yield "Farm TL set: (" + look.x + ", " + look.y + ", " + look.z + "). Area " + spanX + "x" + spanZ + " (" + total + " blocks).";
                        }
                        this.logger.at(Level.INFO).log("[FarmDiag] corner TL set | companion=" + companion.uniqueId + " TL=(" + companion.farmAreaTopLeft.x + "," + companion.farmAreaTopLeft.y + "," + companion.farmAreaTopLeft.z + ")");
                        yield "Farm TL set: (" + look.x + ", " + look.y + ", " + look.z + ").";
                    }
                    case "cmd_3": {
                        BlockPos look = this.resolveFarmCornerTarget(playerRef);
                        if (look == null) {
                            yield "Look at a block to set Farm BR.";
                        }
                        companion.farmAreaBottomRight = look;
                        this.normalizeFarmAreaCorners(companion);
                        rt.lastFarmScanTick = -1000000L;
                        rt.farmTargetPos = null;
                        rt.farmTaskType = null;
                        rt.farmPlantSeedId = null;
                        rt.farmStatusText = "Farm BR set";
                        this.saveSilently();
                        if (companion.farmAreaTopLeft != null) {
                            int spanX = Math.abs(companion.farmAreaBottomRight.x - companion.farmAreaTopLeft.x) + 1;
                            int spanZ = Math.abs(companion.farmAreaBottomRight.z - companion.farmAreaTopLeft.z) + 1;
                            int total = spanX * spanZ;
                            this.logger.at(Level.INFO).log("[FarmDiag] corner BR set | companion=" + companion.uniqueId + " TL=(" + companion.farmAreaTopLeft.x + "," + companion.farmAreaTopLeft.y + "," + companion.farmAreaTopLeft.z + ") BR=(" + companion.farmAreaBottomRight.x + "," + companion.farmAreaBottomRight.y + "," + companion.farmAreaBottomRight.z + ") area=" + spanX + "x" + spanZ + " (" + total + ")");
                            yield "Farm BR set: (" + look.x + ", " + look.y + ", " + look.z + "). Area " + spanX + "x" + spanZ + " (" + total + " blocks).";
                        }
                        this.logger.at(Level.INFO).log("[FarmDiag] corner BR set | companion=" + companion.uniqueId + " BR=(" + companion.farmAreaBottomRight.x + "," + companion.farmAreaBottomRight.y + "," + companion.farmAreaBottomRight.z + ")");
                        yield "Farm BR set: (" + look.x + ", " + look.y + ", " + look.z + ").";
                    }
                }
                yield "Unknown.";
            }
            case CompanionMode.MINER -> {
                switch (cmd) {
                    case "cmd_1": {
                        BlockPos lookBlock;
                        this.companionSystem.prepareIssuedCommand(playerRef, companion);
                        World mineWorld = Universe.get().getWorld(playerRef.getWorldUuid());
                        BlockPos v2 = lookBlock = mineWorld != null ? WorldQueries.getRobustLookBlockPos(mineWorld, playerRef, 20.0) : null;
                        if (lookBlock == null) {
                            yield "Look at a block to mine.";
                        }
                        yield this.companionSystem.startDirectionalMine(playerRef, companion, lookBlock.x, lookBlock.y, lookBlock.z);
                    }
                    case "cmd_2": {
                        this.companionSystem.prepareIssuedCommand(playerRef, companion);
                        String msg = this.companionSystem.stopMining(playerRef, companion);
                        this.companionSystem.stopCompanionMotionNow(playerRef, companion);
                        yield msg;
                    }
                    case "cmd_3": {
                        String blockType;
                        BlockPos lookBlock;
                        this.companionSystem.prepareIssuedCommand(playerRef, companion);
                        World mineWorld = Universe.get().getWorld(playerRef.getWorldUuid());
                        BlockPos v3 = lookBlock = mineWorld != null ? WorldQueries.getRobustLookBlockPos(mineWorld, playerRef, 20.0) : null;
                        if (lookBlock == null) {
                            yield "Look directly at the block to mine for.";
                        }
                        String v4 = blockType = mineWorld != null ? WorldQueries.getBlockTypeAt(mineWorld, lookBlock.x, lookBlock.y, lookBlock.z) : null;
                        if (blockType == null) {
                            yield "Could not identify block type.";
                        }
                        String canonicalMineFor = this.canonicalBlockIdForMine(blockType);
                        if (canonicalMineFor == null || canonicalMineFor.isBlank() || this.isAirLikeMineTarget(canonicalMineFor)) {
                            this.logger.at(Level.INFO).log("[MineFor] rejected target at (" + lookBlock.x + "," + lookBlock.y + "," + lookBlock.z + ") raw=" + blockType + " canonical=" + canonicalMineFor);
                            yield "That block is not mineable (air/empty).";
                        }
                        this.logger.at(Level.INFO).log("[MineFor] target set at (" + lookBlock.x + "," + lookBlock.y + "," + lookBlock.z + ") raw=" + blockType + " canonical=" + canonicalMineFor);
                        yield this.companionSystem.startMineForBlock(playerRef, companion, canonicalMineFor, lookBlock.x, lookBlock.y, lookBlock.z);
                    }
                }
                yield "Unknown.";
            }
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

    private void rebuildGrids(UIContext ctx, PlayerRef playerRef, Store<EntityStore> store) {
        UUID playerId = playerRef.getUuid();
        PlayerCompanionData pData = this.companionManager.getOrCreate(playerId);
        CompanionRecord companion = this.resolveCompanion(pData);
        List<String[]> inventoryItems = this.equipmentManager.collectInventoryItems(playerRef, store);
        this.inventoryCache.put(playerId, inventoryItems);
        this.rebuildEquipSlot(ctx, companion, EquipmentSlot.HELMET, "equip-slot-head", "Head", pData);
        this.rebuildEquipSlot(ctx, companion, EquipmentSlot.CHESTPLATE, "equip-slot-chest", "Chest", pData);
        this.rebuildEquipSlot(ctx, companion, EquipmentSlot.LEGGINGS, "equip-slot-legs", "Legs", pData);
        this.rebuildEquipSlot(ctx, companion, EquipmentSlot.BOOTS, "equip-slot-gloves", "Gloves", pData);
        this.rebuildEquipSlot(ctx, companion, EquipmentSlot.WEAPON, "equip-slot-weapon", "Weapon", pData);
        this.rebuildEquipSlot(ctx, companion, EquipmentSlot.OFFHAND, "equip-slot-offhand", "Offhand", pData);
        this.rebuildCompanionInventoryGrid(ctx, companion, playerRef, pData);
        this.rebuildPlayerGrid(ctx, inventoryItems, pData);
        this.rebuildStatsLabels(ctx, playerRef, companion);
        ctx.updatePage(true);
    }

    private void rebuildStatsLabels(UIContext ctx, PlayerRef playerRef, CompanionRecord companion) {
        if (companion == null) {
            return;
        }
        try {
            int combatIdx = Math.max(0, Math.min(companion.combatLevel - 1, ProgressionConfig.COMBAT_LEVEL_NAMES.length - 1));
            int farmIdx = Math.max(0, Math.min(companion.farmLevel - 1, ProgressionConfig.FARM_LEVEL_NAMES.length - 1));
            int mineIdx = Math.max(0, Math.min(companion.mineLevel - 1, ProgressionConfig.MINE_LEVEL_NAMES.length - 1));
            ctx.getById("stat-combat-level", LabelBuilder.class).ifPresent(label -> label.withText("Level " + companion.combatLevel + " - " + ProgressionConfig.COMBAT_LEVEL_NAMES[combatIdx]));
            ctx.getById("stat-combat-kills", LabelBuilder.class).ifPresent(label -> label.withText("Kills: " + companion.combatKills));
            ctx.getById("stat-farm-level", LabelBuilder.class).ifPresent(label -> label.withText("Level " + companion.farmLevel + " - " + ProgressionConfig.FARM_LEVEL_NAMES[farmIdx]));
            ctx.getById("stat-farm-harvests", LabelBuilder.class).ifPresent(label -> label.withText("Harvests: " + companion.farmHarvests));
            ctx.getById("stat-mine-level", LabelBuilder.class).ifPresent(label -> label.withText("Level " + companion.mineLevel + " - " + ProgressionConfig.MINE_LEVEL_NAMES[mineIdx]));
            ctx.getById("stat-mine-blocks", LabelBuilder.class).ifPresent(label -> label.withText("Blocks mined: " + companion.mineBlocks));
            int blocksNeeded = ProgressionConfig.blocksToNextMineLevel(companion.mineLevel, companion.mineBlocks);
            ctx.getById("stat-mine-next", LabelBuilder.class).ifPresent(label -> label.withText(this.formatMineNextLevelText(blocksNeeded)));
            double mineMult = ProgressionConfig.MINE_SPEED_MULT[Math.min(mineIdx, ProgressionConfig.MINE_SPEED_MULT.length - 1)];
            ctx.getById("stat-mine-speed", LabelBuilder.class).ifPresent(label -> label.withText(this.formatPercentBonusText("Mining speed", mineMult)));
            double mineMoveMult = ProgressionConfig.MINE_MOVE_SPEED_MULT[Math.min(mineIdx, ProgressionConfig.MINE_MOVE_SPEED_MULT.length - 1)];
            ctx.getById("stat-mine-move-speed", LabelBuilder.class).ifPresent(label -> label.withText(this.formatPercentBonusText("Move speed", mineMoveMult)));
            ctx.getById("stat-deaths", LabelBuilder.class).ifPresent(label -> label.withText("Deaths: " + companion.deathCount));
            ctx.getById("stat-location", LabelBuilder.class).ifPresent(label -> label.withText(this.formatCompanionLocation(playerRef, companion)));
            String[] eqStats = this.companionSystem.getEquipmentStats(playerRef, companion);
            if (eqStats == null || eqStats.length < 4) {
                return;
            }
            ctx.getById("stat-weapon", LabelBuilder.class).ifPresent(label -> label.withText("Weapon: " + eqStats[0] + " (Atk: " + eqStats[2] + ")"));
            ctx.getById("stat-armor", LabelBuilder.class).ifPresent(label -> label.withText("Armor: " + eqStats[1] + " (Def: " + eqStats[3] + ")"));
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Failed to rebuild stat labels.");
        }
    }

    private String formatMineNextLevelText(int blocksNeeded) {
        if (blocksNeeded == -1) {
            return "MAX LEVEL";
        }
        if (blocksNeeded > 0) {
            return "Next level in " + blocksNeeded + " blocks";
        }
        return "Next level in 0 blocks";
    }

    private String formatPercentBonusText(String label, double multiplier) {
        int bonus = (int)Math.round((multiplier - 1.0) * 100.0);
        if (bonus < 0) {
            bonus = 0;
        }
        return label + ": +" + bonus + "%";
    }

    private void rebuildCompanionInventoryGrid(UIContext ctx, CompanionRecord companion, PlayerRef playerRef, PlayerCompanionData pData) {
        try {
            List<String[]> companionItems = this.collectCompanionInventoryItems(companion, playerRef);
            this.companionInventoryCache.put(playerRef.getUuid(), companionItems);
            boolean tipsOn = pData != null && pData.uiTooltipsEnabled;
            ctx.getById("companion-inv-grid", ItemGridBuilder.class).ifPresent(grid -> {
                List<ItemGridSlot> slots = grid.getSlots();
                for (int i = slots.size() - 1; i >= 0; --i) {
                    grid.removeSlot(i);
                }
                int count = 0;
                for (String[] entry : companionItems) {
                    if (count >= 27) break;
                    ItemGridSlot gs = new ItemGridSlot(new ItemStack(entry[0], Math.max(1, this.parseQty(entry[2]))));
                    if (tipsOn) {
                        gs.setName(entry[1] + " x" + entry[2]);
                    }
                    gs.setActivatable(true);
                    grid.addSlot(gs);
                    ++count;
                }
                for (int i = count; i < 27; ++i) {
                    grid.addSlot(new ItemGridSlot());
                }
            });
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Failed to rebuild companion inv grid");
        }
    }

    private int parseQty(String qtyText) {
        try {
            return Integer.parseInt(qtyText);
        }
        catch (Throwable ignored) {
            return 1;
        }
    }

    private List<String[]> collectCompanionInventoryItems(CompanionRecord companion, PlayerRef playerRef) {
        if (companion == null) {
            return Collections.emptyList();
        }
        Map<String, Integer> inv = null;
        List<String[]> liveStacks = null;
        boolean liveReadAttempted = false;
        if (companion.active) {
            liveReadAttempted = true;
            liveStacks = this.companionSystem.getLiveInventoryStacks(playerRef, companion);
            inv = this.companionSystem.getLiveInventory(playerRef, companion);
        }
        if (liveStacks != null && !liveStacks.isEmpty()) {
            List<String[]> filteredStacks = this.subtractEquippedFromStackList(liveStacks, companion);
            if (filteredStacks.size() > 27) {
                return new ArrayList<String[]>(filteredStacks.subList(0, 27));
            }
            return filteredStacks;
        }
        if (companion.savedInventoryStacks != null && !companion.savedInventoryStacks.isEmpty()) {
            List<String[]> filteredSavedStacks = this.subtractEquippedFromStackList(companion.savedInventoryStacks, companion);
            if (filteredSavedStacks.size() > 27) {
                return new ArrayList<String[]>(filteredSavedStacks.subList(0, 27));
            }
            return filteredSavedStacks;
        }
        if (inv == null || inv.isEmpty()) {
            inv = companion.savedInventory;
        }
        if (inv == null || inv.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashMap<String, Integer> filtered = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> entry : inv.entrySet()) {
            int qty;
            if (entry == null || entry.getKey() == null || (qty = Math.max(0, entry.getValue() != null ? entry.getValue() : 0)) <= 0) continue;
            filtered.put(entry.getKey(), qty);
        }
        this.subtractEquippedFromInventory(filtered, companion.getEquipped(EquipmentSlot.HELMET));
        this.subtractEquippedFromInventory(filtered, companion.getEquipped(EquipmentSlot.CHESTPLATE));
        this.subtractEquippedFromInventory(filtered, companion.getEquipped(EquipmentSlot.LEGGINGS));
        this.subtractEquippedFromInventory(filtered, companion.getEquipped(EquipmentSlot.BOOTS));
        this.subtractEquippedFromInventory(filtered, companion.getEquipped(EquipmentSlot.WEAPON));
        this.subtractEquippedFromInventory(filtered, companion.getEquipped(EquipmentSlot.OFFHAND));
        ArrayList<String[]> items = new ArrayList<String[]>();
        for (Map.Entry entry : filtered.entrySet()) {
            if (entry == null || entry.getKey() == null) continue;
            int qty = Math.max(1, entry.getValue() != null ? (Integer)entry.getValue() : 1);
            items.add(new String[]{(String)entry.getKey(), EquipmentSlot.getItemDisplayName((String)entry.getKey()), Integer.toString(qty)});
            if (items.size() < 27) continue;
            break;
        }
        return items;
    }

    private List<String[]> subtractEquippedFromStackList(List<String[]> stacks, CompanionRecord companion) {
        if (stacks == null || stacks.isEmpty()) {
            return Collections.emptyList();
        }
        HashMap<String, Integer> toSkip = new HashMap<String, Integer>();
        this.addSkipEquip(toSkip, companion.getEquipped(EquipmentSlot.HELMET));
        this.addSkipEquip(toSkip, companion.getEquipped(EquipmentSlot.CHESTPLATE));
        this.addSkipEquip(toSkip, companion.getEquipped(EquipmentSlot.LEGGINGS));
        this.addSkipEquip(toSkip, companion.getEquipped(EquipmentSlot.BOOTS));
        this.addSkipEquip(toSkip, companion.getEquipped(EquipmentSlot.WEAPON));
        this.addSkipEquip(toSkip, companion.getEquipped(EquipmentSlot.OFFHAND));
        ArrayList<String[]> filtered = new ArrayList<String[]>(stacks.size());
        for (String[] stack : stacks) {
            if (stack == null || stack.length == 0 || stack[0] == null) continue;
            String key = stack[0].toLowerCase(Locale.ROOT);
            int skip = toSkip.getOrDefault(key, 0);
            if (skip > 0) {
                toSkip.put(key, skip - 1);
                continue;
            }
            filtered.add(stack);
        }
        return filtered;
    }

    private void addSkipEquip(Map<String, Integer> toSkip, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        String key = itemId.toLowerCase(Locale.ROOT);
        toSkip.merge(key, 1, Integer::sum);
    }

    private void subtractEquippedFromInventory(Map<String, Integer> inv, String equippedItemId) {
        if (inv == null || equippedItemId == null || equippedItemId.isBlank()) {
            return;
        }
        String matchKey = null;
        for (String key : inv.keySet()) {
            if (key == null || !key.equalsIgnoreCase(equippedItemId)) continue;
            matchKey = key;
            break;
        }
        if (matchKey == null) {
            return;
        }
        Integer qty = inv.get(matchKey);
        if (qty == null || qty <= 0) {
            return;
        }
        if (qty <= 1) {
            inv.remove(matchKey);
        } else {
            inv.put(matchKey, qty - 1);
        }
    }

    private void rebuildEquipSlot(UIContext ctx, CompanionRecord companion, EquipmentSlot slot, String id, String label, PlayerCompanionData pData) {
        try {
            boolean tipsOn = pData != null && pData.uiTooltipsEnabled;
            ctx.getById(id, ItemGridBuilder.class).ifPresent(grid -> {
                String equipped;
                List<ItemGridSlot> slots = grid.getSlots();
                for (int i = slots.size() - 1; i >= 0; --i) {
                    grid.removeSlot(i);
                }
                String string = equipped = companion != null ? companion.getEquipped(slot) : null;
                if (equipped != null) {
                    ItemGridSlot gs = new ItemGridSlot(new ItemStack(equipped, 1));
                    if (tipsOn) {
                        gs.setName(EquipmentSlot.getItemDisplayName(equipped));
                        gs.setDescription("Click to unequip");
                    }
                    gs.setActivatable(true);
                    grid.addSlot(gs);
                } else {
                    ItemGridSlot gs = new ItemGridSlot();
                    if (tipsOn) {
                        gs.setName(label + " (empty)");
                        gs.setDescription(label + " slot");
                    }
                    gs.setActivatable(true);
                    grid.addSlot(gs);
                }
            });
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Failed to rebuild equip slot " + id);
        }
    }

    private void rebuildPlayerGrid(UIContext ctx, List<String[]> inventoryItems, PlayerCompanionData pData) {
        try {
            boolean tipsOn = pData != null && pData.uiTooltipsEnabled;
            ctx.getById("player-inventory-grid", ItemGridBuilder.class).ifPresent(grid -> {
                List<ItemGridSlot> slots = grid.getSlots();
                for (int i = slots.size() - 1; i >= 0; --i) {
                    grid.removeSlot(i);
                }
                int maxSlots = 45;
                for (int i = 0; i < maxSlots; ++i) {
                    if (i < inventoryItems.size()) {
                        String[] item = (String[])inventoryItems.get(i);
                        int qty = this.parseQty(item.length >= 3 ? item[2] : "1");
                        ItemGridSlot gs = new ItemGridSlot(new ItemStack(item[0], qty));
                        if (tipsOn) {
                            gs.setName(item[1] + " x" + qty);
                        }
                        gs.setActivatable(true);
                        grid.addSlot(gs);
                        continue;
                    }
                    grid.addSlot(new ItemGridSlot());
                }
            });
        }
        catch (Throwable t) {
            ((HytaleLogger.Api)this.logger.at(Level.FINE).withCause(t)).log("Failed to rebuild player inv grid");
        }
    }

    private void navigateCompanion(PlayerRef playerRef, int direction) {
        PlayerCompanionData pData = this.companionManager.getOrCreate(playerRef.getUuid());
        if (pData.companions.isEmpty()) {
            return;
        }
        CompanionRecord current = this.resolveCompanion(pData);
        int currentIdx = 0;
        if (current != null) {
            for (int i = 0; i < pData.companions.size(); ++i) {
                if (current.uniqueId == null || !current.uniqueId.equals(pData.companions.get((int)i).uniqueId)) continue;
                currentIdx = i;
                break;
            }
        }
        int newIdx = (currentIdx + direction + pData.companions.size()) % pData.companions.size();
        CompanionRecord selected = pData.companions.get(newIdx);
        pData.selectedCompanionId = selected.uniqueId;
        this.saveSilently();
    }

    private void saveSilently() {
        try {
            this.companionManager.save();
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private BlockPos resolveFarmCornerTarget(PlayerRef playerRef) {
        if (playerRef == null || playerRef.getWorldUuid() == null) {
            return null;
        }
        try {
            World world = Universe.get().getWorld(playerRef.getWorldUuid());
            BlockPos lookedAt = WorldQueries.getRobustLookBlockPos(world, playerRef, 6.0);
            if (lookedAt != null) {
                return lookedAt;
            }
        }
        catch (Throwable world) {
            // empty catch block
        }
        try {
            BlockPos fallback = WorldQueries.getLookBlockPos(playerRef, 8.0);
            if (fallback != null) {
                return fallback;
            }
        }
        catch (Throwable fallback) {
            // empty catch block
        }
        try {
            if (playerRef.getTransform() != null && playerRef.getTransform().getPosition() != null) {
                int px = (int)Math.floor(playerRef.getTransform().getPosition().x);
                int py = (int)Math.floor(playerRef.getTransform().getPosition().y - 1.0);
                int pz = (int)Math.floor(playerRef.getTransform().getPosition().z);
                return new BlockPos(playerRef.getWorldUuid().toString(), px, py, pz);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    private String formatCompanionLocation(PlayerRef playerRef, CompanionRecord companion) {
        if (companion == null) {
            return "Location: --";
        }
        BlockPos pos = this.companionSystem.getCompanionCurrentOrLastKnownLocation(playerRef, companion);
        if (pos == null) {
            return companion.fallen ? "Last known location: --" : "Location: --";
        }
        String prefix = companion.fallen ? "Last known location: " : "Location: ";
        return prefix + pos.x + ", " + pos.y + ", " + pos.z;
    }

    private String formatHeaderDeathCause(CompanionRecord companion) {
        if (companion == null || !companion.fallen) {
            return "";
        }
        String cause = companion.deathCause;
        if (cause == null || cause.isBlank()) {
            cause = "Unknown";
        }
        return "Killed by: " + cause;
    }

    private void normalizeFarmAreaCorners(CompanionRecord companion) {
        if (companion == null || companion.farmAreaTopLeft == null || companion.farmAreaBottomRight == null) {
            return;
        }
        BlockPos a = companion.farmAreaTopLeft;
        BlockPos b = companion.farmAreaBottomRight;
        String worldId = a.worldId != null && !a.worldId.isBlank() ? a.worldId : b.worldId;
        int minX = Math.min(a.x, b.x);
        int minY = Math.min(a.y, b.y);
        int minZ = Math.min(a.z, b.z);
        int maxX = Math.max(a.x, b.x);
        int maxY = Math.max(a.y, b.y);
        int maxZ = Math.max(a.z, b.z);
        companion.farmAreaTopLeft = new BlockPos(worldId, minX, minY, minZ);
        companion.farmAreaBottomRight = new BlockPos(worldId, maxX, maxY, maxZ);
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;").replace("\"", "&quot;");
    }

    public void shutdown() {
        for (UUID playerId : new ArrayList<UUID>(this.openPages.keySet())) {
            this.closePanel(playerId);
        }
    }

    private String gridInfoDisplayMode(PlayerCompanionData pData) {
        return pData != null && pData.uiTooltipsEnabled ? "Tooltip" : "None";
    }
}

