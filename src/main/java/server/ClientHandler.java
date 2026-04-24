package server;

import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import common.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ClientHandler implements Runnable {
    private Socket socket;
    private DatabaseManager dbManager;
    private Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public ClientHandler(Socket socket, DatabaseManager dbManager) {
        this.socket = socket;
        this.dbManager = dbManager;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("📨 Received: " + line);

                Request request = gson.fromJson(line, Request.class);
                ResponseMessage response = process(request);

                String jsonResponse = gson.toJson(response);
                System.out.println("📤 Sending: " + jsonResponse);
                out.println(jsonResponse);

                if (request.getCommand() == CommandType.LOGOUT) break;
            }
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ResponseMessage process(Request request) {
        try {
            switch (request.getCommand()) {
                case LOGIN:
                    return handleLogin(request.getData());
                default:
                    return ResponseMessage.error("Command not implemented");
            }
        } catch (Exception e) {
            return ResponseMessage.error(e.getMessage());
        }
    }


    private ResponseMessage handleLogin(Object data) throws SQLException {
        String username = null;
        String password = null;

        if (data instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) data;
            username = (String) list.get(0);
            password = (String) list.get(1);
        } else if (data instanceof Object[]) {
            Object[] arr = (Object[]) data;
            username = (String) arr[0];
            password = (String) arr[1];
        }

        System.out.println("🔐 Login attempt: " + username + "/" + password);

        User user = dbManager.login(username, password);
        if (user != null) {
            return ResponseMessage.success("Login success", user);
        }
        return ResponseMessage.authFailed("Sai tên đăng nhập hoặc mật khẩu");
    }
}