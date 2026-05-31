package com.auction.server.scheduler;

import com.auction.common.constant.AuctionConstants;
import com.auction.common.model.event.PriceUpdateEvent;
import com.auction.common.model.item.Item;
import com.auction.common.model.item.ItemStatus;
import com.auction.server.broadcast.EventBroadcaster;
import com.auction.server.pool.DatabaseConnectionPool;
import com.auction.server.repository.ItemRepository;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionScheduler {

    private static final Logger LOGGER = Logger.getLogger(AuctionScheduler.class.getName());

    private final ScheduledExecutorService scheduler;
    private final ItemRepository itemRepo;
    private final EventBroadcaster broadcaster;
    private final DatabaseConnectionPool dbPool;

    // Batch processing configuration
    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRY_PER_ITEM = 3;
    private final AtomicInteger totalEndedAuctions = new AtomicInteger(0);

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
                conn = dbPool.getConnection();

                // Phân trang để tránh load quá nhiều items
                int offset = 0;
                int totalProcessed = 0;
                int endedCount = 0;

                while (true) {
                    List<Item> activeItems = getActiveItemsWithPagination(conn, offset, BATCH_SIZE);
                    if (activeItems.isEmpty()) {
                        break;
                    }

                    for (Item item : activeItems) {
                        if (shouldEndAuction(item)) {
                            boolean ended = endAuctionWithRetry(item, conn);
                            if (ended) {
                                endedCount++;
                            }
                        }
                    }

                    totalProcessed += activeItems.size();
                    offset += BATCH_SIZE;

                    // Tránh query quá lớn
                    if (offset > 1000) {
                        LOGGER.warning("AuctionScheduler processed " + totalProcessed +
                                " items, stopping to avoid overload");
                        break;
                    }
                }

                if (endedCount > 0) {
                    LOGGER.info("Auction ended: " + endedCount + " auctions ended in this cycle");
                    totalEndedAuctions.addAndGet(endedCount);
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Lỗi kiểm tra đấu giá: " + e.getMessage(), e);
            } finally {
                if (conn != null) {
                    dbPool.returnConnection(conn);
                }
            }
        }, 0, AuctionConstants.AUCTION_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    // Phương thức lấy active items với phân trang
    private List<Item> getActiveItemsWithPagination(Connection conn, int offset, int limit)
            throws SQLException {
        return itemRepo.findActiveItemsWithPagination(conn, limit, offset);
    }

    // Kiểm tra có nên kết thúc auction không
    private boolean shouldEndAuction(Item item) {
        if (item.getStatus() != ItemStatus.ACTIVE) {
            return false;
        }
        if (item.getEndTime() == null) {
            return false;
        }
        return item.getEndTime().isBefore(LocalDateTime.now());
    }

    // Kết thúc auction với retry
    private boolean endAuctionWithRetry(Item item, Connection conn) {
        for (int attempt = 1; attempt <= MAX_RETRY_PER_ITEM; attempt++) {
            try {
                // Kiểm tra lại trạng thái trước khi end
                Item freshItem = itemRepo.findByIdForUpdate(item.getId(), conn).orElse(null);
                if (freshItem == null || freshItem.getStatus() != ItemStatus.ACTIVE) {
                    return false;
                }

                // Cập nhật status
                itemRepo.updateStatus(item.getId(), ItemStatus.ENDED, conn);

                // Broadcast kết thúc
                String winnerName = freshItem.getCurrentBidderName() != null ?
                        freshItem.getCurrentBidderName() : "Không có người thắng";

                PriceUpdateEvent endEvent = PriceUpdateEvent.auctionEnd(
                        freshItem.getId(),
                        freshItem.getName(),
                        freshItem.getCurrentPrice(),
                        winnerName
                );
                broadcaster.broadcastToItem(freshItem.getId(), endEvent);

                LOGGER.info("Kết thúc đấu giá: " + freshItem.getName() +
                        " - Người thắng: " + winnerName +
                        " - Giá: " + String.format("%,.0f", freshItem.getCurrentPrice()) + " VND");

                return true;

            } catch (SQLException e) {
                LOGGER.warning("Attempt " + attempt + " to end auction failed for item " +
                        item.getId() + ": " + e.getMessage());
                if (attempt == MAX_RETRY_PER_ITEM) {
                    LOGGER.severe("Failed to end auction for item " + item.getId() +
                            " after " + MAX_RETRY_PER_ITEM + " attempts");
                }
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    // Phương thức lấy thống kê
    public int getTotalEndedAuctions() {
        return totalEndedAuctions.get();
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("Total auctions ended by scheduler: " + totalEndedAuctions.get());
    }
}