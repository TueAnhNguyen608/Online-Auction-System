package common;

import javafx.scene.paint.Color;

public class AppConfig {
    // ---- CẤU HÌNH KẾT NỐI SERVER ----
    public static final String API_BASE_URL = "http://localhost:8080/api";
    public static final int TIMEOUT_SECONDS = 10;

    // ---- CẤU HÌNH GIAO DIỆN (UI/UX) ----
    public static final Color COLOR_PRIMARY = Color.web("#24416B"); // Xanh Navy chủ đạo
    public static final Color COLOR_BACKGROUND = Color.web("#E8ECEF"); // Nền xám nhạt
    public static final Color COLOR_ERROR = Color.web("#E53935"); // Màu đỏ báo lỗi
}