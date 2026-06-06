package com.kiselgram.desktop.model;

import java.util.Map;

public class Message {
    private int message_id;
    private int sender_id;
    private Integer receiver_id;
    private String content;
    private String file_path;
    private String file_url;
    private String file_type;
    private String file_name;
    private boolean is_read;
    private String timestamp;
    private String edited_at;
    private Integer reply_to_id;
    private String sender_username;
    private String sender_avatar_url;
    private Map<String, Integer> reactions;

    public int getMessageId() { return message_id; }
    public int getSenderId() { return sender_id; }
    public Integer getReceiverId() { return receiver_id; }
    public String getContent() { return content; }
    public String getFilePath() { return file_path; }
    public String getFileUrl() { return file_url; }
    public String getFileType() { return file_type; }
    public String getFileName() { return file_name; }
    public boolean isRead() { return is_read; }
    public String getTimestamp() { return timestamp; }
    public String getEditedAt() { return edited_at; }
    public Integer getReplyToId() { return reply_to_id; }
    public String getSenderUsername() { return sender_username; }
    public String getSenderAvatarUrl() { return sender_avatar_url; }
    public Map<String, Integer> getReactions() { return reactions; }
}
