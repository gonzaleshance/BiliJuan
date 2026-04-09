package com.appdev.bilijuan.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

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
        // Reset all tabs
        int[] tabs = {R.id.tabOverview, R.id.tabUsers, R.id.tabOrders, R.id.tabSellers};
        View[] tabViews = {binding.tabOverview, binding.tabUsers,
                binding.tabOrders, binding.tabSellers};
        for (View t : tabViews) {
            ((android.widget.TextView) t)
                    .setBackgroundResource(android.R.color.transparent);
            ((android.widget.TextView) t).setTextColor(0xCCFFFFFF);
        }

        // Hide all sections
        binding.sectionOverview.setVisibility(View.GONE);
        binding.sectionUsers.setVisibility(View.GONE);
        binding.sectionOrders.setVisibility(View.GONE);
        binding.sectionSellers.setVisibility(View.GONE);

        // Activate selected
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

    private void activate(android.widget.TextView tab) {
        tab.setBackgroundResource(R.drawable.bg_tab_active_white);
        tab.setTextColor(getColor(R.color.primary));
    }

    // ── RecyclerViews ─────────────────────────────────────────────────────────

    private void setupRecyclerViews() {
        ordersAdapter = new AdminOrdersAdapter(orders);
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(ordersAdapter);

        usersAdapter = new AdminUsersAdapter(users, this::toggleApproved);
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUsers.setAdapter(usersAdapter);

        sellersAdapter = new AdminUsersAdapter(sellers, this::toggleApproved);
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

    // ── Data loaders ──────────────────────────────────────────────────────────

    private void loadUsers() {
        if (usersListener != null) usersListener.remove();
        usersListener = FirebaseHelper.getDb().collection("users")
                .whereEqualTo("role", "customer")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    users.clear();
                    for (QueryDocumentSnapshot d : snap) {
                        users.add(d.toObject(User.class));
                    }
                    usersAdapter.notifyDataSetChanged();
                    binding.tvUsersEmpty.setVisibility(
                            users.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void loadSellers() {
        if (sellersListener != null) sellersListener.remove();
        sellersListener = FirebaseHelper.getDb().collection("users")
                .whereEqualTo("role", "seller")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    sellers.clear();
                    for (QueryDocumentSnapshot d : snap) {
                        sellers.add(d.toObject(User.class));
                    }
                    sellersAdapter.notifyDataSetChanged();
                    binding.tvSellersEmpty.setVisibility(
                            sellers.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

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

    // ── Toggle User Approved ──────────────────────────────────────────────────

    private void toggleApproved(User user) {
        boolean newStatus = !user.isApproved();
        FirebaseHelper.getDb().collection("users").document(user.getUid())
                .update("approved", newStatus)
                .addOnSuccessListener(v ->
                        Toast.makeText(this,
                                user.getName() + (newStatus ? " approved" : " suspended"),
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
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