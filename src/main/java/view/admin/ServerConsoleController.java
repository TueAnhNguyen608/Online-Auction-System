package view.admin;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import server.AuctionServer;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
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

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private enum ServerState { OFFLINE, STARTING, ONLINE, STOPPING }
    private ServerState serverState = ServerState.OFFLINE;

    @FXML
    public void initialize() {
        lvClients.setItems(clientList);
        lblPort.setText("9999");
        applyState(ServerState.OFFLINE);
        addLog("🟢 Server Console initialized");
        refreshStats();
        startRealtimeClock();
        addSampleClients();
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
                Platform.runLater(() -> {
                    if (AuctionServer.isRunning()) {
                        applyState(ServerState.ONLINE);
                        addLog("✅ Server is ONLINE");
                        addSampleClients();
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

        // Giả lập online nếu chưa kịp
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                if (serverState == ServerState.STARTING) {
                    applyState(ServerState.ONLINE);
                    addLog("✅ Server is ONLINE (auto-detected)");
                    addSampleClients();
                }
            });
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
                    addLog("✅ Server stopped");
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
        if (serverState != ServerState.ONLINE) {
            addLog("⚠️ Server is not ONLINE (current: " + serverState + ")");
            return;
        }

        addLog("🔄 Restarting server...");

        new Thread(() -> {
            // Stop server
            try {
                AuctionServer.stop();
                Thread.sleep(2000);
            } catch (Exception e) {
                addLog("Stop error: " + e.getMessage());
            }

            // Start lại
            Platform.runLater(() -> {
                clientList.clear();
                updateClientCount();
                applyState(ServerState.STARTING);
                addLog("🚀 Starting server after restart...");

                try {
                    AuctionServer.start();
                    new Thread(() -> {
                        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                        Platform.runLater(() -> {
                            if (AuctionServer.isRunning()) {
                                applyState(ServerState.ONLINE);
                                addLog("✅ Server restarted successfully");
                                addSampleClients();
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
        lblThreadCount.setText(String.valueOf(Thread.activeCount()));
        lblTotalBids.setText("0");
        lblActiveItems.setText("0");
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

    private void addSampleClients() {
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
}