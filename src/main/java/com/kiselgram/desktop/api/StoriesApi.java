package com.kiselgram.desktop.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Map;

public class StoriesApi {
    private final ApiClient client;
    private final Gson gson = new Gson();

    public StoriesApi(ApiClient client) { this.client = client; }

    public List<Map<String, Object>> getStories() throws Exception {
        var res = client.get("/stories");
        var data = res.getAsJsonObject("data");
        var stories = data.getAsJsonArray("stories");
        return gson.fromJson(stories, new TypeToken<List<Map<String, Object>>>(){}.getType());
    }

    public void viewStory(int storyId) throws Exception {
        client.post("/stories/" + storyId + "/view");
    }

    public void likeStory(int storyId) throws Exception {
        client.post("/stories/" + storyId + "/like");
    }
}
