package com.auction.server.service;

import com.auction.common.model.exception.AuthenticationException;
import com.auction.common.model.user.User;
import com.auction.server.repository.UserRepository;
import java.sql.SQLException;
import java.util.List;

public class UserService {

    private static volatile UserService instance;
    private final UserRepository userRepo;

    private UserService() {
        this.userRepo = UserRepository.getInstance();
    }

    public static UserService getInstance() {
        if (instance == null) {
            synchronized (UserService.class) {
                if (instance == null) {
                    instance = new UserService();
                }
            }
        }
        return instance;
    }

    public User login(String username, String password) throws AuthenticationException, SQLException {
        if (username == null || username.trim().isEmpty()) {
            throw new AuthenticationException("Tên đăng nhập không được để trống");
        }
        if (password == null || password.isEmpty()) {
            throw new AuthenticationException("Mật khẩu không được để trống");
        }

        System.out.println("🔍 UserService.login: " + username);

        User user = userRepo.findByUsername(username);
        if (user == null) {
            System.out.println("❌ User not found: " + username);
            throw new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu", username);
        }

        // 🔥 SO SÁNH PLAIN TEXT TRỰC TIẾP (KHÔNG HASH)
        if (!password.equals(user.getPassword())) {
            System.out.println("❌ Wrong password for: " + username);
            throw new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu", username);
        }

        System.out.println("✅ User found: " + username);
        return user;
    }

    public boolean register(User user) throws SQLException {
        if (userRepo.existsByUsername(user.getUsername())) {
            return false;
        }
        // 🔥 LƯU PLAIN TEXT (KHÔNG HASH)
        // user.setPassword(PasswordUtil.hash(user.getPassword()));
        return userRepo.save(user);
    }

    public List<User> getAllUsers() throws SQLException {
        return userRepo.findAll();
    }

    public User getUserById(String userId) throws SQLException {
        return userRepo.findById(userId);
    }

    public User getUserByUsername(String username) throws SQLException {
        return userRepo.findByUsername(username);
    }

    public boolean updateProfile(String userId, String email, String fullName, String phone) throws SQLException {
        return userRepo.updateProfile(userId, email, fullName, phone);
    }

    public boolean changePassword(String userId, String oldPassword, String newPassword) throws SQLException {
        User user = userRepo.findById(userId);
        if (user == null) return false;

        // 🔥 SO SÁNH PLAIN TEXT
        if (!oldPassword.equals(user.getPassword())) {
            return false;
        }

        // 🔥 LƯU PLAIN TEXT (KHÔNG HASH)
        return userRepo.updatePassword(userId, newPassword);
    }

    public boolean deleteUser(String userId) throws SQLException {
        return userRepo.deleteById(userId);
    }

    public int getTotalUsers() throws SQLException {
        return userRepo.count();
    }
}