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

    private static final int SNIPING_WINDOW_SECONDS = 30;
    private static final int EXTENSION_SECONDS = 60;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/auction_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Nhom7123456";

    // ========== SINGLETON PATTERN ==========
    private static volatile DatabaseManager instance;

    private DatabaseManager() throws SQLException {
        this.conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        createTablesIfNotExist();
        System.out.println("✅ Kết nối database thành công!");
    }

    public static DatabaseManager getInstance() throws SQLException {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    @Deprecated
    public DatabaseManager(boolean forceNew) throws SQLException {
        this.conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        createTablesIfNotExist();
        System.out.println("✅ Kết nối database thành công!");
    }

    // ========== PHẦN CÒN LẠI GIỮ NGUYÊN ==========

    private void createTablesIfNotExist() throws SQLException {
        String createUsers = "CREATE TABLE IF NOT EXISTS users (" +
                "id VARCHAR(36) PRIMARY KEY, username VARCHAR(50) UNIQUE NOT NULL, " +
                "password VARCHAR(255) NOT NULL, email VARCHAR(100), role VARCHAR(20) DEFAULT 'BIDDER', " +
                "balance DECIMAL(15,2) DEFAULT 0, is_active BOOLEAN DEFAULT TRUE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        String createItems = "CREATE TABLE IF NOT EXISTS items (" +
                "id VARCHAR(36) PRIMARY KEY, name VARCHAR(200) NOT NULL, description TEXT, " +
                "starting_price DECIMAL(15,2) NOT NULL, current_price DECIMAL(15,2) NOT NULL, " +
                "current_bidder_id VARCHAR(36), current_bidder_name VARCHAR(50), seller_id VARCHAR(36) NOT NULL, " +
                "seller_name VARCHAR(50), start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, end_time TIMESTAMP NOT NULL, " +
                "status VARCHAR(20) DEFAULT 'ACTIVE', bid_count INT DEFAULT 0, step_price DECIMAL(15,2) DEFAULT 0, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        String createBids = "CREATE TABLE IF NOT EXISTS bids (" +
                "id VARCHAR(36) PRIMARY KEY, item_id VARCHAR(36) NOT NULL, item_name VARCHAR(200), " +
                "bidder_id VARCHAR(36) NOT NULL, bidder_name VARCHAR(50), amount DECIMAL(15,2) NOT NULL, " +
                "bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, is_auto_bid BOOLEAN DEFAULT FALSE)";

        String createAutoBid = "CREATE TABLE IF NOT EXISTS auto_bid_settings (" +
                "user_id VARCHAR(36) NOT NULL, item_id VARCHAR(36) NOT NULL, max_amount DECIMAL(15,2) NOT NULL, " +
                "PRIMARY KEY (user_id, item_id))";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createUsers);
            stmt.execute(createItems);
            stmt.execute(createBids);
            stmt.execute(createAutoBid);
        }
    }

    private Connection getNewConnection() throws SQLException {
        Connection newConn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        newConn.setAutoCommit(false);
        return newConn;
    }

    // ========== USER ==========
    public User login(String username, String password) throws SQLException, AuthenticationException {
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
        throw new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu", username);
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

    // ========== ITEM ==========
    public void validateItem(Item item) throws DataValidationException {
        DataValidationException validation = new DataValidationException("Dữ liệu sản phẩm không hợp lệ");

        if (item.getName() == null || item.getName().trim().isEmpty()) {
            validation.addError("Tên sản phẩm không được để trống");
        }

        if (item.getStartingPrice() <= 0) {
            validation.addError("Giá khởi điểm phải lớn hơn 0");
        }

        if (item.getEndTime() == null) {
            validation.addError("Thời gian kết thúc không được để trống");
        } else if (item.getEndTime().isBefore(LocalDateTime.now().plusMinutes(5))) {
            validation.addError("Thời gian kết thúc phải sau ít nhất 5 phút");
        }

        if (validation.hasErrors()) {
            throw validation;
        }
    }

    public boolean createItem(Item item) throws SQLException, DataValidationException {
        validateItem(item);

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
        stmt.setString(10, "ACTIVE");
        stmt.setDouble(11, item.getStartingPrice() * 0.05);
        return stmt.executeUpdate() > 0;
    }

    public List<Item> getActiveItems() throws SQLException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE status='ACTIVE' AND end_time > NOW() ORDER BY end_time ASC";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            items.add(extractItem(rs));
        }
        return items;
    }

    public Item getItemById(String itemId) throws SQLException {
        String sql = "SELECT * FROM items WHERE id=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, itemId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return extractItem(rs);
        }
        return null;
    }

    private Item extractItem(ResultSet rs) throws SQLException {
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
        if (rs.getTimestamp("start_time") != null) {
            item.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        }
        return item;
    }

    // ========== BID WITH VALIDATION ==========
    public void placeBidWithValidation(Bid bid) throws SQLException, InvalidBidException, AuctionClosedException {
        ReentrantLock lock = itemLocks.computeIfAbsent(bid.getItemId(), k -> new ReentrantLock());
        lock.lock();
        Connection localConn = null;

        try {
            localConn = getNewConnection();

            String checkSql = "SELECT * FROM items WHERE id=? FOR UPDATE";
            PreparedStatement checkStmt = localConn.prepareStatement(checkSql);
            checkStmt.setString(1, bid.getItemId());
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                localConn.rollback();
                throw new InvalidBidException("Sản phẩm không tồn tại");
            }

            String status = rs.getString("status");
            Timestamp endTime = rs.getTimestamp("end_time");
            double currentPrice = rs.getDouble("current_price");
            double stepPrice = rs.getDouble("step_price");
            if (stepPrice <= 0) stepPrice = currentPrice * 0.05;

            if ("ENDED".equals(status) || "CANCELLED".equals(status)) {
                localConn.rollback();
                throw new AuctionClosedException("Phiên đấu giá đã kết thúc", bid.getItemId());
            }

            if (endTime.before(new Timestamp(System.currentTimeMillis()))) {
                localConn.rollback();
                throw new AuctionClosedException("Thời gian đấu giá đã kết thúc", bid.getItemId());
            }

            double minimumBid = currentPrice + stepPrice;
            if (bid.getAmount() < minimumBid) {
                localConn.rollback();
                throw new InvalidBidException(
                        "Giá đặt phải cao hơn giá hiện tại ít nhất " + stepPrice + " VND",
                        bid.getItemId(), bid.getAmount(), minimumBid);
            }

            String sellerId = rs.getString("seller_id");
            if (bid.getBidderId().equals(sellerId)) {
                localConn.rollback();
                throw new InvalidBidException("Người bán không thể đấu giá sản phẩm của chính mình");
            }

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

            String updateSql = "UPDATE items SET current_price=?, current_bidder_id=?, current_bidder_name=?, bid_count=bid_count+1 WHERE id=?";
            PreparedStatement updateStmt = localConn.prepareStatement(updateSql);
            updateStmt.setDouble(1, bid.getAmount());
            updateStmt.setString(2, bid.getBidderId());
            updateStmt.setString(3, bid.getBidderName());
            updateStmt.setString(4, bid.getItemId());
            updateStmt.executeUpdate();

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

            if (isExtended) {
                PriceUpdateEvent extEvent = new PriceUpdateEvent();
                extEvent.setType("EXTENSION");
                extEvent.setItemId(bid.getItemId());
                extEvent.setRemainingSeconds(EXTENSION_SECONDS);
                AuctionServer.broadcastToItem(bid.getItemId(), extEvent);
            }

            triggerAutoBids(bid.getItemId(), bid.getAmount(), bid.getBidderId());

        } catch (SQLException e) {
            if (localConn != null) localConn.rollback();
            throw e;
        } finally {
            if (localConn != null) localConn.close();
            lock.unlock();
        }
    }

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
                try {
                    placeBidWithValidation(autoBid);
                } catch (InvalidBidException | AuctionClosedException e) {
                    System.out.println("Auto-bid failed: " + e.getMessage());
                }
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

    public void endExpiredAuctions() throws SQLException {
        String sql = "UPDATE items SET status='ENDED' WHERE status='ACTIVE' AND end_time < NOW()";
        Statement stmt = conn.createStatement();
        int updated = stmt.executeUpdate(sql);
        if (updated > 0) {
            System.out.println("✅ Đã kết thúc " + updated + " phiên đấu giá");
        }
    }

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

    // ========== STATISTICS METHODS ==========

    public int getTotalBids() throws SQLException {
        String sql = "SELECT COUNT(*) FROM bids";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getActiveItemsCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM items WHERE status='ACTIVE' AND end_time > NOW()";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int getTotalUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public double getTotalRevenue() throws SQLException {
        String sql = "SELECT SUM(current_price) FROM items WHERE status='ENDED'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        return 0;
    }

    // ========== ADMIN METHODS ==========

    public List<Item> getAllItems() throws SQLException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items ORDER BY created_at DESC";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        while (rs.next()) {
            items.add(extractItem(rs));
        }
        return items;
    }

    public boolean updateItem(Item item) throws SQLException {
        String sql = "UPDATE items SET name=?, description=?, starting_price=?, current_price=?, end_time=? WHERE id=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, item.getName());
        stmt.setString(2, item.getDescription());
        stmt.setDouble(3, item.getStartingPrice());
        stmt.setDouble(4, item.getCurrentPrice());
        stmt.setTimestamp(5, Timestamp.valueOf(item.getEndTime()));
        stmt.setString(6, item.getId());
        return stmt.executeUpdate() > 0;
    }

    public boolean deleteItem(String itemId) throws SQLException {
        String deleteBidsSql = "DELETE FROM bids WHERE item_id=?";
        PreparedStatement stmt1 = conn.prepareStatement(deleteBidsSql);
        stmt1.setString(1, itemId);
        stmt1.executeUpdate();
        stmt1.close();

        String deleteAutoBidSql = "DELETE FROM auto_bid_settings WHERE item_id=?";
        PreparedStatement stmt2 = conn.prepareStatement(deleteAutoBidSql);
        stmt2.setString(1, itemId);
        stmt2.executeUpdate();
        stmt2.close();

        String sql = "DELETE FROM items WHERE id=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, itemId);
        return stmt.executeUpdate() > 0;
    }

    public boolean forceEndAuction(String itemId) throws SQLException {
        String sql = "UPDATE items SET status='ENDED', end_time=NOW() WHERE id=? AND status='ACTIVE'";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, itemId);
        return stmt.executeUpdate() > 0;
    }

    public User getWinner(String itemId) throws SQLException {
        String sql = "SELECT u.* FROM users u " +
                "JOIN bids b ON b.bidder_id = u.id " +
                "WHERE b.item_id = ? " +
                "ORDER BY b.amount DESC LIMIT 1";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, itemId);
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

    // =========================================================================
    // PHƯƠNG THỨC MỚI CHO NHÓM 2 & 4 (THÊM VÀO CUỐI FILE)
    // =========================================================================

    public boolean updateUser(User user) throws SQLException {
        String sql = "UPDATE users SET email = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean changePassword(String userId, String oldPassword, String newPassword) throws SQLException {
        String checkSql = "SELECT password FROM users WHERE id = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, userId);
            ResultSet rs = checkStmt.executeQuery();
            if (!rs.next() || !rs.getString("password").equals(oldPassword)) {
                return false;
            }
        }
        String updateSql = "UPDATE users SET password = ? WHERE id = ?";
        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
            updateStmt.setString(1, newPassword);
            updateStmt.setString(2, userId);
            return updateStmt.executeUpdate() > 0;
        }
    }

    public List<Bid> getMyBidHistory(String userId) throws SQLException {
        List<Bid> history = new ArrayList<>();
        String sql = "SELECT b.*, i.current_bidder_id AS winner_id " +
                "FROM bids b JOIN items i ON b.item_id = i.id " +
                "WHERE b.bidder_id = ? ORDER BY b.bid_time DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Bid bid = new Bid();
                bid.setId(rs.getString("id"));
                bid.setItemId(rs.getString("item_id"));
                bid.setItemName(rs.getString("item_name"));
                bid.setBidderId(rs.getString("bidder_id"));
                bid.setBidderName(rs.getString("bidder_name"));
                bid.setAmount(rs.getDouble("amount"));
                if (rs.getTimestamp("bid_time") != null) {
                    bid.setBidTime(rs.getTimestamp("bid_time").toLocalDateTime());
                }
                bid.setAutoBid(rs.getBoolean("is_auto_bid"));
                bid.setWinning(userId.equals(rs.getString("winner_id")));
                history.add(bid);
            }
        }
        return history;
    }

    public List<Item> getWonProducts(String userId) throws SQLException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE status='ENDED' AND current_bidder_id = ? ORDER BY end_time DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                items.add(extractItem(rs));
            }
        }
        return items;
    }

    public List<PricePoint> getPriceHistory(String itemId) throws SQLException {
        List<PricePoint> points = new ArrayList<>();
        String sql = "SELECT amount, bid_time FROM bids WHERE item_id = ? ORDER BY bid_time ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                points.add(new PricePoint(rs.getDouble("amount"),
                        rs.getTimestamp("bid_time").toLocalDateTime()));
            }
        }
        if (points.isEmpty()) {
            Item item = getItemById(itemId);
            if (item != null && item.getStartTime() != null) {
                points.add(new PricePoint(item.getStartingPrice(), item.getStartTime()));
            }
        }
        return points;
    }
}