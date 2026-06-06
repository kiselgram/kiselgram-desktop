package com.kiselgram.desktop.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kiselgram.desktop.model.ChatItem;
import com.kiselgram.desktop.model.User;

import java.util.List;
import java.util.Map;

public class SearchApi {
    private final ApiClient client;
    private final Gson gson = new Gson();

    public SearchApi(ApiClient client) { this.client = client; }

    public static class GlobalSearchResult {
        public List<User> users;
        public List<ChatItem.GroupInfo> groups;
        public List<ChatItem.ChannelInfo> channels;
    }

    @SuppressWarnings("unchecked")
    public GlobalSearchResult globalSearch(String query) throws Exception {
        var res = client.get("/search/global", Map.of("q", query));
        var data = res.getAsJsonObject("data");
        var results = data.getAsJsonObject("results");
        var resultObj = new GlobalSearchResult();

        if (results.has("users")) resultObj.users = gson.fromJson(results.getAsJsonArray("users"), new TypeToken<List<User>>(){}.getType());
        else resultObj.users = List.of();

        if (results.has("groups")) resultObj.groups = gson.fromJson(results.getAsJsonArray("groups"), new TypeToken<List<ChatItem.GroupInfo>>(){}.getType());
        else resultObj.groups = List.of();

        if (results.has("channels")) resultObj.channels = gson.fromJson(results.getAsJsonArray("channels"), new TypeToken<List<ChatItem.ChannelInfo>>(){}.getType());
        else resultObj.channels = List.of();

        return resultObj;
    }
}
