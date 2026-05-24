package common.model.bidding;

import java.io.Serializable;

public class BidResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Bid bid;
    private final boolean extended;
    private final String message;

    public BidResult(Bid bid, boolean extended) {
        this.bid = bid;
        this.extended = extended;
        this.message = extended ? "Bid placed successfully (auction extended)" : "Bid placed successfully";
    }

    public BidResult(Bid bid, boolean extended, String message) {
        this.bid = bid;
        this.extended = extended;
        this.message = message;
    }

    public Bid getBid() {
        return bid;
    }

    public boolean isExtended() {
        return extended;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "BidResult{bid=" + bid + ", extended=" + extended + ", message='" + message + "'}";
    }
}