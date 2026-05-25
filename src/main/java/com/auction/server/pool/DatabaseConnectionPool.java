package com.auction.server.pool;

import com.auction.common.constant.AuctionConstants;
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
        // Kiểm tra môi trường CI/CD hoặc test
        boolean isCiEnvironment = "true".equals(System.getenv("CI")) ||
                "test".equals(System.getProperty("environment")) ||
                System.getenv("GITHUB_ACTIONS") != null;

        if (isCiEnvironment) {
            // Dùng H2 in-memory cho CI/CD và test
            this.url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL";
            this.username = "sa";
            this.password = "";
            this.poolSize = 5;
            System.out.println("✅ Using H2 in-memory database for CI/CD/Test environment");
        } else {
            // Dùng MySQL cho production
            this.url = AuctionConstants.DB_URL;
            this.username = AuctionConstants.DB_USER;
            this.password = AuctionConstants.DB_PASSWORD;
            this.poolSize = AuctionConstants.DB_POOL_SIZE;
            System.out.println("✅ Using MySQL database for production environment");
        }

        this.connectionPool = new LinkedBlockingQueue<>(poolSize);

        // Load H2 driver (cho test)
        try {
            Class.forName("org.h2.Driver");
            System.out.println("✅ H2 Driver loaded");
        } catch (ClassNotFoundException e) {
            // Không sao nếu không có H2 (production)
        }

        // Load MySQL driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✅ MySQL Driver loaded");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL Driver not found!");
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
        } else {
            System.err.println("❌ Failed to initialize database connection pool!");
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
                // Reset connection before returning to pool
                if (!conn.isClosed()) {
                    conn.setAutoCommit(true);
                    conn.rollback();
                    if (connectionPool.size() < poolSize) {
                        connectionPool.offer(conn);
                    } else {
                        conn.close();
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error returning connection: " + e.getMessage());
                try {
                    conn.close();
                } catch (SQLException ex) {
                    // Ignore
                }
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