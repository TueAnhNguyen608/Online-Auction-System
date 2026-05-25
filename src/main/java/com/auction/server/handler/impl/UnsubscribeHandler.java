package com.auction.server.handler.impl;

import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.server.broadcast.EventBroadcaster;
import com.auction.server.handler.ClientConnectionHandler;
import com.auction.server.handler.RequestHandler;
import com.auction.server.handler.SubscribableHandler;

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

        if (clientHandler != null) {
            EventBroadcaster.getInstance().unsubscribe(itemId, clientHandler);
            System.out.println("📡 " + userId + " unsubscribed from item: " + itemId);
        }

        return ResponseMessage.success("Đã hủy theo dõi sản phẩm: " + itemId, null);
    }
}