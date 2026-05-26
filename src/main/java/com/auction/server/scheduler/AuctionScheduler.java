package com.auction.server.scheduler;

import com.auction.common.model.event.PriceUpdateEvent;
import com.auction.common.model.item.Item;
import com.auction.common.model.item.ItemStatus;
import com.auction.server.broadcast.EventBroadcaster;
import com.auction.server.pool.DatabaseConnectionPool;
import com.auction.server.repository.ItemRepository;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionScheduler {

    private final ScheduledExecutorService scheduler;
    private final ItemRepository itemRepo;
    private final EventBroadcaster broadcaster;
    private final DatabaseConnectionPool dbPool;

    public AuctionScheduler() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.itemRepo = ItemRepository.getInstance();
        this.broadcaster = EventBroadcaster.getInstance();
        this.dbPool = DatabaseConnectionPool.getInstance();
        start();
    }

    private void start() {
        scheduler.scheduleAtFixedRate(() -> {
            Connection conn = null;
            try {
                // 🔥 Lấy connection cho transaction
                conn = dbPool.getConnection();

                List<Item> activeItems = itemRepo.findActiveItems();
                for (Item item : activeItems) {
                    if (item.getEndTime() != null &&
                            item.getEndTime().isBefore(java.time.LocalDateTime.now())) {

                        // 🔥 SỬA: Gọi updateStatus có connection
                        itemRepo.updateStatus(item.getId(), ItemStatus.ENDED, conn);

                        String winnerName = item.getCurrentBidderName() != null ?
                                item.getCurrentBidderName() : "Không có người thắng";

                        PriceUpdateEvent endEvent = PriceUpdateEvent.auctionEnd(
                                item.getId(),
                                item.getName(),
                                item.getCurrentPrice(),
                                winnerName
                        );
                        broadcaster.broadcastToItem(item.getId(), endEvent);

                        System.out.println("🏆 Kết thúc đấu giá: " + item.getName() +
                                " - Người thắng: " + winnerName +
                                " - Giá: " + String.format("%,.0f", item.getCurrentPrice()) + " VND");
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi kiểm tra đấu giá: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    dbPool.returnConnection(conn);
                }
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}