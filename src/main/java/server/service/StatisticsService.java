package server.service;

import server.repository.BidRepository;
import server.repository.ItemRepository;
import server.repository.UserRepository;
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
        stats.put("totalBids", bidRepo.countAll());  // ✅ ĐÃ SỬA
        stats.put("totalRevenue", 0); // Có thể tính sau từ các phiên đã kết thúc
        return stats;
    }
}