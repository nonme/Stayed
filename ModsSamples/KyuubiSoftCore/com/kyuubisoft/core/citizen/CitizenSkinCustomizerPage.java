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
 *  com.hypixel.hytale.protocol.PlayerSkin
 *  com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
 *  com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
 *  com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry
 *  com.hypixel.hytale.server.core.cosmetics.CosmeticsModule
 *  com.hypixel.hytale.server.core.cosmetics.PlayerSkinGradientSet
 *  com.hypixel.hytale.server.core.cosmetics.PlayerSkinPart
 *  com.hypixel.hytale.server.core.cosmetics.PlayerSkinPart$Variant
 *  com.hypixel.hytale.server.core.cosmetics.PlayerSkinPartTexture
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
 *  com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent
 *  com.hypixel.hytale.server.core.ui.builder.EventData
 *  com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
 *  com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  javax.annotation.Nonnull
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinGradientSet;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPart;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPartTexture;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.CorePlugin;
import com.kyuubisoft.core.citizen.CitizenAdminPage;
import com.kyuubisoft.core.citizen.CitizenData;
import com.kyuubisoft.core.citizen.CitizenService;
import com.kyuubisoft.core.citizen.CitizenSkinManager;
import com.kyuubisoft.core.i18n.I18nContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class CitizenSkinCustomizerPage
extends InteractiveCustomUIPage<SkinPageData> {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Skin Customizer");
    private static final int OPTIONS_COUNT = 30;
    private static final int COLS_PER_ROW = 3;
    private static final int ROW_COUNT = 10;
    private static final int CATEGORY_COUNT = 20;
    private static final int COLOR_COUNT = 12;
    private static final String[] CATEGORY_NAMES = new String[]{"Body", "Underwear", "Face", "Eyes", "Ears", "Mouth", "Facial Hair", "Haircut", "Eyebrows", "Pants", "Overpants", "Undertop", "Overtop", "Shoes", "Head Acc", "Face Acc", "Ear Acc", "Skin Feat", "Gloves", "Cape"};
    private final CorePlugin plugin;
    private final Player player;
    private final PlayerRef playerRef;
    private final String citizenId;
    private final CitizenService citizenService;
    private Ref<EntityStore> storedRef;
    private Store<EntityStore> storedStore;
    private PlayerSkin editingSkin;
    private int currentCategory = 0;
    private List<String> currentOptions = new ArrayList<String>();
    private Map<String, PlayerSkinPart> currentParts = new LinkedHashMap<String, PlayerSkinPart>();
    private List<String> currentTextureKeys = new ArrayList<String>();
    private List<String> currentTextureColors = new ArrayList<String>();

    public CitizenSkinCustomizerPage(CorePlugin plugin, Player player, PlayerRef playerRef, CitizenData citizen) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, SkinPageData.CODEC);
        this.plugin = plugin;
        this.player = player;
        this.playerRef = playerRef;
        this.citizenId = citizen.id;
        this.citizenService = plugin.getCitizenService();
        this.editingSkin = citizen.customSkin != null ? new PlayerSkin(citizen.customSkin) : CosmeticsModule.get().generateRandomSkin(new Random());
        try {
            this.loadCategoryOptions();
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[SkinCustomizer] Failed to load initial categories", e);
            this.currentOptions = new ArrayList<String>();
            this.currentOptions.add("");
        }
    }

    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder ui, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        I18nContext.run(this.playerRef, () -> {
            this.storedRef = ref;
            this.storedStore = store;
            ui.append("Pages/CitizenAdmin/SkinCustomizer.ui");
            this.bindEvents(events);
            this.buildCategories(ui);
            this.buildOptions(ui);
            this.buildColors(ui);
        });
    }

    private void bindEvents(UIEventBuilder events) {
        int i;
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SkinCloseBtn", EventData.of((String)"Button", (String)"cancel"), false);
        for (i = 0; i < 20; ++i) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#Cat" + i, EventData.of((String)"Button", (String)("cat_" + i)), false);
        }
        for (i = 0; i < 30; ++i) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#Opt" + i, EventData.of((String)"Button", (String)("opt_" + i)), false);
        }
        for (i = 0; i < 12; ++i) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#Clr" + i, EventData.of((String)"Button", (String)("clr_" + i)), false);
        }
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SkinMyBtn", EventData.of((String)"Button", (String)"mySkin"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SkinRandBtn", EventData.of((String)"Button", (String)"random"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SkinResetBtn", EventData.of((String)"Button", (String)"reset"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SkinApplyBtn", EventData.of((String)"Button", (String)"apply"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SkinCancelBtn", EventData.of((String)"Button", (String)"cancel"), false);
    }

    private void buildCategories(UICommandBuilder ui) {
        for (int i = 0; i < 20; ++i) {
            String label = i == this.currentCategory ? "> " + CATEGORY_NAMES[i] : "  " + CATEGORY_NAMES[i];
            ui.set("#CatLabel" + i + ".Text", label);
        }
    }

    private void buildOptions(UICommandBuilder ui) {
        ui.set("#CurrentCatLabel.Text", CATEGORY_NAMES[this.currentCategory]);
        String currentVal = this.getSkinField(this.editingSkin, this.currentCategory);
        String currentBase = CitizenSkinCustomizerPage.extractBaseId(currentVal);
        ui.set("#CurrentValLabel.Text", currentBase != null ? currentBase : "(none)");
        int totalOptions = this.currentOptions.size();
        for (int i = 0; i < 30; ++i) {
            if (i < totalOptions) {
                PlayerSkinPart part;
                String optionId = this.currentOptions.get(i);
                boolean isNone = optionId.isEmpty();
                String optionBase = CitizenSkinCustomizerPage.extractBaseId(optionId);
                boolean isSelected = Objects.equals(optionBase, currentBase) || isNone && currentVal == null;
                String displayName = isNone ? "(none)" : optionBase;
                ui.set("#OptName" + i + ".Text", (String)(isSelected ? "[*] " + displayName : displayName));
                String color = "#888888";
                if (!isNone && (part = this.currentParts.get(optionBase)) != null) {
                    color = this.getCompoundColor(optionId, part);
                }
                ui.set("#OptColor" + i + ".Background", color);
                continue;
            }
            ui.set("#OptName" + i + ".Text", "");
            ui.set("#OptColor" + i + ".Background", "#888888");
        }
        for (int row = 0; row < 10; ++row) {
            int firstSlotInRow = row * 3;
            ui.set("#OptRow" + row + ".Visible", firstSlotInRow < totalOptions);
        }
    }

    private void buildColors(UICommandBuilder ui) {
        try {
            String[] parts;
            String currentVal;
            boolean hasColors = this.currentTextureKeys.size() > 1;
            ui.set("#ColorPanel.Visible", hasColors);
            String currentTexKey = null;
            if (hasColors && (currentVal = this.getSkinField(this.editingSkin, this.currentCategory)) != null && (parts = currentVal.split("\\.")).length >= 2) {
                currentTexKey = parts[1];
            }
            for (int i = 0; i < 12; ++i) {
                if (hasColors && i < this.currentTextureKeys.size()) {
                    String texKey = this.currentTextureKeys.get(i);
                    String color = this.currentTextureColors.get(i);
                    boolean selected = texKey.equals(currentTexKey);
                    ui.set("#ClrWrap" + i + ".Visible", true);
                    ui.set("#ClrWrap" + i + ".Background", selected ? "#aa88ee" : "#333344");
                    ui.set("#ClrSwatch" + i + ".Background", color);
                    continue;
                }
                ui.set("#ClrWrap" + i + ".Visible", false);
            }
        }
        catch (Exception e) {
            LOGGER.warning("[SkinCustomizer] Failed to build colors: " + e.getMessage());
            ui.set("#ColorPanel.Visible", false);
        }
    }

    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull SkinPageData data) {
        super.handleDataEvent(ref, store, (Object)data);
        I18nContext.run(this.playerRef, () -> {
            this.storedRef = ref;
            this.storedStore = store;
            if (data.button == null) {
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            if (data.button.startsWith("cat_")) {
                try {
                    int catIdx = Integer.parseInt(data.button.substring(4));
                    if (catIdx >= 0 && catIdx < 20 && catIdx != this.currentCategory) {
                        this.currentCategory = catIdx;
                        this.currentTextureKeys.clear();
                        this.currentTextureColors.clear();
                        this.loadCategoryOptions();
                    }
                }
                catch (NumberFormatException catIdx) {
                    // empty catch block
                }
                this.refreshUI();
                return;
            }
            if (data.button.startsWith("opt_")) {
                try {
                    int slotIdx = Integer.parseInt(data.button.substring(4));
                    if (slotIdx >= 0 && slotIdx < this.currentOptions.size()) {
                        String optionId = this.currentOptions.get(slotIdx);
                        this.setSkinField(this.editingSkin, this.currentCategory, optionId.isEmpty() ? null : optionId);
                        this.loadColorOptions(optionId);
                        this.applyLivePreview();
                    }
                }
                catch (NumberFormatException slotIdx) {
                    // empty catch block
                }
                this.refreshUI();
                return;
            }
            if (data.button.startsWith("clr_")) {
                try {
                    int colorIdx = Integer.parseInt(data.button.substring(4));
                    if (colorIdx >= 0 && colorIdx < this.currentTextureKeys.size()) {
                        String newTextureKey = this.currentTextureKeys.get(colorIdx);
                        String currentVal = this.getSkinField(this.editingSkin, this.currentCategory);
                        if (currentVal != null) {
                            String newVal = CitizenSkinCustomizerPage.replaceTextureKey(currentVal, newTextureKey);
                            this.setSkinField(this.editingSkin, this.currentCategory, newVal);
                            this.applyLivePreview();
                        }
                    }
                }
                catch (NumberFormatException colorIdx) {
                    // empty catch block
                }
                this.refreshUI();
                return;
            }
            switch (data.button) {
                case "mySkin": {
                    try {
                        PlayerSkinComponent skinComp = (PlayerSkinComponent)this.storedStore.getComponent(this.storedRef, PlayerSkinComponent.getComponentType());
                        if (skinComp != null) {
                            this.editingSkin = new PlayerSkin(skinComp.getPlayerSkin());
                        }
                    }
                    catch (Exception e) {
                        LOGGER.warning("[SkinCustomizer] Could not read player skin: " + e.getMessage());
                    }
                    this.currentTextureKeys.clear();
                    this.currentTextureColors.clear();
                    this.loadCategoryOptions();
                    this.applyLivePreview();
                    this.refreshUI();
                    break;
                }
                case "random": {
                    this.editingSkin = CosmeticsModule.get().generateRandomSkin(new Random());
                    this.currentTextureKeys.clear();
                    this.currentTextureColors.clear();
                    this.loadCategoryOptions();
                    this.applyLivePreview();
                    this.refreshUI();
                    break;
                }
                case "reset": {
                    this.editingSkin = CosmeticsModule.get().generateRandomSkin(new Random());
                    this.editingSkin.facialHair = null;
                    this.editingSkin.haircut = null;
                    this.editingSkin.eyebrows = null;
                    this.editingSkin.pants = null;
                    this.editingSkin.overpants = null;
                    this.editingSkin.undertop = null;
                    this.editingSkin.overtop = null;
                    this.editingSkin.shoes = null;
                    this.editingSkin.headAccessory = null;
                    this.editingSkin.faceAccessory = null;
                    this.editingSkin.earAccessory = null;
                    this.editingSkin.skinFeature = null;
                    this.editingSkin.gloves = null;
                    this.editingSkin.cape = null;
                    this.currentTextureKeys.clear();
                    this.currentTextureColors.clear();
                    this.loadCategoryOptions();
                    this.applyLivePreview();
                    this.refreshUI();
                    break;
                }
                case "apply": {
                    CitizenData citizen = this.citizenService.getCitizen(this.citizenId);
                    if (citizen != null) {
                        citizen.customSkin = new PlayerSkin(this.editingSkin);
                        citizen.isPlayerModel = true;
                        this.citizenService.updateCitizen(citizen);
                    }
                    this.returnToAdmin();
                    break;
                }
                case "cancel": {
                    this.returnToAdmin();
                    break;
                }
                default: {
                    this.sendUpdate(new UICommandBuilder(), false);
                }
            }
        });
    }

    private void refreshUI() {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        this.bindEvents(events);
        this.buildCategories(ui);
        this.buildOptions(ui);
        this.buildColors(ui);
        this.sendUpdate(ui, events, false);
    }

    private void applyLivePreview() {
        CitizenData citizen = this.citizenService.getCitizen(this.citizenId);
        if (citizen != null && citizen.entityRef != null) {
            World world = this.player.getWorld();
            if (world == null) {
                return;
            }
            CitizenSkinManager skinMgr = this.citizenService.getSkinManager();
            PlayerSkin skinCopy = new PlayerSkin(this.editingSkin);
            Ref<EntityStore> entityRef = citizen.entityRef;
            float scale = citizen.scale;
            world.execute(() -> skinMgr.applySkin(entityRef, skinCopy, scale));
        }
    }

    private void returnToAdmin() {
        CitizenAdminPage adminPage = new CitizenAdminPage(this.plugin, this.player, this.playerRef);
        adminPage.setSelectedCitizenId(this.citizenId);
        this.player.getPageManager().openCustomPage(this.storedRef, this.storedStore, (CustomUIPage)adminPage);
    }

    private static boolean categoryUsesBareId(int category) {
        return category == 2 || category == 4 || category == 5;
    }

    private void loadCategoryOptions() {
        CosmeticRegistry registry = CosmeticsModule.get().getRegistry();
        Map<String, PlayerSkinPart> parts = this.getRegistryParts(registry, this.currentCategory);
        this.currentParts = parts;
        this.currentOptions = new ArrayList<String>();
        this.currentOptions.add("");
        for (String partId : new TreeSet<String>(parts.keySet())) {
            if (CitizenSkinCustomizerPage.categoryUsesBareId(this.currentCategory)) {
                this.currentOptions.add(partId);
                continue;
            }
            PlayerSkinPart part = parts.get(partId);
            String compound = this.buildCompoundValue(partId, part);
            if (compound == null) continue;
            this.currentOptions.add(compound);
        }
        try {
            String currentVal = this.getSkinField(this.editingSkin, this.currentCategory);
            if (currentVal != null && !CitizenSkinCustomizerPage.categoryUsesBareId(this.currentCategory)) {
                this.loadColorOptions(currentVal);
            }
        }
        catch (Exception e) {
            LOGGER.warning("[SkinCustomizer] Failed to auto-load colors: " + e.getMessage());
            this.currentTextureKeys.clear();
            this.currentTextureColors.clear();
        }
    }

    private void loadColorOptions(String selectedCompoundOrBareId) {
        PlayerSkinPart.Variant variant;
        CosmeticRegistry reg;
        PlayerSkinGradientSet gradientSet;
        this.currentTextureKeys.clear();
        this.currentTextureColors.clear();
        if (selectedCompoundOrBareId == null || selectedCompoundOrBareId.isEmpty()) {
            return;
        }
        if (CitizenSkinCustomizerPage.categoryUsesBareId(this.currentCategory)) {
            return;
        }
        String baseId = CitizenSkinCustomizerPage.extractBaseId(selectedCompoundOrBareId);
        PlayerSkinPart part = this.currentParts.get(baseId);
        if (part == null) {
            return;
        }
        if (part.getGradientSet() != null && (gradientSet = (PlayerSkinGradientSet)(reg = CosmeticsModule.get().getRegistry()).getGradientSets().get(part.getGradientSet())) != null && gradientSet.getGradients() != null) {
            for (Map.Entry e : gradientSet.getGradients().entrySet()) {
                this.currentTextureKeys.add((String)e.getKey());
                this.currentTextureColors.add(this.texColorToHex((String)e.getKey(), (PlayerSkinPartTexture)e.getValue()));
            }
        }
        String variantKey = CitizenSkinCustomizerPage.extractVariantKey(selectedCompoundOrBareId);
        Map textures = part.getTextures();
        if (variantKey != null && part.getVariants() != null && (variant = (PlayerSkinPart.Variant)part.getVariants().get(variantKey)) != null && variant.getTextures() != null) {
            textures = variant.getTextures();
        }
        if (textures != null) {
            for (Map.Entry e : textures.entrySet()) {
                if (this.currentTextureKeys.contains(e.getKey())) continue;
                this.currentTextureKeys.add((String)e.getKey());
                this.currentTextureColors.add(this.texColorToHex((String)e.getKey(), (PlayerSkinPartTexture)e.getValue()));
            }
        }
    }

    private Map<String, PlayerSkinPart> getRegistryParts(CosmeticRegistry registry, int catIdx) {
        return switch (catIdx) {
            case 0 -> registry.getBodyCharacteristics();
            case 1 -> registry.getUnderwear();
            case 2 -> registry.getFaces();
            case 3 -> registry.getEyes();
            case 4 -> registry.getEars();
            case 5 -> registry.getMouths();
            case 6 -> registry.getFacialHairs();
            case 7 -> registry.getHaircuts();
            case 8 -> registry.getEyebrows();
            case 9 -> registry.getPants();
            case 10 -> registry.getOverpants();
            case 11 -> registry.getUndertops();
            case 12 -> registry.getOvertops();
            case 13 -> registry.getShoes();
            case 14 -> registry.getHeadAccessories();
            case 15 -> registry.getFaceAccessories();
            case 16 -> registry.getEarAccessories();
            case 17 -> registry.getSkinFeatures();
            case 18 -> registry.getGloves();
            case 19 -> registry.getCapes();
            default -> Collections.emptyMap();
        };
    }

    private String getSkinField(PlayerSkin skin, int catIdx) {
        return switch (catIdx) {
            case 0 -> skin.bodyCharacteristic;
            case 1 -> skin.underwear;
            case 2 -> skin.face;
            case 3 -> skin.eyes;
            case 4 -> skin.ears;
            case 5 -> skin.mouth;
            case 6 -> skin.facialHair;
            case 7 -> skin.haircut;
            case 8 -> skin.eyebrows;
            case 9 -> skin.pants;
            case 10 -> skin.overpants;
            case 11 -> skin.undertop;
            case 12 -> skin.overtop;
            case 13 -> skin.shoes;
            case 14 -> skin.headAccessory;
            case 15 -> skin.faceAccessory;
            case 16 -> skin.earAccessory;
            case 17 -> skin.skinFeature;
            case 18 -> skin.gloves;
            case 19 -> skin.cape;
            default -> null;
        };
    }

    private void setSkinField(PlayerSkin skin, int catIdx, String value) {
        switch (catIdx) {
            case 0: {
                skin.bodyCharacteristic = value;
                break;
            }
            case 1: {
                skin.underwear = value;
                break;
            }
            case 2: {
                skin.face = value;
                break;
            }
            case 3: {
                skin.eyes = value;
                break;
            }
            case 4: {
                skin.ears = value;
                break;
            }
            case 5: {
                skin.mouth = value;
                break;
            }
            case 6: {
                skin.facialHair = value;
                break;
            }
            case 7: {
                skin.haircut = value;
                break;
            }
            case 8: {
                skin.eyebrows = value;
                break;
            }
            case 9: {
                skin.pants = value;
                break;
            }
            case 10: {
                skin.overpants = value;
                break;
            }
            case 11: {
                skin.undertop = value;
                break;
            }
            case 12: {
                skin.overtop = value;
                break;
            }
            case 13: {
                skin.shoes = value;
                break;
            }
            case 14: {
                skin.headAccessory = value;
                break;
            }
            case 15: {
                skin.faceAccessory = value;
                break;
            }
            case 16: {
                skin.earAccessory = value;
                break;
            }
            case 17: {
                skin.skinFeature = value;
                break;
            }
            case 18: {
                skin.gloves = value;
                break;
            }
            case 19: {
                skin.cape = value;
            }
        }
    }

    private static String extractBaseId(String compoundValue) {
        if (compoundValue == null || compoundValue.isEmpty()) {
            return compoundValue;
        }
        int dot = compoundValue.indexOf(46);
        return dot > 0 ? compoundValue.substring(0, dot) : compoundValue;
    }

    private static String extractVariantKey(String compoundValue) {
        if (compoundValue == null) {
            return null;
        }
        String[] parts = compoundValue.split("\\.");
        return parts.length >= 3 ? parts[2] : null;
    }

    private static String replaceTextureKey(String compoundValue, String newTextureKey) {
        if (compoundValue == null) {
            return null;
        }
        String[] parts = compoundValue.split("\\.");
        if (parts.length >= 3) {
            return parts[0] + "." + newTextureKey + "." + parts[2];
        }
        if (parts.length == 2) {
            return parts[0] + "." + newTextureKey;
        }
        return compoundValue;
    }

    private String buildCompoundValue(String partId, PlayerSkinPart part) {
        CosmeticRegistry reg;
        PlayerSkinGradientSet gradientSet;
        Map textureMap = part.getTextures();
        String variantKey = null;
        if (part.getVariants() != null && !part.getVariants().isEmpty()) {
            for (Map.Entry entry : part.getVariants().entrySet()) {
                PlayerSkinPart.Variant variant = (PlayerSkinPart.Variant)entry.getValue();
                if (variant == null || variant.getTextures() == null || variant.getTextures().isEmpty()) continue;
                variantKey = (String)entry.getKey();
                textureMap = variant.getTextures();
                break;
            }
            if (variantKey == null) {
                variantKey = (String)part.getVariants().keySet().iterator().next();
            }
        }
        String textureKey = null;
        if (part.getGradientSet() != null && (gradientSet = (PlayerSkinGradientSet)(reg = CosmeticsModule.get().getRegistry()).getGradientSets().get(part.getGradientSet())) != null && gradientSet.getGradients() != null && !gradientSet.getGradients().isEmpty()) {
            textureKey = (String)gradientSet.getGradients().keySet().iterator().next();
        }
        if (textureKey == null && textureMap != null && !textureMap.isEmpty()) {
            textureKey = (String)textureMap.keySet().iterator().next();
        }
        if (textureKey == null) {
            return null;
        }
        if (variantKey != null) {
            return partId + "." + textureKey + "." + variantKey;
        }
        return partId + "." + textureKey;
    }

    private String getCompoundColor(String compoundValue, PlayerSkinPart part) {
        try {
            PlayerSkinPartTexture tex;
            PlayerSkinPartTexture tex2;
            PlayerSkinPart.Variant variant;
            PlayerSkinPartTexture tex3;
            CosmeticRegistry reg;
            PlayerSkinGradientSet gradientSet;
            if (compoundValue == null || part == null) {
                return "#888888";
            }
            String[] segments = compoundValue.split("\\.");
            if (segments.length < 2) {
                if (part.getTextures() != null && !part.getTextures().isEmpty()) {
                    Map.Entry first = part.getTextures().entrySet().iterator().next();
                    return this.texColorToHex((String)first.getKey(), (PlayerSkinPartTexture)first.getValue());
                }
                return "#888888";
            }
            String textureKey = segments[1];
            if (part.getGradientSet() != null && (gradientSet = (PlayerSkinGradientSet)(reg = CosmeticsModule.get().getRegistry()).getGradientSets().get(part.getGradientSet())) != null && gradientSet.getGradients() != null && (tex3 = (PlayerSkinPartTexture)gradientSet.getGradients().get(textureKey)) != null) {
                return this.texColorToHex(textureKey, tex3);
            }
            if (segments.length >= 3 && part.getVariants() != null && (variant = (PlayerSkinPart.Variant)part.getVariants().get(segments[2])) != null && variant.getTextures() != null && (tex2 = (PlayerSkinPartTexture)variant.getTextures().get(textureKey)) != null) {
                return this.texColorToHex(textureKey, tex2);
            }
            if (part.getTextures() != null && (tex = (PlayerSkinPartTexture)part.getTextures().get(textureKey)) != null) {
                return this.texColorToHex(textureKey, tex);
            }
            return this.texColorToHex(textureKey, null);
        }
        catch (Exception e) {
            return "#888888";
        }
    }

    private String texColorToHex(String keyHint, PlayerSkinPartTexture tex) {
        try {
            String[] baseColor;
            if (tex != null && (baseColor = tex.getBaseColor()) != null && baseColor.length >= 1) {
                if (baseColor[0].startsWith("#")) {
                    return baseColor[0];
                }
                if (baseColor.length >= 3) {
                    int r = Math.max(0, Math.min(255, (int)(Float.parseFloat(baseColor[0]) * 255.0f)));
                    int g = Math.max(0, Math.min(255, (int)(Float.parseFloat(baseColor[1]) * 255.0f)));
                    int b = Math.max(0, Math.min(255, (int)(Float.parseFloat(baseColor[2]) * 255.0f)));
                    return String.format("#%02x%02x%02x", r, g, b);
                }
            }
            if (keyHint != null && !keyHint.isEmpty()) {
                int hash = keyHint.hashCode();
                int r = 80 + Math.abs(hash >> 16 & 0xFF) % 160;
                int g = 80 + Math.abs(hash >> 8 & 0xFF) % 160;
                int b = 80 + Math.abs(hash & 0xFF) % 160;
                return String.format("#%02x%02x%02x", r, g, b);
            }
            return "#888888";
        }
        catch (Exception e) {
            return "#888888";
        }
    }

    public static class SkinPageData {
        public static final BuilderCodec<SkinPageData> CODEC = ((BuilderCodec.Builder)BuilderCodec.builder(SkinPageData.class, SkinPageData::new).addField(new KeyedCodec("Button", (Codec)Codec.STRING), (d, v) -> {
            d.button = v;
        }, d -> d.button)).build();
        String button;
    }
}

