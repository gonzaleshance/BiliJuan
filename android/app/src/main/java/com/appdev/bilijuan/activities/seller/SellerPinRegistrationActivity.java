package com.appdev.bilijuan.activities.seller;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.appdev.bilijuan.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * SellerPinRegistrationActivity
 *
 * Updated with a fixed center pointer and Premium Bottom Sheet confirmations.
 */
public class SellerPinRegistrationActivity extends AppCompatActivity {

    private static final double DEFAULT_LAT  = 14.7548685;
    private static final double DEFAULT_LNG  = 121.0258531;
    private static final double DEFAULT_ZOOM = 17.0;
    private static final int    PERMISSION_REQUEST = 5001;

    private MapView        mapView;
    private GeoPoint       selectedPoint;
    private String         selectedAddress = "";

    private TextView       tvInstruction;
    private TextView       tvAddress;
    private EditText       etSearch;
    private ProgressBar    progressSearch;
    private MaterialButton btnConfirm;
    private MaterialButton btnVerify;

    private FusedLocationProviderClient fusedClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable reverseGeocodeRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_seller_pin_registration);

        tvInstruction = findViewById(R.id.tvInstruction);
        tvAddress     = findViewById(R.id.tvAddress);
        btnConfirm    = findViewById(R.id.btnConfirmPin);
        btnVerify     = findViewById(R.id.btnVerifyExternal);
        etSearch      = findViewById(R.id.etSearch);
        progressSearch = findViewById(R.id.progressSearch);
        mapView       = findViewById(R.id.mapView);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        setupMap();
        setupSearch();

        btnConfirm.setEnabled(true);
        btnConfirm.setAlpha(1.0f);
        btnConfirm.setOnClickListener(v -> showFinalConfirmationSheet());
        
        btnVerify.setOnClickListener(v -> showExternalVerifySheet());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnMyLocation).setOnClickListener(v -> goToMyLocation());

        goToMyLocation();
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(
                org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);

        IMapController controller = mapView.getController();
        controller.setZoom(DEFAULT_ZOOM);
        controller.setCenter(new GeoPoint(DEFAULT_LAT, DEFAULT_LNG));

        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                onMapMoved();
                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                onMapMoved();
                return true;
            }
        });

        selectedPoint = (GeoPoint) mapView.getMapCenter();
    }

    private void onMapMoved() {
        selectedPoint = (GeoPoint) mapView.getMapCenter();
        
        if (reverseGeocodeRunnable != null) {
            mainHandler.removeCallbacks(reverseGeocodeRunnable);
        }
        reverseGeocodeRunnable = () -> reverseGeocode(selectedPoint);
        mainHandler.postDelayed(reverseGeocodeRunnable, 800);
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
        if (query == null || query.trim().isEmpty()) return;

        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        progressSearch.setVisibility(View.VISIBLE);
        
        GeoPoint mapCenter = (GeoPoint) mapView.getMapCenter();
        double biasLat = mapCenter.getLatitude();
        double biasLng = mapCenter.getLongitude();

        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = null;

                try {
                    addresses = geocoder.getFromLocationName(query, 1, 
                            biasLat - 0.2, biasLng - 0.2, 
                            biasLat + 0.2, biasLng + 0.2);
                } catch (Exception ignored) {}

                if (addresses == null || addresses.isEmpty()) {
                    addresses = geocoder.getFromLocationName(query + ", Philippines", 1);
                }

                if (addresses == null || addresses.isEmpty()) {
                    addresses = geocoder.getFromLocationName(query, 1);
                }
                
                final List<Address> finalResults = addresses;
                mainHandler.post(() -> {
                    progressSearch.setVisibility(View.GONE);
                    if (finalResults != null && !finalResults.isEmpty()) {
                        Address addr = finalResults.get(0);
                        GeoPoint point = new GeoPoint(addr.getLatitude(), addr.getLongitude());
                        mapView.getController().animateTo(point);
                        mapView.getController().setZoom(18.5); 
                        etSearch.clearFocus();
                    } else {
                        Toast.makeText(this, "Could not find '" + query + "'", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                mainHandler.post(() -> {
                    progressSearch.setVisibility(View.GONE);
                    Toast.makeText(this, "Network error during search", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void reverseGeocode(GeoPoint point) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocation(
                        point.getLatitude(), point.getLongitude(), 1);
                if (results != null && !results.isEmpty()) {
                    String addr = results.get(0).getAddressLine(0);
                    selectedAddress = addr;
                    runOnUiThread(() -> tvAddress.setText(addr));
                }
            } catch (IOException ignored) {}
        }).start();
    }

    private void showExternalVerifySheet() {
        if (selectedPoint == null) return;

        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetStyle);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_google_maps_redirection, null);
        sheet.setContentView(v);

        v.findViewById(R.id.btnOpenMaps).setOnClickListener(view -> {
            sheet.dismiss();
            double lat = selectedPoint.getLatitude();
            double lng = selectedPoint.getLongitude();
            
            // To avoid the "0,0 bug" and ensure a pin drops at the center:
            // geo:lat,lng?q=lat,lng(Label) is the most reliable format.
            String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f(%s)", 
                    lat, lng, lat, lng, Uri.encode("Verify Store Location"));
            
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps");
            
            try {
                startActivity(mapIntent);
            } catch (Exception e) {
                // Fallback: Web browser
                Uri webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng);
                startActivity(new Intent(Intent.ACTION_VIEW, webUri));
            }
        });

        v.findViewById(R.id.btnCancel).setOnClickListener(view -> sheet.dismiss());
        sheet.show();
    }

    private void goToMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST);
            return;
        }
        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                            mapView.getController().animateTo(point);
                        }
                    });
        } catch (SecurityException ignored) {}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            goToMyLocation();
        }
    }

    private void showFinalConfirmationSheet() {
        if (selectedPoint == null) return;

        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetStyle);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_repin_confirm, null);
        sheet.setContentView(v);

        v.findViewById(R.id.btnConfirm).setOnClickListener(view -> {
            sheet.dismiss();
            Intent result = new Intent();
            result.putExtra("lat", selectedPoint.getLatitude());
            result.putExtra("lng", selectedPoint.getLongitude());
            result.putExtra("address", selectedAddress);
            setResult(RESULT_OK, result);
            finish();
        });

        v.findViewById(R.id.btnCancel).setOnClickListener(view -> sheet.dismiss());
        sheet.show();
    }

    @Override protected void onResume()  { super.onResume();  mapView.onResume(); }
    @Override protected void onPause()   { super.onPause();   mapView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDetach(); }
}