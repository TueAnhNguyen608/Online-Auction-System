package view.bidding;

import client.AuctionClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import common.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import view.common.SessionManager;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

public class ProductDetailPanel implements Initializable {

    @FXML private Label     lblUserName;
    @FXML private Button    btnTabDanhSach, btnTabDauGia, btnTabAutoBid;
    @FXML private Button    btnBack;

    @FXML private Label     lblProductName, lblDesc, lblCategory;
    @FXML private Label     lblCondition, lblSeller, lblPostedDate;
    @FXML private TableView<BidRow>            tableBidHistory;
    @FXML private TableColumn<BidRow, String>  colBidNo, colBidder, colBidAmount, colBidTime;

    @FXML private javafx.scene.layout.HBox     imgContainer;
    @FXML private javafx.scene.image.ImageView imgProduct;
    @FXML private Label     lblCurrentPrice, lblMinStep, lblTimeLeft;
    @FXML private Label     lblBidCount, lblStatus;
    @FXML private TextField txtBidAmount;
    @FXML private Label     lblBidWarning;
    @FXML private Button    btnPlaceBid;

    private static final String TAB_INACTIVE =
            "-fx-background-color: #0e1c40; -fx-text-fill: #CCDFF5; -fx-font-weight: bold;" +
                    " -fx-font-size: 13px; -fx-padding: 9 20 9 20; -fx-background-radius: 4 4 0 0;" +
                    " -fx-border-color: #1b2f5e; -fx-border-radius: 4 4 0 0; -fx-border-width: 1; -fx-cursor: hand;";
    private static final String TAB_HOVER =
            "-fx-background-color: #F5C518; -fx-text-fill: #0A1929; -fx-font-weight: bold;" +
                    " -fx-font-size: 13px; -fx-padding: 9 20 9 20; -fx-background-radius: 4 4 0 0;" +
                    " -fx-border-color: #F5C518; -fx-border-radius: 4 4 0 0; -fx-border-width: 1; -fx-cursor: hand;";
    private static final String BTN_BID_NORMAL =
            "-fx-background-color: #F5C518; -fx-text-fill: #0c1535; -fx-font-size: 13px;" +
                    " -fx-font-weight: bold; -fx-padding: 8 0 8 0; -fx-cursor: hand;" +
                    " -fx-background-radius: 6; -fx-border-radius: 6;" +
                    " -fx-effect: dropshadow(gaussian,rgba(245,197,24,0.35),10,0,0,2);";
    private static final String BTN_BID_HOVER =
            "-fx-background-color: #FFD700; -fx-text-fill: #0c1535; -fx-font-size: 13px;" +
                    " -fx-font-weight: bold; -fx-padding: 8 0 8 0; -fx-cursor: hand;" +
                    " -fx-background-radius: 6; -fx-border-radius: 6;" +
                    " -fx-effect: dropshadow(gaussian,rgba(245,197,24,0.5),10,0,0,2);";

    private long remainingSeconds = 6 * 86400L + 23 * 3600L + 52 * 60L + 47L;
    private long currentPrice     = 24_500_000L;
    private long minStep          = 500_000L;
    private Timeline countdownTimer;
    private String itemId; // ĐÃ THÊM: lưu itemId để gọi server
    private String currentUsername = "Bạn";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public static class BidRow {
        private final String no, bidder, amount, time;
        public BidRow(String no, String bidder, String amount, String time) {
            this.no = no; this.bidder = bidder; this.amount = amount; this.time = time;
        }
        public String getNo()     { return no; }
        public String getBidder() { return bidder; }
        public String getAmount() { return amount; }
        public String getTime()   { return time; }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (lblUserName != null && view.common.SessionManager.getCurrentUser() != null) {
            lblUserName.setText(view.common.SessionManager.getCurrentUser().getUsername());
        }
        tableBidHistory.sceneProperty().addListener((obs, o, scene) -> {
            if (scene != null) {
                java.net.URL css = getClass().getResource("/css/bidding_style.css");
                if (css != null) { String p = css.toExternalForm(); if (!scene.getStylesheets().contains(p)) scene.getStylesheets().add(p); }
            }
        });
        // Lấy tên user từ SessionManager
        if (SessionManager.getCurrentUser() != null) {
            currentUsername = SessionManager.getCurrentUser().getUsername();
        }
        setupBidHistoryTable();
        setupHovers();
        bindImageSize();
        loadSampleData();
        startCountdown();
        styleHeaderTable();
    }

