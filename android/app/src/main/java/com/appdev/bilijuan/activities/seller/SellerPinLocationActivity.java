package com.appdev.bilijuan.activities.seller;

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

public class SellerPinLocationActivity extends AppCompatActivity {

    private static final double SARANAY_LAT  = 14.7548685;
    private static final double SARANAY_LNG  = 121.0258531;
    private static final double DEFAULT_ZOOM = 17.0;

    private MapView        mapView;
    private Marker         pinMarker;
    private GeoPoint       selectedPoint;
    private TextView       tvInstruction;
    private MaterialButton btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_pin_location); // reuse same layout

        tvInstruction = findViewById(R.id.tvInstruction);
        btnConfirm    = findViewById(R.id.btnConfirmPin);
        mapView       = findViewById(R.id.mapView);

        // Hide skip button — seller must pin
        findViewById(R.id.btnSkipPin).setVisibility(android.view.View.GONE);

        setupMap();
        btnConfirm.setEnabled(false);
        btnConfirm.setOnClickListener(v -> saveLocation());
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        IMapController controller = mapView.getController();
        controller.setZoom(DEFAULT_ZOOM);
        controller.setCenter(new GeoPoint(SARANAY_LAT, SARANAY_LNG));

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
        pinMarker.setTitle("Your store location");
        pinMarker.setIcon(getResources().getDrawable(
                org.osmdroid.library.R.drawable.marker_default, getTheme()));

        mapView.getOverlays().add(pinMarker);
        mapView.invalidate();

        tvInstruction.setText("📍 Pin dropped! Tap Confirm to save your store location.");
        btnConfirm.setEnabled(true);
    }

    private void saveLocation() {
        if (selectedPoint == null) return;
        String uid = FirebaseHelper.getCurrentUid();
        if (uid == null) { finish(); return; }

        btnConfirm.setEnabled(false);
        btnConfirm.setText("Saving…");

        Map<String, Object> update = new HashMap<>();
        update.put("latitude",  selectedPoint.getLatitude());
        update.put("longitude", selectedPoint.getLongitude());

        FirebaseHelper.getDb().collection("users").document(uid)
                .update(update)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Store location saved! ✅", Toast.LENGTH_SHORT).show();
                    finish(); // goes back to SellerAccountActivity
                })
                .addOnFailureListener(e -> {
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("Confirm Location");
                    Toast.makeText(this, "Could not save. Try again.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause()  { super.onPause();  mapView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDetach(); }
}