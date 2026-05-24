package server.repository;

import server.pool.DatabaseConnectionPool;
import java.sql.*;
import java.util.*;

public class AutoBidRepository {

    private static volatile AutoBidRepository instance;
    private final DatabaseConnectionPool pool;

    private AutoBidRepository() {
        this.pool = DatabaseConnectionPool.getInstance();
    }

    public static AutoBidRepository getInstance() {
        if (instance == null) {
            synchronized (AutoBidRepository.class) {
                if (instance == null) {
                    instance = new AutoBidRepository();
                }
            }
        }
        return instance;
    }

    public boolean setAutoBid(String userId, String itemId, double maxAmount) throws SQLException {
        String sql = "INSERT INTO auto_bid_settings (user_id, item_id, max_amount) VALUES (?,?,?) " +
                "ON DUPLICATE KEY UPDATE max_amount = ?";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, itemId);
            stmt.setDouble(3, maxAmount);
            stmt.setDouble(4, maxAmount);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean cancelAutoBid(String userId, String itemId) throws SQLException {
        String sql = "DELETE FROM auto_bid_settings WHERE user_id = ? AND item_id = ?";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, itemId);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Map<String, Object>> findByUserId(String userId) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT a.item_id, a.max_amount, i.name, i.current_price " +
                "FROM auto_bid_settings a JOIN items i ON a.item_id = i.id " +
                "WHERE a.user_id = ?";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("itemId", rs.getString("item_id"));
                row.put("itemName", rs.getString("name"));
                row.put("maxAmount", rs.getDouble("max_amount"));
                row.put("currentPrice", rs.getDouble("current_price"));
                result.add(row);
            }
        }
        return result;
    }

    // ✅ PHƯƠNG THỨC findByItemId - BẮT BUỘC PHẢI CÓ
    public List<Map<String, Object>> findByItemId(String itemId) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT a.user_id, u.username, a.max_amount " +
                "FROM auto_bid_settings a JOIN users u ON a.user_id = u.id " +
                "WHERE a.item_id = ? ORDER BY a.max_amount DESC";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("user_id", rs.getString("user_id"));
                row.put("username", rs.getString("username"));
                row.put("max_amount", rs.getDouble("max_amount"));
                result.add(row);
            }
        }
        return result;
    }
}