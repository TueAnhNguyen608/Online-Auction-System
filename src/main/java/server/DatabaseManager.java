package server;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import common.*;

public class DatabaseManager {
    private Connection conn;
    private final ConcurrentHashMap<String, ReentrantLock> itemLocks = new ConcurrentHashMap<>();

    // Anti-Sniping constants
    private static final int SNIPING_WINDOW_SECONDS = 30;
    private static final int EXTENSION_SECONDS = 60;

    // Database configuration
    private static final String DB_URL = "jdbc:mysql://localhost:3306/auction_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Nhom7123456";

    public DatabaseManager() throws SQLException {
        conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        System.out.println("✅ Kết nối database thành công!");
    }

    // Lấy connection mới cho transaction
    private Connection getNewConnection() throws SQLException {
        Connection newConn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        newConn.setAutoCommit(false);
        return newConn;
    }

    // ========== USER ==========
    public User login(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username=? AND password=? AND is_active=true";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, username);
        stmt.setString(2, password);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            User user = new User();
            user.setId(rs.getString("id"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setRole(rs.getString("role"));
            user.setBalance(rs.getDouble("balance"));
            return user;
        }
        return null;
    }

    public boolean register(User user) throws SQLException {
        String sql = "INSERT INTO users(id, username, password, email, role, balance) VALUES(?,?,?,?,?,0)";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, user.getId());
        stmt.setString(2, user.getUsername());
        stmt.setString(3, user.getPassword());
        stmt.setString(4, user.getEmail());
        stmt.setString(5, user.getRole());
        return stmt.executeUpdate() > 0;
    }

