package client;

import common.model.bidding.Bid;
import common.model.communication.ResponseCode;
import common.model.communication.ResponseMessage;

public class TestBidder {
    public static void main(String[] args) throws Exception {
        System.out.println("========== BIDDER TEST ==========");

        AuctionClient client = new AuctionClient();
        client.connect("localhost", 9999);

        // Listener để nhận realtime
        client.startRealtimeListener(msg -> {
            System.out.println("🔔 [REALTIME] " + msg.getMessage());
        });

        // Đăng nhập
        ResponseMessage loginResp = client.login("bidder2", "123456");
        System.out.println("Login: " + loginResp.getMessage());

        if (loginResp.getCode() == ResponseCode.SUCCESS) {
            // Lấy danh sách item
            ResponseMessage itemsResp = client.getActiveItems();
            System.out.println("Active items: " + itemsResp.getMessage());

            // Đặt giá cho item đầu tiên (giả sử có item)
            System.out.println("\n💰 Placing bid...");
            Bid bid = new Bid();
            bid.setItemId("item1");  // Thay bằng item ID thực tế
            bid.setAmount(10000000); // 10 triệu

            ResponseMessage bidResp = client.placeBid(bid);
            System.out.println("Bid result: " + bidResp.getCode() + " - " + bidResp.getMessage());
        }

        // Giữ client chạy 30s để nhận realtime
        System.out.println("\n⏳ Waiting 30 seconds for realtime updates...");
        Thread.sleep(30000);

        client.disconnect();
        System.out.println("========== TEST COMPLETED ==========");
    }
}