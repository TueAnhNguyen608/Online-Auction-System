package com.auction.server.broadcast;

import com.auction.common.model.communication.ResponseMessage;
import com.auction.common.model.event.PriceUpdateEvent;
import com.auction.common.util.JsonUtils;
import com.auction.server.handler.ClientConnectionHandler;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class EventBroadcaster {

    private static volatile EventBroadcaster instance;
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<ClientConnectionHandler>> subscribers;

    // 🔥 THÊM: Stats tracking
    private final AtomicInteger totalSubscriptions = new AtomicInteger(0);
    private final AtomicInteger totalBroadcasts = new AtomicInteger(0);

    private EventBroadcaster() {
        this.subscribers = new ConcurrentHashMap<>();
    }

    public static EventBroadcaster getInstance() {
        if (instance == null) {
            synchronized (EventBroadcaster.class) {
                if (instance == null) {
                    instance = new EventBroadcaster();
                }
            }
        }
        return instance;
    }

    // 🔥 SỬA: Subscribe với thread-safe check
    public void subscribe(String itemId, ClientConnectionHandler handler) {
        if (itemId == null || handler == null) return;

        // 🔥 THÊM: Kiểm tra handler còn sống
        if (!handler.isLoggedIn()) {
            System.out.println("⚠️ Cannot subscribe: handler not logged in");
            return;
        }

        subscribers.computeIfAbsent(itemId, k -> new CopyOnWriteArrayList<>())
                .addIfAbsent(handler);

        totalSubscriptions.incrementAndGet();
        System.out.println("📡 Client subscribed to item: " + itemId +
                " (Total subs: " + totalSubscriptions.get() + ")");
    }

    // 🔥 SỬA: Unsubscribe với thread-safe check
    public void unsubscribe(String itemId, ClientConnectionHandler handler) {
        if (itemId == null || handler == null) return;

        CopyOnWriteArrayList<ClientConnectionHandler> list = subscribers.get(itemId);
        if (list != null) {
            boolean removed = list.remove(handler);
            if (removed) {
                totalSubscriptions.decrementAndGet();
                System.out.println("📡 Client unsubscribed from item: " + itemId +
                        " (Remaining subs: " + totalSubscriptions.get() + ")");
            }
            if (list.isEmpty()) {
                subscribers.remove(itemId, list);
            }
        }
    }

    // 🔥 SỬA: Cleanup client với thread-safe
    public void cleanupClient(ClientConnectionHandler handler) {
        if (handler == null) return;

        int removedCount = 0;
        for (String itemId : subscribers.keySet()) {
            CopyOnWriteArrayList<ClientConnectionHandler> list = subscribers.get(itemId);
            if (list != null && list.remove(handler)) {
                removedCount++;
                if (list.isEmpty()) {
                    subscribers.remove(itemId, list);
                }
            }
        }
        if (removedCount > 0) {
            totalSubscriptions.addAndGet(-removedCount);
            System.out.println("📡 Cleaned up client from " + removedCount +
                    " subscriptions (Total subs: " + totalSubscriptions.get() + ")");
        }
    }

    // 🔥 SỬA: Broadcast với kiểm tra và async
    public void broadcastToItem(String itemId, PriceUpdateEvent event) {
        if (itemId == null || event == null) return;

        CopyOnWriteArrayList<ClientConnectionHandler> list = subscribers.get(itemId);
        if (list != null && !list.isEmpty()) {
            ResponseMessage response = ResponseMessage.success("realtime_update", event);
            String message = JsonUtils.toJson(response);

            // 🔥 THÊM: Filter handlers còn sống và broadcast async
            int sentCount = 0;
            for (ClientConnectionHandler handler : list) {
                if (handler != null && handler.isLoggedIn()) {
                    handler.pushRealtimeUpdate(message);
                    sentCount++;
                }
            }

            totalBroadcasts.incrementAndGet();
            System.out.println("📢 Broadcast to " + sentCount + "/" + list.size() +
                    " clients for item: " + itemId +
                    " (Total broadcasts: " + totalBroadcasts.get() + ")");
        }
    }

    // 🔥 SỬA: Broadcast to all với kiểm tra
    public void broadcastToAll(PriceUpdateEvent event) {
        if (event == null) return;

        ResponseMessage response = ResponseMessage.success("realtime_update", event);
        String message = JsonUtils.toJson(response);
        int totalSent = 0;

        for (CopyOnWriteArrayList<ClientConnectionHandler> list : subscribers.values()) {
            for (ClientConnectionHandler handler : list) {
                if (handler != null && handler.isLoggedIn()) {
                    handler.pushRealtimeUpdate(message);
                    totalSent++;
                }
            }
        }

        totalBroadcasts.addAndGet(totalSent > 0 ? 1 : 0);
        if (totalSent > 0) {
            System.out.println("📢 Broadcast to all " + totalSent + " clients");
        }
    }

    // 🔥 THÊM: Method lấy thống kê
    public String getStats() {
        return String.format("Items: %d, Subscriptions: %d, Total broadcasts: %d",
                subscribers.size(), totalSubscriptions.get(), totalBroadcasts.get());
    }

    public void start() {
        System.out.println("✅ EventBroadcaster started");
    }

    public void stop() {
        subscribers.clear();
        totalSubscriptions.set(0);
        totalBroadcasts.set(0);
        System.out.println("🛑 EventBroadcaster stopped");
    }

    public boolean isRunning() {
        return true;
    }
}