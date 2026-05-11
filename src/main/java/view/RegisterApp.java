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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import utils.DialogUtils;

public class RegisterApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hệ thống Đấu giá - Đăng ký");

        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #E8ECEF;");

        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setMaxWidth(380);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: white; -fx-background-radius: 30; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 20, 0, 0, 10);");

        Label lblIcon = new Label("⚖");
        lblIcon.setFont(Font.font("System", 40));
        lblIcon.setTextFill(Color.web("#24416B"));

        Label lblTitle = new Label("TẠO TÀI KHOẢN");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 22));
        lblTitle.setTextFill(Color.web("#24416B"));

        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Tên đăng nhập");
        txtUsername.setPrefHeight(45);
        txtUsername.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #D1D5DB; -fx-border-width: 1.5;");
        txtUsername.setPadding(new Insets(0, 20, 0, 20));

        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Mật khẩu");
        txtPassword.setPrefHeight(45);
        txtPassword.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #D1D5DB; -fx-border-width: 1.5;");
        txtPassword.setPadding(new Insets(0, 20, 0, 20));

        PasswordField txtConfirm = new PasswordField();
        txtConfirm.setPromptText("Nhập lại mật khẩu");
        txtConfirm.setPrefHeight(45);
        txtConfirm.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #D1D5DB; -fx-border-width: 1.5;");
        txtConfirm.setPadding(new Insets(0, 20, 0, 20));

        TextField txtEmail = new TextField();
        txtEmail.setPromptText("Email (tùy chọn)");
        txtEmail.setPrefHeight(45);
        txtEmail.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #D1D5DB; -fx-border-width: 1.5;");
        txtEmail.setPadding(new Insets(0, 20, 0, 20));

        ComboBox<String> cbRole = new ComboBox<>();
        cbRole.getItems().addAll("BIDDER", "SELLER");
        cbRole.setValue("BIDDER");
        cbRole.setPrefHeight(45);
        cbRole.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #D1D5DB; -fx-border-width: 1.5;");

        Button btnRegister = new Button("ĐĂNG KÝ");
        btnRegister.setMaxWidth(Double.MAX_VALUE);
        btnRegister.setPrefHeight(45);
        btnRegister.setStyle("-fx-background-color: #24416B; -fx-background-radius: 25; -fx-text-fill: white; -fx-font-weight: bold;");
        btnRegister.setCursor(Cursor.HAND);

        Label lblError = new Label();
        lblError.setTextFill(Color.RED);

        HBox loginBox = new HBox(5);
        loginBox.setAlignment(Pos.CENTER);
        Label lblAsk = new Label("ĐÃ CÓ TÀI KHOẢN?");
        Label lblLogin = new Label("ĐĂNG NHẬP");
        lblLogin.setTextFill(Color.web("#24416B"));
        lblLogin.setFont(Font.font("System", FontWeight.BOLD, 12));
        lblLogin.setCursor(Cursor.HAND);
        loginBox.getChildren().addAll(lblAsk, lblLogin);

        layout.getChildren().addAll(lblIcon, lblTitle, txtUsername, txtPassword, txtConfirm, txtEmail, cbRole, btnRegister, loginBox, lblError);
        root.getChildren().add(layout);

        Scene scene = new Scene(root, 600, 650);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        btnRegister.setOnAction(e -> {
            String username = txtUsername.getText().trim();
            String password = txtPassword.getText().trim();
            String confirm = txtConfirm.getText().trim();
            String email = txtEmail.getText().trim();
            String role = cbRole.getValue();

            if (username.isEmpty() || password.isEmpty()) {
                lblError.setText("Vui lòng nhập tên và mật khẩu");
                return;
            }
            if (!password.equals(confirm)) {
                lblError.setText("Mật khẩu không khớp");
                return;
            }
            if (password.length() < 6) {
                lblError.setText("Mật khẩu tối thiểu 6 ký tự");
                return;
            }

            btnRegister.setText("ĐANG XỬ LÝ...");
            btnRegister.setDisable(true);
            lblError.setText("");

            new Thread(() -> {
                try {
                    AuctionClient client = new AuctionClient();
                    client.connect("localhost", 9999);
                    client.startRealtimeListener(null);
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setPassword(password);
                    newUser.setEmail(email.isEmpty() ? null : email);
                    newUser.setRole(role);
                    ResponseMessage resp = client.register(newUser);
                    client.disconnect();

                    Platform.runLater(() -> {
                        if (resp.getCode() == ResponseCode.SUCCESS) {
                            DialogUtils.showInfo("Thành công", "Đăng ký thành công!");
                            new LoginApp().start(new Stage());
                            primaryStage.close();
                        } else {
                            lblError.setText("✕ " + resp.getMessage());
                            btnRegister.setText("ĐĂNG KÝ");
                            btnRegister.setDisable(false);
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        lblError.setText("✕ Lỗi kết nối: " + ex.getMessage());
                        btnRegister.setText("ĐĂNG KÝ");
                        btnRegister.setDisable(false);
                    });
                }
            }).start();
        });

        lblLogin.setOnMouseClicked(e -> {
            new LoginApp().start(new Stage());
            primaryStage.close();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}