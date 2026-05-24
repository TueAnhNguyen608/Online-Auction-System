package server.handler;

import common.model.communication.Request;
import common.model.communication.ResponseMessage;

public interface RequestHandler {
    ResponseMessage handle(Request request, String userId) throws Exception;
}