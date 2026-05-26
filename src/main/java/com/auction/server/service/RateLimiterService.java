package com.auction.server.service;

import com.auction.common.constant.AuctionConstants;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiterService {

    private static volatile RateLimiterService instance;
    private final Map<String, Queue<Long>> requestTimestamps = new ConcurrentHashMap<>();

    private RateLimiterService() {}

    public static RateLimiterService getInstance() {
        if (instance == null) {
            synchronized (RateLimiterService.class) {
                if (instance == null) {
                    instance = new RateLimiterService();
                }
            }
        }
        return instance;
    }

    public boolean isAllowed(String userId) {
        if (userId == null) return true;

        long now = System.currentTimeMillis();
        Queue<Long> timestamps = requestTimestamps.computeIfAbsent(userId,
                k -> new LinkedList<>());

        // Xóa các timestamp cũ hơn 1 phút
        long windowStart = now - (AuctionConstants.RATE_LIMIT_WINDOW_SECONDS * 1000L);
        while (!timestamps.isEmpty() && timestamps.peek() < windowStart) {
            timestamps.poll();
        }

        if (timestamps.size() >= AuctionConstants.MAX_REQUESTS_PER_MINUTE) {
            return false;
        }

        timestamps.offer(now);
        return true;
    }

    public void removeUser(String userId) {
        requestTimestamps.remove(userId);
    }
}