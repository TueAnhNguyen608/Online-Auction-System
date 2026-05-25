package com.auction.server.handler.impl;

import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.server.handler.RequestHandler;
import com.auction.server.service.StatisticsService;

public class GetSystemStatsHandler implements RequestHandler {
    private final StatisticsService statsService = StatisticsService.getInstance();

    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        var stats = statsService.getSystemStats();
        return ResponseMessage.success("Lấy thống kê hệ thống thành công", stats);
    }
}