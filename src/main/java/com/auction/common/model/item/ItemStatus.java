package com.auction.common.model.item;

public enum ItemStatus {
    PENDING("Chờ duyệt"),
    ACTIVE("Đang đấu giá"),
    ENDED("Đã kết thúc"),
    CANCELLED("Đã hủy");

    private final String displayName;

    ItemStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ItemStatus fromString(String status) {
        for (ItemStatus s : ItemStatus.values()) {
            if (s.name().equalsIgnoreCase(status)) {
                return s;
            }
        }
        return PENDING;
    }

    @Override
    public String toString() {
        return displayName;
    }
}