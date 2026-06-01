package com.auction.client;

import com.auction.common.model.bidding.Bid;
import com.auction.common.model.communication.ResponseCode;
import com.auction.common.model.communication.ResponseMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestBidder {
    public static void main(String[] args) throws Exception {
        System.out.println("========== BIDDER TEST ==========");

        AuctionClient client = new AuctionClient();

        // Test kết nối
        try {
            client.connect("localhost", 9999);
            System.out.println("✅ Connected to server");
        } catch (Exception e) {
            System.err.println("❌ Connection failed: " + e.getMessage());
            return;
        }

        // Test realtime listener
        client.startRealtimeListener(msg -> {
            System.out.println("🔔 [REALTIME] " + msg.getMessage());
            if (msg.getData() != null) {
                System.out.println("        Data: " + msg.getData());
            }
        });

        // Test login
        ResponseMessage loginResp = client.login("bidder2", "123456");
        System.out.println("Login: " + loginResp.getCode() + " - " + loginResp.getMessage());

        if (loginResp.getCode() != ResponseCode.SUCCESS) {
            System.err.println("❌ Login failed, exiting...");
            client.disconnect();
            return;
        }

        // Test get active items
        ResponseMessage itemsResp = client.getActiveItems();
        System.out.println("Active items: " + itemsResp.getCode() + " - " + itemsResp.getMessage());

        // Kiểm tra dữ liệu trả về
        if (itemsResp.getData() != null) {
            Gson gson = new Gson();
            String json = gson.toJson(itemsResp.getData());
            Type itemListType = new TypeToken<ArrayList<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> items = gson.fromJson(json, itemListType);
            System.out.println("   Found " + items.size() + " items");

            if (!items.isEmpty()) {
                String firstItemId = (String) items.get(0).get("id");
                String firstItemName = (String) items.get(0).get("name");
                double currentPrice = ((Number) items.get(0).get("currentPrice")).doubleValue();

                System.out.println("\n💰 Placing bid on: " + firstItemName);
                System.out.println("   Current price: " + String.format("%,.0f", currentPrice) + " VND");

                double bidAmount = currentPrice + 1000000;
                System.out.println("   Bid amount: " + String.format("%,.0f", bidAmount) + " VND");

                Bid bid = new Bid();
                bid.setItemId(firstItemId);
                bid.setAmount(bidAmount);

                long startTime = System.currentTimeMillis();
                ResponseMessage bidResp = client.placeBid(bid);
                long endTime = System.currentTimeMillis();

                System.out.println("Bid result: " + bidResp.getCode() + " - " + bidResp.getMessage());
                System.out.println("   Time: " + (endTime - startTime) + "ms");

                // Kiểm tra lỗi cụ thể
                if (bidResp.getCode() == ResponseCode.BID_TOO_LOW) {
                    System.out.println("   ⚠️ Bid too low! Minimum required: need 5% higher");
                } else if (bidResp.getCode() == ResponseCode.AUCTION_ENDED) {
                    System.out.println("   ⚠️ Auction already ended!");
                } else if (bidResp.getCode() == ResponseCode.SUCCESS) {
                    System.out.println("   ✅ Bid placed successfully!");
                }
            }
        }

        System.out.println("\n⏳ Waiting 30 seconds for realtime updates...");
        Thread.sleep(30000);

        client.disconnect();
        System.out.println("========== TEST COMPLETED ==========");
    }
}