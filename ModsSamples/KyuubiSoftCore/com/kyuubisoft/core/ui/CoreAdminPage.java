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
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage
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
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
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
import com.kyuubisoft.core.citizen.CitizenAdminPage;
import com.kyuubisoft.core.citizen.NpcViewerPage;
import com.kyuubisoft.core.config.CoreConfig;
import com.kyuubisoft.core.i18n.CoreI18n;
import com.kyuubisoft.core.i18n.I18nContext;
import com.kyuubisoft.core.registry.ModMenuRegistry;
import com.kyuubisoft.core.ui.LootbagAdminPage;
import com.kyuubisoft.core.ui.ShopAdminPage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class CoreAdminPage
extends InteractiveCustomUIPage<PageEventData> {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core Admin");
    private static final String[] KNOWN_MODS = new String[]{"quests", "achievements", "infohub", "lootbags", "citizens", "shops", "npcviewer", "seasonpass", "itemcontrol"};
    private static final String[] BUTTON_IDS = new String[]{"#ModQuests", "#ModAchievements", "#ModInfohub", "#ModLootbags", "#ModCitizens", "#ModShops", "#ModNpcviewer", "#ModSeasonpass", "#ModItemcontrol"};
    private static final String[] DEFAULT_NAME_KEYS = new String[]{"mod.quests.name", "mod.achievements.name", "mod.infohub.name", "mod.lootbags.name", "mod.citizens.name", "mod.shops.name", "mod.npcviewer.name", "mod.seasonpass.name", "mod.itemcontrol.name"};
    private static final String[] DEFAULT_DESC_KEYS = new String[]{"mod.quests.desc", "mod.achievements.desc", "mod.infohub.desc", "mod.lootbags.desc", "mod.citizens.desc", "mod.shops.desc", "mod.npcviewer.desc", "mod.seasonpass.desc", "mod.itemcontrol.desc"};
    private static final int MAX_LINKS = 3;
    private final CorePlugin plugin;
    private final Player player;
    private final PlayerRef playerRef;

    public CoreAdminPage(CorePlugin plugin, Player player, PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageEventData.CODEC);
        this.plugin = plugin;
        this.player = player;
        this.playerRef = playerRef;
    }

    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder ui, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        I18nContext.run(this.playerRef, () -> {
            ui.append("Pages/CoreAdmin/AdminPanel.ui");
            events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of((String)"Button", (String)"close"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ModQuests", EventData.of((String)"Button", (String)"quests"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ModAchievements", EventData.of((String)"Button", (String)"achievements"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ModInfohub", EventData.of((String)"Button", (String)"infohub"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ModLootbags", EventData.of((String)"Button", (String)"lootbags"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ModCitizens", EventData.of((String)"Button", (String)"citizens"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ModShops", EventData.of((String)"Button", (String)"shops"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ModNpcviewer", EventData.of((String)"Button", (String)"npcviewer"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ModSeasonpass", EventData.of((String)"Button", (String)"seasonpass"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ModItemcontrol", EventData.of((String)"Button", (String)"itemcontrol"), false);
            for (int i = 0; i < 3; ++i) {
                events.addEventBinding(CustomUIEventBindingType.Activating, "#Link" + i, EventData.of((String)"Button", (String)("link" + i)), false);
            }
            events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LanguageDropdown", EventData.of((String)"@Language", (String)"#LanguageDropdown.Value"));
            this.refreshUI(ui);
        });
    }

    private void refreshUI(UICommandBuilder ui) {
        CoreConfig config = this.plugin.getCoreConfig();
        CoreI18n i18n = CoreI18n.getInstance();
        ui.set("#ServerTitle.Text", config.getTitle());
        ui.set("#WelcomeLabel.Text", i18n.get("admin.welcome", this.playerRef.getUsername()));
        ui.set("#ServerDesc.Text", i18n.get("admin.description"));
        ui.set("#LanguageLabel.Text", i18n.get("admin.language"));
        ArrayList<DropdownEntryInfo> langEntries = new ArrayList<DropdownEntryInfo>();
        langEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)"English"), "en-US"));
        langEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)"Deutsch"), "de-DE"));
        langEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)"Francais"), "fr-FR"));
        langEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)"Espanol"), "es-ES"));
        langEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)"Italiano"), "it-IT"));
        langEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)"Portugues (BR)"), "pt-BR"));
        langEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)"Polski"), "pl-PL"));
        langEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)"Turkish"), "tr-TR"));
        langEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)"Russian"), "ru-RU"));
        ui.set("#LanguageDropdown.Entries", langEntries);
        ui.set("#LanguageDropdown.Value", config.getLanguage());
        List<ModMenuRegistry.ModMenuEntry> mods = ModMenuRegistry.getEntries();
        int moduleCount = 0;
        for (int k = 0; k < KNOWN_MODS.length; ++k) {
            String modId = KNOWN_MODS[k];
            String btnId = BUTTON_IDS[k];
            String cap = CoreAdminPage.capitalize(modId);
            ModMenuRegistry.ModMenuEntry entry = null;
            for (ModMenuRegistry.ModMenuEntry m : mods) {
                if (!modId.equals(m.id())) continue;
                entry = m;
                break;
            }
            if ("lootbags".equals(modId) || "citizens".equals(modId) || "shops".equals(modId) || "npcviewer".equals(modId)) {
                String statusColor = "lootbags".equals(modId) ? "#cc66ff" : ("shops".equals(modId) ? "#ffaa44" : ("npcviewer".equals(modId) ? "#44ddbb" : "#66bbff"));
                ui.set(btnId + " #ModName" + cap + ".Text", i18n.get(DEFAULT_NAME_KEYS[k]));
                ui.set(btnId + " #ModVersion" + cap + ".Text", i18n.get("admin.badge.builtin"));
                ui.set(btnId + " #ModDesc" + cap + ".Text", i18n.get(DEFAULT_DESC_KEYS[k]));
                ui.set(btnId + " #ModStatus" + cap + ".Text", i18n.get("admin.badge.core"));
                ui.set(btnId + " #ModStatus" + cap + ".Style.TextColor", statusColor);
                ++moduleCount;
                continue;
            }
            if (entry != null) {
                ui.set(btnId + " #ModName" + cap + ".Text", entry.name());
                ui.set(btnId + " #ModVersion" + cap + ".Text", "v" + entry.version());
                ui.set(btnId + " #ModDesc" + cap + ".Text", entry.description());
                ui.set(btnId + " #ModStatus" + cap + ".Text", i18n.get("admin.badge.active"));
                ui.set(btnId + " #ModStatus" + cap + ".Style.TextColor", "#44cc88");
                ++moduleCount;
                continue;
            }
            ui.set(btnId + " #ModName" + cap + ".Text", i18n.get(DEFAULT_NAME_KEYS[k]));
            ui.set(btnId + " #ModVersion" + cap + ".Text", "");
            ui.set(btnId + " #ModDesc" + cap + ".Text", i18n.get("status.not_installed"));
            ui.set(btnId + " #ModStatus" + cap + ".Text", i18n.get("admin.badge.na"));
            ui.set(btnId + " #ModStatus" + cap + ".Style.TextColor", "#556677");
        }
        ui.set("#FooterLabel.Text", "KyuubiSoft Core v2.2.3 \u2014 " + i18n.get("admin.footer", moduleCount));
        List<CoreConfig.LinkEntry> links = config.getLinks();
        boolean hasLinks = !links.isEmpty();
        ui.set("#LinkSeparator.Visible", hasLinks);
        for (int i = 0; i < 3; ++i) {
            boolean visible = i < links.size();
            ui.set("#Link" + i + ".Visible", visible);
            if (i < 2) {
                ui.set("#LinkSpacer" + i + ".Visible", visible && i + 1 < links.size());
            }
            if (!visible) continue;
            CoreConfig.LinkEntry link = links.get(i);
            ui.set("#LinkName" + i + ".Text", link.name);
            ui.set("#LinkDesc" + i + ".Text", link.description != null ? link.description : "");
            String linkColor = link.color != null ? link.color : "#7289da";
            ui.set("#LinkName" + i + ".Style.TextColor", linkColor);
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageEventData data) {
        I18nContext.run(this.playerRef, () -> {
            super.handleDataEvent(ref, store, (Object)data);
            if (data.languageValue != null) {
                this.handleLanguageChange(data.languageValue);
                return;
            }
            if (data.button == null) {
                return;
            }
            LOGGER.info("Button pressed: " + data.button + " by " + this.playerRef.getUsername());
            switch (data.button) {
                case "close": {
                    this.close();
                    break;
                }
                case "quests": 
                case "achievements": 
                case "infohub": 
                case "seasonpass": 
                case "itemcontrol": {
                    this.handleModButton(data.button, ref, store);
                    break;
                }
                case "lootbags": {
                    this.openLootbagAdmin(ref, store);
                    break;
                }
                case "citizens": {
                    this.openCitizenAdmin(ref, store);
                    break;
                }
                case "shops": {
                    this.openShopAdmin(ref, store);
                    break;
                }
                case "npcviewer": {
                    this.openNpcViewer(ref, store);
                    break;
                }
                default: {
                    if (data.button.startsWith("link")) {
                        this.handleLinkButton(data.button);
                        break;
                    }
                    LOGGER.warning("Unknown button: " + data.button);
                }
            }
        });
    }

    private void handleModButton(String modId, Ref<EntityStore> ref, Store<EntityStore> store) {
        List<ModMenuRegistry.ModMenuEntry> mods = ModMenuRegistry.getEntries();
        for (ModMenuRegistry.ModMenuEntry entry : mods) {
            if (!modId.equals(entry.id())) continue;
            try {
                LOGGER.info("Opening mod panel: " + entry.id());
                entry.opener().open(this.player, this.playerRef, ref, store);
                return;
            }
            catch (Exception e) {
                LOGGER.warning("Error opening mod panel: " + e.getMessage());
                this.player.sendMessage(Message.raw((String)("Error: " + e.getMessage())).color("#FF5555"));
            }
        }
        this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("error.module_not_installed")).color("#FF5555"));
        UICommandBuilder ui = new UICommandBuilder();
        this.refreshUI(ui);
        this.sendUpdate(ui, false);
    }

    private void openLootbagAdmin(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            LOGGER.info("Opening Lootbag Admin panel");
            LootbagAdminPage lootbagPage = new LootbagAdminPage(this.plugin, this.player, this.playerRef);
            this.player.getPageManager().openCustomPage(ref, store, (CustomUIPage)lootbagPage);
        }
        catch (Exception e) {
            LOGGER.warning("Error opening lootbag admin: " + e.getMessage());
            this.player.sendMessage(Message.raw((String)("Error: " + e.getMessage())).color("#FF5555"));
            UICommandBuilder ui = new UICommandBuilder();
            this.refreshUI(ui);
            this.sendUpdate(ui, false);
        }
    }

    private void openShopAdmin(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            LOGGER.info("Opening Shop Admin panel");
            ShopAdminPage shopPage = new ShopAdminPage(this.plugin, this.player, this.playerRef);
            this.player.getPageManager().openCustomPage(ref, store, (CustomUIPage)shopPage);
        }
        catch (Exception e) {
            LOGGER.warning("Error opening shop admin: " + e.getMessage());
            this.player.sendMessage(Message.raw((String)("Error: " + e.getMessage())).color("#FF5555"));
            UICommandBuilder ui = new UICommandBuilder();
            this.refreshUI(ui);
            this.sendUpdate(ui, false);
        }
    }

    private void openNpcViewer(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            LOGGER.info("Opening NPC Viewer panel");
            NpcViewerPage npcViewerPage = new NpcViewerPage(this.plugin, this.player, this.playerRef);
            this.player.getPageManager().openCustomPage(ref, store, (CustomUIPage)npcViewerPage);
        }
        catch (Exception e) {
            LOGGER.warning("Error opening NPC viewer: " + e.getMessage());
            this.player.sendMessage(Message.raw((String)("Error: " + e.getMessage())).color("#FF5555"));
            UICommandBuilder ui = new UICommandBuilder();
            this.refreshUI(ui);
            this.sendUpdate(ui, false);
        }
    }

    private void openCitizenAdmin(Ref<EntityStore> ref, Store<EntityStore> store) {
        try {
            LOGGER.info("Opening Citizen Admin panel");
            CitizenAdminPage citizenPage = new CitizenAdminPage(this.plugin, this.player, this.playerRef);
            this.player.getPageManager().openCustomPage(ref, store, (CustomUIPage)citizenPage);
        }
        catch (Exception e) {
            LOGGER.warning("Error opening citizen admin: " + e.getMessage());
            this.player.sendMessage(Message.raw((String)("Error: " + e.getMessage())).color("#FF5555"));
            UICommandBuilder ui = new UICommandBuilder();
            this.refreshUI(ui);
            this.sendUpdate(ui, false);
        }
    }

    private void handleLinkButton(String buttonId) {
        try {
            int index = Integer.parseInt(buttonId.substring(4));
            List<CoreConfig.LinkEntry> links = this.plugin.getCoreConfig().getLinks();
            if (index >= 0 && index < links.size()) {
                CoreConfig.LinkEntry link = links.get(index);
                this.close();
                Message linkMessage = Message.raw((String)"").insert(Message.raw((String)">>> ").color("#7289DA").bold(true)).insert(Message.raw((String)(link.name + ": ")).color("#ffffff").bold(true)).insert(Message.raw((String)link.url).link(link.url).color("#88CCFF"));
                this.player.sendMessage(linkMessage);
                if (link.description != null && !link.description.isEmpty()) {
                    this.player.sendMessage(Message.raw((String)link.description).color("#99AABB").italic(true));
                }
                return;
            }
        }
        catch (NumberFormatException e) {
            LOGGER.warning("Invalid link button: " + buttonId);
        }
        UICommandBuilder ui = new UICommandBuilder();
        this.refreshUI(ui);
        this.sendUpdate(ui, false);
    }

    private void handleLanguageChange(String newLanguage) {
        if (CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) {
            this.sendUpdate(new UICommandBuilder(), false);
            return;
        }
        CoreConfig config = this.plugin.getCoreConfig();
        String currentLang = config.getLanguage();
        if (newLanguage.equals(currentLang)) {
            this.sendUpdate(new UICommandBuilder(), false);
            return;
        }
        config.setLanguage(newLanguage);
        config.save();
        CoreI18n.getInstance().load(newLanguage, this.plugin.getDataDirectory());
        ModMenuRegistry.notifyLanguageChange(newLanguage);
        LOGGER.info("Language changed to " + newLanguage + " by " + this.playerRef.getUsername());
        UICommandBuilder ui = new UICommandBuilder();
        this.refreshUI(ui);
        this.sendUpdate(ui, false);
        this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("admin.language.changed", newLanguage)).color("#44cc88"));
    }

    public static class PageEventData {
        public String button;
        public String languageValue;
        public static final BuilderCodec<PageEventData> CODEC = ((BuilderCodec.Builder)((BuilderCodec.Builder)BuilderCodec.builder(PageEventData.class, PageEventData::new).addField(new KeyedCodec("Button", (Codec)Codec.STRING), (d, v) -> {
            d.button = v;
        }, d -> d.button)).addField(new KeyedCodec("@Language", (Codec)Codec.STRING), (d, v) -> {
            d.languageValue = v;
        }, d -> d.languageValue)).build();
    }
}

