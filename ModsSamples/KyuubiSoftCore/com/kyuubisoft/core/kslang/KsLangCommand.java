/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.command.system.CommandContext
 *  com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.kyuubisoft.core.kslang;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kyuubisoft.core.kslang.KsLang;
import com.kyuubisoft.core.kslang.KsLangPage;
import java.util.Map;

public class KsLangCommand
extends AbstractPlayerCommand {
    protected boolean canGeneratePermission() {
        return false;
    }

    public KsLangCommand() {
        super("kslang", "Select your preferred language");
        this.setAllowsExtraArguments(true);
    }

    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
        String arg;
        Player player = (Player)store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        String input = ctx.getInputString();
        String[] parts = input != null ? input.trim().split("\\s+") : new String[]{};
        String string = arg = parts.length > 1 ? parts[1].trim() : null;
        if (arg == null || arg.isEmpty()) {
            KsLangPage page = new KsLangPage(playerRef);
            player.getPageManager().openCustomPage(ref, store, (CustomUIPage)page);
            return;
        }
        if ("auto".equalsIgnoreCase(arg) || "reset".equalsIgnoreCase(arg)) {
            KsLang.clearPlayerLanguage(playerRef.getUuid());
            String detected = playerRef.getLanguage();
            if (detected == null || detected.isEmpty()) {
                detected = "en-US";
            }
            playerRef.sendMessage(Message.raw((String)("\u00a7a[KsLang] \u00a77Language override removed. Using auto-detected: \u00a7f" + detected)));
            return;
        }
        Map<String, String> supported = KsLang.getSupportedLanguages();
        for (Map.Entry<String, String> entry : supported.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase(arg) && !entry.getKey().replace("-", "").equalsIgnoreCase(arg)) continue;
            KsLang.setPlayerLanguage(playerRef.getUuid(), entry.getKey());
            playerRef.sendMessage(Message.raw((String)("\u00a7a[KsLang] \u00a77Language set to: \u00a7f" + entry.getValue() + " (" + entry.getKey() + ")")));
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\u00a7e[KsLang] \u00a77Unknown language code: \u00a7c").append(arg);
        sb.append("\n\u00a77Available: ");
        boolean first = true;
        for (Map.Entry<String, String> entry : supported.entrySet()) {
            if (!first) {
                sb.append("\u00a77, ");
            }
            first = false;
            sb.append("\u00a7f").append(entry.getKey());
        }
        sb.append("\n\u00a77Use \u00a7f/kslang auto \u00a77to reset to auto-detect.");
        playerRef.sendMessage(Message.raw((String)sb.toString()));
    }
}

