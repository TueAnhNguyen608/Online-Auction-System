package view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import services.ApiService;
import utils.DialogUtils;

public class RegisterApp {

    public void showWindow() {
        Stage stage = new Stage();
        stage.setTitle("Hệ thống Đấu giá - Đăng ký");

        // Giao diện nền
        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setBackground(new Background(new BackgroundFill(Color.web("#E8ECEF"), CornerRadii.EMPTY, Insets.EMPTY)));

        // Form đăng ký
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30, 30, 30, 30));
        layout.setMaxWidth(380);
        layout.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(30), Insets.EMPTY)));
        layout.setEffect(new DropShadow(20, Color.rgb(0, 0, 0, 0.1)));

        // Tiêu đề
        Label lblIcon = new Label("⚖");
        lblIcon.setFont(Font.font("System", 40));
        lblIcon.setTextFill(Color.web("#24416B"));

        Label lblTitle = new Label("TẠO TÀI KHOẢN");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 22));
        lblTitle.setTextFill(Color.web("#24416B"));

        VBox titleBox = new VBox(5, lblIcon, lblTitle);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(0, 0, 10, 0));

        // Style chung cho ô nhập liệu
        Background tfBg = new Background(new BackgroundFill(Color.WHITE, new CornerRadii(20), Insets.EMPTY));
        Border tfBorder = new Border(new BorderStroke(Color.web("#D1D5DB"), BorderStrokeStyle.SOLID, new CornerRadii(20), new BorderWidths(1.5)));

        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Tên đăng nhập");
        txtUsername.setPrefHeight(45);
        txtUsername.setBackground(tfBg);
        txtUsername.setBorder(tfBorder);
        txtUsername.setPadding(new Insets(0, 20, 0, 20));

        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Mật khẩu");
        txtPassword.setPrefHeight(45);
        txtPassword.setBackground(tfBg);
        txtPassword.setBorder(tfBorder);
        txtPassword.setPadding(new Insets(0, 20, 0, 20));

        PasswordField txtConfirmPassword = new PasswordField();
        txtConfirmPassword.setPromptText("Nhập lại mật khẩu");
        txtConfirmPassword.setPrefHeight(45);
        txtConfirmPassword.setBackground(tfBg);
        txtConfirmPassword.setBorder(tfBorder);
        txtConfirmPassword.setPadding(new Insets(0, 20, 0, 20));

        ComboBox<String> cbRole = new ComboBox<>();
        cbRole.getItems().addAll("Người đấu giá (Bidder)", "Người bán (Seller)", "Người dùng thường (User)");
        cbRole.setValue("Người đấu giá (Bidder)");
        cbRole.setPrefHeight(45);
        cbRole.setMaxWidth(Double.MAX_VALUE);
        cbRole.setBackground(tfBg);
        cbRole.setBorder(tfBorder);

        // Nút Đăng Ký
        Button btnRegister = new Button("ĐĂNG KÝ");
        btnRegister.setBackground(new Background(new BackgroundFill(Color.web("#24416B"), new CornerRadii(25), Insets.EMPTY)));
        btnRegister.setTextFill(Color.WHITE);
        btnRegister.setFont(Font.font("System", FontWeight.BOLD, 14));
        btnRegister.setPrefHeight(45);
        btnRegister.setMaxWidth(Double.MAX_VALUE);
        btnRegister.setCursor(Cursor.HAND);

        Label lblError = new Label("");
        lblError.setTextFill(Color.web("#E53935"));
        lblError.setFont(Font.font("System", 12));

        // Nút Quay lại Đăng Nhập
        HBox loginBox = new HBox(5);
        loginBox.setAlignment(Pos.CENTER);
        Label lblAsk = new Label("ĐÃ CÓ TÀI KHOẢN?");
        lblAsk.setTextFill(Color.web("#8898AA"));
        lblAsk.setFont(Font.font("System", 12));

        Label lblLogin = new Label("ĐĂNG NHẬP");
        lblLogin.setTextFill(Color.web("#24416B"));
        lblLogin.setFont(Font.font("System", FontWeight.BOLD, 12));
        lblLogin.setCursor(Cursor.HAND);

        // =========================================================================
        // SỰ KIỆN NÚT ĐĂNG KÝ (Đã fix lỗi biến final)
        // =========================================================================
        btnRegister.setOnAction(e -> {
            String username = txtUsername.getText().trim();
            String password = txtPassword.getText().trim();
            String confirmPassword = txtConfirmPassword.getText().trim();
            String selectedRole = cbRole.getValue();

            // 1. Validate form
            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                lblError.setText("✕ Vui lòng nhập đầy đủ thông tin");
                return;
            }

            if (!password.equals(confirmPassword)) {
                lblError.setText("✕ Mật khẩu nhập lại không khớp");
                return;
            }

            // 2. Chuyển đổi Role (ĐÃ FIX THÀNH HẰNG SỐ FINAL)
            final String finalRoleCode;
            if (selectedRole.equals("Người bán (Seller)")) {
                finalRoleCode = "seller";
            } else if (selectedRole.equals("Người dùng thường (User)")) {
                finalRoleCode = "user";
            } else {
                finalRoleCode = "bidder";
            }

            // 3. UX/UI: Đổi trạng thái báo hiệu đang gọi mạng
            btnRegister.setText("ĐANG XỬ LÝ...");
            btnRegister.setDisable(true);
            lblError.setText("");

            // 4. Gọi API bất đồng bộ
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    // Truyền finalRoleCode hợp lệ vào luồng ngầm
                    String responseData = ApiService.getInstance().register(username, password, finalRoleCode);

                    // THÀNH CÔNG
                    Platform.runLater(() -> {
                        DialogUtils.showInfo("Thành công", "Đăng ký tài khoản thành công!");
                        try {
                            LoginApp loginApp = new LoginApp();
                            loginApp.start(new Stage());
                            stage.close();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });

                } catch (Exception ex) {
                    // THẤT BẠI
                    Platform.runLater(() -> {
                        lblError.setText("✕ " + ex.getMessage());
                        // Nhả nút bấm ra để thử lại
                        btnRegister.setText("ĐĂNG KÝ");
                        btnRegister.setDisable(false);
                    });
                }
            });
        });
        // =========================================================================

        lblLogin.setOnMouseClicked(e -> {
            try {
                LoginApp loginApp = new LoginApp();
                loginApp.start(new Stage());
                stage.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        loginBox.getChildren().addAll(lblAsk, lblLogin);

        layout.getChildren().addAll(titleBox, txtUsername, txtPassword, txtConfirmPassword, cbRole, btnRegister, loginBox, lblError);
        root.getChildren().add(layout);

        Scene scene = new Scene(root, 600, 600);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }
}