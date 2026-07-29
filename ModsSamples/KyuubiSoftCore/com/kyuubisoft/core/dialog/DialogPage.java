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
 *  com.hypixel.hytale.server.core.HytaleServer
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
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
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.citizen.CitizenService;
import com.kyuubisoft.core.dialog.DialogChoice;
import com.kyuubisoft.core.dialog.DialogNode;
import com.kyuubisoft.core.dialog.DialogNodeType;
import com.kyuubisoft.core.dialog.DialogService;
import com.kyuubisoft.core.dialog.DialogTree;
import com.kyuubisoft.core.i18n.CoreI18n;
import com.kyuubisoft.core.i18n.I18nContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class DialogPage
extends InteractiveCustomUIPage<PageData> {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Dialogs");
    private static final int MAX_LINES = 4;
    private static final int MAX_CHOICES = 4;
    private final DialogService dialogService;
    private final Player player;
    private final PlayerRef playerRef;
    private final DialogTree tree;
    private final String citizenId;
    private final String speakerName;
    private DialogNode currentNode;
    private List<DialogChoice> visibleChoices;
    private String pendingInputText;
    private Ref<EntityStore> lastRef;
    private Store<EntityStore> lastStore;
    private static final int TYPEWRITER_INTERVAL_MS = 50;
    private static final int CHARS_PER_TICK = 2;
    private boolean typewriterActive = false;
    private int typewriterCharIndex = 0;
    private int typewriterTotalChars = 0;
    private List<String> fullLines = new ArrayList<String>();
    private String typewriterFullText = "";
    private ScheduledFuture<?> typewriterTask;
    private int activeLineCount = 1;

    public DialogPage(DialogService dialogService, Player player, PlayerRef playerRef, DialogTree tree, String citizenId) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.dialogService = dialogService;
        this.player = player;
        this.playerRef = playerRef;
        this.tree = tree;
        this.citizenId = citizenId;
        this.speakerName = dialogService.resolveSpeakerName(tree, citizenId);
        this.currentNode = tree.getNode(tree.startNode);
    }

    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder ui, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        I18nContext.run(this.playerRef, () -> {
            this.lastRef = ref;
            this.lastStore = store;
            ui.append("Pages/Dialog/DialogPage.ui");
            ui.set("#SpeakerTitle.Text", this.speakerName);
            this.renderNode(ui);
            this.bindEvents(events);
            if (this.currentNode != null && this.currentNode.macro != null) {
                this.dialogService.executeMacro(this.player, this.playerRef, this.currentNode.macro, this.citizenId);
            }
        });
    }

    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        I18nContext.run(this.playerRef, () -> {
            super.handleDataEvent(ref, store, (Object)data);
            this.lastRef = ref;
            this.lastStore = store;
            if (data.button != null) {
                this.handleButton(data.button);
                return;
            }
            if (data.inputText != null) {
                this.pendingInputText = data.inputText;
                this.sendUpdate(new UICommandBuilder(), false);
                return;
            }
            this.sendUpdate(new UICommandBuilder(), false);
        });
    }

    private void handleButton(String action) {
        if (action == null) {
            return;
        }
        if (action.equals("continue") || action.equals("end")) {
            this.handleContinue();
            return;
        }
        if (action.equals("input_submit")) {
            this.handleInputSubmit();
            return;
        }
        if (action.startsWith("choice_")) {
            try {
                int choiceIndex = Integer.parseInt(action.substring(7));
                this.handleChoice(choiceIndex);
            }
            catch (NumberFormatException e) {
                LOGGER.fine("Invalid choice index: " + action);
                this.sendUpdate(new UICommandBuilder(), false);
            }
            return;
        }
        this.sendUpdate(new UICommandBuilder(), false);
    }

    private void handleContinue() {
        if (this.typewriterActive) {
            this.skipTypewriter();
            return;
        }
        if (this.currentNode == null || this.currentNode.next == null) {
            this.finishDialog();
            return;
        }
        this.advanceToNode(this.currentNode.next);
    }

    private void handleChoice(int choiceIndex) {
        if (this.visibleChoices == null || choiceIndex < 0 || choiceIndex >= this.visibleChoices.size()) {
            this.sendUpdate(new UICommandBuilder(), false);
            return;
        }
        DialogChoice choice = this.visibleChoices.get(choiceIndex);
        CitizenService citizenService = CitizenService.getInstance();
        if (citizenService != null) {
            String choiceText = this.dialogService.resolveText(choice.text, this.playerRef.getUsername(), this.speakerName);
            citizenService.dispatchDialogChoice(this.player, this.tree.id, choiceIndex, choiceText);
        }
        if (choice.macro != null) {
            this.dialogService.executeMacro(this.player, this.playerRef, choice.macro, this.citizenId);
        }
        if (choice.next == null) {
            this.finishDialog();
        } else {
            this.advanceToNode(choice.next);
        }
    }

    private void handleInputSubmit() {
        String input = this.pendingInputText != null ? this.pendingInputText : "";
        CitizenService citizenService = CitizenService.getInstance();
        if (citizenService != null) {
            citizenService.dispatchDialogInput(this.player, this.tree.id, input);
        }
        if (this.currentNode == null || this.currentNode.next == null) {
            this.finishDialog();
        } else {
            this.advanceToNode(this.currentNode.next);
        }
    }

    private void advanceToNode(String nodeId) {
        this.cancelTypewriter();
        DialogNode nextNode = this.tree.getNode(nodeId);
        if (nextNode == null) {
            LOGGER.warning("Dialog node not found: " + nodeId + " in dialog " + this.tree.id);
            this.finishDialog();
            return;
        }
        if (nextNode.condition != null && !this.dialogService.checkCondition(this.player, nextNode.condition)) {
            if (nextNode.next != null) {
                this.advanceToNode(nextNode.next);
            } else {
                this.finishDialog();
            }
            return;
        }
        this.currentNode = nextNode;
        this.pendingInputText = null;
        this.dialogService.advanceToNode(this.playerRef.getUuid(), nodeId);
        if (this.currentNode.macro != null) {
            this.dialogService.executeMacro(this.player, this.playerRef, this.currentNode.macro, this.citizenId);
        }
        this.refreshUI();
    }

    private void finishDialog() {
        this.cancelTypewriter();
        CitizenService citizenService = CitizenService.getInstance();
        if (citizenService != null) {
            citizenService.dispatchDialogComplete(this.player, this.tree.id);
        }
        Logger.getLogger("KyuubiSoft Citizens").info("[Dialog] finishDialog called for citizenId=" + this.citizenId);
        this.resumeCitizenMovement();
        this.dialogService.closeDialog(this.playerRef.getUuid());
        this.close();
    }

    private void refreshUI() {
        UICommandBuilder ui = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        this.renderNode(ui);
        this.bindEvents(events);
        this.sendUpdate(ui, events, false);
    }

    private String getActiveLabel() {
        return "#DL" + this.activeLineCount;
    }

    private void renderNode(UICommandBuilder ui) {
        if (this.currentNode == null) {
            return;
        }
        DialogNodeType type = this.currentNode.getType();
        String playerName = this.playerRef.getUsername();
        this.fullLines.clear();
        if (this.currentNode.lines != null) {
            for (String line : this.currentNode.lines) {
                this.fullLines.add(this.dialogService.resolveText(line, playerName, this.speakerName));
            }
        }
        this.activeLineCount = Math.max(1, Math.min(this.fullLines.size(), 4));
        for (int i = 1; i <= 4; ++i) {
            ui.set("#DL" + i + ".Visible", i == this.activeLineCount);
        }
        String joinedText = String.join((CharSequence)" ", this.fullLines);
        if (type == DialogNodeType.TEXT && !joinedText.isEmpty() && this.currentNode.typewriterEffect) {
            this.startTypewriter(ui, joinedText);
        } else {
            ui.set(this.getActiveLabel() + ".Text", joinedText);
        }
        ui.set("#ChoiceArea.Visible", type == DialogNodeType.CHOICE);
        ui.set("#InputArea.Visible", type == DialogNodeType.INPUT);
        ui.set("#ContinueArea.Visible", type == DialogNodeType.TEXT);
        switch (type) {
            case CHOICE: {
                this.renderChoices(ui);
                break;
            }
            case INPUT: {
                this.renderInput(ui);
                break;
            }
            case TEXT: {
                this.renderContinue(ui);
            }
        }
    }

    private void renderChoices(UICommandBuilder ui) {
        this.visibleChoices = this.dialogService.getVisibleChoices(this.player, this.currentNode);
        String playerName = this.playerRef.getUsername();
        for (int i = 0; i < 4; ++i) {
            boolean visible = i < this.visibleChoices.size();
            ui.set("#Choice" + i + ".Visible", visible);
            if (i < 3) {
                ui.set("#ChoiceSpacer" + i + ".Visible", visible && i < this.visibleChoices.size() - 1);
            }
            if (!visible) continue;
            String text = this.dialogService.resolveText(this.visibleChoices.get((int)i).text, playerName, this.speakerName);
            ui.set("#Choice" + i + ".Text", text);
        }
    }

    private void renderInput(UICommandBuilder ui) {
        CoreI18n i18n = CoreI18n.getInstance();
        ui.set("#InputSubmit.Text", i18n.get("dialog.btn.submit"));
    }

    private void renderContinue(UICommandBuilder ui) {
        CoreI18n i18n = CoreI18n.getInstance();
        boolean hasNext = this.currentNode.next != null;
        ui.set("#ContinueBtn.Visible", hasNext);
        ui.set("#EndBtn.Visible", !hasNext);
        ui.set("#ContinueBtn.Text", i18n.get("dialog.btn.continue"));
        ui.set("#EndBtn.Text", i18n.get("dialog.btn.close"));
    }

    private void startTypewriter(UICommandBuilder ui, String joinedText) {
        this.cancelTypewriter();
        if (joinedText.isEmpty()) {
            return;
        }
        this.typewriterFullText = joinedText;
        this.typewriterTotalChars = joinedText.length();
        this.typewriterCharIndex = 0;
        this.typewriterActive = true;
        ui.set(this.getActiveLabel() + ".Text", "");
        this.typewriterTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(this::typewriterTick, 50L, 50L, TimeUnit.MILLISECONDS);
    }

    private void typewriterTick() {
        try {
            if (!this.typewriterActive) {
                this.cancelTypewriter();
                return;
            }
            this.typewriterCharIndex += 2;
            if (this.typewriterCharIndex >= this.typewriterTotalChars) {
                this.typewriterCharIndex = this.typewriterTotalChars;
                this.typewriterActive = false;
                this.cancelTypewriter();
            }
            UICommandBuilder ui = new UICommandBuilder();
            ui.set(this.getActiveLabel() + ".Text", this.typewriterFullText.substring(0, this.typewriterCharIndex));
            this.sendUpdate(ui, false);
        }
        catch (Exception e) {
            LOGGER.fine("Typewriter tick error: " + e.getMessage());
            this.typewriterActive = false;
            this.cancelTypewriter();
        }
    }

    private void skipTypewriter() {
        this.cancelTypewriter();
        this.typewriterActive = false;
        UICommandBuilder ui = new UICommandBuilder();
        ui.set(this.getActiveLabel() + ".Text", this.typewriterFullText);
        this.sendUpdate(ui, false);
    }

    private void cancelTypewriter() {
        if (this.typewriterTask != null && !this.typewriterTask.isDone()) {
            this.typewriterTask.cancel(false);
        }
        this.typewriterTask = null;
    }

    private void bindEvents(UIEventBuilder events) {
        for (int i = 0; i < 4; ++i) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#Choice" + i, EventData.of((String)"Button", (String)("choice_" + i)), false);
        }
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ContinueBtn", EventData.of((String)"Button", (String)"continue"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#EndBtn", EventData.of((String)"Button", (String)"end"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#InputField", EventData.of((String)"@InputText", (String)"#InputField.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#InputSubmit", EventData.of((String)"Button", (String)"input_submit"), false);
    }

    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Logger csLogger = Logger.getLogger("KyuubiSoft Citizens");
        csLogger.info("[Dialog] onDismiss called for citizenId=" + this.citizenId);
        try {
            super.onDismiss(ref, store);
        }
        catch (Exception e) {
            csLogger.warning("[Dialog] super.onDismiss error (citizenId=" + this.citizenId + "): " + e.getMessage());
        }
        try {
            this.cancelTypewriter();
        }
        catch (Exception e) {
            // empty catch block
        }
        this.resumeCitizenMovement();
        try {
            this.dialogService.closeDialog(this.playerRef.getUuid());
        }
        catch (Exception e) {
            LOGGER.fine("closeDialog error: " + e.getMessage());
        }
    }

    private void resumeCitizenMovement() {
        Logger csLogger = Logger.getLogger("KyuubiSoft Citizens");
        if (this.citizenId == null) {
            csLogger.info("[Dialog\u2192Resume] citizenId is null, skipping");
            return;
        }
        try {
            CitizenService citizenService = CitizenService.getInstance();
            if (citizenService == null) {
                csLogger.warning("[Dialog\u2192Resume] CitizenService is null!");
                return;
            }
            csLogger.info("[Dialog\u2192Resume] calling resumeMovement for citizenId=" + this.citizenId);
            citizenService.resumeMovement(this.citizenId);
        }
        catch (Exception e) {
            csLogger.warning("[Dialog\u2192Resume] Failed for " + this.citizenId + ": " + e.getMessage());
        }
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = ((BuilderCodec.Builder)((BuilderCodec.Builder)BuilderCodec.builder(PageData.class, PageData::new).addField(new KeyedCodec("Button", (Codec)Codec.STRING), (data, value) -> {
            data.button = value;
        }, data -> data.button)).addField(new KeyedCodec("@InputText", (Codec)Codec.STRING), (data, value) -> {
            data.inputText = value;
        }, data -> data.inputText)).build();
        public String button;
        public String inputText;
    }
}

