package com.auction.ui.utils;

import java.io.*;
import java.util.Properties;

public class ConfigLoader {

    private static final Properties props = new Properties();
    private static boolean loaded = false;

    static {
        load();
    }

    public static void load() {
        if (loaded) return;

        // Thử đọc từ file config
        try (InputStream input = ConfigLoader.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                props.load(input);
                System.out.println("✅ Loaded config from application.properties");
                loaded = true;
            }
        } catch (IOException e) {
            System.out.println("⚠️ No application.properties found, using defaults");
        }

        // Fallback to defaults
        if (!loaded) {
            setDefaults();
            loaded = true;
        }
    }

    private static void setDefaults() {
        props.setProperty("server.host", "localhost");
        props.setProperty("server.port", "9999");
        props.setProperty("db.url", "jdbc:mysql://localhost:3306/auction_db");
        props.setProperty("db.username", "root");
        props.setProperty("db.password", "Nhom7123456");
        props.setProperty("cache.duration.ms", "60000");
        props.setProperty("heartbeat.interval.seconds", "30");
        props.setProperty("session.timeout.minutes", "30");
        props.setProperty("rate.limit.per.minute", "60");
    }

    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getServerHost() {
        return get("server.host", "localhost");
    }

    public static int getServerPort() {
        return getInt("server.port", 9999);
    }

    public static String getDbUrl() {
        return get("db.url", "jdbc:mysql://localhost:3306/auction_db");
    }

    public static String getDbUsername() {
        return get("db.username", "root");
    }

    public static String getDbPassword() {
        return get("db.password", "Nhom7123456");
    }

    public static int getCacheDurationMs() {
        return getInt("cache.duration.ms", 60000);
    }

    public static int getHeartbeatIntervalSeconds() {
        return getInt("heartbeat.interval.seconds", 30);
    }

    public static int getSessionTimeoutMinutes() {
        return getInt("session.timeout.minutes", 30);
    }

    public static int getRateLimitPerMinute() {
        return getInt("rate.limit.per.minute", 60);
    }
}