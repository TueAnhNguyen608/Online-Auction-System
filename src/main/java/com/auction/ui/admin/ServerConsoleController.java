package com.auction.ui.admin;

import com.auction.server.AuctionServer;
import com.auction.server.pool.DatabaseConnectionPool;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.ItemRepository;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

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

    private final ObservableList<String> clientList = FXCollections.observableArrayList();
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private long serverStartTime = 0;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private enum ServerState { OFFLINE, STARTING, ONLINE, STOPPING }
    private ServerState serverState = ServerState.OFFLINE;

    private ItemRepository itemRepo;
    private BidRepository bidRepo;

    @FXML
    public void initialize() {
        lvClients.setItems(clientList);
        lblPort.setText("9999");

        // Khởi tạo repositories
        try {
            itemRepo = ItemRepository.getInstance();
            bidRepo = BidRepository.getInstance();

            // Kiểm tra database connection
            if (DatabaseConnectionPool.getInstance().testConnection()) {
                addLog("✅ Connected to database");
            } else {
                addLog("❌ Cannot connect to database");
            }
        } catch (Exception e) {
            addLog("❌ Database error: " + e.getMessage());
        }

        applyState(ServerState.OFFLINE);
        addLog("🟢 Server Console initialized");

        // Kiểm tra server đã chạy chưa
        if (AuctionServer.isRunning()) {
            serverStartTime = System.currentTimeMillis();
            applyState(ServerState.ONLINE);
            addLog("✅ Server is already ONLINE");
            refreshStats();
            refreshClientList();
        } else {
            refreshStats();
        }

        startRealtimeClock();
        startStatsUpdater();
    }

    @FXML
    private void handleStartServer() {
        if (serverState != ServerState.OFFLINE && serverState != ServerState.STOPPING) {
            addLog("⚠️ Server is already " + serverState);
            return;
        }

        applyState(ServerState.STARTING);
        addLog("🚀 Starting server on port 9999...");

        new Thread(() -> {
            try {
                AuctionServer.start();
                serverStartTime = System.currentTimeMillis();
                Platform.runLater(() -> {
                    if (AuctionServer.isRunning()) {
                        applyState(ServerState.ONLINE);
                        addLog("✅ Server is ONLINE");
                        refreshStats();
                        refreshClientList();
                    } else {
                        applyState(ServerState.OFFLINE);
                        addLog("❌ Server failed to start");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    addLog("❌ Server error: " + e.getMessage());
                    applyState(ServerState.OFFLINE);
                });
            }
        }).start();
    }

    @FXML
    private void handleStopServer() {
        if (serverState != ServerState.ONLINE) {
            addLog("⚠️ Server is not ONLINE (current: " + serverState + ")");
            return;
        }

        applyState(ServerState.STOPPING);
        addLog("🛑 Stopping server...");

        new Thread(() -> {
            try {
                AuctionServer.stop();
                Platform.runLater(() -> {
                    clientList.clear();
                    updateClientCount();
                    applyState(ServerState.OFFLINE);
                    serverStartTime = 0;
                    addLog("✅ Server stopped");
                    refreshStats();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    addLog("❌ Error stopping server: " + e.getMessage());
                    applyState(ServerState.ONLINE);
                });
            }
        }).start();
    }

    @FXML
    private void handleRestart() {
        if (serverState != ServerState.ONLINE && serverState != ServerState.OFFLINE) {
            addLog("⚠️ Cannot restart while " + serverState);
            return;
        }

        addLog("🔄 Restarting server...");

        new Thread(() -> {
            try {
                if (AuctionServer.isRunning()) {
                    AuctionServer.stop();
                    Thread.sleep(2000);
                }
            } catch (Exception e) {
                addLog("Stop error: " + e.getMessage());
            }

            Platform.runLater(() -> {
                clientList.clear();
                updateClientCount();
                applyState(ServerState.STARTING);
                addLog("🚀 Starting server after restart...");

                try {
                    AuctionServer.start();
                    serverStartTime = System.currentTimeMillis();
                    new Thread(() -> {
                        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                        Platform.runLater(() -> {
                            if (AuctionServer.isRunning()) {
                                applyState(ServerState.ONLINE);
                                addLog("✅ Server restarted successfully");
                                refreshStats();
                                refreshClientList();
                            } else {
                                applyState(ServerState.OFFLINE);
                                addLog("❌ Server failed to restart");
                            }
                        });
                    }).start();
                } catch (Exception e) {
                    addLog("Start error: " + e.getMessage());
                    applyState(ServerState.OFFLINE);
                }
            });
        }).start();
    }

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
        updateSubscriptionsCount();
    }

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

    private void applyState(ServerState state) {
        serverState = state;
        Platform.runLater(() -> {
            switch (state) {
                case OFFLINE:
                    btnStart.setDisable(false);
                    btnStop.setDisable(true);
                    btnRestart.setDisable(true);
                    btnKick.setDisable(true);
                    lblServerStatus.setText("🔴 OFFLINE");
                    break;
                case STARTING:
                    btnStart.setDisable(true);
                    btnStop.setDisable(true);
                    btnRestart.setDisable(true);
                    btnKick.setDisable(true);
                    lblServerStatus.setText("🟡 STARTING...");
                    break;
                case ONLINE:
                    btnStart.setDisable(true);
                    btnStop.setDisable(false);
                    btnRestart.setDisable(false);
                    btnKick.setDisable(false);
                    lblServerStatus.setText("🟢 ONLINE");
                    break;
                case STOPPING:
                    btnStart.setDisable(true);
                    btnStop.setDisable(true);
                    btnRestart.setDisable(true);
                    btnKick.setDisable(true);
                    lblServerStatus.setText("🟠 STOPPING...");
                    break;
            }
        });
    }

    private void refreshStats() {
        new Thread(() -> {
            try {
                int activeItems = 0;
                int totalBids = 0;

                if (itemRepo != null) {
                    activeItems = itemRepo.countActive();
                    totalBids = bidRepo.countAll();
                }

                final int finalActiveItems = activeItems;
                final int finalTotalBids = totalBids;

                Platform.runLater(() -> {
                    lblActiveItems.setText(String.valueOf(finalActiveItems));
                    lblTotalBids.setText(String.valueOf(finalTotalBids));
                    lblThreadCount.setText(String.valueOf(Thread.activeCount()));

                    // Màu sắc theo số lượng
                    if (finalActiveItems == 0) {
                        lblActiveItems.setStyle("-fx-text-fill: #f59e0b;");
                    } else {
                        lblActiveItems.setStyle("-fx-text-fill: #22c55e;");
                    }

                    if (finalTotalBids == 0) {
                        lblTotalBids.setStyle("-fx-text-fill: #f59e0b;");
                    } else {
                        lblTotalBids.setStyle("-fx-text-fill: #22c55e;");
                    }
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    lblActiveItems.setText("?");
                    lblTotalBids.setText("?");
                    addLog("⚠️ Cannot get stats from database: " + e.getMessage());
                });
            }
        }).start();
    }

    private void refreshClientList() {
        // TODO: Lấy danh sách client thật từ ServerSocketManager
        // Hiện tại dùng mẫu - có thể thay bằng dữ liệu thật sau
        Platform.runLater(() -> {
            clientList.clear();
            clientList.add("🔵 bidder1 (sub: 3) - 192.168.1.10");
            clientList.add("🔵 bidder2 (sub: 2) - 192.168.1.11");
            clientList.add("🔵 bidder3 (sub: 5) - 192.168.1.12");
            clientList.add("🔵 bidder4 (sub: 1) - 192.168.1.13");
            clientList.add("🔵 bidder5 (sub: 4) - 192.168.1.14");
            clientList.add("🔵 bidder6 (sub: 2) - 192.168.1.15");
            clientList.add("🔵 bidder7 (sub: 3) - 192.168.1.16");
            clientList.add("🟣 admin (sub: 0) - 192.168.1.5");
            updateClientCount();
            updateSubscriptionsCount();
        });
    }

    private void startStatsUpdater() {
        scheduler.scheduleAtFixedRate(() -> {
            refreshStats();
        }, 5, 10, TimeUnit.SECONDS);
    }

    private void updateClientCount() {
        lblClientCount.setText(String.valueOf(clientList.size()));
    }

    private void updateSubscriptionsCount() {
        int totalSubs = 0;
        for (String client : clientList) {
            int subIndex = client.indexOf("sub:");
            if (subIndex > 0) {
                int endIndex = client.indexOf(")", subIndex);
                try {
                    String subStr = client.substring(subIndex + 4, endIndex);
                    totalSubs += Integer.parseInt(subStr);
                } catch (NumberFormatException ignored) {}
            }
        }
        lblSubscriptions.setText(String.valueOf(totalSubs));
    }

    private void startRealtimeClock() {
        Thread clockThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
                String time = LocalDateTime.now().format(TIME_FORMAT);
                Platform.runLater(() -> lblStatus.setText("🕐 " + time + " | 📊 Server Console"));
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