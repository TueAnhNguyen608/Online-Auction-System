package common;

import common.model.item.Item;
import common.model.item.ItemStatus;
import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ItemTest {

    @Test
    void testCreateItem() {
        Item item = new Item();
        item.setName("iPhone 15");
        item.setStartingPrice(10000000);
        item.setCurrentPrice(10000000);
        item.setStatus(ItemStatus.ACTIVE);  // ✅ SỬA: dùng enum thay vì String

        assertEquals("iPhone 15", item.getName());
        assertEquals(10000000, item.getStartingPrice());
        assertTrue(item.isActive());
    }

    @Test
    void testItemIsActive() {
        Item item = new Item();
        item.setStatus(ItemStatus.ACTIVE);  // ✅ SỬA
        assertTrue(item.isActive());

        item.setStatus(ItemStatus.ENDED);    // ✅ SỬA
        assertFalse(item.isActive());
    }

    @Test
    void testItemGetTimeRemaining() {
        Item item = new Item();
        item.setEndTime(LocalDateTime.now().plusHours(2));
        // ✅ SỬA: dùng getSecondsRemaining() thay vì getTimeRemainingMillis()
        long remaining = item.getSecondsRemaining();
        assertTrue(remaining > 0);
    }

    @Test
    void testItemStatusValues() {
        Item item = new Item();
        item.setStatus(ItemStatus.PENDING);
        assertEquals(ItemStatus.PENDING, item.getStatus());
        assertFalse(item.isActive());

        item.setStatus(ItemStatus.ACTIVE);
        assertEquals(ItemStatus.ACTIVE, item.getStatus());
        assertTrue(item.isActive());

        item.setStatus(ItemStatus.ENDED);
        assertEquals(ItemStatus.ENDED, item.getStatus());
        assertFalse(item.isActive());

        item.setStatus(ItemStatus.CANCELLED);
        assertEquals(ItemStatus.CANCELLED, item.getStatus());
        assertFalse(item.isActive());
    }
}