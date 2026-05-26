package com.auction.server.handler.impl;

import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.server.handler.RequestHandler;

public class KeepAliveHandler implements RequestHandler {

    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        // Chỉ cần trả về OK, heartbeat đã được cập nhật ở dispatcher
        return ResponseMessage.success("OK", null);
    }
}