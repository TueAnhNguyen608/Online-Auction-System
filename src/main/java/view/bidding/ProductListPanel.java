package view.bidding;

import client.AuctionClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import common.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
import java.util.stream.Collectors;

public class ProductListPanel implements Initializable {

    @FXML private Label     lblUserName;
    @FXML private Button    btnTabDanhSach, btnTabDauGia, btnTabAutoBid;
    @FXML private Button    btnQuayLai, btnSearch;
    @FXML private TextField txtSearch;
    @FXML private TableView<ProductRow>            tableProducts;
    @FXML private TableColumn<ProductRow, Integer> colStt;
    @FXML private TableColumn<ProductRow, String>  colName, colPrice, colSeller, colTime;
    @FXML private HBox   paginationBox;
    @FXML private Button btnFirst, btnPrev, btnNext, btnLast;

    private static final String TAB_INACTIVE =
            "-fx-background-color: #1A3A6E; -fx-text-fill: #CCDFF5; -fx-font-weight: bold;" +
                    " -fx-font-size: 13px; -fx-padding: 9 20 9 20; -fx-background-radius: 4 4 0 0;" +
                    " -fx-border-color: #2A5090; -fx-border-radius: 4 4 0 0; -fx-border-width: 1; -fx-cursor: hand;";
    private static final String TAB_HOVER =
            "-fx-background-color: #F5C518; -fx-text-fill: #0A1929; -fx-font-weight: bold;" +
                    " -fx-font-size: 13px; -fx-padding: 9 20 9 20; -fx-background-radius: 4 4 0 0;" +
                    " -fx-border-color: #F5C518; -fx-border-radius: 4 4 0 0; -fx-border-width: 1; -fx-cursor: hand;";
    private static final String BTN_BACK_NORMAL =
            "-fx-background-color: #132840; -fx-background-radius: 6; -fx-border-color: #2A5080;" +
                    " -fx-border-radius: 6; -fx-border-width: 1; -fx-text-fill: #CCDFF5; -fx-font-size: 12px;" +
                    " -fx-font-weight: bold; -fx-padding: 7 18 7 14; -fx-cursor: hand;";
    private static final String BTN_BACK_HOVER =
            "-fx-background-color: #F5C518; -fx-background-radius: 6; -fx-border-color: #F5C518;" +
                    " -fx-border-radius: 6; -fx-border-width: 1; -fx-text-fill: #0A1929; -fx-font-size: 12px;" +
                    " -fx-font-weight: bold; -fx-padding: 7 18 7 14; -fx-cursor: hand;";
    private static final String PAGE_NAV_NORMAL =
            "-fx-background-color: #0C1E35; -fx-background-radius: 6; -fx-border-color: #2A4A6E;" +
                    " -fx-border-radius: 6; -fx-border-width: 1; -fx-text-fill: #CCDFF5; -fx-font-size: 14px;" +
                    " -fx-font-weight: bold; -fx-pref-width: 36; -fx-pref-height: 36; -fx-cursor: hand;";
    private static final String PAGE_NAV_HOVER =
            "-fx-background-color: #F5C518; -fx-background-radius: 6; -fx-border-color: #F5C518;" +
                    " -fx-border-radius: 6; -fx-border-width: 1; -fx-text-fill: #0A1929; -fx-font-size: 14px;" +
                    " -fx-font-weight: bold; -fx-pref-width: 36; -fx-pref-height: 36; -fx-cursor: hand;";
    private static final String PAGE_NUM_NORMAL =
            "-fx-background-color: #0C1E35; -fx-background-radius: 6; -fx-border-color: #2A4A6E;" +
                    " -fx-border-radius: 6; -fx-border-width: 1; -fx-text-fill: #CCDFF5; -fx-font-size: 13px;" +
                    " -fx-font-weight: bold; -fx-pref-width: 36; -fx-pref-height: 36; -fx-cursor: hand;";
    private static final String PAGE_NUM_ACTIVE =
            "-fx-background-color: #F5C518; -fx-background-radius: 6; -fx-border-color: #F5C518;" +
                    " -fx-border-radius: 6; -fx-border-width: 1; -fx-text-fill: #0A1929; -fx-font-size: 13px;" +
                    " -fx-font-weight: bold; -fx-pref-width: 36; -fx-pref-height: 36; -fx-cursor: hand;";

