package view;

import client.AuctionClient;
import common.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import utils.DialogUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class WonProductsUI {

    private AuctionClient client;
    private ObservableList<Item> wonItems = FXCollections.observableArrayList();
    private TableView<Item> tableView;

    public void show(AuctionClient client) {
        this.client = client;
        Stage stage = new Stage();
        stage.setTitle("🏆 Sản phẩm đã thắng đấu giá");

        tableView = new TableView<>();
        setupTable();

        Button btnRefresh = new Button("🔄 Tải lại");
        styleButton(btnRefresh, "#24416B");

        VBox root = new VBox(10, btnRefresh, tableView);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #E8ECEF;");

        Scene scene = new Scene(root, 900, 500);
        stage.setScene(scene);
        stage.show();

        loadWonProducts();
    }

    private void setupTable() {
        TableColumn<Item, String> colName = new TableColumn<>("Sản phẩm");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(250);

        TableColumn<Item, Double> colWinPrice = new TableColumn<>("Giá thắng");
        colWinPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colWinPrice.setCellFactory(col -> new TableCell<Item, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty) setText(null);
                else setText(String.format("%,.0f VNĐ", price));
            }
        });
        colWinPrice.setPrefWidth(150);

        TableColumn<Item, LocalDateTime> colEndTime = new TableColumn<>("Thời gian kết thúc");
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colEndTime.setCellFactory(col -> new TableCell<Item, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime time, boolean empty) {
                super.updateItem(time, empty);
                if (empty || time == null) setText(null);
                else setText(time.format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")));
            }
        });
        colEndTime.setPrefWidth(200);

        TableColumn<Item, String> colSeller = new TableColumn<>("Người bán");
        colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));

        tableView.getColumns().addAll(colName, colWinPrice, colEndTime, colSeller);
        tableView.setItems(wonItems);
        tableView.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 5);");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadWonProducts() {
        new Thread(() -> {
            try {
                ResponseMessage response = client.sendRequest(CommandType.GET_WON_PRODUCTS, null);
                Platform.runLater(() -> {
                    if (response.getCode() == ResponseCode.SUCCESS) {
                        @SuppressWarnings("unchecked")
                        List<Item> items = (List<Item>) response.getData();
                        wonItems.clear();
                        wonItems.addAll(items);
                    } else {
                        DialogUtils.showError("Lỗi", response.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> DialogUtils.showError("Lỗi", e.getMessage()));
            }
        }).start();
    }

    private void styleButton(Button btn, String colorHex) {
        btn.setPrefHeight(40);
        btn.setTextFill(Color.WHITE);
        btn.setFont(Font.font("System", FontWeight.BOLD, 13));
        btn.setBackground(new Background(new BackgroundFill(Color.web(colorHex), new CornerRadii(20), Insets.EMPTY)));
        btn.setCursor(Cursor.HAND);
    }
}