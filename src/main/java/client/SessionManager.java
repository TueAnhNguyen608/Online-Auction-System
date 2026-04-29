package client;

import common.User;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public int getCurrentUserId() {
        // User dùng String id (UUID) nên cần xử lý
        return currentUser != null ? currentUser.getId().hashCode() : 0;
    }

    public String getCurrentRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }

    public boolean isAdmin() {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());
    }

    public boolean isBidder() {
        return currentUser != null && "BIDDER".equalsIgnoreCase(currentUser.getRole());
    }

    public void logout() {
        currentUser = null;
    }
}