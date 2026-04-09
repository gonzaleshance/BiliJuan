package com.appdev.bilijuan;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.appdev.bilijuan.activities.customer.OrderTrackingActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class BiliJuanFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID   = "bilijuan_orders";
    private static final String CHANNEL_NAME = "Order Updates";

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        String title   = "BiliJuan";
        String body    = "Your order has been updated.";
        String orderId = null;

        if (message.getNotification() != null) {
            if (message.getNotification().getTitle() != null)
                title = message.getNotification().getTitle();
            if (message.getNotification().getBody() != null)
                body = message.getNotification().getBody();
        }

        if (message.getData().containsKey("orderId"))
            orderId = message.getData().get("orderId");

        showNotification(title, body, orderId);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // Save token to Firestore so server can send targeted notifications
        String uid = com.appdev.bilijuan.utils.FirebaseHelper.getCurrentUid();
        if (uid != null) {
            com.appdev.bilijuan.utils.FirebaseHelper.getDb()
                    .collection("users").document(uid)
                    .update("fcmToken", token);
        }
    }

    private void showNotification(String title, String body, String orderId) {
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Live order status updates");
            nm.createNotificationChannel(channel);
        }

        // Tap opens OrderTrackingActivity
        Intent intent = new Intent(this, OrderTrackingActivity.class);
        if (orderId != null) intent.putExtra("orderId", orderId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_delivery)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        nm.notify((int) System.currentTimeMillis(), builder.build());
    }
}