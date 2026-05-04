package view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import common.Item;
import common.ItemManager;

import java.time.LocalDateTime;
import java.util.List;

public class MainApp {
    public void showWindow(String username, String role) {
        Stage stage = new Stage();
        stage.setTitle("Trang chủ Đấu giá - Người dùng: " + username + " [" + role + "]");

        ItemManager itemManager = new ItemManager();
        List<Item> itemList = itemManager.loadItems();

        // =======================================================
        // TỰ ĐỘNG CẬP NHẬT TRẠNG THÁI THEO THỜI GIAN
        // =======================================================
        boolean needSave = false;
        LocalDateTime now = LocalDateTime.now();
        for (Item item : itemList) {
            try {
                LocalDateTime start = item.getStartTime();
                LocalDateTime end = item.getEndTime();

                if (now.isAfter(end) && !"ENDED".equals(item.getStatus())) {
                    item.setStatus("ENDED");
                    needSave = true;
                }
                else if (now.isAfter(start) && now.isBefore(end) && "PENDING".equals(item.getStatus())) {
                    item.setStatus("ACTIVE");
                    needSave = true;
                }
            } catch (Exception ignored) {
            }
        }
        if (needSave) itemManager.saveItems(itemList);

        // =======================================================
        // BẢNG HIỂN THỊ
        // =======================================================
        ObservableList<Item> data = FXCollections.observableArrayList(itemList);
        TableView<Item> table = new TableView<>();

        TableColumn<Item, String> colName = new TableColumn<>("Tên sản phẩm");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(200);

        TableColumn<Item, Double> colPrice = new TableColumn<>("Giá hiện tại");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));

        TableColumn<Item, String> colBidder = new TableColumn<>("Người trả cao nhất");
        colBidder.setCellValueFactory(new PropertyValueFactory<>("currentBidderName"));
        colBidder.setPrefWidth(130);

        TableColumn<Item, String> colSeller = new TableColumn<>("Người bán");
        colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));

        TableColumn<Item, String> colStatus = new TableColumn<>("Trạng thái");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(colName, colPrice, colBidder, colSeller, colStatus);

        // =======================================================
        // THANH TÌM KIẾM VÀ LỌC
        // =======================================================
        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Tìm tên sản phẩm...");

        ComboBox<String> cbFilter = new ComboBox<>();
        cbFilter.getItems().addAll("Tất cả", "ACTIVE", "PENDING", "ENDED");
        cbFilter.setValue("Tất cả");

        FilteredList<Item> filteredData = new FilteredList<>(data, p -> true);
        table.setItems(filteredData);

        Runnable updateFilter = () -> {
            filteredData.setPredicate(item -> {
                boolean matchName = item.getName().toLowerCase().contains(txtSearch.getText().toLowerCase());
                boolean matchStatus = cbFilter.getValue().equals("Tất cả") || item.getStatus().equals(cbFilter.getValue());
                return matchName && matchStatus;
            });
        };
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> updateFilter.run());
        cbFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter.run());

        HBox filterBox = new HBox(15, new Label("Tìm kiếm:"), txtSearch, new Label("Lọc trạng thái:"), cbFilter);
        filterBox.setPadding(new Insets(10, 0, 10, 0));

        // =======================================================
        // KHU VỰC ĐẤU GIÁ
        // =======================================================
        Label lblSelected = new Label("Chưa chọn sản phẩm nào.");
        lblSelected.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");

        TextField txtBidAmount = new TextField();
        txtBidAmount.setPromptText("Nhập giá...");
        Button btnBid = new Button("Đặt Giá");
        btnBid.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        HBox bidBox = new HBox(15, txtBidAmount, btnBid);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                lblSelected.setText("Đang chọn: " + newSelection.getName() + " (Giá: " + newSelection.getCurrentPrice() + ")");
            }
        });

        btnBid.setOnAction(e -> {
            Item selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn sản phẩm!"); return;
            }
            if (selected.getSellerName() != null && selected.getSellerName().equals(username)) {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Không thể tự mua đồ của mình!"); return;
            }
            if (!"ACTIVE".equals(selected.getStatus())) {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Sản phẩm không mở đấu giá!"); return;
            }
            try {
                double newPrice = Double.parseDouble(txtBidAmount.getText());
                if (newPrice <= selected.getCurrentPrice()) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá đặt phải lớn hơn giá hiện tại!"); return;
                }
                selected.setCurrentPrice(newPrice);
                selected.setCurrentBidderName(username);
                itemManager.saveItems(itemList);
                table.refresh();
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đặt giá thành công!");
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Nhập số không hợp lệ!");
            }
        });

        // =======================================================
        // ĐĂNG BÁN SẢN PHẨM MỚI (Chỉ Seller/Admin)
        // =======================================================
        VBox addBox = new VBox(10);
        addBox.setPadding(new Insets(10));
        addBox.setStyle("-fx-border-color: #34495e; -fx-border-width: 2px; -fx-border-radius: 5px;");

        Label lblAdd = new Label("--- KHU VỰC DÀNH RIÊNG CHO NGƯỜI BÁN ---");
        lblAdd.setStyle("-fx-font-weight: bold; -fx-text-fill: #c0392b;");

        TextField txtNewName = new TextField(); txtNewName.setPromptText("Tên sản phẩm");
        TextField txtNewPrice = new TextField(); txtNewPrice.setPromptText("Giá khởi điểm");
        Button btnAdd = new Button("Đăng bán sản phẩm");

        btnAdd.setOnAction(e -> {
            try {
                String name = txtNewName.getText();
                double price = Double.parseDouble(txtNewPrice.getText());
                if (name.isEmpty()) throw new Exception("Tên không được để trống");

                LocalDateTime start = LocalDateTime.now().withNano(0);
                LocalDateTime end = start.plusDays(7);

                // ✅ ĐÃ SỬA - Dùng constructor mới có 5 tham số
                Item newItem = new Item(name, "Sản phẩm mới", price, start, end);
                newItem.setSellerName(username);
                newItem.setStatus("ACTIVE");

                itemList.add(newItem);
                data.add(newItem);
                itemManager.saveItems(itemList);

                txtNewName.clear(); txtNewPrice.clear();
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã đăng bán sản phẩm mới!");
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá khởi điểm phải là số!");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", ex.getMessage());
            }
        });

        addBox.getChildren().addAll(lblAdd, new HBox(10, txtNewName, txtNewPrice, btnAdd));

        if (!"Seller".equals(role) && !"Admin".equals(role)) {
            addBox.setVisible(false);
            addBox.setManaged(false);
        }

        // =======================================================
        // BỐ CỤC CHUNG
        // =======================================================
        Button btnLogout = new Button("Đăng xuất");
        btnLogout.setOnAction(e -> stage.close());

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(new Label("GIAO DIỆN ĐẤU GIÁ"), filterBox, table, lblSelected, bidBox, addBox, btnLogout);

        Scene scene = new Scene(root, 900, 700);
        stage.setScene(scene);
        stage.show();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); alert.showAndWait();
    }
}