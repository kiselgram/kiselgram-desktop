package com.kiselgram.desktop.ui;

import com.kiselgram.desktop.api.ApiClient;
import com.kiselgram.desktop.api.AuthApi;
import com.kiselgram.desktop.controller.SessionManager;
import com.kiselgram.desktop.model.User;
import com.google.gson.Gson;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class LoginView extends VBox {
    private final Runnable onLogin;
    private final Runnable onShowRegister;
    private final Runnable onShowQr;
    private final Runnable onShowEmail;
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label errorLabel = new Label();
    private final Button loginBtn = new Button("Sign In");

    public LoginView(ApiClient client, SessionManager session, Runnable onLogin, Runnable onShowRegister, Runnable onShowQr, Runnable onShowEmail) {
        this.onLogin = onLogin;
        this.onShowRegister = onShowRegister;
        this.onShowQr = onShowQr;
        this.onShowEmail = onShowEmail;

        setSpacing(12);
        setPadding(new Insets(40));
        setMaxWidth(360);
        setAlignment(javafx.geometry.Pos.TOP_CENTER);

        var title = new Label("Kiselgram");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");

        var subtitle = new Label("Sign in to your account");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #a0a0b0;");

        usernameField.setPromptText("Username");
        usernameField.setStyle(labelStyle());
        passwordField.setPromptText("Password");
        passwordField.setStyle(labelStyle());

        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 13px;");
        errorLabel.setVisible(false);

        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle(buttonStyle());
        loginBtn.setOnAction(e -> doLogin(client, session));

        var qrBtn = new Button("QR Code");
        qrBtn.setMaxWidth(Double.MAX_VALUE);
        qrBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6ab2f2; -fx-border-color: #6ab2f2; -fx-border-radius: 6; -fx-padding: 10; -fx-cursor: hand; -fx-font-size: 14px;");
        qrBtn.setOnAction(e -> onShowQr.run());

        var emailBtn = new Button("Email Login");
        emailBtn.setMaxWidth(Double.MAX_VALUE);
        emailBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6ab2f2; -fx-border-color: #6ab2f2; -fx-border-radius: 6; -fx-padding: 10; -fx-cursor: hand; -fx-font-size: 14px;");
        emailBtn.setOnAction(e -> onShowEmail.run());

        var registerLink = new Hyperlink("Don't have an account? Register");
        registerLink.setStyle("-fx-text-fill: #6ab2f2; -fx-font-size: 13px;");
        registerLink.setOnAction(e -> onShowRegister.run());

        getChildren().addAll(title, subtitle, usernameField, passwordField, errorLabel, loginBtn, qrBtn, emailBtn, registerLink);
    }

    private void doLogin(ApiClient client, SessionManager session) {
        var auth = new AuthApi(client);
        var user = usernameField.getText().trim();
        var pass = passwordField.getText();
        if (user.isEmpty() || pass.isEmpty()) {
            showError("Fill in all fields");
            return;
        }
        loginBtn.setDisable(true);
        loginBtn.setText("Signing in...");
        errorLabel.setVisible(false);

        new Thread(() -> {
            try {
                var data = auth.login(user, pass);
                var json = new Gson().toJson(data.get("user"));
                var u = new Gson().fromJson(json, User.class);
                session.setUser(u);
                javafx.application.Platform.runLater(onLogin);
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    showError(e.getMessage());
                    loginBtn.setDisable(false);
                    loginBtn.setText("Sign In");
                });
            }
        }).start();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private String labelStyle() {
        return "-fx-background-color: #2e2e3e; -fx-text-fill: #f1f5f9; -fx-prompt-text-fill: #6e6e7e; " +
                "-fx-padding: 10 14; -fx-border-color: #3e3e4e; -fx-border-radius: 8; -fx-background-radius: 8;";
    }

    private String buttonStyle() {
        return "-fx-background-color: #4A90D9; -fx-text-fill: #fff; -fx-font-size: 15px; " +
                "-fx-padding: 10; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;";
    }
}
