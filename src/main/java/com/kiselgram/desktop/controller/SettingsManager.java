package com.kiselgram.desktop.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.kiselgram.desktop.api.ApiClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SettingsManager {
    private static final Path SETTINGS_FILE = Paths.get(System.getProperty("user.home"), ".kiselgram_settings");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, Object> settings;
    private ApiClient client;
    private Runnable onThemeChange;
    private Runnable onFontChange;
    private Runnable onColorChange;

    public SettingsManager() {
        settings = loadFromDisk();
    }

    public void setClient(ApiClient client) { this.client = client; }
    public void setOnThemeChange(Runnable r) { this.onThemeChange = r; }
    public void setOnFontChange(Runnable r) { this.onFontChange = r; }
    public void setOnColorChange(Runnable r) { this.onColorChange = r; }

    public boolean isDark() { return !"light".equals(get("theme", "dark")); }
    public String getTheme() { return (String) get("theme", "dark"); }
    public void setTheme(String theme) {
        settings.put("theme", theme);
        save();
        if (onThemeChange != null) onThemeChange.run();
        syncToServer();
    }

    public String getFontSize() { return (String) get("font_size", "medium"); }
    public int getFontSizePx() {
        var s = getFontSize();
        return switch (s) {
            case "small" -> 12;
            case "large" -> 16;
            default -> 14;
        };
    }

    public void setFontSize(String size) {
        settings.put("font_size", size);
        save();
        if (onFontChange != null) onFontChange.run();
        syncToServer();
    }

    public String getColorMy() { return (String) get("color_my", "#2b5278"); }
    public void setColorMy(String color) {
        settings.put("color_my", color);
        save();
        if (onColorChange != null) onColorChange.run();
        syncToServer();
    }

    public String getColorTheir() { return (String) get("color_their", "#182533"); }
    public void setColorTheir(String color) {
        settings.put("color_their", color);
        save();
        if (onColorChange != null) onColorChange.run();
        syncToServer();
    }

    public String getHeroUrl() { return (String) get("hero_url", ""); }
    public void setHeroUrl(String url) {
        settings.put("hero_url", url);
        save();
        syncToServer();
    }

    @SuppressWarnings("unchecked")
    private <T> T get(String key, T def) {
        return (T) settings.getOrDefault(key, def);
    }

    private Map<String, Object> loadFromDisk() {
        try {
            if (Files.exists(SETTINGS_FILE)) {
                var json = Files.readString(SETTINGS_FILE);
                var type = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> m = gson.fromJson(json, type);
                if (m != null) return m;
            }
        } catch (IOException ignored) {}
        return new HashMap<>();
    }

    private void save() {
        try {
            Files.writeString(SETTINGS_FILE, gson.toJson(settings));
        } catch (IOException ignored) {}
    }

    private void syncToServer() {
        if (client == null || !client.hasToken()) return;
        new Thread(() -> {
            try {
                var body = new HashMap<String, Object>();
                body.put("theme", getTheme());
                body.put("font_size", getFontSize());
                body.put("color_my", getColorMy());
                body.put("color_their", getColorTheir());
                body.put("hero_url", getHeroUrl());

                var wrapped = new HashMap<String, Object>();
                wrapped.put("settings", body);
                client.put("/k/settings", wrapped);
            } catch (Exception ignored) {}
        }).start();
    }

    public void loadFromServer() {
        if (client == null || !client.hasToken()) return;
        new Thread(() -> {
            try {
                var res = client.get("/k/settings");
                if (res.has("data")) {
                    var data = res.getAsJsonObject("data");
                    var serverSettings = data.getAsJsonObject("settings");
                    if (serverSettings != null) {
                        if (serverSettings.has("theme"))
                            settings.put("theme", serverSettings.get("theme").getAsString());
                        if (serverSettings.has("font_size"))
                            settings.put("font_size", serverSettings.get("font_size").getAsString());
                        if (serverSettings.has("color_my"))
                            settings.put("color_my", serverSettings.get("color_my").getAsString());
                        if (serverSettings.has("color_their"))
                            settings.put("color_their", serverSettings.get("color_their").getAsString());
                        if (serverSettings.has("hero_url"))
                            settings.put("hero_url", serverSettings.get("hero_url").getAsString());
                        save();
                        if (onThemeChange != null) onThemeChange.run();
                        if (onFontChange != null) onFontChange.run();
                        if (onColorChange != null) onColorChange.run();
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }
}
