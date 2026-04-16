package com.appdev.bilijuan.activities.seller;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.adapters.SellerOrdersAdapter;
import com.appdev.bilijuan.adapters.SellerMenuAdapter;
import com.appdev.bilijuan.databinding.ActivitySellerBinding;
import com.appdev.bilijuan.models.CartItem;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.models.Product;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.LocationHelper;
import com.appdev.bilijuan.utils.NotificationHelper;
import com.appdev.bilijuan.utils.NotificationUIHelper;
import com.appdev.bilijuan.utils.StoreNavHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SellerDashboardActivity extends AppCompatActivity {

    private ActivitySellerBinding binding;
    private SellerMenuAdapter menuAdapter;
    private SellerOrdersAdapter ordersAdapter;

    private final List<Order>   allOrders    = new ArrayList<>();
    private final List<Order>   activeOrders = new ArrayList<>();
    private final List<Product> menuItems    = new ArrayList<>();

    private ListenerRegistration ordersListener;
    private ListenerRegistration menuListener;

    private String sellerId;
    private double sellerLat, sellerLng;
    private String activeTab = "overview";

    private MediaPlayer alertPlayer;
    private final Handler alertHandler = new Handler(Looper.getMainLooper());
    private Runnable alertRunnable;
    private int previousOrderCount = -1;

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

        binding.btnNotification.setOnClickListener(
                v -> NotificationUIHelper.showNotificationSheet(this));

        binding.btnGoToOrders.setOnClickListener(v -> switchTab("orders"));

        loadSellerProfile();
    }

    private void autoSaveSellerLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LocationHelper.LOCATION_PERMISSION_REQUEST);
            return;
        }

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        try {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(this, location -> {
                if (location != null) saveSellerGpsToFirestore(location.getLatitude(), location.getLongitude());
                else client.getLastLocation().addOnSuccessListener(this, last -> {
                    if (last != null) saveSellerGpsToFirestore(last.getLatitude(), last.getLongitude());
                    else Toast.makeText(this, "Could not get location. Enable GPS and try again.", Toast.LENGTH_SHORT).show();
                });
            });
        } catch (SecurityException ignored) {}
    }

    private void saveSellerGpsToFirestore(double lat, double lng) {
        sellerLat = lat; sellerLng = lng;
        Map<String, Object> update = new HashMap<>();
        update.put("latitude",  lat);
        update.put("longitude", lng);
        FirebaseHelper.getDb().collection("users").document(sellerId).update(update)
                .addOnSuccessListener(v -> Toast.makeText(this, "Store location updated ✓", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LocationHelper.LOCATION_PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) autoSaveSellerLocation();
    }

    private void setupStoreNav() {
        StoreNavHelper.setup(this, binding.storeNav.getRoot(), StoreNavHelper.Tab.HOME);
        binding.storeNav.fabPost.setOnClickListener(v -> showPostBottomSheet());
    }

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

    private void setupRecyclerViews() {
        menuAdapter = new SellerMenuAdapter(menuItems, this::onMenuItemToggle, this::onMenuItemEdit, this::onMenuItemDelete);
        binding.rvMenu.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMenu.setAdapter(menuAdapter);

        ordersAdapter = new SellerOrdersAdapter(activeOrders, new SellerOrdersAdapter.ActionListener() {
            @Override
            public void onAdvance(Order order) { showAdvanceStatusModal(order); }
            @Override
            public void onReject(Order order) { showRejectOrderSheet(order); }
            @Override
            public void onViewMap(Order order) {
                Intent intent = new Intent(SellerDashboardActivity.this, SellerDeliveryMapActivity.class);
                intent.putExtra("orderId", order.getOrderId());
                startActivity(intent);
            }
            @Override
            public void onViewDetails(Order order) {
                showOrderDetailsSheet(order);
            }
        });
        binding.rvActiveOrders2.setLayoutManager(new LinearLayoutManager(this));
        binding.rvActiveOrders2.setAdapter(ordersAdapter);

        binding.btnAddItem.setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));
    }

    private void loadSellerProfile() {
        FirebaseHelper.getDb().collection("users").document(sellerId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    User seller = doc.toObject(User.class);
                    if (seller != null) {
                        sellerLat = seller.getLatitude();
                        sellerLng = seller.getLongitude();
                        binding.tvSellerName.setText(seller.getName());
                        listenOrders();
                        listenMenu();
                    }
                });
    }

    private void listenOrders() {
        ordersListener = FirebaseHelper.getDb().collection("orders")
                .whereEqualTo("sellerId", sellerId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    allOrders.clear();
                    activeOrders.clear();
                    double todaySales = 0;
                    int todayCount = 0;
                    Map<Integer, Double> weekSales = new HashMap<>();
                    Calendar today = Calendar.getInstance();

                    for (QueryDocumentSnapshot doc : snap) {
                        Order o = doc.toObject(Order.class);
                        o.setOrderId(doc.getId());
                        allOrders.add(o);

                        if (o.isActive()) {
                            activeOrders.add(o);
                        }

                        if (Order.STATUS_DELIVERED.equals(o.getStatus())) {
                            if (o.getCreatedAt() != null) {
                                Calendar oCal = Calendar.getInstance();
                                oCal.setTime(o.getCreatedAt());
                                if (oCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) && oCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                                    todaySales += o.getTotalAmount();
                                    todayCount++;
                                }
                                if (oCal.get(Calendar.WEEK_OF_YEAR) == today.get(Calendar.WEEK_OF_YEAR) && oCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                                    int dow = oCal.get(Calendar.DAY_OF_WEEK);
                                    weekSales.merge(dow, o.getTotalAmount(), Double::sum);
                                }
                            }
                        }
                    }

                    // Sort Active Orders: Pending first, then newest
                    Collections.sort(activeOrders, (a, b) -> {
                        if (a.getStatus().equals(Order.STATUS_PENDING) && !b.getStatus().equals(Order.STATUS_PENDING)) return -1;
                        if (!a.getStatus().equals(Order.STATUS_PENDING) && b.getStatus().equals(Order.STATUS_PENDING)) return 1;
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });

                    int pendingCount = 0;
                    for (Order o : activeOrders) {
                        if (Order.STATUS_PENDING.equals(o.getStatus())) pendingCount++;
                    }

                    if (previousOrderCount >= 0 && pendingCount > previousOrderCount) {
                        startNewOrderAlert();
                        binding.notifDot.setVisibility(View.VISIBLE);
                    }
                    previousOrderCount = pendingCount;
                    if (pendingCount == 0) stopNewOrderAlert();

                    int activeCount = activeOrders.size();
                    binding.tvActionMessage.setText(activeCount > 0 ? activeCount + " active orders require your attention" : "You have no active orders");
                    binding.tvActionSubMessage.setText(activeCount > 0 ? "Tap here to manage and track orders" : "Tap here to view order history");

                    binding.tvTodayEarnings.setText(String.format("₱%.0f", todaySales));
                    binding.tvTodayOrders.setText(String.valueOf(todayCount));
                    
                    ordersAdapter.notifyDataSetChanged();
                    binding.emptyOrders.setVisibility(activeOrders.isEmpty() ? View.VISIBLE : View.GONE);
                    updateWeeklyChart(weekSales);
                });
    }

    private void listenMenu() {
        menuListener = FirebaseHelper.getDb().collection("products").whereEqualTo("sellerId", sellerId)
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

    private void showAdvanceStatusModal(Order order) {
        String next = nextStatus(order.getStatus());
        if (next == null) return;
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View v = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_advance_status, null);
        sheet.setContentView(v);

        TextView tvTitle = v.findViewById(R.id.tvAdvanceTitle);
        TextView tvMsg = v.findViewById(R.id.tvAdvanceMessage);
        
        if (tvTitle != null) tvTitle.setText("Update to " + next + "?");
        if (tvMsg != null) tvMsg.setText("Are you sure you want to move this order to " + next + "? The customer will be notified.");

        v.findViewById(R.id.btnConfirmAdvance).setOnClickListener(view -> {
            sheet.dismiss();
            updateOrderStatus(order, next);
        });
        v.findViewById(R.id.btnCancelAdvance).setOnClickListener(view -> sheet.dismiss());
        sheet.show();
    }

    private void updateOrderStatus(Order order, String next) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", next);
        
        FirebaseHelper.getDb().collection("orders").document(order.getOrderId()).update(update)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Order updated", Toast.LENGTH_SHORT).show();
                    NotificationHelper.notifyStatusChange(order.getOrderId(), next, order.getProductName(), order.getCustomerId());
                });
    }

    private void showRejectOrderSheet(Order order) {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetStyle);
        View v = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_reject_order, null);
        sheet.setContentView(v);

        EditText etNote = v.findViewById(R.id.etRejectionNote);
        View btnConfirm = v.findViewById(R.id.btnConfirmReject);
        View btnCancel = v.findViewById(R.id.btnCancelReject);

        btnConfirm.setOnClickListener(view -> {
            String note = etNote.getText().toString().trim();
            if (TextUtils.isEmpty(note)) {
                etNote.setError("Please provide a reason");
                return;
            }
            sheet.dismiss();
            rejectOrder(order, note);
        });

        btnCancel.setOnClickListener(view -> sheet.dismiss());
        sheet.show();
    }

    private void rejectOrder(Order order, String note) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", Order.STATUS_REJECTED);
        update.put("rejectionNote", note);
        update.put("active", false);

        FirebaseHelper.getDb().collection("orders").document(order.getOrderId())
                .update(update)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Order rejected", Toast.LENGTH_SHORT).show();
                    NotificationHelper.notifyStatusChange(order.getOrderId(), Order.STATUS_REJECTED, order.getProductName(), order.getCustomerId());
                });
    }

    private String nextStatus(String current) {
        switch (current) {
            case Order.STATUS_PENDING:    return Order.STATUS_CONFIRMED;
            case Order.STATUS_CONFIRMED:  return Order.STATUS_PREPARING;
            case Order.STATUS_PREPARING:  return Order.STATUS_ON_THE_WAY;
            default: return null;
        }
    }

    private void showOrderDetailsSheet(Order order) {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View v = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_order_details, null);
        sheet.setContentView(v);

        TextView tvOrderId = v.findViewById(R.id.tvOrderId);
        TextView tvOrderDate = v.findViewById(R.id.tvOrderDate);
        TextView tvCustomerName = v.findViewById(R.id.tvCustomerName);
        TextView tvCustomerPhone = v.findViewById(R.id.tvCustomerPhone);
        TextView tvCustomerAddress = v.findViewById(R.id.tvCustomerAddress);
        LinearLayout layoutItems = v.findViewById(R.id.layoutOrderItems);
        TextView tvSubtotal = v.findViewById(R.id.tvSubtotal);
        TextView tvDeliveryFee = v.findViewById(R.id.tvDeliveryFee);
        TextView tvTotalAmount = v.findViewById(R.id.tvTotalAmount);

        String shortId = order.getOrderId().length() > 6 ? order.getOrderId().substring(0, 6).toUpperCase() : order.getOrderId();
        tvOrderId.setText("Order #" + shortId);
        
        if (order.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
            tvOrderDate.setText("Placed on " + sdf.format(order.getCreatedAt()));
        }

        tvCustomerName.setText(order.getCustomerName());
        tvCustomerPhone.setText(order.getCustomerPhone());
        tvCustomerAddress.setText(order.getCustomerAddress());

        double subtotal = 0;
        layoutItems.removeAllViews();
        List<CartItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            // Legacy support
            items = new ArrayList<>();
            items.add(new CartItem(order.getProductId(), order.getProductName(), order.getProductPrice(), order.getQuantity(), order.getSellerId(), order.getSellerName(), order.getProductImageBase64()));
        }

        for (CartItem item : items) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_order_summary_product, layoutItems, false);
            ((TextView)itemView.findViewById(R.id.tvProductName)).setText(item.getProductName());
            ((TextView)itemView.findViewById(R.id.tvProductQty)).setText("x" + item.getQuantity());
            ((TextView)itemView.findViewById(R.id.tvProductPrice)).setText(String.format("₱%.0f", item.getPrice() * item.getQuantity()));
            layoutItems.addView(itemView);
            subtotal += (item.getPrice() * item.getQuantity());
        }

        tvSubtotal.setText(String.format("₱%.0f", subtotal));
        tvDeliveryFee.setText(String.format("₱%.0f", order.getDeliveryFee()));
        tvTotalAmount.setText(String.format("₱%.0f", order.getTotalAmount()));

        v.findViewById(R.id.btnClose).setOnClickListener(view -> sheet.dismiss());
        sheet.show();
    }

    private void onMenuItemToggle(Product product, boolean available) {
        FirebaseHelper.getDb().collection("products").document(product.getProductId()).update("available", available);
    }

    private void onMenuItemEdit(Product product) {
        startActivity(new Intent(this, AddProductActivity.class).putExtra("productId", product.getProductId()));
    }

    private void onMenuItemDelete(Product product) {
        FirebaseHelper.getDb().collection("products").document(product.getProductId()).delete();
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
            switchTab("products");
        });
        sheet.show();
    }

    private void startNewOrderAlert() {
        stopNewOrderAlert();
        Uri alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        alertPlayer = new MediaPlayer();
        try {
            alertPlayer.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
            alertPlayer.setDataSource(this, alertUri);
            alertPlayer.prepare();
        } catch (Exception ex) { return; }
        alertRunnable = new Runnable() {
            @Override
            public void run() {
                if (alertPlayer != null && !alertPlayer.isPlaying()) alertPlayer.start();
                alertHandler.postDelayed(this, 3000);
            }
        };
        alertHandler.post(alertRunnable);
    }

    private void stopNewOrderAlert() {
        if (alertRunnable != null) alertHandler.removeCallbacks(alertRunnable);
        if (alertPlayer != null) { alertPlayer.release(); alertPlayer = null; }
    }

    private void updateWeeklyChart(Map<Integer, Double> weekSales) {
        double max = 1;
        for (double v : weekSales.values()) if (v > max) max = v;
        int[] days = {2, 3, 4, 5, 6, 7, 1};
        ProgressBar[] bars = {binding.barMon, binding.barTue, binding.barWed, binding.barThu, binding.barFri, binding.barSat, binding.barSun};
        TextView[] labels = {binding.tvMon, binding.tvTue, binding.tvWed, binding.tvThu, binding.tvFri, binding.tvSat, binding.tvSun};
        for (int i = 0; i < 7; i++) {
            double val = weekSales.getOrDefault(days[i], 0.0);
            bars[i].setProgress((int) ((val / max) * 100));
            labels[i].setText(String.format("₱%.0f", val));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopNewOrderAlert();
        if (ordersListener != null) ordersListener.remove();
        if (menuListener != null) menuListener.remove();
    }
}