package com.appdev.bilijuan.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.appdev.bilijuan.activities.LoginActivity;
import com.appdev.bilijuan.databinding.ActivityAdminBinding;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.utils.AdminNavHelper;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.NotificationUIHelper;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AdminDashboardActivity extends AppCompatActivity {

    private ActivityAdminBinding binding;
    private ListenerRegistration notifListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupBottomNav();
        loadAdminName();
        loadStats();
        listenForNotifications();

        binding.sectionOverview.setVisibility(View.VISIBLE);
        binding.btnLogout.setOnClickListener(v -> logout());
        binding.btnNotification.setOnClickListener(v -> NotificationUIHelper.showNotificationSheet(this));
    }

    private void listenForNotifications() {
        String uid = FirebaseHelper.getCurrentUid();
        if (uid == null) return;

        notifListener = FirebaseHelper.getDb().collection("notifications")
                .whereEqualTo("userId", uid)
                .whereEqualTo("read", false)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    binding.notifDot.setVisibility(snap.isEmpty() ? View.GONE : View.VISIBLE);
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
    }
}
