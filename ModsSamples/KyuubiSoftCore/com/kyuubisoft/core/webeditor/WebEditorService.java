/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.webeditor;

import com.google.gson.JsonElement;
import com.kyuubisoft.core.webeditor.ModConfigProvider;
import com.kyuubisoft.core.webeditor.WebEditorConfig;
import com.kyuubisoft.core.webeditor.WebEditorRequest;
import com.kyuubisoft.core.webeditor.WebEditorResponse;
import com.kyuubisoft.core.webeditor.WebEditorSession;
import com.kyuubisoft.core.webeditor.http.BytebinClient;
import com.kyuubisoft.core.webeditor.http.BytesocksClient;
import com.kyuubisoft.core.webeditor.socket.EditorSocket;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebEditorService {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft WebEditor");
    private final WebEditorConfig config;
    private final BytebinClient bytebin;
    private final BytesocksClient bytesocks;
    private final Map<String, WebEditorSession> sessions = new ConcurrentHashMap<String, WebEditorSession>();
    private final Map<UUID, WebEditorSession> playerSessions = new ConcurrentHashMap<UUID, WebEditorSession>();
    private List<ModConfigProvider> configProviders;
    private static final UUID CONSOLE_UUID = new UUID(0L, 0L);

    public WebEditorService(WebEditorConfig config) {
        this.config = config;
        this.bytebin = new BytebinClient(config.getBytebinUrl());
        this.bytesocks = new BytesocksClient(config.getBytesocksUrl());
    }

    public void setConfigProviders(List<ModConfigProvider> providers) {
        this.configProviders = providers;
    }

    public WebEditorConfig getConfig() {
        return this.config;
    }

    public String createSession(UUID playerUuid, String playerName, String serverName) throws Exception {
        WebEditorSession existing = this.playerSessions.get(playerUuid);
        if (existing != null) {
            existing.close();
            this.sessions.remove(existing.getSessionId());
        }
        WebEditorSession session = new WebEditorSession(playerUuid, playerName);
        if (CONSOLE_UUID.equals(playerUuid)) {
            session.setAutoTrusted();
        }
        WebEditorRequest request = WebEditorRequest.generate(this.configProviders, serverName, playerName, playerUuid.toString());
        try {
            EditorSocket socket = new EditorSocket(session, this);
            String channelId = socket.initialize(this.bytesocks);
            session.setSocket(socket);
            request.appendSocketInfo(channelId, 1);
            LOGGER.info("WebSocket channel created: " + channelId);
        }
        catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create WebSocket channel (continuing without)", e);
        }
        String json = request.encode();
        String pasteId = this.bytebin.postContent(json);
        LOGGER.info("Config uploaded to Bytebin: " + pasteId);
        this.sessions.put(session.getSessionId(), session);
        this.sessions.put(pasteId, session);
        this.playerSessions.put(playerUuid, session);
        return this.config.getEditorUrl() + "/remote/" + pasteId;
    }

    public String applyChanges(WebEditorSession session, String bytebinCode) throws Exception {
        JsonElement raw = this.bytebin.getContent(bytebinCode);
        if (!raw.isJsonObject()) {
            throw new IllegalStateException("Invalid change payload from Bytebin");
        }
        WebEditorResponse response = new WebEditorResponse(raw.getAsJsonObject());
        List<String> results = response.apply(this.configProviders);
        for (String result : results) {
            LOGGER.info("Apply result: " + result);
        }
        try {
            return this.createFollowUpSession(session);
        }
        catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to create follow-up session", e);
            return null;
        }
    }

    public List<String> applyChangesManual(String bytebinCode) throws Exception {
        JsonElement raw = this.bytebin.getContent(bytebinCode);
        if (!raw.isJsonObject()) {
            throw new IllegalStateException("Invalid change payload from Bytebin");
        }
        WebEditorResponse response = new WebEditorResponse(raw.getAsJsonObject());
        return response.apply(this.configProviders);
    }

    public boolean confirmTrust(UUID playerUuid, String nonce) {
        WebEditorSession session = this.playerSessions.get(playerUuid);
        if (session == null) {
            return false;
        }
        if (session.getPendingTrustNonce() == null) {
            return false;
        }
        if (!session.getPendingTrustNonce().equals(nonce)) {
            return false;
        }
        session.confirmTrust(nonce);
        return true;
    }

    public WebEditorSession getPlayerSession(UUID playerUuid) {
        return this.playerSessions.get(playerUuid);
    }

    private String createFollowUpSession(WebEditorSession session) throws Exception {
        WebEditorRequest request = WebEditorRequest.generate(this.configProviders, "follow-up", session.getPlayerName(), session.getPlayerUuid().toString());
        if (session.getSocket() != null && !session.getSocket().isClosed()) {
            request.appendSocketInfo(session.getSocket().getChannelId(), 1);
        }
        String json = request.encode();
        return this.bytebin.postContent(json);
    }

    public void cleanupExpiredSessions() {
        int timeout = this.config.getSessionTimeoutMinutes();
        this.sessions.entrySet().removeIf(entry -> {
            WebEditorSession s = (WebEditorSession)entry.getValue();
            if (s.isExpired(timeout)) {
                s.close();
                this.playerSessions.remove(s.getPlayerUuid());
                return true;
            }
            return false;
        });
    }

    public void shutdown() {
        for (WebEditorSession session : this.sessions.values()) {
            session.close();
        }
        this.sessions.clear();
        this.playerSessions.clear();
    }
}

