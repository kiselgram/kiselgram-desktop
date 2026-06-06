package com.kiselgram.desktop.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kiselgram.desktop.model.ChatItem;
import com.kiselgram.desktop.model.Contact;

import java.util.List;
import java.util.Map;

public class ContactApi {
    private final ApiClient client;
    private final Gson gson = new Gson();

    public ContactApi(ApiClient client) { this.client = client; }

    public List<Contact> getContacts() throws Exception {
        var res = client.get("/contacts");
        var data = res.getAsJsonObject("data");
        return gson.fromJson(data.getAsJsonArray("contacts"), new TypeToken<List<Contact>>(){}.getType());
    }

    public void addContact(int userId) throws Exception {
        client.post("/contacts", Map.of("contact_id", userId));
    }

    public void removeContact(int userId) throws Exception {
        client.delete("/contacts/" + userId);
    }

    public void blockUser(int userId) throws Exception {
        client.post("/block_user/" + userId);
    }

    public void unblockUser(int userId) throws Exception {
        client.post("/unblock_user/" + userId);
    }

    public List<Integer> getBlockedUsers() throws Exception {
        var res = client.get("/blocked_users");
        var data = res.getAsJsonObject("data");
        var usersArr = data.getAsJsonArray("blocked_users");
        return gson.fromJson(usersArr, new TypeToken<List<Integer>>(){}.getType());
    }
}
