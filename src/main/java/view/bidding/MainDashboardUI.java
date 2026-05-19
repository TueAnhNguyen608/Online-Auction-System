package view.bidding;

import client.AuctionClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import common.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.stage.Stage;
import view.common.SessionManager;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class MainDashboardUI implements Initializable {

    @FXML private Label  usernameLabel;
    @FXML private Label  welcomeLabel;
    @FXML private Label  statTotal;
    @FXML private Label  statSessions;
    @FXML private Label  statValue;
    @FXML private Label  statEnding;
    @FXML private Button btnNotification;
    @FXML private Label  lblNotifCount;
    @FXML private Label  statTotalSub;
    @FXML private Label  statSessionsSub;
    @FXML private Label  statEndingSub;
    @FXML private HBox   userMenuBox;

    private static String currentUser = "User";
    private Popup userMenuPopup;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public static void setCurrentUser(String name) {
        if (name != null && !name.isBlank()) currentUser = name;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        usernameLabel.setText(currentUser);
        welcomeLabel.setText("Xin chào, " + currentUser + "!");
        updateNotifCount(0);
        loadStats();
        countUnreadNotifications();
        if (userMenuBox != null) {
            userMenuBox.setOnMouseClicked(e -> showUserMenu());
        }
    }

    // ── Dropdown menu user ────────────────────────────────────────
    private void showUserMenu() {
        if (userMenuPopup != null && userMenuPopup.isShowing()) {
            userMenuPopup.hide(); return;
        }
        userMenuPopup = new Popup();
        userMenuPopup.setAutoHide(true);

        VBox menu = new VBox(0);
        menu.setStyle("-fx-background-color: #0e1c40; -fx-border-color: #1E3A5F;" +
                "-fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 12, 0, 0, 3);");
        menu.setPrefWidth(200);

        Label lblHeader = new Label(currentUser);
        lblHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblHeader.setTextFill(Color.web("#F5C518"));
        lblHeader.setPadding(new Insets(12, 16, 10, 16));
        lblHeader.setMaxWidth(Double.MAX_VALUE);
        lblHeader.setStyle("-fx-border-color: transparent transparent #1E3A5F transparent; -fx-border-width: 0 0 1 0;");

        Label btnProfile = menuItem("👤  Thông tin cá nhân");
        btnProfile.setOnMouseClicked(e -> { userMenuPopup.hide(); showProfileDialog(); });

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #1E3A5F;");

        Label btnLogout = menuItem("🚪  Đăng xuất");
        btnLogout.setOnMouseClicked(e -> { userMenuPopup.hide(); showLogoutMenu(); });

        menu.getChildren().addAll(lblHeader, btnProfile, sep, btnLogout);
        userMenuPopup.getContent().add(menu);

        javafx.geometry.Bounds bounds = userMenuBox.localToScreen(userMenuBox.getBoundsInLocal());
        userMenuPopup.show(userMenuBox.getScene().getWindow(),
                bounds.getMaxX() - 200, bounds.getMaxY() + 4);
    }

    // ── Submenu đăng xuất ────────────────────────────────────────
    private void showLogoutMenu() {
        Popup logoutPopup = new Popup();
        logoutPopup.setAutoHide(true);

        VBox menu = new VBox(0);
        menu.setStyle("-fx-background-color: #0e1c40; -fx-border-color: #1E3A5F;" +
                "-fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 12, 0, 0, 3);");
        menu.setPrefWidth(220);

        Label lblHeader = new Label("Chuyển tài khoản");
        lblHeader.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblHeader.setTextFill(Color.web("#8AADD0"));
        lblHeader.setPadding(new Insets(10, 16, 8, 16));
        lblHeader.setMaxWidth(Double.MAX_VALUE);
        lblHeader.setStyle("-fx-border-color: transparent transparent #1E3A5F transparent; -fx-border-width: 0 0 1 0;");
        menu.getChildren().add(lblHeader);

        List<User> accounts = SessionManager.getSavedAccounts();
        for (User account : accounts) {
            if (!account.getUsername().equals(currentUser)) {
                Label btnAccount = menuItem("🔘  " + account.getUsername());
                btnAccount.setOnMouseClicked(e -> { logoutPopup.hide(); switchAccount(account); });
                menu.getChildren().add(btnAccount);
            }
        }

        if (accounts.size() > 1) {
            Region sep = new Region();
            sep.setPrefHeight(1);
            sep.setStyle("-fx-background-color: #1E3A5F;");
            menu.getChildren().add(sep);
        }

        Label btnAddAccount = menuItem("➕  Thêm tài khoản mới");
        btnAddAccount.setStyle(btnAddAccount.getStyle() + "-fx-text-fill: #F5C518;");
        btnAddAccount.setOnMouseClicked(e -> { logoutPopup.hide(); goToLogin(); });
        menu.getChildren().add(btnAddAccount);

        logoutPopup.getContent().add(menu);
        javafx.geometry.Bounds bounds = userMenuBox.localToScreen(userMenuBox.getBoundsInLocal());
        logoutPopup.show(userMenuBox.getScene().getWindow(),
                bounds.getMaxX() - 220, bounds.getMaxY() + 4);
    }

    private Label menuItem(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", 13));
        lbl.setTextFill(Color.web("#CCDFF5"));
        lbl.setPadding(new Insets(10, 16, 10, 16));
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setCursor(javafx.scene.Cursor.HAND);
        String normal = "-fx-background-color: transparent;";
        String hover  = "-fx-background-color: #162550;";
        lbl.setStyle(normal);
        lbl.setOnMouseEntered(e -> lbl.setStyle(hover));
        lbl.setOnMouseExited(e  -> lbl.setStyle(normal));
        return lbl;
    }

    private void switchAccount(User account) {
        new Thread(() -> {
            try {
                AuctionClient newClient = new AuctionClient();
                newClient.connect("localhost", 9999);
                String savedPassword = account.getPassword();
                ResponseMessage resp = newClient.login(
                        account.getUsername(),
                        savedPassword != null ? savedPassword : "123456");
                Platform.runLater(() -> {
                    if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                        SessionManager.logout();
                        account.setPassword(savedPassword);
                        SessionManager.setCurrentUser(account);
                        SessionManager.setClient(newClient);
                        Stage stage = (Stage) usernameLabel.getScene().getWindow();
                        MainDashboardUI.show(stage, account.getUsername());
                    } else {
                        goToLogin();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(this::goToLogin);
            }
        }).start();
    }

    private void goToLogin() {
        SessionManager.logout();
        Stage stage = (Stage) usernameLabel.getScene().getWindow();
        try {
            new view.LoginApp().start(new Stage());
            stage.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Dialog Thông tin cá nhân ─────────────────────────────────
    private void showProfileDialog() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        Stage dialog = new Stage();
        dialog.setTitle("Thông tin cá nhân");
        dialog.initOwner(usernameLabel.getScene().getWindow());
        dialog.setResizable(false);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #0e1c40;");
        root.setPrefWidth(440);

        Label title = new Label("👤  Thông tin cá nhân");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#F5C518"));

        // Thông tin chỉ xem
        VBox infoBox = new VBox(10);
        infoBox.setStyle("-fx-background-color: #162550; -fx-background-radius: 8; -fx-padding: 14;");
        infoBox.getChildren().addAll(
                infoRow("Tên đăng nhập:", user.getUsername()),
                infoRow("Vai trò:", user.getRole()),
                infoRow("Số dư:", String.format("%,.0f đ", user.getBalance()).replace(",", "."))
        );

        // ĐÃ THÊM: Thông tin có thể sửa (Email, Họ tên, SĐT)
        Label lblInfoTitle = new Label("✏️  Thông tin cá nhân");
        lblInfoTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblInfoTitle.setTextFill(Color.web("#CCDFF5"));

        VBox emailBox = new VBox(10);
        emailBox.setStyle("-fx-background-color: #162550; -fx-background-radius: 8; -fx-padding: 14;");

        // Email
        Label lblEmailLbl = new Label("Email");
        lblEmailLbl.setFont(Font.font("Segoe UI", 11));
        lblEmailLbl.setTextFill(Color.web("#8AADD0"));
        TextField txtEmail = new TextField(user.getEmail() != null ? user.getEmail() : "");
        txtEmail.setPromptText("Nhập email");
        txtEmail.setStyle(inputStyle());

        // Họ và tên
        Label lblNameLbl = new Label("Họ và tên");
        lblNameLbl.setFont(Font.font("Segoe UI", 11));
        lblNameLbl.setTextFill(Color.web("#8AADD0"));
        TextField txtFullName = new TextField(user.getFullName() != null ? user.getFullName() : "");
        txtFullName.setPromptText("Nhập họ và tên");
        txtFullName.setStyle(inputStyle());

        // Số điện thoại
        Label lblPhoneLbl = new Label("Số điện thoại");
        lblPhoneLbl.setFont(Font.font("Segoe UI", 11));
        lblPhoneLbl.setTextFill(Color.web("#8AADD0"));
        TextField txtPhone = new TextField(user.getPhone() != null ? user.getPhone() : "");
        txtPhone.setPromptText("Nhập số điện thoại");
        txtPhone.setStyle(inputStyle());

        Label lblEmailMsg = new Label();
        lblEmailMsg.setFont(Font.font("Segoe UI", 12));
        lblEmailMsg.setWrapText(true);

        Button btnSaveEmail = new Button("💾  Lưu thông tin");
        btnSaveEmail.setMaxWidth(Double.MAX_VALUE);
        btnSaveEmail.setPrefHeight(38);
        btnSaveEmail.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btnSaveEmail.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        btnSaveEmail.setOnAction(e -> {
            String newEmail    = txtEmail.getText().trim();
            String newFullName = txtFullName.getText().trim();
            String newPhone    = txtPhone.getText().trim();
            if (newEmail.isEmpty()) {
                lblEmailMsg.setText("Vui lòng nhập email!");
                lblEmailMsg.setTextFill(Color.web("#ef4444")); return;
            }
            new Thread(() -> {
                try {
                    AuctionClient client = SessionManager.getClient();
                    if (client == null) throw new Exception("Chưa kết nối server");
                    // ĐÃ THÊM: dùng UPDATE_PROFILE thay vì UPDATE_USER_INFO
                    java.util.Map<String, Object> profileData = new java.util.HashMap<>();
                    profileData.put("email", newEmail);
                    profileData.put("fullName", newFullName);
                    profileData.put("phone", newPhone);
                    ResponseMessage resp = client.sendRequest(CommandType.UPDATE_PROFILE, profileData);
                    Platform.runLater(() -> {
                        if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                            lblEmailMsg.setText("✅ Đã cập nhật thông tin!");
                            lblEmailMsg.setTextFill(Color.web("#22c55e"));
                            user.setEmail(newEmail);
                        } else {
                            lblEmailMsg.setText("✕ " + (resp != null ? resp.getMessage() : "Lỗi server"));
                            lblEmailMsg.setTextFill(Color.web("#ef4444"));
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        lblEmailMsg.setText("✕ Lỗi: " + ex.getMessage());
                        lblEmailMsg.setTextFill(Color.web("#ef4444"));
                    });
                }
            }).start();
        });
        emailBox.getChildren().addAll(
                lblEmailLbl, txtEmail,
                lblNameLbl, txtFullName,
                lblPhoneLbl, txtPhone,
                lblEmailMsg, btnSaveEmail);

        // Đổi mật khẩu (ẩn/hiện)
        HBox passHeader = new HBox(10);
        passHeader.setAlignment(Pos.CENTER_LEFT);
        passHeader.setCursor(javafx.scene.Cursor.HAND);
        passHeader.setStyle("-fx-background-color: #162550; -fx-background-radius: 8; -fx-padding: 12 14 12 14;");

        Label lblPassTitle = new Label("🔒  Đổi mật khẩu");
        lblPassTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblPassTitle.setTextFill(Color.web("#CCDFF5"));
        lblPassTitle.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(lblPassTitle, Priority.ALWAYS);

        Label lblArrow = new Label("▼");
        lblArrow.setFont(Font.font("Segoe UI", 12));
        lblArrow.setTextFill(Color.web("#8AADD0"));
        passHeader.getChildren().addAll(lblPassTitle, lblArrow);

        VBox passPanel = new VBox(10);
        passPanel.setStyle("-fx-background-color: #0e1c40; -fx-background-radius: 0 0 8 8;" +
                "-fx-border-color: #162550; -fx-border-radius: 0 0 8 8; -fx-border-width: 0 1 1 1; -fx-padding: 14;");
        passPanel.setVisible(false);
        passPanel.setManaged(false);

        PasswordField txtOldPass = new PasswordField();
        txtOldPass.setPromptText("Mật khẩu hiện tại");
        txtOldPass.setStyle(inputStyle());

        PasswordField txtNewPass = new PasswordField();
        txtNewPass.setPromptText("Mật khẩu mới");
        txtNewPass.setStyle(inputStyle());

        PasswordField txtConfPass = new PasswordField();
        txtConfPass.setPromptText("Xác nhận mật khẩu mới");
        txtConfPass.setStyle(inputStyle());

        Label lblPassMsg = new Label();
        lblPassMsg.setFont(Font.font("Segoe UI", 12));
        lblPassMsg.setWrapText(true);

        Button btnChangePass = new Button("Xác nhận đổi mật khẩu");
        btnChangePass.setMaxWidth(Double.MAX_VALUE);
        btnChangePass.setPrefHeight(38);
        btnChangePass.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btnChangePass.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        btnChangePass.setOnAction(e -> {
            String oldPass = txtOldPass.getText().trim();
            String newPass = txtNewPass.getText().trim();
            String confPass = txtConfPass.getText().trim();
            if (oldPass.isEmpty() || newPass.isEmpty() || confPass.isEmpty()) {
                lblPassMsg.setText("Vui lòng nhập đầy đủ!");
                lblPassMsg.setTextFill(Color.web("#ef4444")); return;
            }
            if (!newPass.equals(confPass)) {
                lblPassMsg.setText("Mật khẩu mới không khớp!");
                lblPassMsg.setTextFill(Color.web("#ef4444")); return;
            }
            if (newPass.length() < 6) {
                lblPassMsg.setText("Mật khẩu tối thiểu 6 ký tự!");
                lblPassMsg.setTextFill(Color.web("#ef4444")); return;
            }
            new Thread(() -> {
                try {
                    AuctionClient client = SessionManager.getClient();
                    if (client == null) throw new Exception("Chưa kết nối server");
                    // ĐÃ SỬA: dùng Map thay vì Object[]
                    Map<String, String> passData = new HashMap<>();
                    passData.put("oldPassword", oldPass);
                    passData.put("newPassword", newPass);
                    ResponseMessage resp = client.sendRequest(CommandType.CHANGE_PASSWORD, passData);
                    Platform.runLater(() -> {
                        if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                            lblPassMsg.setText("✅ Đổi mật khẩu thành công!");
                            lblPassMsg.setTextFill(Color.web("#22c55e"));
                            txtOldPass.clear(); txtNewPass.clear(); txtConfPass.clear();
                            // Xóa password cũ → lần sau chuyển acc sẽ hỏi lưu lại
                            SessionManager.clearPassword(user.getUsername());
                        } else {
                            lblPassMsg.setText("✕ " + (resp != null ? resp.getMessage() : "Lỗi server"));
                            lblPassMsg.setTextFill(Color.web("#ef4444"));
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        lblPassMsg.setText("✕ Lỗi: " + ex.getMessage());
                        lblPassMsg.setTextFill(Color.web("#ef4444"));
                    });
                }
            }).start();
        });
        passPanel.getChildren().addAll(txtOldPass, txtNewPass, txtConfPass, lblPassMsg, btnChangePass);

        final boolean[] passExpanded = {false};
        passHeader.setOnMouseClicked(e -> {
            passExpanded[0] = !passExpanded[0];
            passPanel.setVisible(passExpanded[0]);
            passPanel.setManaged(passExpanded[0]);
            lblArrow.setText(passExpanded[0] ? "▲" : "▼");
            passHeader.setStyle(passExpanded[0]
                    ? "-fx-background-color: #1E3A6E; -fx-background-radius: 8 8 0 0; -fx-padding: 12 14 12 14;"
                    : "-fx-background-color: #162550; -fx-background-radius: 8; -fx-padding: 12 14 12 14;");
            dialog.sizeToScene();
        });

        VBox passBox = new VBox(0, passHeader, passPanel);
        root.getChildren().addAll(title, infoBox, lblInfoTitle, emailBox, passBox);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #0e1c40; -fx-background: #0e1c40;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Scene scene = new Scene(scroll, 440, 480);
        scene.setFill(Color.web("#0e1c40"));
        dialog.setScene(scene);
        dialog.show();
    }

    private HBox infoRow(String label, String value) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", 12));
        lbl.setTextFill(Color.web("#8AADD0"));
        lbl.setMinWidth(120);
        Label val = new Label(value);
        val.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        val.setTextFill(Color.web("#FFFFFF"));
        row.getChildren().addAll(lbl, val);
        return row;
    }

    private String inputStyle() {
        return "-fx-background-color: #0e1c40; -fx-border-color: #1E3A5F;" +
                "-fx-border-radius: 6; -fx-background-radius: 6;" +
                "-fx-text-fill: #FFFFFF; -fx-prompt-text-fill: #4a6a9a;" +
                "-fx-font-size: 13; -fx-pref-height: 38; -fx-padding: 0 12;";
    }

    // ── Load stats ───────────────────────────────────────────────
    private void loadStats() {
        new Thread(() -> {
            try {
                AuctionClient client = SessionManager.getClient();
                if (client == null) throw new Exception("Chưa có kết nối");
                ResponseMessage resp = client.getActiveItems();
                if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                    java.lang.reflect.Type listType = new TypeToken<List<Item>>(){}.getType();
                    List<Item> items = gson.fromJson(gson.toJson(resp.getData()), listType);
                    if (items != null) {
                        int total = items.size(), active = 0, ending = 0;
                        long totalValue = 0;
                        for (Item item : items) {
                            totalValue += (long) item.getCurrentPrice();
                            if ("ACTIVE".equals(item.getStatus())) active++;
                            if (item.getEndTime() != null) {
                                long secs = java.time.Duration.between(
                                        LocalDateTime.now(), item.getEndTime()).getSeconds();
                                if (secs > 0 && secs <= 86400) ending++;
                            }
                        }
                        final int fTotal = total, fActive = active, fEnding = ending;
                        final long fValue = totalValue;
                        Platform.runLater(() -> {
                            statTotal.setText(String.valueOf(fTotal));
                            statTotalSub.setText("+" + fTotal + " sản phẩm đang đấu giá");
                            statSessions.setText(String.valueOf(fTotal));
                            statSessionsSub.setText("Đang diễn ra: " + fActive);
                            statValue.setText(String.format("%,d", fValue).replace(",", ".") + " đ");
                            statEnding.setText(String.valueOf(fEnding));
                            statEndingSub.setText(fEnding > 0 ? "Trong 24 giờ tới" : "Không có phiên nào");
                        });
                        return;
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠ Dùng stats mẫu: " + e.getMessage());
            }
            Platform.runLater(this::loadSampleStats);
        }).start();
    }

    private void loadSampleStats() {
        long[] prices = {25_000_000L,22_000_000L,20_000_000L,15_000_000L,18_000_000L,
                45_000_000L,35_000_000L,28_000_000L,32_000_000L,5_000_000L,
                4_500_000L,6_000_000L,15_000_000L,7_000_000L,3_000_000L,
                2_000_000L,28_000_000L,22_000_000L};
        long[] days = {7,5,10,6,8,12,9,7,11,4,3,5,8,6,10,15,20,18};
        int total = prices.length, active = 0, ending = 0;
        long totalValue = 0;
        for (int i = 0; i < total; i++) {
            totalValue += prices[i];
            if (days[i] > 0) active++;
            if (days[i] == 0) ending++;
        }
        statTotal.setText(String.valueOf(total));
        statTotalSub.setText("+" + total + " sản phẩm đang đấu giá");
        statSessions.setText(String.valueOf(total));
        statSessionsSub.setText("Đang diễn ra: " + active);
        statValue.setText(String.format("%,d", totalValue).replace(",", ".") + " đ");
        statEnding.setText(String.valueOf(ending));
        statEndingSub.setText(ending > 0 ? "Trong 24 giờ tới" : "Không có phiên nào");
    }

    private void countUnreadNotifications() {
        // Dùng count từ SessionManager (được cập nhật bởi NotificationUI và RealTimeAlertPanel)
        int cached = SessionManager.getUnreadNotifCount();
        if (cached > 0) {
            updateNotifCount(cached);
            return;
        }
        // Nếu chưa có cached thì fetch từ server
        new Thread(() -> {
            try {
                AuctionClient client = SessionManager.getClient();
                User user = SessionManager.getCurrentUser();
                if (client == null || user == null) return;
                ResponseMessage resp = client.sendRequest(CommandType.GET_MY_BID_HISTORY, user.getId());
                if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                    java.lang.reflect.Type listType = new TypeToken<List<Bid>>(){}.getType();
                    List<Bid> bids = gson.fromJson(gson.toJson(resp.getData()), listType);
                    int count = bids != null ? bids.size() : 0;
                    SessionManager.setUnreadNotifCount(count);
                    Platform.runLater(() -> updateNotifCount(count));
                }
            } catch (Exception e) {
                System.out.println("Không đếm được thông báo: " + e.getMessage());
            }
        }).start();
    }

    // ĐÃ THÊM: gọi khi quay lại từ NotificationUI để refresh badge
    public static void refreshNotifBadge() {
        SessionManager.setUnreadNotifCount(0);
    }

    private void updateNotifCount(int count) {
        if (count <= 0) {
            lblNotifCount.setText("");
        } else if (count <= 9) {
            lblNotifCount.setText("(" + count + ")");
            lblNotifCount.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
        } else {
            lblNotifCount.setText("(9+)");
            lblNotifCount.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");
        }
    }

    @FXML private void onSanPham()      { navigate("/fxml/bidding/ProductListPanel.fxml", "Sản phẩm"); }
    @FXML private void onAutoBid()      { navigate("/fxml/bidding/AutoBidPanel.fxml", "Đấu giá tự động"); }
    @FXML private void onLichSu()       { navigate("/fxml/history/MyBidsHistoryUI.fxml", "Lịch sử đấu giá"); }
    @FXML private void onNotification() {
        // Reset badge khi mở NotificationUI
        SessionManager.clearUnreadNotif();
        updateNotifCount(0);
        navigate("/fxml/notification/NotificationUI.fxml", "Thông báo");
    }

    private void navigate(String fxmlPath, String title) {
        try {
            URL res = getClass().getResource(fxmlPath);
            if (res == null) { info("Màn hình \"" + title + "\" đang được phát triển."); return; }
            FXMLLoader loader = new FXMLLoader(res);
            Parent root = loader.load();
            Stage stage = (Stage) usernameLabel.getScene().getWindow();
            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            URL css = getClass().getResource("/css/bidding_style.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title + " – Hệ thống đấu giá");
        } catch (Exception e) { info("Lỗi mở màn hình: " + e.getMessage()); }
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Thông báo"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    public static void show(Stage stage, String username) {
        try {
            setCurrentUser(username);
            URL fxml = MainDashboardUI.class.getResource("/fxml/bidding/MainDashboardUI.fxml");
            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();
            Scene scene = new Scene(root, 1280, 780);
            URL css = MainDashboardUI.class.getResource("/css/bidding_style.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Hệ thống đấu giá");
            stage.setMinWidth(960);
            stage.setMinHeight(640);
            stage.setResizable(true);
            boolean wasMaximized = stage.isMaximized();
            if (wasMaximized) stage.setMaximized(false);
            stage.show();
            if (wasMaximized) stage.setMaximized(true);
        } catch (Exception e) { e.printStackTrace(); }
    }
}