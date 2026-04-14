package com.appdev.bilijuan.utils;

public class DeliveryUtils {

    // Public so OrderSummaryActivity can use it as the safe default
    public static final double BASE_FEE   = 20.0;  // ₱20 flat base
    public static final double FEE_PER_KM = 10.0;  // ₱10 per km after 1km
    public static final double FREE_KM    = 1.0;   // First 1km is free

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
     * Calculates delivery fee: ₱20 base + ₱10 per km after first 1km.
     * Examples:
     *   0.3 km → ₱20
     *   1.0 km → ₱20
     *   1.5 km → ₱25
     *   2.0 km → ₱30
     *   3.5 km → ₱45
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