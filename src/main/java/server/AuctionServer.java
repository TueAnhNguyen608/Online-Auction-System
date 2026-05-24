package server;

import common.constant.AuctionConstants;
import server.config.ServerConfig;
import server.network.ServerSocketManager;
import server.pool.DatabaseConnectionPool;
import server.scheduler.AuctionScheduler;
import server.scheduler.TimeBroadcastScheduler;

import java.util.concurrent.atomic.AtomicBoolean;

public class AuctionServer {

    private static volatile boolean isRunning = false;
    private static ServerSocketManager serverSocketManager;
    private static AuctionScheduler auctionScheduler;
    private static TimeBroadcastScheduler timeScheduler;
    private static DatabaseConnectionPool dbPool;
    private static Thread serverThread;
    private static final AtomicBoolean serverStarted = new AtomicBoolean(false);

    public static void start() {
        if (isRunning) {
            System.out.println("⚠️ Server already running");
            return;
        }

        serverThread = new Thread(() -> {
            try {
                System.out.println("🚀 Starting Auction Server...");
                System.out.println("========================================");

                // Step 1: Initialize Database
                System.out.println("\n[1/5] Initializing Database...");
                dbPool = DatabaseConnectionPool.getInstance();
                if (!dbPool.testConnection()) {
                    System.err.println("❌ Database connection failed! Server cannot start.");
                    return;
                }
                System.out.println("✅ Database connected successfully");

                // Step 2: Initialize Data
                System.out.println("\n[2/5] Initializing sample data...");
                DataInitializer.initData();

                // Step 3: Start Schedulers
                System.out.println("\n[3/5] Starting schedulers...");
                auctionScheduler = new AuctionScheduler();
                timeScheduler = new TimeBroadcastScheduler();
                System.out.println("✅ Schedulers started");

                // Step 4: Start Server Socket
                System.out.println("\n[4/5] Starting server socket...");
                int port = ServerConfig.getInstance().getPort();
                serverSocketManager = new ServerSocketManager(port);

                // Step 5: Register shutdown hook
                System.out.println("\n[5/5] Registering shutdown hook...");

                isRunning = true;
                serverStarted.set(true);
                System.out.println("\n========================================");
                System.out.println("✅ Auction Server started successfully!");
                System.out.println("   Port: " + port);
                System.out.println("========================================\n");

                // Start accepting connections (blocking)
                serverSocketManager.startAccepting();

            } catch (Exception e) {
                System.err.println("❌ Server startup failed: " + e.getMessage());
                e.printStackTrace();
                isRunning = false;
                serverStarted.set(false);
            }
        });
        serverThread.setDaemon(false);
        serverThread.start();

        // Đợi server khởi động
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
    }

    public static void stop() {
        if (!isRunning) {
            System.out.println("⚠️ Server is not running");
            return;
        }

        System.out.println("Stopping server components...");

        isRunning = false;
        serverStarted.set(false);

        if (serverSocketManager != null) {
            serverSocketManager.stop();
        }
        if (auctionScheduler != null) {
            auctionScheduler.shutdown();
        }
        if (timeScheduler != null) {
            timeScheduler.shutdown();
        }
        if (dbPool != null) {
            dbPool.closeAll();
        }

        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }

        System.out.println("✅ Server stopped");
    }

    public static boolean isRunning() {
        return isRunning;
    }

    public static boolean isServerStarted() {
        return serverStarted.get();
    }

    public static void main(String[] args) {
        start();
        // Keep main thread alive
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stop();
        }
    }
}