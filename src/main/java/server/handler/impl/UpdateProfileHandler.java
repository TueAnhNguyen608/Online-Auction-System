package server.handler.impl;

import common.model.communication.Request;
import common.model.communication.ResponseMessage;
import server.handler.RequestHandler;
import server.service.UserService;
import java.util.Map;

public class UpdateProfileHandler implements RequestHandler {
    private final UserService userService = UserService.getInstance();

    @Override
    @SuppressWarnings("unchecked")
    public ResponseMessage handle(Request request, String userId) throws Exception {
        Map<String, Object> map = (Map<String, Object>) request.getData();
        String email = (String) map.get("email");
        String fullName = (String) map.get("fullName");
        String phone = (String) map.get("phone");

        boolean success = userService.updateProfile(userId, email, fullName, phone);
        if (success) {
            return ResponseMessage.success("Cập nhật thông tin thành công", null);
        }
        return ResponseMessage.error("Cập nhật thất bại");
    }
}