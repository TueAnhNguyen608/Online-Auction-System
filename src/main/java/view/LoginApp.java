package view;

import client.AuctionClient;
import common.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.Map;

public class LoginApp extends Application {

    private Gson gson;

    private static final String BG      = "#0a0e27";
    private static final String BG_CARD = "#11153a";
    private static final String BG_IN   = "#1a1f45";
    private static final String BORDER  = "#2d3270";
    private static final String PURPLE  = "#2563eb";
    private static final String GOLD    = "#f59e0b";
    private static final String TEXT    = "#f0f4ff";
    private static final String MUTED   = "#8899bb";
    private static final String LABEL   = "#c4cfe8";

    @Override
    public void start(Stage stage) {
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
        stage.setTitle("Hệ thống Đấu giá - Đăng nhập");

        HBox root = new HBox();
        root.setPrefSize(900, 580);
        root.setStyle("-fx-background-color:" + BG + ";");

        // ── TRÁI ─────────────────────────────────────────────────
        VBox left = new VBox(0);
        left.setPrefWidth(420);
        left.setStyle("-fx-background-color: linear-gradient(to bottom, #0c1030, #16103a);"
                + "-fx-border-color:" + BORDER + ";-fx-border-width:0 1 0 0;");

        // Logo
        HBox logo = buildLogo();
        logo.setPadding(new Insets(22, 20, 18, 22));
        logo.setStyle("-fx-border-color:transparent transparent " + BORDER + " transparent;"
                + "-fx-border-width:0 0 1 0;");

        // Ảnh búa + text
        VBox imgArea = new VBox(18);
        imgArea.setAlignment(Pos.CENTER);
        imgArea.setPadding(new Insets(30, 20, 20, 20));
        VBox.setVgrow(imgArea, Priority.ALWAYS);

        ImageView gavelImg = loadImage("/images/gavel.png", 300, 280);
        // PNG đã cắt phông, hiển thị trực tiếp

        Label t1 = new Label("Chào mừng trở lại!");
        t1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        t1.setTextFill(Color.web(TEXT));

        Label t2 = new Label("Đăng nhập để tiếp tục tham gia\nđấu giá và khám phá những\nsản phẩm tuyệt vời.");
        t2.setFont(Font.font("Segoe UI", 13));
        t2.setTextFill(Color.web(MUTED));
        t2.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        t2.setLineSpacing(4);

        if (gavelImg != null) imgArea.getChildren().addAll(gavelImg, t1, t2);
        else imgArea.getChildren().addAll(new Label("🔨") {{ setFont(Font.font(80)); }}, t1, t2);

        left.getChildren().addAll(logo, imgArea);

        // ── PHẢI – form ───────────────────────────────────────────
        VBox right = new VBox();
        right.setAlignment(Pos.CENTER);
        right.setPadding(new Insets(0, 40, 0, 24));
        HBox.setHgrow(right, Priority.ALWAYS);

        VBox card = new VBox(0);
        card.setMaxWidth(380);
        card.setPadding(new Insets(38, 38, 38, 38));
        card.setStyle("-fx-background-color:" + BG_CARD + ";"
                + "-fx-background-radius:16;-fx-border-color:" + BORDER + ";"
                + "-fx-border-radius:16;-fx-border-width:1;"
                + "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.25),24,0,0,6);");

        Label title = new Label("Đăng nhập");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.web(TEXT));

        Label sub = new Label("Nhập thông tin tài khoản của bạn");
        sub.setFont(Font.font("Segoe UI", 13));
        sub.setTextFill(Color.web(MUTED));
        VBox.setMargin(sub, new Insets(4, 0, 26, 0));

        // Username
        Label lUser = fl("Tên đăng nhập");
        TextField txtUser = new TextField();
        txtUser.setPromptText("Nhập tên đăng nhập");
        HBox userBox = inputBox("👤", txtUser, null);
        VBox.setMargin(userBox, new Insets(6, 0, 16, 0));

        // Password
        Label lPass = fl("Mật khẩu");
        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Nhập mật khẩu");
        txtPass.setMaxWidth(Double.MAX_VALUE);
        txtPass.setStyle(fStyle());

