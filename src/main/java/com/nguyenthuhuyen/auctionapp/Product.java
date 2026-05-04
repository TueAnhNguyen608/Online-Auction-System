package com.nguyenthuhuyen.auctionapp;

public class Product {
    private String name;
    private String price;
    private String time;
    private String type;

    public Product(String name, String price, String time, String type) {
        this.name = name;
        this.price = price;
        this.time = time;
        this.type = type;
    }

    // Quan trọng: Tên các hàm getter này phải khớp để TableView tìm thấy dữ liệu
    public String getName() { return name; }
    public String getPrice() { return price; }
    public String getTime() { return time; }
    public String getType() { return type; }
}
