/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.webeditor;

public class WebEditorConfig {
    private boolean enabled = true;
    private String _enabled_comment = "Auf true setzen um /kseditor zu aktivieren. Bytebin + Bytesocks muessen erreichbar sein.";
    private String editorUrl = "https://editor.kyuubisoft.com";
    private String bytebinUrl = "https://editor.kyuubisoft.com/api/bin";
    private String bytesocksUrl = "wss://editor.kyuubisoft.com/api/ws";
    private int sessionTimeoutMinutes = 120;

    public boolean isEnabled() {
        return this.enabled;
    }

    public String getEditorUrl() {
        return this.editorUrl;
    }

    public String getBytebinUrl() {
        return this.bytebinUrl;
    }

    public String getBytesocksUrl() {
        return this.bytesocksUrl;
    }

    public int getSessionTimeoutMinutes() {
        return this.sessionTimeoutMinutes;
    }
}

