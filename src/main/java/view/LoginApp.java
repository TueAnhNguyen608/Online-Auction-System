package view;

import javafx.application.Application;
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

// Thêm thư viện gọi mạng của bạn vào
import services.ApiService;
import utils.DialogUtils;

public class LoginApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // 1. Giao diện nền (Màu xám/đen nhạt để làm nổi bật Form trắng)
        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setBackground(new Background(new BackgroundFill(Color.web("#E8ECEF"), CornerRadii.EMPTY, Insets.EMPTY)));

        // 2. Form đăng nhập (Trắng tinh, bo góc siêu tròn giống ảnh)
        VBox loginBox = new VBox(20);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(40, 30, 40, 30));
        loginBox.setMaxWidth(350);
        loginBox.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(30), Insets.EMPTY)));
        loginBox.setEffect(new DropShadow(20, Color.rgb(0, 0, 0, 0.1)));

        // Icon Cán cân công lý (Mô phỏng logo trong ảnh)
        Label lblIcon = new Label("⚖");
        lblIcon.setFont(Font.font("System", 45));
        lblIcon.setTextFill(Color.web("#24416B"));

        // 3. Tiêu đề (Màu Xanh Navy)
        Label lblTitle = new Label("ĐẤU GIÁ");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 26));
        lblTitle.setTextFill(Color.web("#24416B"));

        Label lblSubTitle = new Label("ĐĂNG NHẬP ĐỂ TIẾP TỤC");
        lblSubTitle.setFont(Font.font("System", 12));
        lblSubTitle.setTextFill(Color.web("#8898AA"));

        VBox titleBox = new VBox(5, lblIcon, lblTitle, lblSubTitle);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(0, 0, 15, 0));

        // Style chung cho ô nhập liệu (Bo tròn dạng viên thuốc)
        Background tfBg = new Background(new BackgroundFill(Color.WHITE, new CornerRadii(20), Insets.EMPTY));
        Border tfBorder = new Border(new BorderStroke(Color.web("#D1D5DB"), BorderStrokeStyle.SOLID, new CornerRadii(20), new BorderWidths(1.5)));

        // 4. Ô nhập liệu
        TextField txtUser = new TextField();
        txtUser.setPromptText("Email hoặc tên đăng nhập");
        txtUser.setPrefHeight(45);
        txtUser.setBackground(tfBg);
        txtUser.setBorder(tfBorder);
        txtUser.setPadding(new Insets(0, 20, 0, 20));

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Mật khẩu");
        txtPass.setPrefHeight(45);
        txtPass.setBackground(tfBg);
        txtPass.setBorder(tfBorder);
        txtPass.setPadding(new Insets(0, 20, 0, 20));

        // 5. Nút Đăng nhập (Màu Xanh Navy đậm, bo tròn viên thuốc)
        Button btnLogin = new Button("ĐĂNG NHẬP");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setPrefHeight(45);
        btnLogin.setTextFill(Color.WHITE);
        btnLogin.setFont(Font.font("System", FontWeight.BOLD, 14));
        btnLogin.setBackground(new Background(new BackgroundFill(Color.web("#24416B"), new CornerRadii(25), Insets.EMPTY)));
        btnLogin.setCursor(Cursor.HAND);

        // 6. Label Lỗi mô phỏng trong ảnh
        Label lblError = new Label("");
        lblError.setTextFill(Color.web("#E53935"));
        lblError.setFont(Font.font("System", 12));

        // =========================================================================
        // ĐỘNG CƠ API CHẠY NGẦM - ĐÃ ĐƯỢC CẤY GHÉP VÀO SỰ KIỆN NÚT BẤM
        // =========================================================================
        btnLogin.setOnAction(e -> {
            String username = txtUser.getText().trim();
            String password = txtPass.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                lblError.setText("✕ Vui lòng nhập đầy đủ thông tin");
                return;
            }

            btnLogin.setText("ĐANG XỬ LÝ...");
            btnLogin.setDisable(true);
            lblError.setText("");

            // Dùng luồng chạy ngầm để không làm đơ giao diện bóng bẩy của bạn
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    // Gọi Server qua ApiService
                    String responseData = ApiService.getInstance().login(username, password);

                    // NẾU THÀNH CÔNG -> Chuyển về luồng giao diện chính để đổi màn hình
                    Platform.runLater(() -> {
                        DialogUtils.showInfo("Thành công", "Đăng nhập thành công!");
                        try {
                            MainApp mainApp = new MainApp();
                            // Tạm thời truyền role mặc định là "Bidder" để MainApp hoạt động,
                            // sau này nhóm làm xong API xịn sẽ bóc tách Role thật từ responseData
                            mainApp.showWindow(username, "Bidder");
                            primaryStage.close();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });

                } catch (Exception ex) {
                    // NẾU THẤT BẠI (Do sai pass hoặc sập mạng) -> Báo chữ đỏ
                    Platform.runLater(() -> {
                        lblError.setText("✕ " + ex.getMessage());
                        // Trả lại nút bấm gốc để người dùng có thể thử lại
                        btnLogin.setText("ĐĂNG NHẬP");
                        btnLogin.setDisable(false);
                    });
                }
            });
        });
        // =========================================================================

        // 7. Chuyển sang Đăng Ký
        HBox registerBox = new HBox(5);
        registerBox.setAlignment(Pos.CENTER);
        Label lblAsk = new Label("CHƯA CÓ TÀI KHOẢN?");
        lblAsk.setTextFill(Color.web("#8898AA"));
        lblAsk.setFont(Font.font("System", 12));

        Label lblRegister = new Label("ĐĂNG KÝ NGAY");
        lblRegister.setTextFill(Color.web("#24416B"));
        lblRegister.setFont(Font.font("System", FontWeight.BOLD, 12));
        lblRegister.setCursor(Cursor.HAND);

        lblRegister.setOnMouseClicked(e -> {
            try {
                RegisterApp registerApp = new RegisterApp();
                registerApp.showWindow();
                primaryStage.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        registerBox.getChildren().addAll(lblAsk, lblRegister);

        loginBox.getChildren().addAll(titleBox, txtUser, txtPass, btnLogin, registerBox, lblError);
        root.getChildren().add(loginBox);

        Scene scene = new Scene(root, 600, 550);
        primaryStage.setTitle("Hệ thống Đấu giá - Đăng nhập");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}