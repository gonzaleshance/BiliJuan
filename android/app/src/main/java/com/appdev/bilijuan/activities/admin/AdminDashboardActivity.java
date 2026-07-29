package com.appdev.bilijuan.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.appdev.bilijuan.activities.LoginActivity;
import com.appdev.bilijuan.databinding.ActivityAdminBinding;
import com.appdev.bilijuan.models.Notification;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.models.Report;
import com.appdev.bilijuan.utils.AdminNavHelper;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.NotificationHelper;
import com.appdev.bilijuan.utils.NotificationUIHelper;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AdminDashboardActivity extends AppCompatActivity {

    private ActivityAdminBinding binding;
    private ListenerRegistration notifListener;
    private ListenerRegistration reportListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupBottomNav();
        loadAdminName();
        loadStats();
        listenForNotifications();
        listenForReports();

        binding.btnLogout.setOnClickListener(v -> logout());
        binding.btnNotification.setOnClickListener(v -> NotificationUIHelper.showNotificationSheet(this));
        
        binding.btnReports.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminReportsActivity.class));
        });

        binding.btnSystemSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminSettingsActivity.class));
        });

        binding.btnBroadcast.setOnClickListener(v -> sendBroadcast());
        
        // --- Fixed Stats Card Click Listeners ---
        
        // Wrap in a general layout reference if needed, or target cards directly
        // Based on activity_admin.xml, the cards don't have IDs, but the inner TextViews do.
        // We find the parent CardView of the TextView to make the whole card clickable.

        setupCardClick(binding.tvStatCustomers, AdminUsersActivity.class);
        setupCardClick(binding.tvStatSellers, AdminSellersActivity.class);
        setupCardClick(binding.tvStatActiveOrders, AdminOrdersActivity.class);
        setupCardClick(binding.tvStatDelivered, AdminOrdersActivity.class);
        setupCardClick(binding.tvStatProducts, AdminFoodsActivity.class);
    }

    private void setupCardClick(View view, Class<?> activityClass) {
        if (view == null) return;
        // Find the parent MaterialCardView
        View parent = (View) view.getParent();
        if (parent != null) {
            parent.setOnClickListener(v -> {
                Intent intent = new Intent(this, activityClass);
                // Clear flags to prevent stacking if navigating between tabs
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        }
    }

    private void sendBroadcast() {
        String message = binding.etAnnouncement.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter an announcement", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnBroadcast.setEnabled(false);
        binding.btnBroadcast.setText("Sending...");

        // BROADCAST LOGIC: Create a notification for EVERY user in the database
        FirebaseHelper.getDb().collection("users").get().addOnSuccessListener(snap -> {
            if (snap == null || snap.isEmpty()) {
                binding.btnBroadcast.setEnabled(true);
                binding.btnBroadcast.setText("Send Platform-Wide Alert");
                return;
            }

            int total = snap.size();
            for (QueryDocumentSnapshot doc : snap) {
                // We use the helper to write to the 'notifications' collection
                NotificationHelper.sendNotification(
                    doc.getId(), 
                    "Platform Announcement", 
                    message, 
                    Notification.TYPE_NOTICE, 
                    "admin_broadcast"
                );
            }
            
            binding.etAnnouncement.setText("");
            binding.btnBroadcast.setEnabled(true);
            binding.btnBroadcast.setText("Send Platform-Wide Alert");
            Toast.makeText(this, "Broadcast sent to " + total + " users", Toast.LENGTH_LONG).show();
            
        }).addOnFailureListener(e -> {
            binding.btnBroadcast.setEnabled(true);
            binding.btnBroadcast.setText("Send Platform-Wide Alert");
            Toast.makeText(this, "Broadcast failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void listenForNotifications() {
        String uid = FirebaseHelper.getCurrentUid();
        if (uid == null) return;

        notifListener = FirebaseHelper.getDb().collection("notifications")
                .whereEqualTo("userId", uid)
                .whereEqualTo("read", false)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                });
    }

    private void listenForReports() {
        reportListener = FirebaseHelper.getDb().collection("reports")
                .whereEqualTo("status", Report.STATUS_PENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    binding.tvReportCount.setText(value.size() + " pending");
                });
    }

    private void setupBottomNav() {
        AdminNavHelper.setup(this, binding.adminNav.getRoot(), AdminNavHelper.Tab.OVERVIEW);
    }

    private void loadAdminName() {
        String uid = FirebaseHelper.getCurrentUid();
        if (uid == null) return;
        FirebaseHelper.getDb().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null) binding.tvAdminName.setText(name);
                    }
                });
    }

    private void loadStats() {
        FirebaseHelper.getDb().collection("users").get().addOnSuccessListener(snap -> {
            int c = 0, s = 0;
            for (QueryDocumentSnapshot d : snap) {
                String role = d.getString("role");
                if ("customer".equals(role)) c++;
                else if ("seller".equals(role)) s++;
            }
            binding.tvStatCustomers.setText(String.valueOf(c));
            binding.tvStatSellers.setText(String.valueOf(s));
        });

        FirebaseHelper.getDb().collection("orders").get().addOnSuccessListener(snap -> {
            int active = 0, delivered = 0;
            for (QueryDocumentSnapshot d : snap) {
                String status = d.getString("status");
                if (Order.STATUS_DELIVERED.equals(status)) delivered++;
                else if (!Order.STATUS_CANCELLED.equals(status)) active++;
            }
            binding.tvStatActiveOrders.setText(String.valueOf(active));
            binding.tvStatDelivered.setText(String.valueOf(delivered));
        });

        FirebaseHelper.getDb().collection("products").get().addOnSuccessListener(snap ->
                binding.tvStatProducts.setText(String.valueOf(snap.size())));
    }

    private void logout() {
        FirebaseHelper.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notifListener != null) notifListener.remove();
        if (reportListener != null) reportListener.remove();
    }
}
