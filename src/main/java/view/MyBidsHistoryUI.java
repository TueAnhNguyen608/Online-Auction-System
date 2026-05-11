
package view;

import client.AuctionClient;
import common.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class MyBidsHistoryUI {

    private AuctionClient client;
    private ObservableList<Bid> bidHistory = FXCollections.observableArrayList();
    private TableView<Bid> tableView;
    private Gson gson;

    public void show(AuctionClient client) {
        this.client = client;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
        Stage stage = new Stage();
        stage.setTitle("📜 Lịch sử đấu giá của tôi");

        tableView = new TableView<>();
        setupTable();

        Button btnRefresh = new Button("🔄 Tải lại");
        styleButton(btnRefresh, "#24416B");
        btnRefresh.setOnAction(e -> loadBidHistory());

        VBox root = new VBox(10, btnRefresh, tableView);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #E8ECEF;");

        Scene scene = new Scene(root, 900, 500);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.show();

        loadBidHistory();
    }

    private void setupTable() {
        TableColumn<Bid, String> colItemName = new TableColumn<>("Sản phẩm");
        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colItemName.setPrefWidth(250);

        TableColumn<Bid, Double> colAmount = new TableColumn<>("Giá đặt");
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colAmount.setCellFactory(col -> new TableCell<Bid, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty) setText(null);
                else setText(String.format("%,.0f VNĐ", amount));
            }
        });
        colAmount.setPrefWidth(150);

        TableColumn<Bid, LocalDateTime> colTime = new TableColumn<>("Thời gian");
        colTime.setCellValueFactory(new PropertyValueFactory<>("bidTime"));
        colTime.setCellFactory(col -> new TableCell<Bid, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime time, boolean empty) {
                super.updateItem(time, empty);
                if (empty || time == null) setText(null);
                else setText(time.format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")));
            }
        });
        colTime.setPrefWidth(200);

        TableColumn<Bid, String> colResult = new TableColumn<>("Kết quả");
        colResult.setCellValueFactory(cellData -> {
            Bid bid = cellData.getValue();
            boolean isWinner = bid.isWinning();
            return new javafx.beans.property.SimpleStringProperty(isWinner ? "🏆 THẮNG" : "❌ THUA");
        });
        colResult.setCellFactory(col -> new TableCell<Bid, String>() {
            @Override
            protected void updateItem(String result, boolean empty) {
                super.updateItem(result, empty);
                if (empty) setText(null);
                else {
                    setText(result);
                    if (result.contains("THẮNG")) setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    else setStyle("-fx-text-fill: #e74c3c;");
                }
            }
        });
        colResult.setPrefWidth(100);

        tableView.getColumns().addAll(colItemName, colAmount, colTime, colResult);
        tableView.setItems(bidHistory);
        tableView.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadBidHistory() {
        new Thread(() -> {
            try {
                ResponseMessage response = client.sendRequest(CommandType.GET_MY_BID_HISTORY, null);
                Platform.runLater(() -> {
                    if (response.getCode() == ResponseCode.SUCCESS) {
                        Object data = response.getData();
                        List<Bid> history = null;
                        if (data instanceof List) {
                            List<?> list = (List<?>) data;
                            if (!list.isEmpty()) {
                                Object first = list.get(0);
                                if (first instanceof Bid) {
                                    history = (List<Bid>) list;
                                } else if (first instanceof Map) {
                                    // Convert LinkedTreeMap list to Bid list
                                    String json = gson.toJson(data);
                                    Type listType = new TypeToken<List<Bid>>(){}.getType();
                                    history = gson.fromJson(json, listType);
                                }
                            } else {
                                history = List.of();
                            }
                        }
                        if (history != null) {
                            bidHistory.clear();
                            bidHistory.addAll(history);
                        } else {
                            DialogUtils.showError("Lỗi dữ liệu", "Dữ liệu trả về không đúng định dạng (cần List<Bid>)");
                        }
                    } else {
                        DialogUtils.showError("Lỗi", response.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> DialogUtils.showError("Lỗi kết nối", e.getMessage()));
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