package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.adapters.FoodListingAdapter;
import com.appdev.bilijuan.databinding.ActivityStoreDetailBinding;
import com.appdev.bilijuan.models.Product;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StoreDetailActivity extends AppCompatActivity {

    private ActivityStoreDetailBinding binding;
    private FoodListingAdapter adapter;
    private final List<Product> products = new ArrayList<>();
    private ListenerRegistration listener;
    private String sellerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStoreDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sellerId = getIntent().getStringExtra("sellerId");
        String storeName = getIntent().getStringExtra("storeName");
        if (sellerId == null) { finish(); return; }

        binding.tvStoreName.setText(storeName != null ? storeName : "Store");
        binding.btnBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        loadProducts();
    }

    private void setupRecyclerView() {
        adapter = new FoodListingAdapter(products, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("productId", product.getProductId());
            startActivity(intent);
        });
        binding.rvProducts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvProducts.setAdapter(adapter);
    }

    private void loadProducts() {
        listener = FirebaseHelper.getDb().collection("products")
                .whereEqualTo("sellerId", sellerId)
                .whereEqualTo("available", true)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    products.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Product p = doc.toObject(Product.class);
                        p.setProductId(doc.getId());
                        products.add(p);
                    }
                    adapter.notifyDataSetChanged();
                    binding.tvItemCount.setText(products.size() + " items available");
                    binding.emptyProducts.setVisibility(
                            products.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}