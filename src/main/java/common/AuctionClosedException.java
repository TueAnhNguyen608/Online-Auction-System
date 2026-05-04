package common;

public class AuctionClosedException extends Exception {
    private String itemId;
    private String winnerId;
    private double finalPrice;

    public AuctionClosedException(String message) {
        super(message);
    }

    public AuctionClosedException(String message, String itemId) {
        super(message);
        this.itemId = itemId;
    }

    public AuctionClosedException(String message, String itemId, String winnerId, double finalPrice) {
        super(message);
        this.itemId = itemId;
        this.winnerId = winnerId;
        this.finalPrice = finalPrice;
    }

    public String getItemId() { return itemId; }
    public String getWinnerId() { return winnerId; }
    public double getFinalPrice() { return finalPrice; }
}