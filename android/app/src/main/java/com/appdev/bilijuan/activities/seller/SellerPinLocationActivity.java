package com.appdev.bilijuan.activities.seller;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
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
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SellerPinLocationActivity extends AppCompatActivity {

    private static final double DEFAULT_LAT = 12.8797;
    private static final double DEFAULT_LNG = 121.7740;
    private static final double DEFAULT_ZOOM = 18.0;
    private static final int GPS_PERMISSION_REQUEST = 3001;

    private MapView mapView;
    private TextView tvCurrentAddress;
    private EditText etSearch;
    private ProgressBar progressSearch;
    private MaterialButton btnConfirm;
    private FloatingActionButton btnMyLocation;

    private FusedLocationProviderClient fusedClient;
    private Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_pin_location);

        mapView = findViewById(R.id.mapView);
        tvCurrentAddress = findViewById(R.id.tvCurrentAddress);
        etSearch = findViewById(R.id.etSearchLocation);
        progressSearch = findViewById(R.id.progressSearch);
        btnConfirm = findViewById(R.id.btnConfirmPin);
        btnMyLocation = findViewById(R.id.btnMyLocation);

        findViewById(R.id.btnSkipPin).setVisibility(View.GONE);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(this, Locale.getDefault());

        setupMap();
        setupSearch();

        btnConfirm.setOnClickListener(v -> saveLocation());
        btnMyLocation.setOnClickListener(v -> goToMyLocation());

        goToMyLocation();
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);

        IMapController controller = mapView.getController();
        controller.setZoom(DEFAULT_ZOOM);
        controller.setCenter(new GeoPoint(DEFAULT_LAT, DEFAULT_LNG));

        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                updateAddressFromCenter();
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                return false;
            }
        });
    }

    private void setupSearch() {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch(etSearch.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void performSearch(String query) {
        if (query.isEmpty()) return;
        
        progressSearch.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocationName(query + ", Philippines", 1);
                runOnUiThread(() -> {
                    progressSearch.setVisibility(View.GONE);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        GeoPoint gp = new GeoPoint(addr.getLatitude(), addr.getLongitude());
                        mapView.getController().animateTo(gp);
                    } else {
                        Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    progressSearch.setVisibility(View.GONE);
                    Toast.makeText(this, "Search error", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void updateAddressFromCenter() {
        GeoPoint center = (GeoPoint) mapView.getMapCenter();
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocation(center.getLatitude(), center.getLongitude(), 1);
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        String line = addresses.get(0).getAddressLine(0);
                        tvCurrentAddress.setText(line);
                    } else {
                        tvCurrentAddress.setText("Unknown location");
                    }
                });
            } catch (IOException ignored) {}
        }).start();
    }

    private void goToMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            GeoPoint gp = new GeoPoint(location.getLatitude(), location.getLongitude());
                            mapView.getController().animateTo(gp);
                        }
                    });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GPS_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == GPS_PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            goToMyLocation();
        }
    }

    private void saveLocation() {
        GeoPoint center = (GeoPoint) mapView.getMapCenter();
        String uid = FirebaseHelper.getCurrentUid();
        if (uid == null) return;

        btnConfirm.setEnabled(false);
        btnConfirm.setText("Saving...");

        Map<String, Object> update = new HashMap<>();
        update.put("latitude", center.getLatitude());
        update.put("longitude", center.getLongitude());
        update.put("address", tvCurrentAddress.getText().toString());

        FirebaseHelper.getDb().collection("users").document(uid).update(update)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Store location saved! ✅", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("Confirm Pin");
                    Toast.makeText(this, "Failed to save location", Toast.LENGTH_SHORT).show();
                });
    }

    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDetach(); }
}