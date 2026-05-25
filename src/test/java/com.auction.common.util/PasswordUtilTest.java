package com.auction.common.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PasswordUtilTest {

    @Nested
    @DisplayName("Hash Function Tests")
    class HashTests {

        @Test
        @Order(1)
        @DisplayName("TC-PWD-01: Hash returns non-null string with salt separator")
        void testHash_ReturnsValidFormat() {
            String hash = PasswordUtil.hash("myPassword123");

            assertNotNull(hash);
            assertTrue(hash.contains(":"));
            assertEquals(2, hash.split(":").length);
        }

        @Test
        @Order(2)
        @DisplayName("TC-PWD-02: Same password produces different hashes (due to random salt)")
        void testHash_DifferentForSamePassword() {
            String password = "samePassword123";
            String hash1 = PasswordUtil.hash(password);
            String hash2 = PasswordUtil.hash(password);

            assertNotEquals(hash1, hash2);

            // Both should verify correctly
            assertTrue(PasswordUtil.verify(password, hash1));
            assertTrue(PasswordUtil.verify(password, hash2));
        }

        @ParameterizedTest
        @Order(3)
        @ValueSource(strings = {
                "short",
                "normalPassword123",
                "veryLongPasswordWithSpecialChars!@#$%^&*()",
                "1234567890"
        })
        @DisplayName("TC-PWD-03: Hash works with various password lengths")
        void testHash_VariousLengths(String password) {
            String hash = PasswordUtil.hash(password);

            assertNotNull(hash);
            assertTrue(hash.contains(":"));
            assertTrue(PasswordUtil.verify(password, hash));
        }

        @Test
        @Order(4)
        @DisplayName("TC-PWD-04: Hash throws exception for empty password")
        void testHash_EmptyPassword() {
            assertThrows(IllegalArgumentException.class, () -> {
                PasswordUtil.hash("");
            });
        }

        @Test
        @Order(5)
        @DisplayName("TC-PWD-05: Hash throws exception for null password")
        void testHash_NullPassword() {
            assertThrows(IllegalArgumentException.class, () -> {
                PasswordUtil.hash(null);
            });
        }
    }

    @Nested
    @DisplayName("Verify Function Tests")
    class VerifyTests {

        private String testPassword = "verifyTest123";
        private String testHash;

        @BeforeEach
        void setUp() {
            testHash = PasswordUtil.hash(testPassword);
        }

        @Test
        @Order(6)
        @DisplayName("TC-PWD-06: Verify correct password returns true")
        void testVerify_CorrectPassword() {
            assertTrue(PasswordUtil.verify(testPassword, testHash));
        }

        @Test
        @Order(7)
        @DisplayName("TC-PWD-07: Verify wrong password returns false")
        void testVerify_WrongPassword() {
            assertFalse(PasswordUtil.verify("wrongPassword", testHash));
        }

        @Test
        @Order(8)
        @DisplayName("TC-PWD-08: Verify with null hash returns false")
        void testVerify_NullHash() {
            assertFalse(PasswordUtil.verify(testPassword, null));
        }

        @Test
        @Order(9)
        @DisplayName("TC-PWD-09: Verify with invalid format hash returns false")
        void testVerify_InvalidHashFormat() {
            assertFalse(PasswordUtil.verify(testPassword, "invalid_format"));
            assertFalse(PasswordUtil.verify(testPassword, "only_one_part"));
            assertFalse(PasswordUtil.verify(testPassword, "part1:part2:part3"));
        }

        @Test
        @Order(10)
        @DisplayName("TC-PWD-10: Verify with empty password returns false")
        void testVerify_EmptyPassword() {
            assertFalse(PasswordUtil.verify("", testHash));
        }

        @Test
        @Order(11)
        @DisplayName("TC-PWD-11: Verify with null password returns false")
        void testVerify_NullPassword() {
            assertFalse(PasswordUtil.verify(null, testHash));
        }

        @ParameterizedTest
        @Order(12)
        @CsvSource({
                "password123, password123",
                "CaseSensitive, CaseSensitive",
                "special!@#, special!@#"
        })
        @DisplayName("TC-PWD-12: Verify is case-sensitive")
        void testVerify_CaseSensitive(String original, String attempt) {
            String hash = PasswordUtil.hash(original);

            if (original.equals(attempt)) {
                assertTrue(PasswordUtil.verify(attempt, hash));
            } else {
                assertFalse(PasswordUtil.verify(attempt, hash));
            }
        }
    }

    @Nested
    @DisplayName("Strong Password Tests")
    class StrongPasswordTests {

        @Test
        @Order(13)
        @DisplayName("TC-PWD-13: Valid strong passwords")
        void testStrongPassword_Valid() {
            assertTrue(PasswordUtil.isStrongPassword("Abc123456"));
            assertTrue(PasswordUtil.isStrongPassword("StrongP@ss1"));
            assertTrue(PasswordUtil.isStrongPassword("HelloWorld123"));
            assertTrue(PasswordUtil.isStrongPassword("Aa1!bbbb"));
        }

        @Test
        @Order(14)
        @DisplayName("TC-PWD-14: Invalid - missing uppercase")
        void testStrongPassword_NoUppercase() {
            assertFalse(PasswordUtil.isStrongPassword("abc123456"));
            assertFalse(PasswordUtil.isStrongPassword("alllowercase1"));
        }

        @Test
        @Order(15)
        @DisplayName("TC-PWD-15: Invalid - missing lowercase")
        void testStrongPassword_NoLowercase() {
            assertFalse(PasswordUtil.isStrongPassword("ABC123456"));
            assertFalse(PasswordUtil.isStrongPassword("ALLCAPS1"));
        }

        @Test
        @Order(16)
        @DisplayName("TC-PWD-16: Invalid - missing digit")
        void testStrongPassword_NoDigit() {
            assertFalse(PasswordUtil.isStrongPassword("Abcdefgh"));
            assertFalse(PasswordUtil.isStrongPassword("NoNumbersHere"));
        }

        @Test
        @Order(17)
        @DisplayName("TC-PWD-17: Invalid - too short (less than 6 chars)")
        void testStrongPassword_TooShort() {
            assertFalse(PasswordUtil.isStrongPassword("Ab1"));
            assertFalse(PasswordUtil.isStrongPassword("Aa1"));
            assertFalse(PasswordUtil.isStrongPassword("Abc12"));
        }

        @ParameterizedTest
        @Order(18)
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("TC-PWD-18: Invalid - null or empty")
        void testStrongPassword_NullOrEmpty(String invalid) {
            assertFalse(PasswordUtil.isStrongPassword(invalid));
        }

        @ParameterizedTest
        @Order(19)
        @ValueSource(strings = {
                "Valid1",           // Minimum length 6
                "Abcdef1",          // Good
                "XyZ99!",           // With special char
                "LongPasswordWithNumbers123"
        })
        @DisplayName("TC-PWD-19: Various valid strong passwords")
        void testStrongPassword_VariousValid(String password) {
            assertTrue(PasswordUtil.isStrongPassword(password));
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @Order(20)
        @DisplayName("TC-PWD-20: Hash then verify workflow works correctly")
        void testHashThenVerifyWorkflow() {
            String originalPassword = "IntegrationTest123";

            // Hash the password
            String hashedPassword = PasswordUtil.hash(originalPassword);

            // Verify with correct password
            assertTrue(PasswordUtil.verify(originalPassword, hashedPassword));

            // Verify with wrong password
            assertFalse(PasswordUtil.verify("WrongPassword", hashedPassword));

            // Re-hash and verify again
            String newHash = PasswordUtil.hash(originalPassword);
            assertNotEquals(hashedPassword, newHash);
            assertTrue(PasswordUtil.verify(originalPassword, newHash));
        }

        @Test
        @Order(21)
        @DisplayName("TC-PWD-21: Multiple hash-verify cycles")
        void testMultipleHashVerifyCycles() {
            String[] passwords = {"pwd1", "pwd2", "pwd3"};

            for (String pwd : passwords) {
                String hash = PasswordUtil.hash(pwd);
                for (int i = 0; i < 10; i++) {
                    assertTrue(PasswordUtil.verify(pwd, hash));
                    assertFalse(PasswordUtil.verify(pwd + "wrong", hash));
                }
            }
        }
    }
}