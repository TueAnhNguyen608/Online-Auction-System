package client;

import common.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class SellerDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<Item> productsTable;

    private AuctionClient client;

    @FXML
    public void initialize() {
        client = new AuctionClient();
        try {
            client.connect("localhost", 9999);

            User user = SessionManager.getInstance().getCurrentUser();
            welcomeLabel.setText("Xin chao " + user.getUsername() + " (ADMIN)");
            statusLabel.setText("Da ket noi server | " + new java.util.Date());
        } catch (Exception e) {
            statusLabel.setText("Loi: " + e.getMessage());
        }
    }

    @FXML
    private void handleCreateProduct() {
        showAlert("Tao san pham", "Chuc nang dang phat trien");
    }

    @FXML
    private void handleStartAuction() {
        showAlert("Bat dau dau gia", "Chuc nang dang phat trien");
    }

    @FXML
    private void handleHelp() {
        showAlert("Huong dan", "1. Dang nhap\n2. Tao san pham\n3. Bat dau dau gia");
    }

    @FXML
    private void handleAbout() {
        showAlert("Thong tin", "He thong dau gia - Nhom 3\nPhien ban 1.0");
    }

    @FXML
    private void handleRefresh() {
        statusLabel.setText("Da cap nhat luc " + new java.util.Date());
    }

    @FXML
    private void handleLogout() {
        try {
            if (client != null) {
                client.disconnect();
            }
            SessionManager.getInstance().logout();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 400, 500));
            stage.setTitle("Dang nhap");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Loi", "Khong the dang xuat: " + e.getMessage());
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