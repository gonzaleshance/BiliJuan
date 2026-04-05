package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.android.material.button.MaterialButton;

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

    // Saranay Road, Barangay 171, Caloocan
    private static final double SARANAY_LAT  = 14.7548685;
    private static final double SARANAY_LNG  = 121.0258531;
    private static final double DEFAULT_ZOOM = 17.0;

    private MapView        mapView;
    private Marker         pinMarker;
    private GeoPoint       selectedPoint;

    private TextView       tvInstruction;
    private MaterialButton btnConfirm;
    private MaterialButton btnSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Required by OSMDroid before setContentView
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_pin_location);

        tvInstruction = findViewById(R.id.tvInstruction);
        btnConfirm    = findViewById(R.id.btnConfirmPin);
        btnSkip       = findViewById(R.id.btnSkipPin);
        mapView       = findViewById(R.id.mapView);

        setupMap();

        btnConfirm.setEnabled(false);

        btnConfirm.setOnClickListener(v -> saveLocation());

        btnSkip.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
    }

    // ── Map setup ─────────────────────────────────────────────────────────────

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(
                org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT
        );

        // Center on Saranay
        IMapController controller = mapView.getController();
        controller.setZoom(DEFAULT_ZOOM);
        controller.setCenter(new GeoPoint(SARANAY_LAT, SARANAY_LNG));

        // Tap overlay — converts screen tap to GeoPoint and drops pin
        Overlay tapOverlay = new Overlay() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView) {
                // Convert screen pixel tap to map coordinates
                GeoPoint tappedPoint = (GeoPoint) mapView.getProjection()
                        .fromPixels((int) e.getX(), (int) e.getY());
                dropPin(tappedPoint);
                return true;
            }
        };

        mapView.getOverlays().add(tapOverlay);
    }

    private void dropPin(GeoPoint point) {
        selectedPoint = point;

        // Remove existing pin
        if (pinMarker != null) {
            mapView.getOverlays().remove(pinMarker);
        }

        // Create new marker with OSMDroid default icon (always works)
        pinMarker = new Marker(mapView);
        pinMarker.setPosition(point);
        pinMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        pinMarker.setTitle("Your delivery location");
        // Default OSMDroid marker icon — guaranteed to show
        pinMarker.setIcon(
                getResources().getDrawable(
                        org.osmdroid.library.R.drawable.marker_default, getTheme()
                )
        );

        mapView.getOverlays().add(pinMarker);
        mapView.invalidate(); // refresh the map

        tvInstruction.setText("📍 Pin dropped! Tap Confirm to save your location.");
        btnConfirm.setEnabled(true);
    }

    // ── Save to Firestore ─────────────────────────────────────────────────────

    private void saveLocation() {
        if (selectedPoint == null) return;

        String uid = FirebaseHelper.getCurrentUid();
        if (uid == null) {
            Toast.makeText(this,
                    "Session expired. Please log in again.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirm.setEnabled(false);
        btnConfirm.setText("Saving…");

        Map<String, Object> update = new HashMap<>();
        update.put("latitude",  selectedPoint.getLatitude());
        update.put("longitude", selectedPoint.getLongitude());

        FirebaseHelper.getDb()
                .collection("users")
                .document(uid)
                .update(update)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            "Location saved! Riders will find you easily. ✅",
                            Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("Confirm Location");
                    Toast.makeText(this,
                            "Could not save location. Please try again.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ── OSMDroid lifecycle — always required ──────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDetach();
    }
}