/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.kyuubisoft.core.api;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.CorePlugin;
import com.kyuubisoft.core.citizen.CitizenBankHandler;
import com.kyuubisoft.core.citizen.CitizenData;
import com.kyuubisoft.core.citizen.CitizenDialogInterceptor;
import com.kyuubisoft.core.citizen.CitizenListener;
import com.kyuubisoft.core.citizen.CitizenService;
import com.kyuubisoft.core.data.GameDataProvider;
import com.kyuubisoft.core.dialog.DialogConditionProvider;
import com.kyuubisoft.core.dialog.DialogService;
import com.kyuubisoft.core.dialog.DialogTree;
import com.kyuubisoft.core.economy.ExternalEconomyBridge;
import com.kyuubisoft.core.i18n.CoreI18n;
import com.kyuubisoft.core.i18n.PlayerPreferences;
import com.kyuubisoft.core.i18n.PlayerPreferencesService;
import com.kyuubisoft.core.lootbag.LootbagDefinition;
import com.kyuubisoft.core.lootbag.LootbagService;
import com.kyuubisoft.core.reward.RewardGrantHelper;
import com.kyuubisoft.core.shop.CurrencyProvider;
import com.kyuubisoft.core.shop.ShopConfig;
import com.kyuubisoft.core.shop.ShopService;
import com.kyuubisoft.core.storage.PlayerDataStorage;
import com.kyuubisoft.core.tracking.TrackingListener;
import com.kyuubisoft.core.tracking.TrackingService;
import com.kyuubisoft.core.util.CommandUtils;
import com.kyuubisoft.core.util.ItemUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class CoreAPI {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core API");
    private static CoreAPI instance;
    private final CorePlugin plugin;
    private static volatile boolean showcaseMode;
    private static Set<UUID> showcaseAdminUuids;
    private static final Set<String> showcaseBreakableBlocks;
    private static final Map<String, NpcPageOpener> npcPageOpeners;
    private static final List<LanguageChangeListener> languageChangeListeners;

    public static boolean isShowcaseMode() {
        return showcaseMode;
    }

    public static void setShowcaseMode(boolean enabled) {
        showcaseMode = enabled;
        LOGGER.info("Showcase mode " + (enabled ? "ENABLED" : "DISABLED"));
    }

    public static void setShowcaseAdminUuids(Set<UUID> uuids) {
        showcaseAdminUuids = uuids != null ? Set.copyOf(uuids) : Set.of();
    }

    public static boolean isRealAdmin(PlayerRef playerRef) {
        if (!showcaseMode) {
            return true;
        }
        if (playerRef == null) {
            return false;
        }
        return showcaseAdminUuids.contains(playerRef.getUuid());
    }

    public static boolean isRealAdmin(UUID uuid) {
        if (!showcaseMode) {
            return true;
        }
        if (uuid == null) {
            return false;
        }
        return showcaseAdminUuids.contains(uuid);
    }

    public static boolean isShowcaseBlocked(PlayerRef playerRef) {
        return showcaseMode && !CoreAPI.isRealAdmin(playerRef);
    }

    public static boolean showcaseWriteGuard(Player player, PlayerRef playerRef) {
        if (!CoreAPI.isShowcaseBlocked(playerRef)) {
            return false;
        }
        player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("showcase.readonly")).color("#FF5555"));
        return true;
    }

    public static void registerShowcaseBreakableBlock(String blockId) {
        if (blockId == null) {
            return;
        }
        showcaseBreakableBlocks.add(blockId.toLowerCase(Locale.ROOT));
        LOGGER.info("Showcase breakable block registered: " + blockId);
    }

    public static void removeShowcaseBreakableBlock(String blockId) {
        if (blockId == null) {
            return;
        }
        showcaseBreakableBlocks.remove(blockId.toLowerCase(Locale.ROOT));
    }

    public static boolean isShowcaseBreakable(String blockId) {
        if (blockId == null || showcaseBreakableBlocks.isEmpty()) {
            return false;
        }
        String lower = blockId.toLowerCase(Locale.ROOT);
        if (showcaseBreakableBlocks.contains(lower)) {
            return true;
        }
        for (String allowed : showcaseBreakableBlocks) {
            if (!lower.contains(allowed)) continue;
            return true;
        }
        return false;
    }

    public static Set<String> getShowcaseBreakableBlocks() {
        return Collections.unmodifiableSet(showcaseBreakableBlocks);
    }

    public static void registerNpcPageOpener(String citizenId, NpcPageOpener opener) {
        npcPageOpeners.put(citizenId, opener);
        LOGGER.info("NPC page opener registered: " + citizenId);
    }

    public static void removeNpcPageOpener(String citizenId) {
        npcPageOpeners.remove(citizenId);
    }

    public static NpcPageOpener getNpcPageOpener(String citizenId) {
        return npcPageOpeners.get(citizenId);
    }

    public static Set<String> getNpcPageOpenerIds() {
        return Collections.unmodifiableSet(npcPageOpeners.keySet());
    }

    private CoreAPI(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public static void init(CorePlugin plugin) {
        instance = new CoreAPI(plugin);
        LOGGER.info("CoreAPI initialized");
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    public static CoreAPI getInstance() {
        return instance;
    }

    public void addTrackingListener(TrackingListener l) {
        TrackingService svc = this.plugin.getTrackingService();
        if (svc == null) {
            LOGGER.fine("TrackingService not available \u2014 cannot add listener");
            return;
        }
        svc.addListener(l);
    }

    public void removeTrackingListener(TrackingListener l) {
        TrackingService svc = this.plugin.getTrackingService();
        if (svc == null) {
            LOGGER.fine("TrackingService not available \u2014 cannot remove listener");
            return;
        }
        svc.removeListener(l);
    }

    public CitizenData getCitizen(String id) {
        CitizenService svc = this.plugin.getCitizenService();
        if (svc == null) {
            LOGGER.fine("CitizenService not available");
            return null;
        }
        return svc.getCitizen(id);
    }

    public Collection<CitizenData> getAllCitizens() {
        CitizenService svc = this.plugin.getCitizenService();
        if (svc == null) {
            LOGGER.fine("CitizenService not available");
            return Collections.emptyList();
        }
        Collection<CitizenData> result = svc.getAllCitizens();
        return result != null ? result : Collections.emptyList();
    }

    public List<CitizenData> getCitizensByGroup(String group) {
        CitizenService svc = this.plugin.getCitizenService();
        if (svc == null) {
            LOGGER.fine("CitizenService not available");
            return Collections.emptyList();
        }
        return svc.getCitizensByGroup(group);
    }

    public void addCitizenListener(CitizenListener l) {
        CitizenService svc = this.plugin.getCitizenService();
        if (svc == null) {
            LOGGER.fine("CitizenService not available \u2014 cannot add listener");
            return;
        }
        svc.addListener(l);
    }

    public void removeCitizenListener(CitizenListener l) {
        CitizenService svc = this.plugin.getCitizenService();
        if (svc == null) {
            LOGGER.fine("CitizenService not available \u2014 cannot remove listener");
            return;
        }
        svc.removeListener(l);
    }

    public void addDialogInterceptor(CitizenDialogInterceptor i) {
        CitizenService svc = this.plugin.getCitizenService();
        if (svc == null) {
            LOGGER.fine("CitizenService not available \u2014 cannot add interceptor");
            return;
        }
        svc.addDialogInterceptor(i);
    }

    public void setBankHandler(CitizenBankHandler handler) {
        CitizenService svc = this.plugin.getCitizenService();
        if (svc == null) {
            LOGGER.fine("CitizenService not available \u2014 cannot set bank handler");
            return;
        }
        svc.setBankHandler(handler);
    }

    public CitizenBankHandler getBankHandler() {
        CitizenService svc = this.plugin.getCitizenService();
        if (svc == null) {
            return null;
        }
        return svc.getBankHandler();
    }

    public void removeDialogInterceptor(CitizenDialogInterceptor i) {
        CitizenService svc = this.plugin.getCitizenService();
        if (svc == null) {
            LOGGER.fine("CitizenService not available \u2014 cannot remove interceptor");
            return;
        }
        svc.removeDialogInterceptor(i);
    }

    public void dispatchCitizenInteract(Player p, String citizenId) {
        CitizenService svc = this.plugin.getCitizenService();
        if (svc == null) {
            LOGGER.fine("CitizenService not available \u2014 cannot dispatch interact");
            return;
        }
        svc.dispatchInteract(p, citizenId);
    }

    public void pauseCitizenMovement(CitizenData c, Player p) {
        CitizenService svc = this.plugin.getCitizenService();
        if (svc == null) {
            LOGGER.fine("CitizenService not available \u2014 cannot pause movement");
            return;
        }
        svc.pauseMovement(c, p);
    }

    public void resumeCitizenMovement(String citizenId) {
        CitizenService svc = this.plugin.getCitizenService();
        if (svc == null) {
            LOGGER.fine("CitizenService not available \u2014 cannot resume movement");
            return;
        }
        svc.resumeMovement(citizenId);
    }

    public boolean isCitizenPaused(String citizenId) {
        CitizenService svc = this.plugin.getCitizenService();
        if (svc == null) {
            LOGGER.fine("CitizenService not available");
            return false;
        }
        return svc.isPaused(citizenId);
    }

    public void registerProtectedNpcUuid(UUID uuid) {
        CitizenService svc = this.plugin.getCitizenService();
        if (svc == null) {
            return;
        }
        svc.registerProtectedNpcUuid(uuid);
    }

    public void unregisterProtectedNpcUuid(UUID uuid) {
        CitizenService svc = this.plugin.getCitizenService();
        if (svc == null) {
            return;
        }
        svc.unregisterProtectedNpcUuid(uuid);
    }

    public static void protectNpcUuid(UUID uuid) {
        CoreAPI api = CoreAPI.getInstance();
        if (api != null) {
            api.registerProtectedNpcUuid(uuid);
        }
    }

    public static void unprotectNpcUuid(UUID uuid) {
        CoreAPI api = CoreAPI.getInstance();
        if (api != null) {
            api.unregisterProtectedNpcUuid(uuid);
        }
    }

    public void openDialog(Player p, PlayerRef pr, Ref<EntityStore> ref, Store<EntityStore> store, String dialogId, String citizenId) {
        DialogService svc = this.plugin.getDialogService();
        if (svc == null) {
            LOGGER.fine("DialogService not available \u2014 cannot open dialog");
            return;
        }
        svc.openDialog(p, pr, ref, store, dialogId, citizenId);
    }

    public void openDialogFromTree(Player p, PlayerRef pr, Ref<EntityStore> ref, Store<EntityStore> store, DialogTree tree, String citizenId) {
        DialogService svc = this.plugin.getDialogService();
        if (svc == null) {
            LOGGER.fine("DialogService not available \u2014 cannot open dialog from tree");
            return;
        }
        svc.openDialogFromTree(p, pr, ref, store, tree, citizenId);
    }

    public void addDialogConditionProvider(DialogConditionProvider p) {
        DialogService svc = this.plugin.getDialogService();
        if (svc == null) {
            LOGGER.fine("DialogService not available \u2014 cannot add condition provider");
            return;
        }
        svc.addConditionProvider(p);
    }

    public void removeDialogConditionProvider(DialogConditionProvider p) {
        DialogService svc = this.plugin.getDialogService();
        if (svc == null) {
            LOGGER.fine("DialogService not available \u2014 cannot remove condition provider");
            return;
        }
        svc.removeConditionProvider(p);
    }

    public void openShop(Player p, PlayerRef pr, Ref<EntityStore> ref, Store<EntityStore> store, String shopId) {
        ShopService svc = this.plugin.getShopService();
        if (svc == null) {
            LOGGER.fine("ShopService not available \u2014 cannot open shop");
            return;
        }
        svc.openShop(p, pr, ref, store, shopId);
    }

    public ShopConfig getShop(String id) {
        ShopService svc = this.plugin.getShopService();
        if (svc == null) {
            LOGGER.fine("ShopService not available");
            return null;
        }
        return svc.getShop(id);
    }

    public void registerShop(ShopConfig c) {
        ShopService svc = this.plugin.getShopService();
        if (svc == null) {
            LOGGER.fine("ShopService not available \u2014 cannot register shop");
            return;
        }
        svc.registerShop(c);
    }

    public void registerCurrency(CurrencyProvider p) {
        ShopService svc = this.plugin.getShopService();
        if (svc == null) {
            LOGGER.fine("ShopService not available \u2014 cannot register currency");
            return;
        }
        svc.registerCurrency(p);
    }

    public CurrencyProvider getCurrency(String id) {
        ShopService svc = this.plugin.getShopService();
        if (svc == null) {
            LOGGER.fine("ShopService not available");
            return null;
        }
        return svc.getCurrency(id);
    }

    public boolean grantItem(Player p, String itemId, int amount) {
        return ItemUtils.grantItem(p, itemId, amount);
    }

    public int countItem(Player p, String itemId) {
        return ItemUtils.countItem(p, itemId);
    }

    public boolean removeItem(Player p, String itemId, int amount) {
        return ItemUtils.removeItem(p, itemId, amount);
    }

    public String formatItemName(String itemId) {
        return ItemUtils.formatItemName(itemId);
    }

    public boolean grantLootbag(Player p, String lootbagId) {
        return RewardGrantHelper.grantLootbag(p, lootbagId);
    }

    public boolean grantCommand(Player p, String command, String executor) {
        return RewardGrantHelper.grantCommand(p, command, executor);
    }

    public boolean grantMmoXp(Player p, String skill, int amount) {
        return RewardGrantHelper.grantMmoXp(p, skill, amount);
    }

    public boolean grantRpgXp(Player p, int amount) {
        return RewardGrantHelper.grantRpgXp(p, amount);
    }

    public boolean grantCurrency(Player p, String currency, int amount) {
        return RewardGrantHelper.grantCurrency(p, currency, amount);
    }

    public double getEconomyBalance(Player p) {
        ExternalEconomyBridge bridge = ExternalEconomyBridge.getInstance();
        if (bridge.isAvailable()) {
            return bridge.getBalance(p.getPlayerRef().getUuid());
        }
        return -1.0;
    }

    public boolean isExternalEconomyAvailable() {
        return ExternalEconomyBridge.getInstance().isAvailable();
    }

    public LootbagDefinition getLootbagDefinition(String id) {
        return LootbagService.getDefinition(id);
    }

    public boolean lootbagExists(String id) {
        return LootbagService.exists(id);
    }

    public Map<String, LootbagDefinition> getAllLootbags() {
        return LootbagService.getAllDefinitions();
    }

    public List<String> getAllBlockIds() {
        return GameDataProvider.getAllBlockIds();
    }

    public List<String> getAllItemIds() {
        return GameDataProvider.getAllItemIds();
    }

    public List<String> getBlockIdsByGroup(String group) {
        return GameDataProvider.getBlockIdsByGroup(group);
    }

    public List<String> getAllNPCRoleNames() {
        return GameDataProvider.getAllNPCRoleNames();
    }

    public boolean executeCommand(Player p, String command, String executor) {
        return CommandUtils.executeCommand(p, command, executor);
    }

    public static PlayerDataStorage getMySQLStorage() {
        if (instance == null) {
            return null;
        }
        return CoreAPI.instance.plugin.getMySQLStorage();
    }

    public static boolean isMySQLStorageActive() {
        if (instance == null) {
            return false;
        }
        return CoreAPI.instance.plugin.isMySQLStorageActive();
    }

    public static boolean isDebug() {
        if (instance == null) {
            return false;
        }
        return CoreAPI.instance.plugin.getCoreConfig().getLogLevelNum() >= 3;
    }

    public static boolean shouldLogInfo() {
        if (instance == null) {
            return true;
        }
        return CoreAPI.instance.plugin.getCoreConfig().getLogLevelNum() >= 2;
    }

    public static boolean shouldLogWarning() {
        if (instance == null) {
            return true;
        }
        return CoreAPI.instance.plugin.getCoreConfig().getLogLevelNum() >= 1;
    }

    public static String getLogLevel() {
        if (instance == null) {
            return "info";
        }
        return CoreAPI.instance.plugin.getCoreConfig().getLogLevel();
    }

    public String translate(String key) {
        return CoreI18n.getInstance().get(key);
    }

    public static void addLanguageChangeListener(LanguageChangeListener listener) {
        languageChangeListeners.add(listener);
    }

    public static void removeLanguageChangeListener(LanguageChangeListener listener) {
        languageChangeListeners.remove(listener);
    }

    public static void notifyLanguageChanged(PlayerRef playerRef, UUID playerId) {
        for (LanguageChangeListener listener : languageChangeListeners) {
            try {
                listener.onLanguageChanged(playerRef, playerId);
            }
            catch (Exception e) {
                LOGGER.warning("[Core] LanguageChangeListener error: " + e.getMessage());
            }
        }
    }

    public static String getPlayerLanguage(PlayerRef ref) {
        PlayerPreferences prefs;
        if (instance == null || ref == null) {
            return "en-US";
        }
        PlayerPreferencesService svc = CoreAPI.instance.plugin.getPlayerPreferencesService();
        if (svc != null && (prefs = svc.getPreferences(ref.getUuid())) != null && prefs.hasLanguageOverride()) {
            return prefs.getLanguageOverride();
        }
        String clientLang = ref.getLanguage();
        if (clientLang != null && !clientLang.isEmpty()) {
            return clientLang;
        }
        return CoreAPI.getServerLanguage();
    }

    public static String getServerLanguage() {
        if (instance == null) {
            return "en-US";
        }
        String lang = CoreAPI.instance.plugin.getCoreConfig().getLanguage();
        return lang != null ? lang : "en-US";
    }

    public static void setPlayerLanguageOverride(UUID playerId, String username, String language) {
        if (instance == null) {
            return;
        }
        PlayerPreferencesService svc = CoreAPI.instance.plugin.getPlayerPreferencesService();
        if (svc != null) {
            svc.setLanguageOverride(playerId, username, language);
        }
    }

    public static void clearPlayerLanguageOverride(UUID playerId, String username) {
        if (instance == null) {
            return;
        }
        PlayerPreferencesService svc = CoreAPI.instance.plugin.getPlayerPreferencesService();
        if (svc != null) {
            svc.clearLanguageOverride(playerId, username);
        }
    }

    public static PlayerPreferences getPlayerPreferences(UUID playerId) {
        if (instance == null) {
            return new PlayerPreferences();
        }
        PlayerPreferencesService svc = CoreAPI.instance.plugin.getPlayerPreferencesService();
        if (svc != null) {
            return svc.getPreferences(playerId);
        }
        return new PlayerPreferences();
    }

    public static String getPlayerLanguage(PlayerRef ref, String modId) {
        String modOverride;
        PlayerPreferences prefs;
        if (instance == null || ref == null) {
            return "en-US";
        }
        PlayerPreferencesService svc = CoreAPI.instance.plugin.getPlayerPreferencesService();
        if (svc != null && (prefs = svc.getPreferences(ref.getUuid())) != null && (modOverride = prefs.getModLanguageOverride(modId)) != null && !modOverride.isEmpty()) {
            return modOverride;
        }
        return CoreAPI.getPlayerLanguage(ref);
    }

    public static void setPlayerModLanguageOverride(UUID playerId, String username, String modId, String language) {
        if (instance == null) {
            return;
        }
        PlayerPreferencesService svc = CoreAPI.instance.plugin.getPlayerPreferencesService();
        if (svc != null) {
            svc.setModLanguageOverride(playerId, username, modId, language);
        }
    }

    public static void clearPlayerModLanguageOverride(UUID playerId, String username, String modId) {
        if (instance == null) {
            return;
        }
        PlayerPreferencesService svc = CoreAPI.instance.plugin.getPlayerPreferencesService();
        if (svc != null) {
            svc.clearModLanguageOverride(playerId, username, modId);
        }
    }

    static {
        showcaseMode = false;
        showcaseAdminUuids = Set.of();
        showcaseBreakableBlocks = ConcurrentHashMap.newKeySet();
        npcPageOpeners = new ConcurrentHashMap<String, NpcPageOpener>();
        languageChangeListeners = new ArrayList<LanguageChangeListener>();
    }

    @FunctionalInterface
    public static interface NpcPageOpener {
        public void open(Player var1, PlayerRef var2, Ref<EntityStore> var3, Store<EntityStore> var4);
    }

    public static interface LanguageChangeListener {
        public void onLanguageChanged(PlayerRef var1, UUID var2);
    }
}

