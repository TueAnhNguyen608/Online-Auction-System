package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import common.*;

public class AuctionServer {
    public static final int PORT = 9999;
    private static DatabaseManager dbManager;
    private static ServerSocket serverSocket;
    private static ExecutorService threadPool = Executors.newCachedThreadPool();
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Observer registry cho realtime
    private static ConcurrentHashMap<String, CopyOnWriteArrayList<ClientHandler>> itemSubscribers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try {
            dbManager = new DatabaseManager();
            serverSocket = new ServerSocket(PORT);
            System.out.println("[" + getTime() + "] 🚀 Server started on port " + PORT);

            // Scheduler 1: Kết thúc đấu giá mỗi 10 giây
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    dbManager.endExpiredAuctions();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }, 0, 10, TimeUnit.SECONDS);

            // Scheduler 2: Broadcast thời gian còn lại mỗi giây (cho realtime)
            scheduler.scheduleAtFixedRate(() -> {
                broadcastRemainingTime();
            }, 0, 1, TimeUnit.SECONDS);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[" + getTime() + "] 📱 New client connected");
                ClientHandler handler = new ClientHandler(clientSocket, dbManager);
                threadPool.execute(handler);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Đăng ký client theo dõi item
    public static void subscribe(String itemId, ClientHandler handler) {
        itemSubscribers.computeIfAbsent(itemId, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    // Hủy đăng ký
    public static void unsubscribe(String itemId, ClientHandler handler) {
        CopyOnWriteArrayList<ClientHandler> subscribers = itemSubscribers.get(itemId);
        if (subscribers != null) {
            subscribers.remove(handler);
            if (subscribers.isEmpty()) {
                itemSubscribers.remove(itemId);
            }
        }
    }

    // Broadcast tới tất cả client đang xem item
    public static void broadcastToItem(String itemId, PriceUpdateEvent event) {
        CopyOnWriteArrayList<ClientHandler> subscribers = itemSubscribers.get(itemId);
        if (subscribers != null) {
            for (ClientHandler handler : subscribers) {
                handler.pushRealtimeUpdate(event);
            }
        }
    }

    // Broadcast thời gian còn lại cho các item đang active
    private static void broadcastRemainingTime() {
        try {
            List<Item> activeItems = dbManager.getActiveItems();
            for (Item item : activeItems) {
                long remainingSeconds = item.getEndTime() != null ?
                        java.time.Duration.between(java.time.LocalDateTime.now(), item.getEndTime()).getSeconds() : 0;

                if (remainingSeconds >= 0) {
                    PriceUpdateEvent event = new PriceUpdateEvent();
                    event.setType("TIME_UPDATE");
                    event.setItemId(item.getId());
                    event.setRemainingSeconds(remainingSeconds);
                    broadcastToItem(item.getId(), event);
                }
            }
        } catch (SQLException e) {
            // Silent fail
        }
    }

    private static String getTime() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }
}