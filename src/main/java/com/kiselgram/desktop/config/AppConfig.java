package com.kiselgram.desktop.config;

public class AppConfig {
    public static final String BASE_URL = "https://web.kiselgram.ru";
    public static final String API_PREFIX = "/api.v2/api";

    public static String endpoint(String path) {
        return BASE_URL + API_PREFIX + path;
    }
}
