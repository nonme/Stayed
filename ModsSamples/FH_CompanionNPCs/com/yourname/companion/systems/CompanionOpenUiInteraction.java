/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.codec.builder.BuilderCodec
 *  com.hypixel.hytale.component.CommandBuffer
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.protocol.InteractionType
 *  com.hypixel.hytale.server.core.entity.InteractionContext
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler
 *  com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 */
package com.yourname.companion.systems;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.function.Consumer;

public final class CompanionOpenUiInteraction
extends SimpleInstantInteraction {
    public static final BuilderCodec<CompanionOpenUiInteraction> CODEC = BuilderCodec.builder(CompanionOpenUiInteraction.class, CompanionOpenUiInteraction::new, (BuilderCodec)SimpleInstantInteraction.CODEC).build();
    private static volatile Consumer<PlayerRef> openUiCallback;

    public CompanionOpenUiInteraction() {
        super("CompanionOpenUi");
    }

    public static void setOpenUiCallback(Consumer<PlayerRef> callback) {
        openUiCallback = callback;
    }

    protected void firstRun(InteractionType actionType, InteractionContext context, CooldownHandler cooldownHandler) {
        if (context == null) {
            return;
        }
        Ref entityRef = context.getEntity();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }
        CommandBuffer commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            return;
        }
        Player player = (Player)commandBuffer.getComponent(entityRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        PlayerRef playerRef = player.getPlayerRef();
        if (playerRef == null) {
            return;
        }
        Consumer<PlayerRef> cb = openUiCallback;
        if (cb != null) {
            cb.accept(playerRef);
        }
    }
}

