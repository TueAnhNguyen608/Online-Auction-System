package com.auction.server.handler.impl;

import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.server.handler.RequestHandler;
import com.auction.server.service.BiddingService;

public class WonProductsHandler implements RequestHandler {
    private final BiddingService biddingService = BiddingService.getInstance();

    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        var wonItems = biddingService.getWonProducts(userId);
        return ResponseMessage.success("Lấy danh sách sản phẩm đã thắng thành công", wonItems);
    }
}