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

public class TrustEditorCommand
extends AbstractPlayerCommand {
    private final CorePlugin plugin;
    private final RequiredArg<String> nonceArg;

    protected boolean canGeneratePermission() {
        return false;
    }

    public TrustEditorCommand(CorePlugin plugin) {
        super("kstrusteditor", "Trusts a web editor connection");
        this.requirePermission("ks.admin.editor");
        this.plugin = plugin;
        this.nonceArg = this.withRequiredArg("nonce", "The trust nonce shown in chat", (ArgumentType)ArgTypes.STRING);
    }

    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
        Player player = (Player)ctx.senderAs(Player.class);
        if (!player.hasPermission("kyuubisoft.editor")) {
            ctx.sendMessage(Message.raw((String)CoreI18n.getInstance().get("error.no_permission")).color("#FF5555"));
            return;
        }
        WebEditorService editorService = this.plugin.getWebEditorService();
        if (editorService == null) {
            ctx.sendMessage(Message.raw((String)"Web-Editor ist nicht aktiviert.").color("#FF5555"));
            return;
        }
        String nonce = (String)ctx.get(this.nonceArg);
        boolean success = editorService.confirmTrust(playerRef.getUuid(), nonce);
        if (success) {
            ctx.sendMessage(Message.raw((String)"Editor-Verbindung bestaetigt! Aenderungen werden jetzt live angewendet.").color("#55FF55"));
        } else {
            ctx.sendMessage(Message.raw((String)"Ungueltiger Nonce oder keine aktive Editor-Session.").color("#FF5555"));
        }
    }
}

