package com.auction.server;

import com.auction.common.model.exception.DataValidationException;
import com.auction.common.model.item.Item;
import com.auction.common.model.item.ItemStatus;
import com.auction.common.model.user.User;
import com.auction.common.model.user.UserRole;
import com.auction.common.util.PasswordUtil;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class DataInitializer {

    public static void initData() {
        System.out.println("Khoi tao du lieu co dinh...");

        try {
            UserRepository userRepo = UserRepository.getInstance();
            ItemRepository itemRepo = ItemRepository.getInstance();

            if (userRepo.count() > 0) {
                System.out.println("Du lieu da ton tai, bo qua");
                return;
            }

            System.out.println("\nDang tao users...");

            createUser("admin", "admin123", "admin@auction.com", UserRole.ADMIN, "Quản Trị Viên", "0981000001");

            createUser("bidder1", "123456", "bidder1@email.com", UserRole.BIDDER, "Nguyễn Văn An", "0981000002");
            createUser("bidder2", "123456", "bidder2@email.com", UserRole.BIDDER, "Trần Thị Bình", "0981000003");
            createUser("bidder3", "123456", "bidder3@email.com", UserRole.BIDDER, "Lê Văn Cường", "0981000004");
            createUser("bidder4", "123456", "bidder4@email.com", UserRole.BIDDER, "Phạm Thị Dung", "0981000005");
            createUser("bidder5", "123456", "bidder5@email.com", UserRole.BIDDER, "Hoàng Văn Em", "0981000006");
            createUser("bidder6", "123456", "bidder6@email.com", UserRole.BIDDER, "Đặng Thị Phương", "0981000007");
            createUser("bidder7", "123456", "bidder7@email.com", UserRole.BIDDER, "Vũ Văn Giang", "0981000008");

            createUser("seller1", "123456", "seller1@email.com", UserRole.SELLER, "Nguyễn Thị Hà", "0981000009");
            createUser("seller2", "123456", "seller2@email.com", UserRole.SELLER, "Trần Văn Hùng", "0981000010");

            System.out.println("\nDang tao items...");

            User seller1 = userRepo.findByUsername("seller1");
            User seller2 = userRepo.findByUsername("seller2");

            String seller1Id = seller1 != null ? seller1.getId() : "seller1";
            String seller2Id = seller2 != null ? seller2.getId() : "seller2";

            createItem("iPhone 15 Pro Max 1TB", "Hang chinh hang VN/A, bao hanh 12 thang", 25000000, 7, seller1Id, "seller1");
            createItem("iPhone 15 Pro 512GB", "Mau Titan, fullbox, phu kien day du", 22000000, 5, seller1Id, "seller1");
            createItem("Samsung Galaxy S24 Ultra", "S Pen, camera 200MP, hang chinh hang", 20000000, 10, seller1Id, "seller1");
            createItem("Xiaomi 14 Ultra", "Camera Leica, pin 5000mAh", 15000000, 6, seller2Id, "seller2");
            createItem("Google Pixel 8 Pro", "Android thuan, AI features", 18000000, 8, seller2Id, "seller2");
            createItem("MacBook Pro M3 Max", "36GB RAM, 1TB SSD, chip M3 Max", 45000000, 12, seller1Id, "seller1");
            createItem("Dell XPS 15", "Intel Core i9, RTX 4060, 32GB RAM", 35000000, 9, seller2Id, "seller2");
            createItem("LG Gram 17", "Sieue nhe 1.35kg, pin 20 gio", 28000000, 7, seller1Id, "seller1");
            createItem("Asus ROG Zephyrus G14", "Gaming, AMD Ryzen 9, RTX 4080", 32000000, 11, seller2Id, "seller2");
            createItem("Sony WH-1000XM5", "Chong on tot nhat, pin 30 gio", 5000000, 4, seller1Id, "seller1");
            createItem("AirPods Pro 2", "Chip H2, chong on thich ung", 4500000, 3, seller1Id, "seller1");
            createItem("Bose QC Ultra", "Thoai mai deo ca ngay", 6000000, 5, seller2Id, "seller2");
            createItem("Apple Watch Ultra 2", "Chong nuoc 100m, GPS", 15000000, 8, seller1Id, "seller1");
            createItem("Samsung Galaxy Watch 6 Classic", "Xoay bezel, the thao", 7000000, 6, seller2Id, "seller2");
            createItem("Magic Keyboard", "Ban phim Apple chinh hang", 3000000, 10, seller1Id, "seller1");
            createItem("Logitech MX Master 3S", "Chuot cao cap, silent click", 2000000, 15, seller2Id, "seller2");
            createItem("iPad Pro 12.9 inch M2", "Wi-Fi + Cellular, 512GB", 28000000, 20, seller1Id, "seller1");
            createItem("Samsung Tab S9 Ultra", "Man hinh 14.6 inch, S Pen", 22000000, 18, seller2Id, "seller2");

            System.out.println("\n✅ KHOI TAO HOAN TAT!");
            System.out.println("   1 Admin + 7 Bidder + 2 Seller = 10 users");
            System.out.println("   18 items da dang");
            System.out.println("Server san sang nhan dau gia!\n");

        } catch (SQLException e) {
            System.err.println("Loi khoi tao database: " + e.getMessage());
            e.printStackTrace();
        } catch (DataValidationException e) {
            System.err.println("Loi validation du lieu: " + e.getMessage());
        }
    }

    private static void createUser(String username, String password, String email,
                                   UserRole role, String fullName, String phone)
            throws SQLException {
        UserRepository userRepo = UserRepository.getInstance();

        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.hash(password));
        user.setEmail(email);
        user.setRole(role);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setBalance(100000000);

        userRepo.save(user);
        System.out.println("   User: " + username + " (" + role + ") - " + fullName);
    }

    private static void createItem(String name, String desc, double price,
                                   int days, String sellerId, String sellerName)
            throws SQLException, DataValidationException {
        ItemRepository itemRepo = ItemRepository.getInstance();

        Item item = new Item();
        item.setId(UUID.randomUUID().toString());
        item.setName(name);
        item.setDescription(desc);
        item.setStartingPrice(price);
        item.setCurrentPrice(price);
        item.setStartTime(LocalDateTime.now());
        item.setEndTime(LocalDateTime.now().plusDays(days));
        item.setStatus(ItemStatus.ACTIVE);
        item.setSellerId(sellerId);
        item.setSellerName(sellerName);
        item.setBidCount(0);

        itemRepo.save(item);
        System.out.println("   Item: " + name + " - " + String.format("%,.0f", price) + " VND (" + days + " ngay)");
    }
}