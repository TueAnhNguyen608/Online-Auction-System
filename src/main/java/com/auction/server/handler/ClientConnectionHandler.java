package com.auction.server.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.auction.common.LocalDateTimeAdapter;
import com.auction.common.constant.AuctionConstants;
import com.auction.common.model.communication.CommandType;
import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.common.model.user.User;
import com.auction.server.broadcast.EventBroadcaster;
import com.auction.server.repository.ItemRepository;
import com.auction.common.model.item.Item;
import com.auction.server.service.RateLimiterService;
import com.auction.server.service.SessionManagerService;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ClientConnectionHandler implements Runnable {

    private final Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String currentUserId;
    private String currentUsername;
    private boolean isLoggedIn = false;

    // 🔥 THÊM: Heartbeat tracking
    private long lastHeartbeat;
    private volatile boolean heartbeatValid = true;
    private Thread timeoutChecker;

    // 🔥 THÊM: Session manager
    private final SessionManagerService sessionManager = SessionManagerService.getInstance();

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public ClientConnectionHandler(Socket socket) {
        this.socket = socket;
        this.lastHeartbeat = System.currentTimeMillis();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // 🔥 THÊM: Start timeout checker
            startTimeoutChecker();

            String line;
            while ((line = in.readLine()) != null && heartbeatValid && isLoggedIn) {
                if (line.trim().isEmpty()) continue;

                System.out.println("📨 Received: " + line);

                // 🔥 THÊM: Update heartbeat
                lastHeartbeat = System.currentTimeMillis();

                try {
                    Request request = gson.fromJson(line, Request.class);

                    // 🔥 THÊM: Rate limiting check
                    if (currentUserId != null &&
                            !RateLimiterService.getInstance().isAllowed(currentUserId)) {
                        String errorJson = gson.toJson(
                                ResponseMessage.error(AuctionConstants.MSG_RATE_LIMIT));
                        out.println(errorJson);
                        out.flush();
                        continue;
                    }

                    // 🔥 THÊM: Session timeout check
                    if (currentUserId != null && !sessionManager.isSessionValid(currentUserId)) {
                        String errorJson = gson.toJson(
                                ResponseMessage.error(AuctionConstants.MSG_SESSION_EXPIRED));
                        out.println(errorJson);
                        out.flush();
                        break;
                    }

                    // 🔥 THÊM: Update session activity
                    if (currentUserId != null) {
                        sessionManager.updateActivity(currentUserId);
                    }

                    RequestDispatcher dispatcher = RequestDispatcher.getInstance();
                    ResponseMessage response = dispatcher.dispatch(request, this);

                    String jsonResponse = gson.toJson(response);
                    System.out.println("📤 Sending: " + jsonResponse);
                    out.println(jsonResponse);
                    out.flush();

                    if (request.getCommand() == CommandType.LOGOUT) {
                        if (currentUserId != null) {
                            sessionManager.removeSession(currentUserId);
                        }
                        EventBroadcaster.getInstance().cleanupClient(this);
                        System.out.println("📡 User " + currentUsername + " logged out");
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
            if (currentUserId != null) {
                sessionManager.removeSession(currentUserId);
            }
            EventBroadcaster.getInstance().cleanupClient(this);
            try { socket.close(); } catch (IOException e) {}
        }
    }

    // 🔥 THÊM: Timeout checker thread
    private void startTimeoutChecker() {
        timeoutChecker = new Thread(() -> {
            while (isLoggedIn && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(10000); // Kiểm tra mỗi 10 giây
                    long idleTime = System.currentTimeMillis() - lastHeartbeat;
                    if (idleTime > AuctionConstants.HEARTBEAT_TIMEOUT_SECONDS * 1000L) {
                        System.out.println("⚠️ Client " + currentUsername + " heartbeat timeout!");
                        heartbeatValid = false;
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Ignore
                        }
                        break;
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        timeoutChecker.setDaemon(true);
        timeoutChecker.start();
    }

    // 🔥 THÊM: Update heartbeat từ KEEP_ALIVE
    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public String getCurrentUserId() { return currentUserId; }
    public String getCurrentUsername() { return currentUsername; }
    public boolean isLoggedIn() { return isLoggedIn; }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
        this.isLoggedIn = userId != null;
        if (userId != null) {
            sessionManager.createSession(userId);
        }
    }
    public void setCurrentUsername(String username) { this.currentUsername = username; }

    public void pushRealtimeUpdate(String message) {
        if (out != null && !out.checkError() && isLoggedIn && heartbeatValid) {
            out.println(message);
            out.flush();
        }
    }
}