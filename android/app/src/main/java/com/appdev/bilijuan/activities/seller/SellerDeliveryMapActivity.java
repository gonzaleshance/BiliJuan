package com.appdev.bilijuan.activities.seller;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.databinding.ActivitySellerDeliveryMapBinding;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.ListenerRegistration;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * SellerDeliveryMapActivity
 *
 * Launched when the seller taps "View Map" on an order card in SellerOrdersActivity.
 * Shows:
 *   • Seller's current live GPS position (blue rider marker)
 *   • Customer's pinned delivery location (green destination marker)
 *   • A straight-line route between the two points
 *   • Order summary card (customer name, address, total, status)
 *   • "Call Customer" and "Open in Google Maps" action buttons
 *
 * Also pushes the seller's live GPS updates to Firestore (riderLat / riderLng)
 * so the customer's OrderTrackingActivity can display real-time movement.
 *
 * Requires:
 *   - Intent extra "orderId" (String)
 *   - osmdroid dependency already in build.gradle.kts
 *   - play-services-location (add to build.gradle.kts if missing — see PDF summary)
 */
public class SellerDeliveryMapActivity extends AppCompatActivity {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final int    LOCATION_PERMISSION_REQUEST = 1001;
    private static final long   LOCATION_INTERVAL_MS        = 5_000L;   // update every 5 s
    private static final long   LOCATION_FASTEST_MS         = 2_000L;
    private static final double DEFAULT_ZOOM                = 15.5;

    // ── Views / Binding ───────────────────────────────────────────────────────
    private ActivitySellerDeliveryMapBinding binding;

    // ── State ─────────────────────────────────────────────────────────────────
    private String orderId;
    private Order  order;

    // ── Map overlays ──────────────────────────────────────────────────────────
    private Marker   riderMarker;       // seller's live GPS
    private Marker   customerMarker;    // customer delivery pin
    private Polyline routeLine;

    // ── Location ──────────────────────────────────────────────────────────────
    private FusedLocationProviderClient fusedClient;
    private LocationCallback            locationCallback;
    private boolean                     mapCenteredOnce = false;

    // ── Firestore ─────────────────────────────────────────────────────────────
    private ListenerRegistration orderListener;

    // ═════════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // OSMDroid must be configured before inflate
        Configuration.getInstance().setUserAgentValue(getPackageName());

        binding = ActivitySellerDeliveryMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        orderId = getIntent().getStringExtra("orderId");
        if (orderId == null) {
            Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupMap();
        setupButtons();
        setupLocationClient();
        listenOrder();
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.mapView.onResume();
        if (hasLocationPermission()) startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.mapView.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        if (orderListener != null) orderListener.remove();
        binding.mapView.onDetach();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Setup helpers
    // ═════════════════════════════════════════════════════════════════════════

    private void setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK);
        binding.mapView.setMultiTouchControls(true);
        binding.mapView.getController().setZoom(DEFAULT_ZOOM);
    }

    private void setupButtons() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnCallCustomer.setOnClickListener(v -> {
            if (order == null || order.getCustomerPhone() == null) return;
            startActivity(new Intent(Intent.ACTION_DIAL,
                    Uri.parse("tel:" + order.getCustomerPhone())));
        });

        binding.btnOpenGoogleMaps.setOnClickListener(v -> {
            if (order == null) return;
            double lat = order.getCustomerLat();
            double lng = order.getCustomerLng();
            Uri geoUri = Uri.parse("google.navigation:q=" + lat + "," + lng + "&mode=d");
            Intent navIntent = new Intent(Intent.ACTION_VIEW, geoUri);
            navIntent.setPackage("com.google.android.apps.maps");
            if (navIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(navIntent);
            } else {
                // Fallback: open in browser
                Uri browserUri = Uri.parse(
                        "https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng);
                startActivity(new Intent(Intent.ACTION_VIEW, browserUri));
            }
        });

        // "Mark Delivered" quick-action button (only visible when status is ON_THE_WAY)
        binding.btnMarkDelivered.setOnClickListener(v -> advanceToDelivered());
    }

    private void setupLocationClient() {
        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc != null) onRiderLocationUpdated(loc);
            }
        };

        if (hasLocationPermission()) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Firestore listener — live order data
    // ═════════════════════════════════════════════════════════════════════════

    private void listenOrder() {
        orderListener = FirebaseHelper.getDb()
                .collection("orders")
                .document(orderId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null || !snap.exists()) return;
                    order = snap.toObject(Order.class);
                    if (order == null) return;
                    order.setOrderId(snap.getId());

                    updateOrderCard();
                    placeCustomerMarker();
                    updateDeliveredButtonVisibility();
                });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UI update helpers
    // ═════════════════════════════════════════════════════════════════════════

    private void updateOrderCard() {
        binding.tvOrderId.setText(shortOrderId());
        binding.tvCustomerName.setText(order.getCustomerName());
        binding.tvCustomerAddress.setText(order.getCustomerAddress());
        binding.tvOrderTotal.setText(String.format("₱%.2f", order.getTotalAmount()));
        binding.tvOrderStatus.setText(order.getStatus());

        // Colour status badge
        int statusColor = statusColor(order.getStatus());
        binding.tvOrderStatus.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(statusColor));
    }

    private void placeCustomerMarker() {
        double lat = order.getCustomerLat();
        double lng = order.getCustomerLng();
        if (lat == 0 && lng == 0) return;

        GeoPoint customerPoint = new GeoPoint(lat, lng);

        if (customerMarker == null) {
            customerMarker = new Marker(binding.mapView);
            customerMarker.setTitle(order.getCustomerName());
            customerMarker.setSubDescription(order.getCustomerAddress());
            customerMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_location));
        }
        customerMarker.setPosition(customerPoint);

        if (!binding.mapView.getOverlays().contains(customerMarker))
            binding.mapView.getOverlays().add(customerMarker);

        // If we already have a rider marker, redraw the route
        if (riderMarker != null) drawRoute();

        // Centre map once on the customer location if GPS not yet available
        if (!mapCenteredOnce) {
            binding.mapView.getController().setCenter(customerPoint);
            mapCenteredOnce = true;
        }

        binding.mapView.invalidate();
    }

    private void updateDeliveredButtonVisibility() {
        boolean onTheWay = Order.STATUS_ON_THE_WAY.equals(order.getStatus());
        binding.btnMarkDelivered.setVisibility(onTheWay ? View.VISIBLE : View.GONE);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  GPS / Location
    // ═════════════════════════════════════════════════════════════════════════

    private void onRiderLocationUpdated(Location loc) {
        double lat = loc.getLatitude();
        double lng = loc.getLongitude();

        GeoPoint riderPoint = new GeoPoint(lat, lng);

        // Update rider marker
        if (riderMarker == null) {
            riderMarker = new Marker(binding.mapView);
            riderMarker.setTitle("You (Rider)");
            riderMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_delivery_dining));
        }
        riderMarker.setPosition(riderPoint);

        if (!binding.mapView.getOverlays().contains(riderMarker))
            binding.mapView.getOverlays().add(riderMarker);

        // Centre map on rider on first GPS fix
        if (!mapCenteredOnce) {
            binding.mapView.getController().setCenter(riderPoint);
            mapCenteredOnce = true;
        }

        drawRoute();
        binding.mapView.invalidate();

        // Push riderLat/riderLng to Firestore for real-time customer tracking
        pushRiderLocationToFirestore(lat, lng);
    }

    /**
     * Draws (or redraws) a straight-line polyline between rider and customer.
     */
    private void drawRoute() {
        if (riderMarker == null || customerMarker == null) return;

        if (routeLine == null) {
            routeLine = new Polyline();
            routeLine.getOutlinePaint().setColor(
                    android.graphics.Color.RED);
            routeLine.getOutlinePaint().setStrokeWidth(8f);
        }
        routeLine.setPoints(Arrays.asList(
                riderMarker.getPosition(),
                customerMarker.getPosition()));

        if (!binding.mapView.getOverlays().contains(routeLine))
            binding.mapView.getOverlays().add(0, routeLine); // add below markers
    }

    /**
     * Writes the seller's live coordinates to the order document.
     * The customer's OrderTrackingActivity listens to this document
     * and updates its map accordingly.
     */
    private void pushRiderLocationToFirestore(double lat, double lng) {
        Map<String, Object> update = new HashMap<>();
        update.put("riderLat", lat);
        update.put("riderLng", lng);
        FirebaseHelper.getDb().collection("orders")
                .document(orderId)
                .update(update);
        // Failures are silent — GPS updates arrive every 5 s, so one miss is fine.
    }

    private void startLocationUpdates() {
        if (!hasLocationPermission()) return;
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,
                LOCATION_INTERVAL_MS)
                .setMinUpdateIntervalMillis(LOCATION_FASTEST_MS)
                .build();
        try {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        } catch (SecurityException ignored) { }
    }

    private void stopLocationUpdates() {
        if (fusedClient != null && locationCallback != null)
            fusedClient.removeLocationUpdates(locationCallback);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Permission result
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            Toast.makeText(this,
                    "Location permission needed to show your position on the map",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Order status advance
    // ═════════════════════════════════════════════════════════════════════════

    private void advanceToDelivered() {
        binding.btnMarkDelivered.setEnabled(false);
        FirebaseHelper.getDb().collection("orders")
                .document(orderId)
                .update("status", Order.STATUS_DELIVERED)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Order marked as Delivered!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnMarkDelivered.setEnabled(true);
                    Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Utility helpers
    // ═════════════════════════════════════════════════════════════════════════

    private String shortOrderId() {
        if (orderId.length() >= 6)
            return "Order #" + orderId.substring(0, 6).toUpperCase();
        return "Order #" + orderId;
    }

    private int statusColor(String status) {
        if (status == null) return ContextCompat.getColor(this, R.color.primary);
        switch (status) {
            case Order.STATUS_ON_THE_WAY: return ContextCompat.getColor(this, R.color.primary);
            case Order.STATUS_DELIVERED:  return ContextCompat.getColor(this, R.color.success);
            case Order.STATUS_PREPARING:  return ContextCompat.getColor(this, R.color.warning);
            default: return ContextCompat.getColor(this, R.color.text_secondary);
        }
    }
}