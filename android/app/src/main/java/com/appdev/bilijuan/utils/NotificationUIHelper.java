package com.appdev.bilijuan.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.activities.customer.MyOrdersActivity;
import com.appdev.bilijuan.activities.customer.OrderTrackingActivity;
import com.appdev.bilijuan.activities.seller.SellerOrdersActivity;
import com.appdev.bilijuan.adapters.NotificationAdapter;
import com.appdev.bilijuan.models.Notification;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationUIHelper {

    public static void showNotificationSheet(Activity activity) {
        String uid = FirebaseHelper.getCurrentUid();
        if (uid == null) return;

        // Create a custom dialog for TopSheet behavior
        Dialog dialog = new Dialog(activity, R.style.TopSheetDialogTheme);
        View v = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_notifications, null);
        dialog.setContentView(v);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.TOP);
        }

        RecyclerView rv = v.findViewById(R.id.rvNotifications);
        ProgressBar pb = v.findViewById(R.id.progressBar);
        View empty = v.findViewById(R.id.layoutEmpty);
        View btnMarkAll = v.findViewById(R.id.btnMarkAllRead);

        List<Notification> list = new ArrayList<>();
        NotificationAdapter adapter = new NotificationAdapter(list, new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onNotificationClick(Notification n) {
                markAsRead(n.getId());
                handleRedirection(activity, n);
                dialog.dismiss();
            }

            @Override
            public void onMarkAsReadClick(Notification n) {
                markAsRead(n.getId());
                n.setRead(true);
                if (rv.getAdapter() != null) rv.getAdapter().notifyDataSetChanged();
            }
        });

        rv.setLayoutManager(new LinearLayoutManager(activity));
        rv.setAdapter(adapter);

        // Fetch notifications
        FirebaseHelper.getDb().collection("notifications")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener((snap, e) -> {
                    if (pb != null) pb.setVisibility(View.GONE);
                    if (e != null || snap == null) return;
                    
                    list.clear();
                    for (DocumentSnapshot doc : snap) {
                        Notification n = doc.toObject(Notification.class);
                        if (n != null) {
                            n.setId(doc.getId());
                            list.add(n);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (empty != null) empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                });

        if (btnMarkAll != null) {
            btnMarkAll.setOnClickListener(view -> {
                for (Notification n : list) {
                    if (!n.isRead()) markAsRead(n.getId());
                }
            });
        }

        dialog.show();
    }

    private static void markAsRead(String id) {
        FirebaseHelper.getDb().collection("notifications").document(id).update("read", true);
    }

    private static void handleRedirection(Activity activity, Notification n) {
        Intent intent = null;
        switch (n.getType()) {
            case Notification.TYPE_ORDER:
                if (activity.getClass().getSimpleName().contains("Seller")) {
                    intent = new Intent(activity, SellerOrdersActivity.class);
                } else {
                    intent = new Intent(activity, MyOrdersActivity.class);
                }
                break;
            case Notification.TYPE_TRACKING:
                intent = new Intent(activity, OrderTrackingActivity.class);
                intent.putExtra("orderId", n.getRelatedId());
                break;
            case Notification.TYPE_NOTICE:
            default:
                break;
        }
        if (intent != null) activity.startActivity(intent);
    }
}
