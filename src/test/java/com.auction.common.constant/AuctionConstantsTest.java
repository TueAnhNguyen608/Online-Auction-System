package com.auction.common.constant;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuctionConstantsTest {

    @Nested
    @DisplayName("Server Configuration Tests")
    class ServerConfigTests {

        @Test
        @Order(1)
        @DisplayName("TC-CONST-01: Server port and host")
        void testServerConnection() {
            assertEquals(9999, AuctionConstants.DEFAULT_SERVER_PORT);
            assertEquals("localhost", AuctionConstants.DEFAULT_SERVER_HOST);
        }

        @Test
        @Order(2)
        @DisplayName("TC-CONST-02: Thread pool configuration")
        void testThreadPool() {
            assertEquals(50, AuctionConstants.MAX_THREAD_POOL_SIZE);
            assertEquals(30, AuctionConstants.CONNECTION_TIMEOUT_SECONDS);
        }
    }

    @Nested
    @DisplayName("Auction Rules Tests")
    class AuctionRulesTests {

        @Test
        @Order(3)
        @DisplayName("TC-CONST-03: Sniping and extension rules")
        void testSnipingRules() {
            assertEquals(30, AuctionConstants.SNIPING_WINDOW_SECONDS);
            assertEquals(60, AuctionConstants.EXTENSION_SECONDS);
        }

        @Test
        @Order(4)
        @DisplayName("TC-CONST-04: Bidding rules")
        void testBiddingRules() {
            assertEquals(0.05, AuctionConstants.DEFAULT_STEP_PERCENT);
            assertEquals(1000, AuctionConstants.MINIMUM_BID_AMOUNT);
        }
    }

    @Nested
    @DisplayName("Validation Rules Tests")
    class ValidationRulesTests {

        @Test
        @Order(5)
        @DisplayName("TC-CONST-05: Password validation")
        void testPasswordRules() {
            assertEquals(6, AuctionConstants.MIN_PASSWORD_LENGTH);
        }

        @Test
        @Order(6)
        @DisplayName("TC-CONST-06: Item field length limits")
        void testItemFieldLimits() {
            assertEquals(200, AuctionConstants.MAX_ITEM_NAME_LENGTH);
            assertEquals(1000, AuctionConstants.MAX_DESCRIPTION_LENGTH);
        }

        @Test
        @Order(7)
        @DisplayName("TC-CONST-07: Auction duration limits")
        void testAuctionDurationLimits() {
            assertEquals(5, AuctionConstants.MIN_AUCTION_DURATION_MINUTES);
            assertEquals(30, AuctionConstants.MAX_AUCTION_DURATION_DAYS);
        }
    }

    @Nested
    @DisplayName("Item Status Tests")
    class ItemStatusTests {

        @Test
        @Order(8)
        @DisplayName("TC-CONST-08: Item status constants")
        void testItemStatus() {
            assertEquals("PENDING", AuctionConstants.ITEM_STATUS_PENDING);
            assertEquals("ACTIVE", AuctionConstants.ITEM_STATUS_ACTIVE);
            assertEquals("ENDED", AuctionConstants.ITEM_STATUS_ENDED);
            assertEquals("CANCELLED", AuctionConstants.ITEM_STATUS_CANCELLED);
        }
    }

    @Nested
    @DisplayName("User Roles Tests")
    class UserRolesTests {

        @Test
        @Order(9)
        @DisplayName("TC-CONST-09: User role constants")
        void testUserRoles() {
            assertEquals("ADMIN", AuctionConstants.ROLE_ADMIN);
            assertEquals("BIDDER", AuctionConstants.ROLE_BIDDER);
            assertEquals("SELLER", AuctionConstants.ROLE_SELLER);
        }
    }

    @Nested
    @DisplayName("Scheduler Configuration Tests")
    class SchedulerTests {

        @Test
        @Order(10)
        @DisplayName("TC-CONST-10: Scheduler intervals")
        void testSchedulerIntervals() {
            assertEquals(10, AuctionConstants.AUCTION_CHECK_INTERVAL_SECONDS);
            assertEquals(1, AuctionConstants.TIME_BROADCAST_INTERVAL_SECONDS);
        }
    }

    @Nested
    @DisplayName("Socket Configuration Tests")
    class SocketConfigTests {

        @Test
        @Order(11)
        @DisplayName("TC-CONST-11: Socket timeout values")
        void testSocketTimeouts() {
            assertEquals(30000, AuctionConstants.SOCKET_TIMEOUT_MS);
            assertEquals(30000, AuctionConstants.HEARTBEAT_INTERVAL_MS);
        }
    }

    @Nested
    @DisplayName("Message Constants Tests")
    class MessageTests {

        @Test
        @Order(12)
        @DisplayName("TC-CONST-12: All message constants are defined")
        void testAllMessagesAreDefined() {
            assertNotNull(AuctionConstants.MSG_LOGIN_SUCCESS);
            assertNotNull(AuctionConstants.MSG_LOGIN_FAILED);
            assertNotNull(AuctionConstants.MSG_BID_SUCCESS);
            assertNotNull(AuctionConstants.MSG_BID_TOO_LOW);
            assertNotNull(AuctionConstants.MSG_AUCTION_ENDED);
            assertNotNull(AuctionConstants.MSG_ITEM_NOT_FOUND);
            assertNotNull(AuctionConstants.MSG_PERMISSION_DENIED);
        }
    }

    @Nested
    @DisplayName("Database Configuration Tests")
    class DatabaseConfigTests {

        @Test
        @Order(13)
        @DisplayName("TC-CONST-13: Database connection pool size")
        void testDatabasePoolSize() {
            assertEquals(10, AuctionConstants.DB_POOL_SIZE);
            assertEquals(5, AuctionConstants.DB_MIN_POOL_SIZE);
        }

        @Test
        @Order(14)
        @DisplayName("TC-CONST-14: Database URL contains required parameters")
        void testDatabaseUrl() {
            String url = AuctionConstants.DB_URL;
            assertTrue(url.contains("jdbc:mysql://"));
            assertTrue(url.contains("useSSL=false"));
            assertTrue(url.contains("characterEncoding=utf8"));
        }
    }

    // 🔥 KHÔNG test instantiation vì class có private constructor
    // Class AuctionConstants là final và có private constructor
    // Nên không thể tạo instance, chỉ truy cập static fields
}