        TextField txtVis = new TextField();
        txtVis.setPromptText("Nhập mật khẩu");
        txtVis.setMaxWidth(Double.MAX_VALUE);
        txtVis.setStyle(fStyle());
        txtVis.setVisible(false); txtVis.setManaged(false);
        txtPass.textProperty().bindBidirectional(txtVis.textProperty());

        Button eye = eyeBtn();
        Label lockIco = ico("🔒");
        StackPane passToggle = new StackPane(txtPass, txtVis);
        HBox.setHgrow(passToggle, Priority.ALWAYS);
        HBox passBox = new HBox(8, lockIco, passToggle, eye);
        passBox.setAlignment(Pos.CENTER_LEFT);
        passBox.setPadding(new Insets(0, 12, 0, 14));
        passBox.setPrefHeight(50);
        passBox.setStyle(wStyle());
        VBox.setMargin(passBox, new Insets(6, 0, 14, 0));

        final boolean[] shown = {false};
        eye.setOnAction(e -> {
            shown[0] = !shown[0];
            txtPass.setVisible(!shown[0]); txtPass.setManaged(!shown[0]);
            txtVis.setVisible(shown[0]);   txtVis.setManaged(shown[0]);
            eye.setText(shown[0] ? "🙈" : "👁");
        });

        // Ghi nhớ
        CheckBox chk = new CheckBox("Ghi nhớ đăng nhập");
        chk.setTextFill(Color.web(MUTED)); chk.setFont(Font.font("Segoe UI", 12));
        HBox remRow = new HBox(chk); remRow.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(remRow, new Insets(0, 0, 22, 0));

        // Nút đăng nhập
        Button btnLogin = new Button("Đăng nhập");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setPrefHeight(48);
        btnLogin.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        btnLogin.setStyle(bStyle());
        btnLogin.setOnMouseEntered(e -> btnLogin.setStyle(bHover()));
        btnLogin.setOnMouseExited(e -> btnLogin.setStyle(bStyle()));
        VBox.setMargin(btnLogin, new Insets(0, 0, 16, 0));

        Label ask = new Label("Chưa có tài khoản?");
        ask.setFont(Font.font("Segoe UI", 13)); ask.setTextFill(Color.web(MUTED));
        Label reg = new Label("Đăng ký ngay");
        reg.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        reg.setTextFill(Color.web("#a78bfa")); reg.setCursor(Cursor.HAND);
        HBox regRow = new HBox(5, ask, reg); regRow.setAlignment(Pos.CENTER);

        Label lblErr = new Label();
        lblErr.setFont(Font.font("Segoe UI", 12));
        lblErr.setTextFill(Color.web("#ef4444"));
        lblErr.setWrapText(true); lblErr.setMaxWidth(340);
        VBox.setMargin(lblErr, new Insets(8, 0, 0, 0));

        card.getChildren().addAll(title, sub, lUser, userBox, lPass, passBox,
                remRow, btnLogin, regRow, lblErr);
        right.getChildren().add(card);
        root.getChildren().addAll(left, right);

