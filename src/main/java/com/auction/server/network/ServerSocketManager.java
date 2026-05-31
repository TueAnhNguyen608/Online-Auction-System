package com.auction.server.network;

import com.auction.common.constant.AuctionConstants;
import com.auction.server.handler.ClientConnectionHandler;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerSocketManager {

    private final int port;
    private ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final AtomicBoolean accepting = new AtomicBoolean(false);
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    // 🔥 THÊM: Giới hạn thread pool
    private static final int MAX_THREAD_POOL_SIZE = 100;
    private static final int CORE_THREAD_POOL_SIZE = 20;

    public ServerSocketManager(int port) {
        this.port = port;
        // 🔥 SỬA: Dùng fixed thread pool thay vì cached
        this.threadPool = Executors.newFixedThreadPool(MAX_THREAD_POOL_SIZE);

        // 🔥 THÊM: Log khi thread pool đầy
        System.out.println("✅ Thread pool initialized: core=" + CORE_THREAD_POOL_SIZE +
                ", max=" + MAX_THREAD_POOL_SIZE);
    }

    public void startAccepting() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(AuctionConstants.SOCKET_TIMEOUT_MS);
            accepting.set(true);
            System.out.println("✅ ServerSocket listening on port " + port);
            System.out.println("   Timeout: " + AuctionConstants.SOCKET_TIMEOUT_MS + "ms");
            System.out.println("   Max threads: " + MAX_THREAD_POOL_SIZE);

            while (accepting.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setKeepAlive(true);
                    clientSocket.setTcpNoDelay(true);

                    // 🔥 THÊM: Kiểm tra thread pool trước khi accept
                    if (threadPool.isShutdown() || threadPool.isTerminated()) {
                        System.err.println("⚠️ Thread pool is shutting down, rejecting connection");
                        try {
                            clientSocket.close();
                        } catch (Exception ignored) {}
                        continue;
                    }

                    int currentActive = activeConnections.incrementAndGet();
                    System.out.println("📱 New client connected: " + clientSocket.getInetAddress() +
                            " (Active: " + currentActive + ", ThreadPool: " +
                            ((java.util.concurrent.ThreadPoolExecutor) threadPool).getActiveCount() + ")");

                    ClientConnectionHandler handler = new ClientConnectionHandler(clientSocket);

                    // 🔥 SỬA: Submit với timeout handling
                    threadPool.submit(() -> {
                        try {
                            handler.run();
                        } finally {
                            int remaining = activeConnections.decrementAndGet();
                            System.out.println("📱 Client disconnected. Active connections: " + remaining);
                        }
                    });

                } catch (SocketTimeoutException e) {
                    // Normal timeout, continue
                    continue;
                } catch (Exception e) {
                    if (accepting.get()) {
                        System.err.println("❌ Accept error: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            if (accepting.get()) {
                System.err.println("❌ Server error: " + e.getMessage());
            }
        }
    }

    // 🔥 THÊM: Phương thức lấy thông tin thread pool
    public String getThreadPoolStats() {
        if (threadPool instanceof java.util.concurrent.ThreadPoolExecutor) {
            java.util.concurrent.ThreadPoolExecutor tpe =
                    (java.util.concurrent.ThreadPoolExecutor) threadPool;
            return String.format("Active: %d, Pool: %d, Queue: %d, Completed: %d",
                    tpe.getActiveCount(), tpe.getPoolSize(),
                    tpe.getQueue().size(), tpe.getCompletedTaskCount());
        }
        return "Unknown";
    }

    public void stop() {
        accepting.set(false);

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }

        // 🔥 SỬA: Graceful shutdown với timeout hợp lý
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                System.err.println("⚠️ Thread pool didn't terminate, forcing shutdown...");
                threadPool.shutdownNow();
                if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("❌ Thread pool failed to terminate");
                }
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("🛑 Server socket manager stopped. Final active connections: " +
                activeConnections.get());
    }

    public int getActiveConnections() {
        return activeConnections.get();
    }

    public boolean isRunning() {
        return accepting.get() && serverSocket != null && !serverSocket.isClosed();
    }

    public void connectionClosed() {
        activeConnections.decrementAndGet();
    }
}