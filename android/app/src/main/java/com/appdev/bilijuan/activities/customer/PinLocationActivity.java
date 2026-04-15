package com.appdev.bilijuan.activities.customer;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.adapters.AddressSuggestionAdapter;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
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
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PinLocationActivity extends AppCompatActivity {

    private static final double DEFAULT_LAT = 14.5995; 
    private static final double DEFAULT_LNG = 120.9842;
    private static final double DEFAULT_ZOOM = 18.0;
    private static final int GPS_PERMISSION_REQUEST = 3001;
    private static final int REQUEST_CHECK_SETTINGS = 3002;

    private MapView mapView;
    private TextView tvCurrentAddress;
    private EditText etSearch;
    private ProgressBar progressSearch;
    private MaterialButton btnConfirm;
    private View cardSuggestions;
    private RecyclerView rvSuggestions;
    private AddressSuggestionAdapter suggestionAdapter;
    private final List<Address> suggestionList = new ArrayList<>();
    
    private String productId;
    private boolean fromCart = false;
    private FusedLocationProviderClient fusedClient;
    private Geocoder geocoder;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private Runnable geocodingRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_pin_location);

        productId = getIntent().getStringExtra("productId");
        fromCart = getIntent().getBooleanExtra("fromCart", false);

        mapView = findViewById(R.id.mapView);
        tvCurrentAddress = findViewById(R.id.tvCurrentAddress);
        etSearch = findViewById(R.id.etSearchLocation);
        progressSearch = findViewById(R.id.progressSearch);
        btnConfirm = findViewById(R.id.btnConfirmPin);
        cardSuggestions = findViewById(R.id.cardSuggestions);
        rvSuggestions = findViewById(R.id.rvSuggestions);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(this, Locale.getDefault());

        setupMap();
        setupSearch();
        setupSuggestions();

        btnConfirm.setOnClickListener(v -> saveLocationAndProceed());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSkipPin).setOnClickListener(v -> finish());
        findViewById(R.id.btnMyLocation).setOnClickListener(v -> checkSettingsAndGetLocation());
        findViewById(R.id.btnVerifyExternal).setOnClickListener(v -> showExternalVerifySheet());

        checkSettingsAndGetLocation();
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);

        RotationGestureOverlay rotationGestureOverlay = new RotationGestureOverlay(mapView);
        rotationGestureOverlay.setEnabled(true);
        mapView.getOverlays().add(rotationGestureOverlay);

        CompassOverlay compassOverlay = new CompassOverlay(this, new InternalCompassOrientationProvider(this), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);

        IMapController controller = mapView.getController();
        controller.setZoom(DEFAULT_ZOOM);
        controller.setCenter(new GeoPoint(DEFAULT_LAT, DEFAULT_LNG));

        geocodingRunnable = this::updateAddressFromCenter;

        mapView.addMapListener(new MapListener() {
            @Override public boolean onScroll(ScrollEvent event) { 
                searchHandler.removeCallbacks(geocodingRunnable);
                tvCurrentAddress.setText("Detecting location...");
                searchHandler.postDelayed(geocodingRunnable, 800);
                return false; 
            }
            @Override public boolean onZoom(ZoomEvent event) { 
                searchHandler.removeCallbacks(geocodingRunnable);
                searchHandler.postDelayed(geocodingRunnable, 800);
                return false; 
            }
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacks(searchRunnable);
                if (s.length() > 2) {
                    searchRunnable = () -> fetchSuggestions(s.toString());
                    searchHandler.postDelayed(searchRunnable, 600); 
                } else {
                    cardSuggestions.setVisibility(View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch(etSearch.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void setupSuggestions() {
        suggestionAdapter = new AddressSuggestionAdapter(suggestionList, addr -> {
            GeoPoint gp = new GeoPoint(addr.getLatitude(), addr.getLongitude());
            mapView.getController().animateTo(gp);
            cardSuggestions.setVisibility(View.GONE);
            
            String displayName = addr.getFeatureName();
            if (displayName == null || displayName.isEmpty()) displayName = addr.getAddressLine(0);
            etSearch.setText(displayName);
            etSearch.setSelection(etSearch.getText().length());
            
            updateAddressFromCenter();
        });
        rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        rvSuggestions.setAdapter(suggestionAdapter);
    }

    private void fetchSuggestions(String query) {
        new Thread(() -> {
            try {
                GeoPoint center = (GeoPoint) mapView.getMapCenter();
                List<Address> results = geocoder.getFromLocationName(query, 15,
                        center.getLatitude() - 0.5, center.getLongitude() - 0.5,
                        center.getLatitude() + 0.5, center.getLongitude() + 0.5);
                
                if (results == null || results.isEmpty()) {
                    results = geocoder.getFromLocationName(query + ", Philippines", 15);
                }

                List<Address> filteredResults = new ArrayList<>();
                if (results != null) {
                    for (Address a : results) {
                        boolean isGeneric = isGenericResult(a, query);
                        if (!isGeneric) filteredResults.add(a);
                    }
                }

                runOnUiThread(() -> {
                    suggestionList.clear();
                    suggestionList.addAll(filteredResults);
                    suggestionAdapter.notifyDataSetChanged();
                    cardSuggestions.setVisibility(suggestionList.isEmpty() ? View.GONE : View.VISIBLE);
                });
            } catch (IOException ignored) {}
        }).start();
    }

    private void performSearch(String query) {
        if (query.isEmpty()) return;
        progressSearch.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocationName(query + ", Philippines", 10);
                runOnUiThread(() -> {
                    progressSearch.setVisibility(View.GONE);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address bestMatch = null;
                        for (Address a : addresses) {
                            if (!isGenericResult(a, query)) { bestMatch = a; break; }
                        }
                        if (bestMatch == null) bestMatch = addresses.get(0);

                        GeoPoint gp = new GeoPoint(bestMatch.getLatitude(), bestMatch.getLongitude());
                        mapView.getController().animateTo(gp);
                        cardSuggestions.setVisibility(View.GONE);
                        
                        String foundName = bestMatch.getFeatureName();
                        if (foundName != null) {
                            etSearch.setText(foundName);
                            etSearch.setSelection(etSearch.getText().length());
                        }
                        updateAddressFromCenter();
                    } else {
                        Toast.makeText(this, "Place not found. Be more specific.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> progressSearch.setVisibility(View.GONE));
            }
        }).start();
    }

    private boolean isGenericResult(Address a, String query) {
        String feature = a.getFeatureName();
        if (feature == null) return true;
        boolean matchesCity = feature.equalsIgnoreCase(a.getLocality()) || feature.equalsIgnoreCase(a.getSubAdminArea()) || feature.equalsIgnoreCase(a.getAdminArea());
        if (matchesCity) return !query.toLowerCase().contains(feature.toLowerCase());
        return false;
    }

    private void updateAddressFromCenter() {
        GeoPoint center = (GeoPoint) mapView.getMapCenter();
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocation(center.getLatitude(), center.getLongitude(), 1);
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        String addr = addresses.get(0).getAddressLine(0);
                        tvCurrentAddress.setText(addr != null ? addr : "Unknown Location");
                    } else {
                        tvCurrentAddress.setText("Location not found");
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> tvCurrentAddress.setText("Geocoder unavailable"));
            }
        }).start();
    }

    private void showExternalVerifySheet() {
        GeoPoint center = (GeoPoint) mapView.getMapCenter();
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetStyle);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_google_maps_redirection, null);
        sheet.setContentView(v);

        v.findViewById(R.id.btnOpenMaps).setOnClickListener(view -> {
            sheet.dismiss();
            double lat = center.getLatitude();
            double lng = center.getLongitude();
            
            // Format: geo:lat,lng?q=lat,lng(Label)
            String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f(%s)", 
                    lat, lng, lat, lng, Uri.encode("Confirm My Location"));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps");
            
            try {
                startActivity(mapIntent);
            } catch (ActivityNotFoundException e) {
                Uri webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng);
                startActivity(new Intent(Intent.ACTION_VIEW, webUri));
            }
        });

        v.findViewById(R.id.btnCancel).setOnClickListener(view -> sheet.dismiss());
        sheet.show();
    }

    private void checkSettingsAndGetLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GPS_PERMISSION_REQUEST);
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true); 
        
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> goToMyLocation());

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(PinLocationActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    Log.e("PinLocation", "Error resolution", sendEx);
                }
            } else {
                goToMyLocation();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) goToMyLocation();
            else Toast.makeText(this, "Enable GPS for precise delivery pinning", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        GeoPoint gp = new GeoPoint(location.getLatitude(), location.getLongitude());
                        mapView.getController().animateTo(gp);
                        updateAddressFromCenter();
                    } else {
                        fusedClient.getLastLocation().addOnSuccessListener(lastLoc -> {
                            if (lastLoc != null) {
                                GeoPoint gp = new GeoPoint(lastLoc.getLatitude(), lastLoc.getLongitude());
                                mapView.getController().animateTo(gp);
                                updateAddressFromCenter();
                            }
                        });
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == GPS_PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkSettingsAndGetLocation();
        }
    }

    private void saveLocationAndProceed() {
        GeoPoint center = (GeoPoint) mapView.getMapCenter();
        String uid = FirebaseHelper.getCurrentUid();
        if (uid == null) return;

        btnConfirm.setEnabled(false);
        btnConfirm.setText("Setting location...");

        Map<String, Object> update = new HashMap<>();
        String fullAddress = tvCurrentAddress.getText().toString();
        
        if (fullAddress.equals("Detecting location...") || fullAddress.equals("Geocoder unavailable")) {
            Toast.makeText(this, "Please wait for address to be detected", Toast.LENGTH_SHORT).show();
            btnConfirm.setEnabled(true);
            btnConfirm.setText("Confirm Pin");
            return;
        }

        update.put("latitude", center.getLatitude());
        update.put("longitude", center.getLongitude());
        update.put("address", fullAddress);

        FirebaseHelper.getDb().collection("users").document(uid).update(update)
                .addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent(this, OrderSummaryActivity.class);
                    intent.putExtra("productId", productId);
                    intent.putExtra("fromCart", fromCart);
                    intent.putExtra("pinnedAddress", fullAddress);
                    intent.putExtra("pinnedLat", center.getLatitude());
                    intent.putExtra("pinnedLng", center.getLongitude());
                    startActivity(intent);
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
    @Override protected void onDestroy() { super.onDestroy(); if (mapView != null) mapView.onDetach(); }
}