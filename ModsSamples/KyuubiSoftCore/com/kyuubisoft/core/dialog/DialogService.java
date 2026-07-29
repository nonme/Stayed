/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.server.core.command.system.CommandManager
 *  com.hypixel.hytale.server.core.command.system.CommandSender
 *  com.hypixel.hytale.server.core.console.ConsoleSender
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.kyuubisoft.core.dialog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.api.CoreAPI;
import com.kyuubisoft.core.citizen.CitizenData;
import com.kyuubisoft.core.citizen.CitizenService;
import com.kyuubisoft.core.dialog.ActiveDialog;
import com.kyuubisoft.core.dialog.DialogChoice;
import com.kyuubisoft.core.dialog.DialogCondition;
import com.kyuubisoft.core.dialog.DialogConditionProvider;
import com.kyuubisoft.core.dialog.DialogMacro;
import com.kyuubisoft.core.dialog.DialogNode;
import com.kyuubisoft.core.dialog.DialogPage;
import com.kyuubisoft.core.dialog.DialogTree;
import com.kyuubisoft.core.i18n.CoreI18n;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class DialogService {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Dialogs");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DialogService instance;
    private final Map<String, DialogTree> dialogs = new ConcurrentHashMap<String, DialogTree>();
    private final Map<UUID, ActiveDialog> activeSessions = new ConcurrentHashMap<UUID, ActiveDialog>();
    private final List<DialogConditionProvider> conditionProviders = new CopyOnWriteArrayList<DialogConditionProvider>();
    private Path dataFolder;

    public DialogService() {
        instance = this;
    }

    public static DialogService getInstance() {
        return instance;
    }

    public void load(Path dataFolder) {
        this.dataFolder = dataFolder;
        Path dialogsFolder = dataFolder.resolve("dialogs");
        this.extractDefaults(dialogsFolder);
        try {
            if (Files.exists(dialogsFolder, new LinkOption[0]) && Files.isDirectory(dialogsFolder, new LinkOption[0])) {
                Files.list(dialogsFolder).filter(p -> p.toString().endsWith(".json")).forEach(this::loadDialogFile);
            }
        }
        catch (Exception e) {
            LOGGER.warning("Failed to load dialogs: " + e.getMessage());
        }
        LOGGER.info("Loaded " + this.dialogs.size() + " dialog trees");
    }

    private void loadDialogFile(Path filePath) {
        try {
            String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            DialogTree tree = GSON.fromJson(content, DialogTree.class);
            if (tree != null && tree.id != null) {
                this.dialogs.put(tree.id, tree);
            } else {
                LOGGER.warning("Invalid dialog file (no id): " + String.valueOf(filePath.getFileName()));
            }
        }
        catch (Exception e) {
            LOGGER.warning("Failed to load dialog " + String.valueOf(filePath.getFileName()) + ": " + e.getMessage());
        }
    }

    private void extractDefaults(Path dialogsFolder) {
        try {
            String[] defaultDialogs;
            if (!Files.exists(dialogsFolder, new LinkOption[0])) {
                Files.createDirectories(dialogsFolder, new FileAttribute[0]);
            }
            for (String dialogFile : defaultDialogs = new String[]{"example_dialog.json"}) {
                this.extractDialogResource("defaults/dialogs/" + dialogFile, dialogsFolder.resolve(dialogFile));
            }
        }
        catch (Exception e) {
            LOGGER.warning("Failed to extract default dialogs: " + e.getMessage());
        }
    }

    private void extractDialogResource(String resourcePath, Path targetPath) {
        if (Files.exists(targetPath, new LinkOption[0])) {
            return;
        }
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(resourcePath);){
            if (is != null) {
                Files.copy(is, targetPath, new CopyOption[0]);
                if (CoreAPI.isDebug()) {
                    LOGGER.info("Extracted default dialog: " + String.valueOf(targetPath.getFileName()));
                }
            }
        }
        catch (Exception e) {
            LOGGER.fine("Failed to extract " + resourcePath + ": " + e.getMessage());
        }
    }

    public void reload() {
        this.dialogs.clear();
        this.activeSessions.clear();
        this.load(this.dataFolder);
    }

    public DialogTree getDialog(String dialogId) {
        return this.dialogs.get(dialogId);
    }

    public Collection<DialogTree> getAllDialogs() {
        return Collections.unmodifiableCollection(this.dialogs.values());
    }

    public DialogTree createDialog(String id) {
        if (id == null || id.isEmpty() || this.dialogs.containsKey(id)) {
            return null;
        }
        DialogTree tree = new DialogTree();
        tree.id = id;
        tree.startNode = "start";
        tree.nodes = new ArrayList<DialogNode>();
        DialogNode startNode = new DialogNode();
        startNode.nodeId = "start";
        startNode.type = "TEXT";
        startNode.lines = new ArrayList<String>();
        startNode.lines.add("...");
        tree.nodes.add(startNode);
        this.dialogs.put(id, tree);
        this.saveDialog(tree);
        return tree;
    }

    public boolean saveDialog(DialogTree tree) {
        if (tree == null || tree.id == null || this.dataFolder == null) {
            return false;
        }
        Path dialogsFolder = this.dataFolder.resolve("dialogs");
        try {
            if (!Files.exists(dialogsFolder, new LinkOption[0])) {
                Files.createDirectories(dialogsFolder, new FileAttribute[0]);
            }
            String json = GSON.toJson(tree);
            Files.write(dialogsFolder.resolve(tree.id + ".json"), json.getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
            this.dialogs.put(tree.id, tree);
            return true;
        }
        catch (Exception e) {
            LOGGER.warning("Failed to save dialog " + tree.id + ": " + e.getMessage());
            return false;
        }
    }

    public boolean deleteDialog(String id) {
        if (id == null || this.dataFolder == null) {
            return false;
        }
        this.dialogs.remove(id);
        Path dialogsFolder = this.dataFolder.resolve("dialogs");
        Path file = dialogsFolder.resolve(id + ".json");
        try {
            if (Files.exists(file, new LinkOption[0])) {
                Files.delete(file);
            }
            return true;
        }
        catch (Exception e) {
            LOGGER.warning("Failed to delete dialog file " + id + ": " + e.getMessage());
            return false;
        }
    }

    public void openDialog(Player player, PlayerRef playerRef, Ref<EntityStore> ref, Store<EntityStore> store, String dialogId, String citizenId) {
        DialogTree tree = this.dialogs.get(dialogId);
        if (tree == null) {
            LOGGER.warning("Dialog not found: " + dialogId);
            return;
        }
        if (tree.startNode == null || tree.getNode(tree.startNode) == null) {
            LOGGER.warning("Dialog has no valid startNode: " + dialogId);
            return;
        }
        UUID playerId = playerRef.getUuid();
        this.activeSessions.remove(playerId);
        ActiveDialog session = new ActiveDialog(dialogId, citizenId, tree.startNode);
        this.activeSessions.put(playerId, session);
        DialogPage page = new DialogPage(this, player, playerRef, tree, citizenId);
        player.getPageManager().openCustomPage(ref, store, (CustomUIPage)page);
    }

    public void openDialogFromTree(Player player, PlayerRef playerRef, Ref<EntityStore> ref, Store<EntityStore> store, DialogTree tree, String citizenId) {
        if (tree == null || tree.nodes == null || tree.nodes.isEmpty()) {
            LOGGER.warning("Cannot open dialog from tree: tree is null or empty");
            return;
        }
        if (tree.startNode == null || tree.getNode(tree.startNode) == null) {
            LOGGER.warning("Dialog tree has no valid startNode: " + tree.id);
            return;
        }
        UUID playerId = playerRef.getUuid();
        this.activeSessions.remove(playerId);
        this.dialogs.put(tree.id, tree);
        ActiveDialog session = new ActiveDialog(tree.id, citizenId, tree.startNode);
        this.activeSessions.put(playerId, session);
        DialogPage page = new DialogPage(this, player, playerRef, tree, citizenId);
        player.getPageManager().openCustomPage(ref, store, (CustomUIPage)page);
    }

    public void closeDialog(UUID playerId) {
        CitizenService citizenService;
        ActiveDialog session = this.activeSessions.remove(playerId);
        if (session == null || (citizenService = CitizenService.getInstance()) != null) {
            // empty if block
        }
    }

    public ActiveDialog getActiveDialog(UUID playerId) {
        return this.activeSessions.get(playerId);
    }

    public boolean hasActiveDialog(UUID playerId) {
        return this.activeSessions.containsKey(playerId);
    }

    public void advanceToNode(UUID playerId, String nodeId) {
        ActiveDialog session = this.activeSessions.get(playerId);
        if (session != null) {
            session.currentNodeId = nodeId;
        }
    }

    public void addConditionProvider(DialogConditionProvider provider) {
        this.conditionProviders.add(provider);
        if (CoreAPI.isDebug()) {
            LOGGER.info("Dialog condition provider registered: " + provider.getClass().getSimpleName());
        }
    }

    public void removeConditionProvider(DialogConditionProvider provider) {
        this.conditionProviders.remove(provider);
    }

    public boolean checkCondition(Player player, DialogCondition condition) {
        if (condition == null) {
            return true;
        }
        String type = condition.type;
        String value = condition.value;
        boolean negate = condition.negate;
        if ("permission".equals(type)) {
            boolean result = player.hasPermission(value != null ? value : "");
            return negate ? !result : result;
        }
        for (DialogConditionProvider provider : this.conditionProviders) {
            try {
                if (!provider.handles(type)) continue;
                return provider.evaluate(player, type, value, negate);
            }
            catch (Exception e) {
                LOGGER.fine("Condition provider error: " + e.getMessage());
            }
        }
        LOGGER.fine("Unknown condition type: " + type);
        return negate;
    }

    public List<DialogChoice> getVisibleChoices(Player player, DialogNode node) {
        if (node.choices == null) {
            return Collections.emptyList();
        }
        ArrayList<DialogChoice> visible = new ArrayList<DialogChoice>();
        for (DialogChoice choice : node.choices) {
            if (!this.checkCondition(player, choice.condition)) continue;
            visible.add(choice);
        }
        return visible;
    }

    public void executeMacro(Player player, PlayerRef playerRef, DialogMacro macro, String citizenId) {
        if (macro == null || macro.commands == null || macro.commands.isEmpty()) {
            return;
        }
        for (String command : macro.commands) {
            try {
                String resolved = command.replace("{player}", playerRef.getUsername()).replace("{citizen}", citizenId != null ? citizenId : "");
                if (macro.runAsServer) {
                    CommandManager.get().handleCommand((CommandSender)ConsoleSender.INSTANCE, resolved);
                    continue;
                }
                CommandManager.get().handleCommand(playerRef, resolved);
            }
            catch (Exception e) {
                LOGGER.warning("Failed to execute dialog macro: " + command + " \u2014 " + e.getMessage());
            }
        }
    }

    public String resolveText(String key, String playerName, String citizenName) {
        if (key == null) {
            return "";
        }
        CoreI18n i18n = CoreI18n.getInstance();
        String text = i18n.get(key);
        if (playerName != null) {
            text = text.replace("{player}", playerName);
        }
        if (citizenName != null) {
            text = text.replace("{citizen}", citizenName);
        }
        return text;
    }

    public String resolveSpeakerName(DialogTree tree, String citizenId) {
        CitizenData citizen;
        CitizenService citizenService;
        CoreI18n i18n = CoreI18n.getInstance();
        if (tree.speakerName != null && !tree.speakerName.isEmpty()) {
            String resolved = i18n.get(tree.speakerName);
            if (!resolved.equals(tree.speakerName)) {
                return resolved;
            }
            return tree.speakerName;
        }
        if (citizenId != null && (citizenService = CitizenService.getInstance()) != null && (citizen = citizenService.getCitizen(citizenId)) != null) {
            return citizen.getDisplayName();
        }
        return "???";
    }

    public String resolveDialogForCitizen(Player player, CitizenData citizen) {
        if (citizen.dialogs == null || citizen.dialogs.isEmpty()) {
            return null;
        }
        for (CitizenData.ConditionalDialog conditionalDialog : citizen.dialogs) {
            if (conditionalDialog.dialogId == null) continue;
            if (conditionalDialog.condition == null) {
                return conditionalDialog.dialogId;
            }
            DialogCondition condition = new DialogCondition();
            condition.type = conditionalDialog.condition.type;
            condition.value = conditionalDialog.condition.value;
            condition.negate = conditionalDialog.condition.negate;
            if (!this.checkCondition(player, condition)) continue;
            return conditionalDialog.dialogId;
        }
        return null;
    }
}

