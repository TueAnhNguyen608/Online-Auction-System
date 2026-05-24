package server.handler.impl;

import common.model.communication.Request;
import common.model.communication.ResponseMessage;
import server.handler.RequestHandler;
import server.service.ItemService;

public class GetActiveItemsHandler implements RequestHandler {
    private final ItemService itemService = ItemService.getInstance();

    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        var items = itemService.getActiveItems();
        return ResponseMessage.success("Lấy danh sách sản phẩm thành công", items);
    }
}