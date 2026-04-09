package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.adapters.FoodListingAdapter;
import com.appdev.bilijuan.databinding.ActivitySearchBinding;
import com.appdev.bilijuan.models.Product;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private ActivitySearchBinding binding;
    private FoodListingAdapter adapter;
    private final List<Product> results = new ArrayList<>();
    private final List<Product> allProducts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();
        binding.btnBack.setOnClickListener(v -> finish());
        binding.etSearch.requestFocus();

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                filterProducts(s.toString().trim());
            }
        });

        loadAllProducts();
    }

    private void setupRecyclerView() {
        adapter = new FoodListingAdapter(results, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("productId", product.getProductId());
            startActivity(intent);
        });
        binding.rvResults.setLayoutManager(new LinearLayoutManager(this));
        binding.rvResults.setAdapter(adapter);
    }

    private void loadAllProducts() {
        FirebaseHelper.getDb().collection("products")
                .whereEqualTo("available", true)
                .get()
                .addOnSuccessListener(snap -> {
                    allProducts.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Product p = doc.toObject(Product.class);
                        p.setProductId(doc.getId());
                        allProducts.add(p);
                    }
                });
    }

    private void filterProducts(String query) {
        results.clear();
        if (query.isEmpty()) {
            adapter.notifyDataSetChanged();
            binding.emptyResults.setVisibility(View.GONE);
            return;
        }
        String lower = query.toLowerCase();
        for (Product p : allProducts) {
            if (p.getName().toLowerCase().contains(lower)
                    || p.getSellerName().toLowerCase().contains(lower)
                    || p.getCategory().toLowerCase().contains(lower)) {
                results.add(p);
            }
        }
        adapter.notifyDataSetChanged();
        binding.emptyResults.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
    }
}