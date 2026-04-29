package client;

import common.*;

public class TestBidder {
    public static void main(String[] args) throws Exception {
        AuctionClient client = new AuctionClient();
        client.connect("localhost", 9999);

        client.startRealtimeListener(msg -> {
            System.out.println("🔔 [BIDDER] REALTIME: " + msg.getMessage());
        });

        ResponseMessage loginResp = client.login("bidder1", "123456");
        System.out.println("Login result: " + loginResp.getCode() + " - " + loginResp.getMessage());

        if (loginResp.getCode() == ResponseCode.SUCCESS) {
            client.subscribeItem("item1");

            Thread.sleep(1000);

            System.out.println("💰 BIDDING...");
            Bid bid = new Bid();
            bid.setItemId("item1");
            bid.setAmount(6000000);

            ResponseMessage bidResp = client.placeBid(bid);
            System.out.println("Bid result: " + bidResp.getCode() + " - " + bidResp.getMessage());

            // Giữ client chạy để nhận realtime
            System.out.println("⏳ Waiting for realtime updates... (Press Enter to exit)");
            System.in.read();
        }

        client.disconnect();
    }
}