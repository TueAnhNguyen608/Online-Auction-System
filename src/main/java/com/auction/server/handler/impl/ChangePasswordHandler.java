package com.auction.server.handler.impl;

import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.server.handler.RequestHandler;
import com.auction.server.service.UserService;
import java.util.Map;

public class ChangePasswordHandler implements RequestHandler {

    private final UserService userService = UserService.getInstance();

    @Override
    @SuppressWarnings("unchecked")
    public ResponseMessage handle(Request request, String userId) throws Exception {
        Map<String, String> map = (Map<String, String>) request.getData();
        String oldPassword = map.get("oldPassword");
        String newPassword = map.get("newPassword");

        if (newPassword == null || newPassword.length() < 6) {
            return ResponseMessage.error("Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        boolean success = userService.changePassword(userId, oldPassword, newPassword);
        if (success) {
            return ResponseMessage.success("Đổi mật khẩu thành công", null);
        }
        return ResponseMessage.error("Mật khẩu cũ không đúng");
    }
}