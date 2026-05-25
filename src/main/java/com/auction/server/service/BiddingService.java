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
import com.auction.server.repository.AutoBidRepository;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.ItemRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class BiddingService {

    private static final double MAX_BID_AMOUNT = 1_000_000_000_000L;
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

    public BidResult placeBid(Bid bid) throws Exception {
        if (bid.getAmount() > MAX_BID_AMOUNT) {
            throw new InvalidBidException("Giá đặt không được vượt quá " + String.format("%,.0f", MAX_BID_AMOUNT) + " VND");
        }

        if (bid.getAmount() <= 0) {
            throw new InvalidBidException("Giá đặt phải lớn hơn 0");
        }

        ReentrantLock lock = itemLocks.computeIfAbsent(bid.getItemId(), k -> new ReentrantLock());
        lock.lock();

        try {
            Item item = itemRepo.findById(bid.getItemId())
                    .orElseThrow(() -> new InvalidBidException("Sản phẩm không tồn tại"));

            if (item.getStatus() == ItemStatus.ENDED) {
                throw new AuctionClosedException("Phiên đấu giá đã kết thúc", bid.getItemId());
            }

            if (item.getEndTime().isBefore(LocalDateTime.now())) {
                itemRepo.updateStatus(bid.getItemId(), ItemStatus.ENDED);
                throw new AuctionClosedException("Thời gian đấu giá đã kết thúc", bid.getItemId());
            }

            if (bid.getBidderId().equals(item.getCurrentBidderId()) && item.getCurrentBidderId() != null) {
                throw new InvalidBidException("Bạn đang là người trả giá cao nhất, không thể tự ra giá cao hơn");
            }

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

            long secondsLeft = item.getSecondsRemaining();
            boolean extended = false;

            if (secondsLeft <= AuctionConstants.SNIPING_WINDOW_SECONDS && secondsLeft > 0) {
                itemRepo.extendEndTime(bid.getItemId(), AuctionConstants.EXTENSION_SECONDS);
                extended = true;
                System.out.println("⏰ Anti-Sniping: Kéo dài phiên đấu giá thêm " + AuctionConstants.EXTENSION_SECONDS + " giây");

                PriceUpdateEvent extensionEvent = PriceUpdateEvent.extension(bid.getItemId(), AuctionConstants.EXTENSION_SECONDS);
                broadcaster.broadcastToItem(bid.getItemId(), extensionEvent);
            }

            bid.setItemName(item.getName());
            bid.setBidTime(LocalDateTime.now());
            bidRepo.save(bid);

            itemRepo.updateCurrentPrice(bid.getItemId(), bid.getAmount(),
                    bid.getBidderId(), bid.getBidderName());

            item = itemRepo.findById(bid.getItemId()).orElse(item);

            long newRemaining = Math.max(0, item.getSecondsRemaining());
            PriceUpdateEvent event = new PriceUpdateEvent(
                    item.getId(), item.getName(), bid.getAmount(),
                    bid.getBidderName(), newRemaining, item.getBidCount()
            );
            broadcaster.broadcastToItem(bid.getItemId(), event);

            System.out.println("✅ Đặt giá thành công: " + bid.getBidderName() + " - " + String.format("%,.0f", bid.getAmount()) + " VND");

            triggerAutoBids(bid.getItemId());

            return new BidResult(bid, extended);

        } finally {
            lock.unlock();
        }
    }

    private void triggerAutoBids(String itemId) throws Exception {
        int maxIterations = 50;
        int iteration = 0;

        while (iteration++ < maxIterations) {
            Item item = itemRepo.findById(itemId).orElse(null);
            if (item == null || item.getStatus() == ItemStatus.ENDED) break;

            List<Map<String, Object>> autoBids = autoBidRepo.findByItemId(itemId);
            if (autoBids.isEmpty()) break;

            String currentWinnerId = item.getCurrentBidderId();
            double currentPrice = item.getCurrentPrice();

            double stepPrice = currentPrice * AuctionConstants.DEFAULT_STEP_PERCENT;
            if (stepPrice < AuctionConstants.MINIMUM_BID_AMOUNT) {
                stepPrice = AuctionConstants.MINIMUM_BID_AMOUNT;
            }

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

            double nextPrice = currentPrice + stepPrice;
            double bidAmount = Math.min(nextPrice, maxAmount);

            if (bidAmount <= currentPrice) break;

            if (bidAmount > MAX_BID_AMOUNT) {
                System.err.println("⚠️ Auto-bid amount exceeds max limit: " + bidAmount);
                break;
            }

            System.out.println("🤖 Auto-bid kích hoạt: " + nextBidderName + " đặt " + String.format("%,.0f", bidAmount) + " VND");

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

            item = itemRepo.findById(itemId).orElse(item);

            PriceUpdateEvent event = new PriceUpdateEvent(
                    item.getId(), item.getName(), bidAmount,
                    nextBidderName, item.getSecondsRemaining(), item.getBidCount()
            );
            broadcaster.broadcastToItem(itemId, event);
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
        if (maxAmount > MAX_BID_AMOUNT) {
            return false;
        }
        Optional<Item> item = itemRepo.findById(itemId);
        if (item.isEmpty()) {
            return false;
        }
        if (maxAmount <= item.get().getCurrentPrice()) {
            return false;
        }
        return autoBidRepo.setAutoBid(userId, itemId, maxAmount);
    }

    public boolean isWinner(String userId, String itemId) throws SQLException {
        Optional<Item> item = itemRepo.findById(itemId);
        if (item.isEmpty()) {
            return false;
        }
        return item.get().getStatus() == ItemStatus.ENDED &&
                userId.equals(item.get().getCurrentBidderId());
    }

    public double getTotalSpent(String userId) throws SQLException {
        List<Item> wonItems = getWonProducts(userId);
        return wonItems.stream()
                .mapToDouble(Item::getCurrentPrice)
                .sum();
    }
}