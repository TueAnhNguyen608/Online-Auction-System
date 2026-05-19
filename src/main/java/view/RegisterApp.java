package view;

import client.AuctionClient;
import common.*;
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
import utils.DialogUtils;

public class RegisterApp extends Application {

    private static final String BG     = "#0a0e27";
    private static final String BG_IN  = "#1a1f45";
    private static final String BORDER = "#2d3270";
    private static final String TEXT   = "#f0f4ff";
    private static final String MUTED  = "#8899bb";
    private static final String LABEL  = "#c4cfe8";

    @Override
    public void start(Stage stage) {
        stage.setTitle("Hệ thống Đấu giá - Đăng ký");

        HBox root = new HBox();
        root.setPrefSize(980, 640);
        root.setStyle("-fx-background-color:" + BG + ";");

        // ── TRÁI ─────────────────────────────────────────────────
        VBox left = new VBox(0);
        left.setPrefWidth(380);
        left.setStyle("-fx-background-color:linear-gradient(to bottom,#0c1030,#16103a);"
                + "-fx-border-color:" + BORDER + ";-fx-border-width:0 1 0 0;");

        HBox logo = LoginApp.buildLogo();
        logo.setPadding(new Insets(22, 20, 18, 22));
        logo.setStyle("-fx-border-color:transparent transparent " + BORDER + " transparent;-fx-border-width:0 0 1 0;");

        VBox imgArea = new VBox(18);
        imgArea.setAlignment(Pos.CENTER);
        imgArea.setPadding(new Insets(30, 20, 20, 20));
        VBox.setVgrow(imgArea, Priority.ALWAYS);

        ImageView shieldImg = LoginApp.loadImage("/images/shield.png", 220, 220);
        if (shieldImg != null) {
            DropShadow gs = new DropShadow(30, Color.web("#22c55e", 0.6));
            shieldImg.setEffect(gs);
        }

        Label t1 = new Label("Tham gia ngay!");
        t1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        t1.setTextFill(Color.web(TEXT));

        Label t2 = new Label("Tạo tài khoản để bắt đầu\nhành trình đấu giá của bạn.");
        t2.setFont(Font.font("Segoe UI", 13));
        t2.setTextFill(Color.web(MUTED));
        t2.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        t2.setLineSpacing(4);

        if (shieldImg != null) imgArea.getChildren().addAll(shieldImg, t1, t2);
        else imgArea.getChildren().addAll(new Label("🛡️") {{ setFont(Font.font(80)); }}, t1, t2);

        left.getChildren().addAll(logo, imgArea);

        // ── PHẢI – form ───────────────────────────────────────────
        VBox right = new VBox();
        right.setAlignment(Pos.CENTER);
        right.setPadding(new Insets(0, 36, 0, 24));
        HBox.setHgrow(right, Priority.ALWAYS);

        VBox card = new VBox(0);
        card.setPadding(new Insets(32, 36, 32, 36));
        card.setStyle("-fx-background-color:#11153a;-fx-background-radius:16;"
                + "-fx-border-color:" + BORDER + ";-fx-border-radius:16;-fx-border-width:1;"
                + "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.2),20,0,0,4);");

        Label title = new Label("Đăng ký tài khoản");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.web(TEXT));

        Label sub = new Label("Điền thông tin để tạo tài khoản mới");
        sub.setFont(Font.font("Segoe UI", 13));
        sub.setTextFill(Color.web(MUTED));
        VBox.setMargin(sub, new Insets(4, 0, 20, 0));

        // Grid 2 cột
        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(14);
        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(50);
        grid.getColumnConstraints().addAll(cc, new ColumnConstraints(){{ setPercentWidth(50); }});

        TextField txtName = inp("Nhập họ và tên");
        TextField txtUser = inp("Nhập tên đăng nhập");
        TextField txtEmail = inp("Nhập email");
        TextField txtPhone = inp("Nhập số điện thoại");

        PasswordField txtPass = pass("Nhập mật khẩu");
        PasswordField txtConf = pass("Nhập lại mật khẩu");

        // Eye buttons
        Button eye1 = eyeBtn(); Button eye2 = eyeBtn();
        TextField pVis1 = visField("Nhập mật khẩu");
        TextField pVis2 = visField("Nhập lại mật khẩu");
        txtPass.textProperty().bindBidirectional(pVis1.textProperty());
        txtConf.textProperty().bindBidirectional(pVis2.textProperty());

        StackPane ps1 = new StackPane(txtPass, pVis1);
        StackPane ps2 = new StackPane(txtConf, pVis2);
        HBox.setHgrow(ps1, Priority.ALWAYS); HBox.setHgrow(ps2, Priority.ALWAYS);

        HBox passBox = passWrap("🔒", ps1, eye1);
        HBox confBox = passWrap("🔒", ps2, eye2);

