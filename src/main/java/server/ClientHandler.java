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
                    ResponseMessage response = processRequest(request);
                    if (response != null) {
                        out.println(gson.toJson(response));
                    }
                    if (request.getCommand() == CommandType.LOGOUT) break;
                } catch (Exception e) {
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

    private ResponseMessage processRequest(Request request) {
        try {
            switch (request.getCommand()) {
                case LOGIN:
                    return handleLogin(request.getData());
                case REGISTER:
                    return handleRegister(request.getData());
                case GET_ACTIVE_ITEMS:
                    return ResponseMessage.success("Lấy danh sách thành công", dbManager.getActiveItems());
                case CREATE_ITEM:
                    return handleCreateItem(request.getData());
                case PLACE_BID:
                    return handlePlaceBid(request.getData());
                case GET_BID_HISTORY:
                    return ResponseMessage.success("Lấy lịch sử thành công", dbManager.getBidHistory((String) request.getData()));
                case SUBSCRIBE_ITEM:
                    return handleSubscribe(request.getData());
                case UNSUBSCRIBE_ITEM:
                    return handleUnsubscribe(request.getData());
                case SET_AUTO_BID:
                    return handleSetAutoBid(request.getData());
                // Huyền đã thêm: xử lý hủy auto-bid
                case CANCEL_AUTO_BID:
                    return handleCancelAutoBid(request.getData());
                // Huyền đã thêm: lấy danh sách auto-bid của user
                case GET_AUTO_BID_STATUS:
                    return handleGetAutoBidStatus();
                case LOGOUT:
                    return ResponseMessage.success("Đăng xuất thành công", null);

                // =========================================================
                // CODE BỔ SUNG CHO NHÓM 2 (Profile)
                // =========================================================
                case CHANGE_PASSWORD:
                    return handleChangePassword(request.getData());
                case UPDATE_USER_INFO:
                    return handleUpdateUserInfo(request.getData());
                // Huyền đã thêm
                case UPDATE_PROFILE:
                    return handleUpdateProfile(request.getData());
                // =========================================================
                // CODE BỔ SUNG CHO NHÓM 4 (Lịch sử & Thống kê)
                // =========================================================
                case GET_MY_BID_HISTORY:
                    return handleGetMyBidHistory();
                case GET_WON_PRODUCTS:
                    return handleGetWonProducts();
                case GET_PRICE_HISTORY:
                    return handleGetPriceHistory(request.getData());

                default:
                    return ResponseMessage.error("Command chưa được hỗ trợ");
            }
        } catch (InvalidBidException e) {
            return new ResponseMessage(ResponseCode.BID_TOO_LOW, e.getMessage(), null);
        } catch (AuctionClosedException e) {
            return new ResponseMessage(ResponseCode.AUCTION_ENDED, e.getMessage(), null);
        } catch (AuthenticationException e) {
            return ResponseMessage.authFailed(e.getMessage());
        } catch (DataValidationException e) {
            return new ResponseMessage(ResponseCode.INVALID_REQUEST, e.getFormattedErrors(), null);
        } catch (SQLException e) {
            return ResponseMessage.error("Lỗi database: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMessage.error("Lỗi hệ thống: " + e.getMessage());
        }
    }

    private ResponseMessage handleLogin(Object data) throws SQLException, AuthenticationException {
        List<?> list = (List<?>) data;
        String username = (String) list.get(0);
        String password = (String) list.get(1);

        if (username == null || username.trim().isEmpty()) {
            throw new AuthenticationException("Tên đăng nhập không được để trống");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new AuthenticationException("Mật khẩu không được để trống");
        }

        User user = dbManager.login(username, password);
        currentUserId = user.getId();
        currentUsername = user.getUsername();
        System.out.println("✅ User logged in: " + username);
        return ResponseMessage.success("Đăng nhập thành công", user);
    }

    @SuppressWarnings("unchecked")
    private ResponseMessage handleRegister(Object data) throws SQLException {
        User user;
        if (data instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) data;
            user = new User();
            user.setUsername((String) map.get("username"));
            user.setPassword((String) map.get("password"));
            user.setEmail((String) map.get("email"));
            user.setRole((String) map.get("role"));
        } else {
            user = (User) data;
        }

        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return ResponseMessage.error("Tên đăng nhập không được để trống");
        }
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            return ResponseMessage.error("Mật khẩu phải có ít nhất 6 ký tự");
        }

        boolean success = dbManager.register(user);
        if (success) {
            return ResponseMessage.success("Đăng ký thành công", null);
        }
        return ResponseMessage.error("Đăng ký thất bại - Username đã tồn tại");
    }

    @SuppressWarnings("unchecked")
    private ResponseMessage handleCreateItem(Object data) throws SQLException, DataValidationException {
        Item item;
        if (data instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) data;
            item = new Item();
            item.setName((String) map.get("name"));
            item.setDescription((String) map.get("description"));
            item.setStartingPrice(((Number) map.get("startingPrice")).doubleValue());
            item.setCurrentPrice(item.getStartingPrice());
            item.setStartTime(LocalDateTime.now());
            if (map.get("endTime") instanceof String) {
                item.setEndTime(LocalDateTime.parse((String) map.get("endTime")));
            } else {
                item.setEndTime(LocalDateTime.now().plusHours(24));
            }
        } else {
            item = (Item) data;
        }

        item.setSellerId(currentUserId);
        item.setSellerName(currentUsername);

        boolean success = dbManager.createItem(item);
        if (success) {
            return ResponseMessage.success("Tạo sản phẩm thành công", item);
        }
        return ResponseMessage.error("Tạo sản phẩm thất bại");
    }

    @SuppressWarnings("unchecked")
    private ResponseMessage handlePlaceBid(Object data) throws SQLException, InvalidBidException, AuctionClosedException {
        Bid bid;
        if (data instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) data;
            bid = new Bid();
            bid.setItemId((String) map.get("itemId"));
            bid.setAmount(((Number) map.get("amount")).doubleValue());
        } else {
            bid = (Bid) data;
        }

        if (bid.getAmount() <= 0) {
            throw new InvalidBidException("Số tiền đặt giá phải lớn hơn 0");
        }

        bid.setBidderId(currentUserId);
        bid.setBidderName(currentUsername);
        bid.setBidTime(LocalDateTime.now());

        dbManager.placeBidWithValidation(bid);

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

        return ResponseMessage.success("Đấu giá thành công!", bid);
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

    @SuppressWarnings("unchecked")
    private ResponseMessage handleSetAutoBid(Object data) throws SQLException {
        Map<String, Object> map = (Map<String, Object>) data;
        String itemId = (String) map.get("itemId");
        double maxAmount = ((Number) map.get("maxAmount")).doubleValue();

        boolean success = dbManager.setAutoBid(currentUserId, itemId, maxAmount);
        if (success) {
            return ResponseMessage.success("Đã cài đặt auto-bid với giá tối đa: " + maxAmount, null);
        }
        return ResponseMessage.error("Cài đặt auto-bid thất bại");
    }

    // Huyền đã thêm: handler hủy auto-bid
    @SuppressWarnings("unchecked")
    private ResponseMessage handleCancelAutoBid(Object data) throws SQLException {
        Map<String, Object> map = (Map<String, Object>) data;
        String itemId = (String) map.get("itemId");
        boolean success = dbManager.cancelAutoBid(currentUserId, itemId);
        if (success) {
            return ResponseMessage.success("Đã hủy auto-bid thành công", null);
        }
        return ResponseMessage.error("Hủy auto-bid thất bại hoặc không tìm thấy");
    }

    // =========================================================================
    // CÁC HÀM XỬ LÝ MỚI CHO NHÓM 2 & 4 (ĐÃ TRIỂN KHAI)
    // =========================================================================

    private ResponseMessage handleChangePassword(Object data) throws SQLException {
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) data;
        String oldPass = map.get("oldPassword");
        String newPass = map.get("newPassword");
        if (oldPass == null || newPass == null || newPass.length() < 6) {
            return ResponseMessage.error("Mật khẩu mới phải có ít nhất 6 ký tự");
        }
        boolean ok = dbManager.changePassword(currentUserId, oldPass, newPass);
        return ok ? ResponseMessage.success("Đổi mật khẩu thành công", null)
                : ResponseMessage.error("Mật khẩu cũ không đúng");
    }

    private ResponseMessage handleUpdateUserInfo(Object data) throws SQLException {
        User user;
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) data;
            user = new User();
            user.setId((String) map.get("id"));
            user.setEmail((String) map.get("email"));
        } else {
            user = (User) data;
        }
        user.setId(currentUserId); // đảm bảo đúng user đang đăng nhập
        boolean ok = dbManager.updateUser(user);
        return ok ? ResponseMessage.success("Cập nhật thông tin thành công", null)
                : ResponseMessage.error("Không thể cập nhật thông tin");
    }
    // Huyền đã thêm
    @SuppressWarnings("unchecked")
    private ResponseMessage handleUpdateProfile(Object data) throws SQLException {
        Map<String, Object> map = (Map<String, Object>) data;
        String email    = (String) map.get("email");
        String fullName = (String) map.get("fullName");
        String phone    = (String) map.get("phone");
        boolean ok = dbManager.updateUserProfile(currentUserId, email, fullName, phone);
        return ok ? ResponseMessage.success("Cập nhật thành công", null)
                : ResponseMessage.error("Cập nhật thất bại");
    }

    private ResponseMessage handleGetMyBidHistory() throws SQLException {
        List<Bid> history = dbManager.getMyBidHistory(currentUserId);
        return ResponseMessage.success("Lấy lịch sử thành công", history);
    }

    private ResponseMessage handleGetWonProducts() throws SQLException {
        List<Item> wonItems = dbManager.getWonProducts(currentUserId);
        return ResponseMessage.success("Lấy sản phẩm đã thắng thành công", wonItems);
    }

    private ResponseMessage handleGetPriceHistory(Object data) throws SQLException {
        String itemId = (String) data;
        List<PricePoint> priceHistory = dbManager.getPriceHistory(itemId);
        return ResponseMessage.success("Lấy lịch sử giá thành công", priceHistory);
    }

    // =========================================================================

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
    // Huyền đã thêm: handler lấy danh sách auto-bid
    private ResponseMessage handleGetAutoBidStatus() throws SQLException {
        List<Map<String, Object>> settings = dbManager.getAutoBidSettings(currentUserId);
        return ResponseMessage.success("Lấy danh sách auto-bid thành công", settings);
    }
}