package server;

import common.*;
import java.sql.*;
import java.time.LocalDateTime;

public class DataInitializer {

    public static void initData(DatabaseManager dbManager) {
        System.out.println("Khoi tao du lieu co dinh...");

        try {
            if (dbManager.getTotalUsers() > 0) {
                System.out.println("Du lieu da ton tai, bo qua");
                return;
            }

            // Tao users
            System.out.println("\nDang tao users...");

            // Admin
            createUser(dbManager, "admin", "admin123", "admin@auction.com", "ADMIN", "Quản Trị Viên", "0981000001");

            // 7 Bidder
            createUser(dbManager, "bidder1", "123456", "bidder1@email.com", "BIDDER", "Nguyễn Văn An", "0981000002");
            createUser(dbManager, "bidder2", "123456", "bidder2@email.com", "BIDDER", "Trần Thị Bình", "0981000003");
            createUser(dbManager, "bidder3", "123456", "bidder3@email.com", "BIDDER", "Lê Văn Cường", "0981000004");
            createUser(dbManager, "bidder4", "123456", "bidder4@email.com", "BIDDER", "Phạm Thị Dung", "0981000005");
            createUser(dbManager, "bidder5", "123456", "bidder5@email.com", "BIDDER", "Hoàng Văn Em", "0981000006");
            createUser(dbManager, "bidder6", "123456", "bidder6@email.com", "BIDDER", "Đặng Thị Phương", "0981000007");
            createUser(dbManager, "bidder7", "123456", "bidder7@email.com", "BIDDER", "Vũ Văn Giang", "0981000008");

            // Sellers
            createUser(dbManager, "seller1", "123456", "seller1@email.com", "SELLER", "Nguyễn Thị Hà (Seller)", "0981000009");
            createUser(dbManager, "seller2", "123456", "seller2@email.com", "SELLER", "Trần Văn Hùng (Seller)", "0981000010");

            // Tao items
            System.out.println("\nDang tao items...");

            createItem(dbManager, "iPhone 15 Pro Max 1TB", "Hang chinh hang VN/A, bao hanh 12 thang", 25000000, 7, "seller1");
            createItem(dbManager, "iPhone 15 Pro 512GB", "Mau Titan, fullbox, phu kien day du", 22000000, 5, "seller1");
            createItem(dbManager, "Samsung Galaxy S24 Ultra", "S Pen, camera 200MP, hang chinh hang", 20000000, 10, "seller1");
            createItem(dbManager, "Xiaomi 14 Ultra", "Camera Leica, pin 5000mAh", 15000000, 6, "seller2");
            createItem(dbManager, "Google Pixel 8 Pro", "Android thuan, AI features", 18000000, 8, "seller2");
            createItem(dbManager, "MacBook Pro M3 Max", "36GB RAM, 1TB SSD, chip M3 Max", 45000000, 12, "seller1");
            createItem(dbManager, "Dell XPS 15", "Intel Core i9, RTX 4060, 32GB RAM", 35000000, 9, "seller2");
            createItem(dbManager, "LG Gram 17", "Sieue nhe 1.35kg, pin 20 gio", 28000000, 7, "seller1");
            createItem(dbManager, "Asus ROG Zephyrus G14", "Gaming, AMD Ryzen 9, RTX 4080", 32000000, 11, "seller2");
            createItem(dbManager, "Sony WH-1000XM5", "Chong on tot nhat, pin 30 gio", 5000000, 4, "seller1");
            createItem(dbManager, "AirPods Pro 2", "Chip H2, chong on thich ung", 4500000, 3, "seller1");
            createItem(dbManager, "Bose QC Ultra", "Thoai mai deo ca ngay", 6000000, 5, "seller2");
            createItem(dbManager, "Apple Watch Ultra 2", "Chong nuoc 100m, GPS", 15000000, 8, "seller1");
            createItem(dbManager, "Samsung Galaxy Watch 6 Classic", "Xoay bezel, the thao", 7000000, 6, "seller2");
            createItem(dbManager, "Magic Keyboard", "Ban phim Apple chinh hang", 3000000, 10, "seller1");
            createItem(dbManager, "Logitech MX Master 3S", "Chuot cao cap, silent click", 2000000, 15, "seller2");
            createItem(dbManager, "iPad Pro 12.9 inch M2", "Wi-Fi + Cellular, 512GB", 28000000, 20, "seller1");
            createItem(dbManager, "Samsung Tab S9 Ultra", "Man hinh 14.6 inch, S Pen", 22000000, 18, "seller2");

            System.out.println("\nKHOI TAO HOAN TAT!");
            System.out.println("   1 Admin + 7 Bidder + 2 Seller = 10 users");
            System.out.println("   18 items da dang");
            System.out.println("Server san sang nhan dau gia!\n");

        } catch (SQLException e) {
            System.err.println("Loi khoi tao: " + e.getMessage());
        } catch (DataValidationException e) {
            System.err.println("Loi validation du lieu: " + e.getMessage());
        }
    }

    private static void createUser(DatabaseManager db, String username,
                                   String password, String email, String role,
                                   String fullName, String phone)
            throws SQLException {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setRole(role);
        user.setFullName(fullName);
        user.setPhone(phone);
        db.register(user);
        System.out.println("   User: " + username + " (" + role + ") - " + fullName);
    }

    private static void createItem(DatabaseManager db, String name,
                                   String desc, double price, int days,
                                   String sellerName)
            throws SQLException, DataValidationException {
        Item item = new Item();
        item.setName(name);
        item.setDescription(desc);
        item.setStartingPrice(price);
        item.setCurrentPrice(price);
        item.setStartTime(LocalDateTime.now());
        item.setEndTime(LocalDateTime.now().plusDays(days));
        item.setStatus("ACTIVE");
        item.setSellerName(sellerName);
        item.setSellerId(sellerName);
        db.createItem(item);
        System.out.println("   Item: " + name + " - " + String.format("%,.0f", price) + " VND (" + days + " ngay)");
    }
}