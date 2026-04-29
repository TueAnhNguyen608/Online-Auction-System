package client;

import common.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private AuctionClient client;

    @FXML
    public void initialize() {
        client = new AuctionClient();
        try {
            client.connect("localhost", 9999);
            // Khởi động realtime listener
            client.startRealtimeListener(msg -> {
                System.out.println("Realtime: " + msg.getMessage());
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("📡 " + msg.getMessage());
                });
            });
            statusLabel.setText("✅ Đã kết nối server");
        } catch (IOException e) {
            statusLabel.setText("❌ Không kết nối được server");
            showAlert("Lỗi kết nối", e.getMessage());
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập tên đăng nhập và mật khẩu");
            return;
        }

        try {
            ResponseMessage response = client.login(username, password);

            if (response.getCode() == ResponseCode.SUCCESS) {
                User user = (User) response.getData();
                SessionManager.getInstance().setUser(user);
                showAlert("Thành công", "Chào " + user.getUsername());

                if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                    openSellerDashboard();
                } else if ("BIDDER".equalsIgnoreCase(user.getRole())) {
                    showAlert("Thông báo", "Bạn là người đấu giá. Vui lòng sử dụng ứng dụng dành cho người mua.");
                }
            } else {
                showAlert("Đăng nhập thất bại", response.getMessage());
            }
        } catch (IOException e) {
            showAlert("Lỗi", "Lỗi kết nối: " + e.getMessage());
        } catch (InterruptedException e) {
            showAlert("Lỗi", "Lỗi: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    @FXML
    private void handleGoToRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/register.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 450, 600));
            stage.setTitle("Đăng ký tài khoản");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể mở giao diện đăng ký: " + e.getMessage());
        }
    }

    private void openSellerDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/seller_dashboard.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("Hệ thống đấu giá - ADMIN Dashboard");
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể mở giao diện: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}