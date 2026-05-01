package common;

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
        item.setStatus("ACTIVE");

        assertEquals("iPhone 15", item.getName());
        assertEquals(10000000, item.getStartingPrice());
        assertTrue(item.isActive());
    }

    @Test
    void testItemIsActive() {
        Item item = new Item();
        item.setStatus("ACTIVE");
        assertTrue(item.isActive());

        item.setStatus("ENDED");
        assertFalse(item.isActive());
    }

    @Test
    void testItemGetTimeRemaining() {
        Item item = new Item();
        item.setEndTime(LocalDateTime.now().plusHours(2));
        long remaining = item.getTimeRemainingMillis();
        assertTrue(remaining > 0);
    }
}