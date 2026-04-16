package com.appdev.bilijuan.activities.seller;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.databinding.ActivitySellerDeliveryMapBinding;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
 * Fixed "Failed to update order" bug and lifecycle crashes.
 */
public class SellerDeliveryMapActivity extends AppCompatActivity {

    private static final int    LOCATION_PERMISSION_REQUEST = 1001;
    private static final int    REQUEST_CHECK_SETTINGS      = 1002;
    private static final long   LOCATION_INTERVAL_MS        = 5_000L;
    private static final long   LOCATION_FASTEST_MS         = 2_000L;
    private static final double DEFAULT_ZOOM                = 15.5;

    private ActivitySellerDeliveryMapBinding binding;

    private String orderId;
    private Order  order;

    private Marker   riderMarker;
    private Marker   customerMarker;
    private Polyline routeLine;

    private FusedLocationProviderClient fusedClient;
    private LocationCallback            locationCallback;
    private LocationRequest             locationRequest;
    private boolean                     mapCenteredOnce = false;

    private ListenerRegistration orderListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        if (binding != null && binding.mapView != null) {
            binding.mapView.onResume();
        }
        checkLocationSettingsAndStartUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (binding != null && binding.mapView != null) {
            binding.mapView.onPause();
        }
        stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        if (orderListener != null) orderListener.remove();
        if (binding != null && binding.mapView != null) {
            binding.mapView.onDetach();
        }
        binding = null;
    }

    private void setupMap() {
        if (binding == null) return;
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK);
        binding.mapView.setMultiTouchControls(true);
        binding.mapView.getController().setZoom(DEFAULT_ZOOM);
    }

    private void setupButtons() {
        if (binding == null) return;
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnCallCustomer.setOnClickListener(v -> {
            if (order == null || order.getCustomerPhone() == null) return;
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + order.getCustomerPhone())));
        });
        binding.btnOpenGoogleMaps.setOnClickListener(v -> {
            if (order == null) return;
            double lat = order.getCustomerLat();
            double lng = order.getCustomerLng();
            
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + lat + "," + lng + "(Customer Delivery Spot)");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            
            try {
                startActivity(mapIntent);
            } catch (ActivityNotFoundException e) {
                Uri webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng);
                startActivity(new Intent(Intent.ACTION_VIEW, webUri));
            }
        });
        binding.btnMarkDelivered.setOnClickListener(v -> showConfirmDeliveredSheet());
    }

    private void showConfirmDeliveredSheet() {
        if (isFinishing() || isDestroyed() || binding == null) return;

        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetStyle);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_confirm_delivered, null);
        sheet.setContentView(v);

        v.findViewById(R.id.btnFinalConfirm).setOnClickListener(view -> {
            sheet.dismiss();
            advanceToDelivered();
        });

        v.findViewById(R.id.btnCancel).setOnClickListener(view -> sheet.dismiss());
        sheet.show();
    }

    private void setupLocationClient() {
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
                .setMinUpdateIntervalMillis(LOCATION_FASTEST_MS)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                if (isFinishing() || isDestroyed()) return;
                Location loc = result.getLastLocation();
                if (loc != null) onRiderLocationUpdated(loc);
            }
        };
    }

    private void checkLocationSettingsAndStartUpdates() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> startLocationUpdates());

        task.addOnFailureListener(this, e -> {
            if (isFinishing() || isDestroyed()) return;
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(SellerDeliveryMapActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    Log.e("Location", "Error resolution", sendEx);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) startLocationUpdates();
            else Toast.makeText(this, "Location services must be enabled to track delivery", Toast.LENGTH_SHORT).show();
        }
    }

    private void listenOrder() {
        orderListener = FirebaseHelper.getDb().collection("orders").document(orderId)
                .addSnapshotListener((snap, e) -> {
                    if (isFinishing() || isDestroyed() || binding == null) return;
                    if (e != null || snap == null || !snap.exists()) return;
                    order = snap.toObject(Order.class);
                    if (order == null) return;
                    order.setOrderId(snap.getId());
                    updateOrderCard();
                    placeCustomerMarker();
                    updateDeliveredButtonVisibility();
                });
    }

    private void updateOrderCard() {
        if (binding == null) return;
        binding.tvOrderId.setText(shortOrderId());
        binding.tvCustomerName.setText(order.getCustomerName());
        binding.tvCustomerAddress.setText(order.getCustomerAddress());
        binding.tvOrderTotal.setText(String.format("₱%.2f", order.getTotalAmount()));
        binding.tvOrderStatus.setText(order.getStatus());
        binding.tvOrderStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor(order.getStatus())));
    }

    private void placeCustomerMarker() {
        if (binding == null || order == null) return;
        double lat = order.getCustomerLat();
        double lng = order.getCustomerLng();
        if (lat == 0 && lng == 0) return;
        GeoPoint customerPoint = new GeoPoint(lat, lng);
        if (customerMarker == null) {
            customerMarker = new Marker(binding.mapView);
            customerMarker.setTitle(order.getCustomerName());
            customerMarker.setSubDescription(order.getCustomerAddress());
            customerMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_location));
            customerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        }
        customerMarker.setPosition(customerPoint);
        if (!binding.mapView.getOverlays().contains(customerMarker)) binding.mapView.getOverlays().add(customerMarker);
        if (riderMarker != null) drawRoute();
        if (!mapCenteredOnce) {
            binding.mapView.getController().setCenter(customerPoint);
            mapCenteredOnce = true;
        }
        binding.mapView.invalidate();
    }

    private void updateDeliveredButtonVisibility() {
        if (binding == null || order == null) return;
        binding.btnMarkDelivered.setVisibility(Order.STATUS_ON_THE_WAY.equals(order.getStatus()) ? View.VISIBLE : View.GONE);
    }

    private void onRiderLocationUpdated(Location loc) {
        if (binding == null) return;
        double lat = loc.getLatitude();
        double lng = loc.getLongitude();
        GeoPoint riderPoint = new GeoPoint(lat, lng);
        if (riderMarker == null) {
            riderMarker = new Marker(binding.mapView);
            riderMarker.setTitle("You (Rider)");
            riderMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_rider));
            riderMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        }
        riderMarker.setPosition(riderPoint);
        if (!binding.mapView.getOverlays().contains(riderMarker)) binding.mapView.getOverlays().add(riderMarker);
        if (!mapCenteredOnce) {
            binding.mapView.getController().setCenter(riderPoint);
            mapCenteredOnce = true;
        }
        drawRoute();
        binding.mapView.invalidate();
        pushRiderLocationToFirestore(lat, lng);
    }

    private void drawRoute() {
        if (riderMarker == null || customerMarker == null) return;
        fetchRoadRoute(riderMarker.getPosition(), customerMarker.getPosition());
    }

    private void fetchRoadRoute(GeoPoint from, GeoPoint to) {
        String url = "https://router.project-osrm.org/route/v1/driving/" + from.getLongitude() + "," + from.getLatitude() + ";" + to.getLongitude() + "," + to.getLatitude() + "?overview=full&geometries=geojson";
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(5000); conn.setReadTimeout(5000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder(); String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                JSONObject json = new JSONObject(sb.toString());
                JSONArray coords = json.getJSONArray("routes").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");
                List<GeoPoint> routePoints = new ArrayList<>();
                for (int i = 0; i < coords.length(); i++) {
                    JSONArray point = coords.getJSONArray(i);
                    routePoints.add(new GeoPoint(point.getDouble(1), point.getDouble(0)));
                }
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || binding == null) return;
                    if (routeLine == null) {
                        routeLine = new Polyline();
                        routeLine.getOutlinePaint().setColor(android.graphics.Color.parseColor("#2E7D32"));
                        routeLine.getOutlinePaint().setStrokeWidth(10f);
                    }
                    routeLine.setPoints(routePoints);
                    if (!binding.mapView.getOverlays().contains(routeLine)) binding.mapView.getOverlays().add(0, routeLine);
                    binding.mapView.invalidate();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || binding == null) return;
                    if (routeLine == null) {
                        routeLine = new Polyline();
                        routeLine.getOutlinePaint().setColor(android.graphics.Color.RED);
                        routeLine.getOutlinePaint().setStrokeWidth(8f);
                    }
                    routeLine.setPoints(Arrays.asList(from, to));
                    if (!binding.mapView.getOverlays().contains(routeLine)) binding.mapView.getOverlays().add(0, routeLine);
                    binding.mapView.invalidate();
                });
            }
        }).start();
    }

    private void pushRiderLocationToFirestore(double lat, double lng) {
        if (orderId == null) return;
        Map<String, Object> update = new HashMap<>();
        update.put("riderLat", lat);
        update.put("riderLng", lng);
        FirebaseHelper.getDb().collection("orders").document(orderId).update(update);
    }

    private void startLocationUpdates() {
        if (!hasLocationPermission()) return;
        try {
            fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException ignored) { }
    }

    private void stopLocationUpdates() {
        if (fusedClient != null && locationCallback != null) fusedClient.removeLocationUpdates(locationCallback);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) checkLocationSettingsAndStartUpdates();
    }

    private void advanceToDelivered() {
        if (orderId == null || binding == null) return;
        binding.btnMarkDelivered.setEnabled(false);
        
        // Use document(orderId).update("status", ...) instead of passing a whole Map
        // This is safer and less prone to schema validation issues
        FirebaseHelper.getDb().collection("orders").document(orderId)
                .update("status", Order.STATUS_DELIVERED)
                .addOnSuccessListener(v -> { 
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "Delivered!", Toast.LENGTH_SHORT).show(); 
                    finish(); 
                })
                .addOnFailureListener(e -> { 
                    if (binding != null) binding.btnMarkDelivered.setEnabled(true); 
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show(); 
                });
    }

    private String shortOrderId() {
        return orderId.length() >= 6 ? "Order #" + orderId.substring(0, 6).toUpperCase() : "Order #" + orderId;
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