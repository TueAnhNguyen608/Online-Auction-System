package common;

import com.auction.common.model.item.Item;
import com.auction.common.model.item.ItemStatus;
import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ItemTest {

    @Test
    @DisplayName("TC-ITEM-01: Tạo item mới thành công")
    void testCreateItem() {
        Item item = new Item();
        item.setName("iPhone 15");
        item.setDescription("Điện thoại cao cấp");
        item.setStartingPrice(10000000);
        item.setCurrentPrice(10000000);
        item.setStatus(ItemStatus.ACTIVE);

        assertEquals("iPhone 15", item.getName());
        assertEquals(10000000, item.getStartingPrice());
        assertEquals(ItemStatus.ACTIVE, item.getStatus());
        assertTrue(item.isActive());
    }

    @Test
    @DisplayName("TC-ITEM-02: Kiểm tra item active status")
    void testItemIsActive() {
        Item item = new Item();

        item.setStatus(ItemStatus.ACTIVE);
        assertTrue(item.isActive());

        item.setStatus(ItemStatus.ENDED);
        assertFalse(item.isActive());
        assertTrue(item.isEnded());
    }

    @Test
    @DisplayName("TC-ITEM-03: Kiểm tra thời gian còn lại")
    void testGetSecondsRemaining() {
        Item item = new Item();
        item.setEndTime(LocalDateTime.now().plusHours(2));

        long remaining = item.getSecondsRemaining();
        assertTrue(remaining > 0);
    }

    @Test
    @DisplayName("TC-ITEM-04: Kiểm tra ItemStatus enum")
    void testItemStatusValues() {
        Item item = new Item();

        item.setStatus(ItemStatus.PENDING);
        assertEquals(ItemStatus.PENDING, item.getStatus());

        item.setStatus(ItemStatus.ACTIVE);
        assertEquals(ItemStatus.ACTIVE, item.getStatus());

        item.setStatus(ItemStatus.ENDED);
        assertEquals(ItemStatus.ENDED, item.getStatus());

        item.setStatus(ItemStatus.CANCELLED);
        assertEquals(ItemStatus.CANCELLED, item.getStatus());
    }

    @Test
    @DisplayName("TC-ITEM-05: Kiểm tra increment bid count")
    void testIncrementBidCount() {
        Item item = new Item();
        assertEquals(0, item.getBidCount());

        item.incrementBidCount();
        assertEquals(1, item.getBidCount());
    }
}
