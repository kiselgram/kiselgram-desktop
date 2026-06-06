package com.kiselgram.desktop.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kiselgram.desktop.config.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

public class ApiClient {
    private static final Gson gson = new Gson();
    private final HttpClient client;
    private String token;

    public ApiClient() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void setToken(String token) { this.token = token; }
    public String getToken() { return token; }
    public boolean hasToken() { return token != null && !token.isEmpty(); }

    private HttpRequest.Builder request(String path) {
        var builder = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.endpoint(path)))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    public JsonObject get(String path) throws Exception {
        var req = request(path).GET().build();
        var res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return handleResponse(res);
    }

    public JsonObject get(String path, Map<String, String> query) throws Exception {
        var uri = URI.create(AppConfig.endpoint(path));
        var qs = query.entrySet().stream()
                .map(e -> e.getKey() + "=" + java.net.URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .reduce((a, b) -> a + "&" + b)
                .orElse("");
        var fullUri = URI.create(uri + "?" + qs);
        var req = HttpRequest.newBuilder(fullUri)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + (token != null ? token : ""))
                .GET().build();
        var res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return handleResponse(res);
    }

    public JsonObject post(String path, Map<String, Object> body) throws Exception {
        var json = gson.toJson(body);
        var req = request(path).POST(HttpRequest.BodyPublishers.ofString(json)).build();
        var res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return handleResponse(res);
    }

    public JsonObject post(String path) throws Exception {
        var req = request(path).POST(HttpRequest.BodyPublishers.noBody()).build();
        var res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return handleResponse(res);
    }

    public JsonObject uploadFile(String path, Path filePath, Map<String, String> fields) throws Exception {
        var boundary = "----KiselgramBoundary" + System.currentTimeMillis() + "Hex";
        var os = new java.io.ByteArrayOutputStream();

        for (var entry : fields.entrySet()) {
            os.write(("--" + boundary + "\r\n").getBytes());
            os.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n").getBytes());
            os.write((entry.getValue() + "\r\n").getBytes());
        }

        var fileName = filePath.getFileName().toString();
        var mimeType = Files.probeContentType(filePath);
        if (mimeType == null) mimeType = "application/octet-stream";

        os.write(("--" + boundary + "\r\n").getBytes());
        os.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n").getBytes());
        os.write(("Content-Type: " + mimeType + "\r\n\r\n").getBytes());
        os.write(Files.readAllBytes(filePath));
        os.write(("\r\n--" + boundary + "--\r\n").getBytes());

        var req = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.endpoint(path)))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Authorization", "Bearer " + (token != null ? token : ""))
                .POST(HttpRequest.BodyPublishers.ofByteArray(os.toByteArray()))
                .timeout(Duration.ofSeconds(60))
                .build();

        var res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return handleResponse(res);
    }

    public JsonObject put(String path, Map<String, Object> body) throws Exception {
        var json = gson.toJson(body);
        var req = request(path).PUT(HttpRequest.BodyPublishers.ofString(json)).build();
        var res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return handleResponse(res);
    }

    public JsonObject delete(String path) throws Exception {
        var req = request(path).DELETE().build();
        var res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return handleResponse(res);
    }

    private JsonObject handleResponse(HttpResponse<String> res) throws Exception {
        var obj = gson.fromJson(res.body(), JsonObject.class);
        if (res.statusCode() >= 400 && obj.has("error")) {
            var err = obj.getAsJsonObject("error");
            var msg = err.has("message") ? err.get("message").getAsString() : "Unknown error";
            throw new ApiException(res.statusCode(), msg);
        }
        return obj;
    }

    public static class ApiException extends Exception {
        private final int statusCode;
        public ApiException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }
        public int getStatusCode() { return statusCode; }
    }
}
