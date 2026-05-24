package server.repository;

import common.model.item.Item;
import common.model.item.ItemStatus;
import server.pool.DatabaseConnectionPool;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class ItemRepository {

    private static volatile ItemRepository instance;
    private final DatabaseConnectionPool pool;

    private ItemRepository() {
        this.pool = DatabaseConnectionPool.getInstance();
    }

    public static ItemRepository getInstance() {
        if (instance == null) {
            synchronized (ItemRepository.class) {
                if (instance == null) {
                    instance = new ItemRepository();
                }
            }
        }
        return instance;
    }

    // ========== FIND METHODS ==========

    public Optional<Item> findById(String id) throws SQLException {
        String sql = "SELECT * FROM items WHERE id = ?";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        }
    }

    public List<Item> findAll() throws SQLException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items ORDER BY created_at DESC";

        try (Connection conn = pool.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(mapRow(rs));
            }
        }
        return items;
    }

    public List<Item> findActiveItems() throws SQLException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE status = 'ACTIVE' AND end_time > NOW() ORDER BY end_time ASC";

        try (Connection conn = pool.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(mapRow(rs));
            }
        }
        return items;
    }

    public List<Item> findBySellerId(String sellerId) throws SQLException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE seller_id = ? ORDER BY created_at DESC";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sellerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                items.add(mapRow(rs));
            }
        }
        return items;
    }

    public List<Item> findByStatus(ItemStatus status) throws SQLException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE status = ? ORDER BY end_time ASC";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                items.add(mapRow(rs));
            }
        }
        return items;
    }

    // ========== COUNT METHODS ==========

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM items";

        try (Connection conn = pool.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    public int countActive() throws SQLException {
        String sql = "SELECT COUNT(*) FROM items WHERE status = 'ACTIVE' AND end_time > NOW()";

        try (Connection conn = pool.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    public int countBySellerId(String sellerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM items WHERE seller_id = ?";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sellerId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    // ========== SAVE/UPDATE METHODS ==========

    public boolean save(Item item) throws SQLException {
        String sql = "INSERT INTO items(id, name, description, starting_price, current_price, " +
                "seller_id, seller_name, start_time, end_time, status, step_price) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getId());
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getDescription());
            stmt.setDouble(4, item.getStartingPrice());
            stmt.setDouble(5, item.getCurrentPrice());
            stmt.setString(6, item.getSellerId());
            stmt.setString(7, item.getSellerName());
            stmt.setTimestamp(8, Timestamp.valueOf(item.getStartTime()));
            stmt.setTimestamp(9, Timestamp.valueOf(item.getEndTime()));
            stmt.setString(10, item.getStatus().name());
            stmt.setDouble(11, item.getStepPrice());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean update(Item item) throws SQLException {
        String sql = "UPDATE items SET name = ?, description = ?, starting_price = ?, " +
                "current_price = ?, end_time = ?, status = ? WHERE id = ?";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getName());
            stmt.setString(2, item.getDescription());
            stmt.setDouble(3, item.getStartingPrice());
            stmt.setDouble(4, item.getCurrentPrice());
            stmt.setTimestamp(5, Timestamp.valueOf(item.getEndTime()));
            stmt.setString(6, item.getStatus().name());
            stmt.setString(7, item.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateCurrentPrice(String itemId, double newPrice, String bidderId, String bidderName) throws SQLException {
        String sql = "UPDATE items SET current_price = ?, current_bidder_id = ?, current_bidder_name = ?, bid_count = bid_count + 1 WHERE id = ?";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, newPrice);
            stmt.setString(2, bidderId);
            stmt.setString(3, bidderName);
            stmt.setString(4, itemId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(String itemId, ItemStatus status) throws SQLException {
        String sql = "UPDATE items SET status = ? WHERE id = ?";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setString(2, itemId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean extendEndTime(String itemId, int seconds) throws SQLException {
        String sql = "UPDATE items SET end_time = DATE_ADD(end_time, INTERVAL ? SECOND) WHERE id = ?";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, seconds);
            stmt.setString(2, itemId);
            return stmt.executeUpdate() > 0;
        }
    }

    // ========== DELETE METHODS ==========

    public boolean deleteById(String id) throws SQLException {
        String sql = "DELETE FROM items WHERE id = ?";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    // ========== AUCTION MANAGEMENT ==========

    public void endExpiredAuctions() throws SQLException {
        String sql = "UPDATE items SET status = 'ENDED' WHERE status = 'ACTIVE' AND end_time < NOW()";

        try (Connection conn = pool.getConnection();
             Statement stmt = conn.createStatement()) {
            int updated = stmt.executeUpdate(sql);
            if (updated > 0) {
                System.out.println("✅ Đã kết thúc " + updated + " phiên đấu giá");
            }
        }
    }

    public boolean forceEndAuction(String itemId) throws SQLException {
        String sql = "UPDATE items SET status = 'ENDED', end_time = NOW() WHERE id = ? AND status = 'ACTIVE'";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            return stmt.executeUpdate() > 0;
        }
    }

    // ========== PRIVATE METHODS ==========

    private Item mapRow(ResultSet rs) throws SQLException {
        Item item = new Item();
        item.setId(rs.getString("id"));
        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        item.setStartingPrice(rs.getDouble("starting_price"));
        item.setCurrentPrice(rs.getDouble("current_price"));
        item.setCurrentBidderId(rs.getString("current_bidder_id"));
        item.setCurrentBidderName(rs.getString("current_bidder_name"));
        item.setSellerId(rs.getString("seller_id"));
        item.setSellerName(rs.getString("seller_name"));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                item.setStatus(ItemStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                item.setStatus(ItemStatus.PENDING);
            }
        }

        item.setBidCount(rs.getInt("bid_count"));

        if (rs.getTimestamp("end_time") != null) {
            item.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        }
        if (rs.getTimestamp("start_time") != null) {
            item.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        }

        return item;
    }
}