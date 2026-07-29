package com.appdev.bilijuan.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class User {

    private String uid;
    private String name;
    private String email;
    private String role;            // "customer" | "seller" | "admin"
    private String phone;
    private String address;
    private double latitude;
    private double longitude;
    private String profileImageUrl;

    // ── Account status ────────────────────────────────────────────────────────
    private String status;          // "active" | "disabled" | "archived"
    private String disableReason;
    private String disableNote;
    private String disabledBy;      // admin uid who disabled
    private boolean isSuspended;    // Added for moderation
    private String suspensionMessage; // Added for moderation

    @ServerTimestamp
    private Date disabledAt;

    // ── Store-only fields ─────────────────────────────────────────────────────
    private boolean isOpen;         // store open/closed toggle
    private String storeImageBase64;
    private boolean isFeatured;     // Promotion toggle

    // ── FCM ───────────────────────────────────────────────────────────────────
    private String fcmToken;

    // ── Report tracking ───────────────────────────────────────────────────────
    private int reportCount;        // incremented each time store is reported

    // ── Legacy fields kept for compatibility ──────────────────────────────────
    private String subdivision;
    private String block;
    private String lot;
    private String street;
    private String landmark;
    private boolean approved;

    @ServerTimestamp
    private Date createdAt;

    // Required empty constructor for Firestore
    public User() {}

    // Customer registration constructor
    public User(String uid, String name, String email, String role,
                String phone, String address) {
        this.uid     = uid;
        this.name    = name;
        this.email   = email;
        this.role    = role;
        this.phone   = phone;
        this.address = address;
        this.status  = "active";
        this.isOpen  = true;
        this.approved = true;
        this.reportCount = 0;
        this.isSuspended = false;
        this.isFeatured = false;
    }

    // Full constructor (legacy support)
    public User(String uid, String name, String email, String role,
                String phone, String subdivision, String block,
                String lot, String street, String landmark, String address) {
        this.uid         = uid;
        this.name        = name;
        this.email       = email;
        this.role        = role;
        this.phone       = phone;
        this.subdivision = subdivision;
        this.block       = block;
        this.lot         = lot;
        this.street      = street;
        this.landmark    = landmark;
        this.address     = address;
        this.status      = "active";
        this.isOpen      = true;
        this.approved    = true;
        this.reportCount = 0;
        this.isSuspended = false;
        this.isFeatured = false;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getUid()              { return uid; }
    public String getName()             { return name; }
    public String getEmail()            { return email; }
    public String getRole()             { return role; }
    public String getPhone()            { return phone; }
    public String getAddress()          { return address; }
    public double getLatitude()         { return latitude; }
    public double getLongitude()        { return longitude; }
    public String getProfileImageUrl()  { return profileImageUrl; }
    public String getStatus()           { return status != null ? status : "active"; }
    public String getDisableReason()    { return disableReason; }
    public String getDisableNote()      { return disableNote; }
    public String getDisabledBy()       { return disabledBy; }
    public Date   getDisabledAt()       { return disabledAt; }
    public boolean isOpen()             { return isOpen; }
    public String getStoreImageBase64() { return storeImageBase64; }
    public boolean isFeatured()         { return isFeatured; }
    public String getFcmToken()         { return fcmToken; }
    public int    getReportCount()      { return reportCount; }
    public String getSubdivision()      { return subdivision; }
    public String getBlock()            { return block; }
    public String getLot()              { return lot; }
    public String getStreet()           { return street; }
    public String getLandmark()         { return landmark; }
    public boolean isApproved()         { return approved; }
    public Date   getCreatedAt()        { return createdAt; }
    public boolean isSuspended()        { return isSuspended; }
    public String getSuspensionMessage(){ return suspensionMessage; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setUid(String uid)                    { this.uid = uid; }
    public void setName(String name)                  { this.name = name; }
    public void setEmail(String email)                { this.email = email; }
    public void setRole(String role)                  { this.role = role; }
    public void setPhone(String phone)                { this.phone = phone; }
    public void setAddress(String address)            { this.address = address; }
    public void setLatitude(double lat)               { this.latitude = lat; }
    public void setLongitude(double lng)              { this.longitude = lng; }
    public void setProfileImageUrl(String url)        { this.profileImageUrl = url; }
    public void setStatus(String status)              { this.status = status; }
    public void setDisableReason(String reason)       { this.disableReason = reason; }
    public void setDisableNote(String note)           { this.disableNote = note; }
    public void setDisabledBy(String uid)             { this.disabledBy = uid; }
    public void setDisabledAt(Date date)              { this.disabledAt = date; }
    public void setOpen(boolean open)                 { this.isOpen = open; }
    public void setStoreImageBase64(String img)       { this.storeImageBase64 = img; }
    public void setFeatured(boolean featured)         { this.isFeatured = featured; }
    public void setFcmToken(String token)             { this.fcmToken = token; }
    public void setReportCount(int count)             { this.reportCount = count; }
    public void setSubdivision(String s)              { this.subdivision = s; }
    public void setBlock(String b)                    { this.block = b; }
    public void setLot(String l)                      { this.lot = l; }
    public void setStreet(String s)                   { this.street = s; }
    public void setLandmark(String l)                 { this.landmark = l; }
    public void setApproved(boolean approved)         { this.approved = approved; }
    public void setCreatedAt(Date date)               { this.createdAt = date; }
    public void setSuspended(boolean suspended)       { isSuspended = suspended; }
    public void setSuspensionMessage(String message)  { this.suspensionMessage = message; }

    // ── Helpers ───────────────────────────────────────────────────────────────
    public boolean isAdmin()      { return "admin".equals(role); }
    public boolean isSeller()     { return "seller".equals(role); }
    public boolean isCustomer()   { return "customer".equals(role); }
    public boolean hasLocation()  { return latitude != 0 && longitude != 0; }
    public boolean isActive()     { return "active".equals(getStatus()); }
    public boolean isDisabled()   { return "disabled".equals(getStatus()) || isSuspended; }
    public boolean isArchived()   { return "archived".equals(getStatus()); }
}
