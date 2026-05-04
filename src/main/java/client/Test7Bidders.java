package client;

import common.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;

public class Test7Bidders {

    public static void main(String[] args) throws Exception {
        List<AuctionClient> clients = new ArrayList<>();
        List<String> bidders = Arrays.asList("bidder1", "bidder2", "bidder3",
                "bidder4", "bidder5", "bidder6", "bidder7");

        System.out.println("KHOI DONG 7 BIDDER...\n");

        // Ket noi va login cho 7 bidder
        for (String bidderName : bidders) {
            AuctionClient client = new AuctionClient();
            client.connect("localhost", 9999);

            client.startRealtimeListener(msg ->
                    System.out.println("🔔 [" + bidderName + "] " + msg.getMessage())
            );

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

            // Dùng Gson để parse dữ liệu
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(java.time.LocalDateTime.class, new common.LocalDateTimeAdapter())
                    .create();

            Object data = itemsResp.getData();
            if (data instanceof List<?>) {
                // Chuyển List sang JSON rồi parse lại thành List<Item>
                String json = gson.toJson(data);
                Type itemListType = new TypeToken<ArrayList<Item>>(){}.getType();
                List<Item> items = gson.fromJson(json, itemListType);

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