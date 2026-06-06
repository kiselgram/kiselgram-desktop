package com.kiselgram.desktop.api;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import com.kiselgram.desktop.model.ChatItem;
import com.kiselgram.desktop.model.Message;
import com.kiselgram.desktop.model.User;

import java.util.List;
import java.util.Map;

public class ChatApi {
    private final ApiClient client;
    private final Gson gson = new Gson();

    public ApiClient getClient() { return client; }

    public ChatApi(ApiClient client) { this.client = client; }

    public List<ChatItem> getChatList() throws Exception {
        var res = client.get("/chat_list");
        var data = res.getAsJsonObject("data");
        var chats = data.getAsJsonArray("chats");
        return gson.fromJson(chats, new TypeToken<List<ChatItem>>(){}.getType());
    }

    public JsonObject getMessages(int userId, int after, int limit) throws Exception {
        var res = client.get("/messages/" + userId, Map.of(
                "after", String.valueOf(after), "limit", String.valueOf(limit)
        ));
        return res.getAsJsonObject("data");
    }

    public JsonObject sendMessage(int receiverId, String content) throws Exception {
        var res = client.post("/send_message", Map.of(
                "receiver_id", receiverId, "content", content
        ));
        return res.getAsJsonObject("data");
    }

    public JsonObject sendMessage(int receiverId, String content, Integer replyToId) throws Exception {
        var params = new java.util.HashMap<String, Object>();
        params.put("receiver_id", receiverId);
        params.put("content", content);
        if (replyToId != null) params.put("reply_to_id", replyToId);
        var res = client.post("/send_message", params);
        return res.getAsJsonObject("data");
    }

    public void markRead(int userId) throws Exception {
        client.post("/mark_read/" + userId);
    }

    public void addReaction(int messageId, String emoji) throws Exception {
        client.post("/reactions/add", Map.of("message_id", messageId, "reaction_type", emoji));
    }

    public void deleteMessage(int messageId) throws Exception {
        client.post("/messages/" + messageId + "/delete");
    }

    public void editMessage(int messageId, String content) throws Exception {
        client.post("/messages/" + messageId + "/edit", Map.of("content", content));
    }

    public void sendTyping(String chatType, int chatId) throws Exception {
        try {
            client.post("/typing/" + chatType + "/" + chatId);
        } catch (Exception ignored) {}
    }

    public List<Integer> getTypingUsers(String chatType, int chatId) throws Exception {
        try {
            var res = client.get("/typing/" + chatType + "/" + chatId);
            var data = res.getAsJsonObject("data");
            var usersArr = data.getAsJsonArray("users");
            return new Gson().fromJson(usersArr, new TypeToken<List<Integer>>(){}.getType());
        } catch (Exception e) {
            return List.of();
        }
    }

    public JsonObject getSavedMessages(int after, int limit) throws Exception {
        var res = client.get("/saved_messages", Map.of(
                "after", String.valueOf(after), "limit", String.valueOf(limit)
        ));
        return res.getAsJsonObject("data");
    }

    public JsonObject getChannelMessages(int channelId, int after, int limit) throws Exception {
        var res = client.get("/channel_messages/" + channelId, Map.of(
                "after", String.valueOf(after), "limit", String.valueOf(limit)
        ));
        return res.getAsJsonObject("data");
    }

    public void saveMessage(int messageId) throws Exception {
        client.post("/saved_messages", Map.of("message_id", messageId));
    }

    public List<Integer> getPinnedChats() throws Exception {
        try {
            var res = client.get("/pinned");
            var data = res.getAsJsonObject("data");
            var pinnedArr = data.getAsJsonArray("pinned");
            return new Gson().fromJson(pinnedArr, new TypeToken<List<Integer>>(){}.getType());
        } catch (Exception e) {
            return List.of();
        }
    }

    public void pinChat(int chatId) throws Exception {
        client.post("/pin", Map.of("chat_id", chatId));
    }

    public void unpinChat(int chatId) throws Exception {
        client.post("/unpin", Map.of("chat_id", chatId));
    }

    public JsonObject deleteChat(int userId) throws Exception {
        return client.post("/delete_chat/" + userId);
    }
}
