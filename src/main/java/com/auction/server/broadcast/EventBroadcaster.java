package com.auction.server.broadcast;

import com.auction.common.model.communication.ResponseMessage;
import com.auction.common.model.event.PriceUpdateEvent;
import com.auction.common.util.JsonUtils;
import com.auction.server.handler.ClientConnectionHandler;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBroadcaster {

    private static volatile EventBroadcaster instance;
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<ClientConnectionHandler>> subscribers;

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

    public void subscribe(String itemId, ClientConnectionHandler handler) {
        if (itemId == null || handler == null) return;
        subscribers.computeIfAbsent(itemId, k -> new CopyOnWriteArrayList<>())
                .add(handler);
        System.out.println("📡 Client subscribed to item: " + itemId);
    }

    public void unsubscribe(String itemId, ClientConnectionHandler handler) {
        if (itemId == null || handler == null) return;
        CopyOnWriteArrayList<ClientConnectionHandler> list = subscribers.get(itemId);
        if (list != null) {
            list.remove(handler);
            if (list.isEmpty()) {
                subscribers.remove(itemId);
            }
            System.out.println("📡 Client unsubscribed from item: " + itemId);
        }
    }

    // 🔥 THÊM METHOD NÀY: xóa client khỏi tất cả subscriptions khi disconnect
    public void cleanupClient(ClientConnectionHandler handler) {
        if (handler == null) return;
        int removedCount = 0;
        for (String itemId : subscribers.keySet()) {
            CopyOnWriteArrayList<ClientConnectionHandler> list = subscribers.get(itemId);
            if (list != null && list.remove(handler)) {
                removedCount++;
                if (list.isEmpty()) {
                    subscribers.remove(itemId);
                }
            }
        }
        if (removedCount > 0) {
            System.out.println("📡 Cleaned up client from " + removedCount + " subscriptions");
        }
    }

    public void broadcastToItem(String itemId, PriceUpdateEvent event) {
        if (itemId == null || event == null) return;
        CopyOnWriteArrayList<ClientConnectionHandler> list = subscribers.get(itemId);
        if (list != null && !list.isEmpty()) {
            ResponseMessage response = ResponseMessage.success("realtime_update", event);
            String message = JsonUtils.toJson(response);
            for (ClientConnectionHandler handler : list) {
                handler.pushRealtimeUpdate(message);
            }
            System.out.println("📢 Broadcast to " + list.size() + " clients for item: " + itemId);
        }
    }

    public void broadcastToAll(PriceUpdateEvent event) {
        if (event == null) return;
        ResponseMessage response = ResponseMessage.success("realtime_update", event);
        String message = JsonUtils.toJson(response);
        int totalSent = 0;
        for (CopyOnWriteArrayList<ClientConnectionHandler> list : subscribers.values()) {
            for (ClientConnectionHandler handler : list) {
                handler.pushRealtimeUpdate(message);
                totalSent++;
            }
        }
        if (totalSent > 0) {
            System.out.println("📢 Broadcast to all " + totalSent + " clients");
        }
    }

    public void start() {
        System.out.println("✅ EventBroadcaster started");
    }

    public void stop() {
        System.out.println("🛑 EventBroadcaster stopped");
    }

    public boolean isRunning() {
        return true;
    }
}