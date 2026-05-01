package test;

import common.*;

public class ManualTest {
    public static void main(String[] args) {
        System.out.println("=== KIỂM TRA NGOẠI LỆ HỆ THỐNG ĐẤU GIÁ ===\n");

        int passed = 0;
        int total = 0;

        // ===== TEST 1: InvalidBidException =====
        total++;
        try {
            System.out.println("Test " + total + ": InvalidBidException");
            throw new InvalidBidException("Giá đặt không hợp lệ", "item_001", 50000, 100000);
        } catch (InvalidBidException e) {
            System.out.println("   ✅ Bắt được ngoại lệ: " + e.getMessage());
            System.out.println("   📌 Item ID: " + e.getItemId());
            System.out.println("   📌 Giá đặt: " + e.getAttemptedBid());
            System.out.println("   📌 Giá tối thiểu: " + e.getMinimumRequired());
            passed++;
        } catch (Exception e) {
            System.out.println("   ❌ Sai: " + e.getMessage());
        }
        System.out.println();

        // ===== TEST 2: AuctionClosedException (4 tham số) =====
        total++;
        try {
            System.out.println("Test " + total + ": AuctionClosedException (đầy đủ)");
            throw new AuctionClosedException("Phiên đấu giá đã kết thúc", "item_001", "bidder_001", 1500000);
        } catch (AuctionClosedException e) {
            System.out.println("   ✅ Bắt được ngoại lệ: " + e.getMessage());
            System.out.println("   📌 Item ID: " + e.getItemId());
            System.out.println("   📌 Người thắng: " + e.getWinnerId());
            System.out.println("   📌 Giá cuối: " + e.getFinalPrice());
            passed++;
        } catch (Exception e) {
            System.out.println("   ❌ Sai: " + e.getMessage());
        }
        System.out.println();

        // ===== TEST 3: AuctionClosedException (2 tham số) =====
        total++;
        try {
            System.out.println("Test " + total + ": AuctionClosedException (đơn giản)");
            throw new AuctionClosedException("Phiên đấu giá đã bị hủy", "item_002");
        } catch (AuctionClosedException e) {
            System.out.println("   ✅ Bắt được ngoại lệ: " + e.getMessage());
            System.out.println("   📌 Item ID: " + e.getItemId());
            passed++;
        } catch (Exception e) {
            System.out.println("   ❌ Sai: " + e.getMessage());
        }
        System.out.println();

        // ===== TEST 4: AuthenticationException =====
        total++;
        try {
            System.out.println("Test " + total + ": AuthenticationException");
            throw new AuthenticationException("Sai tên đăng nhập hoặc mật khẩu", "john_doe", "password sai 3 lần");
        } catch (AuthenticationException e) {
            System.out.println("   ✅ Bắt được ngoại lệ: " + e.getMessage());
            System.out.println("   📌 Username: " + e.getUsername());
            System.out.println("   📌 Lý do: " + e.getReason());
            passed++;
        } catch (Exception e) {
            System.out.println("   ❌ Sai: " + e.getMessage());
        }
        System.out.println();

        // ===== TEST 5: DataValidationException =====
        total++;
        try {
            System.out.println("Test " + total + ": DataValidationException");
            DataValidationException e = new DataValidationException("Dữ liệu sản phẩm không hợp lệ");
            e.addError("Tên sản phẩm không được để trống");
            e.addError("Giá khởi điểm phải lớn hơn 0");
            e.addError("Thời gian kết thúc phải sau thời gian bắt đầu");
            throw e;
        } catch (DataValidationException e) {
            System.out.println("   ✅ Bắt được ngoại lệ: " + e.getMessage());
            System.out.println("   📌 Các lỗi: " + e.getFormattedErrors());
            System.out.println("   📌 Số lỗi: " + e.getErrors().size());
            passed++;
        } catch (Exception e) {
            System.out.println("   ❌ Sai: " + e.getMessage());
        }
        System.out.println();

        // ===== TEST 6: ConnectionException =====
        total++;
        try {
            System.out.println("Test " + total + ": ConnectionException");
            throw new ConnectionException("Không thể kết nối đến server", "localhost", 9999);
        } catch (ConnectionException e) {
            System.out.println("   ✅ Bắt được ngoại lệ: " + e.getMessage());
            System.out.println("   📌 Host: " + e.getHost());
            System.out.println("   📌 Port: " + e.getPort());
            passed++;
        } catch (Exception e) {
            System.out.println("   ❌ Sai: " + e.getMessage());
        }
        System.out.println();

        // ===== KẾT QUẢ =====
        System.out.println("═══════════════════════════════════════");
        System.out.println("📊 KẾT QUẢ KIỂM TRA: " + passed + "/" + total + " PASSED");
        System.out.println("═══════════════════════════════════════");

        if (passed == total) {
            System.out.println("🎉 TẤT CẢ TEST ĐỀU THÀNH CÔNG!");
        } else {
            System.out.println("⚠️ CÓ " + (total - passed) + " TEST THẤT BẠI!");
        }
    }
}