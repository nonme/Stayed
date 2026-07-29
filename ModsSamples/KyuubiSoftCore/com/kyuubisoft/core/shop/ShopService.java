/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.kyuubisoft.core.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.api.CoreAPI;
import com.kyuubisoft.core.i18n.CoreI18n;
import com.kyuubisoft.core.shop.CurrencyProvider;
import com.kyuubisoft.core.shop.ItemCurrencyProvider;
import com.kyuubisoft.core.shop.PurchaseResult;
import com.kyuubisoft.core.shop.SellResult;
import com.kyuubisoft.core.shop.SellableItem;
import com.kyuubisoft.core.shop.ShopConfig;
import com.kyuubisoft.core.shop.ShopPage;
import com.kyuubisoft.core.shop.ShopPlayerData;
import com.kyuubisoft.core.storage.JsonFilePlayerDataStorage;
import com.kyuubisoft.core.storage.PlayerDataStorage;
import com.kyuubisoft.core.util.CommandUtils;
import com.kyuubisoft.core.util.ItemUtils;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ShopService {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft ShopService");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ShopService instance;
    private static final String MYSQL_TABLE = "shop_players";
    private static final String FILE_DIR = "shop_data";
    private final Map<String, ShopConfig> shops = new ConcurrentHashMap<String, ShopConfig>();
    private final Map<String, CurrencyProvider> currencies = new ConcurrentHashMap<String, CurrencyProvider>();
    private final Map<UUID, ShopPlayerData> playerData = new ConcurrentHashMap<UUID, ShopPlayerData>();
    private Path shopDataFolder;
    private PlayerDataStorage storage;
    private String tableName = "shop_data";

    public ShopService() {
        instance = this;
    }

    public static ShopService getInstance() {
        return instance;
    }

    public void setDataFolder(Path dataFolder) {
        this.shopDataFolder = dataFolder.resolve(FILE_DIR);
        try {
            if (!Files.exists(this.shopDataFolder, new LinkOption[0])) {
                Files.createDirectories(this.shopDataFolder, new FileAttribute[0]);
            }
        }
        catch (Exception e) {
            LOGGER.warning("Failed to create shop_data directory: " + e.getMessage());
        }
    }

    public void initStorage(PlayerDataStorage mysqlStorage) {
        if (mysqlStorage != null) {
            this.storage = mysqlStorage;
            this.tableName = MYSQL_TABLE;
            this.storage.ensureTableExists(this.tableName);
            LOGGER.info("ShopService using MySQL storage");
        } else if (this.shopDataFolder != null) {
            this.storage = new JsonFilePlayerDataStorage(this.shopDataFolder.getParent());
            this.tableName = FILE_DIR;
            this.storage.ensureTableExists(this.tableName);
            LOGGER.info("ShopService using file storage: " + String.valueOf(this.shopDataFolder));
        }
    }

    public void registerCurrency(CurrencyProvider provider) {
        this.currencies.put(provider.getCurrencyId(), provider);
        if (CoreAPI.isDebug()) {
            LOGGER.info("Currency registered: " + provider.getCurrencyId() + " (" + provider.getDisplayName() + ")");
        }
    }

    public CurrencyProvider getCurrency(String currencyId) {
        CurrencyProvider provider = this.currencies.get(currencyId);
        if (provider != null) {
            return provider;
        }
        if (currencyId != null && currencyId.startsWith("item:")) {
            ItemCurrencyProvider itemProvider = ItemCurrencyProvider.fromCurrencyId(currencyId);
            this.currencies.put(currencyId, itemProvider);
            if (CoreAPI.isDebug()) {
                LOGGER.info("Auto-created ItemCurrencyProvider: " + currencyId);
            }
            return itemProvider;
        }
        return null;
    }

    public void loadShop(String id, Path configFile) {
        try {
            String source;
            String content;
            if (Files.exists(configFile, new LinkOption[0])) {
                content = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
                source = "file";
            } else {
                try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("defaults/shops/" + id + ".json");){
                    if (is == null) {
                        LOGGER.warning("Shop config not found: " + String.valueOf(configFile) + " (no JAR fallback)");
                        return;
                    }
                    content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    source = "JAR fallback";
                }
            }
            ShopConfig config = GSON.fromJson(content, ShopConfig.class);
            if (config != null) {
                config.id = id;
                this.shops.put(id, config);
                LOGGER.info("Loaded shop: " + id + " (" + config.items.size() + " items, dailyShopSize=" + config.settings.dailyShopSize + ", source=" + source + ")");
            }
        }
        catch (Exception e) {
            LOGGER.warning("Failed to load shop " + id + ": " + e.getMessage());
        }
    }

    public void loadShopWithCustom(String id, Path configFile, Path dataFolder) {
        this.loadShop(id, configFile);
        try {
            Path customFile;
            Path customDir = dataFolder.resolve("custom");
            Files.createDirectories(customDir, new FileAttribute[0]);
            Path exampleFile = customDir.resolve("custom_" + id + ".json.example");
            if (!Files.exists(exampleFile, new LinkOption[0])) {
                LinkedHashMap<String, Object> example = new LinkedHashMap<String, Object>();
                example.put("_comment", "Custom shop items override base ones by itemId. New items are appended. This file is NEVER overwritten on updates.");
                example.put("items", List.of());
                try (BufferedWriter w = Files.newBufferedWriter(exampleFile, StandardCharsets.UTF_8, new OpenOption[0]);){
                    GSON.toJson(example, (Appendable)w);
                }
            }
            if (!Files.exists(customFile = customDir.resolve("custom_" + id + ".json"), new LinkOption[0])) {
                return;
            }
            ShopConfig baseShop = this.shops.get(id);
            if (baseShop == null) {
                return;
            }
            String content = new String(Files.readAllBytes(customFile), StandardCharsets.UTF_8);
            ShopConfig customConfig = GSON.fromJson(content, ShopConfig.class);
            if (customConfig != null) {
                int before = baseShop.items.size();
                baseShop.mergeItems(customConfig);
                int after = baseShop.items.size();
                if (CoreAPI.isDebug()) {
                    LOGGER.info("Merged custom shop for " + id + ": " + before + " -> " + after + " items");
                }
            }
        }
        catch (Exception e) {
            LOGGER.warning("Failed to load custom shop " + id + ": " + e.getMessage());
        }
    }

    public void extractDefaults(Path shopsDir) {
        try {
            String[] defaultShops;
            if (!Files.exists(shopsDir, new LinkOption[0])) {
                Files.createDirectories(shopsDir, new FileAttribute[0]);
            }
            for (String shopFile : defaultShops = new String[]{"example_shop.json", "general_store.json", "blacksmith.json", "alchemist.json", "daily_deals.json", "rare_collector.json"}) {
                Path target = shopsDir.resolve(shopFile);
                if (Files.exists(target, new LinkOption[0])) continue;
                try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("defaults/shops/" + shopFile);){
                    if (is == null) continue;
                    Files.copy(is, target, new CopyOption[0]);
                    if (!CoreAPI.isDebug()) continue;
                    LOGGER.info("Extracted default shop: " + shopFile);
                }
                catch (Exception e) {
                    LOGGER.fine("Failed to extract " + shopFile + ": " + e.getMessage());
                }
            }
        }
        catch (Exception e) {
            LOGGER.warning("Failed to extract default shops: " + e.getMessage());
        }
    }

    public void loadShopsFromDirectory(Path shopsDir) {
        if (!Files.exists(shopsDir, new LinkOption[0])) {
            return;
        }
        try {
            Files.list(shopsDir).filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                String filename = p.getFileName().toString();
                String id = filename.substring(0, filename.length() - 5);
                this.loadShop(id, (Path)p);
            });
        }
        catch (Exception e) {
            LOGGER.warning("Failed to load shops directory: " + e.getMessage());
        }
    }

    public void registerShop(ShopConfig config) {
        if (config.id == null || config.id.isEmpty()) {
            LOGGER.warning("Cannot register shop without ID");
            return;
        }
        this.shops.put(config.id, config);
        if (CoreAPI.isDebug()) {
            LOGGER.info("Shop registered: " + config.id);
        }
    }

    public ShopConfig getShop(String id) {
        return this.shops.get(id);
    }

    public Collection<ShopConfig> getAllShops() {
        return Collections.unmodifiableCollection(this.shops.values());
    }

    public void deleteShop(String shopId) {
        this.shops.remove(shopId);
        if (this.shopDataFolder != null) {
            try {
                Path shopsDir = this.shopDataFolder.getParent().resolve("shops");
                Path file = shopsDir.resolve(shopId + ".json");
                if (Files.exists(file, new LinkOption[0])) {
                    Files.delete(file);
                    if (CoreAPI.isDebug()) {
                        LOGGER.info("Deleted shop file: " + String.valueOf(file));
                    }
                }
            }
            catch (Exception e) {
                LOGGER.warning("Failed to delete shop file for " + shopId + ": " + e.getMessage());
            }
        }
        if (CoreAPI.isDebug()) {
            LOGGER.info("Shop deleted: " + shopId);
        }
    }

    public void saveShopToFile(ShopConfig config) {
        if (config.id == null || config.id.isEmpty()) {
            LOGGER.warning("Cannot save shop without ID");
            return;
        }
        if (this.shopDataFolder == null) {
            LOGGER.warning("Cannot save shop - no data folder set");
            return;
        }
        try {
            Path shopsDir = this.shopDataFolder.getParent().resolve("shops");
            if (!Files.exists(shopsDir, new LinkOption[0])) {
                Files.createDirectories(shopsDir, new FileAttribute[0]);
            }
            Path file = shopsDir.resolve(config.id + ".json");
            String json = GSON.toJson(config);
            Files.writeString(file, (CharSequence)json, StandardCharsets.UTF_8, new OpenOption[0]);
            if (CoreAPI.isDebug()) {
                LOGGER.info("Saved shop config: " + config.id + " (" + config.items.size() + " items)");
            }
            this.shops.put(config.id, config);
        }
        catch (Exception e) {
            LOGGER.warning("Failed to save shop " + config.id + ": " + e.getMessage());
        }
    }

    public boolean isSellable(ShopConfig config, String itemId) {
        if (!config.sellEnabled) {
            return false;
        }
        if (itemId == null) {
            return false;
        }
        for (ShopConfig.ShopItemConfig shopItem : config.items) {
            if (!itemId.equals(shopItem.itemId) || !shopItem.isCommand()) continue;
            return false;
        }
        if (config.sellBlacklist != null && config.sellBlacklist.contains(itemId)) {
            return false;
        }
        if (config.sellWhitelist != null && !config.sellWhitelist.isEmpty()) {
            return config.sellWhitelist.contains(itemId);
        }
        return true;
    }

    public int getSellPrice(ShopConfig config, String itemId) {
        if (!this.isSellable(config, itemId)) {
            return -1;
        }
        for (ShopConfig.ShopItemConfig shopItem : config.items) {
            if (!itemId.equals(shopItem.itemId)) continue;
            if (shopItem.sellPrice <= 0) break;
            return shopItem.sellPrice;
        }
        if (config.sellItems != null) {
            for (ShopConfig.SellItemConfig sellItem : config.sellItems) {
                if (!itemId.equals(sellItem.itemId)) continue;
                return sellItem.sellPrice;
            }
        }
        for (ShopConfig.ShopItemConfig shopItem : config.items) {
            if (!itemId.equals(shopItem.itemId)) continue;
            return Math.max(1, shopItem.cost * config.sellPricePercent / 100);
        }
        return -1;
    }

    public List<SellableItem> getSellableItems(Player player, ShopConfig config) {
        ArrayList<SellableItem> result = new ArrayList<SellableItem>();
        if (!config.sellEnabled) {
            return result;
        }
        LinkedHashSet<String> candidateIds = new LinkedHashSet<String>();
        for (ShopConfig.ShopItemConfig item : config.items) {
            if (item.itemId == null) continue;
            candidateIds.add(item.itemId);
        }
        if (config.sellItems != null) {
            for (ShopConfig.SellItemConfig sellItem : config.sellItems) {
                if (sellItem.itemId == null) continue;
                candidateIds.add(sellItem.itemId);
            }
        }
        if (config.sellWhitelist != null) {
            candidateIds.addAll(config.sellWhitelist);
        }
        for (String itemId : candidateIds) {
            int price = this.getSellPrice(config, itemId);
            if (price <= 0) continue;
            int count = ItemUtils.countItem(player, itemId);
            result.add(new SellableItem(itemId, count, price));
        }
        return result;
    }

    public int getMaxSells(ShopConfig config, String itemId) {
        for (ShopConfig.ShopItemConfig shopItem : config.items) {
            if (!itemId.equals(shopItem.itemId) || shopItem.maxSells <= 0) continue;
            return shopItem.maxSells;
        }
        if (config.sellItems != null) {
            for (ShopConfig.SellItemConfig sellItem : config.sellItems) {
                if (!itemId.equals(sellItem.itemId) || sellItem.maxSellsPerDay <= 0) continue;
                return sellItem.maxSellsPerDay;
            }
        }
        return 0;
    }

    public SellResult sell(Player player, ShopConfig config, String itemId, int quantity) {
        int owned;
        if (!config.sellEnabled) {
            return SellResult.SELL_DISABLED;
        }
        int sellPrice = this.getSellPrice(config, itemId);
        if (sellPrice < 0) {
            return SellResult.ITEM_NOT_SELLABLE;
        }
        String currId = config.sellCurrencyId != null ? config.sellCurrencyId : config.currencyId;
        CurrencyProvider currency = this.getCurrency(currId);
        if (currency == null) {
            return SellResult.CURRENCY_NOT_FOUND;
        }
        UUID playerId = player.getPlayerRef().getUuid();
        ShopPlayerData data = this.getOrLoadPlayerData(playerId);
        ShopPlayerData.ShopState state = data.getShopState(config.id);
        int sold = state.getSellCount(itemId);
        for (ShopConfig.ShopItemConfig shopItem : config.items) {
            if (!itemId.equals(shopItem.itemId) || shopItem.maxSells <= 0) continue;
            if (sold + quantity <= shopItem.maxSells) break;
            return SellResult.SELL_LIMIT_REACHED;
        }
        if (config.sellItems != null) {
            for (ShopConfig.SellItemConfig sellItem : config.sellItems) {
                if (!itemId.equals(sellItem.itemId) || sellItem.maxSellsPerDay <= 0) continue;
                if (sold + quantity <= sellItem.maxSellsPerDay) break;
                return SellResult.SELL_LIMIT_REACHED;
            }
        }
        if ((owned = ItemUtils.countItem(player, itemId)) < quantity) {
            return SellResult.ITEM_NOT_FOUND;
        }
        if (!ItemUtils.removeItem(player, itemId, quantity)) {
            return SellResult.ITEM_NOT_FOUND;
        }
        int totalPrice = sellPrice * quantity;
        currency.refund(player, totalPrice);
        for (int i = 0; i < quantity; ++i) {
            state.recordSell(itemId);
        }
        this.savePlayerData(playerId);
        LOGGER.fine("Sell: " + String.valueOf(playerId) + " sold " + quantity + "x " + itemId + " for " + totalPrice + " " + currId);
        return SellResult.SUCCESS;
    }

    public void openShop(Player player, PlayerRef playerRef, Ref<EntityStore> ref, Store<EntityStore> store, String shopId) {
        this.openShop(player, playerRef, ref, store, shopId, null);
    }

    public void openShop(Player player, PlayerRef playerRef, Ref<EntityStore> ref, Store<EntityStore> store, String shopId, String citizenId) {
        ShopConfig config = this.shops.get(shopId);
        if (config == null) {
            LOGGER.warning("Shop not found: " + shopId);
            return;
        }
        if (config.permission != null && !config.permission.isEmpty() && !player.hasPermission(config.permission)) {
            player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("shop.error.no_access")).color("#FF5555"));
            return;
        }
        CurrencyProvider currency = this.getCurrency(config.currencyId);
        if (currency == null) {
            LOGGER.warning("Currency not found for shop " + shopId + ": " + config.currencyId);
            player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("shop.error.currency_not_found")).color("#FF5555"));
            return;
        }
        UUID playerId = playerRef.getUuid();
        ShopPlayerData data = this.getOrLoadPlayerData(playerId);
        List<ShopConfig.ShopItemConfig> displayItems = this.getDisplayItems(player, config, data);
        ShopPage page = new ShopPage(this, config, currency, player, playerRef, displayItems, data, citizenId);
        player.getPageManager().openCustomPage(ref, store, (CustomUIPage)page);
    }

    public void openSellShop(Player player, PlayerRef playerRef, Ref<EntityStore> ref, Store<EntityStore> store, String shopId, String citizenId) {
        ShopConfig config = this.shops.get(shopId);
        if (config == null) {
            LOGGER.warning("Shop not found: " + shopId);
            return;
        }
        if (!config.sellEnabled) {
            LOGGER.warning("Sell not enabled for shop " + shopId);
            return;
        }
        if (config.permission != null && !config.permission.isEmpty() && !player.hasPermission(config.permission)) {
            player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("shop.error.no_access")).color("#FF5555"));
            return;
        }
        String currId = config.sellCurrencyId != null ? config.sellCurrencyId : config.currencyId;
        CurrencyProvider currency = this.getCurrency(currId);
        if (currency == null) {
            LOGGER.warning("Currency not found for shop " + shopId + ": " + currId);
            player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("shop.error.currency_not_found")).color("#FF5555"));
            return;
        }
        UUID playerId = playerRef.getUuid();
        ShopPlayerData data = this.getOrLoadPlayerData(playerId);
        List<ShopConfig.ShopItemConfig> displayItems = this.getDisplayItems(player, config, data);
        ShopPage page = new ShopPage(this, config, currency, player, playerRef, displayItems, data, citizenId, ShopPage.Mode.SELL);
        player.getPageManager().openCustomPage(ref, store, (CustomUIPage)page);
    }

    public List<ShopConfig.ShopItemConfig> getDisplayItems(Player player, ShopConfig config, ShopPlayerData playerData) {
        ShopPlayerData.ShopState state = playerData.getShopState(config.id);
        UUID playerId = player.getPlayerRef().getUuid();
        if (state.checkDailyPurchaseReset()) {
            LOGGER.info("Shop " + config.id + ": Daily purchase reset for " + String.valueOf(playerId));
            this.savePlayerData(playerId);
        }
        if (config.settings.dailyShopSize <= 0) {
            return config.items;
        }
        if (state.isDailyShopValid()) {
            List<ShopConfig.ShopItemConfig> resolved = this.resolveItemsById(config, state.getDailyItemIds());
            if (resolved.size() >= config.settings.dailyShopSize) {
                return resolved;
            }
            LOGGER.warning("Shop " + config.id + ": Resolved " + resolved.size() + "/" + state.getDailyItemIds().size() + " items, regenerating (config may have changed)");
        }
        List<ShopConfig.ShopItemConfig> dailyItems = this.generateDailyShop(config, playerId);
        ArrayList<String> itemIds = new ArrayList<String>();
        for (ShopConfig.ShopItemConfig item : dailyItems) {
            itemIds.add(item.itemId);
        }
        state.setDailyItems(itemIds);
        this.savePlayerData(playerId);
        LOGGER.info("Shop " + config.id + ": Generated " + dailyItems.size() + " daily items for " + String.valueOf(playerId));
        return dailyItems;
    }

    private List<ShopConfig.ShopItemConfig> generateDailyShop(ShopConfig config, UUID playerId) {
        int totalWeight;
        long seed = (long)playerId.hashCode() + LocalDate.now().toEpochDay();
        Random random = new Random(seed);
        int size = config.settings.dailyShopSize;
        ArrayList<ShopConfig.ShopItemConfig> pool = new ArrayList<ShopConfig.ShopItemConfig>(config.items);
        ArrayList<ShopConfig.ShopItemConfig> result = new ArrayList<ShopConfig.ShopItemConfig>();
        if (pool.isEmpty()) {
            LOGGER.warning("Shop " + config.id + ": Cannot generate daily shop \u2014 item pool is empty");
            return result;
        }
        for (int i = 0; i < size && !pool.isEmpty() && (totalWeight = pool.stream().mapToInt(item -> item.weight).sum()) > 0; ++i) {
            int roll = random.nextInt(totalWeight);
            int cumulative = 0;
            ShopConfig.ShopItemConfig selected = null;
            for (ShopConfig.ShopItemConfig item2 : pool) {
                if (roll >= (cumulative += item2.weight)) continue;
                selected = item2;
                break;
            }
            if (selected == null) continue;
            result.add(selected);
            pool.remove(selected);
        }
        if (CoreAPI.isDebug()) {
            LOGGER.info("Shop " + config.id + ": Generated daily shop \u2014 seed=" + seed + ", pool=" + config.items.size() + ", selected=" + result.size() + "/" + size);
        }
        return result;
    }

    private List<ShopConfig.ShopItemConfig> resolveItemsById(ShopConfig config, List<String> itemIds) {
        ArrayList<ShopConfig.ShopItemConfig> result = new ArrayList<ShopConfig.ShopItemConfig>();
        for (String id : itemIds) {
            boolean found = false;
            for (ShopConfig.ShopItemConfig item : config.items) {
                if (!id.equals(item.itemId)) continue;
                result.add(item);
                found = true;
                break;
            }
            if (found) continue;
            LOGGER.warning("Shop " + config.id + ": Item '" + id + "' not found in config (removed or renamed?)");
        }
        return result;
    }

    public PurchaseResult purchase(Player player, ShopConfig config, ShopConfig.ShopItemConfig item) {
        int purchased;
        CurrencyProvider currency = this.getCurrency(config.currencyId);
        if (currency == null) {
            return PurchaseResult.CURRENCY_NOT_FOUND;
        }
        if (item.permission != null && !item.permission.isEmpty() && !player.hasPermission(item.permission)) {
            return PurchaseResult.NO_PERMISSION;
        }
        UUID playerId = player.getPlayerRef().getUuid();
        ShopPlayerData data = this.getOrLoadPlayerData(playerId);
        ShopPlayerData.ShopState state = data.getShopState(config.id);
        if (item.maxPurchases > 0 && (purchased = state.getPurchaseCount(item.itemId)) >= item.maxPurchases) {
            return PurchaseResult.PURCHASE_LIMIT_REACHED;
        }
        int balance = currency.getBalance(player);
        if (balance < item.cost) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }
        if (!currency.spend(player, item.cost)) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }
        if (item.isCommand()) {
            try {
                String cmd = item.command.trim();
                if (cmd.startsWith("/")) {
                    cmd = cmd.substring(1);
                }
                PlayerRef playerRef = player.getPlayerRef();
                cmd = cmd.replace("{player}", playerRef.getUsername());
                cmd = cmd.replace("{PlayerName}", playerRef.getUsername());
                cmd = cmd.replace("{uuid}", playerRef.getUuid().toString());
                String executor = item.commandRunAsServer ? "server" : "player";
                CommandUtils.executeCommand(player, cmd, executor);
                LOGGER.info("Shop command executed: " + cmd + " (runAsServer=" + item.commandRunAsServer + ")");
            }
            catch (Exception e) {
                LOGGER.warning("Shop command failed: " + item.command + " \u2014 " + e.getMessage());
                currency.refund(player, item.cost);
                return PurchaseResult.ITEM_NOT_FOUND;
            }
        } else {
            boolean itemGiven = ItemUtils.grantItem(player, item.itemId, item.quantity);
            if (!itemGiven) {
                currency.refund(player, item.cost);
                return PurchaseResult.INVENTORY_FULL;
            }
        }
        state.recordPurchase(item.itemId);
        this.savePlayerData(playerId);
        LOGGER.fine("Purchase: " + String.valueOf(playerId) + " bought " + (item.isCommand() ? "command:" + item.command : item.quantity + "x " + item.itemId) + " for " + item.cost + " " + config.currencyId);
        return PurchaseResult.SUCCESS;
    }

    private ShopPlayerData getOrLoadPlayerData(UUID playerId) {
        ShopPlayerData data = this.playerData.get(playerId);
        if (data != null) {
            return data;
        }
        data = this.loadPlayerDataFromStorage(playerId);
        this.playerData.put(playerId, data);
        return data;
    }

    private ShopPlayerData loadPlayerDataFromStorage(UUID playerId) {
        if (this.storage != null) {
            String json = this.storage.loadJson(this.tableName, playerId);
            if (json != null) {
                return ShopPlayerData.fromJson(json);
            }
            return new ShopPlayerData();
        }
        if (this.shopDataFolder == null) {
            return new ShopPlayerData();
        }
        Path file = this.shopDataFolder.resolve(playerId.toString() + ".json");
        if (!Files.exists(file, new LinkOption[0])) {
            return new ShopPlayerData();
        }
        try {
            String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            return ShopPlayerData.fromJson(content);
        }
        catch (Exception e) {
            LOGGER.warning("Failed to load shop data for " + String.valueOf(playerId) + ": " + e.getMessage());
            return new ShopPlayerData();
        }
    }

    private void savePlayerData(UUID playerId) {
        ShopPlayerData data = this.playerData.get(playerId);
        if (data == null || data.isEmpty()) {
            return;
        }
        if (this.storage != null) {
            this.storage.saveJson(this.tableName, playerId, playerId.toString(), data.toJson());
            return;
        }
        if (this.shopDataFolder == null) {
            return;
        }
        try {
            Path file = this.shopDataFolder.resolve(playerId.toString() + ".json");
            Files.writeString(file, (CharSequence)data.toJson(), StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (Exception e) {
            LOGGER.warning("Failed to save shop data for " + String.valueOf(playerId) + ": " + e.getMessage());
        }
    }

    public void clearPlayer(UUID playerId) {
        this.savePlayerData(playerId);
        this.playerData.remove(playerId);
    }
}

