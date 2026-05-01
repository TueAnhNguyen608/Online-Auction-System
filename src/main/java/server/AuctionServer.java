package server;

import java.net.InetSocketAddress;

import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import common.*;

public class AuctionServer {

    public static final int PORT = 9999;

    private static DatabaseManager dbManager;
    private static ServerSocket serverSocket;
    private static ExecutorService threadPool;
    private static ScheduledExecutorService scheduler;

    // volatile đảm bảo visibility giữa các thread
    private static volatile boolean isRunning = false;

    private static Consumer<String> logCallback;

    private static ConcurrentHashMap<String, CopyOnWriteArrayList<ClientHandler>> itemSubscribers
            = new ConcurrentHashMap<>();

    // ── Entry point chạy trực tiếp (không qua UI) ────────────────────────────
    public static void main(String[] args) {
        startServer(null);
        // Giữ main thread sống cho đến khi server dừng
        while (isRunning) {
            try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
        }
    }

    // ── Khởi động server ─────────────────────────────────────────────────────
    /**
     * Khởi động server trong một background thread.
     * @param callback  Nhận log string để hiển thị lên UI (có thể null).
     */
    public static synchronized void startServer(Consumer<String> callback) {
        if (isRunning) {
            logToUI("⚠️ Server already running!");
            return;
        }

        logCallback = callback;

        // Khởi tạo lại thread pool mỗi lần start (quan trọng khi restart)
        threadPool = Executors.newCachedThreadPool();
        scheduler  = Executors.newScheduledThreadPool(2);

        new Thread(() -> {
            try {
                dbManager = new DatabaseManager();
                DataInitializer.initData(dbManager);  // ← THÊM DÒNG NÀY VÀO ĐÂY

                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(PORT));
                isRunning = true;

                logToUI("🚀 Server started on port " + PORT);

                // Kết thúc phiên đấu giá hết hạn mỗi 10s
                scheduler.scheduleAtFixedRate(() -> {
                    try { dbManager.endExpiredAuctions(); }
                    catch (SQLException e) { logToUI("❌ Error ending auctions: " + e.getMessage()); }
                }, 0, 10, TimeUnit.SECONDS);

                // Broadcast thời gian còn lại mỗi 1s
                scheduler.scheduleAtFixedRate(
                        AuctionServer::broadcastRemainingTime, 0, 1, TimeUnit.SECONDS);

                // Vòng lặp chấp nhận kết nối
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        logToUI("📱 New client: " + clientSocket.getInetAddress());
                        ClientHandler handler = new ClientHandler(clientSocket, dbManager);
                        threadPool.execute(handler);
                    } catch (Exception e) {
                        if (isRunning) {
                            logToUI("❌ Accept error: " + e.getMessage());
                        }
                    }
                }

            } catch (Exception e) {
                isRunning = false;
                logToUI("❌ Server startup failed: " + e.getMessage());
                e.printStackTrace();
            }
        }, "AuctionServer-Main").start();
    }
    // ── Dừng server ──────────────────────────────────────────────────────────
    /**
     * Dừng server: đóng socket, tắt scheduler và thread pool.
     * Blocking – chờ cleanup hoàn tất (tối đa 5s).
     */
    public static synchronized void stopServer() {
        if (!isRunning) {
            logToUI("⚠️ Server is not running!");
            return;
        }

        logToUI("🛑 Stopping server...");
        isRunning = false;

        // 1. Đóng ServerSocket để phá vỡ vòng lặp accept()
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            logToUI("❌ Error closing socket: " + e.getMessage());
        }

        // 2. Dừng scheduler (không nhận task mới, chờ task hiện tại kết thúc)
        if (scheduler != null) {
            scheduler.shutdown();
            try { scheduler.awaitTermination(3, TimeUnit.SECONDS); }
            catch (InterruptedException ignored) {}
        }

        // 3. Dừng thread pool
        if (threadPool != null) {
            threadPool.shutdown();
            try { threadPool.awaitTermination(3, TimeUnit.SECONDS); }
            catch (InterruptedException ignored) {}
        }

        logToUI("✅ Server stopped!");
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public static boolean isRunning() {
        return isRunning;
    }

    // ── Subscription / broadcast ──────────────────────────────────────────────
    public static void subscribe(String itemId, ClientHandler handler) {
        itemSubscribers.computeIfAbsent(itemId, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    public static void unsubscribe(String itemId, ClientHandler handler) {
        CopyOnWriteArrayList<ClientHandler> subs = itemSubscribers.get(itemId);
        if (subs != null) {
            subs.remove(handler);
            if (subs.isEmpty()) itemSubscribers.remove(itemId);
        }
    }

    public static void broadcastToItem(String itemId, PriceUpdateEvent event) {
        CopyOnWriteArrayList<ClientHandler> subs = itemSubscribers.get(itemId);
        if (subs != null) {
            for (ClientHandler handler : subs) {
                handler.pushRealtimeUpdate(event);
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private static void broadcastRemainingTime() {
        if (!isRunning || dbManager == null) return;
        try {
            List<Item> activeItems = dbManager.getActiveItems();
            for (Item item : activeItems) {
                long remaining = item.getEndTime() != null
                        ? java.time.Duration.between(java.time.LocalDateTime.now(), item.getEndTime()).getSeconds()
                        : 0;
                if (remaining >= 0) {
                    PriceUpdateEvent event = new PriceUpdateEvent();
                    event.setType("TIME_UPDATE");
                    event.setItemId(item.getId());
                    event.setRemainingSeconds(remaining);
                    broadcastToItem(item.getId(), event);
                }
            }
        } catch (SQLException e) {
            // Silent – tránh spam log
        }
    }

    private static void logToUI(String message) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String log  = "[" + time + "] " + message;
        System.out.println(log);
        if (logCallback != null) {
            logCallback.accept(message);   // truyền message thô, Controller tự thêm timestamp
        }
    }
}
