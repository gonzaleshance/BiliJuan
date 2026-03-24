package com.appdev.bilijuan.models;

public class User {
    private String uid, name, email, role, phone, phase, lot, zone;

    public User() {}

    public User(String uid, String name, String email, String role,
                String phone, String phase, String lot, String zone) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.phone = phone;
        this.phase = phase;
        this.lot = lot;
        this.zone = zone;
    }

    public String getUid()   { return uid; }
    public String getName()  { return name; }
    public String getEmail() { return email; }
    public String getRole()  { return role; }
    public String getPhone() { return phone; }
    public String getPhase() { return phase; }
    public String getLot()   { return lot; }
    public String getZone()  { return zone; }

    public void setUid(String uid)     { this.uid = uid; }
    public void setName(String name)   { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role)   { this.role = role; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setPhase(String phase) { this.phase = phase; }
    public void setLot(String lot)     { this.lot = lot; }
    public void setZone(String zone)   { this.zone = zone; }
}