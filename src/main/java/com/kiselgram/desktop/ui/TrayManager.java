package com.kiselgram.desktop.ui;

import javafx.application.Platform;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.TrayIcon.MessageType;

public class TrayManager {
    private TrayIcon trayIcon;
    private boolean supported;

    public TrayManager() {
        supported = SystemTray.isSupported();
        if (!supported) return;

        Platform.runLater(() -> {
            try {
                var tray = SystemTray.getSystemTray();
                var img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                var g = img.createGraphics();
                g.setColor(new Color(0x2b, 0x52, 0x78));
                g.fillRect(0, 0, 16, 16);
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 10));
                g.drawString("K", 4, 12);
                g.dispose();
                trayIcon = new TrayIcon(img, "Kiselgram");
                trayIcon.setImageAutoSize(true);
                tray.add(trayIcon);
            } catch (Exception ignored) {}
        });
    }

    public void notify(String title, String message) {
        if (!supported || trayIcon == null) return;
        Platform.runLater(() -> trayIcon.displayMessage(title, message, MessageType.INFO));
    }

    public void dispose() {
        if (!supported || trayIcon == null) return;
        Platform.runLater(() -> {
            try { SystemTray.getSystemTray().remove(trayIcon); } catch (Exception ignored) {}
        });
    }
}
