/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState
 *  com.hypixel.hytale.server.npc.asset.builder.BuilderSupport
 *  com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase
 *  com.hypixel.hytale.server.npc.instructions.Action
 */
package com.kyuubisoft.core.citizen;

import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.kyuubisoft.core.citizen.ActionCitizenInteract;

public class BuilderActionCitizenInteract
extends BuilderActionBase {
    public String getShortDescription() {
        return "Citizen Interaction";
    }

    public String getLongDescription() {
        return "Citizen Interaction";
    }

    public BuilderDescriptorState getBuilderDescriptorState() {
        return BuilderDescriptorState.Stable;
    }

    public Action build(BuilderSupport builderSupport) {
        return new ActionCitizenInteract(this);
    }
}

