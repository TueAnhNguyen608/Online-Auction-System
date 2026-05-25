package com.auction.server.scheduler;

import com.auction.common.model.event.PriceUpdateEvent;
import com.auction.common.model.item.Item;
import com.auction.server.broadcast.EventBroadcaster;
import com.auction.server.repository.ItemRepository;
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
        scheduler.scheduleAtFixedRate(() -> {
            if (!running) return;
            try {
                List<Item> activeItems = itemRepo.findActiveItems();
                for (Item item : activeItems) {
                    long remaining = item.getSecondsRemaining();
                    if (remaining % 5 == 0 || remaining <= 30) {
                        PriceUpdateEvent event = new PriceUpdateEvent();
                        event.setType("TIME_UPDATE");
                        event.setItemId(item.getId());
                        event.setRemainingSeconds(remaining);
                        broadcaster.broadcastToItem(item.getId(), event);
                    }
                }
            } catch (Exception e) {
                // Silent
            }
        }, 0, 2, TimeUnit.SECONDS);
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