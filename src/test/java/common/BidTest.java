package common;

import common.model.bidding.Bid;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class BidTest {

    @Test
    void testCreateBid() {
        Bid bid = new Bid();
        bid.setItemId("item1");
        bid.setBidderId("bidder1");
        bid.setAmount(15000000);
        bid.setAutoBid(false);

        assertEquals("item1", bid.getItemId());
        assertEquals("bidder1", bid.getBidderId());
        assertEquals(15000000, bid.getAmount());
        assertFalse(bid.isAutoBid());
    }

    @Test
    void testAutoBid() {
        Bid bid = new Bid();
        bid.setAutoBid(true);
        assertTrue(bid.isAutoBid());
    }
}