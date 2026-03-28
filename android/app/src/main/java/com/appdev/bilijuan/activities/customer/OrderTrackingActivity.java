package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

public class OrderTrackingActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Intent extras — pass these when launching this activity
    public static final String EXTRA_ORDER_ID     = "order_id";
    public static final String EXTRA_SELLER_NAME  = "seller_name";
    public static final String EXTRA_SELLER_PHONE = "seller_phone";

    private static final double SARANAY_LAT = 14.7548685;
    private static final double SARANAY_LNG = 121.0258531;

    private GoogleMap           map;
    private Marker              riderMarker;
    private Marker              customerMarker;
    private Marker              sellerMarker;
    private ListenerRegistration riderListener;

    private String orderId;
    private String sellerName;
    private String sellerPhone;

    // UI
    private TextView tvOrderStatus;
    private TextView tvSellerName;
    private TextView tvSellerPhone;
    private View     btnCallSeller;
    private View     btnSmsCustomer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_tracking);

        orderId     = getIntent().getStringExtra(EXTRA_ORDER_ID);
        sellerName  = getIntent().getStringExtra(EXTRA_SELLER_NAME);
        sellerPhone = getIntent().getStringExtra(EXTRA_SELLER_PHONE);

        tvOrderStatus   = findViewById(R.id.tvOrderStatus);
        tvSellerName    = findViewById(R.id.tvSellerName);
        tvSellerPhone   = findViewById(R.id.tvSellerPhone);
        btnCallSeller   = findViewById(R.id.btnCallSeller);
        btnSmsCustomer  = findViewById(R.id.btnSmsCustomer);

        if (sellerName  != null) tvSellerName.setText(sellerName);
        if (sellerPhone != null) tvSellerPhone.setText(sellerPhone);

        // Call seller button
        btnCallSeller.setOnClickListener(v -> {
            if (sellerPhone != null && !sellerPhone.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + sellerPhone)));
            } else {
                Toast.makeText(this, "No seller phone number available.", Toast.LENGTH_SHORT).show();
            }
        });

        // SMS customer button (used by rider — phone number from order)
        btnSmsCustomer.setOnClickListener(v -> openSmsToCustomer());

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapTracking);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(SARANAY_LAT, SARANAY_LNG), 16f));
        map.getUiSettings().setZoomControlsEnabled(true);

        loadOrderAndTrack();
    }

    // ── Load order data + set up markers ─────────────────────────────────────

    private void loadOrderAndTrack() {
        if (orderId == null) return;

        FirebaseHelper.getDb()
                .collection("orders")
                .document(orderId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    placeStaticMarkers(doc);
                    listenToRiderLocation(doc);
                    updateStatusUI(doc.getString("status"));
                });
    }

    /**
     * Places a pin for the customer's delivery address and
     * the seller's location — both fixed (not live).
     */
    private void placeStaticMarkers(DocumentSnapshot doc) {
        // Customer marker
        double custLat = doc.getDouble("customerLat") != null ? doc.getDouble("customerLat") : 0;
        double custLng = doc.getDouble("customerLng") != null ? doc.getDouble("customerLng") : 0;
        if (custLat != 0) {
            customerMarker = map.addMarker(new MarkerOptions()
                    .position(new LatLng(custLat, custLng))
                    .title("📦 Delivery Address")
                    .snippet(doc.getString("customerAddress"))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }

        // Seller marker
        double sellLat = doc.getDouble("sellerLat") != null ? doc.getDouble("sellerLat") : 0;
        double sellLng = doc.getDouble("sellerLng") != null ? doc.getDouble("sellerLng") : 0;
        if (sellLat != 0) {
            sellerMarker = map.addMarker(new MarkerOptions()
                    .position(new LatLng(sellLat, sellLng))
                    .title("🍳 " + sellerName)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }
    }

    /**
     * Listens in real-time to the rider's GPS updates
     * stored in Firestore under orders/{orderId}/riderLat+riderLng.
     */
    private void listenToRiderLocation(DocumentSnapshot initialDoc) {
        riderListener = FirebaseHelper.getDb()
                .collection("orders")
                .document(orderId)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null || !snap.exists()) return;

                    Double lat = snap.getDouble("riderLat");
                    Double lng = snap.getDouble("riderLng");

                    updateStatusUI(snap.getString("status"));

                    if (lat == null || lng == null || lat == 0) return;

                    LatLng riderPos = new LatLng(lat, lng);

                    if (riderMarker == null) {
                        riderMarker = map.addMarker(new MarkerOptions()
                                .position(riderPos)
                                .title("🛵 Rider")
                                .icon(BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_ORANGE)));
                        // Fly to rider on first appearance
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(riderPos, 17f));
                    } else {
                        // Smoothly update the marker position
                        riderMarker.setPosition(riderPos);
                    }
                });
    }

    private void updateStatusUI(String status) {
        if (status == null) return;
        String label;
        switch (status) {
            case "confirmed": label = "✅ Order Confirmed — Preparing your food"; break;
            case "picked_up": label = "🛵 Rider picked up your order"; break;
            case "on_the_way": label = "🛵 On the way — Watch the map!"; break;
            case "delivered": label = "🎉 Delivered! Enjoy your meal."; break;
            default:          label = "⏳ Waiting for confirmation…"; break;
        }
        tvOrderStatus.setText(label);
    }

    // ── SMS helper ────────────────────────────────────────────────────────────

    private void openSmsToCustomer() {
        if (orderId == null) return;
        FirebaseHelper.getDb().collection("orders").document(orderId).get()
                .addOnSuccessListener(doc -> {
                    String phone = doc.getString("customerPhone");
                    if (phone != null && !phone.isEmpty()) {
                        Intent sms = new Intent(Intent.ACTION_SENDTO,
                                Uri.parse("smsto:" + phone));
                        sms.putExtra("sms_body", "Hi! I'm your BiliJuan rider. I'm on my way.");
                        startActivity(sms);
                    } else {
                        Toast.makeText(this, "No customer phone on record.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (riderListener != null) riderListener.remove();
    }
}
