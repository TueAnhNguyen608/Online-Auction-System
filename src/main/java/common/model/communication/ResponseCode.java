package common.model.communication;

import java.io.Serializable;

public enum ResponseCode implements Serializable {
    SUCCESS(200, "Success"),
    ERROR(500, "Internal server error"),
    AUTH_FAILED(401, "Authentication failed"),
    INVALID_REQUEST(400, "Invalid request"),
    ITEM_NOT_FOUND(404, "Item not found"),
    BID_TOO_LOW(409, "Bid too low - current price is higher"),
    AUCTION_ENDED(410, "Auction already ended"),
    USER_NOT_FOUND(404, "User not found"),
    PERMISSION_DENIED(403, "Permission denied"),
    DUPLICATE_ITEM(409, "Item already exists"),
    INVALID_AMOUNT(400, "Invalid bid amount"),
    CONNECTION_ERROR(503, "Connection error");

    private final int code;
    private final String defaultMessage;

    ResponseCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() { return code; }
    public String getDefaultMessage() { return defaultMessage; }
}

