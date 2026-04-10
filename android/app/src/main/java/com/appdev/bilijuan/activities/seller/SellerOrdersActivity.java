package com.appdev.bilijuan.activities.seller;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.adapters.SellerOrdersAdapter;
import com.appdev.bilijuan.databinding.ActivitySellerOrdersBinding;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.utils.DeliveryUtils;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.NotificationHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SellerOrdersActivity  (UPDATED — wires onViewMap callback)
 *
 * Only change from original:
 *   • setupRecyclerView() now passes an ActionListener that implements
 *     both onAdvance() and onViewMap()
 *   • onViewMap() launches SellerDeliveryMapActivity with the order ID
 *
 * Everything else is identical to the original file.
 */
public class SellerOrdersActivity extends AppCompatActivity {

    private ActivitySellerOrdersBinding binding;
    private SellerOrdersAdapter adapter;
    private final List<Order> allOrders      = new ArrayList<>();
    private final List<Order> filteredOrders = new ArrayList<>();
    private ListenerRegistration listener;
    private String sellerId;
    private double sellerLat, sellerLng;
    private String activeFilter = "Active";

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

    // ── RecyclerView — now includes onViewMap ─────────────────────────────────

    private void setupRecyclerView() {
        adapter = new SellerOrdersAdapter(filteredOrders, new SellerOrdersAdapter.ActionListener() {

            @Override
            public void onAdvance(Order order) {
                SellerOrdersActivity.this.onAdvanceStatus(order);
            }

            @Override
            public void onViewMap(Order order) {
                // ── NEW: open the delivery map screen ──────────────────────
                Intent intent = new Intent(
                        SellerOrdersActivity.this,
                        SellerDeliveryMapActivity.class);
                intent.putExtra("orderId", order.getOrderId());
                startActivity(intent);
            }
        });

        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(adapter);
    }

    // ── Everything below is identical to the original ─────────────────────────

    private void setupBottomNav() {
        binding.bottomNav.setSelectedItemId(R.id.nav_orders);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_orders) return true;
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, SellerDashboardActivity.class));
                overridePendingTransition(0, 0);
                finish(); return true;
            }
            if (id == R.id.nav_post) {
                showPostBottomSheet();
                return false;
            }
            if (id == R.id.nav_account) {
                startActivity(new Intent(this, SellerAccountActivity.class));
                overridePendingTransition(0, 0);
                finish(); return true;
            }
            return false;
        });
    }

    private void showPostBottomSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetStyle);
        View v = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_post, null);
        sheet.setContentView(v);
        v.findViewById(R.id.btnNewItem).setOnClickListener(btn -> {
            sheet.dismiss();
            startActivity(new Intent(this, AddProductActivity.class));
        });
        v.findViewById(R.id.btnEditExisting).setOnClickListener(btn -> {
            sheet.dismiss();
            startActivity(new Intent(this, SellerAccountActivity.class)
                    .putExtra("openMenu", true));
        });
        sheet.show();
    }

    private void setupFilterChips() {
        binding.chipActive.setOnClickListener(v -> {
            activeFilter = "Active";
            applyFilter();
            binding.chipActive.setBackgroundResource(R.drawable.bg_chip_active);
            binding.chipHistory.setBackgroundResource(R.drawable.bg_chip_inactive);
        });
        binding.chipHistory.setOnClickListener(v -> {
            activeFilter = "History";
            applyFilter();
            binding.chipHistory.setBackgroundResource(R.drawable.bg_chip_active);
            binding.chipActive.setBackgroundResource(R.drawable.bg_chip_inactive);
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
                    applyFilter();
                });
    }

    private void applyFilter() {
        filteredOrders.clear();
        if ("Active".equals(activeFilter)) {
            for (Order o : allOrders) if (o.isActive()) filteredOrders.add(o);
            Collections.sort(filteredOrders, (a, b) -> {
                double dA = DeliveryUtils.haversineKm(sellerLat, sellerLng,
                        a.getCustomerLat(), a.getCustomerLng());
                double dB = DeliveryUtils.haversineKm(sellerLat, sellerLng,
                        b.getCustomerLat(), b.getCustomerLng());
                return Double.compare(dA, dB);
            });
        } else {
            for (Order o : allOrders) if (!o.isActive()) filteredOrders.add(o);
            Collections.sort(filteredOrders, (a, b) -> {
                if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });
        }
        adapter.notifyDataSetChanged();
        binding.emptyOrders.setVisibility(
                filteredOrders.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onAdvanceStatus(Order order) {
        String next = nextStatus(order.getStatus());
        if (next == null) return;
        FirebaseHelper.getDb().collection("orders")
                .document(order.getOrderId())
                .update("status", next)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Order → " + next, Toast.LENGTH_SHORT).show();
                    NotificationHelper.notifyStatusChange(
                            order.getOrderId(), next, order.getProductName());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
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