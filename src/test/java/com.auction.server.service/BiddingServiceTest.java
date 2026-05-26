package com.auction.server.service;

import com.auction.common.constant.AuctionConstants;
import com.auction.common.model.bidding.Bid;
import com.auction.common.model.bidding.BidResult;
import com.auction.common.model.item.Item;
import com.auction.common.model.item.ItemStatus;
import com.auction.server.pool.DatabaseConnectionPool;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.ItemRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BiddingServiceTest {

    private static BiddingService biddingService;
    private static ItemRepository itemRepo;
    private static BidRepository bidRepo;
    private static String testItemId;
    private static String testUserId;
    private static String testUserName;

    @BeforeAll
    static void setUp() throws SQLException {
        DatabaseConnectionPool.getInstance();

        biddingService = BiddingService.getInstance();
        itemRepo = ItemRepository.getInstance();
        bidRepo = BidRepository.getInstance();

        testUserId = "test_bidder_" + UUID.randomUUID().toString().substring(0, 8);
        testUserName = "Test Bidder";

        createTestItem();
    }

    @AfterAll
    static void cleanUp() throws SQLException {
        if (testItemId != null) {
            // 🔥 SỬA: Xóa item trực tiếp
            itemRepo.deleteById(testItemId);
        }
    }

    private static void createTestItem() throws SQLException {
        Item item = new Item();
        testItemId = UUID.randomUUID().toString();
        item.setId(testItemId);
        item.setName("Test Laptop for Bidding");
        item.setDescription("High-end gaming laptop for testing");
        item.setStartingPrice(10000000);
        item.setCurrentPrice(10000000);
        item.setStartTime(LocalDateTime.now());
        item.setEndTime(LocalDateTime.now().plusHours(24));
        item.setStatus(ItemStatus.ACTIVE);
        item.setSellerId("test_seller");
        item.setSellerName("Test Seller");

        itemRepo.save(item);
    }

    // ========== PLACE BID TESTS ==========

    @Nested
    @DisplayName("Place Bid Tests")
    class PlaceBidTests {

        @Test
        @Order(1)
        @DisplayName("TC-BID-SRV-01: Place bid successfully with valid amount")
        void testPlaceBid_Success() throws Exception {
            Bid bid = new Bid();
            bid.setItemId(testItemId);
            bid.setAmount(11000000);
            bid.setBidderId(testUserId);
            bid.setBidderName(testUserName);
            bid.setAutoBid(false);

            BidResult result = biddingService.placeBid(bid);

            assertNotNull(result);
            assertNotNull(result.getBid());
            assertEquals(11000000, result.getBid().getAmount());
            assertFalse(result.isExtended());

            Item updatedItem = itemRepo.findById(testItemId).orElse(null);
            assertNotNull(updatedItem);
            assertEquals(11000000, updatedItem.getCurrentPrice());
            assertEquals(testUserId, updatedItem.getCurrentBidderId());
        }

        @Test
        @Order(2)
        @DisplayName("TC-BID-SRV-02: Place bid with amount too low - throws exception")
        void testPlaceBid_AmountTooLow() {
            Bid bid = new Bid();
            bid.setItemId(testItemId);
            bid.setAmount(10100000);
            bid.setBidderId(testUserId);
            bid.setBidderName(testUserName);

            Exception exception = assertThrows(Exception.class, () -> {
                biddingService.placeBid(bid);
            });

            assertTrue(exception.getMessage().contains("Giá đặt phải cao hơn") ||
                    exception.getMessage().contains("quá thấp"));
        }

        @Test
        @Order(3)
        @DisplayName("TC-BID-SRV-03: Place bid on non-existent item - throws exception")
        void testPlaceBid_ItemNotFound() {
            Bid bid = new Bid();
            bid.setItemId("non_existent_id_12345");
            bid.setAmount(20000000);
            bid.setBidderId(testUserId);
            bid.setBidderName(testUserName);

            Exception exception = assertThrows(Exception.class, () -> {
                biddingService.placeBid(bid);
            });

            assertTrue(exception.getMessage().contains("không tồn tại"));
        }

        @Test
        @Order(4)
        @DisplayName("TC-BID-SRV-04: Current winner cannot bid again")
        void testPlaceBid_CurrentWinnerCannotBid() throws Exception {
            Bid bid1 = new Bid();
            bid1.setItemId(testItemId);
            bid1.setAmount(12000000);
            bid1.setBidderId(testUserId);
            bid1.setBidderName(testUserName);
            biddingService.placeBid(bid1);

            Bid bid2 = new Bid();
            bid2.setItemId(testItemId);
            bid2.setAmount(13000000);
            bid2.setBidderId(testUserId);
            bid2.setBidderName(testUserName);

            Exception exception = assertThrows(Exception.class, () -> {
                biddingService.placeBid(bid2);
            });

            assertTrue(exception.getMessage().contains("không thể") ||
                    exception.getMessage().contains("tự ra giá"));
        }

        @Test
        @Order(5)
        @DisplayName("TC-BID-SRV-05: Place bid with zero amount - throws exception")
        void testPlaceBid_ZeroAmount() {
            Bid bid = new Bid();
            bid.setItemId(testItemId);
            bid.setAmount(0);
            bid.setBidderId(testUserId);
            bid.setBidderName(testUserName);

            Exception exception = assertThrows(Exception.class, () -> {
                biddingService.placeBid(bid);
            });

            assertNotNull(exception);
        }
    }

    // ========== ANTI-SNIPING TESTS ==========

    @Nested
    @DisplayName("Anti-Sniping Tests")
    class AntiSnipingTests {

        private String snipingItemId;

        @BeforeEach
        void setUpSnipingItem() throws SQLException {
            snipingItemId = UUID.randomUUID().toString();
            Item item = new Item();
            item.setId(snipingItemId);
            item.setName("Sniping Test Item");
            item.setStartingPrice(10000000);
            item.setCurrentPrice(10000000);
            item.setStartTime(LocalDateTime.now());
            item.setEndTime(LocalDateTime.now().plusSeconds(25));
            item.setStatus(ItemStatus.ACTIVE);
            item.setSellerId("test_seller");
            item.setSellerName("Test Seller");
            itemRepo.save(item);
        }

        @AfterEach
        void cleanUpSnipingItem() throws SQLException {
            itemRepo.deleteById(snipingItemId);
        }

        @Test
        @Order(6)
        @DisplayName("TC-BID-SRV-06: Anti-sniping extends auction time when bid in last 30 seconds")
        void testAntiSniping_ExtendsAuction() throws Exception {
            Item itemBefore = itemRepo.findById(snipingItemId).orElse(null);
            assertNotNull(itemBefore);
            long originalEndTimeSeconds = itemBefore.getSecondsRemaining();

            Thread.sleep(1000);

            Bid bid = new Bid();
            bid.setItemId(snipingItemId);
            bid.setAmount(11000000);
            bid.setBidderId("anti_sniper");
            bid.setBidderName("Anti Sniper");
            bid.setAutoBid(false);

            BidResult result = biddingService.placeBid(bid);

            Item itemAfter = itemRepo.findById(snipingItemId).orElse(null);
            assertNotNull(itemAfter);

            if (originalEndTimeSeconds <= AuctionConstants.SNIPING_WINDOW_SECONDS) {
                assertTrue(result.isExtended() ||
                        itemAfter.getSecondsRemaining() > originalEndTimeSeconds);
            }
        }
    }

    // ========== BID HISTORY TESTS ==========

    @Nested
    @DisplayName("Bid History Tests")
    class BidHistoryTests {

        private String historyItemId;

        @BeforeEach
        void setUpHistoryItem() throws SQLException, Exception {
            historyItemId = UUID.randomUUID().toString();
            Item item = new Item();
            item.setId(historyItemId);
            item.setName("History Test Item");
            item.setStartingPrice(10000000);
            item.setCurrentPrice(10000000);
            item.setStartTime(LocalDateTime.now());
            item.setEndTime(LocalDateTime.now().plusHours(24));
            item.setStatus(ItemStatus.ACTIVE);
            item.setSellerId("test_seller");
            item.setSellerName("Test Seller");
            itemRepo.save(item);

            for (int i = 0; i < 3; i++) {
                Bid bid = new Bid();
                bid.setItemId(historyItemId);
                bid.setAmount(11000000 + i * 1000000);
                bid.setBidderId("bidder_" + i);
                bid.setBidderName("Bidder " + i);
                bid.setAutoBid(false);
                biddingService.placeBid(bid);
            }
        }

        @AfterEach
        void cleanUpHistoryItem() throws SQLException {
            itemRepo.deleteById(historyItemId);
        }

        @Test
        @Order(7)
        @DisplayName("TC-BID-SRV-07: Get bid history for item returns all bids")
        void testGetBidHistory() throws SQLException {
            List<Bid> history = biddingService.getBidHistory(historyItemId);
            assertNotNull(history);
            assertTrue(history.size() >= 3);
        }

        @Test
        @Order(8)
        @DisplayName("TC-BID-SRV-08: Get bid history for non-existent item returns empty list")
        void testGetBidHistory_ItemNotFound() throws SQLException {
            List<Bid> history = biddingService.getBidHistory("non_existent");
            assertNotNull(history);
            assertTrue(history.isEmpty());
        }

        @Test
        @Order(9)
        @DisplayName("TC-BID-SRV-09: Get my bid history returns user's bids")
        void testGetMyBidHistory() throws SQLException {
            List<Bid> myHistory = biddingService.getMyBidHistory("bidder_0");
            assertNotNull(myHistory);
            assertTrue(myHistory.size() > 0);
            assertEquals("bidder_0", myHistory.get(0).getBidderId());
        }
    }

    // ========== PRICE HISTORY TESTS ==========

    @Nested
    @DisplayName("Price History Tests")
    class PriceHistoryTests {

        private String priceHistoryItemId;

        @BeforeEach
        void setUpPriceHistoryItem() throws SQLException, Exception {
            priceHistoryItemId = UUID.randomUUID().toString();
            Item item = new Item();
            item.setId(priceHistoryItemId);
            item.setName("Price History Item");
            item.setStartingPrice(10000000);
            item.setCurrentPrice(10000000);
            item.setStartTime(LocalDateTime.now());
            item.setEndTime(LocalDateTime.now().plusHours(24));
            item.setStatus(ItemStatus.ACTIVE);
            item.setSellerId("test_seller");
            item.setSellerName("Test Seller");
            itemRepo.save(item);

            double[] amounts = {11000000, 12500000, 14000000};
            for (double amount : amounts) {
                Bid bid = new Bid();
                bid.setItemId(priceHistoryItemId);
                bid.setAmount(amount);
                bid.setBidderId("price_bidder");
                bid.setBidderName("Price Bidder");
                bid.setAutoBid(false);
                biddingService.placeBid(bid);
                Thread.sleep(100);
            }
        }

        @AfterEach
        void cleanUpPriceHistoryItem() throws SQLException {
            itemRepo.deleteById(priceHistoryItemId);
        }

        @Test
        @Order(10)
        @DisplayName("TC-BID-SRV-10: Get price history returns chronological order")
        void testGetPriceHistory() throws SQLException {
            var priceHistory = biddingService.getPriceHistory(priceHistoryItemId);
            assertNotNull(priceHistory);
            assertTrue(priceHistory.size() >= 3);

            for (int i = 1; i < priceHistory.size(); i++) {
                assertTrue(priceHistory.get(i).getTime().isAfter(priceHistory.get(i - 1).getTime()));
            }
        }

        @Test
        @Order(11)
        @DisplayName("TC-BID-SRV-11: Get price history for item with no bids returns starting price")
        void testGetPriceHistory_NoBids() throws SQLException {
            String newItemId = UUID.randomUUID().toString();
            Item item = new Item();
            item.setId(newItemId);
            item.setName("No Bid Item");
            item.setStartingPrice(5000000);
            item.setCurrentPrice(5000000);
            item.setStartTime(LocalDateTime.now());
            item.setEndTime(LocalDateTime.now().plusHours(24));
            item.setStatus(ItemStatus.ACTIVE);
            item.setSellerId("test_seller");
            item.setSellerName("Test Seller");
            itemRepo.save(item);

            var priceHistory = biddingService.getPriceHistory(newItemId);

            assertNotNull(priceHistory);
            assertTrue(priceHistory.size() >= 1);
            assertEquals(5000000, priceHistory.get(0).getPrice());

            itemRepo.deleteById(newItemId);
        }
    }

    // ========== WINNER TESTS ==========

    @Nested
    @DisplayName("Winner Tests")
    class WinnerTests {

        private String winnerItemId;
        private String winnerUserId;

        @BeforeEach
        void setUpWinnerItem() throws SQLException {
            winnerItemId = UUID.randomUUID().toString();
            winnerUserId = "winner_test_user";

            Item item = new Item();
            item.setId(winnerItemId);
            item.setName("Winner Test Item");
            item.setStartingPrice(10000000);
            item.setCurrentPrice(10000000);
            item.setStartTime(LocalDateTime.now());
            item.setEndTime(LocalDateTime.now().minusSeconds(1));
            item.setStatus(ItemStatus.ENDED);
            item.setCurrentBidderId(winnerUserId);
            item.setCurrentBidderName("Winner User");
            item.setSellerId("test_seller");
            item.setSellerName("Test Seller");
            itemRepo.save(item);
        }

        @AfterEach
        void cleanUpWinnerItem() throws SQLException {
            itemRepo.deleteById(winnerItemId);
        }

        @Test
        @Order(12)
        @DisplayName("TC-BID-SRV-12: Get won products returns items user has won")
        void testGetWonProducts() throws SQLException {
            List<Item> wonItems = biddingService.getWonProducts(winnerUserId);
            assertNotNull(wonItems);
            assertTrue(wonItems.stream().anyMatch(i -> i.getId().equals(winnerItemId)));
        }

        @Test
        @Order(13)
        @DisplayName("TC-BID-SRV-13: Check if user is winner of specific item")
        void testIsWinner() throws SQLException {
            boolean isWinner = biddingService.isWinner(winnerUserId, winnerItemId);
            assertTrue(isWinner);

            boolean isNotWinner = biddingService.isWinner("other_user", winnerItemId);
            assertFalse(isNotWinner);
        }

        @Test
        @Order(14)
        @DisplayName("TC-BID-SRV-14: Get total spent by winner")
        void testGetTotalSpent() throws SQLException {
            double totalSpent = biddingService.getTotalSpent(winnerUserId);
            assertTrue(totalSpent >= 10000000);
        }
    }

    // ========== CONCURRENT BIDDING TESTS ==========

    @Nested
    @DisplayName("Concurrent Bidding Tests")
    class ConcurrentBiddingTests {

        private String concurrentItemId;

        @BeforeEach
        void setUpConcurrentItem() throws SQLException {
            concurrentItemId = UUID.randomUUID().toString();
            Item item = new Item();
            item.setId(concurrentItemId);
            item.setName("Concurrent Test Item");
            item.setStartingPrice(10000000);
            item.setCurrentPrice(10000000);
            item.setStartTime(LocalDateTime.now());
            item.setEndTime(LocalDateTime.now().plusHours(1));
            item.setStatus(ItemStatus.ACTIVE);
            item.setSellerId("test_seller");
            item.setSellerName("Test Seller");
            itemRepo.save(item);
        }

        @AfterEach
        void cleanUpConcurrentItem() throws SQLException {
            itemRepo.deleteById(concurrentItemId);
        }

        @Test
        @Order(15)
        @DisplayName("TC-BID-SRV-15: Multiple bids concurrently - thread safety")
        void testConcurrentBidding() throws InterruptedException {
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int bidderNum = i;
                executor.submit(() -> {
                    try {
                        Bid bid = new Bid();
                        bid.setItemId(concurrentItemId);
                        bid.setAmount(11000000 + bidderNum * 500000);
                        bid.setBidderId("concurrent_" + bidderNum);
                        bid.setBidderName("Concurrent Bidder " + bidderNum);
                        bid.setAutoBid(false);
                        biddingService.placeBid(bid);
                    } catch (Exception e) {
                        // Expected for some bids (outbid or too low)
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(30, TimeUnit.SECONDS);
            assertTrue(completed);
            executor.shutdown();

            try {
                Item finalItem = itemRepo.findById(concurrentItemId).orElse(null);
                assertNotNull(finalItem);
                assertTrue(finalItem.getCurrentPrice() >= 11000000);
            } catch (SQLException e) {
                fail("Database error: " + e.getMessage());
            }
        }
    }
}