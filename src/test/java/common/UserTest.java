package common;

import com.auction.common.model.user.User;
import com.auction.common.model.user.UserRole;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setEmail("test@example.com");
        user.setRole(UserRole.BIDDER);
        user.setFullName("Test User");
        user.setPhone("0123456789");
        user.setBalance(1000000);
    }

    @Test
    @DisplayName("TC-USER-01: Tạo user mới thành công")
    void testCreateUser() {
        assertNotNull(user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals(UserRole.BIDDER, user.getRole());
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("TC-USER-02: Kiểm tra role ADMIN")
    void testAdminRole() {
        user.setRole(UserRole.ADMIN);
        assertTrue(user.isAdmin());
        assertFalse(user.isBidder());
    }

    @Test
    @DisplayName("TC-USER-03: Kiểm tra role BIDDER")
    void testBidderRole() {
        user.setRole(UserRole.BIDDER);
        assertTrue(user.isBidder());
        assertFalse(user.isAdmin());
    }

    @Test
    @DisplayName("TC-USER-04: Kiểm tra role SELLER")
    void testSellerRole() {
        user.setRole(UserRole.SELLER);
        assertTrue(user.isSeller());
        assertFalse(user.isAdmin());
    }

    @Test
    @DisplayName("TC-USER-05: Cập nhật thông tin user")
    void testUpdateUser() {
        user.setEmail("newemail@example.com");
        user.setFullName("New Name");

        assertEquals("newemail@example.com", user.getEmail());
        assertEquals("New Name", user.getFullName());
    }

    @Test
    @DisplayName("TC-USER-06: User mặc định có role BIDDER")
    void testDefaultRole() {
        User newUser = new User();
        assertEquals(UserRole.BIDDER, newUser.getRole());
    }

    @Test
    @DisplayName("TC-USER-07: So sánh 2 user cùng ID")
    void testUserEquality() {
        User user1 = new User();
        User user2 = new User();
        user2.setId(user1.getId());

        assertEquals(user1, user2);
    }
}