    public double getUserBalance(String userId) throws SQLException {
        String sql = "SELECT balance FROM users WHERE id=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, userId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getDouble("balance");
        }
        return 0;
    }

    public boolean updateBalance(String userId, double newBalance) throws SQLException {
        String sql = "UPDATE users SET balance=? WHERE id=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setDouble(1, newBalance);
        stmt.setString(2, userId);
        return stmt.executeUpdate() > 0;
    }

    // ========== ITEM ==========
    public boolean createItem(Item item) throws SQLException {
        String sql = "INSERT INTO items(id, name, description, starting_price, current_price, " +
                "seller_id, seller_name, start_time, end_time, status, step_price) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, item.getId());
        stmt.setString(2, item.getName());
        stmt.setString(3, item.getDescription());
        stmt.setDouble(4, item.getStartingPrice());
        stmt.setDouble(5, item.getCurrentPrice());
        stmt.setString(6, item.getSellerId());
        stmt.setString(7, item.getSellerName());
        stmt.setTimestamp(8, Timestamp.valueOf(item.getStartTime()));
        stmt.setTimestamp(9, Timestamp.valueOf(item.getEndTime()));
        stmt.setString(10, item.getStatus());
        stmt.setDouble(11, item.getStartingPrice() * 0.05);
        return stmt.executeUpdate() > 0;
    }

    public List<Item> getActiveItems() throws SQLException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE status='ACTIVE' AND end_time > NOW() ORDER BY end_time ASC";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
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
            item.setStatus(rs.getString("status"));
            item.setBidCount(rs.getInt("bid_count"));
            if (rs.getTimestamp("end_time") != null) {
                item.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
            }
            items.add(item);
        }
        return items;
    }

    public Item getItemById(String itemId) throws SQLException {
        String sql = "SELECT * FROM items WHERE id=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, itemId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
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
            item.setStatus(rs.getString("status"));
            item.setBidCount(rs.getInt("bid_count"));
            if (rs.getTimestamp("end_time") != null) {
                item.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
            }
            return item;
        }
        return null;
    }

    // ========== BID - CONCURRENT SAFE with Anti-Sniping ==========
    public boolean placeBidSafe(Bid bid) throws SQLException {
        ReentrantLock lock = itemLocks.computeIfAbsent(bid.getItemId(), k -> new ReentrantLock());
        lock.lock();

        Connection localConn = null;

        try {
            localConn = getNewConnection();

            // SELECT FOR UPDATE
            String checkSql = "SELECT * FROM items WHERE id=? FOR UPDATE";
            PreparedStatement checkStmt = localConn.prepareStatement(checkSql);
            checkStmt.setString(1, bid.getItemId());
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                localConn.rollback();
                return false;
            }

            if (!"ACTIVE".equals(rs.getString("status"))) {
                localConn.rollback();
                return false;
            }

            Timestamp endTime = rs.getTimestamp("end_time");
            if (endTime.before(new Timestamp(System.currentTimeMillis()))) {
                localConn.rollback();
                return false;
            }

            double currentPrice = rs.getDouble("current_price");
            double stepPrice = rs.getDouble("step_price");
            if (stepPrice <= 0) stepPrice = currentPrice * 0.05;

            if (bid.getAmount() < currentPrice + stepPrice) {
                localConn.rollback();
                return false;
            }

            String sellerId = rs.getString("seller_id");
            if (bid.getBidderId().equals(sellerId)) {
                localConn.rollback();
                return false;
            }

            // ========== ANTI-SNIPING ==========
            long secondsLeft = (endTime.getTime() - System.currentTimeMillis()) / 1000;
            boolean isExtended = false;

            if (secondsLeft <= SNIPING_WINDOW_SECONDS && secondsLeft > 0) {
                String extendSql = "UPDATE items SET end_time = DATE_ADD(end_time, INTERVAL ? SECOND) WHERE id=?";
                PreparedStatement extendStmt = localConn.prepareStatement(extendSql);
                extendStmt.setInt(1, EXTENSION_SECONDS);
                extendStmt.setString(2, bid.getItemId());
                extendStmt.executeUpdate();
                extendStmt.close();
                isExtended = true;
                System.out.println("⏰ Anti-Sniping: Extended auction by " + EXTENSION_SECONDS + " seconds");
            }

            // Update item
            String updateSql = "UPDATE items SET current_price=?, current_bidder_id=?, current_bidder_name=?, bid_count=bid_count+1 WHERE id=?";
            PreparedStatement updateStmt = localConn.prepareStatement(updateSql);
            updateStmt.setDouble(1, bid.getAmount());
            updateStmt.setString(2, bid.getBidderId());
            updateStmt.setString(3, bid.getBidderName());
            updateStmt.setString(4, bid.getItemId());
            updateStmt.executeUpdate();

            // Insert bid
            String insertSql = "INSERT INTO bids(id, item_id, item_name, bidder_id, bidder_name, amount, bid_time, is_auto_bid) VALUES(?,?,?,?,?,?,NOW(),?)";
            PreparedStatement insertStmt = localConn.prepareStatement(insertSql);
            insertStmt.setString(1, bid.getId());
            insertStmt.setString(2, bid.getItemId());
            insertStmt.setString(3, rs.getString("name"));
            insertStmt.setString(4, bid.getBidderId());
            insertStmt.setString(5, bid.getBidderName());
            insertStmt.setDouble(6, bid.getAmount());
            insertStmt.setBoolean(7, bid.isAutoBid());
            insertStmt.executeUpdate();

            localConn.commit();

            // Broadcast extension if needed
            if (isExtended) {
                PriceUpdateEvent extEvent = new PriceUpdateEvent();
                extEvent.setType("EXTENSION");
                extEvent.setItemId(bid.getItemId());
                extEvent.setRemainingSeconds(EXTENSION_SECONDS);
                AuctionServer.broadcastToItem(bid.getItemId(), extEvent);
            }

            // ========== TRIGGER AUTO-BID ==========
            triggerAutoBids(bid.getItemId(), bid.getAmount(), bid.getBidderId());

            return true;

        } catch (SQLException e) {
            if (localConn != null) localConn.rollback();
            throw e;
        } finally {
            if (localConn != null) localConn.close();
            lock.unlock();
        }
    }

    // ========== AUTO-BID TRIGGER ==========
    public void triggerAutoBids(String itemId, double currentPrice, String currentBidderId) throws SQLException {
        String getItemSql = "SELECT step_price FROM items WHERE id=?";
        PreparedStatement getItemStmt = conn.prepareStatement(getItemSql);
        getItemStmt.setString(1, itemId);
        ResultSet rs = getItemStmt.executeQuery();

        double stepPrice = 250000;
        if (rs.next()) {
            stepPrice = rs.getDouble("step_price");
            if (stepPrice <= 0) stepPrice = 250000;
        }
        rs.close();
        getItemStmt.close();

        String sql = "SELECT user_id, max_amount FROM auto_bid_settings WHERE item_id=? AND user_id != ? ORDER BY max_amount DESC";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, itemId);
        stmt.setString(2, currentBidderId);
        ResultSet autoBidders = stmt.executeQuery();

        double nextPrice = currentPrice + stepPrice;

        while (autoBidders.next()) {
            String userId = autoBidders.getString("user_id");
            double maxAmount = autoBidders.getDouble("max_amount");

            if (maxAmount >= nextPrice) {
                String getUserSql = "SELECT username FROM users WHERE id=?";
                PreparedStatement getUserStmt = conn.prepareStatement(getUserSql);
                getUserStmt.setString(1, userId);
                ResultSet userRs = getUserStmt.executeQuery();
                String username = userId;
                if (userRs.next()) {
                    username = userRs.getString("username");
                }
                userRs.close();
                getUserStmt.close();

                Bid autoBid = new Bid();
                autoBid.setItemId(itemId);
                autoBid.setBidderId(userId);
                autoBid.setBidderName(username);
                autoBid.setAmount(nextPrice);
                autoBid.setAutoBid(true);

                System.out.println("🤖 Auto-bid triggered for " + username + " with amount " + nextPrice);
                placeBidSafe(autoBid);

                nextPrice += stepPrice;
            } else {
                break;
            }
        }
        autoBidders.close();
        stmt.close();
    }

    public List<Bid> getBidHistory(String itemId) throws SQLException {
        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT * FROM bids WHERE item_id=? ORDER BY amount DESC";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, itemId);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
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
            bids.add(bid);
        }
        return bids;
    }

    public List<PricePoint> getPriceHistory(String itemId) throws SQLException {
        List<PricePoint> history = new ArrayList<>();
        String sql = "SELECT amount, bid_time FROM bids WHERE item_id=? ORDER BY bid_time ASC";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, itemId);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            history.add(new PricePoint(
                    rs.getDouble("amount"),
                    rs.getTimestamp("bid_time").toLocalDateTime()
            ));
        }
        return history;
    }

    public void endExpiredAuctions() throws SQLException {
        String sql = "UPDATE items SET status='ENDED' WHERE status='ACTIVE' AND end_time < NOW()";
        Statement stmt = conn.createStatement();
        int updated = stmt.executeUpdate(sql);
        if (updated > 0) {
            System.out.println("✅ Đã kết thúc " + updated + " phiên đấu giá");
        }
    }

    // ========== AUTO BID SETTINGS ==========
    public boolean setAutoBid(String userId, String itemId, double maxAmount) throws SQLException {
        String sql = "INSERT INTO auto_bid_settings (user_id, item_id, max_amount) VALUES (?,?,?) " +
                "ON DUPLICATE KEY UPDATE max_amount=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, userId);
        stmt.setString(2, itemId);
        stmt.setDouble(3, maxAmount);
        stmt.setDouble(4, maxAmount);
        return stmt.executeUpdate() > 0;
    }

    public boolean cancelAutoBid(String userId, String itemId) throws SQLException {
        String sql = "DELETE FROM auto_bid_settings WHERE user_id=? AND item_id=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, userId);
        stmt.setString(2, itemId);
        return stmt.executeUpdate() > 0;
    }

    public double getAutoBidMax(String userId, String itemId) throws SQLException {
        String sql = "SELECT max_amount FROM auto_bid_settings WHERE user_id=? AND item_id=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, userId);
        stmt.setString(2, itemId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getDouble("max_amount");
        }
        return 0;
    }
}