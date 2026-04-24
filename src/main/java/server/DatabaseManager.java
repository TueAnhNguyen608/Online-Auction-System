package server;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import common.*;

public class DatabaseManager {
    private Connection conn;

    public DatabaseManager() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/auction_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        String user = "root";
        String password = "Nhom7123456";

        conn = DriverManager.getConnection(url, user, password);
        System.out.println("✅ Kết nối database thành công!");
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
        String sql = "INSERT INTO users(id, username, password, email, role) VALUES(?,?,?,?,?)";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, user.getId());
        stmt.setString(2, user.getUsername());
        stmt.setString(3, user.getPassword());
        stmt.setString(4, user.getEmail());
        stmt.setString(5, user.getRole());
        return stmt.executeUpdate() > 0;
    }

    // ========== ITEM ==========
    public boolean createItem(Item item) throws SQLException {
        String sql = "INSERT INTO items(id, name, description, starting_price, current_price, seller_id, seller_name, start_time, end_time, status) VALUES(?,?,?,?,?,?,?,?,?,?)";
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
        return stmt.executeUpdate() > 0;
    }

    public List<Item> getActiveItems() throws SQLException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE status='ACTIVE' AND end_time > NOW()";
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
            items.add(item);
        }
        return items;
    }

    // ========== BID ==========
    public boolean placeBid(Bid bid) throws SQLException {
        String checkSql = "SELECT * FROM items WHERE id=? AND status='ACTIVE' AND end_time > NOW()";
        PreparedStatement checkStmt = conn.prepareStatement(checkSql);
        checkStmt.setString(1, bid.getItemId());
        ResultSet rs = checkStmt.executeQuery();

        if (!rs.next()) return false;
        if (bid.getAmount() <= rs.getDouble("current_price")) return false;
        if (bid.getBidderId().equals(rs.getString("seller_id"))) return false;

        String updateSql = "UPDATE items SET current_price=?, current_bidder_id=?, current_bidder_name=?, bid_count=bid_count+1 WHERE id=?";
        PreparedStatement updateStmt = conn.prepareStatement(updateSql);
        updateStmt.setDouble(1, bid.getAmount());
        updateStmt.setString(2, bid.getBidderId());
        updateStmt.setString(3, bid.getBidderName());
        updateStmt.setString(4, bid.getItemId());
        updateStmt.executeUpdate();

        String insertSql = "INSERT INTO bids(id, item_id, item_name, bidder_id, bidder_name, amount) VALUES(?,?,?,?,?,?)";
        PreparedStatement insertStmt = conn.prepareStatement(insertSql);
        insertStmt.setString(1, bid.getId());
        insertStmt.setString(2, bid.getItemId());
        insertStmt.setString(3, rs.getString("name"));
        insertStmt.setString(4, bid.getBidderId());
        insertStmt.setString(5, bid.getBidderName());
        insertStmt.setDouble(6, bid.getAmount());
        return insertStmt.executeUpdate() > 0;
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
}

