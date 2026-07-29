/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.command.system.CommandManager
 *  com.hypixel.hytale.server.core.command.system.CommandSender
 *  com.hypixel.hytale.server.core.console.ConsoleSender
 *  com.hypixel.hytale.server.core.entity.entities.Player
 */
package com.kyuubisoft.core.util;

import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import java.util.logging.Logger;

public class CommandUtils {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft Core");

    public static boolean executeCommand(Player player, String command, String executor) {
        if (command == null || command.isBlank()) {
            LOGGER.warning("Empty command");
            return false;
        }
        try {
            String resolved = command.replace("{player}", player.getDisplayName());
            CommandManager cmdManager = CommandManager.get();
            if (cmdManager == null) {
                LOGGER.warning("CommandManager not available for: " + resolved);
                return false;
            }
            Object sender = "player".equalsIgnoreCase(executor) ? player : ConsoleSender.INSTANCE;
            cmdManager.handleCommand((CommandSender)sender, resolved);
            return true;
        }
        catch (Exception e) {
            LOGGER.warning("Failed to execute command '" + command + "': " + e.getMessage());
            return false;
        }
    }

    public static boolean executeAsConsole(Player player, String command) {
        return CommandUtils.executeCommand(player, command, "console");
    }
}

