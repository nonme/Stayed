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
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
 *  com.hypixel.hytale.server.core.ui.builder.EventData
 *  com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
 *  com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.kyuubisoft.core.kslang;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.kslang.KsLang;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class KsLangPage
extends InteractiveCustomUIPage<PageData> {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft KsLang");
    private static final String[] LANG_CODES = new String[]{"en-US", "de-DE", "fr-FR", "es-ES", "pt-BR", "ru-RU", "pl-PL", "tr-TR", "it-IT"};

    public KsLangPage(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
    }

    public void build(Ref<EntityStore> ref, UICommandBuilder builder, UIEventBuilder events, Store<EntityStore> store) {
        builder.append("Pages/KsLang/LanguagePage.ui");
        for (String code : LANG_CODES) {
            String btnId = "#Btn" + KsLangPage.langCodeToId(code);
            events.addEventBinding(CustomUIEventBindingType.Activating, btnId, EventData.of((String)"Button", (String)("lang_" + code)), false);
        }
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnAuto", EventData.of((String)"Button", (String)"auto"), false);
        this.updateDisplay(builder);
    }

    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, PageData data) {
        super.handleDataEvent(ref, store, (Object)data);
        if (data.action == null) {
            this.sendUpdate(new UICommandBuilder(), false);
            return;
        }
        if (data.action.startsWith("lang_")) {
            String langCode = data.action.substring(5);
            KsLang.setPlayerLanguage(this.playerRef.getUuid(), langCode);
        } else if ("auto".equals(data.action)) {
            KsLang.clearPlayerLanguage(this.playerRef.getUuid());
        }
        this.rebuild();
    }

    private void updateDisplay(UICommandBuilder ui) {
        String currentLang = KsLang.getPlayerLanguage(this.playerRef);
        String override = KsLang.getPlayerOverride(this.playerRef.getUuid());
        boolean isAuto = override == null || override.isEmpty();
        Map<String, String> supported = KsLang.getSupportedLanguages();
        for (String code : LANG_CODES) {
            String indId = "#Ind" + KsLangPage.langCodeToId(code);
            boolean isActive = code.equals(currentLang);
            ui.set(indId + ".Background.Color", isActive ? "#00bfff" : "#00000000");
        }
        String currentDisplay = supported.getOrDefault(currentLang, currentLang);
        if (isAuto) {
            ui.set("#CurrentLangLabel.Text", "Current: " + currentDisplay + " (auto)");
            ui.set("#BtnAutoInd.Background.Color", "#00bfff");
        } else {
            ui.set("#CurrentLangLabel.Text", "Current: " + currentDisplay);
            ui.set("#BtnAutoInd.Background.Color", "#00000000");
        }
        List<Map<String, String>> mods = KsLang.getRegisteredMods();
        String modList = mods.stream().map(m -> m.getOrDefault("name", m.getOrDefault("id", "?"))).collect(Collectors.joining(", "));
        if (modList.isEmpty()) {
            modList = "-";
        }
        ui.set("#ModsLabel.Text", "Mods using KsLang: " + modList);
    }

    private static String langCodeToId(String code) {
        String[] parts = code.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() <= 1) continue;
            sb.append(part.substring(1));
        }
        return sb.toString();
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = ((BuilderCodec.Builder)BuilderCodec.builder(PageData.class, PageData::new).addField(new KeyedCodec("Button", (Codec)Codec.STRING), (data, s) -> {
            data.action = s;
        }, data -> data.action)).build();
        private String action;
    }
}

