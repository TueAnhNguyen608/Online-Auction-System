package com.auction.server.service;

import com.auction.server.repository.AutoBidRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class AutoBidService {

    private static volatile AutoBidService instance;
    private final AutoBidRepository autoBidRepo;

    private AutoBidService() {
        this.autoBidRepo = AutoBidRepository.getInstance();
    }

    public static AutoBidService getInstance() {
        if (instance == null) {
            synchronized (AutoBidService.class) {
                if (instance == null) {
                    instance = new AutoBidService();
                }
            }
        }
        return instance;
    }

    public boolean setAutoBid(String userId, String itemId, double maxAmount) throws SQLException {
        return autoBidRepo.setAutoBid(userId, itemId, maxAmount);
    }

    public boolean cancelAutoBid(String userId, String itemId) throws SQLException {
        return autoBidRepo.cancelAutoBid(userId, itemId);
    }

    public List<Map<String, Object>> getAutoBidSettings(String userId) throws SQLException {
        return autoBidRepo.findByUserId(userId);
    }
}