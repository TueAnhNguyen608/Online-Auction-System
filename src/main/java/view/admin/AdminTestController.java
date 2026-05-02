package view.admin;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import common.*;
import server.DatabaseManager;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

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
    private DatabaseManager dbManager;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter END_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        setupTableColumns();
        setupPriceFormat();
        setupStatusFormat();
        setupEndTimeFormat();

        filteredList = new FilteredList<>(masterList, p -> true);
        tvProducts.setItems(filteredList);

        setupComboBoxes();
        setupFilterListeners();

        try {
            dbManager = DatabaseManager.getInstance();
            loadItems();
            lblStatus.setText("✅ Connected to database");
        } catch (SQLException e) {
            lblStatus.setText("❌ Database connection failed: " + e.getMessage());
            e.printStackTrace();
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
        colSTT.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(filteredList.indexOf(cellData.getValue()) + 1).asObject()
        );
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        colBidder.setCellValueFactory(cellData -> {
            String bidder = cellData.getValue().getCurrentBidderName();
            return new javafx.beans.property.SimpleStringProperty(
                    (bidder == null || bidder.isEmpty()) ? "Chưa có" : bidder
            );
        });
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colBidCount.setCellValueFactory(new PropertyValueFactory<>("bidCount"));
    }

    private void setupEndTimeFormat() {
        colEndTime.setCellFactory(col -> new TableCell<Item, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                    return;
                }

                Item product = getTableView().getItems().get(getIndex());
                if (product != null && product.getEndTime() != null) {
                    LocalDateTime endTime = product.getEndTime();
                    String endTimeStr = endTime.format(END_TIME_FORMAT);

                    if ("ACTIVE".equals(product.getStatus())) {
                        long hoursLeft = LocalDateTime.now().until(endTime, ChronoUnit.HOURS);
                        if (hoursLeft < 24) {
                            setText(endTimeStr + " ⏰");
                            setStyle("-fx-text-fill: #e65100; -fx-font-weight: bold;");
                        } else {
                            setText(endTimeStr);
                            setStyle("-fx-text-fill: #2e7d32;");
                        }
                    } else {
                        setText(endTimeStr);
                        setStyle("-fx-text-fill: #757575;");
                    }
                } else {
                    setText("N/A");
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
            if (status != null && !status.equals("Tất cả") && !item.getStatus().equals(status)) return false;
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
        tvProducts.refresh();
        updateStatusMessage();
    }

    @FXML private void handleFilter() { applyFilter(); }
    @FXML private void handleClearFilter() {
        txtSearch.clear();
        cbFilterStatus.setValue("Tất cả");
        cbFilterPrice.setValue("Tất cả");
        applyFilter();
    }

    private void loadItems() {
        new Thread(() -> {
            try {
                List<Item> items = dbManager.getAllItems();
                Platform.runLater(() -> {
                    masterList.setAll(items);
                    updateStatusMessage();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> lblStatus.setText("❌ Lỗi tải: " + e.getMessage()));
                e.printStackTrace();
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
        lblStatus.setText("🕐 " + time + " | 📦 " + filteredList.size() + "/" + masterList.size() + " sản phẩm");
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
            item.setName(name);
            item.setDescription(cbCategory.getValue() + " - Sản phẩm chất lượng cao");
            item.setStartingPrice(price);
            item.setCurrentPrice(price);
            item.setStartTime(LocalDateTime.now());
            item.setEndTime(LocalDateTime.now().plusDays(cbDays.getValue()));
            item.setStatus("ACTIVE");
            item.setSellerName("admin");
            item.setSellerId("admin");

            new Thread(() -> {
                try {
                    dbManager.createItem(item);
                    Platform.runLater(() -> {
                        loadItems();
                        clearForm();
                        showAlert("✅ Thêm thành công!", Alert.AlertType.INFORMATION);
                    });
                } catch (SQLException | DataValidationException e) {
                    Platform.runLater(() -> showAlert("❌ Lỗi: " + e.getMessage(), Alert.AlertType.ERROR));
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
                    if (dbManager.updateItem(selected)) {
                        Platform.runLater(() -> {
                            loadItems();
                            clearForm();
                            showAlert("✏️ Cập nhật thành công!", Alert.AlertType.INFORMATION);
                        });
                    }
                } catch (SQLException e) {
                    Platform.runLater(() -> showAlert("❌ Lỗi: " + e.getMessage(), Alert.AlertType.ERROR));
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
                    if (dbManager.deleteItem(selected.getId())) {
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
        if (!"ACTIVE".equals(selected.getStatus())) { showAlert("Chỉ kết thúc phiên đang hoạt động!", Alert.AlertType.WARNING); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setContentText("Kết thúc: " + selected.getName() + "?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            new Thread(() -> {
                try {
                    dbManager.forceEndAuction(selected.getId());
                    selected.setStatus("ENDED");
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
                List<Bid> bids = dbManager.getBidHistory(selected.getId());
                Platform.runLater(() -> showBidHistoryDialog(selected.getName(), bids));
            } catch (SQLException e) { Platform.runLater(() -> showAlert("Lỗi: " + e.getMessage(), Alert.AlertType.ERROR)); }
        }).start();
    }

    @FXML private void handleViewWinner() {
        Item selected = tvProducts.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Chọn sản phẩm!", Alert.AlertType.WARNING); return; }
        new Thread(() -> {
            try {
                User winner = dbManager.getWinner(selected.getId());
                Platform.runLater(() -> {
                    if (winner != null) showAlert("🏆 " + winner.getUsername() + "\n💰 " + String.format("%,.0f VND", selected.getCurrentPrice()), Alert.AlertType.INFORMATION);
                    else showAlert("Chưa có lượt đấu!", Alert.AlertType.INFORMATION);
                });
            } catch (SQLException e) { Platform.runLater(() -> showAlert("Lỗi: " + e.getMessage(), Alert.AlertType.ERROR)); }
        }).start();
    }

    @FXML private void handleExport() {
        new Thread(() -> {
            try {
                List<Item> items = dbManager.getAllItems();
                StringBuilder csv = new StringBuilder();
                csv.append("STT,ID,Tên SP,Giá khởi điểm,Giá hiện tại,Người bán,Trạng thái,Thời gian kết thúc\n");
                int stt = 1;
                for (Item item : items) {
                    csv.append(stt++).append(",");
                    csv.append(item.getId()).append(",");
                    csv.append(item.getName()).append(",");
                    csv.append(item.getStartingPrice()).append(",");
                    csv.append(item.getCurrentPrice()).append(",");
                    csv.append(item.getSellerName()).append(",");
                    csv.append(item.getStatus()).append(",");
                    csv.append(item.getEndTime() != null ? item.getEndTime() : "N/A").append("\n");
                }
                Platform.runLater(() -> {
                    TextArea ta = new TextArea(csv.toString());
                    ta.setPrefHeight(400);
                    Dialog<Void> dialog = new Dialog<>();
                    dialog.setTitle("📤 Xuất dữ liệu");
                    dialog.getDialogPane().setContent(ta);
                    dialog.getDialogPane().setPrefSize(600, 450);
                    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                    dialog.showAndWait();
                });
            } catch (SQLException e) { Platform.runLater(() -> showAlert("Xuất lỗi: " + e.getMessage(), Alert.AlertType.ERROR)); }
        }).start();
    }

    private void showBidHistoryDialog(String itemName, List<Bid> bids) {
        ListView<String> lv = new ListView<>();
        ObservableList<String> items = FXCollections.observableArrayList();
        if (bids.isEmpty()) items.add("📭 Chưa có lượt đấu giá!");
        else { for (Bid b : bids) items.add(String.format("💰 %s - %,.0f ₫ (%s)", b.getBidderName(), b.getAmount(), b.getBidTime().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM")))); }
        lv.setItems(items);
        Dialog<Void> d = new Dialog<>();
        d.setTitle("📜 Lịch sử - " + itemName);
        d.getDialogPane().setContent(lv);
        d.getDialogPane().setPrefSize(450, 350);
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        d.showAndWait();
    }

    private void showAlert(String msg, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}