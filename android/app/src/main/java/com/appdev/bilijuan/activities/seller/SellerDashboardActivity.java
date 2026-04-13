package com.appdev.bilijuan.activities.seller;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.activities.customer.OrderTrackingActivity;
import com.appdev.bilijuan.adapters.ActiveOrderCardAdapter;
import com.appdev.bilijuan.adapters.SellerMenuAdapter;
import com.appdev.bilijuan.databinding.ActivitySellerBinding;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.models.Product;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.DeliveryUtils;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.NotificationHelper;
import com.appdev.bilijuan.utils.StoreNavHelper;
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

    private final List<Order> allOrders    = new ArrayList<>();
    private final List<Order> recentOrders = new ArrayList<>();
    private final List<Product> menuItems  = new ArrayList<>();

    private ListenerRegistration ordersListener;
    private ListenerRegistration menuListener;

    private String sellerId;
    private double sellerLat, sellerLng;
    private boolean isOpen = true;
    private String activeTab = "overview";

    // Sound alert for new orders
    private MediaPlayer alertPlayer;
    private Handler alertHandler = new Handler(Looper.getMainLooper());
    private Runnable alertRunnable;
    private int previousOrderCount = -1; // -1 = first load

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySellerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sellerId = FirebaseHelper.getCurrentUid();
        if (sellerId == null) { finish(); return; }

        setupTopTabs();
        setupStoreNav();
        setupRecyclerViews();
        setupOpenCloseToggle();
        loadSellerProfile();
    }

    // ── Store Nav ─────────────────────────────────────────────────────────────

    private void setupStoreNav() {
        // FIXED: Use .getRoot() to pass the View, not the binding object
        StoreNavHelper.setup(this, binding.storeNav.getRoot(), StoreNavHelper.Tab.HOME);
        
        // Override FAB to show bottom sheet
        binding.storeNav.fabPost.setOnClickListener(v ->
                showPostBottomSheet());
    }

    // ── Open/Close Toggle ─────────────────────────────────────────────────────

    private void setupOpenCloseToggle() {
        binding.btnToggleOpen.setOnClickListener(v -> {
            isOpen = !isOpen;
            updateOpenCloseUI();
            FirebaseHelper.getDb().collection("users").document(sellerId)
                    .update("isOpen", isOpen)
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to update status",
                                    Toast.LENGTH_SHORT).show());
        });
    }

    private void updateOpenCloseUI() {
        binding.tvOpenStatus.setText(isOpen ? "Open" : "Closed");
        binding.tvOpenStatus.setTextColor(ContextCompat.getColor(this, isOpen ? R.color.primary : R.color.error));
        binding.dotOpenStatus.setBackgroundResource(
                isOpen ? R.drawable.bg_notification_dot : R.drawable.bg_dot_inactive);
    }

    // ── Top Tabs ──────────────────────────────────────────────────────────────

    private void setupTopTabs() {
        binding.tabOverview.setOnClickListener(v -> switchTab("overview"));
        binding.tabProducts.setOnClickListener(v -> switchTab("products"));
        binding.tabOrders.setOnClickListener(v   -> switchTab("orders"));
        switchTab("overview");
    }

    private void switchTab(String tab) {
        activeTab = tab;
        binding.tabOverview.setBackgroundResource(android.R.color.transparent);
        binding.tabProducts.setBackgroundResource(android.R.color.transparent);
        binding.tabOrders.setBackgroundResource(android.R.color.transparent);
        binding.tabOverview.setTextColor(0xCCFFFFFF);
        binding.tabProducts.setTextColor(0xCCFFFFFF);
        binding.tabOrders.setTextColor(0xCCFFFFFF);

        binding.sectionOverview.setVisibility(View.GONE);
        binding.sectionProducts.setVisibility(View.GONE);
        binding.sectionOrders.setVisibility(View.GONE);

        switch (tab) {
            case "overview":
                binding.tabOverview.setBackgroundResource(R.drawable.bg_tab_active_white);
                binding.tabOverview.setTextColor(ContextCompat.getColor(this, R.color.primary));
                binding.sectionOverview.setVisibility(View.VISIBLE);
                break;
            case "products":
                binding.tabProducts.setBackgroundResource(R.drawable.bg_tab_active_white);
                binding.tabProducts.setTextColor(ContextCompat.getColor(this, R.color.primary));
                binding.sectionProducts.setVisibility(View.VISIBLE);
                break;
            case "orders":
                binding.tabOrders.setBackgroundResource(R.drawable.bg_tab_active_white);
                binding.tabOrders.setTextColor(ContextCompat.getColor(this, R.color.primary));
                binding.sectionOrders.setVisibility(View.VISIBLE);
                break;
        }
    }

    // ── RecyclerViews ─────────────────────────────────────────────────────────

    private void setupRecyclerViews() {
        ActiveOrderCardAdapter.ActionListener orderAction = new ActiveOrderCardAdapter.ActionListener() {
            @Override
            public void onAction(Order order) {
                onOrderAction(order);
            }

            @Override
            public void onViewMap(Order order) {
                Intent intent = new Intent(SellerDashboardActivity.this, OrderTrackingActivity.class);
                intent.putExtra("orderId", order.getOrderId());
                startActivity(intent);
            }
        };

        recentOrdersAdapter = new ActiveOrderCardAdapter(recentOrders, orderAction);
        binding.rvActiveOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvActiveOrders.setAdapter(recentOrdersAdapter);
        binding.rvActiveOrders.setNestedScrollingEnabled(false);

        ordersTabAdapter = new ActiveOrderCardAdapter(allOrders, orderAction);
        binding.rvActiveOrders2.setLayoutManager(new LinearLayoutManager(this));
        binding.rvActiveOrders2.setAdapter(ordersTabAdapter);

        menuAdapter = new SellerMenuAdapter(menuItems,
                this::onMenuItemToggle, this::onMenuItemEdit, this::onMenuItemDelete);
        binding.rvMenu.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMenu.setAdapter(menuAdapter);
        binding.rvMenu.setNestedScrollingEnabled(false);

        binding.btnAddItem.setOnClickListener(v ->
                startActivity(new Intent(this, AddProductActivity.class)));
        binding.tvSeeAllOrders.setOnClickListener(v ->
                startActivity(new Intent(this, SellerOrdersActivity.class)));
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

                    // Load open/closed state
                    Boolean open = doc.getBoolean("isOpen");
                    isOpen = open == null || open; // default open
                    updateOpenCloseUI();

                    listenOrders();
                    listenMenu();
                });
    }

    // ── Orders Listener + New Order Sound ────────────────────────────────────

    private void listenOrders() {
        ordersListener = FirebaseHelper.getDb()
                .collection("orders")
                .whereEqualTo("sellerId", sellerId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    allOrders.clear();
                    double totalSales = 0, todaySales = 0;
                    int todayCount = 0;
                    Map<Integer, Double> weekSales = new HashMap<>();
                    Calendar today = Calendar.getInstance();

                    for (QueryDocumentSnapshot doc : snap) {
                        Order o = doc.toObject(Order.class);
                        o.setOrderId(doc.getId());
                        allOrders.add(o);

                        if (Order.STATUS_DELIVERED.equals(o.getStatus())) {
                            totalSales += o.getTotalAmount();
                            if (o.getCreatedAt() != null) {
                                Calendar oCal = Calendar.getInstance();
                                oCal.setTime(o.getCreatedAt());
                                if (oCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                                        && oCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                                    todaySales += o.getTotalAmount();
                                    todayCount++;
                                }
                                if (oCal.get(Calendar.WEEK_OF_YEAR) == today.get(Calendar.WEEK_OF_YEAR)
                                        && oCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                                    int dow = oCal.get(Calendar.DAY_OF_WEEK);
                                    weekSales.merge(dow, o.getTotalAmount(), Double::sum);
                                }
                            }
                        }
                    }

                    // Check for NEW orders — trigger sound
                    int pendingCount = 0;
                    for (Order o : allOrders) {
                        if (Order.STATUS_PENDING.equals(o.getStatus())) pendingCount++;
                    }
                    if (previousOrderCount >= 0 && pendingCount > previousOrderCount) {
                        startNewOrderAlert();
                        binding.notifDot.setVisibility(View.VISIBLE);
                    }
                    previousOrderCount = pendingCount;
                    if (pendingCount == 0) stopNewOrderAlert();

                    // Sort active by Haversine (nearest first)
                    List<Order> activeOrders = new ArrayList<>();
                    for (Order o : allOrders) if (o.isActive()) activeOrders.add(o);
                    Collections.sort(activeOrders, (a, b) -> {
                        double dA = DeliveryUtils.haversineKm(sellerLat, sellerLng,
                                a.getCustomerLat(), a.getCustomerLng());
                        double dB = DeliveryUtils.haversineKm(sellerLat, sellerLng,
                                b.getCustomerLat(), b.getCustomerLng());
                        return Double.compare(dA, dB);
                    });

                    recentOrders.clear();
                    int cap = Math.min(5, activeOrders.size());
                    recentOrders.addAll(activeOrders.subList(0, cap));

                    Collections.sort(allOrders, (a, b) -> {
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });

                    recentOrdersAdapter.notifyDataSetChanged();
                    ordersTabAdapter.notifyDataSetChanged();

                    // Update UI
                    binding.tvTotalOrders.setText(String.valueOf(allOrders.size()));
                    binding.tvTotalSales.setText(String.format("₱%.0f", totalSales));
                    binding.tvOrdersToday.setText("+" + todayCount + " today");
                    binding.tvTodayEarnings.setText(String.format("₱%.0f", todaySales));
                    binding.tvTodayOrders.setText(todayCount + " orders");
                    binding.emptyOrders.setVisibility(recentOrders.isEmpty() ? View.VISIBLE : View.GONE);

                    updateWeeklyChart(weekSales);
                });
    }

    // ── New Order Sound Alert ─────────────────────────────────────────────────

    private void startNewOrderAlert() {
        stopNewOrderAlert(); // prevent duplicate
        Uri alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        alertPlayer = new MediaPlayer();
        try {
            alertPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build());
            alertPlayer.setDataSource(this, alertUri);
            alertPlayer.prepare();
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        // Repeat sound every 3 seconds until store owner confirms
        alertRunnable = new Runnable() {
            @Override
            public void run() {
                if (alertPlayer != null && !alertPlayer.isPlaying()) {
                    alertPlayer.start();
                }
                alertHandler.postDelayed(this, 3000);
            }
        };
        alertHandler.post(alertRunnable);

        // Show toast
        Toast.makeText(this, "🔔 New order received!", Toast.LENGTH_SHORT).show();
    }

    private void stopNewOrderAlert() {
        if (alertRunnable != null) {
            alertHandler.removeCallbacks(alertRunnable);
            alertRunnable = null;
        }
        if (alertPlayer != null) {
            if (alertPlayer.isPlaying()) alertPlayer.stop();
            alertPlayer.release();
            alertPlayer = null;
        }
    }

    // ── Weekly Chart ──────────────────────────────────────────────────────────

    private void updateWeeklyChart(Map<Integer, Double> weekSales) {
        double max = 1;
        for (double v : weekSales.values()) if (v > max) max = v;
        int[] days = {2, 3, 4, 5, 6, 7, 1};
        ProgressBar[] bars = {binding.barMon, binding.barTue, binding.barWed,
                binding.barThu, binding.barFri, binding.barSat, binding.barSun};
        TextView[] labels = {binding.tvMon, binding.tvTue, binding.tvWed,
                binding.tvThu, binding.tvFri, binding.tvSat, binding.tvSun};
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

    // ── Actions ───────────────────────────────────────────────────────────────

    private void onOrderAction(Order order) {
        String next = nextStatus(order.getStatus());
        if (next == null) return;
        // Stop alert when first order is confirmed
        if (Order.STATUS_PENDING.equals(order.getStatus())) {
            stopNewOrderAlert();
            binding.notifDot.setVisibility(View.GONE);
        }
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
                                        Toast.makeText(this, "Item deleted",
                                                Toast.LENGTH_SHORT).show()))
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
        stopNewOrderAlert();
        if (ordersListener != null) ordersListener.remove();
        if (menuListener   != null) menuListener.remove();
    }
}
