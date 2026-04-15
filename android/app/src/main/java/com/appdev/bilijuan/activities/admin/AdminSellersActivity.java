package com.appdev.bilijuan.activities.admin;

import android.content.Intent;
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

    // allSellers holds every seller from Firestore
    // displayedSellers is the filtered subset shown in the RecyclerView
    private final List<User> allSellers       = new ArrayList<>();
    private final List<User> displayedSellers = new ArrayList<>();

    private ListenerRegistration sellersListener, notifListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminSellersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupBottomNav();
        setupRecyclerView();
        setupSearch();
        loadSellers();
        listenForNotifications();

        binding.btnNotification.setOnClickListener(
                v -> NotificationUIHelper.showNotificationSheet(this));
    }

    // ── Bottom nav ────────────────────────────────────────────────────────────

    private void setupBottomNav() {
        AdminNavHelper.setup(this, binding.adminNav.getRoot(),
                AdminNavHelper.Tab.SELLERS);
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        sellersAdapter = new AdminUsersAdapter(
                displayedSellers,
                this::showActionSheet,
                user -> {
                    // Tap → view seller's foods
                    Intent intent = new Intent(this, AdminFoodsActivity.class);
                    intent.putExtra("sellerId",   user.getUid());
                    intent.putExtra("sellerName", user.getName());
                    startActivity(intent);
                });
        binding.rvSellers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSellers.setAdapter(sellersAdapter);
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                filterSellers(s.toString().trim());
            }
        });
    }

    private void filterSellers(String query) {
        displayedSellers.clear();
        if (query.isEmpty()) {
            displayedSellers.addAll(allSellers);
        } else {
            String lower = query.toLowerCase();
            for (User u : allSellers) {
                boolean nameMatch  = u.getName()  != null && u.getName().toLowerCase().contains(lower);
                boolean phoneMatch = u.getPhone() != null && u.getPhone().contains(lower);
                boolean statusMatch = u.getStatus() != null && u.getStatus().toLowerCase().contains(lower);
                if (nameMatch || phoneMatch || statusMatch) displayedSellers.add(u);
            }
        }
        sellersAdapter.notifyDataSetChanged();
        binding.tvEmpty.setVisibility(displayedSellers.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ── Load sellers ──────────────────────────────────────────────────────────

    private void loadSellers() {
        sellersListener = FirebaseHelper.getDb().collection("users")
                .whereEqualTo("role", "seller")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    allSellers.clear();
                    int openCount = 0;
                    for (QueryDocumentSnapshot d : snap) {
                        User u = d.toObject(User.class);
                        allSellers.add(u);
                        if (u.isOpen()) openCount++;
                    }

                    // Re-apply current search query
                    String currentQuery = binding.etSearch.getText() != null
                            ? binding.etSearch.getText().toString().trim() : "";
                    filterSellers(currentQuery);

                    binding.tvTotalSellers.setText(String.valueOf(allSellers.size()));
                    binding.tvOpenSellers.setText(String.valueOf(openCount));
                    binding.tvEmpty.setVisibility(
                            displayedSellers.isEmpty() ? View.VISIBLE : View.GONE);
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
        if (sellersListener != null) sellersListener.remove();
        if (notifListener   != null) notifListener.remove();
    }
}