package com.auction.server.handler;

public interface SubscribableHandler {
    void setClientHandler(ClientConnectionHandler handler);
}