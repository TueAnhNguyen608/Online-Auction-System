package com.auction.server.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionCleanupService {

    private final ScheduledExecutorService executor;
    private final SessionManagerService sessionManager;

    public SessionCleanupService() {
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.sessionManager = SessionManagerService.getInstance();
    }

    public void start() {
        executor.scheduleAtFixedRate(() -> {
            System.out.println("🧹 Session cleanup running...");
            // SessionManager tự cleanup, chỉ log
        }, 5, 5, TimeUnit.MINUTES);
    }

    public void stop() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        sessionManager.stop();
    }
}