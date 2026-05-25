package com.auction.server.repository;

import com.auction.common.model.user.User;
import com.auction.common.model.user.UserRole;
import com.auction.common.util.PasswordUtil;
import com.auction.server.pool.DatabaseConnectionPool;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserRepository {

    private static volatile UserRepository instance;
    private final DatabaseConnectionPool pool;

    private UserRepository() {
        this.pool = DatabaseConnectionPool.getInstance();
    }

    public static UserRepository getInstance() {
        if (instance == null) {
            synchronized (UserRepository.class) {
                if (instance == null) {
                    instance = new UserRepository();
                }
            }
        }
        return instance;
    }

    public User findByUsernameAndPassword(String username, String password) throws SQLException {
        User user = findByUsername(username);
        if (user != null && PasswordUtil.verify(password, user.getPassword())) {
            return user;
        }
        return null;
    }

    public User findById(String id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
            return null;
        }
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
            return null;
        }
    }

    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        try (Connection conn = pool.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) users.add(mapRow(rs));
        }
        return users;
    }

    public List<User> getAllUsers() throws SQLException {
        return findAll();
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = pool.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
            return 0;
        }
    }

    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
            return false;
        }
    }

    public boolean save(User user) throws SQLException {
        String sql = "INSERT INTO users(id, username, password, email, role, full_name, phone, balance) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getRole() != null ? user.getRole().name() : "BIDDER");
            stmt.setString(6, user.getFullName());
            stmt.setString(7, user.getPhone());
            stmt.setDouble(8, user.getBalance());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean update(User user) throws SQLException {
        String sql = "UPDATE users SET email = ?, full_name = ?, phone = ? WHERE id = ?";
        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getFullName());
            stmt.setString(3, user.getPhone());
            stmt.setString(4, user.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateProfile(String userId, String email, String fullName, String phone) throws SQLException {
        String sql = "UPDATE users SET email = ?, full_name = ?, phone = ? WHERE id = ?";
        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, fullName);
            stmt.setString(3, phone);
            stmt.setString(4, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updatePassword(String userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPassword);
            stmt.setString(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean verifyPassword(String userId, String password) throws SQLException {
        User user = findById(userId);
        if (user == null) return false;
        return PasswordUtil.verify(password, user.getPassword());
    }

    public boolean deleteById(String id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = pool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getString("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));
        user.setRole(rs.getString("role"));
        user.setBalance(rs.getDouble("balance"));
        user.setFullName(rs.getString("full_name"));
        user.setPhone(rs.getString("phone"));
        return user;
    }
}