package com.kiselgram.desktop.api;

import com.google.gson.JsonObject;
import com.kiselgram.desktop.model.User;

import java.util.Map;

public class AuthApi {
    private final ApiClient client;

    public AuthApi(ApiClient client) { this.client = client; }

    public JsonObject register(String username, String email, String password) throws Exception {
        return client.post("/auth/register", Map.of(
                "username", username, "email", email, "password", password
        ));
    }

    public JsonObject login(String username, String password) throws Exception {
        var res = client.post("/auth/login", Map.of("username", username, "password", password));
        var data = res.getAsJsonObject("data");
        client.setToken(data.get("session_token").getAsString());
        return data;
    }

    public void logout() throws Exception {
        try { client.post("/auth/logout"); } catch (Exception ignored) {}
        client.setToken(null);
    }

    public JsonObject requestQr() throws Exception {
        return client.post("/auth/qr/request");
    }

    public JsonObject pollQr(String token) throws Exception {
        return client.get("/auth/qr/status/" + token);
    }
}
