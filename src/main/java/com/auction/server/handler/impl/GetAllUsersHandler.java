package com.auction.server.handler.impl;

import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.server.handler.RequestHandler;
import com.auction.server.service.UserService;
import java.util.List;
import com.auction.common.model.user.User;

public class GetAllUsersHandler implements RequestHandler {

    private final UserService userService = UserService.getInstance();

    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        List<User> users = userService.getAllUsers();
        return ResponseMessage.success("Lấy danh sách người dùng thành công", users);
    }
}