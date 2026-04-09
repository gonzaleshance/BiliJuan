package com.appdev.bilijuan.utils;

import com.google.firebase.firestore.DocumentSnapshot;

public class NotificationHelper {

    /**
     * When seller advances an order status, call this to update Firestore
     * with a notification field. A Cloud Function (or the app itself via FCM)
     * picks this up and sends the push notification to the customer.
     *
     * For now — writes a "lastNotification" field to the order document
     * so the customer app can show an in-app banner when they open it.
     */
    public static void notifyStatusChange(String orderId, String newStatus,
                                          String productName) {
        String message = messageForStatus(newStatus, productName);

        java.util.Map<String, Object> update = new java.util.HashMap<>();
        update.put("lastNotificationStatus", newStatus);
        update.put("lastNotificationMessage", message);
        update.put("lastNotificationTime",
                com.google.firebase.firestore.FieldValue.serverTimestamp());

        FirebaseHelper.getDb().collection("orders")
                .document(orderId)
                .update(update);
    }

    public static String messageForStatus(String status, String productName) {
        switch (status) {
            case "Confirmed":  return "Your order has been confirmed! " + productName + " is being prepared.";
            case "Preparing":  return "🍳 " + productName + " is being cooked right now!";
            case "On the way": return "🛵 Your order is on the way! Get ready to receive it.";
            case "Delivered":  return "✅ Your order has been delivered. Enjoy your meal!";
            default:           return "Your order status has been updated.";
        }
    }
}