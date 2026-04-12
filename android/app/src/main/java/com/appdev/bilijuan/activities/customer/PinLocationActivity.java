package com.appdev.bilijuan.activities.customer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.util.HashMap;
import java.util.Map;

public class PinLocationActivity extends AppCompatActivity {

    // Default center — Saranay Road, Barangay 171, Caloocan
    private static final double DEFAULT_LAT  = 14.7548685;
    private static final double DEFAULT_LNG  = 121.0258531;
    private static final double DEFAULT_ZOOM = 17.0;
    private static final int    GPS_PERMISSION_REQUEST = 3001;

    private MapView        mapView;
    private Marker         pinMarker;
    private GeoPoint       selectedPoint;
    private TextView       tvInstruction;
    private MaterialButton btnConfirm;
    private MaterialButton btnSkip;
    private FloatingActionButton btnMyLocation;

    private FusedLocationProviderClient fusedClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_pin_location);

        tvInstruction  = findViewById(R.id.tvInstruction);
        btnConfirm     = findViewById(R.id.btnConfirmPin);
        btnSkip        = findViewById(R.id.btnSkipPin);
        mapView        = findViewById(R.id.mapView);
        btnMyLocation  = findViewById(R.id.btnMyLocation);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        setupMap();
        btnConfirm.setEnabled(false);
        btnConfirm.setOnClickListener(v -> saveLocation());
        btnSkip.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });

        // "My Location" FAB — centers map on device GPS
        if (btnMyLocation != null) {
            btnMyLocation.setOnClickListener(v -> goToMyLocation());
        }

        // Auto-center on GPS when activity opens
        goToMyLocation();
    }

    // ── GPS centering ─────────────────────────────────────────────────────────

    private void goToMyLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchGpsAndCenter();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    GPS_PERMISSION_REQUEST);
        }
    }

    private void fetchGpsAndCenter() {
        try {
            fusedClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    GeoPoint gps = new GeoPoint(location.getLatitude(), location.getLongitude());
                    mapView.getController().animateTo(gps);
                    mapView.getController().setZoom(DEFAULT_ZOOM);
                    // Auto-drop a pin at current GPS location
                    dropPin(gps);
                } else {
                    // No last location — try fresh
                    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener(this, fresh -> {
                                if (fresh != null) {
                                    GeoPoint gps = new GeoPoint(fresh.getLatitude(), fresh.getLongitude());
                                    mapView.getController().animateTo(gps);
                                    mapView.getController().setZoom(DEFAULT_ZOOM);
                                    dropPin(gps);
                                }
                            });
                }
            });
        } catch (SecurityException ignored) {}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == GPS_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchGpsAndCenter();
        }
    }

    // ── Map setup ─────────────────────────────────────────────────────────────

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(
                org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);

        // Default center while GPS loads
        IMapController controller = mapView.getController();
        controller.setZoom(DEFAULT_ZOOM);
        controller.setCenter(new GeoPoint(DEFAULT_LAT, DEFAULT_LNG));

        // Tap to drop pin
        Overlay tapOverlay = new Overlay() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView) {
                GeoPoint tapped = (GeoPoint) mapView.getProjection()
                        .fromPixels((int) e.getX(), (int) e.getY());
                dropPin(tapped);
                return true;
            }
        };
        mapView.getOverlays().add(tapOverlay);
    }

    private void dropPin(GeoPoint point) {
        selectedPoint = point;

        if (pinMarker != null) mapView.getOverlays().remove(pinMarker);

        pinMarker = new Marker(mapView);
        pinMarker.setPosition(point);
        pinMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        pinMarker.setTitle("Your delivery location");
        pinMarker.setIcon(getResources().getDrawable(
                org.osmdroid.library.R.drawable.marker_default, getTheme()));

        mapView.getOverlays().add(pinMarker);
        mapView.invalidate();

        tvInstruction.setText("📍 Pin dropped! Tap Confirm to save, or tap elsewhere to adjust.");
        btnConfirm.setEnabled(true);
    }

    // ── Save to Firestore ─────────────────────────────────────────────────────

    private void saveLocation() {
        if (selectedPoint == null) return;

        String uid = FirebaseHelper.getCurrentUid();
        if (uid == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirm.setEnabled(false);
        btnConfirm.setText("Saving…");

        Map<String, Object> update = new HashMap<>();
        update.put("latitude",  selectedPoint.getLatitude());
        update.put("longitude", selectedPoint.getLongitude());

        FirebaseHelper.getDb().collection("users").document(uid)
                .update(update)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Location saved! ✅", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("Confirm Location");
                    Toast.makeText(this, "Could not save location. Try again.", Toast.LENGTH_SHORT).show();
                });
    }

    // ── OSMDroid lifecycle ────────────────────────────────────────────────────

    @Override protected void onResume()  { super.onResume();  mapView.onResume(); }
    @Override protected void onPause()   { super.onPause();   mapView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDetach(); }
}