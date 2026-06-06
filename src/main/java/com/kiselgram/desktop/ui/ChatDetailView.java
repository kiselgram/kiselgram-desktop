package com.kiselgram.desktop.ui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kiselgram.desktop.api.ApiClient;
import com.kiselgram.desktop.api.ChatApi;
import com.kiselgram.desktop.api.ContactApi;
import com.kiselgram.desktop.api.GroupApi;
import com.kiselgram.desktop.controller.SessionManager;
import com.kiselgram.desktop.controller.SettingsManager;
import com.kiselgram.desktop.model.ChatItem;
import com.kiselgram.desktop.model.Message;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ChatDetailView extends BorderPane {
    private final ApiClient client;
    private final ChatApi chatApi;
    private final GroupApi groupApi;
    private final ContactApi contactApi;
    private final SessionManager session;
    private final SettingsManager settings;
    private final ChatItem chat;
    private final VBox messageArea = new VBox(4);
    private final ScrollPane scroll = new ScrollPane(messageArea);
    private final TextField inputField = new TextField();
    private final Button sendBtn = new Button("Send");
    private final Button attachBtn = new Button("+");
    private final Button micBtn = new Button("\uD83C\uDFA4");
    private final Button emojiBtn = new Button("\uD83D\uDE00");
    private final Label titleLabel = new Label();
    private final Label statusLabel = new Label();
    private final VBox replyBar = new VBox(2);
    private final Runnable onBack;
    private final HBox searchBar = new HBox(4);
    private Message replyTarget;
    private Message editTarget;
    private volatile boolean recording = false;
    private ByteArrayOutputStream recordBuffer;
    private AudioFormat recordFormat;
    private boolean searchVisible = false;
    private String searchQuery = "";
    private volatile boolean typingPolling = true;

    private static final String[] REACTION_EMOJIS = {"❤️", "👍", "😂", "😮", "😢"};
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMMM yyyy").withZone(ZoneId.systemDefault());

    public ChatDetailView(ApiClient client, ChatApi chatApi, GroupApi groupApi, ContactApi contactApi, SessionManager session, SettingsManager settings, ChatItem chat, Runnable onBack) {
        this.client = client;
        this.chatApi = chatApi;
        this.groupApi = groupApi;
        this.contactApi = contactApi;
        this.session = session;
        this.settings = settings;
        this.chat = chat;
        this.onBack = onBack;

        setStyle("-fx-background-color: #0e1621;");

        titleLabel.setText(chat.getTitle());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #fff; -fx-padding: 0;");

        var isSaved = chat.isSaved();
        var isChannel = "channel".equals(chat.getChatType()) && chat.getChannel() != null;

        var onlineText = "";
        if (isSaved) {
            onlineText = "Your notes";
        } else if (isChannel) {
            var subs = chat.getChannel().getSubscriberCount();
            onlineText = subs > 0 ? subs + " subscribers" : "Channel";
        } else if (chat.getPeer() != null) {
            onlineText = chat.getPeer().isOnline() ? "online" : "";
        } else if (chat.getGroup() != null) {
            onlineText = chat.getGroup().getMemberCount() + " members";
        }
        statusLabel.setText(onlineText);
        statusLabel.setStyle("-fx-text-fill: " + (onlineText.equals("online") ? "#4ade80" : "#8e9297") + "; -fx-font-size: 12px;");

        var backBtn = new Button("<");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6ab2f2; -fx-cursor: hand; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 0 8;");
        backBtn.setOnAction(e -> { typingPolling = false; if (onBack != null) onBack.run(); });

        var avatar = new Label(chat.getTitle().substring(0, 1).toUpperCase());
        avatar.setMinSize(36, 36);
        avatar.setMaxSize(36, 36);
        avatar.setStyle("-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 18; -fx-alignment: center;");
        avatar.setAlignment(Pos.CENTER);

        var titleBox = new VBox(0, titleLabel, statusLabel);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        var searchBtn = new Button("\uD83D\uDD0D");
        searchBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8e9297; -fx-cursor: hand; -fx-font-size: 16px; -fx-padding: 4 8;");
        searchBtn.setOnAction(e -> toggleSearch());

        var infoBtn = new Button("i");
        infoBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8e9297; -fx-cursor: hand; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 4 8; -fx-border-color: #8e9297; -fx-border-radius: 12; -fx-border-width: 1;");
        infoBtn.setVisible(chat.getGroup() != null);
        infoBtn.setOnAction(e -> {
            if (chat.getGroup() != null) {
                new GroupInfoView(client, groupApi, chat.getGroup(), () -> {}).show();
            }
        });

        var topBar = new HBox(8, backBtn, avatar, titleBox, searchBtn, infoBtn);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        topBar.setPadding(new Insets(6, 10, 6, 6));
        topBar.setStyle("-fx-background-color: #17212b; -fx-border-color: #1e2d3a; -fx-border-width: 0 0 1 0;");

        searchBar.setPadding(new Insets(4, 8, 4, 8));
        searchBar.setStyle("-fx-background-color: #242f3d; -fx-border-color: #1e2d3a; -fx-border-width: 0 0 1 0;");
        searchBar.setVisible(false);
        var searchField = new TextField();
        searchField.setPromptText("Search in chat...");
        searchField.setStyle("-fx-background-color: #17212b; -fx-text-fill: #fff; -fx-prompt-text-fill: #8e9297; -fx-padding: 6 10; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 13px;");
        searchField.setOnAction(e -> { searchQuery = searchField.getText().trim(); loadMessages(); });
        var closeSearch = new Button("✕");
        closeSearch.setStyle("-fx-background-color: transparent; -fx-text-fill: #f87171; -fx-cursor: hand; -fx-padding: 0 6; -fx-font-size: 13px;");
        closeSearch.setOnAction(e -> toggleSearch());
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchBar.getChildren().addAll(new Label("\uD83D\uDD0D") {{
            setStyle("-fx-text-fill: #8e9297; -fx-font-size: 13px;");
        }}, searchField, closeSearch);

        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #0e1621; -fx-background-color: #0e1621;");
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        messageArea.setPadding(new Insets(8, 40, 8, 40));
        messageArea.setStyle("-fx-background-color: #0e1621;");

        replyBar.setPadding(new Insets(6, 10, 4, 10));
        replyBar.setStyle("-fx-background-color: #17212b; -fx-border-color: #6ab2f2; -fx-border-width: 0 0 0 3;");
        replyBar.setVisible(false);

        inputField.setPromptText("Message...");
        inputField.setStyle("-fx-background-color: #242f3d; -fx-text-fill: #fff; -fx-prompt-text-fill: #8e9297; -fx-padding: 10 14; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 14px;");
        inputField.setOnAction(e -> sendMessage());

        sendBtn.setStyle("-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-padding: 8 20; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14px; -fx-font-weight: bold;");
        sendBtn.setOnAction(e -> sendMessage());

        attachBtn.setStyle("-fx-background-color: #242f3d; -fx-text-fill: #6ab2f2; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 4 14; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        attachBtn.setOnAction(e -> pickAndSendFile());

        micBtn.setStyle("-fx-background-color: #242f3d; -fx-text-fill: #f1f5f9; -fx-font-size: 16px; -fx-padding: 4 12; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        micBtn.setOnAction(e -> toggleRecording());

        emojiBtn.setStyle("-fx-background-color: #242f3d; -fx-text-fill: #f1f5f9; -fx-font-size: 16px; -fx-padding: 4 12; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        emojiBtn.setOnAction(e -> {
            var picker = new EmojiPicker(emoji -> {
                inputField.appendText(emoji);
                inputField.requestFocus();
            });
            picker.show(emojiBtn);
        });

        var inputRow = new HBox(6, attachBtn, emojiBtn, micBtn, inputField, sendBtn);
        inputRow.setPadding(new Insets(6, 8, 8, 8));
        inputRow.setStyle("-fx-background-color: #17212b; -fx-border-color: #1e2d3a; -fx-border-width: 1 0 0 0;");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        var inputArea = new VBox(0, replyBar, inputRow);

        var topSection = new VBox(0, topBar, searchBar);

        setTop(topSection);
        setCenter(scroll);
        setBottom(inputArea);

        loadMessages();
        startTypingPolling();
    }

    private void toggleSearch() {
        searchVisible = !searchVisible;
        searchBar.setVisible(searchVisible);
        if (!searchVisible) { searchQuery = ""; loadMessages(); }
    }

    private void markReadIfNeeded() {
        if (chat.getPeer() != null) {
            new Thread(() -> {
                try { chatApi.markRead(chat.getPeer().getUserId()); } catch (Exception ignored) {}
            }).start();
        }
    }

    private void startTypingPolling() {
        if (chat.getPeer() == null) return;
        var peerId = chat.getPeer().getUserId();
        new Thread(() -> {
            while (typingPolling) {
                try {
                    Thread.sleep(3000);
                    if (!typingPolling) break;
                    var typingUsers = chatApi.getTypingUsers("personal", peerId);
                    var currentUser = session.getUser();
                    Platform.runLater(() -> {
                        var filtered = typingUsers.stream()
                                .filter(id -> currentUser == null || id != currentUser.getUserId())
                                .toList();
                        if (!filtered.isEmpty()) {
                            statusLabel.setText("typing...");
                            statusLabel.setStyle("-fx-text-fill: #6ab2f2; -fx-font-size: 12px;");
                        } else {
                            var onlineText = chat.getPeer().isOnline() ? "online" : "";
                            statusLabel.setText(onlineText);
                            statusLabel.setStyle("-fx-text-fill: " + (onlineText.equals("online") ? "#4ade80" : "#8e9297") + "; -fx-font-size: 12px;");
                        }
                    });
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void loadMessages() {
        messageArea.getChildren().clear();
        messageArea.getChildren().add(new Label("Loading messages...") {{
            setStyle("-fx-text-fill: #8e9297; -fx-padding: 20;");
        }});
        markReadIfNeeded();

        new Thread(() -> {
            try {
                final List<Message> allMsgs;
                var isSaved = chat.isSaved();
                var isChannel = "channel".equals(chat.getChatType()) && chat.getChannel() != null;

                if (isSaved) {
                    var data = chatApi.getSavedMessages(0, 50);
                    var json = new Gson().toJson(data.get("messages"));
                    allMsgs = new Gson().fromJson(json, new TypeToken<List<Message>>(){}.getType());
                } else if (isChannel) {
                    var data = chatApi.getChannelMessages(chat.getChannel().getChannelId(), 0, 50);
                    var json = new Gson().toJson(data.get("messages"));
                    allMsgs = new Gson().fromJson(json, new TypeToken<List<Message>>(){}.getType());
                } else if ("group".equals(chat.getChatType()) && chat.getGroup() != null) {
                    var data = groupApi.getGroupMessages(chat.getGroup().getGroupId(), 0, 50);
                    var json = new Gson().toJson(data.get("messages"));
                    allMsgs = new Gson().fromJson(json, new TypeToken<List<Message>>(){}.getType());
                } else if (chat.getPeer() != null) {
                    var data = chatApi.getMessages(chat.getPeer().getUserId(), 0, 50);
                    var json = new Gson().toJson(data.get("messages"));
                    allMsgs = new Gson().fromJson(json, new TypeToken<List<Message>>(){}.getType());
                } else {
                    allMsgs = new ArrayList<>();
                }

                final List<Message> msgs;
                if (!searchQuery.isEmpty()) {
                    var q = searchQuery.toLowerCase();
                    msgs = allMsgs.stream()
                            .filter(m -> m.getContent() != null && m.getContent().toLowerCase().contains(q))
                            .collect(Collectors.toList());
                } else {
                    msgs = allMsgs;
                }

                Platform.runLater(() -> {
                    messageArea.getChildren().clear();
                    if (msgs.isEmpty()) {
                        messageArea.getChildren().add(new Label(searchQuery.isEmpty() ? "No messages" : "No results for \"" + searchQuery + "\"") {{
                            setStyle("-fx-text-fill: #8e9297; -fx-padding: 20;");
                        }});
                    } else {
                        String lastDate = null;
                        for (var msg : msgs) {
                            var dateKey = getDateKey(msg.getTimestamp());
                            if (dateKey != null && !dateKey.equals(lastDate)) {
                                messageArea.getChildren().add(createDateSeparator(msg.getTimestamp()));
                                lastDate = dateKey;
                            }
                            messageArea.getChildren().add(createBubble(msg));
                        }
                    }
                    scroll.setVvalue(1.0);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    messageArea.getChildren().clear();
                    messageArea.getChildren().add(new Label("Error: " + e.getMessage()) {{
                        setStyle("-fx-text-fill: #f87171;");
                    }});
                });
            }
        }).start();
    }

    private String getDateKey(String timestamp) {
        if (timestamp == null) return null;
        try {
            var instant = Instant.parse(timestamp);
            var localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
            return localDate.toString();
        } catch (Exception e) { return null; }
    }

    private Label createDateSeparator(String timestamp) {
        if (timestamp == null) return new Label();
        try {
            var instant = Instant.parse(timestamp);
            var localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
            var today = LocalDate.now(ZoneId.systemDefault());
            var yesterday = today.minusDays(1);

            String text;
            if (localDate.equals(today)) text = "Today";
            else if (localDate.equals(yesterday)) text = "Yesterday";
            else text = DATE_FMT.format(instant);

            var sep = new Label(text);
            sep.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 12 0 4 0; -fx-alignment: center;");
            sep.setMaxWidth(Double.MAX_VALUE);
            sep.setAlignment(Pos.CENTER);
            return sep;
        } catch (Exception e) { return new Label(); }
    }

    private Node createBubble(Message msg) {
        var currentUser = session.getUser();
        var isSelf = currentUser != null && msg.getSenderId() == currentUser.getUserId();

        var bubbleContent = new VBox(2);
        bubbleContent.setMaxWidth(440);

        if (msg.getContent() != null && !msg.getContent().isEmpty()) {
            var textLabel = new Label(msg.getContent());
            textLabel.setWrapText(true);
            var fontSize = settings.getFontSizePx();
            textLabel.setStyle("-fx-text-fill: #fff; -fx-padding: 8 12 2 12; -fx-font-size: " + fontSize + "px; -fx-line-spacing: 2;");
            bubbleContent.getChildren().add(textLabel);
        }

        if (msg.getFileUrl() != null && msg.getFileType() != null) {
            var fileIcon = getFileIcon(msg.getFileType());
            if (msg.getFileType().equals("image")) {
                var imgView = new ImageView();
                imgView.setFitWidth(300);
                imgView.setPreserveRatio(true);
                imgView.setStyle("-fx-cursor: hand; -fx-background-radius: 6;");
                loadImageAsync(msg.getFileUrl(), imgView);
                imgView.setOnMouseClicked(e -> showImagePreview(msg.getFileUrl()));
                var imgBox = new StackPane(imgView);
                imgBox.setPadding(new Insets(4, 0, 0, 0));
                bubbleContent.getChildren().add(imgBox);
            } else if (msg.getFileType().equals("audio")) {
                var playBtn = new Button("\u25B6 Play Voice (" + (msg.getFileName() != null ? msg.getFileName() : "audio") + ")");
                playBtn.setStyle("-fx-background-color: #2b5278; -fx-text-fill: #fff; -fx-font-size: 13px; -fx-padding: 8 16; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: bold;");
                playBtn.setOnAction(e -> playAudio(msg.getFileUrl()));
                var audioBox = new HBox(playBtn);
                audioBox.setPadding(new Insets(4, 8, 2, 8));
                bubbleContent.getChildren().add(audioBox);
            } else if (msg.getFileType().equals("video")) {
                var fileLabel = new Label("\uD83C\uDFA5 " + (msg.getFileName() != null ? msg.getFileName() : "Video"));
                fileLabel.setStyle("-fx-text-fill: #6ab2f2; -fx-padding: 8 12 2 12; -fx-font-size: 13px; -fx-cursor: hand;");
                bubbleContent.getChildren().add(fileLabel);
            } else {
                var fileLabel = new Label(fileIcon + " " + (msg.getFileName() != null ? msg.getFileName() : "File"));
                fileLabel.setStyle("-fx-text-fill: #6ab2f2; -fx-padding: 8 12 2 12; -fx-font-size: 13px; -fx-cursor: hand;");
                bubbleContent.getChildren().add(fileLabel);
            }
        }

        var infoRow = new HBox(4);
        infoRow.setPadding(new Insets(2, 10, 4, 10));
        infoRow.setAlignment(Pos.CENTER_RIGHT);

        if (msg.getEditedAt() != null) {
            var editedLabel = new Label("edited");
            editedLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.35); -fx-font-size: 10px; -fx-font-style: italic;");
            infoRow.getChildren().add(editedLabel);
        }

        if (msg.getTimestamp() != null) {
            var timeLabel = new Label(formatTime(msg.getTimestamp()));
            timeLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 11px;");
            infoRow.getChildren().add(timeLabel);
        }

        if (isSelf) {
            var readIcon = msg.isRead() ? "\u2713\u2713" : "\u2713";
            var readLabel = new Label(readIcon);
            readLabel.setStyle("-fx-text-fill: " + (msg.isRead() ? "#6ab2f2" : "rgba(255,255,255,0.4)") + "; -fx-font-size: 11px;");
            infoRow.getChildren().add(readLabel);
        }

        if (msg.getReactions() != null && !msg.getReactions().isEmpty()) {
            var reactText = new Label(formatReactions(msg.getReactions()));
            reactText.setStyle("-fx-text-fill: #ffd700; -fx-font-size: 12px;");
            infoRow.getChildren().add(reactText);
        }

        if (infoRow.getChildren().size() > 0) {
            bubbleContent.getChildren().add(infoRow);
        }

        var bubble = new VBox();
        var myColor = settings.getColorMy();
        var theirColor = settings.getColorTheir();
        var bg = isSelf ? myColor : theirColor;
        bubble.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 8; -fx-padding: 0;");
        bubble.getChildren().add(bubbleContent);

        var bubbleRow = new HBox(bubble);
        var insets = new Insets(2, 0, 2, 0);
        if (isSelf) {
            bubbleRow.setAlignment(Pos.CENTER_RIGHT);
            HBox.setMargin(bubble, new Insets(0, 0, 0, 60));
        } else {
            bubbleRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setMargin(bubble, new Insets(0, 60, 0, 0));
        }

        var row = new VBox(2);
        row.setPadding(insets);

        var reactionBar = new HBox(3);
        reactionBar.setPadding(new Insets(0, 0, 2, 0));
        reactionBar.setAlignment(isSelf ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        reactionBar.setVisible(false);

        for (var emoji : REACTION_EMOJIS) {
            var btn = new Button(emoji);
            btn.setStyle("-fx-background-color: #17212b; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 1 5; -fx-border-radius: 4; -fx-background-radius: 4;");
            var msgId = msg.getMessageId();
            btn.setOnAction(e -> {
                new Thread(() -> {
                    try {
                        chatApi.addReaction(msgId, emoji);
                        Platform.runLater(this::loadMessages);
                    } catch (Exception ignored) {}
                }).start();
            });
            reactionBar.getChildren().add(btn);
        }

        bubbleRow.setOnMouseEntered(e -> reactionBar.setVisible(true));
        bubbleRow.setOnMouseExited(e -> reactionBar.setVisible(false));

        var copyItem = new MenuItem("Copy");
        copyItem.setOnAction(e -> copyText(msg));
        var replyItem = new MenuItem("Reply");
        replyItem.setOnAction(e -> startReply(msg));
        var forwardItem = new MenuItem("Forward");
        forwardItem.setOnAction(e -> forwardMessage(msg));
        var saveItem = new MenuItem("Save");
        saveItem.setOnAction(e -> saveMessage(msg));

        if (isSelf) {
            var editItem = new MenuItem("Edit");
            editItem.setOnAction(e -> startEdit(msg));
            var deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(e -> deleteMessage(msg.getMessageId()));
            var menu = new ContextMenu(copyItem, editItem, deleteItem, forwardItem);
            bubbleRow.setOnContextMenuRequested(e -> menu.show(bubbleRow, e.getScreenX(), e.getScreenY()));
        } else {
            var blockItem = new MenuItem("Block");
            blockItem.setOnAction(e -> blockUser(msg));
            var menu = new ContextMenu(copyItem, replyItem, forwardItem, saveItem, blockItem);
            bubbleRow.setOnContextMenuRequested(e -> menu.show(bubbleRow, e.getScreenX(), e.getScreenY()));
        }

        row.getChildren().addAll(bubbleRow, reactionBar);
        return row;
    }

    private void blockUser(Message msg) {
        new Thread(() -> {
            try {
                contactApi.blockUser(msg.getSenderId());
                Platform.runLater(() -> {
                    var alert = new Alert(Alert.AlertType.INFORMATION, "User blocked");
                    alert.show();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    var alert = new Alert(Alert.AlertType.ERROR, "Block failed: " + e.getMessage());
                    alert.show();
                });
            }
        }).start();
    }

    private String getFileIcon(String type) {
        if (type == null) return "\uD83D\uDCCE";
        switch (type) {
            case "video": return "\uD83C\uDFA5";
            case "audio": return "\uD83C\uDFB5";
            case "image": return "\uD83D\uDDBC";
            case "pdf": return "\uD83D\uDCC4";
            case "zip": case "rar": case "7z": case "gz": return "\uD83D\uDCE6";
            case "doc": case "docx": return "\uD83D\uDCDD";
            case "xls": case "xlsx": return "\uD83D\uDCCA";
            default: return "\uD83D\uDCCE";
        }
    }

    private void forwardMessage(Message msg) {
        var stage = (Stage) getScene().getWindow();
        var dialog = new ForwardDialog(client, chatApi,
                msg.getContent() != null ? msg.getContent() : "Media",
                stage);
        dialog.show();
    }

    private void loadImageAsync(String url, ImageView imgView) {
        new Thread(() -> {
            try {
                var fullUrl = url.startsWith("http") ? url : "https://web.kiselgram.ru" + url;
                var image = new Image(fullUrl, true);
                Platform.runLater(() -> imgView.setImage(image));
            } catch (Exception ignored) {}
        }).start();
    }

    private void showImagePreview(String url) {
        var fullUrl = url.startsWith("http") ? url : "https://web.kiselgram.ru" + url;
        var stage = new javafx.stage.Stage();
        stage.setTitle("Image");
        var imgView = new ImageView();
        imgView.setPreserveRatio(true);
        imgView.setFitWidth(800);
        var sp = new ScrollPane(imgView);
        sp.setStyle("-fx-background: #000; -fx-background-color: #000;");
        var scene = new javafx.scene.Scene(sp, 900, 700);
        stage.setScene(scene);
        stage.show();
        new Thread(() -> {
            try {
                var image = new Image(fullUrl, true);
                Platform.runLater(() -> imgView.setImage(image));
            } catch (Exception ignored) {}
        }).start();
    }

    private void startReply(Message msg) {
        replyTarget = msg;
        editTarget = null;
        replyBar.getChildren().clear();
        var cancelBtn = new Button("✕");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f87171; -fx-cursor: hand; -fx-padding: 0 6; -fx-font-size: 14px;");

        var previewText = msg.getContent() != null ? msg.getContent() : "Media";
        if (previewText.length() > 40) previewText = previewText.substring(0, 40) + "...";
        var preview = new Label(previewText);
        preview.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 12px;");

        var topRow = new HBox(6, new Label("↩") {{
            setStyle("-fx-text-fill: #6ab2f2; -fx-font-size: 14px;");
        }}, preview, cancelBtn);
        topRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(preview, Priority.ALWAYS);

        replyBar.getChildren().add(topRow);
        replyBar.setVisible(true);
        cancelBtn.setOnAction(e -> cancelReply());
        inputField.requestFocus();
    }

    private void cancelReply() {
        replyTarget = null;
        replyBar.setVisible(false);
        replyBar.getChildren().clear();
    }

    private void startEdit(Message msg) {
        editTarget = msg;
        replyTarget = null;
        replyBar.getChildren().clear();
        var cancelBtn = new Button("✕");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f87171; -fx-cursor: hand; -fx-padding: 0 6; -fx-font-size: 14px;");

        var preview = new Label("Editing message");
        preview.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 12px;");

        var topRow = new HBox(6, new Label("✎") {{
            setStyle("-fx-text-fill: #4ade80; -fx-font-size: 14px;");
        }}, preview, cancelBtn);
        topRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(preview, Priority.ALWAYS);

        replyBar.getChildren().add(topRow);
        replyBar.setVisible(true);
        inputField.setText(msg.getContent() != null ? msg.getContent() : "");
        inputField.requestFocus();
        cancelBtn.setOnAction(e -> cancelEdit());
    }

    private void cancelEdit() {
        editTarget = null;
        inputField.clear();
        replyBar.setVisible(false);
        replyBar.getChildren().clear();
    }

    private void deleteMessage(int messageId) {
        new Thread(() -> {
            try {
                chatApi.deleteMessage(messageId);
                Platform.runLater(this::loadMessages);
            } catch (Exception ignored) {}
        }).start();
    }

    private void toggleRecording() {
        if (recording) stopRecording();
        else startRecording();
    }

    private void startRecording() {
        new Thread(() -> {
            try {
                recordFormat = new AudioFormat(16000, 16, 1, true, false);
                var info = new DataLine.Info(TargetDataLine.class, recordFormat);
                if (!AudioSystem.isLineSupported(info)) {
                    Platform.runLater(() -> showStatus("Mic not available"));
                    return;
                }
                var line = (TargetDataLine) AudioSystem.getLine(info);
                recordBuffer = new ByteArrayOutputStream();
                line.open(recordFormat);
                line.start();
                recording = true;
                Platform.runLater(() -> {
                    micBtn.setText("\uD83D\uDD34");
                    micBtn.setStyle("-fx-background-color: #f87171; -fx-text-fill: #fff; -fx-font-size: 16px; -fx-padding: 4 12; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
                });

                byte[] buf = new byte[4096];
                while (recording) {
                    int bytesRead = line.read(buf, 0, buf.length);
                    if (bytesRead > 0) recordBuffer.write(buf, 0, bytesRead);
                }
                line.stop();
                line.close();
            } catch (Exception e) {
                Platform.runLater(() -> showStatus("Record error: " + e.getMessage()));
                recording = false;
            }
        }).start();
    }

    private void stopRecording() {
        recording = false;
        micBtn.setText("\uD83C\uDFA4");
        micBtn.setStyle("-fx-background-color: #242f3d; -fx-text-fill: #f1f5f9; -fx-font-size: 16px; -fx-padding: 4 12; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");

        new Thread(() -> {
            try {
                byte[] audioData = recordBuffer.toByteArray();
                if (audioData.length < 1000) return;

                var tempFile = File.createTempFile("voice_", ".wav");
                try (var dos = new AudioInputStream(
                        new ByteArrayInputStream(audioData), recordFormat, audioData.length / recordFormat.getFrameSize())) {
                    AudioSystem.write(dos, AudioFileFormat.Type.WAVE, tempFile);
                }

                var fields = new HashMap<String, String>();
                if (chat.getPeer() != null) fields.put("receiver_id", String.valueOf(chat.getPeer().getUserId()));
                else if (chat.getGroup() != null) fields.put("group_id", String.valueOf(chat.getGroup().getGroupId()));

                client.uploadFile("/files/upload_file", tempFile.toPath(), fields);
                tempFile.delete();
                Platform.runLater(this::loadMessages);
            } catch (Exception e) {
                Platform.runLater(() -> showStatus("Upload failed: " + e.getMessage()));
            }
        }).start();
    }

    private void playAudio(String url) {
        new Thread(() -> {
            try {
                var fullUrl = url.startsWith("http") ? url : "https://web.kiselgram.ru" + url;
                var audioStream = new java.net.URL(fullUrl).openStream();
                var tmp = File.createTempFile("play_", ".wav");
                Files.copy(audioStream, tmp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                audioStream.close();

                var clip = AudioSystem.getClip();
                clip.open(AudioSystem.getAudioInputStream(tmp));
                clip.start();
                Platform.runLater(() -> showStatus("Playing..."));
                clip.drain();
                tmp.delete();
                Platform.runLater(() -> statusLabel.setText(""));
            } catch (Exception e) {
                Platform.runLater(() -> showStatus("Play error: " + e.getMessage()));
            }
        }).start();
    }

    private void showStatus(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 12px;");
    }

    private void copyText(Message msg) {
        if (msg.getContent() != null) {
            var clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            var content = new javafx.scene.input.ClipboardContent();
            content.putString(msg.getContent());
            clipboard.setContent(content);
        }
    }

    private void saveMessage(Message msg) {
        new Thread(() -> {
            try {
                chatApi.saveMessage(msg.getMessageId());
            } catch (Exception ignored) {}
        }).start();
    }

    private void pickAndSendFile() {
        var chooser = new FileChooser();
        chooser.setTitle("Send file");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );
        var file = chooser.showOpenDialog(getScene().getWindow());
        if (file == null) return;

        attachBtn.setDisable(true);
        attachBtn.setText("...");

        new Thread(() -> {
            try {
                var fields = new HashMap<String, String>();
                if (chat.getPeer() != null) {
                    fields.put("receiver_id", String.valueOf(chat.getPeer().getUserId()));
                } else if (chat.getGroup() != null) {
                    fields.put("group_id", String.valueOf(chat.getGroup().getGroupId()));
                }

                client.uploadFile("/files/upload_file", file.toPath(), fields);
                Platform.runLater(() -> {
                    attachBtn.setDisable(false);
                    attachBtn.setText("+");
                    loadMessages();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    attachBtn.setDisable(false);
                    attachBtn.setText("+");
                    var err = new Label("Upload failed: " + e.getMessage());
                    err.setStyle("-fx-text-fill: #f87171;");
                    messageArea.getChildren().add(err);
                });
            }
        }).start();
    }

    private void sendMessage() {
        var text = inputField.getText().trim();
        if (text.isEmpty() && editTarget == null) return;

        if (editTarget != null) {
            if (text.isEmpty()) { cancelEdit(); return; }
            var msgId = editTarget.getMessageId();
            editTarget = null;
            cancelEdit();
            new Thread(() -> {
                try {
                    chatApi.editMessage(msgId, text);
                    Platform.runLater(this::loadMessages);
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        var err = new Label("Edit failed: " + e.getMessage());
                        err.setStyle("-fx-text-fill: #f87171;");
                        messageArea.getChildren().add(err);
                    });
                }
            }).start();
            return;
        }

        inputField.clear();
        var replyingTo = replyTarget;
        if (replyTarget != null) cancelReply();

        new Thread(() -> {
            try {
                var isChannel = "channel".equals(chat.getChatType()) && chat.getChannel() != null;
                var isSaved = chat.isSaved();

                if (isChannel) {
                    client.post("/send_channel_message", Map.of(
                        "channel_id", chat.getChannel().getChannelId(), "content", text
                    ));
                } else if (isSaved) {
                    chatApi.sendMessage(session.getUser().getUserId(), text);
                } else if ("group".equals(chat.getChatType()) && chat.getGroup() != null) {
                    if (replyingTo != null) {
                        groupApi.sendGroupMessage(chat.getGroup().getGroupId(), text, replyingTo.getMessageId());
                    } else {
                        groupApi.sendGroupMessage(chat.getGroup().getGroupId(), text);
                    }
                } else if (chat.getPeer() != null) {
                    if (replyingTo != null) {
                        chatApi.sendMessage(chat.getPeer().getUserId(), text, replyingTo.getMessageId());
                    } else {
                        chatApi.sendMessage(chat.getPeer().getUserId(), text);
                    }
                    chatApi.sendTyping("personal", chat.getPeer().getUserId());
                }
                Platform.runLater(this::loadMessages);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    var err = new Label("Send failed: " + e.getMessage());
                    err.setStyle("-fx-text-fill: #f87171;");
                    messageArea.getChildren().add(err);
                });
            }
        }).start();
    }

    private String formatTime(String iso) {
        try {
            var instant = Instant.parse(iso);
            return TIME_FMT.format(instant);
        } catch (Exception e) {
            return "";
        }
    }

    private String formatReactions(Map<String, Integer> reactions) {
        var sb = new StringBuilder();
        for (var e : reactions.entrySet()) {
            sb.append(e.getKey()).append(" ").append(e.getValue()).append("  ");
        }
        return sb.toString().trim();
    }
}
