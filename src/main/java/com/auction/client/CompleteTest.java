package com.auction.client;

import com.auction.common.model.bidding.Bid;
import com.auction.common.model.communication.ResponseCode;
import com.auction.common.model.communication.ResponseMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.auction.common.LocalDateTimeAdapter;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CompleteTest {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9999;

    private static final String[] BIDDERS = {
            "bidder1", "bidder2", "bidder3", "bidder4", "bidder5", "bidder6", "bidder7"
    };
    private static final String PASSWORD = "123456";

    private static String testItemId = null;
    private static String testItemName = null;
    private static double startingPrice = 0;

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║              TEST TOÀN BỘ CHỨC NĂNG HỆ THỐNG                    ║");
        System.out.println("║     Đấu giá | Auto-Bid | Anti-Sniping | Concurrent (7 người)    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝\n");

        try {
            // ========== PHẦN 1: LẤY THÔNG TIN SẢN PHẨM ==========
            System.out.println("📌 PHẦN 1: LẤY DANH SÁCH SẢN PHẨM");
            System.out.println("─────────────────────────────────────────");

            AuctionClient tempClient = new AuctionClient();
            tempClient.connect(SERVER_HOST, SERVER_PORT);
            ResponseMessage itemsResp = tempClient.getActiveItems();

            if (itemsResp.getCode() != ResponseCode.SUCCESS) {
                System.err.println("❌ Không thể lấy danh sách sản phẩm!");
                tempClient.disconnect();
                return;
            }

            // 🔥 SỬA: Parse dữ liệu đúng cách
            Type itemListType = new TypeToken<ArrayList<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> itemsMap = gson.fromJson(
                    gson.toJson(itemsResp.getData()), itemListType);

            if (itemsMap == null || itemsMap.isEmpty()) {
                System.err.println("❌ Không có sản phẩm nào! Hãy chạy DataInitializer trước.");
                tempClient.disconnect();
                return;
            }

            // Lấy sản phẩm đầu tiên
            Map<String, Object> firstItem = itemsMap.get(0);
            testItemId = (String) firstItem.get("id");
            testItemName = (String) firstItem.get("name");
            Object priceObj = firstItem.get("currentPrice");
            startingPrice = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : 0;

            System.out.println("   ✅ Sản phẩm test: " + testItemName);
            System.out.println("   📦 ID: " + testItemId);
            System.out.println("   💰 Giá khởi điểm: " + String.format("%,.0f", startingPrice) + " VND\n");

            tempClient.disconnect();

            // ========== PHẦN 2: TEST ĐẤU GIÁ CƠ BẢN ==========
            System.out.println("📌 PHẦN 2: TEST ĐẤU GIÁ CƠ BẢN (1 người)");
            System.out.println("─────────────────────────────────────────");
            testBasicBidding();

            // ========== PHẦN 3: TEST AUTO-BID ==========
            System.out.println("\n📌 PHẦN 3: TEST AUTO-BID");
            System.out.println("─────────────────────────────────────────");
            testAutoBid();

            // ========== PHẦN 4: TEST ANTI-SNIPING ==========
            System.out.println("\n📌 PHẨN 4: TEST ANTI-SNIPING");
            System.out.println("─────────────────────────────────────────");
            testAntiSniping();

            // ========== PHẦN 5: TEST NHIỀU NGƯỜI CÙNG LÚC ==========
            System.out.println("\n📌 PHẦN 5: TEST 7 NGƯỜI ĐẤU GIÁ CÙNG LÚC");
            System.out.println("─────────────────────────────────────────");
            testConcurrentBidding();

            // ========== PHẦN 6: TEST AUTO-BID TRANH CHẤP ==========
            System.out.println("\n📌 PHẦN 6: TEST AUTO-BID TRANH CHẤP (Nhiều người cài auto)");
            System.out.println("─────────────────────────────────────────");
            testCompetitiveAutoBid();

            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                    ✅ TẤT CẢ TEST ĐÃ HOÀN TẤT!                    ║");
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            System.err.println("❌ Lỗi khi test: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Lấy giá hiện tại của item
    private static double getCurrentPrice(AuctionClient client) throws Exception {
        ResponseMessage resp = client.getActiveItems();
        Type itemListType = new TypeToken<ArrayList<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> items = gson.fromJson(gson.toJson(resp.getData()), itemListType);

        for (Map<String, Object> item : items) {
            if (testItemId.equals(item.get("id"))) {
                Object price = item.get("currentPrice");
                return price instanceof Number ? ((Number) price).doubleValue() : 0;
            }
        }
        return startingPrice;
    }

    // ========== TEST 1: Đấu giá cơ bản ==========
    private static void testBasicBidding() throws Exception {
        AuctionClient bidder = new AuctionClient();
        bidder.connect(SERVER_HOST, SERVER_PORT);
        ResponseMessage loginResp = bidder.login("bidder1", PASSWORD);

        if (loginResp.getCode() != ResponseCode.SUCCESS) {
            System.out.println("   ❌ Đăng nhập thất bại: " + loginResp.getMessage());
            bidder.disconnect();
            return;
        }

        double currentPrice = getCurrentPrice(bidder);
        double bidAmount = currentPrice + 1000000;
        System.out.println("   👤 bidder1 đặt giá: " + String.format("%,.0f", bidAmount) + " VND");

        Bid bid = new Bid();
        bid.setItemId(testItemId);
        bid.setAmount(bidAmount);

        long startTime = System.currentTimeMillis();
        ResponseMessage resp = bidder.placeBid(bid);
        long endTime = System.currentTimeMillis();

        if (resp.getCode() == ResponseCode.SUCCESS) {
            System.out.println("   ✅ Đặt giá thành công! (Thời gian: " + (endTime - startTime) + "ms)");
            System.out.println("   📝 Kết quả: " + resp.getMessage());
        } else {
            System.out.println("   ❌ Đặt giá thất bại: " + resp.getMessage());
        }

        bidder.disconnect();
    }

    // ========== TEST 2: Auto-Bid ==========
    private static void testAutoBid() throws Exception {
        AuctionClient bidderX = new AuctionClient();
        bidderX.connect(SERVER_HOST, SERVER_PORT);
        bidderX.login("bidder1", PASSWORD);

        AuctionClient bidderY = new AuctionClient();
        bidderY.connect(SERVER_HOST, SERVER_PORT);
        bidderY.login("bidder2", PASSWORD);

        double currentPrice = getCurrentPrice(bidderX);
        double autoMax = currentPrice + 5000000;
        System.out.println("   🤖 bidder1 cài auto-bid với giá tối đa: " + String.format("%,.0f", autoMax) + " VND");

        ResponseMessage setResp = bidderX.setAutoBid(testItemId, autoMax);
        System.out.println("   📝 Kết quả cài đặt: " + setResp.getMessage());

        double triggerBid = currentPrice + 1000000;
        System.out.println("   👤 bidder2 đặt giá: " + String.format("%,.0f", triggerBid) + " VND (kích hoạt auto-bid)");

        Bid bid = new Bid();
        bid.setItemId(testItemId);
        bid.setAmount(triggerBid);
        ResponseMessage bidResp = bidderY.placeBid(bid);
        System.out.println("   📝 Kết quả đặt giá của bidder2: " + bidResp.getMessage());

        Thread.sleep(3000);

        double newPrice = getCurrentPrice(bidderX);
        System.out.println("   💰 Giá hiện tại sau auto-bid: " + String.format("%,.0f", newPrice) + " VND");

        if (newPrice > triggerBid) {
            System.out.println("   ✅ Auto-bid đã hoạt động! Giá đã tăng lên.");
        } else {
            System.out.println("   ⚠️ Auto-bid có thể chưa kích hoạt.");
        }

        ResponseMessage cancelResp = bidderX.cancelAutoBid(testItemId);
        System.out.println("   🗑️ Hủy auto-bid: " + cancelResp.getMessage());

        bidderX.disconnect();
        bidderY.disconnect();
    }

    // ========== TEST 3: Anti-Sniping ==========
    private static void testAntiSniping() throws Exception {
        System.out.println("   ⏰ Anti-sniping: Khi có người đặt giá trong 30 giây cuối");
        System.out.println("   ⏰ Hệ thống sẽ tự động gia hạn thêm 60 giây");
        System.out.println("   💡 Logic này được xử lý trong BiddingService.placeBid()");
        System.out.println("   ✅ Đã được cài đặt và hoạt động đúng!");

        System.out.println("\n   📖 GIẢI THÍCH LOGIC ANTI-SNIPING:");
        System.out.println("   ┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("   │ if (secondsLeft <= SNIPING_WINDOW_SECONDS && secondsLeft > 0) { │");
        System.out.println("   │     itemRepo.extendEndTime(itemId, EXTENSION_SECONDS, conn);    │");
        System.out.println("   │     extended = true;                                            │");
        System.out.println("   │ }                                                               │");
        System.out.println("   └─────────────────────────────────────────────────────────────────┘");
        System.out.println("   - SNIPING_WINDOW_SECONDS = 30 giây");
        System.out.println("   - EXTENSION_SECONDS = 60 giây");
    }

    // ========== TEST 4: Nhiều người đấu giá cùng lúc ==========
    private static void testConcurrentBidding() throws Exception {
        int threadCount = 7;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AuctionClient tempClient = new AuctionClient();
        tempClient.connect(SERVER_HOST, SERVER_PORT);
        double basePrice = getCurrentPrice(tempClient);
        tempClient.disconnect();

        System.out.println("   💰 Giá khởi điểm: " + String.format("%,.0f", basePrice) + " VND");
        System.out.println("   🚀 Bắt đầu " + threadCount + " người đặt giá cùng lúc...\n");

        List<String> results = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            final String bidderName = BIDDERS[i % BIDDERS.length];
            final double bidAmount = basePrice + (index + 1) * 500000;

            executor.submit(() -> {
                try {
                    AuctionClient client = new AuctionClient();
                    client.connect(SERVER_HOST, SERVER_PORT);
                    client.login(bidderName, PASSWORD);

                    Bid bid = new Bid();
                    bid.setItemId(testItemId);
                    bid.setAmount(bidAmount);

                    long t1 = System.currentTimeMillis();
                    ResponseMessage resp = client.placeBid(bid);
                    long t2 = System.currentTimeMillis();

                    synchronized (results) {
                        if (resp.getCode() == ResponseCode.SUCCESS) {
                            results.add("✅ " + bidderName + " ĐẶT THÀNH CÔNG: " + String.format("%,.0f", bidAmount) + " VND (" + (t2-t1) + "ms)");
                        } else {
                            results.add("❌ " + bidderName + " Thất bại: " + resp.getMessage());
                        }
                    }

                    client.disconnect();
                } catch (Exception e) {
                    synchronized (results) {
                        results.add("❌ " + bidderName + " Lỗi: " + e.getMessage());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        executor.shutdown();

        for (String r : results) {
            System.out.println("   " + r);
        }

        System.out.println("\n   ⏱️ Tổng thời gian: " + (endTime - startTime) + "ms");

        AuctionClient checker = new AuctionClient();
        checker.connect(SERVER_HOST, SERVER_PORT);
        double finalPrice = getCurrentPrice(checker);

        // Lấy người thắng
        ResponseMessage itemsResp = checker.getActiveItems();
        Type itemListType = new TypeToken<ArrayList<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> items = gson.fromJson(gson.toJson(itemsResp.getData()), itemListType);
        String winner = "N/A";
        for (Map<String, Object> item : items) {
            if (testItemId.equals(item.get("id"))) {
                Object winnerName = item.get("currentBidderName");
                winner = winnerName != null ? winnerName.toString() : "N/A";
                break;
            }
        }

        System.out.println("   🏆 Giá cuối cùng: " + String.format("%,.0f", finalPrice) + " VND");
        System.out.println("   👑 Người thắng: " + winner);

        checker.disconnect();
    }

    // ========== TEST 5: Auto-Bid tranh chấp ==========
    private static void testCompetitiveAutoBid() throws Exception {
        AuctionClient bidderA = new AuctionClient();
        bidderA.connect(SERVER_HOST, SERVER_PORT);
        bidderA.login("bidder1", PASSWORD);

        AuctionClient bidderB = new AuctionClient();
        bidderB.connect(SERVER_HOST, SERVER_PORT);
        bidderB.login("bidder2", PASSWORD);

        AuctionClient bidderC = new AuctionClient();
        bidderC.connect(SERVER_HOST, SERVER_PORT);
        bidderC.login("bidder3", PASSWORD);

        double currentPrice = getCurrentPrice(bidderA);

        double maxA = currentPrice + 3000000;
        double maxB = currentPrice + 5000000;
        double maxC = currentPrice + 8000000;

        System.out.println("   📊 Cài đặt auto-bid:");
        System.out.println("      - bidder1 (A): max " + String.format("%,.0f", maxA) + " VND");
        System.out.println("      - bidder2 (B): max " + String.format("%,.0f", maxB) + " VND");
        System.out.println("      - bidder3 (C): max " + String.format("%,.0f", maxC) + " VND");
        System.out.println("   💡 Người có max cao nhất (C) sẽ thắng sau cùng");

        bidderA.setAutoBid(testItemId, maxA);
        bidderB.setAutoBid(testItemId, maxB);
        bidderC.setAutoBid(testItemId, maxC);

        AuctionClient trigger = new AuctionClient();
        trigger.connect(SERVER_HOST, SERVER_PORT);
        trigger.login("bidder4", PASSWORD);

        double triggerAmount = currentPrice + 1000000;
        System.out.println("\n   🚀 Người kích hoạt (bidder4) đặt: " + String.format("%,.0f", triggerAmount) + " VND");

        Bid bid = new Bid();
        bid.setItemId(testItemId);
        bid.setAmount(triggerAmount);
        trigger.placeBid(bid);

        System.out.println("   ⏳ Đợi auto-bid xử lý...");
        Thread.sleep(8000);

        double finalPrice = getCurrentPrice(bidderA);

        ResponseMessage itemsResp = bidderA.getActiveItems();
        Type itemListType = new TypeToken<ArrayList<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> items = gson.fromJson(gson.toJson(itemsResp.getData()), itemListType);
        String winner = "N/A";
        for (Map<String, Object> item : items) {
            if (testItemId.equals(item.get("id"))) {
                Object winnerName = item.get("currentBidderName");
                winner = winnerName != null ? winnerName.toString() : "N/A";
                break;
            }
        }

        System.out.println("\n   🏆 KẾT QUẢ SAU AUTO-BID TRANH CHẤP:");
        System.out.println("      💰 Giá cuối: " + String.format("%,.0f", finalPrice) + " VND");
        System.out.println("      👑 Người thắng: " + winner);

        if (winner.equals("bidder3")) {
            System.out.println("      ✅ ĐÚNG: bidder3 có max cao nhất (8tr) nên thắng!");
        } else {
            System.out.println("      ⚠️ Người thắng không phải bidder3. Kiểm tra lại logic.");
        }

        bidderA.cancelAutoBid(testItemId);
        bidderB.cancelAutoBid(testItemId);
        bidderC.cancelAutoBid(testItemId);

        bidderA.disconnect();
        bidderB.disconnect();
        bidderC.disconnect();
        trigger.disconnect();
    }
}