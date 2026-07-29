package com.appdev.bilijuan.utils;

import com.appdev.bilijuan.models.GlobalConfig;

public class DeliveryUtils {

    // Default values if Firestore config is not yet loaded
    public static double BASE_FEE   = 20.0;
    public static double FEE_PER_KM = 10.0;
    public static final double FREE_KM    = 1.0;

    public static void updateConfig(GlobalConfig config) {
        if (config != null) {
            BASE_FEE = config.getBase_delivery_fee();
            FEE_PER_KM = config.getPrice_per_km();
        }
    }

    /**
     * Haversine formula — calculates straight-line distance in km
     * between two lat/lng coordinates.
     */
    public static double haversineKm(double lat1, double lng1,
                                     double lat2, double lng2) {
        final int R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Calculates delivery fee: BASE_FEE + FEE_PER_KM per km after first 1km.
     */
    public static double calculateDeliveryFee(double distanceKm) {
        if (distanceKm <= FREE_KM) {
            return BASE_FEE;
        }
        double extraKm = distanceKm - FREE_KM;
        return BASE_FEE + (extraKm * FEE_PER_KM);
    }

    /**
     * Formats distance for display. e.g. "0.8 km" or "2.3 km"
     */
    public static String formatDistance(double distanceKm) {
        return String.format("%.1f km away", distanceKm);
    }

    /**
     * Formats delivery fee for display. e.g. "₱20" or "₱35"
     */
    public static String formatFee(double fee) {
        if (fee == Math.floor(fee)) {
            return "₱" + (int) fee;
        }
        return String.format("₱%.2f", fee);
    }
}
