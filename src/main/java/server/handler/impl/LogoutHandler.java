package server.handler.impl;

import common.model.communication.Request;
import common.model.communication.ResponseMessage;
import server.handler.RequestHandler;

public class LogoutHandler implements RequestHandler {
    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        return ResponseMessage.success("Đăng xuất thành công", null);
    }
}