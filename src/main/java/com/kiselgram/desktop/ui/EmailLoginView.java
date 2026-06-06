package com.kiselgram.desktop.ui;

import com.kiselgram.desktop.api.ApiClient;
import com.kiselgram.desktop.controller.SessionManager;
import com.kiselgram.desktop.model.User;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.Map;

public class EmailLoginView extends VBox {
    private final ApiClient client;
    private final SessionManager session;
    private final Runnable onLogin;
    private final Runnable onBack;

    private final Label titleLabel = new Label();
    private final Label stepLabel = new Label();
    private final Label errorLabel = new Label();
    private final TextField emailField = new TextField();
    private final TextField otpField = new TextField();
    private final PasswordField passField = new PasswordField();
    private final TextField usernameField = new TextField();
    private final Button actionBtn = new Button();
    private final Button backBtn = new Button("Back");

    private String currentEmail;
    private boolean userExists;

    public EmailLoginView(ApiClient client, SessionManager session, Runnable onLogin, Runnable onBack) {
        this.client = client;
        this.session = session;
        this.onLogin = onLogin;
        this.onBack = onBack;

        setSpacing(12);
        setPadding(new Insets(40));
        setMaxWidth(400);
        setAlignment(Pos.TOP_CENTER);
        setStyle("-fx-background-color: #17212b;");

        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #fff;");
        titleLabel.setText("Email Login");

        stepLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8e9297;");

        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 13px;");
        errorLabel.setVisible(false);

        var style = "-fx-background-color: #242f3d; -fx-text-fill: #fff; -fx-prompt-text-fill: #8e9297; -fx-padding: 10 14; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 14px;";
        emailField.setPromptText("Email address");
        emailField.setStyle(style);
        otpField.setPromptText("6-digit code");
        otpField.setStyle(style);
        passField.setPromptText("Password");
        passField.setStyle(style);
        usernameField.setPromptText("Choose username");
        usernameField.setStyle(style);

        actionBtn.setMaxWidth(Double.MAX_VALUE);
        actionBtn.setStyle("-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-font-size: 15px; -fx-padding: 10; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: bold;");
        actionBtn.setOnAction(e -> doStep());

        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6ab2f2; -fx-cursor: hand; -fx-border-color: #6ab2f2; -fx-border-radius: 6; -fx-padding: 8 24;");
        backBtn.setOnAction(e -> currentStep = 0);

        getChildren().addAll(titleLabel, stepLabel, emailField, otpField, passField, usernameField, errorLabel, actionBtn, backBtn);
        showStepEmail();
    }

    private int currentStep = 0;

    private void doStep() {
        switch (currentStep) {
            case 0 -> checkEmail();
            case 1 -> sendOtpOrRegister();
            case 2 -> verifyOtp();
            case 3 -> loginPassword();
            case 4 -> finishRegister();
        }
    }

    private void showStepEmail() {
        currentStep = 0;
        stepLabel.setText("Step 1: Enter your email");
        emailField.setVisible(true);
        otpField.setVisible(false);
        passField.setVisible(false);
        usernameField.setVisible(false);
        actionBtn.setText("Continue");
        errorLabel.setVisible(false);
        emailField.requestFocus();
    }

