package com.auction.server.handler.impl;

import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.server.handler.RequestHandler;
import com.auction.server.service.BiddingService;

public class MyBidHistoryHandler implements RequestHandler {
    private final BiddingService biddingService = BiddingService.getInstance();

    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        var history = biddingService.getMyBidHistory(userId);
        return ResponseMessage.success("Lấy lịch sử đấu giá của bạn thành công", history);
    }
}