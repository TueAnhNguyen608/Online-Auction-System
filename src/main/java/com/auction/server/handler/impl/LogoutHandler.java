package com.auction.server.handler.impl;

import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.server.handler.RequestHandler;

public class LogoutHandler implements RequestHandler {
    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        return ResponseMessage.success("Đăng xuất thành công", null);
    }
}