    private void checkEmail() {
        var email = emailField.getText().trim();
        if (email.isEmpty()) { showError("Enter your email"); return; }
        currentEmail = email;
        disable(true);
        new Thread(() -> {
            try {
                var res = client.post("/auth/check-email", Map.of("email", email));
                var data = res.getAsJsonObject("data");
                userExists = data.get("exists").getAsBoolean();
                Platform.runLater(() -> {
                    disable(false);
                    if (userExists) {
                        showStepOtp();
                    } else {
                        showStepRegisterCode();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> { showError(e.getMessage()); disable(false); });
            }
        }).start();
    }

    private void showStepOtp() {
        currentStep = 1;
        stepLabel.setText("Step 2: A code was sent to your Kiselgram messages");
        emailField.setVisible(false);
        otpField.setVisible(true);
        otpField.setPromptText("Enter the 6-digit code");
        otpField.setText("");
        passField.setVisible(false);
        usernameField.setVisible(false);
        actionBtn.setText("Verify");
        errorLabel.setVisible(false);
        otpField.requestFocus();
        sendOtp();
    }

    private void sendOtp() {
        disable(true);
        new Thread(() -> {
            try {
                client.post("/auth/send-otp", Map.of("email", currentEmail));
                Platform.runLater(() -> {
                    disable(false);
                    stepLabel.setText("Step 2: Code sent to your Kiselgram messages. Enter it below.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> { showError(e.getMessage()); disable(false); });
            }
        }).start();
    }

    private void showStepRegisterCode() {
        currentStep = 1;
        stepLabel.setText("Step 2: New user! Check your email for verification code");
        emailField.setVisible(false);
        otpField.setVisible(true);
        passField.setVisible(false);
        usernameField.setVisible(false);
        actionBtn.setText("Verify code");
        errorLabel.setVisible(false);
        otpField.requestFocus();
    }

    private void sendOtpOrRegister() {
        var otp = otpField.getText().trim();
        if (otp.isEmpty()) { showError("Enter the code"); return; }

        if (!userExists) {
            verifyRegisterCode(otp);
            return;
        }

        verifyOtp(otp);
    }

    private void verifyOtp() {
        verifyOtp(otpField.getText().trim());
    }

    private void verifyOtp(String code) {
        if (code.isEmpty()) { showError("Enter the code"); return; }
        disable(true);
        new Thread(() -> {
            try {
                client.post("/auth/verify-otp", Map.of("email", currentEmail, "otp", code));
                Platform.runLater(() -> {
                    disable(false);
                    showStepPassword();
                });
            } catch (Exception e) {
                Platform.runLater(() -> { showError(e.getMessage()); disable(false); });
            }
        }).start();
    }

    private void showStepPassword() {
        currentStep = 3;
        stepLabel.setText("Step 4: Enter your password");
        otpField.setVisible(false);
        passField.setVisible(true);
        actionBtn.setText("Sign In");
        errorLabel.setVisible(false);
        passField.requestFocus();
    }

    private void loginPassword() {
        var pass = passField.getText();
        if (pass.isEmpty()) { showError("Enter your password"); return; }
        disable(true);
        new Thread(() -> {
            try {
                var res = client.post("/auth/login-password", Map.of("email", currentEmail, "password", pass));
                var data = res.getAsJsonObject("data");
                var token = data.get("session_token").getAsString();
                client.setToken(token);
                session.setToken(token);
                if (data.has("user") && !data.get("user").isJsonNull()) {
                    var u = new Gson().fromJson(data.get("user"), User.class);
                    session.setUser(u);
                }
                Platform.runLater(() -> {
                    disable(false);
                    onLogin.run();
                });
            } catch (Exception e) {
                Platform.runLater(() -> { showError(e.getMessage()); disable(false); });
            }
        }).start();
    }

    private void verifyRegisterCode(String code) {
        disable(true);
        new Thread(() -> {
            try {
                client.post("/auth/register-verify-code", Map.of("email", currentEmail, "code", code));
                Platform.runLater(() -> {
                    disable(false);
                    showStepFinishRegister();
                });
            } catch (Exception e) {
                Platform.runLater(() -> { showError(e.getMessage()); disable(false); });
            }
        }).start();
    }

    private void showStepFinishRegister() {
        currentStep = 4;
        stepLabel.setText("Step 4: Choose your username");
        otpField.setVisible(false);
        usernameField.setVisible(true);
        actionBtn.setText("Create Account");
        errorLabel.setVisible(false);
        usernameField.requestFocus();
    }

    private void finishRegister() {
        var username = usernameField.getText().trim();
        if (username.isEmpty()) { showError("Choose a username"); return; }
        disable(true);
        new Thread(() -> {
            try {
                var res = client.post("/auth/register-finish", Map.of(
                        "email", currentEmail, "username", username
                ));
                var data = res.getAsJsonObject("data");
                var token = data.get("session_token").getAsString();
                client.setToken(token);
                session.setToken(token);
                if (data.has("user") && !data.get("user").isJsonNull()) {
                    var u = new Gson().fromJson(data.get("user"), User.class);
                    session.setUser(u);
                }
                Platform.runLater(() -> {
                    disable(false);
                    onLogin.run();
                });
            } catch (Exception e) {
                Platform.runLater(() -> { showError(e.getMessage()); disable(false); });
            }
        }).start();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void disable(boolean d) {
        actionBtn.setDisable(d);
        actionBtn.setText(d ? "..." : actionBtn.getText());
    }
}
