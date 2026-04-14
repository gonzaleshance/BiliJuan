package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.databinding.ActivityOrderSummaryBinding;
import com.appdev.bilijuan.models.CartItem;
import com.appdev.bilijuan.models.Order;
import com.appdev.bilijuan.models.Product;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.CartHelper;
import com.appdev.bilijuan.utils.DeliveryUtils;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.appdev.bilijuan.utils.ImageHelper;
import com.appdev.bilijuan.utils.NetworkHelper;
import com.appdev.bilijuan.utils.NotificationHelper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrderSummaryActivity extends AppCompatActivity {

    private ActivityOrderSummaryBinding binding;
    private String productId;
    private boolean fromCart = false;
    private String currentUid;
    private Product singleProduct;
    private List<CartItem> cartItems = new ArrayList<>();
    private User customer;

    private String pinnedAddress;
    private double pinnedLat;
    private double pinnedLng;

    private double sellerLat   = 0;
    private double sellerLng   = 0;
    private String sellerName  = "";
    private double deliveryFee = DeliveryUtils.BASE_FEE; 
    private double distanceKm  = 0;

    private int singleQuantity = 1;

    private boolean dataLoaded     = false;
    private boolean customerLoaded = false;
    private boolean sellerLoaded   = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderSummaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        productId     = getIntent().getStringExtra("productId");
        fromCart      = getIntent().getBooleanExtra("fromCart", false);
        pinnedAddress = getIntent().getStringExtra("pinnedAddress");
        pinnedLat     = getIntent().getDoubleExtra("pinnedLat", 0);
        pinnedLng     = getIntent().getDoubleExtra("pinnedLng", 0);
        singleQuantity = getIntent().getIntExtra("quantity", 1);
        
        currentUid    = FirebaseHelper.getCurrentUid();

        if (currentUid == null) {
            finish();
            return;
        }

        setupClickListeners();
        
        if (fromCart) {
            loadCartData();
        } else {
            loadSingleProduct();
        }
        
        loadCustomer();
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnPlaceOrder.setOnClickListener(v -> placeOrder());
    }

    private void loadCartData() {
        cartItems = CartHelper.getCart(this);
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // All items are from the same seller
        String sellerId = cartItems.get(0).getSellerId();
        loadSellerLocation(sellerId);
        dataLoaded = true;
        tryRenderTotals();
    }

    private void loadSingleProduct() {
        if (productId == null) { finish(); return; }
        FirebaseHelper.getDb().collection("products").document(productId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { 
                        Toast.makeText(this, "Product no longer available", Toast.LENGTH_SHORT).show();
                        finish(); 
                        return; 
                    }
                    singleProduct = doc.toObject(Product.class);
                    singleProduct.setProductId(doc.getId());
                    
                    loadSellerLocation(singleProduct.getSellerId());
                    dataLoaded = true;
                    tryRenderTotals();
                });
    }

    private void loadCustomer() {
        FirebaseHelper.getDb().collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    customer = doc.toObject(User.class);
                    customerLoaded = true;
                    binding.tvCustomerAddress.setText(pinnedAddress != null ? pinnedAddress : customer.getAddress());
                    tryRenderTotals();
                });
    }

    private void loadSellerLocation(String sellerId) {
        FirebaseHelper.getDb().collection("users").document(sellerId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Double lat  = doc.getDouble("latitude");
                        Double lng  = doc.getDouble("longitude");
                        String name = doc.getString("name");

                        if (lat != null && lng != null && lat != 0 && lng != 0) {
                            sellerLat = lat;
                            sellerLng = lng;
                        }
                        if (name != null) sellerName = name;
                    }

                    calculateDeliveryFee();
                    sellerLoaded = true;
                    tryRenderTotals();
                })
                .addOnFailureListener(e -> {
                    deliveryFee = DeliveryUtils.BASE_FEE;
                    distanceKm  = 0;
                    sellerLoaded = true;
                    tryRenderTotals();
                });
    }

    private void calculateDeliveryFee() {
        boolean sellerLocationValid   = sellerLat != 0 && sellerLng != 0;
        boolean customerLocationValid = pinnedLat != 0 && pinnedLng != 0;

        if (sellerLocationValid && customerLocationValid) {
            double dist = DeliveryUtils.haversineKm(sellerLat, sellerLng, pinnedLat, pinnedLng);
            if (dist <= 50) {
                distanceKm  = dist;
                deliveryFee = DeliveryUtils.calculateDeliveryFee(dist);
            } else {
                distanceKm  = 0;
                deliveryFee = DeliveryUtils.BASE_FEE;
            }
        } else {
            distanceKm  = 0;
            deliveryFee = DeliveryUtils.BASE_FEE;
        }
    }

    private void tryRenderTotals() {
        if (dataLoaded && customerLoaded && sellerLoaded) {
            renderTotals();
        }
    }

    private void renderTotals() {
        binding.layoutItems.removeAllViews();
        double subtotal = 0;

        if (fromCart) {
            for (CartItem item : cartItems) {
                addItemToLayout(item.getProductName(), item.getQuantity(), item.getPrice(), item.getImageBase64());
                subtotal += (item.getPrice() * item.getQuantity());
            }
        } else if (singleProduct != null) {
            addItemToLayout(singleProduct.getName(), singleQuantity, singleProduct.getPrice(), singleProduct.getImageBase64());
            subtotal = singleProduct.getPrice() * singleQuantity;
        }

        double total = subtotal + deliveryFee;

        binding.tvSubtotal.setText(String.format("₱%.0f", subtotal));
        binding.tvDeliveryFee.setText(DeliveryUtils.formatFee(deliveryFee));
        binding.tvTotal.setText(String.format("₱%.0f", total));

        if (distanceKm > 0) {
            binding.tvDistance.setText(DeliveryUtils.formatDistance(distanceKm));
            binding.tvDistance.setVisibility(View.VISIBLE);
        } else {
            binding.tvDistance.setVisibility(View.GONE);
        }
    }

    private void addItemToLayout(String name, int qty, double price, String imgBase64) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_order_summary_product, binding.layoutItems, false);
        
        ImageView iv = view.findViewById(R.id.ivProduct);
        TextView tvName = view.findViewById(R.id.tvProductName);
        TextView tvQty = view.findViewById(R.id.tvProductQty);
        TextView tvPrice = view.findViewById(R.id.tvProductPrice);

        tvName.setText(name);
        tvQty.setText("x" + qty);
        tvPrice.setText(String.format("₱%.0f", price * qty));

        if (imgBase64 != null && !imgBase64.isEmpty()) {
            Bitmap bm = ImageHelper.base64ToBitmap(imgBase64);
            if (bm != null) iv.setImageBitmap(bm);
        }

        binding.layoutItems.addView(view);
    }

    private void placeOrder() {
        if (customer == null || (!fromCart && singleProduct == null) || (fromCart && cartItems.isEmpty())) {
            Toast.makeText(this, "Order data incomplete", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!NetworkHelper.isOnline(this)) {
            NetworkHelper.showOfflineToast(this);
            return;
        }

        setLoading(true);

        // FINAL AVAILABILITY CHECK: Fetch latest product data before saving order
        List<String> idsToCheck = new ArrayList<>();
        if (fromCart) {
            for (CartItem item : cartItems) idsToCheck.add(item.getProductId());
        } else {
            idsToCheck.add(productId);
        }

        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String id : idsToCheck) {
            tasks.add(FirebaseHelper.getDb().collection("products").document(id).get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            for (Object obj : results) {
                DocumentSnapshot doc = (DocumentSnapshot) obj;
                if (!doc.exists() || Boolean.FALSE.equals(doc.getBoolean("available"))) {
                    setLoading(false);
                    Toast.makeText(this, "Sorry, some items are no longer available.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            // All items available, proceed to save order
            saveOrderToFirestore();
        }).addOnFailureListener(e -> {
            setLoading(false);
            Toast.makeText(this, "Verification failed. Try again.", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveOrderToFirestore() {
        Order order = new Order();
        order.setCustomerId(currentUid);
        order.setCustomerName(customer.getName());
        order.setCustomerPhone(customer.getPhone());
        order.setCustomerAddress(pinnedAddress != null ? pinnedAddress : customer.getAddress());
        order.setCustomerLat(pinnedLat);
        order.setCustomerLng(pinnedLng);
        
        if (fromCart) {
            order.setSellerId(cartItems.get(0).getSellerId());
            order.setSellerName(cartItems.get(0).getSellerName());
            order.setItems(cartItems);
            
            order.setProductId(cartItems.get(0).getProductId());
            order.setProductName(cartItems.get(0).getProductName() + (cartItems.size() > 1 ? " & others" : ""));
            order.setProductImageBase64(cartItems.get(0).getImageBase64());
        } else {
            order.setSellerId(singleProduct.getSellerId());
            order.setSellerName(sellerName.isEmpty() ? singleProduct.getSellerName() : sellerName);
            order.setProductId(singleProduct.getProductId());
            order.setProductName(singleProduct.getName());
            order.setProductImageBase64(singleProduct.getImageBase64());
            order.setQuantity(singleQuantity);
            order.setProductPrice(singleProduct.getPrice());
            
            List<CartItem> items = new ArrayList<>();
            items.add(new CartItem(productId, singleProduct.getName(), singleProduct.getPrice(), singleQuantity, singleProduct.getSellerId(), singleProduct.getSellerName(), singleProduct.getImageBase64()));
            order.setItems(items);
        }

        order.setSellerLat(sellerLat);
        order.setSellerLng(sellerLng);
        order.setPaymentMethod("COD");
        order.setDeliveryFee(deliveryFee);
        order.setDistanceKm(distanceKm);
        
        double subtotal = 0;
        for (CartItem item : order.getItems()) subtotal += (item.getPrice() * item.getQuantity());
        order.setTotalAmount(subtotal + deliveryFee);
        order.setStatus(Order.STATUS_PENDING);

        FirebaseHelper.getDb().collection("orders").add(order)
                .addOnSuccessListener(ref -> {
                    String orderId = ref.getId();
                    ref.update("orderId", orderId);
                    
                    if (fromCart) CartHelper.clearCart(this);
                    
                    NotificationHelper.notifyNewOrder(orderId, order.getSellerId(), customer.getName());

                    setLoading(false);
                    Intent intent = new Intent(this, OrderSuccessActivity.class);
                    intent.putExtra("orderId", orderId);
                    intent.putExtra("productName", order.getProductName());
                    intent.putExtra("total", String.format("₱%.0f", order.getTotalAmount()));
                    intent.putExtra("storeName", order.getSellerName());
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Order failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean loading) {
        binding.btnPlaceOrder.setEnabled(!loading);
        binding.progressOrder.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnPlaceOrder.setText(loading ? "" : "Confirm and Place Order");
    }
}
