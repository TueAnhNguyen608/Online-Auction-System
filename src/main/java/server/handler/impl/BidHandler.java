package server.handler.impl;

import common.model.communication.Request;
import common.model.communication.ResponseMessage;
import common.model.bidding.Bid;
import common.model.bidding.BidResult;
import server.handler.RequestHandler;
import server.service.BiddingService;
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

        Bid bid = new Bid();
        bid.setItemId((String) map.get("itemId"));
        bid.setAmount(((Number) map.get("amount")).doubleValue());
        bid.setBidderId(userId);
        bid.setBidderName(userId);
        bid.setAutoBid(map.containsKey("isAutoBid") && (Boolean) map.get("isAutoBid"));

        BidResult result = biddingService.placeBid(bid);
        return ResponseMessage.success(result.getMessage(), result);
    }
}