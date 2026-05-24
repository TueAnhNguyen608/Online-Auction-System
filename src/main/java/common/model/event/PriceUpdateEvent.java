package common.model.event;

import java.io.Serializable;

public class PriceUpdateEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type; // "PRICE_UPDATE", "AUCTION_END", "EXTENSION"
    private String itemId;
    private String itemName;
    private double currentPrice;
    private String currentBidderName;
    private long remainingSeconds;
    private int bidCount;

    public PriceUpdateEvent() {}

    public PriceUpdateEvent(String itemId, String itemName, double currentPrice, String currentBidderName, long remainingSeconds, int bidCount) {
        this.type = "PRICE_UPDATE";
        this.itemId = itemId;
        this.itemName = itemName;
        this.currentPrice = currentPrice;
        this.currentBidderName = currentBidderName;
        this.remainingSeconds = remainingSeconds;
        this.bidCount = bidCount;
    }

    public static PriceUpdateEvent auctionEnd(String itemId, String itemName, double finalPrice, String winnerName) {
        PriceUpdateEvent event = new PriceUpdateEvent();
        event.type = "AUCTION_END";
        event.itemId = itemId;
        event.itemName = itemName;
        event.currentPrice = finalPrice;
        event.currentBidderName = winnerName;
        event.remainingSeconds = 0;
        return event;
    }

    public static PriceUpdateEvent extension(String itemId, int extraSeconds) {
        PriceUpdateEvent event = new PriceUpdateEvent();
        event.type = "EXTENSION";
        event.itemId = itemId;
        event.remainingSeconds = extraSeconds;
        return event;
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public String getCurrentBidderName() { return currentBidderName; }
    public void setCurrentBidderName(String currentBidderName) { this.currentBidderName = currentBidderName; }

    public long getRemainingSeconds() { return remainingSeconds; }
    public void setRemainingSeconds(long remainingSeconds) { this.remainingSeconds = remainingSeconds; }

    public int getBidCount() { return bidCount; }
    public void setBidCount(int bidCount) { this.bidCount = bidCount; }
}