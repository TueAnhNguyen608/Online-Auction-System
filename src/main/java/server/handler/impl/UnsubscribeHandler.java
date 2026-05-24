package server.handler.impl;

import common.model.communication.Request;
import common.model.communication.ResponseMessage;
import server.handler.ClientConnectionHandler;
import server.handler.RequestHandler;
import server.handler.SubscribableHandler;

public class UnsubscribeHandler implements RequestHandler, SubscribableHandler {
    private ClientConnectionHandler clientHandler;

    @Override
    public void setClientHandler(ClientConnectionHandler handler) {
        this.clientHandler = handler;
    }

    @Override
    public ResponseMessage handle(Request request, String userId) throws Exception {
        String itemId = (String) request.getData();
        if (itemId == null || itemId.trim().isEmpty()) {
            return ResponseMessage.error("itemId không được để trống");
        }

        return ResponseMessage.success("Đã hủy theo dõi sản phẩm: " + itemId, null);
    }
}