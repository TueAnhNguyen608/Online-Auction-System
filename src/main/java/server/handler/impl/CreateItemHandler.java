package server.handler.impl;

import common.model.communication.Request;
import common.model.communication.ResponseMessage;
import common.model.item.Item;
import server.handler.RequestHandler;
import server.service.ItemService;
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