package view.common;

import client.AuctionClient;
import common.User;

import java.util.ArrayList;
import java.util.List;

public class SessionManager {
    private static User currentUser;
    private static AuctionClient client;
    private static final List<User> savedAccounts = new ArrayList<>();
    private static int unreadNotifCount = 0;

    // ĐÃ THÊM: cache auto-bid theo username
    private static final java.util.Map<String, java.util.List<AutoBidCache>> autoBidCache = new java.util.HashMap<>();

    public static class AutoBidCache {
        public final String itemId, itemName, maxPrice, currentPrice;
        public AutoBidCache(String itemId, String itemName, String maxPrice, String currentPrice) {
            this.itemId = itemId; this.itemName = itemName;
            this.maxPrice = maxPrice; this.currentPrice = currentPrice;
        }
    }

    public static void addAutoBidCache(String username, String itemId, String itemName, String maxPrice, String currentPrice) {
        autoBidCache.computeIfAbsent(username, k -> new java.util.ArrayList<>())
                .removeIf(a -> a.itemId.equals(itemId));
        autoBidCache.get(username).add(new AutoBidCache(itemId, itemName, maxPrice, currentPrice));
    }

    public static void removeAutoBidCache(String username, String itemId) {
        java.util.List<AutoBidCache> list = autoBidCache.get(username);
        if (list != null) list.removeIf(a -> a.itemId.equals(itemId));
    }

    public static java.util.List<AutoBidCache> getAutoBidCache(String username) {
        return autoBidCache.getOrDefault(username, new java.util.ArrayList<>());
    }

    // ĐÃ THÊM: danh sách thông báo realtime nhận được
    public static class NotifData {
        public final String type, title, message, time;
        public boolean read;
        public NotifData(String type, String title, String message, String time) {
            this.type = type; this.title = title;
            this.message = message; this.time = time;
            this.read = false;
        }
    }
    private static final List<NotifData> realtimeNotifs = new ArrayList<>();

    // ── Thêm thông báo realtime ──────────────────────────────────
    public static void addRealtimeNotif(String type, String title, String message) {
        String time = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        realtimeNotifs.add(0, new NotifData(type, title, message, time)); // thêm vào đầu
        incrementUnreadNotif();
    }

    public static List<NotifData> getRealtimeNotifs() { return realtimeNotifs; }

    public static void clearRealtimeNotifs() { realtimeNotifs.clear(); }

    // ── Existing methods ─────────────────────────────────────────
    public static void setCurrentUser(User user) { currentUser = user; }

    public static void saveAccount(User user) {
        if (user == null) return;
        boolean exists = false;
        for (User u : savedAccounts) {
            if (u.getUsername().equals(user.getUsername())) {
                u.setPassword(user.getPassword());
                exists = true; break;
            }
        }
        if (!exists) savedAccounts.add(user);
    }

    public static void clearPassword(String username) {
        savedAccounts.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .ifPresent(u -> u.setPassword(null));
        boolean exists = savedAccounts.stream()
                .anyMatch(u -> u.getUsername().equals(username));
        if (!exists && currentUser != null && currentUser.getUsername().equals(username)) {
            User copy = new User();
            copy.setUsername(username);
            copy.setPassword(null);
            savedAccounts.add(copy);
        }
    }

    public static User getCurrentUser() { return currentUser; }
    public static void setClient(AuctionClient c) { client = c; }
    public static AuctionClient getClient() { return client; }
    public static List<User> getSavedAccounts() { return savedAccounts; }

    public static void setUnreadNotifCount(int count) { unreadNotifCount = count; }
    public static int getUnreadNotifCount() { return unreadNotifCount; }
    public static void clearUnreadNotif() { unreadNotifCount = 0; }
    public static void incrementUnreadNotif() { unreadNotifCount++; }

    public static void logout() {
        try { if (client != null) client.disconnect(); } catch (Exception ignored) {}
        currentUser = null;
        client = null;
        unreadNotifCount = 0;
        realtimeNotifs.clear();
    }
}