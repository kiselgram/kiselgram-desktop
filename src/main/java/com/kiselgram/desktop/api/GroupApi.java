package com.kiselgram.desktop.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.kiselgram.desktop.model.ChatItem;

import java.util.List;
import java.util.Map;

public class GroupApi {
    private final ApiClient client;
    private final Gson gson = new Gson();

    public GroupApi(ApiClient client) { this.client = client; }

    public List<ChatItem.GroupInfo> getGroups() throws Exception {
        var res = client.get("/groups");
        var data = res.getAsJsonObject("data");
        return gson.fromJson(data.getAsJsonArray("groups"), new TypeToken<List<ChatItem.GroupInfo>>(){}.getType());
    }

    public JsonObject getGroupMessages(int groupId, int after, int limit) throws Exception {
        var res = client.get("/group_messages/" + groupId, Map.of(
                "after", String.valueOf(after), "limit", String.valueOf(limit)
        ));
        return res.getAsJsonObject("data");
    }

    public void sendGroupMessage(int groupId, String content) throws Exception {
        client.post("/send_group_message", Map.of("group_id", groupId, "content", content));
    }

    public void sendGroupMessage(int groupId, String content, Integer replyToId) throws Exception {
        var params = new java.util.HashMap<String, Object>();
        params.put("group_id", groupId);
        params.put("content", content);
        if (replyToId != null) params.put("reply_to_id", replyToId);
        client.post("/send_group_message", params);
    }

    public JsonObject createGroup(String name, String description, List<Integer> memberIds) throws Exception {
        var params = new java.util.HashMap<String, Object>();
        params.put("name", name);
        params.put("description", description != null ? description : "");
        params.put("member_ids", memberIds);
        var res = client.post("/groups/create", params);
        return res.getAsJsonObject("data");
    }

    public void updateGroup(int groupId, String name, String description) throws Exception {
        var params = new java.util.HashMap<String, Object>();
        if (name != null) params.put("name", name);
        if (description != null) params.put("description", description);
        client.post("/groups/" + groupId + "/update", params);
    }

    public void leaveGroup(int groupId) throws Exception {
        client.post("/leave_group/" + groupId);
    }
}
