package com.appdev.bilijuan.activities.customer;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(
                new File(getCacheDir(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(
                new File(getCacheDir(), "osmdroid/tiles"));
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
        setStep(binding.stepConfirmed.getRoot(), binding.lineConfirmed,
                "Order Confirmed", "Completed",
                isDone(status, Order.STATUS_CONFIRMED),
                isActive(status, Order.STATUS_CONFIRMED));

        setStep(binding.stepPreparing.getRoot(), binding.linePreparing,
                "Preparing Your Food", "Completed",
                isDone(status, Order.STATUS_PREPARING),
                isActive(status, Order.STATUS_PREPARING));

        setStep(binding.stepOnTheWay.getRoot(), binding.lineOnTheWay,
                "Out for Delivery", "In Progress...",
                isDone(status, Order.STATUS_ON_THE_WAY),
                isActive(status, Order.STATUS_ON_THE_WAY));

        setStep(binding.stepDelivered.getRoot(), null,
                "Delivered", "Waiting...",
                Order.STATUS_DELIVERED.equals(status), false);
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
        boolean showMap = Order.STATUS_ON_THE_WAY.equals(order.getStatus())
                || Order.STATUS_DELIVERED.equals(order.getStatus());
        binding.cardMap.setVisibility(showMap ? View.VISIBLE : View.GONE);
        if (showMap) showRouteOnMap(order);
    }

    private void showRouteOnMap(Order order) {
        double riderLat = order.getRiderLat();   // live rider GPS from Firestore
        double riderLng = order.getRiderLng();
        double cusLat   = order.getCustomerLat();
        double cusLng   = order.getCustomerLng();

        // Fall back to seller's store location if rider hasn't moved yet
        if (riderLat == 0 && riderLng == 0) {
            riderLat = order.getSellerLat();
            riderLng = order.getSellerLng();
        }
        if (riderLat == 0 && cusLat == 0) return;

        final double fromLat = riderLat, fromLng = riderLng;
        GeoPoint riderPoint    = new GeoPoint(fromLat, fromLng);
        GeoPoint customerPoint = new GeoPoint(cusLat, cusLng);

        // Centre map between rider and customer
        binding.mapView.getController().setCenter(
                new GeoPoint((fromLat + cusLat) / 2, (fromLng + cusLng) / 2));
        binding.mapView.getController().setZoom(16.5);
        binding.mapView.getOverlays().clear();

        // Place rider marker
        if (sellerMarker == null) sellerMarker = new Marker(binding.mapView);
        sellerMarker.setPosition(riderPoint);
        sellerMarker.setTitle("🛵 Rider");
        sellerMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_delivery_dining));
        binding.mapView.getOverlays().add(sellerMarker);

        // Place customer marker
        if (customerMarker == null) customerMarker = new Marker(binding.mapView);
        customerMarker.setPosition(customerPoint);
        customerMarker.setTitle("📍 Your Location");
        customerMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_location));
        binding.mapView.getOverlays().add(customerMarker);

        binding.mapView.invalidate();

        // Fetch road-snapped route + ETA
        fetchRoadRouteAndEta(fromLat, fromLng, cusLat, cusLng);
    }

    private void fetchRoadRouteAndEta(double fromLat, double fromLng,
                                      double toLat,   double toLng) {
        String url = "https://router.project-osrm.org/route/v1/driving/"
                + fromLng + "," + fromLat + ";"
                + toLng   + "," + toLat
                + "?overview=full&geometries=geojson";

        new Thread(() -> {
            try {
                HttpURLConnection conn =
                        (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json  = new JSONObject(sb.toString());
                JSONObject route = json.getJSONArray("routes").getJSONObject(0);

                // Road distance in km and duration in minutes
                double distanceKm  = route.getDouble("distance") / 1000.0;
                double durationMin = route.getDouble("duration") / 60.0;

                // Parse road-snapped coordinates
                JSONArray coords = route.getJSONObject("geometry")
                        .getJSONArray("coordinates");
                List<GeoPoint> roadPoints = new ArrayList<>();
                for (int i = 0; i < coords.length(); i++) {
                    JSONArray pt = coords.getJSONArray(i);
                    roadPoints.add(new GeoPoint(pt.getDouble(1), pt.getDouble(0)));
                }

                runOnUiThread(() -> {
                    // Draw road-snapped polyline
                    if (routeLine == null) {
                        routeLine = new Polyline();
                        routeLine.getOutlinePaint().setColor(
                                ContextCompat.getColor(this, R.color.primary));
                        routeLine.getOutlinePaint().setStrokeWidth(10f);
                    }
                    routeLine.setPoints(roadPoints);
                    // Insert below markers so markers stay on top
                    binding.mapView.getOverlays().add(0, routeLine);
                    binding.mapView.invalidate();

                    // Update ETA text
                    int etaMin = (int) Math.ceil(durationMin);
                    String etaText;
                    if (etaMin <= 1) {
                        etaText = "Arriving now";
                    } else if (etaMin < 60) {
                        etaText = etaMin + " min away";
                    } else {
                        etaText = (etaMin / 60) + "h " + (etaMin % 60) + "m away";
                    }
                    String distText = String.format("%.1f km • %s", distanceKm, etaText);
                    binding.tvEta.setText(distText);
                });

            } catch (Exception e) {
                // Fallback: straight line if OSRM unavailable
                runOnUiThread(() -> {
                    if (routeLine == null) {
                        routeLine = new Polyline();
                        routeLine.getOutlinePaint().setColor(
                                ContextCompat.getColor(this, R.color.primary));
                        routeLine.getOutlinePaint().setStrokeWidth(8f);
                    }
                    routeLine.setPoints(Arrays.asList(
                            new GeoPoint(fromLat, fromLng),
                            new GeoPoint(toLat, toLng)));
                    binding.mapView.getOverlays().add(0, routeLine);
                    binding.mapView.invalidate();
                    binding.tvEta.setText("Estimating...");
                });
            }
        }).start();
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
            case Order.STATUS_PENDING:    return 0;
            case Order.STATUS_CONFIRMED:  return 1;
            case Order.STATUS_PREPARING:  return 2;
            case Order.STATUS_ON_THE_WAY: return 3;
            case Order.STATUS_DELIVERED:  return 4;
            default: return 0;
        }
    }

    private int progressForStatus(String status) {
        if (status == null) return 0;
        switch (status) {
            case Order.STATUS_CONFIRMED:  return 25;
            case Order.STATUS_PREPARING:  return 50;
            case Order.STATUS_ON_THE_WAY: return 75;
            case Order.STATUS_DELIVERED:  return 100;
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
