package view.bidding;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * MainDashboardUI – Màn hình trang chủ chính.
 * Nhóm 3 – view/bidding/
 *
 * Cách mở từ LoginController:
 *     MainDashboardUI.show(stage, "TênUser");
 */
public class MainDashboardUI implements Initializable {

    /* ── FXML ──────────────────────────────────── */
    @FXML private Label usernameLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label statTotal;
    @FXML private Label statSessions;
    @FXML private Label statValue;
    @FXML private Label statEnding;
    @FXML private Button btnNotification; // [THÊM]
    @FXML private Label  lblNotifCount;   // [THÊM]
    @FXML private Label  statTotalSub;    // [THÊM]
    @FXML private Label  statSessionsSub; // [THÊM]
    @FXML private Label  statEndingSub;   // [THÊM]

    /* ── Username truyền từ Login ──────────────── */
    private static String currentUser = "Admin";

    public static void setCurrentUser(String name) {
        if (name != null && !name.isBlank()) currentUser = name;
    }

    /* ══════════════════════════════════════════════
       INITIALIZE
       ══════════════════════════════════════════════ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        usernameLabel.setText(currentUser);
        welcomeLabel.setText("Xin chào, " + currentUser + "!");

        // [THÊM] Tính toán thống kê từ dữ liệu thực (cùng data với ProductListPanel)
        loadStats();

        // [THÊM] Cập nhật số thông báo – thay 4 bằng số thực từ server
        updateNotifCount(4);
    }

    /* ══════════════════════════════════════════════
       XỬ LÝ NÚT
       ══════════════════════════════════════════════ */

    @FXML
    private void onSanPham() {
        navigate("/fxml/bidding/ProductListPanel.fxml", "Sản phẩm");
    }

    @FXML
    private void onAutoBid() {
        navigate("/fxml/bidding/AutoBidPanel.fxml", "Đấu giá tự động");
    }

    @FXML
    private void onLichSu() {
        navigate("/fxml/history/MyBidsHistoryUI.fxml", "Lịch sử đấu giá");
    }

    // [THÊM] Tính thống kê từ dữ liệu sản phẩm
    // Khi tích hợp server: thay bằng SessionManager.getClient().getStats()
    private void loadStats() {
        // Dữ liệu giống ProductListPanel – đồng bộ 1 chỗ khi có server
        long[] prices = {
                25_000_000L, 22_000_000L, 20_000_000L, 15_000_000L,
                18_000_000L, 45_000_000L, 35_000_000L, 28_000_000L,
                32_000_000L,  5_000_000L,  4_500_000L,  6_000_000L,
                15_000_000L,  7_000_000L,  3_000_000L,  2_000_000L,
                28_000_000L, 22_000_000L
        };
        long[] days = {7, 5, 10, 6, 8, 12, 9, 7, 11, 4, 3, 5, 8, 6, 10, 15, 20, 18};

        int total   = prices.length;
        int active  = 0;   // đang diễn ra (chưa kết thúc)
        int ending  = 0;   // kết thúc trong 24 giờ (days == 0)
        long totalValue = 0;

        for (int i = 0; i < total; i++) {
            totalValue += prices[i];
            if (days[i] > 0) active++;
            if (days[i] == 0) ending++;
        }

        // Cập nhật labels
        statTotal.setText(String.valueOf(total));
        statTotalSub.setText("+" + total + " sản phẩm đang đấu giá");

        statSessions.setText(String.valueOf(total));
        statSessionsSub.setText("Đang diễn ra: " + active);

        statValue.setText(String.format("%,d", totalValue).replace(",", ".") + " đ");

        statEnding.setText(String.valueOf(ending));
        statEndingSub.setText(ending > 0 ? "Trong 24 giờ tới" : "Không có phiên nào");
    }

    // [THÊM] Mở màn hình thông báo
    @FXML
    private void onNotification() {
        navigate("/fxml/notification/NotificationUI.fxml", "Thông báo");
    }

    // [THÊM] Cập nhật số thông báo chưa đọc
    private void updateNotifCount(int count) {
        if (count <= 0) {
            lblNotifCount.setText("");
        } else if (count <= 9) {
            lblNotifCount.setText("(" + count + ")");
            lblNotifCount.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
        } else {
            lblNotifCount.setText("(9+)");
            lblNotifCount.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");
        }
    }

    /* ══════════════════════════════════════════════
       NAVIGATE
       ══════════════════════════════════════════════ */
    private void navigate(String fxmlPath, String title) {
        try {
            URL res = getClass().getResource(fxmlPath);
            if (res == null) {
                info("Màn hình \"" + title + "\" đang được phát triển.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(res);
            Parent root = loader.load();
            Stage stage = (Stage) usernameLabel.getScene().getWindow();
            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            URL css = getClass().getResource("/css/bidding_style.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title + " – Hệ thống đấu giá");
        } catch (Exception e) {
            info("Lỗi mở màn hình: " + e.getMessage());
        }
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Thông báo");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    /* ══════════════════════════════════════════════
       STATIC ENTRY – gọi từ LoginController
       ══════════════════════════════════════════════ */
    public static void show(Stage stage, String username) {
        try {
            setCurrentUser(username);
            URL fxml = MainDashboardUI.class
                    .getResource("/fxml/bidding/MainDashboardUI.fxml");
            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();
            Scene scene = new Scene(root, 1280, 780);
            URL css = MainDashboardUI.class.getResource("/css/bidding_style.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Hệ thống đấu giá");
            stage.setMinWidth(960);
            stage.setMinHeight(640);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}