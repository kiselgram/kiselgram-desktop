package com.kiselgram.desktop.model;

public class Contact {
    private int user_id;
    private String username;
    private String display_name;
    private String avatar_url;
    private String custom_name;
    private boolean is_online;
    private String last_seen;
    private String status_emoji;

    public int getUserId() { return user_id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return display_name != null ? display_name : username; }
    public String getAvatarUrl() { return avatar_url; }
    public String getCustomName() { return custom_name; }
    public boolean isOnline() { return is_online; }
    public String getStatusEmoji() { return status_emoji != null ? status_emoji : ""; }
    public String getDisplay() { return custom_name != null ? custom_name : getDisplayName(); }
}
