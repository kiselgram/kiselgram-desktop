package com.kiselgram.desktop.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class StoriesViewer extends Stage {
    private List<Map<String, Object>> stories;
    private int currentIndex = 0;

    @SuppressWarnings("unchecked")
    public StoriesViewer(String username, String avatarUrl, List<Map<String, Object>> stories) {
        this.stories = stories;
        setTitle(username + " — Stories");

        var root = new VBox(10);
        root.setStyle("-fx-background-color: #000; -fx-padding: 40;");
        root.setAlignment(Pos.CENTER);

        var avatar = new Label(username.substring(0, 1).toUpperCase());
        avatar.setMinSize(60, 60);
        avatar.setMaxSize(60, 60);
        avatar.setStyle("-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-font-size: 28px; -fx-font-weight: bold; -fx-background-radius: 30; -fx-alignment: center;");
        avatar.setAlignment(Pos.CENTER);

        var nameLabel = new Label(username);
        nameLabel.setStyle("-fx-text-fill: #fff; -fx-font-size: 18px; -fx-font-weight: bold;");

        var mediaView = new StackPane();
        mediaView.setMinSize(400, 400);
        mediaView.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 12;");

        var navRow = new HBox(20);
        navRow.setAlignment(Pos.CENTER);

        var prevBtn = new Button("<");
        prevBtn.setStyle(btnStyle());
        prevBtn.setOnAction(e -> { if (currentIndex > 0) { currentIndex--; showCurrent(mediaView); } });

        var nextBtn = new Button(">");
        nextBtn.setStyle(btnStyle());
        nextBtn.setOnAction(e -> { if (currentIndex < stories.size() - 1) { currentIndex++; showCurrent(mediaView); } });

        var closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-padding: 8 24; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: bold;");
        closeBtn.setOnAction(e -> close());

        navRow.getChildren().addAll(prevBtn, closeBtn, nextBtn);

        root.getChildren().addAll(avatar, nameLabel, mediaView, navRow);
        var scene = new Scene(root, 520, 600);
        setScene(scene);
        showCurrent(mediaView);
        show();
    }

    private void showCurrent(StackPane mediaView) {
        if (stories == null || stories.isEmpty() || currentIndex >= stories.size()) {
            mediaView.getChildren().setAll(new Label("No stories") {{
                setStyle("-fx-text-fill: #8e9297; -fx-font-size: 16px;");
            }});
            return;
        }
        var s = stories.get(currentIndex);
        var caption = (String) s.getOrDefault("caption", "");
        var mediaPath = (String) s.get("media_path");

        var content = new VBox(8);
        content.setAlignment(Pos.CENTER);

        if (mediaPath != null) {
            var imgView = new ImageView();
            imgView.setFitWidth(380);
            imgView.setPreserveRatio(true);
            var fullUrl = mediaPath.startsWith("http") ? mediaPath : "https://web.kiselgram.ru" + mediaPath;
            new Thread(() -> {
                try {
                    var img = new Image(fullUrl, true);
                    Platform.runLater(() -> imgView.setImage(img));
                } catch (Exception ignored) {}
            }).start();
            content.getChildren().add(imgView);
        }

        if (!caption.isEmpty()) {
            var capLabel = new Label(caption);
            capLabel.setStyle("-fx-text-fill: #fff; -fx-font-size: 14px; -fx-padding: 8;");
            capLabel.setWrapText(true);
            content.getChildren().add(capLabel);
        }

        var idxLabel = new Label((currentIndex + 1) + " / " + stories.size());
        idxLabel.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 12px;");
        content.getChildren().add(idxLabel);

        mediaView.getChildren().setAll(content);
    }

    private String btnStyle() {
        return "-fx-background-color: #242f3d; -fx-text-fill: #fff; -fx-padding: 8 16; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 16px; -fx-font-weight: bold;";
    }
}
