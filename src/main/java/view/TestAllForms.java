package view;

import common.*;
import javafx.application.Application;
import javafx.stage.Stage;

public class TestAllForms extends Application {

    @Override
    public void start(Stage primaryStage) {
        // ===== Dữ liệu ảo =====
        User mockUser = new User();
        mockUser.setUsername("bidder1");
        mockUser.setEmail("bidder1@example.com");
        mockUser.setBalance(15_000_000);
        mockUser.setRole("BIDDER");
        mockUser.setId("mock-id-001");

        // ===== Mở từng form (bỏ comment để test) =====

        // 1. ProfileForm (đã có style đẹp)
        System.out.println("🔹 Mở ProfileForm...");
        new ProfileForm().show(null, mockUser);

        // 2. MyBidsHistoryUI (bảng trống vì không có server)
        System.out.println("🔹 Mở MyBidsHistoryUI...");
        new MyBidsHistoryUI().show(null);

        // 3. WonProductsUI (bảng trống)
        System.out.println("🔹 Mở WonProductsUI...");
        new WonProductsUI().show(null);

        // 4. PriceChartUI (sẽ vẽ biểu đồ mẫu)
        System.out.println("🔹 Mở PriceChartUI với dữ liệu mẫu...");
        new PriceChartUI().show(null, "mock-item-123", "iPhone 15 Pro Max");
    }

    public static void main(String[] args) {
        launch(args);
    }
}