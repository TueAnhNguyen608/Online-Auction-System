package common.model.bidding;

import common.Entity;
import java.time.LocalDateTime;
import java.util.UUID;

public class Bid extends Entity {

    private String itemId;
    private String itemName;
    private String bidderId;
    private String bidderName;
    private double amount;
    private LocalDateTime bidTime;
    private boolean isAutoBid;
    private boolean isWinning;
    private double finalPrice;
    private String itemStatus;

    public Bid() {
        super();
        this.setId(UUID.randomUUID().toString());
        this.bidTime = LocalDateTime.now();
        this.isAutoBid = false;
        this.isWinning = false;
    }

    public Bid(String itemId, String bidderId, double amount) {
        this();
        this.itemId = itemId;
        this.bidderId = bidderId;
        this.amount = amount;
    }

    // ========== GETTERS & SETTERS ==========

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getBidderId() { return bidderId; }
    public void setBidderId(String bidderId) { this.bidderId = bidderId; }

    public String getBidderName() { return bidderName; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDateTime getBidTime() { return bidTime; }
    public void setBidTime(LocalDateTime bidTime) { this.bidTime = bidTime; }

    public boolean isAutoBid() { return isAutoBid; }
    public void setAutoBid(boolean autoBid) { isAutoBid = autoBid; }

    public boolean isWinning() { return isWinning; }
    public void setWinning(boolean winning) { isWinning = winning; }

    public double getFinalPrice() { return finalPrice; }
    public void setFinalPrice(double finalPrice) { this.finalPrice = finalPrice; }

    public String getItemStatus() { return itemStatus; }
    public void setItemStatus(String itemStatus) { this.itemStatus = itemStatus; }

    // ========== HELPER ==========

    public long getTimestampMillis() {
        return System.currentTimeMillis();
    }

    public static Bid createAutoBid(String itemId, String bidderId, double amount) {
        Bid bid = new Bid(itemId, bidderId, amount);
        bid.setAutoBid(true);
        return bid;
    }

    @Override
    public String toString() {
        return String.format("Bid{bidder='%s', amount=%,.0f, auto=%s}",
                bidderName, amount, isAutoBid);
    }
}