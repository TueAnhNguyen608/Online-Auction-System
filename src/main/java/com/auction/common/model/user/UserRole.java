package com.auction.common.model.user;

public enum UserRole {
    ADMIN("Quản trị viên"),
    BIDDER("Người đấu giá"),
    SELLER("Người bán");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static UserRole fromString(String role) {
        if (role == null) return BIDDER;
        for (UserRole r : UserRole.values()) {
            if (r.name().equalsIgnoreCase(role)) {
                return r;
            }
        }
        return BIDDER;
    }

    @Override
    public String toString() {
        return displayName;
    }
}