    // ĐÃ THÊM: setter để ProductListPanel truyền itemId vào
    public void setItemId(String id) {
        this.itemId = id;
        loadBidHistoryFromServer(); // load lịch sử thật từ server
    }

    // ĐÃ THÊM: load lịch sử đấu giá từ server
    private void loadBidHistoryFromServer() {
        if (itemId == null) return;
        new Thread(() -> {
            try {
                AuctionClient client = SessionManager.getClient();
                if (client == null) return;
                ResponseMessage resp = client.getBidHistory(itemId);
                if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Bid>>(){}.getType();
                    List<Bid> bids = gson.fromJson(gson.toJson(resp.getData()), listType);
                    if (bids != null) {
                        List<BidRow> rows = new ArrayList<>();
                        for (int i = 0; i < bids.size(); i++) {
                            Bid bid = bids.get(i);
                            String time = bid.getBidTime() != null ?
                                    bid.getBidTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "";
                            rows.add(new BidRow(
                                    String.valueOf(i + 1),
                                    bid.getBidderName() != null ? bid.getBidderName() : "N/A",
                                    formatPrice((long) bid.getAmount()),
                                    time
                            ));
                        }
                        Platform.runLater(() -> {
                            tableBidHistory.setItems(FXCollections.observableArrayList(rows));
                            lblBidCount.setText(String.valueOf(rows.size()));
                        });
                    }
                }
            } catch (Exception e) {
                System.out.println("Lỗi load bid history: " + e.getMessage());
            }
        }).start();
    }

    public void loadProduct(int id, String name, long price, String seller,
                            long remainingSecondsIn, String imagePath) {
        if (countdownTimer != null) countdownTimer.stop();
        currentPrice     = price;
        remainingSeconds = remainingSecondsIn;
        minStep          = 500_000L;

        lblProductName.setText(name);
        lblSeller.setText(seller);
        lblCurrentPrice.setText(formatPrice(currentPrice));
        lblMinStep.setText(formatPrice(minStep));
        lblTimeLeft.setText(formatTime(remainingSeconds));
        lblBidCount.setText("0");
        lblBidWarning.setText("⚠  Giá phải lớn hơn " + formatPrice(currentPrice));
        lblBidWarning.setStyle("-fx-text-fill: #F5C518; -fx-font-size: 11px; -fx-padding: 0 0 0 2;");
        tableBidHistory.setItems(FXCollections.observableArrayList());

        try {
            java.net.URL imgUrl = getClass().getResource(imagePath);
            if (imgUrl == null) imgUrl = getClass().getResource("/icons/sp1.png");
            if (imgUrl != null) imgProduct.setImage(new javafx.scene.image.Image(imgUrl.toExternalForm()));
        } catch (Exception e) {
            System.out.println("[ProductDetailPanel] Không load ảnh: " + imagePath);
        }
        startCountdown();
    }

    private void loadSampleData() {
        lblProductName.setText("iPhone 15 Pro Max 1TB");
        lblDesc.setText("Apple chip A17 Pro, RAM 8GB, bộ nhớ 1TB, màn hình Super Retina XDR 6.7 inch, camera 48MP.");
        lblCategory.setText("Điện tử");
        lblCondition.setText("Mới 100%");
        lblSeller.setText("Nguyễn Văn A");
        lblPostedDate.setText("01/06/2024 14:30:25");
        lblCurrentPrice.setText(formatPrice(currentPrice));
        lblMinStep.setText(formatPrice(minStep));
        lblBidCount.setText("12");
        lblBidWarning.setText("⚠  Giá phải lớn hơn " + formatPrice(currentPrice));

        List<BidRow> rows = new ArrayList<>();
        rows.add(new BidRow("1", "Nguyễn Văn B", formatPrice(24_500_000L), "01/06/2024 16:45:12"));
        rows.add(new BidRow("2", "Trần Văn C",   formatPrice(24_000_000L), "01/06/2024 16:40:03"));
        rows.add(new BidRow("3", "Lê Văn D",     formatPrice(23_500_000L), "01/06/2024 16:35:21"));
        rows.add(new BidRow("4", "Phạm Văn E",   formatPrice(23_000_000L), "01/06/2024 16:30:10"));
        rows.add(new BidRow("5", "Hoàng Văn F",  formatPrice(22_500_000L), "01/06/2024 16:25:45"));
        tableBidHistory.setItems(FXCollections.observableArrayList(rows));
    }

    private void bindImageSize() {
        imgProduct.fitWidthProperty().bind(imgContainer.widthProperty().subtract(32));
    }

    private void setupBidHistoryTable() {
        tableBidHistory.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colBidNo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNo()));
        colBidNo.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean e) {
                super.updateItem(v, e);
                if (e || v == null) { setText(null); setStyle(""); return; }
                setText(v); setStyle("-fx-alignment: CENTER; -fx-text-fill: #C0D8F0;");
            }
        });
        colBidder.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBidder()));
        colBidder.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean e) {
                super.updateItem(v, e);
                if (e || v == null) { setText(null); setStyle(""); return; }
                String color = getIndex() == 0 ? "#22c55e" : "#FFFFFF";
                setText(v); setStyle("-fx-alignment: CENTER_LEFT; -fx-text-fill: " + color + "; -fx-padding: 0 0 0 10;");
            }
        });
        colBidAmount.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAmount()));
        colBidAmount.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean e) {
                super.updateItem(v, e);
                if (e || v == null) { setText(null); setStyle(""); return; }
                String color = getIndex() == 0 ? "#22c55e" : "#C0D8F0";
                setText(v); setStyle("-fx-alignment: CENTER; -fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });
        colBidTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTime()));
        colBidTime.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean e) {
                super.updateItem(v, e);
                if (e || v == null) { setText(null); setStyle(""); return; }
                setText(v); setStyle("-fx-alignment: CENTER; -fx-text-fill: #8AADD0;");
            }
        });

        tableBidHistory.setRowFactory(tv -> new TableRow<BidRow>() {
            @Override protected void updateItem(BidRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("-fx-background-color: #0e1c40; -fx-border-color: transparent;");
                    setOnMouseEntered(null); setOnMouseExited(null);
                } else {
                    String bg = (getIndex() % 2 == 0) ? "#0e1c40" : "#11224a";
                    String normal = "-fx-background-color: " + bg + "; -fx-border-color: #1b2f5e; -fx-border-width: 0 0 1 0;";
                    String hover  = "-fx-background-color: #1E4268; -fx-border-color: #1b2f5e; -fx-border-width: 0 0 1 0; -fx-cursor: hand;";
                    setStyle(normal);
                    setOnMouseEntered(ev -> setStyle(hover));
                    setOnMouseExited(ev  -> setStyle(normal));
                }
            }
        });
    }

    private void styleHeaderTable() {
        Platform.runLater(() -> Platform.runLater(() -> {
            tableBidHistory.lookupAll(".column-header").forEach(node ->
                    node.setStyle("-fx-background-color: #162550; -fx-border-color: transparent #2A5090 transparent transparent; -fx-border-width: 0 1 0 0;"));
            tableBidHistory.lookupAll(".column-header .label").forEach(node ->
                    node.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 12px;"));
            tableBidHistory.lookupAll(".column-header-background").forEach(node ->
                    node.setStyle("-fx-background-color: #162550;"));
            tableBidHistory.lookupAll(".filler").forEach(node ->
                    node.setStyle("-fx-background-color: #162550;"));
        }));
    }

    private void startCountdown() {
        if (countdownTimer != null) countdownTimer.stop();
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (remainingSeconds > 0) remainingSeconds--;
            lblTimeLeft.setText(formatTime(remainingSeconds));
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private static String formatTime(long secs) {
        if (secs <= 0) return "Đã kết thúc";
        long d=secs/86400, h=(secs%86400)/3600, m=(secs%3600)/60, s=secs%60;
        return d > 0 ? String.format("%02d ngày %02d:%02d:%02d",d,h,m,s) : String.format("%02d:%02d:%02d",h,m,s);
    }
    private static String formatPrice(long p) {
        return String.format("%,d", p).replace(",", ".") + " đ";
    }

    private void setupHovers() {
        addHover(btnTabDanhSach, TAB_INACTIVE, TAB_HOVER);
        addHover(btnTabAutoBid,  TAB_INACTIVE, TAB_HOVER);
        addHover(btnPlaceBid,    BTN_BID_NORMAL, BTN_BID_HOVER);
    }
    private void addHover(Button b, String n, String h) {
        b.setOnMouseEntered(e -> b.setStyle(h));
        b.setOnMouseExited(e  -> b.setStyle(n));
    }

    @FXML void onTabDanhSach() {
        if (countdownTimer != null) countdownTimer.stop();
        navigate("/fxml/bidding/ProductListPanel.fxml");
    }
    @FXML void onTabDauGia() { /* đang ở đây */ }
    @FXML void onTabAutoBid() {
        if (countdownTimer != null) countdownTimer.stop();
        navigate("/fxml/bidding/AutoBidPanel.fxml");
    }
    @FXML void onBack() {
        if (countdownTimer != null) countdownTimer.stop();
        navigate("/fxml/bidding/ProductListPanel.fxml");
    }

    private void navigate(String fxmlPath) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) btnBack.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @FXML void onBidUp() {
        try {
            long cur = txtBidAmount.getText().trim().isEmpty() ? currentPrice
                    : Long.parseLong(txtBidAmount.getText().trim().replaceAll("[^0-9]",""));
            txtBidAmount.setText(String.valueOf(cur + minStep));
        } catch (NumberFormatException ignored) {
            txtBidAmount.setText(String.valueOf(currentPrice + minStep));
        }
    }

    @FXML void onBidDown() {
        try {
            long cur = txtBidAmount.getText().trim().isEmpty() ? currentPrice + minStep
                    : Long.parseLong(txtBidAmount.getText().trim().replaceAll("[^0-9]",""));
            long next = cur - minStep;
            if (next > 0) txtBidAmount.setText(String.valueOf(next));
        } catch (NumberFormatException ignored) {}
    }

    @FXML void onPlaceBid() {
        String input = txtBidAmount.getText().trim();
        try {
            long amount = Long.parseLong(input.replaceAll("[^0-9]",""));
            if (amount <= currentPrice) {
                lblBidWarning.setText("⚠  Giá phải lớn hơn " + formatPrice(currentPrice));
                lblBidWarning.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-padding: 0 0 0 2;");
                return;
            }

            // ĐÃ THÊM: gọi server đặt giá nếu có itemId
            if (itemId != null && SessionManager.getClient() != null) {
                User user = SessionManager.getCurrentUser();
                Bid bid = new Bid();
                bid.setItemId(itemId);
                bid.setAmount(amount);
                if (user != null) {
                    bid.setBidderId(user.getId());
                    bid.setBidderName(user.getUsername());
                }
                new Thread(() -> {
                    try {
                        ResponseMessage resp = SessionManager.getClient().placeBid(bid);
                        Platform.runLater(() -> {
                            if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                                currentPrice = amount;
                                lblCurrentPrice.setText(formatPrice(currentPrice));
                                lblBidWarning.setText("✓  Đặt giá thành công: " + formatPrice(amount));
                                lblBidWarning.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px; -fx-padding: 0 0 0 2;");
                                txtBidAmount.clear();
                                loadBidHistoryFromServer(); // reload lịch sử thật
                            } else {
                                lblBidWarning.setText("✕  " + (resp != null ? resp.getMessage() : "Lỗi đặt giá"));
                                lblBidWarning.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-padding: 0 0 0 2;");
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            lblBidWarning.setText("✕  Lỗi kết nối: " + e.getMessage());
                            lblBidWarning.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-padding: 0 0 0 2;");
                        });
                    }
                }).start();
                return;
            }

            // Fallback: xử lý local nếu không có server
            currentPrice = amount;
            lblCurrentPrice.setText(formatPrice(currentPrice));
            lblBidWarning.setText("✓  Đặt giá thành công: " + formatPrice(amount));
            lblBidWarning.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px; -fx-padding: 0 0 0 2;");

            String now = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            int newNo = tableBidHistory.getItems().size() + 1;
            BidRow newRow = new BidRow(String.valueOf(newNo), currentUsername, formatPrice(amount), now);
            List<BidRow> updated = new ArrayList<>();
            updated.add(newRow);
            updated.addAll(tableBidHistory.getItems());
            List<BidRow> renumbered = new ArrayList<>();
            for (int i = 0; i < updated.size(); i++) {
                BidRow r = updated.get(i);
                renumbered.add(new BidRow(String.valueOf(i+1), r.getBidder(), r.getAmount(), r.getTime()));
            }
            tableBidHistory.setItems(FXCollections.observableArrayList(renumbered));
            lblBidCount.setText(String.valueOf(renumbered.size()));
            txtBidAmount.clear();

        } catch (NumberFormatException ex) {
            lblBidWarning.setText("⚠  Vui lòng nhập số tiền hợp lệ");
            lblBidWarning.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-padding: 0 0 0 2;");
        }
    }
}