package com.appdev.bilijuan.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Order {

    // Status constants — use these everywhere, never raw strings
    public static final String STATUS_PENDING    = "Pending";
    public static final String STATUS_CONFIRMED  = "Confirmed";
    public static final String STATUS_PREPARING  = "Preparing";
    public static final String STATUS_ON_THE_WAY = "On the way";
    public static final String STATUS_DELIVERED  = "Delivered";
    public static final String STATUS_CANCELLED  = "Cancelled";

    private String orderId;
    private String customerId;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private double customerLat;
    private double customerLng;

    private String sellerId;
    private String sellerName;
    private double sellerLat;
    private double sellerLng;

    // Live rider location — updated by seller while delivering
    private double riderLat;
    private double riderLng;

    private String productId;
    private String productName;
    private String productImageBase64;
    private int    quantity;
    private double productPrice;

    private String status;          // See STATUS_ constants above
    private String paymentMethod;   // "COD" or "GCash"
    private double deliveryFee;     // ₱20 base + ₱10/km after 1km
    private double totalAmount;     // (productPrice × quantity) + deliveryFee
    private double distanceKm;      // Haversine distance seller → customer




    @ServerTimestamp
    private Date createdAt;

    // Required empty constructor for Firestore
    public Order() {}

    public Order(String customerId, String customerName, String customerPhone,
                 String customerAddress, double customerLat, double customerLng,
                 String sellerId, String sellerName, double sellerLat, double sellerLng,
                 String productId, String productName, String productImageBase64,
                 int quantity, double productPrice,
                 String paymentMethod, double deliveryFee, double distanceKm) {
        this.customerId         = customerId;
        this.customerName       = customerName;
        this.customerPhone      = customerPhone;
        this.customerAddress    = customerAddress;
        this.customerLat        = customerLat;
        this.customerLng        = customerLng;
        this.sellerId           = sellerId;
        this.sellerName         = sellerName;
        this.sellerLat          = sellerLat;
        this.sellerLng          = sellerLng;
        this.riderLat           = 0;
        this.riderLng           = 0;
        this.productId          = productId;
        this.productName        = productName;
        this.productImageBase64 = productImageBase64;
        this.quantity           = quantity;
        this.productPrice       = productPrice;
        this.paymentMethod      = paymentMethod;
        this.deliveryFee        = deliveryFee;
        this.distanceKm         = distanceKm;
        this.totalAmount        = (productPrice * quantity) + deliveryFee;
        this.status             = STATUS_PENDING;
        this.active = true; // add this line in the Order constructor
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getOrderId()              { return orderId; }
    public String getCustomerId()           { return customerId; }
    public String getCustomerName()         { return customerName; }
    public String getCustomerPhone()        { return customerPhone; }
    public String getCustomerAddress()      { return customerAddress; }
    public double getCustomerLat()          { return customerLat; }
    public double getCustomerLng()          { return customerLng; }
    public String getSellerId()             { return sellerId; }
    public String getSellerName()           { return sellerName; }
    public double getSellerLat()            { return sellerLat; }
    public double getSellerLng()            { return sellerLng; }
    public double getRiderLat()             { return riderLat; }
    public double getRiderLng()             { return riderLng; }
    public String getProductId()            { return productId; }
    public String getProductName()          { return productName; }
    public String getProductImageBase64()   { return productImageBase64; }
    public int    getQuantity()             { return quantity; }
    public double getProductPrice()         { return productPrice; }
    public String getStatus()               { return status; }
    public String getPaymentMethod()        { return paymentMethod; }
    public double getDeliveryFee()          { return deliveryFee; }
    public double getTotalAmount()          { return totalAmount; }
    public double getDistanceKm()           { return distanceKm; }
    public Date   getCreatedAt()            { return createdAt; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setOrderId(String orderId)                      { this.orderId = orderId; }
    public void setCustomerId(String customerId)                { this.customerId = customerId; }
    public void setCustomerName(String customerName)            { this.customerName = customerName; }
    public void setCustomerPhone(String customerPhone)          { this.customerPhone = customerPhone; }
    public void setCustomerAddress(String customerAddress)      { this.customerAddress = customerAddress; }
    public void setCustomerLat(double customerLat)              { this.customerLat = customerLat; }
    public void setCustomerLng(double customerLng)              { this.customerLng = customerLng; }
    public void setSellerId(String sellerId)                    { this.sellerId = sellerId; }
    public void setSellerName(String sellerName)                { this.sellerName = sellerName; }
    public void setSellerLat(double sellerLat)                  { this.sellerLat = sellerLat; }
    public void setSellerLng(double sellerLng)                  { this.sellerLng = sellerLng; }
    public void setRiderLat(double riderLat)                    { this.riderLat = riderLat; }
    public void setRiderLng(double riderLng)                    { this.riderLng = riderLng; }
    public void setProductId(String productId)                  { this.productId = productId; }
    public void setProductName(String productName)              { this.productName = productName; }
    public void setProductImageBase64(String img)               { this.productImageBase64 = img; }
    public void setQuantity(int quantity)                       { this.quantity = quantity; }
    public void setProductPrice(double productPrice)            { this.productPrice = productPrice; }
    public void setStatus(String status)                        { this.status = status; }
    public void setPaymentMethod(String paymentMethod)          { this.paymentMethod = paymentMethod; }
    public void setDeliveryFee(double deliveryFee)              { this.deliveryFee = deliveryFee; }
    public void setTotalAmount(double totalAmount)              { this.totalAmount = totalAmount; }
    public void setDistanceKm(double distanceKm)                { this.distanceKm = distanceKm; }
    public void setCreatedAt(Date createdAt)                    { this.createdAt = createdAt; }

    // ── Helpers ───────────────────────────────────────────────────────────────
    public boolean canCustomerCancel() {
        return STATUS_PENDING.equals(status) || STATUS_CONFIRMED.equals(status);
    }

    public boolean isActive() {
        return active; // use the field directly instead of computing it
    }

    private boolean active; // add this field

    // add getter and setter:
    public void setActive(boolean active) { this.active = active; }

}
