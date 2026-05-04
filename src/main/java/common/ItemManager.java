package common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ItemManager {
    // Tên file lưu trữ dữ liệu của bạn (nếu của bạn tên khác thì đổi lại nhé)
    private static final String FILE_PATH = "items.json";

    // "Dạy" Gson cách đọc/ghi kiểu thời gian LocalDateTime
    private Gson getCustomGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, jsonContext) -> LocalDateTime.parse(json.getAsJsonPrimitive().getAsString()))
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (localDateTime, type, jsonContext) -> new JsonPrimitive(localDateTime.toString()))
                .create();
    }

    public List<Item> loadItems() {
        try (Reader reader = new FileReader(FILE_PATH)) {
            Gson gson = getCustomGson();
            Type listType = new TypeToken<ArrayList<Item>>(){}.getType();
            List<Item> items = gson.fromJson(reader, listType);
            return (items != null) ? items : new ArrayList<>();
        } catch (Exception e) {
            System.out.println("Chưa có file dữ liệu hoặc lỗi đọc file, tạo danh sách trống.");
            return new ArrayList<>();
        }
    }

    public void saveItems(List<Item> itemList) {
        try (Writer writer = new FileWriter(FILE_PATH)) {
            Gson gson = getCustomGson();
            gson.toJson(itemList, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}