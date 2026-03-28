package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.Map;

public class PinLocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Exact coordinates of Saranay Rd, Caloocan (from Google Maps)
    private static final double SARANAY_LAT = 14.7548685;
    private static final double SARANAY_LNG = 121.0258531;
    private static final float  DEFAULT_ZOOM = 17f;

    // Tight boundary around Saranay to prevent users pinning outside
    private static final LatLngBounds SARANAY_BOUNDS = new LatLngBounds(
            new LatLng(14.7480, 121.0180), // SW corner
            new LatLng(14.7620, 121.0340)  // NE corner
    );

    private GoogleMap map;
    private Marker    pinMarker;
    private LatLng    selectedLatLng;

    private TextView tvInstruction;
    private Button   btnConfirm;
    private Button   btnSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_location);

        tvInstruction = findViewById(R.id.tvInstruction);
        btnConfirm    = findViewById(R.id.btnConfirmPin);
        btnSkip       = findViewById(R.id.btnSkipPin);

        btnConfirm.setEnabled(false); // disabled until user drops a pin

        btnConfirm.setOnClickListener(v -> saveLocation());
        btnSkip.setOnClickListener(v -> {
            // User skips — go to home without saving a lat/lng
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapPinLocation);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        // Center map on Saranay
        LatLng saranay = new LatLng(SARANAY_LAT, SARANAY_LNG);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(saranay, DEFAULT_ZOOM));

        // Lock the camera to the Saranay boundary
        map.setLatLngBoundsForCameraTarget(SARANAY_BOUNDS);
        map.setMinZoomPreference(15f);

        // Tap on map to drop pin
        map.setOnMapClickListener(latLng -> {
            selectedLatLng = latLng;

            if (pinMarker != null) pinMarker.remove();

            pinMarker = map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Your delivery pin")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            );

            tvInstruction.setText("📍 Pin dropped! Tap Confirm to save.");
            btnConfirm.setEnabled(true);
        });
    }

    private void saveLocation() {
        if (selectedLatLng == null) return;

        String uid = FirebaseHelper.getCurrentUid();
        if (uid == null) {
            Toast.makeText(this, "Session expired. Please log in again.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirm.setEnabled(false);
        btnConfirm.setText("Saving…");

        Map<String, Object> update = new HashMap<>();
        update.put("latitude",  selectedLatLng.latitude);
        update.put("longitude", selectedLatLng.longitude);

        FirebaseHelper.getDb()
                .collection("users")
                .document(uid)
                .update(update)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this,
                            "Location saved! Riders will now find you easily. ✅",
                            Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("Confirm Location");
                    Toast.makeText(this,
                            "Could not save location. Try again.",
                            Toast.LENGTH_SHORT).show();
                });
    }
}
