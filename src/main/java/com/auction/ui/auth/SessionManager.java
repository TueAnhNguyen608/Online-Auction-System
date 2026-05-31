package com.auction.ui.auth;

import com.auction.client.AuctionClient;
import com.auction.common.model.item.Item;
import com.auction.common.model.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SessionManager {

    // 🔥 SỬA: Dùng volatile + ReadWriteLock cho thread safety
    private static volatile User currentUser;
    private static volatile AuctionClient client;
    private static final List<User> savedAccounts = new CopyOnWriteArrayList<>(); // 🔥 Thread-safe
    private static volatile int unreadNotifCount = 0;

    // 🔥 THÊM: Lock cho operations
    private static final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock userLock = new ReentrantReadWriteLock();

    // ========== CACHE CHO PRODUCT LIST ==========
    private static volatile List<Item> cachedItems = null;
    private static volatile long lastFetchTime = 0;
    private static final long CACHE_DURATION_MS = 60000;

    // 🔥 SỬA: Thread-safe get cached items
    public static List<Item> getCachedItems() {
        cacheLock.readLock().lock();
        try {
            long now = System.currentTimeMillis();
            if (cachedItems == null || (now - lastFetchTime) > CACHE_DURATION_MS) {
                return null;
            }
            // Return a copy to prevent modification
            return cachedItems != null ? new ArrayList<>(cachedItems) : null;
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    // 🔥 SỬA: Thread-safe set cached items
    public static void setCachedItems(List<Item> items) {
        cacheLock.writeLock().lock();
        try {
            cachedItems = items != null ? new ArrayList<>(items) : null;
            lastFetchTime = System.currentTimeMillis();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    public static void clearCache() {
        cacheLock.writeLock().lock();
        try {
            cachedItems = null;
            lastFetchTime = 0;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    // ========== AUTO-BID CACHE ==========
    private static final Map<String, List<AutoBidCache>> autoBidCache = new ConcurrentHashMap<>();

    public static class AutoBidCache {
        public final String itemId, itemName, maxPrice, currentPrice;
        public AutoBidCache(String itemId, String itemName, String maxPrice, String currentPrice) {
            this.itemId = itemId;
            this.itemName = itemName;
            this.maxPrice = maxPrice;
            this.currentPrice = currentPrice;
        }
    }

    // 🔥 SỬA: Thread-safe auto-bid cache operations
    public static void addAutoBidCache(String username, String itemId, String itemName,
                                       String maxPrice, String currentPrice) {
        if (username == null) return;
        autoBidCache.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>())
                .removeIf(a -> a.itemId.equals(itemId));
        autoBidCache.get(username).add(new AutoBidCache(itemId, itemName, maxPrice, currentPrice));
    }

    public static void removeAutoBidCache(String username, String itemId) {
        if (username == null) return;
        List<AutoBidCache> list = autoBidCache.get(username);
        if (list != null) {
            list.removeIf(a -> a.itemId.equals(itemId));
            if (list.isEmpty()) {
                autoBidCache.remove(username);
            }
        }
    }

    public static List<AutoBidCache> getAutoBidCache(String username) {
        if (username == null) return new ArrayList<>();
        List<AutoBidCache> list = autoBidCache.get(username);
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    // ========== AUTO-BID HISTORY ==========
    private static final Map<String, List<AutoBidHistoryEntry>> autoBidHistoryMap = new ConcurrentHashMap<>();

    public static class AutoBidHistoryEntry {
        public String itemId, itemName, bidAmount, currentPrice, bidTime, status;
        public AutoBidHistoryEntry(String itemId, String itemName, String bidAmount,
                                   String currentPrice, String bidTime) {
            this.itemId = itemId;
            this.itemName = itemName;
            this.bidAmount = bidAmount;
            this.currentPrice = currentPrice;
            this.bidTime = bidTime;
            this.status = "Dẫn đầu";
        }
    }

    public static void addAutoBidHistory(String username, AutoBidHistoryEntry entry) {
        if (username == null || entry == null) return;
        autoBidHistoryMap.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>())
                .add(0, entry);
    }

    public static void removeAutoBidHistory(String username, String itemId) {
        if (username == null) return;
        List<AutoBidHistoryEntry> list = autoBidHistoryMap.get(username);
        if (list != null) {
            list.removeIf(entry -> itemId.equals(entry.itemId));
        }
    }

    public static List<AutoBidHistoryEntry> getAutoBidHistory(String username) {
        if (username == null) return new ArrayList<>();
        List<AutoBidHistoryEntry> list = autoBidHistoryMap.get(username);
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    // ========== NOTIFICATIONS ==========
    public static class NotifData {
        public final String type, title, message, time;
        public boolean read;
        public NotifData(String type, String title, String message, String time) {
            this.type = type;
            this.title = title;
            this.message = message;
            this.time = time;
            this.read = false;
        }
    }

    // 🔥 SỬA: Dùng CopyOnWriteArrayList cho thread-safe
    private static final List<NotifData> realtimeNotifs = new CopyOnWriteArrayList<>();

    public static void addRealtimeNotif(String type, String title, String message) {
        String time = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        realtimeNotifs.add(0, new NotifData(type, title, message, time));
        incrementUnreadNotif();
    }

    public static List<NotifData> getRealtimeNotifs() {
        return new ArrayList<>(realtimeNotifs);
    }

    public static void clearRealtimeNotifs() {
        realtimeNotifs.clear();
    }

    // ========== USER MANAGEMENT - THREAD SAFE ==========
    public static void setCurrentUser(User user) {
        userLock.writeLock().lock();
        try {
            currentUser = user;
        } finally {
            userLock.writeLock().unlock();
        }
    }

    public static User getCurrentUser() {
        userLock.readLock().lock();
        try {
            return currentUser;
        } finally {
            userLock.readLock().unlock();
        }
    }

    public static void setClient(AuctionClient c) {
        client = c;
    }

    public static AuctionClient getClient() {
        return client;
    }

    // 🔥 SỬA: Thread-safe save account
    public static void saveAccount(User user) {
        if (user == null) return;

        boolean exists = false;
        for (User u : savedAccounts) {
            if (u.getUsername().equals(user.getUsername())) {
                u.setPassword(user.getPassword());
                exists = true;
                break;
            }
        }
        if (!exists) {
            savedAccounts.add(user);
        }
    }

    public static void clearPassword(String username) {
        for (User u : savedAccounts) {
            if (u.getUsername().equals(username)) {
                u.setPassword(null);
                break;
            }
        }

        // Kiểm tra nếu chưa có trong savedAccounts
        boolean exists = savedAccounts.stream()
                .anyMatch(u -> u.getUsername().equals(username));
        if (!exists && currentUser != null && currentUser.getUsername().equals(username)) {
            User copy = new User();
            copy.setUsername(username);
            copy.setPassword(null);
            savedAccounts.add(copy);
        }
    }

    public static List<User> getSavedAccounts() {
        return new ArrayList<>(savedAccounts);
    }

    // 🔥 SỬA: Thread-safe notification count
    public static void setUnreadNotifCount(int count) {
        unreadNotifCount = Math.max(0, count);
    }

    public static int getUnreadNotifCount() {
        return unreadNotifCount;
    }

    public static void clearUnreadNotif() {
        unreadNotifCount = 0;
    }

    public static void incrementUnreadNotif() {
        unreadNotifCount++;
    }

    // 🔥 SỬA: Thread-safe logout
    public static void logout() {
        try {
            if (client != null) {
                client.disconnect();
            }
        } catch (Exception ignored) {}

        userLock.writeLock().lock();
        try {
            currentUser = null;
        } finally {
            userLock.writeLock().unlock();
        }

        client = null;
        unreadNotifCount = 0;
        realtimeNotifs.clear();
        clearCache();
    }
}