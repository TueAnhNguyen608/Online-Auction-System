package common.model.user;

import common.Entity;
import java.time.LocalDateTime;
import java.util.UUID;

public class User extends Entity {

    private String username;
    private String password;
    private String email;
    private UserRole role;
    private String fullName;
    private String phone;
    private double balance;
    private LocalDateTime createdAt;
    private boolean isActive;

    // ========== CONSTRUCTORS ==========

    public User() {
        super();
        this.setId(UUID.randomUUID().toString());
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
        this.balance = 0.0;
        this.role = UserRole.BIDDER; // default role
    }

    public User(String username, String password, String email, UserRole role) {
        this();
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }

    // ========== GETTERS & SETTERS ==========

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public void setRole(String role) {
        this.role = UserRole.fromString(role);
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // ========== HELPER METHODS ==========

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isBidder() {
        return role == UserRole.BIDDER;
    }

    public boolean isSeller() {
        return role == UserRole.SELLER;
    }

    // ========== TO STRING ==========

    @Override
    public String toString() {
        return String.format("User{id='%s', username='%s', role=%s, fullName='%s'}",
                getId(), username, role, fullName);
    }
}