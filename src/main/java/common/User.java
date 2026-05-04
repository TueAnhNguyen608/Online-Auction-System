package common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class User extends Entity {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String email;
    private String role;  // "ADMIN" or "BIDDER"
    private double balance;
    private LocalDateTime createdAt;
    private boolean isActive;

    public User() {
        this.setId(java.util.UUID.randomUUID().toString());
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
        this.balance = 0.0;
    }

    public User(String username, String password, String email, String role) {
        this();
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isAdmin() { return "ADMIN".equals(role); }
    public boolean isBidder() { return "BIDDER".equals(role); }

    @Override
    public String toString() {
        return "User{" + username + ", role=" + role + '}';
    }
}