package com.auction.server.handler.impl;

import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.server.handler.RequestHandler;
import com.auction.server.service.ItemService;

public class GetActiveItemsHandler implements RequestHandler {
    private final ItemService itemService = ItemService.getInstance();

    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        var items = itemService.getActiveItems();
        return ResponseMessage.success("Lấy danh sách sản phẩm thành công", items);
    }
}