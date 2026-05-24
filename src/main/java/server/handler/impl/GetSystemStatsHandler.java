package server.handler.impl;

import common.model.communication.Request;
import common.model.communication.ResponseMessage;
import server.handler.RequestHandler;
import server.service.StatisticsService;

public class GetSystemStatsHandler implements RequestHandler {
    private final StatisticsService statsService = StatisticsService.getInstance();

    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        var stats = statsService.getSystemStats();
        return ResponseMessage.success("Lấy thống kê hệ thống thành công", stats);
    }
}