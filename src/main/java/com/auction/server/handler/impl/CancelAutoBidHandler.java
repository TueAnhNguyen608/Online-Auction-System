package com.auction.server.handler.impl;

import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.server.handler.RequestHandler;
import com.auction.server.service.BiddingService;
import java.util.Map;

public class CancelAutoBidHandler implements RequestHandler {
    private final BiddingService biddingService = BiddingService.getInstance();

    @Override
    @SuppressWarnings("unchecked")
    public ResponseMessage handle(Request request, String userId) throws Exception {
        if (userId == null) {
            return ResponseMessage.error("Vui lòng đăng nhập trước");
        }

        Map<String, Object> map = (Map<String, Object>) request.getData();
        String itemId = (String) map.get("itemId");

        boolean success = biddingService.cancelAutoBid(userId, itemId);
        if (success) {
            return ResponseMessage.success("Đã hủy auto-bid thành công", null);
        }
        return ResponseMessage.error("Hủy auto-bid thất bại");
    }
}