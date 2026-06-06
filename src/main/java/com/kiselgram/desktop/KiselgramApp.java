package com.kiselgram.desktop;

import com.kiselgram.desktop.api.ApiClient;
import com.kiselgram.desktop.controller.SessionManager;
import com.kiselgram.desktop.controller.SettingsManager;
import com.kiselgram.desktop.ui.EmailLoginView;
import com.kiselgram.desktop.ui.LoginView;
import com.kiselgram.desktop.ui.MainView;
import com.kiselgram.desktop.ui.QrLoginView;
import com.kiselgram.desktop.ui.TrayManager;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class KiselgramApp extends Application {
    private final ApiClient client = new ApiClient();
    private final SessionManager session = new SessionManager();
    private final SettingsManager settings = new SettingsManager();
    private final TrayManager tray = new TrayManager();
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Kiselgram v0.2 ALPHA");
        stage.setMinWidth(720);
        stage.setMinHeight(500);

        var root = new StackPane();
        root.setStyle("-fx-background-color: #17212b;");

        var scene = new Scene(root);
        stage.setScene(scene);

        var token = session.loadToken();
        if (token != null) {
            client.setToken(token);
            settings.setClient(client);
            settings.loadFromServer();
            showMain();
        } else {
            showLogin();
        }

        stage.show();

        stage.setOnCloseRequest(e -> {
            tray.dispose();
        });
    }

    private void showLogin() {
        var login = new LoginView(client, session, this::onLoginSuccess, this::showRegister, this::showQr, this::showEmailLogin);
        setRoot(login);
    }

    private void onLoginSuccess() {
        settings.setClient(client);
        settings.loadFromServer();
        showMain();
    }

    private void showRegister() {
        var register = new com.kiselgram.desktop.ui.RegisterView(client, session, this::showLogin);
        setRoot(register);
    }

    private void showQr() {
        var qr = new QrLoginView(client, session, this::onLoginSuccess, this::showLogin);
        setRoot(qr);
    }

    private void showEmailLogin() {
        var emailLogin = new EmailLoginView(client, session, this::onLoginSuccess, this::showLogin);
        setRoot(emailLogin);
    }

    private void showMain() {
        var main = new MainView(client, session, settings, tray, () -> {
            client.setToken(null);
            showLogin();
        });
        setRoot(main);
        main.onShown();
    }

    private void setRoot(javafx.scene.Node node) {
        var root = (StackPane) stage.getScene().getRoot();
        root.getChildren().setAll(node);
        StackPane.setAlignment(node, Pos.TOP_CENTER);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
