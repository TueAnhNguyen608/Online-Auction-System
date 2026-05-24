package server.scheduler;

import common.model.event.PriceUpdateEvent;
import common.model.item.Item;
import common.model.item.ItemStatus;
import server.broadcast.EventBroadcaster;
import server.repository.ItemRepository;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionScheduler {

    private final ScheduledExecutorService scheduler;
    private final ItemRepository itemRepo;
    private final EventBroadcaster broadcaster;

    public AuctionScheduler() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.itemRepo = ItemRepository.getInstance();
        this.broadcaster = EventBroadcaster.getInstance();
        start();
    }

    private void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<Item> activeItems = itemRepo.findActiveItems();
                for (Item item : activeItems) {
                    // Kiểm tra nếu item đã hết hạn
                    if (item.getEndTime() != null &&
                            item.getEndTime().isBefore(java.time.LocalDateTime.now())) {

                        // Cập nhật status thành ENDED
                        itemRepo.updateStatus(item.getId(), ItemStatus.ENDED);

                        // ✅ Broadcast auction end event
                        String winnerName = item.getCurrentBidderName() != null ?
                                item.getCurrentBidderName() : "Không có người thắng";
                        String winnerId = item.getCurrentBidderId() != null ?
                                item.getCurrentBidderId() : "";

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