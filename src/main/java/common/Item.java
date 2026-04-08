package common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Item implements Serializable {
    private static final long serialVersionUID = 1L;

    private String itemId;
    private String name;
    private String description;
    private double startingPrice;
    private double currentPrice;
    private String currentBidderId;
    private String currentBidderName;
    private String sellerId;
    private String sellerName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;  // PENDING, ACTIVE, ENDED, CANCELLED
    private int bidCount;
    private String imageUrl;

    public Item() {
        this.itemId = java.util.UUID.randomUUID().toString();
        this.status = "PENDING";
        this.bidCount = 0;
    }

    public Item(String name, String description, double startingPrice, long durationSeconds) {
        this();
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.startTime = LocalDateTime.now();
        this.endTime = startTime.plusSeconds(durationSeconds);
    }

    // Getters and Setters
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public String getCurrentBidderId() { return currentBidderId; }
    public void setCurrentBidderId(String currentBidderId) { this.currentBidderId = currentBidderId; }

    public String getCurrentBidderName() { return currentBidderName; }
    public void setCurrentBidderName(String currentBidderName) { this.currentBidderName = currentBidderName; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getBidCount() { return bidCount; }
    public void setBidCount(int bidCount) { this.bidCount = bidCount; }
    public void incrementBidCount() { this.bidCount++; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isActive() { return "ACTIVE".equals(status); }
    public boolean isEnded() { return "ENDED".equals(status); }

    public long getTimeRemainingMillis() {
        if (endTime == null) return 0;
        return Math.max(0, java.time.Duration.between(LocalDateTime.now(), endTime).toMillis());
    }

    @Override
    public String toString() {
        return "Item{" + name + ", price=" + currentPrice + ", status=" + status + '}';
    }
}

