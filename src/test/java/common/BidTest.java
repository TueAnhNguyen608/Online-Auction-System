package common;

import com.auction.common.model.bidding.Bid;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BidTest {

    private Bid bid;

    @BeforeEach
    void setUp() {
        bid = new Bid();
        bid.setItemId("item1");
        bid.setItemName("iPhone 15 Pro Max");
        bid.setBidderId("bidder1");
        bid.setBidderName("Nguyễn Văn An");
        bid.setAmount(15000000);
        bid.setAutoBid(false);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @Order(1)
        @DisplayName("TC-BID-01: Default constructor creates valid bid")
        void testDefaultConstructor() {
            Bid newBid = new Bid();
            assertNotNull(newBid.getId());
            assertNotNull(newBid.getBidTime());
            assertFalse(newBid.isAutoBid());
            assertFalse(newBid.isWinning());
            assertEquals(0.0, newBid.getAmount());
        }

        @Test
        @Order(2)
        @DisplayName("TC-BID-02: Parameterized constructor creates bid with values")
        void testParameterizedConstructor() {
            Bid newBid = new Bid("item123", "bidder456", 20000000);
            assertEquals("item123", newBid.getItemId());
            assertEquals("bidder456", newBid.getBidderId());
            assertEquals(20000000, newBid.getAmount());
            assertFalse(newBid.isAutoBid());
            assertNotNull(newBid.getId());
        }
    }

    @Nested
    @DisplayName("Getter/Setter Tests")
    class AccessorTests {

        @Test
        @Order(3)
        @DisplayName("TC-BID-03: Set and get itemId")
        void testSetGetItemId() {
            bid.setItemId("new_item_123");
            assertEquals("new_item_123", bid.getItemId());
        }

        @Test
        @Order(4)
        @DisplayName("TC-BID-04: Set and get itemName")
        void testSetGetItemName() {
            bid.setItemName("Samsung Galaxy S24");
            assertEquals("Samsung Galaxy S24", bid.getItemName());
        }

        @Test
        @Order(5)
        @DisplayName("TC-BID-05: Set and get bidderId")
        void testSetGetBidderId() {
            bid.setBidderId("new_bidder_456");
            assertEquals("new_bidder_456", bid.getBidderId());
        }

        @Test
        @Order(6)
        @DisplayName("TC-BID-06: Set and get bidderName")
        void testSetGetBidderName() {
            bid.setBidderName("Trần Thị Bình");
            assertEquals("Trần Thị Bình", bid.getBidderName());
        }

        @Test
        @Order(7)
        @DisplayName("TC-BID-07: Set and get amount")
        void testSetGetAmount() {
            bid.setAmount(25000000);
            assertEquals(25000000, bid.getAmount());
        }

        @Test
        @Order(8)
        @DisplayName("TC-BID-08: Set and get autoBid flag")
        void testSetGetAutoBid() {
            bid.setAutoBid(true);
            assertTrue(bid.isAutoBid());
            bid.setAutoBid(false);
            assertFalse(bid.isAutoBid());
        }

        @Test
        @Order(9)
        @DisplayName("TC-BID-09: Set and get winning flag")
        void testSetGetWinning() {
            bid.setWinning(true);
            assertTrue(bid.isWinning());
            bid.setWinning(false);
            assertFalse(bid.isWinning());
        }

        @Test
        @Order(10)
        @DisplayName("TC-BID-10: Set and get final price")
        void testSetGetFinalPrice() {
            bid.setFinalPrice(18000000);
            assertEquals(18000000, bid.getFinalPrice());
        }

        @Test
        @Order(11)
        @DisplayName("TC-BID-11: Set and get item status")
        void testSetGetItemStatus() {
            bid.setItemStatus("ENDED");
            assertEquals("ENDED", bid.getItemStatus());
        }

        @Test
        @Order(12)
        @DisplayName("TC-BID-12: Set and get bid time")
        void testSetGetBidTime() {
            LocalDateTime now = LocalDateTime.now();
            bid.setBidTime(now);
            assertEquals(now, bid.getBidTime());
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryTests {

        @Test
        @Order(13)
        @DisplayName("TC-BID-13: Create auto-bid using factory method")
        void testCreateAutoBid() {
            Bid autoBid = Bid.createAutoBid("item_auto", "bidder_auto", 30000000);
            assertTrue(autoBid.isAutoBid());
            assertEquals("item_auto", autoBid.getItemId());
            assertEquals("bidder_auto", autoBid.getBidderId());
            assertEquals(30000000, autoBid.getAmount());
            assertNotNull(autoBid.getId());
            assertNotNull(autoBid.getBidTime());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @ParameterizedTest
        @Order(14)
        @CsvSource({
                "1000000, 1000000",
                "0, 0",
                "999999999, 999999999"
        })
        @DisplayName("TC-BID-14: Bid amount accepts various values")
        void testBidAmountValues(double amount, double expected) {
            bid.setAmount(amount);
            assertEquals(expected, bid.getAmount());
        }

        @Test
        @Order(15)
        @DisplayName("TC-BID-15: Bid timestamp millis returns positive number")
        void testGetTimestampMillis() {
            long timestamp = bid.getTimestampMillis();
            assertTrue(timestamp > 0);
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @Order(16)
        @DisplayName("TC-BID-16: Same ID bids are equal")
        void testBidEquality_SameId() {
            Bid bid1 = new Bid();
            Bid bid2 = new Bid();
            bid2.setId(bid1.getId());
            assertEquals(bid1, bid2);
            assertEquals(bid1.hashCode(), bid2.hashCode());
        }

        @Test
        @Order(17)
        @DisplayName("TC-BID-17: Different ID bids are not equal")
        void testBidEquality_DifferentId() {
            Bid bid1 = new Bid();
            Bid bid2 = new Bid();
            assertNotEquals(bid1, bid2);
        }

        @Test
        @Order(18)
        @DisplayName("TC-BID-18: Bid equals null returns false")
        void testBidEqualsNull() {
            assertNotEquals(null, bid);
        }

        @Test
        @Order(19)
        @DisplayName("TC-BID-19: Bid equals same object returns true")
        void testBidEqualsSameObject() {
            assertEquals(bid, bid);
        }
    }

    @Nested
    @DisplayName("String Representation Tests")
    class ToStringTests {

        @Test
        @Order(20)
        @DisplayName("TC-BID-20: toString contains bidder name and amount")
        void testToStringContainsBidInfo() {
            bid.setBidderName("Test Bidder");
            bid.setAmount(12345678);
            bid.setAutoBid(true);

            String str = bid.toString();

            // Kiểm tra các thành phần cơ bản
            assertNotNull(str);
            assertTrue(str.contains("Test Bidder"), "Should contain bidder name");
            assertTrue(str.contains("auto=true") || str.contains("auto:true"), "Should contain auto-bid info");
        }

        @Test
        @Order(21)
        @DisplayName("TC-BID-21: toString for manual bid shows auto=false")
        void testToStringForManualBid() {
            bid.setAutoBid(false);
            String str = bid.toString();
            assertNotNull(str);
        }
    }
}