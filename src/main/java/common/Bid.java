
package common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Bid extends Entity {
    private static final long serialVersionUID = 1L;

    private String itemId;
    private String itemName;
    private String bidderId;
    private String bidderName;
    private double amount;
    private LocalDateTime bidTime;
    private boolean isAutoBid;
    private boolean isWinning;

    public Bid() {
        this.setId(java.util.UUID.randomUUID().toString());
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

    public Bid(String itemId, String bidderId, String bidderName, double amount) {
        this(itemId, bidderId, amount);
        this.bidderName = bidderName;
    }

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

    // CÁCH 1: Dùng System.currentTimeMillis() - ĐƠN GIẢN NHẤT
    public long getTimestampMillis() {
        return System.currentTimeMillis();
    }

    // CÁCH 2: Nếu muốn lấy từ bidTime (chính xác hơn)
    public long getBidTimeMillis() {
        return bidTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    @Override
    public String toString() {
        return "Bid{" + bidderName + "=" + amount + " on " + itemId + '}';
    }
}