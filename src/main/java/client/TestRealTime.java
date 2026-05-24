package client;

import common.model.communication.ResponseCode;
import common.model.communication.ResponseMessage;
import common.model.event.PriceUpdateEvent;

public class TestRealTime {
    public static void main(String[] args) throws Exception {
        System.out.println("========== REALTIME TEST ==========");

        AuctionClient client = new AuctionClient();
        client.connect("localhost", 9999);

        // Đăng ký listener để nhận realtime push
        client.startRealtimeListener(msg -> {
            System.out.println("\n🔔 REALTIME PUSH RECEIVED:");
            System.out.println("   Message: " + msg.getMessage());
            System.out.println("   Code: " + msg.getCode());

            if (msg.getData() != null) {
                System.out.println("   Data: " + msg.getData());

                // Nếu là PriceUpdateEvent, hiển thị chi tiết
                try {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    String json = gson.toJson(msg.getData());
                    System.out.println("   JSON: " + json);
                } catch (Exception e) {
                    // Ignore
                }
            }
            System.out.println("-----------------------------------");
        });

        // Đăng nhập
        ResponseMessage loginResp = client.login("bidder1", "123456");
        System.out.println("Login result: " + loginResp.getCode() + " - " + loginResp.getMessage());

        if (loginResp.getCode() == ResponseCode.SUCCESS) {
            // Subscribe vào item để nhận realtime
            // Lấy danh sách item đang đấu giá trước
            ResponseMessage itemsResp = client.getActiveItems();
            if (itemsResp.getData() != null) {
                // Lấy item đầu tiên để test
                com.google.gson.Gson gson = new com.google.gson.Gson();
                String json = gson.toJson(itemsResp.getData());
                com.google.gson.reflect.TypeToken token = new com.google.gson.reflect.TypeToken<java.util.ArrayList<common.model.item.Item>>(){};
                java.util.List<common.model.item.Item> items = gson.fromJson(json, token.getType());

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