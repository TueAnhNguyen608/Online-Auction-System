package com.nguyenthuhuyen.auctionapp;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class ProductController {

    // ===== TABLE =====
    @FXML
    private TableView<Product> productTable;

    @FXML
    private TableColumn<Product, String> colName;
    @FXML
    private TableColumn<Product, String> colPrice;
    @FXML
    private TableColumn<Product, String> colTime;
    @FXML
    private TableColumn<Product, String> colType;

    // ===== INPUT =====
    @FXML
    private TextField txtProductName;
    @FXML
    private TextField txtProductPrice;
    @FXML
    private TextField txtProductDuration;

    @FXML
    private ComboBox<String> cbType;

    // ===== DATA =====
    private ObservableList<Product> productList = FXCollections.observableArrayList();

    // ===== INIT =====
    @FXML
    public void initialize() {

        // Gắn dữ liệu cho từng cột
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));

        // Đổ dữ liệu vào bảng
        productTable.setItems(productList);

        // ComboBox
        cbType.setItems(FXCollections.observableArrayList(
                "Điện tử",
                "Đồ cổ",
                "Nghệ thuật",
                "Khác"
        ));
    }

    // ===== THÊM =====
    @FXML
    void onAddProduct(ActionEvent event) {
        try {
            String name = txtProductName.getText();
            String price = txtProductPrice.getText();
            String time = txtProductDuration.getText();
            String type = cbType.getValue();

            if (name == null || name.isEmpty() || type == null) {
                showAlert("Vui lòng nhập đầy đủ thông tin!");
                return;
            }

            productList.add(new Product(name, price, time, type));

            clearFields();

        } catch (Exception e) {
            showAlert("Lỗi nhập dữ liệu!");
        }
    }

    // ===== XÓA =====
    @FXML
    void onDeleteProduct(ActionEvent event) {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            productList.remove(selected);
        } else {
            showAlert("Chọn sản phẩm để xóa!");
        }
    }

    // ===== SỬA =====
    @FXML
    void onEditProduct(ActionEvent event) {
        Product selected = productTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Chọn sản phẩm để sửa!");
            return;
        }

        int index = productTable.getSelectionModel().getSelectedIndex();

        Product updated = new Product(
                txtProductName.getText(),
                txtProductPrice.getText(),
                txtProductDuration.getText(),
                cbType.getValue()
        );

        productList.set(index, updated);
    }

    // ===== CLEAR =====
    private void clearFields() {
        txtProductName.clear();
        txtProductPrice.clear();
        txtProductDuration.clear();
        cbType.setValue(null);
    }

    // ===== ALERT =====
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(msg);
        alert.show();
    }
}
