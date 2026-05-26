package com.auction.server;

import com.auction.common.constant.AuctionConstants;
import com.auction.server.config.ServerConfig;
import com.auction.server.network.ServerSocketManager;
import com.auction.server.pool.DatabaseConnectionPool;
import com.auction.server.scheduler.AuctionScheduler;
import com.auction.server.scheduler.TimeBroadcastScheduler;
import com.auction.server.service.SessionCleanupService;

import java.util.concurrent.atomic.AtomicBoolean;

public class AuctionServer {

    private static volatile boolean isRunning = false;
    private static ServerSocketManager serverSocketManager;
    private static AuctionScheduler auctionScheduler;
    private static TimeBroadcastScheduler timeScheduler;
    private static DatabaseConnectionPool dbPool;
    private static Thread serverThread;
    private static final AtomicBoolean serverStarted = new AtomicBoolean(false);

    // 🔥 THÊM: Session cleanup service
    private static SessionCleanupService sessionCleanupService;

    public static void start() {
        if (isRunning) {
            System.out.println("⚠️ Server already running");
            return;
        }

        serverThread = new Thread(() -> {
            try {
                System.out.println("🚀 Starting Auction Server...");
                System.out.println("========================================");

                System.out.println("\n[1/6] Initializing Database...");
                dbPool = DatabaseConnectionPool.getInstance();
                if (!dbPool.testConnection()) {
                    System.err.println("❌ Database connection failed! Server cannot start.");
                    return;
                }
                System.out.println("✅ Database connected successfully");

                System.out.println("\n[2/6] Initializing sample data...");
                DataInitializer.initData();

                System.out.println("\n[3/6] Starting schedulers...");
                auctionScheduler = new AuctionScheduler();
                timeScheduler = new TimeBroadcastScheduler();
                System.out.println("✅ Schedulers started");

                System.out.println("\n[4/6] Starting session cleanup service...");
                sessionCleanupService = new SessionCleanupService();
                sessionCleanupService.start();
                System.out.println("✅ Session cleanup service started");

                System.out.println("\n[5/6] Starting server socket...");
                int port = ServerConfig.getInstance().getPort();
                serverSocketManager = new ServerSocketManager(port);

                System.out.println("\n[6/6] Registering shutdown hook...");

                isRunning = true;
                serverStarted.set(true);
                System.out.println("\n========================================");
                System.out.println("✅ Auction Server started successfully!");
                System.out.println("   Port: " + port);
                System.out.println("   Heartbeat: " + AuctionConstants.HEARTBEAT_INTERVAL_SECONDS + "s");
                System.out.println("   Session timeout: " + AuctionConstants.SESSION_TIMEOUT_MINUTES + " minutes");
                System.out.println("========================================\n");

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

        if (sessionCleanupService != null) {
            sessionCleanupService.stop();
        }
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
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stop();
        }
    }
}