/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.codec.Codec
 *  com.hypixel.hytale.codec.KeyedCodec
 *  com.hypixel.hytale.codec.builder.BuilderCodec
 *  com.hypixel.hytale.codec.builder.BuilderCodec$Builder
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
 *  com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
 *  com.hypixel.hytale.server.core.ui.DropdownEntryInfo
 *  com.hypixel.hytale.server.core.ui.LocalizableString
 *  com.hypixel.hytale.server.core.ui.builder.EventData
 *  com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
 *  com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.kyuubisoft.core.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.CorePlugin;
import com.kyuubisoft.core.api.CoreAPI;
import com.kyuubisoft.core.i18n.CoreI18n;
import com.kyuubisoft.core.i18n.I18nContext;
import com.kyuubisoft.core.lootbag.LootbagAdminService;
import com.kyuubisoft.core.lootbag.LootbagDefinition;
import com.kyuubisoft.core.registry.ModMenuRegistry;
import com.kyuubisoft.core.ui.BrowsePanelHelper;
import java.lang.invoke.LambdaMetafactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class LootbagAdminPage
extends InteractiveCustomUIPage<PageEventData> {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Lootbag Admin");
    private static final int LOOTBAGS_PER_PAGE = 10;
    private static final int MAX_DYNAMIC_SLOTS = 10;
    private static final int MAX_GUARANTEED_SLOTS = 5;
    private static final String[] RARITIES = new String[]{"common", "uncommon", "rare", "epic", "legendary"};
    private static final String[] LOOT_TYPES = new String[]{"fixed", "random", "hybrid"};
    private final CorePlugin plugin;
    private final Player player;
    private final PlayerRef playerRef;
    private final LootbagAdminService service;
    private Ref<EntityStore> storedRef;
    private Store<EntityStore> storedStore;
    private int lootPage = 0;
    private String rarityFilter = "all";
    private String searchQuery = "";
    private String selectedLootbagId = null;
    private List<Map.Entry<String, LootbagDefinition>> filteredLootbags = new ArrayList<Map.Entry<String, LootbagDefinition>>();
    private boolean showingEditor = false;
    private boolean creatingNew = false;
    private boolean showingDeleteConfirm = false;
    private String edOriginalId = null;
    private boolean editorDropdownsInitialized = false;
    private boolean listDropdownsInitialized = false;
    private String edId = "";
    private String edName = "";
    private String edDescription = "";
    private String edIcon = "";
    private String edRarity = "common";
    private String edType = "fixed";
    private String edPickCount = "1";
    private boolean edAllowDuplicates = false;
    private List<String[]> edItems = new ArrayList<String[]>();
    private List<String[]> edPool = new ArrayList<String[]>();
    private List<String[]> edGuaranteed = new ArrayList<String[]>();
    private final BrowsePanelHelper browseHelper = new BrowsePanelHelper(this::loadBrowseItems);
    private boolean showingBrowse = false;
    private String browseTarget = null;
    private static final String[] LOC_LANGUAGES = new String[]{"en-US", "de-DE", "fr-FR", "es-ES", "pt-BR", "ru-RU", "pl-PL", "tr-TR", "it-IT"};
    private static final String[] LOC_LABELS = new String[]{"NAME", "DESCRIPTION"};
    private int locEditorFieldIndex = -1;
    private final Map<String, String[]> locValues = new LinkedHashMap<String, String[]>();

    public LootbagAdminPage(CorePlugin plugin, Player player, PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageEventData.CODEC);
        this.plugin = plugin;
        this.player = player;
        this.playerRef = playerRef;
        this.service = plugin.getLootbagAdminService();
        this.filterLootbags();
    }

    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder ui, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        I18nContext.run(this.playerRef, () -> {
            this.storedRef = ref;
            this.storedStore = store;
            ui.append("Pages/LootbagAdmin/AdminPanel.ui");
            this.setI18nLabels(ui);
            this.bindEvents(ui, events);
            this.buildListPanel(ui);
            this.buildDetailPanel(ui);
            this.buildEditorOverlay(ui);
            this.buildDeleteConfirm(ui);
            this.buildBrowsePanel(ui);
        });
    }

    private void setI18nLabels(UICommandBuilder ui) {
        CoreI18n i18n = CoreI18n.getInstance();
        ui.set("#LootHeaderLabel.Text", i18n.get("lootbag.admin.title"));
        ui.set("#EmptyLootLabel.Text", i18n.get("lootbag.admin.no_selection"));
        ui.set("#LootDetailHeaderLabel.Text", i18n.get("lootbag.admin.title"));
        ui.set("#LootItemsSectionLabel.Text", i18n.get("lootbag.admin.items") + ":");
        ui.set("#LootGuarSectionLabel.Text", i18n.get("lootbag.admin.guaranteed") + ":");
        ui.set("#LootPoolSectionLabel.Text", i18n.get("lootbag.admin.pool") + ":");
        ui.set("#LootDeleteTitle.Text", i18n.get("lootbag.admin.delete_confirm", "..."));
        ui.set("#LootDeleteConfirmText.Text", i18n.get("lootbag.admin.delete_hint"));
    }

    private void bindEvents(UICommandBuilder ui, UIEventBuilder events) {
        int i;
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of((String)"Button", (String)"back"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of((String)"Button", (String)"close"), false);
        for (i = 0; i < 10; ++i) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#LootEntry" + i, EventData.of((String)"Button", (String)("select_" + i)), false);
        }
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LootPrevButton", EventData.of((String)"Button", (String)"lootPrev"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LootNextButton", EventData.of((String)"Button", (String)"lootNext"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LootAddButton", EventData.of((String)"Button", (String)"lootAdd"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LootEditButton", EventData.of((String)"Button", (String)"lootEdit"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LootCopyButton", EventData.of((String)"Button", (String)"lootCopy"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LootDeleteButton", EventData.of((String)"Button", (String)"lootDelete"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LootDeleteYes", EventData.of((String)"Button", (String)"lootDeleteYes"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LootDeleteNo", EventData.of((String)"Button", (String)"lootDeleteNo"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LootEdSaveBtn", EventData.of((String)"Button", (String)"lootEdSave"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LootEdCancelBtn", EventData.of((String)"Button", (String)"lootEdCancel"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LootBrowseIconBtn", EventData.of((String)"Button", (String)"browseIcon"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BrowseCloseBtn", EventData.of((String)"Button", (String)"browseClose"), false);
        for (i = 0; i < 10; ++i) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#LootBrowseItem" + i + "Btn", EventData.of((String)"Button", (String)("browseItem" + i)), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#LootBrowsePool" + i + "Btn", EventData.of((String)"Button", (String)("browsePool" + i)), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#LootEdItemRemove" + i, EventData.of((String)"Button", (String)("itemRemove" + i)), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#LootEdPoolRemove" + i, EventData.of((String)"Button", (String)("poolRemove" + i)), false);
        }
        for (i = 0; i < 5; ++i) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#LootBrowseGuar" + i + "Btn", EventData.of((String)"Button", (String)("browseGuar" + i)), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#LootEdGuarRemove" + i, EventData.of((String)"Button", (String)("guarRemove" + i)), false);
        }
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LootEdItemAdd", EventData.of((String)"Button", (String)"itemAdd"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LootEdPoolAdd", EventData.of((String)"Button", (String)"poolAdd"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LootEdGuarAdd", EventData.of((String)"Button", (String)"guarAdd"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#RarityFilterDropdown", EventData.of((String)"@RarityFilter", (String)"#RarityFilterDropdown.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LootSearchField", EventData.of((String)"@LootSearch", (String)"#LootSearchField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LootEdRarityDropdown", EventData.of((String)"@EdRarity", (String)"#LootEdRarityDropdown.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LootEdTypeDropdown", EventData.of((String)"@EdType", (String)"#LootEdTypeDropdown.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LootEdIdField", EventData.of((String)"@EdId", (String)"#LootEdIdField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LootEdNameField", EventData.of((String)"@EdName", (String)"#LootEdNameField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LootEdDescField", EventData.of((String)"@EdDesc", (String)"#LootEdDescField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LootEdIconField", EventData.of((String)"@EdIcon", (String)"#LootEdIconField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LootEdPickCountField", EventData.of((String)"@EdPickCount", (String)"#LootEdPickCountField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LootEdDuplicatesCheck", EventData.of((String)"@EdDuplicates", (String)"#LootEdDuplicatesCheck.Value"), false);
        for (i = 0; i < 10; ++i) {
            events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LootEdItemId" + i, EventData.of((String)("@EdItemId" + i), (String)("#LootEdItemId" + i + ".Value")), false);
            events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LootEdItemAmt" + i, EventData.of((String)("@EdItemAmt" + i), (String)("#LootEdItemAmt" + i + ".Value")), false);
            events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LootEdPoolId" + i, EventData.of((String)("@EdPoolId" + i), (String)("#LootEdPoolId" + i + ".Value")), false);
            events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LootEdPoolAmt" + i, EventData.of((String)("@EdPoolAmt" + i), (String)("#LootEdPoolAmt" + i + ".Value")), false);
            events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LootEdPoolWeight" + i, EventData.of((String)("@EdPoolWt" + i), (String)("#LootEdPoolWeight" + i + ".Value")), false);
        }
        for (i = 0; i < 5; ++i) {
            events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LootEdGuarId" + i, EventData.of((String)("@EdGuarId" + i), (String)("#LootEdGuarId" + i + ".Value")), false);
            events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LootEdGuarAmt" + i, EventData.of((String)("@EdGuarAmt" + i), (String)("#LootEdGuarAmt" + i + ".Value")), false);
        }
        this.browseHelper.bindEvents(events);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LootLocEditNameBtn", EventData.of((String)"Button", (String)"locEditName"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LootLocEditDescBtn", EventData.of((String)"Button", (String)"locEditDesc"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LocEdDoneBtn", EventData.of((String)"Button", (String)"locEdDone"), false);
        for (i = 0; i < LOC_LANGUAGES.length; ++i) {
            events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LocEdField" + i, EventData.of((String)("@LocEdField" + i), (String)("#LocEdField" + i + ".Value")), false);
        }
    }

    private void refreshUI() {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        this.setI18nLabels(ui);
        this.bindEvents(ui, events);
        this.buildListPanel(ui);
        this.buildDetailPanel(ui);
        this.buildEditorOverlay(ui);
        this.buildDeleteConfirm(ui);
        this.buildBrowsePanel(ui);
        this.buildLocEditorOverlay(ui);
        this.sendUpdate(ui, events, false);
    }

    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageEventData data) {
        I18nContext.run(this.playerRef, () -> {
            int i;
            super.handleDataEvent(ref, store, (Object)data);
            this.storedRef = ref;
            this.storedStore = store;
            if (data.button != null) {
                this.handleButton(data.button);
                return;
            }
            if (data.rarityFilter != null) {
                if (!data.rarityFilter.equals(this.rarityFilter)) {
                    this.rarityFilter = data.rarityFilter;
                    this.lootPage = 0;
                    this.filterLootbags();
                }
                this.refreshUI();
                return;
            }
            if (data.lootSearch != null) {
                this.searchQuery = data.lootSearch;
                this.lootPage = 0;
                this.filterLootbags();
                this.refreshUI();
                return;
            }
            if (data.edRarity != null) {
                this.edRarity = data.edRarity;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.edType != null) {
                String oldType = this.edType;
                this.edType = data.edType;
                if (!this.edType.equals(oldType)) {
                    this.refreshUI();
                } else {
                    this.sendUpdate(new UICommandBuilder(), false);
                }
                return;
            }
            if (data.edId != null) {
                this.edId = data.edId;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.edName != null) {
                this.edName = data.edName;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.edDesc != null) {
                this.edDescription = data.edDesc;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.edIcon != null) {
                this.edIcon = data.edIcon;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.edPickCount != null) {
                this.edPickCount = data.edPickCount;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.edDuplicates != null) {
                this.edAllowDuplicates = data.edDuplicates;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            for (i = 0; i < 10; ++i) {
                if (data.edItemIds[i] != null && i < this.edItems.size()) {
                    this.edItems.get((int)i)[0] = data.edItemIds[i];
                    this.sendUpdate(new UICommandBuilder(), false);
                    return;
                }
                if (data.edItemAmts[i] != null && i < this.edItems.size()) {
                    this.edItems.get((int)i)[1] = data.edItemAmts[i];
                    this.sendUpdate(new UICommandBuilder(), false);
                    return;
                }
                if (data.edPoolIds[i] != null && i < this.edPool.size()) {
                    this.edPool.get((int)i)[0] = data.edPoolIds[i];
                    this.sendUpdate(new UICommandBuilder(), false);
                    return;
                }
                if (data.edPoolAmts[i] != null && i < this.edPool.size()) {
                    this.edPool.get((int)i)[1] = data.edPoolAmts[i];
                    this.sendUpdate(new UICommandBuilder(), false);
                    return;
                }
                if (data.edPoolWts[i] == null || i >= this.edPool.size()) continue;
                this.edPool.get((int)i)[2] = data.edPoolWts[i];
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            for (i = 0; i < 5; ++i) {
                if (data.edGuarIds[i] != null && i < this.edGuaranteed.size()) {
                    this.edGuaranteed.get((int)i)[0] = data.edGuarIds[i];
                    this.sendUpdate(new UICommandBuilder(), false);
                    return;
                }
                if (data.edGuarAmts[i] == null || i >= this.edGuaranteed.size()) continue;
                this.edGuaranteed.get((int)i)[1] = data.edGuarAmts[i];
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.browseSearch != null) {
                this.browseHelper.setSearchQuery(data.browseSearch);
                UICommandBuilder browseUi = new UICommandBuilder();
                this.browseHelper.buildGrid(browseUi);
                this.sendUpdate(browseUi, false);
                return;
            }
            if (data.browseFilterValue != null) {
                if (!data.browseFilterValue.equals(this.browseHelper.getFilter())) {
                    this.browseHelper.setFilter(data.browseFilterValue);
                    UICommandBuilder browseUi = new UICommandBuilder();
                    this.browseHelper.buildGrid(browseUi);
                    this.sendUpdate(browseUi, false);
                } else {
                    this.sendUpdate(new UICommandBuilder(), false);
                }
                return;
            }
            if (data.browseSelect != null) {
                String selected = this.browseHelper.handleSelection(data.browseSelect);
                if (selected != null) {
                    this.handleBrowseSelection(selected);
                } else {
                    this.refreshUI();
                }
                return;
            }
            for (i = 0; i < LOC_LANGUAGES.length; ++i) {
                if (data.locEdFields[i] == null || this.locEditorFieldIndex < 0) continue;
                this.locValues.computeIfAbsent((String)LootbagAdminPage.LOC_LANGUAGES[i], (Function<String, String[]>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Ljava/lang/Object;, lambda$handleDataEvent$1(java.lang.String ), (Ljava/lang/String;)[Ljava/lang/String;)())[this.locEditorFieldIndex] = data.locEdFields[i];
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            this.sendUpdate(new UICommandBuilder(), false);
        });
    }

    private void handleButton(String button) {
        switch (button) {
            case "lootAdd": 
            case "lootEdit": 
            case "lootCopy": 
            case "lootDelete": 
            case "lootDeleteYes": 
            case "lootEdSave": 
            case "itemAdd": 
            case "poolAdd": 
            case "guarAdd": 
            case "locEdDone": {
                if (!CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) break;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            default: {
                if (!button.startsWith("itemRemove") && !button.startsWith("poolRemove") && !button.startsWith("guarRemove") || !CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) break;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
        }
        switch (button) {
            case "back": {
                ModMenuRegistry.openCoreAdmin(this.player, this.playerRef, this.storedRef, this.storedStore);
                return;
            }
            case "close": {
                this.close();
                return;
            }
            case "lootPrev": {
                if (this.lootPage <= 0) break;
                --this.lootPage;
                break;
            }
            case "lootNext": {
                int totalPages = Math.max(1, (int)Math.ceil((double)this.filteredLootbags.size() / 10.0));
                if (this.lootPage >= totalPages - 1) break;
                ++this.lootPage;
                break;
            }
            case "lootAdd": {
                this.openEditor(null, true);
                break;
            }
            case "lootEdit": {
                LootbagDefinition def;
                if (this.selectedLootbagId == null || (def = this.service.get(this.selectedLootbagId)) == null || !this.service.isCustom(this.selectedLootbagId)) break;
                this.openEditor(def, false);
                break;
            }
            case "lootCopy": {
                LootbagDefinition def;
                if (this.selectedLootbagId == null || (def = this.service.get(this.selectedLootbagId)) == null) break;
                this.openEditor(def, true);
                this.edId = this.selectedLootbagId + "_custom";
                break;
            }
            case "lootDelete": {
                if (this.selectedLootbagId == null || !this.service.isCustom(this.selectedLootbagId)) break;
                this.showingDeleteConfirm = true;
                break;
            }
            case "lootDeleteYes": {
                if (this.selectedLootbagId == null || !this.service.isCustom(this.selectedLootbagId)) break;
                this.service.removeCustom(this.selectedLootbagId);
                this.service.saveCustomLootbags();
                this.selectedLootbagId = null;
                this.showingDeleteConfirm = false;
                this.filterLootbags();
                break;
            }
            case "lootDeleteNo": {
                this.showingDeleteConfirm = false;
                break;
            }
            case "lootEdSave": {
                this.saveEditor();
                break;
            }
            case "lootEdCancel": {
                this.showingEditor = false;
                this.showingBrowse = false;
                break;
            }
            case "browseIcon": {
                this.openBrowse("icon");
                break;
            }
            case "browseClose": {
                this.closeBrowse();
                break;
            }
            case "itemAdd": {
                if (this.edItems.size() >= 10) break;
                this.edItems.add(new String[]{"", "1"});
                break;
            }
            case "poolAdd": {
                if (this.edPool.size() >= 10) break;
                this.edPool.add(new String[]{"", "1", "10"});
                break;
            }
            case "guarAdd": {
                if (this.edGuaranteed.size() >= 5) break;
                this.edGuaranteed.add(new String[]{"", "1"});
                break;
            }
            case "locEditName": {
                if (!this.showingEditor) break;
                this.loadLocValues();
                this.locEditorFieldIndex = 0;
                break;
            }
            case "locEditDesc": {
                if (!this.showingEditor) break;
                this.loadLocValues();
                this.locEditorFieldIndex = 1;
                break;
            }
            case "locEdDone": {
                this.locEditorFieldIndex = -1;
                break;
            }
            default: {
                if (button.startsWith("select_")) {
                    int actualIdx;
                    int idx = LootbagAdminPage.parseIntSafe(button.substring(7), -1);
                    if (idx < 0 || (actualIdx = this.lootPage * 10 + idx) < 0 || actualIdx >= this.filteredLootbags.size()) break;
                    this.selectedLootbagId = this.filteredLootbags.get(actualIdx).getKey();
                    break;
                }
                if (button.startsWith("browseItem")) {
                    int idx = LootbagAdminPage.parseIntSafe(button.substring(10), -1);
                    if (idx < 0) break;
                    this.openBrowse("item_" + idx);
                    break;
                }
                if (button.startsWith("browsePool")) {
                    int idx = LootbagAdminPage.parseIntSafe(button.substring(10), -1);
                    if (idx < 0) break;
                    this.openBrowse("pool_" + idx);
                    break;
                }
                if (button.startsWith("browseGuar")) {
                    int idx = LootbagAdminPage.parseIntSafe(button.substring(10), -1);
                    if (idx < 0) break;
                    this.openBrowse("guar_" + idx);
                    break;
                }
                if (button.startsWith("itemRemove")) {
                    int idx = LootbagAdminPage.parseIntSafe(button.substring(10), -1);
                    if (idx < 0 || idx >= this.edItems.size()) break;
                    this.edItems.remove(idx);
                    break;
                }
                if (button.startsWith("poolRemove")) {
                    int idx = LootbagAdminPage.parseIntSafe(button.substring(10), -1);
                    if (idx < 0 || idx >= this.edPool.size()) break;
                    this.edPool.remove(idx);
                    break;
                }
                if (button.startsWith("guarRemove")) {
                    int idx = LootbagAdminPage.parseIntSafe(button.substring(10), -1);
                    if (idx < 0 || idx >= this.edGuaranteed.size()) break;
                    this.edGuaranteed.remove(idx);
                    break;
                }
                if (button.equals("browsePrev")) {
                    this.browseHelper.prevPage();
                    break;
                }
                if (button.equals("browseNext")) {
                    this.browseHelper.nextPage();
                    break;
                }
                LOGGER.warning("Unknown button: " + button);
            }
        }
        this.refreshUI();
    }

    private void buildListPanel(UICommandBuilder ui) {
        this.filterLootbags();
        if (!this.listDropdownsInitialized) {
            this.listDropdownsInitialized = true;
            ArrayList<DropdownEntryInfo> filterEntries = new ArrayList<DropdownEntryInfo>();
            CoreI18n i18nFilter = CoreI18n.getInstance();
            filterEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)i18nFilter.get("lootbag.admin.filter.all")), "all"));
            for (String r : RARITIES) {
                filterEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)i18nFilter.get("lootbag.admin.filter." + r)), r));
            }
            filterEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)i18nFilter.get("lootbag.admin.filter.custom")), "custom"));
            ui.set("#RarityFilterDropdown.Entries", filterEntries);
        }
        ui.set("#RarityFilterDropdown.Value", this.rarityFilter);
        int start = this.lootPage * 10;
        for (int i = 0; i < 10; ++i) {
            String prefix = "#LootEntry" + i;
            int actualIdx = start + i;
            if (actualIdx < this.filteredLootbags.size()) {
                Map.Entry<String, LootbagDefinition> entry = this.filteredLootbags.get(actualIdx);
                String id = entry.getKey();
                LootbagDefinition def = entry.getValue();
                ui.set(prefix + ".Visible", true);
                ui.set(prefix + " #LootIcon" + i + ".ItemId", def.getIcon());
                ui.set(prefix + " #LootName" + i + ".Text", def.getName());
                String rarity = def.getRarity().substring(0, 1).toUpperCase() + def.getRarity().substring(1).toLowerCase();
                ui.set(prefix + " #LootRarity" + i + ".Text", rarity);
                ui.set(prefix + " #LootRarity" + i + ".Style.TextColor", def.getRarityColor());
                ui.set(prefix + " #LootType" + i + ".Text", this.formatLootbagType(def));
                boolean isCustom = this.service.isCustom(id);
                ui.set(prefix + " #LootCustom" + i + ".Text", isCustom ? "CUSTOM" : "");
                boolean isSelected = id.equals(this.selectedLootbagId);
                ui.set(prefix + ".Background", isSelected ? "#ffd70030" : "#ffffff08");
                continue;
            }
            ui.set(prefix + ".Visible", false);
        }
        int totalPages = Math.max(1, (int)Math.ceil((double)this.filteredLootbags.size() / 10.0));
        if (this.lootPage >= totalPages) {
            this.lootPage = totalPages - 1;
        }
        if (this.lootPage < 0) {
            this.lootPage = 0;
        }
        ui.set("#LootPageLabel.Text", this.lootPage + 1 + " / " + totalPages + " (" + this.filteredLootbags.size() + ")");
        ui.set("#LootPrevButton.Visible", this.lootPage > 0);
        ui.set("#LootNextButton.Visible", this.lootPage < totalPages - 1);
        boolean hasSelection = this.selectedLootbagId != null;
        boolean isCustomSelected = hasSelection && this.service.isCustom(this.selectedLootbagId);
        ui.set("#LootEditButton.Visible", isCustomSelected);
        ui.set("#LootCopyButton.Visible", hasSelection);
        ui.set("#LootDeleteButton.Visible", isCustomSelected);
        int customCount = this.service.getCustomLootbags().size();
        int totalCount = this.service.getAll().size();
        ui.set("#LootCountLabel.Text", CoreI18n.getInstance().get("lootbag.admin.count", totalCount, customCount));
    }

    private void buildDetailPanel(UICommandBuilder ui) {
        List<LootbagDefinition.PoolItem> pool;
        List<LootbagDefinition.LootItem> guaranteed;
        boolean hasSelection = this.selectedLootbagId != null;
        LootbagDefinition def = hasSelection ? this.service.get(this.selectedLootbagId) : null;
        ui.set("#LootDetailPanel.Visible", hasSelection && def != null);
        ui.set("#EmptyDetailPanel.Visible", !hasSelection || def == null);
        if (def == null) {
            return;
        }
        ui.set("#LootDetailId.Text", this.selectedLootbagId);
        ui.set("#LootDetailName.Text", def.getName());
        ui.set("#LootDetailRarity.Text", LootbagAdminPage.capitalize(def.getRarity()));
        ui.set("#LootDetailType.Text", this.formatLootbagType(def));
        ui.set("#LootDetailIconImg.ItemId", def.getIcon());
        ui.set("#LootDetailIcon.Text", def.getIcon());
        ui.set("#LootDetailDesc.Text", def.getDescription() != null ? def.getDescription() : CoreI18n.getInstance().get("lootbag.admin.no_desc"));
        List<LootbagDefinition.LootItem> items = def.getItems();
        boolean hasItems = items != null && !items.isEmpty();
        ui.set("#LootDetailItemsSection.Visible", hasItems);
        if (hasItems) {
            for (int i = 0; i < 8; ++i) {
                if (i < items.size()) {
                    LootbagDefinition.LootItem item = items.get(i);
                    ui.set("#LootDetailItem" + i + ".Visible", true);
                    ui.set("#LootDetailItemIcon" + i + ".ItemId", item.getItemId());
                    ui.set("#LootDetailItemLabel" + i + ".Text", item.getItemId() + " x" + item.getAmount());
                    continue;
                }
                ui.set("#LootDetailItem" + i + ".Visible", false);
            }
        }
        boolean hasGuaranteed = (guaranteed = def.getGuaranteedItems()) != null && !guaranteed.isEmpty();
        ui.set("#LootDetailGuarSection.Visible", hasGuaranteed);
        if (hasGuaranteed) {
            for (int i = 0; i < 4; ++i) {
                if (i < guaranteed.size()) {
                    LootbagDefinition.LootItem item = guaranteed.get(i);
                    ui.set("#LootDetailGuar" + i + ".Visible", true);
                    ui.set("#LootDetailGuarIcon" + i + ".ItemId", item.getItemId());
                    ui.set("#LootDetailGuarLabel" + i + ".Text", item.getItemId() + " x" + item.getAmount());
                    continue;
                }
                ui.set("#LootDetailGuar" + i + ".Visible", false);
            }
        }
        boolean hasPool = (pool = def.getPool()) != null && !pool.isEmpty();
        ui.set("#LootDetailPoolSection.Visible", hasPool);
        if (hasPool) {
            int totalWeight = pool.stream().mapToInt(LootbagDefinition.PoolItem::getWeight).sum();
            CoreI18n i18nPick = CoreI18n.getInstance();
            ui.set("#LootDetailPickCount.Text", def.isAllowDuplicates() ? i18nPick.get("lootbag.admin.pick_info_dupes", def.getPickCount()) : i18nPick.get("lootbag.admin.pick_info", def.getPickCount()));
            for (int i = 0; i < 8; ++i) {
                if (i < pool.size()) {
                    LootbagDefinition.PoolItem pItem = pool.get(i);
                    double pct = totalWeight > 0 ? (double)pItem.getWeight() / (double)totalWeight * 100.0 : 0.0;
                    ui.set("#LootDetailPool" + i + ".Visible", true);
                    ui.set("#LootDetailPoolIcon" + i + ".ItemId", pItem.getItemId());
                    ui.set("#LootDetailPoolLabel" + i + ".Text", pItem.getItemId() + " x" + pItem.getAmount() + " W:" + pItem.getWeight() + " (" + String.format("%.1f", pct) + "%)");
                    continue;
                }
                ui.set("#LootDetailPool" + i + ".Visible", false);
            }
        }
    }

    private void buildEditorOverlay(UICommandBuilder ui) {
        int i;
        ui.set("#LootEditorOverlay.Visible", this.showingEditor);
        if (!this.showingEditor) {
            return;
        }
        CoreI18n i18nEd = CoreI18n.getInstance();
        ui.set("#LootEditorTitle.Text", this.creatingNew ? i18nEd.get("lootbag.admin.new_title") : i18nEd.get("lootbag.admin.edit_title", this.edOriginalId));
        ui.set("#LootEdIdField.Value", this.edId != null ? this.edId : "");
        ui.set("#LootEdNameField.Value", this.edName != null ? this.edName : "");
        ui.set("#LootEdDescField.Value", this.edDescription != null ? this.edDescription : "");
        ui.set("#LootEdIconField.Value", this.edIcon != null ? this.edIcon : "");
        ui.set("#LootEdPickCountField.Value", this.edPickCount != null ? this.edPickCount : "1");
        ui.set("#LootEdDuplicatesCheck.Value", this.edAllowDuplicates);
        if (!this.editorDropdownsInitialized) {
            this.editorDropdownsInitialized = true;
            ArrayList<DropdownEntryInfo> rarityEntries = new ArrayList<DropdownEntryInfo>();
            for (String r : RARITIES) {
                rarityEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)LootbagAdminPage.capitalize(r)), r));
            }
            ui.set("#LootEdRarityDropdown.Entries", rarityEntries);
            ArrayList<DropdownEntryInfo> typeEntries = new ArrayList<DropdownEntryInfo>();
            CoreI18n i18nType = CoreI18n.getInstance();
            typeEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)i18nType.get("lootbag.admin.type.fixed")), "fixed"));
            typeEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)i18nType.get("lootbag.admin.type.random")), "random"));
            typeEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)i18nType.get("lootbag.admin.type.hybrid")), "hybrid"));
            ui.set("#LootEdTypeDropdown.Entries", typeEntries);
        }
        ui.set("#LootEdRarityDropdown.Value", this.edRarity != null ? this.edRarity : "common");
        ui.set("#LootEdTypeDropdown.Value", this.edType != null ? this.edType : "fixed");
        boolean showFixed = "fixed".equals(this.edType);
        boolean showRandom = "random".equals(this.edType);
        boolean showHybrid = "hybrid".equals(this.edType);
        ui.set("#LootEdFixedGroup.Visible", showFixed);
        ui.set("#LootEdPoolGroup.Visible", showRandom || showHybrid);
        ui.set("#LootEdGuarGroup.Visible", showHybrid);
        ui.set("#LootEdPoolSettings.Visible", showRandom || showHybrid);
        if (showFixed) {
            for (i = 0; i < 10; ++i) {
                if (i < this.edItems.size()) {
                    ui.set("#LootEdItemRow" + i + ".Visible", true);
                    ui.set("#LootEdItemId" + i + ".Value", this.edItems.get(i)[0]);
                    ui.set("#LootEdItemAmt" + i + ".Value", this.edItems.get(i)[1]);
                    continue;
                }
                ui.set("#LootEdItemRow" + i + ".Visible", false);
            }
            ui.set("#LootEdItemAdd.Visible", this.edItems.size() < 10);
        }
        if (showRandom || showHybrid) {
            for (i = 0; i < 10; ++i) {
                if (i < this.edPool.size()) {
                    ui.set("#LootEdPoolRow" + i + ".Visible", true);
                    ui.set("#LootEdPoolId" + i + ".Value", this.edPool.get(i)[0]);
                    ui.set("#LootEdPoolAmt" + i + ".Value", this.edPool.get(i)[1]);
                    ui.set("#LootEdPoolWeight" + i + ".Value", this.edPool.get(i)[2]);
                    continue;
                }
                ui.set("#LootEdPoolRow" + i + ".Visible", false);
            }
            ui.set("#LootEdPoolAdd.Visible", this.edPool.size() < 10);
        }
        if (showHybrid) {
            for (i = 0; i < 5; ++i) {
                if (i < this.edGuaranteed.size()) {
                    ui.set("#LootEdGuarRow" + i + ".Visible", true);
                    ui.set("#LootEdGuarId" + i + ".Value", this.edGuaranteed.get(i)[0]);
                    ui.set("#LootEdGuarAmt" + i + ".Value", this.edGuaranteed.get(i)[1]);
                    continue;
                }
                ui.set("#LootEdGuarRow" + i + ".Visible", false);
            }
            ui.set("#LootEdGuarAdd.Visible", this.edGuaranteed.size() < 5);
        }
    }

    private void buildDeleteConfirm(UICommandBuilder ui) {
        ui.set("#LootDeleteConfirmOverlay.Visible", this.showingDeleteConfirm);
        if (this.showingDeleteConfirm && this.selectedLootbagId != null) {
            ui.set("#LootDeleteTitle.Text", CoreI18n.getInstance().get("lootbag.admin.delete_confirm", this.selectedLootbagId));
            ui.set("#LootDeleteConfirmText.Text", CoreI18n.getInstance().get("lootbag.admin.delete_hint"));
        }
    }

    private void buildBrowsePanel(UICommandBuilder ui) {
        ui.set("#BrowsePanel.Visible", this.showingBrowse);
        if (this.showingBrowse) {
            if (this.browseHelper.getFilteredItems().isEmpty() && this.browseHelper.getActiveTarget() != null) {
                this.browseHelper.reloadItems();
            }
            this.browseHelper.buildGrid(ui);
            String selectedInfo = this.browseTarget != null ? this.browseTarget.replace("_", " ") : "";
            ui.set("#BrowseSelectedField.Value", selectedInfo);
            ArrayList<DropdownEntryInfo> filterEntries = new ArrayList<DropdownEntryInfo>();
            for (String key : BrowsePanelHelper.FILTER_KEYS) {
                filterEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)LootbagAdminPage.capitalize(key)), key));
            }
            String contextKey = this.browseTarget != null ? this.browseTarget : "default";
            this.browseHelper.buildFilterDropdown(ui, contextKey, filterEntries);
        }
    }

    private void openEditor(LootbagDefinition def, boolean isNew) {
        this.showingEditor = true;
        this.creatingNew = isNew;
        this.editorDropdownsInitialized = false;
        this.showingBrowse = false;
        this.locEditorFieldIndex = -1;
        this.locValues.clear();
        if (def != null) {
            String string = this.edOriginalId = isNew ? null : def.getId();
            this.edId = isNew ? "" : (def.getId() != null ? def.getId() : "");
            this.edName = def.getName() != null ? def.getName() : "";
            this.edDescription = def.getDescription() != null ? def.getDescription() : "";
            this.edIcon = def.getIcon() != null ? def.getIcon() : "";
            this.edRarity = def.getRarity() != null ? def.getRarity() : "common";
            this.edPickCount = String.valueOf(def.getPickCount());
            this.edAllowDuplicates = def.isAllowDuplicates();
            this.edType = def.isHybrid() ? "hybrid" : (def.isRandomPool() ? "random" : "fixed");
            this.edItems = new ArrayList<String[]>();
            if (def.getItems() != null) {
                for (LootbagDefinition.LootItem lootItem : def.getItems()) {
                    this.edItems.add(new String[]{lootItem.getItemId() != null ? lootItem.getItemId() : "", String.valueOf(lootItem.getAmount())});
                }
            }
            this.edPool = new ArrayList<String[]>();
            if (def.getPool() != null) {
                for (LootbagDefinition.PoolItem poolItem : def.getPool()) {
                    this.edPool.add(new String[]{poolItem.getItemId() != null ? poolItem.getItemId() : "", String.valueOf(poolItem.getAmount()), String.valueOf(poolItem.getWeight())});
                }
            }
            this.edGuaranteed = new ArrayList<String[]>();
            if (def.getGuaranteedItems() != null) {
                for (LootbagDefinition.LootItem lootItem : def.getGuaranteedItems()) {
                    this.edGuaranteed.add(new String[]{lootItem.getItemId() != null ? lootItem.getItemId() : "", String.valueOf(lootItem.getAmount())});
                }
            }
        } else {
            this.edOriginalId = null;
            this.edId = "";
            this.edName = "";
            this.edDescription = "";
            this.edIcon = "Chest_Wood";
            this.edRarity = "common";
            this.edType = "fixed";
            this.edPickCount = "1";
            this.edAllowDuplicates = false;
            this.edItems = new ArrayList<String[]>();
            this.edPool = new ArrayList<String[]>();
            this.edGuaranteed = new ArrayList<String[]>();
        }
    }

    private void saveEditor() {
        if (this.edId == null || this.edId.trim().isEmpty()) {
            LOGGER.warning("Cannot save lootbag: ID is empty");
            this.refreshUI();
            return;
        }
        String saveId = this.edId.trim();
        LootbagDefinition def = new LootbagDefinition();
        def.setName(this.edName != null && !this.edName.isEmpty() ? this.edName : saveId);
        def.setDescription(this.edDescription != null && !this.edDescription.isEmpty() ? this.edDescription : null);
        def.setIcon(this.edIcon != null && !this.edIcon.isEmpty() ? this.edIcon : "Chest_Wood");
        def.setRarity(this.edRarity != null ? this.edRarity : "common");
        switch (this.edType) {
            case "fixed": {
                ArrayList<LootbagDefinition.LootItem> items = new ArrayList<LootbagDefinition.LootItem>();
                for (String[] slot : this.edItems) {
                    if (slot[0] == null || slot[0].trim().isEmpty()) continue;
                    items.add(new LootbagDefinition.LootItem(slot[0].trim(), LootbagAdminPage.parseIntSafe(slot[1], 1)));
                }
                def.setItems(items.isEmpty() ? null : items);
                def.setPool(null);
                def.setGuaranteedItems(null);
                def.setPickCount(null);
                def.setAllowDuplicates(null);
                break;
            }
            case "random": {
                ArrayList<LootbagDefinition.PoolItem> pool = new ArrayList<LootbagDefinition.PoolItem>();
                for (String[] slot : this.edPool) {
                    if (slot[0] == null || slot[0].trim().isEmpty()) continue;
                    pool.add(new LootbagDefinition.PoolItem(slot[0].trim(), LootbagAdminPage.parseIntSafe(slot[1], 1), LootbagAdminPage.parseIntSafe(slot[2], 10)));
                }
                def.setPool(pool.isEmpty() ? null : pool);
                def.setPickCount(LootbagAdminPage.parseIntSafe(this.edPickCount, 1));
                def.setAllowDuplicates(this.edAllowDuplicates);
                def.setItems(null);
                def.setGuaranteedItems(null);
                break;
            }
            case "hybrid": {
                ArrayList<LootbagDefinition.LootItem> guaranteed = new ArrayList<LootbagDefinition.LootItem>();
                for (String[] slot : this.edGuaranteed) {
                    if (slot[0] == null || slot[0].trim().isEmpty()) continue;
                    guaranteed.add(new LootbagDefinition.LootItem(slot[0].trim(), LootbagAdminPage.parseIntSafe(slot[1], 1)));
                }
                ArrayList<LootbagDefinition.PoolItem> pool = new ArrayList<LootbagDefinition.PoolItem>();
                for (String[] slot : this.edPool) {
                    if (slot[0] == null || slot[0].trim().isEmpty()) continue;
                    pool.add(new LootbagDefinition.PoolItem(slot[0].trim(), LootbagAdminPage.parseIntSafe(slot[1], 1), LootbagAdminPage.parseIntSafe(slot[2], 10)));
                }
                def.setGuaranteedItems(guaranteed.isEmpty() ? null : guaranteed);
                def.setPool(pool.isEmpty() ? null : pool);
                def.setPickCount(LootbagAdminPage.parseIntSafe(this.edPickCount, 1));
                def.setAllowDuplicates(this.edAllowDuplicates);
                def.setItems(null);
            }
        }
        if (this.creatingNew || this.edOriginalId == null) {
            this.service.addCustom(saveId, def);
        } else {
            this.service.updateCustom(this.edOriginalId, saveId, def);
        }
        this.service.saveCustomLootbags();
        this.saveLocValues(saveId);
        this.selectedLootbagId = saveId;
        this.showingEditor = false;
        this.showingBrowse = false;
        this.locEditorFieldIndex = -1;
        this.filterLootbags();
        LOGGER.info("Saved lootbag: " + saveId + " (type: " + this.edType + ")");
    }

    private void openBrowse(String target) {
        this.showingBrowse = true;
        this.browseTarget = target;
        this.browseHelper.openForTarget(target);
    }

    private void closeBrowse() {
        this.showingBrowse = false;
        this.browseTarget = null;
        this.browseHelper.close();
    }

    private void loadBrowseItems(List<String[]> items, String target, String filter) {
        BrowsePanelHelper.loadByFilter(items, filter);
    }

    private void handleBrowseSelection(String itemId) {
        int idx;
        if (this.browseTarget == null || itemId == null) {
            this.refreshUI();
            return;
        }
        if ("icon".equals(this.browseTarget)) {
            this.edIcon = itemId;
        } else if (this.browseTarget.startsWith("item_")) {
            int idx2 = LootbagAdminPage.parseIntSafe(this.browseTarget.substring(5), -1);
            if (idx2 >= 0 && idx2 < this.edItems.size()) {
                this.edItems.get((int)idx2)[0] = itemId;
            }
        } else if (this.browseTarget.startsWith("pool_")) {
            int idx3 = LootbagAdminPage.parseIntSafe(this.browseTarget.substring(5), -1);
            if (idx3 >= 0 && idx3 < this.edPool.size()) {
                this.edPool.get((int)idx3)[0] = itemId;
            }
        } else if (this.browseTarget.startsWith("guar_") && (idx = LootbagAdminPage.parseIntSafe(this.browseTarget.substring(5), -1)) >= 0 && idx < this.edGuaranteed.size()) {
            this.edGuaranteed.get((int)idx)[0] = itemId;
        }
        this.closeBrowse();
        this.refreshUI();
    }

    private void loadLocValues() {
        if (this.edId == null || this.edId.trim().isEmpty()) {
            return;
        }
        CoreI18n i18n = CoreI18n.getInstance();
        String id = this.edId.trim();
        this.locValues.clear();
        for (String lang : LOC_LANGUAGES) {
            String descVal;
            String nameKey = "lootbag.name." + id;
            String descKey = "lootbag.desc." + id;
            String nameVal = i18n.has(nameKey) ? i18n.get(nameKey) : "";
            String string = descVal = i18n.has(descKey) ? i18n.get(descKey) : "";
            if (nameVal.equals(nameKey)) {
                nameVal = "";
            }
            if (descVal.equals(descKey)) {
                descVal = "";
            }
            this.locValues.put(lang, new String[]{nameVal, descVal});
        }
    }

    private void saveLocValues(String id) {
        CoreI18n i18n = CoreI18n.getInstance();
        for (String lang : LOC_LANGUAGES) {
            String[] vals = this.locValues.get(lang);
            if (vals == null) continue;
            if (vals[0] != null && !vals[0].isEmpty()) {
                i18n.setCustomTranslation(lang, "lootbag.name." + id, vals[0]);
            }
            if (vals[1] == null || vals[1].isEmpty()) continue;
            i18n.setCustomTranslation(lang, "lootbag.desc." + id, vals[1]);
        }
    }

    private void buildLocEditorOverlay(UICommandBuilder ui) {
        boolean showing = this.locEditorFieldIndex >= 0;
        ui.set("#LocEditorOverlay.Visible", showing);
        if (!showing) {
            return;
        }
        ui.set("#LocEdTitle.Text", "LOCALIZATION: " + LOC_LABELS[this.locEditorFieldIndex]);
        for (int i = 0; i < LOC_LANGUAGES.length; ++i) {
            ui.set("#LocEdLang" + i + ".Text", LOC_LANGUAGES[i]);
            String[] vals = this.locValues.get(LOC_LANGUAGES[i]);
            String val = vals != null && this.locEditorFieldIndex < vals.length ? vals[this.locEditorFieldIndex] : "";
            ui.set("#LocEdField" + i + ".Value", val != null ? val : "");
        }
    }

    private void filterLootbags() {
        Map<String, LootbagDefinition> all = this.service.getAll();
        String query = this.searchQuery != null ? this.searchQuery.trim().toLowerCase() : "";
        this.filteredLootbags = all.entrySet().stream().filter(entry -> {
            LootbagDefinition def = (LootbagDefinition)entry.getValue();
            if ("custom".equals(this.rarityFilter) ? !this.service.isCustom((String)entry.getKey()) : !"all".equals(this.rarityFilter) && !this.rarityFilter.equalsIgnoreCase(def.getRarity())) {
                return false;
            }
            if (!query.isEmpty()) {
                String id = ((String)entry.getKey()).toLowerCase();
                String name = def.getName() != null ? def.getName().toLowerCase() : "";
                return id.contains(query) || name.contains(query);
            }
            return true;
        }).sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());
    }

    private String formatLootbagType(LootbagDefinition def) {
        CoreI18n i18n = CoreI18n.getInstance();
        if (def.isHybrid()) {
            return i18n.get("lootbag.admin.type.hybrid");
        }
        if (def.isRandomPool()) {
            return i18n.get("lootbag.admin.type.random");
        }
        if (def.isFixedItems()) {
            return i18n.get("lootbag.admin.type.fixed");
        }
        return i18n.get("lootbag.admin.type.empty");
    }

    private int countLootbagItems(LootbagDefinition def) {
        int count = 0;
        if (def.getItems() != null) {
            count += def.getItems().size();
        }
        if (def.getGuaranteedItems() != null) {
            count += def.getGuaranteedItems().size();
        }
        if (def.getPool() != null) {
            count += def.getPool().size();
        }
        return count;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private static int parseIntSafe(String s, int def) {
        if (s == null || s.trim().isEmpty()) {
            return def;
        }
        try {
            return Integer.parseInt(s.trim());
        }
        catch (NumberFormatException e) {
            return def;
        }
    }

    private static /* synthetic */ String[] lambda$handleDataEvent$1(String k) {
        return new String[]{"", ""};
    }

    public static class PageEventData {
        public static final BuilderCodec<PageEventData> CODEC;
        String button;
        String rarityFilter;
        String lootSearch;
        String edRarity;
        String edType;
        String edId;
        String edName;
        String edDesc;
        String edIcon;
        String edPickCount;
        Boolean edDuplicates;
        String[] edItemIds = new String[10];
        String[] edItemAmts = new String[10];
        String[] edPoolIds = new String[10];
        String[] edPoolAmts = new String[10];
        String[] edPoolWts = new String[10];
        String[] edGuarIds = new String[5];
        String[] edGuarAmts = new String[5];
        String browseSearch;
        String browseFilterValue;
        String browseSelect;
        String[] locEdFields = new String[9];

        static {
            int idx;
            int i;
            BuilderCodec.Builder builder = (BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)BuilderCodec.builder(PageEventData.class, PageEventData::new).addField(new KeyedCodec("Button", (Codec)Codec.STRING), (d, v) -> {
                d.button = v;
            }, d -> d.button)).addField(new KeyedCodec("@RarityFilter", (Codec)Codec.STRING), (d, v) -> {
                d.rarityFilter = v;
            }, d -> d.rarityFilter)).addField(new KeyedCodec("@LootSearch", (Codec)Codec.STRING), (d, v) -> {
                d.lootSearch = v;
            }, d -> d.lootSearch)).addField(new KeyedCodec("@EdRarity", (Codec)Codec.STRING), (d, v) -> {
                d.edRarity = v;
            }, d -> d.edRarity)).addField(new KeyedCodec("@EdType", (Codec)Codec.STRING), (d, v) -> {
                d.edType = v;
            }, d -> d.edType)).addField(new KeyedCodec("@EdId", (Codec)Codec.STRING), (d, v) -> {
                d.edId = v;
            }, d -> d.edId)).addField(new KeyedCodec("@EdName", (Codec)Codec.STRING), (d, v) -> {
                d.edName = v;
            }, d -> d.edName)).addField(new KeyedCodec("@EdDesc", (Codec)Codec.STRING), (d, v) -> {
                d.edDesc = v;
            }, d -> d.edDesc)).addField(new KeyedCodec("@EdIcon", (Codec)Codec.STRING), (d, v) -> {
                d.edIcon = v;
            }, d -> d.edIcon)).addField(new KeyedCodec("@EdPickCount", (Codec)Codec.STRING), (d, v) -> {
                d.edPickCount = v;
            }, d -> d.edPickCount)).addField(new KeyedCodec("@EdDuplicates", (Codec)Codec.BOOLEAN), (d, v) -> {
                d.edDuplicates = v;
            }, d -> d.edDuplicates)).addField(new KeyedCodec("@BrowseSearch", (Codec)Codec.STRING), (d, v) -> {
                d.browseSearch = v;
            }, d -> d.browseSearch)).addField(new KeyedCodec("@BrowseFilterValue", (Codec)Codec.STRING), (d, v) -> {
                d.browseFilterValue = v;
            }, d -> d.browseFilterValue)).addField(new KeyedCodec("BrowseSelect", (Codec)Codec.STRING), (d, v) -> {
                d.browseSelect = v;
            }, d -> d.browseSelect)).addField(new KeyedCodec("@LocEdField0", (Codec)Codec.STRING), (d, v) -> {
                d.locEdFields[0] = v;
            }, d -> d.locEdFields[0])).addField(new KeyedCodec("@LocEdField1", (Codec)Codec.STRING), (d, v) -> {
                d.locEdFields[1] = v;
            }, d -> d.locEdFields[1])).addField(new KeyedCodec("@LocEdField2", (Codec)Codec.STRING), (d, v) -> {
                d.locEdFields[2] = v;
            }, d -> d.locEdFields[2])).addField(new KeyedCodec("@LocEdField3", (Codec)Codec.STRING), (d, v) -> {
                d.locEdFields[3] = v;
            }, d -> d.locEdFields[3])).addField(new KeyedCodec("@LocEdField4", (Codec)Codec.STRING), (d, v) -> {
                d.locEdFields[4] = v;
            }, d -> d.locEdFields[4])).addField(new KeyedCodec("@LocEdField5", (Codec)Codec.STRING), (d, v) -> {
                d.locEdFields[5] = v;
            }, d -> d.locEdFields[5])).addField(new KeyedCodec("@LocEdField6", (Codec)Codec.STRING), (d, v) -> {
                d.locEdFields[6] = v;
            }, d -> d.locEdFields[6])).addField(new KeyedCodec("@LocEdField7", (Codec)Codec.STRING), (d, v) -> {
                d.locEdFields[7] = v;
            }, d -> d.locEdFields[7])).addField(new KeyedCodec("@LocEdField8", (Codec)Codec.STRING), (d, v) -> {
                d.locEdFields[8] = v;
            }, d -> d.locEdFields[8]);
            for (i = 0; i < 10; ++i) {
                idx = i;
                builder = (BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)builder.addField(new KeyedCodec("@EdItemId" + i, (Codec)Codec.STRING), (d, v) -> {
                    d.edItemIds[idx] = v;
                }, d -> d.edItemIds[idx])).addField(new KeyedCodec("@EdItemAmt" + i, (Codec)Codec.STRING), (d, v) -> {
                    d.edItemAmts[idx] = v;
                }, d -> d.edItemAmts[idx])).addField(new KeyedCodec("@EdPoolId" + i, (Codec)Codec.STRING), (d, v) -> {
                    d.edPoolIds[idx] = v;
                }, d -> d.edPoolIds[idx])).addField(new KeyedCodec("@EdPoolAmt" + i, (Codec)Codec.STRING), (d, v) -> {
                    d.edPoolAmts[idx] = v;
                }, d -> d.edPoolAmts[idx])).addField(new KeyedCodec("@EdPoolWt" + i, (Codec)Codec.STRING), (d, v) -> {
                    d.edPoolWts[idx] = v;
                }, d -> d.edPoolWts[idx]);
            }
            for (i = 0; i < 5; ++i) {
                idx = i;
                builder = (BuilderCodec.Builder)((BuilderCodec.Builder)builder.addField(new KeyedCodec("@EdGuarId" + i, (Codec)Codec.STRING), (d, v) -> {
                    d.edGuarIds[idx] = v;
                }, d -> d.edGuarIds[idx])).addField(new KeyedCodec("@EdGuarAmt" + i, (Codec)Codec.STRING), (d, v) -> {
                    d.edGuarAmts[idx] = v;
                }, d -> d.edGuarAmts[idx]);
            }
            CODEC = builder.build();
        }
    }
}

