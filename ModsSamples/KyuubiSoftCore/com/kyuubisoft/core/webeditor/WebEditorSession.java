/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.webeditor;

import com.google.gson.JsonObject;
import com.kyuubisoft.core.webeditor.socket.EditorSocket;
import com.kyuubisoft.core.webeditor.socket.SocketMessageType;
import java.util.UUID;
import java.util.logging.Logger;

public class WebEditorSession {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft WebEditor");
    private final String sessionId = UUID.randomUUID().toString().substring(0, 8);
    private final UUID playerUuid;
    private final String playerName;
    private final long createdAt;
    private EditorSocket socket;
    private String pendingTrustNonce;
    private boolean trusted = false;
    private boolean completed = false;

    public WebEditorSession(UUID playerUuid, String playerName) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.createdAt = System.currentTimeMillis();
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public UUID getPlayerUuid() {
        return this.playerUuid;
    }

    public String getPlayerName() {
        return this.playerName;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

    public boolean isCompleted() {
        return this.completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public EditorSocket getSocket() {
        return this.socket;
    }

    public void setSocket(EditorSocket socket) {
        this.socket = socket;
    }

    public boolean isTrusted() {
        return this.trusted;
    }

    public void setAutoTrusted() {
        this.trusted = true;
        LOGGER.info("Session " + this.sessionId + " auto-trusted (console)");
    }

    public String getPendingTrustNonce() {
        return this.pendingTrustNonce;
    }

    public void onEditorHello(String nonce) {
        this.pendingTrustNonce = nonce;
        LOGGER.info("Editor hello received for session " + this.sessionId + ", nonce: " + nonce);
    }

    public void confirmTrust(String nonce) {
        if (this.pendingTrustNonce != null && this.pendingTrustNonce.equals(nonce)) {
            this.trusted = true;
            this.pendingTrustNonce = null;
            if (this.socket != null) {
                JsonObject msg = new JsonObject();
                this.socket.send(SocketMessageType.ACCEPTED, msg);
            }
            LOGGER.info("Editor trusted for session " + this.sessionId);
        }
    }

    public void onSocketClosed() {
        LOGGER.info("Socket closed for session " + this.sessionId);
    }

    public void close() {
        this.completed = true;
        if (this.socket != null && !this.socket.isClosed()) {
            this.socket.close();
        }
    }

    public boolean isExpired(int timeoutMinutes) {
        return System.currentTimeMillis() - this.createdAt > (long)timeoutMinutes * 60L * 1000L;
    }
}

