package com.kiselgram.desktop.ui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kiselgram.desktop.api.ApiClient;
import com.kiselgram.desktop.api.GroupApi;
import com.kiselgram.desktop.model.ChatItem;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class GroupInfoView extends Stage {
    private static final Gson gson = new Gson();

    @SuppressWarnings("unchecked")
    public GroupInfoView(ApiClient client, GroupApi groupApi, ChatItem.GroupInfo group, Runnable onChanged) {
        setTitle(group.getName());
        var root = new VBox(12);
        root.setStyle("-fx-background-color: #17212b; -fx-padding: 20;");

        var avatar = new Label(group.getName().substring(0, 1).toUpperCase());
        avatar.setMinSize(64, 64);
        avatar.setMaxSize(64, 64);
        avatar.setStyle("-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-font-size: 28px; -fx-font-weight: bold; -fx-background-radius: 32; -fx-alignment: center;");
        avatar.setAlignment(Pos.CENTER);

        var nameLabel = new Label(group.getName());
        nameLabel.setStyle("-fx-text-fill: #fff; -fx-font-size: 20px; -fx-font-weight: bold;");

        var descLabel = new Label(group.getDescription() != null ? group.getDescription() : "No description");
        descLabel.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 13px;");
        descLabel.setWrapText(true);

        var membersLabel = new Label("Members: " + group.getMemberCount());
        membersLabel.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 13px;");

        var roleLabel = new Label("Role: " + group.getMyRole());
        roleLabel.setStyle("-fx-text-fill: #6ab2f2; -fx-font-size: 12px;");

        var membersSection = new VBox(4);
        membersSection.setPadding(new Insets(12, 0, 0, 0));
        var sectionTitle = new Label("Members");
        sectionTitle.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 14px; -fx-font-weight: bold;");

        var loading = new Label("Loading members...");
        loading.setStyle("-fx-text-fill: #8e9297;");

        membersSection.getChildren().addAll(sectionTitle, loading);

        var leaveBtn = new Button("Leave Group");
        leaveBtn.setStyle("-fx-background-color: #f87171; -fx-text-fill: #fff; -fx-padding: 8 24; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: bold;");
        leaveBtn.setOnAction(e -> {
            new Thread(() -> {
                try {
                    groupApi.leaveGroup(group.getGroupId());
                    Platform.runLater(() -> {
                        close();
                        if (onChanged != null) onChanged.run();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        var alert = new Alert(Alert.AlertType.ERROR, "Failed: " + ex.getMessage());
                        alert.show();
                    });
                }
            }).start();
        });

        var closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #242f3d; -fx-text-fill: #fff; -fx-padding: 8 24; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> close());

        root.getChildren().addAll(avatar, nameLabel, descLabel, membersLabel, roleLabel, membersSection, leaveBtn, closeBtn);

        var scene = new Scene(root, 360, 520);
        setScene(scene);

        new Thread(() -> {
            try {
                var res = client.get("/groups/" + group.getGroupId() + "/members");
                var data = res.getAsJsonObject("data");
                var membersArr = data.getAsJsonArray("members");
                List<Map<String, Object>> memberList = gson.fromJson(membersArr, new TypeToken<List<Map<String, Object>>>(){}.getType());
                Platform.runLater(() -> {
                    membersSection.getChildren().remove(loading);
                    for (var m : memberList) {
                        var username = (String) m.getOrDefault("username", "?");
                        var role = (String) m.getOrDefault("role", "member");
                        var memberItem = new HBox(8);
                        var initial = new Label(username.substring(0, 1).toUpperCase());
                        initial.setMinSize(28, 28);
                        initial.setMaxSize(28, 28);
                        initial.setStyle("-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 14; -fx-alignment: center;");
                        var nameLbl = new Label("@" + username);
                        nameLbl.setStyle("-fx-text-fill: #fff; -fx-font-size: 13px;");
                        var roleLbl = new Label(role);
                        roleLbl.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 11px;");
                        memberItem.getChildren().addAll(initial, nameLbl, roleLbl);
                        membersSection.getChildren().add(memberItem);
                    }
                });
            } catch (Exception ignored) {
                Platform.runLater(() -> loading.setText("Could not load members"));
            }
        }).start();
    }
}
