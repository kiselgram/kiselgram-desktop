package com.kiselgram.desktop.controller;

import com.kiselgram.desktop.model.User;

import java.io.*;
import java.nio.file.*;

public class SessionManager {
    private static final Path TOKEN_FILE = Paths.get(System.getProperty("user.home"), ".kiselgram_token");
    private String token;
    private User user;

    public String getToken() { return token; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public void setToken(String token) {
        this.token = token;
        if (token != null) {
            try {
                Files.writeString(TOKEN_FILE, token);
            } catch (IOException ignored) {}
        } else {
            try { Files.deleteIfExists(TOKEN_FILE); } catch (IOException ignored) {}
        }
    }

    public String loadToken() {
        if (token != null) return token;
        try {
            if (Files.exists(TOKEN_FILE)) {
                token = Files.readString(TOKEN_FILE).trim();
                return token;
            }
        } catch (IOException ignored) {}
        return null;
    }

    public boolean isLoggedIn() { return loadToken() != null; }

    public void logout() {
        setToken(null);
        user = null;
    }
}
