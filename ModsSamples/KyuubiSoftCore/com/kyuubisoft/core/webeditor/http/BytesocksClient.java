/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.webeditor.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class BytesocksClient {
    private final String wsUrl;
    private final String httpUrl;
    private final HttpClient httpClient;

    public BytesocksClient(String wsUrl) {
        this.wsUrl = wsUrl.endsWith("/") ? wsUrl.substring(0, wsUrl.length() - 1) : wsUrl;
        this.httpUrl = this.wsUrl.replaceFirst("^wss://", "https://").replaceFirst("^ws://", "http://");
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();
    }

    public String createChannel() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(this.httpUrl + "/create")).header("User-Agent", "KyuubiSoft-Editor/1.0").GET().timeout(Duration.ofSeconds(10L)).build();
        HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new IOException("Failed to create Bytesocks channel: HTTP " + response.statusCode() + " " + response.body());
        }
        String body = response.body().trim();
        if (body.startsWith("{")) {
            int keyStart = body.indexOf("\"key\"");
            if (keyStart == -1) {
                throw new IOException("Bytesocks /create response missing 'key': " + body);
            }
            int valueStart = body.indexOf(34, body.indexOf(58, keyStart)) + 1;
            int valueEnd = body.indexOf(34, valueStart);
            return body.substring(valueStart, valueEnd);
        }
        return body;
    }

    public CompletableFuture<WebSocket> connect(String channelId, WebSocket.Listener listener) {
        return this.httpClient.newWebSocketBuilder().header("User-Agent", "KyuubiSoft-Editor/1.0").connectTimeout(Duration.ofSeconds(10L)).buildAsync(URI.create(this.wsUrl + "/" + channelId), listener);
    }
}

