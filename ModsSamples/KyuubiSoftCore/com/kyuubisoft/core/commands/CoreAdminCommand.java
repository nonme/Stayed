/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Holder
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.logger.HytaleLogger$Api
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.command.system.AbstractCommand
 *  com.hypixel.hytale.server.core.command.system.CommandContext
 *  com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
 *  com.hypixel.hytale.server.core.command.system.basecommands.CommandBase
 *  com.hypixel.hytale.server.core.entity.Entity
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage
 *  com.hypixel.hytale.server.core.entity.nameplate.Nameplate
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.Universe
 *  com.hypixel.hytale.server.core.universe.playerdata.PlayerStorage
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.kyuubisoft.core.commands;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.playerdata.PlayerStorage;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.CorePlugin;
import com.kyuubisoft.core.api.CoreAPI;
import com.kyuubisoft.core.i18n.CoreI18n;
import com.kyuubisoft.core.registry.ModMenuRegistry;
import com.kyuubisoft.core.reward.RewardGrantHelper;
import com.kyuubisoft.core.storage.DatabaseManager;
import com.kyuubisoft.core.storage.MySQLPlayerDataStorage;
import com.kyuubisoft.core.ui.CoreAdminPage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CoreAdminCommand
extends AbstractPlayerCommand {
    private final CorePlugin plugin;

    protected boolean canGeneratePermission() {
        return false;
    }

    public CoreAdminCommand(CorePlugin plugin) {
        super("ksadmin", "Opens the KyuubiSoft admin panel");
        this.requirePermission("ks.admin");
        this.plugin = plugin;
        CleanupCommand cleanupCmd = new CleanupCommand(this);
        cleanupCmd.addSubCommand((AbstractCommand)new CleanupNametagsCommand(this));
        this.addSubCommand((AbstractCommand)cleanupCmd);
        this.addSubCommand((AbstractCommand)new ReloadCommand(this));
        MigrateStorageCommand migrateCmd = new MigrateStorageCommand(this);
        migrateCmd.addSubCommand((AbstractCommand)new MigrateFileToMysqlCommand(this));
        migrateCmd.addSubCommand((AbstractCommand)new MigrateMysqlToFileCommand(this));
        this.addSubCommand((AbstractCommand)migrateCmd);
        this.addSubCommand((AbstractCommand)new AddXpCommand(this));
        this.addSubCommand((AbstractCommand)new RemoveXpCommand(this));
    }

    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
        Player player = (Player)ctx.senderAs(Player.class);
        if (!player.hasPermission("kyuubisoft.admin")) {
            ctx.sendMessage(Message.raw((String)CoreI18n.getInstance().get("error.no_permission")).color("#FF5555"));
            return;
        }
        try {
            CoreAdminPage page = new CoreAdminPage(this.plugin, player, playerRef);
            player.getPageManager().openCustomPage(ref, store, (CustomUIPage)page);
        }
        catch (Exception e) {
            ((HytaleLogger.Api)LOGGER.at(Level.SEVERE).withCause((Throwable)e)).log("Failed to open admin panel");
            ctx.sendMessage(Message.raw((String)CoreI18n.getInstance().get("error.admin_panel_open", e.getMessage())).color("#FF5555"));
        }
    }

    private void cleanupAllNametags(CommandContext ctx, Player sender) {
        Collection<Player> onlinePlayers = this.plugin.getOnlinePlayers();
        Set onlineUuids = onlinePlayers.stream().map(Entity::getUuid).collect(Collectors.toSet());
        int onlineCount = 0;
        for (Player p : onlinePlayers) {
            this.plugin.resetPlayerNameplate(p);
            ++onlineCount;
        }
        ctx.sendMessage(Message.raw((String)("Resetting " + onlineCount + " online player(s)...")).color("#55FF55"));
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                ctx.sendMessage(Message.raw((String)"Error: Universe not available.").color("#FF5555"));
                return;
            }
            PlayerStorage storage = universe.getPlayerStorage();
            if (storage == null) {
                ctx.sendMessage(Message.raw((String)"Error: PlayerStorage not available.").color("#FF5555"));
                return;
            }
            Set allUuids = storage.getPlayers();
            Set offlineUuids = allUuids.stream().filter(uuid -> !onlineUuids.contains(uuid)).collect(Collectors.toSet());
            if (offlineUuids.isEmpty()) {
                ctx.sendMessage(Message.raw((String)"No offline players to process.").color("#AAAAAA"));
                return;
            }
            ctx.sendMessage(Message.raw((String)("Processing " + offlineUuids.size() + " offline player(s)...")).color("#FFAA00"));
            AtomicInteger cleanedCount = new AtomicInteger(0);
            AtomicInteger processedCount = new AtomicInteger(0);
            int totalOffline = offlineUuids.size();
            for (UUID uuid2 : offlineUuids) {
                ((CompletableFuture)storage.load(uuid2).thenAccept(holder -> {
                    try {
                        boolean changed = this.cleanNameplateInHolder((Holder<EntityStore>)holder);
                        if (changed) {
                            storage.save(uuid2, holder).thenRun(() -> {
                                cleanedCount.incrementAndGet();
                                this.checkCompletion(sender, processedCount.incrementAndGet(), totalOffline, cleanedCount.get());
                            });
                        } else {
                            this.checkCompletion(sender, processedCount.incrementAndGet(), totalOffline, cleanedCount.get());
                        }
                    }
                    catch (Exception e) {
                        this.checkCompletion(sender, processedCount.incrementAndGet(), totalOffline, cleanedCount.get());
                        ((HytaleLogger.Api)LOGGER.atWarning()).log("Failed to clean nameplate for %s: %s", (Object)uuid2, (Object)e.getMessage());
                    }
                })).exceptionally(ex -> {
                    this.checkCompletion(sender, processedCount.incrementAndGet(), totalOffline, cleanedCount.get());
                    ((HytaleLogger.Api)LOGGER.atWarning()).log("Failed to load player data for %s: %s", (Object)uuid2, (Object)ex.getMessage());
                    return null;
                });
            }
        }
        catch (Exception e) {
            ctx.sendMessage(Message.raw((String)("Error: " + e.getMessage())).color("#FF5555"));
            ((HytaleLogger.Api)LOGGER.at(Level.SEVERE).withCause((Throwable)e)).log("Cleanup nametags error");
        }
    }

    private void checkCompletion(Player sender, int processed, int total, int cleaned) {
        if (processed >= total) {
            sender.sendMessage(Message.raw((String)("Cleanup complete: " + cleaned + "/" + total + " offline nameplate(s) cleaned.")).color("#55FF55"));
        }
    }

    private boolean cleanNameplateInHolder(Holder<EntityStore> holder) {
        Nameplate nameplate = (Nameplate)holder.getComponent(Nameplate.getComponentType());
        if (nameplate == null) {
            return false;
        }
        String text = nameplate.getText();
        if (text == null || text.isEmpty()) {
            return false;
        }
        if (!text.contains("[")) {
            return false;
        }
        String cleaned = text.replaceAll("\\[.*?\\]", "").trim();
        if ((cleaned = cleaned.replaceAll("\\s+", " ").trim()).contains("\n")) {
            String[] lines = cleaned.split("\n");
            for (int i = lines.length - 1; i >= 0; --i) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                cleaned = line;
                break;
            }
        }
        if (cleaned.isEmpty() || cleaned.equals(text)) {
            return false;
        }
        nameplate.setText(cleaned);
        if (CoreAPI.isDebug()) {
            ((HytaleLogger.Api)LOGGER.atInfo()).log("Cleaned nameplate: '%s' -> '%s'", (Object)text, (Object)cleaned);
        }
        return true;
    }

    private Player findPlayerByName(String name) {
        for (Player p : this.plugin.getOnlinePlayers()) {
            if (!p.getPlayerRef().getUsername().equalsIgnoreCase(name)) continue;
            return p;
        }
        return null;
    }

    private class CleanupCommand
    extends CommandBase {
        protected boolean canGeneratePermission() {
            return false;
        }

        CleanupCommand(CoreAdminCommand coreAdminCommand) {
            Objects.requireNonNull(coreAdminCommand);
            super("cleanup", "Cleanup stale data");
            this.requirePermission("ks.admin.cleanup");
        }

        protected void executeSync(CommandContext ctx) {
            ctx.sendMessage(Message.raw((String)"Usage: /ksadmin cleanup nametags").color("#FFAA00"));
        }
    }

    private class CleanupNametagsCommand
    extends CommandBase {
        final /* synthetic */ CoreAdminCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        CleanupNametagsCommand(CoreAdminCommand coreAdminCommand) {
            CoreAdminCommand coreAdminCommand2 = coreAdminCommand;
            Objects.requireNonNull(coreAdminCommand2);
            this.this$0 = coreAdminCommand2;
            super("nametags", "Reset all player nameplates (online + offline)");
            this.requirePermission("ks.admin.cleanup");
        }

        protected void executeSync(CommandContext ctx) {
            if (!ctx.isPlayer()) {
                return;
            }
            Player player = (Player)ctx.senderAs(Player.class);
            if (!player.hasPermission("kyuubisoft.admin")) {
                ctx.sendMessage(Message.raw((String)CoreI18n.getInstance().get("error.no_permission")).color("#FF5555"));
                return;
            }
            if (CoreAPI.isShowcaseBlocked(player.getPlayerRef())) {
                ctx.sendMessage(Message.raw((String)CoreI18n.getInstance().get("error.no_permission")).color("#FF5555"));
                return;
            }
            this.this$0.cleanupAllNametags(ctx, player);
        }
    }

    private class ReloadCommand
    extends CommandBase {
        protected boolean canGeneratePermission() {
            return false;
        }

        ReloadCommand(CoreAdminCommand coreAdminCommand) {
            Objects.requireNonNull(coreAdminCommand);
            super("reload", "Reload all mod configs without server restart");
            this.requirePermission("ks.admin.reload");
        }

        protected void executeSync(CommandContext ctx) {
            if (ctx.isPlayer()) {
                Player player = (Player)ctx.senderAs(Player.class);
                if (!player.hasPermission("kyuubisoft.admin")) {
                    ctx.sendMessage(Message.raw((String)CoreI18n.getInstance().get("error.no_permission")).color("#FF5555"));
                    return;
                }
                if (CoreAPI.isShowcaseBlocked(player.getPlayerRef())) {
                    ctx.sendMessage(Message.raw((String)CoreI18n.getInstance().get("error.no_permission")).color("#FF5555"));
                    return;
                }
            }
            ctx.sendMessage(Message.raw((String)CoreI18n.getInstance().get("admin.reload.start")).color("#FFAA00"));
            List<String> results = ModMenuRegistry.reloadAll();
            for (String result : results) {
                boolean isError = result.contains("ERROR");
                ctx.sendMessage(Message.raw((String)("  " + result)).color(isError ? "#FF5555" : "#55FF55"));
            }
            if (results.isEmpty()) {
                ctx.sendMessage(Message.raw((String)CoreI18n.getInstance().get("admin.reload.none")).color("#AAAAAA"));
            } else {
                ctx.sendMessage(Message.raw((String)CoreI18n.getInstance().get("admin.reload.done", results.size())).color("#55FF55"));
            }
        }
    }

    private class MigrateStorageCommand
    extends CommandBase {
        protected boolean canGeneratePermission() {
            return false;
        }

        MigrateStorageCommand(CoreAdminCommand coreAdminCommand) {
            Objects.requireNonNull(coreAdminCommand);
            super("migrate-storage", "Migrate player data between file and MySQL");
            this.requirePermission("ks.admin.migrate");
        }

        protected void executeSync(CommandContext ctx) {
            ctx.sendMessage(Message.raw((String)"Usage: /ksadmin migrate-storage file-to-mysql|mysql-to-file").color("#FFAA00"));
        }
    }

    private class MigrateFileToMysqlCommand
    extends CommandBase {
        final /* synthetic */ CoreAdminCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        MigrateFileToMysqlCommand(CoreAdminCommand coreAdminCommand) {
            CoreAdminCommand coreAdminCommand2 = coreAdminCommand;
            Objects.requireNonNull(coreAdminCommand2);
            this.this$0 = coreAdminCommand2;
            super("file-to-mysql", "Import all local JSON player data into MySQL");
            this.requirePermission("ks.admin.migrate");
        }

        protected void executeSync(CommandContext ctx) {
            if (!ctx.isPlayer()) {
                return;
            }
            Player player = (Player)ctx.senderAs(Player.class);
            if (!player.hasPermission("kyuubisoft.admin")) {
                ctx.sendMessage(Message.raw((String)CoreI18n.getInstance().get("error.no_permission")).color("#FF5555"));
                return;
            }
            if (CoreAPI.isShowcaseBlocked(player.getPlayerRef())) {
                ctx.sendMessage(Message.raw((String)CoreI18n.getInstance().get("error.no_permission")).color("#FF5555"));
                return;
            }
            DatabaseManager dbManager = this.this$0.plugin.getDatabaseManager();
            if (dbManager == null || !dbManager.isConnected()) {
                ctx.sendMessage(Message.raw((String)"Error: MySQL is not configured or not connected. Set storage.type = \"mysql\" in config.json first.").color("#FF5555"));
                return;
            }
            MySQLPlayerDataStorage mysqlStorage = (MySQLPlayerDataStorage)this.this$0.plugin.getMySQLStorage();
            if (mysqlStorage == null) {
                ctx.sendMessage(Message.raw((String)"Error: MySQL storage not initialized.").color("#FF5555"));
                return;
            }
            ctx.sendMessage(Message.raw((String)"Starting file-to-mysql migration...").color("#FFAA00"));
            CompletableFuture.runAsync(() -> {
                String[][] modTables;
                int totalSuccess = 0;
                int totalFailed = 0;
                for (String[] modInfo : modTables = new String[][]{{"quests", "quest_players", "players", "kyuubisoft_questbook"}, {"achievements", "achievement_players", "players", "kyuubisoft_achievements"}, {"shop", "shop_players", "shop_data", "kyuubisoft_core"}}) {
                    String modName = modInfo[0];
                    String tableName = modInfo[1];
                    String subFolder = modInfo[2];
                    String pluginFolder = modInfo[3];
                    Path modDataFolder = this.this$0.plugin.getDataDirectory().getParent().resolve(pluginFolder).resolve("data").resolve(subFolder);
                    if (!Files.exists(modDataFolder, new LinkOption[0])) {
                        player.sendMessage(Message.raw((String)("  [" + modName + "] No data folder found, skipping")).color("#AAAAAA"));
                        continue;
                    }
                    mysqlStorage.ensureTableExists(tableName);
                    int success = 0;
                    int failed = 0;
                    try (Stream<Path> stream = Files.list(modDataFolder);){
                        List files = stream.filter(p -> p.toString().endsWith(".json")).filter(p -> !p.toString().endsWith(".bak")).collect(Collectors.toList());
                        for (Path file : files) {
                            try {
                                String fileName = file.getFileName().toString();
                                String uuidStr = fileName.replace(".json", "");
                                UUID uuid = UUID.fromString(uuidStr);
                                String json = Files.readString(file, StandardCharsets.UTF_8);
                                if (json == null || json.isBlank()) continue;
                                mysqlStorage.saveJson(tableName, uuid, uuidStr, json);
                                ++success;
                            }
                            catch (Exception e) {
                                ++failed;
                                ((HytaleLogger.Api)LOGGER.atInfo()).log("Migration failed for file %s: %s", (Object)file, (Object)e.getMessage());
                            }
                        }
                    }
                    catch (IOException e) {
                        player.sendMessage(Message.raw((String)("  [" + modName + "] Error reading directory: " + e.getMessage())).color("#FF5555"));
                        continue;
                    }
                    totalSuccess += success;
                    totalFailed += failed;
                    player.sendMessage(Message.raw((String)("  [" + modName + "] " + success + " migrated, " + failed + " failed")).color(failed > 0 ? "#FFAA00" : "#55FF55"));
                }
                player.sendMessage(Message.raw((String)("Migration complete: " + totalSuccess + " total records migrated" + (String)(totalFailed > 0 ? ", " + totalFailed + " failed" : ""))).color(totalFailed > 0 ? "#FFAA00" : "#55FF55"));
            });
        }
    }

    private class MigrateMysqlToFileCommand
    extends CommandBase {
        final /* synthetic */ CoreAdminCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        MigrateMysqlToFileCommand(CoreAdminCommand coreAdminCommand) {
            CoreAdminCommand coreAdminCommand2 = coreAdminCommand;
            Objects.requireNonNull(coreAdminCommand2);
            this.this$0 = coreAdminCommand2;
            super("mysql-to-file", "Export all MySQL player data to local JSON files");
            this.requirePermission("ks.admin.migrate");
        }

        protected void executeSync(CommandContext ctx) {
            if (!ctx.isPlayer()) {
                return;
            }
            Player player = (Player)ctx.senderAs(Player.class);
            if (!player.hasPermission("kyuubisoft.admin")) {
                ctx.sendMessage(Message.raw((String)CoreI18n.getInstance().get("error.no_permission")).color("#FF5555"));
                return;
            }
            if (CoreAPI.isShowcaseBlocked(player.getPlayerRef())) {
                ctx.sendMessage(Message.raw((String)CoreI18n.getInstance().get("error.no_permission")).color("#FF5555"));
                return;
            }
            DatabaseManager dbManager = this.this$0.plugin.getDatabaseManager();
            if (dbManager == null || !dbManager.isConnected()) {
                ctx.sendMessage(Message.raw((String)"Error: MySQL is not configured or not connected.").color("#FF5555"));
                return;
            }
            ctx.sendMessage(Message.raw((String)"Starting mysql-to-file export...").color("#FFAA00"));
            CompletableFuture.runAsync(() -> {
                String[][] modTables;
                int totalSuccess = 0;
                int totalFailed = 0;
                String prefix = this.this$0.plugin.getCoreConfig().getStorageConfig().mysql.tablePrefix;
                for (String[] modInfo : modTables = new String[][]{{"quests", "quest_players", "players", "kyuubisoft_questbook"}, {"achievements", "achievement_players", "players", "kyuubisoft_achievements"}, {"shop", "shop_players", "shop_data", "kyuubisoft_core"}}) {
                    String modName = modInfo[0];
                    String tableName = modInfo[1];
                    String subFolder = modInfo[2];
                    String pluginFolder = modInfo[3];
                    Path modDataFolder = this.this$0.plugin.getDataDirectory().getParent().resolve(pluginFolder).resolve("data").resolve(subFolder);
                    try {
                        Files.createDirectories(modDataFolder, new FileAttribute[0]);
                    }
                    catch (IOException e) {
                        player.sendMessage(Message.raw((String)("  [" + modName + "] Cannot create directory: " + e.getMessage())).color("#FF5555"));
                        continue;
                    }
                    int success = 0;
                    int failed = 0;
                    String fullTableName = prefix + tableName;
                    try (Connection conn = dbManager.getConnection();
                         PreparedStatement stmt = conn.prepareStatement("SELECT uuid, data FROM " + fullTableName);
                         ResultSet rs = stmt.executeQuery();){
                        while (rs.next()) {
                            try {
                                String uuidStr = rs.getString("uuid");
                                String json = rs.getString("data");
                                Path file = modDataFolder.resolve(uuidStr + ".json");
                                Files.writeString(file, (CharSequence)json, StandardCharsets.UTF_8, new OpenOption[0]);
                                ++success;
                            }
                            catch (Exception e) {
                                ++failed;
                                ((HytaleLogger.Api)LOGGER.atInfo()).log("Export failed: %s", (Object)e.getMessage());
                            }
                        }
                    }
                    catch (Exception e) {
                        player.sendMessage(Message.raw((String)("  [" + modName + "] Table not found or error: " + e.getMessage())).color("#FF5555"));
                        continue;
                    }
                    totalSuccess += success;
                    totalFailed += failed;
                    player.sendMessage(Message.raw((String)("  [" + modName + "] " + success + " exported, " + failed + " failed")).color(failed > 0 ? "#FFAA00" : "#55FF55"));
                }
                player.sendMessage(Message.raw((String)("Export complete: " + totalSuccess + " total records exported" + (String)(totalFailed > 0 ? ", " + totalFailed + " failed" : ""))).color(totalFailed > 0 ? "#FFAA00" : "#55FF55"));
            });
        }
    }

    private class AddXpCommand
    extends CommandBase {
        final /* synthetic */ CoreAdminCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        AddXpCommand(CoreAdminCommand coreAdminCommand) {
            CoreAdminCommand coreAdminCommand2 = coreAdminCommand;
            Objects.requireNonNull(coreAdminCommand2);
            this.this$0 = coreAdminCommand2;
            super("addxp", "Grant LevelingCore XP to a player");
            this.requirePermission("ks.admin.xp");
            this.setAllowsExtraArguments(true);
        }

        protected void executeSync(CommandContext ctx) {
            long amount;
            String input;
            String[] parts;
            if (ctx.isPlayer()) {
                Player player = (Player)ctx.senderAs(Player.class);
                if (!player.hasPermission("kyuubisoft.admin")) {
                    ctx.sendMessage(Message.raw((String)"No permission.").color("#FF5555"));
                    return;
                }
                if (CoreAPI.isShowcaseBlocked(player.getPlayerRef())) {
                    ctx.sendMessage(Message.raw((String)"No permission.").color("#FF5555"));
                    return;
                }
            }
            if ((parts = (input = ctx.getInputString().trim()).split("\\s+")).length < 3) {
                ctx.sendMessage(Message.raw((String)"Usage: /ksadmin addxp <player> <amount>").color("#FFAA00"));
                return;
            }
            String playerName = parts[1];
            try {
                amount = Long.parseLong(parts[2]);
            }
            catch (NumberFormatException e) {
                ctx.sendMessage(Message.raw((String)("Invalid amount: " + parts[2])).color("#FF5555"));
                return;
            }
            if (amount <= 0L) {
                ctx.sendMessage(Message.raw((String)"Amount must be positive.").color("#FF5555"));
                return;
            }
            Player target = this.this$0.findPlayerByName(playerName);
            if (target == null) {
                ctx.sendMessage(Message.raw((String)("Player not found: " + playerName)).color("#FF5555"));
                return;
            }
            boolean success = RewardGrantHelper.grantLevelingXp(target, amount);
            if (success) {
                ctx.sendMessage(Message.raw((String)("Granted " + amount + " XP to " + target.getDisplayName())).color("#55FF55"));
            } else {
                ctx.sendMessage(Message.raw((String)"Failed \u2014 LevelingCore not available.").color("#FF5555"));
            }
        }
    }

    private class RemoveXpCommand
    extends CommandBase {
        final /* synthetic */ CoreAdminCommand this$0;

        protected boolean canGeneratePermission() {
            return false;
        }

        RemoveXpCommand(CoreAdminCommand coreAdminCommand) {
            CoreAdminCommand coreAdminCommand2 = coreAdminCommand;
            Objects.requireNonNull(coreAdminCommand2);
            this.this$0 = coreAdminCommand2;
            super("removexp", "Remove LevelingCore XP from a player");
            this.requirePermission("ks.admin.xp");
            this.setAllowsExtraArguments(true);
        }

        protected void executeSync(CommandContext ctx) {
            long amount;
            String input;
            String[] parts;
            if (ctx.isPlayer()) {
                Player player = (Player)ctx.senderAs(Player.class);
                if (!player.hasPermission("kyuubisoft.admin")) {
                    ctx.sendMessage(Message.raw((String)"No permission.").color("#FF5555"));
                    return;
                }
                if (CoreAPI.isShowcaseBlocked(player.getPlayerRef())) {
                    ctx.sendMessage(Message.raw((String)"No permission.").color("#FF5555"));
                    return;
                }
            }
            if ((parts = (input = ctx.getInputString().trim()).split("\\s+")).length < 3) {
                ctx.sendMessage(Message.raw((String)"Usage: /ksadmin removexp <player> <amount>").color("#FFAA00"));
                return;
            }
            String playerName = parts[1];
            try {
                amount = Long.parseLong(parts[2]);
            }
            catch (NumberFormatException e) {
                ctx.sendMessage(Message.raw((String)("Invalid amount: " + parts[2])).color("#FF5555"));
                return;
            }
            if (amount <= 0L) {
                ctx.sendMessage(Message.raw((String)"Amount must be positive.").color("#FF5555"));
                return;
            }
            Player target = this.this$0.findPlayerByName(playerName);
            if (target == null) {
                ctx.sendMessage(Message.raw((String)("Player not found: " + playerName)).color("#FF5555"));
                return;
            }
            boolean success = RewardGrantHelper.removeLevelingXp(target, amount);
            if (success) {
                ctx.sendMessage(Message.raw((String)("Removed " + amount + " XP from " + target.getDisplayName())).color("#55FF55"));
            } else {
                ctx.sendMessage(Message.raw((String)"Failed \u2014 LevelingCore not available.").color("#FF5555"));
            }
        }
    }
}

