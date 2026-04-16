package com.appdev.bilijuan.activities.customer;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchActivity extends AppCompatActivity {

    private ActivitySearchBinding binding;
    private FoodListingAdapter adapter;
    private StoreCircleAdapter storeAdapter;
    private StoreCircleAdapter searchStoresAdapter;
    private final List<Product> results = new ArrayList<>();
    private final List<Product> allProducts = new ArrayList<>();
    private final List<User> suggestedStores = new ArrayList<>();
    private final List<User> allStores = new ArrayList<>();
    private final List<User> filteredStores = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerViews();
        setupBottomNav();
        setupListeners();
        
        loadAllProducts();
        loadAllStores();
        
        setupDefaultCategories();
    }

    private void setupListeners() {
        binding.etSearch.setOnFocusChangeListener((v, hasFocus) -> 
            updateSearchIcons(hasFocus, binding.etSearch.getText().toString().trim())
        );

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                updateSearchIcons(binding.etSearch.hasFocus(), query);
                filterSearch(query);
            }
        });

        binding.btnBackSearch.setOnClickListener(v -> {
            binding.etSearch.setText("");
            binding.etSearch.clearFocus();
            hideKeyboard();
            updateSearchIcons(false, "");
        });
    }

    private void updateSearchIcons(boolean hasFocus, String query) {
        boolean showBack = hasFocus || !query.isEmpty();
        binding.btnBackSearch.setVisibility(showBack ? View.VISIBLE : View.GONE);
        binding.ivSearchIcon.setVisibility(showBack ? View.GONE : View.VISIBLE);
    }

    private void setupBottomNav() {
        CustomerNavHelper.setup(this, binding.customerNav.getRoot(), CustomerNavHelper.Tab.SEARCH);
    }

    private void setupRecyclerViews() {
        // Results Adapter (Products)
        adapter = new FoodListingAdapter(results, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("productId", product.getProductId());
            startActivity(intent);
        });
        binding.rvResults.setLayoutManager(new LinearLayoutManager(this));
        binding.rvResults.setAdapter(adapter);
        binding.rvResults.setNestedScrollingEnabled(false);

        // Store Suggestions Adapter (Horizontal)
        storeAdapter = new StoreCircleAdapter(suggestedStores, store -> {
            Intent intent = new Intent(this, StoreDetailActivity.class);
            intent.putExtra("sellerId", store.getUid());
            startActivity(intent);
        });
        binding.rvStoreSuggestions.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvStoreSuggestions.setAdapter(storeAdapter);
        binding.rvStoreSuggestions.setNestedScrollingEnabled(false);

        // Filtered Stores Adapter (Horizontal, shown during search)
        searchStoresAdapter = new StoreCircleAdapter(filteredStores, store -> {
            Intent intent = new Intent(this, StoreDetailActivity.class);
            intent.putExtra("sellerId", store.getUid());
            startActivity(intent);
        });
        binding.rvSearchStores.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvSearchStores.setAdapter(searchStoresAdapter);
        binding.rvSearchStores.setNestedScrollingEnabled(false);
    }

    private void setupDefaultCategories() {
        String[] cats = getResources().getStringArray(R.array.product_categories);
        setupCategoryChips(new HashSet<>(Arrays.asList(cats)));
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
                    
                    // Re-filter if user already typed something (avoids race condition)
                    String query = binding.etSearch.getText().toString().trim();
                    if (!query.isEmpty()) filterSearch(query);
                })
                .addOnFailureListener(e -> Log.e("SearchActivity", "Error loading products", e));
    }

    private void loadAllStores() {
        FirebaseHelper.getDb().collection("users")
                .whereEqualTo("role", "seller")
                .get()
                .addOnSuccessListener(snap -> {
                    allStores.clear();
                    suggestedStores.clear();
                    if (snap != null) {
                        for (QueryDocumentSnapshot doc : snap) {
                            User user = doc.toObject(User.class);
                            user.setUid(doc.getId());
                            if (user.isActive()) {
                                allStores.add(user);
                                if (suggestedStores.size() < 15) suggestedStores.add(user);
                            }
                        }
                    }
                    storeAdapter.notifyDataSetChanged();
                    
                    boolean hasStores = !suggestedStores.isEmpty();
                    binding.layoutPopularStores.setVisibility(hasStores ? View.VISIBLE : View.GONE);
                    
                    // Re-filter if user already typed something (avoids race condition)
                    String query = binding.etSearch.getText().toString().trim();
                    if (!query.isEmpty()) {
                        filterSearch(query);
                    } else {
                        binding.layoutSuggestions.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Log.e("SearchActivity", "Error loading stores", e));
    }

    private void setupCategoryChips(Set<String> categories) {
        binding.chipGroupCategories.removeAllViews();
        for (String cat : categories) {
            Chip chip = new Chip(this);
            chip.setText(cat);
            chip.setCheckable(true);
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#EAFAF1")));
            chip.setTextColor(ColorStateList.valueOf(Color.parseColor("#27AE60")));
            chip.setChipStrokeColor(ColorStateList.valueOf(Color.parseColor("#27AE60")));
            chip.setChipStrokeWidth(2f);

            chip.setOnClickListener(v -> {
                binding.etSearch.setText(cat);
                filterSearch(cat);
                hideKeyboard();
                binding.etSearch.clearFocus();
            });
            binding.chipGroupCategories.addView(chip);
        }
    }

    private void filterSearch(String query) {
        if (query.isEmpty()) {
            binding.layoutSuggestions.setVisibility(View.VISIBLE);
            binding.layoutPopularStores.setVisibility(suggestedStores.isEmpty() ? View.GONE : View.VISIBLE);
            binding.layoutSearchResults.setVisibility(View.GONE);
            binding.emptyResults.setVisibility(View.GONE);
            return;
        }

        binding.layoutSuggestions.setVisibility(View.GONE);
        binding.layoutSearchResults.setVisibility(View.VISIBLE);
        
        String lower = query.toLowerCase();
        
        // Identify store IDs that match by product info (name, category, or sellerName)
        Set<String> storesWithMatches = new HashSet<>();
        for (Product p : allProducts) {
            boolean pMatches = (p.getName() != null && p.getName().toLowerCase().contains(lower)) ||
                              (p.getSellerName() != null && p.getSellerName().toLowerCase().contains(lower)) ||
                              (p.getCategory() != null && p.getCategory().toLowerCase().contains(lower));
            if (pMatches) {
                storesWithMatches.add(p.getSellerId());
            }
        }

        // Filter Stores: match by store name OR if they have matching products
        filteredStores.clear();
        for (User store : allStores) {
            boolean storeNameMatches = store.getName() != null && store.getName().toLowerCase().contains(lower);
            boolean hasMatchingInfo = storesWithMatches.contains(store.getUid());
            
            if (storeNameMatches || hasMatchingInfo) {
                filteredStores.add(store);
            }
        }

        // Filter Products (Standard matching)
        results.clear();
        for (Product p : allProducts) {
            boolean matchesName = p.getName() != null && p.getName().toLowerCase().contains(lower);
            boolean matchesSeller = p.getSellerName() != null && p.getSellerName().toLowerCase().contains(lower);
            boolean matchesCategory = p.getCategory() != null && p.getCategory().toLowerCase().contains(lower);
            if (matchesName || matchesSeller || matchesCategory) {
                results.add(p);
            }
        }

        updateSearchResultsUI();
    }

    private void updateSearchResultsUI() {
        adapter.notifyDataSetChanged();
        searchStoresAdapter.notifyDataSetChanged();
        
        binding.layoutSearchStores.setVisibility(filteredStores.isEmpty() ? View.GONE : View.VISIBLE);
        binding.rvResults.setVisibility(results.isEmpty() ? View.GONE : View.VISIBLE);

        boolean hasResults = !results.isEmpty() || !filteredStores.isEmpty();
        binding.emptyResults.setVisibility(hasResults ? View.GONE : View.VISIBLE);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
