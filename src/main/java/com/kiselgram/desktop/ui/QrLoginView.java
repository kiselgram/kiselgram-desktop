package com.kiselgram.desktop.ui;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.kiselgram.desktop.api.ApiClient;
import com.kiselgram.desktop.api.AuthApi;
import com.kiselgram.desktop.controller.SessionManager;
import com.kiselgram.desktop.model.User;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class QrLoginView extends VBox {
    private final ImageView qrImage = new ImageView();
    private final Label statusLabel = new Label("Generating QR code...");
    private final Button backBtn = new Button("Back");
    private volatile boolean polling = true;

    public QrLoginView(ApiClient client, SessionManager session, Runnable onLogin, Runnable onBack) {
        setSpacing(16);
        setPadding(new Insets(40));
        setAlignment(Pos.TOP_CENTER);
        setMaxWidth(480);

        var title = new Label("QR Code Login");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");

        var subtitle = new Label("Scan with your phone camera");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #a0a0b0;");

        qrImage.setFitWidth(220);
        qrImage.setFitHeight(220);

        statusLabel.setStyle("-fx-text-fill: #a0a0b0; -fx-font-size: 13px;");

        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4A90D9; -fx-cursor: hand; -fx-border-color: #4A90D9; -fx-border-radius: 8; -fx-padding: 8 24;");
        backBtn.setOnAction(e -> { polling = false; onBack.run(); });

        getChildren().addAll(title, subtitle, qrImage, statusLabel, backBtn);

        startQrFlow(client, session, onLogin);
    }

    private void startQrFlow(ApiClient client, SessionManager session, Runnable onLogin) {
        new Thread(() -> {
            try {
                var auth = new AuthApi(client);
                var res = auth.requestQr();
                var data = res.getAsJsonObject("data");
                var qrToken = data.get("qr_token").getAsString();
                var qrUrl = data.get("qr_url").getAsString();

                var qrCode = generateQr(qrUrl, 300);
                Platform.runLater(() -> {
                    qrImage.setImage(qrCode);
                    statusLabel.setText("Scan this QR code with your phone");
                });

                long start = System.currentTimeMillis();
                while (polling && (System.currentTimeMillis() - start) < 120_000) {
                    Thread.sleep(2000);
                    var pollRes = auth.pollQr(qrToken);
                    var pollData = pollRes.getAsJsonObject("data");
                    var status = pollData.get("status").getAsString();

                    if ("claimed".equals(status)) {
                        var sessToken = pollData.get("session_token").getAsString();
                        client.setToken(sessToken);
                        session.setToken(sessToken);

                        if (pollData.has("user") && !pollData.get("user").isJsonNull()) {
                            var u = new Gson().fromJson(pollData.get("user"), User.class);
                            session.setUser(u);
                        }

                        Platform.runLater(() -> {
                            statusLabel.setText("Login confirmed!");
                            polling = false;
                            onLogin.run();
                        });
                        return;
                    } else if ("expired".equals(status)) {
                        Platform.runLater(() -> statusLabel.setText("QR code expired. Try again."));
                        return;
                    }
                }
                if (polling) {
                    Platform.runLater(() -> statusLabel.setText("Timed out. Try again."));
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    polling = false;
                });
            }
        }).start();
    }

    private Image generateQr(String text, int size) throws WriterException, java.io.IOException {
        var writer = new QRCodeWriter();
        var bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size);
        var bos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", bos);
        return new Image(new ByteArrayInputStream(bos.toByteArray()));
    }
}
