package com.kiselgram.desktop.ui;

import com.kiselgram.desktop.model.ChatItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.BiConsumer;

public class ChatCell extends ListCell<ChatItem> {
    private BiConsumer<ChatItem, String> onAction;

    public ChatCell() {}
    public ChatCell(BiConsumer<ChatItem, String> onAction) { this.onAction = onAction; }

    @Override
    protected void updateItem(ChatItem chat, boolean empty) {
        super.updateItem(chat, empty);
        if (empty || chat == null) {
            setGraphic(null);
            setText(null);
            return;
        }

        var avatar = new Label(chat.getTitle().substring(0, 1).toUpperCase());
        avatar.setMinSize(40, 40);
        avatar.setMaxSize(40, 40);
        avatar.setStyle("-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-alignment: center;");
        avatar.setAlignment(Pos.CENTER);

        var title = new Label(chat.getTitle());
        title.setStyle("-fx-text-fill: #fff; -fx-font-size: 14px; -fx-font-weight: bold;");

        var lastMsg = chat.getLastMessage();
        var preview = new Label(lastMsg != null && lastMsg.getContent() != null ? lastMsg.getContent() : "");
        preview.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 12px;");
        preview.setMaxWidth(200);
        preview.setEllipsisString("...");

        var infoBox = new VBox(0, title, preview);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        var unread = chat.getUnreadCount();
        var rightBox = new VBox(4);
        rightBox.setAlignment(Pos.TOP_RIGHT);

        if (unread > 0) {
            var badge = new Label(String.valueOf(unread));
            badge.setStyle("-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 2 6; -fx-min-width: 20; -fx-alignment: center;");
            badge.setAlignment(Pos.CENTER);
            rightBox.getChildren().add(badge);
        }

        var row = new HBox(10, avatar, infoBox, rightBox);
        row.setPadding(new Insets(8, 10, 8, 10));
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        row.setStyle("-fx-background-color: #17212b; -fx-border-color: #1e2d3a; -fx-border-width: 0 0 1 0;");

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #1e2d3a; -fx-border-color: #1e2d3a; -fx-border-width: 0 0 1 0;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: #17212b; -fx-border-color: #1e2d3a; -fx-border-width: 0 0 1 0;"));

        if (onAction != null) {
            var pinItem = new MenuItem("Pin");
            pinItem.setOnAction(e -> onAction.accept(chat, "pin"));
            var unpinItem = new MenuItem("Unpin");
            unpinItem.setOnAction(e -> onAction.accept(chat, "unpin"));
            var delItem = new MenuItem("Delete chat");
            delItem.setOnAction(e -> onAction.accept(chat, "delete"));
            var menu = new ContextMenu(pinItem, unpinItem, delItem);
            row.setOnContextMenuRequested(e -> menu.show(row, e.getScreenX(), e.getScreenY()));
        }

        setGraphic(row);
        setText(null);
        setStyle("-fx-background-color: transparent; -fx-padding: 0;");
    }
}
