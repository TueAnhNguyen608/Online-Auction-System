package com.auction.ui.notification;

import com.auction.client.AuctionClient;
import com.auction.common.model.bidding.Bid;
import com.auction.common.model.communication.CommandType;
import com.auction.common.model.communication.ResponseCode;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.common.model.user.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.auction.ui.auth.SessionManager;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class NotificationUI implements Initializable {

    @FXML private Label    lblUnreadDot, lblPageInfo;
    @FXML private Button   btnTabDanhSach, btnTabCaiDat, btnQuayLai;
    @FXML private ComboBox<String> cboLoai, cboTrangThai, cboThoiGian, cboPageSize;
    @FXML private DatePicker dpTuNgay, dpDenNgay;
    @FXML private Button   btnDatLai, btnLoc;
    @FXML private TextField txtSearch;
    @FXML private VBox     vboxNotifications;
    @FXML private HBox     paginationBox;
    @FXML private Button   btnPageFirst, btnPageLast;

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

    private int currentPage = 1;
    private int pageSize    = 10;
    private List<NotifItem> allItems      = new ArrayList<>();
    private List<NotifItem> filteredItems = new ArrayList<>();
    private final Map<String, Boolean> notifSettings = new LinkedHashMap<>();

    public static class NotifItem {
        public final int    id;
        public final String type, title, message, time;
        public boolean read;
        public NotifItem(int id, String type, String title, String message, String time, boolean read) {
            this.id = id; this.type = type; this.title = title;
            this.message = message; this.time = time; this.read = read;
        }
    }

    private String getIconPath(String type) {
        switch (type) {
            case "WIN": return "/icons/thanhtoanthanhcong.png";
            case "OUTBID": return "/icons/conguoitrcaohon.png";
            case "ENDING": return "/icons/sapketthuc.png";
            case "NEW": return "/icons/phiendaugiamoi.png";
            case "PAY_OK": return "/icons/thanhtoanthanhcong.png";
            case "PAY_FAIL": return "/icons/thanhtoanthatbai.png";
            case "PAY_REMIND": return "/icons/xacnhanthanhtoan.png";
            case "WARNING": return "/icons/canhbao.png";
            default: return "/icons/thongbao.png";
        }
    }

    private String getIconBg(String type) {
        switch (type) {
            case "WIN": return "#22c55e";
            case "OUTBID": return "#3b82f6";
            case "ENDING": return "#8b5cf6";
            case "NEW": return "#0ea5e9";
            case "PAY_OK": return "#22c55e";
            case "PAY_FAIL": return "#ef4444";
            case "PAY_REMIND": return "#f59e0b";
            case "WARNING": return "#f59e0b";
            default: return "#6b7280";
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        SessionManager.clearUnreadNotif();
        notifSettings.put("Thông báo khi bị outbid",              true);
        notifSettings.put("Thông báo khi phiên sắp kết thúc",     true);
        notifSettings.put("Thông báo thanh toán",                  true);
        notifSettings.put("Thông báo phiên đấu giá mới",          true);
        notifSettings.put("Thông báo khi thắng đấu giá",          true);
        notifSettings.put("Cảnh báo tài khoản",                   false);

        setupComboBoxes();
        setupHovers();
        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            try {
                AuctionClient client = SessionManager.getClient();
                User user = SessionManager.getCurrentUser();
                if (client == null || user == null) throw new Exception("Chưa đăng nhập");

                List<NotifItem> items = new ArrayList<>();
                int id = 1;

                List<SessionManager.NotifData> rtNotifs =
                        new ArrayList<>(SessionManager.getRealtimeNotifs());
                for (SessionManager.NotifData notif : rtNotifs) {
                    items.add(new NotifItem(id++, notif.type, notif.title,
                            notif.message, notif.time, notif.read));
                }

                ResponseMessage resp = client.sendRequest(CommandType.GET_MY_BID_HISTORY, user.getId());
                if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                    com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                            .registerTypeAdapter(java.time.LocalDateTime.class, new com.auction.common.LocalDateTimeAdapter())
                            .create();
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Bid>>(){}.getType();
                    List<Bid> bids = gson.fromJson(gson.toJson(resp.getData()), listType);
                    if (bids != null) {
                        for (Bid bid : bids) {
                            String time = bid.getBidTime() != null ?
                                    bid.getBidTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
                            String type = bid.isWinning() ? "WIN" : "OUTBID";
                            String title = bid.isWinning() ? "Bạn đang dẫn đầu" : "Lịch sử đặt giá";
                            String msg = "Bạn đã đặt " +
                                    String.format("%,.0f đ", bid.getAmount()).replace(",", ".") +
                                    " cho sản phẩm \"" + (bid.getItemName() != null ? bid.getItemName() : "N/A") + "\"";
                            items.add(new NotifItem(id++, type, title, msg, time, true));
                        }
                    }
                }

                if (!items.isEmpty()) {
                    final List<NotifItem> finalItems = items;
                    Platform.runLater(() -> {
                        allItems.clear();
                        allItems.addAll(finalItems);
                        applyFilter();
                        System.out.println("✅ Loaded " + finalItems.size() + " thông báo");
                    });
                    return;
                }
            } catch (Exception e) {
                System.out.println("⚠ Dùng thông báo mẫu: " + e.getMessage());
            }
            Platform.runLater(() -> {
                loadSampleData();
                applyFilter();
            });
        }).start();
    }

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
    }

    private void buildNotificationList() {
        vboxNotifications.getChildren().clear();
        int from = (currentPage - 1) * pageSize;
        int to   = Math.min(from + pageSize, filteredItems.size());
        List<NotifItem> page = filteredItems.subList(from, to);
        for (int i = 0; i < page.size(); i++) {
            vboxNotifications.getChildren().add(buildRow(page.get(i), i));
        }
        updatePageInfo();
        buildPagination();
    }

    private HBox buildRow(NotifItem item, int idx) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        String bg = (idx % 2 == 0) ? "#0e1c40" : "#11224a";
        String normalStyle = "-fx-background-color: " + bg + "; -fx-padding: 14 20 14 20; -fx-border-color: #1b2f5e; -fx-border-width: 0 0 1 0; -fx-cursor: hand;";
        String hoverStyle  = "-fx-background-color: #162550; -fx-padding: 14 20 14 20; -fx-border-color: #1b2f5e; -fx-border-width: 0 0 1 0; -fx-cursor: hand;";
        row.setStyle(normalStyle);
        row.setOnMouseEntered(e -> row.setStyle(hoverStyle));
        row.setOnMouseExited(e  -> row.setStyle(normalStyle));
        row.setOnMouseClicked(e -> markAsRead(item, row));

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane();
        iconWrap.setMinSize(42, 42); iconWrap.setMaxSize(42, 42);
        iconWrap.setStyle("-fx-background-color: " + getIconBg(item.type) + "; -fx-background-radius: 21;");

        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(11);
        clip.setCenterX(11); clip.setCenterY(11);

        ImageView iv = new ImageView();
        iv.setFitWidth(22); iv.setFitHeight(22);
        iv.setPreserveRatio(true); iv.setSmooth(true); iv.setClip(clip);
        try {
            URL u = getClass().getResource(getIconPath(item.type));
            if (u != null) iv.setImage(new Image(u.toExternalForm()));
        } catch (Exception ignored) {}
        iconWrap.getChildren().add(iv);

        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);
        Label title = new Label(item.title);
        title.setStyle("-fx-text-fill: " + (item.read ? "#8AADD0" : "#FFFFFF") +
                "; -fx-font-size: 13px; -fx-font-weight: " + (item.read ? "normal" : "bold") + ";");
        Label msg = new Label(item.message);
        msg.setWrapText(true);
        msg.setStyle("-fx-text-fill: #8AADD0; -fx-font-size: 12px;");
        content.getChildren().addAll(title, msg);

        VBox right = new VBox(4);
        right.setAlignment(Pos.TOP_RIGHT);
        Label time = new Label(item.time);
        time.setStyle("-fx-text-fill: #4A6A8A; -fx-font-size: 11px;");
        Label status = new Label(item.read ? "• Đã đọc" : "• Chưa đọc");
        status.setStyle("-fx-text-fill: " + (item.read ? "#22c55e" : "#F5C518") + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        Button btnMenu = new Button("···");
        btnMenu.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #8AADD0; -fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");

        right.getChildren().addAll(time, status);
        row.getChildren().addAll(iconWrap, content, right, btnMenu);
        return row;
    }

    private void markAsRead(NotifItem item, HBox row) {
        if (!item.read) {
            item.read = true;
            buildNotificationList();
            updateBadge();
        }
    }

    private void updateBadge() {
        long unread = allItems.stream().filter(i -> !i.read).count();
        if (unread > 0) {
            lblUnreadDot.setText("●");
            lblUnreadDot.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 10px;");
        } else {
            lblUnreadDot.setText("");
        }
        SessionManager.setUnreadNotifCount((int) unread);
    }

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

    private void setupHovers() {
        String backN = "-fx-background-color: #162550; -fx-background-radius: 6; -fx-border-color: #2A5090; -fx-border-radius: 6; -fx-border-width: 1; -fx-text-fill: #CCDFF5; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 7 14 7 12; -fx-cursor: hand;";
        String backH = "-fx-background-color: #F5C518; -fx-background-radius: 6; -fx-border-color: #F5C518; -fx-border-radius: 6; -fx-border-width: 1; -fx-text-fill: #0A1929; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 7 14 7 12; -fx-cursor: hand;";
        addHover(btnQuayLai, backN, backH);

        String locN = "-fx-background-color: #F5C518; -fx-background-radius: 6; -fx-border-color: #F5C518; -fx-border-radius: 6; -fx-border-width: 1; -fx-text-fill: #0c1535; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 0 8 0; -fx-cursor: hand;";
        String locH = "-fx-background-color: #d4aa00; -fx-background-radius: 6; -fx-border-color: #d4aa00; -fx-border-radius: 6; -fx-border-width: 1; -fx-text-fill: #0c1535; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 0 8 0; -fx-cursor: hand;";
        addHover(btnLoc, locN, locH);

        addHover(btnTabCaiDat, TAB_INACTIVE,
                TAB_INACTIVE.replace("#0e1c40","#F5C518").replace("#CCDFF5","#0A1929").replace("#1b2f5e","#F5C518"));
    }

    private void addHover(Button b, String n, String h) {
        b.setOnMouseEntered(e -> b.setStyle(h));
        b.setOnMouseExited(e  -> b.setStyle(n));
    }

    private void buildSettingsPanel() {
        vboxNotifications.getChildren().clear();
        paginationBox.getChildren().clear();
        lblPageInfo.setText("");

        Label lblTitle = new Label("⚙  Cài đặt thông báo");
        lblTitle.setStyle("-fx-text-fill: #F5C518; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0 0 10 0;");

        Label lblSub = new Label("Chọn loại thông báo bạn muốn nhận");
        lblSub.setStyle("-fx-text-fill: #8AADD0; -fx-font-size: 12px; -fx-padding: 0 0 16 0;");

        VBox settingsBox = new VBox(12);
        settingsBox.setPadding(new Insets(20, 24, 20, 24));
        settingsBox.setStyle("-fx-background-color: #0e1c40; -fx-background-radius: 10; -fx-border-color: #1b2f5e; -fx-border-radius: 10; -fx-border-width: 1;");
        settingsBox.getChildren().addAll(lblTitle, lblSub);

        for (Map.Entry<String, Boolean> entry : notifSettings.entrySet()) {
            settingsBox.getChildren().add(buildSettingRow(entry.getKey(), entry.getValue()));
            Region sep = new Region();
            sep.setPrefHeight(1);
            sep.setStyle("-fx-background-color: #1b2f5e;");
            settingsBox.getChildren().add(sep);
        }

        Button btnSave = new Button("💾  Lưu cài đặt");
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setPrefHeight(44);
        btnSave.setStyle("-fx-background-color: #F5C518; -fx-text-fill: #0A1929; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        btnSave.setOnMouseEntered(e -> btnSave.setStyle("-fx-background-color: #d4aa00; -fx-text-fill: #0A1929; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;"));
        btnSave.setOnMouseExited(e  -> btnSave.setStyle("-fx-background-color: #F5C518; -fx-text-fill: #0A1929; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;"));
        btnSave.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thành công");
            alert.setHeaderText(null);
            alert.setContentText("✅ Đã lưu cài đặt thông báo!");
            alert.showAndWait();
        });

        VBox.setMargin(btnSave, new Insets(8, 0, 0, 0));
        settingsBox.getChildren().add(btnSave);
        vboxNotifications.getChildren().add(settingsBox);
    }

    private HBox buildSettingRow(String label, boolean defaultValue) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 0, 8, 0));

        String emoji;
        switch (label) {
            case "Thông báo khi bị outbid": emoji = "🔔"; break;
            case "Thông báo khi phiên sắp kết thúc": emoji = "⏰"; break;
            case "Thông báo thanh toán": emoji = "💳"; break;
            case "Thông báo phiên đấu giá mới": emoji = "🆕"; break;
            case "Thông báo khi thắng đấu giá": emoji = "🏆"; break;
            case "Cảnh báo tài khoản": emoji = "⚠️"; break;
            default: emoji = "📢";
        }

        Label lblEmoji = new Label(emoji);
        lblEmoji.setStyle("-fx-font-size: 18px; -fx-padding: 0 12 0 0;");

        VBox textBox = new VBox(2);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        Label lblName = new Label(label);
        lblName.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label lblDesc = new Label(getSettingDescription(label));
        lblDesc.setStyle("-fx-text-fill: #8AADD0; -fx-font-size: 11px;");
        textBox.getChildren().addAll(lblName, lblDesc);

        CheckBox toggle = new CheckBox();
        toggle.setSelected(defaultValue);
        toggle.setStyle("-fx-cursor: hand;");

        Label lblState = new Label(defaultValue ? "BẬT" : "TẮT");
        lblState.setMinWidth(36);
        lblState.setStyle("-fx-text-fill: " + (defaultValue ? "#22c55e" : "#ef4444") +
                "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 0 0 0 8;");

        toggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            notifSettings.put(label, newVal);
            lblState.setText(newVal ? "BẬT" : "TẮT");
            lblState.setStyle("-fx-text-fill: " + (newVal ? "#22c55e" : "#ef4444") +
                    "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 0 0 0 8;");
        });

        row.getChildren().addAll(lblEmoji, textBox, toggle, lblState);
        return row;
    }

    private String getSettingDescription(String label) {
        switch (label) {
            case "Thông báo khi bị outbid": return "Nhận thông báo khi có người trả giá cao hơn bạn";
            case "Thông báo khi phiên sắp kết thúc": return "Nhắc nhở khi phiên đấu giá còn 5 phút";
            case "Thông báo thanh toán": return "Xác nhận và nhắc nhở thanh toán sau khi thắng";
            case "Thông báo phiên đấu giá mới": return "Thông báo khi có sản phẩm mới được đăng";
            case "Thông báo khi thắng đấu giá": return "Chúc mừng khi bạn thắng một phiên đấu giá";
            case "Cảnh báo tài khoản": return "Cảnh báo đăng nhập từ thiết bị lạ";
            default: return "";
        }
    }

    @FXML void onTabDanhSach() {
        btnTabDanhSach.setStyle(TAB_ACTIVE);
        btnTabCaiDat.setStyle(TAB_INACTIVE);
        applyFilter();
    }

    @FXML void onTabCaiDat() {
        btnTabCaiDat.setStyle(TAB_ACTIVE);
        btnTabDanhSach.setStyle(TAB_INACTIVE);
        buildSettingsPanel();
    }

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