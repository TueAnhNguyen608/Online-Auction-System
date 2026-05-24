package server.handler.impl;

import common.model.communication.Request;
import common.model.communication.ResponseMessage;
import server.handler.RequestHandler;
import server.service.BiddingService;

public class GetPriceHistoryHandler implements RequestHandler {
    private final BiddingService biddingService = BiddingService.getInstance();

    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        String itemId = (String) request.getData();
        var priceHistory = biddingService.getPriceHistory(itemId);
        return ResponseMessage.success("Lấy lịch sử giá thành công", priceHistory);
    }
}