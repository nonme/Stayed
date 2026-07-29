/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.entity.entities.Player
 */
package com.kyuubisoft.core.dialog;

import com.hypixel.hytale.server.core.entity.entities.Player;

public interface DialogConditionProvider {
    public boolean handles(String var1);

    public boolean evaluate(Player var1, String var2, String var3, boolean var4);
}

