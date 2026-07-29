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
package com.kyuubisoft.core.shop;

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
import com.kyuubisoft.core.citizen.CitizenService;
import com.kyuubisoft.core.i18n.CoreI18n;
import com.kyuubisoft.core.i18n.I18nContext;
import com.kyuubisoft.core.shop.CurrencyProvider;
import com.kyuubisoft.core.shop.PurchaseResult;
import com.kyuubisoft.core.shop.SellResult;
import com.kyuubisoft.core.shop.SellableItem;
import com.kyuubisoft.core.shop.ShopConfig;
import com.kyuubisoft.core.shop.ShopPlayerData;
import com.kyuubisoft.core.shop.ShopService;
import com.kyuubisoft.core.util.ItemUtils;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class ShopPage
extends InteractiveCustomUIPage<ShopEventData> {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Shop");
    private static final int ITEMS_PER_PAGE = 9;
    private final ShopService shopService;
    private final ShopConfig config;
    private final CurrencyProvider currency;
    private final Player player;
    private final PlayerRef playerRef;
    private final List<ShopConfig.ShopItemConfig> displayItems;
    private final ShopPlayerData playerData;
    private final String citizenId;
    private Mode mode;
    private int currentPage = 0;
    private List<SellableItem> sellableItems;
    private boolean confirmActive = false;
    private int confirmSlotIndex = -1;
    private int confirmQuantity = 1;

    public ShopPage(ShopService shopService, ShopConfig config, CurrencyProvider currency, Player player, PlayerRef playerRef, List<ShopConfig.ShopItemConfig> displayItems, ShopPlayerData playerData, String citizenId) {
        this(shopService, config, currency, player, playerRef, displayItems, playerData, citizenId, Mode.BUY);
    }

    public ShopPage(ShopService shopService, ShopConfig config, CurrencyProvider currency, Player player, PlayerRef playerRef, List<ShopConfig.ShopItemConfig> displayItems, ShopPlayerData playerData, String citizenId, Mode initialMode) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, ShopEventData.CODEC);
        this.shopService = shopService;
        this.config = config;
        this.currency = currency;
        this.player = player;
        this.playerRef = playerRef;
        this.displayItems = displayItems;
        this.playerData = playerData;
        this.citizenId = citizenId;
        this.mode = !config.buyEnabled && config.sellEnabled ? Mode.SELL : initialMode;
    }

    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder ui, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        I18nContext.run(this.playerRef, () -> {
            ui.append("Pages/Shop/ShopPage.ui");
            this.bindAllEvents(events);
            if (this.mode == Mode.SELL) {
                this.sellableItems = this.shopService.getSellableItems(this.player, this.config);
            }
            this.buildUI(ui);
        });
    }

    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull ShopEventData data) {
        I18nContext.run(this.playerRef, () -> {
            super.handleDataEvent(ref, store, (Object)data);
            if (data.tab != null) {
                this.handleTab(data.tab);
                return;
            }
            if (data.confirm != null) {
                this.handleConfirm(data.confirm);
                return;
            }
            if (data.button != null) {
                this.handleButton(data.button);
                return;
            }
            if (data.buy != null) {
                if (this.mode == Mode.BUY) {
                    this.handleBuy(data.buy);
                } else {
                    this.handleSell(data.buy);
                }
                return;
            }
            this.sendUpdate(new UICommandBuilder(), false);
        });
    }

    private void handleTab(String tab) {
        Mode newMode;
        Mode mode = newMode = "sell".equals(tab) ? Mode.SELL : Mode.BUY;
        if (newMode == this.mode) {
            this.sendUpdate(new UICommandBuilder(), false);
            return;
        }
        this.mode = newMode;
        this.currentPage = 0;
        this.confirmActive = false;
        if (this.mode == Mode.SELL) {
            this.sellableItems = this.shopService.getSellableItems(this.player, this.config);
        }
        this.refreshUI();
    }

    private void handleButton(String button) {
        switch (button) {
            case "prev_page": {
                if (this.currentPage > 0) {
                    --this.currentPage;
                    this.refreshUI();
                    break;
                }
                this.sendUpdate(new UICommandBuilder(), false);
                break;
            }
            case "next_page": {
                int maxPage = this.getMaxPage();
                if (this.currentPage < maxPage) {
                    ++this.currentPage;
                    this.refreshUI();
                    break;
                }
                this.sendUpdate(new UICommandBuilder(), false);
                break;
            }
            default: {
                this.sendUpdate(new UICommandBuilder(), false);
            }
        }
    }

    private void handleBuy(String slotStr) {
        try {
            ShopConfig.ShopItemConfig item;
            PurchaseResult result;
            int slotIndex = Integer.parseInt(slotStr);
            int actualIndex = this.currentPage * 9 + slotIndex;
            if (actualIndex >= 0 && actualIndex < this.displayItems.size() && (result = this.shopService.purchase(this.player, this.config, item = this.displayItems.get(actualIndex))) != PurchaseResult.SUCCESS) {
                String errorKey;
                LOGGER.fine("Purchase failed for " + this.player.getDisplayName() + ": " + String.valueOf((Object)result));
                switch (result) {
                    case NO_PERMISSION: {
                        String string = "shop.error.no_permission";
                        break;
                    }
                    case INSUFFICIENT_FUNDS: {
                        String string = "shop.not_enough";
                        break;
                    }
                    case PURCHASE_LIMIT_REACHED: {
                        String string = "shop.error.limit_reached";
                        break;
                    }
                    default: {
                        String string = errorKey = null;
                    }
                }
                if (errorKey != null) {
                    String msg = CoreI18n.getInstance().get(errorKey);
                    this.player.sendMessage(Message.raw((String)msg).color("#FF5555"));
                }
            }
        }
        catch (NumberFormatException e) {
            LOGGER.warning("Invalid buy slot: " + slotStr);
        }
        this.refreshUI();
    }

    private void handleSell(String slotStr) {
        try {
            int slotIndex = Integer.parseInt(slotStr);
            int actualIndex = this.currentPage * 9 + slotIndex;
            if (this.sellableItems != null && actualIndex >= 0 && actualIndex < this.sellableItems.size()) {
                this.confirmActive = true;
                this.confirmSlotIndex = actualIndex;
                this.confirmQuantity = 1;
                this.refreshUI();
                return;
            }
        }
        catch (NumberFormatException e) {
            LOGGER.warning("Invalid sell slot: " + slotStr);
        }
        this.sendUpdate(new UICommandBuilder(), false);
    }

    private void handleConfirm(String action) {
        switch (action) {
            case "yes": {
                if (this.confirmActive && this.confirmSlotIndex >= 0 && this.sellableItems != null && this.confirmSlotIndex < this.sellableItems.size()) {
                    SellableItem item = this.sellableItems.get(this.confirmSlotIndex);
                    SellResult result = this.shopService.sell(this.player, this.config, item.itemId, this.confirmQuantity);
                    if (result != SellResult.SUCCESS) {
                        LOGGER.fine("Sell failed for " + this.player.getDisplayName() + ": " + String.valueOf((Object)result));
                    }
                    this.sellableItems = this.shopService.getSellableItems(this.player, this.config);
                }
                this.confirmActive = false;
                this.confirmSlotIndex = -1;
                this.refreshUI();
                break;
            }
            case "no": {
                this.confirmActive = false;
                this.confirmSlotIndex = -1;
                this.refreshUI();
                break;
            }
            case "plus": {
                int max;
                if (this.confirmActive && this.confirmSlotIndex >= 0 && this.confirmQuantity < (max = this.getConfirmMaxQuantity())) {
                    ++this.confirmQuantity;
                }
                this.refreshUI();
                break;
            }
            case "minus": {
                if (this.confirmActive && this.confirmQuantity > 1) {
                    --this.confirmQuantity;
                }
                this.refreshUI();
                break;
            }
            default: {
                this.sendUpdate(new UICommandBuilder(), false);
            }
        }
    }

    private int getConfirmMaxQuantity() {
        int sold;
        int remaining;
        if (this.sellableItems == null || this.confirmSlotIndex < 0 || this.confirmSlotIndex >= this.sellableItems.size()) {
            return 1;
        }
        SellableItem item = this.sellableItems.get(this.confirmSlotIndex);
        int max = item.quantity;
        int maxSells = this.shopService.getMaxSells(this.config, item.itemId);
        if (maxSells > 0 && (remaining = maxSells - (sold = this.playerData.getShopState(this.config.id).getSellCount(item.itemId))) < max) {
            max = remaining;
        }
        return Math.max(1, max);
    }

    private void refreshUI() {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        this.bindAllEvents(events);
        this.buildUI(ui);
        this.sendUpdate(ui, events, false);
    }

    private void bindAllEvents(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#PrevButton", EventData.of((String)"Button", (String)"prev_page"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NextButton", EventData.of((String)"Button", (String)"next_page"), false);
        for (int i = 0; i < 9; ++i) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#CardGrid #Card" + i + " #CardBtn", EventData.of((String)"Buy", (String)String.valueOf(i)), false);
        }
        if (this.config.sellEnabled) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#TabBar #BuyTab", EventData.of((String)"Tab", (String)"buy"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#TabBar #SellTab", EventData.of((String)"Tab", (String)"sell"), false);
        }
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmYes", EventData.of((String)"Confirm", (String)"yes"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmNo", EventData.of((String)"Confirm", (String)"no"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmPlus", EventData.of((String)"Confirm", (String)"plus"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmMinus", EventData.of((String)"Confirm", (String)"minus"), false);
    }

    private void buildUI(UICommandBuilder ui) {
        boolean sellOnly;
        CurrencyProvider sellCurr;
        CoreI18n i18n = CoreI18n.getInstance();
        CurrencyProvider activeCurrency = this.currency;
        if (this.mode == Mode.SELL && this.config.sellCurrencyId != null && (sellCurr = this.shopService.getCurrency(this.config.sellCurrencyId)) != null) {
            activeCurrency = sellCurr;
        }
        int balance = activeCurrency.getBalance(this.player);
        String title = this.config.title != null ? i18n.get(this.config.title) : this.config.id;
        ui.set("#Title #ShopTitle.Text", title);
        String iconItemId = activeCurrency.getIconItemId();
        if (iconItemId != null) {
            ui.set("#Title #CurrencyDisplay #CurrencyIcon.ItemId", iconItemId);
        }
        ui.set("#Title #CurrencyDisplay #CurrencyBalance.Text", String.valueOf(balance));
        boolean showBoth = this.config.buyEnabled && this.config.sellEnabled;
        boolean bl = sellOnly = !this.config.buyEnabled && this.config.sellEnabled;
        if (showBoth) {
            ui.set("#TabBar.Visible", true);
            ui.set("#TabBar #BuyTab.Visible", true);
            ui.set("#TabBar #SellTab.Visible", true);
            String buyLabel = i18n.get("shop.tab.buy");
            String sellLabel = i18n.get("shop.tab.sell");
            if (this.mode == Mode.BUY) {
                ui.set("#TabBar #BuyTab.Text", "[ " + buyLabel + " ]");
                ui.set("#TabBar #SellTab.Text", sellLabel);
            } else {
                ui.set("#TabBar #BuyTab.Text", buyLabel);
                ui.set("#TabBar #SellTab.Text", "[ " + sellLabel + " ]");
            }
        } else {
            ui.set("#TabBar.Visible", false);
        }
        if (this.mode == Mode.BUY) {
            this.buildBuyUI(ui, i18n, balance, activeCurrency);
        } else {
            this.buildSellUI(ui, i18n, activeCurrency);
        }
        ui.set("#ConfirmOverlay.Visible", this.confirmActive);
        if (this.confirmActive && this.sellableItems != null && this.confirmSlotIndex >= 0 && this.confirmSlotIndex < this.sellableItems.size()) {
            SellableItem item = this.sellableItems.get(this.confirmSlotIndex);
            String itemName = ItemUtils.formatItemName(item.itemId);
            int maxQty = this.getConfirmMaxQuantity();
            if (this.confirmQuantity > maxQty) {
                this.confirmQuantity = maxQty;
            }
            if (this.confirmQuantity < 1) {
                this.confirmQuantity = 1;
            }
            int totalPrice = item.sellPrice * this.confirmQuantity;
            ui.set("#ConfirmTitle.Text", i18n.get("shop.sell.confirm.title"));
            ui.set("#ConfirmIcon.ItemId", item.itemId);
            ui.set("#ConfirmItemName.Text", itemName);
            ui.set("#ConfirmPrice.Text", i18n.get("shop.sell.price", item.sellPrice, activeCurrency.getDisplayName()));
            ui.set("#ConfirmQty.Text", String.valueOf(this.confirmQuantity));
            ui.set("#ConfirmTotal.Text", i18n.get("shop.sell.confirm.total", totalPrice, activeCurrency.getDisplayName()));
            ui.set("#ConfirmMinus.Visible", this.confirmQuantity > 1);
            ui.set("#ConfirmPlus.Visible", this.confirmQuantity < maxQty);
            ui.set("#ConfirmYes.Text", i18n.get("shop.sell.confirm.yes"));
            ui.set("#ConfirmNo.Text", i18n.get("shop.sell.confirm.no"));
        }
    }

    private void buildBuyUI(UICommandBuilder ui, CoreI18n i18n, int balance, CurrencyProvider activeCurrency) {
        int totalPages = Math.max(1, (int)Math.ceil((double)this.displayItems.size() / 9.0));
        ShopPlayerData.ShopState state = this.playerData.getShopState(this.config.id);
        String desc = this.config.settings.dailyShopSize > 0 ? i18n.get("shop.desc.daily") : i18n.get("shop.desc.catalog");
        ui.set("#Header #ShopDescription.Text", desc);
        if (this.config.settings.dailyShopSize > 0) {
            ui.set("#Header #RefreshInfo.Text", i18n.get("shop.refresh_daily"));
        } else {
            ui.set("#Header #RefreshInfo.Text", "");
        }
        String iconItemId = activeCurrency.getIconItemId();
        int startIndex = this.currentPage * 9;
        for (int i = 0; i < 9; ++i) {
            String prefix = "#CardGrid #Card" + i;
            int actualIndex = startIndex + i;
            if (actualIndex < this.displayItems.size()) {
                boolean canBuy;
                ShopConfig.ShopItemConfig item = this.displayItems.get(actualIndex);
                boolean canAfford = balance >= item.cost;
                int purchased = 0;
                boolean limitReached = false;
                if (item.maxPurchases > 0) {
                    purchased = state.getPurchaseCount(item.itemId);
                    limitReached = purchased >= item.maxPurchases;
                }
                ui.set(prefix + ".Visible", true);
                ui.set(prefix + " #ItemIcon.ItemId", item.itemId);
                if (item.quantity > 1) {
                    ui.set(prefix + " #Quantity.Text", item.quantity + "x");
                } else {
                    ui.set(prefix + " #Quantity.Text", "");
                }
                String name = item.name != null ? i18n.get(item.name) : ItemUtils.formatItemName(item.itemId);
                ui.set(prefix + " #Name.Text", name);
                ui.set(prefix + " #Cost.Text", String.valueOf(item.cost));
                if (iconItemId != null) {
                    ui.set(prefix + " #CostIcon.ItemId", iconItemId);
                }
                if (item.maxPurchases > 0) {
                    int remaining = item.maxPurchases - purchased;
                    ui.set(prefix + " #LimitInfo.Text", remaining + "/" + item.maxPurchases + " " + i18n.get("shop.remaining"));
                } else {
                    ui.set(prefix + " #LimitInfo.Text", "");
                }
                boolean bl = canBuy = canAfford && !limitReached;
                if (limitReached) {
                    ui.set(prefix + " #Overlay.Visible", true);
                    ui.set(prefix + " #OverlayText.Text", i18n.get("shop.sold_out"));
                    continue;
                }
                if (!canAfford) {
                    ui.set(prefix + " #Overlay.Visible", true);
                    ui.set(prefix + " #OverlayText.Text", i18n.get("shop.not_enough"));
                    continue;
                }
                ui.set(prefix + " #Overlay.Visible", false);
                continue;
            }
            ui.set(prefix + ".Visible", false);
        }
        ui.set("#Footer #PageInfo.Text", i18n.get("shop.page", this.currentPage + 1, totalPages));
        ui.set("#Footer #PrevButton.Visible", this.currentPage > 0);
        ui.set("#Footer #NextButton.Visible", this.currentPage < totalPages - 1);
    }

    private void buildSellUI(UICommandBuilder ui, CoreI18n i18n, CurrencyProvider activeCurrency) {
        CurrencyProvider sellCurr;
        if (this.sellableItems == null) {
            this.sellableItems = this.shopService.getSellableItems(this.player, this.config);
        }
        int totalPages = Math.max(1, (int)Math.ceil((double)this.sellableItems.size() / 9.0));
        ui.set("#Header #ShopDescription.Text", i18n.get("shop.sell.desc"));
        ui.set("#Header #RefreshInfo.Text", "");
        if (this.sellableItems.isEmpty()) {
            ui.set("#Header #RefreshInfo.Text", i18n.get("shop.sell.no_items"));
        }
        String iconItemId = activeCurrency.getIconItemId();
        if (this.mode == Mode.SELL && this.config.sellCurrencyId != null && (sellCurr = this.shopService.getCurrency(this.config.sellCurrencyId)) != null && sellCurr.getIconItemId() != null) {
            iconItemId = sellCurr.getIconItemId();
        }
        int startIndex = this.currentPage * 9;
        for (int i = 0; i < 9; ++i) {
            String prefix = "#CardGrid #Card" + i;
            int actualIndex = startIndex + i;
            if (actualIndex < this.sellableItems.size()) {
                boolean canSell;
                SellableItem item = this.sellableItems.get(actualIndex);
                String itemName = ItemUtils.formatItemName(item.itemId);
                boolean hasItem = item.quantity > 0;
                int maxSells = this.shopService.getMaxSells(this.config, item.itemId);
                int sold = this.playerData.getShopState(this.config.id).getSellCount(item.itemId);
                boolean limitReached = maxSells > 0 && sold >= maxSells;
                ui.set(prefix + ".Visible", true);
                ui.set(prefix + " #ItemIcon.ItemId", item.itemId);
                ui.set(prefix + " #Name.Text", itemName);
                if (hasItem) {
                    ui.set(prefix + " #Quantity.Text", item.quantity + "x");
                } else {
                    ui.set(prefix + " #Quantity.Text", "");
                }
                ui.set(prefix + " #Cost.Text", String.valueOf(item.sellPrice));
                if (iconItemId != null) {
                    ui.set(prefix + " #CostIcon.ItemId", iconItemId);
                }
                if (maxSells > 0) {
                    int remaining = Math.max(0, maxSells - sold);
                    ui.set(prefix + " #LimitInfo.Text", remaining + "/" + maxSells + " " + i18n.get("shop.remaining"));
                } else {
                    ui.set(prefix + " #LimitInfo.Text", hasItem ? i18n.get("shop.sell.in_inventory") : "");
                }
                boolean bl = canSell = hasItem && !limitReached;
                if (limitReached) {
                    ui.set(prefix + " #Overlay.Visible", true);
                    ui.set(prefix + " #OverlayText.Text", i18n.get("shop.sell.error.limit"));
                    continue;
                }
                if (!hasItem) {
                    ui.set(prefix + " #Overlay.Visible", true);
                    ui.set(prefix + " #OverlayText.Text", i18n.get("shop.sell.not_in_inventory"));
                    continue;
                }
                ui.set(prefix + " #Overlay.Visible", false);
                continue;
            }
            ui.set(prefix + ".Visible", false);
        }
        ui.set("#Footer #PageInfo.Text", i18n.get("shop.page", this.currentPage + 1, totalPages));
        ui.set("#Footer #PrevButton.Visible", this.currentPage > 0);
        ui.set("#Footer #NextButton.Visible", this.currentPage < totalPages - 1);
    }

    private int getMaxPage() {
        if (this.mode == Mode.SELL && this.sellableItems != null) {
            return Math.max(0, (int)Math.ceil((double)this.sellableItems.size() / 9.0) - 1);
        }
        return Math.max(0, (int)Math.ceil((double)this.displayItems.size() / 9.0) - 1);
    }

    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        try {
            super.onDismiss(ref, store);
        }
        catch (Exception e) {
            LOGGER.warning("super.onDismiss error: " + e.getMessage());
        }
        this.resumeCitizenMovement();
    }

    private void resumeCitizenMovement() {
        if (this.citizenId == null) {
            return;
        }
        try {
            CitizenService citizenService = CitizenService.getInstance();
            if (citizenService != null) {
                LOGGER.info("[Shop] resumeCitizenMovement: citizenId=" + this.citizenId);
                citizenService.resumeMovement(this.citizenId);
            }
        }
        catch (Exception e) {
            LOGGER.warning("[Shop] Failed to resume citizen movement for " + this.citizenId + ": " + e.getMessage());
        }
    }

    public static enum Mode {
        BUY,
        SELL;

    }

    public static class ShopEventData {
        public static final BuilderCodec<ShopEventData> CODEC = ((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)BuilderCodec.builder(ShopEventData.class, ShopEventData::new).addField(new KeyedCodec("Button", (Codec)Codec.STRING), (data, value) -> {
            data.button = value;
        }, data -> data.button)).addField(new KeyedCodec("Buy", (Codec)Codec.STRING), (data, value) -> {
            data.buy = value;
        }, data -> data.buy)).addField(new KeyedCodec("Tab", (Codec)Codec.STRING), (data, value) -> {
            data.tab = value;
        }, data -> data.tab)).addField(new KeyedCodec("Confirm", (Codec)Codec.STRING), (data, value) -> {
            data.confirm = value;
        }, data -> data.confirm)).build();
        private String button;
        private String buy;
        private String tab;
        private String confirm;
    }
}

