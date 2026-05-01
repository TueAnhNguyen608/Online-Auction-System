package common;

public class InvalidBidException extends Exception {
    private double attemptedBid;
    private double minimumRequired;
    private String itemId;

    public InvalidBidException(String message) {
        super(message);
    }

    public InvalidBidException(String message, double attemptedBid, double minimumRequired) {
        super(message);
        this.attemptedBid = attemptedBid;
        this.minimumRequired = minimumRequired;
    }

    public InvalidBidException(String message, String itemId, double attemptedBid, double minimumRequired) {
        super(message);
        this.itemId = itemId;
        this.attemptedBid = attemptedBid;
        this.minimumRequired = minimumRequired;
    }

    public double getAttemptedBid() { return attemptedBid; }
    public double getMinimumRequired() { return minimumRequired; }
    public String getItemId() { return itemId; }
}