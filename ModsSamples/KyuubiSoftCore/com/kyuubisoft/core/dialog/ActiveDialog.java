/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.dialog;

public class ActiveDialog {
    public String dialogId;
    public String citizenId;
    public String currentNodeId;
    public long startedAt;

    public ActiveDialog(String dialogId, String citizenId, String startNodeId) {
        this.dialogId = dialogId;
        this.citizenId = citizenId;
        this.currentNodeId = startNodeId;
        this.startedAt = System.currentTimeMillis();
    }
}

