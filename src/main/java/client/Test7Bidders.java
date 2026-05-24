package client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import common.LocalDateTimeAdapter;
import common.model.bidding.Bid;
import common.model.communication.ResponseCode;
import common.model.communication.ResponseMessage;
import common.model.item.Item;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Test7Bidders {

    private static final AtomicInteger realtimeCounter = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        List<AuctionClient> clients = new ArrayList<>();
        List<String> bidders = Arrays.asList("bidder1", "bidder2", "bidder3",
                "bidder4", "bidder5", "bidder6", "bidder7");

        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                   7 BIDDERS TEST                              ║");
        System.out.println("║           Testing: Concurrent Bidding + Auto-Bid             ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        System.out.println("[1] Connecting 7 bidders to server...\n");

        // Ket noi va login cho 7 bidder
        for (String bidderName : bidders) {
            AuctionClient client = new AuctionClient();
            client.connect("localhost", 9999);

            final String name = bidderName;
            client.startRealtimeListener(msg -> {
                int count = realtimeCounter.incrementAndGet();
                System.out.println("🔔 [" + name + "] Realtime #" + count + ": " + msg.getMessage());
                if (msg.getData() != null && msg.getData().toString().contains("currentPrice")) {
                    System.out.println("        📊 Price update received!");
                }
            });

            ResponseMessage login = client.login(bidderName, "123456");
            if (login.getCode() == ResponseCode.SUCCESS) {
                System.out.println("   ✅ " + bidderName + " logged in");
                clients.add(client);
            } else {
                System.out.println("   ❌ " + bidderName + " login failed: " + login.getMessage());
            }
        }

        System.out.println("\n[2] Getting active items...");
        Thread.sleep(1000);

        // Lay danh sach items dang dau gia
        if (clients.isEmpty()) {
            System.err.println("❌ No clients connected!");
            return;
        }

        AuctionClient tempClient = clients.get(0);
        ResponseMessage itemsResp = tempClient.getActiveItems();

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(java.time.LocalDateTime.class, new LocalDateTimeAdapter())
                .create();

        Object data = itemsResp.getData();
        if (!(data instanceof List<?>)) {
            System.err.println("❌ No active items found!");
            return;
        }

        String json = gson.toJson(data);
        Type itemListType = new TypeToken<ArrayList<Item>>(){}.getType();
        List<Item> items = gson.fromJson(json, itemListType);

        if (items == null || items.isEmpty()) {
            System.err.println("❌ No active items found! Please run DataInitializer first.");
            return;
        }

        System.out.println("\n   Found " + items.size() + " active items:");
        for (Item item : items) {
            System.out.println("      📦 " + item.getName() + " | Current price: " +
                    String.format("%,.0f", item.getCurrentPrice()) + " VND | ID: " + item.getId());
        }

        // Subscribe tat ca items cho tat ca bidder
        System.out.println("\n[3] Subscribing all bidders to all items...");
        for (AuctionClient client : clients) {
            for (Item item : items) {
                client.subscribeItem(item.getId());
            }
        }
        System.out.println("   ✅ All bidders subscribed!");

        System.out.println("\n[4] Starting test scenarios...\n");
        System.out.println("╔══════════════════════════════════════════════════════════════╗");

        // ========== TEST 1: Basic bidding ==========
        System.out.println("║ TEST 1: Basic Bidding                                        ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");

        if (!items.isEmpty()) {
            Item firstItem = items.get(0);
            double currentPrice = firstItem.getCurrentPrice();
            double bidAmount = currentPrice + 500000;

            System.out.println("   Item: " + firstItem.getName());
            System.out.println("   Current price: " + String.format("%,.0f", currentPrice) + " VND");
            System.out.println("   Bidder1 bids: " + String.format("%,.0f", bidAmount) + " VND");

            Bid bid = new Bid();
            bid.setItemId(firstItem.getId());
            bid.setAmount(bidAmount);

            ResponseMessage bidResp = clients.get(0).placeBid(bid);
            System.out.println("   Result: " + bidResp.getMessage());
            Thread.sleep(2000);
        }

        // ========== TEST 2: Outbid test ==========
        System.out.println("\n║ TEST 2: Outbid Test                                          ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");

        if (items.size() > 1) {
            Item secondItem = items.get(1);
            double currentPrice = secondItem.getCurrentPrice();
            double bidAmount1 = currentPrice + 1000000;
            double bidAmount2 = bidAmount1 + 500000;

            System.out.println("   Item: " + secondItem.getName());
            System.out.println("   Current price: " + String.format("%,.0f", currentPrice) + " VND");
            System.out.println("   Bidder1 bids: " + String.format("%,.0f", bidAmount1) + " VND");

            Bid bid1 = new Bid();
            bid1.setItemId(secondItem.getId());
            bid1.setAmount(bidAmount1);
            clients.get(0).placeBid(bid1);
            Thread.sleep(1000);

            System.out.println("   Bidder2 outbid: " + String.format("%,.0f", bidAmount2) + " VND");
            Bid bid2 = new Bid();
            bid2.setItemId(secondItem.getId());
            bid2.setAmount(bidAmount2);
            clients.get(1).placeBid(bid2);
            Thread.sleep(2000);
        }

        // ========== TEST 3: Auto-bid test ==========
        System.out.println("\n║ TEST 3: Auto-Bid Test                                        ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");

        if (items.size() > 2) {
            Item thirdItem = items.get(2);
            double currentPrice = thirdItem.getCurrentPrice();
            double autoMaxAmount = currentPrice + 5000000;

            System.out.println("   Item: " + thirdItem.getName());
            System.out.println("   Current price: " + String.format("%,.0f", currentPrice) + " VND");
            System.out.println("   Setting auto-bid for bidder3: max " + String.format("%,.0f", autoMaxAmount) + " VND");

            ResponseMessage autoResp = clients.get(2).setAutoBid(thirdItem.getId(), autoMaxAmount);
            System.out.println("   Auto-bid result: " + autoResp.getMessage());
            Thread.sleep(1000);

            System.out.println("   Bidder4 places bid: " + String.format("%,.0f", currentPrice + 1000000) + " VND");
            Bid triggerBid = new Bid();
            triggerBid.setItemId(thirdItem.getId());
            triggerBid.setAmount(currentPrice + 1000000);
            clients.get(3).placeBid(triggerBid);
            Thread.sleep(3000);

            System.out.println("   🤖 Auto-bid should trigger automatically!");
        }

        // ========== TEST 4: Anti-sniping test ==========
        System.out.println("\n║ TEST 4: Anti-Sniping Test (if time near end)                ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");

        if (items.size() > 3) {
            Item fourthItem = items.get(3);
            System.out.println("   Item: " + fourthItem.getName());
            System.out.println("   Note: Anti-sniping triggers when bid in last 30 seconds");
            System.out.println("   If auction ends soon, placing bid will extend time by 60s");
        }

        System.out.println("\n╚══════════════════════════════════════════════════════════════╝");

        // Summary
        System.out.println("\n[5] Test Summary");
        System.out.println("   Total realtime messages received: " + realtimeCounter.get());
        System.out.println("   Active clients: " + clients.size());
        System.out.println("   Active items: " + items.size());

        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    TEST COMPLETED                            ║");
        System.out.println("║  Press Enter to exit...                                      ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        System.in.read();

        // Disconnect
        for (AuctionClient client : clients) {
            client.disconnect();
        }

        System.out.println("\n✅ All clients disconnected. Test finished!");
    }
}