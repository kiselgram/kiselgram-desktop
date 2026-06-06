package com.kiselgram.desktop.ui;

import com.kiselgram.desktop.api.ApiClient;
import com.kiselgram.desktop.api.GroupApi;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class CreateGroupDialog extends Stage {
    public CreateGroupDialog(ApiClient client, GroupApi groupApi, Runnable onCreated) {
        setTitle("Create Group");

        var root = new VBox(10);
        root.setStyle("-fx-background-color: #17212b; -fx-padding: 20;");
        root.setAlignment(Pos.TOP_CENTER);

        var titleLabel = new Label("New Group");
        titleLabel.setStyle("-fx-text-fill: #fff; -fx-font-size: 18px; -fx-font-weight: bold;");

        var nameField = new TextField();
        nameField.setPromptText("Group name");
        nameField.setStyle("-fx-background-color: #242f3d; -fx-text-fill: #fff; -fx-prompt-text-fill: #8e9297; -fx-padding: 10 14; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 14px; -fx-max-width: 300;");

        var descField = new TextField();
        descField.setPromptText("Description (optional)");
        descField.setStyle("-fx-background-color: #242f3d; -fx-text-fill: #fff; -fx-prompt-text-fill: #8e9297; -fx-padding: 10 14; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 14px; -fx-max-width: 300;");

        var memberField = new TextField();
        memberField.setPromptText("Member usernames (comma-separated)");
        memberField.setStyle("-fx-background-color: #242f3d; -fx-text-fill: #fff; -fx-prompt-text-fill: #8e9297; -fx-padding: 10 14; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 13px; -fx-max-width: 300;");

        var statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 12px;");

        var createBtn = new Button("Create");
        createBtn.setStyle("-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-padding: 10 32; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14px; -fx-font-weight: bold;");
        createBtn.setOnAction(e -> {
            var name = nameField.getText().trim();
            if (name.isEmpty()) { statusLabel.setText("Name required"); return; }
            createBtn.setDisable(true);
            createBtn.setText("Creating...");
            new Thread(() -> {
                try {
                    groupApi.createGroup(name, descField.getText().trim(), new ArrayList<>());
                    Platform.runLater(() -> {
                        close();
                        if (onCreated != null) onCreated.run();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        createBtn.setDisable(false);
                        createBtn.setText("Create");
                        statusLabel.setText("Error: " + ex.getMessage());
                    });
                }
            }).start();
        });

        root.getChildren().addAll(titleLabel, nameField, descField, memberField, statusLabel, createBtn);

        var scene = new Scene(root, 360, 400);
        setScene(scene);
    }
}
