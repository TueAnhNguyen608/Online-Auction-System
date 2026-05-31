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

    @FXML
    private Label lblUserName;
    @FXML
    private Button btnTabDanhSach, btnTabDauGia, btnTabAutoBid;
    @FXML
    private Button btnBack;

    @FXML
    private Label lblProductName, lblDesc, lblCategory;
    @FXML
    private Label lblCondition, lblSeller, lblPostedDate;
    @FXML
    private TableView<BidRow> tableBidHistory;
    @FXML
    private TableColumn<BidRow, String> colBidNo, colBidder, colBidAmount, colBidTime;

    @FXML
    private HBox imgContainer;
    @FXML
    private javafx.scene.image.ImageView imgProduct;
    @FXML
    private Label lblCurrentPrice, lblMinStep, lblTimeLeft;
    @FXML
    private Label lblBidCount, lblStatus;
    @FXML
    private TextField txtBidAmount;
    @FXML
    private Label lblBidWarning;
    @FXML
    private Button btnPlaceBid;

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
    private long currentPrice = 24_500_000L;
    private long minStep = 500_000L;
    private Timeline countdownTimer;
    private String itemId;
    private String currentUsername = "Bạn";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public static class BidRow {
        private final String no, bidder, amount, time;

        public BidRow(String no, String bidder, String amount, String time) {
            this.no = no;
            this.bidder = bidder;
            this.amount = amount;
            this.time = time;
        }

        public String getNo() {
            return no;
        }

        public String getBidder() {
            return bidder;
        }

        public String getAmount() {
            return amount;
        }

        public String getTime() {
            return time;
        }
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
        minStep = 500_000L;

        lblProductName.setText(name);
        lblSeller.setText(seller);
        lblCurrentPrice.setText(formatPrice(currentPrice));
        lblMinStep.setText(formatPrice(minStep));
        lblTimeLeft.setText(formatTime(remainingSeconds));
        lblBidCount.setText("0");
        lblBidWarning.setText("⚠ Giá phải lớn hơn " + formatPrice(currentPrice));
        lblBidWarning.setStyle("-fx-text-fill: #F5C518; -fx-font-size: 11px; -fx-padding: 0 0 0 2;");
        tableBidHistory.setItems(FXCollections.observableArrayList());

        loadProductImage(name, imagePath);
        startCountdown();
    }

    private void loadBidHistoryFromServer() {
        if (itemId == null) return;
        new Thread(() -> {
            try {
                AuctionClient client = SessionManager.getClient();
                if (client == null) return;
                ResponseMessage resp = client.getBidHistory(itemId);
                if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Bid>>() {
                    }.getType();
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

    private void loadProductImage(String productName, String fallbackPath) {
        String imageUrl = getProductImageUrl(productName);
        new Thread(() -> {
            try {
                javafx.scene.image.Image img = null;
                if (imageUrl != null) {
                    try {
                        img = new javafx.scene.image.Image(imageUrl, 400, 300, true, true, true);
                        if (img.isError()) img = null;
                    } catch (Exception ignored) {
                    }
                }
                if (img == null && fallbackPath != null) {
                    try {
                        java.net.URL imgUrl = getClass().getResource(fallbackPath);
                        if (imgUrl == null) imgUrl = getClass().getResource("/icons/sp1.png");
                        if (imgUrl != null) {
                            img = new javafx.scene.image.Image(imgUrl.toExternalForm());
                        }
                    } catch (Exception ignored) {
                    }
                }
                final javafx.scene.image.Image finalImg = img;
                Platform.runLater(() -> {
                    if (finalImg != null) {
                        imgProduct.setImage(finalImg);
                    }
                });
            } catch (Exception e) {
                System.out.println("Không load được ảnh: " + e.getMessage());
            }
        }).start();
    }

    private String getProductImageUrl(String name) {
        if (name == null) return null;
        String l = name.toLowerCase();
        if (l.contains("iphone 15 pro max"))
            return "https://store.storeimages.cdn-apple.com/4668/as-images.apple.com/is/iphone-15-pro-max-black-titanium-select?wid=470&hei=556&fmt=png-alpha";
        if (l.contains("iphone 15 pro"))
            return "https://store.storeimages.cdn-apple.com/4668/as-images.apple.com/is/iphone-15-pro-black-titanium-select?wid=470&hei=556&fmt=png-alpha";
        if (l.contains("samsung galaxy s24 ultra"))
            return "https://images.samsung.com/is/image/samsung/p6pim/levant/2401/gallery/levant-galaxy-s24-ultra-s928-sm-s928bzageub-thumb-539573242";
        if (l.contains("macbook pro m3 max"))
            return "https://store.storeimages.cdn-apple.com/4668/as-images.apple.com/is/mbp14-m3-pro-max-spacegray-select-202310?wid=452&hei=420&fmt=jpeg&qlt=95";
        if (l.contains("dell xps"))
            return "https://i.dell.com/sites/csimages/App-Merchandizing_Images/all/xps-15-9530-pdp-module1.jpg";
        if (l.contains("asus rog") || l.contains("rog zephyrus"))
            return "https://dlcdnwebimgs.asus.com/gain/F1F41893-CC63-418E-9F96-F6A31EC7A38F/w717/h525";
        if (l.contains("sony wh") || l.contains("wh-1000"))
            return "https://www.sony.com/image/5d02da5df552836db894cead401a464b?fmt=png-alpha&wid=440";
        if (l.contains("airpods pro"))
            return "https://store.storeimages.cdn-apple.com/4668/as-images.apple.com/is/MQD83?wid=532&hei=582&fmt=png-alpha";
        if (l.contains("bose"))
            return "https://assets.bose.com/content/dam/Bose_DAM/Web/consumer_electronics/global/products/headphones/quietcomfort_ultra_headphones/product_silo_images/QuietComfort_Ultra_Headphones_Black_EC_hero.png/jcr:content/renditions/cq5dam.web.600.600.png";
        if (l.contains("apple watch ultra"))
            return "https://store.storeimages.cdn-apple.com/4668/as-images.apple.com/is/watch-ultra2-49-lead-titanium-natural-titanium-band-S?wid=470&hei=556&fmt=png-alpha";
        if (l.contains("ipad pro"))
            return "https://store.storeimages.cdn-apple.com/4668/as-images.apple.com/is/ipad-pro-13-select-wifi-spacegray-202405?wid=470&hei=556&fmt=png-alpha";
        return null;
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

    @FXML
    void onTabDanhSach() {
        if (countdownTimer != null) countdownTimer.stop();
        navigate("/fxml/bidding/ProductListPanel.fxml");
    }

    @FXML
    void onTabDauGia() {
    }

    @FXML
    void onTabAutoBid() {
        if (countdownTimer != null) countdownTimer.stop();
        navigate("/fxml/bidding/AutoBidPanel.fxml");
    }

    @FXML
    void onBack() {
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

    @FXML
    void onBidUp() {
        try {
            long cur = txtBidAmount.getText().trim().isEmpty() ? currentPrice
                    : Long.parseLong(txtBidAmount.getText().trim().replaceAll("[^0-9]", ""));
            txtBidAmount.setText(String.valueOf(cur + minStep));
        } catch (NumberFormatException ignored) {
            txtBidAmount.setText(String.valueOf(currentPrice + minStep));
        }
    }

    @FXML
    void onBidDown() {
        try {
            long cur = txtBidAmount.getText().trim().isEmpty() ? currentPrice + minStep
                    : Long.parseLong(txtBidAmount.getText().trim().replaceAll("[^0-9]", ""));
            long next = cur - minStep;
            if (next > 0) txtBidAmount.setText(String.valueOf(next));
        } catch (NumberFormatException ignored) {
        }
    }

    @FXML
    void onPlaceBid() {
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

                                // 🔥 CẬP NHẬT LỊCH SỬ NGAY LẬP TỨC - KHÔNG CHỜ SERVER
                                addBidHistoryLocally(bidderName, bidAmount, nowTime);

                                // Gọi server để đồng bộ (chạy ngầm, không block UI)
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

            // Fallback cho demo không cần server
            currentPrice = amount;
            lblCurrentPrice.setText(formatPrice(currentPrice));
            lblBidWarning.setText("✓ Đặt giá thành công: " + formatPrice(amount));
            lblBidWarning.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px; -fx-padding: 0 0 0 2;");

            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            int newNo = tableBidHistory.getItems().size() + 1;
            BidRow newRow = new BidRow(String.valueOf(newNo), currentUsername, formatPrice(amount), now);
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
            txtBidAmount.clear();

        } catch (NumberFormatException ex) {
            lblBidWarning.setText("⚠ Vui lòng nhập số tiền hợp lệ");
            lblBidWarning.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-padding: 0 0 0 2;");
        }
    }

    // 🔥 THÊM METHOD NÀY ĐỂ CẬP NHẬT LỊCH SỬ NGAY LẬP TỨC
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

            // Cuộn lên đầu để thấy dòng mới nhất
            tableBidHistory.scrollTo(0);
        });
    }
}