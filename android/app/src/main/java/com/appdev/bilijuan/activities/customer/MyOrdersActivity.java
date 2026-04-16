package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.adapters.CustomerOrdersAdapter;
import com.appdev.bilijuan.databinding.ActivityMyOrdersBinding;
import com.appdev.bilijuan.models.CartItem;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.utils.CustomerNavHelper;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
        
        listenOrders();
        checkForPendingReview();
    }

    private void setupRecyclerView() {
        adapter = new CustomerOrdersAdapter(filtered, new CustomerOrdersAdapter.OnOrderClickListener() {
            @Override
            public void onTrack(Order order) {
                onTrackOrder(order);
            }

            @Override
            public void onReorder(Order order) {
                onReorderOrder(order);
            }

            @Override
            public void onCancel(Order order) {
                onCancelOrder(order);
            }

            @Override
            public void onClick(Order order) {
                showOrderDetails(order);
            }
        });
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(adapter);
    }

    private void showOrderDetails(Order order) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_order_details, null);
        dialog.setContentView(view);

        TextView tvOrderId = view.findViewById(R.id.tvOrderId);
        TextView tvOrderDate = view.findViewById(R.id.tvOrderDate);
        TextView tvCustomerName = view.findViewById(R.id.tvCustomerName);
        TextView tvCustomerPhone = view.findViewById(R.id.tvCustomerPhone);
        TextView tvCustomerAddress = view.findViewById(R.id.tvCustomerAddress);
        LinearLayout layoutOrderItems = view.findViewById(R.id.layoutOrderItems);
        TextView tvSubtotal = view.findViewById(R.id.tvSubtotal);
        TextView tvDeliveryFee = view.findViewById(R.id.tvDeliveryFee);
        TextView tvTotalAmount = view.findViewById(R.id.tvTotalAmount);
        View btnClose = view.findViewById(R.id.btnClose);

        // Progress components
        View layoutProgress = view.findViewById(R.id.layoutProgress);
        TextView tvSecondaryStatus = view.findViewById(R.id.tvSecondaryStatus);
        TextView tvEta = view.findViewById(R.id.tvEta);
        LinearProgressIndicator indicatorProgress = view.findViewById(R.id.indicatorProgress);

        String shortId = order.getOrderId().length() > 8 ? order.getOrderId().substring(0, 8).toUpperCase() : order.getOrderId();
        tvOrderId.setText("Order #" + shortId);

        if (order.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
            tvOrderDate.setText("Placed on " + sdf.format(order.getCreatedAt()));
        }

        // Setup Progress logic
        if (order.isActive()) {
            layoutProgress.setVisibility(View.VISIBLE);
            String eta = "40-45 mins";
            int progress = 10;
            String msg = "Waiting for store...";

            switch (order.getStatus()) {
                case Order.STATUS_CONFIRMED:
                    eta = "30-40 mins"; progress = 25; msg = "Order accepted!"; break;
                case Order.STATUS_PREPARING:
                    eta = "20-30 mins"; progress = 50; msg = "Cooking your meal!"; break;
                case Order.STATUS_ON_THE_WAY:
                    eta = "10-15 mins"; progress = 75; msg = "Rider is on the way!"; break;
            }
            tvEta.setText("ETA: " + eta);
            tvSecondaryStatus.setText(msg);
            indicatorProgress.setProgress(progress);
        } else {
            layoutProgress.setVisibility(View.GONE);
        }

        tvCustomerName.setText(order.getCustomerName());
        tvCustomerPhone.setText(order.getCustomerPhone());
        tvCustomerAddress.setText(order.getCustomerAddress());

        layoutOrderItems.removeAllViews();
        double subtotal = 0;

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (CartItem item : order.getItems()) {
                View itemView = getLayoutInflater().inflate(R.layout.item_order_detail, layoutOrderItems, false);
                ((TextView) itemView.findViewById(R.id.tvItemName)).setText(item.getProductName());
                ((TextView) itemView.findViewById(R.id.tvItemQty)).setText("x" + item.getQuantity());
                ((TextView) itemView.findViewById(R.id.tvItemPrice)).setText(String.format("₱%.0f", item.getPrice() * item.getQuantity()));
                layoutOrderItems.addView(itemView);
                subtotal += item.getPrice() * item.getQuantity();
            }
        } else {
            View itemView = getLayoutInflater().inflate(R.layout.item_order_detail, layoutOrderItems, false);
            ((TextView) itemView.findViewById(R.id.tvItemName)).setText(order.getProductName());
            ((TextView) itemView.findViewById(R.id.tvItemQty)).setText("x" + order.getQuantity());
            ((TextView) itemView.findViewById(R.id.tvItemPrice)).setText(String.format("₱%.0f", order.getProductPrice() * order.getQuantity()));
            layoutOrderItems.addView(itemView);
            subtotal = order.getProductPrice() * order.getQuantity();
        }

        tvSubtotal.setText(String.format("₱%.0f", subtotal));
        tvDeliveryFee.setText(String.format("₱%.0f", order.getDeliveryFee()));
        tvTotalAmount.setText(String.format("₱%.0f", order.getTotalAmount()));

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
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
        CustomerNavHelper.setup(this, binding.customerNav.getRoot(), CustomerNavHelper.Tab.ORDERS);
    }

    private void setupTabs() {
        binding.swipeRefresh.setColorSchemeColors(0xFF27AE60);
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

        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_cancel_order, null);

        view.findViewById(R.id.btnConfirmCancel).setOnClickListener(v -> {
            dialog.dismiss();
            FirebaseHelper.getDb().collection("orders")
                    .document(order.getOrderId())
                    .update("status", Order.STATUS_CANCELLED)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "Order cancelled successfully.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to cancel order.", Toast.LENGTH_SHORT).show());
        });

        view.findViewById(R.id.btnKeepOrder).setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(view);
        dialog.show();
    }

    private void onReorderOrder(Order order) {
        Intent intent = new Intent(this, OrderSummaryActivity.class);
        intent.putExtra("productId", order.getProductId());
        intent.putExtra("pinnedAddress", order.getCustomerAddress());
        intent.putExtra("pinnedLat", order.getCustomerLat());
        intent.putExtra("pinnedLng", order.getCustomerLng());
        intent.putExtra("quantity", order.getQuantity());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}
