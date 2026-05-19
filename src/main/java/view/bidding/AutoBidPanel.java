package view.bidding;

import client.AuctionClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import common.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import view.common.SessionManager;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

public class AutoBidPanel implements Initializable {

    @FXML private Label    lblUserName;
    @FXML private Button   btnTabDanhSach, btnTabDauGia, btnTabAutoBid;
    @FXML private Button   btnQuayLai;

    @FXML private ComboBox<String>  cboProduct;
    @FXML private Label             lblCurrentPrice;
    @FXML private TextField         txtMaxPrice;
    @FXML private Button            btnToggle;
    @FXML private TextField         txtStep;
    @FXML private TextField         txtMaxTimes;
    @FXML private TextField         txtDelay;
    @FXML private Label             lblStepError;

    @FXML private TableView<AutoBidRow>            tableAutoBid;
    @FXML private TableColumn<AutoBidRow, Integer> colStt;
    @FXML private TableColumn<AutoBidRow, String>  colProduct;
    @FXML private TableColumn<AutoBidRow, String>  colCurrentPrice;
    @FXML private TableColumn<AutoBidRow, String>  colMaxPrice;
    @FXML private TableColumn<AutoBidRow, String>  colStep;
    @FXML private TableColumn<AutoBidRow, String>  colStatus;
    @FXML private TableColumn<AutoBidRow, String>  colTimes;
    @FXML private TableColumn<AutoBidRow, String>  colTime;
    @FXML private TableColumn<AutoBidRow, Void>    colActions;

    @FXML private Button btnRefresh, btnHistory, btnCreate;

    private static final String TAB_INACTIVE =
            "-fx-background-color: #0e1c40; -fx-text-fill: #CCDFF5; -fx-font-weight: bold;" +
                    " -fx-font-size: 13px; -fx-padding: 8 16 8 16; -fx-background-radius: 4 4 0 0;" +
                    " -fx-border-color: #1b2f5e; -fx-border-radius: 4 4 0 0; -fx-border-width: 1; -fx-cursor: hand;";
    private static final String TAB_HOVER =
            "-fx-background-color: #F5C518; -fx-text-fill: #0A1929; -fx-font-weight: bold;" +
                    " -fx-font-size: 13px; -fx-padding: 8 16 8 16; -fx-background-radius: 4 4 0 0;" +
                    " -fx-border-color: #F5C518; -fx-border-radius: 4 4 0 0; -fx-border-width: 1; -fx-cursor: hand;";

    private boolean autoBidOn = true;
    private int editingIndex = -1;

    // ĐÃ THÊM: lưu danh sách Item từ server để lấy itemId khi cần
    private final List<Item> serverItems = new ArrayList<>();

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public static class AutoBidRow {
        int stt;
        String product, currentPrice, maxPrice, step, status, times, time;
        String itemId; // ĐÃ THÊM: lưu itemId để gọi server

        public AutoBidRow(int stt, String product, String currentPrice, String maxPrice,
                          String step, String status, String times, String time) {
            this.stt = stt; this.product = product; this.currentPrice = currentPrice;
            this.maxPrice = maxPrice; this.step = step;
            this.status = status; this.times = times; this.time = time;
        }
        public int    getStt()            { return stt; }
        public String getProduct()        { return product; }
        public String getCurrentPrice()   { return currentPrice; }
        public String getMaxPrice()       { return maxPrice; }
        public String getStep()           { return step; }
        public String getStatus()         { return status; }
        public String getTimes()          { return times; }
        public String getTime()           { return time; }
        public void   setStatus(String s) { this.status = s; }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        tableAutoBid.sceneProperty().addListener((obs, o, scene) -> {
            if (scene != null) {
                java.net.URL css = getClass().getResource("/css/bidding_style.css");
                if (css != null) { String p = css.toExternalForm(); if (!scene.getStylesheets().contains(p)) scene.getStylesheets().add(p); }
            }
        });
        setupTableColumns();
        setupHovers();
        tableAutoBid.setItems(FXCollections.observableArrayList());
        styleHeaderTable();
        styleScrollBarAndCombo();
        loadProductsFromServer(); // ĐÃ ĐỔI: load từ server thay vì hardcode
        loadAutoBidFromServer(); // ĐÃ THÊM: load auto-bid từ cache
    }

