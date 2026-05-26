package com.auction.common.constant;

public final class AuctionConstants {

    // ========== SERVER CONFIGURATION ==========
    public static final int DEFAULT_SERVER_PORT = 9999;
    public static final String DEFAULT_SERVER_HOST = "localhost";
    public static final int MAX_THREAD_POOL_SIZE = 50;
    public static final int CONNECTION_TIMEOUT_SECONDS = 30;

    // ========== AUCTION RULES ==========
    public static final int SNIPING_WINDOW_SECONDS = 30;
    public static final int EXTENSION_SECONDS = 60;
    public static final double DEFAULT_STEP_PERCENT = 0.05;
    public static final double MINIMUM_BID_AMOUNT = 1000;

    // 🔥 THÊM: Rate limiting
    public static final int MAX_REQUESTS_PER_MINUTE = 60;
    public static final int RATE_LIMIT_WINDOW_SECONDS = 60;

    // 🔥 THÊM: Session timeout
    public static final int SESSION_TIMEOUT_MINUTES = 30;
    public static final int HEARTBEAT_INTERVAL_SECONDS = 30;
    public static final int HEARTBEAT_TIMEOUT_SECONDS = 60;

    // ========== VALIDATION RULES ==========
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_ITEM_NAME_LENGTH = 200;
    public static final int MAX_DESCRIPTION_LENGTH = 1000;
    public static final int MIN_AUCTION_DURATION_MINUTES = 5;
    public static final int MAX_AUCTION_DURATION_DAYS = 30;

    // 🔥 THÊM: Validation cho bid
    public static final double MAX_BID_AMOUNT = 1_000_000_000_000L;  // 1 nghìn tỷ
    public static final double MIN_BID_AMOUNT = 1000;

    // ========== ITEM STATUS ==========
    public static final String ITEM_STATUS_PENDING = "PENDING";
    public static final String ITEM_STATUS_ACTIVE = "ACTIVE";
    public static final String ITEM_STATUS_ENDED = "ENDED";
    public static final String ITEM_STATUS_CANCELLED = "CANCELLED";

    // ========== USER ROLES ==========
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_BIDDER = "BIDDER";
    public static final String ROLE_SELLER = "SELLER";

    // ========== DATABASE ==========
    public static final String DB_URL = "jdbc:mysql://localhost:3306/auction_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&useUnicode=true&characterEncoding=utf8";
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "Nhom7123456";
    public static final int DB_POOL_SIZE = 10;
    public static final int DB_MIN_POOL_SIZE = 5;

    // ========== SCHEDULER ==========
    public static final int AUCTION_CHECK_INTERVAL_SECONDS = 10;
    public static final int TIME_BROADCAST_INTERVAL_SECONDS = 1;

    // ========== SOCKET ==========
    public static final int SOCKET_TIMEOUT_MS = 30000;
    public static final int HEARTBEAT_INTERVAL_MS = 30000;

    // ========== MESSAGES ==========
    public static final String MSG_LOGIN_SUCCESS = "Đăng nhập thành công";
    public static final String MSG_LOGIN_FAILED = "Sai tên đăng nhập hoặc mật khẩu";
    public static final String MSG_BID_SUCCESS = "Đấu giá thành công";
    public static final String MSG_BID_TOO_LOW = "Giá đặt quá thấp";
    public static final String MSG_AUCTION_ENDED = "Phiên đấu giá đã kết thúc";
    public static final String MSG_ITEM_NOT_FOUND = "Không tìm thấy sản phẩm";
    public static final String MSG_PERMISSION_DENIED = "Bạn không có quyền thực hiện hành động này";
    public static final String MSG_RATE_LIMIT = "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau.";
    public static final String MSG_SESSION_EXPIRED = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";

    private AuctionConstants() {
        throw new UnsupportedOperationException("Cannot instantiate constants class");
    }
}