        Scene scene = new Scene(root, 900, 580);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        // ── Logic server ──────────────────────────────────────────
        btnLogin.setOnAction(e -> {
            String username = txtUser.getText().trim();
            String password = txtPass.getText().trim();
            if (username.isEmpty() || password.isEmpty()) {
                lblErr.setText("Vui lòng nhập đầy đủ thông tin!"); return;
            }
            btnLogin.setText("Đang xử lý..."); btnLogin.setDisable(true); lblErr.setText("");
            new Thread(() -> {
                try {
                    AuctionClient client = new AuctionClient();
                    client.connect("localhost", 9999);
                    client.startRealtimeListener(null);
                    ResponseMessage resp = client.login(username, password);
                    Platform.runLater(() -> {
                        if (resp != null && resp.getCode() == ResponseCode.SUCCESS) {
                            Object data = resp.getData();
                            User user = null;
                            if (data instanceof User) user = (User) data;
                            else if (data instanceof Map) user = gson.fromJson(gson.toJson(data), User.class);
                            new Thread(() -> { try { client.disconnect(); } catch (Exception ignored) {} }).start();
                            if (user == null) { lblErr.setText("Lỗi dữ liệu user"); btnLogin.setText("Đăng nhập"); btnLogin.setDisable(false); return; }
                            new MainApp().showWindow(user.getUsername(), user.getRole());
                            stage.close();
                        } else {
                            new Thread(() -> { try { client.disconnect(); } catch (Exception ignored) {} }).start();
                            lblErr.setText("✕ " + (resp != null ? resp.getMessage() : "Đăng nhập thất bại"));
                            btnLogin.setText("Đăng nhập"); btnLogin.setDisable(false);
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> { lblErr.setText("✕ Lỗi kết nối: " + ex.getMessage()); btnLogin.setText("Đăng nhập"); btnLogin.setDisable(false); });
                }
            }).start();
        });

        reg.setOnMouseClicked(e -> { try { new RegisterApp().start(new Stage()); stage.close(); } catch (Exception ex) { ex.printStackTrace(); } });
    }

    // ── Shared helpers ────────────────────────────────────────────
    static HBox buildLogo() {
        HBox logo = new HBox(10);
        logo.setAlignment(Pos.CENTER_LEFT);
        ImageView gv = loadImage("/images/gavel.png", 36, 36);
        Label icon = gv != null ? new Label("", gv) : new Label("⚖") {{ setFont(Font.font(22)); }};
        VBox textBox = new VBox(1);
        Label l1 = new Label("HỆ THỐNG"); l1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11)); l1.setTextFill(Color.web("#f59e0b"));
        Label l2 = new Label("ĐẤU GIÁ TRỰC TUYẾN"); l2.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10)); l2.setTextFill(Color.web("#c4cfe8"));
        textBox.getChildren().addAll(l1, l2);
        logo.getChildren().addAll(icon, textBox);
        return logo;
    }

    static ImageView loadImage(String path, double w, double h) {
        try {
            var stream = LoginApp.class.getResourceAsStream(path);
            if (stream == null) return null;
            ImageView iv = new ImageView(new Image(stream));
            iv.setFitWidth(w); iv.setFitHeight(h); iv.setPreserveRatio(true);
            return iv;
        } catch (Exception e) { return null; }
    }

    Label fl(String s) { Label l = new Label(s); l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12)); l.setTextFill(Color.web(LABEL)); return l; }
    Label ico(String s) { Label l = new Label(s); l.setFont(Font.font(15)); return l; }
    Button eyeBtn() { Button b = new Button("👁"); b.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;-fx-cursor:hand;-fx-font-size:15;"); return b; }

    HBox inputBox(String icon, TextField tf, Button eye) {
        tf.setMaxWidth(Double.MAX_VALUE); tf.setStyle(fStyle()); tf.setPrefHeight(50);
        HBox b = eye == null ? new HBox(8, ico(icon), tf) : new HBox(8, ico(icon), tf, eye);
        b.setAlignment(Pos.CENTER_LEFT); b.setPadding(new Insets(0, 12, 0, 14));
        b.setPrefHeight(50); b.setStyle(wStyle()); HBox.setHgrow(tf, Priority.ALWAYS);
        return b;
    }

    String fStyle() { return "-fx-background-color:transparent;-fx-border-color:transparent;-fx-text-fill:" + TEXT + ";-fx-prompt-text-fill:#4a5a7a;-fx-font-size:13;-fx-padding:0 8;"; }
    String wStyle() { return "-fx-background-color:" + BG_IN + ";-fx-border-color:" + BORDER + ";-fx-border-radius:12;-fx-background-radius:12;"; }
    String bStyle() { return "-fx-background-color:linear-gradient(to right,#2563eb,#1d4ed8);-fx-text-fill:white;-fx-background-radius:10;-fx-cursor:hand;"; }
    String bHover() { return "-fx-background-color:linear-gradient(to right,#1e40af,#4338ca);-fx-text-fill:white;-fx-background-radius:10;-fx-cursor:hand;"; }

    public static void main(String[] args) { launch(args); }
}
