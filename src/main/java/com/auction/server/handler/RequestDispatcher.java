package com.auction.server.handler;

import com.auction.common.model.communication.CommandType;
import com.auction.common.model.communication.Request;
import com.auction.common.model.communication.ResponseMessage;
import com.auction.common.model.communication.ResponseCode;
import com.auction.common.model.user.User;
import com.auction.server.AuctionExceptionHandler;
import com.auction.server.handler.impl.*;

import java.util.EnumMap;
import java.util.Map;

public class RequestDispatcher {

    private static volatile RequestDispatcher instance;
    private final Map<CommandType, RequestHandler> handlers = new EnumMap<>(CommandType.class);

    private RequestDispatcher() {
        registerHandlers();
    }

    public static RequestDispatcher getInstance() {
        if (instance == null) {
            synchronized (RequestDispatcher.class) {
                if (instance == null) {
                    instance = new RequestDispatcher();
                }
            }
        }
        return instance;
    }

    private void registerHandlers() {
        handlers.put(CommandType.LOGIN, new LoginHandler());
        handlers.put(CommandType.REGISTER, new RegisterHandler());
        handlers.put(CommandType.LOGOUT, new LogoutHandler());
        handlers.put(CommandType.PLACE_BID, new BidHandler());
        handlers.put(CommandType.GET_ACTIVE_ITEMS, new GetActiveItemsHandler());
        handlers.put(CommandType.CREATE_ITEM, new CreateItemHandler());
        handlers.put(CommandType.GET_BID_HISTORY, new GetBidHistoryHandler());
        handlers.put(CommandType.GET_MY_BID_HISTORY, new MyBidHistoryHandler());
        handlers.put(CommandType.GET_WON_PRODUCTS, new WonProductsHandler());
        handlers.put(CommandType.GET_PRICE_HISTORY, new GetPriceHistoryHandler());
        handlers.put(CommandType.SET_AUTO_BID, new AutoBidHandler());
        handlers.put(CommandType.CANCEL_AUTO_BID, new CancelAutoBidHandler());
        handlers.put(CommandType.GET_AUTO_BID_STATUS, new GetAutoBidStatusHandler());
        handlers.put(CommandType.CHANGE_PASSWORD, new ChangePasswordHandler());
        handlers.put(CommandType.UPDATE_PROFILE, new UpdateProfileHandler());
        handlers.put(CommandType.GET_ALL_USERS, new GetAllUsersHandler());
        handlers.put(CommandType.GET_SYSTEM_STATS, new GetSystemStatsHandler());
        handlers.put(CommandType.SUBSCRIBE_ITEM, new SubscribeHandler());
        handlers.put(CommandType.UNSUBSCRIBE_ITEM, new UnsubscribeHandler());
        handlers.put(CommandType.KEEP_ALIVE, new KeepAliveHandler());
    }

    public ResponseMessage dispatch(Request request, ClientConnectionHandler clientHandler) {
        RequestHandler handler = handlers.get(request.getCommand());
        if (handler == null) {
            return ResponseMessage.error("Unknown command: " + request.getCommand());
        }

        // 🔥 THÊM ĐOẠN NÀY: set clientHandler cho các handler hỗ trợ subscribe
        if (handler instanceof SubscribableHandler && clientHandler != null) {
            ((SubscribableHandler) handler).setClientHandler(clientHandler);
        }

        try {
            String userId = clientHandler != null ? clientHandler.getCurrentUserId() : null;
            ResponseMessage response = handler.handle(request, userId);

            if (request.getCommand() == CommandType.LOGIN &&
                    response != null &&
                    response.getCode() == ResponseCode.SUCCESS &&
                    clientHandler != null) {

                Object data = response.getData();
                if (data instanceof User) {
                    User user = (User) data;
                    clientHandler.setCurrentUserId(user.getId());
                    clientHandler.setCurrentUsername(user.getUsername());
                    System.out.println("✅ Set user info: " + user.getUsername());
                }
            }
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseMessage.error(AuctionExceptionHandler.getErrorMessage(e));
        }
    }

    public void start() { System.out.println("✅ RequestDispatcher started"); }
    public void stop() { System.out.println("🛑 RequestDispatcher stopped"); }
    public boolean isRunning() { return true; }
}