package com.appdev.bilijuan.activities.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

    private final List<User> allUsers       = new ArrayList<>();
    private final List<User> displayedUsers = new ArrayList<>();

    private ListenerRegistration usersListener, notifListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupBottomNav();
        setupRecyclerView();
        setupSearch();
        loadUsers();
        listenForNotifications();

        binding.btnNotification.setOnClickListener(
                v -> NotificationUIHelper.showNotificationSheet(this));
    }

    // ── Bottom nav ────────────────────────────────────────────────────────────

    private void setupBottomNav() {
        AdminNavHelper.setup(this, binding.adminNav.getRoot(),
                AdminNavHelper.Tab.USERS);
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        usersAdapter = new AdminUsersAdapter(
                displayedUsers,
                this::showActionSheet,
                user -> {});   // No detail drill-down for customers
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUsers.setAdapter(usersAdapter);
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                filterUsers(s.toString().trim());
            }
        });
    }

    private void filterUsers(String query) {
        displayedUsers.clear();
        if (query.isEmpty()) {
            displayedUsers.addAll(allUsers);
        } else {
            String lower = query.toLowerCase();
            for (User u : allUsers) {
                boolean nameMatch   = u.getName()   != null && u.getName().toLowerCase().contains(lower);
                boolean emailMatch  = u.getEmail()  != null && u.getEmail().toLowerCase().contains(lower);
                boolean phoneMatch  = u.getPhone()  != null && u.getPhone().contains(lower);
                boolean statusMatch = u.getStatus() != null && u.getStatus().toLowerCase().contains(lower);
                if (nameMatch || emailMatch || phoneMatch || statusMatch)
                    displayedUsers.add(u);
            }
        }
        usersAdapter.notifyDataSetChanged();
        binding.tvEmpty.setVisibility(displayedUsers.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ── Load users ────────────────────────────────────────────────────────────

    private void loadUsers() {
        usersListener = FirebaseHelper.getDb().collection("users")
                .whereEqualTo("role", "customer")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    allUsers.clear();
                    int activeCount = 0;
                    for (QueryDocumentSnapshot d : snap) {
                        User u = d.toObject(User.class);
                        allUsers.add(u);
                        if ("active".equals(u.getStatus())) activeCount++;
                    }

                    String currentQuery = binding.etSearch.getText() != null
                            ? binding.etSearch.getText().toString().trim() : "";
                    filterUsers(currentQuery);

                    binding.tvTotalUsers.setText(String.valueOf(allUsers.size()));
                    binding.tvActiveUsers.setText(String.valueOf(activeCount));
                    binding.tvEmpty.setVisibility(
                            displayedUsers.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    // ── Action sheet ──────────────────────────────────────────────────────────

    private void showActionSheet(User user) {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetStyle);
        View v = LayoutInflater.from(this).inflate(
                R.layout.bottom_sheet_admin_action, null);
        sheet.setContentView(v);
        ((TextView) v.findViewById(R.id.tvUserName)).setText(user.getName());
        v.findViewById(R.id.btnArchive).setOnClickListener(btn -> {
            sheet.dismiss();
            FirebaseHelper.getDb().collection("users")
                    .document(user.getUid())
                    .update("status", "archived")
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "User archived",
                                    Toast.LENGTH_SHORT).show());
        });
        sheet.show();
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private void listenForNotifications() {
        String uid = FirebaseHelper.getCurrentUid();
        if (uid == null) return;
        notifListener = FirebaseHelper.getDb().collection("notifications")
                .whereEqualTo("userId", uid)
                .whereEqualTo("read", false)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    binding.notifDot.setVisibility(
                            snap.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (usersListener != null) usersListener.remove();
        if (notifListener != null) notifListener.remove();
    }
}