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
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.api.CoreAPI;
import com.kyuubisoft.core.i18n.CoreI18n;
import com.kyuubisoft.core.i18n.I18nContext;
import com.kyuubisoft.core.i18n.LanguageSettingsHelper;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class LanguageSettingsPage
extends InteractiveCustomUIPage<PageEventData> {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core");
    private static final int MAX_MOD_ROWS = 10;
    private final Player player;
    private final PlayerRef playerRef;
    private final List<LanguageSettingsHelper.ModLanguageEntry> mods;

    public LanguageSettingsPage(Player player, PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageEventData.CODEC);
        this.player = player;
        this.playerRef = playerRef;
        this.mods = LanguageSettingsHelper.getRegisteredMods();
    }

    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder ui, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        I18nContext.run(this.playerRef, () -> {
            ui.append("Pages/Language/LanguageSettings.ui");
            this.updateUI(ui);
            this.bindEvents(events);
        });
    }

    private void updateUI(UICommandBuilder ui) {
        CoreI18n i18n = CoreI18n.getInstance();
        String autoName = LanguageSettingsHelper.getAutoLanguageName(this.playerRef);
        List<DropdownEntryInfo> globalEntries = LanguageSettingsHelper.createLanguageDropdownEntries(autoName);
        ui.set("#GlobalLang.Entries", globalEntries);
        ui.set("#GlobalLang.Value", LanguageSettingsHelper.getCurrentDropdownValue(this.playerRef.getUuid()));
        ui.set("#LangTitle.Text", i18n.get("lang.page.title"));
        ui.set("#GlobalLangLabel.Text", i18n.get("lang.page.global"));
        ui.set("#LangGlobalSection.Text", i18n.get("lang.page.global_section"));
        ui.set("#LangGlobalHint.Text", i18n.get("lang.page.global_hint"));
        ui.set("#ModsSectionLabel.Text", i18n.get("lang.page.mods"));
        ui.set("#LangModsHint.Text", i18n.get("lang.page.mods_hint"));
        ui.set("#LangApplyLbl.Text", i18n.get("lang.page.apply"));
        ui.set("#LangResetLbl.Text", i18n.get("lang.page.reset"));
        String globalResolved = CoreAPI.getPlayerLanguage(this.playerRef);
        String globalDisplayName = LanguageSettingsHelper.getLanguageDisplayName(globalResolved);
        int modCount = Math.min(this.mods.size(), 10);
        for (int i = 0; i < 10; ++i) {
            if (i < modCount) {
                LanguageSettingsHelper.ModLanguageEntry mod = this.mods.get(i);
                ui.set("#ModRow" + i + ".Visible", true);
                ui.set("#ModName" + i + ".Text", mod.displayName());
                List<DropdownEntryInfo> modEntries = LanguageSettingsHelper.createModLanguageDropdownEntries(globalDisplayName, autoName);
                ui.set("#ModLang" + i + ".Entries", modEntries);
                ui.set("#ModLang" + i + ".Value", LanguageSettingsHelper.getCurrentModDropdownValue(this.playerRef.getUuid(), mod.modId()));
                continue;
            }
            ui.set("#ModRow" + i + ".Visible", false);
        }
    }

    private EventData applyEventData() {
        EventData data = new EventData().append("Button", "apply").append("@GlobalLang", "#GlobalLang.Value");
        int modCount = Math.min(this.mods.size(), 10);
        for (int i = 0; i < modCount; ++i) {
            data.append("@ModLang" + i, "#ModLang" + i + ".Value");
        }
        return data;
    }

    private void bindEvents(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BackgroundOverlay", EventData.of((String)"Button", (String)"close"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LangCloseBtn", EventData.of((String)"Button", (String)"close"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LangApplyBtn", this.applyEventData(), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LangResetBtn", EventData.of((String)"Button", (String)"reset"), false);
    }

    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageEventData data) {
        I18nContext.run(this.playerRef, () -> {
            try {
                super.handleDataEvent(ref, store, (Object)data);
                if (data.button == null) {
                    return;
                }
                switch (data.button) {
                    case "close": {
                        this.close();
                        return;
                    }
                    case "apply": {
                        this.applySettings(data);
                        this.close();
                        return;
                    }
                    case "reset": {
                        this.resetSettings();
                        this.refreshUI();
                        return;
                    }
                }
            }
            catch (Exception e) {
                LOGGER.severe("Exception in LanguageSettingsPage: " + e.getMessage());
            }
        });
    }

    private void applySettings(PageEventData data) {
        String username = this.player.getDisplayName();
        UUID playerId = this.playerRef.getUuid();
        if (data.globalLang != null) {
            LanguageSettingsHelper.applyLanguageSelection(playerId, username, data.globalLang);
        }
        int modCount = Math.min(this.mods.size(), 10);
        String[] modValues = new String[]{data.modLang0, data.modLang1, data.modLang2, data.modLang3, data.modLang4, data.modLang5, data.modLang6, data.modLang7, data.modLang8, data.modLang9};
        for (int i = 0; i < modCount; ++i) {
            String value = modValues[i];
            if (value == null) continue;
            LanguageSettingsHelper.applyModLanguageSelection(playerId, username, this.mods.get(i).modId(), value);
        }
        CoreAPI.notifyLanguageChanged(this.playerRef, playerId);
        if (CoreAPI.isDebug()) {
            LOGGER.info("Applied language settings for " + username);
        }
    }

    private void resetSettings() {
        String username = this.player.getDisplayName();
        UUID playerId = this.playerRef.getUuid();
        LanguageSettingsHelper.applyLanguageSelection(playerId, username, "auto");
        int modCount = Math.min(this.mods.size(), 10);
        for (int i = 0; i < modCount; ++i) {
            LanguageSettingsHelper.applyModLanguageSelection(playerId, username, this.mods.get(i).modId(), "global");
        }
        CoreAPI.notifyLanguageChanged(this.playerRef, playerId);
    }

    private void refreshUI() {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        this.updateUI(ui);
        this.bindEvents(events);
        this.sendUpdate(ui, events, false);
    }

    public static class PageEventData {
        public static final BuilderCodec<PageEventData> CODEC = ((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)BuilderCodec.builder(PageEventData.class, PageEventData::new).addField(new KeyedCodec("Button", (Codec)Codec.STRING), (d, v) -> {
            d.button = v;
        }, d -> d.button)).addField(new KeyedCodec("@GlobalLang", (Codec)Codec.STRING), (d, v) -> {
            d.globalLang = v;
        }, d -> d.globalLang)).addField(new KeyedCodec("@ModLang0", (Codec)Codec.STRING), (d, v) -> {
            d.modLang0 = v;
        }, d -> d.modLang0)).addField(new KeyedCodec("@ModLang1", (Codec)Codec.STRING), (d, v) -> {
            d.modLang1 = v;
        }, d -> d.modLang1)).addField(new KeyedCodec("@ModLang2", (Codec)Codec.STRING), (d, v) -> {
            d.modLang2 = v;
        }, d -> d.modLang2)).addField(new KeyedCodec("@ModLang3", (Codec)Codec.STRING), (d, v) -> {
            d.modLang3 = v;
        }, d -> d.modLang3)).addField(new KeyedCodec("@ModLang4", (Codec)Codec.STRING), (d, v) -> {
            d.modLang4 = v;
        }, d -> d.modLang4)).addField(new KeyedCodec("@ModLang5", (Codec)Codec.STRING), (d, v) -> {
            d.modLang5 = v;
        }, d -> d.modLang5)).addField(new KeyedCodec("@ModLang6", (Codec)Codec.STRING), (d, v) -> {
            d.modLang6 = v;
        }, d -> d.modLang6)).addField(new KeyedCodec("@ModLang7", (Codec)Codec.STRING), (d, v) -> {
            d.modLang7 = v;
        }, d -> d.modLang7)).addField(new KeyedCodec("@ModLang8", (Codec)Codec.STRING), (d, v) -> {
            d.modLang8 = v;
        }, d -> d.modLang8)).addField(new KeyedCodec("@ModLang9", (Codec)Codec.STRING), (d, v) -> {
            d.modLang9 = v;
        }, d -> d.modLang9)).build();
        private String button;
        private String globalLang;
        private String modLang0;
        private String modLang1;
        private String modLang2;
        private String modLang3;
        private String modLang4;
        private String modLang5;
        private String modLang6;
        private String modLang7;
        private String modLang8;
        private String modLang9;
    }
}

