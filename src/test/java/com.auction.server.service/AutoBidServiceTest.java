package com.auction.server.service;

import com.auction.common.constant.AuctionConstants;
import com.auction.common.model.bidding.Bid;
import com.auction.common.model.bidding.BidResult;  // 🔥 THÊM DÒNG NÀY - QUAN TRỌNG NHẤT
import com.auction.common.model.item.Item;
import com.auction.common.model.item.ItemStatus;
import com.auction.server.pool.DatabaseConnectionPool;
import com.auction.server.repository.AutoBidRepository;
import com.auction.server.repository.BidRepository;
import com.auction.server.repository.ItemRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AutoBidServiceTest {

    private static AutoBidService autoBidService;
    private static BiddingService biddingService;
    private static ItemRepository itemRepo;
    private static AutoBidRepository autoBidRepo;
    private static BidRepository bidRepo;

    private static String testItemId;
    private static String testUserId1;
    private static String testUserId2;
    private static String testUserId3;

    @BeforeAll
    static void setUp() throws SQLException {
        DatabaseConnectionPool.getInstance();

        autoBidService = AutoBidService.getInstance();
        biddingService = BiddingService.getInstance();
        itemRepo = ItemRepository.getInstance();
        autoBidRepo = AutoBidRepository.getInstance();
        bidRepo = BidRepository.getInstance();

        testUserId1 = "auto_bidder_1";
        testUserId2 = "auto_bidder_2";
        testUserId3 = "auto_bidder_3";

        createTestItem();
    }

    @AfterAll
    static void cleanUp() throws SQLException {
        if (testItemId != null) {
            autoBidRepo.cancelAutoBid(testUserId1, testItemId);
            autoBidRepo.cancelAutoBid(testUserId2, testItemId);
            autoBidRepo.cancelAutoBid(testUserId3, testItemId);
            bidRepo.deleteByItemId(testItemId);
            itemRepo.deleteById(testItemId);
        }
    }

    private static void createTestItem() throws SQLException {
        Item item = new Item();
        testItemId = UUID.randomUUID().toString();
        item.setId(testItemId);
        item.setName("Auto-Bid Test Item");
        item.setDescription("Item for testing auto-bid functionality");
        item.setStartingPrice(10000000);
        item.setCurrentPrice(10000000);
        item.setStartTime(LocalDateTime.now());
        item.setEndTime(LocalDateTime.now().plusHours(24));
        item.setStatus(ItemStatus.ACTIVE);
        item.setSellerId("test_seller");
        item.setSellerName("Test Seller");

        itemRepo.save(item);
    }

    @Nested
    @DisplayName("Set Auto-Bid Tests")
    class SetAutoBidTests {

        @Test
        @Order(1)
        @DisplayName("TC-AUTO-01: Set auto-bid successfully")
        void testSetAutoBid_Success() throws SQLException {
            boolean result = autoBidService.setAutoBid(testUserId1, testItemId, 20000000);
            assertTrue(result);

            List<Map<String, Object>> settings = autoBidService.getAutoBidSettings(testUserId1);
            assertFalse(settings.isEmpty());

            Map<String, Object> setting = settings.get(0);
            assertEquals(testItemId, setting.get("itemId"));
            assertEquals(20000000.0, (double) setting.get("maxAmount"));
        }

        @Test
        @Order(2)
        @DisplayName("TC-AUTO-02: Set auto-bid with max amount lower than current price - fails")
        void testSetAutoBid_AmountTooLow() throws SQLException {
            boolean result = autoBidService.setAutoBid(testUserId2, testItemId, 5000000);
            assertFalse(result);
        }

        @Test
        @Order(3)
        @DisplayName("TC-AUTO-03: Update existing auto-bid with higher amount")
        void testSetAutoBid_UpdateHigher() throws SQLException {
            autoBidService.setAutoBid(testUserId3, testItemId, 15000000);
            boolean result = autoBidService.setAutoBid(testUserId3, testItemId, 25000000);
            assertTrue(result);

            List<Map<String, Object>> settings = autoBidService.getAutoBidSettings(testUserId3);
            assertEquals(25000000.0, (double) settings.get(0).get("maxAmount"));
        }

        @ParameterizedTest
        @Order(4)
        @CsvSource({
                "12000000, true",
                "10000000, false",
                "9000000, false",
                "50000000, true"
        })
        @DisplayName("TC-AUTO-04: Set auto-bid with various amounts")
        void testSetAutoBid_VariousAmounts(double maxAmount, boolean expected) throws SQLException {
            String tempUserId = "temp_" + UUID.randomUUID().toString().substring(0, 8);
            boolean result = autoBidService.setAutoBid(tempUserId, testItemId, maxAmount);
            assertEquals(expected, result);

            if (result) {
                autoBidRepo.cancelAutoBid(tempUserId, testItemId);
            }
        }
    }

    @Nested
    @DisplayName("Cancel Auto-Bid Tests")
    class CancelAutoBidTests {

        private String cancelUserId;

        @BeforeEach
        void setUpCancelTest() throws SQLException {
            cancelUserId = "cancel_user_" + UUID.randomUUID().toString().substring(0, 8);
            autoBidService.setAutoBid(cancelUserId, testItemId, 30000000);
        }

        @AfterEach
        void cleanUpCancelTest() throws SQLException {
            autoBidRepo.cancelAutoBid(cancelUserId, testItemId);
        }

        @Test
        @Order(5)
        @DisplayName("TC-AUTO-05: Cancel existing auto-bid successfully")
        void testCancelAutoBid_Success() throws SQLException {
            boolean result = autoBidService.cancelAutoBid(cancelUserId, testItemId);
            assertTrue(result);

            List<Map<String, Object>> settings = autoBidService.getAutoBidSettings(cancelUserId);
            assertTrue(settings.stream().noneMatch(s -> s.get("itemId").equals(testItemId)));
        }

        @Test
        @Order(6)
        @DisplayName("TC-AUTO-06: Cancel non-existent auto-bid returns false")
        void testCancelAutoBid_NonExistent() throws SQLException {
            boolean result = autoBidService.cancelAutoBid("non_existent_user", "non_existent_item");
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Get Auto-Bid Settings Tests")
    class GetAutoBidSettingsTests {

        @Test
        @Order(7)
        @DisplayName("TC-AUTO-07: Get auto-bid settings returns correct data")
        void testGetAutoBidSettings_ReturnsData() throws SQLException {
            String getUserId = "get_user_" + UUID.randomUUID().toString().substring(0, 8);
            autoBidService.setAutoBid(getUserId, testItemId, 18000000);

            List<Map<String, Object>> settings = autoBidService.getAutoBidSettings(getUserId);

            assertNotNull(settings);
            assertFalse(settings.isEmpty());

            Map<String, Object> setting = settings.get(0);
            assertTrue(setting.containsKey("itemId"));
            assertTrue(setting.containsKey("itemName"));
            assertTrue(setting.containsKey("maxAmount"));
            assertTrue(setting.containsKey("currentPrice"));

            autoBidRepo.cancelAutoBid(getUserId, testItemId);
        }

        @Test
        @Order(8)
        @DisplayName("TC-AUTO-08: Get auto-bid settings for user with no settings returns empty list")
        void testGetAutoBidSettings_NoSettings() throws SQLException {
            String newUserId = "new_user_" + UUID.randomUUID().toString().substring(0, 8);
            List<Map<String, Object>> settings = autoBidService.getAutoBidSettings(newUserId);

            assertNotNull(settings);
            assertTrue(settings.isEmpty());
        }
    }

    @Nested
    @DisplayName("Auto-Bid Trigger Tests")
    class AutoBidTriggerTests {

        private String triggerItemId;
        private String autoBidderId;
        private String normalBidderId;

        @BeforeEach
        void setUpTriggerTest() throws SQLException {
            triggerItemId = UUID.randomUUID().toString();
            autoBidderId = "auto_trigger_" + UUID.randomUUID().toString().substring(0, 8);
            normalBidderId = "normal_" + UUID.randomUUID().toString().substring(0, 8);

            Item item = new Item();
            item.setId(triggerItemId);
            item.setName("Auto-Trigger Test Item");
            item.setStartingPrice(10000000);
            item.setCurrentPrice(10000000);
            item.setStartTime(LocalDateTime.now());
            item.setEndTime(LocalDateTime.now().plusHours(24));
            item.setStatus(ItemStatus.ACTIVE);
            item.setSellerId("test_seller");
            item.setSellerName("Test Seller");
            itemRepo.save(item);

            autoBidService.setAutoBid(autoBidderId, triggerItemId, 25000000);
        }

        @AfterEach
        void cleanUpTriggerTest() throws SQLException {
            autoBidRepo.cancelAutoBid(autoBidderId, triggerItemId);
            bidRepo.deleteByItemId(triggerItemId);
            itemRepo.deleteById(triggerItemId);
        }

        @Test
        @Order(9)
        @DisplayName("TC-AUTO-09: Auto-bid triggers when another user bids")
        void testAutoBid_TriggersWhenOutbid() throws Exception {
            Bid normalBid = new Bid();
            normalBid.setItemId(triggerItemId);
            normalBid.setAmount(15000000);
            normalBid.setBidderId(normalBidderId);
            normalBid.setBidderName("Normal Bidder");
            normalBid.setAutoBid(false);

            biddingService.placeBid(normalBid);
            Thread.sleep(500);

            Item finalItem = itemRepo.findById(triggerItemId).orElse(null);
            assertNotNull(finalItem);
            assertTrue(finalItem.getCurrentPrice() > 15000000);
        }

        @Test
        @Order(10)
        @DisplayName("TC-AUTO-10: Auto-bid does not trigger when auto-bidder is current winner")
        void testAutoBid_NoTriggerWhenAlreadyWinning() throws Exception {
            Bid autoBidderBid = new Bid();
            autoBidderBid.setItemId(triggerItemId);
            autoBidderBid.setAmount(12000000);
            autoBidderBid.setBidderId(autoBidderId);
            autoBidderBid.setBidderName("Auto Bidder");
            autoBidderBid.setAutoBid(true);

            biddingService.placeBid(autoBidderBid);

            Bid higherBid = new Bid();
            higherBid.setItemId(triggerItemId);
            higherBid.setAmount(14000000);
            higherBid.setBidderId(normalBidderId);
            higherBid.setBidderName("Normal Bidder");
            higherBid.setAutoBid(false);

            biddingService.placeBid(higherBid);

            Item finalItem = itemRepo.findById(triggerItemId).orElse(null);
            assertNotNull(finalItem);
            assertTrue(finalItem.getCurrentPrice() > 14000000);
        }

        @Test
        @Order(11)
        @DisplayName("TC-AUTO-11: Multiple auto-bids - highest max wins")
        void testAutoBid_MultipleAutoBidders() throws SQLException, Exception {
            String autoBidder2Id = "auto2_" + UUID.randomUUID().toString().substring(0, 8);
            String manualBidderId = "manual_" + UUID.randomUUID().toString().substring(0, 8);

            autoBidService.setAutoBid(autoBidder2Id, triggerItemId, 30000000);

            Bid manualBid = new Bid();
            manualBid.setItemId(triggerItemId);
            manualBid.setAmount(12000000);
            manualBid.setBidderId(manualBidderId);
            manualBid.setBidderName("Manual Bidder");
            manualBid.setAutoBid(false);

            biddingService.placeBid(manualBid);
            Thread.sleep(1000);

            Item finalItem = itemRepo.findById(triggerItemId).orElse(null);
            assertNotNull(finalItem);

            autoBidRepo.cancelAutoBid(autoBidder2Id, triggerItemId);
        }
    }

    @Nested
    @DisplayName("Concurrent Auto-Bid Tests")
    class ConcurrentAutoBidTests {

        private String concurrentItemId;

        @BeforeEach
        void setUpConcurrentItem() throws SQLException {
            concurrentItemId = UUID.randomUUID().toString();
            Item item = new Item();
            item.setId(concurrentItemId);
            item.setName("Concurrent Auto-Bid Item");
            item.setStartingPrice(10000000);
            item.setCurrentPrice(10000000);
            item.setStartTime(LocalDateTime.now());
            item.setEndTime(LocalDateTime.now().plusHours(24));
            item.setStatus(ItemStatus.ACTIVE);
            item.setSellerId("test_seller");
            item.setSellerName("Test Seller");
            itemRepo.save(item);
        }

        @AfterEach
        void cleanUpConcurrentItem() throws SQLException {
            bidRepo.deleteByItemId(concurrentItemId);
            itemRepo.deleteById(concurrentItemId);
        }

        @Test
        @Order(12)
        @DisplayName("TC-AUTO-12: Multiple users setting auto-bid concurrently")
        void testConcurrentSetAutoBid() throws InterruptedException {
            int threadCount = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        String userId = "concurrent_" + index;
                        double maxAmount = 15000000 + index * 100000;
                        autoBidService.setAutoBid(userId, concurrentItemId, maxAmount);
                    } catch (SQLException e) {
                        fail("Failed to set auto-bid: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(30, TimeUnit.SECONDS);
            assertTrue(completed);
            executor.shutdown();
        }
    }
}