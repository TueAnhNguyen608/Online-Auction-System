package com.auction.server.service;

import com.auction.common.constant.AuctionConstants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionManagerService {

    private static volatile SessionManagerService instance;
    private final Map<String, Long> sessionLastActive = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner;

    private SessionManagerService() {
        cleaner = Executors.newSingleThreadScheduledExecutor();
        cleaner.scheduleAtFixedRate(this::cleanExpiredSessions, 60, 60, TimeUnit.SECONDS);
    }

    public static SessionManagerService getInstance() {
        if (instance == null) {
            synchronized (SessionManagerService.class) {
                if (instance == null) {
                    instance = new SessionManagerService();
                }
            }
        }
        return instance;
    }

    public void createSession(String userId) {
        sessionLastActive.put(userId, System.currentTimeMillis());
    }

    public void updateActivity(String userId) {
        if (userId != null) {
            sessionLastActive.put(userId, System.currentTimeMillis());
        }
    }

    public boolean isSessionValid(String userId) {
        Long lastActive = sessionLastActive.get(userId);
        if (lastActive == null) return false;
        long timeoutMs = AuctionConstants.SESSION_TIMEOUT_MINUTES * 60L * 1000L;
        return System.currentTimeMillis() - lastActive < timeoutMs;
    }

    public void removeSession(String userId) {
        sessionLastActive.remove(userId);
    }

    private void cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        long timeoutMs = AuctionConstants.SESSION_TIMEOUT_MINUTES * 60L * 1000L;
        sessionLastActive.entrySet().removeIf(entry ->
                now - entry.getValue() > timeoutMs
        );
    }

    public void stop() {
        cleaner.shutdown();
    }
}