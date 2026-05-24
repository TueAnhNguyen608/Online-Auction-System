package server.handler.impl;

import common.model.communication.Request;
import common.model.communication.ResponseMessage;
import common.model.user.User;
import server.handler.RequestHandler;
import server.service.UserService;
import java.util.Map;

public class RegisterHandler implements RequestHandler {
    private final UserService userService = UserService.getInstance();

    @Override
    @SuppressWarnings("unchecked")
    public ResponseMessage handle(Request request, String userId) throws Exception {
        Map<String, Object> map = (Map<String, Object>) request.getData();
        User user = new User();
        user.setUsername((String) map.get("username"));
        user.setPassword((String) map.get("password"));
        user.setEmail((String) map.get("email"));
        user.setRole((String) map.get("role"));

        boolean success = userService.register(user);
        if (success) {
            return ResponseMessage.success("Đăng ký thành công", null);
        }
        return ResponseMessage.error("Username đã tồn tại");
    }
}