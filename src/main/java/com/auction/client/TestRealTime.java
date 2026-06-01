package com.auction.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.auction.common.model.communication.ResponseCode;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.common.model.item.Item;

import java.util.List;

public class TestRealTime {
    public static void main(String[] args) throws Exception {
        System.out.println("========== REALTIME TEST ==========");

        AuctionClient client = new AuctionClient();
        client.connect("localhost", 9999);

        // 🔥 ĐỢI KẾT NỐI ỔN ĐỊNH
        Thread.sleep(500);

        // 🔥 ĐĂNG NHẬP TRƯỚC
        ResponseMessage loginResp = client.login("bidder1", "123456");
        System.out.println("Login result: " + loginResp.getCode() + " - " + loginResp.getMessage());

        if (loginResp.getCode() != ResponseCode.SUCCESS) {
            System.err.println("Login failed! Exiting...");
            client.disconnect();
            return;
        }

        // 🔥 ĐỢI SESSION ĐƯỢC TẠO TRÊN SERVER
        Thread.sleep(500);

        // 🔥 SAU ĐÓ MỚI BẬT LISTENER
        client.startRealtimeListener(msg -> {
            System.out.println("\n🔔 REALTIME PUSH RECEIVED:");
            System.out.println("   Message: " + msg.getMessage());
            System.out.println("   Code: " + msg.getCode());
            if (msg.getData() != null) {
                System.out.println("   Data: " + msg.getData());
            }
            System.out.println("-----------------------------------");
        });

        // 🔥 LẤY DANH SÁCH SẢN PHẨM
        ResponseMessage itemsResp = client.getActiveItems();
        System.out.println("Get items result: " + itemsResp.getCode());

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
                System.out.println("❌ No active items found.");
            }
        } else {
            System.out.println("❌ No active items found.");
        }

        System.in.read();
        client.disconnect();
        System.out.println("\n========== TEST COMPLETED ==========");
    }
}