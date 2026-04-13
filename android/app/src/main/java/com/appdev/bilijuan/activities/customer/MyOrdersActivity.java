package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.adapters.CustomerOrdersAdapter;
import com.appdev.bilijuan.databinding.ActivityMyOrdersBinding;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyOrdersActivity extends AppCompatActivity {

    private ActivityMyOrdersBinding binding;
    private CustomerOrdersAdapter adapter;
    private final List<Order> allOrders = new ArrayList<>();
    private final List<Order> filtered  = new ArrayList<>();
    private ListenerRegistration listener;
    private String currentUid;
    private String activeTab = "Active";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUid = FirebaseHelper.getCurrentUid();
        if (currentUid == null) { finish(); return; }

        setupRecyclerView();
        setupBottomNav();
        setupTabs();
        binding.btnBack.setOnClickListener(v -> finish());
        listenOrders();
        checkForPendingReview();
    }

    private void setupRecyclerView() {
        adapter = new CustomerOrdersAdapter(filtered,
                this::onTrackOrder, this::onCancelOrder, this::onReorder);
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(adapter);
    }
    private void checkForPendingReview() {
        FirebaseHelper.getDb().collection("orders")
                .whereEqualTo("customerId", currentUid)
                .whereEqualTo("status", Order.STATUS_DELIVERED)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        Boolean reviewed = doc.getBoolean("reviewed");
                        if (reviewed == null || !reviewed) {
                            Order o = doc.toObject(Order.class);
                            o.setOrderId(doc.getId());
                            showReviewPrompt(o);
                            return; // show one at a time
                        }
                    }
                });
    }

    private void showReviewPrompt(Order order) {
        Intent intent = new Intent(this, ReviewPromptActivity.class);
        intent.putExtra("orderId",     order.getOrderId());
        intent.putExtra("productId",   order.getProductId());
        intent.putExtra("productName", order.getProductName());
        intent.putExtra("storeName",   order.getSellerName());
        startActivity(intent);
    }
    private void setupBottomNav() {
        binding.bottomNav.setSelectedItemId(R.id.nav_orders);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_orders) return true;
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void setupTabs() {
        binding.swipeRefresh.setColorSchemeColors(getColor(R.color.primary));
        binding.swipeRefresh.setOnRefreshListener(() -> {
            if (listener != null) listener.remove();
            listenOrders();
            binding.swipeRefresh.postDelayed(
                    () -> binding.swipeRefresh.setRefreshing(false), 1200);
        });

        binding.tabActive.setOnClickListener(v -> {
            activeTab = "Active";
            applyFilter();
            binding.tabActive.setAlpha(1f);
            binding.tabHistory.setAlpha(0.4f);
        });
        binding.tabHistory.setOnClickListener(v -> {
            activeTab = "History";
            applyFilter();
            binding.tabHistory.setAlpha(1f);
            binding.tabActive.setAlpha(0.4f);
        });
    }

    private void listenOrders() {
        listener = FirebaseHelper.getDb().collection("orders")
                .whereEqualTo("customerId", currentUid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    allOrders.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Order o = doc.toObject(Order.class);
                        o.setOrderId(doc.getId());
                        allOrders.add(o);
                    }
                    // Sort newest first
                    Collections.sort(allOrders, (a, b) -> {
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });
                    applyFilter();
                });
    }

    private void applyFilter() {
        filtered.clear();
        for (Order o : allOrders) {
            if ("Active".equals(activeTab) && o.isActive()) filtered.add(o);
            else if ("History".equals(activeTab) && !o.isActive()) filtered.add(o);
        }
        adapter.notifyDataSetChanged();
        binding.emptyOrders.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onTrackOrder(Order order) {
        Intent intent = new Intent(this, OrderTrackingActivity.class);
        intent.putExtra("orderId", order.getOrderId());
        startActivity(intent);
    }

    private void onCancelOrder(Order order) {
        if (!order.canCustomerCancel()) {
            Toast.makeText(this, "Cannot cancel at this stage.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel this order? This cannot be undone.")
                .setPositiveButton("Yes, Cancel", (d, w) ->
                        FirebaseHelper.getDb().collection("orders")
                                .document(order.getOrderId())
                                .update("status", Order.STATUS_CANCELLED)
                                .addOnSuccessListener(v ->
                                        Toast.makeText(this, "Order cancelled.",
                                                Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to cancel.",
                                                Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Keep Order", null)
                .show();
    }
    private void onReorder(Order order) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("productId", order.getProductId());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}
