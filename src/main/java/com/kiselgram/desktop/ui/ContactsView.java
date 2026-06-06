package com.kiselgram.desktop.ui;

import com.kiselgram.desktop.api.ContactApi;
import com.kiselgram.desktop.model.ChatItem;
import com.kiselgram.desktop.model.Contact;
import com.kiselgram.desktop.model.User;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class ContactsView extends VBox {
    private final ContactApi contactApi;
    private final ListView<Contact> listView = new ListView<>();
    private final Label loadingLabel = new Label("Loading...");
    private final Consumer<ChatItem> onOpenChat;

    public ContactsView(ContactApi contactApi, Consumer<ChatItem> onOpenChat) {
        this.contactApi = contactApi;
        this.onOpenChat = onOpenChat;
        setupUI();
        load();
    }

    private void setupUI() {
        setPadding(new Insets(0));
        setStyle("-fx-background-color: #17212b;");

        loadingLabel.setStyle("-fx-text-fill: #8e9297; -fx-padding: 24; -fx-font-size: 14px;");

        listView.setStyle("-fx-background-color: #17212b; -fx-control-inner-background: #17212b;");
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Contact item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    var avatar = new Label(item.getDisplay().substring(0, 1).toUpperCase());
                    avatar.setMinSize(40, 40);
                    avatar.setMaxSize(40, 40);
                    avatar.setStyle("-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 22; -fx-alignment: center;");
                    avatar.setAlignment(Pos.CENTER);

                    var nameLabel = new Label(item.getDisplay());
                    nameLabel.setStyle("-fx-text-fill: #fff; -fx-font-size: 15px; -fx-font-weight: bold;");
                    var statusText = item.isOnline() ? "Online" : "Offline";
                    var statusLabel = new Label(statusText);
                    statusLabel.setStyle("-fx-text-fill: " + (item.isOnline() ? "#4ade80" : "#8e9297") + "; -fx-font-size: 12px;");
                    var textBox = new VBox(2, nameLabel, statusLabel);

                    var hbox = new HBox(10, avatar, textBox);
                    hbox.setPadding(new Insets(8, 12, 8, 12));
                    hbox.setStyle("-fx-background-color: #17212b;");

                    setGraphic(hbox);
                    setText(null);
                    setStyle("-fx-padding: 0; -fx-background-color: #17212b; -fx-border-color: #1e2d3a; -fx-border-width: 0 0 1 0;");
                }
            }
        });

        if (onOpenChat != null) {
            listView.setOnMouseClicked(e -> {
                var contact = listView.getSelectionModel().getSelectedItem();
                if (contact != null) openChat(contact);
            });
        }

        getChildren().add(loadingLabel);
    }

    private void openChat(Contact contact) {
        var user = new User();
        user.setUserId(contact.getUserId());
        user.setUsername(contact.getUsername());
        user.setDisplayName(contact.getDisplayName());
        user.setAvatarUrl(contact.getAvatarUrl());
        user.setOnline(contact.isOnline());
        var chat = new ChatItem();
        chat.setPeer(user);
        if (onOpenChat != null) onOpenChat.accept(chat);
    }

    private void load() {
        new Thread(() -> {
            try {
                var contacts = contactApi.getContacts();
                Platform.runLater(() -> {
                    getChildren().clear();
                    listView.getItems().setAll(contacts);
                    getChildren().add(listView);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    getChildren().clear();
                    getChildren().add(new Label("Error: " + e.getMessage()) {{
                        setStyle("-fx-text-fill: #f87171; -fx-padding: 20;");
                    }});
                });
            }
        }).start();
    }
}
