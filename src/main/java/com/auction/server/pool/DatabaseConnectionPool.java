package com.auction.server.pool;

import com.auction.common.constant.AuctionConstants;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DatabaseConnectionPool {

    private static volatile DatabaseConnectionPool instance;
    private final BlockingQueue<Connection> connectionPool;
    private final String url;
    private final String username;
    private final String password;
    private final int poolSize;
    private boolean isInitialized = false;

    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger totalConnections = new AtomicInteger(0);

    private DatabaseConnectionPool() {
        this.url = AuctionConstants.DB_URL;
        this.username = AuctionConstants.DB_USER;
        this.password = AuctionConstants.DB_PASSWORD;
        this.poolSize = AuctionConstants.DB_POOL_SIZE;
        System.out.println("✅ Using MySQL database");

        this.connectionPool = new LinkedBlockingQueue<>(poolSize);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✅ MySQL Driver loaded");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL Driver not found!");
        }

        initializePool();
        startLeakDetector();
    }

    public static DatabaseConnectionPool getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnectionPool.class) {
                if (instance == null) {
                    instance = new DatabaseConnectionPool();
                }
            }
        }
        return instance;
    }

    private void initializePool() {
        int successCount = 0;
        for (int i = 0; i < poolSize; i++) {
            try {
                Connection conn = DriverManager.getConnection(url, username, password);
                if (conn != null) {
                    conn.setAutoCommit(true);
                    connectionPool.offer(conn);
                    successCount++;
                    totalConnections.incrementAndGet();
                }
            } catch (SQLException e) {
                System.err.println("Failed to create connection " + (i + 1) + ": " + e.getMessage());
            }
        }

        if (successCount > 0) {
            isInitialized = true;
            System.out.println("✅ Database connection pool initialized with " + successCount + "/" + poolSize + " connections");
        } else {
            System.err.println("❌ Failed to initialize database connection pool!");
        }
    }

    private void startLeakDetector() {
        Thread leakDetector = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000);
                    int active = activeConnections.get();
                    if (active > poolSize) {
                        System.err.println("⚠️ WARNING: " + active + " active connections (pool size: " + poolSize + ")");
                    } else if (active > 0) {
                        System.out.println("📊 Active connections: " + active + "/" + poolSize);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        leakDetector.setDaemon(true);
        leakDetector.start();
    }

    public Connection getConnection() throws SQLException {
        if (!isInitialized) {
            throw new SQLException("Connection pool not initialized");
        }

        try {
            Connection conn = connectionPool.poll(5, TimeUnit.SECONDS);
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(url, username, password);
                totalConnections.incrementAndGet();
                System.err.println("⚠️ Pool exhausted! Created new connection (total: " + totalConnections.get() + ")");
            }
            conn.setAutoCommit(true);
            activeConnections.incrementAndGet();
            return conn;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Cannot get connection from pool", e);
        }
    }

    public void returnConnection(Connection conn) {
        if (conn == null) return;

        try {
            if (conn.isClosed()) {
                activeConnections.decrementAndGet();
                return;
            }

            // Rollback nếu đang trong transaction
            if (!conn.getAutoCommit()) {
                try {
                    conn.rollback();
                } catch (SQLException e) {
                    // Ignore
                }
                conn.setAutoCommit(true);
            }

            // Trả connection về pool
            if (connectionPool.size() < poolSize) {
                if (connectionPool.offer(conn)) {
                    activeConnections.decrementAndGet();
                } else {
                    conn.close();
                    activeConnections.decrementAndGet();
                    totalConnections.decrementAndGet();
                }
            } else {
                conn.close();
                activeConnections.decrementAndGet();
                totalConnections.decrementAndGet();
            }
        } catch (SQLException e) {
            System.err.println("Error returning connection: " + e.getMessage());
            activeConnections.decrementAndGet();
            try {
                conn.close();
                totalConnections.decrementAndGet();
            } catch (SQLException ex) {
                // Ignore
            }
        }
    }

    public void closeAll() {
        int closedCount = 0;
        for (Connection conn : connectionPool) {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                    closedCount++;
                }
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
        connectionPool.clear();
        isInitialized = false;
        System.out.println("✅ Closed " + closedCount + " database connections");
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            boolean valid = conn != null && !conn.isClosed() && conn.isValid(2);
            if (conn != null) returnConnection(conn);
            return valid;
        } catch (SQLException e) {
            System.err.println("❌ Database connection test failed: " + e.getMessage());
            return false;
        }
    }

    public int getActiveConnections() {
        return activeConnections.get();
    }
}