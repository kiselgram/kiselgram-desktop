package com.kiselgram.desktop.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.kiselgram.desktop.model.ChatItem;

import java.util.List;
import java.util.Map;

public class ChannelApi {
    private final ApiClient client;
    private final Gson gson = new Gson();

    public ChannelApi(ApiClient client) { this.client = client; }

    public List<ChatItem.ChannelInfo> getSubscribedChannels() throws Exception {
        var res = client.get("/chat_list");
        var data = res.getAsJsonObject("data");
        var chats = data.getAsJsonArray("chats");
        var type = new TypeToken<List<ChatItem>>(){}.getType();
        @SuppressWarnings("unchecked")
        List<ChatItem> all = (List<ChatItem>) gson.fromJson(chats, type);
        return all.stream()
                .filter(c -> "channel".equals(c.getChatType()))
                .map(c -> {
                    var info = c.getChannel();
                    info.setLastMessage(c.getLastMessage());
                    return info;
                })
                .toList();
    }

    public JsonObject getChannelMessages(int channelId, int after, int limit) throws Exception {
        var res = client.get("/channel_messages/" + channelId, Map.of(
                "after", String.valueOf(after), "limit", String.valueOf(limit)
        ));
        return res.getAsJsonObject("data");
    }

    public JsonObject sendChannelMessage(int channelId, String content) throws Exception {
        var res = client.post("/send_channel_message", Map.of(
                "channel_id", channelId, "content", content
        ));
        return res.getAsJsonObject("data");
    }

    public void subscribe(int channelId) throws Exception {
        client.post("/channels/" + channelId + "/subscribe");
    }

    public void unsubscribe(int channelId) throws Exception {
        client.post("/channels/" + channelId + "/unsubscribe");
    }
}
