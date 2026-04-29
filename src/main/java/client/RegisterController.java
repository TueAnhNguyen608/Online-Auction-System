package client;

import common.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleComboBox;

    private AuctionClient client;

    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll("BIDDER", "ADMIN");
        roleComboBox.setValue("BIDDER");

        client = new AuctionClient();
        try {
            client.connect("localhost", 9999);
            // Khởi động realtime listener (có thể bỏ qua nếu không cần)
            client.startRealtimeListener(msg -> {
                System.out.println("Realtime: " + msg.getMessage());
            });
        } catch (IOException e) {
            showAlert("Lỗi", "Không thể kết nối server: " + e.getMessage());
        }
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        String role = roleComboBox.getValue();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert("Lỗi", "Vui lòng điền đầy đủ thông tin");
            return;
        }

        if (!password.equals(confirm)) {
            showAlert("Lỗi", "Mật khẩu xác nhận không khớp");
            return;
        }

        if (password.length() < 6) {
            showAlert("Lỗi", "Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }

        try {
            User newUser = new User(username, password, email, role);
            ResponseMessage response = client.register(newUser);

            if (response.getCode() == ResponseCode.SUCCESS) {
                showAlert("Thành công", "Đăng ký thành công! Vui lòng đăng nhập.");
                handleBackToLogin();
            } else {
                showAlert("Đăng ký thất bại", response.getMessage());
            }
        } catch (IOException e) {
            showAlert("Lỗi", "Lỗi kết nối: " + e.getMessage());
        } catch (InterruptedException e) {
            showAlert("Lỗi", "Lỗi: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 400, 500));
            stage.setTitle("Đăng nhập");
        } catch (Exception e) {
            e.printStackTrace();
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