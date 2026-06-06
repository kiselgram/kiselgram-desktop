package com.kiselgram.desktop.model;

public class ChatItem {
    private String chat_type;
    private boolean is_saved;
    private User peer;
    private GroupInfo group;
    private ChannelInfo channel;
    private Message last_message;
    private int unread_count;

    public String getChatType() { return chat_type; }
    public boolean isSaved() { return is_saved; }
    public User getPeer() { return peer; }
    public void setPeer(User peer) { this.peer = peer; this.chat_type = "private"; }
    public GroupInfo getGroup() { return group; }
    public ChannelInfo getChannel() { return channel; }
    public Message getLastMessage() { return last_message; }
    public int getUnreadCount() { return unread_count; }

    public String getTitle() {
        if (is_saved) return "Saved Messages";
        if (peer != null) return peer.getDisplayName();
        if (group != null) return group.getName();
        if (channel != null) return channel.getName();
        return "Unknown";
    }

    public String getAvatarUrl() {
        if (peer != null) return peer.getAvatarUrl();
        if (group != null) return group.getAvatarUrl();
        return null;
    }

    public static class GroupInfo {
        private int group_id; private String name; private String avatar_url;
        private String description; private int member_count; private String my_role;
        public int getGroupId() { return group_id; }
        public String getName() { return name; }
        public String getAvatarUrl() { return avatar_url; }
        public String getDescription() { return description; }
        public int getMemberCount() { return member_count; }
        public String getMyRole() { return my_role; }
    }

    public static class ChannelInfo {
        private int channel_id; private String name; private String avatar_url;
        private String description; private int subscriber_count; private boolean is_subscribed;
        private Message last_message;
        public int getChannelId() { return channel_id; }
        public String getName() { return name; }
        public String getAvatarUrl() { return avatar_url; }
        public String getDescription() { return description; }
        public int getSubscriberCount() { return subscriber_count; }
        public boolean isSubscribed() { return is_subscribed; }
        public Message getLastMessage() { return last_message; }
        public void setLastMessage(Message m) { this.last_message = m; }
    }
}
