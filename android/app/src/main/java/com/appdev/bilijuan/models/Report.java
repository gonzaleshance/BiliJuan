package com.appdev.bilijuan.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Report {

    public static final String STATUS_PENDING   = "pending";
    public static final String STATUS_REVIEWED  = "reviewed";
    public static final String STATUS_DISMISSED = "dismissed";

    public static final String[] REASONS = {
            "Wrong item delivered",
            "Poor food quality",
            "Rude or unprofessional behavior",
            "Fake or misleading listing",
            "Order never arrived",
            "Other"
    };

    private String reportId;
    private String productId;
    private String productName;
    private String storeId;
    private String storeName;
    private String customerId;
    private String customerName;
    private String reason;
    private String note;
    private String status;          // pending | reviewed | dismissed

    @ServerTimestamp
    private Date createdAt;

    public Report() {}

    public Report(String productId, String productName,
                  String storeId, String storeName,
                  String customerId, String customerName,
                  String reason, String note) {
        this.productId    = productId;
        this.productName  = productName;
        this.storeId      = storeId;
        this.storeName    = storeName;
        this.customerId   = customerId;
        this.customerName = customerName;
        this.reason       = reason;
        this.note         = note;
        this.status       = STATUS_PENDING;
    }

    // Getters
    public String getReportId()     { return reportId; }
    public String getProductId()    { return productId; }
    public String getProductName()  { return productName; }
    public String getStoreId()      { return storeId; }
    public String getStoreName()    { return storeName; }
    public String getCustomerId()   { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getReason()       { return reason; }
    public String getNote()         { return note; }
    public String getStatus()       { return status; }
    public Date   getCreatedAt()    { return createdAt; }

    // Setters
    public void setReportId(String id)      { this.reportId = id; }
    public void setStatus(String status)    { this.status = status; }
    public void setCreatedAt(Date date)     { this.createdAt = date; }
}