package com.appdev.bilijuan.activities.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.adapters.AdminUsersAdapter;
import com.appdev.bilijuan.databinding.ActivityAdminUsersBinding;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.AdminNavHelper;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.NotificationUIHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminUsersActivity extends AppCompatActivity {

    private ActivityAdminUsersBinding binding;
    private AdminUsersAdapter usersAdapter;
    private final List<User> users = new ArrayList<>();
    private ListenerRegistration usersListener, notifListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupBottomNav();
        setupRecyclerView();
        loadUsers();
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
        AdminNavHelper.setup(this, binding.adminNav.getRoot(), AdminNavHelper.Tab.USERS);
    }

    private void setupRecyclerView() {
        usersAdapter = new AdminUsersAdapter(users,
                this::showActionSheet,
                user -> {});
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUsers.setAdapter(usersAdapter);
    }

    private void loadUsers() {
        usersListener = FirebaseHelper.getDb().collection("users")
                .whereEqualTo("role", "customer")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    users.clear();
                    int activeCount = 0;
                    for (QueryDocumentSnapshot d : snap) {
                        User u = d.toObject(User.class);
                        users.add(u);
                        if ("active".equals(u.getStatus())) activeCount++;
                    }
                    usersAdapter.notifyDataSetChanged();
                    
                    binding.tvTotalUsers.setText(String.valueOf(users.size()));
                    binding.tvActiveUsers.setText(String.valueOf(activeCount));
                    
                    binding.tvEmpty.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
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
        if (usersListener != null) usersListener.remove();
        if (notifListener != null) notifListener.remove();
    }
}
