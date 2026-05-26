package com.auction.server.handler.impl;

import com.auction.common.constant.AuctionConstants;
import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.common.model.bidding.Bid;
import com.auction.common.model.bidding.BidResult;
import com.auction.server.handler.RequestHandler;
import com.auction.server.service.BiddingService;
import java.util.Map;

public class BidHandler implements RequestHandler {
    private final BiddingService biddingService = BiddingService.getInstance();

    @Override
    @SuppressWarnings("unchecked")
    public ResponseMessage handle(Request request, String userId) throws Exception {
        if (userId == null) {
            return ResponseMessage.error("Vui lòng đăng nhập trước");
        }

        Map<String, Object> map = (Map<String, Object>) request.getData();

        // 🔥 THÊM: Input validation
        String itemId = (String) map.get("itemId");
        if (itemId == null || itemId.trim().isEmpty()) {
            return ResponseMessage.error("itemId không được để trống");
        }

        Number amountNum = (Number) map.get("amount");
        if (amountNum == null) {
            return ResponseMessage.error("Số tiền không được để trống");
        }

        double amount = amountNum.doubleValue();
        if (amount <= 0) {
            return ResponseMessage.error("Số tiền phải lớn hơn 0");
        }

        if (amount > AuctionConstants.MAX_BID_AMOUNT) {
            return ResponseMessage.error("Số tiền không được vượt quá " +
                    String.format("%,.0f", AuctionConstants.MAX_BID_AMOUNT) + " VND");
        }

        Bid bid = new Bid();
        bid.setItemId(itemId);
        bid.setAmount(amount);
        bid.setBidderId(userId);
        bid.setBidderName(userId);
        bid.setAutoBid(map.containsKey("isAutoBid") && (Boolean) map.get("isAutoBid"));

        BidResult result = biddingService.placeBid(bid);

        return ResponseMessage.success(result.getMessage(), result);
    }
}