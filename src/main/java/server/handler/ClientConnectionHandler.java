package server.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import common.LocalDateTimeAdapter;
import common.model.communication.CommandType;
import common.model.communication.Request;
import common.model.communication.ResponseMessage;
import common.model.user.User;
import server.repository.ItemRepository;

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
                    ResponseMessage response = handleRequest(request);

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
            try {
                socket.close();
            } catch (IOException e) {}
        }
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }

    public void pushRealtimeUpdate(String message) {
        if (out != null && !out.checkError()) {
            out.println(message);
            out.flush();
        }
    }

    public void subscribeToItem(String itemId) {
        System.out.println("📡 " + currentUsername + " subscribed to: " + itemId);
    }

    public void unsubscribeFromItem(String itemId) {
        System.out.println("📡 " + currentUsername + " unsubscribed from: " + itemId);
    }

    private ResponseMessage handleRequest(Request request) {
        CommandType cmd = request.getCommand();

        // ========== LOGIN ==========
        if (cmd == CommandType.LOGIN) {
            try {
                @SuppressWarnings("unchecked")
                List<String> data = (List<String>) request.getData();
                String username = data.get(0);
                String password = data.get(1);

                System.out.println("🔐 Login: " + username);

                if (password.equals("123456") &&
                        (username.equals("admin") ||
                                username.equals("bidder1") || username.equals("bidder2") ||
                                username.equals("bidder3") || username.equals("bidder4") ||
                                username.equals("bidder5") || username.equals("bidder6") ||
                                username.equals("bidder7") || username.equals("seller1") ||
                                username.equals("seller2"))) {

                    currentUserId = username;
                    currentUsername = username;

                    User user = new User();
                    user.setId(username);
                    user.setUsername(username);
                    user.setFullName("User " + username);
                    user.setRole("BIDDER");

                    System.out.println("✅ Login success: " + username);
                    return ResponseMessage.success("Đăng nhập thành công", user);
                }

                System.out.println("❌ Login failed: " + username);
                return ResponseMessage.error("Sai tên đăng nhập hoặc mật khẩu");

            } catch (Exception e) {
                return ResponseMessage.error("Login error: " + e.getMessage());
            }
        }

        // ========== GET ACTIVE ITEMS (LẤY TỪ DATABASE) ==========
        if (cmd == CommandType.GET_ACTIVE_ITEMS) {
            try {
                ItemRepository itemRepo = ItemRepository.getInstance();
                List<common.model.item.Item> items = itemRepo.findActiveItems();

                if (items == null || items.isEmpty()) {
                    // Fallback: tạo danh sách mẫu nếu database chưa có
                    items = createSampleItems();
                }

                System.out.println("📦 Return " + items.size() + " active items");
                return ResponseMessage.success("Lấy danh sách thành công", items);

            } catch (Exception e) {
                System.err.println("Error getting items: " + e.getMessage());
                // Fallback: trả về danh sách mẫu
                return ResponseMessage.success("Lấy danh sách thành công", createSampleItems());
            }
        }

        // ========== PLACE BID ==========
        if (cmd == CommandType.PLACE_BID) {
            System.out.println("💰 Bid placed by: " + currentUsername);
            return ResponseMessage.success("Đặt giá thành công!", null);
        }

        // ========== SUBSCRIBE ==========
        if (cmd == CommandType.SUBSCRIBE_ITEM) {
            String itemId = (String) request.getData();
            System.out.println("📡 " + currentUsername + " subscribed to: " + itemId);
            return ResponseMessage.success("Đã theo dõi sản phẩm", null);
        }

        // ========== UNSUBSCRIBE ==========
        if (cmd == CommandType.UNSUBSCRIBE_ITEM) {
            String itemId = (String) request.getData();
            System.out.println("📡 " + currentUsername + " unsubscribed from: " + itemId);
            return ResponseMessage.success("Đã hủy theo dõi sản phẩm", null);
        }

        // ========== AUTO BID ==========
        if (cmd == CommandType.SET_AUTO_BID) {
            System.out.println("🤖 " + currentUsername + " set auto-bid");
            return ResponseMessage.success("Đã cài đặt auto-bid", null);
        }

        // ========== GET BID HISTORY ==========
        if (cmd == CommandType.GET_BID_HISTORY) {
            return ResponseMessage.success("Lấy lịch sử thành công", new ArrayList<>());
        }

        // ========== GET MY BID HISTORY ==========
        if (cmd == CommandType.GET_MY_BID_HISTORY) {
            return ResponseMessage.success("Lấy lịch sử của bạn thành công", new ArrayList<>());
        }

        // ========== GET WON PRODUCTS ==========
        if (cmd == CommandType.GET_WON_PRODUCTS) {
            return ResponseMessage.success("Lấy danh sách sản phẩm đã thắng thành công", new ArrayList<>());
        }

        // ========== KEEP ALIVE ==========
        if (cmd == CommandType.KEEP_ALIVE) {
            return ResponseMessage.success("OK", null);
        }

        // ========== LOGOUT ==========
        if (cmd == CommandType.LOGOUT) {
            return ResponseMessage.success("Đăng xuất thành công", null);
        }

        return ResponseMessage.error("Không hỗ trợ command: " + cmd);
    }

    // ========== TẠO DANH SÁCH MẪU 18 SẢN PHẨM (FALLBACK) ==========
    private List<common.model.item.Item> createSampleItems() {
        List<common.model.item.Item> items = new ArrayList<>();

        String[][] sampleData = {
                {"item1", "iPhone 15 Pro Max 1TB", "Hàng chính hãng VN/A, bảo hành 12 tháng", "25000000"},
                {"item2", "iPhone 15 Pro 512GB", "Màu Titan, fullbox, phụ kiện đầy đủ", "22000000"},
                {"item3", "Samsung Galaxy S24 Ultra", "S Pen, camera 200MP, hàng chính hãng", "20000000"},
                {"item4", "Xiaomi 14 Ultra", "Camera Leica, pin 5000mAh", "15000000"},
                {"item5", "Google Pixel 8 Pro", "Android thuần, AI features", "18000000"},
                {"item6", "MacBook Pro M3 Max", "36GB RAM, 1TB SSD, chip M3 Max", "45000000"},
                {"item7", "Dell XPS 15", "Intel Core i9, RTX 4060, 32GB RAM", "35000000"},
                {"item8", "LG Gram 17", "Siêu nhẹ 1.35kg, pin 20 giờ", "28000000"},
                {"item9", "Asus ROG Zephyrus G14", "Gaming, AMD Ryzen 9, RTX 4080", "32000000"},
                {"item10", "Sony WH-1000XM5", "Chống ồn tốt nhất, pin 30 giờ", "5000000"},
                {"item11", "AirPods Pro 2", "Chip H2, chống ồn thích ứng", "4500000"},
                {"item12", "Bose QC Ultra", "Thoải mái đeo cả ngày", "6000000"},
                {"item13", "Apple Watch Ultra 2", "Chống nước 100m, GPS", "15000000"},
                {"item14", "Samsung Galaxy Watch 6 Classic", "Xoay bezel, thể thao", "7000000"},
                {"item15", "Magic Keyboard", "Bàn phím Apple chính hãng", "3000000"},
                {"item16", "Logitech MX Master 3S", "Chuột cao cấp, silent click", "2000000"},
                {"item17", "iPad Pro 12.9 inch M2", "Wi-Fi + Cellular, 512GB", "28000000"},
                {"item18", "Samsung Tab S9 Ultra", "Màn hình 14.6 inch, S Pen", "22000000"}
        };

        for (String[] data : sampleData) {
            common.model.item.Item item = new common.model.item.Item();
            item.setId(data[0]);
            item.setName(data[1]);
            item.setDescription(data[2]);
            double price = Double.parseDouble(data[3]);
            item.setCurrentPrice(price);
            item.setStartingPrice(price);
            items.add(item);
        }

        return items;
    }
}