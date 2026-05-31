package com.auction.ui.admin;

import com.auction.common.model.bidding.Bid;
import com.auction.common.model.item.Item;
import com.auction.common.model.item.ItemStatus;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AdminTestController {

    @FXML private TableView<Item> tvProducts;
    @FXML private TableColumn<Item, Integer> colSTT;
    @FXML private TableColumn<Item, String> colId;
    @FXML private TableColumn<Item, String> colName;
    @FXML private TableColumn<Item, Double> colStartPrice;
    @FXML private TableColumn<Item, Double> colCurrentPrice;
    @FXML private TableColumn<Item, String> colSeller;
    @FXML private TableColumn<Item, String> colBidder;
    @FXML private TableColumn<Item, String> colEndTime;
    @FXML private TableColumn<Item, String> colStatus;
    @FXML private TableColumn<Item, Integer> colBidCount;
    @FXML private Label lblStatus;

    @FXML private TextField txtName;
    @FXML private TextField txtPrice;
    @FXML private ComboBox<Integer> cbDays;
    @FXML private ComboBox<String> cbCategory;

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbFilterStatus;
    @FXML private ComboBox<String> cbFilterPrice;

    @FXML private Button btnAdd;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private Button btnRefresh;

    private ObservableList<Item> masterList = FXCollections.observableArrayList();
    private FilteredList<Item> filteredList;

    private final ItemRepository itemRepo = ItemRepository.getInstance();
    private final BidRepository bidRepo = BidRepository.getInstance();
    private final UserRepository userRepo = UserRepository.getInstance();

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter END_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        System.out.println("📋 AdminTestController initialized");

        setupTableColumns();
        setupPriceFormat();
        setupStatusFormat();
        setupEndTimeFormat();

        filteredList = new FilteredList<>(masterList, p -> true);
        tvProducts.setItems(filteredList);

        System.out.println("✅ TableView setup completed");

        setupComboBoxes();
        setupFilterListeners();

        // Kiểm tra kết nối database
        try {
            int itemCount = itemRepo.count();
            System.out.println("📊 Database has " + itemCount + " items");
            if (itemCount == 0) {
                lblStatus.setText("⚠️ Database trống! Hãy chạy DataInitializer hoặc restart server để tạo dữ liệu mẫu.");
            } else {
                lblStatus.setText("✅ Connected to database (" + itemCount + " items)");
            }
            loadItems();
        } catch (SQLException e) {
            System.err.println("❌ Database connection error: " + e.getMessage());
            e.printStackTrace();
            lblStatus.setText("❌ Database connection failed: " + e.getMessage());
        }

        tvProducts.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            boolean hasSelection = selected != null;
            btnEdit.setDisable(!hasSelection);
            btnDelete.setDisable(!hasSelection);
            if (hasSelection) {
                loadItemToForm(selected);
            }
        });

        btnEdit.setDisable(true);
        btnDelete.setDisable(true);

        startRealtimeClock();
        startRealtimeRefresh();
    }

    private void setupTableColumns() {
        // STT column - dùng cellFactory để tính số thứ tự
        colSTT.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(filteredList.indexOf(cellData.getValue()) + 1).asObject()
        );

        // Các column khác dùng PropertyValueFactory
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        colBidCount.setCellValueFactory(new PropertyValueFactory<>("bidCount"));
        colStatus.setCellValueFactory(cellData -> {
            Item item = cellData.getValue();
            String status = item.getStatus() != null ? item.getStatus().name() : "UNKNOWN";
            return new SimpleStringProperty(status);
        });

        // Bidder column - xử lý null
        colBidder.setCellValueFactory(cellData -> {
            String bidder = cellData.getValue().getCurrentBidderName();
            return new SimpleStringProperty(bidder == null || bidder.isEmpty() ? "Chưa có" : bidder);
        });

        System.out.println("✅ Table columns configured");
    }

    private void setupEndTimeFormat() {
        colEndTime.setCellValueFactory(cellData -> {
            Item item = cellData.getValue();
            if (item != null && item.getEndTime() != null) {
                return new SimpleStringProperty(item.getEndTime().format(END_TIME_FORMAT));
            }
            return new SimpleStringProperty("N/A");
        });

        colEndTime.setCellFactory(col -> new TableCell<Item, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                Item product = getTableView().getItems().get(getIndex());
                if (product != null && product.getEndTime() != null) {
                    if (product.isActive()) {
                        long hoursLeft = LocalDateTime.now().until(product.getEndTime(), ChronoUnit.HOURS);
                        if (hoursLeft < 24) {
                            setText(item + " ⏰");
                            setStyle("-fx-text-fill: #e65100; -fx-font-weight: bold;");
                        } else {
                            setText(item);
                            setStyle("-fx-text-fill: #2e7d32;");
                        }
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: #757575;");
                    }
                } else {
                    setText(item);
                }
            }
        });
    }

    private void setupPriceFormat() {
        colStartPrice.setCellFactory(col -> new TableCell<Item, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText((empty || price == null) ? null : String.format("%,.0f ₫", price));
            }
        });

        colCurrentPrice.setCellFactory(col -> new TableCell<Item, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else if (price > 0) {
                    setText(String.format("%,.0f ₫", price));
                } else {
                    setText("Chưa có");
                }
            }
        });
    }

    private void setupStatusFormat() {
        colStatus.setCellFactory(col -> new TableCell<Item, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("ACTIVE".equals(status)) {
                        setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                    } else if ("ENDED".equals(status)) {
                        setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #757575;");
                    }
                }
            }
        });
    }

    private void setupComboBoxes() {
        for (int i = 1; i <= 30; i++) cbDays.getItems().add(i);
        cbDays.setValue(7);
        cbCategory.getItems().addAll("Điện thoại", "Laptop", "Đồng hồ", "Phụ kiện", "Khác");
        cbCategory.setValue("Điện thoại");
        cbFilterStatus.getItems().addAll("Tất cả", "ACTIVE", "ENDED");
        cbFilterStatus.setValue("Tất cả");
        cbFilterPrice.getItems().addAll("Tất cả", "Dưới 1 triệu", "1 - 10 triệu", "10 - 50 triệu", "Trên 50 triệu");
        cbFilterPrice.setValue("Tất cả");
    }

    private void setupFilterListeners() {
        txtSearch.textProperty().addListener((obs, old, val) -> applyFilter());
        cbFilterStatus.valueProperty().addListener((obs, old, val) -> applyFilter());
        cbFilterPrice.valueProperty().addListener((obs, old, val) -> applyFilter());
    }

    private void applyFilter() {
        String keyword = txtSearch.getText().toLowerCase().trim();
        String status = cbFilterStatus.getValue();
        String priceRange = cbFilterPrice.getValue();

        filteredList.setPredicate(item -> {
            if (!keyword.isEmpty() && !item.getName().toLowerCase().contains(keyword)) return false;
            if (status != null && !status.equals("Tất cả") && !item.getStatus().name().equals(status)) return false;
            if (priceRange != null && !priceRange.equals("Tất cả")) {
                double price = item.getCurrentPrice();
                switch (priceRange) {
                    case "Dưới 1 triệu": if (price >= 1_000_000) return false; break;
                    case "1 - 10 triệu": if (price < 1_000_000 || price > 10_000_000) return false; break;
                    case "10 - 50 triệu": if (price < 10_000_000 || price > 50_000_000) return false; break;
                    case "Trên 50 triệu": if (price <= 50_000_000) return false; break;
                }
            }
            return true;
        });

        Platform.runLater(() -> {
            tvProducts.refresh();
            updateStatusMessage();
        });
    }

    @FXML private void handleFilter() { applyFilter(); }
    @FXML private void handleClearFilter() {
        txtSearch.clear();
        cbFilterStatus.setValue("Tất cả");
        cbFilterPrice.setValue("Tất cả");
        applyFilter();
    }

    private void loadItems() {
        System.out.println("📡 Admin: Loading items from database...");
        new Thread(() -> {
            try {
                List<Item> items = itemRepo.findAll();
                System.out.println("📡 Admin: Loaded " + items.size() + " items from database");

                // Debug: in ra 3 sản phẩm đầu
                for (int i = 0; i < Math.min(3, items.size()); i++) {
                    System.out.println("   - " + items.get(i).getName() + " | " + items.get(i).getCurrentPrice());
                }

                Platform.runLater(() -> {
                    masterList.setAll(items);
                    System.out.println("✅ masterList size: " + masterList.size());
                    updateStatusMessage();
                    tvProducts.refresh();

                    if (items.isEmpty()) {
                        lblStatus.setText("⚠️ Không có sản phẩm nào! Hãy chạy DataInitializer để tạo dữ liệu mẫu.");
                    } else {
                        lblStatus.setText("✅ Đã tải " + items.size() + " sản phẩm");
                    }
                });
            } catch (SQLException e) {
                System.err.println("❌ Admin: Error loading items: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> lblStatus.setText("❌ Lỗi database: " + e.getMessage()));
            }
        }).start();
    }

    private void loadItemToForm(Item item) {
        txtName.setText(item.getName());
        txtPrice.setText(String.valueOf((long) item.getStartingPrice()));
    }

    private void clearForm() {
        txtName.clear();
        txtPrice.clear();
        cbDays.setValue(7);
        cbCategory.setValue("Điện thoại");
    }

    private void updateStatusMessage() {
        String time = LocalDateTime.now().format(TIME_FORMAT);
        Platform.runLater(() ->
                lblStatus.setText("🕐 " + time + " | 📦 " + filteredList.size() + "/" + masterList.size() + " sản phẩm")
        );
    }

    private void startRealtimeClock() {
        Thread timerThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    Platform.runLater(() -> {
                        updateStatusMessage();
                        tvProducts.refresh();
                    });
                } catch (InterruptedException e) { break; }
            }
        });
        timerThread.setDaemon(true);
        timerThread.start();
    }

    private void startRealtimeRefresh() {
        Thread refreshThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000);
                    Platform.runLater(this::loadItems);
                } catch (InterruptedException e) { break; }
            }
        });
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    @FXML private void handleRefresh() { loadItems(); clearForm(); }

    @FXML
    private void handleAddItem() {
        String name = txtName.getText().trim();
        String priceText = txtPrice.getText().trim();

        if (name.isEmpty() || priceText.isEmpty()) {
            showAlert("Vui lòng nhập đầy đủ thông tin!", Alert.AlertType.WARNING);
            return;
        }

        try {
            double price = Double.parseDouble(priceText);
            if (price <= 0) throw new NumberFormatException();

            Item item = new Item();
            item.setId(UUID.randomUUID().toString());
            item.setName(name);
            item.setDescription(cbCategory.getValue() + " - Sản phẩm chất lượng cao");
            item.setStartingPrice(price);
            item.setCurrentPrice(price);
            item.setStartTime(LocalDateTime.now());
            item.setEndTime(LocalDateTime.now().plusDays(cbDays.getValue()));
            item.setStatus(ItemStatus.ACTIVE);
            item.setSellerName("admin");
            item.setSellerId("admin");

            new Thread(() -> {
                try {
                    itemRepo.save(item);
                    Platform.runLater(() -> {
                        loadItems();
                        clearForm();
                        showAlert("✅ Thêm thành công!", Alert.AlertType.INFORMATION);
                    });
                } catch (SQLException e) {
                    Platform.runLater(() -> showAlert("❌ Lỗi DB: " + e.getMessage(), Alert.AlertType.ERROR));
                }
            }).start();
        } catch (NumberFormatException e) {
            showAlert("Giá không hợp lệ!", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleEditItem() {
        Item selected = tvProducts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Vui lòng chọn sản phẩm cần sửa!", Alert.AlertType.WARNING);
            return;
        }

        String name = txtName.getText().trim();
        String priceText = txtPrice.getText().trim();

        if (name.isEmpty() || priceText.isEmpty()) {
            showAlert("Vui lòng nhập đầy đủ thông tin!", Alert.AlertType.WARNING);
            return;
        }

        try {
            double price = Double.parseDouble(priceText);
            if (price <= 0) throw new NumberFormatException();

            selected.setName(name);
            selected.setStartingPrice(price);
            selected.setCurrentPrice(price);

            new Thread(() -> {
                try {
                    if (itemRepo.update(selected)) {
                        Platform.runLater(() -> {
                            loadItems();
                            clearForm();
                            showAlert("✏️ Cập nhật thành công!", Alert.AlertType.INFORMATION);
                        });
                    }
                } catch (SQLException e) {
                    Platform.runLater(() -> showAlert("❌ Lỗi DB: " + e.getMessage(), Alert.AlertType.ERROR));
                }
            }).start();
        } catch (NumberFormatException e) {
            showAlert("Giá không hợp lệ!", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleDeleteItem() {
        Item selected = tvProducts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Vui lòng chọn sản phẩm cần xóa!", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText(null);
        confirm.setContentText("Xóa sản phẩm: " + selected.getName() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    if (itemRepo.deleteById(selected.getId())) {
                        Platform.runLater(() -> {
                            masterList.remove(selected);
                            clearForm();
                            updateStatusMessage();
                            showAlert("🗑️ Xóa thành công!", Alert.AlertType.INFORMATION);
                        });
                    } else {
                        Platform.runLater(() -> showAlert("❌ Xóa thất bại!", Alert.AlertType.ERROR));
                    }
                } catch (SQLException e) {
                    Platform.runLater(() -> showAlert("❌ Lỗi DB: " + e.getMessage(), Alert.AlertType.ERROR));
                }
            }).start();
        }
    }

    @FXML private void handleForceEnd() {
        Item selected = tvProducts.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Chọn sản phẩm!", Alert.AlertType.WARNING); return; }
        if (!selected.isActive()) { showAlert("Chỉ kết thúc phiên đang hoạt động!", Alert.AlertType.WARNING); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setContentText("Kết thúc: " + selected.getName() + "?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            new Thread(() -> {
                try {
                    itemRepo.forceEndAuction(selected.getId());
                    selected.setStatus(ItemStatus.ENDED);
                    Platform.runLater(() -> { tvProducts.refresh(); showAlert("🏁 Đã kết thúc!", Alert.AlertType.INFORMATION); });
                } catch (SQLException e) {
                    Platform.runLater(() -> showAlert("❌ Lỗi: " + e.getMessage(), Alert.AlertType.ERROR));
                }
            }).start();
        }
    }

    @FXML private void handleViewBidHistory() {
        Item selected = tvProducts.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Chọn sản phẩm!", Alert.AlertType.WARNING); return; }
        new Thread(() -> {
            try {
                List<Bid> bids = bidRepo.findByItemId(selected.getId());
                Platform.runLater(() -> showBidHistoryDialog(selected.getName(), bids));
            } catch (SQLException e) {
                Platform.runLater(() -> showAlert("Lỗi: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }).start();
    }

    @FXML private void handleViewWinner() {
        Item selected = tvProducts.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Chọn sản phẩm!", Alert.AlertType.WARNING); return; }
        new Thread(() -> {
            try {
                String winnerName = selected.getCurrentBidderName();
                double finalPrice = selected.getCurrentPrice();
                Platform.runLater(() -> {
                    if (winnerName != null && !winnerName.isEmpty()) {
                        showAlert("🏆 " + winnerName + "\n💰 " + String.format("%,.0f VND", finalPrice), Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("Chưa có lượt đấu!", Alert.AlertType.INFORMATION);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }).start();
    }

    @FXML private void handleExport() {
        new Thread(() -> {
            try {
                List<Item> items = itemRepo.findAll();
                StringBuilder csv = new StringBuilder();
                csv.append("STT,ID,Tên SP,Giá khởi điểm,Giá hiện tại,Người bán,Trạng thái,Thời gian kết thúc\n");
                int stt = 1;
                for (Item item : items) {
                    csv.append(stt++).append(",");
                    csv.append(item.getId()).append(",");
                    csv.append(item.getName()).append(",");
                    csv.append(item.getStartingPrice()).append(",");
                    csv.append(item.getCurrentPrice()).append(",");
                    csv.append(item.getSellerName() != null ? item.getSellerName() : "N/A").append(",");
                    csv.append(item.getStatus() != null ? item.getStatus().name() : "N/A").append(",");
                    csv.append(item.getEndTime() != null ? item.getEndTime() : "N/A").append("\n");
                }
                Platform.runLater(() -> {
                    TextArea ta = new TextArea(csv.toString());
                    ta.setPrefHeight(400);
                    javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
                    dialog.setTitle("📤 Xuất dữ liệu");
                    dialog.getDialogPane().setContent(ta);
                    dialog.getDialogPane().setPrefSize(600, 450);
                    dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
                    dialog.showAndWait();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> showAlert("Xuất lỗi: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }).start();
    }

    private void showBidHistoryDialog(String itemName, List<Bid> bids) {
        ListView<String> lv = new ListView<>();
        ObservableList<String> items = FXCollections.observableArrayList();
        if (bids.isEmpty()) items.add("📭 Chưa có lượt đấu giá!");
        else {
            for (Bid b : bids) {
                String time = b.getBidTime() != null ?
                        b.getBidTime().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM")) : "N/A";
                items.add(String.format("💰 %s - %,.0f ₫ (%s)",
                        b.getBidderName() != null ? b.getBidderName() : "N/A",
                        b.getAmount(), time));
            }
        }
        lv.setItems(items);
        javafx.scene.control.Dialog<Void> d = new javafx.scene.control.Dialog<>();
        d.setTitle("📜 Lịch sử - " + itemName);
        d.getDialogPane().setContent(lv);
        d.getDialogPane().setPrefSize(450, 350);
        d.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        d.showAndWait();
    }

    private void showAlert(String msg, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}