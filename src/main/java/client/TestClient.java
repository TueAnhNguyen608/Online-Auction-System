package client;

import java.io.*;
import java.net.*;
import com.google.gson.Gson;
import common.*;

public class TestClient {
    public static void main(String[] args) {
        try {
            System.out.println("1. Đang kết nối tới server...");
            Socket socket = new Socket("localhost", 9999);
            System.out.println("2. Kết nối thành công!");

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Gson gson = new Gson();

            Object[] loginData = new Object[]{"john", "123456"};
            Request request = new Request(CommandType.LOGIN, loginData);
            String jsonRequest = gson.toJson(request);
            System.out.println("3. Gửi request: " + jsonRequest);
            out.println(jsonRequest);

            System.out.println("4. Đã gửi xong, chờ response...");
            String response = in.readLine();
            System.out.println("5. Response: " + response);

            socket.close();
        } catch (Exception e) {
            System.err.println("LỖI: " + e.getMessage());
        }
    }
}