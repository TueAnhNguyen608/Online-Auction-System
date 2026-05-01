package utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;

/**
 * ĐỊNH NGHĨA: Công cụ hiển thị thông báo (Lỗi, Thành công).
 * TẠI SAO: Dùng Platform.runLater để đảm bảo thông báo luôn hiện lên an toàn,
 * không gây treo ứng dụng khi luồng mạng đang chạy.
 */
public class DialogUtils {

    // Hàm hiển thị lỗi (Màu đỏ)
    public static void showError(String title, String content) {
        // Platform.runLater: "Lệnh" cho JavaFX ưu tiên hiển thị cái này lên màn hình
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null); // Không cần tiêu đề phụ cho gọn
            alert.setContentText(content);
            alert.showAndWait(); // Hiện và đợi người dùng bấm OK
        });
    }

    // Hàm hiển thị thông báo thành công (Màu xanh lá/Xanh dương)
    public static void showInfo(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}