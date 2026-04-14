package com.appdev.bilijuan.models;

public class CartItem {
    private String productId;
    private String productName;
    private double price;
    private int quantity;
    private String sellerId;
    private String sellerName;
    private String imageBase64;

    public CartItem() {}

    public CartItem(String productId, String productName, double price, int quantity, String sellerId, String sellerName, String imageBase64) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.imageBase64 = imageBase64;
    }

    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public String getImageBase64() { return imageBase64; }
}
