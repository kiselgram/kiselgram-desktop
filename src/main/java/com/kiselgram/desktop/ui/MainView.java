package com.kiselgram.desktop.ui;

import com.kiselgram.desktop.api.ApiClient;
import com.kiselgram.desktop.api.ChatApi;
import com.kiselgram.desktop.api.ContactApi;
import com.kiselgram.desktop.api.GroupApi;
import com.kiselgram.desktop.controller.SessionManager;
import com.kiselgram.desktop.controller.SettingsManager;
import com.kiselgram.desktop.model.ChatItem;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class MainView extends BorderPane {
    private final TabPane tabs = new TabPane();
    private final ChatListView chatList;
    private final ContactsView contactsView;
    private final GroupsView groupsView;
    private final ProfileView profileView;
    private final SearchView searchView;
    private final SettingsView settingsView;
    private final ChatApi chatApi;
    private final GroupApi groupApi;
    private final ContactApi contactApi;
    private final SessionManager session;
    private final ApiClient client;
    private final SettingsManager settings;
    private final TrayManager tray;

    public MainView(ApiClient client, SessionManager session, SettingsManager settings, TrayManager tray, Runnable onLogout) {
        this.client = client;
        this.chatApi = new ChatApi(client);
        this.groupApi = new GroupApi(client);
        this.contactApi = new ContactApi(client);
        this.session = session;
        this.settings = settings;
        this.tray = tray;

        chatList = new ChatListView(client, chatApi, groupApi, session, tray, this::openChat);
        contactsView = new ContactsView(contactApi, this::openChat);
        groupsView = new GroupsView(groupApi, client);
        profileView = new ProfileView(client, session, onLogout);
        searchView = new SearchView(client, chatApi, groupApi, session, this::openChat);
        settingsView = new SettingsView(settings);

        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color: #17212b;");

        tabs.getTabs().addAll(
            tab("Chats", chatList),
            tab("Contacts", contactsView),
            tab("Search", searchView),
            tab("Groups", groupsView),
            tab("Profile", profileView),
            tab("Settings", settingsView)
        );
        tabs.setTabMinWidth(60);
        tabs.setTabMaxWidth(120);

        setCenter(tabs);
        setStyle("-fx-background-color: #17212b;");
        setPadding(new Insets(0));
    }

    private void openChat(ChatItem chat) {
        var detail = new ChatDetailView(client, chatApi, groupApi, contactApi, session, settings, chat, () -> setCenter(tabs));
        setCenter(detail);
    }

    private Tab tab(String name, Node content) {
        var t = new Tab();
        var label = new Label(name);
        label.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4 8;");
        t.setGraphic(label);
        t.setContent(content);
        t.setStyle("-fx-background-color: #1e2d3a;");
        return t;
    }

    public void onShown() {
        chatList.loadChats();
    }
}
