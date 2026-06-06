package com.kiselgram.desktop.ui;

import com.kiselgram.desktop.controller.SettingsManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class SettingsView extends VBox {
    private final SettingsManager settings;

    public SettingsView(SettingsManager settings) {
        this.settings = settings;
        setSpacing(20);
        setPadding(new Insets(24));
        setStyle("-fx-background-color: #17212b;");

        var title = new Label("Settings");
        title.setStyle("-fx-text-fill: #fff; -fx-font-size: 20px; -fx-font-weight: bold;");

        getChildren().addAll(title, themeSection(), fontSection(), colorsSection(), heroSection());
    }

    private VBox themeSection() {
        var section = section("Appearance");

        var toggle = new ToggleButton(settings.isDark() ? "Dark" : "Light");
        toggle.setStyle(btnStyle(settings.isDark()));
        toggle.setPrefWidth(120);
        toggle.setOnAction(e -> {
            var isDark = !settings.isDark();
            settings.setTheme(isDark ? "dark" : "light");
            toggle.setText(isDark ? "Dark" : "Light");
            toggle.setStyle(btnStyle(isDark));
            applyTheme();
        });

        section.getChildren().add(toggle);
        return section;
    }

    private VBox fontSection() {
        var section = section("Font Size");

        var small = new ToggleButton("Sm");
        var medium = new ToggleButton("Md");
        var large = new ToggleButton("Lg");

        var current = settings.getFontSize();
        small.setStyle(btnStyle("small".equals(current)));
        medium.setStyle(btnStyle("medium".equals(current)));
        large.setStyle(btnStyle("large".equals(current)));

        small.setOnAction(e -> {
            settings.setFontSize("small");
            small.setStyle(btnStyle(true));
            medium.setStyle(btnStyle(false));
            large.setStyle(btnStyle(false));
        });
        medium.setOnAction(e -> {
            settings.setFontSize("medium");
            small.setStyle(btnStyle(false));
            medium.setStyle(btnStyle(true));
            large.setStyle(btnStyle(false));
        });
        large.setOnAction(e -> {
            settings.setFontSize("large");
            small.setStyle(btnStyle(false));
            medium.setStyle(btnStyle(false));
            large.setStyle(btnStyle(true));
        });

        var row = new HBox(8, small, medium, large);
        section.getChildren().add(row);
        return section;
    }

    private VBox colorsSection() {
        var section = section("Bubble Colors");

        var myLabel = new Label("My messages:");
        myLabel.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 13px;");
        var myPicker = new ColorPicker(Color.web(settings.getColorMy()));
        myPicker.setStyle("-fx-background-color: #242f3d;");
        myPicker.setOnAction(e -> {
            var hex = toHex(myPicker.getValue());
            settings.setColorMy(hex);
        });

        var theirLabel = new Label("Their messages:");
        theirLabel.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 13px;");
        var theirPicker = new ColorPicker(Color.web(settings.getColorTheir()));
        theirPicker.setStyle("-fx-background-color: #242f3d;");
        theirPicker.setOnAction(e -> {
            var hex = toHex(theirPicker.getValue());
            settings.setColorTheir(hex);
        });

        section.getChildren().addAll(myLabel, myPicker, theirLabel, theirPicker);
        return section;
    }

    private VBox heroSection() {
        var section = section("Background Image");

        var urlField = new TextField(settings.getHeroUrl());
        urlField.setPromptText("Image URL...");
        urlField.setStyle("-fx-background-color: #242f3d; -fx-text-fill: #fff; -fx-prompt-text-fill: #8e9297; -fx-padding: 8 12; -fx-border-radius: 6; -fx-background-radius: 6;");

        var applyBtn = new Button("Apply");
        applyBtn.setStyle("-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-padding: 8 20; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        applyBtn.setOnAction(e -> settings.setHeroUrl(urlField.getText().trim()));

        var resetBtn = new Button("Reset");
        resetBtn.setStyle("-fx-background-color: #3e3e4e; -fx-text-fill: #f1f5f9; -fx-padding: 8 20; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        resetBtn.setOnAction(e -> {
            urlField.setText("");
            settings.setHeroUrl("");
        });

        var row = new HBox(8, urlField, applyBtn, resetBtn);
        HBox.setHgrow(urlField, javafx.scene.layout.Priority.ALWAYS);
        section.getChildren().add(row);
        return section;
    }

    private VBox section(String title) {
        var sec = new VBox(10);
        sec.setStyle("-fx-background-color: #242f3d; -fx-background-radius: 8; -fx-padding: 16;");
        var label = new Label(title);
        label.setStyle("-fx-text-fill: #fff; -fx-font-size: 15px; -fx-font-weight: bold;");
        sec.getChildren().add(label);
        return sec;
    }

    private String btnStyle(boolean active) {
        if (active) return "-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-padding: 8 20; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: bold;";
        return "-fx-background-color: #242f3d; -fx-text-fill: #8e9297; -fx-padding: 8 20; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;";
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    private void applyTheme() {
        var bg = settings.isDark() ? "#17212b" : "#e9f0f5";
        var panel = settings.isDark() ? "#242f3d" : "#ffffff";
        var text = settings.isDark() ? "#fff" : "#1e2f3e";

        setStyle("-fx-background-color: " + bg + ";");
        for (var node : getChildren()) {
            if (node instanceof VBox s) {
                s.setStyle("-fx-background-color: " + panel + "; -fx-background-radius: 8; -fx-padding: 16;");
            }
        }
    }
}
