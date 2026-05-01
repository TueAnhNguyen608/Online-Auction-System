package common;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testCreateUser() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("123456");
        user.setEmail("test@email.com");
        user.setRole("BIDDER");

        assertEquals("testuser", user.getUsername());
        assertEquals("123456", user.getPassword());
        assertEquals("test@email.com", user.getEmail());
        assertEquals("BIDDER", user.getRole());
    }

    @Test
    void testUserIsAdmin() {
        User admin = new User();
        admin.setRole("ADMIN");
        assertTrue(admin.isAdmin());

        User bidder = new User();
        bidder.setRole("BIDDER");
        assertFalse(bidder.isAdmin());
    }

    @Test
    void testUserIsBidder() {
        User bidder = new User();
        bidder.setRole("BIDDER");
        assertTrue(bidder.isBidder());

        User admin = new User();
        admin.setRole("ADMIN");
        assertFalse(admin.isBidder());
    }
}