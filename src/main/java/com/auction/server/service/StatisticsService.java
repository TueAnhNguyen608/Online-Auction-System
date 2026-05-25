package com.auction.server.service;

import com.auction.server.repository.BidRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class StatisticsService {

    private static volatile StatisticsService instance;
    private final UserRepository userRepo;
    private final ItemRepository itemRepo;
    private final BidRepository bidRepo;

    private StatisticsService() {
        this.userRepo = UserRepository.getInstance();
        this.itemRepo = ItemRepository.getInstance();
        this.bidRepo = BidRepository.getInstance();
    }

    public static StatisticsService getInstance() {
        if (instance == null) {
            synchronized (StatisticsService.class) {
                if (instance == null) {
                    instance = new StatisticsService();
                }
            }
        }
        return instance;
    }

    public Map<String, Object> getSystemStats() throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepo.count());
        stats.put("activeItems", itemRepo.countActive());
        stats.put("totalBids", bidRepo.countAll());
        stats.put("totalRevenue", 0);
        return stats;
    }
}