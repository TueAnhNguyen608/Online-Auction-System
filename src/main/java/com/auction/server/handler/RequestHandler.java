package com.auction.server.handler;

import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;

public interface RequestHandler {
    ResponseMessage handle(Request request, String userId) throws Exception;
}