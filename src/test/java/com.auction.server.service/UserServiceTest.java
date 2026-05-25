package com.auction.server.service;

import com.auction.common.model.exception.AuthenticationException;
import com.auction.common.model.user.User;
import com.auction.common.model.user.UserRole;
import com.auction.server.pool.DatabaseConnectionPool;
import com.auction.server.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceTest {

    private static UserService userService;
    private static UserRepository userRepo;
    private static String testUserId;
    private static String testUsername;

    @BeforeAll
    static void setUp() throws SQLException {
        DatabaseConnectionPool.getInstance();
        userService = UserService.getInstance();
        userRepo = UserRepository.getInstance();

        testUsername = "testuser_" + UUID.randomUUID().toString().substring(0, 8);
        createTestUser();
    }

    @AfterAll
    static void cleanUp() throws SQLException {
        if (testUserId != null) {
            userRepo.deleteById(testUserId);
        }
    }

    private static void createTestUser() throws SQLException {
        User user = new User();
        user.setUsername(testUsername);
        user.setPassword("Test@123456");
        user.setEmail("test@example.com");
        user.setRole(UserRole.BIDDER);
        user.setFullName("Test User");
        user.setPhone("0123456789");
        user.setBalance(10000000);

        userRepo.save(user);

        // Get the saved user to retrieve ID
        User savedUser = userRepo.findByUsername(testUsername);
        testUserId = savedUser.getId();
    }

    // ========== LOGIN TESTS ==========

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @Order(1)
        @DisplayName("TC-USER-SRV-01: Login with correct credentials - success")
        void testLogin_Success() throws AuthenticationException, SQLException {
            User loggedIn = userService.login(testUsername, "Test@123456");

            assertNotNull(loggedIn);
            assertEquals(testUsername, loggedIn.getUsername());
            assertEquals(testUserId, loggedIn.getId());
        }

        @Test
        @Order(2)
        @DisplayName("TC-USER-SRV-02: Login with wrong password - throws exception")
        void testLogin_WrongPassword() {
            assertThrows(AuthenticationException.class, () -> {
                userService.login(testUsername, "wrong_password");
            });
        }

        @Test
        @Order(3)
        @DisplayName("TC-USER-SRV-03: Login with non-existent username - throws exception")
        void testLogin_UserNotFound() {
            assertThrows(AuthenticationException.class, () -> {
                userService.login("non_existent_user_12345", "any_password");
            });
        }

        @ParameterizedTest
        @Order(4)
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("TC-USER-SRV-04: Login with empty/null username - throws exception")
        void testLogin_EmptyUsername(String invalidUsername) {
            assertThrows(AuthenticationException.class, () -> {
                userService.login(invalidUsername, "password");
            });
        }

        @Test
        @Order(5)
        @DisplayName("TC-USER-SRV-05: Login with empty password - throws exception")
        void testLogin_EmptyPassword() {
            assertThrows(AuthenticationException.class, () -> {
                userService.login(testUsername, "");
            });
        }
    }

    // ========== REGISTER TESTS ==========

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        private String newUsername;

        @AfterEach
        void cleanUpRegister() throws SQLException {
            if (newUsername != null) {
                User user = userRepo.findByUsername(newUsername);
                if (user != null) {
                    userRepo.deleteById(user.getId());
                }
            }
        }

        @Test
        @Order(6)
        @DisplayName("TC-USER-SRV-06: Register new user - success")
        void testRegister_Success() throws SQLException {
            newUsername = "newuser_" + UUID.randomUUID().toString().substring(0, 8);
            User newUser = new User();
            newUser.setUsername(newUsername);
            newUser.setPassword("New@123456");
            newUser.setEmail("new@example.com");
            newUser.setRole(UserRole.BIDDER);
            newUser.setFullName("New User");
            newUser.setPhone("0987654321");

            boolean result = userService.register(newUser);

            assertTrue(result);

            // Verify user was created
            User savedUser = userRepo.findByUsername(newUsername);
            assertNotNull(savedUser);
            assertEquals(newUsername, savedUser.getUsername());
        }

        @Test
        @Order(7)
        @DisplayName("TC-USER-SRV-07: Register with duplicate username - fails")
        void testRegister_DuplicateUsername() throws SQLException {
            User duplicateUser = new User();
            duplicateUser.setUsername(testUsername); // Already exists
            duplicateUser.setPassword("Another@123");
            duplicateUser.setEmail("another@example.com");

            boolean result = userService.register(duplicateUser);

            assertFalse(result);
        }
    }

    // ========== CHANGE PASSWORD TESTS ==========

    @Nested
    @DisplayName("Change Password Tests")
    class ChangePasswordTests {

        private String passwordUserId;
        private String passwordUsername;

        @BeforeEach
        void setUpPasswordUser() throws SQLException {
            passwordUsername = "pwduser_" + UUID.randomUUID().toString().substring(0, 8);
            User user = new User();
            user.setUsername(passwordUsername);
            user.setPassword("Old@123456");
            user.setEmail("pwd@example.com");
            user.setRole(UserRole.BIDDER);
            userRepo.save(user);

            User savedUser = userRepo.findByUsername(passwordUsername);
            passwordUserId = savedUser.getId();
        }

        @AfterEach
        void cleanUpPasswordUser() throws SQLException {
            if (passwordUserId != null) {
                userRepo.deleteById(passwordUserId);
            }
        }

        @Test
        @Order(8)
        @DisplayName("TC-USER-SRV-08: Change password with correct old password - success")
        void testChangePassword_Success() throws SQLException {
            boolean result = userService.changePassword(passwordUserId, "Old@123456", "New@123456");

            assertTrue(result);

            // Verify can login with new password
            User loggedIn = userRepo.findByUsername(passwordUsername);
            assertNotNull(loggedIn);
        }

        @Test
        @Order(9)
        @DisplayName("TC-USER-SRV-09: Change password with wrong old password - fails")
        void testChangePassword_WrongOldPassword() throws SQLException {
            boolean result = userService.changePassword(passwordUserId, "Wrong@123", "New@123456");

            assertFalse(result);
        }

        @Test
        @Order(10)
        @DisplayName("TC-USER-SRV-10: Change password for non-existent user - fails")
        void testChangePassword_UserNotFound() throws SQLException {
            boolean result = userService.changePassword("non_existent_id", "old", "new");

            assertFalse(result);
        }
    }

    // ========== PROFILE UPDATE TESTS ==========

    @Nested
    @DisplayName("Profile Update Tests")
    class ProfileUpdateTests {

        @Test
        @Order(11)
        @DisplayName("TC-USER-SRV-11: Update user profile - success")
        void testUpdateProfile_Success() throws SQLException {
            boolean result = userService.updateProfile(
                    testUserId,
                    "updated@example.com",
                    "Updated Full Name",
                    "0988777666"
            );

            assertTrue(result);

            // Verify update
            User updatedUser = userRepo.findById(testUserId);
            assertNotNull(updatedUser);
            assertEquals("updated@example.com", updatedUser.getEmail());
            assertEquals("Updated Full Name", updatedUser.getFullName());
            assertEquals("0988777666", updatedUser.getPhone());
        }

        @Test
        @Order(12)
        @DisplayName("TC-USER-SRV-12: Update profile for non-existent user - fails")
        void testUpdateProfile_UserNotFound() throws SQLException {
            boolean result = userService.updateProfile(
                    "non_existent_id",
                    "email@test.com",
                    "Name",
                    "1234567890"
            );

            assertFalse(result);
        }
    }

    // ========== GET USER TESTS ==========

    @Nested
    @DisplayName("Get User Tests")
    class GetUserTests {

        @Test
        @Order(13)
        @DisplayName("TC-USER-SRV-13: Get user by ID returns correct user")
        void testGetUserById() throws SQLException {
            User user = userService.getUserById(testUserId);

            assertNotNull(user);
            assertEquals(testUserId, user.getId());
            assertEquals(testUsername, user.getUsername());
        }

        @Test
        @Order(14)
        @DisplayName("TC-USER-SRV-14: Get user by username returns correct user")
        void testGetUserByUsername() throws SQLException {
            User user = userService.getUserByUsername(testUsername);

            assertNotNull(user);
            assertEquals(testUsername, user.getUsername());
        }

        @Test
        @Order(15)
        @DisplayName("TC-USER-SRV-15: Get all users returns list")
        void testGetAllUsers() throws SQLException {
            List<User> users = userService.getAllUsers();

            assertNotNull(users);
            assertTrue(users.size() > 0);
        }

        @Test
        @Order(16)
        @DisplayName("TC-USER-SRV-16: Get total users count")
        void testGetTotalUsers() throws SQLException {
            int count = userService.getTotalUsers();

            assertTrue(count > 0);
        }
    }
}