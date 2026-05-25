package com.auction.client;

import com.auction.common.model.communication.ResponseMessage;

public class TestClient {
    public static void main(String[] args) {
        System.out.println("========== BASIC CONNECTION TEST ==========");

        try {
            AuctionClient client = new AuctionClient();
            client.connect("localhost", 9999);
            System.out.println("✅ Connected to server");

            System.out.println("\n[TEST] Login with bidder1/123456...");
            ResponseMessage loginResp = client.login("bidder1", "123456");
            System.out.println("   Result: " + loginResp.getCode() + " - " + loginResp.getMessage());

            if (loginResp.getCode().getCode() == 200) {
                System.out.println("✅ Login successful!");

                System.out.println("\n[TEST] Getting active items...");
                ResponseMessage itemsResp = client.getActiveItems();
                System.out.println("   Result: " + itemsResp.getMessage());
                System.out.println("   Data: " + itemsResp.getData());
            }

            client.disconnect();
            System.out.println("\n✅ Basic connection test completed!");

        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("============================================");
    }
}