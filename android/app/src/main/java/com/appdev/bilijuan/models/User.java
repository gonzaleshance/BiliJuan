package com.appdev.bilijuan.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class User {

    private String uid;
    private String name;
    private String email;
    private String role;            // "customer" | "seller" | "admin"
    private String phone;

    // ── Address fields (Saranay-specific) ────────────────────────────────────
    private String subdivision;     // e.g. "Saranay Homes", "Tierra Nova Royale"
    private String block;           // e.g. "Block 4"
    private String lot;             // e.g. "Lot 12"
    private String street;          // e.g. "Ilang-Ilang St."
    private String landmark;        // e.g. "near sari-sari store"
    private String address;         // Full auto-built address string saved to Firestore

    // ── Map coordinates (set after user pins location) ────────────────────────
    private double latitude;
    private double longitude;

    // ── Profile ───────────────────────────────────────────────────────────────
    private String profileImageUrl;
    private boolean approved;

    @ServerTimestamp
    private Date createdAt;

    // Required empty constructor for Firestore
    public User() {}

    // Constructor used during registration
    public User(String uid, String name, String email, String role,
                String phone, String subdivision, String block,
                String lot, String street, String landmark, String address) {
        this.uid          = uid;
        this.name         = name;
        this.email        = email;
        this.role         = role;
        this.phone        = phone;
        this.subdivision  = subdivision;
        this.block        = block;
        this.lot          = lot;
        this.street       = street;
        this.landmark     = landmark;
        this.address      = address;
        this.latitude     = 0;
        this.longitude    = 0;
        this.profileImageUrl = "";
        this.approved     = true;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getUid()              { return uid; }
    public String getName()             { return name; }
    public String getEmail()            { return email; }
    public String getRole()             { return role; }
    public String getPhone()            { return phone; }
    public String getSubdivision()      { return subdivision; }
    public String getBlock()            { return block; }
    public String getLot()              { return lot; }
    public String getStreet()           { return street; }
    public String getLandmark()         { return landmark; }
    public String getAddress()          { return address; }
    public double getLatitude()         { return latitude; }
    public double getLongitude()        { return longitude; }
    public String getProfileImageUrl()  { return profileImageUrl; }
    public boolean isApproved()         { return approved; }
    public Date getCreatedAt()          { return createdAt; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setUid(String uid)                  { this.uid = uid; }
    public void setName(String name)                { this.name = name; }
    public void setEmail(String email)              { this.email = email; }
    public void setRole(String role)                { this.role = role; }
    public void setPhone(String phone)              { this.phone = phone; }
    public void setSubdivision(String subdivision)  { this.subdivision = subdivision; }
    public void setBlock(String block)              { this.block = block; }
    public void setLot(String lot)                  { this.lot = lot; }
    public void setStreet(String street)            { this.street = street; }
    public void setLandmark(String landmark)        { this.landmark = landmark; }
    public void setAddress(String address)          { this.address = address; }
    public void setLatitude(double lat)             { this.latitude = lat; }
    public void setLongitude(double lng)            { this.longitude = lng; }
    public void setProfileImageUrl(String url)      { this.profileImageUrl = url; }
    public void setApproved(boolean approved)       { this.approved = approved; }
    public void setCreatedAt(Date createdAt)        { this.createdAt = createdAt; }

    // ── Helpers ───────────────────────────────────────────────────────────────
    public boolean isAdmin()      { return "admin".equals(role); }
    public boolean isSeller()     { return "seller".equals(role); }
    public boolean isCustomer()   { return "customer".equals(role); }
    public boolean hasLocation()  { return latitude != 0 && longitude != 0; }
}