package com.appdev.bilijuan.activities.customer;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.appdev.bilijuan.R;
import com.appdev.bilijuan.adapters.FoodListingAdapter;
import com.appdev.bilijuan.adapters.StoreCircleAdapter;
import com.appdev.bilijuan.databinding.ActivitySearchBinding;
import com.appdev.bilijuan.models.Product;
import com.appdev.bilijuan.models.User;
import com.appdev.bilijuan.utils.CustomerNavHelper;
import com.appdev.bilijuan.utils.FirebaseHelper;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchActivity extends AppCompatActivity {

    private ActivitySearchBinding binding;
    private FoodListingAdapter adapter;
    private StoreCircleAdapter storeAdapter;
    private final List<Product> results = new ArrayList<>();
    private final List<Product> allProducts = new ArrayList<>();
    private final List<User> suggestedStores = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerViews();
        setupBottomNav();
        
        // Removed back button click listener since the button was removed from XML
        
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                filterProducts(s.toString().trim());
            }
        });

        loadAllProducts();
        loadStores();
    }

    private void setupBottomNav() {
        CustomerNavHelper.setup(this, binding.customerNav.getRoot(), CustomerNavHelper.Tab.SEARCH);
    }

    private void setupRecyclerViews() {
        // Results Adapter
        adapter = new FoodListingAdapter(results, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("productId", product.getProductId());
            startActivity(intent);
        });
        binding.rvResults.setLayoutManager(new LinearLayoutManager(this));
        binding.rvResults.setAdapter(adapter);

        // Store Suggestions Adapter
        storeAdapter = new StoreCircleAdapter(suggestedStores, store -> {
            binding.etSearch.setText(store.getName());
            filterProducts(store.getName());
        });
        binding.rvStoreSuggestions.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvStoreSuggestions.setAdapter(storeAdapter);
    }

    private void loadAllProducts() {
        FirebaseHelper.getDb().collection("products")
                .whereEqualTo("available", true)
                .get()
                .addOnSuccessListener(snap -> {
                    allProducts.clear();
                    Set<String> categories = new HashSet<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Product p = doc.toObject(Product.class);
                        p.setProductId(doc.getId());
                        allProducts.add(p);
                        if (p.getCategory() != null && !p.getCategory().isEmpty()) {
                            categories.add(p.getCategory());
                        }
                    }
                    setupCategoryChips(categories);
                })
                .addOnFailureListener(e -> Log.e("SearchActivity", "Error loading products", e));
    }

    private void loadStores() {
        FirebaseHelper.getDb().collection("users")
                .whereEqualTo("role", "seller")
                .limit(15)
                .get()
                .addOnSuccessListener(snap -> {
                    suggestedStores.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        User user = doc.toObject(User.class);
                        user.setUid(doc.getId());
                        // Status might be null in old docs, handle as active
                        String status = user.getStatus();
                        if (status == null || "active".equals(status)) {
                            suggestedStores.add(user);
                        }
                    }
                    Log.d("SearchActivity", "Loaded " + suggestedStores.size() + " stores");
                    storeAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("SearchActivity", "Error loading stores", e));
    }

    private void setupCategoryChips(Set<String> categories) {
        binding.chipGroupCategories.removeAllViews();
        for (String cat : categories) {
            Chip chip = new Chip(this);
            chip.setText(cat);
            chip.setCheckable(true);
            
            // Hardcoded Colors for Categories
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#EAFAF1"))); // Light green
            chip.setTextColor(ColorStateList.valueOf(Color.parseColor("#27AE60"))); // Dark green
            chip.setChipStrokeColor(ColorStateList.valueOf(Color.parseColor("#27AE60")));
            chip.setChipStrokeWidth(2f);

            chip.setOnClickListener(v -> {
                binding.etSearch.setText(cat);
                filterProducts(cat);
            });
            binding.chipGroupCategories.addView(chip);
        }
    }

    private void filterProducts(String query) {
        if (query.isEmpty()) {
            binding.layoutSuggestions.setVisibility(View.VISIBLE);
            binding.rvResults.setVisibility(View.GONE);
            binding.emptyResults.setVisibility(View.GONE);
            return;
        }

        binding.layoutSuggestions.setVisibility(View.GONE);
        binding.rvResults.setVisibility(View.VISIBLE);
        
        results.clear();
        String lower = query.toLowerCase();
        for (Product p : allProducts) {
            boolean matchesName = p.getName() != null && p.getName().toLowerCase().contains(lower);
            boolean matchesSeller = p.getSellerName() != null && p.getSellerName().toLowerCase().contains(lower);
            boolean matchesCategory = p.getCategory() != null && p.getCategory().toLowerCase().contains(lower);
            
            if (matchesName || matchesSeller || matchesCategory) {
                results.add(p);
            }
        }
        adapter.notifyDataSetChanged();
        binding.emptyResults.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
    }
}