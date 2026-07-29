/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.webeditor.socket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kyuubisoft.core.webeditor.WebEditorService;
import com.kyuubisoft.core.webeditor.WebEditorSession;
import com.kyuubisoft.core.webeditor.http.BytesocksClient;
import com.kyuubisoft.core.webeditor.socket.SocketMessageType;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EditorSocket
implements WebSocket.Listener {
    private static final Logger LOGGER = Logger.getLogger("KyuubiSoft WebEditor");
    private static final Gson GSON = new Gson();
    private final WebEditorSession session;
    private final WebEditorService service;
    private volatile WebSocket webSocket;
    private volatile String channelId;
    private volatile boolean closed = false;
    private final StringBuilder messageBuffer = new StringBuilder();

    public EditorSocket(WebEditorSession session, WebEditorService service) {
        this.session = session;
        this.service = service;
    }

    public String initialize(BytesocksClient client) throws Exception {
        this.channelId = client.createChannel();
        CompletableFuture<WebSocket> future = client.connect(this.channelId, this);
        this.webSocket = future.get(10L, TimeUnit.SECONDS);
        LOGGER.info("WebSocket connected to channel: " + this.channelId);
        return this.channelId;
    }

    public String getChannelId() {
        return this.channelId;
    }

    public boolean isClosed() {
        return this.closed;
    }

    public void send(JsonObject message) {
        if (this.webSocket == null || this.closed) {
            LOGGER.warning("Cannot send: WebSocket not connected");
            return;
        }
        String json = GSON.toJson(message);
        this.webSocket.sendText(json, true);
    }

    public void send(SocketMessageType type, JsonObject payload) {
        payload.addProperty("type", type.getId());
        this.send(payload);
    }

    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.webSocket != null) {
            try {
                this.webSocket.sendClose(1000, "session ended");
            }
            catch (Exception e) {
                LOGGER.log(Level.FINE, "Error closing WebSocket", e);
            }
        }
    }

    @Override
    public void onOpen(WebSocket ws) {
        LOGGER.info("WebSocket opened for session: " + this.session.getSessionId());
        ws.request(1L);
    }

    @Override
    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
        this.messageBuffer.append(data);
        if (last) {
            String fullMessage = this.messageBuffer.toString();
            this.messageBuffer.setLength(0);
            try {
                this.handleMessage(fullMessage);
            }
            catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error handling WebSocket message", e);
            }
        }
        ws.request(1L);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
        LOGGER.info("WebSocket closed: " + statusCode + " " + reason);
        this.closed = true;
        this.session.onSocketClosed();
        return null;
    }

    @Override
    public void onError(WebSocket ws, Throwable error) {
        LOGGER.log(Level.WARNING, "WebSocket error", error);
        this.closed = true;
        this.session.onSocketClosed();
    }

    private void handleMessage(String raw) {
        SocketMessageType type;
        LOGGER.info("Received WebSocket message: " + raw);
        JsonObject msg = JsonParser.parseString(raw).getAsJsonObject();
        String typeStr = msg.has("type") ? msg.get("type").getAsString() : null;
        SocketMessageType socketMessageType = type = typeStr != null ? SocketMessageType.fromId(typeStr) : null;
        if (type == null) {
            LOGGER.warning("Unknown message type: " + typeStr);
            return;
        }
        switch (type) {
            case HELLO: {
                this.handleHello(msg);
                break;
            }
            case CHANGE_REQUEST: {
                this.handleChangeRequest(msg);
                break;
            }
            case PING: {
                this.handlePing();
                break;
            }
            default: {
                LOGGER.fine("Ignoring message type: " + String.valueOf((Object)type));
            }
        }
    }

    private void handleHello(JsonObject msg) {
        String nonce;
        String string = nonce = msg.has("nonce") ? msg.get("nonce").getAsString() : null;
        if (nonce == null) {
            LOGGER.warning("Hello message without nonce");
            return;
        }
        if (this.session.isTrusted()) {
            JsonObject response = new JsonObject();
            this.send(SocketMessageType.ACCEPTED, response);
            LOGGER.info("Auto-accepted reconnect for trusted session " + this.session.getSessionId());
        } else {
            this.session.onEditorHello(nonce);
            JsonObject response = new JsonObject();
            response.addProperty("nonce", nonce);
            this.send(SocketMessageType.HELLO, response);
        }
    }

    private void handleChangeRequest(JsonObject msg) {
        String code;
        String string = code = msg.has("code") ? msg.get("code").getAsString() : null;
        if (code == null) {
            LOGGER.warning("Change request without code");
            return;
        }
        JsonObject accepted = new JsonObject();
        accepted.addProperty("state", "accepted");
        this.send(SocketMessageType.CHANGE_RESPONSE, accepted);
        CompletableFuture.runAsync(() -> {
            try {
                String newSessionCode = this.service.applyChanges(this.session, code);
                JsonObject applied = new JsonObject();
                applied.addProperty("state", "applied");
                if (newSessionCode != null) {
                    applied.addProperty("newSessionCode", newSessionCode);
                }
                this.send(SocketMessageType.CHANGE_RESPONSE, applied);
            }
            catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to apply changes", e);
                JsonObject error = new JsonObject();
                error.addProperty("state", "error");
                error.addProperty("message", e.getMessage());
                this.send(SocketMessageType.CHANGE_RESPONSE, error);
            }
        });
    }

    private void handlePing() {
        this.send(SocketMessageType.PONG, new JsonObject());
    }
}

