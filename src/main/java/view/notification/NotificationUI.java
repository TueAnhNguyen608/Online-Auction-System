package view.notification;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * NotificationUI – Danh sách thông báo (NHÓM 5).
 * File FXML: /fxml/notification/NotificationUI.fxml
 *
 * Tích hợp server:
 *   GET_NOTIFICATIONS        → client.getNotifications()
 *   MARK_NOTIFICATION_READ   → client.markNotificationRead(id)
 */
public class NotificationUI implements Initializable {

    // ── FXML ────────────────────────────────────────────────────
    @FXML private Label    lblUnreadDot, lblPageInfo;
    @FXML private Button   btnTabDanhSach, btnTabCaiDat, btnQuayLai;
    @FXML private ComboBox<String> cboLoai, cboTrangThai, cboThoiGian, cboPageSize;
    @FXML private DatePicker dpTuNgay, dpDenNgay;
    @FXML private Button   btnDatLai, btnLoc;
    @FXML private TextField txtSearch;
    @FXML private VBox     vboxNotifications;
    @FXML private HBox     paginationBox;
    @FXML private Button   btnPageFirst, btnPageLast;

    // ── Styles ──────────────────────────────────────────────────
    private static final String TAB_ACTIVE =
            "-fx-background-color: #F5C518; -fx-text-fill: #0A1929; -fx-font-weight: bold;" +
                    " -fx-font-size: 13px; -fx-padding: 8 18 8 14; -fx-background-radius: 4 4 0 0;" +
                    " -fx-border-color: #F5C518; -fx-border-radius: 4 4 0 0; -fx-border-width: 1; -fx-cursor: hand;";
    private static final String TAB_INACTIVE =
            "-fx-background-color: #0e1c40; -fx-text-fill: #CCDFF5; -fx-font-weight: bold;" +
                    " -fx-font-size: 13px; -fx-padding: 8 18 8 14; -fx-background-radius: 4 4 0 0;" +
                    " -fx-border-color: #1b2f5e; -fx-border-radius: 4 4 0 0; -fx-border-width: 1; -fx-cursor: hand;";
    private static final String PAGE_NUM_NORMAL =
            "-fx-background-color: #162550; -fx-background-radius: 4; -fx-border-color: #2A5090;" +
                    " -fx-border-radius: 4; -fx-border-width: 1; -fx-text-fill: #CCDFF5; -fx-font-size: 12px;" +
                    " -fx-pref-width: 30; -fx-pref-height: 30; -fx-cursor: hand;";
    private static final String PAGE_NUM_ACTIVE =
            "-fx-background-color: #F5C518; -fx-background-radius: 4; -fx-border-color: #F5C518;" +
                    " -fx-border-radius: 4; -fx-border-width: 1; -fx-text-fill: #0c1535; -fx-font-size: 12px;" +
                    " -fx-pref-width: 30; -fx-pref-height: 30; -fx-cursor: hand;";

    // ── State ───────────────────────────────────────────────────
    private int currentPage = 1;
    private int pageSize    = 10;
    private List<NotifItem> allItems     = new ArrayList<>();
    private List<NotifItem> filteredItems = new ArrayList<>();

    // ── Model ────────────────────────────────────────────────────
    public static class NotifItem {
        public final int    id;
        public final String type, title, message, time;
        public boolean read;
        public NotifItem(int id, String type, String title, String message, String time, boolean read) {
            this.id = id; this.type = type; this.title = title;
            this.message = message; this.time = time; this.read = read;
        }
    }

    // ── Icon mapping theo loại ───────────────────────────────────
    private String getIconPath(String type) {
        return switch (type) {
            case "WIN"        -> "/icons/thanhtoanthanhcong.png";
            case "OUTBID"     -> "/icons/conguoitrcaohon.png";
            case "ENDING"     -> "/icons/sapketthuc.png";
            case "NEW"        -> "/icons/phiendaugiamoi.png";
            case "PAY_OK"     -> "/icons/thanhtoanthanhcong.png";
            case "PAY_FAIL"   -> "/icons/thanhtoanthatbai.png";
            case "PAY_REMIND" -> "/icons/xacnhanthanhtoan.png";
            case "WARNING"    -> "/icons/canhbao.png";
            default           -> "/icons/thongbao.png";
        };
    }

