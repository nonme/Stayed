/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.server.core.command.system.CommandContext
 *  com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 *  com.hypixel.hytale.server.core.universe.world.World
 *  com.hypixel.hytale.server.core.universe.world.storage.EntityStore
 */
package com.yourname.companion.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.yourname.companion.commands.CompanionCommand;

public final class FhcCommand
extends AbstractPlayerCommand {
    private final CompanionCommand companionCommand;

    public FhcCommand(CompanionCommand companionCommand) {
        super("fhc", "Open Forest Companion UI");
        this.companionCommand = companionCommand;
        this.setAllowsExtraArguments(true);
    }

    protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> playerRefEntity, PlayerRef playerRef, World world) {
        this.companionCommand.openUIMenuForPlayer(playerRef);
    }
}

