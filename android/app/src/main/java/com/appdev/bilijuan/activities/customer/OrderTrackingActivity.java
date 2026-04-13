package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.databinding.ActivityOrderTrackingBinding;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.firebase.firestore.ListenerRegistration;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.Arrays;

public class OrderTrackingActivity extends AppCompatActivity {

    private ActivityOrderTrackingBinding binding;
    private ListenerRegistration orderListener;
    private String orderId;
    private Marker riderMarker, customerMarker;
    private Polyline routeLine;

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
                    updateMapVisibility(order);
                });
    }

    private void updateHeader(Order order) {
        String shortId = orderId.length() >= 6 ? "Order #" + orderId.substring(0, 6).toUpperCase() : "Order #" + orderId;
        binding.tvOrderId.setText(shortId);
        binding.tvSellerName.setText(order.getSellerName());
        binding.tvCurrentStatus.setText(order.getStatus());
        binding.tvEta.setText("10-20 min"); 

        int pct = progressForStatus(order.getStatus());
        binding.progressDelivery.setProgress(pct);
        binding.tvProgressPct.setText(pct + "% complete");

        binding.btnCallSeller.setOnClickListener(v -> {
            FirebaseHelper.getDb().collection("users").document(order.getSellerId()).get().addOnSuccessListener(doc -> {
                String sp = doc.getString("phone");
                if (sp != null && !sp.isEmpty()) startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + sp)));
                else Toast.makeText(this, "No phone available", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void updateProgressSteps(String status) {
        setStep(binding.stepConfirmed.getRoot(), binding.lineConfirmed, "Order Confirmed", "Completed", isDone(status, "Confirmed"), "Confirmed".equals(status));
        setStep(binding.stepPreparing.getRoot(), binding.linePreparing, "Preparing Your Food", "Completed", isDone(status, "Preparing"), "Preparing".equals(status));
        setStep(binding.stepOnTheWay.getRoot(), binding.lineOnTheWay, "Out for Delivery", "In Progress...", isDone(status, "On the way"), "On the way".equals(status));
        setStep(binding.stepDelivered.getRoot(), null, "Delivered", "Waiting...", "Delivered".equals(status), false);
    }

    private void setStep(View stepView, View lineView, String title, String subtitle, boolean done, boolean active) {
        View circle = stepView.findViewById(R.id.stepCircle);
        ImageView icon = stepView.findViewById(R.id.stepIcon);
        TextView tvTitle = stepView.findViewById(R.id.stepTitle);
        TextView tvSub = stepView.findViewById(R.id.stepSubtitle);
        tvTitle.setText(title);

        if (done) {
            circle.setBackgroundResource(R.drawable.bg_primary_circle);
            icon.setImageResource(R.drawable.ic_check_circle);
            icon.setColorFilter(ContextCompat.getColor(this, R.color.primary));
            tvSub.setText("✓ Completed");
            if (lineView != null) lineView.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        } else if (active) {
            circle.setBackgroundResource(R.drawable.bg_primary_pill);
            icon.setColorFilter(ContextCompat.getColor(this, R.color.on_primary));
            tvSub.setText("In Progress...");
            if (lineView != null) lineView.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_variant));
        } else {
            circle.setBackgroundResource(R.drawable.bg_chip_inactive);
            tvSub.setText(subtitle);
            if (lineView != null) lineView.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_variant));
        }
    }

    private void updateOrderDetails(Order order) {
        binding.tvProductName.setText(order.getProductName() + " × " + order.getQuantity());
        binding.tvProductTotal.setText(String.format("₱%.0f", order.getProductPrice() * order.getQuantity()));
        binding.tvDeliveryFee.setText(String.format("₱%.0f", order.getDeliveryFee()));
        binding.tvTotal.setText(String.format("₱%.0f", order.getTotalAmount()));
    }

    private void updateMapVisibility(Order order) {
        boolean showMap = "On the way".equals(order.getStatus()) || "Delivered".equals(order.getStatus());
        binding.cardMap.setVisibility(showMap ? View.VISIBLE : View.GONE);
        if (showMap) updateMapMarkers(order);
    }

    private void updateMapMarkers(Order order) {
        // Use RIDER coordinates if available, otherwise fallback to seller store coordinates
        double lat = (order.getRiderLat() != 0) ? order.getRiderLat() : order.getSellerLat();
        double lng = (order.getRiderLng() != 0) ? order.getRiderLng() : order.getSellerLng();
        double cusLat = order.getCustomerLat(), cusLng = order.getCustomerLng();

        if (lat == 0 || cusLat == 0) return;

        GeoPoint riderPoint = new GeoPoint(lat, lng);
        GeoPoint customerPoint = new GeoPoint(cusLat, cusLng);

        if (riderMarker == null) {
            riderMarker = new Marker(binding.mapView);
            riderMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_delivery_dining));
            riderMarker.setTitle("Rider is here");
            binding.mapView.getOverlays().add(riderMarker);
        }
        riderMarker.setPosition(riderPoint);

        if (customerMarker == null) {
            customerMarker = new Marker(binding.mapView);
            customerMarker.setTitle("Your Location");
            binding.mapView.getOverlays().add(customerMarker);
        }
        customerMarker.setPosition(customerPoint);

        binding.mapView.getController().animateTo(riderPoint);
        binding.mapView.invalidate();
    }

    private boolean isDone(String current, String step) { return stepIndex(current) > stepIndex(step); }
    private int stepIndex(String status) {
        switch (status) {
            case "Pending": return 0;
            case "Confirmed": return 1;
            case "Preparing": return 2;
            case "On the way": return 3;
            case "Delivered": return 4;
            default: return 0;
        }
    }
    private int progressForStatus(String status) {
        switch (status) {
            case "Confirmed": return 25;
            case "Preparing": return 50;
            case "On the way": return 75;
            case "Delivered": return 100;
            default: return 10;
        }
    }

    @Override protected void onResume() { super.onResume(); binding.mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); binding.mapView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); if (orderListener != null) orderListener.remove(); binding.mapView.onDetach(); }
}
