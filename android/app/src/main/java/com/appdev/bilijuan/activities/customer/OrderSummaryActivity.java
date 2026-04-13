package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.appdev.bilijuan.databinding.ActivityOrderSummaryBinding;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.models.Product;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.DeliveryUtils;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.ImageHelper;
import com.appdev.bilijuan.utils.NetworkHelper;

public class OrderSummaryActivity extends AppCompatActivity {

    private ActivityOrderSummaryBinding binding;
    private String productId;
    private String currentUid;
    private Product product;
    private User customer;
    private String selectedPayment = "COD";
    private int quantity = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderSummaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        productId  = getIntent().getStringExtra("productId");
        currentUid = FirebaseHelper.getCurrentUid();
        if (productId == null || currentUid == null) { finish(); return; }

        setupClickListeners();
        loadData();
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnCod.setOnClickListener(v -> {
            selectedPayment = "COD";
            binding.btnCod.setAlpha(1f);
            binding.btnGcash.setAlpha(0.4f);
        });
        binding.btnGcash.setOnClickListener(v -> {
            selectedPayment = "GCash";
            binding.btnGcash.setAlpha(1f);
            binding.btnCod.setAlpha(0.4f);
        });

        binding.btnMinus.setOnClickListener(v -> {
            if (quantity > 1) { quantity--; updateTotals(); }
        });
        binding.btnPlus.setOnClickListener(v -> {
            quantity++;
            updateTotals();
        });

        binding.btnPlaceOrder.setOnClickListener(v -> placeOrder());
    }

    private void loadData() {
        // Load product
        FirebaseHelper.getDb().collection("products").document(productId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { finish(); return; }
                    product = doc.toObject(Product.class);
                    product.setProductId(doc.getId());
                    bindProduct();
                });

        // Load customer
        FirebaseHelper.getDb().collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    customer = doc.toObject(User.class);
                    bindCustomer();
                    updateTotals();
                });
    }

    private void bindProduct() {
        binding.tvProductName.setText(product.getName());
        binding.tvProductPrice.setText(String.format("₱%.0f each", product.getPrice()));
        if (product.getImageBase64() != null && !product.getImageBase64().isEmpty()) {
            Bitmap bm = ImageHelper.base64ToBitmap(product.getImageBase64());
            if (bm != null) binding.ivProduct.setImageBitmap(bm);
        }
        updateTotals();
    }

    private void bindCustomer() {
        binding.tvCustomerName.setText(customer.getName());
        binding.tvCustomerAddress.setText(customer.getAddress());
        binding.tvCustomerPhone.setText(customer.getPhone());
    }

    private void updateTotals() {
        if (product == null || customer == null) return;
        binding.tvQuantity.setText(String.valueOf(quantity));

        // Fetch seller location to calculate delivery fee
        FirebaseHelper.getDb().collection("users").document(product.getSellerId()).get()
                .addOnSuccessListener(doc -> {
                    double sellerLat = 0, sellerLng = 0;
                    if (doc.exists()) {
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");
                        if (lat != null) sellerLat = lat;
                        if (lng != null) sellerLng = lng;
                    }
                    double dist = DeliveryUtils.haversineKm(
                            sellerLat, sellerLng,
                            customer.getLatitude(), customer.getLongitude());
                    double fee   = DeliveryUtils.calculateDeliveryFee(dist);
                    double total = (product.getPrice() * quantity) + fee;

                    binding.tvDistance.setText(DeliveryUtils.formatDistance(dist));
                    binding.tvDeliveryFee.setText(DeliveryUtils.formatFee(fee));
                    binding.tvSubtotal.setText(String.format("₱%.0f", product.getPrice() * quantity));
                    binding.tvTotal.setText(String.format("₱%.0f", total));

                    // Store for order placement
                    binding.btnPlaceOrder.setTag(new double[]{sellerLat, sellerLng, fee, dist, total});
                });
    }

    private void placeOrder() {
        if (product == null || customer == null) return;
        // Example usage in any Activity:
        if (!NetworkHelper.isOnline(this)) {
            NetworkHelper.showOfflineToast(this);
            return;
        }

        // ── Location check ──
        if (customer.getLatitude() == 0 && customer.getLongitude() == 0) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("No Location Set")
                    .setMessage("You haven't pinned your delivery location yet. Please set it before ordering so we can calculate the delivery fee and find you.")
                    .setPositiveButton("Set Location", (d, w) ->
                            startActivity(new Intent(this, PinLocationActivity.class)))
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        Object tag = binding.btnPlaceOrder.getTag();
        if (tag == null) {
            Toast.makeText(this, "Still loading, please wait.", Toast.LENGTH_SHORT).show();
            return;
        }
        double[] vals = (double[]) tag;
        double sellerLat = vals[0], sellerLng = vals[1],
                fee = vals[2], dist = vals[3];

        setLoading(true);

        // Get seller name
        FirebaseHelper.getDb().collection("users").document(product.getSellerId()).get()
                .addOnSuccessListener(doc -> {
                    String sellerName = doc.exists() ? doc.getString("name") : product.getSellerName();

                    Order order = new Order(
                            currentUid, customer.getName(), customer.getPhone(),
                            customer.getAddress(), customer.getLatitude(), customer.getLongitude(),
                            product.getSellerId(), sellerName, sellerLat, sellerLng,
                            productId, product.getName(), product.getImageBase64(),
                            quantity, product.getPrice(),
                            selectedPayment, fee, dist
                    );

                    FirebaseHelper.getDb().collection("orders").add(order)
                            .addOnSuccessListener(ref -> {
                                ref.update("orderId", ref.getId());
                                setLoading(false);
                                Intent intent = new Intent(this, OrderSuccessActivity.class);
                                intent.putExtra("orderId",     ref.getId());
                                intent.putExtra("productName", product.getName() + " × " + quantity);
                                intent.putExtra("total",       String.format("₱%.0f", order.getTotalAmount()));
                                intent.putExtra("storeName",   order.getSellerName());
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this, "Order failed. Try again.", Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void setLoading(boolean loading) {
        binding.btnPlaceOrder.setEnabled(!loading);
        binding.progressOrder.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}