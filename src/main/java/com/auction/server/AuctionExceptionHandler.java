package com.auction.server;

import com.auction.common.model.communication.ResponseCode;
import com.auction.common.model.exception.AuctionClosedException;
import com.auction.common.model.exception.AuthenticationException;
import com.auction.common.model.exception.DataValidationException;
import com.auction.common.model.exception.InvalidBidException;

public class AuctionExceptionHandler {

    public static String getErrorMessage(Exception e) {
        if (e instanceof InvalidBidException) {
            InvalidBidException ibe = (InvalidBidException) e;
            return String.format("❌ Giá đặt không hợp lệ! Cần đặt tối thiểu: %.0f VND", ibe.getMinimumRequired());
        }
        if (e instanceof AuctionClosedException) {
            AuctionClosedException ace = (AuctionClosedException) e;
            return "❌ " + ace.getMessage();
        }
        if (e instanceof AuthenticationException) {
            return "❌ Đăng nhập thất bại: " + e.getMessage();
        }
        if (e instanceof DataValidationException) {
            DataValidationException dve = (DataValidationException) e;
            return "❌ Dữ liệu không hợp lệ: " + dve.getFormattedErrors();
        }
        if (e instanceof java.sql.SQLException) {
            return "❌ Lỗi cơ sở dữ liệu: " + e.getMessage();
        }
        return "❌ Lỗi hệ thống: " + e.getMessage();
    }

    public static ResponseCode getResponseCode(Exception e) {
        if (e instanceof InvalidBidException) return ResponseCode.BID_TOO_LOW;
        if (e instanceof AuctionClosedException) return ResponseCode.AUCTION_ENDED;
        if (e instanceof AuthenticationException) return ResponseCode.AUTH_FAILED;
        if (e instanceof DataValidationException) return ResponseCode.INVALID_REQUEST;
        return ResponseCode.ERROR;
    }
}