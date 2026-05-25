package com.auction.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.auction.common.model.communication.ResponseCode;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.common.model.item.Item;

import java.util.ArrayList;
import java.util.List;

public class TestRealTime {
    public static void main(String[] args) throws Exception {
        System.out.println("========== REALTIME TEST ==========");

        AuctionClient client = new AuctionClient();
        client.connect("localhost", 9999);

        client.startRealtimeListener(msg -> {
            System.out.println("\n🔔 REALTIME PUSH RECEIVED:");
            System.out.println("   Message: " + msg.getMessage());
            System.out.println("   Code: " + msg.getCode());

            if (msg.getData() != null) {
                System.out.println("   Data: " + msg.getData());

                try {
                    Gson gson = new Gson();
                    String json = gson.toJson(msg.getData());
                    System.out.println("   JSON: " + json);
                } catch (Exception e) {
                    // Ignore
                }
            }
            System.out.println("-----------------------------------");
        });

        ResponseMessage loginResp = client.login("bidder1", "123456");
        System.out.println("Login result: " + loginResp.getCode() + " - " + loginResp.getMessage());

        if (loginResp.getCode() == ResponseCode.SUCCESS) {
            ResponseMessage itemsResp = client.getActiveItems();
            if (itemsResp.getData() != null) {
                Gson gson = new Gson();
                String json = gson.toJson(itemsResp.getData());
                TypeToken<List<Item>> token = new TypeToken<List<Item>>() {};
                List<Item> items = gson.fromJson(json, token.getType());

                if (!items.isEmpty()) {
                    String firstItemId = items.get(0).getId();
                    String firstItemName = items.get(0).getName();

                    System.out.println("\n📦 Subscribing to item: " + firstItemName + " (" + firstItemId + ")");
                    ResponseMessage subResp = client.subscribeItem(firstItemId);
                    System.out.println("Subscribe result: " + subResp.getMessage());

                    System.out.println("\n⏳ Waiting for realtime updates...");
                    System.out.println("   (Open another terminal and run TestBidder to place bids)");
                    System.out.println("   Press Enter to exit...\n");
                } else {
                    System.out.println("❌ No active items found. Please create an item first.");
                }
            } else {
                System.out.println("❌ No active items found.");
            }

            System.in.read();
        }

        client.disconnect();
        System.out.println("\n========== TEST COMPLETED ==========");
    }
}