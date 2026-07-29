/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
 *  com.hypixel.hytale.server.core.ui.DropdownEntryInfo
 *  com.hypixel.hytale.server.core.ui.builder.EventData
 *  com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
 *  com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
 */
package com.kyuubisoft.core.ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.kyuubisoft.core.data.GameDataProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BrowsePanelHelper {
    public static final int ITEMS_PER_PAGE = 40;
    public static final String[] FILTER_KEYS = new String[]{"all", "blocks", "groups", "items", "npcs", "zones"};
    public static final String CODEC_SEARCH = "@BrowseSearch";
    public static final String CODEC_FILTER = "@BrowseFilterValue";
    public static final String CODEC_SELECT = "BrowseSelect";
    private String filter = "all";
    private String searchQuery = "";
    private int page = 0;
    private final List<String[]> items = new ArrayList<String[]>();
    private List<String[]> filteredItems = new ArrayList<String[]>();
    private String activeTarget = null;
    private String lastContext = "";
    private final BrowseItemLoader itemLoader;

    public BrowsePanelHelper(BrowseItemLoader itemLoader) {
        this.itemLoader = itemLoader;
    }

    public String getActiveTarget() {
        return this.activeTarget;
    }

    public String getFilter() {
        return this.filter;
    }

    public String getSearchQuery() {
        return this.searchQuery;
    }

    public int getPage() {
        return this.page;
    }

    public List<String[]> getFilteredItems() {
        return this.filteredItems;
    }

    public void openForTarget(String target) {
        this.activeTarget = target;
        this.searchQuery = "";
        this.page = 0;
        this.reloadItems();
    }

    public void close() {
        this.activeTarget = null;
        this.filter = "all";
        this.searchQuery = "";
        this.page = 0;
        this.lastContext = "";
        this.reloadItems();
    }

    public void reloadItems() {
        this.items.clear();
        this.itemLoader.loadItems(this.items, this.activeTarget, this.filter);
        this.applySearch();
    }

    public void setFilter(String key) {
        this.filter = key;
        this.page = 0;
        this.searchQuery = "";
        this.reloadItems();
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.trim().toLowerCase() : "";
        this.page = 0;
        this.applySearch();
    }

    public void nextPage() {
        int maxPage = Math.max(0, (this.filteredItems.size() - 1) / 40);
        this.page = Math.min(maxPage, this.page + 1);
    }

    public void prevPage() {
        this.page = Math.max(0, this.page - 1);
    }

    public String handleSelection(String browseAction) {
        int entryIdx;
        if (browseAction == null || !browseAction.startsWith("browse_")) {
            return null;
        }
        try {
            entryIdx = Integer.parseInt(browseAction.substring(7));
        }
        catch (NumberFormatException e) {
            return null;
        }
        int actualIdx = this.page * 40 + entryIdx;
        if (actualIdx < 0 || actualIdx >= this.filteredItems.size()) {
            return null;
        }
        return this.filteredItems.get(actualIdx)[0];
    }

    public void buildGrid(UICommandBuilder ui) {
        int start = this.page * 40;
        for (int i = 0; i < 40; ++i) {
            int idx = start + i;
            String prefix = "#BrowseItem" + i;
            if (idx < this.filteredItems.size()) {
                String[] item = this.filteredItems.get(idx);
                ui.set(prefix + ".Visible", true);
                String iconId = item.length > 3 && item[3] != null && !item[3].isEmpty() ? item[3] : item[0];
                ui.set(prefix + " #BrowseIcon" + i + ".ItemId", iconId);
                ui.set(prefix + " #BrowseLabel" + i + ".Text", item[1]);
                continue;
            }
            ui.set(prefix + ".Visible", false);
        }
        int totalPages = Math.max(1, (int)Math.ceil((double)this.filteredItems.size() / 40.0));
        if (this.page >= totalPages) {
            this.page = totalPages - 1;
        }
        if (this.page < 0) {
            this.page = 0;
        }
        ui.set("#BrowsePageInfo.Text", this.page + 1 + " / " + totalPages + " (" + this.filteredItems.size() + ")");
        ui.set("#BrowsePrevBtn.Visible", this.page > 0);
        ui.set("#BrowseNextBtn.Visible", this.page < totalPages - 1);
    }

    public void buildFilterDropdown(UICommandBuilder ui, String contextKey, List<DropdownEntryInfo> entries) {
        if (!contextKey.equals(this.lastContext)) {
            this.lastContext = contextKey;
            ui.set("#BrowseFilterDropdown.Entries", entries);
        }
        ui.set("#BrowseFilterDropdown.Value", this.filter);
    }

    public void bindEvents(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#BrowseSearchField", EventData.of((String)CODEC_SEARCH, (String)"#BrowseSearchField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#BrowseFilterDropdown", EventData.of((String)CODEC_FILTER, (String)"#BrowseFilterDropdown.Value"));
        for (int i = 0; i < 40; ++i) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#BrowseItem" + i, EventData.of((String)CODEC_SELECT, (String)("browse_" + i)), false);
        }
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BrowsePrevBtn", EventData.of((String)"Button", (String)"browsePrev"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BrowseNextBtn", EventData.of((String)"Button", (String)"browseNext"), false);
    }

    public static void loadByFilter(List<String[]> items, String filter) {
        switch (filter) {
            case "blocks": {
                BrowsePanelHelper.addBlockEntries(items);
                break;
            }
            case "groups": {
                BrowsePanelHelper.addGroupEntries(items);
                break;
            }
            case "items": {
                BrowsePanelHelper.addItemEntries(items);
                break;
            }
            case "npcs": {
                BrowsePanelHelper.addNpcEntries(items);
                break;
            }
            case "zones": {
                BrowsePanelHelper.addZoneEntries(items);
                break;
            }
            default: {
                BrowsePanelHelper.addBlockEntries(items);
                BrowsePanelHelper.addGroupEntries(items);
                BrowsePanelHelper.addItemEntries(items);
                BrowsePanelHelper.addNpcEntries(items);
                BrowsePanelHelper.addZoneEntries(items);
            }
        }
    }

    public static void addBlockEntries(List<String[]> items) {
        Map<String, String> blockToItem = GameDataProvider.getBlockToItemMap();
        for (String blockId : GameDataProvider.getAllBlockIds()) {
            String iconId = blockToItem.get(blockId);
            items.add(new String[]{blockId, blockId, "blocks", iconId != null ? iconId : ""});
        }
    }

    public static void addGroupEntries(List<String[]> items) {
        for (String group : GameDataProvider.getAllBlockGroups()) {
            items.add(new String[]{group, group, "groups", ""});
        }
    }

    public static void addItemEntries(List<String[]> items) {
        for (String itemId : GameDataProvider.getAllItemIds()) {
            items.add(new String[]{itemId, BrowsePanelHelper.formatShortId(itemId), "items", itemId});
        }
    }

    public static void addNpcEntries(List<String[]> items) {
        for (String npc : GameDataProvider.getAllNPCRoleNames()) {
            items.add(new String[]{npc, BrowsePanelHelper.formatShortId(npc), "npcs", ""});
        }
        for (String grp : GameDataProvider.getAllNPCGroups()) {
            items.add(new String[]{grp, grp, "npcs", ""});
        }
    }

    public static void addZoneEntries(List<String[]> items) {
        for (Map.Entry<String, String> entry : GameDataProvider.getZoneTypes().entrySet()) {
            items.add(new String[]{entry.getKey(), entry.getValue(), "zones", ""});
        }
    }

    public static String formatShortId(String id) {
        String[] prefixes;
        if (id == null) {
            return "?";
        }
        for (String prefix : prefixes = new String[]{"Weapon_", "Tool_", "Armor_", "Food_", "Potion_", "Block_", "Ingredient_", "Resource_", "Consumable_", "Material_", "Item_", "Ore_", "Deco_"}) {
            if (!id.startsWith(prefix)) continue;
            return id.substring(prefix.length()).replace("_", " ");
        }
        return id.replace("_", " ");
    }

    private void applySearch() {
        this.filteredItems = new ArrayList<String[]>();
        String query = this.searchQuery != null ? this.searchQuery.toLowerCase().trim() : "";
        for (String[] item : this.items) {
            if (!query.isEmpty() && !item[0].toLowerCase().contains(query) && !item[1].toLowerCase().contains(query)) continue;
            this.filteredItems.add(item);
        }
    }

    @FunctionalInterface
    public static interface BrowseItemLoader {
        public void loadItems(List<String[]> var1, String var2, String var3);
    }
}

