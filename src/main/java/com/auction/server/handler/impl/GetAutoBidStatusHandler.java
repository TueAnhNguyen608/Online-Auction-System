package com.auction.server.handler.impl;

import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.server.handler.RequestHandler;
import com.auction.server.service.AutoBidService;
import java.util.List;
import java.util.Map;

public class GetAutoBidStatusHandler implements RequestHandler {
    private final AutoBidService autoBidService = AutoBidService.getInstance();

    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        List<Map<String, Object>> settings = autoBidService.getAutoBidSettings(userId);
        return ResponseMessage.success("Lấy danh sách auto-bid thành công", settings);
    }
}