package server;

import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import common.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ClientHandler implements Runnable {
    private Socket socket;
    private DatabaseManager dbManager;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson;
    private String currentUserId;
    private String currentUsername;
    private Set<String> subscribedItems = ConcurrentHashMap.newKeySet();

    public ClientHandler(Socket socket, DatabaseManager dbManager) {
        this.socket = socket;
        this.dbManager = dbManager;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("📨 Received from " + currentUsername + ": " + line);

                try {
                    Request request = gson.fromJson(line, Request.class);
                    ResponseMessage response = process(request);

                    if (response != null) {
                        String jsonResponse = gson.toJson(response);
                        out.println(jsonResponse);
                    }

                    if (request.getCommand() == CommandType.LOGOUT) break;

                } catch (Exception e) {
                    e.printStackTrace();
                    ResponseMessage error = ResponseMessage.error("Lỗi xử lý: " + e.getMessage());
                    out.println(gson.toJson(error));
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Client disconnected: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private ResponseMessage process(Request request) {
        try {
            switch (request.getCommand()) {
                case LOGIN:
                    return handleLogin(request.getData());
                case REGISTER:
                    return handleRegister(request.getData());
                case GET_ACTIVE_ITEMS:
                    return handleGetActiveItems();
                case CREATE_ITEM:
                    return handleCreateItem(request.getData());
                case PLACE_BID:
                    return handlePlaceBid(request.getData());
                case GET_BID_HISTORY:
                    return handleGetBidHistory(request.getData());
                case SUBSCRIBE_ITEM:
                    return handleSubscribe(request.getData());
                case UNSUBSCRIBE_ITEM:
                    return handleUnsubscribe(request.getData());
                case SET_AUTO_BID:
                    return handleSetAutoBid(request.getData());
                case LOGOUT:
                    return ResponseMessage.success("Đăng xuất thành công", null);
                default:
                    return ResponseMessage.error("Command chưa được hỗ trợ: " + request.getCommand());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMessage.error("Lỗi server: " + e.getMessage());
        }
    }

    private ResponseMessage handleLogin(Object data) throws SQLException {
        List<?> list = (List<?>) data;
        String username = (String) list.get(0);
        String password = (String) list.get(1);

        User user = dbManager.login(username, password);
        if (user != null) {
            currentUserId = user.getId();
            currentUsername = user.getUsername();
            System.out.println("✅ User logged in: " + username);
            return ResponseMessage.success("Đăng nhập thành công", user);
        }
        return ResponseMessage.authFailed("Sai tên đăng nhập hoặc mật khẩu");
    }

    private ResponseMessage handleRegister(Object data) throws SQLException {
        User user;
        if (data instanceof com.google.gson.internal.LinkedTreeMap) {
            com.google.gson.internal.LinkedTreeMap<?, ?> map = (com.google.gson.internal.LinkedTreeMap<?, ?>) data;
            user = new User();
            user.setUsername((String) map.get("username"));
            user.setPassword((String) map.get("password"));
            user.setEmail((String) map.get("email"));
            user.setRole((String) map.get("role"));
        } else {
            user = (User) data;
        }
        boolean success = dbManager.register(user);
        if (success) {
            return ResponseMessage.success("Đăng ký thành công", null);
        }
        return ResponseMessage.error("Đăng ký thất bại");
    }

    private ResponseMessage handleGetActiveItems() throws SQLException {
        List<Item> items = dbManager.getActiveItems();
        return ResponseMessage.success("Lấy danh sách thành công", items);
    }

    private ResponseMessage handleCreateItem(Object data) throws SQLException {
        Item item;
        if (data instanceof com.google.gson.internal.LinkedTreeMap) {
            com.google.gson.internal.LinkedTreeMap<?, ?> map = (com.google.gson.internal.LinkedTreeMap<?, ?>) data;
            item = new Item();
            item.setName((String) map.get("name"));
            item.setDescription((String) map.get("description"));
            item.setStartingPrice(((Number) map.get("startingPrice")).doubleValue());
            item.setCurrentPrice(((Number) map.get("currentPrice")).doubleValue());
        } else {
            item = (Item) data;
        }
        item.setSellerId(currentUserId);
        item.setSellerName(currentUsername);
        item.setStatus("ACTIVE");
        boolean success = dbManager.createItem(item);
        if (success) {
            return ResponseMessage.success("Tạo sản phẩm thành công", item);
        }
        return ResponseMessage.error("Tạo sản phẩm thất bại");
    }

    private ResponseMessage handlePlaceBid(Object data) throws SQLException {
        Bid bid;

        if (data instanceof com.google.gson.internal.LinkedTreeMap) {
            com.google.gson.internal.LinkedTreeMap<?, ?> map = (com.google.gson.internal.LinkedTreeMap<?, ?>) data;
            bid = new Bid();
            bid.setItemId((String) map.get("itemId"));
            bid.setAmount(((Number) map.get("amount")).doubleValue());
            if (map.get("id") != null) bid.setId((String) map.get("id"));
            if (map.get("isAutoBid") != null) bid.setAutoBid((Boolean) map.get("isAutoBid"));
        } else {
            bid = (Bid) data;
        }

        bid.setBidderId(currentUserId);
        bid.setBidderName(currentUsername);
        bid.setBidTime(LocalDateTime.now());

        boolean success = dbManager.placeBidSafe(bid);
        if (success) {
            Item item = dbManager.getItemById(bid.getItemId());
            if (item != null) {
                long remainingSeconds = item.getEndTime() != null ?
                        java.time.Duration.between(LocalDateTime.now(), item.getEndTime()).getSeconds() : 0;

                PriceUpdateEvent event = new PriceUpdateEvent(
                        item.getId(), item.getName(),
                        item.getCurrentPrice(), item.getCurrentBidderName(),
                        remainingSeconds, item.getBidCount()
                );

                AuctionServer.broadcastToItem(bid.getItemId(), event);
            }
            // SỬA: Đổi message thành "Đấu giá thành công"
            return ResponseMessage.success("Đấu giá thành công!", bid);
        }
        return ResponseMessage.error("Đấu giá thất bại! Giá đấu phải cao hơn giá hiện tại");
    }

    private ResponseMessage handleGetBidHistory(Object data) throws SQLException {
        String itemId = (String) data;
        List<Bid> bids = dbManager.getBidHistory(itemId);
        return ResponseMessage.success("Lấy lịch sử thành công", bids);
    }

    private ResponseMessage handleSubscribe(Object data) {
        String itemId = (String) data;
        subscribedItems.add(itemId);
        AuctionServer.subscribe(itemId, this);
        System.out.println("📡 User " + currentUsername + " subscribed to item " + itemId);
        return ResponseMessage.success("Đã theo dõi sản phẩm", null);
    }

    private ResponseMessage handleUnsubscribe(Object data) {
        String itemId = (String) data;
        subscribedItems.remove(itemId);
        AuctionServer.unsubscribe(itemId, this);
        return ResponseMessage.success("Đã hủy theo dõi", null);
    }

    private ResponseMessage handleSetAutoBid(Object data) throws SQLException {
        Map<?, ?> map = (Map<?, ?>) data;
        String itemId = (String) map.get("itemId");
        double maxAmount = ((Number) map.get("maxAmount")).doubleValue();

        boolean success = dbManager.setAutoBid(currentUserId, itemId, maxAmount);
        if (success) {
            return ResponseMessage.success("Đã cài đặt auto-bid với giá tối đa: " + maxAmount, null);
        }
        return ResponseMessage.error("Cài đặt auto-bid thất bại");
    }

    public void pushRealtimeUpdate(PriceUpdateEvent event) {
        ResponseMessage push = ResponseMessage.success("realtime_update", event);
        out.println(gson.toJson(push));
    }

    private void cleanup() {
        try {
            for (String itemId : subscribedItems) {
                AuctionServer.unsubscribe(itemId, this);
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("🔌 Client disconnected: " + currentUsername);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}