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
import com.appdev.bilijuan.activities.LoginActivity;
import com.appdev.bilijuan.adapters.AdminOrdersAdapter;
import com.appdev.bilijuan.adapters.AdminUsersAdapter;
import com.appdev.bilijuan.databinding.ActivityAdminBinding;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity {

    private ActivityAdminBinding binding;
    private AdminOrdersAdapter ordersAdapter;
    private AdminUsersAdapter  usersAdapter;
    private AdminUsersAdapter  sellersAdapter;

    private final List<Order> orders  = new ArrayList<>();
    private final List<User>  users   = new ArrayList<>();
    private final List<User>  sellers = new ArrayList<>();

    private ListenerRegistration ordersListener;
    private ListenerRegistration usersListener;
    private ListenerRegistration sellersListener;

    // Disable reasons
    private static final String[] DISABLE_REASONS_STORE = {
            "Repeated customer complaints",
            "Selling prohibited items",
            "Unresponsive to orders",
            "Health/safety concern",
            "Inactive for 30+ days",
            "Other"
    };
    private static final String[] DISABLE_REASONS_CUSTOMER = {
            "Abusive behavior",
            "Repeated cancellations",
            "Suspicious activity",
            "Other"
    };
    private static final String[] ARCHIVE_REASONS_STORE = {
            "Fraud or scam",
            "Fake store",
            "Owner requested closure",
            "Multiple violations",
            "Other"
    };
    private static final String[] ARCHIVE_REASONS_CUSTOMER = {
            "Fraud",
            "Owner requested deletion",
            "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupTabs();
        setupRecyclerViews();
        loadAdminName();
        loadStats();
        showSection("overview");

        binding.btnLogout.setOnClickListener(v -> logout());
    }

    // ── Tabs ──────────────────────────────────────────────────────────────────

    private void setupTabs() {
        binding.tabOverview.setOnClickListener(v -> showSection("overview"));
        binding.tabUsers.setOnClickListener(v    -> showSection("users"));
        binding.tabOrders.setOnClickListener(v   -> showSection("orders"));
        binding.tabSellers.setOnClickListener(v  -> showSection("sellers"));
    }

    private void showSection(String section) {
        // Reset tabs
        TextView[] tabs = {binding.tabOverview, binding.tabUsers,
                binding.tabOrders, binding.tabSellers};
        for (TextView t : tabs) {
            t.setBackgroundResource(android.R.color.transparent);
            t.setTextColor(0xCCFFFFFF);
        }

        binding.sectionOverview.setVisibility(View.GONE);
        binding.sectionUsers.setVisibility(View.GONE);
        binding.sectionOrders.setVisibility(View.GONE);
        binding.sectionSellers.setVisibility(View.GONE);

        switch (section) {
            case "overview":
                activate(binding.tabOverview);
                binding.sectionOverview.setVisibility(View.VISIBLE);
                break;
            case "users":
                activate(binding.tabUsers);
                binding.sectionUsers.setVisibility(View.VISIBLE);
                loadUsers();
                break;
            case "orders":
                activate(binding.tabOrders);
                binding.sectionOrders.setVisibility(View.VISIBLE);
                loadOrders();
                break;
            case "sellers":
                activate(binding.tabSellers);
                binding.sectionSellers.setVisibility(View.VISIBLE);
                loadSellers();
                break;
        }
    }

    private void activate(TextView tab) {
        tab.setBackgroundResource(R.drawable.bg_tab_active_white);
        tab.setTextColor(getColor(R.color.primary));
    }

    // ── RecyclerViews ─────────────────────────────────────────────────────────

    private void setupRecyclerViews() {
        ordersAdapter = new AdminOrdersAdapter(orders);
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(ordersAdapter);

        usersAdapter = new AdminUsersAdapter(users,
                user -> showActionSheet(user, false),
                user -> showStoreDetail(user));
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUsers.setAdapter(usersAdapter);

        sellersAdapter = new AdminUsersAdapter(sellers,
                user -> showActionSheet(user, true),
                user -> showStoreDetail(user));
        binding.rvSellers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSellers.setAdapter(sellersAdapter);
    }

    // ── Admin Name ────────────────────────────────────────────────────────────

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

    // ── Stats ─────────────────────────────────────────────────────────────────

    private void loadStats() {
        FirebaseHelper.getDb().collection("users").get()
                .addOnSuccessListener(snap -> {
                    int c = 0, s = 0;
                    for (QueryDocumentSnapshot d : snap) {
                        String role = d.getString("role");
                        if ("customer".equals(role)) c++;
                        else if ("seller".equals(role)) s++;
                    }
                    binding.tvStatCustomers.setText(String.valueOf(c));
                    binding.tvStatSellers.setText(String.valueOf(s));
                });

        FirebaseHelper.getDb().collection("orders").get()
                .addOnSuccessListener(snap -> {
                    int active = 0, delivered = 0;
                    for (QueryDocumentSnapshot d : snap) {
                        String status = d.getString("status");
                        if (Order.STATUS_DELIVERED.equals(status)) delivered++;
                        else if (!Order.STATUS_CANCELLED.equals(status)) active++;
                    }
                    binding.tvStatActiveOrders.setText(String.valueOf(active));
                    binding.tvStatDelivered.setText(String.valueOf(delivered));
                });

        FirebaseHelper.getDb().collection("products").get()
                .addOnSuccessListener(snap ->
                        binding.tvStatProducts.setText(String.valueOf(snap.size())));
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    private void loadUsers() {
        if (usersListener != null) usersListener.remove();
        usersListener = FirebaseHelper.getDb().collection("users")
                .whereEqualTo("role", "customer")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    users.clear();
                    for (QueryDocumentSnapshot d : snap)
                        users.add(d.toObject(User.class));
                    usersAdapter.notifyDataSetChanged();
                    binding.tvUsersEmpty.setVisibility(
                            users.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    // ── Sellers / Stores ──────────────────────────────────────────────────────

    private void loadSellers() {
        if (sellersListener != null) sellersListener.remove();
        sellersListener = FirebaseHelper.getDb().collection("users")
                .whereEqualTo("role", "seller")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    sellers.clear();
                    for (QueryDocumentSnapshot d : snap)
                        sellers.add(d.toObject(User.class));
                    sellersAdapter.notifyDataSetChanged();
                    binding.tvSellersEmpty.setVisibility(
                            sellers.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    // ── Orders ────────────────────────────────────────────────────────────────

    private void loadOrders() {
        if (ordersListener != null) ordersListener.remove();
        ordersListener = FirebaseHelper.getDb().collection("orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    orders.clear();
                    for (QueryDocumentSnapshot d : snap) {
                        Order o = d.toObject(Order.class);
                        o.setOrderId(d.getId());
                        orders.add(o);
                    }
                    ordersAdapter.notifyDataSetChanged();
                    binding.tvOrdersEmpty.setVisibility(
                            orders.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    // ── Action Sheet (Disable / Archive) ─────────────────────────────────────

    private void showActionSheet(User user, boolean isStore) {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetStyle);
        View v = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_admin_action, null);
        sheet.setContentView(v);

        String currentStatus = user.getStatus();
        TextView tvName    = v.findViewById(R.id.tvUserName);
        View btnEnable     = v.findViewById(R.id.btnEnable);
        View btnDisable    = v.findViewById(R.id.btnDisable);
        View btnArchive    = v.findViewById(R.id.btnArchive);

        tvName.setText(user.getName());

        // Show/hide enable based on current status
        boolean isDisabled = "disabled".equals(currentStatus);
        btnEnable.setVisibility(isDisabled ? View.VISIBLE : View.GONE);
        btnDisable.setVisibility(isDisabled ? View.GONE : View.VISIBLE);
        btnArchive.setVisibility("archived".equals(currentStatus) ? View.GONE : View.VISIBLE);

        btnEnable.setOnClickListener(btn -> {
            sheet.dismiss();
            enableUser(user);
        });

        btnDisable.setOnClickListener(btn -> {
            sheet.dismiss();
            String[] reasons = isStore ? DISABLE_REASONS_STORE : DISABLE_REASONS_CUSTOMER;
            showReasonDialog(user, "disable", reasons);
        });

        btnArchive.setOnClickListener(btn -> {
            sheet.dismiss();
            String[] reasons = isStore ? ARCHIVE_REASONS_STORE : ARCHIVE_REASONS_CUSTOMER;
            showReasonDialog(user, "archive", reasons);
        });

        sheet.show();
    }

    private void showReasonDialog(User user, String action, String[] reasons) {
        // Build reason list as single-choice dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(action.equals("disable") ? "Reason for disabling" : "Reason for archiving")
                .setSingleChoiceItems(reasons, -1, (dialog, which) -> {
                    dialog.dismiss();
                    String selectedReason = reasons[which];

                    // Show note input
                    android.widget.EditText noteInput = new android.widget.EditText(this);
                    noteInput.setHint("Additional note (optional)");
                    noteInput.setPadding(48, 24, 48, 24);

                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Add a note")
                            .setView(noteInput)
                            .setPositiveButton("Confirm", (d2, w2) -> {
                                String note = noteInput.getText().toString().trim();
                                if ("disable".equals(action)) {
                                    disableUser(user, selectedReason, note);
                                } else {
                                    archiveUser(user, selectedReason, note);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void enableUser(User user) {
        FirebaseHelper.getDb().collection("users").document(user.getUid())
                .update("status", "active",
                        "disableReason", null,
                        "disableNote", null)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, user.getName() + " re-enabled",
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show());
    }

    private void disableUser(User user, String reason, String note) {
        String adminUid = FirebaseHelper.getCurrentUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status",        "disabled");
        updates.put("disableReason", reason);
        updates.put("disableNote",   note);
        updates.put("disabledBy",    adminUid);
        updates.put("disabledAt",    FieldValue.serverTimestamp());

        FirebaseHelper.getDb().collection("users").document(user.getUid())
                .update(updates)
                .addOnSuccessListener(v ->
                        Toast.makeText(this,
                                user.getName() + " disabled: " + reason,
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show());
    }

    private void archiveUser(User user, String reason, String note) {
        String adminUid = FirebaseHelper.getCurrentUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status",        "archived");
        updates.put("disableReason", reason);
        updates.put("disableNote",   note);
        updates.put("disabledBy",    adminUid);
        updates.put("disabledAt",    FieldValue.serverTimestamp());

        FirebaseHelper.getDb().collection("users").document(user.getUid())
                .update(updates)
                .addOnSuccessListener(v ->
                        Toast.makeText(this,
                                user.getName() + " archived: " + reason,
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show());
    }

    // ── Store Detail Bottom Sheet ─────────────────────────────────────────────

    private void showStoreDetail(User store) {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetStyle);
        View v = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_store_detail, null);
        sheet.setContentView(v);

        ((TextView) v.findViewById(R.id.tvStoreName)).setText(store.getName());
        ((TextView) v.findViewById(R.id.tvStorePhone)).setText(
                store.getPhone() != null ? store.getPhone() : "—");
        ((TextView) v.findViewById(R.id.tvStoreAddress)).setText(
                store.getAddress() != null ? store.getAddress() : "—");
        ((TextView) v.findViewById(R.id.tvStoreStatus)).setText(store.getStatus());
        ((TextView) v.findViewById(R.id.tvReportCount)).setText(
                store.getReportCount() + " reports");

        // Load store's products count + avg rating
        FirebaseHelper.getDb().collection("products")
                .whereEqualTo("sellerId", store.getUid()).get()
                .addOnSuccessListener(snap -> {
                    ((TextView) v.findViewById(R.id.tvProductCount))
                            .setText(snap.size() + " products");
                    float sum = 0; int count = 0;
                    for (QueryDocumentSnapshot d : snap) {
                        Float stars = d.getDouble("stars") != null
                                ? d.getDouble("stars").floatValue() : 0f;
                        int rc = d.getLong("ratingCount") != null
                                ? d.getLong("ratingCount").intValue() : 0;
                        if (rc > 0) { sum += stars; count++; }
                    }
                    String rating = count > 0
                            ? String.format("%.1f / 5.0", sum / count) : "No ratings";
                    ((TextView) v.findViewById(R.id.tvStoreRating)).setText(rating);
                });

        // Load delivered orders count
        FirebaseHelper.getDb().collection("orders")
                .whereEqualTo("sellerId", store.getUid())
                .whereEqualTo("status", Order.STATUS_DELIVERED).get()
                .addOnSuccessListener(snap ->
                        ((TextView) v.findViewById(R.id.tvOrderCount))
                                .setText(snap.size() + " orders delivered"));

        // Load reports
        FirebaseHelper.getDb().collection("reports")
                .whereEqualTo("storeId", store.getUid())
                .whereEqualTo("status", "pending").get()
                .addOnSuccessListener(snap -> {
                    int flagCount = snap.size();
                    TextView tvFlags = v.findViewById(R.id.tvFlagCount);
                    tvFlags.setText(flagCount > 0
                            ? flagCount + " pending reports" : "No pending reports");
                    tvFlags.setTextColor(getColor(
                            flagCount > 0 ? R.color.error : R.color.text_secondary));
                });

        // Action buttons
        v.findViewById(R.id.btnDisableStore).setOnClickListener(btn -> {
            sheet.dismiss();
            showReasonDialog(store, "disable", DISABLE_REASONS_STORE);
        });
        v.findViewById(R.id.btnArchiveStore).setOnClickListener(btn -> {
            sheet.dismiss();
            showReasonDialog(store, "archive", ARCHIVE_REASONS_STORE);
        });

        sheet.show();
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    private void logout() {
        FirebaseHelper.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ordersListener  != null) ordersListener.remove();
        if (usersListener   != null) usersListener.remove();
        if (sellersListener != null) sellersListener.remove();
    }
}