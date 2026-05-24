package server.handler.impl;

import common.model.communication.Request;
import common.model.communication.ResponseMessage;
import server.handler.RequestHandler;
import server.service.UserService;

import java.util.List;
import common.model.user.User;

public class GetAllUsersHandler implements RequestHandler {

    private final UserService userService = UserService.getInstance();

    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        List<User> users = userService.getAllUsers();
        return ResponseMessage.success("Lấy danh sách người dùng thành công", users);
    }
}