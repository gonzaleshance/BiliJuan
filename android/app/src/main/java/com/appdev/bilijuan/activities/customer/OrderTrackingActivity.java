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
    private Marker sellerMarker, customerMarker;
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
        String shortId = orderId.length() >= 6
                ? "Order #" + orderId.substring(0, 6).toUpperCase()
                : "Order #" + orderId;
        binding.tvOrderId.setText(shortId);
        binding.tvSellerName.setText(order.getSellerName());
        binding.tvCurrentStatus.setText(order.getStatus());

        binding.tvEta.setText("10-20 min"); 

        int pct = progressForStatus(order.getStatus());
        binding.progressDelivery.setProgress(pct);
        binding.tvProgressPct.setText(pct + "% complete");

        binding.btnCallSeller.setOnClickListener(v -> {
            FirebaseHelper.getDb().collection("users")
                    .document(order.getSellerId()).get()
                    .addOnSuccessListener(doc -> {
                        String sp = doc.getString("phone");
                        if (sp != null && !sp.isEmpty()) {
                            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + sp)));
                        } else {
                            Toast.makeText(this, "No phone available", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void updateProgressSteps(String status) {
        // FIXED: Use .getRoot() because binding.stepConfirmed is an include binding
        setStep(binding.stepConfirmed.getRoot(), binding.lineConfirmed,
                "Order Confirmed", "Completed",
                isDone(status, "CONFIRMED"),
                isActive(status, "CONFIRMED"));

        setStep(binding.stepPreparing.getRoot(), binding.linePreparing,
                "Preparing Your Food", "Completed",
                isDone(status, "PREPARING"),
                isActive(status, "PREPARING"));

        setStep(binding.stepOnTheWay.getRoot(), binding.lineOnTheWay,
                "Out for Delivery", "In Progress...",
                isDone(status, "ON_THE_WAY"),
                isActive(status, "ON_THE_WAY"));

        setStep(binding.stepDelivered.getRoot(), null,
                "Delivered", "Waiting...",
                "DELIVERED".equals(status), false);
    }

    private void setStep(View stepView, View lineView,
                         String title, String subtitle,
                         boolean done, boolean active) {
        View circle       = stepView.findViewById(R.id.stepCircle);
        ImageView icon    = stepView.findViewById(R.id.stepIcon);
        TextView tvTitle  = stepView.findViewById(R.id.stepTitle);
        TextView tvSub    = stepView.findViewById(R.id.stepSubtitle);
        TextView tvLive   = stepView.findViewById(R.id.stepLiveBadge);

        tvTitle.setText(title);

        if (done) {
            circle.setBackgroundResource(R.drawable.bg_primary_circle);
            icon.setImageResource(R.drawable.ic_check_circle);
            icon.setColorFilter(ContextCompat.getColor(this, R.color.primary));
            tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tvSub.setText("✓ Completed");
            tvSub.setTextColor(ContextCompat.getColor(this, R.color.primary));
            tvLive.setVisibility(View.GONE);
            if (lineView != null)
                lineView.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        } else if (active) {
            circle.setBackgroundResource(R.drawable.bg_primary_pill);
            icon.setColorFilter(ContextCompat.getColor(this, R.color.on_primary));
            tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tvSub.setText("In Progress...");
            tvSub.setTextColor(ContextCompat.getColor(this, R.color.primary));
            tvLive.setVisibility(title.equals("Out for Delivery") ? View.VISIBLE : View.GONE);
            if (lineView != null)
                lineView.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_variant));
        } else {
            circle.setBackgroundResource(R.drawable.bg_chip_inactive);
            icon.setColorFilter(ContextCompat.getColor(this, R.color.text_hint));
            tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
            tvSub.setText(subtitle);
            tvSub.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
            tvLive.setVisibility(View.GONE);
            if (lineView != null)
                lineView.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_variant));
        }
    }

    private void updateOrderDetails(Order order) {
        binding.tvProductName.setText(order.getProductName() + " × " + order.getQuantity());
        binding.tvProductTotal.setText(String.format("₱%.0f", order.getProductPrice() * order.getQuantity()));
        binding.tvDeliveryFee.setText(String.format("₱%.0f", order.getDeliveryFee()));
        binding.tvTotal.setText(String.format("₱%.0f", order.getTotalAmount()));
    }

    private void updateMapVisibility(Order order) {
        boolean showMap = "ON_THE_WAY".equals(order.getStatus()) || "DELIVERED".equals(order.getStatus());
        binding.cardMap.setVisibility(showMap ? View.VISIBLE : View.GONE);
        if (showMap) showRouteOnMap(order);
    }

    private void showRouteOnMap(Order order) {
        double selLat = order.getSellerLat(), selLng = order.getSellerLng();
        double cusLat = order.getCustomerLat(), cusLng = order.getCustomerLng();
        if (selLat == 0 && cusLat == 0) return;

        GeoPoint sellerPoint   = new GeoPoint(selLat, selLng);
        GeoPoint customerPoint = new GeoPoint(cusLat, cusLng);

        binding.mapView.getController().setCenter(new GeoPoint((selLat + cusLat) / 2, (selLng + cusLng) / 2));
        binding.mapView.getController().setZoom(16.5);
        binding.mapView.getOverlays().clear();

        if (routeLine == null) {
            routeLine = new Polyline();
            routeLine.getOutlinePaint().setColor(ContextCompat.getColor(this, R.color.primary));
            routeLine.getOutlinePaint().setStrokeWidth(8f);
        }
        routeLine.setPoints(Arrays.asList(sellerPoint, customerPoint));
        binding.mapView.getOverlays().add(routeLine);

        if (sellerMarker == null) sellerMarker = new Marker(binding.mapView);
        sellerMarker.setPosition(sellerPoint);
        sellerMarker.setTitle(order.getSellerName());
        binding.mapView.getOverlays().add(sellerMarker);

        if (customerMarker == null) customerMarker = new Marker(binding.mapView);
        customerMarker.setPosition(customerPoint);
        customerMarker.setTitle("Your Location");
        binding.mapView.getOverlays().add(customerMarker);

        binding.mapView.invalidate();
    }

    private boolean isDone(String current, String step) {
        return stepIndex(current) > stepIndex(step);
    }

    private boolean isActive(String current, String step) {
        return step.equals(current);
    }

    private int stepIndex(String status) {
        if (status == null) return 0;
        switch (status) {
            case "PENDING":    return 0;
            case "CONFIRMED":  return 1;
            case "PREPARING":  return 2;
            case "ON_THE_WAY": return 3;
            case "DELIVERED":  return 4;
            default: return 0;
        }
    }

    private int progressForStatus(String status) {
        if (status == null) return 0;
        switch (status) {
            case "CONFIRMED":  return 25;
            case "PREPARING":  return 50;
            case "ON_THE_WAY": return 75;
            case "DELIVERED":  return 100;
            default: return 10;
        }
    }

    @Override protected void onResume() { super.onResume(); binding.mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); binding.mapView.onPause(); }
    @Override protected void onDestroy() {
        super.onDestroy();
        if (orderListener != null) orderListener.remove();
        binding.mapView.onDetach();
    }
}
