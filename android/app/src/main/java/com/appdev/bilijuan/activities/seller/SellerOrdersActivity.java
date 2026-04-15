package com.appdev.bilijuan.activities.seller;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.adapters.SellerOrdersAdapter;
import com.appdev.bilijuan.databinding.ActivitySellerOrdersBinding;
import com.appdev.bilijuan.models.CartItem;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.utils.DeliveryUtils;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.NotificationHelper;
import com.appdev.bilijuan.utils.StoreNavHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SellerOrdersActivity extends AppCompatActivity {

    private ActivitySellerOrdersBinding binding;
    private SellerOrdersAdapter adapter;
    private final List<Order> allOrders      = new ArrayList<>();
    private final List<Order> filteredOrders = new ArrayList<>();
    private ListenerRegistration listener;
    private String sellerId;
    private double sellerLat = 0;
    private double sellerLng = 0;
    private String activeFilter = "Active";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySellerOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sellerId = FirebaseHelper.getCurrentUid();
        if (sellerId == null) { finish(); return; }

        setupRecyclerView();
        setupStoreNav();
        setupFilterChips();
        binding.btnBack.setOnClickListener(v -> finish());
        loadSellerLocation();
    }

    private void setupRecyclerView() {
        adapter = new SellerOrdersAdapter(filteredOrders,
                new SellerOrdersAdapter.ActionListener() {
                    @Override
                    public void onAdvance(Order order) {
                        onAdvanceStatus(order);
                    }

                    @Override
                    public void onViewMap(Order order) {
                        Intent intent = new Intent(
                                SellerOrdersActivity.this,
                                SellerDeliveryMapActivity.class);
                        intent.putExtra("orderId", order.getOrderId());
                        startActivity(intent);
                    }

                    @Override
                    public void onViewDetails(Order order) {
                        showOrderDetailsSheet(order);
                    }
                });

        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(adapter);
    }

    private void setupStoreNav() {
        StoreNavHelper.setup(this, binding.storeNav.getRoot(), StoreNavHelper.Tab.ORDERS);
        binding.storeNav.fabPost.setOnClickListener(v -> showPostBottomSheet());
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
            startActivity(new Intent(this, SellerAccountActivity.class).putExtra("openMenu", true));
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
                    sellerLat  = lat != null ? lat : 0;
                    sellerLng  = lng != null ? lng : 0;

                    if (sellerLat == 0 || sellerLng == 0) {
                        showNoLocationWarning();
                    }
                    listenOrders();
                });
    }

    private void showNoLocationWarning() {
        binding.emptyOrders.setVisibility(View.VISIBLE);
        binding.emptyOrders.setText("⚠️ Your shop location is not set.\nGo to Account → Edit Profile to pin your store location.\nDelivery distances may be inaccurate until this is fixed.");
        binding.emptyOrders.setTextColor(getColor(R.color.error));
        binding.emptyOrders.setBackgroundColor(0xFFFFF9E6); 
        binding.emptyOrders.setPadding(32, 24, 32, 24);
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
                double dA = safeDistance(sellerLat, sellerLng, a.getCustomerLat(), a.getCustomerLng());
                double dB = safeDistance(sellerLat, sellerLng, b.getCustomerLat(), b.getCustomerLng());
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
        
        boolean hasOrders = !filteredOrders.isEmpty();
        boolean hasWarning = sellerLat == 0 || sellerLng == 0;
        if (hasOrders) {
            if (!hasWarning) binding.emptyOrders.setVisibility(View.GONE);
        } else {
            if (!hasWarning) {
                binding.emptyOrders.setVisibility(View.VISIBLE);
                binding.emptyOrders.setText("No orders yet.");
                binding.emptyOrders.setTextColor(getColor(R.color.text_secondary));
                binding.emptyOrders.setBackgroundColor(0x00000000);
            }
        }
    }

    private double safeDistance(double lat1, double lng1, double lat2, double lng2) {
        if (lat1 == 0 || lng1 == 0 || lat2 == 0 || lng2 == 0) return 9999;
        double dist = DeliveryUtils.haversineKm(lat1, lng1, lat2, lng2);
        return dist > 100 ? 9999 : dist;
    }

    private void onAdvanceStatus(Order order) {
        String next = nextStatus(order.getStatus());
        if (next == null) return;
        Map<String, Object> update = new HashMap<>();
        update.put("status", next);
        if (Order.STATUS_DELIVERED.equals(next)) update.put("active", false);
        FirebaseHelper.getDb().collection("orders").document(order.getOrderId()).update(update)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Order → " + next, Toast.LENGTH_SHORT).show();
                    NotificationHelper.notifyStatusChange(order.getOrderId(), next, order.getProductName(), order.getCustomerId());
                });
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
            items = new ArrayList<>();
            items.add(new CartItem(order.getProductId(), order.getProductName(), order.getProductPrice(), order.getQuantity(), order.getSellerId(), order.getSellerName(), order.getProductImageBase64()));
        }
        for (CartItem item : items) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_order_summary_product, layoutItems, false);
            ((TextView) itemView.findViewById(R.id.tvProductName)).setText(item.getProductName());
            ((TextView) itemView.findViewById(R.id.tvProductQty)).setText("x" + item.getQuantity());
            ((TextView) itemView.findViewById(R.id.tvProductPrice)).setText(String.format(Locale.getDefault(), "₱%.0f", item.getPrice() * item.getQuantity()));
            layoutItems.addView(itemView);
            subtotal += (item.getPrice() * item.getQuantity());
        }
        tvSubtotal.setText(String.format(Locale.getDefault(), "₱%.0f", subtotal));
        tvDeliveryFee.setText(String.format(Locale.getDefault(), "₱%.0f", order.getDeliveryFee()));
        tvTotalAmount.setText(String.format(Locale.getDefault(), "₱%.0f", order.getTotalAmount()));
        v.findViewById(R.id.btnClose).setOnClickListener(view -> sheet.dismiss());
        sheet.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}
