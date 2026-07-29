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
package com.kyuubisoft.core.commands;

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
import com.kyuubisoft.core.api.CoreAPI;
import com.kyuubisoft.core.i18n.CoreI18n;
import com.kyuubisoft.core.i18n.I18nContext;
import com.kyuubisoft.core.i18n.LanguageSettingsHelper;
import com.kyuubisoft.core.ui.LanguageSettingsPage;
import java.util.Set;

public class LanguageCommand
extends AbstractPlayerCommand {
    private static final Set<String> VALID_LANGUAGES = Set.of("en-US", "de-DE", "fr-FR", "es-ES", "pt-BR", "ru-RU", "pl-PL", "tr-TR", "it-IT");

    protected boolean canGeneratePermission() {
        return false;
    }

    public LanguageCommand() {
        super("kslang", "Set your preferred language");
        this.setAllowsExtraArguments(true);
    }

    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
        I18nContext.run(playerRef, () -> {
            CoreI18n i18n = CoreI18n.getInstance();
            String input = ctx.getInputString().trim();
            String[] parts = input.split("\\s+");
            if (parts.length <= 1) {
                Player player = (Player)store.getComponent(ref, Player.getComponentType());
                if (player != null) {
                    LanguageSettingsPage page = new LanguageSettingsPage(player, playerRef);
                    player.getPageManager().openCustomPage(ref, store, (CustomUIPage)page);
                }
                return;
            }
            String arg = parts[1].trim();
            if ("auto".equalsIgnoreCase(arg)) {
                LanguageSettingsHelper.applyLanguageSelection(playerRef.getUuid(), playerRef.getUsername(), "auto");
                String resolved = CoreAPI.getPlayerLanguage(playerRef);
                String displayName = LanguageSettingsHelper.getLanguageDisplayName(resolved);
                ctx.sendMessage(Message.raw((String)i18n.get("lang.set.auto", displayName)).color("#55FF55"));
                return;
            }
            if (VALID_LANGUAGES.contains(arg)) {
                LanguageSettingsHelper.applyLanguageSelection(playerRef.getUuid(), playerRef.getUsername(), arg);
                String displayName = LanguageSettingsHelper.getLanguageDisplayName(arg);
                ctx.sendMessage(Message.raw((String)i18n.get("lang.set", displayName)).color("#55FF55"));
                return;
            }
            ctx.sendMessage(Message.raw((String)i18n.get("lang.invalid", arg)).color("#FF5555"));
            ctx.sendMessage(Message.raw((String)i18n.get("lang.available")).color("#AAAAAA"));
            StringBuilder langs = new StringBuilder();
            for (String lang : VALID_LANGUAGES.stream().sorted().toList()) {
                if (!langs.isEmpty()) {
                    langs.append(", ");
                }
                langs.append(lang);
            }
            ctx.sendMessage(Message.raw((String)("  " + String.valueOf(langs))).color("#AAAAAA"));
        });
    }
}

