package com.kiselgram.desktop.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kiselgram.desktop.model.User;

import java.util.Map;

public class ProfileApi {
    private final ApiClient client;
    private final Gson gson = new Gson();

    public ProfileApi(ApiClient client) { this.client = client; }

    public User getProfile() throws Exception {
        var res = client.get("/profile");
        var data = res.getAsJsonObject("data");
        var userData = data.getAsJsonObject("user");
        return gson.fromJson(userData, User.class);
    }

    public void updateProfile(String displayName, String bio, String statusEmoji) throws Exception {
        var params = new java.util.HashMap<String, Object>();
        if (displayName != null) params.put("display_name", displayName);
        if (bio != null) params.put("bio", bio);
        if (statusEmoji != null) params.put("status_emoji", statusEmoji);
        client.put("/profile", params);
    }

    public void uploadAvatar(String filePath) throws Exception {
        client.uploadFile("/files/upload_avatar", java.nio.file.Paths.get(filePath), Map.of());
    }
}
