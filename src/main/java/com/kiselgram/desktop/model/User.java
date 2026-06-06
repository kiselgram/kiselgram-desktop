package com.kiselgram.desktop.model;

public class User {
    private int user_id;
    private String username;
    private String display_name;
    private String email;
    private String avatar_url;
    private String bio;
    private boolean is_online;
    private boolean is_premium;
    private boolean is_admin;
    private String last_seen;
    private String created_at;
    private String status_emoji;
    private boolean is_bot;
    private String bot_webapp_url;

    public int getUserId() { return user_id; }
    public void setUserId(int id) { this.user_id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return display_name != null ? display_name : username; }
    public void setDisplayName(String name) { this.display_name = name; }
    public String getEmail() { return email; }
    public String getAvatarUrl() { return avatar_url; }
    public void setAvatarUrl(String url) { this.avatar_url = url; }
    public String getBio() { return bio; }
    public boolean isOnline() { return is_online; }
    public void setOnline(boolean online) { this.is_online = online; }
    public boolean isPremium() { return is_premium; }
    public boolean isAdmin() { return is_admin; }
    public String getLastSeen() { return last_seen; }
    public String getStatusEmoji() { return status_emoji != null ? status_emoji : ""; }
}
