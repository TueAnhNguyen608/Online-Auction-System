package server.scheduler;

import common.model.event.PriceUpdateEvent;
import common.model.item.Item;
import server.broadcast.EventBroadcaster;
import server.repository.ItemRepository;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimeBroadcastScheduler {

    private final ScheduledExecutorService scheduler;
    private final ItemRepository itemRepo;
    private final EventBroadcaster broadcaster;
    private volatile boolean running = true;

    public TimeBroadcastScheduler() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.itemRepo = ItemRepository.getInstance();
        this.broadcaster = EventBroadcaster.getInstance();
        start();
    }

    private void start() {
        // ✅ TỐI ƯU: Broadcast mỗi 2 giây thay vì 1 giây
        scheduler.scheduleAtFixedRate(() -> {
            if (!running) return;
            try {
                List<Item> activeItems = itemRepo.findActiveItems();
                for (Item item : activeItems) {
                    long remaining = item.getSecondsRemaining();

                    // Chỉ broadcast khi có thay đổi đáng kể hoặc 5 giây/lần cho time update
                    if (remaining % 5 == 0 || remaining <= 30) {
                        PriceUpdateEvent event = new PriceUpdateEvent();
                        event.setType("TIME_UPDATE");
                        event.setItemId(item.getId());
                        event.setRemainingSeconds(remaining);
                        broadcaster.broadcastToItem(item.getId(), event);
                    }
                }
            } catch (Exception e) {
                // Silent - avoid spam
            }
        }, 0, 2, TimeUnit.SECONDS); // ✅ 2 giây thay vì 1 giây
    }

    public void shutdown() {
        running = false;
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}