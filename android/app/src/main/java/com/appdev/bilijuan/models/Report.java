package com.appdev.bilijuan.models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

@IgnoreExtraProperties
public class Report {

    public static final String STATUS_PENDING   = "pending";
    public static final String STATUS_RESOLVED  = "resolved";
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
    
    // Core Fields (Must match Firestore exactly)
    private String customerId;
    private String customerName;
    private String productId;
    private String productName;
    private String storeId;
    private String storeName;
    private String reason;
    private String status;
    private String note;

    // New Schema support (if you start using these)
    private String reporterId;
    private String reporterName;
    private String targetId;
    private String targetName;
    private String targetType;

    @ServerTimestamp
    private Date timestamp;
    private Date createdAt; // Old schema field

    public Report() {}

    // Constructor for new reports
    public Report(String reporterId, String reporterName, String targetId, String targetName, String targetType, String reason, String storeId, String storeName) {
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.targetId = targetId;
        this.targetName = targetName;
        this.targetType = targetType;
        this.reason = reason;
        this.storeId = storeId;
        this.storeName = storeName;
        this.status = STATUS_PENDING;
        
        // Populate fallback fields for backward compatibility
        this.customerId = reporterId;
        this.customerName = reporterName;
        if ("product".equals(targetType)) {
            this.productId = targetId;
            this.productName = targetName;
        }
    }

    // --- Helper Getters for the UI ---

    public String getDisplayTargetName() {
        if (targetName != null && !targetName.isEmpty()) return targetName;
        if (productName != null && !productName.isEmpty()) return productName;
        return "Unknown Entity";
    }

    public String getDisplayReporterName() {
        if (reporterName != null && !reporterName.isEmpty()) return reporterName;
        if (customerName != null && !customerName.isEmpty()) return customerName;
        return "Anonymous";
    }

    public String getDisplayTargetType() {
        if (targetType != null && !targetType.isEmpty()) return targetType;
        if (productId != null || productName != null) return "product";
        return "user";
    }

    // --- Standard Getters & Setters ---

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