    private static final int ROW_HEIGHT = 55;
    private static final int PAGE_SIZE  = 8;
    private int currentPage = 1;
    private final List<ProductRow> allProducts = new ArrayList<>();
    private List<ProductRow> filteredProducts  = new ArrayList<>();
    private Timeline countdownTimer;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public static class ProductRow {
        private final int stt;
        private final String name, seller;
        private final long price;
        private long remainingSeconds;
        private final StringProperty timeDisplay = new SimpleStringProperty();
        private String itemId;

        public ProductRow(int stt, String name, long price, String seller, long remainingSecs) {
            this.stt = stt; this.name = name; this.price = price; this.seller = seller;
            this.remainingSeconds = remainingSecs;
            timeDisplay.set(fmt(remainingSeconds));
        }
        public void tick() { if (remainingSeconds > 0) remainingSeconds--; timeDisplay.set(fmt(remainingSeconds)); }
        private static String fmt(long s) {
            if (s <= 0) return "Đã kết thúc";
            long d=s/86400, h=(s%86400)/3600, m=(s%3600)/60, ss=s%60;
            return d > 0 ? String.format("%d ngày %02d:%02d:%02d",d,h,m,ss) : String.format("%02d:%02d:%02d",h,m,ss);
        }
        public int    getStt()               { return stt; }
        public String getName()              { return name; }
        public String getSeller()            { return seller; }
        public long   getPrice()             { return price; }
        public long   getRemainingSeconds()  { return remainingSeconds; }
        public String getItemId()            { return itemId; }
        public void   setItemId(String id)   { this.itemId = id; }
        public StringProperty timeDisplayProperty() { return timeDisplay; }
        public String getFormattedPrice() { return String.format("%,d", price).replace(",", ".") + " đ"; }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (lblUserName != null && view.common.SessionManager.getCurrentUser() != null) {
            lblUserName.setText(view.common.SessionManager.getCurrentUser().getUsername());
        }
        tableProducts.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableProducts.setFixedCellSize(ROW_HEIGHT);
        tableProducts.prefHeightProperty().bind(
                tableProducts.fixedCellSizeProperty().multiply(PAGE_SIZE).add(44));
        tableProducts.minHeightProperty().bind(tableProducts.prefHeightProperty());
        tableProducts.maxHeightProperty().bind(tableProducts.prefHeightProperty());

        tableProducts.sceneProperty().addListener((obs, o, scene) -> {
            if (scene != null) {
                java.net.URL css = getClass().getResource("/css/bidding_style.css");
                if (css != null) { String p = css.toExternalForm(); if (!scene.getStylesheets().contains(p)) scene.getStylesheets().add(p); }
            }
        });

        setupTableColumns();
        setupRowFactory();
        setupHoverEffects();
        loadData(); // ĐÃ ĐỔI: dùng loadData thay vì loadSampleData
        setupSearchListener();
        updateTable();
        startCountdown();
        styleNavButtons();
        Platform.runLater(() -> Platform.runLater(() -> {
            tableProducts.lookupAll(".column-header").forEach(node ->
                    node.setStyle("-fx-background-color: #1E4268; -fx-border-color: transparent #2A5090 transparent transparent; -fx-border-width: 0 1 0 0;"));
            tableProducts.lookupAll(".column-header .label").forEach(node ->
                    node.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 13px;"));
            tableProducts.lookupAll(".column-header-background").forEach(node ->
                    node.setStyle("-fx-background-color: #1E4268;"));
            tableProducts.lookupAll(".filler").forEach(node ->
                    node.setStyle("-fx-background-color: #1E4268;"));
            javafx.scene.Node header = tableProducts.lookup("TableHeaderRow");
            if (header != null)
                header.setStyle("-fx-min-height: 45px; -fx-pref-height: 45px; -fx-max-height: 45px;");
        }));
    }

    // ĐÃ THÊM: Load dữ liệu từ server, fallback về sample nếu lỗi
    private void loadData() {
        new Thread(() -> {
            try {
                AuctionClient client = SessionManager.getClient();
                if (client == null) throw new Exception("Chưa có kết nối server");
                ResponseMessage resp = client.getActiveItems();
                if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                    java.lang.reflect.Type listType = new TypeToken<List<Item>>(){}.getType();
                    List<Item> items = gson.fromJson(gson.toJson(resp.getData()), listType);
                    if (items != null && !items.isEmpty()) {
                        List<ProductRow> rows = new ArrayList<>();
                        for (int i = 0; i < items.size(); i++) {
                            Item item = items.get(i);
                            long remaining = 0;
                            if (item.getEndTime() != null) {
                                remaining = java.time.Duration.between(
                                        LocalDateTime.now(), item.getEndTime()).getSeconds();
                                if (remaining < 0) remaining = 0;
                            }
                            ProductRow row = new ProductRow(
                                    i + 1,
                                    item.getName(),
                                    (long) item.getCurrentPrice(),
                                    item.getSellerName() != null ? item.getSellerName() : "N/A",
                                    remaining
                            );
                            row.setItemId(item.getId());
                            rows.add(row);
                        }
                        Platform.runLater(() -> {
                            allProducts.clear();
                            allProducts.addAll(rows);
                            filteredProducts = new ArrayList<>(allProducts);
                            updateTable();
                            System.out.println("✅ Loaded " + rows.size() + " sản phẩm từ server");
                        });
                        return;
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠ Không lấy được dữ liệu server, dùng dữ liệu mẫu: " + e.getMessage());
            }
            // Fallback: dùng dữ liệu mẫu
            Platform.runLater(this::loadSampleData);
        }).start();
    }

    private void loadSampleData() {
        allProducts.clear();
        allProducts.add(new ProductRow( 1,"iPhone 15 Pro Max 1TB",          25_000_000L,"seller1", 7 * 86400L));
        allProducts.add(new ProductRow( 2,"iPhone 15 Pro 512GB",            22_000_000L,"seller1", 5 * 86400L));
        allProducts.add(new ProductRow( 3,"Samsung Galaxy S24 Ultra",       20_000_000L,"seller1",10 * 86400L));
        allProducts.add(new ProductRow( 4,"Xiaomi 14 Ultra",                15_000_000L,"seller2", 6 * 86400L));
        allProducts.add(new ProductRow( 5,"Google Pixel 8 Pro",             18_000_000L,"seller2", 8 * 86400L));
        allProducts.add(new ProductRow( 6,"MacBook Pro M3 Max",             45_000_000L,"seller1",12 * 86400L));
        allProducts.add(new ProductRow( 7,"Dell XPS 15",                    35_000_000L,"seller2", 9 * 86400L));
        allProducts.add(new ProductRow( 8,"LG Gram 17",                     28_000_000L,"seller1", 7 * 86400L));
        allProducts.add(new ProductRow( 9,"Asus ROG Zephyrus G14",          32_000_000L,"seller2",11 * 86400L));
        allProducts.add(new ProductRow(10,"Sony WH-1000XM5",                 5_000_000L,"seller1", 4 * 86400L));
        allProducts.add(new ProductRow(11,"AirPods Pro 2",                   4_500_000L,"seller1", 3 * 86400L));
        allProducts.add(new ProductRow(12,"Bose QC Ultra",                   6_000_000L,"seller2", 5 * 86400L));
        allProducts.add(new ProductRow(13,"Apple Watch Ultra 2",            15_000_000L,"seller1", 8 * 86400L));
        allProducts.add(new ProductRow(14,"Samsung Galaxy Watch 6 Classic",  7_000_000L,"seller2", 6 * 86400L));
        allProducts.add(new ProductRow(15,"Magic Keyboard",                  3_000_000L,"seller1",10 * 86400L));
        allProducts.add(new ProductRow(16,"Logitech MX Master 3S",           2_000_000L,"seller2",15 * 86400L));
        allProducts.add(new ProductRow(17,"iPad Pro 12.9 inch M2",          28_000_000L,"seller1",20 * 86400L));
        allProducts.add(new ProductRow(18,"Samsung Tab S9 Ultra",           22_000_000L,"seller2",18 * 86400L));
        filteredProducts = new ArrayList<>(allProducts);
        updateTable();
    }

    private void setupRowFactory() {
        tableProducts.setRowFactory(tv -> new TableRow<ProductRow>() {
            @Override
            protected void updateItem(ProductRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("-fx-background-color: #0e1c40; -fx-border-color: #1A2F4A; -fx-border-width: 0 0 1 0;");
                    setOnMouseEntered(null);
                    setOnMouseExited(null);
                    setOnMouseClicked(null);
                } else {
                    String normalBg = (getIndex() % 2 == 0) ? "#0e1c40" : "#162550";
                    String normal   = "-fx-background-color: " + normalBg + "; -fx-border-color: #1A2F4A; -fx-border-width: 0 0 1 0;";
                    String hover    = "-fx-background-color: #1E4268; -fx-border-color: #1A2F4A; -fx-border-width: 0 0 1 0; -fx-cursor: hand;";
                    String selected = "-fx-background-color: #2A5090; -fx-border-color: #F5C518; -fx-border-width: 1; -fx-cursor: hand;";

                    if (isSelected()) setStyle(selected); else setStyle(normal);
                    setOnMouseEntered(e -> { if (!isSelected()) setStyle(hover); });
                    setOnMouseExited(e  -> { if (!isSelected()) setStyle(normal); else setStyle(selected); });
                    setOnMouseClicked(e -> {
                        tableProducts.getSelectionModel().select(getIndex());
                        tableProducts.refresh();
                        if (e.getClickCount() == 2) openProductDetail(item);
                    });
                }
            }
            {
                tableProducts.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> {
                    if (getIndex() < 0 || getItem() == null) return;
                    String normalBg = (getIndex() % 2 == 0) ? "#0e1c40" : "#162550";
                    String normal   = "-fx-background-color: " + normalBg + "; -fx-border-color: #1A2F4A; -fx-border-width: 0 0 1 0;";
                    String selected = "-fx-background-color: #2A5090; -fx-border-color: #F5C518; -fx-border-width: 1; -fx-cursor: hand;";
                    if (isSelected()) setStyle(selected); else setStyle(normal);
                });
            }
        });
    }

