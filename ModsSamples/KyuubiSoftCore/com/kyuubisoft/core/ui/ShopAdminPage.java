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
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
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
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.CorePlugin;
import com.kyuubisoft.core.api.CoreAPI;
import com.kyuubisoft.core.i18n.CoreI18n;
import com.kyuubisoft.core.i18n.I18nContext;
import com.kyuubisoft.core.registry.ModMenuRegistry;
import com.kyuubisoft.core.shop.ShopConfig;
import com.kyuubisoft.core.shop.ShopService;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class ShopAdminPage
extends InteractiveCustomUIPage<PageEventData> {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Shop Admin");
    private static final int SHOPS_PER_PAGE = 8;
    private static final int ITEMS_PER_PAGE = 10;
    private final CorePlugin plugin;
    private final Player player;
    private final PlayerRef playerRef;
    private final ShopService shopService;
    private Ref<EntityStore> storedRef;
    private Store<EntityStore> storedStore;
    private boolean showingDetail = false;
    private List<ShopConfig> shopList = new ArrayList<ShopConfig>();
    private int shopPage = 0;
    private int selectedShopIndex = -1;
    private ShopConfig currentShop = null;
    private boolean creatingNewShop = false;
    private List<ShopConfig.ShopItemConfig> currentItems = new ArrayList<ShopConfig.ShopItemConfig>();
    private int itemPage = 0;
    private int selectedItemIndex = -1;
    private String itemSearchFilter = "";
    private String editId = "";
    private String editTitle = "";
    private String editCurrency = "";
    private String editDailySize = "0";
    private String editPerPage = "9";
    private String editSellPrice = "50";
    private String editSellCurrency = "";
    private String editWhitelist = "";
    private String editBlacklist = "";
    private boolean showingItemEditor = false;
    private boolean creatingNewItem = false;
    private boolean showingDeleteConfirm = false;
    private String deleteType = null;
    private String itemEdId = "";
    private String itemEdName = "";
    private String itemEdDesc = "";
    private String itemEdQty = "1";
    private String itemEdCost = "0";
    private String itemEdSellPrice = "0";
    private String itemEdWeight = "100";
    private String itemEdCategory = "";
    private String itemEdMaxPurch = "0";
    private String itemEdMaxSells = "0";
    private String itemEdPerm = "";
    private String itemEdCommand = "";
    private String itemEdCmdServer = "true";

    public ShopAdminPage(CorePlugin plugin, Player player, PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageEventData.CODEC);
        this.plugin = plugin;
        this.player = player;
        this.playerRef = playerRef;
        this.shopService = ShopService.getInstance();
        this.loadShopList();
    }

    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder ui, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        I18nContext.run(this.playerRef, () -> {
            this.storedRef = ref;
            this.storedStore = store;
            ui.append("Pages/ShopAdmin/ShopAdmin.ui");
            this.bindEvents(ui, events);
            this.buildUI(ui);
        });
    }

    private void bindEvents(UICommandBuilder ui, UIEventBuilder events) {
        int i;
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of((String)"Button", (String)"back"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of((String)"Button", (String)"close"), false);
        for (i = 0; i < 8; ++i) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ShopEntry" + i, EventData.of((String)"Button", (String)("selectShop" + i)), false);
        }
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ShopPrevBtn", EventData.of((String)"Button", (String)"shopPrev"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ShopNextBtn", EventData.of((String)"Button", (String)"shopNext"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddShopBtn", EventData.of((String)"Button", (String)"addShop"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteShopBtn", EventData.of((String)"Button", (String)"deleteShop"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#OpenShopBtn", EventData.of((String)"Button", (String)"openShop"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DetailBackBtn", EventData.of((String)"Button", (String)"detailBack"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DetailSaveBtn", EventData.of((String)"Button", (String)"detailSave"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#DetailIdField", EventData.of((String)"@EditId", (String)"#DetailIdField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#DetailTitleField", EventData.of((String)"@EditTitle", (String)"#DetailTitleField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#DetailCurrencyField", EventData.of((String)"@EditCurrency", (String)"#DetailCurrencyField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#DetailDailySizeField", EventData.of((String)"@DailySize", (String)"#DetailDailySizeField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#DetailPerPageField", EventData.of((String)"@PerPage", (String)"#DetailPerPageField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BuyEnabledBtn", EventData.of((String)"Button", (String)"toggleBuy"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SellEnabledBtn", EventData.of((String)"Button", (String)"toggleSell"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SellPriceField", EventData.of((String)"@SellPrice", (String)"#SellPriceField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SellCurrField", EventData.of((String)"@SellCurr", (String)"#SellCurrField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SellWhitelistField", EventData.of((String)"@SellWhitelist", (String)"#SellWhitelistField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SellBlacklistField", EventData.of((String)"@SellBlacklist", (String)"#SellBlacklistField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ItemSearchField", EventData.of((String)"@ItemSearch", (String)"#ItemSearchField.Value"), false);
        for (i = 0; i < 10; ++i) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ItemEntry" + i, EventData.of((String)"Button", (String)("selectItem" + i)), false);
        }
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ItemPrevBtn", EventData.of((String)"Button", (String)"itemPrev"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ItemNextBtn", EventData.of((String)"Button", (String)"itemNext"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddItemBtn", EventData.of((String)"Button", (String)"addItem"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#EditItemBtn", EventData.of((String)"Button", (String)"editItem"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteItemBtn", EventData.of((String)"Button", (String)"deleteItem"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ItemEdIdField", EventData.of((String)"@ItemEdId", (String)"#ItemEdIdField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ItemEdNameField", EventData.of((String)"@ItemEdName", (String)"#ItemEdNameField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ItemEdDescField", EventData.of((String)"@ItemEdDesc", (String)"#ItemEdDescField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ItemEdQtyField", EventData.of((String)"@ItemEdQty", (String)"#ItemEdQtyField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ItemEdCostField", EventData.of((String)"@ItemEdCost", (String)"#ItemEdCostField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ItemEdSellPriceField", EventData.of((String)"@ItemEdSellPrice", (String)"#ItemEdSellPriceField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ItemEdWeightField", EventData.of((String)"@ItemEdWeight", (String)"#ItemEdWeightField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ItemEdCategoryField", EventData.of((String)"@ItemEdCategory", (String)"#ItemEdCategoryField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ItemEdMaxPurchField", EventData.of((String)"@ItemEdMaxPurch", (String)"#ItemEdMaxPurchField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ItemEdMaxSellsField", EventData.of((String)"@ItemEdMaxSells", (String)"#ItemEdMaxSellsField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ItemEdPermField", EventData.of((String)"@ItemEdPerm", (String)"#ItemEdPermField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ItemEdCommandField", EventData.of((String)"@ItemEdCommand", (String)"#ItemEdCommandField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ItemEdCmdServerBtn", EventData.of((String)"Button", (String)"itemEdToggleCmdServer"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ItemEdCancel", EventData.of((String)"Button", (String)"itemEdCancel"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ItemEdSave", EventData.of((String)"Button", (String)"itemEdSave"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DelConfirmYes", EventData.of((String)"Button", (String)"deleteYes"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DelConfirmNo", EventData.of((String)"Button", (String)"deleteNo"), false);
    }

    private void refreshUI() {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        this.bindEvents(ui, events);
        this.buildUI(ui);
        this.sendUpdate(ui, events, false);
    }

    private void buildUI(UICommandBuilder ui) {
        CoreI18n i18n = CoreI18n.getInstance();
        ui.set("#ShopListView.Visible", !this.showingDetail);
        ui.set("#ShopDetailView.Visible", this.showingDetail);
        ui.set("#ShopsHeader.Text", i18n.get("admin.shops.header"));
        ui.set("#ColId.Text", i18n.get("admin.shops.col.id"));
        ui.set("#ColTitle.Text", i18n.get("admin.shops.col.title"));
        ui.set("#ColCurrency.Text", i18n.get("admin.shops.col.currency"));
        ui.set("#ColItems.Text", i18n.get("admin.shops.col.items"));
        ui.set("#ColDaily.Text", i18n.get("admin.shops.col.daily"));
        ui.set("#ColSell.Text", i18n.get("admin.shops.col.sell"));
        ui.set("#AddShopLabel.Text", i18n.get("admin.shops.btn.add_shop"));
        ui.set("#DeleteShopLabel.Text", i18n.get("admin.shops.btn.delete"));
        ui.set("#OpenDetailLabel.Text", i18n.get("admin.shops.btn.open_detail"));
        ui.set("#DetailIdLabel.Text", i18n.get("admin.shops.editor.field.id"));
        ui.set("#DetailTitleLabel.Text", i18n.get("admin.shops.editor.field.title"));
        ui.set("#CurrencyLabel.Text", i18n.get("admin.shops.detail.currency"));
        ui.set("#DailySizeLabel.Text", i18n.get("admin.shops.detail.daily_size"));
        ui.set("#ItemsPerPageLabel.Text", i18n.get("admin.shops.detail.items_per_page"));
        ui.set("#BuyEnabledLabel.Text", i18n.get("admin.shops.buy.enabled"));
        ui.set("#SellEnabledLabel.Text", i18n.get("admin.shops.sell.enabled"));
        ui.set("#SellPriceLabel.Text", i18n.get("admin.shops.sell.price"));
        ui.set("#SellCurrLabel.Text", i18n.get("admin.shops.sell.currency"));
        ui.set("#SellWhitelistLabel.Text", i18n.get("admin.shops.sell.whitelist"));
        ui.set("#SellBlacklistLabel.Text", i18n.get("admin.shops.sell.blacklist"));
        ui.set("#ItemsHeader.Text", i18n.get("admin.shops.detail.items_header"));
        ui.set("#SearchLabel.Text", i18n.get("admin.shops.detail.search"));
        ui.set("#ColItemId.Text", i18n.get("admin.shops.detail.col.item_id"));
        ui.set("#ColItemName.Text", i18n.get("admin.shops.detail.col.name"));
        ui.set("#ColItemCost.Text", i18n.get("admin.shops.detail.col.cost"));
        ui.set("#ColItemSellPrice.Text", i18n.get("admin.shops.detail.col.sell_price"));
        ui.set("#ColItemQty.Text", i18n.get("admin.shops.detail.col.qty"));
        ui.set("#ColItemWeight.Text", i18n.get("admin.shops.detail.col.weight"));
        ui.set("#ColItemCategory.Text", i18n.get("admin.shops.detail.col.category"));
        ui.set("#ColItemLimit.Text", i18n.get("admin.shops.detail.col.limit"));
        ui.set("#AddItemLabel.Text", i18n.get("admin.shops.detail.btn.add_item"));
        ui.set("#EditItemLabel.Text", i18n.get("admin.shops.btn.edit"));
        ui.set("#DeleteItemLabel.Text", i18n.get("admin.shops.btn.delete"));
        ui.set("#SaveShopLabel.Text", i18n.get("admin.shops.detail.btn.save_shop"));
        ui.set("#ItemEdIdLabel.Text", i18n.get("admin.shops.item_editor.field.item_id"));
        ui.set("#ItemEdNameLabel.Text", i18n.get("admin.shops.item_editor.field.name"));
        ui.set("#ItemEdDescLabel.Text", i18n.get("admin.shops.item_editor.field.description"));
        ui.set("#ItemEdQtyLabel.Text", i18n.get("admin.shops.item_editor.field.quantity"));
        ui.set("#ItemEdCostLabel.Text", i18n.get("admin.shops.item_editor.field.cost"));
        ui.set("#ItemEdSellPriceLabel.Text", i18n.get("admin.shops.item_editor.field.sell_price"));
        ui.set("#ItemEdWeightLabel.Text", i18n.get("admin.shops.item_editor.field.weight"));
        ui.set("#ItemEdMaxPurchLabel.Text", i18n.get("admin.shops.item_editor.field.max_purch"));
        ui.set("#ItemEdMaxSellsLabel.Text", i18n.get("admin.shops.item_editor.field.max_sells"));
        ui.set("#ItemEdCategoryLabel.Text", i18n.get("admin.shops.item_editor.field.category"));
        ui.set("#ItemEdPermLabel.Text", i18n.get("admin.shops.item_editor.field.permission"));
        ui.set("#ItemEdCommandLabel.Text", i18n.get("admin.shops.item_editor.field.command"));
        ui.set("#ItemEdCmdServerLabel.Text", i18n.get("admin.shops.item_editor.field.run_as"));
        ui.set("#ItemEdCancelLabel.Text", i18n.get("admin.shops.editor.btn.cancel"));
        ui.set("#ItemEdSaveLabel.Text", i18n.get("admin.shops.editor.btn.save"));
        ui.set("#DelConfirmNoLabel.Text", i18n.get("admin.shops.delete_dialog.no"));
        ui.set("#DelConfirmYesLabel.Text", i18n.get("admin.shops.delete_dialog.yes"));
        if (this.showingDetail) {
            this.buildDetailView(ui);
        } else {
            this.buildShopList(ui);
        }
        this.buildItemEditor(ui);
        this.buildDeleteConfirm(ui);
    }

    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageEventData data) {
        I18nContext.run(this.playerRef, () -> {
            super.handleDataEvent(ref, store, (Object)data);
            this.storedRef = ref;
            this.storedStore = store;
            if (data.button != null) {
                this.handleButton(data.button);
                return;
            }
            if (data.editId != null) {
                this.editId = data.editId;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.editTitle != null) {
                this.editTitle = data.editTitle;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.editCurrency != null) {
                this.editCurrency = data.editCurrency;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.dailySize != null) {
                this.editDailySize = data.dailySize;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.perPage != null) {
                this.editPerPage = data.perPage;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.sellPrice != null) {
                this.editSellPrice = data.sellPrice;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.sellCurr != null) {
                this.editSellCurrency = data.sellCurr;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.sellWhitelist != null) {
                this.editWhitelist = data.sellWhitelist;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.sellBlacklist != null) {
                this.editBlacklist = data.sellBlacklist;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.itemSearch != null) {
                this.itemSearchFilter = data.itemSearch;
                this.itemPage = 0;
                this.selectedItemIndex = -1;
                this.refreshUI();
                return;
            }
            if (data.itemEdId != null) {
                this.itemEdId = data.itemEdId;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.itemEdName != null) {
                this.itemEdName = data.itemEdName;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.itemEdDesc != null) {
                this.itemEdDesc = data.itemEdDesc;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.itemEdQty != null) {
                this.itemEdQty = data.itemEdQty;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.itemEdCost != null) {
                this.itemEdCost = data.itemEdCost;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.itemEdSellPrice != null) {
                this.itemEdSellPrice = data.itemEdSellPrice;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.itemEdWeight != null) {
                this.itemEdWeight = data.itemEdWeight;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.itemEdCategory != null) {
                this.itemEdCategory = data.itemEdCategory;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.itemEdMaxPurch != null) {
                this.itemEdMaxPurch = data.itemEdMaxPurch;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.itemEdMaxSells != null) {
                this.itemEdMaxSells = data.itemEdMaxSells;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.itemEdPerm != null) {
                this.itemEdPerm = data.itemEdPerm;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.itemEdCommand != null) {
                this.itemEdCommand = data.itemEdCommand;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            this.sendUpdate(new UICommandBuilder(), false);
        });
    }

    private void handleButton(String button) {
        switch (button) {
            case "addShop": 
            case "deleteShop": 
            case "detailSave": 
            case "toggleBuy": 
            case "toggleSell": 
            case "addItem": 
            case "editItem": 
            case "deleteItem": 
            case "itemEdSave": 
            case "deleteYes": {
                if (!CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) break;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
        }
        switch (button) {
            case "back": {
                if (this.showingDetail) {
                    this.showingDetail = false;
                    this.currentShop = null;
                    this.selectedItemIndex = -1;
                    this.loadShopList();
                    break;
                }
                ModMenuRegistry.openCoreAdmin(this.player, this.playerRef, this.storedRef, this.storedStore);
                return;
            }
            case "close": {
                this.close();
                return;
            }
            case "shopPrev": {
                if (this.shopPage <= 0) break;
                --this.shopPage;
                break;
            }
            case "shopNext": {
                int totalPages = Math.max(1, (int)Math.ceil((double)this.shopList.size() / 8.0));
                if (this.shopPage >= totalPages - 1) break;
                ++this.shopPage;
                break;
            }
            case "addShop": {
                this.creatingNewShop = true;
                this.currentShop = new ShopConfig();
                this.currentShop.id = "";
                this.currentShop.title = "";
                this.currentShop.currencyId = "item:Ingredient_Bar_Gold";
                this.currentItems = new ArrayList<ShopConfig.ShopItemConfig>();
                this.showingDetail = true;
                this.loadDetailFields();
                break;
            }
            case "deleteShop": {
                if (this.getSelectedShop() == null) break;
                this.deleteType = "shop";
                this.showingDeleteConfirm = true;
                break;
            }
            case "openShop": {
                ShopConfig sel = this.getSelectedShop();
                if (sel == null) break;
                this.openDetailView(sel);
                break;
            }
            case "detailBack": {
                this.showingDetail = false;
                this.currentShop = null;
                this.creatingNewShop = false;
                this.selectedItemIndex = -1;
                this.loadShopList();
                break;
            }
            case "detailSave": {
                this.saveCurrentShop();
                break;
            }
            case "toggleBuy": {
                if (this.currentShop == null) break;
                this.currentShop.buyEnabled = !this.currentShop.buyEnabled;
                break;
            }
            case "toggleSell": {
                if (this.currentShop == null) break;
                this.currentShop.sellEnabled = !this.currentShop.sellEnabled;
                break;
            }
            case "itemPrev": {
                if (this.itemPage <= 0) break;
                --this.itemPage;
                break;
            }
            case "itemNext": {
                List<ShopConfig.ShopItemConfig> filtered = this.getFilteredItems();
                int totalPages = Math.max(1, (int)Math.ceil((double)filtered.size() / 10.0));
                if (this.itemPage >= totalPages - 1) break;
                ++this.itemPage;
                break;
            }
            case "addItem": {
                this.openItemEditor(null);
                break;
            }
            case "editItem": {
                ShopConfig.ShopItemConfig sel = this.getSelectedItem();
                if (sel == null) break;
                this.openItemEditor(sel);
                break;
            }
            case "deleteItem": {
                if (this.getSelectedItem() == null) break;
                this.deleteType = "item";
                this.showingDeleteConfirm = true;
                break;
            }
            case "itemEdToggleCmdServer": {
                this.itemEdCmdServer = "true".equals(this.itemEdCmdServer) ? "false" : "true";
                break;
            }
            case "itemEdCancel": {
                this.showingItemEditor = false;
                break;
            }
            case "itemEdSave": {
                this.saveItemEditor();
                break;
            }
            case "deleteYes": {
                this.executeDelete();
                break;
            }
            case "deleteNo": {
                this.showingDeleteConfirm = false;
                break;
            }
            default: {
                int idx;
                if (button.startsWith("selectShop")) {
                    int actualIdx;
                    int idx2 = ShopAdminPage.parseIntSafe(button.substring(10), -1);
                    if (idx2 < 0 || (actualIdx = this.shopPage * 8 + idx2) < 0 || actualIdx >= this.shopList.size()) break;
                    this.selectedShopIndex = actualIdx;
                    break;
                }
                if (!button.startsWith("selectItem") || (idx = ShopAdminPage.parseIntSafe(button.substring(10), -1)) < 0) break;
                List<ShopConfig.ShopItemConfig> filtered = this.getFilteredItems();
                int actualIdx = this.itemPage * 10 + idx;
                if (actualIdx < 0 || actualIdx >= filtered.size()) break;
                this.selectedItemIndex = actualIdx;
            }
        }
        this.refreshUI();
    }

    private void buildShopList(UICommandBuilder ui) {
        CoreI18n i18n = CoreI18n.getInstance();
        ui.set("#ShopCountLabel.Text", i18n.get("admin.shops.count", this.shopList.size()));
        int start = this.shopPage * 8;
        for (int i = 0; i < 8; ++i) {
            String prefix = "#ShopEntry" + i;
            int actualIdx = start + i;
            if (actualIdx < this.shopList.size()) {
                ShopConfig config = this.shopList.get(actualIdx);
                ui.set(prefix + ".Visible", true);
                ui.set(prefix + " #ShopId" + i + ".Text", config.id != null ? config.id : "");
                ui.set(prefix + " #ShopTitle" + i + ".Text", config.title != null ? config.title : "");
                ui.set(prefix + " #ShopCurrency" + i + ".Text", config.currencyId != null ? config.currencyId : "");
                ui.set(prefix + " #ShopItemCount" + i + ".Text", String.valueOf(config.items.size()));
                ui.set(prefix + " #ShopDaily" + i + ".Text", config.settings.dailyShopSize > 0 ? String.valueOf(config.settings.dailyShopSize) : "-");
                ui.set(prefix + " #ShopSell" + i + ".Text", config.sellEnabled ? "Yes" : "-");
                boolean isSelected = actualIdx == this.selectedShopIndex;
                ui.set(prefix + ".Background", isSelected ? "#ffd70030" : "#ffffff06");
                continue;
            }
            ui.set(prefix + ".Visible", false);
        }
        int totalPages = Math.max(1, (int)Math.ceil((double)this.shopList.size() / 8.0));
        if (this.shopPage >= totalPages) {
            this.shopPage = totalPages - 1;
        }
        if (this.shopPage < 0) {
            this.shopPage = 0;
        }
        ui.set("#ShopPageLabel.Text", this.shopPage + 1 + " / " + totalPages);
        ui.set("#ShopPrevBtn.Visible", this.shopPage > 0);
        ui.set("#ShopNextBtn.Visible", this.shopPage < totalPages - 1);
        boolean hasSelection = this.selectedShopIndex >= 0 && this.selectedShopIndex < this.shopList.size();
        ui.set("#DeleteShopBtn.Visible", hasSelection);
        ui.set("#OpenShopBtn.Visible", hasSelection);
    }

    private void buildDetailView(UICommandBuilder ui) {
        if (this.currentShop == null) {
            return;
        }
        CoreI18n i18n = CoreI18n.getInstance();
        ui.set("#DetailTitle.Text", this.creatingNewShop ? i18n.get("admin.shops.editor.new_shop") : (this.currentShop.id != null ? this.currentShop.id : "Shop"));
        ui.set("#DetailIdField.Value", this.editId);
        ui.set("#DetailTitleField.Value", this.editTitle);
        ui.set("#DetailCurrencyField.Value", this.editCurrency);
        ui.set("#DetailDailySizeField.Value", this.editDailySize);
        ui.set("#DetailPerPageField.Value", this.editPerPage);
        boolean buyOn = this.currentShop.buyEnabled;
        ui.set("#BuyEnabledLabel.Text", i18n.get("admin.shops.buy.enabled"));
        ui.set("#BuyEnabledBtnLabel.Text", buyOn ? i18n.get("admin.shops.sell.on") : i18n.get("admin.shops.sell.off"));
        boolean sellOn = this.currentShop.sellEnabled;
        ui.set("#SellEnabledBtnLabel.Text", sellOn ? i18n.get("admin.shops.sell.on") : i18n.get("admin.shops.sell.off"));
        ui.set("#SellPriceField.Value", this.editSellPrice);
        ui.set("#SellCurrField.Value", this.editSellCurrency);
        ui.set("#SellWhitelistField.Value", this.editWhitelist);
        ui.set("#SellBlacklistField.Value", this.editBlacklist);
        List<ShopConfig.ShopItemConfig> filtered = this.getFilteredItems();
        if (this.currentItems.size() != filtered.size()) {
            ui.set("#ItemCountLabel.Text", i18n.get("admin.shops.detail.item_count_filtered", filtered.size(), this.currentItems.size()));
        } else {
            ui.set("#ItemCountLabel.Text", i18n.get("admin.shops.detail.item_count", filtered.size()));
        }
        int start = this.itemPage * 10;
        for (int i = 0; i < 10; ++i) {
            String prefix = "#ItemEntry" + i;
            int actualIdx = start + i;
            if (actualIdx < filtered.size()) {
                ShopConfig.ShopItemConfig item = filtered.get(actualIdx);
                ui.set(prefix + ".Visible", true);
                ui.set(prefix + " #ItemId" + i + ".Text", item.itemId != null ? item.itemId : "");
                ui.set(prefix + " #ItemName" + i + ".Text", item.name != null ? item.name : "");
                ui.set(prefix + " #ItemCost" + i + ".Text", String.valueOf(item.cost));
                ui.set(prefix + " #ItemSellPrice" + i + ".Text", item.sellPrice > 0 ? String.valueOf(item.sellPrice) : "-");
                ui.set(prefix + " #ItemQty" + i + ".Text", String.valueOf(item.quantity));
                ui.set(prefix + " #ItemWeight" + i + ".Text", String.valueOf(item.weight));
                ui.set(prefix + " #ItemCategory" + i + ".Text", item.category != null ? item.category : "");
                ui.set(prefix + " #ItemMaxPurch" + i + ".Text", item.maxPurchases > 0 ? String.valueOf(item.maxPurchases) : "-");
                boolean isSelected = actualIdx == this.selectedItemIndex;
                ui.set(prefix + ".Background", isSelected ? "#ffd70030" : "#ffffff06");
                continue;
            }
            ui.set(prefix + ".Visible", false);
        }
        int totalPages = Math.max(1, (int)Math.ceil((double)filtered.size() / 10.0));
        if (this.itemPage >= totalPages) {
            this.itemPage = totalPages - 1;
        }
        if (this.itemPage < 0) {
            this.itemPage = 0;
        }
        ui.set("#ItemPageLabel.Text", this.itemPage + 1 + " / " + totalPages);
        ui.set("#ItemPrevBtn.Visible", this.itemPage > 0);
        ui.set("#ItemNextBtn.Visible", this.itemPage < totalPages - 1);
        boolean hasItemSelection = this.selectedItemIndex >= 0 && this.selectedItemIndex < filtered.size();
        ui.set("#EditItemBtn.Visible", hasItemSelection);
        ui.set("#DeleteItemBtn.Visible", hasItemSelection);
    }

    private void buildItemEditor(UICommandBuilder ui) {
        ui.set("#ItemEditorBackdrop.Visible", this.showingItemEditor);
        if (!this.showingItemEditor) {
            return;
        }
        CoreI18n i18n = CoreI18n.getInstance();
        ui.set("#ItemEdTitle.Text", this.creatingNewItem ? i18n.get("admin.shops.item_editor.new_item") : i18n.get("admin.shops.item_editor.edit_item"));
        ui.set("#ItemEdIdField.Value", this.itemEdId != null ? this.itemEdId : "");
        ui.set("#ItemEdNameField.Value", this.itemEdName != null ? this.itemEdName : "");
        ui.set("#ItemEdDescField.Value", this.itemEdDesc != null ? this.itemEdDesc : "");
        ui.set("#ItemEdQtyField.Value", this.itemEdQty != null ? this.itemEdQty : "1");
        ui.set("#ItemEdCostField.Value", this.itemEdCost != null ? this.itemEdCost : "0");
        ui.set("#ItemEdSellPriceField.Value", this.itemEdSellPrice != null ? this.itemEdSellPrice : "0");
        ui.set("#ItemEdWeightField.Value", this.itemEdWeight != null ? this.itemEdWeight : "100");
        ui.set("#ItemEdCategoryField.Value", this.itemEdCategory != null ? this.itemEdCategory : "");
        ui.set("#ItemEdMaxPurchField.Value", this.itemEdMaxPurch != null ? this.itemEdMaxPurch : "0");
        ui.set("#ItemEdMaxSellsField.Value", this.itemEdMaxSells != null ? this.itemEdMaxSells : "0");
        ui.set("#ItemEdPermField.Value", this.itemEdPerm != null ? this.itemEdPerm : "");
        ui.set("#ItemEdCommandField.Value", this.itemEdCommand != null ? this.itemEdCommand : "");
        boolean cmdServer = "true".equals(this.itemEdCmdServer);
        ui.set("#ItemEdCmdServerBtnLabel.Text", cmdServer ? i18n.get("admin.shops.item_editor.run_as.server") : i18n.get("admin.shops.item_editor.run_as.player"));
    }

    private void buildDeleteConfirm(UICommandBuilder ui) {
        ui.set("#DeleteConfirmBackdrop.Visible", this.showingDeleteConfirm);
        if (!this.showingDeleteConfirm) {
            return;
        }
        CoreI18n i18n = CoreI18n.getInstance();
        if ("shop".equals(this.deleteType)) {
            ShopConfig sel = this.getSelectedShop();
            ui.set("#DeleteConfirmText.Text", i18n.get("admin.shops.delete_confirm", sel != null ? sel.id : ""));
        } else if ("item".equals(this.deleteType)) {
            ShopConfig.ShopItemConfig sel = this.getSelectedItem();
            ui.set("#DeleteConfirmText.Text", i18n.get("admin.shops.item_delete_confirm", sel != null ? sel.itemId : ""));
        }
    }

    private void loadShopList() {
        this.shopList = new ArrayList<ShopConfig>(this.shopService.getAllShops());
        this.shopList.sort((a, b) -> {
            String aId = a.id != null ? a.id : "";
            String bId = b.id != null ? b.id : "";
            return aId.compareTo(bId);
        });
    }

    private void openDetailView(ShopConfig config) {
        this.showingDetail = true;
        this.creatingNewShop = false;
        this.currentShop = config;
        this.currentItems = new ArrayList<ShopConfig.ShopItemConfig>(config.items);
        this.itemPage = 0;
        this.selectedItemIndex = -1;
        this.itemSearchFilter = "";
        this.loadDetailFields();
    }

    private void loadDetailFields() {
        this.editId = this.currentShop.id != null ? this.currentShop.id : "";
        this.editTitle = this.currentShop.title != null ? this.currentShop.title : "";
        this.editCurrency = this.currentShop.currencyId != null ? this.currentShop.currencyId : "";
        this.editDailySize = String.valueOf(this.currentShop.settings.dailyShopSize);
        this.editPerPage = String.valueOf(this.currentShop.settings.itemsPerPage);
        this.editSellPrice = String.valueOf(this.currentShop.sellPricePercent);
        this.editSellCurrency = this.currentShop.sellCurrencyId != null ? this.currentShop.sellCurrencyId : "";
        this.editWhitelist = this.currentShop.sellWhitelist != null ? String.join((CharSequence)", ", this.currentShop.sellWhitelist) : "";
        this.editBlacklist = this.currentShop.sellBlacklist != null ? String.join((CharSequence)", ", this.currentShop.sellBlacklist) : "";
    }

    private void openItemEditor(ShopConfig.ShopItemConfig item) {
        this.showingItemEditor = true;
        boolean bl = this.creatingNewItem = item == null;
        if (item != null) {
            this.itemEdId = item.itemId != null ? item.itemId : "";
            this.itemEdName = item.name != null ? item.name : "";
            this.itemEdDesc = item.description != null ? item.description : "";
            this.itemEdQty = String.valueOf(item.quantity);
            this.itemEdCost = String.valueOf(item.cost);
            this.itemEdSellPrice = String.valueOf(item.sellPrice);
            this.itemEdWeight = String.valueOf(item.weight);
            this.itemEdCategory = item.category != null ? item.category : "";
            this.itemEdMaxPurch = String.valueOf(item.maxPurchases);
            this.itemEdMaxSells = String.valueOf(item.maxSells);
            this.itemEdPerm = item.permission != null ? item.permission : "";
            this.itemEdCommand = item.command != null ? item.command : "";
            this.itemEdCmdServer = item.commandRunAsServer ? "true" : "false";
        } else {
            this.itemEdId = "";
            this.itemEdName = "";
            this.itemEdDesc = "";
            this.itemEdQty = "1";
            this.itemEdCost = "0";
            this.itemEdSellPrice = "0";
            this.itemEdWeight = "100";
            this.itemEdCategory = "";
            this.itemEdMaxPurch = "0";
            this.itemEdMaxSells = "0";
            this.itemEdPerm = "";
            this.itemEdCommand = "";
            this.itemEdCmdServer = "true";
        }
    }

    private void saveItemEditor() {
        if (this.itemEdId == null || this.itemEdId.trim().isEmpty()) {
            this.showingItemEditor = false;
            return;
        }
        if (this.creatingNewItem) {
            ShopConfig.ShopItemConfig item = new ShopConfig.ShopItemConfig();
            this.applyItemEditorFields(item);
            this.currentItems.add(item);
            this.selectedItemIndex = this.currentItems.size() - 1;
        } else {
            ShopConfig.ShopItemConfig item = this.getSelectedItem();
            if (item != null) {
                this.applyItemEditorFields(item);
            }
        }
        this.showingItemEditor = false;
    }

    private void applyItemEditorFields(ShopConfig.ShopItemConfig item) {
        item.itemId = this.itemEdId.trim();
        item.name = this.itemEdName != null ? this.itemEdName.trim() : item.itemId;
        item.description = this.itemEdDesc != null && !this.itemEdDesc.trim().isEmpty() ? this.itemEdDesc.trim() : null;
        item.quantity = ShopAdminPage.parseIntSafe(this.itemEdQty, 1);
        item.cost = ShopAdminPage.parseIntSafe(this.itemEdCost, 0);
        item.sellPrice = ShopAdminPage.parseIntSafe(this.itemEdSellPrice, 0);
        item.weight = ShopAdminPage.parseIntSafe(this.itemEdWeight, 100);
        item.category = this.itemEdCategory != null && !this.itemEdCategory.trim().isEmpty() ? this.itemEdCategory.trim() : null;
        item.maxPurchases = ShopAdminPage.parseIntSafe(this.itemEdMaxPurch, 0);
        item.maxSells = ShopAdminPage.parseIntSafe(this.itemEdMaxSells, 0);
        item.permission = this.itemEdPerm != null && !this.itemEdPerm.trim().isEmpty() ? this.itemEdPerm.trim() : null;
        item.command = this.itemEdCommand != null && !this.itemEdCommand.trim().isEmpty() ? this.itemEdCommand.trim() : null;
        item.commandRunAsServer = "true".equals(this.itemEdCmdServer);
    }

    private void saveCurrentShop() {
        String newId;
        if (this.currentShop == null) {
            return;
        }
        String string = newId = this.editId != null ? this.editId.trim() : "";
        if (newId.isEmpty()) {
            return;
        }
        String oldId = this.currentShop.id;
        this.currentShop.id = newId;
        this.currentShop.title = this.editTitle != null ? this.editTitle.trim() : newId;
        this.currentShop.currencyId = this.editCurrency != null ? this.editCurrency.trim() : "";
        this.currentShop.settings.dailyShopSize = ShopAdminPage.parseIntSafe(this.editDailySize, 0);
        this.currentShop.settings.itemsPerPage = ShopAdminPage.parseIntSafe(this.editPerPage, 9);
        this.currentShop.sellPricePercent = ShopAdminPage.parseIntSafe(this.editSellPrice, 50);
        this.currentShop.sellCurrencyId = this.editSellCurrency != null && !this.editSellCurrency.trim().isEmpty() ? this.editSellCurrency.trim() : null;
        this.currentShop.sellWhitelist = ShopAdminPage.parseCommaList(this.editWhitelist);
        this.currentShop.sellBlacklist = ShopAdminPage.parseCommaList(this.editBlacklist);
        this.currentShop.items = new ArrayList<ShopConfig.ShopItemConfig>(this.currentItems);
        this.shopService.saveShopToFile(this.currentShop);
        this.creatingNewShop = false;
        this.loadShopList();
        LOGGER.info("Saved shop: " + this.currentShop.id + " (" + this.currentItems.size() + " items)");
        this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("admin.shops.shop_saved_detail", this.currentShop.id, this.currentItems.size())).color("#44cc88"));
    }

    private void executeDelete() {
        ShopConfig.ShopItemConfig sel;
        this.showingDeleteConfirm = false;
        if ("shop".equals(this.deleteType)) {
            ShopConfig sel2 = this.getSelectedShop();
            if (sel2 != null) {
                this.shopService.deleteShop(sel2.id);
                this.selectedShopIndex = -1;
                this.loadShopList();
                this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("admin.shops.shop_deleted", sel2.id)).color("#ff8888"));
            }
        } else if ("item".equals(this.deleteType) && (sel = this.getSelectedItem()) != null) {
            this.currentItems.remove(sel);
            this.selectedItemIndex = -1;
        }
        this.deleteType = null;
    }

    private ShopConfig getSelectedShop() {
        if (this.selectedShopIndex >= 0 && this.selectedShopIndex < this.shopList.size()) {
            return this.shopList.get(this.selectedShopIndex);
        }
        return null;
    }

    private ShopConfig.ShopItemConfig getSelectedItem() {
        List<ShopConfig.ShopItemConfig> filtered = this.getFilteredItems();
        if (this.selectedItemIndex >= 0 && this.selectedItemIndex < filtered.size()) {
            return filtered.get(this.selectedItemIndex);
        }
        return null;
    }

    private List<ShopConfig.ShopItemConfig> getFilteredItems() {
        if (this.itemSearchFilter == null || this.itemSearchFilter.trim().isEmpty()) {
            return this.currentItems;
        }
        String query = this.itemSearchFilter.trim().toLowerCase();
        ArrayList<ShopConfig.ShopItemConfig> result = new ArrayList<ShopConfig.ShopItemConfig>();
        for (ShopConfig.ShopItemConfig item : this.currentItems) {
            String cat;
            String id = item.itemId != null ? item.itemId.toLowerCase() : "";
            String name = item.name != null ? item.name.toLowerCase() : "";
            String string = cat = item.category != null ? item.category.toLowerCase() : "";
            if (!id.contains(query) && !name.contains(query) && !cat.contains(query)) continue;
            result.add(item);
        }
        return result;
    }

    private static List<String> parseCommaList(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        ArrayList<String> result = new ArrayList<String>();
        for (String s : input.split(",")) {
            String trimmed = s.trim();
            if (trimmed.isEmpty()) continue;
            result.add(trimmed);
        }
        return result.isEmpty() ? null : result;
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

    public static class PageEventData {
        String button;
        String editId;
        String editTitle;
        String editCurrency;
        String dailySize;
        String perPage;
        String sellPrice;
        String sellCurr;
        String sellWhitelist;
        String sellBlacklist;
        String itemSearch;
        String itemEdId;
        String itemEdName;
        String itemEdDesc;
        String itemEdQty;
        String itemEdCost;
        String itemEdSellPrice;
        String itemEdWeight;
        String itemEdCategory;
        String itemEdMaxPurch;
        String itemEdMaxSells;
        String itemEdPerm;
        String itemEdCommand;
        public static final BuilderCodec<PageEventData> CODEC = ((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)BuilderCodec.builder(PageEventData.class, PageEventData::new).addField(new KeyedCodec("Button", (Codec)Codec.STRING), (d, v) -> {
            d.button = v;
        }, d -> d.button)).addField(new KeyedCodec("@EditId", (Codec)Codec.STRING), (d, v) -> {
            d.editId = v;
        }, d -> d.editId)).addField(new KeyedCodec("@EditTitle", (Codec)Codec.STRING), (d, v) -> {
            d.editTitle = v;
        }, d -> d.editTitle)).addField(new KeyedCodec("@EditCurrency", (Codec)Codec.STRING), (d, v) -> {
            d.editCurrency = v;
        }, d -> d.editCurrency)).addField(new KeyedCodec("@DailySize", (Codec)Codec.STRING), (d, v) -> {
            d.dailySize = v;
        }, d -> d.dailySize)).addField(new KeyedCodec("@PerPage", (Codec)Codec.STRING), (d, v) -> {
            d.perPage = v;
        }, d -> d.perPage)).addField(new KeyedCodec("@SellPrice", (Codec)Codec.STRING), (d, v) -> {
            d.sellPrice = v;
        }, d -> d.sellPrice)).addField(new KeyedCodec("@SellCurr", (Codec)Codec.STRING), (d, v) -> {
            d.sellCurr = v;
        }, d -> d.sellCurr)).addField(new KeyedCodec("@SellWhitelist", (Codec)Codec.STRING), (d, v) -> {
            d.sellWhitelist = v;
        }, d -> d.sellWhitelist)).addField(new KeyedCodec("@SellBlacklist", (Codec)Codec.STRING), (d, v) -> {
            d.sellBlacklist = v;
        }, d -> d.sellBlacklist)).addField(new KeyedCodec("@ItemSearch", (Codec)Codec.STRING), (d, v) -> {
            d.itemSearch = v;
        }, d -> d.itemSearch)).addField(new KeyedCodec("@ItemEdId", (Codec)Codec.STRING), (d, v) -> {
            d.itemEdId = v;
        }, d -> d.itemEdId)).addField(new KeyedCodec("@ItemEdName", (Codec)Codec.STRING), (d, v) -> {
            d.itemEdName = v;
        }, d -> d.itemEdName)).addField(new KeyedCodec("@ItemEdDesc", (Codec)Codec.STRING), (d, v) -> {
            d.itemEdDesc = v;
        }, d -> d.itemEdDesc)).addField(new KeyedCodec("@ItemEdQty", (Codec)Codec.STRING), (d, v) -> {
            d.itemEdQty = v;
        }, d -> d.itemEdQty)).addField(new KeyedCodec("@ItemEdCost", (Codec)Codec.STRING), (d, v) -> {
            d.itemEdCost = v;
        }, d -> d.itemEdCost)).addField(new KeyedCodec("@ItemEdSellPrice", (Codec)Codec.STRING), (d, v) -> {
            d.itemEdSellPrice = v;
        }, d -> d.itemEdSellPrice)).addField(new KeyedCodec("@ItemEdWeight", (Codec)Codec.STRING), (d, v) -> {
            d.itemEdWeight = v;
        }, d -> d.itemEdWeight)).addField(new KeyedCodec("@ItemEdCategory", (Codec)Codec.STRING), (d, v) -> {
            d.itemEdCategory = v;
        }, d -> d.itemEdCategory)).addField(new KeyedCodec("@ItemEdMaxPurch", (Codec)Codec.STRING), (d, v) -> {
            d.itemEdMaxPurch = v;
        }, d -> d.itemEdMaxPurch)).addField(new KeyedCodec("@ItemEdMaxSells", (Codec)Codec.STRING), (d, v) -> {
            d.itemEdMaxSells = v;
        }, d -> d.itemEdMaxSells)).addField(new KeyedCodec("@ItemEdPerm", (Codec)Codec.STRING), (d, v) -> {
            d.itemEdPerm = v;
        }, d -> d.itemEdPerm)).addField(new KeyedCodec("@ItemEdCommand", (Codec)Codec.STRING), (d, v) -> {
            d.itemEdCommand = v;
        }, d -> d.itemEdCommand)).build();
    }
}

