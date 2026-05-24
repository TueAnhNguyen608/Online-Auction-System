package server.handler.impl;

import common.model.communication.Request;
import common.model.communication.ResponseMessage;
import server.handler.RequestHandler;
import server.service.AutoBidService;

import java.util.List;
import java.util.Map;

public class GetAutoBidStatusHandler implements RequestHandler {
    private final AutoBidService autoBidService = AutoBidService.getInstance();

    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        List<Map<String, Object>> settings = autoBidService.getAutoBidSettings(userId);
        return ResponseMessage.success("Lấy danh sách auto-bid thành công", settings);
    }
}