    private void setupTableColumns() {
        colStt.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getStt()).asObject());
        colStt.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(String.valueOf(v));
                setStyle("-fx-alignment: CENTER; -fx-text-fill: #FFFFFF;");
            }
        });
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colName.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle("-fx-alignment: CENTER_LEFT; -fx-text-fill: #FFFFFF; -fx-padding: 0 0 0 14;");
            }
        });
        colPrice.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedPrice()));
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle("-fx-alignment: CENTER; -fx-text-fill: #F5C518; -fx-font-weight: bold;");
            }
        });
        colSeller.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSeller()));
        colSeller.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle("-fx-alignment: CENTER; -fx-text-fill: #FFFFFF;");
            }
        });
        colTime.setCellValueFactory(d -> d.getValue().timeDisplayProperty());
        colTime.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                String color = v.equals("Đã kết thúc") ? "#888888" : "#FF6B6B";
                setStyle("-fx-alignment: CENTER; -fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });
    }

    private void setupHoverEffects() {
        addHover(btnTabDauGia,  TAB_INACTIVE, TAB_HOVER);
        addHover(btnTabAutoBid, TAB_INACTIVE, TAB_HOVER);
        addHover(btnQuayLai,    BTN_BACK_NORMAL, BTN_BACK_HOVER);
        String sn = "-fx-background-color: #2A5080; -fx-background-radius: 0 8 8 0; -fx-border-color: transparent; -fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-padding: 8 18 8 16; -fx-pref-height: 42; -fx-cursor: hand;";
        String sh = "-fx-background-color: #F5C518; -fx-background-radius: 0 8 8 0; -fx-border-color: transparent; -fx-text-fill: #0A1929; -fx-font-size: 14px; -fx-padding: 8 18 8 16; -fx-pref-height: 42; -fx-cursor: hand;";
        addHover(btnSearch, sn, sh);
        styleNavButtons();
    }

    private void addHover(Button b, String n, String h) {
        b.setOnMouseEntered(e -> b.setStyle(h));
        b.setOnMouseExited(e  -> b.setStyle(n));
    }

    private void styleNavButtons() {
        for (Button b : new Button[]{btnFirst, btnPrev, btnNext, btnLast}) {
            b.setStyle(PAGE_NAV_NORMAL);
            addHover(b, PAGE_NAV_NORMAL, PAGE_NAV_HOVER);
        }
    }

    private void startCountdown() {
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1),
                e -> allProducts.forEach(ProductRow::tick)));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void setupSearchListener() {
        txtSearch.textProperty().addListener((obs, o, n) -> filterProducts(n));
    }

    private void filterProducts(String kw) {
        if (kw == null || kw.trim().isEmpty()) {
            filteredProducts = new ArrayList<>(allProducts);
        } else {
            String lower = kw.trim().toLowerCase();
            filteredProducts = allProducts.stream()
                    .filter(p -> p.getName().toLowerCase().contains(lower) || p.getSeller().toLowerCase().contains(lower))
                    .collect(Collectors.toList());
        }
        currentPage = 1; updateTable();
    }

    private void updateTable() {
        int total = filteredProducts.size();
        int pages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (currentPage > pages) currentPage = pages;
        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);
        tableProducts.setItems(FXCollections.observableArrayList(filteredProducts.subList(from, to)));
        rebuildPageButtons(pages);
    }

    private void rebuildPageButtons(int total) {
        paginationBox.getChildren().removeIf(n -> n instanceof Button && "p".equals(((Button) n).getUserData()));
        int idx = paginationBox.getChildren().indexOf(btnPrev) + 1;
        for (int i = 1; i <= total; i++) {
            final int pg = i;
            Button b = new Button(String.valueOf(i));
            b.setUserData("p");
            b.setStyle(i == currentPage ? PAGE_NUM_ACTIVE : PAGE_NUM_NORMAL);
            if (i != currentPage) addHover(b, PAGE_NUM_NORMAL, PAGE_NAV_HOVER);
            b.setOnAction(e -> { currentPage = pg; updateTable(); });
            paginationBox.getChildren().add(idx++, b);
        }
    }

    @FXML void onSearch()    { filterProducts(txtSearch.getText()); }
    @FXML void onFirstPage() { currentPage = 1; updateTable(); }
    @FXML void onLastPage()  { currentPage = Math.max(1,(int)Math.ceil((double)filteredProducts.size()/PAGE_SIZE)); updateTable(); }
    @FXML void onPrevPage()  { if (currentPage > 1) { currentPage--; updateTable(); } }
    @FXML void onNextPage()  { int t = Math.max(1,(int)Math.ceil((double)filteredProducts.size()/PAGE_SIZE)); if (currentPage < t) { currentPage++; updateTable(); } }

    @FXML void onTabDanhSach() { /* đang ở đây */ }

    @FXML void onTabDauGia() {
        ProductRow selected = tableProducts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Thông báo"); a.setHeaderText(null);
            a.setContentText("Vui lòng chọn 1 sản phẩm để đấu giá.");
            a.showAndWait();
            return;
        }
        openProductDetail(selected);
    }

    @FXML void onTabAutoBid() { navigate("/fxml/bidding/AutoBidPanel.fxml"); }
    @FXML void onQuayLai()    { navigate("/fxml/bidding/MainDashboardUI.fxml"); }

    private void navigate(String fxmlPath) {
        try {
            if (countdownTimer != null) countdownTimer.stop();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) tableProducts.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void openProductDetail(ProductRow product) {
        try {
            if (countdownTimer != null) countdownTimer.stop();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/bidding/ProductDetailPanel.fxml"));
            javafx.scene.Parent root = loader.load();
            ProductDetailPanel controller = loader.getController();
            controller.loadProduct(
                    product.getStt(), product.getName(), product.getPrice(),
                    product.getSeller(), product.getRemainingSeconds(),
                    getImagePath(product.getName()));
            if (product.getItemId() != null) controller.setItemId(product.getItemId());
            javafx.stage.Stage stage = (javafx.stage.Stage) tableProducts.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private String getImagePath(String productName) {
        if (productName == null) return "/icons/sp1.png";
        String name = productName.toLowerCase();
        if (name.contains("iphone 15 pro max"))    return "/icons/sp1.png";
        if (name.contains("iphone 15 pro"))        return "/icons/sp2.png";
        if (name.contains("samsung galaxy s24"))   return "/icons/sp3.png";
        if (name.contains("xiaomi 14"))            return "/icons/sp4.png";
        if (name.contains("google pixel"))         return "/icons/sp5.png";
        if (name.contains("macbook pro m3 max"))   return "/icons/sp6.png";
        if (name.contains("dell xps"))             return "/icons/sp7.png";
        if (name.contains("lg gram"))              return "/icons/sp8.png";
        if (name.contains("asus rog"))             return "/icons/sp9.png";
        if (name.contains("sony wh"))              return "/icons/sp10.png";
        if (name.contains("airpods pro"))          return "/icons/sp11.png";
        if (name.contains("bose"))                 return "/icons/sp12.png";
        if (name.contains("apple watch ultra"))    return "/icons/sp13.png";
        if (name.contains("samsung galaxy watch")) return "/icons/sp14.png";
        if (name.contains("magic keyboard"))       return "/icons/sp15.png";
        if (name.contains("logitech"))             return "/icons/sp16.png";
        if (name.contains("ipad pro"))             return "/icons/sp17.png";
        if (name.contains("samsung tab"))          return "/icons/sp18.png";
        return "/icons/sp1.png";
    }
}