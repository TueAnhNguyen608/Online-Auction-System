package com.auction.ui.bidding;

import com.auction.client.AuctionClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.auction.common.LocalDateTimeAdapter;
import com.auction.common.model.bidding.Bid;
import com.auction.common.model.communication.ResponseCode;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.common.model.user.User;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.auction.ui.auth.SessionManager;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ProductDetailPanel implements Initializable {

    @FXML private Label lblUserName;
    @FXML private Button btnTabDanhSach, btnTabDauGia, btnTabAutoBid;
    @FXML private Button btnBack;
    @FXML private Label lblProductName, lblDesc, lblCategory;
    @FXML private Label lblCondition, lblSeller, lblPostedDate;
    @FXML private TableView<BidRow> tableBidHistory;
    @FXML private TableColumn<BidRow, String> colBidNo, colBidder, colBidAmount, colBidTime;
    @FXML private HBox imgContainer;
    @FXML private javafx.scene.image.ImageView imgProduct;
    @FXML private Label lblCurrentPrice, lblMinStep, lblTimeLeft;
    @FXML private Label lblBidCount, lblStatus;
    @FXML private TextField txtBidAmount;
    @FXML private Label lblBidWarning;
    @FXML private Button btnPlaceBid;

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

    private long remainingSeconds = 7 * 86400L;
    private long currentPrice = 25000000L;
    private long minStep = 500000L;
    private Timeline countdownTimer;
    private String itemId;
    private String currentUsername = "Bạn";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public static class BidRow {
        private final String no, bidder, amount, time;
        public BidRow(String no, String bidder, String amount, String time) {
            this.no = no; this.bidder = bidder; this.amount = amount; this.time = time;
        }
        public String getNo() { return no; }
        public String getBidder() { return bidder; }
        public String getAmount() { return amount; }
        public String getTime() { return time; }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (lblUserName != null && SessionManager.getCurrentUser() != null) {
            lblUserName.setText(SessionManager.getCurrentUser().getUsername());
        }
        if (SessionManager.getCurrentUser() != null) {
            currentUsername = SessionManager.getCurrentUser().getUsername();
        }
        setupBidHistoryTable();
        setupHovers();
        bindImageSize();
        startCountdown();
        styleHeaderTable();
        System.out.println("🔧 ProductDetailPanel.initialize() - Hoàn tất");
    }

    public void setItemId(String id) {
        this.itemId = id;
        loadBidHistoryFromServer();
    }

    public void loadProduct(int id, String name, long price, String seller,
                            long remainingSecondsIn, String imagePath) {
        if (countdownTimer != null) countdownTimer.stop();
        currentPrice = price;
        remainingSeconds = remainingSecondsIn;
        minStep = 500000L;

        lblProductName.setText(name);
        lblSeller.setText(seller);
        lblCurrentPrice.setText(formatPrice(currentPrice));
        lblMinStep.setText(formatPrice(minStep));
        lblTimeLeft.setText(formatTime(remainingSeconds));
        lblBidCount.setText("0");
        lblBidWarning.setText("⚠ Giá phải lớn hơn " + formatPrice(currentPrice));
        lblBidWarning.setStyle("-fx-text-fill: #F5C518; -fx-font-size: 11px; -fx-padding: 0 0 0 2;");
        tableBidHistory.setItems(FXCollections.observableArrayList());

        loadProductImage(name);
        startCountdown();
        System.out.println("📱 Load sản phẩm: " + name + " - Giá: " + formatPrice(price));
    }

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
                                    bid.getBidTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "";
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

    // 🔥 Load ảnh từ thư mục icons (sp1.png → sp18.png)
    private void loadProductImage(String productName) {
        String imagePath = getImagePath(productName);

        new Thread(() -> {
            try {
                javafx.scene.image.Image img = null;

                if (imagePath != null) {
                    java.net.URL imgUrl = getClass().getResource(imagePath);
                    if (imgUrl != null) {
                        img = new javafx.scene.image.Image(imgUrl.toExternalForm(), 400, 300, true, true, true);
                        System.out.println("✅ Đã tải ảnh: " + imagePath + " cho sản phẩm: " + productName);
                    } else {
                        System.out.println("⚠️ Không tìm thấy ảnh: " + imagePath);
                    }
                }

                // Fallback: ảnh mặc định sp1.png
                if (img == null || img.isError()) {
                    java.net.URL defaultUrl = getClass().getResource("/icons/sp1.png");
                    if (defaultUrl != null) {
                        img = new javafx.scene.image.Image(defaultUrl.toExternalForm(), 400, 300, true, true, true);
                        System.out.println("📦 Dùng ảnh mặc định: /icons/sp1.png");
                    }
                }

                final javafx.scene.image.Image finalImg = img;
                Platform.runLater(() -> {
                    if (finalImg != null && !finalImg.isError()) {
                        imgProduct.setImage(finalImg);
                    }
                });
            } catch (Exception e) {
                System.out.println("Không load được ảnh: " + e.getMessage());
            }
        }).start();
    }

    // 🔥 Map tên sản phẩm → ảnh sp1.png → sp18.png
    private String getImagePath(String productName) {
        if (productName == null) return "/icons/sp1.png";
        String name = productName.toLowerCase();

        if (name.contains("iphone 15 pro max")) return "/icons/sp1.png";
        if (name.contains("iphone 15 pro")) return "/icons/sp2.png";
        if (name.contains("samsung galaxy s24 ultra")) return "/icons/sp3.png";
        if (name.contains("xiaomi 14 ultra")) return "/icons/sp4.png";
        if (name.contains("google pixel 8 pro")) return "/icons/sp5.png";
        if (name.contains("macbook pro m3 max")) return "/icons/sp6.png";
        if (name.contains("dell xps 15")) return "/icons/sp7.png";
        if (name.contains("lg gram 17")) return "/icons/sp8.png";
        if (name.contains("asus rog zephyrus g14")) return "/icons/sp9.png";
        if (name.contains("sony wh-1000xm5")) return "/icons/sp10.png";
        if (name.contains("airpods pro 2")) return "/icons/sp11.png";
        if (name.contains("bose qc ultra")) return "/icons/sp12.png";
        if (name.contains("apple watch ultra 2")) return "/icons/sp13.png";
        if (name.contains("samsung galaxy watch 6 classic")) return "/icons/sp14.png";
        if (name.contains("magic keyboard")) return "/icons/sp15.png";
        if (name.contains("logitech mx master 3s")) return "/icons/sp16.png";
        if (name.contains("ipad pro 12.9 inch m2")) return "/icons/sp17.png";
        if (name.contains("samsung tab s9 ultra")) return "/icons/sp18.png";

        return "/icons/sp1.png";
    }

    private void bindImageSize() {
        imgProduct.fitWidthProperty().bind(imgContainer.widthProperty().subtract(32));
    }

    private void setupBidHistoryTable() {
        TableView<BidRow> tableView = tableBidHistory;
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colBidNo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNo()));
        colBidder.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBidder()));
        colBidAmount.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAmount()));
        colBidTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTime()));

        tableView.setRowFactory(tv -> new TableRow<BidRow>() {
            @Override
            protected void updateItem(BidRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("-fx-background-color: #0e1c40; -fx-border-color: transparent;");
                } else {
                    String bg = (getIndex() % 2 == 0) ? "#0e1c40" : "#11224a";
                    setStyle("-fx-background-color: " + bg + "; -fx-border-color: #1b2f5e; -fx-border-width: 0 0 1 0;");
                }
            }
        });
    }

    private void styleHeaderTable() {
        Platform.runLater(() -> {
            if (tableBidHistory == null) return;
            tableBidHistory.lookupAll(".column-header").forEach(node ->
                    node.setStyle("-fx-background-color: #162550; -fx-border-color: transparent #2A5090 transparent transparent; -fx-border-width: 0 1 0 0;"));
            tableBidHistory.lookupAll(".column-header .label").forEach(node ->
                    node.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 12px;"));
        });
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
        long d = secs / 86400, h = (secs % 86400) / 3600, m = (secs % 3600) / 60, s = secs % 60;
        return d > 0 ? String.format("%02d ngày %02d:%02d:%02d", d, h, m, s) : String.format("%02d:%02d:%02d", h, m, s);
    }

    private static String formatPrice(long p) {
        return String.format("%,d", p).replace(",", ".") + " đ";
    }

    private void setupHovers() {
        addHover(btnTabDanhSach, TAB_INACTIVE, TAB_HOVER);
        addHover(btnTabAutoBid, TAB_INACTIVE, TAB_HOVER);
        addHover(btnPlaceBid, BTN_BID_NORMAL, BTN_BID_HOVER);
    }

    private void addHover(Button b, String n, String h) {
        b.setOnMouseEntered(e -> b.setStyle(h));
        b.setOnMouseExited(e -> b.setStyle(n));
    }

    @FXML void onTabDanhSach() {
        if (countdownTimer != null) countdownTimer.stop();
        navigate("/fxml/bidding/ProductListPanel.fxml");
    }

    @FXML void onTabDauGia() {}

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) btnBack.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML void onBidUp() {
        try {
            long cur = txtBidAmount.getText().trim().isEmpty() ? currentPrice
                    : Long.parseLong(txtBidAmount.getText().trim().replaceAll("[^0-9]", ""));
            txtBidAmount.setText(String.valueOf(cur + minStep));
        } catch (NumberFormatException ignored) {
            txtBidAmount.setText(String.valueOf(currentPrice + minStep));
        }
    }

    @FXML void onBidDown() {
        try {
            long cur = txtBidAmount.getText().trim().isEmpty() ? currentPrice + minStep
                    : Long.parseLong(txtBidAmount.getText().trim().replaceAll("[^0-9]", ""));
            long next = cur - minStep;
            if (next > 0) txtBidAmount.setText(String.valueOf(next));
        } catch (NumberFormatException ignored) {}
    }

    @FXML void onPlaceBid() {
        String input = txtBidAmount.getText().trim();
        try {
            long amount = Long.parseLong(input.replaceAll("[^0-9]", ""));
            if (amount <= currentPrice) {
                lblBidWarning.setText("⚠ Giá phải lớn hơn " + formatPrice(currentPrice));
                lblBidWarning.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-padding: 0 0 0 2;");
                return;
            }

            if (itemId != null && SessionManager.getClient() != null) {
                User user = SessionManager.getCurrentUser();
                Bid bid = new Bid();
                bid.setItemId(itemId);
                bid.setAmount(amount);
                if (user != null) {
                    bid.setBidderId(user.getId());
                    bid.setBidderName(user.getUsername());
                }

                final long bidAmount = amount;
                final String bidderName = user != null ? user.getUsername() : currentUsername;
                final String nowTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

                new Thread(() -> {
                    try {
                        ResponseMessage resp = SessionManager.getClient().placeBid(bid);
                        Platform.runLater(() -> {
                            if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                                currentPrice = bidAmount;
                                lblCurrentPrice.setText(formatPrice(currentPrice));
                                lblBidWarning.setText("✓ Đặt giá thành công: " + formatPrice(bidAmount));
                                lblBidWarning.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px; -fx-padding: 0 0 0 2;");
                                txtBidAmount.clear();
                                addBidHistoryLocally(bidderName, bidAmount, nowTime);
                                loadBidHistoryFromServer();
                            } else {
                                lblBidWarning.setText("✕ " + (resp != null ? resp.getMessage() : "Lỗi đặt giá"));
                                lblBidWarning.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-padding: 0 0 0 2;");
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            lblBidWarning.setText("✕ Lỗi kết nối: " + e.getMessage());
                            lblBidWarning.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-padding: 0 0 0 2;");
                        });
                    }
                }).start();
                return;
            }

            currentPrice = amount;
            lblCurrentPrice.setText(formatPrice(currentPrice));
            lblBidWarning.setText("✓ Đặt giá thành công: " + formatPrice(amount));
            lblBidWarning.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px; -fx-padding: 0 0 0 2;");
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            addBidHistoryLocally(currentUsername, amount, now);
            txtBidAmount.clear();

        } catch (NumberFormatException ex) {
            lblBidWarning.setText("⚠ Vui lòng nhập số tiền hợp lệ");
            lblBidWarning.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-padding: 0 0 0 2;");
        }
    }

    private void addBidHistoryLocally(String bidderName, long amount, String time) {
        Platform.runLater(() -> {
            int newNo = tableBidHistory.getItems().size() + 1;
            BidRow newRow = new BidRow(String.valueOf(newNo), bidderName, formatPrice(amount), time);
            List<BidRow> updated = new ArrayList<>();
            updated.add(newRow);
            updated.addAll(tableBidHistory.getItems());
            List<BidRow> renumbered = new ArrayList<>();
            for (int i = 0; i < updated.size(); i++) {
                BidRow r = updated.get(i);
                renumbered.add(new BidRow(String.valueOf(i + 1), r.getBidder(), r.getAmount(), r.getTime()));
            }
            tableBidHistory.setItems(FXCollections.observableArrayList(renumbered));
            lblBidCount.setText(String.valueOf(renumbered.size()));
            tableBidHistory.scrollTo(0);
        });
    }
}