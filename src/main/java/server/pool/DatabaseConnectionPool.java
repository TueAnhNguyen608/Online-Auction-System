package server.pool;

import common.constant.AuctionConstants;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DatabaseConnectionPool {

    private static volatile DatabaseConnectionPool instance;
    private final BlockingQueue<Connection> connectionPool;
    private final String url;
    private final String username;
    private final String password;
    private final int poolSize;
    private boolean isInitialized = false;

    private DatabaseConnectionPool() {
        this.url = AuctionConstants.DB_URL;
        this.username = AuctionConstants.DB_USER;
        this.password = AuctionConstants.DB_PASSWORD;
        this.poolSize = AuctionConstants.DB_POOL_SIZE;
        this.connectionPool = new LinkedBlockingQueue<>(poolSize);

        // Load MySQL Driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✅ MySQL Driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL Driver not found! Please add mysql-connector-java to classpath");
            System.err.println("   Download from: https://dev.mysql.com/downloads/connector/j/");
            e.printStackTrace();
        }

        initializePool();
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
                    connectionPool.offer(conn);
                    successCount++;
                }
            } catch (SQLException e) {
                System.err.println("Failed to create connection " + (i + 1) + ": " + e.getMessage());
            }
        }

        if (successCount > 0) {
            isInitialized = true;
            System.out.println("✅ Database connection pool initialized with " + successCount + "/" + poolSize + " connections");
            System.out.println("   Database: " + url);
            System.out.println("   User: " + username);
        } else {
            System.err.println("❌ Failed to initialize database connection pool!");
            System.err.println("   Please check:");
            System.err.println("   1. MySQL server is running");
            System.err.println("   2. Database 'auction_db' exists");
            System.err.println("   3. Username/password is correct: " + username + "/" + password);
        }
    }

    public Connection getConnection() throws SQLException {
        if (!isInitialized) {
            throw new SQLException("Connection pool not initialized");
        }

        try {
            Connection conn = connectionPool.poll(5, TimeUnit.SECONDS);
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(url, username, password);
            }
            return conn;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Cannot get connection from pool", e);
        }
    }

    public void returnConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed() && connectionPool.size() < poolSize) {
                    connectionPool.offer(conn);
                } else if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error returning connection: " + e.getMessage());
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
            return conn != null && !conn.isClosed() && conn.isValid(3);
        } catch (SQLException e) {
            System.err.println("❌ Database connection test failed: " + e.getMessage());
            return false;
        }
    }
}