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
package com.kyuubisoft.core.dialog;

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
import com.kyuubisoft.core.dialog.DialogChoice;
import com.kyuubisoft.core.dialog.DialogMacro;
import com.kyuubisoft.core.dialog.DialogNode;
import com.kyuubisoft.core.dialog.DialogService;
import com.kyuubisoft.core.dialog.DialogTree;
import com.kyuubisoft.core.i18n.I18nContext;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class DialogEditorPage
extends InteractiveCustomUIPage<PageEventData> {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft DialogEditor");
    private static final int DLGS_PER_PAGE = 8;
    private static final int NODES_PER_PAGE = 8;
    private static final int MAX_LINES = 4;
    private static final int MAX_CHOICES = 6;
    private final CorePlugin plugin;
    private final Player player;
    private final PlayerRef playerRef;
    private final DialogService dialogService;
    private String selectedDialogId;
    private int selectedNodeIndex = -1;
    private int dialogPage = 0;
    private int nodePage = 0;
    private int editingChoiceIndex = -2;
    private List<DialogTree> allDialogs = new ArrayList<DialogTree>();
    private Ref<EntityStore> storedRef;
    private Store<EntityStore> storedStore;

    public DialogEditorPage(CorePlugin plugin, Player player, PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageEventData.CODEC);
        this.plugin = plugin;
        this.player = player;
        this.playerRef = playerRef;
        this.dialogService = DialogService.getInstance();
    }

    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder ui, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        I18nContext.run(this.playerRef, () -> {
            this.storedRef = ref;
            this.storedStore = store;
            ui.append("Pages/Dialog/DialogEditor.ui");
            try {
                this.reloadDialogs();
                this.buildAll(ui, events);
            }
            catch (Exception e) {
                LOGGER.log(Level.SEVERE, "DialogEditor build() failed", e);
            }
        });
    }

    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageEventData data) {
        super.handleDataEvent(ref, store, (Object)data);
        I18nContext.run(this.playerRef, () -> {
            boolean hasTextFieldData;
            this.storedRef = ref;
            this.storedStore = store;
            if (data.button != null) {
                this.handleButton(data.button, data);
                return;
            }
            if (data.nodeType != null) {
                if (CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) {
                    this.sendUpdate(new UICommandBuilder(), false);
                    return;
                }
                DialogNode node = this.getSelectedNode();
                if (node != null) {
                    node.type = data.nodeType;
                    if ("CHOICE".equals(data.nodeType) && node.choices == null) {
                        node.choices = new ArrayList<DialogChoice>();
                    }
                }
                this.refreshUI();
                return;
            }
            if (data.macroServer != null) {
                if (CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) {
                    this.sendUpdate(new UICommandBuilder(), false);
                    return;
                }
                DialogNode node = this.getSelectedNode();
                if (node != null) {
                    if (node.macro == null) {
                        node.macro = new DialogMacro();
                    }
                    node.macro.runAsServer = "true".equals(data.macroServer);
                }
                this.refreshUI();
                return;
            }
            boolean bl = hasTextFieldData = data.speaker != null || data.startNode != null || data.nodeId != null || data.nextNode != null || data.line0 != null || data.line1 != null || data.line2 != null || data.line3 != null || data.macroCmds != null;
            if (hasTextFieldData && CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) {
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            this.handleTextFields(data);
            this.refreshUI();
        });
    }

    private void handleButton(String button, PageEventData data) {
        switch (button) {
            case "newDlg": 
            case "deleteDlg": 
            case "addNode": 
            case "deleteNode": 
            case "save": 
            case "addChoice": 
            case "chEditSave": {
                if (!CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) break;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            default: {
                if (!button.startsWith("chDel_") || !CoreAPI.showcaseWriteGuard(this.player, this.playerRef)) break;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
        }
        switch (button) {
            case "close": {
                CitizenAdminPage adminPage = new CitizenAdminPage(this.plugin, this.player, this.playerRef);
                this.player.getPageManager().openCustomPage(this.storedRef, this.storedStore, (CustomUIPage)adminPage);
                return;
            }
            case "dlgPrev": {
                if (this.dialogPage <= 0) break;
                --this.dialogPage;
                break;
            }
            case "dlgNext": {
                int totalDlgPages = Math.max(1, (int)Math.ceil((double)this.allDialogs.size() / 8.0));
                if (this.dialogPage >= totalDlgPages - 1) break;
                ++this.dialogPage;
                break;
            }
            case "nodePrev": {
                if (this.nodePage <= 0) break;
                --this.nodePage;
                break;
            }
            case "nodeNext": {
                DialogTree tree = this.getSelectedDialog();
                int nodeCount = tree != null && tree.nodes != null ? tree.nodes.size() : 0;
                int totalNodePages = Math.max(1, (int)Math.ceil((double)nodeCount / 8.0));
                if (this.nodePage >= totalNodePages - 1) break;
                ++this.nodePage;
                break;
            }
            case "newDlg": {
                this.handleNewDialog(data.newDlgId);
                break;
            }
            case "deleteDlg": {
                if (this.selectedDialogId == null) break;
                this.dialogService.deleteDialog(this.selectedDialogId);
                this.selectedDialogId = null;
                this.selectedNodeIndex = -1;
                this.reloadDialogs();
                break;
            }
            case "addNode": {
                DialogTree tree = this.getSelectedDialog();
                if (tree == null) break;
                DialogNode node = new DialogNode();
                tree.nodes = DialogEditorPage.ensureMutable(tree.nodes);
                node.nodeId = "node_" + tree.nodes.size();
                node.type = "TEXT";
                node.lines = new ArrayList<String>();
                node.lines.add("...");
                tree.nodes.add(node);
                this.selectedNodeIndex = tree.nodes.size() - 1;
                this.nodePage = Math.max(0, (tree.nodes.size() - 1) / 8);
                break;
            }
            case "deleteNode": {
                DialogTree tree = this.getSelectedDialog();
                if (tree == null || tree.nodes == null || this.selectedNodeIndex < 0 || this.selectedNodeIndex >= tree.nodes.size()) break;
                tree.nodes = DialogEditorPage.ensureMutable(tree.nodes);
                tree.nodes.remove(this.selectedNodeIndex);
                this.selectedNodeIndex = -1;
                break;
            }
            case "save": {
                this.handleSave(data);
                break;
            }
            case "addChoice": {
                DialogNode node = this.getSelectedNode();
                if (node == null) break;
                node.choices = DialogEditorPage.ensureMutable(node.choices);
                this.editingChoiceIndex = -1;
                break;
            }
            case "chEditSave": {
                this.handleChoiceEditSave(data);
                break;
            }
            case "chEditCancel": {
                this.editingChoiceIndex = -2;
                break;
            }
            default: {
                if (button.startsWith("selDlg_")) {
                    int actualIdx;
                    int idx = DialogEditorPage.parseIntSafe(button.substring(7), -1);
                    if (idx < 0 || (actualIdx = this.dialogPage * 8 + idx) < 0 || actualIdx >= this.allDialogs.size()) break;
                    this.selectedDialogId = this.allDialogs.get((int)actualIdx).id;
                    this.selectedNodeIndex = -1;
                    this.nodePage = 0;
                    break;
                }
                if (button.startsWith("selNode_")) {
                    int idx = DialogEditorPage.parseIntSafe(button.substring(8), -1);
                    if (idx < 0) break;
                    DialogTree tree = this.getSelectedDialog();
                    int actualIdx = this.nodePage * 8 + idx;
                    if (tree == null || tree.nodes == null || actualIdx < 0 || actualIdx >= tree.nodes.size()) break;
                    this.selectedNodeIndex = actualIdx;
                    this.editingChoiceIndex = -2;
                    break;
                }
                if (button.startsWith("chEdit_")) {
                    this.editingChoiceIndex = DialogEditorPage.parseIntSafe(button.substring(7), -2);
                    break;
                }
                if (!button.startsWith("chDel_")) break;
                int ci = DialogEditorPage.parseIntSafe(button.substring(6), -1);
                DialogNode node = this.getSelectedNode();
                if (node == null || node.choices == null || ci < 0 || ci >= node.choices.size()) break;
                node.choices = DialogEditorPage.ensureMutable(node.choices);
                node.choices.remove(ci);
            }
        }
        this.refreshUI();
    }

    private void handleTextFields(PageEventData data) {
        DialogTree tree = this.getSelectedDialog();
        DialogNode node = this.getSelectedNode();
        if (tree != null) {
            if (data.speaker != null) {
                String string = tree.speakerName = data.speaker.isEmpty() ? null : data.speaker;
            }
            if (data.startNode != null) {
                String string = tree.startNode = data.startNode.isEmpty() ? null : data.startNode;
            }
        }
        if (node != null) {
            if (data.nodeId != null) {
                node.nodeId = data.nodeId;
            }
            if (data.nextNode != null) {
                String string = node.next = data.nextNode.isEmpty() ? null : data.nextNode;
            }
            if (data.line0 != null) {
                this.setLine(node, 0, data.line0);
            }
            if (data.line1 != null) {
                this.setLine(node, 1, data.line1);
            }
            if (data.line2 != null) {
                this.setLine(node, 2, data.line2);
            }
            if (data.line3 != null) {
                this.setLine(node, 3, data.line3);
            }
            if (data.macroCmds != null) {
                if (data.macroCmds.isEmpty()) {
                    node.macro = null;
                } else {
                    if (node.macro == null) {
                        node.macro = new DialogMacro();
                    }
                    node.macro.commands = new ArrayList<String>();
                    for (String cmd : data.macroCmds.split(",")) {
                        String trimmed = cmd.trim();
                        if (trimmed.isEmpty()) continue;
                        node.macro.commands.add(trimmed);
                    }
                }
            }
        }
    }

    private void setLine(DialogNode node, int index, String value) {
        node.lines = DialogEditorPage.ensureMutable(node.lines);
        while (node.lines.size() <= index) {
            node.lines.add("");
        }
        node.lines.set(index, value);
        while (!node.lines.isEmpty() && node.lines.get(node.lines.size() - 1).isEmpty()) {
            node.lines.remove(node.lines.size() - 1);
        }
    }

    private void handleSave(PageEventData data) {
        this.handleTextFields(data);
        DialogTree tree = this.getSelectedDialog();
        if (tree != null) {
            if (this.dialogService.saveDialog(tree)) {
                this.player.sendMessage(Message.raw((String)("Dialog saved: " + tree.id)).color("#44cc88"));
            } else {
                this.player.sendMessage(Message.raw((String)"Failed to save dialog!").color("#ff4444"));
            }
            this.reloadDialogs();
        }
    }

    private void handleNewDialog(String newId) {
        LOGGER.info("handleNewDialog called with newDlgId='" + newId + "'");
        if (newId == null || newId.trim().isEmpty()) {
            this.player.sendMessage(Message.raw((String)"Enter a dialog ID first.").color("#ffaa44"));
            return;
        }
        String id = newId.trim().replaceAll("[^a-zA-Z0-9_]", "_");
        DialogTree created = this.dialogService.createDialog(id);
        if (created != null) {
            this.selectedDialogId = id;
            this.selectedNodeIndex = 0;
            this.reloadDialogs();
            this.player.sendMessage(Message.raw((String)("Created dialog: " + id)).color("#44cc88"));
        } else {
            this.player.sendMessage(Message.raw((String)"Dialog ID already exists!").color("#ff4444"));
        }
    }

    private void handleChoiceEditSave(PageEventData data) {
        DialogNode node = this.getSelectedNode();
        if (node == null || node.choices == null) {
            this.editingChoiceIndex = -2;
            return;
        }
        if (this.editingChoiceIndex == -1) {
            choice = new DialogChoice();
            node.choices = DialogEditorPage.ensureMutable(node.choices);
            node.choices.add(choice);
        } else if (this.editingChoiceIndex >= 0 && this.editingChoiceIndex < node.choices.size()) {
            choice = node.choices.get(this.editingChoiceIndex);
        } else {
            this.editingChoiceIndex = -2;
            return;
        }
        if (data.chText != null) {
            choice.text = data.chText;
        }
        if (data.chNext != null) {
            choice.next = data.chNext.isEmpty() ? null : data.chNext;
        }
        this.editingChoiceIndex = -2;
    }

    private void reloadDialogs() {
        if (this.dialogService == null) {
            LOGGER.warning("DialogService not initialized!");
            this.allDialogs = new ArrayList<DialogTree>();
            return;
        }
        this.allDialogs = new ArrayList<DialogTree>(this.dialogService.getAllDialogs());
        this.allDialogs.sort((a, b) -> {
            if (a.id == null) {
                return 1;
            }
            if (b.id == null) {
                return -1;
            }
            return a.id.compareTo(b.id);
        });
    }

    private DialogTree getSelectedDialog() {
        if (this.selectedDialogId == null) {
            return null;
        }
        return this.dialogService.getDialog(this.selectedDialogId);
    }

    private DialogNode getSelectedNode() {
        DialogTree tree = this.getSelectedDialog();
        if (tree == null || tree.nodes == null || this.selectedNodeIndex < 0 || this.selectedNodeIndex >= tree.nodes.size()) {
            return null;
        }
        return tree.nodes.get(this.selectedNodeIndex);
    }

    private void refreshUI() {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        this.buildAll(ui, events);
        this.sendUpdate(ui, events, false);
    }

    private static <T> ArrayList<T> ensureMutable(List<T> list) {
        if (list == null) {
            return new ArrayList();
        }
        if (list instanceof ArrayList) {
            return (ArrayList)list;
        }
        return new ArrayList<T>(list);
    }

    private static int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        }
        catch (Exception e) {
            return fallback;
        }
    }

    private void buildAll(UICommandBuilder ui, UIEventBuilder events) {
        this.bindStaticEvents(events);
        this.buildDialogList(ui, events);
        this.buildNodeList(ui, events);
        this.buildDetailPanel(ui, events);
        this.buildChoiceOverlay(ui, events);
    }

    private void bindStaticEvents(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of((String)"Button", (String)"close"), false);
        EventData newDlgData = new EventData();
        newDlgData.append("Button", "newDlg");
        newDlgData.append("@NewDlgId", "#NewDlgField.Value");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NewDlgBtn", newDlgData, false);
        EventData saveData = new EventData();
        saveData.append("Button", "save");
        saveData.append("@Speaker", "#SpeakerField.Value");
        saveData.append("@StartNode", "#StartNodeField.Value");
        saveData.append("@NodeId", "#NodeIdField.Value");
        saveData.append("@NextNode", "#NextNodeField.Value");
        saveData.append("@Line0", "#Line0Field.Value");
        saveData.append("@Line1", "#Line1Field.Value");
        saveData.append("@Line2", "#Line2Field.Value");
        saveData.append("@Line3", "#Line3Field.Value");
        saveData.append("@MacroCmds", "#MacroCmdsField.Value");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SaveBtn", saveData, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DlgPrevBtn", EventData.of((String)"Button", (String)"dlgPrev"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DlgNextBtn", EventData.of((String)"Button", (String)"dlgNext"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NodePrevBtn", EventData.of((String)"Button", (String)"nodePrev"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NodeNextBtn", EventData.of((String)"Button", (String)"nodeNext"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddNodeBtn", EventData.of((String)"Button", (String)"addNode"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteDlgBtn", EventData.of((String)"Button", (String)"deleteDlg"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteNodeBtn", EventData.of((String)"Button", (String)"deleteNode"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AddChoiceBtn", EventData.of((String)"Button", (String)"addChoice"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NodeTypeDropdown", EventData.of((String)"@NodeType", (String)"#NodeTypeDropdown.Value"));
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#MacroServerDd", EventData.of((String)"@MacroServer", (String)"#MacroServerDd.Value"));
        events.addEventBinding(CustomUIEventBindingType.Validating, "#SpeakerField", EventData.of((String)"@Speaker", (String)"#SpeakerField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Validating, "#StartNodeField", EventData.of((String)"@StartNode", (String)"#StartNodeField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Validating, "#NodeIdField", EventData.of((String)"@NodeId", (String)"#NodeIdField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Validating, "#NextNodeField", EventData.of((String)"@NextNode", (String)"#NextNodeField.Value"), false);
        for (int i = 0; i < 4; ++i) {
            events.addEventBinding(CustomUIEventBindingType.Validating, "#Line" + i + "Field", EventData.of((String)("@Line" + i), (String)("#Line" + i + "Field.Value")), false);
        }
        events.addEventBinding(CustomUIEventBindingType.Validating, "#MacroCmdsField", EventData.of((String)"@MacroCmds", (String)"#MacroCmdsField.Value"), false);
        EventData chSaveData = new EventData();
        chSaveData.append("Button", "chEditSave");
        chSaveData.append("@ChText", "#ChEditText.Value");
        chSaveData.append("@ChNext", "#ChEditNext.Value");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ChEditSave", chSaveData, false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ChEditCancel", EventData.of((String)"Button", (String)"chEditCancel"), false);
    }

    private void buildDialogList(UICommandBuilder ui, UIEventBuilder events) {
        ui.set("#DlgCount.Text", this.allDialogs.size() + " dialogs");
        int totalPages = Math.max(1, (int)Math.ceil((double)this.allDialogs.size() / 8.0));
        ui.set("#DlgPageLabel.Text", this.dialogPage + 1 + "/" + totalPages);
        for (int i = 0; i < 8; ++i) {
            int actualIdx = this.dialogPage * 8 + i;
            boolean visible = actualIdx < this.allDialogs.size();
            ui.set("#DlgEntry" + i + ".Visible", visible);
            if (!visible) continue;
            DialogTree tree = this.allDialogs.get(actualIdx);
            boolean selected = tree.id.equals(this.selectedDialogId);
            String prefix = selected ? "> " : "";
            ui.set("#DlgName" + i + ".Text", prefix + tree.id);
            int nodeCount = tree.nodes != null ? tree.nodes.size() : 0;
            ui.set("#DlgInfo" + i + ".Text", nodeCount + " nodes");
            events.addEventBinding(CustomUIEventBindingType.Activating, "#DlgEntry" + i, EventData.of((String)"Button", (String)("selDlg_" + i)), false);
        }
    }

    private void buildNodeList(UICommandBuilder ui, UIEventBuilder events) {
        DialogTree tree = this.getSelectedDialog();
        List nodes = tree != null && tree.nodes != null ? tree.nodes : List.of();
        ui.set("#NodeCount.Text", nodes.size() + " nodes");
        ui.set("#AddNodeBtn.Visible", tree != null);
        int totalPages = Math.max(1, (int)Math.ceil((double)nodes.size() / 8.0));
        ui.set("#NodePageLabel.Text", this.nodePage + 1 + "/" + totalPages);
        for (int i = 0; i < 8; ++i) {
            boolean isStart;
            int actualIdx = this.nodePage * 8 + i;
            boolean visible = actualIdx < nodes.size();
            String entryId = "#NodeEntry" + i;
            ui.set(entryId + ".Visible", visible);
            if (!visible) continue;
            DialogNode node = (DialogNode)nodes.get(actualIdx);
            boolean selected = actualIdx == this.selectedNodeIndex;
            boolean bl = isStart = tree.startNode != null && tree.startNode.equals(node.nodeId);
            String prefix = selected ? "> " : (isStart ? "* " : "");
            ui.set("#NodeName" + i + ".Text", prefix + (node.nodeId != null ? node.nodeId : "?"));
            ui.set("#NodeType" + i + ".Text", node.type != null ? node.type : "TEXT");
            events.addEventBinding(CustomUIEventBindingType.Activating, entryId, EventData.of((String)"Button", (String)("selNode_" + i)), false);
        }
    }

    private void buildDetailPanel(UICommandBuilder ui, UIEventBuilder events) {
        DialogTree tree = this.getSelectedDialog();
        DialogNode node = this.getSelectedNode();
        ui.set("#EmptyDetailPanel.Visible", tree == null);
        boolean showDialogProps = tree != null;
        ui.set("#DialogProps.Visible", showDialogProps);
        if (showDialogProps) {
            ui.set("#DlgIdValue.Text", tree.id);
            ui.set("#SpeakerField.Value", tree.speakerName != null ? tree.speakerName : "");
            ui.set("#StartNodeField.Value", tree.startNode != null ? tree.startNode : "");
        }
        boolean showNodeProps = node != null;
        ui.set("#NodeProps.Visible", showNodeProps);
        if (showNodeProps) {
            ui.set("#NodeIdField.Value", node.nodeId != null ? node.nodeId : "");
            ArrayList<DropdownEntryInfo> typeEntries = new ArrayList<DropdownEntryInfo>();
            typeEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)"TEXT"), "TEXT"));
            typeEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)"CHOICE"), "CHOICE"));
            typeEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)"INPUT"), "INPUT"));
            ui.set("#NodeTypeDropdown.Entries", typeEntries);
            ui.set("#NodeTypeDropdown.Value", node.type != null ? node.type : "TEXT");
            for (int i = 0; i < 4; ++i) {
                String lineVal = node.lines != null && i < node.lines.size() ? node.lines.get(i) : "";
                ui.set("#Line" + i + "Field.Value", lineVal);
            }
            ui.set("#NextNodeField.Value", node.next != null ? node.next : "");
            boolean isChoice = "CHOICE".equals(node.type);
            ui.set("#ChoicesSection.Visible", isChoice);
            if (isChoice) {
                this.buildChoices(ui, events, node);
            }
            String macroCmds = "";
            boolean macroServer = false;
            if (node.macro != null) {
                if (node.macro.commands != null) {
                    macroCmds = String.join((CharSequence)", ", node.macro.commands);
                }
                macroServer = node.macro.runAsServer;
            }
            ui.set("#MacroCmdsField.Value", macroCmds);
            ArrayList<DropdownEntryInfo> serverEntries = new ArrayList<DropdownEntryInfo>();
            serverEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)"false"), "false"));
            serverEntries.add(new DropdownEntryInfo(LocalizableString.fromString((String)"true"), "true"));
            ui.set("#MacroServerDd.Entries", serverEntries);
            ui.set("#MacroServerDd.Value", String.valueOf(macroServer));
        }
        ui.set("#SaveBtn.Visible", tree != null);
        ui.set("#DeleteNodeBtn.Visible", node != null);
        ui.set("#DeleteDlgBtn.Visible", tree != null && node == null);
    }

    private void buildChoices(UICommandBuilder ui, UIEventBuilder events, DialogNode node) {
        List<Object> choices = node.choices != null ? node.choices : List.of();
        for (int i = 0; i < 6; ++i) {
            boolean visible = i < choices.size();
            String entryId = "#ChoiceEntry" + i;
            ui.set(entryId + ".Visible", visible);
            if (!visible) continue;
            DialogChoice choice = (DialogChoice)choices.get(i);
            ui.set(entryId + " #ChoiceText" + i + ".Text", choice.text != null ? choice.text : "(empty)");
            ui.set(entryId + " #ChoiceNext" + i + ".Text", (String)(choice.next != null ? "-> " + choice.next : "-> END"));
            events.addEventBinding(CustomUIEventBindingType.Activating, entryId, EventData.of((String)"Button", (String)("chEdit_" + i)), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ChoiceDel" + i, EventData.of((String)"Button", (String)("chDel_" + i)), false);
        }
    }

    private void buildChoiceOverlay(UICommandBuilder ui, UIEventBuilder events) {
        boolean show = this.editingChoiceIndex >= -1;
        ui.set("#ChoiceOverlay.Visible", show);
        if (show) {
            DialogNode node = this.getSelectedNode();
            if (node != null && this.editingChoiceIndex >= 0 && node.choices != null && this.editingChoiceIndex < node.choices.size()) {
                DialogChoice choice = node.choices.get(this.editingChoiceIndex);
                ui.set("#ChEditText.Value", choice.text != null ? choice.text : "");
                ui.set("#ChEditNext.Value", choice.next != null ? choice.next : "");
            } else {
                ui.set("#ChEditText.Value", "");
                ui.set("#ChEditNext.Value", "");
            }
        }
    }

    public static class PageEventData {
        String button;
        String newDlgId;
        String speaker;
        String startNode;
        String nodeId;
        String nodeType;
        String nextNode;
        String line0;
        String line1;
        String line2;
        String line3;
        String macroCmds;
        String macroServer;
        String chText;
        String chNext;
        public static final BuilderCodec<PageEventData> CODEC = ((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)((BuilderCodec.Builder)BuilderCodec.builder(PageEventData.class, PageEventData::new).addField(new KeyedCodec("Button", (Codec)Codec.STRING), (d, v) -> {
            d.button = v;
        }, d -> d.button)).addField(new KeyedCodec("@NewDlgId", (Codec)Codec.STRING), (d, v) -> {
            d.newDlgId = v;
        }, d -> d.newDlgId)).addField(new KeyedCodec("@Speaker", (Codec)Codec.STRING), (d, v) -> {
            d.speaker = v;
        }, d -> d.speaker)).addField(new KeyedCodec("@StartNode", (Codec)Codec.STRING), (d, v) -> {
            d.startNode = v;
        }, d -> d.startNode)).addField(new KeyedCodec("@NodeId", (Codec)Codec.STRING), (d, v) -> {
            d.nodeId = v;
        }, d -> d.nodeId)).addField(new KeyedCodec("@NodeType", (Codec)Codec.STRING), (d, v) -> {
            d.nodeType = v;
        }, d -> d.nodeType)).addField(new KeyedCodec("@NextNode", (Codec)Codec.STRING), (d, v) -> {
            d.nextNode = v;
        }, d -> d.nextNode)).addField(new KeyedCodec("@Line0", (Codec)Codec.STRING), (d, v) -> {
            d.line0 = v;
        }, d -> d.line0)).addField(new KeyedCodec("@Line1", (Codec)Codec.STRING), (d, v) -> {
            d.line1 = v;
        }, d -> d.line1)).addField(new KeyedCodec("@Line2", (Codec)Codec.STRING), (d, v) -> {
            d.line2 = v;
        }, d -> d.line2)).addField(new KeyedCodec("@Line3", (Codec)Codec.STRING), (d, v) -> {
            d.line3 = v;
        }, d -> d.line3)).addField(new KeyedCodec("@MacroCmds", (Codec)Codec.STRING), (d, v) -> {
            d.macroCmds = v;
        }, d -> d.macroCmds)).addField(new KeyedCodec("@MacroServer", (Codec)Codec.STRING), (d, v) -> {
            d.macroServer = v;
        }, d -> d.macroServer)).addField(new KeyedCodec("@ChText", (Codec)Codec.STRING), (d, v) -> {
            d.chText = v;
        }, d -> d.chText)).addField(new KeyedCodec("@ChNext", (Codec)Codec.STRING), (d, v) -> {
            d.chNext = v;
        }, d -> d.chNext)).build();
    }
}

