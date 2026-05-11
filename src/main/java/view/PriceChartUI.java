package view;

import client.AuctionClient;
import common.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.time.ZoneOffset;
import java.util.List;

public class PriceChartUI {

    private AuctionClient client;

    public void show(AuctionClient client, String itemId, String itemName) {
        this.client = client;
        Stage stage = new Stage();
        stage.setTitle("📈 Biểu đồ giá - " + itemName);

        VBox root = new VBox(15);
        root.setPadding(new Insets(15));
        root.getStyleClass().add("info-card"); // Gắn class nền

        Label loadingLabel = new Label("⏳ Đang tải dữ liệu...");
        loadingLabel.getStyleClass().add("title");
        root.getChildren().add(loadingLabel);

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        // Link file CSS để nhận style biểu đồ mới
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch(Exception e) {}

        stage.setScene(scene);
        stage.show();

        new Thread(() -> {
            try {
                // Gọi API thật từ Server
                ResponseMessage response = client.sendRequest(CommandType.GET_PRICE_HISTORY, itemId);
                Platform.runLater(() -> {
                    if (response.getCode() == ResponseCode.SUCCESS) {
                        @SuppressWarnings("unchecked")
                        List<PricePoint> history = (List<PricePoint>) response.getData();
                        if(history == null || history.isEmpty()) {
                            drawMockChart(root, itemName); // Nếu chưa có data thật thì vẽ mẫu
                        } else {
                            drawRealChart(root, itemName, history);
                        }
                    } else {
                        drawMockChart(root, itemName);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> drawMockChart(root, itemName));
            }
        }).start();
    }

    private void drawRealChart(VBox root, String itemName, List<PricePoint> history) {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Thời gian (phút)");
        yAxis.setLabel("Giá (VNĐ)");

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Lịch sử giá: " + itemName);
        lineChart.getStyleClass().add("price-chart"); // CSS Class

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(itemName);

        long startTime = history.get(0).getTime().toEpochSecond(ZoneOffset.ofHours(7));
        for (PricePoint p : history) {
            long diffMinutes = (p.getTime().toEpochSecond(ZoneOffset.ofHours(7)) - startTime) / 60;
            series.getData().add(new XYChart.Data<>(diffMinutes, p.getPrice()));
        }

        lineChart.getData().add(series);
        root.getChildren().clear();
        root.getChildren().add(lineChart);
    }

    private void drawMockChart(VBox root, String itemName) {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Thời gian (phút)");
        yAxis.setLabel("Giá (VNĐ)");

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Biểu đồ giá mẫu: " + itemName);
        lineChart.getStyleClass().add("price-chart"); // Gắn CSS Class quan trọng

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(itemName);
        series.getData().add(new XYChart.Data<>(0, 10_000_000));
        series.getData().add(new XYChart.Data<>(5, 12_000_000));
        series.getData().add(new XYChart.Data<>(10, 15_000_000));
        series.getData().add(new XYChart.Data<>(20, 18_000_000));

        lineChart.getData().add(series);

        // ĐÃ XÓA DÒNG setStyle("-fx-stroke...") ĐỂ NHẬN MÀU TỪ CSS

        root.getChildren().clear();
        root.getChildren().add(lineChart);
    }
}