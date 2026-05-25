package com.auction.client;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.auction.common.LocalDateTimeAdapter;
import com.auction.common.model.bidding.Bid;
import com.auction.common.model.communication.CommandType;
import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.common.model.item.Item;
import com.auction.common.model.user.User;

public class AuctionClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean isConnected = false;
    private Thread listenerThread;
    private Consumer<ResponseMessage> callback;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        socket.setKeepAlive(true);
        socket.setSoTimeout(60000);

        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        isConnected = true;

        System.out.println("✅ Connected to server at " + host + ":" + port);
    }

    public ResponseMessage sendRequest(CommandType command, Object data) throws IOException {
        if (!isConnected) {
            throw new IOException("Not connected to server");
        }

        Request request = new Request(command, data);
        String jsonRequest = gson.toJson(request);
        System.out.println("📤 Sending: " + jsonRequest);
        out.println(jsonRequest);
        out.flush();

        String jsonResponse = in.readLine();
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            throw new IOException("Server returned empty response");
        }

        System.out.println("📥 Received: " + jsonResponse);

        try {
            ResponseMessage response = gson.fromJson(jsonResponse, ResponseMessage.class);
            if (response == null) {
                throw new IOException("Failed to parse response");
            }
            return response;
        } catch (JsonSyntaxException e) {
            System.err.println("❌ Failed to parse JSON: " + jsonResponse);
            System.err.println("   Error: " + e.getMessage());
            throw new IOException("Invalid response from server: " + e.getMessage());
        }
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

    public ResponseMessage subscribeItem(String itemId) throws IOException {
        return sendRequest(CommandType.SUBSCRIBE_ITEM, itemId);
    }

    public ResponseMessage unsubscribeItem(String itemId) throws IOException {
        return sendRequest(CommandType.UNSUBSCRIBE_ITEM, itemId);
    }

    public ResponseMessage setAutoBid(String itemId, double maxAmount) throws IOException {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("itemId", itemId);
        data.put("maxAmount", maxAmount);
        return sendRequest(CommandType.SET_AUTO_BID, data);
    }

    public ResponseMessage cancelAutoBid(String itemId) throws IOException {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("itemId", itemId);
        return sendRequest(CommandType.CANCEL_AUTO_BID, data);
    }

    public ResponseMessage getAutoBidStatus() throws IOException {
        return sendRequest(CommandType.GET_AUTO_BID_STATUS, null);
    }

    public ResponseMessage getMyBidHistory() throws IOException {
        return sendRequest(CommandType.GET_MY_BID_HISTORY, null);
    }

    public ResponseMessage getWonProducts() throws IOException {
        return sendRequest(CommandType.GET_WON_PRODUCTS, null);
    }

    public ResponseMessage changePassword(String oldPassword, String newPassword) throws IOException {
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("oldPassword", oldPassword);
        data.put("newPassword", newPassword);
        return sendRequest(CommandType.CHANGE_PASSWORD, data);
    }

    public ResponseMessage updateProfile(String email, String fullName, String phone) throws IOException {
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("email", email);
        data.put("fullName", fullName);
        data.put("phone", phone);
        return sendRequest(CommandType.UPDATE_PROFILE, data);
    }

    public void startRealtimeListener(Consumer<ResponseMessage> callback) {
        this.callback = callback;
        listenerThread = new Thread(() -> {
            try {
                String line;
                while (isConnected && (line = in.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    try {
                        ResponseMessage msg = gson.fromJson(line, ResponseMessage.class);
                        if (callback != null && msg != null) {
                            callback.accept(msg);
                        }
                    } catch (JsonSyntaxException e) {
                        System.err.println("❌ Failed to parse realtime message: " + line);
                        System.err.println("   Error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                if (isConnected) {
                    System.err.println("Realtime listener stopped: " + e.getMessage());
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
        System.out.println("✅ Realtime listener started");
    }

    public void disconnect() throws IOException {
        isConnected = false;

        try {
            sendRequest(CommandType.LOGOUT, null);
        } catch (Exception e) {
            // Ignore
        }

        if (listenerThread != null) {
            listenerThread.interrupt();
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        System.out.println("🔌 Disconnected from server");
    }

    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }
}