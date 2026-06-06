package com.kiselgram.desktop.ui;

import com.kiselgram.desktop.api.ApiClient;
import com.kiselgram.desktop.api.ChatApi;
import com.kiselgram.desktop.api.GroupApi;
import com.kiselgram.desktop.api.StoriesApi;
import com.kiselgram.desktop.controller.SessionManager;
import com.kiselgram.desktop.model.ChatItem;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ChatListView extends VBox {
    private final ApiClient client;
    private final ChatApi chatApi;
    private final GroupApi groupApi;
    private final SessionManager session;
    private final TrayManager tray;
    private final Consumer<ChatItem> onOpenChat;
    private final ListView<ChatItem> listView = new ListView<>();
    private final Label loadingLabel = new Label("Loading...");
    private final HBox storyBar = new HBox(8);
    private volatile boolean polling = true;
    private int prevUnreadTotal = 0;

    public ChatListView(ApiClient client, ChatApi chatApi, GroupApi groupApi, SessionManager session, TrayManager tray, Consumer<ChatItem> onOpenChat) {
        this.client = client;
        this.chatApi = chatApi;
        this.groupApi = groupApi;
        this.session = session;
        this.tray = tray;
        this.onOpenChat = onOpenChat;

        setPadding(new Insets(0));
        setSpacing(0);
        setStyle("-fx-background-color: #17212b;");

        loadingLabel.setStyle("-fx-text-fill: #8e9297; -fx-padding: 24; -fx-font-size: 14px;");

        storyBar.setPadding(new Insets(8, 10, 8, 10));
        storyBar.setStyle("-fx-background-color: #17212b; -fx-border-color: #1e2d3a; -fx-border-width: 0 0 1 0;");
        storyBar.setVisible(false);

        listView.setStyle("-fx-background-color: #17212b; -fx-control-inner-background: #17212b;");
        listView.setCellFactory(lv -> new ChatCell(this::onChatAction));
        listView.setOnMouseClicked(e -> {
            var chat = listView.getSelectionModel().getSelectedItem();
            if (chat != null && onOpenChat != null) {
                onOpenChat.accept(chat);
            }
        });

        getChildren().addAll(storyBar, loadingLabel);
        startPolling();
        loadStories();
    }

    private void onChatAction(ChatItem chat, String action) {
        switch (action) {
            case "pin" -> {
                var peerId = chat.getPeer() != null ? chat.getPeer().getUserId() : 0;
                if (peerId > 0) new Thread(() -> { try { chatApi.pinChat(peerId); loadChats(); } catch (Exception ignored) {} }).start();
            }
            case "unpin" -> {
                var peerId = chat.getPeer() != null ? chat.getPeer().getUserId() : 0;
                if (peerId > 0) new Thread(() -> { try { chatApi.unpinChat(peerId); loadChats(); } catch (Exception ignored) {} }).start();
            }
            case "delete" -> {
                var peerId = chat.getPeer() != null ? chat.getPeer().getUserId() : 0;
                if (peerId > 0) new Thread(() -> { try { chatApi.deleteChat(peerId); loadChats(); } catch (Exception ignored) {} }).start();
            }
        }
    }

    private void startPolling() {
        new Thread(() -> {
            while (polling) {
                try {
                    Thread.sleep(5000);
                    if (!polling) break;
                    var chats = chatApi.getChatList();
                    int unreadTotal = chats.stream().mapToInt(ChatItem::getUnreadCount).sum();
                    if (unreadTotal > prevUnreadTotal) {
                        int diff = unreadTotal - prevUnreadTotal;
                        for (var c : chats) {
                            if (c.getUnreadCount() > 0 && c.getLastMessage() != null) {
                                tray.notify(c.getTitle(), c.getLastMessage().getContent() != null ?
                                        c.getLastMessage().getContent() : "New media");
                                diff -= c.getUnreadCount();
                                if (diff <= 0) break;
                            }
                        }
                    }
                    prevUnreadTotal = unreadTotal;
                    Platform.runLater(() -> listView.getItems().setAll(chats));
                } catch (Exception ignored) {}
            }
        }).start();
    }

    public void loadChats() {
        loadingLabel.setText("Loading...");
        if (!getChildren().contains(loadingLabel)) getChildren().add(getChildren().size(), loadingLabel);
        getChildren().remove(listView);

        new Thread(() -> {
            try {
                var chats = chatApi.getChatList();
                prevUnreadTotal = chats.stream().mapToInt(ChatItem::getUnreadCount).sum();
                Platform.runLater(() -> {
                    getChildren().remove(loadingLabel);
                    listView.getItems().setAll(chats);
                    if (!getChildren().contains(listView)) getChildren().add(listView);
                });
            } catch (Exception e) {
                Platform.runLater(() -> loadingLabel.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    @SuppressWarnings("unchecked")
    private void loadStories() {
        new Thread(() -> {
            try {
                var api = new StoriesApi(client);
                var storyGroups = api.getStories();
                Platform.runLater(() -> {
                    storyBar.getChildren().clear();
                    for (var group : storyGroups) {
                        var username = (String) group.get("username");
                        var avatarUrl = (String) group.get("avatar_url");
                        var hasUnviewed = (boolean) group.get("has_unviewed");
                        var stories = (List<Map<String, Object>>) group.get("stories");

                        var ring = new Label(username.substring(0, 1).toUpperCase());
                        ring.setMinSize(44, 44);
                        ring.setMaxSize(44, 44);
                        ring.setStyle("-fx-background-color: #242f3d; -fx-text-fill: #fff; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 22; -fx-alignment: center; -fx-border-color: " + (hasUnviewed ? "#2b5278" : "#3e4e5e") + "; -fx-border-width: 2;");
                        ring.setAlignment(Pos.CENTER);
                        ring.setTooltip(new Tooltip(username));
                        ring.setOnMouseClicked(e -> {
                            new StoriesViewer(username, avatarUrl, stories);
                            try { api.viewStory(((Number) stories.get(0).get("story_id")).intValue()); } catch (Exception ignored) {}
                        });
                        storyBar.getChildren().add(ring);
                    }
                    storyBar.setVisible(!storyGroups.isEmpty());
                });
            } catch (Exception ignored) {}
        }).start();
    }

    public void stopPolling() {
        polling = false;
    }
}
