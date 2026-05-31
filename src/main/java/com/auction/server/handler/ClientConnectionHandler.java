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

    // Heartbeat tracking
    private long lastHeartbeat;
    private volatile boolean heartbeatValid = true;
    private Thread timeoutChecker;

    // Session manager
    private final SessionManagerService sessionManager = SessionManagerService.getInstance();

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public ClientConnectionHandler(Socket socket) {
        this.socket = socket;
        this.lastHeartbeat = System.currentTimeMillis();
        System.out.println("🔌 ClientConnectionHandler created for socket: " + socket.getRemoteSocketAddress());
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("✅ Streams initialized for client");

            // Start timeout checker
            startTimeoutChecker();

            String line;
            while ((line = in.readLine()) != null && heartbeatValid) {
                if (line.trim().isEmpty()) continue;

                System.out.println("📨 [RAW] Received: " + line);

                // Update heartbeat
                lastHeartbeat = System.currentTimeMillis();

                try {
                    Request request = gson.fromJson(line, Request.class);
                    System.out.println("📨 [PARSED] Command: " + request.getCommand() + ", Data: " + request.getData());

                    // Rate limiting check (chỉ cho logged in users)
                    if (currentUserId != null && request.getCommand() != CommandType.LOGIN &&
                            !RateLimiterService.getInstance().isAllowed(currentUserId)) {
                        String errorJson = gson.toJson(
                                ResponseMessage.error(AuctionConstants.MSG_RATE_LIMIT));
                        out.println(errorJson);
                        out.flush();
                        System.out.println("🚫 Rate limit exceeded for user: " + currentUsername);
                        continue;
                    }

                    // Session timeout check (chỉ cho logged in users)
                    if (currentUserId != null && request.getCommand() != CommandType.LOGIN &&
                            !sessionManager.isSessionValid(currentUserId)) {
                        String errorJson = gson.toJson(
                                ResponseMessage.error(AuctionConstants.MSG_SESSION_EXPIRED));
                        out.println(errorJson);
                        out.flush();
                        System.out.println("⏰ Session expired for user: " + currentUsername);
                        break;
                    }

                    // Update session activity
                    if (currentUserId != null) {
                        sessionManager.updateActivity(currentUserId);
                    }

                    RequestDispatcher dispatcher = RequestDispatcher.getInstance();
                    ResponseMessage response = dispatcher.dispatch(request, this);

                    String jsonResponse = gson.toJson(response);
                    System.out.println("📤 Sending response: " + jsonResponse);
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
                    System.err.println("❌ Error processing request: " + e.getMessage());
                    e.printStackTrace();
                    String errorJson = gson.toJson(ResponseMessage.error("Lỗi xử lý: " + e.getMessage()));
                    out.println(errorJson);
                    out.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Client disconnected (IO Error): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("🧹 Cleaning up client: " + (currentUsername != null ? currentUsername : "unknown"));
            if (currentUserId != null) {
                sessionManager.removeSession(currentUserId);
            }
            EventBroadcaster.getInstance().cleanupClient(this);
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
            System.out.println("✅ Client cleanup completed");
        }
    }

    // Timeout checker thread
    private void startTimeoutChecker() {
        timeoutChecker = new Thread(() -> {
            while (isLoggedIn && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(10000); // Kiểm tra mỗi 10 giây
                    long idleTime = System.currentTimeMillis() - lastHeartbeat;
                    if (idleTime > AuctionConstants.HEARTBEAT_TIMEOUT_SECONDS * 1000L) {
                        System.out.println("⚠️ Client " + currentUsername + " heartbeat timeout! Idle: " + idleTime + "ms");
                        heartbeatValid = false;
                        try {
                            if (socket != null && !socket.isClosed()) {
                                socket.close();
                            }
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

    // Update heartbeat từ KEEP_ALIVE
    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
        System.out.println("💓 Heartbeat received from: " + currentUsername);
    }

    public String getCurrentUserId() { return currentUserId; }
    public String getCurrentUsername() { return currentUsername; }
    public boolean isLoggedIn() { return isLoggedIn; }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
        this.isLoggedIn = userId != null;
        if (userId != null) {
            sessionManager.createSession(userId);
            System.out.println("✅ User logged in: " + currentUsername + " (ID: " + userId + ")");
        }
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
        System.out.println("👤 Set username: " + username);
    }

    public void pushRealtimeUpdate(String message) {
        if (out != null && !out.checkError() && isLoggedIn && heartbeatValid) {
            System.out.println("📢 Pushing realtime update to: " + currentUsername);
            out.println(message);
            out.flush();
        }
    }
}