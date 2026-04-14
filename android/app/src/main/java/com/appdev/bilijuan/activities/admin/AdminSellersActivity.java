package com.appdev.bilijuan.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.adapters.AdminUsersAdapter;
import com.appdev.bilijuan.databinding.ActivityAdminSellersBinding;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.AdminNavHelper;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.NotificationUIHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminSellersActivity extends AppCompatActivity {

    private ActivityAdminSellersBinding binding;
    private AdminUsersAdapter sellersAdapter;
    private final List<User> sellers = new ArrayList<>();
    private ListenerRegistration sellersListener, notifListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminSellersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupBottomNav();
        setupRecyclerView();
        loadSellers();
        listenForNotifications();

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
        AdminNavHelper.setup(this, binding.adminNav.getRoot(), AdminNavHelper.Tab.SELLERS);
    }

    private void setupRecyclerView() {
        sellersAdapter = new AdminUsersAdapter(sellers,
                this::showActionSheet,
                user -> {
                    Intent intent = new Intent(this, AdminFoodsActivity.class);
                    intent.putExtra("sellerId", user.getUid());
                    intent.putExtra("sellerName", user.getName());
                    startActivity(intent);
                });
        binding.rvSellers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSellers.setAdapter(sellersAdapter);
    }

    private void loadSellers() {
        sellersListener = FirebaseHelper.getDb().collection("users")
                .whereEqualTo("role", "seller")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    sellers.clear();
                    int openCount = 0;
                    for (QueryDocumentSnapshot d : snap) {
                        User u = d.toObject(User.class);
                        sellers.add(u);
                        if (u.isOpen()) openCount++;
                    }
                    sellersAdapter.notifyDataSetChanged();
                    
                    binding.tvTotalSellers.setText(String.valueOf(sellers.size()));
                    binding.tvOpenSellers.setText(String.valueOf(openCount));
                    
                    binding.tvEmpty.setVisibility(sellers.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void showActionSheet(User user) {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetStyle);
        View v = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_admin_action, null);
        sheet.setContentView(v);
        ((TextView) v.findViewById(R.id.tvUserName)).setText(user.getName());
        v.findViewById(R.id.btnArchive).setOnClickListener(btn -> {
            sheet.dismiss();
            FirebaseHelper.getDb().collection("users").document(user.getUid()).update("status", "archived")
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "User archived", Toast.LENGTH_SHORT).show());
        });
        sheet.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sellersListener != null) sellersListener.remove();
        if (notifListener != null) notifListener.remove();
    }
}
