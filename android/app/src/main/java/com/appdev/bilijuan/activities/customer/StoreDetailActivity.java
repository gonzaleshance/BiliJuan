package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.adapters.FoodListingAdapter;
import com.appdev.bilijuan.databinding.ActivityStoreDetailBinding;
import com.appdev.bilijuan.models.Product;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.CustomerNavHelper;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StoreDetailActivity extends AppCompatActivity {

    private ActivityStoreDetailBinding binding;
    private FoodListingAdapter adapter;
    private final List<Product> products = new ArrayList<>();
    private ListenerRegistration productListener;
    private String sellerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStoreDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sellerId = getIntent().getStringExtra("sellerId");
        if (sellerId == null) { finish(); return; }

        setupUI();
        loadStoreInfo();
        loadProducts();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());
        
        // Setup Bottom Nav using the included layout via ViewBinding
        CustomerNavHelper.setup(this, binding.customerNav.getRoot(), null);

        // Initialize with loading state or empty
        binding.tvRating.setText("");
        binding.tvItemCount.setText("Loading items...");

        // Setup Products RecyclerView
        adapter = new FoodListingAdapter(products, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("productId", product.getProductId());
            startActivity(intent);
        });
        binding.rvProducts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvProducts.setAdapter(adapter);
        binding.rvProducts.setNestedScrollingEnabled(false);
    }

    private void loadStoreInfo() {
        FirebaseHelper.getDb().collection("users").document(sellerId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    User seller = doc.toObject(User.class);
                    if (seller == null) return;

                    binding.tvStoreName.setText(seller.getName());
                    
                    // Load Logo
                    if (seller.getStoreImageBase64() != null && !seller.getStoreImageBase64().isEmpty()) {
                        try {
                            byte[] bytes = Base64.decode(seller.getStoreImageBase64(), Base64.DEFAULT);
                            Glide.with(this).asBitmap().load(bytes).circleCrop().into(binding.ivStoreLogo);
                        } catch (Exception e) {
                            binding.ivStoreLogo.setImageResource(R.drawable.ic_store);
                        }
                    }

                    // Load Banner (Using profile image or placeholder)
                    if (seller.getProfileImageUrl() != null && !seller.getProfileImageUrl().isEmpty()) {
                        Glide.with(this).load(seller.getProfileImageUrl()).centerCrop().into(binding.ivStoreBanner);
                    }
                });
    }

    private void loadProducts() {
        productListener = FirebaseHelper.getDb().collection("products")
                .whereEqualTo("sellerId", sellerId)
                .whereEqualTo("available", true)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    products.clear();
                    double totalStars = 0;
                    int totalRatings = 0;
                    for (QueryDocumentSnapshot doc : snap) {
                        Product p = doc.toObject(Product.class);
                        p.setProductId(doc.getId());
                        products.add(p);
                        totalStars += (p.getStars() * p.getRatingCount());
                        totalRatings += p.getRatingCount();
                    }
                    adapter.notifyDataSetChanged();
                    
                    if (totalRatings > 0) {
                        double avg = totalStars / totalRatings;
                        binding.tvRating.setText(String.format(Locale.getDefault(), "%.1f (%d reviews)", avg, totalRatings));
                    } else if (!products.isEmpty()) {
                        binding.tvRating.setText("No ratings yet");
                    }

                    binding.tvItemCount.setText(products.size() + " items available");
                    binding.emptyProducts.setVisibility(products.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (productListener != null) productListener.remove();
    }
}