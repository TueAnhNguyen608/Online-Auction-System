package com.auction.server.service;

import com.auction.common.constant.AuctionConstants;
import com.auction.common.model.bidding.Bid;
import com.auction.common.model.bidding.BidResult;
import com.auction.common.model.bidding.PricePoint;
import com.auction.common.model.event.PriceUpdateEvent;
import com.auction.common.model.exception.AuctionClosedException;
import com.auction.common.model.exception.InvalidBidException;
import com.auction.common.model.item.Item;
import com.auction.common.model.item.ItemStatus;
import com.auction.server.broadcast.EventBroadcaster;
import com.auction.server.pool.DatabaseConnectionPool;
import com.auction.server.repository.AutoBidRepository;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.ItemRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class BiddingService {

    private static final double MAX_BID_AMOUNT = 1_000_000_000_000L;
    private static volatile BiddingService instance;
    private final ItemRepository itemRepo;
    private final BidRepository bidRepo;
    private final AutoBidRepository autoBidRepo;
    private final EventBroadcaster broadcaster;
    private final DatabaseConnectionPool dbPool;
    private final ConcurrentHashMap<String, ReentrantLock> itemLocks = new ConcurrentHashMap<>();

    // 🔥 THÊM: Scheduled cleanup cho itemLocks
    private final ScheduledExecutorService lockCleanupExecutor;
    private static final long LOCK_CLEANUP_INTERVAL_MINUTES = 30;
    private static final long LOCK_IDLE_TIMEOUT_MINUTES = 60;

    private BiddingService() {
        this.itemRepo = ItemRepository.getInstance();
        this.bidRepo = BidRepository.getInstance();
        this.autoBidRepo = AutoBidRepository.getInstance();
        this.broadcaster = EventBroadcaster.getInstance();
        this.dbPool = DatabaseConnectionPool.getInstance();

        // 🔥 THÊM: Khởi tạo scheduler cleanup lock
        this.lockCleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        startLockCleanup();
    }

    public static BiddingService getInstance() {
        if (instance == null) {
            synchronized (BiddingService.class) {
                if (instance == null) {
                    instance = new BiddingService();
                }
            }
        }
        return instance;
    }

    // 🔥 THÊM: Cleanup lock cũ không còn dùng
    private void startLockCleanup() {
        lockCleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                int cleanedCount = 0;
                for (Map.Entry<String, ReentrantLock> entry : itemLocks.entrySet()) {
                    ReentrantLock lock = entry.getValue();
                    // Chỉ cleanup lock không bị giữ và không có thread đang chờ
                    if (!lock.isLocked() && !lock.hasQueuedThreads()) {
                        // Kiểm tra item có còn active không
                        try {
                            Optional<Item> item = itemRepo.findById(entry.getKey());
                            if (item.isEmpty() || item.get().getStatus() == ItemStatus.ENDED) {
                                itemLocks.remove(entry.getKey(), lock);
                                cleanedCount++;
                            }
                        } catch (SQLException e) {
                            System.err.println("Error checking item for lock cleanup: " + e.getMessage());
                        }
                    }
                }
                if (cleanedCount > 0) {
                    System.out.println("🧹 Cleaned up " + cleanedCount + " idle locks. Remaining: " +
                            itemLocks.size());
                }
            } catch (Exception e) {
                System.err.println("Error in lock cleanup: " + e.getMessage());
            }
        }, LOCK_CLEANUP_INTERVAL_MINUTES, LOCK_CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    // 🔥 THÊM: Phương thức lấy lock với timeout để tránh deadlock
    private boolean tryLockWithTimeout(ReentrantLock lock, String itemId, long timeoutMillis)
            throws InterruptedException {
        if (lock.tryLock(timeoutMillis, TimeUnit.MILLISECONDS)) {
            return true;
        }
        System.err.println("⚠️ Timeout acquiring lock for item: " + itemId);
        return false;
    }

    // 🔥 THÊM: Helper method tính step price
    private double calculateStepPrice(double currentPrice) {
        double stepPrice = currentPrice * AuctionConstants.DEFAULT_STEP_PERCENT;
        if (stepPrice < AuctionConstants.MINIMUM_BID_AMOUNT) {
            stepPrice = AuctionConstants.MINIMUM_BID_AMOUNT;
        }
        return stepPrice;
    }

    // 🔥 SỬA: placeBid với timeout và better error handling
    public BidResult placeBid(Bid bid) throws Exception {
        // Validation cơ bản
        if (bid.getAmount() > MAX_BID_AMOUNT) {
            throw new InvalidBidException("Giá đặt không được vượt quá " +
                    String.format("%,.0f", MAX_BID_AMOUNT) + " VND");
        }
        if (bid.getAmount() <= 0) {
            throw new InvalidBidException("Giá đặt phải lớn hơn 0");
        }

        System.out.println("💰 [placeBid] Received: itemId=" + bid.getItemId() +
                ", amount=" + bid.getAmount() +
                ", bidderId=" + bid.getBidderId() +
                ", bidderName=" + bid.getBidderName());

        ReentrantLock lock = itemLocks.computeIfAbsent(bid.getItemId(),
                k -> new ReentrantLock(true)); // 🔥 THÊM: fair lock

        // 🔥 SỬA: Lock với timeout 10 giây
        if (!tryLockWithTimeout(lock, bid.getItemId(), 10000)) {
            throw new Exception("Hệ thống đang bận, vui lòng thử lại sau");
        }

        Connection conn = null;

        try {
            conn = dbPool.getConnection();
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

            // Lấy item với FOR UPDATE (lock row)
            Optional<Item> itemOpt = itemRepo.findByIdForUpdate(bid.getItemId(), conn);
            if (itemOpt.isEmpty()) {
                System.err.println("❌ [placeBid] Item not found: " + bid.getItemId());
                throw new InvalidBidException("Sản phẩm không tồn tại");
            }

            Item item = itemOpt.get();
            System.out.println("📦 [placeBid] Item found: " + item.getName() +
                    ", currentPrice=" + item.getCurrentPrice() +
                    ", currentBidder=" + item.getCurrentBidderName());

            if (item.getStatus() == ItemStatus.ENDED) {
                throw new AuctionClosedException("Phiên đấu giá đã kết thúc", bid.getItemId());
            }

            if (item.getEndTime().isBefore(LocalDateTime.now())) {
                itemRepo.updateStatus(bid.getItemId(), ItemStatus.ENDED, conn);
                conn.commit();
                throw new AuctionClosedException("Thời gian đấu giá đã kết thúc", bid.getItemId());
            }

            // 🔥 ĐÃ CÓ: Check current winner không thể tự outbid
            if (bid.getBidderId().equals(item.getCurrentBidderId()) && item.getCurrentBidderId() != null) {
                System.err.println("❌ [placeBid] Same bidder: " + bid.getBidderId());
                throw new InvalidBidException("Bạn đang là người trả giá cao nhất, không thể tự ra giá cao hơn");
            }

            double stepPrice = calculateStepPrice(item.getCurrentPrice());
            double minBid = item.getCurrentPrice() + stepPrice;
            System.out.println("📊 [placeBid] CurrentPrice=" + item.getCurrentPrice() +
                    ", stepPrice=" + stepPrice +
                    ", minBid=" + minBid +
                    ", bidAmount=" + bid.getAmount());

            if (bid.getAmount() < minBid) {
                throw new InvalidBidException(
                        String.format("Giá đặt phải cao hơn %,.0f VND (bước giá tối thiểu)", minBid),
                        bid.getItemId(), bid.getAmount(), minBid);
            }

            long secondsLeft = item.getSecondsRemaining();
            boolean extended = false;

            // Anti-sniping
            if (secondsLeft <= AuctionConstants.SNIPING_WINDOW_SECONDS && secondsLeft > 0) {
                itemRepo.extendEndTime(bid.getItemId(), AuctionConstants.EXTENSION_SECONDS, conn);
                extended = true;
                System.out.println("⏰ Anti-Sniping: Kéo dài phiên đấu giá thêm " +
                        AuctionConstants.EXTENSION_SECONDS + " giây");
            }

            bid.setItemName(item.getName());
            bid.setBidTime(LocalDateTime.now());

            bidRepo.save(bid, conn);
            itemRepo.updateCurrentPrice(bid.getItemId(), bid.getAmount(),
                    bid.getBidderId(), bid.getBidderName(), conn);

            conn.commit();

            // Broadcast sau khi commit
            item = itemRepo.findById(bid.getItemId()).orElse(item);
            long newRemaining = Math.max(0, item.getSecondsRemaining());
            PriceUpdateEvent event = new PriceUpdateEvent(
                    item.getId(), item.getName(), bid.getAmount(),
                    bid.getBidderName(), newRemaining, item.getBidCount() + 1
            );
            broadcaster.broadcastToItem(bid.getItemId(), event);

            System.out.println("✅ Đặt giá thành công: " + bid.getBidderName() +
                    " - " + String.format("%,.0f", bid.getAmount()) + " VND");

            // 🔥 SỬA: Trigger auto-bids trong thread riêng để không block response
            final String finalItemId = bid.getItemId();
            CompletableFuture.runAsync(() -> {
                try {
                    triggerAutoBids(finalItemId);
                } catch (Exception e) {
                    System.err.println("Auto-bid trigger error: " + e.getMessage());
                }
            });

            return new BidResult(bid, extended);

        } catch (Exception e) {
            System.err.println("❌ [placeBid] Error: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    if (!conn.getAutoCommit()) {
                        conn.rollback();
                        conn.setAutoCommit(true);
                    }
                } catch (SQLException re) {
                    System.err.println("Rollback failed: " + re.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println("Error resetting auto-commit: " + e.getMessage());
                }
                dbPool.returnConnection(conn);
            }
            lock.unlock();
        }
    }

    // 🔥 SỬA: triggerAutoBids với vòng lặp có giới hạn (đã có maxIterations)
    private void triggerAutoBids(String itemId) {
        int maxIterations = 50;
        int iteration = 0;

        while (iteration++ < maxIterations) {
            Connection conn = null;
            try {
                conn = dbPool.getConnection();
                conn.setAutoCommit(false);
                conn.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

                Optional<Item> itemOpt = itemRepo.findByIdForUpdate(itemId, conn);
                if (itemOpt.isEmpty()) {
                    if (conn != null && !conn.getAutoCommit()) {
                        conn.rollback();
                        conn.setAutoCommit(true);
                    }
                    break;
                }

                Item item = itemOpt.get();
                if (item.getStatus() == ItemStatus.ENDED) {
                    if (conn != null && !conn.getAutoCommit()) {
                        conn.rollback();
                        conn.setAutoCommit(true);
                    }
                    break;
                }

                List<Map<String, Object>> autoBids = autoBidRepo.findByItemId(itemId, conn);
                if (autoBids.isEmpty()) {
                    if (conn != null && !conn.getAutoCommit()) {
                        conn.commit();
                        conn.setAutoCommit(true);
                    }
                    break;
                }

                String currentWinnerId = item.getCurrentBidderId();
                double currentPrice = item.getCurrentPrice();

                double stepPrice = calculateStepPrice(currentPrice);

                Map<String, Object> bestAutoBid = null;
                double bestMaxAmount = 0;

                for (Map<String, Object> setting : autoBids) {
                    String userId = (String) setting.get("user_id");
                    if (userId.equals(currentWinnerId)) continue;

                    double maxAmount = (double) setting.get("max_amount");
                    if (maxAmount > currentPrice && maxAmount > bestMaxAmount) {
                        bestMaxAmount = maxAmount;
                        bestAutoBid = setting;
                    }
                }

                if (bestAutoBid == null) {
                    if (conn != null && !conn.getAutoCommit()) {
                        conn.commit();
                        conn.setAutoCommit(true);
                    }
                    break;
                }

                String nextBidderId = (String) bestAutoBid.get("user_id");
                String nextBidderName = (String) bestAutoBid.get("username");
                double maxAmount = (double) bestAutoBid.get("max_amount");

                double nextPrice = currentPrice + stepPrice;
                double bidAmount = Math.min(nextPrice, maxAmount);

                if (bidAmount <= currentPrice) {
                    if (conn != null && !conn.getAutoCommit()) {
                        conn.commit();
                        conn.setAutoCommit(true);
                    }
                    break;
                }

                System.out.println("🤖 Auto-bid kích hoạt: " + nextBidderName +
                        " đặt " + String.format("%,.0f", bidAmount) + " VND");

                Bid autoBid = new Bid();
                autoBid.setItemId(itemId);
                autoBid.setAmount(bidAmount);
                autoBid.setBidderId(nextBidderId);
                autoBid.setBidderName(nextBidderName);
                autoBid.setAutoBid(true);
                autoBid.setBidTime(LocalDateTime.now());
                autoBid.setItemName(item.getName());

                bidRepo.save(autoBid, conn);
                itemRepo.updateCurrentPrice(itemId, bidAmount, nextBidderId, nextBidderName, conn);

                conn.commit();
                conn.setAutoCommit(true);

                // Broadcast
                item = itemRepo.findById(itemId).orElse(item);
                PriceUpdateEvent event = new PriceUpdateEvent(
                        item.getId(), item.getName(), bidAmount,
                        nextBidderName, item.getSecondsRemaining(), item.getBidCount()
                );
                broadcaster.broadcastToItem(itemId, event);

            } catch (Exception e) {
                if (conn != null) {
                    try {
                        if (!conn.getAutoCommit()) {
                            conn.rollback();
                            conn.setAutoCommit(true);
                        }
                    } catch (SQLException re) {
                        System.err.println("Auto-bid rollback failed: " + re.getMessage());
                    }
                }
                System.err.println("Auto-bid error: " + e.getMessage());
                break;
            } finally {
                if (conn != null) {
                    dbPool.returnConnection(conn);
                }
            }
        }

        if (iteration >= maxIterations) {
            System.err.println("⚠️ Auto-bid vượt quá số lần lặp cho item: " + itemId);
        }
    }

    public List<Bid> getBidHistory(String itemId) throws SQLException {
        return bidRepo.findByItemId(itemId);
    }

    public List<Bid> getMyBidHistory(String userId) throws SQLException {
        return bidRepo.findByBidderId(userId);
    }

    public List<Item> getWonProducts(String userId) throws SQLException {
        if (userId == null || userId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Item> allItems = itemRepo.findAll();
        return allItems.stream()
                .filter(item -> item.getStatus() == ItemStatus.ENDED)
                .filter(item -> userId.equals(item.getCurrentBidderId()))
                .sorted((a, b) -> b.getEndTime().compareTo(a.getEndTime()))
                .collect(Collectors.toList());
    }

    public List<PricePoint> getPriceHistory(String itemId) throws SQLException {
        List<PricePoint> points = new ArrayList<>();
        List<Bid> bids = bidRepo.findByItemId(itemId);
        for (Bid bid : bids) {
            points.add(new PricePoint(bid.getAmount(), bid.getBidTime()));
        }
        if (points.isEmpty()) {
            itemRepo.findById(itemId).ifPresent(item ->
                    points.add(new PricePoint(item.getStartingPrice(), item.getStartTime()))
            );
        }
        points.sort((a, b) -> a.getTime().compareTo(b.getTime()));
        return points;
    }

    public boolean cancelAutoBid(String userId, String itemId) throws SQLException {
        return autoBidRepo.cancelAutoBid(userId, itemId);
    }

    public boolean setAutoBid(String userId, String itemId, double maxAmount) throws SQLException {
        if (maxAmount > MAX_BID_AMOUNT) return false;
        Optional<Item> item = itemRepo.findById(itemId);
        if (item.isEmpty()) return false;
        if (maxAmount <= item.get().getCurrentPrice()) return false;
        return autoBidRepo.setAutoBid(userId, itemId, maxAmount);
    }

    public boolean isWinner(String userId, String itemId) throws SQLException {
        Optional<Item> item = itemRepo.findById(itemId);
        if (item.isEmpty()) return false;
        return item.get().getStatus() == ItemStatus.ENDED &&
                userId.equals(item.get().getCurrentBidderId());
    }

    public double getTotalSpent(String userId) throws SQLException {
        List<Item> wonItems = getWonProducts(userId);
        return wonItems.stream().mapToDouble(Item::getCurrentPrice).sum();
    }

    // 🔥 THÊM: Shutdown method để cleanup resources
    public void shutdown() {
        if (lockCleanupExecutor != null) {
            lockCleanupExecutor.shutdown();
            try {
                lockCleanupExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                lockCleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        itemLocks.clear();
        System.out.println("✅ BiddingService shutdown complete. Locks cleared: " + itemLocks.size());
    }
}