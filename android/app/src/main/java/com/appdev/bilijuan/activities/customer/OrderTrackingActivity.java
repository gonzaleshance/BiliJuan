package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.databinding.ActivityOrderTrackingBinding;
import com.appdev.bilijuan.models.CartItem;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.utils.DeliveryUtils;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.ImageHelper;
import com.google.firebase.firestore.ListenerRegistration;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.util.Locale;

public class OrderTrackingActivity extends AppCompatActivity {

    private ActivityOrderTrackingBinding binding;
    private ListenerRegistration orderListener;
    private String orderId;
    private Marker riderMarker, customerMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        binding = ActivityOrderTrackingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderId = getIntent().getStringExtra("orderId");
        if (orderId == null) orderId = getIntent().getStringExtra("order_id");
        if (orderId == null) { finish(); return; }

        binding.btnBack.setOnClickListener(v -> finish());
        setupMap();
        listenOrder();
    }

    private void setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK);
        binding.mapView.setMultiTouchControls(true);
        binding.mapView.getController().setZoom(17.0);
    }

    private void listenOrder() {
        orderListener = FirebaseHelper.getDb()
                .collection("orders")
                .document(orderId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null || !snap.exists()) return;
                    Order order = snap.toObject(Order.class);
                    if (order == null) return;
                    order.setOrderId(snap.getId());

                    updateHeader(order);
                    updateProgressSteps(order.getStatus());
                    updateOrderDetails(order);
                    updateAddress(order);
                    updateContactInfo(order);
                    updateMapVisibility(order);
                });
    }

    private void updateHeader(Order order) {
        binding.tvCurrentStatus.setText(order.getStatus());
        
        // Accurate ETA Calculation
        if (Order.STATUS_DELIVERED.equals(order.getStatus())) {
            binding.tvEta.setText("Delivered");
            binding.ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
            return;
        }

        double lat = (order.getRiderLat() != 0) ? order.getRiderLat() : order.getSellerLat();
        double lng = (order.getRiderLng() != 0) ? order.getRiderLng() : order.getSellerLng();
        
        if (lat != 0 && order.getCustomerLat() != 0) {
            double dist = DeliveryUtils.haversineKm(lat, lng, order.getCustomerLat(), order.getCustomerLng());
            
            int travelTimeMin = (int) Math.ceil(dist * 4); // 4 mins per km (slower for traffic/motorcycle)
            int prepTimeMin = 0;

            // Add prep time based on status
            if (Order.STATUS_PENDING.equals(order.getStatus())) prepTimeMin = 15;
            else if (Order.STATUS_CONFIRMED.equals(order.getStatus())) prepTimeMin = 12;
            else if (Order.STATUS_PREPARING.equals(order.getStatus())) prepTimeMin = 8;
            else prepTimeMin = 2; // Buffer for on the way

            int totalEta = travelTimeMin + prepTimeMin;
            
            if (totalEta < 2 && !Order.STATUS_DELIVERED.equals(order.getStatus())) {
                binding.tvEta.setText("Arriving soon");
            } else {
                binding.tvEta.setText(String.format(Locale.getDefault(), "%d-%d mins", totalEta, totalEta + 5));
            }
        } else {
            binding.tvEta.setText("--");
        }

        if (Order.STATUS_ON_THE_WAY.equals(order.getStatus())) {
            binding.ivStatusIcon.setImageResource(R.drawable.ic_delivery_dining);
        } else {
            binding.ivStatusIcon.setImageResource(R.drawable.ic_restaurant);
        }
    }

    private void updateProgressSteps(String status) {
        // Status flow: Pending -> Confirmed -> Preparing -> Out for delivery -> Delivered
        
        boolean preparingDone = isDone(status, Order.STATUS_PREPARING);
        boolean preparingActive = Order.STATUS_PREPARING.equals(status) || Order.STATUS_CONFIRMED.equals(status);
        boolean pendingActive = Order.STATUS_PENDING.equals(status);

        String firstStepTitle = "Preparing Your Food";
        String firstStepSub = "Waiting...";
        int firstStepIcon = R.drawable.ic_restaurant;

        if (pendingActive) {
            firstStepTitle = "Order Received";
            firstStepSub = "Waiting for seller confirmation...";
            firstStepIcon = R.drawable.ic_notifications;
        } else if (Order.STATUS_CONFIRMED.equals(status)) {
            firstStepTitle = "Order Confirmed";
            firstStepSub = "Seller is checking your order...";
            firstStepIcon = R.drawable.ic_check_circle;
        } else if (Order.STATUS_PREPARING.equals(status)) {
            firstStepTitle = "Preparing Food";
            firstStepSub = "Your food is being cooked!";
            firstStepIcon = R.drawable.ic_restaurant;
        }

        setStep(binding.stepPreparing.getRoot(), binding.linePreparing, firstStepTitle, preparingDone, preparingActive || pendingActive, firstStepSub, firstStepIcon);

        boolean onWayDone = isDone(status, Order.STATUS_ON_THE_WAY);
        boolean onWayActive = Order.STATUS_ON_THE_WAY.equals(status);
        setStep(binding.stepOnTheWay.getRoot(), binding.lineOnTheWay, "Out for Delivery", onWayDone, onWayActive, null, R.drawable.ic_delivery_dining);

        boolean deliveredDone = Order.STATUS_DELIVERED.equals(status);
        setStep(binding.stepDelivered.getRoot(), null, "Delivered", deliveredDone, deliveredDone, null, R.drawable.ic_check_circle);
    }

    private void setStep(View stepView, View lineView, String title, boolean done, boolean active, String customSub, int activeIcon) {
        View circle = stepView.findViewById(R.id.stepCircle);
        ImageView icon = stepView.findViewById(R.id.stepIcon);
        TextView tvTitle = stepView.findViewById(R.id.stepTitle);
        TextView tvSub = stepView.findViewById(R.id.stepSubtitle);
        TextView tvLive = stepView.findViewById(R.id.stepLiveBadge);

        tvTitle.setText(title);

        if (done) {
            circle.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
            icon.setImageResource(R.drawable.ic_check_circle);
            icon.setColorFilter(ContextCompat.getColor(this, R.color.on_primary));
            tvSub.setText("✓ Completed");
            tvSub.setTextColor(ContextCompat.getColor(this, R.color.primary));
            tvLive.setVisibility(View.GONE);
            if (lineView != null) lineView.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        } else if (active) {
            circle.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
            icon.setImageResource(activeIcon);
            icon.setColorFilter(ContextCompat.getColor(this, R.color.on_primary));
            tvSub.setText(customSub != null ? customSub : "In Progress...");
            tvSub.setTextColor(ContextCompat.getColor(this, R.color.primary));
            tvLive.setVisibility(View.VISIBLE);
            if (lineView != null) lineView.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_variant));
        } else {
            circle.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.surface_variant));
            icon.setImageResource(R.drawable.ic_check_circle);
            icon.setColorFilter(ContextCompat.getColor(this, R.color.text_hint));
            tvSub.setText("Waiting...");
            tvSub.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
            tvLive.setVisibility(View.GONE);
            if (lineView != null) lineView.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_variant));
        }
    }

    private void updateOrderDetails(Order order) {
        binding.layoutOrderItems.removeAllViews();

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (CartItem item : order.getItems()) {
                addItemToLayout(item.getProductName(), item.getQuantity(), item.getPrice(), item.getImageBase64());
            }
        } else {
            // Fallback for legacy single-item orders
            addItemToLayout(order.getProductName(), order.getQuantity(), order.getProductPrice(), order.getProductImageBase64());
        }

        binding.tvTotal.setText(String.format(Locale.getDefault(), "₱%.0f", order.getTotalAmount()));
    }

    private void addItemToLayout(String name, int qty, double price, String imgBase64) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_order_summary_product, binding.layoutOrderItems, false);
        
        ImageView iv = view.findViewById(R.id.ivProduct);
        TextView tvName = view.findViewById(R.id.tvProductName);
        TextView tvQty = view.findViewById(R.id.tvProductQty);
        TextView tvPrice = view.findViewById(R.id.tvProductPrice);

        tvName.setText(name);
        tvQty.setText("x" + qty);
        tvPrice.setText(String.format(Locale.getDefault(), "₱%.0f", price * qty));

        if (imgBase64 != null && !imgBase64.isEmpty()) {
            Bitmap bm = ImageHelper.base64ToBitmap(imgBase64);
            if (bm != null) iv.setImageBitmap(bm);
        }

        binding.layoutOrderItems.addView(view);
    }

    private void updateAddress(Order order) {
        String fullAddress = order.getCustomerAddress();
        if (fullAddress != null && !fullAddress.isEmpty()) {
            if (fullAddress.contains(",")) {
                int firstComma = fullAddress.indexOf(",");
                binding.tvAddressMain.setText(fullAddress.substring(0, firstComma).trim());
                binding.tvAddressSub.setText(fullAddress.substring(firstComma + 1).trim());
            } else {
                binding.tvAddressMain.setText(fullAddress);
                binding.tvAddressSub.setText("");
            }
        } else {
            binding.tvAddressMain.setText("Location pinned");
            binding.tvAddressSub.setText("");
        }
    }

    private void updateContactInfo(Order order) {
        binding.tvContactName.setText(order.getSellerName());
        
        FirebaseHelper.getDb().collection("users").document(order.getSellerId()).get().addOnSuccessListener(doc -> {
            String phone = doc.getString("phone");
            if (phone != null) {
                binding.tvContactPhone.setText(phone);
                binding.btnCall.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone))));
                binding.btnMessage.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("smsto:" + phone));
                    startActivity(intent);
                });
            }
        });
    }

    private void updateMapVisibility(Order order) {
        boolean showMap = Order.STATUS_ON_THE_WAY.equals(order.getStatus());
        binding.cardMap.setVisibility(showMap ? View.VISIBLE : View.GONE);
        if (showMap) updateMapMarkers(order);
    }

    private void updateMapMarkers(Order order) {
        double lat = (order.getRiderLat() != 0) ? order.getRiderLat() : order.getSellerLat();
        double lng = (order.getRiderLng() != 0) ? order.getRiderLng() : order.getSellerLng();
        double cusLat = order.getCustomerLat(), cusLng = order.getCustomerLng();

        if (lat == 0 || cusLat == 0) return;

        GeoPoint riderPoint = new GeoPoint(lat, lng);
        GeoPoint customerPoint = new GeoPoint(cusLat, cusLng);

        if (riderMarker == null) {
            riderMarker = new Marker(binding.mapView);
            riderMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_delivery_dining));
            binding.mapView.getOverlays().add(riderMarker);
        }
        riderMarker.setPosition(riderPoint);

        if (customerMarker == null) {
            customerMarker = new Marker(binding.mapView);
            binding.mapView.getOverlays().add(customerMarker);
        }
        customerMarker.setPosition(customerPoint);

        binding.mapView.getController().animateTo(riderPoint);
        binding.mapView.invalidate();
    }

    private boolean isDone(String current, String step) { return stepIndex(current) > stepIndex(step); }
    private int stepIndex(String status) {
        if (Order.STATUS_PENDING.equals(status)) return 0;
        if (Order.STATUS_CONFIRMED.equals(status)) return 1;
        if (Order.STATUS_PREPARING.equals(status)) return 2;
        if (Order.STATUS_ON_THE_WAY.equals(status)) return 3;
        if (Order.STATUS_DELIVERED.equals(status)) return 4;
        return 0;
    }

    @Override protected void onResume() { super.onResume(); binding.mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); binding.mapView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); if (orderListener != null) orderListener.remove(); binding.mapView.onDetach(); }
}