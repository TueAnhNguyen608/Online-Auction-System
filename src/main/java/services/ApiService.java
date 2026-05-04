package services;

import common.AppConfig;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Lớp chịu trách nhiệm giao tiếp với Server qua REST API.
 * Phiên bản tương thích hoàn toàn với Java 8 (Sử dụng HttpURLConnection).
 */
public class ApiService {

    private static ApiService instance;

    // Hàm khởi tạo private (Singleton Pattern)
    private ApiService() {
        // Với Java 8, ta không khởi tạo Client cố định ở đây,
        // mà sẽ tạo Connection mới cho mỗi lần gọi hàm.
    }

    public static synchronized ApiService getInstance() {
        if (instance == null) {
            instance = new ApiService();
        }
        return instance;
    }

    /**
     * HÀM MẪU: Đăng nhập
     */
    public String login(String username, String password) throws Exception {
        // 1. Tạo JSON body
        String jsonBody = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", username, password);

        // 2. Thiết lập đường dẫn URL
        URL url = new URL(AppConfig.API_BASE_URL + "/login");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            // 3. Cấu hình Request (Gửi kiểu POST, định dạng JSON)
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");

            // Cài đặt Timeout (Java 8 tính bằng mili-giây nên phải nhân 1000)
            conn.setConnectTimeout(AppConfig.TIMEOUT_SECONDS * 1000);
            conn.setReadTimeout(AppConfig.TIMEOUT_SECONDS * 1000);

            // Cho phép gửi dữ liệu (Body)
            conn.setDoOutput(true);

            // 4. Gửi dữ liệu JSON lên Server
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 5. Nhận kết quả từ Server
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) { // Mã 200 là thành công
                // Đọc dữ liệu trả về (Phải đọc từng dòng gộp lại)
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return response.toString(); // Trả về data thành công
                }
            } else {
                // Mã lỗi (VD: 401, 404, 500...)
                throw new Exception("Lỗi từ server: " + responseCode);
            }
        } finally {
            // 6. Luôn luôn nhớ đóng kết nối để giải phóng RAM
            conn.disconnect();
        }
    }
    /**
     * HÀM MỚI: Đăng ký tài khoản
     */
    public String register(String username, String password, String role) throws Exception {
        // 1. Tạo JSON body gửi lên Server (có thêm trường role)
        String jsonBody = String.format("{\"username\":\"%s\", \"password\":\"%s\", \"role\":\"%s\"}", username, password, role);

        // 2. Thiết lập đường dẫn URL (gọi vào API /register)
        URL url = new URL(AppConfig.API_BASE_URL + "/register");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            // 3. Cấu hình Request
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");

            conn.setConnectTimeout(AppConfig.TIMEOUT_SECONDS * 1000);
            conn.setReadTimeout(AppConfig.TIMEOUT_SECONDS * 1000);
            conn.setDoOutput(true);

            // 4. Gửi dữ liệu JSON lên Server
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 5. Nhận kết quả từ Server
            int responseCode = conn.getResponseCode();

            // HTTP 200 (OK) hoặc 201 (Created) đều tính là đăng ký thành công
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return response.toString();
                }
            } else {
                throw new Exception("Lỗi từ server: " + responseCode);
            }
        } finally {
            conn.disconnect();
        }
    }
}