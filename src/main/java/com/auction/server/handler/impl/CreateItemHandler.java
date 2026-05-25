package com.auction.server.handler.impl;

import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.common.model.item.Item;
import com.auction.server.handler.RequestHandler;
import com.auction.server.service.ItemService;
import java.time.LocalDateTime;
import java.util.Map;

public class CreateItemHandler implements RequestHandler {
    private final ItemService itemService = ItemService.getInstance();

    @Override
    @SuppressWarnings("unchecked")
    public ResponseMessage handle(Request request, String userId) throws Exception {
        Map<String, Object> map = (Map<String, Object>) request.getData();
        Item item = new Item();
        item.setName((String) map.get("name"));
        item.setDescription((String) map.get("description"));
        item.setStartingPrice(((Number) map.get("startingPrice")).doubleValue());
        item.setCurrentPrice(item.getStartingPrice());
        item.setEndTime(LocalDateTime.now().plusHours(24));

        Item created = itemService.createItem(item, userId, userId);
        return ResponseMessage.success("Tạo sản phẩm thành công", created);
    }
}