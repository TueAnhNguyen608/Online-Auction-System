package server.handler.impl;

import common.model.communication.Request;
import common.model.communication.ResponseMessage;
import common.model.user.User;
import server.handler.RequestHandler;
import server.service.UserService;
import java.util.List;

public class LoginHandler implements RequestHandler {
    private final UserService userService = UserService.getInstance();

    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        List<?> list = (List<?>) request.getData();
        String username = (String) list.get(0);
        String password = (String) list.get(1);

        System.out.println("🔐 Login attempt: " + username);

        try {
            User user = userService.login(username, password);

            // Tạo user copy không có password
            User safeUser = new User();
            safeUser.setId(user.getId());
            safeUser.setUsername(user.getUsername());
            safeUser.setEmail(user.getEmail());
            safeUser.setRole(user.getRole());
            safeUser.setFullName(user.getFullName());
            safeUser.setPhone(user.getPhone());
            safeUser.setBalance(user.getBalance());

            System.out.println("✅ Login success: " + username);
            return ResponseMessage.success("Đăng nhập thành công", safeUser);

        } catch (Exception e) {
            System.err.println("❌ Login failed: " + username + " - " + e.getMessage());
            return ResponseMessage.error("Đăng nhập thất bại: " + e.getMessage());
        }
    }
}