package com.auction.common.util;

import com.google.gson.*;
import com.auction.common.LocalDateTimeAdapter;
import java.lang.reflect.Type;
import java.time.LocalDateTime;

public final class JsonUtils {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private JsonUtils() {
        throw new UnsupportedOperationException("Cannot instantiate utility class");
    }

    public static String toJson(Object obj) {
        if (obj == null) {
            return "{}";
        }
        try {
            return gson.toJson(obj);
        } catch (Exception e) {
            System.err.println("❌ Failed to convert to JSON: " + e.getMessage());
            return "{\"error\":\"JSON serialization failed\"}";
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(json, clazz);
        } catch (JsonSyntaxException e) {
            System.err.println("❌ Failed to parse JSON: " + json);
            throw e;
        }
    }

    public static <T> T fromJson(String json, Type type) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            System.err.println("❌ Failed to parse JSON: " + json);
            throw e;
        }
    }

    public static Gson getGson() {
        return gson;
    }
}