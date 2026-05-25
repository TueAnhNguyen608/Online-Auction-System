package com.auction.common.model.communication;

import java.io.Serializable;

public enum CommandType implements Serializable {
    // AUTHENTICATION
    LOGIN,
    LOGOUT,
    REGISTER,

    // ITEM MANAGEMENT
    GET_ITEMS,
    GET_ACTIVE_ITEMS,
    GET_ITEM_DETAIL,
    CREATE_ITEM,
    END_AUCTION,
    DELETE_ITEM,

    // BIDDING
    PLACE_BID,
    GET_BID_HISTORY,

    // AUTO BID
    SET_AUTO_BID,
    CANCEL_AUTO_BID,
    GET_AUTO_BID_STATUS,

    // USER PROFILE
    GET_MY_BID_HISTORY,
    GET_WON_PRODUCTS,
    GET_PRICE_HISTORY,
    CHANGE_PASSWORD,
    UPDATE_USER_INFO,
    UPDATE_PROFILE,

    // ADMIN
    GET_ALL_USERS,
    GET_ALL_BIDS,
    GET_SYSTEM_STATS,

    // REALTIME
    SUBSCRIBE_ITEM,
    UNSUBSCRIBE_ITEM,

    // SYSTEM
    KEEP_ALIVE,
    DISCONNECT
}