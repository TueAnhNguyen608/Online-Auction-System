package client;

import common.*;

public class TestRealTime {
    public static void main(String[] args) throws Exception {
        AuctionClient client = new AuctionClient();
        client.connect("localhost", 9999);

        client.startRealtimeListener(msg -> {
            System.out.println("🔔 REALTIME PUSH: " + msg.getMessage());
            if (msg.getData() != null) {
                System.out.println("   Data: " + msg.getData());
            }
        });

        ResponseMessage loginResp = client.login("bidder1", "123456");
        System.out.println("Login: " + loginResp.getMessage());

        if (loginResp.getCode() == ResponseCode.SUCCESS) {
            client.subscribeItem("item1");
            System.out.println("⏳ Listening for realtime updates... (Press Enter to exit)");
            System.in.read();
        }

        client.disconnect();
    }
}