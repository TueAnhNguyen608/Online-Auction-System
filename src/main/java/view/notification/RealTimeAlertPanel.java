package view.notification;

import client.AuctionClient;
import common.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import view.common.SessionManager;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RealTimeAlertPanel implements Initializable {

    @FXML private VBox   rootBox;
    @FXML private HBox   headerBox;
    @FXML private Label  lblIcon;
    @FXML private Label  lblTitle;
    @FXML private Button btnClose;
    @FXML private Label  lblMessage;
    @FXML private Label  lblTime;

    private static final int AUTO_CLOSE_SECONDS = 5;
    private static final BlockingQueue<AlertData> alertQueue = new LinkedBlockingQueue<>();
    private static volatile boolean isShowing = false;

    public static class AlertData {
        public final String type, title, message;
        public AlertData(String type, String title, String message) {
            this.type = type; this.title = title; this.message = message;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    // ── Gọi từ bất kỳ đâu để hiện popup ─────────────────────────
    public static void show(String type, String title, String message) {
        Platform.runLater(() -> {
            alertQueue.offer(new AlertData(type, title, message));
            // Lưu vào danh sách thông báo realtime + tăng badge
            view.common.SessionManager.addRealtimeNotif(type, title, message);
            if (!isShowing) processQueue();
        });
    }

    private static void processQueue() {
        if (alertQueue.isEmpty()) { isShowing = false; return; }
        isShowing = true;
        AlertData data = alertQueue.poll();
        try {
            FXMLLoader loader = new FXMLLoader(
                    RealTimeAlertPanel.class.getResource("/fxml/notification/RealTimeAlertPanel.fxml"));
            VBox root = loader.load();
            RealTimeAlertPanel controller = loader.getController();
            controller.setup(data);

            Stage stage = new Stage(StageStyle.TRANSPARENT);
            stage.setAlwaysOnTop(true);
            Scene scene = new Scene(root);
            scene.setFill(null);
            stage.setScene(scene);

            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            stage.setX(screen.getMaxX() - 360);
            stage.setY(screen.getMaxY() + 20); // bắt đầu từ dưới màn hình
            stage.setOpacity(0);
            stage.show();

            controller.animateIn(stage, () -> controller.startAutoClose(stage));

        } catch (Exception e) {
            System.out.println("RealTimeAlertPanel lỗi: " + e.getMessage());
            isShowing = false;
        }
    }

    // ── Setup nội dung ───────────────────────────────────────────
    private void setup(AlertData data) {
        lblTitle.setText(data.title);
        lblMessage.setText(data.message);
        lblTime.setText("Vừa xong");

        String headerBg, icon;
        switch (data.type) {
            case "OUTBID" -> { headerBg = "#1a3a6e"; icon = "🔔"; }
            case "WIN"    -> { headerBg = "#14432a"; icon = "🏆"; }
            case "ENDING" -> { headerBg = "#2d1a5e"; icon = "⏰"; }
            case "PAY"    -> { headerBg = "#3a2a00"; icon = "💳"; }
            case "WARNING"-> { headerBg = "#3a1010"; icon = "⚠️"; }
            default       -> { headerBg = "#1a2336"; icon = "📢"; }
        }
        lblIcon.setText(icon);
        headerBox.setStyle(
                "-fx-background-color: " + headerBg + ";" +
                        "-fx-background-radius: 12 12 0 0;" +
                        "-fx-padding: 11 14 11 14;" +
                        "-fx-border-color: transparent transparent #1e3a6e transparent;" +
                        "-fx-border-width: 0 0 1 0;"
        );
    }

    // ── Animation trượt lên + fade in ───────────────────────────
    private void animateIn(Stage stage, Runnable onFinished) {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double targetY = screen.getMaxY() - 130;

        // Dùng Timeline với setX/setY trực tiếp thay vì KeyValue
        final double startY = screen.getMaxY() + 20;
        final double[] current = {startY};
        stage.setY(startY);
        stage.setOpacity(0);

        Timeline anim = new Timeline();
        int frames = 20;
        for (int i = 0; i <= frames; i++) {
            final double progress = (double) i / frames;
            final double easedProgress = 1 - Math.pow(1 - progress, 3); // ease-out cubic
            final double y = startY + (targetY - startY) * easedProgress;
            final double opacity = progress;
            final int frameIdx = i;
            anim.getKeyFrames().add(new KeyFrame(
                    Duration.millis(frameIdx * (350.0 / frames)),
                    e -> {
                        stage.setY(y);
                        stage.setOpacity(opacity);
                    }
            ));
        }
        anim.setOnFinished(e -> {
            stage.setY(targetY);
            stage.setOpacity(1);
            onFinished.run();
        });
        anim.play();
    }

    // ── Auto close countdown ─────────────────────────────────────
    private void startAutoClose(Stage stage) {
        final int[] remaining = {AUTO_CLOSE_SECONDS};
        Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remaining[0]--;
            if (remaining[0] <= 0) animateOut(stage);
        }));
        countdown.setCycleCount(AUTO_CLOSE_SECONDS);
        countdown.play();

        rootBox.setOnMouseEntered(e -> countdown.pause());
        rootBox.setOnMouseExited(e  -> countdown.play());
        btnClose.setOnAction(e -> { countdown.stop(); animateOut(stage); });
    }

    // ── Animation trượt xuống + fade out ────────────────────────
    private void animateOut(Stage stage) {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double startY = stage.getY();
        double endY = screen.getMaxY() + 20;

        Timeline anim = new Timeline();
        int frames = 15;
        for (int i = 0; i <= frames; i++) {
            final double progress = (double) i / frames;
            final double easedProgress = progress * progress; // ease-in
            final double y = startY + (endY - startY) * easedProgress;
            final double opacity = 1 - progress;
            final int frameIdx = i;
            anim.getKeyFrames().add(new KeyFrame(
                    Duration.millis(frameIdx * (250.0 / frames)),
                    e -> {
                        stage.setY(y);
                        stage.setOpacity(opacity);
                    }
            ));
        }
        anim.setOnFinished(e -> {
            stage.close();
            Platform.runLater(RealTimeAlertPanel::processQueue);
        });
        anim.play();
    }

    // ── Kết nối realtime từ server ───────────────────────────────
    public static void startListening() {
        AuctionClient client = SessionManager.getClient();
        if (client == null) return;

        client.startRealtimeListener(msg -> {
            if (msg == null) return;
            if ("realtime_update".equals(msg.getMessage())) {
                try {
                    com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                            .registerTypeAdapter(java.time.LocalDateTime.class,
                                    new common.LocalDateTimeAdapter())
                            .create();
                    PriceUpdateEvent event = gson.fromJson(
                            gson.toJson(msg.getData()), PriceUpdateEvent.class);
                    if (event == null) return;

                    // ĐÃ SỬA: dùng getCurrentPrice() thay vì getNewPrice()
                    String priceStr = String.format("%,.0f đ", event.getCurrentPrice())
                            .replace(",", ".");

                    switch (event.getType()) {
                        case "PRICE_UPDATE" -> show("OUTBID",
                                "Có người trả giá cao hơn!",
                                (event.getItemName() != null ? "\"" + event.getItemName() + "\" " : "") +
                                        "Giá mới: " + priceStr);
                        case "OUTBID" -> show("OUTBID",
                                "Bạn bị vượt giá!",
                                "Ai đó vừa trả " + priceStr + ". Hãy đặt lại ngay!");
                        case "AUCTION_END" -> show("WIN",
                                "Phiên đấu giá kết thúc!",
                                (event.getItemName() != null ? "\"" + event.getItemName() + "\" " : "") +
                                        "Người thắng: " + (event.getCurrentBidderName() != null
                                        ? event.getCurrentBidderName() : "N/A"));
                        case "EXTENSION" -> show("ENDING",
                                "Phiên được gia hạn!",
                                "Thời gian đấu giá vừa được kéo dài thêm " +
                                        event.getRemainingSeconds() + " giây.");
                    }
                } catch (Exception e) {
                    System.out.println("Parse realtime lỗi: " + e.getMessage());
                }
            }
        });
    }
}