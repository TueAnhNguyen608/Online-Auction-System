package com.auction.server.handler.impl;

import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.server.handler.RequestHandler;
import com.auction.server.service.BiddingService;
import java.util.Map;

public class AutoBidHandler implements RequestHandler {
    private final BiddingService biddingService = BiddingService.getInstance();

    @Override
    @SuppressWarnings("unchecked")
    public ResponseMessage handle(Request request, String userId) throws Exception {
        if (userId == null) {
            return ResponseMessage.error("Vui lòng đăng nhập trước");
        }
        Map<String, Object> map = (Map<String, Object>) request.getData();
        String itemId = (String) map.get("itemId");
        double maxAmount = ((Number) map.get("maxAmount")).doubleValue();

        boolean success = biddingService.setAutoBid(userId, itemId, maxAmount);
        if (success) {
            return ResponseMessage.success("Đã cài đặt auto-bid với giá tối đa: " + String.format("%,.0f", maxAmount) + " VND", null);
        }
        return ResponseMessage.error("Cài đặt auto-bid thất bại");
    }
}