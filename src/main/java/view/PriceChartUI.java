package view;

import client.AuctionClient;
import common.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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
        root.setStyle("-fx-background-color: #E8ECEF;");

        Label loadingLabel = new Label("⏳ Đang tải dữ liệu...");
        loadingLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        loadingLabel.setTextFill(Color.web("#24416B"));
        root.getChildren().add(loadingLabel);

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.show();

        new Thread(() -> {
            try {
                if (client == null) {
                    Platform.runLater(() -> drawMockChart(root, itemName));
                    return;
                }
                ResponseMessage response = client.sendRequest(CommandType.GET_PRICE_HISTORY, itemId);
                Platform.runLater(() -> {
                    if (response.getCode() == ResponseCode.SUCCESS && response.getData() instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<PricePoint> history = (List<PricePoint>) response.getData();
                        if (history != null && !history.isEmpty())
                            drawChart(root, history, itemName);
                        else
                            drawMockChart(root, itemName);
                    } else {
                        drawMockChart(root, itemName);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> drawMockChart(root, itemName));
            }
        }).start();
    }

    private void drawChart(VBox root, List<PricePoint> history, String itemName) {
        if (history.isEmpty()) {
            drawMockChart(root, itemName);
            return;
        }

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Thời gian (phút từ lúc bắt đầu)");
        yAxis.setLabel("Giá (VNĐ)");

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Biểu đồ giá: " + itemName);
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
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(itemName);

        // Dữ liệu mẫu
        series.getData().add(new XYChart.Data<>(0, 10_000_000));
        series.getData().add(new XYChart.Data<>(5, 12_000_000));
        series.getData().add(new XYChart.Data<>(10, 15_000_000));
        series.getData().add(new XYChart.Data<>(15, 18_000_000));
        series.getData().add(new XYChart.Data<>(20, 22_000_000));

        lineChart.getData().add(series);
        root.getChildren().clear();
        root.getChildren().add(lineChart);
    }
}