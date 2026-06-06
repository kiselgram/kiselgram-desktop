package com.kiselgram.desktop.ui;

import com.kiselgram.desktop.api.ApiClient;
import com.kiselgram.desktop.api.ChatApi;
import com.kiselgram.desktop.model.ChatItem;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

public class ForwardDialog extends Stage {
    public ForwardDialog(ApiClient client, ChatApi chatApi, String messageContent, Stage owner) {
        setTitle("Forward Message");
        initOwner(owner);

        var root = new VBox(8);
        root.setStyle("-fx-background-color: #17212b; -fx-padding: 16;");

        var title = new Label("Select a chat:");
        title.setStyle("-fx-text-fill: #fff; -fx-font-size: 16px; -fx-font-weight: bold;");

        var listView = new ListView<ChatItem>();
        listView.setStyle("-fx-background-color: #242f3d; -fx-control-inner-background: #242f3d;");
        listView.setPrefHeight(300);

        var loading = new Label("Loading chats...");
        loading.setStyle("-fx-text-fill: #8e9297;");

        var cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #242f3d; -fx-text-fill: #fff; -fx-padding: 8 20; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> close());

        root.getChildren().addAll(title, loading, cancelBtn);

        var scene = new Scene(root, 340, 420);
        setScene(scene);

        new Thread(() -> {
            try {
                var chats = chatApi.getChatList();
                Platform.runLater(() -> {
                    root.getChildren().remove(loading);
                    listView.getItems().setAll(chats);
                    listView.setCellFactory(lv -> new ChatCell());
                    listView.setOnMouseClicked(e -> {
                        var chat = listView.getSelectionModel().getSelectedItem();
                        if (chat != null) {
                            forwardMessage(chat, messageContent, client);
                            close();
                        }
                    });
                    root.getChildren().add(1, listView);
                });
            } catch (Exception e) {
                Platform.runLater(() -> loading.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    private void forwardMessage(ChatItem target, String content, ApiClient client) {
        new Thread(() -> {
            try {
                if (target.getPeer() != null) {
                    client.post("/send_message", java.util.Map.of(
                            "receiver_id", target.getPeer().getUserId(),
                            "content", "[Forwarded] " + content
                    ));
                } else if (target.getGroup() != null) {
                    client.post("/send_group_message", java.util.Map.of(
                            "group_id", target.getGroup().getGroupId(),
                            "content", "[Forwarded] " + content
                    ));
                } else if (target.getChannel() != null) {
                    client.post("/send_channel_message", java.util.Map.of(
                            "channel_id", target.getChannel().getChannelId(),
                            "content", "[Forwarded] " + content
                    ));
                }
            } catch (Exception ignored) {}
        }).start();
    }
}
