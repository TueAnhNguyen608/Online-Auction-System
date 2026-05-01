package common;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    @Test
    void testInvalidBidException() {
        InvalidBidException e = new InvalidBidException("Gia khong hop le", "item1", 50000, 100000);
        assertEquals(50000, e.getAttemptedBid());
        assertEquals(100000, e.getMinimumRequired());
        assertEquals("item1", e.getItemId());
    }

    @Test
    void testAuctionClosedException() {
        AuctionClosedException e = new AuctionClosedException("Phien dau gia da ket thuc", "item1", "winner1", 1000000);
        assertEquals("item1", e.getItemId());
        assertEquals("winner1", e.getWinnerId());
        assertEquals(1000000, e.getFinalPrice());
    }

    @Test
    void testAuthenticationException() {
        AuthenticationException e = new AuthenticationException("Sai mat khau", "john", "sai 3 lan");
        assertEquals("john", e.getUsername());
        assertEquals("sai 3 lan", e.getReason());
    }

    @Test
    void testDataValidationException() {
        DataValidationException e = new DataValidationException("Loi validation");
        e.addError("Ten khong duoc trong");
        e.addError("Gia phai lon hon 0");

        assertTrue(e.hasErrors());
        assertEquals(2, e.getErrors().size());
    }
}