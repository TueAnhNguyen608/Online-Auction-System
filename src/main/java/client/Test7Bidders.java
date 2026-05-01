package client;

import common.*;
import java.util.*;
import java.util.concurrent.*;

public class Test7Bidders {

    public static void main(String[] args) throws Exception {
        List<AuctionClient> clients = new ArrayList<>();
        List<String> bidders = Arrays.asList("bidder1", "bidder2", "bidder3",
                "bidder4", "bidder5", "bidder6", "bidder7");

        System.out.println("KHOI DONG 7 BIDDER...\n");

        // Ket noi va login cho 7 bidder
        for (int i = 0; i < bidders.size(); i++) {
            String bidderName = bidders.get(i);
            AuctionClient client = new AuctionClient();
            client.connect("localhost", 9999);

            final String name = bidderName;
            client.startRealtimeListener(msg -> {
                System.out.println("🔔 [" + name + "] " + msg.getMessage());
            });

            ResponseMessage login = client.login(bidderName, "123456");
            if (login.getCode() == ResponseCode.SUCCESS) {
                System.out.println("✅ " + bidderName + " da dang nhap");
                clients.add(client);
            }
        }

        Thread.sleep(2000);

        // Lay danh sach items dang dau gia
        if (!clients.isEmpty()) {
            AuctionClient tempClient = clients.get(0);
            ResponseMessage itemsResp = tempClient.getActiveItems();
            Object data = itemsResp.getData();

            if (data instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Item> items = (List<Item>) data;

                if (items != null && !items.isEmpty()) {
                    System.out.println("\n📦 Co " + items.size() + " san pham dang dau gia\n");

                    // Subscribe tat ca items
                    for (AuctionClient client : clients) {
                        for (Item item : items) {
                            client.subscribeItem(item.getId());
                        }
                    }

                    System.out.println("🎯 7 BIDDER DA SAN SANG!\n");
                    System.out.println("⏳ De nguyen de nhan realtime update...");
                    System.out.println("   Hoac mo them terminal de test dau gia\n");
                }
            }
        }

        // Giữ chương trình chạy
        System.out.println("Nhan Enter de thoat...");
        System.in.read();

        // Disconnect
        for (AuctionClient client : clients) {
            client.disconnect();
        }
    }
}