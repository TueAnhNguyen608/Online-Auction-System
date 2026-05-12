package view.bidding;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * MainDashboardController – Điều phối navigate toàn bộ giao diện.
 * Nhóm 3 – view/bidding/
 *
 * Cách dùng:
 *   MainDashboardController.navigateTo(stage, Screen.PRODUCT_LIST);
 *
 * Khi tích hợp server, truyền thêm dữ liệu qua các setter của controller:
 *   FXMLLoader loader = MainDashboardController.loadScreen(Screen.PRODUCT_DETAIL);
 *   ProductDetailPanel ctrl = loader.getController();
 *   ctrl.loadProduct(id, name, price, seller, remainingSeconds, imagePath);
 */
public class MainDashboardController {

    // ── Danh sách tất cả màn hình ────────────────────────────────
    public enum Screen {
        MAIN_DASHBOARD   ("/fxml/bidding/MainDashboardUI.fxml"),
        PRODUCT_LIST     ("/fxml/bidding/ProductListPanel.fxml"),
        PRODUCT_DETAIL   ("/fxml/bidding/ProductDetailPanel.fxml"),
        AUTO_BID         ("/fxml/bidding/AutoBidPanel.fxml"),
        NOTIFICATION     ("/fxml/notification/NotificationUI.fxml"),
        // Nhóm 4 – Lịch sử
        BID_HISTORY      ("/fxml/history/MyBidsHistoryUI.fxml");

        public final String fxmlPath;
        Screen(String path) { this.fxmlPath = path; }
    }

    private static final String CSS_PATH = "/css/bidding_style.css";

    // ── Navigate đơn giản (không cần truyền dữ liệu) ─────────────
    /**
     * Navigate sang màn hình mới, giữ nguyên kích thước cửa sổ.
     * @param stage  Stage hiện tại (lấy từ bất kỳ node nào: node.getScene().getWindow())
     * @param screen Màn hình muốn chuyển sang
     */
    public static void navigateTo(Stage stage, Screen screen) {
        try {
            URL res = MainDashboardController.class.getResource(screen.fxmlPath);
            if (res == null) {
                showDevAlert(stage, screen);
                return;
            }
            FXMLLoader loader = new FXMLLoader(res);
            Parent root = loader.load();

            double w = stage.getScene().getWidth();
            double h = stage.getScene().getHeight();
            Scene scene = new Scene(root, w, h);

            URL css = MainDashboardController.class.getResource(CSS_PATH);
            if (css != null) scene.getStylesheets().add(css.toExternalForm());

            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Navigate kèm truyền dữ liệu sản phẩm ────────────────────
    /**
     * Mở ProductDetailPanel với dữ liệu sản phẩm cụ thể.
     * Gọi từ ProductListPanel khi user double-click vào 1 hàng.
     */
    public static void navigateToDetail(Stage stage,
                                        int id, String name, long price,
                                        String seller, long remainingSeconds,
                                        String imagePath) {
        try {
            URL res = MainDashboardController.class
                    .getResource(Screen.PRODUCT_DETAIL.fxmlPath);
            if (res == null) return;

            FXMLLoader loader = new FXMLLoader(res);
            Parent root = loader.load();

            ProductDetailPanel ctrl = loader.getController();
            ctrl.loadProduct(id, name, price, seller, remainingSeconds, imagePath);

            double w = stage.getScene().getWidth();
            double h = stage.getScene().getHeight();
            Scene scene = new Scene(root, w, h);

            URL css = MainDashboardController.class.getResource(CSS_PATH);
            if (css != null) scene.getStylesheets().add(css.toExternalForm());

            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Load FXML và trả về loader (dùng khi cần lấy controller) ─
    /**
     * Load FXML mà không navigate ngay.
     * Dùng khi cần lấy controller để set data trước khi hiển thị.
     *
     * Ví dụ:
     *   FXMLLoader loader = MainDashboardController.loadScreen(Screen.AUTO_BID);
     *   AutoBidPanel ctrl = loader.getController();
     *   // set data...
     *   stage.setScene(new Scene(loader.getRoot(), w, h));
     */
    public static FXMLLoader loadScreen(Screen screen) throws Exception {
        URL res = MainDashboardController.class.getResource(screen.fxmlPath);
        if (res == null) throw new Exception("Không tìm thấy FXML: " + screen.fxmlPath);
        FXMLLoader loader = new FXMLLoader(res);
        loader.load();
        return loader;
    }

    // ── Helper: lấy Stage từ bất kỳ node nào ────────────────────
    /**
     * Tiện ích lấy Stage từ bất kỳ node FXML nào.
     * Ví dụ: Stage stage = MainDashboardController.getStage(btnBack);
     */
    public static Stage getStage(javafx.scene.Node node) {
        return (Stage) node.getScene().getWindow();
    }

    // ── Thông báo màn hình đang phát triển ───────────────────────
    private static void showDevAlert(Stage stage, Screen screen) {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        a.setTitle("Thông báo");
        a.setHeaderText(null);
        a.setContentText("Màn hình \"" + screen.name() + "\" đang được phát triển.");
        a.showAndWait();
    }
}