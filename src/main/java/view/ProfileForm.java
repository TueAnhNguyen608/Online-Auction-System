package view;

import client.AuctionClient;
import common.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import utils.DialogUtils;

import java.util.HashMap;
import java.util.Map;

public class ProfileForm {

    private AuctionClient client;
    private User currentUser;
    private Stage stage;

    public void show(AuctionClient client, User user) {
        this.client = client;
        this.currentUser = user;
        this.stage = new Stage();
        stage.setTitle("👤 Quản lý tài khoản - " + user.getUsername());

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 30; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 20, 0, 0, 10);"
        );

        Tab infoTab = new Tab("📋 Thông tin cá nhân");
        infoTab.setContent(createInfoPanel());

        Tab passwordTab = new Tab("🔐 Đổi mật khẩu");
        passwordTab.setContent(createPasswordPanel());

        Tab balanceTab = new Tab("💰 Số dư");
        balanceTab.setContent(createBalancePanel());

        tabPane.getTabs().addAll(infoTab, passwordTab, balanceTab);

        StackPane root = new StackPane(tabPane);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #E8ECEF;");

        Scene scene = new Scene(root, 550, 500);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    private void styleTextField(TextField tf) {
        tf.setPrefHeight(45);
        tf.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(20), Insets.EMPTY)));
        tf.setBorder(new Border(new BorderStroke(
                Color.web("#D1D5DB"), BorderStrokeStyle.SOLID, new CornerRadii(20), new BorderWidths(1.5)
        )));
        tf.setPadding(new Insets(0, 20, 0, 20));
    }

    private void styleTextFieldDisabled(TextField tf) {
        styleTextField(tf);
        tf.setStyle("-fx-background-color: #f0f0f0;");
    }

    private void styleButton(Button btn, String colorHex) {
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(45);
        btn.setTextFill(Color.WHITE);
        btn.setFont(Font.font("System", FontWeight.BOLD, 14));
        btn.setBackground(new Background(new BackgroundFill(Color.web(colorHex), new CornerRadii(25), Insets.EMPTY)));
        btn.setCursor(Cursor.HAND);
    }

    private VBox createInfoPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(30));
        panel.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label("THÔNG TIN CÁ NHÂN");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        lblTitle.setTextFill(Color.web("#24416B"));

        Label lblUsername = new Label("Tên đăng nhập:");
        lblUsername.setTextFill(Color.web("#24416B"));
        TextField txtUsername = new TextField(currentUser.getUsername());
        txtUsername.setEditable(false);
        styleTextFieldDisabled(txtUsername);

        Label lblEmail = new Label("Email:");
        lblEmail.setTextFill(Color.web("#24416B"));
        TextField txtEmail = new TextField(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        styleTextField(txtEmail);

        Button btnUpdate = new Button("💾 Cập nhật Email");
        styleButton(btnUpdate, "#24416B");

        Label lblMessage = new Label();
        lblMessage.setTextFill(Color.web("#E53935"));

        btnUpdate.setOnAction(e -> {
            String email = txtEmail.getText().trim();
            if (email.isEmpty()) {
                lblMessage.setText("Email không được để trống!");
                return;
            }

            User updated = new User();
            updated.setId(currentUser.getId());
            updated.setUsername(currentUser.getUsername());
            updated.setEmail(email);
            updated.setRole(currentUser.getRole());

            new Thread(() -> {
                try {
                    ResponseMessage resp = client.sendRequest(CommandType.UPDATE_USER_INFO, updated);
                    Platform.runLater(() -> {
                        if (resp.getCode() == ResponseCode.SUCCESS) {
                            DialogUtils.showInfo("Thành công", "Cập nhật email thành công!");
                            currentUser.setEmail(email);
                            lblMessage.setText("");
                        } else {
                            lblMessage.setText("Lỗi: " + resp.getMessage());
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> lblMessage.setText("Lỗi kết nối: " + ex.getMessage()));
                }
            }).start();
        });

        panel.getChildren().addAll(
                lblTitle, new Separator(),
                lblUsername, txtUsername,
                lblEmail, txtEmail,
                btnUpdate, lblMessage
        );
        return panel;
    }

    private VBox createPasswordPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(30));
        panel.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label("ĐỔI MẬT KHẨU");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        lblTitle.setTextFill(Color.web("#24416B"));

        PasswordField txtOld = new PasswordField();
        txtOld.setPromptText("Mật khẩu cũ");
        PasswordField txtNew = new PasswordField();
        txtNew.setPromptText("Mật khẩu mới (≥6 ký tự)");
        PasswordField txtConfirm = new PasswordField();
        txtConfirm.setPromptText("Xác nhận mật khẩu mới");

        styleTextField(txtOld);
        styleTextField(txtNew);
        styleTextField(txtConfirm);

        Button btnChange = new Button("🔄 Đổi mật khẩu");
        styleButton(btnChange, "#24416B");

        Label lblMsg = new Label();
        lblMsg.setTextFill(Color.web("#E53935"));

        btnChange.setOnAction(e -> {
            String oldPass = txtOld.getText();
            String newPass = txtNew.getText();
            String confirm = txtConfirm.getText();

            if (oldPass.isEmpty() || newPass.isEmpty()) {
                lblMsg.setText("Vui lòng nhập đầy đủ!");
                return;
            }
            if (!newPass.equals(confirm)) {
                lblMsg.setText("Mật khẩu mới không khớp!");
                return;
            }
            if (newPass.length() < 6) {
                lblMsg.setText("Mật khẩu mới phải có ít nhất 6 ký tự!");
                return;
            }

            Map<String, String> data = new HashMap<>();
            data.put("oldPassword", oldPass);
            data.put("newPassword", newPass);

            new Thread(() -> {
                try {
                    ResponseMessage resp = client.sendRequest(CommandType.CHANGE_PASSWORD, data);
                    Platform.runLater(() -> {
                        if (resp.getCode() == ResponseCode.SUCCESS) {
                            DialogUtils.showInfo("Thành công", "Đổi mật khẩu thành công!");
                            txtOld.clear();
                            txtNew.clear();
                            txtConfirm.clear();
                            lblMsg.setText("");
                        } else {
                            lblMsg.setText("Lỗi: " + resp.getMessage());
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> lblMsg.setText("Lỗi kết nối: " + ex.getMessage()));
                }
            }).start();
        });

        panel.getChildren().addAll(
                lblTitle, new Separator(),
                new Label("Mật khẩu cũ:"), txtOld,
                new Label("Mật khẩu mới:"), txtNew,
                new Label("Xác nhận:"), txtConfirm,
                btnChange, lblMsg
        );
        return panel;
    }

    private VBox createBalancePanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(30));
        panel.setAlignment(Pos.CENTER);

        Label lblTitle = new Label("💰 SỐ DƯ HIỆN TẠI");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        lblTitle.setTextFill(Color.web("#24416B"));

        Label lblBalance = new Label(String.format("%,.0f VNĐ", currentUser.getBalance()));
        lblBalance.setFont(Font.font("System", FontWeight.BOLD, 36));
        lblBalance.setTextFill(Color.web("#27ae60"));

        Button btnRefresh = new Button("🔄 Làm mới số dư");
        styleButton(btnRefresh, "#24416B");

        btnRefresh.setOnAction(e -> {
            DialogUtils.showInfo("Số dư", "Hiện tại: " + String.format("%,.0f VNĐ", currentUser.getBalance()));
        });

        panel.getChildren().addAll(lblTitle, lblBalance, btnRefresh);
        return panel;
    }
}