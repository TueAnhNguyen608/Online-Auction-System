package server.service;

import common.constant.AuctionConstants;
import common.model.bidding.Bid;
import common.model.bidding.BidResult;
import common.model.bidding.PricePoint;
import common.model.event.PriceUpdateEvent;
import common.model.exception.AuctionClosedException;
import common.model.exception.InvalidBidException;
import common.model.item.Item;
import common.model.item.ItemStatus;
import server.broadcast.EventBroadcaster;
import server.repository.AutoBidRepository;
import server.repository.BidRepository;
import server.repository.ItemRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class BiddingService {

    private static volatile BiddingService instance;
    private final ItemRepository itemRepo;
    private final BidRepository bidRepo;
    private final AutoBidRepository autoBidRepo;
    private final EventBroadcaster broadcaster;
    private final ConcurrentHashMap<String, ReentrantLock> itemLocks = new ConcurrentHashMap<>();

    private BiddingService() {
        this.itemRepo = ItemRepository.getInstance();
        this.bidRepo = BidRepository.getInstance();
        this.autoBidRepo = AutoBidRepository.getInstance();
        this.broadcaster = EventBroadcaster.getInstance();
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

    // ========== PLACE BID ==========
    public BidResult placeBid(Bid bid) throws Exception {
        ReentrantLock lock = itemLocks.computeIfAbsent(bid.getItemId(), k -> new ReentrantLock());
        lock.lock();

        try {
            Item item = itemRepo.findById(bid.getItemId())
                    .orElseThrow(() -> new InvalidBidException("Sản phẩm không tồn tại"));

            // Kiểm tra phiên đấu giá đã kết thúc chưa
            if (item.getStatus() == ItemStatus.ENDED) {
                throw new AuctionClosedException("Phiên đấu giá đã kết thúc", bid.getItemId());
            }

            if (item.getEndTime().isBefore(LocalDateTime.now())) {
                itemRepo.updateStatus(bid.getItemId(), ItemStatus.ENDED);
                throw new AuctionClosedException("Thời gian đấu giá đã kết thúc", bid.getItemId());
            }

            // ✅ KHÔNG cho người đang thắng tự outbid chính mình
            if (bid.getBidderId().equals(item.getCurrentBidderId()) && item.getCurrentBidderId() != null) {
                throw new InvalidBidException("Bạn đang là người trả giá cao nhất, không thể tự ra giá cao hơn");
            }

            // ✅ Tính bước giá (step price) = 5% giá hiện tại, tối thiểu 1000đ
            double stepPrice = item.getCurrentPrice() * AuctionConstants.DEFAULT_STEP_PERCENT;
            if (stepPrice < AuctionConstants.MINIMUM_BID_AMOUNT) {
                stepPrice = AuctionConstants.MINIMUM_BID_AMOUNT;
            }

            double minBid = item.getCurrentPrice() + stepPrice;
            if (bid.getAmount() < minBid) {
                throw new InvalidBidException(
                        String.format("Giá đặt phải cao hơn %,.0f VND (bước giá tối thiểu)", minBid),
                        bid.getItemId(), bid.getAmount(), minBid);
            }

            // ✅ ANTI-SNIPING: Nếu bid trong 30 giây cuối, kéo dài thêm 60 giây
            long secondsLeft = item.getSecondsRemaining();
            boolean extended = false;

            if (secondsLeft <= AuctionConstants.SNIPING_WINDOW_SECONDS && secondsLeft > 0) {
                itemRepo.extendEndTime(bid.getItemId(), AuctionConstants.EXTENSION_SECONDS);
                extended = true;
                System.out.println("⏰ Anti-Sniping: Kéo dài phiên đấu giá thêm " + AuctionConstants.EXTENSION_SECONDS + " giây");

                // Broadcast thông báo kéo dài
                PriceUpdateEvent extensionEvent = PriceUpdateEvent.extension(bid.getItemId(), AuctionConstants.EXTENSION_SECONDS);
                broadcaster.broadcastToItem(bid.getItemId(), extensionEvent);
            }

            // Lưu bid và cập nhật item
            bid.setItemName(item.getName());
            bid.setBidTime(LocalDateTime.now());
            bidRepo.save(bid);

            itemRepo.updateCurrentPrice(bid.getItemId(), bid.getAmount(),
                    bid.getBidderId(), bid.getBidderName());

            // Broadcast cập nhật giá
            long newRemaining = Math.max(0, item.getSecondsRemaining() + (extended ? AuctionConstants.EXTENSION_SECONDS : 0));
            PriceUpdateEvent event = new PriceUpdateEvent(
                    item.getId(), item.getName(), bid.getAmount(),
                    bid.getBidderName(), newRemaining, item.getBidCount() + 1
            );
            broadcaster.broadcastToItem(bid.getItemId(), event);

            System.out.println("✅ Đặt giá thành công: " + bid.getBidderName() + " - " + String.format("%,.0f", bid.getAmount()) + " VND");

            // ✅ Kích hoạt AUTO-BID sau khi đặt giá thành công
            triggerAutoBids(bid.getItemId());

            return new BidResult(bid, extended);

        } finally {
            lock.unlock();
        }
    }

    // ========== TRIGGER AUTO-BID ==========
    private void triggerAutoBids(String itemId) throws Exception {
        int maxIterations = 50; // Tránh vòng lặp vô hạn
        int iteration = 0;

        while (iteration++ < maxIterations) {
            Item item = itemRepo.findById(itemId).orElse(null);
            if (item == null || item.getStatus() == ItemStatus.ENDED) break;

            // Lấy danh sách auto-bid của item này
            List<Map<String, Object>> autoBids = autoBidRepo.findByItemId(itemId);
            if (autoBids.isEmpty()) break;

            String currentWinnerId = item.getCurrentBidderId();
            double currentPrice = item.getCurrentPrice();

            // Tính bước giá
            double stepPrice = currentPrice * AuctionConstants.DEFAULT_STEP_PERCENT;
            if (stepPrice < AuctionConstants.MINIMUM_BID_AMOUNT) {
                stepPrice = AuctionConstants.MINIMUM_BID_AMOUNT;
            }

            // Tìm auto-bid cao nhất không phải của người đang thắng
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

            if (bestAutoBid == null) break;

            String nextBidderId = (String) bestAutoBid.get("user_id");
            String nextBidderName = (String) bestAutoBid.get("username");
            double maxAmount = (double) bestAutoBid.get("max_amount");

            // Tính giá đặt auto-bid
            double nextPrice = currentPrice + stepPrice;
            double bidAmount = Math.min(nextPrice, maxAmount);

            if (bidAmount <= currentPrice) break;

            System.out.println("🤖 Auto-bid kích hoạt: " + nextBidderName + " đặt " + String.format("%,.0f", bidAmount) + " VND");

            // Tạo và lưu auto-bid
            Bid autoBid = new Bid();
            autoBid.setItemId(itemId);
            autoBid.setAmount(bidAmount);
            autoBid.setBidderId(nextBidderId);
            autoBid.setBidderName(nextBidderName);
            autoBid.setAutoBid(true);
            autoBid.setBidTime(LocalDateTime.now());
            autoBid.setItemName(item.getName());

            bidRepo.save(autoBid);
            itemRepo.updateCurrentPrice(itemId, bidAmount, nextBidderId, nextBidderName);

            // Broadcast cập nhật
            PriceUpdateEvent event = new PriceUpdateEvent(
                    item.getId(), item.getName(), bidAmount,
                    nextBidderName, item.getSecondsRemaining(), item.getBidCount() + 1
            );
            broadcaster.broadcastToItem(itemId, event);

            // Tiếp tục vòng lặp để kiểm tra auto-bid tiếp theo
        }

        if (iteration >= maxIterations) {
            System.err.println("⚠️ Auto-bid vượt quá số lần lặp cho item: " + itemId);
        }
    }

    // ========== GET BID HISTORY ==========
    public List<Bid> getBidHistory(String itemId) throws SQLException {
        return bidRepo.findByItemId(itemId);
    }

    public List<Bid> getMyBidHistory(String userId) throws SQLException {
        return bidRepo.findByBidderId(userId);
    }

    public List<Item> getWonProducts(String userId) throws SQLException {
        List<Item> wonItems = new ArrayList<>();
        List<Item> allItems = itemRepo.findAll();

        for (Item item : allItems) {
            if (item.getStatus() == ItemStatus.ENDED &&
                    userId != null && userId.equals(item.getCurrentBidderId())) {
                wonItems.add(item);
            }
        }
        return wonItems;
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
        return points;
    }

    // ========== CANCEL AUTO-BID ==========
    public boolean cancelAutoBid(String userId, String itemId) throws SQLException {
        return autoBidRepo.cancelAutoBid(userId, itemId);
    }

    // ========== SET AUTO-BID ==========
    public boolean setAutoBid(String userId, String itemId, double maxAmount) throws SQLException {
        // Kiểm tra item có tồn tại không
        Optional<Item> item = itemRepo.findById(itemId);
        if (item.isEmpty()) {
            return false;
        }

        // Kiểm tra maxAmount phải lớn hơn giá hiện tại
        if (maxAmount <= item.get().getCurrentPrice()) {
            return false;
        }

        return autoBidRepo.setAutoBid(userId, itemId, maxAmount);
    }
}