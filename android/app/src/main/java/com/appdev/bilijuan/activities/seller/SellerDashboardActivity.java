package com.appdev.bilijuan.activities.seller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.adapters.ActiveOrderCardAdapter;
import com.appdev.bilijuan.adapters.SellerMenuAdapter;
import com.appdev.bilijuan.databinding.ActivitySellerBinding;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.models.Product;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.DeliveryUtils;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SellerDashboardActivity extends AppCompatActivity {

    private ActivitySellerBinding binding;
    private ActiveOrderCardAdapter orderAdapter;
    private SellerMenuAdapter menuAdapter;
    private final List<Order> activeOrders = new ArrayList<>();
    private final List<Product> menuItems = new ArrayList<>();
    private ListenerRegistration ordersListener;
    private ListenerRegistration menuListener;
    private String sellerId;
    private double sellerLat;
    private double sellerLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySellerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sellerId = FirebaseHelper.getCurrentUid();
        if (sellerId == null) {
            finish();
            return;
        }

        setupRecyclerViews();
        setupBottomNav();
        setupClickListeners();
        loadSellerProfile();
    }

    private void setupRecyclerViews() {
        orderAdapter = new ActiveOrderCardAdapter(activeOrders, this::onOrderAction);
        binding.rvActiveOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvActiveOrders.setAdapter(orderAdapter);
        binding.rvActiveOrders.setNestedScrollingEnabled(false);

        menuAdapter = new SellerMenuAdapter(menuItems, this::onMenuItemToggle, this::onMenuItemEdit);
        binding.rvMenu.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMenu.setAdapter(menuAdapter);
        binding.rvMenu.setNestedScrollingEnabled(false);
    }

    private void setupBottomNav() {
        binding.bottomNav.setSelectedItemId(R.id.nav_dashboard);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) return true;
            if (id == R.id.nav_orders) {
                startActivity(new Intent(this, SellerOrdersActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            if (id == R.id.nav_menu) {
                startActivity(new Intent(this, AddProductActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void setupClickListeners() {
        binding.btnAddItem.setOnClickListener(v ->
                startActivity(new Intent(this, AddProductActivity.class)));
        binding.tvSeeAllOrders.setOnClickListener(v ->
                startActivity(new Intent(this, SellerOrdersActivity.class)));
    }

    private void loadSellerProfile() {
        FirebaseHelper.getDb().collection("users").document(sellerId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    User seller = doc.toObject(User.class);
                    if (seller == null) return;
                    sellerLat = seller.getLatitude();
                    sellerLng = seller.getLongitude();
                    binding.tvSellerName.setText("Hello, " + seller.getName());
                    listenOrders();
                    listenMenu();
                    loadStats();
                });
    }

    private void listenOrders() {
        ordersListener = FirebaseHelper.getDb()
                .collection("orders")
                .whereEqualTo("sellerId", sellerId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    activeOrders.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Order o = doc.toObject(Order.class);
                        o.setOrderId(doc.getId());
                        if (o.isActive()) activeOrders.add(o);
                    }
                    Collections.sort(activeOrders, (a, b) -> {
                        double dA = DeliveryUtils.haversineKm(sellerLat, sellerLng,
                                a.getCustomerLat(), a.getCustomerLng());
                        double dB = DeliveryUtils.haversineKm(sellerLat, sellerLng,
                                b.getCustomerLat(), b.getCustomerLng());
                        return Double.compare(dA, dB);
                    });
                    orderAdapter.notifyDataSetChanged();
                    binding.tvActiveOrderCount.setText(String.valueOf(activeOrders.size()));
                    binding.emptyOrders.setVisibility(
                            activeOrders.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void listenMenu() {
        menuListener = FirebaseHelper.getDb()
                .collection("products")
                .whereEqualTo("sellerId", sellerId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    menuItems.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Product p = doc.toObject(Product.class);
                        p.setProductId(doc.getId());
                        menuItems.add(p);
                    }
                    menuAdapter.notifyDataSetChanged();
                    binding.emptyMenu.setVisibility(
                            menuItems.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void loadStats() {
        FirebaseHelper.getDb().collection("orders")
                .whereEqualTo("sellerId", sellerId)
                .whereEqualTo("status", Order.STATUS_DELIVERED)
                .get()
                .addOnSuccessListener(snap ->
                        binding.tvTotalOrders.setText(String.valueOf(snap.size())));

        FirebaseHelper.getDb().collection("products")
                .whereEqualTo("sellerId", sellerId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) return;
                    float sumStars = 0;
                    int count = 0;
                    for (QueryDocumentSnapshot doc : snap) {
                        Product p = doc.toObject(Product.class);
                        if (p.getRatingCount() > 0) {
                            sumStars += p.getStars();
                            count++;
                        }
                    }
                    if (count > 0)
                        binding.tvRating.setText(String.format("%.1f", sumStars / count));
                });
    }

    private void onOrderAction(Order order) {
        String next = nextStatus(order.getStatus());
        if (next == null) return;
        FirebaseHelper.getDb().collection("orders")
                .document(order.getOrderId())
                .update("status", next)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "Order → " + next, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }

    private void onMenuItemToggle(Product product, boolean available) {
        FirebaseHelper.getDb().collection("products")
                .document(product.getProductId())
                .update("available", available);
    }

    private void onMenuItemEdit(Product product) {
        Intent intent = new Intent(this, AddProductActivity.class);
        intent.putExtra("productId", product.getProductId());
        startActivity(intent);
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
        if (ordersListener != null) ordersListener.remove();
        if (menuListener != null) menuListener.remove();
    }
}