        final boolean[] s1 = {false}, s2 = {false};
        eye1.setOnAction(e -> { s1[0]=!s1[0]; txtPass.setVisible(!s1[0]); txtPass.setManaged(!s1[0]); pVis1.setVisible(s1[0]); pVis1.setManaged(s1[0]); eye1.setText(s1[0]?"🙈":"👁"); });
        eye2.setOnAction(e -> { s2[0]=!s2[0]; txtConf.setVisible(!s2[0]); txtConf.setManaged(!s2[0]); pVis2.setVisible(s2[0]); pVis2.setManaged(s2[0]); eye2.setText(s2[0]?"🙈":"👁"); });

        grid.add(fieldBox("Họ và tên",        "👤", txtName),  0, 0);
        grid.add(fieldBox("Tên đăng nhập",    "🪪", txtUser),  1, 0);
        grid.add(fieldBox("Email",            "✉️", txtEmail), 0, 1);
        grid.add(fieldBox("Số điện thoại",    "📞", txtPhone), 1, 1);
        grid.add(labeled("Mật khẩu",    passBox), 0, 2);
        grid.add(labeled("Xác nhận mật khẩu", confBox), 1, 2);
        VBox.setMargin(grid, new Insets(0, 0, 14, 0));

        // Điều khoản
        CheckBox chkTerms = new CheckBox();
        Label tTxt = new Label("Tôi đồng ý với ");
        tTxt.setFont(Font.font("Segoe UI", 12)); tTxt.setTextFill(Color.web(MUTED));
        Hyperlink lnkTerms = link("Điều khoản sử dụng");
        Label tAnd = new Label(" và "); tAnd.setFont(Font.font("Segoe UI", 12)); tAnd.setTextFill(Color.web(MUTED));
        Hyperlink lnkPriv = link("Chính sách bảo mật");
        lnkTerms.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION,"Điều khoản sử dụng hệ thống đấu giá."){{ setHeaderText(null); showAndWait(); }});
        lnkPriv.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION,"Chính sách bảo mật hệ thống đấu giá."){{ setHeaderText(null); showAndWait(); }});
        HBox termsRow = new HBox(5, chkTerms, tTxt, lnkTerms, tAnd, lnkPriv);
        termsRow.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(termsRow, new Insets(0, 0, 16, 0));

        // Nút đăng ký – màu xanh lá giống màn hình mẫu
        Button btnReg = new Button("Đăng ký");
        btnReg.setMaxWidth(Double.MAX_VALUE); btnReg.setPrefHeight(48);
        btnReg.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        btnReg.setStyle("-fx-background-color:linear-gradient(to right,#16a34a,#15803d);-fx-text-fill:white;-fx-background-radius:10;-fx-cursor:hand;");
        btnReg.setOnMouseEntered(e -> btnReg.setStyle("-fx-background-color:linear-gradient(to right,#15803d,#166534);-fx-text-fill:white;-fx-background-radius:10;-fx-cursor:hand;"));
        btnReg.setOnMouseExited(e -> btnReg.setStyle("-fx-background-color:linear-gradient(to right,#16a34a,#15803d);-fx-text-fill:white;-fx-background-radius:10;-fx-cursor:hand;"));
        VBox.setMargin(btnReg, new Insets(0, 0, 12, 0));

        Label ask = new Label("Đã có tài khoản?"); ask.setFont(Font.font("Segoe UI", 13)); ask.setTextFill(Color.web(MUTED));
        Label lnkLogin = new Label("Đăng nhập ngay"); lnkLogin.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lnkLogin.setTextFill(Color.web("#a78bfa")); lnkLogin.setCursor(Cursor.HAND);
        HBox loginRow = new HBox(5, ask, lnkLogin); loginRow.setAlignment(Pos.CENTER);

        Label lblErr = new Label(); lblErr.setFont(Font.font("Segoe UI", 12)); lblErr.setTextFill(Color.web("#ef4444")); lblErr.setWrapText(true);
        Label lblOk  = new Label(); lblOk.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12)); lblOk.setTextFill(Color.web("#10b981"));
        VBox.setMargin(lblErr, new Insets(6, 0, 0, 0));

        card.getChildren().addAll(title, sub, grid, termsRow, btnReg, loginRow, lblErr, lblOk);
        right.getChildren().add(card);
        root.getChildren().addAll(left, right);

        Scene scene = new Scene(root, 980, 640);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        // ── Logic ─────────────────────────────────────────────────
        btnReg.setOnAction(e -> {
            String username = txtUser.getText().trim();
            String password = txtPass.getText().trim();
            String confirm  = txtConf.getText().trim();
            String email    = txtEmail.getText().trim();
            if (username.isEmpty() || password.isEmpty()) { lblErr.setText("Vui lòng nhập tên và mật khẩu!"); return; }
            if (!password.equals(confirm))                { lblErr.setText("Mật khẩu không khớp!"); return; }
            if (password.length() < 6)                    { lblErr.setText("Mật khẩu tối thiểu 6 ký tự!"); return; }
            if (!chkTerms.isSelected())                   { lblErr.setText("Vui lòng đồng ý điều khoản!"); return; }
            btnReg.setText("Đang xử lý..."); btnReg.setDisable(true); lblErr.setText("");
            new Thread(() -> {
                try {
                    AuctionClient client = new AuctionClient();
                    client.connect("localhost", 9999);
                    // ĐÃ SỬA: comment out dòng này vì method chưa có trong AuctionClient
                    // client.startRealtimeListener(null);
                    User u = new User(); u.setUsername(username); u.setPassword(password);
                    u.setEmail(email.isEmpty() ? null : email); u.setRole("BIDDER");
                    // Huyền đã thêm
                    u.setFullName(txtName.getText().trim().isEmpty() ? null : txtName.getText().trim());
                    u.setPhone(txtPhone.getText().trim().isEmpty() ? null : txtPhone.getText().trim());
                    ResponseMessage resp = client.register(u);
                    new Thread(() -> { try { client.disconnect(); } catch (Exception ignored) {} }).start();
                    Platform.runLater(() -> {
                        if (resp.getCode() == ResponseCode.SUCCESS) {
                            lblOk.setText("✓ Đăng ký thành công! Đang chuyển hướng...");
                            new Thread(() -> {
                                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                                Platform.runLater(() -> { new LoginApp().start(new Stage()); stage.close(); });
                            }).start();
                        } else { lblErr.setText("✕ " + resp.getMessage()); btnReg.setText("Đăng ký"); btnReg.setDisable(false); }
                    });
                } catch (Exception ex) { Platform.runLater(() -> { lblErr.setText("✕ Lỗi kết nối: " + ex.getMessage()); btnReg.setText("Đăng ký"); btnReg.setDisable(false); }); }
            }).start();
        });
        lnkLogin.setOnMouseClicked(e -> { new LoginApp().start(new Stage()); stage.close(); });
    }

    private VBox fieldBox(String label, String icon, TextField tf) {
        Label l = new Label(label); l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11)); l.setTextFill(Color.web(LABEL));
        tf.setMaxWidth(Double.MAX_VALUE); tf.setStyle(fStyle()); tf.setPrefHeight(42);
        Label ico = new Label(icon); ico.setFont(Font.font(13));
        HBox box = new HBox(6, ico, tf); box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 10, 0, 12)); box.setStyle(wStyle()); HBox.setHgrow(tf, Priority.ALWAYS);
        VBox v = new VBox(5, l, box); v.setMaxWidth(Double.MAX_VALUE); return v;
    }

    private VBox labeled(String label, HBox field) {
        Label l = new Label(label); l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11)); l.setTextFill(Color.web(LABEL));
        VBox v = new VBox(5, l, field); v.setMaxWidth(Double.MAX_VALUE); return v;
    }

    private HBox passWrap(String icon, StackPane sp, Button eye) {
        Label ico = new Label(icon); ico.setFont(Font.font(13));
        HBox b = new HBox(6, ico, sp, eye); b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(0, 10, 0, 12)); b.setPrefHeight(42); b.setStyle(wStyle());
        HBox.setHgrow(sp, Priority.ALWAYS); return b;
    }

    private TextField inp(String p) { TextField tf = new TextField(); tf.setPromptText(p); return tf; }
    private PasswordField pass(String p) { PasswordField pf = new PasswordField(); pf.setPromptText(p); pf.setMaxWidth(Double.MAX_VALUE); pf.setStyle(fStyle()); return pf; }
    private TextField visField(String p) { TextField tf = new TextField(); tf.setPromptText(p); tf.setMaxWidth(Double.MAX_VALUE); tf.setStyle(fStyle()); tf.setVisible(false); tf.setManaged(false); return tf; }
    private Button eyeBtn() { Button b = new Button("👁"); b.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;-fx-cursor:hand;-fx-font-size:13;"); return b; }
    private Hyperlink link(String s) { Hyperlink h = new Hyperlink(s); h.setTextFill(Color.web("#a78bfa")); h.setBorder(Border.EMPTY); h.setFont(Font.font("Segoe UI", 12)); return h; }
    private String fStyle() { return "-fx-background-color:transparent;-fx-border-color:transparent;-fx-text-fill:" + TEXT + ";-fx-prompt-text-fill:#4a5a7a;-fx-font-size:12;-fx-padding:0 8;"; }
    private String wStyle() { return "-fx-background-color:" + BG_IN + ";-fx-border-color:" + BORDER + ";-fx-border-radius:10;-fx-background-radius:10;"; }

    public static void main(String[] args) { launch(args); }
}