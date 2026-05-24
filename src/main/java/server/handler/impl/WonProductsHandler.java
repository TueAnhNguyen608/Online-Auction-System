package server.handler.impl;

import common.model.communication.Request;
import common.model.communication.ResponseMessage;
import server.handler.RequestHandler;
import server.service.BiddingService;

public class WonProductsHandler implements RequestHandler {
    private final BiddingService biddingService = BiddingService.getInstance();

    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        var wonItems = biddingService.getWonProducts(userId);
        return ResponseMessage.success("Lấy danh sách sản phẩm đã thắng thành công", wonItems);
    }
}