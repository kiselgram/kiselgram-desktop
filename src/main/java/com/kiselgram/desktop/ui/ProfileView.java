package com.kiselgram.desktop.ui;

import com.kiselgram.desktop.api.ApiClient;
import com.kiselgram.desktop.api.ProfileApi;
import com.kiselgram.desktop.controller.SessionManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ProfileView extends VBox {
    private final ApiClient client;
    private final SessionManager session;
    private final Runnable onLogout;
    private TextField nameField;
    private TextArea bioArea;
    private Button saveBtn;

    public ProfileView(ApiClient client, SessionManager session, Runnable onLogout) {
        this.client = client;
        this.session = session;
        this.onLogout = onLogout;

        setSpacing(12);
        setPadding(new Insets(40));
        setAlignment(Pos.TOP_CENTER);
        setStyle("-fx-background-color: #17212b;");

        buildUI();
    }

    private void buildUI() {
        getChildren().clear();
        var user = session.getUser();
        if (user == null) {
            getChildren().add(new Label("Not logged in") {{
                setStyle("-fx-text-fill: #8e9297;");
            }});
            return;
        }

        var avatar = new Label(user.getDisplayName().substring(0, 1).toUpperCase());
        avatar.setMinSize(72, 72);
        avatar.setMaxSize(72, 72);
        avatar.setStyle("-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-font-size: 32px; -fx-font-weight: bold; -fx-background-radius: 36; -fx-alignment: center; -fx-cursor: hand;");
        avatar.setAlignment(Pos.CENTER);

        var usernameLabel = new Label("@" + user.getUsername());
        usernameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #8e9297;");

        nameField = new TextField(user.getDisplayName());
        nameField.setPromptText("Display name");
        nameField.setStyle("-fx-background-color: #242f3d; -fx-text-fill: #fff; -fx-prompt-text-fill: #8e9297; -fx-padding: 10 14; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 14px; -fx-max-width: 300;");

        bioArea = new TextArea(user.getBio() != null ? user.getBio() : "");
        bioArea.setPromptText("Bio");
        bioArea.setPrefRowCount(3);
        bioArea.setWrapText(true);
        bioArea.setStyle("-fx-background-color: #242f3d; -fx-text-fill: #fff; -fx-prompt-text-fill: #8e9297; -fx-padding: 8; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 13px; -fx-max-width: 300; -fx-control-inner-background: #242f3d;");

        saveBtn = new Button("Save");
        saveBtn.setStyle("-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-padding: 8 24; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14px; -fx-font-weight: bold;");
        saveBtn.setOnAction(e -> saveProfile());

        var emailLabel = new Label(user.getEmail() != null ? user.getEmail() : "");
        emailLabel.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 12px;");

        var premiumLabel = new Label(user.isPremium() ? "Premium" : "Free");
        premiumLabel.setStyle("-fx-text-fill: " + (user.isPremium() ? "#4ade80" : "#8e9297") + "; -fx-font-size: 12px;");

        var logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color: #f87171; -fx-text-fill: #fff; -fx-padding: 10 32; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14px; -fx-font-weight: bold;");
        logoutBtn.setOnAction(e -> {
            session.logout();
            onLogout.run();
        });

        getChildren().addAll(avatar, usernameLabel, nameField, bioArea, new HBox(8, saveBtn) {{
            setAlignment(Pos.CENTER);
        }}, emailLabel, premiumLabel, logoutBtn);
    }

    private void saveProfile() {
        saveBtn.setDisable(true);
        saveBtn.setText("Saving...");
        new Thread(() -> {
            try {
                var api = new ProfileApi(client);
                api.updateProfile(nameField.getText().trim(), bioArea.getText().trim(), null);
                var newProfile = api.getProfile();
                session.setUser(newProfile);
                Platform.runLater(() -> {
                    saveBtn.setDisable(false);
                    saveBtn.setText("Saved!");
                    saveBtn.setStyle("-fx-background-color: #4ade80; -fx-text-fill: #fff; -fx-padding: 8 24; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14px; -fx-font-weight: bold;");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    saveBtn.setDisable(false);
                    saveBtn.setText("Error: " + e.getMessage());
                });
            }
        }).start();
    }
}
