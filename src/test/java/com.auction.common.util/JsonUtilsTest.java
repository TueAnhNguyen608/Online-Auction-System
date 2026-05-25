package com.auction.common.util;

import com.auction.common.model.item.Item;
import com.auction.common.model.item.ItemStatus;
import com.auction.common.model.user.User;
import com.auction.common.model.user.UserRole;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JsonUtilsTest {

    @Nested
    @DisplayName("Serialization Tests")
    class SerializationTests {

        @Test
        @Order(1)
        @DisplayName("TC-JSON-01: Serialize User object to JSON")
        void testToJson_User() {
            User user = new User();
            user.setUsername("testuser");
            user.setEmail("test@example.com");
            user.setRole(UserRole.BIDDER);
            user.setFullName("Test User");

            String json = JsonUtils.toJson(user);

            assertNotNull(json);
            assertTrue(json.contains("testuser"));
            assertTrue(json.contains("test@example.com"));
            assertTrue(json.contains("BIDDER"));
        }

        @Test
        @Order(2)
        @DisplayName("TC-JSON-02: Serialize Item object to JSON")
        void testToJson_Item() {
            Item item = new Item();
            item.setName("Test Item");
            item.setStartingPrice(10000000);
            item.setStatus(ItemStatus.ACTIVE);
            item.setEndTime(LocalDateTime.of(2025, 12, 31, 23, 59, 59));

            String json = JsonUtils.toJson(item);

            assertNotNull(json);
            // Kiểm tra linh hoạt - không cần exact format
            assertTrue(json.contains("Test Item"));
            assertTrue(json.contains("10000000"));
            assertTrue(json.contains("ACTIVE"));
        }

        @Test
        @Order(3)
        @DisplayName("TC-JSON-03: Serialize List to JSON")
        void testToJson_List() {
            List<String> list = new ArrayList<>();
            list.add("item1");
            list.add("item2");
            list.add("item3");

            String json = JsonUtils.toJson(list);

            assertNotNull(json);
            assertTrue(json.contains("item1"));
            assertTrue(json.contains("item2"));
            assertTrue(json.contains("item3"));
        }

        @Test
        @Order(4)
        @DisplayName("TC-JSON-04: Serialize Map to JSON")
        void testToJson_Map() {
            Map<String, Object> map = new HashMap<>();
            map.put("key1", "value1");
            map.put("key2", 123);
            map.put("key3", true);

            String json = JsonUtils.toJson(map);

            assertNotNull(json);
            assertTrue(json.contains("key1"));
            assertTrue(json.contains("value1"));
            assertTrue(json.contains("123"));
        }

        @Test
        @Order(5)
        @DisplayName("TC-JSON-05: Serialize null returns empty object")
        void testToJson_Null() {
            String json = JsonUtils.toJson(null);
            assertEquals("{}", json);
        }

        @Test
        @Order(6)
        @DisplayName("TC-JSON-06: Serialize object with LocalDateTime")
        void testToJson_LocalDateTime() {
            LocalDateTime dateTime = LocalDateTime.of(2025, 6, 15, 10, 30, 0);

            String json = JsonUtils.toJson(dateTime);

            assertNotNull(json);
            // Kiểm tra linh hoạt
            assertTrue(json.contains("2025") || json.contains("2025-06-15"));
        }
    }

    @Nested
    @DisplayName("Deserialization Tests")
    class DeserializationTests {

        @Test
        @Order(7)
        @DisplayName("TC-JSON-07: Deserialize JSON to User object")
        void testFromJson_User() {
            String json = "{\"username\":\"testuser\",\"email\":\"test@example.com\",\"role\":\"BIDDER\"}";

            User user = JsonUtils.fromJson(json, User.class);

            assertNotNull(user);
            assertEquals("testuser", user.getUsername());
            assertEquals("test@example.com", user.getEmail());
        }

        @Test
        @Order(8)
        @DisplayName("TC-JSON-08: Deserialize JSON to Item object")
        void testFromJson_Item() {
            String json = "{\"name\":\"Test Laptop\",\"startingPrice\":15000000,\"status\":\"ACTIVE\"}";

            Item item = JsonUtils.fromJson(json, Item.class);

            assertNotNull(item);
            assertEquals("Test Laptop", item.getName());
            assertEquals(15000000, item.getStartingPrice());
        }

        @Test
        @Order(9)
        @DisplayName("TC-JSON-09: Deserialize JSON to List")
        void testFromJson_List() {
            String json = "[\"item1\",\"item2\",\"item3\"]";

            Type listType = new TypeToken<ArrayList<String>>(){}.getType();
            List<String> list = JsonUtils.fromJson(json, listType);

            assertNotNull(list);
            assertEquals(3, list.size());
            assertEquals("item1", list.get(0));
        }

        @Test
        @Order(10)
        @DisplayName("TC-JSON-10: Deserialize JSON to Map")
        void testFromJson_Map() {
            String json = "{\"key1\":\"value1\",\"key2\":123}";

            Type mapType = new TypeToken<HashMap<String, Object>>(){}.getType();
            Map<String, Object> map = JsonUtils.fromJson(json, mapType);

            assertNotNull(map);
            assertEquals("value1", map.get("key1"));
            assertEquals(123.0, map.get("key2"));
        }

        @ParameterizedTest
        @Order(11)
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("TC-JSON-11: Deserialize null or empty returns null")
        void testFromJson_NullOrEmpty(String invalidJson) {
            assertNull(JsonUtils.fromJson(invalidJson, String.class));
        }

        @Test
        @Order(12)
        @DisplayName("TC-JSON-12: Deserialize invalid JSON throws exception")
        void testFromJson_InvalidJson() {
            String invalidJson = "{invalid json}";

            assertThrows(com.google.gson.JsonSyntaxException.class, () -> {
                JsonUtils.fromJson(invalidJson, String.class);
            });
        }

        @Test
        @Order(13)
        @DisplayName("TC-JSON-13: Deserialize JSON with LocalDateTime")
        void testFromJson_LocalDateTime() {
            String json = "\"2025-06-15T10:30:00\"";

            LocalDateTime dateTime = JsonUtils.fromJson(json, LocalDateTime.class);

            assertNotNull(dateTime);
            assertEquals(2025, dateTime.getYear());
            assertEquals(6, dateTime.getMonthValue());
            assertEquals(15, dateTime.getDayOfMonth());
        }
    }

    @Nested
    @DisplayName("Round Trip Tests")
    class RoundTripTests {

        @Test
        @Order(14)
        @DisplayName("TC-JSON-14: User serialization-deserialization round trip")
        void testUserRoundTrip() {
            User original = new User();
            original.setUsername("roundtrip_user");
            original.setEmail("roundtrip@example.com");
            original.setRole(UserRole.ADMIN);
            original.setFullName("Round Trip User");

            String json = JsonUtils.toJson(original);
            User deserialized = JsonUtils.fromJson(json, User.class);

            assertNotNull(deserialized);
            assertEquals(original.getUsername(), deserialized.getUsername());
            assertEquals(original.getEmail(), deserialized.getEmail());
        }

        @Test
        @Order(15)
        @DisplayName("TC-JSON-15: Item serialization-deserialization round trip")
        void testItemRoundTrip() {
            Item original = new Item();
            original.setName("Round Trip Laptop");
            original.setStartingPrice(20000000);
            original.setStatus(ItemStatus.ACTIVE);

            String json = JsonUtils.toJson(original);
            Item deserialized = JsonUtils.fromJson(json, Item.class);

            assertNotNull(deserialized);
            assertEquals(original.getName(), deserialized.getName());
            assertEquals(original.getStartingPrice(), deserialized.getStartingPrice());
        }

        @Test
        @Order(16)
        @DisplayName("TC-JSON-16: List serialization-deserialization round trip")
        void testListRoundTrip() {
            List<Integer> original = List.of(1, 2, 3, 4, 5);

            String json = JsonUtils.toJson(original);
            Type listType = new TypeToken<ArrayList<Integer>>(){}.getType();
            List<Integer> deserialized = JsonUtils.fromJson(json, listType);

            assertNotNull(deserialized);
            assertEquals(original.size(), deserialized.size());
        }
    }

    @Nested
    @DisplayName("Gson Instance Tests")
    class GsonInstanceTests {

        @Test
        @Order(17)
        @DisplayName("TC-JSON-17: GetGson returns same instance")
        void testGetGson_SameInstance() {
            var gson1 = JsonUtils.getGson();
            var gson2 = JsonUtils.getGson();

            assertNotNull(gson1);
            assertSame(gson1, gson2);
        }

        @Test
        @Order(18)
        @DisplayName("TC-JSON-18: Gson instance is properly configured")
        void testGsonConfiguration() {
            var gson = JsonUtils.getGson();

            Item item = new Item();
            item.setEndTime(LocalDateTime.now());

            String json = gson.toJson(item);
            assertNotNull(json);
        }
    }
}