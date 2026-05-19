package client;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;
import com.google.gson.Gson;
import common.*;

public class AuctionClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson = new Gson();

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println("✅ Connected to server");
    }

    public ResponseMessage sendRequest(CommandType command, Object data) throws IOException {
        Request request = new Request(command, data);
        String jsonRequest = gson.toJson(request);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        return gson.fromJson(jsonResponse, ResponseMessage.class);
    }

    public ResponseMessage login(String username, String password) throws IOException {
        return sendRequest(CommandType.LOGIN, new Object[]{username, password});
    }

    public ResponseMessage register(User user) throws IOException {
        return sendRequest(CommandType.REGISTER, user);
    }

    public ResponseMessage getActiveItems() throws IOException {
        return sendRequest(CommandType.GET_ACTIVE_ITEMS, null);
    }

    public ResponseMessage createItem(Item item) throws IOException {
        return sendRequest(CommandType.CREATE_ITEM, item);
    }

    public ResponseMessage placeBid(Bid bid) throws IOException {
        return sendRequest(CommandType.PLACE_BID, bid);
    }

    public ResponseMessage getBidHistory(String itemId) throws IOException {
        return sendRequest(CommandType.GET_BID_HISTORY, itemId);
    }

    public void disconnect() throws IOException {
        sendRequest(CommandType.LOGOUT, null);
        socket.close();
    }

    // ── Các method thêm mới để hỗ trợ realtime ───────────────────

    public void startRealtimeListener(Consumer<ResponseMessage> callback) {
        if (callback == null) return;
        Thread listenerThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    ResponseMessage msg = gson.fromJson(line, ResponseMessage.class);
                    callback.accept(msg);
                }
            } catch (IOException e) {
                System.out.println("Realtime listener dừng: " + e.getMessage());
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void subscribeItem(String itemId) {
        try {
            sendRequest(CommandType.GET_BID_HISTORY, itemId);
        } catch (IOException e) {
            System.out.println("subscribeItem lỗi: " + e.getMessage());
        }
    }

    public void unsubscribeItem(String itemId) {
        // Placeholder – server chưa có lệnh unsubscribe
        System.out.println("unsubscribeItem: " + itemId);
    }
    public ResponseMessage setAutoBid(String itemId, double maxAmount) throws IOException {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("itemId", itemId);
        data.put("maxAmount", maxAmount);
        return sendRequest(CommandType.SET_AUTO_BID, data);
    }
}