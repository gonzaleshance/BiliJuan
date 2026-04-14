package com.appdev.bilijuan.utils;

import com.appdev.bilijuan.models.Notification;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class NotificationHelper {

    public static void sendNotification(String userId, String title, String message, String type, String relatedId) {
        Map<String, Object> notifMap = new HashMap<>();
        notifMap.put("userId", userId);
        notifMap.put("title", title);
        notifMap.put("message", message);
        notifMap.put("type", type);
        notifMap.put("relatedId", relatedId);
        notifMap.put("read", false);
        notifMap.put("timestamp", FieldValue.serverTimestamp());

        FirebaseHelper.getDb().collection("notifications")
                .add(notifMap)
                .addOnSuccessListener(ref -> ref.update("id", ref.getId()));
    }

    public static void notifyStatusChange(String orderId, String newStatus, String productName, String customerId) {
        String title = "Order Update";
        String message = messageForStatus(newStatus, productName);
        String type = Notification.TYPE_ORDER;
        
        if ("On the way".equals(newStatus)) {
            type = Notification.TYPE_TRACKING;
        }

        sendNotification(customerId, title, message, type, orderId);
        
        Map<String, Object> update = new HashMap<>();
        update.put("lastNotificationStatus", newStatus);
        update.put("lastNotificationMessage", message);
        update.put("lastNotificationTime", FieldValue.serverTimestamp());

        FirebaseHelper.getDb().collection("orders").document(orderId).update(update);
    }

    public static void notifyNewOrder(String orderId, String sellerId, String customerName) {
        sendNotification(sellerId, "New Order Received!", 
                customerName + " just placed an order. Tap to view details.", 
                Notification.TYPE_ORDER, orderId);
    }

    public static void notifyNewReport(String reportId, String customerName, String storeName) {
        // Send notification to all admins
        FirebaseHelper.getDb().collection("users")
                .whereEqualTo("role", "admin")
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        sendNotification(doc.getId(), "New Report Received",
                                customerName + " reported " + storeName + ". Tap to review.",
                                Notification.TYPE_NOTICE, reportId);
                    }
                });
    }

    public static String messageForStatus(String status, String productName) {
        switch (status) {
            case "Confirmed":  return "Your order has been confirmed! " + productName + " is being prepared.";
            case "Preparing":  return "🍳 " + productName + " is being cooked right now!";
            case "On the way": return "🛵 Your order is on the way! Get ready to receive it.";
            case "Delivered":  return "✅ Your order has been delivered. Enjoy your meal!";
            default:           return "Your order status for " + productName + " has been updated.";
        }
    }
}
