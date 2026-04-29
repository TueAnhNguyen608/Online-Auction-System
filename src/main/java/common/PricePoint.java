package common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PricePoint implements Serializable {
    private static final long serialVersionUID = 1L;

    private double price;
    private LocalDateTime time;
    private long timestampMillis;

    public PricePoint() {}

    public PricePoint(double price, LocalDateTime time) {
        this.price = price;
        this.time = time;
        this.timestampMillis = time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }

    public long getTimestampMillis() { return timestampMillis; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }
}