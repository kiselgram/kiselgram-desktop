package com.kiselgram.desktop.ui;

import com.kiselgram.desktop.api.ApiClient;
import com.kiselgram.desktop.api.AuthApi;
import com.kiselgram.desktop.controller.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class RegisterView extends VBox {
    public RegisterView(ApiClient client, SessionManager session, Runnable onBack) {
        setSpacing(12);
        setPadding(new Insets(40));
        setMaxWidth(360);
        setAlignment(Pos.TOP_CENTER);

        var title = new Label("Create Account");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");

        var usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setStyle(fieldStyle());

        var emailField = new TextField();
        emailField.setPromptText("Email");
        emailField.setStyle(fieldStyle());

        var passField = new PasswordField();
        passField.setPromptText("Password");
        passField.setStyle(fieldStyle());

        var errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 13px;");
        errorLabel.setVisible(false);

        var registerBtn = new Button("Create Account");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setStyle(buttonStyle());
        registerBtn.setOnAction(e -> {
            var user = usernameField.getText().trim();
            var email = emailField.getText().trim();
            var pass = passField.getText();
            if (user.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                errorLabel.setText("Fill in all fields");
                errorLabel.setVisible(true);
                return;
            }
            registerBtn.setDisable(true);
            registerBtn.setText("Creating...");
            new Thread(() -> {
                try {
                    new AuthApi(client).register(user, email, pass);
                    javafx.application.Platform.runLater(() -> {
                        var alert = new Alert(Alert.AlertType.INFORMATION, "Account created! Check your email for verification.");
                        alert.showAndWait();
                        onBack.run();
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        errorLabel.setText(ex.getMessage());
                        errorLabel.setVisible(true);
                        registerBtn.setDisable(false);
                        registerBtn.setText("Create Account");
                    });
                }
            }).start();
        });

        var backLink = new Hyperlink("Already have an account? Sign in");
        backLink.setStyle("-fx-text-fill: #4A90D9;");
        backLink.setOnAction(e -> onBack.run());

        getChildren().addAll(title, usernameField, emailField, passField, errorLabel, registerBtn, backLink);
    }

    private String fieldStyle() {
        return "-fx-background-color: #2e2e3e; -fx-text-fill: #f1f5f9; -fx-prompt-text-fill: #6e6e7e; " +
                "-fx-padding: 10 14; -fx-border-color: #3e3e4e; -fx-border-radius: 8; -fx-background-radius: 8;";
    }

    private String buttonStyle() {
        return "-fx-background-color: #4A90D9; -fx-text-fill: #fff; -fx-font-size: 15px; " +
                "-fx-padding: 10; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;";
    }
}
