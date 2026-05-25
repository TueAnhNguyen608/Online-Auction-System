package common;

import com.auction.common.model.exception.AuctionClosedException;
import com.auction.common.model.exception.AuthenticationException;
import com.auction.common.model.exception.ConnectionException;
import com.auction.common.model.exception.DataValidationException;
import com.auction.common.model.exception.InvalidBidException;
import org.junit.jupiter.api.*;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    @Test
    @DisplayName("TC-EXC-01: AuctionClosedException")
    void testAuctionClosedException() {
        AuctionClosedException ex1 = new AuctionClosedException("Phiên đấu giá đã kết thúc");
        assertEquals("Phiên đấu giá đã kết thúc", ex1.getMessage());

        AuctionClosedException ex2 = new AuctionClosedException("Đã kết thúc", "item1");
        assertEquals("item1", ex2.getItemId());

        AuctionClosedException ex3 = new AuctionClosedException("Kết thúc", "item1", "winner1", 1000000);
        assertEquals("item1", ex3.getItemId());
        assertEquals("winner1", ex3.getWinnerId());
        assertEquals(1000000, ex3.getFinalPrice());
    }

    @Test
    @DisplayName("TC-EXC-02: AuthenticationException")
    void testAuthenticationException() {
        AuthenticationException ex1 = new AuthenticationException("Sai mật khẩu");
        assertEquals("Sai mật khẩu", ex1.getMessage());

        AuthenticationException ex2 = new AuthenticationException("Sai mật khẩu", "bidder1");
        assertEquals("bidder1", ex2.getUsername());

        AuthenticationException ex3 = new AuthenticationException("Sai mật khẩu", "bidder1", "wrong password");
        assertEquals("bidder1", ex3.getUsername());
        assertEquals("wrong password", ex3.getReason());
    }

    @Test
    @DisplayName("TC-EXC-03: ConnectionException")
    void testConnectionException() {
        ConnectionException ex = new ConnectionException("Không thể kết nối", "localhost", 9999);
        assertEquals("localhost", ex.getHost());
        assertEquals(9999, ex.getPort());
    }

    @Test
    @DisplayName("TC-EXC-04: DataValidationException")
    void testDataValidationException() {
        DataValidationException ex = new DataValidationException("Dữ liệu không hợp lệ");
        assertTrue(ex.getErrors().isEmpty());

        List<String> errors = new ArrayList<>();
        errors.add("Tên không được để trống");
        errors.add("Giá phải lớn hơn 0");
        DataValidationException ex2 = new DataValidationException("Lỗi validation", errors);

        assertEquals(2, ex2.getErrors().size());
        assertTrue(ex2.hasErrors());
        assertEquals("Tên không được để trống; Giá phải lớn hơn 0", ex2.getFormattedErrors());

        ex2.addError("Email không hợp lệ");
        assertEquals(3, ex2.getErrors().size());
    }

    @Test
    @DisplayName("TC-EXC-05: InvalidBidException")
    void testInvalidBidException() {
        InvalidBidException ex1 = new InvalidBidException("Giá không hợp lệ");
        assertEquals("Giá không hợp lệ", ex1.getMessage());

        InvalidBidException ex2 = new InvalidBidException("Giá quá thấp", 1000000, 2000000);
        assertEquals(1000000, ex2.getAttemptedBid());
        assertEquals(2000000, ex2.getMinimumRequired());

        InvalidBidException ex3 = new InvalidBidException("Giá quá thấp", "item1", 1000000, 2000000);
        assertEquals("item1", ex3.getItemId());
        assertEquals(1000000, ex3.getAttemptedBid());
        assertEquals(2000000, ex3.getMinimumRequired());
    }
}