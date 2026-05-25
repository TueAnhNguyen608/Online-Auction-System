package com.auction.client;

import com.auction.common.model.bidding.Bid;
import com.auction.common.model.communication.ResponseCode;
import com.auction.common.model.communication.ResponseMessage;

public class TestBidder {
    public static void main(String[] args) throws Exception {
        System.out.println("========== BIDDER TEST ==========");

        AuctionClient client = new AuctionClient();
        client.connect("localhost", 9999);

        client.startRealtimeListener(msg -> {
            System.out.println("🔔 [REALTIME] " + msg.getMessage());
        });

        ResponseMessage loginResp = client.login("bidder2", "123456");
        System.out.println("Login: " + loginResp.getMessage());

        if (loginResp.getCode() == ResponseCode.SUCCESS) {
            ResponseMessage itemsResp = client.getActiveItems();
            System.out.println("Active items: " + itemsResp.getMessage());

            System.out.println("\n💰 Placing bid...");
            Bid bid = new Bid();
            bid.setItemId("item1");
            bid.setAmount(10000000);

            ResponseMessage bidResp = client.placeBid(bid);
            System.out.println("Bid result: " + bidResp.getCode() + " - " + bidResp.getMessage());
        }

        System.out.println("\n⏳ Waiting 30 seconds for realtime updates...");
        Thread.sleep(30000);

        client.disconnect();
        System.out.println("========== TEST COMPLETED ==========");
    }
}