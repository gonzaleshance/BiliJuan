package com.appdev.bilijuan.activities.seller;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.appdev.bilijuan.utils.NotificationHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellerDashboardActivity extends AppCompatActivity {

    private ActivitySellerBinding binding;
    private ActiveOrderCardAdapter recentOrdersAdapter;
    private ActiveOrderCardAdapter ordersTabAdapter;
    private SellerMenuAdapter menuAdapter;

    private final List<Order> allOrders   = new ArrayList<>();
    private final List<Order> recentOrders = new ArrayList<>();
    private final List<Product> menuItems  = new ArrayList<>();

    private ListenerRegistration ordersListener;
    private ListenerRegistration menuListener;

    private String sellerId;
    private double sellerLat, sellerLng;
    private String activeTab = "overview";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySellerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sellerId = FirebaseHelper.getCurrentUid();
        if (sellerId == null) { finish(); return; }

        setupTopTabs();
        setupBottomNav();
        setupRecyclerViews();
        loadSellerProfile();
    }

    // ── Top Tabs ──────────────────────────────────────────────────────────────

    private void setupTopTabs() {
        binding.tabOverview.setOnClickListener(v  -> switchTab("overview"));
        binding.tabProducts.setOnClickListener(v  -> switchTab("products"));
        binding.tabOrders.setOnClickListener(v    -> switchTab("orders"));
        switchTab("overview");
    }

    private void switchTab(String tab) {
        activeTab = tab;

        // Reset all tabs
        binding.tabOverview.setBackgroundResource(android.R.color.transparent);
        binding.tabProducts.setBackgroundResource(android.R.color.transparent);
        binding.tabOrders.setBackgroundResource(android.R.color.transparent);
        binding.tabOverview.setTextColor(0xCCFFFFFF);
        binding.tabProducts.setTextColor(0xCCFFFFFF);
        binding.tabOrders.setTextColor(0xCCFFFFFF);

        // Activate selected
        switch (tab) {
            case "overview":
                binding.tabOverview.setBackgroundResource(R.drawable.bg_tab_active_white);
                binding.tabOverview.setTextColor(getColor(R.color.primary));
                binding.sectionOverview.setVisibility(View.VISIBLE);
                binding.sectionProducts.setVisibility(View.GONE);
                binding.sectionOrders.setVisibility(View.GONE);
                break;
            case "products":
                binding.tabProducts.setBackgroundResource(R.drawable.bg_tab_active_white);
                binding.tabProducts.setTextColor(getColor(R.color.primary));
                binding.sectionOverview.setVisibility(View.GONE);
                binding.sectionProducts.setVisibility(View.VISIBLE);
                binding.sectionOrders.setVisibility(View.GONE);
                break;
            case "orders":
                binding.tabOrders.setBackgroundResource(R.drawable.bg_tab_active_white);
                binding.tabOrders.setTextColor(getColor(R.color.primary));
                binding.sectionOverview.setVisibility(View.GONE);
                binding.sectionProducts.setVisibility(View.GONE);
                binding.sectionOrders.setVisibility(View.VISIBLE);
                break;
        }
    }

    // ── Bottom Nav ────────────────────────────────────────────────────────────

    private void setupBottomNav() {
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home)   return true;
            if (id == R.id.nav_post) {
                showPostBottomSheet(); return false;
            }
            if (id == R.id.nav_orders) {
                startActivity(new Intent(this, SellerOrdersActivity.class));
                overridePendingTransition(0, 0); return true;
            }
            if (id == R.id.nav_account) {
                startActivity(new Intent(this, SellerAccountActivity.class));
                overridePendingTransition(0, 0); return true;
            }
            return false;
        });
    }

    // ── Post Bottom Sheet ─────────────────────────────────────────────────────

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
            switchTab("products");
        });
        sheet.show();
    }

    // ── RecyclerViews ─────────────────────────────────────────────────────────

    private void setupRecyclerViews() {
        // Recent orders (overview tab)
        recentOrdersAdapter = new ActiveOrderCardAdapter(recentOrders, this::onOrderAction);
        binding.rvActiveOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvActiveOrders.setAdapter(recentOrdersAdapter);
        binding.rvActiveOrders.setNestedScrollingEnabled(false);

        // All orders (orders tab)
        ordersTabAdapter = new ActiveOrderCardAdapter(allOrders, this::onOrderAction);
        binding.rvActiveOrders2.setLayoutManager(new LinearLayoutManager(this));
        binding.rvActiveOrders2.setAdapter(ordersTabAdapter);

        // Menu (products tab)
        menuAdapter = new SellerMenuAdapter(menuItems,
                this::onMenuItemToggle, this::onMenuItemEdit, this::onMenuItemDelete);
        binding.rvMenu.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMenu.setAdapter(menuAdapter);
        binding.rvMenu.setNestedScrollingEnabled(false);

        binding.btnAddItem.setOnClickListener(v ->
                startActivity(new Intent(this, AddProductActivity.class)));
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    private void loadSellerProfile() {
        FirebaseHelper.getDb().collection("users").document(sellerId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    User seller = doc.toObject(User.class);
                    if (seller == null) return;
                    sellerLat = seller.getLatitude();
                    sellerLng = seller.getLongitude();
                    binding.tvSellerName.setText(seller.getName());
                    listenOrders();
                    listenMenu();
                });
    }

    // ── Orders Listener ───────────────────────────────────────────────────────

    private void listenOrders() {
        ordersListener = FirebaseHelper.getDb()
                .collection("orders")
                .whereEqualTo("sellerId", sellerId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    allOrders.clear();
                    double totalSales = 0;
                    int todayCount = 0;
                    Map<Integer, Double> weekSales = new HashMap<>();

                    Calendar today = Calendar.getInstance();

                    for (QueryDocumentSnapshot doc : snap) {
                        Order o = doc.toObject(Order.class);
                        o.setOrderId(doc.getId());
                        allOrders.add(o);

                        if (Order.STATUS_DELIVERED.equals(o.getStatus())) {
                            totalSales += o.getTotalAmount();
                        }

                        // Today's orders
                        if (o.getCreatedAt() != null) {
                            Calendar oCal = Calendar.getInstance();
                            oCal.setTime(o.getCreatedAt());
                            if (oCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                                    && oCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                                todayCount++;
                            }
                            // Weekly sales (current week)
                            if (oCal.get(Calendar.WEEK_OF_YEAR) == today.get(Calendar.WEEK_OF_YEAR)
                                    && oCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                                    && Order.STATUS_DELIVERED.equals(o.getStatus())) {
                                int dow = oCal.get(Calendar.DAY_OF_WEEK); // 1=Sun,2=Mon...
                                weekSales.merge(dow, o.getTotalAmount(), Double::sum);
                            }
                        }
                    }

                    // Sort all orders newest first
                    Collections.sort(allOrders, (a, b) -> {
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });

                    // Recent = first 5 active
                    recentOrders.clear();
                    int count = 0;
                    for (Order o : allOrders) {
                        if (o.isActive() && count < 5) { recentOrders.add(o); count++; }
                    }

                    recentOrdersAdapter.notifyDataSetChanged();
                    ordersTabAdapter.notifyDataSetChanged();

                    // Update stats
                    binding.tvTotalOrders.setText(String.valueOf(allOrders.size()));
                    binding.tvTotalSales.setText(String.format("₱%.0f", totalSales));
                    binding.tvOrdersToday.setText("+" + todayCount + " today");
                    binding.emptyOrders.setVisibility(recentOrders.isEmpty() ? View.VISIBLE : View.GONE);

                    updateWeeklyChart(weekSales);
                });
    }

    // ── Weekly Chart ──────────────────────────────────────────────────────────

    private void updateWeeklyChart(Map<Integer, Double> weekSales) {
        // Find max for scaling
        double max = 1;
        for (double v : weekSales.values()) if (v > max) max = v;

        // Calendar.DAY_OF_WEEK: 1=Sun,2=Mon,3=Tue,4=Wed,5=Thu,6=Fri,7=Sat
        int[] days = {2, 3, 4, 5, 6, 7, 1};
        ProgressBar[] bars = {
                binding.barMon, binding.barTue, binding.barWed,
                binding.barThu, binding.barFri, binding.barSat, binding.barSun
        };
        TextView[] labels = {
                binding.tvMon, binding.tvTue, binding.tvWed,
                binding.tvThu, binding.tvFri, binding.tvSat, binding.tvSun
        };

        for (int i = 0; i < 7; i++) {
            double val = weekSales.getOrDefault(days[i], 0.0);
            bars[i].setProgress((int) ((val / max) * 100));
            labels[i].setText(String.format("₱%.0f", val));
        }
    }

    // ── Menu Listener ─────────────────────────────────────────────────────────

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
                    binding.emptyMenu.setVisibility(menuItems.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void onOrderAction(Order order) {
        String next = nextStatus(order.getStatus());
        if (next == null) return;
        FirebaseHelper.getDb().collection("orders")
                .document(order.getOrderId())
                .update("status", next)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Order → " + next, Toast.LENGTH_SHORT).show();
                    // Notify customer
                    NotificationHelper.notifyStatusChange(
                            order.getOrderId(), next, order.getProductName());
                })
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

    private void onMenuItemDelete(Product product) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Remove \"" + product.getName() + "\" from your menu?")
                .setPositiveButton("Delete", (d, w) ->
                        FirebaseHelper.getDb().collection("products")
                                .document(product.getProductId()).delete()
                                .addOnSuccessListener(v ->
                                        Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(ex ->
                                        Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null)
                .show();
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
        if (menuListener   != null) menuListener.remove();
    }
}