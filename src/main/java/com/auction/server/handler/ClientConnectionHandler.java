package com.auction.server.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.auction.common.LocalDateTimeAdapter;
import com.auction.common.model.communication.CommandType;
import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.common.model.user.User;
import com.auction.server.broadcast.EventBroadcaster;
import com.auction.server.repository.ItemRepository;
import com.auction.common.model.item.Item;
import com.auction.common.model.item.ItemStatus;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ClientConnectionHandler implements Runnable {

    private final Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String currentUserId;
    private String currentUsername;
    private boolean isLoggedIn = false;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public ClientConnectionHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                System.out.println("📨 Received: " + line);

                try {
                    Request request = gson.fromJson(line, Request.class);

                    // 🔥 THÊM: lấy dispatcher và gửi kèm this (client handler)
                    RequestDispatcher dispatcher = RequestDispatcher.getInstance();
                    ResponseMessage response = dispatcher.dispatch(request, this);

                    String jsonResponse = gson.toJson(response);
                    System.out.println("📤 Sending: " + jsonResponse);
                    out.println(jsonResponse);
                    out.flush();

                    if (request.getCommand() == CommandType.LOGOUT) {
                        break;
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    String errorJson = gson.toJson(ResponseMessage.error(e.getMessage()));
                    out.println(errorJson);
                    out.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        } finally {
            // 🔥 THÊM: cleanup subscriptions khi disconnect
            EventBroadcaster.getInstance().cleanupClient(this);
            try { socket.close(); } catch (IOException e) {}
        }
    }

    public String getCurrentUserId() { return currentUserId; }
    public String getCurrentUsername() { return currentUsername; }
    public boolean isLoggedIn() { return isLoggedIn; }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
        this.isLoggedIn = userId != null;
    }
    public void setCurrentUsername(String username) { this.currentUsername = username; }

    public void pushRealtimeUpdate(String message) {
        if (out != null && !out.checkError() && isLoggedIn) {
            out.println(message);
            out.flush();
        }
    }

    private List<Item> createSampleItems() {
        List<Item> items = new ArrayList<>();
        String[][] sampleData = {
                {"item1", "iPhone 15 Pro Max 1TB", "Hàng chính hãng VN/A", "25000000"},
                {"item2", "iPhone 15 Pro 512GB", "Màu Titan, fullbox", "22000000"},
                {"item3", "Samsung Galaxy S24 Ultra", "S Pen, camera 200MP", "20000000"},
                {"item4", "Xiaomi 14 Ultra", "Camera Leica, pin 5000mAh", "15000000"},
                {"item5", "Google Pixel 8 Pro", "Android thuần, AI features", "18000000"},
                {"item6", "MacBook Pro M3 Max", "36GB RAM, 1TB SSD", "45000000"},
                {"item7", "Dell XPS 15", "Intel Core i9, RTX 4060", "35000000"},
                {"item8", "LG Gram 17", "Siêu nhẹ 1.35kg, pin 20 giờ", "28000000"},
                {"item9", "Asus ROG Zephyrus G14", "Gaming, AMD Ryzen 9", "32000000"},
                {"item10", "Sony WH-1000XM5", "Chống ồn tốt nhất", "5000000"},
                {"item11", "AirPods Pro 2", "Chip H2, chống ồn", "4500000"},
                {"item12", "Bose QC Ultra", "Thoải mái đeo cả ngày", "6000000"},
                {"item13", "Apple Watch Ultra 2", "Chống nước 100m", "15000000"},
                {"item14", "Samsung Galaxy Watch 6 Classic", "Xoay bezel", "7000000"},
                {"item15", "Magic Keyboard", "Bàn phím Apple", "3000000"},
                {"item16", "Logitech MX Master 3S", "Chuột cao cấp", "2000000"},
                {"item17", "iPad Pro 12.9 inch M2", "Wi-Fi + Cellular", "28000000"},
                {"item18", "Samsung Tab S9 Ultra", "Màn hình 14.6 inch", "22000000"}
        };
        for (String[] data : sampleData) {
            Item item = new Item();
            item.setId(data[0]);
            item.setName(data[1]);
            item.setDescription(data[2]);
            double price = Double.parseDouble(data[3]);
            item.setCurrentPrice(price);
            item.setStartingPrice(price);
            item.setStatus(ItemStatus.ACTIVE);
            items.add(item);
        }
        return items;
    }
}