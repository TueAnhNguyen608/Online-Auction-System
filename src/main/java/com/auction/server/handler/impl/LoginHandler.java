package com.auction.server.handler.impl;

import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.common.model.user.User;
import com.auction.server.handler.RequestHandler;
import com.auction.server.service.UserService;
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