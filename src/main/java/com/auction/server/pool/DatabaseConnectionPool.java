package com.auction.server.pool;

import com.auction.common.constant.AuctionConstants;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

public class DatabaseConnectionPool {

    private static volatile DatabaseConnectionPool instance;
    private final BlockingQueue<Connection> connectionPool;
    private final String url;
    private final String username;
    private final String password;
    private final int poolSize;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger totalConnections = new AtomicInteger(0);

    private static final int MAX_RETRY_COUNT = 5;
    private static final int RETRY_DELAY_MS = 2000;
    // 🔥 TĂNG MAX TOTAL CONNECTIONS LÊN 500
    private static final int MAX_TOTAL_CONNECTIONS = 500;
    private static final int CONNECTION_TIMEOUT_MS = 10000;  // Tăng timeout

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
            throw new RuntimeException("MySQL Driver not found", e);
        }

        boolean initialized = initializePoolWithRetry();
        if (!initialized) {
            System.err.println("❌ Failed to initialize connection pool after " +
                    MAX_RETRY_COUNT + " attempts");
            throw new RuntimeException("Cannot initialize database connection pool");
        }

        startLeakDetector();
        startHealthChecker();
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

    private boolean initializePoolWithRetry() {
        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            System.out.println("📡 Connecting to database... Attempt " + attempt + "/" + MAX_RETRY_COUNT);

            int successCount = 0;
            for (int i = 0; i < poolSize; i++) {
                try {
                    Connection conn = createConnectionWithTimeout(CONNECTION_TIMEOUT_MS);
                    if (conn != null && isValidConnection(conn)) {
                        boolean offered = connectionPool.offer(conn);
                        if (offered) {
                            successCount++;
                            totalConnections.incrementAndGet();
                        } else {
                            try { conn.close(); } catch (SQLException ignored) {}
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Failed to create connection " + (i + 1) + ": " + e.getMessage());
                }
            }

            if (successCount >= Math.max(1, poolSize / 2)) {
                isInitialized.set(true);
                System.out.println("✅ Database connection pool initialized with " +
                        successCount + "/" + poolSize + " connections");
                return true;
            }

            System.err.println("⚠️ Attempt " + attempt + " failed. Only " +
                    successCount + "/" + poolSize + " connections created.");

            if (attempt < MAX_RETRY_COUNT) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                connectionPool.clear();
                totalConnections.set(0);
            }
        }
        return false;
    }

    private Connection createConnectionWithTimeout(int timeoutMs) throws SQLException {
        java.util.Properties props = new java.util.Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);
        props.setProperty("connectTimeout", String.valueOf(timeoutMs));
        props.setProperty("socketTimeout", String.valueOf(timeoutMs));
        return DriverManager.getConnection(url, props);
    }

    private boolean isValidConnection(Connection conn) {
        try {
            return conn != null && !conn.isClosed() && conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    private void startHealthChecker() {
        ScheduledExecutorService healthChecker = Executors.newSingleThreadScheduledExecutor();
        healthChecker.scheduleAtFixedRate(() -> {
            if (!isInitialized.get()) return;

            try {
                int currentSize = connectionPool.size();
                int total = totalConnections.get();

                if (currentSize < poolSize / 2 && total < MAX_TOTAL_CONNECTIONS) {
                    System.out.println("⚠️ Pool size low (" + currentSize + "/" + poolSize +
                            "), attempting to replenish...");
                    for (int i = 0; i < poolSize - currentSize && totalConnections.get() < MAX_TOTAL_CONNECTIONS; i++) {
                        try {
                            Connection conn = createConnectionWithTimeout(CONNECTION_TIMEOUT_MS);
                            if (isValidConnection(conn) && connectionPool.offer(conn)) {
                                totalConnections.incrementAndGet();
                                System.out.println("✅ Added new connection. Total: " + totalConnections.get());
                            }
                        } catch (SQLException e) {}
                    }
                }
            } catch (Exception e) {
                System.err.println("Health check error: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(healthChecker::shutdown));
    }

    private void startLeakDetector() {
        Thread leakDetector = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000);
                    int active = activeConnections.get();
                    if (active > poolSize * 2) {
                        System.err.println("⚠️ WARNING: " + active + " active connections (pool size: " +
                                poolSize + ") - Possible connection leak!");
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
        if (!isInitialized.get()) {
            throw new SQLException("Connection pool not initialized");
        }

        try {
            Connection conn = connectionPool.poll(15, TimeUnit.SECONDS);  // Tăng timeout
            if (conn == null || !isValidConnection(conn)) {
                if (totalConnections.get() < MAX_TOTAL_CONNECTIONS) {
                    conn = createConnectionWithTimeout(CONNECTION_TIMEOUT_MS);
                    totalConnections.incrementAndGet();
                    System.err.println("⚠️ Pool exhausted! Created new connection (total: " +
                            totalConnections.get() + ")");
                } else {
                    throw new SQLException("Connection pool exhausted and max connections reached");
                }
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
            if (!isValidConnection(conn)) {
                activeConnections.decrementAndGet();
                totalConnections.decrementAndGet();
                try { conn.close(); } catch (SQLException ignored) {}
                return;
            }

            if (!conn.getAutoCommit()) {
                try { conn.rollback(); } catch (SQLException ignored) {}
                conn.setAutoCommit(true);
            }

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
            try { conn.close(); totalConnections.decrementAndGet(); } catch (SQLException ex) {}
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
        isInitialized.set(false);
        System.out.println("✅ Closed " + closedCount + " database connections");
    }

    public boolean isInitialized() {
        return isInitialized.get();
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

    public int getPoolSize() {
        return connectionPool.size();
    }
}