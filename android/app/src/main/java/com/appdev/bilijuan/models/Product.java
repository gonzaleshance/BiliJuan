package com.appdev.bilijuan.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Product {

    private String productId;
    private String sellerId;
    private String sellerName;
    private String name;
    private String description;
    private double price;
    private String category;     // Ulam, Rice Meals, Meryenda, Pulutan, Panghimagas, Street Food, Specialty/Celebration
    private String imageBase64;  // Base64-encoded image stored in Firestore
    private boolean available;
    private int likes;
    private float stars;         // Average star rating
    private int ratingCount;     // Total number of ratings
    private double shopScore;    // stars + (likes × 0.5) — used for feed ranking

    @ServerTimestamp
    private Date createdAt;

    // Required empty constructor for Firestore
    public Product() {}

    public Product(String sellerId, String sellerName, String name,
                   String description, double price, String category) {
        this.sellerId    = sellerId;
        this.sellerName  = sellerName;
        this.name        = name;
        this.description = description;
        this.price       = price;
        this.category    = category;
        this.imageBase64 = "";
        this.available   = true;
        this.likes       = 0;
        this.stars       = 0f;
        this.ratingCount = 0;
        this.shopScore   = 0.0;
    }

    // Getters
    public String getProductId()    { return productId; }
    public String getSellerId()     { return sellerId; }
    public String getSellerName()   { return sellerName; }
    public String getName()         { return name; }
    public String getDescription()  { return description; }
    public double getPrice()        { return price; }
    public String getCategory()     { return category; }
    public String getImageBase64()  { return imageBase64; }
    public boolean isAvailable()    { return available; }
    public int getLikes()           { return likes; }
    public float getStars()         { return stars; }
    public int getRatingCount()     { return ratingCount; }
    public double getShopScore()    { return shopScore; }
    public Date getCreatedAt()      { return createdAt; }

    // Setters
    public void setProductId(String productId)      { this.productId = productId; }
    public void setSellerId(String sellerId)        { this.sellerId = sellerId; }
    public void setSellerName(String sellerName)    { this.sellerName = sellerName; }
    public void setName(String name)                { this.name = name; }
    public void setDescription(String description)  { this.description = description; }
    public void setPrice(double price)              { this.price = price; }
    public void setCategory(String category)        { this.category = category; }
    public void setImageBase64(String imageBase64)  { this.imageBase64 = imageBase64; }
    public void setAvailable(boolean available)     { this.available = available; }
    public void setLikes(int likes)                 { this.likes = likes; }
    public void setStars(float stars)               { this.stars = stars; }
    public void setRatingCount(int ratingCount)     { this.ratingCount = ratingCount; }
    public void setShopScore(double shopScore)      { this.shopScore = shopScore; }
    public void setCreatedAt(Date createdAt)        { this.createdAt = createdAt; }

    // Helper — recalculate shop score locally
    public void recalculateShopScore() {
        this.shopScore = stars + (likes * 0.5);
    }
}