    // ĐÃ THÊM: Load danh sách sản phẩm từ server vào ComboBox
    private void loadProductsFromServer() {
        new Thread(() -> {
            try {
                AuctionClient client = SessionManager.getClient();
                if (client == null) throw new Exception("Chưa có kết nối");
                ResponseMessage resp = client.getActiveItems();
                if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                    java.lang.reflect.Type listType = new TypeToken<List<Item>>(){}.getType();
                    List<Item> items = gson.fromJson(gson.toJson(resp.getData()), listType);
                    if (items != null && !items.isEmpty()) {
                        serverItems.clear();
                        serverItems.addAll(items);
                        List<String> names = new ArrayList<>();
                        for (Item item : items) names.add(item.getName());
                        Platform.runLater(() -> {
                            cboProduct.getItems().setAll(names);
                            setupComboBoxListener();
                            System.out.println("✅ Loaded " + items.size() + " sản phẩm vào AutoBid ComboBox");
                        });
                        return;
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠ Dùng danh sách sản phẩm mẫu: " + e.getMessage());
            }
            // Fallback: dùng danh sách cứng
            Platform.runLater(this::loadSampleProducts);
        }).start();
    }

    private void loadSampleProducts() {
        String[] PRODUCTS = {
                "iPhone 15 Pro Max 1TB", "iPhone 15 Pro 512GB",
                "Samsung Galaxy S24 Ultra", "Xiaomi 14 Ultra",
                "Google Pixel 8 Pro", "MacBook Pro M3 Max",
                "Dell XPS 15", "LG Gram 17",
                "Asus ROG Zephyrus G14", "Sony WH-1000XM5",
                "AirPods Pro 2", "Bose QC Ultra",
                "Apple Watch Ultra 2", "Samsung Galaxy Watch 6 Classic",
                "Magic Keyboard", "Logitech MX Master 3S",
                "iPad Pro 12.9 inch M2", "Samsung Tab S9 Ultra"
        };
        long[] PRICES = {
                25_000_000L, 22_000_000L, 20_000_000L, 15_000_000L,
                18_000_000L, 45_000_000L, 35_000_000L, 28_000_000L,
                32_000_000L,  5_000_000L,  4_500_000L,  6_000_000L,
                15_000_000L,  7_000_000L,  3_000_000L,  2_000_000L,
                28_000_000L, 22_000_000L
        };
        cboProduct.getItems().setAll(PRODUCTS);
        // Lưu vào serverItems dạng fake để dùng chung logic
        serverItems.clear();
        for (int i = 0; i < PRODUCTS.length; i++) {
            Item item = new Item();
            item.setName(PRODUCTS[i]);
            item.setCurrentPrice(PRICES[i]);
            serverItems.add(item);
        }
        setupComboBoxListener();
    }

    private void setupComboBoxListener() {
        cboProduct.getSelectionModel().selectedIndexProperty().addListener((obs, o, idx) -> {
            int i = idx.intValue();
            if (i >= 0 && i < serverItems.size()) {
                lblCurrentPrice.setText(formatPrice((long) serverItems.get(i).getCurrentPrice()));
            }
        });
    }

    private void resetForm() {
        editingIndex = -1;
        cboProduct.getSelectionModel().clearSelection();
        lblCurrentPrice.setText("0");
        txtMaxPrice.clear();
        txtStep.clear();
        txtMaxTimes.clear();
        txtDelay.clear();
        if (lblStepError != null) lblStepError.setText("");
        btnCreate.setText("＋  Tạo auto-bid mới");
    }

    private void styleScrollBarAndCombo() {
        Platform.runLater(() -> {
            cboProduct.setCellFactory(lv -> new ListCell<String>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); setStyle(""); return; }
                    setText(item);
                    setStyle("-fx-text-fill: #FFFFFF; -fx-background-color: #162550; -fx-font-size: 13px;");
                }
                { setOnMouseEntered(e -> setStyle("-fx-text-fill: #FFFFFF; -fx-background-color: #1E4268; -fx-font-size: 13px;"));
                    setOnMouseExited(e  -> setStyle("-fx-text-fill: #FFFFFF; -fx-background-color: #162550; -fx-font-size: 13px;")); }
            });
            cboProduct.setButtonCell(new ListCell<String>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item);
                    setStyle("-fx-text-fill: #FFFFFF; -fx-background-color: transparent; -fx-font-size: 13px;");
                }
            });
        });
    }

    private void setupTableColumns() {
        tableAutoBid.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colStt.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(d.getValue().getStt()).asObject());
        colStt.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean e) {
                super.updateItem(v, e);
                if (e || v == null) { setText(null); setStyle(""); return; }
                setText(String.valueOf(v));
                setStyle("-fx-alignment: CENTER; -fx-text-fill: #C0D8F0; -fx-font-weight: bold;");
            }
        });

        colProduct.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getProduct()));
        colProduct.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean e) {
                super.updateItem(v, e);
                if (e || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-alignment: CENTER_LEFT; -fx-padding: 0 0 0 10;");
            }
        });

        colCurrentPrice.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getCurrentPrice()));
        colCurrentPrice.setCellFactory(c -> priceCell());
        colMaxPrice.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getMaxPrice()));
        colMaxPrice.setCellFactory(c -> priceCell());

        colStep.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getStep()));
        colStep.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean e) {
                super.updateItem(v, e);
                if (e || v == null) { setText(null); setStyle(""); return; }
                setText(v); setStyle("-fx-text-fill: #C0D8F0; -fx-alignment: CENTER;");
            }
        });

        colStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getStatus()));
        colStatus.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean e) {
                super.updateItem(v, e);
                if (e || v == null) { setText(null); setGraphic(null); setStyle(""); return; }
                String color = switch (v) {
                    case "Đang hoạt động" -> "#22c55e";
                    case "Tạm dừng"       -> "#f59e0b";
                    default               -> "#ef4444";
                };
                Label lbl = new Label(v);
                lbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold;");
                setGraphic(lbl); setText(null);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        colTimes.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getTimes()));
        colTimes.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean e) {
                super.updateItem(v, e);
                if (e || v == null) { setText(null); setStyle(""); return; }
                setText(v); setStyle("-fx-text-fill: #C0D8F0; -fx-alignment: CENTER;");
            }
        });

        colTime.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getTime()));
        colTime.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean e) {
                super.updateItem(v, e);
                if (e || v == null) { setText(null); setStyle(""); return; }
                setText(v); setStyle("-fx-text-fill: #8AADD0; -fx-alignment: CENTER;");
            }
        });

        colActions.setCellFactory(c -> new TableCell<>() {
            private final String BTN_EDIT = "-fx-background-color: #F5C518; -fx-background-radius: 5; -fx-border-color: transparent; -fx-cursor: hand; -fx-pref-width: 28; -fx-pref-height: 28; -fx-min-width: 28; -fx-min-height: 28; -fx-padding: 0;";
            private final String BTN_DEL  = "-fx-background-color: #ef4444; -fx-background-radius: 5; -fx-border-color: transparent; -fx-cursor: hand; -fx-pref-width: 28; -fx-pref-height: 28; -fx-min-width: 28; -fx-min-height: 28; -fx-padding: 0;";
            private final Button btnEdit   = new Button();
            private final Button btnDelete = new Button();
            private final HBox   box       = new HBox(5, btnEdit, btnDelete);
            {
                javafx.scene.image.ImageView iEdit = new javafx.scene.image.ImageView();
                iEdit.setFitWidth(14); iEdit.setFitHeight(14); iEdit.setPreserveRatio(true);
                try { java.net.URL u = getClass().getResource("/icons/edit.png"); if (u != null) iEdit.setImage(new javafx.scene.image.Image(u.toExternalForm())); } catch (Exception ignored) {}
                btnEdit.setGraphic(iEdit); btnEdit.setStyle(BTN_EDIT);
                btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(BTN_EDIT.replace("#F5C518","#d4aa00")));
                btnEdit.setOnMouseExited(e  -> btnEdit.setStyle(BTN_EDIT));

                javafx.scene.image.ImageView iDel = new javafx.scene.image.ImageView();
                iDel.setFitWidth(14); iDel.setFitHeight(14); iDel.setPreserveRatio(true);
                try { java.net.URL u = getClass().getResource("/icons/delete.png"); if (u != null) iDel.setImage(new javafx.scene.image.Image(u.toExternalForm())); } catch (Exception ignored) {}
                btnDelete.setGraphic(iDel); btnDelete.setStyle(BTN_DEL);
                btnDelete.setOnMouseEntered(e -> btnDelete.setStyle(BTN_DEL.replace("#ef4444","#dc2626")));
                btnDelete.setOnMouseExited(e  -> btnDelete.setStyle(BTN_DEL));

                box.setStyle("-fx-alignment: CENTER;");

                btnEdit.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx < 0 || idx >= tableAutoBid.getItems().size()) return;
                    AutoBidRow row = tableAutoBid.getItems().get(idx);
                    editingIndex = idx;
                    cboProduct.getSelectionModel().select(row.getProduct());
                    txtMaxPrice.setText(row.getMaxPrice().replaceAll("[^0-9]",""));
                    txtStep.setText(row.getStep().replaceAll("[^0-9]",""));
                    String[] tp = row.getTimes().split("/");
                    txtMaxTimes.setText(tp.length > 1 ? tp[1].trim() : "20");
                    btnCreate.setText("💾  Lưu thay đổi");
                    row.setStatus("Tạm dừng");
                    tableAutoBid.refresh();
                });

                btnDelete.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx < 0) return;
                    AutoBidRow row = tableAutoBid.getItems().get(idx);

                    // Gọi server HỦY auto-bid (xóa hẳn khỏi database)
                    if (row.itemId != null) {
                        final String cancelItemId = row.itemId;
                        new Thread(() -> {
                            try {
                                AuctionClient client = view.common.SessionManager.getClient();
                                if (client != null) {
                                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                                    data.put("itemId", cancelItemId);
                                    ResponseMessage resp = client.sendRequest(CommandType.CANCEL_AUTO_BID, data);
                                    System.out.println("✅ Hủy auto-bid: " +
                                            (resp != null ? resp.getMessage() : "Không phản hồi"));
                                }
                            } catch (Exception ex) {
                                System.out.println("⚠ Lỗi hủy auto-bid: " + ex.getMessage());
                            }
                        }).start();
                    }

                    // Xóa khỏi cache
                    User usr = SessionManager.getCurrentUser();
                    if (usr != null && row.itemId != null) {
                        SessionManager.removeAutoBidCache(usr.getUsername(), row.itemId);
                    }
                    tableAutoBid.getItems().remove(idx);
                    for (int i = 0; i < tableAutoBid.getItems().size(); i++)
                        tableAutoBid.getItems().get(i).stt = i + 1;
                    tableAutoBid.refresh();
                    if (editingIndex == idx) resetForm();
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableAutoBid.setRowFactory(tv -> new TableRow<AutoBidRow>() {
            @Override protected void updateItem(AutoBidRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("-fx-background-color: #0e1c40; -fx-border-color: transparent;");
                    setOnMouseEntered(null); setOnMouseExited(null);
                } else {
                    String bg = (getIndex() % 2 == 0) ? "#0e1c40" : "#11224a";
                    String normal = "-fx-background-color: " + bg + "; -fx-border-color: #1b2f5e; -fx-border-width: 0 0 1 0;";
                    String hover  = "-fx-background-color: #1E4268; -fx-border-color: #1b2f5e; -fx-border-width: 0 0 1 0;";
                    setStyle(normal);
                    setOnMouseEntered(ev -> setStyle(hover));
                    setOnMouseExited(ev  -> setStyle(normal));
                }
            }
        });
    }

    private TableCell<AutoBidRow, String> priceCell() {
        return new TableCell<>() {
            @Override protected void updateItem(String v, boolean e) {
                super.updateItem(v, e);
                if (e || v == null) { setText(null); setStyle(""); return; }
                setText(v); setStyle("-fx-text-fill: #F5C518; -fx-font-weight: bold; -fx-alignment: CENTER;");
            }
        };
    }

    private void styleHeaderTable() {
        Platform.runLater(() -> Platform.runLater(() -> {
            tableAutoBid.lookupAll(".column-header").forEach(node ->
                    node.setStyle("-fx-background-color: #162550; -fx-border-color: transparent #2A5090 transparent transparent; -fx-border-width: 0 1 0 0;"));
            tableAutoBid.lookupAll(".column-header .label").forEach(node ->
                    node.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 12px;"));
            tableAutoBid.lookupAll(".column-header-background").forEach(node ->
                    node.setStyle("-fx-background-color: #162550;"));
            tableAutoBid.lookupAll(".filler").forEach(node ->
                    node.setStyle("-fx-background-color: #162550;"));
        }));
    }

    private void setupHovers() {
        addHover(btnTabDanhSach, TAB_INACTIVE, TAB_HOVER);
        addHover(btnTabDauGia,   TAB_INACTIVE, TAB_HOVER);
        String backN = "-fx-background-color: #162550; -fx-background-radius: 6; -fx-border-color: #2A5090; -fx-border-radius: 6; -fx-border-width: 1; -fx-text-fill: #CCDFF5; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 16 8 14; -fx-cursor: hand;";
        String backH = "-fx-background-color: #F5C518; -fx-background-radius: 6; -fx-border-color: #F5C518; -fx-border-radius: 6; -fx-border-width: 1; -fx-text-fill: #0A1929; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 16 8 14; -fx-cursor: hand;";
        addHover(btnQuayLai, backN, backH);
        String btnN = "-fx-background-color: #162550; -fx-background-radius: 6; -fx-border-color: #2A5090; -fx-border-radius: 6; -fx-border-width: 1; -fx-text-fill: #CCDFF5; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 18 8 14; -fx-cursor: hand;";
        String btnH = "-fx-background-color: #1E3A5C; -fx-background-radius: 6; -fx-border-color: #4A7AAC; -fx-border-radius: 6; -fx-border-width: 1; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 18 8 14; -fx-cursor: hand;";
        addHover(btnRefresh, btnN, btnH);
        addHover(btnHistory,  btnN, btnH);
        String createN = "-fx-background-color: #22c55e; -fx-background-radius: 6; -fx-border-color: transparent; -fx-border-radius: 6; -fx-text-fill: #0c1535; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 18 8 14; -fx-cursor: hand;";
        String createH = "-fx-background-color: #16a34a; -fx-background-radius: 6; -fx-border-color: transparent; -fx-border-radius: 6; -fx-text-fill: #0c1535; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 18 8 14; -fx-cursor: hand;";
        addHover(btnCreate, createN, createH);
    }

    private void addHover(Button b, String n, String h) {
        b.setOnMouseEntered(e -> b.setStyle(h));
        b.setOnMouseExited(e  -> b.setStyle(n));
    }

    private static String formatPrice(long p) {
        return String.format("%,d", p).replace(",", ".") + " đ";
    }

    @FXML void onTabDanhSach() { navigate("/fxml/bidding/ProductListPanel.fxml"); }
    @FXML void onTabDauGia()   { navigate("/fxml/bidding/ProductDetailPanel.fxml"); }
    @FXML void onTabAutoBid()  { /* đang ở đây */ }
    @FXML void onQuayLai()     { navigate("/fxml/bidding/MainDashboardUI.fxml"); }

    @FXML void onToggleAutoBid() {
        boolean formEmpty = cboProduct.getValue() == null || cboProduct.getValue().isEmpty();
        autoBidOn = !autoBidOn;

        if (formEmpty && !tableAutoBid.getItems().isEmpty()) {
            String newStatus = autoBidOn ? "Đang hoạt động" : "Tạm dừng";
            for (AutoBidRow row : tableAutoBid.getItems()) {
                if (!row.getStatus().equals("Đã dừng")) row.setStatus(newStatus);
            }
            tableAutoBid.refresh();
        }

        if (autoBidOn) {
            btnToggle.setText("BẬT");
            btnToggle.setStyle("-fx-background-color: #22c55e; -fx-background-radius: 20; -fx-border-color: transparent; -fx-text-fill: #0c1535; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 7 10 7 10; -fx-cursor: hand; -fx-pref-height: 36;");
        } else {
            btnToggle.setText("TẮT");
            btnToggle.setStyle("-fx-background-color: #374151; -fx-background-radius: 20; -fx-border-color: transparent; -fx-text-fill: #9ca3af; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 7 10 7 10; -fx-cursor: hand; -fx-pref-height: 36;");
        }
    }

    @FXML void onRefresh() {
        loadProductsFromServer();
        styleHeaderTable();
    }

    @FXML void onHistory() { System.out.println("[AutoBidPanel] onHistory()"); }

    @FXML void onCreateAutoBid() {
        String product = cboProduct.getValue();
        if (product == null || product.isEmpty()) { showAlert("Vui lòng chọn sản phẩm trước."); return; }
        String maxPriceStr = txtMaxPrice.getText().trim().replaceAll("[^0-9]", "");
        if (maxPriceStr.isEmpty()) { showAlert("Vui lòng nhập giá tối đa."); return; }

        try {
            long maxPrice = Long.parseLong(maxPriceStr);
            String stepStr = txtStep.getText().trim().replaceAll("[^0-9]", "");
            long stepVal = stepStr.isEmpty() ? 0 : Long.parseLong(stepStr);
            if (stepVal < 500_000L) {
                lblStepError.setText("⚠ Bước giá tối thiểu là 500.000 đ");
                return;
            } else {
                lblStepError.setText("");
            }
            String times = txtMaxTimes.getText().trim().isEmpty() ? "20" : txtMaxTimes.getText().trim().replaceAll("[^0-9]","");

            // Lấy item từ serverItems
            int selectedIdx = cboProduct.getSelectionModel().getSelectedIndex();
            String itemId = null;
            String currentP = "0 đ";
            if (selectedIdx >= 0 && selectedIdx < serverItems.size()) {
                Item selectedItem = serverItems.get(selectedIdx);
                itemId = selectedItem.getId();
                currentP = formatPrice((long) selectedItem.getCurrentPrice());
            }

            // ĐÃ THÊM: Gọi server setAutoBid nếu có itemId
            final String finalItemId = itemId;
            final long finalMaxPrice = maxPrice;
            final String finalCurrentP = currentP;
            final long finalStep = stepVal;
            final String finalTimes = times;
            final String finalProduct = product;

            if (finalItemId != null) {
                new Thread(() -> {
                    try {
                        AuctionClient client = SessionManager.getClient();
                        User user = SessionManager.getCurrentUser();
                        if (client == null || user == null) throw new Exception("Chưa đăng nhập");

                        // Huyền đã sửa: dùng setAutoBid() thay vì sendRequest trực tiếp
                        ResponseMessage resp = client.setAutoBid(finalItemId, finalMaxPrice);
                        Platform.runLater(() -> {
                            if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                                addRowToTable(finalProduct, finalCurrentP, finalMaxPrice,
                                        finalStep, finalTimes, finalItemId);
                                // Lưu vào cache để dùng khi chuyển acc
                                User usr = SessionManager.getCurrentUser();
                                if (usr != null) {
                                    SessionManager.addAutoBidCache(usr.getUsername(),
                                            finalItemId, finalProduct,
                                            formatPrice(finalMaxPrice), finalCurrentP);
                                }
                                System.out.println("✅ Auto-bid đã được lưu lên server");
                            } else {
                                showAlert("Lỗi server: " + (resp != null ? resp.getMessage() : "Không phản hồi"));
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            // Fallback: vẫn thêm vào bảng local nếu server lỗi
                            addRowToTable(finalProduct, finalCurrentP, finalMaxPrice, finalStep, finalTimes, finalItemId);
                            System.out.println("⚠ Lưu local (server lỗi): " + e.getMessage());
                        });
                    }
                }).start();
            } else {
                // Không có itemId (dữ liệu mẫu) → thêm local
                addRowToTable(finalProduct, finalCurrentP, finalMaxPrice, finalStep, finalTimes, null);
            }

        } catch (NumberFormatException ex) { showAlert("Giá không hợp lệ."); }
    }

    // ĐÃ THÊM tham số itemId để lưu vào row
    private void addRowToTable(String product, String currentP, long maxPrice,
                               long stepVal, String times, String itemId) {
        if (editingIndex >= 0 && editingIndex < tableAutoBid.getItems().size()) {
            AutoBidRow row = tableAutoBid.getItems().get(editingIndex);
            row.product      = product;
            row.currentPrice = currentP;
            row.maxPrice     = formatPrice(maxPrice);
            row.step         = formatPrice(stepVal);
            row.status       = autoBidOn ? "Đang hoạt động" : "Tạm dừng";
            row.times        = "0 / " + times;
            row.time         = "Vừa xong";
            row.itemId       = itemId; // ĐÃ THÊM: lưu itemId để xóa được
            tableAutoBid.refresh();
        } else {
            int newStt = tableAutoBid.getItems().size() + 1;
            AutoBidRow row = new AutoBidRow(newStt, product, currentP,
                    formatPrice(maxPrice), formatPrice(stepVal),
                    autoBidOn ? "Đang hoạt động" : "Tạm dừng",
                    "0 / " + times, "Vừa xong");
            row.itemId = itemId; // ĐÃ THÊM: lưu itemId để xóa được
            tableAutoBid.getItems().add(row);
        }
        resetForm();
    }

    private void navigate(String fxmlPath) {
        try {
            URL res = getClass().getResource(fxmlPath);
            if (res == null) { System.out.println("Chưa có: " + fxmlPath); return; }
            FXMLLoader loader = new FXMLLoader(res);
            Parent root = loader.load();
            Stage stage = (Stage) tableAutoBid.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight()));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ĐÃ THÊM: load auto-bid từ cache khi mở màn hình
    private void loadAutoBidFromServer() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;
        java.util.List<view.common.SessionManager.AutoBidCache> cache =
                SessionManager.getAutoBidCache(user.getUsername());
        if (cache.isEmpty()) return;
        tableAutoBid.getItems().clear();
        int stt = 1;
        for (view.common.SessionManager.AutoBidCache c : cache) {
            AutoBidRow row = new AutoBidRow(stt++, c.itemName,
                    c.currentPrice, c.maxPrice,
                    "500.000 đ", "Đang hoạt động", "0 / 20", "Đang chạy");
            row.itemId = c.itemId;
            tableAutoBid.getItems().add(row);
        }
        System.out.println("✅ Loaded " + cache.size() + " auto-bid từ cache");
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Thông báo"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}