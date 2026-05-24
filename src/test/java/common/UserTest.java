package common;

import common.model.user.User;
import common.model.user.UserRole;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testCreateUser() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setEmail("test@example.com");
        user.setRole(UserRole.BIDDER);
        user.setFullName("Test User");
        user.setPhone("0123456789");

        assertEquals("testuser", user.getUsername());
        assertEquals("password123", user.getPassword());
        assertEquals("test@example.com", user.getEmail());

        // ✅ SỬA DÒNG NÀY: So sánh với name() thay vì enum trực tiếp
        assertEquals(UserRole.BIDDER.name(), user.getRole());
        // Hoặc: assertEquals("BIDDER", user.getRole());

        assertEquals("Test User", user.getFullName());
        assertEquals("0123456789", user.getPhone());
        assertTrue(user.isActive());
    }

    @Test
    void testUserRole() {
        User user = new User();
        user.setRole(UserRole.ADMIN);
        assertTrue(user.isAdmin());
        assertFalse(user.isBidder());
        assertFalse(user.isSeller());

        user.setRole(UserRole.BIDDER);
        assertTrue(user.isBidder());
        assertFalse(user.isAdmin());
        assertFalse(user.isSeller());

        user.setRole(UserRole.SELLER);
        assertTrue(user.isSeller());
        assertFalse(user.isAdmin());
        assertFalse(user.isBidder());
    }

    @Test
    void testUserDefaultValues() {
        User user = new User();
        assertNotNull(user.getId());
        assertNotNull(user.getCreatedAt());
        assertTrue(user.isActive());
        assertEquals(0.0, user.getBalance());
        assertEquals(UserRole.BIDDER, user.getRole());
    }
}