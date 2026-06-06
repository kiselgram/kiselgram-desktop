package com.kiselgram.desktop.ui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kiselgram.desktop.api.ApiClient;
import com.kiselgram.desktop.api.ChatApi;
import com.kiselgram.desktop.api.GroupApi;
import com.kiselgram.desktop.api.SearchApi;
import com.kiselgram.desktop.controller.SessionManager;
import com.kiselgram.desktop.model.ChatItem;
import com.kiselgram.desktop.model.User;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SearchView extends VBox {
    private final ApiClient client;
    private final ChatApi chatApi;
    private final GroupApi groupApi;
    private final SessionManager session;
    private final Consumer<ChatItem> onOpenChat;
    private final TextField searchField = new TextField();
    private final VBox resultsArea = new VBox(8);
    private final Label statusLabel = new Label("");

    public SearchView(ApiClient client, ChatApi chatApi, GroupApi groupApi, SessionManager session, Consumer<ChatItem> onOpenChat) {
        this.client = client;
        this.chatApi = chatApi;
        this.groupApi = groupApi;
        this.session = session;
        this.onOpenChat = onOpenChat;

        setPadding(new Insets(8));
        setSpacing(8);
        setStyle("-fx-background-color: #17212b;");

        searchField.setPromptText("Search users, groups, channels...");
        searchField.setStyle("-fx-background-color: #242f3d; -fx-text-fill: #fff; -fx-prompt-text-fill: #8e9297; -fx-padding: 10 14; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 14px;");
        searchField.textProperty().addListener((obs, old, val) -> {
            if (val.length() >= 2) search(val.trim());
            else {
                resultsArea.getChildren().clear();
                statusLabel.setText("");
            }
        });

        statusLabel.setStyle("-fx-text-fill: #8e9297; -fx-padding: 4; -fx-font-size: 13px;");

        var scroll = new ScrollPane(resultsArea);
        scroll.setStyle("-fx-background: #17212b; -fx-background-color: #17212b;");
        scroll.setFitToWidth(true);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        getChildren().addAll(searchField, statusLabel, scroll);
    }

    private void search(String query) {
        statusLabel.setText("Searching...");
        resultsArea.getChildren().clear();

        new Thread(() -> {
            try {
                var searchApi = new SearchApi(client);
                var result = searchApi.globalSearch(query);

                Platform.runLater(() -> {
                    resultsArea.getChildren().clear();

                    if (!result.users.isEmpty()) {
                        var usersHeader = new Label("Users (" + result.users.size() + ")");
                        usersHeader.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 4 2 4;");
                        resultsArea.getChildren().add(usersHeader);
                        for (var user : result.users) {
                            resultsArea.getChildren().add(createUserItem(user));
                        }
                    }

                    if (!result.groups.isEmpty()) {
                        var groupsHeader = new Label("Groups (" + result.groups.size() + ")");
                        groupsHeader.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 4 2 4;");
                        resultsArea.getChildren().add(groupsHeader);
                        for (var g : result.groups) {
                            var ci = new ChatItem();
                            ci.setPeer(new User() {{ setDisplayName(g.getName()); }});
                            resultsArea.getChildren().add(createGenericItem(g.getName(), g.getMemberCount() + " members", "#2b5278", ci));
                        }
                    }

                    if (!result.channels.isEmpty()) {
                        var chHeader = new Label("Channels (" + result.channels.size() + ")");
                        chHeader.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 4 2 4;");
                        resultsArea.getChildren().add(chHeader);
                        for (var ch : result.channels) {
                            var ci = new ChatItem();
                            ci.setPeer(new User() {{ setDisplayName(ch.getName()); }});
                            resultsArea.getChildren().add(createGenericItem(ch.getName(), ch.getSubscriberCount() + " subscribers", "#6ab2f2", ci));
                        }
                    }

                    int total = result.users.size() + result.groups.size() + result.channels.size();
                    statusLabel.setText(total + " result(s) found");
                    if (total == 0) {
                        resultsArea.getChildren().add(new Label("No results") {{
                            setStyle("-fx-text-fill: #8e9297; -fx-padding: 20;");
                        }});
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    resultsArea.getChildren().clear();
                    statusLabel.setText("Error: " + ex.getMessage());
                });
            }
        }).start();
    }

    private HBox createUserItem(User user) {
        var avatar = new Label(user.getDisplayName().substring(0, 1).toUpperCase());
        avatar.setMinSize(36, 36);
        avatar.setMaxSize(36, 36);
        avatar.setStyle("-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 18; -fx-alignment: center;");

        var nameLabel = new Label(user.getDisplayName());
        nameLabel.setStyle("-fx-text-fill: #fff; -fx-font-size: 15px;");
        var userLabel = new Label("@" + user.getUsername());
        userLabel.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 12px;");
        var textBox = new VBox(1, nameLabel, userLabel);

        var hbox = new HBox(10, avatar, textBox);
        hbox.setPadding(new Insets(6, 12, 6, 12));
        hbox.setStyle("-fx-background-color: #17212b; -fx-border-color: #1e2d3a; -fx-border-width: 0 0 1 0;");
        hbox.setOnMouseEntered(e -> hbox.setStyle("-fx-background-color: #1e2d3a; -fx-border-color: #1e2d3a; -fx-border-width: 0 0 1 0;"));
        hbox.setOnMouseExited(e -> hbox.setStyle("-fx-background-color: #17212b; -fx-border-color: #1e2d3a; -fx-border-width: 0 0 1 0;"));

        var chatItem = new ChatItem();
        chatItem.setPeer(user);
        hbox.setOnMouseClicked(e -> {
            if (onOpenChat != null) onOpenChat.accept(chatItem);
        });

        return hbox;
    }

    private HBox createGenericItem(String name, String subtitle, String color, ChatItem chatItem) {
        var avatar = new Label(name.substring(0, 1).toUpperCase());
        avatar.setMinSize(36, 36);
        avatar.setMaxSize(36, 36);
        avatar.setStyle("-fx-background-color: " + color + "; -fx-text-fill: #fff; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 18; -fx-alignment: center;");

        var nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: #fff; -fx-font-size: 15px;");
        var subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 12px;");
        var textBox = new VBox(1, nameLabel, subLabel);

        var hbox = new HBox(10, avatar, textBox);
        hbox.setPadding(new Insets(6, 12, 6, 12));
        hbox.setStyle("-fx-background-color: #17212b; -fx-border-color: #1e2d3a; -fx-border-width: 0 0 1 0;");
        hbox.setOnMouseEntered(e -> hbox.setStyle("-fx-background-color: #1e2d3a; -fx-border-color: #1e2d3a; -fx-border-width: 0 0 1 0;"));
        hbox.setOnMouseExited(e -> hbox.setStyle("-fx-background-color: #17212b; -fx-border-color: #1e2d3a; -fx-border-width: 0 0 1 0;"));
        hbox.setOnMouseClicked(e -> {
            if (onOpenChat != null) onOpenChat.accept(chatItem);
        });

        return hbox;
    }
}
