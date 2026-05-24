package server.handler.impl;

import common.model.communication.Request;
import common.model.communication.ResponseMessage;
import common.model.bidding.Bid;
import server.handler.RequestHandler;
import server.service.BiddingService;

import java.util.List;

public class BidHistoryHandler implements RequestHandler {

    private final BiddingService biddingService = BiddingService.getInstance();

    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        String itemId = (String) request.getData();
        List<Bid> history = biddingService.getBidHistory(itemId);
        return ResponseMessage.success("Lấy lịch sử đấu giá thành công", history);
    }
}