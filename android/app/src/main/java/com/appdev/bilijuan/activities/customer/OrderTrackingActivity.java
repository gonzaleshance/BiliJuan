package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class OrderTrackingActivity extends AppCompatActivity {

    // Intent extras — pass these when starting this activity
    public static final String EXTRA_ORDER_ID     = "order_id";
    public static final String EXTRA_SELLER_NAME  = "seller_name";
    public static final String EXTRA_SELLER_PHONE = "seller_phone";

    // Saranay default center
    private static final double SARANAY_LAT  = 14.7548685;
    private static final double SARANAY_LNG  = 121.0258531;
    private static final double DEFAULT_ZOOM = 16.0;

    // Map & markers
    private MapView mapView;
    private Marker  riderMarker;
    private Marker  customerMarker;
    private Marker  sellerMarker;

    // Firestore real-time listener
    private ListenerRegistration riderListener;

    // Data from intent
    private String orderId;
    private String sellerName;
    private String sellerPhone;

    // UI
    private TextView    tvOrderStatus;
    private TextView    tvSellerName;
    private TextView    tvSellerPhone;
    private ImageButton btnCallSeller;
    private ImageButton btnSmsCustomer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_order_tracking);

        // Get data passed from previous screen
        orderId     = getIntent().getStringExtra(EXTRA_ORDER_ID);
        sellerName  = getIntent().getStringExtra(EXTRA_SELLER_NAME);
        sellerPhone = getIntent().getStringExtra(EXTRA_SELLER_PHONE);

        // Bind views
        tvOrderStatus  = findViewById(R.id.tvOrderStatus);
        tvSellerName   = findViewById(R.id.tvSellerName);
        tvSellerPhone  = findViewById(R.id.tvSellerPhone);
        btnCallSeller  = findViewById(R.id.btnCallSeller);
        btnSmsCustomer = findViewById(R.id.btnSmsCustomer);
        mapView        = findViewById(R.id.mapView);

        if (sellerName  != null) tvSellerName.setText(sellerName);
        if (sellerPhone != null) tvSellerPhone.setText(sellerPhone);

        // Call button — opens phone dialer
        btnCallSeller.setOnClickListener(v -> {
            if (sellerPhone != null && !sellerPhone.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + sellerPhone)));
            } else {
                Toast.makeText(this,
                        "No seller phone number available.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // SMS button — opens SMS app to message customer
        btnSmsCustomer.setOnClickListener(v -> openSmsToCustomer());

        setupMap();
        loadOrderAndTrack();
    }

    // ── Map setup ─────────────────────────────────────────────────────────────

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(
                org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT
        );

        IMapController controller = mapView.getController();
        controller.setZoom(DEFAULT_ZOOM);
        controller.setCenter(new GeoPoint(SARANAY_LAT, SARANAY_LNG));
    }

    // ── Load order data ───────────────────────────────────────────────────────

    private void loadOrderAndTrack() {
        if (orderId == null) return;

        FirebaseHelper.getDb()
                .collection("orders")
                .document(orderId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    placeStaticMarkers(doc);
                    updateStatusUI(doc.getString("status"));
                    listenToRiderLocation();
                });
    }

    /**
     * Places fixed markers for the customer delivery pin
     * and the seller's registered location.
     */
    private void placeStaticMarkers(DocumentSnapshot doc) {
        android.graphics.drawable.Drawable defaultIcon =
                getResources().getDrawable(
                        org.osmdroid.library.R.drawable.marker_default, getTheme()
                );

        // Customer marker
        Double custLat = doc.getDouble("customerLat");
        Double custLng = doc.getDouble("customerLng");
        if (custLat != null && custLat != 0) {
            customerMarker = new Marker(mapView);
            customerMarker.setPosition(new GeoPoint(custLat, custLng));
            customerMarker.setTitle("📦 Delivery Address");
            customerMarker.setSnippet(doc.getString("customerAddress"));
            customerMarker.setIcon(defaultIcon);
            customerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(customerMarker);
        }

        // Seller marker
        Double sellLat = doc.getDouble("sellerLat");
        Double sellLng = doc.getDouble("sellerLng");
        if (sellLat != null && sellLat != 0) {
            sellerMarker = new Marker(mapView);
            sellerMarker.setPosition(new GeoPoint(sellLat, sellLng));
            sellerMarker.setTitle("🍳 " + (sellerName != null ? sellerName : "Seller"));
            sellerMarker.setIcon(defaultIcon);
            sellerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(sellerMarker);
        }

        mapView.invalidate();
    }

    /**
     * Listens in real-time to riderLat + riderLng in Firestore.
     * The seller updates these from their Seller Dashboard
     * while they are delivering. The marker moves on the map live.
     */
    private void listenToRiderLocation() {
        if (orderId == null) return;

        riderListener = FirebaseHelper.getDb()
                .collection("orders")
                .document(orderId)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null || !snap.exists()) return;

                    updateStatusUI(snap.getString("status"));

                    Double lat = snap.getDouble("riderLat");
                    Double lng = snap.getDouble("riderLng");
                    if (lat == null || lng == null || lat == 0) return;

                    GeoPoint riderPos = new GeoPoint(lat, lng);

                    if (riderMarker == null) {
                        // First appearance — create marker
                        riderMarker = new Marker(mapView);
                        riderMarker.setTitle("🛵 Rider on the way");
                        riderMarker.setIcon(
                                getResources().getDrawable(
                                        org.osmdroid.library.R.drawable.marker_default,
                                        getTheme()
                                )
                        );
                        riderMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        mapView.getOverlays().add(riderMarker);
                        // Fly camera to rider
                        mapView.getController().animateTo(riderPos);
                    }

                    riderMarker.setPosition(riderPos);
                    mapView.invalidate();
                });
    }

    // ── Order status label ────────────────────────────────────────────────────

    private void updateStatusUI(String status) {
        if (status == null) return;
        String label;
        switch (status) {
            case "confirmed":  label = "✅ Order Confirmed — Preparing your food"; break;
            case "picked_up":  label = "🛵 Rider picked up your order";            break;
            case "on_the_way": label = "🛵 On the way — Watch the map!";           break;
            case "delivered":  label = "🎉 Delivered! Enjoy your meal.";            break;
            default:           label = "⏳ Waiting for confirmation…";              break;
        }
        tvOrderStatus.setText(label);
    }

    // ── SMS to customer ───────────────────────────────────────────────────────

    private void openSmsToCustomer() {
        if (orderId == null) return;
        FirebaseHelper.getDb()
                .collection("orders")
                .document(orderId)
                .get()
                .addOnSuccessListener(doc -> {
                    String phone = doc.getString("customerPhone");
                    if (phone != null && !phone.isEmpty()) {
                        Intent sms = new Intent(Intent.ACTION_SENDTO,
                                Uri.parse("smsto:" + phone));
                        sms.putExtra("sms_body",
                                "Hi! I'm your BiliJuan rider. I'm on my way! 🛵");
                        startActivity(sms);
                    } else {
                        Toast.makeText(this,
                                "No customer phone on record.",
                                Toast.LENGTH_SHORT).show();
                    }
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
        if (riderListener != null) riderListener.remove();
        mapView.onDetach();
    }
}
