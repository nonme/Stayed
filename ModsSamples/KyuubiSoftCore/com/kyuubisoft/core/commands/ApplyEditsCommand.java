/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.command.system.CommandContext
 *  com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
 *  com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
 *  com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType
 *  com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.kyuubisoft.core.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.CorePlugin;
import com.kyuubisoft.core.i18n.CoreI18n;
import com.kyuubisoft.core.webeditor.WebEditorService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApplyEditsCommand
extends AbstractPlayerCommand {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft WebEditor");
    private final CorePlugin plugin;
    private final RequiredArg<String> codeArg;

    protected boolean canGeneratePermission() {
        return false;
    }

    public ApplyEditsCommand(CorePlugin plugin) {
        super("ksapplyconfig", "Applies config changes from the web editor");
        this.requirePermission("ks.admin.editor");
        this.plugin = plugin;
        this.codeArg = this.withRequiredArg("code", "The Bytebin paste code from the editor", (ArgumentType)ArgTypes.STRING);
    }

    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
        Player player = (Player)ctx.senderAs(Player.class);
        if (!player.hasPermission("kyuubisoft.editor")) {
            ctx.sendMessage(Message.raw((String)CoreI18n.getInstance().get("error.no_permission")).color("#FF5555"));
            return;
        }
        WebEditorService editorService = this.plugin.getWebEditorService();
        if (editorService == null || !editorService.getConfig().isEnabled()) {
            ctx.sendMessage(Message.raw((String)"Web-Editor ist nicht aktiviert.").color("#FF5555"));
            return;
        }
        String code = (String)ctx.get(this.codeArg);
        ctx.sendMessage(Message.raw((String)"Aenderungen werden angewendet...").color("#FFAA00"));
        CompletableFuture.runAsync(() -> {
            try {
                List<String> results = editorService.applyChangesManual(code);
                ctx.sendMessage(Message.raw((String)"Aenderungen angewendet:").color("#55FF55").bold(true));
                for (String result : results) {
                    boolean isError = result.contains("ERROR");
                    ctx.sendMessage(Message.raw((String)("  " + result)).color(isError ? "#FF5555" : "#55FF55"));
                }
            }
            catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to apply config changes", e);
                ctx.sendMessage(Message.raw((String)("Fehler: " + e.getMessage())).color("#FF5555"));
            }
        });
    }
}

