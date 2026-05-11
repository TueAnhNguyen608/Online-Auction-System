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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.Map;

public class LoginApp extends Application {

    private Gson gson;

    @Override
    public void start(Stage primaryStage) {
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();

        primaryStage.setTitle("Hệ thống Đấu giá - Đăng nhập");

        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #E8ECEF;");

        VBox loginBox = new VBox(20);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setMaxWidth(350);
        loginBox.setPadding(new Insets(30));
        loginBox.setStyle("-fx-background-color: white; -fx-background-radius: 30; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 20, 0, 0, 10);");

        Label lblTitle = new Label("ĐĂNG NHẬP");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 22));
        lblTitle.setTextFill(Color.web("#24416B"));

        TextField txtUser = new TextField();
        txtUser.setPromptText("Tên đăng nhập");
        txtUser.setPrefHeight(45);
        txtUser.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #D1D5DB; -fx-border-width: 1.5;");
        txtUser.setPadding(new Insets(0, 20, 0, 20));

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Mật khẩu");
        txtPass.setPrefHeight(45);
        txtPass.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #D1D5DB; -fx-border-width: 1.5;");
        txtPass.setPadding(new Insets(0, 20, 0, 20));

        Label lblError = new Label();
        lblError.setTextFill(Color.RED);

        Button btnLogin = new Button("ĐĂNG NHẬP");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setPrefHeight(45);
        btnLogin.setStyle("-fx-background-color: #24416B; -fx-background-radius: 25; -fx-text-fill: white; -fx-font-weight: bold;");
        btnLogin.setCursor(Cursor.HAND);

        HBox registerBox = new HBox(5);
        registerBox.setAlignment(Pos.CENTER);
        Label lblAsk = new Label("Chưa có tài khoản?");
        Label lblRegister = new Label("Đăng ký ngay");
        lblRegister.setTextFill(Color.web("#24416B"));
        lblRegister.setFont(Font.font("System", FontWeight.BOLD, 12));
        lblRegister.setCursor(Cursor.HAND);
        registerBox.getChildren().addAll(lblAsk, lblRegister);

        loginBox.getChildren().addAll(lblTitle, txtUser, txtPass, btnLogin, registerBox, lblError);
        root.getChildren().add(loginBox);

        Scene scene = new Scene(root, 600, 550);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        btnLogin.setOnAction(e -> {
            String username = txtUser.getText().trim();
            String password = txtPass.getText().trim();
            if (username.isEmpty() || password.isEmpty()) {
                lblError.setText("Vui lòng nhập đủ thông tin!");
                return;
            }
            btnLogin.setText("ĐANG XỬ LÝ...");
            btnLogin.setDisable(true);
            lblError.setText("");

            new Thread(() -> {
                try {
                    AuctionClient client = new AuctionClient();
                    client.connect("localhost", 9999);
                    client.startRealtimeListener(null);
                    ResponseMessage resp = client.login(username, password);
                    client.disconnect();

                    Platform.runLater(() -> {
                        if (resp.getCode() == ResponseCode.SUCCESS) {
                            Object data = resp.getData();
                            User user = null;
                            if (data instanceof User) {
                                user = (User) data;
                            } else if (data instanceof Map) {
                                user = gson.fromJson(gson.toJson(data), User.class);
                            }
                            if (user == null) {
                                lblError.setText("Lỗi dữ liệu user");
                                btnLogin.setText("ĐĂNG NHẬP");
                                btnLogin.setDisable(false);
                                return;
                            }
                            MainApp mainApp = new MainApp();
                            mainApp.showWindow(user.getUsername(), user.getRole());
                            primaryStage.close();
                        } else {
                            lblError.setText("✕ " + resp.getMessage());
                            btnLogin.setText("ĐĂNG NHẬP");
                            btnLogin.setDisable(false);
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        lblError.setText("✕ Lỗi kết nối: " + ex.getMessage());
                        btnLogin.setText("ĐĂNG NHẬP");
                        btnLogin.setDisable(false);
                    });
                }
            }).start();
        });

        lblRegister.setOnMouseClicked(e -> {
            try {
                new RegisterApp().start(new Stage());
                primaryStage.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}