package com.kiselgram.desktop.ui;

import com.kiselgram.desktop.api.ApiClient;
import com.kiselgram.desktop.api.GroupApi;
import com.kiselgram.desktop.model.ChatItem;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class GroupsView extends VBox {
    private final GroupApi groupApi;
    private final ApiClient client;
    private final ListView<ChatItem.GroupInfo> listView = new ListView<>();
    private final Label loadingLabel = new Label("Loading...");

    public GroupsView(GroupApi groupApi, ApiClient client) {
        this.groupApi = groupApi;
        this.client = client;

        setPadding(new Insets(8));
        setSpacing(8);
        setStyle("-fx-background-color: #17212b;");

        var newGroupBtn = new Button("+ New Group");
        newGroupBtn.setStyle("-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-padding: 8 20; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14px; -fx-font-weight: bold;");
        newGroupBtn.setOnAction(e -> new CreateGroupDialog(client, groupApi, this::load).show());

        loadingLabel.setStyle("-fx-text-fill: #8e9297; -fx-padding: 20;");

        listView.setStyle("-fx-background-color: #17212b; -fx-control-inner-background: #17212b;");
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ChatItem.GroupInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    var avatar = new Label(item.getName().substring(0, 1).toUpperCase());
                    avatar.setMinSize(40, 40);
                    avatar.setMaxSize(40, 40);
                    avatar.setStyle("-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-alignment: center;");

                    var nameLabel = new Label(item.getName());
                    nameLabel.setStyle("-fx-text-fill: #fff; -fx-font-size: 15px; -fx-font-weight: bold;");
                    var infoLabel = new Label(item.getMemberCount() + " members");
                    infoLabel.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 12px;");
                    var textBox = new VBox(1, nameLabel, infoLabel);

                    var hbox = new HBox(10, avatar, textBox);
                    hbox.setPadding(new Insets(8, 12, 8, 12));
                    hbox.setStyle("-fx-background-color: #17212b; -fx-border-color: #1e2d3a; -fx-border-width: 0 0 1 0;");

                    setGraphic(hbox);
                    setText(null);
                    setStyle("-fx-padding: 0; -fx-background-color: #17212b;");
                }
            }
        });
        listView.setOnMouseClicked(e -> {
            var group = listView.getSelectionModel().getSelectedItem();
            if (group != null) {
                new GroupInfoView(client, groupApi, group, this::load).show();
            }
        });

        getChildren().addAll(newGroupBtn, loadingLabel);
        load();
    }

    private void load() {
        loadingLabel.setText("Loading...");
        new Thread(() -> {
            try {
                var groups = groupApi.getGroups();
                Platform.runLater(() -> {
                    getChildren().clear();
                    getChildren().addAll(new Button("+ New Group") {{
                        setStyle("-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-padding: 8 20; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14px; -fx-font-weight: bold;");
                        setOnAction(e -> new CreateGroupDialog(client, groupApi, GroupsView.this::load).show());
                    }});
                    if (groups.isEmpty()) {
                        getChildren().add(new Label("No groups yet") {{
                            setStyle("-fx-text-fill: #8e9297; -fx-padding: 20;");
                        }});
                    } else {
                        listView.getItems().setAll(groups);
                        getChildren().add(listView);
                    }
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
