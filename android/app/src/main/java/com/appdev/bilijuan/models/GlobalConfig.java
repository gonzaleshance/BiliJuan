package com.appdev.bilijuan.models;

public class GlobalConfig {
    private double base_delivery_fee;
    private double price_per_km;

    public GlobalConfig() {}

    public GlobalConfig(double base_delivery_fee, double price_per_km) {
        this.base_delivery_fee = base_delivery_fee;
        this.price_per_km = price_per_km;
    }

    public double getBase_delivery_fee() {
        return base_delivery_fee;
    }

    public void setBase_delivery_fee(double base_delivery_fee) {
        this.base_delivery_fee = base_delivery_fee;
    }

    public double getPrice_per_km() {
        return price_per_km;
    }

    public void setPrice_per_km(double price_per_km) {
        this.price_per_km = price_per_km;
    }
}
