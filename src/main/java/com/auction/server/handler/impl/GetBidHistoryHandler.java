package com.auction.server.handler.impl;

import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.server.handler.RequestHandler;
import com.auction.server.service.BiddingService;

public class GetBidHistoryHandler implements RequestHandler {
    private final BiddingService biddingService = BiddingService.getInstance();

    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        String itemId = (String) request.getData();
        var history = biddingService.getBidHistory(itemId);
        return ResponseMessage.success("Lấy lịch sử đấu giá thành công", history);
    }
}