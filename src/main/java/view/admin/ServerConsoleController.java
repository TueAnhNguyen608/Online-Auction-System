package view.admin;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import server.AuctionServer;
import server.DatabaseManager;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerConsoleController {

    @FXML private Button btnStart;
    @FXML private Button btnStop;
    @FXML private Button btnRestart;
    @FXML private Button btnKick;
    @FXML private ListView<String> lvClients;
    @FXML private TextArea taLogs;
    @FXML private Label lblClientCount;
    @FXML private Label lblThreadCount;
    @FXML private Label lblSubscriptions;
    @FXML private Label lblTotalBids;
    @FXML private Label lblActiveItems;
    @FXML private Label lblPort;
    @FXML private Label lblServerStatus;
    @FXML private Label lblStatus;

    private ObservableList<String> clientList = FXCollections.observableArrayList();
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private DatabaseManager dbManager;

    private static final DateTimeFormatter TIME_FORMAT     = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ── Trạng thái server ──────────────────────────────────────────────────────
    private enum ServerState { OFFLINE, STARTING, ONLINE, STOPPING }
    private ServerState serverState = ServerState.OFFLINE;

    @FXML
    public void initialize() {
        lvClients.setItems(clientList);
        lblPort.setText("9999");

        // Trạng thái ban đầu: chỉ cho phép nhấn START
        applyState(ServerState.OFFLINE);

        addLog("🟢 Server Console initialized");

        // Kết nối DB (chỉ để hiển thị thống kê, không liên quan start/stop)
        try {
            dbManager = DatabaseManager.getInstance();
            addLog("✅ Connected to database");
            loadStatistics();
        } catch (SQLException e) {
            addLog("❌ Database connection failed: " + e.getMessage());
        }

        // Cập nhật stats định kỳ
        scheduler.scheduleAtFixedRate(() ->
                Platform.runLater(this::refreshStats), 0, 2, TimeUnit.SECONDS);

        startRealtimeClock();
    }

    // ── Xử lý nút START ───────────────────────────────────────────────────────
    @FXML
    private void handleStartServer() {
        if (serverState != ServerState.OFFLINE) return;

        applyState(ServerState.STARTING);
        addLog("🚀 Starting server on port 9999...");

        // Gọi AuctionServer.startServer() trong background thread,
        // truyền callback để nhận log ngược về UI
        AuctionServer.startServer(logMsg -> addLog(logMsg));

        // Chờ server bind xong rồi chuyển trạng thái sang ONLINE
        new Thread(() -> {
            try {
                // Poll cho đến khi AuctionServer báo isRunning = true (tối đa 5s)
                long deadline = System.currentTimeMillis() + 5000;
                while (!AuctionServer.isRunning() && System.currentTimeMillis() < deadline) {
                    Thread.sleep(200);
                }
            } catch (InterruptedException ignored) {}

            Platform.runLater(() -> {
                if (AuctionServer.isRunning()) {
                    applyState(ServerState.ONLINE);
                    addLog("✅ Server is ONLINE");
                } else {
                    applyState(ServerState.OFFLINE);
                    addLog("❌ Server failed to start");
                }
            });
        }).start();
    }

    // ── Xử lý nút STOP ────────────────────────────────────────────────────────
    @FXML
    private void handleStopServer() {
        if (serverState != ServerState.ONLINE) return;

        applyState(ServerState.STOPPING);
        addLog("🛑 Stopping server...");

        new Thread(() -> {
            AuctionServer.stopServer();   // blocking call – chờ cleanup xong

            Platform.runLater(() -> {
                clientList.clear();
                updateClientCount();
                applyState(ServerState.OFFLINE);
                addLog("✅ Server stopped");
            });
        }).start();
    }

    // ── Xử lý nút RESTART ─────────────────────────────────────────────────────
    @FXML
    private void handleRestart() {
        if (serverState != ServerState.ONLINE) return;

        addLog("🔄 Restarting server...");
        // Stop → sau đó Start lại
        applyState(ServerState.STOPPING);

        new Thread(() -> {
            AuctionServer.stopServer();

            Platform.runLater(() -> {
                clientList.clear();
                updateClientCount();
                addLog("✅ Server stopped. Restarting...");
                applyState(ServerState.STARTING);

                AuctionServer.startServer(logMsg -> addLog(logMsg));

                new Thread(() -> {
                    try {
                        long deadline = System.currentTimeMillis() + 5000;
                        while (!AuctionServer.isRunning() && System.currentTimeMillis() < deadline) {
                            Thread.sleep(200);
                        }
                    } catch (InterruptedException ignored) {}

                    Platform.runLater(() -> {
                        if (AuctionServer.isRunning()) {
                            applyState(ServerState.ONLINE);
                            addLog("✅ Server restarted successfully");
                        } else {
                            applyState(ServerState.OFFLINE);
                            addLog("❌ Server failed to restart");
                        }
                    });
                }).start();
            });
        }).start();
    }

    // ── Kick client ───────────────────────────────────────────────────────────
    @FXML
    private void handleKickClient() {
        String selected = lvClients.getSelectionModel().getSelectedItem();
        if (selected == null) {
            addLog("⚠️ No client selected!");
            return;
        }
        addLog("👢 Kicking client: " + selected);
        clientList.remove(selected);
        updateClientCount();
        addLog("✅ Client " + selected + " disconnected");
    }

    // ── Logs ──────────────────────────────────────────────────────────────────
    @FXML
    private void handleClearLogs() {
        taLogs.clear();
        addLog("📝 Logs cleared");
    }

    @FXML
    private void handleExportLogs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Logs");
        fileChooser.setInitialFileName("server_logs_" + System.currentTimeMillis() + ".txt");
        File file = fileChooser.showSaveDialog(taLogs.getScene().getWindow());

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.print(taLogs.getText());
                addLog("📁 Logs exported to: " + file.getName());
            } catch (Exception e) {
                addLog("❌ Failed to export: " + e.getMessage());
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Áp dụng trạng thái server lên toàn bộ UI (buttons, badge, style).
     * Phải gọi trên JavaFX Application Thread.
     */
    private void applyState(ServerState state) {
        serverState = state;

        switch (state) {
            case OFFLINE -> {
                btnStart.setDisable(false);
                btnStop.setDisable(true);
                btnRestart.setDisable(true);
                btnKick.setDisable(true);
                setStatusBadge("🔴 OFFLINE", "status-badge-offline");
            }
            case STARTING -> {
                btnStart.setDisable(true);
                btnStop.setDisable(true);
                btnRestart.setDisable(true);
                btnKick.setDisable(true);
                setStatusBadge("🟡 STARTING...", "status-badge-starting");
            }
            case ONLINE -> {
                btnStart.setDisable(true);
                btnStop.setDisable(false);
                btnRestart.setDisable(false);
                btnKick.setDisable(false);
                setStatusBadge("🟢 ONLINE", "status-badge-online");
            }
            case STOPPING -> {
                btnStart.setDisable(true);
                btnStop.setDisable(true);
                btnRestart.setDisable(true);
                btnKick.setDisable(true);
                setStatusBadge("🟠 STOPPING...", "status-badge-stopping");
            }
        }
    }

    private void setStatusBadge(String text, String styleClass) {
        lblServerStatus.setText(text);
        lblServerStatus.getStyleClass().setAll(styleClass);
    }

    private void refreshStats() {
        lblThreadCount.setText(String.valueOf(Thread.activeCount()));
        loadStatistics();
    }

    private void loadStatistics() {
        if (dbManager == null) return;
        new Thread(() -> {
            try {
                int totalBids   = dbManager.getTotalBids();
                int activeItems = dbManager.getActiveItemsCount();
                Platform.runLater(() -> {
                    lblTotalBids.setText(String.valueOf(totalBids));
                    lblActiveItems.setText(String.valueOf(activeItems));
                });
            } catch (SQLException e) {
                // Silent fail – không dừng UI
            }
        }).start();
    }

    private void updateClientCount() {
        lblClientCount.setText(String.valueOf(clientList.size()));
    }

    private void startRealtimeClock() {
        Thread clockThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                    String time = LocalDateTime.now().format(TIME_FORMAT);
                    Platform.runLater(() ->
                            lblStatus.setText("🕐 " + time + " | 📊 Server Console"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        clockThread.setDaemon(true);
        clockThread.start();
    }

    private void addLog(String message) {
        Platform.runLater(() -> {
            String timestamp = LocalDateTime.now().format(LOG_TIME_FORMAT);
            taLogs.appendText("[" + timestamp + "] " + message + "\n");
            taLogs.setScrollTop(Double.MAX_VALUE);
        });
    }
}
