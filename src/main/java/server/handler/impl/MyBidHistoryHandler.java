package server.handler.impl;

import common.model.communication.Request;
import common.model.communication.ResponseMessage;
import server.handler.RequestHandler;
import server.service.BiddingService;

public class MyBidHistoryHandler implements RequestHandler {
    private final BiddingService biddingService = BiddingService.getInstance();

    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        var history = biddingService.getMyBidHistory(userId);
        return ResponseMessage.success("Lấy lịch sử đấu giá của bạn thành công", history);
    }
}