/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.command.system.CommandContext
 *  com.hypixel.hytale.server.core.command.system.basecommands.CommandBase
 *  com.hypixel.hytale.server.core.entity.entities.Player
 */
package com.kyuubisoft.core.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.kyuubisoft.core.CorePlugin;
import com.kyuubisoft.core.i18n.CoreI18n;
import com.kyuubisoft.core.webeditor.WebEditorService;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EditorCommand
extends CommandBase {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft WebEditor");
    private static final UUID CONSOLE_UUID = new UUID(0L, 0L);
    private final CorePlugin plugin;

    protected boolean canGeneratePermission() {
        return false;
    }

    public EditorCommand(CorePlugin plugin) {
        super("kseditor", "Opens the web config editor");
        this.requirePermission("ks.admin.editor");
        this.plugin = plugin;
    }

    protected void executeSync(CommandContext ctx) {
        String senderName;
        UUID senderUuid;
        Player player;
        if (ctx.isPlayer() && !(player = (Player)ctx.senderAs(Player.class)).hasPermission("kyuubisoft.editor")) {
            ctx.sendMessage(Message.raw((String)CoreI18n.getInstance().get("error.no_permission")).color("#FF5555"));
            return;
        }
        WebEditorService editorService = this.plugin.getWebEditorService();
        if (editorService == null || !editorService.getConfig().isEnabled()) {
            ctx.sendMessage(Message.raw((String)"Web-Editor is not enabled. Set webEditor.enabled to true in config.json.").color("#FF5555"));
            return;
        }
        ctx.sendMessage(Message.raw((String)"Preparing editor session...").color("#FFAA00"));
        if (ctx.isPlayer()) {
            Player p = (Player)ctx.senderAs(Player.class);
            senderUuid = p.getPlayerRef().getUuid();
            senderName = p.getPlayerRef().getUsername();
        } else {
            senderUuid = CONSOLE_UUID;
            senderName = "Console";
        }
        CompletableFuture.runAsync(() -> {
            try {
                String serverName = this.plugin.getCoreConfig().getTitle();
                String url = editorService.createSession(senderUuid, senderName, serverName);
                ctx.sendMessage(Message.raw((String)"").color("#55FF55"));
                ctx.sendMessage(Message.raw((String)"Config Editor ready!").color("#55FF55").bold(true));
                ctx.sendMessage(Message.raw((String)url).color("#55FFFF").link(url));
                ctx.sendMessage(Message.raw((String)"Open this URL in your browser.").color("#AAAAAA"));
                ctx.sendMessage(Message.raw((String)"").color("#55FF55"));
                LOGGER.info("Editor session created by " + senderName + ": " + url);
            }
            catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to create editor session", e);
                ctx.sendMessage(Message.raw((String)("Failed to create editor session: " + e.getMessage())).color("#FF5555"));
            }
        });
    }
}

