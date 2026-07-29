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
 *  com.hypixel.hytale.math.vector.Transform
 *  com.hypixel.hytale.math.vector.Vector3d
 *  com.hypixel.hytale.math.vector.Vector3f
 *  com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
 *  com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset
 *  com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem
 *  com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry
 *  com.hypixel.hytale.server.core.cosmetics.CosmeticsModule
 *  com.hypixel.hytale.server.core.cosmetics.Emote
 *  com.hypixel.hytale.server.core.cosmetics.PlayerSkinGradientSet
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
 *  com.hypixel.hytale.server.core.inventory.InventoryComponent$Armor
 *  com.hypixel.hytale.server.core.inventory.InventoryComponent$Hotbar
 *  com.hypixel.hytale.server.core.inventory.InventoryComponent$Utility
 *  com.hypixel.hytale.server.core.inventory.ItemStack
 *  com.hypixel.hytale.server.core.inventory.container.ItemContainer
 *  com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
 *  com.hypixel.hytale.server.core.ui.DropdownEntryInfo
 *  com.hypixel.hytale.server.core.ui.LocalizableString
 *  com.hypixel.hytale.server.core.ui.builder.EventData
 *  com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
 *  com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.Universe
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 *  com.hypixel.hytale.server.npc.entities.NPCEntity
 *  com.hypixel.hytale.server.npc.movement.controllers.MotionController
 *  com.hypixel.hytale.server.npc.role.Role
 *  com.hypixel.hytale.server.npc.role.support.StateSupport
 *  javax.annotation.Nonnull
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem;
import com.hypixel.hytale.server.core.cosmetics.CosmeticRegistry;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.cosmetics.Emote;
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinGradientSet;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.movement.controllers.MotionController;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.StateSupport;
import com.kyuubisoft.core.CorePlugin;
import com.kyuubisoft.core.api.CoreAPI;
import com.kyuubisoft.core.citizen.CitizenAnimationManager;
import com.kyuubisoft.core.citizen.CitizenData;
import com.kyuubisoft.core.citizen.CitizenService;
import com.kyuubisoft.core.citizen.CitizenSkinCustomizerPage;
import com.kyuubisoft.core.dialog.DialogEditorPage;
import com.kyuubisoft.core.i18n.CoreI18n;
import com.kyuubisoft.core.i18n.I18nContext;
import com.kyuubisoft.core.registry.ModMenuRegistry;
import com.kyuubisoft.core.util.CommandUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class CitizenAdminPage
extends InteractiveCustomUIPage<PageEventData> {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Citizen Admin");
    private static final int CITIZENS_PER_PAGE = 16;
    private static final int MAX_ANIMATIONS = 8;
    private static final int MAX_MESSAGES = 8;
    private static final int MAX_COMMANDS = 8;
    private static final List<BehaviorPreset> PRESETS = List.of(new BehaviorPreset("citizen.admin.preset.custom", null, null, null, 0.0f, 0.0f), new BehaviorPreset("citizen.admin.preset.idle_interact", "KS_NPC_Interactable_Role", "PASSIVE", "IDLE", 0.0f, 0.0f), new BehaviorPreset("citizen.admin.preset.idle_silent", "Empty_Role", "PASSIVE", "IDLE", 0.0f, 0.0f), new BehaviorPreset("citizen.admin.preset.wander_r2", "KS_NPC_WanderI_R2_Interactable_Role", "PASSIVE", "WANDER", 2.0f, 0.3f), new BehaviorPreset("citizen.admin.preset.wander_r5", "KS_NPC_WanderI_R5_Interactable_Role", "PASSIVE", "WANDER", 5.0f, 0.4f), new BehaviorPreset("citizen.admin.preset.wander_r10", "KS_NPC_WanderI_R10_Interactable_Role", "PASSIVE", "WANDER", 10.0f, 0.5f), new BehaviorPreset("citizen.admin.preset.wander_r15", "KS_NPC_WanderI_R15_Interactable_Role", "PASSIVE", "WANDER", 15.0f, 0.5f), new BehaviorPreset("citizen.admin.preset.guard_neutral", "KS_NPC_Interactable_Role", "PASSIVE", "IDLE", 0.0f, 0.0f), new BehaviorPreset("citizen.admin.preset.patrol_r5", null, "NEUTRAL", "PATH", 5.0f, 0.4f), new BehaviorPreset("citizen.admin.preset.aggressive_r5", "KS_NPC_WanderI_R5_Interactable_Role", "AGGRESSIVE", "WANDER", 5.0f, 0.6f));
    private final CorePlugin plugin;
    private final Player player;
    private final PlayerRef playerRef;
    private final CitizenService citizenService;
    private Ref<EntityStore> storedRef;
    private Store<EntityStore> storedStore;
    private int listPage = 0;
    private String groupFilter = "all";
    private String searchQuery = "";
    private String selectedCitizenId = null;
    private List<CitizenData> filteredCitizens = new ArrayList<CitizenData>();
    private boolean showingDeleteConfirm = false;
    private boolean dropdownsInitialized = false;
    private boolean editDropdownsInitialized = false;
    private String currentTab = "general";
    private int editingAnimIndex = -2;
    private int editingCmdIndex = -2;
    private int editingMsgIndex = -2;
    private static final String[] LOC_LANGUAGES = new String[]{"en-US", "de-DE", "fr-FR", "es-ES", "pt-BR", "ru-RU", "pl-PL", "tr-TR", "it-IT"};
    private boolean locEditorOpen = false;
    private final Map<String, String> locNameValues = new LinkedHashMap<String, String>();
    private Map<String, String> animSlotHints = new HashMap<String, String>();
    private List<String> allAnimNames = new ArrayList<String>();
    private List<String> allModelAnimNames = new ArrayList<String>();
    private List<String> allEmoteAnimNames = new ArrayList<String>();
    private String currentSlotFilter = null;
    private String autoDetectedSlot = null;
    private String currentAnimName = null;

    public CitizenAdminPage(CorePlugin plugin, Player player, PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageEventData.CODEC);
        this.plugin = plugin;
        this.player = player;
        this.playerRef = playerRef;
        this.citizenService = plugin.getCitizenService();
        this.filterCitizens();
    }

    public void setSelectedCitizenId(String id) {
        this.selectedCitizenId = id;
        this.filterCitizens();
    }

    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder ui, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        I18nContext.run(this.playerRef, () -> {
            this.storedRef = ref;
            this.storedStore = store;
            ui.append("Pages/CitizenAdmin/AdminPanel.ui");
            this.setI18nLabels(ui);
            this.bindEvents(events);
            this.buildListPanel(ui);
            this.buildDetailPanel(ui);
            this.buildDeleteConfirm(ui);
            this.buildOverlays(ui);
        });
    }

    private void setI18nLabels(UICommandBuilder ui) {
        CoreI18n i18n = CoreI18n.getInstance();
        ui.set("#CitizensHeaderLabel.Text", i18n.get("citizen.admin.citizens_header"));
        ui.set("#CreateHereLabel.Text", i18n.get("citizen.admin.btn.create_here"));
        ui.set("#EmptyDetailLabel.Text", i18n.get("citizen.admin.no_selection"));
        ui.set("#CloneLabel.Text", i18n.get("citizen.admin.btn.clone"));
        ui.set("#SectionIdentity.Text", i18n.get("citizen.admin.section.identity"));
        ui.set("#MyPosLabel.Text", i18n.get("citizen.admin.btn.my_pos"));
        ui.set("#SectionAppearance.Text", i18n.get("citizen.admin.section.appearance"));
        ui.set("#MySkinLabel.Text", i18n.get("citizen.admin.btn.my_skin"));
        ui.set("#SectionInteraction.Text", i18n.get("citizen.admin.section.interaction"));
        ui.set("#SectionBehavior.Text", i18n.get("citizen.admin.section.behavior"));
        ui.set("#SectionEquipment.Text", i18n.get("citizen.admin.section.equipment"));
        ui.set("#FromPlayerLabel.Text", i18n.get("citizen.admin.btn.from_player"));
        ui.set("#SectionAssignment.Text", i18n.get("citizen.admin.section.assignment"));
        ui.set("#AddAnimLabel.Text", i18n.get("citizen.admin.btn.add_anim"));
        ui.set("#SectionNpcState.Text", i18n.get("citizen.admin.section.npc_state"));
        ui.set("#StateRefreshLabel.Text", i18n.get("citizen.admin.btn.refresh"));
        ui.set("#SectionRoleInfo.Text", i18n.get("citizen.admin.section.role_info"));
        ui.set("#PresetApplyLabel.Text", i18n.get("citizen.admin.btn.apply_preset"));
        ui.set("#CmdPlaceholderHint.Text", i18n.get("citizen.admin.cmd_placeholder"));
        ui.set("#MsgPlaceholderHint.Text", i18n.get("citizen.admin.msg_placeholder"));
        ui.set("#SectionActions.Text", i18n.get("citizen.admin.section.actions"));
        ui.set("#SaveLabel.Text", i18n.get("citizen.admin.btn.save"));
        ui.set("#MoveToMeLabel.Text", i18n.get("citizen.admin.btn.move_to_me"));
        ui.set("#TeleportLabel.Text", i18n.get("citizen.admin.btn.teleport"));
        ui.set("#RespawnLabel.Text", i18n.get("citizen.admin.btn.respawn"));
        ui.set("#DespawnLabel.Text", i18n.get("citizen.admin.btn.despawn"));
        ui.set("#DeleteLabel.Text", i18n.get("citizen.admin.btn.delete"));
        ui.set("#DeleteAlsoDespawnLabel.Text", i18n.get("citizen.admin.delete_also_despawn"));
        ui.set("#DeleteYesLabel.Text", i18n.get("citizen.admin.btn.delete"));
        ui.set("#DeleteNoLabel.Text", i18n.get("citizen.admin.btn.cancel"));
        ui.set("#AnimEditTitle.Text", i18n.get("citizen.admin.edit_animation"));
        ui.set("#CmdEditTitle.Text", i18n.get("citizen.admin.edit_command"));
        ui.set("#MsgEditTitle.Text", i18n.get("citizen.admin.edit_message"));
        ui.set("#AnimEditSaveLabel.Text", i18n.get("citizen.admin.btn.save"));
        ui.set("#AnimEditCancelLabel.Text", i18n.get("citizen.admin.btn.cancel"));
        ui.set("#CmdEditSaveLabel.Text", i18n.get("citizen.admin.btn.save"));
        ui.set("#CmdEditCancelLabel.Text", i18n.get("citizen.admin.btn.cancel"));
        ui.set("#MsgEditSaveLabel.Text", i18n.get("citizen.admin.btn.save"));
        ui.set("#MsgEditCancelLabel.Text", i18n.get("citizen.admin.btn.cancel"));
    }

    private void bindEvents(UIEventBuilder events) {
        int i;
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of((String)"Button", (String)"back"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of((String)"Button", (String)"close"), false);
        for (int i2 = 0; i2 < 16; ++i2) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#CitEntry" + i2, EventData.of((String)"Button", (String)("select_" + i2)), false);
        }
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CitPrevButton", EventData.of((String)"Button", (String)"citPrev"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CitNextButton", EventData.of((String)"Button", (String)"citNext"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CreateHereButton", EventData.of((String)"Button", (String)"createHere"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloneButton", EventData.of((String)"Button", (String)"clone"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#UseMySkinBtn", EventData.of((String)"Button", (String)"useMySkin"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SetItemsBtn", EventData.of((String)"Button", (String)"setItems"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#UseMyPosBtn", EventData.of((String)"Button", (String)"useMyPos"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabGenBtn", EventData.of((String)"Button", (String)"tabGeneral"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabAppBtn", EventData.of((String)"Button", (String)"tabAppearance"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabBhvBtn", EventData.of((String)"Button", (String)"tabBehavior"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabIntBtn", EventData.of((String)"Button", (String)"tabInteraction"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabCbtBtn", EventData.of((String)"Button", (String)"tabCombat"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabDspBtn", EventData.of((String)"Button", (String)"tabDisplay"), false);
        EventData saveData = new EventData();
        saveData.append("Button", "save");
        saveData.append("@EditId", "#CitDetailId.Value");
        saveData.append("@EditName", "#EditName.Value");
        saveData.append("@EditGroup", "#EditGroup.Value");
        saveData.append("@EditWorld", "#EditWorld.Value");
        saveData.append("@EditPosX", "#EditPosX.Value");
        saveData.append("@EditPosY", "#EditPosY.Value");
        saveData.append("@EditPosZ", "#EditPosZ.Value");
        saveData.append("@EditSkin", "#EditSkin.Value");
        saveData.append("@EditScale", "#EditScale.Value");
        saveData.append("@EditNametagOff", "#EditNametagOff.Value");
        saveData.append("@EditPermission", "#EditPermission.Value");
        saveData.append("@EditNoPermMsg", "#EditNoPermMsg.Value");
        saveData.append("@EditRespawnDelay", "#EditRespawnDelay.Value");
        saveData.append("@EditMovRadius", "#EditMovRadius.Value");
        saveData.append("@EditWalkSpeed", "#EditWalkSpeed.Value");
        saveData.append("@EditHelmet", "#EditHelmet.Value");
        saveData.append("@EditChest", "#EditChest.Value");
        saveData.append("@EditLeggings", "#EditLeggings.Value");
        saveData.append("@EditGloves", "#EditGloves.Value");
        saveData.append("@EditMainHand", "#EditMainHand.Value");
        saveData.append("@EditOffHand", "#EditOffHand.Value");
        saveData.append("@EditDialog", "#EditDialog.Value");
        saveData.append("@EditShop", "#EditShop.Value");
        saveData.append("@EditMsgDelay", "#EditMsgDelay.Value");
        saveData.append("@EditSpawnFx", "#EditSpawnFx.Value");
        saveData.append("@EditSpawnFxDur", "#EditSpawnFxDur.Value");
        saveData.append("@EditDespawnFx", "#EditDespawnFx.Value");
        saveData.append("@EditDespawnFxDur", "#EditDespawnFxDur.Value");
        saveData.append("@EditAttachments", "#EditAttachments.Value");
        saveData.append("@EditGradientSet", "#EditGradientSet.Value");
        saveData.append("@EditGradientId", "#EditGradientId.Value");
        saveData.append("@EditNametagColor", "#EditNametagColor.Value");
        saveData.append("@EditNametagFmt", "#EditNametagFmt.Value");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton", saveData, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#MoveToMeButton", EventData.of((String)"Button", (String)"moveToMe"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TeleportButton", EventData.of((String)"Button", (String)"teleport"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#RespawnButton", EventData.of((String)"Button", (String)"respawn"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DespawnButton", EventData.of((String)"Button", (String)"despawn"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteButton", EventData.of((String)"Button", (String)"delete"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteYesBtn", EventData.of((String)"Button", (String)"deleteYes"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteNoBtn", EventData.of((String)"Button", (String)"deleteNo"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#EditDlgBtn", EventData.of((String)"Button", (String)"openDlgEditor"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#QuestProfileBtn", EventData.of((String)"Button", (String)"openQuestProfile"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AnimAddBtn", EventData.of((String)"Button", (String)"animAdd"), false);
        for (int i3 = 0; i3 < 8; ++i3) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#AnimEdit" + i3, EventData.of((String)"Button", (String)("animEdit_" + i3)), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#AnimDel" + i3, EventData.of((String)"Button", (String)("animDel_" + i3)), false);
        }
        EventData animEditSave = new EventData();
        animEditSave.append("Button", "animEditSave");
        animEditSave.append("@AnimEditTrigger", "#AnimEditTriggerDD.Value");
        animEditSave.append("@AnimEditName", "#AnimEditNameDD.Value");
        animEditSave.append("@AnimEditSlot", "#AnimEditSlotDD.Value");
        animEditSave.append("@AnimEditInterval", "#AnimEditIntervalField.Value");
        animEditSave.append("@AnimEditProxRange", "#AnimEditProxRangeField.Value");
        animEditSave.append("@AnimEditStopEnabled", "#AnimEditStopDD.Value");
        animEditSave.append("@AnimEditStopTime", "#AnimEditStopTimeField.Value");
        animEditSave.append("@AnimEditStopAnim", "#AnimEditStopAnimDD.Value");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AnimEditSaveBtn", animEditSave, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AnimEditCancelBtn", EventData.of((String)"Button", (String)"animEditCancel"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#AnimEditNameDD", EventData.of((String)"@AnimNameChanged", (String)"#AnimEditNameDD.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#AnimEditSlotDD", EventData.of((String)"@SlotChanged", (String)"#AnimEditSlotDD.Value"));
        for (int i4 = 0; i4 < 8; ++i4) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#CmdEdit" + i4, EventData.of((String)"Button", (String)("cmdEdit_" + i4)), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#CmdDel" + i4, EventData.of((String)"Button", (String)("cmdDel_" + i4)), false);
        }
        EventData cmdAddData = new EventData();
        cmdAddData.append("Button", "cmdAdd");
        cmdAddData.append("@CmdNew", "#CmdNewField.Value");
        cmdAddData.append("@CmdRunMode", "#CmdRunMode.Value");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CmdAddBtn", cmdAddData, false);
        EventData cmdEditSave = new EventData();
        cmdEditSave.append("Button", "cmdEditSave");
        cmdEditSave.append("@CmdEditText", "#CmdEditOverlayField.Value");
        cmdEditSave.append("@CmdEditMode", "#CmdEditRunMode.Value");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CmdEditSaveBtn", cmdEditSave, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CmdEditCancelBtn", EventData.of((String)"Button", (String)"cmdEditCancel"), false);
        EventData msgAddData = new EventData();
        msgAddData.append("Button", "msgAdd");
        msgAddData.append("@MsgNew", "#MsgNewField.Value");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#MsgAddBtn", msgAddData, false);
        for (int i5 = 0; i5 < 8; ++i5) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#MsgEdit" + i5, EventData.of((String)"Button", (String)("msgEdit_" + i5)), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#MsgDel" + i5, EventData.of((String)"Button", (String)("msgDel_" + i5)), false);
        }
        EventData msgEditSave = new EventData();
        msgEditSave.append("Button", "msgEditSave");
        msgEditSave.append("@MsgEditText", "#MsgEditOverlayField.Value");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#MsgEditSaveBtn", msgEditSave, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#MsgEditCancelBtn", EventData.of((String)"Button", (String)"msgEditCancel"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#WpAddBtn", EventData.of((String)"Button", (String)"wpAdd"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#WpRemoveBtn", EventData.of((String)"Button", (String)"wpRemove"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#WpClearBtn", EventData.of((String)"Button", (String)"wpClear"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#WpRecordBtn", EventData.of((String)"Button", (String)"wpRecord"), false);
        EventData presetApplyData = new EventData();
        presetApplyData.append("Button", "presetApply");
        presetApplyData.append("@Preset", "#EditPreset.Value");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#PresetApplyBtn", presetApplyData, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#StateRefreshBtn", EventData.of((String)"Button", (String)"stateRefresh"), false);
        EventData deathCmdAddData = new EventData();
        deathCmdAddData.append("Button", "deathCmdAdd");
        deathCmdAddData.append("@DeathCmdNew", "#DeathCmdNewField.Value");
        deathCmdAddData.append("@DeathCmdRunMode", "#DeathCmdRunMode.Value");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DeathCmdAddBtn", deathCmdAddData, false);
        for (i = 0; i < 4; ++i) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#DeathCmdDel" + i, EventData.of((String)"Button", (String)("deathCmdDel_" + i)), false);
        }
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditPathShape", EventData.of((String)"@PathShape", (String)"#EditPathShape.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#GroupFilterDropdown", EventData.of((String)"@GroupFilter", (String)"#GroupFilterDropdown.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditModelType", EventData.of((String)"@ModelType", (String)"#EditModelType.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditEntityType", EventData.of((String)"@EntityType", (String)"#EditEntityType.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditAttitude", EventData.of((String)"@Attitude", (String)"#EditAttitude.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditMovement", EventData.of((String)"@Movement", (String)"#EditMovement.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditRotate", EventData.of((String)"@Rotate", (String)"#EditRotate.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditInvulnerable", EventData.of((String)"@Invulnerable", (String)"#EditInvulnerable.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditTakesDamage", EventData.of((String)"@TakesDamage", (String)"#EditTakesDamage.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditRespawn", EventData.of((String)"@Respawn", (String)"#EditRespawn.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditFKey", EventData.of((String)"@FKey", (String)"#EditFKey.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditHideNpc", EventData.of((String)"@HideNpc", (String)"#EditHideNpc.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditBankingEnabled", EventData.of((String)"@BankingEnabled", (String)"#EditBankingEnabled.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditHideNametag", EventData.of((String)"@HideNametag", (String)"#EditHideNametag.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditLiveSkin", EventData.of((String)"@LiveSkin", (String)"#EditLiveSkin.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditMsgMode", EventData.of((String)"@MsgMode", (String)"#EditMsgMode.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditSpawnRelative", EventData.of((String)"@SpawnRelative", (String)"#EditSpawnRelative.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditNpcRole", EventData.of((String)"@NpcRole", (String)"#EditNpcRole.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#EditGradientSet", EventData.of((String)"@GradSetChanged", (String)"#EditGradientSet.Value"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#EditSkinBtn", EventData.of((String)"Button", (String)"editSkin"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#CitizenSearchField", EventData.of((String)"@Search", (String)"#CitizenSearchField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CitLocEditNameBtn", EventData.of((String)"Button", (String)"locEditName"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#LocEdDoneBtn", EventData.of((String)"Button", (String)"locEdDone"), false);
        for (i = 0; i < LOC_LANGUAGES.length; ++i) {
            events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#LocEdField" + i, EventData.of((String)("@LocEdField" + i), (String)("#LocEdField" + i + ".Value")), false);
        }
    }

    private void refreshUI() {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        this.setI18nLabels(ui);
        this.bindEvents(events);
        this.buildListPanel(ui);
        this.buildDetailPanel(ui);
        this.buildDeleteConfirm(ui);
        this.buildOverlays(ui);
        this.buildLocEditorOverlay(ui);
        this.sendUpdate(ui, events, false);
    }

    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageEventData data) {
        I18nContext.run(this.playerRef, () -> {
            CitizenData citizen;
            super.handleDataEvent(ref, store, (Object)data);
            this.storedRef = ref;
            this.storedStore = store;
            if (data.button != null) {
                if ("save".equals(data.button)) {
                    if (CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) {
                        this.sendUpdate(new UICommandBuilder(), false);
                        return;
                    }
                    this.handleSave(data);
                } else if ("msgAdd".equals(data.button)) {
                    if (CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) {
                        this.sendUpdate(new UICommandBuilder(), false);
                        return;
                    }
                    this.handleMessageAdd(data.msgNew);
                } else if ("cmdAdd".equals(data.button)) {
                    if (CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) {
                        this.sendUpdate(new UICommandBuilder(), false);
                        return;
                    }
                    this.handleCommandAdd(data.cmdNew, data.cmdRunMode);
                } else if ("animEditSave".equals(data.button)) {
                    if (CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) {
                        this.sendUpdate(new UICommandBuilder(), false);
                        return;
                    }
                    this.handleAnimEditSave(data);
                    this.refreshUI();
                } else if ("presetApply".equals(data.button)) {
                    if (CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) {
                        this.sendUpdate(new UICommandBuilder(), false);
                        return;
                    }
                    this.handlePresetApply(data.preset);
                    this.refreshUI();
                } else if ("deathCmdAdd".equals(data.button)) {
                    if (CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) {
                        this.sendUpdate(new UICommandBuilder(), false);
                        return;
                    }
                    this.handleDeathCommandAdd(data.deathCmdNew, data.deathCmdRunMode);
                } else if ("cmdEditSave".equals(data.button)) {
                    if (CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) {
                        this.sendUpdate(new UICommandBuilder(), false);
                        return;
                    }
                    this.handleCommandEditSave(data.cmdEditText, data.cmdEditMode);
                    this.refreshUI();
                } else if ("msgEditSave".equals(data.button)) {
                    if (CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) {
                        this.sendUpdate(new UICommandBuilder(), false);
                        return;
                    }
                    this.handleMessageEditSave(data.msgEditText);
                    this.refreshUI();
                } else {
                    this.handleButton(data.button);
                }
                return;
            }
            if (data.groupFilter != null) {
                if (!data.groupFilter.equals(this.groupFilter)) {
                    this.groupFilter = data.groupFilter;
                    this.listPage = 0;
                    this.filterCitizens();
                }
                this.refreshUI();
                return;
            }
            if (data.search != null) {
                this.searchQuery = data.search;
                this.listPage = 0;
                this.filterCitizens();
                this.refreshUI();
                return;
            }
            if (data.entityType != null) {
                CitizenData citizen2;
                if (CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) {
                    this.sendUpdate(new UICommandBuilder(), false);
                    return;
                }
                CitizenData citizenData = citizen2 = this.selectedCitizenId != null ? this.citizenService.getCitizen(this.selectedCitizenId) : null;
                if (citizen2 != null) {
                    citizen2.entityTypeId = data.entityType;
                }
                this.refreshUI();
                return;
            }
            if (data.animNameChanged != null) {
                this.currentAnimName = data.animNameChanged;
                String hint = this.animSlotHints.get(data.animNameChanged);
                if ("Emote".equals(hint)) {
                    this.autoDetectedSlot = hint;
                    this.currentSlotFilter = hint;
                }
                this.refreshUI();
                return;
            }
            if (data.slotChanged != null) {
                this.currentSlotFilter = data.slotChanged;
                this.refreshUI();
                return;
            }
            if (data.gradSetChanged != null) {
                CitizenData gradCitizen;
                if (CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) {
                    this.sendUpdate(new UICommandBuilder(), false);
                    return;
                }
                CitizenData citizenData = gradCitizen = this.selectedCitizenId != null ? this.citizenService.getCitizen(this.selectedCitizenId) : null;
                if (gradCitizen != null) {
                    gradCitizen.gradientSet = CitizenAdminPage.emptyToNull(data.gradSetChanged);
                    gradCitizen.gradientId = null;
                }
                this.editDropdownsInitialized = false;
                this.refreshUI();
                return;
            }
            CitizenData citizenData = citizen = this.selectedCitizenId != null ? this.citizenService.getCitizen(this.selectedCitizenId) : null;
            if (citizen != null) {
                boolean hasWriteDropdown;
                boolean bl = hasWriteDropdown = data.modelType != null || data.attitude != null || data.movement != null || data.rotate != null || data.invulnerable != null || data.takesDamage != null || data.respawn != null || data.fKey != null || data.hideNpc != null || data.bankingEnabled != null || data.hideNametag != null || data.liveSkin != null || data.msgMode != null || data.spawnRelative != null || data.npcRole != null || data.pathShape != null;
                if (hasWriteDropdown && CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) {
                    this.sendUpdate(new UICommandBuilder(), false);
                    return;
                }
                boolean changed = false;
                if (data.modelType != null) {
                    boolean isPlayer;
                    citizen.isPlayerModel = isPlayer = "Player Model".equals(data.modelType);
                    if (isPlayer) {
                        citizen.entityTypeId = null;
                    }
                    changed = true;
                }
                if (data.attitude != null) {
                    citizen.attitude = data.attitude;
                    changed = true;
                }
                if (data.movement != null) {
                    citizen.movementType = data.movement;
                    changed = true;
                }
                if (data.rotate != null) {
                    citizen.rotateTowardsPlayer = "Yes".equals(data.rotate);
                    changed = true;
                }
                if (data.invulnerable != null) {
                    citizen.invulnerable = "Yes".equals(data.invulnerable);
                    changed = true;
                }
                if (data.takesDamage != null) {
                    citizen.takesDamage = "Yes".equals(data.takesDamage);
                    changed = true;
                }
                if (data.respawn != null) {
                    citizen.respawnOnDeath = "Yes".equals(data.respawn);
                    changed = true;
                }
                if (data.fKey != null) {
                    citizen.fKeyInteractionEnabled = "Yes".equals(data.fKey);
                    changed = true;
                }
                if (data.hideNpc != null) {
                    citizen.hideNpc = "Yes".equals(data.hideNpc);
                    changed = true;
                }
                if (data.bankingEnabled != null) {
                    citizen.bankingEnabled = "Yes".equals(data.bankingEnabled);
                    changed = true;
                }
                if (data.hideNametag != null) {
                    citizen.hideNametag = "Yes".equals(data.hideNametag);
                    changed = true;
                }
                if (data.liveSkin != null) {
                    citizen.useLiveSkin = "Yes".equals(data.liveSkin);
                    changed = true;
                }
                if (data.msgMode != null) {
                    citizen.messageSelectionMode = data.msgMode;
                    changed = true;
                }
                if (data.spawnRelative != null) {
                    citizen.spawnRelative = "Yes".equals(data.spawnRelative);
                    changed = true;
                }
                if (data.npcRole != null) {
                    citizen.npcRoleId = "Auto".equals(data.npcRole) ? null : data.npcRole;
                    changed = true;
                }
                if (data.pathShape != null) {
                    citizen.pathShape = data.pathShape;
                    changed = true;
                }
                if (changed) {
                    boolean needsRespawn;
                    boolean bl2 = needsRespawn = data.movement != null || data.attitude != null || data.npcRole != null;
                    if (needsRespawn && citizen.spawnedEntityUUID != null) {
                        if ((data.movement != null || data.attitude != null) && data.npcRole == null) {
                            citizen.npcRoleId = null;
                        }
                        this.citizenService.updateCitizen(citizen);
                        World world = this.player.getWorld();
                        if (world != null) {
                            world.execute(() -> {
                                this.citizenService.despawnCitizen(citizen, world);
                                this.citizenService.spawnCitizen(citizen, world);
                            });
                        }
                        this.editDropdownsInitialized = false;
                    }
                    this.refreshUI();
                    return;
                }
            }
            if (this.locEditorOpen) {
                for (int i = 0; i < LOC_LANGUAGES.length; ++i) {
                    if (data.locEdFields[i] == null) continue;
                    this.locNameValues.put(LOC_LANGUAGES[i], data.locEdFields[i]);
                    this.sendUpdate(new UICommandBuilder(), false);
                    return;
                }
            }
            this.sendUpdate(new UICommandBuilder(), false);
        });
    }

    private void handleButton(String button) {
        switch (button) {
            case "createHere": 
            case "clone": 
            case "useMySkin": 
            case "setItems": 
            case "moveToMe": 
            case "useMyPos": 
            case "wpAdd": 
            case "wpRemove": 
            case "wpClear": 
            case "wpRecord": 
            case "teleport": 
            case "respawn": 
            case "despawn": 
            case "delete": 
            case "deleteYes": 
            case "editSkin": 
            case "animAdd": 
            case "locEdDone": {
                if (!CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) break;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            default: {
                if (!button.startsWith("animDel_") && !button.startsWith("cmdDel_") && !button.startsWith("deathCmdDel_") && !button.startsWith("msgDel_") || !CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) break;
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
            case "citPrev": {
                if (this.listPage <= 0) break;
                --this.listPage;
                break;
            }
            case "citNext": {
                int totalPages = Math.max(1, (int)Math.ceil((double)this.filteredCitizens.size() / 16.0));
                if (this.listPage >= totalPages - 1) break;
                ++this.listPage;
                break;
            }
            case "tabGeneral": {
                this.currentTab = "general";
                break;
            }
            case "tabAppearance": {
                this.currentTab = "appearance";
                break;
            }
            case "tabBehavior": {
                this.currentTab = "behavior";
                break;
            }
            case "tabInteraction": {
                this.currentTab = "interaction";
                break;
            }
            case "tabCombat": {
                this.currentTab = "combat";
                break;
            }
            case "tabDisplay": {
                this.currentTab = "display";
                break;
            }
            case "createHere": {
                this.handleCreateHere();
                break;
            }
            case "clone": {
                this.handleClone();
                break;
            }
            case "useMySkin": {
                this.handleUseMySkin();
                break;
            }
            case "setItems": {
                this.handleSetItems();
                break;
            }
            case "moveToMe": {
                this.handleMoveToMe();
                break;
            }
            case "useMyPos": {
                this.handleUseMyPos();
                break;
            }
            case "wpAdd": {
                this.handleWaypointAdd();
                break;
            }
            case "wpRemove": {
                this.handleWaypointRemoveLast();
                break;
            }
            case "wpClear": {
                this.handleWaypointClear();
                break;
            }
            case "wpRecord": {
                this.handleWaypointRecord();
                return;
            }
            case "teleport": {
                this.handleTeleport();
                break;
            }
            case "respawn": {
                this.handleRespawn();
                break;
            }
            case "despawn": {
                this.handleDespawn();
                break;
            }
            case "delete": {
                if (this.selectedCitizenId == null) break;
                this.showingDeleteConfirm = true;
                break;
            }
            case "deleteYes": {
                this.handleDeleteConfirm();
                break;
            }
            case "deleteNo": {
                this.showingDeleteConfirm = false;
                break;
            }
            case "openDlgEditor": {
                DialogEditorPage editorPage = new DialogEditorPage(this.plugin, this.player, this.playerRef);
                this.player.getPageManager().openCustomPage(this.storedRef, this.storedStore, (CustomUIPage)editorPage);
                return;
            }
            case "openQuestProfile": {
                if (this.selectedCitizenId == null || !ModMenuRegistry.hasNpcProfileEditor()) break;
                ModMenuRegistry.openNpcProfileEditor(this.player, this.playerRef, this.storedRef, this.storedStore, this.selectedCitizenId);
                return;
            }
            case "stateRefresh": {
                break;
            }
            case "editSkin": {
                CitizenData skinCitizen;
                if (this.selectedCitizenId == null || (skinCitizen = this.citizenService.getCitizen(this.selectedCitizenId)) == null) break;
                try {
                    CitizenSkinCustomizerPage skinPage = new CitizenSkinCustomizerPage(this.plugin, this.player, this.playerRef, skinCitizen);
                    this.player.getPageManager().openCustomPage(this.storedRef, this.storedStore, (CustomUIPage)skinPage);
                    return;
                }
                catch (Exception e) {
                    Logger.getLogger("KyuubiSoft Citizens").log(Level.SEVERE, "[SkinCustomizer] Failed to open", e);
                    break;
                }
            }
            case "animAdd": {
                this.editingAnimIndex = -1;
                this.currentSlotFilter = null;
                this.currentAnimName = null;
                break;
            }
            case "animEditCancel": {
                this.editingAnimIndex = -2;
                this.currentSlotFilter = null;
                this.currentAnimName = null;
                break;
            }
            case "cmdEditCancel": {
                this.editingCmdIndex = -2;
                break;
            }
            case "msgEditCancel": {
                this.editingMsgIndex = -2;
                break;
            }
            case "locEditName": {
                if (this.selectedCitizenId == null) break;
                this.loadLocNameValues();
                this.locEditorOpen = true;
                break;
            }
            case "locEdDone": {
                this.saveLocNameValues();
                this.locEditorOpen = false;
                break;
            }
            default: {
                if (button.startsWith("select_")) {
                    int actualIdx;
                    int idx = CitizenAdminPage.parseIntSafe(button.substring(7), -1);
                    if (idx < 0 || (actualIdx = this.listPage * 16 + idx) < 0 || actualIdx >= this.filteredCitizens.size()) break;
                    this.selectedCitizenId = this.filteredCitizens.get((int)actualIdx).id;
                    this.editDropdownsInitialized = false;
                    break;
                }
                if (button.startsWith("animEdit_")) {
                    this.editingAnimIndex = CitizenAdminPage.parseIntSafe(button.substring(9), -2);
                    this.currentSlotFilter = null;
                    this.currentAnimName = null;
                    break;
                }
                if (button.startsWith("animDel_")) {
                    this.handleAnimationDelete(CitizenAdminPage.parseIntSafe(button.substring(8), -1));
                    break;
                }
                if (button.startsWith("cmdEdit_")) {
                    this.editingCmdIndex = CitizenAdminPage.parseIntSafe(button.substring(8), -2);
                    break;
                }
                if (button.startsWith("cmdDel_")) {
                    this.handleCommandDelete(CitizenAdminPage.parseIntSafe(button.substring(7), -1));
                    break;
                }
                if (button.startsWith("deathCmdDel_")) {
                    this.handleDeathCommandDelete(CitizenAdminPage.parseIntSafe(button.substring(12), -1));
                    break;
                }
                if (button.startsWith("msgEdit_")) {
                    this.editingMsgIndex = CitizenAdminPage.parseIntSafe(button.substring(8), -2);
                    break;
                }
                if (!button.startsWith("msgDel_")) break;
                this.handleMessageDelete(CitizenAdminPage.parseIntSafe(button.substring(7), -1));
            }
        }
        this.refreshUI();
    }

    private void handleCreateHere() {
        World world;
        String newId = "citizen_" + (this.citizenService.getCitizenCount() + 1);
        while (this.citizenService.getCitizen(newId) != null) {
            newId = "citizen_" + System.currentTimeMillis() % 10000L;
        }
        CitizenData citizen = new CitizenData();
        citizen.id = newId;
        citizen.name = newId;
        try {
            TransformComponent transform = (TransformComponent)this.storedStore.getComponent(this.storedRef, TransformComponent.getComponentType());
            if (transform != null) {
                Vector3f rot;
                Vector3d pos = transform.getPosition();
                if (pos != null) {
                    citizen.posX = pos.x;
                    citizen.posY = pos.y;
                    citizen.posZ = pos.z;
                }
                if ((rot = transform.getRotation()) != null) {
                    citizen.rotY = rot.y;
                }
            }
        }
        catch (Exception e) {
            LOGGER.warning("Could not get player position: " + e.getMessage());
        }
        try {
            world = this.player.getWorld();
            if (world != null) {
                citizen.worldName = world.getName();
            }
        }
        catch (Exception e) {
            citizen.worldName = "world";
        }
        citizen.attitude = "PASSIVE";
        citizen.movementType = "IDLE";
        citizen.isPlayerModel = true;
        citizen.scale = 1.0f;
        citizen.npcRoleId = "Empty_Role";
        citizen.invulnerable = true;
        citizen.fKeyInteractionEnabled = true;
        citizen.messageSelectionMode = "RANDOM";
        this.citizenService.addCitizen(citizen);
        this.selectedCitizenId = newId;
        this.editDropdownsInitialized = false;
        this.filterCitizens();
        try {
            world = this.player.getWorld();
            if (world != null) {
                world.execute(() -> this.citizenService.spawnCitizen(citizen, world));
            }
        }
        catch (Exception e) {
            LOGGER.warning("Could not spawn new citizen: " + e.getMessage());
        }
        this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("citizen.admin.created", newId)).color("#44cc88"));
    }

    private void handleClone() {
        if (this.selectedCitizenId == null) {
            return;
        }
        CitizenData clone = this.citizenService.cloneCitizen(this.selectedCitizenId);
        if (clone == null) {
            return;
        }
        this.selectedCitizenId = clone.id;
        this.editDropdownsInitialized = false;
        this.dropdownsInitialized = false;
        this.filterCitizens();
        this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("citizen.admin.cloned", clone.id)).color("#aabbee"));
    }

    private void handleUseMySkin() {
        if (this.selectedCitizenId == null) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null) {
            return;
        }
        citizen.skinUsername = this.playerRef.getUsername();
        citizen.isPlayerModel = true;
        this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("citizen.admin.skin_set", citizen.skinUsername)).color("#aabbee"));
    }

    private void handleSetItems() {
        if (this.selectedCitizenId == null) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null) {
            return;
        }
        try {
            InventoryComponent.Utility utilityComp;
            ItemContainer utility;
            InventoryComponent.Hotbar hotbarComp;
            ItemContainer hotbar;
            ItemContainer armor;
            Ref ref = this.player.getReference();
            Store store = ref.getStore();
            InventoryComponent.Armor armorComp = (InventoryComponent.Armor)store.getComponent(ref, InventoryComponent.Armor.getComponentType());
            ItemContainer itemContainer = armor = armorComp != null ? armorComp.getInventory() : null;
            if (armor != null) {
                ItemStack helm = armor.getItemStack((short)0);
                citizen.helmet = helm != null ? helm.getItemId() : null;
                ItemStack chest = armor.getItemStack((short)1);
                citizen.chest = chest != null ? chest.getItemId() : null;
                ItemStack gloves = armor.getItemStack((short)2);
                citizen.gloves = gloves != null ? gloves.getItemId() : null;
                ItemStack legs = armor.getItemStack((short)3);
                citizen.leggings = legs != null ? legs.getItemId() : null;
            }
            ItemContainer itemContainer2 = hotbar = (hotbarComp = (InventoryComponent.Hotbar)store.getComponent(ref, InventoryComponent.Hotbar.getComponentType())) != null ? hotbarComp.getInventory() : null;
            if (hotbar != null) {
                ItemStack main = hotbar.getItemStack((short)0);
                citizen.mainHand = main != null ? main.getItemId() : null;
            }
            ItemContainer itemContainer3 = utility = (utilityComp = (InventoryComponent.Utility)store.getComponent(ref, InventoryComponent.Utility.getComponentType())) != null ? utilityComp.getInventory() : null;
            if (utility != null) {
                ItemStack off = utility.getItemStack((short)0);
                citizen.offHand = off != null ? off.getItemId() : null;
            }
            this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("citizen.admin.equipment_copied")).color("#88aacc"));
        }
        catch (Exception e) {
            LOGGER.warning("Failed to copy equipment: " + e.getMessage());
            this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("citizen.admin.equipment_copy_failed")).color("#ff6666"));
        }
    }

    private void handleSave(PageEventData data) {
        boolean wasSpawned;
        String newId2;
        if (this.selectedCitizenId == null) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null) {
            return;
        }
        if (data.editId != null && !(newId2 = data.editId.trim()).isEmpty() && !newId2.equals(citizen.id)) {
            if (this.citizenService.renameCitizen(citizen.id, newId2)) {
                this.selectedCitizenId = newId2;
                this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("citizen.admin.renamed", citizen.id, newId2)).color("#aabbee"));
            } else {
                this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("citizen.admin.rename_failed", newId2)).color("#ff6666"));
            }
            citizen = this.citizenService.getCitizen(this.selectedCitizenId);
            if (citizen == null) {
                return;
            }
        }
        if (data.editName != null) {
            citizen.name = CitizenAdminPage.emptyToNull(data.editName);
        }
        if (data.editGroup != null) {
            citizen.group = CitizenAdminPage.emptyToNull(data.editGroup);
        }
        if (data.editWorld != null) {
            citizen.worldName = CitizenAdminPage.emptyToNull(data.editWorld);
        }
        if (data.editPosX != null) {
            try {
                citizen.posX = Double.parseDouble(data.editPosX.trim());
            }
            catch (NumberFormatException newId2) {
                // empty catch block
            }
        }
        if (data.editPosY != null) {
            try {
                citizen.posY = Double.parseDouble(data.editPosY.trim());
            }
            catch (NumberFormatException newId2) {
                // empty catch block
            }
        }
        if (data.editPosZ != null) {
            try {
                citizen.posZ = Double.parseDouble(data.editPosZ.trim());
            }
            catch (NumberFormatException newId2) {
                // empty catch block
            }
        }
        if (data.editSkin != null) {
            citizen.skinUsername = CitizenAdminPage.emptyToNull(data.editSkin);
            if (citizen.isPlayerModel && citizen.skinUsername == null && citizen.customSkin == null) {
                citizen.isPlayerModel = false;
            }
        }
        if (data.editScale != null) {
            try {
                float s = Float.parseFloat(data.editScale.trim());
                if (s > 0.0f && s <= 10.0f) {
                    citizen.scale = s;
                }
            }
            catch (NumberFormatException s) {
                // empty catch block
            }
        }
        if (data.editNametagOff != null) {
            try {
                citizen.nametagOffset = Float.parseFloat(data.editNametagOff.trim());
            }
            catch (NumberFormatException s) {
                // empty catch block
            }
        }
        if (data.editPermission != null) {
            citizen.permission = CitizenAdminPage.emptyToNull(data.editPermission);
        }
        if (data.editNoPermMsg != null) {
            citizen.noPermissionMessage = CitizenAdminPage.emptyToNull(data.editNoPermMsg);
        }
        if (data.editRespawnDelay != null) {
            try {
                citizen.respawnDelay = Float.parseFloat(data.editRespawnDelay.trim());
            }
            catch (NumberFormatException s) {
                // empty catch block
            }
        }
        if (data.editMovRadius != null) {
            try {
                citizen.movementRadius = Float.parseFloat(data.editMovRadius.trim());
            }
            catch (NumberFormatException s) {
                // empty catch block
            }
        }
        if (data.editWalkSpeed != null) {
            try {
                float sp = Float.parseFloat(data.editWalkSpeed.trim());
                if (sp > 0.0f && sp <= 20.0f) {
                    citizen.walkSpeed = sp;
                }
            }
            catch (NumberFormatException sp) {
                // empty catch block
            }
        }
        if (data.editHelmet != null) {
            citizen.helmet = CitizenAdminPage.emptyToNull(data.editHelmet);
        }
        if (data.editChest != null) {
            citizen.chest = CitizenAdminPage.emptyToNull(data.editChest);
        }
        if (data.editLeggings != null) {
            citizen.leggings = CitizenAdminPage.emptyToNull(data.editLeggings);
        }
        if (data.editGloves != null) {
            citizen.gloves = CitizenAdminPage.emptyToNull(data.editGloves);
        }
        if (data.editMainHand != null) {
            citizen.mainHand = CitizenAdminPage.emptyToNull(data.editMainHand);
        }
        if (data.editOffHand != null) {
            citizen.offHand = CitizenAdminPage.emptyToNull(data.editOffHand);
        }
        if (data.editDialog != null) {
            String dialogStr = data.editDialog.trim();
            if (dialogStr.isEmpty()) {
                citizen.dialogs = null;
            } else {
                HashMap<String, CitizenData.DialogCondition> existingConditions = new HashMap<String, CitizenData.DialogCondition>();
                if (citizen.dialogs != null) {
                    for (CitizenData.ConditionalDialog cd : citizen.dialogs) {
                        if (cd.dialogId == null || cd.condition == null) continue;
                        existingConditions.put(cd.dialogId, cd.condition);
                    }
                }
                citizen.dialogs = new ArrayList<CitizenData.ConditionalDialog>();
                for (String id : dialogStr.split(",")) {
                    String trimmed = id.trim();
                    if (trimmed.isEmpty()) continue;
                    CitizenData.ConditionalDialog dialog = new CitizenData.ConditionalDialog();
                    dialog.dialogId = trimmed;
                    dialog.condition = (CitizenData.DialogCondition)existingConditions.get(trimmed);
                    citizen.dialogs.add(dialog);
                }
            }
        }
        if (data.editShop != null) {
            citizen.shopId = CitizenAdminPage.emptyToNull(data.editShop);
        }
        if (data.editMsgDelay != null) {
            try {
                float delay = Float.parseFloat(data.editMsgDelay.trim());
                if (delay >= 0.0f && delay <= 60.0f) {
                    citizen.messageDelay = delay;
                }
            }
            catch (NumberFormatException delay) {
                // empty catch block
            }
        }
        if (data.editSpawnFx != null) {
            citizen.spawnParticles = CitizenAdminPage.emptyToNull(data.editSpawnFx);
        }
        if (data.editSpawnFxDur != null) {
            try {
                citizen.spawnFxDuration = Math.max(0.0f, Float.parseFloat(data.editSpawnFxDur.trim()));
            }
            catch (NumberFormatException delay) {
                // empty catch block
            }
        }
        if (data.editDespawnFx != null) {
            citizen.despawnParticles = CitizenAdminPage.emptyToNull(data.editDespawnFx);
        }
        if (data.editDespawnFxDur != null) {
            try {
                citizen.despawnFxDuration = Math.max(0.0f, Float.parseFloat(data.editDespawnFxDur.trim()));
            }
            catch (NumberFormatException delay) {
                // empty catch block
            }
        }
        if (data.editAttachments != null) {
            citizen.attachmentIds = CitizenAdminPage.parseAttachments(data.editAttachments);
        }
        if (data.editGradientSet != null) {
            citizen.gradientSet = CitizenAdminPage.emptyToNull(data.editGradientSet);
        }
        if (data.editGradientId != null) {
            citizen.gradientId = CitizenAdminPage.emptyToNull(data.editGradientId);
        }
        if (data.editNametagColor != null) {
            citizen.nametagColor = CitizenAdminPage.emptyToNull(data.editNametagColor);
        }
        if (data.editNametagFmt != null) {
            citizen.nametagFormat = CitizenAdminPage.emptyToNull(data.editNametagFmt);
        }
        this.citizenService.updateCitizen(citizen);
        boolean bl = wasSpawned = citizen.spawnedEntityUUID != null;
        if (wasSpawned) {
            try {
                World world = this.player.getWorld();
                if (world != null) {
                    CitizenData c = citizen;
                    world.execute(() -> {
                        this.citizenService.despawnCitizen(c, world);
                        this.citizenService.spawnCitizen(c, world);
                    });
                }
            }
            catch (Exception e) {
                LOGGER.warning("Could not respawn citizen after save: " + e.getMessage());
            }
        }
        this.dropdownsInitialized = false;
        this.editDropdownsInitialized = false;
        this.filterCitizens();
        this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("citizen.admin.saved", this.selectedCitizenId)).color("#44cc88"));
        this.refreshUI();
    }

    private void handleAnimEditSave(PageEventData data) {
        if (this.selectedCitizenId == null) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null) {
            return;
        }
        if (citizen.animations == null) {
            citizen.animations = new ArrayList<CitizenData.AnimationConfig>();
        }
        if (this.editingAnimIndex >= 0 && this.editingAnimIndex < citizen.animations.size()) {
            anim = citizen.animations.get(this.editingAnimIndex);
        } else {
            if (citizen.animations.size() >= 8) {
                this.editingAnimIndex = -2;
                this.currentSlotFilter = null;
                return;
            }
            anim = new CitizenData.AnimationConfig();
            citizen.animations.add(anim);
        }
        if (data.animEditTrigger != null) {
            anim.type = data.animEditTrigger;
        }
        if (data.animEditName != null) {
            anim.animationName = data.animEditName;
        }
        if (data.animEditSlot != null) {
            anim.animationSlot = CitizenAdminPage.resolveSlotIndex(data.animEditSlot);
        }
        if (data.animEditInterval != null) {
            try {
                anim.interval = Float.parseFloat(data.animEditInterval.trim());
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        if (data.animEditProxRange != null) {
            try {
                anim.proximityRange = Float.parseFloat(data.animEditProxRange.trim());
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        if (data.animEditStopEnabled != null) {
            anim.stopAfterTime = "Yes".equals(data.animEditStopEnabled);
        }
        if (data.animEditStopTime != null) {
            try {
                anim.stopTime = Float.parseFloat(data.animEditStopTime.trim());
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        if (data.animEditStopAnim != null) {
            anim.stopAnimation = CitizenAdminPage.emptyToNull(data.animEditStopAnim);
        }
        this.citizenService.updateCitizen(citizen);
        CitizenAnimationManager animMgr = this.citizenService.getAnimationManager();
        if (animMgr != null) {
            animMgr.unregister(citizen.id);
            animMgr.register(citizen);
        }
        this.editingAnimIndex = -2;
        this.currentSlotFilter = null;
        this.currentAnimName = null;
    }

    private void handleAnimationDelete(int idx) {
        if (this.selectedCitizenId == null || idx < 0) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null || citizen.animations == null) {
            return;
        }
        if (idx < citizen.animations.size()) {
            citizen.animations.remove(idx);
            this.citizenService.updateCitizen(citizen);
            CitizenAnimationManager animMgr = this.citizenService.getAnimationManager();
            if (animMgr != null) {
                animMgr.unregister(citizen.id);
                animMgr.register(citizen);
            }
        }
    }

    private void handleCommandAdd(String cmdText, String runMode) {
        if (this.selectedCitizenId == null || cmdText == null || cmdText.trim().isEmpty()) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null) {
            return;
        }
        if (citizen.commandActions == null) {
            citizen.commandActions = new ArrayList<CitizenData.CommandAction>();
        }
        if (citizen.commandActions.size() >= 8) {
            return;
        }
        boolean isServer = !"Player".equals(runMode);
        citizen.commandActions.add(new CitizenData.CommandAction(cmdText.trim(), isServer));
        this.refreshUI();
    }

    private void handleCommandEditSave(String cmdText, String runMode) {
        if (this.selectedCitizenId == null || this.editingCmdIndex < 0) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null || citizen.commandActions == null) {
            return;
        }
        if (this.editingCmdIndex < citizen.commandActions.size()) {
            CitizenData.CommandAction cmd = citizen.commandActions.get(this.editingCmdIndex);
            if (cmdText != null && !cmdText.trim().isEmpty()) {
                cmd.command = cmdText.trim();
            }
            if (runMode != null) {
                cmd.runAsServer = !"Player".equals(runMode);
            }
        }
        this.editingCmdIndex = -2;
    }

    private void handleCommandDelete(int idx) {
        if (this.selectedCitizenId == null || idx < 0) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null || citizen.commandActions == null) {
            return;
        }
        if (idx < citizen.commandActions.size()) {
            citizen.commandActions.remove(idx);
        }
    }

    private void handleMessageAdd(String msgText) {
        if (this.selectedCitizenId == null || msgText == null || msgText.trim().isEmpty()) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null) {
            return;
        }
        if (citizen.messages == null) {
            citizen.messages = new ArrayList<String>();
        }
        if (citizen.messages.size() >= 8) {
            return;
        }
        citizen.messages.add(msgText.trim());
        this.refreshUI();
    }

    private void handleMessageEditSave(String msgText) {
        if (this.selectedCitizenId == null || this.editingMsgIndex < 0) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null || citizen.messages == null) {
            return;
        }
        if (this.editingMsgIndex < citizen.messages.size() && msgText != null && !msgText.trim().isEmpty()) {
            citizen.messages.set(this.editingMsgIndex, msgText.trim());
        }
        this.editingMsgIndex = -2;
    }

    private void handleMessageDelete(int idx) {
        if (this.selectedCitizenId == null || idx < 0) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null || citizen.messages == null) {
            return;
        }
        if (idx < citizen.messages.size()) {
            citizen.messages.remove(idx);
        }
    }

    private void handleMoveToMe() {
        World world2;
        if (this.selectedCitizenId == null) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null) {
            return;
        }
        try {
            TransformComponent transform = (TransformComponent)this.storedStore.getComponent(this.storedRef, TransformComponent.getComponentType());
            if (transform != null) {
                Vector3f rot;
                Vector3d pos = transform.getPosition();
                if (pos != null) {
                    this.applyPositionToCitizen(citizen, pos.x, pos.y, pos.z);
                }
                if ((rot = transform.getRotation()) != null) {
                    citizen.rotY = rot.y;
                }
            }
        }
        catch (Exception e) {
            LOGGER.warning("Could not get player position: " + e.getMessage());
        }
        try {
            world2 = this.player.getWorld();
            if (world2 != null) {
                citizen.worldName = world2.getName();
            }
        }
        catch (Exception world2) {
            // empty catch block
        }
        this.citizenService.updateCitizen(citizen);
        if (citizen.spawnedEntityUUID != null) {
            try {
                world2 = this.player.getWorld();
                if (world2 != null) {
                    world2.execute(() -> {
                        this.citizenService.despawnCitizen(citizen, world2);
                        this.citizenService.spawnCitizen(citizen, world2);
                    });
                }
            }
            catch (Exception e) {
                LOGGER.warning("Could not respawn citizen at new position: " + e.getMessage());
            }
        }
        this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("citizen.admin.moved", this.selectedCitizenId)).color("#44ccee"));
    }

    private void handleUseMyPos() {
        if (this.selectedCitizenId == null) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null) {
            return;
        }
        try {
            World world;
            Vector3d pos;
            TransformComponent transform = (TransformComponent)this.storedStore.getComponent(this.storedRef, TransformComponent.getComponentType());
            if (transform != null && (pos = transform.getPosition()) != null) {
                this.applyPositionToCitizen(citizen, pos.x, pos.y, pos.z);
            }
            if ((world = this.player.getWorld()) != null) {
                citizen.worldName = world.getName();
            }
        }
        catch (Exception e) {
            LOGGER.warning("Could not get player position: " + e.getMessage());
        }
        CoreI18n i18nPos = CoreI18n.getInstance();
        String msg = citizen.spawnRelative ? i18nPos.get("citizen.admin.pos_relative") : i18nPos.get("citizen.admin.pos_absolute");
        this.player.sendMessage(Message.raw((String)msg).color("#88bbee"));
    }

    private void handleWaypointAdd() {
        if (this.selectedCitizenId == null) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null) {
            return;
        }
        try {
            TransformComponent transform = (TransformComponent)this.storedStore.getComponent(this.storedRef, TransformComponent.getComponentType());
            if (transform == null) {
                return;
            }
            Vector3d pos = transform.getPosition();
            Vector3f rot = transform.getRotation();
            if (pos == null) {
                return;
            }
            if (citizen.waypoints == null) {
                citizen.waypoints = new ArrayList<CitizenData.WaypointData>();
            }
            float rotY = rot != null ? rot.y : 0.0f;
            citizen.waypoints.add(new CitizenData.WaypointData(pos.x, pos.y, pos.z, rotY, 0.0f));
            this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("citizen.admin.wp_added", citizen.waypoints.size(), String.format("%.1f, %.1f, %.1f", pos.x, pos.y, pos.z))).color("#4488ff"));
        }
        catch (Exception e) {
            LOGGER.warning("Could not add waypoint: " + e.getMessage());
        }
    }

    private void handleWaypointRemoveLast() {
        if (this.selectedCitizenId == null) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null || citizen.waypoints == null || citizen.waypoints.isEmpty()) {
            return;
        }
        citizen.waypoints.removeLast();
        this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("citizen.admin.wp_removed", citizen.waypoints.size())).color("#ff8844"));
    }

    private void handleWaypointClear() {
        if (this.selectedCitizenId == null) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null) {
            return;
        }
        if (citizen.waypoints != null) {
            citizen.waypoints.clear();
        }
        this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("citizen.admin.wp_cleared")).color("#ff6666"));
    }

    private void handleWaypointRecord() {
        if (this.selectedCitizenId == null) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null) {
            return;
        }
        this.citizenService.startWaypointRecording(this.playerRef.getUuid(), this.selectedCitizenId, this.player, this.playerRef);
        int wpCount = citizen.waypoints != null ? citizen.waypoints.size() : 0;
        CoreI18n wpI18n = CoreI18n.getInstance();
        this.player.sendMessage(Message.raw((String)wpI18n.get("citizen.admin.wp_recording", this.selectedCitizenId, wpCount)).color("#44cc88"));
        this.player.sendMessage(Message.raw((String)("  /kscitizen wpadd \u2014 " + wpI18n.get("citizen.admin.wp_recording_add"))).color("#88aacc"));
        this.player.sendMessage(Message.raw((String)("  /kscitizen wpundo \u2014 " + wpI18n.get("citizen.admin.wp_recording_undo"))).color("#88aacc"));
        this.player.sendMessage(Message.raw((String)("  /kscitizen wpdone \u2014 " + wpI18n.get("citizen.admin.wp_recording_done"))).color("#88aacc"));
        this.close();
    }

    private void handleDeathCommandAdd(String command, String runMode) {
        if (this.selectedCitizenId == null || command == null || command.trim().isEmpty()) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null) {
            return;
        }
        if (citizen.deathCommands == null) {
            citizen.deathCommands = new ArrayList<CitizenData.CommandAction>();
        }
        if (citizen.deathCommands.size() >= 4) {
            this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("citizen.admin.death_cmd_max")).color("#ff6666"));
            return;
        }
        CitizenData.CommandAction cmd = new CitizenData.CommandAction();
        cmd.command = command.trim();
        cmd.runAsServer = !"Player".equals(runMode);
        citizen.deathCommands.add(cmd);
    }

    private void handleDeathCommandDelete(int index) {
        if (this.selectedCitizenId == null) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null || citizen.deathCommands == null || index < 0 || index >= citizen.deathCommands.size()) {
            return;
        }
        citizen.deathCommands.remove(index);
    }

    private void applyPositionToCitizen(CitizenData citizen, double absX, double absY, double absZ) {
        if (citizen.spawnRelative) {
            try {
                Transform spawn;
                ISpawnProvider spawnProvider;
                World world = this.player.getWorld();
                if (world != null && (spawnProvider = world.getWorldConfig().getSpawnProvider()) != null && (spawn = spawnProvider.getSpawnPoint(world, this.playerRef.getUuid())) != null) {
                    Vector3d spawnPos = spawn.getPosition();
                    citizen.posX = absX - spawnPos.x;
                    citizen.posY = absY - spawnPos.y;
                    citizen.posZ = absZ - spawnPos.z;
                    return;
                }
            }
            catch (Exception e) {
                LOGGER.warning("Could not resolve spawn point for relative position, using absolute: " + e.getMessage());
            }
        }
        citizen.posX = absX;
        citizen.posY = absY;
        citizen.posZ = absZ;
    }

    private void handleTeleport() {
        if (this.selectedCitizenId == null) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null) {
            return;
        }
        World world = this.player.getWorld();
        Vector3d pos = world != null ? this.citizenService.resolvePosition(citizen, world) : new Vector3d(citizen.posX, citizen.posY, citizen.posZ);
        String cmd = "tp " + this.playerRef.getUsername() + " " + pos.x + " " + pos.y + " " + pos.z;
        CommandUtils.executeAsConsole(this.player, cmd);
        this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("citizen.admin.teleported", this.selectedCitizenId)).color("#88bbee"));
    }

    private void handleRespawn() {
        if (this.selectedCitizenId == null) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null) {
            return;
        }
        try {
            World world = this.player.getWorld();
            if (world != null) {
                world.execute(() -> {
                    this.citizenService.despawnCitizen(citizen, world);
                    this.citizenService.spawnCitizen(citizen, world);
                });
            }
        }
        catch (Exception e) {
            LOGGER.warning("Could not respawn citizen: " + e.getMessage());
        }
        this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("citizen.admin.respawned", this.selectedCitizenId)).color("#88eeaa"));
    }

    private void handleDespawn() {
        if (this.selectedCitizenId == null) {
            return;
        }
        CitizenData citizen = this.citizenService.getCitizen(this.selectedCitizenId);
        if (citizen == null) {
            return;
        }
        try {
            World world = this.player.getWorld();
            if (world != null) {
                world.execute(() -> this.citizenService.despawnCitizen(citizen, world));
            } else {
                this.citizenService.despawnCitizen(citizen);
            }
        }
        catch (Exception e) {
            this.citizenService.despawnCitizen(citizen);
        }
        this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("citizen.admin.despawned", this.selectedCitizenId)).color("#eebb88"));
    }

    private void handleDeleteConfirm() {
        if (this.selectedCitizenId == null) {
            return;
        }
        World world = this.player.getWorld();
        this.citizenService.removeCitizen(this.selectedCitizenId, world);
        this.player.sendMessage(Message.raw((String)CoreI18n.getInstance().get("citizen.admin.deleted", this.selectedCitizenId)).color("#ff6666"));
        this.selectedCitizenId = null;
        this.showingDeleteConfirm = false;
        this.editDropdownsInitialized = false;
        this.dropdownsInitialized = false;
        this.filterCitizens();
    }

    private void buildListPanel(UICommandBuilder ui) {
        this.filterCitizens();
        if (!this.dropdownsInitialized) {
            this.dropdownsInitialized = true;
            TreeSet<String> groups = new TreeSet<String>();
            for (CitizenData citizenData : this.citizenService.getAllCitizens()) {
                if (citizenData.group == null || citizenData.group.isEmpty()) continue;
                groups.add(citizenData.group);
            }
            ArrayList<DropdownEntryInfo> filterEntries = new ArrayList<DropdownEntryInfo>();
            filterEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)CoreI18n.getInstance().get("citizen.admin.dd.all")), "all"));
            for (String g : groups) {
                filterEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)g), g));
            }
            ui.set("#GroupFilterDropdown.Entries", filterEntries);
        }
        ui.set("#GroupFilterDropdown.Value", this.groupFilter);
        int start = this.listPage * 16;
        for (int i = 0; i < 16; ++i) {
            String string = "#CitEntry" + i;
            int actualIdx = start + i;
            if (actualIdx < this.filteredCitizens.size()) {
                CitizenData citizen = this.filteredCitizens.get(actualIdx);
                ui.set(string + ".Visible", true);
                boolean spawned = citizen.spawnedEntityUUID != null;
                ui.set("#CitStatus" + i + ".Text", spawned ? "\u25cf" : "\u25cb");
                ui.set("#CitStatus" + i + ".Style.TextColor", spawned ? "#44cc88" : "#cc4444");
                ui.set("#CitName" + i + ".Text", citizen.getDisplayName());
                ui.set("#CitGroup" + i + ".Text", citizen.group != null ? citizen.group : "");
                boolean isSelected = citizen.id.equals(this.selectedCitizenId);
                ui.set(string + ".Background", isSelected ? "#44cc8830" : "#ffffff08");
                continue;
            }
            ui.set(string + ".Visible", false);
        }
        int totalPages = Math.max(1, (int)Math.ceil((double)this.filteredCitizens.size() / 16.0));
        if (this.listPage >= totalPages) {
            this.listPage = totalPages - 1;
        }
        if (this.listPage < 0) {
            this.listPage = 0;
        }
        ui.set("#CitPageLabel.Text", this.listPage + 1 + " / " + totalPages + " (" + this.filteredCitizens.size() + ")");
        ui.set("#CitPrevButton.Visible", this.listPage > 0);
        ui.set("#CitNextButton.Visible", this.listPage < totalPages - 1);
        ui.set("#CitizenCountLabel.Text", CoreI18n.getInstance().get("citizen.admin.count", this.citizenService.getCitizenCount()));
    }

    private void buildDetailPanel(UICommandBuilder ui) {
        String[][] tabs;
        boolean hasSelection = this.selectedCitizenId != null;
        CitizenData citizen = hasSelection ? this.citizenService.getCitizen(this.selectedCitizenId) : null;
        ui.set("#CitDetailPanel.Visible", hasSelection && citizen != null);
        ui.set("#EmptyDetailPanel.Visible", !hasSelection || citizen == null);
        if (citizen == null) {
            return;
        }
        ui.set("#CitDetailId.Value", citizen.id);
        boolean spawned = citizen.spawnedEntityUUID != null;
        CoreI18n i18n = CoreI18n.getInstance();
        ui.set("#CitDetailStatus.Text", spawned ? "\u25cf " + i18n.get("citizen.admin.status.spawned") : "\u25cb " + i18n.get("citizen.admin.status.not_spawned"));
        ui.set("#CitDetailStatus.Style.TextColor", spawned ? "#44cc88" : "#cc4444");
        boolean tabGen = "general".equals(this.currentTab);
        boolean tabApp = "appearance".equals(this.currentTab);
        boolean tabBhv = "behavior".equals(this.currentTab);
        boolean tabInt = "interaction".equals(this.currentTab);
        boolean tabCbt = "combat".equals(this.currentTab);
        boolean tabDsp = "display".equals(this.currentTab);
        ui.set("#TabGen.Visible", tabGen);
        ui.set("#TabAppMain.Visible", tabApp);
        ui.set("#TabAppEquip.Visible", tabApp);
        ui.set("#TabAppNew.Visible", tabApp);
        ui.set("#TabBhvMain.Visible", tabBhv);
        ui.set("#TabIntMain.Visible", tabInt);
        ui.set("#TabIntAssign.Visible", tabInt);
        ui.set("#TabIntAnim.Visible", tabInt);
        ui.set("#TabIntCmd.Visible", tabInt);
        ui.set("#TabIntMsg.Visible", tabInt);
        ui.set("#TabCbtMain.Visible", tabCbt);
        ui.set("#TabDspMain.Visible", tabDsp);
        for (String[] t : tabs = new String[][]{{"Gen", "general", "#44cc88"}, {"App", "appearance", "#aa88ee"}, {"Bhv", "behavior", "#4488ff"}, {"Int", "interaction", "#ffaa44"}, {"Cbt", "combat", "#ff6666"}, {"Dsp", "display", "#88aacc"}}) {
            boolean active = t[1].equals(this.currentTab);
            ui.set("#Tab" + t[0] + "Label.Style.TextColor", active ? "#ffffff" : t[2]);
        }
        ui.set("#EditName.Value", citizen.getDisplayName());
        ui.set("#EditGroup.Value", citizen.group != null ? citizen.group : "");
        ui.set("#EditWorld.Value", citizen.worldName != null ? citizen.worldName : "");
        ui.set("#EditPosX.Value", String.format("%.1f", citizen.posX));
        ui.set("#EditPosY.Value", String.format("%.1f", citizen.posY));
        ui.set("#EditPosZ.Value", String.format("%.1f", citizen.posZ));
        ui.set("#EditSkin.Value", citizen.skinUsername != null ? citizen.skinUsername : "");
        ui.set("#EditScale.Value", String.format("%.2f", Float.valueOf(citizen.scale)));
        ui.set("#EditNametagOff.Value", String.format("%.1f", Float.valueOf(citizen.nametagOffset)));
        boolean showEntityType = !citizen.isPlayerModel;
        ui.set("#EntityTypeRow.Visible", showEntityType);
        ui.set("#EntityTypeSpacer.Visible", showEntityType);
        ui.set("#EditPermission.Value", citizen.permission != null ? citizen.permission : "");
        ui.set("#EditNoPermMsg.Value", citizen.noPermissionMessage != null ? citizen.noPermissionMessage : "");
        ui.set("#EditRespawnDelay.Value", String.format("%.1f", Float.valueOf(citizen.respawnDelay)));
        ui.set("#EditMovRadius.Value", String.format("%.1f", Float.valueOf(citizen.movementRadius)));
        ui.set("#EditWalkSpeed.Value", String.format("%.1f", Float.valueOf(citizen.walkSpeed)));
        ui.set("#EditHelmet.Value", citizen.helmet != null ? citizen.helmet : "");
        ui.set("#EditChest.Value", citizen.chest != null ? citizen.chest : "");
        ui.set("#EditLeggings.Value", citizen.leggings != null ? citizen.leggings : "");
        ui.set("#EditGloves.Value", citizen.gloves != null ? citizen.gloves : "");
        ui.set("#EditMainHand.Value", citizen.mainHand != null ? citizen.mainHand : "");
        ui.set("#EditOffHand.Value", citizen.offHand != null ? citizen.offHand : "");
        if (citizen.dialogs != null && !citizen.dialogs.isEmpty()) {
            String dialogStr = citizen.dialogs.stream().map(d -> d.dialogId).filter(Objects::nonNull).collect(Collectors.joining(", "));
            ui.set("#EditDialog.Value", dialogStr);
        } else {
            ui.set("#EditDialog.Value", "");
        }
        ui.set("#EditShop.Value", citizen.shopId != null ? citizen.shopId : "");
        ui.set("#QuestProfileBtn.Visible", ModMenuRegistry.hasNpcProfileEditor());
        if (!this.editDropdownsInitialized) {
            this.editDropdownsInitialized = true;
            CoreI18n ddI18n = CoreI18n.getInstance();
            ui.set("#EditModelType.Entries", List.of(new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.player_model")), "Player Model"), new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.other_entity")), "Other Entity")));
            ui.set("#EditAttitude.Entries", List.of(new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.passive")), "PASSIVE"), new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.neutral")), "NEUTRAL"), new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.aggressive")), "AGGRESSIVE")));
            ui.set("#EditMovement.Entries", List.of(new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.idle")), "IDLE"), new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.wander")), "WANDER"), new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.wander_circle")), "WANDER_CIRCLE"), new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.wander_rect")), "WANDER_RECT"), new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.path")), "PATH")));
            List<DropdownEntryInfo> yesNo = List.of(new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.yes")), "Yes"), new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.no")), "No"));
            ui.set("#EditRotate.Entries", yesNo);
            ui.set("#EditInvulnerable.Entries", yesNo);
            ui.set("#EditTakesDamage.Entries", yesNo);
            ui.set("#EditRespawn.Entries", yesNo);
            ui.set("#EditFKey.Entries", yesNo);
            ui.set("#EditHideNpc.Entries", yesNo);
            ui.set("#EditBankingEnabled.Entries", yesNo);
            ui.set("#EditHideNametag.Entries", yesNo);
            ui.set("#EditLiveSkin.Entries", yesNo);
            ui.set("#EditSpawnRelative.Entries", yesNo);
            ui.set("#EditMsgMode.Entries", List.of(new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.random")), "RANDOM"), new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.sequential")), "SEQUENTIAL"), new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.all_messages")), "ALL")));
            ArrayList<DropdownEntryInfo> roleEntries = new ArrayList<DropdownEntryInfo>();
            roleEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.role.auto")), "Auto"));
            roleEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.role.empty")), "Empty_Role"));
            roleEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.role.idle_interact")), "KS_NPC_Interactable_Role"));
            roleEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.role.wander_r2_interact")), "KS_NPC_WanderI_R2_Interactable_Role"));
            roleEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.role.wander_r2")), "KS_NPC_Wander_R2_Role"));
            roleEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.role.wander_r5_interact")), "KS_NPC_WanderI_R5_Interactable_Role"));
            roleEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.role.wander_r5")), "KS_NPC_Wander_R5_Role"));
            roleEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.role.wander_r10_interact")), "KS_NPC_WanderI_R10_Interactable_Role"));
            roleEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.role.wander_r10")), "KS_NPC_Wander_R10_Role"));
            roleEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.role.wander_r15_interact")), "KS_NPC_WanderI_R15_Interactable_Role"));
            roleEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.role.wander_r15")), "KS_NPC_Wander_R15_Role"));
            ui.set("#EditNpcRole.Entries", roleEntries);
            ui.set("#CmdRunMode.Entries", List.of(new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.server")), "Server"), new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.player")), "Player")));
            ui.set("#EditPathShape.Entries", List.of(new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.loop")), "LOOP"), new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.line")), "LINE")));
            ui.set("#DeathCmdRunMode.Entries", List.of(new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.server")), "Server"), new DropdownEntryInfo(LocalizableString.fromString((String)ddI18n.get("citizen.admin.dd.player")), "Player")));
            try {
                Map assetMap = ModelAsset.getAssetMap().getAssetMap();
                ArrayList<DropdownEntryInfo> entityEntries = new ArrayList<DropdownEntryInfo>();
                ArrayList modelIds = new ArrayList(assetMap.keySet());
                Collections.sort(modelIds, String.CASE_INSENSITIVE_ORDER);
                for (String modelId : modelIds) {
                    entityEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)modelId), modelId));
                }
                ui.set("#EditEntityType.Entries", entityEntries);
            }
            catch (Exception e) {
                LOGGER.fine("Could not load model asset list: " + e.getMessage());
                ui.set("#EditEntityType.Entries", List.of(new DropdownEntryInfo(LocalizableString.fromString((String)CoreI18n.getInstance().get("citizen.admin.dd.unavailable")), "")));
            }
            try {
                Map particleMap = ParticleSystem.getAssetMap().getAssetMap();
                ArrayList<DropdownEntryInfo> fxEntries = new ArrayList<DropdownEntryInfo>();
                fxEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)CoreI18n.getInstance().get("citizen.admin.dd.none")), ""));
                new TreeSet(particleMap.keySet()).forEach(id -> fxEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)id), id)));
                ui.set("#EditSpawnFx.Entries", fxEntries);
                ui.set("#EditDespawnFx.Entries", fxEntries);
            }
            catch (Exception e) {
                LOGGER.fine("Could not load particle systems: " + e.getMessage());
                List<DropdownEntryInfo> fallback = List.of(new DropdownEntryInfo(LocalizableString.fromString((String)CoreI18n.getInstance().get("citizen.admin.dd.none")), ""));
                ui.set("#EditSpawnFx.Entries", fallback);
                ui.set("#EditDespawnFx.Entries", fallback);
            }
            try {
                Map gradientSets = CosmeticsModule.get().getRegistry().getGradientSets();
                ArrayList<DropdownEntryInfo> setEntries = new ArrayList<DropdownEntryInfo>();
                setEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)CoreI18n.getInstance().get("citizen.admin.dd.none")), ""));
                if (gradientSets != null) {
                    new TreeSet(gradientSets.keySet()).forEach(id -> setEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)id), id)));
                }
                ui.set("#EditGradientSet.Entries", setEntries);
            }
            catch (Exception e) {
                LOGGER.fine("Could not load gradient sets: " + e.getMessage());
                ui.set("#EditGradientSet.Entries", List.of(new DropdownEntryInfo(LocalizableString.fromString((String)CoreI18n.getInstance().get("citizen.admin.dd.none")), "")));
            }
            CoreI18n pi18n = CoreI18n.getInstance();
            ArrayList<DropdownEntryInfo> presetEntries = new ArrayList<DropdownEntryInfo>();
            for (int pi = 0; pi < PRESETS.size(); ++pi) {
                presetEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)pi18n.get(PRESETS.get(pi).i18nKey())), String.valueOf(pi)));
            }
            ui.set("#EditPreset.Entries", presetEntries);
        }
        try {
            PlayerSkinGradientSet set;
            Map gradientSets;
            ArrayList<DropdownEntryInfo> gradIdEntries = new ArrayList<DropdownEntryInfo>();
            gradIdEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)CoreI18n.getInstance().get("citizen.admin.dd.none")), ""));
            if (citizen.gradientSet != null && !citizen.gradientSet.isEmpty() && (gradientSets = CosmeticsModule.get().getRegistry().getGradientSets()) != null && (set = (PlayerSkinGradientSet)gradientSets.get(citizen.gradientSet)) != null && set.getGradients() != null) {
                new TreeSet(set.getGradients().keySet()).forEach(id -> gradIdEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)id), id)));
            }
            ui.set("#EditGradientId.Entries", gradIdEntries);
        }
        catch (Exception e) {
            LOGGER.fine("Could not load gradient IDs: " + e.getMessage());
            ui.set("#EditGradientId.Entries", List.of(new DropdownEntryInfo(LocalizableString.fromString((String)CoreI18n.getInstance().get("citizen.admin.dd.none")), "")));
        }
        ui.set("#EditModelType.Value", citizen.isPlayerModel ? "Player Model" : "Other Entity");
        ui.set("#EditAttitude.Value", citizen.attitude != null ? citizen.attitude : "PASSIVE");
        ui.set("#EditMovement.Value", citizen.movementType != null ? citizen.movementType : "IDLE");
        ui.set("#EditRotate.Value", citizen.rotateTowardsPlayer ? "Yes" : "No");
        ui.set("#EditInvulnerable.Value", citizen.invulnerable ? "Yes" : "No");
        ui.set("#EditTakesDamage.Value", citizen.takesDamage ? "Yes" : "No");
        ui.set("#EditRespawn.Value", citizen.respawnOnDeath ? "Yes" : "No");
        ui.set("#EditFKey.Value", citizen.fKeyInteractionEnabled ? "Yes" : "No");
        ui.set("#EditHideNpc.Value", citizen.hideNpc ? "Yes" : "No");
        ui.set("#EditBankingEnabled.Value", citizen.bankingEnabled ? "Yes" : "No");
        ui.set("#EditHideNametag.Value", citizen.hideNametag ? "Yes" : "No");
        ui.set("#EditLiveSkin.Value", citizen.useLiveSkin ? "Yes" : "No");
        ui.set("#EditMsgMode.Value", citizen.messageSelectionMode != null ? citizen.messageSelectionMode : "RANDOM");
        ui.set("#EditMsgDelay.Value", String.format("%.1f", Float.valueOf(citizen.messageDelay)));
        ui.set("#EditSpawnRelative.Value", citizen.spawnRelative ? "Yes" : "No");
        boolean isAutoRole = citizen.npcRoleId == null || citizen.npcRoleId.isEmpty();
        ui.set("#EditNpcRole.Value", isAutoRole ? "Auto" : citizen.npcRoleId);
        String effectiveRole = citizen.resolveRoleName();
        ui.set("#NpcRoleEffective.Text", effectiveRole);
        ui.set("#NpcRoleDesc.Text", CitizenAdminPage.getRoleDescription(effectiveRole));
        ui.set("#EditEntityType.Value", citizen.entityTypeId != null ? citizen.entityTypeId : "");
        ui.set("#EditSpawnFx.Value", citizen.spawnParticles != null ? citizen.spawnParticles : "");
        ui.set("#EditSpawnFxDur.Value", citizen.spawnFxDuration > 0.0f ? String.format("%.1f", Float.valueOf(citizen.spawnFxDuration)) : "0");
        ui.set("#EditDespawnFx.Value", citizen.despawnParticles != null ? citizen.despawnParticles : "");
        ui.set("#EditDespawnFxDur.Value", citizen.despawnFxDuration > 0.0f ? String.format("%.1f", Float.valueOf(citizen.despawnFxDuration)) : "0");
        ui.set("#EditAttachments.Value", CitizenAdminPage.formatAttachments(citizen.attachmentIds));
        ui.set("#EditGradientSet.Value", citizen.gradientSet != null ? citizen.gradientSet : "");
        ui.set("#EditGradientId.Value", citizen.gradientId != null ? citizen.gradientId : "");
        ui.set("#EditNametagColor.Value", citizen.nametagColor != null ? citizen.nametagColor : "");
        ui.set("#EditNametagFmt.Value", citizen.nametagFormat != null ? citizen.nametagFormat : "");
        int animCount = citizen.animations != null ? citizen.animations.size() : 0;
        ui.set("#AnimHeader.Text", i18n.get("citizen.admin.section.animations", animCount));
        for (int i = 0; i < 8; ++i) {
            if (i < animCount) {
                CitizenData.AnimationConfig anim = citizen.animations.get(i);
                String slotName = CitizenAdminPage.resolveSlotName(anim.animationSlot);
                ui.set("#AnimRow" + i + ".Visible", true);
                ui.set("#AnimInfo" + i + ".Text", (anim.type != null ? anim.type : "?") + ": " + (anim.animationName != null ? anim.animationName : "?") + " [" + slotName + "]");
                continue;
            }
            ui.set("#AnimRow" + i + ".Visible", false);
        }
        int cmdCount = citizen.commandActions != null ? citizen.commandActions.size() : 0;
        ui.set("#CmdHeader.Text", i18n.get("citizen.admin.section.commands", cmdCount));
        for (int i = 0; i < 8; ++i) {
            if (i < cmdCount) {
                CitizenData.CommandAction cmd = citizen.commandActions.get(i);
                ui.set("#CmdRow" + i + ".Visible", true);
                ui.set("#CmdBadge" + i + ".Text", cmd.runAsServer ? "S" : "P");
                ui.set("#CmdBadge" + i + ".Style.TextColor", cmd.runAsServer ? "#aa88ee" : "#4488ff");
                String display = cmd.command != null ? (cmd.command.length() > 50 ? cmd.command.substring(0, 47) + "..." : cmd.command) : "";
                ui.set("#CmdText" + i + ".Text", display);
                continue;
            }
            ui.set("#CmdRow" + i + ".Visible", false);
        }
        int msgCount = citizen.messages != null ? citizen.messages.size() : 0;
        ui.set("#MsgHeader.Text", i18n.get("citizen.admin.section.messages", msgCount));
        for (int i = 0; i < 8; ++i) {
            if (i < msgCount) {
                String msg = citizen.messages.get(i);
                String display = msg.length() > 55 ? msg.substring(0, 52) + "..." : msg;
                ui.set("#MsgRow" + i + ".Visible", true);
                ui.set("#MsgText" + i + ".Text", display);
                continue;
            }
            ui.set("#MsgRow" + i + ".Visible", false);
        }
        boolean isPathMovement = "PATH".equals(citizen.movementType);
        ui.set("#WaypointSection.Visible", isPathMovement);
        if (isPathMovement) {
            int wpCount = citizen.waypoints != null ? citizen.waypoints.size() : 0;
            ui.set("#WpCountLabel.Text", i18n.get("citizen.admin.waypoints_count", wpCount));
            ui.set("#EditPathShape.Value", citizen.pathShape != null ? citizen.pathShape : "LOOP");
            if (wpCount > 0) {
                StringBuilder wpListText = new StringBuilder();
                for (int i = 0; i < wpCount; ++i) {
                    CitizenData.WaypointData wp = citizen.waypoints.get(i);
                    wpListText.append("#").append(i + 1).append(": ").append(String.format("%.1f, %.1f, %.1f", wp.x, wp.y, wp.z));
                    if (wp.rotY != 0.0f) {
                        wpListText.append(" rot:").append(String.format("%.0f", Float.valueOf(wp.rotY)));
                    }
                    if (i >= wpCount - 1) continue;
                    wpListText.append("\n");
                }
                ui.set("#WpList.Text", wpListText.toString());
            } else {
                ui.set("#WpList.Text", i18n.get("citizen.admin.no_waypoints"));
            }
        }
        this.readNpcState(ui, citizen);
        this.readRoleInfo(ui, citizen);
        ui.set("#EditPreset.Value", "0");
        int deathCmdCount = citizen.deathCommands != null ? citizen.deathCommands.size() : 0;
        ui.set("#DeathCmdSection.Visible", true);
        ui.set("#DeathCmdHeader.Text", i18n.get("citizen.admin.section.death_commands", deathCmdCount));
        for (int i = 0; i < 4; ++i) {
            if (i < deathCmdCount) {
                CitizenData.CommandAction dcmd = citizen.deathCommands.get(i);
                ui.set("#DeathCmdRow" + i + ".Visible", true);
                ui.set("#DeathCmdBadge" + i + ".Text", dcmd.runAsServer ? "S" : "P");
                ui.set("#DeathCmdBadge" + i + ".Style.TextColor", dcmd.runAsServer ? "#ff6666" : "#4488ff");
                String display = dcmd.command != null ? (dcmd.command.length() > 50 ? dcmd.command.substring(0, 47) + "..." : dcmd.command) : "";
                ui.set("#DeathCmdText" + i + ".Text", display);
                continue;
            }
            ui.set("#DeathCmdRow" + i + ".Visible", false);
        }
    }

    private void readNpcState(UICommandBuilder ui, CitizenData citizen) {
        NPCEntity npcEntity = this.citizenService.getNpcEntity(citizen.id);
        boolean hasState = false;
        if (npcEntity != null) {
            try {
                Role role = npcEntity.getRole();
                if (role != null) {
                    MotionController mc;
                    StateSupport ss = role.getStateSupport();
                    if (ss != null) {
                        ui.set("#StateValue.Text", ss.getStateName());
                        ui.set("#StateBusy.Text", ss.isInBusyState() ? CoreI18n.getInstance().get("citizen.admin.dd.yes") : CoreI18n.getInstance().get("citizen.admin.dd.no"));
                        hasState = true;
                    }
                    ui.set("#StateMotion.Text", (mc = role.getActiveMotionController()) != null ? mc.getType() : CoreI18n.getInstance().get("citizen.admin.none"));
                }
            }
            catch (Exception role) {
                // empty catch block
            }
        }
        if (!hasState) {
            CoreI18n stI18n = CoreI18n.getInstance();
            ui.set("#StateValue.Text", stI18n.get("citizen.admin.state.not_spawned"));
            ui.set("#StateBusy.Text", stI18n.get("citizen.admin.placeholder"));
            ui.set("#StateMotion.Text", stI18n.get("citizen.admin.placeholder"));
        }
        ui.set("#StateSection.Visible", this.selectedCitizenId != null);
    }

    private void readRoleInfo(UICommandBuilder ui, CitizenData citizen) {
        NPCEntity npcEntity = this.citizenService.getNpcEntity(citizen.id);
        boolean hasInfo = false;
        if (npcEntity != null) {
            try {
                Role role = npcEntity.getRole();
                if (role != null) {
                    ui.set("#RoleInfoName.Text", npcEntity.getRoleName());
                    ui.set("#RoleInfoHp.Text", String.valueOf(role.getInitialMaxHealth()));
                    ui.set("#RoleInfoInertia.Text", String.format("%.2f", role.getInertia()));
                    ui.set("#RoleInfoInvuln.Text", role.isInvulnerable() ? CoreI18n.getInstance().get("citizen.admin.dd.yes") : CoreI18n.getInstance().get("citizen.admin.dd.no"));
                    ui.set("#RoleInfoCollision.Text", String.format("%.2f", role.getCollisionRadius()));
                    String dropList = role.getDropListId();
                    ui.set("#RoleInfoDrop.Text", dropList != null ? dropList : CoreI18n.getInstance().get("citizen.admin.none"));
                    hasInfo = true;
                }
            }
            catch (Exception role) {
                // empty catch block
            }
        }
        if (!hasInfo) {
            CoreI18n riI18n = CoreI18n.getInstance();
            String ph = riI18n.get("citizen.admin.placeholder");
            ui.set("#RoleInfoName.Text", citizen.resolveRoleName());
            ui.set("#RoleInfoHp.Text", ph);
            ui.set("#RoleInfoInertia.Text", ph);
            ui.set("#RoleInfoInvuln.Text", ph);
            ui.set("#RoleInfoCollision.Text", ph);
            ui.set("#RoleInfoDrop.Text", ph);
        }
        ui.set("#RoleInfoSection.Visible", this.selectedCitizenId != null);
    }

    private void handlePresetApply(String presetIdx) {
        CitizenData citizen;
        CitizenData citizenData = citizen = this.selectedCitizenId != null ? this.citizenService.getCitizen(this.selectedCitizenId) : null;
        if (citizen == null || presetIdx == null) {
            return;
        }
        int idx = CitizenAdminPage.parseIntSafe(presetIdx, 0);
        if (idx <= 0 || idx >= PRESETS.size()) {
            return;
        }
        BehaviorPreset p = PRESETS.get(idx);
        citizen.npcRoleId = p.roleId();
        if (p.attitude() != null) {
            citizen.attitude = p.attitude();
        }
        if (p.movement() != null) {
            citizen.movementType = p.movement();
        }
        if (p.radius() > 0.0f) {
            citizen.movementRadius = p.radius();
        }
        if (p.speed() > 0.0f) {
            citizen.walkSpeed = p.speed();
        }
        this.citizenService.updateCitizen(citizen);
        Universe universe = Universe.get();
        World world = universe.getDefaultWorld();
        world.execute(() -> {
            this.citizenService.despawnCitizen(citizen, world);
            this.citizenService.spawnCitizen(citizen, world);
        });
    }

    private void buildDeleteConfirm(UICommandBuilder ui) {
        ui.set("#DeleteConfirmOverlay.Visible", this.showingDeleteConfirm);
        if (this.showingDeleteConfirm && this.selectedCitizenId != null) {
            ui.set("#DeleteConfirmText.Text", CoreI18n.getInstance().get("citizen.admin.delete_confirm", this.selectedCitizenId));
        }
    }

    private void buildOverlays(UICommandBuilder ui) {
        CitizenData citizen = this.selectedCitizenId != null ? this.citizenService.getCitizen(this.selectedCitizenId) : null;
        boolean showAnimEdit = this.editingAnimIndex >= -1 && this.editingAnimIndex != -2;
        ui.set("#AnimEditOverlay.Visible", showAnimEdit);
        if (showAnimEdit && citizen != null) {
            CitizenData.AnimationConfig anim;
            CoreI18n aeI18n = CoreI18n.getInstance();
            ui.set("#AnimEditTriggerDD.Entries", List.of(new DropdownEntryInfo(LocalizableString.fromString((String)aeI18n.get("citizen.admin.dd.trigger.on_interact")), "ON_INTERACT"), new DropdownEntryInfo(LocalizableString.fromString((String)aeI18n.get("citizen.admin.dd.trigger.timed")), "TIMED"), new DropdownEntryInfo(LocalizableString.fromString((String)aeI18n.get("citizen.admin.dd.trigger.timed_random")), "TIMED_RANDOM"), new DropdownEntryInfo(LocalizableString.fromString((String)aeI18n.get("citizen.admin.dd.trigger.proximity")), "PROXIMITY"), new DropdownEntryInfo(LocalizableString.fromString((String)aeI18n.get("citizen.admin.dd.trigger.on_attack")), "ON_ATTACK"), new DropdownEntryInfo(LocalizableString.fromString((String)aeI18n.get("citizen.admin.dd.trigger.default")), "DEFAULT")));
            ui.set("#AnimEditSlotDD.Entries", List.of(new DropdownEntryInfo(LocalizableString.fromString((String)aeI18n.get("citizen.admin.dd.slot.action")), "Action"), new DropdownEntryInfo(LocalizableString.fromString((String)aeI18n.get("citizen.admin.dd.slot.movement")), "Movement"), new DropdownEntryInfo(LocalizableString.fromString((String)aeI18n.get("citizen.admin.dd.slot.status")), "Status"), new DropdownEntryInfo(LocalizableString.fromString((String)aeI18n.get("citizen.admin.dd.slot.face")), "Face"), new DropdownEntryInfo(LocalizableString.fromString((String)aeI18n.get("citizen.admin.dd.slot.emote")), "Emote")));
            ui.set("#AnimEditStopDD.Entries", List.of(new DropdownEntryInfo(LocalizableString.fromString((String)aeI18n.get("citizen.admin.dd.no")), "No"), new DropdownEntryInfo(LocalizableString.fromString((String)aeI18n.get("citizen.admin.dd.yes")), "Yes")));
            this.collectAnimations(citizen);
            if (this.autoDetectedSlot != null) {
                this.currentSlotFilter = this.autoDetectedSlot;
                this.autoDetectedSlot = null;
            }
            if (this.editingAnimIndex >= 0 && citizen.animations != null && this.editingAnimIndex < citizen.animations.size()) {
                anim = citizen.animations.get(this.editingAnimIndex);
                ui.set("#AnimEditTriggerDD.Value", anim.type != null ? anim.type : "ON_INTERACT");
                if (this.currentSlotFilter == null) {
                    this.currentSlotFilter = CitizenAdminPage.resolveSlotName(anim.animationSlot);
                }
                if (this.currentAnimName == null) {
                    this.currentAnimName = anim.animationName != null ? anim.animationName : "";
                }
                ui.set("#AnimEditNameDD.Value", this.currentAnimName);
                ui.set("#AnimEditIntervalField.Value", String.format("%.1f", Float.valueOf(anim.interval)));
                ui.set("#AnimEditProxRangeField.Value", String.format("%.1f", Float.valueOf(anim.proximityRange)));
                ui.set("#AnimEditStopDD.Value", anim.stopAfterTime ? "Yes" : "No");
                ui.set("#AnimEditStopTimeField.Value", String.format("%.1f", Float.valueOf(anim.stopTime)));
                ui.set("#AnimEditStopAnimDD.Value", anim.stopAnimation != null ? anim.stopAnimation : "");
            } else {
                ui.set("#AnimEditTriggerDD.Value", "ON_INTERACT");
                if (this.currentSlotFilter == null) {
                    this.currentSlotFilter = "Action";
                }
                if (this.currentAnimName == null) {
                    this.currentAnimName = "Idle";
                }
                ui.set("#AnimEditNameDD.Value", this.currentAnimName);
                ui.set("#AnimEditIntervalField.Value", "5.0");
                ui.set("#AnimEditProxRangeField.Value", "8.0");
                ui.set("#AnimEditStopDD.Value", "No");
                ui.set("#AnimEditStopTimeField.Value", "3.0");
                ui.set("#AnimEditStopAnimDD.Value", "");
            }
            ui.set("#AnimEditSlotDD.Value", this.currentSlotFilter);
            anim = this.editingAnimIndex >= 0 && citizen.animations != null && this.editingAnimIndex < citizen.animations.size() ? citizen.animations.get(this.editingAnimIndex) : null;
            String existingAnimName = anim != null ? anim.animationName : null;
            ui.set("#AnimEditNameDD.Entries", this.filteredAnimEntries(existingAnimName));
            ui.set("#AnimEditStopAnimDD.Entries", this.allAnimEntries());
        }
        boolean showCmdEdit = this.editingCmdIndex >= 0;
        ui.set("#CmdEditOverlay.Visible", showCmdEdit);
        if (showCmdEdit && citizen != null && citizen.commandActions != null && this.editingCmdIndex < citizen.commandActions.size()) {
            CitizenData.CommandAction cmd = citizen.commandActions.get(this.editingCmdIndex);
            ui.set("#CmdEditOverlayField.Value", cmd.command != null ? cmd.command : "");
            CoreI18n ceI18n = CoreI18n.getInstance();
            ui.set("#CmdEditRunMode.Entries", List.of(new DropdownEntryInfo(LocalizableString.fromString((String)ceI18n.get("citizen.admin.dd.server")), "Server"), new DropdownEntryInfo(LocalizableString.fromString((String)ceI18n.get("citizen.admin.dd.player")), "Player")));
            ui.set("#CmdEditRunMode.Value", cmd.runAsServer ? "Server" : "Player");
        }
        boolean showMsgEdit = this.editingMsgIndex >= 0;
        ui.set("#MsgEditOverlay.Visible", showMsgEdit);
        if (showMsgEdit && citizen != null && citizen.messages != null && this.editingMsgIndex < citizen.messages.size()) {
            ui.set("#MsgEditOverlayField.Value", citizen.messages.get(this.editingMsgIndex));
        }
    }

    private void filterCitizens() {
        Collection<CitizenData> all = this.citizenService.getAllCitizens();
        String query = this.searchQuery != null ? this.searchQuery.trim().toLowerCase() : "";
        this.filteredCitizens = all.stream().filter(c -> {
            if (!("all".equals(this.groupFilter) || c.group != null && this.groupFilter.equals(c.group))) {
                return false;
            }
            if (!query.isEmpty()) {
                String id = c.id != null ? c.id.toLowerCase() : "";
                String name = c.name != null ? c.name.toLowerCase() : "";
                return id.contains(query) || name.contains(query);
            }
            return true;
        }).sorted(Comparator.comparing(c -> c.id != null ? c.id : "")).collect(Collectors.toList());
    }

    private static String resolveSlotName(int slot) {
        return switch (slot) {
            case 0 -> "Movement";
            case 1 -> "Status";
            case 2 -> "Action";
            case 3 -> "Face";
            case 4 -> "Emote";
            default -> "Action";
        };
    }

    private static int resolveSlotIndex(String name) {
        if (name == null) {
            return 2;
        }
        return switch (name) {
            case "Movement" -> 0;
            case "Status" -> 1;
            case "Action" -> 2;
            case "Face" -> 3;
            case "Emote" -> 4;
            default -> 2;
        };
    }

    private void collectAnimations(CitizenData citizen) {
        TreeSet modelAnims = new TreeSet(String.CASE_INSENSITIVE_ORDER);
        TreeSet<String> emoteAnims = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        String modelId = citizen.isPlayerModel ? "Player" : (citizen.entityTypeId != null ? citizen.entityTypeId : "Player");
        try {
            ModelAsset model = (ModelAsset)ModelAsset.getAssetMap().getAsset((Object)modelId);
            if (model != null && model.getAnimationSetMap() != null) {
                modelAnims.addAll(model.getAnimationSetMap().keySet());
            }
        }
        catch (Exception e) {
            LOGGER.warning("[CitizenAdmin] Failed to load animations for model " + modelId + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        if ("Player".equals(modelId)) {
            try {
                CosmeticsModule module = CosmeticsModule.get();
                if (module == null) {
                    LOGGER.warning("[CitizenAdmin] CosmeticsModule.get() returned null \u2014 emotes unavailable");
                } else {
                    CosmeticRegistry registry = module.getRegistry();
                    if (registry == null) {
                        LOGGER.warning("[CitizenAdmin] CosmeticsModule.getRegistry() returned null \u2014 emotes unavailable");
                    } else {
                        Map emotes = registry.getEmotes();
                        if (emotes != null) {
                            for (Emote emote : emotes.values()) {
                                emoteAnims.add(emote.getId());
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                LOGGER.warning("[CitizenAdmin] Failed to load emotes: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        this.allModelAnimNames = new ArrayList<String>(modelAnims);
        this.allEmoteAnimNames = new ArrayList<String>(emoteAnims);
        TreeSet<Object> combined = new TreeSet<Object>(String.CASE_INSENSITIVE_ORDER);
        combined.addAll(modelAnims);
        combined.addAll(emoteAnims);
        this.allAnimNames = new ArrayList<String>(combined);
        this.animSlotHints.clear();
        for (String name : emoteAnims) {
            this.animSlotHints.put(name, "Emote");
        }
    }

    private List<DropdownEntryInfo> allAnimEntries() {
        ArrayList<DropdownEntryInfo> entries = new ArrayList<DropdownEntryInfo>();
        entries.add(new DropdownEntryInfo(LocalizableString.fromString((String)CoreI18n.getInstance().get("citizen.admin.dd.none")), ""));
        for (String name : this.allAnimNames) {
            entries.add(new DropdownEntryInfo(LocalizableString.fromString((String)name), name));
        }
        return entries;
    }

    private List<DropdownEntryInfo> filteredAnimEntries(String existingAnimName) {
        ArrayList<DropdownEntryInfo> entries = new ArrayList<DropdownEntryInfo>();
        entries.add(new DropdownEntryInfo(LocalizableString.fromString((String)CoreI18n.getInstance().get("citizen.admin.dd.none")), ""));
        List<String> source = "Emote".equals(this.currentSlotFilter) ? this.allEmoteAnimNames : this.allAnimNames;
        HashSet<String> added = new HashSet<String>();
        for (String name : source) {
            entries.add(new DropdownEntryInfo(LocalizableString.fromString((String)name), name));
            added.add(name);
        }
        if (existingAnimName != null && !existingAnimName.isEmpty() && !added.contains(existingAnimName)) {
            entries.add(new DropdownEntryInfo(LocalizableString.fromString((String)existingAnimName), existingAnimName));
        }
        return entries;
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

    private void loadLocNameValues() {
        this.locNameValues.clear();
        if (this.selectedCitizenId == null) {
            return;
        }
        CoreI18n i18n = CoreI18n.getInstance();
        String key = "citizen.name." + this.selectedCitizenId;
        for (String lang : LOC_LANGUAGES) {
            String val;
            String string = val = i18n.has(key) ? i18n.get(key) : "";
            if (val.equals(key)) {
                val = "";
            }
            this.locNameValues.put(lang, val);
        }
    }

    private void saveLocNameValues() {
        if (this.selectedCitizenId == null) {
            return;
        }
        CoreI18n i18n = CoreI18n.getInstance();
        String key = "citizen.name." + this.selectedCitizenId;
        for (String lang : LOC_LANGUAGES) {
            String val = this.locNameValues.get(lang);
            if (val == null || val.isEmpty()) continue;
            i18n.setCustomTranslation(lang, key, val);
        }
    }

    private void buildLocEditorOverlay(UICommandBuilder ui) {
        ui.set("#LocEditorOverlay.Visible", this.locEditorOpen);
        if (!this.locEditorOpen) {
            return;
        }
        ui.set("#LocEdTitle.Text", CoreI18n.getInstance().get("citizen.admin.loc_editor_title"));
        for (int i = 0; i < LOC_LANGUAGES.length; ++i) {
            ui.set("#LocEdLang" + i + ".Text", LOC_LANGUAGES[i]);
            String val = this.locNameValues.getOrDefault(LOC_LANGUAGES[i], "");
            ui.set("#LocEdField" + i + ".Value", val != null ? val : "");
        }
    }

    private static String getRoleDescription(String roleName) {
        if (roleName == null || roleName.isEmpty()) {
            return "";
        }
        CoreI18n i18n = CoreI18n.getInstance();
        return switch (roleName) {
            case "Empty_Role" -> i18n.get("citizen.admin.role_desc.empty");
            case "KS_NPC_Interactable_Role" -> i18n.get("citizen.admin.role_desc.idle_interact");
            case "KS_NPC_WanderI_R2_Interactable_Role" -> i18n.get("citizen.admin.role_desc.wander_r2_interact");
            case "KS_NPC_Wander_R2_Role" -> i18n.get("citizen.admin.role_desc.wander_r2");
            case "KS_NPC_WanderI_R5_Interactable_Role" -> i18n.get("citizen.admin.role_desc.wander_r5_interact");
            case "KS_NPC_Wander_R5_Role" -> i18n.get("citizen.admin.role_desc.wander_r5");
            case "KS_NPC_WanderI_R10_Interactable_Role" -> i18n.get("citizen.admin.role_desc.wander_r10_interact");
            case "KS_NPC_Wander_R10_Role" -> i18n.get("citizen.admin.role_desc.wander_r10");
            case "KS_NPC_WanderI_R15_Interactable_Role" -> i18n.get("citizen.admin.role_desc.wander_r15_interact");
            case "KS_NPC_Wander_R15_Role" -> i18n.get("citizen.admin.role_desc.wander_r15");
            case "KS_NPC_Idle_Role" -> i18n.get("citizen.admin.role_desc.idle");
            case "KS_Path_Role" -> i18n.get("citizen.admin.role_desc.path");
            case "KS_Path_Interactable_Role" -> i18n.get("citizen.admin.role_desc.path_interact");
            default -> i18n.get("citizen.admin.role_desc.custom", roleName);
        };
    }

    private static String emptyToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String formatAttachments(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        return map.entrySet().stream().map(e -> (String)e.getKey() + ":" + (String)e.getValue()).collect(Collectors.joining(", "));
    }

    private static Map<String, String> parseAttachments(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        for (String pair : text.split(",")) {
            String[] parts = pair.trim().split(":", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) continue;
            map.put(parts[0].trim(), parts[1].trim());
        }
        return map.isEmpty() ? null : map;
    }

    public static class PageEventData {
        public static final BuilderCodec<PageEventData> CODEC = ((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)BuilderCodec.builder(PageEventData.class, PageEventData::new).addField(new KeyedCodec("Button", (Codec)Codec.STRING), (d, v) -> {
            d.button = v;
        }, d -> d.button)).addField(new KeyedCodec("@GroupFilter", (Codec)Codec.STRING), (d, v) -> {
            d.groupFilter = v;
        }, d -> d.groupFilter)).addField(new KeyedCodec("@Search", (Codec)Codec.STRING), (d, v) -> {
            d.search = v;
        }, d -> d.search)).addField(new KeyedCodec("@EditId", (Codec)Codec.STRING), (d, v) -> {
            d.editId = v;
        }, d -> d.editId)).addField(new KeyedCodec("@EditName", (Codec)Codec.STRING), (d, v) -> {
            d.editName = v;
        }, d -> d.editName)).addField(new KeyedCodec("@EditGroup", (Codec)Codec.STRING), (d, v) -> {
            d.editGroup = v;
        }, d -> d.editGroup)).addField(new KeyedCodec("@EditWorld", (Codec)Codec.STRING), (d, v) -> {
            d.editWorld = v;
        }, d -> d.editWorld)).addField(new KeyedCodec("@EditPosX", (Codec)Codec.STRING), (d, v) -> {
            d.editPosX = v;
        }, d -> d.editPosX)).addField(new KeyedCodec("@EditPosY", (Codec)Codec.STRING), (d, v) -> {
            d.editPosY = v;
        }, d -> d.editPosY)).addField(new KeyedCodec("@EditPosZ", (Codec)Codec.STRING), (d, v) -> {
            d.editPosZ = v;
        }, d -> d.editPosZ)).addField(new KeyedCodec("@EditSkin", (Codec)Codec.STRING), (d, v) -> {
            d.editSkin = v;
        }, d -> d.editSkin)).addField(new KeyedCodec("@EditScale", (Codec)Codec.STRING), (d, v) -> {
            d.editScale = v;
        }, d -> d.editScale)).addField(new KeyedCodec("@EditNametagOff", (Codec)Codec.STRING), (d, v) -> {
            d.editNametagOff = v;
        }, d -> d.editNametagOff)).addField(new KeyedCodec("@EditPermission", (Codec)Codec.STRING), (d, v) -> {
            d.editPermission = v;
        }, d -> d.editPermission)).addField(new KeyedCodec("@EditNoPermMsg", (Codec)Codec.STRING), (d, v) -> {
            d.editNoPermMsg = v;
        }, d -> d.editNoPermMsg)).addField(new KeyedCodec("@EditRespawnDelay", (Codec)Codec.STRING), (d, v) -> {
            d.editRespawnDelay = v;
        }, d -> d.editRespawnDelay)).addField(new KeyedCodec("@EditMovRadius", (Codec)Codec.STRING), (d, v) -> {
            d.editMovRadius = v;
        }, d -> d.editMovRadius)).addField(new KeyedCodec("@EditWalkSpeed", (Codec)Codec.STRING), (d, v) -> {
            d.editWalkSpeed = v;
        }, d -> d.editWalkSpeed)).addField(new KeyedCodec("@EditHelmet", (Codec)Codec.STRING), (d, v) -> {
            d.editHelmet = v;
        }, d -> d.editHelmet)).addField(new KeyedCodec("@EditChest", (Codec)Codec.STRING), (d, v) -> {
            d.editChest = v;
        }, d -> d.editChest)).addField(new KeyedCodec("@EditLeggings", (Codec)Codec.STRING), (d, v) -> {
            d.editLeggings = v;
        }, d -> d.editLeggings)).addField(new KeyedCodec("@EditGloves", (Codec)Codec.STRING), (d, v) -> {
            d.editGloves = v;
        }, d -> d.editGloves)).addField(new KeyedCodec("@EditMainHand", (Codec)Codec.STRING), (d, v) -> {
            d.editMainHand = v;
        }, d -> d.editMainHand)).addField(new KeyedCodec("@EditOffHand", (Codec)Codec.STRING), (d, v) -> {
            d.editOffHand = v;
        }, d -> d.editOffHand)).addField(new KeyedCodec("@EditDialog", (Codec)Codec.STRING), (d, v) -> {
            d.editDialog = v;
        }, d -> d.editDialog)).addField(new KeyedCodec("@EditShop", (Codec)Codec.STRING), (d, v) -> {
            d.editShop = v;
        }, d -> d.editShop)).addField(new KeyedCodec("@EditMsgDelay", (Codec)Codec.STRING), (d, v) -> {
            d.editMsgDelay = v;
        }, d -> d.editMsgDelay)).addField(new KeyedCodec("@EditSpawnFx", (Codec)Codec.STRING), (d, v) -> {
            d.editSpawnFx = v;
        }, d -> d.editSpawnFx)).addField(new KeyedCodec("@EditSpawnFxDur", (Codec)Codec.STRING), (d, v) -> {
            d.editSpawnFxDur = v;
        }, d -> d.editSpawnFxDur)).addField(new KeyedCodec("@EditDespawnFx", (Codec)Codec.STRING), (d, v) -> {
            d.editDespawnFx = v;
        }, d -> d.editDespawnFx)).addField(new KeyedCodec("@EditDespawnFxDur", (Codec)Codec.STRING), (d, v) -> {
            d.editDespawnFxDur = v;
        }, d -> d.editDespawnFxDur)).addField(new KeyedCodec("@EditAttachments", (Codec)Codec.STRING), (d, v) -> {
            d.editAttachments = v;
        }, d -> d.editAttachments)).addField(new KeyedCodec("@EditGradientSet", (Codec)Codec.STRING), (d, v) -> {
            d.editGradientSet = v;
        }, d -> d.editGradientSet)).addField(new KeyedCodec("@EditGradientId", (Codec)Codec.STRING), (d, v) -> {
            d.editGradientId = v;
        }, d -> d.editGradientId)).addField(new KeyedCodec("@EditNametagColor", (Codec)Codec.STRING), (d, v) -> {
            d.editNametagColor = v;
        }, d -> d.editNametagColor)).addField(new KeyedCodec("@EditNametagFmt", (Codec)Codec.STRING), (d, v) -> {
            d.editNametagFmt = v;
        }, d -> d.editNametagFmt)).addField(new KeyedCodec("@ModelType", (Codec)Codec.STRING), (d, v) -> {
            d.modelType = v;
        }, d -> d.modelType)).addField(new KeyedCodec("@EntityType", (Codec)Codec.STRING), (d, v) -> {
            d.entityType = v;
        }, d -> d.entityType)).addField(new KeyedCodec("@Attitude", (Codec)Codec.STRING), (d, v) -> {
            d.attitude = v;
        }, d -> d.attitude)).addField(new KeyedCodec("@Movement", (Codec)Codec.STRING), (d, v) -> {
            d.movement = v;
        }, d -> d.movement)).addField(new KeyedCodec("@Rotate", (Codec)Codec.STRING), (d, v) -> {
            d.rotate = v;
        }, d -> d.rotate)).addField(new KeyedCodec("@Invulnerable", (Codec)Codec.STRING), (d, v) -> {
            d.invulnerable = v;
        }, d -> d.invulnerable)).addField(new KeyedCodec("@TakesDamage", (Codec)Codec.STRING), (d, v) -> {
            d.takesDamage = v;
        }, d -> d.takesDamage)).addField(new KeyedCodec("@Respawn", (Codec)Codec.STRING), (d, v) -> {
            d.respawn = v;
        }, d -> d.respawn)).addField(new KeyedCodec("@FKey", (Codec)Codec.STRING), (d, v) -> {
            d.fKey = v;
        }, d -> d.fKey)).addField(new KeyedCodec("@HideNpc", (Codec)Codec.STRING), (d, v) -> {
            d.hideNpc = v;
        }, d -> d.hideNpc)).addField(new KeyedCodec("@BankingEnabled", (Codec)Codec.STRING), (d, v) -> {
            d.bankingEnabled = v;
        }, d -> d.bankingEnabled)).addField(new KeyedCodec("@HideNametag", (Codec)Codec.STRING), (d, v) -> {
            d.hideNametag = v;
        }, d -> d.hideNametag)).addField(new KeyedCodec("@LiveSkin", (Codec)Codec.STRING), (d, v) -> {
            d.liveSkin = v;
        }, d -> d.liveSkin)).addField(new KeyedCodec("@MsgMode", (Codec)Codec.STRING), (d, v) -> {
            d.msgMode = v;
        }, d -> d.msgMode)).addField(new KeyedCodec("@SpawnRelative", (Codec)Codec.STRING), (d, v) -> {
            d.spawnRelative = v;
        }, d -> d.spawnRelative)).addField(new KeyedCodec("@NpcRole", (Codec)Codec.STRING), (d, v) -> {
            d.npcRole = v;
        }, d -> d.npcRole)).addField(new KeyedCodec("@MsgNew", (Codec)Codec.STRING), (d, v) -> {
            d.msgNew = v;
        }, d -> d.msgNew)).addField(new KeyedCodec("@CmdNew", (Codec)Codec.STRING), (d, v) -> {
            d.cmdNew = v;
        }, d -> d.cmdNew)).addField(new KeyedCodec("@CmdRunMode", (Codec)Codec.STRING), (d, v) -> {
            d.cmdRunMode = v;
        }, d -> d.cmdRunMode)).addField(new KeyedCodec("@CmdEditText", (Codec)Codec.STRING), (d, v) -> {
            d.cmdEditText = v;
        }, d -> d.cmdEditText)).addField(new KeyedCodec("@CmdEditMode", (Codec)Codec.STRING), (d, v) -> {
            d.cmdEditMode = v;
        }, d -> d.cmdEditMode)).addField(new KeyedCodec("@AnimNameChanged", (Codec)Codec.STRING), (d, v) -> {
            d.animNameChanged = v;
        }, d -> d.animNameChanged)).addField(new KeyedCodec("@SlotChanged", (Codec)Codec.STRING), (d, v) -> {
            d.slotChanged = v;
        }, d -> d.slotChanged)).addField(new KeyedCodec("@AnimEditTrigger", (Codec)Codec.STRING), (d, v) -> {
            d.animEditTrigger = v;
        }, d -> d.animEditTrigger)).addField(new KeyedCodec("@AnimEditName", (Codec)Codec.STRING), (d, v) -> {
            d.animEditName = v;
        }, d -> d.animEditName)).addField(new KeyedCodec("@AnimEditSlot", (Codec)Codec.STRING), (d, v) -> {
            d.animEditSlot = v;
        }, d -> d.animEditSlot)).addField(new KeyedCodec("@AnimEditInterval", (Codec)Codec.STRING), (d, v) -> {
            d.animEditInterval = v;
        }, d -> d.animEditInterval)).addField(new KeyedCodec("@AnimEditProxRange", (Codec)Codec.STRING), (d, v) -> {
            d.animEditProxRange = v;
        }, d -> d.animEditProxRange)).addField(new KeyedCodec("@AnimEditStopEnabled", (Codec)Codec.STRING), (d, v) -> {
            d.animEditStopEnabled = v;
        }, d -> d.animEditStopEnabled)).addField(new KeyedCodec("@AnimEditStopTime", (Codec)Codec.STRING), (d, v) -> {
            d.animEditStopTime = v;
        }, d -> d.animEditStopTime)).addField(new KeyedCodec("@AnimEditStopAnim", (Codec)Codec.STRING), (d, v) -> {
            d.animEditStopAnim = v;
        }, d -> d.animEditStopAnim)).addField(new KeyedCodec("@MsgEditText", (Codec)Codec.STRING), (d, v) -> {
            d.msgEditText = v;
        }, d -> d.msgEditText)).addField(new KeyedCodec("@PathShape", (Codec)Codec.STRING), (d, v) -> {
            d.pathShape = v;
        }, d -> d.pathShape)).addField(new KeyedCodec("@DeathCmdNew", (Codec)Codec.STRING), (d, v) -> {
            d.deathCmdNew = v;
        }, d -> d.deathCmdNew)).addField(new KeyedCodec("@DeathCmdRunMode", (Codec)Codec.STRING), (d, v) -> {
            d.deathCmdRunMode = v;
        }, d -> d.deathCmdRunMode)).addField(new KeyedCodec("@GradSetChanged", (Codec)Codec.STRING), (d, v) -> {
            d.gradSetChanged = v;
        }, d -> d.gradSetChanged)).addField(new KeyedCodec("@Preset", (Codec)Codec.STRING), (d, v) -> {
            d.preset = v;
        }, d -> d.preset)).addField(new KeyedCodec("@LocEdField0", (Codec)Codec.STRING), (d, v) -> {
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
        }, d -> d.locEdFields[8])).build();
        String button;
        String groupFilter;
        String search;
        String editId;
        String editName;
        String editGroup;
        String editWorld;
        String editPosX;
        String editPosY;
        String editPosZ;
        String editSkin;
        String editScale;
        String editNametagOff;
        String editPermission;
        String editNoPermMsg;
        String editRespawnDelay;
        String editMovRadius;
        String editWalkSpeed;
        String editHelmet;
        String editChest;
        String editLeggings;
        String editGloves;
        String editMainHand;
        String editOffHand;
        String editDialog;
        String editShop;
        String editMsgDelay;
        String editSpawnFx;
        String editSpawnFxDur;
        String editDespawnFx;
        String editDespawnFxDur;
        String editAttachments;
        String editGradientSet;
        String editGradientId;
        String editNametagColor;
        String editNametagFmt;
        String modelType;
        String entityType;
        String attitude;
        String movement;
        String rotate;
        String invulnerable;
        String takesDamage;
        String respawn;
        String fKey;
        String hideNpc;
        String bankingEnabled;
        String hideNametag;
        String liveSkin;
        String msgMode;
        String spawnRelative;
        String npcRole;
        String pathShape;
        String msgNew;
        String cmdNew;
        String cmdRunMode;
        String cmdEditText;
        String cmdEditMode;
        String deathCmdNew;
        String deathCmdRunMode;
        String animNameChanged;
        String slotChanged;
        String animEditTrigger;
        String animEditName;
        String animEditSlot;
        String animEditInterval;
        String animEditProxRange;
        String animEditStopEnabled;
        String animEditStopTime;
        String animEditStopAnim;
        String msgEditText;
        String gradSetChanged;
        String preset;
        String[] locEdFields = new String[9];
    }

    record BehaviorPreset(String i18nKey, String roleId, String attitude, String movement, float radius, float speed) {
    }
}

