/*
 * Decompiled with CFR 0.152.
 */
package com.kyuubisoft.core.webeditor.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.zip.GZIPOutputStream;

public class BytebinClient {
    private final String baseUrl;
    private final HttpClient httpClient;

    public BytebinClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();
    }

    public String postContent(String json) throws IOException, InterruptedException {
        byte[] compressed = BytebinClient.gzip(json.getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(this.baseUrl + "/post")).header("Content-Type", "application/json").header("Content-Encoding", "gzip").header("User-Agent", "KyuubiSoft-Editor/1.0").POST(HttpRequest.BodyPublishers.ofByteArray(compressed)).timeout(Duration.ofSeconds(30L)).build();
        HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new IOException("Bytebin POST failed: HTTP " + response.statusCode());
        }
        String location = response.headers().firstValue("Location").orElse(null);
        if (location == null || location.isBlank()) {
            String body = response.body();
            if (body != null && !body.isBlank()) {
                return body.trim().replace("\"", "");
            }
            throw new IOException("Bytebin POST: no Location header or body key");
        }
        return location;
    }

    public JsonElement getContent(String key) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(this.baseUrl + "/" + key)).header("User-Agent", "KyuubiSoft-Editor/1.0").header("Accept", "application/json").GET().timeout(Duration.ofSeconds(30L)).build();
        HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Bytebin GET failed: HTTP " + response.statusCode() + " for key " + key);
        }
        return JsonParser.parseString(response.body());
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos);){
            gzip.write(data);
        }
        return bos.toByteArray();
    }
}

