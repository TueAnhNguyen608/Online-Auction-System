package server.repository;

import common.model.bidding.Bid;
import server.pool.DatabaseConnectionPool;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class BidRepository {

    private static volatile BidRepository instance;
    private final DatabaseConnectionPool pool;

    private BidRepository() {
        this.pool = DatabaseConnectionPool.getInstance();
    }

    public static BidRepository getInstance() {
        if (instance == null) {
            synchronized (BidRepository.class) {
                if (instance == null) {
                    instance = new BidRepository();
                }
            }
        }
        return instance;
    }

    // ========== SAVE ==========

    public boolean save(Bid bid) throws SQLException {
        String sql = "INSERT INTO bids(id, item_id, item_name, bidder_id, bidder_name, amount, bid_time, is_auto_bid) " +
                "VALUES(?,?,?,?,?,?,?,?)";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, bid.getItemId());
            stmt.setString(3, bid.getItemName());
            stmt.setString(4, bid.getBidderId());
            stmt.setString(5, bid.getBidderName());
            stmt.setDouble(6, bid.getAmount());
            stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setBoolean(8, bid.isAutoBid());
            return stmt.executeUpdate() > 0;
        }
    }

    // ========== FIND METHODS ==========

    public List<Bid> findByItemId(String itemId) throws SQLException {
        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT * FROM bids WHERE item_id = ? ORDER BY amount DESC";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                bids.add(mapRow(rs));
            }
        }
        return bids;
    }

    public List<Bid> findByBidderId(String userId) throws SQLException {
        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT b.*, i.current_bidder_id AS winner_id, " +
                "i.current_price AS final_price, i.status AS item_status " +
                "FROM bids b JOIN items i ON b.item_id = i.id " +
                "WHERE b.bidder_id = ? ORDER BY b.bid_time DESC";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Bid bid = mapRow(rs);
                try {
                    bid.setFinalPrice(rs.getDouble("final_price"));
                } catch (SQLException ignored) {}
                try {
                    bid.setItemStatus(rs.getString("item_status"));
                } catch (SQLException ignored) {}
                try {
                    String winnerId = rs.getString("winner_id");
                    bid.setWinning(userId != null && userId.equals(winnerId));
                } catch (SQLException ignored) {}
                bids.add(bid);
            }
        }
        return bids;
    }

    public Optional<Bid> findHighestBidByItemId(String itemId) throws SQLException {
        String sql = "SELECT * FROM bids WHERE item_id = ? ORDER BY amount DESC LIMIT 1";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        }
    }

    // ========== COUNT METHODS ==========

    public int countByItemId(String itemId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bids WHERE item_id = ?";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    public int countByBidderId(String userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bids WHERE bidder_id = ?";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM bids";

        try (Connection conn = pool.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    // ========== DELETE METHODS ==========

    public boolean deleteByItemId(String itemId) throws SQLException {
        String sql = "DELETE FROM bids WHERE item_id = ?";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteByBidderId(String userId) throws SQLException {
        String sql = "DELETE FROM bids WHERE bidder_id = ?";

        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    // ========== PRIVATE METHODS ==========

    private Bid mapRow(ResultSet rs) throws SQLException {
        Bid bid = new Bid();
        bid.setId(rs.getString("id"));
        bid.setItemId(rs.getString("item_id"));
        bid.setItemName(rs.getString("item_name"));
        bid.setBidderId(rs.getString("bidder_id"));
        bid.setBidderName(rs.getString("bidder_name"));
        bid.setAmount(rs.getDouble("amount"));
        bid.setAutoBid(rs.getBoolean("is_auto_bid"));
        if (rs.getTimestamp("bid_time") != null) {
            bid.setBidTime(rs.getTimestamp("bid_time").toLocalDateTime());
        }
        return bid;
    }
}