    private String getIconBg(String type) {
        return switch (type) {
            case "WIN"        -> "#22c55e";
            case "OUTBID"     -> "#3b82f6";
            case "ENDING"     -> "#8b5cf6";
            case "NEW"        -> "#0ea5e9";
            case "PAY_OK"     -> "#22c55e";
            case "PAY_FAIL"   -> "#ef4444";
            case "PAY_REMIND" -> "#f59e0b";
            case "WARNING"    -> "#f59e0b";
            default           -> "#6b7280";
        };
    }

    // ── Initialize ───────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupComboBoxes();
        setupHovers();
        loadSampleData();
        applyFilter();
    }

    // ── ComboBox setup ───────────────────────────────────────────
    private void setupComboBoxes() {
        cboLoai.getItems().addAll("Tất cả","Phiên đấu giá kết thúc","Xác nhận thanh toán",
                "Có người trả giá cao hơn","Sắp kết thúc phiên đấu giá","Phiên đấu giá mới",
                "Thanh toán thành công","Thanh toán thất bại","Cảnh báo tài khoản");
        cboTrangThai.getItems().addAll("Tất cả","Chưa đọc","Đã đọc");
        cboThoiGian.getItems().addAll("7 ngày gần đây","30 ngày gần đây","3 tháng gần đây","Tất cả");
        cboPageSize.getItems().addAll("5","10","20","50");

        styleComboBox(cboLoai);
        styleComboBox(cboTrangThai);
        styleComboBox(cboThoiGian);
        styleComboBox(cboPageSize);
    }

    private void styleComboBox(ComboBox<String> cb) {
        cb.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-text-fill: #FFFFFF; -fx-background-color: #162550; -fx-font-size: 13px;");
            }
            { setOnMouseEntered(e -> setStyle("-fx-text-fill: #FFFFFF; -fx-background-color: #1E4268; -fx-font-size: 13px;"));
                setOnMouseExited(e  -> setStyle("-fx-text-fill: #FFFFFF; -fx-background-color: #162550; -fx-font-size: 13px;")); }
        });
        cb.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill: #FFFFFF; -fx-background-color: transparent; -fx-font-size: 13px;");
            }
        });
    }

    // ── Sample data ──────────────────────────────────────────────
    private void loadSampleData() {
        allItems.clear();
        allItems.add(new NotifItem(1,"WIN",    "Phiên đấu giá kết thúc",
                "Phiên đấu giá \"iPhone 15 Pro Max 1TB\" đã kết thúc. Bạn là người thắng với mức giá 24.500.000 đ.",
                "06/06/2024 14:30", true));
        allItems.add(new NotifItem(2,"PAY_REMIND","Xác nhận thanh toán",
                "Vui lòng thanh toán số tiền 24.500.000 đ cho sản phẩm \"iPhone 15 Pro Max 1TB\" trước 07/06/2024.",
                "06/06/2024 14:25", false));
        allItems.add(new NotifItem(3,"OUTBID",  "Có người trả giá cao hơn",
                "Nguyễn Văn B đã trả giá 24.000.000 đ cho sản phẩm \"iPhone 15 Pro Max 1TB\".",
                "06/06/2024 14:20", false));
        allItems.add(new NotifItem(4,"ENDING",  "Sắp kết thúc phiên đấu giá",
                "Phiên đấu giá \"iPhone 15 Pro Max 1TB\" sẽ kết thúc sau 5 phút nữa.",
                "06/06/2024 14:15", true));
        allItems.add(new NotifItem(5,"NEW",     "Phiên đấu giá mới",
                "Có phiên đấu giá mới cho sản phẩm \"MacBook Pro M3 14 inch 512GB\".",
                "06/06/2024 13:10", false));
        allItems.add(new NotifItem(6,"PAY_OK",  "Thanh toán thành công",
                "Bạn đã thanh toán thành công cho sản phẩm \"AirPods Pro 2nd Generation\".",
                "05/06/2024 16:45", true));
        allItems.add(new NotifItem(7,"PAY_FAIL","Thanh toán thất bại",
                "Thanh toán cho sản phẩm \"iPad Air 5 64GB\" không thành công. Vui lòng thử lại.",
                "05/06/2024 15:30", false));
        allItems.add(new NotifItem(8,"WARNING", "Cảnh báo tài khoản",
                "Tài khoản của bạn đã đăng nhập từ thiết bị mới.",
                "05/06/2024 10:20", true));
        // TODO: thay bằng SessionManager.getClient().getNotifications()
    }

    // ── Build notification rows ──────────────────────────────────
    private void buildNotificationList() {
        vboxNotifications.getChildren().clear();
        int from = (currentPage - 1) * pageSize;
        int to   = Math.min(from + pageSize, filteredItems.size());
        List<NotifItem> page = filteredItems.subList(from, to);

        for (int i = 0; i < page.size(); i++) {
            NotifItem item = page.get(i);
            vboxNotifications.getChildren().add(buildRow(item, i));
        }

        updatePageInfo();
        buildPagination();
    }

    private HBox buildRow(NotifItem item, int idx) {
        HBox row = new HBox(14);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        String bg = (idx % 2 == 0) ? "#0e1c40" : "#11224a";
        String normalStyle = "-fx-background-color: " + bg + "; -fx-padding: 14 20 14 20; -fx-border-color: #1b2f5e; -fx-border-width: 0 0 1 0; -fx-cursor: hand;";
        String hoverStyle  = "-fx-background-color: #162550; -fx-padding: 14 20 14 20; -fx-border-color: #1b2f5e; -fx-border-width: 0 0 1 0; -fx-cursor: hand;";
        row.setStyle(normalStyle);
        row.setOnMouseEntered(e -> row.setStyle(hoverStyle));
        row.setOnMouseExited(e  -> row.setStyle(normalStyle));
        row.setOnMouseClicked(e -> markAsRead(item, row));

        // Icon circle
        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane();
        iconWrap.setMinSize(42, 42); iconWrap.setMaxSize(42, 42);
        iconWrap.setStyle("-fx-background-color: " + getIconBg(item.type) + "; -fx-background-radius: 21;");

        // Clip tròn để xóa khung vuông của ảnh PNG
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(11);
        clip.setCenterX(11); clip.setCenterY(11);

        ImageView iv = new ImageView();
        iv.setFitWidth(22); iv.setFitHeight(22);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setClip(clip); // [FIX] cắt bo tròn
        try {
            URL u = getClass().getResource(getIconPath(item.type));
            if (u != null) iv.setImage(new Image(u.toExternalForm()));
        } catch (Exception ignored) {}
        iconWrap.getChildren().add(iv);

        // Content
        VBox content = new VBox(4);
        HBox.setHgrow(content, javafx.scene.layout.Priority.ALWAYS);
        Label title = new Label(item.title);
        title.setStyle("-fx-text-fill: " + (item.read ? "#8AADD0" : "#FFFFFF") +
                "; -fx-font-size: 13px; -fx-font-weight: " + (item.read ? "normal" : "bold") + ";");
        Label msg = new Label(item.message);
        msg.setWrapText(true);
        msg.setStyle("-fx-text-fill: #8AADD0; -fx-font-size: 12px;");
        content.getChildren().addAll(title, msg);

        // Right: time + status
        VBox right = new VBox(4);
        right.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
        Label time = new Label(item.time);
        time.setStyle("-fx-text-fill: #4A6A8A; -fx-font-size: 11px;");
        Label status = new Label(item.read ? "• Đã đọc" : "• Chưa đọc");
        status.setStyle("-fx-text-fill: " + (item.read ? "#22c55e" : "#F5C518") + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        // Menu button "..."
        Button btnMenu = new Button("···");
        btnMenu.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #8AADD0; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");

        right.getChildren().addAll(time, status);
        row.getChildren().addAll(iconWrap, content, right, btnMenu);
        return row;
    }

    // ── Đánh dấu đã đọc ─────────────────────────────────────────
    private void markAsRead(NotifItem item, HBox row) {
        if (!item.read) {
            item.read = true;
            buildNotificationList();
            updateBadge();
            // TODO: SessionManager.getClient().markNotificationRead(item.id)
        }
    }

    private void updateBadge() {
        // TODO: cập nhật badge ở MainDashboardUI nếu cần
    }

    // ── Pagination ───────────────────────────────────────────────
    private void updatePageInfo() {
        int from  = Math.min((currentPage - 1) * pageSize + 1, filteredItems.size());
        int to    = Math.min(currentPage * pageSize, filteredItems.size());
        int total = filteredItems.size();
        lblPageInfo.setText("Hiển thị " + from + " đến " + to + " trong tổng số " + total + " thông báo");
    }

    private void buildPagination() {
        paginationBox.getChildren().clear();
        int pages = Math.max(1, (int) Math.ceil((double) filteredItems.size() / pageSize));
        for (int i = 1; i <= pages; i++) {
            final int pg = i;
            Button b = new Button(String.valueOf(i));
            b.setStyle(i == currentPage ? PAGE_NUM_ACTIVE : PAGE_NUM_NORMAL);
            b.setOnAction(e -> { currentPage = pg; buildNotificationList(); });
            if (i != currentPage) {
                b.setOnMouseEntered(ev -> b.setStyle(PAGE_NUM_ACTIVE.replace("#F5C518","#1E4268").replace("#0c1535","#FFFFFF")));
                b.setOnMouseExited(ev  -> b.setStyle(PAGE_NUM_NORMAL));
            }
            paginationBox.getChildren().add(b);
        }
    }

    // ── Filter ───────────────────────────────────────────────────
    private void applyFilter() {
        String loai      = cboLoai.getValue();
        String trangThai = cboTrangThai.getValue();
        String search    = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase();

        filteredItems = allItems.stream().filter(item -> {
            boolean matchLoai = loai == null || loai.equals("Tất cả") || item.title.contains(loai);
            boolean matchTT   = trangThai == null || trangThai.equals("Tất cả")
                    || (trangThai.equals("Chưa đọc") && !item.read)
                    || (trangThai.equals("Đã đọc") && item.read);
            boolean matchSearch = search.isEmpty()
                    || item.title.toLowerCase().contains(search)
                    || item.message.toLowerCase().contains(search);
            return matchLoai && matchTT && matchSearch;
        }).collect(Collectors.toList());

        currentPage = 1;
        buildNotificationList();
        updateBadge();
    }

    // ── Hover ────────────────────────────────────────────────────
    private void setupHovers() {
        String backN = "-fx-background-color: #162550; -fx-background-radius: 6; -fx-border-color: #2A5090; -fx-border-radius: 6; -fx-border-width: 1; -fx-text-fill: #CCDFF5; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 7 14 7 12; -fx-cursor: hand;";
        String backH = "-fx-background-color: #F5C518; -fx-background-radius: 6; -fx-border-color: #F5C518; -fx-border-radius: 6; -fx-border-width: 1; -fx-text-fill: #0A1929; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 7 14 7 12; -fx-cursor: hand;";
        addHover(btnQuayLai, backN, backH);

        String locN = "-fx-background-color: #F5C518; -fx-background-radius: 6; -fx-border-color: #F5C518; -fx-border-radius: 6; -fx-border-width: 1; -fx-text-fill: #0c1535; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 0 8 0; -fx-cursor: hand;";
        String locH = "-fx-background-color: #d4aa00; -fx-background-radius: 6; -fx-border-color: #d4aa00; -fx-border-radius: 6; -fx-border-width: 1; -fx-text-fill: #0c1535; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 0 8 0; -fx-cursor: hand;";
        addHover(btnLoc, locN, locH);

        addHover(btnTabCaiDat,
                TAB_INACTIVE,
                TAB_INACTIVE.replace("#0e1c40","#F5C518").replace("#CCDFF5","#0A1929").replace("#1b2f5e","#F5C518"));
    }

    private void addHover(Button b, String n, String h) {
        b.setOnMouseEntered(e -> b.setStyle(h));
        b.setOnMouseExited(e  -> b.setStyle(n));
    }

    // ── Handlers ─────────────────────────────────────────────────
    @FXML void onTabDanhSach() { /* đang ở đây */ }
    @FXML void onTabCaiDat()   { /* TODO: cài đặt thông báo */ }

    @FXML void onQuayLai() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/bidding/MainDashboardUI.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) vboxNotifications.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @FXML void onDatLai() {
        cboLoai.setValue("Tất cả");
        cboTrangThai.setValue("Tất cả");
        cboThoiGian.setValue("7 ngày gần đây");
        txtSearch.clear();
        dpTuNgay.setValue(null);
        dpDenNgay.setValue(null);
        applyFilter();
    }

    @FXML void onLoc()    { applyFilter(); }
    @FXML void onSearch() { applyFilter(); }

    @FXML void onFirstPage() { currentPage = 1; buildNotificationList(); }
    @FXML void onLastPage()  {
        int pages = Math.max(1, (int) Math.ceil((double) filteredItems.size() / pageSize));
        currentPage = pages;
        buildNotificationList();
    }
}