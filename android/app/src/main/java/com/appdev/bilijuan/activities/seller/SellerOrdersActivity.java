package com.appdev.bilijuan.activities.seller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.adapters.SellerOrdersAdapter;
import com.appdev.bilijuan.databinding.ActivitySellerOrdersBinding;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.utils.DeliveryUtils;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SellerOrdersActivity extends AppCompatActivity {

    private ActivitySellerOrdersBinding binding;
    private SellerOrdersAdapter adapter;
    private final List<Order> allOrders = new ArrayList<>();
    private final List<Order> filteredOrders = new ArrayList<>();
    private ListenerRegistration listener;
    private String sellerId;
    private double sellerLat, sellerLng;
    private String activeFilter = "Active"; // Active | History

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySellerOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sellerId = FirebaseHelper.getCurrentUid();
        if (sellerId == null) { finish(); return; }

        setupRecyclerView();
        setupBottomNav();
        setupFilterChips();
        binding.btnBack.setOnClickListener(v -> finish());
        loadSellerLocation();
    }

    private void setupRecyclerView() {
        adapter = new SellerOrdersAdapter(filteredOrders, this::onAdvanceStatus);
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(adapter);
    }

    private void setupBottomNav() {
        binding.bottomNav.setSelectedItemId(com.appdev.bilijuan.R.id.nav_orders);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == com.appdev.bilijuan.R.id.nav_orders) return true;
            if (id == com.appdev.bilijuan.R.id.nav_dashboard) {
                startActivity(new Intent(this, SellerDashboardActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            if (id == com.appdev.bilijuan.R.id.nav_menu) {
                startActivity(new Intent(this, AddProductActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void setupFilterChips() {
        binding.chipActive.setOnClickListener(v -> {
            activeFilter = "Active";
            applyFilter();
            binding.chipActive.setBackground(getDrawable(com.appdev.bilijuan.R.drawable.bg_chip_active));
            binding.chipHistory.setBackground(getDrawable(com.appdev.bilijuan.R.drawable.bg_chip_inactive));
        });
        binding.chipHistory.setOnClickListener(v -> {
            activeFilter = "History";
            applyFilter();
            binding.chipActive.setBackground(getDrawable(com.appdev.bilijuan.R.drawable.bg_chip_inactive));
            binding.chipHistory.setBackground(getDrawable(com.appdev.bilijuan.R.drawable.bg_chip_active));
        });
    }

    private void loadSellerLocation() {
        FirebaseHelper.getDb().collection("users").document(sellerId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    Double lat = doc.getDouble("latitude");
                    Double lng = doc.getDouble("longitude");
                    sellerLat = lat != null ? lat : 0;
                    sellerLng = lng != null ? lng : 0;
                    listenOrders();
                });
    }

    private void listenOrders() {
        listener = FirebaseHelper.getDb().collection("orders")
                .whereEqualTo("sellerId", sellerId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    allOrders.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Order o = doc.toObject(Order.class);
                        o.setOrderId(doc.getId());
                        allOrders.add(o);
                    }
                    // Sort active orders by distance (nearest first), history by time (newest first)
                    applyFilter();
                });
    }

    private void applyFilter() {
        filteredOrders.clear();
        if ("Active".equals(activeFilter)) {
            for (Order o : allOrders) { if (o.isActive()) filteredOrders.add(o); }
            Collections.sort(filteredOrders, (a, b) -> {
                double dA = DeliveryUtils.haversineKm(sellerLat, sellerLng, a.getCustomerLat(), a.getCustomerLng());
                double dB = DeliveryUtils.haversineKm(sellerLat, sellerLng, b.getCustomerLat(), b.getCustomerLng());
                return Double.compare(dA, dB);
            });
        } else {
            for (Order o : allOrders) { if (!o.isActive()) filteredOrders.add(o); }
            Collections.sort(filteredOrders, (a, b) -> {
                if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });
        }
        adapter.notifyDataSetChanged();
        binding.emptyOrders.setVisibility(filteredOrders.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onAdvanceStatus(Order order) {
        String next = nextStatus(order.getStatus());
        if (next == null) return;
        FirebaseHelper.getDb().collection("orders").document(order.getOrderId())
                .update("status", next)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "Status → " + next, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show());
    }

    private String nextStatus(String current) {
        switch (current) {
            case Order.STATUS_PENDING:    return Order.STATUS_CONFIRMED;
            case Order.STATUS_CONFIRMED:  return Order.STATUS_PREPARING;
            case Order.STATUS_PREPARING:  return Order.STATUS_ON_THE_WAY;
            case Order.STATUS_ON_THE_WAY: return Order.STATUS_DELIVERED;
            default: return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}